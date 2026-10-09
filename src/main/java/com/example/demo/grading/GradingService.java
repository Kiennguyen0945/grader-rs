package com.example.demo.grading;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.example.demo.submission.Submission;



@Service
public class GradingService {
   public void grade(Submission submission) throws IOException {
        Path tempDir = Files.createTempDirectory("runner-");
        try {
            Path mainJava = tempDir.resolve("Main.java");
            Files.writeString(mainJava, submission.getSourceCode());
            submission.setStatus(Submission.Status.RUNNING);
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", mainJava.toAbsolutePath().toString());
            Process compileProcess = compileBuilder.start();
            try {
                int compileExitCode = compileProcess.waitFor();
                if (compileExitCode != 0) {
                    submission.setStatus(Submission.Status.COMPILE_ERROR);
                    return;
                }
            } catch (InterruptedException e) {
                submission.setStatus(Submission.Status.SYSTEM_ERROR);
                Thread.currentThread().interrupt();
                return;
            }
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tempDir.toAbsolutePath().toString(), "Main");
            Process runProcess = runBuilder.start();
            try {
                
                int runExitCode = runProcess.waitFor();
                String stdout;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                    stdout = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
                if (runExitCode != 0) {
                    submission.setStatus(Submission.Status.RUNTIME_ERROR);
                    return;
                }
                String expectedResult = "Hello World!";
                boolean passed = stdout.trim().equals(expectedResult);
                if(passed) {
                    submission.setStatus(Submission.Status.ACCEPTED);
                    submission.setGrade(10D);
                    return;
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