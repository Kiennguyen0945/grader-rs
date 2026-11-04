package com.example.grader_worker.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.config.KubernetesJobService;
import com.example.grader_worker.gradingrun.GradingRun;
import com.example.grader_worker.gradingrun.GradingRunRepository;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;
import com.example.grader_worker.submissiontest.SubmissionTest;
import com.example.grader_worker.submissiontest.SubmissionTestRepository;


class GradingServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionTestRepository submissionTestRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private GradingRunRepository gradingRunRepository;

    @Mock
    private KubernetesJobService kubernetesJobService;

    private GradingService gradingService;
    private Assignment assignment;
    private com.example.grader_worker.test.Test test;
    private SubmissionTest submissionTest;
    private GradingRun gradingRun;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gradingService = new GradingService(
            assignmentRepository,
            submissionTestRepository,
            submissionRepository,
            gradingRunRepository,
            kubernetesJobService
        );

        assignment = new Assignment(
            "A001",
            "Hello World",
            Assignment.AssignmentStatus.PUBLISHED
        );

        test = new com.example.grader_worker.test.Test(
            assignment, "Hello World", "", "Hello World", true, 1, 4000L, 256L * 1024
        );
        assignment.addTest(test);
        
        when(assignmentRepository.findByIdWithTests(null))
            .thenReturn(Optional.of(assignment));
    }
    void setUp(Submission submission, com.example.grader_worker.test.Test test) {
        gradingRun = new GradingRun(submission, "test");
        submissionTest = new SubmissionTest(
                    test,
                    gradingRun,
                    SubmissionTest.Status.QUEUED,   
                    null, 
                    null, 
                    null
                );
        submissionTestRepository.save(submissionTest);
        List<SubmissionTest> submissionTests = new ArrayList<>();
        submissionTests.add(submissionTest);
        when(submissionTestRepository
                .findByGradingRun_Submission_IdAndTest_IdAndGradingRun_Id(
                    submission.getId(), test.getId(), gradingRun.getId())
            ).thenReturn(Optional.of(submissionTest));
        when(submissionTestRepository
            .findByGradingRun_Submission_IdAndGradingRun_Id(submission.getId(), gradingRun.getId()))
            .thenReturn(submissionTests);
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
        setUp(submission, test);
        
        gradingService.grade(submission, gradingRun);
        
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
        setUp(submission, test);
        gradingService.grade(submission, gradingRun);
        System.out.printf(
            "status=%s, grade=%s, submission=%s%n",
            submission.getStatus(),
            submission.getGrade(),
            submission
        );
        System.out.printf(
            "STstatus=%s, STactualOutput=%s",
            submissionTest.getStatus(),
            submissionTest.getActualOutput()
        );
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
        setUp(submission, test);
        gradingService.grade(submission, gradingRun);
        System.out.printf(
            "status=%s, grade=%s, submission=%s%n",
            submission.getStatus(),
            submission.getGrade(),
            submission
        );
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
        setUp(submission, test);
        gradingService.grade(submission, gradingRun);

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
        setUp(submission, test);
        gradingService.grade(submission, gradingRun);

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
        when(assignmentRepository.findByIdWithTests(null))
            .thenReturn(Optional.of(assignmentWithoutTests));

        Submission submission = new Submission(
            assignmentWithoutTests,
            "public class Main {}",
            Submission.Status.QUEUED,
            null,
            1
        );

        gradingRun = new GradingRun(submission, "test");
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> gradingService.grade(submission, gradingRun)
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
