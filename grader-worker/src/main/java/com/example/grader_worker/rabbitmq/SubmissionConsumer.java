package com.example.grader_worker.rabbitmq;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


import com.example.grader_worker.grading.GradingService;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;

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
            () -> new IllegalStateException("Submission not found: " + message));
        try {
            gradingService.grade(submission);
            submissionRepository.save(submission);
        } catch (IOException e) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
        }
            
    }
}
