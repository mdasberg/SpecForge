import { describe, expect, it } from 'vitest';
import {
  hasActiveFilters,
  parseSpecsView,
  specsViewToParams,
  toggleFilterValue,
  viewToListParams,
  withSearchTerm,
} from './specsQuery';
import type { SpecsView } from './specsQuery';

describe('parseSpecsView', () => {
  it('restores the full view from a filtered URL, and round-trips through params', () => {
    const params = new URLSearchParams(
      '?groupBy=TEAM&status=CHANGES_REQUESTED&status=IN_REVIEW&domain=claims&tag=money&q=validation',
    );
    const view = parseSpecsView(params);

    expect(view).toEqual<SpecsView>({
      groupBy: 'TEAM',
      status: ['CHANGES_REQUESTED', 'IN_REVIEW'],
      owner: [],
      team: [],
      domain: ['claims'],
      tag: ['money'],
      q: 'validation',
    });

    // A copied URL must show the second reader the same view.
    const roundTripped = parseSpecsView(specsViewToParams(view));
    expect(roundTripped).toEqual(view);
  });

  it('defaults to PROJECT grouping, no filters and no active filters on an empty query', () => {
    const view = parseSpecsView(new URLSearchParams());

    expect(view).toEqual<SpecsView>({
      groupBy: 'PROJECT',
      status: [],
      owner: [],
      team: [],
      domain: [],
      tag: [],
      q: '',
    });
    expect(hasActiveFilters(view)).toBe(false);
  });

  it('drops unrecognised groupBy and status values instead of passing them through', () => {
    const view = parseSpecsView(new URLSearchParams('?groupBy=SIDEWAYS&status=BANANA'));

    expect(view.groupBy).toBe('PROJECT');
    expect(view.status).toEqual([]);
  });
});

describe('toggleFilterValue', () => {
  it('is idempotent in pairs and leaves everything else alone', () => {
    const original = 'groupBy=TEAM&q=validation&owner=alice&status=IN_REVIEW';
    const params = new URLSearchParams(original);

    const toggledOn = toggleFilterValue(params, 'status', 'APPROVED');
    // The other fields of the view are untouched by toggling one filter.
    const before = parseSpecsView(params);
    const after = parseSpecsView(toggledOn);
    expect(after.groupBy).toBe(before.groupBy);
    expect(after.q).toBe(before.q);
    expect(after.owner).toEqual(before.owner);
    expect(after.status).toEqual([...before.status, 'APPROVED']);

    const toggledOff = toggleFilterValue(toggledOn, 'status', 'APPROVED');
    expect(toggledOff.toString()).toBe(original);
  });
});

describe('withSearchTerm', () => {
  it('sets q without touching filters, and clears it for an empty string', () => {
    const params = new URLSearchParams('?groupBy=DOMAIN&status=APPROVED');

    const withQ = withSearchTerm(params, 'billing');
    expect(withQ.get('q')).toBe('billing');
    expect(withQ.getAll('status')).toEqual(['APPROVED']);
    expect(withQ.get('groupBy')).toBe('DOMAIN');

    const cleared = withSearchTerm(withQ, '');
    expect(cleared.has('q')).toBe(false);
    expect(cleared.getAll('status')).toEqual(['APPROVED']);
  });
});

describe('viewToListParams', () => {
  it('turns an empty q into undefined and passes limit/cursor through', () => {
    const view: SpecsView = { groupBy: 'PROJECT', status: [], owner: [], team: [], domain: [], tag: [], q: '' };

    expect(viewToListParams(view, 50)).toEqual({
      groupBy: 'PROJECT',
      status: [],
      owner: [],
      team: [],
      domain: [],
      tag: [],
      q: undefined,
      limit: 50,
      cursor: undefined,
    });

    expect(viewToListParams(view, 25, 'cursor-1')).toMatchObject({ limit: 25, cursor: 'cursor-1' });
  });
});
