/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Component: Input
 *
 * Accessible form input with label and error states
 * Based on Zendpag Visual Identity Guidelines
 */

import React, { InputHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/utils/cn';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /**
   * Input label text
   */
  label?: string;
  /**
   * Error message to display
   */
  error?: string;
  /**
   * Helper text to display below input
   */
  helperText?: string;
  /**
   * Icon to display before input
   */
  leftIcon?: React.ReactNode;
  /**
   * Icon to display after input
   */
  rightIcon?: React.ReactNode;
}

/**
 * Input Component
 *
 * @example
 * // Basic input with label
 * <Input
 *   label="Email"
 *   type="email"
 *   placeholder="seu@email.com"
 * />
 *
 * @example
 * // Input with error state
 * <Input
 *   label="CPF"
 *   error="CPF inválido"
 *   value={cpf}
 * />
 *
 * @example
 * // Input with icons
 * <Input
 *   label="Buscar"
 *   leftIcon={<SearchIcon />}
 *   placeholder="Buscar transações..."
 * />
 */
const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      type = 'text',
      label,
      error,
      helperText,
      leftIcon,
      rightIcon,
      id,
      disabled,
      ...props
    },
    ref
  ) => {
    // Generate unique ID if not provided
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;
    const hasError = !!error;

    return (
      <div className="w-full">
        {/* Label */}
        {label && (
          <label
            htmlFor={inputId}
            className={cn(
              'block mb-2 text-sm font-medium font-inter',
              hasError ? 'text-error' : 'text-dark-neutral',
              disabled && 'opacity-50 cursor-not-allowed'
            )}
          >
            {label}
          </label>
        )}

        {/* Input Container */}
        <div className="relative">
          {/* Left Icon */}
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-medium">
              {leftIcon}
            </div>
          )}

          {/* Input Element */}
          <input
            ref={ref}
            id={inputId}
            type={type}
            disabled={disabled}
            className={cn(
              // Base styles
              'w-full',
              'h-[40px]',
              'px-4',
              'py-3',
              'bg-white',
              'border',
              'rounded-sm',
              'text-base',
              'font-inter',
              'text-dark-neutral',
              'placeholder:text-gray-medium',
              'transition-all',
              'duration-base',

              // Focus state
              'focus:outline-none',
              'focus:ring-3',
              'focus:ring-primary/20',
              'focus:border-primary',

              // Error state
              hasError && [
                'border-error',
                'focus:ring-error/20',
                'focus:border-error',
              ],

              // Normal state
              !hasError && 'border-gray-300',

              // Disabled state
              disabled && [
                'opacity-50',
                'cursor-not-allowed',
                'bg-gray-light',
              ],

              // Icon padding adjustments
              leftIcon && 'pl-10',
              rightIcon && 'pr-10',

              className
            )}
            {...props}
          />

          {/* Right Icon */}
          {rightIcon && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-medium">
              {rightIcon}
            </div>
          )}
        </div>

        {/* Error Message */}
        {error && (
          <p className="mt-2 text-sm text-error font-inter flex items-center gap-1">
            <svg
              className="w-4 h-4"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                clipRule="evenodd"
              />
            </svg>
            {error}
          </p>
        )}

        {/* Helper Text */}
        {helperText && !error && (
          <p className="mt-2 text-sm text-gray-medium font-inter">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export { Input };
