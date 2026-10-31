package com.example.demo.submissiontest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionTestRepository
        extends JpaRepository<SubmissionTest, Long> {

    @Query("""
        SELECT MAX(gr.id)
        FROM SubmissionTest st
        JOIN st.gradingRun gr
        WHERE gr.submission.id = :submissionId
        """)
    Long findMaxGradingRunIdBySubmissionId(@Param("submissionId") Long submissionId);

    boolean existsByGradingRun_Submission_IdAndTest_Id(
            Long submissionId,
            Long testId);

    Optional<SubmissionTest>
    findByGradingRun_Submission_IdAndTest_IdAndGradingRunId(
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

    @Query("""
        SELECT st
        FROM SubmissionTest st
        JOIN FETCH st.test t
        JOIN FETCH st.gradingRun gr
        JOIN FETCH gr.submission sub
        WHERE sub.id = :submissionId
        ORDER BY gr.id ASC, t.displayOrder ASC
        """)
    List<SubmissionTest> findBySubmission_Id(
            @Param("submissionId") Long submissionId);
}