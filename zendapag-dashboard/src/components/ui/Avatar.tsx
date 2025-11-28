/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Component: Avatar
 *
 * User avatar with fallback initials
 * Based on Zendpag Visual Identity Guidelines
 */

import React, { HTMLAttributes, forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/utils/cn';

const avatarVariants = cva(
  [
    'inline-flex',
    'items-center',
    'justify-center',
    'rounded-full',
    'overflow-hidden',
    'bg-gradient-primary',
    'text-white',
    'font-inter',
    'font-semibold',
    'select-none',
  ].join(' '),
  {
    variants: {
      size: {
        sm: 'w-8 h-8 text-xs',
        md: 'w-10 h-10 text-sm',
        lg: 'w-14 h-14 text-base',
        xl: 'w-20 h-20 text-xl',
      },
    },
    defaultVariants: {
      size: 'md',
    },
  }
);

export interface AvatarProps
  extends HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof avatarVariants> {
  /**
   * Image source URL
   */
  src?: string;
  /**
   * Alt text for image
   */
  alt?: string;
  /**
   * Fallback initials (e.g., "JD" for John Doe)
   */
  fallback?: string;
}

/**
 * Avatar Component
 *
 * @example
 * // With image
 * <Avatar src="/user.jpg" alt="John Doe" />
 *
 * @example
 * // With fallback initials
 * <Avatar fallback="JD" size="lg" />
 */
const Avatar = forwardRef<HTMLDivElement, AvatarProps>(
  ({ className, size, src, alt, fallback, ...props }, ref) => {
    const [imageError, setImageError] = React.useState(false);

    const showFallback = !src || imageError;

    return (
      <div
        ref={ref}
        className={cn(avatarVariants({ size }), className)}
        {...props}
      >
        {!showFallback ? (
          <img
            src={src}
            alt={alt || 'Avatar'}
            className="w-full h-full object-cover"
            onError={() => setImageError(true)}
          />
        ) : (
          <span>{fallback || '?'}</span>
        )}
      </div>
    );
  }
);

Avatar.displayName = 'Avatar';

export { Avatar, avatarVariants };
