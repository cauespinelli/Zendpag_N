# 🔧 Deploy Fix - Correção do Erro de Login

## ✅ Problema Identificado e Corrigido

### **Erro Original:**
```
Erro ao fazer login - Recurso não encontrado
```

### **Causa Raiz:**
O arquivo `.env.production` estava configurado com:
```
REACT_APP_API_URL=/api/v1
```

Mas o servidor mock backend em produção não usa o prefixo `/api/v1`, causando erro 404 em:
- `POST /api/v1/auth/login` ❌ (não existe)

### **Correção Aplicada:**
Atualizado `.env.production` para apontar para o domínio correto:
```
REACT_APP_API_URL=http://api.zendapag.com
```

---

## 📦 Build de Produção Concluído

### Status:
✅ **Build compilado com sucesso!**

### Arquivos Gerados:
```
/c/Projetos/zendapag/zendapag-dashboard/build/
```

### Tamanhos dos Arquivos (após gzip):
- **Total:** ~840 KB
- Main bundle: 253.88 KB
- Ant Design: 367.99 KB
- Charts: 86.64 KB
- CSS: 2.98 KB

---

## 🚀 Instruções de Deploy para Digital Ocean

### **Opção 1: Deploy via SSH (RECOMENDADO)**

#### Passo 1: Conectar ao servidor
```bash
ssh root@167.99.12.191
```

#### Passo 2: Fazer backup do dashboard atual
```bash
cd /opt/zendapag
cp -r dashboard dashboard.backup.$(date +%Y%m%d_%H%M%S)
```

#### Passo 3: No seu computador local, enviar o novo build
```bash
cd /c/Projetos/zendapag/zendapag-dashboard

# Criar tar do build
tar czf dashboard-build.tar.gz build/

# Enviar para o servidor
scp dashboard-build.tar.gz root@167.99.12.191:/tmp/
```

#### Passo 4: No servidor, extrair e substituir
```bash
# Conectar SSH
ssh root@167.99.12.191

# Extrair build
cd /tmp
tar xzf dashboard-build.tar.gz

# Remover arquivos antigos e copiar novos
rm -rf /opt/zendapag/dashboard/*
cp -r build/* /opt/zendapag/dashboard/

# Limpar
rm -rf build dashboard-build.tar.gz

# Verificar permissões
chown -R www-data:www-data /opt/zendapag/dashboard
chmod -R 755 /opt/zendapag/dashboard

# Recarregar nginx
nginx -t && nginx -s reload

echo "✅ Deploy concluído!"
```

#### Passo 5: Verificar
```bash
# Testar localmente no servidor
curl http://localhost/

# Verificar no navegador
# http://app.zendapag.com
```

---

### **Opção 2: Deploy via SFTP/FTP**

1. Conectar via SFTP cliente (FileZilla, WinSCP, etc.)
2. Navegar para `/opt/zendapag/dashboard/`
3. Fazer backup da pasta atual
4. Deletar conteúdo da pasta dashboard
5. Upload de todos os arquivos de `C:\Projetos\zendapag\zendapag-dashboard\build\`
6. Ajustar permissões: 755 para pastas, 644 para arquivos
7. Recarregar Nginx via SSH ou painel

---

### **Opção 3: Deploy via Docker (se usando containers)**

```bash
# No servidor
ssh root@167.99.12.191

cd /opt/zendapag

# Reconstruir container do dashboard
docker-compose -f docker-compose.frontend.yml build dashboard

# Reiniciar serviço
docker-compose -f docker-compose.frontend.yml up -d dashboard

# Verificar logs
docker-compose -f docker-compose.frontend.yml logs -f dashboard
```

---

## 🧪 Teste de Login Após Deploy

### Credenciais de Teste:

#### **Administrador:**
```
Email: admin@zendapag.com
Senha: admin123
```

#### **Comerciante:**
```
Email: merchant@example.com
Senha: merchant123
```

### Endpoints Corretos:
```bash
# Login (sem /api/v1!)
POST http://api.zendapag.com/auth/login
Content-Type: application/json

{
  "email": "admin@zendapag.com",
  "password": "admin123"
}

# Resposta esperada:
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": "1",
    "email": "admin@zendapag.com",
    "name": "Admin ZendaPag",
    "roles": ["ADMIN", "MERCHANT"]
  }
}
```

---

## 🔍 Verificação Pós-Deploy

### Checklist:

#### 1. Build Copiado
```bash
ssh root@167.99.12.191
ls -lh /opt/zendapag/dashboard/
# Deve mostrar: index.html, static/, etc.
```

#### 2. Nginx Configurado
```bash
cat /etc/nginx/sites-enabled/app.zendapag.com
# Deve apontar para /opt/zendapag/dashboard
```

#### 3. Permissões Corretas
```bash
ls -la /opt/zendapag/dashboard/
# Deve ser: drwxr-xr-x root root (ou www-data)
```

#### 4. Nginx Rodando
```bash
systemctl status nginx
nginx -t
```

#### 5. Teste de Acesso
```bash
# No servidor
curl -I http://localhost/

# Do navegador
http://app.zendapag.com
```

#### 6. Teste de Login
1. Abrir http://app.zendapag.com
2. Clicar em "Login"
3. Usar: admin@zendapag.com / admin123
4. ✅ Deve logar com sucesso

---

## 📊 Arquivos Modificados

### `/c/Projetos/zendapag/zendapag-dashboard/.env.production`
**ANTES:**
```env
REACT_APP_API_URL=/api/v1
REACT_APP_ENVIRONMENT=production
REACT_APP_VERSION=$npm_package_version
REACT_APP_ENABLE_DEBUG=false
GENERATE_SOURCEMAP=true
```

**DEPOIS:**
```env
# API Configuration - Production
REACT_APP_API_URL=http://api.zendapag.com
REACT_APP_WORKER_API_URL=http://api.zendapag.com
REACT_APP_WEBSOCKET_URL=ws://api.zendapag.com/ws
REACT_APP_ENVIRONMENT=production
REACT_APP_VERSION=2.0.0
REACT_APP_ENABLE_DEBUG=false
GENERATE_SOURCEMAP=false
REACT_APP_NAME=Zendapag Dashboard
REACT_APP_ENABLE_ANALYTICS=true
REACT_APP_ENABLE_REAL_TIME=true
REACT_APP_ENABLE_NOTIFICATIONS=true
REACT_APP_METRICS_REFRESH_INTERVAL=30000
REACT_APP_PAYMENTS_REFRESH_INTERVAL=10000
```

---

## ⚠️ Observações Importantes

### 1. Configuração DNS
O `.env.production` agora usa `http://api.zendapag.com`. Certifique-se de que:
- DNS está configurado corretamente
- Nginx reverse proxy está apontando `api.zendapag.com` → `localhost:8093`

### 2. CORS
Verifique se a API permite requisições de `app.zendapag.com`:
```javascript
// No backend
Access-Control-Allow-Origin: http://app.zendapag.com
```

### 3. Sem /api/v1
O mock backend **NÃO** usa o prefixo `/api/v1`. Os endpoints são:
- `/auth/login` ✅
- `/auth/refresh` ✅
- `/auth/me` ✅
- `/payments` ✅

### 4. Cache do Navegador
Após o deploy, limpar cache:
```
Ctrl + Shift + R (ou Cmd + Shift + R no Mac)
```

---

## 🎯 Comandos Rápidos de Deploy

### Deploy Completo (One-liner):
```bash
cd /c/Projetos/zendapag/zendapag-dashboard && \
export NODE_OPTIONS="--max-old-space-size=4096" && \
npm run build && \
tar czf /tmp/dashboard-build.tar.gz build/ && \
scp /tmp/dashboard-build.tar.gz root@167.99.12.191:/tmp/ && \
ssh root@167.99.12.191 "cd /tmp && tar xzf dashboard-build.tar.gz && rm -rf /opt/zendapag/dashboard/* && cp -r build/* /opt/zendapag/dashboard/ && chown -R www-data:www-data /opt/zendapag/dashboard && nginx -s reload && echo '✅ Deploy concluído!'"
```

---

## 📞 Suporte

Se encontrar problemas:

1. **Verificar logs do Nginx:**
```bash
tail -f /var/log/nginx/error.log
tail -f /var/log/nginx/access.log
```

2. **Verificar console do navegador:**
- Abrir DevTools (F12)
- Aba Console
- Procurar erros de CORS ou 404

3. **Testar API diretamente:**
```bash
curl -X POST http://api.zendapag.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@zendapag.com","password":"admin123"}'
```

---

**✅ Build pronto em:** `C:\Projetos\zendapag\zendapag-dashboard\build\`
**🚀 Pronto para deploy!**

**Data:** 28 de Outubro de 2025
**Versão:** 2.0.0
