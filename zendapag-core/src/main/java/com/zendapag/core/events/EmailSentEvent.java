package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when an email is sent.
 */
public class EmailSentEvent extends DomainEvent {

    @NotNull
    private final String emailId;

    @NotNull
    private final String to;

    @NotNull
    private final String subject;

    private final String from;
    private final List<String> cc;
    private final List<String> bcc;
    private final String templateName;
    private final String status;
    private final Instant sentAt;
    private final String messageId;
    private final String provider;

    public EmailSentEvent(String emailId,
                         String to,
                         String from,
                         String subject,
                         List<String> cc,
                         List<String> bcc,
                         String templateName,
                         String status,
                         String messageId,
                         String provider,
                         String correlationId) {

        super("email_sent", emailId, "Email", correlationId, null, 1L);

        this.emailId = emailId;
        this.to = to;
        this.from = from;
        this.subject = subject;
        this.cc = cc;
        this.bcc = bcc;
        this.templateName = templateName;
        this.status = status;
        this.sentAt = Instant.now();
        this.messageId = messageId;
        this.provider = provider;

        this.withMetadata("to", to)
            .withMetadata("template", templateName)
            .withMetadata("status", status)
            .withMetadata("provider", provider);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("emailId", emailId);
        data.put("to", to);
        data.put("from", from);
        data.put("subject", subject);
        data.put("cc", cc);
        data.put("bcc", bcc);
        data.put("templateName", templateName);
        data.put("status", status);
        data.put("sentAt", sentAt);
        data.put("messageId", messageId);
        data.put("provider", provider);
        return data;
    }

    public String getEmailId() { return emailId; }
    public String getTo() { return to; }
    public String getFrom() { return from; }
    public String getSubject() { return subject; }
    public List<String> getCc() { return cc; }
    public List<String> getBcc() { return bcc; }
    public String getTemplateName() { return templateName; }
    public String getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
    public String getMessageId() { return messageId; }
    public String getProvider() { return provider; }

    @Override
    public String getEventSummary() {
        return String.format("EmailSent[id=%s, to=%s, status=%s]", emailId, to, status);
    }
}
