package com.example.grader_worker.gradingrun;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingRunRepository extends JpaRepository<GradingRun, Long> {
    boolean existsByIdAndSubmission_Id(Long gradingRunId, Long submissionId);
}
