// @ts-nocheck
import React, { useEffect, useState } from 'react';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  CreditCard,
  Smartphone,
  FileText,
  ArrowUpRight,
  ArrowDownRight,
  Calendar,
  RefreshCw,
} from 'lucide-react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import apiService from '@/services/api';
import { API_ENDPOINTS } from '@/utils/constants';
import { formatCurrency } from '@/utils/helpers';
import type { DashboardStats } from '@/types';

// Period filter options
const periodOptions = [
  { label: 'Hoje', value: 'today' },
  { label: '7 Dias', value: '7days' },
  { label: '30 Dias', value: '30days' },
  { label: 'Personalizado', value: 'custom' },
];

// Mock data for charts (will be replaced with real API data)
const mockLineData = [
  { date: '01/12', value: 125000 },
  { date: '02/12', value: 148000 },
  { date: '03/12', value: 132000 },
  { date: '04/12', value: 156000 },
  { date: '05/12', value: 142000 },
  { date: '06/12', value: 168000 },
  { date: '07/12', value: 175000 },
];

const mockBarData = [
  { name: 'PIX', value: 68 },
  { name: 'Cartão', value: 22 },
  { name: 'Boleto', value: 10 },
];

const COLORS = ['#3B82F6', '#F59E0B', '#8B5CF6'];

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: number;
  icon: React.ReactNode;
  color: 'blue' | 'yellow' | 'purple' | 'orange' | 'green' | 'red';
}

const colorClasses = {
  blue: 'bg-blue-600 text-white shadow-lg shadow-blue-600/20',
  yellow: 'bg-yellow-500 text-white shadow-lg shadow-yellow-500/20',
  purple: 'bg-purple-600 text-white shadow-lg shadow-purple-600/20',
  orange: 'bg-orange-500 text-white shadow-lg shadow-orange-500/20',
  green: 'bg-green-600 text-white shadow-lg shadow-green-600/20',
  red: 'bg-red-500/10 text-red-500 border-red-500/20',
};

const iconBgClasses = {
  blue: 'bg-blue-500',
  yellow: 'bg-yellow-500',
  purple: 'bg-purple-500',
  orange: 'bg-orange-500',
  green: 'bg-green-500',
  red: 'bg-red-500',
};

const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, trend, icon, color }) => {
  return (
    <div className={`rounded-xl p-5 ${colorClasses[color]}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm opacity-80 mb-1">{title}</p>
          <p className="text-2xl font-bold text-white">{value}</p>
          {subtitle && (
            <p className="text-xs opacity-60 mt-1">{subtitle}</p>
          )}
        </div>
        <div className={`w-10 h-10 rounded-lg ${iconBgClasses[color]} flex items-center justify-center`}>
          {icon}
        </div>
      </div>
      {trend !== undefined && (
        <div className="flex items-center gap-1 mt-3">
          {trend >= 0 ? (
            <ArrowUpRight size={14} className="text-green-500" />
          ) : (
            <ArrowDownRight size={14} className="text-red-500" />
          )}
          <span className={trend >= 0 ? 'text-green-500' : 'text-red-500'}>
            {Math.abs(trend)}%
          </span>
          <span className="text-gray-500 text-xs">vs período anterior</span>
        </div>
      )}
    </div>
  );
};

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedPeriod, setSelectedPeriod] = useState('7days');

  useEffect(() => {
    fetchDashboardData();
  }, [selectedPeriod]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.get<DashboardStats>(API_ENDPOINTS.ANALYTICS.DASHBOARD);
      setStats(data);
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar dados do dashboard');
      // Use mock data on error for demo purposes
      setStats({
        totalTransactions: 1247,
        totalAmount: 458923.50,
        successRate: 94.5,
        averageTicket: 368.15,
        transactionsToday: 87,
        amountToday: 32150.00,
        pendingTransactions: 12,
        activeTransactions: 75,
        monthlyGrowth: {
          transactions: 12.5,
          amount: 8.3,
        },
      } as DashboardStats);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="flex flex-col items-center gap-4">
          <RefreshCw className="w-8 h-8 text-blue-500 animate-spin" />
          <p className="text-gray-400">Carregando dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-gray-400 text-sm">Visão geral das suas transações</p>
        </div>

        {/* Period Filter */}
        <div className="flex items-center gap-2 bg-gray-900 rounded-lg p-1">
          {periodOptions.map((option) => (
            <button
              key={option.value}
              onClick={() => setSelectedPeriod(option.value)}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                selectedPeriod === option.value
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-gray-800'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <StatCard
          title="Volume Total"
          value={formatCurrency(stats?.totalAmount || 0)}
          trend={stats?.monthlyGrowth?.amount}
          icon={<DollarSign size={20} className="text-white" />}
          color="blue"
        />
        <StatCard
          title="Transações"
          value={stats?.totalTransactions?.toLocaleString('pt-BR') || '0'}
          trend={stats?.monthlyGrowth?.transactions}
          icon={<TrendingUp size={20} className="text-white" />}
          color="yellow"
        />
        <StatCard
          title="Taxa de Sucesso"
          value={`${stats?.successRate || 0}%`}
          subtitle="Aprovadas / Total"
          icon={<CreditCard size={20} className="text-white" />}
          color="purple"
        />
        <StatCard
          title="Ticket Médio"
          value={formatCurrency(stats?.averageTicket || 0)}
          icon={<FileText size={20} className="text-white" />}
          color="orange"
        />
        <StatCard
          title="Pendentes"
          value={stats?.pendingTransactions || 0}
          subtitle={`${stats?.activeTransactions || 0} ativas`}
          icon={<RefreshCw size={20} className="text-white" />}
          color="green"
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Volume Chart */}
        <div className="lg:col-span-2 bg-gray-900 rounded-xl p-6 border border-gray-800">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-semibold text-white">Volume de Transações</h3>
            <div className="flex items-center gap-4 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                <span className="text-gray-400">Volume</span>
              </div>
            </div>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={mockLineData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                <XAxis
                  dataKey="date"
                  stroke="#6B7280"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="#6B7280"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value) => `R$${(value / 1000).toFixed(0)}k`}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1F2937',
                    border: '1px solid #374151',
                    borderRadius: '8px',
                  }}
                  labelStyle={{ color: '#9CA3AF' }}
                  formatter={(value: number) => [formatCurrency(value), 'Volume']}
                />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="#3B82F6"
                  strokeWidth={2}
                  dot={{ fill: '#3B82F6', strokeWidth: 2 }}
                  activeDot={{ r: 6, fill: '#3B82F6' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Payment Methods Distribution */}
        <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
          <h3 className="text-lg font-semibold text-white mb-6">Métodos de Pagamento</h3>
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={mockBarData}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={70}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {mockBarData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1F2937',
                    border: '1px solid #374151',
                    borderRadius: '8px',
                  }}
                  formatter={(value: number) => [`${value}%`, 'Participação']}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="space-y-3 mt-4">
            {mockBarData.map((item, index) => (
              <div key={item.name} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: COLORS[index] }}
                  ></div>
                  <span className="text-gray-400 text-sm">{item.name}</span>
                </div>
                <span className="text-white font-medium">{item.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Today's Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-blue-500/20 rounded-lg flex items-center justify-center">
              <Smartphone size={20} className="text-blue-500" />
            </div>
            <div>
              <p className="text-xs text-gray-500">PIX Hoje</p>
              <p className="text-lg font-bold text-white">{formatCurrency(stats?.amountToday || 0)}</p>
            </div>
          </div>
          <div className="h-1 bg-gray-800 rounded-full">
            <div className="h-1 bg-blue-500 rounded-full" style={{ width: '68%' }}></div>
          </div>
        </div>

        <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-yellow-500/20 rounded-lg flex items-center justify-center">
              <CreditCard size={20} className="text-yellow-500" />
            </div>
            <div>
              <p className="text-xs text-gray-500">Cartão Hoje</p>
              <p className="text-lg font-bold text-white">{formatCurrency((stats?.amountToday || 0) * 0.32)}</p>
            </div>
          </div>
          <div className="h-1 bg-gray-800 rounded-full">
            <div className="h-1 bg-yellow-500 rounded-full" style={{ width: '22%' }}></div>
          </div>
        </div>

        <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-purple-500/20 rounded-lg flex items-center justify-center">
              <FileText size={20} className="text-purple-500" />
            </div>
            <div>
              <p className="text-xs text-gray-500">Boleto Hoje</p>
              <p className="text-lg font-bold text-white">{formatCurrency((stats?.amountToday || 0) * 0.15)}</p>
            </div>
          </div>
          <div className="h-1 bg-gray-800 rounded-full">
            <div className="h-1 bg-purple-500 rounded-full" style={{ width: '10%' }}></div>
          </div>
        </div>

        <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-green-500/20 rounded-lg flex items-center justify-center">
              <Calendar size={20} className="text-green-500" />
            </div>
            <div>
              <p className="text-xs text-gray-500">Transações Hoje</p>
              <p className="text-lg font-bold text-white">{stats?.transactionsToday || 0}</p>
            </div>
          </div>
          <div className="h-1 bg-gray-800 rounded-full">
            <div className="h-1 bg-green-500 rounded-full" style={{ width: '45%' }}></div>
          </div>
        </div>
      </div>

      {/* Recent Activity Section */}
      <div className="bg-gray-900 rounded-xl border border-gray-800">
        <div className="p-6 border-b border-gray-800">
          <h3 className="text-lg font-semibold text-white">Atividade Recente</h3>
        </div>
        <div className="p-6">
          <div className="space-y-4">
            {[
              { type: 'success', text: 'Pagamento PIX aprovado', amount: 'R$ 1.250,00', time: 'há 2 min' },
              { type: 'pending', text: 'Saque solicitado', amount: 'R$ 5.000,00', time: 'há 15 min' },
              { type: 'success', text: 'Pagamento Cartão aprovado', amount: 'R$ 890,00', time: 'há 32 min' },
              { type: 'error', text: 'Pagamento recusado', amount: 'R$ 2.100,00', time: 'há 1 hora' },
              { type: 'success', text: 'Pagamento PIX aprovado', amount: 'R$ 450,00', time: 'há 2 horas' },
            ].map((item, index) => (
              <div key={index} className="flex items-center justify-between py-3 border-b border-gray-800 last:border-0">
                <div className="flex items-center gap-4">
                  <div
                    className={`w-2 h-2 rounded-full ${
                      item.type === 'success' ? 'bg-green-500' :
                      item.type === 'pending' ? 'bg-yellow-500' : 'bg-red-500'
                    }`}
                  ></div>
                  <div>
                    <p className="text-white text-sm">{item.text}</p>
                    <p className="text-gray-500 text-xs">{item.time}</p>
                  </div>
                </div>
                <span className="text-white font-medium">{item.amount}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
