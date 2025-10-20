// @ts-nocheck
import React, { useState } from 'react';
import { Card, Button, Table, Tag, Space, Typography, DatePicker, Select, message, Row, Col } from 'antd';
import { DownloadOutlined, FileTextOutlined } from '@ant-design/icons';
import { formatDate } from '@/utils/helpers';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const ReportsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState([
    {
      id: '1',
      type: 'TRANSACTIONS',
      period: '01/10/2025 - 11/10/2025',
      status: 'COMPLETED',
      format: 'CSV',
      createdAt: '2025-10-11T10:00:00',
      recordCount: 1234,
    },
  ]);

  const handleGenerate = () => {
    message.success('Relatório está sendo gerado. Você será notificado quando estiver pronto.');
  };

  const columns = [
    {
      title: 'Tipo',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => {
        const labels = { TRANSACTIONS: 'Transações', FINANCIAL: 'Financeiro', RECONCILIATION: 'Reconciliação' };
        return labels[type] || type;
      },
    },
    {
      title: 'Período',
      dataIndex: 'period',
      key: 'period',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colors = { GENERATING: 'processing', COMPLETED: 'success', FAILED: 'error' };
        const labels = { GENERATING: 'Gerando', COMPLETED: 'Concluído', FAILED: 'Falhou' };
        return <Tag color={colors[status]}>{labels[status]}</Tag>;
      },
    },
    {
      title: 'Formato',
      dataIndex: 'format',
      key: 'format',
    },
    {
      title: 'Registros',
      dataIndex: 'recordCount',
      key: 'recordCount',
    },
    {
      title: 'Criado em',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => formatDate(date),
    },
    {
      title: 'Ações',
      key: 'actions',
      render: (record: any) => (
        <Button type="primary" icon={<DownloadOutlined />} size="small" disabled={record.status !== 'COMPLETED'}>
          Download
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Title level={2}>Relatórios</Title>
      <Text type="secondary">Gere e baixe relatórios de transações</Text>

      <Card style={{ marginTop: 24, marginBottom: 16 }}>
        <Title level={4}>Gerar Novo Relatório</Title>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Row gutter={16}>
            <Col span={8}>
              <Select placeholder="Tipo de Relatório" style={{ width: '100%' }}>
                <Select.Option value="TRANSACTIONS">Transações</Select.Option>
                <Select.Option value="FINANCIAL">Financeiro</Select.Option>
                <Select.Option value="RECONCILIATION">Reconciliação</Select.Option>
              </Select>
            </Col>
            <Col span={8}>
              <RangePicker style={{ width: '100%' }} />
            </Col>
            <Col span={4}>
              <Select placeholder="Formato" defaultValue="CSV" style={{ width: '100%' }}>
                <Select.Option value="CSV">CSV</Select.Option>
                <Select.Option value="PDF">PDF</Select.Option>
                <Select.Option value="JSON">JSON</Select.Option>
              </Select>
            </Col>
            <Col span={4}>
              <Button type="primary" icon={<FileTextOutlined />} onClick={handleGenerate} block>
                Gerar
              </Button>
            </Col>
          </Row>
        </Space>
      </Card>

      <Card>
        <Table columns={columns} dataSource={reports} rowKey="id" loading={loading} pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  );
};

export default ReportsPage;
