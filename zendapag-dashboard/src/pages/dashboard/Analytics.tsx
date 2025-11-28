import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Users,
  CreditCard,
  Calendar,
  Download,
  Filter
} from 'lucide-react';
import Chart from '../../components/dashboard/Chart';
import MetricCard from '../../components/dashboard/MetricCard';

type TimeRange = '7d' | '30d' | '90d' | '1y';

export default function Analytics() {
  const [timeRange, setTimeRange] = useState<TimeRange>('30d');

  const metrics = [
    {
      title: 'Receita Total',
      value: 'R$ 284.320',
      change: 28.4,
      icon: DollarSign,
      color: 'success' as const
    },
    {
      title: 'Ticket Médio',
      value: 'R$ 342,15',
      change: -3.2,
      icon: TrendingUp,
      color: 'primary' as const
    },
    {
      title: 'Taxa de Conversão',
      value: '68.4%',
      change: 12.5,
      icon: TrendingUp,
      color: 'cyan' as const
    },
    {
      title: 'Clientes Ativos',
      value: '2.847',
      change: 15.8,
      icon: Users,
      color: 'warning' as const
    }
  ];

  const paymentMethodsData = [
    { method: 'PIX', transactions: 4520, amount: 127800, percentage: 45 },
    { method: 'Cartão de Crédito', transactions: 3215, amount: 85400, percentage: 30 },
    { method: 'Cartão de Débito', transactions: 1840, amount: 42650, percentage: 15 },
    { method: 'Boleto', transactions: 925, amount: 28470, percentage: 10 }
  ];

  const topProducts = [
    { name: 'Curso Avançado React', sales: 847, revenue: 420935 },
    { name: 'Mentoria Individual', sales: 432, revenue: 129240 },
    { name: 'Ebook JavaScript', sales: 1243, revenue: 62150 },
    { name: 'Workshop Node.js', sales: 284, revenue: 56800 },
    { name: 'Template Premium', sales: 625, revenue: 43750 }
  ];

  const conversionFunnel = [
    { stage: 'Visitantes', count: 15420, percentage: 100 },
    { stage: 'Checkout Iniciado', count: 8842, percentage: 57.3 },
    { stage: 'Informações Preenchidas', count: 6234, percentage: 40.4 },
    { stage: 'Pagamento Processado', count: 4521, percentage: 29.3 },
    { stage: 'Compra Finalizada', count: 4012, percentage: 26.0 }
  ];

  return (
    <div className="analytics-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Analytics</h1>
          <p className="page-subtitle">
            Análise detalhada de métricas e performance
          </p>
        </div>

        <div className="page-actions">
          {/* Time Range Selector */}
          <div className="time-range-selector">
            <button
              className={`time-range-btn ${timeRange === '7d' ? 'active' : ''}`}
              onClick={() => setTimeRange('7d')}
            >
              7 dias
            </button>
            <button
              className={`time-range-btn ${timeRange === '30d' ? 'active' : ''}`}
              onClick={() => setTimeRange('30d')}
            >
              30 dias
            </button>
            <button
              className={`time-range-btn ${timeRange === '90d' ? 'active' : ''}`}
              onClick={() => setTimeRange('90d')}
            >
              90 dias
            </button>
            <button
              className={`time-range-btn ${timeRange === '1y' ? 'active' : ''}`}
              onClick={() => setTimeRange('1y')}
            >
              1 ano
            </button>
          </div>

          <button className="btn btn-secondary">
            <Download size={18} />
            Exportar
          </button>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="metrics-grid">
        {metrics.map((metric, index) => (
          <motion.div
            key={metric.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <MetricCard {...metric} />
          </motion.div>
        ))}
      </div>

      {/* Charts Section */}
      <div className="analytics-charts">
        {/* Revenue Chart */}
        <motion.div
          className="analytics-chart-card full-width"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <div className="chart-header">
            <div>
              <h3 className="chart-title">Receita ao Longo do Tempo</h3>
              <p className="chart-subtitle">Evolução de vendas no período</p>
            </div>
            <select className="chart-select">
              <option>Receita</option>
              <option>Transações</option>
              <option>Ticket Médio</option>
            </select>
          </div>
          <div className="chart-content">
            <Chart type="line" />
          </div>
        </motion.div>

        {/* Payment Methods & Transactions */}
        <div className="analytics-charts-row">
          <motion.div
            className="analytics-chart-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
          >
            <div className="chart-header">
              <h3 className="chart-title">Métodos de Pagamento</h3>
            </div>
            <div className="chart-content">
              <Chart type="doughnut" />
            </div>
          </motion.div>

          <motion.div
            className="analytics-chart-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6 }}
          >
            <div className="chart-header">
              <h3 className="chart-title">Transações por Dia</h3>
            </div>
            <div className="chart-content">
              <Chart type="bar" />
            </div>
          </motion.div>
        </div>
      </div>

      {/* Payment Methods Table */}
      <motion.div
        className="analytics-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7 }}
      >
        <h2 className="section-title">Performance por Método de Pagamento</h2>
        <div className="payment-methods-table">
          <table className="data-table">
            <thead>
              <tr>
                <th>Método</th>
                <th>Transações</th>
                <th>Valor Total</th>
                <th>Participação</th>
                <th>Tendência</th>
              </tr>
            </thead>
            <tbody>
              {paymentMethodsData.map((item, index) => (
                <motion.tr
                  key={item.method}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.8 + (index * 0.1) }}
                >
                  <td>
                    <div className="method-cell">
                      <CreditCard size={18} />
                      <span className="method-name">{item.method}</span>
                    </div>
                  </td>
                  <td className="font-mono">{item.transactions.toLocaleString('pt-BR')}</td>
                  <td className="font-mono">
                    R$ {item.amount.toLocaleString('pt-BR', {
                      minimumFractionDigits: 2
                    })}
                  </td>
                  <td>
                    <div className="percentage-cell">
                      <div className="percentage-bar">
                        <div
                          className="percentage-fill"
                          style={{ width: `${item.percentage}%` }}
                        />
                      </div>
                      <span className="percentage-text">{item.percentage}%</span>
                    </div>
                  </td>
                  <td>
                    <div className="trend-cell positive">
                      <TrendingUp size={16} />
                      <span>+{Math.floor(Math.random() * 20)}%</span>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Top Products */}
      <motion.div
        className="analytics-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.0 }}
      >
        <h2 className="section-title">Produtos Mais Vendidos</h2>
        <div className="top-products-grid">
          {topProducts.map((product, index) => (
            <div key={product.name} className="product-card">
              <div className="product-rank">#{index + 1}</div>
              <div className="product-info">
                <h4 className="product-name">{product.name}</h4>
                <div className="product-stats">
                  <span className="product-sales">{product.sales} vendas</span>
                  <span className="product-revenue font-mono">
                    R$ {product.revenue.toLocaleString('pt-BR')}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* Conversion Funnel */}
      <motion.div
        className="analytics-section"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.2 }}
      >
        <h2 className="section-title">Funil de Conversão</h2>
        <div className="conversion-funnel">
          {conversionFunnel.map((stage, index) => (
            <div key={stage.stage} className="funnel-stage">
              <div className="funnel-stage-header">
                <span className="funnel-stage-name">{stage.stage}</span>
                <span className="funnel-stage-count font-mono">
                  {stage.count.toLocaleString('pt-BR')}
                </span>
              </div>
              <div className="funnel-stage-bar">
                <div
                  className="funnel-stage-fill"
                  style={{ width: `${stage.percentage}%` }}
                >
                  <span className="funnel-stage-percentage">
                    {stage.percentage}%
                  </span>
                </div>
              </div>
              {index < conversionFunnel.length - 1 && (
                <div className="funnel-arrow">↓</div>
              )}
            </div>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
