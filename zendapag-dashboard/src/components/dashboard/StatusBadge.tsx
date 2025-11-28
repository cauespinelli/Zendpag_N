import React from 'react';
import { CheckCircle, Clock, XCircle, RefreshCw } from 'lucide-react';

interface StatusBadgeProps {
  status: 'completed' | 'pending' | 'failed' | 'refunded';
}

const statusConfig = {
  completed: {
    label: 'Concluído',
    icon: CheckCircle,
    className: 'status-completed'
  },
  pending: {
    label: 'Pendente',
    icon: Clock,
    className: 'status-pending'
  },
  failed: {
    label: 'Falhou',
    icon: XCircle,
    className: 'status-failed'
  },
  refunded: {
    label: 'Reembolsado',
    icon: RefreshCw,
    className: 'status-refunded'
  }
};

export default function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <span className={`status-badge ${config.className}`}>
      <Icon size={14} />
      {config.label}
    </span>
  );
}
