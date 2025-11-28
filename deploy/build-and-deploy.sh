#!/bin/bash

# Script para buildar e fazer deploy do Zendapag em produção
# Executar diretamente no servidor: bash /opt/zendapag/deploy/build-and-deploy.sh

set -e

cd /opt/zendapag

echo "=========================================="
echo "🚀 BUILD E DEPLOY ZENDAPAG - PRODUÇÃO"
echo "=========================================="
echo "Data: $(date)"
echo ""

# 1. Verificar docker-compose.prod.yml
echo "1. Verificando docker-compose.prod.yml..."
if [ ! -f docker-compose.prod.yml ]; then
    echo "❌ Erro: docker-compose.prod.yml não encontrado!"
    exit 1
fi
echo "✅ docker-compose.prod.yml encontrado"
echo ""

# 2. Parar containers existentes (se houver)
echo "2. Parando containers existentes..."
docker-compose -f docker-compose.prod.yml down 2>/dev/null || echo "Nenhum container para parar"
echo "✅ Containers parados"
echo ""

# 3. Limpar images antigas (opcional - comentado por segurança)
# echo "3. Limpando images antigas..."
# docker image prune -f
# echo "✅ Images limpas"
# echo ""

# 4. Build da API
echo "3. Buildando Zendapag API..."
echo "   (Isso pode levar 10-15 minutos)"
docker-compose -f docker-compose.prod.yml build zendapag-api
echo "✅ API buildada com sucesso!"
echo ""

# 5. Build do Worker
echo "4. Buildando Zendapag Worker..."
echo "   (Isso pode levar 10-15 minutos)"
docker-compose -f docker-compose.prod.yml build zendapag-worker
echo "✅ Worker buildado com sucesso!"
echo ""

# 6. Build do Dashboard
echo "5. Buildando Zendapag Dashboard..."
echo "   (Isso pode levar 5 minutos)"
docker-compose -f docker-compose.prod.yml build zendapag-dashboard
echo "✅ Dashboard buildado com sucesso!"
echo ""

# 7. Verificar images criadas
echo "6. Verificando images Docker criadas..."
docker images | grep zendapag
echo ""

# 8. Iniciar infraestrutura (Kafka, Redis, Zookeeper)
echo "7. Iniciando infraestrutura (Zookeeper, Kafka, Redis)..."
docker-compose -f docker-compose.prod.yml up -d zookeeper kafka redis
echo "✅ Infraestrutura iniciada"
echo ""

# 9. Aguardar serviços ficarem saudáveis
echo "8. Aguardando serviços ficarem saudáveis (60 segundos)..."
sleep 60
echo "✅ Aguardado concluído"
echo ""

# 10. Verificar status da infraestrutura
echo "9. Verificando status da infraestrutura..."
docker-compose -f docker-compose.prod.yml ps
echo ""

# 11. Criar topics do Kafka
echo "10. Criando topics do Kafka..."
chmod +x scripts/create-kafka-topics-prod.sh
./scripts/create-kafka-topics-prod.sh
echo "✅ Topics criados"
echo ""

# 12. Iniciar aplicações (API, Worker, Dashboard)
echo "11. Iniciando aplicações (API, Worker, Dashboard)..."
docker-compose -f docker-compose.prod.yml up -d zendapag-api zendapag-worker zendapag-dashboard
echo "✅ Aplicações iniciadas"
echo ""

# 13. Aguardar aplicações ficarem saudáveis
echo "12. Aguardando aplicações ficarem saudáveis (90 segundos)..."
sleep 90
echo "✅ Aguardado concluído"
echo ""

# 14. Verificar health checks
echo "13. Verificando health checks..."
echo ""
echo "=== API Health Check ==="
curl -s http://localhost:8091/actuator/health 2>/dev/null || echo "❌ API health check falhou"
echo ""
echo ""
echo "=== Worker Health Check ==="
curl -s http://localhost:8092/actuator/health 2>/dev/null || echo "❌ Worker health check falhou"
echo ""
echo ""
echo "=== Dashboard Health Check ==="
curl -s http://localhost:3005/health 2>/dev/null || echo "❌ Dashboard health check falhou"
echo ""

# 15. Status final
echo ""
echo "14. Status final de todos os containers..."
docker-compose -f docker-compose.prod.yml ps
echo ""

# 16. Verificar logs recentes
echo "15. Últimas linhas dos logs..."
echo ""
echo "=== API Logs ==="
docker-compose -f docker-compose.prod.yml logs --tail=30 zendapag-api
echo ""
echo "=== Worker Logs ==="
docker-compose -f docker-compose.prod.yml logs --tail=30 zendapag-worker
echo ""
echo "=== Dashboard Logs ==="
docker-compose -f docker-compose.prod.yml logs --tail=20 zendapag-dashboard
echo ""

# 17. Sumário
echo "=========================================="
echo "📊 DEPLOY CONCLUÍDO COM SUCESSO!"
echo "=========================================="
echo ""
echo "🌐 URLs de Acesso:"
echo "  - API: http://159.89.80.179:8091"
echo "  - API Health: http://159.89.80.179:8091/actuator/health"
echo "  - API Swagger: http://159.89.80.179:8091/swagger-ui.html"
echo "  - Worker: http://159.89.80.179:8092"
echo "  - Worker Health: http://159.89.80.179:8092/actuator/health"
echo "  - Dashboard: http://159.89.80.179:3005"
echo ""
echo "📝 Comandos úteis:"
echo "  - Ver logs API: docker-compose -f docker-compose.prod.yml logs -f zendapag-api"
echo "  - Ver logs Worker: docker-compose -f docker-compose.prod.yml logs -f zendapag-worker"
echo "  - Ver todos containers: docker-compose -f docker-compose.prod.yml ps"
echo "  - Parar tudo: docker-compose -f docker-compose.prod.yml down"
echo "  - Reiniciar serviço: docker-compose -f docker-compose.prod.yml restart [service-name]"
echo ""
echo "✅ Deploy finalizado em: $(date)"
echo ""
