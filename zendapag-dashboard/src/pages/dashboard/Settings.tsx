import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  User,
  Building,
  Bell,
  Shield,
  CreditCard,
  Webhook,
  Save,
  Check,
  AlertCircle
} from 'lucide-react';

type SettingsTab = 'profile' | 'company' | 'notifications' | 'security' | 'billing' | 'webhooks';

export default function Settings() {
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile');
  const [saved, setSaved] = useState(false);

  // Profile Data
  const [profileData, setProfileData] = useState({
    name: 'Kleber Gobbi',
    email: 'kleber@zendpag.com',
    phone: '(11) 98765-4321',
    avatar: ''
  });

  // Company Data
  const [companyData, setCompanyData] = useState({
    companyName: 'KLEBER CAVALCANTI GOBBI DESENVOLVIMENTO',
    cnpj: '12.345.678/0001-90',
    address: 'Rua Exemplo, 123',
    city: 'Jundiaí',
    state: 'SP',
    zipCode: '13200-000'
  });

  // Notification Settings
  const [notifications, setNotifications] = useState({
    emailNewPayment: true,
    emailFailedPayment: true,
    emailRefund: true,
    emailWeeklySummary: true,
    pushNewPayment: false,
    pushFailedPayment: true,
    smsHighValue: false
  });

  // Security Settings
  const [security, setSecurity] = useState({
    twoFactorEnabled: false,
    sessionTimeout: '30',
    ipWhitelist: ''
  });

  // Webhook Settings
  const [webhooks, setWebhooks] = useState([
    {
      id: '1',
      url: 'https://api.example.com/webhooks/zendpag',
      events: ['payment.created', 'payment.completed', 'payment.failed'],
      active: true
    }
  ]);

  const handleSave = () => {
    // Simular salvamento
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  const tabs = [
    { id: 'profile' as SettingsTab, label: 'Perfil', icon: User },
    { id: 'company' as SettingsTab, label: 'Empresa', icon: Building },
    { id: 'notifications' as SettingsTab, label: 'Notificações', icon: Bell },
    { id: 'security' as SettingsTab, label: 'Segurança', icon: Shield },
    { id: 'billing' as SettingsTab, label: 'Faturamento', icon: CreditCard },
    { id: 'webhooks' as SettingsTab, label: 'Webhooks', icon: Webhook }
  ];

  return (
    <div className="settings-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Configurações</h1>
          <p className="page-subtitle">
            Gerencie suas preferências e configurações da conta
          </p>
        </div>
      </div>

      <div className="settings-container">
        {/* Sidebar Tabs */}
        <aside className="settings-sidebar">
          <nav className="settings-nav">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  className={`settings-nav-item ${activeTab === tab.id ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.id)}
                >
                  <Icon size={20} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>
        </aside>

        {/* Content */}
        <div className="settings-content">
          {/* Profile Tab */}
          {activeTab === 'profile' && (
            <motion.div
              key="profile"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Informações do Perfil</h2>

              <div className="settings-form">
                <div className="form-group">
                  <label className="form-label">Nome Completo</label>
                  <input
                    type="text"
                    className="form-input"
                    value={profileData.name}
                    onChange={(e) => setProfileData({...profileData, name: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    className="form-input"
                    value={profileData.email}
                    onChange={(e) => setProfileData({...profileData, email: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Telefone</label>
                  <input
                    type="tel"
                    className="form-input"
                    value={profileData.phone}
                    onChange={(e) => setProfileData({...profileData, phone: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Foto de Perfil</label>
                  <div className="avatar-upload">
                    <div className="avatar-preview">
                      {profileData.avatar ? (
                        <img src={profileData.avatar} alt="Avatar" />
                      ) : (
                        <div className="avatar-placeholder">
                          {profileData.name.charAt(0)}
                        </div>
                      )}
                    </div>
                    <button className="btn btn-secondary btn-sm">
                      Alterar Foto
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* Company Tab */}
          {activeTab === 'company' && (
            <motion.div
              key="company"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Dados da Empresa</h2>

              <div className="settings-form">
                <div className="form-group">
                  <label className="form-label">Razão Social</label>
                  <input
                    type="text"
                    className="form-input"
                    value={companyData.companyName}
                    onChange={(e) => setCompanyData({...companyData, companyName: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">CNPJ</label>
                  <input
                    type="text"
                    className="form-input"
                    value={companyData.cnpj}
                    onChange={(e) => setCompanyData({...companyData, cnpj: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Endereço</label>
                  <input
                    type="text"
                    className="form-input"
                    value={companyData.address}
                    onChange={(e) => setCompanyData({...companyData, address: e.target.value})}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Cidade</label>
                    <input
                      type="text"
                      className="form-input"
                      value={companyData.city}
                      onChange={(e) => setCompanyData({...companyData, city: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Estado</label>
                    <select
                      className="form-select"
                      value={companyData.state}
                      onChange={(e) => setCompanyData({...companyData, state: e.target.value})}
                    >
                      <option value="SP">São Paulo</option>
                      <option value="RJ">Rio de Janeiro</option>
                      <option value="MG">Minas Gerais</option>
                      {/* Adicionar outros estados */}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">CEP</label>
                    <input
                      type="text"
                      className="form-input"
                      value={companyData.zipCode}
                      onChange={(e) => setCompanyData({...companyData, zipCode: e.target.value})}
                    />
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <motion.div
              key="notifications"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Preferências de Notificação</h2>

              <div className="notification-groups">
                <div className="notification-group">
                  <h3 className="notification-group-title">Email</h3>
                  <div className="notification-items">
                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Novo Pagamento</span>
                        <span className="notification-description">
                          Receba um email quando um novo pagamento for recebido
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.emailNewPayment}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            emailNewPayment: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>

                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Pagamento Falhou</span>
                        <span className="notification-description">
                          Seja notificado quando um pagamento falhar
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.emailFailedPayment}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            emailFailedPayment: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>

                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Reembolso Processado</span>
                        <span className="notification-description">
                          Receba notificação sobre reembolsos
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.emailRefund}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            emailRefund: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>

                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Resumo Semanal</span>
                        <span className="notification-description">
                          Relatório semanal de transações
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.emailWeeklySummary}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            emailWeeklySummary: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="notification-group">
                  <h3 className="notification-group-title">Push</h3>
                  <div className="notification-items">
                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Novo Pagamento</span>
                        <span className="notification-description">
                          Notificações push para novos pagamentos
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.pushNewPayment}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            pushNewPayment: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>

                    <div className="notification-item">
                      <div className="notification-info">
                        <span className="notification-label">Falhas de Pagamento</span>
                        <span className="notification-description">
                          Alertas instantâneos de falhas
                        </span>
                      </div>
                      <label className="toggle-switch">
                        <input
                          type="checkbox"
                          checked={notifications.pushFailedPayment}
                          onChange={(e) => setNotifications({
                            ...notifications,
                            pushFailedPayment: e.target.checked
                          })}
                        />
                        <span className="toggle-slider"></span>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* Security Tab */}
          {activeTab === 'security' && (
            <motion.div
              key="security"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Segurança</h2>

              <div className="security-card">
                <div className="security-card-header">
                  <Shield size={24} />
                  <div>
                    <h3>Autenticação de Dois Fatores</h3>
                    <p>Adicione uma camada extra de segurança à sua conta</p>
                  </div>
                </div>
                <label className="toggle-switch">
                  <input
                    type="checkbox"
                    checked={security.twoFactorEnabled}
                    onChange={(e) => setSecurity({
                      ...security,
                      twoFactorEnabled: e.target.checked
                    })}
                  />
                  <span className="toggle-slider"></span>
                </label>
              </div>

              <div className="settings-form">
                <div className="form-group">
                  <label className="form-label">Timeout de Sessão</label>
                  <select
                    className="form-select"
                    value={security.sessionTimeout}
                    onChange={(e) => setSecurity({
                      ...security,
                      sessionTimeout: e.target.value
                    })}
                  >
                    <option value="15">15 minutos</option>
                    <option value="30">30 minutos</option>
                    <option value="60">1 hora</option>
                    <option value="240">4 horas</option>
                  </select>
                  <span className="form-hint">
                    Tempo de inatividade antes de fazer logout automático
                  </span>
                </div>

                <div className="form-group">
                  <label className="form-label">Alterar Senha</label>
                  <button className="btn btn-secondary">
                    Alterar Senha
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {/* Billing Tab */}
          {activeTab === 'billing' && (
            <motion.div
              key="billing"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Faturamento</h2>

              <div className="billing-plan-card">
                <div className="plan-header">
                  <div>
                    <h3>Plano Pro</h3>
                    <p>R$ 99/mês</p>
                  </div>
                  <span className="plan-badge">Ativo</span>
                </div>
                <div className="plan-features">
                  <div className="plan-feature">
                    <Check size={16} />
                    <span>Transações ilimitadas</span>
                  </div>
                  <div className="plan-feature">
                    <Check size={16} />
                    <span>API completa</span>
                  </div>
                  <div className="plan-feature">
                    <Check size={16} />
                    <span>Suporte prioritário</span>
                  </div>
                </div>
                <button className="btn btn-secondary">
                  Alterar Plano
                </button>
              </div>

              <div className="billing-info">
                <h3>Próxima Cobrança</h3>
                <p className="billing-next-date">15 de Dezembro, 2024</p>
                <p className="billing-amount">R$ 99,00</p>
              </div>
            </motion.div>
          )}

          {/* Webhooks Tab */}
          {activeTab === 'webhooks' && (
            <motion.div
              key="webhooks"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="settings-section"
            >
              <h2 className="settings-section-title">Webhooks</h2>
              <p className="section-description">
                Configure endpoints para receber notificações em tempo real sobre eventos
              </p>

              <div className="webhooks-list">
                {webhooks.map((webhook) => (
                  <div key={webhook.id} className="webhook-card">
                    <div className="webhook-header">
                      <code className="webhook-url font-mono">{webhook.url}</code>
                      <span className={`webhook-status ${webhook.active ? 'active' : 'inactive'}`}>
                        {webhook.active ? 'Ativo' : 'Inativo'}
                      </span>
                    </div>
                    <div className="webhook-events">
                      {webhook.events.map((event) => (
                        <span key={event} className="webhook-event">
                          {event}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>

              <button className="btn btn-secondary">
                <Webhook size={18} />
                Adicionar Webhook
              </button>
            </motion.div>
          )}

          {/* Save Button */}
          <div className="settings-footer">
            <button
              className={`btn btn-primary btn-lg ${saved ? 'success' : ''}`}
              onClick={handleSave}
            >
              {saved ? (
                <>
                  <Check size={20} />
                  Salvo com Sucesso!
                </>
              ) : (
                <>
                  <Save size={20} />
                  Salvar Alterações
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
