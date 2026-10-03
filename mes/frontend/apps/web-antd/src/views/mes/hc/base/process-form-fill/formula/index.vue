<script lang="ts" setup>
import { multiFields, fieldLabel, readFormulaValue } from '#/views/mes/hc/stationform/modules/formula-multi-fields';
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, Modal, Tag, message } from 'ant-design-vue';
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

defineOptions({ name: 'MesHcProcessFormFillFormula' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type StationRecord = MesHcStationRecordApi.Record & {
  processRecordId?: number;
  sourceKind?: 'process-form' | 'station-record';
};
type StationRecordItem = MesHcStationRecordApi.RecordItem;
type ProductionActualField = string;
type ExcelAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

const FORMULA_FORM_CODE_PREFIX = 'FORMULA_';
const FORMULA_PROCESS_CODE = 'FORMULA';
const FORMULA_PASS_WORK_IMPORT_ATTACHMENT_KEY = 'formulaPassWorkImportAttachment';
const PROCESS_FORM_ROW_OFFSET = 20_000_000;
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const currentRecord = ref<StationRecord | null>(null);
const currentProcessFormRecord = ref<MesHcProcessFormApi.Record | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const excelLoading = ref(false);

const detailRows = computed(() => currentRecord.value?.items || []);
const isStartupForm = computed(() => getDisplayFormName(currentRecord.value).includes('开机点检表'));
const isCleaningForm = computed(() => {
  const name = getDisplayFormName(currentRecord.value);
  return name.includes('清洁保养表') || name.includes('清洁点检表');
});
const isProductionCheckForm = computed(() => isFormulaProductionCheck(currentRecord.value));
const dialogTitle = computed(() => `查看${getDisplayFormName(currentRecord.value) || '配料过站记录'}`);
const currentImportAttachment = computed(() => readImportAttachment(currentRecord.value));
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

const headerFields = computed(() => {
  const record = currentRecord.value;
  return [
    {
      field: 'mixerEquipmentCode',
      label: '搅拌机台号',
      value: record?.mixerEquipmentCode || record?.equipmentCode || record?.equipmentName || '-',
    },
    {
      field: 'foamingEquipmentCode',
      label: '脱泡机台号',
      value: record?.foamingEquipmentCode || '-',
    },
    {
      field: 'recordUserName',
      label: '记录人',
      value: record?.recordUserName || '-',
    },
    {
      field: 'confirmUserName',
      label: '确认人',
      value: record?.confirmUserName || '-',
    },
    {
      field: 'recordTime',
      label: '记录时间',
      value: formatDateTime(record?.recordTime),
    },
  ];
});

const productionDetailRows = computed(() =>
  detailRows.value.flatMap((record, index) => {
    if (record.valueMode === 'MULTI_FIELDS') {
      return multiFields(record).map((field) => ({
        source: record, id: record.itemSeq, category: record.itemCategory, node: record.stepNode,
        item: record.itemName, standard: record.standardText, sourceIndex: index,
        sourceRowId: record.id || `row-${index}`, actualField: `multi:${field.key}`,
        recordLabel: fieldLabel(field),
      }));
    }
    if (!isDualValueMode(record)) {
      return [
        {
          actualField: 'actualValue' as ProductionActualField,
          category: record.itemCategory,
          id: record.itemSeq,
          item: record.itemName,
          node: record.stepNode,
          recordLabel: '',
          source: record,
          sourceIndex: index,
          sourceRowId: record.id || `row-${index}`,
          standard: record.standardText,
        },
      ];
    }
    return [
      {
        actualField: 'actualValue' as ProductionActualField,
        category: record.itemCategory,
        id: record.itemSeq,
        item: record.itemName,
        node: record.stepNode,
        recordLabel: record.dualLabel1 || '重量',
        source: record,
        sourceIndex: index,
        sourceRowId: record.id || `row-${index}`,
        standard: record.standardText,
      },
      {
        actualField: 'actualValue2' as ProductionActualField,
        category: record.itemCategory,
        id: record.itemSeq,
        item: record.itemName,
        node: record.stepNode,
        recordLabel: record.dualLabel2 || '批号',
        source: record,
        sourceIndex: index,
        sourceRowId: record.id || `row-${index}`,
        standard: record.standardText,
      },
    ];
  }),
);

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
  { field: 'formName', title: '表单名称', minWidth: 260, slots: { default: 'formName' } },
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
    proxyConfig: { ajax: { query: queryFormulaRecordPage } },

    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<StationRecord>,
});

async function queryFormulaRecordPage({ page }: any, formValues: Record<string, any>) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  const queryPageSize = pageNo * pageSize;
  const params = { ...formValues, pageNo: 1, pageSize: queryPageSize };
  const [stationPage, processFormPage] = await Promise.all([
    getStationRecordPage({
      ...params,
      formCodePrefix: FORMULA_FORM_CODE_PREFIX,
    }),
    getProcessFormRecordPage({
      ...params,
      processCode: FORMULA_PROCESS_CODE,
    }),
  ]);
  const processRows = ((processFormPage?.list || []) as MesHcProcessFormApi.Record[])
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
    operationName: firstText(row.processName, header.processName, '配料'),
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
    fieldDefinitionsJson: source.fieldDefinitionsJson,
    fieldValuesJson: source.fieldValuesJson,
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
        !!source.sliceBatchNo ||
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

function parseJsonObject(value?: string): Record<string, any> {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function normalizeImportAttachment(value?: ExcelAttachment | ExcelAttachment[] | unknown): ExcelAttachment | undefined {
  const raw = Array.isArray(value) ? value[value.length - 1] : value;
  if (!raw || typeof raw !== 'object') return undefined;
  const attachment = raw as ExcelAttachment;
  const name = formatText(attachment.name, '');
  const path = formatText(attachment.path, '');
  const size = Number(attachment.size);
  const type = formatText(attachment.type, '');
  const uploadTime = formatText(attachment.uploadTime, '');
  const url = formatText(attachment.url, '');
  if (!name && !path && !url) return undefined;
  return {
    name: name || path || url,
    path: path || undefined,
    size: Number.isFinite(size) && size > 0 ? size : undefined,
    type: type || undefined,
    uploadTime: uploadTime || undefined,
    url: url || undefined,
  };
}

function readImportAttachment(record?: StationRecord | null) {
  const headerData = parseJsonObject(record?.headerDataJson) as Record<string, unknown>;
  return normalizeImportAttachment(headerData[FORMULA_PASS_WORK_IMPORT_ATTACHMENT_KEY]);
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: ExcelAttachment) {
  const url = formatText(attachment?.url || attachment?.path, '');
  if (!url) {
    message.warning('未找到附件下载地址');
    return;
  }
  window.open(url, '_blank');
}

function isFormulaProductionCheck(record?: Pick<StationRecord, 'formCode' | 'formName'> | null) {
  const formCode = String(record?.formCode || '').toUpperCase();
  const formName = String(record?.formName || '');
  return formCode.startsWith('FORMULA_PROCESS_CHECK') || formName.includes('配料生产点检表');
}

function getDisplayFormName(record?: Pick<StationRecord, 'formCode' | 'formName'> | null) {
  if (isFormulaProductionCheck(record)) {
    return '配料生产点检表';
  }
  return String(record?.formName || '');
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

function isDualValueMode(record: StationRecordItem) {
  return String(record.valueMode || '').toUpperCase() === 'DUAL_TEXT' || String(record.itemName || '').includes('加入');
}

function isTimeValueMode(record: StationRecordItem) {
  return String(record.valueMode || '').toUpperCase() === 'TIME';
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

function getProductionFieldRowSpan(index: number, field: 'category' | 'item' | 'node') {
  const rows = productionDetailRows.value;
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

function getProductionSourceRowSpan(index: number) {
  const rows = productionDetailRows.value;
  const current = rows[index];
  if (!current) return 1;
  if (index > 0 && rows[index - 1]?.sourceRowId === current.sourceRowId) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (rows[cursor]?.sourceRowId !== current.sourceRowId) break;
    span += 1;
  }
  return span;
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
    const detail = await getStationRecordDetail(Number(row.id));
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
  if (isStartupForm.value) {
    return [
      { title: '序号', width: 90 },
      { title: '点检项目', width: 260 },
      { title: '标准', width: 360 },
      { title: '实际/记录', width: 220 },
      { title: '备注', width: 260 },
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
    { title: '序号', width: 70 },
    { title: '项目类别', width: 110 },
    { title: '步骤节点', width: 110 },
    { title: '点检项目', width: 220 },
    { title: '标准', width: 260 },
    { title: '记录项', width: 90 },
    { title: '记录值', width: 220 },
    { title: '异常说明', width: 280 },
  ];
}

function buildExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isStartupForm.value) {
    return detailRows.value.map((row, index) => ({
      cells: [
        excelCell(0, row.itemSeq || index + 1),
        excelCell(1, formatText(row.itemName)),
        excelCell(2, formatText(row.standardText)),
        excelCell(3, row.actualValue || ''),
        excelCell(4, row.abnormalRemark || ''),
      ],
    }));
  }
  if (isCleaningForm.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      if (categorySpan > 0) cells.push(excelCell(0, formatText(row.itemCategory), { rowSpan: categorySpan }));
      cells.push(excelCell(1, formatText(row.itemName)));
      cells.push(excelCell(2, formatText(row.standardText)));
      cells.push(excelCell(3, row.actualValue || ''));
      cells.push(excelCell(4, row.abnormalRemark || ''));
      return { cells };
    });
  }
  return productionDetailRows.value.map((row, index) => {
    const cells: MesHcProcessFormApi.LayoutCell[] = [];
    const sourceSpan = getProductionSourceRowSpan(index);
    const categorySpan = getProductionFieldRowSpan(index, 'category');
    const nodeSpan = getProductionFieldRowSpan(index, 'node');
    const itemSpan = getProductionFieldRowSpan(index, 'item');
    if (sourceSpan > 0) cells.push(excelCell(0, row.id || row.sourceIndex + 1, { rowSpan: sourceSpan }));
    if (categorySpan > 0) cells.push(excelCell(1, formatText(row.category), { rowSpan: categorySpan }));
    if (nodeSpan > 0) cells.push(excelCell(2, formatText(row.node), { rowSpan: nodeSpan }));
    if (itemSpan > 0) cells.push(excelCell(3, formatText(row.item), { rowSpan: itemSpan }));
    if (sourceSpan > 0) cells.push(excelCell(4, formatText(row.standard), { rowSpan: sourceSpan }));
    cells.push(excelCell(5, formatText(row.recordLabel, '')));
    cells.push(excelCell(6, readFormulaValue(row.source, row.actualField)));
    if (sourceSpan > 0) {
      cells.push(excelCell(7, row.source.abnormalRemark || '', { rowSpan: sourceSpan }));
    }
    return { cells };
  });
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = getDisplayFormName(currentRecord.value) || '配料过站记录';
  return {
    columns: buildExcelColumns(),
    detailTitle: '明细项目',
    fileName: `${formatText(currentRecord.value?.planNo, '配料')}_${formName}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
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
    sheetName: '配料过站记录',
    title: formName,
    visualMode: isProductionCheckForm.value ? 'formula-production-check' : 'formula-pass-work',
  };
}

async function handleExportExcel() {
  if (!currentRecord.value) return;
  excelLoading.value = true;
  try {
    const layout = buildExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || '配料过站记录.xlsx', source: data });
  } finally {
    excelLoading.value = false;
  }
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="formula-station-record-page">
    <div class="formula-record-layout">
      <Grid v-if="!props.viewerOnly" @cell-dblclick="({ row }) => openDetail(row)">
        <template #formName="{ row }">
          <button class="formula-record-link" type="button" @click.stop="openDetail(row)">
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
      wrap-class-name="formula-station-record-modal"
      @cancel="closeDetail"
    >
      <StationFormRuntimeRenderer
        v-if="currentRecord && isRuntimeDynamicForm"
        class="formula-runtime-view"
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

      <div v-else class="formula-detail-modal">
        <div class="formula-detail-toolbar">
          <div class="formula-detail-toolbar__title">
            <span class="formula-detail-toolbar__main">{{ dialogTitle }}</span>
            <div class="formula-detail-toolbar__meta">
              <span>计划号：{{ formatText(currentRecord?.planNo) }}</span>
              <span>当前工序：{{ formatText(currentRecord?.operationName) }}</span>
              <span>执行时机：{{ formatText(currentRecord?.triggerTimingName) }}</span>
            </div>
          </div>
          <div class="formula-detail-toolbar__actions">
            <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="formula-detail-body">
          <fieldset class="formula-fieldset">
            <legend>表单信息</legend>
            <div class="formula-head-grid">
              <div v-for="item in headerFields" :key="item.field" class="formula-head-item">
                <span class="formula-head-item__label">{{ item.label }}</span>
                <span class="formula-head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachment" class="formula-import-attachment">
              <span class="formula-import-attachment__label">导入附件</span>
              <Button size="small" type="link" @click="downloadImportAttachment(currentImportAttachment)">
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>导入时间：{{ formatDateTime(currentImportAttachment.uploadTime) }}</span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="formula-detail-panel">
            <div class="formula-detail-panel__header">明细项目</div>
            <div class="formula-table-wrap">
              <table class="formula-grid">
                <thead v-if="isStartupForm">
                  <tr>
                    <th width="90">序号</th>
                    <th width="260">点检项目</th>
                    <th width="360">标准</th>
                    <th>实际/记录</th>
                    <th width="260">备注</th>
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
                    <th width="70">序号</th>
                    <th width="110">项目类别</th>
                    <th width="110">步骤节点</th>
                    <th width="220">点检项目</th>
                    <th width="260">标准</th>
                    <th width="90">记录项</th>
                    <th width="220">记录值</th>
                    <th width="280">异常说明</th>
                  </tr>
                </thead>
                <tbody v-if="isStartupForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td align="center">{{ item.itemSeq || index + 1 }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="5" class="formula-empty-cell" align="center">暂无明细数据</td>
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
                    <td colspan="5" class="formula-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr
                    v-for="(item, index) in productionDetailRows"
                    :key="`${item.sourceRowId}-${item.actualField}`"
                  >
                    <td v-if="getProductionSourceRowSpan(index) > 0" align="center" :rowspan="getProductionSourceRowSpan(index)">
                      {{ item.id || item.sourceIndex + 1 }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'category') > 0" :rowspan="getProductionFieldRowSpan(index, 'category')">
                      {{ formatText(item.category) }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'node') > 0" :rowspan="getProductionFieldRowSpan(index, 'node')">
                      {{ formatText(item.node) }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'item') > 0" :rowspan="getProductionFieldRowSpan(index, 'item')">
                      {{ formatText(item.item) }}
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">
                      {{ formatText(item.standard) }}
                    </td>
                    <td class="formula-record-label-cell">{{ formatText(item.recordLabel, '-') }}</td>
                    <td :class="['formula-record-value-cell', { 'formula-record-value-cell--time': isTimeValueMode(item.source) }]">
                      <span>{{ formatText(readFormulaValue(item.source, item.actualField)) }}</span>
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">
                      {{ formatText(item.source.abnormalRemark) }}
                    </td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="8" class="formula-empty-cell" align="center">暂无明细数据</td>
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
.formula-station-record-page {
  min-height: 0;
}

.formula-record-layout {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
}

.formula-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.formula-record-link:hover {
  text-decoration: underline;
}

.formula-runtime-view {
  height: 100vh;
  min-height: 0;
  background: #f5f7fa;
}

.formula-runtime-view :deep(.station-form-runtime) {
  height: 100%;
}

.formula-detail-modal {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background: #f5f7fa;
}

.formula-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.formula-detail-toolbar__title {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.formula-detail-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.formula-detail-toolbar__meta,
.formula-detail-toolbar__actions {
  display: flex;
  gap: 8px 16px;
  align-items: center;
}

.formula-detail-toolbar__meta {
  flex-wrap: wrap;
  color: #4b5563;
  font-size: 12px;
}

.formula-detail-toolbar__actions {
  gap: 8px;
}

.formula-detail-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.formula-fieldset {
  min-width: 0;
  margin: 0;
  padding: 8px 12px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.formula-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.formula-head-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.formula-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  align-items: center;
  margin-top: 8px;
  color: #374151;
  font-size: 12px;
}

.formula-import-attachment__label {
  color: #4b5563;
  font-weight: 700;
}

.formula-head-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.formula-head-item__label {
  color: #4b5563;
  font-weight: 700;
}

.formula-head-item__value {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  color: #374151;
  background: #fafafa;
  border: 1px solid #d9d9d9;
}

.formula-detail-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.formula-detail-panel__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  height: 34px;
  padding: 0 10px;
  color: #1677ff;
  font-weight: 700;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.formula-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.formula-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.formula-grid th,
.formula-grid td {
  min-height: 28px;
  padding: 3px 5px;
  vertical-align: middle;
  border: 1px solid #e5e7eb;
}

.formula-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-weight: 400;
  text-align: center;
  background: #f8fafc;
}

.formula-empty-cell {
  color: #9ca3af;
}

.formula-record-value-cell {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
}

.formula-record-value-cell--time {
  max-width: 180px;
}

.formula-record-label-cell {
  color: #4b5563;
  font-weight: 700;
  text-align: center;
}

:global(.formula-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.formula-station-record-modal .ant-modal-content) {
  height: 100vh;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}
</style>
