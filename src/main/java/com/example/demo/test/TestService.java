package com.example.demo.test;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.Assignment;
import com.example.demo.assignment.AssignmentRepository;
import com.example.demo.submission.SubmissionRepository;

import jakarta.transaction.Transactional;

import java.util.List;

@Service
public class TestService {
    private final TestRepository testRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    public TestService(
            TestRepository testRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository) {
        this.testRepository = testRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<TestResponseDto> getTestsByAssignmentId(Long assignmentId) {
        ensureAssignmentExists(assignmentId);
        return testRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(TestResponseDto::fromEntity)
                .toList();
    }

    public TestStatsDto getStats(Long assignmentId) {
        ensureAssignmentExists(assignmentId);
        long total = testRepository.countByAssignmentId(assignmentId);
        long published = testRepository.countByAssignmentIdAndIsPublicTrue(assignmentId);
        return new TestStatsDto(total, published, total - published);
    }

    @Transactional
    public TestResponseDto createTestByAssignmentId(
            Long assignmentId,
            String name,
            String input,
            String expectedOutput,
            boolean isPublic,
            Long timeLimitMs,
            Long memoryLimitKib) {
        Assignment assignment = ensureAssignmentExists(assignmentId);
        if (expectedOutput == null || expectedOutput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected output is required.");
        }
        validateName(assignmentId, name, null);

        int displayOrder = testRepository.findMaxDisplayOrder(assignmentId) + 1;
        Test test = new Test(assignment, name, input, expectedOutput, isPublic, displayOrder, timeLimitMs, memoryLimitKib);
        return TestResponseDto.fromEntity(testRepository.save(test));
    }

    @Transactional
    public TestResponseDto updateTest(
            Long assignmentId,
            Long testId,
            String name,
            String input,
            String expectedOutput,
            Boolean isPublic,
            Long timeLimitMs,
            Long memoryLimitKib) {
        Test test = findTest(assignmentId, testId);
        lockIfHasSubmissions(assignmentId, "Test cases cannot be changed after the assignment has received submissions.");

        if (name != null) {
            validateName(assignmentId, name, testId);
            test.setName(name);
        }
        if (input != null) {
            test.setInput(input);
        }
        if (expectedOutput != null) {
            if (expectedOutput.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected output cannot be empty.");
            }
            test.setExpectedOutput(expectedOutput);
        }
        if (isPublic != null) {
            test.setPublic(isPublic);
        }
        if (timeLimitMs != null) {
            test.setTimeLimitMs(timeLimitMs);
        }
        if (memoryLimitKib != null) {
            test.setMemoryLimitKib(memoryLimitKib);
        }

        return TestResponseDto.fromEntity(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long assignmentId, Long testId) {
        Test test = findTest(assignmentId, testId);
        lockIfHasSubmissions(assignmentId, "Test cases cannot be deleted after the assignment has received submissions.");
        testRepository.delete(test);
    }

    private Assignment ensureAssignmentExists(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
    }

    private Test findTest(Long assignmentId, Long testId) {
        return testRepository.findByIdAndAssignmentId(testId, assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case Not Found."));
    }

    private void lockIfHasSubmissions(Long assignmentId, String message) {
        if (submissionRepository.countByAssignmentId(assignmentId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void validateName(Long assignmentId, String name, Long currentTestId) {
        if (name == null || name.isBlank()) {
            return;
        }
        boolean duplicate = testRepository.findByAssignmentId(assignmentId).stream()
                .anyMatch(t -> name.equals(t.getName())
                        && (currentTestId == null || !currentTestId.equals(t.getId())));
        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A test case named " + name + " already exists in this assignment.");
        }
    }

    public record TestStatsDto(long total, long publicCount, long privateCount) {
    }

    public record TestResponseDto(
            Long testId,
            String name,
            String input,
            String expectedOutput,
            boolean isPublic,
            int displayOrder,
            Long timeLimitMs,
            Long memoryLimitKib) {
        public static TestResponseDto fromEntity(Test test) {
            return new TestResponseDto(
                test.getId(),
                test.getName(),
                test.getInput(),
                test.getExpectedOutput(),
                test.isPublic(),
                test.getDisplayOrder(),
                test.getTimeLimitMs(),
                test.getMemoryLimitKib());
        }
    }

}
