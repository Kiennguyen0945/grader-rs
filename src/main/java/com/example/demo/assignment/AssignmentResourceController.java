package com.example.demo.assignment;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.demo.user.CustomUserPrincipal;
import com.example.demo.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentResourceController {

    private final AssignmentService assignmentService;

    public AssignmentResourceController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/{assignmentId}")
    public AssignmentService.AssignmentDetailResponseDto fetchSingleAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        boolean isLecturer = principal != null && principal.hasRole(User.Role.LECTURER);
        Long userId = principal != null ? principal.getId() : null;
        return assignmentService.fetchAssignmentWorkspaceDetail(assignmentId, userId, isLecturer);
    }


    @PatchMapping("/{assignmentId}")
    public AssignmentService.AssignmentResponseDto partialUpdateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request) {

        return assignmentService.partialUpdateAssignment(
                assignmentId,
                request.code(),
                request.title(),
                request.assignmentStatus(),
                request.description(),
                request.submissionLimit(),
                request.openingTime(),
                request.closingTime(),
                request.allowedLanguages()
        );
    }

    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(assignmentId);
    }

    public record UpdateAssignmentRequest(
            @Size(max = 50)
            String code,

            @Size(max = 200)
            String title,

            Assignment.AssignmentStatus assignmentStatus,

            String description,

            Integer submissionLimit,

            LocalDateTime openingTime,

            LocalDateTime closingTime,

            Set<Assignment.ProgrammingLanguage> allowedLanguages
    ) {
    }
}