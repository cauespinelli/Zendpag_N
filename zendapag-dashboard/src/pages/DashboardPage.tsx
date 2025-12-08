// @ts-nocheck
import React, { useState } from 'react';
import {
  Users,
  FileText,
  Wallet,
  DollarSign,
  TrendingUp,
  ArrowUpRight,
  ArrowDownRight,
  Eye,
  EyeOff,
  Calendar,
  MoreHorizontal,
  Smartphone,
  CreditCard,
  Clock,
  Receipt,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

type Period = 'today' | '7days' | '30days' | 'custom';

const DashboardPage: React.FC = () => {
  const [showValues, setShowValues] = useState(true);
  const [period, setPeriod] = useState<Period>('30days');

  const formatCurrency = (value: number) => {
    if (!showValues) return 'R$ •••••••';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const formatNumber = (value: number) => {
    if (!showValues) return '•••';
    return new Intl.NumberFormat('pt-BR').format(value);
  };

  const chartData = [
    { name: '01', value: 45000 },
    { name: '05', value: 52000 },
    { name: '10', value: 38000 },
    { name: '15', value: 65000 },
    { name: '20', value: 78000 },
    { name: '25', value: 55000 },
    { name: '30', value: 82000 },
  ];

  const stats = [
    { title: 'Total de Usuários', value: 27, icon: Users, trend: { value: 12, positive: true } },
    { title: 'Docs em Análise', value: 6, icon: FileText, trend: { value: 3, positive: false } },
    { title: 'Total de Saques', value: 9698, icon: Wallet, trend: { value: 8, positive: true } },
    { title: 'Valor dos Saques', value: 764599.01, icon: DollarSign, color: 'gold' as const, isCurrency: true },
    { title: 'Lucro Total', value: 10745.56, icon: TrendingUp, color: 'green' as const, isCurrency: true },
  ];

  const paymentMethods = [
    { name: 'PIX', value: 774675.02, percent: 68, color: '#00C853' },
    { name: 'Cartão', value: 0, percent: 22, color: '#4A90D9' },
    { name: 'Boleto', value: 0, percent: 10, color: '#C9A962' },
  ];

  const recentActivity = [
    { type: 'PIX aprovado', time: 'há 2 min', value: 1250, status: 'success' },
    { type: 'Saque solicitado', time: 'há 15 min', value: 5000, status: 'warning' },
    { type: 'Cartão aprovado', time: 'há 32 min', value: 890, status: 'success' },
    { type: 'Pagamento recusado', time: 'há 1 hora', value: 2100, status: 'error' },
  ];

  const todayStats = [
    { title: 'PIX Hoje', value: 32150, icon: Smartphone, color: '#00C853', progress: 75 },
    { title: 'Cartão Hoje', value: 10288, icon: CreditCard, color: '#4A90D9', progress: 45 },
    { title: 'Boleto Hoje', value: 4822.5, icon: FileText, color: '#C9A962', progress: 25 },
    { title: 'Transações Hoje', value: 87, icon: Receipt, color: '#E53935', progress: 60, isCount: true },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Dashboard</h1>
          <p className="text-[#5C5C5C] text-sm mt-1">Visão geral das suas operações</p>
        </div>

        <div className="flex items-center gap-4">
          {/* Period Selector */}
          <div className="flex items-center gap-1 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl p-1">
            {[
              { key: 'today', label: 'Hoje' },
              { key: '7days', label: '7 dias' },
              { key: '30days', label: '30 dias' },
            ].map((item) => (
              <button
                key={item.key}
                onClick={() => setPeriod(item.key as Period)}
                className={`px-4 py-2 rounded-lg text-sm transition-colors ${
                  period === item.key
                    ? 'bg-[#2D2D2D] text-white'
                    : 'text-[#8C8C8C] hover:text-white'
                }`}
              >
                {item.label}
              </button>
            ))}
            <button className="p-2 text-[#8C8C8C] hover:text-white">
              <Calendar size={18} strokeWidth={1.5} />
            </button>
          </div>

          {/* Toggle Values */}
          <button
            onClick={() => setShowValues(!showValues)}
            className="flex items-center gap-2 px-4 py-2 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl text-[#8C8C8C] hover:text-white transition-colors"
          >
            {showValues ? <EyeOff size={18} strokeWidth={1.5} /> : <Eye size={18} strokeWidth={1.5} />}
            <span className="text-sm">{showValues ? 'Ocultar' : 'Mostrar'}</span>
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div
              key={index}
              className="bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-5 hover:border-[#3D3D3D] transition-all duration-200"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="p-2.5 bg-[#2D2D2D] rounded-xl">
                  <Icon size={18} strokeWidth={1.5} className="text-[#C9A962]" />
                </div>
                {stat.trend && (
                  <div className={`flex items-center gap-1 text-xs ${stat.trend.positive ? 'text-[#00C853]' : 'text-[#E53935]'}`}>
                    {stat.trend.positive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                    {stat.trend.value}%
                  </div>
                )}
              </div>
              <p className="text-[#8C8C8C] text-sm">{stat.title}</p>
              <p className={`text-xl font-semibold mt-1 ${
                stat.color === 'gold' ? 'text-[#C9A962]' :
                stat.color === 'green' ? 'text-[#00C853]' : 'text-white'
              }`}>
                {stat.isCurrency ? formatCurrency(stat.value) : formatNumber(stat.value)}
              </p>
            </div>
          );
        })}
      </div>

      {/* Chart Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Chart */}
        <div className="lg:col-span-2 bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-white font-medium">Volume de Transações</h3>
              <p className="text-[#5C5C5C] text-sm">Últimos 30 dias</p>
            </div>
            <button className="p-2 text-[#5C5C5C] hover:text-white rounded-lg hover:bg-[#2D2D2D] transition-colors">
              <MoreHorizontal size={18} />
            </button>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#C9A962" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#C9A962" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#2D2D2D" vertical={false} />
                <XAxis dataKey="name" stroke="#5C5C5C" axisLine={false} tickLine={false} />
                <YAxis stroke="#5C5C5C" axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1A1A1A',
                    border: '1px solid #2D2D2D',
                    borderRadius: '12px',
                    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
                  }}
                  labelStyle={{ color: '#8C8C8C' }}
                  itemStyle={{ color: '#C9A962' }}
                  formatter={(value: number) => [formatCurrency(value), 'Volume']}
                />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#C9A962"
                  strokeWidth={2}
                  fill="url(#colorValue)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Payment Methods */}
        <div className="bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-6">
          <h3 className="text-white font-medium mb-6">Métodos de Pagamento</h3>
          <div className="space-y-4">
            {paymentMethods.map((method, index) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full" style={{ backgroundColor: method.color }} />
                    <span className="text-sm text-[#8C8C8C]">{method.name}</span>
                  </div>
                  <span className="text-sm text-white">{formatCurrency(method.value)}</span>
                </div>
                <div className="h-1.5 bg-[#2D2D2D] rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{ width: `${method.percent}%`, backgroundColor: method.color }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* Summary */}
          <div className="mt-6 pt-6 border-t border-[#2D2D2D]">
            <div className="flex items-center justify-between mb-3">
              <span className="text-[#8C8C8C] text-sm">Total Geral</span>
              <span className="text-white font-medium">{formatCurrency(774675.02)}</span>
            </div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-[#8C8C8C] text-sm">Valor Líquido</span>
              <span className="text-[#00C853] font-medium">{formatCurrency(774176.82)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-[#8C8C8C] text-sm">Saques Pendentes</span>
              <span className="text-[#FFD600] font-medium">2</span>
            </div>
          </div>
        </div>
      </div>

      {/* Today Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {todayStats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-5 hover:border-[#3D3D3D] transition-colors">
              <div className="flex items-center gap-3 mb-4">
                <div
                  className="w-10 h-10 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${stat.color}20` }}
                >
                  <Icon size={20} strokeWidth={1.5} style={{ color: stat.color }} />
                </div>
                <div>
                  <p className="text-[#5C5C5C] text-xs">{stat.title}</p>
                  <p className="text-white font-semibold">
                    {stat.isCount ? formatNumber(stat.value) : formatCurrency(stat.value)}
                  </p>
                </div>
              </div>
              <div className="h-1.5 bg-[#2D2D2D] rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500"
                  style={{ width: `${stat.progress}%`, backgroundColor: stat.color }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {/* Recent Activity */}
      <div className="bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Clock size={20} strokeWidth={1.5} className="text-[#8C8C8C]" />
            <h3 className="text-white font-medium">Atividade Recente</h3>
          </div>
          <button className="text-sm text-[#C9A962] hover:underline">Ver tudo</button>
        </div>
        <div className="space-y-4">
          {recentActivity.map((activity, index) => (
            <div
              key={index}
              className="flex items-center justify-between py-3 border-b border-[#2D2D2D] last:border-0"
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-2 h-2 rounded-full ${
                    activity.status === 'success'
                      ? 'bg-[#00C853]'
                      : activity.status === 'warning'
                      ? 'bg-[#FFD600]'
                      : 'bg-[#E53935]'
                  }`}
                />
                <div>
                  <p className="text-white text-sm">{activity.type}</p>
                  <p className="text-[#5C5C5C] text-xs">{activity.time}</p>
                </div>
              </div>
              <span className="text-white font-medium">{formatCurrency(activity.value)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
