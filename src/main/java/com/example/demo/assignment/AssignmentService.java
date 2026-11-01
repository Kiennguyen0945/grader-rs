package com.example.demo.assignment;

import com.example.demo.assignment.Assignment.AssignmentStatus;
import com.example.demo.assignment.Assignment.ProgrammingLanguage;
import com.example.demo.course.Course;
import com.example.demo.course.CourseRepository;
import com.example.demo.courseUser.CourseUserRepository;
import com.example.demo.test.Test;
import com.example.demo.test.TestRepository;
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
    private final CourseUserRepository courseUserRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            CourseUserRepository courseUserRepository,
            UserRepository userRepository,
            TestRepository testRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.courseUserRepository = courseUserRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
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

    public List<AssignmentResponseDto> getAssignmentsForCourse(Long courseId, Long userId, boolean isLecturer) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Not Found.");
        }

        if (!isLecturer) {
            boolean isEnrolled = courseUserRepository.existsByCourseIdAndUserId(courseId, userId);
            if (!isEnrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not enrolled in this course.");
            }
            return assignmentRepository.findByCourseIdAndAssignmentStatus(courseId, AssignmentStatus.PUBLISHED)
                    .stream()
                    .map(AssignmentResponseDto::fromEntity)
                    .toList();
        }

        return assignmentRepository.findByCourseId(courseId)
                .stream()
                .map(AssignmentResponseDto::fromEntity)
                .toList();
    }

    public AssignmentResponseDto fetchSingleAssignment(Long id) {
        return assignmentRepository.findById(id)
                .map(AssignmentResponseDto::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));
    }

    public AssignmentDetailResponseDto fetchAssignmentWorkspaceDetail(Long id, Long userId, boolean isLecturer) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment Not Found."));

        if (!isLecturer) {
            boolean isEnrolled = courseUserRepository.existsByCourseIdAndUserId(assignment.getCourse().getId(), userId);
            if (!isEnrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not enrolled in this course.");
            }
            if (assignment.getAssignmentStatus() != AssignmentStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignment is not published.");
            }
        }

        List<SampleTestCaseDto> sampleTests = testRepository
                .findByAssignmentIdAndIsPublicTrueOrderByDisplayOrderAsc(id)
                .stream()
                .map(SampleTestCaseDto::fromEntity)
                .toList();

        return AssignmentDetailResponseDto.fromEntity(assignment, sampleTests);
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

    public record SampleTestCaseDto(
            Long id,
            String name,
            String input,
            String expectedOutput,
            int displayOrder
    ) {
        public static SampleTestCaseDto fromEntity(Test test) {
            if (test == null) {
                return null;
            }
            return new SampleTestCaseDto(
                    test.getId(),
                    test.getName(),
                    test.getInput(),
                    test.getExpectedOutput(),
                    test.getDisplayOrder()
            );
        }
    }

    public record AssignmentDetailResponseDto(
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
            Set<ProgrammingLanguage> allowedLanguages,
            List<SampleTestCaseDto> sampleTestCases
    ) {
        public static AssignmentDetailResponseDto fromEntity(Assignment assignment, List<SampleTestCaseDto> sampleTestCases) {
            if (assignment == null) {
                return null;
            }
            return new AssignmentDetailResponseDto(
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
                    assignment.getAllowedLanguages() != null ? Set.copyOf(assignment.getAllowedLanguages()) : Set.of(),
                    sampleTestCases != null ? sampleTestCases : List.of()
            );
        }
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