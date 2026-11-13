package com.example.grader_worker.model;
public record SubmissionTestResponse(
    Long gradingRunId,
    Long testId,
    String status,
    String actualOutput,
    Long memoryUsedKib,
    Long executionTimeMs
) {}
