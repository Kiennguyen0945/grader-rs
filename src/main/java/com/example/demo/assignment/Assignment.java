package com.example.demo.assignment;

import com.example.demo.course.Course;
import jakarta.persistence.*;

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

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus assignmentStatus;


    protected Assignment() {
    }
    public Assignment(Course course, String code, String title, AssignmentStatus assignmentStatus) {
        this.course = course;
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

}
