/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Component: Button
 *
 * Modern, accessible button component with multiple variants and sizes
 * Based on Zendpag Visual Identity Guidelines
 */

import React, { ButtonHTMLAttributes, forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/utils/cn';

/**
 * Button Variants Configuration
 * Following Zendpag color system and interaction patterns
 */
const buttonVariants = cva(
  // Base styles - applied to all buttons
  [
    'inline-flex',
    'items-center',
    'justify-center',
    'gap-2',
    'font-inter',
    'font-semibold',
    'transition-all',
    'duration-base',
    'disabled:opacity-50',
    'disabled:cursor-not-allowed',
    'disabled:hover:transform-none',
    'focus:outline-none',
    'focus:ring-3',
    'focus:ring-primary/20',
    'focus:ring-offset-2',
    'active:scale-[0.98]',
  ].join(' '),
  {
    variants: {
      /**
       * Visual variants
       * - primary: Main action button (#0066FF)
       * - secondary: Outlined button with border
       * - success: Confirmation actions (#10B981)
       * - ghost: Transparent with hover effect
       */
      variant: {
        primary: [
          'bg-primary',
          'text-white',
          'border-2',
          'border-primary',
          'hover:bg-primary-hover',
          'hover:border-primary-hover',
          'hover:shadow-primary',
          'hover:-translate-y-0.5',
          'shadow-sm',
        ].join(' '),
        secondary: [
          'bg-transparent',
          'text-primary',
          'border-2',
          'border-primary',
          'hover:bg-primary',
          'hover:text-white',
          'hover:shadow-md',
          'hover:-translate-y-0.5',
        ].join(' '),
        success: [
          'bg-success',
          'text-white',
          'border-2',
          'border-success',
          'hover:opacity-90',
          'hover:shadow-success',
          'hover:-translate-y-0.5',
          'shadow-sm',
        ].join(' '),
        ghost: [
          'bg-transparent',
          'text-dark-neutral',
          'hover:bg-gray-light',
          'hover:text-primary',
        ].join(' '),
      },
      /**
       * Size variants
       * - sm: 32px height (12px 16px padding)
       * - md: 40px height (16px 24px padding)
       * - lg: 48px height (20px 32px padding)
       */
      size: {
        sm: [
          'h-[32px]',
          'px-4',
          'py-2',
          'text-sm',
          'rounded-sm',
        ].join(' '),
        md: [
          'h-[40px]',
          'px-6',
          'py-3',
          'text-base',
          'rounded-md',
        ].join(' '),
        lg: [
          'h-[48px]',
          'px-8',
          'py-4',
          'text-lg',
          'rounded-lg',
        ].join(' '),
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
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
 * Button Component
 *
 * @example
 * // Primary button
 * <Button variant="primary" size="md">Criar Conta</Button>
 *
 * @example
 * // Secondary with icon
 * <Button variant="secondary" leftIcon={<ArrowLeft />}>Voltar</Button>
 *
 * @example
 * // Loading state
 * <Button loading>Processando...</Button>
 */
const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant,
      size,
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
        className={cn(buttonVariants({ variant, size, className }))}
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

export { Button, buttonVariants };
