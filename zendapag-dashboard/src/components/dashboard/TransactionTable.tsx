import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search,
  Filter,
  Download,
  ChevronLeft,
  ChevronRight,
  MoreVertical,
  Eye,
  Copy,
  RefreshCw
} from 'lucide-react';
import StatusBadge from './StatusBadge';

interface Transaction {
  id: string;
  date: string;
  customer: string;
  amount: number;
  method: 'pix' | 'credit' | 'debit' | 'boleto';
  status: 'completed' | 'pending' | 'failed' | 'refunded';
}

interface TransactionTableProps {
  limit?: number;
}

export default function TransactionTable({ limit }: TransactionTableProps) {
  const [currentPage, setCurrentPage] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedTransaction, setSelectedTransaction] = useState<string | null>(null);

  // Mock data
  const allTransactions: Transaction[] = [
    {
      id: 'TRX-2024-001247',
      date: '2024-11-12 14:32',
      customer: 'João Silva',
      amount: 3499.00,
      method: 'pix',
      status: 'completed'
    },
    {
      id: 'TRX-2024-001246',
      date: '2024-11-12 14:15',
      customer: 'Maria Santos',
      amount: 1299.90,
      method: 'credit',
      status: 'completed'
    },
    {
      id: 'TRX-2024-001245',
      date: '2024-11-12 13:48',
      customer: 'Pedro Costa',
      amount: 899.00,
      method: 'pix',
      status: 'pending'
    },
    {
      id: 'TRX-2024-001244',
      date: '2024-11-12 13:22',
      customer: 'Ana Oliveira',
      amount: 2150.00,
      method: 'boleto',
      status: 'pending'
    },
    {
      id: 'TRX-2024-001243',
      date: '2024-11-12 12:55',
      customer: 'Carlos Mendes',
      amount: 549.90,
      method: 'credit',
      status: 'failed'
    },
    {
      id: 'TRX-2024-001242',
      date: '2024-11-12 12:30',
      customer: 'Beatriz Lima',
      amount: 1899.00,
      method: 'pix',
      status: 'completed'
    },
    {
      id: 'TRX-2024-001241',
      date: '2024-11-12 11:45',
      customer: 'Rafael Souza',
      amount: 3200.00,
      method: 'credit',
      status: 'refunded'
    },
    {
      id: 'TRX-2024-001240',
      date: '2024-11-12 11:20',
      customer: 'Juliana Rocha',
      amount: 750.00,
      method: 'debit',
      status: 'completed'
    },
    {
      id: 'TRX-2024-001239',
      date: '2024-11-12 10:55',
      customer: 'Fernando Alves',
      amount: 4500.00,
      method: 'pix',
      status: 'completed'
    },
    {
      id: 'TRX-2024-001238',
      date: '2024-11-12 10:30',
      customer: 'Camila Martins',
      amount: 1650.00,
      method: 'credit',
      status: 'completed'
    },
  ];

  // Filtrar transações
  const filteredTransactions = allTransactions.filter(transaction => {
    const matchesSearch =
      transaction.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      transaction.customer.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesFilter =
      filterStatus === 'all' || transaction.status === filterStatus;

    return matchesSearch && matchesFilter;
  });

  // Aplicar limite se fornecido
  const displayTransactions = limit
    ? filteredTransactions.slice(0, limit)
    : filteredTransactions;

  // Paginação
  const itemsPerPage = 10;
  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedTransactions = limit
    ? displayTransactions
    : filteredTransactions.slice(startIndex, endIndex);

  // Método de pagamento labels
  const methodLabels = {
    pix: 'PIX',
    credit: 'Crédito',
    debit: 'Débito',
    boleto: 'Boleto'
  };

  // Copiar ID
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    // Adicionar toast notification aqui
  };

  return (
    <div className="transaction-table-container">
      {/* Filters & Actions */}
      {!limit && (
        <div className="table-toolbar">
          <div className="table-toolbar-left">
            {/* Search */}
            <div className="table-search">
              <Search size={18} />
              <input
                type="text"
                placeholder="Buscar transações..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            {/* Status Filter */}
            <select
              className="table-filter"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              <option value="all">Todos os status</option>
              <option value="completed">Concluídos</option>
              <option value="pending">Pendentes</option>
              <option value="failed">Falhados</option>
              <option value="refunded">Reembolsados</option>
            </select>

            <button className="btn btn-ghost btn-sm">
              <Filter size={18} />
              Mais filtros
            </button>
          </div>

          <div className="table-toolbar-right">
            <button className="btn btn-ghost btn-sm">
              <RefreshCw size={18} />
              Atualizar
            </button>
            <button className="btn btn-secondary btn-sm">
              <Download size={18} />
              Exportar
            </button>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="table-wrapper">
        <table className="transaction-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Data/Hora</th>
              <th>Cliente</th>
              <th>Valor</th>
              <th>Método</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <AnimatePresence mode="wait">
              {paginatedTransactions.map((transaction, index) => (
                <motion.tr
                  key={transaction.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -20 }}
                  transition={{ delay: index * 0.05 }}
                  className="table-row"
                >
                  <td>
                    <div className="transaction-id">
                      <span className="font-mono">{transaction.id}</span>
                      <button
                        className="copy-button"
                        onClick={() => copyToClipboard(transaction.id)}
                        title="Copiar ID"
                      >
                        <Copy size={14} />
                      </button>
                    </div>
                  </td>
                  <td>
                    <span className="transaction-date">{transaction.date}</span>
                  </td>
                  <td>
                    <div className="customer-info">
                      <div className="customer-avatar">
                        {transaction.customer.charAt(0)}
                      </div>
                      <span className="customer-name">{transaction.customer}</span>
                    </div>
                  </td>
                  <td>
                    <span className="transaction-amount font-mono">
                      R$ {transaction.amount.toLocaleString('pt-BR', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2
                      })}
                    </span>
                  </td>
                  <td>
                    <span className={`payment-method method-${transaction.method}`}>
                      {methodLabels[transaction.method]}
                    </span>
                  </td>
                  <td>
                    <StatusBadge status={transaction.status} />
                  </td>
                  <td>
                    <div className="table-actions">
                      <button
                        className="action-button"
                        onClick={() => setSelectedTransaction(
                          selectedTransaction === transaction.id ? null : transaction.id
                        )}
                      >
                        <MoreVertical size={18} />
                      </button>

                      {/* Dropdown Menu */}
                      <AnimatePresence>
                        {selectedTransaction === transaction.id && (
                          <>
                            <div
                              className="action-overlay"
                              onClick={() => setSelectedTransaction(null)}
                            />
                            <motion.div
                              initial={{ opacity: 0, scale: 0.95 }}
                              animate={{ opacity: 1, scale: 1 }}
                              exit={{ opacity: 0, scale: 0.95 }}
                              className="action-dropdown"
                            >
                              <button className="action-dropdown-item">
                                <Eye size={16} />
                                Ver detalhes
                              </button>
                              <button className="action-dropdown-item">
                                <Copy size={16} />
                                Copiar link
                              </button>
                              <button className="action-dropdown-item">
                                <Download size={16} />
                                Baixar recibo
                              </button>
                              {transaction.status === 'completed' && (
                                <button className="action-dropdown-item danger">
                                  <RefreshCw size={16} />
                                  Solicitar reembolso
                                </button>
                              )}
                            </motion.div>
                          </>
                        )}
                      </AnimatePresence>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </AnimatePresence>
          </tbody>
        </table>

        {/* Empty State */}
        {paginatedTransactions.length === 0 && (
          <div className="table-empty">
            <p>Nenhuma transação encontrada</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {!limit && totalPages > 1 && (
        <div className="table-pagination">
          <span className="pagination-info">
            Mostrando {startIndex + 1} a {Math.min(endIndex, filteredTransactions.length)} de {filteredTransactions.length}
          </span>

          <div className="pagination-controls">
            <button
              className="btn btn-ghost btn-sm"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage(currentPage - 1)}
            >
              <ChevronLeft size={18} />
              Anterior
            </button>

            <div className="pagination-pages">
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i + 1;
                } else if (currentPage <= 3) {
                  pageNum = i + 1;
                } else if (currentPage >= totalPages - 2) {
                  pageNum = totalPages - 4 + i;
                } else {
                  pageNum = currentPage - 2 + i;
                }

                return (
                  <button
                    key={pageNum}
                    className={`pagination-page ${currentPage === pageNum ? 'active' : ''}`}
                    onClick={() => setCurrentPage(pageNum)}
                  >
                    {pageNum}
                  </button>
                );
              })}
            </div>

            <button
              className="btn btn-ghost btn-sm"
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage(currentPage + 1)}
            >
              Próxima
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
