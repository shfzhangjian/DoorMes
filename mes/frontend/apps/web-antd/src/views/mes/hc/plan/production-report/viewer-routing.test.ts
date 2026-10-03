import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({ page: vi.fn(), open: vi.fn(), error: vi.fn() }));
vi.mock('#/api/mes/hc/execution/press-slot-console', () => ({ getPressSlotFormRecordPage: api.page }));
vi.mock('ant-design-vue', () => ({ message: { error: api.error } }));
vi.mock('../../base/process-form-fill/press-slot/index.vue', () => ({
  default: { name: 'ExistingFormViewer', props: ['viewerOnly'], setup(_: unknown, { expose }: any) { expose({ openDetail: api.open }); }, template: '<div />' },
}));
vi.mock('../../base/process-form-fill/adhesive2/index.vue', () => ({
  default: { name: 'ExistingFormViewer', props: ['viewerOnly'], setup(_: unknown, { expose }: any) { expose({ openDetail: api.open }); }, template: '<div />' },
}));
import Viewer from './modules/ProductionFormViewer.vue';

describe('完整表单业务来源匹配', () => {
  it('压槽中间品按明确绑定找到原表，不误取其他表同号数据', async () => {
    api.open.mockClear();
    const matching = { planNo: 'P1', id: 'INTERMEDIATE-96', sourceId: 96, sourceType: 'INTERMEDIATE_RECORD' };
    api.page.mockResolvedValue({ total: 3, list: [
      { planNo: 'P1', id: 'STATION-1061', sourceId: 1061, sourceType: 'STATION_RECORD' },
      { ...matching, planNo: 'P10' }, matching,
    ] });
    const wrapper = mount(Viewer);
    try {
      await wrapper.vm.open({ id: 1061, sourceType: 'PROCESS_FORM', contextJson: '{"bindType":"PRESS_SLOT_INTERMEDIATE_RECORD","sourceId":96,"sourceProcess":"PRESS_SLOT"}' }, '压槽', 'P1');
      expect(api.open).toHaveBeenCalledExactlyOnceWith(matching);
      expect(wrapper.findComponent({ name: 'ExistingFormViewer' }).props('viewerOnly')).toBe(true);
    } finally { wrapper.unmount(); }
  });

  it('粘胶2中间品使用原业务记录 ID 和只读查看模式', async () => {
    api.open.mockClear();
    const wrapper = mount(Viewer);
    try {
      await wrapper.vm.open({ id: 1138, sourceType: 'PROCESS_FORM', templateName: '粘胶2中间品记录表', contextJson: '{"bindType":"ADHESIVE2_INTERMEDIATE_RECORD","sourceId":83,"sourceProcess":"ADHESIVE2"}' }, '粘胶2', 'P1');
      const row = api.open.mock.calls[0]![0];
      expect(row.adhesive2IntermediateRecordId).toBe(83);
      expect(JSON.parse(row.headerDataJson).recordSource).toBe('ADHESIVE2_INTERMEDIATE_BUSINESS');
      expect(api.error).not.toHaveBeenCalled();
    } finally { wrapper.unmount(); }
  });
});
