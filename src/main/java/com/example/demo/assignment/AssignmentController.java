package com.example.demo.assignment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.demo.user.CustomUserPrincipal;
import com.example.demo.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/courses")
public class AssignmentController {
    private final AssignmentService assignmentService;
    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/{courseId}/assignments")
    public List<AssignmentService.AssignmentResponseDto> getAssignmentsByCourseId(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        boolean isLecturer = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + User.Role.LECTURER.name()));
        Long userId = principal != null ? principal.getId() : null;
        return assignmentService.getAssignmentsForCourse(courseId, userId, isLecturer);
    }
    
    @PostMapping("/{courseId}/assignments")
    public AssignmentService.AssignmentResponseDto createAssignmentByCourseId(@PathVariable Long courseId, @Valid @RequestBody CreateAssignmentRequest request) {
            return assignmentService.createAssignment(
            courseId,
            request.userId(),
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


    public record CreateAssignmentRequest(
        @NotNull Long userId,
        @Size(max = 50) @NotBlank String code,
        @NotBlank String title,
        @NotNull Assignment.AssignmentStatus assignmentStatus,
        String description,
        @Min(1) Integer submissionLimit,
        LocalDateTime openingTime,
        LocalDateTime closingTime,
        Set<Assignment.ProgrammingLanguage> allowedLanguages
    ) {}
    
}
