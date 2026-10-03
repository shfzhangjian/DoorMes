import { beforeEach, describe, expect, it, vi } from 'vitest';

const { getLock, warning } = vi.hoisted(() => ({ getLock: vi.fn(), warning: vi.fn() }));
vi.mock('ant-design-vue', () => ({ Modal: { warning } }));
vi.mock('#/api/mes/quality/sample-abnormal-recheck', () => ({ getActiveSampleAbnormalLock: getLock }));

import {
  buildSegmentChainSampleLockCandidates,
  ensureProcessingSampleAbnormalUnlocked,
  ensureSampleAbnormalUnlocked,
  findActiveProcessingSampleAbnormalLock,
} from './sampleAbnormalLockGuard';
import { processingSampleLockCandidates } from './sampleAbnormalLockPolicy';

const candidates = buildSegmentChainSampleLockCandidates({ motherBatchNo: 'W26H162A', segmentBatchNo: 'W26H162AP' });
const grindingLock = {
  sourceProcessCode: 'ROUGH_GRINDING', objectNo: 'W26H162AP', objectType: 'SEGMENT',
  lockReason: '当前分段 W26H162AP 因2026-08-26 18:46:28，磨皮 留样送检NG异常，锁定不允许继续报工，等待复检确认后继续。',
};

beforeEach(() => { getLock.mockReset(); warning.mockReset(); });
describe('生产留样NG后移裁切门禁', () => {
  it('W26H162AP旧磨皮锁文案不能再阻止开工分切', async () => {
    getLock.mockImplementation(async (candidate) => candidate.sourceProcessCode === 'ROUGH_GRINDING' ? grindingLock : null);
    expect(await ensureProcessingSampleAbnormalUnlocked(candidates, '开工分切')).toBe(true);
    expect(warning).not.toHaveBeenCalled();
    expect(getLock.mock.calls.every(([candidate]) => candidate.sourceProcessCode === 'ADHESIVE1')).toBe(true);
  });
  it('湿法与磨皮同时NG不阻止生产，待加工列表也不再标记锁定', async () => {
    getLock.mockImplementation(async (candidate) => candidate.sourceProcessCode === 'ADHESIVE1' ? null : grindingLock);
    expect(await findActiveProcessingSampleAbnormalLock(candidates)).toBeNull();
    expect(await ensureProcessingSampleAbnormalUnlocked(candidates, '加载裁切')).toBe(true);
  });
  it('粘胶1仍阻断，不能被前面的湿法或磨皮异常遮蔽', async () => {
    getLock.mockResolvedValue({ sourceProcessCode: 'ADHESIVE1', objectType: 'SEGMENT', lockReason: '粘胶1留样NG' });
    expect(await ensureProcessingSampleAbnormalUnlocked(candidates, '报工')).toBe(false);
    expect(warning).toHaveBeenCalledWith(expect.objectContaining({ content: '粘胶1留样NG' }));
  });
  it('包装等未选择生产放行策略的调用仍保留原拦截', async () => {
    getLock.mockResolvedValue(grindingLock);
    expect(await ensureSampleAbnormalUnlocked(candidates, '包装')).toBe(false);
    expect(warning).toHaveBeenCalled();
  });
  it('未明确放行的工序继续保留检查', () => {
    expect(processingSampleLockCandidates([{ sourceProcessCode: 'WET' }, { sourceProcessCode: 'ROUGH_GRINDING' }, { sourceProcessCode: 'ADHESIVE1' }, { sourceProcessCode: 'OTHER' }]))
      .toEqual([{ sourceProcessCode: 'ADHESIVE1' }, { sourceProcessCode: 'OTHER' }]);
  });
});
