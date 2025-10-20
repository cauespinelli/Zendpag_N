// @ts-nocheck
// @ts-nocheck
import React, { useState, useMemo, useRef, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Select,
  DatePicker,
  Space,
  Typography,
  Statistic,
  Button,
  Tooltip,
  Switch,
  Alert,
  Spin,
} from 'antd';
import {
  LineChartOutlined,
  BarChartOutlined,
  PieChartOutlined,
  RiseOutlined,
  FallOutlined,
  InfoCircleOutlined,
  FullscreenOutlined,
  DownloadOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  ArcElement,
  Tooltip as ChartTooltip,
  Legend,
  TimeScale,
  Filler,
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import 'chartjs-adapter-date-fns';
import dayjs from 'dayjs';

import { useTransactionAnalytics, useRevenueAnalytics } from '@/hooks/useQuery';
import { useDashboardUpdates } from '@/hooks/useWebSocket';
import { formatCurrency, formatNumber, formatPercentage } from '@/utils/helpers';
import { CHART_COLORS } from '@/utils/constants';
import type { AnalyticsData, ChartData } from '@/types';

const { Text, Title: AntTitle } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  ArcElement,
  ChartTooltip,
  Legend,
  TimeScale,
  Filler
);

interface AnalyticsChartsProps {
  embedded?: boolean;
  height?: number;
}

type PeriodType = '24h' | '7d' | '30d' | '90d' | 'custom';
type ChartType = 'line' | 'bar' | 'area';

const AnalyticsCharts: React.FC<AnalyticsChartsProps> = ({
  embedded = false,
  height = 300,
}) => {
  const [period, setPeriod] = useState<PeriodType>('30d');
  const [chartType, setChartType] = useState<ChartType>('line');
  const [customDateRange, setCustomDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [realTimeEnabled, setRealTimeEnabled] = useState(true);

  const chartRefs = {
    volume: useRef<any>(null),
    revenue: useRef<any>(null),
    conversion: useRef<any>(null),
  };

  // Real-time updates
  const { statsUpdated } = useDashboardUpdates();

  // Calculate date range based on period
  const dateRange = useMemo(() => {
    if (period === 'custom' && customDateRange) {
      return {
        startDate: customDateRange[0].format('YYYY-MM-DD'),
        endDate: customDateRange[1].format('YYYY-MM-DD'),
      };
    }

    const endDate = dayjs();
    let startDate = dayjs();

    switch (period) {
      case '24h':
        startDate = endDate.subtract(1, 'day');
        break;
      case '7d':
        startDate = endDate.subtract(7, 'days');
        break;
      case '30d':
        startDate = endDate.subtract(30, 'days');
        break;
      case '90d':
        startDate = endDate.subtract(90, 'days');
        break;
    }

    return {
      startDate: startDate.format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
    };
  }, [period, customDateRange]);

  // Data fetching
  const {
    data: transactionAnalytics,
    isLoading: transactionLoading,
    refetch: refetchTransactions,
  } = useTransactionAnalytics({
    period,
    groupBy: period === '24h' ? 'hour' : 'day',
    ...dateRange,
  });

  const {
    data: revenueAnalytics,
    isLoading: revenueLoading,
    refetch: refetchRevenue,
  } = useRevenueAnalytics({
    period,
    currency: 'BRL',
    ...dateRange,
  });

  // Auto refresh on real-time updates
  useEffect(() => {
    if (statsUpdated && realTimeEnabled) {
      refetchTransactions();
      refetchRevenue();
    }
  }, [statsUpdated, realTimeEnabled, refetchTransactions, refetchRevenue]);

  const handleRefresh = () => {
    refetchTransactions();
    refetchRevenue();
  };

  const handleExportChart = (chartRef: any, filename: string) => {
    if (chartRef.current) {
      const url = chartRef.current.toBase64Image();
      const link = document.createElement('a');
      link.download = `${filename}_${period}_${dayjs().format('YYYY-MM-DD')}.png`;
      link.href = url;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  // Chart configurations
  const baseChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top' as const,
        align: 'end' as const,
      },
      tooltip: {
        mode: 'index' as const,
        intersect: false,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: '#fff',
        bodyColor: '#fff',
        borderColor: 'var(--primary-color)',
        borderWidth: 1,
      },
    },
    scales: {
      x: {
        type: period === '24h' ? 'time' : 'category' as const,
        time: period === '24h' ? {
          unit: 'hour' as const,
          displayFormats: {
            hour: 'HH:mm',
          },
        } : undefined,
        grid: {
          color: 'var(--border-color)',
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'var(--border-color)',
        },
        ticks: {
          callback: function(value: any) {
            return typeof value === 'number' ? formatNumber(value) : value;
          },
        },
      },
    },
    interaction: {
      mode: 'nearest' as const,
      axis: 'x' as const,
      intersect: false,
    },
  };

  // Transaction Volume Chart Data
  const volumeChartData: ChartData = useMemo(() => {
    if (!transactionAnalytics?.dailyStats) {
      return { labels: [], datasets: [] };
    }

    const labels = transactionAnalytics.dailyStats.map(stat =>
      period === '24h'
        ? dayjs(stat.date).format('HH:mm')
        : dayjs(stat.date).format('DD/MM')
    );

    const data = transactionAnalytics.dailyStats.map(stat => stat.transactions);

    return {
      labels,
      datasets: [
        {
          label: 'Transações',
          data,
          borderColor: CHART_COLORS.PRIMARY,
          backgroundColor: chartType === 'area'
            ? `${CHART_COLORS.PRIMARY}20`
            : CHART_COLORS.PRIMARY,
          fill: chartType === 'area',
          tension: 0.4,
        },
      ],
    };
  }, [transactionAnalytics, period, chartType]);

  // Revenue Chart Data
  const revenueChartData: ChartData = useMemo(() => {
    if (!revenueAnalytics?.dailyStats) {
      return { labels: [], datasets: [] };
    }

    const labels = revenueAnalytics.dailyStats.map(stat =>
      period === '24h'
        ? dayjs(stat.date).format('HH:mm')
        : dayjs(stat.date).format('DD/MM')
    );

    const data = revenueAnalytics.dailyStats.map(stat => stat.amount);

    return {
      labels,
      datasets: [
        {
          label: 'Receita (R$)',
          data,
          borderColor: CHART_COLORS.SUCCESS,
          backgroundColor: chartType === 'area'
            ? `${CHART_COLORS.SUCCESS}20`
            : CHART_COLORS.SUCCESS,
          fill: chartType === 'area',
          tension: 0.4,
        },
      ],
    };
  }, [revenueAnalytics, period, chartType]);

  // PIX Key Types Distribution Chart Data
  const pixKeyTypesData: ChartData = useMemo(() => {
    if (!transactionAnalytics?.topPixKeyTypes) {
      return { labels: [], datasets: [] };
    }

    const labels = transactionAnalytics.topPixKeyTypes.map(item => item.type);
    const data = transactionAnalytics.topPixKeyTypes.map(item => item.count);
    const colors = CHART_COLORS.GRADIENT.slice(0, labels.length);

    return {
      labels,
      datasets: [
        {
          data,
          backgroundColor: colors,
          borderColor: colors.map(color => color),
          borderWidth: 2,
        },
      ],
    };
  }, [transactionAnalytics]);

  // Success Rate Chart Options
  const successRateOptions = {
    ...baseChartOptions,
    scales: {
      ...baseChartOptions.scales,
      y: {
        ...baseChartOptions.scales.y,
        min: 0,
        max: 100,
        ticks: {
          callback: function(value: any) {
            return `${value}%`;
          },
        },
      },
    },
  };

  const isLoading = transactionLoading || revenueLoading;

  return (
    <div className="analytics-charts">
      {/* Controls */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row justify="space-between" align="middle" gutter={[16, 8]}>
          <Col xs={24} sm={12} md={8}>
            <Space>
              <Select
                value={period}
                onChange={setPeriod}
                style={{ width: 120 }}
              >
                <Option value="24h">Últimas 24h</Option>
                <Option value="7d">Últimos 7 dias</Option>
                <Option value="30d">Últimos 30 dias</Option>
                <Option value="90d">Últimos 90 dias</Option>
                <Option value="custom">Personalizado</Option>
              </Select>

              {period === 'custom' && (
                <RangePicker
                  value={customDateRange}
                  onChange={setCustomDateRange}
                  format="DD/MM/YYYY"
                  style={{ width: 250 }}
                />
              )}
            </Space>
          </Col>

          <Col xs={24} sm={12} md={8}>
            <Space>
              <Select
                value={chartType}
                onChange={setChartType}
                style={{ width: 100 }}
                suffixIcon={<LineChartOutlined />}
              >
                <Option value="line">Linha</Option>
                <Option value="bar">Barra</Option>
                <Option value="area">Área</Option>
              </Select>

              <Tooltip title="Atualização em tempo real">
                <Switch
                  checked={realTimeEnabled}
                  onChange={setRealTimeEnabled}
                  checkedChildren="Auto"
                  unCheckedChildren="Manual"
                  size="small"
                />
              </Tooltip>

              <Button icon={<ReloadOutlined />} onClick={handleRefresh} size="small">
                Atualizar
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Key Metrics */}
      {(transactionAnalytics || revenueAnalytics) && (
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={6}>
            <Card>
              <Statistic
                title="Total de Transações"
                value={transactionAnalytics?.totalTransactions || 0}
                formatter={(value) => formatNumber(Number(value))}
                prefix={<RiseOutlined style={{ color: CHART_COLORS.PRIMARY }} />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card>
              <Statistic
                title="Receita Total"
                value={revenueAnalytics?.totalAmount || 0}
                formatter={(value) => formatCurrency(Number(value))}
                prefix={<RiseOutlined style={{ color: CHART_COLORS.SUCCESS }} />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card>
              <Statistic
                title="Taxa de Sucesso"
                value={transactionAnalytics?.successRate || 0}
                formatter={(value) => formatPercentage(Number(value) / 100)}
                prefix={<RiseOutlined style={{ color: CHART_COLORS.SUCCESS }} />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card>
              <Statistic
                title="Ticket Médio"
                value={revenueAnalytics?.averageAmount || 0}
                formatter={(value) => formatCurrency(Number(value))}
                prefix={<RiseOutlined style={{ color: CHART_COLORS.PRIMARY }} />}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* Charts Grid */}
      <Row gutter={[16, 16]}>
        {/* Transaction Volume Chart */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <LineChartOutlined />
                Volume de Transações
                <Tooltip title="Número de transações ao longo do tempo">
                  <InfoCircleOutlined style={{ color: 'var(--text-secondary)' }} />
                </Tooltip>
              </Space>
            }
            extra={
              <Button
                type="text"
                size="small"
                icon={<DownloadOutlined />}
                onClick={() => handleExportChart(chartRefs.volume, 'volume_transacoes')}
              />
            }
            loading={isLoading}
          >
            <div style={{ height }}>
              {chartType === 'bar' ? (
                <Bar
                  ref={chartRefs.volume}
                  data={volumeChartData}
                  options={baseChartOptions}
                />
              ) : (
                <Line
                  ref={chartRefs.volume}
                  data={volumeChartData}
                  options={baseChartOptions}
                />
              )}
            </div>
          </Card>
        </Col>

        {/* Revenue Chart */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <BarChartOutlined />
                Receita
                <Tooltip title="Receita total ao longo do tempo">
                  <InfoCircleOutlined style={{ color: 'var(--text-secondary)' }} />
                </Tooltip>
              </Space>
            }
            extra={
              <Button
                type="text"
                size="small"
                icon={<DownloadOutlined />}
                onClick={() => handleExportChart(chartRefs.revenue, 'receita')}
              />
            }
            loading={isLoading}
          >
            <div style={{ height }}>
              {chartType === 'bar' ? (
                <Bar
                  ref={chartRefs.revenue}
                  data={revenueChartData}
                  options={{
                    ...baseChartOptions,
                    scales: {
                      ...baseChartOptions.scales,
                      y: {
                        ...baseChartOptions.scales.y,
                        ticks: {
                          callback: function(value: any) {
                            return formatCurrency(Number(value));
                          },
                        },
                      },
                    },
                  }}
                />
              ) : (
                <Line
                  ref={chartRefs.revenue}
                  data={revenueChartData}
                  options={{
                    ...baseChartOptions,
                    scales: {
                      ...baseChartOptions.scales,
                      y: {
                        ...baseChartOptions.scales.y,
                        ticks: {
                          callback: function(value: any) {
                            return formatCurrency(Number(value));
                          },
                        },
                      },
                    },
                  }}
                />
              )}
            </div>
          </Card>
        </Col>

        {/* PIX Key Types Distribution */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <PieChartOutlined />
                Distribuição por Tipo de Chave PIX
              </Space>
            }
            loading={isLoading}
          >
            <div style={{ height }}>
              <Doughnut
                data={pixKeyTypesData}
                options={{
                  responsive: true,
                  maintainAspectRatio: false,
                  plugins: {
                    legend: {
                      position: 'right',
                    },
                    tooltip: {
                      callbacks: {
                        label: function(context) {
                          const total = context.dataset.data.reduce((a: any, b: any) => a + b, 0);
                          const percentage = ((context.parsed / total) * 100).toFixed(1);
                          return `${context.label}: ${context.parsed} (${percentage}%)`;
                        },
                      },
                    },
                  },
                }}
              />
            </div>
          </Card>
        </Col>

        {/* Success Rate Chart */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <RiseOutlined />
                Taxa de Sucesso
                <Tooltip title="Percentual de transações bem-sucedidas">
                  <InfoCircleOutlined style={{ color: 'var(--text-secondary)' }} />
                </Tooltip>
              </Space>
            }
            loading={isLoading}
          >
            <div style={{ height }}>
              <Line
                data={{
                  labels: revenueAnalytics?.dailyStats?.map(stat =>
                    period === '24h'
                      ? dayjs(stat.date).format('HH:mm')
                      : dayjs(stat.date).format('DD/MM')
                  ) || [],
                  datasets: [
                    {
                      label: 'Taxa de Sucesso (%)',
                      data: revenueAnalytics?.dailyStats?.map(stat => stat.successRate * 100) || [],
                      borderColor: CHART_COLORS.SUCCESS,
                      backgroundColor: `${CHART_COLORS.SUCCESS}20`,
                      fill: true,
                      tension: 0.4,
                    },
                  ],
                }}
                options={successRateOptions}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Real-time Update Indicator */}
      {statsUpdated && realTimeEnabled && (
        <Alert
          message="Dados atualizados em tempo real"
          type="success"
          style={{
            position: 'fixed',
            top: 80,
            right: 24,
            zIndex: 1000,
            animation: 'fadeIn 0.3s ease-in-out',
          }}
          closable
        />
      )}
    </div>
  );
};

export default AnalyticsCharts;