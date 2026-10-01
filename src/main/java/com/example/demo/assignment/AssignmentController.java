package com.example.demo.assignment;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.demo.course.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/courses")
public class AssignmentController {
    private final AssignmentService assignmentService;
    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/{courseId}/assignments")
    public List<AssignmentService.AssignmentResponseDto> getAssignmentsByCourseId(@PathVariable Long courseId) {
        return assignmentService.getAssignmentsByCourseId(courseId);
    }
    
    @PostMapping("/{courseId}/assignments")
    public AssignmentService.AssignmentResponseDto createAssignmentByCourseId(@PathVariable Long courseId, @RequestBody CreateAssignmentRequest request) {
        return assignmentService.createAssignment(courseId, request.code(), request.title(), request.assignmentStatus());
    }

    public record CreateAssignmentRequest(@NotBlank String code, @NotBlank String title, @NotNull Assignment.AssignmentStatus assignmentStatus) {
    }
    
}
