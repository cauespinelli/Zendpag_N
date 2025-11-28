#!/bin/bash
set -e

echo "======================================"
echo "🚀 ZENDAPAG - DEPLOY DASHBOARD"
echo "======================================"
echo ""

# Configuration
SERVER="root@167.99.12.191"
REMOTE_PATH="/opt/zendapag/dashboard"
LOCAL_BUILD="/c/Projetos/zendapag/zendapag-dashboard/build"

echo "📦 Passo 1: Criando pacote do build..."
cd /c/Projetos/zendapag/zendapag-dashboard
tar czf /tmp/dashboard-deploy-$(date +%Y%m%d_%H%M%S).tar.gz build/
PACKAGE_FILE=$(ls -t /tmp/dashboard-deploy-*.tar.gz | head -1)
echo "✅ Pacote criado: $PACKAGE_FILE"
echo ""

echo "📤 Passo 2: Enviando para o servidor..."
scp "$PACKAGE_FILE" "$SERVER:/tmp/dashboard-new.tar.gz"
echo "✅ Arquivo enviado"
echo ""

echo "🔄 Passo 3: Fazendo backup e deploy no servidor..."
ssh "$SERVER" bash << 'EOF'
set -e
cd /opt/zendapag

# Backup
if [ -d "dashboard" ]; then
  echo "📦 Criando backup..."
  tar czf "dashboard-backup-$(date +%Y%m%d_%H%M%S).tar.gz" dashboard/
  echo "✅ Backup criado"
fi

# Extract new build
echo "📂 Extraindo novo build..."
cd /tmp
tar xzf dashboard-new.tar.gz

# Deploy
echo "🚀 Deployando..."
rm -rf /opt/zendapag/dashboard/*
cp -r build/* /opt/zendapag/dashboard/
chown -R www-data:www-data /opt/zendapag/dashboard 2>/dev/null || chown -R nginx:nginx /opt/zendapag/dashboard 2>/dev/null || true
chmod -R 755 /opt/zendapag/dashboard

# Cleanup
rm -rf build dashboard-new.tar.gz

echo "✅ Deploy concluído!"

# Reload nginx
echo "🔄 Recarregando Nginx..."
nginx -t && nginx -s reload && echo "✅ Nginx recarregado" || echo "⚠️  Verifique configuração do Nginx"

echo ""
echo "======================================"
echo "✅ DEPLOY CONCLUÍDO COM SUCESSO!"
echo "======================================"
echo ""
echo "🌐 Acesse: http://app.zendapag.com"
echo "👤 Login: admin@zendapag.com"
echo "🔑 Senha: admin123"
echo ""
EOF

echo ""
echo "🎉 DEPLOY FINALIZADO!"
echo ""
echo "📋 Próximos passos:"
echo "1. Abrir http://app.zendapag.com"
echo "2. Limpar cache do navegador (Ctrl+Shift+R)"
echo "3. Fazer login com admin@zendapag.com / admin123"
echo ""
