package com.example.demo.submissiontest;

import com.example.demo.submission.Submission;
import com.example.demo.test.Test;
import jakarta.persistence.*;



@Entity
@Table(name = "submission_tests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_submission_test_run",
            columnNames = {"submission_id", "test_id", "grading_run_id"}
        )
    }
)
public class SubmissionTest {

    public enum Status {
        QUEUED,
        RUNNING,
        PASSED,
        WRONG_ANSWER,
        COMPILE_ERROR,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        RUNTIME_ERROR,
        SYSTEM_ERROR,
        NOT_RUN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "submission_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_submission_tests_submission")
    )
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "test_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_submission_tests_test")
    )
    private Test test;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "memory_used_kib")
    private Long memoryUsedKib;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;


    @Column(name = "grading_run_id", nullable = false)
    private Long gradingRunId;

    protected SubmissionTest() {
    }

    public SubmissionTest(
            Submission submission,
            Test test,
            Status status,
            String actualOutput,
            Long memoryUsedKib,
            Long executionTimeMs, 
            Long gradingRunId) {
        this.submission = submission;
        this.test = test;
        this.status = status;
        this.actualOutput = actualOutput;
        this.memoryUsedKib = memoryUsedKib;
        this.executionTimeMs = executionTimeMs;
        this.gradingRunId = gradingRunId;
    }

    public Long getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public Long getMemoryUsedKib() {
        return memoryUsedKib;
    }

    public void setMemoryUsedKib(Long memoryUsedKib) {
        this.memoryUsedKib = memoryUsedKib;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Long getGradingRunId() {
        return gradingRunId;
    }

    public void setGradingRunId(Long gradingRunId) {
        this.gradingRunId = gradingRunId;
    }
}