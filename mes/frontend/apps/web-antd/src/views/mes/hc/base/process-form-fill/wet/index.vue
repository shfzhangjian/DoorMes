<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, Modal, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
} from '#/api/mes/hc/processform';
import {
  getStationRecordDetail,
  getStationRecordPage,
} from '#/api/mes/hc/stationrecord';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';

defineOptions({ name: 'MesHcProcessFormFillWet' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type StationRecord = MesHcStationRecordApi.Record & {
  processRecordId?: number;
  sourceKind?: 'process-form' | 'station-record';
};
type StationRecordItem = MesHcStationRecordApi.RecordItem;
type WetImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

const WET_FORM_CODE_PREFIX = 'WET_';
const WET_PROCESS_CODE = 'WET';
const PROCESS_FORM_ROW_OFFSET = 30_000_000;
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const currentRecord = ref<StationRecord | null>(null);
const currentProcessFormRecord = ref<MesHcProcessFormApi.Record | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const excelLoading = ref(false);

const detailRows = computed(() => currentRecord.value?.items || []);
const headerData = computed(() => parseJsonObject(currentRecord.value?.headerDataJson));
const isProductionCheckForm = computed(() => isWetProductionCheck(currentRecord.value));
const isSemiFinishedForm = computed(() => isWetSemiFinished(currentRecord.value));
const isCleaningForm = computed(() => getDisplayFormName(currentRecord.value).includes('清洁点检'));
const dialogTitle = computed(() => `查看${getDisplayFormName(currentRecord.value) || '湿法过站记录'}`);
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
    batchNo: header.batchNo || header.motherBatchNo || record?.batchNo || currentRecord.value?.batchNo,
    equipmentCode: header.equipmentCode || record?.equipmentCode || currentRecord.value?.equipmentCode,
    equipmentName: header.equipmentName || record?.equipmentName || currentRecord.value?.equipmentName,
    machine: header.machine || record?.equipmentCode || record?.equipmentName || currentRecord.value?.equipmentCode,
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
const currentImportAttachment = computed(() =>
  normalizeWetAttachments(headerData.value.attachments)[0],
);

const semiThicknessHeaders = computed(() => {
  const name = String(currentRecord.value?.formName || '');
  const position = name.includes('凝固') ? '出槽' : '出箱';
  return [
    `左侧10cm${position}厚度/mm`,
    `左侧20cm${position}厚度/mm`,
    `右侧10cm${position}厚度/mm`,
    `右侧20cm${position}厚度/mm`,
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
    { field: 'recordUserName', label: '记录人', value: record?.recordUserName || '-' },
    { field: 'confirmUserName', label: '确认人', value: record?.confirmUserName || '-' },
    { field: 'recordTime', label: '记录时间', value: formatDateTime(record?.recordTime) },
  ];
  if (isSemiFinishedForm.value) {
    return [
      { field: 'recordDate', label: '生产日期', value: record?.recordDate || header.productionDate || '-' },
      { field: 'modelCode', label: '型号', value: record?.modelName || record?.modelCode || header.modelCode || '-' },
      { field: 'batchNo', label: '产品批号', value: record?.batchNo || header.batchNo || '-' },
      { field: 'generatedLength', label: '半成品长度/m', value: header.generatedLength || '-' },
      { field: 'semiWidth', label: '宽幅/m', value: header.semiWidth || '-' },
      { field: 'poreDevelopment', label: '泡孔发育', value: header.poreDevelopment || '-' },
      { field: 'finalResult', label: '判定', value: header.finalResult || record?.resultStatus || '-' },
      ...base,
    ];
  }
  if (isProductionCheckForm.value) {
    return [
      { field: 'batchNo', label: '产品批号', value: record?.batchNo || header.batchNo || '-' },
      { field: 'modelCode', label: '型号', value: record?.modelName || record?.modelCode || header.modelCode || '-' },
      { field: 'startTime', label: '开始时间', value: header.startTime || '-' },
      { field: 'endTime', label: '结束时间', value: header.endTime || '-' },
      { field: 'inWashTime', label: '进水洗时间', value: header.inWashTime || '-' },
      { field: 'outWashTime', label: '出水洗时间', value: header.outWashTime || '-' },
      { field: 'inSolidifyTime', label: '进凝固时间', value: header.inSolidifyTime || '-' },
      { field: 'outSolidifyTime', label: '出凝固时间', value: header.outSolidifyTime || '-' },
      { field: 'inOvenTime', label: '进烘箱时间', value: header.inOvenTime || '-' },
      { field: 'outOvenTime', label: '出烘箱时间', value: header.outOvenTime || '-' },
      ...base,
    ];
  }
  return [
    { field: 'modelCode', label: '型号', value: record?.modelName || record?.modelCode || '-' },
    { field: 'batchNo', label: '批次号', value: record?.batchNo || '-' },
    ...base,
  ];
});

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { placeholder: '计划号 / 表单 / 设备 / 工序' },
    fieldName: 'keyword',
    label: '关键词',
  },
  { component: 'Input', fieldName: 'planNo', label: '计划号' },
  { component: 'Input', fieldName: 'formName', label: '表单名称' },
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
  { field: 'formName', title: '表单名称', minWidth: 240, slots: { default: 'formName' } },
  { field: 'planNo', title: '计划号', minWidth: 150 },
  { field: 'batchNo', title: '批次号', minWidth: 160 },
  { field: 'modelCode', title: '型号', minWidth: 140 },
  { field: 'operationName', title: '工序', width: 120 },
  { field: 'triggerTimingName', title: '执行时机', width: 120 },
  { field: 'docStatus', title: '状态', width: 110, align: 'center', slots: { default: 'docStatus' } },
  { field: 'resultStatus', title: '结果', width: 90, align: 'center', slots: { default: 'resultStatus' } },
  { field: 'recordUserName', title: '记录人', width: 110 },
  {
    field: 'recordTime',
    title: '记录时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  { field: 'confirmUserName', title: '确认人', width: 110 },
  {
    field: 'confirmTime',
    title: '确认时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  {
    field: 'createTime',
    title: '创建时间',
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
    proxyConfig: { ajax: { query: queryWetRecordPage } },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<StationRecord>,
});

async function queryWetRecordPage({ page }: any, formValues: Record<string, any>) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  const queryPageSize = pageNo * pageSize;
  const params = { ...formValues, pageNo: 1, pageSize: queryPageSize };
  const [stationPage, processFormPage] = await Promise.all([
    getStationRecordPage({
      ...params,
      formCodePrefix: WET_FORM_CODE_PREFIX,
    }),
    getProcessFormRecordPage({
      ...params,
      processCode: WET_PROCESS_CODE,
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
    ...processRows,
  ].sort((a, b) => getRecordTimeValue(b) - getRecordTimeValue(a));
  const start = (pageNo - 1) * pageSize;
  return {
    list: list.slice(start, start + pageSize),
    total: Number(stationPage?.total || 0) + processRows.length,
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
    batchNo: firstText(header.batchNo, header.motherBatchNo, row.batchNo),
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
    operationName: firstText(row.processName, header.processName, '湿法'),
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

function normalizeDocStatus(status?: string) {
  return inputText(status) || 'RECORDED';
}

function parseRecordId(value?: unknown) {
  const id = Number(String(value ?? '').trim());
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function getRecordTimeValue(record?: null | StationRecord) {
  const value = record?.recordTime || record?.createTime || record?.updateTime || record?.confirmTime;
  const time = value ? dayjs(value).valueOf() : 0;
  return Number.isFinite(time) ? time : 0;
}

function inputText(value?: null | number | string) {
  return String(value ?? '').trim();
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function formatText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
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

function normalizeWetAttachments(value?: WetImportAttachment | WetImportAttachment[] | unknown): WetImportAttachment[] {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as WetImportAttachment & Record<string, any>;
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
    .slice(-1) as WetImportAttachment[];
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: WetImportAttachment) {
  const url = formatText(attachment?.url || attachment?.path, '');
  if (!url) return;
  window.open(url, '_blank');
}

function stripWetModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  const stripped = text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim();
  return stripped || text;
}

function isWetProductionCheck(record?: Pick<StationRecord, 'formCode' | 'formName'> | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.startsWith('WET_PROCESS_CHECK') || name.includes('湿法点检') || name.includes('工艺参数');
}

function isWetSemiFinished(record?: Pick<StationRecord, 'formCode' | 'formName'> | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.startsWith('WET_SOLID_SEMI') || code.startsWith('WET_OVEN_SEMI') || name.includes('半成品记录');
}

function getDisplayFormName(record?: Pick<StationRecord, 'formCode' | 'formName'> | null) {
  const name = String(record?.formName || '');
  if (isWetProductionCheck(record)) return '湿法生产点检表';
  if (isWetSemiFinished(record)) {
    if (name.includes('凝固')) return '凝固半成品记录表';
    if (name.includes('烘箱')) return '烘箱半成品记录表';
    return stripWetModelPrefix(name) || '半成品记录单';
  }
  return stripWetModelPrefix(name);
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

function getFieldRowSpan(index: number, field: 'itemCategory' | 'stepNode') {
  const rows = detailRows.value;
  const current = rows[index];
  if (!current?.[field]) return 0;
  if (index > 0 && rows[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (rows[cursor]?.[field] !== current[field]) break;
    span += 1;
  }
  return span;
}

function getSemiDetailLength(row: StationRecordItem, index: number) {
  const numberValue = Number(row.itemSeq || index + 1);
  const generatedLength = Number(headerData.value.generatedLength);
  if (!Number.isFinite(generatedLength) || generatedLength <= 0) {
    return numberValue * 2;
  }
  const totalRows = Math.max(1, detailRows.value.length);
  const rowStep = Math.max(1, Math.ceil(generatedLength / totalRows));
  return Math.min(generatedLength, (index + 1) * rowStep);
}

async function openDetail(row: StationRecord) {
  if (!row.id) return;
  detailLoading.value = true;
  try {
    if (row.sourceKind === 'process-form' && row.processRecordId) {
      const detail = await getProcessFormRecordDetail(row.processRecordId);
      currentProcessFormRecord.value = detail;
      currentRecord.value = normalizeProcessFormDetail(detail);
      detailVisible.value = true;
      return;
    }
    const detail = await getStationRecordDetail(row.id);
    currentProcessFormRecord.value = null;
    currentRecord.value = {
      ...detail,
      items: detail.items || [],
      sourceKind: 'station-record',
    };
    detailVisible.value = true;
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailVisible.value = false;
  currentRecord.value = null;
  currentProcessFormRecord.value = null;
}

function excelCell(
  colIndex: number,
  text?: null | number | string,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    colIndex,
    colSpan: options.colSpan ?? 1,
    editable: false,
    rowSpan: options.rowSpan ?? 1,
    text: String(text ?? ''),
  };
}

function buildExcelColumns(): MesHcProcessFormApi.LayoutColumn[] {
  if (isSemiFinishedForm.value) {
    return [
      { title: '长度/m', width: 100 },
      ...semiThicknessHeaders.value.map((title) => ({ title, width: 180 })),
      { title: '备注', width: 260 },
    ];
  }
  if (isProductionCheckForm.value) {
    return [
      { title: '物料/生产环节', width: 120 },
      { title: '确认节点', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '点检标准', width: 180 },
      { title: '实际/记录', width: 180 },
      { title: '异常备注', width: 220 },
    ];
  }
  if (isCleaningForm.value) {
    return [
      { title: '工序', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '检查标准', width: 420 },
      { title: '实际/记录', width: 220 },
      { title: '备注', width: 260 },
    ];
  }
  return [
    { title: '序号', width: 90 },
    { title: '点检项目', width: 260 },
    { title: '标准', width: 360 },
    { title: '实际/记录', width: 220 },
    { title: '备注', width: 260 },
  ];
}

function buildExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isSemiFinishedForm.value) {
    return detailRows.value.map((row, index) => ({
      cells: [
        excelCell(0, getSemiDetailLength(row, index)),
        excelCell(1, row.itemName || ''),
        excelCell(2, row.standardText || ''),
        excelCell(3, row.actualValue || ''),
        excelCell(4, row.stepNode || ''),
        excelCell(5, row.abnormalRemark || ''),
      ],
    }));
  }
  if (isProductionCheckForm.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      const nodeSpan = getFieldRowSpan(index, 'stepNode');
      if (categorySpan > 0) cells.push(excelCell(0, row.itemCategory || '', { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(excelCell(1, row.stepNode || '', { rowSpan: nodeSpan }));
      cells.push(excelCell(2, row.itemName || ''));
      cells.push(excelCell(3, row.standardText || ''));
      cells.push(excelCell(4, row.actualValue || ''));
      cells.push(excelCell(5, row.abnormalRemark || ''));
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
      cells.push(excelCell(3, row.actualValue || ''));
      cells.push(excelCell(4, row.abnormalRemark || ''));
      return { cells };
    }
    return {
      cells: [
        excelCell(0, row.itemSeq || index + 1),
        excelCell(1, row.itemName || ''),
        excelCell(2, row.standardText || ''),
        excelCell(3, row.actualValue || ''),
        excelCell(4, row.abnormalRemark || ''),
      ],
    };
  });
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = getDisplayFormName(currentRecord.value) || '湿法过站记录';
  return {
    columns: buildExcelColumns(),
    detailTitle: isSemiFinishedForm.value ? '半成品记录明细' : '明细项目',
    fileName: `${formatText(currentRecord.value?.planNo, '湿法')}_${formName}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
    headerItems: [
      { editable: false, label: '计划号', value: currentRecord.value?.planNo || '' },
      { editable: false, label: '当前工序', value: currentRecord.value?.operationName || '' },
      { editable: false, label: '执行时机', value: currentRecord.value?.triggerTimingName || '' },
      ...headerFields.value.map((field) => ({
        editable: false,
        label: field.label,
        value: field.value || '',
      })),
    ],
    rows: buildExcelRows(),
    sheetName: '湿法过站记录',
    title: formName,
    visualMode: isSemiFinishedForm.value ? 'wet-semi' : 'wet-pass-work',
  };
}

async function handleExportExcel() {
  if (!currentRecord.value) return;
  excelLoading.value = true;
  try {
    const layout = buildExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || '湿法过站记录.xlsx', source: data });
  } finally {
    excelLoading.value = false;
  }
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="wet-station-record-page">
    <div class="wet-record-layout">
      <Grid v-if="!props.viewerOnly" @cell-dblclick="({ row }) => openDetail(row)">
        <template #formName="{ row }">
          <button class="wet-record-link" type="button" @click.stop="openDetail(row)">
            {{ getDisplayFormName(row) || '-' }}
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
      wrap-class-name="wet-station-record-modal"
      @cancel="closeDetail"
    >
      <StationFormRuntimeRenderer
        v-if="currentRecord && isRuntimeDynamicForm"
        class="wet-runtime-view"
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

      <div v-else class="wet-detail-modal">
        <div class="wet-detail-toolbar">
          <div class="wet-detail-toolbar__title">
            <span class="wet-detail-toolbar__main">{{ dialogTitle }}</span>
            <div class="wet-detail-toolbar__meta">
              <span>计划号：{{ formatText(currentRecord?.planNo) }}</span>
              <span>当前工序：{{ formatText(currentRecord?.operationName) }}</span>
              <span>执行时机：{{ formatText(currentRecord?.triggerTimingName) }}</span>
            </div>
          </div>
          <div class="wet-detail-toolbar__actions">
            <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="wet-detail-body">
          <fieldset class="wet-fieldset">
            <legend>表单信息</legend>
            <div class="wet-head-grid">
              <div v-for="item in headerFields" :key="item.field" class="wet-head-item">
                <span class="wet-head-item__label">{{ item.label }}</span>
                <span class="wet-head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachment" class="wet-import-attachment">
              <span class="wet-import-attachment__label">导入附件</span>
              <Button size="small" type="link" @click="downloadImportAttachment(currentImportAttachment)">
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>导入时间：{{ formatDateTime(currentImportAttachment.uploadTime) }}</span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="wet-detail-panel">
            <div class="wet-detail-panel__header">明细项目</div>
            <div class="wet-table-wrap">
              <table class="wet-grid">
                <thead v-if="isSemiFinishedForm">
                  <tr>
                    <th width="100">长度/m</th>
                    <th v-for="header in semiThicknessHeaders" :key="header" width="180">{{ header }}</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <thead v-else-if="isProductionCheckForm">
                  <tr>
                    <th width="120">物料/生产环节</th>
                    <th width="120">确认节点</th>
                    <th width="220">点检项目</th>
                    <th width="180">点检标准</th>
                    <th width="180">实际/记录</th>
                    <th width="220">异常备注</th>
                  </tr>
                </thead>
                <thead v-else-if="isCleaningForm">
                  <tr>
                    <th width="120">工序</th>
                    <th width="220">点检项目</th>
                    <th width="420">检查标准</th>
                    <th>实际/记录</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <thead v-else>
                  <tr>
                    <th width="90">序号</th>
                    <th width="260">点检项目</th>
                    <th width="360">标准</th>
                    <th>实际/记录</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody v-if="isSemiFinishedForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td align="center">{{ getSemiDetailLength(item, index) }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.stepNode) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="6" class="wet-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else-if="isProductionCheckForm">
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
                    <td colspan="6" class="wet-empty-cell" align="center">暂无明细数据</td>
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
                    <td colspan="5" class="wet-empty-cell" align="center">暂无明细数据</td>
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
                    <td colspan="5" class="wet-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </component>
</template>

<style scoped>
.wet-station-record-page {
  min-height: 0;
}

.wet-record-layout,
.wet-detail-modal,
.wet-detail-body,
.wet-detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.wet-record-layout {
  gap: 8px;
  height: 100%;
}

.wet-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.wet-record-link:hover {
  text-decoration: underline;
}

.wet-detail-modal {
  height: 100vh;
  background: #f5f7fa;
}

.wet-runtime-view {
  height: 100vh;
}

.wet-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.wet-detail-toolbar__title,
.wet-detail-toolbar__meta,
.wet-detail-toolbar__actions {
  display: flex;
  gap: 8px 16px;
  align-items: center;
}

.wet-detail-toolbar__meta {
  flex-wrap: wrap;
  color: #4b5563;
  font-size: 12px;
}

.wet-detail-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.wet-detail-body {
  flex: 1;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.wet-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.wet-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
}

.wet-head-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 8px;
}

.wet-head-item__label {
  display: block;
  margin-bottom: 4px;
  color: #374151;
  font-weight: 700;
}

.wet-head-item__value {
  display: block;
  min-height: 32px;
  padding: 6px 10px;
  color: #4b5563;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.wet-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
}

.wet-import-attachment__label {
  color: #374151;
  font-weight: 700;
}

.wet-detail-panel {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.wet-detail-panel__header {
  flex-shrink: 0;
  height: 34px;
  padding: 7px 10px;
  color: #1677ff;
  font-weight: 700;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.wet-table-wrap {
  flex: 1;
  overflow: auto;
}

.wet-grid {
  width: 100%;
  min-width: 1120px;
  border-collapse: collapse;
}

.wet-grid th,
.wet-grid td {
  padding: 7px 8px;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}

.wet-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #374151;
  font-weight: 700;
  background: #f8fafc;
}

.wet-empty-cell {
  color: #9ca3af;
}

:global(.wet-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.wet-station-record-modal .ant-modal-content) {
  height: 100vh;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}
</style>
