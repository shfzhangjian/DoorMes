import { readFileSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Resolve tests to the copied packages, never to the standalone prototype.
const root = dirname(fileURLToPath(import.meta.url));
const alias = readdirSync(resolve(root, 'packages')).flatMap((folder) => {
  const manifest = JSON.parse(readFileSync(resolve(root, 'packages', folder, 'package.json'), 'utf8'));
  return Object.entries(manifest.exports ?? { '.': './src/index.ts' }).map(([key, target]) => ({
    find: key === '.' ? manifest.name : `${manifest.name}${key.slice(1)}`,
    replacement: resolve(root, 'packages', folder, target as string)
  }));
}).sort((a, b) => b.find.length - a.find.length);

export default {
  root,
  resolve: { alias },
  test: { environment: 'node', include: [
    'packages/drawing-projection/src/index.test.ts',
    'packages/renderer-drawing-svg/src/index.test.ts'
  ] }
};
