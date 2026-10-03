<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import type { PickerOption } from '#/components/picker';

import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Select,
  Table,
  Tabs,
  TabPane,
  Tag,
  Textarea,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmProcessFormRecordBySigner,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import {
  cancelFgInboundPackageLock,
  cancelFgInboundPackageLocks,
  createPackagingManualPiece,
  deletePackagingManualPiece,
  exportPackagingManualPieceImportTemplate,
  getFgInboundPackedSegmentPackageList,
  getFgInboundPackedSegmentPage,
  getFgInboundWaitSegmentPage,
  getFgInboundWaitSegmentPieceList,
  getPackagingPieceLabelCandidatePage,
  getPackagingPieceLabels,
  importPackagingManualPieces,
  lockFgInboundPackage,
  markPackagingPieceLabelsPrinted,
} from '#/api/mes/hc/package-fg/finished-packaging';
import {
  getStationFormDetail,
  getStationFormSimpleList,
} from '#/api/mes/hc/stationform';
import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';
import { PickerModal, productBomPickerConfig } from '#/components/picker';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';
import {
  buildPackagingPieceLabelPayload,
  resolvePackagingPieceLabelTemplateCode,
} from '../shared/packagingPieceLabelPrint';
import { resolveTransferTicketQrBusinessNo } from '../shared/workOrderTicketPrint';
import {
  buildSegmentChainSampleLockCandidates,
  ensureSampleAbnormalUnlocked,
} from '../shared/sampleAbnormalLockGuard';

import PackagingNcrFlowModal from './modules/PackagingNcrFlowModal.vue';

import '../../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesExecutionPackagingConsole' });

type InspectionSlice = MesHcFinishedPackagingApi.InspectionSlice;
type InspectionSliceSegment = MesHcFinishedPackagingApi.InspectionSliceSegment;
type InspectionSliceSegmentWithCoaResult = InspectionSliceSegment &
  Pick<InspectionSlice, 'coaInspectionResult'>;
type InboundPackage = MesHcFinishedPackagingApi.InboundBox;
type InboundPackageItem = NonNullable<InboundPackage['items']>[number];
type InboundPackageSegment = MesHcFinishedPackagingApi.InboundPackageSegment;
type PieceLabel = MesHcFinishedPackagingApi.PieceLabel;
type PackageAuxBalance = MesHcToolingConsumableLedgerApi.Balance;
type PackageAuxConsumeFormItem = {
  consumeQty?: number;
  ledgerId?: number;
};
type InspectionSliceDisplayRow = Partial<InspectionSlice> & {
  __group?: boolean;
  children?: InspectionSliceDisplayRow[];
  groupCount?: number;
  groupKey?: string;
  hasChild?: boolean;
  productionDateEnd?: string;
  productionDateStart?: string;
  sampleSliceBatchNos?: string[];
  waitRowKey: string;
};
type InboundPackageDisplayRow = InboundPackage & {
  __group?: boolean;
  children?: InboundPackageDisplayRow[];
  groupPackageCount?: number;
  groupPieceCount?: number;
  hasChild?: boolean;
  packageRowKey: string;
  sampleSliceBatchNos?: string[];
  segmentBatchNo?: string;
};
type PackagingQualityStatus = 'FROZEN' | 'NG' | 'OK';
type GridCellClickColumn = {
  field?: string;
  type?: string;
};
type GridCellClickEvent<T> = {
  $event?: Event;
  column?: GridCellClickColumn;
  row: T;
};
type PackagingProcessFormAction = 'CLEANING' | 'STARTUP';
type ProcessFormEditorMode = 'edit' | 'view';
type PackagingDailyCheckCardStatus =
  | 'CONFIRMED'
  | 'PENDING_CONFIRM'
  | 'UNFILLED';
type PackagingDailyCheckCard = {
  action: PackagingProcessFormAction;
  record?: MesHcProcessFormApi.Record;
  status: PackagingDailyCheckCardStatus;
  timing: string;
  title: string;
};

const ncrFlowVisible = ref(false);

const PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PACKAGING_PROCESS_CODE = 'PACKAGING';
const PACKAGING_PROCESS_NAME = '包装';
const PRINT_BATCH_MAX_COUNT = 50;
const WAIT_SEGMENT_COA_PREFETCH_CONCURRENCY = 4;
const WAIT_QUALIFIED_TAB = 'waitQualified';
const WAIT_FROZEN_TAB = 'waitFrozen';
const WAIT_UNQUALIFIED_TAB = 'waitUnqualified';
const PACKED_TAB = 'packed';
const STARTUP_CHECK_TAB = 'startupCheck';
const CLEANING_CHECK_TAB = 'cleaningCheck';
const SOURCE_CUT_ROUND_REPORT = 'CUT_ROUND_REPORT';
const SOURCE_MANUAL_HISTORY = 'MANUAL_HISTORY';
const EXTENDED_RECORD_TABS = [STARTUP_CHECK_TAB, CLEANING_CHECK_TAB];
const PACKAGING_PROCESS_FORMS: Record<
  PackagingProcessFormAction,
  {
    formCode: string;
    formType: string;
    formTypeName: string;
    title: string;
  }
> = {
  CLEANING: {
    formCode: 'PACKAGING_CLEANING_CHECK_DEV',
    formType: 'CLEANING_CHECK',
    formTypeName: '内包装清洁保养',
    title: 'CMP软垫内包装设备清洁点检表',
  },
  STARTUP: {
    formCode: 'PACKAGING_STARTUP_CHECK_DEV',
    formType: 'STARTUP_CHECK',
    formTypeName: '内包装开机点检',
    title: 'CMP软垫内包装开机点检表',
  },
};
const PACKAGING_DAILY_RECORDS = [
  {
    action: 'STARTUP' as const,
    formType: 'STARTUP_CHECK',
    label: '包装开机点检',
  },
  {
    action: 'CLEANING' as const,
    formType: 'CLEANING_CHECK',
    label: '包装清洁保养',
  },
];
const PACKAGING_DAILY_CHECK_ACTIONS: PackagingProcessFormAction[] = [
  'STARTUP',
  'CLEANING',
];
const PACKAGING_DAILY_CHECK_CARD_CONFIG: Record<
  PackagingProcessFormAction,
  { timing: string; title: string }
> = {
  CLEANING: { timing: '清洁后/开机前', title: '清洁点检表' },
  STARTUP: { timing: '开机前', title: '开机点检表' },
};

const userStore = useUserStore();
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统',
);

const activeTab = ref(WAIT_QUALIFIED_TAB);
const showPackagingRecordTabs = ref(false);
const packagingDailyCheckDialogVisible = ref(false);
const packagingDailyCheckCards = ref<PackagingDailyCheckCard[]>([]);
const packagingDailyCheckLoading = ref(false);
const waitLoading = ref(false);
const packageLoading = ref(false);
const startupCheckLoading = ref(false);
const cleaningCheckLoading = ref(false);
const packaging = ref(false);
const revoking = ref(false);
const keyword = ref('');
const packageKeyword = ref('');
const qualifiedWaitTotal = ref(0);
const frozenWaitTotal = ref(0);
const frozenWaitPrintRows = ref<InspectionSlice[]>([]);
const frozenWaitPrintCount = computed(() => frozenWaitPrintRows.value.length);
const unqualifiedWaitTotal = ref(0);
const packedSegmentTotal = ref(0);
const selectedRowKeys = ref<string[]>([]);
const selectedRows = ref<InspectionSlice[]>([]);
const qualifiedWaitPrintRows = ref<InspectionSlice[]>([]);
const unqualifiedWaitPrintRows = ref<InspectionSlice[]>([]);
const selectedPackagePrintRows = ref<InboundPackage[]>([]);
const batchRevoking = ref(false);
const waitSegmentPackageSelecting = ref(false);
const waitSegmentPrintSelecting = ref(false);
const currentPackage = ref<InboundPackage | null>(null);
const packageVisible = ref(false);
const packageScanVisible = ref(false);
const packageScanResolving = ref(false);
const packageScanCode = ref('');
const packageScanRows = ref<InspectionSlice[]>([]);
const detailVisible = ref(false);
const runtimeFillOpen = ref(false);
const runtimeFillAction = ref<PackagingProcessFormAction>('STARTUP');
const runtimeFillForm = ref<MesHcStationFormApi.StationForm | null>(null);
const runtimeInitialParams = ref<Record<string, any>>({});
const runtimeViewVisible = ref(false);
const runtimeViewLoading = ref(false);
const runtimeViewSaving = ref(false);
const runtimeViewConfirming = ref(false);
const runtimeSaveAuthVisible = ref(false);
const runtimeConfirmAuthVisible = ref(false);
const runtimeConfirmAuthRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeViewMode = ref<ProcessFormEditorMode>('view');
const runtimeViewRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeViewHeaderData = ref<Record<string, any>>({});
const runtimeViewSchema = ref<Record<string, any>>({});
const runtimeRendererRef = ref<any>();
const packagingCheckQuery = reactive({
  recordDate: dayjs().format('YYYY-MM-DD'),
});

const packageForm = reactive({
  auxConsumeItems: [{}] as PackageAuxConsumeFormItem[],
  packageNo: '',
  remark: '',
});
const packageAuxBalances = ref<PackageAuxBalance[]>([]);
const packageAuxLoading = ref(false);
const manualPieceVisible = ref(false);
const manualPieceSubmitting = ref(false);
const manualPieceDeleteVisible = ref(false);
const manualPieceDeleting = ref(false);
const manualPieceDeleteReason = ref('');
const manualPieceDeleteTarget = ref<InspectionSliceDisplayRow>();
const manualPieceImportVisible = ref(false);
const manualPieceImporting = ref(false);
const manualPieceTemplateExporting = ref(false);
const manualPieceImportInputRef = ref<HTMLInputElement>();
const productBomPickerOpen = ref(false);
const manualPieceForm =
  reactive<MesHcFinishedPackagingApi.PackagingManualPieceCreateReq>({
    backfillReason: '',
    coaInspectionResult: 'OK',
    expiryDate: '',
    inspectionResult: 'OK',
    materialCode: '',
    modelCode: '',
    productionDate: '',
    recorderName: '',
    remark: '',
    segmentBatchNo: '',
    sliceBatchNo: '',
  });
const piecePrintVisible = ref(false);
const piecePrintLoading = ref(false);
const piecePrinting = ref(false);
const pieceLabels = ref<PieceLabel[]>([]);
const piecePrintCandidates = ref<PieceLabel[]>([]);
const piecePrintCandidatePage = reactive({ pageNo: 1, pageSize: 10, total: 0 });
const piecePrintFilters =
  reactive<MesHcFinishedPackagingApi.PieceLabelCandidatePageParams>({
    businessStatus: '',
    pageNo: 1,
    pageSize: 10,
    sliceBatchNo: '',
    sourceType: '',
  });
const piecePrintCandidateColumns = [
  { key: 'selected', title: '选择', width: 64 },
  { dataIndex: 'sliceBatchNo', key: 'sliceBatchNo', title: '片号', width: 190 },
  { key: 'product', title: '型号 / 料号', width: 190 },
  { key: 'source', title: '来源', width: 110 },
  { key: 'businessStatus', title: '业务状态', width: 130 },
];

const qualifiedWaitPrintCount = computed(
  () => qualifiedWaitPrintRows.value.length,
);
const unqualifiedWaitPrintCount = computed(
  () => unqualifiedWaitPrintRows.value.length,
);
const selectedPackagePrintCount = computed(
  () => selectedPackagePrintRows.value.length,
);
const selectedPackagePiecePrintCount = computed(
  () =>
    new Set(
      selectedPackagePrintRows.value.flatMap((row) =>
        (row.items || [])
          .map((item) =>
            String(item.sliceBatchNo || item.productionBatchNo || '').trim(),
          )
          .filter(Boolean),
      ),
    ).size,
);

const qualifiedSelectedCount = computed(
  () =>
    selectedRows.value.filter(
      (item) => resolvePackagingQualityStatus(item) === 'OK',
    ).length,
);
const frozenSelectedCount = computed(() => selectedRows.value.filter((row) => resolvePackagingQualityStatus(row) === 'FROZEN').length);
const unqualifiedSelectedCount = computed(
  () =>
    selectedRows.value.filter(
      (item) => resolvePackagingQualityStatus(item) === 'NG',
    ).length,
);
const coaNgSelectedCount = computed(
  () =>
    selectedRows.value.filter((item) => item.coaInspectionResult === 'NG')
      .length,
);
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const runtimeViewAction = computed(() =>
  resolveProcessFormAction(runtimeViewRecord.value || {}),
);
const runtimeViewReadOnly = computed(
  () =>
    runtimeViewMode.value === 'view' ||
    runtimeViewRecord.value?.recordStatus === 'CONFIRMED',
);
const runtimeViewTitle = computed(
  () =>
    runtimeViewRecord.value?.templateName ||
    PACKAGING_PROCESS_FORMS[runtimeViewAction.value].title,
);
const runtimeFillTitle = computed(
  () => PACKAGING_PROCESS_FORMS[runtimeFillAction.value].title,
);
const runtimeViewRecordMeta = computed(() => [
  `工序：${runtimeViewRecord.value?.processName || PACKAGING_PROCESS_NAME}`,
  `类型：${runtimeViewRecord.value?.formTypeName || PACKAGING_PROCESS_FORMS[runtimeViewAction.value].formTypeName}`,
  `状态：${recordStatusText(runtimeViewRecord.value?.recordStatus)}`,
  `上报日期：${runtimeViewRecord.value?.recordDate || '-'}`,
]);
const runtimeViewItems = computed<MesHcStationFormApi.StationFormItem[]>(() =>
  buildRuntimeDetailRows(runtimeViewRecord.value).map((row) => ({
    defaultResult: row.resultFlag,
    id: row.templateItemId,
    itemCategory: row.itemCategory,
    itemName: row.itemName,
    itemSeq: row.seq,
    standardText: row.standardText,
    stepNode: row.stepNode,
    valueMode: row.valueMode,
  })),
);
const packageAuxOptions = computed(() =>
  packageAuxBalances.value.map((item) => ({
    label: `${item.consumableTypeName || '-'}${item.model ? ` / ${item.model}` : ''}｜批次 ${item.batchNo || '-'}｜可用 ${formatPackageAuxQty(item.balanceQty)} ${item.uomName || item.uom || '个'}`,
    value: item.ledgerId,
  })),
);
const piecePrintButtonText = computed(
  () => `批量打印（${pieceLabels.value.length}）`,
);
watch(
  () => manualPieceForm.productionDate,
  (productionDate) => {
    manualPieceForm.expiryDate = productionDate
      ? dayjs(productionDate)
          .add(10, 'month')
          .subtract(1, 'day')
          .format('YYYY-MM-DD')
      : '';
  },
);
function openQuickCheck(type: 'STARTUP' | 'CLEANING') {
  hidePackagingRecordTabs();
  void openProcessFormEditor(type);
}

function resultColor(result?: string) {
  if (result === 'FROZEN') return 'gold';
  if (result === 'OK') return 'green';
  if (result === 'NG') return 'red';
  return 'default';
}

function isNgResult(result?: string) {
  return String(result || '').toUpperCase() === 'NG';
}

function resolvePackagingQualityStatus(row?: InspectionSlice) {
  if (!row) return 'OK';
  const status = String(row.packagingQualityStatus || '').toUpperCase();
  if (status === 'OK' || status === 'NG' || status === 'FROZEN') {
    return status;
  }
  return isNgResult(row.inspectionResult) || isNgResult(row.coaInspectionResult)
    ? 'NG'
    : 'OK';
}

function coaResultText(result?: string) {
  const map: Record<string, string> = {
    NG: 'NG',
    OK: 'OK',
    PENDING: '待检',
    UNKNOWN: '未送检',
  };
  return map[result || ''] || result || '-';
}

function coaResultColor(result?: string) {
  if (result === 'OK') return 'green';
  if (result === 'NG') return 'red';
  if (result === 'PENDING') return 'gold';
  return 'default';
}

function formatCoaTooltip(row: Partial<InspectionSlice>) {
  const items = [
    `段号：${row.coaScopeBatchNo || row.parentProductionBatchNo || '-'}`,
    `送检片：${row.coaSampleBatchNo || '-'}`,
    `COA单：${row.coaInspectionNo || '-'}`,
  ];
  if (row.coaInspectionStatus) {
    items.push(`状态：${row.coaInspectionStatus}`);
  }
  if (row.coaNgReason) {
    items.push(`原因：${row.coaNgReason}`);
  }
  return items.join('；');
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: '已上架',
    INBOUND_LOCKED: '待上架',
    PACKED: '已包装待上架',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    INBOUNDED: 'green',
    INBOUND_LOCKED: 'gold',
    PACKED: 'blue',
  };
  return map[status || ''] || 'default';
}

function recordStatusText(status?: string) {
  const map: Record<string, string> = {
    CONFIRMED: '已确认',
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    VOID: '已作废',
  };
  return map[status || ''] || status || '-';
}

function recordStatusColor(status?: string) {
  const map: Record<string, string> = {
    CONFIRMED: 'green',
    DRAFT: 'default',
    SUBMITTED: 'blue',
    VOID: 'red',
  };
  return map[status || ''] || 'default';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatPackageSliceList(row?: InboundPackage | null) {
  const values = (row?.items || [])
    .map((item) => item.sliceBatchNo || item.productionBatchNo)
    .filter(Boolean);
  return values.join(',') || '-';
}

function safeParseJson<T = any>(value?: null | string): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function firstText(...values: Array<null | number | string | undefined>) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function schemaText(schema: Record<string, any>, ...keys: string[]) {
  for (const key of keys) {
    const value = schema?.[key];
    if (Array.isArray(value)) {
      const text = value
        .map((item) => String(item ?? '').trim())
        .filter(Boolean)
        .join(',');
      if (text) return text;
      continue;
    }
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function normalizeResultFlag(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (!text) return '';
  if (
    text.includes('NG') ||
    text.includes('×') ||
    text.includes('不合格') ||
    text.includes('异常')
  )
    return 'NG';
  if (
    text.includes('OK') ||
    text.includes('√') ||
    text.includes('合格') ||
    text.includes('正常')
  )
    return 'OK';
  return '';
}

function resolveProcessFormAction(record: Partial<MesHcProcessFormApi.Record>) {
  if (record.formType === 'STARTUP_CHECK') return 'STARTUP';
  return 'CLEANING';
}

async function loadStationTemplate(action: PackagingProcessFormAction) {
  const config = PACKAGING_PROCESS_FORMS[action];
  const forms = await getStationFormSimpleList(PACKAGING_PROCESS_CODE);
  const target =
    forms.find((item) => item.formCode === config.formCode) ||
    forms.find((item) => {
      const schema = safeParseJson<Record<string, any>>(item.schemaJson) || {};
      return (
        schemaText(schema, 'processFormType', 'formType') === config.formType
      );
    });
  if (!target?.id) {
    throw new Error(`未找到 DEV 模板：${config.formCode}`);
  }
  return getStationFormDetail(target.id);
}

function stationFormType(
  form: MesHcStationFormApi.StationForm,
  action: PackagingProcessFormAction,
) {
  const schema = safeParseJson<Record<string, any>>(form.schemaJson) || {};
  return (
    schemaText(schema, 'processFormType', 'formType') ||
    PACKAGING_PROCESS_FORMS[action].formType
  );
}

function stationFormTypeName(
  form: MesHcStationFormApi.StationForm,
  action: PackagingProcessFormAction,
) {
  const schema = safeParseJson<Record<string, any>>(form.schemaJson) || {};
  return (
    schemaText(schema, 'formTypeName', 'processFormTypeName') ||
    PACKAGING_PROCESS_FORMS[action].formTypeName
  );
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function resetManualPieceForm() {
  Object.assign(manualPieceForm, {
    backfillReason: '',
    coaInspectionResult: 'OK',
    expiryDate: '',
    inspectionResult: 'OK',
    materialCode: '',
    modelCode: '',
    productionDate: '',
    recorderName: currentUserName.value,
    remark: '',
    segmentBatchNo: '',
    sliceBatchNo: '',
  });
}

function openManualPieceModal() {
  resetManualPieceForm();
  manualPieceVisible.value = true;
}

function handleProductBomPick(option: PickerOption) {
  const extra = option.extra || {};
  manualPieceForm.materialCode = String(
    extra.productMaterialCode || option.code || '',
  ).trim();
  manualPieceForm.modelCode = String(extra.productModelCode || '').trim();
  productBomPickerOpen.value = false;
}

function validateManualPieceForm() {
  const requiredFields = [
    [manualPieceForm.sliceBatchNo, '片号'],
    [manualPieceForm.segmentBatchNo, '分段批号'],
    [manualPieceForm.modelCode, '产品型号'],
    [manualPieceForm.materialCode, '产品料号'],
    [manualPieceForm.productionDate, '生产日期'],
    [manualPieceForm.expiryDate, '有效期'],
    [manualPieceForm.backfillReason, '补录原因'],
  ];
  const missing = requiredFields.find(([value]) => !String(value || '').trim());
  if (missing) {
    message.warning(`请填写${missing[1]}`);
    return false;
  }
  const expectedExpiryDate = dayjs(manualPieceForm.productionDate)
    .add(10, 'month')
    .subtract(1, 'day')
    .format('YYYY-MM-DD');
  if (manualPieceForm.expiryDate !== expectedExpiryDate) {
    message.warning(`有效期应为生产日期加10个月减1天：${expectedExpiryDate}`);
    return false;
  }
  return true;
}

async function submitManualPiece() {
  if (!validateManualPieceForm()) return;
  manualPieceSubmitting.value = true;
  try {
    const created = await createPackagingManualPiece({
      ...manualPieceForm,
      backfillReason: manualPieceForm.backfillReason.trim(),
      materialCode: manualPieceForm.materialCode.trim().toUpperCase(),
      modelCode: manualPieceForm.modelCode.trim().toUpperCase(),
      recorderName: currentUserName.value,
      remark: manualPieceForm.remark?.trim() || undefined,
      segmentBatchNo: manualPieceForm.segmentBatchNo.trim().toUpperCase(),
      sliceBatchNo: manualPieceForm.sliceBatchNo.trim().toUpperCase(),
    });
    manualPieceVisible.value = false;
    activeTab.value =
      resolvePackagingQualityStatus(created) === 'NG'
        ? WAIT_UNQUALIFIED_TAB
        : resolvePackagingQualityStatus(created) === 'FROZEN' ? WAIT_FROZEN_TAB : WAIT_QUALIFIED_TAB;
    keyword.value = created.sliceBatchNo || created.productionBatchNo || '';
    await fetchAllWaitPieces();
    Modal.confirm({
      cancelText: '稍后打印',
      content: `历史片 ${created.sliceBatchNo || created.productionBatchNo || ''} 已进入待包装区，是否立即打印片号标签？`,
      okText: '立即打印',
      title: '历史片新增成功',
      onOk: () =>
        openPiecePrintModal(created.sliceBatchNo || created.productionBatchNo),
    });
  } finally {
    manualPieceSubmitting.value = false;
  }
}

function canDeleteManualPiece(row?: InspectionSliceDisplayRow) {
  return Boolean(
    row &&
    !row.__group &&
    row.sourceType === SOURCE_MANUAL_HISTORY &&
    Number(row.sourceManualPieceId || 0) > 0,
  );
}

function openManualPieceDeleteModal(row: InspectionSliceDisplayRow) {
  if (!canDeleteManualPiece(row)) return;
  manualPieceDeleteTarget.value = row;
  manualPieceDeleteReason.value = '';
  manualPieceDeleteVisible.value = true;
}

async function submitManualPieceDelete() {
  const target = manualPieceDeleteTarget.value;
  const deleteReason = manualPieceDeleteReason.value.trim();
  if (!canDeleteManualPiece(target)) {
    message.warning('历史补录片不存在，请刷新后重试');
    return;
  }
  if (!deleteReason) {
    message.warning('请填写删除原因');
    return;
  }
  manualPieceDeleting.value = true;
  try {
    await deletePackagingManualPiece({
      deleteReason,
      id: Number(target?.sourceManualPieceId),
    });
    manualPieceDeleteVisible.value = false;
    message.success(
      `历史补录片 ${target?.sliceBatchNo || target?.productionBatchNo || ''} 已删除`,
    );
    await fetchAllWaitPieces();
  } finally {
    manualPieceDeleting.value = false;
  }
}

async function downloadManualPieceImportTemplate() {
  manualPieceTemplateExporting.value = true;
  try {
    const data = await exportPackagingManualPieceImportTemplate();
    downloadFileFromBlobPart({
      fileName: '成品包装历史片导入模板.xlsx',
      source: data,
    });
  } finally {
    manualPieceTemplateExporting.value = false;
  }
}

function selectManualPieceImportFile() {
  manualPieceImportInputRef.value?.click();
}

function showManualPieceImportFailures(
  result: MesHcFinishedPackagingApi.PackagingManualPieceImportResult,
) {
  const failures = result.failures || [];
  Modal.warning({
    content: h('div', { class: 'manual-piece-import-failures' }, [
      h(
        'div',
        `读取 ${result.totalRows || 0} 行，跳过 ${result.skippedRows || 0} 行，失败 ${result.failureCount || 0} 条；本次未写入任何数据，可滚动查看全部错误信息。`,
      ),
      h('pre', failures.join('\n')),
    ]),
    title: '历史片导入校验未通过',
    width: 780,
  });
}

async function importManualPieceFile(file: File) {
  manualPieceImporting.value = true;
  const hideLoading = message.loading({
    content: '正在校验并导入历史片...',
    duration: 0,
  });
  try {
    const result = await importPackagingManualPieces(
      file,
      currentUserName.value,
    );
    if (Number(result.failureCount || 0) > 0) {
      showManualPieceImportFailures(result);
      return;
    }
    manualPieceImportVisible.value = false;
    message.success(
      `历史片导入成功：${result.successCount || 0} 条，跳过空行 ${result.skippedRows || 0} 条`,
    );
    await fetchAllWaitPieces();
  } finally {
    hideLoading();
    manualPieceImporting.value = false;
  }
}

function handleManualPieceImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  Modal.confirm({
    cancelText: '取消',
    content: `系统将先校验 ${file.name} 的全部数据，任意一行失败都不会写入。`,
    okText: '确认导入',
    onOk: () => importManualPieceFile(file),
    title: '确认批量导入历史片',
  });
}

function pieceRecordStatusText(status?: string) {
  const map: Record<string, string> = {
    ALLOCATED: '已配货',
    AVAILABLE: '已入库可用',
    CONFIRMED: '已确认报工',
    INBOUND_LOCKED: '已包装待上架',
    INBOUNDED: '已入库',
    OUTBOUND_LOCKED: '出库锁定',
    PACKED: '已包装待上架',
    REPORTED: '历史报工片',
    SUBMITTED: '已提交报工',
    WAIT_PACKAGING: '待包装',
  };
  return map[status || ''] || status || '-';
}

function isPiecePrintCandidateSelected(label: PieceLabel) {
  const key = piecePrintTicketId(label);
  return pieceLabels.value.some((item) => piecePrintTicketId(item) === key);
}

function setPiecePrintCandidateSelected(label: PieceLabel, selected: boolean) {
  const key = piecePrintTicketId(label);
  if (!selected) {
    pieceLabels.value = pieceLabels.value.filter(
      (item) => piecePrintTicketId(item) !== key,
    );
    return;
  }
  if (isPiecePrintCandidateSelected(label)) return;
  pieceLabels.value = [...pieceLabels.value, label];
}

function selectCurrentPiecePrintCandidatePage() {
  piecePrintCandidates.value
    .filter((item) => !isPiecePrintCandidateSelected(item))
    .forEach((item) => setPiecePrintCandidateSelected(item, true));
}

function clearPiecePrintSelection() {
  pieceLabels.value = [];
}

async function loadPiecePrintCandidates() {
  piecePrintLoading.value = true;
  try {
    const result = await getPackagingPieceLabelCandidatePage({
      ...piecePrintFilters,
      pageNo: piecePrintCandidatePage.pageNo,
      pageSize: piecePrintCandidatePage.pageSize,
    });
    piecePrintCandidates.value = result.list || [];
    piecePrintCandidatePage.total = Number(result.total || 0);
  } finally {
    piecePrintLoading.value = false;
  }
}

function resetPiecePrintCandidatePage() {
  piecePrintCandidatePage.pageNo = 1;
  void loadPiecePrintCandidates();
}

function changePiecePrintCandidatePage(pageNo: number, pageSize: number) {
  piecePrintCandidatePage.pageNo =
    pageSize === piecePrintCandidatePage.pageSize ? pageNo : 1;
  piecePrintCandidatePage.pageSize = pageSize;
  void loadPiecePrintCandidates();
}

async function preloadPiecePrintLabel(sliceBatchNo: string) {
  try {
    const result = await getPackagingPieceLabels([sliceBatchNo]);
    const label = result.labels?.[0];
    if (label) {
      setPiecePrintCandidateSelected(label, true);
      return;
    }
    message.warning(result.failures?.[0] || `未找到片号 ${sliceBatchNo}`);
  } catch (error: any) {
    message.warning(error?.message || `未能加载片号 ${sliceBatchNo}`);
  }
}

function openPiecePrintModal(sliceBatchNo?: string) {
  Object.assign(piecePrintFilters, {
    businessStatus: '',
    pageNo: 1,
    pageSize: piecePrintCandidatePage.pageSize,
    sliceBatchNo: '',
    sourceType: '',
  });
  piecePrintCandidatePage.pageNo = 1;
  piecePrintCandidatePage.total = 0;
  pieceLabels.value = [];
  piecePrintCandidates.value = [];
  piecePrintVisible.value = true;
  void loadPiecePrintCandidates();
  const normalizedSliceBatchNo = String(sliceBatchNo || '')
    .trim()
    .toUpperCase();
  if (normalizedSliceBatchNo) {
    void preloadPiecePrintLabel(normalizedSliceBatchNo);
  }
}

function removePiecePrintLabel(label: PieceLabel) {
  pieceLabels.value = pieceLabels.value.filter(
    (item) =>
      !(
        item.sourceType === label.sourceType && item.sourceId === label.sourceId
      ),
  );
}

function piecePrintTicketId(label: PieceLabel) {
  return `${label.sourceType}-${label.sourceId}`;
}

function groupPieceLabelsByTemplate(
  labels: PieceLabel[],
  printQualityStatus?: PackagingQualityStatus,
) {
  const groups = new Map<string, PieceLabel[]>();
  labels.forEach((label) => {
    const templateCode = resolvePackagingPieceLabelTemplateCode(
      printQualityStatus ?? label.qualityStatus,
    );
    groups.set(templateCode, [...(groups.get(templateCode) || []), label]);
  });
  return groups;
}

async function sendPiecePrintToAgent(
  labels: PieceLabel[],
  templateCode: string,
) {
  const printTime = buildNowText();
  const payload = await buildPackagingPieceLabelPayload(
    labels.map((label) => ({
      expiryDate: label.expiryDate,
      modelCode: label.modelCode,
      segmentBatchNo: label.segmentBatchNo,
      sliceBatchNo: label.sliceBatchNo,
      ticketId: piecePrintTicketId(label),
    })),
    printTime,
    { templateCode },
  );
  const endpoint =
    labels.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return { payload, printTime, result };
}

function buildPieceLabelContentSnapshot(
  payload: any,
  ticketId: string,
  printTime: string,
  templateCode: string,
) {
  const item = Array.isArray(payload?.items)
    ? payload.items.find((candidate: any) => candidate?.ticketId === ticketId)
    : payload;
  return JSON.stringify({
    fields: item?.fields || [],
    printTime,
    qrBottomText: item?.qrBottomText || printTime,
    qrTopText: item?.qrTopText || '',
    qrValue: item?.qrValue || '',
    documentTitle: item?.title || payload?.title || '',
    templateCode,
  });
}

async function dispatchPieceLabels(
  labels: PieceLabel[],
  manageLoading = true,
  printQualityStatus?: PackagingQualityStatus,
) {
  if (labels.length === 0) {
    message.warning('请先从候选列表选择可打印片号');
    return new Set<string>();
  }
  if (manageLoading) {
    piecePrinting.value = true;
  }
  let acceptedCount = 0;
  const acceptedTicketIds = new Set<string>();
  const failures: string[] = [];
  const writeBackFailures: string[] = [];
  try {
    for (const [templateCode, templateLabels] of groupPieceLabelsByTemplate(
      labels,
      printQualityStatus,
    )) {
      for (
        let start = 0;
        start < templateLabels.length;
        start += PRINT_BATCH_MAX_COUNT
      ) {
        const batch = templateLabels.slice(
          start,
          start + PRINT_BATCH_MAX_COUNT,
        );
        const { payload, printTime, result } = await sendPiecePrintToAgent(
          batch,
          templateCode,
        );
        const itemResults = Array.isArray(result?.itemResults)
          ? result.itemResults
          : [];
        const successfulLabels = batch.filter((label) => {
          const itemResult = itemResults.find(
            (item: any) => item.ticketId === piecePrintTicketId(label),
          );
          return itemResult
            ? itemResult.success === true && itemResult.accepted !== false
            : result?.success === true;
        });
        acceptedCount += successfulLabels.length;
        successfulLabels.forEach((label) =>
          acceptedTicketIds.add(piecePrintTicketId(label)),
        );
        if (successfulLabels.length > 0) {
          try {
            await markPackagingPieceLabelsPrinted({
              items: successfulLabels.map((label) => ({
                labelContentJson: buildPieceLabelContentSnapshot(
                  payload,
                  piecePrintTicketId(label),
                  printTime,
                  templateCode,
                ),
                operatorName: currentUserName.value,
                printerName: result?.printerName || 'slitting',
                sourceId: label.sourceId,
                sourceType: label.sourceType,
              })),
            });
          } catch (error: any) {
            writeBackFailures.push(
              `${successfulLabels.map((label) => label.sliceBatchNo).join('、')}：${error?.message || error}`,
            );
          }
        }
        itemResults
          .filter(
            (item: any) => item?.success !== true || item?.accepted === false,
          )
          .forEach((item: any) =>
            failures.push(
              item?.message || `标签 ${item?.ticketId || '-'} 未进入打印队列`,
            ),
          );
      }
    }
    if (acceptedCount === 0) {
      throw new Error(failures[0] || '本机打印服务未确认任何标签进入打印队列');
    }
    const failedCount = labels.length - acceptedCount;
    if (
      failedCount > 0 ||
      failures.length > 0 ||
      writeBackFailures.length > 0
    ) {
      Modal.warning({
        content: [
          `已发送 ${acceptedCount} 张标签；另有 ${failedCount} 张未完成。${failures[0] || ''}`,
          writeBackFailures.length > 0
            ? `其中 ${writeBackFailures.length} 个批次的打印记录回写失败；标签已下发，请勿重复打印。${writeBackFailures[0]}`
            : '',
        ]
          .filter(Boolean)
          .join('\n'),
        title: '片号批量打印部分完成',
      });
    } else {
      message.success(`已发送 ${acceptedCount} 张片号标签`);
    }
  } catch (error: any) {
    Modal.warning({
      content:
        acceptedCount > 0
          ? `已有 ${acceptedCount} 张标签进入打印队列；${error?.message || error}`
          : `未能完成片号标签打印：${error?.message || error}。请确认 HC-MES-PrintAgent 已启动且分切标签打印机配置正确。`,
      title: acceptedCount > 0 ? '片号批量打印部分完成' : '片号打印失败',
    });
  } finally {
    if (manageLoading) {
      piecePrinting.value = false;
    }
  }
  return acceptedTicketIds;
}

async function printPieceLabels() {
  const labels = [...pieceLabels.value];
  const acceptedTicketIds = await dispatchPieceLabels(labels);
  if (acceptedTicketIds.size > 0) {
    pieceLabels.value = labels.filter(
      (label) => !acceptedTicketIds.has(piecePrintTicketId(label)),
    );
  }
}

function buildRuntimeInitialParams(
  form: MesHcStationFormApi.StationForm,
  action: PackagingProcessFormAction,
) {
  const recordDate = dayjs().format('YYYY-MM-DD');
  const formType = stationFormType(form, action);
  const formTypeName = stationFormTypeName(form, action);
  return {
    checkerName: currentUserName.value,
    fillUserName: currentUserName.value,
    formType,
    formTypeName,
    materialCode: '',
    modelCode: 'COMMON',
    month: dayjs(recordDate).format('YYYY-MM'),
    packagingType: 'INNER',
    packagingTypeName: '内包装',
    processCode: PACKAGING_PROCESS_CODE,
    processName: PACKAGING_PROCESS_NAME,
    productionDate: recordDate,
    recordDate,
    recordUserName: currentUserName.value,
    recorder: currentUserName.value,
    recorderName: currentUserName.value,
    workshop: '内包装',
  };
}

function buildRuntimeDetailRows(record?: MesHcProcessFormApi.Record | null) {
  return (record?.items || []).map((item, index) => ({
    actualValue: firstText(item.actualValue),
    actualValue2: firstText(item.actualValue2),
    abnormalRemark: firstText(item.abnormalRemark),
    fieldLabel: firstText(item.fieldLabel),
    id: item.id || index + 1,
    itemCategory: firstText(item.itemCategory),
    itemName: firstText(item.fieldLabel, item.fieldKey),
    resultFlag: firstText(item.resultFlag, 'OK'),
    seq: item.itemSeq || index + 1,
    sortNo: item.itemSeq || index + 1,
    standardText: firstText(item.standardText, '-'),
    stepNode: firstText(item.stepNode),
    templateItemId: item.templateItemId,
    valueMode: firstText(item.valueMode, 'TEXT'),
  }));
}

function buildRuntimeHeaderData(record?: MesHcProcessFormApi.Record | null) {
  const header = {
    ...(safeParseJson<Record<string, any>>(record?.headerDataJson) || {}),
  };
  header.formType = firstText(header.formType, record?.formType);
  header.formTypeName = firstText(header.formTypeName, record?.formTypeName);
  header.processCode = firstText(
    header.processCode,
    record?.processCode,
    PACKAGING_PROCESS_CODE,
  );
  header.processName = firstText(
    header.processName,
    record?.processName,
    PACKAGING_PROCESS_NAME,
  );
  header.recordDate = firstText(header.recordDate, record?.recordDate);
  if (!Array.isArray(header.previewDetails)) {
    header.previewDetails = buildRuntimeDetailRows(record);
  }
  return header;
}

function applyRuntimeUser(
  header: Record<string, any>,
  options: { confirmer?: boolean; recorder?: boolean },
) {
  if (options.recorder) {
    header.recorder = firstText(header.recorder, currentUserName.value);
    header.recorderName = firstText(header.recorderName, currentUserName.value);
    header.recordUserName = firstText(
      header.recordUserName,
      currentUserName.value,
    );
    header.checkerName = firstText(header.checkerName, currentUserName.value);
    header.fillUserName = firstText(header.fillUserName, currentUserName.value);
  }
  if (options.confirmer) {
    header.confirmer = currentUserName.value;
    header.confirmerName = currentUserName.value;
    header.confirmUserName = currentUserName.value;
  }
  return header;
}

async function loadRuntimeSchemaForRecord(record: MesHcProcessFormApi.Record) {
  const context = safeParseJson<Record<string, any>>(record.contextJson) || {};
  if (context.runtimeSchema?.runtimeLayout) {
    return context.runtimeSchema as Record<string, any>;
  }
  try {
    const form = await loadStationTemplate(resolveProcessFormAction(record));
    return safeParseJson<Record<string, any>>(form.schemaJson) || {};
  } catch {
    return context.runtimeSchema || {};
  }
}

async function refreshStartupCheckGrid() {
  await startupCheckGridApi.query();
}

async function refreshCleaningCheckGrid() {
  await cleaningCheckGridApi.query();
}

async function refreshProcessRecordGrid(action: PackagingProcessFormAction) {
  if (action === 'STARTUP') {
    await refreshStartupCheckGrid();
    return;
  }
  await refreshCleaningCheckGrid();
}

function hidePackagingRecordTabs() {
  showPackagingRecordTabs.value = false;
  if (EXTENDED_RECORD_TABS.includes(String(activeTab.value))) {
    activeTab.value = WAIT_QUALIFIED_TAB;
  }
}

function packagingDailyCheckCardStatus(
  record?: MesHcProcessFormApi.Record,
): PackagingDailyCheckCardStatus {
  if (!record) {
    return 'UNFILLED';
  }
  return record.recordStatus === 'CONFIRMED' ? 'CONFIRMED' : 'PENDING_CONFIRM';
}

function packagingDailyCheckCardStatusText(
  status: PackagingDailyCheckCardStatus,
) {
  const map: Record<PackagingDailyCheckCardStatus, string> = {
    CONFIRMED: '已确认',
    PENDING_CONFIRM: '待确认',
    UNFILLED: '未填写',
  };
  return map[status];
}

function packagingDailyCheckCardStatusColor(
  status: PackagingDailyCheckCardStatus,
) {
  const map: Record<PackagingDailyCheckCardStatus, string> = {
    CONFIRMED: 'green',
    PENDING_CONFIRM: 'gold',
    UNFILLED: 'orange',
  };
  return map[status];
}

function selectPackagingDailyCheckRecord(
  records: MesHcProcessFormApi.Record[],
) {
  return records.find((record) => record.recordStatus !== 'VOID');
}

async function loadPackagingDailyCheckCards() {
  packagingDailyCheckLoading.value = true;
  try {
    const recordDate = dayjs().format('YYYY-MM-DD');
    const pages = await Promise.all(
      PACKAGING_DAILY_CHECK_ACTIONS.map((action) =>
        getProcessFormRecordPage({
          formType: PACKAGING_PROCESS_FORMS[action].formType,
          pageNo: 1,
          pageSize: 100,
          processCode: PACKAGING_PROCESS_CODE,
          recordDate,
        }),
      ),
    );
    packagingDailyCheckCards.value = PACKAGING_DAILY_CHECK_ACTIONS.map(
      (action, index) => {
        const record = selectPackagingDailyCheckRecord(
          (pages[index]?.list || []) as MesHcProcessFormApi.Record[],
        );
        return {
          action,
          record,
          status: packagingDailyCheckCardStatus(record),
          timing: PACKAGING_DAILY_CHECK_CARD_CONFIG[action].timing,
          title: PACKAGING_DAILY_CHECK_CARD_CONFIG[action].title,
        };
      },
    );
  } finally {
    packagingDailyCheckLoading.value = false;
  }
}

async function openPackagingDailyCheckDialog() {
  hidePackagingRecordTabs();
  packagingDailyCheckDialogVisible.value = true;
  try {
    await loadPackagingDailyCheckCards();
  } catch (error: any) {
    message.error(error?.message || '加载今日点检记录失败');
  }
}

function openPackagingDailyCheckCard(
  card: PackagingDailyCheckCard,
  mode: ProcessFormEditorMode = 'edit',
) {
  void openProcessFormEditor(card.action, card.record, mode);
}

async function openProcessFormEditor(
  action: PackagingProcessFormAction,
  row?: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'edit',
) {
  runtimeFillAction.value = action;
  try {
    if (row?.id) {
      await openRuntimeView(row, mode);
      return;
    }

    const stationForm = await loadStationTemplate(action);
    runtimeFillForm.value = stationForm;
    runtimeInitialParams.value = buildRuntimeInitialParams(stationForm, action);
    runtimeFillOpen.value = true;
  } catch (error: any) {
    message.error(error?.message || '加载点检模板失败');
  }
}

async function handleRuntimeFillSaved() {
  if (packagingDailyCheckDialogVisible.value) {
    await loadPackagingDailyCheckCards();
  }
  if (showPackagingRecordTabs.value) {
    await refreshProcessRecordGrid(runtimeFillAction.value);
  }
}

async function openRuntimeView(
  row: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'view',
) {
  if (!row.id) return;
  runtimeViewLoading.value = true;
  runtimeViewVisible.value = true;
  runtimeViewMode.value = mode;
  try {
    const detail = await getProcessFormRecordDetail(row.id);
    runtimeViewRecord.value = detail;
    runtimeViewHeaderData.value = applyRuntimeUser(
      buildRuntimeHeaderData(detail),
      {
        confirmer: false,
        recorder: mode === 'edit' && detail.recordStatus !== 'CONFIRMED',
      },
    );
    runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
  } catch (error: any) {
    runtimeViewVisible.value = false;
    message.error(error?.message || '加载点检表详情失败');
  } finally {
    runtimeViewLoading.value = false;
  }
}

function closeRuntimeView() {
  runtimeViewVisible.value = false;
  runtimeSaveAuthVisible.value = false;
  runtimeConfirmAuthVisible.value = false;
  runtimeConfirmAuthRecord.value = null;
  runtimeViewRecord.value = null;
  runtimeViewHeaderData.value = {};
  runtimeViewSchema.value = {};
  runtimeViewMode.value = 'view';
}

function buildRuntimeUpdatePayload(
  record: MesHcProcessFormApi.Record,
  headerData: Record<string, any>,
  items?: MesHcProcessFormApi.RecordItem[],
) {
  const { template: _template, ...payload } =
    record as MesHcProcessFormApi.Record & { template?: unknown };
  const runtimeItems = items || record.items || [];
  const resultStatus = runtimeItems.some(
    (item) => normalizeResultFlag(item.resultFlag) === 'NG',
  )
    ? 'NG'
    : 'OK';
  return {
    ...payload,
    fillTime: firstText(record.fillTime, buildNowText()),
    fillUserName: firstText(record.fillUserName, currentUserName.value),
    headerDataJson: JSON.stringify(headerData),
    items: runtimeItems,
    processCode: firstText(record.processCode, PACKAGING_PROCESS_CODE),
    processName: firstText(record.processName, PACKAGING_PROCESS_NAME),
    resultStatus,
  } as MesHcProcessFormApi.Record;
}

async function refreshRuntimeViewRecord(id: number) {
  const detail = await getProcessFormRecordDetail(id);
  runtimeViewRecord.value = detail;
  runtimeViewHeaderData.value = buildRuntimeHeaderData(detail);
  runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
  if (packagingDailyCheckDialogVisible.value) {
    await loadPackagingDailyCheckCards();
  }
  if (showPackagingRecordTabs.value) {
    await refreshProcessRecordGrid(resolveProcessFormAction(detail));
  }
  return detail;
}

function authenticatedUserName(userInfo: any) {
  return String(
    userInfo?.empName ||
      userInfo?.nickname ||
      userInfo?.username ||
      userInfo?.empNo ||
      '',
  ).trim();
}

function requestRuntimeSaveAuth() {
  if (!runtimeViewRecord.value?.id) return;
  runtimeSaveAuthVisible.value = true;
}

async function handleRuntimeSaveAuthSuccess(userInfo: any) {
  runtimeSaveAuthVisible.value = false;
  await saveRuntimeView(false, userInfo);
}

async function saveRuntimeView(silent = false, userInfo?: any) {
  const record = runtimeViewRecord.value;
  if (!record?.id) return undefined;
  if (record.recordStatus === 'CONFIRMED') {
    message.warning('已确认的点检表不允许修改');
    return undefined;
  }
  runtimeViewSaving.value = true;
  try {
    const runtimeHeader =
      runtimeRendererRef.value?.getHeaderData?.() ||
      runtimeViewHeaderData.value;
    const recorderName = authenticatedUserName(userInfo);
    if (!recorderName || !userInfo?.userId) {
      message.warning('未识别到认证填写人，请重新进行身份认证');
      return undefined;
    }
    const recordTime = buildNowText();
    runtimeHeader.recorder = recorderName;
    runtimeHeader.recorderName = recorderName;
    runtimeHeader.recordUserName = recorderName;
    runtimeHeader.checkerName = recorderName;
    runtimeHeader.fillUserName = recorderName;
    runtimeHeader.recorderTime = recordTime;
    runtimeHeader.recordTime = recordTime;
    runtimeHeader.checkerTime = recordTime;
    const runtimeItems =
      runtimeRendererRef.value?.buildRecordItems?.() || record.items || [];
    const payload = buildRuntimeUpdatePayload(
      record,
      runtimeHeader,
      runtimeItems,
    );
    payload.fillUserId = Number(userInfo.userId);
    payload.fillUserName = recorderName;
    payload.fillTime = recordTime;
    await updateProcessFormRecord(payload);
    if (!silent) {
      message.success('保存成功');
    }
    await refreshRuntimeViewRecord(record.id);
    return record.id;
  } finally {
    runtimeViewSaving.value = false;
  }
}

async function executeConfirmRuntimeRecord(
  record: MesHcProcessFormApi.Record,
  userInfo: any,
) {
  if (!record.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该点检表已确认');
    return;
  }
  const confirmedRecordId = record.id!;
  const confirmerId = Number(userInfo?.userId || 0);
  const confirmerName = authenticatedUserName(userInfo);
  if (!confirmerId || !confirmerName) {
    message.warning('未识别到认证确认人，请重新进行身份认证');
    return;
  }
  runtimeViewConfirming.value = true;
  try {
    const detail = await getProcessFormRecordDetail(confirmedRecordId);
    const isCurrentEditingRecord =
      runtimeViewRecord.value?.id === confirmedRecordId;
    const header = isCurrentEditingRecord
      ? runtimeRendererRef.value?.getHeaderData?.() ||
        runtimeViewHeaderData.value
      : buildRuntimeHeaderData(detail);
    const items = isCurrentEditingRecord
      ? runtimeRendererRef.value?.buildRecordItems?.() || detail.items || []
      : detail.items || [];
    applyRuntimeUser(header, { confirmer: true, recorder: false });
    header.confirmer = confirmerName;
    header.confirmerName = confirmerName;
    header.confirmUserName = confirmerName;
    header.inspectionResult = firstText(
      header.inspectionResult,
      detail.resultStatus,
      'OK',
    );
    const confirmTime = buildNowText();
    header.confirmerTime = confirmTime;
    header.confirmTime = confirmTime;
    const payload = buildRuntimeUpdatePayload(detail, header, items);
    payload.confirmUserId = confirmerId;
    payload.confirmUserName = confirmerName;
    payload.confirmTime = confirmTime;
    await updateProcessFormRecord(payload);
    await confirmProcessFormRecordBySigner({
      confirmUserId: confirmerId,
      id: confirmedRecordId,
    });
    message.success('确认成功');
    if (runtimeViewRecord.value?.id === confirmedRecordId) {
      await refreshRuntimeViewRecord(confirmedRecordId);
      runtimeViewMode.value = 'view';
    } else {
      if (packagingDailyCheckDialogVisible.value) {
        await loadPackagingDailyCheckCards();
      }
      if (showPackagingRecordTabs.value) {
        await refreshProcessRecordGrid(resolveProcessFormAction(detail));
      }
    }
  } finally {
    runtimeViewConfirming.value = false;
  }
}

function confirmRuntimeRecord(record: MesHcProcessFormApi.Record) {
  if (!record.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该点检表已确认');
    return;
  }
  runtimeConfirmAuthRecord.value = record;
  runtimeConfirmAuthVisible.value = true;
}

async function handleRuntimeConfirmAuthSuccess(userInfo: any) {
  const record = runtimeConfirmAuthRecord.value;
  runtimeConfirmAuthVisible.value = false;
  runtimeConfirmAuthRecord.value = null;
  if (!record) return;
  await executeConfirmRuntimeRecord(record, userInfo);
}

function openPackagingCheckRecord(
  row: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'edit',
) {
  void openProcessFormEditor(resolveProcessFormAction(row), row, mode);
}

function confirmPackagingCheckRow(row: MesHcProcessFormApi.Record) {
  confirmRuntimeRecord(row);
}

async function fetchWaitPieces() {
  if (activeTab.value === WAIT_FROZEN_TAB) {
    await waitFrozenGridApi.query();
    return;
  }
  if (activeTab.value === WAIT_UNQUALIFIED_TAB) {
    await waitUnqualifiedGridApi.query();
    return;
  }
  await waitQualifiedGridApi.query();
}

async function fetchAllWaitPieces() {
  await Promise.all([
    waitQualifiedGridApi.query(),
    waitUnqualifiedGridApi.query(),
    waitFrozenGridApi.query(),
  ]);
}

async function fetchPackages() {
  await packageGridApi.query();
}

async function fetchStartupChecks() {
  await refreshStartupCheckGrid();
}

async function fetchCleaningChecks() {
  await refreshCleaningCheckGrid();
}

async function refreshAll() {
  const tasks: Array<Promise<void>> = [fetchAllWaitPieces(), fetchPackages()];
  if (showPackagingRecordTabs.value) {
    tasks.push(fetchStartupChecks(), fetchCleaningChecks());
  }
  await Promise.all(tasks);
}

watch(showPackagingRecordTabs, (visible) => {
  if (!visible && EXTENDED_RECORD_TABS.includes(String(activeTab.value))) {
    activeTab.value = WAIT_QUALIFIED_TAB;
  }
  if (visible) {
    void nextTick(() =>
      Promise.all([fetchStartupChecks(), fetchCleaningChecks()]),
    );
  }
});

watch(activeTab, (tab, oldTab) => {
  if (
    tab === oldTab ||
    ![WAIT_QUALIFIED_TAB, WAIT_FROZEN_TAB, WAIT_UNQUALIFIED_TAB].includes(String(tab))
  ) {
    return;
  }
  selectedRowKeys.value = [];
  selectedRows.value = [];
});

function resetPackageAuxForm() {
  packageForm.auxConsumeItems = [{}];
}

function formatPackageAuxQty(value?: number) {
  const qty = Number(value || 0);
  return Number.isFinite(qty) ? qty.toFixed(3) : '-';
}

function getPackageAuxAvailableQty(ledgerId?: number) {
  const balance = packageAuxBalances.value.find(
    (item) => Number(item.ledgerId) === Number(ledgerId),
  );
  if (!balance) {
    return '-';
  }
  return `${formatPackageAuxQty(balance.balanceQty)} ${balance.uomName || balance.uom || '个'}`;
}

async function loadPackageAuxStocks() {
  packageAuxLoading.value = true;
  try {
    const page = await getToolingConsumableBalancePage({
      onlyPositiveBalance: true,
      pageNo: 1,
      pageSize: 200,
      processCode: PACKAGING_PROCESS_CODE,
      usageStatus: 'ACTIVE',
    });
    packageAuxBalances.value = page.list || [];
  } catch {
    packageAuxBalances.value = [];
    message.error('加载包装工序耗材领用台账失败，请稍后重试');
  } finally {
    packageAuxLoading.value = false;
  }
}

function buildPackageAuxConsumeItems() {
  if (packageForm.auxConsumeItems.length === 0) {
    message.warning('请至少选择一条包装辅材');
    return undefined;
  }
  const usedLedgerIds = new Set<number>();
  const items: Array<{ consumeQty: number; ledgerId: number }> = [];
  for (const item of packageForm.auxConsumeItems) {
    const ledgerId = Number(item.ledgerId || 0);
    if (ledgerId <= 0) {
      message.warning('请完整选择包装辅材批次');
      return undefined;
    }
    if (usedLedgerIds.has(ledgerId)) {
      message.warning('同一包装辅材批次只能填写一行，请合并领用数量');
      return undefined;
    }
    usedLedgerIds.add(ledgerId);
    const consumeQty = Number(item.consumeQty);
    if (!Number.isFinite(consumeQty) || consumeQty <= 0) {
      message.warning('请填写大于 0 的包装辅材本次领用量');
      return undefined;
    }
    items.push({ consumeQty, ledgerId });
  }
  return items;
}

function isWaitGroupRow(row?: InspectionSliceDisplayRow) {
  return !!row?.__group;
}

function isWaitPieceRow(
  row?: InspectionSliceDisplayRow,
): row is InspectionSliceDisplayRow & InspectionSlice {
  return (
    !isWaitGroupRow(row) &&
    (Number(row?.sourceCutRoundReportId) > 0 ||
      Number(row?.sourceManualPieceId) > 0)
  );
}

function getWaitPieceKey(row?: Partial<InspectionSlice>) {
  if (
    Number(row?.sourceManualPieceId) > 0 ||
    row?.sourceType === SOURCE_MANUAL_HISTORY
  ) {
    return `${SOURCE_MANUAL_HISTORY}:${Number(row?.sourceManualPieceId || 0)}`;
  }
  return `${SOURCE_CUT_ROUND_REPORT}:${Number(row?.sourceCutRoundReportId || 0)}`;
}

function getWaitPieceSourceType(row?: Partial<InspectionSlice>) {
  return Number(row?.sourceManualPieceId) > 0 ||
    row?.sourceType === SOURCE_MANUAL_HISTORY
    ? SOURCE_MANUAL_HISTORY
    : SOURCE_CUT_ROUND_REPORT;
}

function getWaitPackageSegmentKey(row?: Partial<InspectionSliceDisplayRow>) {
  return (
    normalizePackageSegmentNo(
      row?.segmentBatchNo || row?.groupKey || row?.parentProductionBatchNo,
    ) ||
    String(
      row?.segmentBatchNo ||
        row?.groupKey ||
        row?.parentProductionBatchNo ||
        '',
    )
  );
}

function syncSelectedPackagingRows(rows: InspectionSlice[]) {
  const uniqueRows = new Map<string, InspectionSlice>();
  rows.forEach((row) => {
    if (
      Number(row.sourceCutRoundReportId) > 0 ||
      Number(row.sourceManualPieceId) > 0
    ) {
      uniqueRows.set(getWaitPieceKey(row), row);
    }
  });
  selectedRows.value = Array.from(uniqueRows.values());
  selectedRowKeys.value = selectedRows.value.map((row) => getWaitPieceKey(row));
}

function isWaitPiecePackageSelected(row: InspectionSliceDisplayRow) {
  return (
    isWaitPieceRow(row) && selectedRowKeys.value.includes(getWaitPieceKey(row))
  );
}

function getWaitSegmentPackageSelectedCount(row: InspectionSliceDisplayRow) {
  const segmentKey = getWaitPackageSegmentKey(row);
  if (!segmentKey) return 0;
  return selectedRows.value.filter(
    (item) => getWaitPackageSegmentKey(item) === segmentKey,
  ).length;
}

function isWaitSegmentPackageSelected(row: InspectionSliceDisplayRow) {
  const total = Number(row.groupCount || 0);
  return total > 0 && getWaitSegmentPackageSelectedCount(row) >= total;
}

function setWaitPiecePackageSelected(
  row: InspectionSliceDisplayRow,
  selected: boolean,
) {
  if (!isWaitPieceRow(row)) return;
  const rowKey = getWaitPieceKey(row);
  if (!selected) {
    syncSelectedPackagingRows(
      selectedRows.value.filter((item) => getWaitPieceKey(item) !== rowKey),
    );
    return;
  }
  if (selectedRowKeys.value.includes(rowKey)) return;

  const selectedSourceType =
    selectedRows.value[0] && getWaitPieceSourceType(selectedRows.value[0]);
  if (
    selectedSourceType &&
    selectedSourceType !== getWaitPieceSourceType(row)
  ) {
    message.warning('历史补录片与正常报工片不能混合包装，请分别选择');
    return;
  }
  const segmentKey = getWaitPackageSegmentKey(row);
  const selectedSegmentKey =
    selectedRows.value[0] && getWaitPackageSegmentKey(selectedRows.value[0]);
  if (selectedSegmentKey && selectedSegmentKey !== segmentKey) {
    message.warning('不同段号的片号不能包装在一起，请先取消当前已选片号');
    return;
  }
  syncSelectedPackagingRows([...selectedRows.value, row]);
}

async function setWaitSegmentPackageSelected(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
  selected: boolean,
) {
  if (!isWaitGroupRow(row)) return;
  waitSegmentPackageSelecting.value = true;
  try {
    const pieces = (
      await loadWaitSegmentChildren(row, packagingQualityStatus)
    ).filter(isWaitPieceRow);
    const pieceKeys = new Set(pieces.map((item) => getWaitPieceKey(item)));
    if (!selected) {
      syncSelectedPackagingRows(
        selectedRows.value.filter(
          (item) => !pieceKeys.has(getWaitPieceKey(item)),
        ),
      );
      return;
    }
    if (pieces.length === 0) {
      message.warning('当前段没有可打包片号，请刷新后重试');
      return;
    }
    const sourceTypes = new Set(
      pieces.map((item) => getWaitPieceSourceType(item)),
    );
    if (sourceTypes.size > 1) {
      message.warning(
        '本段同时存在历史补录片与正常报工片，请展开后按来源分别选择',
      );
      return;
    }
    const segmentKey = getWaitPackageSegmentKey(row);
    const selectedSegmentKey =
      selectedRows.value[0] && getWaitPackageSegmentKey(selectedRows.value[0]);
    if (selectedSegmentKey && selectedSegmentKey !== segmentKey) {
      message.warning('不同段号的片号不能包装在一起，请先取消当前已选片号');
      return;
    }
    const selectedSourceType =
      selectedRows.value[0] && getWaitPieceSourceType(selectedRows.value[0]);
    if (selectedSourceType && !sourceTypes.has(selectedSourceType)) {
      message.warning('历史补录片与正常报工片不能混合包装，请分别选择');
      return;
    }
    syncSelectedPackagingRows([...selectedRows.value, ...pieces]);
  } catch (error: any) {
    message.error(error?.message || '加载分段待包装片失败，请稍后重试');
  } finally {
    waitSegmentPackageSelecting.value = false;
  }
}

function getWaitPrintRows(packagingQualityStatus: PackagingQualityStatus) {
  if (packagingQualityStatus === 'FROZEN') return frozenWaitPrintRows.value;
  return packagingQualityStatus === 'OK'
    ? qualifiedWaitPrintRows.value
    : unqualifiedWaitPrintRows.value;
}

function setWaitPrintRows(
  packagingQualityStatus: PackagingQualityStatus,
  rows: InspectionSlice[],
) {
  if (packagingQualityStatus === 'FROZEN') {
    frozenWaitPrintRows.value = rows;
    return;
  }
  if (packagingQualityStatus === 'OK') {
    qualifiedWaitPrintRows.value = rows;
    return;
  }
  unqualifiedWaitPrintRows.value = rows;
}

function getWaitPrintSegmentKey(row?: Partial<InspectionSliceDisplayRow>) {
  return (
    normalizePackageSegmentNo(
      row?.segmentBatchNo || row?.groupKey || row?.parentProductionBatchNo,
    ) ||
    String(
      row?.segmentBatchNo ||
        row?.groupKey ||
        row?.parentProductionBatchNo ||
        '',
    )
  );
}

function isWaitPiecePrintSelected(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
) {
  if (!isWaitPieceRow(row)) return false;
  const key = getWaitPieceKey(row);
  return getWaitPrintRows(packagingQualityStatus).some(
    (item) => getWaitPieceKey(item) === key,
  );
}

function setWaitPiecePrintSelected(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
  selected: boolean,
) {
  if (!isWaitPieceRow(row)) return;
  const key = getWaitPieceKey(row);
  const rows = getWaitPrintRows(packagingQualityStatus);
  if (!selected) {
    setWaitPrintRows(
      packagingQualityStatus,
      rows.filter((item) => getWaitPieceKey(item) !== key),
    );
    return;
  }
  if (!rows.some((item) => getWaitPieceKey(item) === key)) {
    setWaitPrintRows(packagingQualityStatus, [...rows, row]);
  }
}

function getWaitSegmentPrintSelectedCount(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
) {
  const segmentKey = getWaitPrintSegmentKey(row);
  if (!segmentKey) return 0;
  return getWaitPrintRows(packagingQualityStatus).filter(
    (item) => getWaitPrintSegmentKey(item) === segmentKey,
  ).length;
}

function isWaitSegmentPrintSelected(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
) {
  const total = Number(row.groupCount || 0);
  return (
    total > 0 &&
    getWaitSegmentPrintSelectedCount(row, packagingQualityStatus) >= total
  );
}

async function setWaitSegmentPrintSelected(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
  selected: boolean,
) {
  if (!isWaitGroupRow(row)) return;
  waitSegmentPrintSelecting.value = true;
  try {
    const pieces = (
      await loadWaitSegmentChildren(row, packagingQualityStatus)
    ).filter(isWaitPieceRow);
    const pieceKeys = new Set(pieces.map((item) => getWaitPieceKey(item)));
    const rows = getWaitPrintRows(packagingQualityStatus);
    if (!selected) {
      setWaitPrintRows(
        packagingQualityStatus,
        rows.filter((item) => !pieceKeys.has(getWaitPieceKey(item))),
      );
      return;
    }
    const existingKeys = new Set(rows.map((item) => getWaitPieceKey(item)));
    setWaitPrintRows(packagingQualityStatus, [
      ...rows,
      ...pieces.filter((item) => !existingKeys.has(getWaitPieceKey(item))),
    ]);
  } catch (error: any) {
    message.error(error?.message || '加载分段待包装片失败，请稍后重试');
  } finally {
    waitSegmentPrintSelecting.value = false;
  }
}

function clearWaitPrintSelection(
  packagingQualityStatus: PackagingQualityStatus,
) {
  setWaitPrintRows(packagingQualityStatus, []);
}

async function printSelectedWaitPieceLabels(
  packagingQualityStatus: PackagingQualityStatus,
  printQualityStatus?: PackagingQualityStatus,
) {
  if (piecePrinting.value) return;
  const selectedRows = getWaitPrintRows(packagingQualityStatus);
  if (selectedRows.length === 0) {
    message.warning('请先选择本段内需要打印的片号');
    return;
  }
  const selectedKeys = new Set(
    selectedRows.map((item) => getWaitPieceKey(item)),
  );
  const sliceBatchNos = Array.from(
    new Set(
      selectedRows
        .map((item) =>
          String(item.productionBatchNo || item.sliceBatchNo || '').trim(),
        )
        .filter(Boolean),
    ),
  );
  const labels: PieceLabel[] = [];
  const failures: string[] = [];
  piecePrinting.value = true;
  try {
    for (
      let start = 0;
      start < sliceBatchNos.length;
      start += PRINT_BATCH_MAX_COUNT
    ) {
      const result = await getPackagingPieceLabels(
        sliceBatchNos.slice(start, start + PRINT_BATCH_MAX_COUNT),
      );
      labels.push(
        ...(result.labels || []).filter((item) =>
          selectedKeys.has(
            item.sourceType === SOURCE_MANUAL_HISTORY
              ? `${SOURCE_MANUAL_HISTORY}:${item.sourceId}`
              : `${SOURCE_CUT_ROUND_REPORT}:${item.sourceId}`,
          ),
        ),
      );
      failures.push(...(result.failures || []));
    }
    if (failures.length > 0 || labels.length !== selectedRows.length) {
      Modal.warning({
        content: `已选择 ${selectedRows.length} 张片号，但未获取完整标签数据：${failures[0] || '部分选择项已变化'}。为避免漏打，本次未向打印机提交任务。`,
        title: '片号标签未下发',
      });
      return;
    }
    // 打印模板独立于来源页签及片号实际质量状态，不回写品质结论。
    const acceptedTicketIds = await dispatchPieceLabels(
      labels,
      false,
      printQualityStatus,
    );
    if (acceptedTicketIds.size > 0) {
      clearWaitPrintSelection(packagingQualityStatus);
      await fetchWaitPieces().catch((error: any) => {
        message.error(error?.message || '标签已下发，但刷新待包装列表失败');
      });
    }
  } catch (error: any) {
    message.error(error?.message || '加载已选片号标签失败，请稍后重试');
  } finally {
    piecePrinting.value = false;
  }
}

async function resetPackageFormAndOpen() {
  packageForm.packageNo = '';
  packageForm.remark = '';
  await loadPackageAuxStocks();
  const firstAvailableAux = packageAuxBalances.value[0];
  packageForm.auxConsumeItems = firstAvailableAux?.ledgerId
    ? [{ ledgerId: firstAvailableAux.ledgerId }]
    : [{}];
  packageVisible.value = true;
}

async function findMissingPackagingDailyRecords() {
  const recordDate = dayjs().format('YYYY-MM-DD');
  const pages = await Promise.all(
    PACKAGING_DAILY_RECORDS.map((item) =>
      getProcessFormRecordPage({
        formType: item.formType,
        pageNo: 1,
        pageSize: 100,
        processCode: PACKAGING_PROCESS_CODE,
        recordDate,
      }),
    ),
  );
  return PACKAGING_DAILY_RECORDS.filter(
    (_, index) =>
      !(pages[index]?.list || []).some(
        (record) => record.recordStatus !== 'VOID',
      ),
  );
}

async function ensurePackagingDailyRecordsReady() {
  try {
    const missingRecords = await findMissingPackagingDailyRecords();
    if (missingRecords.length === 0) return true;
    Modal.warning({
      content: `今天尚未填写${missingRecords.map((item) => item.label).join('、')}记录，不能确认包装。`,
      okText: '去填写',
      title: '今日点检未完成',
      onOk() {
        packageVisible.value = false;
        void openPackagingDailyCheckDialog();
      },
    });
    return false;
  } catch {
    message.error('无法校验今日开机点检和清洁保养记录，请稍后重试');
    return false;
  }
}

function normalizePackageSegmentNo(value?: string) {
  return String(value || '')
    .trim()
    .toUpperCase()
    .replace(/-J\d+$/, '')
    .replace(/-S\d+$/, '')
    .replace(/^(.+[PQRS])\d{3}[A-Z]?$/, '$1');
}

function resolvePackageSegmentNo(row: Partial<InspectionSlice>) {
  const candidates = [
    row.segmentBatchNo,
    row.parentProductionBatchNo,
    row.coaScopeBatchNo,
    row.productionBatchNo,
    row.sliceBatchNo,
  ];
  let fallback = '';
  for (const candidate of candidates) {
    const normalized = normalizePackageSegmentNo(candidate);
    if (!normalized) {
      continue;
    }
    fallback ||= normalized;
    if (/[PQRS]$/.test(normalized)) {
      return normalized;
    }
  }
  return fallback;
}

function normalizePackageScanCode(value?: string) {
  return resolveTransferTicketQrBusinessNo(value).trim().toUpperCase();
}

function isSamePackageScanPiece(row: InspectionSlice, sliceBatchNo: string) {
  const normalizedSliceBatchNo = normalizePackageScanCode(sliceBatchNo);
  return [row.productionBatchNo, row.sliceBatchNo].some(
    (value) =>
      String(value || '')
        .trim()
        .toUpperCase() === normalizedSliceBatchNo,
  );
}

function resetPackageScan() {
  packageScanCode.value = '';
  packageScanRows.value = [];
}

function openPackageScanModal() {
  resetPackageScan();
  packageScanVisible.value = true;
}

function removePackageScanPiece(row: InspectionSlice) {
  const key = getWaitPieceKey(row);
  packageScanRows.value = packageScanRows.value.filter(
    (item) => getWaitPieceKey(item) !== key,
  );
}

async function confirmPackageScanSelection() {
  if (packageScanRows.value.length === 0) {
    message.warning('请先扫描待包装片号');
    return;
  }
  syncSelectedPackagingRows([...packageScanRows.value]);
  packageScanVisible.value = false;
  await openPackageModal();
}

async function resolvePackageScanCandidates(sliceBatchNo: string) {
  const segments = await getFgInboundWaitSegmentPage({
    keyword: sliceBatchNo,
    pageNo: 1,
    pageSize: 20,
  });
  const segmentBatchNos = Array.from(
    new Set(
      (segments.list || [])
        .map((item) => String(item.segmentBatchNo || '').trim())
        .filter(Boolean),
    ),
  );
  if (segmentBatchNos.length === 0) {
    return [];
  }
  const rows = (
    await Promise.all(
      segmentBatchNos.map((segmentBatchNo) =>
        getFgInboundWaitSegmentPieceList({
          keyword: sliceBatchNo,
          segmentBatchNo,
        }),
      ),
    )
  ).flat();
  const uniqueRows = new Map<string, InspectionSlice>();
  rows
    .filter((row) => isSamePackageScanPiece(row, sliceBatchNo))
    .forEach((row) => {
      uniqueRows.set(getWaitPieceKey(row), row);
    });
  return Array.from(uniqueRows.values());
}

async function scanPackagePiece() {
  const sliceBatchNo = normalizePackageScanCode(packageScanCode.value);
  if (!sliceBatchNo) {
    message.warning('请扫描或输入待包装片号');
    return;
  }
  packageScanResolving.value = true;
  try {
    const candidates = await resolvePackageScanCandidates(sliceBatchNo);
    if (candidates.length === 0) {
      message.warning(`未找到片号 ${sliceBatchNo} 对应的待包装记录`);
      return;
    }
    if (candidates.length > 1) {
      message.warning(
        `片号 ${sliceBatchNo} 命中多条待包装记录，请通过列表手工选择`,
      );
      return;
    }
    const candidate = candidates[0];
    if (
      packageScanRows.value.some(
        (item) => getWaitPieceKey(item) === getWaitPieceKey(candidate),
      )
    ) {
      message.warning(`片号 ${sliceBatchNo} 已扫码，请继续扫描另一片`);
      return;
    }
    const firstRow = packageScanRows.value[0];
    if (firstRow) {
      if (firstRow.sourceType !== candidate.sourceType) {
        message.warning('历史补录片与正常裁切片不能混合包装');
        return;
      }
      if (
        resolvePackageSegmentNo(firstRow) !== resolvePackageSegmentNo(candidate)
      ) {
        message.warning('不同段号的片号不能包装在一起');
        return;
      }
      if (
        resolvePackagingQualityStatus(firstRow) !==
        resolvePackagingQualityStatus(candidate)
      ) {
        message.warning('合格片、冻结片与不合格片不能混合包装');
        return;
      }
      if (
        firstRow.sourceType === SOURCE_MANUAL_HISTORY &&
        (firstRow.materialCode !== candidate.materialCode ||
          firstRow.modelCode !== candidate.modelCode)
      ) {
        message.warning('不同产品料号或型号的历史补录片不能包装在一起');
        return;
      }
    }
    packageScanRows.value = [...packageScanRows.value, candidate];
    packageScanCode.value = '';
    message.success(
      `片号 ${candidate.productionBatchNo || candidate.sliceBatchNo || sliceBatchNo} 已加入批量包装（已选 ${packageScanRows.value.length} 片）`,
    );
  } catch (error: any) {
    message.warning(
      error?.message || `未能解析片号 ${sliceBatchNo}，请稍后重试`,
    );
  } finally {
    packageScanResolving.value = false;
  }
}

function getPackagingSampleLockCandidates(row: Partial<InspectionSlice>) {
  const segmentBatchNo = resolvePackageSegmentNo(row);
  const motherBatchNo =
    row.parentProductionBatchNo ||
    row.coaScopeBatchNo ||
    row.segmentBatchNo ||
    segmentBatchNo;
  return buildSegmentChainSampleLockCandidates({
    motherBatchNo,
    segmentBatchNo,
  });
}

async function ensureSelectedPackagingSampleUnlocked(actionName: string) {
  const candidates = selectedRows.value.flatMap((row) =>
    getPackagingSampleLockCandidates(row),
  );
  if (candidates.length === 0) return true;
  return ensureSampleAbnormalUnlocked(candidates, actionName);
}

function toWaitSegmentRow(
  row: InspectionSliceSegmentWithCoaResult,
  packagingQualityStatus: 'FROZEN' | 'NG' | 'OK',
  index: number,
): InspectionSliceDisplayRow {
  const segmentBatchNo = normalizePackageSegmentNo(
    row.segmentBatchNo || row.sampleSliceBatchNo,
  );
  const groupKey =
    segmentBatchNo ||
    `${packagingQualityStatus}-${row.planOperationId || row.planNo || index}`;
  return {
    __group: true,
    expiryDate: row.expiryDate,
    groupCount: Number(row.totalPieceCount || 0),
    groupKey,
    hasChild: true,
    coaInspectionResult: row.coaInspectionResult,
    inspectionStatus: row.inspectionStatus,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelCode: row.modelCode,
    packagingQualityStatus:
      row.packagingQualityStatus || packagingQualityStatus,
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    productionBatchNo: segmentBatchNo || row.segmentBatchNo,
    productionDate: row.productionDateStart,
    productionDateEnd: row.productionDateEnd,
    productionDateStart: row.productionDateStart,
    sampleSliceBatchNos:
      row.sampleSliceBatchNos ||
      (row.sampleSliceBatchNo ? [row.sampleSliceBatchNo] : []),
    segmentBatchNo: segmentBatchNo || row.segmentBatchNo,
    sourceCutRoundReportId: 0,
    stockStatus: row.stockStatus,
    waitRowKey: `segment-${packagingQualityStatus}-${groupKey}`,
  };
}

function toWaitPieceRow(row: InspectionSlice): InspectionSliceDisplayRow {
  return {
    ...row,
    segmentBatchNo: resolvePackageSegmentNo(row),
    waitRowKey: `piece-${getWaitPieceKey(row)}`,
  };
}

function isInboundPackedSegmentGroupRow(row?: InboundPackageDisplayRow) {
  return !!row?.__group;
}

function toInboundPackedSegmentRow(
  row: InboundPackageSegment,
  index: number,
): InboundPackageDisplayRow {
  const segmentBatchNo =
    normalizePackageSegmentNo(row.segmentBatchNo) ||
    row.segmentBatchNo ||
    `未识别段批次-${index + 1}`;
  return {
    __group: true,
    boxNo: segmentBatchNo,
    currentQty: Number(row.totalPieceCount || 0),
    groupPackageCount: Number(row.packageCount || 0),
    groupPieceCount: Number(row.totalPieceCount || 0),
    hasChild: true,
    id: 0,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelCode: row.modelCode,
    packageRowKey: `packed-segment-${segmentBatchNo}`,
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    sampleSliceBatchNos: row.sampleSliceBatchNos || [],
    segmentBatchNo,
  };
}

function toInboundPackedPackageRow(
  row: InboundPackage,
): InboundPackageDisplayRow {
  const segmentCandidates = [
    row.motherSegmentBatchNo,
    ...(row.items || []).flatMap((item) => [
      item.productionBatchNo,
      item.sliceBatchNo,
    ]),
  ];
  const segmentBatchNo =
    segmentCandidates
      .map(normalizePackageSegmentNo)
      .find((value) => /[PQRS]$/.test(value)) ||
    segmentCandidates.map(normalizePackageSegmentNo).find(Boolean) ||
    '';
  return {
    ...row,
    packageRowKey: `packed-package-${row.id}`,
    segmentBatchNo,
  };
}

function formatInboundPackedSegmentSamples(row: InboundPackageDisplayRow) {
  const samples = (row.sampleSliceBatchNos || []).filter(Boolean);
  return samples.length > 0 ? `示例：${samples.join('、')}` : '展开加载包装单';
}

async function loadInboundPackedSegmentPackages(row: InboundPackageDisplayRow) {
  if (!isInboundPackedSegmentGroupRow(row) || !row.segmentBatchNo) {
    return [];
  }
  const packages = await getFgInboundPackedSegmentPackageList({
    keyword: packageKeyword.value.trim(),
    segmentBatchNo: row.segmentBatchNo,
  });
  return (packages || []).map(toInboundPackedPackageRow);
}

function isPendingPackagePrintRow(
  row?: InboundPackageDisplayRow,
): row is InboundPackageDisplayRow {
  return (
    !isInboundPackedSegmentGroupRow(row) &&
    Number(row?.id) > 0 &&
    ['PACKED', 'INBOUND_LOCKED'].includes(row?.status || '')
  );
}

function getInboundPackagePrintSegmentKey(
  row?: Partial<InboundPackageDisplayRow>,
) {
  return (
    normalizePackageSegmentNo(
      row?.segmentBatchNo || row?.motherSegmentBatchNo,
    ) || String(row?.segmentBatchNo || row?.motherSegmentBatchNo || '')
  );
}

function isPackagePrintSelected(row: InboundPackageDisplayRow) {
  return (
    isPendingPackagePrintRow(row) &&
    selectedPackagePrintRows.value.some(
      (item) => Number(item.id) === Number(row.id),
    )
  );
}

function setPackagePrintSelected(
  row: InboundPackageDisplayRow,
  selected: boolean,
) {
  if (!isPendingPackagePrintRow(row)) return;
  if (!selected) {
    selectedPackagePrintRows.value = selectedPackagePrintRows.value.filter(
      (item) => Number(item.id) !== Number(row.id),
    );
    return;
  }
  if (!isPackagePrintSelected(row)) {
    selectedPackagePrintRows.value = [...selectedPackagePrintRows.value, row];
  }
}

function getPackageSegmentPrintSelectedCount(row: InboundPackageDisplayRow) {
  const segmentKey = getInboundPackagePrintSegmentKey(row);
  if (!segmentKey) return 0;
  return selectedPackagePrintRows.value.filter(
    (item) => getInboundPackagePrintSegmentKey(item) === segmentKey,
  ).length;
}

function isPackageSegmentPrintSelected(row: InboundPackageDisplayRow) {
  const total = Number(row.groupPackageCount || 0);
  return total > 0 && getPackageSegmentPrintSelectedCount(row) >= total;
}

async function setPackageSegmentPrintSelected(
  row: InboundPackageDisplayRow,
  selected: boolean,
) {
  if (!isInboundPackedSegmentGroupRow(row)) return;
  waitSegmentPrintSelecting.value = true;
  try {
    const packages = (await loadInboundPackedSegmentPackages(row)).filter(
      isPendingPackagePrintRow,
    );
    const packageIds = new Set(packages.map((item) => Number(item.id)));
    if (!selected) {
      selectedPackagePrintRows.value = selectedPackagePrintRows.value.filter(
        (item) => !packageIds.has(Number(item.id)),
      );
      return;
    }
    const existingIds = new Set(
      selectedPackagePrintRows.value.map((item) => Number(item.id)),
    );
    selectedPackagePrintRows.value = [
      ...selectedPackagePrintRows.value,
      ...packages.filter((item) => !existingIds.has(Number(item.id))),
    ];
  } catch (error: any) {
    message.error(error?.message || '加载分段待上架包装失败，请稍后重试');
  } finally {
    waitSegmentPrintSelecting.value = false;
  }
}

function clearPackagePrintSelection() {
  selectedPackagePrintRows.value = [];
}

function getInboundPackagePieceSourceKey(item: InboundPackageItem) {
  if (
    Number(item.sourceManualPieceId || 0) > 0 ||
    item.sourceType === SOURCE_MANUAL_HISTORY
  ) {
    return `${SOURCE_MANUAL_HISTORY}:${Number(item.sourceManualPieceId || 0)}`;
  }
  if (
    Number(item.sourceCutRoundReportId || 0) > 0 ||
    item.sourceType === SOURCE_CUT_ROUND_REPORT
  ) {
    return `${SOURCE_CUT_ROUND_REPORT}:${Number(item.sourceCutRoundReportId || 0)}`;
  }
  return '';
}

async function printSelectedPackedPackagePieceLabels() {
  if (piecePrinting.value) return;
  const selectedPackages = [...selectedPackagePrintRows.value];
  if (selectedPackages.length === 0) {
    message.warning('请先在待上架包装段选择需要打印片号的包装单');
    return;
  }
  const selectedSourceKeys = new Set<string>();
  const sliceBatchNos: string[] = [];
  const failures: string[] = [];
  for (const item of selectedPackages.flatMap((row) => row.items || [])) {
    const sourceKey = getInboundPackagePieceSourceKey(item);
    const sliceBatchNo = String(
      item.sliceBatchNo || item.productionBatchNo || '',
    ).trim();
    if (!sourceKey || !sliceBatchNo || sourceKey.endsWith(':0')) {
      failures.push(
        `包装单 ${item.boxNo || '-'} 存在来源不完整的片号，未加入打印`,
      );
      continue;
    }
    if (selectedSourceKeys.has(sourceKey)) continue;
    selectedSourceKeys.add(sourceKey);
    sliceBatchNos.push(sliceBatchNo);
  }
  if (sliceBatchNos.length === 0) {
    message.warning(failures[0] || '所选包装单没有可打印的片号，请刷新后重试');
    return;
  }
  const labels: PieceLabel[] = [];
  piecePrinting.value = true;
  try {
    for (
      let start = 0;
      start < sliceBatchNos.length;
      start += PRINT_BATCH_MAX_COUNT
    ) {
      const result = await getPackagingPieceLabels(
        sliceBatchNos.slice(start, start + PRINT_BATCH_MAX_COUNT),
      );
      labels.push(
        ...(result.labels || []).filter((item) =>
          selectedSourceKeys.has(
            item.sourceType === SOURCE_MANUAL_HISTORY
              ? `${SOURCE_MANUAL_HISTORY}:${item.sourceId}`
              : `${SOURCE_CUT_ROUND_REPORT}:${item.sourceId}`,
          ),
        ),
      );
      failures.push(...(result.failures || []));
    }
    if (failures.length > 0 || labels.length !== selectedSourceKeys.size) {
      Modal.warning({
        content: `已选择 ${selectedSourceKeys.size} 张片号，但未获取完整标签数据：${failures[0] || '部分选择项已变化'}。为避免漏打，本次未向打印机提交任务。`,
        title: '片号标签未下发',
      });
      return;
    }
    const acceptedTicketIds = await dispatchPieceLabels(labels, false);
    if (acceptedTicketIds.size > 0) {
      clearPackagePrintSelection();
      await fetchPackages().catch((error: any) => {
        message.error(error?.message || '标签已下发，但刷新待上架包装列表失败');
      });
    }
  } catch (error: any) {
    message.error(error?.message || '加载已选包装片号标签失败，请稍后重试');
  } finally {
    piecePrinting.value = false;
  }
}

function formatWaitProductionDate(row: InspectionSliceDisplayRow) {
  if (!isWaitGroupRow(row)) {
    return row.productionDate || '-';
  }
  const start = row.productionDateStart || row.productionDate;
  const end = row.productionDateEnd;
  if (!start && !end) {
    return '-';
  }
  if (!end || start === end) {
    return start || '-';
  }
  return `${start || '-'} ~ ${end}`;
}

function formatWaitExpiryDate(row: InspectionSliceDisplayRow) {
  return row.expiryDate || '-';
}

function formatWaitSegmentSamples(row: InspectionSliceDisplayRow) {
  const samples = (row.sampleSliceBatchNos || []).filter(Boolean);
  if (samples.length === 0) {
    return '展开加载片号';
  }
  return `示例：${samples.join('、')}`;
}

function buildSelectedSegmentMismatchText() {
  const segmentSliceMap = new Map<string, string[]>();
  for (const row of selectedRows.value) {
    const segmentNo = resolvePackageSegmentNo(row) || '-';
    const sliceNo =
      row.productionBatchNo || row.sliceBatchNo || getWaitPieceKey(row);
    const sliceNos = segmentSliceMap.get(segmentNo) || [];
    sliceNos.push(sliceNo);
    segmentSliceMap.set(segmentNo, sliceNos);
  }
  if (segmentSliceMap.size <= 1) {
    return '';
  }
  const parts = Array.from(segmentSliceMap.entries()).map(
    ([segmentNo, sliceNos]) => {
      const samples = sliceNos.slice(0, 3);
      const suffix =
        sliceNos.length > samples.length ? `等${sliceNos.length}片` : '';
      return `${segmentNo}：${samples.join('、')}${suffix}`;
    },
  );
  return `不同段号的片号不能包装在一起。当前选择包含 ${parts.join('；')}，请按段号分开包装。`;
}

function buildSelectedQualityRiskText() {
  const frozenCount = selectedRows.value.filter((row) => resolvePackagingQualityStatus(row) === 'FROZEN').length;
  if (frozenCount > 0) return `已选 ${frozenCount} 片为冻结品，可包装、入库和手工出库，COA放行前禁止发货。`;
  const appearanceNgRows = selectedRows.value.filter(
    (item) => item.inspectionResult === 'NG',
  );
  const coaNgRows = selectedRows.value.filter(
    (item) => item.coaInspectionResult === 'NG',
  );
  if (appearanceNgRows.length === 0 && coaNgRows.length === 0) {
    return '';
  }
  const parts: string[] = [];
  if (appearanceNgRows.length > 0) {
    parts.push(`裁切FQC NG ${appearanceNgRows.length}片`);
  }
  if (coaNgRows.length > 0) {
    const segments = Array.from(
      new Set(
        coaNgRows.map(
          (item) => item.coaScopeBatchNo || item.parentProductionBatchNo || '-',
        ),
      ),
    );
    parts.push(
      `COA NG ${coaNgRows.length}片，涉及段号：${segments.join('、')}`,
    );
  }
  return `${parts.join('；')}。继续包装后将按NG质量状态写入包装/库存台账，发货环节仍按质量状态限制。`;
}

async function openPackageModal() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择待包装片');
    return;
  }
  if (!(await ensurePackagingDailyRecordsReady())) {
    return;
  }
  const segmentMismatchText = buildSelectedSegmentMismatchText();
  if (segmentMismatchText) {
    Modal.warning({
      content: segmentMismatchText,
      title: '当前选择包含不同段号',
    });
    return;
  }
  if (!(await ensureSelectedPackagingSampleUnlocked('包装'))) {
    return;
  }
  const riskText = buildSelectedQualityRiskText();
  if (riskText) {
    Modal.confirm({
      cancelText: '返回检查',
      content: riskText,
      okText: '继续包装',
      title: '当前选择包含NG片',
      onOk: () => resetPackageFormAndOpen(),
    });
    return;
  }
  await resetPackageFormAndOpen();
}

async function submitPackage() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择待包装片');
    return;
  }
  if (!(await ensurePackagingDailyRecordsReady())) {
    return;
  }
  if (!(await ensureSelectedPackagingSampleUnlocked('提交包装'))) {
    return;
  }
  const auxConsumeItems = buildPackageAuxConsumeItems();
  if (!auxConsumeItems) {
    return;
  }
  packaging.value = true;
  try {
    const result = await lockFgInboundPackage({
      auxConsumeItems,
      cutRoundReportIds: selectedRows.value
        .map((item) => Number(item.sourceCutRoundReportId || 0))
        .filter((id) => id > 0),
      manualPieceIds: selectedRows.value
        .map((item) => Number(item.sourceManualPieceId || 0))
        .filter((id) => id > 0),
      operatorName: currentUserName.value,
      packageNo: packageForm.packageNo.trim() || undefined,
      remark: packageForm.remark,
    });
    packageVisible.value = false;
    const packageNos = result.packageNos || [];
    const preview = packageNos.slice(0, 3).join('、');
    const suffix = packageNos.length > 3 ? '等' : '';
    message.success(
      `已完成 ${result.pieceCount || selectedRows.value.length} 片单片包装，生成 ${result.packageCount || packageNos.length} 个包装单${preview ? `：${preview}${suffix}` : ''}`,
    );
    await refreshAll();
  } finally {
    packaging.value = false;
  }
}

function openDetail(row: InboundPackage) {
  currentPackage.value = row;
  detailVisible.value = true;
  nextTick(() => detailGridApi.query());
}

function cancelPackage(row: InboundPackage) {
  if (!['PACKED', 'INBOUND_LOCKED'].includes(row.status || '')) {
    message.warning('只有待上架包装可以取消');
    return;
  }
  Modal.confirm({
    cancelText: '取消',
    content: `取消后，包装内片号会返回待包装区，包装单 ${row.boxNo || ''} 将移除。`,
    okButtonProps: { danger: true },
    okText: '取消包装',
    title: '确认取消包装？',
    async onOk() {
      revoking.value = true;
      try {
        await cancelFgInboundPackageLock({
          id: row.id,
          operatorName: currentUserName.value,
        });
        message.success('已取消包装，片号已返回待包装区');
        await refreshAll();
      } finally {
        revoking.value = false;
      }
    },
  });
}

function cancelSelectedPackages() {
  if (batchRevoking.value) return;
  const selectedPackages = [...selectedPackagePrintRows.value];
  if (selectedPackages.length === 0) {
    message.warning('请先在待上架包装段选择需要取消的包装单');
    return;
  }
  if (
    selectedPackages.some(
      (row) => !['PACKED', 'INBOUND_LOCKED'].includes(row.status || ''),
    )
  ) {
    message.warning('仅待上架包装可以批量取消，请刷新后重新选择');
    return;
  }
  Modal.confirm({
    cancelText: '返回',
    content: `取消后，已选 ${selectedPackages.length} 个包装内的片号将返回待包装区，包装单将移除。`,
    okButtonProps: { danger: true },
    okText: '批量取消包装',
    title: '确认批量取消包装？',
    async onOk() {
      batchRevoking.value = true;
      try {
        const result = await cancelFgInboundPackageLocks({
          operatorName: currentUserName.value,
          packageIds: selectedPackages.map((row) => Number(row.id)),
        });
        clearPackagePrintSelection();
        message.success(
          `已取消 ${result.packageCount || selectedPackages.length} 个包装，片号已返回待包装区`,
        );
        await refreshAll();
      } finally {
        batchRevoking.value = false;
      }
    },
  });
}

function getPageNo(page?: { currentPage?: number; pageNo?: number }) {
  return Number(page?.currentPage || page?.pageNo || 1);
}

function getPageSize(page?: { pageSize?: number }) {
  return Number(page?.pageSize || 20);
}

function buildPagedResult<T>(
  rows: T[],
  page?: { currentPage?: number; pageNo?: number; pageSize?: number },
) {
  const pageNo = Math.max(getPageNo(page), 1);
  const pageSize = Math.max(getPageSize(page), 1);
  const start = (pageNo - 1) * pageSize;
  return {
    list: rows.slice(start, start + pageSize),
    total: rows.length,
  };
}

const waitColumns = [
  {
    align: 'center',
    field: 'packageSelection',
    fixed: 'left',
    slots: { default: 'waitPackageSelection' },
    title: '打包选择',
    width: 96,
  },
  {
    field: 'productionBatchNo',
    fixed: 'left',
    slots: { default: 'waitPieceNo' },
    title: '段批次 / 片号',
    treeNode: true,
    width: 240,
  },
  { field: 'modelCode', title: '产品型号', width: 130 },
  { field: 'materialCode', title: '产品料号', width: 150 },
  {
    field: 'productionDate',
    slots: { default: 'waitProductionDate' },
    title: '生产日期',
    width: 170,
  },
  {
    field: 'expiryDate',
    slots: { default: 'waitExpiryDate' },
    title: '有效期',
    width: 120,
  },
  {
    field: 'inspectionResult',
    slots: { default: 'inspectionResult' },
    title: '裁切FQC',
    width: 120,
  },
  {
    field: 'coaInspectionResult',
    slots: { default: 'coaInspectionResult' },
    title: 'COA送检',
    width: 120,
  },
  {
    field: 'inspectionRemark',
    showOverflow: 'tooltip',
    title: '表观检验备注',
    minWidth: 220,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'waitActions' },
    title: '操作',
    width: 180,
  },
];

const packageColumns = [
  {
    field: 'boxNo',
    fixed: 'left',
    slots: { default: 'packedSegmentOrBoxNo' },
    title: '段批次 / 包装编号',
    treeNode: true,
    width: 260,
  },
  {
    field: 'currentQty',
    slots: { default: 'packedPackageQty' },
    title: '包装数量',
    width: 120,
  },
  {
    field: 'qualityStatus',
    slots: { default: 'packedQualityStatus' },
    title: '质量状态',
    width: 110,
  },
  {
    field: 'sliceList',
    slots: { default: 'packedSliceList' },
    showOverflow: 'tooltip',
    title: '片号',
    width: 300,
  },
  { field: 'remark', showOverflow: 'tooltip', title: '备注', minWidth: 180 },
  { field: 'lockUserName', title: '包装人', width: 110 },
  {
    field: 'lockTime',
    formatter: ({ row }: { row: InboundPackage }) =>
      formatDateTime(row.lockTime),
    title: '包装时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'packageActions' },
    title: '操作',
    width: 240,
  },
];

const packagingCheckColumns = [
  { field: 'recordNo', fixed: 'left', title: '记录编号', width: 190 },
  {
    field: 'templateName',
    minWidth: 230,
    showOverflow: 'tooltip',
    title: '表单名称',
  },
  { field: 'recordDate', title: '上报日期', width: 120 },
  { field: 'formTypeName', title: '类型', width: 110 },
  {
    field: 'resultStatus',
    slots: { default: 'processResultStatus' },
    title: '结果',
    width: 90,
  },
  {
    field: 'recordStatus',
    slots: { default: 'processRecordStatus' },
    title: '状态',
    width: 100,
  },
  { field: 'fillUserName', title: '填写人', width: 110 },
  {
    field: 'fillTime',
    formatter: ({ row }: { row: MesHcProcessFormApi.Record }) =>
      formatDateTime(row.fillTime || row.createTime),
    title: '填写时间',
    width: 160,
  },
  { field: 'confirmUserName', title: '确认人', width: 110 },
  {
    field: 'confirmTime',
    formatter: ({ row }: { row: MesHcProcessFormApi.Record }) =>
      formatDateTime(row.confirmTime),
    title: '确认时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'packagingCheckActions' },
    title: '操作',
    width: 220,
  },
];

const detailColumns = [
  { field: 'sliceBatchNo', title: '片号', minWidth: 190 },
  {
    field: 'qualityStatus',
    slots: { default: 'detailQualityStatus' },
    title: '质量状态',
    width: 110,
  },
  { field: 'scanUserName', title: '包装人', width: 120 },
  {
    field: 'scanTime',
    formatter: ({ row }: { row: MesHcFinishedPackagingApi.InboundBoxItem }) =>
      formatDateTime(row.scanTime),
    title: '包装时间',
    width: 170,
  },
];

async function loadWaitSegmentChildren(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: 'FROZEN' | 'NG' | 'OK',
) {
  if (!isWaitGroupRow(row)) {
    return [];
  }
  const segmentBatchNo = row.segmentBatchNo || row.groupKey;
  if (!segmentBatchNo) {
    return [];
  }
  const rows = await getFgInboundWaitSegmentPieceList({
    keyword: keyword.value.trim(),
    packagingQualityStatus,
    segmentBatchNo,
  });
  return (rows || []).map(toWaitPieceRow);
}

async function preloadWaitSegmentCoaResults(
  segments: InspectionSliceSegment[],
  packagingQualityStatus: 'FROZEN' | 'NG' | 'OK',
): Promise<InspectionSliceSegmentWithCoaResult[]> {
  const rows: InspectionSliceSegmentWithCoaResult[] = segments.map(
    (segment) => ({ ...segment }),
  );
  let nextIndex = 0;

  async function loadNextSegment() {
    while (nextIndex < rows.length) {
      const index = nextIndex++;
      const row = rows[index];
      const segmentBatchNo = normalizePackageSegmentNo(
        row.segmentBatchNo || row.sampleSliceBatchNo,
      );
      if (!segmentBatchNo) {
        row.coaInspectionResult = 'UNKNOWN';
        continue;
      }
      try {
        const pieces = await getFgInboundWaitSegmentPieceList({
          keyword: keyword.value.trim(),
          packagingQualityStatus,
          segmentBatchNo,
        });
        row.coaInspectionResult = pieces?.[0]?.coaInspectionResult || 'UNKNOWN';
      } catch (error) {
        console.warn(
          `预加载待包装段 ${segmentBatchNo} 的 COA 送检结果失败`,
          error,
        );
        row.coaInspectionResult = 'UNKNOWN';
      }
    }
  }

  await Promise.all(
    Array.from(
      { length: Math.min(WAIT_SEGMENT_COA_PREFETCH_CONCURRENCY, rows.length) },
      () => loadNextSegment(),
    ),
  );
  return rows;
}

function buildWaitGridOptions(
  packagingQualityStatus: 'FROZEN' | 'NG' | 'OK',
  updateTotal: (total: number) => void,
): VxeTableGridOptions<InspectionSliceDisplayRow> {
  return {
    border: true,
    columns: waitColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          waitLoading.value = true;
          try {
            const result = await getFgInboundWaitSegmentPage({
              keyword: keyword.value.trim(),
              pageNo: getPageNo(page),
              pageSize: getPageSize(page),
              packagingQualityStatus,
            });
            updateTotal(Number(result.total || 0));
            selectedRowKeys.value = [];
            selectedRows.value = [];
            setWaitPrintRows(packagingQualityStatus, []);
            const segments = await preloadWaitSegmentCoaResults(
              result.list || [],
              packagingQualityStatus,
            );
            return {
              ...result,
              list: segments.map((item, index) =>
                toWaitSegmentRow(item, packagingQualityStatus, index),
              ),
            };
          } finally {
            waitLoading.value = false;
          }
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'waitRowKey',
    },
    rowClassName: ({ row }: { row: InspectionSliceDisplayRow }) =>
      isWaitGroupRow(row) ? 'package-wait-segment-row' : '',
    treeConfig: {
      children: 'children',
      hasChild: 'hasChild',
      lazy: true,
      loadMethod: ({ row }: { row: InspectionSliceDisplayRow }) =>
        loadWaitSegmentChildren(row, packagingQualityStatus),
      reserve: true,
      showLine: true,
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  };
}

function isRowSelectionClickIgnored(
  column?: GridCellClickColumn,
  event?: Event,
) {
  if (
    column?.type === 'checkbox' ||
    ['action', 'packageSelection', 'printSelection'].includes(
      column?.field || '',
    )
  ) {
    return true;
  }
  const target = event?.target;
  return (
    target instanceof Element &&
    !!target.closest(
      'button, input, label, .vxe-tree--btn, .vxe-tree--btn-wrapper',
    )
  );
}

async function handleWaitPackagingRowClick(
  row: InspectionSliceDisplayRow,
  packagingQualityStatus: PackagingQualityStatus,
  column?: GridCellClickColumn,
  event?: Event,
) {
  if (isRowSelectionClickIgnored(column, event)) return;
  if (isWaitGroupRow(row)) {
    await setWaitSegmentPackageSelected(
      row,
      packagingQualityStatus,
      !isWaitSegmentPackageSelected(row),
    );
    return;
  }
  setWaitPiecePackageSelected(row, !isWaitPiecePackageSelected(row));
}

async function handlePackagePrintRowClick(
  row: InboundPackageDisplayRow,
  column?: GridCellClickColumn,
  event?: Event,
) {
  if (isRowSelectionClickIgnored(column, event)) return;
  if (isInboundPackedSegmentGroupRow(row)) {
    await setPackageSegmentPrintSelected(
      row,
      !isPackageSegmentPrintSelected(row),
    );
    return;
  }
  setPackagePrintSelected(row, !isPackagePrintSelected(row));
}

const [WaitQualifiedGrid, waitQualifiedGridApi] = useVbenVxeGrid({
  gridOptions: buildWaitGridOptions('OK', (total) => {
    qualifiedWaitTotal.value = total;
  }),
  gridEvents: {
    cellClick: ({
      row,
      column,
      $event,
    }: GridCellClickEvent<InspectionSliceDisplayRow>) => {
      void handleWaitPackagingRowClick(row, 'OK', column, $event);
    },
  },
});

const [WaitFrozenGrid, waitFrozenGridApi] = useVbenVxeGrid({
  gridOptions: buildWaitGridOptions('FROZEN', (total) => {
    frozenWaitTotal.value = total;
  }),
  gridEvents: {
    cellClick: ({
      row,
      column,
      $event,
    }: GridCellClickEvent<InspectionSliceDisplayRow>) => {
      void handleWaitPackagingRowClick(row, 'FROZEN', column, $event);
    },
  },
});

const [WaitUnqualifiedGrid, waitUnqualifiedGridApi] = useVbenVxeGrid({
  gridOptions: buildWaitGridOptions('NG', (total) => {
    unqualifiedWaitTotal.value = total;
  }),
  gridEvents: {
    cellClick: ({
      row,
      column,
      $event,
    }: GridCellClickEvent<InspectionSliceDisplayRow>) => {
      void handleWaitPackagingRowClick(row, 'NG', column, $event);
    },
  },
});

const [PackageGrid, packageGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: packageColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          packageLoading.value = true;
          try {
            const result = await getFgInboundPackedSegmentPage({
              keyword: packageKeyword.value.trim(),
              pageNo: getPageNo(page),
              pageSize: getPageSize(page),
            });
            packedSegmentTotal.value = Number(result.total || 0);
            selectedPackagePrintRows.value = [];
            return {
              ...result,
              list: (result.list || []).map(toInboundPackedSegmentRow),
            };
          } finally {
            packageLoading.value = false;
          }
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'packageRowKey',
    },
    rowClassName: ({ row }: { row: InboundPackageDisplayRow }) =>
      isInboundPackedSegmentGroupRow(row) ? 'package-wait-segment-row' : '',
    treeConfig: {
      children: 'children',
      hasChild: 'hasChild',
      lazy: true,
      loadMethod: ({ row }: { row: InboundPackageDisplayRow }) =>
        loadInboundPackedSegmentPackages(row),
      reserve: true,
      showLine: true,
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<InboundPackageDisplayRow>,
  gridEvents: {
    cellClick: ({
      row,
      column,
      $event,
    }: GridCellClickEvent<InboundPackageDisplayRow>) => {
      void handlePackagePrintRowClick(row, column, $event);
    },
  },
});

function buildProcessRecordGridOptions(
  action: PackagingProcessFormAction,
  loadingRef: { value: boolean },
): VxeTableGridOptions<MesHcProcessFormApi.Record> {
  return {
    border: true,
    columns: packagingCheckColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          loadingRef.value = true;
          try {
            return await getProcessFormRecordPage({
              formType: PACKAGING_PROCESS_FORMS[action].formType,
              pageNo: getPageNo(page),
              pageSize: getPageSize(page),
              processCode: PACKAGING_PROCESS_CODE,
              recordDate: packagingCheckQuery.recordDate || undefined,
            });
          } finally {
            loadingRef.value = false;
          }
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  };
}

const [StartupCheckGrid, startupCheckGridApi] = useVbenVxeGrid({
  gridOptions: buildProcessRecordGridOptions('STARTUP', startupCheckLoading),
});

const [CleaningCheckGrid, cleaningCheckGridApi] = useVbenVxeGrid({
  gridOptions: buildProcessRecordGridOptions('CLEANING', cleaningCheckLoading),
});

const [DetailGrid, detailGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: detailColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) =>
          buildPagedResult(currentPackage.value?.items || [], page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
  } as VxeTableGridOptions<MesHcFinishedPackagingApi.InboundBoxItem>,
});

onMounted(refreshAll);
</script>

<template>
  <PackagingNcrFlowModal v-model:open="ncrFlowVisible" />
  <Page auto-content-height>
    <input
      ref="manualPieceImportInputRef"
      accept=".xlsx,.xls"
      class="hidden"
      type="file"
      @change="handleManualPieceImportFileChange"
    />
    <div class="fg-packaging-workbench package-fg-console">
      <section class="prototype-banner">
        <button
          class="console-main-icon packaging-record-toggle"
          :class="{ 'is-active': packagingDailyCheckDialogVisible }"
          type="button"
          title="查看今日点检/清洁记录"
          @click="openPackagingDailyCheckDialog"
        >
          <IconifyIcon icon="lucide:package-check" />
        </button>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">成品包装报工</h2>
            <Button
              class="piece-label-entry"
              size="small"
              @click="openPiecePrintModal()"
            >
              <template #icon><IconifyIcon icon="lucide:printer" /></template>
              片号打印
            </Button>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="ncrFlowVisible = true">
            <IconifyIcon icon="lucide:git-pull-request" />
            <span>处置单流转</span>
          </button>
          <button class="action-tile" type="button" @click="refreshAll">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('STARTUP')"
          >
            <IconifyIcon icon="lucide:power" />
            <span>开机点检</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('CLEANING')"
          >
            <IconifyIcon icon="lucide:spray-can" />
            <span>清洁保养</span>
          </button>
          <button
            class="action-tile action-tile-history"
            type="button"
            @click="openManualPieceModal"
          >
            <IconifyIcon icon="lucide:plus" />
            <span>历史片新增</span>
          </button>
          <button
            class="action-tile action-tile-history-import"
            type="button"
            @click="manualPieceImportVisible = true"
          >
            <IconifyIcon icon="lucide:file-up" />
            <span>历史片导入</span>
          </button>
          <button
            class="action-tile"
            :class="{ 'is-disabled': packaging || packageScanResolving }"
            :disabled="packaging || packageScanResolving"
            type="button"
            @click="openPackageScanModal"
          >
            <IconifyIcon icon="lucide:scan-line" />
            <span>扫码包装</span>
          </button>
          <button
            class="action-tile"
            :class="{
              'is-disabled': selectedRowKeys.length === 0 || packaging,
            }"
            :disabled="selectedRowKeys.length === 0 || packaging"
            type="button"
            @click="openPackageModal"
          >
            <IconifyIcon icon="lucide:package-plus" />
            <span>确定包装</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="activeTab = PACKED_TAB"
          >
            <IconifyIcon icon="lucide:clipboard-list" />
            <span>待上架</span>
          </button>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="fg-packaging-tabs">
        <TabPane
          :key="WAIT_QUALIFIED_TAB"
          :tab="`合格待包装段(${qualifiedWaitTotal})`"
        >
          <div class="fg-tab-panel">
            <div class="fg-tab-toolbar-stack">
              <div class="fg-query-bar">
                <Input
                  v-model:value="keyword"
                  allow-clear
                  placeholder="段批次 / 片号 / 料号 / 型号"
                  @press-enter="fetchWaitPieces"
                />
                <Button type="primary" @click="fetchWaitPieces">
                  <template #icon
                    ><IconifyIcon icon="lucide:search"
                  /></template>
                  查询合格段
                </Button>
              </div>
              <div class="fg-segment-print-toolbar">
                <span
                  >标签打印：已选
                  {{ qualifiedWaitPrintCount }}
                  片；勾选分段行可全选本段全部片号</span
                >
                <div>
                  <Button
                    size="small"
                    :disabled="qualifiedWaitPrintCount === 0"
                    @click="clearWaitPrintSelection('OK')"
                    >清空</Button
                  >
                  <Button
                    size="small"
                    type="primary"
                    :disabled="qualifiedWaitPrintCount === 0 || piecePrinting"
                    :loading="piecePrinting"
                    @click="printSelectedWaitPieceLabels('OK')"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:printer"
                    /></template>
                    打印已选
                  </Button>
                </div>
              </div>
            </div>
            <section class="package-fg-grid-panel">
              <WaitQualifiedGrid table-title="合格待包装段">
                <template #waitPackageSelection="{ row }">
                  <Checkbox
                    v-if="row.__group"
                    :checked="isWaitSegmentPackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    :indeterminate="
                      getWaitSegmentPackageSelectedCount(row) > 0 &&
                      !isWaitSegmentPackageSelected(row)
                    "
                    @click.stop
                    @change="
                      setWaitSegmentPackageSelected(
                        row,
                        'OK',
                        $event.target.checked,
                      )
                    "
                    >本段</Checkbox
                  >
                  <Checkbox
                    v-else
                    :checked="isWaitPiecePackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    @click.stop
                    @change="
                      setWaitPiecePackageSelected(row, $event.target.checked)
                    "
                  />
                </template>
                <template #waitPieceNo="{ row }">
                  <div v-if="row.__group" class="package-wait-segment-cell">
                    <strong>{{
                      row.segmentBatchNo || row.groupKey || '-'
                    }}</strong>
                    <em
                      >共 {{ row.groupCount || 0 }} 片，{{
                        formatWaitSegmentSamples(row)
                      }}</em
                    >
                  </div>
                  <span v-else class="package-wait-piece-no">
                    {{ row.productionBatchNo || row.sliceBatchNo || '-' }}
                    <Tag
                      v-if="row.sourceType === SOURCE_MANUAL_HISTORY"
                      color="orange"
                      >历史补录</Tag
                    >
                  </span>
                </template>
                <template #waitProductionDate="{ row }">
                  <span>{{ formatWaitProductionDate(row) }}</span>
                </template>
                <template #waitExpiryDate="{ row }">
                  <span>{{ formatWaitExpiryDate(row) }}</span>
                </template>
                <template #inspectionResult="{ row }">
                  <Tag v-if="row.__group" color="blue">分组</Tag>
                  <Tag v-else :color="resultColor(row.inspectionResult)">{{
                    row.inspectionResult || '-'
                  }}</Tag>
                </template>
                <template #coaInspectionResult="{ row }">
                  <Tag
                    v-if="row.__group"
                    :color="coaResultColor(row.coaInspectionResult)"
                  >
                    {{ coaResultText(row.coaInspectionResult) }}
                  </Tag>
                  <Tooltip v-else :title="formatCoaTooltip(row)">
                    <Tag :color="coaResultColor(row.coaInspectionResult)">
                      {{ coaResultText(row.coaInspectionResult) }}
                    </Tag>
                  </Tooltip>
                </template>
                <template #waitActions="{ row }">
                  <div class="fg-row-actions">
                    <Checkbox
                      v-if="row.__group"
                      :checked="isWaitSegmentPrintSelected(row, 'OK')"
                      :disabled="waitSegmentPrintSelecting"
                      :indeterminate="
                        getWaitSegmentPrintSelectedCount(row, 'OK') > 0 &&
                        !isWaitSegmentPrintSelected(row, 'OK')
                      "
                      @click.stop
                      @change="
                        setWaitSegmentPrintSelected(
                          row,
                          'OK',
                          $event.target.checked,
                        )
                      "
                      >打印本段</Checkbox
                    >
                    <Checkbox
                      v-else
                      :checked="isWaitPiecePrintSelected(row, 'OK')"
                      :disabled="waitSegmentPrintSelecting"
                      @click.stop
                      @change="
                        setWaitPiecePrintSelected(
                          row,
                          'OK',
                          $event.target.checked,
                        )
                      "
                      >选择打印</Checkbox
                    >
                    <Button
                      v-if="canDeleteManualPiece(row)"
                      danger
                      size="small"
                      type="link"
                      @click.stop="openManualPieceDeleteModal(row)"
                    >
                      <template #icon
                        ><IconifyIcon icon="lucide:trash-2"
                      /></template>
                      删除
                    </Button>
                  </div>
                </template>
              </WaitQualifiedGrid>
            </section>
          </div>
        </TabPane>

        <TabPane
          :key="WAIT_FROZEN_TAB"
          :tab="`冻结待包装段(${frozenWaitTotal})`"
        >
          <div class="fg-tab-panel">
            <div class="fg-tab-toolbar-stack">
              <div class="fg-query-bar">
                <Input
                  v-model:value="keyword"
                  allow-clear
                  placeholder="段批次 / 片号 / 料号 / 型号"
                  @press-enter="fetchWaitPieces"
                />
                <Button type="primary" @click="fetchWaitPieces">
                  <template #icon
                    ><IconifyIcon icon="lucide:search"
                  /></template>
                  查询合格段
                </Button>
              </div>
              <div class="fg-segment-print-toolbar">
                <span
                  >标签打印：已选
                  {{ frozenWaitPrintCount }}
                  片；勾选分段行可全选本段全部片号</span
                >
                <div>
                  <Button
                    size="small"
                    :disabled="frozenWaitPrintCount === 0"
                    @click="clearWaitPrintSelection('FROZEN')"
                    >清空</Button
                  >
                  <Button
                    size="small"
                    type="primary"
                    :disabled="frozenWaitPrintCount === 0 || piecePrinting"
                    :loading="piecePrinting"
                    @click="printSelectedWaitPieceLabels('FROZEN', 'OK')"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:printer"
                    /></template>
                    打印已选
                  </Button>
                </div>
              </div>
            </div>
            <section class="package-fg-grid-panel">
              <WaitFrozenGrid table-title="冻结待包装段">
                <template #waitPackageSelection="{ row }">
                  <Checkbox
                    v-if="row.__group"
                    :checked="isWaitSegmentPackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    :indeterminate="
                      getWaitSegmentPackageSelectedCount(row) > 0 &&
                      !isWaitSegmentPackageSelected(row)
                    "
                    @click.stop
                    @change="
                      setWaitSegmentPackageSelected(
                        row,
                        'FROZEN',
                        $event.target.checked,
                      )
                    "
                    >本段</Checkbox
                  >
                  <Checkbox
                    v-else
                    :checked="isWaitPiecePackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    @click.stop
                    @change="
                      setWaitPiecePackageSelected(row, $event.target.checked)
                    "
                  />
                </template>
                <template #waitPieceNo="{ row }">
                  <div v-if="row.__group" class="package-wait-segment-cell">
                    <strong>{{
                      row.segmentBatchNo || row.groupKey || '-'
                    }}</strong>
                    <em
                      >共 {{ row.groupCount || 0 }} 片，{{
                        formatWaitSegmentSamples(row)
                      }}</em
                    >
                  </div>
                  <span v-else class="package-wait-piece-no">
                    {{ row.productionBatchNo || row.sliceBatchNo || '-' }}
                    <Tag
                      v-if="row.sourceType === SOURCE_MANUAL_HISTORY"
                      color="orange"
                      >历史补录</Tag
                    >
                  </span>
                </template>
                <template #waitProductionDate="{ row }">
                  <span>{{ formatWaitProductionDate(row) }}</span>
                </template>
                <template #waitExpiryDate="{ row }">
                  <span>{{ formatWaitExpiryDate(row) }}</span>
                </template>
                <template #inspectionResult="{ row }">
                  <Tag v-if="row.__group" color="blue">分组</Tag>
                  <Tag v-else :color="resultColor(row.inspectionResult)">{{
                    row.inspectionResult || '-'
                  }}</Tag>
                </template>
                <template #coaInspectionResult="{ row }">
                  <Tag
                    v-if="row.__group"
                    :color="coaResultColor(row.coaInspectionResult)"
                  >
                    {{ coaResultText(row.coaInspectionResult) }}
                  </Tag>
                  <Tooltip v-else :title="formatCoaTooltip(row)">
                    <Tag :color="coaResultColor(row.coaInspectionResult)">
                      {{ coaResultText(row.coaInspectionResult) }}
                    </Tag>
                  </Tooltip>
                </template>
                <template #waitActions="{ row }">
                  <div class="fg-row-actions">
                    <Checkbox
                      v-if="row.__group"
                      :checked="isWaitSegmentPrintSelected(row, 'FROZEN')"
                      :disabled="waitSegmentPrintSelecting"
                      :indeterminate="
                        getWaitSegmentPrintSelectedCount(row, 'FROZEN') > 0 &&
                        !isWaitSegmentPrintSelected(row, 'FROZEN')
                      "
                      @click.stop
                      @change="
                        setWaitSegmentPrintSelected(
                          row,
                          'FROZEN',
                          $event.target.checked,
                        )
                      "
                      >打印本段</Checkbox
                    >
                    <Checkbox
                      v-else
                      :checked="isWaitPiecePrintSelected(row, 'FROZEN')"
                      :disabled="waitSegmentPrintSelecting"
                      @click.stop
                      @change="
                        setWaitPiecePrintSelected(
                          row,
                          'FROZEN',
                          $event.target.checked,
                        )
                      "
                      >选择打印</Checkbox
                    >
                    <Button
                      v-if="canDeleteManualPiece(row)"
                      danger
                      size="small"
                      type="link"
                      @click.stop="openManualPieceDeleteModal(row)"
                    >
                      <template #icon
                        ><IconifyIcon icon="lucide:trash-2"
                      /></template>
                      删除
                    </Button>
                  </div>
                </template>
              </WaitFrozenGrid>
            </section>
          </div>
        </TabPane>

        <TabPane
          :key="WAIT_UNQUALIFIED_TAB"
          :tab="`不合格待包装段(${unqualifiedWaitTotal})`"
        >
          <div class="fg-tab-panel">
            <div class="fg-tab-toolbar-stack">
              <div class="fg-query-bar">
                <Input
                  v-model:value="keyword"
                  allow-clear
                  placeholder="段批次 / 片号 / 料号 / 型号"
                  @press-enter="fetchWaitPieces"
                />
                <Button danger type="primary" @click="fetchWaitPieces">
                  <template #icon
                    ><IconifyIcon icon="lucide:search"
                  /></template>
                  查询不合格段
                </Button>
              </div>
              <div class="fg-segment-print-toolbar">
                <span
                  >标签打印：已选
                  {{ unqualifiedWaitPrintCount }}
                  片；勾选分段行可全选本段全部片号</span
                >
                <div>
                  <Button
                    size="small"
                    :disabled="unqualifiedWaitPrintCount === 0"
                    @click="clearWaitPrintSelection('NG')"
                    >清空</Button
                  >
                  <Button
                    size="small"
                    type="primary"
                    :disabled="unqualifiedWaitPrintCount === 0 || piecePrinting"
                    :loading="piecePrinting"
                    @click="printSelectedWaitPieceLabels('NG', 'OK')"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:printer"
                    /></template>
                    打印合格
                  </Button>
                  <Button
                    danger
                    size="small"
                    type="primary"
                    :disabled="unqualifiedWaitPrintCount === 0 || piecePrinting"
                    :loading="piecePrinting"
                    @click="printSelectedWaitPieceLabels('NG', 'NG')"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:printer"
                    /></template>
                    打印不合格
                  </Button>
                </div>
              </div>
            </div>
            <section class="package-fg-grid-panel">
              <WaitUnqualifiedGrid table-title="不合格待包装段">
                <template #waitPackageSelection="{ row }">
                  <Checkbox
                    v-if="row.__group"
                    :checked="isWaitSegmentPackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    :indeterminate="
                      getWaitSegmentPackageSelectedCount(row) > 0 &&
                      !isWaitSegmentPackageSelected(row)
                    "
                    @click.stop
                    @change="
                      setWaitSegmentPackageSelected(
                        row,
                        'NG',
                        $event.target.checked,
                      )
                    "
                    >本段</Checkbox
                  >
                  <Checkbox
                    v-else
                    :checked="isWaitPiecePackageSelected(row)"
                    :disabled="waitSegmentPackageSelecting"
                    @click.stop
                    @change="
                      setWaitPiecePackageSelected(row, $event.target.checked)
                    "
                  />
                </template>
                <template #waitPieceNo="{ row }">
                  <div v-if="row.__group" class="package-wait-segment-cell">
                    <strong>{{
                      row.segmentBatchNo || row.groupKey || '-'
                    }}</strong>
                    <em
                      >共 {{ row.groupCount || 0 }} 片，{{
                        formatWaitSegmentSamples(row)
                      }}</em
                    >
                  </div>
                  <span v-else class="package-wait-piece-no">
                    {{ row.productionBatchNo || row.sliceBatchNo || '-' }}
                    <Tag
                      v-if="row.sourceType === SOURCE_MANUAL_HISTORY"
                      color="orange"
                      >历史补录</Tag
                    >
                  </span>
                </template>
                <template #waitProductionDate="{ row }">
                  <span>{{ formatWaitProductionDate(row) }}</span>
                </template>
                <template #waitExpiryDate="{ row }">
                  <span>{{ formatWaitExpiryDate(row) }}</span>
                </template>
                <template #inspectionResult="{ row }">
                  <Tag v-if="row.__group" color="blue">分组</Tag>
                  <Tag v-else :color="resultColor(row.inspectionResult)">{{
                    row.inspectionResult || '-'
                  }}</Tag>
                </template>
                <template #coaInspectionResult="{ row }">
                  <Tag
                    v-if="row.__group"
                    :color="coaResultColor(row.coaInspectionResult)"
                  >
                    {{ coaResultText(row.coaInspectionResult) }}
                  </Tag>
                  <Tooltip v-else :title="formatCoaTooltip(row)">
                    <Tag :color="coaResultColor(row.coaInspectionResult)">
                      {{ coaResultText(row.coaInspectionResult) }}
                    </Tag>
                  </Tooltip>
                </template>
                <template #waitActions="{ row }">
                  <div class="fg-row-actions">
                    <Checkbox
                      v-if="row.__group"
                      :checked="isWaitSegmentPrintSelected(row, 'NG')"
                      :disabled="waitSegmentPrintSelecting"
                      :indeterminate="
                        getWaitSegmentPrintSelectedCount(row, 'NG') > 0 &&
                        !isWaitSegmentPrintSelected(row, 'NG')
                      "
                      @click.stop
                      @change="
                        setWaitSegmentPrintSelected(
                          row,
                          'NG',
                          $event.target.checked,
                        )
                      "
                      >打印本段</Checkbox
                    >
                    <Checkbox
                      v-else
                      :checked="isWaitPiecePrintSelected(row, 'NG')"
                      :disabled="waitSegmentPrintSelecting"
                      @click.stop
                      @change="
                        setWaitPiecePrintSelected(
                          row,
                          'NG',
                          $event.target.checked,
                        )
                      "
                      >选择打印</Checkbox
                    >
                    <Button
                      v-if="canDeleteManualPiece(row)"
                      danger
                      size="small"
                      type="link"
                      @click.stop="openManualPieceDeleteModal(row)"
                    >
                      <template #icon
                        ><IconifyIcon icon="lucide:trash-2"
                      /></template>
                      删除
                    </Button>
                  </div>
                </template>
              </WaitUnqualifiedGrid>
            </section>
          </div>
        </TabPane>

        <TabPane :key="PACKED_TAB" :tab="`待上架包装段(${packedSegmentTotal})`">
          <div class="fg-tab-panel">
            <div class="fg-tab-toolbar-stack">
              <div class="fg-package-toolbar">
                <Input
                  v-model:value="packageKeyword"
                  allow-clear
                  placeholder="段批次 / 片号 / 包装编号 / 料号 / 型号"
                  @press-enter="fetchPackages"
                />
                <Button :loading="packageLoading" @click="fetchPackages">
                  <template #icon
                    ><IconifyIcon icon="lucide:refresh-cw"
                  /></template>
                  刷新
                </Button>
              </div>
              <div class="fg-segment-print-toolbar">
                <span
                  >片号标签：已选 {{ selectedPackagePrintCount }} 个包装，共
                  {{ selectedPackagePiecePrintCount }}
                  片；勾选分段行可全选本段全部包装</span
                >
                <div>
                  <Button
                    size="small"
                    :disabled="selectedPackagePrintCount === 0"
                    @click="clearPackagePrintSelection"
                    >清空</Button
                  >
                  <Button
                    size="small"
                    type="primary"
                    :disabled="selectedPackagePrintCount === 0 || piecePrinting || batchRevoking"
                    :loading="piecePrinting"
                    @click="printSelectedPackedPackagePieceLabels"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:printer"
                    /></template>
                    批量打印
                  </Button>
                  <Button
                    danger
                    size="small"
                    :disabled="selectedPackagePrintCount === 0 || piecePrinting || batchRevoking"
                    :loading="batchRevoking"
                    @click="cancelSelectedPackages"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:undo-2"
                    /></template>
                    批量取消
                  </Button>
                </div>
              </div>
            </div>
            <section class="package-fg-grid-panel">
              <PackageGrid table-title="待上架包装段">
                <template #packedSegmentOrBoxNo="{ row }">
                  <div v-if="row.__group" class="package-wait-segment-cell">
                    <strong>{{ row.segmentBatchNo || '-' }}</strong>
                    <em>
                      {{ row.groupPackageCount || 0 }} 个包装，{{
                        row.groupPieceCount || 0
                      }}
                      片，
                      {{ formatInboundPackedSegmentSamples(row) }}
                    </em>
                  </div>
                  <strong v-else class="package-wait-piece-no">{{
                    row.boxNo || '-'
                  }}</strong>
                </template>
                <template #packedPackageQty="{ row }">
                  <span v-if="row.__group"
                    >{{ row.groupPieceCount || 0 }} 片</span
                  >
                  <span v-else>{{ row.currentQty || 0 }} 片</span>
                </template>
                <template #packedQualityStatus="{ row }">
                  <span v-if="row.__group">展开后查看</span>
                  <Tag v-else :color="resultColor(row.qualityStatus)">{{
                    row.qualityStatus === 'FROZEN' ? '冻结' : row.qualityStatus || '-'
                  }}</Tag>
                </template>
                <template #packedSliceList="{ row }">
                  <span v-if="row.__group">-</span>
                  <span v-else>{{ formatPackageSliceList(row) }}</span>
                </template>
                <template #packageActions="{ row }">
                  <div class="fg-row-actions">
                    <Checkbox
                      v-if="row.__group"
                      :checked="isPackageSegmentPrintSelected(row)"
                      :disabled="waitSegmentPrintSelecting"
                      :indeterminate="
                        getPackageSegmentPrintSelectedCount(row) > 0 &&
                        !isPackageSegmentPrintSelected(row)
                      "
                      @click.stop
                      @change="
                        setPackageSegmentPrintSelected(
                          row,
                          $event.target.checked,
                        )
                      "
                      >打印本段</Checkbox
                    >
                    <template v-else>
                      <Checkbox
                        :checked="isPackagePrintSelected(row)"
                        :disabled="
                          !isPendingPackagePrintRow(row) ||
                          waitSegmentPrintSelecting
                        "
                        @click.stop
                        @change="
                          setPackagePrintSelected(row, $event.target.checked)
                        "
                        >选择打印</Checkbox
                      >
                      <Button size="small" type="link" @click="openDetail(row)"
                        >详情</Button
                      >
                      <Button
                        danger
                        :disabled="revoking"
                        size="small"
                        type="link"
                        @click="cancelPackage(row)"
                        >取消</Button
                      >
                    </template>
                  </div>
                </template>
              </PackageGrid>
            </section>
          </div>
        </TabPane>

        <TabPane
          v-if="showPackagingRecordTabs"
          :key="STARTUP_CHECK_TAB"
          tab="开机点检记录"
        >
          <div class="fg-tab-panel">
            <div class="fg-package-check-toolbar">
              <DatePicker
                v-model:value="packagingCheckQuery.recordDate"
                allow-clear
                placeholder="上报日期"
                value-format="YYYY-MM-DD"
                @change="fetchStartupChecks"
              />
              <Button
                type="primary"
                :loading="startupCheckLoading"
                @click="fetchStartupChecks"
              >
                <template #icon><IconifyIcon icon="lucide:search" /></template>
                查询
              </Button>
              <Button type="primary" @click="openProcessFormEditor('STARTUP')">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                新增
              </Button>
            </div>
            <section class="package-fg-grid-panel">
              <StartupCheckGrid table-title="开机点检记录">
                <template #processRecordStatus="{ row }">
                  <Tag :color="recordStatusColor(row.recordStatus)">{{
                    recordStatusText(row.recordStatus)
                  }}</Tag>
                </template>
                <template #processResultStatus="{ row }">
                  <Tag :color="resultColor(row.resultStatus)">{{
                    row.resultStatus || '-'
                  }}</Tag>
                </template>
                <template #packagingCheckActions="{ row }">
                  <div class="fg-row-actions">
                    <Button
                      size="small"
                      type="link"
                      @click="openPackagingCheckRecord(row, 'view')"
                      >查看</Button
                    >
                    <Button
                      size="small"
                      type="link"
                      :disabled="row.recordStatus === 'CONFIRMED'"
                      @click="openPackagingCheckRecord(row, 'edit')"
                    >
                      修改
                    </Button>
                    <Button
                      danger
                      size="small"
                      type="link"
                      :disabled="row.recordStatus === 'CONFIRMED'"
                      @click="confirmPackagingCheckRow(row)"
                    >
                      确认
                    </Button>
                  </div>
                </template>
              </StartupCheckGrid>
            </section>
          </div>
        </TabPane>

        <TabPane
          v-if="showPackagingRecordTabs"
          :key="CLEANING_CHECK_TAB"
          tab="清洁保养记录"
        >
          <div class="fg-tab-panel">
            <div class="fg-package-check-toolbar">
              <DatePicker
                v-model:value="packagingCheckQuery.recordDate"
                allow-clear
                placeholder="上报日期"
                value-format="YYYY-MM-DD"
                @change="fetchCleaningChecks"
              />
              <Button
                type="primary"
                :loading="cleaningCheckLoading"
                @click="fetchCleaningChecks"
              >
                <template #icon><IconifyIcon icon="lucide:search" /></template>
                查询
              </Button>
              <Button type="primary" @click="openProcessFormEditor('CLEANING')">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                新增
              </Button>
            </div>
            <section class="package-fg-grid-panel">
              <CleaningCheckGrid table-title="清洁保养记录">
                <template #processRecordStatus="{ row }">
                  <Tag :color="recordStatusColor(row.recordStatus)">{{
                    recordStatusText(row.recordStatus)
                  }}</Tag>
                </template>
                <template #processResultStatus="{ row }">
                  <Tag :color="resultColor(row.resultStatus)">{{
                    row.resultStatus || '-'
                  }}</Tag>
                </template>
                <template #packagingCheckActions="{ row }">
                  <div class="fg-row-actions">
                    <Button
                      size="small"
                      type="link"
                      @click="openPackagingCheckRecord(row, 'view')"
                      >查看</Button
                    >
                    <Button
                      size="small"
                      type="link"
                      :disabled="row.recordStatus === 'CONFIRMED'"
                      @click="openPackagingCheckRecord(row, 'edit')"
                    >
                      修改
                    </Button>
                    <Button
                      danger
                      size="small"
                      type="link"
                      :disabled="row.recordStatus === 'CONFIRMED'"
                      @click="confirmPackagingCheckRow(row)"
                    >
                      确认
                    </Button>
                  </div>
                </template>
              </CleaningCheckGrid>
            </section>
          </div>
        </TabPane>
      </Tabs>

      <Modal
        v-model:open="packagingDailyCheckDialogVisible"
        :footer="null"
        :width="720"
        destroy-on-close
        title="今日点检/清洁记录"
      >
        <div class="packaging-daily-check-modal-head">
          <span>记录日期：{{ dayjs().format('YYYY-MM-DD') }}</span>
          <Button
            size="small"
            :loading="packagingDailyCheckLoading"
            @click="loadPackagingDailyCheckCards"
          >
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            刷新
          </Button>
        </div>
        <div class="packaging-daily-check-card-grid">
          <section
            v-for="card in packagingDailyCheckCards"
            :key="card.action"
            class="packaging-daily-check-card"
          >
            <span class="packaging-daily-check-card__icon">
              <IconifyIcon
                :icon="
                  card.action === 'CLEANING'
                    ? 'lucide:sparkles'
                    : 'lucide:power'
                "
              />
            </span>
            <span class="packaging-daily-check-card__content">
              <strong>{{ card.title }}</strong>
              <em>{{ PACKAGING_PROCESS_FORMS[card.action].title }}</em>
              <span
                >{{ card.timing }} /
                {{ packagingDailyCheckCardStatusText(card.status) }}</span
              >
            </span>
            <Tag
              :color="packagingDailyCheckCardStatusColor(card.status)"
              class="packaging-daily-check-card__tag"
            >
              {{ packagingDailyCheckCardStatusText(card.status) }}
            </Tag>
            <span class="packaging-daily-check-card__actions">
              <Button
                v-if="card.status !== 'CONFIRMED'"
                size="small"
                type="primary"
                @click="openPackagingDailyCheckCard(card)"
              >
                填写
              </Button>
              <Button
                v-if="card.status === 'PENDING_CONFIRM' && card.record"
                danger
                size="small"
                @click="confirmPackagingCheckRow(card.record)"
              >
                确认
              </Button>
              <Button
                v-if="card.record"
                size="small"
                @click="openPackagingDailyCheckCard(card, 'view')"
              >
                查看
              </Button>
            </span>
          </section>
        </div>
        <div
          v-if="
            !packagingDailyCheckLoading && packagingDailyCheckCards.length === 0
          "
          class="packaging-daily-check-empty"
        >
          暂无可用的包装点检模板，请检查动态表单配置。
        </div>
        <div class="packaging-daily-check-modal-footer">
          <Button @click="packagingDailyCheckDialogVisible = false"
            >关闭</Button
          >
        </div>
      </Modal>

      <StationFormRuntimeFillModal
        v-model:open="runtimeFillOpen"
        :confirm-auth-action-name="`确认${runtimeFillTitle}`"
        enable-confirm
        :form="runtimeFillForm"
        :initial-params="runtimeInitialParams"
        :save-auth-action-name="`保存${runtimeFillTitle}`"
        skip-business-param-step
        @success="handleRuntimeFillSaved"
      />

      <Modal
        :open="runtimeViewVisible"
        :footer="null"
        :title="null"
        :z-index="3220"
        destroy-on-close
        width="100vw"
        wrap-class-name="hc-pass-work-modal packaging-dev-process-form-view-modal"
        @cancel="closeRuntimeView"
      >
        <div class="packaging-dev-runtime-view">
          <div class="packaging-dev-runtime-view__header">
            <div>
              <h3>{{ runtimeViewTitle }}</h3>
              <div class="packaging-dev-runtime-view__meta">
                <span v-for="item in runtimeViewRecordMeta" :key="item">{{
                  item
                }}</span>
              </div>
            </div>
            <div class="packaging-dev-runtime-view__actions">
              <Button
                v-if="!runtimeViewReadOnly"
                :loading="runtimeViewSaving"
                type="primary"
                @click="requestRuntimeSaveAuth"
              >
                保存
              </Button>
              <Button
                v-if="
                  runtimeViewRecord &&
                  runtimeViewRecord.recordStatus !== 'CONFIRMED'
                "
                :loading="runtimeViewConfirming"
                danger
                type="primary"
                @click="confirmRuntimeRecord(runtimeViewRecord)"
              >
                确认
              </Button>
              <Button @click="closeRuntimeView">关闭</Button>
            </div>
          </div>
          <div class="packaging-dev-runtime-view__body">
            <StationFormRuntimeRenderer
              v-if="runtimeViewRecord && !runtimeViewLoading"
              ref="runtimeRendererRef"
              v-model:header-data="runtimeViewHeaderData"
              :form-name="runtimeViewTitle"
              :items="runtimeViewItems"
              :record-meta="runtimeViewRecordMeta"
              :schema="runtimeViewSchema"
              :readonly="runtimeViewReadOnly"
            />
            <div v-else class="packaging-dev-runtime-loading">
              正在加载包装点检表详情...
            </div>
          </div>
        </div>
      </Modal>

      <AuthModal
        v-model:visible="runtimeSaveAuthVisible"
        :action-name="`保存${runtimeViewTitle}`"
        auth-mode="username"
        @success="handleRuntimeSaveAuthSuccess"
      />

      <AuthModal
        v-model:visible="runtimeConfirmAuthVisible"
        :action-name="`确认${runtimeViewTitle}`"
        auth-mode="username"
        @success="handleRuntimeConfirmAuthSuccess"
      />

      <Modal
        v-model:open="manualPieceVisible"
        :confirm-loading="manualPieceSubmitting"
        ok-text="新增并进入待包装"
        title="新增历史片"
        width="680px"
        @ok="submitManualPiece"
      >
        <div class="manual-piece-tip">
          仅用于补录 MES
          上线前已有片号；新增后进入待包装区，仍需执行确定包装和成品上架。
        </div>
        <div class="manual-piece-form">
          <label><i>*</i>片号</label>
          <Input
            v-model:value="manualPieceForm.sliceBatchNo"
            allow-clear
            maxlength="100"
            placeholder="例如：W26G143AS102B"
          />
          <label><i>*</i>分段批号</label>
          <Input
            v-model:value="manualPieceForm.segmentBatchNo"
            allow-clear
            maxlength="100"
            placeholder="请按实际分段批号填写"
          />
          <label><i>*</i>产品</label>
          <div class="manual-piece-product">
            <Input
              v-model:value="manualPieceForm.modelCode"
              allow-clear
              placeholder="产品型号，例如：W33P0300"
            />
            <Input
              v-model:value="manualPieceForm.materialCode"
              allow-clear
              placeholder="产品料号，例如：03.13.10055"
            />
            <Button @click="productBomPickerOpen = true">
              <template #icon
                ><IconifyIcon icon="lucide:list-filter"
              /></template>
              从产品BOM选择
            </Button>
          </div>
          <label><i>*</i>生产日期</label>
          <DatePicker
            v-model:value="manualPieceForm.productionDate"
            class="w-full"
            placeholder="选择生产日期"
            value-format="YYYY-MM-DD"
          />
          <label><i>*</i>有效期</label>
          <Input
            :value="manualPieceForm.expiryDate"
            disabled
            placeholder="按生产日期自动计算"
          />
          <label><i>*</i>FQC结果</label>
          <Select
            v-model:value="manualPieceForm.inspectionResult"
            :options="[
              { label: 'OK', value: 'OK' },
              { label: 'NG', value: 'NG' },
            ]"
          />
          <label><i>*</i>COA结果</label>
          <Select
            v-model:value="manualPieceForm.coaInspectionResult"
            :options="[
              { label: 'OK', value: 'OK' },
              { label: 'NG', value: 'NG' },
            ]"
          />
          <label><i>*</i>补录原因</label>
          <Textarea
            v-model:value="manualPieceForm.backfillReason"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            maxlength="500"
            placeholder="请填写历史数据补录原因"
            show-count
          />
          <label>备注</label>
          <Textarea
            v-model:value="manualPieceForm.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            maxlength="500"
            placeholder="选填"
            show-count
          />
        </div>
      </Modal>

      <Modal
        v-model:open="manualPieceDeleteVisible"
        cancel-text="取消"
        :confirm-loading="manualPieceDeleting"
        ok-text="确认删除"
        :ok-button-props="{ danger: true }"
        title="删除历史补录片"
        @ok="submitManualPieceDelete"
      >
        <div class="manual-piece-delete-panel">
          <div class="manual-piece-delete-summary">
            <strong>{{
              manualPieceDeleteTarget?.sliceBatchNo ||
              manualPieceDeleteTarget?.productionBatchNo ||
              '-'
            }}</strong>
            <span>型号：{{ manualPieceDeleteTarget?.modelCode || '-' }}</span>
            <span
              >料号：{{ manualPieceDeleteTarget?.materialCode || '-' }}</span
            >
          </div>
          <div
            v-if="Number(manualPieceDeleteTarget?.printCount || 0) > 0"
            class="manual-piece-delete-warning"
          >
            该片号已打印
            {{ manualPieceDeleteTarget?.printCount }}
            次。删除后请立即销毁或隔离旧标签，打印日志将继续保留。
          </div>
          <label class="manual-piece-delete-label">删除原因（必填）</label>
          <Textarea
            v-model:value="manualPieceDeleteReason"
            :maxlength="500"
            placeholder="例如：导入片号错误"
            :rows="4"
            show-count
          />
          <div class="manual-piece-delete-hint">
            仅待包装的历史补录片允许删除；已包装请先撤销包装，已入库数据禁止删除。
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="manualPieceImportVisible"
        :footer="null"
        title="批量导入历史片"
        width="680px"
      >
        <div class="manual-piece-import-panel">
          <div class="manual-piece-tip">
            导入只用于补录 MES
            上线前已有片号。系统会整批校验片号唯一性、分段批号、日期、FQC和COA，不校验是否存在启用产品BOM；任意一行失败，本批次均不写入。
          </div>
          <div class="manual-piece-import-steps">
            <div>
              <strong>1. 下载导入模板</strong>
              <span
                >分段批号必须填写；有效期可留空，由系统按生产日期加10个月减1天计算；模板不包含过程风险。</span
              >
              <Button
                :loading="manualPieceTemplateExporting"
                @click="downloadManualPieceImportTemplate"
              >
                <template #icon
                  ><IconifyIcon icon="lucide:download"
                /></template>
                下载导入模板
              </Button>
            </div>
            <div>
              <strong>2. 填写后选择文件导入</strong>
              <span
                >支持 .xlsx/.xls，文件不超过5MB，有效数据不超过2,000行。</span
              >
              <Button
                :loading="manualPieceImporting"
                type="primary"
                @click="selectManualPieceImportFile"
              >
                <template #icon><IconifyIcon icon="lucide:upload" /></template>
                选择Excel并导入
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      <PickerModal
        :config="productBomPickerConfig"
        :open="productBomPickerOpen"
        @close="productBomPickerOpen = false"
        @pick="handleProductBomPick"
      />

      <Modal
        v-model:open="piecePrintVisible"
        :confirm-loading="piecePrinting"
        :ok-button-props="{ disabled: pieceLabels.length === 0 }"
        :ok-text="piecePrintButtonText"
        :style="{ top: '0', paddingBottom: '0' }"
        title="片号批量打印"
        width="100vw"
        wrap-class-name="packaging-piece-print-fullscreen-modal"
        @ok="printPieceLabels"
      >
        <div class="piece-print-picker-filter">
          <Input
            v-model:value="piecePrintFilters.sliceBatchNo"
            allow-clear
            placeholder="输入或扫码片号，支持模糊搜索"
            @press-enter="resetPiecePrintCandidatePage"
          />
          <Select
            v-model:value="piecePrintFilters.sourceType"
            :options="[
              { label: '全部来源', value: '' },
              { label: '正常裁切片', value: 'CUT_ROUND_REPORT' },
              { label: '历史补录片', value: 'MANUAL_HISTORY' },
            ]"
            @change="resetPiecePrintCandidatePage"
          />
          <Select
            v-model:value="piecePrintFilters.businessStatus"
            :options="[
              { label: '全部业务状态', value: '' },
              { label: '待包装', value: 'WAIT_PACKAGING' },
              { label: '已包装', value: 'PACKED' },
              { label: '已入库/库存中', value: 'IN_STOCK' },
            ]"
            @change="resetPiecePrintCandidatePage"
          />
          <Button type="primary" @click="resetPiecePrintCandidatePage">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            搜索
          </Button>
          <Button
            :loading="piecePrintLoading"
            @click="loadPiecePrintCandidates"
          >
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            刷新候选
          </Button>
        </div>
        <div class="piece-print-picker-toolbar">
          <span
            >共
            {{ piecePrintCandidatePage.total }} 个候选片号；跨页保留选择，超过
            {{ PRINT_BATCH_MAX_COUNT }} 张将自动分批打印</span
          >
          <div>
            <Button size="small" @click="selectCurrentPiecePrintCandidatePage"
              >选择当前页</Button
            >
            <Button
              size="small"
              :disabled="pieceLabels.length === 0"
              @click="clearPiecePrintSelection"
              >清空已选</Button
            >
          </div>
        </div>
        <Table
          class="piece-print-candidate-table"
          :columns="piecePrintCandidateColumns"
          :data-source="piecePrintCandidates"
          :loading="piecePrintLoading"
          :pagination="false"
          :row-key="piecePrintTicketId"
          :scroll="{ x: 900, y: 'max(180px, calc(100vh - 580px))' }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <Checkbox
              v-if="column.key === 'selected'"
              :checked="isPiecePrintCandidateSelected(record)"
              @change="
                setPiecePrintCandidateSelected(record, $event.target.checked)
              "
            />
            <strong v-else-if="column.key === 'sliceBatchNo'">{{
              record.sliceBatchNo
            }}</strong>
            <span v-else-if="column.key === 'product'"
              >{{ record.modelCode || '-' }}<br />{{
                record.materialCode || '-'
              }}</span
            >
            <Tag
              v-else-if="column.key === 'source'"
              :color="record.historical ? 'orange' : 'blue'"
            >
              {{ record.historical ? '历史补录片' : '正常裁切片' }}
            </Tag>
            <span v-else-if="column.key === 'businessStatus'">{{
              pieceRecordStatusText(record.recordStatus)
            }}</span>
          </template>
        </Table>
        <Pagination
          v-model:current="piecePrintCandidatePage.pageNo"
          v-model:page-size="piecePrintCandidatePage.pageSize"
          :page-size-options="['10', '20', '50']"
          :total="piecePrintCandidatePage.total"
          class="piece-print-candidate-pagination"
          show-size-changer
          @change="changePiecePrintCandidatePage"
        />
        <div v-if="pieceLabels.length > 0" class="piece-print-card">
          <div class="piece-print-summary">
            <strong>待打印 {{ pieceLabels.length }} 张</strong>
          </div>
          <div class="piece-print-table">
            <div class="piece-print-table__head">
              <span>片号</span><span>型号 / 料号</span><span>来源</span
              ><span>状态</span><span>操作</span>
            </div>
            <div
              v-for="label in pieceLabels"
              :key="piecePrintTicketId(label)"
              class="piece-print-table__row"
            >
              <strong>{{ label.sliceBatchNo }}</strong>
              <span
                >{{ label.modelCode || '-' }}<br />{{
                  label.materialCode || '-'
                }}</span
              >
              <span
                ><Tag :color="label.historical ? 'orange' : 'blue'">{{
                  label.historical ? '历史补录片' : '原有报工片'
                }}</Tag></span
              >
              <span>{{ pieceRecordStatusText(label.recordStatus) }}</span>
              <span
                ><Button
                  danger
                  size="small"
                  type="link"
                  @click="removePiecePrintLabel(label)"
                  >移除</Button
                ></span
              >
            </div>
          </div>
          <div class="piece-print-consistency">
            标签规格和打印机路由保持分切定义：80×50
            mm、分切打印机；字段取包装片号标签模板，二维码与上方均为片号，下方为本次打印时间。
          </div>
        </div>
        <div v-else class="piece-print-empty">
          请直接从上方候选列表勾选片号。
        </div>
      </Modal>

      <Modal
        v-model:open="packageScanVisible"
        :footer="null"
        title="扫码选择包装片号"
        width="680px"
      >
        <section class="package-scan-panel">
          <div class="package-scan-tip">
            可连续扫描同段号、同质量、同来源的待包装流转单；完成选择后点击“确认选择”进入“确定包装”。支持当前标签二维码“计划号;片号”及纯片号。
          </div>
          <Input
            v-model:value="packageScanCode"
            allow-clear
            autofocus
            :placeholder="`请扫描第 ${packageScanRows.length + 1} 片`"
            @press-enter="scanPackagePiece"
          >
            <template #prefix><IconifyIcon icon="lucide:scan-line" /></template>
            <template #addonAfter>
              <Button
                :loading="packageScanResolving"
                type="link"
                @click="scanPackagePiece"
                >加入</Button
              >
            </template>
          </Input>
          <div class="package-scan-progress">
            已扫码 <strong>{{ packageScanRows.length }}</strong> 片
          </div>
          <div v-if="packageScanRows.length > 0" class="package-scan-list">
            <div
              v-for="row in packageScanRows"
              :key="getWaitPieceKey(row)"
              class="package-scan-row"
            >
              <div>
                <strong>{{
                  row.productionBatchNo || row.sliceBatchNo || '-'
                }}</strong>
                <span
                  >{{ row.modelCode || '-' }} /
                  {{ row.materialCode || '-' }}</span
                >
                <span>段号：{{ resolvePackageSegmentNo(row) || '-' }}</span>
              </div>
              <div class="package-scan-row__actions">
                <Tag :color="resultColor(resolvePackagingQualityStatus(row))">
                  {{ resolvePackagingQualityStatus(row) }}
                </Tag>
                <Button
                  danger
                  size="small"
                  type="link"
                  @click="removePackageScanPiece(row)"
                  >移除</Button
                >
              </div>
            </div>
          </div>
          <div v-else class="package-scan-empty">
            请扫描第一片待包装流转单。
          </div>
          <div class="package-scan-footer">
            <span
              >扫码仅选择片号；提交包装时仍会执行点检、状态和辅材余量校验。</span
            >
            <div class="gap-8px flex">
              <Button
                :disabled="packageScanRows.length === 0"
                @click="resetPackageScan"
                >清空</Button
              >
              <Button
                :disabled="packageScanRows.length === 0"
                type="primary"
                @click="confirmPackageScanSelection"
                >确认选择</Button
              >
            </div>
          </div>
        </section>
      </Modal>

      <Modal
        v-model:open="packageVisible"
        :confirm-loading="packaging"
        title="确定包装"
        width="660px"
        @ok="submitPackage"
      >
        <div class="fg-package-form">
          <div class="fg-form-tip">
            已选择 <strong>{{ selectedRowKeys.length }}</strong> 片，合格
            {{ qualifiedSelectedCount }} / 冻结 {{ frozenSelectedCount }} / 不合格
            {{ unqualifiedSelectedCount }}。本次将生成
            {{ selectedRowKeys.length }} 个单片包装单，每个包装单仅含 1 片。
          </div>
          <label>包装编号</label>
          <Input
            v-if="selectedRowKeys.length === 1"
            v-model:value="packageForm.packageNo"
            allow-clear
            placeholder="为空时自动生成：yyyyMMdd-两位流水"
          />
          <Input v-else value="批量包装由系统自动生成连续编号" disabled />
          <label>包装辅材</label>
          <div class="package-aux-lines">
            <div
              v-for="(item, index) in packageForm.auxConsumeItems"
              :key="index"
              class="package-aux-line"
            >
              <Select
                v-model:value="item.ledgerId"
                allow-clear
                class="package-aux-batch-select"
                :loading="packageAuxLoading"
                :options="packageAuxOptions"
                placeholder="选择包装工序耗材领用台账批次"
                show-search
              />
              <div class="package-aux-quantity-field">
                <span>可用量</span>
                <Input
                  :value="getPackageAuxAvailableQty(item.ledgerId)"
                  class="w-full"
                  disabled
                  placeholder="-"
                />
              </div>
              <div class="package-aux-quantity-field">
                <span>本次领用量</span>
                <InputNumber
                  v-model:value="item.consumeQty"
                  :min="1"
                  :precision="0"
                  class="w-full"
                  placeholder="请输入实际领用数量"
                />
              </div>
            </div>
            <span
              >从“包装工序耗材领用台账”选择实际批次；本次领用量由现场手工填写，提交时按实时可用结存校验并扣减。</span
            >
          </div>
          <label>备注</label>
          <Textarea
            v-model:value="packageForm.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="包装备注"
          />
        </div>
      </Modal>

      <Modal v-model:open="detailVisible" title="包装明细" width="760px">
        <div v-if="currentPackage" class="fg-package-detail">
          <div class="fg-detail-head">
            <strong>{{ currentPackage.boxNo }}</strong>
            <Tag :color="statusColor(currentPackage.status)">{{
              statusText(currentPackage.status)
            }}</Tag>
          </div>
          <section class="package-fg-modal-grid-panel">
            <DetailGrid table-title="包装明细">
              <template #detailQualityStatus="{ row }">
                <Tag :color="resultColor(row.qualityStatus)">{{
                  row.qualityStatus === 'FROZEN' ? '冻结' : row.qualityStatus || '-'
                }}</Tag>
              </template>
            </DetailGrid>
          </section>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.fg-packaging-workbench .console-action-group {
  flex-wrap: wrap;
}

.fg-packaging-workbench {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.package-scan-panel {
  display: grid;
  gap: 12px;
}

.package-scan-tip,
.package-scan-progress,
.package-scan-empty,
.package-scan-footer {
  color: #475569;
  font-size: 13px;
}

.package-scan-tip {
  padding: 10px 12px;
  line-height: 1.6;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.package-scan-progress strong {
  color: #1d4ed8;
  font-size: 17px;
}

.package-scan-list {
  display: grid;
  gap: 8px;
}

.package-scan-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.package-scan-row > div:first-child {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 4px 14px;
  align-items: center;
  min-width: 0;
}

.package-scan-row strong {
  color: #0f172a;
}

.package-scan-row span {
  color: #64748b;
  font-size: 12px;
}

.package-scan-row__actions,
.package-scan-footer {
  display: flex;
  gap: 8px;
  align-items: center;
}

.package-scan-footer {
  justify-content: space-between;
}

.packaging-record-toggle {
  padding: 0;
  border: 0;
  cursor: pointer;
  transition:
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.packaging-record-toggle:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.32);
}

.packaging-record-toggle.is-active {
  background: linear-gradient(135deg, #0f766e, #2563eb);
  box-shadow:
    0 0 0 2px rgba(13, 148, 136, 0.26),
    0 10px 20px rgba(15, 118, 110, 0.32);
}

.packaging-record-toggle:focus-visible {
  outline: 2px solid #0ea5e9;
  outline-offset: 2px;
}

.packaging-daily-check-modal-head,
.packaging-daily-check-modal-footer {
  display: flex;
  gap: 8px;
  align-items: center;
}

.packaging-daily-check-modal-head {
  justify-content: space-between;
  padding-bottom: 12px;
  color: #475569;
  font-size: 13px;
  border-bottom: 1px solid #d8e0ea;
}

.packaging-daily-check-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 0;
}

.packaging-daily-check-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 136px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.packaging-daily-check-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  color: #0369a1;
  font-size: 25px;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
  border-radius: 4px;
}

.packaging-daily-check-card__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.packaging-daily-check-card__content strong {
  color: #0f172a;
  font-weight: 800;
}

.packaging-daily-check-card__content em,
.packaging-daily-check-card__content span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.packaging-daily-check-card__tag {
  align-self: start;
  margin: 0;
}

.packaging-daily-check-card__actions {
  display: flex;
  grid-column: 2 / 4;
  gap: 8px;
  justify-content: flex-end;
}

.packaging-daily-check-empty {
  padding: 32px 0;
  color: #64748b;
  text-align: center;
}

.packaging-daily-check-modal-footer {
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #d8e0ea;
}

.piece-label-entry {
  margin-left: 10px;
  color: #0f766e;
  font-weight: 800;
  border-color: #0f766e;
}

.action-tile-history {
  background: linear-gradient(135deg, #9a3412, #ea580c);
}

.action-tile-history-import {
  background: linear-gradient(135deg, #6b21a8, #9333ea);
}

.fg-query-bar,
.fg-package-toolbar,
.fg-package-check-toolbar,
.fg-aux-toolbar {
  display: grid;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.fg-query-bar,
.fg-package-toolbar {
  grid-template-columns: minmax(0, 1fr) 128px;
}

.fg-tab-toolbar-stack {
  display: grid;
  gap: 8px;
}

.fg-segment-print-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  color: #475569;
  font-size: 12px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.fg-segment-print-toolbar > div {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.fg-package-check-toolbar {
  grid-template-columns: 180px 96px 112px minmax(0, 1fr);
}

.fg-package-check-toolbar :deep(.ant-picker),
.fg-package-check-toolbar :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.fg-aux-toolbar {
  grid-template-columns: 180px 110px 128px minmax(0, 1fr);
}

.fg-aux-toolbar :deep(.ant-picker),
.fg-aux-toolbar :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.fg-packaging-tabs {
  display: flex;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.fg-packaging-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.fg-packaging-tabs :deep(.ant-tabs-content-holder),
.fg-packaging-tabs :deep(.ant-tabs-content),
.fg-packaging-tabs :deep(.ant-tabs-tabpane) {
  width: 100%;
  max-width: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.fg-packaging-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
}

.fg-packaging-tabs :deep(.ant-tabs-content),
.fg-packaging-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.package-fg-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.package-fg-grid-panel :deep(.vben-vxe-grid),
.package-fg-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.package-fg-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.package-fg-grid-panel :deep(.package-wait-segment-row) {
  background: #eef6ff;
}

.package-wait-segment-cell {
  display: grid;
  gap: 2px;
  min-width: 0;
  line-height: 1.2;
}

.package-wait-segment-cell strong,
.package-wait-segment-cell em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-wait-segment-cell strong {
  color: #075985;
  font-weight: 700;
}

.package-wait-segment-cell em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.package-wait-piece-no {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.fg-tab-panel {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.fg-row-actions {
  display: flex;
  gap: 4px;
  white-space: nowrap;
}

.fg-package-form {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.fg-package-form label {
  color: #4b5563;
  font-weight: 700;
  text-align: right;
}

.manual-piece-tip,
.piece-print-consistency,
.piece-print-empty {
  padding: 10px 12px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
}

.manual-piece-form {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-top: 14px;
}

.manual-piece-form > label {
  color: #475569;
  font-weight: 700;
  text-align: right;
}

.manual-piece-form label i {
  margin-right: 3px;
  color: #dc2626;
  font-style: normal;
}

.manual-piece-product {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) max-content;
  gap: 8px;
}

.manual-piece-delete-panel,
.manual-piece-delete-summary {
  display: grid;
  gap: 10px;
}

.manual-piece-delete-summary {
  grid-template-columns: minmax(0, 1fr) max-content max-content;
  align-items: center;
  padding: 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.manual-piece-delete-summary strong {
  color: #0f172a;
  font-size: 18px;
}

.manual-piece-delete-warning {
  padding: 10px 12px;
  color: #9a3412;
  background: #fff7ed;
  border: 1px solid #fdba74;
}

.manual-piece-delete-label {
  color: #475569;
  font-weight: 700;
}

.manual-piece-delete-hint {
  color: #64748b;
  font-size: 12px;
}

.manual-piece-import-panel,
.manual-piece-import-steps,
.manual-piece-import-steps > div {
  display: grid;
  gap: 12px;
}

.manual-piece-import-steps {
  margin-top: 16px;
}

.manual-piece-import-steps > div {
  grid-template-columns: minmax(0, 1fr) max-content;
  align-items: center;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.manual-piece-import-steps strong,
.manual-piece-import-steps span {
  grid-column: 1;
}

.manual-piece-import-steps strong {
  color: #0f172a;
}

.manual-piece-import-steps span {
  color: #64748b;
  font-size: 12px;
}

.manual-piece-import-steps :deep(.ant-btn) {
  grid-row: 1 / span 2;
  grid-column: 2;
}

:global(.manual-piece-import-failures pre) {
  max-height: 360px;
  padding: 10px;
  margin: 10px 0 0;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.piece-print-picker-filter {
  display: grid;
  grid-template-columns:
    minmax(240px, 1.4fr) repeat(2, minmax(0, 1fr))
    max-content max-content;
  gap: 8px;
  align-items: center;
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal) {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0;
  padding-bottom: 0;
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal-header) {
  flex: 0 0 auto;
  padding: 14px 20px;
  margin-bottom: 0;
  border-bottom: 1px solid #d9e2ef;
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 16px 20px;
  overflow: auto;
  background: #f7fafe;
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal-footer) {
  flex: 0 0 auto;
  padding: 12px 20px;
  margin-top: 0;
  background: #fff;
  border-top: 1px solid #d9e2ef;
  box-shadow: 0 -6px 16px rgb(15 39 72 / 8%);
}

:global(.packaging-piece-print-fullscreen-modal .ant-modal-close) {
  top: 12px;
  right: 18px;
}

.piece-print-picker-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  margin-top: 10px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.piece-print-picker-toolbar > div {
  display: flex;
  gap: 8px;
}

.piece-print-candidate-table {
  margin-top: 10px;
  border: 1px solid #cbd5e1;
}

.piece-print-candidate-table :deep(.ant-table-cell) {
  vertical-align: middle;
}

.piece-print-candidate-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.piece-print-card {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.piece-print-reprint-reason {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 10px 12px;
  margin-top: 12px;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.piece-print-reprint-reason > label {
  padding-top: 5px;
  font-weight: 700;
}

.piece-print-summary {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 10px 12px;
  color: #475569;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
}

.piece-print-summary strong {
  color: #0f172a;
}

.piece-print-table {
  max-height: 240px;
  overflow: auto;
  border: 1px solid #cbd5e1;
}

.piece-print-table__head,
.piece-print-table__row {
  display: grid;
  grid-template-columns:
    minmax(180px, 1.4fr) minmax(180px, 1.2fr)
    110px 120px 64px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
}

.piece-print-table__head {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #475569;
  font-weight: 800;
  background: #e2e8f0;
}

.piece-print-table__row {
  min-height: 52px;
  color: #475569;
  border-top: 1px solid #e2e8f0;
}

.piece-print-table__row strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
}

.piece-print-card__head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.piece-print-card__head > strong {
  color: #0f172a;
  font-size: 20px;
}

.piece-print-fields {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 86px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.piece-print-fields > span {
  color: #64748b;
  text-align: right;
}

.piece-print-fields > strong {
  min-width: 0;
  overflow: hidden;
  color: #1e293b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.piece-print-empty {
  margin-top: 14px;
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.package-aux-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.package-aux-line {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
}

.package-aux-batch-select {
  grid-column: 1 / -1;
  width: 100%;
}

.package-aux-quantity-field {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.package-aux-quantity-field > span {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  text-align: right;
  white-space: nowrap;
}

.package-aux-lines > span {
  color: #64748b;
  font-size: 12px;
}

.fg-form-tip {
  grid-column: 1 / -1;
  padding: 10px 12px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
}

.fg-detail-head {
  display: flex;
  gap: 8px;
  align-items: center;
}

.fg-detail-head {
  justify-content: space-between;
  margin-bottom: 10px;
}

:deep(.packaging-dev-process-form-view-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
  margin: 0;
}

:deep(.packaging-dev-process-form-view-modal .ant-modal-content) {
  height: 100vh;
  padding: 0;
  overflow: hidden;
  background: #f8fafc;
  border-radius: 0;
}

:deep(.packaging-dev-process-form-view-modal .ant-modal-body) {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.packaging-dev-runtime-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  color: #0f172a;
  background: #eef2f7;
}

.packaging-dev-runtime-view__header {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 10px 16px;
  background: #0f172a;
  border-bottom: 1px solid #1e293b;
}

.packaging-dev-runtime-view__header h3 {
  margin: 0;
  color: #f8fafc;
  font-size: 18px;
  font-weight: 900;
}

.packaging-dev-runtime-view__meta,
.packaging-dev-runtime-view__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.packaging-dev-runtime-view__meta {
  flex-wrap: wrap;
  margin-top: 4px;
  color: #cbd5e1;
  font-size: 12px;
}

.packaging-dev-runtime-view__actions {
  flex: 0 0 auto;
}

.packaging-dev-runtime-view__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  background: #f8fafc;
}

.packaging-dev-runtime-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 240px;
  color: #64748b;
  font-weight: 800;
}

.package-fg-modal-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  height: 340px;
  min-height: 0;
  overflow: hidden;
}

.package-fg-modal-grid-panel :deep(.vben-vxe-grid),
.package-fg-modal-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-modal-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-modal-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-modal-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}
</style>
