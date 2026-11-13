package com.example.grader_worker.model;

import java.util.List;

public record GradingResultResponse(
    SubmissionResponse submission,
    GradingRunResponse gradingRun,
    List<SubmissionTestResponse> submissionTestResponses
) {}
