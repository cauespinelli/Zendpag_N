// @ts-nocheck
import React, { useState } from 'react';
import { Button, Tooltip, message } from 'antd';
import { CopyOutlined, CheckOutlined } from '@ant-design/icons';

interface CopyButtonProps {
  text: string;
  tooltip?: string;
  successMessage?: string;
  size?: 'small' | 'middle' | 'large';
  type?: 'text' | 'link' | 'default' | 'primary' | 'dashed';
}

const CopyButton: React.FC<CopyButtonProps> = ({
  text,
  tooltip = 'Copiar',
  successMessage = 'Copiado!',
  size = 'small',
  type = 'text',
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
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
    <Tooltip title={copied ? 'Copiado!' : tooltip}>
      <Button
        type={type}
        size={size}
        icon={copied ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
        onClick={handleCopy}
      />
    </Tooltip>
  );
};

export default CopyButton;
