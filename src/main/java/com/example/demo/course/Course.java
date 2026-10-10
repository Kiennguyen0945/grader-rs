package com.example.demo.course;

import com.example.demo.assignment.Assignment;
import com.example.demo.courseUser.CourseUser;
import com.example.demo.user.User;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @OneToMany(mappedBy = "course")
    private List<Assignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseUser> courseUsers = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = true)
    private String description;

    protected Course() {
    }

    public Course(String code, String courseName, String description) {
        this.code = code;
        this.courseName = courseName;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code; 
    }

    public String getCourseName() {
        return courseName;
    }

    // Fixed: Returned description instead of courseName
    public String getDescription() {
        return description;
    }

    public List<CourseUser> getCourseUsers() {
        return courseUsers;
    }

    // Fixed: Return type changed from String to void
    public void setCode(String code) {
        this.code = code;
    }

    // Fixed: Return type changed from String to void
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    // Fixed: Return type changed from String to void
    public void setDescription(String description) {
        this.description = description;
    }

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
        assignment.setCourse(this);
    }

    public void removeAssignment(Assignment assignment) {
        assignments.remove(assignment);
        assignment.setCourse(null);
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void addUser(User user) {
        CourseUser courseUser = new CourseUser(this, user);
        courseUsers.add(courseUser);
        user.addCourseUser(courseUser);
    }
    
}