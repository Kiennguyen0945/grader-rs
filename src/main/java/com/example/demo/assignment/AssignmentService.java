package com.example.demo.assignment;
import org.springframework.stereotype.Service;

import com.example.demo.assignment.Assignment.AssignmentStatus;
import com.example.demo.course.*;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;




@Service 
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    public AssignmentService(
        AssignmentRepository assignmentRepository, 
        CourseRepository courseRepository,
        UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }
    public AssignmentResponseDto createAssignment(Long courseId, Long userId, String code, String title, AssignmentStatus assignmentStatus) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Not Found."));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found."));
        if(assignmentRepository.existsByCourseIdAndCode(course.getId(), code)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment with code " + code + " already exists in this course.");
        }
        Assignment assignment = new Assignment(course, user, code, title, assignmentStatus);
        return AssignmentResponseDto.fromEntity(assignmentRepository.save(assignment));
    }
    public List<AssignmentResponseDto> getAssignmentsByCourseId(Long courseId) {
        return assignmentRepository.findByCourseId(courseId).stream().map(AssignmentResponseDto::fromEntity).toList();
    }
    @Transactional 
    public AssignmentResponseDto partialUpdateAssignment(Long assignmentId, String code, String title, AssignmentStatus assignmentStatus) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
        if (code != null && !code.equals(assignment.getCode())) {
            if(assignmentRepository.existsByCourseIdAndCode(assignment.getCourse().getId(), code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment with code " + code + " already exists in this course.");
            }
            assignment.setCode(code);
        }
        if (title != null) {
            assignment.setTitle(title);
        }
        if (assignmentStatus != null) {
            assignment.setAssignmentStatus(assignmentStatus);
        }
        return AssignmentResponseDto.fromEntity(assignmentRepository.save(assignment));
    }
    public AssignmentResponseDto fetchSingleAssignment(Long id) {
        return assignmentRepository.findById(id)
        .map(AssignmentResponseDto::fromEntity)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
    }
    public void deleteAssignment(Long id) {
        if(!assignmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found");
        }
        assignmentRepository.deleteById(id);
    }
    public record AssignmentResponseDto(Long id, String code, String title, Assignment.AssignmentStatus assignmentStatus) {
        public static AssignmentResponseDto fromEntity(Assignment assignment) {
        return new AssignmentResponseDto(assignment.getId(), assignment.getCode(), assignment.getTitle(), assignment.getAssignmentStatus());
    }
    }
    
}
