#!/usr/bin/env node
// Assembles design/<Name>.dc.html from parts/<Name>.body.html + parts/shell.css,
// and writes canvas.json. Icons expand from @i(name,size) tokens.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const P = (f) => join(ROOT, 'parts', f);

// One artboard: artboards share no state at runtime, so the whole clickable flow
// has to live in a single file. Screens and tabs are fragments under parts/.
const BOARDS = [
  { name: 'Main', w: 1440, h: 900, x: 0, y: 0, interactive: true },
];

const NOTES = [
  { id: 'flow-note', x: 0, y: -250, w: 760,
    text: 'SpecForge — clickable prototype\nThe whole flow works: Home, Specs, Reviews, Projects and Activity in the top nav; inside a review, the Document / Changes / Discussions / Checks / History / Traceability tabs.\n\nStart here: Projects, then Connect a repository — pick a GitHub repo, confirm where the specs live, set how new versions enter review, and connect.\n\nThen the review walkthrough: Home, click the Claim Pre-Authorization row, read the document, switch to Changes for the v2 to v3 diff (inline or side by side), resolve or reopen a discussion, accept or dismiss the agent suggestion, create a ticket from a comment, then Approve or Request changes — and read it all back under History.\n\nDiscussion state is shared, so resolving a thread in the document updates the tab badge and the Discussions list. The theme tweak above the artboard switches between dark and light.' },
];

// 24x24 stroke icons, currentColor
const ICONS = {
  logo: '<path d="M12 2.5 20.5 7v10L12 21.5 3.5 17V7z"/><path d="M13.4 7.5 9 13.2h3.2L11.4 17.5 16.4 11.6h-3.4z" fill="currentColor" stroke="none"/>',
  check: '<path d="M20 6.5 9 17.5 4 12.5"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  minus: '<path d="M5 12h14"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3.5 2"/>',
  comment: '<path d="M20 15a2 2 0 0 1-2 2H8l-4 3.5V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2z"/>',
  ticket: '<path d="M4 8.5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2V10a2 2 0 0 0 0 4v1.5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V14a2 2 0 0 0 0-4z"/><path d="M13.5 6.5v11" stroke-dasharray="2 2.5"/>',
  pr: '<circle cx="6" cy="5.5" r="2.5"/><circle cx="6" cy="18.5" r="2.5"/><path d="M6 8v8"/><circle cx="18" cy="18.5" r="2.5"/><path d="M18 16V9a3 3 0 0 0-3-3h-3.5"/><path d="M13.5 3.5 11 6l2.5 2.5"/>',
  branch: '<circle cx="6" cy="5" r="2.2"/><circle cx="6" cy="19" r="2.2"/><circle cx="18" cy="8.5" r="2.2"/><path d="M6 7.2v9.6"/><path d="M18 10.7v.8a4 4 0 0 1-4 4H6"/>',
  commit: '<circle cx="12" cy="12" r="3.4"/><path d="M2.5 12h6.1M15.4 12h6.1"/>',
  chevR: '<path d="M9.5 5.5 16 12l-6.5 6.5"/>',
  chevD: '<path d="M5.5 9.5 12 16l6.5-6.5"/>',
  search: '<circle cx="11" cy="11" r="6.8"/><path d="M20 20l-4.3-4.3"/>',
  bell: '<path d="M18 9.5a6 6 0 1 0-12 0c0 5.5-2 6.5-2 6.5h16s-2-1-2-6.5"/><path d="M13.6 19.5a1.9 1.9 0 0 1-3.2 0"/>',
  filter: '<path d="M4 5.5h16l-6.2 7.2v5.8l-3.6 1.8v-7.6z"/>',
  bot: '<rect x="4" y="8" width="16" height="12" rx="3"/><circle cx="9.5" cy="14" r="1.1" fill="currentColor" stroke="none"/><circle cx="14.5" cy="14" r="1.1" fill="currentColor" stroke="none"/><path d="M12 4.5V8M2.5 13.5h1.5M20 13.5h1.5"/>',
  file: '<path d="M14 3.5H7.5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V8z"/><path d="M13.8 3.5V8.2h4.7"/>',
  folder: '<path d="M4 7.5a2 2 0 0 1 2-2h3l2 2h7a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/>',
  warn: '<path d="M12 4.2 21.5 20H2.5z"/><path d="M12 10v4M12 17h.01"/>',
  alert: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.8v4.4M12 16h.01"/>',
  arrowR: '<path d="M4 12h14.5M13 6.5 18.5 12 13 17.5"/>',
  deploy: '<path d="M4 16.5V19a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-2.5"/><path d="M12 4v11M8 8l4-4 4 4"/>',
  eye: '<path d="M2.5 12S6 6.2 12 6.2 21.5 12 21.5 12 18 17.8 12 17.8 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="2.9"/>',
  link: '<path d="M9.4 14.6l5.2-5.2"/><path d="M10.8 6.9 12.3 5.4a4.2 4.2 0 0 1 6 6l-1.5 1.5"/><path d="M13.2 17.1 11.7 18.6a4.2 4.2 0 0 1-6-6l1.5-1.5"/>',
  history: '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1"/><path d="M3.2 4.4v4.2h4.2"/><path d="M12 8v4.2l3 1.9"/>',
  layers: '<path d="M12 3.2 3.5 8 12 12.8 20.5 8z"/><path d="M3.5 13 12 17.8 20.5 13"/>',
  dots: '<circle cx="6" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="18" cy="12" r="1.5" fill="currentColor" stroke="none"/>',
  tag: '<path d="M4 11.2V5a1 1 0 0 1 1-1h6.2l8.8 8.8-7.2 7.2z"/><circle cx="8" cy="8" r="1.3"/>',
  shield: '<path d="M12 3.2l7.8 2.9v5.6c0 4.9-7.8 9.1-7.8 9.1s-7.8-4.2-7.8-9.1V6.1z"/>',
  db: '<ellipse cx="12" cy="6.2" rx="7.2" ry="2.9"/><path d="M4.8 6.2v11.6c0 1.6 3.2 2.9 7.2 2.9s7.2-1.3 7.2-2.9V6.2"/><path d="M4.8 12c0 1.6 3.2 2.9 7.2 2.9s7.2-1.3 7.2-2.9"/>',
  zap: '<path d="M13.2 3 5.5 14h5.6l-1 7 8.4-11h-5.7z"/>',
  book: '<path d="M4.5 5.5a2 2 0 0 1 2-2H19v17H6.5a2 2 0 0 1-2-2z"/><path d="M9.5 3.5v17"/>',
  user: '<circle cx="12" cy="8" r="3.4"/><path d="M5.2 20a6.8 6.8 0 0 1 13.6 0"/>',
  flag: '<path d="M6 20.5V4.2h11.5l-2.4 4.2 2.4 4.2H6"/>',
  circleDot: '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3.2" fill="currentColor" stroke="none"/>',
  resolve: '<circle cx="12" cy="12" r="8.5"/><path d="M8 12.4l2.9 2.9 5.3-5.6"/>',
  sliders: '<path d="M4 8h8M16.5 8H20M4 16h3.5M12 16h8"/><circle cx="14" cy="8" r="2.2"/><circle cx="9.5" cy="16" r="2.2"/>',
  split: '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M12 4.5v15" stroke-dasharray="2.5 2.5"/>',
  rows: '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M3.5 12h17"/>',
  spec: '<path d="M6 3.5h9l4 4v13H6z"/><path d="M14.8 3.6V7.8h4.2"/><path d="M9 12h7M9 16h4"/>',
  play: '<path d="M7 4.5 19 12 7 19.5z"/>',
};

const INCLUDE_RE = /^[ \t]*@include\(([A-Za-z0-9._-]+)\)[ \t]*$/gm;
const inline = (s, depth = 0) => {
  if (depth > 4) throw new Error('@include nested too deep');
  return s.replace(INCLUDE_RE, (m, file) => inline(readFileSync(P(file), 'utf8').trimEnd(), depth + 1));
};

const ICON_RE = /@i\(([A-Za-z]+),(\d+)\)/g;
const expand = (s) => s.replace(ICON_RE, (m, name, size) => {
  const d = ICONS[name];
  if (!d) throw new Error(`unknown icon: ${name}`);
  const sw = Number(size) <= 13 ? 2 : Number(size) <= 17 ? 1.8 : 1.7;
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" style="flex:0 0 auto">${d}</svg>`;
});

const css = readFileSync(P('shell.css'), 'utf8');
const FONTS = '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap">';

for (const b of BOARDS) {
  const body = expand(inline(readFileSync(P(`${b.name}.body.html`), 'utf8').trim()));
  const logicFile = P(`${b.name}.logic.js`);
  const propsFile = P(`${b.name}.props.json`);
  const theme = b.theme || 'dark';

  const props = existsSync(propsFile)
    ? JSON.parse(readFileSync(propsFile, 'utf8'))
    : {};
  props.theme = { editor: 'enum', options: ['dark', 'light'], default: theme, section: 'Theme' };
  props.$preview = { width: b.w, height: b.h };

  const logic = existsSync(logicFile)
    ? readFileSync(logicFile, 'utf8').trim()
    : `class Component extends DCLogic {\n  renderVals() {\n    return { theme: this.props.theme ?? '${theme}' };\n  }\n}`;

  const propsAttr = JSON.stringify(props)
    .replace(/&/g, '&amp;')
    .replace(/'/g, '&#39;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  const out = `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
  ${FONTS}
  <style>
${css}  </style>
</helmet>
${body}
</x-dc>
<script data-dc-script data-props='${propsAttr}'>
${logic}
</script>
</body>
</html>
`;
  writeFileSync(join(ROOT, `${b.name}.dc.html`), out);
}

const canvas = {
  artboards: BOARDS.map((b) => ({
    file: `${b.name}.dc.html`, x: b.x, y: b.y, w: b.w, h: b.h,
    ...(b.interactive ? { is_interactive: true } : {}),
  })),
  annotations: NOTES,
  launch: { view: 'focused', file: 'Main.dc.html' },
};
writeFileSync(join(ROOT, 'canvas.json'), JSON.stringify(canvas, null, 2) + '\n');
console.log(`built ${BOARDS.length} artboards + canvas.json`);
