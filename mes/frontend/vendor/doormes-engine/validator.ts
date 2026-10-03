import { readFileSync, writeFileSync } from 'node:fs';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';
import { seedDocument } from './model';
import { normalizeCatalog, applyCatalog } from './catalog';
import { inspectManagedGltfAsset } from '@doormes/visual-asset-storage';
import { deriveDrawingBom } from './bom';

// Invoked with server-generated temporary file paths, never executable user text.
try {
  const input = readFileSync(process.argv[2], 'utf8');
  if (Buffer.byteLength(input) > 5_000_000) throw new Error('Design JSON is too large.');
  const request = JSON.parse(input);
  const document = request.mode === 'asset-inspect' ? await inspectManagedGltfAsset({bytes:Uint8Array.from(readFileSync(request.payloadPath)).buffer,expectedContentHash:request.expectedContentHash})
    : request.mode === 'catalog' ? normalizeCatalog(request.id,request.revision,request.input)
    : request.mode === 'apply-catalog' ? applyCatalog(request.document,request.windowId,request.target,request.choice)
    : request.mode === 'bom' ? deriveDrawingBom(request.document)
    : request.mode === 'seed' ? seedDocument(request.input) : parseFormalDesignDocument(request.document);
  writeFileSync(process.argv[3], JSON.stringify({ok:true,document}), {flag:'wx'});
} catch {
  // Do not expose geometry source, file paths, environment or runtime diagnostics.
  writeFileSync(process.argv[3], JSON.stringify({ok:false}), {flag:'wx'});
  process.exitCode=2;
}
