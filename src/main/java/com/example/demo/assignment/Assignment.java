package com.example.demo.assignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import com.example.demo.course.Course;
import com.example.demo.submission.Submission;
import com.example.demo.test.Test;
import com.example.demo.user.User;

import jakarta.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "assignments", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_assignment_course_code",
        columnNames = {"course_id", "code"}
    )
)
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

    @OneToMany(mappedBy = "assignment")
    private List<Submission> submissions = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus assignmentStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private LocalDateTime openingTime;

    @Column
    private LocalDateTime closingTime;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "assignment_allowed_languages",
        joinColumns = @JoinColumn(name = "assignment_id")
    )
    @Column(name = "language", nullable = false)
    private Set<ProgrammingLanguage> allowedLanguages = new HashSet<>();

    @Column
    private Integer submissionLimit;

    protected Assignment() {
    }
    public Assignment(
            Course course, 
            User user, 
            String code, 
            String title, 
            AssignmentStatus assignmentStatus,
            String description,
            Integer submissionLimit,
            LocalDateTime openingTime,
            LocalDateTime closingTime,
            Set<ProgrammingLanguage> allowedLanguages
        ) {
        validateTimes(openingTime, closingTime);
        this.course = course;
        this.user = user;
        this.code = code;
        this.title = title;
        this.assignmentStatus = assignmentStatus;
        this.description = description;
        setSubmissionLimit(submissionLimit);
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.allowedLanguages = allowedLanguages == null ? new HashSet<>() : new HashSet<>(allowedLanguages);
    }

    public enum AssignmentStatus {
        DRAFT,
        PUBLISHED,
        CLOSED
    }
    public enum ProgrammingLanguage {
        JAVA,
        PYTHON
    }

    private static void validateTimes(
        LocalDateTime openingTime,
        LocalDateTime closingTime
    ) {
        if (openingTime != null
                && closingTime != null
                && !closingTime.isAfter(openingTime)) {
            throw new IllegalArgumentException(
                "closingTime must be after openingTime"
            );
        }
    }
    public void updateSchedule(
        LocalDateTime openingTime,
        LocalDateTime closingTime
    ) {
        validateTimes(openingTime, closingTime);
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }


    public boolean hasSubmissionLimit() {
       return submissionLimit != null;
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

    public Integer getSubmissionLimit() {
        return submissionLimit;
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
    public void setSubmissionLimit(Integer submissionLimit) {
        if (submissionLimit != null && submissionLimit < 1) {
            throw new IllegalArgumentException(
                "submissionLimit must be positive"
            );
        }
        this.submissionLimit = submissionLimit;
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

    public void addSubmission(Submission submission) {
    submissions.add(submission);
    submission.setAssignment(this);
}

    public void removeSubmission(Submission submission) {
        submissions.remove(submission);
        submission.setAssignment(null);
    }
    
    public List<Submission> getSubmissions() {
        return submissions;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalDateTime openingTime) {
        validateTimes(openingTime, this.closingTime);
        this.openingTime = openingTime;
    }

    public LocalDateTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalDateTime closingTime) {
        validateTimes(this.openingTime, closingTime);
        this.closingTime = closingTime;
    }

    public Set<ProgrammingLanguage> getAllowedLanguages() {
        return Set.copyOf(allowedLanguages);
    }

    public void setAllowedLanguages(Set<ProgrammingLanguage> languages) {
        this.allowedLanguages.clear(); 
        if(languages != null) {
            this.allowedLanguages.addAll(languages);
        }
    }

    public void addAllowedLanguage(ProgrammingLanguage language) {
        if (language != null) {
            this.allowedLanguages.add(language);
        }
    }

    public void removeAllowedLanguage(ProgrammingLanguage language) {
        if (language != null) {
            this.allowedLanguages.remove(language);
        }
    }

}
