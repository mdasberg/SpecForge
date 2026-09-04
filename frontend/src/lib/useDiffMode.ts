import { useEffect, useState } from 'react';

export type DiffMode = 'inline' | 'split';

const MODE_KEY = 'specforge.diffMode';

/**
 * The reviewer's preferred rendering, remembered in the browser rather than on the server.
 *
 * <p>It is a per-person display habit, not something another reviewer, an agent or the audit trail
 * ever reads, so a row in the database would be a synchronised copy of something only this browser
 * cares about. It lives outside the component file so that editing a component still hot-reloads.
 */
export function useDiffMode(): [DiffMode, (mode: DiffMode) => void] {
  const [mode, setMode] = useState<DiffMode>(() =>
    (globalThis.localStorage?.getItem(MODE_KEY) === 'split' ? 'split' : 'inline'));

  useEffect(() => {
    globalThis.localStorage?.setItem(MODE_KEY, mode);
  }, [mode]);

  return [mode, setMode];
}
