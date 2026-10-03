import { processingSampleLockCandidates } from './sampleAbnormalLockPolicy';

import { Modal as AModal } from 'ant-design-vue';

import {
  getActiveSampleAbnormalLock,
  type MesQmsSampleAbnormalRecheckApi,
} from '#/api/mes/quality/sample-abnormal-recheck';

export type SampleAbnormalLockCandidate = MesQmsSampleAbnormalRecheckApi.ActiveReqVO & {
  objectLabel: string;
  processName: string;
};

function normalizeBatchNo(value?: string | null) {
  return String(value || '').trim();
}

function pushCandidate(
  candidates: SampleAbnormalLockCandidate[],
  seen: Set<string>,
  candidate: SampleAbnormalLockCandidate,
) {
  if (!candidate.objectNo || !candidate.objectType || !candidate.sourceProcessCode) return;
  const key = `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}`;
  if (seen.has(key)) return;
  seen.add(key);
  candidates.push(candidate);
}

function removeTrailingSegmentMark(batchNo?: string) {
  const normalized = normalizeBatchNo(batchNo);
  return /[PQRS]$/i.test(normalized) ? normalized.slice(0, -1) : '';
}

function buildWetMotherObjectNos(motherBatchNo?: string, segmentBatchNo?: string) {
  const objectNos = [
    normalizeBatchNo(motherBatchNo),
    normalizeBatchNo(segmentBatchNo),
    removeTrailingSegmentMark(motherBatchNo),
    removeTrailingSegmentMark(segmentBatchNo),
  ];
  return objectNos.filter(Boolean);
}

export function buildSegmentChainSampleLockCandidates(params: {
  motherBatchNo?: string;
  segmentBatchNo?: string;
}) {
  const candidates: SampleAbnormalLockCandidate[] = [];
  const seen = new Set<string>();
  const segmentBatchNo = normalizeBatchNo(params.segmentBatchNo || params.motherBatchNo);

  buildWetMotherObjectNos(params.motherBatchNo, segmentBatchNo).forEach((objectNo) => {
    pushCandidate(candidates, seen, {
      qualificationObjectNo: segmentBatchNo || params.motherBatchNo,
      objectLabel: '母卷',
      objectNo,
      objectType: 'MOTHER_ROLL',
      processName: '湿法',
      sourceProcessCode: 'WET',
    });
  });
  pushCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: segmentBatchNo,
    objectType: 'SEGMENT',
    processName: '磨皮',
    sourceProcessCode: 'ROUGH_GRINDING',
  });
  pushCandidate(candidates, seen, {
    objectLabel: '分段',
    objectNo: segmentBatchNo,
    objectType: 'SEGMENT',
    processName: '粘胶1',
    sourceProcessCode: 'ADHESIVE1',
  });

  return candidates;
}

export async function findActiveSampleAbnormalLock(candidates: SampleAbnormalLockCandidate[]) {
  const seen = new Set<string>();
  for (const candidate of candidates) {
    const key = `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}`;
    if (seen.has(key)) continue;
    seen.add(key);
    const lock = await getActiveSampleAbnormalLock(candidate);
    if (lock) return { candidate, lock };
  }
  return null;
}

export async function ensureSampleAbnormalUnlocked(
  candidates: SampleAbnormalLockCandidate[],
  actionName = '操作',
) {
  const active = await findActiveSampleAbnormalLock(candidates);
  if (!active) return true;
  const { candidate, lock } = active;
  const objectLabel =
    String(lock.objectType || '').toUpperCase() === 'MOTHER_ROLL' ? '母卷' : candidate.objectLabel;
  const processName = lock.sourceProcessName || candidate.processName;
  AModal.warning({
    content:
      lock.lockReason ||
      `当前${objectLabel} ${lock.objectNo || candidate.objectNo} 因 ${lock.abnormalFeedbackTime || '-'}，${processName} 留样送检NG异常，锁定不允许继续${actionName}，等待复检确认后继续。`,
    title: '留样异常锁定',
  });
  return false;
}

/** 生产开工/加载/报工专用。裁切报检及完工由后端实时门禁负责，包装保持严格查询。 */
export function findActiveProcessingSampleAbnormalLock(candidates: SampleAbnormalLockCandidate[]) {
  return findActiveSampleAbnormalLock(processingSampleLockCandidates(candidates));
}

export function ensureProcessingSampleAbnormalUnlocked(
  candidates: SampleAbnormalLockCandidate[],
  actionName = '操作',
) {
  return ensureSampleAbnormalUnlocked(processingSampleLockCandidates(candidates), actionName);
}
