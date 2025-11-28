import React from 'react';
import { motion } from 'framer-motion';
import { Smartphone, CreditCard, FileText, Zap, Check } from 'lucide-react';

type PaymentMethod = 'pix' | 'credit' | 'debit' | 'boleto';

interface PaymentMethodSelectorProps {
  onSelect: (method: PaymentMethod) => void;
}

const methods = [
  {
    id: 'pix' as PaymentMethod,
    name: 'PIX',
    description: 'Pagamento instantâneo',
    icon: Smartphone,
    badge: 'Recomendado',
    color: 'success',
    features: ['Aprovação instantânea', 'Sem taxas adicionais', 'QR Code fácil']
  },
  {
    id: 'credit' as PaymentMethod,
    name: 'Cartão de Crédito',
    description: 'Parcele em até 12x',
    icon: CreditCard,
    badge: null,
    color: 'primary',
    features: ['Parcele sem juros', 'Aprovação em segundos', 'Todas as bandeiras']
  },
  {
    id: 'debit' as PaymentMethod,
    name: 'Cartão de Débito',
    description: 'Débito à vista',
    icon: CreditCard,
    badge: null,
    color: 'cyan',
    features: ['Débito imediato', 'Aprovação rápida', 'Seguro e confiável']
  },
  {
    id: 'boleto' as PaymentMethod,
    name: 'Boleto Bancário',
    description: 'Vencimento em 3 dias',
    icon: FileText,
    badge: null,
    color: 'warning',
    features: ['Pague em qualquer banco', 'Sem necessidade de cartão', 'Aprovação em 1-2 dias úteis']
  }
];

export default function PaymentMethodSelector({ onSelect }: PaymentMethodSelectorProps) {
  return (
    <div className="payment-methods">
      {methods.map((method, index) => {
        const Icon = method.icon;

        return (
          <motion.button
            key={method.id}
            className={`payment-method-card ${method.color}`}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            whileHover={{ y: -4, transition: { duration: 0.2 } }}
            onClick={() => onSelect(method.id)}
          >
            {method.badge && (
              <div className="method-badge">
                <Zap size={12} />
                {method.badge}
              </div>
            )}

            <div className="method-header">
              <div className={`method-icon icon-${method.color}`}>
                <Icon size={24} />
              </div>
              <div className="method-info">
                <h3 className="method-name">{method.name}</h3>
                <p className="method-description">{method.description}</p>
              </div>
            </div>

            <ul className="method-features">
              {method.features.map((feature, i) => (
                <li key={i}>
                  <Check size={16} />
                  <span>{feature}</span>
                </li>
              ))}
            </ul>

            <div className="method-footer">
              <span>Selecionar →</span>
            </div>
          </motion.button>
        );
      })}
    </div>
  );
}
