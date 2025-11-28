import React from 'react';
import { LucideIcon, TrendingUp, TrendingDown } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string;
  change: number;
  icon: LucideIcon;
  color: 'primary' | 'success' | 'cyan' | 'warning' | 'error';
}

const colorClasses = {
  primary: {
    bg: 'bg-blue-50',
    text: 'text-blue-600',
    icon: 'bg-blue-100'
  },
  success: {
    bg: 'bg-green-50',
    text: 'text-green-600',
    icon: 'bg-green-100'
  },
  cyan: {
    bg: 'bg-cyan-50',
    text: 'text-cyan-600',
    icon: 'bg-cyan-100'
  },
  warning: {
    bg: 'bg-amber-50',
    text: 'text-amber-600',
    icon: 'bg-amber-100'
  },
  error: {
    bg: 'bg-red-50',
    text: 'text-red-600',
    icon: 'bg-red-100'
  }
};

export default function MetricCard({ title, value, change, icon: Icon, color }: MetricCardProps) {
  const colorClass = colorClasses[color];
  const isPositive = change >= 0;

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-4">
        <span className="text-sm font-medium text-gray-600">{title}</span>
        <div className={`p-2 rounded-lg ${colorClass.icon}`}>
          <Icon className={`w-5 h-5 ${colorClass.text}`} />
        </div>
      </div>

      <div className="space-y-2">
        <div className="text-3xl font-bold text-gray-900">{value}</div>

        <div className="flex items-center gap-1">
          {isPositive ? (
            <TrendingUp className="w-4 h-4 text-green-600" />
          ) : (
            <TrendingDown className="w-4 h-4 text-red-600" />
          )}
          <span className={`text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
            {isPositive ? '+' : ''}{change}%
          </span>
          <span className="text-sm text-gray-500 ml-1">vs último período</span>
        </div>
      </div>
    </div>
  );
}
