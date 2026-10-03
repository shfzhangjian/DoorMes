import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { createMemoryHistory, createRouter, isNavigationFailure, NavigationFailureType } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DesignCenter from '../../apps/web-antd/src/views/doormes/designs/index.vue';
import DrawingWorkspace from '../../apps/web-antd/src/views/doormes/drawing-workspace/index.vue';
import type { RequirementDocument, RequirementLine } from '../../apps/web-antd/src/api/doormes/requirements';
import LayoutContent from '../../packages/effects/layouts/src/basic/content/content.vue';
import { Modal, modalState, Table, Dropdown, Pagination, resetTestIdentity } from './mocks';

// S: Real Vue DesignCenter -> real DrawingWorkspace, mocked transport and engine.
// This suite does not contact an HTTP endpoint, database, or browser/WebGL.
const controlled = vi.hoisted(() => ({
  documents: [] as any[],
  engineDocument: null as any,
  onChange: null as null | ((document: any) => void),
  mounts: 0, disposals: 0,
  list: vi.fn(), create: vi.fn(), get: vi.fn(), versions: vi.fn(), save: vi.fn(), find: vi.fn(), open: vi.fn(), factory:vi.fn(),
}));
vi.mock('#/api/doormes/drawings', () => ({
  listDrawings: controlled.list, createDrawing: controlled.create,
  getDrawing: controlled.get, getDrawingVersions: controlled.versions, saveDrawing: controlled.save,
  findDrawing: controlled.find, openDrawing: controlled.open,
}));
vi.mock('#/api/doormes/catalog', () => ({ listCatalog: async () => ({ list: [], total: 0 }) }));
vi.mock('#/api/doormes/assets', () => ({ assetTransport: {} }));
vi.mock('../../packages/effects/layouts/src/iframe', () => ({ IFrameRouterView: defineComponent({ render: () => null }) }));
vi.mock('../../vendor/doormes-engine/dist/engine.mjs', () => ({
  createFactoryDrawingPreview:controlled.factory,
  validateDocument: (document: any) => structuredClone(document),
  mountDrawing(host: HTMLElement, document: any, onDocumentChange: (value: any) => void) {
    controlled.mounts++; controlled.engineDocument = structuredClone(document); controlled.onChange = onDocumentChange;
    host.append(globalThis.document.createElement('span'));
    return {
      getDocument: () => structuredClone(controlled.engineDocument), applyCatalog: vi.fn(),
      dispose: () => { controlled.disposals++; host.replaceChildren(); },
    };
  },
}));

const copied = <T>(value: T): T => structuredClone(value);
const mounted: ReturnType<typeof mount>[] = [];
const sourceFile = resolve('apps/web-antd/src/views/doormes/designs/index.vue');
beforeEach(() => {
  // PRE-01: Controlled tenant 1 / designer 1003 uses only design-create/save.
  resetTestIdentity();
  controlled.documents = []; controlled.mounts = 0; controlled.disposals = 0; controlled.onChange = null;
  modalState.allowLeave = false; modalState.confirmations = 0;
  [controlled.list, controlled.create, controlled.get, controlled.versions, controlled.save, controlled.find, controlled.open,controlled.factory].forEach((mock) => mock.mockReset());
  controlled.factory.mockImplementation((_document,options)=>({...options,revisionLabel:`R${options.revision}`,subjects:[],warnings:[],pages:[{id:'factory-1',title:'图形页',kind:'drawing',widthMm:420,heightMm:297,svg:'<svg xmlns="http://www.w3.org/2000/svg"><text>C1</text></svg>'},{id:'factory-2',title:'组成件表',kind:'components',widthMm:420,heightMm:297,svg:'<svg xmlns="http://www.w3.org/2000/svg"><text>C1-F01</text></svg>'}]}));
  controlled.list.mockImplementation(async (params: any) => {
    const head = controlled.documents.at(-1);
    return { list: head ? [{ id: head.id, revision: head.revision, status: head.status, ...head.metadata,
      createdBy: 1003, updatedAt: head.updatedAt, editable: true }] : [], total: head ? 1 : 0 };
  });
  controlled.create.mockImplementation(async (input: any) => {
    const snapshot = { schemaVersion: 'doormes-drawing-snapshot.v1', id: 'drawing-controlled-1', tenantId: 1,
      requirementId: null, requirementRevision: 0, lineId: null, revision: 1, status: 'DRAFT', createdBy: 1003,
      changedBy: 1003, createdAt: '2026-10-02T12:00:00Z', updatedAt: '2026-10-02T12:00:00Z', changeNote: '创建独立设计',
      metadata: { number: input.number || 'D-0001', name: input.name, note: input.note || '', source: 'INDEPENDENT' }, editable: true,
      document: { schemaVersion: 'doormes-domain.v1', designId: 'controlled-design', revision: 1,
        windows: [{ objectId: 'window-1', mark: input.mark, width: input.widthMm, height: input.heightMm, quantity: input.quantity }] },
    };
    controlled.documents.push(copied(snapshot)); return copied(snapshot);
  });
  controlled.get.mockImplementation(async (id: string, revision?: number) => {
    const snapshot = revision ? controlled.documents.find((value) => value.revision === revision) : controlled.documents.at(-1);
    if (!snapshot || snapshot.id !== id) throw new Error('controlled drawing not found');
    return copied(snapshot);
  });
  controlled.versions.mockImplementation(async () => controlled.documents.toReversed().map((snapshot) => ({
    revision: snapshot.revision, changeNote: snapshot.changeNote, changedBy: snapshot.changedBy, updatedAt: snapshot.updatedAt,
  })));
  controlled.save.mockImplementation(async (id: string, expectedRevision: number, changeNote: string, document: any, metadata: any) => {
    const head = controlled.documents.at(-1);
    if (!head || head.id !== id || head.revision !== expectedRevision) throw new Error('controlled version conflict');
    const snapshot = { ...copied(head), revision: head.revision + 1, document: copied(document),
      metadata: { ...head.metadata, ...metadata }, changeNote, updatedAt: '2026-10-02T12:01:00Z' };
    controlled.documents.push(copied(snapshot)); return copied(snapshot);
  });
});
afterEach(() => {
  // CLEAN-01: Only isolated memory fixtures and mounted DOM are discarded.
  mounted.splice(0).forEach((wrapper) => wrapper.unmount()); document.body.replaceChildren();
  resetTestIdentity(); vi.restoreAllMocks();
});
async function settled() { await flushPromises(); await nextTick(); await new Promise((resolve) => setTimeout(resolve, 80)); await flushPromises(); }
async function setup() {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/factory/drawings', name: 'drawings', meta: { keepAlive: false }, component: DesignCenter },
    { path: '/factory/catalog', name: 'catalog', meta: { keepAlive: false }, component: defineComponent({ render: () => h('main', { 'data-page': 'catalog' }, '目录') }) },
  ] });
  await router.push('/factory/drawings'); await router.isReady();
  const wrapper = mount(LayoutContent, { attachTo: document.body, global: { plugins: [router], stubs: { transition: false, keepAlive: false } } });
  mounted.push(wrapper); await settled(); return { wrapper, router };
}
function click(root: Element, text: string) {
  const target = Array.from(root.querySelectorAll('button')).find((button) => button.textContent?.trim() === text);
  expect(target, `missing button ${text}`).toBeDefined(); return new DOMWrapper(target!).trigger('click');
}
function dialog(title: string) { return document.querySelector(`[role="dialog"][aria-label="${title}"]`)!; }
function workspace() { return document.querySelector('.mes-drawing-workspace')!; }
async function field(root: Element, label: string, value: string) {
  const target = Array.from(root.querySelectorAll('label')).find((element) => element.textContent?.trim() === label)?.querySelector('input,textarea');
  expect(target, `missing input ${label}`).toBeDefined(); await new DOMWrapper(target!).setValue(value);
}
async function createInUi() {
  // ACT-01..04: Open menu, open form, enter real bound fields, submit.
  await click(document.querySelector('.design-header')!, '新建图纸 ▾');
  await click(document.querySelector('.design-header')!, '新建独立设计'); await settled();
  const creation = dialog('新建独立设计'); expect(creation).not.toBeNull();
  await field(creation, '图纸编号', 'CUSTOM-01'); await field(creation, '图纸名称', '自定义门窗');
  await field(creation, '图纸备注', '受控组件链测试');
  await click(creation, '创建并进入设计'); await settled();
  expect(controlled.create).toHaveBeenCalledWith(expect.objectContaining({ number: 'CUSTOM-01', name: '自定义门窗', widthMm: 1200, heightMm: 1500 }));
  await vi.waitFor(() => expect(workspace()).not.toBeNull(), { timeout: 5000, interval: 20 });
  await vi.waitFor(() => expect(controlled.mounts).toBeGreaterThan(0), { timeout: 5000, interval: 20 });
}
async function changedGeometry(width: number) {
  controlled.engineDocument.windows[0].width = width;
  controlled.onChange!(copied(controlled.engineDocument)); await nextTick();
}
async function saveInUi(reason: string) {
  await new DOMWrapper(workspace().querySelector('input[placeholder="本次修订原因（尺寸、构造、材料等）"]')!).setValue(reason);
  await click(workspace(), '保存图纸新版本'); await settled();
}
async function rowMenu(action: string) {
  const cell = document.querySelector('.design-table td[data-column="operation"]')!;
  await click(cell, '操作 ▾'); await click(cell, action); await settled();
}

describe('S-DESIGN-UI-v1: real design components with controlled transport/engine', () => {
  it('ASSERT-01 full-parent table, bottom pagination, right-fixed Dropdown and popup-only detail forms', async () => {
    const { wrapper } = await setup();
    const table = wrapper.findComponent(Table);
    expect(table.props('pagination')).toBe(false);
    expect(table.props('scroll')).toEqual(expect.objectContaining({ x: 1210, y: expect.any(Number) }));
    expect(table.props('columns')).toContainEqual(expect.objectContaining({ key: 'operation', fixed: 'right' }));
    expect(wrapper.find('.design-pagination').findComponent(Pagination).exists()).toBe(true);
    expect(wrapper.find('.design-header').findComponent(Dropdown).exists()).toBe(true);
    expect(wrapper.find('.drawing-create-form').exists()).toBe(false);
    const source = readFileSync(sourceFile, 'utf8');
    expect(source).toContain('.design-center{height:100%;min-height:0;display:flex;flex-direction:column');
    expect(source).toContain('.design-table{flex:1;min-height:0;');
    expect(source).toContain('.design-pagination{flex:none;');
    expect(source).not.toContain('placeholder-card');
  });

  it('ACT-01..14 / ASSERT-02 create -> metadata + geometry -> save -> close -> reopen -> change -> history read-only', async () => {
    await setup(); await createInUi();
    expect(controlled.mounts).toBe(1);
    await click(workspace(), '图纸信息'); await settled();
    await field(workspace(), '图纸编号', 'CUSTOM-RENAMED');
    await field(workspace(), '图纸名称', '人工修改名称'); await field(workspace(), '图纸备注', '人工可维护备注');
    await click(dialog('图纸信息'), '确认修改'); await settled();
    await changedGeometry(1320); await saveInUi('调整尺寸与图纸信息');
    expect(controlled.save).toHaveBeenLastCalledWith('drawing-controlled-1', 1, '调整尺寸与图纸信息',
      expect.objectContaining({ windows: [expect.objectContaining({ width: 1320 })] }),
      { number: 'CUSTOM-RENAMED', name: '人工修改名称', note: '人工可维护备注' });
    expect(controlled.documents).toHaveLength(2);
    expect(controlled.documents[0].metadata.number).toBe('CUSTOM-01');
    expect(controlled.documents[0].document.windows[0].width).toBe(1200);
    await click(workspace(), '返回图纸中心'); await settled();
    expect(workspace()).toBeNull(); expect(controlled.disposals).toBe(1);
    await rowMenu('查看明细');
    const detail = dialog('CUSTOM-RENAMED · 图纸明细');
    expect(detail.textContent).toContain('人工修改名称'); expect(detail.textContent).toContain('人工可维护备注');
    await click(detail, '关闭'); await settled();
    await rowMenu('打开设计'); expect(controlled.engineDocument.windows[0].width).toBe(1320);
    await changedGeometry(1450); await saveInUi('第二次尺寸调整');
    expect(controlled.documents).toHaveLength(3);
    await click(workspace(), '返回图纸中心'); await settled(); await rowMenu('版本历史');
    const history = dialog('CUSTOM-RENAMED · 版本历史'); expect(history).not.toBeNull();
    const row = Array.from(history.querySelectorAll('tr')).find((element) => element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
    await click(row, '操作 ▾');
    await click(row, '预览历史'); await settled();
    expect(controlled.engineDocument.windows[0].width).toBe(1200);
    expect(workspace().textContent).toContain('历史预览');
    expect(Array.from(workspace().querySelectorAll('button')).some((button) => button.textContent === '保存图纸新版本')).toBe(false);
    await click(workspace(), '图纸信息'); await settled();
    expect(Array.from(workspace().querySelectorAll('.drawing-metadata input')).every((input) => (input as HTMLInputElement).disabled)).toBe(true);
    expect(controlled.save).toHaveBeenCalledTimes(2);
  });

  it('ASSERT-NEG-01 unsaved metadata cancels close and route navigation without removing the real workspace', async () => {
    const { router } = await setup(); await createInUi();
    await click(workspace(), '图纸信息'); await settled(); await field(workspace(), '图纸名称', '尚未保存');
    await click(dialog('图纸信息'), '确认修改'); await settled();
    const unload = new Event('beforeunload', {cancelable:true});
    window.dispatchEvent(unload);expect(unload.defaultPrevented).toBe(true);
    await click(workspace(), '返回图纸中心'); await settled();
    expect(workspace()).not.toBeNull(); expect(modalState.confirmations).toBe(1); expect(controlled.disposals).toBe(0);
    const cancelled = await router.push('/factory/catalog'); await settled();
    expect(isNavigationFailure(cancelled, NavigationFailureType.aborted)).toBe(true);
    expect(router.currentRoute.value.path).toBe('/factory/drawings'); expect(workspace()).not.toBeNull();
    modalState.allowLeave = true;
    await router.push('/factory/catalog'); await settled();
    expect(workspace()).toBeNull(); expect(document.querySelector('[data-page="catalog"]')).not.toBeNull();
    expect(controlled.save).not.toHaveBeenCalled();
    const afterClose = new Event('beforeunload', {cancelable:true});window.dispatchEvent(afterClose);
    expect(afterClose.defaultPrevented).toBe(false);
  });

  it('ACT-15..19 / ASSERT-04 opening an owned requirement mounts without a false leave prompt, then protects real changes', async () => {
    const { router } = await setup();
    // DATA-02: This designer owns an in-design requirement in the isolated tenant fixture.
    const line: RequirementLine = { id: 'LINE-OWNED', mark: 'C1', kind: 'custom', quantity: 2,
      requirement: { widthMm: 1200, heightMm: 1500, material: 'AL70', glass: 'GL-24', hardware: 'HW-TURN',
        finish: 'RAL7016', dueDate: '2026-10-30', note: '本人领用需求' } };
    const requirement: RequirementDocument = { schemaVersion: 'doormes-design-requirement.v1', id: 'REQ-OWNED', tenantId: 1,
      revision: 3, status: 'IN_DESIGN', assignedTo: 1003, createdBy: 1001, changedBy: 1003,
      createdAt: '2026-10-02T12:00:00Z', updatedAt: '2026-10-02T12:00:00Z', action: 'CLAIM', changeNote: '设计领用',
      demand: { schemaVersion: 'doormes-design-demand.v1', number: 'REQ-TEST-OWNED', customer: '受控客户', project: '组件回归', lines: [line], note: '' } };
    const snapshot = await controlled.create({ number: 'D-OWNED', name: '需求图纸', mark: 'C1', quantity: 2, widthMm: 1200, heightMm: 1500 });
    Object.assign(snapshot, { requirementId: requirement.id, requirementRevision: 3, lineId: line.id,
      metadata: { ...snapshot.metadata, source: 'REQUIREMENT' } });
    controlled.documents = [copied(snapshot)];
    controlled.find.mockResolvedValue(null);
    controlled.open.mockResolvedValue(copied(snapshot));
    router.addRoute({ path: '/factory/owned-design', component: defineComponent({
      render: () => h('main', [h(DrawingWorkspace, { requirement, line })]),
    }) });

    // ACT-15: First entry has permissions but no mounted document; it is not a dirty edit.
    await router.push('/factory/owned-design'); await settled();
    expect(modalState.confirmations).toBe(0);
    expect(controlled.find).toHaveBeenCalledWith('REQ-OWNED', 'LINE-OWNED');
    expect(controlled.open).toHaveBeenCalledWith('REQ-OWNED', 'LINE-OWNED', 3);
    expect(controlled.get).not.toHaveBeenCalled();
    expect(controlled.versions).toHaveBeenCalledWith(snapshot.id);
    expect(controlled.mounts).toBe(1);
    expect(workspace().textContent).toContain('图纸 R1 · 已保存');
    const cleanUnload = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(cleanUnload);
    expect(cleanUnload.defaultPrevented).toBe(false);

    // ACT-16..17: Only a subsequent real geometry change should trigger both guards.
    await changedGeometry(1330);
    const dirtyUnload = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(dirtyUnload);
    expect(dirtyUnload.defaultPrevented).toBe(true);
    await click(workspace(), '重新加载当前版'); await settled();
    expect(modalState.confirmations).toBe(1);
    expect(controlled.get).not.toHaveBeenCalled();
    expect(controlled.engineDocument.windows[0].width).toBe(1330);
    const cancelled = await router.push('/factory/catalog'); await settled();
    expect(isNavigationFailure(cancelled, NavigationFailureType.aborted)).toBe(true);
    expect(modalState.confirmations).toBe(2);
    expect(workspace()).not.toBeNull();

    // ACT-18..19: Explicit discard reloads the actual drawing ID and restores the saved baseline.
    modalState.allowLeave = true;
    await click(workspace(), '重新加载当前版'); await settled();
    expect(controlled.get).toHaveBeenCalledWith(snapshot.id);
    expect(controlled.engineDocument.windows[0].width).toBe(1200);
    expect(controlled.save).not.toHaveBeenCalled();
    const reloadedUnload = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(reloadedUnload);
    expect(reloadedUnload.defaultPrevented).toBe(false);
  });

  it('ASSERT-NEG-02 a discarded leave dialog cannot restart loading after the workspace was disposed', async () => {
    const { wrapper } = await setup(); await createInUi(); await changedGeometry(1330);
    let confirm: Parameters<typeof Modal.confirm>[0] | undefined;
    vi.spyOn(Modal, 'confirm').mockImplementationOnce((options) => { confirm = options; });
    const requestCount = controlled.get.mock.calls.length;
    await click(workspace(), '重新加载当前版'); await nextTick();
    expect(confirm).toBeDefined();
    // ACT-20: Simulate route teardown/HMR while the confirmation is still awaiting input.
    mounted.splice(mounted.indexOf(wrapper), 1); wrapper.unmount();
    confirm!.onOk(); await settled();
    expect(controlled.get).toHaveBeenCalledTimes(requestCount);
    expect(controlled.mounts).toBe(1);
    expect(controlled.disposals).toBe(1);
    expect(controlled.save).not.toHaveBeenCalled();
  });

  it('ASSERT-03 workspace history uses a popup with fixed-right operation menu and preserved read-only old geometry', async()=>{
    await setup();await createInUi();await changedGeometry(1600);await saveInUi('保存尺寸1600');
    await click(workspace(),'版本历史');await settled();
    const history=dialog('版本历史');expect(history).not.toBeNull();
    expect(history.querySelector('.history-pagination [data-mock-pagination]')).not.toBeNull();
    const row=Array.from(history.querySelectorAll('tr')).find(element=>element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
    expect(row.querySelector('[data-column="operation"]')?.getAttribute('data-fixed')).toBe('right');
    await click(row,'操作 ▾');await click(row,'预览历史');await settled();
    expect(dialog('版本历史')).toBeNull();expect(controlled.engineDocument.windows[0].width).toBe(1200);
    expect(workspace().textContent).toContain('历史预览');
    expect(workspace().querySelector('input[placeholder="本次修订原因（尺寸、构造、材料等）"]')).toBeNull();
    await changedGeometry(1800);expect(controlled.save).toHaveBeenCalledTimes(1);
  });

  it('ASSERT-HISTORY-01 R3 -> R1 -> current can repeat without using a history row as the drawing identity',async()=>{
    await setup();await createInUi();await changedGeometry(1320);await saveInUi('R2尺寸');await changedGeometry(1600);await saveInUi('R3尺寸');
    for(let attempt=0;attempt<3;attempt++){
      controlled.get.mockClear();controlled.versions.mockClear();
      await click(workspace(),'版本历史');await settled();
      const row=Array.from(dialog('版本历史').querySelectorAll('tr')).find(element=>element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
      await click(row,'操作 ▾');await click(row,'预览历史');await settled();
      expect(controlled.get.mock.calls).toEqual([['drawing-controlled-1'],['drawing-controlled-1',1]]);
      expect(controlled.versions).toHaveBeenLastCalledWith('drawing-controlled-1');
      expect(controlled.engineDocument.windows[0].width).toBe(1200);expect(workspace().textContent).toContain('图纸 R1 历史预览');
      expect(workspace().textContent).not.toContain('保存图纸新版本');
      await click(workspace(),'重新加载当前版');await settled();
      expect(controlled.get).toHaveBeenLastCalledWith('drawing-controlled-1');
      expect(controlled.engineDocument.windows[0].width).toBe(1600);expect(workspace().textContent).toContain('图纸 R3 · 已保存');
      expect(controlled.save).toHaveBeenCalledTimes(2);
    }
  });

  it('ASSERT-HISTORY-NEG-01 malformed head cannot issue a missing-id history request or replace the usable drawing',async()=>{
    const diagnostic=vi.spyOn(console,'error').mockImplementation(()=>{});
    await setup();await createInUi();await changedGeometry(1600);await saveInUi('R2尺寸');
    controlled.get.mockClear();controlled.versions.mockClear();
    controlled.get.mockResolvedValueOnce({code:0,data:copied(controlled.documents.at(-1))});
    await click(workspace(),'版本历史');await settled();
    const row=Array.from(dialog('版本历史').querySelectorAll('tr')).find(element=>element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
    await click(row,'操作 ▾');await click(row,'预览历史');await settled();
    expect(controlled.get.mock.calls).toEqual([['drawing-controlled-1']]);expect(controlled.versions).not.toHaveBeenCalled();
    expect(controlled.engineDocument.windows[0].width).toBe(1600);expect(controlled.disposals).toBe(0);
    expect(workspace().textContent).toContain('图纸 R2 · 已保存');expect(dialog('版本历史')).not.toBeNull();
    expect(workspace().querySelector('aside[type="error"]')?.getAttribute('message')).toContain('读取当前图纸失败：返回图纸缺少唯一编号');
    expect(diagnostic).toHaveBeenLastCalledWith('[DoorMes drawing load]','读取当前图纸','返回图纸缺少唯一编号，已停止后续版本请求。');
    // The same visible row remains retryable; neither the failed attempt nor retry saves a version.
    await click(row,'操作 ▾');await click(row,'预览历史');await settled();
    expect(controlled.get).toHaveBeenLastCalledWith('drawing-controlled-1',1);
    expect(controlled.engineDocument.windows[0].width).toBe(1200);expect(controlled.save).toHaveBeenCalledTimes(1);
    expect(dialog('版本历史')).toBeNull();
  });

  it('ASSERT-HISTORY-NEG-02 a different revision response cannot be presented as the requested historical version',async()=>{
    vi.spyOn(console,'error').mockImplementation(()=>{});
    await setup();await createInUi();await changedGeometry(1600);await saveInUi('R2尺寸');
    controlled.get.mockResolvedValueOnce(copied(controlled.documents.at(-1))).mockResolvedValueOnce(copied(controlled.documents.at(-1)));
    await click(workspace(),'版本历史');await settled();
    const row=Array.from(dialog('版本历史').querySelectorAll('tr')).find(element=>element.querySelector('[data-column="revision"]')?.textContent==='R1')!;
    await click(row,'操作 ▾');await click(row,'预览历史');await settled();
    expect(controlled.engineDocument.windows[0].width).toBe(1600);expect(controlled.disposals).toBe(0);
    expect(workspace().querySelector('aside[type="error"]')?.getAttribute('message')).toContain('读取历史 R1失败：返回图纸版本与请求不一致');
    expect(workspace().textContent).toContain('图纸 R2 · 已保存');expect(controlled.save).toHaveBeenCalledTimes(1);
  });

  it('order locked-version preview stays read-only for its designer and respects the requested revision', async()=>{
    const {router}=await setup();await createInUi();await changedGeometry(1600);await saveInUi('新版本1600');
    await click(workspace(),'返回图纸中心');await settled();
    for(const snapshot of [controlled.documents[0],controlled.documents.at(-1)]){
      const path=`/factory/locked-preview-${snapshot.revision}`;
      router.addRoute({path,component:defineComponent({render:()=>h('main',[h(DrawingWorkspace,{initialDrawing:copied(snapshot),readOnly:true,closeLabel:'返回订单'})])})});
      await router.push(path);await settled();
      expect(controlled.engineDocument.windows[0].width).toBe(snapshot.document.windows[0].width);
      expect(workspace().querySelector('input[placeholder="本次修订原因（尺寸、构造、材料等）"]')).toBeNull();
      expect(workspace().textContent).not.toContain('保存图纸新版本');
      expect(workspace().textContent).toContain('返回订单');expect(workspace().textContent).not.toContain('返回图纸中心');
      await click(workspace(),'图纸信息');await settled();
      expect(Array.from(dialog('图纸信息').querySelectorAll('input,textarea')).every(input=>(input as HTMLInputElement).disabled)).toBe(true);
      await changedGeometry(1800);expect(controlled.save).toHaveBeenCalledTimes(1);
      await router.push('/factory/drawings');await settled();
    }
  });

  it('ACT-FACTORY-01..03 factory output uses saved geometry and metadata despite unsaved edits, including historical preview',async()=>{
    await setup();await createInUi();await changedGeometry(1600);await saveInUi('已保存宽1600');
    await changedGeometry(1900);await click(workspace(),'图纸信息');await settled();await field(dialog('图纸信息'),'图纸名称','未保存名称');await click(dialog('图纸信息'),'确认修改');await settled();
    await click(workspace(),'工厂图预览');await settled();
    await vi.waitFor(()=>expect(dialog('工厂图预览')).not.toBeNull(),{timeout:5000,interval:20});await settled();
    expect(dialog('工厂图预览')).not.toBeNull();expect(controlled.get).toHaveBeenLastCalledWith('drawing-controlled-1',2);
    expect(controlled.factory).toHaveBeenLastCalledWith(expect.objectContaining({windows:[expect.objectContaining({width:1600})]}),{drawingNumber:'CUSTOM-01',name:'自定义门窗',revision:2,remark:'受控组件链测试'});
    expect(dialog('工厂图预览').querySelector('aside[type="warning"]')?.getAttribute('message')).toContain('不包含这些调整');
    expect(controlled.engineDocument.windows[0].width).toBe(1900);expect(controlled.save).toHaveBeenCalledTimes(1);
    await click(dialog('工厂图预览'),'返回图纸');await settled();
    modalState.allowLeave=true;await click(workspace(),'版本历史');await settled();const row=Array.from(dialog('版本历史').querySelectorAll('tr')).find(item=>item.querySelector('[data-column="revision"]')?.textContent==='R1')!;
    await click(row,'操作 ▾');await click(row,'预览历史');await settled();await changedGeometry(1800);
    await click(workspace(),'工厂图预览');await settled();
    expect(controlled.get).toHaveBeenLastCalledWith('drawing-controlled-1',1);
    expect(controlled.factory).toHaveBeenLastCalledWith(expect.objectContaining({windows:[expect.objectContaining({width:1200})]}),expect.objectContaining({revision:1}));
    expect(dialog('工厂图预览').querySelector('aside[type="warning"]')).not.toBeNull();expect(controlled.save).toHaveBeenCalledTimes(1);
  });
});
