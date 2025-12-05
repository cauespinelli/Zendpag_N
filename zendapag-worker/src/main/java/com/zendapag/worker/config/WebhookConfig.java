package com.zendapag.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "zendapag.webhook")
@Data
public class WebhookConfig {

    private int maxRetries = 3;
    private int maxRetryAttempts = 5;
    private Duration retryDelay = Duration.ofMinutes(5);
    private Duration timeout = Duration.ofSeconds(30);
    private String secret;
    private boolean signatureValidation = true;
    private long initialRetryDelaySeconds = 1;
    private long maxRetryDelaySeconds = 30;

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public String getSecret() {
        return secret;
    }

    public boolean isSignatureValidation() {
        return signatureValidation;
    }

    public long getInitialRetryDelaySeconds() {
        return initialRetryDelaySeconds;
    }

    public long getMaxRetryDelaySeconds() {
        return maxRetryDelaySeconds;
    }
}
