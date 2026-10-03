import { readFile, readdir, copyFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from '../../node_modules/vite/dist/node/index.js';

const root = dirname(fileURLToPath(import.meta.url));
const own = createRequire(resolve(root, 'package.json'));
// Build-only fallback to the existing cached prototype dependencies. Output is
// self-contained; the running MES does not fetch code or start the prototype.
const cached = createRequire(resolve(root, '../../../../DoorMes/package.json'));
const dependency = (name) => { try { return own.resolve(name); } catch { return cached.resolve(name); } };
const alias = [];
for (const folder of await readdir(resolve(root, 'packages'))) {
  const manifest = JSON.parse(await readFile(resolve(root, 'packages', folder, 'package.json'), 'utf8'));
  for (const [key, target] of Object.entries(manifest.exports ?? { '.': './src/index.ts' })) {
    alias.push({ find: key === '.' ? manifest.name : `${manifest.name}${key.slice(1)}`, replacement: resolve(root, 'packages', folder, target) });
  }
}
alias.sort((a, b) => b.find.length - a.find.length);
alias.push({ find: /^three\/addons\/(.*)$/, replacement: `${dirname(dependency('three'))}/../examples/jsm/$1` });
alias.push({ find: /^three$/, replacement: resolve(dirname(dependency('three')),'three.module.js') });
alias.push({ find: /^ajv\/dist\/2020\.js$/, replacement: dependency('ajv/dist/2020.js') });
const common = { configFile: false, root, resolve: { alias }, logLevel: 'warn' };
await build({ ...common, build: { target: 'es2022', outDir: 'dist', emptyOutDir: true,
  lib: { entry: resolve(root, 'adapter.ts'), formats: ['es'], fileName: () => 'engine.mjs' } } });
await build({ ...common, build: { target: 'es2022', outDir: 'dist', emptyOutDir: false,
  minify: false, lib: { entry: resolve(root, 'validator.ts'), formats: ['es'], fileName: () => 'validator.mjs' },
  rollupOptions: { external: ['node:fs', 'node:path', 'node:process'] } } });
console.log('Built native MES drawing adapter and shared headless geometry validator.');
await copyFile(resolve(root,'adapter-api.d.mts'),resolve(root,'dist/engine.d.mts'));
