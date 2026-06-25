# Integração Zendpag — Guia da API

Documentação de integração com a API da Zendpag (gateway de pagamentos). Cobre
autenticação, cobrança PIX, consulta de transações, saque PIX (cash-out),
consulta de saldo, webhooks e códigos de erro.

> **Base desta documentação:** endpoints **implementados** no código (módulo
> `zendapag-api`). Onde o comportamento depende do ambiente sandbox (sem PSP
> real), isso está sinalizado.

---

## Sumário

1. [Convenções](#1-convenções)
2. [Autenticação](#2-autenticação)
3. [Criar cobrança PIX](#3-criar-cobrança-pix)
4. [Consultar status de transação](#4-consultar-status-de-transação)
5. [Criar saque PIX (cash-out)](#5-criar-saque-pix-cash-out)
6. [Consultar saldo](#6-consultar-saldo)
7. [Webhooks](#7-webhooks)
8. [Códigos de erro](#8-códigos-de-erro)

---

## 1. Convenções

- **Base URL (exemplo):** `https://api.zendpag.com` — em desenvolvimento, `http://localhost:8080`.
- **Prefixo de versão:** todas as rotas começam com `/api/v1`.
- **Formato:** JSON em requisições e respostas (`Content-Type: application/json`).
- **Moeda:** `BRL` (centavos representados em decimal, ex.: `100.00`).

### Envelope de resposta

Toda resposta usa o mesmo envelope (`ApiResponse`):

```json
{
  "success": true,
  "message": "Mensagem legível",
  "data": { },
  "error": null,
  "timestamp": "2026-06-25T17:18:51.123"
}
```

Em caso de erro, `success = false`, `data = null` e `error` traz o **código do erro**
(ver [seção 8](#8-códigos-de-erro)).

---

## 2. Autenticação

A API usa **JWT (Bearer token)**. O cliente se autentica com e-mail/usuário e senha
e recebe um token, que deve ser enviado no header `Authorization` de cada chamada
autenticada.

> **Nota sobre API Keys:** existe um endpoint que **gera** uma API Key
> (`POST /api/v1/merchants/me/api-keys`), porém no estado atual ela ainda **não é
> persistida nem aceita como mecanismo de autenticação** — o fluxo suportado hoje
> é o JWT abaixo. Use a API Key apenas como referência futura.

### 2.1 Registrar conta

```
POST /api/v1/auth/register
```

```json
{
  "username": "loja.aurora",
  "email": "financeiro@auroradigital.com.br",
  "password": "SenhaForte@123",
  "fullName": "Aurora Digital LTDA",
  "cpfCnpj": "12345678000190"
}
```

### 2.2 Login (obter o token)

```
POST /api/v1/auth/login
```

```json
{
  "usernameOrEmail": "financeiro@auroradigital.com.br",
  "password": "SenhaForte@123"
}
```

**Resposta** (`data`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiIt...",
  "type": "Bearer",
  "user": { "id": "...", "email": "...", "roles": ["MERCHANT"] }
}
```

### 2.3 Usar o token

Envie o token em todas as chamadas autenticadas:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIt...
```

```bash
curl -X GET https://api.zendpag.com/api/v1/merchants/me/balance \
  -H "Authorization: Bearer $TOKEN"
```

### 2.4 Renovar o token

```
POST /api/v1/auth/refresh
Authorization: Bearer <token-atual>
```

---

## 3. Criar cobrança PIX

Cria uma cobrança PIX e retorna o QR Code (copia-e-cola e imagem) para o pagador.

```
POST /api/v1/payments/pix
Authorization: Bearer <token>        (perfil MERCHANT)
```

### Request — `CreatePixPaymentRequest`

| Campo | Tipo | Obrigatório | Observação |
|---|---|:--:|---|
| `referenceId` | string (≤255) | ✅ | Seu identificador único da cobrança (idempotência do seu lado) |
| `amount` | decimal | ✅ | Valor bruto, mínimo `0.01` |
| `currency` | string | ✅ | `BRL` (default) |
| `description` | string (≤1000) | — | Descrição da cobrança |
| `customerEmail` | string | — | E-mail do pagador |
| `customerName` | string (≤255) | — | Nome do pagador |
| `customerDocument` | string (≤20) | — | CPF/CNPJ do pagador |
| `customerPhone` | string | — | Telefone do pagador |
| `pixKey` | string (≤255) | — | Chave PIX de recebimento (se aplicável) |
| `pixKeyType` | string | — | Tipo da chave (`CPF`/`CNPJ`/`EMAIL`/`PHONE`/`RANDOM`) |
| `expirationMinutes` | int | — | Validade do QR Code (default `60`) |
| `notificationUrl` | string (≤1000) | — | URL para notificação específica desta cobrança |
| `externalId` | string (≤500) | — | Seu identificador externo adicional |

```bash
curl -X POST https://api.zendpag.com/api/v1/payments/pix \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "referenceId": "PEDIDO-2026-0001",
        "amount": 100.00,
        "currency": "BRL",
        "description": "Curso de Marketing Digital",
        "customerName": "João da Silva",
        "customerEmail": "joao@email.com",
        "customerDocument": "12345678909",
        "expirationMinutes": 30
      }'
```

### Response — `201 Created` (`data` = `PaymentResponse`)

```json
{
  "success": true,
  "message": "PIX payment created successfully",
  "data": {
    "id": "fa94b1e5-37d5-4c86-821e-14bcf88ab217",
    "referenceId": "PEDIDO-2026-0001",
    "externalId": null,
    "amount": 100.00,
    "currency": "BRL",
    "status": "PENDING",
    "description": "Curso de Marketing Digital",
    "merchantId": "f75e004e-70b6-496b-90d0-88b287349971",
    "merchantName": "Loja Aurora Digital",
    "pixKey": "...",
    "pixKeyType": "RANDOM",
    "pixQrCode": "data:image/png;base64,iVBORw0KGgo...",
    "pixQrCodeText": "00020126580014br.gov.bcb.pix...",
    "pixTransactionId": "...",
    "grossAmount": 100.00,
    "feeAmount": null,
    "netAmount": null,
    "createdAt": "2026-06-25T17:18:51Z",
    "expiresAt": "2026-06-25T17:48:51Z"
  }
}
```

- `pixQrCodeText` → código **copia-e-cola** PIX.
- `pixQrCode` → imagem do QR Code (data URI).
- `status` inicial é `PENDING`. Quando o pagamento é confirmado, a taxa (`feeAmount`)
  e o líquido (`netAmount`) são preenchidos e o `status` passa a `APPROVED`.

> **Sandbox:** sem PSP real conectado, a confirmação do pagamento é simulada por
> uma ação administrativa interna. Em produção, a confirmação chega pelo provedor
> PIX e dispara o webhook `PAYMENT_COMPLETED` (ver [seção 7](#7-webhooks)).

---

## 4. Consultar status de transação

Há duas formas de consultar uma cobrança: pelo **ID** da Zendpag ou pelo **seu**
`referenceId`.

### 4.1 Por ID

```
GET /api/v1/payments/{id}
Authorization: Bearer <token>
```

### 4.2 Pelo seu referenceId

```
GET /api/v1/payments/reference/{referenceId}
Authorization: Bearer <token>
```

```bash
curl -X GET https://api.zendpag.com/api/v1/payments/reference/PEDIDO-2026-0001 \
  -H "Authorization: Bearer $TOKEN"
```

A resposta é o mesmo `PaymentResponse` da seção 3. Verifique o campo `status`:

| `status` | Significado |
|---|---|
| `PENDING` | Aguardando pagamento |
| `PROCESSING` | Em processamento |
| `APPROVED` | Pago e aprovado (líquido creditado) |
| `REFUNDED` | Estornado |
| `REJECTED` / `FAILED` | Não aprovado |
| `CANCELLED` | Cancelado |
| `EXPIRED` | QR Code expirou |
| `CHARGEBACK` | Em disputa/chargeback |

### 4.3 Listar pagamentos (paginado)

```
GET /api/v1/payments?page=0&size=20
Authorization: Bearer <token>
```

---

## 5. Criar saque PIX (cash-out)

Solicita o saque do saldo disponível para uma chave PIX.

```
POST /api/v1/withdrawals?accountId={accountId}&merchantId={merchantId}
Authorization: Bearer <token>        (perfil MERCHANT ou USER)
```

> Os parâmetros `accountId` (conta de saldo) e `merchantId` (estabelecimento) vão
> na **query string**; os dados do saque vão no **corpo**.

### Request — `CreatePixWithdrawalRequest`

| Campo | Tipo | Obrigatório | Observação |
|---|---|:--:|---|
| `amount` | decimal | ✅ | Valor do saque, mínimo `0.01` |
| `pixKey` | string | ✅ | Chave PIX de destino |
| `pixKeyType` | string | ✅ | `CPF`/`CNPJ`/`EMAIL`/`PHONE`/`RANDOM` |
| `description` | string | — | Descrição |
| `externalReference` | string | — | Seu identificador externo |
| `recipientName` | string | — | Nome do recebedor |
| `recipientDocument` | string | — | Documento do recebedor |

```bash
curl -X POST "https://api.zendpag.com/api/v1/withdrawals?accountId=10&merchantId=f75e004e-70b6-496b-90d0-88b287349971" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "amount": 500.00,
        "pixKey": "financeiro@auroradigital.com.br",
        "pixKeyType": "EMAIL",
        "recipientName": "Aurora Digital LTDA",
        "recipientDocument": "12345678000190"
      }'
```

### Response — `PixWithdrawalResponse`

```json
{
  "success": true,
  "message": "Withdrawal created",
  "data": {
    "id": "...",
    "referenceId": "WD1750000000ABCD1234",
    "amount": 500.00,
    "feeAmount": 2.50,
    "netAmount": 497.50,
    "pixKey": "financeiro@auroradigital.com.br",
    "pixKeyType": "EMAIL",
    "status": "PENDING",
    "balanceBefore": 1000.00,
    "balanceAfter": 500.00,
    "requestedAt": "2026-06-25T17:20:00Z"
  }
}
```

**Regras de saque (validadas pela API):**
- Saldo insuficiente → erro de negócio.
- Limite diário: até **10 saques/dia** por conta e até **R$ 50.000,00/dia** somados.

**Status do saque** (`WithdrawalStatus`): `PENDING` → `PROCESSING` → `APPROVED` →
`COMPLETED`; ou `REJECTED` / `CANCELLED` / `FAILED` / `REVERSED`.

> **Sandbox:** a aprovação do saque é uma ação administrativa que dispara o
> webhook `WITHDRAWAL_COMPLETED`. O cancelamento dispara `WITHDRAWAL_FAILED`.

### Consultar um saque

```
GET /api/v1/withdrawals/{id}
GET /api/v1/withdrawals/reference/{referenceId}
```

---

## 6. Consultar saldo

```
GET /api/v1/merchants/me/balance
Authorization: Bearer <token>        (perfil MERCHANT)
```

### Response — `BalanceResponse`

```json
{
  "success": true,
  "message": "Balance retrieved",
  "data": {
    "balance": 1098.01,
    "currency": "BRL",
    "timestamp": "2026-06-25T17:25:00Z"
  }
}
```

`balance` é o saldo disponível da conta do estabelecimento autenticado.

---

## 7. Webhooks

A Zendpag notifica o endpoint do cliente via **HTTP POST** quando eventos
acontecem. Cada POST é **assinado com HMAC-SHA256** para o cliente validar a
autenticidade, e tem **retentrega automática com backoff** em caso de falha.

### 7.1 Configurar o webhook

Defina a URL de destino e o **segredo** (usado na assinatura):

```
POST /api/v1/webhooks
Authorization: Bearer <token>        (perfil MERCHANT)
```

```json
{
  "url": "https://meusite.com/webhooks/zendpag",
  "secret": "whsec_meu_segredo_super_secreto",
  "eventTypes": ["PAYMENT_COMPLETED", "PAYMENT_REFUNDED", "DISPUTE_CREATED"]
}
```

Alternativa (apenas URL/segredo):

```
PUT /api/v1/merchants/me/webhook-url?webhookUrl=...&webhookSecret=...
```

Enviar um webhook de teste para validar o endpoint:

```
POST /api/v1/webhooks/test
```

### 7.2 Eventos disponíveis

| Evento | Quando dispara |
|---|---|
| `PAYMENT_COMPLETED` | Pagamento aprovado (líquido creditado) |
| `PAYMENT_FAILED` | Pagamento recusado |
| `PAYMENT_REFUNDED` | Pagamento estornado |
| `WITHDRAWAL_COMPLETED` | Saque aprovado/concluído |
| `WITHDRAWAL_FAILED` | Saque cancelado/falhou |
| `DISPUTE_CREATED` | Disputa/chargeback aberto sobre um pagamento |

### 7.3 Headers do POST

| Header | Conteúdo |
|---|---|
| `Content-Type` | `application/json` |
| `X-Zendapag-Event` | Nome do evento (ex.: `PAYMENT_COMPLETED`) |
| `X-Zendapag-Webhook-Id` | ID único desta entrega |
| `X-Zendapag-Signature` | Assinatura HMAC: `sha256=<hex>` |

### 7.4 Estrutura do payload

**Eventos de pagamento** (`PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `PAYMENT_REFUNDED`):

```json
{
  "event": "PAYMENT_COMPLETED",
  "payment_id": "fa94b1e5-37d5-4c86-821e-14bcf88ab217",
  "reference_id": "PEDIDO-2026-0001",
  "status": "APPROVED",
  "amount": 100.00,
  "fee": 1.99,
  "net": 98.01,
  "merchant_id": "f75e004e-70b6-496b-90d0-88b287349971"
}
```

**Eventos de saque** (`WITHDRAWAL_COMPLETED`, `WITHDRAWAL_FAILED`):

```json
{
  "event": "WITHDRAWAL_COMPLETED",
  "withdrawal_id": "...",
  "reference_id": "WD1750000000ABCD1234",
  "status": "PROCESSING",
  "amount": 500.00,
  "net": 497.50,
  "merchant_id": "f75e004e-70b6-496b-90d0-88b287349971"
}
```

**Evento de disputa** (`DISPUTE_CREATED`):

```json
{
  "event": "DISPUTE_CREATED",
  "dispute_id": "dda8a1e7-807c-4457-98db-a425faa07dda",
  "external_id": "DSP-CC44C77B",
  "status": "OPENED",
  "reason": "FRAUD",
  "amount": 100.00,
  "payment_id": "fa94b1e5-37d5-4c86-821e-14bcf88ab217",
  "payment_reference_id": "PEDIDO-2026-0001",
  "merchant_id": "f75e004e-70b6-496b-90d0-88b287349971"
}
```

### 7.5 Validar a assinatura HMAC

A assinatura é o **HMAC-SHA256 do corpo bruto recebido**, usando o seu `secret`,
em hexadecimal, prefixado por `sha256=`. **Valide sobre o corpo bruto (raw body)**,
antes de qualquer parsing/reserialização.

`X-Zendapag-Signature = "sha256=" + hex( HMAC_SHA256(secret, rawBody) )`

**Node.js:**

```js
const crypto = require("crypto");

function isValidSignature(rawBody, signatureHeader, secret) {
  const expected = "sha256=" + crypto
    .createHmac("sha256", secret)
    .update(rawBody, "utf8")
    .digest("hex");
  // comparação em tempo constante
  return crypto.timingSafeEqual(
    Buffer.from(signatureHeader),
    Buffer.from(expected)
  );
}
```

**PHP:**

```php
$expected = 'sha256=' . hash_hmac('sha256', $rawBody, $secret);
$valid = hash_equals($expected, $_SERVER['HTTP_X_ZENDAPAG_SIGNATURE']);
```

Responda **HTTP 2xx** para confirmar o recebimento. Qualquer resposta fora da
faixa 2xx (ou timeout/erro de conexão) marca a entrega como falha e agenda
**retentativa com backoff exponencial** (≈ 60s, 120s, 240s, 480s, 960s — até 5
tentativas).

### 7.6 Acompanhar entregas

```
GET  /api/v1/webhooks/events            # lista entregas e status (paginado)
GET  /api/v1/webhooks/events/{id}       # detalhe de uma entrega
POST /api/v1/webhooks/events/{id}/retry # reentrega manual de uma falha
GET  /api/v1/webhooks/stats             # estatísticas de entrega
```

---

## 8. Códigos de erro

Erros seguem o envelope `ApiResponse` com `success: false` e o código em `error`:

```json
{
  "success": false,
  "message": "Insufficient balance",
  "data": null,
  "error": "BUSINESS_ERROR",
  "timestamp": "2026-06-25T17:30:00.000"
}
```

Para erros de validação, `error = "VALIDATION_ERROR"` e os detalhes vêm em
`validation_errors`:

```json
{
  "success": false,
  "message": "Validation failed",
  "error": "VALIDATION_ERROR",
  "data": null
}
```

### Mapa de erros

| HTTP | `error` | Quando |
|:--:|---|---|
| `400` | `VALIDATION_ERROR` | Corpo/parametro inválido (campos obrigatórios, formato) |
| `400` | `BUSINESS_ERROR` | Regra de negócio violada (ex.: saldo insuficiente, limite diário) |
| `400` | *(código específico)* | Ex.: `INVALID_WEBHOOK_URL`, `WEBHOOK_URL_NOT_CONFIGURED`, `PAYMENT_PROCESSING_ERROR` |
| `401` | — | Token ausente/inválido (não autenticado) |
| `403` | `ACCESS_DENIED` | Recurso pertence a outro estabelecimento / sem permissão |
| `404` | `RESOURCE_NOT_FOUND` | Recurso inexistente (pagamento, saque, webhook) |
| `409` | *(conflito)* | Estado inválido para a operação (ex.: status incompatível) |
| `402` | *(limites)* | Limites insuficientes para a operação |
| `429` | — | Rate limit excedido |
| `500` | `INTERNAL_ERROR` | Erro inesperado |

> Cada operação valida o escopo do estabelecimento (multi-tenant): consultar um
> pagamento/saque de outro estabelecimento retorna `403 ACCESS_DENIED`.

---

## Resumo dos endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Criar conta |
| `POST` | `/api/v1/auth/login` | Login (obter JWT) |
| `POST` | `/api/v1/auth/refresh` | Renovar token |
| `POST` | `/api/v1/payments/pix` | Criar cobrança PIX |
| `GET` | `/api/v1/payments/{id}` | Consultar pagamento por ID |
| `GET` | `/api/v1/payments/reference/{referenceId}` | Consultar por referenceId |
| `GET` | `/api/v1/payments` | Listar pagamentos |
| `POST` | `/api/v1/withdrawals` | Criar saque PIX |
| `GET` | `/api/v1/withdrawals/{id}` | Consultar saque por ID |
| `GET` | `/api/v1/withdrawals/reference/{referenceId}` | Consultar saque por referência |
| `GET` | `/api/v1/merchants/me/balance` | Consultar saldo |
| `POST` | `/api/v1/webhooks` | Configurar webhook |
| `POST` | `/api/v1/webhooks/test` | Enviar webhook de teste |
| `GET` | `/api/v1/webhooks/events` | Listar entregas de webhook |
| `POST` | `/api/v1/webhooks/events/{id}/retry` | Reentregar webhook |
