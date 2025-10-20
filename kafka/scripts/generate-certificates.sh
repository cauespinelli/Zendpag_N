#!/bin/bash

# Kafka SSL Certificate Generation Script for Zendapag
set -e

VALIDITY_DAYS=3650
KEYSTORE_PASSWORD="kafkastore"
TRUSTSTORE_PASSWORD="kafkastore"
KEY_PASSWORD="kafkastore"

# Create directories
mkdir -p ../secrets
cd ../secrets

# Generate CA key and certificate
echo "Generating Certificate Authority..."
openssl req -new -x509 -keyout ca-key -out ca-cert -days $VALIDITY_DAYS -subj "/CN=ZendapagCA" -nodes

# Generate server keystore and certificate for each Kafka broker
for i in 1 2 3; do
    echo "Generating certificate for kafka$i..."

    # Generate keystore
    keytool -genkey -noprompt \
        -alias kafka$i \
        -dname "CN=kafka$i,OU=Zendapag,O=Zendapag,L=SaoPaulo,S=SP,C=BR" \
        -keystore kafka$i.keystore.jks \
        -keyalg RSA \
        -storepass $KEYSTORE_PASSWORD \
        -keypass $KEY_PASSWORD \
        -storetype JKS \
        -validity $VALIDITY_DAYS

    # Generate certificate signing request
    keytool -certreq -alias kafka$i \
        -keystore kafka$i.keystore.jks \
        -file kafka$i.csr \
        -storepass $KEYSTORE_PASSWORD

    # Sign certificate with CA
    openssl x509 -req -CA ca-cert -CAkey ca-key \
        -in kafka$i.csr \
        -out kafka$i-cert-signed \
        -days $VALIDITY_DAYS \
        -CAcreateserial

    # Import CA certificate into keystore
    keytool -import -alias CARoot \
        -file ca-cert \
        -keystore kafka$i.keystore.jks \
        -storepass $KEYSTORE_PASSWORD \
        -noprompt

    # Import signed certificate into keystore
    keytool -import -alias kafka$i \
        -file kafka$i-cert-signed \
        -keystore kafka$i.keystore.jks \
        -storepass $KEYSTORE_PASSWORD

    # Clean up CSR file
    rm kafka$i.csr
done

# Create truststore
echo "Creating truststore..."
keytool -import -alias CARoot \
    -file ca-cert \
    -keystore kafka.truststore.jks \
    -storepass $TRUSTSTORE_PASSWORD \
    -noprompt

# Generate client certificates for applications
echo "Generating client certificates..."

# Generate client keystore
keytool -genkey -noprompt \
    -alias zendapag-client \
    -dname "CN=zendapag-client,OU=Zendapag,O=Zendapag,L=SaoPaulo,S=SP,C=BR" \
    -keystore client.keystore.jks \
    -keyalg RSA \
    -storepass $KEYSTORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -storetype JKS \
    -validity $VALIDITY_DAYS

# Generate client certificate signing request
keytool -certreq -alias zendapag-client \
    -keystore client.keystore.jks \
    -file client.csr \
    -storepass $KEYSTORE_PASSWORD

# Sign client certificate with CA
openssl x509 -req -CA ca-cert -CAkey ca-key \
    -in client.csr \
    -out client-cert-signed \
    -days $VALIDITY_DAYS \
    -CAcreateserial

# Import CA certificate into client keystore
keytool -import -alias CARoot \
    -file ca-cert \
    -keystore client.keystore.jks \
    -storepass $KEYSTORE_PASSWORD \
    -noprompt

# Import signed certificate into client keystore
keytool -import -alias zendapag-client \
    -file client-cert-signed \
    -keystore client.keystore.jks \
    -storepass $KEYSTORE_PASSWORD

# Clean up
rm client.csr

# Create client truststore (copy of server truststore)
cp kafka.truststore.jks client.truststore.jks

# Set permissions
chmod 600 *.jks
chmod 600 ca-key
chmod 644 ca-cert
chmod 644 *-cert-signed

echo "SSL certificates generated successfully!"
echo "Keystore password: $KEYSTORE_PASSWORD"
echo "Truststore password: $TRUSTSTORE_PASSWORD"
echo "Key password: $KEY_PASSWORD"

# List certificate details
echo ""
echo "Certificate details:"
for i in 1 2 3; do
    echo "kafka$i keystore:"
    keytool -list -keystore kafka$i.keystore.jks -storepass $KEYSTORE_PASSWORD | head -10
    echo ""
done

echo "Client keystore:"
keytool -list -keystore client.keystore.jks -storepass $KEYSTORE_PASSWORD | head -10

echo ""
echo "Truststore:"
keytool -list -keystore kafka.truststore.jks -storepass $TRUSTSTORE_PASSWORD | head -10