import React from 'react';

interface LogoProps {
  collapsed?: boolean;
  variant?: 'full' | 'icon' | 'white';
  width?: number;
  height?: number;
}

const Logo: React.FC<LogoProps> = ({
  collapsed = false,
  variant = 'full',
  width,
  height,
}) => {
  // Icon only (for collapsed sidebar or small spaces)
  if (collapsed || variant === 'icon') {
    return (
      <svg
        width={width || 40}
        height={height || 40}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ display: 'block' }}
      >
        <defs>
          <linearGradient id="icon-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#6366F1" />
            <stop offset="100%" stopColor="#4F46E5" />
          </linearGradient>
        </defs>
        <rect width="48" height="48" rx="12" fill="url(#icon-gradient)" />
        <g fill="white">
          <path d="M14,16 h20 l-14,14 h14 v4 h-20 l14,-14 h-14 z" />
          <circle cx="36" cy="18" r="2.5" />
          <circle cx="36" cy="30" r="2.5" />
        </g>
      </svg>
    );
  }

  // White version (for dark backgrounds)
  if (variant === 'white') {
    return (
      <svg
        width={width || 180}
        height={height || 40}
        viewBox="0 0 180 40"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ display: 'block' }}
      >
        <rect x="2" y="2" width="36" height="36" rx="8" fill="white" fillOpacity="0.15" />
        <rect x="2" y="2" width="36" height="36" rx="8" stroke="white" strokeWidth="2" />
        <g fill="white">
          <path d="M13,14 h12 l-8,8 h8 v2.5 h-12 l8,-8 h-8 z" />
          <circle cx="28" cy="16" r="1.5" />
          <circle cx="28" cy="24" r="1.5" />
        </g>
        <text
          x="46"
          y="27"
          fontFamily="Space Grotesk, sans-serif"
          fontWeight="700"
          fontSize="18"
          letterSpacing="-0.02em"
          fill="white"
        >
          zendapag
        </text>
      </svg>
    );
  }

  // Full logo (default)
  return (
    <svg
      width={width || 180}
      height={height || 40}
      viewBox="0 0 180 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      style={{ display: 'block' }}
    >
      <defs>
        <linearGradient id="logo-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#6366F1" />
          <stop offset="100%" stopColor="#4F46E5" />
        </linearGradient>
      </defs>
      <rect x="2" y="2" width="36" height="36" rx="8" fill="url(#logo-gradient)" />
      <g fill="white">
        <path d="M13,14 h12 l-8,8 h8 v2.5 h-12 l8,-8 h-8 z" />
        <circle cx="28" cy="16" r="1.5" />
        <circle cx="28" cy="24" r="1.5" />
      </g>
      <text
        x="46"
        y="27"
        fontFamily="Space Grotesk, system-ui, sans-serif"
        fontWeight="700"
        fontSize="18"
        letterSpacing="-0.02em"
        fill="#1E293B"
      >
        zendapag
      </text>
    </svg>
  );
};

export default Logo;
