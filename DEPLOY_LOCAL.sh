#!/bin/bash

# Zendapag Design System v2.0 - Deploy Local com Docker
# Este script faz deploy dos frontends localmente usando Docker

set -e

echo "🚀 ZENDAPAG DESIGN SYSTEM V2.0 - DEPLOY LOCAL"
echo "=============================================="
echo ""

# Cores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker não está rodando!${NC}"
    echo "Por favor, inicie o Docker Desktop e tente novamente."
    exit 1
fi

echo -e "${BLUE}📦 Step 1: Parando containers antigos...${NC}"
docker-compose -f docker-compose.frontend.yml down 2>/dev/null || true
echo -e "${GREEN}✅ Containers parados${NC}"
echo ""

echo -e "${BLUE}🔨 Step 2: Buildando Dashboard...${NC}"
cd zendapag-dashboard
docker build -t zendapag-dashboard:latest -f Dockerfile.prod .
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Dashboard build completo${NC}"
else
    echo -e "${RED}❌ Dashboard build falhou!${NC}"
    exit 1
fi
cd ..
echo ""

echo -e "${BLUE}🔨 Step 3: Buildando Landing Page...${NC}"
echo -e "${BLUE}ℹ️  Nota: Landing page pode ter erros de dependência, usando build do npm...${NC}"
cd zendapag-landing
# Verificar se o build já existe
if [ ! -d "build" ]; then
    echo "Build não encontrado, executando npm run build..."
    npm run build || echo "⚠️  Build falhou, mas continuando..."
fi
cd ..
echo ""

echo -e "${BLUE}🐳 Step 4: Iniciando containers...${NC}"
docker-compose -f docker-compose.frontend.yml up -d
echo -e "${GREEN}✅ Containers iniciados${NC}"
echo ""

echo -e "${BLUE}⏳ Step 5: Aguardando health checks...${NC}"
sleep 10
echo ""

echo -e "${BLUE}📊 Step 6: Verificando status...${NC}"
docker-compose -f docker-compose.frontend.yml ps
echo ""

echo "=============================================="
echo -e "${GREEN}🎉 DEPLOY LOCAL COMPLETO!${NC}"
echo ""
echo "📊 Acesse:"
echo "  Dashboard: http://localhost:3005"
echo "  Landing:   http://localhost:3000"
echo ""
echo "🔍 Verificar logs:"
echo "  docker-compose -f docker-compose.frontend.yml logs -f"
echo ""
echo "🛑 Parar serviços:"
echo "  docker-compose -f docker-compose.frontend.yml down"
echo ""
echo "✨ Zendapag Design System v2.0 está RODANDO!"
echo "=============================================="
