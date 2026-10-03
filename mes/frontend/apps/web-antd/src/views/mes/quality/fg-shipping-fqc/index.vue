<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';

import {
  computed,
  nextTick,
  onActivated,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Table as ATable,
  Button,
  Checkbox,
  Empty,
  Image,
  Input,
  message,
  Modal,
  Pagination,
  Progress,
  Radio,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditFgShippingFqc,
  createFgShippingFqcFromShippingNotice,
  getFgShippingFqcDefectCodeOptions,
  getFgShippingFqcDetail,
  getFgShippingFqcPage,
  getFgShippingFqcPendingList,
  saveFgShippingFqcProgramEntry,
  submitFgShippingFqcProgramEntry,
} from '#/api/mes/quality/fg-shipping-fqc';

import {
  FgShippingFqcAlignmentOptions,
  FgShippingFqcJudgmentOptions,
  FgShippingFqcStatusOptions,
  optionMeta,
  useGridColumns,
  useGridFormSchema,
} from './data';

defineOptions({ name: 'MesQualityFgShippingFqc' });

const DEFECT_PAGE_SIZE = 10;

interface DefectSelectOption {
  causes: MesFgShippingFqcApi.DefectCause[];
  defectCode: string;
  defectLevel?: string;
  defectName: string;
  label: string;
  referencePicUrls?: string[];
  remark?: string;
  value: number;
}

const viewMode = ref<'ENTRY' | 'LEDGER'>('LEDGER');
const entryMode = ref<'EDIT' | 'READONLY'>('EDIT');
const activeRecord = ref<MesFgShippingFqcApi.Record>();
const selectedDetailId = ref<number>();
const pendingModalOpen = ref(false);
const pendingKeyword = ref('');
const pendingLoading = ref(false);
const pendingTasks = ref<MesFgShippingFqcApi.PendingTask[]>([]);
const loadingDetail = ref(false);
const saving = ref(false);
const savingItemIds = ref<Set<number>>(new Set());
const auditModalOpen = ref(false);
const auditTargetRecord = ref<MesFgShippingFqcApi.Record>();
const auditResult = ref<'PASS' | 'REJECT'>('PASS');
const rejectReason = ref('');
const itemPanelRef = ref<HTMLElement>();
const itemTableScrollY = ref(520);
const ngModalOpen = ref(false);
const defectOptions = ref<DefectSelectOption[]>([]);
const defectOptionsLoaded = ref(false);
const defectPickerKeyword = ref('');
const defectPickerPage = ref(1);
const highlightedDefectCodeId = ref<number>();
const ngDraft = reactive({
  defectCodeIds: [] as number[],
  itemId: undefined as number | undefined,
  remark: '',
});
const isReadonlyEntry = computed(() => entryMode.value === 'READONLY');

const searchFormFields = [
  'fqcNo',
  'shippingNoticeNo',
  'customerName',
  'erpOrderNo',
  'actualSliceBatchNo',
  'materialCode',
  'modelCode',
  'status',
  'judgment',
  'recheckFlag',
  'alignmentStatus',
  'submissionTime',
];

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGridFormSchema().map((item) => ({
    ...item,
    hide: collapsed && !keepFields.has(item.fieldName),
  }));
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getFgShippingFqcPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
}

const pendingColumns = [
  { dataIndex: 'shippingNoticeNo', title: '发货通知单', width: 170 },
  { dataIndex: 'customerName', title: '客户', width: 160 },
  { dataIndex: 'erpOrderNo', title: 'ERP订单', width: 150 },
  { dataIndex: 'materialCode', title: '物料编码', width: 130 },
  { dataIndex: 'modelCode', title: '产品型号', width: 130 },
  { align: 'right', dataIndex: 'pickedQty', title: '配货数', width: 90 },
  { align: 'right', dataIndex: 'inspectedQty', title: '已检数', width: 90 },
  { dataIndex: 'existingFqcStatus', title: 'FQC状态', width: 110 },
  { dataIndex: 'action', fixed: 'right', title: '操作', width: 130 },
];

const detailRows = computed(() => activeRecord.value?.shippingDetails || []);
const selectedDetail = computed(() => {
  if (detailRows.value.length === 0) return undefined;
  return (
    detailRows.value.find((item) => item.id === selectedDetailId.value) ||
    detailRows.value[0]
  );
});
const selectedItems = computed(() => {
  const detail = selectedDetail.value;
  if (!detail) return [];
  if (detail.items && detail.items.length > 0) return detail.items;
  return (activeRecord.value?.items || []).filter(
    (item) =>
      item.submissionDetailId === detail.id ||
      item.productionBatchNo === detail.actualSliceBatchNo,
  );
});
const showActualValueColumn = computed(() =>
  selectedItems.value.some((item) => item.actualValueRequired),
);
const itemColumns = computed(() =>
  [
    { dataIndex: 'inspectionItem', title: '检验项目', width: 180 },
    { dataIndex: 'itemType', title: '类型', width: 90 },
    { dataIndex: 'standardDesc', title: '标准', width: 260 },
    { dataIndex: 'sampleSize', title: '抽样数', width: 90 },
    { dataIndex: 'itemResult', title: '判定', width: 110 },
    { dataIndex: 'actualValue', title: '实际值', width: 180 },
    {
      dataIndex: 'action',
      fixed: 'right',
      title: '操作',
      width: 160,
    },
  ].filter(
    (column) =>
      (!isReadonlyEntry.value || column.dataIndex !== 'action') &&
      (column.dataIndex !== 'actualValue' || showActualValueColumn.value),
  ),
);

let itemPanelResizeObserver: ResizeObserver | undefined;

function updateItemTableScrollY() {
  const panel = itemPanelRef.value;
  if (!panel) return;
  const detailHeader = panel.querySelector<HTMLElement>(
    '.fg-shipping-fqc-detail-header',
  );
  const panelHeight = panel.getBoundingClientRect().height;
  const detailHeaderHeight = detailHeader?.getBoundingClientRect().height || 0;
  const tableHeaderHeight = 46;
  itemTableScrollY.value = Math.max(
    240,
    Math.floor(panelHeight - detailHeaderHeight - tableHeaderHeight),
  );
}

function scheduleItemTableScrollUpdate() {
  void nextTick(updateItemTableScrollY);
}

function bindItemPanelResizeObserver() {
  itemPanelResizeObserver?.disconnect();
  const panel = itemPanelRef.value;
  if (!panel || typeof ResizeObserver === 'undefined') return;

  itemPanelResizeObserver = new ResizeObserver(updateItemTableScrollY);
  itemPanelResizeObserver.observe(panel);

  const detailHeader = panel.querySelector<HTMLElement>(
    '.fg-shipping-fqc-detail-header',
  );
  if (detailHeader) {
    itemPanelResizeObserver.observe(detailHeader);
  }
}

function fallbackText(...values: Array<null | number | string | undefined>) {
  const matched = values.find(
    (value) => value !== undefined && value !== null && String(value).trim(),
  );
  return matched === undefined || matched === null ? '-' : String(matched);
}

function displayDateTime(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}

function detailPrimaryNo(detail: MesFgShippingFqcApi.ShippingDetail) {
  return fallbackText(
    detail.actualSliceBatchNo,
    detail.sliceBatchNo,
    detail.stockNo,
    `明细 ${detail.id}`,
  );
}

function detailSliceModelText(detail: MesFgShippingFqcApi.ShippingDetail) {
  return `${detailPrimaryNo(detail)} / ${fallbackText(detail.modelCode)}`;
}

function isCompletedItem(item: MesFgShippingFqcApi.FqcItem) {
  return item.itemResult === 'OK' || item.itemResult === 'NG';
}

function isNgItem(item: MesFgShippingFqcApi.FqcItem) {
  return item.itemResult === 'NG' || item.qaResult === 'NG';
}

function hasNgRecord(item: MesFgShippingFqcApi.FqcItem) {
  return (
    isNgItem(item) ||
    (item.samples || []).some(
      (sample) =>
        sample.sampleResult === 'NG' ||
        sample.qualitativeValue === 'NG' ||
        (sample.defects || []).length > 0,
    ) ||
    (item.qaValues || []).some(
      (sample) =>
        sample.sampleResult === 'NG' ||
        sample.value === 'NG' ||
        (sample.defects || []).length > 0,
    )
  );
}

function normalizeActualValue(value?: string) {
  return String(value ?? '').trim();
}

function collectRequiredActualValueMissingItems(
  record: MesFgShippingFqcApi.Record,
) {
  const keyedItems = new Map<number, MesFgShippingFqcApi.FqcItem>();
  const unkeyedItems: MesFgShippingFqcApi.FqcItem[] = [];
  const collect = (item: MesFgShippingFqcApi.FqcItem) => {
    if (item.id) {
      keyedItems.set(item.id, { ...keyedItems.get(item.id), ...item });
      return;
    }
    unkeyedItems.push(item);
  };
  (record.shippingDetails || []).forEach((detail) => {
    (detail.items || []).forEach((item) => collect(item));
  });
  (record.items || []).forEach((item) => collect(item));

  return [...keyedItems.values(), ...unkeyedItems].filter(
    (item) =>
      item.actualValueRequired && !normalizeActualValue(item.actualValue),
  );
}

function buildActualValueMissingText(items: MesFgShippingFqcApi.FqcItem[]) {
  return items
    .slice(0, 5)
    .map((item) =>
      [
        item.productionBatchNo || item.parentProductionBatchNo,
        item.inspectionItem || '未命名项目',
      ]
        .filter(Boolean)
        .join('/'),
    )
    .join('、');
}

function validateRequiredActualValuesBeforeSubmit(
  record: MesFgShippingFqcApi.Record,
) {
  const missingItems = collectRequiredActualValueMissingItems(record);
  if (missingItems.length === 0) return true;
  const suffix = missingItems.length > 5 ? `等 ${missingItems.length} 项` : '';
  message.warning(
    `检验项目【${buildActualValueMissingText(missingItems)}】${suffix}必须填写实际值`,
  );
  return false;
}

function withDetailStats(
  detail: MesFgShippingFqcApi.ShippingDetail,
): MesFgShippingFqcApi.ShippingDetail {
  const items = detail.items || [];
  if (items.length === 0) return detail;
  const requiredItemCount = items.length;
  const completedItemCount = items.filter(isCompletedItem).length;
  const abnormalItemCount = items.filter(isNgItem).length;
  const itemJudgment =
    abnormalItemCount > 0
      ? 'NG'
      : completedItemCount === requiredItemCount
        ? 'OK'
        : 'PENDING';
  const rowJudgment =
    itemJudgment === 'PENDING' && detail.rowJudgment === 'NG'
      ? 'NG'
      : itemJudgment;
  return {
    ...detail,
    abnormalItemCount,
    completedItemCount,
    entryProgress:
      rowJudgment === 'NG'
        ? 100
        : Math.floor((completedItemCount * 100) / requiredItemCount),
    requiredItemCount,
    rowJudgment,
  };
}

function setItemSaving(itemId: number, value: boolean) {
  const nextIds = new Set(savingItemIds.value);
  if (value) {
    nextIds.add(itemId);
  } else {
    nextIds.delete(itemId);
  }
  savingItemIds.value = nextIds;
}

function isItemSaving(itemId?: number) {
  return !!itemId && savingItemIds.value.has(itemId);
}

const selectedDetailSaving = computed(() =>
  selectedItems.value.some((item) => isItemSaving(item.id)),
);
const entryCompleted = computed(() => {
  const rows = detailRows.value;
  return (
    rows.length > 0 &&
    rows.every((detail) => ['NG', 'OK'].includes(detail.rowJudgment)) &&
    rows.every(
      (detail) =>
        detail.rowJudgment === 'NG' ||
        !detail.requiredItemCount ||
        (detail.completedItemCount || 0) >= detail.requiredItemCount,
    )
  );
});
function hasShippingDetails(record?: MesFgShippingFqcApi.Record) {
  return !!record?.shippingDetails?.length;
}

function resolveSelectedDetailId(
  record?: MesFgShippingFqcApi.Record,
  preferredDetailId?: number,
) {
  const details = record?.shippingDetails || [];
  if (details.length === 0) return undefined;
  if (
    preferredDetailId &&
    details.some((detail) => detail.id === preferredDetailId)
  ) {
    return preferredDetailId;
  }
  return details[0]?.id;
}

function patchRecordItems(
  record: MesFgShippingFqcApi.Record,
  itemId: number,
  updater: (item: MesFgShippingFqcApi.FqcItem) => MesFgShippingFqcApi.FqcItem,
) {
  const patchItem = (row: MesFgShippingFqcApi.FqcItem) =>
    row.id === itemId ? updater(row) : row;

  return {
    ...record,
    items: (record.items || []).map(patchItem),
    shippingDetails: (record.shippingDetails || []).map((detail) => ({
      ...withDetailStats({
        ...detail,
        items: (detail.items || []).map(patchItem),
      }),
    })),
  };
}

function updateItemActualValue(
  item: MesFgShippingFqcApi.FqcItem,
  value: string,
) {
  if (!activeRecord.value || !item.id) return;
  activeRecord.value = patchRecordItems(activeRecord.value, item.id, (row) => ({
    ...row,
    actualValue: value,
  }));
}

function findRecordItemById(
  record: MesFgShippingFqcApi.Record,
  itemId: number,
) {
  const detailItem = (record.shippingDetails || [])
    .flatMap((detail) => detail.items || [])
    .find((row) => row.id === itemId);
  return detailItem || (record.items || []).find((row) => row.id === itemId);
}

function mergeItemList(
  previousItems: MesFgShippingFqcApi.FqcItem[],
  nextItems: MesFgShippingFqcApi.FqcItem[],
) {
  if (nextItems.length === 0) return previousItems;
  const nextItemMap = new Map(
    nextItems
      .filter((item) => item.id)
      .map((item) => [item.id as number, item]),
  );
  const mergedItems = previousItems.map((item) => {
    const latestItem = item.id ? nextItemMap.get(item.id) : undefined;
    return latestItem ? { ...item, ...latestItem } : item;
  });
  const previousIds = new Set(
    previousItems.filter((item) => item.id).map((item) => item.id as number),
  );
  nextItems.forEach((item) => {
    if (!item.id || !previousIds.has(item.id)) {
      mergedItems.push(item);
    }
  });
  return mergedItems;
}

function mergeRecordKeepingDetails(
  next: MesFgShippingFqcApi.Record,
  previous: MesFgShippingFqcApi.Record,
) {
  const latestItems = mergeItemList(previous.items || [], next.items || []);
  const latestItemMap = new Map(
    latestItems
      .filter((item) => item.id)
      .map((item) => [item.id as number, item]),
  );
  const nextDetailMap = new Map(
    (next.shippingDetails || []).map((detail) => [detail.id, detail]),
  );
  const shippingDetails = (previous.shippingDetails || []).map((detail) => {
    const latestDetail = nextDetailMap.get(detail.id);
    return withDetailStats({
      ...detail,
      ...(latestDetail || {}),
      items: (detail.items || []).map((item) => {
        const latestItem = item.id ? latestItemMap.get(item.id) : undefined;
        return latestItem ? { ...item, ...latestItem } : item;
      }),
    });
  });
  const previousDetailIds = new Set(
    (previous.shippingDetails || []).map((detail) => detail.id),
  );
  (next.shippingDetails || []).forEach((detail) => {
    if (!previousDetailIds.has(detail.id)) {
      shippingDetails.push(withDetailStats(detail));
    }
  });
  const alignmentRows =
    next.alignmentRows !== undefined
      ? next.alignmentRows
      : previous.alignmentRows;
  const alignmentCandidates =
    next.alignmentCandidates !== undefined
      ? next.alignmentCandidates
      : previous.alignmentCandidates;
  const alignmentStatus =
    next.alignmentRows !== undefined || next.alignmentStatus !== undefined
      ? next.alignmentStatus
      : previous.alignmentStatus;
  const mismatchReason =
    next.alignmentRows !== undefined || next.mismatchReason !== undefined
      ? next.mismatchReason
      : previous.mismatchReason;

  return {
    ...previous,
    ...next,
    alignmentCandidates,
    alignmentRows,
    alignmentStatus,
    items: latestItems,
    mismatchReason,
    submissionDetailCount:
      next.submissionDetailCount && next.submissionDetailCount > 0
        ? next.submissionDetailCount
        : previous.submissionDetailCount || shippingDetails.length,
    shippingDetails,
  };
}

async function normalizeReturnedRecord(
  next: MesFgShippingFqcApi.Record,
  previous?: MesFgShippingFqcApi.Record,
) {
  if (hasShippingDetails(next) || !hasShippingDetails(previous)) {
    return next;
  }
  try {
    const fresh = await getFgShippingFqcDetail(next.id || previous.id);
    if (hasShippingDetails(fresh)) {
      return fresh;
    }
  } catch {
    // 保存已成功但详情回读失败时，保留当前明细，避免工作台被空响应覆盖。
  }
  return mergeRecordKeepingDetails(next, previous);
}

async function applyReturnedRecord(
  next: MesFgShippingFqcApi.Record,
  previous?: MesFgShippingFqcApi.Record,
  preferredDetailId?: number,
) {
  activeRecord.value = filterRecheckRecord(
    await normalizeReturnedRecord(next, previous),
  );
  selectedDetailId.value = resolveSelectedDetailId(
    activeRecord.value,
    preferredDetailId,
  );
}

function filterRecheckRecord(record: MesFgShippingFqcApi.Record) {
  if (!record.recheckFlag) return record;
  const shippingDetails = (record.shippingDetails || []).filter(
    (detail) =>
      detail.recheckDetailFlag ||
      detail.items?.some(
        (item) =>
          item.recheckItemFlag ||
          item.samples?.some((sample) => sample.recheckItemFlag),
      ),
  );
  const items = (record.items || []).filter(
    (item) =>
      item.recheckItemFlag ||
      item.samples?.some((sample) => sample.recheckItemFlag),
  );
  return {
    ...record,
    items: items.length ? items : record.items,
    shippingDetails: shippingDetails.length
      ? shippingDetails
      : record.shippingDetails,
  };
}

const selectedDefects = computed(
  () =>
    ngDraft.defectCodeIds
      .map((id) => defectOptions.value.find((item) => item.value === id))
      .filter(Boolean) as DefectSelectOption[],
);
const filteredDefectOptions = computed(() => {
  const keyword = defectPickerKeyword.value.trim().toLowerCase();
  if (!keyword) return defectOptions.value;
  return defectOptions.value.filter((item) =>
    [item.defectCode, item.defectName, item.remark]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(keyword)),
  );
});
const defectTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredDefectOptions.value.length / DEFECT_PAGE_SIZE)),
);
const currentDefectPage = computed(() =>
  Math.min(defectPickerPage.value, defectTotalPages.value),
);
const pagedDefectOptions = computed(() => {
  const start = (currentDefectPage.value - 1) * DEFECT_PAGE_SIZE;
  return filteredDefectOptions.value.slice(start, start + DEFECT_PAGE_SIZE);
});
const ngModalTitle = '录入检验项目NG';
const highlightedDefect = computed(() => {
  return (
    filteredDefectOptions.value.find(
      (item) => item.value === highlightedDefectCodeId.value,
    ) ||
    filteredDefectOptions.value.find(
      (item) => item.value === ngDraft.defectCodeIds[0],
    ) ||
    filteredDefectOptions.value[0]
  );
});
const highlightedDefectCauses = computed(() =>
  normalizeCauseList(highlightedDefect.value),
);
const selectedDefectLabels = computed(() =>
  selectedDefects.value.map(
    (item) => `${item.defectCode} - ${item.defectName}`,
  ),
);

watch(defectPickerKeyword, () => {
  defectPickerPage.value = 1;
});

watch(itemPanelRef, () => {
  void nextTick(() => {
    bindItemPanelResizeObserver();
    updateItemTableScrollY();
  });
});

watch(
  () => [viewMode.value, selectedDetailId.value, selectedItems.value.length],
  scheduleItemTableScrollUpdate,
);

function statusTag(value?: string) {
  return optionMeta(FgShippingFqcStatusOptions, value);
}

function judgmentTag(value?: string) {
  return optionMeta(FgShippingFqcJudgmentOptions, value || 'PENDING');
}

function alignmentTag(value?: string, productType?: string) {
  if (productType === 'SAMPLE') return { color: 'default', label: '无需对齐' };
  return optionMeta(FgShippingFqcAlignmentOptions, value || 'PENDING');
}

function rowJudgmentTag(value?: string) {
  if (value === 'OK') return { color: 'success', label: 'OK' };
  if (value === 'NG') return { color: 'error', label: 'NG' };
  return { color: 'default', label: '待判定' };
}

function itemTypeLabel(value?: string) {
  if (value === 'QUALITATIVE') return '定性';
  if (value === 'QUANTITATIVE') return '定量';
  return value || '-';
}

function causeKey(cause: MesFgShippingFqcApi.DefectCause) {
  return String(cause.id || cause.reasonCode || cause.reasonName);
}

function normalizeCauseList(option?: DefectSelectOption) {
  return (option?.causes || []).filter(
    (item) => item.status !== 0 && item.reasonName,
  );
}

function handleDefectPageChange(page: number) {
  defectPickerPage.value = page;
  const firstRow = pagedDefectOptions.value[0];
  if (firstRow) {
    highlightedDefectCodeId.value = firstRow.value;
  }
}

function defectPageTotalText(total: number) {
  return `共 ${total} 条`;
}

async function loadDefectOptions() {
  if (defectOptionsLoaded.value) return;
  const list = await getFgShippingFqcDefectCodeOptions();
  defectOptions.value = (list || [])
    .filter((item) => item.type === 'ITEM' && item.status === 1 && item.id)
    .map((item) => ({
      causes: item.causes || [],
      defectCode: item.code,
      defectLevel: item.level,
      defectName: item.name,
      label: `${item.code} - ${item.name}`,
      referencePicUrls: item.referencePicUrls || [],
      remark: item.remark,
      value: item.id,
    }));
  defectOptionsLoaded.value = true;
}

function isDefectSelected(value: number) {
  return ngDraft.defectCodeIds.includes(value);
}

function toggleDefectSelection(option: DefectSelectOption, checked: boolean) {
  highlightedDefectCodeId.value = option.value;
  if (checked) {
    if (!ngDraft.defectCodeIds.includes(option.value)) {
      ngDraft.defectCodeIds = [...ngDraft.defectCodeIds, option.value];
    }
    return;
  }
  ngDraft.defectCodeIds = ngDraft.defectCodeIds.filter(
    (item) => item !== option.value,
  );
}

function buildNgReason() {
  const parts = [
    selectedDefectLabels.value.length > 0
      ? `缺陷：${selectedDefectLabels.value.join('；')}`
      : '',
    ngDraft.remark.trim() ? `补充：${ngDraft.remark.trim()}` : '',
  ].filter(Boolean);
  return parts.join('；');
}

function extractRemark(reason?: string) {
  if (!reason) return '';
  const marker = '补充：';
  const index = reason.lastIndexOf(marker);
  return index >= 0 ? reason.slice(index + marker.length).trim() : '';
}

function hasReferenceImages(option?: DefectSelectOption) {
  return !!option?.referencePicUrls?.length;
}

function canEdit() {
  return (
    !isReadonlyEntry.value &&
    !!activeRecord.value &&
    ['INSPECTING', 'PENDING'].includes(activeRecord.value.status)
  );
}

function canSubmit() {
  return (
    !isReadonlyEntry.value &&
    !!activeRecord.value &&
    ['INSPECTING', 'PENDING'].includes(activeRecord.value.status)
  );
}

function canAuditRecord(record?: MesFgShippingFqcApi.Record) {
  return record?.status === 'WAITING_QA';
}

function canAudit() {
  return !isReadonlyEntry.value && canAuditRecord(activeRecord.value);
}

async function openEntry(
  row: MesFgShippingFqcApi.Record,
  detailId?: number,
  mode: 'EDIT' | 'READONLY' = 'EDIT',
) {
  if (!row.id) return;
  loadingDetail.value = true;
  try {
    const record = await getFgShippingFqcDetail(row.id);
    await applyReturnedRecord(record, undefined, detailId);
    entryMode.value = mode;
    viewMode.value = 'ENTRY';
  } finally {
    loadingDetail.value = false;
  }
}

async function openReadonlyEntry(row: MesFgShippingFqcApi.Record) {
  await openEntry(row, undefined, 'READONLY');
}

async function refreshActiveRecord() {
  const previousRecord = activeRecord.value;
  if (!previousRecord?.id) return;
  const currentDetailId = selectedDetailId.value;
  const record = await getFgShippingFqcDetail(previousRecord.id);
  await applyReturnedRecord(record, previousRecord, currentDetailId);
}

function backToLedger() {
  viewMode.value = 'LEDGER';
  entryMode.value = 'EDIT';
  gridApi.query();
}

function selectDetail(detail: MesFgShippingFqcApi.ShippingDetail) {
  selectedDetailId.value = detail.id;
}

async function refreshPendingList() {
  pendingLoading.value = true;
  try {
    pendingTasks.value = await getFgShippingFqcPendingList(
      pendingKeyword.value.trim() || undefined,
    );
  } finally {
    pendingLoading.value = false;
  }
}

function openPendingModal() {
  pendingModalOpen.value = true;
  void refreshPendingList();
}

async function createFromPending(row: MesFgShippingFqcApi.PendingTask) {
  const record = await createFgShippingFqcFromShippingNotice(
    row.shippingNoticeId,
  );
  pendingModalOpen.value = false;
  await applyReturnedRecord(record);
  entryMode.value = 'EDIT';
  viewMode.value = 'ENTRY';
  gridApi.query();
  message.success(
    row.existingFqcId ? '已刷新发货成品检验单' : '已自动生成发货成品检验单',
  );
}

function buildSampleValues(
  item: MesFgShippingFqcApi.FqcItem,
  judgment: MesFgShippingFqcApi.Judgment,
  extra: {
    defects?: MesFgShippingFqcApi.FqcSampleDefect[];
    remark?: string;
  } = {},
) {
  const sampleSize = Math.max(1, item.sampleSize || 1);
  return Array.from({ length: sampleSize }).map(() => ({
    defects: judgment === 'NG' ? extra.defects || [] : [],
    remark: judgment === 'NG' ? extra.remark : undefined,
    sampleResult: judgment,
    value: judgment,
  }));
}

async function applyItemJudgment(
  item: MesFgShippingFqcApi.FqcItem,
  judgment: MesFgShippingFqcApi.Judgment,
  extra: {
    defects?: MesFgShippingFqcApi.FqcSampleDefect[];
    remark?: string;
  } = {},
) {
  if (!activeRecord.value || !item.id) return;
  if (isItemSaving(item.id)) return;
  const currentDetailId = selectedDetail.value?.id;
  const patchedRecord = patchRecordItems(
    activeRecord.value,
    item.id,
    (row) => ({
      ...row,
      itemResult: judgment,
      qaResult: judgment,
      qaValues: buildSampleValues(row, judgment, extra),
    }),
  );
  activeRecord.value = patchedRecord;
  setItemSaving(item.id, true);
  saving.value = true;
  try {
    const scopedItem = findRecordItemById(patchedRecord, item.id);
    if (!scopedItem) {
      message.error('未找到当前检验项目，请刷新后重试');
      return;
    }
    const savedRecord = await saveFgShippingFqcProgramEntry(patchedRecord, [
      scopedItem,
    ]);
    activeRecord.value = mergeRecordKeepingDetails(savedRecord, patchedRecord);
    selectedDetailId.value = resolveSelectedDetailId(
      activeRecord.value,
      currentDetailId,
    );
    message.success(
      judgment === 'OK' ? '检验项目已录入OK' : '检验项目已录入NG',
    );
  } finally {
    setItemSaving(item.id, false);
    saving.value = savingItemIds.value.size > 0;
  }
}

async function markItemOk(item: MesFgShippingFqcApi.FqcItem) {
  await applyItemJudgment(item, 'OK');
}

async function markSelectedDetailOk() {
  if (!activeRecord.value || !selectedDetail.value) return;
  if (!canEdit()) {
    message.warning('当前单据状态不可修改');
    return;
  }
  if (selectedItems.value.length === 0) {
    message.warning('当前片号没有检验项目');
    return;
  }
  if (selectedItems.value.some(hasNgRecord)) {
    message.warning('当前片号已有NG记录，不能一键合格');
    return;
  }
  if (selectedItems.value.some((item) => !item.id)) {
    message.warning('当前片号存在未加载的检验项目，请刷新后重试');
    return;
  }

  const itemsToMark = selectedItems.value.filter(
    (item) => item.itemResult !== 'OK' || item.qaResult !== 'OK',
  );
  if (itemsToMark.length === 0) {
    message.info('当前片号已全部合格');
    return;
  }

  const currentDetailId = selectedDetail.value.id;
  const itemIds = new Set(itemsToMark.map((item) => item.id as number));
  const patchItem = (row: MesFgShippingFqcApi.FqcItem) =>
    row.id && itemIds.has(row.id)
      ? {
          ...row,
          itemResult: 'OK' as const,
          qaResult: 'OK' as const,
          qaValues: buildSampleValues(row, 'OK'),
        }
      : row;
  const patchedRecord = {
    ...activeRecord.value,
    items: (activeRecord.value.items || []).map(patchItem),
    shippingDetails: (activeRecord.value.shippingDetails || []).map(
      (detail) => ({
        ...withDetailStats({
          ...detail,
          items: (detail.items || []).map(patchItem),
        }),
      }),
    ),
  };

  activeRecord.value = patchedRecord;
  itemsToMark.forEach((item) => setItemSaving(item.id as number, true));
  saving.value = true;
  try {
    const scopedItems = itemsToMark
      .map((item) => findRecordItemById(patchedRecord, item.id as number))
      .filter(Boolean) as MesFgShippingFqcApi.FqcItem[];
    const savedRecord = await saveFgShippingFqcProgramEntry(
      patchedRecord,
      scopedItems,
    );
    activeRecord.value = mergeRecordKeepingDetails(savedRecord, patchedRecord);
    selectedDetailId.value = resolveSelectedDetailId(
      activeRecord.value,
      currentDetailId,
    );
    message.success('当前片号已一键合格');
  } finally {
    itemsToMark.forEach((item) => setItemSaving(item.id as number, false));
    saving.value = savingItemIds.value.size > 0;
  }
}

async function openNgModal(item: MesFgShippingFqcApi.FqcItem) {
  if (item.id && isItemSaving(item.id)) return;
  await loadDefectOptions();
  defectPickerKeyword.value = '';
  defectPickerPage.value = 1;
  const firstNgSample = (item.samples || []).find(
    (sample) =>
      sample.sampleResult === 'NG' ||
      sample.qualitativeValue === 'NG' ||
      (sample.defects || []).length > 0,
  );
  const existingDefectIds = Array.from(
    new Set(
      (firstNgSample?.defects || [])
        .map(
          (defect) =>
            defectOptions.value.find(
              (option) =>
                option.value === defect.defectCodeId ||
                option.defectCode === defect.defectCode,
            )?.value,
        )
        .filter((value): value is number => !!value),
    ),
  );
  ngDraft.itemId = item.id;
  ngDraft.defectCodeIds = existingDefectIds;
  ngDraft.remark = extractRemark(firstNgSample?.remark);
  highlightedDefectCodeId.value =
    existingDefectIds[0] || defectOptions.value[0]?.value;
  ngModalOpen.value = true;
}

async function submitNgModal() {
  if (
    ngDraft.defectCodeIds.length === 0 ||
    selectedDefects.value.length === 0
  ) {
    message.warning('请选择缺陷码');
    return;
  }
  const item =
    selectedItems.value.find((row) => row.id === ngDraft.itemId) ||
    (activeRecord.value?.items || []).find((row) => row.id === ngDraft.itemId);
  if (!item) return;
  await applyItemJudgment(item, 'NG', {
    defects: selectedDefects.value.map((defect, index) => ({
      defectCode: defect.defectCode,
      defectCodeId: defect.value,
      defectLevel: defect.defectLevel,
      defectName: defect.defectName,
      sort: (index + 1) * 10,
    })),
    remark: buildNgReason(),
  });
  ngModalOpen.value = false;
}

async function submitInspection() {
  const previousRecord = activeRecord.value;
  if (!previousRecord) return;
  if (!validateRequiredActualValuesBeforeSubmit(previousRecord)) return;
  if (!entryCompleted.value) {
    message.warning('请完成全部片级检验项目后再提交审核');
    return;
  }
  const currentDetailId = selectedDetail.value?.id;
  saving.value = true;
  try {
    const savedRecord = await submitFgShippingFqcProgramEntry(previousRecord);
    await applyReturnedRecord(savedRecord, previousRecord, currentDetailId);
    message.success('发货成品检验已提交审核');
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

function openAudit(record = activeRecord.value) {
  if (!record?.id) return;
  if (!canAuditRecord(record)) {
    message.warning('发货成品检验提交检测结果后才允许审核');
    return;
  }
  auditTargetRecord.value = record;
  auditResult.value = 'PASS';
  rejectReason.value = '';
  auditModalOpen.value = true;
}

function resetAuditModal() {
  auditTargetRecord.value = undefined;
  auditResult.value = 'PASS';
  rejectReason.value = '';
}

async function submitAudit() {
  const record = auditTargetRecord.value;
  if (!record?.id) return;
  const result = auditResult.value;
  const reason = rejectReason.value.trim();
  if (result === 'REJECT' && !reason) {
    message.warning('请填写驳回原因');
    return;
  }
  saving.value = true;
  try {
    const previousRecord = activeRecord.value;
    const savedRecord = await auditFgShippingFqc(
      record.id,
      result,
      result === 'REJECT' ? reason : undefined,
    );
    if (previousRecord?.id === record.id) {
      await applyReturnedRecord(
        savedRecord,
        previousRecord,
        selectedDetailId.value,
      );
    }
    auditModalOpen.value = false;
    resetAuditModal();
    message.success(
      result === 'REJECT'
        ? '发货成品检验已驳回，已退回重新填写'
        : savedRecord.judgment === 'NG'
          ? '发货成品检验已审核；NG片已自动退回不合格待包装区'
          : savedRecord.productType === 'SAMPLE'
            ? '样品检验已审核完成，无需批号对齐，可继续包装流程'
            : '发货成品检验已审核完成，请到“发货客户批号对齐”菜单继续处理',
    );
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

function itemRowClassName(record: MesFgShippingFqcApi.FqcItem) {
  if (record.itemResult === 'NG') return 'bg-red-50';
  if (record.itemResult === 'OK') return 'bg-emerald-50';
  return '';
}

onActivated(() => {
  if (viewMode.value === 'ENTRY' && activeRecord.value?.id) {
    void refreshActiveRecord();
  }
  scheduleItemTableScrollUpdate();
});

onMounted(() => {
  window.addEventListener('resize', scheduleItemTableScrollUpdate);
  scheduleItemTableScrollUpdate();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', scheduleItemTableScrollUpdate);
  itemPanelResizeObserver?.disconnect();
});
</script>

<template>
  <Page auto-content-height>
    <Modal
      v-model:open="auditModalOpen"
      :confirm-loading="saving"
      destroy-on-close
      title="发货成品检验审核"
      @cancel="resetAuditModal"
      @ok="submitAudit"
    >
      <div class="space-y-4">
        <div class="text-sm text-slate-600">
          {{ auditTargetRecord?.fqcNo || '-' }}：送检
          {{ auditTargetRecord?.submissionDetailCount || 0 }} 片，OK
          {{ auditTargetRecord?.okQty || 0 }} 片，NG
          {{ auditTargetRecord?.ngQty || 0 }} 片。
        </div>
        <Radio.Group v-model:value="auditResult">
          <Radio value="PASS">确认审核（NG片自动退回不合格待包装区）</Radio>
          <Radio value="REJECT">驳回退回重填</Radio>
        </Radio.Group>
        <Input.TextArea
          v-if="auditResult === 'REJECT'"
          v-model:value="rejectReason"
          :maxlength="500"
          :rows="4"
          placeholder="请输入驳回原因（必填）"
          show-count
        />
      </div>
    </Modal>

    <Modal
      v-model:open="pendingModalOpen"
      destroy-on-close
      :footer="null"
      title="待检发货单"
      width="1120px"
    >
      <div class="mb-3 flex items-center gap-2">
        <Input
          v-model:value="pendingKeyword"
          allow-clear
          placeholder="按发货通知单、客户、ERP订单、物料或产品型号搜索"
          @press-enter="refreshPendingList"
        />
        <Button
          :loading="pendingLoading"
          type="primary"
          @click="refreshPendingList"
        >
          搜索
        </Button>
      </div>
      <ATable
        :columns="pendingColumns"
        :data-source="pendingTasks"
        :loading="pendingLoading"
        :pagination="{ pageSize: 8, showSizeChanger: false }"
        :row-key="(row: any) => row.shippingNoticeId"
        :scroll="{ x: 980 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'existingFqcStatus'">
            <Tag
              v-if="record.existingFqcId"
              :color="statusTag(record.existingFqcStatus).color"
            >
              {{ statusTag(record.existingFqcStatus).label }}
            </Tag>
            <Tag v-else color="default">未生成</Tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <Button
              v-access:code="['mes:fg-shipping-fqc:workbench']"
              size="small"
              type="link"
              @click="createFromPending(record)"
            >
              打开/补齐FQC
            </Button>
          </template>
        </template>
      </ATable>
    </Modal>

    <div v-show="viewMode === 'LEDGER'" class="flex h-full min-h-0 flex-col">
      <Grid table-title="发货成品检验">
        <template #fqcNo="{ row }">
          <span class="text-slate-700">
            {{ row.fqcNo || row.sourceReportNo || row.reportNo || '打开' }}
          </span>
        </template>
        <template #status="{ row }">
          <Tag :color="statusTag(row.status).color">
            {{ statusTag(row.status).label }}
          </Tag>
        </template>
        <template #judgment="{ row }">
          <Tag :color="judgmentTag(row.judgment).color">
            {{ judgmentTag(row.judgment).label }}
          </Tag>
        </template>
        <template #recheckFlag="{ row }">
          <Tag :color="row.recheckFlag ? 'orange' : 'default'">
            {{ row.recheckFlag ? '复检' : '原检' }}
          </Tag>
        </template>
        <template #alignmentStatus="{ row }">
          <Tag :color="alignmentTag(row.alignmentStatus, row?.productType).color">
            {{ alignmentTag(row.alignmentStatus, row?.productType).label }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:fg-shipping-fqc:query'],
                onClick: () => openReadonlyEntry(row),
              },
              {
                label: '数据录入',
                type: 'link',
                icon: 'lucide:keyboard',
                auth: ['mes:fg-shipping-fqc:workbench'],
                onClick: () => openEntry(row),
              },
              {
                label: '审核',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:fg-shipping-fqc:confirm'],
                disabled: !canAuditRecord(row),
                onClick: () => openAudit(row),
              },
            ]"
          />
        </template>
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:fg-shipping-fqc:workbench']"
              @click="openPendingModal"
            >
              <IconifyIcon icon="lucide:list-checks" class="mr-1" /> 待检发货单
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
      </Grid>
    </div>

    <div
      v-if="viewMode === 'ENTRY'"
      class="flex h-full min-h-0 flex-col bg-white"
    >
      <div
        v-if="activeRecord"
        class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3"
      >
        <div class="flex min-w-0 items-center gap-3">
          <Button size="small" @click="backToLedger">
            <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回
          </Button>
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-semibold text-slate-900">
                {{ activeRecord.fqcNo }}
              </span>
              <Tag :color="statusTag(activeRecord.status).color">
                {{ statusTag(activeRecord.status).label }}
              </Tag>
              <Tag :color="judgmentTag(activeRecord.judgment).color">
                {{ judgmentTag(activeRecord.judgment).label }}
              </Tag>
              <Tag :color="alignmentTag(activeRecord.alignmentStatus, activeRecord?.productType).color">
                {{ alignmentTag(activeRecord.alignmentStatus, activeRecord?.productType).label }}
              </Tag>
              <Tag v-if="isReadonlyEntry" color="default">只读查看</Tag>
            </div>
            <div
              class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500"
            >
              <span>
                {{
                  activeRecord.shippingNoticeNo || activeRecord.reportNo || '-'
                }}
                <template v-if="activeRecord.productType !== 'SAMPLE'">/ {{ activeRecord.customerName || '-' }}</template> /
                {{ activeRecord.erpOrderNo || activeRecord.workOrderNo || '-' }}
                / 送检 {{ activeRecord.submissionDetailCount || 0 }} 片
              </span>
              <span>
                检验 {{ activeRecord.inspectorName || '-' }} /
                {{ displayDateTime(activeRecord.inspectionTime) }}
              </span>
              <span>
                审核 {{ activeRecord.qaInspectorName || '-' }} /
                {{ displayDateTime(activeRecord.qaTime) }}
              </span>
            </div>
            <div
              v-if="activeRecord.lastReturnReason"
              class="mt-1 text-xs text-orange-600"
            >
              最近驳回原因：{{ activeRecord.lastReturnReason }}
            </div>
            <div
              v-if="activeRecord.mismatchReason"
              class="mt-1 text-xs text-red-600"
            >
              {{ activeRecord.mismatchReason }}
            </div>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Tag color="success">OK {{ activeRecord.okQty || 0 }}</Tag>
          <Tag color="error">NG {{ activeRecord.ngQty || 0 }}</Tag>
          <Button :loading="loadingDetail" @click="refreshActiveRecord">
            <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
          </Button>
          <Button
            v-if="canSubmit()"
            v-access:code="['mes:fg-shipping-fqc:submit']"
            :loading="saving"
            type="primary"
            @click="submitInspection"
          >
            <IconifyIcon icon="lucide:send" class="mr-1" /> 提交审核
          </Button>
          <Button
            v-if="canAudit()"
            v-access:code="['mes:fg-shipping-fqc:confirm']"
            type="primary"
            @click="openAudit()"
          >
            <IconifyIcon icon="lucide:badge-check" class="mr-1" /> 审核
          </Button>
        </div>
      </div>

      <div
        v-if="activeRecord"
        class="grid min-h-0 flex-1 grid-cols-[340px_1fr] gap-3 bg-slate-50 p-3"
      >
        <div class="min-h-0 overflow-auto border border-slate-200 bg-white">
          <div
            class="border-b border-slate-200 px-3 py-2 font-semibold text-slate-800"
          >
            实际片号检验
          </div>
          <div class="divide-y divide-slate-100">
            <button
              v-for="detail in detailRows"
              :key="detail.id"
              class="block w-full px-3 py-3 text-left hover:bg-blue-50"
              :class="
                detail.id === selectedDetail?.id ? 'bg-blue-50' : 'bg-white'
              "
              @click="selectDetail(detail)"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="font-medium text-slate-800">
                  {{ detailSliceModelText(detail) }}
                </span>
                <div class="flex shrink-0 items-center gap-1">
                  <Tag
                    :color="rowJudgmentTag(detail.rowJudgment).color"
                    class="!m-0"
                  >
                    {{ rowJudgmentTag(detail.rowJudgment).label }}
                  </Tag>
                  <Tag
                    :color="alignmentTag(detail.alignmentStatus, activeRecord?.productType).color"
                    class="!m-0"
                  >
                    {{ alignmentTag(detail.alignmentStatus, activeRecord?.productType).label }}
                  </Tag>
                </div>
              </div>
              <div
                v-if="detail.mismatchReason"
                class="mt-1 line-clamp-2 text-xs text-red-600"
              >
                {{ detail.mismatchReason }}
              </div>
              <div class="mt-2 flex items-center gap-2">
                <Progress
                  :percent="detail.entryProgress || 0"
                  :show-info="false"
                  size="small"
                />
                <span class="shrink-0 text-xs text-slate-500">
                  {{ detail.completedItemCount || 0 }}/{{
                    detail.requiredItemCount || 0
                  }}
                </span>
              </div>
            </button>
          </div>
        </div>

        <div
          ref="itemPanelRef"
          class="fg-shipping-fqc-detail-panel flex min-h-0 flex-col overflow-hidden border border-slate-200 bg-white"
        >
          <div
            v-if="selectedDetail"
            class="fg-shipping-fqc-detail-header flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-3 py-2"
          >
            <div>
              <div class="font-semibold text-slate-900">
                {{ detailSliceModelText(selectedDetail) }}
              </div>
              <div class="mt-1 text-xs text-slate-500">
                检验
                {{
                  fallbackText(
                    selectedDetail.inspectorName,
                    activeRecord.inspectorName,
                  )
                }}
                /
                {{
                  displayDateTime(
                    selectedDetail.inspectionTime ||
                      activeRecord.inspectionTime,
                  )
                }}
              </div>
            </div>
            <div class="flex items-center gap-2">
              <Button
                v-access:code="['mes:fg-shipping-fqc:save']"
                :disabled="
                  !canEdit() ||
                  selectedItems.length === 0 ||
                  selectedDetailSaving
                "
                :loading="selectedDetailSaving"
                size="small"
                type="primary"
                @click="markSelectedDetailOk"
              >
                <IconifyIcon icon="lucide:check-check" class="mr-1" /> 合格
              </Button>
              <Tag :color="rowJudgmentTag(selectedDetail.rowJudgment).color">
                {{ rowJudgmentTag(selectedDetail.rowJudgment).label }}
              </Tag>
              <Tag :color="alignmentTag(selectedDetail.alignmentStatus, activeRecord?.productType).color">
                {{ alignmentTag(selectedDetail.alignmentStatus, activeRecord?.productType).label }}
              </Tag>
              <Tag color="blue">
                项目 {{ selectedDetail.completedItemCount || 0 }}/{{
                  selectedDetail.requiredItemCount || 0
                }}
              </Tag>
            </div>
          </div>

          <ATable
            v-if="selectedDetail"
            class="fg-shipping-fqc-item-table"
            :columns="itemColumns"
            :data-source="selectedItems"
            :pagination="false"
            :row-class-name="itemRowClassName"
            :row-key="(row: any) => row.id"
            :scroll="{ x: 1160, y: itemTableScrollY }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'itemType'">
                {{ itemTypeLabel(record.itemType) }}
              </template>
              <template v-if="column.dataIndex === 'itemResult'">
                <Tag :color="rowJudgmentTag(record.itemResult).color">
                  {{ rowJudgmentTag(record.itemResult).label }}
                </Tag>
              </template>
              <template v-if="column.dataIndex === 'actualValue'">
                <Input
                  v-if="canEdit() && record.actualValueRequired"
                  :value="record.actualValue"
                  allow-clear
                  placeholder="请输入实际值"
                  size="small"
                  @update:value="
                    (value) => updateItemActualValue(record, value)
                  "
                />
                <span v-else>{{ record.actualValue || '-' }}</span>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <div class="flex justify-center gap-2">
                  <Button
                    v-access:code="['mes:fg-shipping-fqc:save']"
                    :disabled="!canEdit() || isItemSaving(record.id)"
                    :loading="isItemSaving(record.id)"
                    size="small"
                    @click="markItemOk(record)"
                  >
                    OK
                  </Button>
                  <Button
                    v-access:code="['mes:fg-shipping-fqc:save']"
                    :disabled="!canEdit() || isItemSaving(record.id)"
                    danger
                    size="small"
                    @click="openNgModal(record)"
                  >
                    NG
                  </Button>
                </div>
              </template>
            </template>
          </ATable>
          <Empty v-else class="py-16" description="请选择实际片号" />
        </div>
      </div>
    </div>

    <Modal
      v-model:open="ngModalOpen"
      :body-style="{
        height: 'calc(100vh - 116px)',
        overflow: 'hidden',
        padding: '12px 16px',
      }"
      cancel-text="取消"
      ok-text="确认NG"
      :title="ngModalTitle"
      width="100vw"
      :mask-closable="false"
      wrap-class-name="fg-shipping-fqc-ng-fullscreen-modal"
      @ok="submitNgModal"
    >
      <div class="flex h-full min-h-0 flex-col gap-3">
        <Input
          v-model:value="defectPickerKeyword"
          allow-clear
          placeholder="按缺陷代码、缺陷名称或备注搜索"
          size="large"
        />

        <div class="grid min-h-0 flex-1 grid-cols-[minmax(0,1fr)_420px] gap-4">
          <div class="flex min-h-0 flex-col rounded border">
            <div class="min-h-0 flex-1 overflow-auto">
              <table class="w-full min-w-[720px] border-collapse text-sm">
                <thead class="sticky top-0 bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th class="w-12 border-b px-2 py-2 text-center">选择</th>
                    <th class="w-32 border-b px-2 py-2 text-left">缺陷代码</th>
                    <th class="w-36 border-b px-2 py-2 text-left">缺陷名称</th>
                    <th class="w-24 border-b px-2 py-2 text-left">严重等级</th>
                    <th class="w-24 border-b px-2 py-2 text-center">
                      原因参考
                    </th>
                    <th class="border-b px-2 py-2 text-left">备注</th>
                    <th class="w-20 border-b px-2 py-2 text-center">图片</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="option in pagedDefectOptions"
                    :key="option.value"
                    class="cursor-pointer border-b last:border-b-0 hover:bg-slate-50"
                    :class="{
                      'bg-blue-50': option.value === highlightedDefect?.value,
                      'text-blue-700': isDefectSelected(option.value),
                    }"
                    @click="highlightedDefectCodeId = option.value"
                  >
                    <td class="px-2 py-2 text-center">
                      <Checkbox
                        :checked="isDefectSelected(option.value)"
                        @click.stop
                        @change="
                          (event) =>
                            toggleDefectSelection(
                              option,
                              !!event?.target?.checked,
                            )
                        "
                      />
                    </td>
                    <td class="px-2 py-2 font-mono text-slate-700">
                      {{ option.defectCode }}
                    </td>
                    <td class="px-2 py-2 text-slate-700">
                      {{ option.defectName }}
                    </td>
                    <td class="px-2 py-2">
                      <Tag class="!m-0">{{ option.defectLevel || '-' }}</Tag>
                    </td>
                    <td class="px-2 py-2 text-center">
                      <Tag
                        :color="
                          normalizeCauseList(option).length > 0
                            ? 'blue'
                            : 'default'
                        "
                        class="!m-0"
                      >
                        {{ normalizeCauseList(option).length }}
                      </Tag>
                    </td>
                    <td class="px-2 py-2 text-slate-600">
                      {{ option.remark || '-' }}
                    </td>
                    <td class="px-2 py-2 text-center">
                      <Tag
                        :color="
                          hasReferenceImages(option) ? 'success' : 'default'
                        "
                        class="!m-0"
                      >
                        {{ hasReferenceImages(option) ? '有' : '无' }}
                      </Tag>
                    </td>
                  </tr>
                  <tr v-if="filteredDefectOptions.length === 0">
                    <td
                      class="px-2 py-10 text-center text-slate-400"
                      colspan="7"
                    >
                      暂无匹配缺陷码
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="flex shrink-0 justify-end border-t bg-white px-3 py-2">
              <Pagination
                :current="currentDefectPage"
                :page-size="DEFECT_PAGE_SIZE"
                :show-size-changer="false"
                :show-total="defectPageTotalText"
                :total="filteredDefectOptions.length"
                size="small"
                @change="handleDefectPageChange"
              />
            </div>
          </div>

          <aside class="min-h-0 overflow-auto rounded border p-3">
            <template v-if="highlightedDefect">
              <div class="space-y-3">
                <div>
                  <div class="text-xs font-bold text-slate-400">缺陷代码</div>
                  <div class="mt-1 font-mono text-base text-slate-800">
                    {{ highlightedDefect.defectCode }}
                  </div>
                </div>
                <div>
                  <div class="text-xs font-bold text-slate-400">缺陷名称</div>
                  <div class="mt-1 text-base font-bold text-slate-800">
                    {{ highlightedDefect.defectName }}
                  </div>
                </div>
                <div>
                  <div class="text-xs font-bold text-slate-400">严重等级</div>
                  <Tag class="!mb-0 !mt-1">
                    {{ highlightedDefect.defectLevel || '-' }}
                  </Tag>
                </div>
                <div>
                  <div class="text-xs font-bold text-slate-400">备注</div>
                  <div class="mt-1 whitespace-pre-wrap text-sm text-slate-700">
                    {{ highlightedDefect.remark || '-' }}
                  </div>
                </div>

                <div>
                  <div class="mb-2 text-xs font-bold text-slate-400">
                    发生原因参考
                  </div>
                  <div v-if="highlightedDefectCauses.length" class="space-y-2">
                    <div
                      v-for="cause in highlightedDefectCauses"
                      :key="causeKey(cause)"
                      class="rounded border bg-slate-50 p-2 text-xs text-slate-700"
                    >
                      <div class="flex items-center justify-between gap-2">
                        <span class="font-bold">
                          {{ cause.reasonCode || '-' }} -
                          {{ cause.reasonName || '-' }}
                        </span>
                        <Tag class="!m-0">{{ cause.reasonType || '-' }}</Tag>
                      </div>
                      <div
                        v-if="cause.reasonDesc"
                        class="mt-1 whitespace-pre-wrap"
                      >
                        {{ cause.reasonDesc }}
                      </div>
                      <div
                        v-if="cause.remark"
                        class="mt-1 whitespace-pre-wrap text-slate-500"
                      >
                        {{ cause.remark }}
                      </div>
                    </div>
                  </div>
                  <div v-else class="text-sm text-slate-400">
                    暂无发生原因参考
                  </div>
                </div>

                <div>
                  <div class="mb-2 text-xs font-bold text-slate-400">
                    参考缺陷图片
                  </div>
                  <div
                    v-if="highlightedDefect.referencePicUrls?.length"
                    class="flex flex-wrap gap-2"
                  >
                    <Image
                      v-for="url in highlightedDefect.referencePicUrls"
                      :key="url"
                      :height="72"
                      :src="url"
                      :width="96"
                      class="rounded border object-cover"
                    />
                  </div>
                  <div v-else class="text-sm text-slate-400">暂无参考图片</div>
                </div>
              </div>
            </template>
            <div v-else class="py-12 text-center text-sm text-slate-400">
              请选择左侧缺陷项查看参考信息
            </div>
          </aside>
        </div>

        <Input.TextArea
          v-model:value="ngDraft.remark"
          :maxlength="500"
          :rows="3"
          placeholder="备注，可填写补充说明"
          show-count
        />

        <div class="flex flex-wrap items-center gap-2 text-xs text-slate-500">
          <span>已选择 {{ ngDraft.defectCodeIds.length }} 个缺陷码</span>
          <Tag
            v-for="label in selectedDefectLabels"
            :key="label"
            color="blue"
            class="!m-0"
          >
            {{ label }}
          </Tag>
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style>
.fg-shipping-fqc-ng-fullscreen-modal .ant-modal {
  top: 0;
  max-width: 100vw;
  margin: 0;
  padding-bottom: 0;
}

.fg-shipping-fqc-ng-fullscreen-modal .ant-modal-content {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  border-radius: 0;
}

.fg-shipping-fqc-ng-fullscreen-modal .ant-modal-body {
  flex: 1;
  min-height: 0;
}

.fg-shipping-fqc-ng-fullscreen-modal .ant-modal-footer {
  margin-top: 0;
}
</style>
