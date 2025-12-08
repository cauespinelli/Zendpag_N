/**
 * ZENDPAG DESIGN SYSTEM - C6 BANK STYLE
 * Component: Card
 *
 * Premium dark card container with C6 Bank aesthetics
 */

import React, { HTMLAttributes, forwardRef } from 'react';
import { cn } from '@/utils/cn';

/* ============================================
   CARD CONTAINER
   ============================================ */

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * Card variant
   * - default: Standard dark card with subtle border
   * - elevated: Card with shadow
   * - gold: Premium card with gold accent
   */
  variant?: 'default' | 'elevated' | 'gold';
  /**
   * Enable hover effect (border highlight)
   */
  hoverable?: boolean;
  /**
   * Remove padding (for custom content)
   */
  noPadding?: boolean;
}

/**
 * Card - Main container component (C6 Bank Style)
 */
const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'default', hoverable = false, noPadding = false, children, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          // Base styles - C6 Bank dark theme
          'bg-[#1A1A1A] border border-[#2D2D2D] rounded-2xl transition-all duration-200',

          // Padding
          !noPadding && 'p-6',

          // Elevated variant
          variant === 'elevated' && 'shadow-lg shadow-black/20',

          // Gold variant - premium look
          variant === 'gold' && 'border-[#C9A962]/30',

          // Hover effect
          hoverable && 'hover:border-[#3D3D3D] cursor-pointer',

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
      className={cn('mb-6 space-y-1', className)}
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
        'text-white text-lg font-semibold leading-tight',
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
        'text-sm text-[#5C5C5C] leading-normal',
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
      className={cn('mt-6 pt-6 border-t border-[#2D2D2D] flex items-center gap-4', className)}
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
