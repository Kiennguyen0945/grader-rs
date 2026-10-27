package com.example.demo.submissiontest;

import com.example.demo.gradingrun.GradingRun;
import com.example.demo.test.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;



@Entity
@Table(name = "submission_tests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_submission_test_run",
            columnNames = {"test_id", "grading_run_id"}
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grading_run_id", nullable = false, foreignKey =  @ForeignKey(name = "fk_submission_tests_gradingrun"))
    private GradingRun gradingRun;

    protected SubmissionTest() {
    }

    public SubmissionTest(
            Test test,
            GradingRun gradingRun,
            Status status,
            String actualOutput,
            Long memoryUsedKib,
            Long executionTimeMs) {

        this.test = test;
        this.gradingRun = gradingRun;
        this.status = status;
        this.actualOutput = actualOutput;
        this.memoryUsedKib = memoryUsedKib;
        this.executionTimeMs = executionTimeMs;
    }

    public Long getId() {
        return id;
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

    public GradingRun getGradingRun() {
    return gradingRun;
}

    public void setGradingRun(GradingRun gradingRun) {
        this.gradingRun = gradingRun;
    }

    
}