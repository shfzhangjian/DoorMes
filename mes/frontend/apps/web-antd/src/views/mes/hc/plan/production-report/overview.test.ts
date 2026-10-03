import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

const api = vi.hoisted(() => ({ page: vi.fn(), detail: vi.fn() }));
vi.mock('#/api/mes/hc/production-report', () => ({
  getProcessReportOverviewPage: api.page,
  getProcessReportOverviewDetail: api.detail,
}));
vi.mock('#/api/mes/hc/planorder', () => ({ exportPlanProcessPivot: vi.fn() }));
vi.mock('@vben/common-ui', () => ({
  Page: { template: '<div><slot /></div>' },
}));
vi.mock('@vben/icons', () => ({ IconifyIcon: { template: '<span />' } }));
vi.mock('@vben/utils', () => ({ downloadFileFromBlobPart: vi.fn() }));
vi.mock('#/adapter/vxe-table', async () => {
  const { defineComponent, h, onMounted, ref } = await import('vue');
  return {
    useVbenVxeGrid: (options: any) => {
      const rows = ref<any[]>([]);
      const run = async () => {
        const result = await options.gridOptions.proxyConfig.ajax.query({
          page: { currentPage: 1, pageSize: 15 },
        });
        rows.value = result.list;
      };
      return [
        defineComponent({
          setup(_, { slots }) {
            onMounted(run);
            return () =>
              h(
                'div',
                rows.value.map((row) =>
                  h('div', { key: row.pivotRowKey }, [
                    slots.planStatus?.({ row }),
                    slots.WET?.({ row }),
                    slots.actions?.({ row }),
                  ]),
                ),
              );
          },
        }),
        { query: run, reload: run },
      ];
    },
  };
});
vi.mock('./modules/ProductionFormViewer.vue', () => ({
  default: { template: '<div />' },
}));
// 保留真实 Tabs，覆盖页签数值 ID 和字符串选中值不匹配的回归。
vi.mock('ant-design-vue', async (load) => ({
  ...(await load<object>()),
  Drawer: { props: ['open'], template: '<div v-if="open"><slot /></div>' },
  Spin: { template: '<div><slot /></div>' },
  DatePicker: { template: '<span />' },
  Select: { template: '<span />' },
}));
import Overview from './index.vue';

let wrapper: ReturnType<typeof mount>;
afterEach(() => {
  wrapper?.unmount();
  vi.clearAllMocks();
});
async function render() {
  api.page.mockResolvedValue({
    total: 1,
    list: [
      {
        id: 120,
        pivotRowKey: 'PLAN|120|A',
        planNo: '20260911-001',
        planStatus: 'RELEASED',
      },
    ],
  });
  api.detail.mockResolvedValue({
    plan: { id: 120, planNo: '20260911-001' },
    operations: [
      {
        id: 960,
        opName: '配料',
        formCount: 1,
        productionRecordCount: 1,
        productionRecords: [
          {
            id: 700,
            sourceType: 'FORMULA_PRODUCTION_RECORD',
            productionData: {
              inputWeight: 100,
              outputWeight: 98,
              filterBatchNo: 'FILTER-01',
              mixerEquipmentCode: 'MIXER-01',
            },
            reportDate: '2026-09-11',
            modelCode: 'CMP-001',
            padType: 'WHITE_PAD',
            materialCode: 'MAT-001',
            productionBatchNo: 'FORMULA-20260911-001',
            feedQty: 100,
            inputUom: 'kg',
            outputQty: 98,
            outputUom: 'kg',
            goodQty: 98,
            reportStatus: 'RECORDED',
            recorderName: '李四',
            reportTime: '2026-09-11 15:20:00',
            remark: '配料备注',
          },
        ],
        forms: [
          {
            id: 1,
            sourceType: 'STATION_RECORD',
            templateName: '配料生产点检表',
            items: [{ id: 11, actualValue: '1.05' }],
          },
        ],
      },
      {
        id: 961,
        opName: '湿法',
        formCount: 1,
        productionRecordCount: 1,
        productionRecords: [
          {
            id: 701,
            sourceType: 'WET_PRODUCTION_RECORD',
            reportDate: '2026-09-11',
            modelCode: 'CMP-001',
            padType: 'BLACK_PAD',
            materialCode: 'MAT-002',
            productionBatchNo: 'WET-20260911-001',
            feedQty: 120,
            inputUom: 'kg',
            outputQty: 118,
            outputUom: 'm',
            goodQty: 118,
            reportStatus: 'CONFIRMED',
            recorderName: '张三',
            reportTime: '2026-09-11 16:20:00',
            detailJson: '{"batchNo":"WET-20260911-001"}',
          },
        ],
        forms: [
          {
            id: 1,
            sourceType: 'PROCESS_FORM',
            templateName: '烘箱半成品记录表',
            items: [{ id: 12, actualValue: '1.062' }],
          },
        ],
      },
    ],
  });
  wrapper = mount(Overview, { attachTo: document.body });
  await flushPromises();
}

describe('工序综合报表详情', () => {
  it('打开详情后能选中真实页签并展示首张表单', async () => {
    await render();
    expect(wrapper.text()).not.toContain('0 张已保存表单');
    await wrapper.find('[data-action="view-all"]').trigger('click');
    await flushPromises();
    expect(wrapper.find('[role="tab"][aria-selected="true"]').text()).toContain(
      '配料',
    );
    expect(wrapper.find('.form-detail').text()).toContain('配料生产点检表');
    expect(wrapper.find('.form-detail').text()).toContain('1.05');
    expect(wrapper.text()).toContain('生产表单 2 张');
  });

  it('切换工序自动选首张表单，同号不同来源不串记录', async () => {
    await render();
    await wrapper.find('[data-action="view-all"]').trigger('click');
    await flushPromises();
    const tab = wrapper
      .findAll('[role="tab"]')
      .find((item) => item.text().includes('湿法'))!;
    await tab.trigger('click');
    await nextTick();
    expect(wrapper.find('[role="tab"][aria-selected="true"]').text()).toContain(
      '湿法',
    );
    const pane = wrapper.find('.form-detail');
    expect(pane.text()).toContain('烘箱半成品记录表');
    expect(pane.text()).toContain('1.062');
    expect(pane.text()).toContain('查看完整表单');
  });

  it('生产记录页签仅展示工序生产记录服务数据并隐藏原始报工追溯', async () => {
    await render();
    await wrapper.find('[data-action="view-all"]').trigger('click');
    await flushPromises();
    const tab = wrapper
      .findAll('[role="tab"]')
      .find((item) => item.text().includes('湿法'))!;
    await tab.trigger('click');
    await nextTick();
    const reportTab = wrapper
      .findAll('[role="tab"]')
      .find((item) => item.text().includes('报工 / 生产记录'))!;
    await reportTab.trigger('click');
    await nextTick();
    expect(wrapper.text()).toContain(
      '生产记录表（复用各工序生产记录页面口径）',
    );
    expect(wrapper.text()).toContain('日期');
    expect(wrapper.text()).toContain('型号');
    expect(wrapper.text()).toContain('类型');
    expect(wrapper.text()).toContain('料号');
    expect(wrapper.text()).toContain('批号');
    expect(wrapper.text()).toContain('备注');
    expect(wrapper.text()).toContain('FORMULA-20260911-001');
    expect(wrapper.text()).toContain('CMP-001');
    expect(wrapper.text()).toContain('白垫');
    expect(wrapper.text()).toContain('配料备注');
    expect(wrapper.text()).toContain('投料重量(kg)');
    expect(wrapper.text()).toContain('FILTER-01');
    expect(wrapper.text()).toContain('MIXER-01');
    expect(wrapper.text()).not.toContain('角色/类型');
    expect(wrapper.text()).not.toContain('原始报工追溯（按来源展开）');
  });

  it('接口失败显示明确错误', async () => {
    await render();
    api.detail.mockRejectedValueOnce(new Error('详情查询失败'));
    await wrapper.find('[data-action="view-all"]').trigger('click');
    await flushPromises();
    expect(wrapper.text()).toContain('详情查询失败');
  });

  it('点击湿法单元格直接打开对应工序及首张表单', async () => {
    await render();
    await wrapper.find('.stage-cell').trigger('click');
    await flushPromises();
    expect(wrapper.find('[role="tab"][aria-selected="true"]').text()).toContain(
      '湿法',
    );
    expect(wrapper.find('.form-detail').text()).toContain('1.062');
  });

  it('计划状态显示使用计划字典，清除日期标签同步实际查询', async () => {
    await render();
    expect(wrapper.find('.report-status-tag').text()).toBe('下达');
    expect(api.page.mock.lastCall?.[0].productionStartDateStart).toMatch(
      /^\d{4}-\d{2}-\d{2}$/,
    );
    await wrapper.find('[aria-label="清除生产开始日期起"]').trigger('click');
    await flushPromises();
    expect(
      api.page.mock.lastCall?.[0].productionStartDateStart,
    ).toBeUndefined();
    expect(api.page.mock.lastCall?.[0].pageNo).toBe(1);
  });
});
