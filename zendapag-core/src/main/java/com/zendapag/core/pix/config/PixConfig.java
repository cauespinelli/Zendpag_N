package com.zendapag.core.pix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "zendapag.pix")
@Data
public class PixConfig {

    private Participant participant = new Participant();
    private Certificate certificate = new Certificate();
    private QrCode qrCode = new QrCode();
    private Webhook webhook = new Webhook();
    private Reconciliation reconciliation = new Reconciliation();
    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();

    @Data
    public static class Participant {
        private String baseUrl = "https://api.participant.pix.bcb.gov.br";
        private String sandboxUrl = "https://api-sandbox.participant.pix.bcb.gov.br";
        private String participantCode;
        private String version = "v1";
        private boolean sandbox = true;
    }

    @Data
    public static class Certificate {
        private String keystorePath;
        private String keystorePassword;
        private String keyAlias;
        private String keyPassword;
        private CertificateType type = CertificateType.A1;
        private String truststorePath;
        private String truststorePassword;
        private boolean validateHostname = true;
    }

    @Data
    public static class QrCode {
        private int defaultExpirationMinutes = 30;
        private int maxExpirationMinutes = 1440; // 24 hours
        private String merchantCity = "SAO PAULO";
        private String merchantCategoryCode = "0000";
        private ImageConfig image = new ImageConfig();
    }

    @Data
    public static class ImageConfig {
        private int size = 256;
        private String format = "PNG";
        private int margin = 4;
        private String backgroundColor = "#FFFFFF";
        private String foregroundColor = "#000000";
    }

    @Data
    public static class Webhook {
        private String callbackUrl;
        private String secret;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean validateSignature = true;
        private int maxRetries = 3;
        private Duration retryDelay = Duration.ofMinutes(5);
    }

    @Data
    public static class Reconciliation {
        private String cronExpression = "0 0 1 * * ?"; // Daily at 1 AM
        private int daysToReconcile = 7;
        private boolean autoResolveDiscrepancies = false;
        private String reportPath = "/tmp/pix-reconciliation";
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private double multiplier = 2.0;
        private Duration maxDelay = Duration.ofMinutes(10);
    }

    @Data
    public static class Timeout {
        private Duration connection = Duration.ofSeconds(30);
        private Duration read = Duration.ofSeconds(60);
        private Duration write = Duration.ofSeconds(60);
    }

    public enum CertificateType {
        A1, A3
    }

    // Helper methods
    public String getEffectiveBaseUrl() {
        return sandbox ? participant.sandboxUrl : participant.baseUrl;
    }

    public String buildEndpointUrl(String endpoint) {
        return getEffectiveBaseUrl() + "/" + participant.version + endpoint;
    }

    public Map<String, String> getDefaultHeaders() {
        return Map.of(
            "Content-Type", "application/json",
            "Accept", "application/json",
            "User-Agent", "Zendapag-PIX-Client/1.0"
        );
    }
}