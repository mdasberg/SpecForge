import { useState } from 'react';
import type { SpecSection } from '../api/catalog';
import type { TextRange } from '../api/discussion';

/**
 * Opens a thread anchored to a section, or to an exact phrase within it. The phrase is matched
 * against the review's own content client-side before it is ever sent, so a typo produces an
 * immediate message instead of a round trip to learn the server rejected it.
 *
 * <p>ponytail: matching is `content.indexOf(phrase)` — the first occurrence anywhere in the
 * document, not a real text-selection-to-offset mapping from the rendered page. Good enough for a
 * phrase that only appears once, which covers the common case; a true click-to-select affordance
 * in the rendered document is the next step up from this.
 */
export function NewThreadForm({
  sections,
  content,
  onSubmit,
}: {
  sections: SpecSection[];
  content: string;
  onSubmit: (request: { sectionKey: string; range?: TextRange; quotedText?: string; body: string }) => Promise<void>;
}) {
  const [sectionKey, setSectionKey] = useState(sections[0]?.anchorKey ?? '');
  const [phrase, setPhrase] = useState('');
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!sectionKey || !body.trim()) return;
    setError(null);

    let range: TextRange | undefined;
    let quotedText: string | undefined;
    if (phrase.trim()) {
      const start = content.indexOf(phrase.trim());
      if (start < 0) {
        setError('That phrase does not appear in the document exactly as typed.');
        return;
      }
      range = { startOffset: start, endOffset: start + phrase.trim().length };
      quotedText = phrase.trim();
    }

    setBusy(true);
    try {
      await onSubmit({ sectionKey, range, quotedText, body: body.trim() });
      setPhrase('');
      setBody('');
    } catch {
      setError('Could not open the thread.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card card-pad" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <select className="input" style={{ flex: '0 0 auto' }} value={sectionKey} onChange={(e) => setSectionKey(e.target.value)}>
          {sections.map((section) => (
            <option key={section.anchorKey} value={section.anchorKey}>
              {section.heading}
            </option>
          ))}
        </select>
        <input
          className="input"
          style={{ flex: '1 1 auto' }}
          placeholder="Optional: quote an exact phrase to comment on instead of the whole section"
          value={phrase}
          onChange={(e) => setPhrase(e.target.value)}
        />
      </div>
      <textarea
        className="input area"
        placeholder="Start a discussion…"
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      {error && <div style={{ color: 'var(--red)', fontSize: 11.5 }}>{error}</div>}
      <div>
        <button type="button" className="btn btn-primary btn-sm" disabled={busy || !body.trim()} onClick={() => void submit()}>
          Comment
        </button>
      </div>
    </div>
  );
}
