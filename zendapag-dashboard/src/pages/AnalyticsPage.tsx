// @ts-nocheck
import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Spin, DatePicker, Space } from 'antd';
import { RiseOutlined, FallOutlined, DollarOutlined, TransactionOutlined } from '@ant-design/icons';
import { Line, Column, Pie } from '@ant-design/plots';
import apiService from '@/services/api';
import { API_ENDPOINTS } from '@/utils/constants';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const AnalyticsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<any>(null);

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    try {
      setLoading(true);
      const response = await apiService.get(API_ENDPOINTS.ANALYTICS.DASHBOARD);
      setData(response);
    } catch (error) {
      console.error('Error fetching analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ textAlign: 'center', padding: 50 }}><Spin size="large" /></div>;
  if (!data) return null;

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2} style={{ margin: 0 }}>Analytics</Title>
          <Text type="secondary">Análise detalhada das suas transações</Text>
        </Col>
        <Col>
          <RangePicker onChange={fetchAnalytics} />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Receita Total"
              value={data.totalAmount || 0}
              precision={2}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total de Transações"
              value={data.totalTransactions || 0}
              prefix={<TransactionOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Taxa de Sucesso"
              value={data.successRate || 0}
              suffix="%"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Usuários Ativos"
              value={data.activeUsers || 0}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Transações por Dia">
            <Column
              data={data.dailyStats || []}
              xField="date"
              yField="transactions"
              label={{ position: 'top' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Volume Financeiro">
            <Line
              data={data.dailyStats || []}
              xField="date"
              yField="amount"
              smooth
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AnalyticsPage;
