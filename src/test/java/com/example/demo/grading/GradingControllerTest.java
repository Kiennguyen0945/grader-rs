package com.example.demo.grading;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.submission.Submission;
import com.example.demo.submission.SubmissionController;
import com.example.demo.submission.SubmissionService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SubmissionController.class)
class GradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @Test
    void validHelloWorld_returnsAccepted() throws Exception {
        Long userId = 1L;
        Long assignmentId = 1L;
        Long submissionId = 1L;
        Integer attemptNumber = 1;
        Double grade = 10D;

        String sourceCode = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            """;

        SubmissionService.SubmissionResponseDto response =
                new SubmissionService.SubmissionResponseDto(
                        submissionId,
                        assignmentId,
                        userId,
                        sourceCode,
                        Submission.Status.ACCEPTED,
                        grade,
                        attemptNumber
                );

        when(submissionService.createSubmissionByAssignmentIdAndUserId(
                assignmentId, userId, sourceCode
        )).thenReturn(response);

        SubmissionController.CreateSubmissionRequest request =
                new SubmissionController.CreateSubmissionRequest(
                        userId,
                        sourceCode
                );

        mockMvc.perform(
                post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.grade").value(10.0));
    }
    @Test
    void invalidHelloWorld_returnsWrongAnswer() throws Exception {
        Long userId = 1L;
        Long assignmentId = 1L;
        Long submissionId = 1L;
        Integer attemptNumber = 1;
        Double grade = 0D;

        String sourceCode = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello Diddy Skibidi");
                }
            }
            """;

        SubmissionService.SubmissionResponseDto response =
                new SubmissionService.SubmissionResponseDto(
                        submissionId,
                        assignmentId,
                        userId,
                        sourceCode,
                        Submission.Status.WRONG_ANSWER,
                        grade,
                        attemptNumber
                );

        when(submissionService.createSubmissionByAssignmentIdAndUserId(
                assignmentId, userId, sourceCode
        )).thenReturn(response);

        SubmissionController.CreateSubmissionRequest request =
                new SubmissionController.CreateSubmissionRequest(
                        userId,
                        sourceCode
                );
        mockMvc.perform(
                post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WRONG_ANSWER"))
        .andExpect(jsonPath("$.grade").value(0.0));
    }

    @Test
    void compileError_returnCompileError() throws Exception {
        Long userId = 1L;
        Long assignmentId = 1L;
        Long submissionId = 1L;
        Integer attemptNumber = 1;
        Double grade = 0D;

        String sourceCode = """
            public class Man and Diddy Skibidi {
                public static void main(String[] args) {
                    System.out.println("Hello Diddy Skibidi");
                }
            }
            """;

        SubmissionService.SubmissionResponseDto response =
                new SubmissionService.SubmissionResponseDto(
                        submissionId,
                        assignmentId,
                        userId,
                        sourceCode,
                        Submission.Status.COMPILE_ERROR,
                        grade,
                        attemptNumber
                );

        when(submissionService.createSubmissionByAssignmentIdAndUserId(
                assignmentId, userId, sourceCode
        )).thenReturn(response);

        SubmissionController.CreateSubmissionRequest request =
                new SubmissionController.CreateSubmissionRequest(
                        userId,
                        sourceCode
                );
        mockMvc.perform(
                post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPILE_ERROR"))
        .andExpect(jsonPath("$.grade").value(0.0));
    }
    @Test
    void programThrowsException_returnsRuntimeError() throws Exception {
        Long userId = 1L;
        Long assignmentId = 1L;
        Long submissionId = 1L;
        Integer attemptNumber = 1;
        Double grade = 0D;

        String sourceCode = """
            public class Man and Diddy Skibidi {
                public static void main(String[] args) {
                    String HelloWorld = "Hello World";
                    System.out.println(DiddySkibidi);
                }
            }
            """;

        SubmissionService.SubmissionResponseDto response =
                new SubmissionService.SubmissionResponseDto(
                        submissionId,
                        assignmentId,
                        userId,
                        sourceCode,
                        Submission.Status.RUNTIME_ERROR,
                        grade,
                        attemptNumber
                );

        when(submissionService.createSubmissionByAssignmentIdAndUserId(
                assignmentId, userId, sourceCode
        )).thenReturn(response);

        SubmissionController.CreateSubmissionRequest request =
                new SubmissionController.CreateSubmissionRequest(
                        userId,
                        sourceCode
                );
        mockMvc.perform(
                post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNTIME_ERROR"))
        .andExpect(jsonPath("$.grade").value(0.0));
    }

}