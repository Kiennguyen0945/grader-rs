package com.example.grader_worker.submissiontest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubmissionTestRepository extends JpaRepository<SubmissionTest, Long> {
    @Query(
        """
        SELECT MAX(st.gradingRunId)
        From SubmissionTest st
        Where st.submission.id = :submissionId
    """)
    Long findMaxGradingRunIdBySubmissionId(Long submissionId);
    boolean existsBySubmission_IdAndTest_Id(Long submissionId, Long testId);
    Optional<SubmissionTest>
    findBySubmission_IdAndTest_IdAndGradingRunId(
            Long submissionId,
            Long testId,
            Long gradingRunId);
    List<SubmissionTest>
        findBySubmission_IdAndGradingRunId(Long submissionId, Long gradingRunId);
}
