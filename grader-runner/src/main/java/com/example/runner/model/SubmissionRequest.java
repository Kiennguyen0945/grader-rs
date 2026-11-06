package com.example.runner.model;

public record SubmissionRequest(
    Long submissionId,
    String sourceCode,
    String status,
    Double submissionGrade
) {}
