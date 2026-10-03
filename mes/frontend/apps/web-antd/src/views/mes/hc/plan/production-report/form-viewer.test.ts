import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({ station: vi.fn(), process: vi.fn(), page: vi.fn() }));
vi.mock('#/api/mes/hc/stationrecord', () => ({
  getStationRecordDetail: api.station, getStationRecordPage: api.page,
}));
vi.mock('#/api/mes/hc/processform', () => ({
  getProcessFormRecordDetail: api.process, getProcessFormRecordPage: api.page,
  exportProcessFormRecordLayout: vi.fn(),
}));
vi.mock('#/adapter/vxe-table', () => ({
  useVbenVxeGrid: () => [{ template: '<div>不应挂载列表</div>' }],
}));
vi.mock('@vben/common-ui', () => ({ Page: { template: '<div><slot /></div>' } }));
vi.mock('@vben/utils', () => ({ downloadFileFromBlobPart: vi.fn() }));
vi.mock('#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue', () => ({
  default: { template: '<div><slot name="actions" /></div>' },
}));
vi.mock('ant-design-vue', async (load) => ({
  ...(await load<object>()),
  Modal: { props: ['open'], template: '<div v-if="open"><slot /></div>' },
}));
import WetViewer from '../../base/process-form-fill/wet/index.vue';

describe('复用湿法完整表单只读查看', () => {
  it('通过记录 ID 加载原表，保留半成品四列厚度，不挂载填写列表', async () => {
    api.station.mockResolvedValue({
      id: 1505, planNo: '20260911-001', batchNo: 'W26J169A',
      formName: '烘箱半成品记录表', formCode: 'WET_OVEN_SEMI_W33P0100',
      headerDataJson: '{"generatedLength":"104"}',
      items: [{ id: 1, itemSeq: 1, itemName: '1.054', standardText: '1.056', actualValue: '1.057', stepNode: '1.058' }],
    });
    const wrapper = mount(WetViewer, { props: { viewerOnly: true } });
    try {
      await wrapper.vm.openDetail({ id: 1505 });
      await flushPromises();
      expect(api.station).toHaveBeenCalledWith(1505);
      expect(wrapper.text()).toContain('烘箱半成品记录表');
      for (const value of ['1.054', '1.056', '1.057', '1.058']) expect(wrapper.text()).toContain(value);
      expect(wrapper.text()).not.toContain('不应挂载列表');
      expect(wrapper.text()).not.toContain('导入Excel');
      expect(api.page).not.toHaveBeenCalled();
    } finally { wrapper.unmount(); }
  });
});
