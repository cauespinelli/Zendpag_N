#!/bin/bash

# Script to create Kafka topics for PIX Withdrawal module
# Usage: ./create-withdrawal-topics.sh [environment]
# Environments: dev, staging, prod

ENVIRONMENT=${1:-dev}
KAFKA_BROKER=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}
PARTITIONS=${KAFKA_PARTITIONS:-3}
REPLICATION_FACTOR=${KAFKA_REPLICATION_FACTOR:-1}

echo "=================================================="
echo "Creating Kafka Topics for PIX Withdrawal Module"
echo "Environment: $ENVIRONMENT"
echo "Kafka Broker: $KAFKA_BROKER"
echo "Partitions: $PARTITIONS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo "=================================================="
echo ""

# Function to create topic
create_topic() {
    local topic_name=$1
    local partitions=$2
    local replication=$3
    local retention=${4:-604800000} # Default 7 days in milliseconds

    echo "Creating topic: $topic_name"

    kafka-topics.sh --create \
        --bootstrap-server $KAFKA_BROKER \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --config retention.ms=$retention \
        --config compression.type=lz4 \
        --config min.insync.replicas=1 \
        --if-not-exists

    if [ $? -eq 0 ]; then
        echo "✓ Topic '$topic_name' created successfully"
    else
        echo "✗ Failed to create topic '$topic_name'"
    fi
    echo ""
}

# Create withdrawal-events topic
# Main topic for withdrawal processing
create_topic "withdrawal-events-${ENVIRONMENT}" $PARTITIONS $REPLICATION_FACTOR 604800000

# Create withdrawal-processing topic
# Alternative topic for processing by ID
create_topic "withdrawal-processing-${ENVIRONMENT}" $PARTITIONS $REPLICATION_FACTOR 604800000

# Create withdrawal-events-dlq topic
# Dead Letter Queue for failed messages
create_topic "withdrawal-events-dlq-${ENVIRONMENT}" 1 $REPLICATION_FACTOR 2592000000 # 30 days

echo "=================================================="
echo "Kafka Topics Creation Complete!"
echo "=================================================="
echo ""
echo "Listing created topics:"
kafka-topics.sh --list --bootstrap-server $KAFKA_BROKER | grep "withdrawal.*${ENVIRONMENT}"
echo ""
echo "To describe a topic, run:"
echo "kafka-topics.sh --describe --bootstrap-server $KAFKA_BROKER --topic withdrawal-events-${ENVIRONMENT}"
