package com.example.demo.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    long countByAssignmentIdAndUserId(Long assignmentId, Long userId);
    List<Submission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);
}
