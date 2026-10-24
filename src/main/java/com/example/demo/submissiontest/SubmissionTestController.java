package com.example.demo.submissiontest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/submissions/")
public class SubmissionTestController {
    private final SubmissionTestService submissionTestService;
    public SubmissionTestController(SubmissionTestService submissionTestService) {
        this.submissionTestService = submissionTestService;
    }

    @GetMapping("/{submissionId}/tests")
    public List<SubmissionTestService.SubmissionTestResponseDto> getSubmissionTests(
        @PathVariable Long submissionId, 
        @RequestParam(name = "gradingId", required = false) Long gradingId) {
        if (gradingId == null) {
            return submissionTestService.getSubmissionTestsBySubmissionId(submissionId);
        }
        return submissionTestService.getSubmissionTestsBySubmissionIdAndGradingId(submissionId, gradingId);
    }
}
