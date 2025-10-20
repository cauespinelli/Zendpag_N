// @ts-nocheck
import React from 'react';
import { createRoot } from 'react-dom/client';
import dayjs from 'dayjs';
import 'dayjs/locale/pt-br';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import advancedFormat from 'dayjs/plugin/advancedFormat';
import weekday from 'dayjs/plugin/weekday';
import localeData from 'dayjs/plugin/localeData';
import weekOfYear from 'dayjs/plugin/weekOfYear';
import weekYear from 'dayjs/plugin/weekYear';
import quarterOfYear from 'dayjs/plugin/quarterOfYear';

import App from './App';
import './index.css';

// Configure dayjs plugins
dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(relativeTime);
dayjs.extend(duration);
dayjs.extend(customParseFormat);
dayjs.extend(advancedFormat);
dayjs.extend(weekday);
dayjs.extend(localeData);
dayjs.extend(weekOfYear);
dayjs.extend(weekYear);
dayjs.extend(quarterOfYear);

// Set default locale
dayjs.locale('pt-br');

// Set default timezone to Sao Paulo
dayjs.tz.setDefault('America/Sao_Paulo');

// Error handling for unhandled promises
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason);

  // Don't prevent default in development for debugging
  if (process.env.NODE_ENV === 'production') {
    event.preventDefault();
  }
});

// Error handling for global errors
window.addEventListener('error', (event) => {
  console.error('Global error:', event.error);

  // Report to error monitoring service in production
  if (process.env.NODE_ENV === 'production') {
    // TODO: Integrate with error monitoring service (Sentry, LogRocket, etc.)
  }
});

// Performance monitoring
if (process.env.NODE_ENV === 'development') {
  // Log performance metrics
  window.addEventListener('load', () => {
    setTimeout(() => {
      const perfData = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;

      console.log('Performance metrics:', {
        'DNS Lookup': perfData.domainLookupEnd - perfData.domainLookupStart,
        'TCP Connection': perfData.connectEnd - perfData.connectStart,
        'Request': perfData.responseStart - perfData.requestStart,
        'Response': perfData.responseEnd - perfData.responseStart,
        'DOM Content Loaded': perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart,
        'Load Complete': perfData.loadEventEnd - perfData.loadEventStart,
        'Total': perfData.loadEventEnd - perfData.navigationStart,
      });
    }, 0);
  });
}

// Environment validation
if (!process.env.REACT_APP_API_URL) {
  console.error('REACT_APP_API_URL environment variable is not set');
}

// Browser compatibility checks
const checkBrowserSupport = () => {
  const isSupported = {
    fetch: typeof fetch !== 'undefined',
    localStorage: typeof localStorage !== 'undefined',
    sessionStorage: typeof sessionStorage !== 'undefined',
    webStorage: typeof Storage !== 'undefined',
    promise: typeof Promise !== 'undefined',
    es6: typeof Symbol !== 'undefined',
  };

  const unsupported = Object.entries(isSupported)
    .filter(([_, supported]) => !supported)
    .map(([feature, _]) => feature);

  if (unsupported.length > 0) {
    console.warn('Browser missing features:', unsupported);

    // Show warning to user for critical features
    if (!isSupported.localStorage || !isSupported.fetch) {
      alert('Seu navegador não suporta recursos necessários para esta aplicação. Por favor, atualize seu navegador.');
    }
  }
};

checkBrowserSupport();

// Initialize app
const container = document.getElementById('root');
if (!container) {
  throw new Error('Root element not found');
}

const root = createRoot(container);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// Service worker registration for PWA features
if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production') {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js')
      .then((registration) => {
        console.log('SW registered: ', registration);
      })
      .catch((registrationError) => {
        console.log('SW registration failed: ', registrationError);
      });
  });
}

// Hot module replacement in development
if (process.env.NODE_ENV === 'development' && module.hot) {
  module.hot.accept('./App', () => {
    const NextApp = require('./App').default;
    root.render(
      <React.StrictMode>
        <NextApp />
      </React.StrictMode>
    );
  });
}