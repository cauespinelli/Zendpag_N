/**
 * Zendapag Ant Design Theme Configuration
 * Aplica design system enterprise nas cores do Ant Design
 */

import type { ThemeConfig } from 'antd';

export const zendapagTheme: ThemeConfig = {
  token: {
    // CORES PRIMÁRIAS
    colorPrimary: '#4F46E5',      // zp-primary-600
    colorSuccess: '#10B981',      // zp-success-500
    colorWarning: '#F59E0B',      // zp-accent-500
    colorError: '#EF4444',        // zp-error-500
    colorInfo: '#3B82F6',         // zp-info-500

    // CORES DE TEXTO
    colorText: '#334155',         // zp-neutral-700
    colorTextSecondary: '#64748B', // zp-neutral-500
    colorTextTertiary: '#94A3B8',  // zp-neutral-400
    colorTextQuaternary: '#CBD5E1', // zp-neutral-300

    // BACKGROUNDS
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#F8FAFC',     // zp-neutral-50
    colorBgSpotlight: '#F1F5F9',  // zp-neutral-100

    // BORDERS
    colorBorder: '#E2E8F0',       // zp-neutral-200
    colorBorderSecondary: '#F1F5F9', // zp-neutral-100

    // TIPOGRAFIA
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    fontSize: 14,
    fontSizeHeading1: 36,
    fontSizeHeading2: 30,
    fontSizeHeading3: 24,
    fontSizeHeading4: 20,
    fontSizeHeading5: 16,

    // BORDER RADIUS
    borderRadius: 8,              // zp-radius-lg
    borderRadiusLG: 12,           // zp-radius-xl
    borderRadiusSM: 6,            // zp-radius-md
    borderRadiusXS: 4,            // zp-radius-sm

    // ESPAÇAMENTO
    controlHeight: 40,
    controlHeightLG: 48,
    controlHeightSM: 32,

    // SOMBRAS
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
    boxShadowSecondary: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',

    // LINE HEIGHT
    lineHeight: 1.5,
    lineHeightHeading1: 1.2,
    lineHeightHeading2: 1.2,
    lineHeightHeading3: 1.375,

    // MOTION
    motionDurationFast: '0.15s',
    motionDurationMid: '0.25s',
    motionDurationSlow: '0.35s',
  },

  components: {
    // BUTTON
    Button: {
      primaryShadow: '0 10px 25px -5px rgba(99, 102, 241, 0.3)',
      controlHeight: 40,
      controlHeightLG: 48,
      fontWeight: 600,
      borderRadius: 8,
    },

    // CARD
    Card: {
      borderRadius: 12,
      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
    },

    // TABLE
    Table: {
      borderRadius: 12,
      headerBg: '#F8FAFC',
      headerColor: '#334155',
      headerSplitColor: '#E2E8F0',
    },

    // INPUT
    Input: {
      controlHeight: 40,
      borderRadius: 8,
      activeBorderColor: '#4F46E5',
      hoverBorderColor: '#6366F1',
    },

    // SELECT
    Select: {
      controlHeight: 40,
      borderRadius: 8,
    },

    // MENU
    Menu: {
      itemBorderRadius: 6,
      itemSelectedBg: '#EEF2FF',      // zp-primary-50
      itemSelectedColor: '#4338CA',    // zp-primary-700
      itemHoverBg: '#F8FAFC',          // zp-neutral-50
      itemHoverColor: '#4F46E5',       // zp-primary-600
    },

    // BADGE
    Badge: {
      statusSize: 8,
    },

    // TAG
    Tag: {
      borderRadiusSM: 4,
    },

    // MODAL
    Modal: {
      borderRadiusLG: 16,
      boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
    },

    // NOTIFICATION
    Notification: {
      borderRadiusLG: 12,
    },

    // MESSAGE
    Message: {
      borderRadiusLG: 8,
    },
  },
};

// Dark theme configuration
export const zendapagDarkTheme: ThemeConfig = {
  ...zendapagTheme,
  token: {
    ...zendapagTheme.token,
    colorBgContainer: '#1E293B',
    colorBgElevated: '#334155',
    colorBgLayout: '#0F172A',
    colorText: '#F1F5F9',
    colorTextSecondary: '#94A3B8',
    colorBorder: '#334155',
  },
};

export default zendapagTheme;
