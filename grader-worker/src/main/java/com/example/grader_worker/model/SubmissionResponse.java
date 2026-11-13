package com.example.grader_worker.model;

public record SubmissionResponse(
    Long submissionId,
    String status,
    Double submissionGrade
) {}
