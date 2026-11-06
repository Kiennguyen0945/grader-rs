package com.example.runner.model;

public record TestRequest(
    Long testId,
    String input,
    String expectedOutput,
    Long timeLimitMs,
    Long memoryLimitKib
) {}
