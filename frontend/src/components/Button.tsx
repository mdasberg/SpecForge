import type { ButtonHTMLAttributes } from 'react';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost' | 'warn';
  small?: boolean;
}

/** Plain `.btn` with no variant is the neutral action; primary/ghost/warn map to the `.btn-*` modifiers. */
export function Button({ variant, small, className, ...rest }: ButtonProps) {
  const classes = ['btn'];
  if (variant) classes.push(`btn-${variant}`);
  if (small) classes.push('btn-sm');
  if (className) classes.push(className);
  return <button className={classes.join(' ')} {...rest} />;
}
