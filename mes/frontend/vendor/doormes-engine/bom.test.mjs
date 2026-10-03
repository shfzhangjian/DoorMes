import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname, basename } from 'node:path';
import { spawnSync } from 'node:child_process';
import { applyCatalog, deriveDrawingBom, normalizeCatalog, seedDocument, validateDocument } from './dist/engine.mjs';

const input = { designId: 'BOM-TEST', lineId: 'L1', mark: 'C1', quantity: 2, widthMm: 1200, heightMm: 1500 };
const seed = (overrides = {}) => seedDocument({ ...input, ...overrides });
const byComponent = (bom, component) => bom.lines.find((line) => line.sourceComponentId === component);

function hingedDocument() {
  const document = seed();
  const cell = document.windows[0].layout.cells[0];
  Object.assign(cell, { type: 'turn_tilt', opening: 'right_in', hardwareSetId: 'HW-TT-STD', openingAssembly: {
    mechanism: 'tilt_turn', panelCount: 1, activePanelCount: 1, trackCount: 1, stackSide: 'none', primarySide: 'right',
    mullionMode: 'fixed_mullion', openPlane: 'in', operationPriority: 'turn_first', ventilationMode: 'tilt',
    trafficDoor: 'none', screenMode: 'none', cornerAngleDeg: 90, cornerPostMode: 'postless', pocketDepthMm: 0,
    openPercent: 80, panels: [{ id: 'P1', label: '1号扇', role: 'primary', movable: true, hingeSide: 'right', trackIndex: 0, operationOrder: 0 }],
    operationSequence: ['P1']
  }});
  return validateDocument(document);
}

function slidingDocument() {
  const document = seed();
  const cell = document.windows[0].layout.cells[0];
  Object.assign(cell, { type: 'sliding', opening: 'slide_right', hardwareSetId: 'HW-SLIDE-UNMAPPED', openingAssembly: {
    mechanism: 'sliding', panelCount: 2, activePanelCount: 1, trackCount: 2, stackSide: 'right', overlapMm: 40, openPercent: 80,
    panels: [
      { id: 'P1', label: '1号扇', role: 'active', movable: true, trackIndex: 0, closedPositionIndex: 0, operationOrder: 0, travelDirection: 'right' },
      { id: 'P2', label: '2号扇', role: 'passive', movable: false, trackIndex: 1, closedPositionIndex: 1 }
    ], operationSequence: ['P1']
  }});
  return validateDocument(document);
}

function joinedDocument() {
  const document = seed({ quantity: 1 });
  const second = seed({ lineId: 'L2', mark: 'C2', quantity: 1 }).windows[0];
  document.windows.push(second);
  document.assemblies = [{
    kind: 'fabrication-assembly', objectId: 'ASSEMBLY-1', mark: 'A1',
    instances: [
      { objectId: 'INSTANCE-1', windowId: document.windows[0].objectId, transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
      { objectId: 'INSTANCE-2', windowId: second.objectId, transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } }
    ],
    joints: [{ objectId: 'JOINT-1', jointType: 'mullion_joint', firstInstanceId: 'INSTANCE-1', firstEdge: 'right',
      secondInstanceId: 'INSTANCE-2', secondEdge: 'left', gapMm: 30, factoryScope: 'factory' }],
    openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 },
    installation: structuredClone(document.windows[0].installation)
  }];
  return validateDocument(document);
}

test('fixed window BOM contains four separate frames, glass, beads and seals with reference-only dimensions', () => {
  const document = seed();
  const original = structuredClone(document);
  const bom = deriveDrawingBom(document);
  assert.deepEqual(document, original);
  assert.equal(bom.schemaVersion, 'doormes-drawing-bom.v1');
  assert.equal(bom.productionReady, false);
  assert.equal(bom.quantityBasis, 'document-window-quantities');
  assert.deepEqual(bom.lines.filter((line) => line.sourceComponentId.startsWith('frame.')).map((line) => line.name), ['上框', '下框', '左框', '右框']);
  assert.equal(bom.lines.length, 8);
  assert.equal(byComponent(bom, 'frame.top').quantity, 2);
  assert.equal(byComponent(bom, 'frame.top').modelCode, 'AL70-K01');
  assert.equal(byComponent(bom, 'cell.1.1.glass').quantity, 2);
  assert.equal(byComponent(bom, 'cell.1.1.glass.bead.h').quantity, 4);
  assert.equal(byComponent(bom, 'cell.1.1.glass.gasket').unit, 'm');
  assert.ok(bom.lines.every((line) => !line.productionReady && line.dimensions.basis === 'reference-rule'));
  assert.match(byComponent(bom, 'frame.top').dimensions.label, /参考/);
  assert.ok(bom.diagnostics.some((item) => item.code === 'REFERENCE_RULES_NOT_FACTORY_APPROVED' && item.blocksProduction));
  assert.deepEqual(deriveDrawingBom(document), bom);
});

test('internal IDs survive dimension, revision, quantity and mark edits; authored part numbers and notes survive', () => {
  const document = seed();
  const before = deriveDrawingBom(document);
  document.revision += 7;
  document.windows[0].widthMm = 1400;
  document.windows[0].quantity = 5;
  document.windows[0].mark = 'C9';
  document.windows[0].designComponentRemarks = { profile: '框料备注', glass: '玻璃备注', hardware: '五金备注', surround: '包套备注' };
  document.factoryDrawingElementOptions = [{ objectId: 'W-L1:frame.top', factoryDrawingNumber: 'TOP-007', showInComponentTable: false, showDimensions: false }];
  const after = deriveDrawingBom(document);
  assert.deepEqual(after.lines.map((line) => line.objectId), before.lines.map((line) => line.objectId));
  assert.equal(byComponent(after, 'frame.top').displayCode, 'TOP-007');
  assert.equal(byComponent(after, 'frame.top').quantity, 5);
  assert.equal(byComponent(after, 'frame.top').dimensions.lengthMm, 1400);
  assert.equal(byComponent(after, 'frame.top').remark, '框料备注');
  assert.equal(byComponent(after, 'cell.1.1.glass').remark, '玻璃备注');
  assert.equal(new Set(after.lines.map((line) => line.displayCode)).size, after.lines.length);
});

test('hinged openings include four sash members and count handle sets and hinges once', () => {
  const bom = deriveDrawingBom(hingedDocument());
  const sashes = bom.lines.filter((line) => line.sourceComponentId.includes('.sash.'));
  assert.equal(sashes.length, 4);
  assert.ok(sashes.every((line) => line.quantity === 2 && line.modelCode === 'AL70-S01'));
  assert.equal(byComponent(bom, 'cell.1.1.handle').quantity, 2);
  assert.equal(byComponent(bom, 'cell.1.1.hinge').quantity, 4);
  assert.ok(bom.diagnostics.some((item) => item.code === 'HARDWARE_MACHINING_TEMPLATE_REQUIRED'));
});

test('changing only a finish/catalog revision changes BOM comparison data while retaining part identity', () => {
  const document = seed();
  const catalogId = 'eaec5d47-9cb1-402e-a4d1-4dfb0797b7c5';
  const data = normalizeCatalog(catalogId, 2, { category: 'finish', code: 'BLACK-MATTE', name: '哑黑外表面', specification: '哑光',
    note: '', materialFamily: 'metal', baseColor: '#121212', metalness: 0.5, roughness: 0.8, opacity: 1,
    thicknessMm: null, compatibleProfileSystemIds: [] });
  const updated = applyCatalog(document, document.windows[0].objectId, 'profile-outside', { id: catalogId, revision: 2, status: 'PUBLISHED', data });
  const before = deriveDrawingBom(document), after = deriveDrawingBom(updated);
  const left = byComponent(before, 'frame.top'), right = byComponent(after, 'frame.top');
  assert.equal(left.objectId, right.objectId);
  assert.equal(left.quantity, right.quantity);
  assert.deepEqual(left.dimensions, right.dimensions);
  assert.notDeepEqual(left, right);
  assert.equal(right.appearance.outside.finishCode, 'BLACK-MATTE');
  assert.equal(right.appearance.outside.appearanceVersion, '2');
  assert.match(right.color, /#121212/);
  assert.equal(right.productionReady, false);
});

test('grid and explicit topology members remain individually traceable and carry their own notes', () => {
  const document = seed();
  const window = document.windows[0];
  window.geometryMode = 'topology';
  window.topology.members = [{ objectId: 'MEMBER-1', role: 'mullion', orientation: 'vertical', hostRegionId: window.layout.cells[0].objectId,
    positionRatio: 0.5, span: { startRatio: 0, endRatio: 1 }, profileId: 'CUSTOM-MULLION', throughMode: 'continuous',
    connectionStart: 'through', connectionEnd: 'through', note: '保留中梃备注' }];
  const bom = deriveDrawingBom(document);
  const member = byComponent(bom, 'topology.member.MEMBER-1');
  assert.ok(member);
  assert.equal(member.modelCode, 'CUSTOM-MULLION');
  assert.equal(member.quantity, 2);
  assert.ok(member.sourceObjectIds.includes('MEMBER-1'));
  assert.match(member.remark, /保留中梃备注/);
  assert.equal(bom.lines.filter((line) => line.category === 'glass').length, 2);
});

test('sliding geometry yields explicit sash, glass, tracks and hardware without invented cut sizes', () => {
  const bom = deriveDrawingBom(slidingDocument());
  assert.equal(bom.lines.filter((line) => line.sourceComponentId.startsWith('frame.')).length, 4);
  assert.equal(bom.lines.filter((line) => line.sourceComponentId.includes('.sash.')).length, 8);
  assert.equal(bom.lines.filter((line) => line.category === 'glass').length, 2);
  assert.equal(bom.lines.filter((line) => line.sourceComponentId.includes('.track.')).length, 2);
  assert.equal(bom.lines.find((line) => line.category === 'hardware').modelCode, 'HW-SLIDE-UNMAPPED');
  assert.ok(bom.lines.every((line) => !('lengthMm' in line.dimensions) && !('grossLengthMm' in line.dimensions)));
  assert.ok(bom.lines.filter((line) => line.category === 'glass').every((line) => line.dimensions.basis === 'design-envelope' && !('widthMm' in line.dimensions)));
  assert.ok(bom.diagnostics.some((item) => item.code === 'DESIGN_COMPONENTS_REQUIRE_MANUFACTURING_MAPPING'));
});

test('an unknown catalog preserves design parts and does not remove other windows reference BOM', () => {
  const document = seed();
  const second = seed({ lineId: 'L2', mark: 'C2' }).windows[0];
  second.profileSystemId = 'CUSTOM-SERIES-UNMAPPED';
  document.windows.push(second);
  const bom = deriveDrawingBom(document);
  assert.equal(bom.lines.filter((line) => line.sourceWindowId === 'W-L1').length, 8);
  assert.equal(bom.lines.filter((line) => line.sourceWindowId === 'W-L2').length, 5);
  assert.ok(bom.lines.filter((line) => line.sourceWindowId === 'W-L2').every((line) => line.dimensions.basis !== 'reference-rule'));
  assert.ok(bom.diagnostics.some((item) => item.code === 'MANUFACTURING_REFERENCE_UNAVAILABLE'));
});

test('physical assembly joints include reference connector, fastener, seal and cover demand', () => {
  const bom = deriveDrawingBom(joinedDocument());
  const joints = bom.lines.filter((line) => line.sourceObjectIds.includes('JOINT-1'));
  assert.ok(joints.length >= 4);
  assert.ok(joints.some((line) => line.sourceComponentId.endsWith('.connector')));
  assert.ok(joints.some((line) => line.sourceComponentId.endsWith('.fastener')));
  assert.ok(joints.some((line) => line.sourceComponentId.endsWith('.seal')));
  assert.ok(joints.every((line) => !line.productionReady));
  assert.equal(new Set(bom.lines.map((line) => line.objectId)).size, bom.lines.length);
});

test('unknown catalog fallback retains actual glazing partitions and enabled surround components', () => {
  const document = seed();
  const window = document.windows[0];
  window.profileSystemId = 'CUSTOM-SERIES-UNMAPPED';
  window.geometryMode = 'topology';
  window.topology.members = [{ objectId: 'MEMBER-1', role: 'mullion', orientation: 'vertical', hostRegionId: window.layout.cells[0].objectId,
    positionRatio: 0.5, span: { startRatio: 0, endRatio: 1 }, profileId: 'CUSTOM-MULLION', throughMode: 'continuous',
    connectionStart: 'through', connectionEnd: 'through', note: '' }];
  window.installation.surround.enabled = true;
  const bom = deriveDrawingBom(document);
  assert.equal(bom.lines.filter((line) => line.category === 'glass').length, 2);
  assert.equal(bom.lines.filter((line) => line.sourceComponentId.startsWith('installation.surround.')).length, 14);
  assert.equal(byComponent(bom, 'installation.surround.corner_connector').quantity, 16);
  assert.ok(bom.lines.every((line) => line.dimensions.basis !== 'reference-rule'));
});

test('malformed snapshots are rejected before BOM derivation', () => {
  const document = seed();
  document.windows[0].widthMm = 0;
  assert.throws(() => deriveDrawingBom(document));
});

test('headless BOM mode returns the same deterministic snapshot as the native adapter', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'doormes-bom-'));
  try {
    const source = join(directory, 'input.json'), output = join(directory, 'output.json');
    const document = hingedDocument();
    await writeFile(source, JSON.stringify({ mode: 'bom', document }));
    const result = spawnSync(process.execPath, ['dist/validator.mjs', source, output], { timeout: 20000 });
    assert.equal(result.status, 0, result.stderr?.toString());
    assert.deepEqual(JSON.parse(await readFile(output, 'utf8')), { ok: true, document: deriveDrawingBom(document) });
  } finally {
    assert.equal(dirname(directory), tmpdir());
    assert.match(basename(directory), /^doormes-bom-/);
    await rm(directory, { recursive: true, force: true });
  }
});
