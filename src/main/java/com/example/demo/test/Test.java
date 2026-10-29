package com.example.demo.test;


import com.example.demo.assignment.Assignment;
import jakarta.persistence.*;

@Entity
@Table(name = "tests", uniqueConstraints = @UniqueConstraint(
    name = "uk_test_assignment_order", 
    columnNames = {"assignment_id", "display_order"}
))
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_test_assignment"))
    private Assignment assignment;

    @Column
    private String name;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private Long timeLimitMs;
    
    @Column(nullable = false)
    private Long memoryLimitKib;

    // Constructors
    public Test() {}

    public Test(Assignment assignment, String name, String input, String expectedOutput, boolean isPublic, int displayOrder, Long timeLimitMs, Long memoryLimitKiB) {
        this.assignment = assignment;
        this.name = name;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.isPublic = isPublic;
        this.displayOrder = displayOrder;
        this.timeLimitMs = timeLimitMs == null ? 4 : timeLimitMs;
        this.memoryLimitKib = memoryLimitKiB == null ? 256L * 1024 : memoryLimitKiB;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Long getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(Long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public Long getMemoryLimitKib() {
        return memoryLimitKib;
    }
    
     public void setMemoryLimitKib(Long memoryLimitKib) {
        this.memoryLimitKib = memoryLimitKib;
    }
}