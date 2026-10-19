package com.example.grader_worker.grading;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.test.Test;

import jakarta.transaction.Transactional;



@Service

public class GradingService {
    private static final long PROCESS_TIMEOUT_SECONDS = 2;
    private static final double FULL_GRADE = 10D; 
    private final AssignmentRepository assignmentRepository;
    public GradingService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }
    @Transactional 
   public void grade(Submission submission) throws IOException {
        Assignment assignment = findAssignment(submission);
        validateAssignment(assignment, submission);
        Path tempDir = Files.createTempDirectory("runner-");
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, submission.getSourceCode());
            submission.setStatus(Submission.Status.RUNNING);
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            Process compileProcess = compileBuilder.start();
            try {
                boolean finished = compileProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    stopProcess(compileProcess);
                    markAsFailed(submission, Submission.Status.TIME_LIMIT_EXCEEDED);
                    throw new RuntimeException("Process timed out after 2 seconds.");
                }
                int compileExitCode = compileProcess.exitValue();
                if (compileExitCode != 0) {
                    markAsFailed(submission, Submission.Status.COMPILE_ERROR);
                    return;
                }
            } catch (InterruptedException e) {
                submission.setStatus(Submission.Status.SYSTEM_ERROR);
                Thread.currentThread().interrupt();
                return;
            }
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tempDir.toAbsolutePath().toString(), "Main");
            int testsPassedCount = 0;
            for(Test test : assignment.getTests()) {
                Process runProcess = runBuilder.start();
                try {
                   
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
                        if (test.getInput() != null) {
                            writer.write(test.getInput());
                        }
                    }
                    
                    boolean finished = runProcess.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!finished) {
                        stopProcess(runProcess);
                        markAsFailed(submission, Submission.Status.TIME_LIMIT_EXCEEDED);
                        return;
                    }
                    int runExitCode = runProcess.exitValue();
                    if(runExitCode != 0) {
                        markAsFailed(submission, Submission.Status.RUNTIME_ERROR);
                        return;
                    }
                    String stdout;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                        stdout = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                    }
                    boolean passed = stdout.trim().equals(test.getExpectedOutput());
                    if(passed) {
                        testsPassedCount += 1;
                    }
                    else {
                        markAsFailed(submission, Submission.Status.WRONG_ANSWER);
                        return;
                    }

                } catch (InterruptedException e) {
                    submission.setStatus(Submission.Status.SYSTEM_ERROR);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (testsPassedCount == assignment.getTests().size()) {
                markAsAccepted(submission);
                return;
            }
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

   private void stopProcess(Process process) {
        process.destroy();
        if(process.isAlive()) {
            process.destroyForcibly();
        }
   }

   private void markAsFailed(Submission submission, Submission.Status status) {
    submission.setStatus(status);
    submission.setGrade(0D);
   }
   private void markAsAccepted(Submission submission) {
     submission.setStatus(Submission.Status.ACCEPTED);
     submission.setGrade(FULL_GRADE);
   }

}
