package com.example.demo.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    long countByAssignmentIdAndUserId(Long assignmentId, Long userId);
    long countByAssignmentId(Long assignmentId);
    List<Submission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);
    /** Lecturer view: every submission for the assignment, newest first. */
    List<Submission> findByAssignmentIdOrderByIdDesc(Long assignmentId);
}
