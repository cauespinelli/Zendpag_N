#!/bin/bash

# Script para criar topics do Kafka em produção
# Uso: ./create-kafka-topics-prod.sh

set -e

KAFKA_CONTAINER="zendapag-kafka"
BOOTSTRAP_SERVER="localhost:9092"

echo "=========================================="
echo "Criando Topics do Kafka - PRODUÇÃO"
echo "=========================================="
echo ""

# Verificar se o container do Kafka está rodando
if ! docker ps | grep -q $KAFKA_CONTAINER; then
    echo "❌ Erro: Container $KAFKA_CONTAINER não está rodando!"
    echo "   Execute: docker-compose -f docker-compose.prod.yml up -d kafka"
    exit 1
fi

# Aguardar Kafka estar pronto
echo "⏳ Aguardando Kafka estar pronto..."
sleep 10

# Função para criar topic
create_topic() {
    TOPIC_NAME=$1
    PARTITIONS=$2
    REPLICATION=$3
    RETENTION_MS=$4

    echo "📝 Criando topic: $TOPIC_NAME"

    docker exec $KAFKA_CONTAINER kafka-topics \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --create \
        --topic $TOPIC_NAME \
        --partitions $PARTITIONS \
        --replication-factor $REPLICATION \
        --config retention.ms=$RETENTION_MS \
        --if-not-exists

    if [ $? -eq 0 ]; then
        echo "✅ Topic $TOPIC_NAME criado com sucesso!"
    else
        echo "⚠️  Topic $TOPIC_NAME já existe ou houve erro na criação"
    fi
    echo ""
}

# Criar topics de produção
# Formato: create_topic NOME PARTIÇÕES REPLICATION RETENTION_MS

# Topic principal de eventos de saque
create_topic "withdrawal-events-prod" 3 1 604800000  # 7 dias

# Topic para processamento de saques
create_topic "withdrawal-processing-prod" 3 1 604800000  # 7 dias

# Topic DLQ (Dead Letter Queue) para erros
create_topic "withdrawal-events-dlq-prod" 1 1 2592000000  # 30 dias

echo "=========================================="
echo "Listando todos os topics criados:"
echo "=========================================="
docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --list | grep withdrawal

echo ""
echo "=========================================="
echo "Detalhes dos topics:"
echo "=========================================="
for topic in "withdrawal-events-prod" "withdrawal-processing-prod" "withdrawal-events-dlq-prod"; do
    echo ""
    echo "📊 Topic: $topic"
    docker exec $KAFKA_CONTAINER kafka-topics \
        --bootstrap-server $BOOTSTRAP_SERVER \
        --describe \
        --topic $topic
done

echo ""
echo "✅ Topics do Kafka criados com sucesso!"
echo ""
