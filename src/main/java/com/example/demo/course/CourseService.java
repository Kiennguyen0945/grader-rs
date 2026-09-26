package com.example.demo.course;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already exists.");
        }
        Course course = new Course(
            code,
            courseName,
            description
        );
        return courseRepository.save(course);
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
    public Course partialUpdateCourse(Long id, String courseName, String description) {
        Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if(courseName != null) {
            course.setCourseName(courseName);
        }
        if(description != null) {
            course.setDescription(description);
        }
        return course;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    public Course getCourseById(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(() 
        -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return course;
    }
}