package com.example.demo.test;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.AssignmentRepository;

import com.example.demo.assignment.Assignment;
import java.util.List;

@Service
public class TestService {
    private final TestRepository testRepository;
    private final AssignmentRepository assignmentRepository;
    public TestService(TestRepository testRepository, AssignmentRepository assignmentRepository) {
        this.testRepository = testRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<TestResponseDto> getTestsByAssignmentId(Long assignmentId) {
        return testRepository.findByAssignmentId(assignmentId).stream().map(TestResponseDto::fromEntity).toList();
    }

  
    public TestResponseDto createTestByAssignmentId(Long assignmentId, String name, String input, String expectedOutput, boolean isPublic, Long timeLimitMs, Long memoryLimitKib) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
        int displayOrder = testRepository.countByAssignmentId(assignmentId);
        Test test = new Test(assignment, name, input, expectedOutput, isPublic, displayOrder + 1, timeLimitMs, memoryLimitKib);
        return TestResponseDto.fromEntity(testRepository.save(test));
    }

    public record TestResponseDto(Long testId, String name, String input, String expectedOutput, boolean isPublic, int displayOrder) {
        public static TestResponseDto fromEntity(Test test) {
            return new TestResponseDto(
                test.getId(), 
                test.getName(), 
                test.getInput(), 
                test.getExpectedOutput(), 
                test.isPublic(), 
                test.getDisplayOrder());
        }
    }

}
