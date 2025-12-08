#!/bin/bash

# Zendapag - Quick Deploy Script
# Design System v2.0

echo "🚀 ZENDAPAG DEPLOY - Design System v2.0"
echo "========================================"

# Configurações
SERVER="root@67.205.171.243"
DASHBOARD_DIR="/opt/zendapag/dashboard"
LANDING_DIR="/opt/zendapag/landing"

echo ""
echo "📦 Step 1: Building Dashboard..."
cd zendapag-dashboard
npm run build
if [ $? -ne 0 ]; then
    echo "❌ Dashboard build failed!"
    exit 1
fi
echo "✅ Dashboard build complete"

echo ""
echo "📦 Step 2: Building Landing Page..."
cd ../zendapag-landing
npm run build
if [ $? -ne 0 ]; then
    echo "❌ Landing build failed!"
    exit 1
fi
echo "✅ Landing build complete"

echo ""
echo "📤 Step 3: Deploying Dashboard..."
cd ../zendapag-dashboard
rsync -avz --delete build/ ${SERVER}:${DASHBOARD_DIR}/
if [ $? -eq 0 ]; then
    echo "✅ Dashboard deployed"
else
    echo "❌ Dashboard deploy failed"
fi

echo ""
echo "📤 Step 4: Deploying Landing Page..."
cd ../zendapag-landing
rsync -avz --delete build/ ${SERVER}:${LANDING_DIR}/
if [ $? -eq 0 ]; then
    echo "✅ Landing deployed"
else
    echo "❌ Landing deploy failed"
fi

echo ""
echo "🔄 Step 5: Restarting Nginx..."
ssh ${SERVER} "systemctl reload nginx"
if [ $? -eq 0 ]; then
    echo "✅ Nginx reloaded"
else
    echo "❌ Nginx reload failed"
fi

echo ""
echo "========================================"
echo "🎉 DEPLOY COMPLETE!"
echo ""
echo "📊 Access:"
echo "  Dashboard: http://67.205.171.243:3005"
echo "  Landing: http://67.205.171.243:3000"
echo ""
echo "✨ Zendapag Design System v2.0 is LIVE!"
echo "========================================"
