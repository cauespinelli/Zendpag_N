import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { CreditCard, Lock, ArrowLeft, AlertCircle } from 'lucide-react';

interface CardPaymentProps {
  type: 'credit' | 'debit';
  amount: number;
  onSubmit: (data: any) => void;
  onBack: () => void;
}

interface FormData {
  cardNumber: string;
  cardName: string;
  expiryDate: string;
  cvv: string;
  installments: number;
}

interface FormErrors {
  cardNumber?: string;
  cardName?: string;
  expiryDate?: string;
  cvv?: string;
}

export default function CardPayment({ type, amount, onSubmit, onBack }: CardPaymentProps) {
  const [formData, setFormData] = useState<FormData>({
    cardNumber: '',
    cardName: '',
    expiryDate: '',
    cvv: '',
    installments: 1
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [processing, setProcessing] = useState(false);

  // Formatar número do cartão
  const formatCardNumber = (value: string) => {
    return value
      .replace(/\s/g, '')
      .replace(/(\d{4})/g, '$1 ')
      .trim()
      .substring(0, 19);
  };

  // Formatar data de validade
  const formatExpiryDate = (value: string) => {
    return value
      .replace(/\D/g, '')
      .replace(/(\d{2})(\d)/, '$1/$2')
      .substring(0, 5);
  };

  // Detectar bandeira do cartão
  const detectCardBrand = (number: string) => {
    const cleaned = number.replace(/\s/g, '');
    if (/^4/.test(cleaned)) return 'visa';
    if (/^5[1-5]/.test(cleaned)) return 'mastercard';
    if (/^3[47]/.test(cleaned)) return 'amex';
    if (/^6(?:011|5)/.test(cleaned)) return 'discover';
    if (/^3(?:0[0-5]|[68])/.test(cleaned)) return 'diners';
    if (/^35/.test(cleaned)) return 'jcb';
    return null;
  };

  const handleInputChange = (field: keyof FormData, value: string | number) => {
    let formattedValue = value;

    if (field === 'cardNumber' && typeof value === 'string') {
      formattedValue = formatCardNumber(value);
    } else if (field === 'expiryDate' && typeof value === 'string') {
      formattedValue = formatExpiryDate(value);
    } else if (field === 'cvv' && typeof value === 'string') {
      formattedValue = value.replace(/\D/g, '').substring(0, 4);
    } else if (field === 'cardName' && typeof value === 'string') {
      formattedValue = value.toUpperCase();
    }

    setFormData(prev => ({ ...prev, [field]: formattedValue }));

    // Limpar erro do campo
    if (errors[field as keyof FormErrors]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Validar número do cartão
    const cardNumber = formData.cardNumber.replace(/\s/g, '');
    if (!cardNumber) {
      newErrors.cardNumber = 'Número do cartão é obrigatório';
    } else if (cardNumber.length < 13 || cardNumber.length > 19) {
      newErrors.cardNumber = 'Número do cartão inválido';
    }

    // Validar nome
    if (!formData.cardName.trim()) {
      newErrors.cardName = 'Nome do titular é obrigatório';
    } else if (formData.cardName.trim().length < 3) {
      newErrors.cardName = 'Nome muito curto';
    }

    // Validar validade
    if (!formData.expiryDate) {
      newErrors.expiryDate = 'Data de validade é obrigatória';
    } else {
      const [month, year] = formData.expiryDate.split('/');
      const currentDate = new Date();
      const currentYear = currentDate.getFullYear() % 100;
      const currentMonth = currentDate.getMonth() + 1;

      if (parseInt(month) < 1 || parseInt(month) > 12) {
        newErrors.expiryDate = 'Mês inválido';
      } else if (parseInt(year) < currentYear ||
                 (parseInt(year) === currentYear && parseInt(month) < currentMonth)) {
        newErrors.expiryDate = 'Cartão vencido';
      }
    }

    // Validar CVV
    if (!formData.cvv) {
      newErrors.cvv = 'CVV é obrigatório';
    } else if (formData.cvv.length < 3) {
      newErrors.cvv = 'CVV inválido';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;

    setProcessing(true);

    // Simular processamento
    await new Promise(resolve => setTimeout(resolve, 2500));

    onSubmit({
      method: type,
      cardBrand: detectCardBrand(formData.cardNumber),
      lastFourDigits: formData.cardNumber.slice(-4),
      installments: formData.installments
    });
  };

  const installmentOptions = Array.from({ length: 12 }, (_, i) => {
    const installment = i + 1;
    const installmentAmount = amount / installment;
    return {
      value: installment,
      label: `${installment}x de R$ ${installmentAmount.toLocaleString('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })} ${installment === 1 ? '' : 'sem juros'}`
    };
  });

  const cardBrand = detectCardBrand(formData.cardNumber);

  return (
    <div className="card-payment">
      <button className="back-button" onClick={onBack}>
        <ArrowLeft size={20} />
        Voltar
      </button>

      <div className="card-header">
        <div className="card-icon">
          <CreditCard size={32} />
        </div>
        <h2 className="card-title">
          {type === 'credit' ? 'Cartão de Crédito' : 'Cartão de Débito'}
        </h2>
        <p className="card-subtitle">
          Preencha os dados do seu cartão
        </p>
      </div>

      {/* Card Preview */}
      <motion.div
        className="card-preview"
        initial={{ rotateY: 0 }}
        animate={{ rotateY: 0 }}
      >
        <div className="card-preview-bg">
          <div className="card-preview-chip"></div>
          <div className="card-preview-number">
            {formData.cardNumber || '•••• •••• •••• ••••'}
          </div>
          <div className="card-preview-footer">
            <div className="card-preview-name">
              {formData.cardName || 'NOME DO TITULAR'}
            </div>
            <div className="card-preview-expiry">
              {formData.expiryDate || 'MM/AA'}
            </div>
          </div>
          {cardBrand && (
            <div className="card-preview-brand">
              {cardBrand.toUpperCase()}
            </div>
          )}
        </div>
      </motion.div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="card-form">
        {/* Card Number */}
        <div className="form-group">
          <label className="form-label">Número do cartão</label>
          <div className="form-input-wrapper">
            <input
              type="text"
              className={`form-input ${errors.cardNumber ? 'error' : ''}`}
              placeholder="0000 0000 0000 0000"
              value={formData.cardNumber}
              onChange={(e) => handleInputChange('cardNumber', e.target.value)}
              maxLength={19}
            />
            <CreditCard className="input-icon" size={20} />
          </div>
          {errors.cardNumber && (
            <span className="form-error">
              <AlertCircle size={14} />
              {errors.cardNumber}
            </span>
          )}
        </div>

        {/* Card Name */}
        <div className="form-group">
          <label className="form-label">Nome no cartão</label>
          <input
            type="text"
            className={`form-input ${errors.cardName ? 'error' : ''}`}
            placeholder="NOME COMO NO CARTÃO"
            value={formData.cardName}
            onChange={(e) => handleInputChange('cardName', e.target.value)}
          />
          {errors.cardName && (
            <span className="form-error">
              <AlertCircle size={14} />
              {errors.cardName}
            </span>
          )}
        </div>

        {/* Expiry & CVV */}
        <div className="form-row">
          <div className="form-group">
            <label className="form-label">Validade</label>
            <input
              type="text"
              className={`form-input ${errors.expiryDate ? 'error' : ''}`}
              placeholder="MM/AA"
              value={formData.expiryDate}
              onChange={(e) => handleInputChange('expiryDate', e.target.value)}
              maxLength={5}
            />
            {errors.expiryDate && (
              <span className="form-error">
                <AlertCircle size={14} />
                {errors.expiryDate}
              </span>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">CVV</label>
            <input
              type="text"
              className={`form-input ${errors.cvv ? 'error' : ''}`}
              placeholder="123"
              value={formData.cvv}
              onChange={(e) => handleInputChange('cvv', e.target.value)}
              maxLength={4}
            />
            {errors.cvv && (
              <span className="form-error">
                <AlertCircle size={14} />
                {errors.cvv}
              </span>
            )}
          </div>
        </div>

        {/* Installments (only for credit) */}
        {type === 'credit' && (
          <div className="form-group">
            <label className="form-label">Parcelas</label>
            <select
              className="form-select"
              value={formData.installments}
              onChange={(e) => handleInputChange('installments', parseInt(e.target.value))}
            >
              {installmentOptions.map(option => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Submit Button */}
        <button
          type="submit"
          className="btn btn-primary btn-lg w-full"
          disabled={processing}
        >
          {processing ? (
            <>
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
              >
                <Lock size={20} />
              </motion.div>
              Processando pagamento...
            </>
          ) : (
            <>
              <Lock size={20} />
              Pagar R$ {amount.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}
            </>
          )}
        </button>

        {/* Security Note */}
        <div className="security-note">
          <Lock size={16} />
          <span>Seus dados estão protegidos com criptografia SSL</span>
        </div>
      </form>
    </div>
  );
}
