package com.example.demo.submission;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.CustomUserPrincipal;
import com.example.demo.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);


    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public SubmissionService.SubmissionResponseDto createSubmission(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateSubmissionRequest request
    ) {
        log.info("POST reached: assignmentId={}, userId={}, authorities={}",
                assignmentId,
                principal.getId(),
                principal.getAuthorities());
        return submissionService.createSubmissionByAssignmentIdAndUserId(
            assignmentId,
            principal.getId(),
            request.sourceCode()
        );
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public List<SubmissionService.SubmissionResponseDto> getSubmissions(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        
        return submissionService.getSubmissionsByAssignmentIdAndUserId(
            assignmentId,
            principal.getId()
        );
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionService.SubmissionDetailResponseDto getSubmissionById(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        boolean isLecturer = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + User.Role.LECTURER.name()));
        Long userId = principal != null ? principal.getId() : null;
        return submissionService.getSubmissionDetail(submissionId, userId, isLecturer);
    }

    public record CreateSubmissionRequest(
            @NotBlank String sourceCode
    ) {
    }
}
