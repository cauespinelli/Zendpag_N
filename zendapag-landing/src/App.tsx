import React from 'react';
import './styles/design-system.css';
import './styles/landing.css';

const App: React.FC = () => {
  return (
    <div className="zp-app">
      {/* NAVIGATION */}
      <nav className="zp-navbar">
        <div className="container">
          <div className="zp-navbar-content">
            {/* Logo */}
            <div className="zp-logo">
              <svg width="160" height="40" viewBox="0 0 160 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <defs>
                  <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#6366F1"/>
                    <stop offset="100%" stopColor="#4F46E5"/>
                  </linearGradient>
                </defs>
                <rect x="2" y="2" width="36" height="36" rx="8" fill="url(#logo-grad)"/>
                <path d="M12,14 h12 l-8,8 h8 v2.5 h-12 l8,-8 h-8 z" fill="white"/>
                <circle cx="27" cy="16" r="1.5" fill="white"/>
                <circle cx="27" cy="24" r="1.5" fill="white"/>
                <text x="46" y="27" fontFamily="Space Grotesk, sans-serif" fontWeight="700" fontSize="18" letterSpacing="-0.02em" fill="#1E293B">
                  zendapag
                </text>
              </svg>
            </div>

            {/* Desktop Menu */}
            <ul className="zp-nav-links">
              <li><a href="#features">Recursos</a></li>
              <li><a href="#pricing">Preços</a></li>
              <li><a href="#integration">Integração</a></li>
              <li><a href="#docs">Documentação</a></li>
              <li>
                <a href="http://167.99.12.191:3005" className="zp-btn-nav">
                  Acessar Dashboard →
                </a>
              </li>
            </ul>
          </div>
        </div>
      </nav>

      {/* HERO SECTION */}
      <header className="zp-hero">
        <div className="container">
          <div className="zp-hero-content">
            <div className="zp-hero-badge">
              <span className="zp-badge-glow">✨ Nova Plataforma</span>
            </div>

            <h1 className="zp-hero-title">
              Pagamentos PIX
              <span className="zp-gradient-text"> Simplificados</span>
            </h1>

            <p className="zp-hero-subtitle">
              Plataforma completa para receber e gerenciar pagamentos PIX
              com segurança enterprise, API robusta e analytics em tempo real.
            </p>

            <div className="zp-hero-buttons">
              <a href="http://167.99.12.191:3005" className="zp-btn zp-btn-primary zp-btn-lg">
                Começar Gratuitamente
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </a>
              <a href="#features" className="zp-btn zp-btn-secondary zp-btn-lg">
                Ver Demonstração
              </a>
            </div>

            {/* Stats */}
            <div className="zp-hero-stats">
              <div className="zp-stat">
                <div className="zp-stat-value">99.9%</div>
                <div className="zp-stat-label">Uptime Garantido</div>
              </div>
              <div className="zp-stat">
                <div className="zp-stat-value">&lt;50ms</div>
                <div className="zp-stat-label">Latência Média</div>
              </div>
              <div className="zp-stat">
                <div className="zp-stat-value">24/7</div>
                <div className="zp-stat-label">Suporte Dedicado</div>
              </div>
            </div>
          </div>

          {/* Hero Visual */}
          <div className="zp-hero-visual">
            <div className="zp-dashboard-preview">
              <div className="zp-preview-card">
                <div className="zp-preview-header">
                  <span className="zp-preview-dot"></span>
                  <span className="zp-preview-dot"></span>
                  <span className="zp-preview-dot"></span>
                </div>
                <div className="zp-preview-content">
                  {/* Simplified dashboard preview */}
                  <div className="zp-preview-stat-row">
                    <div className="zp-preview-stat-card">
                      <span className="zp-preview-label">Receita Hoje</span>
                      <span className="zp-preview-value">R$ 12.450,00</span>
                      <span className="zp-preview-trend">↗ +23%</span>
                    </div>
                    <div className="zp-preview-stat-card">
                      <span className="zp-preview-label">Transações</span>
                      <span className="zp-preview-value">1.248</span>
                      <span className="zp-preview-trend">↗ +18%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* FEATURES SECTION */}
      <section id="features" className="zp-section zp-section-features">
        <div className="container">
          <div className="zp-section-header">
            <span className="zp-section-badge">Recursos</span>
            <h2 className="zp-section-title">Tudo que você precisa para crescer</h2>
            <p className="zp-section-subtitle">
              Ferramentas enterprise-grade para processar pagamentos PIX com segurança e eficiência
            </p>
          </div>

          <div className="zp-features-grid">
            {/* Feature 1 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-primary">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <h3 className="zp-feature-title">Processamento Instantâneo</h3>
              <p className="zp-feature-description">
                Pagamentos processados em tempo real com confirmação imediata e webhook instantâneo.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-success">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <h3 className="zp-feature-title">Segurança Enterprise</h3>
              <p className="zp-feature-description">
                Criptografia ponta-a-ponta, conformidade PCI DSS e proteção contra fraudes.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <h3 className="zp-feature-title">Analytics Avançado</h3>
              <p className="zp-feature-description">
                Dashboards em tempo real com insights de vendas, conversão e performance.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-primary">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                </svg>
              </div>
              <h3 className="zp-feature-title">API RESTful Completa</h3>
              <p className="zp-feature-description">
                Documentação detalhada, SDKs oficiais e integração em minutos.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-success">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
              <h3 className="zp-feature-title">Webhooks Confiáveis</h3>
              <p className="zp-feature-description">
                Notificações automáticas com retry inteligente e logs detalhados.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="zp-feature-card">
              <div className="zp-feature-icon zp-icon-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 className="zp-feature-title">Reconciliação Automática</h3>
              <p className="zp-feature-description">
                Conciliação bancária automática e relatórios financeiros completos.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA SECTION */}
      <section className="zp-section zp-section-cta">
        <div className="container">
          <div className="zp-cta-card">
            <div className="zp-cta-content">
              <h2 className="zp-cta-title">Pronto para começar?</h2>
              <p className="zp-cta-subtitle">
                Junte-se a centenas de empresas que processam pagamentos PIX com Zendapag
              </p>
              <a href="http://167.99.12.191:3005" className="zp-btn zp-btn-primary zp-btn-xl">
                Criar Conta Gratuitamente →
              </a>
              <p className="zp-cta-note">Sem cartão de crédito • Setup em 5 minutos</p>
            </div>
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="zp-footer">
        <div className="container">
          <div className="zp-footer-content">
            <div className="zp-footer-brand">
              <div className="zp-footer-logo">
                <svg width="140" height="36" viewBox="0 0 140 36" fill="none">
                  <rect x="2" y="2" width="32" height="32" rx="6" fill="#6366F1" fillOpacity="0.15"/>
                  <rect x="2" y="2" width="32" height="32" rx="6" stroke="#6366F1" strokeWidth="2"/>
                  <path d="M11,13 h10 l-7,7 h7 v2 h-10 l7,-7 h-7 z" fill="#6366F1"/>
                  <circle cx="24" cy="14" r="1.5" fill="#6366F1"/>
                  <circle cx="24" cy="22" r="1.5" fill="#6366F1"/>
                  <text x="42" y="24" fontFamily="Space Grotesk" fontWeight="700" fontSize="16" fill="white">
                    zendapag
                  </text>
                </svg>
              </div>
              <p className="zp-footer-tagline">
                Plataforma moderna de pagamentos PIX para empresas que buscam crescimento.
              </p>
            </div>

            <div className="zp-footer-links">
              <div className="zp-footer-column">
                <h4>Produto</h4>
                <ul>
                  <li><a href="#features">Recursos</a></li>
                  <li><a href="#pricing">Preços</a></li>
                  <li><a href="http://167.99.12.191:3005">Dashboard</a></li>
                </ul>
              </div>

              <div className="zp-footer-column">
                <h4>Desenvolvedores</h4>
                <ul>
                  <li><a href="http://167.99.12.191:8093/api">API</a></li>
                  <li><a href="#docs">Documentação</a></li>
                  <li><a href="https://github.com/klebergobbi/zendapag">GitHub</a></li>
                </ul>
              </div>

              <div className="zp-footer-column">
                <h4>Empresa</h4>
                <ul>
                  <li><a href="#about">Sobre</a></li>
                  <li><a href="#contact">Contato</a></li>
                  <li><a href="#privacy">Privacidade</a></li>
                </ul>
              </div>
            </div>
          </div>

          <div className="zp-footer-bottom">
            <p>&copy; 2025 Zendapag. Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
