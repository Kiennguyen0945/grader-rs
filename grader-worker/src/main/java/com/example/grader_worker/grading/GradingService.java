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
import com.example.grader_worker.test.TestRepository;
import com.example.grader_worker.config.KubernetesJobService;
import com.example.grader_worker.model.GradingJobRequest;
import com.example.grader_worker.model.GradingResultResponse;
import com.example.grader_worker.model.TestRequest;
import com.example.grader_worker.model.SubmissionRequest;
import com.example.grader_worker.model.SubmissionTestResponse;
import com.example.grader_worker.model.GradingRunRequest;



import tools.jackson.databind.ObjectMapper;



@Service

public class GradingService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionTestRepository submissionTestRepository;
    private final SubmissionRepository submissionRepository;
    private final GradingRunRepository gradingRunRepository;
    private final TestRepository testRepository;
    private final KubernetesJobService kubernetesJobService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    public GradingService(
            AssignmentRepository assignmentRepository, 
            SubmissionTestRepository submissionTestRepository, 
            SubmissionRepository submissionRepository, 
            GradingRunRepository gradingRunRepository,
            TestRepository testRepository, 
            KubernetesJobService kubernetesJobService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionTestRepository = submissionTestRepository;
        this.submissionRepository = submissionRepository;
        this.gradingRunRepository = gradingRunRepository;
        this.testRepository = testRepository;
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
        SubmissionRequest submissionRequest = new SubmissionRequest(submission.getId(), submission.getSourceCode(), Submission.Status.RUNNING.name(), null);
        LocalDateTime createdAt = LocalDateTime.now();
        GradingRunRequest gradingRunRequest = new GradingRunRequest(gradingRun.getId(), GradingRun.Status.RUNNING.name(), null, createdAt, null, null);
        List<Test> tests = assignment.getTests();
        List<TestRequest> testRequests = addTests(gradingRun, tests);

        GradingJobRequest gradingJobRequest = new GradingJobRequest(submissionRequest, gradingRunRequest, testRequests);

        String json = objectMapper.writeValueAsString(gradingJobRequest);

        GradingResultResponse response = kubernetesJobService.createJob(submission.getId(), gradingRun.getId(), json);

        setSubmission(submission, toSubmissionStatus(response.submission().status()), response.submission().submissionGrade());
        System.out.println("Updated Submission");
        setGradingRun(gradingRun, toGradingRunStatus(response.gradingRun().status()), response.gradingRun().gradingRunGrade(), null, response.gradingRun().startedAt(), response.gradingRun().finishedAt());
        List<SubmissionTestResponse> submissionTestResponses = response.submissionTestResponses();
        for(int i = 0; i < submissionTestResponses.size(); i++) {
            SubmissionTestResponse current = submissionTestResponses.get(i);
            Long testId = current.testId();
            Test test = testRepository.findById(testId)
            .orElseThrow(() -> new IllegalStateException("Test not found with id: " + testId));
            SubmissionTest submissionTest = new SubmissionTest(test, gradingRun, toSubmissionTestStatus(current.status()), current.actualOutput(), current.memoryUsedKib(), current.executionTimeMs());
            submissionTestRepository.save(submissionTest);
        }

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

    private Submission.Status toSubmissionStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return Submission.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback if the string doesn't match any enum constant
            System.out.print(e);
            return Submission.Status.SYSTEM_ERROR; // or return null;
        }
    }

    private GradingRun.Status toGradingRunStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return GradingRun.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback if the string doesn't match any enum constant
            System.out.print(e);
            return GradingRun.Status.FAILED; // or return null;
        }
    }

    private SubmissionTest.Status toSubmissionTestStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return SubmissionTest.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback if the string doesn't match any enum constant
            return SubmissionTest.Status.SYSTEM_ERROR; // or return null;
        }
    }
}
 

