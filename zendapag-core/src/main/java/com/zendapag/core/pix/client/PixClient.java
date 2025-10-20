package com.zendapag.core.pix.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.pix.config.PixConfig;
import com.zendapag.core.pix.dto.PixPaymentRequest;
import com.zendapag.core.pix.dto.PixPaymentResponse;
import com.zendapag.core.pix.security.PixCertificateManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retryable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PixClient {

    private final PixConfig pixConfig;
    private final PixCertificateManager certificateManager;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public PixClient(PixConfig pixConfig, PixCertificateManager certificateManager, ObjectMapper objectMapper) {
        this.pixConfig = pixConfig;
        this.certificateManager = certificateManager;
        this.objectMapper = objectMapper;
        this.restTemplate = createConfiguredRestTemplate();
    }

    private RestTemplate createConfiguredRestTemplate() {
        RestTemplate template = new RestTemplate();

        // Configure SSL
        try {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                        super.prepareConnection(connection, httpMethod);

                        if (connection instanceof HttpsURLConnection) {
                            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                            httpsConnection.setSSLSocketFactory(certificateManager.getSSLSocketFactory());
                            httpsConnection.setHostnameVerifier(certificateManager.getHostnameVerifier());
                        }

                        // Set timeouts
                        connection.setConnectTimeout((int) pixConfig.getTimeout().getConnection().toMillis());
                        connection.setReadTimeout((int) pixConfig.getTimeout().getRead().toMillis());
                    }
                };

            template.setRequestFactory(factory);
        } catch (Exception e) {
            log.error("Failed to configure RestTemplate SSL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure PIX client SSL", e);
        }

        return template;
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        fallbackMethod = "fallbackCreatePixPayment"
    )
    @CircuitBreaker(name = "pix-client", fallbackMethod = "fallbackCreatePixPayment")
    public PixPaymentResponse createPixPayment(PixPaymentRequest request) {
        log.info("Creating PIX payment for txId: {}", request.getTxId());

        try {
            String endpoint = "/payments";
            String url = pixConfig.buildEndpointUrl(endpoint);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create signed request
            HttpHeaders headers = createSignedHeaders("POST", endpoint, requestBody);
            HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

            log.debug("Sending PIX payment request to: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class
            );

            // Validate response signature
            validateResponseSignature(response);

            PixPaymentResponse pixResponse = objectMapper.readValue(response.getBody(), PixPaymentResponse.class);

            log.info("PIX payment created successfully: txId={}, status={}",
                pixResponse.getTxId(), pixResponse.getStatus());

            return pixResponse;

        } catch (Exception e) {
            log.error("Error creating PIX payment for txId {}: {}", request.getTxId(), e.getMessage(), e);
            throw new PixClientException("Failed to create PIX payment", e);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 3)
    @CircuitBreaker(name = "pix-client")
    public PixPaymentResponse checkPaymentStatus(String txId) {
        log.debug("Checking PIX payment status for txId: {}", txId);

        try {
            String endpoint = "/payments/" + txId;
            String url = pixConfig.buildEndpointUrl(endpoint);

            // Create signed request
            HttpHeaders headers = createSignedHeaders("GET", endpoint, null);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class
            );

            // Validate response signature
            validateResponseSignature(response);

            PixPaymentResponse pixResponse = objectMapper.readValue(response.getBody(), PixPaymentResponse.class);

            log.debug("PIX payment status retrieved: txId={}, status={}",
                pixResponse.getTxId(), pixResponse.getStatus());

            return pixResponse;

        } catch (Exception e) {
            log.error("Error checking PIX payment status for txId {}: {}", txId, e.getMessage(), e);
            throw new PixClientException("Failed to check PIX payment status", e);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 3)
    @CircuitBreaker(name = "pix-client")
    public PixPaymentResponse cancelPixPayment(String txId, String reason) {
        log.info("Cancelling PIX payment for txId: {} reason: {}", txId, reason);

        try {
            String endpoint = "/payments/" + txId + "/cancel";
            String url = pixConfig.buildEndpointUrl(endpoint);

            Map<String, String> cancelRequest = Map.of("reason", reason);
            String requestBody = objectMapper.writeValueAsString(cancelRequest);

            // Create signed request
            HttpHeaders headers = createSignedHeaders("POST", endpoint, requestBody);
            HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class
            );

            // Validate response signature
            validateResponseSignature(response);

            PixPaymentResponse pixResponse = objectMapper.readValue(response.getBody(), PixPaymentResponse.class);

            log.info("PIX payment cancelled successfully: txId={}, status={}",
                pixResponse.getTxId(), pixResponse.getStatus());

            return pixResponse;

        } catch (Exception e) {
            log.error("Error cancelling PIX payment for txId {}: {}", txId, e.getMessage(), e);
            throw new PixClientException("Failed to cancel PIX payment", e);
        }
    }

    public PixPaymentResponse[] listPayments(Instant startDate, Instant endDate, int page, int size) {
        log.debug("Listing PIX payments from {} to {}", startDate, endDate);

        try {
            String endpoint = "/payments";
            Map<String, String> params = new HashMap<>();
            params.put("startDate", startDate.toString());
            params.put("endDate", endDate.toString());
            params.put("page", String.valueOf(page));
            params.put("size", String.valueOf(size));

            String queryString = buildQueryString(params);
            String url = pixConfig.buildEndpointUrl(endpoint) + "?" + queryString;

            // Create signed request
            HttpHeaders headers = createSignedHeaders("GET", endpoint + "?" + queryString, null);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class
            );

            // Validate response signature
            validateResponseSignature(response);

            PixPaymentResponse[] payments = objectMapper.readValue(response.getBody(), PixPaymentResponse[].class);

            log.debug("Retrieved {} PIX payments", payments.length);

            return payments;

        } catch (Exception e) {
            log.error("Error listing PIX payments: {}", e.getMessage(), e);
            throw new PixClientException("Failed to list PIX payments", e);
        }
    }

    public boolean validatePixKey(String pixKey, PixPaymentRequest.PixKeyType keyType) {
        log.debug("Validating PIX key: {} type: {}", maskPixKey(pixKey), keyType);

        try {
            String endpoint = "/keys/validate";
            String url = pixConfig.buildEndpointUrl(endpoint);

            Map<String, String> validationRequest = Map.of(
                "pixKey", pixKey,
                "keyType", keyType.name()
            );
            String requestBody = objectMapper.writeValueAsString(validationRequest);

            // Create signed request
            HttpHeaders headers = createSignedHeaders("POST", endpoint, requestBody);
            HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class
            );

            // Parse validation response
            Map<String, Object> validationResponse = objectMapper.readValue(response.getBody(), Map.class);
            boolean isValid = (Boolean) validationResponse.get("valid");

            log.debug("PIX key validation result: {} valid: {}", maskPixKey(pixKey), isValid);

            return isValid;

        } catch (Exception e) {
            log.warn("PIX key validation failed for key {}: {}", maskPixKey(pixKey), e.getMessage());
            // Return false for validation failures to avoid blocking payments
            return false;
        }
    }

    private HttpHeaders createSignedHeaders(String httpMethod, String endpoint, String requestBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();

        // Set basic headers
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        // Add custom headers
        pixConfig.getDefaultHeaders().forEach(headers::set);

        // Add timestamp
        String timestamp = Instant.now().toString();
        headers.set("X-Timestamp", timestamp);

        // Add request ID
        String requestId = UUID.randomUUID().toString();
        headers.set("X-Request-ID", requestId);

        // Add participant code
        if (pixConfig.getParticipant().getParticipantCode() != null) {
            headers.set("X-Participant-Code", pixConfig.getParticipant().getParticipantCode());
        }

        // Create and add signature
        String signature = certificateManager.createRequestSignature(httpMethod, endpoint, requestBody, timestamp);
        headers.set("X-Signature", signature);

        // Add certificate fingerprint
        String fingerprint = certificateManager.getCertificateFingerprint();
        headers.set("X-Certificate-Fingerprint", fingerprint);

        return headers;
    }

    private void validateResponseSignature(ResponseEntity<String> response) throws Exception {
        String receivedSignature = response.getHeaders().getFirst("X-Signature");
        String timestamp = response.getHeaders().getFirst("X-Timestamp");
        String responseBody = response.getBody();

        if (receivedSignature != null && timestamp != null && responseBody != null) {
            String stringToVerify = "RESPONSE\n" + timestamp + "\n" + responseBody;

            if (!certificateManager.verifySignature(stringToVerify, receivedSignature)) {
                log.error("Response signature validation failed");
                throw new PixClientException("Invalid response signature");
            }

            log.debug("Response signature validated successfully");
        } else {
            log.warn("Response signature validation skipped - missing signature headers");
        }
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .reduce((param1, param2) -> param1 + "&" + param2)
            .orElse("");
    }

    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() <= 4) {
            return pixKey;
        }
        return pixKey.substring(0, 2) + "***" + pixKey.substring(pixKey.length() - 2);
    }

    // Fallback methods for Circuit Breaker
    public PixPaymentResponse fallbackCreatePixPayment(PixPaymentRequest request, Exception ex) {
        log.error("Fallback: PIX payment creation failed for txId {}: {}", request.getTxId(), ex.getMessage());

        PixPaymentResponse response = new PixPaymentResponse();
        response.setTxId(request.getTxId());
        response.setReferenceId(request.getReferenceId());
        response.setStatus(PixPaymentResponse.PixPaymentStatus.ERROR);
        response.setError(new PixPaymentResponse.ErrorInfo());
        response.getError().setCode("SERVICE_UNAVAILABLE");
        response.getError().setMessage("PIX service temporarily unavailable");
        response.getError().setTimestamp(Instant.now());

        return response;
    }

    // Custom exception class
    public static class PixClientException extends RuntimeException {
        public PixClientException(String message) {
            super(message);
        }

        public PixClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}