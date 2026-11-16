package com.portfolio.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class ContactWorker {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContactWorker.class);
    private final ObjectMapper json;
    private final Validator validator;
    private final NotificationService notifications;
    public ContactWorker(ObjectMapper json, Validator validator, NotificationService notifications) {
        this.json = json; this.validator = validator; this.notifications = notifications;
    }
    @KafkaListener(id = "contact-mail", topics = "${contact.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void receive(String payload, Acknowledgment acknowledgment) {
        ContactMessageSubmitted event;
        try {
            event = json.readValue(payload, ContactMessageSubmitted.class);
            if (event == null || !"ContactMessageSubmitted".equals(event.eventType()) || event.version() != 1
                    || event.submissionId() == null || event.submittedAt() == null || event.contact() == null
                    || !validator.validate(event.contact()).isEmpty()) throw new IllegalArgumentException("Invalid contact event");
        } catch (Exception ex) { throw new IllegalArgumentException("Invalid contact event", ex); }
        notifications.sendSingleEmail(event);
        acknowledgment.acknowledge();
        LOG.info("SMTP accepted contact submission {}; offset acknowledged", event.submissionId());
    }
}
