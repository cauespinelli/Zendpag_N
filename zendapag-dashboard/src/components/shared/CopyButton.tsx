// @ts-nocheck
import React, { useState } from 'react';
import { Button, Tooltip, message } from 'antd';
import { CopyOutlined, CheckOutlined } from '@ant-design/icons';

interface CopyButtonProps {
  text: string;
  size?: 'small' | 'middle' | 'large';
  type?: 'link' | 'text' | 'default' | 'primary' | 'dashed';
  tooltipText?: string;
  successMessage?: string;
  className?: string;
}

const CopyButton: React.FC<CopyButtonProps> = ({
  text,
  size = 'small',
  type = 'link',
  tooltipText = 'Copiar',
  successMessage = 'Copiado!',
  className,
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      message.success(successMessage);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      message.error('Erro ao copiar');
    }
  };

  return (
    <Tooltip title={copied ? 'Copiado!' : tooltipText}>
      <Button
        type={type}
        size={size}
        icon={copied ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
        onClick={handleCopy}
        className={className}
      />
    </Tooltip>
  );
};

export default CopyButton;
