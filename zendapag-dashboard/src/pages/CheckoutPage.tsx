import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Check,
  Lock,
  Shield
} from 'lucide-react';
import PaymentMethodSelector from '../components/checkout/PaymentMethodSelector';
import PixPayment from '../components/checkout/PixPayment';
import CardPayment from '../components/checkout/CardPayment';
import BoletoPayment from '../components/checkout/BoletoPayment';
import OrderSummary from '../components/checkout/OrderSummary';
import PaymentSuccess from '../components/checkout/PaymentSuccess';

type PaymentMethod = 'pix' | 'credit' | 'debit' | 'boleto';
type CheckoutStep = 'method' | 'payment' | 'success';

export default function CheckoutPage() {
  const [currentStep, setCurrentStep] = useState<CheckoutStep>('method');
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod | null>(null);
  const [transactionId, setTransactionId] = useState<string>('');

  // Dados do pedido (mock)
  const orderData = {
    items: [
      { name: 'Curso Avançado de React', price: 497.00, quantity: 1 },
      { name: 'Mentoria Individual (2h)', price: 299.00, quantity: 1 }
    ],
    subtotal: 796.00,
    discount: 79.60,
    total: 716.40,
    customer: {
      name: 'Kleber Gobbi',
      email: 'kleber@zendpag.com',
      document: '123.456.789-00'
    }
  };

  const handleMethodSelect = (method: PaymentMethod) => {
    setSelectedMethod(method);
    setCurrentStep('payment');
  };

  const handlePaymentSubmit = async (paymentData: any) => {
    // Simular processamento
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Gerar ID de transação
    const txId = `TRX-${Date.now()}-${Math.random().toString(36).substr(2, 9).toUpperCase()}`;
    setTransactionId(txId);
    setCurrentStep('success');
  };

  const handleBackToMethod = () => {
    setCurrentStep('method');
    setSelectedMethod(null);
  };

  const renderPaymentForm = () => {
    if (!selectedMethod) return null;

    switch (selectedMethod) {
      case 'pix':
        return (
          <PixPayment
            amount={orderData.total}
            onSuccess={handlePaymentSubmit}
            onBack={handleBackToMethod}
          />
        );
      case 'credit':
      case 'debit':
        return (
          <CardPayment
            type={selectedMethod}
            amount={orderData.total}
            onSubmit={handlePaymentSubmit}
            onBack={handleBackToMethod}
          />
        );
      case 'boleto':
        return (
          <BoletoPayment
            amount={orderData.total}
            customer={orderData.customer}
            onGenerate={handlePaymentSubmit}
            onBack={handleBackToMethod}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="checkout-page">
      {/* Header */}
      <header className="checkout-header">
        <div className="container">
          <div className="checkout-header-content">
            <a href="/" className="checkout-logo">
              <div className="logo-symbol">
                <span>Z</span>
              </div>
              <span className="logo-text">Zendpag</span>
            </a>

            {/* Progress Steps */}
            {currentStep !== 'success' && (
              <div className="checkout-steps">
                <div className={`checkout-step ${currentStep === 'method' ? 'active' : 'completed'}`}>
                  <div className="step-number">
                    {currentStep === 'payment' ? <Check size={16} /> : '1'}
                  </div>
                  <span className="step-label">Método</span>
                </div>
                <div className="step-divider" />
                <div className={`checkout-step ${currentStep === 'payment' ? 'active' : ''}`}>
                  <div className="step-number">2</div>
                  <span className="step-label">Pagamento</span>
                </div>
              </div>
            )}

            {/* Security Badge */}
            <div className="security-badge">
              <Lock size={16} />
              <span>Pagamento Seguro</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="checkout-main">
        <div className="container">
          <div className="checkout-grid">
            {/* Left Column - Payment Form */}
            <div className="checkout-content">
              <AnimatePresence mode="wait">
                {currentStep === 'method' && (
                  <motion.div
                    key="method"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                  >
                    <h1 className="checkout-title">Escolha o método de pagamento</h1>
                    <p className="checkout-subtitle">
                      Selecione como você deseja pagar
                    </p>
                    <PaymentMethodSelector onSelect={handleMethodSelect} />
                  </motion.div>
                )}

                {currentStep === 'payment' && (
                  <motion.div
                    key="payment"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                  >
                    {renderPaymentForm()}
                  </motion.div>
                )}

                {currentStep === 'success' && (
                  <motion.div
                    key="success"
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                  >
                    <PaymentSuccess
                      transactionId={transactionId}
                      amount={orderData.total}
                      method={selectedMethod!}
                    />
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Right Column - Order Summary */}
            {currentStep !== 'success' && (
              <aside className="checkout-sidebar">
                <OrderSummary data={orderData} />
              </aside>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="checkout-footer">
        <div className="container">
          <div className="checkout-footer-content">
            <div className="security-badges">
              <div className="security-item">
                <Shield size={18} />
                <span>PCI-DSS Certificado</span>
              </div>
              <div className="security-item">
                <Lock size={18} />
                <span>SSL 256-bit</span>
              </div>
            </div>
            <p className="copyright">© 2025 Zendpag. Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
