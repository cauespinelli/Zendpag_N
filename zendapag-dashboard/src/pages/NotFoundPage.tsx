// @ts-nocheck
import React from 'react';
import { Result, Button } from 'antd';
import { HomeOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const NotFoundPage: React.FC = () => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/dashboard');
  };

  const handleGoBack = () => {
    navigate(-1);
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--background-color)',
      padding: '20px'
    }}>
      <Result
        status="404"
        title="404"
        subTitle="Desculpe, a página que você está procurando não existe."
        extra={[
          <Button type="primary" key="home" icon={<HomeOutlined />} onClick={handleGoHome}>
            Ir para Dashboard
          </Button>,
          <Button key="back" icon={<ArrowLeftOutlined />} onClick={handleGoBack}>
            Voltar
          </Button>
        ]}
      />
    </div>
  );
};

export default NotFoundPage;