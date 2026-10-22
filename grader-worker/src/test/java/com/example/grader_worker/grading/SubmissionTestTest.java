package com.example.grader_worker.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.assignment.AssignmentRepository;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;
import com.example.grader_worker.submissiontest.SubmissionTest;
import com.example.grader_worker.submissiontest.SubmissionTestRepository;

/**
 * These are integration-style tests: they run the real javac and java
 * (through /usr/bin/time), so they need Linux with a JDK. Only the database
 * repositories are mocked.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(60) // a hanging grader should fail the test, not freeze the build
class SubmissionTestTest {

    // ---------- student programs used by the tests ----------

    private static final String HELLO = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello World");
            }
        }
        """;

    private static final String HELLO_WITH_EXTRA_WHITESPACE = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello World   ");
                System.out.println();
            }
        }
        """;

    private static final String HELLO_WITH_DOUBLE_SPACE = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello  World");
            }
        }
        """;

    private static final String WRONG_TEXT = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello May Cung");
            }
        }
        """;

    private static final String SYNTAX_ERROR = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello May Cung);
            }
        }
        """;

    private static final String INFINITE_LOOP = """
        public class Main {
            public static void main(String[] args) {
                while (true) {
                }
            }
        }
        """;

    private static final String DIVIDE_BY_ZERO = """
        public class Main {
            public static void main(String[] args) {
                int value = 1 / 0;
                System.out.println(value);
            }
        }
        """;

    // reads one int from stdin and prints it doubled
    private static final String DOUBLE_INPUT = """
        import java.util.Scanner;
        public class Main {
            public static void main(String[] args) {
                Scanner in = new Scanner(System.in);
                int n = in.nextInt();
                System.out.println(n * 2);
            }
        }
        """;

    // reads one int n and prints 10 / n (crashes when n == 0)
    private static final String TEN_DIVIDED_BY_INPUT = """
        import java.util.Scanner;
        public class Main {
            public static void main(String[] args) {
                Scanner in = new Scanner(System.in);
                int n = in.nextInt();
                System.out.println(10 / n);
            }
        }
        """;

    // ---------- test setup ----------

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionTestRepository submissionTestRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    private GradingService gradingService;
    private Assignment assignment;

    // The SubmissionTest rows that our fake repository hands back to the service.
    // We keep them so a test can check the status of each individual test case.
    private List<SubmissionTest> rows;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gradingService = new GradingService(
            assignmentRepository, submissionTestRepository, submissionRepository);

        // an assignment with NO test cases yet; each test adds the ones it needs
        assignment = new Assignment(
            "A001", "Test assignment", Assignment.AssignmentStatus.PUBLISHED);

        when(assignmentRepository.findById(any()))
            .thenReturn(Optional.of(assignment));

        // "no earlier grading run" -> the service will use grading run 1
        when(submissionTestRepository.findMaxGradingRunIdBySubmissionId(any()))
            .thenReturn(null);
    }

    // ---------- verdict tests: one scenario each ----------

    @Test
    void correctOutput_isAccepted() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(HELLO, t1);

        assertEquals(Submission.Status.ACCEPTED, submission.getStatus());
        assertEquals(10.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.PASSED);
    }

    @Test
    void wrongOutput_isWrongAnswer() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(WRONG_TEXT, t1);

        assertEquals(Submission.Status.WRONG_ANSWER, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.WRONG_ANSWER);
    }

    @Test
    void syntaxError_isCompileError_andTestsAreNotRun() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(SYNTAX_ERROR, t1);

        assertEquals(Submission.Status.COMPILE_ERROR, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.NOT_RUN);
    }

    @Test
    void infiniteLoop_isTimeLimitExceeded() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(INFINITE_LOOP, t1);

        assertEquals(Submission.Status.TIME_LIMIT_EXCEEDED, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void divideByZero_isRuntimeError() throws IOException {
        var t1 = testCase("", "anything");

        Submission submission = grade(DIVIDE_BY_ZERO, t1);

        assertEquals(Submission.Status.RUNTIME_ERROR, submission.getStatus());
        assertEquals(0.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.RUNTIME_ERROR);
    }

    // ---------- output comparison rules ----------

    @Test
    void trailingSpacesAndBlankLines_areIgnored() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(HELLO_WITH_EXTRA_WHITESPACE, t1);

        assertEquals(Submission.Status.ACCEPTED, submission.getStatus());
    }

    @Test
    void differentSpacingInsideTheLine_isWrongAnswer() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(HELLO_WITH_DOUBLE_SPACE, t1);

        assertEquals(Submission.Status.WRONG_ANSWER, submission.getStatus());
    }

    // ---------- input and scoring ----------

    @Test
    void programReceivesTheTestInput() throws IOException {
        var t1 = testCase("2", "4");
        var t2 = testCase("21", "42");

        Submission submission = grade(DOUBLE_INPUT, t1, t2);

        assertEquals(Submission.Status.ACCEPTED, submission.getStatus());
        assertEquals(10.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.PASSED, SubmissionTest.Status.PASSED);
    }

    @Test
    void oneOfTwoTestsPasses_givesHalfTheGrade() throws IOException {
        var t1 = testCase("2", "4");     // program prints 4 -> pass
        var t2 = testCase("3", "999");   // program prints 6 -> fail

        Submission submission = grade(DOUBLE_INPUT, t1, t2);

        assertEquals(Submission.Status.WRONG_ANSWER, submission.getStatus());
        assertEquals(5.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.PASSED, SubmissionTest.Status.WRONG_ANSWER);
    }

    @Test
    void crashOnSecondTest_stillGradesTheFirstOne() throws IOException {
        var t1 = testCase("1", "10");    // 10 / 1 = 10 -> pass
        var t2 = testCase("0", "0");     // 10 / 0 -> crash

        Submission submission = grade(TEN_DIVIDED_BY_INPUT, t1, t2);

        assertEquals(Submission.Status.RUNTIME_ERROR, submission.getStatus());
        assertEquals(5.0, submission.getGrade());
        assertRowStatuses(SubmissionTest.Status.PASSED, SubmissionTest.Status.RUNTIME_ERROR);
    }

    // ---------- other behaviour ----------

    @Test
    void finalResultIsSaved() throws IOException {
        var t1 = testCase("", "Hello World");

        Submission submission = grade(HELLO, t1);

        verify(submissionRepository, atLeastOnce()).save(submission);
    }

    @Test
    void assignmentWithoutTestCases_throwsAndMarksSystemError() {
        // "assignment" from setUp() has no test cases
        Submission submission = newSubmission("public class Main {}");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> gradingService.grade(submission));

        assertEquals("Assignment has no test cases.", exception.getMessage());
        assertEquals(Submission.Status.SYSTEM_ERROR, submission.getStatus());
    }

    // ---------- helpers (keep the tests above short and readable) ----------

    /** Creates a test case and adds it to the assignment. */
    private com.example.grader_worker.test.Test testCase(String input, String expectedOutput) {
        int order = assignment.getTests().size() + 1;
        var test = new com.example.grader_worker.test.Test(
            assignment, "case " + order, input, expectedOutput, true, order);
        assignment.addTest(test);
        return test;
    }

    private Submission newSubmission(String sourceCode) {
        return new Submission(assignment, sourceCode, Submission.Status.QUEUED, null, 1);
    }

    /**
     * Prepares the fake repositories, runs the grader and returns the submission.
     *
     * The service creates its own SubmissionTest rows and saves them to the mock
     * (which does nothing), then looks them up again. So we create the rows
     * ourselves here and make the mock return them, one per call, in order.
     */
    private Submission grade(String sourceCode, com.example.grader_worker.test.Test... cases)
            throws IOException {
        Submission submission = newSubmission(sourceCode);

        rows = new ArrayList<>();
        for (var c : cases) {
            rows.add(new SubmissionTest(
                submission, c, SubmissionTest.Status.QUEUED, null, null, null, 1L));
        }

        // used when the service looks up ALL rows of this run
        when(submissionTestRepository.findBySubmission_IdAndGradingRunId(any(), eq(1L)))
            .thenReturn(rows);

        // used inside the loop: 1st call -> row 1, 2nd call -> row 2, ...
        OngoingStubbing<Optional<SubmissionTest>> stubbing = when(
            submissionTestRepository.findBySubmission_IdAndTest_IdAndGradingRunId(
                any(), any(), eq(1L)))
            .thenReturn(Optional.of(rows.get(0)));
        for (int i = 1; i < rows.size(); i++) {
            stubbing = stubbing.thenReturn(Optional.of(rows.get(i)));
        }

        gradingService.grade(submission);
        return submission;
    }

    private void assertRowStatuses(SubmissionTest.Status... expected) {
        List<SubmissionTest.Status> actual = rows.stream().map(SubmissionTest::getStatus).toList();
        assertEquals(List.of(expected), actual);
    }
}