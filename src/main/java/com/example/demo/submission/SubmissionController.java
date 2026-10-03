package com.example.demo.submission;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/assignments")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/{assignmentId}/submissions")
    public SubmissionService.SubmissionResponseDto createSubmission(
            @PathVariable Long assignmentId,
            @Valid @RequestBody CreateSubmissionRequest request
    ) {
        return submissionService.createSubmissionByAssignmentIdAndUserId(
            assignmentId,
            request.userId(),
            request.sourceCode()
        );
    }

    @GetMapping("/{assignmentId}/submissions")
    public List<SubmissionService.SubmissionResponseDto> getSubmissions(
            @PathVariable Long assignmentId,
            @RequestParam Long userId
    ) {
        return submissionService.getSubmissionsByAssignmentIdAndUserId(
            assignmentId,
            userId
        );
    }

    public record CreateSubmissionRequest(
            @NotNull Long userId,
            @NotBlank String sourceCode
    ) {
    }
}