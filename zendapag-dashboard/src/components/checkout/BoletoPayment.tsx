import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { FileText, ArrowLeft, Download, Copy, Check, Printer, Calendar } from 'lucide-react';

interface BoletoPaymentProps {
  amount: number;
  customer: {
    name: string;
    email: string;
    document: string;
  };
  onGenerate: (data: any) => void;
  onBack: () => void;
}

export default function BoletoPayment({ amount, customer, onGenerate, onBack }: BoletoPaymentProps) {
  const [generated, setGenerated] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  // Dados do boleto (mock)
  const boletoData = {
    barcode: '23793.38128 60000.123451 23456.789012 3 98760000012345',
    digitableLine: '23793381286000012345123456789012398760000012345',
    dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toLocaleDateString('pt-BR'),
    recipient: 'Zendpag Pagamentos Ltda',
    document: '12.345.678/0001-90',
    amount: amount
  };

  const handleGenerate = async () => {
    setGenerating(true);

    // Simular geração
    await new Promise(resolve => setTimeout(resolve, 1500));

    setGenerated(true);
    setGenerating(false);

    // Notificar parent após 2 segundos
    setTimeout(() => {
      onGenerate({
        method: 'boleto',
        barcode: boletoData.barcode,
        dueDate: boletoData.dueDate
      });
    }, 2000);
  };

  const copyBarcode = () => {
    navigator.clipboard.writeText(boletoData.digitableLine);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadBoleto = () => {
    // Simular download
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('Boleto Mock'));
    element.setAttribute('download', `boleto-${Date.now()}.pdf`);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const printBoleto = () => {
    window.print();
  };

  return (
    <div className="boleto-payment">
      <button className="back-button" onClick={onBack}>
        <ArrowLeft size={20} />
        Voltar
      </button>

      <div className="boleto-header">
        <div className="boleto-icon">
          <FileText size={32} />
        </div>
        <h2 className="boleto-title">Boleto Bancário</h2>
        <p className="boleto-subtitle">
          {!generated
            ? 'Gere seu boleto para pagamento'
            : 'Boleto gerado com sucesso'}
        </p>
      </div>

      {!generated ? (
        <>
          {/* Customer Info */}
          <div className="boleto-info-card">
            <h3 className="info-title">Dados do pagador</h3>
            <div className="info-row">
              <span className="info-label">Nome:</span>
              <span className="info-value">{customer.name}</span>
            </div>
            <div className="info-row">
              <span className="info-label">CPF/CNPJ:</span>
              <span className="info-value font-mono">{customer.document}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Email:</span>
              <span className="info-value">{customer.email}</span>
            </div>
          </div>

          {/* Payment Info */}
          <div className="boleto-info-card">
            <h3 className="info-title">Informações do pagamento</h3>
            <div className="info-row">
              <span className="info-label">Valor:</span>
              <span className="info-value font-mono">
                R$ {amount.toLocaleString('pt-BR', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2
                })}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">Vencimento:</span>
              <span className="info-value">{boletoData.dueDate}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Destinatário:</span>
              <span className="info-value">{boletoData.recipient}</span>
            </div>
          </div>

          {/* Instructions */}
          <div className="boleto-instructions">
            <h3>Instruções:</h3>
            <ul>
              <li>O boleto será enviado para seu email</li>
              <li>Prazo de pagamento: até {boletoData.dueDate}</li>
              <li>Compensação em 1-2 dias úteis após o pagamento</li>
              <li>Após o vencimento, não será possível pagar este boleto</li>
            </ul>
          </div>

          {/* Generate Button */}
          <button
            className="btn btn-primary btn-lg w-full"
            onClick={handleGenerate}
            disabled={generating}
          >
            {generating ? (
              <>
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                >
                  <FileText size={20} />
                </motion.div>
                Gerando boleto...
              </>
            ) : (
              <>
                <FileText size={20} />
                Gerar Boleto
              </>
            )}
          </button>
        </>
      ) : (
        <>
          {/* Boleto Generated */}
          <motion.div
            className="boleto-success"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
          >
            <div className="success-icon">
              <Check size={48} />
            </div>
            <h3>Boleto gerado com sucesso!</h3>
            <p>Enviamos uma cópia para {customer.email}</p>
          </motion.div>

          {/* Barcode */}
          <div className="barcode-section">
            <label className="barcode-label">Código de barras</label>
            <div className="barcode-display">
              <div className="barcode-lines">
                {Array.from({ length: 50 }).map((_, i) => (
                  <div
                    key={i}
                    className="barcode-line"
                    style={{
                      width: Math.random() > 0.5 ? '2px' : '4px',
                      height: i % 5 === 0 ? '60px' : '50px'
                    }}
                  />
                ))}
              </div>
              <div className="barcode-number font-mono">
                {boletoData.barcode}
              </div>
            </div>

            <div className="barcode-actions">
              <button
                className={`btn btn-secondary ${copied ? 'success' : ''}`}
                onClick={copyBarcode}
              >
                {copied ? (
                  <>
                    <Check size={18} />
                    Copiado!
                  </>
                ) : (
                  <>
                    <Copy size={18} />
                    Copiar código
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="boleto-quick-actions">
            <button className="quick-action-btn" onClick={downloadBoleto}>
              <Download size={20} />
              <span>Baixar PDF</span>
            </button>
            <button className="quick-action-btn" onClick={printBoleto}>
              <Printer size={20} />
              <span>Imprimir</span>
            </button>
            <button className="quick-action-btn">
              <Calendar size={20} />
              <span>Adicionar ao calendário</span>
            </button>
          </div>

          {/* Payment Instructions */}
          <div className="payment-instructions">
            <h3>Como pagar:</h3>
            <ol>
              <li>Acesse o app ou internet banking do seu banco</li>
              <li>Selecione a opção "Pagar boleto"</li>
              <li>Escaneie o código de barras ou digite o código</li>
              <li>Confirme o pagamento</li>
            </ol>
            <p className="payment-note">
              <strong>Importante:</strong> O pagamento será confirmado em 1-2 dias úteis
            </p>
          </div>
        </>
      )}
    </div>
  );
}
