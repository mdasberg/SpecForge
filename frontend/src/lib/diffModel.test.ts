import { describe, expect, it } from 'vitest';
import { sideBySideRows, changeSetOf } from './diffModel';
import type { DiffLine, DiffSection } from '../api/review';

function line(type: DiffLine['type'], text: string, baseLine?: number, headLine?: number): DiffLine {
  return { type, text, baseLine: baseLine ?? null, headLine: headLine ?? null, words: [] };
}

const MODIFIED: DiffSection = {
  anchorKey: 'validation-rules-1',
  heading: 'Validation Rules',
  level: 2,
  change: 'MODIFIED',
  changedLines: 3,
  lines: [
    line('CONTEXT', '## Validation Rules', 7, 7),
    line('CONTEXT', '', 8, 8),
    line('REMOVED', 'The member must be active.', 9),
    line('REMOVED', 'The benefit must have remaining balance.', 10),
    line('ADDED', 'The member must be enrolled.', undefined, 9),
  ],
};

describe('sideBySideRows', () => {
  it('shows the same change set as the inline lines', () => {
    const inline = changeSetOf(MODIFIED.lines);
    const sideBySide = changeSetOf(sideBySideRows(MODIFIED.lines).flatMap((row) => [row.left, row.right]));

    expect(sideBySide).toEqual(inline);
  });

  it('puts a context line on both sides of the same row', () => {
    const [first] = sideBySideRows(MODIFIED.lines);

    expect(first.left?.text).toBe('## Validation Rules');
    expect(first.right?.text).toBe('## Validation Rules');
  });

  it('pairs a removed line with the added line that replaced it', () => {
    const rows = sideBySideRows(MODIFIED.lines);
    const paired = rows.find((row) => row.left?.type === 'REMOVED');

    expect(paired?.left?.text).toBe('The member must be active.');
    expect(paired?.right?.text).toBe('The member must be enrolled.');
  });

  it('leaves the other side empty when a deletion has no replacement', () => {
    const rows = sideBySideRows(MODIFIED.lines);
    const unpaired = rows.find((row) => row.left?.text === 'The benefit must have remaining balance.');

    expect(unpaired?.right).toBeNull();
  });

  it('keeps a pure insertion on the right', () => {
    const rows = sideBySideRows([line('CONTEXT', '# Spec', 1, 1), line('ADDED', 'New.', undefined, 2)]);

    expect(rows[1].left).toBeNull();
    expect(rows[1].right?.text).toBe('New.');
  });
});
