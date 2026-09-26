package com.example.demo.course;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;


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
    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id); 
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourseById(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Course partialUpdateCourse(@PathVariable Long id, @RequestBody UpdateCourseRequest request)
    {
        return courseService.partialUpdateCourse(id, request.courseName(), request.description());
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

