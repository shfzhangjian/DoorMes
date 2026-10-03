<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcRoughGrindingConsoleApi } from '#/api/mes/hc/execution/rough-grinding-console';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, message, Modal, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmRoughGrindingConsoleMiddleProductRecord,
  getOrInitRoughGrindingConsoleSegmentMiddleProductRecord,
  getRoughGrindingConsoleMiddleProductRecord,
  getRoughGrindingConsoleMiddleProductRecordPage,
  getRoughGrindingConsoleMiddleProductRecordByStation,
  saveRoughGrindingConsoleMiddleProductRecord,
} from '#/api/mes/hc/execution/rough-grinding-console';
import {
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import { uploadFile } from '#/api/infra/file';
import {
  getStationRecordDetail,
  getStationRecordPage,
  updateStationRecord,
} from '#/api/mes/hc/stationrecord';
import {
  buildRoughMiddleProductExcelLayout,
  loadRoughMiddleProductThicknessLabels,
  resolveRoughMiddleProductColumns,
  getRoughDisplayFormName,
  getRoughRecordLayoutConfig,
  isRoughCleaningForm,
  isRoughMiddleProductForm,
  isRoughProcessCheckForm,
  isRoughStartupForm,
  toRoughExcelColumns,
} from '#/views/mes/hc/shared/roughGrindingFormLayout';
import RoughMiddleProductRecordSheet from '#/views/mes/hc/shared/RoughMiddleProductRecordSheet.vue';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

defineOptions({ name: 'MesHcProcessFormFillRoughGrinding' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type StationRecord = MesHcStationRecordApi.Record & {
  processRecordId?: number;
  sourceKind?: 'process-form' | 'station-record';
};
type StationRecordItem = MesHcStationRecordApi.RecordItem;
type RoughImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

type MiddleProductWorkRecordContext = {
  batchNo?: string;
  id?: number;
  middleProductRecordId?: number;
  passType: 'SECOND';
  planNo?: string;
  processLength?: number;
  productionBatchNo?: string;
  segmentMark: string;
};

const middleThicknessLabels = ref<string[]>([]);
const middleThicknessColumns = computed(() => resolveRoughMiddleProductColumns(middleThicknessLabels.value));

const ROUGH_FORM_CODE_PREFIX = 'ROUGH_';
const ROUGH_MIDDLE_PRODUCT_FORM_CODE = 'ROUGH_MIDDLE_PRODUCT_RECORD';
const ROUGH_MIDDLE_PRODUCT_RECORD_SOURCE = 'ROUGH_MIDDLE_PRODUCT_BUSINESS';
const ROUGH_PROCESS_CODE = 'ROUGH_GRINDING';
const PROCESS_FORM_ROW_OFFSET = 40_000_000;
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const MIDDLE_PRODUCT_PAGE_SIZE = 50;

const currentRecord = ref<StationRecord | null>(null);
const currentProcessFormRecord = ref<MesHcProcessFormApi.Record | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const excelLoading = ref(false);
const excelImportInputRef = ref<HTMLInputElement>();
const middleProductBusinessRecord = ref<MesHcRoughGrindingConsoleApi.MiddleProductRecord | null>(null);
const middleProductDetailPage = ref(1);
const middleProductConfirmAuthVisible = ref(false);

const detailRows = computed(() => currentRecord.value?.items || []);
const headerData = computed(() => parseJsonObject(currentRecord.value?.headerDataJson));
const runtimeDynamicContext = computed(() => parseJsonObject(currentProcessFormRecord.value?.contextJson));
const runtimeDynamicSchema = computed(() => runtimeDynamicContext.value.runtimeSchema || {});
const isRuntimeDynamicForm = computed(
  () => currentRecord.value?.sourceKind === 'process-form' && !!runtimeDynamicSchema.value?.runtimeLayout,
);
const runtimeDynamicHeader = computed(() => {
  const record = currentProcessFormRecord.value;
  const header = parseJsonObject(record?.headerDataJson || currentRecord.value?.headerDataJson);
  const previewDetails = Array.isArray(header.previewDetails)
    ? header.previewDetails
    : buildRuntimePreviewDetails(record?.items || []);
  return {
    ...header,
    batchNo: header.batchNo || header.productionBatchNo || record?.batchNo || currentRecord.value?.batchNo,
    equipmentCode: header.equipmentCode || record?.equipmentCode || currentRecord.value?.equipmentCode,
    equipmentName: header.equipmentName || record?.equipmentName || currentRecord.value?.equipmentName,
    modelCode: header.modelCode || record?.modelCode || currentRecord.value?.modelCode,
    planNo: header.planNo || record?.planNo || currentRecord.value?.planNo,
    previewDetails,
    processName: header.processName || record?.processName || currentRecord.value?.operationName,
    triggerTimingName: header.triggerTimingName || record?.formTypeName || currentRecord.value?.triggerTimingName,
  };
});
const runtimeDynamicMetaItems = computed(() => [
  `计划号：${formatText(currentRecord.value?.planNo)}`,
  `型号：${formatText(currentRecord.value?.modelCode)}`,
  `批号：${formatText(currentRecord.value?.batchNo)}`,
  `当前工序：${formatText(currentRecord.value?.operationName)}`,
  `执行时机：${formatText(currentRecord.value?.triggerTimingName)}`,
]);
const middleProductHeaderData = computed(() => parseJsonObject(middleProductBusinessRecord.value?.headerDataJson));
const isStartupForm = computed(() => isRoughStartupForm(currentRecord.value));
const isCleaningForm = computed(() => isRoughCleaningForm(currentRecord.value));
const isDailyForm = computed(() => isStartupForm.value || isCleaningForm.value);
const isMiddleProductForm = computed(() => isRoughMiddleProductForm(currentRecord.value));
const isProcessCheckForm = computed(() => isRoughProcessCheckForm(currentRecord.value));
const roughLayoutConfig = computed(() => getRoughRecordLayoutConfig(currentRecord.value));
const detailColumns = computed(() => roughLayoutConfig.value.tableColumns);
const detailColumnCount = computed(() => detailColumns.value.length);
const isCurrentMiddleProductConfirmed = computed(
  () =>
    isMiddleProductForm.value &&
    String(middleProductBusinessRecord.value?.docStatus || currentRecord.value?.docStatus || '').toUpperCase() ===
      'CONFIRMED',
);
const dialogTitle = computed(() => `查看${getRoughDisplayFormName(currentRecord.value) || '磨皮过站记录'}`);
const currentImportAttachment = computed(() =>
  normalizeRoughAttachments(
    isMiddleProductForm.value
      ? middleProductHeaderData.value.attachments || middleProductHeaderData.value.importAttachment
      : headerData.value.attachments || headerData.value.importAttachment,
  )[0],
);
const middleProductAttachments = computed(() =>
  currentImportAttachment.value ? [currentImportAttachment.value] : [],
);
const middleProductMetaItems = computed(() => [
  `计划号：${formatText(currentRecord.value?.planNo)}`,
  `机台编号：${formatText(currentRecord.value?.equipmentCode || currentRecord.value?.equipmentName)}`,
  `磨皮阶段：${formatText(middleProductBusinessRecord.value?.passName || middleProductHeaderData.value.passName || headerData.value.passName || currentRecord.value?.triggerTimingName)}`,
  `报工米数：${formatText(middleProductHeaderData.value.processLength || headerData.value.processLength || middleProductBusinessRecord.value?.processLength)}`,
]);

const middleProductHeadFields = computed(() => {
  const record = currentRecord.value;
  const businessRecord = middleProductBusinessRecord.value;
  const header = middleProductHeaderData.value;
  const stationHeader = headerData.value;
  return [
    {
      field: 'modelCode',
      label: '型号',
      value:
        businessRecord?.motherModelName ||
        businessRecord?.motherModelCode ||
        stationHeader.modelName ||
        stationHeader.modelCode ||
        record?.modelName ||
        record?.modelCode ||
        header.modelName ||
        header.modelCode ||
        '-',
    },
    {
      field: 'materialCode',
      label: '料号',
      value: businessRecord?.materialCode || header.materialCode || stationHeader.materialCode || '-',
    },
    {
      field: 'productionBatchNo',
      label: '批号',
      value:
        businessRecord?.productionBatchNo ||
        header.productionBatchNo ||
        stationHeader.productionBatchNo ||
        record?.batchNo ||
        header.batchNo ||
        stationHeader.batchNo ||
        '-',
    },
    {
      field: 'processLength',
      label: '报工米数/m',
      value:
        header.processLength ||
        stationHeader.processLength ||
        businessRecord?.processLength ||
        businessRecord?.segmentTotalLength ||
        '-',
    },
    {
      field: 'widthMm',
      label: '宽幅mm',
      value: header.widthMm || stationHeader.widthMm || header.width || stationHeader.width || businessRecord?.widthMm || '-',
    },
    {
      field: 'recordDate',
      label: '生产日期',
      value: header.recordDate || stationHeader.recordDate || businessRecord?.recordDate || record?.recordDate || '-',
    },
  ];
});

const middleProductSignatureFields = computed(() => {
  const record = currentRecord.value;
  const businessRecord = middleProductBusinessRecord.value;
  const header = middleProductHeaderData.value;
  return [
    {
      field: 'recorder',
      label: '记录人',
      value: header.recorder || businessRecord?.recorder || record?.recordUserName || '-',
    },
    {
      field: 'recorderTime',
      label: '记录时间',
      value: formatDateTime(header.recorderTime || businessRecord?.recorderTime || record?.recordTime),
    },
    {
      field: 'confirmer',
      label: '确认人',
      value: header.confirmer || businessRecord?.confirmer || record?.confirmUserName || '-',
    },
    {
      field: 'confirmerTime',
      label: '确认时间',
      value: formatDateTime(header.confirmerTime || businessRecord?.confirmerTime || record?.confirmTime),
    },
  ];
});

const headerFields = computed(() => {
  const record = currentRecord.value;
  const header = headerData.value;
  const base = [
    {
      field: 'equipment',
      label: '机台编号',
      value: record?.equipmentCode || record?.equipmentName || '-',
    },
    {
      field: 'workCenter',
      label: '工作中心',
      value: record?.workCenterName || record?.workCenterCode || '-',
    },
    { field: 'recordUserName', label: '记录人', value: record?.recordUserName || '-' },
    { field: 'confirmUserName', label: '确认人', value: record?.confirmUserName || '-' },
    { field: 'recordTime', label: '记录时间', value: formatDateTime(record?.recordTime) },
  ];
  if (isDailyForm.value) {
    return [
      { field: 'recordDate', label: '点检日期', value: record?.recordDate || '-' },
      ...base,
    ];
  }
  if (isMiddleProductForm.value) {
    return middleProductHeadFields.value;
  }
  return [
    { field: 'planNo', label: '计划号', value: record?.planNo || '-' },
    { field: 'batchNo', label: '批次号', value: header.productionBatchNo || record?.batchNo || header.batchNo || '-' },
    { field: 'modelCode', label: '型号', value: record?.modelName || record?.modelCode || header.modelCode || '-' },
    { field: 'operationName', label: '工序', value: record?.operationName || '-' },
    ...base,
  ];
});

const middleProductRows = computed(() => {
  const businessDetails = middleProductBusinessRecord.value?.details || [];
  if (businessDetails.length > 0) {
    return mapMiddleProductRows(
      businessDetails,
      middleProductBusinessRecord.value?.productionBatchNo || currentRecord.value?.batchNo || '',
    );
  }
  return detailRows.value.map((record, index) => ({
    innerThickness: record.standardText || '',
    key: record.id || record.itemSeq || index + 1,
    lengthMeter: record.actualValue || '',
    outerThickness: record.actualValue2 || '',
    remark: record.abnormalRemark || '',
    seq: record.itemSeq || index + 1,
  }));
});

function todayText() {
  return dayjs().format('YYYY-MM-DD');
}

function buildMiddleProductRowsFromDetails(details: any[]) {
  return (details || []).map((item: any, index: number) => ({
    batchNo: item.batchNo || '',
    grindingPass: item.grindingPass || '',
    guideClothBatchNo: item.guideClothBatchNo || '',
    guideClothLife: item.guideClothLife ?? '',
    innerThickness: item.innerThickness || item.thickness || '',
    inputLength: item.inputLength ?? item.length ?? '',
    key: index + 1,
    length: item.length ?? '',
    lengthMeter: item.lengthMeter ?? item.length ?? item.inputLength ?? '',
    materialCode: item.materialCode || '',
    modelCode: item.modelCode || '',
    outerThickness: item.outerThickness || item.width || '',
    outputLength: item.outputLength ?? '',
    recorderName: item.recorderName || '',
    recordDate: item.recordDate || todayText(),
    remark: '',
    replaceReason: item.replaceReason || '',
    result: item.result === 'NG' ? 'NG' : 'OK',
    sandpaperBatchNo: item.sandpaperBatchNo || '',
    sandpaperLife: item.sandpaperLife ?? '',
    seq: index + 1,
    thickness: item.thickness || item.innerThickness || '',
    width: item.width || item.outerThickness || '',
  }));
}

function mapMiddleProductRows(details: any[], fallbackBatchNo = '') {
  const rows = buildMiddleProductRowsFromDetails(details || []);
  (details || []).forEach((item: any, index: number) => {
    const seq = Number(item.seq || index + 1);
    const target = rows[seq - 1];
    if (!target) return;
    target.batchNo = item.batchNo || fallbackBatchNo;
    target.grindingPass = item.grindingPass || target.grindingPass;
    target.guideClothBatchNo = item.guideClothBatchNo || '';
    target.guideClothLife = item.guideClothLife ?? '';
    target.innerThickness = item.innerThickness || item.thickness || '';
    target.inputLength = item.inputLength ?? item.length ?? '';
    target.length = item.length ?? '';
    target.lengthMeter = item.lengthMeter ?? item.length ?? item.inputLength ?? '';
    target.materialCode = item.materialCode || '';
    target.modelCode = item.modelCode || '';
    target.outerThickness = item.outerThickness || item.width || '';
    target.outputLength = item.outputLength ?? '';
    target.recorderName = item.recorderName || '';
    target.recordDate = item.recordDate || todayText();
    target.width = item.width || item.outerThickness || '';
    target.thickness = item.thickness || item.innerThickness || '';
    target.result = item.result === 'NG' ? 'NG' : 'OK';
    target.replaceReason = item.replaceReason || '';
    target.sandpaperBatchNo = item.sandpaperBatchNo || '';
    target.sandpaperLife = item.sandpaperLife ?? '';
    target.remark = item.remark || '';
  });
  return rows;
}

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { placeholder: '计划号 / 表单 / 设备 / 工序' },
    fieldName: 'keyword',
    label: '关键词',
  },
  { component: 'Input', fieldName: 'planNo', label: '计划号' },
  { component: 'Input', fieldName: 'formName', label: '表单名称' },
  { component: 'Input', fieldName: 'equipmentName', label: '设备' },
  {
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: DATETIME_FORMAT },
    fieldName: 'createTimeStart',
    label: '创建开始',
  },
  {
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: DATETIME_FORMAT },
    fieldName: 'createTimeEnd',
    label: '创建结束',
  },
];

const recordColumns: VxeTableGridOptions<StationRecord>['columns'] = [
  { field: 'formName', title: '表单名称', minWidth: 250, slots: { default: 'formName' } },
  { field: 'planNo', title: '计划号', minWidth: 150, formatter: ({ cellValue }) => formatText(cellValue) },
  {
    field: 'batchNo',
    title: '批次号',
    minWidth: 160,
    formatter: ({ cellValue, row }) => {
      const header = parseJsonObject(row?.headerDataJson);
      return formatText(header.productionBatchNo || header.batchNo || cellValue);
    },
  },
  {
    field: 'modelCode',
    title: '型号',
    minWidth: 140,
    formatter: ({ row }) => formatText(row.modelName || row.modelCode),
  },
  {
    field: 'equipmentName',
    title: '设备',
    minWidth: 140,
    formatter: ({ row }) => formatText(row.equipmentCode || row.equipmentName),
  },
  { field: 'recordDate', title: '记录日期', width: 120, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'recordScope', title: '记录范围', width: 120, formatter: ({ cellValue }) => scopeText(cellValue) },
  { field: 'docStatus', title: '状态', width: 110, align: 'center', slots: { default: 'docStatus' } },
  { field: 'resultStatus', title: '结果', width: 90, align: 'center', slots: { default: 'resultStatus' } },
  { field: 'recordUserName', title: '记录人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  {
    field: 'recordTime',
    title: '记录时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  { field: 'confirmUserName', title: '确认人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  {
    field: 'confirmTime',
    title: '确认时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  { title: '操作', width: 100, fixed: 'right', align: 'center', slots: { default: 'actions' } },
];

const [Grid] = useVbenVxeGrid({
  formOptions: { schema: gridFormSchema },
  gridOptions: {
    columns: recordColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: queryRoughRecordPage,
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<StationRecord>,
});

async function queryRoughRecordPage({ page }: any, formValues: Record<string, any>) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  const queryPageSize = pageNo * pageSize;
  const params = {
    ...formValues,
    pageNo: 1,
    pageSize: queryPageSize,
  };
  const [stationPage, middleProductPage, processFormPage] = await Promise.all([
    getStationRecordPage({
      ...params,
      excludeFormCode: ROUGH_MIDDLE_PRODUCT_FORM_CODE,
      formCodePrefix: ROUGH_FORM_CODE_PREFIX,
    }),
    getRoughGrindingConsoleMiddleProductRecordPage({
      ...params,
      formCode: ROUGH_MIDDLE_PRODUCT_FORM_CODE,
    }),
    getProcessFormRecordPage({
      ...params,
      processCode: ROUGH_PROCESS_CODE,
    }),
  ]);
  const processRows = ((processFormPage?.list || []) as MesHcProcessFormApi.Record[])
    .filter((row) => isRuntimeProcessFormRecord(row))
    .map((row) => normalizeProcessFormRow(row))
    .filter((row) => matchesProcessFormListFilters(row, formValues));
  const list = [
    ...((stationPage?.list || []) as StationRecord[]).map((row) => ({
      ...row,
      sourceKind: 'station-record' as const,
    })),
    ...((middleProductPage?.list || []) as StationRecord[]).map(normalizeMiddleProductBusinessRow),
    ...processRows,
  ].sort((a, b) => getRecordTimeValue(b) - getRecordTimeValue(a));
  const start = (pageNo - 1) * pageSize;
  return {
    list: list.slice(start, start + pageSize),
    total: Number(stationPage?.total || 0) + Number(middleProductPage?.total || 0) + processRows.length,
  };
}

function normalizeMiddleProductBusinessRow(row: StationRecord) {
  const header = parseJsonObject(row.headerDataJson);
  const recordId = parseRecordId(header.middleProductRecordId || row.id);
  return {
    ...row,
    id: recordId ? -recordId : row.id,
    headerDataJson: JSON.stringify({
      ...header,
      middleProductRecordId: recordId,
      recordSource: ROUGH_MIDDLE_PRODUCT_RECORD_SOURCE,
    }),
    sourceKind: 'station-record' as const,
  };
}

function isRuntimeProcessFormRecord(row: MesHcProcessFormApi.Record) {
  const context = parseJsonObject(row.contextJson);
  return (
    context.source === 'station-form-dev-runtime' ||
    !!context.runtimeSchema?.runtimeLayout ||
    String(row.templateCode || '').toUpperCase().endsWith('_DEV')
  );
}

function matchesProcessFormListFilters(row: StationRecord, filters: Record<string, any>) {
  const keyword = inputText(filters.keyword).toLowerCase();
  if (keyword) {
    const values = [
      row.planNo,
      row.formName,
      row.batchNo,
      row.modelName,
      row.modelCode,
      row.equipmentCode,
      row.equipmentName,
      row.operationName,
      row.triggerTimingName,
      row.recordUserName,
      row.confirmUserName,
    ];
    if (!values.some((value) => String(value || '').toLowerCase().includes(keyword))) {
      return false;
    }
  }
  const planNo = inputText(filters.planNo).toLowerCase();
  if (planNo && !String(row.planNo || '').toLowerCase().includes(planNo)) {
    return false;
  }
  const formName = inputText(filters.formName).toLowerCase();
  if (formName && !String(row.formName || '').toLowerCase().includes(formName)) {
    return false;
  }
  const equipmentName = inputText(filters.equipmentName).toLowerCase();
  if (equipmentName) {
    const equipmentValues = [row.equipmentCode, row.equipmentName];
    if (!equipmentValues.some((value) => String(value || '').toLowerCase().includes(equipmentName))) {
      return false;
    }
  }
  const createTimeStart = inputText(filters.createTimeStart);
  const createTimeEnd = inputText(filters.createTimeEnd);
  const createTime = row.createTime ? dayjs(row.createTime) : null;
  if (createTimeStart && createTime?.isValid() && createTime.isBefore(dayjs(createTimeStart))) {
    return false;
  }
  if (createTimeEnd && createTime?.isValid() && createTime.isAfter(dayjs(createTimeEnd))) {
    return false;
  }
  return true;
}

function normalizeProcessFormRow(row: MesHcProcessFormApi.Record): StationRecord {
  const header = parseJsonObject(row.headerDataJson);
  const id = parseRecordId(row.id);
  return {
    batchNo: firstText(header.batchNo, header.productionBatchNo, row.batchNo),
    confirmTime: row.confirmTime,
    confirmUserName: row.confirmUserName,
    createTime: row.createTime,
    docStatus: normalizeDocStatus(row.recordStatus),
    equipmentCode: row.equipmentCode,
    equipmentId: row.equipmentId,
    equipmentName: row.equipmentName,
    formCode: row.templateCode,
    formId: row.templateId,
    formName: row.templateName,
    headerDataJson: row.headerDataJson,
    id: id ? -(PROCESS_FORM_ROW_OFFSET + id) : row.id,
    inspectionResult: firstText(row.resultStatus, header.inspectionResult, header.result),
    modelCode: firstText(row.modelCode, header.modelCode),
    modelName: firstText(row.modelName, header.modelName),
    operationCode: row.processCode,
    operationName: firstText(row.processName, header.processName, '磨皮'),
    planId: row.planId,
    planNo: firstText(row.planNo, header.planNo),
    planOperationId: row.planOperationId,
    processRecordId: id,
    recordDate: row.recordDate,
    recordScope: 'PROCESS_DETAIL',
    recordTime: row.fillTime || row.createTime,
    recordUserName: row.fillUserName,
    resultStatus: firstText(row.resultStatus, header.inspectionResult, header.result),
    sourceKind: 'process-form',
    triggerTimingCode: row.formType,
    triggerTimingName: firstText(row.formTypeName, header.triggerTimingName),
    updateTime: (row as any).updateTime || row.createTime,
  };
}

function normalizeProcessFormDetail(row: MesHcProcessFormApi.Record): StationRecord {
  return {
    ...normalizeProcessFormRow(row),
    items: (row.items || []).map((item, index) => mapProcessRecordItemToStationItem(item, index)),
  };
}

function mapProcessRecordItemToStationItem(
  item: MesHcProcessFormApi.RecordItem,
  index: number,
): StationRecordItem {
  const source = parseJsonObject(item.sourceRowJson);
  return {
    abnormalRemark: firstText(item.abnormalRemark, source.abnormalRemark, source.remark),
    actualValue: firstText(item.actualValue, source.actualValue, source.recordValue, source.value),
    actualValue2: firstText(item.actualValue2, source.actualValue2),
    dualLabel1: firstText(source.dualLabel1),
    dualLabel2: firstText(source.dualLabel2),
    id: item.id,
    itemCategory: firstText(item.itemCategory, source.itemCategory, source.category),
    itemName: firstText(item.fieldLabel, source.itemName, source.item),
    itemSeq: Number(item.itemSeq || source.sortNo || source.seq) || index + 1,
    resultFlag: firstText(item.resultFlag, source.resultFlag, source.status, 'OK'),
    standardText: firstText(item.standardText, source.standardText, source.standard),
    stepNode: firstText(item.stepNode, source.stepNode, source.node),
    valueMode: firstText(item.valueMode, source.valueMode),
  };
}

function buildRuntimePreviewDetails(items: MesHcProcessFormApi.RecordItem[] = []) {
  return items
    .map((item, index) => {
      const source = parseJsonObject(item.sourceRowJson);
      const looksLikeDetail =
        String(item.fieldKey || '').startsWith('runtime_detail') ||
        !!source.itemName ||
        !!source.item ||
        !!source.standardText ||
        !!source.standard ||
        !!source.seq ||
        !!source.sortNo ||
        !item.sourceRowJson;
      if (!looksLikeDetail) return null;
      return {
        ...source,
        abnormalRemark: firstText(item.abnormalRemark, source.abnormalRemark, source.remark),
        actualValue: firstText(item.actualValue, source.actualValue, source.recordValue, source.value),
        actualValue2: firstText(item.actualValue2, source.actualValue2),
        itemCategory: firstText(item.itemCategory, source.itemCategory, source.category),
        itemName: firstText(source.itemName, source.item, item.fieldLabel),
        resultFlag: firstText(item.resultFlag, source.resultFlag, source.status, 'OK'),
        seq: Number(source.seq || item.itemSeq) || index + 1,
        sortNo: Number(source.sortNo || source.seq || item.itemSeq) || index + 1,
        standardText: firstText(item.standardText, source.standardText, source.standard),
        stepNode: firstText(item.stepNode, source.stepNode, source.node),
        valueMode: firstText(item.valueMode, source.valueMode),
      };
    })
    .filter(Boolean) as Record<string, any>[];
}

function getRecordTimeValue(record?: StationRecord | null) {
  const value = record?.recordTime || record?.createTime || record?.updateTime || record?.confirmTime;
  const time = value ? dayjs(value).valueOf() : 0;
  return Number.isFinite(time) ? time : 0;
}

function formatText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function inputText(value?: null | number | string) {
  return String(value ?? '').trim();
}

function formatDateTime(value?: null | number | string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format(DATETIME_FORMAT) : String(value);
}

function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, any>;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, any>)
      : ({} as Record<string, any>);
  } catch {
    return {} as Record<string, any>;
  }
}

function normalizeDocStatus(status?: string) {
  return inputText(status) || 'RECORDED';
}

function parseRecordId(value?: unknown) {
  const text = String(value ?? '').trim();
  if (!text) return undefined;
  const id = Number(text);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function resolveMiddleProductRecordId(record?: StationRecord | null) {
  const header = parseJsonObject(record?.headerDataJson);
  return parseRecordId(header.middleProductRecordId || header.businessRecordId);
}

function normalizeSegmentMark(segmentMark?: unknown) {
  const value = String(segmentMark ?? '').trim().toUpperCase();
  return value && value !== '-' ? value : '';
}

function inferSegmentMarkFromBatchNo(batchNo?: unknown) {
  const value = String(batchNo ?? '').trim().toUpperCase();
  const match = value.match(/[PQRS]$/);
  return match?.[0] || '';
}

function resolveMiddleProductSegmentMark(record?: StationRecord | null) {
  const header = parseJsonObject(record?.headerDataJson);
  return normalizeSegmentMark(
    header.segmentMark ||
      inferSegmentMarkFromBatchNo(header.productionBatchNo || header.batchNo || record?.batchNo),
  );
}

function toNumberValue(value?: unknown) {
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

function buildMiddleProductWorkRecordContext(record?: StationRecord | null): MiddleProductWorkRecordContext | null {
  if (!record) return null;
  const header = parseJsonObject(record.headerDataJson);
  const productionBatchNo = formatText(
    header.productionBatchNo || header.batchNo || record.batchNo,
    '',
  );
  const segmentMark = resolveMiddleProductSegmentMark(record);
  return {
    batchNo: productionBatchNo,
    id: record.bizType === 'GRINDING_SECOND' ? parseRecordId(record.bizId) : undefined,
    middleProductRecordId: resolveMiddleProductRecordId(record) || parseRecordId(record.id),
    passType: 'SECOND',
    planNo: record.planNo,
    processLength: toNumberValue(header.processLength),
    productionBatchNo,
    segmentMark,
  };
}

function isMiddleProductBusinessRecord(record?: StationRecord | null) {
  const header = parseJsonObject(record?.headerDataJson);
  return header.recordSource === ROUGH_MIDDLE_PRODUCT_RECORD_SOURCE;
}

async function fetchMiddleProductBusinessRecord(record?: StationRecord | null) {
  if (!record?.id) return null;
  const latestRecord = buildMiddleProductWorkRecordContext(record);
  if (latestRecord?.middleProductRecordId) {
    return getRoughGrindingConsoleMiddleProductRecord(latestRecord.middleProductRecordId);
  }
  if (record.planOperationId && latestRecord?.id) {
    return getOrInitRoughGrindingConsoleSegmentMiddleProductRecord({
      passType: 'SECOND',
      planOperationId: record.planOperationId,
      segmentMark: latestRecord.segmentMark,
    });
  }
  return getRoughGrindingConsoleMiddleProductRecordByStation(Math.abs(record.id));
}

function normalizeRoughAttachments(value?: RoughImportAttachment | RoughImportAttachment[] | unknown): RoughImportAttachment[] {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as RoughImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return {
        name,
        path: path || undefined,
        size: Number(raw.size) || undefined,
        type: raw.type,
        uploadTime: raw.uploadTime,
        url,
      };
    })
    .filter(Boolean)
    .slice(-1) as RoughImportAttachment[];
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: RoughImportAttachment) {
  const url = formatText(attachment?.url || attachment?.path, '');
  if (!url) return;
  window.open(url, '_blank');
}

function scopeText(scope?: string) {
  const map: Record<string, string> = {
    EQUIPMENT_DAILY: '设备日记录',
    PLAN_OPERATION: '计划工序',
    PROCESS_DETAIL: '报工明细',
  };
  return map[String(scope || '')] || formatText(scope);
}

function statusMeta(status?: string) {
  const map: Record<string, { color: string; text: string }> = {
    CONFIRMED: { color: 'success', text: '已确认' },
    DRAFT: { color: 'default', text: '草稿' },
    PENDING_CHECK: { color: 'default', text: '待执行' },
    RECORDED: { color: 'processing', text: '已填写' },
  };
  return map[status || ''] || { color: 'default', text: status || '-' };
}

function resultMeta(result?: string) {
  if (result === 'OK' || result === 'PASS') return { color: 'success', text: result };
  if (result === 'NG' || result === 'FAIL') return { color: 'error', text: result };
  return { color: 'default', text: result || '-' };
}

function normalizeResultFlag(value?: string) {
  const text = String(value || '').trim().toUpperCase();
  if (!text) return '';
  if (text.includes('NG') || text.includes('不合格')) return 'NG';
  if (text.includes('NA') || text.includes('N/A') || text.includes('不适用')) return 'NA';
  if (text.includes('OK') || text.includes('合格')) return 'OK';
  return text;
}

function getFieldRowSpan(index: number, field: 'itemCategory' | 'stepNode') {
  const rows = detailRows.value;
  const current = rows[index];
  if (!current) return 1;
  if (index > 0 && rows[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (rows[cursor]?.[field] !== current[field]) break;
    span += 1;
  }
  return span;
}

async function openDetail(row: StationRecord) {
  if (!row.id) return;
  detailLoading.value = true;
  try {
    middleThicknessLabels.value = [];
    if (isRoughMiddleProductForm(row)) middleThicknessLabels.value = await loadRoughMiddleProductThicknessLabels();
    if (row.sourceKind === 'process-form' && row.processRecordId) {
      const detail = await getProcessFormRecordDetail(row.processRecordId);
      currentProcessFormRecord.value = detail;
      middleProductBusinessRecord.value = null;
      currentRecord.value = normalizeProcessFormDetail(detail);
      middleProductDetailPage.value = 1;
      detailVisible.value = true;
      return;
    }
    if (isRoughMiddleProductForm(row) && isMiddleProductBusinessRecord(row)) {
      currentProcessFormRecord.value = null;
      currentRecord.value = {
        ...row,
        items: [],
      };
      middleProductBusinessRecord.value = await fetchMiddleProductBusinessRecord(row);
      middleProductDetailPage.value = 1;
      detailVisible.value = true;
      return;
    }
    const detail = await getStationRecordDetail(row.id);
    currentProcessFormRecord.value = null;
    middleProductBusinessRecord.value = null;
    currentRecord.value = {
      ...detail,
      items: detail.items || [],
      sourceKind: 'station-record',
    };
    if (isRoughMiddleProductForm(detail) && detail.id) {
      try {
        middleProductBusinessRecord.value = await fetchMiddleProductBusinessRecord(detail);
      } catch {
        middleProductBusinessRecord.value = null;
      }
      middleProductDetailPage.value = 1;
    }
    detailVisible.value = true;
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailVisible.value = false;
  currentRecord.value = null;
  currentProcessFormRecord.value = null;
  middleProductBusinessRecord.value = null;
  middleProductDetailPage.value = 1;
}

function buildImportedAttachment(file: File, uploaded: any): RoughImportAttachment {
  const url = typeof uploaded === 'string' ? uploaded : uploaded?.url;
  return {
    name: uploaded?.name || file.name,
    path: uploaded?.path,
    size: uploaded?.size || file.size,
    type: uploaded?.type || file.type,
    uploadTime: dayjs().format(DATETIME_FORMAT),
    url: url || uploaded?.path,
  };
}

function buildHeaderDataWithAttachment(attachment?: RoughImportAttachment) {
  const header = { ...headerData.value };
  if (attachment) {
    header.attachments = [attachment];
    header.importAttachment = attachment;
    header.importTime = attachment.uploadTime;
  }
  return header;
}

function buildMiddleProductHeaderDataWithAttachment(
  attachment?: RoughImportAttachment,
  baseHeader: Record<string, any> = middleProductHeaderData.value,
) {
  const header = { ...baseHeader };
  delete header.generatedLength;
  if (attachment) {
    header.attachments = [attachment];
    header.importAttachment = attachment;
    header.importTime = attachment.uploadTime;
  }
  return header;
}

async function resolveMiddleProductBusinessRecord() {
  if (!currentRecord.value?.id) return null;
  if (middleProductBusinessRecord.value?.recordId) {
    return middleProductBusinessRecord.value;
  }
  middleProductBusinessRecord.value = await fetchMiddleProductBusinessRecord(currentRecord.value);
  return middleProductBusinessRecord.value;
}

async function refreshMiddleProductBusinessRecord(recordId: number) {
  middleProductBusinessRecord.value = await getRoughGrindingConsoleMiddleProductRecord(recordId);
  if (currentRecord.value?.id && !isMiddleProductBusinessRecord(currentRecord.value)) {
    const detail = await getStationRecordDetail(currentRecord.value.id);
    currentRecord.value = {
      ...detail,
      items: detail.items || [],
    };
  } else if (currentRecord.value && middleProductBusinessRecord.value) {
    const header = parseJsonObject(middleProductBusinessRecord.value.headerDataJson);
    currentRecord.value = {
      ...currentRecord.value,
      batchNo: middleProductBusinessRecord.value.productionBatchNo || currentRecord.value.batchNo,
      confirmTime: middleProductBusinessRecord.value.confirmerTime || currentRecord.value.confirmTime,
      confirmUserName: middleProductBusinessRecord.value.confirmer || currentRecord.value.confirmUserName,
      docStatus: middleProductBusinessRecord.value.docStatus || currentRecord.value.docStatus,
      headerDataJson: JSON.stringify({
        ...header,
        middleProductRecordId: middleProductBusinessRecord.value.recordId,
        recordSource: ROUGH_MIDDLE_PRODUCT_RECORD_SOURCE,
      }),
      recordTime: middleProductBusinessRecord.value.recorderTime || currentRecord.value.recordTime,
      recordUserName: middleProductBusinessRecord.value.recorder || currentRecord.value.recordUserName,
      resultStatus: middleProductBusinessRecord.value.resultStatus || currentRecord.value.resultStatus,
    };
  }
  return middleProductBusinessRecord.value;
}

function applyImportedExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  (resp.cellValues || []).forEach((item) => {
    const row = detailRows.value[Number(item.bindKey)];
    if (!row || !item.bindField) return;
    if (item.bindField === 'resultFlag') {
      return;
    }
    if (['actualValue', 'actualValue2', 'abnormalRemark'].includes(item.bindField)) {
      (row as any)[item.bindField] = inputText(item.value);
    }
  });
}

function applyImportedMiddleProductExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  const record = middleProductBusinessRecord.value;
  if (!record) return 0;
  const header = { ...parseJsonObject(record.headerDataJson) };
  const rowMap = new Map<number, Record<string, string>>();
  let appliedCount = 0;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'headerData' || !item.bindKey) return;
    const value = inputText(item.value);
    if (!value && value !== '0') return;
    header[item.bindKey] = value;
    if (item.bindKey === 'processLength') {
      header.processLength = value;
    }
    if (item.bindKey === 'widthMm') {
      header.width = value;
      header.widthMm = value;
    }
    appliedCount += 1;
  });
  (resp.cellValues || []).forEach((item) => {
    const index = Number(item.bodyRowIndex ?? item.bindKey);
    if (!Number.isInteger(index) || index < 0 || !item.bindField) return;
    if (!['innerThickness', 'lengthMeter', 'outerThickness', 'remark'].includes(item.bindField)) return;
    const value = inputText(item.value);
    if (!value && value !== '0') return;
    const row = rowMap.get(index) || {};
    row[item.bindField] = value;
    rowMap.set(index, row);
    appliedCount += 1;
  });
  const details =
    rowMap.size > 0
      ? [...rowMap.keys()].sort((a, b) => a - b).map((sourceIndex, targetIndex) => {
          const imported = rowMap.get(sourceIndex) || {};
          const base = record.details?.[sourceIndex] || record.details?.[targetIndex] || {};
          const lengthMeter = imported.lengthMeter ?? base.lengthMeter ?? base.length ?? base.inputLength ?? '';
          const innerThickness = imported.innerThickness ?? base.innerThickness ?? base.thickness ?? '';
          const outerThickness = imported.outerThickness ?? base.outerThickness ?? base.width ?? '';
          return {
            ...base,
            ...imported,
            innerThickness,
            inputLength: lengthMeter,
            length: lengthMeter,
            lengthMeter,
            outerThickness,
            outputLength: lengthMeter,
            seq: targetIndex + 1,
            thickness: innerThickness,
            width: outerThickness,
          };
        })
      : [...(record.details || [])];
  if (!header.processLength && rowMap.size > 0) {
    const maxLength = Math.max(
      ...details.map((item: any) => Number(item.lengthMeter || item.length || item.inputLength || 0)).filter(Number.isFinite),
      0,
    );
    header.processLength = maxLength || details.length || '';
  }
  delete header.generatedLength;
  middleProductBusinessRecord.value = {
    ...record,
    details,
    generatedLength: details.length,
    headerDataJson: JSON.stringify(header),
    processLength: header.processLength || record.processLength,
  };
  middleProductDetailPage.value = 1;
  return appliedCount;
}

function buildMiddleProductExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  const header = middleProductHeaderData.value;
  const businessRecord = middleProductBusinessRecord.value;
  const bindableHeaderKeys = new Set([
    'confirmer',
    'recordDate',
    'recorder',
    'widthMm',
    'processLength',
  ]);
  return [
    { editable: false, label: '计划号', value: currentRecord.value?.planNo || '' },
    { editable: false, label: '当前工序', value: currentRecord.value?.operationName || '磨皮' },
    {
      editable: false,
      label: '磨皮阶段',
      value: businessRecord?.passName || currentRecord.value?.triggerTimingName || header.passName || header.passType || '',
    },
    ...middleProductHeadFields.value.map((field) => ({
      bindField: bindableHeaderKeys.has(field.field) ? 'headerData' : undefined,
      bindKey: bindableHeaderKeys.has(field.field) ? field.field : undefined,
      editable: bindableHeaderKeys.has(field.field),
      label: field.label,
      value: field.value ?? '',
    })),
    ...middleProductSignatureFields.value.map((field) => ({
      bindField: bindableHeaderKeys.has(field.field) ? 'headerData' : undefined,
      bindKey: bindableHeaderKeys.has(field.field) ? field.field : undefined,
      editable: bindableHeaderKeys.has(field.field),
      label: field.label,
      value: field.value ?? '',
    })),
  ];
}

function calcImportedRecordResult() {
  const hasNg = detailRows.value.some((row) => normalizeResultFlag(row.resultFlag) === 'NG');
  return hasNg ? 'NG' : 'OK';
}

async function saveImportedStationRecord(attachment?: RoughImportAttachment) {
  if (!currentRecord.value?.id) return;
  const result = calcImportedRecordResult();
  const header = buildHeaderDataWithAttachment(attachment);
  await updateStationRecord({
    ...currentRecord.value,
    headerDataJson: JSON.stringify(header),
    inspectionResult: result,
    items: detailRows.value.map((item, index) => ({
      abnormalRemark: item.abnormalRemark,
      actualValue: item.actualValue,
      actualValue2: item.actualValue2,
      dualLabel1: item.dualLabel1,
      dualLabel2: item.dualLabel2,
      itemCategory: item.itemCategory,
      itemName: item.itemName,
      itemSeq: item.itemSeq || index + 1,
      resultFlag: normalizeResultFlag(item.resultFlag) || item.resultFlag,
      standardText: item.standardText,
      stepNode: item.stepNode,
      valueMode: item.valueMode,
    })),
    resultStatus: result,
  });
  const detail = await getStationRecordDetail(currentRecord.value.id);
  currentRecord.value = {
    ...detail,
    items: detail.items || [],
  };
}

function excelCell(
  colIndex: number,
  text?: null | number | string,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    colIndex,
    colSpan: options.colSpan ?? 1,
    bindField: options.bindField,
    bindKey: options.bindKey,
    editable: options.editable ?? false,
    rowSpan: options.rowSpan ?? 1,
    text: String(text ?? ''),
  };
}

function buildExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isMiddleProductForm.value) {
    return [];
  }
  if (isProcessCheckForm.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      const nodeSpan = getFieldRowSpan(index, 'stepNode');
      if (categorySpan > 0) cells.push(excelCell(0, row.itemCategory || '', { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(excelCell(1, row.stepNode || '', { rowSpan: nodeSpan }));
      cells.push(excelCell(2, row.itemName || ''));
      cells.push(excelCell(3, row.standardText || ''));
      cells.push(excelCell(4, row.actualValue || '', { bindField: 'actualValue', bindKey: String(index), editable: true }));
      cells.push(excelCell(5, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey: String(index), editable: true }));
      return { cells };
    });
  }
  return detailRows.value.map((row, index) => {
    if (isCleaningForm.value) {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      if (categorySpan > 0) cells.push(excelCell(0, row.itemCategory || '', { rowSpan: categorySpan }));
      cells.push(excelCell(1, row.itemName || ''));
      cells.push(excelCell(2, row.standardText || ''));
      cells.push(excelCell(3, row.actualValue || '', { bindField: 'actualValue', bindKey: String(index), editable: true }));
      cells.push(excelCell(4, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey: String(index), editable: true }));
      return { cells };
    }
    return {
      cells: [
        excelCell(0, row.itemSeq || index + 1),
        excelCell(1, row.itemName || ''),
        excelCell(2, row.standardText || ''),
        excelCell(3, row.actualValue || '', { bindField: 'actualValue', bindKey: String(index), editable: true }),
        excelCell(4, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey: String(index), editable: true }),
      ],
    };
  });
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = getRoughDisplayFormName(currentRecord.value) || '磨皮过站记录';
  if (isMiddleProductForm.value) {
    return buildRoughMiddleProductExcelLayout({
      thicknessLabels: middleThicknessLabels.value,
      filePrefix: currentRecord.value?.planNo || '磨皮',
      formName,
      headerItems: buildMiddleProductExcelHeaderItems(),
      rows: middleProductRows.value,
    });
  }
  return {
    columns: toRoughExcelColumns(detailColumns.value),
    detailTitle: roughLayoutConfig.value.detailTitle,
    fileName: `${formatText(currentRecord.value?.planNo, '磨皮')}_${formName}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
    headerItems: [
      { editable: false, label: '计划号', value: currentRecord.value?.planNo || '' },
      { editable: false, label: '当前工序', value: currentRecord.value?.operationName || '' },
      { editable: false, label: '执行时机', value: currentRecord.value?.triggerTimingName || '' },
      ...headerFields.value.map((field) => ({
        bindField: isMiddleProductForm.value && field.field === 'processLength' ? 'headerData' : undefined,
        bindKey: isMiddleProductForm.value && field.field === 'processLength' ? 'processLength' : undefined,
        editable: isMiddleProductForm.value && field.field === 'processLength',
        label: field.label,
        value: field.value || '',
      })),
    ],
    rows: buildExcelRows(),
    sheetName: roughLayoutConfig.value.excelSheetName,
    title: formName,
    visualMode: roughLayoutConfig.value.visualMode,
  };
}

async function handleExportExcel() {
  if (!currentRecord.value) return;
  excelLoading.value = true;
  try {
    if (isMiddleProductForm.value && !(await resolveMiddleProductBusinessRecord())?.recordId) {
      message.warning('未找到对应的二次磨皮中间品记录单');
      return;
    }
    const layout = buildExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || '磨皮过站记录.xlsx', source: data });
  } finally {
    excelLoading.value = false;
  }
}

async function handleConfirmMiddleProductRecord() {
  const record = await resolveMiddleProductBusinessRecord();
  if (!record?.recordId) {
    message.warning('未找到对应的二次磨皮中间品记录单');
    return;
  }
  if (!middleProductRows.value.length) {
    message.warning('中间品记录单明细不能为空');
    return;
  }
  middleProductConfirmAuthVisible.value = true;
}

async function handleMiddleProductConfirmAuthSuccess(userInfo: any) {
  middleProductConfirmAuthVisible.value = false;
  const record = await resolveMiddleProductBusinessRecord();
  if (!record?.recordId) {
    message.warning('未找到对应的二次磨皮中间品记录单');
    return;
  }
  if (!middleProductRows.value.length) {
    message.warning('中间品记录单明细不能为空');
    return;
  }
  excelLoading.value = true;
  try {
    const confirmer =
      userInfo?.empName ||
      userInfo?.nickname ||
      userInfo?.username ||
      userInfo?.empNo ||
      middleProductHeaderData.value.confirmer ||
      currentRecord.value?.confirmUserName ||
      '';
    const header = {
      ...parseJsonObject(record.headerDataJson),
      confirmer,
      confirmerTime: dayjs().format(DATETIME_FORMAT),
    };
    delete header.generatedLength;
    await confirmRoughGrindingConsoleMiddleProductRecord({
      details: record.details || [],
      headerDataJson: JSON.stringify(header),
      recordId: record.recordId,
    });
    await refreshMiddleProductBusinessRecord(record.recordId);
    message.success('中间品记录单已确认');
  } finally {
    excelLoading.value = false;
  }
}

function triggerImportExcel() {
  excelImportInputRef.value?.click();
}

async function handleExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file || !currentRecord.value?.id) return;
  if (!/\.xls[xm]?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  excelLoading.value = true;
  try {
    if (isMiddleProductForm.value) {
      const record = await resolveMiddleProductBusinessRecord();
      if (!record?.recordId) {
        message.warning('未找到对应的二次磨皮中间品记录单');
        return;
      }
      const layout = buildExcelLayout();
      const resp = await importProcessFormRecordLayout(file, layout);
      const appliedCount = applyImportedMiddleProductExcel(resp);
      const uploaded = await uploadFile({
        directory: 'mes/rough-grinding-middle-product',
        file,
      });
      const attachment = buildImportedAttachment(file, uploaded);
      const latestRecord = middleProductBusinessRecord.value || record;
      await saveRoughGrindingConsoleMiddleProductRecord({
        details: latestRecord.details || [],
        headerDataJson: JSON.stringify(buildMiddleProductHeaderDataWithAttachment(attachment, parseJsonObject(latestRecord.headerDataJson))),
        recordId: record.recordId,
      });
      await refreshMiddleProductBusinessRecord(record.recordId);
      message.success(`导入成功，已回填 ${appliedCount} 个有值单元格并挂接原始附件`);
      return;
    }
    const layout = buildExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    applyImportedExcel(resp);
    const uploaded = await uploadFile({
      directory: 'mes/rough-grinding-station-record',
      file,
    });
    await saveImportedStationRecord(buildImportedAttachment(file, uploaded));
    message.success(`导入成功，已回填 ${resp.totalCellCount || 0} 个单元格并挂接原始附件`);
  } finally {
    excelLoading.value = false;
  }
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="rough-station-record-page">
    <div class="rough-record-layout">
      <Grid v-if="!props.viewerOnly" @cell-dblclick="({ row }) => openDetail(row)">
        <template #formName="{ row }">
          <button class="rough-record-link" type="button" @click.stop="openDetail(row)">
            {{ getRoughDisplayFormName(row) || '-' }}
          </button>
        </template>

        <template #docStatus="{ row }">
          <Tag :color="statusMeta(row.docStatus).color">{{ statusMeta(row.docStatus).text }}</Tag>
        </template>

        <template #resultStatus="{ row }">
          <Tag :color="resultMeta(row.resultStatus || row.inspectionResult).color">
            {{ resultMeta(row.resultStatus || row.inspectionResult).text }}
          </Tag>
        </template>

        <template #actions="{ row }">
          <Button :loading="detailLoading" size="small" type="link" @click.stop="openDetail(row)">查看</Button>
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      destroy-on-close
      width="100vw"
      wrap-class-name="rough-station-record-modal"
      @cancel="closeDetail"
    >
      <StationFormRuntimeRenderer
        v-if="currentRecord && isRuntimeDynamicForm"
        class="rough-runtime-view"
        :form-name="dialogTitle"
        :header-data="runtimeDynamicHeader"
        :readonly="true"
        :record-meta="runtimeDynamicMetaItems"
        :schema="runtimeDynamicSchema"
        compact
        @open-attachment="downloadImportAttachment"
      >
        <template #actions>
          <Button size="small" @click="closeDetail">关闭</Button>
        </template>
      </StationFormRuntimeRenderer>

      <RoughMiddleProductRecordSheet
        v-else-if="currentRecord && isMiddleProductForm"
        v-model:page="middleProductDetailPage"
        :attachments="middleProductAttachments"
        :columns="middleThicknessColumns"
        :detail-title="roughLayoutConfig.detailTitle"
        :editable="false"
        :head-fields="middleProductHeadFields"
        :meta-items="middleProductMetaItems"
        :page-size="MIDDLE_PRODUCT_PAGE_SIZE"
        :rows="middleProductRows"
        :signature-fields="middleProductSignatureFields"
        :title="dialogTitle"
        @open-attachment="downloadImportAttachment"
      >
        <template #actions>
          <input
            ref="excelImportInputRef"
            accept=".xls,.xlsx,.xlsm"
            hidden
            type="file"
            @change="handleExcelImportChange"
          />
          <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
          <Button size="small" :loading="excelLoading" v-if="!props.viewerOnly" @click="triggerImportExcel">导入Excel</Button>
          <Button
            v-if="!props.viewerOnly && !isCurrentMiddleProductConfirmed"
            size="small"
            :loading="excelLoading"
            type="primary"
            @click="handleConfirmMiddleProductRecord"
          >
            确认
          </Button>
          <Button size="small" @click="closeDetail">关闭</Button>
        </template>
      </RoughMiddleProductRecordSheet>

      <div v-else class="rough-detail-modal">
        <div class="rough-detail-toolbar">
          <div class="rough-detail-toolbar__title">
            <span class="rough-detail-toolbar__main">{{ dialogTitle }}</span>
            <div class="rough-detail-toolbar__meta">
              <span v-if="isDailyForm">记录范围：设备日记录</span>
              <span v-else>计划号：{{ formatText(currentRecord?.planNo) }}</span>
              <span>当前工序：{{ formatText(currentRecord?.operationName) }}</span>
              <span>执行时机：{{ formatText(currentRecord?.triggerTimingName) }}</span>
            </div>
          </div>
          <div class="rough-detail-toolbar__actions">
            <input
              ref="excelImportInputRef"
              accept=".xls,.xlsx,.xlsm"
              hidden
              type="file"
              @change="handleExcelImportChange"
            />
            <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
            <Button size="small" :loading="excelLoading" v-if="!props.viewerOnly" @click="triggerImportExcel">导入Excel</Button>
            <Button
              v-if="!props.viewerOnly && isMiddleProductForm && !isCurrentMiddleProductConfirmed"
              size="small"
              :loading="excelLoading"
              type="primary"
              @click="handleConfirmMiddleProductRecord"
            >
              确认
            </Button>
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="rough-detail-body">
          <fieldset class="rough-fieldset">
            <legend>表单信息</legend>
            <div class="rough-head-grid">
              <div v-for="item in headerFields" :key="item.field" class="rough-head-item">
                <span class="rough-head-item__label">{{ item.label }}</span>
                <span class="rough-head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachment" class="rough-import-attachment">
              <span class="rough-import-attachment__label">导入附件</span>
              <Button size="small" type="link" @click="downloadImportAttachment(currentImportAttachment)">
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>导入时间：{{ formatDateTime(currentImportAttachment.uploadTime) }}</span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="rough-detail-panel">
            <div class="rough-detail-panel__header">{{ roughLayoutConfig.detailTitle }}</div>
            <div class="rough-table-wrap">
              <table class="rough-grid">
                <thead>
                  <tr>
                    <th v-for="column in detailColumns" :key="column.key" :width="column.width">
                      {{ column.title }}
                    </th>
                  </tr>
                </thead>
                <tbody v-if="isProcessCheckForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td v-if="getFieldRowSpan(index, 'itemCategory') > 0" :rowspan="getFieldRowSpan(index, 'itemCategory')">
                      {{ formatText(item.itemCategory) }}
                    </td>
                    <td v-if="getFieldRowSpan(index, 'stepNode') > 0" :rowspan="getFieldRowSpan(index, 'stepNode')">
                      {{ formatText(item.stepNode) }}
                    </td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td :colspan="detailColumnCount" class="rough-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else-if="isCleaningForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td v-if="getFieldRowSpan(index, 'itemCategory') > 0" :rowspan="getFieldRowSpan(index, 'itemCategory')">
                      {{ formatText(item.itemCategory) }}
                    </td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td :colspan="detailColumnCount" class="rough-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td align="center">{{ item.itemSeq || index + 1 }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td :colspan="detailColumnCount" class="rough-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
              <div v-if="isMiddleProductForm" class="rough-signature-row">
                <div
                  v-for="field in middleProductSignatureFields"
                  :key="field.field"
                  class="rough-signature-cell"
                >
                  <span class="rough-signature-cell__label">{{ field.label }}</span>
                  <span class="rough-signature-cell__value">{{ field.value || '-' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <AuthModal
      v-model:visible="middleProductConfirmAuthVisible"
      action-name="确认中间品记录表"
      auth-mode="username"
      @success="handleMiddleProductConfirmAuthSuccess"
    />
  </component>
</template>

<style scoped>
.rough-station-record-page {
  min-height: 0;
}

.rough-record-layout,
.rough-detail-modal,
.rough-detail-body,
.rough-detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.rough-record-layout {
  gap: 8px;
  height: 100%;
}

.rough-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.rough-record-link:hover {
  text-decoration: underline;
}

.pp-plan-modal {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-plan-toolbar__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: baseline;
  min-width: 0;
}

.pp-plan-toolbar__main {
  color: #172033;
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 12px;
}

.pp-plan-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.pp-fieldset {
  padding: 8px 10px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.pp-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
}

.production-check-head-grid {
  display: grid;
  gap: 0;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.production-check-head-grid--cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.production-check-head-cell {
  display: grid;
  grid-template-columns: 126px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.production-check-head-cell__label,
.production-check-head-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.production-check-head-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.production-check-head-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.record-attachment-panel {
  flex-shrink: 0;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.record-attachment-panel__toolbar {
  display: flex;
  align-items: center;
  min-height: 24px;
}

.record-attachment-panel__title {
  color: #1677ff;
  font-weight: 800;
}

.record-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 6px;
}

.record-attachment-item {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
  padding: 2px 6px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.record-attachment-link {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  color: #1677ff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.record-attachment-time,
.record-attachment-size,
.record-attachment-empty {
  color: #64748b;
  font-size: 12px;
}

.record-attachment-empty {
  display: inline-flex;
  margin-top: 4px;
}

.pp-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #e1e9f2 100%);
  border: 1px solid #8794a4;
}

.pp-panel__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  color: #075985;
  font-weight: 800;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-table-wrap,
.pass-work-detail-wrap {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  min-width: 1370px;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #c6d0dc;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #263445;
  font-weight: 800;
  text-align: center;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-empty-cell {
  color: #94a3b8;
}

.middle-product-detail-body {
  gap: 10px;
}

.middle-product-detail-table {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
}

.middle-product-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 4px 2px 0;
  background: #f5f7fa;
  border-top: 1px solid #c6d0dc;
}

.middle-product-signature-row {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 6px;
  overflow: hidden;
  background: #fff;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.middle-product-signature-cell {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.middle-product-signature-cell__label,
.middle-product-signature-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.middle-product-signature-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.middle-product-signature-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.rough-detail-modal {
  height: 100vh;
  background: #f5f7fa;
}

.rough-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.rough-detail-toolbar__title,
.rough-detail-toolbar__meta,
.rough-detail-toolbar__actions {
  display: flex;
  gap: 8px 16px;
  align-items: center;
}

.rough-detail-toolbar__meta {
  flex-wrap: wrap;
  color: #4b5563;
  font-size: 12px;
}

.rough-detail-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.rough-detail-body {
  flex: 1;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.rough-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.rough-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
}

.rough-head-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 8px;
}

.rough-head-item__label {
  display: block;
  margin-bottom: 4px;
  color: #374151;
  font-weight: 700;
}

.rough-head-item__value {
  display: block;
  min-height: 32px;
  padding: 6px 10px;
  color: #4b5563;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.rough-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
}

.rough-import-attachment__label {
  color: #374151;
  font-weight: 700;
}

.rough-detail-panel {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.rough-detail-panel__header {
  flex-shrink: 0;
  height: 34px;
  padding: 7px 10px;
  color: #1677ff;
  font-weight: 700;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.rough-table-wrap {
  flex: 1;
  overflow: auto;
}

.rough-grid {
  width: 100%;
  min-width: 1120px;
  border-collapse: collapse;
}

.rough-grid th,
.rough-grid td {
  padding: 7px 8px;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}

.rough-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #374151;
  font-weight: 700;
  background: #f8fafc;
}

.rough-record-label-cell {
  color: #374151;
  font-weight: 700;
  text-align: center;
}

.rough-signature-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 1120px;
  margin-top: 6px;
  overflow: hidden;
  background: #fff;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.rough-signature-cell {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.rough-signature-cell__label,
.rough-signature-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.rough-signature-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.rough-signature-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.rough-empty-cell {
  color: #9ca3af;
}

:global(.rough-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.rough-station-record-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}

:global(.rough-station-record-modal .ant-modal-body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:global(.rough-station-record-modal .rough-middle-sheet) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}
</style>
