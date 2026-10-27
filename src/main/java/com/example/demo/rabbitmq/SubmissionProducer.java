package com.example.demo.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.submission.SubmissionService.SubmissionGradingMessage;


@Component 
public class SubmissionProducer {
    private final RabbitTemplate rabbitTemplate;
    public SubmissionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    public void send(Long submissionId, Long gradingRunId) {
        SubmissionGradingMessage message = new SubmissionGradingMessage(submissionId, gradingRunId);
        rabbitTemplate.convertAndSend(
            "submission.exchange",
             "submission.key",
            message
        );
    }
}
