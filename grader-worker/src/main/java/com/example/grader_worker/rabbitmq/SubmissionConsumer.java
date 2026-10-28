package com.example.grader_worker.rabbitmq;


import java.time.LocalDateTime;

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
        GradingRun gradingRun = gradingRunRepository.findByIdWithSubmission(message.gradingRunId())
        .orElseThrow(() -> new IllegalStateException("GradingRun not found: " + message.gradingRunId()));
        Submission submission = gradingRun.getSubmission();
        if (!submission.getId().equals(message.submissionId())) {
            throw new IllegalStateException(
                "GradingRun does not belong to Submission"
            );
        }
        try {
            gradingService.grade(submission, gradingRun);
            submissionRepository.save(submission);
            gradingRunRepository.save(gradingRun);
        } catch (Exception e) {
            submission.setStatus(Submission.Status.SYSTEM_ERROR);
            gradingRun.setStatus(GradingRun.Status.FAILED);
            gradingRun.setFinishedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            gradingRunRepository.save(gradingRun);
            throw new IllegalStateException("Grading failed", e);
        }
            
    }
    public record SubmissionGradingMessage(
        Long submissionId,
        Long gradingRunId
    ) {}
}
