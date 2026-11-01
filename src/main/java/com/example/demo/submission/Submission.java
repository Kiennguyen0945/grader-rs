package com.example.demo.submission;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.assignment.Assignment;
import com.example.demo.gradingrun.GradingRun;
import com.example.demo.user.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
        NOT_RUN,
        OUTPUT_LIMIT_EXCEEDED
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_submission_user")
    )
    private User user;


    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL)
    private List<GradingRun> gradingRuns = new ArrayList<>();
    

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
    public Submission(Assignment assignment, User user, String sourceCode, Status status, Double grade, Integer attemptNumber) {
        this.assignment = assignment;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public List<GradingRun> getGradingRuns() {
        return gradingRuns;
    }
    public void addGradingRun(GradingRun gradingRun) {
        gradingRuns.add(gradingRun);
        gradingRun.setSubmission(this);
    }

}