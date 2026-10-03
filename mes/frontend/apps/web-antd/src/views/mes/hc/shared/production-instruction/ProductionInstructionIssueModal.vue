<script lang="ts" setup>
import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';
import type { PickerOption } from '#/components/picker';
import type {
  ProductionInstructionContext,
  ProductionInstructionSegmentOption,
} from './types';

import { computed, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, DatePicker, Input, InputNumber, Modal, Radio, Select, Textarea, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { getBomProductModelOptions } from '#/api/mes/hc/bom';
import { issueProductionInstruction } from '#/api/mes/hc/production-instruction';
import { PickerModal, processPickerConfig } from '#/components/picker';

import { buildInstructionContextSnapshot } from './types';

interface OperationSelectOption {
  label: string;
  operationCode?: string;
  operationName?: string;
  operationIds?: number[];
  planOperationId?: number;
  processCode?: string;
  processId?: number;
  processName?: string;
  value: string;
}

type BeforeSubmitResult = boolean | Partial<MesHcProductionInstructionApi.Instruction> | void;

const props = withDefaults(
  defineProps<{
    allowProcessPicker?: boolean;
    beforeSubmit?: (
      payload: MesHcProductionInstructionApi.Instruction,
    ) => BeforeSubmitResult | Promise<BeforeSubmitResult>;
    context?: ProductionInstructionContext;
    defaultInstructionType?: string;
    defaultRecipientIds?: number[];
    instructionTypeOptions?: Array<{ label: string; value: string }>;
    open?: boolean;
    operationOptions?: OperationSelectOption[];
    segmentOptions?: ProductionInstructionSegmentOption[];
    showInstructionType?: boolean;
    showOperation?: boolean;
    showSegment?: boolean;
    title?: string;
    zIndex?: number;
  }>(),
  {
    allowProcessPicker: false,
    defaultInstructionType: 'DAILY',
    open: false,
    showInstructionType: true,
    title: '下达生产指令',
    zIndex: 4300,
  },
);

const emit = defineEmits<{
  'update:open': [boolean];
  success: [number | undefined, MesHcProductionInstructionApi.Instruction];
}>();

const visible = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

const submitLoading = ref(false);
const processPickerOpen = ref(false);
const selectedOperationValue = ref<string>();
const targetModelLoading = ref(false);
const targetModelOptions = ref<Array<{ label: string; value: string }>>([]);
const formState = reactive<MesHcProductionInstructionApi.Instruction>({
  batchNo: '',
  instructionContent: '',
  instructionType: 'DAILY',
  issuedTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  operationCode: '',
  operationIds: [],
  operationName: '',
  planNo: '',
  planOperationId: undefined,
  processCode: '',
  processId: undefined,
  processName: '',
  productionBatchNo: '',
  recipientIds: [],
  scopeType: 'OPERATION',
  segmentBatchNo: '',
  beforeMaterialCode: '',
  beforeModelCode: '',
  targetMaterialCode: '',
  targetModelCode: '',
  targetQty: 1,
});

const defaultInstructionTypeOptions = [
  { label: '日常指令', value: 'DAILY' },
  { label: '暂停', value: 'PAUSE' },
  { label: '复工', value: 'RESUME' },
  { label: '作废取消', value: 'CANCEL' },
  { label: '换型指令', value: 'CHANGEOVER' },
  { label: '冻结指令', value: 'FREEZE_STOCK' },
  { label: '解冻指令', value: 'UNFREEZE_STOCK' },
];

const isPlanScope = computed(() => formState.scopeType === 'PLAN');
const isChangeoverInstruction = computed(() => formState.instructionType === 'CHANGEOVER');
const showOperationField = computed(() => props.showOperation ?? !isPlanScope.value);
const showSegmentField = computed(
  () => props.showSegment ?? (!isPlanScope.value && Boolean(props.segmentOptions?.length)),
);
const showInstructionTypeField = computed(() => props.showInstructionType !== false);
const availableInstructionTypeOptions = computed(() => (
  props.instructionTypeOptions?.length ? props.instructionTypeOptions : defaultInstructionTypeOptions
));

const contextText = computed(() => ({
  productionBatchNo: props.context?.productionBatchNo || '-',
  operation: isPlanScope.value ? '全部工序' : formState.operationName || formState.processName || '-',
  planNo: formState.planNo || '-',
  segmentBatchNo: formState.segmentBatchNo || props.context?.segmentBatchNo || '-',
}));

const operationSelectOptions = computed<OperationSelectOption[]>(() => {
  const map = new Map<string, OperationSelectOption>();
  const addOption = (option?: Partial<OperationSelectOption>) => {
    const label = String(option?.label || option?.operationName || option?.processName || '').trim();
    const value = String(option?.value || option?.operationCode || option?.processCode || label).trim();
    if (!label || !value || map.has(value)) return;
    map.set(value, {
      label,
      operationCode: option?.operationCode || option?.processCode || '',
      operationIds: option?.operationIds,
      operationName: option?.operationName || label,
      planOperationId: option?.planOperationId,
      processCode: option?.processCode || option?.operationCode || '',
      processId: option?.processId,
      processName: option?.processName || label,
      value,
    });
  };
  props.operationOptions?.forEach(addOption);
  addOption({
    label: props.context?.operationName || props.context?.processName,
    operationCode: props.context?.operationCode,
    operationIds: props.context?.operationIds,
    operationName: props.context?.operationName,
    planOperationId: props.context?.planOperationId,
    processCode: props.context?.processCode,
    processId: props.context?.processId,
    processName: props.context?.processName,
    value: props.context?.operationCode || props.context?.processCode || props.context?.operationName || props.context?.processName,
  });
  return [...map.values()];
});

function applyOperationOption(option?: OperationSelectOption) {
  if (!option) return;
  selectedOperationValue.value = option.value;
  formState.planOperationId = option.planOperationId;
  formState.operationIds = option.operationIds;
  formState.processId = option.processId;
  formState.processCode = option.processCode || option.operationCode || '';
  formState.processName = option.processName || option.operationName || option.label;
  formState.operationCode = option.operationCode || option.processCode || '';
  formState.operationName = option.operationName || option.processName || option.label;
}

function findOperationOption(value?: string) {
  const text = String(value || '').trim();
  if (!text) return undefined;
  return operationSelectOptions.value.find((option) =>
    option.value === text ||
    option.operationCode === text ||
    option.processCode === text ||
    option.operationName === text ||
    option.processName === text ||
    option.label === text,
  );
}

function syncSelectedOperation() {
  if (!showOperationField.value) {
    selectedOperationValue.value = undefined;
    return;
  }
  const currentValue = formState.operationCode || formState.processCode || formState.operationName || formState.processName;
  const option = findOperationOption(currentValue) || operationSelectOptions.value[0];
  if (option) applyOperationOption(option);
}

function applyContext() {
  const defaultInstructionType = props.context?.instructionType || props.defaultInstructionType || 'DAILY';
  const defaultScopeType = props.context?.scopeType || 'OPERATION';
  Object.assign(formState, {
    batchNo: '',
    id: undefined,
    instructionContent: defaultInstructionContent(defaultInstructionType, defaultScopeType),
    instructionType: defaultInstructionType,
    issuedTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    operationCode: '',
    operationIds: [],
    operationName: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    processCode: '',
    processId: undefined,
    processName: '',
    productionBatchNo: '',
    recipientIds: [],
    scopeType: defaultScopeType,
    segmentBatchNo: '',
    beforeMaterialCode: '',
    beforeModelCode: '',
    targetMaterialCode: '',
    targetModelCode: '',
    targetQty: 1,
    ...buildInstructionContextSnapshot(props.context),
  });
  formState.recipientIds = [];
  syncSelectedOperation();
  if (defaultInstructionType === 'CHANGEOVER') {
    void loadTargetModelOptions();
  }
}

function closeModal() {
  visible.value = false;
}

function handlePickProcess(option: PickerOption) {
  formState.processId = Number(option.id) || undefined;
  formState.processCode = option.code;
  formState.processName = option.name;
  formState.operationCode = option.code;
  formState.operationName = option.name;
  processPickerOpen.value = false;
}

function handleSelectOperation(value: string) {
  applyOperationOption(findOperationOption(value));
}

function handleSelectSegment(value: string) {
  formState.segmentBatchNo = value;
  formState.batchNo = value || formState.productionBatchNo || formState.batchNo;
}

function validateForm() {
  if (!formState.batchNo?.trim()) {
    message.warning('请补充批次号');
    return false;
  }
  if (showOperationField.value && !formState.operationName?.trim() && !formState.processName?.trim()) {
    message.warning('请指定工序');
    return false;
  }
  if (!formState.instructionContent?.trim()) {
    message.warning('请填写指令内容');
    return false;
  }
  if (isChangeoverInstruction.value) {
    if (!formState.planOperationId) {
      message.warning('换型指令必须指定计划工序');
      return false;
    }
    if (!String(formState.targetModelCode || '').trim()) {
      message.warning('请选择换型目标产品型号');
      return false;
    }
    if (
      String(formState.beforeModelCode || '').trim()
      && String(formState.beforeModelCode || '').trim().toUpperCase()
        === String(formState.targetModelCode || '').trim().toUpperCase()
    ) {
      message.warning('目标型号不能与计划型号相同');
      return false;
    }
    if (!Number(formState.targetQty || 0) || Number(formState.targetQty || 0) <= 0) {
      message.warning('请填写大于 0 的目标片数');
      return false;
    }
  }
  return true;
}

async function submitInstruction() {
  if (!validateForm()) return;
  submitLoading.value = true;
  try {
    let payload: MesHcProductionInstructionApi.Instruction = {
      ...formState,
      status: 'ISSUED',
    };
    const beforeSubmitResult = await props.beforeSubmit?.({ ...payload });
    if (beforeSubmitResult === false) return;
    if (beforeSubmitResult && typeof beforeSubmitResult === 'object') {
      payload = { ...payload, ...beforeSubmitResult };
    }
    const id = await issueProductionInstruction(payload);
    message.success('生产指令已下达');
    emit('success', id, { ...payload, id });
    closeModal();
  } finally {
    submitLoading.value = false;
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) applyContext();
  },
);

watch(
  () => props.context,
  () => {
    if (props.open) applyContext();
  },
  { deep: true },
);

watch(
  () => props.operationOptions,
  () => {
    if (props.open) syncSelectedOperation();
  },
  { deep: true },
);

watch(
  () => props.segmentOptions,
  () => {
    if (!props.open || formState.segmentBatchNo) return;
    const firstSegment = props.segmentOptions?.[0]?.value;
    if (firstSegment && showSegmentField.value) handleSelectSegment(firstSegment);
  },
  { deep: true },
);

function defaultInstructionContent(type?: string, scopeType?: string) {
  const prefix = scopeType === 'PLAN' ? '计划' : '工序';
  if (type === 'PAUSE') return `${prefix}暂停`;
  if (type === 'RESUME') return `${prefix}复工`;
  if (type === 'CANCEL') return '作废取消';
  if (type === 'CHANGEOVER') return '换型生产指令';
  if (type === 'FREEZE_STOCK') return '冻结后新报工产出进入待上架冻结品';
  if (type === 'UNFREEZE_STOCK') return '解冻冻结库存：合格品自动退中间边库，NG退待上架不合格品';
  return '';
}

function normalizeTargetModelOption(raw: Record<string, any>) {
  const code = String(raw.productModelCode || raw.code || raw.label || '').trim();
  if (!code) return null;
  const name = String(raw.productModelName || raw.modelName || '').trim();
  return {
    label: name && name !== code ? `${code} / ${name}` : code,
    value: code,
  };
}

function ensureTargetModelOption(modelCode?: string) {
  const code = String(modelCode || '').trim();
  if (!code || targetModelOptions.value.some((option) => option.value === code)) return;
  targetModelOptions.value = [{ label: code, value: code }, ...targetModelOptions.value];
}

async function loadTargetModelOptions(keyword?: string) {
  targetModelLoading.value = true;
  try {
    const rows = await getBomProductModelOptions({
      keyword: String(keyword || '').trim() || undefined,
    });
    const seen = new Set<string>();
    targetModelOptions.value = (Array.isArray(rows) ? rows : [])
      .map((row) => normalizeTargetModelOption(row as Record<string, any>))
      .filter((option): option is { label: string; value: string } => {
        if (!option || seen.has(option.value)) return false;
        seen.add(option.value);
        return true;
      });
  } finally {
    targetModelLoading.value = false;
    ensureTargetModelOption(formState.beforeModelCode);
    ensureTargetModelOption(formState.targetModelCode);
  }
}

const defaultInstructionContents = new Set([
  '',
  '计划暂停',
  '工序暂停',
  '计划复工',
  '工序复工',
  '作废取消',
  '换型生产指令',
]);

watch(
  () => formState.instructionType,
  (type) => {
    const currentContent = String(formState.instructionContent || '').trim();
    if (!currentContent || defaultInstructionContents.has(currentContent)) {
      formState.instructionContent = defaultInstructionContent(type, formState.scopeType);
    }
    if (type === 'CHANGEOVER') {
      formState.scopeType = 'SEGMENT';
      formState.targetQty = Number(formState.targetQty || 1);
      void loadTargetModelOptions();
    }
  },
);

function getPopupContainer(triggerNode?: HTMLElement) {
  return triggerNode?.parentElement || document.body;
}

defineExpose({ reset: applyContext, submit: submitInstruction });
</script>

<template>
  <Modal
    v-model:open="visible"
    :confirm-loading="submitLoading"
    :title="title"
    :z-index="zIndex"
    width="680px"
    @ok="submitInstruction"
  >
    <div class="production-instruction-issue">
      <div class="production-instruction-issue__context">
        <div>
          <span>计划号</span>
          <strong>{{ contextText.planNo }}</strong>
        </div>
        <div>
          <span>母批批号</span>
          <strong>{{ contextText.productionBatchNo }}</strong>
        </div>
        <div>
          <span>分段批号</span>
          <strong>{{ contextText.segmentBatchNo }}</strong>
        </div>
        <div v-if="showOperationField">
          <span>工序</span>
          <strong>{{ contextText.operation }}</strong>
        </div>
        <div v-else>
          <span>范围</span>
          <strong>{{ contextText.operation }}</strong>
        </div>
      </div>

      <div class="production-instruction-issue__grid">
        <label v-if="showInstructionTypeField" class="production-instruction-issue__wide">
          <span>指令类型</span>
          <Radio.Group
            v-model:value="formState.instructionType"
            class="production-instruction-issue__type-group"
            option-type="button"
            :options="availableInstructionTypeOptions"
          />
        </label>
        <label>
          <span>批次号</span>
          <Input v-model:value="formState.batchNo" allow-clear placeholder="请输入批次号" />
        </label>
        <label>
          <span>下达时间</span>
          <DatePicker
            v-model:value="formState.issuedTime"
            class="w-full"
            :get-popup-container="getPopupContainer"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </label>
        <label v-if="showSegmentField" class="production-instruction-issue__wide">
          <span>分段批号</span>
          <Select
            v-model:value="formState.segmentBatchNo"
            :options="segmentOptions"
            allow-clear
            :get-popup-container="getPopupContainer"
            placeholder="不选择则下达全部分段"
            @change="handleSelectSegment"
          />
        </label>
        <label v-if="showOperationField" class="production-instruction-issue__wide">
          <span>工序</span>
          <div class="production-instruction-issue__process">
            <Select
              v-if="operationSelectOptions.length > 0"
              v-model:value="selectedOperationValue"
              :options="operationSelectOptions"
              :get-popup-container="getPopupContainer"
              placeholder="请选择工序"
              @change="handleSelectOperation"
            />
            <Input
              v-else
              :value="formState.operationName || formState.processName"
              :readonly="true"
              placeholder="待指定工序"
            />
            <Button v-if="allowProcessPicker" @click="processPickerOpen = true">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              选择
            </Button>
          </div>
        </label>
        <template v-if="isChangeoverInstruction">
          <label>
            <span>计划型号</span>
            <strong class="production-instruction-issue__readonly">{{ formState.beforeModelCode || '-' }}</strong>
          </label>
          <label>
            <span>目标型号</span>
            <Select
              v-model:value="formState.targetModelCode"
              :filter-option="false"
              :get-popup-container="getPopupContainer"
              :loading="targetModelLoading"
              :options="targetModelOptions"
              allow-clear
              placeholder="输入型号搜索并选择"
              show-search
              @search="loadTargetModelOptions"
            />
          </label>
          <label>
            <span>目标片数</span>
            <InputNumber
              v-model:value="formState.targetQty"
              class="w-full"
              :min="1"
              :precision="0"
              placeholder="请输入需要换型生产的片数"
            />
          </label>
          <div class="production-instruction-issue__strategy">
            达到目标片数后，系统自动完成换型指令并恢复计划型号。胶板由粘胶2执行时按目标型号重新领用。
          </div>
        </template>
        <label class="production-instruction-issue__wide">
          <span>指令内容</span>
          <Textarea
            v-model:value="formState.instructionContent"
            :auto-size="{ minRows: 4, maxRows: 8 }"
            placeholder="请输入需要下达到该工序的生产指令"
          />
        </label>
      </div>
    </div>

    <PickerModal
      v-model:open="processPickerOpen"
      :config="processPickerConfig"
      @select="handlePickProcess"
    />
  </Modal>
</template>

<style scoped>
.production-instruction-issue {
  display: grid;
  gap: 14px;
}

.production-instruction-issue__context {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.production-instruction-issue__context div,
.production-instruction-issue__grid label {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.production-instruction-issue__context span,
.production-instruction-issue__grid label > span {
  font-size: 12px;
  color: #64748b;
}

.production-instruction-issue__context strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.production-instruction-issue__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.production-instruction-issue__wide {
  grid-column: 1 / -1;
}

.production-instruction-issue__process {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 8px;
  align-items: center;
}

.production-instruction-issue__readonly {
  box-sizing: border-box;
  min-height: 32px;
  padding: 5px 11px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.production-instruction-issue__strategy {
  align-self: end;
  padding: 7px 10px;
  font-size: 12px;
  line-height: 18px;
  color: #475569;
  background: #f8fafc;
  border-left: 3px solid #1677ff;
}

.production-instruction-issue__type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.production-instruction-issue__type-group :deep(.ant-radio-button-wrapper) {
  min-width: 86px;
  text-align: center;
}

@media (max-width: 720px) {
  .production-instruction-issue__context,
  .production-instruction-issue__grid,
  .production-instruction-issue__process {
    grid-template-columns: 1fr;
  }
}
</style>
