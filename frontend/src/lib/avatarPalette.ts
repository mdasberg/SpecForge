/** The stylesheet's five per-person palettes, cycled by name so one person keeps one colour everywhere. */
export const HUMAN_PALETTE = ['av-md', 'av-am', 'av-jv', 'av-pr', 'av-sb'];

export function paletteClass(name: string): string {
  let hash = 0;
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) | 0;
  return HUMAN_PALETTE[Math.abs(hash) % HUMAN_PALETTE.length];
}
