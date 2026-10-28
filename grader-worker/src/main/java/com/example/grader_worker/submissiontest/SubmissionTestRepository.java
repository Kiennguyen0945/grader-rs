package com.example.grader_worker.submissiontest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SubmissionTestRepository extends JpaRepository<SubmissionTest, Long> {

    Optional<SubmissionTest>
    findByGradingRun_Submission_IdAndTest_IdAndGradingRun_Id(
            Long submissionId,
            Long testId,
            Long gradingRunId);
    @Query("""
        SELECT st
        FROM SubmissionTest st
        JOIN FETCH st.test t
        JOIN FETCH st.gradingRun gr
        JOIN FETCH gr.submission sub
        WHERE sub.id = :submissionId
                AND gr.id = :gradingRunId
        ORDER BY t.displayOrder ASC
        """)
    List<SubmissionTest> findByGradingRun_Submission_IdAndGradingRun_Id(
            @Param("submissionId") Long submissionId,
            @Param("gradingRunId") Long gradingRunId);
}
