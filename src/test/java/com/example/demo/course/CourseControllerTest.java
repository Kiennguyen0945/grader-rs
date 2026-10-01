package com.example.demo.course;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

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
    void getCourseById_returnCourse() throws Exception {
        Long id = 1L;
        Course course = new Course("CS505", "Harvard", "");
        when(courseService.getCourseById(eq(id))).thenReturn(course);
        mockMvc.perform(
            get("/api/courses/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("CS505"));
    }
    
    @Test 
    void partialUpdateCourseById_returnsCourse() throws Exception {
        Long id = 1L;
        Course course = new Course("CS505", "Yale", null);
        when(courseService.partialUpdateCourse(eq(id), anyString(), any())).thenReturn(course);
        mockMvc.perform(
        patch("/api/courses/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(course)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CS505"))
        .andExpect(jsonPath("$.courseName").value("Yale"));
    }

    @Test
    void deleteCourse_returnNoContent() throws Exception {
        Long id = 1L;
        mockMvc.perform(delete("/api/courses/" + id))
        .andExpect(status().isNoContent());
    }

    @Test
    void getCourseDoesNotExist_returnNotFound() throws Exception {
        Long id = 999L;
        when(courseService.getCourseById(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/courses/" + id))
        .andExpect(status().isNotFound());
    }


}
