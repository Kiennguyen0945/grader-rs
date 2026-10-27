package com.example.grader_worker.submissiontest;

import org.springframework.stereotype.Service;

import java.util.List;

@Service 
public class SubmissionTestService {
    private final SubmissionTestRepository submissionTestRepository;
    public SubmissionTestService(SubmissionTestRepository submissionTestRepository) {
        this.submissionTestRepository = submissionTestRepository;
    }
    public List<SubmissionTest> getSubmissionTestsBySubmissionIdAndGradingId(Long submisisonId, Long gradingId){
        List<SubmissionTest> submissionTests = 
            submissionTestRepository
            .findBySubmission_IdAndGradingRunId(submisisonId, gradingId);
        if(submissionTests.isEmpty()) {
             throw new IllegalStateException("The submission has no test cases");
        }
        return submissionTests;
    }
    
}
