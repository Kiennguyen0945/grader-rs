package com.example.grader_worker.grading;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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


import tools.jackson.databind.ObjectMapper;



@Service

public class GradingService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionTestRepository submissionTestRepository;
    private final SubmissionRepository submissionRepository;
    private final GradingRunRepository gradingRunRepository;
    private final KubernetesJobService kubernetesJobService;

    private final ObjectMapper objectMapper = new ObjectMapper();
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
        setSubmission(submission, Submission.Status.RUNNING, null);
        setGradingRun(gradingRun, GradingRun.Status.RUNNING, null,  LocalDateTime.now(), null, null);
        SubmissionRequest submissionRequest = new SubmissionRequest(submission.getId(), submission.getSourceCode(), Submission.Status.RUNNING, null);
        LocalDateTime createdAt = LocalDateTime.now();
        GradingRunRequest gradingRunRequest = new GradingRunRequest(gradingRun.getId(), GradingRun.Status.RUNNING, null, createdAt, null, null);
        List<Test> tests = assignment.getTests();
        List<TestRequest> testRequests = addTests(gradingRun, tests);

        GradingJobRequest gradingJobRequest = new GradingJobRequest(submissionRequest, gradingRunRequest, testRequests);

        String json = objectMapper.writeValueAsString(gradingJobRequest);

        kubernetesJobService.createJob(submission.getId(), gradingRun.getId(), json);
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

    private void setGradingRun(GradingRun gradingRun, GradingRun.Status status, Double gradingRunGrade, LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (gradingRun == null) {
            return;
        }
        if (status != null) {
            gradingRun.setStatus(status);
        }
        if (gradingRunGrade != null) {
            gradingRun.setGrade(gradingRunGrade);
        }
        if (createdAt != null) {
            gradingRun.setCreatedAt(createdAt);
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


  

    private List<TestRequest> addTests(
    GradingRun gradingRun, 
    List<Test> tests
    ) {
    List<TestRequest> testRequests = new ArrayList<>();
    for (Test test : tests) {
        TestRequest testRequest = new TestRequest(
            test.getId(),
            test.getInput(),
            test.getExpectedOutput(),
            null,
            null
        );
        testRequests.add(testRequest);
    }
    return testRequests;
   }

    public record GradingJobRequest(
        SubmissionRequest submission,
        GradingRunRequest gradingRun,
        List<TestRequest> tests
    ) {
    }

    public record TestRequest(
        Long testId,
        String input,
        String expectedOutput,
        Long timeLimitMs,
        Long memoryLimitKib
    ) {}
   
    public record SubmissionRequest(
            Long submissionId, 
            String sourceCode,
            Submission.Status status, 
            Double submissionGrade) {
    }

    public record GradingRunRequest(
            Long gradingRunId, 
            GradingRun.Status status, 
            Double gradingRunGrade, 
            LocalDateTime createdAt,
            LocalDateTime startedAt, 
            LocalDateTime finishedAt) {
    }

 
}
