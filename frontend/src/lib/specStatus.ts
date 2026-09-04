import type { SpecStatus } from '../api/catalog';

export const SPEC_STATUSES: SpecStatus[] = ['DRAFT', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'IMPLEMENTED'];

const LABEL: Record<SpecStatus, string> = {
  DRAFT: 'Draft',
  IN_REVIEW: 'In review',
  CHANGES_REQUESTED: 'Changes requested',
  APPROVED: 'Approved',
  IMPLEMENTED: 'Implemented',
};

export function specStatusLabel(status: SpecStatus): string {
  return LABEL[status];
}
