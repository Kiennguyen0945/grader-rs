package com.example.grader_worker.grading;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.gradingrun.GradingRun;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;
import com.example.grader_worker.submissiontest.SubmissionTest;
import com.example.grader_worker.submissiontest.SubmissionTestRepository;
import com.example.grader_worker.test.Test;

import jakarta.transaction.Transactional;


@Service

public class GradingService {
    private static final long PROCESS_TIMEOUT_SECONDS = 4;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionTestRepository submissionTestRepository;
    private final SubmissionRepository submissionRepository;
    public GradingService(AssignmentRepository assignmentRepository, SubmissionTestRepository submissionTestRepository, SubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionTestRepository = submissionTestRepository;
        this.submissionRepository = submissionRepository;
    }
    @Transactional 
   public void grade(Submission submission, GradingRun gradingRun) throws IOException {
        Assignment assignment = findAssignment(submission);
        validateAssignment(assignment, submission);
        Path tempDir = Files.createTempDirectory("runner-");
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, submission.getSourceCode());
            submission.setStatus(Submission.Status.RUNNING);
            Long gradingRunId = addSubmissionTests(submission, assignment.getTests());
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            Process compileProcess = compileBuilder.start();
            try {
                boolean finished = compileProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    stopProcess(compileProcess);
                    setSubmissionStatus(submission, Submission.Status.TIME_LIMIT_EXCEEDED);
                    submission.setGrade(0.0D);
                    submissionRepository.save(submission);
                    markSubmissionTestsAsFailed(submission, gradingRunId);
                    return;
                }
                int compileExitCode = compileProcess.exitValue();
                if (compileExitCode != 0) {
                    setSubmissionStatus(submission, Submission.Status.COMPILE_ERROR);
                    submission.setGrade(0.0D);
                    submissionRepository.save(submission);
                    markSubmissionTestsAsFailed(submission, gradingRunId);
                    return;
                }
            } catch (InterruptedException e) {
                submission.setStatus(Submission.Status.SYSTEM_ERROR);
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
            int passedTestCases = 0;
            int totalTestCases = assignment.getTests().size();
            for(Test test : assignment.getTests()) {
                long start = System.nanoTime();
                Process runProcess = runBuilder.start();
                SubmissionTest submissionTest = 
                    submissionTestRepository
                    .findBySubmission_IdAndTest_IdAndGradingRunId(
                        submission.getId(), 
                        test.getId(), 
                        gradingRunId
                    ).orElseThrow(() -> new IllegalStateException("SubmissionTest not found.")
                );
                try {
                   
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                        if (test.getInput() != null) {
                            writer.write(test.getInput());
                        }
                    }
                    
                    boolean finished = runProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start
                    );
                    
                    if (!finished) {
                        stopProcess(runProcess);
                        markSubmissionTestAsFailed(submissionTest, SubmissionTest.Status.TIME_LIMIT_EXCEEDED);
                        Files.deleteIfExists(memoryFile);
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

                    String stdout = readActualOutput(runProcess); 
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
                    submission.setStatus(Submission.Status.SYSTEM_ERROR);
                    markSubmissionTestAsFailed(submissionTest, SubmissionTest.Status.SYSTEM_ERROR);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            setSubmissionStatusFromTests(submission, gradingRunId);
            setSubmissionScore(submission, totalTestCases, passedTestCases);
           
            
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

   private Assignment findAssignment(Submission submission) {
        Assignment assignment = 
            assignmentRepository
            .findById(submission.getAssignment().getId())
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
   private void setSubmissionStatus(Submission submission, Submission.Status status) {
    submission.setStatus(status);
    submissionRepository.save(submission);
   }

   private Long addSubmissionTests(
    Submission submission, 
    List<Test> tests
    ) {
    Long maxRunId = submissionTestRepository
        .findMaxGradingRunIdBySubmissionId(submission.getId());
    Long nextRunId = maxRunId == null ? 1L : maxRunId + 1L;
    for (Test test : tests) {
        SubmissionTest submissionTest = new SubmissionTest(
            submission,
            test,
            SubmissionTest.Status.QUEUED,
            null,
            null,
            null,
            nextRunId
        );
        submissionTestRepository.save(submissionTest);
    }
    return nextRunId;
   }
   private void markSubmissionTestsAsFailed(
    Submission submission,
    Long gradingRunId
    ) {
    List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findBySubmission_IdAndGradingRunId(submission.getId(), gradingRunId);
    if(submissionTests.isEmpty()) {
        throw new IllegalStateException("Submission tests not found.");
    }
    for (com.example.grader_worker.submissiontest.SubmissionTest submissionTest : submissionTests) {
        submissionTest.setStatus(SubmissionTest.Status.NOT_RUN);
    }
    submissionTestRepository.saveAll(submissionTests);
   }

   private String readActualOutput(Process process) throws IOException {
    String stdout;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        stdout = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        return stdout;
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
            default -> null;
        };
    }
    private void setSubmissionScore(Submission submission, int totalTestCases, int passedTestCases) {
        if (totalTestCases == 0) {
            submission.setGrade(0.0D);
            throw new IllegalStateException("Total test cases must be greater than zero.");
        }
        double score = (passedTestCases * 10.0) / totalTestCases;
        double totalScore = Math.round(score * 10) / 10.0;
        submission.setGrade(totalScore);
        submissionRepository.save(submission);
    }
    private void setSubmissionStatusFromTests(Submission submission, Long gradingId) {
        List<SubmissionTest> submissionTests = submissionTestRepository.findBySubmission_IdAndGradingRunId(submission.getId(), gradingId);
        boolean allTestsPassed = true;
        boolean isFirstFailedTest = true;
        for(SubmissionTest submissionTest : submissionTests) {
            if(submissionTest.getStatus() != SubmissionTest.Status.PASSED && isFirstFailedTest) {
                allTestsPassed = false;
                isFirstFailedTest = false;
                submission.setStatus(map(submissionTest.getStatus()));;
            }
        }
        if(allTestsPassed) {
            submission.setStatus(Submission.Status.ACCEPTED);
        }
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
