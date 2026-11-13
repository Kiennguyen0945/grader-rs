package com.example.grader_worker.model;

public record SubmissionRequest(
    Long submissionId,
    String sourceCode,
    String status,
    Double submissionGrade
) {}
