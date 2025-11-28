/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Tailwind CSS Configuration
 *
 * Custom theme aligned with Zendpag Visual Identity
 */

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./public/index.html"
  ],
  theme: {
    extend: {
      /* ============================================
         COLORS - Zendpag Brand Palette
         ============================================ */
      colors: {
        primary: {
          DEFAULT: '#0066FF',
          hover: '#0052CC',
          active: '#0043A8',
          light: 'rgba(0, 102, 255, 0.1)',
        },
        cyan: {
          DEFAULT: '#06B6D4',
          hover: '#0891B2',
          active: '#0E7490',
          light: 'rgba(6, 182, 212, 0.1)',
        },
        dark: {
          primary: '#0A2540',
          neutral: '#1A1F36',
        },
        gray: {
          light: '#F7F9FC',
          medium: '#6B7280',
          info: '#6B72D0',
        },
        white: {
          clean: '#FFFFFF',
        },
        success: {
          DEFAULT: '#10B981',
          light: 'rgba(16, 185, 129, 0.1)',
        },
        error: {
          DEFAULT: '#EF4444',
          light: 'rgba(239, 68, 68, 0.1)',
        },
        warning: {
          DEFAULT: '#F59E0B',
          light: 'rgba(245, 158, 11, 0.1)',
        },
        info: {
          DEFAULT: '#3B82F6',
          light: 'rgba(59, 130, 246, 0.1)',
        },
      },

      /* ============================================
         TYPOGRAPHY - Font System
         ============================================ */
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Monaco', 'Courier New', 'monospace'],
      },
      fontSize: {
        'xs': ['12px', { lineHeight: '1.5' }],
        'sm': ['14px', { lineHeight: '1.5' }],
        'base': ['16px', { lineHeight: '1.5' }],
        'md': ['18px', { lineHeight: '1.5' }],
        'lg': ['20px', { lineHeight: '1.5' }],
        'xl': ['24px', { lineHeight: '1.2' }],
        '2xl': ['32px', { lineHeight: '1.2' }],
        '3xl': ['40px', { lineHeight: '1.2' }],
        '4xl': ['48px', { lineHeight: '1.2' }],
      },
      fontWeight: {
        thin: '100',
        light: '300',
        regular: '400',
        medium: '500',
        semibold: '600',
        bold: '700',
        extrabold: '800',
      },

      /* ============================================
         SPACING - 8pt Grid System
         ============================================ */
      spacing: {
        '0': '0',
        '1': '4px',
        '2': '8px',
        '3': '12px',
        '4': '16px',
        '5': '20px',
        '6': '24px',
        '8': '32px',
        '10': '40px',
        '12': '48px',
        '16': '64px',
        '20': '80px',
        '24': '96px',
        '32': '128px',
        // Semantic spacing
        'xs': '8px',
        'sm': '16px',
        'md': '24px',
        'lg': '32px',
        'xl': '48px',
        '2xl': '64px',
        '3xl': '96px',
      },

      /* ============================================
         BORDER RADIUS
         ============================================ */
      borderRadius: {
        'none': '0',
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '24px',
        'full': '9999px',
      },

      /* ============================================
         SHADOWS - Elevation System
         ============================================ */
      boxShadow: {
        'xs': '0 1px 2px rgba(0, 0, 0, 0.05)',
        'sm': '0 1px 3px rgba(0, 0, 0, 0.1)',
        'md': '0 4px 12px rgba(0, 102, 255, 0.24)',
        'lg': '0 8px 24px rgba(0, 0, 0, 0.15)',
        'xl': '0 12px 40px rgba(0, 0, 0, 0.2)',
        '2xl': '0 20px 60px rgba(0, 0, 0, 0.25)',
        'primary': '0 4px 12px rgba(0, 102, 255, 0.3)',
        'cyan': '0 4px 12px rgba(6, 182, 212, 0.3)',
        'success': '0 4px 12px rgba(16, 185, 129, 0.3)',
        'error': '0 4px 12px rgba(239, 68, 68, 0.3)',
      },

      /* ============================================
         TRANSITIONS - Animation Timing
         ============================================ */
      transitionDuration: {
        'fastest': '100ms',
        'fast': '200ms',
        'base': '300ms',
        'slow': '400ms',
        'slowest': '500ms',
      },
      transitionTimingFunction: {
        'ease-in': 'cubic-bezier(0.4, 0, 1, 1)',
        'ease-out': 'cubic-bezier(0, 0, 0.2, 1)',
        'ease-in-out': 'cubic-bezier(0.4, 0, 0.2, 1)',
        'bounce': 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      },

      /* ============================================
         BREAKPOINTS - Responsive Design
         ============================================ */
      screens: {
        'xs': '320px',
        'sm': '640px',
        'md': '768px',
        'lg': '1024px',
        'xl': '1280px',
        '2xl': '1440px',
      },

      /* ============================================
         Z-INDEX - Stacking Order
         ============================================ */
      zIndex: {
        'base': '0',
        'dropdown': '100',
        'sticky': '200',
        'fixed': '300',
        'modal-backdrop': '400',
        'modal': '500',
        'popover': '600',
        'tooltip': '700',
        'toast': '800',
        'maximum': '999',
      },

      /* ============================================
         CONTAINER - Max Widths
         ============================================ */
      maxWidth: {
        'container-sm': '640px',
        'container-md': '768px',
        'container-lg': '1024px',
        'container-xl': '1280px',
        'container-2xl': '1536px',
      },

      /* ============================================
         GRADIENTS - Brand Gradients
         ============================================ */
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #0066FF 0%, #06B6D4 100%)',
        'gradient-dark': 'linear-gradient(135deg, #0A2540 0%, #1A1F36 100%)',
        'gradient-feature': 'linear-gradient(135deg, #0A2540 0%, rgba(6, 182, 212, 0.2) 100%)',
        'gradient-hero': 'linear-gradient(135deg, #0A2540 0%, #0066FF 50%, #06B6D4 100%)',
      },

      /* ============================================
         OPACITY
         ============================================ */
      opacity: {
        '0': '0',
        '10': '0.1',
        '20': '0.2',
        '30': '0.3',
        '40': '0.4',
        '50': '0.5',
        '60': '0.6',
        '70': '0.7',
        '80': '0.8',
        '90': '0.9',
        '100': '1',
      },
    },
  },
  plugins: [],
  // Enable dark mode via class
  darkMode: 'class',
}
