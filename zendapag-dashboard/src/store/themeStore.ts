// @ts-nocheck
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';
import { theme } from 'antd';
import { STORAGE_KEYS } from '@/utils/constants';

interface ThemeState {
  darkMode: boolean;
  primaryColor: string;
  sidebarCollapsed: boolean;
  language: 'pt-BR' | 'en-US';
  compactMode: boolean;
  colorWeakMode: boolean;
  borderRadius: number;
  fontSize: 'small' | 'default' | 'large';
}

interface ThemeActions {
  toggleDarkMode: () => void;
  setPrimaryColor: (color: string) => void;
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setLanguage: (language: 'pt-BR' | 'en-US') => void;
  toggleCompactMode: () => void;
  toggleColorWeakMode: () => void;
  setBorderRadius: (radius: number) => void;
  setFontSize: (size: 'small' | 'default' | 'large') => void;
  resetTheme: () => void;
}

type ThemeStore = ThemeState & ThemeActions;

const defaultState: ThemeState = {
  darkMode: true,
  primaryColor: '#1890ff',
  sidebarCollapsed: false,
  language: 'pt-BR',
  compactMode: false,
  colorWeakMode: false,
  borderRadius: 6,
  fontSize: 'default',
};

export const useThemeStore = create<ThemeStore>()(
  persist(
    immer((set, get) => ({
      ...defaultState,

      toggleDarkMode: () => {
        set((state) => {
          state.darkMode = !state.darkMode;
        });
      },

      setPrimaryColor: (color: string) => {
        set((state) => {
          state.primaryColor = color;
        });
      },

      toggleSidebar: () => {
        set((state) => {
          state.sidebarCollapsed = !state.sidebarCollapsed;
        });
      },

      setSidebarCollapsed: (collapsed: boolean) => {
        set((state) => {
          state.sidebarCollapsed = collapsed;
        });
      },

      setLanguage: (language: 'pt-BR' | 'en-US') => {
        set((state) => {
          state.language = language;
        });
      },

      toggleCompactMode: () => {
        set((state) => {
          state.compactMode = !state.compactMode;
        });
      },

      toggleColorWeakMode: () => {
        set((state) => {
          state.colorWeakMode = !state.colorWeakMode;
        });
      },

      setBorderRadius: (radius: number) => {
        set((state) => {
          state.borderRadius = Math.max(0, Math.min(20, radius));
        });
      },

      setFontSize: (fontSize: 'small' | 'default' | 'large') => {
        set((state) => {
          state.fontSize = fontSize;
        });
      },

      resetTheme: () => {
        set(() => ({ ...defaultState }));
      },
    })),
    {
      name: STORAGE_KEYS.THEME_CONFIG,
    }
  )
);

// Theme configuration generator
export const generateThemeConfig = (themeState: ThemeState) => {
  const { darkMode, primaryColor, compactMode, borderRadius, fontSize } = themeState;

  const fontSizeMap = {
    small: 12,
    default: 14,
    large: 16,
  };

  return {
    algorithm: darkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      colorPrimary: primaryColor,
      borderRadius,
      fontSize: fontSizeMap[fontSize],
      sizeStep: compactMode ? 3 : 4,
      sizeUnit: compactMode ? 3 : 4,
      // Custom tokens
      colorBgContainer: darkMode ? '#141414' : '#ffffff',
      colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
      colorBgLayout: darkMode ? '#000000' : '#f5f5f5',
      colorBorder: darkMode ? '#434343' : '#d9d9d9',
      colorBorderSecondary: darkMode ? '#303030' : '#f0f0f0',
      colorText: darkMode ? '#ffffff' : '#000000d9',
      colorTextSecondary: darkMode ? '#ffffffa6' : '#00000073',
      colorTextTertiary: darkMode ? '#ffffff40' : '#00000040',
      colorTextQuaternary: darkMode ? '#ffffff26' : '#00000026',
      colorFill: darkMode ? '#ffffff1a' : '#0000000f',
      colorFillSecondary: darkMode ? '#ffffff12' : '#00000006',
      colorFillTertiary: darkMode ? '#ffffff08' : '#00000004',
      colorFillQuaternary: darkMode ? '#ffffff04' : '#00000002',
    },
    components: {
      Layout: {
        colorBgHeader: darkMode ? '#001529' : '#ffffff',
        colorBgBody: darkMode ? '#000000' : '#f5f5f5',
        colorBgTrigger: darkMode ? '#1f1f1f' : '#ffffff',
      },
      Menu: {
        colorBgContainer: darkMode ? '#001529' : '#ffffff',
        colorItemText: darkMode ? '#ffffffa6' : '#000000d9',
        colorItemTextSelected: primaryColor,
        colorItemBg: 'transparent',
        colorItemBgSelected: darkMode ? '#1890ff1a' : '#e6f7ff',
        colorItemBgHover: darkMode ? '#ffffff12' : '#f5f5f5',
      },
      Card: {
        colorBgContainer: darkMode ? '#141414' : '#ffffff',
        colorBorderSecondary: darkMode ? '#303030' : '#f0f0f0',
      },
      Table: {
        colorBgContainer: darkMode ? '#141414' : '#ffffff',
        colorFillAlter: darkMode ? '#1f1f1f' : '#fafafa',
      },
      Input: {
        colorBgContainer: darkMode ? '#141414' : '#ffffff',
        colorBorder: darkMode ? '#434343' : '#d9d9d9',
        colorBgContainerDisabled: darkMode ? '#262626' : '#f5f5f5',
      },
      Select: {
        colorBgContainer: darkMode ? '#141414' : '#ffffff',
        colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
      },
      DatePicker: {
        colorBgContainer: darkMode ? '#141414' : '#ffffff',
        colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
      },
      Modal: {
        colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
        colorBgMask: 'rgba(0, 0, 0, 0.45)',
      },
      Drawer: {
        colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
      },
      Popover: {
        colorBgElevated: darkMode ? '#1f1f1f' : '#ffffff',
      },
      Tooltip: {
        colorBgSpotlight: darkMode ? '#1f1f1f' : '#ffffff',
      },
    },
  };
};

// Custom CSS variables generator
export const generateCSSVariables = (themeState: ThemeState) => {
  const { darkMode, primaryColor } = themeState;

  return {
    '--primary-color': primaryColor,
    '--background-color': darkMode ? '#000000' : '#f5f5f5',
    '--surface-color': darkMode ? '#141414' : '#ffffff',
    '--border-color': darkMode ? '#434343' : '#d9d9d9',
    '--text-color': darkMode ? '#ffffff' : '#000000d9',
    '--text-secondary': darkMode ? '#ffffffa6' : '#00000073',
    '--shadow-color': darkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)',
  };
};

// Theme selectors
export const themeSelectors = {
  useDarkMode: () => useThemeStore((state) => state.darkMode),
  usePrimaryColor: () => useThemeStore((state) => state.primaryColor),
  useSidebarCollapsed: () => useThemeStore((state) => state.sidebarCollapsed),
  useLanguage: () => useThemeStore((state) => state.language),
  useCompactMode: () => useThemeStore((state) => state.compactMode),
  useColorWeakMode: () => useThemeStore((state) => state.colorWeakMode),
  useBorderRadius: () => useThemeStore((state) => state.borderRadius),
  useFontSize: () => useThemeStore((state) => state.fontSize),
  useThemeConfig: () => useThemeStore((state) => generateThemeConfig(state)),
  useCSSVariables: () => useThemeStore((state) => generateCSSVariables(state)),
};

// Predefined color schemes
export const colorSchemes = [
  { name: 'Azul Padrão', color: '#1890ff' },
  { name: 'Verde', color: '#52c41a' },
  { name: 'Roxo', color: '#722ed1' },
  { name: 'Rosa', color: '#eb2f96' },
  { name: 'Laranja', color: '#fa8c16' },
  { name: 'Vermelho', color: '#ff4d4f' },
  { name: 'Ciano', color: '#13c2c2' },
  { name: 'Amarelo', color: '#fadb14' },
];

export default useThemeStore;