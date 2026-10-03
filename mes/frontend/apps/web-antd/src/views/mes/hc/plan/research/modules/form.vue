<script lang="ts" setup>
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';
import type { MesHcResearchTaskApi } from '#/api/mes/hc/researchtask';
import type { MesHcRouteApi } from '#/api/mes/hc/route';

import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  InputNumber,
  Select,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getModelRuleDetail,
  getModelRuleSelectOptions,
} from '#/api/mes/hc/modelrule';
import {
  archiveResearchTaskToModel,
  confirmResearchTask,
  createResearchTask,
  getResearchTask,
  previewResearchTaskCode,
  updateResearchTask,
} from '#/api/mes/hc/researchtask';
import { getRouteDetail, getRouteSelectOptions } from '#/api/mes/hc/route';

import {
  FORMULA_CODE_OPTIONS,
  POST_PROCESS_CODE_OPTIONS,
  PRODUCT_CLASS_OPTIONS,
  SINGLE_PROCESS_CODE_OPTIONS,
  formatNumber,
  getTaskStatusMeta,
} from '../data';

const emit = defineEmits(['success']);

type EditableResearchTask = MesHcResearchTaskApi.ResearchTask;
type EditableOperation = MesHcResearchTaskApi.RouteOperation & { localKey: string };
type FormulaOption = { label: string; text?: string; value: string };

const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const previewing = ref(false);
const openToken = ref(0);
const routeMaintenanceMode = ref(false);
const routeOptions = ref<MesHcRouteApi.SelectOption[]>([]);
const formulaOptions = ref<FormulaOption[]>(FORMULA_CODE_OPTIONS);
const operationRows = ref<EditableOperation[]>([]);
const checkedOperationKeys = ref<string[]>([]);
const selectedOperationKey = ref('');
let previewTimer: ReturnType<typeof setTimeout> | undefined;
let rowSeed = 0;

const formState = reactive<EditableResearchTask>({
  id: undefined,
  taskNo: '',
  rdModelCode: '',
  displayModelCode: '',
  taskStatus: 'DRAFT',
  researchDate: dayjs().format('YYYY-MM-DD'),
  issueUserId: undefined,
  issueUserName: '',
  issueTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  productClassCode: 'RD',
  productClassName: 'CMP黑垫',
  batchTypeCode: 'C',
  baseFormulaCode: '33',
  baseFormulaName: '33',
  wetProcessCode: '1',
  wetProcessName: '湿法工艺1',
  grindingProcessCode: '1',
  grindingProcessName: '磨皮工艺1',
  postProcessCode: '01',
  postProcessName: '后工艺01',
  reuseSeq: 1,
  routeId: undefined,
  routeCode: '',
  routeName: '',
  routeVersion: '',
  batchRuleCode: 'LOT-RD-PROCESS-DATE',
  targetQty: 0,
  targetUom: 'PCS',
  taskPurpose: '',
  formulaUsageCount: 0,
  wetUsageCount: 0,
  grindingUsageCount: 0,
  postUsageCount: 0,
  combinationUsageCount: 0,
  archivedModelId: undefined,
  archivedModelCode: '',
  archivedTime: '',
  planId: undefined,
  planNo: '',
  remark: '',
});

const statusMeta = computed(() => getTaskStatusMeta(formState.taskStatus));
const readonlyAll = computed(() => formState.taskStatus === 'ARCHIVED');
const codeFieldsLocked = computed(() => !!formState.id && formState.taskStatus !== 'DRAFT');
const canConfirm = computed(() => !!formState.id && formState.taskStatus === 'DRAFT');
const canArchive = computed(() => !!formState.id && formState.taskStatus === 'CONFIRMED');
const isEdit = computed(() => !!formState.id);
const canMaintainRoute = computed(() => !readonlyAll.value && routeMaintenanceMode.value);

function resetFormState(seed?: Partial<EditableResearchTask>) {
  Object.assign(formState, {
    id: undefined,
    taskNo: '',
    rdModelCode: '',
    displayModelCode: '',
    taskStatus: 'DRAFT',
    researchDate: dayjs().format('YYYY-MM-DD'),
    issueUserId: getDefaultIssueUserId(),
    issueUserName: getDefaultIssueUserName(),
    issueTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    productClassCode: 'RD',
    productClassName: 'CMP黑垫',
    batchTypeCode: 'C',
    baseFormulaCode: '33',
    baseFormulaName: '33',
    wetProcessCode: '1',
    wetProcessName: '湿法工艺1',
    grindingProcessCode: '1',
    grindingProcessName: '磨皮工艺1',
    postProcessCode: '01',
    postProcessName: '后工艺01',
    reuseSeq: 1,
    routeId: undefined,
    routeCode: '',
    routeName: '',
    routeVersion: '',
    batchRuleCode: 'LOT-RD-PROCESS-DATE',
    targetQty: 0,
    targetUom: 'PCS',
    taskPurpose: '',
    formulaUsageCount: 0,
    wetUsageCount: 0,
    grindingUsageCount: 0,
    postUsageCount: 0,
    combinationUsageCount: 0,
    archivedModelId: undefined,
    archivedModelCode: '',
    archivedTime: '',
    planId: undefined,
    planNo: '',
    remark: '',
    ...seed,
  });
  if (!formState.issueUserId) formState.issueUserId = getDefaultIssueUserId();
  if (!formState.issueUserName) formState.issueUserName = getDefaultIssueUserName();
  if (!formState.issueTime) formState.issueTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  syncBaseFormulaName();
}

function getDefaultIssueUserId() {
  const id = userStore.userInfo?.id;
  return id == null ? undefined : Number(id);
}

function getDefaultIssueUserName() {
  const userInfo = userStore.userInfo as any;
  return userInfo?.nickname || userInfo?.username || userInfo?.realName || 'system';
}

async function loadBaseOptions() {
  const [routes] = await Promise.all([
    getRouteSelectOptions(),
    loadFormulaDictOptions(),
  ]);
  routeOptions.value = routes;
}

async function loadFormulaDictOptions() {
  try {
    const rules = await getModelRuleSelectOptions();
    const targetRule = rules.find((item) => item.code === 'MODEL-RD')
      || rules.find((item) => item.code === 'MODEL-WHITE')
      || rules[0];
    if (!targetRule?.value) return;
    const detail = await getModelRuleDetail(Number(targetRule.value));
    const formulaDicts = (detail.modelRuleDicts || []).filter(isFormulaDict);
    if (formulaDicts.length === 0) return;
    formulaOptions.value = formulaDicts.map((item) => ({
      label: item.dictCode || item.dictValue || '',
      text: item.dictValue || item.dictCode || '',
      value: item.dictCode || item.dictValue || '',
    })).filter((item) => item.value);
  } catch {
    formulaOptions.value = FORMULA_CODE_OPTIONS;
  }
}

function isFormulaDict(item: MesHcModelRuleApi.ModelRuleDict) {
  const key = `${item.itemCode || ''}|${item.dictCode || ''}|${item.dictValue || ''}`.toLowerCase();
  return key.includes('formula') || key.includes('recipe') || key.includes('配方');
}

async function loadDetail(id: number, token: number) {
  loading.value = true;
  try {
    const detail = await getResearchTask(id);
    if (token !== openToken.value) return;
    resetFormState(detail);
    const snapshotRows = parseSnapshotRouteOperations(detail.snapshotJson);
    if (snapshotRows.length > 0) {
      operationRows.value = snapshotRows;
    } else if (detail.routeId) {
      await loadRouteOperations(Number(detail.routeId));
    } else {
      operationRows.value = [];
    }
  } finally {
    loading.value = false;
  }
}

function handleProductClassChange() {
  formState.productClassName = 'CMP黑垫';
  formState.batchTypeCode = 'C';
}

function handleBaseFormulaChange(value: string) {
  formState.baseFormulaCode = value;
  syncBaseFormulaName();
}

function handleWetProcessChange(value: string) {
  const code = String(value || '').trim();
  formState.wetProcessCode = code;
  formState.wetProcessName = code ? `湿法工艺${code}` : '';
}

function handleGrindingProcessChange(value: string) {
  const code = String(value || '').trim();
  formState.grindingProcessCode = code;
  formState.grindingProcessName = code ? `磨皮工艺${code}` : '';
}

function handlePostProcessChange(value: string) {
  const rawCode = String(value || '').trim();
  const code = rawCode && /^\d$/.test(rawCode) ? rawCode.padStart(2, '0') : rawCode;
  formState.postProcessCode = code;
  formState.postProcessName = code ? `后工艺${code}` : '';
}

function syncBaseFormulaName() {
  formState.baseFormulaName = formulaOptions.value.find((item) => item.value === formState.baseFormulaCode)?.text
    || formState.baseFormulaCode
    || '';
}

async function handleRouteChange(value?: number, option?: any) {
  const selected = routeOptions.value.find((item) => item.value === value) || option;
  formState.routeId = value;
  formState.routeCode = selected?.code || '';
  formState.routeName = selected?.name || selected?.label || '';
  if (value) {
    await loadRouteOperations(Number(value));
  } else {
    operationRows.value = [];
  }
}

async function loadRouteOperations(routeId: number) {
  const detail = await getRouteDetail(routeId);
  formState.routeCode = detail.routeCode || formState.routeCode || '';
  formState.routeName = detail.routeName || formState.routeName || '';
  formState.routeVersion = detail.versionNo || formState.routeVersion || '';
  operationRows.value = sortRouteOperations(detail.routeOperations || []).map((item, index) => normalizeOperation({
    opSeq: item.seqNo || index + 1,
    opCode: item.operationCode,
    opName: item.operationName,
    workCenterId: item.workCenterId,
    workCenterCode: item.workCenterCode,
    workCenterName: item.workCenterName,
    instructionText: item.remark || '',
  }, index));
  checkedOperationKeys.value = [];
  selectedOperationKey.value = operationRows.value[0]?.localKey || '';
}

function sortRouteOperations(rows: MesHcRouteApi.RouteOperation[]) {
  return [...rows].sort((left, right) => Number(left.seqNo || 0) - Number(right.seqNo || 0));
}

function normalizeOperation(row: Partial<MesHcResearchTaskApi.RouteOperation>, index: number): EditableOperation {
  return {
    ...row,
    localKey: `rd-op-${Date.now()}-${++rowSeed}`,
    opSeq: row.opSeq || index + 1,
  };
}

function parseSnapshotRouteOperations(snapshotJson?: string) {
  if (!snapshotJson) return [];
  try {
    const snapshot = JSON.parse(snapshotJson);
    const rows = Array.isArray(snapshot?.routeOperations) ? snapshot.routeOperations : [];
    return rows.map((item: MesHcResearchTaskApi.RouteOperation, index: number) => normalizeOperation(item, index));
  } catch {
    return [];
  }
}

function toggleRouteMaintenanceMode() {
  routeMaintenanceMode.value = !routeMaintenanceMode.value;
  checkedOperationKeys.value = [];
}

function handleOperationChecked(row: EditableOperation, checked: boolean) {
  checkedOperationKeys.value = checked
    ? [...new Set([...checkedOperationKeys.value, row.localKey])]
    : checkedOperationKeys.value.filter((item) => item !== row.localKey);
}

function handleOperationRowClick(row: EditableOperation) {
  selectedOperationKey.value = row.localKey;
}

function moveOperation(localKey: string, delta: number) {
  const index = operationRows.value.findIndex((item) => item.localKey === localKey);
  const nextIndex = index + delta;
  if (index < 0 || nextIndex < 0 || nextIndex >= operationRows.value.length) return;
  const [row] = operationRows.value.splice(index, 1);
  operationRows.value.splice(nextIndex, 0, row!);
  resequenceOperations();
}

function deleteSelectedOperations() {
  if (checkedOperationKeys.value.length > 0) {
    operationRows.value = operationRows.value.filter((item) => !checkedOperationKeys.value.includes(item.localKey));
  } else {
    operationRows.value = [];
  }
  checkedOperationKeys.value = [];
  selectedOperationKey.value = operationRows.value[0]?.localKey || '';
  resequenceOperations();
}

function clearSelectedRoute() {
  formState.routeId = undefined;
  formState.routeCode = '';
  formState.routeName = '';
  formState.routeVersion = '';
  operationRows.value = [];
  checkedOperationKeys.value = [];
  selectedOperationKey.value = '';
}

function resequenceOperations() {
  operationRows.value.forEach((row, index) => {
    row.opSeq = index + 1;
  });
}

function routeOperationPayload(): MesHcResearchTaskApi.RouteOperation[] {
  return operationRows.value.map((row, index) => ({
    opSeq: index + 1,
    opCode: row.opCode,
    opName: row.opName,
    workCenterId: row.workCenterId,
    workCenterCode: row.workCenterCode,
    workCenterName: row.workCenterName,
    instructionText: row.instructionText,
  }));
}

function hasRequiredCodeFields() {
  return Boolean(
    formState.productClassCode
      && formState.baseFormulaCode
      && formState.wetProcessCode
      && formState.grindingProcessCode
      && formState.postProcessCode,
  );
}

async function refreshPreview() {
  if (!hasRequiredCodeFields() || codeFieldsLocked.value) return;
  previewing.value = true;
  try {
    const preview = await previewResearchTaskCode({
      id: formState.id,
      productClassCode: formState.productClassCode,
      baseFormulaCode: formState.baseFormulaCode,
      wetProcessCode: formState.wetProcessCode,
      grindingProcessCode: formState.grindingProcessCode,
      postProcessCode: formState.postProcessCode,
    });
    formState.rdModelCode = preview.rdModelCode || '';
    formState.displayModelCode = preview.displayModelCode || preview.rdModelCode || '';
    formState.reuseSeq = preview.reuseSeq || 1;
    formState.combinationUsageCount = preview.combinationUsageCount || 0;
    formState.formulaUsageCount = preview.formulaUsageCount || 0;
    formState.wetUsageCount = preview.wetUsageCount || 0;
    formState.grindingUsageCount = preview.grindingUsageCount || 0;
    formState.postUsageCount = preview.postUsageCount || 0;
  } finally {
    previewing.value = false;
  }
}

function schedulePreview() {
  if (previewTimer) clearTimeout(previewTimer);
  previewTimer = setTimeout(() => {
    refreshPreview();
  }, 240);
}

function validateForm() {
  if (!formState.productClassCode) {
    message.warning('请选择型号类型');
    return false;
  }
  if (!formState.baseFormulaCode || !formState.wetProcessCode || !formState.grindingProcessCode || !formState.postProcessCode) {
    message.warning('请完整填写基础配方、湿法、磨皮和后工艺编码');
    return false;
  }
  return true;
}

async function handleSave() {
  if (!validateForm()) return;
  saving.value = true;
  try {
    const payload = {
      ...formState,
      routeOperations: routeOperationPayload(),
    };
    if (payload.id) {
      await updateResearchTask(payload);
      message.success('研发型号已更新');
    } else {
      const id = await createResearchTask(payload);
      formState.id = id;
      message.success('研发型号已创建');
    }
    emit('success');
    modalApi.close();
  } finally {
    saving.value = false;
  }
}

async function handleConfirm() {
  if (!formState.id) return;
  await confirmResearchTask(formState.id);
  message.success('研发型号已确认');
  emit('success');
  await loadDetail(formState.id, openToken.value);
}

async function handleArchive() {
  if (!formState.id) return;
  await archiveResearchTaskToModel(formState.id);
  message.success('已转入产品型号字典');
  emit('success');
  await loadDetail(formState.id, openToken.value);
}

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  fullscreenButton: false,
  closable: false,
  header: false,
  footer: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'hc-research-task-modal',
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      openToken.value += 1;
      routeMaintenanceMode.value = false;
      return;
    }
    const currentToken = ++openToken.value;
    routeMaintenanceMode.value = false;
    await loadBaseOptions();
    if (currentToken !== openToken.value) return;
    const data = modalApi.getData<MesHcResearchTaskApi.ResearchTask | null>();
    if (data?.id) {
      await loadDetail(Number(data.id), currentToken);
    } else {
      resetFormState(data || undefined);
      operationRows.value = [];
      await refreshPreview();
    }
  },
});

watch(
  () => formulaOptions.value,
  () => {
    syncBaseFormulaName();
  },
);

watch(
  () => [
    formState.productClassCode,
    formState.baseFormulaCode,
    formState.wetProcessCode,
    formState.grindingProcessCode,
    formState.postProcessCode,
  ],
  () => {
    schedulePreview();
  },
);

onBeforeUnmount(() => {
  openToken.value += 1;
  if (previewTimer) clearTimeout(previewTimer);
});
</script>

<template>
  <Modal>
    <div class="pp-plan-modal research-plan-modal" :class="{ 'is-loading': loading }">
      <div class="pp-plan-toolbar">
        <div class="pp-plan-toolbar__title">
          <span class="pp-plan-toolbar__main">{{ isEdit ? '研发型号详情' : '新建研发型号' }}</span>
          <span class="pp-plan-desc">{{ formState.taskNo || '保存后生成研发任务号' }}</span>
          <Tag :color="statusMeta.color" class="!m-0 border-none font-bold">{{ statusMeta.label }}</Tag>
          <span class="pp-plan-tag">{{ formState.rdModelCode || '编码预览中' }}</span>
        </div>
        <div class="pp-plan-toolbar__actions">
          <Button size="small" :loading="previewing" :disabled="codeFieldsLocked" @click="refreshPreview">
            <template #icon><IconifyIcon icon="lucide:scan-line" /></template>
            预览编号
          </Button>
          <Button v-if="canConfirm" size="small" type="primary" @click="handleConfirm">
            <template #icon><IconifyIcon icon="lucide:check-circle-2" /></template>
            确认
          </Button>
          <Button v-if="canArchive" size="small" type="primary" @click="handleArchive">
            <template #icon><IconifyIcon icon="lucide:archive" /></template>
            转型号字典
          </Button>
          <Button v-if="!readonlyAll" size="small" type="primary" :loading="saving" @click="handleSave">
            <template #icon><IconifyIcon icon="lucide:save" /></template>
            保存
          </Button>
          <Button size="small" @click="modalApi.close()">关闭</Button>
        </div>
      </div>

      <div class="pp-plan-body">
        <fieldset class="pp-fieldset pp-fieldset--basic">
          <legend>1. 目标与基础属性</legend>
          <div class="pp-form-grid pp-form-grid--basic">
            <div class="pp-form-item">
              <label>型号类型</label>
              <Select
                v-model:value="formState.productClassCode"
                :disabled="codeFieldsLocked || readonlyAll"
                :options="PRODUCT_CLASS_OPTIONS"
                @change="handleProductClassChange"
              />
            </div>
            <div class="pp-form-item">
              <label>基础配方</label>
              <Select
                v-model:value="formState.baseFormulaCode"
                show-search
                option-filter-prop="label"
                :disabled="codeFieldsLocked || readonlyAll"
                :options="formulaOptions"
                @change="handleBaseFormulaChange"
              />
            </div>
            <div class="pp-form-item">
              <label>湿法工艺</label>
              <Select
                v-model:value="formState.wetProcessCode"
                mode="combobox"
                show-search
                :disabled="codeFieldsLocked || readonlyAll"
                :options="SINGLE_PROCESS_CODE_OPTIONS"
                @change="handleWetProcessChange"
              />
            </div>
            <div class="pp-form-item">
              <label>磨皮工艺</label>
              <Select
                v-model:value="formState.grindingProcessCode"
                mode="combobox"
                show-search
                :disabled="codeFieldsLocked || readonlyAll"
                :options="SINGLE_PROCESS_CODE_OPTIONS"
                @change="handleGrindingProcessChange"
              />
            </div>
            <div class="pp-form-item">
              <label>后工艺</label>
              <Select
                v-model:value="formState.postProcessCode"
                mode="combobox"
                show-search
                :disabled="codeFieldsLocked || readonlyAll"
                :options="POST_PROCESS_CODE_OPTIONS"
                @change="handlePostProcessChange"
              />
            </div>
            <div class="pp-form-item">
              <label>研发日期</label>
              <DatePicker
                v-model:value="formState.researchDate"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                :disabled="readonlyAll"
                class="w-full"
                placeholder="请选择研发日期"
              />
            </div>
            <div class="pp-form-item">
              <label>任务下达人</label>
              <Input v-model:value="formState.issueUserName" disabled />
            </div>
            <div class="pp-form-item">
              <label>下达时间</label>
              <DatePicker
                v-model:value="formState.issueTime"
                show-time
                format="YYYY/MM/DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                disabled
                class="w-full"
              />
            </div>
            <div class="pp-form-item">
              <label>目标量</label>
              <InputNumber
                v-model:value="formState.targetQty"
                :min="0"
                :precision="3"
                :disabled="readonlyAll"
                class="w-full"
              />
            </div>
            <div class="pp-form-item">
              <label>备注</label>
              <Input v-model:value="formState.remark" :disabled="readonlyAll" />
            </div>
            <div class="pp-form-item pp-form-item--full">
              <label>研发/生产要求</label>
              <Input v-model:value="formState.taskPurpose" :disabled="readonlyAll" />
            </div>
          </div>
        </fieldset>

        <div class="pp-split research-route-split">
          <div class="pp-panel">
            <div class="pp-panel__header">
              <span>2. 工艺路线与工位任务列表</span>
            </div>
            <div class="pp-route-toolbar">
              <span class="pp-route-toolbar__label">配置</span>
              <Select
                v-model:value="formState.routeId"
                size="small"
                show-search
                allow-clear
                :options="routeOptions"
                :disabled="readonlyAll || !routeMaintenanceMode"
                placeholder="请选择工艺路线"
                class="pp-route-toolbar__select"
                option-filter-prop="label"
                @change="handleRouteChange"
              />
              <Button v-if="!readonlyAll" size="small" @click="toggleRouteMaintenanceMode">
                {{ routeMaintenanceMode ? '退出维护' : '维护路线' }}
              </Button>
              <Button v-if="canMaintainRoute" size="small" @click="clearSelectedRoute">清除选择</Button>
              <Button
                v-if="canMaintainRoute"
                size="small"
                danger
                :disabled="operationRows.length === 0"
                @click="deleteSelectedOperations"
              >
                {{ checkedOperationKeys.length > 0 ? '删除选中工序' : '清空当前路线' }}
              </Button>
            </div>
            <div class="pp-table-wrap">
              <table class="pp-grid">
                <thead>
                  <tr>
                    <th v-if="!routeMaintenanceMode" width="52">序号</th>
                    <th v-if="routeMaintenanceMode && !readonlyAll" width="42">选</th>
                    <th v-if="routeMaintenanceMode && !readonlyAll" width="78">顺序</th>
                    <th width="150">工序名称</th>
                    <th width="150">工作中心</th>
                    <th>执行要求</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, rowIndex) in operationRows"
                    :key="row.localKey"
                    :class="{ 'is-selected': selectedOperationKey === row.localKey }"
                    @click="handleOperationRowClick(row)"
                  >
                    <td v-if="!routeMaintenanceMode" align="center">{{ row.opSeq || rowIndex + 1 }}</td>
                    <td v-if="routeMaintenanceMode && !readonlyAll" align="center" @click.stop>
                      <Checkbox
                        :checked="checkedOperationKeys.includes(row.localKey)"
                        @change="(event) => handleOperationChecked(row, event.target.checked)"
                      />
                    </td>
                    <td v-if="routeMaintenanceMode && !readonlyAll" @click.stop>
                      <div class="pp-row-move">
                        <Tooltip title="上移">
                          <Button
                            aria-label="上移工序"
                            size="small"
                            type="text"
                            :disabled="rowIndex === 0"
                            @click="moveOperation(row.localKey, -1)"
                          >
                            <IconifyIcon icon="lucide:arrow-up" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="下移">
                          <Button
                            aria-label="下移工序"
                            size="small"
                            type="text"
                            :disabled="rowIndex === operationRows.length - 1"
                            @click="moveOperation(row.localKey, 1)"
                          >
                            <IconifyIcon icon="lucide:arrow-down" />
                          </Button>
                        </Tooltip>
                      </div>
                    </td>
                    <td>
                      <div class="pp-grid-readonly" :title="row.opName || row.opCode || ''">
                        {{ row.opName || row.opCode || '-' }}
                      </div>
                    </td>
                    <td>
                      <div class="pp-grid-readonly" :title="row.workCenterName || row.workCenterCode || ''">
                        {{ row.workCenterName || row.workCenterCode || '-' }}
                      </div>
                    </td>
                    <td>
                      <Input
                        v-if="routeMaintenanceMode && !readonlyAll"
                        v-model:value="row.instructionText"
                        size="small"
                      />
                      <div v-else class="pp-grid-readonly pp-grid-readonly--multiline" :title="row.instructionText || ''">
                        {{ row.instructionText || '-' }}
                      </div>
                    </td>
                  </tr>
                  <tr v-if="operationRows.length === 0">
                    <td class="pp-empty-row" :colspan="routeMaintenanceMode && !readonlyAll ? 5 : 4">
                      请选择工艺路线后生成工位任务列表
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <fieldset class="pp-fieldset pp-fieldset--usage">
            <legend>3. 使用次数与归档信息</legend>
            <div class="pp-length-grid">
              <div>
                <span>研发型号</span>
                <b>{{ formState.rdModelCode || '-' }}</b>
              </div>
              <div>
                <span>重复序号</span>
                <b>R{{ formState.reuseSeq || 1 }}</b>
              </div>
              <div>
                <span>组合历史</span>
                <b>{{ formState.combinationUsageCount || 0 }}</b>
              </div>
              <div>
                <span>基础配方使用</span>
                <b>{{ formState.formulaUsageCount || 0 }}</b>
              </div>
              <div>
                <span>湿法使用</span>
                <b>{{ formState.wetUsageCount || 0 }}</b>
              </div>
              <div>
                <span>磨皮使用</span>
                <b>{{ formState.grindingUsageCount || 0 }}</b>
              </div>
              <div>
                <span>后工艺使用</span>
                <b>{{ formState.postUsageCount || 0 }}</b>
              </div>
              <div>
                <span>目标量</span>
                <b>{{ formatNumber(formState.targetQty, 3) }}</b>
              </div>
              <div>
                <span>归档型号</span>
                <b>{{ formState.archivedModelCode || '-' }}</b>
              </div>
            </div>
          </fieldset>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.research-plan-modal {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  width: 100%;
  height: 100vh;
  overflow: hidden;
  color: #1e293b;
  background: #dfe7f0;
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 8px 12px;
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-plan-toolbar__title,
.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pp-plan-toolbar__main {
  font-size: 18px;
  font-weight: 900;
}

.pp-plan-desc {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.pp-plan-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  color: #075985;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 900;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.pp-plan-toolbar__actions :deep(.ant-btn) {
  border-radius: 0;
  font-weight: 800;
}

.pp-plan-body {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.pp-fieldset {
  min-width: 0;
  margin: 0;
  padding: 10px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.pp-fieldset legend {
  padding: 0 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}

.pp-form-grid {
  display: grid;
  gap: 8px;
}

.pp-form-grid--basic {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.pp-form-item {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.pp-form-item--full {
  grid-column: span 5;
}

.pp-form-item label {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.pp-form-item :deep(.ant-input),
.pp-form-item :deep(.ant-input-number),
.pp-form-item :deep(.ant-picker),
.pp-form-item :deep(.ant-select-selector) {
  border-radius: 0;
}

.research-route-split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 8px;
  min-height: 0;
  overflow: hidden;
}

.pp-panel {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.pp-panel__header {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
  background: #dbe3ed;
  border-bottom: 1px solid #8794a4;
}

.pp-route-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 6px 8px;
  background: #f8fafc;
  border-bottom: 1px solid #cbd5e1;
}

.pp-route-toolbar__label {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.pp-route-toolbar__select {
  min-width: 280px;
}

.pp-route-toolbar :deep(.ant-btn),
.pp-route-toolbar :deep(.ant-select-selector) {
  border-radius: 0;
  font-weight: 800;
}

.pp-table-wrap {
  height: 100%;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  height: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: #fff;
}

.pp-grid th,
.pp-grid td {
  height: 34px;
  padding: 4px 6px;
  color: #1e293b;
  font-size: 12px;
  border: 1px solid #cbd5e1;
}

.pp-grid th {
  color: #334155;
  font-weight: 900;
  background: #e2e8f0;
}

.pp-grid tr.is-selected td {
  background: #eff6ff;
}

.pp-grid-readonly {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-grid-readonly--multiline {
  white-space: normal;
}

.pp-row-move {
  display: flex;
  justify-content: center;
  gap: 2px;
}

.pp-row-move :deep(.ant-btn) {
  width: 26px;
  height: 26px;
}

.pp-empty-row {
  height: 96px !important;
  color: #64748b !important;
  text-align: center;
  background: #f8fafc;
}

.pp-fieldset--usage {
  display: grid;
  align-content: start;
  min-height: 0;
  overflow: auto;
}

.pp-length-grid {
  display: grid;
  gap: 8px;
}

.pp-length-grid > div {
  display: grid;
  gap: 4px;
  min-height: 54px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.pp-length-grid span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.pp-length-grid b {
  min-width: 0;
  overflow: hidden;
  color: #075985;
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .pp-plan-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pp-plan-toolbar__actions {
    flex-wrap: wrap;
  }

  .pp-form-grid--basic {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pp-form-item--full {
    grid-column: span 2;
  }

  .research-route-split {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
