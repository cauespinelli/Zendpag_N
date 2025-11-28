/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Component: Badge
 *
 * Status indicators and labels with semantic colors
 * Based on Zendpag Visual Identity Guidelines
 */

import React, { HTMLAttributes, forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/utils/cn';

const badgeVariants = cva(
  [
    'inline-flex',
    'items-center',
    'gap-1',
    'px-3',
    'py-1',
    'rounded-full',
    'text-xs',
    'font-semibold',
    'font-inter',
    'transition-all',
    'duration-fast',
  ].join(' '),
  {
    variants: {
      variant: {
        success: [
          'bg-success-light',
          'text-success',
          'border',
          'border-success',
        ].join(' '),
        error: [
          'bg-error-light',
          'text-error',
          'border',
          'border-error',
        ].join(' '),
        warning: [
          'bg-warning-light',
          'text-warning',
          'border',
          'border-warning',
        ].join(' '),
        info: [
          'bg-info-light',
          'text-info',
          'border',
          'border-info',
        ].join(' '),
        default: [
          'bg-gray-light',
          'text-gray-medium',
          'border',
          'border-gray-300',
        ].join(' '),
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  /**
   * Optional icon to display before text
   */
  icon?: React.ReactNode;
}

/**
 * Badge Component
 *
 * @example
 * <Badge variant="success">Aprovado</Badge>
 * <Badge variant="error">Recusado</Badge>
 * <Badge variant="warning">Pendente</Badge>
 */
const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant, icon, children, ...props }, ref) => {
    return (
      <span
        ref={ref}
        className={cn(badgeVariants({ variant }), className)}
        {...props}
      >
        {icon && <span className="inline-flex w-3 h-3">{icon}</span>}
        {children}
      </span>
    );
  }
);

Badge.displayName = 'Badge';

export { Badge, badgeVariants };
