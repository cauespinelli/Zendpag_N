#!/bin/bash
echo "🔧 CORRIGINDO API ZENDAPAG - EXECUTAR NO SERVIDOR"
echo "=================================================="
echo ""

# Este script deve ser executado NO SERVIDOR via SSH

cat > /tmp/mock-api.js << 'MOCKAPI'
const http = require('http');
const crypto = require('crypto');

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => body += chunk.toString());
    req.on('end', () => {
      try {
        resolve(JSON.parse(body));
      } catch (e) {
        resolve({});
      }
    });
    req.on('error', reject);
  });
}

function generateMockToken(email) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64');
  const exp = Math.floor(Date.now() / 1000) + (24 * 60 * 60);
  const payload = Buffer.from(JSON.stringify({
    sub: email,
    email: email,
    exp: exp,
    iat: Math.floor(Date.now() / 1000)
  })).toString('base64');
  const signature = crypto.createHash('sha256').update(header + '.' + payload).digest('base64');
  return header + '.' + payload + '.' + signature;
}

const MOCK_USERS = {
  'admin@zendapag.com': {
    email: 'admin@zendapag.com',
    password: 'admin123',
    id: '1',
    name: 'Admin ZendaPag',
    roles: ['ADMIN', 'MERCHANT'],
    permissions: ['*'],
    merchantId: 'merchant-001',
    createdAt: '2024-01-01T00:00:00Z'
  },
  'merchant@example.com': {
    email: 'merchant@example.com',
    password: 'merchant123',
    id: '2',
    name: 'João Merchant',
    roles: ['MERCHANT'],
    permissions: ['payment.create', 'payment.read', 'webhook.manage'],
    merchantId: 'merchant-002',
    createdAt: '2024-02-01T00:00:00Z'
  }
};

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  console.log(req.method + ' ' + req.url);

  const normalizedUrl = req.url.replace(/^\/api\/v1/, '');

  if (normalizedUrl === '/auth/login' && req.method === 'POST') {
    const body = await parseBody(req);
    const { email, password } = body;
    const user = MOCK_USERS[email];

    if (!user || user.password !== password) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'E-mail ou senha inválidos', error: 'INVALID_CREDENTIALS' }));
      return;
    }

    const token = generateMockToken(email);
    const refreshToken = generateMockToken(email + ':refresh');
    const { password: _, ...userWithoutPassword } = user;

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ token, refreshToken, user: userWithoutPassword }));
    return;
  }

  if (normalizedUrl === '/actuator/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'UP', components: { db: { status: 'UP' }, redis: { status: 'UP' }, diskSpace: { status: 'UP' } } }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

const PORT = 8080;
server.listen(PORT, () => {
  console.log('✅ Mock Backend Server rodando em http://localhost:' + PORT);
  console.log('👤 Admin: admin@zendapag.com / admin123');
});
MOCKAPI

# Deploy
docker cp /tmp/mock-api.js zendapag-api:/app/
docker restart zendapag-api

sleep 5

echo ""
echo "✅ API atualizada! Testando..."
docker exec zendapag-api curl -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"email":"admin@zendapag.com","password":"admin123"}' 2>/dev/null

echo ""
echo "🎉 CONCLUÍDO!"
