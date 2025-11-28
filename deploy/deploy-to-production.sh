#!/bin/bash

# Script de Deploy do Zendapag para Produção - Digital Ocean
# Servidor: 159.89.80.179
# Data: 2025-10-29

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações
SERVER_IP="159.89.80.179"
SERVER_USER="root"
DEPLOY_DIR="/opt/zendapag"
PROJECT_NAME="zendapag"

echo -e "${BLUE}=========================================="
echo "🚀 DEPLOY ZENDAPAG - PRODUÇÃO"
echo "=========================================="
echo -e "Servidor: ${GREEN}${SERVER_IP}${NC}"
echo -e "Diretório: ${GREEN}${DEPLOY_DIR}${NC}"
echo -e "Data: $(date)${NC}"
echo ""

# Função para executar comandos no servidor
run_remote() {
    ssh ${SERVER_USER}@${SERVER_IP} "$1"
}

# Função para logs
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 1. Testar conexão SSH
log_info "Testando conexão SSH..."
if run_remote "echo 'Conexão OK'"; then
    log_success "Conexão SSH estabelecida!"
else
    log_error "Falha na conexão SSH!"
    exit 1
fi

# 2. Criar estrutura de diretórios no servidor
log_info "Criando estrutura de diretórios..."
run_remote "
    mkdir -p ${DEPLOY_DIR}/{logs/{api,worker},backups}
    echo '✅ Diretórios criados'
"
log_success "Estrutura de diretórios criada!"

# 3. Transferir arquivos do projeto
log_info "Criando arquivo compactado do projeto..."

# Criar arquivo tar com os arquivos necessários (excluindo node_modules, target, etc)
tar -czf /tmp/zendapag-deploy.tar.gz \
    --exclude='node_modules' \
    --exclude='target' \
    --exclude='build' \
    --exclude='dist' \
    --exclude='.git' \
    --exclude='*.log' \
    --exclude='logs' \
    -C /c/Projetos/zendapag \
    .

log_success "Arquivo compactado criado!"

log_info "Transferindo arquivos para o servidor (isso pode levar alguns minutos)..."
scp /tmp/zendapag-deploy.tar.gz ${SERVER_USER}@${SERVER_IP}:${DEPLOY_DIR}/

log_success "Arquivos transferidos!"

# 4. Extrair arquivos no servidor
log_info "Extraindo arquivos no servidor..."
run_remote "
    cd ${DEPLOY_DIR}
    tar -xzf zendapag-deploy.tar.gz
    rm zendapag-deploy.tar.gz
    echo '✅ Arquivos extraídos'
"
log_success "Arquivos extraídos!"

# 5. Verificar estrutura do projeto
log_info "Verificando estrutura do projeto..."
run_remote "
    cd ${DEPLOY_DIR}
    echo '=== Estrutura do Projeto ==='
    ls -la
    echo ''
    echo '=== Verificando docker-compose.prod.yml ==='
    test -f docker-compose.prod.yml && echo '✅ docker-compose.prod.yml encontrado' || echo '❌ docker-compose.prod.yml NÃO encontrado'
"

# 6. Buildar Docker images
log_info "Iniciando build das Docker images..."
log_warning "Este processo pode levar 10-15 minutos..."

run_remote "
    cd ${DEPLOY_DIR}
    echo '=== Building Zendapag API ==='
    docker-compose -f docker-compose.prod.yml build zendapag-api

    echo ''
    echo '=== Building Zendapag Worker ==='
    docker-compose -f docker-compose.prod.yml build zendapag-worker

    echo ''
    echo '=== Building Zendapag Dashboard ==='
    docker-compose -f docker-compose.prod.yml build zendapag-dashboard
"

log_success "Docker images criadas com sucesso!"

# 7. Listar images criadas
log_info "Verificando images Docker criadas..."
run_remote "docker images | grep zendapag"

# 8. Iniciar infraestrutura (Kafka, Redis, Zookeeper)
log_info "Iniciando infraestrutura (Kafka, Redis, Zookeeper)..."
run_remote "
    cd ${DEPLOY_DIR}
    docker-compose -f docker-compose.prod.yml up -d zookeeper kafka redis
    echo '✅ Infraestrutura iniciada'
"

log_success "Infraestrutura iniciada!"

# 9. Aguardar serviços ficarem saudáveis
log_info "Aguardando serviços ficarem saudáveis (60 segundos)..."
sleep 60

# 10. Verificar status da infraestrutura
log_info "Verificando status da infraestrutura..."
run_remote "
    echo '=== Status dos Containers ==='
    docker-compose -f ${DEPLOY_DIR}/docker-compose.prod.yml ps
"

# 11. Criar topics do Kafka
log_info "Criando topics do Kafka..."
run_remote "
    cd ${DEPLOY_DIR}
    chmod +x scripts/create-kafka-topics-prod.sh
    ./scripts/create-kafka-topics-prod.sh
"

log_success "Topics do Kafka criados!"

# 12. Iniciar aplicações (API, Worker, Dashboard)
log_info "Iniciando aplicações (API, Worker, Dashboard)..."
run_remote "
    cd ${DEPLOY_DIR}
    docker-compose -f docker-compose.prod.yml up -d zendapag-api zendapag-worker zendapag-dashboard
    echo '✅ Aplicações iniciadas'
"

log_success "Aplicações iniciadas!"

# 13. Aguardar aplicações ficarem saudáveis
log_info "Aguardando aplicações ficarem saudáveis (90 segundos)..."
sleep 90

# 14. Verificar health checks
log_info "Verificando health checks..."

echo ""
echo "=== API Health Check ==="
run_remote "curl -s http://localhost:8091/actuator/health | python3 -m json.tool || echo 'Health check falhou'"

echo ""
echo "=== Worker Health Check ==="
run_remote "curl -s http://localhost:8092/actuator/health | python3 -m json.tool || echo 'Health check falhou'"

echo ""
echo "=== Dashboard Health Check ==="
run_remote "curl -s http://localhost:3005/health || echo 'Health check falhou'"

# 15. Verificar logs
log_info "Últimas linhas dos logs..."

echo ""
echo "=== API Logs ==="
run_remote "cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml logs --tail=20 zendapag-api"

echo ""
echo "=== Worker Logs ==="
run_remote "cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml logs --tail=20 zendapag-worker"

# 16. Status final
log_info "Status final de todos os containers..."
run_remote "cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml ps"

# 17. Sumário do deploy
echo ""
echo -e "${BLUE}=========================================="
echo "📊 SUMÁRIO DO DEPLOY"
echo "==========================================${NC}"
echo ""
log_success "Deploy concluído com sucesso!"
echo ""
echo "🌐 URLs de Acesso:"
echo "  - API: http://${SERVER_IP}:8091"
echo "  - API Health: http://${SERVER_IP}:8091/actuator/health"
echo "  - API Swagger: http://${SERVER_IP}:8091/swagger-ui.html"
echo "  - Worker: http://${SERVER_IP}:8092"
echo "  - Worker Health: http://${SERVER_IP}:8092/actuator/health"
echo "  - Dashboard: http://${SERVER_IP}:3005"
echo ""
echo "📝 Comandos úteis:"
echo "  - Ver logs API: ssh root@${SERVER_IP} 'cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml logs -f zendapag-api'"
echo "  - Ver logs Worker: ssh root@${SERVER_IP} 'cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml logs -f zendapag-worker'"
echo "  - Ver todos containers: ssh root@${SERVER_IP} 'cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml ps'"
echo "  - Parar tudo: ssh root@${SERVER_IP} 'cd ${DEPLOY_DIR} && docker-compose -f docker-compose.prod.yml down'"
echo ""
echo "✅ Deploy finalizado em: $(date)"
echo ""

# Limpar arquivo temporário
rm -f /tmp/zendapag-deploy.tar.gz
log_success "Arquivo temporário removido"
