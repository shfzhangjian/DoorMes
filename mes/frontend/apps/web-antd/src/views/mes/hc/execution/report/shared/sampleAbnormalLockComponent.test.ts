import { mount, flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
const { getLock, warning } = vi.hoisted(() => ({ getLock: vi.fn(), warning: vi.fn() }));
vi.mock('ant-design-vue', () => ({ Button: { template: '<button><slot /></button>' }, Modal: { warning }, Spin: { template: '<i />' }, Tag: { template: '<span><slot /></span>' }, message: { success: vi.fn() } }));
vi.mock('@vben/icons', () => ({ IconifyIcon: { template: '<i />' } }));
vi.mock('#/api/mes/quality/sample-abnormal-recheck', () => ({ getActiveSampleAbnormalLock: getLock, createSampleAbnormalRecheck: vi.fn() }));
vi.mock('#/views/mes/quality/fai/shared/faiPrint', () => ({ printFaiInspectionTransferTicketById: vi.fn() }));
import Guard from '#/views/mes/quality/sample-abnormal-recheck/components/QmsSampleAbnormalLockGuard.vue';
const wet = { sourceProcessCode: 'WET', objectType: 'MOTHER_ROLL', objectNo: 'W26H162A' };
const adhesive = { sourceProcessCode: 'ADHESIVE1', objectType: 'SEGMENT', objectNo: 'W26H162AP' };
beforeEach(() => { getLock.mockReset(); warning.mockReset(); });
describe('留样异常提醒组件', () => {
  it('后移锁保留提示和原记录，但不再阻断操作或展示旧禁止报工文案', async () => {
    getLock.mockResolvedValue({ ...wet, id: 1, abnormalInspectionNo: 'FAI-1', lockReason: '锁定不允许继续报工' });
    const wrapper = mount(Guard, { props: { deferToCutRound: true, candidates: [wet] }, slots: { default: ({ locked }: { locked: boolean }) => locked ? 'BLOCKED' : 'CAN_WORK' } });
    await flushPromises();
    expect(wrapper.text()).toContain('CAN_WORK');
    expect(wrapper.text()).toContain('允许继续加工');
    expect(wrapper.text()).not.toContain('锁定不允许继续报工');
    expect(wrapper.classes()).toContain('qms-sample-abnormal-lock-guard--deferred');
    expect(await wrapper.vm.ensureUnlocked()).toBe(true);
    expect(warning).not.toHaveBeenCalled();
    wrapper.unmount();
  });
  it('湿法提醒不能遮蔽同一来源上的粘胶1阻断锁', async () => {
    getLock.mockImplementation(async (candidate) => ({ ...candidate, id: 1 }));
    const wrapper = mount(Guard, { props: { deferToCutRound: true, candidates: [wet, adhesive] } });
    await flushPromises();
    expect(wrapper.classes()).toContain('qms-sample-abnormal-lock-guard--locked');
    expect(wrapper.classes()).not.toContain('qms-sample-abnormal-lock-guard--deferred');
    expect(await wrapper.vm.ensureUnlocked()).toBe(false);
    wrapper.unmount();
  });
  it('未显式启用后移策略的组件保持原限制', async () => {
    getLock.mockResolvedValue({ ...wet, id: 1 });
    const wrapper = mount(Guard, { props: { candidates: [wet] } });
    await flushPromises();
    expect(await wrapper.vm.ensureUnlocked()).toBe(false);
    expect(wrapper.classes()).toContain('qms-sample-abnormal-lock-guard--locked');
    wrapper.unmount();
  });
});
