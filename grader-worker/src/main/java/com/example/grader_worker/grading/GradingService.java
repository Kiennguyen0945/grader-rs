package com.example.grader_worker.grading;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.gradingrun.GradingRun;
import com.example.grader_worker.gradingrun.GradingRunRepository;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;
import com.example.grader_worker.submissiontest.SubmissionTest;
import com.example.grader_worker.submissiontest.SubmissionTestRepository;
import com.example.grader_worker.test.Test;
import com.example.grader_worker.config.KubernetesJobService;

import jakarta.annotation.PreDestroy;



@Service

public class GradingService {
    private static final long PROCESS_TIMEOUT_SECONDS = 4;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AssignmentRepository assignmentRepository;
    private final SubmissionTestRepository submissionTestRepository;
    private final SubmissionRepository submissionRepository;
    private final GradingRunRepository gradingRunRepository;
    private final KubernetesJobService kubernetesJobService;
    public GradingService(
            AssignmentRepository assignmentRepository, 
            SubmissionTestRepository submissionTestRepository, 
            SubmissionRepository submissionRepository, 
            GradingRunRepository gradingRunRepository, 
            KubernetesJobService kubernetesJobService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionTestRepository = submissionTestRepository;
        this.submissionRepository = submissionRepository;
        this.gradingRunRepository = gradingRunRepository;
        this.kubernetesJobService = kubernetesJobService;
    }
   public void grade(Submission submission, GradingRun gradingRun) throws IOException {
        if (gradingRun.getStatus() != GradingRun.Status.QUEUED) {
            return;
        }
        Assignment assignment = findAssignment(submission);
        validateAssignment(assignment, submission);
        Path tempDir = Files.createTempDirectory("runner-");
        System.out.println("Grading Worker Reached");
        kubernetesJobService.createTestJob(submission.getId(), gradingRun.getId());
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, submission.getSourceCode());
            setSubmission(submission, Submission.Status.RUNNING, null);
            setGradingRun(gradingRun, GradingRun.Status.RUNNING, null,  LocalDateTime.now(), null);
            addSubmissionTests(gradingRun, assignment.getTests());
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            compileBuilder.redirectErrorStream(true);
            Process compileProcess = compileBuilder.start();
            try {
                boolean finished = compileProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    stopProcess(compileProcess);
                    setSubmission(submission, Submission.Status.TIME_LIMIT_EXCEEDED, 0.0D);
                    setGradingRun(gradingRun, GradingRun.Status.COMPLETED, 0.0D, null, LocalDateTime.now());
                    markSubmissionTestsAsFailed(gradingRun);
                    return;
                }
                int compileExitCode = compileProcess.exitValue();
                if (compileExitCode != 0) {
                    setSubmission(submission, Submission.Status.COMPILE_ERROR, 0.0D);
                    setGradingRun(gradingRun, GradingRun.Status.COMPLETED, 0.0D, null, LocalDateTime.now());
                    markSubmissionTestsAsFailed(gradingRun);
                    return;
                }
            } catch (InterruptedException e) {
                setSubmission(
                    submission,
                    Submission.Status.SYSTEM_ERROR,
                    null
                );

                setGradingRun(
                    gradingRun,
                    GradingRun.Status.FAILED,
                    null,
                    null,
                    LocalDateTime.now()
                );

                Thread.currentThread().interrupt();
                return;
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
            int passedTestCases = 0;
            int totalTestCases = assignment.getTests().size();
            for(Test test : assignment.getTests()) {
                long start = System.nanoTime();
                Process runProcess = runBuilder.start();
                SubmissionTest submissionTest = 
                    submissionTestRepository
                    .findByGradingRun_Submission_IdAndTest_IdAndGradingRun_Id(
                        gradingRun.getSubmission().getId(), 
                        test.getId(), 
                        gradingRun.getId()
                    ).orElseThrow(() -> new IllegalStateException("SubmissionTest not found.")
                );
                
                
                try {
                   
                    Future<String> outputFuture = startOutputReader(runProcess);
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                        if (test.getInput() != null) {
                            writer.write(test.getInput());
                        }
                    }
                    long timeoutMs = test.getTimeLimitMs() != null ? test.getTimeLimitMs() : 4000L;
                    
                    boolean finished = runProcess.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start
                    );
                    
                    if (!finished) {
                        stopProcess(runProcess);
                        outputFuture.cancel(true);
                        updateSubmissionTestResults(
                            submissionTest,
                            SubmissionTest.Status.TIME_LIMIT_EXCEEDED,
                            null,
                            null,
                            durationMs
                        );
                        Files.deleteIfExists(memoryFile);
                        continue;
                    }
                    String stdout;
                    try {
                        stdout = getOutput(outputFuture);
                    } catch (OutputLimitExceededException e) {
                        updateSubmissionTestResults(
                            submissionTest,
                            SubmissionTest.Status.OUTPUT_LIMIT_EXCEEDED,
                            null,
                            null,
                            durationMs
                        );

                        continue;
                    }
                    int runExitCode = runProcess.exitValue();
                    if(runExitCode != 0) {
                        markSubmissionTestAsFailed(submissionTest, SubmissionTest.Status.RUNTIME_ERROR);
                        Files.deleteIfExists(memoryFile);
                        continue;
                    }

                    Long peakMemoryKiB = null;
                    try {
                        peakMemoryKiB = Long.parseLong(Files.readString(memoryFile).trim());
                        Files.deleteIfExists(memoryFile);
                    } catch (IOException | NumberFormatException e) {
                        updateSubmissionTestResults(submissionTest, SubmissionTest.Status.SYSTEM_ERROR, null, null, null);
                        continue;
                    }

                    if (test.getMemoryLimitKib() != null
                            && peakMemoryKiB >= test.getMemoryLimitKib()) {
                        updateSubmissionTestResults(
                            submissionTest,
                            SubmissionTest.Status.MEMORY_LIMIT_EXCEEDED,
                            null,
                            peakMemoryKiB,
                            durationMs
                        );
                        continue;
                    }
                    
                    boolean passed = stdout.trim().equals(test.getExpectedOutput().trim());

                    if(passed) {
                        passedTestCases += 1;
                        updateSubmissionTestResults(submissionTest, SubmissionTest.Status.PASSED, stdout, peakMemoryKiB, durationMs);
                        continue;
                    }
                    else {
                        markSubmissionTestAsFailed(submissionTest, SubmissionTest.Status.WRONG_ANSWER);
                        continue;
                    }

                } catch (InterruptedException e) {
                   setSubmission(
                        submission,
                        Submission.Status.SYSTEM_ERROR,
                        null
                    );

                    setGradingRun(
                        gradingRun,
                        GradingRun.Status.FAILED,
                        null,
                        null,
                        LocalDateTime.now()
                    );

                    markSubmissionTestAsFailed(
                        submissionTest,
                        SubmissionTest.Status.SYSTEM_ERROR
                    );

                    Thread.currentThread().interrupt();
                    return;
            }
            }
            setSubmissionAndGradingRunScore(gradingRun, totalTestCases, passedTestCases);
            setSubmissionStatusFromTests(gradingRun);
            
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

    private void setSubmission(Submission submission, Submission.Status status, Double submissionGrade) {
        if (submission == null) {
            return;
        }
        if (status != null) {
            submission.setStatus(status);
        }
        if (submissionGrade != null) {
            submission.setGrade(submissionGrade);
        }
        submissionRepository.save(submission);
    }

    private void setGradingRun(GradingRun gradingRun, GradingRun.Status status, Double gradingRunGrade, LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (gradingRun == null) {
            return;
        }
        if (status != null) {
            gradingRun.setStatus(status);
        }
        if (gradingRunGrade != null) {
            gradingRun.setGrade(gradingRunGrade);
        }
        if (startedAt != null) {
            gradingRun.setStartedAt(startedAt);
        }
        if (finishedAt != null) {
            gradingRun.setFinishedAt(finishedAt);
        }
        gradingRunRepository.save(gradingRun);
    }

   private Assignment findAssignment(Submission submission) {
        Assignment assignment = 
            assignmentRepository
            .findByIdWithTests(submission.getAssignment().getId())
            .orElseThrow(() -> new IllegalStateException("Assignment not found."));
            return assignment;
   }

   private void validateAssignment(Assignment assignment, Submission submission) {
        int testsCount = assignment.getTests().size();
        if (testsCount == 0) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
            throw new IllegalStateException("Assignment has no test cases.");
        }
   }

   private void stopProcess(Process p) {
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }


   private void addSubmissionTests(
    GradingRun gradingRun, 
    List<Test> tests
    ) {
    for (Test test : tests) {
        SubmissionTest submissionTest = new SubmissionTest(
            test,
            gradingRun,
            SubmissionTest.Status.QUEUED,
            null,
            null,
            null
        );
        submissionTestRepository.save(submissionTest);
    }
   }
   private void markSubmissionTestsAsFailed(
    GradingRun gradingRun
    ) {
    List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findByGradingRun_Submission_IdAndGradingRun_Id(gradingRun.getSubmission().getId(), gradingRun.getId());
    if(submissionTests.isEmpty()) {
        throw new IllegalStateException("Submission tests not found.");
    }
    for (com.example.grader_worker.submissiontest.SubmissionTest submissionTest : submissionTests) {
        submissionTest.setStatus(SubmissionTest.Status.NOT_RUN);
    }
    submissionTestRepository.saveAll(submissionTests);
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

   private void markSubmissionTestAsFailed(
        SubmissionTest submissionTest,
        SubmissionTest.Status status) {
        submissionTest.setStatus(status);
        submissionTestRepository.save(submissionTest);
    }

    private Submission.Status map(SubmissionTest.Status status) {
        return switch (status) {
            case PASSED -> Submission.Status.ACCEPTED;
            case WRONG_ANSWER -> Submission.Status.WRONG_ANSWER;
            case COMPILE_ERROR -> Submission.Status.COMPILE_ERROR;
            case TIME_LIMIT_EXCEEDED -> Submission.Status.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> Submission.Status.MEMORY_LIMIT_EXCEEDED;
            case RUNTIME_ERROR -> Submission.Status.RUNTIME_ERROR;
            case SYSTEM_ERROR -> Submission.Status.SYSTEM_ERROR;
            case QUEUED -> Submission.Status.QUEUED;
            case RUNNING -> Submission.Status.RUNNING;
            case NOT_RUN -> Submission.Status.NOT_RUN;
            case OUTPUT_LIMIT_EXCEEDED -> Submission.Status.OUTPUT_LIMIT_EXCEEDED;
            default -> null;
        };
    }
    private void setSubmissionAndGradingRunScore(GradingRun gradingRun, int totalTestCases, int passedTestCases) {
        if (totalTestCases == 0) {
            setSubmission(gradingRun.getSubmission(), null, 0.0D);
            throw new IllegalStateException("Total test cases must be greater than zero.");
        }
        double score = (passedTestCases * 10.0) / totalTestCases;
        double totalScore = Math.round(score * 10) / 10.0;
        setSubmission(gradingRun.getSubmission(), null, totalScore);
        setGradingRun(gradingRun, null, totalScore, null, null);
    }
    private void setSubmissionStatusFromTests(GradingRun gradingRun) {
        List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findByGradingRun_Submission_IdAndGradingRun_Id(
                gradingRun.getSubmission().getId(),
                gradingRun.getId()
            );
        boolean allTestsPassed = true;
        boolean isFirstFailedTest = true;
        for(SubmissionTest submissionTest : submissionTests) {
            if(submissionTest.getStatus() != SubmissionTest.Status.PASSED && isFirstFailedTest) {
                allTestsPassed = false;
                isFirstFailedTest = false;
                setSubmission(gradingRun.getSubmission(), map(submissionTest.getStatus()), null);
            }
        }
        if(allTestsPassed) {
            setSubmission(gradingRun.getSubmission(), Submission.Status.ACCEPTED, null);
        }
        setGradingRun(gradingRun, GradingRun.Status.COMPLETED, null, null, LocalDateTime.now());
    }
    private void updateSubmissionTestResults(
        SubmissionTest submissionTest,
        SubmissionTest.Status status,
        String actualOutput,
        Long peakMemoryKib,
        Long durationMs) {
        submissionTest.setStatus(status);
        submissionTest.setActualOutput(actualOutput);
        submissionTest.setMemoryUsedKib(peakMemoryKib);
        submissionTest.setExecutionTimeMs(durationMs);
        submissionTestRepository.save(submissionTest);
    }
}
