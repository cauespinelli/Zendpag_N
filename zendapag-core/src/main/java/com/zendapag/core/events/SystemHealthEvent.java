package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published for system health monitoring.
 */
public class SystemHealthEvent extends DomainEvent {

    @NotNull
    private final String serviceName;

    @NotNull
    private final String status;

    private final String component;
    private final String healthDetails;
    private final Map<String, Object> metrics;
    private final Instant checkedAt;
    private final long responseTimeMs;
    private final String version;

    public SystemHealthEvent(String serviceName,
                            String status,
                            String component,
                            String healthDetails,
                            Map<String, Object> metrics,
                            long responseTimeMs,
                            String version,
                            String correlationId) {

        super("system_health", serviceName + "-" + Instant.now().toEpochMilli(), "System", correlationId, null, 1L);

        this.serviceName = serviceName;
        this.status = status;
        this.component = component;
        this.healthDetails = healthDetails;
        this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        this.checkedAt = Instant.now();
        this.responseTimeMs = responseTimeMs;
        this.version = version;

        this.withMetadata("service_name", serviceName)
            .withMetadata("status", status)
            .withMetadata("component", component)
            .withMetadata("response_time_ms", responseTimeMs);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceName", serviceName);
        data.put("status", status);
        data.put("component", component);
        data.put("healthDetails", healthDetails);
        data.put("metrics", metrics);
        data.put("checkedAt", checkedAt);
        data.put("responseTimeMs", responseTimeMs);
        data.put("version", version);
        return data;
    }

    public String getServiceName() { return serviceName; }
    public String getStatus() { return status; }
    public String getComponent() { return component; }
    public String getHealthDetails() { return healthDetails; }
    public Map<String, Object> getMetrics() { return new HashMap<>(metrics); }
    public Instant getCheckedAt() { return checkedAt; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getVersion() { return version; }

    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status) || "HEALTHY".equalsIgnoreCase(status);
    }

    @Override
    public String getEventSummary() {
        return String.format("SystemHealth[service=%s, status=%s, time=%dms]", serviceName, status, responseTimeMs);
    }
}
