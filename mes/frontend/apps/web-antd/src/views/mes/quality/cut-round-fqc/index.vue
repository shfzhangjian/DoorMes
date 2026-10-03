<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';

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

import { useAccess } from '@vben/access';
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
  Select,
  Spin,
  Tabs,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditCutRoundFqc,
  deleteCutRoundFqcItemPhoto,
  getCutRoundFqcDefectCodeOptions,
  getCutRoundFqcDetail,
  getCutRoundFqcItemPhotos,
  getCutRoundFqcPage,
  revokeCutRoundFqcAudit,
  saveCutRoundFqcItemPhoto,
  saveCutRoundFqcProgramEntry,
  saveCutRoundFqcSubmissionDetailPhotos,
  startCutRoundFqcProgramEntry,
  submitCutRoundFqcProgramEntry,
} from '#/api/mes/quality/cut-round-fqc';
import { FileUpload } from '#/components/upload';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';

import EnvironmentDailyRecordModal from '../shared/EnvironmentDailyRecordModal.vue';
import SelfCheckRecordRuntimeViewModal from '../shared/SelfCheckRecordRuntimeViewModal.vue';
import {
  ENVIRONMENT_WORKSHOPS,
  useEnvironmentDailyGuard,
} from '../shared/environmentDailyGuard';
import {
  CutRoundFqcJudgmentOptions,
  CutRoundFqcStatusOptions,
  optionMeta,
  useGridColumns,
  useGridFormSchema,
} from './data';
import QmsCutRoundFqcScanResolveModal from './modules/qms-cut-round-fqc-scan-resolve-modal.vue';
import { useCutRoundFqcSelfCheckGuard } from './shared/cutRoundFqcSelfCheck';

import './shared/cutRoundFqcSelfCheckRuntime.css';

defineOptions({ name: 'MesQualityCutRoundFqc' });

const DEFECT_PAGE_SIZE = 10;
const DETAIL_PHOTO_ACCEPT_TYPES = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'];
const DETAIL_PHOTO_MAX_COUNT = 20;
const ITEM_PHOTO_MAX_COUNT = 6;
const ITEM_PHOTO_EDITOR_Z_INDEX = 100_005;
const ITEM_PHOTO_EDITOR_DROPDOWN_Z_INDEX = 100_020;
const { hasAccessByCodes } = useAccess();

interface DefectSelectOption {
  causes: MesCutRoundFqcApi.DefectCause[];
  defectCode: string;
  defectLevel?: string;
  defectName: string;
  label: string;
  referencePicUrls?: string[];
  remark?: string;
  value: number;
}

interface FqcQualityRiskTrace {
  cutRoundDefectText: string;
  cutRoundNg: boolean;
  sourceDefectText: string;
  sourceNg: boolean;
  sourceReviewResult: 'NG' | 'OK' | '';
}

const viewMode = ref<'ENTRY' | 'LEDGER'>('LEDGER');
const entryMode = ref<'EDIT' | 'READONLY'>('EDIT');
const activeRecord = ref<MesCutRoundFqcApi.Record>();
const selectedDetailId = ref<number>();
const loadingDetail = ref(false);
const saving = ref(false);
const savingItemIds = ref<Set<number>>(new Set());
const auditModalOpen = ref(false);
const auditTargetRecord = ref<MesCutRoundFqcApi.Record>();
const auditResult = ref<'PASS' | 'REJECT'>('PASS');
const rejectReason = ref('');
const revokeAuditModalOpen = ref(false);
const revokeAuditTargetRecord = ref<MesCutRoundFqcApi.Record>();
const revokeAuditReason = ref('');
const scanResolveOpen = ref(false);
const {
  checking: selfCheckChecking,
  ensureReady: ensureCutRoundFqcSelfCheckReady,
  handleRuntimeSuccess: handleSelfCheckRuntimeSuccess,
  runtimeForm: selfCheckRuntimeForm,
  runtimeInitialParams: selfCheckRuntimeInitialParams,
  runtimeOpen: selfCheckRuntimeOpen,
  runtimeRecord: selfCheckRuntimeRecord,
  runtimeRecordOpen: selfCheckRuntimeRecordOpen,
} = useCutRoundFqcSelfCheckGuard();
const {
  ensureReady: ensureTodayEnvironmentReady,
  goFill: goFillEnvironment,
  handleRecordSaved: handleEnvironmentRecordSaved,
  handleStatusClick: handleEnvironmentStatusClick,
  loading: environmentChecking,
  recordOpen: environmentRecordOpen,
  refresh: refreshTodayEnvironment,
  status: environmentStatus,
  todayRecord: todayEnvironmentRecord,
  warnIfAbnormal: warnEnvironmentIfAbnormal,
} = useEnvironmentDailyGuard(ENVIRONMENT_WORKSHOPS.CLASS_100_ROOM);
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
const detailContentTab = ref<'ITEMS' | 'PHOTOS'>('ITEMS');
const photoSaving = ref(false);
const photoDraftUrls = ref<string[]>([]);
const itemPhotoModalOpen = ref(false);
const itemPhotoModalMaximized = ref(false);
const itemPhotoLoading = ref(false);
const itemPhotoTarget = ref<MesCutRoundFqcApi.FqcItem>();
const itemPhotoRecords = ref<MesCutRoundFqcApi.ItemPhoto[]>([]);
const itemPhotoEditorOpen = ref(false);
const itemPhotoEditorSaving = ref(false);
const itemPhotoEditorTarget = ref<MesCutRoundFqcApi.ItemPhoto>();
const itemPhotoEditorUrls = ref<string[]>([]);
const itemPhotoEditorDefectCodeIds = ref<number[]>([]);
const itemPhotoEditorRemark = ref('');
const itemPhotoCountMap = ref<Record<number, number>>({});
const isReadonlyEntry = computed(() => entryMode.value === 'READONLY');

const searchFormFields = [
  'fqcNo',
  'planNo',
  'productionBatchNo',
  'materialCode',
  'productModel',
  'status',
  'judgment',
  'recheckFlag',
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
          return await getCutRoundFqcPage({
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

const detailRows = computed(() => activeRecord.value?.submissionDetails || []);
const selectedDetail = computed(() => {
  if (detailRows.value.length === 0) return undefined;
  return (
    detailRows.value.find((item) => item.id === selectedDetailId.value) ||
    detailRows.value[0]
  );
});
const selectedDetailRiskTrace = computed(() =>
  getFqcQualityRiskTrace(selectedDetail.value),
);
const selectedDetailPhotoCount = computed(
  () => selectedDetail.value?.photoUrls?.length || 0,
);
const canSavePhotos = computed(
  () => canEdit() && hasAccessByCodes(['mes:cut-round-fqc:save']),
);
const photoUploadDirectory = computed(() => {
  const fqcNo = activeRecord.value?.fqcNo || 'draft';
  const batchNo = selectedDetail.value?.productionBatchNo || 'unknown';
  return `mes/qms/cut-round-fqc/${fqcNo}/${batchNo}`;
});
const itemPhotoUploadDirectory = computed(() => {
  const fqcNo = activeRecord.value?.fqcNo || 'draft';
  const batchNo = selectedDetail.value?.productionBatchNo || 'unknown';
  const itemId = itemPhotoTarget.value?.id || 'unknown';
  return `mes/qms/cut-round-fqc/${fqcNo}/${batchNo}/items/${itemId}`;
});
const itemPhotoModalTitle = computed(() => {
  const batchNo = selectedDetail.value?.productionBatchNo || '-';
  const itemName = itemPhotoTarget.value?.inspectionItem || '-';
  return `项目照片 - ${batchNo} / ${itemName}`;
});
const canSaveItemPhotos = computed(
  () => canSavePhotos.value && !!itemPhotoTarget.value?.id,
);
const selectedItems = computed(() => {
  const detail = selectedDetail.value;
  if (!detail) return [];
  if (detail.items && detail.items.length > 0) return detail.items;
  return (activeRecord.value?.items || []).filter(
    (item) =>
      item.submissionDetailId === detail.id ||
      item.productionBatchNo === detail.productionBatchNo,
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
      width: isReadonlyEntry.value ? 120 : 270,
    },
  ].filter(
    (column) =>
      column.dataIndex !== 'actualValue' || showActualValueColumn.value,
  ),
);

let itemPanelResizeObserver: ResizeObserver | undefined;

function updateItemTableScrollY() {
  const panel = itemPanelRef.value;
  if (!panel) return;
  const detailHeader = panel.querySelector<HTMLElement>(
    '.cut-round-fqc-detail-header',
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
    '.cut-round-fqc-detail-header',
  );
  if (detailHeader) {
    itemPanelResizeObserver.observe(detailHeader);
  }
}

function isCompletedItem(item: MesCutRoundFqcApi.FqcItem) {
  return item.itemResult === 'OK' || item.itemResult === 'NG';
}

function isNgItem(item: MesCutRoundFqcApi.FqcItem) {
  return item.itemResult === 'NG' || item.qaResult === 'NG';
}

function hasNgRecord(item: MesCutRoundFqcApi.FqcItem) {
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
  record: MesCutRoundFqcApi.Record,
) {
  const keyedItems = new Map<number, MesCutRoundFqcApi.FqcItem>();
  const unkeyedItems: MesCutRoundFqcApi.FqcItem[] = [];
  const collect = (item: MesCutRoundFqcApi.FqcItem) => {
    if (item.id) {
      keyedItems.set(item.id, { ...(keyedItems.get(item.id) || {}), ...item });
      return;
    }
    unkeyedItems.push(item);
  };
  (record.submissionDetails || []).forEach((detail) => {
    (detail.items || []).forEach(collect);
  });
  (record.items || []).forEach(collect);

  return [...keyedItems.values(), ...unkeyedItems].filter(
    (item) =>
      item.actualValueRequired && !normalizeActualValue(item.actualValue),
  );
}

function buildActualValueMissingText(items: MesCutRoundFqcApi.FqcItem[]) {
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
  record: MesCutRoundFqcApi.Record,
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
  detail: MesCutRoundFqcApi.SubmissionDetail,
): MesCutRoundFqcApi.SubmissionDetail {
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

function hasSubmissionDetails(record?: MesCutRoundFqcApi.Record) {
  return !!record?.submissionDetails?.length;
}

function resolveSelectedDetailId(
  record?: MesCutRoundFqcApi.Record,
  preferredDetailId?: number,
) {
  const details = record?.submissionDetails || [];
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
  record: MesCutRoundFqcApi.Record,
  itemId: number,
  updater: (item: MesCutRoundFqcApi.FqcItem) => MesCutRoundFqcApi.FqcItem,
) {
  const patchItem = (row: MesCutRoundFqcApi.FqcItem) =>
    row.id === itemId ? updater(row) : row;

  return {
    ...record,
    items: (record.items || []).map(patchItem),
    submissionDetails: (record.submissionDetails || []).map((detail) => ({
      ...withDetailStats({
        ...detail,
        items: (detail.items || []).map(patchItem),
      }),
    })),
  };
}

function updateItemActualValue(item: MesCutRoundFqcApi.FqcItem, value: string) {
  if (!activeRecord.value || !item.id) return;
  activeRecord.value = patchRecordItems(activeRecord.value, item.id, (row) => ({
    ...row,
    actualValue: value,
  }));
}

function findRecordItemById(record: MesCutRoundFqcApi.Record, itemId: number) {
  const detailItem = (record.submissionDetails || [])
    .flatMap((detail) => detail.items || [])
    .find((row) => row.id === itemId);
  return detailItem || (record.items || []).find((row) => row.id === itemId);
}

function mergeItemList(
  previousItems: MesCutRoundFqcApi.FqcItem[],
  nextItems: MesCutRoundFqcApi.FqcItem[],
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
  next: MesCutRoundFqcApi.Record,
  previous: MesCutRoundFqcApi.Record,
) {
  const latestItems = mergeItemList(previous.items || [], next.items || []);
  const latestItemMap = new Map(
    latestItems
      .filter((item) => item.id)
      .map((item) => [item.id as number, item]),
  );
  const nextDetailMap = new Map(
    (next.submissionDetails || []).map((detail) => [detail.id, detail]),
  );
  const submissionDetails = (previous.submissionDetails || []).map((detail) => {
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
    (previous.submissionDetails || []).map((detail) => detail.id),
  );
  (next.submissionDetails || []).forEach((detail) => {
    if (!previousDetailIds.has(detail.id)) {
      submissionDetails.push(detail);
    }
  });

  return {
    ...previous,
    ...next,
    items: latestItems,
    submissionDetailCount:
      next.submissionDetailCount && next.submissionDetailCount > 0
        ? next.submissionDetailCount
        : previous.submissionDetailCount || submissionDetails.length,
    submissionDetails,
  };
}

async function normalizeReturnedRecord(
  next: MesCutRoundFqcApi.Record,
  previous?: MesCutRoundFqcApi.Record,
) {
  if (hasSubmissionDetails(next) || !hasSubmissionDetails(previous)) {
    return next;
  }
  try {
    const fresh = await getCutRoundFqcDetail(next.id || previous.id);
    if (hasSubmissionDetails(fresh)) {
      return fresh;
    }
  } catch {
    // 保存已成功但详情回读失败时，保留当前明细，避免工作台被空响应覆盖。
  }
  return mergeRecordKeepingDetails(next, previous);
}

async function applyReturnedRecord(
  next: MesCutRoundFqcApi.Record,
  previous?: MesCutRoundFqcApi.Record,
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

function filterRecheckRecord(record: MesCutRoundFqcApi.Record) {
  if (!record.recheckFlag) return record;
  const submissionDetails = (record.submissionDetails || []).filter(
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
    submissionDetails: submissionDetails.length
      ? submissionDetails
      : record.submissionDetails,
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

watch(
  selectedDetail,
  (detail) => {
    syncPhotoDraftFromDetail(detail);
  },
  { immediate: true },
);

watch(
  () =>
    [
      viewMode.value,
      activeRecord.value?.id,
      selectedDetail.value?.id,
      selectedItems.value.map((item) => item.id || '').join(','),
    ].join('|'),
  () => {
    if (viewMode.value === 'ENTRY') {
      void refreshSelectedItemPhotoCounts();
    }
  },
  { immediate: true },
);

function statusTag(value?: string) {
  return optionMeta(CutRoundFqcStatusOptions, value);
}

function judgmentTag(value?: string) {
  return optionMeta(CutRoundFqcJudgmentOptions, value || 'PENDING');
}

function rowJudgmentTag(value?: string) {
  if (value === 'OK') return { color: 'success', label: 'OK' };
  if (value === 'NG') return { color: 'error', label: 'NG' };
  return { color: 'default', label: '待判定' };
}

function qualityRiskTag(value?: string) {
  if (value === 'ADHESIVE2_NG') return { color: 'orange', label: '粘胶2风险' };
  if (value === 'CUT_ROUND_NG') return { color: 'volcano', label: '裁切风险' };
  if (value === 'BOTH_NG') return { color: 'error', label: '双重风险' };
  return { color: 'default', label: '无过程风险' };
}

function getCutRoundFqcQtimeText(qtime?: MesCutRoundFqcApi.QtimeInfo) {
  if (!qtime) return '-';
  if (qtime.status === 'WAITING_SOURCE_FINISH') return '0分钟（并行开工）';
  if (qtime.status === 'MISSING_SOURCE_TIME') return '裁切未完工';
  if (qtime.status === 'NO_RULE') return '未配置';
  const minutes = Math.max(0, Number(qtime.elapsedMinutes || 0));
  return qtime.timeout ? `超时 ${minutes}分钟` : `${minutes}分钟`;
}

function getCutRoundFqcQtimeColor(qtime?: MesCutRoundFqcApi.QtimeInfo) {
  if (qtime?.timeout) return 'red';
  if (qtime?.status === 'WAITING_SOURCE_FINISH') return 'blue';
  if (qtime?.status === 'NORMAL') return 'green';
  return 'default';
}

function getFqcQualityRiskTrace(
  detail?: MesCutRoundFqcApi.SubmissionDetail,
): FqcQualityRiskTrace {
  const emptyTrace: FqcQualityRiskTrace = {
    cutRoundDefectText: '',
    cutRoundNg: false,
    sourceDefectText: '',
    sourceNg: false,
    sourceReviewResult: '',
  };
  if (!detail?.qualityRiskSnapshotJson) return emptyTrace;
  try {
    const snapshot = JSON.parse(detail.qualityRiskSnapshotJson) as Record<
      string,
      any
    >;
    const readDefectText = (items: unknown, fallbackValues: unknown[]) => {
      const defectNames = Array.isArray(items)
        ? items
            .map(
              (item: Record<string, any>) =>
                item.defectName || item.defectCode || item.remark,
            )
            .filter(Boolean)
        : [];
      const fallbackText = fallbackValues
        .map((value) => String(value || '').trim())
        .find(Boolean);
      return defectNames.join('；') || fallbackText || '';
    };
    const sourceNg =
      snapshot.sourceNg === true ||
      ['ADHESIVE2_NG', 'BOTH_NG'].includes(
        String(detail.qualityRiskFlag || '').toUpperCase(),
      );
    const cutRoundNg =
      snapshot.cutRoundNg === true ||
      String(snapshot.cutRoundSelfCheck || '').toUpperCase() === 'NG';
    const reviewResult = String(
      snapshot.sourceNgReviewResult || '',
    ).toUpperCase();
    return {
      cutRoundDefectText: cutRoundNg
        ? readDefectText(snapshot.cutRoundDefectItems, [
            detail.defectName,
            detail.defectCode,
            snapshot.cutRoundReason,
          ])
        : '',
      cutRoundNg,
      sourceDefectText: sourceNg
        ? readDefectText(snapshot.sourceDefectItems, [
            snapshot.sourceDefectCode,
            snapshot.sourceReason,
            snapshot.sourceQualityLockReason,
          ])
        : '',
      sourceNg,
      sourceReviewResult:
        reviewResult === 'OK' || reviewResult === 'NG' ? reviewResult : '',
    };
  } catch {
    return emptyTrace;
  }
}

function itemTypeLabel(value?: string) {
  if (value === 'QUALITATIVE') return '定性';
  if (value === 'QUANTITATIVE') return '定量';
  return value || '-';
}

function causeKey(cause: MesCutRoundFqcApi.DefectCause) {
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
  const list = await getCutRoundFqcDefectCodeOptions();
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
    ['INSPECTING', 'PENDING'].includes(activeRecord.value.status) &&
    !activeRecord.value.sheetLocked
  );
}

function canSubmit() {
  return (
    !isReadonlyEntry.value &&
    !!activeRecord.value &&
    ['INSPECTING', 'PENDING'].includes(activeRecord.value.status)
  );
}

function canAuditRecord(record?: MesCutRoundFqcApi.Record) {
  return record?.status === 'WAITING_QA';
}

function canRevokeAuditRecord(record?: MesCutRoundFqcApi.Record) {
  return record?.status === 'COMPLETED';
}

function canAudit() {
  return !isReadonlyEntry.value && canAuditRecord(activeRecord.value);
}

function canRevokeAudit() {
  return !isReadonlyEntry.value && canRevokeAuditRecord(activeRecord.value);
}

async function openEntry(
  row: MesCutRoundFqcApi.Record,
  detailId?: number,
  mode: 'EDIT' | 'READONLY' = 'EDIT',
) {
  if (!row.id) return;
  loadingDetail.value = true;
  try {
    if (mode === 'EDIT') {
      await startCutRoundFqcProgramEntry(row.id);
    }
    const record = await getCutRoundFqcDetail(row.id);
    await applyReturnedRecord(record, undefined, detailId);
    entryMode.value = mode;
    viewMode.value = 'ENTRY';
  } finally {
    loadingDetail.value = false;
  }
}

async function openReadonlyEntry(row: MesCutRoundFqcApi.Record) {
  await openEntry(row, undefined, 'READONLY');
}

async function ensureCutRoundFqcEntryReady(
  continuation?: () => Promise<void> | void,
) {
  if (!(await ensureTodayEnvironmentReady())) return false;
  warnEnvironmentIfAbnormal();
  return ensureCutRoundFqcSelfCheckReady(continuation);
}

async function handleOpenEditableEntry(
  row: MesCutRoundFqcApi.Record,
  detailId?: number,
) {
  await ensureCutRoundFqcEntryReady(() => openEntry(row, detailId, 'EDIT'));
}

async function handleOpenScanResolve() {
  await ensureCutRoundFqcEntryReady(() => {
    scanResolveOpen.value = true;
  });
}

function resolveScanEntryMode(
  status?: MesCutRoundFqcApi.Status,
  sheetLocked?: boolean,
) {
  return ['INSPECTING', 'PENDING'].includes(status || '') && !sheetLocked
    ? 'EDIT'
    : 'READONLY';
}

async function handleScanResolved(resp: MesCutRoundFqcApi.ScanResp) {
  if (!resp.record?.id) return;
  const mode = resolveScanEntryMode(
    resp.record.status,
    resp.record.sheetLocked,
  );
  if (mode === 'EDIT') {
    await handleOpenEditableEntry(resp.record, resp.detail?.id);
    return;
  }
  await openEntry(resp.record, resp.detail?.id, mode);
}

async function handleScanCandidateSelected(
  candidate: MesCutRoundFqcApi.ScanCandidate,
) {
  const record = {
    fqcNo: candidate.fqcNo || '-',
    id: candidate.fqcId,
    judgment: 'PENDING',
    status: candidate.fqcStatus || 'PENDING',
  } as MesCutRoundFqcApi.Record;
  const mode = resolveScanEntryMode(candidate.fqcStatus);
  if (mode === 'EDIT') {
    await handleOpenEditableEntry(record, candidate.submissionDetailId);
    return;
  }
  await openEntry(record, candidate.submissionDetailId, mode);
}

async function refreshActiveRecord() {
  const previousRecord = activeRecord.value;
  if (!previousRecord?.id) return;
  const currentDetailId = selectedDetailId.value;
  const record = await getCutRoundFqcDetail(previousRecord.id);
  await applyReturnedRecord(record, previousRecord, currentDetailId);
}

function backToLedger() {
  viewMode.value = 'LEDGER';
  entryMode.value = 'EDIT';
  gridApi.query();
}

function selectDetail(detail: MesCutRoundFqcApi.SubmissionDetail) {
  selectedDetailId.value = detail.id;
}

function switchDetailContentTab(tab: 'ITEMS' | 'PHOTOS') {
  detailContentTab.value = tab;
  if (tab === 'PHOTOS') {
    syncPhotoDraftFromDetail();
  }
  scheduleItemTableScrollUpdate();
}

function handleDetailContentTabChange(activeKey: string) {
  switchDetailContentTab(activeKey === 'PHOTOS' ? 'PHOTOS' : 'ITEMS');
}

function syncPhotoDraftFromDetail(
  detail: MesCutRoundFqcApi.SubmissionDetail | undefined = selectedDetail.value,
) {
  photoDraftUrls.value = [...(detail?.photoUrls || [])];
}

function handlePhotoPreview(file: {
  response?: any;
  thumbUrl?: string;
  url?: string;
}) {
  const url =
    file?.url || file?.thumbUrl || file?.response?.url || file?.response;
  if (!url) {
    message.warning('当前照片尚未生成可访问地址');
    return;
  }
  window.open(String(url), '_blank', 'noopener,noreferrer');
}

async function saveSelectedDetailPhotos() {
  if (!canSavePhotos.value) {
    return;
  }
  const record = activeRecord.value;
  const detailId = selectedDetail.value?.id;
  if (!record?.id || !detailId) return;
  if (!(await ensureCutRoundFqcEntryReady())) return;
  photoSaving.value = true;
  try {
    const savedRecord = await saveCutRoundFqcSubmissionDetailPhotos({
      fqcId: record.id,
      submissionDetailId: detailId,
      photoUrls: [...photoDraftUrls.value],
    });
    await applyReturnedRecord(savedRecord, record, detailId);
    message.success('片级照片已保存');
    detailContentTab.value = 'PHOTOS';
  } finally {
    photoSaving.value = false;
  }
}

let itemPhotoCountRequestSequence = 0;

function getItemPhotoCount(item: MesCutRoundFqcApi.FqcItem) {
  return item.id ? itemPhotoCountMap.value[item.id] : undefined;
}

async function refreshSelectedItemPhotoCounts() {
  const requestSequence = ++itemPhotoCountRequestSequence;
  const fqcId = activeRecord.value?.id;
  const submissionDetailId = selectedDetail.value?.id;
  const itemIds = selectedItems.value
    .map((item) => item.id)
    .filter((itemId): itemId is number => !!itemId);
  if (!fqcId || !submissionDetailId || itemIds.length === 0) {
    itemPhotoCountMap.value = {};
    return;
  }
  const rows = await Promise.all(
    itemIds.map(async (fqcItemId) => {
      try {
        const photos = await getCutRoundFqcItemPhotos(
          fqcId,
          submissionDetailId,
          fqcItemId,
        );
        return [fqcItemId, photos?.length || 0] as const;
      } catch {
        return [fqcItemId, undefined] as const;
      }
    }),
  );
  if (requestSequence !== itemPhotoCountRequestSequence) return;
  itemPhotoCountMap.value = Object.fromEntries(
    rows.filter(
      (row): row is readonly [number, number] => row[1] !== undefined,
    ),
  );
}

function resetItemPhotoModal() {
  itemPhotoModalMaximized.value = false;
  itemPhotoTarget.value = undefined;
  itemPhotoRecords.value = [];
  resetItemPhotoEditor();
}

function resetItemPhotoEditor() {
  itemPhotoEditorTarget.value = undefined;
  itemPhotoEditorUrls.value = [];
  itemPhotoEditorDefectCodeIds.value = [];
  itemPhotoEditorRemark.value = '';
}

async function loadItemPhotos(item = itemPhotoTarget.value) {
  const fqcId = activeRecord.value?.id;
  const submissionDetailId = selectedDetail.value?.id;
  if (!fqcId || !submissionDetailId || !item?.id) return;
  itemPhotoLoading.value = true;
  try {
    const photos = await getCutRoundFqcItemPhotos(
      fqcId,
      submissionDetailId,
      item.id,
    );
    itemPhotoRecords.value = photos || [];
    itemPhotoCountMap.value = {
      ...itemPhotoCountMap.value,
      [item.id]: photos?.length || 0,
    };
  } finally {
    itemPhotoLoading.value = false;
  }
}

async function openItemPhotoModal(item: MesCutRoundFqcApi.FqcItem) {
  if (!activeRecord.value?.id || !selectedDetail.value?.id || !item.id) {
    message.warning('当前检验项目缺少照片关联主键，请刷新后重试');
    return;
  }
  itemPhotoTarget.value = item;
  itemPhotoRecords.value = [];
  itemPhotoModalOpen.value = true;
  await loadItemPhotos(item);
}

function resolveItemPhotoScene(
  item: MesCutRoundFqcApi.FqcItem,
): MesCutRoundFqcApi.ItemPhotoSaveReq['photoScene'] {
  if (activeRecord.value?.recheckFlag || item.recheckItemFlag) return 'RECHECK';
  return hasNgRecord(item) ? 'DEFECT' : 'RESULT';
}

async function openItemPhotoUploadDialog() {
  if (!canSaveItemPhotos.value) return;
  if (itemPhotoRecords.value.length >= ITEM_PHOTO_MAX_COUNT) {
    message.warning(`每个检验项目最多上传${ITEM_PHOTO_MAX_COUNT}张照片`);
    return;
  }
  resetItemPhotoEditor();
  await loadDefectOptions();
  itemPhotoEditorOpen.value = true;
}

async function openItemPhotoEditDialog(photo: MesCutRoundFqcApi.ItemPhoto) {
  if (!canSaveItemPhotos.value) return;
  await loadDefectOptions();
  itemPhotoEditorTarget.value = photo;
  itemPhotoEditorUrls.value = [photo.photoUrl];
  itemPhotoEditorDefectCodeIds.value = (photo.defects || [])
    .map((defect) => defect.defectCodeId)
    .filter((id): id is number => !!id);
  itemPhotoEditorRemark.value = photo.remark || '';
  itemPhotoEditorOpen.value = true;
}

async function saveItemPhotoEditor() {
  const record = activeRecord.value;
  const detail = selectedDetail.value;
  const item = itemPhotoTarget.value;
  if (!canSaveItemPhotos.value || !record?.id || !detail?.id || !item?.id) {
    return;
  }
  if (!(await ensureCutRoundFqcEntryReady())) return;
  const photoUrl = itemPhotoEditorUrls.value[0]?.trim();
  if (!photoUrl) {
    message.warning('请先选择需要上传的照片');
    return;
  }
  const existingPhoto = itemPhotoEditorTarget.value;
  itemPhotoEditorSaving.value = true;
  try {
    await saveCutRoundFqcItemPhoto({
      defects: itemPhotoEditorDefectCodeIds.value.map(
        (defectCodeId, defectIndex) => ({
          defectCodeId,
          sort: (defectIndex + 1) * 10,
        }),
      ),
      fqcId: record.id,
      fqcItemId: item.id,
      photoScene: existingPhoto?.photoScene || resolveItemPhotoScene(item),
      photoUrl,
      remark: itemPhotoEditorRemark.value.trim() || undefined,
      sampleId: existingPhoto?.sampleId,
      sort: existingPhoto?.sort || (itemPhotoRecords.value.length + 1) * 10,
      submissionDetailId: detail.id,
    });
    itemPhotoEditorOpen.value = false;
    await loadItemPhotos(item);
    message.success(existingPhoto ? '照片信息已更新' : '检验项目照片已上传');
  } finally {
    itemPhotoEditorSaving.value = false;
  }
}

function deleteItemPhotoRecord(photo: MesCutRoundFqcApi.ItemPhoto) {
  const recordId = activeRecord.value?.id;
  const item = itemPhotoTarget.value;
  if (!recordId || !item?.id || !canSaveItemPhotos.value) return;
  Modal.confirm({
    cancelText: '取消',
    content: '删除照片不会删除已经保存到检验项目中的正式NG缺陷记录。',
    okText: '确认删除',
    okType: 'danger',
    title: '确认删除该项目照片？',
    async onOk() {
      await deleteCutRoundFqcItemPhoto(recordId, photo.id);
      await loadItemPhotos(item);
      message.success('项目照片已删除');
    },
  });
}

function buildSampleValues(
  item: MesCutRoundFqcApi.FqcItem,
  judgment: MesCutRoundFqcApi.Judgment,
  extra: {
    defects?: MesCutRoundFqcApi.FqcSampleDefect[];
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
  item: MesCutRoundFqcApi.FqcItem,
  judgment: MesCutRoundFqcApi.Judgment,
  extra: {
    defects?: MesCutRoundFqcApi.FqcSampleDefect[];
    remark?: string;
  } = {},
) {
  if (!activeRecord.value || !item.id) return;
  if (isItemSaving(item.id)) return;
  if (!(await ensureCutRoundFqcEntryReady())) return;
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
    const savedRecord = await saveCutRoundFqcProgramEntry(patchedRecord, [
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

async function markItemOk(item: MesCutRoundFqcApi.FqcItem) {
  await applyItemJudgment(item, 'OK');
}

async function markSelectedDetailOk() {
  if (!activeRecord.value || !selectedDetail.value) return;
  if (!canEdit()) {
    message.warning('当前单据状态不可修改');
    return;
  }
  if (!(await ensureCutRoundFqcEntryReady())) return;
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
  const patchItem = (row: MesCutRoundFqcApi.FqcItem) =>
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
    submissionDetails: (activeRecord.value.submissionDetails || []).map(
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
      .filter(Boolean) as MesCutRoundFqcApi.FqcItem[];
    const savedRecord = await saveCutRoundFqcProgramEntry(
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

async function openNgModal(item: MesCutRoundFqcApi.FqcItem) {
  if (item.id && isItemSaving(item.id)) return;
  if (!(await ensureCutRoundFqcEntryReady())) return;
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
  const item = (activeRecord.value?.items || []).find(
    (row) => row.id === ngDraft.itemId,
  );
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
  if (!(await ensureCutRoundFqcEntryReady())) return;
  if (!validateRequiredActualValuesBeforeSubmit(previousRecord)) return;
  const currentDetailId = selectedDetail.value?.id;
  saving.value = true;
  try {
    const savedRecord = await submitCutRoundFqcProgramEntry(previousRecord);
    await applyReturnedRecord(savedRecord, previousRecord, currentDetailId);
    message.success('裁切成品检验已提交审核');
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

function openAudit(record = activeRecord.value) {
  if (!record?.id) return;
  if (!canAuditRecord(record)) {
    message.warning('裁切成品检验提交检测结果后才允许审核');
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

function openRevokeAudit(record = activeRecord.value) {
  if (!record?.id) return;
  if (!canRevokeAuditRecord(record)) {
    message.warning('仅已完成的裁切成品检验单允许撤销审核');
    return;
  }
  revokeAuditTargetRecord.value = record;
  revokeAuditReason.value = '';
  revokeAuditModalOpen.value = true;
}

function resetRevokeAuditModal() {
  revokeAuditTargetRecord.value = undefined;
  revokeAuditReason.value = '';
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
    const savedRecord = await auditCutRoundFqc(
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
      result === 'PASS'
        ? '裁切成品检验已审核完成'
        : '裁切成品检验已驳回，已退回重新填写',
    );
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function submitRevokeAudit() {
  const record = revokeAuditTargetRecord.value;
  const reason = revokeAuditReason.value.trim();
  if (!record?.id) return;
  if (!reason) {
    message.warning('请填写撤销审核原因');
    return;
  }
  saving.value = true;
  try {
    const previousRecord = activeRecord.value;
    const savedRecord = await revokeCutRoundFqcAudit(record.id, reason);
    if (previousRecord?.id === record.id) {
      await applyReturnedRecord(
        savedRecord,
        previousRecord,
        selectedDetailId.value,
      );
    }
    revokeAuditModalOpen.value = false;
    resetRevokeAuditModal();
    message.success('已撤销审核，单据已回到待审核状态');
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

function itemRowClassName(record: MesCutRoundFqcApi.FqcItem) {
  if (record.itemResult === 'NG') return 'bg-red-50';
  if (record.itemResult === 'OK') return 'bg-emerald-50';
  return '';
}

function displayDateTime(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}

onActivated(() => {
  void refreshTodayEnvironment();
  if (viewMode.value === 'ENTRY' && activeRecord.value?.id) {
    void refreshActiveRecord();
  }
  scheduleItemTableScrollUpdate();
});

onMounted(() => {
  window.addEventListener('resize', scheduleItemTableScrollUpdate);
  void refreshTodayEnvironment();
  scheduleItemTableScrollUpdate();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', scheduleItemTableScrollUpdate);
  itemPanelResizeObserver?.disconnect();
});
</script>

<template>
  <Page auto-content-height>
    <StationFormRuntimeFillModal
      v-model:open="selfCheckRuntimeOpen"
      confirm-auth-action-name="确认裁切成品检验自检记录"
      enable-confirm
      :form="selfCheckRuntimeForm"
      :initial-params="selfCheckRuntimeInitialParams"
      readonly-pass-work-header
      save-auth-action-name="保存裁切成品检验自检记录"
      skip-business-param-step
      @success="handleSelfCheckRuntimeSuccess"
    />
    <SelfCheckRecordRuntimeViewModal
      v-model:open="selfCheckRuntimeRecordOpen"
      confirm-auth-action-name="确认裁切成品检验自检记录"
      empty-message="加载裁切成品检验自检记录详情失败"
      fallback-process-code="CUT_ROUND_FQC_SELF_CHECK"
      fallback-process-name="裁切成品检验自检记录"
      :record="selfCheckRuntimeRecord"
      @success="handleSelfCheckRuntimeSuccess"
    />

    <Modal
      v-model:open="auditModalOpen"
      :confirm-loading="saving"
      destroy-on-close
      title="裁切成品检验审核"
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
          <Radio value="PASS">审核通过并按片级结果放行</Radio>
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
      v-model:open="revokeAuditModalOpen"
      :confirm-loading="saving"
      destroy-on-close
      title="撤销裁切成品检验审核"
      @cancel="resetRevokeAuditModal"
      @ok="submitRevokeAudit"
    >
      <div class="space-y-4">
        <div class="text-sm text-slate-600">
          {{ revokeAuditTargetRecord?.fqcNo || '-' }}
          将退回待审核状态；保留片级判定和检验记录，
          但会清除本次审核与放行结果。
        </div>
        <Input.TextArea
          v-model:value="revokeAuditReason"
          :maxlength="500"
          :rows="4"
          placeholder="请输入撤销审核原因（必填）"
          show-count
        />
      </div>
    </Modal>

    <QmsCutRoundFqcScanResolveModal
      v-model:open="scanResolveOpen"
      :current-fqc-id="viewMode === 'ENTRY' ? activeRecord?.id : undefined"
      :scene="viewMode === 'ENTRY' ? 'WORKBENCH_HEADER' : 'LEDGER_TOOLBAR'"
      @candidate-selected="handleScanCandidateSelected"
      @resolved="handleScanResolved"
    />
    <EnvironmentDailyRecordModal
      v-model:open="environmentRecordOpen"
      :record="todayEnvironmentRecord"
      :workshop-code="ENVIRONMENT_WORKSHOPS.CLASS_100_ROOM.code"
      :workshop-name="ENVIRONMENT_WORKSHOPS.CLASS_100_ROOM.name"
      @go-confirm="goFillEnvironment"
      @saved="handleEnvironmentRecordSaved"
    />

    <div v-show="viewMode === 'LEDGER'" class="flex h-full min-h-0 flex-col">
      <Grid table-title="裁切成品检验">
        <template #fqcNo="{ row }">
          <span class="text-slate-700">
            {{ row.fqcNo || '打开' }}
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
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:cut-round-fqc:query'],
                onClick: () => openReadonlyEntry(row),
              },
              {
                label: '数据录入',
                type: 'link',
                icon: 'lucide:keyboard',
                auth: ['mes:cut-round-fqc:workbench'],
                onClick: () => handleOpenEditableEntry(row),
              },
              {
                label: '审核',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:cut-round-fqc:confirm'],
                disabled: !canAuditRecord(row),
                onClick: () => openAudit(row),
              },
              {
                label: '撤销审核',
                type: 'link',
                icon: 'lucide:rotate-ccw',
                auth: ['mes:cut-round-fqc:revoke-audit'],
                disabled: !canRevokeAuditRecord(row),
                onClick: () => openRevokeAudit(row),
              },
            ]"
          />
        </template>
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Tag
              :color="environmentStatus.color"
              class="!m-0 cursor-pointer"
              @click="handleEnvironmentStatusClick"
            >
              <IconifyIcon icon="lucide:thermometer" class="mr-1" />
              {{ environmentStatus.text }}
            </Tag>
            <Button
              v-access:code="['mes:cut-round-fqc:scan']"
              :loading="selfCheckChecking || environmentChecking"
              type="primary"
              @click="handleOpenScanResolve"
            >
              <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码填写
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
              <Tag v-if="isReadonlyEntry" color="default">只读查看</Tag>
            </div>
            <div
              class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500"
            >
              <span>
                {{ activeRecord.workOrderNo || '-' }} /
                {{ activeRecord.materialCode || '-' }} / 送检
                {{ activeRecord.submissionDetailCount || 0 }} 片
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
              最近退回/撤销原因：{{ activeRecord.lastReturnReason }}
            </div>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Tag color="success">OK {{ activeRecord.okQty || 0 }}</Tag>
          <Tag color="error">NG {{ activeRecord.ngQty || 0 }}</Tag>
          <Tag
            :color="environmentStatus.color"
            class="!m-0 cursor-pointer"
            @click="handleEnvironmentStatusClick"
          >
            <IconifyIcon icon="lucide:thermometer" class="mr-1" />
            {{ environmentStatus.text }}
          </Tag>
          <Button
            v-access:code="['mes:cut-round-fqc:scan']"
            :loading="selfCheckChecking || environmentChecking"
            @click="handleOpenScanResolve"
          >
            <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码定位
          </Button>
          <Button :loading="loadingDetail" @click="refreshActiveRecord">
            <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
          </Button>
          <Button
            v-if="canSubmit()"
            v-access:code="['mes:cut-round-fqc:submit']"
            :loading="saving"
            type="primary"
            @click="submitInspection"
          >
            <IconifyIcon icon="lucide:send" class="mr-1" /> 提交审核
          </Button>
          <Button
            v-if="canAudit()"
            v-access:code="['mes:cut-round-fqc:confirm']"
            type="primary"
            @click="openAudit()"
          >
            <IconifyIcon icon="lucide:badge-check" class="mr-1" /> 审核
          </Button>
          <Button
            v-if="canRevokeAudit()"
            v-access:code="['mes:cut-round-fqc:revoke-audit']"
            danger
            @click="openRevokeAudit()"
          >
            <IconifyIcon icon="lucide:rotate-ccw" class="mr-1" /> 撤销审核
          </Button>
        </div>
      </div>

      <div
        v-if="activeRecord"
        class="grid min-h-0 flex-1 grid-cols-[320px_1fr] gap-3 bg-slate-50 p-3"
      >
        <div class="min-h-0 overflow-auto border border-slate-200 bg-white">
          <div
            class="border-b border-slate-200 px-3 py-2 font-semibold text-slate-800"
          >
            送检片号
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
                  {{ detail.productionBatchNo }}
                </span>
                <Tag
                  :color="rowJudgmentTag(detail.rowJudgment).color"
                  class="!m-0"
                >
                  {{ rowJudgmentTag(detail.rowJudgment).label }}
                </Tag>
              </div>
              <div class="mt-1 text-xs text-slate-500">
                {{ detail.parentProductionBatchNo || '-' }} /
                {{ detail.modelCode || '-' }}
                <Tag
                  v-if="
                    detail.qualityRiskFlag && detail.qualityRiskFlag !== 'NONE'
                  "
                  :color="qualityRiskTag(detail.qualityRiskFlag).color"
                  class="!ml-2 !mr-0"
                >
                  {{ qualityRiskTag(detail.qualityRiskFlag).label }}
                </Tag>
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
          class="cut-round-fqc-detail-panel flex min-h-0 flex-col overflow-hidden border border-slate-200 bg-white"
        >
          <div
            v-if="selectedDetail"
            class="cut-round-fqc-detail-header flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-3 py-2"
          >
            <div>
              <div class="font-semibold text-slate-900">
                {{ selectedDetail.productionBatchNo }}
              </div>
              <div
                class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500"
              >
                <span>
                  父批次 {{ selectedDetail.parentProductionBatchNo || '-' }} /
                  物料 {{ selectedDetail.materialCode || '-' }} / 规格
                  {{ selectedDetail.sizeRule || '-' }}
                </span>
                <span>
                  检验
                  {{
                    selectedDetail.inspectorName ||
                    activeRecord.inspectorName ||
                    '-'
                  }}
                  /
                  {{
                    displayDateTime(
                      selectedDetail.inspectionTime ||
                        activeRecord.inspectionTime,
                    )
                  }}
                </span>
                <Tag
                  :color="getCutRoundFqcQtimeColor(activeRecord.qtime)"
                  :title="activeRecord.qtime?.message"
                >
                  本批裁切→FQC QTIME
                  {{ getCutRoundFqcQtimeText(activeRecord.qtime) }}
                </Tag>
              </div>
              <div
                v-if="
                  selectedDetailRiskTrace.sourceNg ||
                  selectedDetailRiskTrace.cutRoundNg
                "
                class="mt-1 text-xs text-orange-600"
              >
                <div v-if="selectedDetailRiskTrace.sourceNg">
                  粘胶2来源 NG：{{
                    selectedDetailRiskTrace.sourceDefectText ||
                    '已带入来源风险快照'
                  }}
                  <span v-if="selectedDetailRiskTrace.sourceReviewResult">
                    ；裁切外观复核：{{
                      selectedDetailRiskTrace.sourceReviewResult
                    }}
                  </span>
                </div>
                <div v-if="selectedDetailRiskTrace.cutRoundNg">
                  裁切外观复核 NG：{{
                    selectedDetailRiskTrace.cutRoundDefectText || '待确认缺陷'
                  }}
                </div>
                <div v-if="selectedDetailRiskTrace.cutRoundNg">
                  FQC 待复检项目：{{
                    selectedDetailRiskTrace.cutRoundDefectText || '待确认缺陷'
                  }}；可逐项复检为合格。
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <Button
                v-access:code="['mes:cut-round-fqc:save']"
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
              <Tag
                v-if="
                  selectedDetail.qualityRiskFlag &&
                  selectedDetail.qualityRiskFlag !== 'NONE'
                "
                :color="qualityRiskTag(selectedDetail.qualityRiskFlag).color"
              >
                {{ qualityRiskTag(selectedDetail.qualityRiskFlag).label }}
              </Tag>
              <Tag color="blue">
                项目 {{ selectedDetail.completedItemCount || 0 }}/{{
                  selectedDetail.requiredItemCount || 0
                }}
              </Tag>
            </div>
          </div>

          <Tabs
            v-if="selectedDetail"
            v-model:active-key="detailContentTab"
            class="cut-round-fqc-detail-tabs"
            size="small"
            @change="handleDetailContentTabChange"
          >
            <Tabs.TabPane key="ITEMS">
              <template #tab>
                <span class="inline-flex items-center gap-1">
                  <IconifyIcon icon="lucide:list-checks" />
                  检验项目
                </span>
              </template>
            </Tabs.TabPane>
            <Tabs.TabPane key="PHOTOS">
              <template #tab>
                <span class="inline-flex items-center gap-1">
                  <IconifyIcon icon="lucide:image-plus" />
                  片号照片
                  <span v-if="selectedDetailPhotoCount > 0">
                    {{ selectedDetailPhotoCount }}
                  </span>
                </span>
              </template>
            </Tabs.TabPane>
          </Tabs>

          <ATable
            v-if="selectedDetail && detailContentTab === 'ITEMS'"
            class="cut-round-fqc-item-table"
            :columns="itemColumns"
            :data-source="selectedItems"
            :pagination="false"
            :row-class-name="itemRowClassName"
            :row-key="(row: any) => row.id"
            :scroll="{ x: 1260, y: itemTableScrollY }"
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
                    v-if="canEdit()"
                    v-access:code="['mes:cut-round-fqc:save']"
                    :disabled="!canEdit() || isItemSaving(record.id)"
                    :loading="isItemSaving(record.id)"
                    size="small"
                    @click="markItemOk(record)"
                  >
                    OK
                  </Button>
                  <Button
                    v-if="canEdit()"
                    v-access:code="['mes:cut-round-fqc:save']"
                    :disabled="!canEdit() || isItemSaving(record.id)"
                    danger
                    size="small"
                    @click="openNgModal(record)"
                  >
                    NG
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    @click="openItemPhotoModal(record)"
                  >
                    <IconifyIcon icon="lucide:camera" class="mr-1" />
                    <template v-if="getItemPhotoCount(record) === undefined">
                      照片
                    </template>
                    <template v-else>
                      照片 {{ getItemPhotoCount(record) }}
                    </template>
                  </Button>
                </div>
              </template>
            </template>
          </ATable>
          <div
            v-else-if="selectedDetail && detailContentTab === 'PHOTOS'"
            class="cut-round-fqc-photo-panel"
          >
            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
              <div>
                <div class="font-semibold text-slate-800">
                  {{ selectedDetail.productionBatchNo }} 片号补充照片
                </div>
                <div class="mt-1 text-xs text-slate-500">
                  支持jpg、jpeg、png、webp、gif、bmp；最多20张，单张不超过10MB
                </div>
              </div>
              <Button
                v-if="canSavePhotos"
                v-access:code="['mes:cut-round-fqc:save']"
                :loading="photoSaving"
                type="primary"
                @click="saveSelectedDetailPhotos"
              >
                <IconifyIcon icon="lucide:save" class="mr-1" /> 保存照片
              </Button>
            </div>
            <FileUpload
              v-if="canSavePhotos"
              v-model="photoDraftUrls"
              :accept="DETAIL_PHOTO_ACCEPT_TYPES"
              :directory="photoUploadDirectory"
              help-text="支持jpg、jpeg、png、webp、gif、bmp；最多20张，单张不超过10MB"
              list-type="picture-card"
              :max-number="DETAIL_PHOTO_MAX_COUNT"
              :max-size="10"
              multiple
              show-description
              @preview="handlePhotoPreview"
            />
            <div
              v-else-if="photoDraftUrls.length > 0"
              class="grid grid-cols-[repeat(auto-fill,minmax(150px,1fr))] gap-3"
            >
              <div
                v-for="(url, index) in photoDraftUrls"
                :key="`${url}-${index}`"
                class="rounded border bg-white p-2 shadow-sm"
              >
                <Image
                  :src="url"
                  :height="118"
                  width="100%"
                  class="rounded object-cover"
                />
                <div class="mt-2 truncate text-xs text-slate-500">
                  照片 {{ index + 1 }}
                </div>
              </div>
            </div>
            <Empty v-else class="py-16" description="当前送检片号暂无照片" />
          </div>
          <Empty v-else class="py-16" description="请选择送检片号" />
        </div>
      </div>
    </div>

    <Modal
      v-model:open="itemPhotoModalOpen"
      :body-style="{
        maxHeight: itemPhotoModalMaximized ? 'calc(100vh - 116px)' : '70vh',
        overflow: 'auto',
      }"
      :destroy-on-close="true"
      :mask-closable="false"
      :width="itemPhotoModalMaximized ? '100vw' : '860px'"
      :wrap-class-name="
        itemPhotoModalMaximized ? 'cut-round-fqc-photo-maximized-modal' : ''
      "
      @after-close="resetItemPhotoModal"
    >
      <template #title>
        <div class="flex items-center justify-between gap-3 pr-8">
          <span class="min-w-0 truncate">{{ itemPhotoModalTitle }}</span>
          <Tooltip :title="itemPhotoModalMaximized ? '还原窗口' : '最大化'">
            <Button
              shape="circle"
              type="text"
              @click.stop="itemPhotoModalMaximized = !itemPhotoModalMaximized"
            >
              <IconifyIcon
                :icon="
                  itemPhotoModalMaximized
                    ? 'lucide:minimize-2'
                    : 'lucide:maximize-2'
                "
              />
            </Button>
          </Tooltip>
        </div>
      </template>
      <Spin :spinning="itemPhotoLoading">
        <div v-if="itemPhotoTarget" class="flex flex-col gap-4">
          <div
            class="grid gap-3 rounded border border-slate-200 bg-slate-50 p-3 md:grid-cols-3"
          >
            <div>
              <div class="text-xs text-slate-500">送检片号</div>
              <div class="mt-1 font-medium text-slate-800">
                {{ selectedDetail?.productionBatchNo || '-' }}
              </div>
            </div>
            <div>
              <div class="text-xs text-slate-500">检验项目</div>
              <div class="mt-1 font-medium text-slate-800">
                {{ itemPhotoTarget.inspectionItem }}
              </div>
            </div>
            <div>
              <div class="text-xs text-slate-500">当前判定</div>
              <div class="mt-1">
                <Tag :color="rowJudgmentTag(itemPhotoTarget.itemResult).color">
                  {{ rowJudgmentTag(itemPhotoTarget.itemResult).label }}
                </Tag>
              </div>
            </div>
          </div>

          <div>
            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
              <div>
                <div class="font-semibold text-slate-800">项目检验照片</div>
                <div class="mt-1 text-xs text-slate-500">
                  照片用于记录当前项目检验结果，OK、NG、待判定均可上传；缺陷码仅用于NG项目。“{{
                    itemPhotoTarget.inspectionItem
                  }}”项目；最多6张，单张不超过10MB
                </div>
              </div>
              <div class="flex items-center gap-2">
                <Tag :color="canSaveItemPhotos ? 'blue' : 'default'">
                  {{ canSaveItemPhotos ? '不限判定，可上传' : '只读查看' }}
                </Tag>
                <Button
                  v-if="canSaveItemPhotos"
                  v-access:code="['mes:cut-round-fqc:save']"
                  type="primary"
                  @click="openItemPhotoUploadDialog"
                >
                  <IconifyIcon icon="lucide:image-plus" class="mr-1" />
                  上传照片
                </Button>
              </div>
            </div>

            <div
              v-if="itemPhotoRecords.length > 0"
              class="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-3"
            >
              <div
                v-for="(photo, index) in itemPhotoRecords"
                :key="photo.id"
                class="flex min-w-0 flex-col rounded border border-slate-200 bg-white p-3 shadow-sm"
              >
                <Image
                  :src="photo.photoUrl"
                  :height="168"
                  width="100%"
                  class="rounded object-cover"
                />
                <div class="mt-3 flex flex-wrap items-center gap-1.5">
                  <Tag color="blue">照片 {{ index + 1 }}</Tag>
                  <Tag v-if="photo.photoScene === 'DEFECT'" color="error">
                    缺陷照片
                  </Tag>
                  <Tag
                    v-else-if="photo.photoScene === 'RECHECK'"
                    color="orange"
                  >
                    复检照片
                  </Tag>
                  <Tag v-else color="success">结果照片</Tag>
                </div>
                <div class="mt-2 flex min-h-7 flex-wrap gap-1.5">
                  <Tag
                    v-for="defect in photo.defects || []"
                    :key="`${photo.id}-${defect.defectCodeId}`"
                    color="error"
                  >
                    {{ defect.defectCode || '-' }}
                    <span v-if="defect.defectName">
                      · {{ defect.defectName }}
                    </span>
                  </Tag>
                  <span
                    v-if="(photo.defects || []).length === 0"
                    class="text-xs text-slate-400"
                  >
                    未标注缺陷码
                  </span>
                </div>
                <div
                  v-if="photo.remark"
                  class="mt-2 line-clamp-2 text-xs leading-5 text-slate-600"
                >
                  备注：{{ photo.remark }}
                </div>
                <div class="mt-2 text-xs text-slate-400">
                  {{ photo.capturedByName || '未知人员' }} ·
                  {{ displayDateTime(photo.capturedTime) }}
                </div>
                <div
                  v-if="canSaveItemPhotos"
                  class="mt-3 flex justify-end gap-2 border-t border-slate-100 pt-3"
                >
                  <Button size="small" @click="openItemPhotoEditDialog(photo)">
                    <IconifyIcon icon="lucide:pencil" class="mr-1" />
                    编辑标注
                  </Button>
                  <Button
                    danger
                    size="small"
                    @click="deleteItemPhotoRecord(photo)"
                  >
                    <IconifyIcon icon="lucide:trash-2" class="mr-1" />
                    删除
                  </Button>
                </div>
              </div>
            </div>
            <Empty v-else class="py-12" description="当前检验项目暂无照片">
              <template v-if="canSaveItemPhotos" #description>
                <div class="flex flex-col items-center gap-3">
                  <span class="text-slate-500">当前检验项目暂无照片</span>
                  <Button type="primary" @click="openItemPhotoUploadDialog">
                    <IconifyIcon icon="lucide:image-plus" class="mr-1" />
                    上传第一张照片
                  </Button>
                </div>
              </template>
            </Empty>
          </div>
        </div>
      </Spin>

      <template #footer>
        <Button @click="itemPhotoModalOpen = false">关闭</Button>
      </template>
    </Modal>

    <Modal
      v-model:open="itemPhotoEditorOpen"
      :destroy-on-close="true"
      :mask-closable="false"
      :title="itemPhotoEditorTarget ? '编辑照片标注' : '上传项目照片'"
      width="680px"
      wrap-class-name="cut-round-fqc-photo-editor-modal"
      :z-index="ITEM_PHOTO_EDITOR_Z_INDEX"
      @after-close="resetItemPhotoEditor"
    >
      <div class="flex flex-col gap-4">
        <div
          class="grid gap-3 rounded border border-slate-200 bg-slate-50 p-3 md:grid-cols-2"
        >
          <div>
            <div class="text-xs text-slate-500">送检片号</div>
            <div class="mt-1 font-medium text-slate-800">
              {{ selectedDetail?.productionBatchNo || '-' }}
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-500">检验项目</div>
            <div class="mt-1 font-medium text-slate-800">
              {{ itemPhotoTarget?.inspectionItem || '-' }}
            </div>
          </div>
        </div>

        <div>
          <div class="mb-2 text-sm font-semibold text-slate-700">照片</div>
          <Image
            v-if="itemPhotoEditorTarget"
            :src="itemPhotoEditorTarget.photoUrl"
            :height="260"
            width="100%"
            class="rounded border border-slate-200 object-contain"
          />
          <FileUpload
            v-else
            v-model="itemPhotoEditorUrls"
            :accept="DETAIL_PHOTO_ACCEPT_TYPES"
            :directory="itemPhotoUploadDirectory"
            help-text="支持jpg、jpeg、png、webp、gif、bmp；单张不超过10MB"
            list-type="picture-card"
            :max-number="1"
            :max-size="10"
            :multiple="false"
            show-description
          />
        </div>

        <div>
          <div class="mb-2 flex items-center justify-between gap-2">
            <span class="text-sm font-semibold text-slate-700">
              缺陷码（选填）
            </span>
            <Tag color="blue">不限当前判定</Tag>
          </div>
          <Select
            v-model:value="itemPhotoEditorDefectCodeIds"
            max-tag-count="responsive"
            mode="multiple"
            :options="defectOptions"
            :dropdown-style="{ zIndex: ITEM_PHOTO_EDITOR_DROPDOWN_Z_INDEX }"
            placeholder="选择该照片对应的缺陷码，可多选"
            show-search
            class="w-full"
          />
          <div class="mt-2 text-xs leading-5 text-slate-500">
            所有照片均可多选缺陷码；非NG项目先保留照片标注，项目判定为NG时再按后端规则合并到正式缺陷记录。
          </div>
        </div>

        <div>
          <div class="mb-2 text-sm font-semibold text-slate-700">照片备注</div>
          <Input.TextArea
            v-model:value="itemPhotoEditorRemark"
            :maxlength="500"
            placeholder="补充该照片的缺陷位置、现象或其他说明"
            :rows="3"
            show-count
          />
        </div>
      </div>

      <template #footer>
        <Button @click="itemPhotoEditorOpen = false">取消</Button>
        <Button
          v-access:code="['mes:cut-round-fqc:save']"
          :loading="itemPhotoEditorSaving"
          type="primary"
          @click="saveItemPhotoEditor"
        >
          <IconifyIcon icon="lucide:save" class="mr-1" />
          {{ itemPhotoEditorTarget ? '保存标注' : '上传并保存' }}
        </Button>
      </template>
    </Modal>

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
      wrap-class-name="cut-round-fqc-ng-fullscreen-modal"
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
.cut-round-fqc-detail-tabs {
  flex: 0 0 auto;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  padding: 0 12px;
}

.cut-round-fqc-detail-tabs .ant-tabs-nav {
  margin: 0;
}

.cut-round-fqc-detail-tabs .ant-tabs-content-holder {
  display: none;
}

.cut-round-fqc-photo-panel {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 12px;
}

.cut-round-fqc-photo-maximized-modal .ant-modal {
  top: 0;
  max-width: 100vw;
  margin: 0;
  padding-bottom: 0;
}

.cut-round-fqc-photo-maximized-modal .ant-modal-content {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  border-radius: 0;
}

.cut-round-fqc-photo-maximized-modal .ant-modal-body {
  flex: 1;
  min-height: 0;
}

.cut-round-fqc-photo-maximized-modal .ant-modal-footer {
  margin-top: 0;
}

.cut-round-fqc-ng-fullscreen-modal .ant-modal {
  top: 0;
  max-width: 100vw;
  margin: 0;
  padding-bottom: 0;
}

.cut-round-fqc-ng-fullscreen-modal .ant-modal-content {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  border-radius: 0;
}

.cut-round-fqc-ng-fullscreen-modal .ant-modal-body {
  flex: 1;
  min-height: 0;
}

.cut-round-fqc-ng-fullscreen-modal .ant-modal-footer {
  margin-top: 0;
}
</style>
