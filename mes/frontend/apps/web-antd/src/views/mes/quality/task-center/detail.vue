<script lang="ts" setup>
import type { MesQualityTaskCenterApi } from '#/api/mes/quality/task-center';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Alert, Button, Input, message, Modal, Spin, Tag } from 'ant-design-vue';

import {
  getQualityTaskDetail,
  returnQualityTaskForRecheck,
  submitQualityTaskProcessNode,
} from '#/api/mes/quality/task-center';
import FaiWorkbench from '#/views/mes/quality/fai/modules/workbench.vue';
import FqcWorkbench from '#/views/mes/quality/fqc/modules/workbench.vue';
import IqcWorkbench from '#/views/mes/quality/iqc/modules/workbench.vue';
import OqcWorkbench from '#/views/mes/quality/oqc/modules/workbench.vue';

import NativeIpqcTaskWorkbench from './modules/native-ipqc-task-workbench.vue';
import TaskDetailPanel from './modules/task-detail-panel.vue';

defineOptions({ name: 'MesQualityTaskCenterDetail' });

const props = withDefaults(
  defineProps<{
    activityId?: string;
    id?: number | string;
    processInstanceId?: string;
    readonly?: boolean;
    taskId?: string;
  }>(),
  { readonly: false },
);
const emit = defineEmits<{ close: [] }>();

const loading = ref(false);
const submitting = ref(false);
const returning = ref(false);
const recheckOpen = ref(false);
const recheckReason = ref('');
const historyOpen = ref(false);
const detail = ref<MesQualityTaskCenterApi.TaskDetail>();
const legacyRoundNumbers = computed(() =>
  (detail.value?.rounds || []).some((round) => round.roundNo === 0),
);

const isNativeExecution = computed(
  () => detail.value?.task.executionMode !== 'TASK_OWNED',
);
const nativeCheckType = computed(() => detail.value?.task.checkType?.toUpperCase());
const nativeTypeName = computed(() => {
  const labels: Record<string, string> = {
    FAI: '首件检验',
    FQC: '成品检验',
    IPQC: '过程检验',
    IQC: '进料检验',
    OQC: '出货检验',
  };
  return labels[nativeCheckType.value || ''] || nativeCheckType.value || '检验记录';
});
const editableItemIds = computed(() =>
  (detail.value?.items || [])
    .map((item) => item.executionItemId)
    .filter((id): id is number => id !== undefined && id !== null),
);
const showProcessAction = computed(() => {
  if (props.readonly || detail.value?.actionable !== true) return false;
  if (detail.value.activeTaskKey === 'task_execute') return true;
  return ['task_close', 'task_confirm'].includes(detail.value.activeTaskKey || '');
});
const canReturnForRecheck = computed(
  () =>
    !props.readonly &&
    detail.value?.actionable === true &&
    detail.value.activeTaskKey === 'task_confirm',
);
const actionLabel = computed(() => {
  const labels: Record<string, string> = {
    task_close: '确认并关闭',
    task_confirm: '确认并关闭',
    task_execute: '提交任务',
  };
  return labels[detail.value?.activeTaskKey || ''] || '提交办理';
});
const actionIcon = computed(() => {
  const icons: Record<string, string> = {
    task_close: 'lucide:circle-check-big',
    task_confirm: 'lucide:badge-check',
    task_execute: 'lucide:send',
  };
  return icons[detail.value?.activeTaskKey || ''] || 'lucide:send';
});

watch(
  () => props.id,
  (id) => {
    if (id) void loadDetail();
    else detail.value = undefined;
  },
  { immediate: true },
);

async function loadDetail() {
  const id = Number(props.id);
  if (!Number.isFinite(id) || id <= 0) return;
  loading.value = true;
  try {
    detail.value = await getQualityTaskDetail(id);
  } finally {
    loading.value = false;
  }
}

function statusLabel(value?: string) {
  const labels: Record<string, string> = {
    CANCELLED: '已取消',
    COMPLETED: '已完成',
    DRAFT: '草稿',
    IN_PROGRESS: '执行中',
    PENDING: '待执行',
    SOURCE_MISSING: '检验单异常',
    WAITING_AUDIT: '待确认',
  };
  return labels[value || ''] || value || '-';
}

async function submitCurrentNode(reason?: string) {
  if (!detail.value?.task.id) return;
  submitting.value = true;
  try {
    await submitQualityTaskProcessNode({
      id: detail.value.task.id,
      reason,
    });
    message.success(`${actionLabel.value}成功`);
    await loadDetail();
  } finally {
    submitting.value = false;
  }
}

async function handleReturnForRecheck() {
  const reason = recheckReason.value.trim();
  if (!reason) {
    message.warning('请填写退回重检原因');
    return;
  }
  if (!detail.value?.task.id) return;
  returning.value = true;
  try {
    await returnQualityTaskForRecheck({ id: detail.value.task.id, reason });
    message.success('已生成下一轮检验单并退回执行人');
    recheckOpen.value = false;
    recheckReason.value = '';
    await loadDetail();
  } finally {
    returning.value = false;
  }
}

function roundLabel(roundNo: number) {
  const displayRound = legacyRoundNumbers.value ? roundNo + 1 : Math.max(1, roundNo);
  return displayRound === 1
    ? '第1轮 原始检验单'
    : `第${displayRound}轮 第${displayRound - 1}次复检`;
}

function roundStatusLabel(value?: string) {
  const labels: Record<string, string> = {
    CONFIRMED: '已确认',
    EXECUTING: '执行中',
    RETURNED: '已退回',
    SUBMITTED: '待确认',
  };
  return labels[value || ''] || value || '-';
}

async function handleNativeSubmitted() {
  try {
    await submitCurrentNode(`${nativeTypeName.value}记录填写完成并提交`);
  } catch {
    message.warning('检验单已提交，但流程节点尚未推进，请点击右上角“提交任务”重试');
    await loadDetail();
  }
}

function handleProcessAction() {
  Modal.confirm({
    cancelText: '取消',
    content: `确认${actionLabel.value}吗？本次操作将推进质量任务流程。`,
    okText: '确认',
    onOk: () => submitCurrentNode(),
    title: actionLabel.value,
  });
}

defineExpose({ reload: loadDetail });
</script>

<template>
  <Spin class="quality-task-detail-spin" :spinning="loading">
    <div v-if="detail && isNativeExecution" class="native-task-detail">
      <header class="task-header">
        <div class="task-heading">
          <div class="task-title">质量检验任务 · {{ nativeTypeName }}</div>
          <div class="task-subtitle">
            任务号：{{ detail.task.taskNo }}　检验单：{{ detail.task.executionNo || '-' }}　节点：{{ detail.activeTaskName || statusLabel(detail.task.effectiveStatus) }}
          </div>
        </div>
        <div class="task-actions">
          <Tag color="blue">{{ statusLabel(detail.task.effectiveStatus) }}</Tag>
          <Button @click="loadDetail">
            <IconifyIcon icon="lucide:refresh-cw" />
            刷新状态
          </Button>
          <Button v-if="detail.rounds?.length" @click="historyOpen = true">
            <IconifyIcon icon="lucide:history" />
            重检历史({{ detail.task.recheckCount || 0 }})
          </Button>
          <Button v-if="canReturnForRecheck" danger @click="recheckOpen = true">
            <IconifyIcon icon="lucide:rotate-ccw" />
            退回重检
          </Button>
          <Button
            v-if="showProcessAction"
            type="primary"
            :loading="submitting"
            @click="handleProcessAction"
          >
            <IconifyIcon :icon="actionIcon" />
            {{ actionLabel }}
          </Button>
          <Button @click="emit('close')">
            <IconifyIcon icon="lucide:x" />
            关闭窗口
          </Button>
        </div>
      </header>

      <Alert
        v-if="detail.task.taskInstruction"
        class="task-instruction"
        :message="`任务要求：${detail.task.taskInstruction}`"
        show-icon
        type="info"
      />

      <div class="native-workbench">
        <FaiWorkbench
          v-if="nativeCheckType === 'FAI'"
          :key="detail.task.executionId"
          :editable-item-ids="editableItemIds"
          :enable-import="false"
          :enable-scan="false"
          embedded
          :initial-record-id="detail.task.executionId"
          page-description="按原首件检验标准的位置、组次和数据规则填写；未纳入本次任务的检验项只读"
          page-title="首件检验记录填写"
          :readonly="props.readonly || detail.editable !== true"
          @submitted="handleNativeSubmitted"
        />
        <IqcWorkbench
          v-else-if="nativeCheckType === 'IQC'"
          :key="detail.task.executionId"
          :initial-record-id="detail.task.executionId"
        />
        <FqcWorkbench
          v-else-if="nativeCheckType === 'FQC'"
          :key="detail.task.executionId"
          :enable-import="false"
          :enable-scan="false"
          :initial-record-id="detail.task.executionId"
          page-description="按原成品检验单的项目、样本和标准规则填写"
          page-title="成品检验记录填写"
        />
        <OqcWorkbench
          v-else-if="nativeCheckType === 'OQC'"
          :key="detail.task.executionId"
          :initial-record-id="detail.task.executionId"
        />
        <NativeIpqcTaskWorkbench
          v-else-if="nativeCheckType === 'IPQC'"
          :key="detail.task.executionId"
          :editable="!props.readonly && detail.editable === true"
          :record-id="detail.task.executionId"
          @submitted="handleNativeSubmitted"
        />
        <Alert
          v-else
          message="当前检验类型暂未配置原生填写工作台"
          show-icon
          type="warning"
        />
      </div>
    </div>

    <TaskDetailPanel
      v-else-if="detail"
      :id="id"
      :readonly="props.readonly"
      @close="emit('close')"
    />
  </Spin>

  <Modal
    v-model:open="recheckOpen"
    cancel-text="取消"
    ok-text="确认退回"
    :confirm-loading="returning"
    title="退回重检"
    @ok="handleReturnForRecheck"
  >
    <Alert
      class="modal-alert"
      message="系统将保留本轮检验单，复制生成下一轮未检记录，并把流程退回执行检验。"
      show-icon
      type="warning"
    />
    <Input.TextArea
      v-model:value="recheckReason"
      :maxlength="500"
      :rows="5"
      show-count
      placeholder="请填写本次退回重检原因（必填）"
    />
  </Modal>

  <Modal v-model:open="historyOpen" :footer="null" title="检验轮次历史" width="920px">
    <div class="round-history-table">
      <table>
        <thead>
          <tr>
            <th>轮次</th>
            <th>检验单号</th>
            <th>状态</th>
            <th>判定</th>
            <th>检验人/时间</th>
            <th>退回信息</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="round in detail?.rounds || []" :key="`${round.roundNo}-${round.executionId}`">
            <td>{{ roundLabel(round.roundNo) }}</td>
            <td>{{ round.executionNo || '-' }}</td>
            <td>{{ roundStatusLabel(round.roundStatus) }}</td>
            <td>{{ round.judgment || '-' }}</td>
            <td>{{ round.inspectorName || '-' }}<br />{{ round.inspectionTime || '-' }}</td>
            <td>{{ round.returnReason || '-' }}<br />{{ round.returnedByName || '' }} {{ round.returnedTime || '' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </Modal>
</template>

<style scoped>
.quality-task-detail-spin,
.quality-task-detail-spin :deep(.ant-spin-nested-loading),
.quality-task-detail-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.quality-task-detail-spin :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.native-task-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  background: #f1f5f9;
}

.task-header {
  display: flex;
  min-height: 68px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #cbd5e1;
  padding: 10px 16px;
  background: #eef6ff;
}

.task-heading {
  min-width: 0;
}

.task-title {
  color: #075985;
  font-size: 18px;
  font-weight: 700;
}

.task-subtitle {
  margin-top: 4px;
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.task-instruction {
  margin: 10px 12px 0;
}

.native-workbench {
  position: relative;
  display: flex;
  min-height: 0;
  flex: 1;
  overflow: hidden;
  padding: 10px 12px 12px;
}

.modal-alert {
  margin-bottom: 14px;
}

.round-history-table {
  max-height: 460px;
  overflow: auto;
  border: 1px solid #d9e2ec;
}

.round-history-table table {
  width: 100%;
  border-collapse: collapse;
}

.round-history-table th,
.round-history-table td {
  padding: 9px 10px;
  border-right: 1px solid #d9e2ec;
  border-bottom: 1px solid #d9e2ec;
  text-align: left;
  vertical-align: top;
}

.round-history-table th {
  position: sticky;
  top: 0;
  background: #eef3f8;
}

@media (max-width: 900px) {
  .task-header {
    align-items: flex-start;
  }

  .task-subtitle {
    white-space: normal;
  }

}
</style>
