package com.example.demo.course;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any; // Added missing import
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourse_saveNewCourse() {
        when(courseRepository.existsByCode("CS505")).thenReturn(false);
        Course savedCourse = new Course("CS505", "Harvard Course", "");
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        Course result = courseService.createCourse("CS505", "Harvard Course", "");
        assertEquals("CS505", result.getCode());
        assertEquals("Harvard Course", result.getCourseName());
        verify(courseRepository).save(any(Course.class));
    }
    
    void getAllCourses_returnCourseList() {
        List<Course> mockCourses = List.of(
            new Course("CS505", "Harvard", "")
        );
        when(courseRepository.findAll()).thenReturn(mockCourses);
        List<Course> result = courseService.getAllCourses();
        assertEquals(1, result.size());
        assertEquals("CS505", result.get(0).getCode());
        
    }
}