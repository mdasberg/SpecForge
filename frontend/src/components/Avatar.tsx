import type { ReactNode } from 'react';

export type ActorKind = 'HUMAN' | 'AGENT';

export interface AvatarProps {
  name: string;
  /** The stylesheet defines one size modifier, `.av-lg`; there is deliberately no small or medium. */
  large?: boolean;
  actorKind?: ActorKind;
}

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? '')
    .join('');
}

/**
 * Violet with a dashed square is reserved for agent output so it is never mistaken for a human's
 * avatar; `.av-bot` (not a per-person `.av-*` colour) carries that styling.
 */
export function Avatar({ name, large = false, actorKind = 'HUMAN' }: AvatarProps) {
  const classes = ['av'];
  if (actorKind === 'AGENT') {
    classes.push('av-bot');
  }
  if (large) {
    classes.push('av-lg');
  }
  return <span className={classes.join(' ')}>{initials(name)}</span>;
}

export interface AvatarStackProps {
  children: ReactNode;
}

export function AvatarStack({ children }: AvatarStackProps) {
  return <div className="avstack">{children}</div>;
}
