package com.example.demo.course;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

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

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourseByCode(@PathVariable String code) {
        courseService.deleteCourse(code);
    }

    @PatchMapping("/{code}")
    @ResponseStatus(HttpStatus.OK)
    public Course partialUpdateCourse(@PathVariable String code, @RequestBody UpdateCourseRequest request)
    {
        return courseService.partialUpdateCourse(code,request.courseName(), request.description());
    }

    @PostMapping
    public Course createCourse (@RequestBody CreateCourseRequest request) {
        return courseService.createCourse(
            request.code(),
            request.courseName(),
            request.description()
        );
    }

    public record CreateCourseRequest (String code, String courseName,String description) {
    }
    public record UpdateCourseRequest (String courseName, String description){

    }
}

