package com.example.demo.submission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.Assignment;
import com.example.demo.assignment.AssignmentRepository;
import com.example.demo.rabbitmq.SubmissionProducer;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import jakarta.transaction.Transactional;

import java.util.List;

@Service 
public class SubmissionService {
    @Autowired 
    private SubmissionProducer producer;

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    public SubmissionService(
        SubmissionRepository submissionRepository,
        AssignmentRepository assignmentRepository,
        UserRepository userRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
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
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Source code is required."
            );
        }
        long previousAttempts =
            submissionRepository.countByAssignmentIdAndUserId(assignmentId, userId);

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
        producer.send(String.valueOf(savedSubmission.getId()));
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
    public record SubmissionResponseDto(Long submissionId, 
        Long assignmentId, 
        Long userId,
        String sourceCode, 
        Submission.Status status,
        Double grade,
        Integer attemptNumber
    ) {
        public static SubmissionResponseDto fromEntity(Submission submission) {
            return new SubmissionResponseDto(submission.getId(),
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
