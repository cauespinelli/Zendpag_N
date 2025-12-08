/**
 * ZENDPAG DESIGN SYSTEM - C6 BANK STYLE
 * Component: Button
 *
 * Premium button component with C6 Bank aesthetics
 */

import React, { ButtonHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/utils/cn';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /**
   * Button variant
   * - primary: Gold gradient button (main actions)
   * - secondary: Dark button with border
   * - ghost: Transparent button
   * - danger: Red button for destructive actions
   */
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  /**
   * Button size
   */
  size?: 'sm' | 'md' | 'lg';
  /**
   * Loading state - shows spinner and disables interaction
   */
  loading?: boolean;
  /**
   * Icon to display before button text
   */
  leftIcon?: React.ReactNode;
  /**
   * Icon to display after button text
   */
  rightIcon?: React.ReactNode;
}

/**
 * Button variants configuration (exported for external use)
 */
export const buttonVariants = {
  variant: {
    primary: 'bg-gradient-to-r from-[#C9A962] to-[#8B6914] text-black font-semibold hover:opacity-90',
    secondary: 'bg-[#1E1E1E] text-white border border-[#2D2D2D] hover:bg-[#2D2D2D] hover:border-[#3D3D3D]',
    ghost: 'bg-transparent text-[#8C8C8C] hover:text-white hover:bg-[#1A1A1A]',
    danger: 'bg-[#E53935]/10 text-[#E53935] border border-[#E53935]/20 hover:bg-[#E53935]/20',
  },
  size: {
    sm: 'px-4 py-2 text-xs rounded-lg',
    md: 'px-6 py-3 text-sm rounded-xl',
    lg: 'px-8 py-4 text-base rounded-xl',
  },
};

/**
 * Button Component (C6 Bank Style)
 */
const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      loading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        className={cn(
          // Base styles
          'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          'focus:outline-none focus:ring-2 focus:ring-[#C9A962]/50 focus:ring-offset-2 focus:ring-offset-[#0D0D0D]',

          // Variant styles
          buttonVariants.variant[variant],

          // Size styles
          buttonVariants.size[size],

          className
        )}
        disabled={isDisabled}
        {...props}
      >
        {loading && (
          <svg
            className="animate-spin h-4 w-4"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {!loading && leftIcon && <span className="inline-flex">{leftIcon}</span>}
        {children}
        {!loading && rightIcon && <span className="inline-flex">{rightIcon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';

export { Button };
