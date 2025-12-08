# 🚀 Zendapag Design System v2.0 - Guia de Deploy

## ✅ Status Atual

- ✅ Design System v2.0 implementado
- ✅ Dashboard build completo (15MB)
- ⚠️  Landing page com erro de dependência (workbox)
- ✅ Dockerfiles criados
- ✅ Docker Compose configurado

---

## 🐳 Deploy Local com Docker (RECOMENDADO)

### Pré-requisitos
- Docker Desktop instalado e rodando
- 2GB de RAM disponível
- Portas 3000 e 3005 livres

### Executar Deploy

```bash
cd /c/Projetos/zendapag
chmod +x DEPLOY_LOCAL.sh
./DEPLOY_LOCAL.sh
```

### Acessar Aplicações

- **Dashboard**: http://localhost:3005
- **Landing**: http://localhost:3000

### Verificar Status

```bash
docker-compose -f docker-compose.frontend.yml ps
docker-compose -f docker-compose.frontend.yml logs -f
```

### Parar Serviços

```bash
docker-compose -f docker-compose.frontend.yml down
```

---

## 🌐 Deploy em Servidor Remoto (67.205.171.243)

### Opção 1: Usando Docker (Recomendado)

#### 1. Preparar servidor

```bash
ssh root@67.205.171.243

# Instalar Docker (se não instalado)
curl -fsSL https://get.docker.com | sh
systemctl start docker
systemctl enable docker

# Instalar Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Criar diretórios
mkdir -p /opt/zendapag
```

#### 2. Enviar arquivos para servidor

```bash
# No Windows (Git Bash)
cd /c/Projetos/zendapag

# Criar tarball do projeto
tar czf /tmp/zendapag-deploy.tar.gz \
    docker-compose.frontend.yml \
    zendapag-dashboard/Dockerfile.prod \
    zendapag-dashboard/nginx.conf \
    zendapag-dashboard/build/ \
    zendapag-landing/Dockerfile.prod \
    zendapag-landing/nginx.conf \
    zendapag-landing/build/ \
    zendapag-dashboard/package*.json \
    zendapag-landing/package*.json

# Enviar para servidor
scp /tmp/zendapag-deploy.tar.gz root@67.205.171.243:/tmp/

# Conectar no servidor
ssh root@67.205.171.243

# Extrair arquivos
cd /opt/zendapag
tar xzf /tmp/zendapag-deploy.tar.gz
rm /tmp/zendapag-deploy.tar.gz

# Iniciar containers
docker-compose -f docker-compose.frontend.yml up -d

# Verificar status
docker-compose -f docker-compose.frontend.yml ps
docker-compose -f docker-compose.frontend.yml logs -f
```

#### 3. Acessar aplicações

- **Dashboard**: http://67.205.171.243:3005
- **Landing**: http://67.205.171.243:3000

### Opção 2: Servir com Nginx Direto

#### 1. Instalar Nginx

```bash
ssh root@67.205.171.243
apt update
apt install -y nginx
```

#### 2. Criar configuração Nginx

```bash
cat > /etc/nginx/sites-available/zendapag-dashboard <<'EOF'
server {
    listen 3005;
    server_name 67.205.171.243;
    root /opt/zendapag/dashboard;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /static/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

cat > /etc/nginx/sites-available/zendapag-landing <<'EOF'
server {
    listen 3000;
    server_name 67.205.171.243;
    root /opt/zendapag/landing;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /static/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# Ativar sites
ln -s /etc/nginx/sites-available/zendapag-dashboard /etc/nginx/sites-enabled/
ln -s /etc/nginx/sites-available/zendapag-landing /etc/nginx/sites-enabled/

# Testar configuração
nginx -t

# Recarregar Nginx
systemctl reload nginx
```

#### 3. Enviar builds

```bash
# No Windows
cd /c/Projetos/zendapag

# Dashboard
tar czf /tmp/dashboard.tar.gz -C zendapag-dashboard/build .
scp /tmp/dashboard.tar.gz root@67.205.171.243:/tmp/

# Landing (se o build funcionar)
tar czf /tmp/landing.tar.gz -C zendapag-landing/build .
scp /tmp/landing.tar.gz root@67.205.171.243:/tmp/

# No servidor
ssh root@67.205.171.243
mkdir -p /opt/zendapag/dashboard /opt/zendapag/landing
tar xzf /tmp/dashboard.tar.gz -C /opt/zendapag/dashboard/
tar xzf /tmp/landing.tar.gz -C /opt/zendapag/landing/
systemctl reload nginx
```

---

## 🐛 Troubleshooting

### Landing Page - Erro de Build

O erro atual:
```
Cannot find module 'es-get-iterator'
```

**Solução temporária:**

```bash
cd /c/Projetos/zendapag/zendapag-landing

# Opção 1: Instalar dependência faltante
npm install es-get-iterator --save-dev

# Opção 2: Limpar e reinstalar
rm -rf node_modules package-lock.json
npm install

# Opção 3: Usar flag --legacy-peer-deps
npm install --legacy-peer-deps
npm run build
```

**Solução permanente:**
Atualizar react-scripts para versão mais recente ou remover workbox-webpack-plugin.

### Docker não inicia

```bash
# Verificar logs
docker-compose -f docker-compose.frontend.yml logs

# Verificar se portas estão em uso
netstat -ano | findstr ":3000"
netstat -ano | findstr ":3005"

# Parar e remover tudo
docker-compose -f docker-compose.frontend.yml down -v
docker system prune -a
```

### CSS não aplicando

1. Verificar se o build incluiu os CSS:
```bash
ls -la zendapag-dashboard/build/static/css/
```

2. Hard refresh no navegador: `Ctrl + Shift + R`

3. Verificar no DevTools > Network se o CSS está sendo carregado

---

## 📊 Checklist de Deploy

### Pré-deploy
- [x] Design system implementado
- [x] Dashboard build completo
- [ ] Landing page build completo (erro de dependência)
- [x] Dockerfiles criados
- [x] Nginx configs criados
- [x] Docker Compose configurado

### Deploy Local
- [ ] Docker Desktop rodando
- [ ] Executar DEPLOY_LOCAL.sh
- [ ] Testar dashboard em localhost:3005
- [ ] Testar landing em localhost:3000
- [ ] Verificar dark mode
- [ ] Testar responsividade

### Deploy Produção
- [ ] Servidor configurado (Docker ou Nginx)
- [ ] Arquivos enviados para servidor
- [ ] Containers iniciados
- [ ] Health checks passando
- [ ] URLs acessíveis:
  - [ ] http://67.205.171.243:3005 (dashboard)
  - [ ] http://67.205.171.243:3000 (landing)
- [ ] Validar design system aplicado
- [ ] Testar dark mode
- [ ] Lighthouse score > 90

---

## 🎯 Próximos Passos

### Imediato
1. ✅ Deploy local com Docker
2. ⏳ Corrigir build da landing page
3. ⏳ Deploy em produção

### Curto Prazo (Esta Semana)
1. Configurar domínio (zendapag.com)
2. Configurar SSL/HTTPS
3. Otimizar performance
4. Analytics e monitoramento

### Médio Prazo (Próximas 2 Semanas)
1. CI/CD com GitHub Actions
2. Testes automatizados
3. Storybook para componentes
4. Documentação expandida

---

## 📞 Suporte

### Comandos Úteis

```bash
# Ver status dos containers
docker-compose -f docker-compose.frontend.yml ps

# Ver logs
docker-compose -f docker-compose.frontend.yml logs -f dashboard
docker-compose -f docker-compose.frontend.yml logs -f landing

# Rebuild sem cache
docker-compose -f docker-compose.frontend.yml build --no-cache

# Remover tudo e recomeçar
docker-compose -f docker-compose.frontend.yml down -v
docker system prune -a
./DEPLOY_LOCAL.sh
```

### Arquivos Importantes

- `docker-compose.frontend.yml` - Orquestração dos frontends
- `zendapag-dashboard/Dockerfile.prod` - Build do dashboard
- `zendapag-landing/Dockerfile.prod` - Build da landing
- `DEPLOY_LOCAL.sh` - Script de deploy local
- `DEPLOY_QUICK.sh` - Script de deploy para servidor (requer rsync)

---

**🎨 Zendapag Design System v2.0**

**Status:** Pronto para deploy local, aguardando correção da landing page para deploy completo

**Versão:** 2.0.0
**Data:** Outubro 2025
