package com.example.demo.course;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }
    public Course createCourse (
        String code,
        String courseName,
        String description
    ) {
        if (courseRepository.existsByCode(code)) {
            throw new IllegalArgumentException(
                "Course already exists."
            );
        }
        Course course = new Course(
            code,
            courseName,
            description
        );
        return courseRepository.save(course);
    }
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    public Optional<Course> getCourseById(Long id) {
        if (!courseExists(id)) {
            throw new IllegalArgumentException (
                "Course doesn't exist."
            );
        }
        return courseRepository.findById(id);
    }
    public boolean courseExists(Long id) {
        return courseRepository.existsById(id); 
    }
}