package com.example.demo.course;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    public CourseController(CourseService courseService){
        this.courseService = courseService;
    }
    @GetMapping
    public List<Course> getCourses() {
        return courseService.getAllCourses();
    }
    @GetMapping("/{code}")
    public Optional<Course> getCourseByCode(@PathVariable String code) {
        return courseService.getCourseByCode(code); 
    }

    @PostMapping
    public Course createCourse (@RequestBody CreateCourseRequest request) {
        return courseService.createCourse(
            request.code(),
            request.courseName(),
            request.description()
        );
    }
    public record CreateCourseRequest (
        String code,
        String courseName,
        String description
    ) {

    }
}

