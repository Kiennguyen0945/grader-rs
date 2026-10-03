package com.example.demo.assignment;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;


@WebMvcTest(AssignmentResourceController.class)
public class AssignmentResourceControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean 
    private AssignmentService assignmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    
    @Test
    void fetchSingleAssignment_returnsAssignment() throws Exception {
        Long courseId = 1L;
        Long assignmentId = 1L;
        AssignmentService.AssignmentResponseDto assignmentResponseDto = 
            new AssignmentService.AssignmentResponseDto(
                courseId,
                1L,
                1L,
                "B001",
                "Print skibidi", 
                Assignment.AssignmentStatus.DRAFT
            );
        when(assignmentService.fetchSingleAssignment(eq(assignmentId))).thenReturn(assignmentResponseDto);
        mockMvc.perform(get("/api/assignments/" + assignmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("B001"));
    }

    @Test
    void partialUpdateAssignmentById_returnsAssignment() throws Exception {
        Long assignmentId = 1L;
        AssignmentService.AssignmentResponseDto assignmentResponseDto = 
            new AssignmentService.AssignmentResponseDto(
                assignmentId,
                1L,
                1L,
                "B001",
                "Print skibidi", 
                Assignment.AssignmentStatus.DRAFT
            );
        when(assignmentService.partialUpdateAssignment(eq(assignmentId), eq("B001"), anyString(), eq(Assignment.AssignmentStatus.DRAFT)))
        .thenReturn(assignmentResponseDto);
        mockMvc.perform(patch("/api/assignments/" + assignmentId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(assignmentResponseDto)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("B001"));
    }

    @Test
    void deleteAssignmentById_returnNoContent() throws Exception {
        Long assignmentId = 1L;
        mockMvc.perform(delete("/api/assignments/" + assignmentId))
        .andExpect(status().isOk());
    }
    
    

}
