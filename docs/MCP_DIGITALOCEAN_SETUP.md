# Digital Ocean MCP - Model Context Protocol

**Data de Instalação:** 2025-01-20
**Projeto:** Zendapag PIX Payment Platform

---

## 📦 O que é MCP?

O **Model Context Protocol (MCP)** é um protocolo que permite ao Claude Code se conectar a APIs externas e executar operações diretamente através de ferramentas especializadas.

Com o MCP da Digital Ocean, o Claude pode:
- Criar e gerenciar Droplets
- Gerenciar Container Registry
- Configurar Load Balancers
- Gerenciar volumes e snapshots
- Configurar firewalls
- E muito mais!

---

## ✅ Status da Instalação

### Pacote Instalado

```bash
# Pacote NPM
@digitalocean/mcp v1.0.11

# Localização
C:\home\kleber\.npm-global\node_modules\@digitalocean\mcp

# Executável
C:\home\kleber\.npm-global\node_modules\@digitalocean\mcp\dist\mcp-digitalocean-windows-amd64.exe
```

### Configuração no Projeto

**Arquivo:** `.claude/settings.local.json`

```json
{
  "mcpServers": {
    "digitalocean": {
      "command": "npx",
      "args": [
        "@digitalocean/mcp",
        "--services",
        "apps,databases,droplets,volumes,kubernetes,registries,load-balancers,firewalls,domains,images,ssh-keys,snapshots,tags"
      ],
      "env": {
        "DIGITALOCEAN_API_TOKEN": "dop_v1_e23961c191de5cf80ed9c953e8f36aa5b1a1f613f646cde394795d90a67ec6e5"
      }
    }
  }
}
```

---

## 🔧 Serviços Habilitados

O MCP foi configurado com os seguintes serviços da Digital Ocean:

| Serviço | Descrição | Exemplos de Uso |
|---------|-----------|-----------------|
| **apps** | App Platform | Deploy de aplicações |
| **databases** | Managed Databases | PostgreSQL, Redis, etc |
| **droplets** | Virtual Machines | Criar, listar, gerenciar VMs |
| **volumes** | Block Storage | Adicionar storage aos droplets |
| **kubernetes** | Kubernetes Clusters | Gerenciar clusters K8s |
| **registries** | Container Registry | Gerenciar imagens Docker |
| **load-balancers** | Load Balancers | Distribuir tráfego |
| **firewalls** | Cloud Firewalls | Regras de segurança |
| **domains** | DNS Management | Gerenciar domínios |
| **images** | Custom Images | Snapshots e backups |
| **ssh-keys** | SSH Keys | Autenticação |
| **snapshots** | Snapshots | Backups de droplets |
| **tags** | Resource Tags | Organização |

---

## 🚀 Como Usar

### Comandos Básicos

Agora você pode pedir ao Claude para executar operações na Digital Ocean diretamente:

```
# Listar droplets
"Liste todos os droplets da minha conta Digital Ocean"

# Criar droplet
"Crie um droplet Ubuntu 22.04 com 2GB RAM chamado 'zendapag-prod'"

# Ver container registry
"Mostre minhas imagens no Container Registry"

# Criar firewall
"Configure um firewall permitindo apenas portas 80, 443 e 22"

# Listar bancos de dados
"Liste todos os bancos de dados gerenciados"
```

### Exemplos Práticos

#### 1. Criar Droplet para Zendapag

```
Claude, crie um droplet com as seguintes especificações:
- Nome: zendapag-prod
- Imagem: ubuntu-22-04-x64
- Size: s-2vcpu-4gb
- Region: nyc1
- Tags: zendapag, production
```

#### 2. Configurar Container Registry

```
Claude, crie um Container Registry chamado 'zendapag'
com tier 'basic' e depois liste todos os repositórios
```

#### 3. Verificar Recursos

```
Claude, me mostre:
1. Todos os droplets ativos
2. Uso do Container Registry
3. Bancos de dados rodando
4. Load balancers configurados
```

#### 4. Gerenciar Imagens

```
Claude, liste todas as imagens Docker no registry 'zendapag'
e me mostre quais tags existem para cada imagem
```

---

## 📚 Ferramentas Disponíveis

O MCP da Digital Ocean disponibiliza as seguintes ferramentas para o Claude:

### Droplets
- `droplets_create` - Criar novo droplet
- `droplets_list` - Listar droplets
- `droplets_get` - Obter detalhes de um droplet
- `droplets_delete` - Deletar droplet
- `droplets_actions` - Ações (power on/off, reboot, resize)
- `droplets_kernels` - Listar kernels disponíveis
- `droplets_snapshots` - Criar snapshots
- `droplets_backups` - Gerenciar backups

### Container Registry
- `registry_create` - Criar registry
- `registry_get` - Ver detalhes do registry
- `registry_delete` - Deletar registry
- `registry_list_repositories` - Listar repositórios
- `registry_list_repository_tags` - Listar tags
- `registry_delete_repository_tag` - Deletar tag

### Databases
- `databases_create` - Criar banco de dados
- `databases_list` - Listar bancos
- `databases_get` - Ver detalhes
- `databases_delete` - Deletar banco
- `databases_resize` - Redimensionar
- `databases_migrate` - Migrar região

### Load Balancers
- `load_balancers_create` - Criar load balancer
- `load_balancers_list` - Listar load balancers
- `load_balancers_get` - Ver detalhes
- `load_balancers_update` - Atualizar configuração
- `load_balancers_delete` - Deletar

### Firewalls
- `firewalls_create` - Criar firewall
- `firewalls_list` - Listar firewalls
- `firewalls_get` - Ver regras
- `firewalls_update` - Atualizar regras
- `firewalls_delete` - Deletar

### Volumes
- `volumes_create` - Criar volume
- `volumes_list` - Listar volumes
- `volumes_get` - Ver detalhes
- `volumes_delete` - Deletar volume
- `volumes_attach` - Anexar a droplet
- `volumes_detach` - Desanexar de droplet

### SSH Keys
- `ssh_keys_create` - Adicionar chave SSH
- `ssh_keys_list` - Listar chaves
- `ssh_keys_get` - Ver detalhes da chave
- `ssh_keys_update` - Atualizar nome
- `ssh_keys_delete` - Deletar chave

### Domains
- `domains_create` - Criar domínio
- `domains_list` - Listar domínios
- `domains_get` - Ver detalhes
- `domains_delete` - Deletar domínio
- `domain_records_create` - Criar record DNS
- `domain_records_list` - Listar records
- `domain_records_update` - Atualizar record
- `domain_records_delete` - Deletar record

### Images
- `images_list` - Listar imagens
- `images_get` - Ver detalhes
- `images_update` - Atualizar metadados
- `images_delete` - Deletar imagem

### Snapshots
- `snapshots_list` - Listar snapshots
- `snapshots_get` - Ver detalhes
- `snapshots_delete` - Deletar snapshot

### Tags
- `tags_create` - Criar tag
- `tags_list` - Listar tags
- `tags_get` - Ver recursos com tag
- `tags_delete` - Deletar tag

### Kubernetes
- `kubernetes_create_cluster` - Criar cluster K8s
- `kubernetes_list_clusters` - Listar clusters
- `kubernetes_get_cluster` - Ver detalhes
- `kubernetes_delete_cluster` - Deletar cluster
- `kubernetes_update_cluster` - Atualizar cluster
- `kubernetes_get_kubeconfig` - Obter kubeconfig

---

## 🔐 Segurança

### Token API

O token da Digital Ocean está configurado apenas **localmente** no projeto Zendapag:
- **Arquivo:** `.claude/settings.local.json`
- **Escopo:** Local (não commitado no Git)
- **Permissões:** Read + Write (full access)

### ⚠️ IMPORTANTE

1. **NÃO commite** o arquivo `.claude/settings.local.json` no Git
2. O arquivo está no `.gitignore` do projeto
3. Cada desenvolvedor deve configurar seu próprio token
4. Para produção, use tokens com escopo limitado

### Adicionar ao .gitignore

```bash
# Já configurado
.claude/settings.local.json
```

---

## 🧪 Testar Instalação

### Teste Rápido

```bash
# No terminal
cd /c/Projetos/zendapag
export DIGITALOCEAN_API_TOKEN=dop_v1_e23961c191de5cf80ed9c953e8f36aa5b1a1f613f646cde394795d90a67ec6e5
npx @digitalocean/mcp --services droplets
```

### Teste com Claude Code

No Claude Code, tente:

```
Claude, liste todos os droplets da minha conta Digital Ocean
```

Se o MCP estiver funcionando, você verá uma lista dos droplets.

---

## 📖 Documentação

### Links Úteis

- **MCP GitHub**: https://github.com/digitalocean/mcp-digitalocean
- **NPM Package**: https://www.npmjs.com/package/@digitalocean/mcp
- **Digital Ocean API**: https://docs.digitalocean.com/reference/api/
- **MCP Protocol**: https://github.com/mark3labs/mcp-go

### README do Pacote

O README completo está em:
```
C:\home\kleber\.npm-global\node_modules\@digitalocean\mcp\README.md
```

---

## 🔄 Comandos de Manutenção

### Atualizar MCP

```bash
npm install -g @digitalocean/mcp@latest
```

### Verificar Versão

```bash
npm list -g @digitalocean/mcp
```

### Reinstalar

```bash
npm uninstall -g @digitalocean/mcp
npm install -g @digitalocean/mcp
```

### Verificar Tools Disponíveis

```bash
cd /c/Projetos/zendapag
npx @digitalocean/mcp --help
```

---

## 🐛 Troubleshooting

### Problema: "API token not provided"

**Solução:** Verifique se o token está no `.claude/settings.local.json`:
```bash
cat .claude/settings.local.json | grep DIGITALOCEAN_API_TOKEN
```

### Problema: MCP não aparece no Claude

**Solução:** Reinicie o Claude Code:
```bash
# No terminal, pressione Ctrl+C e depois
claude
```

### Problema: "Command not found: npx"

**Solução:** Instale o Node.js:
```bash
node --version  # Deve retornar v18+
npm --version   # Deve retornar v8+
```

### Problema: Timeout ao executar comandos

**Solução:** Configure timeout maior no `.claude/settings.local.json`:
```json
{
  "mcpServers": {
    "digitalocean": {
      "timeout": 30000,
      ...
    }
  }
}
```

---

## 📊 Status Atual

| Item | Status | Observações |
|------|--------|-------------|
| **Pacote Instalado** | ✅ | v1.0.11 |
| **Configuração** | ✅ | settings.local.json |
| **Token Configurado** | ✅ | Read + Write |
| **Serviços Ativos** | ✅ | 13 serviços |
| **Testado** | ⚠️ | Aguardando teste no Claude |

---

## 🎯 Próximos Passos

### 1. Testar MCP no Claude Code

```
Claude, liste todos os droplets da minha conta Digital Ocean
```

### 2. Criar Droplet de Produção

```
Claude, usando o MCP da Digital Ocean, crie o droplet
de produção conforme especificado em docs/digital-ocean-setup.md
```

### 3. Configurar Container Registry

```
Claude, crie o Container Registry 'zendapag' com tier 'basic'
```

### 4. Configurar Secrets no GitHub

Após criar a infraestrutura:
```bash
# Obter IP do droplet via MCP
# Claude, me dê o IP do droplet 'zendapag-prod'

# Configurar secrets
gh secret set DO_DROPLET_IP --body "IP_DO_DROPLET"
```

---

## ✨ Benefícios

Com o MCP da Digital Ocean instalado, você pode:

1. ✅ **Automatizar criação de infraestrutura**
   - Criar droplets, databases, registries
   - Tudo via conversa natural com o Claude

2. ✅ **Gerenciar recursos existentes**
   - Listar, atualizar, deletar recursos
   - Monitorar uso e custos

3. ✅ **Debugging e troubleshooting**
   - Verificar status de serviços
   - Diagnosticar problemas

4. ✅ **Integração com CI/CD**
   - Configurar secrets automaticamente
   - Validar infraestrutura

5. ✅ **Documentação automática**
   - Gerar relatórios de infraestrutura
   - Manter docs atualizados

---

## 📝 Exemplo de Workflow Completo

```
Você: Claude, vamos fazer o setup completo da infraestrutura Digital Ocean

Claude: Claro! Vou começar listando os recursos existentes...
        [usa droplets_list]

Claude: Vejo que não há droplets. Vou criar o droplet de produção...
        [usa droplets_create com specs de docs/digital-ocean-setup.md]

Claude: Droplet criado! IP: 123.45.67.89
        Agora vou criar o Container Registry...
        [usa registry_create]

Claude: Registry 'zendapag' criado com sucesso!
        Vou configurar os secrets do GitHub agora...
        [usa Bash para rodar gh secret set]

Claude: Infraestrutura completa!
        - Droplet: zendapag-prod (123.45.67.89)
        - Registry: registry.digitalocean.com/zendapag
        - Secrets: configurados no GitHub

        Pronto para deploy! Quer que eu faça o primeiro deploy?
```

---

**Instalado em:** 2025-01-20
**Por:** Claude Code
**Token Válido até:** Não expira (mas pode ser revogado)
**Última Atualização:** 2025-01-20
