package com.example.demo.gradingrun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


/**
 * GradingRunRepository
 */
public interface GradingRunRepository extends JpaRepository<GradingRun, Long> {
    boolean existsByIdAndSubmission_Id(Long gradingRunId, Long submissionId);

    /** Submission-time source: the earliest grading run created for each submission. */
    List<GradingRun> findBySubmission_Assignment_Id(Long assignmentId);

    @Query("""
        SELECT gr
        FROM GradingRun gr
        JOIN FETCH gr.submission
        WHERE gr.id = :gradingRunId
        """)
    Optional<GradingRun> findByIdWithSubmission(
            @Param("gradingRunId") Long gradingRunId);
}