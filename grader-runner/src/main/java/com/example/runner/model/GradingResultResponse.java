package com.example.runner.model;

import java.util.List;

public record GradingResultResponse(
    SubmissionResponse submission,
    GradingRunResponse gradingRun,
    List<SubmissionTestResponse> submissionTestResponses
) {}
