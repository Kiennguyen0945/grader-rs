package com.example.demo.grading;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.Assignment;
import com.example.demo.assignment.AssignmentRepository;
import com.example.demo.submission.Submission;
import com.example.demo.test.Test;

import jakarta.transaction.Transactional;



@Service

public class GradingService {
    private final AssignmentRepository assignmentRepository;
    public GradingService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }
    @Transactional 
   public void grade(Submission submission) throws IOException {
        Assignment assignment = 
            assignmentRepository
            .findById(submission.getAssignment().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        int testsCount = assignment.getTests().size();
        if (testsCount == 0) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
            throw new IllegalStateException("Assignment has no test cases.");
        }
        Path tempDir = Files.createTempDirectory("runner-");
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, submission.getSourceCode());
            submission.setStatus(Submission.Status.RUNNING);
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            Process compileProcess = compileBuilder.start();
            try {
                boolean finished = compileProcess.waitFor(2, TimeUnit.SECONDS);
                if (!finished) {
                    compileProcess.destroy();
                    if(compileProcess.isAlive()) {
                        compileProcess.destroyForcibly();
                    }
                    submission.setStatus(Submission.Status.TIME_LIMIT_EXCEEDED);
                    submission.setGrade(0D);
                    throw new RuntimeException("Process timed out after 2 seconds.");
                }
                String stderr;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))) {
                        stderr = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                    }
                int compileExitCode = compileProcess.exitValue();
                if (compileExitCode != 0) {
                    submission.setStatus(Submission.Status.COMPILE_ERROR);
                    submission.setGrade(0D);
                    throw new IllegalStateException(stderr);
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
                    
                    boolean finished = runProcess.waitFor(2, TimeUnit.SECONDS);
                    if (!finished) {
                        runProcess.destroy();
                        if(runProcess.isAlive()) {
                            runProcess.destroyForcibly();
                        }
                        submission.setStatus(Submission.Status.TIME_LIMIT_EXCEEDED);
                        submission.setGrade(0D);
                        throw new RuntimeException("Process timed out after 2 seconds.");
                    }
                    int runExitCode = runProcess.exitValue();
                    if(runExitCode != 0) {
                        submission.setStatus(Submission.Status.RUNTIME_ERROR);
                        submission.setGrade(0D);
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
                        submission.setStatus(Submission.Status.WRONG_ANSWER);
                        submission.setGrade(0D);
                        return;
                    }

                } catch (InterruptedException e) {
                    submission.setStatus(Submission.Status.SYSTEM_ERROR);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (testsPassedCount == assignment.getTests().size()) {
                submission.setStatus(Submission.Status.ACCEPTED);
                submission.setGrade(10D);
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

}