package com.example.demo.rabbitmq;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.grading.GradingService;
import com.example.demo.submission.Submission;
import com.example.demo.submission.SubmissionRepository;

@Component
public class SubmissionConsumer {
    private final SubmissionRepository submissionRepository;
    private final GradingService gradingService;
    public SubmissionConsumer(
        SubmissionRepository submissionRepository
        , GradingService gradingService) {
            this.submissionRepository = submissionRepository;
            this.gradingService = gradingService;
    }
    @RabbitListener(queues = "submission.queue")
    public void receiveMessage(String message) {
        Submission submission = submissionRepository.findById(Long.parseLong(message))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found."));
        try {
            gradingService.grade(submission);
            submissionRepository.save(submission);
        } catch (IOException e) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
        }
            
    }
}
