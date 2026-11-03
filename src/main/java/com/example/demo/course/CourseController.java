package com.example.demo.course;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.demo.user.CustomUserPrincipal;
import com.example.demo.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;


@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    public CourseController(CourseService courseService){
        this.courseService = courseService;
    }
    @GetMapping
    public List<CourseService.CourseResponseDto> getCourses(@AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            return courseService.getAllCourses();
        }
        boolean isLecturer = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + User.Role.LECTURER.name()));
        return courseService.getCoursesForUser(principal.getId(), isLecturer);
    }
    @GetMapping("/{id}")
    public CourseService.CourseResponseDto getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id); 
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourseById(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseService.CourseResponseDto partialUpdateCourse(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request)
    {
        return courseService.partialUpdateCourse(id, request.courseName(), request.description());
    }

    @PostMapping
    public CourseService.CourseResponseDto createCourse (@RequestBody CreateCourseRequest request) {
        return courseService.createCourse(
            request.userId(),
            request.code(),
            request.courseName(),
            request.description()
        );
    }

    public record CreateCourseRequest (@NotNull Long userId, @NotBlank String code, @NotBlank String courseName, String description) {
    }
    public record UpdateCourseRequest (String courseName, String description){
    }
}


