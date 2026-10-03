<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';
import { computed, onMounted, reactive, ref } from 'vue';

import dayjs from 'dayjs';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Button,
  Checkbox,
  Input,
  message,
  Modal,
  Select,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createNgManualPiece,
  exportNgManualPieceImportTemplate,
  getNgLocationGrid,
  getNgInventorySegmentPage,
  getNgInventorySegmentPieceList,
  getNgPieceLabels,
  getNgPiecePage,
  importNgManualPieces,
  markNgPieceLabelsPrinted,
  scrapNgPieces,
  unfreezeNgManualPiece,
} from '#/api/mes/hc/ng-inventory';
import { buildSlittingTransferTicketPayload } from '../execution/report/shared/slittingTransferTicketPrint';
import { padTypeNameOf } from '../base/pad-type-options';

import '../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesNgInventory' });

type NgPiece = MesHcNgInventoryApi.NgPiece;

type NgPieceLabel = MesHcNgInventoryApi.NgPieceLabel;

type NgPieceSegment = MesHcNgInventoryApi.NgPieceSegment;

type NgLocation = MesHcNgInventoryApi.NgLocationGrid;

type NgPieceDisplayRow = Partial<NgPiece> & {
  __group?: boolean;
  children?: NgPieceDisplayRow[];
  groupCount?: number;
  hasChild?: boolean;
  samplePieceNos?: string[];
  rowKey: string;
};

const stockViewMode = ref<'flat' | 'group'>('group');

const fixedWarehouse = ref('NG_SLITTING');

const fixedWarehouses = [
  { code: 'NG_SLITTING', name: '分切库' },
  { code: 'NG_PRESS_SLOT', name: '压槽库' },
  { code: 'NG_FREEZE', name: '冻结库' },
];

const padTypeFilter = ref<string>();

const keyword = ref('');

const locations = ref<NgLocation[]>([]);

const selectedPiece = ref<NgPiece | null>(null);

const selectedStockPieces = ref<NgPiece[]>([]);

const selectedPrintPieces = ref<NgPiece[]>([]);

const loadingLocations = ref(false);

const actionLoading = ref(false);

const printSelecting = ref(false);

const printSubmitting = ref(false);

const PRINT_AGENT_URL = 'http://127.0.0.1:17820';

const PRINT_BATCH_MAX_COUNT = 50;

const scrapVisible = ref(false);

const scrapReason = ref('');

const scrapTargets = ref<NgPiece[]>([]);

const manualPieceVisible = ref(false);

const manualPieceSubmitting = ref(false);

const manualPieceImportVisible = ref(false);

const manualPieceImporting = ref(false);

const manualPieceTemplateExporting = ref(false);

const manualPieceImportInputRef = ref<HTMLInputElement>();

const manualUnfreezeVisible = ref(false);

const manualUnfreezeSubmitting = ref(false);

const manualUnfreezeTarget = ref<NgPiece>();

const manualUnfreezeReason = ref('');

const manualPieceForm = reactive<MesHcNgInventoryApi.NgManualPieceCreateReq>({
  backfillReason: '',
  defectSummary: '',
  materialCode: '',
  modelNo: '',
  padType: 'BLACK_PAD',
  pieceNo: '',
  processType: 'SLITTING',
  remark: '',
  segmentBatchNo: '',
  sourceBatchNo: '',
  storageTarget: 'NORMAL',
});

const isManualFrozenStockSelection = computed(
  () =>
    selectedStockPieces.value.length > 0 &&
    selectedStockPieces.value.every((piece) => isManualFrozenPiece(piece)),
);

const activeViewMode = computed(() => stockViewMode.value);

function ngStatusText(status?: string) {
  return (
    {
      FROZEN: '已冻结',
      WAIT_FREEZE_SHELF: '待上架冻结品',
      SCRAPPED: '已报废',
      STORED: '在库',
      WAIT_SHELF: '待上架',
    }[status || ''] ||
    status ||
    '-'
  );
}

function ngStatusColor(status?: string) {
  return (
    {
      FROZEN: 'red',
      WAIT_FREEZE_SHELF: 'purple',
      SCRAPPED: 'default',
      STORED: 'green',
      WAIT_SHELF: 'gold',
    }[status || ''] || 'default'
  );
}

function getPageNo(page?: { currentPage?: number; pageNo?: number }) {
  return Number(page?.currentPage || page?.pageNo || 1);
}

function getPageSize(page?: { pageSize?: number }) {
  return Number(page?.pageSize || 20);
}

async function fetchLocations() {
  loadingLocations.value = true;
  try {
    locations.value = await getNgLocationGrid();
  } finally {
    loadingLocations.value = false;
  }
}

async function refreshAll() {
  clearPrintSelection();
  await Promise.all([fetchLocations(), stockGridApi.query()]);
}

function selectPiece(row: NgPiece) {
  selectedPiece.value = row;
}

function isGroupRow(row: NgPieceDisplayRow) {
  return Boolean(row.__group);
}

function toSegmentDisplayRow(
  row: NgPieceSegment,
  frozen: boolean,
  index: number,
): NgPieceDisplayRow {
  const segmentBatchNo = row.segmentBatchNo || '-';
  const groupKey = [
    frozen ? 'freeze' : 'wait',
    row.processType || '-',
    row.sourcePlanOperationId || '-',
    segmentBatchNo,
    frozen ? row.freezeInstructionId || '-' : '',
  ].join('|');
  return {
    __group: true,
    freezeInstructionId: row.freezeInstructionId,
    freezeInstructionNo: row.freezeInstructionNo,
    groupCount: Number(row.totalPieceCount || 0),
    hasChild: true,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelNo: row.modelNo,
    pieceNo: segmentBatchNo,
    processName: row.processName,
    processType: row.processType,
    qualityResult:
      Number(row.okPieceCount || 0) > 0 && Number(row.ngPieceCount || 0) > 0
        ? 'MIXED'
        : Number(row.okPieceCount || 0) > 0
          ? 'OK'
          : 'NG',
    samplePieceNos: row.samplePieceNos || [],
    sourcePlanId: row.sourcePlanId,
    sourcePlanNo: row.sourcePlanNo,
    sourcePlanOperationId: row.sourcePlanOperationId,
    sourceParentBatchNo: segmentBatchNo,
    rowKey: `${groupKey}|${index}`,
  };
}

function toStockSegmentRow(
  row: NgPieceSegment,
  index: number,
): NgPieceDisplayRow {
  const segmentBatchNo = row.segmentBatchNo || '-';
  return {
    ...toSegmentDisplayRow(row, false, index),
    sourceParentBatchNo: segmentBatchNo,
    rowKey: `stock-segment-${fixedWarehouse.value}-${padTypeFilter.value || 'ALL'}-${row.processType || '-'}-${row.sourcePlanOperationId || '-'}-${segmentBatchNo}-${index}`,
  };
}

function toStockPieceRow(row: NgPiece): NgPieceDisplayRow {
  return {
    ...row,
    rowKey: `stock-piece-${fixedWarehouse.value}-${row.id}`,
  };
}

function formatGroupSamples(row: NgPieceDisplayRow) {
  const samples = (row.samplePieceNos || []).filter(Boolean);
  return samples.length > 0
    ? `共 ${row.groupCount || 0} 片，示例：${samples.join('、')}`
    : `共 ${row.groupCount || 0} 片，展开加载片号`;
}

function handleStockCheckboxChange(records: NgPieceDisplayRow[]) {
  const pieces = records.filter(
    (row) =>
      !isGroupRow(row) &&
      Boolean(row.id) &&
      (row.status === 'STORED' || isManualFrozenPiece(row)),
  ) as NgPiece[];
  selectedStockPieces.value = pieces;
  selectedPiece.value = pieces[0] || null;
}

function isPrintPieceSelected(piece?: Pick<NgPiece, 'id'>) {
  return Boolean(
    piece?.id && selectedPrintPieces.value.some((item) => item.id === piece.id),
  );
}

function setPrintPieceSelected(piece: NgPiece, selected: boolean) {
  if (!piece.id) return;
  if (selected) {
    if (!isPrintPieceSelected(piece)) {
      selectedPrintPieces.value = [...selectedPrintPieces.value, piece];
    }
    return;
  }
  selectedPrintPieces.value = selectedPrintPieces.value.filter(
    (item) => item.id !== piece.id,
  );
}

function handlePrintPieceCheckbox(
  piece: NgPiece,
  event: { target?: { checked?: boolean } },
) {
  setPrintPieceSelected(piece, Boolean(event.target?.checked));
}

function clearPrintSelection() {
  selectedPrintPieces.value = [];
}

async function selectPrintSegment(
  row: NgPieceDisplayRow,
  listType: 'freeze' | 'stock' | 'wait',
) {
  if (!isGroupRow(row)) return;
  printSelecting.value = true;
  try {
    const rows = await loadInventorySegmentChildren(row);
    const selection = new Map(
      selectedPrintPieces.value.map((item) => [item.id, item]),
    );
    rows.forEach((item) => {
      if (item.id) selection.set(item.id, item as NgPiece);
    });
    selectedPrintPieces.value = [...selection.values()];
    message.success(`已选择该段 ${rows.length} 个片号用于打印`);
  } catch (error: any) {
    message.error(error?.message || '加载段内片号失败，请刷新后重试');
  } finally {
    printSelecting.value = false;
  }
}

function ngPiecePrintTicketId(label: NgPieceLabel) {
  return `ng-piece-${label.pieceId}`;
}

async function sendNgPieceLabelsToAgent(labels: NgPieceLabel[]) {
  const now = dayjs().format('YYYY-MM-DD HH:mm:ss');
  const payload = await buildSlittingTransferTicketPayload(
    labels.map((label) => ({
      hideMaterialCodeWhenEmpty: true,
      materialCode: label.materialCode,
      modelCode: label.modelCode,
      planNo: label.planNo,
      processName: label.processName,
      recorderName: label.recorderName,
      segmentBatchNo: label.segmentBatchNo,
      sliceSerialNo: label.pieceNo,
      ticketId: ngPiecePrintTicketId(label),
      workTime: label.workTime ? String(label.workTime).replace('T', ' ') : now,
    })),
    now,
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
  return result;
}

async function executeNgPiecePrint(labels: NgPieceLabel[]) {
  printSubmitting.value = true;
  let acceptedCount = 0;
  const acceptedPieceIds = new Set<number>();
  const failures: string[] = [];
  try {
    for (let start = 0; start < labels.length; start += PRINT_BATCH_MAX_COUNT) {
      const batch = labels.slice(start, start + PRINT_BATCH_MAX_COUNT);
      const result = await sendNgPieceLabelsToAgent(batch);
      const itemResults = Array.isArray(result?.itemResults)
        ? result.itemResults
        : [];
      const acceptedLabels = batch.filter((label) => {
        const itemResult = itemResults.find(
          (item: any) => item?.ticketId === ngPiecePrintTicketId(label),
        );
        return itemResult
          ? itemResult.success === true && itemResult.accepted !== false
          : result?.success === true;
      });
      itemResults
        .filter(
          (item: any) => item?.success !== true || item?.accepted === false,
        )
        .forEach((item: any) =>
          failures.push(
            item?.message || `标签 ${item?.ticketId || '-'} 未进入打印队列`,
          ),
        );
      if (acceptedLabels.length === 0) continue;
      await markNgPieceLabelsPrinted(
        acceptedLabels.map((label) => ({
          labelContentJson: JSON.stringify(label),
          pieceId: label.pieceId,
          printerName: 'slitting',
        })),
      );
      acceptedLabels.forEach((label) => acceptedPieceIds.add(label.pieceId));
      acceptedCount += acceptedLabels.length;
    }
    if (acceptedCount === 0) {
      throw new Error(failures[0] || '本机打印服务未确认任何标签进入打印队列');
    }
    selectedPrintPieces.value = selectedPrintPieces.value.filter(
      (piece) => !acceptedPieceIds.has(piece.id),
    );
    const failedCount = labels.length - acceptedCount;
    if (failedCount > 0 || failures.length > 0) {
      Modal.warning({
        content: `已打印并完成审计 ${acceptedCount} 张；另有 ${failedCount} 张未完成。${failures[0] || ''}`,
        title: '不合格品标签批量打印部分完成',
      });
    } else {
      message.success(`已打印并完成审计 ${acceptedCount} 张不合格品标签`);
    }
    return true;
  } catch (error: any) {
    Modal.warning({
      content:
        acceptedCount > 0
          ? `已有 ${acceptedCount} 张标签完成打印及审计；${error?.message || error}`
          : `未能完成不合格品标签打印：${error?.message || error}。请确认 HC-MES-PrintAgent 已启动且分切标签打印机配置正确。`,
      title:
        acceptedCount > 0
          ? '不合格品标签批量打印部分完成'
          : '不合格品标签打印失败',
    });
    return false;
  } finally {
    printSubmitting.value = false;
  }
}

async function openNgPiecePrint() {
  const pieces = [...selectedPrintPieces.value];
  if (pieces.length === 0) {
    message.warning('请先在三个列表中选择需要打印的片号');
    return;
  }
  printSelecting.value = true;
  const labels: NgPieceLabel[] = [];
  const failures: string[] = [];
  try {
    for (let start = 0; start < pieces.length; start += PRINT_BATCH_MAX_COUNT) {
      const result = await getNgPieceLabels(
        pieces
          .slice(start, start + PRINT_BATCH_MAX_COUNT)
          .map((piece) => piece.id),
      );
      labels.push(...(result.labels || []));
      failures.push(...(result.failures || []));
    }
    if (labels.length === 0) {
      message.warning(
        failures[0] || '未找到可打印的不合格品标签数据，请刷新后重试',
      );
      return;
    }
    if (failures.length > 0 || labels.length !== pieces.length) {
      message.warning(
        `已加载 ${labels.length} 张标签；${failures[0] || '部分选择项已变化，未加入打印'}`,
      );
    }
    await executeNgPiecePrint(labels);
  } catch (error: any) {
    message.error(error?.message || '加载不合格品标签失败，请稍后重试');
  } finally {
    printSelecting.value = false;
  }
}

function resolveStockOperationTargets(row?: NgPiece) {
  const targets = row?.id ? [row] : selectedStockPieces.value;
  if (targets.length === 0) {
    message.warning('请先勾选普通已上架不合格品');
    return [];
  }
  if (targets.some((piece) => piece.status !== 'STORED')) {
    message.warning('冻结片不能直接报废，请先解冻');
    return [];
  }
  return targets;
}

function isStockSelectionCompatible(row: NgPieceDisplayRow) {
  if (isGroupRow(row) || !row.id) return false;
  const candidate = row as NgPiece;
  const isNormalStoredPiece = candidate.status === 'STORED';
  const candidateIsManualFrozen = isManualFrozenPiece(candidate);
  if (!isNormalStoredPiece && !candidateIsManualFrozen) return false;
  const selected = selectedStockPieces.value;
  return (
    selected.length === 0 ||
    selected.some((piece) => piece.id === candidate.id) ||
    selected.every((piece) =>
      candidateIsManualFrozen
        ? isManualFrozenPiece(piece)
        : piece.status === 'STORED',
    )
  );
}

function openScrap(row?: NgPiece) {
  const targets = resolveStockOperationTargets(row);
  if (targets.length === 0) return;
  scrapTargets.value = targets;
  scrapReason.value = '';
  scrapVisible.value = true;
}

function isManualFrozenPiece(piece?: NgPiece) {
  return Boolean(isManualHistoryPiece(piece) && piece?.status === 'FROZEN');
}

async function submitScrap() {
  if (scrapTargets.value.length === 0) return;
  if (!scrapReason.value.trim()) {
    message.warning('请填写报废原因');
    return;
  }
  actionLoading.value = true;
  try {
    await scrapNgPieces({
      pieceIds: scrapTargets.value.map((piece) => piece.id),
      scrapReason: scrapReason.value.trim(),
    });
    message.success(
      `已报废 ${scrapTargets.value.length} 片，已从当前不合格品库存隐藏`,
    );
    scrapVisible.value = false;
    selectedPiece.value = null;
    selectedStockPieces.value = [];

    await refreshAll();
  } finally {
    actionLoading.value = false;
  }
}

function resetManualPieceForm() {
  Object.assign(manualPieceForm, {
    backfillReason: '',
    defectSummary: '',
    materialCode: '',
    modelNo: '',
    padType: 'BLACK_PAD',
    pieceNo: '',
    processType: 'SLITTING',
    remark: '',
    segmentBatchNo: '',
    sourceBatchNo: '',
    storageTarget: 'NORMAL',
  } satisfies MesHcNgInventoryApi.NgManualPieceCreateReq);
}

function openManualPieceModal() {
  resetManualPieceForm();
  manualPieceVisible.value = true;
}

function validateManualPieceForm() {
  const requiredFields: Array<[string, string]> = [
    [manualPieceForm.segmentBatchNo, '段批次'],
    [manualPieceForm.pieceNo, '片号'],
    [manualPieceForm.sourceBatchNo, '来源批号'],
    [manualPieceForm.modelNo, '型号'],
    [manualPieceForm.defectSummary, 'NG原因'],
  ];
  requiredFields.push([manualPieceForm.backfillReason, '补录原因']);
  const missing = requiredFields.find(([value]) => !value.trim());
  if (missing) {
    message.warning(`请填写${missing[1]}`);
    return false;
  }
  return true;
}

async function submitManualPiece() {
  if (!validateManualPieceForm()) return;
  manualPieceSubmitting.value = true;
  try {
    const commonData = {
      defectSummary: manualPieceForm.defectSummary.trim(),
      materialCode:
        manualPieceForm.materialCode?.trim().toUpperCase() || undefined,
      modelNo: manualPieceForm.modelNo.trim().toUpperCase(),
      padType: manualPieceForm.padType,
      pieceNo: manualPieceForm.pieceNo.trim().toUpperCase(),
      processType: manualPieceForm.processType,
      segmentBatchNo: manualPieceForm.segmentBatchNo.trim().toUpperCase(),
      sourceBatchNo: manualPieceForm.sourceBatchNo.trim().toUpperCase(),
      storageTarget: manualPieceForm.storageTarget,
    };
    await createNgManualPiece({
      ...commonData,
      backfillReason: manualPieceForm.backfillReason.trim(),
      remark: manualPieceForm.remark?.trim() || undefined,
    });
    manualPieceVisible.value = false;
    fixedWarehouse.value =
      manualPieceForm.storageTarget === 'FREEZE'
        ? 'NG_FREEZE'
        : manualPieceForm.processType === 'SLITTING'
          ? 'NG_SLITTING'
          : 'NG_PRESS_SLOT';
    message.success('历史不合格品已新增并自动入库');
    await refreshAll();
  } finally {
    manualPieceSubmitting.value = false;
  }
}

function isManualHistoryPiece(piece?: NgPiece) {
  return piece?.sourceType === 'MANUAL_HISTORY';
}

async function downloadManualPieceImportTemplate() {
  manualPieceTemplateExporting.value = true;
  try {
    const data = await exportNgManualPieceImportTemplate();
    downloadFileFromBlobPart({
      fileName: '分切压槽不合格品历史片导入模板.xlsx',
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
  result: MesHcNgInventoryApi.NgManualPieceImportResult,
) {
  Modal.warning({
    content: `读取 ${result.totalRows || 0} 行，跳过 ${result.skippedRows || 0} 行，失败 ${result.failureCount || 0} 条。\n\n${(result.failures || []).join('\n')}`,
    title: '历史不合格品导入校验未通过',
    width: 780,
  });
}

async function importManualPieceFile(file: File) {
  manualPieceImporting.value = true;
  try {
    const result = await importNgManualPieces(file);
    if (Number(result.failureCount || 0) > 0) {
      showManualPieceImportFailures(result);
      return;
    }
    manualPieceImportVisible.value = false;
    message.success(
      `历史不合格品导入成功：${result.successCount || 0} 条，跳过空行 ${result.skippedRows || 0} 条`,
    );
    await refreshAll();
  } finally {
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
    title: '确认导入历史不合格品',
  });
}

function canUnfreezeManualPiece(piece?: NgPiece) {
  return Boolean(isManualHistoryPiece(piece) && piece?.status === 'FROZEN');
}

function openManualUnfreeze(piece?: NgPiece) {
  if (!canUnfreezeManualPiece(piece)) return;
  manualUnfreezeTarget.value = piece;
  manualUnfreezeReason.value = '';
  manualUnfreezeVisible.value = true;
}

async function submitManualUnfreeze() {
  const target = manualUnfreezeTarget.value;
  const unfreezeReason = manualUnfreezeReason.value.trim();
  if (!target?.id || !canUnfreezeManualPiece(target)) {
    message.warning('历史冻结片状态已变化，请刷新后重试');
    return;
  }
  if (!unfreezeReason) {
    message.warning('请填写解除冻结原因');
    return;
  }
  manualUnfreezeSubmitting.value = true;
  try {
    await unfreezeNgManualPiece({ pieceId: target.id, unfreezeReason });
    manualUnfreezeVisible.value = false;
    selectedPiece.value = null;
    message.success(
      `历史冻结片 ${target.pieceNo || ''} 已解除冻结并自动归入对应工序库`,
    );
    await refreshAll();
  } finally {
    manualUnfreezeSubmitting.value = false;
  }
}

function changeFixedWarehouse() {
  selectedPiece.value = null;
  selectedStockPieces.value = [];
  clearPrintSelection();
  stockGridApi.query();
}

function changeStockView(mode: 'flat' | 'group') {
  if (stockViewMode.value === mode) return;
  stockViewMode.value = mode;
  selectedPiece.value = null;
  clearPrintSelection();
  selectedStockPieces.value = [];

  stockGridApi.query();
}

function toggleActiveViewMode() {
  changeStockView(stockViewMode.value === 'group' ? 'flat' : 'group');
}

function queryActiveTab() {
  clearPrintSelection();
  return stockGridApi.query();
}

const stockColumns = [
  { fixed: 'left', type: 'checkbox', width: 46 },
  {
    align: 'center',
    field: 'printSelection',
    fixed: 'left',
    slots: { default: 'stockPrintSelection' },
    title: '打印',
    width: 86,
  },
  {
    field: 'pieceNo',
    fixed: 'left',
    slots: { default: 'stockPieceNo' },
    title: '段批次 / 片号',
    treeNode: true,
    width: 240,
  },
  { field: 'processName', title: '工序', width: 90 },
  { field: 'status', slots: { default: 'status' }, title: '状态', width: 100 },
  {
    field: 'currentLocationName',
    slots: { default: 'stockLocation' },
    title: '所在仓库',
    width: 150,
  },
  { field: 'sourceBatchNo', title: '来源批号', width: 160 },
  { field: 'modelNo', title: '型号', width: 130 },
  {
    field: 'padType',
    formatter: ({ row }) => padTypeNameOf(row.padType),
    title: '垫型',
    width: 110,
  },
  {
    field: 'defectSummary',
    minWidth: 220,
    showOverflow: 'tooltip',
    title: 'NG原因',
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'stockActions' },
    title: '操作',
    width: 160,
  },
];

async function loadInventorySegmentChildren(row: NgPieceDisplayRow) {
  if (!isGroupRow(row) || !row.sourceParentBatchNo) return [];
  const rows = await getNgInventorySegmentPieceList({
    keyword: keyword.value.trim() || undefined,
    warehouseCode: fixedWarehouse.value,
    padType: padTypeFilter.value,
    processType: row.processType,
    segmentBatchNo: row.sourceParentBatchNo,
    sourcePlanOperationId: row.sourcePlanOperationId,
  });
  return (rows || []).map(toStockPieceRow);
}

const [StockGrid, stockGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    checkboxConfig: {
      checkMethod: ({ row }: { row: NgPieceDisplayRow }) =>
        isStockSelectionCompatible(row),
      highlight: true,
      showHeader: false,
      trigger: 'row',
    },
    columns: stockColumns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const params = {
            warehouseCode: fixedWarehouse.value,
            padType: padTypeFilter.value,
            keyword: keyword.value.trim() || undefined,
            pageNo: getPageNo(page),
            pageSize: getPageSize(page),
          };
          selectedStockPieces.value = [];
          if (stockViewMode.value === 'group') {
            const result = await getNgInventorySegmentPage(params);
            return {
              ...result,
              list: (result.list || []).map((row, index) =>
                toStockSegmentRow(row, index),
              ),
            };
          }
          const result = await getNgPiecePage(params);
          return {
            ...result,
            list: (result.list || []).map(toStockPieceRow),
          };
        },
      },
    },
    rowClassName: ({ row }: { row: NgPieceDisplayRow }) =>
      isGroupRow(row) ? 'ng-segment-row' : '',
    rowConfig: { isHover: true, keyField: 'rowKey' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
    treeConfig: {
      children: 'children',
      hasChild: 'hasChild',
      lazy: true,
      loadMethod: ({ row }: { row: NgPieceDisplayRow }) =>
        loadInventorySegmentChildren(row),
      reserve: true,
      showLine: true,
    },
  } as VxeTableGridOptions<NgPieceDisplayRow>,
  gridEvents: {
    checkboxAll: ({ records }: { records: NgPieceDisplayRow[] }) =>
      handleStockCheckboxChange(records),
    checkboxChange: ({ records }: { records: NgPieceDisplayRow[] }) =>
      handleStockCheckboxChange(records),
    cellClick: ({ row }: { row: NgPieceDisplayRow }) => {
      if (!isGroupRow(row)) selectPiece(row as NgPiece);
    },
  },
});

onMounted(refreshAll);
</script>

<template>
  <Page auto-content-height>
    <div class="ng-inventory-shelf package-fg-console">
      <input
        ref="manualPieceImportInputRef"
        accept=".xls,.xlsx"
        class="manual-piece-file-input"
        type="file"
        @change="handleManualPieceImportFileChange"
      />
      <section class="prototype-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:package-x" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">分切压槽不合格品库</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"
              >分切、压槽不合格片自动归库；冻结片统一进入冻结库</span
            >
            <span class="console-meta-item"
              >已选择 {{ selectedStockPieces.length }} 片</span
            >
          </div>
        </div>
        <div class="console-action-group">
          <button
            class="action-tile"
            :disabled="actionLoading"
            type="button"
            @click="openManualPieceModal"
          >
            <IconifyIcon icon="lucide:plus" />
            <span>新增历史片</span>
          </button>
          <button
            class="action-tile"
            :disabled="actionLoading"
            type="button"
            @click="manualPieceImportVisible = true"
          >
            <IconifyIcon icon="lucide:file-up" />
            <span>导入历史片</span>
          </button>
          <button
            class="action-tile"
            :class="{
              'is-disabled':
                selectedPrintPieces.length === 0 ||
                printSelecting ||
                printSubmitting,
            }"
            :disabled="
              selectedPrintPieces.length === 0 ||
              printSelecting ||
              printSubmitting
            "
            type="button"
            @click="openNgPiecePrint"
          >
            <IconifyIcon icon="lucide:printer" />
            <span>打印已选（{{ selectedPrintPieces.length }}）</span>
          </button>
          <button
            class="action-tile"
            :disabled="selectedPrintPieces.length === 0 || printSubmitting"
            type="button"
            @click="clearPrintSelection"
          >
            <IconifyIcon icon="lucide:eraser" />
            <span>清空打印选择</span>
          </button>
          <button
            class="action-tile"
            :disabled="actionLoading || loadingLocations"
            type="button"
            @click="refreshAll"
          >
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button
            class="action-tile"
            :disabled="actionLoading"
            type="button"
            @click="toggleActiveViewMode"
          >
            <IconifyIcon
              :icon="
                activeViewMode === 'group' ? 'lucide:git-branch' : 'lucide:list'
              "
            />
            <span
              >展示方式：{{
                activeViewMode === 'group' ? '分类' : '不分类'
              }}</span
            >
          </button>
        </div>
      </section>

      <section class="ng-shelf-layout">
        <div class="ng-left-panel">
          <div class="ng-toolbar">
            <Select
              v-model:value="padTypeFilter"
              allow-clear
              placeholder="全部垫型"
              style="min-width: 140px"
              :options="[
                { label: '黑垫', value: 'BLACK_PAD' },
                { label: '白垫', value: 'WHITE_PAD' },
              ]"
              @change="queryActiveTab"
            />
            <Input
              v-model:value="keyword"
              allow-clear
              placeholder="段批次 / 片号 / 来源批号 / 计划号"
              @press-enter="queryActiveTab"
            />
            <Button type="primary" @click="queryActiveTab">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
          </div>

          <Tabs
            v-model:active-key="fixedWarehouse"
            class="ng-warehouse-tabs"
            @change="changeFixedWarehouse"
          >
            <TabPane
              v-for="warehouse in fixedWarehouses"
              :key="warehouse.code"
              :tab="`${warehouse.name} (${locations.filter((item) => item.warehouseCode === warehouse.code).reduce((sum, item) => sum + item.occupiedQty, 0)}片)`"
            />
          </Tabs>

          <div class="ng-stock-tab-content">
            <div class="ng-stock-operation-bar">
              <span class="ng-stock-selected-count">
                已选{{
                  isManualFrozenStockSelection ? '历史冻结片' : '普通库存'
                }}
                {{ selectedStockPieces.length }} 片
              </span>
              <Button
                v-if="!isManualFrozenStockSelection"
                danger
                :disabled="selectedStockPieces.length === 0 || actionLoading"
                @click="openScrap()"
              >
                批量报废
              </Button>
            </div>
            <section class="ng-grid-panel">
              <StockGrid table-title="分切压槽不合格品库存">
                <template #stockPrintSelection="{ row }">
                  <Button
                    v-if="row.__group"
                    :loading="printSelecting"
                    size="small"
                    type="link"
                    @click.stop="selectPrintSegment(row, 'stock')"
                  >
                    选择本段
                  </Button>
                  <Checkbox
                    v-else
                    :checked="isPrintPieceSelected(row)"
                    :disabled="printSubmitting"
                    @click.stop
                    @change="handlePrintPieceCheckbox(row, $event)"
                  />
                </template>
                <template #stockPieceNo="{ row }">
                  <div v-if="row.__group" class="ng-segment-cell">
                    <strong>{{
                      row.sourceParentBatchNo || row.pieceNo || '-'
                    }}</strong>
                    <em>{{ formatGroupSamples(row) }}</em>
                  </div>
                  <span v-else>{{ row.pieceNo || '-' }}</span>
                </template>
                <template #status="{ row }">
                  <Tag v-if="row.__group" color="blue">分组</Tag>
                  <Tag v-else :color="ngStatusColor(row.status)">
                    {{ ngStatusText(row.status) }}
                  </Tag>
                </template>
                <template #stockLocation="{ row }">
                  <span v-if="row.__group"
                    >共 {{ row.groupCount || 0 }} 片</span
                  >
                  <span v-else>{{ row.currentLocationName || '-' }}</span>
                </template>
                <template #stockActions="{ row }">
                  <div v-if="!row.__group" class="ng-row-actions is-center">
                    <Button
                      v-if="row.status === 'STORED'"
                      danger
                      :loading="actionLoading"
                      size="small"
                      type="link"
                      @click.stop="openScrap(row)"
                    >
                      报废
                    </Button>
                    <Button
                      v-if="canUnfreezeManualPiece(row)"
                      danger
                      :loading="actionLoading"
                      size="small"
                      type="link"
                      @click.stop="openManualUnfreeze(row)"
                    >
                      解除冻结
                    </Button>
                  </div>
                </template>
              </StockGrid>
            </section>
          </div>
        </div>
      </section>

      <Modal
        v-model:open="manualPieceVisible"
        :confirm-loading="manualPieceSubmitting"
        ok-text="新增并自动入库"
        title="新增历史不合格品"
        width="700px"
        @ok="submitManualPiece"
      >
        <div class="manual-piece-tip">
          仅用于补录 MES
          上线前已有的分切、压槽不合格片；不填写计划号和生产日期。普通片按归属工序自动入分切库或压槽库，冻结片自动入冻结库。
        </div>
        <div class="manual-piece-form">
          <label><i>*</i>段批次</label>
          <Input
            v-model:value="manualPieceForm.segmentBatchNo"
            allow-clear
            maxlength="100"
            placeholder="例如：W26G143"
          />
          <label><i>*</i>片号</label>
          <Input
            v-model:value="manualPieceForm.pieceNo"
            allow-clear
            maxlength="100"
            placeholder="例如：W26G143AS102B"
          />
          <label><i>*</i>工序</label>
          <Select
            v-model:value="manualPieceForm.processType"
            :options="[
              { label: '分切', value: 'SLITTING' },
              { label: '压槽', value: 'PRESS_SLOT' },
            ]"
          />
          <label><i>*</i>来源批号</label>
          <Input
            v-model:value="manualPieceForm.sourceBatchNo"
            allow-clear
            maxlength="100"
            placeholder="请输入来源批号"
          />
          <label>料号</label>
          <Input
            v-model:value="manualPieceForm.materialCode"
            allow-clear
            maxlength="64"
            placeholder="选填"
          />
          <label><i>*</i>型号</label>
          <Input
            v-model:value="manualPieceForm.modelNo"
            allow-clear
            maxlength="64"
            placeholder="例如：W33P0300"
          />
          <label><i>*</i>垫型</label>
          <Select
            v-model:value="manualPieceForm.padType"
            :options="[
              { label: '黑垫', value: 'BLACK_PAD' },
              { label: '白垫', value: 'WHITE_PAD' },
            ]"
          />
          <label><i>*</i>NG原因</label>
          <Input.TextArea
            v-model:value="manualPieceForm.defectSummary"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            maxlength="1000"
            placeholder="请填写 NG 原因"
            show-count
          />
          <label><i>*</i>入库类型</label>
          <Select
            v-model:value="manualPieceForm.storageTarget"
            :options="[
              { label: '普通不合格品', value: 'NORMAL' },
              { label: '冻结品', value: 'FREEZE' },
            ]"
          />
          <template v-if="manualPieceVisible">
            <label><i>*</i>补录原因</label>
            <Input.TextArea
              v-model:value="manualPieceForm.backfillReason"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              maxlength="500"
              placeholder="请填写历史数据补录原因"
              show-count
            />
            <label>备注</label>
            <Input.TextArea
              v-model:value="manualPieceForm.remark"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              maxlength="500"
              placeholder="选填"
              show-count
            />
          </template>
        </div>
      </Modal>

      <Modal
        v-model:open="manualPieceImportVisible"
        :footer="null"
        title="批量导入历史不合格品"
        width="700px"
      >
        <div class="manual-piece-import-panel">
          <div class="manual-piece-tip">
            系统整批校验段批次、片号、工序、来源批号、型号、垫型、NG
            原因和入库类型；料号为选填。黑垫、白垫均可上架；不填写计划号、生产日期。任一行失败，本批次不写入。
          </div>
          <div class="manual-piece-import-steps">
            <div>
              <strong>1. 下载导入模板</strong>
              <span>模板支持普通不合格品和冻结品两种入库类型。</span>
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
                选择 Excel 并导入
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="manualUnfreezeVisible"
        :confirm-loading="manualUnfreezeSubmitting"
        ok-text="确认解除并归库"
        :ok-button-props="{ danger: true }"
        title="人工解除历史冻结片"
        @ok="submitManualUnfreeze"
      >
        <p>
          片号
          {{ manualUnfreezeTarget?.pieceNo || '-' }}
          将解除冻结并自动归入对应的分切库或压槽库，库存数量保持不变。
        </p>
        <Input.TextArea
          v-model:value="manualUnfreezeReason"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          maxlength="500"
          placeholder="请填写解除冻结原因（必填）"
          show-count
        />
      </Modal>

      <Modal
        v-model:open="scrapVisible"
        :confirm-loading="actionLoading"
        ok-text="确认报废"
        title="报废不合格品"
        @ok="submitScrap"
      >
        <p>
          报废后
          {{ scrapTargets.length }}
          片不再显示在当前不合格品库中，但会保留原因、操作人、时间和流水。
        </p>
        <Input.TextArea
          v-model:value="scrapReason"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          maxlength="1000"
          placeholder="请填写报废原因（必填）"
          show-count
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.ng-inventory-shelf {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.location-code {
  max-width: 150px;
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ng-rule-meta {
  max-width: 330px;
}

.ng-shelf-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.ng-left-panel {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.ng-location-tree-panel {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.ng-toolbar {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) 96px;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.ng-toolbar > * {
  min-width: 0;
}

.ng-warehouse-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.ng-warehouse-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.ng-location-toolbar {
  grid-template-columns: minmax(0, 1fr) 72px;
}

.ng-shelf-tabs {
  display: flex;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.ng-shelf-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.ng-shelf-tabs :deep(.ant-tabs-content-holder),
.ng-shelf-tabs :deep(.ant-tabs-content),
.ng-shelf-tabs :deep(.ant-tabs-tabpane) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.ng-shelf-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
}

.ng-shelf-tabs :deep(.ant-tabs-content),
.ng-shelf-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.ng-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.ng-grid-panel :deep(.vben-vxe-grid),
.ng-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.ng-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.ng-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.ng-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.ng-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.ng-stock-tab-content {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.ng-stock-operation-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.ng-stock-view-label {
  color: #334155;
  font-weight: 600;
}

.ng-stock-selected-count {
  margin-right: auto;
  color: #475569;
  font-size: 13px;
}

.ng-grid-panel :deep(.ng-segment-row) {
  background: #edf5ff;
}

.ng-segment-cell {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.ng-segment-cell strong,
.ng-segment-cell em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ng-segment-cell strong {
  color: #075985;
  font-weight: 700;
}

.ng-segment-cell em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.ng-location-tip,
.ng-location-selected {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  align-items: center;
  min-height: 58px;
  max-height: 68px;
  padding: 10px;
  margin: 0;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.ng-location-tip {
  display: none;
}

.ng-location-selected strong,
.ng-location-selected span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ng-location-selected span {
  color: #64748b;
  font-size: 12px;
}

.ng-tree-scroll {
  height: 100%;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.ng-tree-scroll :deep(.ant-tree) {
  min-width: max-content;
}

.ng-tree-scroll :deep(.ant-tree-treenode) {
  align-items: center;
  width: 100%;
  padding: 2px 0;
}

.ng-tree-scroll :deep(.ant-tree-node-content-wrapper) {
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-height: 28px;
  padding: 0 4px;
  line-height: 28px;
  border-radius: 4px;
}

.ng-tree-scroll :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background: #dbeafe;
}

.ng-tree-scroll :deep(.ant-tree-node-disabled .ant-tree-node-content-wrapper) {
  color: inherit;
  cursor: not-allowed;
  opacity: 1;
}

.ng-tree-scroll :deep(.ant-tree-title) {
  display: block;
  width: 100%;
  min-width: 0;
}

.ng-location-node {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 28px;
  color: #334155;
  font-weight: 800;
  white-space: nowrap;
}

.ng-location-node :deep(.iconify) {
  flex: 0 0 auto;
  font-size: 15px;
}

.ng-location-node.is-rack {
  color: #075985;
}

.ng-location-node em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 18px;
  padding: 0 6px;
  color: #475569;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 18px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
}

.ng-pallet-block {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-width: 132px;
  max-width: 180px;
  min-height: 30px;
  padding: 4px 8px;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.ng-pallet-block.is-free {
  color: #166534;
  background: #dcfce7;
  border-color: #86efac;
}

.ng-pallet-block.is-occupied {
  color: #991b1b;
  background: #fee2e2;
  border-color: #fca5a5;
}

.ng-pallet-block.is-unavailable {
  color: #475569;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.ng-pallet-block strong {
  flex: 0 0 auto;
  color: inherit;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 950;
  white-space: nowrap;
}

.ng-pallet-block em {
  flex: 0 0 auto;
  overflow: hidden;
  color: inherit;
  font-style: normal;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ng-row-actions {
  display: flex;
  width: 100%;
  gap: 2px;
  white-space: nowrap;
}

.ng-row-actions.is-center {
  justify-content: center;
}

.ng-row-actions :deep(.ant-btn-sm) {
  min-width: 36px;
  padding-inline: 2px;
}

.manual-piece-file-input {
  display: none;
}

.manual-piece-tip {
  margin-bottom: 14px;
  padding: 10px 12px;
  color: #475569;
  line-height: 1.65;
  background: #f1f5f9;
  border-left: 3px solid #2563eb;
}

.manual-piece-form {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px 14px;
  align-items: center;
}

.manual-piece-form > label {
  color: #334155;
  font-weight: 700;
  text-align: right;
}

.manual-piece-form > label i {
  margin-right: 3px;
  color: #dc2626;
  font-style: normal;
}

.manual-piece-import-steps {
  display: grid;
  gap: 12px;
}

.manual-piece-import-steps > div {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) max-content;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}

.manual-piece-import-steps span {
  color: #64748b;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .ng-toolbar {
    grid-template-columns: minmax(0, 1fr) 80px;
  }

  .ng-toolbar > :first-child {
    grid-column: 1 / -1;
  }
}
</style>
