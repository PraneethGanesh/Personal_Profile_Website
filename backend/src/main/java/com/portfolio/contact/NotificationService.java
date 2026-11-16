package com.portfolio.contact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Adapted from the existing DRMS NotificationService.sendSingleEmail. */
@Service
@Profile("worker")
public class NotificationService {
    private final JavaMailSender mailSender;
    private final String sender;
    private final String recipient;
    public NotificationService(JavaMailSender mailSender, @Value("${contact.mail-from}") String sender,
                               @Value("${contact.mail-to}") String recipient,
                               @Value("${spring.mail.username:}") String username,
                               @Value("${spring.mail.properties.mail.smtp.auth}") boolean authenticated) {
        Assert.hasText(sender, "MAIL_FROM is required");
        Assert.hasText(recipient, "MAIL_TO is required");
        Assert.isTrue(!authenticated || sender.equalsIgnoreCase(username), "MAIL_FROM must match MAIL_USERNAME");
        this.mailSender = mailSender; this.sender = sender; this.recipient = recipient;
    }
    // Synchronous: propagate failures to Kafka; acknowledge only after SMTP send returns.
    public void sendSingleEmail(ContactMessageSubmitted event) {
        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setReplyTo(event.contact().email());
        message.setSubject("Portfolio contact [" + event.submissionId() + "]");
        message.setText("Submission: " + event.submissionId() + "\nName: " + event.contact().name()
                + "\nEmail: " + event.contact().email() + "\n\n" + event.contact().message());
        mailSender.send(message);
    }
}
