package com.example.demo.assignment;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentResourceController {
    
    private final AssignmentService assignmentService;
    public AssignmentResourceController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }
    @GetMapping("/{assignmentId}")
    public AssignmentService.AssignmentResponseDto fetchSingleAssignmentResponseDto(@PathVariable Long assignmentId) {
        return assignmentService.fetchSingleAssignment(assignmentId);
    }
    @PatchMapping("/{assignmentId}")
    public AssignmentService.AssignmentResponseDto partialUpdateAssignment(@PathVariable Long assignmentId, @RequestBody UpdateAssignmentRequest request) {
        return assignmentService.partialUpdateAssignment(assignmentId, request.code(), request.title(), request.assignmentStatus());
    }
    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(assignmentId);
    }

    public record UpdateAssignmentRequest(String code, String title, Assignment.AssignmentStatus assignmentStatus) {
    }
}
