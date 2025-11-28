import React from 'react';
import { ShoppingBag, Tag } from 'lucide-react';

interface OrderSummaryProps {
  data: {
    items: Array<{
      name: string;
      price: number;
      quantity: number;
    }>;
    subtotal: number;
    discount: number;
    total: number;
  };
}

export default function OrderSummary({ data }: OrderSummaryProps) {
  return (
    <div className="order-summary">
      <div className="order-summary-header">
        <ShoppingBag size={24} />
        <h2>Resumo do Pedido</h2>
      </div>

      {/* Items */}
      <div className="order-items">
        {data.items.map((item, index) => (
          <div key={index} className="order-item">
            <div className="item-info">
              <span className="item-name">{item.name}</span>
              <span className="item-quantity">Qtd: {item.quantity}</span>
            </div>
            <span className="item-price font-mono">
              R$ {item.price.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}
            </span>
          </div>
        ))}
      </div>

      {/* Totals */}
      <div className="order-totals">
        <div className="total-row">
          <span>Subtotal</span>
          <span className="font-mono">
            R$ {data.subtotal.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}
          </span>
        </div>

        {data.discount > 0 && (
          <div className="total-row discount">
            <span>
              <Tag size={16} />
              Desconto
            </span>
            <span className="font-mono">
              -R$ {data.discount.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}
            </span>
          </div>
        )}

        <div className="total-divider" />

        <div className="total-row total">
          <span>Total</span>
          <span className="font-mono">
            R$ {data.total.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}
          </span>
        </div>
      </div>

      {/* Security Badge */}
      <div className="order-security">
        <div className="security-badge-small">
          🔒 Pagamento 100% seguro
        </div>
        <div className="security-badges-grid">
          <span className="security-badge-item">SSL</span>
          <span className="security-badge-item">PCI-DSS</span>
          <span className="security-badge-item">3D Secure</span>
        </div>
      </div>
    </div>
  );
}
