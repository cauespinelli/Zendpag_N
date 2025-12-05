package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a notification is sent.
 */
public class NotificationEvent extends DomainEvent {

    @NotNull
    private final String notificationId;

    @NotNull
    private final String recipientId;

    @NotNull
    private final String recipientType;

    @NotNull
    private final String channel;

    @NotNull
    private final String notificationType;

    private final String subject;
    private final String content;
    private final String status;
    private final Instant sentAt;
    private final Map<String, Object> templateData;

    public NotificationEvent(String notificationId,
                            String recipientId,
                            String recipientType,
                            String channel,
                            String notificationType,
                            String subject,
                            String content,
                            String status,
                            Map<String, Object> templateData,
                            String correlationId) {

        super("notification", notificationId, "Notification", correlationId, null, 1L);

        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.channel = channel;
        this.notificationType = notificationType;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.sentAt = Instant.now();
        this.templateData = templateData != null ? new HashMap<>(templateData) : new HashMap<>();

        this.withMetadata("recipient_id", recipientId)
            .withMetadata("channel", channel)
            .withMetadata("notification_type", notificationType)
            .withMetadata("status", status);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notificationId);
        data.put("recipientId", recipientId);
        data.put("recipientType", recipientType);
        data.put("channel", channel);
        data.put("notificationType", notificationType);
        data.put("subject", subject);
        data.put("content", content);
        data.put("status", status);
        data.put("sentAt", sentAt);
        data.put("templateData", templateData);
        return data;
    }

    public String getNotificationId() { return notificationId; }
    public String getRecipientId() { return recipientId; }
    public String getRecipientType() { return recipientType; }
    public String getChannel() { return channel; }
    public String getNotificationType() { return notificationType; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
    public Map<String, Object> getTemplateData() { return new HashMap<>(templateData); }

    @Override
    public String getEventSummary() {
        return String.format("Notification[id=%s, channel=%s, type=%s]", notificationId, channel, notificationType);
    }
}
