package com.portfolio.contact;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfiguration {
    @Bean NewTopic contactTopic(@Value("${contact.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).config("retention.ms", "604800000").build();
    }
    @Bean NewTopic contactDeadLetterTopic(@Value("${contact.topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(1).replicas(1).config("retention.ms", "1209600000").build();
    }
}
