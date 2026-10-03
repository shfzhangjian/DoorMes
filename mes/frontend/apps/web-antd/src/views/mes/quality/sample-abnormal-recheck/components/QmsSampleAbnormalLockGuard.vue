<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue';
import { isSampleLockDeferredToCutRound } from '#/views/mes/hc/execution/report/shared/sampleAbnormalLockPolicy';

import { IconifyIcon } from '@vben/icons';

import { Button, Modal, Spin, Tag, message } from 'ant-design-vue';

import {
  createSampleAbnormalRecheck,
  getActiveSampleAbnormalLock,
  type MesQmsSampleAbnormalRecheckApi,
} from '#/api/mes/quality/sample-abnormal-recheck';
import { printFaiInspectionTransferTicketById } from '#/views/mes/quality/fai/shared/faiPrint';

const props = withDefaults(
  defineProps<{
    actionSourceProcessCode?: string;
    deferToCutRound?: boolean;
    candidates?: MesQmsSampleAbnormalRecheckApi.ActiveReqVO[];
    objectLabel?: string;
    objectNo?: string;
    objectType?: string;
    processName?: string;
    sourceProcessCode?: string;
    variant?: 'cell' | 'panel';
  }>(),
  {
    actionSourceProcessCode: '',
    deferToCutRound: false,
    candidates: () => [],
    objectLabel: '分段',
    objectNo: '',
    objectType: 'SEGMENT',
    processName: '',
    sourceProcessCode: '',
    variant: 'panel',
  },
);

const emit = defineEmits<{
  change: [lock: MesQmsSampleAbnormalRecheckApi.Record | null];
  recheck: [lock: MesQmsSampleAbnormalRecheckApi.Record];
  refresh: [lock: MesQmsSampleAbnormalRecheckApi.Record | null];
}>();

const activeLock = ref<MesQmsSampleAbnormalRecheckApi.Record | null>(null);
const isDeferredLock = computed(() => !!activeLock.value && props.deferToCutRound
  && isSampleLockDeferredToCutRound(activeLock.value.sourceProcessCode));
const isBlockingLock = computed(() => !!activeLock.value && !isDeferredLock.value);
const loading = ref(false);
const recheckLoading = ref(false);

const lockCandidates = computed(() => {
  const candidates: MesQmsSampleAbnormalRecheckApi.ActiveReqVO[] = props.candidates?.length
    ? props.candidates
    : [
        {
          objectNo: props.objectNo,
          objectType: props.objectType,
          sourceProcessCode: props.sourceProcessCode,
        },
      ];
  const seen = new Set<string>();
  return candidates
    .map((candidate) => ({
      qualificationObjectNo: String(candidate?.qualificationObjectNo || '').trim(),
      objectNo: String(candidate?.objectNo || '').trim(),
      objectType: String(candidate?.objectType || '').trim(),
      sourceProcessCode: String(candidate?.sourceProcessCode || '').trim(),
    }))
    .filter((candidate) => candidate.objectNo && candidate.objectType && candidate.sourceProcessCode)
    .filter((candidate) => {
      const key = `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}|${candidate.qualificationObjectNo || ""}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
});
const lockCandidateKey = computed(() =>
  lockCandidates.value
    .map((candidate) => `${candidate.sourceProcessCode}|${candidate.objectType}|${candidate.objectNo}|${candidate.qualificationObjectNo || ""}`)
    .join(';'),
);
const canQuery = computed(() => lockCandidates.value.length > 0);
const resolvedObjectLabel = computed(() => {
  const objectType = String(activeLock.value?.objectType || '').toUpperCase();
  if (objectType === 'MOTHER_ROLL') return '母卷';
  if (objectType === 'SEGMENT') return '分段';
  return props.objectLabel || '对象';
});
const resolvedProcessName = computed(
  () => activeLock.value?.sourceProcessName || props.processName || '当前工序',
);
const resolvedActionProcessCode = computed(() =>
  String(props.actionSourceProcessCode || props.sourceProcessCode || '').trim(),
);
const isOwnProcessLock = computed(() => {
  const actionProcessCode = resolvedActionProcessCode.value;
  if (!activeLock.value || !actionProcessCode) return false;
  return String(activeLock.value.sourceProcessCode || '').trim() === actionProcessCode;
});
const lockMessage = computed(() => {
  if (!activeLock.value) return '';
  if (isDeferredLock.value) {
    return `${resolvedProcessName.value}留样NG，${resolvedObjectLabel.value} ${activeLock.value.objectNo || props.objectNo || '-'}，检验单 ${activeLock.value.abnormalInspectionNo || '-'}；允许继续加工，裁切禁止提交检验、本段完工及工单完工，等待对应复检合格解锁。`;
  }
  if (activeLock.value.lockReason) return activeLock.value.lockReason;
  const time = activeLock.value.abnormalFeedbackTime || activeLock.value.updateTime || '-';
  const objectNo = activeLock.value.objectNo || props.objectNo || '-';
  return `当前${resolvedObjectLabel.value} ${objectNo} 因 ${time}，${resolvedProcessName.value} 留样送检NG异常，锁定不允许继续报工，等待复检确认后继续。`;
});
const waitingText = computed(() => {
  const lock = activeLock.value;
  if (!isOwnProcessLock.value) return '等待前置异常工序复检OK后解锁';
  if (!lock?.recheckInspectionId) return '未发起复检';
  if (lock.recheckResult === 'NG') return '复检NG，需再次复检';
  if (lock.recheckResult === 'OK') return '复检OK，刷新后解锁';
  return '复检单等待反馈';
});
const canCreateRecheck = computed(() => {
  const lock = activeLock.value;
  if (!lock?.id || !isOwnProcessLock.value) return false;
  if (!lock.recheckInspectionId) return true;
  return lock.recheckResult === 'NG';
});
const canShowRecheckActions = computed(() => !!activeLock.value && isOwnProcessLock.value);
const lockTitle = computed(() => isDeferredLock.value ? '留样NG提醒（裁切报检/完工受限）' : (isOwnProcessLock.value ? '留样NG异常锁定' : '前置留样NG锁定'));
const lockIcon = computed(() => (isOwnProcessLock.value ? 'lucide:shield-alert' : 'lucide:octagon-alert'));

async function refresh() {
  if (!canQuery.value) {
    activeLock.value = null;
    emit('change', null);
    emit('refresh', null);
    return null;
  }
  loading.value = true;
  try {
    let matchedLock: MesQmsSampleAbnormalRecheckApi.Record | null = null;
    for (const candidate of lockCandidates.value) {
      const lock = await getActiveSampleAbnormalLock(candidate);
      if (lock) {
        if (props.deferToCutRound && isSampleLockDeferredToCutRound(lock.sourceProcessCode)) {
          // 提醒不能遮蔽后续候选中的粘胶1等真正阻断锁。
          matchedLock ??= lock;
          continue;
        }
        matchedLock = lock;
        break;
      }
    }
    activeLock.value = matchedLock;
    emit('change', activeLock.value);
    emit('refresh', activeLock.value);
    return activeLock.value;
  } finally {
    loading.value = false;
  }
}

async function ensureUnlocked() {
  const lock = await refresh();
  if (!lock || isDeferredLock.value) return true;
  Modal.warning({
    content: lockMessage.value,
    title: '留样异常锁定',
  });
  return false;
}

async function handleCreateRecheck() {
  const lock = activeLock.value;
  if (!lock?.id || !canCreateRecheck.value) return;
  recheckLoading.value = true;
  try {
    const result = await createSampleAbnormalRecheck(lock.id);
    message.success('复检送检单已创建');
    activeLock.value = result;
    emit('recheck', result);
    if (result.recheckInspectionId) {
      Modal.confirm({
        content: '是否立即打印复检送检流转单？',
        onOk: () => printFaiInspectionTransferTicketById(result.recheckInspectionId!),
        title: '复检单已创建',
      });
    }
    await refresh();
  } finally {
    recheckLoading.value = false;
  }
}

watch(
  () => lockCandidateKey.value,
  () => {
    void refresh();
  },
);

onMounted(() => {
  void refresh();
});

defineExpose({
  activeLock,
  ensureUnlocked,
  refresh,
});
</script>

<template>
  <div
    class="qms-sample-abnormal-lock-guard"
    :class="{
      'qms-sample-abnormal-lock-guard--cell': variant === 'cell',
      'qms-sample-abnormal-lock-guard--locked': isBlockingLock,
      'qms-sample-abnormal-lock-guard--deferred': isDeferredLock,
      'qms-sample-abnormal-lock-guard--upstream': !!activeLock && !isOwnProcessLock,
    }"
  >
    <slot
      :ensure-unlocked="ensureUnlocked"
      :lock="activeLock"
      :locked="isBlockingLock"
      :refresh="refresh"
    />
    <div v-if="activeLock" class="qms-sample-abnormal-lock-guard__overlay" @click.stop>
      <div class="qms-sample-abnormal-lock-guard__panel">
        <div class="qms-sample-abnormal-lock-guard__head">
          <IconifyIcon class="qms-sample-abnormal-lock-guard__icon" :icon="lockIcon" />
          <div class="qms-sample-abnormal-lock-guard__title">{{ lockTitle }}</div>
          <Tag color="error">{{ resolvedProcessName }}</Tag>
        </div>
        <div class="qms-sample-abnormal-lock-guard__message">
          {{ lockMessage }}
        </div>
        <div class="qms-sample-abnormal-lock-guard__meta">
          <span>{{ resolvedObjectLabel }}：{{ activeLock.objectNo || objectNo || '-' }}</span>
          <span>{{ waitingText }}</span>
        </div>
        <div class="qms-sample-abnormal-lock-guard__actions">
          <Button :loading="loading" @click.stop="refresh">
            <IconifyIcon class="mr-1" icon="lucide:refresh-cw" />
            刷新
          </Button>
          <Button
            v-if="canShowRecheckActions && activeLock.recheckInspectionId"
            @click.stop="printFaiInspectionTransferTicketById(activeLock.recheckInspectionId!)"
          >
            <IconifyIcon class="mr-1" icon="lucide:printer" />
            打印复检单
          </Button>
          <Button
            v-if="canShowRecheckActions"
            danger
            :disabled="!canCreateRecheck"
            :loading="recheckLoading"
            type="primary"
            @click.stop="handleCreateRecheck"
          >
            <IconifyIcon class="mr-1" icon="lucide:send" />
            异常复检
          </Button>
        </div>
      </div>
      <Spin v-if="loading" class="qms-sample-abnormal-lock-guard__spin" />
    </div>
  </div>
</template>

<style scoped>
.qms-sample-abnormal-lock-guard {
  position: relative;
  min-width: 0;
  min-height: 0;
}

.qms-sample-abnormal-lock-guard--locked {
  outline: 2px solid rgba(220, 38, 38, 0.42);
  outline-offset: -2px;
}

.qms-sample-abnormal-lock-guard__overlay {
  position: absolute;
  z-index: 20;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 10px;
  background: rgba(15, 23, 42, 0.62);
  backdrop-filter: blur(1px);
}

.qms-sample-abnormal-lock-guard__panel {
  width: min(520px, 100%);
  max-width: 100%;
  max-height: 100%;
  overflow: auto;
  padding: 14px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(220, 38, 38, 0.32);
  border-radius: 8px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.22);
  scrollbar-width: thin;
}

.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__overlay {
  align-items: flex-start;
  padding: 44px 24px 24px;
  background: rgba(241, 245, 249, 0.28);
  backdrop-filter: none;
}

.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: min(560px, 84%);
  padding: 12px 24px;
  color: #991b1b;
  text-align: center;
  background: rgba(255, 241, 242, 0.82);
  border: 1px solid #fecdd3;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(148, 27, 27, 0.12);
}

.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__head {
  justify-content: center;
}

.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__message {
  max-width: 100%;
  color: #991b1b;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
}

.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__meta {
  justify-content: center;
}

.qms-sample-abnormal-lock-guard__head,
.qms-sample-abnormal-lock-guard__actions,
.qms-sample-abnormal-lock-guard__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.qms-sample-abnormal-lock-guard__head {
  margin-bottom: 8px;
}

.qms-sample-abnormal-lock-guard__icon {
  flex: 0 0 auto;
  color: #dc2626;
  font-size: 20px;
}

.qms-sample-abnormal-lock-guard__title {
  flex: 1 1 auto;
  min-width: 0;
  color: #991b1b;
  font-size: 15px;
  font-weight: 800;
  overflow-wrap: anywhere;
  white-space: normal;
}

.qms-sample-abnormal-lock-guard__message {
  color: #7f1d1d;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.55;
  overflow-wrap: anywhere;
  white-space: normal;
}

.qms-sample-abnormal-lock-guard__meta {
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
  line-height: 1.4;
}

.qms-sample-abnormal-lock-guard__actions {
  justify-content: flex-end;
  margin-top: 12px;
}

.qms-sample-abnormal-lock-guard__actions :deep(.ant-btn[disabled]),
.qms-sample-abnormal-lock-guard__actions :deep(.ant-btn.ant-btn-disabled) {
  color: #64748b !important;
  text-shadow: none !important;
  cursor: not-allowed;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  box-shadow: none !important;
  opacity: 1;
}

.qms-sample-abnormal-lock-guard__actions :deep(.ant-btn[disabled] *),
.qms-sample-abnormal-lock-guard__actions :deep(.ant-btn.ant-btn-disabled *) {
  color: #64748b !important;
}

.qms-sample-abnormal-lock-guard__spin {
  position: absolute;
  right: 10px;
  bottom: 10px;
}

.qms-sample-abnormal-lock-guard--cell {
  min-width: 0;
  min-height: 0;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__overlay {
  align-items: stretch;
  padding: 4px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__panel {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__head {
  gap: 4px;
  margin-bottom: 2px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__icon {
  font-size: 14px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__title {
  font-size: 12px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__message {
  overflow: visible;
  font-size: 12px;
  line-height: 1.35;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__meta {
  display: none;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__actions {
  gap: 4px;
  justify-content: center;
  margin-top: 6px;
}

.qms-sample-abnormal-lock-guard--cell .qms-sample-abnormal-lock-guard__actions :deep(.ant-btn) {
  height: 22px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 20px;
}

.qms-sample-abnormal-lock-guard--cell.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__overlay {
  align-items: center;
  padding: 6px;
}

.qms-sample-abnormal-lock-guard--cell.qms-sample-abnormal-lock-guard--upstream .qms-sample-abnormal-lock-guard__panel {
  align-items: center;
  justify-content: flex-start;
  width: 100%;
  max-width: 100%;
  height: 100%;
}

/* 后移的留样锁使用正常文档流提醒，不覆盖或吞掉生产区域的操作。 */
.qms-sample-abnormal-lock-guard.qms-sample-abnormal-lock-guard--deferred .qms-sample-abnormal-lock-guard__overlay {
  position: relative;
  inset: auto;
  padding: 4px;
  background: transparent;
  backdrop-filter: none;
}
.qms-sample-abnormal-lock-guard.qms-sample-abnormal-lock-guard--deferred .qms-sample-abnormal-lock-guard__panel {
  width: 100%;
  max-width: 100%;
  height: auto;
  padding: 8px;
  color: #92400e;
  background: #fffbeb;
  border-color: #fcd34d;
  box-shadow: none;
}
</style>
