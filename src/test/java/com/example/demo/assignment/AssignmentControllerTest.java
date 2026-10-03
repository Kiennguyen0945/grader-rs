package com.example.demo.assignment;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;


@WebMvcTest(AssignmentController.class)
public class AssignmentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean 
    private AssignmentService assignmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getAssignmentsByCourseId_returnAssignments() throws Exception {
        Long courseId = 1L;
        Long userId = 1L;
        AssignmentService.AssignmentResponseDto assignmentResponseDto = 
            new AssignmentService.AssignmentResponseDto(
                1L,
                courseId,
                userId,
                "B001",
                "Print skibidi", 
                Assignment.AssignmentStatus.DRAFT
            );
        when(assignmentService.getAssignmentsByCourseIdAndUserId(courseId, userId)).thenReturn(List.of(assignmentResponseDto));
        mockMvc.perform(
        get("/api/courses/{courseId}/assignments", courseId)
            .param("userId", userId.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("B001"));
    }

    @Test
    void createAssignmentByCourseId_returnAssignments() throws Exception {
        Long courseId = 1L;
        Long userId = 1L;

        AssignmentController.CreateAssignmentRequest request =
            new AssignmentController.CreateAssignmentRequest(
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT
            );

        AssignmentService.AssignmentResponseDto assignmentResponseDto =
            new AssignmentService.AssignmentResponseDto(
                1L,
                courseId,
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT
            );

        when(assignmentService.createAssignment(
                courseId,
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT
        )).thenReturn(assignmentResponseDto);

        mockMvc.perform(
                post("/api/courses/{courseId}/assignments", courseId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("B001"));
    }
    

}
