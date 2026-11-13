package com.example.grader_worker.model;

public record TestRequest(
    Long testId,
    String input,
    String expectedOutput,
    Long timeLimitMs,
    Long memoryLimitKib
) {}
