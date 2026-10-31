package com.example.demo.gradingrun;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * GradingRunRepository
 */
public interface GradingRunRepository extends JpaRepository<GradingRun, Long> {
    boolean existsByIdAndSubmission_Id(Long gradingRunId, Long submissionId);
   @Query("""
        SELECT gr
        FROM GradingRun gr
        JOIN FETCH gr.submission
        WHERE gr.id = :gradingRunId
        """)
    Optional<GradingRun> findByIdWithSubmission(
            @Param("gradingRunId") Long gradingRunId);
}