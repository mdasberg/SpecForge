#!/usr/bin/env node
// Serve the seeded design canvas locally:  node serve.mjs [port]
// Loopback only. Run `node serve.mjs --selftest` to check the path guard.
import { createServer } from 'node:http';
import { readFile, readdir, stat } from 'node:fs/promises';
import { dirname, extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const ENTRY = 'specforge.html';
const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.webp': 'image/webp',
  '.woff2': 'font/woff2',
};

// Resolve a request path inside ROOT, or null if it escapes.
export function resolveSafe(pathname) {
  const rel = normalize(decodeURIComponent(pathname === '/' ? `/${ENTRY}` : pathname));
  const file = resolve(ROOT, '.' + rel);
  return file === ROOT || file.startsWith(ROOT + sep) ? file : null;
}

// Warn when the served file predates the sources it was built from.
async function staleCheck() {
  try {
    const seeded = await stat(join(ROOT, ENTRY));
    const parts = join(ROOT, 'parts');
    const names = await readdir(parts);
    const times = await Promise.all(names.map(async (n) => (await stat(join(parts, n))).mtimeMs));
    if (Math.max(...times) > seeded.mtimeMs) {
      console.warn(`! ${ENTRY} is older than parts/ — run: node build.mjs, then re-seed`);
    }
  } catch {
    console.warn(`! ${ENTRY} not found — seed it first (see hot.md)`);
  }
}

if (process.argv.includes('--selftest')) {
  const { strict: assert } = await import('node:assert');
  assert.equal(resolveSafe('/'), join(ROOT, ENTRY), 'root serves the entry file');
  assert.equal(resolveSafe('/canvas.json'), join(ROOT, 'canvas.json'));
  assert.equal(resolveSafe('/parts/shell.css'), join(ROOT, 'parts', 'shell.css'));
  assert.equal(resolveSafe('/sub/../canvas.json'), join(ROOT, 'canvas.json'), 'inner .. stays inside');
  // The invariant that matters: no request path ever resolves outside ROOT.
  for (const hostile of [
    '/../../../etc/passwd',
    '/%2e%2e%2f%2e%2e%2fetc/passwd',
    '/parts/../../../../etc/passwd',
    '/..\\..\\etc\\passwd',
    '//etc/passwd',
    '/./../secrets',
  ]) {
    const got = resolveSafe(hostile);
    assert.ok(got === null || got === ROOT || got.startsWith(ROOT + sep), `escaped ROOT: ${hostile} -> ${got}`);
  }
  console.log('serve.mjs selftest ok');
  process.exit(0);
}

const PORT = Number(process.argv[2] ?? process.env.PORT ?? 4173);

const server = createServer(async (req, res) => {
  const file = resolveSafe(new URL(req.url, 'http://localhost').pathname);
  if (!file) {
    res.writeHead(403, { 'content-type': 'text/plain' }).end('forbidden\n');
    return;
  }
  try {
    const body = await readFile(file);
    res.writeHead(200, {
      'content-type': TYPES[extname(file)] ?? 'application/octet-stream',
      'cache-control': 'no-store',
    }).end(body);
  } catch {
    res.writeHead(404, { 'content-type': 'text/plain' }).end('not found\n');
  }
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Port ${PORT} is already in use. Try: npm start -- ${PORT + 1}`);
    process.exit(1);
  }
  throw err;
});

server.listen(PORT, '127.0.0.1', async () => {
  await staleCheck();
  console.log(`SpecForge design → http://127.0.0.1:${PORT}/`);
});
