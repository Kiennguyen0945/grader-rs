package com.example.demo.gradingrun;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.submission.Submission;
import com.example.demo.submissiontest.SubmissionTest;
import com.example.demo.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name = "grading_runs")
public class GradingRun {

    public enum Status {
        QUEUED, RUNNING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "gradingRun")
    private List<SubmissionTest> submissionTests = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private User triggeredByUser; // null if triggered by student submission, non-null if lecturer re-grade

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(columnDefinition = "TEXT")
    private String reason; // e.g. "Initial grading", "Re-run after test update"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.QUEUED;

    @Column
    private Double grade;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;


    protected GradingRun() {
    }

    public GradingRun(
            Submission submission,
            User triggeredByUser,
            String reason) {

        this.submission = submission;
        this.triggeredByUser = triggeredByUser;
        this.reason = reason;
        this.status = Status.QUEUED;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }


    public List<SubmissionTest> getSubmissionTests() {
        return submissionTests;
    }


    public User getTriggeredByUser() {
        return triggeredByUser;
    }

    public void setTriggeredByUser(User triggeredByUser) {
        this.triggeredByUser = triggeredByUser;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Submission getSubmission() {
    return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public void addSubmissionTest(SubmissionTest submissionTest) {
        submissionTests.add(submissionTest);
        submissionTest.setGradingRun(this);
    }
    public void removeSubmissionTest(SubmissionTest submissionTest) {
        submissionTests.remove(submissionTest);
        submissionTest.setGradingRun(null);
    }
    
}
