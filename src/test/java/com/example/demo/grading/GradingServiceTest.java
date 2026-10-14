package com.example.demo.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.assignment.Assignment;
import com.example.demo.assignment.AssignmentRepository;
import com.example.demo.assignment.AssignmentService;
import com.example.demo.course.Course;
import com.example.demo.course.CourseService;
import com.example.demo.submission.Submission;
import com.example.demo.test.TestService;
import com.example.demo.user.User;
import com.example.demo.user.UserService;

@SpringBootTest
class GradingServiceTest {

    @Autowired
    private GradingService gradingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private TestService testService;

    @Autowired 
    private AssignmentRepository assignmentRepository;

    private User user;
    private Course course;
    private AssignmentService.AssignmentResponseDto assignment;
    private TestService.TestResponseDto test;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        user = userService.createUser(
            "Klein-" + suffix,
            User.Role.STUDENT,
            "strong-password"
        );

        course = courseService.createCourse(
            user.getId(),
            "CS-" + suffix,
            "Introduction to Programming",
            null
        );

        assignment = assignmentService.createAssignment(
            course.getId(),
            user.getId(),
            "A001",
            "Hello World",
            Assignment.AssignmentStatus.PUBLISHED
        );

        test = testService.createTestByAssignmentId(
            assignment.id(), 
            "Hello World", 
            "", 
            "Hello World", 
            true
        );
    }
    
    @Test
    void validHelloWorld_returnsAccepted() throws IOException {
        Assignment assignmentEntity = assignmentRepository.findById(assignment.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        String sourceCode = """
            public class Main {
                public static void main(String[] args) {
                    String HelloWorld = "Hello World";
                    System.out.println(HelloWorld);
                }
            }
            """;
        Submission submission = new Submission(
            assignmentEntity,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );
        gradingService.grade(submission);
        assertEquals(
            Submission.Status.ACCEPTED,
            submission.getStatus()
        );
        assertEquals(10.0, submission.getGrade());
    }
    
    @Test 
    void wrongHelloWorld_returnsWrongAnswer() throws IOException {
        Assignment assignmentEntity = assignmentRepository.findById(assignment.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        String sourceCode = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello May Cung");
                }
            }
            """;
        Submission submission = new Submission(
            assignmentEntity,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );
        gradingService.grade(submission);
        assertEquals(
            Submission.Status.WRONG_ANSWER,
            submission.getStatus()
        );
        assertEquals(0.0, submission.getGrade());
    }
    @Test
    void invalidCode_returnsCompileError() throws IOException {
        Assignment assignmentEntity = assignmentRepository.findById(assignment.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        String sourceCode = """
            public class Main  {
                public static void main(String[] args) {
                    System.out.println("Hello May Cung);
                }
            }
            """;
        Submission submission = new Submission(
            assignmentEntity,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );
        gradingService.grade(submission);
        assertEquals(
            Submission.Status.COMPILE_ERROR,
            submission.getStatus()
        );
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void infiniteLoop_returnsTimeLimitExceed() throws IOException {
        Assignment assignmentEntity = assignmentRepository.findById(assignment.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        String sourceCode = """
            public class Main  {
                public static void main(String[] args) {
                    int i = 1;
                    while(i > 0) {
                        System.out.println(i);
                    }
                }
            }
            """;
        Submission submission = new Submission(
            assignmentEntity,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );
        gradingService.grade(submission);
        assertEquals(
            Submission.Status.TIME_LIMIT_EXCEEDED,
            submission.getStatus()
        );
        assertEquals(0.0, submission.getGrade());
    }

    @Test
    void devideByZero_returnsRuntimeError() throws IOException {
        Assignment assignmentEntity = assignmentRepository.findById(assignment.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));
        String sourceCode = """
            public class Main  {
                public static void main(String[] args) {
                    int a = 1 / 0;
                    System.out.println(a);
                }
            }
            """;
        Submission submission = new Submission(
            assignmentEntity,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );
        gradingService.grade(submission);
        assertEquals(
            Submission.Status.RUNTIME_ERROR,
            submission.getStatus()
        );
        assertEquals(0.0, submission.getGrade());
    }
    @Test
    void assignmentHasNoTestCase_returnsIllegalStateException() throws IOException {
        Assignment assignmentWithoutTest = new Assignment(course, user, "CS001", "Bye", Assignment.AssignmentStatus.PUBLISHED);
        assignmentRepository.save(assignmentWithoutTest);
        String sourceCode = """
            public class Main  {
                public static void main(String[] args) {
                    int a = 1 / 0;
                    System.out.println(a);
                }
            }
            """;
        Submission submission = new Submission(
            assignmentWithoutTest,
            user,
            sourceCode,
            Submission.Status.QUEUED,
            null,
            1  
        );

        IllegalStateException exception = 
            assertThrows(IllegalStateException.class
                , () -> gradingService.grade(submission));

        assertEquals("Assignment has no test cases.", exception.getMessage());
        assertEquals(Submission.Status.SYSTEM_ERROR, submission.getStatus()); 
    }
}