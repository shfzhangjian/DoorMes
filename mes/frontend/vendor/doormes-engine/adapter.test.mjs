import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname, basename } from 'node:path';
import { spawnSync } from 'node:child_process';
import { seedDocument, validateDocument } from './dist/engine.mjs';

const input={designId:'6a5630da-b312-43d3-a4db-54309bf45907',lineId:'e5757683-0d5b-4536-9e83-106fafb5dffe',mark:'C1',quantity:2,widthMm:1200,heightMm:1500};
test('native adapter reuses the formal model and detaches snapshots',()=>{
  const document=seedDocument(input);assert.equal(document.schemaVersion,'doormes-domain.v1');
  assert.equal(document.windows[0].widthMm,1200);assert.equal(document.windows[0].quantity,2);
  const restored=validateDocument(JSON.parse(JSON.stringify(document)));assert.deepEqual(restored,document);
  restored.windows[0].mark='C9';assert.equal(document.windows[0].mark,'C1');
});
test('geometry command replay rejects invalid dimensions and duplicate internal IDs',()=>{
  const document=seedDocument(input),invalid=structuredClone(document);invalid.windows[0].widthMm=0;
  assert.throws(()=>validateDocument(invalid));document.windows.push(structuredClone(document.windows[0]));
  assert.throws(()=>validateDocument(document));
});
test('headless server validator produces the same normalized JSON',async()=>{
  const directory=await mkdtemp(join(tmpdir(),'doormes-engine-'));
  try {
    const source=join(directory,'input.json'),output=join(directory,'output.json');
    await writeFile(source,JSON.stringify({mode:'seed',input}));
    const result=spawnSync(process.execPath,['dist/validator.mjs',source,output],{timeout:20000});
    assert.equal(result.status,0);assert.deepEqual(JSON.parse(await readFile(output,'utf8')).document,seedDocument(input));
  } finally {assert.equal(dirname(directory),tmpdir());assert.match(basename(directory),/^doormes-engine-/);await rm(directory,{recursive:true,force:true});}
});
