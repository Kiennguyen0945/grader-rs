package com.example.demo.submissiontest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class SubmissionTestService {
    private final SubmissionTestRepository submissionTestRepository;
    public SubmissionTestService(SubmissionTestRepository submissionTestRepository) {
        this.submissionTestRepository = submissionTestRepository;
    }
    public List<SubmissionTestResponseDto> getSubmissionTestsBySubmissionIdAndGradingId(Long submisisonId, Long gradingId){
        List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findBySubmission_IdAndGradingRunId(submisisonId, gradingId);
        if (submissionTests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissiontests not found");
        }
        return submissionTests.stream().map(SubmissionTestResponseDto::fromEntity).toList();
    }
    public List<SubmissionTestResponseDto> getSubmissionTestsBySubmissionId(Long submisisonId) {
        List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findBySubmission_Id(submisisonId);
        if (submissionTests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissiontests not found");
        }
        return submissionTests.stream().map(SubmissionTestResponseDto::fromEntity).toList();
    }

    public record SubmissionTestResponseDto(
        Long submissionId,
        Long testId,
        SubmissionTest.Status status,
        String actualOutput,
        Long memoryUsedKib,
        Long executionTimeMs,
        Long gradingRunId
    ) {
        public static SubmissionTestResponseDto fromEntity(SubmissionTest submissionTest) {
            return new SubmissionTestResponseDto(
                submissionTest.getSubmission().getId(),
                submissionTest.getTest().getId(), 
                submissionTest.getStatus(), 
                submissionTest.getActualOutput(), 
                submissionTest.getMemoryUsedKib(), 
                submissionTest.getExecutionTimeMs(),
                submissionTest.getGradingRunId()
            );
        }
    }
}