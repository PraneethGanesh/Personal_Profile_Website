package com.portfolio.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"contact.rate-limit-per-minute=100", "contact.retry-delay-ms=100", "contact.retries=2"})
@AutoConfigureMockMvc
@ActiveProfiles({"api", "worker"})
@EmbeddedKafka(partitions = 1, topics = {"portfolio.contact.submitted.v1", "portfolio.contact.submitted.v1.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@org.springframework.test.annotation.DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContactFlowTest {
    static final TestMailbox mailbox = new TestMailbox();
    @DynamicPropertySource static void mail(DynamicPropertyRegistry properties) {
        properties.add("spring.mail.host", () -> "127.0.0.1");
        properties.add("spring.mail.port", mailbox::port);
        properties.add("spring.mail.properties.mail.smtp.auth", () -> false);
        properties.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
        properties.add("spring.mail.properties.mail.smtp.starttls.required", () -> false);
        // Profiles combined only for integration tests: keep MockMvc's web context.
        properties.add("spring.main.web-application-type", () -> "servlet");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired KafkaListenerEndpointRegistry registry;
    @Autowired org.springframework.core.env.Environment env;

    String submit(String name) throws Exception {
        var result = mvc.perform(post("/api/contact").contentType("application/json")
                .content(json.writeValueAsString(new ContactRequest(name, "visitor@example.test", "Hello from Kafka"))))
                .andExpect(status().isAccepted()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("submissionId").asText();
    }
    @Test @Order(1) void validAndInvalidSubmission() throws Exception {
        int before = mailbox.messages.size();
        String id = submit("Visitor");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(mailbox.messages).hasSize(before + 1));
        String email = mailbox.messages.getLast();
        assertThat(email).contains(id, "Reply-To: visitor@example.test", "From: portfolio@example.test", "To: inbox@example.test", "Hello from Kafka");
        for (var invalid : List.of(new ContactRequest("", "bad-email", ""),
                new ContactRequest("x".repeat(121), "v@example.test", "hello"),
                new ContactRequest("Visitor", "v@example.test", "x".repeat(5001)),
                new ContactRequest("Visitor", "v@example.test\r\nBcc: attacker@example.test", "hello"))) {
            mvc.perform(post("/api/contact").contentType("application/json").content(json.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/contact").contentType("application/json").content("x".repeat(32769)))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(options("/api/contact").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/contact").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test @Order(2) void stoppedWorkerDrainsQueuedSubmissionAfterRestart() throws Exception {
        var container = registry.getListenerContainer("contact-mail");
        var stopped = new CountDownLatch(1);
        container.stop(stopped::countDown);
        assertThat(stopped.await(15, TimeUnit.SECONDS)).isTrue();
        int before = mailbox.messages.size();
        String id;
        try {
            id = submit("Queued while worker stopped");
            await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> assertThat(mailbox.messages).hasSize(before));
        } finally { container.start(); }
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(mailbox.messages).hasSize(before + 1);
            assertThat(mailbox.messages.getLast()).contains(id);
        });
    }
    @Test @Order(3) void exhaustedSmtpRetriesReachDeadLetterTopic() throws Exception {
        int attempts = mailbox.attempts.get();
        int before = mailbox.messages.size();
        mailbox.reject.set(true);
        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getRequiredProperty("spring.kafka.bootstrap-servers"),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"), new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of("portfolio.contact.submitted.v1.DLT"));
            String id = submit("SMTP retry test");
            var found = new AtomicBoolean();
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (var record : consumer.poll(Duration.ofMillis(200))) {
                    if (record.value().contains(id)) found.set(true);
                }
                return found.get();
            });
            assertThat(mailbox.attempts.get() - attempts).isEqualTo(3);
            assertThat(mailbox.messages).hasSize(before);
        } finally { mailbox.reject.set(false); }
        String recoveredId = submit("Worker continues after DLT");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(mailbox.messages.getLast()).contains(recoveredId));
    }
    @AfterAll static void closeMailbox() throws IOException { mailbox.close(); }

    /** A real loopback SMTP receiver. Accepts DATA or returns a transient SMTP 451. */
    static class TestMailbox implements AutoCloseable {
        final ServerSocket socket;
        final List<String> messages = new CopyOnWriteArrayList<>();
        final AtomicBoolean reject = new AtomicBoolean();
        final AtomicInteger attempts = new AtomicInteger();
        TestMailbox() {
            try { socket = new ServerSocket(0, 20, InetAddress.getLoopbackAddress()); }
            catch (IOException ex) { throw new UncheckedIOException(ex); }
            var thread = new Thread(() -> {
                while (!socket.isClosed()) {
                    try (var client = socket.accept()) {
                        client.setSoTimeout(10000);
                        var in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                        var out = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8);
                        out.print("220 localhost ESMTP\r\n"); out.flush();
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (line.startsWith("DATA")) {
                                attempts.incrementAndGet();
                                if (reject.get()) { out.print("451 Temporary failure\r\n"); out.flush(); continue; }
                                out.print("354 End with dot\r\n"); out.flush();
                                var body = new StringBuilder();
                                while ((line = in.readLine()) != null && !line.equals(".")) body.append(line).append("\n");
                                messages.add(body.toString()); out.print("250 Accepted\r\n");
                            } else if (line.startsWith("QUIT")) { out.print("221 Bye\r\n"); out.flush(); break; }
                            else { out.print("250 OK\r\n"); }
                            out.flush();
                        }
                    } catch (IOException ex) { if (!socket.isClosed()) throw new UncheckedIOException(ex); }
                }
            }, "local-test-mailbox");
            thread.setDaemon(true); thread.start();
        }
        int port() { return socket.getLocalPort(); }
        public void close() throws IOException { socket.close(); }
    }
}
