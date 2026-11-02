package com.example.demo.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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

import java.util.List;


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
        AssignmentService.AssignmentDetailResponseDto assignmentDetailResponseDto = 
            new AssignmentService.AssignmentDetailResponseDto(
                assignmentId,
                courseId,
                1L,
                "B001",
                "Print skibidi", 
                Assignment.AssignmentStatus.PUBLISHED,
                null,
                null,
                null,
                null,
                null,
                List.of()
            );
        when(assignmentService.fetchAssignmentWorkspaceDetail(eq(assignmentId), any(), any(Boolean.class)))
                .thenReturn(assignmentDetailResponseDto);
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
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
            );
        when(assignmentService.partialUpdateAssignment(eq(assignmentId), eq("B001"), anyString(), eq(Assignment.AssignmentStatus.DRAFT), any(), any(), any(), any(), any()))
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
    
        @Test
    void fetchSingleAssignment_whenNotFound_returnsNotFound() throws Exception {
        Long assignmentId = 999L;
        when(assignmentService.fetchAssignmentWorkspaceDetail(eq(assignmentId), any(), any(Boolean.class)))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/assignments/" + assignmentId))
            .andExpect(status().isNotFound());
    }

    @Test
    void partialUpdateAssignmentById_withInvalidCodeLength_returnsBadRequest() throws Exception {
        Long assignmentId = 1L;
        // Code exceeds max 50 characters limit defined by @Size(max = 50)
        String tooLongCode = "A".repeat(51);

        AssignmentResourceController.UpdateAssignmentRequest invalidRequest =
            new AssignmentResourceController.UpdateAssignmentRequest(
                tooLongCode,
                "Valid Title",
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
            );

        mockMvc.perform(patch("/api/assignments/" + assignmentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void partialUpdateAssignmentById_updatesMultipleFields() throws Exception {
        Long assignmentId = 1L;
        AssignmentResourceController.UpdateAssignmentRequest updateRequest =
            new AssignmentResourceController.UpdateAssignmentRequest(
                "B002",
                "New Title",
                Assignment.AssignmentStatus.PUBLISHED,
                "Updated description",
                5,
                null,
                null,
                java.util.Set.of(Assignment.ProgrammingLanguage.JAVA)
            );

        AssignmentService.AssignmentResponseDto responseDto =
            new AssignmentService.AssignmentResponseDto(
                assignmentId,
                1L,
                1L,
                "B002",
                "New Title",
                Assignment.AssignmentStatus.PUBLISHED,
                "Updated description",
                5,
                null,
                null,
                java.util.Set.of(Assignment.ProgrammingLanguage.JAVA)
            );

        when(assignmentService.partialUpdateAssignment(
                eq(assignmentId),
                eq("B002"),
                eq("New Title"),
                eq(Assignment.AssignmentStatus.PUBLISHED),
                eq("Updated description"),
                eq(5),
                nullable(java.time.LocalDateTime.class),
                nullable(java.time.LocalDateTime.class),
                eq(java.util.Set.of(Assignment.ProgrammingLanguage.JAVA))
        )).thenReturn(responseDto);

        mockMvc.perform(patch("/api/assignments/" + assignmentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("B002"))
            .andExpect(jsonPath("$.title").value("New Title"))
            .andExpect(jsonPath("$.assignmentStatus").value("PUBLISHED"))
            .andExpect(jsonPath("$.submissionLimit").value(5));
    }

}
