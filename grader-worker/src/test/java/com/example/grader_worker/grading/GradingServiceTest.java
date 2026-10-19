package com.example.grader_worker.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.submission.Submission;

class GradingServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    private GradingService gradingService;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gradingService = new GradingService(assignmentRepository);

        assignment = new Assignment(
            "A001",
            "Hello World",
            Assignment.AssignmentStatus.PUBLISHED
        );

        com.example.grader_worker.test.Test test = new com.example.grader_worker.test.Test(
            assignment, "Hello World", "", "Hello World", true, 1
        );
        assignment.addTest(test);

        when(assignmentRepository.findById(null))
            .thenReturn(Optional.of(assignment));
    }

    @Test
    void validHelloWorld_returnsAccepted() throws IOException {
        Submission submission = submission("""
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
            """);

        gradingService.grade(submission);

        assertEquals(Submission.Status.ACCEPTED, submission.getStatus());
        assertEquals(10.0, submission.getGrade());
    }

    @Test
    void wrongHelloWorld_returnsWrongAnswer() throws IOException {
        Submission submission = submission("""
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello May Cung");
                }
            }
            """);

        gradingService.grade(submission);

        assertEquals(Submission.Status.WRONG_ANSWER, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void invalidCode_returnsCompileError() throws IOException {
        Submission submission = submission("""
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello May Cung);
                }
            }
            """);

        gradingService.grade(submission);

        assertEquals(Submission.Status.COMPILE_ERROR, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void infiniteLoop_returnsTimeLimitExceeded() throws IOException {
        Submission submission = submission("""
            public class Main {
                public static void main(String[] args) {
                    while (true) {
                    }
                }
            }
            """);

        gradingService.grade(submission);

        assertEquals(
            Submission.Status.TIME_LIMIT_EXCEEDED,
            submission.getStatus()
        );
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void divideByZero_returnsRuntimeError() throws IOException {
        Submission submission = submission("""
            public class Main {
                public static void main(String[] args) {
                    int value = 1 / 0;
                    System.out.println(value);
                }
            }
            """);

        gradingService.grade(submission);

        assertEquals(Submission.Status.RUNTIME_ERROR, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void assignmentHasNoTestCase_returnsIllegalStateException() {
        Assignment assignmentWithoutTests = new Assignment(
            "A002",
            "Empty assignment",
            Assignment.AssignmentStatus.PUBLISHED
        );
        when(assignmentRepository.findById(null))
            .thenReturn(Optional.of(assignmentWithoutTests));

        Submission submission = new Submission(
            assignmentWithoutTests,
            "public class Main {}",
            Submission.Status.QUEUED,
            null,
            1
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> gradingService.grade(submission)
        );

        assertEquals("Assignment has no test cases.", exception.getMessage());
        assertEquals(Submission.Status.SYSTEM_ERROR, submission.getStatus());
    }

    private Submission submission(String sourceCode) {
        return new Submission(
            assignment,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1
        );
    }
}
