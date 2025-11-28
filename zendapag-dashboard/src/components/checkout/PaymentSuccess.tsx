import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle, Download, Home, Mail } from 'lucide-react';
import confetti from 'canvas-confetti';

interface PaymentSuccessProps {
  transactionId: string;
  amount: number;
  method: 'pix' | 'credit' | 'debit' | 'boleto';
}

const methodLabels = {
  pix: 'PIX',
  credit: 'Cartão de Crédito',
  debit: 'Cartão de Débito',
  boleto: 'Boleto Bancário'
};

export default function PaymentSuccess({ transactionId, amount, method }: PaymentSuccessProps) {
  const [emailSent, setEmailSent] = useState(false);

  useEffect(() => {
    // Confetti animation
    const duration = 3 * 1000;
    const animationEnd = Date.now() + duration;
    const defaults = { startVelocity: 30, spread: 360, ticks: 60, zIndex: 9999 };

    function randomInRange(min: number, max: number) {
      return Math.random() * (max - min) + min;
    }

    const interval: any = setInterval(function() {
      const timeLeft = animationEnd - Date.now();

      if (timeLeft <= 0) {
        return clearInterval(interval);
      }

      const particleCount = 50 * (timeLeft / duration);

      confetti({
        ...defaults,
        particleCount,
        origin: { x: randomInRange(0.1, 0.3), y: Math.random() - 0.2 }
      });
      confetti({
        ...defaults,
        particleCount,
        origin: { x: randomInRange(0.7, 0.9), y: Math.random() - 0.2 }
      });
    }, 250);

    // Simular envio de email
    setTimeout(() => setEmailSent(true), 2000);

    return () => clearInterval(interval);
  }, []);

  const downloadReceipt = () => {
    // Simular download
    const element = document.createElement('a');
    const receipt = `
      COMPROVANTE DE PAGAMENTO
      ========================

      ID da Transação: ${transactionId}
      Método: ${methodLabels[method]}
      Valor: R$ ${amount.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
      Data: ${new Date().toLocaleString('pt-BR')}

      Pagamento aprovado com sucesso!
    `;
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(receipt));
    element.setAttribute('download', `comprovante-${transactionId}.txt`);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  return (
    <div className="payment-success">
      {/* Success Animation */}
      <motion.div
        className="success-animation"
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{
          type: "spring",
          stiffness: 200,
          damping: 15,
          delay: 0.2
        }}
      >
        <div className="success-circle">
          <CheckCircle size={64} />
        </div>
      </motion.div>

      {/* Success Message */}
      <motion.div
        className="success-content"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
      >
        <h1 className="success-title">Pagamento Aprovado!</h1>
        <p className="success-subtitle">
          Sua compra foi processada com sucesso
        </p>
      </motion.div>

      {/* Transaction Details */}
      <motion.div
        className="transaction-details"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7 }}
      >
        <div className="detail-card">
          <div className="detail-row">
            <span className="detail-label">ID da Transação</span>
            <span className="detail-value font-mono">{transactionId}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Método de Pagamento</span>
            <span className="detail-value">{methodLabels[method]}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Valor Pago</span>
            <span className="detail-value font-mono">
              R$ {amount.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}
            </span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Data</span>
            <span className="detail-value">
              {new Date().toLocaleString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              })}
            </span>
          </div>
        </div>
      </motion.div>

      {/* Email Notification */}
      <motion.div
        className="email-notification"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.9 }}
      >
        <Mail size={20} />
        <span>
          {emailSent
            ? 'Comprovante enviado para seu email'
            : 'Enviando comprovante...'}
        </span>
      </motion.div>

      {/* Actions */}
      <motion.div
        className="success-actions"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.1 }}
      >
        <button className="btn btn-primary btn-lg" onClick={downloadReceipt}>
          <Download size={20} />
          Baixar Comprovante
        </button>
        <a href="/" className="btn btn-secondary btn-lg">
          <Home size={20} />
          Voltar ao Início
        </a>
      </motion.div>

      {/* Next Steps */}
      <motion.div
        className="next-steps"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.3 }}
      >
        <h3>Próximos passos:</h3>
        <ul>
          <li>✓ Você receberá um email com os detalhes da compra</li>
          <li>✓ O acesso ao produto será liberado em alguns minutos</li>
          <li>✓ Em caso de dúvidas, entre em contato com nosso suporte</li>
        </ul>
      </motion.div>
    </div>
  );
}
