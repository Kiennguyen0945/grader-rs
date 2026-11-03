package com.example.demo.course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any; // Added missing import
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.user.User;
import com.example.demo.user.UserRepository;



@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;


    @Test
    void createCourse_saveNewCourse() {
        Long userId = 1L;
        User user = new User(
            "Fors",
            User.Role.STUDENT,
            User.Status.ACTIVE,
            "strong-password"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courseRepository.existsByCode("CS505")).thenReturn(false);
        Course savedCourse = new Course("CS505", "Harvard Course", "");
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        CourseService.CourseResponseDto result = courseService.createCourse(userId, "CS505", "Harvard Course", "");
        assertEquals("CS505", result.code());
        assertEquals("Harvard Course", result.courseName());
        verify(courseRepository).save(any(Course.class));
    }
    
    @Test
    void getAllCourses_returnCourseList() {
        List<Course> mockCourses = List.of(
            new Course("CS505", "Harvard", "")
        );
        when(courseRepository.findAll()).thenReturn(mockCourses);
        List<CourseService.CourseResponseDto> result = courseService.getAllCourses();
        assertEquals(1, result.size());
        assertEquals("CS505", result.get(0).code());
        
    }
}
