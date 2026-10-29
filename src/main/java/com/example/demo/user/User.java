package com.example.demo.user;
import java.util.ArrayList;

import jakarta.persistence.*;
import com.example.demo.assignment.Assignment;
import com.example.demo.courseUser.CourseUser;
import com.example.demo.gradingrun.GradingRun;
import com.example.demo.submission.Submission;

import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @OneToMany(mappedBy = "user")
    private List<Assignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<CourseUser> courseUsers = new ArrayList<>();

    @OneToMany(mappedBy = "triggeredByUser")
    private List<GradingRun> gradingRuns = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique= true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private String passwordHash;

    

    protected User() {

    }

    public User(String username, Role role, Status status, String passwordHash) {
        this.username = username;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    public enum Role {
        ADMIN,
        LECTURER,
        STUDENT
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        LOCKED
    }


    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Status getStatus() {
        return status;
    }

    public List<CourseUser> getCourseUsers() {
        return courseUsers;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Role getRole () {
        return role;
    }

    public void setRole (Role role) {
        this.role = role;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }


    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
        assignment.setUser(this);
    }
    public void removeAssignment(Assignment assignment) {
        assignments.remove(assignment);
        assignment.setUser(null);
    }
    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void addSubmission(Submission submission) {
        submissions.add(submission);
        submission.setUser(this);
    }
    public void removeSubmission(Submission submission) {
        submissions.remove(submission);
        submission.setUser(null);
    }
    public List<Submission> getSubmissions() {
        return submissions;
    }

    public void addCourseUser(CourseUser courseUser) {
        courseUsers.add(courseUser);
    }


    public List<GradingRun> getGradingRuns() {
        return gradingRuns;
    }

    public void setGradingRuns(List<GradingRun> gradingRuns) {
        this.gradingRuns = gradingRuns;
    }

}
