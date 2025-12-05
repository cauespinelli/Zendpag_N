package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when an SMS is sent.
 */
public class SmsSentEvent extends DomainEvent {

    @NotNull
    private final String smsId;

    @NotNull
    private final String phoneNumber;

    @NotNull
    private final String message;

    private final String status;
    private final String provider;
    private final String providerMessageId;
    private final Instant sentAt;
    private final int segmentCount;
    private final String templateName;

    public SmsSentEvent(String smsId,
                       String phoneNumber,
                       String message,
                       String status,
                       String provider,
                       String providerMessageId,
                       int segmentCount,
                       String templateName,
                       String correlationId) {

        super("sms_sent", smsId, "SMS", correlationId, null, 1L);

        this.smsId = smsId;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.status = status;
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.sentAt = Instant.now();
        this.segmentCount = segmentCount;
        this.templateName = templateName;

        this.withMetadata("phone_number", maskPhoneNumber(phoneNumber))
            .withMetadata("status", status)
            .withMetadata("provider", provider)
            .withMetadata("segments", segmentCount);
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("smsId", smsId);
        data.put("phoneNumber", maskPhoneNumber(phoneNumber));
        data.put("message", message);
        data.put("status", status);
        data.put("provider", provider);
        data.put("providerMessageId", providerMessageId);
        data.put("sentAt", sentAt);
        data.put("segmentCount", segmentCount);
        data.put("templateName", templateName);
        return data;
    }

    public String getSmsId() { return smsId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderMessageId() { return providerMessageId; }
    public Instant getSentAt() { return sentAt; }
    public int getSegmentCount() { return segmentCount; }
    public String getTemplateName() { return templateName; }

    @Override
    public String getEventSummary() {
        return String.format("SmsSent[id=%s, to=%s, status=%s]", smsId, maskPhoneNumber(phoneNumber), status);
    }
}
