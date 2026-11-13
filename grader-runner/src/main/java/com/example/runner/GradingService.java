package com.example.runner;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;



import jakarta.annotation.PreDestroy;
import com.example.runner.model.GradingJobRequest;
import com.example.runner.model.GradingResultResponse;
import com.example.runner.model.GradingRunRequest;
import com.example.runner.model.GradingRunResponse;
import com.example.runner.model.SubmissionRequest;
import com.example.runner.model.SubmissionResponse;
import com.example.runner.model.SubmissionTestResponse;
import com.example.runner.model.TestRequest;


public class GradingService {
private final ExecutorService executor = Executors.newCachedThreadPool();
   public GradingResultResponse grade(GradingJobRequest request) throws IOException {

    Integer PROCESS_TIMEOUT_SECONDS = 4;
    
    Long submissionId = request.submission().submissionId();
    Long gradingRunId = request.gradingRun().gradingRunId();
    List<TestRequest> tests = request.tests();

    String submissionStatus = request.submission().status();
    Double submissionGrade = request.submission().submissionGrade();

    String gradingRunStatus = request.gradingRun().status();
    Double gradingRunGrade = request.gradingRun().gradingRunGrade();

    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime finishedAt;

    int passedTestCases = 0;
    int totalTestCases = tests.size();

    SubmissionResponse submissionResponse = new SubmissionResponse(submissionId, submissionStatus, submissionGrade);
    GradingRunResponse gradingRunResponse = new GradingRunResponse(gradingRunId, gradingRunStatus, gradingRunGrade, startedAt, null);


        Path tempDir = Files.createTempDirectory("runner-");
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, request.submission().sourceCode());
            List<SubmissionTestResponse> submissionTestResponses = addSubmissionTests(request.gradingRun(), tests);
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            compileBuilder.redirectErrorStream(true);
            Process compileProcess = compileBuilder.start();
            try {
                boolean finished = compileProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    stopProcess(compileProcess);
                    submissionStatus = "TIME_LIMIT_EXCEEDED";
                    submissionGrade = 0.0D;

                    gradingRunStatus = "COMPLETED";
                    gradingRunGrade = 0.0D;
                    finishedAt = LocalDateTime.now();
                    submissionResponse = updateSubmission(submissionResponse, submissionStatus, submissionGrade);
                    gradingRunResponse = updateGradingRun(gradingRunResponse, gradingRunStatus, gradingRunGrade, startedAt, finishedAt);
                    markSubmissionTestsAsFailed(submissionTestResponses);
                    return new GradingResultResponse(submissionResponse, gradingRunResponse, submissionTestResponses);
                }
                int compileExitCode = compileProcess.exitValue();
                if (compileExitCode != 0) {
                    submissionStatus = "COMPILE_ERROR";
                    submissionGrade = 0.0D;

                    gradingRunStatus = "COMPLETED";
                    gradingRunGrade = 0.0D;
                    finishedAt = LocalDateTime.now();
                    

                    submissionResponse = updateSubmission(submissionResponse, submissionStatus, submissionGrade);
                    gradingRunResponse = updateGradingRun(gradingRunResponse, gradingRunStatus, gradingRunGrade, startedAt, finishedAt);
                    markSubmissionTestsAsFailed(submissionTestResponses);
                    return new GradingResultResponse(submissionResponse, gradingRunResponse, submissionTestResponses);
                }
            } catch (InterruptedException e) {
                submissionStatus = "SYSTEM_ERROR";
                submissionGrade = null;
            
                gradingRunStatus = "FAILED";
                finishedAt = LocalDateTime.now();

                Thread.currentThread().interrupt();
                submissionResponse = updateSubmission(submissionResponse, submissionStatus, submissionGrade);
                gradingRunResponse = updateGradingRun(gradingRunResponse, gradingRunStatus, gradingRunGrade, startedAt, finishedAt);
                markSubmissionTestsAsFailed(submissionTestResponses);
                return new GradingResultResponse(submissionResponse, gradingRunResponse, submissionTestResponses);
            }
            Path memoryFile = tempDir.resolve("memory-file");
            ProcessBuilder runBuilder = new ProcessBuilder(
                "/usr/bin/time",
                "-f",
                "%M",
                "-o",
                memoryFile.toString(),
                "java",
                "-cp",
                tempDir.toAbsolutePath().toString(),
                "Main"
            );
            runBuilder.redirectErrorStream(true);
            for(int i = 0; i < tests.size(); i++) {
                long start = System.nanoTime();
                Process runProcess = runBuilder.start();
                
                try {
                   
                    Future<String> outputFuture = startOutputReader(runProcess);
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                        if (tests.get(i).input() != null) {
                            writer.write(tests.get(i).input());
                        }
                    }
                    long timeoutMs = tests.get(i).timeLimitMs() != null ? tests.get(i).timeLimitMs() : 4000L;
                    
                    boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start
                    );
                    
                    if (!finished) {
                        stopProcess(runProcess);
                        outputFuture.cancel(true);
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "TIME_LIMIT_EXCEEDED",
                                null,
                                null,
                                durationMs
                            )
                        );
                        Files.deleteIfExists(memoryFile);
                        continue;
                    }
                    String stdout;
                    try {
                        stdout = getOutput(outputFuture);
                    } catch (OutputLimitExceededException e) {
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "OUTPUT_LIMIT_EXCEEDED",
                                null,
                                null,
                                durationMs
                            )
                        );
                        continue;
                    }
                    int runExitCode = runProcess.exitValue();
                    if(runExitCode != 0) {
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "RUNTIME_ERROR",
                                null,
                                null,
                                durationMs
                            )
                        );
                        Files.deleteIfExists(memoryFile);
                        continue;
                    }

                    Long peakMemoryKiB = null;
                    try {
                        peakMemoryKiB = Long.parseLong(Files.readString(memoryFile).trim());
                    } catch (IOException | NumberFormatException e) {
    
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "SYSTEM_ERROR",
                                null,
                                null,
                                durationMs
                            )
                        );
                        continue;
                    } finally {
                        Files.deleteIfExists(memoryFile);
                    }

                    if (tests.get(i).memoryLimitKib() != null
                            && peakMemoryKiB >= tests.get(i).memoryLimitKib()) {
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "MEMORY_LIMIT_EXCEEDED",
                                null,
                                peakMemoryKiB,
                                durationMs
                            )
                        );
                        continue;
                    }
                    
                    boolean passed = stdout.trim().equals(tests.get(i).expectedOutput().trim());

                    if(passed) {
                        passedTestCases += 1;
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "PASSED",
                                stdout,
                                peakMemoryKiB,
                                durationMs
                            )
                        );
                        continue;
                    }
                    else {
                        submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "WRONG_ANSWER",
                                stdout,
                                peakMemoryKiB,
                                durationMs
                            )
                        );
                        continue;
                    }

                } catch (InterruptedException e) {
                   submissionStatus = "SYSTEM_ERROR";
                   submissionGrade = null;
     
                    gradingRunStatus = "FAILED";
                    finishedAt = LocalDateTime.now();
                
                    submissionTestResponses.set(
                            i,
                            updateSubmissionTest(
                                submissionTestResponses.get(i),
                                "SYSTEM_ERROR",
                                null,
                                null,
                                null
                            )
                        );
                    Thread.currentThread().interrupt();
                    submissionResponse = updateSubmission(submissionResponse, submissionStatus, submissionGrade);
                    gradingRunResponse = updateGradingRun(gradingRunResponse, gradingRunStatus, gradingRunGrade, startedAt, finishedAt);
                    markSubmissionTestsAsFailed(submissionTestResponses);
                    return new GradingResultResponse(submissionResponse, gradingRunResponse, submissionTestResponses);
            } finally {
                Files.deleteIfExists(memoryFile);
            }
            }
            
            submissionGrade =
                Math.round((passedTestCases * 10.0 / totalTestCases) * 10) / 10.0;
            gradingRunGrade = submissionGrade;
            String firstFailure = submissionTestResponses.stream()
                .map(SubmissionTestResponse::status)
                .filter(status -> !"PASSED".equals(status))
                .findFirst()
                .orElse(null);

            submissionStatus = firstFailure == null
                ? "ACCEPTED"
                : map(firstFailure);

            gradingRunStatus = "COMPLETED";
            finishedAt = LocalDateTime.now();

            submissionResponse = updateSubmission(submissionResponse, submissionStatus, gradingRunGrade);
            gradingRunResponse = updateGradingRun(gradingRunResponse, gradingRunStatus, gradingRunGrade, startedAt, finishedAt);
            return new GradingResultResponse(submissionResponse, gradingRunResponse, submissionTestResponses);
            
        } finally {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted((first, second) -> second.compareTo(first))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }
        }
   }
   @PreDestroy
    public void shutdownExecutor() {
        executor.shutdownNow();
    }

  

   private void stopProcess(Process p) {
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }


   private List<SubmissionTestResponse> addSubmissionTests(
    GradingRunRequest gradingRunRquest, 
    List<TestRequest> tests
    ) {
    List<SubmissionTestResponse> submissionTestResponses = new ArrayList<>();
    for (TestRequest test : tests) {
        SubmissionTestResponse submissionTestResponse = new SubmissionTestResponse(
            gradingRunRquest.gradingRunId(),
            test.testId(),
            "QUEUED",
            null,
            null,
            null
        );
        submissionTestResponses.add(submissionTestResponse);
    }
    return submissionTestResponses;
   }
   private void markSubmissionTestsAsFailed(
    List<SubmissionTestResponse> submissionTestResponses
    ) {
    if(submissionTestResponses.isEmpty()) {
        throw new IllegalStateException("No SubmissionTest was found.");
    }
    for (int i = 0; i < submissionTestResponses.size(); i++) {
        SubmissionTestResponse current = submissionTestResponses.get(i);
        SubmissionTestResponse updated = updateSubmissionTest(
            current,
            "NOT_RUN",
            null,
            null,
            null
        );
        submissionTestResponses.set(i, updated);
    }
   }

   private static class OutputLimitExceededException
        extends RuntimeException {
}

   private Future<String> startOutputReader(Process process) {
        return executor.submit(() -> {
            final long maxOutputCharacters = 1_000_000;
            StringBuilder output = new StringBuilder();
            char[] buffer = new char[4096];
            long totalCharacters = 0;

            try (Reader reader = new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8)) {

                int count;

                while ((count = reader.read(buffer)) != -1) {
                    totalCharacters += count;

                    if (totalCharacters > maxOutputCharacters) {
                        stopProcess(process);
                        throw new OutputLimitExceededException();
                    }

                    output.append(buffer, 0, count);
                }
            }

            return output.toString();
        });
    }

    private String getOutput(Future<String> outputFuture)
        throws IOException {
        try {
            return outputFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Output reading interrupted", e);

        } catch (ExecutionException e) {
            if (e.getCause() instanceof OutputLimitExceededException) {
                throw (OutputLimitExceededException) e.getCause();
            }

            throw new IOException("Output reading failed", e.getCause());
        }
    }

 

    private String map(String status) {
        return switch (status) {
            case "PASSED" -> "ACCEPTED";
            case "WRONG_ANSWER" -> "WRONG_ANSWER";
            case "COMPILE_ERROR" -> "COMPILE_ERROR";
            case "TIME_LIMIT_EXCEEDED" -> "TIME_LIMIT_EXCEEDED";
            case "MEMORY_LIMIT_EXCEEDED" -> "MEMORY_LIMIT_EXCEEDED";
            case "RUNTIME_ERROR" -> "RUNTIME_ERROR";
            case "SYSTEM_ERROR" -> "SYSTEM_ERROR";
            case "QUEUED" -> "QUEUED";
            case "RUNNING" -> "RUNNING";
            case "NOT_RUN" -> "NOT_RUN";
            case "OUTPUT_LIMIT_EXCEEDED" -> "OUTPUT_LIMIT_EXCEEDED";
            default -> null;
        };
    }
   

    private SubmissionTestResponse updateSubmissionTest(
            SubmissionTestResponse existing, 
            String status, 
            String actualOutput, 
            Long memoryUsedKib,
            Long executionTimeMs) {
        return new SubmissionTestResponse(
            existing.gradingRunId(),
            existing.testId(),
            status != null ? status : existing.status(),
            actualOutput != null ? actualOutput : existing.actualOutput(),
            memoryUsedKib != null ? memoryUsedKib : existing.memoryUsedKib(),
            executionTimeMs != null ? executionTimeMs : existing.executionTimeMs()
        );
    }

    private SubmissionResponse updateSubmission(
            SubmissionResponse existing,
            String status,
            Double grade) {
        return new SubmissionResponse(
            existing.submissionId(), 
            status != null ? status : existing.status(), 
            grade != null ? grade : existing.submissionGrade()
        );
    }

    private GradingRunResponse updateGradingRun(
            GradingRunResponse existing,
            String status,
            Double grade,
            LocalDateTime startedAt,
            LocalDateTime finishedAt) {
        return new GradingRunResponse(
            existing.gradingRunId(),
            status != null ? status : existing.status(),
            grade != null ? grade : existing.gradingRunGrade(),
            startedAt != null ? startedAt : existing.startedAt(),
            finishedAt != null ? finishedAt : existing.finishedAt()
        );
    }

}
