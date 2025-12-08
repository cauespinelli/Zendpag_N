#!/bin/bash

# Zendapag Design System v2.0 - Serve Local (sem Docker)
# Serve os builds localmente usando http-server

set -e

echo "🚀 ZENDAPAG DESIGN SYSTEM V2.0 - SERVE LOCAL"
echo "============================================="
echo ""

# Verificar se npx está disponível
if ! command -v npx &> /dev/null; then
    echo "❌ npx não encontrado! Instale Node.js primeiro."
    exit 1
fi

# Verificar se o build do dashboard existe
if [ ! -d "zendapag-dashboard/build" ]; then
    echo "❌ Build do dashboard não encontrado!"
    echo "Execute: cd zendapag-dashboard && npm run build"
    exit 1
fi

echo "✅ Builds encontrados"
echo ""

echo "🌐 Iniciando servidores..."
echo ""

# Dashboard em background
echo "📊 Dashboard: http://localhost:3005"
cd zendapag-dashboard
npx serve -s build -l 3005 > /dev/null 2>&1 &
DASHBOARD_PID=$!
cd ..

# Landing Page em background (se existir build)
if [ -d "zendapag-landing/build" ]; then
    echo "🎨 Landing: http://localhost:3000"
    cd zendapag-landing
    npx serve -s build -l 3000 > /dev/null 2>&1 &
    LANDING_PID=$!
    cd ..
else
    echo "⚠️  Landing page build não encontrado (erro de dependência)"
    LANDING_PID=""
fi

echo ""
echo "============================================="
echo "✅ Servidores iniciados!"
echo ""
echo "📊 Acesse:"
echo "  Dashboard: http://localhost:3005"
if [ -n "$LANDING_PID" ]; then
    echo "  Landing:   http://localhost:3000"
fi
echo ""
echo "ℹ️  PIDs:"
echo "  Dashboard: $DASHBOARD_PID"
if [ -n "$LANDING_PID" ]; then
    echo "  Landing:   $LANDING_PID"
fi
echo ""
echo "🛑 Para parar os servidores:"
echo "  kill $DASHBOARD_PID"
if [ -n "$LANDING_PID" ]; then
    echo "  kill $LANDING_PID"
fi
echo ""
echo "Ou use: killall node"
echo "============================================="
echo ""
echo "✨ Pressione Ctrl+C quando quiser parar os servidores"
echo ""

# Aguardar interrupção
trap "echo ''; echo '🛑 Parando servidores...'; kill $DASHBOARD_PID 2>/dev/null; [ -n '$LANDING_PID' ] && kill $LANDING_PID 2>/dev/null; echo '✅ Servidores parados'; exit 0" INT

# Manter script rodando
wait
