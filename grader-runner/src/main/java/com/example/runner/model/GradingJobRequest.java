package com.example.runner.model;

import java.util.List;

public record GradingJobRequest(
    SubmissionRequest submission,
    GradingRunRequest gradingRun,
    List<TestRequest> tests
) {}
