<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Empty,
  Input,
  message,
  Modal,
  Select,
  Tag,
} from 'ant-design-vue';

import {
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';
import {
  getFaiDetail,
  getFaiStandardItemCandidates,
  getFaiPage,
  getFaiStandardCandidates,
  getGlueBoardFaiDetail,
  getGlueBoardFaiPage,
  getGlueBoardFaiStandardCandidates,
  getPendingFaiTasks,
  getPendingGlueBoardFaiTasks,
  recalculateFaiProgramEntry,
  recalculateGlueBoardFaiProgramEntry,
  resolveFaiDisplayBatchNo,
  saveFaiProgramEntry,
  saveGlueBoardFaiProgramEntry,
  selectFaiStandard,
  selectGlueBoardFaiStandard,
  submitFaiProgramEntry,
  submitGlueBoardFaiProgramEntry,
} from '#/api/mes/quality/fai';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';

import {
  formatInspectionQty,
  resolveFaiOperationLabel,
  sortFaiRecordsByStatus,
} from '../data';
import { useFaiProcessSelfCheckGuard } from '../shared/faiSelfCheck';
import QmsFaiItemValueInputModal from './qms-fai-item-value-input-modal.vue';
import QmsFaiTemplateImportExportModal from './qms-fai-quick-fill-modal.vue';
import QmsFaiScanEntryBox from './qms-fai-scan-entry-box.vue';
import QmsFaiTemplatePreviewModal from './qms-fai-template-preview-modal.vue';
import { confirmFaiSubmitOptions } from './use-fai-submit-confirm';

import '../shared/faiSelfCheckRuntime.css';

const props = withDefaults(
  defineProps<{
    apiMode?: 'FAI' | 'GLUE_BOARD';
    editableItemIds?: number[];
    embedded?: boolean;
    enableImport?: boolean;
    enableScan?: boolean;
    initialRecord?: MesFaiApi.FaiRecord;
    initialRecordId?: number;
    pageDescription?: string;
    pageTitle?: string;
    readonly?: boolean;
  }>(),
  {
    apiMode: 'FAI',
    enableImport: true,
    enableScan: true,
    embedded: false,
    initialRecord: undefined,
    initialRecordId: undefined,
    pageDescription:
      '按检验标准的位置模板填写数值或定性判定，系统重算平均值和标准差',
    pageTitle: '首件检验项明细概览',
  },
);
const emit = defineEmits(['backToLedger', 'submitted']);

type EditableSample = Record<string, any>;

interface DataRuleInputField {
  code: string;
  name: string;
  required?: boolean;
}

interface DataRuleResultField {
  code: string;
  name: string;
  formula: string;
  judgment?: boolean;
  precision?: number;
}

interface DataRuleConfig {
  inputFields: DataRuleInputField[];
  resultFields: DataRuleResultField[];
  judgmentMetric?: string;
}

const activeRecord = ref<MesFaiApi.FaiRecord | null>(null);
const activeItems = ref<MesFaiApi.FaiItem[]>([]);
const selectedItemId = ref<number | string>();
const modalItem = ref<MesFaiApi.FaiItem>();
const inputModalOpen = ref(false);
const templateImportExportModalOpen = ref(false);
const templatePreviewOpen = ref(false);
const expandedItemKeys = ref<string[]>([]);
const pendingList = ref<MesFaiApi.FaiRecord[]>([]);
const searchKeyword = ref('');
const loading = ref(false);
const saving = ref(false);
const standardLoading = ref(false);
const standardBinding = ref(false);
const standardSelectorOpen = ref(false);
const bindableStandards = ref<MesFaiApi.StandardCandidate[]>([]);
const selectedStandardId = ref<number>();
const standardBindReason = ref('');
const recheckItemSelectionOpen = ref(false);
const recheckItemCandidateLoading = ref(false);
const recheckItemCandidates = ref<MesFaiApi.FaiStandardItemCandidate[]>([]);
const selectedRecheckStandardItemIds = ref<number[]>([]);
const showTaskSidebar = false;
const showScanEntry = false;
const isGlueBoardMode = computed(() => props.apiMode === 'GLUE_BOARD');
const requiresRecheckItemSelection = computed(
  () => !isGlueBoardMode.value && activeRecord.value?.recheckFlag === true,
);
const recheckItemSelectionPending = computed(
  () =>
    requiresRecheckItemSelection.value &&
    !!activeRecord.value?.standardId &&
    activeItems.value.length === 0,
);
const {
  ensureReady: ensureFaiProcessSelfCheckReady,
  handleRuntimeSuccess: handleSelfCheckRuntimeSuccess,
  runtimeForm: selfCheckRuntimeForm,
  runtimeInitialParams: selfCheckRuntimeInitialParams,
  runtimeOpen: selfCheckRuntimeOpen,
} = useFaiProcessSelfCheckGuard();

function formatStandardMatchType(matchType?: string) {
  if (matchType === 'EXACT_MODEL') return '【精确型号】';
  if (matchType === 'FAMILY_MODEL') return '【系列通用】';
  return '';
}

const standardOptions = computed(() =>
  bindableStandards.value.map((standard) => ({
    label: [
      standard.recommended ? '【推荐】' : '',
      formatStandardMatchType(standard.matchType),
      standard.standardNo || `标准#${standard.id}`,
      standard.standardName,
      standard.version ? `V${standard.version}` : '',
      standard.processName || standard.processCode,
      standard.glueBoardModel ||
        standard.productModelName ||
        standard.productModelCode ||
        standard.materialName ||
        standard.materialCode,
      `${standard.itemCount ?? 0}项`,
      standard.matchReason,
    ]
      .filter(Boolean)
      .join(' / '),
    value: standard.id,
  })),
);
const standardPanelVisible = computed(
  () =>
    !!activeRecord.value &&
    (standardSelectorOpen.value ||
      !activeRecord.value.standardId ||
      activeRecord.value.standardContentChanged === true ||
      recheckItemSelectionPending.value),
);

interface LoadRecordOptions {
  skipEmptyDetailRefetch?: boolean;
}

onMounted(async () => {
  if (!props.embedded) await refreshTaskList();
  if (props.initialRecord) {
    await loadRecord(props.initialRecord);
  } else if (props.initialRecordId) {
    await loadRecordById(props.initialRecordId);
  }
});

watch(
  () => props.initialRecord,
  async (record) => {
    if (record) await loadRecord(record);
  },
);

watch(
  () => props.initialRecordId,
  async (id) => {
    if (id && id !== activeRecord.value?.id && !props.initialRecord) {
      await loadRecordById(id);
    }
  },
);

const filteredTaskList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return pendingList.value;
  return pendingList.value.filter((item) =>
    [
      item.faiNo,
      item.workOrderNo,
      item.materialCode,
      item.productModel,
      item.operationName,
      resolveFaiDisplayBatchNo(item),
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  );
});

const displayItems = computed(() => {
  const groupedCodes = new Set<string>();
  return activeItems.value.filter((item) => {
    const code = entryGroupCode(item);
    if (!code) return true;
    if (groupedCodes.has(code)) return false;
    groupedCodes.add(code);
    return true;
  });
});

const modalItemIndex = computed(() => {
  if (!modalItem.value) return 0;
  const currentKey = resolveItemKey(modalItem.value);
  const index = displayItems.value.findIndex((item) =>
    isSameItemKey(resolveItemKey(item), currentKey),
  );
  return index === -1 ? 0 : index + 1;
});

async function refreshTaskList() {
  const [pending, page] = await Promise.all([
    isGlueBoardMode.value
      ? getPendingGlueBoardFaiTasks()
      : getPendingFaiTasks(),
    isGlueBoardMode.value
      ? getGlueBoardFaiPage({ pageNo: 1, pageSize: 20 } as MesFaiApi.FaiPageReq)
      : getFaiPage({ pageNo: 1, pageSize: 20 } as MesFaiApi.FaiPageReq),
  ]);
  const map = new Map<string, MesFaiApi.FaiRecord>();
  [...pending, ...(page.list || [])].forEach((item) =>
    map.set(item.faiNo, item),
  );
  pendingList.value = sortSidebarTasks([...map.values()]);
}

async function handleRefresh() {
  if (!props.embedded) {
    await refreshTaskList();
  }
  if (activeRecord.value?.id) {
    await loadRecordById(activeRecord.value.id);
  }
}

function sortSidebarTasks(records: MesFaiApi.FaiRecord[]) {
  return sortFaiRecordsByStatus(records);
}

function syncSidebarTask(record: MesFaiApi.FaiRecord) {
  const next = new Map(pendingList.value.map((item) => [item.faiNo, item]));
  next.set(record.faiNo, record);
  pendingList.value = sortSidebarTasks([...next.values()]);
}

async function loadRecordById(id: number) {
  loading.value = true;
  try {
    const detail = await (isGlueBoardMode.value
      ? getGlueBoardFaiDetail(id)
      : getFaiDetail(id));
    await loadRecord(detail, { skipEmptyDetailRefetch: true });
  } finally {
    loading.value = false;
  }
}

async function handleSelectTask(row: MesFaiApi.FaiRecord) {
  if (!row.id) return;
  await loadRecord(row);
}

async function handleScanResolved(resp: MesFaiApi.FaiScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (!(await ensureSelfCheckBeforeEntry())) return;
  await loadRecord(resp.record);
  syncSidebarTask(resp.record);
  if (resp.openTarget === 'ITEM_MODAL' && resp.matchedFaiItemId) {
    const target = activeItems.value.find(
      (item) => item.id === resp.matchedFaiItemId,
    );
    if (target) {
      await handleOpenItemModal(target);
    }
  }
}

async function loadRecord(
  detail: MesFaiApi.FaiRecord,
  options: LoadRecordOptions = {},
) {
  activeRecord.value = { ...detail };
  let items = detail.items || [];
  if (items.length === 0 && detail.id && !options.skipEmptyDetailRefetch) {
    loading.value = true;
    try {
      const fullDetail = await (isGlueBoardMode.value
        ? getGlueBoardFaiDetail(detail.id)
        : getFaiDetail(detail.id));
      await loadRecord(fullDetail, {
        ...options,
        skipEmptyDetailRefetch: true,
      });
      return;
    } finally {
      loading.value = false;
    }
  }
  items = filterRecheckItems(detail, items);
  activeItems.value = items.map((item) => {
    const normalized = { ...item };
    normalized.qaValues = buildEditableValues(normalized);
    return normalized;
  });
  activeRecord.value.items = activeItems.value;
  selectedItemId.value = activeItems.value[0]
    ? resolveItemKey(activeItems.value[0])
    : undefined;
  expandedItemKeys.value = [];
  standardSelectorOpen.value =
    !detail.standardId ||
    detail.standardContentChanged === true ||
    recheckItemSelectionPending.value;
  if (detail.standardSwitchAllowed && standardSelectorOpen.value) {
    await loadBindableStandards(detail);
  } else {
    bindableStandards.value = [];
    selectedStandardId.value = undefined;
    standardBindReason.value = '';
  }
}

function filterRecheckItems(
  detail: MesFaiApi.FaiRecord,
  items: MesFaiApi.FaiItem[],
) {
  if (!detail.recheckFlag) return items;
  const recheckItems = items.filter(
    (item) =>
      item.recheckItemFlag ||
      item.samples?.some((sample) => sample.recheckItemFlag),
  );
  return recheckItems.length > 0 ? recheckItems : items;
}

async function loadBindableStandards(record: MesFaiApi.FaiRecord) {
  if (!record.id) return;
  standardLoading.value = true;
  selectedStandardId.value = record.standardId;
  standardBindReason.value = '';
  try {
    bindableStandards.value = await (isGlueBoardMode.value
      ? getGlueBoardFaiStandardCandidates(record.id)
      : getFaiStandardCandidates(record.id));
    if (
      selectedStandardId.value &&
      !bindableStandards.value.some(
        (item) => item.id === selectedStandardId.value,
      )
    ) {
      selectedStandardId.value = undefined;
    }
  } catch {
    bindableStandards.value = [];
    message.warning('检验任务已加载，但候选检验标准加载失败，请稍后刷新重试');
  } finally {
    standardLoading.value = false;
  }
}

async function handleOpenStandardSelector() {
  if (!activeRecord.value?.standardSwitchAllowed) return;
  standardSelectorOpen.value = true;
  await loadBindableStandards(activeRecord.value);
}

async function handleBindStandard() {
  if (!activeRecord.value?.id || !selectedStandardId.value) {
    message.warning('请选择检验标准');
    return;
  }
  if (requiresRecheckItemSelection.value) {
    await handleOpenRecheckItemSelector();
    return;
  }
  await applyStandardSelection();
}

async function applyStandardSelection(selectedStandardItemIds?: number[]) {
  if (!activeRecord.value?.id || !selectedStandardId.value) return;
  const hasExistingData =
    !!activeRecord.value.standardId || activeItems.value.length > 0;
  const inspectionId = activeRecord.value.id;
  const standardId = selectedStandardId.value;
  const sameStandard =
    activeRecord.value.standardId === selectedStandardId.value &&
    !activeRecord.value.standardContentChanged;
  const execute = async () => {
    standardBinding.value = true;
    try {
      const selectStandard = isGlueBoardMode.value
        ? selectGlueBoardFaiStandard
        : selectFaiStandard;
      const record = await selectStandard({
        id: inspectionId,
        standardId,
        reason: standardBindReason.value.trim() || undefined,
        selectedStandardItemIds,
      });
      standardSelectorOpen.value = false;
      await loadRecord(record, { skipEmptyDetailRefetch: true });
      syncSidebarTask(record);
      message.success(
        selectedStandardItemIds
          ? '已按所选标准项目生成复检明细'
          : sameStandard
          ? '已确认当前检验标准，模板和录入数据保持不变'
          : '已按所选标准重新生成本次检验明细',
      );
    } finally {
      standardBinding.value = false;
    }
  };
  if (!hasExistingData || sameStandard) {
    await execute();
    return;
  }
  Modal.confirm({
    title: '确认重新加载检验标准？',
    content:
      '切换至不同标准会清空当前检验明细、填值、附件、异常和审核过程数据；已关联的原始记录表模板和历史导入批次会保留，旧数据将保存到标准切换留痕中。',
    okText: '确认重新加载',
    cancelText: '取消',
    onOk: execute,
  });
}

async function handleOpenRecheckItemSelector() {
  if (activeRecord.value?.standardId && !selectedStandardId.value) {
    selectedStandardId.value = activeRecord.value.standardId;
  }
  if (!activeRecord.value?.id || !selectedStandardId.value) {
    message.warning('请先选择检验标准');
    return;
  }
  recheckItemCandidateLoading.value = true;
  selectedRecheckStandardItemIds.value = [];
  try {
    recheckItemCandidates.value = await getFaiStandardItemCandidates(
      activeRecord.value.id,
      selectedStandardId.value,
    );
    recheckItemSelectionOpen.value = true;
  } finally {
    recheckItemCandidateLoading.value = false;
  }
}

function isRecheckStandardItemSelected(standardItemId: number) {
  return selectedRecheckStandardItemIds.value.includes(standardItemId);
}

function toggleRecheckStandardItem(standardItemId: number, checked: boolean) {
  selectedRecheckStandardItemIds.value = checked
    ? [...new Set([...selectedRecheckStandardItemIds.value, standardItemId])]
    : selectedRecheckStandardItemIds.value.filter((id) => id !== standardItemId);
}

async function confirmRecheckItemSelection() {
  if (selectedRecheckStandardItemIds.value.length === 0) {
    message.warning('请至少选择一个复检检验项目');
    return;
  }
  recheckItemSelectionOpen.value = false;
  await applyStandardSelection(selectedRecheckStandardItemIds.value);
}

function resolveItemKey(item: MesFaiApi.FaiItem) {
  return item.id ?? item.standardItemId ?? item.inspectionItem;
}

function isSameItemKey(left?: number | string, right?: number | string) {
  return (
    left !== undefined && right !== undefined && String(left) === String(right)
  );
}

function resolveItemTreeKey(item: MesFaiApi.FaiItem) {
  return String(resolveItemKey(item) ?? metricName(item));
}

function isItemExpanded(item: MesFaiApi.FaiItem) {
  return expandedItemKeys.value.includes(resolveItemTreeKey(item));
}

function toggleItemTree(item: MesFaiApi.FaiItem) {
  const key = resolveItemTreeKey(item);
  expandedItemKeys.value = isItemExpanded(item)
    ? expandedItemKeys.value.filter((itemKey) => itemKey !== key)
    : [...expandedItemKeys.value, key];
}

function resolveDataRule(item: MesFaiApi.FaiItem): DataRuleConfig | undefined {
  const params = parseEntryRuleParams(item.templateParams);
  const dataRule = params.dataRule || params;
  const inputFields = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields
    : [];
  const resultFields = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields
    : [];
  if (inputFields.length === 0 || resultFields.length === 0) return undefined;
  return {
    inputFields: inputFields.map((field: any) => ({
      code: field.code,
      name: field.name || field.code,
      required: field.required !== false,
    })),
    resultFields: resultFields.map((field: any) => ({
      code: field.code,
      name: field.name || field.code,
      formula: field.formula || '',
      judgment: !!field.judgment,
      precision: Number(field.precision ?? 3),
    })),
    judgmentMetric:
      dataRule.judgmentMetric ||
      resultFields.find((field: any) => field.judgment)?.code,
  };
}

function isDataRuleItem(item: MesFaiApi.FaiItem) {
  return item.itemType === 'QUANTITATIVE' && !!resolveDataRule(item);
}

function entryGroupParams(item: MesFaiApi.FaiItem) {
  return parseEntryRuleParams(item.templateParams);
}

function entryGroupCode(item: MesFaiApi.FaiItem) {
  const params = entryGroupParams(item);
  return typeof params.entryGroupCode === 'string'
    ? params.entryGroupCode.trim()
    : '';
}

function entryGroupName(item: MesFaiApi.FaiItem) {
  const params = entryGroupParams(item);
  return typeof params.entryGroupName === 'string'
    ? params.entryGroupName.trim()
    : '';
}

function entryGroupItems(item: MesFaiApi.FaiItem) {
  const code = entryGroupCode(item);
  if (!code) return [item];
  return activeItems.value.filter(
    (activeItem) => entryGroupCode(activeItem) === code,
  );
}

function isEntryGroupItem(item: MesFaiApi.FaiItem) {
  return !!entryGroupCode(item);
}

function metricDisplayName(item: MesFaiApi.FaiItem) {
  const text = [
    item.inspectionItem,
    item.sheetMetricName,
    item.valueTemplateName,
    item.standardDesc,
  ]
    .filter(Boolean)
    .join(' ');
  const dataRule = resolveDataRule(item);
  const judgmentMetric = String(
    item.judgmentMetric || dataRule?.judgmentMetric || '',
  ).toLowerCase();
  if (
    text.includes('弹性') ||
    text.includes('回弹') ||
    judgmentMetric.includes('elastic')
  ) {
    return '压缩弹性率';
  }
  if (text.includes('压缩') || judgmentMetric.includes('compression')) {
    return '压缩率';
  }
  return metricName(item) || '-';
}

function groupMetricRows(item: MesFaiApi.FaiItem) {
  return entryGroupItems(item).map((groupItem) => ({
    key: resolveItemKey(groupItem) ?? metricDisplayName(groupItem),
    name: metricDisplayName(groupItem),
    avgLimitText: controlText(
      groupItem.avgMinLimit,
      groupItem.avgMaxLimit,
      valueUnit(groupItem),
    ),
    stdLimitText: controlText(
      groupItem.stdMinLimit,
      groupItem.stdMaxLimit,
      valueUnit(groupItem),
    ),
    avgValue: formatNumber(groupItem.calculatedAvg ?? groupItem.qaAvg),
    stdValue: formatNumber(groupItem.calculatedStd),
  }));
}

function displayMetricName(item: MesFaiApi.FaiItem) {
  return entryGroupName(item) || metricName(item);
}

function entryInputFields(item: MesFaiApi.FaiItem): DataRuleInputField[] {
  const dataRule = resolveDataRule(item);
  if (dataRule) return dataRule.inputFields;
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      { code: 't1Mm', name: 'T1', required: true },
      { code: 't2Mm', name: 'T2', required: true },
      { code: 't3Mm', name: 'T3', required: true },
    ];
  }
  return [];
}

function buildEditableValues(item: MesFaiApi.FaiItem): EditableSample[] {
  const source = Array.isArray(item.qaValues) ? item.qaValues : [];
  const sourceRows = source.map((value, index) =>
    value && typeof value === 'object'
      ? { ...value }
      : { value, sampleSeq: index + 1 },
  );
  const expectedCount = resolveEntryRuleExpectedSampleCount(item);
  const positions = resolveEntryRulePositions(item);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  const sourceByPositionGroup = new Map<string, EditableSample>();
  const sourceBySeq = new Map<number, EditableSample>();
  sourceRows.forEach((row) => {
    const seq = Number(row.sampleSeq);
    if (Number.isInteger(seq) && seq > 0) sourceBySeq.set(seq, row);
    const groupNo = Number(row.sampleGroupNo);
    const positionKey = normalizeEditablePositionKey(
      row.samplePosition || row.samplePositionCode,
    );
    if (positionKey && Number.isInteger(groupNo) && groupNo > 0) {
      sourceByPositionGroup.set(`${positionKey}|${groupNo}`, row);
    }
  });
  return Array.from({ length: expectedCount }).map((_, index) => {
    const position =
      positions[Math.floor(index / repeatCount)] || positions[index];
    const groupNo = (index % repeatCount) + 1;
    const positionName = position?.name || position?.code || `点位${index + 1}`;
    const positionKeys = [
      normalizeEditablePositionKey(position?.name),
      normalizeEditablePositionKey(position?.code),
    ].filter(Boolean);
    let sourceRow = positionKeys
      .map((key) => sourceByPositionGroup.get(`${key}|${groupNo}`))
      .find(Boolean);
    const seqRow = sourceBySeq.get(index + 1);
    if (
      !sourceRow &&
      seqRow &&
      (positionKeys.length === 0 ||
        !normalizeEditablePositionKey(
          seqRow.samplePosition || seqRow.samplePositionCode,
        ) ||
        positionKeys.includes(
          normalizeEditablePositionKey(
            seqRow.samplePosition || seqRow.samplePositionCode,
          ),
        ))
    ) {
      sourceRow = seqRow;
    }
    if (!sourceRow && positions.length === 0) {
      sourceRow = sourceRows[index];
    }
    const row: EditableSample = sourceRow ? { ...sourceRow } : {};
    row.samplePosition = positionName;
    row.samplePositionCode = position?.code || row.samplePositionCode;
    row.sampleGroupNo = groupNo;
    row.sampleSeq = index + 1;
    return row;
  });
}

function normalizeEditablePositionKey(value?: unknown) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase();
}

function ensureEditableValues(item: MesFaiApi.FaiItem) {
  if (
    !Array.isArray(item.qaValues) ||
    item.qaValues.length !== resolveEntryRuleExpectedSampleCount(item)
  ) {
    item.qaValues = buildEditableValues(item);
  }
}

function isReadOnly() {
  return (
    props.readonly === true ||
    activeRecord.value?.status === 'WAITING_QA' ||
    activeRecord.value?.status === 'COMPLETED' ||
    activeRecord.value?.status === 'CANCELED' ||
    activeRecord.value?.status === 'CANCELLED' ||
    activeRecord.value?.sheetLocked === true
  );
}

function isItemReadOnly(item?: MesFaiApi.FaiItem) {
  if (isReadOnly() || !item) return true;
  if (props.editableItemIds === undefined) return false;
  return (
    item.id === undefined || !props.editableItemIds.includes(Number(item.id))
  );
}

function hasActiveItems() {
  return activeItems.value.length > 0;
}

function canMutateItems() {
  return (
    !!activeRecord.value &&
    hasActiveItems() &&
    !isReadOnly() &&
    activeItems.value.some((item) => !isItemReadOnly(item))
  );
}

function processLabel(record?: MesFaiApi.FaiRecord | null) {
  return resolveFaiOperationLabel(record);
}

function statusLabel(status?: string) {
  if (status === 'PENDING') return '待检验';
  if (status === 'INSPECTING') return '检验中';
  if (status === 'WAITING_QA') return '待审核';
  if (status === 'COMPLETED') return '已完成';
  if (status === 'REJECTED') return '已驳回';
  if (status === 'SUSPENDED') return '已挂起';
  if (status === 'REWORKING') return '调机中';
  return status || '-';
}

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REJECTED' || status === 'REWORKING') return 'error';
  if (status === 'WAITING_QA') return 'purple';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'default';
}

function resultLabel(result?: string) {
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  if (result === '-' || result === 'PENDING' || !result) return '待判定';
  return result;
}

function itemReviewLabel(item: MesFaiApi.FaiItem) {
  const groups = item.auditGroups || [];
  if (groups.some((group) => group.itemRecheckStatus === 'WAIT_RECHECK')) {
    return '待复检';
  }
  if (groups.some((group) => group.itemRecheckStatus === 'WAIT_AUDIT')) {
    return '复检待审';
  }
  if (groups.some((group) => group.itemRecheckStatus === 'CONFIRMED')) {
    return '复检已确认';
  }
  if (groups.some((group) => group.auditResult === 'REJECT_RECHECK')) {
    return '驳回重检';
  }
  if (
    groups.length > 0 &&
    groups.every((group) => group.auditResult === 'CONFIRM')
  ) {
    return '审核确认';
  }
  return resultLabel(item.qaResult);
}

function itemReviewColor(item: MesFaiApi.FaiItem) {
  const groups = item.auditGroups || [];
  if (
    groups.some(
      (group) =>
        group.itemRecheckStatus === 'WAIT_RECHECK' ||
        group.auditResult === 'REJECT_RECHECK',
    )
  ) {
    return 'warning';
  }
  if (groups.some((group) => group.itemRecheckStatus === 'WAIT_AUDIT')) {
    return 'purple';
  }
  if (
    groups.some(
      (group) =>
        group.itemRecheckStatus === 'CONFIRMED' ||
        group.auditResult === 'CONFIRM',
    )
  ) {
    return 'success';
  }
  return resultColor(item.qaResult);
}

function metricName(item: MesFaiApi.FaiItem) {
  if (item.sheetMetricName) return item.sheetMetricName;
  const parts = item.inspectionItem?.split('-') || [];
  return parts.length > 1 ? parts[parts.length - 1] : item.inspectionItem;
}

function controlText(min?: number, max?: number, unit = '') {
  if (min === undefined && max === undefined) return '待定';
  return `${min ?? '-'}~${max ?? '-'}${unit}`;
}

function valueUnit(item: MesFaiApi.FaiItem) {
  if (item.unit) return item.unit;
  const desc = item.standardDesc || '';
  if (desc.includes('g/cm')) return 'g/cm2';
  if (desc.includes('%')) return '%';
  if (desc.includes('HA')) return 'HA';
  if (desc.includes('um')) return 'um';
  if (desc.includes('mm')) return 'mm';
  return '';
}

function formatNumber(value?: number, digits = 3) {
  if (value === undefined || value === null || Number.isNaN(Number(value)))
    return '-';
  return Number(value).toFixed(digits);
}

function formatDateTimeText(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}

function countFilled(item: MesFaiApi.FaiItem) {
  ensureEditableValues(item);
  return item.qaValues!.filter((row) => isSampleFilled(item, row)).length;
}

function canSubmitConfirm() {
  return canMutateItems();
}

function buildFillTreeRows(item: MesFaiApi.FaiItem) {
  ensureEditableValues(item);
  const itemRepeatCount = resolveEntryRuleRepeatCount(item);
  return resolveEntryRulePositions(item).map((position, positionIndex) => ({
    ...position,
    samples: Array.from({ length: itemRepeatCount }).map((_, offset) => {
      const sample =
        item.qaValues?.[positionIndex * itemRepeatCount + offset] || {};
      return {
        filled: isSampleFilled(item, sample),
        groupNo: offset + 1,
        result: sampleResultLabel(sample),
        text: sampleValueText(item, sample),
      };
    }),
  }));
}

function sampleResultLabel(sample: EditableSample) {
  return resultLabel(sample.sampleResult || sample.result || sample.value);
}

function sampleValueText(item: MesFaiApi.FaiItem, sample: EditableSample) {
  if (!isSampleFilled(item, sample)) return '未填写';
  if (item.itemType === 'QUALITATIVE') {
    return resultLabel(sample.value || sample.qualitativeValue);
  }
  const dataRule = resolveDataRule(item);
  if (dataRule) {
    return dataRule.inputFields
      .map((field) => `${field.name} ${formatNumber(sample[field.code])}`)
      .join(' / ');
  }
  if (item.valueTemplate === 'DENSITY_CALC') {
    return `厚度 ${formatNumber(sample.thicknessMm)} / 重量 ${formatNumber(
      sample.weightG,
    )} / 密度 ${formatNumber(densityPreview(sample))}`;
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return `T1 ${formatNumber(sample.t1Mm)} / T2 ${formatNumber(
      sample.t2Mm,
    )} / T3 ${formatNumber(sample.t3Mm)} / 压缩率 ${formatNumber(
      compressionRatePreview(sample),
    )}% / 弹性率 ${formatNumber(compressionElasticityPreview(sample))}%`;
  }
  return `${formatNumber(sample.value ?? sample.resultValue)}${valueUnit(item)}`;
}

function positionSummary(item: MesFaiApi.FaiItem) {
  return `${resolveEntryRulePositions(item)
    .map((position) => position.name)
    .join(' / ')}，每位置 ${resolveEntryRuleRepeatCount(item)} 组`;
}

function isSampleFilled(item: MesFaiApi.FaiItem, row: EditableSample) {
  if (item.itemType === 'QUALITATIVE') {
    return row.value === 'OK' || (row.value === 'NG' && !!row.remark);
  }
  const dataRule = resolveDataRule(item);
  if (dataRule) {
    return dataRule.inputFields
      .filter((field) => field.required !== false)
      .every(
        (field) =>
          row[field.code] !== undefined &&
          row[field.code] !== null &&
          row[field.code] !== '',
      );
  }
  if (item.valueTemplate === 'DENSITY_CALC') {
    return (
      row.thicknessMm !== undefined &&
      row.thicknessMm !== null &&
      row.weightG !== undefined &&
      row.weightG !== null
    );
  }
  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return (
      row.t1Mm !== undefined &&
      row.t1Mm !== null &&
      row.t2Mm !== undefined &&
      row.t2Mm !== null &&
      row.t3Mm !== undefined &&
      row.t3Mm !== null
    );
  }
  return row.value !== undefined && row.value !== null && row.value !== '';
}

function densityPreview(row: EditableSample) {
  const thickness = Number(row.thicknessMm);
  const weight = Number(row.weightG);
  if (
    !Number.isFinite(thickness) ||
    !Number.isFinite(weight) ||
    thickness === 0
  )
    return row.densityValue;
  return weight / (((thickness / 10) * 3.14 * 39 * 39) / 4);
}

function compressionRatePreview(row: EditableSample) {
  const t1 = Number(row.t1Mm);
  const t2 = Number(row.t2Mm);
  if (!Number.isFinite(t1) || !Number.isFinite(t2) || t1 === 0)
    return row.compressionRate;
  return ((t1 - t2) / t1) * 100;
}

function compressionElasticityPreview(row: EditableSample) {
  const t1 = Number(row.t1Mm);
  const t2 = Number(row.t2Mm);
  const t3 = Number(row.t3Mm);
  if (
    !Number.isFinite(t1) ||
    !Number.isFinite(t2) ||
    !Number.isFinite(t3) ||
    t1 === t2
  )
    return row.compressionElasticityRate;
  return ((t3 - t2) / (t1 - t2)) * 100;
}

function evaluateFormula(formula: string, values: Record<string, any>) {
  if (!/^[\w\s+\-*/().,]+$/.test(formula)) return undefined;
  const identifiers = formula.match(/[A-Z_]\w*/gi) || [];
  const allowedFunctions = new Set(['avg', 'round', 'std']);
  const scope: Record<string, ((...args: number[]) => number) | number> = {
    avg: (...args: number[]) =>
      args.reduce((total, value) => total + value, 0) / args.length,
    round: (value: number, precision = 2) =>
      Number(value.toFixed(Math.max(0, precision))),
    std: (...args: number[]) => {
      const avg = args.reduce((total, value) => total + value, 0) / args.length;
      const variance =
        args.reduce((total, value) => total + (value - avg) ** 2, 0) /
        args.length;
      return Math.sqrt(variance);
    },
  };
  for (const identifier of identifiers) {
    if (allowedFunctions.has(identifier)) continue;
    const value = Number(values[identifier]);
    if (!Number.isFinite(value)) return undefined;
    scope[identifier] = value;
  }
  try {
    const names = Object.keys(scope);
    const args = Object.values(scope);
    // eslint-disable-next-line no-new-func -- 公式模板只允许白名单字符和标识符，保留既有表达式计算能力。
    const result = new Function(...names, `"use strict"; return (${formula});`)(
      ...args,
    );
    return Number.isFinite(Number(result)) ? Number(result) : undefined;
  } catch {
    return undefined;
  }
}

function applyDataRuleResults(item: MesFaiApi.FaiItem, row: EditableSample) {
  const dataRule = resolveDataRule(item);
  if (!dataRule) return row;
  const values: Record<string, any> = { ...row };
  for (const result of dataRule.resultFields) {
    const value = evaluateFormula(result.formula, values);
    if (value === undefined) continue;
    // 压缩性能按原始计算精度传给后端统计；result.precision 仍只负责页面展示。
    const precision =
      item.valueTemplate === 'COMPRESSION_CALC'
        ? 6
        : Number(result.precision ?? 3);
    values[result.code] = Number(value.toFixed(Math.max(0, precision)));
    row[result.code] = values[result.code];
  }
  const metric =
    dataRule.judgmentMetric ||
    dataRule.resultFields.find((result) => result.judgment)?.code;
  row.resultValue = metric ? row[metric] : undefined;
  row.measuredValue = row.resultValue;
  row.rawValuesJson = JSON.stringify(
    Object.fromEntries(
      dataRule.inputFields
        .map((field) => [field.code, row[field.code]])
        .filter(([, value]) => value !== undefined && value !== null),
    ),
  );
  return row;
}

function prepareItemValues(item: MesFaiApi.FaiItem) {
  ensureEditableValues(item);
  item.qaValues = item.qaValues!.map((row) => {
    const next = { ...row };
    if (item.itemType === 'QUALITATIVE') {
      next.qualitativeValue = next.value;
      next.sampleResult = next.value || 'PENDING';
    } else if (isDataRuleItem(item)) {
      applyDataRuleResults(item, next);
    } else if (item.valueTemplate === 'DENSITY_CALC') {
      const density = densityPreview(next);
      next.densityValue = density;
      next.resultValue = density;
    } else if (item.valueTemplate === 'COMPRESSION_CALC') {
      const rate = compressionRatePreview(next);
      const elasticity = compressionElasticityPreview(next);
      next.compressionRate = rate;
      next.compressionElasticityRate = elasticity;
      next.resultValue =
        item.judgmentMetric === 'COMPRESSION_ELASTICITY_RATE'
          ? elasticity
          : rate;
    } else {
      next.resultValue = next.value;
    }
    return next;
  });
  return item;
}

function prepareItemDraft(item: MesFaiApi.FaiItem) {
  const prepared = { ...prepareItemValues(item) };
  if (countFilled(prepared) < resolveEntryRuleSampleSize(prepared)) {
    prepared.qaValues = (prepared.qaValues || []).filter((row) =>
      isSampleFilled(prepared, row),
    );
  }
  return prepared;
}

function copyEntryGroupInputs(
  source: MesFaiApi.FaiItem,
  target: MesFaiApi.FaiItem,
) {
  const inputFields = entryInputFields(target);
  const fallbackFields =
    inputFields.length > 0 ? inputFields : entryInputFields(source);
  const sourceRows = source.qaValues || [];
  const qaValues = buildEditableValues(target).map((row, index) => {
    const sourceRow = sourceRows[index] || {};
    const nextRow = { ...row };
    for (const field of fallbackFields) {
      nextRow[field.code] = sourceRow[field.code];
    }
    return nextRow;
  });
  return {
    ...target,
    qaValues,
  };
}

function prepareEntryGroupItemDrafts(item: MesFaiApi.FaiItem) {
  const groupItems = entryGroupItems(item);
  if (groupItems.length <= 1) return [prepareItemDraft(item)];

  const sourceDraft = prepareItemDraft(item);
  const currentKey = resolveItemKey(item);
  return groupItems.map((groupItem) => {
    if (isSameItemKey(resolveItemKey(groupItem), currentKey)) {
      return sourceDraft;
    }
    return prepareItemDraft(copyEntryGroupInputs(sourceDraft, groupItem));
  });
}

function prepareMergedItemDrafts(item: MesFaiApi.FaiItem) {
  const currentKey = resolveItemKey(item);
  const currentDrafts = prepareEntryGroupItemDrafts(item);
  const currentDraftMap = new Map(
    currentDrafts.map((draft) => [String(resolveItemKey(draft)), draft]),
  );
  return activeItems.value
    .map((activeItem) => {
      const draft = currentDraftMap.get(String(resolveItemKey(activeItem)));
      if (draft) return draft;
      return resolveItemKey(activeItem) === currentKey
        ? currentDrafts[0]
        : prepareItemDraft(activeItem);
    })
    .filter(
      (draft) =>
        currentDraftMap.has(String(resolveItemKey(draft))) ||
        resolveItemKey(draft) === currentKey ||
        countFilled(draft) > 0,
    );
}

function recordPayload(items: MesFaiApi.FaiItem[]) {
  return {
    ...activeRecord.value!,
    items,
  };
}

async function ensureSelfCheckBeforeEntry() {
  if (isGlueBoardMode.value) return true;
  return await ensureFaiProcessSelfCheckReady();
}

async function handleOpenItemModal(item: MesFaiApi.FaiItem) {
  if (!(await ensureSelfCheckBeforeEntry())) return;
  selectedItemId.value = resolveItemKey(item);
  modalItem.value = item;
  inputModalOpen.value = true;
}

function findItemByKey(key?: number | string) {
  return activeItems.value.find((item) =>
    isSameItemKey(resolveItemKey(item), key),
  );
}

function resolveNextItemKey(item: MesFaiApi.FaiItem) {
  const currentKey = resolveItemKey(item);
  const currentIndex = displayItems.value.findIndex((activeItem) =>
    isSameItemKey(resolveItemKey(activeItem), currentKey),
  );
  const nextItem =
    currentIndex === -1 ? undefined : displayItems.value[currentIndex + 1];
  return nextItem ? resolveItemKey(nextItem) : undefined;
}

async function handleSaveModalItem(
  item: MesFaiApi.FaiItem,
  closeAfterSave = false,
) {
  if (!activeRecord.value) return;
  if (isItemReadOnly(item)) {
    message.warning('当前检验项不在本次质量任务范围内，只能查看');
    return;
  }
  if (!(await ensureSelfCheckBeforeEntry())) return;
  const currentKey = resolveItemKey(item);
  const nextKey = closeAfterSave ? resolveNextItemKey(item) : undefined;
  saving.value = true;
  try {
    const saveProgramEntry = isGlueBoardMode.value
      ? saveGlueBoardFaiProgramEntry
      : saveFaiProgramEntry;
    const resp = await saveProgramEntry(
      recordPayload(prepareEntryGroupItemDrafts(item)),
    );
    await loadRecord(resp);
    if (closeAfterSave) {
      const nextItem = findItemByKey(nextKey);
      if (nextItem) {
        await handleOpenItemModal(nextItem);
        message.success('当前检验项已保存，已自动跳转下一项');
      } else {
        const currentItem = findItemByKey(currentKey);
        if (currentItem) selectedItemId.value = resolveItemKey(currentItem);
        inputModalOpen.value = false;
        message.success('当前检验项已保存并关闭，已到最后一项');
      }
    } else {
      const currentItem = findItemByKey(currentKey);
      if (currentItem) {
        selectedItemId.value = resolveItemKey(currentItem);
        modalItem.value = currentItem;
      }
      message.success('当前检验项已保存');
    }
  } finally {
    saving.value = false;
  }
}

async function handleRecalculateModalItem(item: MesFaiApi.FaiItem) {
  if (!activeRecord.value || !item) return;
  if (isItemReadOnly(item)) {
    message.warning('当前检验项不在本次质量任务范围内，只能查看');
    return;
  }
  if (!(await ensureSelfCheckBeforeEntry())) return;
  saving.value = true;
  try {
    const recalculateProgramEntry = isGlueBoardMode.value
      ? recalculateGlueBoardFaiProgramEntry
      : recalculateFaiProgramEntry;
    const resp = await recalculateProgramEntry(
      recordPayload(prepareMergedItemDrafts(item)),
    );
    await loadRecord(resp);
    message.success('当前检验项已重算');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  if (!activeRecord.value) return;
  if (!(await ensureSelfCheckBeforeEntry())) return;
  const incomplete = activeItems.value.find(
    (item) => countFilled(item) < resolveEntryRuleSampleSize(item),
  );
  if (incomplete) {
    message.warning(
      `检验项“${incomplete.inspectionItem}”尚未填满 ${resolveEntryRuleSampleSize(incomplete)} 组数据`,
    );
    selectedItemId.value = resolveItemKey(incomplete);
    return;
  }
  const submitOptions = isGlueBoardMode.value
    ? undefined
    : await confirmFaiSubmitOptions(
        '请选择本次首件检验是否需要留样，并填写实际检验时间。取消后不会提交检测结果。',
      );
  if (!isGlueBoardMode.value && !submitOptions) return;
  saving.value = true;
  try {
    const submitProgramEntry = isGlueBoardMode.value
      ? submitGlueBoardFaiProgramEntry
      : submitFaiProgramEntry;
    const payload = recordPayload(
      activeItems.value.map((item) => prepareItemValues(item)),
    );
    if (submitOptions) {
      payload.inspectionTime = submitOptions.inspectionTime;
      payload.retentionStatus = submitOptions.retentionStatus;
    }
    const resp = await submitProgramEntry(payload);
    await loadRecord(resp);
    syncSidebarTask(resp);
    message.success('首件检验数据已提交，等待质检主管审核');
    emit('submitted', resp);
  } finally {
    saving.value = false;
  }
}

async function handleTemplateImportApply(items: MesFaiApi.FaiItem[]) {
  if (!activeRecord.value) return;
  if (!(await ensureSelfCheckBeforeEntry())) return;
  saving.value = true;
  try {
    const saveProgramEntry = isGlueBoardMode.value
      ? saveGlueBoardFaiProgramEntry
      : saveFaiProgramEntry;
    const resp = await saveProgramEntry(
      recordPayload(items.map((item) => prepareItemValues(item))),
    );
    await loadRecord(resp);
    syncSidebarTask(resp);
    templateImportExportModalOpen.value = false;
    message.success('模板数据已导入');
  } finally {
    saving.value = false;
  }
}

async function handleTemplateImportSuccess(record?: MesFaiApi.FaiRecord) {
  if (record) {
    await loadRecord(record);
    syncSidebarTask(record);
  } else if (activeRecord.value?.id) {
    await loadRecordById(activeRecord.value.id);
  }
  templateImportExportModalOpen.value = false;
}
</script>

<template>
  <div class="flex h-full flex-col overflow-hidden bg-slate-100">
    <StationFormRuntimeFillModal
      v-if="!isGlueBoardMode"
      v-model:open="selfCheckRuntimeOpen"
      confirm-auth-action-name="确认过程首检自检记录"
      enable-confirm
      :form="selfCheckRuntimeForm"
      :initial-params="selfCheckRuntimeInitialParams"
      readonly-pass-work-header
      save-auth-action-name="保存过程首检自检记录"
      skip-business-param-step
      @success="handleSelfCheckRuntimeSuccess"
    />
    <header
      class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4"
    >
      <div class="flex min-w-0 items-center gap-3">
        <Button
          v-if="!props.embedded"
          size="small"
          @click="emit('backToLedger')"
        >
          <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回台账
        </Button>
        <div class="min-w-0">
          <div class="text-base font-bold text-slate-800">
            {{ props.pageTitle }}
          </div>
          <div class="text-xs text-slate-500">
            {{ props.pageDescription }}
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <Button
          size="small"
          :disabled="!activeRecord || !hasActiveItems()"
          @click="templatePreviewOpen = true"
        >
          <IconifyIcon icon="lucide:eye" class="mr-1" /> 查看录入表
        </Button>
        <Button
          v-if="props.enableImport"
          size="small"
          :disabled="!canMutateItems()"
          @click="templateImportExportModalOpen = true"
        >
          <IconifyIcon icon="lucide:file-spreadsheet" class="mr-1" />
          模板导入/导出
        </Button>
        <QmsFaiScanEntryBox
          v-if="showScanEntry && props.enableScan"
          :current-fai-id="activeRecord?.id"
          scene="WORKBENCH_HEADER"
          @resolved="handleScanResolved"
        />
        <Button
          v-if="
            requiresRecheckItemSelection &&
            activeRecord?.standardId &&
            activeRecord.standardSwitchAllowed
          "
          size="small"
          @click="handleOpenRecheckItemSelector"
        >
          <IconifyIcon icon="lucide:list-checks" class="mr-1" />
          选择复检项目
        </Button>
        <Button
          v-if="activeRecord?.standardId && activeRecord.standardSwitchAllowed"
          size="small"
          @click="handleOpenStandardSelector"
        >
          <IconifyIcon icon="lucide:replace" class="mr-1" />
          重新选择标准
        </Button>
        <Button size="small" :loading="loading" @click="handleRefresh">
          <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
        </Button>
      </div>
    </header>

    <main class="flex min-h-0 flex-1 gap-3 p-3">
      <aside
        v-if="showTaskSidebar"
        class="flex w-[320px] shrink-0 flex-col overflow-hidden rounded border bg-white"
      >
        <div class="border-b p-3">
          <Input
            v-model:value="searchKeyword"
            allow-clear
            placeholder="搜索单号、工单、物料、型号、批次"
          />
        </div>
        <div class="min-h-0 flex-1 space-y-2 overflow-y-auto p-3">
          <button
            v-for="item in filteredTaskList"
            :key="item.faiNo"
            type="button"
            class="w-full rounded border bg-white p-3 text-left transition hover:border-indigo-400 hover:bg-indigo-50"
            :class="
              activeRecord?.faiNo === item.faiNo
                ? 'border-indigo-500 bg-indigo-50'
                : 'border-slate-200'
            "
            @click="handleSelectTask(item)"
          >
            <div class="flex items-center justify-between gap-2">
              <span
                class="truncate font-mono text-sm font-bold text-indigo-700"
              >
                {{ item.faiNo }}
              </span>
              <Tag :color="statusColor(item.status)" class="!m-0">
                {{ statusLabel(item.status) }}
              </Tag>
            </div>
            <div class="mt-2 grid grid-cols-2 gap-2 text-xs text-slate-500">
              <span class="truncate">{{ item.workOrderNo }}</span>
              <span class="truncate text-right">{{
                resolveFaiDisplayBatchNo(item)
              }}</span>
              <span class="truncate">{{ item.materialCode }}</span>
              <span class="truncate text-right">{{
                item.productModel || '-'
              }}</span>
              <span class="col-span-2 truncate">{{ processLabel(item) }}</span>
            </div>
          </button>
        </div>
      </aside>

      <section
        class="flex min-w-0 flex-1 flex-col overflow-hidden rounded border bg-white"
      >
        <div
          v-if="!activeRecord"
          class="flex h-full flex-col items-center justify-center"
        >
          <Empty description="请从台账中选择首件检验单" />
        </div>

        <template v-else>
          <div class="overflow-x-auto border-b px-4 py-3">
            <div class="flex min-w-[960px] items-center gap-8">
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">首检单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.faiNo }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">工单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.workOrderNo || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">物料编码</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.materialCode || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">产品型号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.productModel || '-' }}
                </div>
              </div>
              <div class="min-w-[130px]">
                <div class="text-xs font-bold text-slate-400">检测工序</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ processLabel(activeRecord) }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">产品批次</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ resolveFaiDisplayBatchNo(activeRecord) }}
                </div>
              </div>
              <div class="min-w-[180px]">
                <div class="text-xs font-bold text-slate-400">检验标准</div>
                <div class="text-sm font-bold text-slate-800">
                  {{
                    activeRecord.standardNo
                      ? `${activeRecord.standardNo} / ${activeRecord.standardVersion || '-'}`
                      : '待选择'
                  }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">送检数量</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ formatInspectionQty(activeRecord) }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">检验人</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ activeRecord.operatorName || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">检验时间</div>
                <div class="text-sm font-bold text-slate-800">
                  {{
                    formatDateTimeText(
                      activeRecord.inspectionTime || activeRecord.operatorTime,
                    )
                  }}
                </div>
              </div>
              <div class="min-w-[120px]">
                <div class="text-xs font-bold text-slate-400">审核人</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ activeRecord.qaInspectorName || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">审核时间</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ formatDateTimeText(activeRecord.qaTime) }}
                </div>
              </div>
              <div class="flex min-w-[170px] items-center gap-2">
                <Tag :color="statusColor(activeRecord.status)" class="!m-0">
                  {{ statusLabel(activeRecord.status) }}
                </Tag>
                <Tag :color="resultColor(activeRecord.judgment)" class="!m-0">
                  {{ resultLabel(activeRecord.judgment) }}
                </Tag>
              </div>
            </div>
            <div
              v-if="activeRecord.lastReturnReason"
              class="mt-3 min-w-[960px] rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
            >
              <span class="font-bold">驳回原因：</span>
              <span>{{ activeRecord.lastReturnReason }}</span>
            </div>
          </div>

          <div
            v-if="standardPanelVisible"
            class="shrink-0 border-b border-amber-200 bg-amber-50 p-3"
          >
            <Alert
              show-icon
              type="warning"
              :message="
                recheckItemSelectionPending
                  ? '复检待选择具体检验项目'
                  : activeRecord.standardContentChanged
                  ? '检验标准内容已发生变化'
                  : activeRecord.standardId
                    ? '可以重新选择检验标准'
                    : '当前检验任务尚未挂接检验标准'
              "
              :description="
                recheckItemSelectionPending
                  ? '本单为复检。请在所选标准中勾选实际需要复检的项目；原检 NG 仅作提示，不会自动勾选。'
                  : activeRecord.standardSelectionMessage ||
                '开始录入前请选择启用且已审核的检验标准，系统会按所选标准重新生成本次检验明细。'
              "
            />
            <div
              v-if="activeRecord.standardSwitchAllowed"
              class="mt-3 flex flex-wrap items-start gap-2"
            >
              <Select
                v-model:value="selectedStandardId"
                class="min-w-[420px] flex-1"
                :filter-option="true"
                :loading="standardLoading"
                :options="standardOptions"
                placeholder="请选择本次执行使用的检验标准"
                show-search
              />
              <Input.TextArea
                v-model:value="standardBindReason"
                class="min-w-[320px] flex-1"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                placeholder="可填写重新选择或重新加载原因"
              />
              <Button
                type="primary"
                :disabled="
                  !selectedStandardId || !activeRecord.standardSwitchAllowed
                "
                :loading="standardBinding"
                @click="handleBindStandard"
              >
                <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                {{
                  requiresRecheckItemSelection
                    ? '选择复检项目'
                    : activeRecord.standardId
                    ? '按所选标准重新加载'
                    : '挂接并生成检验明细'
                }}
              </Button>
              <Button
                v-if="
                  activeRecord.standardId &&
                  !activeRecord.standardContentChanged
                "
                @click="standardSelectorOpen = false"
              >
                取消
              </Button>
            </div>
            <div
              v-if="
                activeRecord.standardSwitchAllowed &&
                !standardLoading &&
                standardOptions.length === 0
              "
              class="mt-2 text-xs text-amber-700"
            >
              当前没有与本任务物料/型号及工序匹配的启用、已审核标准，请先维护标准后刷新任务。
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-auto p-3">
            <div class="overflow-x-auto rounded border">
              <table
                class="w-full min-w-[1400px] table-fixed border-collapse text-sm"
              >
                <thead class="bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th class="w-10 border-b px-2 py-2 text-center"></th>
                    <th class="w-12 border-b px-2 py-2 text-center">序号</th>
                    <th class="w-28 border-b px-2 py-2 text-left">检验项目</th>
                    <th class="w-36 border-b px-2 py-2 text-left">
                      平均值内控
                    </th>
                    <th class="w-36 border-b px-2 py-2 text-left">
                      标准差内控
                    </th>
                    <th class="w-48 border-b px-2 py-2 text-left">位置配置</th>
                    <th class="w-24 border-b px-2 py-2 text-center">填值</th>
                    <th class="w-24 border-b px-2 py-2 text-right">平均值</th>
                    <th class="w-24 border-b px-2 py-2 text-right">标准差</th>
                    <th class="w-24 border-b px-2 py-2 text-center">判定</th>
                    <th class="w-24 border-b px-2 py-2 text-center">检验人</th>
                    <th class="w-28 border-b px-2 py-2 text-center">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="displayItems.length === 0">
                    <td colspan="12" class="py-12">
                      <Empty
                        :description="
                          activeRecord.standardId
                            ? '当前检验标准没有可执行的检验项目'
                            : '请先选择检验标准'
                        "
                      />
                    </td>
                  </tr>
                  <template
                    v-for="(item, index) in displayItems"
                    :key="resolveItemKey(item)"
                  >
                    <tr
                      class="cursor-pointer border-b hover:bg-indigo-50"
                      :class="
                        selectedItemId === resolveItemKey(item)
                          ? 'bg-indigo-50'
                          : ''
                      "
                      @click="selectedItemId = resolveItemKey(item)"
                      @dblclick="handleOpenItemModal(item)"
                    >
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-6 w-6 items-center justify-center rounded text-slate-500 hover:bg-indigo-100 hover:text-indigo-700"
                          :title="
                            isItemExpanded(item)
                              ? '收起填报内容'
                              : '展开填报内容'
                          "
                          @click.stop="toggleItemTree(item)"
                        >
                          <IconifyIcon
                            :icon="
                              isItemExpanded(item)
                                ? 'lucide:chevron-down'
                                : 'lucide:chevron-right'
                            "
                            class="text-base"
                          />
                        </button>
                      </td>
                      <td class="px-2 py-2 text-center text-slate-500">
                        {{ index + 1 }}
                      </td>
                      <td class="px-2 py-2 font-bold text-slate-800">
                        {{ displayMetricName(item) }}
                      </td>
                      <td class="px-2 py-2 font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`avg-limit-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.avgLimitText }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{
                            controlText(
                              item.avgMinLimit,
                              item.avgMaxLimit,
                              valueUnit(item),
                            )
                          }}
                        </template>
                      </td>
                      <td class="px-2 py-2 font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`std-limit-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.stdLimitText }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{
                            controlText(
                              item.stdMinLimit,
                              item.stdMaxLimit,
                              valueUnit(item),
                            )
                          }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-xs text-slate-500">
                        {{ positionSummary(item) }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        {{ countFilled(item) }}/{{
                          resolveEntryRuleSampleSize(item)
                        }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`avg-value-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.avgValue }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{ formatNumber(item.calculatedAvg ?? item.qaAvg) }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        <template v-if="isEntryGroupItem(item)">
                          <div
                            v-for="row in groupMetricRows(item)"
                            :key="`std-value-${row.key}`"
                            class="flex justify-between gap-2 text-xs leading-5"
                          >
                            <span class="font-sans text-slate-500">{{
                              row.name
                            }}</span>
                            <span>{{ row.stdValue }}</span>
                          </div>
                        </template>
                        <template v-else>
                          {{ formatNumber(item.calculatedStd) }}
                        </template>
                      </td>
                      <td class="px-2 py-2 text-center">
                        <Tag :color="itemReviewColor(item)" class="!m-0">
                          {{ itemReviewLabel(item) }}
                        </Tag>
                      </td>
                      <td class="px-2 py-2 text-center text-slate-700">
                        {{ item.operatorName || '-' }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-7 cursor-pointer items-center justify-center rounded border border-indigo-200 bg-indigo-50 px-3 text-xs font-bold text-indigo-700 hover:bg-indigo-100"
                          @click.stop="handleOpenItemModal(item)"
                          @mousedown.stop.prevent="handleOpenItemModal(item)"
                        >
                          {{ isItemReadOnly(item) ? '查看' : '填写' }}
                        </button>
                      </td>
                    </tr>
                    <tr
                      v-if="isItemExpanded(item)"
                      class="border-b bg-slate-50"
                    >
                      <td colspan="12" class="px-4 py-3">
                        <div class="rounded border bg-white p-3 text-xs">
                          <div
                            class="mb-2 flex items-center justify-between gap-2"
                          >
                            <div class="font-bold text-slate-700">
                              {{ displayMetricName(item) }} 填报内容
                            </div>
                            <Tag :color="itemReviewColor(item)" class="!m-0">
                              {{ itemReviewLabel(item) }}
                            </Tag>
                          </div>
                          <div
                            v-if="item.ruleDescription"
                            class="mb-2 rounded border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-800"
                          >
                            <span class="font-bold">填写说明：</span>
                            <span>{{ item.ruleDescription }}</span>
                          </div>
                          <div class="space-y-2">
                            <div
                              v-for="position in buildFillTreeRows(item)"
                              :key="position.code"
                              class="border-l-2 border-indigo-100 pl-3"
                            >
                              <div
                                class="flex items-center gap-2 font-bold text-slate-700"
                              >
                                <IconifyIcon
                                  icon="lucide:map-pin"
                                  class="text-indigo-500"
                                />
                                <span>{{ position.name }}</span>
                              </div>
                              <div class="mt-1 grid gap-1 pl-5">
                                <div
                                  v-for="sample in position.samples"
                                  :key="`${position.code}-${sample.groupNo}`"
                                  class="flex min-w-0 items-center gap-2 rounded bg-slate-50 px-2 py-1"
                                >
                                  <span
                                    class="w-14 shrink-0 font-mono text-slate-500"
                                  >
                                    第{{ sample.groupNo }}组
                                  </span>
                                  <Tag
                                    :color="
                                      sample.filled ? 'success' : 'default'
                                    "
                                    class="!m-0 shrink-0"
                                  >
                                    {{ sample.filled ? '已填' : '未填' }}
                                  </Tag>
                                  <span
                                    class="min-w-0 flex-1 truncate font-mono text-slate-700"
                                  >
                                    {{ sample.text }}
                                  </span>
                                  <span
                                    v-if="sample.filled"
                                    class="shrink-0 text-slate-500"
                                  >
                                    {{ sample.result }}
                                  </span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>

          <footer
            class="flex shrink-0 items-center justify-between border-t bg-white px-4 py-3"
          >
            <div class="text-xs text-slate-500">
              已完成
              {{
                activeRecord.completedItemCount ??
                activeItems.filter(
                  (item) => item.qaResult === 'OK' || item.qaResult === 'NG',
                ).length
              }}
              / {{ activeRecord.requiredItemCount ?? activeItems.length }} 项
            </div>
            <div class="flex items-center gap-2">
              <Button
                type="primary"
                :disabled="!canSubmitConfirm()"
                :loading="saving"
                @click="handleSubmit"
              >
                <IconifyIcon icon="lucide:send" class="mr-1" /> 提交检测结果
              </Button>
            </div>
          </footer>
        </template>
      </section>
    </main>
    <QmsFaiItemValueInputModal
      v-model:open="inputModalOpen"
      :item="modalItem"
      :item-count="displayItems.length"
      :item-index="modalItemIndex"
      :entry-group-items="modalItem ? entryGroupItems(modalItem) : []"
      :readonly="isItemReadOnly(modalItem)"
      :record="activeRecord"
      :saving="saving"
      @recalculate="handleRecalculateModalItem"
      @save="handleSaveModalItem"
    />
    <QmsFaiTemplateImportExportModal
      v-if="props.enableImport"
      v-model:open="templateImportExportModalOpen"
      :api-mode="props.apiMode"
      :items="activeItems"
      :readonly="isReadOnly()"
      :record="activeRecord"
      :saving="saving"
      @apply="handleTemplateImportApply"
      @success="handleTemplateImportSuccess"
    />
    <QmsFaiTemplatePreviewModal
      v-model:open="templatePreviewOpen"
      :items="activeItems"
      :record="activeRecord"
    />
    <Modal
      v-model:open="recheckItemSelectionOpen"
      :confirm-loading="standardBinding"
      :ok-button-props="{
        disabled: selectedRecheckStandardItemIds.length === 0,
      }"
      cancel-text="取消"
      ok-text="确认生成复检项目"
      title="选择复检检验项目"
      width="860px"
      @ok="confirmRecheckItemSelection"
    >
      <Alert
        show-icon
        type="warning"
        message="请按实际复检需要选择项目"
        description="所有项目默认不勾选；标记“原检 NG”的项目仅用于提示，不会被系统自动选择。"
      />
      <div class="mt-3 max-h-[420px] overflow-y-auto rounded border">
        <div
          v-if="recheckItemCandidateLoading"
          class="py-10 text-center text-slate-400"
        >
          正在加载标准检验项目…
        </div>
        <Empty
          v-else-if="recheckItemCandidates.length === 0"
          class="py-6"
          description="当前标准没有可选择的检验项目"
        />
        <label
          v-for="item in recheckItemCandidates"
          v-else
          :key="item.standardItemId"
          class="flex cursor-pointer items-start gap-3 border-b px-4 py-3 last:border-b-0 hover:bg-slate-50"
        >
          <input
            :checked="isRecheckStandardItemSelected(item.standardItemId)"
            :disabled="recheckItemCandidateLoading"
            class="mt-1 h-4 w-4"
            type="checkbox"
            @change="
              toggleRecheckStandardItem(
                item.standardItemId,
                ($event.target as HTMLInputElement).checked,
              )
            "
          />
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2 font-bold text-slate-800">
              <span>{{ item.sort ?? '-' }}. {{ item.inspectionItem }}</span>
              <Tag v-if="item.originalNg" color="error" class="!m-0">
                原检 NG
              </Tag>
            </div>
            <div class="mt-1 text-xs text-slate-500">
              标准：{{ item.standardDesc || '-' }}{{ item.unit ? `（${item.unit}）` : '' }}
            </div>
            <div class="mt-1 text-xs text-slate-400">
              方法：{{ item.inspectionMethod || '-' }} · 样本数：{{ item.sampleSize ?? '-' }}
            </div>
          </div>
        </label>
      </div>
      <div class="mt-2 text-xs text-slate-500">
        已选择 {{ selectedRecheckStandardItemIds.length }} 项
      </div>
    </Modal>
  </div>
</template>
