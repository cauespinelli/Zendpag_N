/**
 * ZENDPAG DESIGN SYSTEM v1.0
 * Utility: Class Name Merger
 *
 * Combines clsx and tailwind-merge for optimal class name handling
 */

import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Merges Tailwind CSS classes efficiently
 *
 * @param inputs - Class names to merge
 * @returns Merged class string
 *
 * @example
 * cn('px-2 py-1', 'px-4') // Returns: 'py-1 px-4'
 * cn('bg-primary', condition && 'bg-secondary') // Conditional classes
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
