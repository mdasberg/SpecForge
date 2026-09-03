#!/usr/bin/env node
// Re-seed specforge.html from Main.dc.html + canvas.json using the design
// skill's seed-canvas.mjs. Point SEED_CANVAS at that script to skip the search.
import { existsSync, globSync, statSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));

function findSeeder() {
  if (process.env.SEED_CANVAS) {
    if (!existsSync(process.env.SEED_CANVAS)) {
      console.error(`SEED_CANVAS is set but does not exist: ${process.env.SEED_CANVAS}`);
      process.exit(1);
    }
    return process.env.SEED_CANVAS;
  }
  // The skill is extracted to a temp directory whose name changes per version.
  const hits = globSync('/private/tmp/claude-*/bundled-skills/*/*/design/seed-canvas.mjs')
    .map((p) => ({ p, t: statSync(p).mtimeMs }))
    .sort((a, b) => b.t - a.t);
  if (!hits.length) {
    console.error([
      'Could not find the design skill\'s seed-canvas.mjs.',
      'Run /design once in Claude Code to extract the skill, or set SEED_CANVAS:',
      '  SEED_CANVAS=/path/to/design/seed-canvas.mjs npm run seed',
    ].join('\n'));
    process.exit(1);
  }
  return hits[0].p;
}

const seeder = findSeeder();
const template = join(dirname(seeder), 'payload.template.html');
const run = (args) => {
  const r = spawnSync(process.execPath, [seeder, ...args], { cwd: ROOT, stdio: 'inherit' });
  if (r.status !== 0) process.exit(r.status ?? 1);
};

run([
  '--template', template,
  '--out', 'specforge.html',
  '--title', 'SpecForge',
  '--artboard', 'Main.dc.html',
  '--canvas', 'canvas.json',
]);
run(['--check', 'specforge.html']);
