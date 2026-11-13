package com.example.grader_worker.model;

import java.time.LocalDateTime;

public record GradingRunResponse(
    Long gradingRunId,
    String status,
    Double gradingRunGrade,
    LocalDateTime startedAt,
    LocalDateTime finishedAt
) {}
