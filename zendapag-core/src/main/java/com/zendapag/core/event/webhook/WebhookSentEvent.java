package com.zendapag.core.event.webhook;

import com.zendapag.core.entity.Webhook;
import com.zendapag.core.event.BaseEvent;

public class WebhookSentEvent extends BaseEvent {
    private final Webhook webhook;
    private final boolean success;
    private final Integer httpStatus;
    private final String response;

    public WebhookSentEvent(Webhook webhook, boolean success, Integer httpStatus, String response, String correlationId) {
        super("webhook.sent", webhook.getMerchant(), correlationId);
        this.webhook = webhook;
        this.success = success;
        this.httpStatus = httpStatus;
        this.response = response;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponse() {
        return response;
    }
}