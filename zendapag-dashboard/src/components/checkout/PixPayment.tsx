import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Copy, Check, Clock, ArrowLeft, Smartphone, Shield } from 'lucide-react';
import QRCode from 'qrcode';

interface PixPaymentProps {
  amount: number;
  onSuccess: (data: any) => void;
  onBack: () => void;
}

export default function PixPayment({ amount, onSuccess, onBack }: PixPaymentProps) {
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  const [pixCode, setPixCode] = useState('');
  const [copied, setCopied] = useState(false);
  const [timeLeft, setTimeLeft] = useState(600); // 10 minutos
  const [checking, setChecking] = useState(false);

  // Gerar QR Code e chave PIX
  useEffect(() => {
    const generatePix = async () => {
      // Simular geração de chave PIX
      const mockPixKey = `00020126580014br.gov.bcb.pix0136${Math.random().toString(36).substr(2, 36)}520400005303986540${amount.toFixed(2)}5802BR5913Zendpag Ltda6009Sao Paulo62070503***6304${Math.random().toString(36).substr(2, 4).toUpperCase()}`;
      setPixCode(mockPixKey);

      // Gerar QR Code
      try {
        const url = await QRCode.toDataURL(mockPixKey, {
          width: 300,
          margin: 2,
          color: {
            dark: '#0A2540',
            light: '#FFFFFF'
          }
        });
        setQrCodeUrl(url);
      } catch (err) {
        console.error('Erro ao gerar QR Code:', err);
      }
    };

    generatePix();
  }, [amount]);

  // Timer countdown
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  // Simular checagem de pagamento
  useEffect(() => {
    if (checking) {
      const checkPayment = setTimeout(() => {
        // Simular pagamento aprovado após 3 segundos
        onSuccess({ method: 'pix', pixKey: pixCode });
      }, 3000);

      return () => clearTimeout(checkPayment);
    }
  }, [checking, pixCode, onSuccess]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const copyPixCode = () => {
    navigator.clipboard.writeText(pixCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCheckPayment = () => {
    setChecking(true);
  };

  return (
    <div className="pix-payment">
      <button className="back-button" onClick={onBack}>
        <ArrowLeft size={20} />
        Voltar
      </button>

      <div className="pix-header">
        <div className="pix-icon">
          <Smartphone size={32} />
        </div>
        <h2 className="pix-title">Pagar com PIX</h2>
        <p className="pix-subtitle">
          Escaneie o QR Code ou copie o código para pagar
        </p>
      </div>

      {/* Timer */}
      <motion.div
        className="pix-timer"
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
      >
        <Clock size={20} />
        <span>Código expira em {formatTime(timeLeft)}</span>
      </motion.div>

      {/* QR Code */}
      <motion.div
        className="qr-code-container"
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        {qrCodeUrl ? (
          <img src={qrCodeUrl} alt="QR Code PIX" className="qr-code" />
        ) : (
          <div className="qr-code-loading">Gerando QR Code...</div>
        )}
      </motion.div>

      {/* PIX Code */}
      <div className="pix-code-section">
        <label className="pix-code-label">Código PIX (Copia e Cola)</label>
        <div className="pix-code-wrapper">
          <input
            type="text"
            value={pixCode}
            readOnly
            className="pix-code-input"
          />
          <button
            className={`copy-button ${copied ? 'copied' : ''}`}
            onClick={copyPixCode}
          >
            {copied ? (
              <>
                <Check size={18} />
                Copiado!
              </>
            ) : (
              <>
                <Copy size={18} />
                Copiar
              </>
            )}
          </button>
        </div>
      </div>

      {/* Instructions */}
      <div className="pix-instructions">
        <h3>Como pagar:</h3>
        <ol>
          <li>Abra o app do seu banco</li>
          <li>Escolha pagar com PIX</li>
          <li>Escaneie o QR Code ou cole o código</li>
          <li>Confirme o pagamento</li>
        </ol>
      </div>

      {/* Check Payment Button */}
      <button
        className="btn btn-primary btn-lg w-full"
        onClick={handleCheckPayment}
        disabled={checking}
      >
        {checking ? (
          <>
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
            >
              <Clock size={20} />
            </motion.div>
            Verificando pagamento...
          </>
        ) : (
          'Já fiz o pagamento'
        )}
      </button>

      {/* Security Note */}
      <div className="security-note">
        <Shield size={16} />
        <span>Pagamento seguro e criptografado</span>
      </div>
    </div>
  );
}
