/**
 * ZENDPAG DESIGN SYSTEM - C6 BANK STYLE
 * Component: StatCard
 *
 * Statistics card component with C6 Bank premium aesthetics
 */

import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: {
    value: number;
    positive: boolean;
  };
  color?: 'default' | 'gold' | 'green' | 'red' | 'blue';
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  color = 'default',
  className = '',
}) => {
  const colorClasses = {
    default: 'text-white',
    gold: 'text-[#C9A962]',
    green: 'text-[#00C853]',
    red: 'text-[#E53935]',
    blue: 'text-[#4A90D9]',
  };

  return (
    <div
      className={`bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl p-6 hover:border-[#3D3D3D] transition-colors ${className}`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="p-3 bg-[#2D2D2D] rounded-xl">
          <Icon size={20} strokeWidth={1.5} className="text-[#C9A962]" />
        </div>
        {trend && (
          <span
            className={`text-xs font-medium ${
              trend.positive ? 'text-[#00C853]' : 'text-[#E53935]'
            }`}
          >
            {trend.positive ? '+' : ''}
            {trend.value}%
          </span>
        )}
      </div>
      <p className="text-[#8C8C8C] text-sm mb-1">{title}</p>
      <p className={`text-2xl font-semibold ${colorClasses[color]}`}>{value}</p>
      {subtitle && <p className="text-[#5C5C5C] text-xs mt-2">{subtitle}</p>}
    </div>
  );
};

export default StatCard;
