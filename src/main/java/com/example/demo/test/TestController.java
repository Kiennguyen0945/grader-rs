package com.example.demo.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class TestController {
    private final TestService testService;
    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/{assignmentId}/tests")
    public List<TestService.TestResponseDto> getTestsByAssignmentId(@PathVariable Long assignmentId) {
        return testService.getTestsByAssignmentId(assignmentId);
    }

    @PostMapping("/{assignmentId}/tests")
    public TestService.TestResponseDto createTestByAssignmentId(@PathVariable Long assignmentId, @RequestBody CreateTestRequest request) {
        return testService.createTestByAssignmentId(assignmentId, request.name(), request.input(), request.expectedOutput(), request.isPublic());
    }

    public record CreateTestRequest(String name, String input, String expectedOutput, boolean isPublic) {
    }


}
