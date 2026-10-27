package com.example.grader_worker.rabbitmq;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


import com.example.grader_worker.grading.GradingService;
import com.example.grader_worker.gradingrun.GradingRun;
import com.example.grader_worker.gradingrun.GradingRunRepository;
import com.example.grader_worker.submission.Submission;
import com.example.grader_worker.submission.SubmissionRepository;

@Component
public class SubmissionConsumer {
    private final SubmissionRepository submissionRepository;
    private final GradingRunRepository gradingRunRepository;
    private final GradingService gradingService;
    public SubmissionConsumer(
        SubmissionRepository submissionRepository,
        GradingRunRepository gradingRunRepository,
        GradingService gradingService) {
            this.submissionRepository = submissionRepository;
            this.gradingRunRepository = gradingRunRepository;
            this.gradingService = gradingService;
    }
    @RabbitListener(queues = "submission.queue")
    public void receiveMessage(SubmissionGradingMessage message) {
        Submission submission = submissionRepository.findById(message.submissionId())
        .orElseThrow(
            () -> new IllegalStateException("Submission not found: " + message.submissionId()));
        GradingRun gradingRun = gradingRunRepository.findById(message.gradingRunId())
        .orElseThrow(() -> new IllegalStateException("GradingRun not found: " + message.gradingRunId()));
        if(!gradingRunRepository.existsByIdAndSubmission_Id(message.gradingRunId(), message.submissionId()))
        {
            throw new IllegalStateException("GradingRun does not belong to Submission.");
        }
        try {
            gradingService.grade(submission, gradingRun);
            submissionRepository.save(submission);
            gradingRunRepository.save(gradingRun);
        } catch (IOException e) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
            gradingRun.setStatus(GradingRun.Status.FAILED);
            submissionRepository.save(submission);
            gradingRunRepository.save(gradingRun);
        }
            
    }
    public record SubmissionGradingMessage(
        Long submissionId,
        Long gradingRunId
    ) {}
}
