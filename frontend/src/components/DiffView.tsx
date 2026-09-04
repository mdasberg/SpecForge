import { useMemo } from 'react';
import type { ReactNode } from 'react';
import type { DiffLine, DiffSection, SpecDiff } from '../api/review';
import { sideBySideRows } from '../lib/diffModel';
import type { DiffMode } from '../lib/useDiffMode';
import { formatRelativeTime } from '../lib/format';

const CHANGE_LABEL: Record<DiffSection['change'], string> = {
  ADDED: 'Added',
  REMOVED: 'Removed',
  MODIFIED: 'Modified',
  UNCHANGED: 'Unchanged',
};

const CHANGE_BADGE: Record<DiffSection['change'], string> = {
  ADDED: 'b-approved',
  REMOVED: 'b-blocked',
  MODIFIED: 'b-changes',
  UNCHANGED: 'b-draft',
};

function lineClass(type: DiffLine['type']): string {
  if (type === 'ADDED') return 'dl-add';
  if (type === 'REMOVED') return 'dl-del';
  return 'dl-ctx';
}

/** Splits the line at the ranges the server marked, so the changed words stand out inside it. */
function Text({ line }: { line: DiffLine }) {
  if (line.words.length === 0) return <>{line.text}</>;
  const nodes: ReactNode[] = [];
  let cursor = 0;
  line.words.forEach((range, i) => {
    if (range.start > cursor) nodes.push(<span key={`p${i}`}>{line.text.slice(cursor, range.start)}</span>);
    nodes.push(<span key={`w${i}`} className="diff-word">{line.text.slice(range.start, range.end)}</span>);
    cursor = range.end;
  });
  if (cursor < line.text.length) nodes.push(<span key="tail">{line.text.slice(cursor)}</span>);
  return <>{nodes}</>;
}

function InlineLines({ lines }: { lines: DiffLine[] }) {
  return (
    <>
      {lines.map((line, i) => (
        <div key={i} className={`diff-row ${lineClass(line.type)}`}>
          <span className="diff-num">{line.baseLine ?? ''}</span>
          <span className="diff-num">{line.headLine ?? ''}</span>
          <span className="diff-text"><Text line={line} /></span>
        </div>
      ))}
    </>
  );
}

function SplitLines({ lines }: { lines: DiffLine[] }) {
  const rows = useMemo(() => sideBySideRows(lines), [lines]);
  return (
    <>
      {rows.map((row, i) => (
        <div key={i} className="diff-row">
          <span className={`diff-half ${row.left ? lineClass(row.left.type) : 'dl-void'}`}>
            <span className="diff-num">{row.left?.baseLine ?? ''}</span>
            <span className="diff-text">{row.left ? <Text line={row.left} /> : ''}</span>
          </span>
          <span className={`diff-half ${row.right ? lineClass(row.right.type) : 'dl-void'}`}>
            <span className="diff-num">{row.right?.headLine ?? ''}</span>
            <span className="diff-text">{row.right ? <Text line={row.right} /> : ''}</span>
          </span>
        </div>
      ))}
    </>
  );
}

function Section({ section, mode }: { section: DiffSection; mode: DiffMode }) {
  return (
    <div className="diff-sec" id={`diff-${section.anchorKey || 'preamble'}`}>
      <div className="diff-sec-h">
        <span className="name">{section.heading}</span>
        <span className={`badge ${CHANGE_BADGE[section.change]}`}>{CHANGE_LABEL[section.change]}</span>
        <span className="faint" style={{ fontSize: 11 }}>
          {section.changedLines} line{section.changedLines === 1 ? '' : 's'}
        </span>
        <span className="spacer" />
        {section.author && (
          <span className="faint" style={{ fontSize: 11 }}>
            {section.author}
            {section.changedAt ? ` · ${formatRelativeTime(section.changedAt)}` : ''}
          </span>
        )}
      </div>
      <div className="diff-body">
        {mode === 'split' ? <SplitLines lines={section.lines} /> : <InlineLines lines={section.lines} />}
      </div>
    </div>
  );
}

/** The changed sections, with the number of changes in each; selecting one scrolls to it. */
export function DiffJumpList({ diff }: { diff: SpecDiff }) {
  const changed = diff.sections.filter((section) => section.change !== 'UNCHANGED');
  if (changed.length === 0) return null;
  return (
    <div>
      <div className="side-t">Changed sections</div>
      <div className="side-group diff-jump">
        {changed.map((section) => (
          <button
            key={section.anchorKey}
            type="button"
            className="side-item"
            style={{ width: '100%', textAlign: 'left', background: 'none', border: 'none', font: 'inherit', cursor: 'pointer' }}
            onClick={() =>
              document
                .getElementById(`diff-${section.anchorKey || 'preamble'}`)
                ?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
          >
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {section.heading}
            </span>
            <span className="count">{section.changedLines}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

export function DiffModeToggle({ mode, onChange }: { mode: DiffMode; onChange: (mode: DiffMode) => void }) {
  return (
    <div style={{ display: 'flex', gap: 4 }}>
      <button type="button" className={`chip${mode === 'inline' ? ' on' : ''}`} onClick={() => onChange('inline')}>
        Inline
      </button>
      <button type="button" className={`chip${mode === 'split' ? ' on' : ''}`} onClick={() => onChange('split')}>
        Side by side
      </button>
    </div>
  );
}

/**
 * The diff itself. Unchanged sections are listed but not expanded: their body is already in the
 * document view, and the server ships no lines for them.
 */
export function DiffView({ diff, mode }: { diff: SpecDiff; mode: DiffMode }) {
  const changed = diff.sections.filter((section) => section.change !== 'UNCHANGED');

  if (changed.length === 0) {
    return (
      <div className="card card-pad">
        <div className="card-t">No changes</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>
          {diff.base.label} and {diff.head.label} have identical content.
        </p>
      </div>
    );
  }

  return (
    <div className="diff">
      {changed.map((section) => (
        <Section key={section.anchorKey} section={section} mode={mode} />
      ))}
    </div>
  );
}

export function DiffSummaryLine({ diff }: { diff: SpecDiff }) {
  const { addedSections, removedSections, modifiedSections, changedLines } = diff.summary;
  return (
    <p className="sub">
      {diff.base.label} → {diff.head.label} · {modifiedSections} modified, {addedSections} added,{' '}
      {removedSections} removed · {changedLines} changed line{changedLines === 1 ? '' : 's'}
    </p>
  );
}
