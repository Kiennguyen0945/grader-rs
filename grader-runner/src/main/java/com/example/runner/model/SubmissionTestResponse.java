package com.example.runner.model;

public record SubmissionTestResponse(
    Long gradingRunId,
    Long testId,
    String status,
    String actualOutput,
    Long memoryUsedKib,
    Long executionTimeMs
) {}
