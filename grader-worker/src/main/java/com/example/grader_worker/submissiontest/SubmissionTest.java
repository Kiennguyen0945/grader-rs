package com.example.grader_worker.submissiontest;

import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.test.Test;
import jakarta.persistence.*;



@Entity
@Table(name = "submission_tests",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"submission_id", "test_id"}
        )
    }
)
public class SubmissionTest {

    public enum Status {
        QUEUED,
        RUNNING,
        ACCEPTED,
        WRONG_ANSWER,
        COMPILE_ERROR,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        RUNTIME_ERROR,
        SYSTEM_ERROR
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

    private Long memoryUsedKb;

    private Long executionTimeMs;

    protected SubmissionTest() {
    }

    public SubmissionTest(
            Submission submission,
            Test test,
            Status status,
            String actualOutput,
            Long memoryUsedKb,
            Long executionTimeMs) {
        this.submission = submission;
        this.test = test;
        this.status = status;
        this.actualOutput = actualOutput;
        this.memoryUsedKb = memoryUsedKb;
        this.executionTimeMs = executionTimeMs;
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

    public Long getMemoryUsedKb() {
        return memoryUsedKb;
    }

    public void setMemoryUsedKb(Long memoryUsedKb) {
        this.memoryUsedKb = memoryUsedKb;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}