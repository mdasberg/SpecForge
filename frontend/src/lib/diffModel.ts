import type { DiffLine } from '../api/review';

/** One row of the side-by-side view: previous on the left, current on the right. */
export interface SideBySideRow {
  left: DiffLine | null;
  right: DiffLine | null;
}

/**
 * Rearranges the server's single ordered list of lines into left/right pairs.
 *
 * <p>Inline and side-by-side are two readings of the same payload rather than two responses. That is
 * what makes switching between them free — nothing is refetched — and it is also why they cannot
 * disagree: there is only one computed diff, and both views are projections of it.
 *
 * <p>A run of removals followed by a run of additions is the shape of a rewrite, so the two runs are
 * paired position by position. Whichever run is longer keeps the surplus lines on its own side with
 * nothing opposite them, because those lines really were only added or only deleted.
 */
export function sideBySideRows(lines: DiffLine[]): SideBySideRow[] {
  const rows: SideBySideRow[] = [];
  let i = 0;
  while (i < lines.length) {
    if (lines[i].type === 'CONTEXT') {
      rows.push({ left: lines[i], right: lines[i] });
      i += 1;
      continue;
    }
    const removed: DiffLine[] = [];
    while (i < lines.length && lines[i].type === 'REMOVED') {
      removed.push(lines[i]);
      i += 1;
    }
    const added: DiffLine[] = [];
    while (i < lines.length && lines[i].type === 'ADDED') {
      added.push(lines[i]);
      i += 1;
    }
    for (let k = 0; k < Math.max(removed.length, added.length); k += 1) {
      rows.push({ left: removed[k] ?? null, right: added[k] ?? null });
    }
  }
  return rows;
}

/**
 * The set of changes a view actually shows, as comparable strings. Context lines are excluded and
 * each line is counted once however many times a view puts it on screen, which is what lets a test
 * assert that two renderings of one payload say the same thing.
 */
export function changeSetOf(lines: (DiffLine | null)[]): string[] {
  const seen = new Set<string>();
  for (const line of lines) {
    if (line && line.type !== 'CONTEXT') {
      seen.add(`${line.type}:${line.baseLine ?? ''}:${line.headLine ?? ''}:${line.text}`);
    }
  }
  return [...seen].sort();
}
