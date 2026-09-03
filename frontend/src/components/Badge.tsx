import type { ReactNode } from 'react';

export type BadgeVariant = 'draft' | 'review' | 'changes' | 'approved' | 'impl' | 'blocked' | 'agent';

export interface BadgeProps {
  children: ReactNode;
  variant: BadgeVariant;
}

/** Status chip for spec/review lifecycle states, styled by the `.b-*` classes in components.css. */
export function Badge({ children, variant }: BadgeProps) {
  return <span className={`badge b-${variant}`}>{children}</span>;
}
