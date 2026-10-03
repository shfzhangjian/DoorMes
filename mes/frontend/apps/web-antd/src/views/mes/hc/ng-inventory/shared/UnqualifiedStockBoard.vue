<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getNgPieceLabels,
  getNgPiecePage,
  manualOutboundNgPieces,
  markNgPieceLabelsPrinted,
} from '#/api/mes/hc/ng-inventory';
import {
  getFgInboundPackagePage,
  getFgInboundWaitPiecePage,
  getFgStockLedgerPage,
  getPackagingPieceLabels,
  manualOutboundFgStocks,
  manualOutboundPendingFgInboundPackages,
  markPackagingPieceLabelsPrinted,
} from '#/api/mes/hc/package-fg/finished-packaging';

import {
  buildPackagingPieceLabelPayload,
  resolvePackagingPieceLabelTemplateCode,
} from '../../execution/report/shared/packagingPieceLabelPrint';
import { buildSlittingTransferTicketPayload } from '../../execution/report/shared/slittingTransferTicketPrint';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'UnqualifiedStockBoard' });

const props = withDefaults(
  defineProps<{
    scope?: 'ALL' | 'FINISHED' | 'PRESS_SLOT' | 'SLITTING';
  }>(),
  { scope: 'ALL' },
);
const isProcessScope = computed(() =>
  ['PRESS_SLOT', 'SLITTING'].includes(props.scope),
);
const pageTitle = computed(
  () =>
    ({
      ALL: '不合格品库存查询',
      SLITTING: '分切不合格品库',
      PRESS_SLOT: '压槽不合格品库',
      FINISHED: '成品不合格品库',
    })[props.scope],
);
const permissionPrefix = computed(
  () =>
    ({
      ALL: 'mes:inv:unqualified-stock-query',
      SLITTING: 'mes:inv:slitting-ng-stock',
      PRESS_SLOT: 'mes:inv:press-slot-ng-stock',
      FINISHED: 'mes:inv:finished-ng-stock',
    })[props.scope],
);
const resultTotal = ref(0);
const visibleRows = ref<UnifiedNgRow[]>([]);

type InventoryArea =
  | 'FG_WAREHOUSE'
  | 'NG_WAREHOUSE'
  | 'PACKAGED_PENDING_SHELF'
  | 'WAIT_PACKAGING';
type UnifiedNgRow = {
  coaInspectionResult?: string;
  fqcInspectionResult?: string;
  id: string;
  inventoryArea: InventoryArea;
  locationText?: string;
  materialCode?: string;
  modelCode?: string;
  ngPieceId?: number;
  ngReason?: string;
  pendingPackageId?: number;
  planNo?: string;
  processName?: string;
  qty: number;
  qualityResult?: string;
  recordTime?: string;
  segmentBatchNo?: string;
  sliceBatchNo?: string;
  status: string;
  stockId?: number;
};
type StockQuery = {
  area: '' | InventoryArea;
  dateEnd: string;
  dateStart: string;
  keyword: string;
  materialCode: string;
  modelCode: string;
  status: string;
};
type OutboundMode =
  | 'FG_WAREHOUSE'
  | 'NG_WAREHOUSE'
  | 'PACKAGED_PENDING_SHELF'
  | undefined;

const PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PRINT_BATCH_MAX_COUNT = 50;
const LOAD_PAGE_SIZE = 200;

const allRows = ref<UnifiedNgRow[]>([]);
const selectedRowMap = ref<Map<string, UnifiedNgRow>>(new Map());
const outboundVisible = ref(false);
const outboundReason = ref('');
const outboundLoading = ref(false);
const printLoading = ref(false);
const query = reactive<StockQuery>({
  area: '',
  dateEnd: '',
  dateStart: '',
  keyword: '',
  materialCode: '',
  modelCode: '',
  status: '',
});

const areaOptions = computed(() =>
  [
    {
      label: isProcessScope.value ? pageTitle.value : '分切压槽不合格品库',
      value: 'NG_WAREHOUSE',
    },
    { label: '不合格待包装区', value: 'WAIT_PACKAGING' },
    { label: '已包装待上架区', value: 'PACKAGED_PENDING_SHELF' },
    { label: '成品仓库', value: 'FG_WAREHOUSE' },
  ].filter(
    (item) => props.scope !== 'FINISHED' || item.value !== 'NG_WAREHOUSE',
  ),
);
const statusOptions = computed(() =>
  [
    { label: '待上架', value: 'WAIT_SHELF' },
    { label: '待冻结上架', value: 'WAIT_FREEZE_SHELF' },
    { label: '正常在库', value: 'STORED' },
    { label: '冻结在库', value: 'FROZEN' },
    { label: '待包装', value: 'WAIT_PACKAGING' },
    { label: '已包装待上架', value: 'PACKED' },
    { label: '下架待重新上架', value: 'INBOUND_LOCKED' },
    { label: '成品在库', value: 'AVAILABLE' },
  ].filter((item) => {
    if (props.scope === 'ALL') return true;
    const ngStatus = [
      'FROZEN',
      'STORED',
      'WAIT_FREEZE_SHELF',
      'WAIT_SHELF',
    ].includes(item.value);
    return isProcessScope.value
      ? ngStatus
      : !ngStatus || item.value === 'FROZEN';
  }),
);

const filteredRows = computed(() => {
  if (isProcessScope.value) return allRows.value;
  const keyword = query.keyword.trim().toLowerCase();
  const materialCode = query.materialCode.trim().toLowerCase();
  const modelCode = query.modelCode.trim().toLowerCase();
  return allRows.value.filter((row) => {
    if (query.area && row.inventoryArea !== query.area) return false;
    if (query.status && row.status !== query.status) return false;
    if (
      materialCode &&
      !String(row.materialCode || '')
        .toLowerCase()
        .includes(materialCode)
    ) {
      return false;
    }
    if (
      modelCode &&
      !String(row.modelCode || '')
        .toLowerCase()
        .includes(modelCode)
    ) {
      return false;
    }
    if (keyword) {
      const searchText = [
        row.sliceBatchNo,
        row.segmentBatchNo,
        row.planNo,
        row.materialCode,
        row.modelCode,
        row.locationText,
        row.ngReason,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      if (!searchText.includes(keyword)) return false;
    }
    const rowDate = String(row.recordTime || '').slice(0, 10);
    if (query.dateStart && (!rowDate || rowDate < query.dateStart))
      return false;
    if (query.dateEnd && (!rowDate || rowDate > query.dateEnd)) return false;
    return true;
  });
});
const selectedRows = computed(() => [...selectedRowMap.value.values()]);
const selectedCount = computed(() => selectedRows.value.length);
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const totalQty = computed(() =>
  filteredRows.value.reduce((sum, row) => sum + Number(row.qty || 0), 0),
);
const areaSummary = computed(() => {
  const summary = {
    FG_WAREHOUSE: 0,
    NG_WAREHOUSE: 0,
    PACKAGED_PENDING_SHELF: 0,
    WAIT_PACKAGING: 0,
  } satisfies Record<InventoryArea, number>;
  filteredRows.value.forEach((row) => {
    summary[row.inventoryArea] += Number(row.qty || 0);
  });
  return summary;
});
const outboundMode = computed<OutboundMode>(() => {
  if (selectedRows.value.length === 0) return undefined;
  if (
    selectedRows.value.every(
      (row) =>
        row.inventoryArea === 'NG_WAREHOUSE' &&
        ['FROZEN', 'STORED', 'WAIT_FREEZE_SHELF', 'WAIT_SHELF'].includes(
          row.status,
        ),
    )
  ) {
    return 'NG_WAREHOUSE';
  }
  if (
    selectedRows.value.every(
      (row) =>
        row.inventoryArea === 'PACKAGED_PENDING_SHELF' &&
        ['INBOUND_LOCKED', 'PACKED'].includes(row.status),
    )
  ) {
    return 'PACKAGED_PENDING_SHELF';
  }
  if (selectedRows.value.every((row) => row.inventoryArea === 'FG_WAREHOUSE')) {
    return 'FG_WAREHOUSE';
  }
  return undefined;
});
const OUTBOUND_UNAVAILABLE_MESSAGE =
  '仅支持同一来源的待上架、冻结、已包装待上架或成品仓在库不合格品批量出库';

function normalizeSliceBatchNo(value?: string) {
  return String(value || '')
    .trim()
    .toUpperCase();
}

function valueText(value?: string) {
  return String(value || '').trim();
}

function appendReason(...values: Array<string | undefined>) {
  return (
    values
      .map((value) => valueText(value))
      .filter(Boolean)
      .join('；') || '-'
  );
}

function buildPackagingNgReason(item: {
  coaInspectionResult?: string;
  coaNgReason?: string;
  inspectionRemark?: string;
  inspectionResult?: string;
  packagingQualityStatus?: string;
  qualityRiskFlag?: string;
  qualityStatus?: string;
}) {
  const packagingQualityStatus = valueText(
    item.packagingQualityStatus || item.qualityStatus,
  ).toUpperCase();
  const coaInspectionResult = valueText(item.coaInspectionResult).toUpperCase();
  let coaReason = '';
  if (coaInspectionResult === 'NG') {
    coaReason = `COA：${item.coaNgReason || '不合格'}`;
  } else if (
    coaInspectionResult === 'PENDING' &&
    packagingQualityStatus === 'NG'
  ) {
    coaReason = `COA：${item.coaNgReason || '待检或未放行'}`;
  }
  const reasons = [
    valueText(item.inspectionResult).toUpperCase() === 'NG'
      ? `FQC：${item.inspectionRemark || '不合格'}`
      : '',
    coaReason,
    item.qualityRiskFlag && item.qualityRiskFlag !== 'NONE'
      ? `质量风险：${item.qualityRiskFlag}`
      : '',
  ].filter(Boolean);
  if (reasons.length === 0 && packagingQualityStatus === 'NG') {
    reasons.push('历史源数据未留存具体不合格原因');
  }
  return reasons.join('；');
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function statusText(status: string) {
  return (
    statusOptions.value.find((item) => item.value === status)?.label || status
  );
}

function statusColor(status: string) {
  if (status === 'STORED' || status === 'AVAILABLE') return 'green';
  if (
    status === 'FROZEN' ||
    status === 'WAIT_FREEZE_SHELF' ||
    status === 'INBOUND_LOCKED'
  ) {
    return 'blue';
  }
  if (
    status === 'WAIT_PACKAGING' ||
    status === 'WAIT_SHELF' ||
    status === 'PACKED'
  ) {
    return 'orange';
  }
  return 'default';
}

function areaText(area: InventoryArea) {
  return areaOptions.value.find((item) => item.value === area)?.label || area;
}

function areaColor(area: InventoryArea) {
  if (area === 'FG_WAREHOUSE') return 'purple';
  if (area === 'WAIT_PACKAGING') return 'gold';
  if (area === 'PACKAGED_PENDING_SHELF') return 'cyan';
  return 'volcano';
}

async function fetchAll<T>(
  loader: (params: Record<string, any>) => Promise<any>,
  params: Record<string, any> = {},
) {
  const result: T[] = [];
  for (let pageNo = 1; ; pageNo += 1) {
    const page = await loader({ ...params, pageNo, pageSize: LOAD_PAGE_SIZE });
    const list = (page?.list || []) as T[];
    result.push(...list);
    if (result.length >= Number(page?.total || 0)) break;
    if (list.length === 0) throw new Error('库存分页数据不完整，请刷新后重试');
  }
  return result;
}

function mapNgPiece(piece: MesHcNgInventoryApi.NgPiece): UnifiedNgRow {
  const status = valueText(piece.status);
  const isPending = status === 'WAIT_SHELF' || status === 'WAIT_FREEZE_SHELF';
  const pendingLocationText =
    status === 'WAIT_FREEZE_SHELF' ? '待冻结上架区' : '待上架区';
  return {
    qualityResult: piece.qualityResult === 'OK' ? 'OK' : 'NG',
    id: `NG-${piece.id}`,
    inventoryArea: 'NG_WAREHOUSE',
    locationText: isPending
      ? pendingLocationText
      : appendReason(piece.currentWarehouseName, piece.currentLocationName),
    materialCode: piece.materialCode,
    modelCode: piece.modelNo,
    ngPieceId: piece.id,
    ngReason: piece.defectSummary,
    planNo: piece.sourcePlanNo,
    processName: piece.processName,
    qty: Number(piece.pieceQty || 0),
    recordTime: piece.shelvedTime,
    // 分切、压槽库展示分段号，保留接口原始来源批号供追溯和打印使用。
    segmentBatchNo: isProcessScope.value
      ? (piece.sourceParentBatchNo?.trim() || piece.sourceBatchNo?.trim())?.replace(
          /-J\d+$/i,
          '',
        )
      : piece.sourceBatchNo,
    sliceBatchNo: piece.pieceNo,
    status,
  };
}

function mapWaitPackaging(
  item: MesHcFinishedPackagingApi.InspectionSlice,
): UnifiedNgRow {
  return {
    coaInspectionResult: item.coaInspectionResult,
    fqcInspectionResult: item.inspectionResult,
    id: `WAIT-PACKAGING-${item.sourceType}-${item.sourceCutRoundReportId || item.sourceManualPieceId || item.sliceBatchNo}`,
    inventoryArea: 'WAIT_PACKAGING',
    locationText: '不合格待包装区',
    materialCode: item.materialCode,
    modelCode: item.modelCode,
    ngReason: buildPackagingNgReason(item),
    planNo: item.planNo,
    processName: '裁切 / 包装',
    qty: 1,
    recordTime: item.recorderTime || item.inspectionTime,
    segmentBatchNo: item.segmentBatchNo,
    sliceBatchNo: item.sliceBatchNo,
    status: 'WAIT_PACKAGING',
  };
}

function mapPendingInboundPackage(
  box: MesHcFinishedPackagingApi.InboundBox,
): UnifiedNgRow[] {
  const packageStatus = valueText(box.status);
  return (box.items || []).map(
    (item): UnifiedNgRow => ({
      coaInspectionResult: item.coaInspectionResult,
      fqcInspectionResult: item.inspectionResult,
      id: `PACKAGED-PENDING-${box.id}-${item.id || item.sliceBatchNo || item.productionBatchNo}`,
      inventoryArea: 'PACKAGED_PENDING_SHELF',
      locationText: `已包装待上架区（包装号：${box.boxNo || '-'}）`,
      materialCode: box.materialCode,
      modelCode: box.modelCode,
      ngReason: appendReason(
        item.ngReason || buildPackagingNgReason(item),
        box.remark ? `包装备注：${box.remark}` : '',
      ),
      pendingPackageId: box.id,
      planNo: box.planNo,
      processName: '成品包装',
      qty: 1,
      recordTime: box.lockTime || box.recorderTime,
      segmentBatchNo: box.motherSegmentBatchNo,
      sliceBatchNo: item.sliceBatchNo || item.productionBatchNo,
      status: packageStatus,
    }),
  );
}

function mapFgStock(
  stock: MesHcFinishedPackagingApi.FgStockLedger,
): UnifiedNgRow {
  return {
    coaInspectionResult: stock.coaInspectionResult,
    fqcInspectionResult: stock.inspectionResult,
    id: `FG-${stock.id}`,
    inventoryArea: 'FG_WAREHOUSE',
    locationText: appendReason(
      stock.warehouseName || stock.warehouseCode,
      stock.locationName || stock.locationCode,
    ),
    materialCode: stock.materialCode,
    modelCode: stock.modelCode,
    ngReason: appendReason(
      stock.inspectionResult === 'NG' ? 'FQC不合格' : '',
      stock.coaInspectionResult === 'NG' ? 'COA不合格' : '',
      stock.remark,
    ),
    processName: '成品包装',
    qty: Number(stock.qty || 0),
    recordTime: stock.inboundTime,
    segmentBatchNo: stock.batchNo,
    sliceBatchNo: stock.sliceBatchNo,
    status: valueText(stock.stockStatus) || 'AVAILABLE',
    stockId: stock.id,
  };
}

async function loadUnifiedRows() {
  const loadNg = (status: string) =>
    props.scope === 'FINISHED'
      ? Promise.resolve([] as MesHcNgInventoryApi.NgPiece[])
      : fetchAll<MesHcNgInventoryApi.NgPiece>(getNgPiecePage, {
          status,
          unqualifiedOnly: true,
        });
  const [
    waitShelf,
    waitFreezeShelf,
    stored,
    frozen,
    waitPackaging,
    pendingPackages,
    fgStocks,
  ] = await Promise.all([
    loadNg('WAIT_SHELF'),
    loadNg('WAIT_FREEZE_SHELF'),
    loadNg('STORED'),
    loadNg('FROZEN'),
    fetchAll<MesHcFinishedPackagingApi.InspectionSlice>(
      getFgInboundWaitPiecePage,
      { packagingQualityStatus: 'NG' },
    ),
    fetchAll<MesHcFinishedPackagingApi.InboundBox>(getFgInboundPackagePage, {
      qualityStatus: 'NG',
      shelfStatus: 'PENDING',
    }),
    fetchAll<MesHcFinishedPackagingApi.FgStockLedger>(getFgStockLedgerPage, {
      qualityStatus: 'NG',
    }),
  ]);
  allRows.value = [
    ...[...waitShelf, ...waitFreezeShelf, ...stored, ...frozen].map((item) =>
      mapNgPiece(item),
    ),
    ...waitPackaging.map((item) => mapWaitPackaging(item)),
    ...pendingPackages
      .filter(
        (box) =>
          valueText(box.qualityStatus) === 'NG' &&
          (box.items || []).length > 0 &&
          (box.items || []).every(
            (item) => valueText(item.qualityStatus) === 'NG',
          ),
      )
      .flatMap((box) => mapPendingInboundPackage(box)),
    ...fgStocks.map((item) => mapFgStock(item)),
  ];
}

function clearSelectedRows() {
  selectedRowMap.value = new Map();
}

function toggleRowSelection(row: UnifiedNgRow, checked: boolean) {
  const nextMap = new Map(selectedRowMap.value);
  const groupedRows = row.pendingPackageId
    ? allRows.value.filter(
        (item) => item.pendingPackageId === row.pendingPackageId,
      )
    : [row];
  groupedRows.forEach((item) => {
    if (checked) nextMap.set(item.id, item);
    else nextMap.delete(item.id);
  });
  selectedRowMap.value = nextMap;
}

function togglePageSelection(checked: boolean) {
  visibleRows.value.forEach((row) => {
    toggleRowSelection(row, checked);
  });
}

function isAllFilteredSelected() {
  return (
    visibleRows.value.length > 0 &&
    visibleRows.value.every((row) => selectedRowMap.value.has(row.id))
  );
}

function buildPageResult(page?: Record<string, any>) {
  const currentPage = Number(page?.currentPage || page?.pageNo || 1);
  const pageSize = Number(page?.pageSize || 20);
  const start = (currentPage - 1) * pageSize;
  visibleRows.value = filteredRows.value.slice(start, start + pageSize);
  return {
    list: visibleRows.value,
    total: filteredRows.value.length,
  };
}

const columns = [
  {
    field: 'selected',
    fixed: 'left',
    slots: { default: 'selection', header: 'selectionHeader' },
    title: '选择',
    width: 62,
  },
  {
    field: 'inventoryArea',
    slots: { default: 'area' },
    title: '库存区域',
    width: 150,
  },
  {
    field: 'status',
    slots: { default: 'status' },
    title: '状态',
    visible: props.scope === 'ALL',
    width: 110,
  },
  {
    field: 'locationText',
    showOverflow: 'tooltip',
    title: '库位 / 区域',
    visible: props.scope === 'ALL',
    width: 180,
  },
  {
    field: 'processName',
    showOverflow: 'tooltip',
    title: '来源工序',
    width: 130,
  },
  { field: 'planNo', showOverflow: 'tooltip', title: '生产计划', width: 170 },
  {
    field: 'segmentBatchNo',
    formatter: ({ row }: { row: UnifiedNgRow }) =>
      props.scope === 'FINISHED'
        ? row.segmentBatchNo?.trim().replace(/-J\d+$/i, '')
        : row.segmentBatchNo,
    showOverflow: 'tooltip',
    title: '分段 / 来源批号',
    width: 180,
  },
  { field: 'sliceBatchNo', showOverflow: 'tooltip', title: '片号', width: 180 },
  { field: 'materialCode', showOverflow: 'tooltip', title: '料号', width: 150 },
  { field: 'modelCode', showOverflow: 'tooltip', title: '型号', width: 130 },
  {
    field: 'qty',
    title: '数量',
    visible: props.scope === 'ALL',
    width: 80,
  },
  {
    field: 'qualityResult',
    title: '质量结果',
    width: 95,
    visible: props.scope !== 'FINISHED',
  },
  {
    field: 'fqcInspectionResult',
    slots: { default: 'fqcResult' },
    title: 'FQC',
    visible: !isProcessScope.value,
    width: 85,
  },
  {
    field: 'coaInspectionResult',
    slots: { default: 'coaResult' },
    title: 'COA',
    visible: !isProcessScope.value,
    width: 85,
  },
  {
    field: 'ngReason',
    showOverflow: 'tooltip',
    title: '不合格原因',
    minWidth: 230,
  },
  {
    field: 'recordTime',
    formatter: ({ row }: { row: UnifiedNgRow }) =>
      formatDateTime(row.recordTime),
    title: '入区 / 上架时间',
    width: 175,
  },
];

if (props.scope !== 'ALL') {
  const modelIndex = columns.findIndex((column) => column.field === 'modelCode');
  const [modelColumn] = columns.splice(modelIndex, 1);
  if (modelColumn) {
    const planIndex = columns.findIndex((column) => column.field === 'planNo');
    columns.splice(planIndex + 1, 0, modelColumn);
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          allRows.value = [];
          visibleRows.value = [];
          resultTotal.value = 0;
          if (isProcessScope.value) {
            const result = await getNgPiecePage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              processType: props.scope,
              unqualifiedOnly: true,
              status: query.status || undefined,
              keyword: query.keyword.trim() || undefined,
              materialCode: query.materialCode.trim() || undefined,
              modelNo: query.modelCode.trim() || undefined,
              shelvedDateStart: query.dateStart || undefined,
              shelvedDateEnd: query.dateEnd || undefined,
            });
            allRows.value = result.list.map((piece) => mapNgPiece(piece));
            visibleRows.value = allRows.value;
            resultTotal.value = result.total;
            return { list: allRows.value, total: result.total };
          }
          await loadUnifiedRows();
          return buildPageResult(page as Record<string, any>);
        },
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
  } as VxeTableGridOptions<UnifiedNgRow>,
});

async function handleSearch() {
  clearSelectedRows();
  await gridApi.query();
}

async function handleReset() {
  Object.assign(query, {
    area: '',
    dateEnd: '',
    dateStart: '',
    keyword: '',
    materialCode: '',
    modelCode: '',
    status: '',
  });
  await handleSearch();
}

function packagingLabelTicketId(label: MesHcFinishedPackagingApi.PieceLabel) {
  return `${label.sourceType}-${label.sourceId}`;
}

function printSnapshot(
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
    qrValue: item?.qrValue || '',
    templateCode,
    title: item?.title || payload?.title || '',
  });
}

async function sendPrint(payload: unknown, rowCount: number) {
  const endpoint =
    rowCount > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
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

async function printNgWarehouseRows(rows: UnifiedNgRow[]) {
  const pieceIds = rows
    .map((row) => row.ngPieceId)
    .filter((id): id is number => !!id);
  if (pieceIds.length !== rows.length)
    throw new Error('所选不合格品缺少逐片标识，无法打印');
  let printed = 0;
  for (let start = 0; start < pieceIds.length; start += PRINT_BATCH_MAX_COUNT) {
    const batchIds = pieceIds.slice(start, start + PRINT_BATCH_MAX_COUNT);
    const labelResult = await getNgPieceLabels(batchIds);
    const labels = labelResult.labels || [];
    if (
      labels.length !== batchIds.length ||
      (labelResult.failures || []).length > 0
    ) {
      throw new Error(
        (labelResult.failures || [])[0] || '未获取到完整的不合格品标签数据',
      );
    }
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
        ticketId: label.pieceId,
        workTime: label.workTime,
      })),
      now,
    );
    const result = await sendPrint(payload, labels.length);
    if (result?.success !== true && !Array.isArray(result?.itemResults)) {
      throw new Error(result?.message || '本机打印服务未确认标签进入打印队列');
    }
    const accepted = labels.filter((label) => {
      const item = result?.itemResults?.find(
        (candidate: any) => Number(candidate?.ticketId) === label.pieceId,
      );
      return item
        ? item.success === true && item.accepted !== false
        : result?.success === true;
    });
    if (accepted.length === 0)
      throw new Error('本机打印服务未确认标签进入打印队列');
    await markNgPieceLabelsPrinted(
      accepted.map((label) => ({
        labelContentJson: JSON.stringify({
          printTime: now,
          ticketId: label.pieceId,
        }),
        pieceId: label.pieceId,
        printerName: result?.printerName || 'ng-piece-label',
      })),
    );
    printed += accepted.length;
  }
  return printed;
}

async function printPackagingRows(rows: UnifiedNgRow[]) {
  const sliceBatchNos = rows.map((row) =>
    normalizeSliceBatchNo(row.sliceBatchNo),
  );
  if (sliceBatchNos.some((item) => !item))
    throw new Error('所选记录存在空片号，无法打印');
  let printed = 0;
  for (
    let start = 0;
    start < sliceBatchNos.length;
    start += PRINT_BATCH_MAX_COUNT
  ) {
    const requestedBatchNos = sliceBatchNos.slice(
      start,
      start + PRINT_BATCH_MAX_COUNT,
    );
    const labelResult = await getPackagingPieceLabels(requestedBatchNos);
    const labelMap = new Map(
      (labelResult.labels || []).map((label) => [
        normalizeSliceBatchNo(label.sliceBatchNo),
        label,
      ]),
    );
    if (
      (labelResult.failures || []).length > 0 ||
      requestedBatchNos.some((item) => !labelMap.has(item))
    ) {
      throw new Error(
        (labelResult.failures || [])[0] || '未获取到完整的包装片号标签数据',
      );
    }
    const labels = requestedBatchNos.map((item) => labelMap.get(item)!);
    const templateGroups = new Map<
      string,
      MesHcFinishedPackagingApi.PieceLabel[]
    >();
    labels.forEach((label) => {
      const templateCode = resolvePackagingPieceLabelTemplateCode(
        label.qualityStatus,
      );
      templateGroups.set(templateCode, [
        ...(templateGroups.get(templateCode) || []),
        label,
      ]);
    });
    for (const [templateCode, templateLabels] of templateGroups) {
      const printTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
      const payload = await buildPackagingPieceLabelPayload(
        templateLabels.map((label) => ({
          expiryDate: label.expiryDate,
          modelCode: label.modelCode,
          segmentBatchNo: label.segmentBatchNo,
          sliceBatchNo: label.sliceBatchNo,
          ticketId: packagingLabelTicketId(label),
        })),
        printTime,
        { templateCode },
      );
      const result = await sendPrint(payload, templateLabels.length);
      if (result?.success !== true && !Array.isArray(result?.itemResults)) {
        throw new Error(
          result?.message || '本机打印服务未确认标签进入打印队列',
        );
      }
      const accepted = templateLabels.filter((label) => {
        const item = result?.itemResults?.find(
          (candidate: any) =>
            candidate?.ticketId === packagingLabelTicketId(label),
        );
        return item
          ? item.success === true && item.accepted !== false
          : result?.success === true;
      });
      if (accepted.length === 0)
        throw new Error('本机打印服务未确认标签进入打印队列');
      await markPackagingPieceLabelsPrinted({
        items: accepted.map((label) => ({
          labelContentJson: printSnapshot(
            payload,
            packagingLabelTicketId(label),
            printTime,
            templateCode,
          ),
          printerName: result?.printerName || 'packaging-piece-label',
          sourceId: label.sourceId,
          sourceType: label.sourceType,
        })),
      });
      printed += accepted.length;
    }
  }
  return printed;
}

async function printSelectedRows() {
  if (selectedCount.value === 0) {
    message.warning('请先勾选需要打印的不合格品');
    return;
  }
  printLoading.value = true;
  try {
    const ngRows = selectedRows.value.filter(
      (row) => row.inventoryArea === 'NG_WAREHOUSE',
    );
    const packagingRows = selectedRows.value.filter(
      (row) => row.inventoryArea !== 'NG_WAREHOUSE',
    );
    const [ngCount, packagingCount] = await Promise.all([
      ngRows.length > 0 ? printNgWarehouseRows(ngRows) : Promise.resolve(0),
      packagingRows.length > 0
        ? printPackagingRows(packagingRows)
        : Promise.resolve(0),
    ]);
    clearSelectedRows();
    message.success(`已发送 ${ngCount + packagingCount} 张不合格品标签`);
  } catch (error: any) {
    Modal.warning({
      content: `未能完成标签打印：${error?.message || error}。请确认 HC-MES-PrintAgent 已启动且标签打印机配置正确。`,
      title: '不合格品标签打印失败',
    });
  } finally {
    printLoading.value = false;
  }
}

function openOutboundModal() {
  if (!outboundMode.value) {
    message.warning(OUTBOUND_UNAVAILABLE_MESSAGE);
    return;
  }
  outboundReason.value = '';
  outboundVisible.value = true;
}

async function confirmOutbound() {
  const reason = outboundReason.value.trim();
  if (!reason) {
    message.warning('请填写出库原因');
    return;
  }
  if (!outboundMode.value) {
    message.warning(OUTBOUND_UNAVAILABLE_MESSAGE);
    return;
  }
  outboundLoading.value = true;
  try {
    if (outboundMode.value === 'NG_WAREHOUSE') {
      await manualOutboundNgPieces({
        pieceIds: selectedRows.value
          .map((row) => row.ngPieceId!)
          .filter(Boolean),
        reason,
      });
    } else if (outboundMode.value === 'PACKAGED_PENDING_SHELF') {
      await manualOutboundPendingFgInboundPackages({
        packageIds: [
          ...new Set(
            selectedRows.value
              .map((row) => row.pendingPackageId)
              .filter((id): id is number => typeof id === 'number'),
          ),
        ],
        reason,
      });
    } else {
      await manualOutboundFgStocks({
        reason,
        stockIds: selectedRows.value.map((row) => row.stockId!).filter(Boolean),
      });
    }
    message.success(`已完成 ${selectedCount.value} 条不合格品出库`);
    outboundVisible.value = false;
    await handleSearch();
  } finally {
    outboundLoading.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console unified-ng-stock-board">
      <section class="prototype-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:package-search"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">{{ pageTitle }}</h2>
            <Tag class="console-title-tag" color="volcano">不合格品台账</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"
              ><span class="console-meta-label">{{
                isProcessScope ? '本页不合格品' : '不合格品总数'
              }}</span
              ><span class="console-meta-value">{{ totalQty }}</span
              ><span class="console-meta-sub">片</span></span
            >
            <span v-if="isProcessScope" class="console-meta-item">
              <span class="console-meta-label">符合条件记录</span>
              <span class="console-meta-value">{{ resultTotal }}</span>
              <span class="console-meta-sub">条</span>
            </span>
            <span v-if="scope === 'ALL'" class="console-meta-item"
              ><span class="console-meta-label">分切压槽库</span
              ><span class="console-meta-value">{{
                areaSummary.NG_WAREHOUSE
              }}</span></span
            >
            <span v-if="!isProcessScope" class="console-meta-item"
              ><span class="console-meta-label">待包装区</span
              ><span class="console-meta-value">{{
                areaSummary.WAIT_PACKAGING
              }}</span></span
            >
            <span v-if="!isProcessScope" class="console-meta-item"
              ><span class="console-meta-label">已包装待上架</span
              ><span class="console-meta-value">{{
                areaSummary.PACKAGED_PENDING_SHELF
              }}</span></span
            >
            <span v-if="!isProcessScope" class="console-meta-item"
              ><span class="console-meta-label">成品仓</span
              ><span class="console-meta-value">{{
                areaSummary.FG_WAREHOUSE
              }}</span></span
            >
          </div>
        </div>
        <div class="work-time-card">
          <span>{{ currentDateText }}</span>
          <b>{{ currentTimeText }}</b>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" /><span>查询</span>
          </button>
          <button
            v-access:code="[`${permissionPrefix}:print`]"
            class="action-tile"
            :disabled="selectedCount === 0 || printLoading"
            type="button"
            @click="printSelectedRows"
          >
            <IconifyIcon icon="lucide:printer" />
            <span>{{
              printLoading
                ? '打印中...'
                : `打印${selectedCount > 0 ? `（${selectedCount}）` : ''}`
            }}</span>
          </button>
          <button
            v-access:code="[`${permissionPrefix}:outbound`]"
            class="action-tile"
            :disabled="!outboundMode"
            type="button"
            @click="openOutboundModal"
          >
            <IconifyIcon icon="lucide:log-out" />
            <span
              >出库{{ selectedCount > 0 ? `（${selectedCount}）` : '' }}</span
            >
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" /><span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar unified-ng-filter-bar">
        <Input
          v-model:value="query.keyword"
          allow-clear
          class="unified-ng-keyword"
          :placeholder="
            isProcessScope
              ? '片号 / 批号 / 计划'
              : '片号 / 批号 / 计划 / 料号 / 型号'
          "
          @press-enter="handleSearch"
        />
        <Select
          v-if="!isProcessScope"
          v-model:value="query.area"
          :options="areaOptions"
          allow-clear
          class="unified-ng-control"
          placeholder="全部库存区域"
        />
        <Select
          v-model:value="query.status"
          :options="statusOptions"
          allow-clear
          class="unified-ng-control"
          placeholder="全部状态"
        />
        <Input
          v-model:value="query.materialCode"
          allow-clear
          class="unified-ng-control"
          placeholder="料号"
          @press-enter="handleSearch"
        />
        <Input
          v-model:value="query.modelCode"
          allow-clear
          class="unified-ng-control"
          placeholder="型号"
          @press-enter="handleSearch"
        />
        <DatePicker
          v-model:value="query.dateStart"
          class="unified-ng-control"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
        />
        <DatePicker
          v-model:value="query.dateEnd"
          class="unified-ng-control"
          placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <Button
          class="unified-ng-query-button"
          type="primary"
          @click="handleSearch"
        >
          <IconifyIcon icon="lucide:search" />查询
        </Button>
      </section>

      <section class="package-fg-grid-panel stock-ledger-panel">
        <Grid
          :table-title="`${pageTitle}台账${selectedCount > 0 ? `（已选 ${selectedCount} 条）` : ''}`"
        >
          <template #selection="{ row }">
            <input
              :checked="selectedRowMap.has(row.id)"
              type="checkbox"
              @change="toggleRowSelection(row, $event.target.checked)"
            />
          </template>
          <template #selectionHeader>
            <input
              :checked="isAllFilteredSelected()"
              type="checkbox"
              @change="togglePageSelection($event.target.checked)"
            />
          </template>
          <template #area="{ row }">
            <Tag :color="areaColor(row.inventoryArea)">
              {{ areaText(row.inventoryArea) }}
            </Tag>
          </template>
          <template #status="{ row }">
            <Tag :color="statusColor(row.status)">
              {{ statusText(row.status) }}
            </Tag>
          </template>
          <template #fqcResult="{ row }">
            <Tag
              v-if="row.fqcInspectionResult"
              :color="row.fqcInspectionResult === 'NG' ? 'red' : 'green'"
            >
              {{ row.fqcInspectionResult }} </Tag
            ><span v-else>-</span>
          </template>
          <template #coaResult="{ row }">
            <Tag
              v-if="row.coaInspectionResult"
              :color="row.coaInspectionResult === 'NG' ? 'red' : 'green'"
            >
              {{ row.coaInspectionResult }} </Tag
            ><span v-else>-</span>
          </template>
        </Grid>
      </section>

      <Modal
        v-model:open="outboundVisible"
        :confirm-loading="outboundLoading"
        title="不合格品批量出库"
        @ok="confirmOutbound"
      >
        <p>
          将出库
          {{ selectedCount }}
          条记录。待包装记录不可直接出库；待上架、冻结、已包装待上架和成品在库记录会按各自来源完成出库。
        </p>
        <Input.TextArea
          v-model:value="outboundReason"
          :maxlength="500"
          :rows="4"
          placeholder="请填写出库原因"
          show-count
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.unified-ng-stock-board {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.unified-ng-filter-bar {
  display: flex;
  box-sizing: border-box;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 8px 10px;
  overflow: visible;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.unified-ng-keyword {
  flex: 2 1 300px;
  min-width: 240px;
}

.unified-ng-control {
  flex: 1 1 150px;
  min-width: 130px;
}

.unified-ng-query-button {
  flex: 0 0 auto;
}

.stock-ledger-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.stock-ledger-panel :deep(.vben-vxe-grid) {
  display: flex;
  min-height: 0;
  flex: 1 1 0;
  flex-direction: column;
}

.stock-ledger-panel :deep(.vxe-grid) {
  display: flex;
  min-height: 0;
  height: 100%;
  flex-direction: column;
}

.stock-ledger-panel :deep(.vxe-grid--table-wrapper) {
  min-height: 0;
  flex: 1 1 auto;
}

.stock-ledger-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.stock-ledger-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

@media (max-width: 1440px) {
  .unified-ng-keyword {
    flex-basis: 260px;
  }

  .unified-ng-control {
    flex-basis: 140px;
  }
}

@media (max-width: 720px) {
  .unified-ng-keyword,
  .unified-ng-control {
    flex-basis: 100%;
  }
}
</style>
