# Módulo de Saque PIX - Zendapag

## 📋 Visão Geral

O módulo de Saque PIX permite que usuários e merchants realizem saques de suas contas para chaves PIX de forma rápida e segura. O processamento é assíncrono via Apache Kafka, garantindo alta disponibilidade e escalabilidade.

## 🏗️ Arquitetura

### Componentes Principais

1. **Backend (zendapag-core)**
   - Entidade: `PixWithdrawal`
   - Repository: `PixWithdrawalRepository`
   - Service: `PixWithdrawalService`
   - DTOs: `CreatePixWithdrawalRequest`, `PixWithdrawalResponse`
   - Enum: `WithdrawalStatus`

2. **API (zendapag-api)**
   - Controller: `PixWithdrawalController`
   - Endpoints REST para gerenciamento de saques

3. **Worker (zendapag-worker)**
   - Consumer: `WithdrawalEventConsumer`
   - Processamento assíncrono via Kafka

4. **Frontend (zendapag-dashboard)**
   - Componente: `CreateWithdrawalModal`
   - Página: `WithdrawalsPage`

## 🔄 Fluxo de Processamento

```
1. Usuário → Frontend → Solicita Saque
2. API → Valida dados → Cria PixWithdrawal (PENDING)
3. API → Atualiza saldo da conta → Cria transação
4. API → Envia para Kafka → Topic: withdrawal-events
5. Worker → Consome mensagem → Processa saque
6. Worker → Integra com PIX → Realiza transferência
7. Worker → Atualiza status → COMPLETED
8. Notificação → Webhook → Merchant/User
```

## 📊 Estados do Saque

| Status | Descrição |
|--------|-----------|
| `PENDING` | Saque solicitado, aguardando processamento |
| `PROCESSING` | Saque em processamento |
| `APPROVED` | Saque aprovado, aguardando transferência |
| `COMPLETED` | Saque concluído com sucesso |
| `REJECTED` | Saque rejeitado |
| `CANCELLED` | Saque cancelado pelo usuário |
| `FAILED` | Saque falhou durante processamento |
| `REVERSED` | Saque estornado |

## 🔌 API Endpoints

### 1. Criar Saque

```http
POST /api/v1/withdrawals?accountId={accountId}&merchantId={merchantId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 1000.00,
  "pixKey": "123.456.789-00",
  "pixKeyType": "CPF",
  "description": "Saque mensal",
  "recipientName": "João Silva",
  "recipientDocument": "12345678900"
}
```

**Response:**
```json
{
  "success": true,
  "message": "PIX withdrawal created successfully",
  "data": {
    "id": "uuid",
    "referenceId": "WD-abc123",
    "amount": 1000.00,
    "feeAmount": 0.00,
    "netAmount": 1000.00,
    "pixKey": "123.456.789-00",
    "pixKeyType": "CPF",
    "status": "PENDING",
    "requestedAt": "2025-01-15T10:30:00Z"
  }
}
```

### 2. Buscar Saque por ID

```http
GET /api/v1/withdrawals/{id}
Authorization: Bearer {token}
```

### 3. Buscar por Referência

```http
GET /api/v1/withdrawals/reference/{referenceId}
Authorization: Bearer {token}
```

### 4. Listar Saques por Conta

```http
GET /api/v1/withdrawals/account/{accountId}?page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer {token}
```

### 5. Listar Saques por Merchant

```http
GET /api/v1/withdrawals/merchant/{merchantId}?page=0&size=20
Authorization: Bearer {token}
```

### 6. Listar por Status

```http
GET /api/v1/withdrawals/status/{status}?page=0&size=20
Authorization: Bearer {token}
```

### 7. Cancelar Saque

```http
POST /api/v1/withdrawals/{id}/cancel?reason=Usuario%20solicitou
Authorization: Bearer {token}
```

## ⚙️ Configurações

Adicione as seguintes configurações no `application.yml`:

```yaml
zendapag:
  withdrawal:
    # Valor máximo por saque
    max-amount: 50000.00

    # Valor mínimo por saque
    min-amount: 0.01

    # Limite diário de saques por conta
    daily-limit: 100000.00

    # Percentual de taxa
    fee-percentage: 0.00

    # Taxa fixa
    fixed-fee: 0.00

    # Máximo de saques pendentes por conta
    max-pending: 5

# Kafka topics
spring:
  kafka:
    topics:
      withdrawal-events: withdrawal-events
      withdrawal-processing: withdrawal-processing
      withdrawal-events-dlq: withdrawal-events-dlq
```

## 🔒 Validações e Limites

### Validações Automáticas

1. **Saldo Suficiente**: Verifica se a conta possui saldo >= valor solicitado
2. **Valor Mínimo**: R$ 0,01
3. **Valor Máximo**: R$ 50.000,00 (configurável)
4. **Limite Diário**: R$ 100.000,00 por conta (configurável)
5. **Saques Pendentes**: Máximo de 5 saques pendentes simultaneamente (configurável)
6. **Conta Ativa**: Apenas contas com status `ACTIVE` podem solicitar saques
7. **Chave PIX Válida**: Valida formato da chave PIX conforme tipo

### Análise de Risco

O módulo integra com `RiskService` para:
- Detectar padrões suspeitos
- Validar histórico de transações
- Aplicar regras de compliance

## 📦 Banco de Dados

### Tabela: `pix_withdrawals`

```sql
CREATE TABLE pix_withdrawals (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(100) UNIQUE NOT NULL,
    account_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    transaction_id UUID,
    amount DECIMAL(15,2) NOT NULL,
    fee_amount DECIMAL(15,2) DEFAULT 0,
    net_amount DECIMAL(15,2),
    pix_key VARCHAR(255) NOT NULL,
    pix_key_type VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(255),
    recipient_document VARCHAR(20),
    recipient_bank VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    rejection_reason VARCHAR(1000),
    pix_transaction_id VARCHAR(100),
    pix_end_to_end_id VARCHAR(100),
    pix_return_id VARCHAR(100),
    external_reference VARCHAR(255),
    external_transaction_id VARCHAR(255),
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    expires_at TIMESTAMP,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    metadata JSONB,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),

    CONSTRAINT fk_withdrawal_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_withdrawal_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_withdrawal_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

-- Índices
CREATE INDEX idx_withdrawal_reference ON pix_withdrawals(reference_id);
CREATE INDEX idx_withdrawal_account ON pix_withdrawals(account_id);
CREATE INDEX idx_withdrawal_merchant ON pix_withdrawals(merchant_id);
CREATE INDEX idx_withdrawal_status ON pix_withdrawals(status);
CREATE INDEX idx_withdrawal_created_at ON pix_withdrawals(created_at);
CREATE INDEX idx_withdrawal_pix_key ON pix_withdrawals(pix_key);
CREATE INDEX idx_withdrawal_deleted ON pix_withdrawals(deleted);
CREATE INDEX idx_withdrawal_composite ON pix_withdrawals(merchant_id, status, created_at);
```

## 🎯 Kafka Topics

### 1. `withdrawal-events`

**Producer**: `PixWithdrawalService`
**Consumer**: `WithdrawalEventConsumer`
**Mensagem**: Objeto `PixWithdrawal` completo
**Uso**: Processamento assíncrono de saques

### 2. `withdrawal-processing`

**Producer**: API
**Consumer**: `WithdrawalEventConsumer`
**Mensagem**: String com UUID do saque
**Uso**: Formato alternativo para processamento

### 3. `withdrawal-events-dlq`

**Dead Letter Queue** para mensagens que falharam após todas as tentativas de retry.

## 🔧 Uso no Frontend

### Importar Componentes

```typescript
import CreateWithdrawalModal from '@/components/CreateWithdrawalModal';
import WithdrawalsPage from '@/pages/WithdrawalsPage';
```

### Exemplo de Uso

```typescript
const [modalOpen, setModalOpen] = useState(false);

<CreateWithdrawalModal
  open={modalOpen}
  onClose={() => setModalOpen(false)}
  onSuccess={(withdrawalId) => {
    console.log('Withdrawal created:', withdrawalId);
    // Recarregar lista ou redirecionar
  }}
  accountBalance={15000.00}
  accountId="account-uuid"
  merchantId="merchant-uuid"
/>
```

## 📈 Métricas e Monitoramento

### Métricas Expostas (Prometheus)

- `kafka.withdrawal.events.received` - Total de eventos recebidos
- `kafka.withdrawal.events.success` - Eventos processados com sucesso
- `kafka.withdrawal.events.error` - Eventos com erro
- `kafka.withdrawal.events.processing.time` - Tempo de processamento

### Logs Importantes

```
INFO  - Creating PIX withdrawal for account: {accountId}, amount: {amount}
INFO  - PIX withdrawal created successfully: {referenceId}
INFO  - Processing withdrawal: {withdrawalId}
INFO  - Withdrawal processed successfully: {referenceId}
ERROR - Failed to process withdrawal {withdrawalId}: {error}
```

## 🧪 Testes

### Testes Unitários

```bash
# Testar service
./mvnw test -Dtest=PixWithdrawalServiceTest

# Testar repository
./mvnw test -Dtest=PixWithdrawalRepositoryTest

# Testar controller
./mvnw test -Dtest=PixWithdrawalControllerTest
```

### Testes de Integração

```bash
# Testar fluxo completo
./mvnw test -Dtest=PixWithdrawalIntegrationTest
```

## 🚀 Deploy

### 1. Build

```bash
# Build do projeto
make build

# Build Docker
make docker-build
```

### 2. Executar Migrações

```bash
# Flyway migrations
./mvnw flyway:migrate
```

### 3. Iniciar Serviços

```bash
# Ambiente completo
make up-full

# Apenas infraestrutura
make up
```

## 🔐 Segurança

### Autenticação e Autorização

- Todos os endpoints requerem autenticação via JWT
- Roles suportadas:
  - `MERCHANT`: Pode criar e gerenciar saques
  - `USER`: Pode criar e visualizar próprios saques
  - `ADMIN`: Acesso completo

### Auditoria

Todas as operações são auditadas via `AuditService`:
- Criação de saques
- Alterações de status
- Cancelamentos
- Falhas e erros

### Rate Limiting

- Limite de requisições: Configurável via `@RateLimiter`
- Circuit Breaker: Proteção contra falhas em cascata
- Retry automático: Até 3 tentativas com backoff exponencial

## 📝 Próximas Implementações

- [ ] Webhook para notificação de conclusão
- [ ] Agendamento de saques
- [ ] Saques recorrentes
- [ ] Dashboard de analytics de saques
- [ ] Exportação de relatórios
- [ ] Integração com múltiplos provedores PIX
- [ ] Suporte a saques em lote

## 🤝 Contribuindo

1. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
2. Commit suas mudanças: `git commit -m 'Add: nova funcionalidade'`
3. Push para a branch: `git push origin feature/nova-funcionalidade`
4. Abra um Pull Request

## 📄 Licença

Este módulo faz parte do projeto Zendapag.

## 📞 Suporte

Para suporte ou dúvidas sobre o módulo de Saque PIX:
- Email: suporte@zendapag.com
- Documentação: https://docs.zendapag.com
- Issues: https://github.com/zendapag/issues
