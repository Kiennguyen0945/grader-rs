package com.example.demo.test;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.CustomUserPrincipal;
import com.example.demo.user.User;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class TestController {
    private final TestService testService;
    public TestController(TestService testService) {
        this.testService = testService;
    }

    /**
     * Lecturers see every test case (public + private). Anyone else only sees the
     * public sample tests, so hidden grading inputs are never exposed to students.
     */
    @GetMapping("/{assignmentId}/tests")
    public List<TestService.TestResponseDto> getTestsByAssignmentId(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        boolean isLecturer = principal != null && principal.hasRole(User.Role.LECTURER);
        return testService.getTestsByAssignmentId(assignmentId)
                .stream()
                .filter(test -> isLecturer || test.isPublic())
                .toList();
    }

    @GetMapping("/{assignmentId}/tests/stats")
    public TestService.TestStatsDto getTestStats(@PathVariable Long assignmentId) {
        return testService.getStats(assignmentId);
    }

    @PostMapping("/{assignmentId}/tests")
    public TestService.TestResponseDto createTestByAssignmentId(
            @PathVariable Long assignmentId,
            @RequestBody CreateTestRequest request) {
        return testService.createTestByAssignmentId(
                assignmentId,
                request.name(),
                request.input(),
                request.expectedOutput(),
                request.isPublic(),
                request.timeLimitMs(),
                request.memoryLimitKib());
    }

    @PatchMapping("/{assignmentId}/tests/{testId}")
    public TestService.TestResponseDto updateTest(
            @PathVariable Long assignmentId,
            @PathVariable Long testId,
            @RequestBody UpdateTestRequest request) {
        return testService.updateTest(
                assignmentId,
                testId,
                request.name(),
                request.input(),
                request.expectedOutput(),
                request.isPublic(),
                request.timeLimitMs(),
                request.memoryLimitKib());
    }

    @DeleteMapping("/{assignmentId}/tests/{testId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTest(@PathVariable Long assignmentId, @PathVariable Long testId) {
        testService.deleteTest(assignmentId, testId);
    }

    public record CreateTestRequest(String name, String input, String expectedOutput, boolean isPublic, Long timeLimitMs, Long memoryLimitKib) {
    }

    public record UpdateTestRequest(String name, String input, String expectedOutput, Boolean isPublic, Long timeLimitMs, Long memoryLimitKib) {
    }

}
