package com.portfolio.contact;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContactApiTest {
    @Test void acceptanceWaitsForKafkaAcknowledgmentAndFailuresReturn503() throws Exception {
        @SuppressWarnings("unchecked") var kafka = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        var ack = new CompletableFuture<SendResult<String, String>>();
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(ack);
        var controller = new ContactController(kafka, JsonMapper.builder().findAndAddModules().build(), "contact");
        var response = CompletableFuture.supplyAsync(() -> controller.submit(new ContactRequest("Visitor", "visitor@example.test", "Hello")));
        verify(kafka, timeout(3000)).send(eq("contact"), anyString(), contains("ContactMessageSubmitted"));
        assertThat(response).isNotDone();
        ack.complete(null);
        assertThat(response.get().getStatusCode().value()).isEqualTo(202);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
        assertThatThrownBy(() -> controller.submit(new ContactRequest("Visitor", "visitor@example.test", "Hello")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
    }
    @Test void rateLimitRejectsWithoutTrustingForwardedAddress() throws Exception {
        var guard = new ContactGuard(1);
        var request = new MockHttpServletRequest("POST", "/api/contact");
        request.setServletPath("/api/contact"); request.setContent("{}".getBytes());
        var first = new MockHttpServletResponse();
        guard.doFilter(request, first, new MockFilterChain());
        assertThat(first.getStatus()).isEqualTo(200);
        var secondRequest = new MockHttpServletRequest("POST", "/api/contact");
        secondRequest.setServletPath("/api/contact"); secondRequest.addHeader("X-Forwarded-For", "203.0.113.2");
        var second = new MockHttpServletResponse();
        guard.doFilter(secondRequest, second, new MockFilterChain());
        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isEqualTo("60");
    }
}
