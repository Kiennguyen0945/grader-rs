package com.example.runner.model;

public record SubmissionResponse(
    Long submissionId,
    String status,
    Double submissionGrade
) {}
