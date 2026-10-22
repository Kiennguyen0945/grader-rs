package com.example.grader_worker.submission;

import com.example.grader_worker.assignment.Assignment;
import com.example.grader_worker.submissiontest.SubmissionTest;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
public class Submission {

    public enum Status {
        QUEUED,
        RUNNING,
        ACCEPTED,
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "assignment_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_submission_assignment")
    )
    private Assignment assignment;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionTest> submissionTests = new ArrayList<>();


    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.QUEUED;

    private Double grade;

    @Column(nullable = false)
    private Integer attemptNumber = 1;

    // Default constructor (required by JPA)
    protected Submission() {
    }

    // Business constructor (excluding auto-generated id)
    public Submission(Assignment assignment, String sourceCode, Status status, Double grade, Integer attemptNumber) {
        this.assignment = assignment;
        this.sourceCode = sourceCode;
        this.status = status != null ? status : Status.QUEUED;
        this.grade = grade;
        this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }
}