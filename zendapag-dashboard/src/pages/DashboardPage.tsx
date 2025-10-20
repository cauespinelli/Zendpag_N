// @ts-nocheck
import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Space, Typography, Spin, Alert } from 'antd';
import {
  DollarOutlined,
  TransactionOutlined,
  RiseOutlined,
  FallOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
} from '@ant-design/icons';
import { Line, Column } from '@ant-design/plots';
import apiService from '@/services/api';
import { API_ENDPOINTS, PAYMENT_STATUS_COLORS, PAYMENT_STATUS_LABELS } from '@/utils/constants';
import { formatCurrency, formatDate } from '@/utils/helpers';
import type { DashboardStats, Payment } from '@/types';

const { Title, Text } = Typography;

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.get<DashboardStats>(API_ENDPOINTS.ANALYTICS.DASHBOARD);
      setStats(data);
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar dados do dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <div style={{ marginTop: 20 }}>Carregando dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        message="Erro ao carregar dashboard"
        description={error}
        type="error"
        showIcon
        style={{ marginBottom: 20 }}
      />
    );
  }

  if (!stats) return null;

  // Chart configurations
  const lineChartConfig = {
    data: stats.dailyStats || [],
    xField: 'date',
    yField: 'amount',
    seriesField: 'type',
    xAxis: {
      type: 'time',
    },
    yAxis: {
      label: {
        formatter: (v: string) => `R$ ${parseFloat(v).toFixed(2)}`,
      },
    },
    smooth: true,
    animation: {
      appear: {
        animation: 'path-in',
        duration: 1000,
      },
    },
  };

  const columnChartConfig = {
    data: stats.dailyStats || [],
    xField: 'date',
    yField: 'transactions',
    label: {
      position: 'top',
      style: {
        fill: '#000000',
        opacity: 0.6,
      },
    },
    xAxis: {
      label: {
        autoHide: true,
        autoRotate: false,
      },
    },
    meta: {
      date: {
        alias: 'Data',
      },
      transactions: {
        alias: 'Transações',
      },
    },
  };

  const recentPaymentsColumns = [
    {
      title: 'ID',
      dataIndex: 'txid',
      key: 'txid',
      width: 120,
      render: (txid: string) => <Text copyable>{txid}</Text>,
    },
    {
      title: 'Cliente',
      dataIndex: 'payerName',
      key: 'payerName',
      render: (name: string) => name || '-',
    },
    {
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number) => formatCurrency(amount),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={PAYMENT_STATUS_COLORS[status]}>
          {PAYMENT_STATUS_LABELS[status]}
        </Tag>
      ),
    },
    {
      title: 'Data',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => formatDate(date),
    },
  ];

  return (
    <div>
      <Title level={2}>Dashboard</Title>
      <Text type="secondary">Visão geral das suas transações PIX</Text>

      {/* Main Statistics */}
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total de Transações"
              value={stats.totalTransactions}
              prefix={<TransactionOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
            {stats.monthlyGrowth?.transactions !== undefined && (
              <Text
                type={stats.monthlyGrowth.transactions >= 0 ? 'success' : 'danger'}
                style={{ fontSize: 14, marginTop: 8, display: 'block' }}
              >
                {stats.monthlyGrowth.transactions >= 0 ? <RiseOutlined /> : <FallOutlined />}
                {Math.abs(stats.monthlyGrowth.transactions)}% este mês
              </Text>
            )}
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Volume Total"
              value={stats.totalAmount}
              prefix={<DollarOutlined />}
              precision={2}
              valueStyle={{ color: '#52c41a' }}
            />
            {stats.monthlyGrowth?.amount !== undefined && (
              <Text
                type={stats.monthlyGrowth.amount >= 0 ? 'success' : 'danger'}
                style={{ fontSize: 14, marginTop: 8, display: 'block' }}
              >
                {stats.monthlyGrowth.amount >= 0 ? <RiseOutlined /> : <FallOutlined />}
                {Math.abs(stats.monthlyGrowth.amount)}% este mês
              </Text>
            )}
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Taxa de Sucesso"
              value={stats.successRate}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Ticket Médio"
              value={stats.averageTicket}
              prefix={<CreditCardOutlined />}
              precision={2}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Today's Stats */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Transações Hoje"
              value={stats.transactionsToday || 0}
              valueStyle={{ fontSize: 24 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Volume Hoje"
              value={stats.amountToday || 0}
              precision={2}
              valueStyle={{ fontSize: 24 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Pendentes"
              value={stats.pendingTransactions || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ fontSize: 24, color: '#faad14' }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Ativas"
              value={stats.activeTransactions || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ fontSize: 24, color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Volume de Transações (7 dias)">
            <Line {...lineChartConfig} />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Quantidade de Transações (7 dias)">
            <Column {...columnChartConfig} />
          </Card>
        </Col>
      </Row>

      {/* Recent Transactions */}
      <Card title="Transações Recentes" style={{ marginTop: 24 }}>
        <Table
          columns={recentPaymentsColumns}
          dataSource={stats.recentTransactions || []}
          rowKey="id"
          pagination={false}
          scroll={{ x: 800 }}
        />
      </Card>
    </div>
  );
};

export default DashboardPage;
