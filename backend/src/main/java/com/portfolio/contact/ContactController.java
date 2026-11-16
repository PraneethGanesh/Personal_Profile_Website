package com.portfolio.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@Profile("api")
public class ContactController {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    public ContactController(KafkaTemplate<String, String> kafka, ObjectMapper json,
                             @Value("${contact.topic}") String topic) {
        this.kafka = kafka; this.json = json; this.topic = topic;
    }
    @PostMapping("/api/contact")
    public ResponseEntity<?> submit(@Valid @RequestBody ContactRequest request) {
        var event = ContactMessageSubmitted.create(request);
        try {
            kafka.send(topic, event.submissionId().toString(), json.writeValueAsString(event)).get(12, TimeUnit.SECONDS);
            return ResponseEntity.accepted().body(Map.of("submissionId", event.submissionId()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Acceptance could not be confirmed");
        } catch (Exception ex) {
            // A timeout is ambiguous: Kafka may have accepted the event. Never claim 202 here.
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Acceptance could not be confirmed");
        }
    }
}
