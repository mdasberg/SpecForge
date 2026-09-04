import type { SpecStatus } from '../api/catalog';
import { specStatusLabel } from '../lib/specStatus';
import { Badge } from './Badge';
import type { BadgeVariant } from './Badge';

const VARIANT: Record<SpecStatus, BadgeVariant> = {
  DRAFT: 'draft',
  IN_REVIEW: 'review',
  CHANGES_REQUESTED: 'changes',
  APPROVED: 'approved',
  IMPLEMENTED: 'impl',
};

/** The one place a `SpecStatus` maps to a badge colour, shared by every screen that shows one. */
export function StatusBadge({ status }: { status: SpecStatus }) {
  return <Badge variant={VARIANT[status]}>{specStatusLabel(status)}</Badge>;
}
