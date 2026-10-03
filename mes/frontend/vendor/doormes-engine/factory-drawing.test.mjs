import test from 'node:test';
import assert from 'node:assert/strict';
import { createFactoryDrawingPreview, deriveDrawingBom, listFactoryDrawingSubjects, seedDocument, validateDocument } from './dist/engine.mjs';

const designId = 'eaec5d47-9cb1-402e-a4d1-4dfb0797b7c5';
const seed = (overrides = {}) => seedDocument({ designId, lineId: '71eaedfe-ff46-4efc-aeba-c947faf232cb', mark: 'C1', quantity: 2, widthMm: 1200, heightMm: 1500, ...overrides });
const options = { drawingNumber: 'DW-FACTORY-021', name: '门窗核对图', revision: 8 };
const escape = (value) => value.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&apos;');
const visibleText = (svg) => [...svg.matchAll(/<text\b[^>]*>([\s\S]*?)<\/text>/g)].map((match) => match[1].replace(/<[^>]+>/g, '')).join('');
const rowMarkup = (pages, id) => pages.map((page) => page.svg).join('').match(new RegExp(`<g class="technical-table-row" data-row-id="${escape(id).replace(/[.*+?^${}()|[\]\\]/g,'\\$&')}"[\\s\\S]*?<\\/g>`))?.[0];

test('saved factory projection is deterministic, detached and prints MES business revision', () => {
  const document = seed();
  const baseline = JSON.stringify(document);
  const preview = createFactoryDrawingPreview(document, options);
  assert.equal(JSON.stringify(document), baseline);
  assert.deepEqual(createFactoryDrawingPreview(document, options), preview);
  assert.equal(preview.revision, 8);
  assert.equal(preview.revisionLabel, 'R8');
  assert.ok(preview.pages.length >= 2);
  assert.equal(preview.pages[0].kind, 'drawing');
  assert.ok(preview.pages.some((page) => page.kind === 'components'));
  preview.pages.forEach((page, index) => {
    assert.equal(page.pageNumber, index + 1);
    assert.equal(page.pageCount, preview.pages.length);
    assert.equal(page.widthMm, 420);
    assert.equal(page.heightMm, 297);
    assert.match(page.svg, /^<svg xmlns="http:\/\/www.w3.org\/2000\/svg"/);
    assert.match(page.svg, /data-drawing-number="DW-FACTORY-021"/);
    assert.match(page.svg, /data-drawing-version="R8"/);
    assert.match(page.svg, /业务版本 R8/);
    assert.match(page.svg, /用途 设计参考/);
    assert.equal(page.svg.includes('设计修订 r1'), false);
    assert.equal(visibleText(page.svg).includes(designId), false);
    assert.equal(visibleText(page.svg).includes('71eaedfe-ff46-4efc-aeba-c947faf232cb'), false);
    assert.equal('sheet' in page, false);
  });
  assert.equal(new Set(preview.pages.map((page) => page.id)).size, preview.pages.length);
});

test('factory schedules use exactly the order BOM identities, numbers, quantities and notes', () => {
  const document = seed();
  document.windows[0].designComponentRemarks.profile = '框型材检查备注';
  document.windows[0].designComponentRemarks.glass = '玻璃核对备注';
  const bom = deriveDrawingBom(document);
  const preview = createFactoryDrawingPreview(document, options);
  for (const line of bom.lines) {
    const markup = rowMarkup(preview.pages, line.objectId);
    assert.ok(markup, `missing BOM row ${line.objectId}`);
    assert.ok(markup.includes(escape(line.displayCode)));
    assert.ok(markup.includes(escape(line.modelCode)));
    assert.ok(markup.includes(`${line.quantity} ${line.unit === 'pcs' ? '件' : line.unit === 'set' ? '套' : 'm'}`));
    if (line.remark) assert.ok(markup.includes(escape(line.remark)));
  }
  const rows = preview.pages.flatMap((page) => [...page.svg.matchAll(/data-row-id="([^"]+)"/g)].map((match) => match[1]));
  assert.deepEqual(rows.sort(), bom.lines.map((line) => escape(line.objectId)).sort());
  const numbers = new Set(bom.lines.map((line) => line.displayCode));
  const calloutTexts = preview.pages.flatMap((page) => [...page.svg.matchAll(/<g class="technical-callout technical-callout--component-callout"[\s\S]*?<\/g>/g)].map((match) => visibleText(match[0])));
  assert.ok(calloutTexts.length >= 5);
  assert.ok(calloutTexts.every((value) => numbers.has(value)), calloutTexts.join(', '));
});

test('drawing and component tables occupy separate pages, without production state or route tables', () => {
  const preview = createFactoryDrawingPreview(seed(), options);
  for (const page of preview.pages) {
    if (page.kind === 'drawing') { assert.ok(page.svg.includes('class="technical-view ')); assert.equal(page.svg.includes('data-table-kind='), false); }
    else { assert.equal(page.svg.includes('class="technical-view '), false); assert.ok(page.svg.includes('data-table-kind="design-selection"')); }
    assert.doesNotMatch(visibleText(page.svg), /下料\(mm\)|生产数据状态|工序|冻结说明/);
  }
});

test('multiple-window scope selection uses only requested windows and validates exact identities', () => {
  const document = seed();
  document.windows.push(seed({ lineId: 'second-window', mark: 'C2' }).windows[0]);
  const subjects = listFactoryDrawingSubjects(document);
  assert.equal(subjects.length, 2);
  assert.ok(subjects.every((subject) => subject.defaultSelected));
  const preview = createFactoryDrawingPreview(document, { ...options, subjectIds: [subjects[1].id] });
  assert.ok(preview.pages.every((page) => page.subjectId === subjects[1].id));
  const source = preview.pages.map((page) => page.svg).join('');
  assert.ok(source.includes('C2-FR'));
  assert.equal(source.includes('C1-FR'), false);
  assert.throws(() => createFactoryDrawingPreview(document, { ...options, subjectIds: ['not-in-this-document'] }));
  assert.throws(() => createFactoryDrawingPreview(document, { ...options, subjectIds: [subjects[0].id, subjects[0].id] }));
  assert.throws(() => createFactoryDrawingPreview(document, { ...options, subjectIds: [] }));
});

test('assembly defaults avoid printing each contained window again', () => {
  const document = seed({ lineId: 'left', quantity: 1 });
  document.windows.push(seed({ lineId: 'right', mark: 'C2', quantity: 1 }).windows[0]);
  document.assemblies = [{ kind: 'fabrication-assembly', objectId: 'ASSEMBLY-1', mark: 'A1',
    instances: [
      { objectId: 'I1', windowId: 'W-left', transform: { xMm: 0, yMm: 0, zMm: 0, rotationYDeg: 0 } },
      { objectId: 'I2', windowId: 'W-right', transform: { xMm: 1230, yMm: 0, zMm: 0, rotationYDeg: 0 } }
    ], joints: [{ objectId: 'J1', jointType: 'mullion_joint', firstInstanceId: 'I1', firstEdge: 'right', secondInstanceId: 'I2', secondEdge: 'left', gapMm: 30, factoryScope: 'factory' }],
    openingClearance: { topMm: 10, rightMm: 10, bottomMm: 10, leftMm: 10 }, installation: structuredClone(document.windows[0].installation) }];
  const subjects = listFactoryDrawingSubjects(document);
  assert.deepEqual(subjects.filter((subject) => subject.defaultSelected).map((subject) => subject.id), ['ASSEMBLY-1']);
  const preview = createFactoryDrawingPreview(document, options);
  assert.ok(preview.pages.every((page) => page.subjectId === 'ASSEMBLY-1'));
  for (const line of deriveDrawingBom(document).lines) assert.ok(rowMarkup(preview.pages, line.objectId));
});

test('unknown manufacturing parameters remain unspecified; real design parts still receive matching rows', () => {
  const document = seed();
  document.windows[0].profileSystemId = 'NOT-MAPPED';
  const preview = createFactoryDrawingPreview(document, options);
  const bom = deriveDrawingBom(document);
  assert.equal(bom.lines.length, 5);
  for (const line of bom.lines) assert.ok(rowMarkup(preview.pages, line.objectId));
  const text = preview.pages.map((page) => visibleText(page.svg)).join('');
  assert.match(text, /非下料/);
  assert.doesNotMatch(text, /参考毛坯/);
});

test('sliding designs print their real panels and tracks with no invented manufacturing dimensions', () => {
  const document = seed();
  const cell = document.windows[0].layout.cells[0];
  Object.assign(cell, { type: 'sliding', opening: 'slide_right', hardwareSetId: 'HW-SLIDE-UNMAPPED', openingAssembly: {
    mechanism: 'sliding', panelCount: 2, activePanelCount: 1, trackCount: 2, stackSide: 'right', overlapMm: 40, openPercent: 80,
    panels: [
      { id: 'P1', label: '1号扇', role: 'active', movable: true, trackIndex: 0, closedPositionIndex: 0, operationOrder: 0, travelDirection: 'right' },
      { id: 'P2', label: '2号扇', role: 'passive', movable: false, trackIndex: 1, closedPositionIndex: 1 }
    ], operationSequence: ['P1']
  }});
  const bom = deriveDrawingBom(document);
  const preview = createFactoryDrawingPreview(document, options);
  assert.ok(bom.lines.filter((line) => line.sourceComponentId.includes('.sash.')).length === 8);
  for (const line of bom.lines) assert.ok(rowMarkup(preview.pages, line.objectId));
  const text = preview.pages.map((page) => visibleText(page.svg)).join('');
  assert.match(text, /加工尺寸未提供/);
  assert.doesNotMatch(text, /参考毛坯/);
  assert.ok(preview.pages.some((page) => page.kind === 'drawing' && page.svg.includes('C1-SA')));
  const drawing = preview.pages.filter((page) => page.kind === 'drawing').map((page) => page.svg).join('');
  for (const panel of ['P1', 'P2']) {
    assert.ok(drawing.includes(`:panel.${panel}:sliding-sash`));
    assert.ok(drawing.includes(`:panel.${panel}:sliding-actual-plan`));
  }
  assert.ok(drawing.includes(':track.1:track-plan'));
  assert.ok(drawing.includes(':track.2:track-plan'));
  assert.match(visibleText(drawing), /轨道示意/);
});

test('long notes continue onto printable rows and all user text is safely escaped', () => {
  const document = seed();
  const note = '很长的构件备注需要完整保留。'.repeat(30) + '最终备注END';
  document.windows[0].designComponentRemarks.glass = note;
  document.windows[0].designComponentRemarks.profile = note;
  document.factoryDrawingElementOptions = [{ objectId: `${document.windows[0].objectId}:frame.top`, factoryDrawingNumber: 'CUSTOM-1234567890123456789012345', showInComponentTable: true, showDimensions: true }];
  const preview = createFactoryDrawingPreview(document, { ...options, name: '<script>用户名称</script>', remark: '图纸备注 <img src=x onerror="alert(1)"> & 特殊字符' });
  const source = preview.pages.map((page) => page.svg).join('');
  assert.doesNotMatch(source, /<script\b|<img\b|<[^>]+\sonerror=/);
  assert.ok(source.includes('&lt;script&gt;'));
  assert.ok(source.includes('&lt;img'));
  assert.match(source, /text-continuation/);
  assert.match(visibleText(source), /最终备注END/);
  assert.ok(preview.pages.length >= 3);
  const ids = [...source.matchAll(/(?:\s)id="([^"]+)"/g)].map((match) => match[1]);
  assert.equal(new Set(ids).size, ids.length);
});

test('invalid saved documents and metadata are rejected without partial preview output', () => {
  const document = seed(); document.windows[0].widthMm = 0;
  assert.throws(() => createFactoryDrawingPreview(document, options));
  assert.throws(() => createFactoryDrawingPreview(seed(), { ...options, revision: 0 }));
  assert.throws(() => createFactoryDrawingPreview(seed(), { ...options, drawingNumber: '' }));
  assert.throws(() => createFactoryDrawingPreview(seed(), { ...options, name: 'x'.repeat(201) }));
  assert.throws(() => createFactoryDrawingPreview(seed(), { ...options, remark: 'x'.repeat(2001) }));
  assert.deepEqual(validateDocument(seed()), seed());
});
