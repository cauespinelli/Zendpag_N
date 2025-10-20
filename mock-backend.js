const http = require('http');
const crypto = require('crypto');

// Helper para ler body de requisições POST
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

// Gerar token JWT mock
function generateMockToken(email) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64');
  const exp = Math.floor(Date.now() / 1000) + (24 * 60 * 60); // 24 horas
  const payload = Buffer.from(JSON.stringify({
    sub: email,
    email: email,
    exp: exp,
    iat: Math.floor(Date.now() / 1000)
  })).toString('base64');
  const signature = crypto.createHash('sha256').update(header + '.' + payload).digest('base64');
  return `${header}.${payload}.${signature}`;
}

// Usuários mock
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
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Request-ID, X-Environment, X-Version');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  console.log(`${req.method} ${req.url}`);

  try {
    // Normalize URL - remove /api/v1 prefix if present
    const normalizedUrl = req.url.replace(/^\/api\/v1/, '');

    // AUTH ENDPOINTS
    if (normalizedUrl === '/auth/login' && req.method === 'POST') {
      const body = await parseBody(req);
      const { email, password } = body;

      const user = MOCK_USERS[email];

      if (!user || user.password !== password) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          message: 'E-mail ou senha inválidos',
          error: 'INVALID_CREDENTIALS'
        }));
        return;
      }

      const token = generateMockToken(email);
      const refreshToken = generateMockToken(email + ':refresh');

      const { password: _, ...userWithoutPassword } = user;

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        token,
        refreshToken,
        user: userWithoutPassword
      }));
      return;
    }

    if (normalizedUrl === '/auth/me' && req.method === 'GET') {
      const authHeader = req.headers.authorization;
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Token não fornecido' }));
        return;
      }

      // Mock: retornar admin user
      const { password: _, ...adminUser } = MOCK_USERS['admin@zendapag.com'];
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(adminUser));
      return;
    }

    if (normalizedUrl === '/auth/logout' && req.method === 'POST') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'Logout realizado com sucesso' }));
      return;
    }

    if (normalizedUrl === '/auth/refresh' && req.method === 'POST') {
      const body = await parseBody(req);
      const { refreshToken } = body;

      if (!refreshToken) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Refresh token inválido' }));
        return;
      }

      const token = generateMockToken('admin@zendapag.com');
      const newRefreshToken = generateMockToken('admin@zendapag.com:refresh');
      const { password: _, ...adminUser } = MOCK_USERS['admin@zendapag.com'];

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        token,
        refreshToken: newRefreshToken,
        user: adminUser
      }));
      return;
    }

    // ANALYTICS ENDPOINTS
    if (normalizedUrl === '/analytics/dashboard' || normalizedUrl.startsWith('/dashboard')) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        totalTransactions: 1234,
        totalAmount: 50000.00,
        successRate: 98.5,
        activeUsers: 456,
        dailyStats: [
          { date: '2025-10-01', transactions: 100, amount: 5000 },
          { date: '2025-10-02', transactions: 120, amount: 6000 },
          { date: '2025-10-03', transactions: 95, amount: 4500 },
          { date: '2025-10-04', transactions: 110, amount: 5500 },
          { date: '2025-10-05', transactions: 130, amount: 6500 },
          { date: '2025-10-06', transactions: 105, amount: 5200 },
          { date: '2025-10-07', transactions: 115, amount: 5800 },
        ]
      }));
      return;
    }

    // PAYMENTS ENDPOINTS
    if (normalizedUrl.startsWith('/payments')) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        content: [
          {
            id: '1',
            txid: 'TXN001',
            amount: 150.00,
            status: 'COMPLETED',
            createdAt: '2025-10-10T10:00:00',
            payerName: 'João Silva',
            payerDocument: '12345678900',
            description: 'Pagamento de produto'
          },
          {
            id: '2',
            txid: 'TXN002',
            amount: 250.00,
            status: 'PENDING',
            createdAt: '2025-10-10T11:00:00',
            payerName: 'Maria Santos',
            payerDocument: '98765432100',
            description: 'Pagamento de serviço'
          },
          {
            id: '3',
            txid: 'TXN003',
            amount: 350.00,
            status: 'COMPLETED',
            createdAt: '2025-10-10T12:00:00',
            payerName: 'Pedro Oliveira',
            payerDocument: '11122233344',
            description: 'Pagamento de assinatura'
          }
        ],
        totalElements: 3,
        totalPages: 1,
        page: 0,
        size: 20
      }));
      return;
    }

    // ANALYTICS ENDPOINTS (duplicate check removed)
    if (normalizedUrl.startsWith('/analytics') && normalizedUrl !== '/analytics/dashboard') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        revenue: { current: 45000, previous: 40000, growth: 12.5 },
        transactions: { current: 850, previous: 780, growth: 8.9 },
        avgTicket: { current: 52.94, previous: 51.28, growth: 3.2 },
        conversionRate: { current: 94.5, previous: 92.3, growth: 2.4 }
      }));
      return;
    }

    // MERCHANTS ENDPOINTS
    if (normalizedUrl === '/merchants/me') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        id: 'merchant-001',
        name: 'ZendaPag Admin',
        email: 'admin@zendapag.com',
        document: '12345678000190',
        balance: 15000.50,
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z'
      }));
      return;
    }

    if (normalizedUrl === '/merchants/me/balance') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        available: 15000.50,
        pending: 2500.00,
        total: 17500.50
      }));
      return;
    }

    // WEBHOOKS ENDPOINTS
    if (normalizedUrl.startsWith('/webhooks')) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        content: [
          {
            id: '1',
            url: 'https://example.com/webhook',
            events: ['payment.completed', 'payment.failed'],
            status: 'ACTIVE',
            createdAt: '2024-01-01T00:00:00Z'
          }
        ],
        totalElements: 1
      }));
      return;
    }

    // HEALTH CHECK
    if (normalizedUrl === '/actuator/health' || normalizedUrl === '/health') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'UP', service: 'mock-backend-with-auth' }));
      return;
    }

    // 404 para rotas não encontradas
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      error: 'Not found',
      path: normalizedUrl,
      originalPath: req.url,
      message: 'Endpoint não encontrado no mock backend'
    }));

  } catch (error) {
    console.error('Error:', error);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal server error', message: error.message }));
  }
});

const PORT = 8093;
server.listen(PORT, () => {
  console.log(`\n✅ Mock Backend Server rodando em http://localhost:${PORT}`);
  console.log(`📊 Pronto para servir o frontend ZendaPag\n`);
  console.log(`👤 Credenciais de login disponíveis:`);
  console.log(`   Admin:    admin@zendapag.com / admin123`);
  console.log(`   Merchant: merchant@example.com / merchant123\n`);
});
