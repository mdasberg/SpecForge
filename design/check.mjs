#!/usr/bin/env node
// Guard for the built artboard: every template hole is bound, no build tokens
// survive, tags balance, and the logic parses. Exits non-zero on failure.
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const ART = join(ROOT, 'Main.dc.html');
const LOGIC = join(ROOT, 'parts', 'Main.logic.js');

// Hole names that renderVals() builds at runtime rather than assigning literally,
// plus loop-scoped names. Keep in step with Main.logic.js.
const COMPUTED = [
  /^theme$/,
  /^t[1245](Class|Label|Btn|Toggle)$/,
  /^(doc|changes|discussions|checks|history|trace)Display$/,
  /^tab(Doc|Changes|Discussions|Checks|History|Trace)(On)?$/,
  /^repo[0-9]+Class$/,
  /^pickRepo[0-9]+$/,
  /^repoStep[1-4]Display$/,
  /^f\.[a-zA-Z]+$/,
];

const fail = [];
const art = readFileSync(ART, 'utf8');
const body = art.split('<x-dc>')[1]?.split('</x-dc>')[0];
if (!body) {
  console.error('FAIL  no <x-dc> block in Main.dc.html');
  process.exit(1);
}

const holes = [...new Set([...body.matchAll(/\{\{\s*([A-Za-z0-9_.$]+)\s*\}\}/g)].map((m) => m[1]))];
const logic = readFileSync(LOGIC, 'utf8');
const assigned = new Set([...logic.matchAll(/vals\.([A-Za-z0-9_]+)\s*=/g)].map((m) => m[1]));
const bound = (h) => assigned.has(h) || COMPUTED.some((re) => re.test(h));

const missing = holes.filter((h) => !bound(h));
if (missing.length) fail.push(`unbound holes in the markup: ${missing.join(', ')}`);

const unused = [...assigned].filter((a) => !holes.includes(a));
if (unused.length) fail.push(`values built in the logic but never rendered: ${unused.join(', ')}`);

const stray = body.match(/@i\([A-Za-z]+,\d+\)|@include\([^)]*\)/g);
if (stray) fail.push(`unexpanded build tokens: ${[...new Set(stray)].join(', ')}`);

for (const tag of ['div', 'span', 'button', 'sc-for', 'table', 'ul', 'li']) {
  const open = (body.match(new RegExp(`<${tag}[\\s>]`, 'g')) || []).length;
  const close = (body.match(new RegExp(`</${tag}>`, 'g')) || []).length;
  if (open !== close) fail.push(`<${tag}> unbalanced: ${open} open, ${close} close`);
}

const parsed = spawnSync(process.execPath, ['--check', LOGIC], { encoding: 'utf8' });
if (parsed.status !== 0) fail.push(`Main.logic.js does not parse:\n${parsed.stderr.trim()}`);

if (fail.length) {
  for (const f of fail) console.error(`FAIL  ${f}`);
  process.exit(1);
}
console.log(`ok: ${holes.length} holes bound, tags balanced, logic parses`);
