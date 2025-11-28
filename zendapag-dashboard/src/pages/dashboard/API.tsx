import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Code,
  Copy,
  Check,
  Eye,
  EyeOff,
  Plus,
  Trash2,
  Key,
  Shield,
  Book,
  ExternalLink
} from 'lucide-react';

interface APIKey {
  id: string;
  name: string;
  key: string;
  createdAt: string;
  lastUsed: string;
  requests: number;
}

export default function APIPage() {
  const [apiKeys, setApiKeys] = useState<APIKey[]>([
    {
      id: '1',
      name: 'Produção',
      key: 'zp_live_sk_abc123def456ghi789jkl012mno345pqr',
      createdAt: '2024-01-15',
      lastUsed: '2024-11-12 14:23',
      requests: 45820
    },
    {
      id: '2',
      name: 'Desenvolvimento',
      key: 'zp_test_sk_xyz789abc456def123ghi456jkl789mno',
      createdAt: '2024-01-10',
      lastUsed: '2024-11-12 10:15',
      requests: 12340
    }
  ]);

  const [showNewKeyModal, setShowNewKeyModal] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const toggleKeyVisibility = (keyId: string) => {
    setVisibleKeys(prev => {
      const newSet = new Set(prev);
      if (newSet.has(keyId)) {
        newSet.delete(keyId);
      } else {
        newSet.add(keyId);
      }
      return newSet;
    });
  };

  const copyKey = (key: string, keyId: string) => {
    navigator.clipboard.writeText(key);
    setCopiedKey(keyId);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  const createNewKey = () => {
    if (!newKeyName.trim()) return;

    const newKey: APIKey = {
      id: Date.now().toString(),
      name: newKeyName,
      key: `zp_live_sk_${Math.random().toString(36).substr(2, 36)}`,
      createdAt: new Date().toISOString().split('T')[0],
      lastUsed: 'Nunca',
      requests: 0
    };

    setApiKeys([...apiKeys, newKey]);
    setNewKeyName('');
    setShowNewKeyModal(false);
  };

  const deleteKey = (keyId: string) => {
    if (confirm('Tem certeza que deseja excluir esta chave?')) {
      setApiKeys(apiKeys.filter(k => k.id !== keyId));
    }
  };

  const codeExamples = {
    curl: `curl https://api.zendpag.com/v1/payments \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 10000,
    "currency": "BRL",
    "payment_method": "pix",
    "customer": {
      "name": "João Silva",
      "email": "joao@example.com"
    }
  }'`,
    javascript: `const zendpag = require('zendpag-node');
const client = new zendpag('YOUR_API_KEY');

const payment = await client.payments.create({
  amount: 10000,
  currency: 'BRL',
  payment_method: 'pix',
  customer: {
    name: 'João Silva',
    email: 'joao@example.com'
  }
});

console.log(payment);`,
    python: `import zendpag

client = zendpag.Client('YOUR_API_KEY')

payment = client.payments.create(
    amount=10000,
    currency='BRL',
    payment_method='pix',
    customer={
        'name': 'João Silva',
        'email': 'joao@example.com'
    }
)

print(payment)`,
    php: `<?php
require 'vendor/autoload.php';

$zendpag = new \\Zendpag\\Client('YOUR_API_KEY');

$payment = $zendpag->payments->create([
    'amount' => 10000,
    'currency' => 'BRL',
    'payment_method' => 'pix',
    'customer' => [
        'name' => 'João Silva',
        'email' => 'joao@example.com'
    ]
]);

print_r($payment);
?>`
  };

  const [selectedLanguage, setSelectedLanguage] = useState<keyof typeof codeExamples>('curl');

  const endpoints = [
    {
      method: 'POST',
      path: '/v1/payments',
      description: 'Criar um novo pagamento',
      auth: true
    },
    {
      method: 'GET',
      path: '/v1/payments/:id',
      description: 'Obter detalhes de um pagamento',
      auth: true
    },
    {
      method: 'GET',
      path: '/v1/payments',
      description: 'Listar todos os pagamentos',
      auth: true
    },
    {
      method: 'POST',
      path: '/v1/refunds',
      description: 'Criar um reembolso',
      auth: true
    },
    {
      method: 'GET',
      path: '/v1/customers',
      description: 'Listar clientes',
      auth: true
    },
    {
      method: 'POST',
      path: '/v1/webhooks',
      description: 'Configurar webhooks',
      auth: true
    }
  ];

  return (
    <div className="api-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">API & Documentação</h1>
          <p className="page-subtitle">
            Gerencie suas chaves de API e acesse a documentação
          </p>
        </div>

        <div className="page-actions">
          <a
            href="https://docs.zendpag.com"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
          >
            <Book size={18} />
            Documentação Completa
            <ExternalLink size={16} />
          </a>
          <button
            className="btn btn-primary"
            onClick={() => setShowNewKeyModal(true)}
          >
            <Plus size={18} />
            Nova Chave API
          </button>
        </div>
      </div>

      {/* API Keys Section */}
      <motion.div
        className="api-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div className="section-header">
          <div>
            <h2 className="section-title">Chaves de API</h2>
            <p className="section-description">
              Gerencie suas chaves para autenticação na API
            </p>
          </div>
        </div>

        <div className="api-keys-list">
          {apiKeys.map((apiKey, index) => (
            <motion.div
              key={apiKey.id}
              className="api-key-card"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.1 }}
            >
              <div className="api-key-header">
                <div className="api-key-info">
                  <div className="api-key-icon">
                    <Key size={20} />
                  </div>
                  <div>
                    <h3 className="api-key-name">{apiKey.name}</h3>
                    <span className="api-key-date">
                      Criada em {new Date(apiKey.createdAt).toLocaleDateString('pt-BR')}
                    </span>
                  </div>
                </div>
                <button
                  className="btn-icon-danger"
                  onClick={() => deleteKey(apiKey.id)}
                  title="Excluir chave"
                >
                  <Trash2 size={18} />
                </button>
              </div>

              <div className="api-key-body">
                <div className="api-key-value">
                  <code className="font-mono">
                    {visibleKeys.has(apiKey.id)
                      ? apiKey.key
                      : apiKey.key.replace(/./g, '•')}
                  </code>
                  <div className="api-key-actions">
                    <button
                      className="btn-icon"
                      onClick={() => toggleKeyVisibility(apiKey.id)}
                      title={visibleKeys.has(apiKey.id) ? 'Ocultar' : 'Mostrar'}
                    >
                      {visibleKeys.has(apiKey.id) ? (
                        <EyeOff size={18} />
                      ) : (
                        <Eye size={18} />
                      )}
                    </button>
                    <button
                      className={`btn-icon ${copiedKey === apiKey.id ? 'success' : ''}`}
                      onClick={() => copyKey(apiKey.key, apiKey.id)}
                      title="Copiar"
                    >
                      {copiedKey === apiKey.id ? (
                        <Check size={18} />
                      ) : (
                        <Copy size={18} />
                      )}
                    </button>
                  </div>
                </div>

                <div className="api-key-stats">
                  <div className="api-key-stat">
                    <span className="stat-label">Último uso:</span>
                    <span className="stat-value">{apiKey.lastUsed}</span>
                  </div>
                  <div className="api-key-stat">
                    <span className="stat-label">Requisições:</span>
                    <span className="stat-value font-mono">
                      {apiKey.requests.toLocaleString('pt-BR')}
                    </span>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* Quick Start Guide */}
      <motion.div
        className="api-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <h2 className="section-title">Início Rápido</h2>

        {/* Language Selector */}
        <div className="language-selector">
          {Object.keys(codeExamples).map((lang) => (
            <button
              key={lang}
              className={`language-btn ${selectedLanguage === lang ? 'active' : ''}`}
              onClick={() => setSelectedLanguage(lang as keyof typeof codeExamples)}
            >
              {lang.toUpperCase()}
            </button>
          ))}
        </div>

        {/* Code Example */}
        <div className="code-example">
          <div className="code-header">
            <span className="code-title">Criar um Pagamento</span>
            <button
              className="btn-icon"
              onClick={() => {
                navigator.clipboard.writeText(codeExamples[selectedLanguage]);
              }}
            >
              <Copy size={18} />
            </button>
          </div>
          <pre className="code-content">
            <code>{codeExamples[selectedLanguage]}</code>
          </pre>
        </div>
      </motion.div>

      {/* Endpoints List */}
      <motion.div
        className="api-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
      >
        <h2 className="section-title">Endpoints Principais</h2>

        <div className="endpoints-list">
          {endpoints.map((endpoint, index) => (
            <motion.div
              key={`${endpoint.method}-${endpoint.path}`}
              className="endpoint-card"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.6 + (index * 0.05) }}
            >
              <div className="endpoint-header">
                <span className={`http-method ${endpoint.method.toLowerCase()}`}>
                  {endpoint.method}
                </span>
                <code className="endpoint-path font-mono">{endpoint.path}</code>
                {endpoint.auth && (
                  <span className="endpoint-auth" title="Requer autenticação">
                    <Shield size={14} />
                  </span>
                )}
              </div>
              <p className="endpoint-description">{endpoint.description}</p>
            </motion.div>
          ))}
        </div>

        <div className="api-docs-link">
          <p>Para mais detalhes, acesse a documentação completa</p>
          <a
            href="https://docs.zendpag.com"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
          >
            <Book size={18} />
            Ver Documentação
            <ExternalLink size={16} />
          </a>
        </div>
      </motion.div>

      {/* New API Key Modal */}
      {showNewKeyModal && (
        <div className="modal-overlay" onClick={() => setShowNewKeyModal(false)}>
          <motion.div
            className="modal"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <h3>Nova Chave de API</h3>
              <button
                className="modal-close"
                onClick={() => setShowNewKeyModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Nome da Chave</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Ex: Produção, Desenvolvimento, Teste"
                  value={newKeyName}
                  onChange={(e) => setNewKeyName(e.target.value)}
                  autoFocus
                />
                <span className="form-hint">
                  Escolha um nome descritivo para identificar esta chave
                </span>
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => setShowNewKeyModal(false)}
              >
                Cancelar
              </button>
              <button
                className="btn btn-primary"
                onClick={createNewKey}
                disabled={!newKeyName.trim()}
              >
                <Plus size={18} />
                Criar Chave
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
