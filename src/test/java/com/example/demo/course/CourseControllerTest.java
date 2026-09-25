package com.example.demo.course;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CourseController.class)
class CourseControllerTest {
    @Autowired 
    private MockMvc mockMvc;
    @MockitoBean
    private CourseService courseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test 
    void  getCourses_returnsCourses() throws Exception {
        Course course = new Course("CS505", "Harvard", "");
        when(courseService.getAllCourses()).thenReturn(List.of(course));
        mockMvc.perform(get("/api/courses")).andExpect(status().isOk()).
        andExpect(jsonPath("$[0].code").value("CS505")).
        andExpect(jsonPath("$[0].courseName").value("Harvard"));
    }
    @Test
    void createCourse_saveNewCourse() throws Exception {
        Course course = new Course("CS505", "Harvard", "");
        when(courseService.createCourse("CS505", "Harvard", "")).thenReturn(course);
        mockMvc.perform(
            post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(course)))   
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("CS505"));
    }
    @Test
    void getCourseByCode_returnCourse() throws Exception {
        Course course = new Course("CS505", "Harvard", "");
        when(courseService.getCourseByCode("CS505")).thenReturn(Optional.of(course));
        mockMvc.perform(
            get("/api/courses/CS505"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("CS505"));
    }
    
    @Test 
    void partialUpdateCodeByCode_returnsCourse() throws Exception {
        Course course = new Course("CS505", "Yale", null);
        when(courseService.partialUpdateCourse("CS505", "Yale", null)).thenReturn(course);
        mockMvc.perform(
        patch("/api/courses/CS505")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(course)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CS505"))
        .andExpect(jsonPath("$.courseName").value("Yale"));
    }

    @Test
    void deleteCourse_returnNoContent() throws Exception {
        mockMvc.perform(delete("/api/courses/CS505"))
        .andExpect(status().isNoContent());
    }


}
