package com.portfolio.contact;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageSubmitted(String eventType, int version, UUID submissionId,
                                     Instant submittedAt, ContactRequest contact) {
    public static ContactMessageSubmitted create(ContactRequest contact) {
        return new ContactMessageSubmitted("ContactMessageSubmitted", 1, UUID.randomUUID(), Instant.now(), contact);
    }
}
