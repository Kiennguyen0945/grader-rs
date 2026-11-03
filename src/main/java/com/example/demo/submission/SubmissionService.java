package com.example.demo.submission;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.Assignment;
import com.example.demo.assignment.AssignmentRepository;
import com.example.demo.courseUser.CourseUserRepository;
import com.example.demo.gradingrun.GradingRun;
import com.example.demo.gradingrun.GradingRunRepository;
import com.example.demo.rabbitmq.SubmissionProducer;
import com.example.demo.submissiontest.SubmissionTest;
import com.example.demo.submissiontest.SubmissionTestRepository;
import com.example.demo.test.Test;
import com.example.demo.test.TestRepository;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import jakarta.transaction.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Service 
public class SubmissionService {

    private final SubmissionProducer producer;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final CourseUserRepository courseUserRepository;
    private final GradingRunRepository gradingRunRepository;
    private final SubmissionTestRepository submissionTestRepository;
    private final TestRepository testRepository;

    public SubmissionService(
        SubmissionRepository submissionRepository,
        AssignmentRepository assignmentRepository,
        UserRepository userRepository,
        CourseUserRepository courseUserRepository,
        GradingRunRepository gradingRunRepository,
        SubmissionTestRepository submissionTestRepository,
        TestRepository testRepository,
        SubmissionProducer producer
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.courseUserRepository = courseUserRepository;
        this.gradingRunRepository = gradingRunRepository;
        this.submissionTestRepository = submissionTestRepository;
        this.testRepository = testRepository;
        this.producer = producer;
    }

    @Transactional
    public SubmissionResponseDto createSubmissionByAssignmentIdAndUserId(
            Long assignmentId,
            Long userId,
            String sourceCode
    ) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Assignment Not Found."
            ));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User Not Found."
            ));

        if (user.getRole() == User.Role.STUDENT) {
            boolean isEnrolled = courseUserRepository.existsByCourseIdAndUserId(assignment.getCourse().getId(), userId);
            if (!isEnrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not enrolled in this course.");
            }

            if (assignment.getAssignmentStatus() != Assignment.AssignmentStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not open for submission.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (assignment.getOpeningTime() != null && now.isBefore(assignment.getOpeningTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment submission period has not started yet.");
            }
            if (assignment.getClosingTime() != null && now.isAfter(assignment.getClosingTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment deadline has passed.");
            }
        }

        if (sourceCode == null || sourceCode.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Source code is required."
            );
        }

        long previousAttempts =
            submissionRepository.countByAssignmentIdAndUserId(assignmentId, userId);

        int maxLimit = assignment.getSubmissionLimit() != null ? assignment.getSubmissionLimit() : 3;
        if (previousAttempts >= maxLimit) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Submission attempt limit of " + maxLimit + " exceeded."
            );
        }

        int attemptNumber = (int) previousAttempts + 1;

        Submission submission = new Submission(
            assignment,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            attemptNumber
        );
        Submission savedSubmission = submissionRepository.save(submission);
        GradingRun gradingRun = new GradingRun(savedSubmission, null, null);
        GradingRun savedGradingRun = gradingRunRepository.save(gradingRun);
        producer.send(savedSubmission.getId(), savedGradingRun.getId());
        return SubmissionResponseDto.fromEntity(
            savedSubmission
        );
    }

    public List<SubmissionResponseDto> getSubmissionsByAssignmentIdAndUserId(
        Long assignmentId,
        Long userId
    ) {
        return submissionRepository.findByAssignmentIdAndUserId(
            assignmentId,
            userId
        )
        .stream()
        .map(SubmissionResponseDto::fromEntity)
        .toList();
    }

    public SubmissionDetailResponseDto getSubmissionDetail(Long submissionId, Long userId, boolean isLecturer) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission Not Found."));

        if (!isLecturer && !submission.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to submission.");
        }

        Long latestGradingRunId = submissionTestRepository.findMaxGradingRunIdBySubmissionId(submissionId);

        List<SubmissionTest> runTests = latestGradingRunId != null
            ? submissionTestRepository.findByGradingRun_Submission_IdAndGradingRun_Id(submissionId, latestGradingRunId)
            : List.of();

        int totalTests = testRepository.countByAssignmentId(submission.getAssignment().getId());
        int passedTests = 0;
        for (SubmissionTest st : runTests) {
            if (st.getStatus() == SubmissionTest.Status.PASSED) {
                passedTests++;
            }
        }

        List<SubmissionTestResultDto> sampleTestResults = runTests.stream()
            .filter(st -> st.getTest().isPublic())
            .map(SubmissionTestResultDto::fromEntity)
            .toList();

        return new SubmissionDetailResponseDto(
            submission.getId(),
            submission.getAssignment().getId(),
            submission.getUser().getId(),
            submission.getSourceCode(),
            submission.getStatus(),
            submission.getGrade(),
            submission.getAttemptNumber(),
            passedTests,
            totalTests,
            sampleTestResults
        );
    }

    public record SubmissionTestResultDto(
        Long testId,
        String testName,
        int displayOrder,
        SubmissionTest.Status status,
        String input,
        String expectedOutput,
        String actualOutput,
        Long executionTimeMs,
        Long memoryUsedKib
    ) {
        public static SubmissionTestResultDto fromEntity(SubmissionTest st) {
            Test t = st.getTest();
            return new SubmissionTestResultDto(
                t.getId(),
                t.getName(),
                t.getDisplayOrder(),
                st.getStatus(),
                t.getInput(),
                t.getExpectedOutput(),
                st.getActualOutput(),
                st.getExecutionTimeMs(),
                st.getMemoryUsedKib()
            );
        }
    }

    public record SubmissionDetailResponseDto(
        Long submissionId,
        Long assignmentId,
        Long userId,
        String sourceCode,
        Submission.Status status,
        Double grade,
        Integer attemptNumber,
        int passedTests,
        int totalTests,
        List<SubmissionTestResultDto> sampleTestResults
    ) {}

    public record SubmissionGradingMessage(Long submissionId, Long gradingRunId) implements Serializable {
    }

    public record SubmissionResponseDto(
        Long submissionId, 
        Long assignmentId, 
        Long userId,
        String sourceCode, 
        Submission.Status status,
        Double grade,
        Integer attemptNumber
    ) {
        public static SubmissionResponseDto fromEntity(Submission submission) {
            return new SubmissionResponseDto(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getUser().getId(),
                submission.getSourceCode(),
                submission.getStatus(),
                submission.getGrade(),
                submission.getAttemptNumber()
            );
        }
    }
}

