// @ts-nocheck
import React from 'react';
import { Spin, Typography } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface LoadingScreenProps {
  tip?: string;
  size?: 'small' | 'default' | 'large';
  fullScreen?: boolean;
}

const LoadingScreen: React.FC<LoadingScreenProps> = ({
  tip = 'Carregando...',
  size = 'large',
  fullScreen = true
}) => {
  const customIcon = <LoadingOutlined style={{ fontSize: 48, color: 'var(--primary-color)' }} spin />;

  const loadingContent = (
    <div className="loading-content" style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 16
    }}>
      <div className="logo-container" style={{ marginBottom: 24 }}>
        <svg
          width="64"
          height="64"
          viewBox="0 0 64 64"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <circle
            cx="32"
            cy="32"
            r="30"
            fill="var(--primary-color)"
            opacity="0.1"
          />
          <path
            d="M20 24L32 36L44 24"
            stroke="var(--primary-color)"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M20 40L32 28L44 40"
            stroke="var(--primary-color)"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </div>

      <Spin indicator={customIcon} size={size} />

      <div className="loading-text" style={{ textAlign: 'center' }}>
        <Text style={{ fontSize: 16, color: 'var(--text-secondary)' }}>
          {tip}
        </Text>
      </div>

      <div className="loading-dots" style={{
        display: 'flex',
        gap: 4,
        marginTop: 8
      }}>
        <div className="dot" style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: 'var(--primary-color)',
          opacity: 0.3,
          animation: 'loadingDots 1.5s infinite ease-in-out'
        }} />
        <div className="dot" style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: 'var(--primary-color)',
          opacity: 0.3,
          animation: 'loadingDots 1.5s infinite ease-in-out 0.2s'
        }} />
        <div className="dot" style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: 'var(--primary-color)',
          opacity: 0.3,
          animation: 'loadingDots 1.5s infinite ease-in-out 0.4s'
        }} />
      </div>

      <style>{`
        @keyframes loadingDots {
          0%, 80%, 100% {
            opacity: 0.3;
            transform: scale(1);
          }
          40% {
            opacity: 1;
            transform: scale(1.2);
          }
        }

        .loading-content .logo-container svg {
          animation: logoFloat 3s ease-in-out infinite;
        }

        @keyframes logoFloat {
          0%, 100% {
            transform: translateY(0px);
          }
          50% {
            transform: translateY(-10px);
          }
        }
      `}</style>
    </div>
  );

  if (fullScreen) {
    return (
      <div className="loading-full-screen" style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--background-color)',
        zIndex: 'var(--z-loading)',
        backdropFilter: 'blur(2px)'
      }}>
        {loadingContent}
      </div>
    );
  }

  return (
    <div className="loading-container" style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 200,
      padding: 40
    }}>
      {loadingContent}
    </div>
  );
};

export default LoadingScreen;