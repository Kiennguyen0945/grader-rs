package com.example.grader_worker.assignment;

import java.util.ArrayList;

import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.test.Test;


import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Test> tests = new ArrayList<>();


    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus assignmentStatus;


    protected Assignment() {
    }
    public Assignment(String code, String title, AssignmentStatus assignmentStatus) {
        this.code = code;
        this.title = title;
        this.assignmentStatus = assignmentStatus;
    }

    public enum AssignmentStatus {
        DRAFT,
        PUBLISHED,
        CLOSED
    }

    public Long getId() {
        return id;
    }
    public String getCode() {
        return code;
    }
    public String getTitle() {
        return title;
    }
    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }


    public void setCode(String code) {
        this.code = code;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
    public void addTest(Test test) {
        tests.add(test);
        test.setAssignment(this);
    }
    public void removeTest(Test test) {
        tests.remove(test);
        test.setAssignment(null);
    }
    public List<Test> getTests() {
        return tests;
    }


}
