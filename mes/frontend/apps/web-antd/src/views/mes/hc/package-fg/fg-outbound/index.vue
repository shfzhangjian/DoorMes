<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, h, onBeforeUnmount, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Tabs,
  TabPane,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid, VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  getShippingNotice,
  getShippingNoticePage,
  getShippingNoticePickCandidateSegmentPage,
  getShippingNoticePickCandidateSegmentPieceList,
  pickShippingNotice,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgOutbound' });

type ShippingNotice = MesHcFinishedPackagingApi.ShippingNotice;
type ShippingNoticeItem = MesHcFinishedPackagingApi.ShippingNoticeItem;
type ShippingNoticePickItem = MesHcFinishedPackagingApi.ShippingNoticePickItem;
type PickCandidate = MesHcFinishedPackagingApi.ShippingNoticePickCandidate;
type PickCandidateSegment =
  MesHcFinishedPackagingApi.ShippingNoticePickCandidateSegment;
type PickCandidateDisplayRow = PickCandidate & {
  __group?: boolean;
  children?: PickCandidateDisplayRow[];
  groupCount?: number;
  groupKey?: string;
  hasChild?: boolean;
  packagingDirectPieceCount?: number;
  rowKey?: string;
  sampleSliceBatchNos?: string[];
  segmentBatchNo?: string;
  warehousePieceCount?: number;
};
type ActualDeliveryRow = ShippingNoticePickItem & {
  _rowKey?: string;
  candidateKey?: string;
  candidateType?: MesHcFinishedPackagingApi.ShippingPickCandidateType;
  isDraft?: boolean;
  rowNo?: number;
};
type PickSourceRow = {
  candidateKey?: string;
  candidateType?: string;
  finishedStockId?: number;
  id?: number;
  modelCode?: string;
  pickSourceType?: string;
  sliceBatchNo?: string;
  sourceCutRoundReportId?: number;
  sourceInnerPackItemId?: number;
  stockId?: number;
};
type OutboundQuery = {
  customerName: string;
  keyword: string;
  materialCode: string;
  modelCode: string;
  noticeNo: string;
  orderNo: string;
  productType?: string;
  shippingDateEnd: Dayjs | null;
  shippingDateStart: Dayjs | null;
};
type QueryFieldKey = Exclude<keyof OutboundQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: { label: string; value: string }[];
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<keyof OutboundQuery, string>>;
};
type ShippingPickStatusTab = 'PENDING_PICKING' | 'PICK_COMPLETED';

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:fg-outbound:query-templates';

const userStore = useUserStore();
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统',
);
const nowText = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const clockTimer = window.setInterval(() => {
  nowText.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
}, 1000);

const query = reactive<OutboundQuery>({
  customerName: '',
  keyword: '',
  materialCode: '',
  modelCode: '',
  noticeNo: '',
  orderNo: '',
  productType: undefined,
  shippingDateEnd: null,
  shippingDateStart: null,
});
const activeStatusTab = ref<ShippingPickStatusTab>('PENDING_PICKING');
const advancedQueryVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());
const notices = ref<ShippingNotice[]>([]);
const noticeTotal = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);

const detailVisible = ref(false);
const detailLoading = ref(false);
const activeDetailTab = ref('pick');
const detailMaximized = ref(false);
const currentNotice = ref<ShippingNotice>();
const actualRows = ref<ActualDeliveryRow[]>([]);
const pickCandidateVisible = ref(false);
const pickCandidateLoading = ref(false);
const pickCandidateRows = ref<PickCandidateDisplayRow[]>([]);
const pickCandidateTotal = ref(0);
const pickCandidatePageNo = ref(1);
const pickCandidatePageSize = ref(20);
const pickSegmentSelecting = ref(false);
const pickSelectedRowKeys = ref<string[]>([]);
const pickSelectedRows = ref<PickCandidate[]>([]);
const pickSearch = reactive({
  modelCode: '',
  sliceBatchNo: '',
});

const currentDateText = computed(() => nowText.value.split(' ')[0] || '-');
const currentTimeText = computed(() => nowText.value.split(' ')[1] || '-');
const pendingCount = computed(
  () => notices.value.filter(isNoticePendingPicking).length,
);
const completedCount = computed(
  () => notices.value.filter(isNoticePickCompleted).length,
);
const detailItems = computed(() => currentNotice.value?.items || []);
const planRows = computed(() =>
  detailItems.value.map((item, index) => ({ ...item, rowNo: index + 1 })),
);
const detailQtyTotal = computed(
  () =>
    actualRows.value.filter((item) => item.lockStatus !== 'CANCELLED').length,
);
const canEditPick = computed(() => isNoticePendingPicking(currentNotice.value));
const pickDraftRows = computed(() =>
  actualRows.value.filter(
    (item) =>
      item.isDraft &&
      (isPackagingDirect(item)
        ? hasSinglePackagingDirectSource(item)
        : !!item.finishedStockId),
  ),
);
const pickedCandidateKeys = computed(
  () =>
    new Set(
      actualRows.value
        .map((item) => getPickRowCandidateKey(item))
        .filter(Boolean),
    ),
);
const requiredShipQty = computed(() =>
  Number(
    currentNotice.value?.requiredShipQty || currentNotice.value?.noticeQty || 0,
  ),
);
const pickRemainingQty = computed(() =>
  Math.max(0, requiredShipQty.value - detailQtyTotal.value),
);
const pickPreviewQty = computed(
  () => detailQtyTotal.value + pickSelectedRows.value.length,
);
const pickRequirementTags = computed(() => {
  const map = new Map<
    string,
    { internalItemCode?: string; internalModelCode?: string; qty: number }
  >();
  detailItems.value.forEach((item) => {
    const internalModelCode =
      item.internalModelCode || item.customerModelCode || item.modelCode || '-';
    const internalItemCode =
      item.internalItemCode || item.customerSliceBatchNo || '-';
    const key = `${internalModelCode}||${internalItemCode}`;
    const current = map.get(key) || {
      internalItemCode,
      internalModelCode,
      qty: 0,
    };
    current.qty += Number(item.lockedQty || item.actualShipQty || 1);
    map.set(key, current);
  });
  return [...map.values()];
});
const productTypeOptions = [
  { label: '量产', value: 'MASS' },
  { label: '样品', value: 'SAMPLE' },
];
const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'noticeNo', label: '需求单号' },
  { key: 'customerName', label: '发货客户' },
  { key: 'productType', label: '类型', options: productTypeOptions },
  { key: 'modelCode', label: '型号' },
  { key: 'materialCode', label: '料号' },
  { key: 'orderNo', label: '订单编号' },
  { key: 'shippingDateStart', label: '发货日期起', type: 'date' },
  { key: 'shippingDateEnd', label: '发货日期止', type: 'date' },
];
const activeQueryTags = computed(() =>
  queryFieldConfigs
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);
const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);

const noticeColumns = [
  {
    field: 'noticeNo',
    fixed: 'left',
    showOverflow: 'tooltip',
    title: '需求单号',
    width: 180,
  },
  {
    field: 'productType',
    slots: { default: 'productType' },
    title: '类型',
    width: 90,
  },
  {
    field: 'externalProductModel', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue,
    showOverflow: 'tooltip',
    title: '外部型号',
    width: 130,
  },
  {
    field: 'externalProductCode', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue,
    showOverflow: 'tooltip',
    title: '外部编码',
    width: 130,
  },
  { field: 'orderNo', showOverflow: 'tooltip', title: '订单编号', width: 160 },
  {
    field: 'erpOrderNo',
    showOverflow: 'tooltip',
    title: 'ERP订单号',
    width: 150,
  },
  {
    field: 'shippingTime',
    slots: { default: 'shippingTime' },
    title: '发货时间',
    width: 160,
  },
  {
    field: 'noticeStatus',
    slots: { default: 'noticeStatus' },
    title: '状态',
    width: 100,
  },
  {
    field: 'recorderName',
    showOverflow: 'tooltip',
    title: '记录人',
    width: 100,
  },
  {
    field: 'recorderTime',
    slots: { default: 'recorderTime' },
    title: '记录时间',
    width: 160,
  },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 110,
  },
];

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatShippingTime(row?: ShippingNotice) {
  return formatDateTime(row?.shippingTime || row?.recorderTime);
}

function productTypeText(value?: string) {
  const map: Record<string, string> = {
    MASS: '量产',
    SAMPLE: '样品',
    RND: '研发',
  };
  return map[value || ''] || value || '-';
}

function requiredPickQty(notice?: ShippingNotice) {
  return Number(notice?.requiredShipQty || notice?.noticeQty || 0);
}

function isNoticePickCompleted(notice?: ShippingNotice) {
  const noticeStatus = notice?.noticeStatus || '';
  if (
    [
      'SHIP_CONFIRMED',
      'INSPECTED',
      'PACKAGED',
      'LOCKED',
      'OUTBOUND',
      'SHIPPED',
      'CLOSED',
    ].includes(noticeStatus)
  ) {
    return true;
  }
  return (
    noticeStatus === 'PICKED' &&
    Number(notice?.lockedQty || 0) >= requiredPickQty(notice)
  );
}

function isNoticePendingPicking(notice?: ShippingNotice) {
  const noticeStatus = notice?.noticeStatus || '';
  return (
    noticeStatus === 'SUBMITTED' ||
    (noticeStatus === 'PICKED' && !isNoticePickCompleted(notice))
  );
}

function noticeStatusText(notice?: ShippingNotice) {
  return isNoticePickCompleted(notice) ? '已完成' : '待配货';
}

function noticeStatusColor(notice?: ShippingNotice) {
  return isNoticePickCompleted(notice) ? 'green' : 'gold';
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PICK_COMPLETED: '已完成',
    PENDING_PICKING: '待配货',
    SUBMITTED: '待配货',
  };
  return map[status || ''] || status || '-';
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'green';
  if (value === 'NG') return 'red';
  return 'default';
}

function pickSourceType(row?: PickSourceRow) {
  const sourceType = String(row?.pickSourceType || row?.candidateType || '')
    .trim()
    .toUpperCase();
  if (
    sourceType === 'PACKAGING_DIRECT' ||
    (!sourceType && (row?.sourceInnerPackItemId || row?.sourceCutRoundReportId))
  ) {
    return 'PACKAGING_DIRECT';
  }
  return 'WAREHOUSE_STOCK';
}

function isPackagingDirect(row?: PickSourceRow) {
  return pickSourceType(row) === 'PACKAGING_DIRECT';
}

function pickSourceText(row?: PickSourceRow) {
  return isPackagingDirect(row) ? '包装直发' : '在库配货';
}

function pickStageText(row?: PickSourceRow) {
  if (!isPackagingDirect(row)) return '已上架库存';
  if (row?.sourceCutRoundReportId) return '待包装直发';
  if (row?.sourceInnerPackItemId) return '已包装未上架';
  return '-';
}

function hasSinglePackagingDirectSource(row?: PickSourceRow) {
  return (
    Boolean(row?.sourceInnerPackItemId) !== Boolean(row?.sourceCutRoundReportId)
  );
}

function getCandidateKey(row: PickCandidate) {
  const responseKey = String(row.candidateKey || '').trim();
  if (responseKey) return responseKey;
  if (isPackagingDirect(row)) {
    if (row.sourceCutRoundReportId) {
      return `PACKAGING_DIRECT:CUT_ROUND:${row.sourceCutRoundReportId}`;
    }
    if (row.sourceInnerPackItemId) {
      return `PACKAGING_DIRECT:INNER_PACK:${row.sourceInnerPackItemId}`;
    }
    return `PACKAGING_DIRECT:${row.modelCode || ''}:${row.sliceBatchNo || ''}`;
  }
  return `WAREHOUSE_STOCK:${row.stockId ?? row.id ?? `${row.modelCode || ''}:${row.sliceBatchNo || ''}`}`;
}

function getPickRowCandidateKey(row: ActualDeliveryRow) {
  const responseKey = String(row.candidateKey || '').trim();
  if (responseKey) return responseKey;
  if (isPackagingDirect(row)) {
    if (row.sourceCutRoundReportId) {
      return `PACKAGING_DIRECT:CUT_ROUND:${row.sourceCutRoundReportId}`;
    }
    return row.sourceInnerPackItemId
      ? `PACKAGING_DIRECT:INNER_PACK:${row.sourceInnerPackItemId}`
      : '';
  }
  return row.finishedStockId ? `WAREHOUSE_STOCK:${row.finishedStockId}` : '';
}

function normalizePickCandidate(row: PickCandidate): PickCandidate {
  const candidateType = pickSourceType(row);
  return {
    ...row,
    candidateKey: getCandidateKey({ ...row, candidateType }),
    candidateType,
    sourceCutRoundReportId:
      candidateType === 'PACKAGING_DIRECT'
        ? row.sourceCutRoundReportId
        : undefined,
    sourceInnerPackItemId:
      candidateType === 'PACKAGING_DIRECT'
        ? row.sourceInnerPackItemId
        : undefined,
    stockId:
      candidateType === 'WAREHOUSE_STOCK' ? (row.stockId ?? row.id) : undefined,
  };
}

function normalizePickSegmentNo(value?: string) {
  return String(value || '未识别分段批号')
    .trim()
    .toUpperCase();
}

function isPickCandidateGroup(
  row?: PickCandidateDisplayRow,
): row is PickCandidateDisplayRow {
  return Boolean(row?.__group);
}

function toPickCandidateSegmentRow(
  row: PickCandidateSegment,
  index: number,
): PickCandidateDisplayRow {
  const segmentBatchNo = String(
    row.segmentBatchNo || '未识别分段批号',
  ).trim();
  return {
    __group: true,
    groupCount: Number(row.totalPieceCount || 0),
    groupKey: normalizePickSegmentNo(segmentBatchNo),
    hasChild: Number(row.totalPieceCount || 0) > 0,
    materialCode: segmentBatchNo,
    modelCode: row.modelCode,
    packagingDirectPieceCount: Number(row.packagingDirectPieceCount || 0),
    rowKey: `segment-${normalizePickSegmentNo(segmentBatchNo)}-${index}`,
    sampleSliceBatchNos: row.sampleSliceBatchNos || [],
    segmentBatchNo,
    warehousePieceCount: Number(row.warehousePieceCount || 0),
  };
}

function toPickCandidatePieceRow(row: PickCandidate): PickCandidateDisplayRow {
  const candidate = normalizePickCandidate(row);
  return {
    ...candidate,
    __group: false,
    rowKey: getCandidateKey(candidate),
  };
}

function formatPickSegmentSamples(row: PickCandidateDisplayRow) {
  const samples = (row.sampleSliceBatchNos || []).filter(Boolean);
  return samples.length > 0 ? `示例：${samples.join('、')}` : '展开查看片号';
}

function selectedPickSegmentCount(row: PickCandidateDisplayRow) {
  const segmentKey = normalizePickSegmentNo(
    row.segmentBatchNo || row.materialCode,
  );
  return pickSelectedRows.value.filter(
    (item) => normalizePickSegmentNo(item.materialCode) === segmentKey,
  ).length;
}

function isPickSegmentFullySelected(row: PickCandidateDisplayRow) {
  const total = Number(row.groupCount || 0);
  return total > 0 && selectedPickSegmentCount(row) >= total;
}

function isPickSegmentDisabled(row: PickCandidateDisplayRow) {
  if (pickSegmentSelecting.value || Number(row.groupCount || 0) <= 0) {
    return true;
  }
  if (isPickSegmentFullySelected(row)) return false;
  const appendCount = Math.max(
    0,
    Number(row.groupCount || 0) - selectedPickSegmentCount(row),
  );
  return pickSelectedRows.value.length + appendCount > pickRemainingQty.value;
}

function pickSegmentActionText(row: PickCandidateDisplayRow) {
  if (isPickSegmentFullySelected(row)) return '取消整段';
  if (isPickSegmentDisabled(row) && !pickSegmentSelecting.value) {
    return '展开选择';
  }
  return '选整段';
}

async function loadPickCandidateSegmentChildren({
  row,
}: {
  row: PickCandidateDisplayRow;
}) {
  if (!isPickCandidateGroup(row)) return [];
  const result = await getShippingNoticePickCandidateSegmentPieceList({
    ...buildPickCandidateQuery(),
    segmentBatchNo: row.segmentBatchNo || row.materialCode,
  });
  const children = (result || []).map(toPickCandidatePieceRow);
  row.children = children;
  return children;
}

async function togglePickSegment(row: PickCandidateDisplayRow) {
  const segmentKey = normalizePickSegmentNo(
    row.segmentBatchNo || row.materialCode,
  );
  if (isPickSegmentFullySelected(row)) {
    pickSelectedRows.value = pickSelectedRows.value.filter(
      (item) => normalizePickSegmentNo(item.materialCode) !== segmentKey,
    );
    pickSelectedRowKeys.value = pickSelectedRows.value.map(getCandidateKey);
    return;
  }
  if (isPickSegmentDisabled(row)) {
    message.warning(
      `本段共 ${row.groupCount || 0} 片，超过本次剩余可选数量，请展开后按片选择`,
    );
    return;
  }
  pickSegmentSelecting.value = true;
  try {
    const children =
      row.children || (await loadPickCandidateSegmentChildren({ row }));
    const exists = new Set(pickSelectedRowKeys.value);
    const appendRows = children.filter(
      (item) =>
        !isPickCandidateInvalid(item) &&
        !pickedCandidateKeys.value.has(getCandidateKey(item)) &&
        !exists.has(getCandidateKey(item)),
    );
    if (
      pickSelectedRows.value.length + appendRows.length >
      pickRemainingQty.value
    ) {
      message.warning('本段可选片数已变化并超过剩余数量，请展开后按片选择');
      return;
    }
    pickSelectedRows.value = [...pickSelectedRows.value, ...appendRows];
    pickSelectedRowKeys.value = pickSelectedRows.value.map(getCandidateKey);
  } finally {
    pickSegmentSelecting.value = false;
  }
}

const pickCandidateTreeConfig = {
  children: 'children',
  hasChild: 'hasChild',
  lazy: true,
  loadMethod: loadPickCandidateSegmentChildren,
  reserve: true,
  showLine: true,
};

function pickCandidateRowClassName({ row }: { row: PickCandidateDisplayRow }) {
  return isPickCandidateGroup(row) ? 'outbound-pick-segment-row' : '';
}

function locationPart(
  row: PickCandidate | ShippingNoticeItem | ShippingNoticePickItem | undefined,
  key: 'area' | 'layer' | 'rack' | 'tray',
) {
  if (isPackagingDirect(row)) return '-';
  const parts = String(row?.locationCode || '').split('-');
  const map = {
    area: parts[2] || '-',
    layer: parts[1] || '-',
    rack: parts[0] || '-',
    tray: parts[3] || '-',
  };
  return map[key] || '-';
}

function locationText(row?: PickSourceRow & { locationCode?: string }) {
  return isPackagingDirect(row) ? '无需上架' : row?.locationCode || '-';
}

function isPickCandidateSelected(row: PickCandidate) {
  return pickSelectedRowKeys.value.includes(getCandidateKey(row));
}

function isPickCandidateInvalid(row: PickCandidate) {
  return isPackagingDirect(row)
    ? !hasSinglePackagingDirectSource(row)
    : !(row.stockId ?? row.id);
}

function isPickCandidateDisabled(row: PickCandidate) {
  return (
    isPickCandidateInvalid(row) ||
    pickedCandidateKeys.value.has(getCandidateKey(row)) ||
    (!isPickCandidateSelected(row) &&
      pickSelectedRows.value.length >= pickRemainingQty.value)
  );
}

function pickCandidateActionText(row: PickCandidate) {
  if (isPickCandidateInvalid(row)) return '不可选';
  if (pickedCandidateKeys.value.has(getCandidateKey(row))) return '已带回';
  if (
    !isPickCandidateSelected(row) &&
    pickSelectedRows.value.length >= pickRemainingQty.value
  ) {
    return '数量已满';
  }
  return isPickCandidateSelected(row) ? '已选' : '选择';
}

function togglePickCandidate(row: PickCandidate) {
  const candidateKey = getCandidateKey(row);
  if (isPickCandidateSelected(row)) {
    pickSelectedRowKeys.value = pickSelectedRowKeys.value.filter(
      (key) => key !== candidateKey,
    );
    pickSelectedRows.value = pickSelectedRows.value.filter(
      (item) => getCandidateKey(item) !== candidateKey,
    );
    return;
  }
  if (isPickCandidateDisabled(row)) {
    if (pickRemainingQty.value <= pickSelectedRows.value.length) {
      message.warning(`配货数量必须与要求数量一致，最多可选择 ${pickRemainingQty.value} 片`);
    }
    return;
  }
  pickSelectedRowKeys.value = [...pickSelectedRowKeys.value, candidateKey];
  pickSelectedRows.value = [
    ...pickSelectedRows.value.filter(
      (item) => getCandidateKey(item) !== candidateKey,
    ),
    row,
  ];
}

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(
      QUERY_TEMPLATE_STORAGE_KEY,
    );
    const parsedTemplates = rawTemplates ? JSON.parse(rawTemplates) : [];
    if (!Array.isArray(parsedTemplates)) return [];
    return parsedTemplates
      .filter((item) => item?.name && item?.values)
      .map((item) => ({
        name: String(item.name),
        values: item.values,
      })) as QueryTemplate[];
  } catch {
    return [];
  }
}

function persistQueryTemplates() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(
    QUERY_TEMPLATE_STORAGE_KEY,
    JSON.stringify(queryTemplates.value),
  );
}

function getQueryFieldConfig(key: QueryFieldKey) {
  return queryFieldConfigs.find((item) => item.key === key);
}

function getQueryFieldText(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  const config = getQueryFieldConfig(key);
  if (config?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  const text = String(value).trim();
  if (!text) return '';
  return config?.options?.find((item) => item.value === text)?.label || text;
}

function getQueryFieldRawValue(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  if (getQueryFieldConfig(key)?.type === 'date')
    return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  switch (key) {
    case 'customerName': {
      query.customerName = value || '';
      break;
    }
    case 'materialCode': {
      query.materialCode = value || '';
      break;
    }
    case 'modelCode': {
      query.modelCode = value || '';
      break;
    }
    case 'noticeNo': {
      query.noticeNo = value || '';
      break;
    }
    case 'orderNo': {
      query.orderNo = value || '';
      break;
    }
    case 'productType': {
      query.productType = value || undefined;
      break;
    }
    case 'shippingDateEnd': {
      query.shippingDateEnd = value ? dayjs(value) : null;
      break;
    }
    case 'shippingDateStart': {
      query.shippingDateStart = value ? dayjs(value) : null;
      break;
    }
  }
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
  selectedTemplateName.value = undefined;
  queryTemplateName.value = '';
}

function snapshotAdvancedQuery() {
  const values: QueryTemplate['values'] = {};
  if (query.keyword.trim()) values.keyword = query.keyword.trim();
  queryFieldConfigs.forEach((config) => {
    const value = getQueryFieldRawValue(config.key);
    if (value) values[config.key] = value;
  });
  return values;
}

function openAdvancedQuery() {
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  searchNotices();
}

function removeQueryCondition(key: QueryFieldKey) {
  setQueryFieldValue(key);
  selectedTemplateName.value = undefined;
  searchNotices();
}

function saveQueryTemplate() {
  const name = queryTemplateName.value.trim();
  if (!name) {
    message.warning('请填写查询模板名称');
    return;
  }
  const values = snapshotAdvancedQuery();
  if (Object.keys(values).length === 0) {
    message.warning('请先填写查询条件');
    return;
  }
  const nextTemplates = queryTemplates.value.filter(
    (item) => item.name !== name,
  );
  nextTemplates.unshift({ name, values });
  queryTemplates.value = nextTemplates.slice(0, 20);
  selectedTemplateName.value = name;
  persistQueryTemplates();
  message.success('查询模板已保存');
}

function loadQueryTemplate(name?: string) {
  if (!name) return;
  const template = queryTemplates.value.find((item) => item.name === name);
  if (!template) return;
  query.keyword = '';
  clearAdvancedQueryValues();
  Object.entries(template.values).forEach(([key, value]) => {
    if (key === 'keyword') {
      query.keyword = value || '';
      return;
    }
    setQueryFieldValue(key as QueryFieldKey, value);
  });
  queryTemplateName.value = template.name;
  searchNotices();
}

function handleLoadQueryTemplate(value?: string | number) {
  loadQueryTemplate(value ? String(value) : undefined);
}

function buildQuery(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, any> = {
    noticeStatus: activeStatusTab.value,
    pageNo: page?.currentPage || pageNo.value,
    pageSize: page?.pageSize || pageSize.value,
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  if (query.noticeNo.trim()) params.noticeNo = query.noticeNo.trim();
  if (query.customerName.trim())
    params.customerName = query.customerName.trim();
  if (query.productType) params.productType = query.productType;
  if (query.materialCode.trim())
    params.materialCode = query.materialCode.trim();
  if (query.modelCode.trim()) params.modelCode = query.modelCode.trim();
  if (query.orderNo.trim()) params.orderNo = query.orderNo.trim();
  if (query.shippingDateStart)
    params.shippingDateStart = query.shippingDateStart.format('YYYY-MM-DD');
  if (query.shippingDateEnd)
    params.shippingDateEnd = query.shippingDateEnd.format('YYYY-MM-DD');
  return params;
}

async function queryNoticePage(page?: {
  currentPage?: number;
  pageSize?: number;
}) {
  pageNo.value = page?.currentPage || pageNo.value;
  pageSize.value = page?.pageSize || pageSize.value;
  const result = await getShippingNoticePage(buildQuery(page));
  notices.value = result.list || [];
  noticeTotal.value = Number(result.total || 0);
  return result;
}

async function searchNotices() {
  pageNo.value = 1;
  await noticeGridApi.query();
}

async function switchStatusTab(status: ShippingPickStatusTab) {
  activeStatusTab.value = status;
  await searchNotices();
}

function resetQuery() {
  query.keyword = '';
  clearAdvancedQuery();
  searchNotices();
}

function toPickRows(items: ShippingNoticePickItem[]) {
  return items
    .filter((item) => item.lockStatus !== 'CANCELLED')
    .map((item, index) => ({
      ...item,
      _rowKey: `PICK:${item.id}`,
      actualSliceBatchNo: item.actualSliceBatchNo || '',
      rowNo: index + 1,
    }));
}

function toDraftPickRow(stock: PickCandidate): ActualDeliveryRow {
  const candidateKey = getCandidateKey(stock);
  const candidateType = pickSourceType(stock);
  const sourceCutRoundReportId =
    candidateType === 'PACKAGING_DIRECT'
      ? stock.sourceCutRoundReportId
      : undefined;
  const sourceInnerPackItemId =
    candidateType === 'PACKAGING_DIRECT'
      ? stock.sourceInnerPackItemId
      : undefined;
  const stockId =
    candidateType === 'WAREHOUSE_STOCK'
      ? (stock.stockId ?? stock.id)
      : undefined;
  return {
    _rowKey: `DRAFT:${candidateKey}`,
    actualShipQty: 1,
    actualSliceBatchNo: stock.sliceBatchNo || '',
    batchNo: stock.batchNo,
    candidateKey,
    candidateType,
    finishedStockId: stockId,
    id: -(
      stockId ??
      sourceInnerPackItemId ??
      sourceCutRoundReportId ??
      actualRows.value.length + 1
    ),
    inboundNo: stock.inboundNo,
    inboundTime: stock.inboundTime,
    innerUnitNo: stock.innerUnitNo,
    isDraft: true,
    locationCode: stock.locationCode,
    locationName: stock.locationName,
    lockName: currentUserName.value,
    lockStatus: 'PICKED',
    materialCode: stock.materialCode,
    materialName: stock.materialName,
    modelCode: stock.modelCode,
    packageNo: stock.packageNo,
    pickSourceType: candidateType,
    productSize: stock.productSize,
    qualityStatus: stock.inspectionResult || stock.qualityStatus,
    rowNo: actualRows.value.length + 1,
    sliceBatchNo: stock.sliceBatchNo,
    sourceCutRoundReportId,
    sourceInnerPackItemId,
    stockNo: stock.stockNo,
    stockQty: stock.qty,
    warehouseCode: stock.warehouseCode,
    warehouseName: stock.warehouseName,
  };
}

async function openDelivery(row: ShippingNotice) {
  detailVisible.value = true;
  activeDetailTab.value = 'pick';
  detailMaximized.value = false;
  detailLoading.value = true;
  try {
    currentNotice.value = await getShippingNotice(row.id);
    actualRows.value = toPickRows(currentNotice.value.pickItems || []);
  } finally {
    detailLoading.value = false;
  }
}

async function refreshDetail() {
  if (!currentNotice.value?.id) return;
  currentNotice.value = await getShippingNotice(currentNotice.value.id);
  actualRows.value = toPickRows(currentNotice.value.pickItems || []);
}

function buildPickCandidateQuery() {
  const params: Record<string, any> = {
    includePackagingDirect: true,
    noticeId: currentNotice.value?.id,
    pageNo: pickCandidatePageNo.value,
    pageSize: pickCandidatePageSize.value,
  };
  if (pickSearch.sliceBatchNo.trim()) {
    params.sliceBatchNo = pickSearch.sliceBatchNo.trim();
  }
  if (pickSearch.modelCode.trim()) {
    params.modelCode = pickSearch.modelCode.trim();
  }
  return params;
}

async function fetchPickCandidates(reset = false) {
  if (!currentNotice.value?.id) return;
  if (reset) pickCandidatePageNo.value = 1;
  pickCandidateLoading.value = true;
  try {
    const result = await getShippingNoticePickCandidateSegmentPage(
      buildPickCandidateQuery(),
    );
    pickCandidateRows.value = (result.list || []).map((item, index) =>
      toPickCandidateSegmentRow(item, index),
    );
    pickCandidateTotal.value = Number(result.total || 0);
  } finally {
    pickCandidateLoading.value = false;
  }
}

async function changePickCandidatePage(page: number, size: number) {
  pickCandidatePageNo.value = page;
  pickCandidatePageSize.value = size;
  await fetchPickCandidates();
}

async function openPickCandidate() {
  if (!currentNotice.value?.id) {
    message.warning('请先打开发货需求单');
    return;
  }
  if (!canEditPick.value) {
    message.warning('当前需求单已进入出货确认或后续流程，不能继续配货');
    return;
  }
  if (requiredShipQty.value <= 0) {
    message.warning('当前发货需求单未维护有效要求数量，不能配货');
    return;
  }
  if (pickRemainingQty.value <= 0) {
    message.warning('当前配货数量已达到要求，不能继续选择');
    return;
  }
  pickSearch.modelCode = '';
  pickSearch.sliceBatchNo = '';
  pickCandidateRows.value = [];
  pickCandidateTotal.value = 0;
  pickSelectedRowKeys.value = [];
  pickSelectedRows.value = [];
  pickSegmentSelecting.value = false;
  pickCandidateVisible.value = true;
  await fetchPickCandidates(true);
}

function appendPickCandidateRows() {
  if (pickSelectedRows.value.length === 0) {
    message.warning('请选择待配货片号');
    return;
  }
  const exists = pickedCandidateKeys.value;
  const appendRows = pickSelectedRows.value
    .filter((stock) => !exists.has(getCandidateKey(stock)))
    .map((stock) => toDraftPickRow(stock));
  if (appendRows.length === 0) {
    message.warning('选择的片号已在配货列表中');
    return;
  }
  if (detailQtyTotal.value + appendRows.length > requiredShipQty.value) {
    message.warning(
      `配货数量不能超过要求数量，当前 ${detailQtyTotal.value} / 要求 ${requiredShipQty.value}`,
    );
    return;
  }
  actualRows.value = [...actualRows.value, ...appendRows].map(
    (item, index) => ({ ...item, rowNo: index + 1 }),
  );
  pickCandidateVisible.value = false;
  message.success(`已加入配货清单 ${appendRows.length} 片，刷新页面前不会落库`);
}

async function doPickCandidate() {
  if (!currentNotice.value?.id) return;
  const rows = pickDraftRows.value;
  if (rows.length === 0) {
    message.warning('没有待锁定的新增配货片号');
    return;
  }
  currentNotice.value = await pickShippingNotice({
    confirmPicked: true,
    items: rows.map((row) =>
      isPackagingDirect(row)
        ? row.sourceCutRoundReportId
          ? {
              candidateKey: getPickRowCandidateKey(row),
              candidateType: 'PACKAGING_DIRECT' as const,
              sourceCutRoundReportId: row.sourceCutRoundReportId,
            }
          : {
              candidateKey: getPickRowCandidateKey(row),
              candidateType: 'PACKAGING_DIRECT' as const,
              sourceInnerPackItemId: row.sourceInnerPackItemId!,
            }
        : {
            candidateKey: getPickRowCandidateKey(row),
            candidateType: 'WAREHOUSE_STOCK' as const,
            stockId: row.finishedStockId!,
          },
    ),
    noticeId: currentNotice.value.id,
    operatorName: currentUserName.value,
  });
  actualRows.value = toPickRows(currentNotice.value.pickItems || []);
  message.success('配货已锁定，待出货管理确认后推送发货成品检验');
  await searchNotices();
}

function submitPickCandidate() {
  if (pickDraftRows.value.length === 0) {
    message.warning('请先选择待配货片号并加入清单');
    return;
  }
  if (detailQtyTotal.value !== requiredShipQty.value) {
    message.warning(
      `配货数量必须与要求数量一致，当前 ${detailQtyTotal.value} / 要求 ${requiredShipQty.value}`,
    );
    return;
  }
  Modal.confirm({
    cancelText: '取消',
    content: h('div', { class: 'return-pick-confirm' }, [
      h(
        'div',
        '确认后系统会锁定本次选择的发货片；在库成品将下架锁定，包装直发成品无需上架。发货成品检验不会在此步骤生成。',
      ),
      h(
        'div',
        { class: 'return-pick-confirm-tip' },
        '提醒：如同一包装单为两片一包且本次只配货其中一片，未配货片会自动回到成品包装报工的待包装片，需重新包装后再配货。',
      ),
    ]),
    okText: '确认配货',
    onOk: () => doPickCandidate(),
    title: '确认配货？',
  });
}

const [NoticeGrid, noticeGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: noticeColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryNoticePage(page),
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
  } as VxeTableGridOptions<ShippingNotice>,
});

onBeforeUnmount(() => window.clearInterval(clockTimer));
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console fg-outbound-board">
      <section class="prototype-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:truck"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">发货配货</h2>
            <Tag class="console-title-tag" color="processing">需求单配货</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">当前状态</span>
              <span class="console-meta-value">{{
                statusText(activeStatusTab)
              }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页待配货</span>
              <span class="console-meta-value">{{ pendingCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页已完成</span>
              <span class="console-meta-value">{{ completedCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">需求单</span>
              <span class="console-meta-value">{{ noticeTotal }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="searchNotices">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar outbound-query-panel">
        <div class="outbound-simple-query">
          <div class="outbound-status-tabs">
            <button
              :class="{ 'is-active': activeStatusTab === 'PENDING_PICKING' }"
              type="button"
              @click="switchStatusTab('PENDING_PICKING')"
            >
              待配货
            </button>
            <button
              :class="{ 'is-active': activeStatusTab === 'PICK_COMPLETED' }"
              type="button"
              @click="switchStatusTab('PICK_COMPLETED')"
            >
              已完成
            </button>
          </div>
          <label class="outbound-keyword-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="需求单号 / 订单编号 / ERP订单号 / 料号 / 型号 / 产品名称"
            @press-enter="searchNotices"
          />
          <Button type="primary" @click="searchNotices">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon
              ><IconifyIcon icon="lucide:sliders-horizontal"
            /></template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            :options="queryTemplateOptions"
            allow-clear
            class="outbound-query-template-select"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="outbound-query-tags">
          <span
            v-for="tag in activeQueryTags"
            :key="tag.key"
            class="outbound-query-tag"
          >
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">
              x
            </button>
          </span>
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="860px"
        wrap-class-name="shipping-advanced-query-modal"
      >
        <div class="outbound-advanced-query-body">
          <div class="outbound-advanced-query-grid">
            <div class="outbound-query-item">
              <label>需求单号</label>
              <Input
                v-model:value="query.noticeNo"
                allow-clear
                placeholder="需求单号"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="outbound-query-item">
              <label>发货客户</label>
              <Input
                v-model:value="query.customerName"
                allow-clear
                placeholder="发货客户"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="outbound-query-item">
              <label>类型</label>
              <Select
                v-model:value="query.productType"
                :options="productTypeOptions"
                allow-clear
                placeholder="类型"
              />
            </div>
            <div class="outbound-query-item">
              <label>型号</label>
              <Input
                v-model:value="query.modelCode"
                allow-clear
                placeholder="型号"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="outbound-query-item">
              <label>料号</label>
              <Input
                v-model:value="query.materialCode"
                allow-clear
                placeholder="料号"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="outbound-query-item">
              <label>订单编号</label>
              <Input
                v-model:value="query.orderNo"
                allow-clear
                placeholder="订单编号"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="outbound-query-item">
              <label>发货日期起</label>
              <DatePicker
                v-model:value="query.shippingDateStart"
                placeholder="发货日期起"
              />
            </div>
            <div class="outbound-query-item">
              <label>发货日期止</label>
              <DatePicker
                v-model:value="query.shippingDateEnd"
                placeholder="发货日期止"
              />
            </div>
          </div>
          <div class="outbound-template-row">
            <label>模板名称</label>
            <Input
              v-model:value="queryTemplateName"
              allow-clear
              placeholder="填写名称后可保存当前查询条件"
            />
            <Button @click="saveQueryTemplate">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存模板
            </Button>
          </div>
          <div class="outbound-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel outbound-grid-panel">
        <NoticeGrid table-title="发货需求单台账">
          <template #productType="{ row }">
            <Tag>{{ productTypeText(row.productType) }}</Tag>
          </template>
          <template #shippingTime="{ row }">{{
            formatShippingTime(row)
          }}</template>
          <template #noticeStatus="{ row }">
            <Tag :color="noticeStatusColor(row)">{{
              noticeStatusText(row)
            }}</Tag>
          </template>
          <template #recorderTime="{ row }">{{
            formatDateTime(row.recorderTime)
          }}</template>
          <template #action="{ row }">
            <Button size="small" type="link" @click="openDelivery(row)">
              {{ isNoticePendingPicking(row) ? '配货' : '查看' }}
            </Button>
          </template>
        </NoticeGrid>
      </section>

      <Modal
        v-model:open="detailVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal outbound-work-modal"
        @cancel="detailVisible = false"
      >
        <div
          class="report-modal-body inspection-task-modal-body outbound-detail-body"
          :class="{
            'is-loading': detailLoading,
            'shipping-detail-maximized-body': detailMaximized,
          }"
        >
          <div
            class="shipping-notice-form-top"
            :class="{ 'shipping-notice-form-top--compact': detailMaximized }"
          >
            <div class="shipping-notice-form-title-row">
              <span></span>
              <div class="inspection-form-title">发货配货单</div>
              <div class="shipping-notice-form-actions">
                <Button
                  :disabled="!canEditPick"
                  size="small"
                  type="primary"
                  @click="openPickCandidate"
                >
                  <template #icon
                    ><IconifyIcon icon="lucide:list-plus"
                  /></template>
                  配货选择
                </Button>
                <Button
                  :disabled="!canEditPick || pickDraftRows.length === 0"
                  size="small"
                  type="primary"
                  @click="submitPickCandidate"
                >
                  <template #icon
                    ><IconifyIcon icon="lucide:package-check"
                  /></template>
                  确认配货
                </Button>
                <Button
                  :loading="detailLoading"
                  size="small"
                  @click="refreshDetail"
                >
                  <template #icon
                    ><IconifyIcon icon="lucide:refresh-cw"
                  /></template>
                  刷新
                </Button>
                <Button size="small" @click="detailVisible = false"
                  >关闭</Button
                >
              </div>
            </div>
            <div class="shipping-form-sections">
              <fieldset class="shipping-form-fieldset">
                <legend>基本信息</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <template v-if="currentNotice?.productType === 'SAMPLE'">
                    <label>样品数量</label>
                    <div class="inspection-form-control">{{ currentNotice?.requiredShipQty || currentNotice?.noticeQty || 0 }}</div>
                  </template>
                  <label>需求单号</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.noticeNo || '-' }}
                  </div>
                  <label>类型</label>
                  <div class="inspection-form-control">
                    {{ productTypeText(currentNotice?.productType) }}
                  </div>
                  <label>ERP订单号</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.erpOrderNo || '-' }}
                  </div>
                  <label>发货时间</label>
                  <div class="inspection-form-control">
                    {{ formatShippingTime(currentNotice) }}
                  </div>
                  <label>配货片数</label>
                  <div class="inspection-form-control">
                    {{ detailQtyTotal }}
                  </div>
                  <label>状态</label>
                  <div class="inspection-form-control">
                    <Tag :color="noticeStatusColor(currentNotice)">{{
                      noticeStatusText(currentNotice)
                    }}</Tag>
                  </div>
                  <label>记录人</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.recorderName || '-' }}
                  </div>
                  <label>订单编号</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.orderNo || '-' }}
                  </div>
                  <label class="shipping-form-label--full-row">发货备注</label>
                  <div
                    class="inspection-form-control shipping-form-control--full-row"
                  >
                    {{ currentNotice?.remark || '-' }}
                  </div>
                </div>
              </fieldset>
              <fieldset v-if="currentNotice?.productType !== 'SAMPLE'" class="shipping-form-fieldset">
                <legend>
                  {{
                    currentNotice?.productType === 'SAMPLE'
                      ? '样品要求'
                      : '客户要求'
                  }}
                </legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>客户名称</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.customerName || '-' }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE'
                      ? '样品型号'
                      : '产品型号'
                  }}</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.externalProductModel || '-' }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE'
                      ? '样品批号'
                      : '产品编码'
                  }}</label>
                  <div class="inspection-form-control">
                    {{
                      currentNotice?.productType === 'SAMPLE'
                        ? currentNotice?.requiredBatchNo || '-'
                        : currentNotice?.externalProductCode || '-'
                    }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE'
                      ? '样品数量'
                      : '发货数量'
                  }}</label>
                  <div class="inspection-form-control">
                    {{
                      currentNotice?.requiredShipQty ||
                      currentNotice?.noticeQty ||
                      0
                    }}
                  </div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'"
                    >片号范围</label
                  >
                  <div
                    v-if="currentNotice?.productType !== 'SAMPLE'"
                    class="inspection-form-control"
                  >
                    {{ currentNotice?.requiredSliceRange || '-' }}
                  </div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'"
                    >生产批号</label
                  >
                  <div
                    v-if="currentNotice?.productType !== 'SAMPLE'"
                    class="inspection-form-control"
                  >
                    {{ currentNotice?.requiredBatchNo || '-' }}
                  </div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'"
                    >包装要求</label
                  >
                  <div
                    v-if="currentNotice?.productType !== 'SAMPLE'"
                    class="inspection-form-control shipping-form-control--span-2"
                  >
                    {{ currentNotice?.packingRequirement || '-' }}
                  </div>
                </div>
              </fieldset>
            </div>
          </div>

          <Tabs
            v-model:active-key="activeDetailTab"
            class="shipping-detail-tabs outbound-detail-tabs"
          >
            <template #rightExtra>
              <Button
                class="shipping-tab-maximize-btn"
                size="small"
                @click="detailMaximized = !detailMaximized"
              >
                <template #icon>
                  <IconifyIcon
                    :icon="
                      detailMaximized
                        ? 'lucide:minimize-2'
                        : 'lucide:maximize-2'
                    "
                  />
                </template>
                {{ detailMaximized ? '还原' : '最大化' }}
              </Button>
            </template>
            <TabPane key="pick" tab="配货领用">
              <div class="inspection-form-subtitle">
                <span>配货领用</span>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="actualRows"
                  auto-resize
                  border
                  height="100%"
                  row-id="_rowKey"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    field="rowNo"
                    fixed="left"
                    title="序号"
                    width="70"
                  />
                  <VxeColumn
                    field="modelCode"
                    fixed="left"
                    title="产品型号"
                    width="130"
                  />
                  <VxeColumn
                    field="actualSliceBatchNo"
                    fixed="left"
                    title="片号"
                    width="190"
                  />
                  <VxeColumn
                    field="pickSourceType"
                    title="配货来源"
                    width="110"
                  >
                    <template #default="{ row }">
                      <Tag :color="isPackagingDirect(row) ? 'cyan' : 'blue'">{{
                        pickSourceText(row)
                      }}</Tag>
                    </template>
                  </VxeColumn>
                  <VxeColumn field="pickStage" title="来源阶段" width="140">
                    <template #default="{ row }">{{
                      pickStageText(row)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="qualityStatus"
                    title="裁切成品检验"
                    width="140"
                  >
                    <template #default="{ row }"
                      ><Tag :color="qualityColor(row.qualityStatus)">{{
                        row.qualityStatus || '-'
                      }}</Tag></template
                    >
                  </VxeColumn>
                  <VxeColumn
                    field="shippingInspectionResult"
                    title="发货成品检验"
                    width="140"
                  >
                    <template #default="{ row }"
                      ><Tag
                        :color="qualityColor(row.shippingInspectionResult)"
                        >{{ row.shippingInspectionResult || '-' }}</Tag
                      ></template
                    >
                  </VxeColumn>
                  <VxeColumn field="lockStatus" title="配货状态" width="110">
                    <template #default="{ row }"
                      ><Tag :color="noticeStatusColor(currentNotice)">{{
                        row.isDraft
                          ? '临时清单'
                          : noticeStatusText(currentNotice)
                      }}</Tag></template
                    >
                  </VxeColumn>
                  <VxeColumn field="locationCode" title="货位编号" width="150">
                    <template #default="{ row }">{{
                      locationText(row)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn field="batchNo" title="生产批号" width="150" />
                  <VxeColumn field="inboundTime" title="入库时间" width="160">
                    <template #default="{ row }">{{
                      formatDateTime(row.inboundTime)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn field="lockName" title="配货人" width="110" />
                  <VxeColumn field="lockTime" title="配货时间" width="160">
                    <template #default="{ row }">{{
                      row.isDraft ? '-' : formatDateTime(row.lockTime)
                    }}</template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>

            <TabPane key="plan" :tab="currentNotice?.productType === 'SAMPLE' ? '样品执行明细' : '客户批号表'">
              <div class="inspection-form-subtitle">
                <span>客户批号表</span>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="planRows"
                  auto-resize
                  border
                  height="100%"
                  row-id="id"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    field="rowNo"
                    fixed="left"
                    title="序号"
                    width="70"
                  />
                  <VxeColumn
                    field="internalModelCode"
                    title="内部型号"
                    width="140"
                  />
                  <VxeColumn
                    field="internalItemCode"
                    title="内部编号"
                    width="170"
                  />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="customerProductBatchNo"
                    title="产品批号"
                    width="150"
                  />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="packageSliceNo"
                    title="包装片号"
                    width="170"
                  />
                  <VxeColumn
                    field="actualSliceBatchNo"
                    title="实际片号"
                    width="180"
                  />
                  <VxeColumn
                    field="shippingInspectionResult"
                    title="裁切成品检验"
                    width="140"
                  >
                    <template #default="{ row }"
                      ><Tag
                        :color="qualityColor(row.shippingInspectionResult)"
                        >{{ row.shippingInspectionResult || '-' }}</Tag
                      ></template
                    >
                  </VxeColumn>
                  <VxeColumn
                    field="shippingPackageName"
                    title="包装人"
                    width="110"
                  />
                  <VxeColumn
                    field="shippingPackageTime"
                    title="包装时间"
                    min-width="160"
                  >
                    <template #default="{ row }">{{
                      formatDateTime(row.shippingPackageTime)
                    }}</template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </Modal>

      <Modal
        v-model:open="pickCandidateVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal outbound-pick-work-modal"
        @cancel="pickCandidateVisible = false"
      >
        <div
          class="report-modal-body inspection-task-modal-body outbound-pick-candidate"
        >
          <fieldset
            class="erp-fieldset report-modal-toolbar inspection-form-sheet"
          >
            <legend>配货选择</legend>
            <div class="shipping-selector-title-row">
              <div></div>
              <div class="inspection-form-title">配货选择</div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="pickCandidateVisible = false"
                  >关闭</Button
                >
                <Button
                  size="small"
                  type="primary"
                  @click="appendPickCandidateRows"
                  >加入配货清单</Button
                >
              </div>
            </div>
            <div
              class="inspection-form-head shipping-selector-form-head outbound-pick-selector-form-head"
            >
              <label>产品型号</label>
              <div class="inspection-form-control">
                <Input
                  v-model:value="pickSearch.modelCode"
                  allow-clear
                  placeholder="产品型号"
                  @press-enter="fetchPickCandidates(true)"
                />
              </div>
              <label>片号</label>
              <div class="inspection-form-control">
                <Input
                  v-model:value="pickSearch.sliceBatchNo"
                  allow-clear
                  placeholder="输入或扫码片号"
                  @press-enter="fetchPickCandidates(true)"
                />
              </div>
              <label>要求数量</label>
              <div class="inspection-form-control">{{ requiredShipQty }}</div>
              <label>配货进度</label>
              <div class="inspection-form-control">
                {{ pickPreviewQty }} / {{ requiredShipQty }}
              </div>
              <label>{{ currentNotice?.productType === 'SAMPLE' ? '配货条件' : '客户要求' }}</label>
              <div
                class="inspection-form-control outbound-fixed-condition-cell"
              >
                <span
                  v-for="item in pickRequirementTags"
                  :key="`${item.internalModelCode}-${item.internalItemCode}`"
                  class="outbound-fixed-tag"
                >
                  {{ item.internalModelCode }} / {{ item.internalItemCode }}
                  <b>{{ item.qty }}</b>
                </span>
              </div>
            </div>
            <div class="shipping-selector-actions">
              <Button type="primary" @click="fetchPickCandidates(true)">
                <template #icon><IconifyIcon icon="lucide:search" /></template>
                查询可配货片
              </Button>
            </div>
          </fieldset>
          <div class="inspection-form-subtitle">
            <span>可配货分段批号（共 {{ pickCandidateTotal }} 段）</span>
            <em
              >展开分段批号可按片选择；整段片数不超过剩余数量时可整段选择。已勾选
              {{ pickSelectedRowKeys.length }} 条 / 本次还可选
              {{ pickRemainingQty }} 条</em
            >
          </div>
          <div
            class="inspection-task-detail-table shipping-vxe-table outbound-candidate-vxe"
          >
            <VxeTable
              :data="pickCandidateRows"
              :loading="pickCandidateLoading"
              auto-resize
              border
              height="100%"
              row-id="rowKey"
              :row-class-name="pickCandidateRowClassName"
              show-overflow
              size="small"
              stripe
              :tree-config="pickCandidateTreeConfig"
            >
              <VxeColumn
                field="select"
                fixed="left"
                title="选择"
                width="72"
                align="center"
              >
                <template #default="{ row }">
                  <Button
                    v-if="isPickCandidateGroup(row)"
                    :disabled="
                      pickSegmentSelecting || Number(row.groupCount || 0) <= 0
                    "
                    :type="
                      isPickSegmentFullySelected(row) ? 'primary' : 'default'
                    "
                    size="small"
                    @click="togglePickSegment(row)"
                  >
                    {{ pickSegmentActionText(row) }}
                  </Button>
                  <Button
                    v-else
                    :disabled="isPickCandidateDisabled(row)"
                    :type="isPickCandidateSelected(row) ? 'primary' : 'default'"
                    size="small"
                    @click="togglePickCandidate(row)"
                  >
                    {{ pickCandidateActionText(row) }}
                  </Button>
                </template>
              </VxeColumn>
              <VxeColumn
                field="segmentBatchNo"
                fixed="left"
                title="分段批号 / 片号"
                tree-node
                width="280"
              >
                <template #default="{ row }">
                  <div
                    v-if="isPickCandidateGroup(row)"
                    class="outbound-pick-segment-cell"
                  >
                    <strong>{{ row.segmentBatchNo || '-' }}</strong>
                    <em>
                      共 {{ row.groupCount || 0 }} 片，{{
                        formatPickSegmentSamples(row)
                      }}
                    </em>
                  </div>
                  <span v-else class="outbound-pick-piece-no">{{
                    row.sliceBatchNo || '-'
                  }}</span>
                </template>
              </VxeColumn>
              <VxeColumn
                field="modelCode"
                fixed="left"
                title="产品型号"
                width="130"
              />
              <VxeColumn field="candidateType" title="配货来源" width="110">
                <template #default="{ row }">
                  <template v-if="isPickCandidateGroup(row)">
                    <Tag v-if="row.warehousePieceCount" color="blue">
                      库存 {{ row.warehousePieceCount }}
                    </Tag>
                    <Tag v-if="row.packagingDirectPieceCount" color="cyan">
                      直发 {{ row.packagingDirectPieceCount }}
                    </Tag>
                  </template>
                  <Tag v-else :color="isPackagingDirect(row) ? 'cyan' : 'blue'">
                    {{ pickSourceText(row) }}
                  </Tag>
                </template>
              </VxeColumn>
              <VxeColumn field="pickStage" title="来源阶段" width="140">
                <template #default="{ row }">{{
                  isPickCandidateGroup(row) ? '展开查看' : pickStageText(row)
                }}</template>
              </VxeColumn>
              <VxeColumn field="productionDate" title="生产时间" width="120" />
              <VxeColumn field="expiryDate" title="有效日期" width="120" />
              <VxeColumn field="rackNo" title="货架" width="80">
                <template #default="{ row }">{{
                  locationPart(row, 'rack')
                }}</template>
              </VxeColumn>
              <VxeColumn field="layerNo" title="层" width="80">
                <template #default="{ row }">{{
                  locationPart(row, 'layer')
                }}</template>
              </VxeColumn>
              <VxeColumn field="areaNo" title="区" width="80">
                <template #default="{ row }">{{
                  locationPart(row, 'area')
                }}</template>
              </VxeColumn>
              <VxeColumn field="trayNo" title="托盘" width="90">
                <template #default="{ row }">{{
                  locationPart(row, 'tray')
                }}</template>
              </VxeColumn>
              <VxeColumn field="locationCode" title="货位编号" width="150">
                <template #default="{ row }">{{ locationText(row) }}</template>
              </VxeColumn>
              <VxeColumn field="qualityStatus" title="裁切成品检验" width="140">
                <template #default="{ row }">
                  <Tag v-if="isPickCandidateGroup(row)" color="blue">分组</Tag>
                  <Tag
                    v-else
                    :color="
                      qualityColor(row.inspectionResult || row.qualityStatus)
                    "
                  >
                    {{ row.inspectionResult || row.qualityStatus || '-' }}
                  </Tag>
                </template>
              </VxeColumn>
              <VxeColumn field="stockStatus" title="库存状态" width="120">
                <template #default="{ row }">{{
                  isPickCandidateGroup(row)
                    ? '共 ' + (row.groupCount || 0) + ' 片'
                    : isPackagingDirect(row)
                      ? '待直发'
                      : row.stockStatus || '-'
                }}</template>
              </VxeColumn>
              <VxeColumn field="inboundTime" title="入库时间" width="160">
                <template #default="{ row }">{{
                  formatDateTime(row.inboundTime)
                }}</template>
              </VxeColumn>
              <VxeColumn field="inboundUserName" title="入库人" width="110" />
            </VxeTable>
          </div>
          <div class="shipping-selector-pagination">
            <Pagination
              :current="pickCandidatePageNo"
              :page-size="pickCandidatePageSize"
              :page-size-options="['20', '50', '100']"
              :total="pickCandidateTotal"
              show-less-items
              show-size-changer
              :show-total="(total: number) => `共 ${total} 个分段批号`"
              @change="changePickCandidatePage"
            />
          </div>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.fg-outbound-board {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
}

.outbound-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.outbound-simple-query {
  display: grid;
  grid-template-columns: auto 72px minmax(280px, 1fr) 94px 118px 210px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.outbound-status-tabs {
  display: inline-flex;
  overflow: hidden;
  border: 1px solid #b8c7d8;
}

.outbound-status-tabs button {
  min-width: 92px;
  height: 34px;
  padding: 0 16px;
  color: #075985;
  font-weight: 800;
  background: #eef5fb;
  border: 0;
  border-right: 1px solid #b8c7d8;
}

.outbound-status-tabs button:last-child {
  border-right: 0;
}

.outbound-status-tabs button.is-active {
  color: #fff;
  background: #0879c9;
}

.outbound-keyword-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.outbound-simple-query :deep(.ant-input-affix-wrapper),
.outbound-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.outbound-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.outbound-query-template-select {
  min-width: 0;
}

.outbound-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 184px;
}

.outbound-query-tag {
  display: inline-flex;
  align-items: center;
  max-width: 360px;
  min-height: 24px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
}

.outbound-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.outbound-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outbound-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.outbound-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.outbound-query-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.outbound-query-item label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  background: #dbe3ed;
  border-right: 1px solid #c6d3df;
}

.outbound-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.outbound-query-item :deep(.ant-input),
.outbound-query-item :deep(.ant-input-affix-wrapper),
.outbound-query-item :deep(.ant-picker),
.outbound-query-item :deep(.ant-select),
.outbound-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.outbound-advanced-query-body {
  display: grid;
  gap: 10px;
}

.outbound-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.outbound-template-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: stretch;
  padding-top: 2px;
}

.outbound-template-row label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.outbound-template-row :deep(.ant-input-affix-wrapper),
.outbound-template-row :deep(.ant-btn) {
  min-height: 32px;
  border-radius: 0;
}

.outbound-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.outbound-grid-panel {
  flex: 1;
  min-height: 0;
  padding: 10px;
}

.outbound-grid-panel :deep(.vben-vxe-grid),
.outbound-grid-panel :deep(.vxe-grid) {
  height: 100%;
}

.outbound-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.outbound-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1;
  min-height: 0;
}

.outbound-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex-shrink: 0;
}

.outbound-detail-body {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  gap: 10px;
  overflow: hidden;
}

.report-modal-body {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
}

.erp-fieldset {
  padding: 8px 10px 10px;
  margin: 0;
  border: 1px solid #9fb6cd;
}

.erp-fieldset legend {
  padding: 0 8px;
  color: #075985;
  font-size: 14px;
  font-weight: 900;
}

.report-modal-toolbar {
  display: block;
  flex: 0 0 auto;
  min-height: 58px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.inspection-form-sheet {
  display: grid;
  gap: 8px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections {
  display: none;
}

.shipping-notice-form-title-row,
.shipping-selector-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.inspection-form-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.1;
  text-align: center;
}

.outbound-detail-title {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
}

.outbound-detail-actions {
  position: absolute;
  right: 0;
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.outbound-detail-actions :deep(.ant-btn) {
  height: 30px;
  border-radius: 4px;
  font-weight: 800;
}

.inspection-form-head {
  display: grid;
  grid-template-columns:
    92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr)
    92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #9fb6cd;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #e2e8f0;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control :deep(.ant-tag) {
  margin: 0;
}

.inspection-form-subtitle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #334155;
  font-size: 12px;
}

.inspection-form-subtitle span {
  font-weight: 900;
}

.inspection-form-subtitle em {
  color: #64748b;
  font-style: normal;
}

.inspection-form-subtitle :deep(.ant-btn) {
  margin-left: auto;
}

.outbound-detail-head {
  grid-template-columns: 115px 1fr 115px 1fr 115px 1fr 115px 1fr;
}

.shipping-section-form-head {
  grid-template-columns:
    110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr)
    110px minmax(0, 1fr);
}

.shipping-selector-form-head {
  grid-template-columns:
    110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr)
    110px minmax(0, 1fr);
}

.shipping-form-control--span-3 {
  grid-column: span 5;
}

.shipping-form-control--span-2 {
  grid-column: span 3;
}

.shipping-form-label--full-row {
  grid-column: 1;
}

.shipping-form-control--full-row {
  grid-column: span 7;
}

.inspection-form-control :deep(.ant-input),
.inspection-form-control :deep(.ant-input-affix-wrapper),
.inspection-form-control :deep(.ant-select),
.inspection-form-control :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.outbound-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0 8px 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.outbound-detail-tabs :deep(.ant-tabs-nav) {
  align-items: center;
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-tab-maximize-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.shipping-detail-maximized-body .outbound-detail-tabs {
  flex: 1 1 auto;
}

.outbound-detail-tabs :deep(.ant-tabs-content-holder),
.outbound-detail-tabs :deep(.ant-tabs-content),
.outbound-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
}

.outbound-detail-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
}

.outbound-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.outbound-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  overflow: hidden;
}

.outbound-detail-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.inspection-task-detail-table.shipping-vxe-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.outbound-detail-table :deep(.ant-table-cell) {
  border-right: 1px solid #e5e7eb;
}

.outbound-detail-table :deep(.ant-spin-nested-loading) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.outbound-detail-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.outbound-detail-table :deep(.ant-table) {
  flex: 1 1 auto;
  height: auto !important;
  min-height: 0;
}

.outbound-detail-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.outbound-detail-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: auto !important;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.outbound-detail-table :deep(.ant-pagination) {
  flex: 0 0 38px;
  margin: 6px 0 0;
  padding: 4px 8px 0;
  border-top: 1px solid #d8e0ea;
}

.shipping-vxe-table :deep(.vxe-table),
.shipping-vxe-table :deep(.vxe-table--render-wrapper),
.shipping-vxe-table :deep(.vxe-table--main-wrapper),
.shipping-vxe-table :deep(.vxe-table--body-wrapper) {
  min-height: 0;
}

.outbound-pick-candidate {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: calc(100vh - 24px);
  min-height: 0;
  overflow: hidden;
}

.outbound-fixed-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  padding: 0 8px;
  color: #075985;
  font-size: 12px;
  font-weight: 800;
  background: #eef6ff;
  border: 1px solid #93c5fd;
  border-radius: 999px;
}

.outbound-fixed-condition-cell {
  grid-column: span 7;
  gap: 6px;
  align-items: center;
  white-space: normal;
}

.outbound-fixed-tag b {
  color: #fff;
  padding: 0 6px;
  background: #0879c9;
  border-radius: 999px;
}

.outbound-fixed-tag.is-count {
  color: #334155;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.outbound-candidate-vxe {
  min-height: 0;
}

.outbound-candidate-vxe :deep(.outbound-pick-segment-row) {
  background: #f0f7ff;
}

.outbound-pick-segment-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.outbound-pick-segment-cell strong {
  color: #075985;
  font-weight: 800;
}

.outbound-pick-segment-cell em {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outbound-pick-piece-no {
  color: #334155;
  font-weight: 700;
}

.shipping-selector-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

.shipping-selector-actions :deep(.ant-btn) {
  width: 152px;
  min-width: 152px;
}

.shipping-selector-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  margin: -2px 0 0;
  padding: 0 2px;
}

.report-action-footer :deep(.ant-btn) {
  width: 152px;
  min-width: 152px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .outbound-simple-query {
    grid-template-columns: 1fr;
  }

  .outbound-keyword-label {
    justify-content: flex-start;
  }

  .outbound-query-tags {
    padding-left: 0;
  }

  .outbound-advanced-query-grid {
    grid-template-columns: 1fr;
  }

  .outbound-detail-head {
    grid-template-columns: 110px 1fr 110px 1fr;
  }
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  display: flex;
  width: 100vw !important;
  height: 100vh !important;
  flex-direction: column;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  width: 100vw !important;
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  flex: 1 1 auto;
  width: 100vw;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    #f5f7fa;
}

.rough-report-work-modal .report-modal-toolbar {
  flex: 0 0 auto;
}

.rough-report-work-modal .modal-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}

.outbound-work-modal .ant-modal {
  top: 12px;
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px);
  margin: 0 12px !important;
}

.outbound-work-modal .ant-modal-body {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
}

.outbound-work-modal .ant-modal-content {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
  border: 1px solid #9fb6cd;
}

.outbound-work-modal .report-modal-body {
  width: calc(100vw - 24px);
  height: calc(100vh - 24px);
}

.outbound-pick-work-modal .ant-modal {
  top: 12px;
  width: calc(100vw - 24px) !important;
  max-width: calc(100vw - 24px);
  height: calc(100vh - 24px);
  margin: 0 12px !important;
  padding-bottom: 0;
}

.outbound-pick-work-modal .ant-modal-body {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
}

.outbound-pick-work-modal .ant-modal-content {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 6px !important;
  box-shadow: 0 10px 26px rgb(15 23 42 / 18%);
}

.outbound-pick-work-modal .report-modal-body {
  width: calc(100vw - 24px);
  height: calc(100vh - 24px);
}

.return-pick-confirm {
  display: grid;
  gap: 10px;
}

.return-pick-confirm-tip {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}
</style>
