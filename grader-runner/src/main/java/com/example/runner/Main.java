package com.example.runner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.runner.model.GradingJobRequest;
import com.example.runner.model.GradingResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Main {
    public static void main(String[] args) throws IOException {
        String json = Files.readString(Path.of("/input/request.json"), StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GradingJobRequest request = objectMapper.readValue(json, GradingJobRequest.class);

        GradingService gradingService = new GradingService();
        try {
    GradingResultResponse response = gradingService.grade(request);
    System.out.println(objectMapper.writeValueAsString(response));
    } catch (IOException e) {
        System.err.println("Grading failed: " + e.getMessage());
        System.exit(1);
    }
        
    }
}
