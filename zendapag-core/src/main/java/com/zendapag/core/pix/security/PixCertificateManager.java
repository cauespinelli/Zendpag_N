package com.zendapag.core.pix.security;

import com.zendapag.core.pix.config.PixConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;
@Component
@Slf4j
public class PixCertificateManager {

    private final PixConfig pixConfig;
    private final boolean mockMode;
    private KeyStore keyStore;
    private KeyStore trustStore;
    private PrivateKey privateKey;
    private X509Certificate certificate;
    private SSLContext sslContext;

    @Autowired
    public PixCertificateManager(PixConfig pixConfig,
                                  @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.pixConfig = pixConfig;
        this.mockMode = "dev".equals(activeProfile) || "test".equals(activeProfile);

        if (mockMode) {
            log.info("PIX Certificate Manager running in MOCK mode (profile: {})", activeProfile);
            initializeMockMode();
        } else {
            initializeCertificates();
        }
    }

    private void initializeMockMode() {
        try {
            // Create a default SSLContext for mock mode
            sslContext = SSLContext.getDefault();
            log.info("PIX certificates initialized in MOCK mode - no real certificates loaded");
        } catch (Exception e) {
            log.warn("Failed to initialize mock SSL context: {}", e.getMessage());
        }
    }

    private void initializeCertificates() {
        try {
            loadKeyStore();
            loadTrustStore();
            loadPrivateKeyAndCertificate();
            createSSLContext();

            log.info("PIX certificates initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize PIX certificates: {}", e.getMessage(), e);
            throw new RuntimeException("PIX certificate initialization failed", e);
        }
    }

    private void loadKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        PixConfig.Certificate certConfig = pixConfig.getCertificate();

        if (certConfig.getKeystorePath() == null || certConfig.getKeystorePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Keystore path is required");
        }

        keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certConfig.getKeystorePath())) {
            keyStore.load(fis, certConfig.getKeystorePassword().toCharArray());
        }

        log.debug("Keystore loaded from: {}", certConfig.getKeystorePath());
    }

    private void loadTrustStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        PixConfig.Certificate certConfig = pixConfig.getCertificate();

        if (certConfig.getTruststorePath() != null && !certConfig.getTruststorePath().trim().isEmpty()) {
            trustStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(certConfig.getTruststorePath())) {
                trustStore.load(fis, certConfig.getTruststorePassword().toCharArray());
            }
            log.debug("Truststore loaded from: {}", certConfig.getTruststorePath());
        } else {
            // Use default system truststore
            trustStore = null;
            log.debug("Using default system truststore");
        }
    }

    private void loadPrivateKeyAndCertificate() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        PixConfig.Certificate certConfig = pixConfig.getCertificate();
        String keyAlias = certConfig.getKeyAlias();

        if (keyAlias == null || keyAlias.trim().isEmpty()) {
            // Find the first available alias
            keyAlias = keyStore.aliases().nextElement();
            log.debug("Using first available key alias: {}", keyAlias);
        }

        privateKey = (PrivateKey) keyStore.getKey(keyAlias, certConfig.getKeyPassword().toCharArray());
        Certificate cert = keyStore.getCertificate(keyAlias);

        if (cert instanceof X509Certificate) {
            certificate = (X509Certificate) cert;
        } else {
            throw new IllegalStateException("Certificate is not X.509 format");
        }

        // Validate certificate
        validateCertificate();

        log.debug("Private key and certificate loaded for alias: {}", keyAlias);
    }

    private void validateCertificate() {
        try {
            // Check if certificate is not expired
            certificate.checkValidity();

            // Log certificate details
            log.info("Certificate Subject: {}", certificate.getSubjectDN());
            log.info("Certificate Issuer: {}", certificate.getIssuerDN());
            log.info("Certificate Valid From: {}", certificate.getNotBefore());
            log.info("Certificate Valid Until: {}", certificate.getNotAfter());

        } catch (CertificateException e) {
            log.error("Certificate validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid certificate", e);
        }
    }

    private void createSSLContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        sslContext = SSLContext.getInstance("TLS");

        // Initialize KeyManager
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, pixConfig.getCertificate().getKeyPassword().toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        // Initialize TrustManager
        TrustManager[] trustManagers;
        if (trustStore != null) {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } else {
            // Use default system trust managers
            trustManagers = null;
        }

        // Initialize SSL context
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        log.debug("SSL context created successfully");
    }

    public String signData(String data) throws Exception {
        return signData(data.getBytes("UTF-8"));
    }

    public String signData(byte[] data) throws Exception {
        if (mockMode) {
            log.debug("Mock mode: returning mock signature");
            return Base64.getEncoder().encodeToString("mock-signature".getBytes());
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);

        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public boolean verifySignature(String data, String signatureBase64) throws Exception {
        return verifySignature(data.getBytes("UTF-8"), signatureBase64);
    }

    public boolean verifySignature(byte[] data, String signatureBase64) throws Exception {
        if (mockMode) {
            log.debug("Mock mode: returning true for signature verification");
            return true;
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate);
        signature.update(data);

        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }

    public String getCertificateFingerprint() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = certificate.getEncoded();
        md.update(der);
        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString().toUpperCase();
    }

    public String getCertificateSubjectDN() {
        return certificate.getSubjectDN().getName();
    }

    public String getCertificateIssuerDN() {
        return certificate.getIssuerDN().getName();
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return sslContext.getSocketFactory();
    }

    public HostnameVerifier getHostnameVerifier() {
        if (pixConfig.getCertificate().isValidateHostname()) {
            return HttpsURLConnection.getDefaultHostnameVerifier();
        } else {
            // WARNING: This disables hostname verification - should only be used in development
            return (hostname, session) -> {
                log.warn("Hostname verification disabled for: {}", hostname);
                return true;
            };
        }
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    // Utility method to create HTTP request signature
    public String createRequestSignature(String httpMethod, String requestPath, String requestBody, String timestamp) throws Exception {
        String stringToSign = httpMethod + "\n" +
                             requestPath + "\n" +
                             timestamp + "\n" +
                             (requestBody != null ? requestBody : "");

        return signData(stringToSign);
    }

    // Utility method to validate webhook signature
    public boolean validateWebhookSignature(String payload, String receivedSignature, String webhookSecret) throws Exception {
        if (webhookSecret != null && !webhookSecret.trim().isEmpty()) {
            // Use HMAC with webhook secret
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(receivedSignature);
        } else {
            // Use certificate signature
            return verifySignature(payload, receivedSignature);
        }
    }

    // Method to check certificate expiration
    public boolean isCertificateExpiringSoon(int daysThreshold) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = certificate.getNotAfter().getTime();
        long thresholdTime = currentTime + (daysThreshold * 24L * 60L * 60L * 1000L);

        return expirationTime <= thresholdTime;
    }

    public void refreshCertificates() {
        log.info("Refreshing PIX certificates...");
        try {
            initializeCertificates();
            log.info("PIX certificates refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh PIX certificates: {}", e.getMessage(), e);
            throw new RuntimeException("Certificate refresh failed", e);
        }
    }
}