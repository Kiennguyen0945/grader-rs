package com.example.demo.submissiontest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.gradingrun.GradingRun;
import com.example.demo.gradingrun.GradingRunRepository;

@Service 
public class SubmissionTestService {
    private final SubmissionTestRepository submissionTestRepository;
    private final GradingRunRepository gradingRunRepository;
    public SubmissionTestService(SubmissionTestRepository submissionTestRepository, GradingRunRepository gradingRunRepository) {
        this.submissionTestRepository = submissionTestRepository;
        this.gradingRunRepository = gradingRunRepository;
    }
    public List<SubmissionTestResponseDto> getSubmissionTestsBySubmissionIdAndGradingId(Long submisisonId, Long gradingId){
        GradingRun gradingRun = gradingRunRepository.findByIdWithSubmission(gradingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GradingRun not found."));
        List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findByGradingRun_Submission_IdAndGradingRun_Id(gradingRun.getSubmission().getId(), gradingId);
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
        Long gradingRunId,
        Long testId,
        SubmissionTest.Status status,
        String actualOutput,
        Long memoryUsedKib,
        Long executionTimeMs
    ) {
        public static SubmissionTestResponseDto fromEntity(SubmissionTest submissionTest) {
            return new SubmissionTestResponseDto(
                submissionTest.getGradingRun().getId(),
                submissionTest.getTest().getId(), 
                submissionTest.getStatus(), 
                submissionTest.getActualOutput(), 
                submissionTest.getMemoryUsedKib(), 
                submissionTest.getExecutionTimeMs()
            );
        }
    }
}