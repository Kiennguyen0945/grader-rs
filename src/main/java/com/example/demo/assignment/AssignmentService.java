package com.example.demo.assignment;

import com.example.demo.assignment.Assignment.AssignmentStatus;
import com.example.demo.assignment.Assignment.ProgrammingLanguage;
import com.example.demo.course.Course;
import com.example.demo.course.CourseRepository;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    @Transactional
    public AssignmentResponseDto createAssignment(
        Long courseId,
        Long userId,
        String code,
        String title,
        AssignmentStatus assignmentStatus,
        String description,
        Integer submissionLimit,
        LocalDateTime openingTime,
        LocalDateTime closingTime,
        Set<ProgrammingLanguage> allowedLanguages
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Not Found."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found."));

        if (assignmentRepository.existsByCourseIdAndCode(course.getId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment with code " + code + " already exists in this course.");
        }

        Assignment assignment = new Assignment(
            course,
            user,
            code,
            title,
            assignmentStatus,
            description,
            submissionLimit,
            openingTime,
            closingTime,
            allowedLanguages
        );
        return AssignmentResponseDto.fromEntity(assignmentRepository.save(assignment));
    }

    public List<AssignmentResponseDto> getAssignmentsByCourseIdAndUserId(Long courseId, Long userId) {
        return assignmentRepository.findByCourseIdAndUserId(courseId, userId)
                .stream()
                .map(AssignmentResponseDto::fromEntity)
                .toList();
    }

    public AssignmentResponseDto fetchSingleAssignment(Long id) {
        return assignmentRepository.findById(id)
                .map(AssignmentResponseDto::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
    }

    @Transactional
    public AssignmentResponseDto partialUpdateAssignment(
            Long assignmentId,
            String code,
            String title,
            AssignmentStatus assignmentStatus,
            String description,
            Integer submissionLimit,
            LocalDateTime openingTime,
            LocalDateTime closingTime,
            Set<ProgrammingLanguage> allowedLanguages
    ) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assignment Not Found."
                ));

        if (code != null && !code.equals(assignment.getCode())) {
            if (assignmentRepository.existsByCourseIdAndCode(
                    assignment.getCourse().getId(), code)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Assignment with code " + code
                                + " already exists in this course."
                );
            }

            assignment.setCode(code);
        }

        if (title != null) {
            assignment.setTitle(title);
        }

        if (assignmentStatus != null) {
            assignment.setAssignmentStatus(assignmentStatus);
        }

        if (description != null) {
            assignment.setDescription(description);
        }

        if (submissionLimit != null) {
            assignment.setSubmissionLimit(submissionLimit);
        }

        if (allowedLanguages != null) {
            assignment.setAllowedLanguages(allowedLanguages);
        }

        if (openingTime != null || closingTime != null) {
            LocalDateTime newOpeningTime =
                    openingTime != null
                            ? openingTime
                            : assignment.getOpeningTime();

            LocalDateTime newClosingTime =
                    closingTime != null
                            ? closingTime
                            : assignment.getClosingTime();

            assignment.updateSchedule(newOpeningTime, newClosingTime);
        }
        return AssignmentResponseDto.fromEntity(
                assignmentRepository.save(assignment)
        );
    }
    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found");
        }
        assignmentRepository.deleteById(id);
    }

    public record AssignmentResponseDto(
        Long id,
        Long courseId,
        Long userId,
        String code,
        String title,
        AssignmentStatus assignmentStatus,
        String description,
        Integer submissionLimit,
        LocalDateTime openingTime,
        LocalDateTime closingTime,
        Set<ProgrammingLanguage> allowedLanguages
    ) {
        public static AssignmentResponseDto fromEntity(Assignment assignment) {
            if (assignment == null) {
                return null;
            }

            return new AssignmentResponseDto(
                    assignment.getId(),
                    assignment.getCourse() != null ? assignment.getCourse().getId() : null,
                    assignment.getUser() != null ? assignment.getUser().getId() : null,
                    assignment.getCode(),
                    assignment.getTitle(),
                    assignment.getAssignmentStatus(),
                    assignment.getDescription(),
                    assignment.getSubmissionLimit(),
                    assignment.getOpeningTime(),
                    assignment.getClosingTime(),
                    assignment.getAllowedLanguages() != null ? Set.copyOf(assignment.getAllowedLanguages()) : Set.of()
            );
        }
    }
}