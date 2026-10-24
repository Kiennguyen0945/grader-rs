package com.example.demo.submissiontest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionTestRepository
        extends JpaRepository<SubmissionTest, Long> {

    @Query("""
        SELECT MAX(st.gradingRunId)
        FROM SubmissionTest st
        WHERE st.submission.id = :submissionId
        """)
    Long findMaxGradingRunIdBySubmissionId(
            @Param("submissionId") Long submissionId);

    boolean existsBySubmission_IdAndTest_Id(
            Long submissionId,
            Long testId);

    Optional<SubmissionTest>
    findBySubmission_IdAndTest_IdAndGradingRunId(
            Long submissionId,
            Long testId,
            Long gradingRunId);

    @Query("""
        SELECT st
        FROM SubmissionTest st
        JOIN st.test t
        WHERE st.submission.id = :submissionId
          AND st.gradingRunId = :gradingRunId
        ORDER BY t.displayOrder ASC
        """)
    List<SubmissionTest> findBySubmission_IdAndGradingRunId(
            @Param("submissionId") Long submissionId,
            @Param("gradingRunId") Long gradingRunId);

    @Query("""
        SELECT st
        FROM SubmissionTest st
        JOIN st.test t
        WHERE st.submission.id = :submissionId
        ORDER BY st.gradingRunId ASC, t.displayOrder ASC
        """)
    List<SubmissionTest> findBySubmission_Id(
            @Param("submissionId") Long submissionId);
}