package com.example.demo.course;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }
    @Transactional
    public CourseResponseDto createCourse (
        Long userId,
        String code,
        String courseName,
        String description
    ) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        if (courseRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already exists.");
        }
        Course course = new Course(
            code,
            courseName,
            description
        );
        course.addUser(user);
        return CourseResponseDto.fromEntity(courseRepository.save(course));
    }
    @Transactional
    public void deleteCourse(Long id)
    {
        Optional<Course> course = courseRepository.findById(id);
        if(course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found.");
        }
        courseRepository.deleteById(id);
        
    }

    @Transactional 
    public CourseResponseDto partialUpdateCourse(Long id, String courseName, String description) {
        Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if(courseName != null) {
            course.setCourseName(courseName);
        }
        if(description != null) {
            course.setDescription(description);
        }
        return CourseResponseDto.fromEntity(courseRepository.save(course));
    }

    public List<CourseResponseDto> getAllCourses() {
        return courseRepository.findAll().stream().map(CourseResponseDto::fromEntity).toList();
    }

    public List<CourseResponseDto> getCoursesForUser(Long userId, boolean isLecturer) {
        List<Course> courses = isLecturer
                ? courseRepository.findAll()
                : courseRepository.findEnrolledCoursesByUserId(userId);
        return courses.stream().map(CourseResponseDto::fromEntity).toList();
    }

    public CourseResponseDto getCourseById(Long id) {
        return courseRepository.findById(id).map(CourseResponseDto::fromEntity).orElseThrow(() 
        -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record CourseResponseDto(
        Long id,
        String code,
        String courseName,
        String description 
    ) {
       public static CourseResponseDto fromEntity(Course course) {
        return new CourseResponseDto(course.getId(), course.getCode(), course.getCourseName(), course.getDescription());
       }
    }
}
