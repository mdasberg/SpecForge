import type { ReactNode } from 'react';

export interface EmptyStateProps {
  title: string;
  children: ReactNode;
}

/**
 * Every screen in the skeleton is empty, and an empty screen that says nothing looks broken. Each
 * one names connecting a repository as the next step, which is the flow `add-spec-repository`
 * delivers.
 */
export function EmptyState({ title, children }: EmptyStateProps) {
  return (
    <div className="card card-pad">
      <div className="card-t">{title}</div>
      <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{children}</p>
    </div>
  );
}
