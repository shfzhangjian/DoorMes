<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesQualityTaskCenterApi } from '#/api/mes/quality/task-center';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  Select,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  getQualityTaskDetail,
  returnQualityTaskForRecheck,
  saveQualityTaskResult,
  submitQualityTaskProcessNode,
} from '#/api/mes/quality/task-center';

defineOptions({ name: 'QmsQualityTaskDetailPanel' });

const props = withDefaults(
  defineProps<{
    id?: number | string;
    readonly?: boolean;
  }>(),
  { readonly: false },
);
const emit = defineEmits<{ close: [] }>();

const loading = ref(false);
const saving = ref(false);
const submitting = ref(false);
const returning = ref(false);
const recheckOpen = ref(false);
const recheckReason = ref('');
const historyOpen = ref(false);
const detail = ref<MesQualityTaskCenterApi.TaskDetail>();
const legacyRoundNumbers = computed(() =>
  (detail.value?.rounds || []).some((round) => round.roundNo === 0),
);

const editable = computed(
  () => !props.readonly && detail.value?.editable === true,
);
const isTaskOwned = computed(
  () => detail.value?.task.executionMode === 'TASK_OWNED',
);
const pieceNoEditable = computed(
  () =>
    editable.value &&
    detail.value?.task.objectMode === 'EXECUTION_PIECE' &&
    detail.value?.task.sampleSelectionMode === 'QUANTITY_ONLY',
);
const nativeSubmitted = computed(() =>
  [
    'ABNORMAL',
    'COMPLETED',
    'FINISHED',
    'REJECTED',
    'WAITING_CONFIRM',
    'WAITING_QA',
  ].includes((detail.value?.task.sourceStatus || '').toUpperCase()),
);
const showProcessAction = computed(() => {
  if (props.readonly || detail.value?.actionable !== true) return false;
  if (detail.value.activeTaskKey === 'task_execute') return nativeSubmitted.value;
  return ['task_close', 'task_confirm'].includes(detail.value.activeTaskKey || '');
});
const canReturnForRecheck = computed(
  () =>
    !props.readonly &&
    detail.value?.actionable === true &&
    detail.value.activeTaskKey === 'task_confirm',
);
const actionLabel = computed(() =>
  detail.value?.activeTaskKey === 'task_execute' ? '提交任务' : '确认并关闭',
);

const columns: TableColumnsType<MesQualityTaskCenterApi.SampleResult> = [
  { dataIndex: 'sampleSeq', title: '样本', width: 74, fixed: 'left' },
  { dataIndex: 'pieceNo', title: '实际片号', width: 170, fixed: 'left' },
  { dataIndex: 'inspectionItem', title: '检验项目', width: 180 },
  { dataIndex: 'standardDesc', title: '标准说明', width: 240 },
  { dataIndex: 'inspectionMethod', title: '检验方法', width: 150 },
  { dataIndex: 'testTool', title: '检测仪器', width: 140 },
  { dataIndex: 'inputValue', title: '检验结果', width: 200 },
  { dataIndex: 'unit', title: '单位', width: 70 },
  { dataIndex: 'result', title: '判定', width: 150, fixed: 'right' },
];

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

function isQualitative(item: MesQualityTaskCenterApi.SampleResult) {
  return item.itemType?.toUpperCase() === 'QUALITATIVE';
}

function handleQualitativeChange(
  item: MesQualityTaskCenterApi.SampleResult,
  value?: string,
) {
  item.qualitativeValue = value;
  if (value === 'OK' || value === 'NG') item.result = value;
}

function displayInputValue(item: MesQualityTaskCenterApi.SampleResult) {
  if (isQualitative(item)) {
    const labels: Record<string, string> = { NG: '不合格', OK: '合格' };
    return item.qualitativeValue
      ? labels[item.qualitativeValue] || item.qualitativeValue
      : '-';
  }
  return item.measuredValue ?? '-';
}

function resultLabel(value?: string) {
  const labels: Record<string, string> = {
    NG: '不合格',
    OK: '合格',
    PENDING: '未检',
  };
  return value ? labels[value] || value : '-';
}

function checkTypeLabel(checkType?: string) {
  const labels: Record<string, string> = {
    FAI: '过程首检',
    FQC: '成品检验',
    GLUE_BOARD_FAI: '胶板检验',
    IPQC: '过程巡检',
    IQC: '进料检验',
    OQC: '出货检验',
  };
  return checkType ? labels[checkType] || checkType : '-';
}

function statusLabel(value?: string) {
  const labels: Record<string, string> = {
    CANCELLED: '已取消',
    COMPLETED: '已完成',
    DRAFT: '草稿',
    IN_PROGRESS: '执行中',
    PENDING: '待执行',
    SOURCE_MISSING: '检验单异常',
    SUSPENDED: '已挂起',
    WAITING_AUDIT: '待确认',
  };
  return value ? labels[value] || value : '-';
}

function materialLabel(task?: MesQualityTaskCenterApi.Task) {
  if (!task) return '-';
  return [task.materialCode, task.materialName].filter(Boolean).join(' / ') || '-';
}

function handlePieceNoChange(
  row: MesQualityTaskCenterApi.SampleResult,
  value?: string,
) {
  const pieceNo = String(value || '').trim();
  for (const item of detail.value?.samples || []) {
    if (item.sampleSeq === row.sampleSeq) item.pieceNo = pieceNo;
  }
}

function sampleLabel(sampleSeq: number) {
  return `样本${sampleSeq}`;
}

async function saveResult() {
  if (!detail.value?.task?.id) return;
  const samples = detail.value.samples || [];
  if (!samples.length) {
    message.warning('当前任务没有可填写的样本检验项');
    return;
  }
  if (detail.value.task.objectMode === 'EXECUTION_PIECE') {
    const samplePieceNos = new Map<number, string>();
    for (const row of samples) {
      const pieceNo = String(row.pieceNo || '').trim();
      if (!pieceNo) {
        message.warning(`请填写${sampleLabel(row.sampleSeq)}的实际片号`);
        return;
      }
      const existing = samplePieceNos.get(row.sampleSeq);
      if (existing && existing !== pieceNo) {
        message.warning(`${sampleLabel(row.sampleSeq)}存在不一致的片号`);
        return;
      }
      samplePieceNos.set(row.sampleSeq, pieceNo);
    }
    const pieceNos = [...samplePieceNos.values()];
    if (new Set(pieceNos).size !== pieceNos.length) {
      message.warning('不同样本不能使用相同片号');
      return;
    }
  }
  const invalid = samples.find((item) => {
    if (!item.result || !['OK', 'NG'].includes(item.result)) return true;
    if (isQualitative(item)) return !item.qualitativeValue;
    return item.measuredValue === undefined || item.measuredValue === null;
  });
  if (invalid) {
    message.warning(
      `请完整填写${sampleLabel(invalid.sampleSeq)}“${invalid.inspectionItem}”的检验结果和判定`,
    );
    return;
  }
  saving.value = true;
  try {
    await saveQualityTaskResult({
      id: detail.value.task.id,
      items: samples.map((item) => ({
        taskItemId: item.taskItemId,
        sampleResultId: item.id,
        sampleSeq: item.sampleSeq,
        pieceNo: item.pieceNo?.trim(),
        measuredValue: item.measuredValue,
        qualitativeValue: item.qualitativeValue,
        result: item.result!,
      })),
    });
    message.success('检验结果已保存');
    await loadDetail();
  } finally {
    saving.value = false;
  }
}

async function submitCurrentNode() {
  if (!detail.value?.task.id) return;
  submitting.value = true;
  try {
    await submitQualityTaskProcessNode({ id: detail.value.task.id });
    message.success(`${actionLabel.value}成功`);
    await loadDetail();
  } finally {
    submitting.value = false;
  }
}

function handleProcessAction() {
  Modal.confirm({
    cancelText: '取消',
    content:
      detail.value?.activeTaskKey === 'task_execute'
        ? '确认提交本轮检验结果并进入结果确认吗？'
        : '确认本轮检验结果并直接关闭质量任务吗？',
    okText: '确认',
    onOk: submitCurrentNode,
    title: actionLabel.value,
  });
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
    message.success(
      isTaskOwned.value
        ? '已生成下一轮任务检验矩阵并退回执行人'
        : '已生成下一轮检验单并退回执行人',
    );
    recheckOpen.value = false;
    recheckReason.value = '';
    await loadDetail();
  } finally {
    returning.value = false;
  }
}

function roundLabel(roundNo: number) {
  const displayRound = legacyRoundNumbers.value ? roundNo + 1 : Math.max(1, roundNo);
  if (displayRound === 1) {
    return isTaskOwned.value ? '第1轮 原始加检任务' : '第1轮 原始检验单';
  }
  return `第${displayRound}轮 第${displayRound - 1}次复检`;
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

defineExpose({ reload: loadDetail });
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="detail" class="task-detail-panel">
      <div class="document-header">
        <div></div>
        <div class="document-heading">
          <h2>质量检验任务</h2>
          <div>
            任务号：{{ detail.task.taskNo }}　状态：{{ statusLabel(detail.task.effectiveStatus) }}
          </div>
        </div>
        <div class="document-actions">
          <Button v-if="detail.rounds?.length" @click="historyOpen = true">
            <IconifyIcon icon="lucide:history" />
            重检历史({{ detail.task.recheckCount || 0 }})
          </Button>
          <Button v-if="canReturnForRecheck" danger @click="recheckOpen = true">
            <IconifyIcon icon="lucide:rotate-ccw" />
            退回重检
          </Button>
          <Button
            v-if="editable"
            type="primary"
            :loading="saving"
            @click="saveResult"
          >
            <IconifyIcon icon="lucide:save" />
            保存
          </Button>
          <Button
            v-if="showProcessAction"
            type="primary"
            :loading="submitting"
            @click="handleProcessAction"
          >
            <IconifyIcon icon="lucide:circle-check-big" />
            {{ actionLabel }}
          </Button>
          <Button @click="emit('close')">
            <IconifyIcon icon="lucide:x" />
            关闭窗口
          </Button>
        </div>
      </div>

      <section class="erp-section">
        <div class="section-title">一、任务信息</div>
        <div class="detail-grid">
          <label>建立方式</label>
          <span>{{ detail.task.taskType === 'RECHECK' ? '复检' : detail.task.taskType === 'ADDITIONAL' ? '加检' : '历史任务' }}</span>
          <label>检验类型</label>
          <span>{{ checkTypeLabel(detail.task.checkType) }}</span>
          <label>{{ isTaskOwned ? '任务执行编号' : '新检验单号' }}</label>
          <span>{{ detail.task.executionNo || '-' }}</span>
          <label>来源检验单</label>
          <span>{{ detail.task.sourceExecutionNo || '-' }}</span>
          <label>检验对象</label>
          <span>{{ detail.task.objectMode === 'EXECUTION_PIECE' ? '执行时按片记录' : '批次检验' }}</span>
          <label>样本规则</label>
          <span>
            {{ detail.task.sampleSelectionMode === 'SPECIFIED_PIECE' ? '指定片号' : `指定数量（${detail.task.requiredSampleQty || 0}）` }}
          </span>
          <label>批次号</label>
          <span>{{ detail.task.lotNo || '-' }}</span>
          <template v-if="detail.task.checkType === 'IQC'">
            <label>关联收料单</label>
            <span>{{ detail.task.receiptNo || '-' }}</span>
            <label>供应商</label>
            <span>{{ detail.task.supplierName || '-' }}</span>
            <label>来料日期</label>
            <span>{{ detail.task.arrivalDate || '-' }}</span>
          </template>
          <label>产品型号</label>
          <span>{{ detail.task.productModel || '-' }}</span>
          <label>物料</label>
          <span>{{ materialLabel(detail.task) }}</span>
          <label>规格</label>
          <span>{{ detail.task.materialSpec || '-' }}</span>
          <label>工序</label>
          <span>{{ detail.task.operationName || '-' }}</span>
          <label>检验量</label>
          <span>{{ detail.task.checkQty ?? '-' }} {{ detail.task.unit || '' }}</span>
          <label>执行人</label>
          <span>{{ detail.task.assigneeUserName || '-' }}</span>
          <label>完成时间</label>
          <span>{{ detail.task.requiredFinishTime || '-' }}</span>
          <label>任务要求</label>
          <span class="wide pre-wrap">{{ detail.task.taskInstruction || '-' }}</span>
        </div>
      </section>

      <section class="erp-section">
        <div class="section-title">二、检验记录</div>
        <Table
          :columns="columns"
          :data-source="detail.samples"
          :pagination="false"
          :row-key="(row) => row.id"
          :scroll="{ x: 1500, y: 460 }"
          bordered
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'sampleSeq'">
              {{ sampleLabel(record.sampleSeq) }}
            </template>
            <template v-else-if="column.dataIndex === 'pieceNo'">
              <Input
                v-if="pieceNoEditable"
                :value="record.pieceNo"
                placeholder="录入实际片号"
                @change="(event) => handlePieceNoChange(record, event.target.value)"
              />
              <span v-else>
                {{ detail.task.objectMode === 'BATCH' ? sampleLabel(record.sampleSeq) : record.pieceNo || '-' }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'inputValue'">
              <template v-if="editable">
                <Select
                  v-if="isQualitative(record)"
                  :value="record.qualitativeValue"
                  :options="[
                    { label: '合格', value: 'OK' },
                    { label: '不合格', value: 'NG' },
                  ]"
                  placeholder="请选择"
                  class="full-control"
                  @change="(value) => handleQualitativeChange(record, value)"
                />
                <InputNumber
                  v-else
                  v-model:value="record.measuredValue"
                  class="full-control"
                  placeholder="请输入实测值"
                />
              </template>
              <span v-else>{{ displayInputValue(record) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'unit'">
              {{ record.unit || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'result'">
              <Radio.Group v-if="editable" v-model:value="record.result">
                <Radio value="OK">合格</Radio>
                <Radio value="NG">不合格</Radio>
              </Radio.Group>
              <Tag
                v-else
                :color="record.result === 'OK' ? 'success' : record.result === 'NG' ? 'error' : 'default'"
              >
                {{ resultLabel(record.result) }}
              </Tag>
            </template>
          </template>
        </Table>
      </section>
    </div>

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
        :message="
          isTaskOwned
            ? '系统将保留本轮任务检验矩阵，复制生成下一轮空白矩阵，并把流程退回执行检验。'
            : '系统将保留本轮检验单，复制生成下一轮未检记录，并把流程退回执行检验。'
        "
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
            <tr><th>轮次</th><th>{{ isTaskOwned ? '任务执行编号' : '检验单号' }}</th><th>状态</th><th>判定</th><th>检验人/时间</th><th>退回信息</th></tr>
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
  </Spin>
</template>

<style scoped>
.task-detail-panel { display: flex; height: 100%; min-height: 0; flex-direction: column; overflow: auto; color: #172b4d; background: #f8fafc; }
.document-header { display: grid; min-height: 78px; grid-template-columns: 1fr auto 1fr; align-items: center; border-bottom: 1px solid #cbd5e1; background: #eef6ff; }
.document-heading { text-align: center; }
.document-heading h2 { margin: 0 0 5px; color: #075985; font-size: 22px; font-weight: 700; }
.document-heading div { color: #64748b; }
.document-actions { display: flex; justify-content: flex-end; gap: 8px; padding-right: 14px; }
.erp-section { margin-top: 12px; border: 1px solid #d9e2ec; }
.section-title { padding: 9px 14px; border-bottom: 1px solid #d9e2ec; color: #0969da; font-size: 16px; font-weight: 700; }
.detail-grid { display: grid; grid-template-columns: 118px minmax(160px, 1fr) 118px minmax(160px, 1fr); }
.detail-grid label, .detail-grid span { min-height: 42px; padding: 9px 11px; border-right: 1px solid #d9e2ec; border-bottom: 1px solid #d9e2ec; }
.detail-grid label { text-align: right; background: #eef3f8; font-weight: 600; }
.detail-grid .wide { grid-column: span 3; }
.pre-wrap { white-space: pre-wrap; }
.full-control { width: 100%; }
.modal-alert { margin-bottom: 14px; }
.round-history-table { max-height: 460px; overflow: auto; border: 1px solid #d9e2ec; }
.round-history-table table { width: 100%; border-collapse: collapse; }
.round-history-table th, .round-history-table td { padding: 9px 10px; border-right: 1px solid #d9e2ec; border-bottom: 1px solid #d9e2ec; text-align: left; vertical-align: top; }
.round-history-table th { position: sticky; top: 0; background: #eef3f8; }
@media (max-width: 1000px) {
  .detail-grid { grid-template-columns: 104px minmax(140px, 1fr); }
  .detail-grid .wide { grid-column: auto; }
  .document-header { grid-template-columns: 80px 1fr 80px; }
}
</style>
