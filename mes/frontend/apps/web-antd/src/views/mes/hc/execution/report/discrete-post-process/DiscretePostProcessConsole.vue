<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  CheckboxGroup,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Pagination,
  RadioGroup,
  Select,
  Switch,
  Table as ATable,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';

import {
  confirmDiscretePostProcessReport,
  createDiscretePostProcessInspectionTask,
  createDiscretePostProcessPlan,
  getDiscretePostProcessCandidates,
  getDiscretePostProcessSourceList,
  getDiscretePostProcessTaskList,
  scanDiscretePostProcessSource,
  type MesHcDiscretePostProcessApi,
} from '#/api/mes/hc/execution/discrete-post-process';

interface OperationOption {
  label: string;
  sourceOpName: string;
  value: 'WC-ADH2' | 'WC-CUT' | 'WC-GROOVE';
}

const props = defineProps<{
  allowedOperations: readonly OperationOption[];
  dispatchOnly?: boolean;
  opCode: 'WC-ADH2' | 'WC-CUT' | 'WC-GROOVE';
  opName: string;
  sourceOpName: string;
}>();

const userStore = useUserStore();

const taskLoading = ref(false);
const sourceLoading = ref(false);
const candidateLoading = ref(false);
const submitLoading = ref(false);
const createLoading = ref(false);

const tasks = ref<MesHcDiscretePostProcessApi.TaskItem[]>([]);
const sources = ref<MesHcDiscretePostProcessApi.SourceItem[]>([]);
const candidates = ref<MesHcDiscretePostProcessApi.CandidateItem[]>([]);
const selectedTask = ref<MesHcDiscretePostProcessApi.TaskItem>();
const selectedSource = ref<MesHcDiscretePostProcessApi.SourceItem>();
const selectedCandidateKeys = ref<string[]>([]);
const selectedCandidateSnapshots = ref<MesHcDiscretePostProcessApi.CandidateItem[]>([]);
const candidatePage = ref(1);
const candidatePageSize = 10;
const showSelectedOnly = ref(false);

const taskQuery = reactive({
  keyword: '',
  taskStatus: 'PENDING',
});
const sourceQuery = reactive({
  keyword: '',
  taskStatus: 'ALL',
});
const candidateQuery = reactive({
  keyword: '',
  locationKeyword: '',
  materialCode: '',
  modelNo: '',
  reportDate: '',
});
const createForm = reactive({
  executionRequirement: '',
  operationCodes: [] as string[],
  planModel: '',
  releaseNow: true,
  remark: '',
});
const reportForm = reactive({
  actualSizeRule: undefined as string | undefined,
  actualSizeSuffix: undefined as string | undefined,
  defectCode: '',
  inputQty: 1,
  lossQty: 0,
  outputQty: 1,
  remark: '',
  reportResult: 'OK',
});
const scanForm = reactive({
  pieceNo: '',
});

const createModalOpen = ref(false);
const reportModalOpen = ref(false);

const taskStats = computed(() => {
  return tasks.value.reduce(
    (acc, row) => {
      acc.planCount += 1;
      acc.pending += Number(row.pendingCount || 0);
      acc.finished += Number(row.finishedCount || 0);
      return acc;
    },
    { finished: 0, pending: 0, planCount: 0 },
  );
});
const pageTitle = computed(() => (props.dispatchOnly ? '离散后加工派工' : `离散${props.opName}报工`));
const currentOperationOptions = computed(() =>
  props.allowedOperations.map((item) => ({ label: item.label, value: item.value })),
);
const firstSelectedOperation = computed(() => {
  const selected = new Set(createForm.operationCodes);
  return props.allowedOperations.find((item) => selected.has(item.value));
});
const candidatePlaceholder = computed(() => {
  const first = firstSelectedOperation.value;
  if (!first) {
    return '请先选择加工工序';
  }
  const sourceName = first?.sourceOpName || props.sourceOpName;
  const opName = first?.label || props.opName;
  return `${sourceName}NG片号/${opName}未报工`;
});
const selectedCandidateRows = computed(() => selectedCandidateSnapshots.value);
const displayCandidates = computed(() => {
  const sourceRows = showSelectedOnly.value ? selectedCandidateRows.value : candidates.value;
  return [...sourceRows].sort(compareCandidateByReportTimeDesc);
});
const candidateTotal = computed(() => displayCandidates.value.length);
const pagedCandidates = computed(() => {
  const start = (candidatePage.value - 1) * candidatePageSize;
  return displayCandidates.value.slice(start, start + candidatePageSize);
});
const selectedModelPrefixes = computed(() =>
  Array.from(new Set(
    selectedCandidateRows.value
      .map((row) => resolveModelPrefix(row.modelNo))
      .filter(Boolean),
  )),
);
const selectedModelPrefixText = computed(() => {
  if (!selectedCandidateRows.value.length) return '-';
  if (!selectedModelPrefixes.value.length) return '未带出';
  return selectedModelPrefixes.value.join('、');
});
const selectedModelPrefixValid = computed(() => selectedModelPrefixes.value.length <= 1);
const planModelPrefixValid = computed(() => isPlanModelMatchingSelection(createForm.planModel));

const statusTextMap: Record<string, string> = {
  ACTIVE: '待报工',
  CANCELED: '已取消',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
  COMPLETED: '已完成',
  CONSUMED: '已报工',
  CREATED: '已创建',
  DRAFT: '草稿',
  FAILED: '失败',
  FINISHED: '已完成',
  IN_PROGRESS: '生产中',
  NG: '不合格',
  NORMAL: '正常',
  NOT_POSTED: '未入库',
  OK: '合格',
  PASS: '合格',
  PENDING: '待处理',
  POSTED: '已入库',
  RELEASED: '已释放',
  REVERSED: '已冲销',
  REWORKING: '返工加工中',
  RETURNED: '已返工',
  FROZEN: '已冻结',
  STORED: '已上架',
  WAIT_FREEZE_SHELF: '待冻结上架',
  WAIT_SHELF: '待上架',
  OUTBOUNDED: '已出库',
  SCRAPPED: '已报废',
  UNFINISHED: '未完成',
  WAIT_INSPECTION: '待送检',
  WAIT_NG_SHELF: '待不良入库',
  NG_STORED: '不合格品已入库',
  NG_FROZEN: '已入冻结库',
};

const planStatusTextMap: Record<string, string> = {
  ...statusTextMap,
  ACTIVE: '执行中',
  PENDING: '待下达',
  RELEASED: '已下达',
};

const sourceTypeTextMap: Record<string, string> = {
  ADHESIVE: '粘胶1',
  ADHESIVE1: '粘胶1',
  ADHESIVE2: '粘胶2',
  ADHESIVE2_REPORT: '粘胶2',
  CUT_ROUND: '裁切',
  CUT_ROUND_REPORT: '裁切',
  PRESS_SLOT: '压槽',
  PRESS_SLOT_REPORT: '压槽',
  SLITTING: '分切',
  SLITTING_SLICE: '分切',
};

function formatStatusText(status?: string, scope: 'default' | 'plan' = 'default') {
  const key = String(status || '').toUpperCase();
  if (!key) {
    return '-';
  }
  return (scope === 'plan' ? planStatusTextMap[key] : statusTextMap[key]) || status || '-';
}

function formatSourceType(value?: string) {
  const key = String(value || '').toUpperCase();
  return sourceTypeTextMap[key] || value || '-';
}

function statusTag(status?: string, scope: 'default' | 'plan' = 'default') {
  const text = formatStatusText(status, scope);
  const key = String(status || '').toUpperCase();
  const color =
    key === 'ACTIVE' || key === 'PENDING' || key === 'RELEASED' || key === 'IN_PROGRESS'
      ? 'processing'
      : key === 'CONSUMED' || key === 'COMPLETED' || key === 'FINISHED' || key === 'POSTED'
        ? 'success'
        : key === 'WAIT_INSPECTION' || key === 'WAIT_NG_SHELF'
          ? 'warning'
          : 'default';
  return h(Tag, { color }, () => text);
}

function formatReportDate(value?: string) {
  return value ? value.slice(0, 10) : '-';
}

function resolveModelPrefix(modelNo?: string) {
  const text = String(modelNo || '').trim().toUpperCase();
  return text.length > 3 ? text.slice(0, 3) : text;
}

function compareCandidateByReportTimeDesc(
  left: MesHcDiscretePostProcessApi.CandidateItem,
  right: MesHcDiscretePostProcessApi.CandidateItem,
) {
  const leftTime = left.reportTime || '';
  const rightTime = right.reportTime || '';
  if (leftTime !== rightTime) {
    return rightTime.localeCompare(leftTime);
  }
  return (left.batchNo || '').localeCompare(right.batchNo || '');
}

const taskColumns = [
  { dataIndex: 'planNo', fixed: 'left', title: '计划号', width: 150 },
  { dataIndex: 'planStatus', title: '计划状态', width: 100, customRender: ({ text }: { text: string }) => statusTag(text, 'plan') },
  { dataIndex: 'modelCode', title: '型号', width: 120 },
  { dataIndex: 'materialCode', title: '料号', width: 140 },
  { dataIndex: 'sourceCount', title: '片数', width: 80 },
  { dataIndex: 'pendingCount', title: '待报', width: 80 },
  { dataIndex: 'finishedCount', title: '已报', width: 80 },
  { dataIndex: 'sourcePlanNos', title: '原计划', width: 220 },
  { dataIndex: 'firstSourceBatchNo', title: '首个片号', width: 150 },
  { dataIndex: 'lastReportTime', title: '最后报工', width: 170 },
];

const sourceColumns = [
  { dataIndex: 'pieceNo', fixed: 'left', title: '片号', width: 170 },
  { dataIndex: 'lockStatus', title: '状态', width: 100, customRender: ({ text }: { text: string }) => statusTag(text) },
  { dataIndex: 'sourcePlanNo', title: '原计划', width: 150 },
  { dataIndex: 'sourceParentBatchNo', title: '原分段批号', width: 160 },
  { dataIndex: 'sourceType', title: '来源工序', width: 110, customRender: ({ text }: { text?: string }) => formatSourceType(text) },
  { dataIndex: 'consumeTime', title: '报工时间', width: 170 },
  { dataIndex: 'outputStockPostStatus', title: '入库状态', width: 110, customRender: ({ text }: { text: string }) => statusTag(text) },
  { dataIndex: 'inspectionStatus', title: '送检状态', width: 120, customRender: ({ text }: { text: string }) => statusTag(text) },
  {
    fixed: 'right',
    key: 'action',
    title: '操作',
    width: 180,
    customRender: ({ record }: { record: MesHcDiscretePostProcessApi.SourceItem }) =>
      h('div', { class: 'row-actions' }, [
        h(
          Button,
          {
            disabled: record.lockStatus !== 'ACTIVE',
            size: 'small',
            type: 'link',
            onClick: () => openReport(record),
          },
          () => '报工',
        ),
        h(
          Button,
          {
            disabled: record.lockStatus !== 'CONSUMED',
            size: 'small',
            type: 'link',
            onClick: () => submitInspection(record),
          },
          () => '送检',
        ),
      ]),
  },
];

const candidateColumns = [
  { dataIndex: 'batchNo', title: '片号', width: 220 },
  { dataIndex: 'modelNo', title: '型号', width: 130 },
  { dataIndex: 'candidateStatus', title: '可选来源', width: 130 },
  {
    dataIndex: 'reportTime',
    title: '报工日期',
    width: 130,
    customRender: ({ text }: { text?: string }) => formatReportDate(text),
  },
  { dataIndex: 'locationName', title: '库位', width: 180 },
];

const candidateRowSelection = computed(() => ({
  preserveSelectedRowKeys: true,
  selectedRowKeys: selectedCandidateKeys.value,
  onChange: (keys: (number | string)[]) => {
    selectedCandidateKeys.value = keys.map((key) => String(key));
    syncSelectedCandidateSnapshots();
  },
}));

function getCandidateKey(row: MesHcDiscretePostProcessApi.CandidateItem) {
  if (row.candidateKey) return row.candidateKey;
  if (row.ngPieceId) return `N:${row.ngPieceId}`;
  if (row.sourceLockId) return `L:${row.sourceLockId}`;
  return `S:${row.stockId}`;
}

function syncSelectedCandidateSnapshots() {
  const selectedKeySet = new Set(selectedCandidateKeys.value);
  const nextRows = selectedCandidateSnapshots.value.filter((row) =>
    selectedKeySet.has(getCandidateKey(row)),
  );
  const nextKeySet = new Set(nextRows.map((row) => getCandidateKey(row)));
  for (const row of candidates.value) {
    const key = getCandidateKey(row);
    if (selectedKeySet.has(key) && !nextKeySet.has(key)) {
      nextRows.push(row);
      nextKeySet.add(key);
    }
  }
  selectedCandidateSnapshots.value = nextRows;
  syncPlanModelWithSelection();
}

function clearSelectedCandidates() {
  selectedCandidateKeys.value = [];
  selectedCandidateSnapshots.value = [];
  showSelectedOnly.value = false;
  candidatePage.value = 1;
  createForm.planModel = '';
}

function removeSelectedCandidate(candidateKey: string) {
  selectedCandidateKeys.value = selectedCandidateKeys.value.filter((key) => key !== candidateKey);
  syncSelectedCandidateSnapshots();
  if (!selectedCandidateKeys.value.length) {
    showSelectedOnly.value = false;
  }
  candidatePage.value = 1;
}

function resolveSuggestedPlanModel() {
  const modelCodes = Array.from(new Set(
    selectedCandidateRows.value
      .map((row) => String(row.modelNo || '').trim().toUpperCase())
      .filter(Boolean),
  ));
  if (modelCodes.length === 1) {
    return modelCodes[0];
  }
  return selectedModelPrefixes.value.length === 1 ? selectedModelPrefixes.value[0] : '';
}

function isPlanModelMatchingSelection(value?: string) {
  const text = String(value || '').trim();
  if (!text || !selectedModelPrefixes.value.length || !selectedModelPrefixValid.value) {
    return true;
  }
  return resolveModelPrefix(text) === selectedModelPrefixes.value[0];
}

function syncPlanModelWithSelection() {
  const suggestion = resolveSuggestedPlanModel();
  if (!suggestion) {
    if (!selectedCandidateRows.value.length) {
      createForm.planModel = '';
    }
    return;
  }
  if (!createForm.planModel || !isPlanModelMatchingSelection(createForm.planModel)) {
    createForm.planModel = suggestion;
  }
}

function toggleSelectedOnly() {
  if (!selectedCandidateKeys.value.length) {
    message.warning('请先选择片号');
    return;
  }
  showSelectedOnly.value = !showSelectedOnly.value;
  candidatePage.value = 1;
}

function splitSelectedCandidates() {
  const ngPieceIds: number[] = [];
  const stockIds: number[] = [];
  const sourceLockIds: number[] = [];
  for (const key of selectedCandidateKeys.value) {
    const [type, rawId] = key.split(':');
    const id = Number(rawId);
    if (!Number.isFinite(id)) {
      continue;
    }
    if (type === 'L') {
      sourceLockIds.push(id);
    } else if (type === 'N') {
      ngPieceIds.push(id);
    } else {
      stockIds.push(id);
    }
  }
  return { ngPieceIds, sourceLockIds, stockIds };
}

async function loadTasks(preserveSelection = true) {
  taskLoading.value = true;
  try {
    const rows = await getDiscretePostProcessTaskList({
      keyword: taskQuery.keyword || undefined,
      opCode: props.opCode,
      taskStatus: taskQuery.taskStatus,
    });
    tasks.value = rows || [];
    const nextSelected = preserveSelection
      ? tasks.value.find((item) => item.planOperationId === selectedTask.value?.planOperationId)
      : undefined;
    selectedTask.value = nextSelected || tasks.value[0];
    await loadSources();
  } finally {
    taskLoading.value = false;
  }
}

async function loadSources() {
  if (!selectedTask.value?.planOperationId) {
    sources.value = [];
    return;
  }
  sourceLoading.value = true;
  try {
    sources.value = await getDiscretePostProcessSourceList({
      keyword: sourceQuery.keyword || undefined,
      planOperationId: selectedTask.value.planOperationId,
      taskStatus: sourceQuery.taskStatus,
    });
  } finally {
    sourceLoading.value = false;
  }
}

async function loadCandidates() {
  const firstOperation = firstSelectedOperation.value;
  if (!firstOperation) {
    candidates.value = [];
    selectedCandidateSnapshots.value = [];
    candidatePage.value = 1;
    return;
  }
  candidateLoading.value = true;
  try {
    candidates.value = await getDiscretePostProcessCandidates({
      keyword: candidateQuery.keyword || undefined,
      locationKeyword: candidateQuery.locationKeyword || undefined,
      materialCode: candidateQuery.materialCode || undefined,
      modelNo: candidateQuery.modelNo || undefined,
      reportDate: candidateQuery.reportDate || undefined,
      sourcePool: 'NG',
      targetOpCode: firstOperation.value,
    });
    candidatePage.value = 1;
    syncSelectedCandidateSnapshots();
  } finally {
    candidateLoading.value = false;
  }
}

function openCreateModal() {
  createForm.operationCodes = props.allowedOperations.map((item) => item.value);
  createForm.executionRequirement = '';
  createForm.planModel = '';
  createForm.releaseNow = true;
  createForm.remark = '';
  clearSelectedCandidates();
  createModalOpen.value = true;
  void loadCandidates();
}

function ensureCurrentOperationSelected(values: Array<boolean | number | string>) {
  const selected = new Set(values.map((item) => String(item)));
  const selectedIndexes = props.allowedOperations
    .map((item, index) => (selected.has(item.value) ? index : -1))
    .filter((index) => index >= 0);
  if (!selectedIndexes.length) {
    createForm.operationCodes = [];
  } else {
    const minIndex = Math.min(...selectedIndexes);
    const maxIndex = Math.max(...selectedIndexes);
    createForm.operationCodes = props.allowedOperations
      .slice(minIndex, maxIndex + 1)
      .map((item) => item.value);
  }
  clearSelectedCandidates();
  void loadCandidates();
}

async function submitCreatePlan() {
  if (!createForm.operationCodes.length) {
    message.warning('请选择加工工序');
    return;
  }
  if (!selectedCandidateKeys.value.length) {
    message.warning('请选择来源片号');
    return;
  }
  if (!selectedModelPrefixValid.value) {
    message.warning(`离散后加工同一计划只能选择型号前三位相同的片号；当前已选：${selectedModelPrefixText.value}`);
    return;
  }
  if (!String(createForm.planModel || '').trim()) {
    message.warning('请填写计划型号');
    return;
  }
  if (!planModelPrefixValid.value) {
    message.warning(`计划型号前三位必须与已选片号一致；当前已选：${selectedModelPrefixText.value}`);
    return;
  }
  if (!String(createForm.executionRequirement || '').trim()) {
    message.warning('请填写执行要求');
    return;
  }
  const { ngPieceIds, sourceLockIds, stockIds } = splitSelectedCandidates();
  createLoading.value = true;
  try {
    const planId = await createDiscretePostProcessPlan({
      operationCodes: createForm.operationCodes,
      executionRequirement: createForm.executionRequirement.trim(),
      ngPieceIds,
      planModel: createForm.planModel.trim(),
      releaseNow: createForm.releaseNow,
      remark: createForm.remark || undefined,
      sourceLockIds,
      stockIds,
    });
    message.success(`已创建离散后加工计划：${planId}`);
    createModalOpen.value = false;
    clearSelectedCandidates();
    await loadTasks(false);
  } finally {
    createLoading.value = false;
  }
}

function selectTask(row: MesHcDiscretePostProcessApi.TaskItem) {
  selectedTask.value = row;
  void loadSources();
}

function openReport(row: MesHcDiscretePostProcessApi.SourceItem) {
  selectedSource.value = row;
  reportForm.reportResult = 'OK';
  reportForm.inputQty = Number(row.remainingQty || 1);
  reportForm.outputQty = Number(row.remainingQty || 1);
  reportForm.lossQty = 0;
  reportForm.defectCode = '';
  reportForm.remark = '';
  reportForm.actualSizeRule = undefined;
  reportForm.actualSizeSuffix = undefined;
  reportModalOpen.value = true;
}

async function scanAndReport() {
  if (!selectedTask.value?.planOperationId) {
    message.warning('请选择计划');
    return;
  }
  if (!scanForm.pieceNo.trim()) {
    message.warning('请输入片号');
    return;
  }
  const row = await scanDiscretePostProcessSource({
    pieceNo: scanForm.pieceNo.trim(),
    planOperationId: selectedTask.value.planOperationId,
  });
  openReport(row);
  scanForm.pieceNo = '';
}

async function submitReport() {
  if (!selectedTask.value?.planOperationId || !selectedSource.value?.lockId) {
    return;
  }
  submitLoading.value = true;
  try {
    await confirmDiscretePostProcessReport({
      actualSizeRule: reportForm.actualSizeRule,
      actualSizeSuffix: reportForm.actualSizeSuffix,
      confirmerName: userStore.userInfo?.nickname,
      defectCode: reportForm.defectCode || undefined,
      inputQty: reportForm.inputQty,
      lockId: selectedSource.value.lockId,
      lossQty: reportForm.lossQty,
      outputQty: reportForm.reportResult === 'NG' ? 0 : reportForm.outputQty,
      pieceNo: selectedSource.value.pieceNo,
      planOperationId: selectedTask.value.planOperationId,
      recorderName: userStore.userInfo?.nickname,
      remark: reportForm.remark || undefined,
      reportResult: reportForm.reportResult,
      selfCheck: reportForm.reportResult,
    });
    message.success('报工已确认');
    reportModalOpen.value = false;
    await loadTasks(true);
  } finally {
    submitLoading.value = false;
  }
}

async function submitInspection(row: MesHcDiscretePostProcessApi.SourceItem) {
  const task = await createDiscretePostProcessInspectionTask({
    inspectionType: 'PROCESS',
    lockId: row.lockId,
    remark: `离散后加工${props.opName}送检`,
  });
  message.success(`已送检：${task.inspectionTaskNo || task.id}`);
  await loadSources();
}

watch(
  () => reportForm.reportResult,
  (value) => {
    if (value === 'NG') {
      reportForm.lossQty = reportForm.inputQty;
      reportForm.outputQty = 0;
    } else {
      reportForm.lossQty = 0;
      reportForm.outputQty = reportForm.inputQty;
    }
  },
);

watch(candidateTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / candidatePageSize));
  if (candidatePage.value > maxPage) {
    candidatePage.value = maxPage;
  }
});

onMounted(() => {
  if (!props.dispatchOnly) {
    void loadTasks(false);
  }
});
</script>

<template>
  <Page :title="pageTitle">
    <div class="discrete-console">
      <section v-if="dispatchOnly" class="toolbar-band dispatch-band">
        <div class="toolbar-main">
          <Button type="primary" @click="openCreateModal">
            <template #icon><IconifyIcon icon="lucide:plus" /></template>
            新建离散后加工计划
          </Button>
          <span class="dispatch-hint">选择首工序后按规则筛选可加工片号，确认后生成离散计划并锁定库存。</span>
        </div>
      </section>

      <section v-if="!dispatchOnly" class="toolbar-band">
        <div class="toolbar-main">
          <Input
            v-model:value="taskQuery.keyword"
            allow-clear
            class="toolbar-input"
            placeholder="计划/片号/原计划"
            @press-enter="loadTasks(false)"
          />
          <Select
            v-model:value="taskQuery.taskStatus"
            class="status-select"
            :options="[
              { label: '待办', value: 'PENDING' },
              { label: '已完工', value: 'COMPLETED' },
              { label: '全部', value: 'ALL' },
            ]"
          />
          <Button @click="loadTasks(false)">
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            刷新
          </Button>
          <Button type="primary" @click="openCreateModal">
            <template #icon><IconifyIcon icon="lucide:plus" /></template>
            新建离散计划
          </Button>
        </div>
        <div class="stat-strip">
          <span>计划 {{ taskStats.planCount }}</span>
          <span>待报 {{ taskStats.pending }}</span>
          <span>已报 {{ taskStats.finished }}</span>
        </div>
      </section>

      <section v-if="!dispatchOnly" class="table-band">
        <ATable
          :columns="taskColumns"
          :data-source="tasks"
          :loading="taskLoading"
          :pagination="{ pageSize: 6, showSizeChanger: false }"
          :row-class-name="(row) => row.planOperationId === selectedTask?.planOperationId ? 'selected-row' : ''"
          :row-key="(row) => row.planOperationId"
          bordered
          size="small"
          @row="(row) => ({ onClick: () => selectTask(row) })"
        />
      </section>

      <section v-if="!dispatchOnly" class="toolbar-band source-toolbar">
        <div class="toolbar-main">
          <Input
            v-model:value="scanForm.pieceNo"
            allow-clear
            class="scan-input"
            placeholder="扫码片号"
            @press-enter="scanAndReport"
          />
          <Button type="primary" :disabled="!selectedTask" @click="scanAndReport">
            <template #icon><IconifyIcon icon="lucide:scan-line" /></template>
            扫码报工
          </Button>
          <Input
            v-model:value="sourceQuery.keyword"
            allow-clear
            class="toolbar-input"
            placeholder="筛选片号/来源"
            @press-enter="loadSources"
          />
          <Select
            v-model:value="sourceQuery.taskStatus"
            class="status-select"
            :options="[
              { label: '全部', value: 'ALL' },
              { label: '待报', value: 'PENDING' },
              { label: '已报', value: 'COMPLETED' },
            ]"
          />
          <Button :disabled="!selectedTask" @click="loadSources">
            <template #icon><IconifyIcon icon="lucide:list-filter" /></template>
            查询
          </Button>
        </div>
        <div class="selected-plan">
          {{ selectedTask?.planNo || '-' }} / {{ selectedTask?.sourceCount || 0 }} 片
        </div>
      </section>

      <section v-if="!dispatchOnly" class="table-band source-table">
        <ATable
          :columns="sourceColumns"
          :data-source="sources"
          :loading="sourceLoading"
          :pagination="{ pageSize: 12, showSizeChanger: false }"
          :row-key="(row) => row.lockId"
          bordered
          size="small"
        />
      </section>
    </div>

    <Modal
      v-model:open="createModalOpen"
      :confirm-loading="createLoading"
      :width="1100"
      title="新建离散后加工计划"
      wrap-class-name="discrete-create-plan-modal"
      @ok="submitCreatePlan"
    >
      <div class="create-layout">
        <Form class="create-plan-main-form" layout="inline">
          <FormItem label="加工工序">
            <CheckboxGroup
              v-model:value="createForm.operationCodes"
              :options="currentOperationOptions"
              @change="ensureCurrentOperationSelected"
            />
          </FormItem>
          <FormItem label="下达">
            <Switch v-model:checked="createForm.releaseNow" />
          </FormItem>
          <FormItem label="计划型号" required>
            <Input
              v-model:value="createForm.planModel"
              allow-clear
              class="plan-model-input"
              :status="planModelPrefixValid ? undefined : 'error'"
              placeholder="如 W33P0200"
            />
          </FormItem>
        </Form>
        <div class="candidate-filter">
          <Input
            v-model:value="candidateQuery.keyword"
            allow-clear
            class="toolbar-input"
            :placeholder="candidatePlaceholder"
            @press-enter="loadCandidates"
          />
          <Input
            v-model:value="candidateQuery.materialCode"
            allow-clear
            class="short-input"
            placeholder="料号"
            @press-enter="loadCandidates"
          />
          <Input
            v-model:value="candidateQuery.modelNo"
            allow-clear
            class="short-input"
            placeholder="型号"
            @press-enter="loadCandidates"
          />
          <Input
            v-model:value="candidateQuery.locationKeyword"
            allow-clear
            class="short-input"
            placeholder="库位"
            @press-enter="loadCandidates"
          />
          <DatePicker
            v-model:value="candidateQuery.reportDate"
            allow-clear
            class="date-input"
            format="YYYY-MM-DD"
            placeholder="扫码确认日期"
            value-format="YYYY-MM-DD"
            @change="loadCandidates"
          />
          <Button @click="loadCandidates">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <span class="selection-text">已选 {{ selectedCandidateKeys.length }} 片</span>
          <Button
            :disabled="!selectedCandidateKeys.length"
            :type="showSelectedOnly ? 'primary' : 'default'"
            size="small"
            @click="toggleSelectedOnly"
          >
            {{ showSelectedOnly ? '显示全部片号' : '显示已选片号' }}
          </Button>
        </div>
        <div class="candidate-table-wrap">
          <ATable
            class="candidate-table"
            :columns="candidateColumns"
            :data-source="pagedCandidates"
            :loading="candidateLoading"
            :pagination="false"
            :row-key="getCandidateKey"
            :row-selection="candidateRowSelection"
            :scroll="{ y: '100%' }"
            bordered
            size="small"
          />
          <div class="candidate-bottom">
            <div class="selected-piece-panel">
              <div class="selected-piece-title">
                <span>已选片号</span>
                <span class="selected-model-prefix" :class="{ 'is-invalid': !selectedModelPrefixValid }">
                  型号前三位：{{ selectedModelPrefixText }}
                </span>
                <Button
                  v-if="selectedCandidateRows.length"
                  size="small"
                  type="link"
                  @click="clearSelectedCandidates"
                >
                  清空
                </Button>
              </div>
              <div v-if="selectedCandidateRows.length" class="selected-piece-list">
                <Tag
                  v-for="row in selectedCandidateRows"
                  :key="getCandidateKey(row)"
                  closable
                  @close="removeSelectedCandidate(getCandidateKey(row))"
                >
                  {{ row.batchNo || row.sourceBatchNo || getCandidateKey(row) }}
                </Tag>
              </div>
              <div v-else class="selected-piece-empty">暂无已选片号</div>
            </div>
            <Form class="create-plan-extra-form" layout="vertical">
              <FormItem label="执行要求" required>
                <Textarea
                  v-model:value="createForm.executionRequirement"
                  allow-clear
                  :auto-size="{ minRows: 2, maxRows: 3 }"
                  placeholder="填写本次离散后加工执行要求"
                />
              </FormItem>
            </Form>
            <Pagination
              v-model:current="candidatePage"
              :page-size="candidatePageSize"
              :show-size-changer="false"
              :total="candidateTotal"
              class="candidate-pagination"
              size="small"
            />
          </div>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="reportModalOpen"
      :confirm-loading="submitLoading"
      :title="`离散${opName}报工 - ${selectedSource?.pieceNo || ''}`"
      @ok="submitReport"
    >
      <Form layout="vertical">
        <FormItem label="结果">
          <RadioGroup
            v-model:value="reportForm.reportResult"
            :options="[
              { label: 'OK', value: 'OK' },
              { label: 'NG', value: 'NG' },
            ]"
            option-type="button"
          />
        </FormItem>
        <FormItem label="投入">
          <InputNumber v-model:value="reportForm.inputQty" :min="0" :precision="3" class="full-input" />
        </FormItem>
        <FormItem label="产出">
          <InputNumber v-model:value="reportForm.outputQty" :disabled="reportForm.reportResult === 'NG'" :min="0" :precision="3" class="full-input" />
        </FormItem>
        <FormItem label="损耗">
          <InputNumber v-model:value="reportForm.lossQty" :min="0" :precision="3" class="full-input" />
        </FormItem>
        <template v-if="opCode === 'WC-ADH2'">
          <FormItem label="实际尺寸">
            <Select
              v-model:value="reportForm.actualSizeRule"
              allow-clear
              :options="[
                { label: '775mm', value: '775mm' },
                { label: '740mm', value: '740mm' },
              ]"
            />
          </FormItem>
          <FormItem label="片号尾号">
            <Select
              v-model:value="reportForm.actualSizeSuffix"
              allow-clear
              :options="[
                { label: 'A', value: 'A' },
                { label: 'B', value: 'B' },
              ]"
            />
          </FormItem>
        </template>
        <FormItem label="不良代码">
          <Input v-model:value="reportForm.defectCode" allow-clear />
        </FormItem>
        <FormItem label="备注">
          <Textarea v-model:value="reportForm.remark" :rows="3" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.discrete-console {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar-band {
  align-items: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  padding: 12px;
}

.toolbar-main,
.candidate-filter {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-input {
  width: 220px;
}

.scan-input {
  width: 260px;
}

.date-input,
.short-input,
.status-select {
  width: 140px;
}

.plan-model-input {
  width: 180px;
}

.stat-strip,
.selected-plan,
.selection-text {
  color: #475569;
  display: flex;
  gap: 12px;
  white-space: nowrap;
}

.dispatch-band {
  min-height: 92px;
}

.dispatch-hint {
  color: #64748b;
  font-size: 13px;
}

.table-band {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
}

.source-toolbar {
  margin-top: 4px;
}

.source-table {
  min-height: 420px;
}

.create-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.create-plan-main-form {
  flex-shrink: 0;
}

.candidate-filter {
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
  padding-top: 12px;
}

.selected-piece-panel {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  flex-shrink: 0;
  min-height: 64px;
  padding: 7px 10px;
}

.selected-piece-title {
  align-items: center;
  color: #334155;
  display: flex;
  font-weight: 500;
  justify-content: space-between;
  min-height: 22px;
}

.selected-model-prefix {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  margin-left: auto;
  margin-right: 8px;
}

.selected-model-prefix.is-invalid {
  color: #dc2626;
}

.selected-piece-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 42px;
  overflow: auto;
}

.selected-piece-empty {
  color: #94a3b8;
  line-height: 28px;
}

.candidate-table-wrap {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.candidate-bottom {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  gap: 8px;
}

.create-plan-extra-form {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  flex-shrink: 0;
  padding: 8px 10px 0;
}

.create-plan-extra-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.candidate-pagination {
  display: flex;
  justify-content: flex-end;
  margin: 0;
}

.full-input {
  width: 100%;
}

.row-actions {
  display: flex;
  gap: 6px;
}

:deep(.selected-row td) {
  background: #eff6ff !important;
}

:global(.discrete-create-plan-modal .ant-modal-body) {
  flex: 1;
  height: auto;
  max-height: none;
  min-height: 0;
  overflow: hidden;
}

:global(.discrete-create-plan-modal .ant-modal) {
  max-width: calc(100vw - 48px);
  padding-bottom: 16px;
  top: 16px;
}

:global(.discrete-create-plan-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: min(800px, calc(100vh - 32px));
}

:global(.discrete-create-plan-modal .ant-modal-header),
:global(.discrete-create-plan-modal .ant-modal-footer) {
  flex-shrink: 0;
}

.candidate-table-wrap :deep(.candidate-table.ant-table-wrapper),
.candidate-table-wrap :deep(.candidate-table .ant-spin-nested-loading),
.candidate-table-wrap :deep(.candidate-table .ant-spin-container) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.candidate-table-wrap :deep(.candidate-table .ant-table) {
  flex: 1;
  min-height: 0;
}

.candidate-table-wrap :deep(.candidate-table .ant-table-container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.candidate-table-wrap :deep(.candidate-table .ant-table-header) {
  flex-shrink: 0;
}

.candidate-table-wrap :deep(.candidate-table .ant-table-body) {
  flex: 1;
  height: 100% !important;
  max-height: none !important;
  overflow-y: auto !important;
}
</style>
