package com.example.demo.course;

import com.example.demo.assignment.Assignment;
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

    // Fixed: Return type changed from String to void
    public void setCode(String code) {
        this.code = code;
    }

    public String getCourseName() {
        return courseName;
    }

    // Fixed: Return type changed from String to void
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    // Fixed: Returned description instead of courseName
    public String getDescription() {
        return description;
    }

    // Fixed: Return type changed from String to void
    public void setDescription(String description) {
        this.description = description;
    }
}