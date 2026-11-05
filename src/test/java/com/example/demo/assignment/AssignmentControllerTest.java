package com.example.demo.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                Assignment.AssignmentStatus.PUBLISHED,
                null,
                null,
                null,
                null,
                null,
                0,
                0L
            );
        when(assignmentService.getAssignmentsForCourse(eq(courseId), any(), any(Boolean.class)))
                .thenReturn(List.of(assignmentResponseDto));
        mockMvc.perform(
        get("/api/courses/{courseId}/assignments", courseId)
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
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
            );

        AssignmentService.AssignmentResponseDto assignmentResponseDto =
            new AssignmentService.AssignmentResponseDto(
                1L,
                courseId,
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                0,
                0L
            );

        when(assignmentService.createAssignment(
                courseId,
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
        )).thenReturn(assignmentResponseDto);

        mockMvc.perform(
                post("/api/courses/{courseId}/assignments", courseId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("B001"));
    }
    
    @Test
    void getAssignmentsByCourseId_whenEmpty_returnsEmptyList() throws Exception {
        Long courseId = 1L;
        Long userId = 1L;

        when(assignmentService.getAssignmentsByCourseIdAndUserId(courseId, userId))
            .thenReturn(java.util.List.of());

        mockMvc.perform(
            get("/api/courses/{courseId}/assignments", courseId)
                .param("userId", userId.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAssignmentsByCourseId_withoutUserIdParam_returnsBadRequest() throws Exception {
        Long courseId = 1L;

        // Missing required @RequestParam Long userId
        mockMvc.perform(get("/api/courses/{courseId}/assignments", courseId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createAssignmentByCourseId_missingRequiredFields_returnsBadRequest() throws Exception {
        Long courseId = 1L;

        // Missing mandatory @NotBlank and @NotNull fields (code, title, assignmentStatus)
        AssignmentController.CreateAssignmentRequest invalidRequest =
            new AssignmentController.CreateAssignmentRequest(
                1L,
                "",     // Blank code violates @NotBlank
                "",     // Blank title violates @NotBlank
                null,   // Null status violates @NotNull
                null,
                0,      // submissionLimit < 1 violates @Min(1)
                null,
                null,
                null
            );

        mockMvc.perform(
            post("/api/courses/{courseId}/assignments", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void createAssignmentByCourseId_whenDuplicateCode_returnsConflict() throws Exception {
        Long courseId = 1L;
        Long userId = 1L;

        AssignmentController.CreateAssignmentRequest request =
            new AssignmentController.CreateAssignmentRequest(
                userId,
                "B001",
                "Print skibidi",
                Assignment.AssignmentStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
            );

        when(assignmentService.createAssignment(
            eq(courseId),
            eq(userId),
            eq("B001"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Assignment with code B001 already exists in this course."));

        mockMvc.perform(
            post("/api/courses/{courseId}/assignments", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict());
    }
}
