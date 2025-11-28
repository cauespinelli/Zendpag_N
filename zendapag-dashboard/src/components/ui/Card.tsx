/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Component: Card
 *
 * Flexible card container with multiple variants
 * Based on Zendpag Visual Identity Guidelines
 */

import React, { HTMLAttributes, forwardRef } from 'react';
import { cn } from '@/utils/cn';

/* ============================================
   CARD CONTAINER
   ============================================ */

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * Card variant
   * - default: Standard white card with border
   * - feature: Premium card with gradient background
   */
  variant?: 'default' | 'feature';
  /**
   * Enable hover effect (lift animation)
   */
  hoverable?: boolean;
}

/**
 * Card - Main container component
 *
 * @example
 * <Card variant="default">
 *   <CardHeader>
 *     <CardTitle>Dashboard</CardTitle>
 *   </CardHeader>
 *   <CardContent>
 *     Content here
 *   </CardContent>
 * </Card>
 */
const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'default', hoverable = false, children, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          // Base styles
          'transition-all duration-base',

          // Default variant - Standard card
          variant === 'default' && [
            'bg-white',
            'border',
            'border-gray-200',
            'rounded-md',
            'p-6',
            'shadow-sm',
          ],

          // Feature variant - Premium gradient card
          variant === 'feature' && [
            'bg-gradient-dark',
            'text-white',
            'border-2',
            'border-cyan',
            'rounded-lg',
            'p-8',
            'shadow-md',
          ],

          // Hover effect
          hoverable && [
            'hover:shadow-lg',
            'hover:-translate-y-1',
            'cursor-pointer',
          ],

          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

/* ============================================
   CARD HEADER
   ============================================ */

export interface CardHeaderProps extends HTMLAttributes<HTMLDivElement> {}

/**
 * CardHeader - Header section of the card
 */
const CardHeader = forwardRef<HTMLDivElement, CardHeaderProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('mb-4 space-y-1', className)}
      {...props}
    />
  )
);

CardHeader.displayName = 'CardHeader';

/* ============================================
   CARD TITLE
   ============================================ */

export interface CardTitleProps extends HTMLAttributes<HTMLHeadingElement> {}

/**
 * CardTitle - Title heading in card header
 */
const CardTitle = forwardRef<HTMLHeadingElement, CardTitleProps>(
  ({ className, ...props }, ref) => (
    <h3
      ref={ref}
      className={cn(
        'text-xl font-bold font-inter leading-tight',
        className
      )}
      {...props}
    />
  )
);

CardTitle.displayName = 'CardTitle';

/* ============================================
   CARD DESCRIPTION
   ============================================ */

export interface CardDescriptionProps extends HTMLAttributes<HTMLParagraphElement> {}

/**
 * CardDescription - Subtitle/description in card header
 */
const CardDescription = forwardRef<HTMLParagraphElement, CardDescriptionProps>(
  ({ className, ...props }, ref) => (
    <p
      ref={ref}
      className={cn(
        'text-sm text-gray-medium font-inter leading-normal',
        className
      )}
      {...props}
    />
  )
);

CardDescription.displayName = 'CardDescription';

/* ============================================
   CARD CONTENT
   ============================================ */

export interface CardContentProps extends HTMLAttributes<HTMLDivElement> {}

/**
 * CardContent - Main content area of the card
 */
const CardContent = forwardRef<HTMLDivElement, CardContentProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('', className)}
      {...props}
    />
  )
);

CardContent.displayName = 'CardContent';

/* ============================================
   CARD FOOTER
   ============================================ */

export interface CardFooterProps extends HTMLAttributes<HTMLDivElement> {}

/**
 * CardFooter - Footer section of the card
 */
const CardFooter = forwardRef<HTMLDivElement, CardFooterProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('mt-4 flex items-center gap-4', className)}
      {...props}
    />
  )
);

CardFooter.displayName = 'CardFooter';

/* ============================================
   EXPORTS
   ============================================ */

export {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
};
