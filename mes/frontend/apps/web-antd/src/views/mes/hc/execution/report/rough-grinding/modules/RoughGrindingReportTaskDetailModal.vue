<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { computed, onBeforeUnmount, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { useQRCode } from '@vueuse/integrations/useQRCode';
import {
  Button,
  Checkbox,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal as AModal,
  Pagination,
  Radio,
  RadioGroup,
  Select,
  Tabs,
  TabPane,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getEquipment, getEquipmentPage } from '#/api/mes/hc/equipment';
import {
  saveRoughGrindingProgress,
  startRoughGrindingReport,
  switchRoughGrindingEquipment,
} from '#/api/mes/hc/execution/rough-grinding-report';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import {
  updateToolingConsumableLedgerUsageStatus,
} from '#/api/mes/hc/tooling-consumable-ledger';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';
import ConsumableLedgerSwitchModal from '#/views/mes/hc/base/tooling-consumable-ledger/components/ConsumableLedgerSwitchModal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import FormulaReportPrintPreviewModal from '../../formula/modules/FormulaReportPrintPreviewModal.vue';
import RoughGrindingFirstInspectionPrintPreviewModal from './RoughGrindingFirstInspectionPrintPreviewModal.vue';
import {
  buildTransferTicketQrValue,
  resolveTransferTicketQrBusinessNo,
} from '../../shared/workOrderTicketPrint';

const emit = defineEmits(['refresh', 'taskChange']);

const WORK_ORDER_PRINT_DOC_CONFIG = {
  department: '材料研发中心',
  docCode: 'HC/R-21-0XX',
  publishDate: '2023.11.1',
  retention: '10年',
  title: 'CMP软垫制造工单',
  version: 'A/0',
};

const WORK_ORDER_PROCESS_CONFIG: Record<string, { metricLabel?: string }> = {
  分切: { metricLabel: '合格数量(片)' },
  压槽: { metricLabel: '合格数量(片)' },
  成品入库: { metricLabel: '入库数量(片)' },
  湿法: { metricLabel: '收卷米数(m)' },
  磨皮: { metricLabel: '磨皮米数(m)' },
  粘胶1: { metricLabel: '生产米数(m)' },
  粘胶2: { metricLabel: '合格数量(片)' },
  裁切: { metricLabel: '合格片数(片)' },
  配料: {},
};

type MainTabKey =
  | 'prepare'
  | 'first-grinding'
  | 'second-grinding'
  | 'abnormal-position'
  | 'production-check'
  | 'semi-finished'
  | 'stock-ledger'
  | 'first-inspection'
  | 'report-info';
type RecordMode = 'confirm' | 'edit' | 'view';
type RecordCategory = 'prepare' | 'production-check' | 'semi-finished';

interface WetSheetDetail {
  category?: string;
  confirmer?: string;
  inspector?: string;
  node?: string;
  placeholder?: boolean;
  pointResult?: string;
  seq: number;
  item?: string;
  standard?: string;
  value?: string;
  result?: string;
  remark?: string;
  segment?: string;
  length?: number;
}

interface WetSheetRow {
  finalResult?: string;
  id: string;
  name: string;
  poreDevelopment?: string;
  timing?: string;
  status: 'COMPLETED' | 'PENDING' | 'WAITING';
  result?: string;
  recorder?: string;
  recorderTime?: string;
  confirmer?: string;
  confirmerTime?: string;
  remark?: string;
  semiWidth?: string;
  generatedLength?: number;
  details?: WetSheetDetail[];
}

interface RoughGrindingCheckItem {
  abnormalRemark: string;
  actualValue: string;
  checkResult: 'NG' | 'OK' | string;
  itemCategory: string;
  itemName: string;
  sortNo: number;
  standardValue: string;
}

interface RoughGrindingAbnormalPositionRow {
  abnormalLength?: number;
  clientKey: string;
  positionText: string;
  remark?: string;
  sortOrder?: number;
  sourceBatchNo?: string;
  sourcePlanNo?: string;
  sourceRowId?: string;
}

interface RoughMaterialUsageRow {
  id: string;
  batchNo: string;
  planNo: string;
  remainStartMeter: number;
  remainLength: number;
  processLength: number;
  lossLength: number;
  outputLength: number;
  napSampleLength: number;
  grindingPass: '1次' | '2次';
  startTime: string;
  endTime: string;
  sandpaperLife?: number;
  sandpaperLifeDays?: number;
  sandpaperBatchNo?: string;
  sandpaperLedgerBatchNo?: string;
  sandpaperLedgerId?: number;
  sandpaperNextUsageStatus?: 'ACTIVE' | 'USED_UP';
  pressure?: string;
  lineSpeed?: string;
  rotationSpeed?: string;
  meterCounter?: string;
  grindingThickness?: string;
  afterGrindingThickness?: string;
  defectCode?: string;
  qualityThickness?: string;
  qualityWidth?: string;
  grindingMeters?: number;
  selfCheck?: 'NG' | 'OK';
  sourceType: '扫码母料' | '边库余料';
  checkItems?: RoughGrindingCheckItem[];
  abnormalPositions?: RoughGrindingAbnormalPositionRow[];
}

interface RoughSegmentRow {
  id: string;
  parentId: string;
  batchNo: string;
  segmentNo: string;
  startMeter: number;
  length: number;
  selfCheck: 'OK' | 'NG';
  thickness?: number;
  width?: number;
  selected?: boolean;
  status: 'PENDING' | 'STORED';
}

interface RoughStockLedgerRow {
  id: string;
  ledgerNo: string;
  status: '已产出' | '已入边库';
  batchNo: string;
  defectCode?: string;
  lossLength?: number;
  napSampleLength?: number;
  outputLength?: number;
  processLength?: number;
  secondGrindingRowId?: string;
  segmentNo: string;
  sourceRowId?: string;
  startMeter: number;
  length: number;
  machineCode: string;
  outputTime: string;
  selfCheck: 'OK' | 'NG';
  thickness?: number;
  width?: number;
  sourceType: '产出子卷' | '边库余料' | '二次磨皮产出';
}

interface RoughSecondGrindingRow {
  id: string;
  sourceRowId: string;
  sourceType?: '扫码母料' | '边库余料';
  batchNo: string;
  productionBatchNo: string;
  processLength: number;
  lossLength: number;
  outputLength: number;
  napSampleLength: number;
  segmentMark: string;
  startTime: string;
  endTime: string;
  sandpaperLife?: number;
  sandpaperLifeDays?: number;
  sandpaperBatchNo?: string;
  sandpaperLedgerBatchNo?: string;
  sandpaperLedgerId?: number;
  sandpaperNextUsageStatus?: 'ACTIVE' | 'USED_UP';
  pressure?: string;
  lineSpeed?: string;
  rotationSpeed?: string;
  meterCounter?: string;
  grindingThickness?: string;
  afterGrindingThickness?: string;
  defectCode?: string;
  qualityThickness?: string;
  qualityWidth?: string;
  grindingMeters?: number;
  selfCheck?: 'NG' | 'OK';
  selectedForPrint?: boolean;
  printStatus?: '已打印' | '未打印';
  printTime?: string;
  printCount?: number;
  confirmStatus?: '已确认' | '未确认';
  confirmTime?: string;
  confirmedBatchNo?: string;
  checkItems?: RoughGrindingCheckItem[];
}

const task = ref<any>(null);
const activeMainTab = ref<MainTabKey>('prepare');
const viewMode = ref<'booking' | 'tabs'>('tabs');
const materialUsageMaximized = ref(false);
const pendingAction = ref<'FINISH' | 'START' | null>(null);
const authVisible = ref(false);
const recordConfirmAuthVisible = ref(false);
const equipmentOptions = ref<any[]>([]);
const deviceSwitchVisible = ref(false);
const deviceSwitchSubmitting = ref(false);
const deviceSwitchForm = ref({ equipmentId: undefined as number | undefined });
const equipmentStatusCard = ref<any>(null);
const firstInspectionTimer = ref<ReturnType<typeof setTimeout> | null>(null);
const roughProgressPersistTimer = ref<ReturnType<typeof setTimeout> | null>(null);
const SEMI_DETAIL_PAGE_SIZE = 50;
const semiDetailPage = ref(1);

const reportForm = ref({
  productionDate: '',
  startTime: '',
  endTime: '',
  equipmentId: undefined as number | undefined,
  equipmentCode: '',
  equipmentName: '',
  inputLength: undefined as number | undefined,
  outputLength: undefined as number | undefined,
  grindingPass: '一次',
  sandpaperLife: undefined as number | undefined,
  sandpaperBatchNo: '',
  guideClothBatchNo: '',
  guideClothLedgerBatchNo: '',
  guideClothLedgerId: undefined as number | undefined,
  guideClothNextUsageStatus: undefined as 'ACTIVE' | 'USED_UP' | undefined,
  guideClothLifeCount: undefined as number | undefined,
  changeReason: '',
  remark: '',
  recorderName: '',
  recorderTime: '',
  confirmerName: '',
  confirmerTime: '',
});

const grindingDefectOptions = [
  { label: '无', value: '' },
  { label: '厚度异常', value: 'THICKNESS' },
  { label: '宽幅异常', value: 'WIDTH' },
  { label: '表面异常', value: 'SURFACE' },
  { label: '磨皮异常', value: 'GRINDING' },
];
const grindingSelfCheckOptions = [
  { label: 'OK', value: 'OK' },
  { label: 'NG', value: 'NG' },
];
const segmentMarkOptions = [
  { label: '空', value: '' },
  { label: 'P', value: 'P' },
  { label: 'Q', value: 'Q' },
  { label: 'R', value: 'R' },
  { label: 'S', value: 'S' },
];
const ROUGH_GRINDING_CHECK_CATEGORY_ORDER = ['一次磨皮', '二次磨皮', '磨皮后Nap层'];
const ROUGH_GRINDING_DEFAULT_STANDARDS: Record<string, string> = {
  '一次磨皮::砂纸累计寿命': '累计磨皮≤500m，且≤15天',
  '一次磨皮::气压': '0.15±0.05MPa',
  '一次磨皮::线速': '0.60±0.10m/min',
  '一次磨皮::转速': '900±100r/min',
  '一次磨皮::计米器': '料皮驶入磨皮机时归零',
  '一次磨皮::磨皮厚度': '0.960±0.020mm',
  '一次磨皮::磨皮后厚度': '1.000±0.050mm',
  '二次磨皮::砂纸累计寿命': '累计磨皮≤500m，且≤15天',
  '二次磨皮::气压': '0.15±0.05MPa',
  '二次磨皮::磨皮厚度': '0.865±0.030mm',
  '二次磨皮::线速': '0.55±0.05m/min',
  '二次磨皮::转速': '900±100r/min',
  '二次磨皮::计米器': '料皮驶入磨皮机时归零',
  '磨皮后Nap层::厚度': '0.914±0.030mm',
  '磨皮后Nap层::磨后宽幅': '1.04±0.04m',
  '磨皮后Nap层::磨皮米数': '/',
};

const workPrepareRows = ref<WetSheetRow[]>([]);
const productionCheckRows = ref<WetSheetRow[]>([]);
const semiFinishedRows = ref<WetSheetRow[]>([]);
const materialUsageRows = ref<RoughMaterialUsageRow[]>([]);
const secondGrindingRows = ref<RoughSecondGrindingRow[]>([]);
const segmentRows = ref<RoughSegmentRow[]>([]);
const stockLedgerRows = ref<RoughStockLedgerRow[]>([]);
const materialScanVisible = ref(false);
const materialScanActiveTab = ref('report');
const materialScanCheckItems = ref<RoughGrindingCheckItem[]>([]);
const materialScanAbnormalRows = ref<RoughGrindingAbnormalPositionRow[]>([]);
const materialScanForm = ref({
  code: '',
  batchNo: '',
  planNo: '',
  remainStartMeter: 0,
  remainLength: 0,
  processLength: undefined as number | undefined,
  lossLength: undefined as number | undefined,
  outputLength: undefined as number | undefined,
  napSampleLength: undefined as number | undefined,
  grindingPass: '1次' as '1次' | '2次',
  startTime: '',
  endTime: '',
  sandpaperLife: undefined as number | undefined,
  sandpaperLifeDays: undefined as number | undefined,
  sandpaperBatchNo: '',
  sandpaperLedgerBatchNo: '',
  sandpaperLedgerId: undefined as number | undefined,
  sandpaperNextUsageStatus: undefined as 'ACTIVE' | 'USED_UP' | undefined,
  pressure: '',
  lineSpeed: '',
  rotationSpeed: '',
  meterCounter: '',
  grindingThickness: '',
  afterGrindingThickness: '',
  defectCode: '',
  qualityThickness: '',
  qualityWidth: '',
  grindingMeters: undefined as number | undefined,
  selfCheck: 'OK' as 'NG' | 'OK',
  sourceType: '扫码母料' as '扫码母料' | '边库余料',
});
const secondGrindingVisible = ref(false);
const secondGrindingActiveTab = ref('report');
const secondGrindingEditRowId = ref('');
const secondGrindingCheckItems = ref<RoughGrindingCheckItem[]>([]);
const secondGrindingSelectionVersion = ref(0);
const secondGrindingConfirmVisible = ref(false);
const secondGrindingConfirmForm = ref({
  rowId: '',
  scanCode: '',
  error: '',
  message: '',
});
const secondGrindingForm = ref({
  sourceCode: '',
  sourceRowId: '',
  sourceType: '' as '' | '扫码母料' | '边库余料',
  batchNo: '',
  productionBatchNo: '',
  processLength: undefined as number | undefined,
  lossLength: undefined as number | undefined,
  outputLength: undefined as number | undefined,
  napSampleLength: undefined as number | undefined,
  segmentMark: '',
  startTime: '',
  endTime: '',
  sandpaperLife: undefined as number | undefined,
  sandpaperLifeDays: undefined as number | undefined,
  sandpaperBatchNo: '',
  sandpaperLedgerBatchNo: '',
  sandpaperLedgerId: undefined as number | undefined,
  sandpaperNextUsageStatus: undefined as 'ACTIVE' | 'USED_UP' | undefined,
  pressure: '',
  lineSpeed: '',
  rotationSpeed: '',
  meterCounter: '',
  grindingThickness: '',
  afterGrindingThickness: '',
  defectCode: '',
  qualityThickness: '',
  qualityWidth: '',
  grindingMeters: undefined as number | undefined,
  selfCheck: 'OK' as 'NG' | 'OK',
});
type RoughSideConsumableType = 'GUIDE_CLOTH' | 'SANDPAPER';
type RoughConsumableLedgerSelection = MesHcToolingConsumableLedgerApi.Ledger & {
  nextUsageStatus?: 'ACTIVE' | 'USED_UP';
};
type RoughSandpaperSelectionTarget = {
  sandpaperBatchNo?: string;
  sandpaperLedgerBatchNo?: string;
  sandpaperLedgerId?: number;
  sandpaperNextUsageStatus?: 'ACTIVE' | 'USED_UP';
};
type RoughConsumableApply = (row: RoughConsumableLedgerSelection) => void;
const roughConsumableSwitchModalRef = ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const roughConsumableApply = ref<RoughConsumableApply>();

function openRoughConsumableLedger(
  consumableType: RoughSideConsumableType,
  apply: RoughConsumableApply,
) {
  roughConsumableApply.value = apply;
  roughConsumableSwitchModalRef.value?.open({
    consumableType,
    processCode: 'ROUGH_GRINDING',
    title: consumableType === 'SANDPAPER' ? '磨皮砂纸边库台账' : '磨皮导布边库台账',
  });
}

function handleRoughConsumableSelected(row: RoughConsumableLedgerSelection) {
  roughConsumableApply.value?.(row);
}

function applySandpaperLedgerSelection(
  target: RoughSandpaperSelectionTarget,
  row: RoughConsumableLedgerSelection,
) {
  target.sandpaperBatchNo = row.batchNo || '';
  target.sandpaperLedgerBatchNo = row.batchNo || '';
  target.sandpaperLedgerId = row.id;
  target.sandpaperNextUsageStatus = row.nextUsageStatus;
}

function clearSandpaperLedgerSelection(target: RoughSandpaperSelectionTarget) {
  target.sandpaperLedgerBatchNo = '';
  target.sandpaperLedgerId = undefined;
  target.sandpaperNextUsageStatus = undefined;
}

function clearReportGuideClothSelection() {
  reportForm.value.guideClothLedgerBatchNo = '';
  reportForm.value.guideClothLedgerId = undefined;
  reportForm.value.guideClothNextUsageStatus = undefined;
}

function handleFirstGrindingSandpaperBatchInput(row: RoughMaterialUsageRow) {
  clearSandpaperLedgerSelection(row);
  handleMaterialUsageChange(row);
}

function handleSecondGrindingSandpaperBatchInput(row: RoughSecondGrindingRow) {
  clearSandpaperLedgerSelection(row);
  handleSecondGrindingRowChange(row);
}

function handleMaterialScanSandpaperBatchInput() {
  clearSandpaperLedgerSelection(materialScanForm.value);
}

function handleSecondGrindingFormSandpaperBatchInput() {
  clearSandpaperLedgerSelection(secondGrindingForm.value);
}

function selectFirstGrindingSandpaper(row: RoughMaterialUsageRow) {
  openRoughConsumableLedger('SANDPAPER', (ledger) => {
    applySandpaperLedgerSelection(row, ledger);
    handleMaterialUsageChange(row);
  });
}

function selectSecondGrindingSandpaper(row: RoughSecondGrindingRow) {
  openRoughConsumableLedger('SANDPAPER', (ledger) => {
    applySandpaperLedgerSelection(row, ledger);
    handleSecondGrindingRowChange(row);
  });
}

function selectMaterialScanSandpaper() {
  openRoughConsumableLedger('SANDPAPER', (ledger) => {
    applySandpaperLedgerSelection(materialScanForm.value, ledger);
  });
}

function selectSecondGrindingFormSandpaper() {
  openRoughConsumableLedger('SANDPAPER', (ledger) => {
    applySandpaperLedgerSelection(secondGrindingForm.value, ledger);
  });
}

function selectReportGuideCloth() {
  openRoughConsumableLedger('GUIDE_CLOTH', (ledger) => {
    reportForm.value.guideClothBatchNo = ledger.batchNo || '';
    reportForm.value.guideClothLedgerBatchNo = ledger.batchNo || '';
    reportForm.value.guideClothLedgerId = ledger.id;
    reportForm.value.guideClothNextUsageStatus = ledger.nextUsageStatus;
  });
}

function addUsedUpLedgerId(target: Set<number>, ledgerId?: number, nextUsageStatus?: string) {
  if (ledgerId && nextUsageStatus === 'USED_UP') {
    target.add(ledgerId);
  }
}

function clearUpdatedRoughConsumableUsageStatus(updatedLedgerIds: Set<number>) {
  if (reportForm.value.guideClothLedgerId && updatedLedgerIds.has(reportForm.value.guideClothLedgerId)) {
    reportForm.value.guideClothNextUsageStatus = undefined;
  }
  materialUsageRows.value.forEach((row) => {
    if (row.sandpaperLedgerId && updatedLedgerIds.has(row.sandpaperLedgerId)) {
      row.sandpaperNextUsageStatus = undefined;
    }
  });
  secondGrindingRows.value.forEach((row) => {
    if (row.sandpaperLedgerId && updatedLedgerIds.has(row.sandpaperLedgerId)) {
      row.sandpaperNextUsageStatus = undefined;
    }
  });
}

async function updateRoughConsumableUsageStatusesAfterBooking() {
  const ledgerIds = new Set<number>();
  addUsedUpLedgerId(
    ledgerIds,
    reportForm.value.guideClothLedgerId,
    reportForm.value.guideClothNextUsageStatus,
  );
  materialUsageRows.value.forEach((row) => {
    addUsedUpLedgerId(ledgerIds, row.sandpaperLedgerId, row.sandpaperNextUsageStatus);
  });
  secondGrindingRows.value.forEach((row) => {
    addUsedUpLedgerId(ledgerIds, row.sandpaperLedgerId, row.sandpaperNextUsageStatus);
  });
  if (ledgerIds.size === 0) return;
  for (const ledgerId of ledgerIds) {
    await updateToolingConsumableLedgerUsageStatus(ledgerId, 'USED_UP');
  }
  clearUpdatedRoughConsumableUsageStatus(ledgerIds);
}
const firstInspection = ref({
  inspectionDesc: '',
  inspectTime: '',
  inspector: '',
  result: '',
  status: 'PENDING' as 'COMPLETED' | 'PENDING' | 'WAITING',
});

const firstGrindingEditableFields = [
  'batchNo',
  'startTime',
  'endTime',
  'sandpaperLife',
  'sandpaperLifeDays',
  'sandpaperBatchNo',
  'pressure',
  'lineSpeed',
  'rotationSpeed',
  'meterCounter',
  'grindingThickness',
  'afterGrindingThickness',
  'selfCheck',
  'defectCode',
] as const;
type FirstGrindingEditableField = (typeof firstGrindingEditableFields)[number];

const secondGrindingEditableFields = [
  'segmentMark',
  'startTime',
  'endTime',
  'sandpaperLife',
  'sandpaperLifeDays',
  'sandpaperBatchNo',
  'pressure',
  'lineSpeed',
  'rotationSpeed',
  'meterCounter',
  'grindingThickness',
  'afterGrindingThickness',
  'selfCheck',
  'defectCode',
  'qualityThickness',
  'qualityWidth',
  'grindingMeters',
] as const;
type SecondGrindingEditableField = (typeof secondGrindingEditableFields)[number];

function roundGrindingNumber(value?: number) {
  return Number(Number(value || 0).toFixed(3));
}

function formatGrindingNumber(value?: number) {
  return roundGrindingNumber(Number(value || 0)).toFixed(3);
}

const firstGrindingSummary = computed(() => {
  const previousMaterialCode = task.value?.materialCode || task.value?.previousMaterialCode || '-';
  const sourceRollLength = Number(
    task.value?.previousGoodQty ?? task.value?.previousOutputLength ?? task.value?.outputLength ?? task.value?.reportQty ?? task.value?.inputLength ?? 0,
  );
  const previousBatchNo = getPreviousOperationMotherBatchNo();
  const finishedUsedLength = previousBatchNo
    ? getUsedLengthForSource(previousBatchNo)
    : materialUsageRows.value.reduce((sum, row) => sum + Number(row.processLength || 0) + Number(row.lossLength || 0), 0);
  return {
    finishedUsedLength: Number(finishedUsedLength.toFixed(3)),
    previousMaterialCode,
    remainingLength: Number(Math.max(0, sourceRollLength - finishedUsedLength).toFixed(3)),
    sourceRollLength: Number(sourceRollLength.toFixed(3)),
  };
});

const materialSourceCatalog = ref<Record<string, { planNo: string; remainStartMeter: number; totalLength: number }>>({});

function getUsedLengthForSource(batchNo: string) {
  const targetBatchNo = String(batchNo || '').trim();
  const sourceBatchNos = isPreviousOperationSourceBatch(targetBatchNo)
    ? getPreviousOperationSourceBatchAliases()
    : [targetBatchNo];
  return roundGrindingNumber(
    materialUsageRows.value
      .filter((row) => sourceBatchNos.includes(String(row.batchNo || '').trim()))
      .reduce((sum, row) => sum + Number(row.processLength || 0) + Number(row.lossLength || 0), 0),
  );
}

function ensureMaterialSourceEntry(batchNo: string, sourceType: '扫码母料' | '边库余料', planNo?: string) {
  if (sourceType === '扫码母料' && isPreviousOperationSourceBatch(batchNo)) {
    const previousEntry = syncPreviousOperationMaterialSourceEntry(batchNo, planNo);
    if (previousEntry) {
      return previousEntry;
    }
  }
  const existed = materialSourceCatalog.value[batchNo];
  if (existed) {
    return existed;
  }
  const totalLength = sourceType === '边库余料' ? 24 : 80;
  const entry = {
    planNo: planNo || task.value?.planNo || '',
    remainStartMeter: 0,
    totalLength: roundGrindingNumber(totalLength),
  };
  materialSourceCatalog.value = {
    ...materialSourceCatalog.value,
    [batchNo]: entry,
  };
  return entry;
}

function getMaterialSourceAvailability(batchNo: string, sourceType: '扫码母料' | '边库余料', planNo?: string) {
  const source = ensureMaterialSourceEntry(batchNo, sourceType, planNo);
  const usedLength = getUsedLengthForSource(batchNo);
  const remainLength = roundGrindingNumber(Math.max(0, source.totalLength - usedLength));
  const remainStartMeter = roundGrindingNumber(source.remainStartMeter + usedLength);
  return {
    remainLength,
    remainStartMeter,
    totalLength: source.totalLength,
  };
}

function getSecondGrindingUsedLength(sourceRowId: string, excludeRowId = '') {
  return roundGrindingNumber(
    secondGrindingRows.value
      .filter((row) => row.sourceRowId === sourceRowId && row.id !== excludeRowId)
      .reduce((sum, row) => sum + Number(row.processLength || 0) + Number(row.lossLength || 0), 0),
  );
}

const secondGrindingSourceAvailability = computed(() => {
  const sourceRow = materialUsageRows.value.find((item) => item.id === secondGrindingForm.value.sourceRowId);
  if (!sourceRow) return 0;
  return roundGrindingNumber(
    Math.max(0, Number(sourceRow.outputLength || 0) - getSecondGrindingUsedLength(sourceRow.id, secondGrindingEditRowId.value)),
  );
});

const previousMaterialInfo = computed(() => ({
  finishedUsedLength: firstGrindingSummary.value.finishedUsedLength || 0,
  previousMaterialCode: firstGrindingSummary.value.previousMaterialCode || '-',
  remainingLength: firstGrindingSummary.value.remainingLength || 0,
  sourceRollLength: firstGrindingSummary.value.sourceRollLength || 0,
}));

function buildGrindingAggregate<
  T extends {
    processLength?: number;
    lossLength?: number;
    outputLength?: number;
    napSampleLength?: number;
    sandpaperBatchNo?: string;
    sandpaperLife?: number;
    sandpaperLifeDays?: number;
  },
>(rows: T[]) {
  const lastRow = rows.length ? rows[rows.length - 1] : undefined;
  return {
    lastSandpaperBatchNo: lastRow?.sandpaperBatchNo || '-',
    lastSandpaperLife: lastRow?.sandpaperLife,
    lastSandpaperLifeDays: lastRow?.sandpaperLifeDays,
    lossLength: roundGrindingNumber(rows.reduce((sum, row) => sum + Number(row.lossLength || 0), 0)),
    napSampleLength: roundGrindingNumber(rows.reduce((sum, row) => sum + Number(row.napSampleLength || 0), 0)),
    outputLength: roundGrindingNumber(rows.reduce((sum, row) => sum + Number(row.outputLength || 0), 0)),
    processLength: roundGrindingNumber(rows.reduce((sum, row) => sum + Number(row.processLength || 0), 0)),
  };
}

const firstGrindingAggregate = computed(() => buildGrindingAggregate(materialUsageRows.value));
const secondGrindingAggregate = computed(() => buildGrindingAggregate(secondGrindingRows.value));
const roughAbnormalPositionRows = computed(() =>
  materialUsageRows.value.flatMap((row) =>
    (row.abnormalPositions || []).map((position, index) => ({
      ...position,
      sourceBatchNo: position.sourceBatchNo || row.batchNo,
      sourcePlanNo: position.sourcePlanNo || row.planNo,
      sourceRowId: position.sourceRowId || row.id,
      sortOrder: position.sortOrder || index + 1,
    })),
  ),
);
const selectedSecondGrindingRows = computed(() => {
  void secondGrindingSelectionVersion.value;
  return secondGrindingRows.value.filter((row) => row.selectedForPrint);
});
const selectedSecondGrindingPrintRows = computed(() => selectedSecondGrindingRows.value);
const currentSecondGrindingConfirmRow = computed(() =>
  secondGrindingRows.value.find((row) => row.id === secondGrindingConfirmForm.value.rowId),
);
const secondGrindingDialogTitle = computed(() =>
  secondGrindingEditRowId.value ? '第二次磨皮参数修改' : '第二次磨皮报工登记',
);
const bookingReportQty = computed(() => secondGrindingAggregate.value.processLength);
const productionEndDateDisplay = computed(
  () => normalizeDate(task.value?.productionEndDate || task.value?.endTime || task.value?.productionStartDate) || '-',
);

const firstGrindingSourceOptions = computed(() =>
  materialUsageRows.value.map((row) => ({
    label: `${row.batchNo} / ${row.sourceType || '第一次磨皮'}`,
    value: row.id,
  })),
);

const materialSourceHintText = computed(() => {
  if (!materialScanForm.value.batchNo) return '请先扫码或输入母料批号，系统将带出当前起米和可加工米数。';
  return `当前起米：${roundGrindingNumber(materialScanForm.value.remainStartMeter || 0)} m，当前可加工米数：${roundGrindingNumber(materialScanForm.value.remainLength || 0)} m，占用按加工米数 + 加工损耗米数计算`;
});

const secondGrindingSourceHintText = computed(() => {
  if (!secondGrindingForm.value.sourceRowId) return '请选择第一次磨皮来源后，再手动填写加工米数。';
  return '已选择第一次磨皮来源，请手动填写加工米数；提交时会校验加工米数 + 加工损耗米数不超过来源剩余。';
});

function resolveGrindingStandards(category: '一次磨皮' | '二次磨皮') {
  const detailRows = productionCheckRows.value[0]?.details || [];
  const standards = new Map<string, string>();
  detailRows
    .filter((item) => item.category === category && item.node === '工艺参数')
    .forEach((item) => {
      if (item.item) {
        standards.set(item.item, item.standard || '-');
      }
    });
  return standards;
}

const firstGrindingStandards = computed(() => {
  const standards = resolveGrindingStandards('一次磨皮');
  return [
    { label: '砂纸累计使用(米)', value: standards.get('砂纸累计寿命') || '-' },
    { label: '气压', value: standards.get('气压') || '-' },
    { label: '线速', value: standards.get('线速') || '-' },
    { label: '转速', value: standards.get('转速') || '-' },
    { label: '计米器', value: standards.get('计米器') || '-' },
    { label: '磨皮厚度', value: standards.get('磨皮厚度') || '-' },
    { label: '磨皮后厚度', value: standards.get('磨皮后厚度') || '-' },
  ];
});

const secondGrindingStandards = computed(() => {
  const standards = resolveGrindingStandards('二次磨皮');
  return [
    { label: '砂纸累计使用(米)', value: standards.get('砂纸累计寿命') || '-' },
    { label: '气压', value: standards.get('气压') || '-' },
    { label: '线速', value: standards.get('线速') || '-' },
    { label: '转速', value: standards.get('转速') || '-' },
    { label: '计米器', value: standards.get('计米器') || '-' },
    { label: '磨皮厚度', value: standards.get('磨皮厚度') || '-' },
    { label: '磨皮后厚度', value: standards.get('磨皮后厚度') || '-' },
  ];
});

function getRoughGrindingCheckCategorySort(category: string) {
  const index = ROUGH_GRINDING_CHECK_CATEGORY_ORDER.indexOf(category);
  return index >= 0 ? index : ROUGH_GRINDING_CHECK_CATEGORY_ORDER.length;
}

function mapRoughGrindingCheckItem(item: any, index = 0): RoughGrindingCheckItem {
  const itemCategory = item?.itemCategory || item?.category || '其他';
  const itemName = item?.itemName || item?.item || '';
  return {
    abnormalRemark: item?.abnormalRemark || item?.remark || '',
    actualValue: item?.actualValue || item?.value || '',
    checkResult: item?.checkResult || item?.result || 'OK',
    itemCategory,
    itemName,
    sortNo: Number(item?.sortNo || item?.seq || index + 1),
    standardValue: item?.standardValue || item?.standard || getDefaultRoughGrindingStandard(itemCategory, itemName),
  };
}

function cloneRoughGrindingCheckItems(items: RoughGrindingCheckItem[]) {
  return items.map((item, index) => mapRoughGrindingCheckItem(item, index));
}

function normalizeRoughAbnormalLength(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function createRoughAbnormalPositionRow(row?: Partial<RoughGrindingAbnormalPositionRow>) {
  return {
    abnormalLength: normalizeRoughAbnormalLength(row?.abnormalLength),
    clientKey: row?.clientKey || `rough-abnormal-${Date.now()}-${Math.random()}`,
    positionText: String(row?.positionText || ''),
    remark: String(row?.remark || ''),
    sortOrder: row?.sortOrder,
    sourceBatchNo: row?.sourceBatchNo || '',
    sourcePlanNo: row?.sourcePlanNo || '',
    sourceRowId: row?.sourceRowId || '',
  };
}

function cloneRoughAbnormalPositionRows(rows?: RoughGrindingAbnormalPositionRow[]) {
  return Array.isArray(rows) ? rows.map((row) => createRoughAbnormalPositionRow(row)) : [];
}

function createRoughGrindingCheckItemsFromTemplate(categories: string[]) {
  const categorySet = new Set(categories);
  const detailRows = productionCheckRows.value[0]?.details || [];
  return detailRows
    .filter((item) => categorySet.has(item.category || '') && !isRoughGrindingTimeItem(item.item))
    .map((item, index) => mapRoughGrindingCheckItem(item, index))
    .sort(
      (a, b) =>
        getRoughGrindingCheckCategorySort(a.itemCategory) - getRoughGrindingCheckCategorySort(b.itemCategory) ||
        Number(a.sortNo || 0) - Number(b.sortNo || 0),
    );
}

function normalizeRoughGrindingItemName(value?: string) {
  return String(value || '').replace(/\s+/g, '');
}

function isRoughGrindingTimeItem(itemName?: string) {
  const normalizedName = normalizeRoughGrindingItemName(itemName);
  return normalizedName.includes('开始时间') || normalizedName.includes('结束时间');
}

function getDefaultRoughGrindingStandard(category?: string, itemName?: string) {
  if (!category || !itemName) return '';
  const normalizedItemName = normalizeRoughGrindingItemName(itemName);
  const matchedKey = Object.keys(ROUGH_GRINDING_DEFAULT_STANDARDS).find((key) => {
    const [keyCategory, keyItemName] = key.split('::');
    return keyCategory === category && normalizeRoughGrindingItemName(keyItemName) === normalizedItemName;
  });
  return matchedKey ? ROUGH_GRINDING_DEFAULT_STANDARDS[matchedKey] : '';
}

function resetMaterialScanCheckItems(items?: RoughGrindingCheckItem[]) {
  materialScanCheckItems.value = items?.length
    ? cloneRoughGrindingCheckItems(items)
    : createRoughGrindingCheckItemsFromTemplate(['一次磨皮']);
}

function resetSecondGrindingCheckItems(items?: RoughGrindingCheckItem[]) {
  secondGrindingCheckItems.value = items?.length
    ? cloneRoughGrindingCheckItems(items)
    : createRoughGrindingCheckItemsFromTemplate(['二次磨皮', '磨皮后Nap层']);
}

const materialScanCheckCategories = computed(() => {
  const categories = Array.from(new Set(materialScanCheckItems.value.map((item) => item.itemCategory || '其他')));
  return categories.sort((a, b) => getRoughGrindingCheckCategorySort(a) - getRoughGrindingCheckCategorySort(b));
});

const secondGrindingCheckCategories = computed(() => {
  const categories = Array.from(new Set(secondGrindingCheckItems.value.map((item) => item.itemCategory || '其他')));
  return categories.sort((a, b) => getRoughGrindingCheckCategorySort(a) - getRoughGrindingCheckCategorySort(b));
});

function getMaterialScanCheckItemsByCategory(category: string) {
  return materialScanCheckItems.value
    .filter((item) => item.itemCategory === category)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function getSecondGrindingCheckItemsByCategory(category: string) {
  return secondGrindingCheckItems.value
    .filter((item) => item.itemCategory === category)
    .sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0));
}

function findRoughGrindingCheckActualValue(
  items: RoughGrindingCheckItem[],
  itemName: string,
  category?: string,
) {
  const matched = items.find(
    (item) => item.itemName === itemName && (!category || item.itemCategory === category),
  );
  return matched?.actualValue || '';
}

function setRoughGrindingCheckActualValue(
  items: RoughGrindingCheckItem[],
  itemName: string,
  actualValue?: unknown,
  category?: string,
) {
  const matched = items.find(
    (item) => item.itemName === itemName && (!category || item.itemCategory === category),
  );
  if (matched && actualValue !== undefined && actualValue !== null && String(actualValue) !== '') {
    matched.actualValue = String(actualValue);
  }
}

function toOptionalGrindingNumber(value?: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function syncMaterialScanCheckItemsToForm() {
  const items = materialScanCheckItems.value;
  setRoughGrindingCheckActualValue(items, '开始时间（年/月/日/时）', materialScanForm.value.startTime, '一次磨皮');
  setRoughGrindingCheckActualValue(items, '结束时间（年/月/日/时）', materialScanForm.value.endTime, '一次磨皮');
  const sandpaperLife = findRoughGrindingCheckActualValue(items, '砂纸累计寿命', '一次磨皮');
  materialScanForm.value.sandpaperLife = toOptionalGrindingNumber(sandpaperLife) ?? materialScanForm.value.sandpaperLife;
  materialScanForm.value.pressure = findRoughGrindingCheckActualValue(items, '气压', '一次磨皮') || materialScanForm.value.pressure;
  materialScanForm.value.lineSpeed = findRoughGrindingCheckActualValue(items, '线速', '一次磨皮') || materialScanForm.value.lineSpeed;
  materialScanForm.value.rotationSpeed = findRoughGrindingCheckActualValue(items, '转速', '一次磨皮') || materialScanForm.value.rotationSpeed;
  materialScanForm.value.meterCounter = findRoughGrindingCheckActualValue(items, '计米器', '一次磨皮') || materialScanForm.value.meterCounter;
  materialScanForm.value.grindingThickness =
    findRoughGrindingCheckActualValue(items, '磨皮厚度', '一次磨皮') || materialScanForm.value.grindingThickness;
  materialScanForm.value.afterGrindingThickness =
    findRoughGrindingCheckActualValue(items, '磨皮后厚度', '一次磨皮') || materialScanForm.value.afterGrindingThickness;
}

function syncSecondGrindingCheckItemsToForm() {
  const items = secondGrindingCheckItems.value;
  setRoughGrindingCheckActualValue(items, '开始时间（年/月/日/时）', secondGrindingForm.value.startTime, '二次磨皮');
  setRoughGrindingCheckActualValue(items, '结束时间（年/月/日/时）', secondGrindingForm.value.endTime, '二次磨皮');
  const sandpaperLife = findRoughGrindingCheckActualValue(items, '砂纸累计寿命', '二次磨皮');
  secondGrindingForm.value.sandpaperLife = toOptionalGrindingNumber(sandpaperLife) ?? secondGrindingForm.value.sandpaperLife;
  secondGrindingForm.value.pressure = findRoughGrindingCheckActualValue(items, '气压', '二次磨皮') || secondGrindingForm.value.pressure;
  secondGrindingForm.value.lineSpeed = findRoughGrindingCheckActualValue(items, '线速', '二次磨皮') || secondGrindingForm.value.lineSpeed;
  secondGrindingForm.value.rotationSpeed = findRoughGrindingCheckActualValue(items, '转速', '二次磨皮') || secondGrindingForm.value.rotationSpeed;
  secondGrindingForm.value.meterCounter = findRoughGrindingCheckActualValue(items, '计米器', '二次磨皮') || secondGrindingForm.value.meterCounter;
  secondGrindingForm.value.grindingThickness =
    findRoughGrindingCheckActualValue(items, '磨皮厚度', '二次磨皮') || secondGrindingForm.value.grindingThickness;
  secondGrindingForm.value.afterGrindingThickness =
    findRoughGrindingCheckActualValue(items, '磨皮后厚度', '二次磨皮') || secondGrindingForm.value.afterGrindingThickness;
  secondGrindingForm.value.qualityThickness =
    findRoughGrindingCheckActualValue(items, '厚度', '磨皮后Nap层') || secondGrindingForm.value.qualityThickness;
  secondGrindingForm.value.qualityWidth =
    findRoughGrindingCheckActualValue(items, '磨后宽幅', '磨皮后Nap层') || secondGrindingForm.value.qualityWidth;
  secondGrindingForm.value.grindingMeters =
    toOptionalGrindingNumber(findRoughGrindingCheckActualValue(items, '磨皮米数', '磨皮后Nap层')) ??
    secondGrindingForm.value.grindingMeters;
}

function buildMaterialScanCheckItemsReq() {
  syncMaterialScanCheckItemsToForm();
  return cloneRoughGrindingCheckItems(materialScanCheckItems.value);
}

function addMaterialScanAbnormalRow() {
  materialScanAbnormalRows.value.push(createRoughAbnormalPositionRow());
}

function removeMaterialScanAbnormalRow(index: number) {
  materialScanAbnormalRows.value.splice(index, 1);
}

function openMaterialScanAbnormalTab() {
  if (!materialScanAbnormalRows.value.length) {
    addMaterialScanAbnormalRow();
  }
  materialScanActiveTab.value = 'abnormal-position';
}

function buildMaterialScanAbnormalPositions(sourceRowId = '') {
  const rows = materialScanAbnormalRows.value
    .map((row, index) => {
      const abnormalLength = normalizeRoughAbnormalLength(row.abnormalLength);
      return {
        abnormalLength,
        clientKey: row.clientKey || `rough-abnormal-${Date.now()}-${index}`,
        positionText: String(row.positionText || '').trim(),
        remark: String(row.remark || '').trim(),
        sortOrder: index + 1,
        sourceBatchNo: materialScanForm.value.batchNo || row.sourceBatchNo || '',
        sourcePlanNo: materialScanForm.value.planNo || row.sourcePlanNo || '',
        sourceRowId: sourceRowId || row.sourceRowId || '',
      };
    })
    .filter(
      (row) =>
        row.positionText ||
        row.abnormalLength !== undefined ||
        Boolean(row.remark),
    );
  for (const row of rows) {
    if (!row.positionText) {
      AModal.warning({
        content: '异常位置不能为空。',
        title: '请完善异常位置',
      });
      materialScanActiveTab.value = 'abnormal-position';
      return null;
    }
    if (
      row.abnormalLength !== undefined &&
      (!Number.isFinite(row.abnormalLength) || row.abnormalLength < 0)
    ) {
      AModal.warning({
        content: '异常位置米数不能为负数。',
        title: '请完善异常位置',
      });
      materialScanActiveTab.value = 'abnormal-position';
      return null;
    }
  }
  return rows;
}

function formatRoughAbnormalPositionSummary(rows?: RoughGrindingAbnormalPositionRow[]) {
  const validRows = (rows || []).filter(
    (row) =>
      String(row.positionText || '').trim() ||
      normalizeRoughAbnormalLength(row.abnormalLength) !== undefined ||
      Boolean(row.remark),
  );
  if (!validRows.length) return '未登记';
  const totalLength = validRows.reduce(
    (sum, row) => sum + Number(normalizeRoughAbnormalLength(row.abnormalLength) || 0),
    0,
  );
  return totalLength > 0
    ? `${validRows.length} 条 / ${formatGrindingNumber(totalLength)} m`
    : `${validRows.length} 条`;
}

function buildSecondGrindingCheckItemsReq() {
  syncSecondGrindingCheckItemsToForm();
  return cloneRoughGrindingCheckItems(secondGrindingCheckItems.value);
}

const previousOperationInfo = computed(() => {
  const previousName =
    task.value?.previousOperationName ||
    task.value?.previousOpName ||
    task.value?.previousProcessName ||
    '';
  const previousStatus = task.value?.previousOperationStatus || task.value?.previousOpStatus || '';
  const previousProductionDate = task.value?.previousProductionDate || '';
  const previousStartTime = task.value?.previousStartTime || '';
  const previousEndTime = task.value?.previousEndTime || '';
  const previousRecorderName = task.value?.previousRecorderName || task.value?.previousRecorder || '';
  if (!previousName && !previousStatus && !previousProductionDate && !previousStartTime && !previousEndTime && !previousRecorderName) {
    return null;
  }
  const statusText =
    previousStatus === 'COMPLETED'
      ? '已完成'
      : previousStatus === 'IN_PROGRESS'
        ? '执行中'
        : previousStatus === 'PENDING'
          ? '待开工'
          : previousStatus || '-';
  const statusColor =
    previousStatus === 'COMPLETED'
      ? 'success'
      : previousStatus === 'IN_PROGRESS'
        ? 'processing'
        : previousStatus === 'PENDING'
          ? 'default'
          : 'default';
  return {
    endTime: previousEndTime || '-',
    opName: previousName || '-',
    productionDate: previousProductionDate || '-',
    startTime: normalizeDateTime(previousStartTime) || normalizeDateTime(previousProductionDate) || previousProductionDate || '-',
    recorderName: previousRecorderName || '-',
    status: {
      color: statusColor,
      text: statusText,
    },
  };
});

function parseRoughExtraJson(extraJson?: string | null) {
  if (!extraJson) return {};
  try {
    return JSON.parse(extraJson) || {};
  } catch {
    return {};
  }
}

function buildRoughSnapshotExtra() {
  return JSON.stringify({
    firstInspection: firstInspection.value,
    materialSourceCatalog: materialSourceCatalog.value,
    materialUsageRows: materialUsageRows.value,
    productionCheckRows: productionCheckRows.value,
    reportForm: reportForm.value,
    secondGrindingRows: secondGrindingRows.value,
    segmentRows: segmentRows.value,
    semiFinishedRows: semiFinishedRows.value,
    stockLedgerRows: stockLedgerRows.value,
    workPrepareRows: workPrepareRows.value,
  });
}

function buildRoughReportPayload(finalSubmit = false) {
  const firstAggregate = firstGrindingAggregate.value;
  const secondAggregate = secondGrindingAggregate.value;
  return {
    batchNo: task.value?.batchNo || undefined,
    confirmerName: finalSubmit ? reportForm.value.confirmerName || undefined : undefined,
    confirmerTime: finalSubmit ? reportForm.value.confirmerTime || undefined : undefined,
    endTime: finalSubmit ? reportForm.value.endTime || undefined : undefined,
    equipmentCode: reportForm.value.equipmentCode || undefined,
    equipmentId: reportForm.value.equipmentId,
    equipmentName: reportForm.value.equipmentName || undefined,
    extraJson: buildRoughSnapshotExtra(),
    firstLossLength: firstAggregate.lossLength,
    firstNapSampleLength: firstAggregate.napSampleLength,
    firstOutputLength: firstAggregate.outputLength,
    firstProcessLength: firstAggregate.processLength,
    inputLength: firstAggregate.processLength,
    lastFirstSandpaperBatchNo: firstAggregate.lastSandpaperBatchNo === '-' ? undefined : firstAggregate.lastSandpaperBatchNo,
    lastFirstSandpaperLife: firstAggregate.lastSandpaperLife,
    lastFirstSandpaperLifeDays: firstAggregate.lastSandpaperLifeDays,
    lastSecondSandpaperBatchNo: secondAggregate.lastSandpaperBatchNo === '-' ? undefined : secondAggregate.lastSandpaperBatchNo,
    lastSecondSandpaperLife: secondAggregate.lastSandpaperLife,
    lastSecondSandpaperLifeDays: secondAggregate.lastSandpaperLifeDays,
    planId: task.value?.planId,
    planOperationId: task.value?.planOperationId,
    recorderName: reportForm.value.recorderName || undefined,
    recorderTime: reportForm.value.recorderTime || undefined,
    remark: reportForm.value.remark || undefined,
    reportDate: reportForm.value.productionDate || undefined,
    reportQty: bookingReportQty.value,
    secondLossLength: secondAggregate.lossLength,
    secondNapSampleLength: secondAggregate.napSampleLength,
    secondOutputLength: secondAggregate.outputLength,
    secondProcessLength: secondAggregate.processLength,
    startTime: reportForm.value.startTime || undefined,
  };
}

async function persistRoughProgress() {
  if (!task.value?.planId || !task.value?.planOperationId || !isStarted() || task.value?.status === 'COMPLETED') {
    return;
  }
  const payload = buildRoughReportPayload(false) as any;
  await saveRoughGrindingProgress(payload);
  task.value = {
    ...task.value,
    equipmentCode: payload.equipmentCode || task.value?.equipmentCode,
    equipmentId: payload.equipmentId || task.value?.equipmentId,
    equipmentName: payload.equipmentName || task.value?.equipmentName,
    extraJson: payload.extraJson,
    goodQty: payload.reportQty ?? task.value?.goodQty,
    productionDate: payload.reportDate || task.value?.productionDate,
    recorderName: payload.recorderName || task.value?.recorderName,
    recorderTime: payload.recorderTime || task.value?.recorderTime,
    reportRemark: payload.remark ?? task.value?.reportRemark,
    startTime: payload.startTime || task.value?.startTime,
  };
  emit('taskChange', { ...task.value });
}

function queuePersistRoughProgress(delay = 450) {
  if (roughProgressPersistTimer.value) clearTimeout(roughProgressPersistTimer.value);
  roughProgressPersistTimer.value = setTimeout(() => {
    roughProgressPersistTimer.value = null;
    void persistRoughProgress();
  }, delay);
}

const recordDialogVisible = ref(false);
const recordDialogCategory = ref<RecordCategory>('prepare');
const recordDialogMode = ref<RecordMode>('view');
const currentRecord = ref<WetSheetRow | null>(null);
const actionPanelExpanded = ref(false);
const recordActionForm = ref({
  result: 'OK',
  inspectionResult: 'OK',
  formRemark: '',
  recorder: '',
  recorderTime: '',
  confirmer: '',
  confirmerTime: '',
  confirmRemark: '',
});

function normalizeDate(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function normalizeDateTime(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function formatStartTimeForSummary(_productionDate?: string, startTime?: string) {
  if (!startTime) return '';
  return normalizeDateTime(startTime);
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function createWorkPrepareRows(): WetSheetRow[] {
  return [
    {
      details: [
        { item: '放卷气缸压力', remark: '', result: 'OK', seq: 1, standard: '0.25-0.65MPa', value: '' },
        { item: '前三轮压力', remark: '', result: 'OK', seq: 2, standard: '0.05-0.25MPa', value: '' },
        { item: '前三轮张力架', remark: '', result: 'OK', seq: 3, standard: '手动抬降无明显卡顿', value: '' },
        { item: '砂纸轮电机', remark: '', result: 'OK', seq: 4, standard: '电机运行正常,无异响', value: '' },
        { item: '毛刷轮电机', remark: '', result: 'OK', seq: 5, standard: '电机运行正常,无异响', value: '' },
        { item: '1#拍打轮压力', remark: '', result: 'OK', seq: 6, standard: '0.05-0.25MPa', value: '' },
        { item: '2#拍打轮压力', remark: '', result: 'OK', seq: 7, standard: '0.05-0.25MPa', value: '' },
        { item: '3#拍打轮压力', remark: '', result: 'OK', seq: 8, standard: '0-0.10MPa', value: '' },
        { item: '3#拍打轮张力架', remark: '', result: 'OK', seq: 9, standard: '手动抬降无明显卡顿', value: '' },
        { item: '收卷张力架压力', remark: '', result: 'OK', seq: 10, standard: '0MPa', value: '' },
        { item: '收卷气缸压力', remark: '', result: 'OK', seq: 11, standard: '0.25-0.65MPa', value: '' },
        { item: '收卷气杆', remark: '', result: 'OK', seq: 12, standard: '气杆抬降无明显卡顿', value: '' },
        { item: '厚度计', remark: '', result: 'OK', seq: 13, standard: '标准块校准合格', value: '' },
        { item: '塞尺', remark: '', result: 'OK', seq: 14, standard: '无形变、无锈蚀', value: '' },
        { item: '砂纸状况', remark: '', result: 'OK', seq: 15, standard: '磨皮米数≤500m（计米器记录），使用周期≤15天；任一条件达到或料皮表观不平、条纹等异常时立即更换', value: '' },
        { item: '导布状况', remark: '', result: 'OK', seq: 16, standard: '累计使用次数≤200次；表面无明显片状脏污，边缘裂口长度<2cm', value: '' },
      ],
      id: 'rough-startup-check',
      name: 'CMP软垫磨皮开机点检表',
      result: '未检查',
      status: 'PENDING',
      timing: '开机前',
    },
    {
      details: [
        { category: '放卷区', item: '放卷气杠', remark: '', result: 'OK', seq: 1, standard: '表面清洁无脏污和异物', value: '' },
        { category: '放卷区', item: '磁粉刹车', remark: '', result: 'OK', seq: 2, standard: '表面清洁无脏污和异物', value: '' },
        { category: '放卷区', item: '放卷辊轮', remark: '', result: 'OK', seq: 3, standard: '表面清洁无脏污和异物', value: '' },
        { category: '前三轮区', item: '前三轮', remark: '', result: 'OK', seq: 4, standard: '表面清洁无脏污和异物', value: '' },
        { category: '前三轮区', item: '前三轮张力架', remark: '', result: 'OK', seq: 5, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '磨皮机操作面板', remark: '', result: 'OK', seq: 6, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '磨皮水箱挡板', remark: '', result: 'OK', seq: 7, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '磨皮机砂纸轮两侧', remark: '', result: 'OK', seq: 8, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '磨辊集尘仓', remark: '', result: 'OK', seq: 9, standard: '无团簇状残留磨屑', value: '' },
        { category: '主机区', item: '磨皮机踏板', remark: '', result: 'OK', seq: 10, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '毛刷轮电机', remark: '', result: 'OK', seq: 11, standard: '表面清洁无脏污和异物', value: '' },
        { category: '主机区', item: '毛刷轮', remark: '', result: 'OK', seq: 12, standard: '无团簇状残留导布丝', value: '' },
        { category: '收卷区', item: '后三轮', remark: '', result: 'OK', seq: 13, standard: '表面清洁无脏污和异物', value: '' },
        { category: '收卷区', item: '后三轮张力架', remark: '', result: 'OK', seq: 14, standard: '表面清洁无脏污和异物', value: '' },
        { category: '收卷区', item: '收卷气杠', remark: '', result: 'OK', seq: 15, standard: '表面清洁无脏污和异物', value: '' },
        { category: '收卷区', item: '收卷辊轮', remark: '', result: 'OK', seq: 16, standard: '表面清洁无脏污和异物', value: '' },
        { category: '收卷区', item: '产品接触辊轮', remark: '', result: 'OK', seq: 17, standard: '表面清洁无脏污和异物', value: '' },
        { category: '辅助区', item: '非直接接触部件', remark: '', result: 'OK', seq: 18, standard: '表面清洁无脏污和异物', value: '' },
        { category: '辅助区', item: '磨皮集尘箱', remark: '', result: 'OK', seq: 19, standard: '表面清洁无脏污和异物', value: '' },
      ],
      id: 'rough-cleaning-check',
      name: 'CMP软垫磨皮设备清洁点检表',
      result: '未检查',
      status: 'PENDING',
      timing: '清洁后/开机前',
    },
  ];
}

function createProductionCheckRows(): WetSheetRow[] {
  return [
    {
      details: [
        { category: '一次磨皮', node: '工艺参数', item: '开始时间（年/月/日/时）', remark: '', result: 'OK', seq: 1, standard: '/', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '结束时间（年/月/日/时）', remark: '', result: 'OK', seq: 2, standard: '/', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '砂纸累计寿命', remark: '', result: 'OK', seq: 3, standard: '累计磨皮≤500m，且≤15天', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '气压', remark: '', result: 'OK', seq: 4, standard: '0.15±0.05MPa', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '线速', remark: '', result: 'OK', seq: 5, standard: '0.60±0.10m/min', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '转速', remark: '', result: 'OK', seq: 6, standard: '900±100r/min', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '计米器', remark: '', result: 'OK', seq: 7, standard: '料皮驶入磨皮机时归零', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '磨皮厚度', remark: '', result: 'OK', seq: 8, standard: '0.960±0.020mm', value: '' },
        { category: '一次磨皮', node: '工艺参数', item: '磨皮后厚度', remark: '', result: 'OK', seq: 9, standard: '1.000±0.050mm', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '开始时间（年/月/日/时）', remark: '', result: 'OK', seq: 10, standard: '/', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '结束时间（年/月/日/时）', remark: '', result: 'OK', seq: 11, standard: '/', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '砂纸累计寿命', remark: '', result: 'OK', seq: 12, standard: '累计磨皮≤500m，且≤15天', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '气压', remark: '', result: 'OK', seq: 13, standard: '0.15±0.05MPa', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '磨皮厚度', remark: '', result: 'OK', seq: 14, standard: '0.865±0.030mm', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '线速', remark: '', result: 'OK', seq: 15, standard: '0.55±0.05m/min', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '转速', remark: '', result: 'OK', seq: 16, standard: '900±100r/min', value: '' },
        { category: '二次磨皮', node: '工艺参数', item: '计米器', remark: '', result: 'OK', seq: 17, standard: '料皮驶入磨皮机时归零', value: '' },
        { category: '磨皮后Nap层', node: '质量结果', item: '厚度', remark: '', result: 'OK', seq: 18, standard: '0.914±0.030mm', value: '' },
        { category: '磨皮后Nap层', node: '质量结果', item: '磨后宽幅', remark: '', result: 'OK', seq: 19, standard: '1.04±0.04m', value: '' },
        { category: '磨皮后Nap层', node: '质量结果', item: '磨皮米数', remark: '', result: 'OK', seq: 20, standard: '/', value: '' },
      ],
      id: 'rough-production-check',
      name: 'CMP软垫（W26P0100）磨皮点检表',
      result: '未检查',
      status: 'PENDING',
      timing: '生产中',
    },
  ];
}

function createSemiFinishedRows(): WetSheetRow[] {
  return [
    {
      details: generateSemiDetails(300),
      finalResult: 'OK',
      generatedLength: 300,
      id: 'rough-middle-product',
      name: 'CMP软垫（W26P0100）磨皮中间品记录表',
      poreDevelopment: 'OK',
      result: '已生成',
      semiWidth: '',
      status: 'PENDING',
      timing: '生产中',
    },
  ];
}

function buildLedgerNo(index: number) {
  return `GM-RK-${dayjs().format('YYYYMMDD')}-${String(index + 1).padStart(3, '0')}`;
}

function buildSegmentNo(index: number) {
  return `D${String(index + 1).padStart(2, '0')}`;
}

function getMotherRollBaseLot() {
  return String(task.value?.batchNo || task.value?.motherMaterialCode || task.value?.planNo || 'ROUGH-LOT');
}

function getCurrentProductionBatchBase() {
  return String(task.value?.productionBatchNo || task.value?.batchNo || task.value?.planNo || 'ROUGH-BATCH').trim();
}

function buildSecondGrindingProductionBatchNo(segmentMark?: string, motherBatchNo?: string) {
  const segmentCode = String(segmentMark || '').trim().toUpperCase();
  if (!segmentCode) return '';
  const baseBatchNo = String(motherBatchNo || secondGrindingForm.value.batchNo || getCurrentProductionBatchBase()).trim();
  if (!baseBatchNo) return '';
  return `${baseBatchNo}${segmentCode}`;
}

function getPreviousOperationMotherBatchNo() {
  return String(
    task.value?.previousBatchNo ||
      task.value?.previousParentBatchNo ||
      task.value?.parentBatchNo ||
      task.value?.batchNo ||
      task.value?.motherBatchNo ||
      task.value?.planNo ||
      '',
  ).trim();
}

function getPreviousOperationSourceBatchAliases() {
  return Array.from(
    new Set(
      [
        task.value?.previousBatchNo,
        task.value?.previousParentBatchNo,
        task.value?.parentBatchNo,
        task.value?.batchNo,
        task.value?.motherBatchNo,
        task.value?.motherMaterialCode,
        task.value?.planNo,
        getPreviousOperationMotherBatchNo(),
        getMotherRollBaseLot(),
      ]
        .map((item) => String(item || '').trim())
        .filter(Boolean),
    ),
  );
}

function isPreviousOperationSourceBatch(batchNo?: string) {
  const targetBatchNo = String(batchNo || '').trim();
  if (!targetBatchNo) return false;
  return getPreviousOperationSourceBatchAliases().includes(targetBatchNo);
}

function getPreviousOperationSourceTotalLength() {
  return roundGrindingNumber(firstGrindingSummary.value.sourceRollLength || Number(reportForm.value.inputLength || 0) || 0);
}

function syncPreviousOperationMaterialSourceEntry(batchNo?: string, planNo?: string) {
  const targetBatchNo = String(batchNo || getPreviousOperationMotherBatchNo()).trim();
  if (!targetBatchNo || !isPreviousOperationSourceBatch(targetBatchNo)) {
    return undefined;
  }
  const existed = materialSourceCatalog.value[targetBatchNo];
  const entry = {
    planNo: planNo || existed?.planNo || task.value?.planNo || '',
    remainStartMeter: 0,
    totalLength: getPreviousOperationSourceTotalLength(),
  };
  materialSourceCatalog.value = {
    ...materialSourceCatalog.value,
    [targetBatchNo]: entry,
  };
  return entry;
}

function resetMaterialScanForm(sourceType: '扫码母料' | '边库余料' = '扫码母料') {
  const defaultLength = Number(reportForm.value.inputLength ?? 0) || 60;
  resetMaterialScanCheckItems();
  materialScanAbnormalRows.value = [];
  materialScanForm.value = {
    code: '',
    batchNo: '',
    planNo: task.value?.planNo || '',
    remainStartMeter: 0,
    remainLength: defaultLength,
    processLength: undefined,
    lossLength: 0,
    outputLength: undefined,
    napSampleLength: 0,
    grindingPass: '1次',
    startTime: '',
    endTime: '',
    sandpaperLife: reportForm.value.sandpaperLife,
    sandpaperLifeDays: undefined,
    sandpaperBatchNo: reportForm.value.sandpaperBatchNo || '',
    sandpaperLedgerBatchNo: '',
    sandpaperLedgerId: undefined,
    sandpaperNextUsageStatus: undefined,
    pressure: '',
    lineSpeed: '',
    rotationSpeed: '',
    meterCounter: '',
    grindingThickness: '',
    afterGrindingThickness: '',
    defectCode: '',
    qualityThickness: '',
    qualityWidth: '',
    grindingMeters: undefined,
    selfCheck: 'OK',
    sourceType,
  };
}

function openMaterialScanDialog(sourceType: '扫码母料' | '边库余料' = '扫码母料') {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  if (!isStarted()) {
    AModal.warning({
      content: '磨皮工序必须先开工后，才允许执行第一次磨皮报工。',
      title: '请先执行开工确认',
    });
    return;
  }
  resetMaterialScanForm(sourceType);
  if (sourceType === '扫码母料') {
    applyPreviousOperationMaterialSource();
  }
  materialScanActiveTab.value = 'report';
  materialScanVisible.value = true;
}

function applyPreviousOperationMaterialSource() {
  const batchNo = getPreviousOperationMotherBatchNo();
  if (!batchNo) return;
  materialScanForm.value.code = batchNo;
  materialScanForm.value.batchNo = batchNo;
  materialScanForm.value.planNo = task.value?.planNo || materialScanForm.value.planNo || '';
  const availability = getMaterialSourceAvailability(batchNo, '扫码母料', materialScanForm.value.planNo);
  materialScanForm.value.remainStartMeter = availability.remainStartMeter;
  materialScanForm.value.remainLength = availability.remainLength;
  materialScanForm.value.processLength = Number(
    Math.min(availability.remainLength, Number(reportForm.value.inputLength || 0) || availability.remainLength).toFixed(3),
  );
  materialScanForm.value.outputLength = materialScanForm.value.processLength;
  materialScanForm.value.startTime = reportForm.value.startTime || buildNowText();
  materialScanForm.value.endTime = reportForm.value.endTime || buildNowText();
  materialScanForm.value.grindingMeters = materialScanForm.value.processLength;
}

function syncMaterialSourceAvailability(batchNo?: string, sourceType?: '扫码母料' | '边库余料') {
  const targetBatchNo = batchNo || materialScanForm.value.batchNo;
  if (!targetBatchNo) return;
  const availability = getMaterialSourceAvailability(targetBatchNo, sourceType || materialScanForm.value.sourceType, materialScanForm.value.planNo);
  materialScanForm.value.remainStartMeter = availability.remainStartMeter;
  materialScanForm.value.remainLength = availability.remainLength;
  if (!materialScanForm.value.processLength || materialScanForm.value.processLength > availability.remainLength) {
    materialScanForm.value.processLength = availability.remainLength;
  }
}

function resetSecondGrindingForm() {
  resetSecondGrindingCheckItems();
  secondGrindingForm.value = {
    sourceCode: '',
    sourceRowId: '',
    sourceType: '',
    batchNo: '',
    productionBatchNo: '',
    processLength: undefined,
    lossLength: 0,
    outputLength: undefined,
    napSampleLength: 0,
    segmentMark: '',
    startTime: reportForm.value.startTime || buildNowText(),
    endTime: reportForm.value.endTime || buildNowText(),
    sandpaperLife: reportForm.value.sandpaperLife,
    sandpaperLifeDays: undefined,
    sandpaperBatchNo: reportForm.value.sandpaperBatchNo || '',
    sandpaperLedgerBatchNo: '',
    sandpaperLedgerId: undefined,
    sandpaperNextUsageStatus: undefined,
    pressure: '',
    lineSpeed: '',
    rotationSpeed: '',
    meterCounter: '',
    grindingThickness: '',
    afterGrindingThickness: '',
    defectCode: '',
    qualityThickness: '',
    qualityWidth: '',
    grindingMeters: undefined,
    selfCheck: 'OK',
  };
}

function openSecondGrindingDialog() {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  if (!isStarted()) {
    AModal.warning({
      content: '磨皮工序必须先开工后，才允许执行第二次磨皮报工。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (!materialUsageRows.value.length) {
    AModal.warning({ title: '暂无一次磨皮来源', content: '请先在第一次磨皮中追加报工记录后，再执行第二次磨皮。' });
    return;
  }
  secondGrindingEditRowId.value = '';
  resetSecondGrindingForm();
  secondGrindingActiveTab.value = 'report';
  secondGrindingVisible.value = true;
}

function applySecondGrindingRowToForm(row: RoughSecondGrindingRow) {
  resetSecondGrindingCheckItems(row.checkItems);
  secondGrindingForm.value = {
    sourceCode: row.batchNo || '',
    sourceRowId: row.sourceRowId || '',
    sourceType: row.sourceType || '',
    batchNo: row.batchNo || '',
    productionBatchNo: row.productionBatchNo || buildSecondGrindingProductionBatchNo(row.segmentMark, row.batchNo),
    processLength: row.processLength,
    lossLength: row.lossLength,
    outputLength: row.outputLength,
    napSampleLength: row.napSampleLength,
    segmentMark: row.segmentMark || '',
    startTime: row.startTime || reportForm.value.startTime || buildNowText(),
    endTime: row.endTime || reportForm.value.endTime || buildNowText(),
    sandpaperLife: row.sandpaperLife,
    sandpaperLifeDays: row.sandpaperLifeDays,
    sandpaperBatchNo: row.sandpaperBatchNo || '',
    sandpaperLedgerBatchNo: row.sandpaperLedgerBatchNo || '',
    sandpaperLedgerId: row.sandpaperLedgerId,
    sandpaperNextUsageStatus: row.sandpaperNextUsageStatus,
    pressure: row.pressure || '',
    lineSpeed: row.lineSpeed || '',
    rotationSpeed: row.rotationSpeed || '',
    meterCounter: row.meterCounter || '',
    grindingThickness: row.grindingThickness || '',
    afterGrindingThickness: row.afterGrindingThickness || '',
    defectCode: row.defectCode || '',
    qualityThickness: row.qualityThickness || '',
    qualityWidth: row.qualityWidth || '',
    grindingMeters: row.grindingMeters,
    selfCheck: row.selfCheck || 'OK',
  };
}

function openSelectedSecondGrindingEditDialog() {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  if (selectedSecondGrindingRows.value.length !== 1) {
    AModal.info({ title: '请选择一行', content: '修改参数一次只能选择一条第二次磨皮记录。' });
    return;
  }
  const row = selectedSecondGrindingRows.value[0];
  if (!row) return;
  if (row.confirmStatus === '已确认') {
    AModal.warning({ title: '已确认记录不能修改', content: '该分段已扫码确认并入账，不能再修改参数。' });
    return;
  }
  secondGrindingEditRowId.value = row.id;
  applySecondGrindingRowToForm(row);
  secondGrindingActiveTab.value = 'report';
  secondGrindingVisible.value = true;
}

function syncSecondGrindingSource() {
  const source = materialUsageRows.value.find((item) => item.id === secondGrindingForm.value.sourceRowId);
  if (!source) return;
  secondGrindingForm.value.sourceCode = source.batchNo;
  secondGrindingForm.value.sourceType = source.sourceType;
  secondGrindingForm.value.batchNo = source.batchNo;
  secondGrindingForm.value.productionBatchNo = buildSecondGrindingProductionBatchNo(
    secondGrindingForm.value.segmentMark,
    source.batchNo,
  );
}

function simulateSecondGrindingSource() {
  if (!firstGrindingSourceOptions.value.length) return;
  const scanCode = (secondGrindingForm.value.sourceCode || '').trim();
  if (scanCode) {
    const source = materialUsageRows.value.find((item) => item.batchNo === scanCode);
    if (source) {
      secondGrindingForm.value.sourceRowId = source.id;
    }
  }
  if (!secondGrindingForm.value.sourceRowId) {
    secondGrindingForm.value.sourceRowId = String(firstGrindingSourceOptions.value[0]?.value || '');
  }
  syncSecondGrindingSource();
}

function handleSecondGrindingFormSegmentChange() {
  secondGrindingForm.value.productionBatchNo = buildSecondGrindingProductionBatchNo(
    secondGrindingForm.value.segmentMark,
    secondGrindingForm.value.batchNo,
  );
}

function toggleMaterialUsageMaximized() {
  materialUsageMaximized.value = !materialUsageMaximized.value;
}

function simulateMaterialScan() {
  const code = (materialScanForm.value.code || '').trim();
  const baseLot = code || `${getMotherRollBaseLot()}-${materialScanForm.value.sourceType === '边库余料' ? 'SIDE' : 'M'}${String(materialUsageRows.value.length + 1).padStart(2, '0')}`;
  const planNo = materialScanForm.value.planNo || task.value?.planNo || 'P-RG-MOCK';
  const availability = getMaterialSourceAvailability(baseLot, materialScanForm.value.sourceType, planNo);
  materialScanForm.value.batchNo = baseLot;
  materialScanForm.value.planNo = planNo;
  materialScanForm.value.remainStartMeter = availability.remainStartMeter;
  materialScanForm.value.remainLength = availability.remainLength;
  if (!materialScanForm.value.processLength || materialScanForm.value.processLength > availability.remainLength) {
    materialScanForm.value.processLength = Number(Math.min(availability.remainLength, Number(reportForm.value.inputLength || 0) || 20).toFixed(3));
  }
  if (materialScanForm.value.lossLength === undefined) {
    materialScanForm.value.lossLength = 0;
  }
  if (materialScanForm.value.napSampleLength === undefined) {
    materialScanForm.value.napSampleLength = 0;
  }
  if (!materialScanForm.value.outputLength) {
    materialScanForm.value.outputLength = Number(
      Math.max(
        0,
        Number(materialScanForm.value.processLength || 0) -
          Number(materialScanForm.value.lossLength || 0) -
          Number(materialScanForm.value.napSampleLength || 0),
      ).toFixed(3),
    );
  }
  if (!materialScanForm.value.startTime) {
    materialScanForm.value.startTime = reportForm.value.startTime || buildNowText();
  }
  if (!materialScanForm.value.endTime) {
    materialScanForm.value.endTime = reportForm.value.endTime || buildNowText();
  }
  if (!materialScanForm.value.grindingMeters) {
    materialScanForm.value.grindingMeters = materialScanForm.value.processLength;
  }
}

function prepareNextMaterialAppend() {
  const preservedCode = materialScanForm.value.code;
  const preservedBatchNo = materialScanForm.value.batchNo;
  const preservedPlanNo = materialScanForm.value.planNo;
  const preservedSourceType = materialScanForm.value.sourceType;
  resetMaterialScanForm(preservedSourceType);
  materialScanForm.value.code = preservedCode;
  materialScanForm.value.batchNo = preservedBatchNo;
  materialScanForm.value.planNo = preservedPlanNo;
  if (preservedBatchNo) {
    syncMaterialSourceAvailability(preservedBatchNo, preservedSourceType);
  }
  materialScanForm.value.outputLength = undefined;
  materialScanForm.value.grindingMeters = undefined;
  materialScanForm.value.lossLength = 0;
  materialScanForm.value.napSampleLength = 0;
  materialScanForm.value.startTime = reportForm.value.startTime || buildNowText();
  materialScanForm.value.endTime = reportForm.value.endTime || buildNowText();
  materialScanForm.value.selfCheck = 'OK';
  materialScanForm.value.defectCode = '';
  materialScanForm.value.qualityThickness = '';
  materialScanForm.value.qualityWidth = '';
}

function prepareNextSecondGrindingAppend() {
  const preservedSourceCode = secondGrindingForm.value.sourceCode;
  const preservedSourceRowId = secondGrindingForm.value.sourceRowId;
  const preservedSourceType = secondGrindingForm.value.sourceType;
  resetSecondGrindingForm();
  secondGrindingForm.value.sourceCode = preservedSourceCode;
  secondGrindingForm.value.sourceRowId = preservedSourceRowId;
  secondGrindingForm.value.sourceType = preservedSourceType;
  if (preservedSourceRowId) {
    syncSecondGrindingSource();
  }
  secondGrindingForm.value.outputLength = undefined;
  secondGrindingForm.value.grindingMeters = undefined;
  secondGrindingForm.value.lossLength = 0;
  secondGrindingForm.value.napSampleLength = 0;
  secondGrindingForm.value.segmentMark = '';
  secondGrindingForm.value.productionBatchNo = '';
  secondGrindingForm.value.selfCheck = 'OK';
  secondGrindingForm.value.defectCode = '';
  secondGrindingForm.value.qualityThickness = '';
  secondGrindingForm.value.qualityWidth = '';
}

function syncSegmentsFromMaterialRows() {
  const previousStoredMap = new Map(
    segmentRows.value.filter((item) => item.status === 'STORED').map((item) => [`${item.parentId}-${item.segmentNo}`, item]),
  );
  const generated: RoughSegmentRow[] = [];
  materialUsageRows.value.forEach((row, rowIndex) => {
    const processLength = Number(row.outputLength || 0);
    const remainLength = Number(row.remainLength || 0);
    const startMeter = Number(row.remainStartMeter || 0);
    if (processLength <= 0 || remainLength <= 0) return;
    const safeLength = Number(Math.min(processLength, remainLength).toFixed(3));
    const segmentNo = buildSegmentNo(rowIndex);
    const cacheKey = `${row.id}-${segmentNo}`;
    const stored = previousStoredMap.get(cacheKey);
    generated.push({
      id: stored?.id || `${row.id}-${segmentNo}`,
      parentId: row.id,
      batchNo: row.batchNo,
      segmentNo,
      startMeter: Number(startMeter.toFixed(3)),
      length: safeLength,
      selfCheck: stored?.selfCheck || 'OK',
      thickness: stored?.thickness ?? (row.qualityThickness ? Number(row.qualityThickness) : undefined),
      width: stored?.width ?? (row.qualityWidth ? Number(row.qualityWidth) : undefined),
      selected: false,
      status: stored?.status || 'PENDING',
    });
  });
  segmentRows.value = generated;
}

function isFirstGrindingRowReferenced(rowId?: string) {
  if (!rowId) return false;
  return secondGrindingRows.value.some((item) => item.sourceRowId === rowId);
}

function isFirstGrindingRowEditable(row: RoughMaterialUsageRow) {
  return !isRoughReadOnly() && !isFirstGrindingRowReferenced(row.id);
}

function pickMaterialUsageRowState(row: RoughMaterialUsageRow) {
  return {
    abnormalPositions: row.abnormalPositions,
    afterGrindingThickness: row.afterGrindingThickness,
    batchNo: row.batchNo,
    defectCode: row.defectCode,
    endTime: row.endTime,
    grindingMeters: row.grindingMeters,
    grindingPass: row.grindingPass,
    grindingThickness: row.grindingThickness,
    id: row.id,
    lineSpeed: row.lineSpeed,
    lossLength: row.lossLength,
    meterCounter: row.meterCounter,
    napSampleLength: row.napSampleLength,
    outputLength: row.outputLength,
    planNo: row.planNo,
    pressure: row.pressure,
    processLength: row.processLength,
    qualityThickness: row.qualityThickness,
    qualityWidth: row.qualityWidth,
    remainLength: row.remainLength,
    remainStartMeter: row.remainStartMeter,
    rotationSpeed: row.rotationSpeed,
    sandpaperBatchNo: row.sandpaperBatchNo,
    sandpaperLedgerBatchNo: row.sandpaperLedgerBatchNo,
    sandpaperLedgerId: row.sandpaperLedgerId,
    sandpaperLife: row.sandpaperLife,
    sandpaperLifeDays: row.sandpaperLifeDays,
    sandpaperNextUsageStatus: row.sandpaperNextUsageStatus,
    selfCheck: row.selfCheck,
    sourceType: row.sourceType,
    startTime: row.startTime,
    checkItems: row.checkItems,
  };
}

function syncMaterialUsageRowToState(row: RoughMaterialUsageRow) {
  const index = materialUsageRows.value.findIndex((item) => item.id === row.id);
  if (index < 0) return row;
  const stateRow = materialUsageRows.value[index];
  if (stateRow && stateRow !== row) {
    Object.assign(stateRow, pickMaterialUsageRowState(row));
    return stateRow;
  }
  return row;
}

function isSecondGrindingRowEditable(row?: RoughSecondGrindingRow) {
  return !!row && !isRoughReadOnly() && row.confirmStatus !== '已确认';
}

function normalizeSecondGrindingRuntimeState(row: RoughSecondGrindingRow) {
  row.printStatus = row.printStatus || '未打印';
  row.confirmStatus = row.confirmStatus || '未确认';
  row.printCount = Number(row.printCount || 0);
  row.selectedForPrint = !!row.selectedForPrint;
  return row;
}

function pickSecondGrindingRowState(row: RoughSecondGrindingRow) {
  return {
    afterGrindingThickness: row.afterGrindingThickness,
    batchNo: row.batchNo,
    confirmStatus: row.confirmStatus,
    confirmTime: row.confirmTime,
    confirmedBatchNo: row.confirmedBatchNo,
    defectCode: row.defectCode,
    endTime: row.endTime,
    grindingMeters: row.grindingMeters,
    grindingThickness: row.grindingThickness,
    id: row.id,
    lineSpeed: row.lineSpeed,
    lossLength: row.lossLength,
    meterCounter: row.meterCounter,
    napSampleLength: row.napSampleLength,
    outputLength: row.outputLength,
    pressure: row.pressure,
    processLength: row.processLength,
    productionBatchNo: row.productionBatchNo,
    qualityThickness: row.qualityThickness,
    qualityWidth: row.qualityWidth,
    rotationSpeed: row.rotationSpeed,
    sandpaperBatchNo: row.sandpaperBatchNo,
    sandpaperLedgerBatchNo: row.sandpaperLedgerBatchNo,
    sandpaperLedgerId: row.sandpaperLedgerId,
    sandpaperLife: row.sandpaperLife,
    sandpaperLifeDays: row.sandpaperLifeDays,
    sandpaperNextUsageStatus: row.sandpaperNextUsageStatus,
    selectedForPrint: row.selectedForPrint,
    segmentMark: row.segmentMark,
    selfCheck: row.selfCheck,
    sourceRowId: row.sourceRowId,
    sourceType: row.sourceType,
    startTime: row.startTime,
    printCount: row.printCount,
    printStatus: row.printStatus,
    printTime: row.printTime,
    checkItems: row.checkItems,
  };
}

function syncSecondGrindingRowToState(row: RoughSecondGrindingRow) {
  const index = secondGrindingRows.value.findIndex((item) => item.id === row.id);
  if (index < 0) return row;
  const stateRow = secondGrindingRows.value[index];
  if (stateRow && stateRow !== row) {
    Object.assign(stateRow, pickSecondGrindingRowState(row));
    return stateRow;
  }
  return row;
}

function confirmAppendMaterialRow(continueAdding = false) {
  simulateMaterialScan();
  const form = materialScanForm.value;
  if (!form.batchNo) {
    AModal.warning({ title: '请先扫码或输入母料批次', content: '请输入或模拟扫码带出母料批次信息。' });
    return;
  }
  if (!form.planNo) {
    AModal.warning({ title: '计划号缺失', content: '请确认本次母料对应的计划号。' });
    return;
  }
  if (!form.processLength || form.processLength <= 0) {
    AModal.warning({ title: '请填写本次加工长度', content: '本次加工长度必须大于 0。' });
    return;
  }
  const requestedLength = roundGrindingNumber(Number(form.processLength || 0) + Number(form.lossLength || 0));
  if (requestedLength > Number(form.remainLength || 0)) {
    AModal.warning({ title: '可加工米数不足', content: '本次加工长度 + 加工损耗米数不能大于当前可加工米数，请重新确认。' });
    return;
  }
  const outputLength = Number(
    Math.max(0, Number(form.outputLength ?? 0) || Number(form.processLength || 0) - Number(form.lossLength || 0) - Number(form.napSampleLength || 0)).toFixed(3),
  );
  const rowId = `rough-usage-${Date.now()}-${materialUsageRows.value.length + 1}`;
  const checkItems = buildMaterialScanCheckItemsReq();
  const abnormalPositions = buildMaterialScanAbnormalPositions(rowId);
  if (abnormalPositions === null) return;
  materialUsageRows.value.push({
    id: rowId,
    batchNo: form.batchNo,
    planNo: form.planNo,
    remainStartMeter: Number(form.remainStartMeter || 0),
    remainLength: Number(form.remainLength || 0),
    processLength: Number(form.processLength || 0),
    lossLength: Number(form.lossLength || 0),
    outputLength,
    napSampleLength: Number(form.napSampleLength || 0),
    grindingPass: form.grindingPass,
    startTime: form.startTime,
    endTime: form.endTime,
    sandpaperLife: form.sandpaperLife,
    sandpaperLifeDays: form.sandpaperLifeDays,
    sandpaperBatchNo: form.sandpaperBatchNo,
    sandpaperLedgerBatchNo: form.sandpaperLedgerBatchNo,
    sandpaperLedgerId: form.sandpaperLedgerId,
    sandpaperNextUsageStatus: form.sandpaperNextUsageStatus,
    pressure: form.pressure,
    lineSpeed: form.lineSpeed,
    rotationSpeed: form.rotationSpeed,
    meterCounter: form.meterCounter,
    grindingThickness: form.grindingThickness,
    afterGrindingThickness: form.afterGrindingThickness,
    qualityThickness: form.qualityThickness,
    qualityWidth: form.qualityWidth,
    grindingMeters: form.grindingMeters,
    selfCheck: form.selfCheck,
    defectCode: form.defectCode,
    sourceType: form.sourceType,
    checkItems,
    abnormalPositions,
  });
  syncSegmentsFromMaterialRows();
  void persistRoughProgress();
  if (continueAdding) {
    prepareNextMaterialAppend();
    return;
  }
  materialScanVisible.value = false;
}

function removeMaterialUsageRow(rowId: string) {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  if (isFirstGrindingRowReferenced(rowId)) {
    AModal.warning({
      content: '该第一次磨皮记录已被第二次磨皮引用。请先移除对应第二次磨皮记录后，再处理该记录。',
      title: '已被二次磨皮引用',
    });
    return;
  }
  const target = materialUsageRows.value.find((item) => item.id === rowId);
  AModal.confirm({
    content: `确认移出第一次磨皮记录【${target?.batchNo || rowId}】？移出后会同步删除该记录关联的分段和台账草稿。`,
    okButtonProps: { danger: true },
    okText: '确认移出',
    onOk: async () => {
      materialUsageRows.value = materialUsageRows.value.filter((item) => item.id !== rowId);
      if (target) {
        stockLedgerRows.value = stockLedgerRows.value.filter(
          (item) =>
            item.batchNo !== target.batchNo &&
            item.batchNo !== `${target.batchNo}-RET` &&
            item.sourceRowId !== rowId,
        );
      }
      syncSegmentsFromMaterialRows();
      await persistRoughProgress();
    },
    title: '确认移出第一次磨皮记录',
  });
}

function handleMaterialUsageChange(row: RoughMaterialUsageRow) {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  if (isFirstGrindingRowReferenced(row.id)) {
    AModal.warning({
      content: '该第一次磨皮记录已被第二次磨皮引用，不能再修改。',
      title: '已被二次磨皮引用',
    });
    return;
  }
  const stateRow = syncMaterialUsageRowToState(row);
  stateRow.remainStartMeter = Number(Math.max(0, Number(stateRow.remainStartMeter || 0)).toFixed(3));
  stateRow.remainLength = Number(Math.max(0, Number(stateRow.remainLength || 0)).toFixed(3));
  stateRow.processLength = Number(Math.max(0, Math.min(Number(stateRow.processLength || 0), stateRow.remainLength)).toFixed(3));
  stateRow.lossLength = Number(Math.max(0, Number(stateRow.lossLength || 0)).toFixed(3));
  stateRow.napSampleLength = Number(Math.max(0, Number(stateRow.napSampleLength || 0)).toFixed(3));
  stateRow.outputLength = Number(
    Math.max(
      0,
      Number(stateRow.outputLength || 0) ||
        Number(stateRow.processLength || 0) - Number(stateRow.lossLength || 0) - Number(stateRow.napSampleLength || 0),
    ).toFixed(3),
  );
  stateRow.grindingMeters = stateRow.grindingMeters ? Number(Math.max(0, stateRow.grindingMeters).toFixed(3)) : stateRow.outputLength;
  if (stateRow !== row) {
    Object.assign(row, pickMaterialUsageRowState(stateRow));
  }
  syncSegmentsFromMaterialRows();
  queuePersistRoughProgress();
}

function getFirstGrindingFieldIndex(field: FirstGrindingEditableField) {
  return firstGrindingEditableFields.findIndex((item) => item === field);
}

function canUseHorizontalExcelNavigation(event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return true;
  const target = event.target;
  if (!(target instanceof HTMLInputElement) && !(target instanceof HTMLTextAreaElement)) return true;
  const value = target.value || '';
  const selectionStart = target.selectionStart ?? 0;
  const selectionEnd = target.selectionEnd ?? 0;
  if (selectionStart !== selectionEnd) return false;
  return event.key === 'ArrowLeft' ? selectionStart === 0 : selectionEnd === value.length;
}

function focusFirstGrindingCell(rowIndex: number, fieldIndex: number) {
  const safeRowIndex = Math.max(0, Math.min(rowIndex, materialUsageRows.value.length - 1));
  const targetRow = materialUsageRows.value[safeRowIndex];
  const targetField = firstGrindingEditableFields[fieldIndex];
  if (!targetRow || !targetField || !isFirstGrindingRowEditable(targetRow)) return;
  setTimeout(() => {
    const selector = `[data-first-grinding-row-id="${targetRow.id}"][data-first-grinding-field="${targetField}"]`;
    const cell = document.querySelector<HTMLElement>(selector);
    const input = cell?.querySelector<HTMLElement>('input, textarea, .ant-select-selector, [tabindex]:not([tabindex="-1"])');
    input?.focus();
    if (input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement) {
      input.select();
    }
  }, 0);
}

function handleFirstGrindingCellKeydown(event: KeyboardEvent, row: RoughMaterialUsageRow, field: FirstGrindingEditableField) {
  if (!isFirstGrindingRowEditable(row)) return;
  const currentRowIndex = materialUsageRows.value.findIndex((item) => item.id === row.id);
  const currentFieldIndex = getFirstGrindingFieldIndex(field);
  if (currentRowIndex < 0 || currentFieldIndex < 0) return;

  let nextRowIndex = currentRowIndex;
  let nextFieldIndex = currentFieldIndex;
  if (event.key === 'Enter' || event.key === 'ArrowDown') {
    nextRowIndex += 1;
  } else if (event.key === 'ArrowUp') {
    nextRowIndex -= 1;
  } else if (event.key === 'ArrowRight') {
    if (!canUseHorizontalExcelNavigation(event)) return;
    nextFieldIndex += 1;
  } else if (event.key === 'ArrowLeft') {
    if (!canUseHorizontalExcelNavigation(event)) return;
    nextFieldIndex -= 1;
  } else {
    return;
  }

  event.preventDefault();
  handleMaterialUsageChange(row);
  focusFirstGrindingCell(nextRowIndex, nextFieldIndex);
}

function confirmAppendSecondGrindingRow(continueAdding = false) {
  const form = secondGrindingForm.value;
  const editRowId = secondGrindingEditRowId.value;
  if (!form.sourceRowId) {
    AModal.warning({ title: '请选择一次磨皮来源', content: '第二次磨皮必须选择第一次磨皮已追加的母料记录。' });
    return;
  }
  syncSecondGrindingSource();
  if (!form.batchNo) {
    AModal.warning({ title: '母料批号缺失', content: '请先选择一次磨皮来源。' });
    return;
  }
  if (!form.processLength || form.processLength <= 0) {
    AModal.warning({ title: '请填写加工米数', content: '第二次磨皮加工米数必须大于 0。' });
    return;
  }
  const requestedLength = roundGrindingNumber(Number(form.processLength || 0) + Number(form.lossLength || 0));
  if (requestedLength > secondGrindingSourceAvailability.value) {
    AModal.warning({ title: '来源剩余不足', content: '第二次磨皮加工米数 + 加工损耗米数不能大于所选来源剩余米数，请重新确认。' });
    return;
  }
  const outputLength = Number(
    Math.max(
      0,
      Number(form.outputLength ?? 0) ||
        Number(form.processLength || 0) - Number(form.lossLength || 0) - Number(form.napSampleLength || 0),
    ).toFixed(3),
  );
  const usedLengthBeforeAppend = getSecondGrindingUsedLength(form.sourceRowId);
  const checkItems = buildSecondGrindingCheckItemsReq();
  const nextRowState = {
    sourceRowId: form.sourceRowId,
    sourceType: form.sourceType || undefined,
    batchNo: form.batchNo,
    productionBatchNo: form.productionBatchNo || buildSecondGrindingProductionBatchNo(form.segmentMark, form.batchNo),
    processLength: Number(form.processLength || 0),
    lossLength: Number(form.lossLength || 0),
    outputLength,
    napSampleLength: Number(form.napSampleLength || 0),
    segmentMark: form.segmentMark || '',
    startTime: form.startTime,
    endTime: form.endTime,
    sandpaperLife: form.sandpaperLife,
    sandpaperLifeDays: form.sandpaperLifeDays,
    sandpaperBatchNo: form.sandpaperBatchNo,
    sandpaperLedgerBatchNo: form.sandpaperLedgerBatchNo,
    sandpaperLedgerId: form.sandpaperLedgerId,
    sandpaperNextUsageStatus: form.sandpaperNextUsageStatus,
    pressure: form.pressure,
    lineSpeed: form.lineSpeed,
    rotationSpeed: form.rotationSpeed,
    meterCounter: form.meterCounter,
    grindingThickness: form.grindingThickness,
    afterGrindingThickness: form.afterGrindingThickness,
    selfCheck: form.selfCheck,
    defectCode: form.defectCode,
    qualityThickness: form.qualityThickness,
    qualityWidth: form.qualityWidth,
    grindingMeters: form.grindingMeters,
    checkItems,
  };
  if (editRowId) {
    const target = secondGrindingRows.value.find((item) => item.id === editRowId);
    if (!target) {
      AModal.warning({ title: '记录不存在', content: '当前要修改的第二次磨皮记录不存在，请关闭后重新选择。' });
      return;
    }
    if (target.confirmStatus === '已确认') {
      AModal.warning({ title: '已确认记录不能修改', content: '该分段已扫码确认并入账，不能再修改参数。' });
      return;
    }
    Object.assign(target, nextRowState);
    normalizeSecondGrindingRuntimeState(target);
    syncSecondGrindingLedgerFromRow(target);
    void persistRoughProgress();
    secondGrindingEditRowId.value = '';
    secondGrindingVisible.value = false;
    return;
  }
  const appendedRow: RoughSecondGrindingRow = {
    id: `rough-second-${Date.now()}-${secondGrindingRows.value.length + 1}`,
    ...nextRowState,
    confirmStatus: '未确认',
    printCount: 0,
    printStatus: '未打印',
    selectedForPrint: false,
  };
  secondGrindingRows.value.push(appendedRow);
  appendSecondGrindingToLedger(appendedRow, usedLengthBeforeAppend);
  void persistRoughProgress();
  if (continueAdding) {
    prepareNextSecondGrindingAppend();
    return;
  }
  secondGrindingVisible.value = false;
}

function removeSelectedSecondGrindingRows() {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  const rows = selectedSecondGrindingRows.value;
  if (!rows.length) {
    AModal.info({ title: '请选择记录', content: '请先选择需要移出的第二次磨皮记录。' });
    return;
  }
  const confirmedRows = rows.filter((row) => row.confirmStatus === '已确认');
  if (confirmedRows.length) {
    AModal.warning({
      title: '已确认记录不能移出',
      content: '选中记录包含已扫码确认并入账的分段，不能移出。',
    });
    return;
  }
  const rowIds = new Set(rows.map((row) => row.id));
  AModal.confirm({
    content: `确认移出选中的 ${rows.length} 条第二次磨皮记录？移出后会同步删除对应产出台账草稿。`,
    okButtonProps: { danger: true },
    okText: '确认移出',
    onOk: async () => {
      secondGrindingRows.value = secondGrindingRows.value.filter((item) => !rowIds.has(item.id));
      stockLedgerRows.value = stockLedgerRows.value.filter((item) => !item.secondGrindingRowId || !rowIds.has(item.secondGrindingRowId));
      await persistRoughProgress();
    },
    title: '确认批量移出第二次磨皮记录',
  });
}

function syncSecondGrindingLedgerFromRow(row: RoughSecondGrindingRow) {
  stockLedgerRows.value.forEach((ledger) => {
    if (ledger.secondGrindingRowId !== row.id) return;
    ledger.batchNo = row.productionBatchNo || row.batchNo;
    ledger.defectCode = row.defectCode;
    ledger.lossLength = row.lossLength;
    ledger.napSampleLength = row.napSampleLength;
    ledger.outputLength = row.outputLength;
    ledger.processLength = row.processLength;
    ledger.segmentNo = row.segmentMark || ledger.segmentNo;
    ledger.outputTime = row.endTime || ledger.outputTime;
    ledger.selfCheck = row.selfCheck || 'OK';
    ledger.thickness = row.qualityThickness ? Number(row.qualityThickness) : undefined;
    ledger.width = row.qualityWidth ? Number(row.qualityWidth) : undefined;
    ledger.length = row.outputLength;
  });
}

function handleSecondGrindingRowChange(row: RoughSecondGrindingRow) {
  if (isRoughReadOnly()) {
    warnRoughReadOnly();
    return;
  }
  const stateRow = syncSecondGrindingRowToState(row);
  stateRow.productionBatchNo = buildSecondGrindingProductionBatchNo(stateRow.segmentMark, stateRow.batchNo);
  stateRow.sandpaperLife = stateRow.sandpaperLife === undefined ? undefined : Number(Math.max(0, Number(stateRow.sandpaperLife || 0)).toFixed(3));
  stateRow.sandpaperLifeDays = stateRow.sandpaperLifeDays === undefined ? undefined : Number(Math.max(0, Number(stateRow.sandpaperLifeDays || 0)).toFixed(0));
  stateRow.grindingMeters = stateRow.grindingMeters === undefined ? undefined : Number(Math.max(0, Number(stateRow.grindingMeters || 0)).toFixed(3));
  stateRow.selfCheck = stateRow.selfCheck || 'OK';
  if (stateRow !== row) {
    Object.assign(row, pickSecondGrindingRowState(stateRow));
  }
  syncSecondGrindingLedgerFromRow(stateRow);
  queuePersistRoughProgress();
}

function handleSecondGrindingSelectionChange(
  row: RoughSecondGrindingRow,
  eventOrChecked: unknown,
) {
  const checked =
    typeof eventOrChecked === 'boolean'
      ? eventOrChecked
      : Boolean((eventOrChecked as { target?: { checked?: boolean } })?.target?.checked);
  const stateRow = secondGrindingRows.value.find((item) => item.id === row.id);
  if (stateRow) {
    stateRow.selectedForPrint = checked;
  }
  row.selectedForPrint = checked;
  secondGrindingSelectionVersion.value += 1;
}

function buildSecondGrindingPrintRows(selectedOnly = true) {
  return selectedOnly ? selectedSecondGrindingPrintRows.value : secondGrindingRows.value;
}

function escapePrintHtml(value?: unknown) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function resolveWorkOrderPrintSectionName(name?: string) {
  const value = String(name || '').trim();
  if (!value) return '';
  if (value.includes('配料')) return '配料';
  if (value.includes('湿法')) return '湿法';
  if (value.includes('磨皮') || value.includes('粗磨') || value.includes('精磨')) return '磨皮';
  if (value.includes('粘胶1')) return '粘胶1';
  if (value.includes('分切')) return '分切';
  if (value.includes('压槽')) return '压槽';
  if (value.includes('粘胶2') || value.includes('背胶')) return '粘胶2';
  if (value.includes('裁切') || value.includes('裁圆')) return '裁切';
  if (value.includes('入库') || value.includes('包装')) return '成品入库';
  return value;
}

function resolveWorkOrderLatestReportDate(operation: any) {
  if (operation?.latestReportDate) return String(operation.latestReportDate);
  const qtyByDate = operation?.reportQtyByDate;
  if (!qtyByDate || typeof qtyByDate !== 'object') return '';
  const keys = Object.keys(qtyByDate)
    .filter(Boolean)
    .sort((a, b) => dayjs(b).valueOf() - dayjs(a).valueOf());
  return keys[0] || '';
}

function formatPrintNumber(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '';
  return formatGrindingNumber(Number(value));
}

function createPrintQrDataUrl(value: string) {
  const source = ref(value);
  const qrCode = useQRCode(source, {
    errorCorrectionLevel: 'M',
    margin: 1,
    width: 128,
  });
  return new Promise<string>((resolve) => {
    const timer = window.setTimeout(() => {
      stop();
      resolve('');
    }, 1500);
    const stop = watch(
      qrCode,
      (dataUrl) => {
        if (!dataUrl) return;
        window.clearTimeout(timer);
        stop();
        resolve(dataUrl);
      },
      { immediate: true },
    );
  });
}

function buildSecondGrindingPrintRowsForWorkOrder(row: RoughSecondGrindingRow, planDetail: any) {
  const operations =
    planDetail?.operations?.length > 0
      ? planDetail.operations
      : [{ id: 'current', opName: task.value?.process || '磨皮' }];
  const currentSection = '磨皮';
  const operationSections = operations.map((operation: any) =>
    resolveWorkOrderPrintSectionName(operation?.opName),
  );
  const currentIndex = operationSections.findIndex((name: string) => name === currentSection);
  const currentProductionDate = normalizeDate(
    row.endTime || reportForm.value.productionDate || task.value?.productionDate || task.value?.productionStartDate,
  );

  return operations.map((operation: any, index: number) => {
    const sectionName = resolveWorkOrderPrintSectionName(operation?.opName);
    const config = WORK_ORDER_PROCESS_CONFIG[sectionName] || {};
    const isCurrent = sectionName === currentSection;
    const latestReportDate = resolveWorkOrderLatestReportDate(operation);
    const productionDate =
      isCurrent
        ? currentProductionDate
        : currentIndex >= 0 && index < currentIndex
          ? normalizeDate(latestReportDate)
          : '';
    return {
      confirmer: isCurrent
        ? reportForm.value.confirmerName || task.value?.confirmerName || ''
        : currentIndex >= 0 && index < currentIndex
          ? operation?.latestConfirmerName || ''
          : '',
      endTime: isCurrent
        ? normalizeDateTime(row.endTime || reportForm.value.endTime || task.value?.endTime)
        : currentIndex >= 0 && index < currentIndex
          ? normalizeDateTime(operation?.latestEndTime)
          : '',
      key: operation?.id || `${sectionName}-${index}`,
      metricLabel: config.metricLabel,
      metricValue: isCurrent ? formatPrintNumber(row.processLength) : '',
      name: sectionName,
      productionDate,
      progressed: currentIndex >= 0 && index <= currentIndex,
      recorder: isCurrent
        ? reportForm.value.recorderName || task.value?.recorderName || ''
        : currentIndex >= 0 && index < currentIndex
          ? operation?.latestRecorderName || ''
          : '',
      remark: isCurrent
        ? [
            `母料批号:${row.batchNo || '-'}`,
            `分段:${row.segmentMark || '-'}`,
            `损耗:${formatPrintNumber(row.lossLength) || '-'}`,
            `产出:${formatPrintNumber(row.outputLength) || '-'}`,
            `NAP:${formatPrintNumber(row.napSampleLength) || '-'}`,
            `自检:${row.selfCheck || '-'}`,
          ].join(' ')
        : currentIndex >= 0 && index < currentIndex
          ? operation?.latestRemark || ''
          : '',
      startTime: isCurrent
        ? normalizeDateTime(row.startTime || reportForm.value.startTime || task.value?.startTime)
        : currentIndex >= 0 && index < currentIndex
          ? normalizeDateTime(operation?.latestStartTime)
          : '',
    };
  });
}

async function buildSecondGrindingTicketHtml(rows: RoughSecondGrindingRow[], planDetail: any) {
  const operations =
    planDetail?.operations?.length > 0
      ? planDetail.operations
      : [{ id: 'current', opName: task.value?.process || '磨皮' }];
  const flowNodes = operations
    .map((operation: any, index: number) => ({
      current: resolveWorkOrderPrintSectionName(operation?.opName) === '磨皮',
      key: operation?.id || `${operation?.opName || 'op'}-${index}`,
      label: resolveWorkOrderPrintSectionName(operation?.opName),
    }))
    .filter((node: any) => !!node.label);
  const workOrderFlags = {
    child: true,
    mother: false,
  };
  const printTime = buildNowText();
  const ticketItems = await Promise.all(
    rows.map(async (row) => {
      const qrDataUrl = await createPrintQrDataUrl(
        buildTransferTicketQrValue(task.value?.planNo, row.productionBatchNo),
      );
      const processRows = buildSecondGrindingPrintRowsForWorkOrder(row, planDetail);
      return `
        <section class="print-sheet">
        <div class="print-dom-wrap">
        <div class="print-page" style="--process-line-height: 4.55mm; --section-font-size: 4.6mm; --client-section-font-size: 5.2mm;">
          <div class="sheet-header">
            <div class="sheet-logo"><img src="${formulaPrintLogo}" alt="HECHEN 禾臣" /></div>
            <div class="sheet-title">${WORK_ORDER_PRINT_DOC_CONFIG.title}</div>
            <div class="sheet-meta">
              <div class="sheet-meta__text">
                <div>编号: ${WORK_ORDER_PRINT_DOC_CONFIG.docCode}</div>
                <div>版本版次号: ${WORK_ORDER_PRINT_DOC_CONFIG.version}</div>
                <div>计划号: ${escapePrintHtml(task.value?.planNo || '-')}</div>
              </div>
              <div class="qr-box"><img src="${qrDataUrl}" alt="分段批次二维码" /></div>
            </div>
          </div>
          <table class="sheet-table">
            <tbody>
              <tr>
                <td class="sheet-section sheet-section--client" rowspan="3">客户<br />规范</td>
                <td colspan="2"><span class="sheet-strong">料号：</span>${escapePrintHtml(planDetail?.motherMaterialCode || planDetail?.materialCode || task.value?.materialCode || '-')}</td>
                <td colspan="2"><span class="sheet-strong">型号：</span>${escapePrintHtml(planDetail?.motherModelCode || planDetail?.modelCode || task.value?.materialName || task.value?.modelCode || '-')}</td>
                <td colspan="2"><span class="sheet-strong">生产批号：</span>${escapePrintHtml(row.productionBatchNo || '-')}</td>
              </tr>
              <tr>
                <td colspan="2">
                  <span class="check-box"><span>母工单</span><span class="check-mark">${workOrderFlags.mother ? '√' : ''}</span></span>
                </td>
                <td colspan="2">
                  <span class="check-box"><span>子工单</span><span class="check-mark">${workOrderFlags.child ? '√' : ''}</span></span>
                </td>
                <td colspan="2"></td>
              </tr>
              <tr>
                <td colspan="6" class="sheet-flow-cell">
                  <span class="sheet-strong">工艺流程：</span>
                  <span class="flow-line">
                    ${flowNodes
                      .map(
                        (node: any, index: number) =>
                          `<span class="flow-node${node.current ? ' flow-node--current' : ''}">${escapePrintHtml(node.label)}</span>${index < flowNodes.length - 1 ? '<span class="flow-arrow">→</span>' : ''}`,
                      )
                      .join('')}
                  </span>
                </td>
              </tr>
              ${processRows
                .map(
                  (processRow: any) => `
                    <tr>
                      <td class="sheet-section${processRow.progressed ? ' sheet-section--progressed' : ''}" rowspan="2">
                        <span class="sheet-section__marker"></span>
                        <span class="sheet-section__label">${escapePrintHtml(processRow.name)}</span>
                      </td>
                      <td colspan="6" class="process-cell process-cell--top">
                        <div class="process-line process-line--top">
                          <div class="process-field"><span class="sheet-strong">生产日期：</span>${escapePrintHtml(processRow.productionDate || '')}</div>
                          <div class="process-field"><span class="sheet-strong">开始时间：</span>${escapePrintHtml(processRow.startTime || '')}</div>
                          <div class="process-field"><span class="sheet-strong">结束时间：</span>${escapePrintHtml(processRow.endTime || '')}</div>
                          <div class="process-field process-field--metric">
                            <span class="sheet-strong">${escapePrintHtml(processRow.metricLabel || '')}</span>${processRow.metricLabel ? `：${escapePrintHtml(processRow.metricValue || '')}` : ''}
                          </div>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="6" class="process-cell process-cell--bottom">
                        <div class="process-line process-line--bottom">
                          <div class="process-field process-field--remark"><span class="sheet-strong">备注：</span>${escapePrintHtml(processRow.remark || '')}</div>
                          <div class="process-field"><span class="sheet-strong">担当：</span>${escapePrintHtml(processRow.recorder || '')}</div>
                          <div class="process-field"><span class="sheet-strong">确认：</span>${escapePrintHtml(processRow.confirmer || '')}</div>
                        </div>
                      </td>
                    </tr>`,
                )
                .join('')}
            </tbody>
          </table>
          <div class="sheet-footer">
            <span>制定/修订部门：${WORK_ORDER_PRINT_DOC_CONFIG.department}</span>
            <span>制定日期：${WORK_ORDER_PRINT_DOC_CONFIG.publishDate}</span>
            <span>修订日期：${normalizeDate(dayjs().format('YYYY-MM-DD'))}</span>
            <span>保管期限：${WORK_ORDER_PRINT_DOC_CONFIG.retention}</span>
            <span>打印时间：${escapePrintHtml(printTime)}</span>
          </div>
        </div>
        </div>
        </section>`;
    }),
  );
  return `
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>磨皮分段制造工单</title>
        <style>
          * { box-sizing: border-box; }
          html, body { margin: 0; padding: 0; background: #fff; }
          body { font-family: "Microsoft YaHei", sans-serif; color: #111; }
          @page { size: A4 landscape; margin: 0; }
          .print-sheet {
            width: 297mm;
            height: 210mm;
            padding: 4mm;
            margin: 0 auto;
            overflow: hidden;
            background: #fff;
            page-break-after: always;
          }
          .print-sheet:last-child { page-break-after: auto; }
          .print-dom-wrap {
            width: 100%;
            height: 100%;
            overflow: hidden;
          }
          .print-page {
            width: 277mm;
            height: 190mm;
            margin: 0 auto;
            padding: 2.5mm 3.5mm 1.5mm;
            background: #fff;
            overflow: hidden;
            color: #111;
            display: flex;
            flex-direction: column;
          }
          .sheet-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1px;
            min-height: 15mm;
          }
          .sheet-logo {
            width: 44mm;
            display: flex;
            align-items: center;
            justify-content: flex-start;
          }
          .sheet-logo img { width: 100%; height: auto; display: block; }
          .sheet-title {
            flex: 1;
            text-align: center;
            font-size: 7.8mm;
            font-weight: 700;
            text-decoration: underline;
            line-height: 1;
          }
          .sheet-meta {
            width: 58mm;
            min-height: 12mm;
            padding: 1mm 1.2mm 0.8mm;
            font-size: 3.1mm;
            line-height: 1.15;
            display: flex;
            flex-direction: row;
            gap: 2mm;
            flex-shrink: 0;
            align-items: flex-start;
            justify-content: space-between;
            border: 1px solid #111;
          }
          .sheet-meta__text {
            display: flex;
            flex-direction: column;
            gap: 0.4mm;
            align-items: flex-start;
            justify-content: center;
            white-space: nowrap;
          }
          .qr-box {
            width: 10.5mm;
            height: 10.5mm;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            flex-shrink: 0;
            margin-right: 0.6mm;
          }
          .qr-box img { width: 100%; height: 100%; display: block; }
          .sheet-table {
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #111;
            font-size: 3.45mm;
            table-layout: fixed;
            flex: 1 1 auto;
          }
          .sheet-table td,
          .sheet-table th {
            border: 1px solid #111;
            padding: 0.45mm 0.9mm;
            vertical-align: middle;
            line-height: 1;
          }
          .sheet-section {
            width: 15mm;
            text-align: center;
            font-size: var(--section-font-size, 4.3mm);
            font-weight: 700;
            position: relative;
            overflow: visible;
            padding-left: 3.2mm !important;
          }
          .sheet-section--client { font-size: var(--client-section-font-size, 5mm); }
          .sheet-section__marker {
            position: absolute;
            left: -1px;
            top: -1px;
            bottom: -1px;
            width: 0;
            border-left: 1.5mm solid #cbd5e1;
          }
          .sheet-section--progressed .sheet-section__marker { border-left-color: #1d4ed8; }
          .sheet-section__label { display: inline-block; position: relative; z-index: 1; }
          .sheet-strong { font-weight: 700; }
          .sheet-flow-cell {
            font-size: 3.1mm;
            line-height: 1.05;
            min-height: 11mm;
          }
          .flow-line {
            display: inline-flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 1.2mm;
          }
          .flow-node { display: inline-flex; align-items: center; padding: 0 0.8mm; }
          .flow-node--current { font-weight: 800; border: 1px dashed #111; }
          .flow-arrow { font-size: 3.4mm; }
          .check-box {
            display: inline-flex;
            align-items: center;
            gap: 1mm;
            margin-right: 5mm;
            font-size: 3.8mm;
          }
          .check-mark {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 4mm;
            height: 4mm;
            border: 1px solid #111;
            font-size: 3.5mm;
          }
          .process-line {
            display: flex;
            align-items: center;
            gap: 2mm;
            min-height: var(--process-line-height, 4.4mm);
          }
          .process-cell--top { border-bottom: none !important; }
          .process-cell--bottom { border-top: none !important; }
          .process-line--top,
          .process-line--bottom { justify-content: space-between; }
          .process-field {
            flex: 1 1 0;
            min-width: 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          .process-field--remark { flex: 2 1 0; }
          .sheet-footer {
            display: flex;
            justify-content: space-between;
            margin-top: auto;
            padding-top: 1mm;
            font-size: 3.2mm;
            line-height: 1.1;
            flex-shrink: 0;
          }
        </style>
      </head>
      <body>${ticketItems.join('')}</body>
    </html>`;
}

async function printSecondGrindingRows(rows: RoughSecondGrindingRow[]) {
  const validRows = rows.filter((row) => !!row.productionBatchNo);
  if (!validRows.length) {
    AModal.info({ title: '没有可打印分段', content: '请选择已有生产批次号的第二次磨皮分段记录。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const planDetail = task.value?.planId ? await getPlanOrderDetail(task.value.planId as any) : {};
  printWindow.document.write(await buildSecondGrindingTicketHtml(validRows, planDetail));
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 300);

  const now = buildNowText();
  validRows.forEach((row) => {
    row.printStatus = '已打印';
    row.printTime = now;
    row.printCount = Number(row.printCount || 0) + 1;
    row.selectedForPrint = false;
  });
  secondGrindingSelectionVersion.value += 1;
  await persistRoughProgress();
}

function printSelectedSecondGrindingRows() {
  void printSecondGrindingRows(buildSecondGrindingPrintRows(true));
}

function printAllSecondGrindingRows() {
  void printSecondGrindingRows(buildSecondGrindingPrintRows(false));
}

function openSecondGrindingConfirmDialog(row: RoughSecondGrindingRow) {
  secondGrindingConfirmForm.value = {
    rowId: row.id,
    scanCode: '',
    error: '',
    message: '',
  };
  secondGrindingConfirmVisible.value = true;
}

function openSelectedSecondGrindingConfirmDialog() {
  if (!secondGrindingRows.value.length) {
    AModal.info({ title: '暂无记录', content: '请先追加第二次磨皮记录后再扫码确认。' });
    return;
  }
  const selectedRow =
    selectedSecondGrindingRows.value.length === 1 && selectedSecondGrindingRows.value[0]?.confirmStatus !== '已确认'
      ? selectedSecondGrindingRows.value[0]
      : undefined;
  const row = selectedRow || secondGrindingRows.value.find((item) => item.confirmStatus !== '已确认');
  if (!row) {
    AModal.info({ title: '已全部确认', content: '第二次磨皮分段均已扫码确认。' });
    return;
  }
  openSecondGrindingConfirmDialog(row);
}

async function confirmSecondGrindingScan() {
  secondGrindingConfirmForm.value.error = '';
  secondGrindingConfirmForm.value.message = '';
  const scanned = resolveTransferTicketQrBusinessNo(secondGrindingConfirmForm.value.scanCode);
  if (!scanned) {
    secondGrindingConfirmForm.value.error = '请扫描或输入分段生产批次号。';
    return;
  }
  const currentRow = currentSecondGrindingConfirmRow.value;
  const matchedRow = secondGrindingRows.value.find((item) => String(item.productionBatchNo || '').trim() === scanned);
  const row = matchedRow || currentRow;
  const expected = String(row?.productionBatchNo || '').trim();
  if (!row || !expected || scanned !== expected) {
    secondGrindingConfirmForm.value.error = `未找到匹配的第二次磨皮分段批次：${scanned}`;
    return;
  }
  if (row.confirmStatus === '已确认') {
    secondGrindingConfirmForm.value.error = `分段批次 ${scanned} 已确认，无需重复扫码。`;
    return;
  }
  const now = buildNowText();
  row.confirmStatus = '已确认';
  row.confirmTime = now;
  row.confirmedBatchNo = scanned;
  row.selectedForPrint = false;
  await persistRoughProgress();
  const nextRow = secondGrindingRows.value.find((item) => item.confirmStatus !== '已确认');
  secondGrindingConfirmForm.value.rowId = nextRow?.id || '';
  secondGrindingConfirmForm.value.scanCode = '';
  secondGrindingConfirmForm.value.message = `分段批次 ${scanned} 已确认，报工已入账。请继续扫描下一张流转单。`;
}

function getSecondGrindingFieldIndex(field: SecondGrindingEditableField) {
  return secondGrindingEditableFields.findIndex((item) => item === field);
}

function focusSecondGrindingCell(rowIndex: number, fieldIndex: number) {
  const safeRowIndex = Math.max(0, Math.min(rowIndex, secondGrindingRows.value.length - 1));
  const targetRow = secondGrindingRows.value[safeRowIndex];
  const targetField = secondGrindingEditableFields[fieldIndex];
  if (!targetRow || !targetField || !isSecondGrindingRowEditable(targetRow)) return;
  setTimeout(() => {
    const selector = `[data-second-grinding-row-id="${targetRow.id}"][data-second-grinding-field="${targetField}"]`;
    const cell = document.querySelector<HTMLElement>(selector);
    const input = cell?.querySelector<HTMLElement>('input, textarea, .ant-select-selector, [tabindex]:not([tabindex="-1"])');
    input?.focus();
    if (input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement) {
      input.select();
    }
  }, 0);
}

function handleSecondGrindingCellKeydown(event: KeyboardEvent, row: RoughSecondGrindingRow, field: SecondGrindingEditableField) {
  if (!isSecondGrindingRowEditable(row)) return;
  const currentRowIndex = secondGrindingRows.value.findIndex((item) => item.id === row.id);
  const currentFieldIndex = getSecondGrindingFieldIndex(field);
  if (currentRowIndex < 0 || currentFieldIndex < 0) return;

  let nextRowIndex = currentRowIndex;
  let nextFieldIndex = currentFieldIndex;
  if (event.key === 'Enter' || event.key === 'ArrowDown') {
    nextRowIndex += 1;
  } else if (event.key === 'ArrowUp') {
    nextRowIndex -= 1;
  } else if (event.key === 'ArrowRight') {
    if (!canUseHorizontalExcelNavigation(event)) return;
    nextFieldIndex += 1;
  } else if (event.key === 'ArrowLeft') {
    if (!canUseHorizontalExcelNavigation(event)) return;
    nextFieldIndex -= 1;
  } else {
    return;
  }

  event.preventDefault();
  handleSecondGrindingRowChange(row);
  focusSecondGrindingCell(nextRowIndex, nextFieldIndex);
}

function appendStockLedgerRow(partial: Omit<RoughStockLedgerRow, 'id' | 'ledgerNo'>) {
  stockLedgerRows.value.push({
    ...partial,
    id: `ledger-${stockLedgerRows.value.length + 1}`,
    ledgerNo: buildLedgerNo(stockLedgerRows.value.length),
  });
}

function appendSecondGrindingToLedger(row: RoughSecondGrindingRow, startMeter: number) {
  appendStockLedgerRow({
    batchNo: row.productionBatchNo || row.batchNo,
    defectCode: row.defectCode,
    length: row.outputLength,
    lossLength: row.lossLength,
    machineCode: currentMachineCode.value,
    napSampleLength: row.napSampleLength,
    outputLength: row.outputLength,
    outputTime: row.endTime || buildNowText(),
    processLength: row.processLength,
    secondGrindingRowId: row.id,
    segmentNo: row.segmentMark || `2ND-${String(stockLedgerRows.value.length + 1).padStart(2, '0')}`,
    selfCheck: row.selfCheck || 'OK',
    sourceRowId: row.sourceRowId,
    sourceType: '二次磨皮产出',
    startMeter: Number(startMeter.toFixed(3)),
    status: '已产出',
    thickness: row.qualityThickness ? Number(row.qualityThickness) : undefined,
    width: row.qualityWidth ? Number(row.qualityWidth) : undefined,
  });
}

function writeSegmentToLedger(segment: RoughSegmentRow) {
  if (segment.status === 'STORED') return;
  appendStockLedgerRow({
    length: segment.length,
    batchNo: segment.batchNo,
    segmentNo: segment.segmentNo,
    machineCode: currentMachineCode.value,
    outputTime: buildNowText(),
    selfCheck: segment.selfCheck,
    thickness: segment.thickness,
    width: segment.width,
    sourceType: '产出子卷',
    startMeter: segment.startMeter,
    status: '已产出',
  });
  segment.status = 'STORED';
}

function pushRemainingToStock(row: RoughMaterialUsageRow) {
  if (!row.remainLength || row.remainLength <= 0) {
    AModal.info({
      title: '无需退库',
      content: '当前母卷已无剩余可退回边库。',
      okText: '关闭',
    });
    return;
  }
  appendStockLedgerRow({
    batchNo: `${row.batchNo}-RET`,
    segmentNo: 'RET',
    length: row.remainLength,
    machineCode: currentMachineCode.value,
    outputTime: buildNowText(),
    selfCheck: 'OK',
    thickness: row.qualityThickness ? Number(row.qualityThickness) : undefined,
    width: row.qualityWidth ? Number(row.qualityWidth) : undefined,
    sourceType: '边库余料',
    startMeter: Number(row.remainStartMeter.toFixed(3)),
    status: '已入边库',
  });
  row.remainLength = 0;
  syncSegmentsFromMaterialRows();
}

const selectedSegments = computed(() => segmentRows.value.filter((item) => item.selected));

const allPendingSegmentsChecked = computed({
  get: () => {
    const pendingRows = segmentRows.value.filter((item) => item.status !== 'STORED');
    return pendingRows.length > 0 && pendingRows.every((item) => item.selected);
  },
  set: (checked: boolean) => {
    segmentRows.value.forEach((item) => {
      if (item.status !== 'STORED') item.selected = checked;
    });
  },
});

const [MaterialUsageGrid, materialUsageGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    columns: [
      { field: 'batchNo', title: '母料批次', minWidth: 160, showOverflow: 'tooltip', slots: { default: 'batchNo' } },
      { field: 'processLength', title: '加工米数(m)', minWidth: 110, slots: { default: 'processLength' } },
      { field: 'lossLength', title: '加工损耗米数(m)', minWidth: 130, slots: { default: 'lossLength' } },
      { field: 'outputLength', title: '产出米数(m)', minWidth: 110, slots: { default: 'outputLength' } },
      { field: 'napSampleLength', title: 'NAP留样米数(m)', minWidth: 130, slots: { default: 'napSampleLength' } },
      { field: 'startTime', title: '开始时间', minWidth: 160, slots: { default: 'startTime' } },
      { field: 'endTime', title: '结束时间', minWidth: 160, slots: { default: 'endTime' } },
      { field: 'sandpaperLife', title: '砂纸累计使用(米)', minWidth: 120, slots: { default: 'sandpaperLife' } },
      { field: 'sandpaperLifeDays', title: '砂纸累计使用天', minWidth: 120, slots: { default: 'sandpaperLifeDays' } },
      { field: 'sandpaperBatchNo', title: '砂纸批号', minWidth: 120, slots: { default: 'sandpaperBatchNo' } },
      { field: 'pressure', title: '气压', minWidth: 88, slots: { default: 'pressure' } },
      { field: 'lineSpeed', title: '线速', minWidth: 88, slots: { default: 'lineSpeed' } },
      { field: 'rotationSpeed', title: '转速', minWidth: 88, slots: { default: 'rotationSpeed' } },
      { field: 'meterCounter', title: '计米器', minWidth: 96, slots: { default: 'meterCounter' } },
      { field: 'grindingThickness', title: '磨皮厚度', minWidth: 100, slots: { default: 'grindingThickness' } },
      { field: 'afterGrindingThickness', title: '磨皮后厚度', minWidth: 116, slots: { default: 'afterGrindingThickness' } },
      { field: 'selfCheck', title: '自检', minWidth: 88, align: 'center', slots: { default: 'selfCheck' } },
      { field: 'defectCode', title: '不良代码', minWidth: 120, slots: { default: 'defectCode' } },
      { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'materialActions' } },
    ],
    data: [],
    height: '100%',
    keepSource: true,
    pagerConfig: { enabled: false },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { enabled: false },
    scrollY: { enabled: true },
    scrollX: { enabled: true },
    showOverflow: true,
  } as VxeTableGridOptions,
});

const [SegmentGrid, segmentGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    columns: [
      { field: 'selected', title: '选择', width: 70, align: 'center', slots: { default: 'segmentSelect' } },
      { field: 'batchNo', title: '母卷批号', minWidth: 180, showOverflow: 'tooltip' },
      { field: 'segmentNo', title: '分段号', minWidth: 110, align: 'center' },
      { field: 'startMeter', title: '起米(m)', minWidth: 100, align: 'right' },
      { field: 'length', title: '长度(m)', minWidth: 100, align: 'right' },
      { field: 'selfCheck', title: '自检', minWidth: 110, align: 'center', slots: { default: 'segmentSelfCheck' } },
      { field: 'thickness', title: '厚度(mm)', minWidth: 110, slots: { default: 'segmentThickness' } },
      { field: 'width', title: '宽幅(mm)', minWidth: 110, slots: { default: 'segmentWidth' } },
      { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'segmentStatus' } },
      { title: '操作', width: 110, fixed: 'right', align: 'center', slots: { default: 'segmentActions' } },
    ],
    data: [],
    height: '100%',
    keepSource: true,
    pagerConfig: { enabled: false },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { enabled: false },
    scrollY: { enabled: true },
    scrollX: { enabled: true },
    showOverflow: true,
  } as VxeTableGridOptions,
});

const [SecondGrindingGrid, secondGrindingGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    columns: [
      { field: 'selectedForPrint', title: '选择', width: 70, fixed: 'left', align: 'center', slots: { default: 'secondPrintSelect' } },
      { field: 'productionBatchNo', title: '批次号', minWidth: 180, showOverflow: 'tooltip' },
      { field: 'sourceType', title: '母批来源', minWidth: 100, formatter: ({ row }: any) => row.sourceType || '-' },
      { field: 'batchNo', title: '母料批号', minWidth: 160, showOverflow: 'tooltip' },
      { field: 'processLength', title: '加工米数(m)', minWidth: 110, align: 'right' },
      { field: 'lossLength', title: '加工损耗米数(m)', minWidth: 130, align: 'right' },
      { field: 'outputLength', title: '产出米数(m)', minWidth: 110, align: 'right' },
      { field: 'napSampleLength', title: 'NAP留样米数(m)', minWidth: 130, align: 'right' },
      { field: 'segmentMark', title: '分段标记', minWidth: 120, slots: { default: 'secondSegmentMark' } },
      { field: 'startTime', title: '开始时间', minWidth: 160, slots: { default: 'secondStartTime' } },
      { field: 'endTime', title: '结束时间', minWidth: 160, slots: { default: 'secondEndTime' } },
      { field: 'sandpaperLife', title: '砂纸累计使用(米)', minWidth: 120, align: 'right', slots: { default: 'secondSandpaperLife' } },
      { field: 'sandpaperLifeDays', title: '砂纸累计使用天', minWidth: 120, align: 'right', slots: { default: 'secondSandpaperLifeDays' } },
      { field: 'sandpaperBatchNo', title: '砂纸批号', minWidth: 120, slots: { default: 'secondSandpaperBatchNo' } },
      { field: 'pressure', title: '气压', minWidth: 88, slots: { default: 'secondPressure' } },
      { field: 'lineSpeed', title: '线速', minWidth: 88, slots: { default: 'secondLineSpeed' } },
      { field: 'rotationSpeed', title: '转速', minWidth: 88, slots: { default: 'secondRotationSpeed' } },
      { field: 'meterCounter', title: '计米器', minWidth: 96, slots: { default: 'secondMeterCounter' } },
      { field: 'grindingThickness', title: '磨皮厚度', minWidth: 100, slots: { default: 'secondGrindingThickness' } },
      { field: 'afterGrindingThickness', title: '磨皮后厚度', minWidth: 116, slots: { default: 'secondAfterGrindingThickness' } },
      { field: 'selfCheck', title: '自检', minWidth: 88, align: 'center', slots: { default: 'secondSelfCheck' } },
      { field: 'defectCode', title: '不良代码', minWidth: 120, slots: { default: 'secondDefectCode' } },
      { field: 'qualityThickness', title: '质量结果-厚度', minWidth: 116, slots: { default: 'secondQualityThickness' } },
      { field: 'qualityWidth', title: '质量结果-磨后宽幅', minWidth: 126, slots: { default: 'secondQualityWidth' } },
      { field: 'grindingMeters', title: '质量结果-磨皮米', minWidth: 126, align: 'right', slots: { default: 'secondGrindingMeters' } },
      { field: 'printStatus', title: '打印标记', minWidth: 98, fixed: 'right', align: 'center', slots: { default: 'secondPrintStatus' } },
      { field: 'confirmStatus', title: '确认状态', minWidth: 98, fixed: 'right', align: 'center', slots: { default: 'secondConfirmStatus' } },
    ],
    data: [],
    height: '100%',
    keepSource: true,
    pagerConfig: { enabled: false },
    rowClassName: ({ row }: any) => {
      const classes = [];
      if (row.printStatus === '已打印') classes.push('rough-second-row--printed');
      if (row.confirmStatus === '已确认') classes.push('rough-second-row--confirmed');
      return classes.join(' ');
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { enabled: false },
    scrollY: { enabled: true },
    scrollX: { enabled: true },
    showOverflow: true,
  } as VxeTableGridOptions,
});

watch(
  materialUsageRows,
  (rows) => {
    materialUsageGridApi.setGridOptions({ data: rows as any[] });
  },
  { deep: true, immediate: true },
);

watch(
  segmentRows,
  (rows) => {
    segmentGridApi.setGridOptions({ data: rows as any[] });
  },
  { deep: true, immediate: true },
);

watch(
  secondGrindingRows,
  (rows) => {
    secondGrindingGridApi.setGridOptions({ data: rows as any[] });
  },
  { deep: true, immediate: true },
);

function removeSelectedSegments() {
  const selected = selectedSegments.value.filter((item) => item.status !== 'STORED');
  if (!selected.length) {
    AModal.info({ title: '请先选择分段', content: '至少选择一条待入账分段后再执行移出。' });
    return;
  }
  const removeIds = new Set(selected.map((item) => item.parentId));
  if ([...removeIds].some((id) => isFirstGrindingRowReferenced(id))) {
    AModal.warning({
      content: '选中的分段包含已被第二次磨皮引用的第一次磨皮记录，不能移出。',
      title: '已被二次磨皮引用',
    });
    return;
  }
  AModal.confirm({
    content: '确认移出选中的待入账分段？对应第一次磨皮记录也会同步移出。',
    okButtonProps: { danger: true },
    okText: '确认移出',
    onOk: async () => {
      materialUsageRows.value = materialUsageRows.value.filter((item) => !removeIds.has(item.id));
      syncSegmentsFromMaterialRows();
      await persistRoughProgress();
    },
    title: '确认移出分段',
  });
}

function batchWriteSegmentsToLedger() {
  const pendingRows = segmentRows.value.filter((item) => item.status !== 'STORED');
  if (!pendingRows.length) {
    AModal.info({ title: '没有可入账数据', content: '当前分段记录均已入账。' });
    return;
  }
  pendingRows.forEach((item) => writeSegmentToLedger(item));
}

function printSelectedSegments() {
  const rows = selectedSegments.value.length ? selectedSegments.value : segmentRows.value.filter((item) => item.status === 'STORED');
  if (!rows.length) {
    AModal.info({ title: '请先选择打印对象', content: '请选择待打印分段，或先执行入账后再打印。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=920,height=700');
  if (!printWindow) return;
  const rowsHtml = rows
    .map(
      (row, index) => `
        <tr>
          <td>${index + 1}</td>
          <td>${row.batchNo}</td>
          <td>${row.segmentNo}</td>
          <td>${row.startMeter}</td>
          <td>${row.length}</td>
          <td>${row.selfCheck}</td>
          <td>${row.thickness ?? ''}</td>
          <td>${row.width ?? ''}</td>
        </tr>`,
    )
    .join('');
  printWindow.document.write(`
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>磨皮分段入库打印</title>
        <style>
          @page { size: A4 portrait; margin: 10mm; }
          body { font-family: "Microsoft YaHei", sans-serif; margin: 0; padding: 0; }
          h2 { text-align: center; margin: 0 0 10px; }
          table { width: 100%; border-collapse: collapse; }
          th, td { border: 1px solid #000; padding: 6px; font-size: 12px; text-align: center; }
        </style>
      </head>
      <body>
        <h2>磨皮分段入库清单</h2>
        <table>
          <thead>
            <tr>
              <th>序号</th>
              <th>母卷批号</th>
              <th>分段号</th>
              <th>起米(m)</th>
              <th>长度(m)</th>
              <th>自检</th>
              <th>厚度(mm)</th>
              <th>宽幅(mm)</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
      </body>
    </html>`);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 300);
}

function printStockTicket(row: RoughStockLedgerRow) {
  const printWindow = window.open('', '_blank', 'width=760,height=640');
  if (!printWindow) return;
  const html = `
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>磨皮单卷入库签</title>
        <style>
          @page { size: A5 portrait; margin: 8mm; }
          body { font-family: "SimSun", "Microsoft YaHei", sans-serif; margin: 0; padding: 0; }
          .ticket { border: 1px solid #000; padding: 12mm; }
          .title { text-align: center; font-size: 20px; font-weight: 700; margin-bottom: 12px; }
          .row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
          .row b { font-size: 16px; }
        </style>
      </head>
      <body>
          <div class="ticket">
          <div class="title">磨皮单卷入库签</div>
          <div class="row"><span>台账单号</span><b>${row.ledgerNo}</b></div>
          <div class="row"><span>母卷批号</span><b>${row.batchNo}</b></div>
          <div class="row"><span>分段号</span><b>${row.segmentNo}</b></div>
          <div class="row"><span>加工机台</span><b>${row.machineCode}</b></div>
          <div class="row"><span>起米(m)</span><b>${row.startMeter}</b></div>
          <div class="row"><span>长度(m)</span><b>${row.length}</b></div>
          <div class="row"><span>自检结果</span><b>${row.selfCheck}</b></div>
          <div class="row"><span>记录时间</span><b>${row.outputTime}</b></div>
        </div>
      </body>
    </html>
  `;
  printWindow.document.write(html);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 300);
}

async function loadEquipmentOptions() {
  const workCenterId = task.value?.workCenterId;
  let list: any[] = [];
  const primaryPage = await getEquipmentPage({
    pageNo: 1,
    pageSize: 200,
    workCenterId,
    status: 0,
  } as any);
  list = primaryPage?.list || [];
  if (list.length === 0) {
    const fallbackPage = await getEquipmentPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
    } as any);
    list = fallbackPage?.list || [];
  }
  equipmentOptions.value = list.map((item: any) => ({
    code: item.equipmentCode,
    label: item.equipmentCode && item.equipmentName ? `${item.equipmentCode} / ${item.equipmentName}` : item.equipmentCode || item.equipmentName,
    name: item.equipmentName,
    value: item.id,
  }));
}

async function loadEquipmentStatusCard() {
  if (!reportForm.value.equipmentId) {
    equipmentStatusCard.value = {
      currentEndTime: '-',
      currentOperationName: '-',
      currentOperatorName: '-',
      currentPlanNo: '-',
      currentStartTime: '-',
      equipmentLabel: currentMachineCode.value || '未挂接设备',
      statusMeta: getEquipmentWorkStatusMeta(),
    };
    return;
  }
  const detail = await getEquipment(reportForm.value.equipmentId);
  equipmentStatusCard.value = {
    currentEndTime: normalizeDateTime(detail?.currentEndTime) || '-',
    currentOperationName: detail?.currentOperationName || detail?.currentOperationCode || '-',
    currentOperatorName: detail?.currentOperatorName || '-',
    currentPlanNo: detail?.currentPlanNo || '-',
    currentStartTime: normalizeDateTime(detail?.currentStartTime) || '-',
    equipmentLabel:
      detail?.equipmentCode && detail?.equipmentName
        ? `${detail.equipmentCode} / ${detail.equipmentName}`
        : detail?.equipmentCode || detail?.equipmentName || currentMachineCode.value || '未挂接设备',
    statusMeta: getEquipmentWorkStatusMeta(detail?.workStatus),
  };
}

function initStateFromTask() {
  const extra: any = parseRoughExtraJson(task.value?.extraJson);
  reportForm.value = {
    confirmerName: task.value?.confirmerName || '',
    confirmerTime: task.value?.confirmerTime || '',
    equipmentCode: task.value?.equipmentCode || '',
    equipmentId: task.value?.equipmentId,
    equipmentName: task.value?.equipmentName || '',
    endTime: task.value?.endTime || '',
    inputLength: undefined,
    outputLength: task.value?.goodQty ?? undefined,
    grindingPass: '一次',
    sandpaperLife: undefined,
    sandpaperBatchNo: '',
    guideClothBatchNo: '',
    guideClothLedgerBatchNo: '',
    guideClothLedgerId: undefined,
    guideClothNextUsageStatus: undefined,
    guideClothLifeCount: undefined,
    changeReason: '',
    productionDate:
      task.value?.productionDate ||
      (task.value?.startTime ? normalizeDate(task.value.startTime) : '') ||
      '',
    recorderName: task.value?.recorderName || '',
    recorderTime: task.value?.recorderTime || '',
    remark: task.value?.reportRemark || '',
    startTime: task.value?.startTime || '',
  };
  if (extra.reportForm && typeof extra.reportForm === 'object') {
    reportForm.value = {
      ...reportForm.value,
      ...extra.reportForm,
      confirmerName: task.value?.confirmerName || extra.reportForm.confirmerName || reportForm.value.confirmerName,
      confirmerTime: task.value?.confirmerTime || extra.reportForm.confirmerTime || reportForm.value.confirmerTime,
      equipmentCode: task.value?.equipmentCode || extra.reportForm.equipmentCode || reportForm.value.equipmentCode,
      equipmentId: task.value?.equipmentId ?? extra.reportForm.equipmentId ?? reportForm.value.equipmentId,
      equipmentName: task.value?.equipmentName || extra.reportForm.equipmentName || reportForm.value.equipmentName,
      endTime: task.value?.endTime || extra.reportForm.endTime || reportForm.value.endTime,
      productionDate: task.value?.productionDate || extra.reportForm.productionDate || reportForm.value.productionDate,
      recorderName: task.value?.recorderName || extra.reportForm.recorderName || reportForm.value.recorderName,
      recorderTime: task.value?.recorderTime || extra.reportForm.recorderTime || reportForm.value.recorderTime,
      startTime: task.value?.startTime || extra.reportForm.startTime || reportForm.value.startTime,
    };
  }
  workPrepareRows.value = Array.isArray(extra.workPrepareRows) ? extra.workPrepareRows : createWorkPrepareRows();
  productionCheckRows.value = Array.isArray(extra.productionCheckRows) ? extra.productionCheckRows : createProductionCheckRows();
  semiFinishedRows.value = Array.isArray(extra.semiFinishedRows) ? extra.semiFinishedRows : createSemiFinishedRows();
  materialUsageRows.value = Array.isArray(extra.materialUsageRows)
    ? extra.materialUsageRows.map((row: RoughMaterialUsageRow) => ({
        ...row,
        abnormalPositions: cloneRoughAbnormalPositionRows(row.abnormalPositions),
        checkItems: Array.isArray(row.checkItems) ? cloneRoughGrindingCheckItems(row.checkItems) : [],
      }))
    : [];
  secondGrindingRows.value = Array.isArray(extra.secondGrindingRows)
    ? extra.secondGrindingRows.map((row: RoughSecondGrindingRow, index: number) => {
        const source = materialUsageRows.value.find((item) => item.id === row.sourceRowId);
        return normalizeSecondGrindingRuntimeState({
          ...row,
          checkItems: Array.isArray(row.checkItems) ? cloneRoughGrindingCheckItems(row.checkItems) : [],
          sourceType: row.sourceType || source?.sourceType,
          productionBatchNo: buildSecondGrindingProductionBatchNo(row.segmentMark, row.batchNo),
        });
      })
    : [];
  segmentRows.value = Array.isArray(extra.segmentRows) ? extra.segmentRows : [];
  stockLedgerRows.value = Array.isArray(extra.stockLedgerRows) ? extra.stockLedgerRows : [];
  secondGrindingRows.value.forEach((row) => syncSecondGrindingLedgerFromRow(row));
  materialSourceCatalog.value = extra.materialSourceCatalog && typeof extra.materialSourceCatalog === 'object' ? extra.materialSourceCatalog : {};
  syncPreviousOperationMaterialSourceEntry();
  firstInspection.value = extra.firstInspection && typeof extra.firstInspection === 'object'
    ? extra.firstInspection
    : {
        inspectionDesc: '',
        inspectTime: '',
        inspector: '',
        result: '',
        status: 'PENDING',
      };
  if (!segmentRows.value.length && materialUsageRows.value.length) {
    syncSegmentsFromMaterialRows();
  }
}

function isStarted() {
  return !!reportForm.value.startTime || task.value?.status === 'COMPLETED' || task.value?.status === 'IN_PROGRESS';
}

function isRoughReadOnly() {
  return task.value?.status === 'COMPLETED';
}

function warnRoughReadOnly() {
  AModal.warning({
    content: '当前磨皮工序已完工，不能再修改一次/二次磨皮记录。',
    title: '当前工序已完工',
  });
}

function getStatusMeta(status?: string) {
  if (status === 'COMPLETED') return { color: 'success', text: '已完成' };
  if (status === 'WAITING') return { color: 'processing', text: '待结果' };
  return { color: 'default', text: '待处理' };
}

function getEquipmentWorkStatusMeta(status?: string) {
  if (status === 'PRODUCING') return { color: 'processing', text: '生产中' };
  if (status === 'MAINTENANCE') return { color: 'warning', text: '检修' };
  if (status === 'FAULT') return { color: 'error', text: '故障' };
  return { color: 'default', text: '待机' };
}

function getDetailFieldRowSpan(details: WetSheetDetail[] = [], index: number, field: 'category' | 'node') {
  const current = details[index];
  if (!current?.[field]) return 0;
  if (index > 0 && details[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < details.length; cursor += 1) {
    if (details[cursor]?.[field] === current[field]) span += 1;
    else break;
  }
  return span;
}

const displayStatus = computed(() => {
  if (task.value?.status === 'COMPLETED') return { color: 'success', text: '已完工' };
  if (task.value?.status === 'IN_PROGRESS') return { color: 'processing', text: '生产中' };
  if (task.value?.status === 'RELEASED') return { color: 'processing', text: '已下达' };
  return { color: 'default', text: '待开工' };
});

const currentMachineCode = computed(
  () => reportForm.value.equipmentCode || task.value?.equipmentCode || task.value?.equipmentName || task.value?.deviceId || '-',
);

const reportView = computed(() => ({
  ...reportForm.value,
  confirmerTimeDisplay: normalizeDateTime(reportForm.value.confirmerTime),
  endTimeDisplay: normalizeDateTime(reportForm.value.endTime),
  productionDateDisplay: normalizeDate(reportForm.value.productionDate),
  recorderTimeDisplay: normalizeDateTime(reportForm.value.recorderTime),
  startTimeDisplay: formatStartTimeForSummary(reportForm.value.productionDate, reportForm.value.startTime),
}));

const recordDialogTitle = computed(() =>
  `${recordDialogMode.value === 'edit' ? '填写' : recordDialogMode.value === 'confirm' ? '确认' : '查看'}${currentRecord.value?.name || '记录单'}`,
);
const recordConfirmAuthActionName = computed(() =>
  `确认${currentRecord.value?.name || '记录单'}`,
);

const recordDialogTopFields = computed(() => {
  const record = currentRecord.value;
  if (!record) return [];
  if (recordDialogCategory.value === 'prepare') {
    return [
      { field: 'machine', label: '机台编号', value: currentMachineCode.value },
      { field: 'recorder', label: '记录人', value: record.recorder || '-' },
      { field: 'recorderTime', label: '记录时间', value: normalizeDateTime(record.recorderTime) || '-' },
      { field: 'confirmer', label: '确认人', value: record.confirmer || '-' },
      { field: 'confirmerTime', label: '确认时间', value: normalizeDateTime(record.confirmerTime) || '-' },
    ];
  }
  if (recordDialogCategory.value === 'production-check') {
    return [
      { field: 'materialCode', label: '生产料号', value: task.value?.materialCode || '-' },
      { field: 'modelCode', label: '生产型号', value: task.value?.modelCode || '-' },
      { field: 'batchNo', label: '生产批号', value: task.value?.batchNo || '-' },
      { field: 'productionDate', label: '生产日期', value: reportView.value.productionDateDisplay || '-' },
      { field: 'machine', label: '机台编号', value: currentMachineCode.value },
      { field: 'recorder', label: '记录人', value: record.recorder || '-' },
      { field: 'recorderTime', label: '记录时间', value: normalizeDateTime(record.recorderTime) || '-' },
      { field: 'confirmer', label: '确认人', value: record.confirmer || '-' },
      { field: 'confirmerTime', label: '确认时间', value: normalizeDateTime(record.confirmerTime) || '-' },
    ];
  }
  return [
    { field: 'machine', label: '机台编号', value: currentMachineCode.value },
    { field: 'length', label: '中间品长度', value: record.generatedLength ? `${record.generatedLength} m` : '-' },
    { field: 'recorder', label: '记录人', value: record.recorder || '-' },
    { field: 'confirmer', label: '确认人', value: record.confirmer || '-' },
    { field: 'recorderTime', label: '记录时间', value: normalizeDateTime(record.recorderTime) || '-' },
  ];
});

const semiFinishedHeadFields = computed(() => {
  if (recordDialogCategory.value !== 'semi-finished' || !currentRecord.value) return [];
  const isSolidify = currentRecord.value.name?.includes('凝固');
  const fields: Array<any> = [
    { key: 'productionDate', label: '生产日期：', type: 'readonly', value: reportView.value.productionDateDisplay || '-' },
    { key: 'modelCode', label: '生产型号：', type: 'readonly', value: task.value?.modelCode || '-' },
    { key: 'batchNo', label: '产品批号：', type: 'readonly', value: task.value?.batchNo || '-' },
    { key: 'generatedLength', label: '中间品长度/m：', type: 'input-number' },
    { key: 'semiWidth', label: isSolidify ? '出槽宽幅/m(xxm)：' : '宽幅/m(XXm)：', type: 'input' },
  ];
  if (isSolidify) {
    fields.push({ key: 'poreDevelopment', label: '泡孔发育：', type: 'radio' });
  }
  fields.push({ key: 'finalResult', label: isSolidify ? '综合判定：' : '判定：', type: 'radio' });

  const fillers = [
    { key: 'recorder', label: '记录人：', type: 'readonly', value: currentRecord.value.recorder || reportForm.value.recorderName || '-' },
    {
      key: 'recorderTime',
      label: '记录时间：',
      type: 'readonly',
      value: normalizeDateTime(currentRecord.value.recorderTime) || reportView.value.recorderTimeDisplay || '-',
    },
    { key: 'confirmer', label: '确认人：', type: 'readonly', value: currentRecord.value.confirmer || reportForm.value.confirmerName || '-' },
    {
      key: 'confirmerTime',
      label: '确认时间：',
      type: 'readonly',
      value: normalizeDateTime(currentRecord.value.confirmerTime) || reportView.value.confirmerTimeDisplay || '-',
    },
  ];

  let index = 0;
  while (fields.length % 4 !== 0 && index < fillers.length) {
    fields.push(fillers[index]);
    index += 1;
  }
  return fields;
});

const recordDialogDetailRows = computed(() => {
  const details = currentRecord.value?.details || [];
  if (recordDialogCategory.value !== 'semi-finished') return details;
  const start = (semiDetailPage.value - 1) * SEMI_DETAIL_PAGE_SIZE;
  return details.slice(start, start + SEMI_DETAIL_PAGE_SIZE);
});

function getSemiDetailLength(_row: WetSheetDetail, index: number) {
  const absoluteIndex =
    recordDialogCategory.value === 'semi-finished'
      ? (semiDetailPage.value - 1) * SEMI_DETAIL_PAGE_SIZE + index
      : index;
  return (absoluteIndex + 1) * 2;
}

// 分段/台账能力暂时隐藏，保留实现供后续恢复入口时直接复用。
void productionEndDateDisplay;
void pushRemainingToStock;
void allPendingSegmentsChecked;
void SegmentGrid;
void removeSelectedSegments;
void batchWriteSegmentsToLedger;
void printSelectedSegments;
void printStockTicket;
void getSemiDetailLength;

function updateRecord(targetRows: WetSheetRow[], updated: WetSheetRow) {
  const index = targetRows.findIndex((item) => item.id === updated.id);
  if (index >= 0) targetRows[index] = { ...updated };
}

function generateSemiDetails(totalLength: number): WetSheetDetail[] {
  const rowCount = Math.max(1, Math.ceil(totalLength / 2));
  return Array.from({ length: rowCount }).map((_, index) => {
    const start = index * 2;
    const end = Math.min(totalLength, start + 2);
    return {
      item: '',
      node: '',
      category: '',
      length: start,
      remark: '',
      result: 'OK',
      segment: `${start}-${end}m`,
      seq: index + 1,
      value: '',
    };
  });
}

function handleSemiLengthChange(length?: number) {
  if (!currentRecord.value || recordDialogCategory.value !== 'semi-finished') return;
  if (!length || length <= 0) {
    currentRecord.value.details = [];
    return;
  }
  currentRecord.value.generatedLength = length;
  currentRecord.value.details = generateSemiDetails(length);
}

function openRecord(record: WetSheetRow, category: RecordCategory, mode: RecordMode) {
  if (!isStarted()) {
    AModal.warning({
      content: '磨皮工序必须先开工后，才能填写或确认相关记录单。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (category === 'semi-finished' && (!record.details || record.details.length === 0)) {
    const cloned = { ...record };
    cloned.generatedLength = cloned.generatedLength || 300;
    cloned.details = generateSemiDetails(cloned.generatedLength);
    cloned.result = cloned.result || '已生成';
    updateRecord(semiFinishedRows.value, cloned);
    record = cloned;
  }
  recordDialogCategory.value = category;
  recordDialogMode.value = mode;
  semiDetailPage.value = 1;
  currentRecord.value = JSON.parse(JSON.stringify(record));
  actionPanelExpanded.value = false;
  recordActionForm.value = {
    confirmer: record.confirmer || reportForm.value.confirmerName || '',
    confirmerTime: record.confirmerTime || reportForm.value.confirmerTime || '',
    confirmRemark: record.remark || '',
    formRemark: record.remark || '',
    inspectionResult: record.result === '异常已确认' ? 'NG' : 'OK',
    recorder: record.recorder || reportForm.value.recorderName || '',
    recorderTime: record.recorderTime || reportForm.value.recorderTime || '',
    result: record.result === '异常待确认' ? 'NG' : 'OK',
  };
  recordDialogVisible.value = true;
}

function closeRecordDialog() {
  recordDialogVisible.value = false;
  currentRecord.value = null;
}

function saveCurrentRecord() {
  if (!currentRecord.value) return;
  const now = buildNowText();
  const updated: WetSheetRow = {
    ...currentRecord.value,
    recorder: reportForm.value.recorderName || currentRecord.value.recorder || '-',
    recorderTime: now,
    remark: recordActionForm.value.formRemark,
    result: recordActionForm.value.result === 'OK' ? '已填写' : '异常待确认',
    status: 'PENDING',
  };
  if (recordDialogCategory.value === 'prepare') updateRecord(workPrepareRows.value, updated);
  if (recordDialogCategory.value === 'production-check') updateRecord(productionCheckRows.value, updated);
  if (recordDialogCategory.value === 'semi-finished') updateRecord(semiFinishedRows.value, updated);
  void persistRoughProgress();
  closeRecordDialog();
}

function confirmCurrentRecord() {
  if (!currentRecord.value) return;
  const now = buildNowText();
  const updated: WetSheetRow = {
    ...currentRecord.value,
    confirmer: recordActionForm.value.confirmer || reportForm.value.confirmerName || reportForm.value.recorderName || '-',
    confirmerTime: recordActionForm.value.confirmerTime || now,
    recorder: currentRecord.value.recorder || reportForm.value.recorderName || '-',
    recorderTime: currentRecord.value.recorderTime || reportForm.value.recorderTime || now,
    remark: recordActionForm.value.confirmRemark || recordActionForm.value.formRemark,
    result: recordActionForm.value.inspectionResult === 'OK' ? '已确认' : '异常已确认',
    status: 'COMPLETED',
  };
  if (recordDialogCategory.value === 'prepare') updateRecord(workPrepareRows.value, updated);
  if (recordDialogCategory.value === 'production-check') updateRecord(productionCheckRows.value, updated);
  if (recordDialogCategory.value === 'semi-finished') updateRecord(semiFinishedRows.value, updated);
  void persistRoughProgress();
  closeRecordDialog();
}

function openRecordConfirmAuth() {
  if (!currentRecord.value) return;
  recordConfirmAuthVisible.value = true;
}

function handleRecordConfirmAuthSuccess(userInfo: any) {
  const confirmer = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || '';
  if (confirmer) {
    recordActionForm.value.confirmer = confirmer;
  }
  recordActionForm.value.confirmerTime = buildNowText();
  recordConfirmAuthVisible.value = false;
  confirmCurrentRecord();
}

function submitFirstInspection() {
  if (!isStarted()) {
    AModal.warning({
      content: '磨皮工序必须先开工后才能提交首检申请。',
      title: '请先执行开工确认',
    });
    return;
  }
  AModal.confirm({
    cancelText: '取消',
    content: '确认提交首检申请后，任务状态将变为“已发送 QMS，请将样品送至品质部（打印首检任务单）”。',
    okText: '确认提交',
    onOk: () => {
      firstInspection.value.status = 'WAITING';
      firstInspection.value.result = '已发送QMS，请将样品送至品质部（打印首检任务单）';
      void persistRoughProgress();
      if (firstInspectionTimer.value) clearTimeout(firstInspectionTimer.value);
      firstInspectionTimer.value = setTimeout(() => {
        firstInspection.value = {
          inspectionDesc: '表面状态正常，厚度检测合格，可继续后续工序。',
          inspectTime: buildNowText(),
          inspector: '品质检验员A',
          result: '首检合格',
          status: 'COMPLETED',
        };
        void persistRoughProgress();
      }, 60_000);
    },
    title: '提交首检申请',
  });
}

function viewFirstInspectionDetail() {
  AModal.info({
    content: `检验结果：${firstInspection.value.result || '-'}\n检验说明：${firstInspection.value.inspectionDesc || '-'}\n检验人：${firstInspection.value.inspector || '-'}\n检验时间：${firstInspection.value.inspectTime || '-'}`,
    okText: '关闭',
    title: '首检结果详情',
  });
}

async function handleStartClick() {
  if (previousOperationInfo.value && previousOperationInfo.value.status.text !== '已完成') {
    AModal.warning({
      content: `前序工序【${previousOperationInfo.value.opName || '未知工序'}】尚未完成报工，请先完成前序工序过站报工后再执行磨皮开工确认。`,
      title: '前序工序尚未报工',
    });
    return;
  }
  await loadEquipmentOptions();
  pendingAction.value = 'START';
  authVisible.value = true;
}

function handleFinishClick() {
  if (!isStarted()) {
    AModal.warning({
      content: '磨皮工序必须先完成开工确认后，才允许进入过站报工。',
      title: '请先执行开工确认',
    });
    return;
  }
  const now = buildNowText();
  if (!reportForm.value.productionDate) reportForm.value.productionDate = dayjs().format('YYYY-MM-DD');
  if (!reportForm.value.startTime) reportForm.value.startTime = now;
  if (!reportForm.value.recorderTime) reportForm.value.recorderTime = now;
  pendingAction.value = 'FINISH';
  authVisible.value = true;
}

async function openDeviceSwitchModal() {
  if (!isStarted() || task.value?.status === 'COMPLETED') {
    AModal.warning({
      content: '磨皮工序开工后且未完工前才允许切换工位设备。',
      title: '不能切换设备',
    });
    return;
  }
  deviceSwitchForm.value = {
    equipmentId: reportForm.value.equipmentId,
  };
  deviceSwitchVisible.value = true;
  await loadEquipmentOptions();
  await loadEquipmentStatusCard();
}

async function handleDeviceSwitchConfirm() {
  if (!deviceSwitchForm.value.equipmentId) {
    AModal.warning({
      content: '请选择要切换的磨皮工位设备。',
      title: '请选择机台编号',
    });
    return;
  }
  const target = equipmentOptions.value.find((item: any) => item.value === deviceSwitchForm.value.equipmentId);
  if (!target) {
    AModal.warning({
      content: '当前机台编号不在可选设备范围内，请重新选择。',
      title: '机台编号无效',
    });
    return;
  }
  deviceSwitchSubmitting.value = true;
  try {
    await switchRoughGrindingEquipment({
      equipmentCode: target.code,
      equipmentId: deviceSwitchForm.value.equipmentId,
      equipmentName: target.name,
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
    } as any);
    reportForm.value.equipmentId = deviceSwitchForm.value.equipmentId;
    reportForm.value.equipmentCode = target.code || '';
    reportForm.value.equipmentName = target.name || '';
    if (task.value) {
      task.value = {
        ...task.value,
        equipmentCode: reportForm.value.equipmentCode,
        equipmentId: reportForm.value.equipmentId,
        equipmentName: reportForm.value.equipmentName,
      };
      emit('taskChange', { ...task.value });
    }
    await persistRoughProgress();
    await loadEquipmentStatusCard();
    deviceSwitchVisible.value = false;
  } finally {
    deviceSwitchSubmitting.value = false;
  }
}

async function handleAuthSuccess(userInfo: any) {
  const now = buildNowText();
  if (pendingAction.value === 'START') {
    const effectiveProductionDate = reportForm.value.productionDate || dayjs().format('YYYY-MM-DD');
    reportForm.value.productionDate = effectiveProductionDate;
    reportForm.value.equipmentId = userInfo.equipmentId;
    reportForm.value.equipmentCode = userInfo.equipmentCode || '';
    reportForm.value.equipmentName = userInfo.equipmentName || '';
    reportForm.value.startTime = now;
    reportForm.value.recorderName = userInfo.empName;
    reportForm.value.recorderTime = now;
    await startRoughGrindingReport({
      equipmentCode: reportForm.value.equipmentCode || undefined,
      equipmentId: reportForm.value.equipmentId,
      equipmentName: reportForm.value.equipmentName || undefined,
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      recorderName: reportForm.value.recorderName || undefined,
      recorderTime: reportForm.value.recorderTime || undefined,
      reportDate: effectiveProductionDate,
      startTime: reportForm.value.startTime || undefined,
    } as any);
    if (task.value) {
      task.value = {
        ...task.value,
        equipmentCode: reportForm.value.equipmentCode,
        equipmentId: reportForm.value.equipmentId,
        equipmentName: reportForm.value.equipmentName,
        productionDate: effectiveProductionDate,
        recorderName: reportForm.value.recorderName,
        recorderTime: reportForm.value.recorderTime,
        startTime: reportForm.value.startTime,
        status: 'IN_PROGRESS',
      };
      emit('taskChange', { ...task.value });
    }
    await loadEquipmentStatusCard();
  } else if (pendingAction.value === 'FINISH') {
    reportForm.value.endTime = now;
    reportForm.value.confirmerName = userInfo.empName;
    reportForm.value.confirmerTime = now;
    viewMode.value = 'booking';
  }
  pendingAction.value = null;
}

async function handleBookingSubmit() {
  const finalLength = Number(bookingReportQty.value ?? 0);
  const reportDateText = reportForm.value.productionDate || dayjs().format('YYYY-MM-DD');
  reportForm.value.productionDate = reportDateText;
  let bookingSubmitted = false;
  if (typeof task.value?.submitBooking === 'function') {
    await task.value.submitBooking({
      ...buildRoughReportPayload(true),
      confirmerName: reportForm.value.confirmerName || undefined,
      confirmerTime: reportForm.value.confirmerTime || undefined,
      equipmentCode: reportForm.value.equipmentCode || undefined,
      equipmentId: reportForm.value.equipmentId || undefined,
      equipmentName: reportForm.value.equipmentName || undefined,
      endTime: reportForm.value.endTime || undefined,
      goodQty: finalLength,
      inputLength: firstGrindingAggregate.value.processLength,
      outputLength: secondGrindingAggregate.value.outputLength,
      guideClothBatchNo: reportForm.value.guideClothBatchNo || undefined,
      guideClothLifeCount: reportForm.value.guideClothLifeCount,
      recorderName: reportForm.value.recorderName || undefined,
      recorderTime: reportForm.value.recorderTime || undefined,
      remark: reportForm.value.remark || undefined,
      reportDate: reportDateText,
      scrapQty: 0,
      startTime: reportForm.value.startTime || undefined,
    });
    bookingSubmitted = true;
  }
  if (bookingSubmitted) {
    try {
      await updateRoughConsumableUsageStatusesAfterBooking();
    } catch {
      message.warning('磨皮报工已提交，但边库耗材使用状态更新失败，请在边库台账中核对处理');
    }
  }
    task.value = {
      ...task.value,
      confirmerName: reportForm.value.confirmerName,
      confirmerTime: reportForm.value.confirmerTime,
      equipmentCode: reportForm.value.equipmentCode,
      equipmentId: reportForm.value.equipmentId,
      equipmentName: reportForm.value.equipmentName,
      endTime: reportForm.value.endTime,
      extraJson: buildRoughSnapshotExtra(),
      goodQty: finalLength,
      inputLength: firstGrindingAggregate.value.processLength,
      outputLength: secondGrindingAggregate.value.outputLength,
      productionDate: reportDateText,
      recorderName: reportForm.value.recorderName,
      recorderTime: reportForm.value.recorderTime,
      reportRemark: reportForm.value.remark,
    startTime: reportForm.value.startTime,
    status: 'COMPLETED',
  };
  emit('taskChange', { ...task.value });
  AModal.success({
    content: `已按报工日期 ${reportDateText} 回写计划日期列，二次磨皮总米数/报工量：${finalLength} ${task.value?.uom || 'm'}。`,
    okText: '知道了',
    title: '磨皮报工成功',
  });
  modalApi.close();
  emit('refresh');
}

function handleBookingConfirm() {
  const finalLength = Number(bookingReportQty.value ?? 0);
  const reportDateText = reportForm.value.productionDate || dayjs().format('YYYY-MM-DD');
  AModal.confirm({
    cancelText: '取消',
    content: `确认后将按报工日期 ${reportDateText} 提交当前磨皮工序，二次磨皮总米数/报工量 ${finalLength} ${task.value?.uom || 'm'} 将回写到对应计划日期列，是否继续？`,
    okText: '确认提交',
    onOk: async () => handleBookingSubmit(),
    title: '确认过站报工',
  });
}

async function handleCloseClick() {
  if (roughProgressPersistTimer.value) {
    clearTimeout(roughProgressPersistTimer.value);
    roughProgressPersistTimer.value = null;
    await persistRoughProgress();
  }
  emit('refresh');
  modalApi.close();
}

const [PrintPreviewModal, printPreviewModalApi] = useVbenModal({
  connectedComponent: FormulaReportPrintPreviewModal,
});

const [FirstInspectionPrintPreviewModal, firstInspectionPrintPreviewModalApi] = useVbenModal({
  connectedComponent: RoughGrindingFirstInspectionPrintPreviewModal,
});

async function handlePrintClick() {
  if (!task.value?.planId) {
    AModal.warning({
      content: '当前任务未关联生产计划，无法生成打印预览。',
      title: '无法打印',
    });
    return;
  }
  const planDetail = await getPlanOrderDetail(task.value.planId as any);
  printPreviewModalApi
    .setData({
      planDetail,
      reportForm: { ...(reportForm.value || {}) },
      startForm: { ...(reportForm.value || {}) },
      task: { ...(task.value || {}) },
    })
    .open();
}

function handleFirstInspectionPrintClick() {
  firstInspectionPrintPreviewModalApi
    .setData({
      firstInspection: { ...firstInspection.value },
      task: { ...(task.value || {}) },
    })
    .open();
}

const [Modal, modalApi] = useVbenModal({
  class: 'hc-workstation-detail-modal',
  closable: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  onOpenChange(isOpen) {
      if (isOpen) {
        modalApi.setState({ fullscreen: true });
        task.value = modalApi.getData<any>();
        activeMainTab.value = 'prepare';
        viewMode.value = 'tabs';
        initStateFromTask();
        void loadEquipmentOptions();
        void loadEquipmentStatusCard();
      }
  },
  showCancelButton: false,
  showConfirmButton: false,
});

onBeforeUnmount(() => {
  if (firstInspectionTimer.value) clearTimeout(firstInspectionTimer.value);
  if (roughProgressPersistTimer.value) clearTimeout(roughProgressPersistTimer.value);
});
</script>

<template>
  <Modal>
    <div class="pp-plan-modal">
      <div class="pp-plan-toolbar">
        <div class="pp-plan-toolbar__title">
          <span class="pp-plan-toolbar__main">磨皮报工工作台</span>
          <span v-if="task?.productionStartDate" class="pp-plan-toolbar__sub">计划生产日期：{{ task.productionStartDate }}</span>
        </div>

        <div class="pp-plan-toolbar__actions">
          <template v-if="viewMode === 'tabs'">
            <Button size="small" :disabled="task?.status === 'COMPLETED' || task?.status === 'IN_PROGRESS'" @click="handleStartClick">
              <IconifyIcon icon="lucide:play" class="mr-1" />
              {{ task?.status === 'IN_PROGRESS' ? '已开工生产中' : '执行开工确认' }}
            </Button>
            <Button size="small" type="primary" danger :disabled="task?.status === 'COMPLETED'" @click="handleFinishClick">
              <IconifyIcon icon="lucide:check-square" class="mr-1" /> 过站报工
            </Button>
            <Button size="small" :disabled="!isStarted() || task?.status === 'COMPLETED'" @click="openDeviceSwitchModal">
              <IconifyIcon icon="lucide:repeat" class="mr-1" /> 切换工位设备
            </Button>
            <Button size="small" @click="handlePrintClick">
              <IconifyIcon icon="lucide:printer" class="mr-1" /> 打印报工单
            </Button>
            <Button size="small" @click="handleCloseClick">
              <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭窗口
            </Button>
          </template>
          <template v-else>
            <Button size="small" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回过程作业
            </Button>
            <Button size="small" @click="handlePrintClick">
              <IconifyIcon icon="lucide:printer" class="mr-1" /> 打印报工单
            </Button>
            <Button size="small" @click="handleCloseClick">
              <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭窗口
            </Button>
          </template>
        </div>
      </div>

      <div v-show="viewMode === 'tabs'" class="pp-plan-body">
        <div v-if="previousOperationInfo" class="wet-prev-op-bar">
          <div class="wet-prev-op-bar__title">前序工序信息</div>
          <div class="wet-prev-op-bar__content">
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">前序工序</span>
              <span class="wet-prev-op-bar__value">{{ previousOperationInfo.opName }}</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">状态</span>
              <Tag :color="previousOperationInfo.status.color" class="!m-0">{{ previousOperationInfo.status.text }}</Tag>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">开始时间</span>
              <span class="wet-prev-op-bar__value">{{ previousOperationInfo.startTime }}</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">结束时间</span>
              <span class="wet-prev-op-bar__value">{{ previousOperationInfo.endTime }}</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">担当</span>
              <span class="wet-prev-op-bar__value">{{ previousOperationInfo.recorderName }}</span>
            </div>
          </div>
        </div>

        <div class="wet-prev-op-bar">
          <div class="wet-prev-op-bar__title">前序母料信息</div>
          <div class="wet-prev-op-bar__content">
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">前工序湿法产出料号</span>
              <span class="wet-prev-op-bar__value">{{ previousMaterialInfo.previousMaterialCode }}</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">收卷米数(报工数)</span>
              <span class="wet-prev-op-bar__value">{{ previousMaterialInfo.sourceRollLength }} m</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">已磨皮米数</span>
              <span class="wet-prev-op-bar__value">{{ previousMaterialInfo.finishedUsedLength }} m</span>
            </div>
            <div class="wet-prev-op-bar__item">
              <span class="wet-prev-op-bar__label">剩余米数</span>
              <span class="wet-prev-op-bar__value">{{ previousMaterialInfo.remainingLength }} m</span>
            </div>
          </div>
        </div>

          <fieldset class="pp-fieldset">
            <legend>1. 基本信息</legend>
            <div class="pp-form-grid">
              <div class="pp-form-item">
                <label>生产计划号</label>
                <div class="pp-readonly-box pp-readonly-box--code">{{ task?.planNo || task?.id || '-' }}</div>
              </div>
              <div class="pp-form-item">
                <label>母料型号</label>
                <div class="pp-readonly-box pp-readonly-box--code">{{ task?.motherModelCode || '-' }}</div>
              </div>
              <div class="pp-form-item">
                <label>母料批号</label>
                <div class="pp-readonly-box">{{ task?.batchNo || '-' }}</div>
              </div>
              <div class="pp-form-item">
                <label>产品型号</label>
                <div class="pp-readonly-box pp-readonly-box--code">{{ task?.modelCode || '-' }}</div>
              </div>
              <div class="pp-form-item">
                <label>产品料号</label>
                <div class="pp-readonly-box">{{ task?.materialCode || task?.product || '-' }}</div>
              </div>
              <div class="pp-form-item">
                <label>状态</label>
                <div class="pp-readonly-box">
                  <Tag :color="displayStatus.color" class="!m-0">{{ displayStatus.text }}</Tag>
                </div>
              </div>
              <div class="pp-form-item">
                <label>开始时间</label>
                <div class="pp-readonly-box">{{ reportView.startTimeDisplay || '-' }}</div>
              </div>
              <div class="pp-form-item">
              <label>结束时间</label>
              <div class="pp-readonly-box">{{ reportView.endTimeDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>记录人</label>
              <div class="pp-readonly-box">{{ reportForm.recorderName || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>记录时间</label>
              <div class="pp-readonly-box">{{ reportView.recorderTimeDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item pp-form-item--full">
              <label>执行要求</label>
              <div class="pp-readonly-box pp-readonly-box--multiline">{{ task?.requirements || '-' }}</div>
            </div>
          </div>
        </fieldset>

        <div class="wet-tabs-shell">
          <Tabs v-model:activeKey="activeMainTab" class="custom-main-tabs flex h-full flex-col">
            <TabPane key="prepare" tab="工作准备" class="h-full">
              <div class="pass-work-panel">
                <div class="pp-panel">
                  <div class="pp-panel__header">
                    <div class="pp-panel__title">
                      <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-[#1677ff]" />
                      工作准备
                    </div>
                    <div class="pp-panel__desc">CMP软垫磨皮开机点检表、CMP软垫磨皮设备清洁点检表合并展示</div>
                  </div>
                  <div class="pp-table-wrap pass-work-list-wrap">
                    <table class="pp-grid">
                      <thead>
                        <tr>
                          <th width="260">表单名称</th>
                          <th width="130">执行时机</th>
                          <th width="120">单据状态</th>
                          <th width="120">执行结果</th>
                          <th width="180">记录人/时间</th>
                          <th width="180">确认人/确认时间</th>
                          <th width="180">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="record in workPrepareRows" :key="record.id">
                          <td>{{ record.name || '-' }}</td>
                          <td align="center">{{ record.timing || '-' }}</td>
                          <td align="center">
                            <Tag :color="getStatusMeta(record.status).color" class="!m-0">{{ getStatusMeta(record.status).text }}</Tag>
                          </td>
                          <td align="center">{{ record.result || '-' }}</td>
                          <td>
                            <div>{{ record.recorder || '-' }}</div>
                            <div class="pp-grid__sub">{{ normalizeDateTime(record.recorderTime) || '-' }}</div>
                          </td>
                          <td>
                            <div>{{ record.confirmer || '-' }}</div>
                            <div class="pp-grid__sub">{{ normalizeDateTime(record.confirmerTime) || '-' }}</div>
                          </td>
                          <td align="center">
                            <div class="pass-work-actions">
                              <Button size="small" type="link" :disabled="record.status === 'COMPLETED'" @click="openRecord(record, 'prepare', 'edit')">
                                填写
                              </Button>
                              <Button size="small" type="link" :disabled="record.status === 'COMPLETED'" @click="openRecord(record, 'prepare', 'confirm')">
                                确认
                              </Button>
                              <Button size="small" type="link" @click="openRecord(record, 'prepare', 'view')">查看</Button>
                            </div>
                          </td>
                        </tr>
                        <tr v-if="!workPrepareRows.length">
                          <td colspan="7" class="pp-empty-cell" align="center">暂无过站工作数据</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="first-grinding" tab="第一次磨皮" class="h-full">
              <div class="pass-work-panel rough-usage-tab">
                <div class="rough-process-workspace" :class="{ 'rough-process-workspace--maximized': materialUsageMaximized }">
                  <div class="rough-process-panel">
                    <div class="rough-grid-toolbar">
                      <div class="rough-grid-toolbar__title">第一次磨皮记录</div>
                      <div class="pass-work-actions rough-grid-toolbar__actions">
                        <Button size="small" @click="toggleMaterialUsageMaximized">
                          {{ materialUsageMaximized ? '还原布局' : '最大化' }}
                        </Button>
                        <Button size="small" type="primary" :disabled="isRoughReadOnly()" @click="openMaterialScanDialog('扫码母料')">按工序报工</Button>
                        <Button size="small" :disabled="isRoughReadOnly()" @click="openMaterialScanDialog('边库余料')">引用边库母料</Button>
                      </div>
                    </div>
                    <div class="rough-grid-wrap rough-grid-wrap--auto">
                      <MaterialUsageGrid
                        v-if="activeMainTab === 'first-grinding'"
                        class="rough-grid-component"
                      >
                        <template #batchNo="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="batchNo">
                            <Input v-model:value="row.batchNo" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'batchNo')" />
                          </div>
                        </template>
                        <template #processLength="{ row }">
                          <div class="rough-readonly-cell">{{ formatGrindingNumber(row.processLength) }}</div>
                        </template>
                        <template #lossLength="{ row }">
                          <div class="rough-readonly-cell">{{ formatGrindingNumber(row.lossLength) }}</div>
                        </template>
                        <template #outputLength="{ row }">
                          <div class="rough-readonly-cell">{{ formatGrindingNumber(row.outputLength) }}</div>
                        </template>
                        <template #napSampleLength="{ row }">
                          <div class="rough-readonly-cell">{{ formatGrindingNumber(row.napSampleLength) }}</div>
                        </template>
                        <template #startTime="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="startTime">
                            <DatePicker v-model:value="row.startTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'startTime')" />
                          </div>
                        </template>
                        <template #endTime="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="endTime">
                            <DatePicker v-model:value="row.endTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'endTime')" />
                          </div>
                        </template>
                        <template #sandpaperLife="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="sandpaperLife">
                            <InputNumber v-model:value="row.sandpaperLife" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" :min="0" :precision="3" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'sandpaperLife')" />
                          </div>
                        </template>
                        <template #sandpaperLifeDays="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="sandpaperLifeDays">
                            <InputNumber v-model:value="row.sandpaperLifeDays" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" :min="0" :precision="0" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'sandpaperLifeDays')" />
                          </div>
                        </template>
                        <template #sandpaperBatchNo="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="sandpaperBatchNo">
                            <Input v-model:value="row.sandpaperBatchNo" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleFirstGrindingSandpaperBatchInput(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'sandpaperBatchNo')">
                              <template #suffix>
                                <Tooltip title="选择磨皮砂纸边库批次">
                                  <Button
                                    class="rough-consumable-picker-button"
                                    size="small"
                                    type="text"
                                    :disabled="!isFirstGrindingRowEditable(row)"
                                    @click.stop="selectFirstGrindingSandpaper(row)"
                                  >
                                    <IconifyIcon icon="lucide:package-search" />
                                  </Button>
                                </Tooltip>
                              </template>
                            </Input>
                          </div>
                        </template>
                        <template #pressure="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="pressure">
                            <Input v-model:value="row.pressure" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'pressure')" />
                          </div>
                        </template>
                        <template #lineSpeed="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="lineSpeed">
                            <Input v-model:value="row.lineSpeed" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'lineSpeed')" />
                          </div>
                        </template>
                        <template #rotationSpeed="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="rotationSpeed">
                            <Input v-model:value="row.rotationSpeed" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'rotationSpeed')" />
                          </div>
                        </template>
                        <template #meterCounter="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="meterCounter">
                            <Input v-model:value="row.meterCounter" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'meterCounter')" />
                          </div>
                        </template>
                        <template #grindingThickness="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="grindingThickness">
                            <Input v-model:value="row.grindingThickness" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'grindingThickness')" />
                          </div>
                        </template>
                        <template #afterGrindingThickness="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="afterGrindingThickness">
                            <Input v-model:value="row.afterGrindingThickness" size="small" :disabled="!isFirstGrindingRowEditable(row)" @update:value="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'afterGrindingThickness')" />
                          </div>
                        </template>
                        <template #selfCheck="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="selfCheck">
                            <Select v-model:value="row.selfCheck" size="small" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" :options="grindingSelfCheckOptions" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'selfCheck')" />
                          </div>
                        </template>
                        <template #defectCode="{ row }">
                          <div class="rough-excel-cell" :data-first-grinding-row-id="row.id" data-first-grinding-field="defectCode">
                            <Select v-model:value="row.defectCode" size="small" class="w-full" :disabled="!isFirstGrindingRowEditable(row)" :options="grindingDefectOptions" @change="() => handleMaterialUsageChange(row)" @keydown="handleFirstGrindingCellKeydown($event, row, 'defectCode')" />
                          </div>
                        </template>
                        <template #materialActions="{ row }">
                          <div class="pass-work-actions pass-work-actions--stack">
                            <Button size="small" type="link" danger :disabled="isRoughReadOnly() || isFirstGrindingRowReferenced(row.id)" @click="removeMaterialUsageRow(row.id)">
                              {{ isFirstGrindingRowReferenced(row.id) ? '已引用' : '移出' }}
                            </Button>
                          </div>
                        </template>
                      </MaterialUsageGrid>
                    </div>
                    <div class="rough-standards-bar">
                      <div class="rough-standards-bar__title">标准值</div>
                      <div class="rough-standards-bar__content">
                        <div v-for="item in firstGrindingStandards" :key="item.label" class="rough-standards-bar__item">
                          <span class="rough-standards-bar__label">{{ item.label }}</span>
                          <span class="rough-standards-bar__value">{{ item.value }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="abnormal-position" tab="异常位置" class="h-full">
              <div class="pass-work-panel">
                <div class="pp-panel">
                  <div class="pp-panel__header">
                    <div class="pp-panel__title">
                      <IconifyIcon icon="lucide:map-pin" class="mr-2 text-[#1677ff]" />
                      异常位置
                    </div>
                    <div class="pp-panel__desc">第一次磨皮固定损耗对应异常位置明细</div>
                  </div>
                  <div class="pp-table-wrap">
                    <table class="pp-grid">
                      <thead>
                        <tr>
                          <th width="72">序号</th>
                          <th width="160">来源计划</th>
                          <th width="180">母料批次</th>
                          <th>异常位置</th>
                          <th width="140">米数</th>
                          <th>备注</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, index) in roughAbnormalPositionRows" :key="`${row.sourceRowId || 'rough'}-${row.clientKey}`">
                          <td align="center">{{ index + 1 }}</td>
                          <td>{{ row.sourcePlanNo || '-' }}</td>
                          <td>{{ row.sourceBatchNo || '-' }}</td>
                          <td>{{ row.positionText || '-' }}</td>
                          <td align="right">{{ row.abnormalLength === undefined ? '-' : formatGrindingNumber(row.abnormalLength) }}</td>
                          <td>{{ row.remark || '-' }}</td>
                        </tr>
                        <tr v-if="!roughAbnormalPositionRows.length">
                          <td colspan="6" class="pp-empty-cell" align="center">暂无异常位置明细</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="second-grinding" tab="第二次磨皮" class="h-full">
              <div class="pass-work-panel rough-usage-tab">
                <div class="rough-process-workspace" :class="{ 'rough-process-workspace--maximized': materialUsageMaximized }">
                  <div class="rough-process-panel">
                    <div class="rough-grid-toolbar">
                      <div class="rough-grid-toolbar__title">第二次磨皮记录</div>
                      <div class="pass-work-actions rough-grid-toolbar__actions">
                        <Button size="small" @click="toggleMaterialUsageMaximized">
                          {{ materialUsageMaximized ? '还原布局' : '最大化' }}
                        </Button>
                        <Button size="small" :disabled="selectedSecondGrindingRows.length !== 1" @click="openSelectedSecondGrindingEditDialog">
                          修改参数
                        </Button>
                        <Button size="small" danger :disabled="!selectedSecondGrindingRows.length || isRoughReadOnly()" @click="removeSelectedSecondGrindingRows">
                          移出
                        </Button>
                        <Button size="small" :disabled="!secondGrindingRows.length" @click="openSelectedSecondGrindingConfirmDialog">
                          扫码确认
                        </Button>
                        <Button size="small" :disabled="!selectedSecondGrindingPrintRows.length" @click="printSelectedSecondGrindingRows">
                          选择打印
                        </Button>
                        <Button size="small" :disabled="!secondGrindingRows.length" @click="printAllSecondGrindingRows">
                          全部打印
                        </Button>
                        <Button size="small" type="primary" :disabled="isRoughReadOnly()" @click="openSecondGrindingDialog">报工</Button>
                      </div>
                    </div>
                    <div class="rough-grid-wrap rough-grid-wrap--auto">
                      <SecondGrindingGrid v-if="activeMainTab === 'second-grinding'" class="rough-grid-component">
                        <template #secondPrintSelect="{ row }">
                          <Checkbox
                            :checked="!!row.selectedForPrint"
                            @change="(event) => handleSecondGrindingSelectionChange(row, event)"
                          />
                        </template>
                        <template #secondSegmentMark="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="segmentMark">
                            <Select v-model:value="row.segmentMark" size="small" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :options="segmentMarkOptions" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'segmentMark')" />
                          </div>
                        </template>
                        <template #secondStartTime="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="startTime">
                            <DatePicker v-model:value="row.startTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'startTime')" />
                          </div>
                        </template>
                        <template #secondEndTime="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="endTime">
                            <DatePicker v-model:value="row.endTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'endTime')" />
                          </div>
                        </template>
                        <template #secondSandpaperLife="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="sandpaperLife">
                            <InputNumber v-model:value="row.sandpaperLife" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :min="0" :precision="3" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'sandpaperLife')" />
                          </div>
                        </template>
                        <template #secondSandpaperLifeDays="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="sandpaperLifeDays">
                            <InputNumber v-model:value="row.sandpaperLifeDays" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :min="0" :precision="0" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'sandpaperLifeDays')" />
                          </div>
                        </template>
                        <template #secondSandpaperBatchNo="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="sandpaperBatchNo">
                            <Input v-model:value="row.sandpaperBatchNo" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingSandpaperBatchInput(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'sandpaperBatchNo')">
                              <template #suffix>
                                <Tooltip title="选择磨皮砂纸边库批次">
                                  <Button
                                    class="rough-consumable-picker-button"
                                    size="small"
                                    type="text"
                                    :disabled="!isSecondGrindingRowEditable(row)"
                                    @click.stop="selectSecondGrindingSandpaper(row)"
                                  >
                                    <IconifyIcon icon="lucide:package-search" />
                                  </Button>
                                </Tooltip>
                              </template>
                            </Input>
                          </div>
                        </template>
                        <template #secondPressure="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="pressure">
                            <Input v-model:value="row.pressure" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'pressure')" />
                          </div>
                        </template>
                        <template #secondLineSpeed="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="lineSpeed">
                            <Input v-model:value="row.lineSpeed" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'lineSpeed')" />
                          </div>
                        </template>
                        <template #secondRotationSpeed="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="rotationSpeed">
                            <Input v-model:value="row.rotationSpeed" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'rotationSpeed')" />
                          </div>
                        </template>
                        <template #secondMeterCounter="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="meterCounter">
                            <Input v-model:value="row.meterCounter" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'meterCounter')" />
                          </div>
                        </template>
                        <template #secondGrindingThickness="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="grindingThickness">
                            <Input v-model:value="row.grindingThickness" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'grindingThickness')" />
                          </div>
                        </template>
                        <template #secondAfterGrindingThickness="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="afterGrindingThickness">
                            <Input v-model:value="row.afterGrindingThickness" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'afterGrindingThickness')" />
                          </div>
                        </template>
                        <template #secondSelfCheck="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="selfCheck">
                            <Select v-model:value="row.selfCheck" size="small" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :options="grindingSelfCheckOptions" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'selfCheck')" />
                          </div>
                        </template>
                        <template #secondDefectCode="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="defectCode">
                            <Select v-model:value="row.defectCode" size="small" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :options="grindingDefectOptions" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'defectCode')" />
                          </div>
                        </template>
                        <template #secondQualityThickness="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="qualityThickness">
                            <Input v-model:value="row.qualityThickness" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'qualityThickness')" />
                          </div>
                        </template>
                        <template #secondQualityWidth="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="qualityWidth">
                            <Input v-model:value="row.qualityWidth" size="small" :disabled="!isSecondGrindingRowEditable(row)" @update:value="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'qualityWidth')" />
                          </div>
                        </template>
                        <template #secondGrindingMeters="{ row }">
                          <div class="rough-excel-cell" :data-second-grinding-row-id="row.id" data-second-grinding-field="grindingMeters">
                            <InputNumber v-model:value="row.grindingMeters" class="w-full" :disabled="!isSecondGrindingRowEditable(row)" :min="0" :precision="3" @change="() => handleSecondGrindingRowChange(row)" @keydown="handleSecondGrindingCellKeydown($event, row, 'grindingMeters')" />
                          </div>
                        </template>
                        <template #secondPrintStatus="{ row }">
                          <Tag :color="row.printStatus === '已打印' ? 'success' : 'default'" class="!m-0">
                            {{ row.printStatus || '未打印' }}
                          </Tag>
                        </template>
                        <template #secondConfirmStatus="{ row }">
                          <Tag :color="row.confirmStatus === '已确认' ? 'success' : 'warning'" class="!m-0">
                            {{ row.confirmStatus || '未确认' }}
                          </Tag>
                        </template>
                      </SecondGrindingGrid>
                    </div>
                    <div class="rough-standards-bar">
                      <div class="rough-standards-bar__title">标准值</div>
                      <div class="rough-standards-bar__content">
                        <div v-for="item in secondGrindingStandards" :key="item.label" class="rough-standards-bar__item">
                          <span class="rough-standards-bar__label">{{ item.label }}</span>
                          <span class="rough-standards-bar__value">{{ item.value }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="semi-finished" tab="中间品记录单" class="h-full">
              <div class="pass-work-panel">
                <div class="pp-panel">
                  <div class="pp-panel__header">
                    <div class="pp-panel__title">
                      <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-[#1677ff]" />
                      中间品记录单
                    </div>
                    <div class="pp-panel__desc">CMP软垫磨皮中间品记录表按中间品长度自动生成记录行</div>
                  </div>
                  <div class="pp-table-wrap pass-work-list-wrap">
                    <table class="pp-grid">
                      <thead>
                        <tr>
                          <th width="260">表单名称</th>
                          <th width="130">执行时机</th>
                          <th width="140">中间品长度</th>
                          <th width="120">单据状态</th>
                          <th width="180">记录人/时间</th>
                          <th width="180">确认人/确认时间</th>
                          <th width="180">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="record in semiFinishedRows" :key="record.id">
                          <td>{{ record.name || '-' }}</td>
                          <td align="center">{{ record.timing || '-' }}</td>
                          <td align="center">{{ record.generatedLength ? `${record.generatedLength} m` : '未生成' }}</td>
                          <td align="center">
                            <Tag :color="getStatusMeta(record.status).color" class="!m-0">{{ getStatusMeta(record.status).text }}</Tag>
                          </td>
                          <td>
                            <div>{{ record.recorder || '-' }}</div>
                            <div class="pp-grid__sub">{{ normalizeDateTime(record.recorderTime) || '-' }}</div>
                          </td>
                          <td>
                            <div>{{ record.confirmer || '-' }}</div>
                            <div class="pp-grid__sub">{{ normalizeDateTime(record.confirmerTime) || '-' }}</div>
                          </td>
                          <td align="center">
                            <div class="pass-work-actions">
                              <Button size="small" type="link" :disabled="record.status === 'COMPLETED'" @click="openRecord(record, 'semi-finished', 'edit')">
                                填写
                              </Button>
                              <Button
                                size="small"
                                type="link"
                                :disabled="record.status === 'COMPLETED'"
                                @click="openRecord(record, 'semi-finished', 'confirm')"
                              >
                                确认
                              </Button>
                              <Button size="small" type="link" @click="openRecord(record, 'semi-finished', 'view')">查看</Button>
                            </div>
                          </td>
                        </tr>
                        <tr v-if="!semiFinishedRows.length">
                          <td colspan="7" class="pp-empty-cell" align="center">暂无过站工作数据</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="first-inspection" tab="首样检验单" class="h-full">
              <div class="pass-work-panel">
                <div class="pp-panel">
                  <div class="pp-panel__header">
                    <div class="pp-panel__title">
                      <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-[#1677ff]" />
                      首样检验单
                    </div>
                    <div class="pp-panel__desc">提交首检申请后模拟发送 QMS，并等待首检完成结果回传</div>
                  </div>
                  <div class="pp-table-wrap pass-work-list-wrap">
                    <table class="pp-grid">
                      <thead>
                        <tr>
                          <th width="220">表单名称</th>
                          <th width="140">单据状态</th>
                          <th>处理结果</th>
                          <th width="140">检验人</th>
                          <th width="180">检验时间</th>
                          <th width="260">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>首样检验单</td>
                          <td align="center">
                            <Tag :color="getStatusMeta(firstInspection.status).color" class="!m-0">{{ getStatusMeta(firstInspection.status).text }}</Tag>
                          </td>
                          <td>
                            {{
                              firstInspection.status === 'PENDING'
                                ? '点击提交首检申请后，将模拟安灯通知并发送至 QMS。'
                                : firstInspection.result || '-'
                            }}
                          </td>
                          <td align="center">{{ firstInspection.inspector || '-' }}</td>
                          <td align="center">{{ firstInspection.inspectTime || '-' }}</td>
                          <td align="center">
                            <div class="pass-work-actions inspection-actions">
                              <Button size="small" type="link" :disabled="firstInspection.status !== 'PENDING'" @click="submitFirstInspection">
                                提交首检申请
                              </Button>
                              <Button size="small" type="link" :disabled="firstInspection.status === 'PENDING'" @click="handleFirstInspectionPrintClick">
                                打印检验单
                              </Button>
                              <Button size="small" type="link" :disabled="firstInspection.status !== 'COMPLETED'" @click="viewFirstInspectionDetail">
                                查看详情
                              </Button>
                            </div>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </TabPane>

            <TabPane key="report-info" tab="报工汇总" class="h-full">
              <div class="pass-work-panel">
                <div class="pp-panel">
                <div class="pp-panel__header">
                  <div class="pp-panel__title">
                    <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-[#1677ff]" />
                    报工汇总
                  </div>
                  <div class="pp-panel__desc">一次/二次加工汇总与当前工序报工信息回显</div>
                </div>
                <div class="wet-report-booking-grid wet-report-summary-grid">
                  <div class="pp-form-item">
                    <label>计划单号</label>
                    <div class="pp-readonly-box pp-readonly-box--code">{{ task?.planNo || task?.id || '-' }}</div>
                  </div>
                  <div class="pp-form-item">
                    <label>生产型号</label>
                    <div class="pp-readonly-box">{{ task?.modelCode || '-' }}</div>
                  </div>
                  <div class="pp-form-item">
                    <label>生产料号</label>
                    <div class="pp-readonly-box">{{ task?.materialCode || task?.product || '-' }}</div>
                  </div>
                  <div class="pp-form-item">
                    <label>母料批号</label>
                    <div class="pp-readonly-box">{{ task?.batchNo || '-' }}</div>
                  </div>
                  <div class="pp-form-item pp-form-item--span-2">
                    <label>一次加工汇总</label>
                    <div class="pp-readonly-box pp-readonly-box--multiline">
                      加工米数：{{ firstGrindingAggregate.processLength }} m　加工损耗：{{ firstGrindingAggregate.lossLength }} m　产出米数：{{ firstGrindingAggregate.outputLength }} m　NAP留样米数：{{ firstGrindingAggregate.napSampleLength }} m
                      <br />
                      最后一次报工砂纸批号：{{ firstGrindingAggregate.lastSandpaperBatchNo }}　最后一次报工砂纸累计寿命：{{ firstGrindingAggregate.lastSandpaperLife ?? '-' }} m　最后一次报工累计使用天：{{ firstGrindingAggregate.lastSandpaperLifeDays ?? '-' }}
                    </div>
                  </div>
                  <div class="pp-form-item pp-form-item--span-2">
                    <label>二次加工汇总</label>
                    <div class="pp-readonly-box pp-readonly-box--multiline">
                      加工米数：{{ secondGrindingAggregate.processLength }} m　加工损耗：{{ secondGrindingAggregate.lossLength }} m　产出米数：{{ secondGrindingAggregate.outputLength }} m　NAP留样米数：{{ secondGrindingAggregate.napSampleLength }} m
                      <br />
                      最后一次报工砂纸批号：{{ secondGrindingAggregate.lastSandpaperBatchNo }}　最后一次报工砂纸累计寿命：{{ secondGrindingAggregate.lastSandpaperLife ?? '-' }} m　最后一次报工累计使用天：{{ secondGrindingAggregate.lastSandpaperLifeDays ?? '-' }}
                    </div>
                  </div>
                  <div class="pp-form-item pp-form-item--span-2">
                    <label>报工量</label>
                    <div class="pp-readonly-box">{{ bookingReportQty }} m</div>
                  </div>
                  <div class="pp-form-item pp-form-item--span-2">
                    <label>导布批号</label>
                    <div class="pp-readonly-box pp-readonly-box--code">{{ reportForm.guideClothBatchNo || '-' }}</div>
                  </div>
                  <div class="pp-form-item pp-form-item--span-2">
                    <label>报工备注</label>
                    <div class="pp-readonly-box pp-readonly-box--multiline">{{ reportForm.remark || '-' }}</div>
                  </div>
                  <div class="pp-form-item">
                      <label>记录人</label>
                      <div class="pp-readonly-box">{{ reportForm.recorderName || '-' }}</div>
                    </div>
                    <div class="pp-form-item">
                      <label>记录时间</label>
                      <div class="pp-readonly-box">{{ reportView.recorderTimeDisplay || '-' }}</div>
                    </div>
                    <div class="pp-form-item">
                      <label>确认人</label>
                      <div class="pp-readonly-box">{{ reportForm.confirmerName || '-' }}</div>
                    </div>
                    <div class="pp-form-item">
                      <label>确认时间</label>
                      <div class="pp-readonly-box">{{ reportView.confirmerTimeDisplay || '-' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </div>

      <div v-if="viewMode === 'booking'" class="pp-plan-body">
        <fieldset class="pp-fieldset">
          <legend>报工汇总</legend>
          <div class="wet-report-booking-grid">
            <div class="pp-form-item">
              <label>计划单号</label>
              <div class="pp-readonly-box pp-readonly-box--code">{{ task?.planNo || task?.id || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>生产型号</label>
              <div class="pp-readonly-box">{{ task?.modelCode || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>生产料号</label>
              <div class="pp-readonly-box">{{ task?.materialCode || task?.product || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>母料批号</label>
              <div class="pp-readonly-box">{{ task?.batchNo || '-' }}</div>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>一次加工汇总</label>
              <div class="pp-readonly-box pp-readonly-box--multiline">
                加工米数：{{ firstGrindingAggregate.processLength }} m　加工损耗：{{ firstGrindingAggregate.lossLength }} m　产出米数：{{ firstGrindingAggregate.outputLength }} m　NAP留样米数：{{ firstGrindingAggregate.napSampleLength }} m
                <br />
                最后一次报工砂纸批号：{{ firstGrindingAggregate.lastSandpaperBatchNo }}　最后一次报工砂纸累计寿命：{{ firstGrindingAggregate.lastSandpaperLife ?? '-' }} m　最后一次报工累计使用天：{{ firstGrindingAggregate.lastSandpaperLifeDays ?? '-' }}
              </div>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>二次加工汇总</label>
              <div class="pp-readonly-box pp-readonly-box--multiline">
                加工米数：{{ secondGrindingAggregate.processLength }} m　加工损耗：{{ secondGrindingAggregate.lossLength }} m　产出米数：{{ secondGrindingAggregate.outputLength }} m　NAP留样米数：{{ secondGrindingAggregate.napSampleLength }} m
                <br />
                最后一次报工砂纸批号：{{ secondGrindingAggregate.lastSandpaperBatchNo }}　最后一次报工砂纸累计寿命：{{ secondGrindingAggregate.lastSandpaperLife ?? '-' }} m　最后一次报工累计使用天：{{ secondGrindingAggregate.lastSandpaperLifeDays ?? '-' }}
              </div>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>二次加工米(报工量)</label>
              <div class="pp-readonly-box">{{ bookingReportQty }} m</div>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>导布批号</label>
              <Input
                v-model:value="reportForm.guideClothBatchNo"
                @update:value="clearReportGuideClothSelection"
              >
                <template #suffix>
                  <Tooltip title="选择磨皮导布边库批次">
                    <Button
                      class="rough-consumable-picker-button"
                      size="small"
                      type="text"
                      @click.stop="selectReportGuideCloth"
                    >
                      <IconifyIcon icon="lucide:package-search" />
                    </Button>
                  </Tooltip>
                </template>
              </Input>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>报工备注</label>
              <Input v-model:value="reportForm.remark" />
            </div>
            <div class="pp-form-item">
              <label>记录人</label>
              <Input v-model:value="reportForm.recorderName" />
            </div>
            <div class="pp-form-item">
              <label>记录时间</label>
              <DatePicker v-model:value="reportForm.recorderTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
            </div>
            <div class="pp-form-item">
              <label>确认人</label>
              <Input v-model:value="reportForm.confirmerName" />
            </div>
            <div class="pp-form-item">
              <label>确认时间</label>
              <DatePicker v-model:value="reportForm.confirmerTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
            </div>
          </div>
        </fieldset>

        <div class="pp-panel pp-panel--booking-actions">
          <div class="pp-booking-panel__actions">
            <Button size="small" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回过程作业
            </Button>
            <Button size="small" type="primary" danger @click="handleBookingConfirm">
              <IconifyIcon icon="lucide:check" class="mr-1" /> 确认提交报工
            </Button>
          </div>
        </div>
      </div>
    </div>

    <AModal v-model:open="materialScanVisible" title="第一次磨皮报工登记" width="100vw" :footer="null" wrap-class-name="hc-rough-report-entry-modal" @cancel="materialScanVisible = false">
      <div class="rough-material-scan-modal">
        <Tabs v-model:activeKey="materialScanActiveTab" class="rough-entry-tabs">
          <TabPane key="report" tab="报工信息">
        <div class="rough-material-scan-row">
          <div class="pp-form-item rough-material-scan-row__field">
            <label>母料批号（扫码/输入）</label>
            <Input v-model:value="materialScanForm.code" placeholder="扫描母料批号，或输入边库母料批号" />
          </div>
          <Button size="small" type="primary" @click="simulateMaterialScan">模拟扫码/带出</Button>
        </div>
        <div class="rough-material-scan-hint">{{ materialSourceHintText }}</div>
        <div class="pp-form-grid rough-material-scan-grid">
          <div class="pp-form-item">
            <label>母料批次</label>
            <Input v-model:value="materialScanForm.batchNo" />
          </div>
          <div class="pp-form-item">
            <label>计划号</label>
            <Input v-model:value="materialScanForm.planNo" />
          </div>
          <div class="pp-form-item">
            <label>加工米数(m)</label>
            <InputNumber v-model:value="materialScanForm.processLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>加工损耗米数(m)</label>
            <InputNumber v-model:value="materialScanForm.lossLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>异常位置</label>
            <div class="rough-abnormal-summary">
              <Button size="small" @click="openMaterialScanAbnormalTab">
                <IconifyIcon icon="lucide:map-pin" class="mr-1" />
                维护异常位置
              </Button>
              <span>{{ formatRoughAbnormalPositionSummary(materialScanAbnormalRows) }}</span>
            </div>
          </div>
          <div class="pp-form-item">
            <label>产出米数(m)</label>
            <InputNumber v-model:value="materialScanForm.outputLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>NAP留样米数(m)</label>
            <InputNumber v-model:value="materialScanForm.napSampleLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>砂纸累计使用天</label>
            <InputNumber v-model:value="materialScanForm.sandpaperLifeDays" class="w-full" :min="0" :precision="0" />
          </div>
          <div class="pp-form-item">
            <label>砂纸批号</label>
            <Input
              v-model:value="materialScanForm.sandpaperBatchNo"
              @update:value="handleMaterialScanSandpaperBatchInput"
            >
              <template #suffix>
                <Tooltip title="选择磨皮砂纸边库批次">
                  <Button
                    class="rough-consumable-picker-button"
                    size="small"
                    type="text"
                    @click.stop="selectMaterialScanSandpaper"
                  >
                    <IconifyIcon icon="lucide:package-search" />
                  </Button>
                </Tooltip>
              </template>
            </Input>
          </div>
          <div class="pp-form-item">
            <label>开始时间</label>
            <DatePicker v-model:value="materialScanForm.startTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
          </div>
          <div class="pp-form-item">
            <label>结束时间</label>
            <DatePicker v-model:value="materialScanForm.endTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
          </div>
          <div class="pp-form-item">
            <label>自检</label>
            <div class="choice-field">
              <RadioGroup v-model:value="materialScanForm.selfCheck" size="small" class="pp-radio-group">
                <Radio value="OK">OK</Radio>
                <Radio value="NG">NG</Radio>
              </RadioGroup>
            </div>
          </div>
          <div class="pp-form-item">
            <label>不良代码</label>
            <Select v-model:value="materialScanForm.defectCode" :options="grindingDefectOptions" class="w-full" />
          </div>
        </div>
          </TabPane>

          <TabPane key="abnormal-position" tab="异常位置">
            <fieldset class="pp-fieldset">
              <legend>异常位置</legend>
              <div class="abnormal-position-dialog">
                <div class="abnormal-position-toolbar">
                  <span>计划号：{{ materialScanForm.planNo || '-' }}　母料批次：{{ materialScanForm.batchNo || '-' }}</span>
                  <Button size="small" type="primary" @click="addMaterialScanAbnormalRow">
                    <IconifyIcon icon="lucide:plus" class="mr-1" />
                    新增
                  </Button>
                </div>
                <table class="pp-grid abnormal-position-edit-table">
                  <thead>
                    <tr>
                      <th width="64">序号</th>
                      <th>异常位置</th>
                      <th width="160">米数</th>
                      <th>备注</th>
                      <th width="80">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in materialScanAbnormalRows" :key="row.clientKey">
                      <td align="center">{{ index + 1 }}</td>
                      <td>
                        <Input v-model:value="row.positionText" placeholder="请输入异常位置" />
                      </td>
                      <td>
                        <InputNumber v-model:value="row.abnormalLength" :min="0" :precision="3" class="w-full" />
                      </td>
                      <td>
                        <Input v-model:value="row.remark" placeholder="备注" />
                      </td>
                      <td align="center">
                        <Button danger size="small" type="link" @click="removeMaterialScanAbnormalRow(index)">删除</Button>
                      </td>
                    </tr>
                    <tr v-if="!materialScanAbnormalRows.length">
                      <td colspan="5" class="pp-empty-cell" align="center">暂无异常位置明细</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </fieldset>
          </TabPane>

          <TabPane v-for="category in materialScanCheckCategories" :key="`check-${category}`" :tab="category">
            <fieldset class="pp-fieldset">
              <legend>{{ category }}</legend>
              <div class="pp-table-wrap adhesive-check-detail-wrap">
                <table class="pp-grid adhesive-check-grid">
                  <colgroup>
                    <col style="width: 14%" />
                    <col style="width: 24%" />
                    <col style="width: 20%" />
                    <col style="width: 18%" />
                    <col style="width: 24%" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th>物料/生产环节</th>
                      <th>工艺参数项目</th>
                      <th>标准</th>
                      <th>实测值</th>
                      <th>异常备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in getMaterialScanCheckItemsByCategory(category)" :key="`${category}-${item.sortNo}-${item.itemName}`">
                      <td>{{ item.itemCategory || '-' }}</td>
                      <td>{{ item.itemName || '-' }}</td>
                      <td>{{ item.standardValue || '-' }}</td>
                      <td>
                        <Input v-model:value="item.actualValue" size="small" />
                      </td>
                      <td>
                        <Input v-model:value="item.abnormalRemark" size="small" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </fieldset>
          </TabPane>
          <TabPane v-if="materialScanCheckCategories.length === 0" key="check-empty" tab="点检明细">
            <fieldset class="pp-fieldset">
              <legend>点检明细</legend>
              <div class="pp-empty-cell">未加载到磨皮点检模板，请检查 CMP软垫（W26P0100）磨皮点检表配置。</div>
            </fieldset>
          </TabPane>
        </Tabs>
        <div class="rough-modal-footer">
          <Button @click="confirmAppendMaterialRow(true)">继续增加</Button>
          <Button type="primary" @click="confirmAppendMaterialRow()">确认</Button>
          <Button @click="materialScanVisible = false">取消</Button>
        </div>
      </div>
    </AModal>

    <AModal v-model:open="secondGrindingVisible" :title="secondGrindingDialogTitle" width="100vw" :footer="null" wrap-class-name="hc-rough-report-entry-modal" @cancel="secondGrindingVisible = false">
      <div class="rough-material-scan-modal">
        <Tabs v-model:activeKey="secondGrindingActiveTab" class="rough-entry-tabs">
          <TabPane key="report" tab="报工信息">
        <div class="rough-material-scan-row">
          <div class="pp-form-item rough-material-scan-row__field">
            <label>来源母料批号（扫码/输入）</label>
            <Input v-model:value="secondGrindingForm.sourceCode" placeholder="扫描来源母料批号，或输入第一次磨皮母料批号" />
          </div>
          <Button size="small" type="primary" @click="simulateSecondGrindingSource">模拟扫码/带出</Button>
        </div>
        <div class="rough-material-scan-hint">{{ secondGrindingSourceHintText }}</div>
        <div class="pp-form-grid rough-material-scan-grid">
          <div class="pp-form-item pp-form-item--span-2">
            <label>来源母料批号（第一次磨皮）</label>
            <Select
              v-model:value="secondGrindingForm.sourceRowId"
              :options="firstGrindingSourceOptions"
              placeholder="请选择第一次磨皮已追加记录"
              class="w-full"
              @change="syncSecondGrindingSource"
            />
          </div>
          <div class="pp-form-item">
            <label>母批来源</label>
            <div class="pp-readonly-box">{{ secondGrindingForm.sourceType || '-' }}</div>
          </div>
          <div class="pp-form-item">
            <label>母料批号</label>
            <Input v-model:value="secondGrindingForm.batchNo" disabled />
          </div>
          <div class="pp-form-item">
            <label>批次号</label>
            <Input v-model:value="secondGrindingForm.productionBatchNo" disabled />
          </div>
          <div class="pp-form-item">
            <label>加工米数(m)</label>
            <InputNumber v-model:value="secondGrindingForm.processLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>加工损耗米数(m)</label>
            <InputNumber v-model:value="secondGrindingForm.lossLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>产出米数(m)</label>
            <InputNumber v-model:value="secondGrindingForm.outputLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label class="report-required-label">NAP留样米数(m)</label>
            <InputNumber v-model:value="secondGrindingForm.napSampleLength" class="w-full" :min="0" :precision="3" />
          </div>
          <div class="pp-form-item">
            <label>砂纸累计使用天</label>
            <InputNumber v-model:value="secondGrindingForm.sandpaperLifeDays" class="w-full" :min="0" :precision="0" />
          </div>
          <div class="pp-form-item">
            <label>砂纸批号</label>
            <Input
              v-model:value="secondGrindingForm.sandpaperBatchNo"
              @update:value="handleSecondGrindingFormSandpaperBatchInput"
            >
              <template #suffix>
                <Tooltip title="选择磨皮砂纸边库批次">
                  <Button
                    class="rough-consumable-picker-button"
                    size="small"
                    type="text"
                    @click.stop="selectSecondGrindingFormSandpaper"
                  >
                    <IconifyIcon icon="lucide:package-search" />
                  </Button>
                </Tooltip>
              </template>
            </Input>
          </div>
          <div class="pp-form-item">
            <label>分段标记</label>
            <Select
              v-model:value="secondGrindingForm.segmentMark"
              :options="segmentMarkOptions"
              class="w-full"
              @change="handleSecondGrindingFormSegmentChange"
            />
          </div>
          <div class="pp-form-item">
            <label>开始时间</label>
            <DatePicker v-model:value="secondGrindingForm.startTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
          </div>
          <div class="pp-form-item">
            <label>结束时间</label>
            <DatePicker v-model:value="secondGrindingForm.endTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
          </div>
          <div class="pp-form-item">
            <label>自检</label>
            <div class="choice-field">
              <RadioGroup v-model:value="secondGrindingForm.selfCheck" size="small" class="pp-radio-group">
                <Radio value="OK">OK</Radio>
                <Radio value="NG">NG</Radio>
              </RadioGroup>
            </div>
          </div>
          <div class="pp-form-item">
            <label>不良代码</label>
            <Select v-model:value="secondGrindingForm.defectCode" :options="grindingDefectOptions" class="w-full" />
          </div>
        </div>
          </TabPane>

          <TabPane v-for="category in secondGrindingCheckCategories" :key="`check-${category}`" :tab="category">
            <fieldset class="pp-fieldset">
              <legend>{{ category }}</legend>
              <div class="pp-table-wrap adhesive-check-detail-wrap">
                <table class="pp-grid adhesive-check-grid">
                  <colgroup>
                    <col style="width: 14%" />
                    <col style="width: 24%" />
                    <col style="width: 20%" />
                    <col style="width: 18%" />
                    <col style="width: 24%" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th>物料/生产环节</th>
                      <th>工艺参数项目</th>
                      <th>标准</th>
                      <th>实测值</th>
                      <th>异常备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in getSecondGrindingCheckItemsByCategory(category)" :key="`${category}-${item.sortNo}-${item.itemName}`">
                      <td>{{ item.itemCategory || '-' }}</td>
                      <td>{{ item.itemName || '-' }}</td>
                      <td>{{ item.standardValue || '-' }}</td>
                      <td>
                        <Input v-model:value="item.actualValue" size="small" />
                      </td>
                      <td>
                        <Input v-model:value="item.abnormalRemark" size="small" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </fieldset>
          </TabPane>
          <TabPane v-if="secondGrindingCheckCategories.length === 0" key="check-empty" tab="点检明细">
            <fieldset class="pp-fieldset">
              <legend>点检明细</legend>
              <div class="pp-empty-cell">未加载到磨皮点检模板，请检查 CMP软垫（W26P0100）磨皮点检表配置。</div>
            </fieldset>
          </TabPane>
        </Tabs>
        <div class="rough-modal-footer">
          <Button v-if="!secondGrindingEditRowId" @click="confirmAppendSecondGrindingRow(true)">继续增加</Button>
          <Button type="primary" @click="confirmAppendSecondGrindingRow()">
            {{ secondGrindingEditRowId ? '确认修改' : '确认' }}
          </Button>
          <Button @click="secondGrindingVisible = false">取消</Button>
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="secondGrindingConfirmVisible"
      title="第二次磨皮扫码确认"
      :footer="null"
    >
      <div class="rough-scan-confirm-dialog">
        <div class="rough-scan-confirm-dialog__hero">
          <IconifyIcon icon="lucide:scan-line" class="rough-scan-confirm-dialog__icon" />
          <div>
            <div class="rough-scan-confirm-dialog__title">请使用扫码枪扫描分段流转单</div>
            <div class="rough-scan-confirm-dialog__desc">请将打印流转单贴纸分段实物中，核对检查无误。</div>
          </div>
        </div>
        <div class="rough-scan-confirm-dialog__batch">
          当前分段批次：{{ currentSecondGrindingConfirmRow?.productionBatchNo || '-' }}
        </div>
        <Input
          v-model:value="secondGrindingConfirmForm.scanCode"
          placeholder="扫描或输入分段生产批次号"
          @press-enter="confirmSecondGrindingScan"
        />
        <div v-if="secondGrindingConfirmForm.error" class="rough-scan-confirm-dialog__error">
          {{ secondGrindingConfirmForm.error }}
        </div>
        <div v-if="secondGrindingConfirmForm.message" class="rough-scan-confirm-dialog__message">
          {{ secondGrindingConfirmForm.message }}
        </div>
        <div class="rough-modal-footer">
          <Button @click="secondGrindingConfirmVisible = false">取消</Button>
          <Button type="primary" @click="confirmSecondGrindingScan">确认入账</Button>
        </div>
      </div>
    </AModal>

    <AModal v-model:open="recordDialogVisible" :title="null" width="100vw" :footer="null" wrap-class-name="hc-pass-work-modal">
      <div class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title pass-work-dialog-title">
            <span class="pp-plan-toolbar__main">{{ recordDialogTitle }}</span>
            <div class="toolbar-meta">
              <span>计划号：{{ task?.planNo || '-' }}</span>
              <span>当前工序：{{ task?.process || '磨皮' }}</span>
              <span>执行时机：{{ currentRecord?.timing || '-' }}</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <Button v-if="recordDialogMode === 'edit'" size="small" type="primary" @click="saveCurrentRecord">保存</Button>
            <Button v-if="recordDialogMode === 'confirm'" size="small" type="primary" danger @click="openRecordConfirmAuth">确认</Button>
            <Button size="small" @click="closeRecordDialog">关闭</Button>
          </div>
        </div>

        <div class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div v-if="recordDialogCategory === 'production-check'" class="production-check-head-grid">
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">生产料号：</span>
                <span class="production-check-head-cell__value">{{ task?.materialCode || '-' }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">生产型号：</span>
                <span class="production-check-head-cell__value">{{ task?.modelCode || '-' }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">生产批号：</span>
                <span class="production-check-head-cell__value">{{ task?.batchNo || '-' }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">生产日期：</span>
                <span class="production-check-head-cell__value">{{ reportView.productionDateDisplay || '-' }}</span>
              </div>

              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">机台编号：</span>
                <span class="production-check-head-cell__value">{{ currentMachineCode }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">记录人：</span>
                <span class="production-check-head-cell__value">{{ currentRecord?.recorder || reportForm.recorderName || '-' }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">记录时间：</span>
                <span class="production-check-head-cell__value">{{ normalizeDateTime(currentRecord?.recorderTime) || reportView.recorderTimeDisplay || '-' }}</span>
              </div>
              <div class="production-check-head-cell">
                <span class="production-check-head-cell__label">确认人：</span>
                <span class="production-check-head-cell__value">{{ currentRecord?.confirmer || reportForm.confirmerName || '-' }}</span>
              </div>
            </div>
            <div v-else-if="recordDialogCategory === 'semi-finished' && currentRecord" class="production-check-head-grid">
              <div v-for="field in semiFinishedHeadFields" :key="field.key" class="production-check-head-cell">
                <span class="production-check-head-cell__label">{{ field.label }}</span>
                <div
                  v-if="field.type === 'input-number' && recordDialogMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <InputNumber
                    v-model:value="currentRecord.generatedLength"
                    class="production-check-head-cell__input"
                    :min="2"
                    :precision="0"
                    @change="(value) => handleSemiLengthChange((value as number | undefined) ?? undefined)"
                  />
                </div>
                <div
                  v-else-if="field.type === 'input' && recordDialogMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <Input v-model:value="currentRecord.semiWidth" class="production-check-head-cell__input" />
                </div>
                <div
                  v-else-if="field.type === 'radio' && recordDialogMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <div class="choice-field production-check-head-cell__choice">
                    <RadioGroup
                      v-if="field.key === 'poreDevelopment'"
                      v-model:value="currentRecord.poreDevelopment"
                      size="small"
                      class="pp-radio-group"
                    >
                      <Radio value="OK">OK</Radio>
                      <Radio value="NG">NG</Radio>
                    </RadioGroup>
                    <RadioGroup
                      v-else
                      v-model:value="currentRecord.finalResult"
                      size="small"
                      class="pp-radio-group"
                    >
                      <Radio value="OK">OK</Radio>
                      <Radio value="NG">NG</Radio>
                    </RadioGroup>
                  </div>
                </div>
                <span v-else class="production-check-head-cell__value">
                  {{
                    field.key === 'generatedLength'
                      ? (currentRecord?.generatedLength ?? '-')
                      : field.key === 'semiWidth'
                        ? (currentRecord?.semiWidth || '-')
                        : field.key === 'poreDevelopment'
                          ? (currentRecord?.poreDevelopment || '-')
                          : field.key === 'finalResult'
                            ? (currentRecord?.finalResult || '-')
                            : (field.value || '-')
                  }}
                </span>
              </div>
            </div>
            <div v-else class="pp-form-grid pass-work-form-grid">
              <div v-for="field in recordDialogTopFields" :key="field.field" class="head-item">
                <span class="head-item__label">{{ field.label }}</span>
                <span class="head-item__value">{{ field.value || '-' }}</span>
              </div>
            </div>
          </fieldset>

          <div class="pp-panel pass-work-detail-panel">
            <div class="pp-panel__header">
              <span>明细项目</span>
            </div>
            <div class="pp-table-wrap pass-work-detail-wrap">
              <div class="pass-work-detail-table-body">
              <div v-if="recordDialogCategory !== 'semi-finished' && recordDialogCategory !== 'production-check'" class="daily-check-result-tip">
                开机、清洁保养时遇到问题请记录在备注列说明情况。
              </div>
              <table class="pp-grid">
                <thead>
                  <tr v-if="recordDialogCategory === 'semi-finished'">
                    <th width="100">长度/m</th>
                    <th width="180">磨皮左侧10cm厚度/mm</th>
                    <th width="180">磨皮右侧10cm厚度/mm</th>
                    <th width="180">复测左侧10cm厚度/mm</th>
                    <th width="180">复测右侧10cm厚度/mm</th>
                    <th width="260">备注</th>
                  </tr>
                  <tr v-else-if="recordDialogCategory === 'production-check'">
                    <th width="120">物料/生产环节</th>
                    <th width="120">确认节点</th>
                    <th width="220">点检项目</th>
                    <th width="180">点检标准</th>
                    <th width="180">实际/记录</th>
                    <th width="220">异常备注</th>
                  </tr>
                  <tr v-else-if="currentRecord?.name?.includes('清洁点检表')">
                    <th width="120">工序</th>
                    <th width="220">点检项目</th>
                    <th width="420">检查标准</th>
                    <th width="120">OK/NG</th>
                    <th width="260">备注</th>
                  </tr>
                  <tr v-else>
                    <th width="90">序号</th>
                    <th width="260">点检项目</th>
                    <th width="360">标准</th>
                    <th width="120">OK/NG</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in recordDialogDetailRows"
                    :key="`${currentRecord?.id}-${row.seq}`"
                  >
                    <template v-if="recordDialogCategory === 'production-check'">
                      <td v-if="getDetailFieldRowSpan(currentRecord?.details || [], index, 'category') > 0" :rowspan="getDetailFieldRowSpan(currentRecord?.details || [], index, 'category')">
                        {{ row.category || '-' }}
                      </td>
                      <td v-if="getDetailFieldRowSpan(currentRecord?.details || [], index, 'node') > 0" :rowspan="getDetailFieldRowSpan(currentRecord?.details || [], index, 'node')">
                        {{ row.node || '-' }}
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.item" size="small" :placeholder="row.placeholder ? '请填写点检项目' : ''" />
                        <span v-else>{{ row.item || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.standard" size="small" :placeholder="row.placeholder ? '请填写点检标准' : ''" />
                        <span v-else>{{ row.standard || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.value" size="small" :placeholder="row.placeholder ? '请填写记录值' : ''" />
                        <span v-else>{{ row.value || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.remark" size="small" />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </template>
                    <template v-else-if="currentRecord?.name?.includes('清洁点检表')">
                      <td v-if="getDetailFieldRowSpan(currentRecord?.details || [], index, 'category') > 0" :rowspan="getDetailFieldRowSpan(currentRecord?.details || [], index, 'category')">
                        {{ row.category || '-' }}
                      </td>
                      <td>{{ row.item || '-' }}</td>
                      <td>{{ row.standard || '-' }}</td>
                      <td>
                        <RadioGroup v-if="recordDialogMode !== 'view'" v-model:value="row.result" class="pp-radio-group" size="small">
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ row.result || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.remark" size="small" />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </template>
                    <template v-else>
                    <template v-if="recordDialogCategory === 'semi-finished'">
                      <td align="center">{{ row.length }}</td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.item" size="small" />
                        <span v-else>{{ row.item || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.standard" size="small" />
                        <span v-else>{{ row.standard || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.value" size="small" />
                        <span v-else>{{ row.value || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.node" size="small" />
                        <span v-else>{{ row.node || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.remark" size="small" />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </template>
                    <template v-else>
                      <td align="center">{{ row.seq }}</td>
                      <td>{{ row.item }}</td>
                      <td>{{ row.standard }}</td>
                      <td>
                        <RadioGroup v-if="recordDialogMode !== 'view'" v-model:value="row.result" class="pp-radio-group" size="small">
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ row.result || '-' }}</span>
                      </td>
                      <td>
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="row.remark" size="small" />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </template>
                    </template>
                  </tr>
                  <tr v-if="!recordDialogDetailRows.length">
                    <td :colspan="recordDialogCategory === 'semi-finished' || recordDialogCategory === 'production-check' ? 6 : 5" class="pp-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
              </div>
              <div
                v-if="recordDialogCategory === 'semi-finished' && (currentRecord?.details?.length || 0) > SEMI_DETAIL_PAGE_SIZE"
                class="semi-detail-pagination"
              >
                <Pagination
                  v-model:current="semiDetailPage"
                  :page-size="SEMI_DETAIL_PAGE_SIZE"
                  :show-size-changer="false"
                  :total="currentRecord?.details?.length || 0"
                  size="small"
                />
              </div>
            </div>
          </div>

          <fieldset class="pp-fieldset pp-fieldset--plain">
            <Form layout="vertical" class="detail-form__body">
              <div class="form-section">
                <div class="form-section__title form-section__title--toggle" @click="actionPanelExpanded = !actionPanelExpanded">
                  <span>执行与验证记录</span>
                  <IconifyIcon :icon="actionPanelExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
                </div>
                <div v-show="actionPanelExpanded" class="form-section-grid">
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">执行记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="执行结果" class="form-grid__inline">
                        <div v-if="recordDialogMode !== 'view'" class="choice-field">
                          <RadioGroup v-model:value="recordActionForm.result" size="small" class="pp-radio-group">
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">{{ recordActionForm.result || '-' }}</div>
                      </FormItem>
                      <FormItem label="记录人">
                        <div class="head-item__value">{{ recordActionForm.recorder || reportForm.recorderName || '-' }}</div>
                      </FormItem>
                      <FormItem label="记录时间">
                        <div class="head-item__value">{{ recordActionForm.recorderTime || reportView.recorderTimeDisplay || '-' }}</div>
                      </FormItem>
                      <FormItem label="执行备注">
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="recordActionForm.formRemark" size="small" />
                        <div v-else class="head-item__value">{{ recordActionForm.formRemark || '-' }}</div>
                      </FormItem>
                    </div>
                  </div>
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">验证记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="检验结果" class="form-grid__inline">
                        <div v-if="recordDialogMode !== 'view'" class="choice-field">
                          <RadioGroup v-model:value="recordActionForm.inspectionResult" size="small" class="pp-radio-group">
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">{{ recordActionForm.inspectionResult || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认人">
                        <div class="head-item__value">{{ recordActionForm.confirmer || reportForm.confirmerName || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认时间">
                        <div class="head-item__value">{{ recordActionForm.confirmerTime || reportView.confirmerTimeDisplay || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认备注">
                        <Input v-if="recordDialogMode !== 'view'" v-model:value="recordActionForm.confirmRemark" size="small" />
                        <div v-else class="head-item__value">{{ recordActionForm.confirmRemark || '-' }}</div>
                      </FormItem>
                    </div>
                  </div>
                </div>
              </div>
            </Form>
          </fieldset>
        </div>
      </div>
    </AModal>

    <ConsumableLedgerSwitchModal
      ref="roughConsumableSwitchModalRef"
      @selected="handleRoughConsumableSelected"
    />


    <AuthModal
      v-model:visible="authVisible"
      :actionName="pendingAction === 'START' ? '磨皮工单开工确认' : '磨皮节点完工与报工'"
      authMode="username"
      :equipment-id="pendingAction === 'START' ? reportForm.equipmentId : undefined"
      :equipment-options="pendingAction === 'START' ? equipmentOptions : []"
      equipment-label="机台编号"
      @success="handleAuthSuccess"
    />
    <AuthModal
      v-model:visible="recordConfirmAuthVisible"
      :action-name="recordConfirmAuthActionName"
      auth-mode="username"
      @success="handleRecordConfirmAuthSuccess"
    />
    <PrintPreviewModal />
    <FirstInspectionPrintPreviewModal />
    <AModal
      v-model:open="deviceSwitchVisible"
      title="切换工位设备"
      ok-text="确认切换"
      cancel-text="取消"
      :confirm-loading="deviceSwitchSubmitting"
      @ok="handleDeviceSwitchConfirm"
    >
      <div class="device-switch-dialog">
        <div class="device-switch-dialog__hint">
          <IconifyIcon icon="lucide:cable" class="device-switch-dialog__hint-icon" />
          <span>当前挂接设备信息</span>
        </div>
        <div v-if="equipmentStatusCard" class="device-switch-current-grid device-switch-current-grid--single">
          <div class="device-switch-current-card">
            <div class="device-switch-current-card__head">
              <span class="device-switch-current-card__title">当前挂接设备</span>
              <Tag :color="equipmentStatusCard.statusMeta.color" class="!m-0">{{ equipmentStatusCard.statusMeta.text }}</Tag>
            </div>
            <div class="device-switch-current-card__name">{{ equipmentStatusCard.equipmentLabel }}</div>
            <div class="device-switch-current-card__meta">计划号：{{ equipmentStatusCard.currentPlanNo }}</div>
            <div class="device-switch-current-card__meta">工序：{{ equipmentStatusCard.currentOperationName }}</div>
            <div class="device-switch-current-card__meta">开工时间：{{ equipmentStatusCard.currentStartTime }}</div>
            <div class="device-switch-current-card__meta">完工时间：{{ equipmentStatusCard.currentEndTime }}</div>
            <div class="device-switch-current-card__meta">操作人：{{ equipmentStatusCard.currentOperatorName }}</div>
          </div>
        </div>
        <div class="pp-form-grid pp-form-grid--switch">
          <div class="pp-form-item">
            <label>机台编号</label>
            <Select v-model:value="deviceSwitchForm.equipmentId" :options="equipmentOptions" :field-names="{ label: 'label', value: 'value' }" />
          </div>
        </div>
      </div>
    </AModal>
  </Modal>
</template>

<style scoped>
.rough-consumable-picker-button {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
}

:deep(.custom-main-tabs .ant-tabs-nav) {
  padding: 0 12px;
  margin-bottom: 0;
  background-color: #ffffff;
  border-bottom: 1px solid #edf0f3;
}

:deep(.custom-main-tabs .ant-tabs-tab) {
  font-size: 13px;
  font-weight: 600;
  padding: 10px 12px !important;
}

:deep(.custom-main-tabs .ant-tabs-content-holder) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

:deep(.custom-main-tabs .ant-tabs-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.custom-main-tabs .ant-tabs-tabpane) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.pp-plan-modal {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.pp-plan-toolbar__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.pp-plan-toolbar__sub {
  color: #6b7280;
  font-size: 12px;
}

.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-switch-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.device-switch-dialog__hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.device-switch-dialog__hint-icon {
  font-size: 16px;
}

.device-switch-current-grid {
  display: grid;
  gap: 12px;
}

.device-switch-current-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.device-switch-current-card {
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.device-switch-current-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.device-switch-current-card__title {
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.device-switch-current-card__name {
  margin-bottom: 4px;
  color: #111827;
  font-weight: 600;
}

.device-switch-current-card__meta {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.pp-plan-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.wet-prev-op-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
}

.wet-prev-op-bar__title {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.wet-prev-op-bar__content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.wet-prev-op-bar__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.wet-prev-op-bar__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.wet-prev-op-bar__value {
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
}

.pp-fieldset {
  margin: 0;
  padding: 8px 12px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

.pp-fieldset--plain {
  padding-top: 10px;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-grid--switch {
  grid-template-columns: minmax(0, 1fr);
}

.wet-report-info-grid,
.wet-report-booking-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.wet-report-info-grid {
  padding: 12px;
}

.wet-report-booking-grid {
  padding: 0;
}

.wet-report-summary-grid {
  padding: 12px;
}

.pp-form-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pp-form-item--full {
  grid-column: 1 / -1;
}

.pp-form-item--span-2 {
  grid-column: span 2;
}

.pp-form-item label {
  color: #4b5563;
  font-weight: 700;
}

.pp-readonly-box {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #374151;
  line-height: 1.4;
}

.pp-readonly-box--code {
  color: #1677ff;
  font-family: Consolas, Monaco, monospace;
}

.pp-readonly-box--multiline {
  align-items: flex-start;
  white-space: pre-wrap;
}

.pp-empty-state {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: #6b7280;
  text-align: center;
}

.pp-empty-state__title {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.pp-empty-state__desc {
  max-width: 560px;
  font-size: 12px;
  line-height: 1.7;
}

.pp-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  background: #fff;
  overflow: hidden;
}

.pp-panel--booking-actions {
  flex: 0 0 auto;
  min-height: auto;
}

.pp-booking-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #e5e7eb;
}

.wet-tabs-shell {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.rough-usage-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  min-height: 0;
  height: 100%;
  box-sizing: border-box;
}

.rough-usage-body--split {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
  height: 100%;
  box-sizing: border-box;
}

.rough-usage-body--maximized {
  position: fixed;
  top: 48px;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 30;
  background: #f5f7fa;
  padding: 8px;
}

.rough-usage-fieldset {
  min-height: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  margin: 0;
  box-sizing: border-box;
  background: transparent;
  overflow: hidden;
}

.rough-usage-fieldset--top {
  min-height: 0;
}

.rough-usage-fieldset--bottom {
  min-height: 0;
}

.rough-toolbar-row {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rough-toolbar-row__hint {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.rough-grid-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  margin-bottom: 0;
  padding: 0 10px 0 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-bottom: 0;
  box-sizing: border-box;
}

.rough-grid-toolbar__title {
  flex-shrink: 0;
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
}

.rough-grid-toolbar__actions {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.rough-usage-tab {
  padding: 0;
}

.rough-process-workspace {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.rough-process-workspace--maximized {
  position: fixed;
  top: 48px;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 30;
  background: #f5f7fa;
  padding: 8px;
}

.rough-process-panel {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.rough-standards-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-top: 0;
  background: #fff;
}

.rough-standards-bar__title {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
}

.rough-standards-bar__content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.rough-standards-bar__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.rough-standards-bar__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.rough-standards-bar__value {
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
}

.rough-material-scan-modal {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.rough-entry-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.rough-entry-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-top: 0;
}

.rough-entry-tabs :deep(.ant-tabs-content),
.rough-entry-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.rough-entry-tabs :deep(.ant-tabs-tabpane) {
  padding: 12px;
}

.rough-material-scan-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.rough-material-scan-row__field {
  flex: 1;
  min-width: 0;
}

.rough-material-scan-row__pass {
  width: 220px;
  flex-shrink: 0;
}

.rough-material-scan-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px 12px;
}

.rough-form-standard {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.rough-material-scan-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.rough-abnormal-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  color: #64748b;
  font-size: 12px;
}

.abnormal-position-dialog {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.abnormal-position-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #4b5563;
  font-size: 13px;
}

.abnormal-position-edit-table :deep(.ant-input-number),
.abnormal-position-edit-table :deep(.ant-input) {
  width: 100%;
}

.rough-modal-footer {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  gap: 8px;
  padding: 2px 0 12px;
}

.rough-scan-confirm-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rough-scan-confirm-dialog__hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
}

.rough-scan-confirm-dialog__icon {
  color: #1677ff;
  font-size: 34px;
}

.rough-scan-confirm-dialog__title {
  color: #111827;
  font-weight: 700;
}

.rough-scan-confirm-dialog__desc,
.rough-scan-confirm-dialog__batch {
  color: #4b5563;
  font-size: 13px;
}

.rough-scan-confirm-dialog__error {
  color: #ff4d4f;
  font-size: 13px;
  font-weight: 700;
}

.rough-scan-confirm-dialog__message {
  color: #16a34a;
  font-size: 13px;
  font-weight: 700;
}

.rough-grid-wrap :deep(.rough-second-row--printed .vxe-body--column) {
  background-color: #f6ffed !important;
}

.rough-grid-wrap :deep(.rough-second-row--confirmed .vxe-body--column) {
  background-color: #e6f4ff !important;
}

.pp-plan-toolbar--sub {
  padding-left: 0;
  padding-right: 0;
  border-bottom: 1px solid #e5e7eb;
}

.semi-detail-pagination {
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding: 8px 2px 0;
  background: #fff;
}

.rough-grid-wrap {
  min-height: 0;
  overflow: hidden;
  background: #fff;
  width: 100%;
}

.rough-grid-wrap--auto {
  height: auto;
}



.rough-grid-surface {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  height: 100%;
  width: 100%;
}

.rough-grid-surface--auto {
  grid-template-rows: auto minmax(0, 1fr);
}

.rough-grid-component {
  height: 100%;
  min-height: 0;
}

.rough-grid-wrap :deep(.vben-grid) {
  height: 100%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.rough-grid-wrap--auto :deep(.vben-grid),
.rough-grid-wrap--auto :deep(.vxe-grid),
.rough-grid-wrap--auto :deep(.vxe-grid--table-wrapper),
.rough-grid-wrap--auto :deep(.vxe-table),
.rough-grid-wrap--auto :deep(.vxe-table--main-wrapper),
.rough-grid-wrap--auto :deep(.vxe-table--render-wrapper) {
  height: auto !important;
  min-height: 0;
}

.rough-grid-wrap :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.rough-grid-wrap :deep(.vxe-grid--table-wrapper),
.rough-grid-wrap :deep(.vxe-table),
.rough-grid-wrap :deep(.vxe-table--main-wrapper),
.rough-grid-wrap :deep(.vxe-table--render-wrapper) {
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
}

.rough-grid-wrap :deep(.vxe-grid .vxe-grid--body-wrapper),
.rough-grid-wrap :deep(.vxe-grid--body-wrapper),
.rough-grid-wrap :deep(.vxe-table--body-wrapper) {
  overflow: auto !important;
}

.rough-grid-wrap :deep(.vxe-grid .vxe-grid--body-wrapper) {
  flex: 1;
  min-height: 0;
}


.rough-grid-wrap :deep(.vxe-table--header-wrapper),
.rough-grid-wrap :deep(.vxe-grid--header-wrapper) {
  flex-shrink: 0;
}

.rough-grid-wrap :deep(.vxe-header--column),
.rough-grid-wrap :deep(.vxe-body--column) {
  white-space: nowrap;
}

.rough-readonly-cell {
  min-height: 24px;
  padding: 0 4px;
  color: #334155;
  font-variant-numeric: tabular-nums;
  line-height: 24px;
  text-align: right;
}

.rough-excel-cell {
  width: 100%;
  min-width: 0;
}

.rough-excel-cell :deep(.ant-input),
.rough-excel-cell :deep(.ant-input-number),
.rough-excel-cell :deep(.ant-picker),
.rough-excel-cell :deep(.ant-select-selector) {
  border-radius: 0;
}

.rough-excel-cell:focus-within :deep(.ant-input),
.rough-excel-cell:focus-within :deep(.ant-input-number),
.rough-excel-cell:focus-within :deep(.ant-picker),
.rough-excel-cell:focus-within :deep(.ant-select-selector) {
  border-color: #1677ff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.16);
}

.pass-work-actions--stack {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.pass-work-panel {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f5f7fa;
  padding: 8px;
}

.pp-panel__header {
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-weight: 700;
}

.pp-panel__title {
  display: flex;
  align-items: center;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.pp-panel__desc {
  font-size: 12px;
  color: #6b7280;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  border: 1px solid #e5e7eb;
  vertical-align: middle;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  font-weight: 400;
  text-align: center;
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-grid td[rowspan] {
  vertical-align: middle;
}

.pp-grid__sub {
  color: #9ca3af;
  font-size: 12px;
}

.pp-empty-cell {
  color: #9ca3af;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #4b5563;
}

.pass-work-list-wrap,
.pass-work-detail-wrap {
  height: 100%;
}

.pass-work-detail-wrap {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.daily-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.pass-work-form-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.production-check-head-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #e5e7eb;
  border-bottom: 0;
  border-right: 0;
}

.production-check-head-cell {
  min-width: 0;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.production-check-head-cell__label {
  flex-shrink: 0;
  width: 116px;
  align-self: stretch;
  display: flex;
  align-items: center;
  padding: 0 8px;
  border-right: 1px solid #e5e7eb;
  background: #f5f5f5;
  color: #111827;
  font-weight: 400;
}

.production-check-head-cell__value {
  min-width: 0;
  flex: 1;
  padding: 0 10px;
  color: #111827;
  line-height: 1.4;
}

.production-check-head-cell__picker {
  flex: 1;
  min-width: 0;
  padding: 0 8px;
}

.production-check-head-cell__input-wrap {
  padding: 0 8px;
}

.production-check-head-cell__input {
  width: 100%;
}

.production-check-head-cell__choice {
  width: 100%;
  min-height: 30px;
  border: 1px solid #d9d9d9;
  background: #fff;
}

.production-check-head-cell__picker :deep(.ant-picker) {
  width: 100%;
  min-height: 30px !important;
  height: 30px !important;
  border: 1px solid #d9d9d9 !important;
  box-shadow: none !important;
  padding: 0 8px !important;
  border-radius: 0 !important;
  background: #fff !important;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
.production-check-head-cell__input-wrap :deep(.ant-input-number),
.production-check-head-cell__input-wrap :deep(.ant-input-number-input-wrap),
.production-check-head-cell__input-wrap :deep(.ant-input-number-input) {
  width: 100%;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
.production-check-head-cell__input-wrap :deep(.ant-input-number) {
  min-height: 30px !important;
  height: 30px !important;
  border: 1px solid #d9d9d9 !important;
  box-shadow: none !important;
  border-radius: 0 !important;
}

.production-check-head-cell__input-wrap :deep(.ant-input) {
  padding: 0 8px !important;
}

.head-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.head-item__label {
  color: #4b5563;
  font-weight: 700;
}

.head-item__value {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
}

.pass-work-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.inspection-actions {
  width: 100%;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  gap: 2px;
}

.inspection-actions :deep(.ant-btn-link) {
  height: 24px;
  padding: 0;
}

.pass-work-dialog-title {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.detail-form__body {
  padding: 8px 10px 0;
}

.form-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-section {
  min-width: 0;
  padding: 0 0 2px;
}

.form-section--inner {
  padding: 0;
}

.form-section__title {
  margin-bottom: 10px;
  padding: 0 0 6px;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.form-section__title--toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.form-section__subtitle {
  margin-bottom: 10px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.report-required-label {
  color: #dc2626;
  font-weight: 900;
}

.form-grid {
  display: grid;
  gap: 8px 12px;
}

.form-grid--section {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-form__body :deep(.ant-form-item) {
  margin-bottom: 8px;
  min-width: 0;
}

.detail-form__body :deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: #4b5563;
  font-weight: 700;
}

.detail-form__body :deep(.ant-form-item-control-input) {
  min-height: 32px;
}

.detail-form__body :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

.detail-form__body :deep(.ant-input),
.detail-form__body :deep(.ant-input-affix-wrapper),
.detail-form__body :deep(.ant-picker),
.detail-form__body :deep(.ant-select-selector) {
  min-height: 32px !important;
  height: 32px !important;
}

.detail-form__body :deep(.ant-input) {
  line-height: 30px;
}

.pp-radio-group {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  min-height: 32px;
  padding: 0 6px;
}

.choice-field {
  min-height: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 5px;
  border: 1px solid #d9d9d9;
  background: #fff;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 0 !important;
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  margin: 0 !important;
  padding-bottom: 0;
  max-width: none;
}

.hc-pass-work-modal [class*='modal__header'],
.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal [class*='modal__close'],
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal [class*='modal__body'],
.hc-pass-work-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal [class*='modal__content'],
.hc-pass-work-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.hc-rough-report-entry-modal .ant-modal {
  top: 18px;
  width: calc(100vw - 48px) !important;
  max-width: none;
  margin: 0 auto;
  padding-bottom: 0;
}

.hc-rough-report-entry-modal .ant-modal-content {
  height: calc(100vh - 36px) !important;
  border-radius: 2px !important;
  overflow: hidden;
}

.hc-rough-report-entry-modal .ant-modal-header {
  min-height: 44px;
  margin: 0;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.hc-rough-report-entry-modal .ant-modal-body {
  height: calc(100vh - 80px) !important;
  padding: 14px 16px !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-rough-report-entry-modal .ant-modal-close {
  top: 8px;
}
</style>
