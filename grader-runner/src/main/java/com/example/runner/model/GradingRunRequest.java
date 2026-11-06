package com.example.runner.model;

import java.time.LocalDateTime;

public record GradingRunRequest(
    Long gradingRunId,
    String status,
    Double gradingRunGrade,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt
) {}
