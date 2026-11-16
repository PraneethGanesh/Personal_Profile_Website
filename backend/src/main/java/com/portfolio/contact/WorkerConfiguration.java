package com.portfolio.contact;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Profile("worker")
public class WorkerConfiguration {
    @Bean DefaultErrorHandler contactErrorHandler(KafkaTemplate<String, String> kafka,
            @Value("${contact.retry-delay-ms}") long delay, @Value("${contact.retries}") long retries) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(delay, retries));
        handler.addNotRetryableExceptions(IllegalArgumentException.class, MailAuthenticationException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
