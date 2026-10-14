package com.example.demo.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;


@Component 
public class SubmissionProducer {
    private final RabbitTemplate rabbitTemplate;
    public SubmissionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    public void send(String message) {
        rabbitTemplate.convertAndSend(
            "submission.exchange",
             "submission.key",
              message
        );
    }
}
