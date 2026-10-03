package com.example.demo.assignment;

import java.util.ArrayList;

import com.example.demo.course.Course;
import com.example.demo.test.Test;
import com.example.demo.user.User;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "course_id", nullable = false, 
        foreignKey = @ForeignKey(name = "fk_assignment_course")
    )
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id", nullable = false,
        foreignKey =  @ForeignKey(name = "fk_assignment_user")
    )
    private User user;

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
    public Assignment(Course course, User user, String code, String title, AssignmentStatus assignmentStatus) {
        this.course = course;
        this.user = user;
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
    public Course getCourse() {
        return course;
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
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public void setCourse(Course course) {
        this.course = course;
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
