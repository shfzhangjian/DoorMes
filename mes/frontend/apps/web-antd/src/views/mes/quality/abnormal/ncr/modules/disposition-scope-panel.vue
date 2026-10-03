<script lang="ts" setup>
import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';

import { computed, ref, watch } from 'vue';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { getNcrDispositionContext } from '#/api/mes/quality/abnormal/ncr';

const props = defineProps<{
  disposition?: string;
  ncrId?: number;
}>();

type ScopeLevel = 'MOTHER_BATCH' | 'PIECE' | 'SEGMENT';
type SelectedScopeCandidate = MesNcrApi.DispositionScopeCandidate & {
  dispositionType?: string;
  dispositionRemark?: string;
};

const dispositionOptions = [
  { label: '挑选', value: 'PICK' },
  { label: '返工', value: 'REWORK' },
  { label: '改切', value: 'RECUT' },
  { label: '报废', value: 'SCRAP' },
  { label: '特采', value: 'CONCESSION' },
];

const loading = ref(false);
const context = ref<MesNcrApi.DispositionContext>();
const scopeLevel = ref<ScopeLevel>('PIECE');
const selectedCandidates = ref<SelectedScopeCandidate[]>([]);
const candidatePickerOpen = ref(false);
const pickerSelectedObjectKeys = ref<string[]>([]);
const remark = ref('');
const recutTargetSize = ref('');
const recutTolerance = ref('');
const recutQty = ref<number>();
const concessionReason = ref('');
const hasExistingExecution = computed(() => !!context.value?.existingExecution?.id);

const availableCandidates = computed(() =>
  (context.value?.candidates || []).filter(
    (candidate) => candidate.scopeLevel === scopeLevel.value,
  ),
);

const availableDispositionOptions = computed(() => {
  const values = parseDispositionValues(props.disposition);
  if (values.length === 0) {
    return dispositionOptions;
  }
  const valueSet = new Set(values);
  return dispositionOptions.filter((option) => valueSet.has(option.value));
});

const selectedDispositionTypes = computed(() =>
  selectedCandidates.value
    .map((candidate) => candidate.dispositionType)
    .filter(Boolean) as string[],
);

const hasRecutDisposition = computed(() =>
  selectedDispositionTypes.value.includes('RECUT'),
);

const hasConcessionDisposition = computed(() =>
  selectedDispositionTypes.value.includes('CONCESSION'),
);

const defaultDispositionType = computed(
  () => availableDispositionOptions.value[0]?.value,
);

const selectedObjectKeys = computed(() =>
  selectedCandidates.value
    .map((candidate) => candidate.objectKey)
    .filter(Boolean),
);

const affectedQty = computed(() =>
  sumQuantity(selectedCandidates.value),
);

const selectedQty = computed(() => sumQuantity(selectedCandidates.value));

const selectedColumns = [
  { dataIndex: 'label', title: '处置对象', width: 220 },
  { dataIndex: 'dispositionType', title: '处置选项', width: 150 },
  { dataIndex: 'quantity', title: '对象数量', width: 80 },
  { dataIndex: 'quantityUnit', title: '单位', width: 60 },
  { dataIndex: 'dispositionRemark', title: '处置说明/备注', width: 320 },
  { dataIndex: 'action', title: '操作', width: 80 },
];

const candidateColumns = [
  { dataIndex: 'label', title: '处置对象', width: 220 },
  { dataIndex: 'quantity', title: '对象数量', width: 80 },
  { dataIndex: 'quantityUnit', title: '单位', width: 60 },
  { dataIndex: 'productionLength', title: '生产长度(m)', width: 120 },
  { dataIndex: 'currentStatusName', title: '生产记录状态', width: 180 },
];

const candidateRowSelection = computed(() => ({
  getCheckboxProps: (record: MesNcrApi.DispositionScopeCandidate) => ({
    disabled: record.selectable === false,
  }),
  onChange: (keys: Array<number | string>) => {
    pickerSelectedObjectKeys.value = keys.map(String);
  },
  preserveSelectedRowKeys: true,
  selectedRowKeys: pickerSelectedObjectKeys.value,
}));

function candidateRowKey(row: MesNcrApi.DispositionScopeCandidate) {
  return row.objectKey;
}

watch(
  () => props.ncrId,
  (id) => {
    resetDraft();
    if (id) {
      void loadContext(id);
    }
  },
  { immediate: true },
);

watch(
  () => props.disposition,
  () => {
    const allowed = new Set(
      availableDispositionOptions.value.map((option) => option.value),
    );
    selectedCandidates.value.forEach((candidate) => {
      if (!candidate.dispositionType || !allowed.has(candidate.dispositionType)) {
        candidate.dispositionType = defaultDispositionType.value;
      }
    });
  },
);

async function loadContext(id: number) {
  loading.value = true;
  try {
    const data = await getNcrDispositionContext(id);
    context.value = data;
    scopeLevel.value = (data.existingExecution?.scopeLevel as ScopeLevel) || data.defaultScopeLevel || 'PIECE';
    if (data.existingExecution) {
      selectedCandidates.value = (data.existingExecution.scopes || [])
        .filter((item) => item.scopeRole !== 'PICK_OUTSIDE_SCRAP')
        .map((item) => ({
          currentStatus: item.executionResult,
          currentStatusName: item.executionResult,
          dispositionType:
            item.dispositionType || context.value?.finalDisposition,
          dispositionRemark: item.remark || '',
          label: `${item.pieceNo || item.segmentBatchNo || item.motherBatchNo || item.objectKey || '-'}`,
          motherBatchNo: item.motherBatchNo,
          objectKey: item.objectKey || '',
          pieceNo: item.pieceNo,
          quantity: item.quantity,
          scopeLevel: (item.scopeLevel || 'PIECE') as ScopeLevel,
          quantityUnit: item.scopeLevel === 'MOTHER_BATCH' ? '卷' : item.scopeLevel === 'SEGMENT' ? '段' : '片',
          segmentBatchNo: item.segmentBatchNo,
          selectable: false,
          sourceObjectType: 'EXECUTION_SCOPE',
          sourceObjectNo: item.pieceNo || item.segmentBatchNo || item.motherBatchNo,
        }))
        .filter((item) => !!item.objectKey);
      remark.value = data.existingExecution.remark || '';
    }
  } catch (error: any) {
    context.value = undefined;
    message.error(error?.message || '加载 NCR 处置范围失败');
  } finally {
    loading.value = false;
  }
}

function resetDraft() {
  context.value = undefined;
  selectedCandidates.value = [];
  pickerSelectedObjectKeys.value = [];
  candidatePickerOpen.value = false;
  remark.value = '';
  recutTargetSize.value = '';
  recutTolerance.value = '';
  recutQty.value = undefined;
  concessionReason.value = '';
}

function openCandidatePicker() {
  if (hasExistingExecution.value) {
    return;
  }
  if (!context.value) {
    message.warning('处置范围尚未加载完成');
    return;
  }
  if (availableCandidates.value.length === 0) {
    message.warning(context.value?.emptyReason || '当前来源范围没有可选对象');
    return;
  }
  pickerSelectedObjectKeys.value = [...selectedObjectKeys.value];
  candidatePickerOpen.value = true;
}

function confirmCandidatePicker() {
  const candidateByKey = new Map<string, MesNcrApi.DispositionScopeCandidate>();
  for (const candidate of availableCandidates.value) {
    candidateByKey.set(candidate.objectKey, candidate);
  }
  const existedRemarkByKey = new Map<string, string>();
  const existedDispositionByKey = new Map<string, string>();
  for (const candidate of selectedCandidates.value) {
    existedRemarkByKey.set(candidate.objectKey, candidate.dispositionRemark || '');
    if (candidate.dispositionType) {
      existedDispositionByKey.set(candidate.objectKey, candidate.dispositionType);
    }
  }
  const nextSelectedCandidates: SelectedScopeCandidate[] = [];
  for (const key of pickerSelectedObjectKeys.value) {
    const candidate = candidateByKey.get(key);
    if (!candidate) {
      continue;
    }
    nextSelectedCandidates.push({
      ...candidate,
      dispositionType:
        existedDispositionByKey.get(candidate.objectKey) ||
        defaultDispositionType.value,
      dispositionRemark: existedRemarkByKey.get(candidate.objectKey) || '',
    });
  }
  selectedCandidates.value = nextSelectedCandidates;
  candidatePickerOpen.value = false;
}

function clearSelectedCandidates() {
  if (hasExistingExecution.value) {
    return;
  }
  selectedCandidates.value = [];
}

function removeSelectedCandidate(record: SelectedScopeCandidate) {
  if (hasExistingExecution.value) {
    return;
  }
  selectedCandidates.value = selectedCandidates.value.filter(
    (candidate) => candidate.objectKey !== record.objectKey,
  );
}

function validateAndBuild(
  executionUserId?: number,
  executionUserName?: string,
): MesNcrApi.DispositionScopeConfirmReq | undefined {
  if (!props.ncrId || !context.value) {
    message.warning('处置范围尚未加载完成');
    return undefined;
  }
  if (availableCandidates.value.length === 0) {
    message.warning(context.value?.emptyReason || '当前来源范围没有可选对象');
    return undefined;
  }
  if (selectedCandidates.value.length === 0) {
    message.warning('请点击“选择对象”添加需要处置的对象');
    return undefined;
  }
  const allowedDispositionValues = new Set(
    availableDispositionOptions.value.map((option) => option.value),
  );
  const missingDisposition = selectedCandidates.value.find(
    (candidate) =>
      !candidate.dispositionType ||
      !allowedDispositionValues.has(candidate.dispositionType),
  );
  if (missingDisposition) {
    message.warning(
      `请选择对象 ${missingDisposition.pieceNo || missingDisposition.objectKey || '-'} 的处置选项`,
    );
    return undefined;
  }
  if (!executionUserId) {
    message.warning('请选择处置执行人');
    return undefined;
  }
  if (!hasExistingExecution.value && hasRecutDisposition.value && !recutTargetSize.value.trim()) {
    message.warning('请填写改切目标尺寸');
    return undefined;
  }
  if (
    !hasExistingExecution.value &&
    hasRecutDisposition.value &&
    (!recutQty.value || recutQty.value <= 0)
  ) {
    message.warning('请填写有效的改切指令数量');
    return undefined;
  }
  if (
    !hasExistingExecution.value &&
    hasConcessionDisposition.value &&
    !concessionReason.value.trim()
  ) {
    message.warning('请填写特采理由');
    return undefined;
  }
  return {
    concessionReason: concessionReason.value.trim() || undefined,
    executionUserId,
    executionUserName,
    id: props.ncrId,
    recutQty: recutQty.value,
    recutTargetSize: recutTargetSize.value.trim() || undefined,
    recutTolerance: recutTolerance.value.trim() || undefined,
    remark: remark.value.trim() || undefined,
    scopeLevel: scopeLevel.value,
    scopeRemarks: selectedCandidates.value.map((candidate) => ({
      dispositionType: candidate.dispositionType,
      objectKey: candidate.objectKey,
      remark: candidate.dispositionRemark?.trim() || undefined,
    })),
    selectedObjectKeys: [...selectedObjectKeys.value],
  };
}

function getConfirmSummary() {
  if (hasExistingExecution.value) {
    return `已确认处置范围，提交后所选对象立即按挑选合格生效`;
  }
  return `确认提交 ${selectedCandidates.value.length} 个对象，生成处置执行单并分派给执行人？${context.value?.pickQualification ? "提交后所选对象立即按NCR挑选后合格生效，未选对象保持原状态。" : ""}`;
}

function sumQuantity(items: Array<MesNcrApi.DispositionScopeCandidate | SelectedScopeCandidate>) {
  return items.reduce(
    (sum, candidate) => sum + Number(candidate.quantity || 0),
    0,
  );
}

function parseDispositionValues(value?: string) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index);
}

defineExpose({
  getConfirmSummary,
  validateAndBuild,
});
</script>

<template>
  <div class="ncr-disposition-scope">
    <Spin :spinning="loading">
      <div class="ncr-disposition-scope__summary">
        <span>终审措施：<b>{{ context?.finalDispositionName || '-' }}</b></span>
        <span>NG 工序：<b>{{ context?.ngProcessName || '-' }}</b></span>
        <span>目标工作台：<b>{{ context?.targetWorkstationName || '-' }}</b></span>
        <span>来源批号：<b>{{ context?.sourceLotNo || '-' }}</b></span>
      </div>

      <p v-if="context?.emptyReason" role="alert">{{ context.emptyReason }}</p>
      <p v-if="context?.pickQualification">{{ context.candidateSourceDescription }}</p>
      <div class="ncr-disposition-scope__toolbar">
        <span class="ncr-disposition-scope__label">处置对象明细</span>
        <Button
          :disabled="hasExistingExecution"
          size="small"
          type="primary"
          @click="openCandidatePicker"
        >
          选择对象
        </Button>
        <Button :disabled="hasExistingExecution" size="small" @click="clearSelectedCandidates">清空</Button>
        <Tag color="blue">已选 {{ selectedCandidates.length }} 项</Tag>
      </div>

      <Table
        :columns="selectedColumns"
        :data-source="selectedCandidates"
        :pagination="false"
        :row-key="candidateRowKey"
        :scroll="{ x: 830, y: 260 }"
        bordered
        class="ncr-disposition-scope__table"
        size="small"
      >
        <template #emptyText>
          暂未添加处置对象，请点击“选择对象”从当前来源批号下选择可处置对象。
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'dispositionType'">
            <Select
              v-model:value="record.dispositionType"
              :disabled="hasExistingExecution"
              :options="availableDispositionOptions"
              placeholder="选择处置"
            />
          </template>
          <template v-else-if="column.dataIndex === 'dispositionRemark'">
            <Input.TextArea
              v-model:value="record.dispositionRemark"
              :auto-size="{ minRows: 1, maxRows: 3 }"
              :disabled="hasExistingExecution"
              :maxlength="1000"
              placeholder="填写该对象的处置说明/备注"
            />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <Button
              :disabled="hasExistingExecution"
              danger
              size="small"
              type="link"
              @click="removeSelectedCandidate(record)"
            >
              移除
            </Button>
          </template>
        </template>
      </Table>

      <div
        v-if="hasRecutDisposition && !hasExistingExecution"
        class="ncr-disposition-scope__extra"
      >
        <label>改切目标尺寸</label>
        <Input
          v-model:value="recutTargetSize"
          placeholder="例如：φ500 mm"
        />
        <label>尺寸公差</label>
        <Input v-model:value="recutTolerance" placeholder="例如：±0.5 mm" />
        <label>指令数量</label>
        <InputNumber
          v-model:value="recutQty"
          :min="0.000001"
          :precision="6"
          class="w-full"
        />
      </div>

      <div
        v-if="hasConcessionDisposition && !hasExistingExecution"
        class="ncr-disposition-scope__extra"
      >
        <label>特采理由</label>
        <Input.TextArea
          v-model:value="concessionReason"
          :rows="2"
          placeholder="填写风险接受依据、限制条件或审批结论"
        />
      </div>

      <div class="ncr-disposition-scope__remark">
        <label>范围确认备注</label>
        <Input.TextArea
          v-model:value="remark"
          :rows="2"
          placeholder="说明选择依据、现场隔离状态及交给执行人的注意事项"
        />
      </div>
    </Spin>

    <Modal
      v-model:open="candidatePickerOpen"
      :destroy-on-close="true"
      title="选择处置对象"
      width="720px"
      @ok="confirmCandidatePicker"
    >
      <div class="ncr-disposition-scope__picker-summary">
        <span>来源批号：<b>{{ context?.sourceLotNo || '-' }}</b></span>
        <span>当前工序：<b>{{ context?.ngProcessName || '-' }}</b></span>
        <span>候选规则：{{ context?.candidateSourceDescription || '未被后续工序扫码确认的片号' }}</span>
      </div>
      <Table
        :columns="candidateColumns"
        :data-source="availableCandidates"
        :pagination="false"
        :row-key="candidateRowKey"
        :row-selection="candidateRowSelection"
        :scroll="{ x: 420, y: 420 }"
        bordered
        class="ncr-disposition-scope__table"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'currentStatusName'">
            <Tag :color="record.currentStatus === 'NG' ? 'red' : 'default'">
              {{ record.currentStatusName || record.currentStatus || '-' }}
            </Tag>
          </template>
        </template>
      </Table>
    </Modal>
  </div>
</template>

<style scoped>
.ncr-disposition-scope {
  grid-column: 1 / -1;
  width: 100%;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  font-size: 14px;
  line-height: 20px;
}

.ncr-disposition-scope__summary,
.ncr-disposition-scope__toolbar,
.ncr-disposition-scope__picker-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  align-items: center;
  margin-bottom: 10px;
}

.ncr-disposition-scope__summary,
.ncr-disposition-scope__picker-summary {
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.ncr-disposition-scope__label,
.ncr-disposition-scope__remark > label,
.ncr-disposition-scope__extra > label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.ncr-disposition-scope__summary,
.ncr-disposition-scope__picker-summary,
.ncr-disposition-scope__toolbar,
.ncr-disposition-scope__table :deep(.ant-table),
.ncr-disposition-scope__table :deep(.ant-input),
.ncr-disposition-scope__table :deep(textarea.ant-input),
.ncr-disposition-scope__table :deep(.ant-tag) {
  font-size: 14px;
}

.ncr-disposition-scope__table :deep(.ant-table-cell) {
  line-height: 20px;
}

.ncr-disposition-scope__remark {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 10px;
  align-items: start;
  margin-top: 10px;
}

.ncr-disposition-scope__extra {
  display: grid;
  grid-template-columns: 120px minmax(180px, 1fr) 100px minmax(140px, 1fr) 100px minmax(120px, 1fr);
  gap: 10px;
  align-items: center;
  margin-top: 10px;
}
</style>
