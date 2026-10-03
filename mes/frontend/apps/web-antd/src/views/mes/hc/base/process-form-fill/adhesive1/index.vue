<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcAdhesiveConsoleApi } from '#/api/mes/hc/execution/adhesive-console';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, message, Modal, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getAdhesiveConsoleIntermediateById,
  getAdhesiveConsoleIntermediateRecordPage,
  saveAdhesiveConsoleIntermediate,
} from '#/api/mes/hc/execution/adhesive-console';
import { uploadFile } from '#/api/infra/file';
import {
  exportProcessFormRecordLayout,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import {
  getStationRecordDetail,
  getStationRecordPage,
  updateStationRecord,
} from '#/api/mes/hc/stationrecord';
import {
  createRoughLayoutCell,
  toRoughExcelColumns,
} from '#/views/mes/hc/shared/roughGrindingFormLayout';
import { ADHESIVE_INTERMEDIATE_NOTES, buildAdhesiveIntermediateFooterNotes, resolveAdhesiveIntermediateColumns } from '#/views/mes/hc/shared/adhesiveIntermediateLayout';
import RoughMiddleProductRecordSheet from '#/views/mes/hc/shared/RoughMiddleProductRecordSheet.vue';

defineOptions({ name: 'MesHcProcessFormFillAdhesive1' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type StationRecord = MesHcStationRecordApi.Record;
type AdhesiveIntermediateRecord = MesHcAdhesiveConsoleApi.IntermediateRecord;
type ImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

const ADHESIVE_FORM_CODE_PREFIX = 'ADHESIVE_';
const ADHESIVE_INTERMEDIATE_FORM_CODE = 'ADHESIVE_INTERMEDIATE_RECORD';
const ADHESIVE_INTERMEDIATE_RECORD_SOURCE = 'ADHESIVE_INTERMEDIATE_BUSINESS';
const ADHESIVE_PROCESS_CHECK_FORM_CODE = 'ADHESIVE_PROCESS_CHECK';
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const MIDDLE_PRODUCT_PAGE_SIZE = 50;

const ADHESIVE_MIDDLE_PRODUCT_COLUMNS = computed(() => {
  const keys = ['lengthMeter', 'innerThickness', 'outerThickness', 'remark'] as const;
  return resolveAdhesiveIntermediateColumns(intermediateRecord.value?.thicknessLabels).map((column, index) => ({ ...column, key: keys[index]!, bindField: keys[index]! }));
});
const intermediateNotes = computed(() => ADHESIVE_INTERMEDIATE_NOTES.map((note) => ({ ...note, text: intermediateRecord.value?.formNotes?.[note.key] || '' })).filter((note) => note.text));

const currentRecord = ref<StationRecord | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const excelLoading = ref(false);
const excelImportInputRef = ref<HTMLInputElement>();
const intermediateRecord = ref<AdhesiveIntermediateRecord | null>(null);
const intermediatePage = ref(1);

const detailRows = computed(() => currentRecord.value?.items || []);
const headerData = computed(() => parseJsonObject(currentRecord.value?.headerDataJson));
const intermediateExtra = computed(() => parseJsonObject(intermediateRecord.value?.extraJson));
const isIntermediateForm = computed(() => isAdhesiveIntermediateForm(currentRecord.value));
const isProcessCheckForm = computed(() => isAdhesiveProcessCheckForm(currentRecord.value));
const isCleaningForm = computed(() => isAdhesiveCleaningForm(currentRecord.value));
const isDailyForm = computed(() => isAdhesiveStartupForm(currentRecord.value) || isCleaningForm.value);
const dialogTitle = computed(() => `查看${getAdhesiveDisplayFormName(currentRecord.value) || '粘胶1记录'}`);

const currentImportAttachment = computed(() =>
  normalizeAttachments(
    isIntermediateForm.value
      ? intermediateExtra.value.attachments || intermediateExtra.value.importAttachment
      : headerData.value.attachments || headerData.value.importAttachment,
  )[0],
);
const intermediateAttachments = computed(() => (currentImportAttachment.value ? [currentImportAttachment.value] : []));

const detailColumns = computed(() => {
  if (isProcessCheckForm.value) {
    return [
      { key: 'itemCategory', title: '类别', width: 140 },
      { key: 'stepNode', title: '确认节点', width: 130 },
      { key: 'itemName', title: '工艺参数项目', width: 220 },
      { key: 'standardText', title: '标准', width: 220 },
      { bindField: 'actualValue', key: 'actualValue', title: '实测值', width: 180 },
      { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '异常备注', width: 240 },
    ];
  }
  if (isCleaningForm.value) {
    return [
      { key: 'itemCategory', title: '工序', width: 140 },
      { key: 'itemName', title: '点检项目', width: 220 },
      { key: 'standardText', title: '检查标准', width: 420 },
      { bindField: 'actualValue', key: 'actualValue', title: '实际/记录', width: 220 },
      { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '备注', width: 240 },
    ];
  }
  return [
    { key: 'itemSeq', title: '序号', width: 90 },
    { key: 'itemName', title: '点检项目', width: 260 },
    { key: 'standardText', title: '标准', width: 360 },
    { bindField: 'actualValue', key: 'actualValue', title: '实际/记录', width: 220 },
    { bindField: 'abnormalRemark', key: 'abnormalRemark', title: '备注', width: 240 },
  ];
});
const detailColumnCount = computed(() => detailColumns.value.length);

const headerFields = computed(() => {
  const record = currentRecord.value;
  const header = headerData.value;
  const base = [
    { field: 'recordUserName', label: '记录人', value: record?.recordUserName || '-' },
    { field: 'recordTime', label: '记录时间', value: formatDateTime(record?.recordTime) },
    { field: 'confirmUserName', label: '确认人', value: record?.confirmUserName || '-' },
    { field: 'confirmTime', label: '确认时间', value: formatDateTime(record?.confirmTime) },
  ];
  return [
    { field: 'planNo', label: '计划号', value: record?.planNo || '-' },
    { field: 'batchNo', label: '批次号', value: formatAdhesiveBatchNo(header.productionBatchNo || header.batchNo || record?.batchNo) },
    { field: 'modelCode', label: '型号', value: header.modelCode || record?.modelName || record?.modelCode || '-' },
    { field: 'equipment', label: '设备', value: record?.equipmentCode || record?.equipmentName || '-' },
    ...base,
  ];
});

const intermediateHeadFields = computed(() => {
  const record = intermediateRecord.value;
  return [
    { field: 'modelCode', label: '型号', value: record?.modelCode || '-' },
    { field: 'materialCode', label: '料号', value: record?.materialCode || '-' },
    { field: 'batchNo', label: '批号', value: record?.batchNo || currentRecord.value?.batchNo || '-' },
    { field: 'processLength', label: '加工米数/m', value: record?.processLength ?? '-' },
    { field: 'productWidthMm', label: '产品宽幅mm', value: record?.productWidthMm ?? '-' },
    { field: 'productionDate', label: '生产日期', value: formatDate(record?.productionDate) },
    { field: 'widthStart', label: '开头宽幅', value: record?.widthStart ?? '-' },
    { field: 'widthMiddle', label: '中间宽幅', value: record?.widthMiddle ?? '-' },
    { field: 'widthEnd', label: '结尾宽幅', value: record?.widthEnd ?? '-' },
  ];
});

const intermediateSignatureFields = computed(() => [
  { field: 'recorderName', label: '记录人', value: intermediateRecord.value?.recorderName || '-' },
  { field: 'recordTime', label: '记录时间', value: formatDateTime(intermediateRecord.value?.recordTime) },
  { field: 'confirmerName', label: '确认人', value: intermediateRecord.value?.confirmerName || '-' },
  { field: 'confirmTime', label: '确认时间', value: '-' },
]);

const intermediateRows = computed(() =>
  (intermediateRecord.value?.details || []).map((item, index) => ({
    innerThickness: item.leftThickness ?? '',
    key: index + 1,
    lengthMeter: item.lengthMark ?? index + 1,
    outerThickness: item.rightThickness ?? '',
    remark: item.remark || '',
    seq: item.sortNo || index + 1,
  })),
);

const intermediateMetaItems = computed(() => [
  `计划号：${formatText(currentRecord.value?.planNo)}`,
  `批号：${formatText(intermediateRecord.value?.batchNo || currentRecord.value?.batchNo)}`,
  `加工米数：${formatText(intermediateRecord.value?.processLength)}`,
]);

const gridFormSchema: VbenFormSchema[] = [
  { component: 'Input', componentProps: { placeholder: '计划号 / 表单 / 批次 / 设备' }, fieldName: 'keyword', label: '关键词' },
  { component: 'Input', fieldName: 'planNo', label: '计划号' },
  { component: 'Input', fieldName: 'formName', label: '表单名称' },
  { component: 'Input', fieldName: 'equipmentName', label: '设备' },
  { component: 'DatePicker', componentProps: { showTime: true, valueFormat: DATETIME_FORMAT }, fieldName: 'createTimeStart', label: '创建开始' },
  { component: 'DatePicker', componentProps: { showTime: true, valueFormat: DATETIME_FORMAT }, fieldName: 'createTimeEnd', label: '创建结束' },
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
      const batchNo = header.productionBatchNo || header.batchNo || cellValue;
      return isAdhesiveProcessCheckForm(row) ? formatAdhesiveBatchNo(batchNo) : formatText(batchNo);
    },
  },
  { field: 'modelCode', title: '型号', minWidth: 140, formatter: ({ row }) => formatText(row.modelName || row.modelCode) },
  { field: 'equipmentName', title: '设备', minWidth: 140, formatter: ({ row }) => formatText(row.equipmentCode || row.equipmentName) },
  { field: 'recordDate', title: '记录日期', width: 120, formatter: ({ cellValue }) => formatDate(cellValue) },
  { field: 'recordScope', title: '记录范围', width: 120, formatter: ({ cellValue }) => scopeText(cellValue) },
  { field: 'docStatus', title: '状态', width: 110, align: 'center', slots: { default: 'docStatus' } },
  { field: 'resultStatus', title: '结果', width: 90, align: 'center', slots: { default: 'resultStatus' } },
  { field: 'recordUserName', title: '记录人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'recordTime', title: '记录时间', width: 170, formatter: ({ cellValue }) => formatDateTime(cellValue) },
  { field: 'confirmUserName', title: '确认人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'confirmTime', title: '确认时间', width: 170, formatter: ({ cellValue }) => formatDateTime(cellValue) },
  { title: '操作', width: 100, fixed: 'right', align: 'center', slots: { default: 'actions' } },
];

const [Grid] = useVbenVxeGrid({
  formOptions: { schema: gridFormSchema },
  gridOptions: {
    columns: recordColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: { ajax: { query: queryAdhesiveRecordPage } },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<StationRecord>,
});

async function queryAdhesiveRecordPage({ page }: any, formValues: Record<string, any>) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  const queryPageSize = pageNo * pageSize;
  const params = { ...formValues, pageNo: 1, pageSize: queryPageSize };
  const [stationPage, intermediatePageData] = await Promise.all([
    getStationRecordPage({ ...params, excludeFormCode: ADHESIVE_INTERMEDIATE_FORM_CODE, formCodePrefix: ADHESIVE_FORM_CODE_PREFIX }),
    getAdhesiveConsoleIntermediateRecordPage({ ...params, formCode: ADHESIVE_INTERMEDIATE_FORM_CODE }),
  ]);
  const list = [
    ...((stationPage?.list || []) as StationRecord[]),
    ...((intermediatePageData?.list || []) as StationRecord[]).map(normalizeIntermediateBusinessRow),
  ].sort((a, b) => getRecordTimeValue(b) - getRecordTimeValue(a));
  const start = (pageNo - 1) * pageSize;
  return { list: list.slice(start, start + pageSize), total: Number(stationPage?.total || 0) + Number(intermediatePageData?.total || 0) };
}

function normalizeIntermediateBusinessRow(row: StationRecord) {
  const header = parseJsonObject(row.headerDataJson);
  const recordId = parseRecordId(header.adhesiveIntermediateRecordId || row.id);
  return {
    ...row,
    id: recordId ? -recordId : row.id,
    headerDataJson: JSON.stringify({ ...header, adhesiveIntermediateRecordId: recordId, recordSource: ADHESIVE_INTERMEDIATE_RECORD_SOURCE }),
  };
}

function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, any>;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
}

function parseRecordId(value?: unknown) {
  const id = Number(String(value ?? '').trim());
  return Number.isFinite(id) && id > 0 ? id : undefined;
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

function stripAdhesiveReportSuffix(value?: null | number | string) {
  const text = String(value ?? '').trim();
  return text.replace(/-J\d+$/iu, '');
}

function formatAdhesiveBatchNo(value?: null | number | string, fallback = '-') {
  return formatText(stripAdhesiveReportSuffix(value), fallback);
}

function inputText(value?: null | number | string) {
  return String(value ?? '').trim();
}

function formatDateTime(value?: null | number | string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format(DATETIME_FORMAT) : String(value);
}

function formatDate(value?: any) {
  if (!value) return '-';
  if (Array.isArray(value) && value.length >= 3) {
    const date = dayjs(`${value[0]}-${value[1]}-${value[2]}`);
    return date.isValid() ? date.format('YYYY-MM-DD') : value.join('-');
  }
  const text = String(value).trim();
  if (/^\d{4},\d{1,2},\d{1,2}$/u.test(text)) {
    const [year, month, day] = text.split(',');
    const date = dayjs(`${year}-${month}-${day}`);
    return date.isValid() ? date.format('YYYY-MM-DD') : text.replaceAll(',', '-');
  }
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD') : text;
}

function stripModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  return text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim() || text;
}

function isAdhesiveStartupForm(record?: StationRecord | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.includes('START') || name.includes('开机点检');
}

function isAdhesiveCleaningForm(record?: StationRecord | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.includes('CLEAN') || name.includes('清洁点检') || name.includes('设备清洁');
}

function isAdhesiveProcessCheckForm(record?: StationRecord | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code === ADHESIVE_PROCESS_CHECK_FORM_CODE || name.includes('工艺参数');
}

function isAdhesiveIntermediateForm(record?: StationRecord | null) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code === ADHESIVE_INTERMEDIATE_FORM_CODE || name.includes('中间品记录');
}

function getAdhesiveDisplayFormName(record?: StationRecord | null) {
  if (isAdhesiveStartupForm(record)) return '粘胶1开机点检表';
  if (isAdhesiveCleaningForm(record)) return '粘胶1设备清洁点检表';
  if (isAdhesiveProcessCheckForm(record)) return '粘胶1工艺参数点检表';
  if (isAdhesiveIntermediateForm(record)) return intermediateRecord.value?.stationFormName || record?.formName || '粘胶1中间品记录表';
  return stripModelPrefix(record?.formName);
}

function isIntermediateBusinessRecord(record?: StationRecord | null) {
  const header = parseJsonObject(record?.headerDataJson);
  return header.recordSource === ADHESIVE_INTERMEDIATE_RECORD_SOURCE;
}

async function fetchIntermediateRecord(record?: StationRecord | null) {
  const header = parseJsonObject(record?.headerDataJson);
  const recordId = parseRecordId(header.adhesiveIntermediateRecordId || Math.abs(Number(record?.id || 0)));
  return recordId ? getAdhesiveConsoleIntermediateById(recordId) : null;
}

function normalizeAttachments(value?: ImportAttachment | ImportAttachment[] | unknown): ImportAttachment[] {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as ImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return { name, path: path || undefined, size: Number(raw.size) || undefined, type: raw.type, uploadTime: raw.uploadTime, url };
    })
    .filter(Boolean)
    .slice(-1) as ImportAttachment[];
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: ImportAttachment) {
  const url = formatText(attachment?.url || attachment?.path, '');
  if (url) window.open(url, '_blank');
}

function buildImportedAttachment(file: File, uploaded: any): ImportAttachment {
  const url = typeof uploaded === 'string' ? uploaded : uploaded?.url;
  return { name: uploaded?.name || file.name, path: uploaded?.path, size: uploaded?.size || file.size, type: uploaded?.type || file.type, uploadTime: dayjs().format(DATETIME_FORMAT), url: url || uploaded?.path };
}

function scopeText(scope?: string) {
  const map: Record<string, string> = { BUSINESS_RECORD: '业务记录', EQUIPMENT_DAILY: '设备日记录', PLAN_OPERATION: '计划工序', PROCESS_DETAIL: '报工明细' };
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
    if (isAdhesiveIntermediateForm(row) && isIntermediateBusinessRecord(row)) {
      currentRecord.value = { ...row, items: [] };
      intermediateRecord.value = await fetchIntermediateRecord(row);
      intermediatePage.value = 1;
      detailVisible.value = true;
      return;
    }
    const detail = await getStationRecordDetail(row.id);
    currentRecord.value = { ...detail, items: detail.items || [] };
    intermediateRecord.value = null;
    detailVisible.value = true;
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailVisible.value = false;
  currentRecord.value = null;
  intermediateRecord.value = null;
  intermediatePage.value = 1;
}

async function refreshIntermediateRecord(recordId: number) {
  intermediateRecord.value = await getAdhesiveConsoleIntermediateById(recordId);
  if (currentRecord.value && intermediateRecord.value) {
    const header = parseJsonObject(intermediateRecord.value.extraJson);
    currentRecord.value = {
      ...currentRecord.value,
      batchNo: intermediateRecord.value.batchNo || currentRecord.value.batchNo,
      headerDataJson: JSON.stringify({ ...header, adhesiveIntermediateRecordId: intermediateRecord.value.id, recordSource: ADHESIVE_INTERMEDIATE_RECORD_SOURCE }),
      recordTime: intermediateRecord.value.recordTime || currentRecord.value.recordTime,
      recordUserName: intermediateRecord.value.recorderName || currentRecord.value.recordUserName,
    };
  }
}

function buildHeaderDataWithAttachment(attachment?: ImportAttachment) {
  const header = { ...headerData.value };
  if (attachment) {
    header.attachments = [attachment];
    header.importAttachment = attachment;
    header.importTime = attachment.uploadTime;
  }
  return header;
}

function buildIntermediateExtraWithAttachment(attachment?: ImportAttachment) {
  const extra = { ...intermediateExtra.value };
  if (attachment) {
    extra.attachments = [attachment];
    extra.importAttachment = attachment;
    extra.importTime = attachment.uploadTime;
  }
  return extra;
}

function applyImportedStationExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  (resp.cellValues || []).forEach((item) => {
    const row = detailRows.value[Number(item.bindKey)];
    if (!row || !item.bindField) return;
    if (item.bindField === 'resultFlag') {
      return;
    } else if (['actualValue', 'actualValue2', 'abnormalRemark'].includes(item.bindField)) {
      (row as any)[item.bindField] = inputText(item.value);
    }
  });
}

function applyImportedIntermediateExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  const record = intermediateRecord.value;
  if (!record) return 0;
  const next: AdhesiveIntermediateRecord = { ...record };
  let appliedCount = 0;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'headerData' || !item.bindKey) return;
    const value = inputText(item.value);
    if (!value && value !== '0') return;
    (next as any)[item.bindKey] = value;
    appliedCount += 1;
  });
  const rowMap = new Map<number, Record<string, string>>();
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
  next.details = rowMap.size
    ? [...rowMap.keys()].sort((a, b) => a - b).map((sourceIndex, targetIndex) => {
        const imported = rowMap.get(sourceIndex) || {};
        const base = record.details?.[sourceIndex] || record.details?.[targetIndex] || {};
        return {
          ...base,
          lengthMark: toNumberOrText(imported.lengthMeter ?? base.lengthMark ?? targetIndex + 1),
          leftThickness: toNumberOrText(imported.innerThickness ?? base.leftThickness),
          rightThickness: toNumberOrText(imported.outerThickness ?? base.rightThickness),
          remark: imported.remark ?? base.remark ?? '',
          sortNo: targetIndex + 1,
        };
      })
    : [...(record.details || [])];
  intermediateRecord.value = next;
  intermediatePage.value = 1;
  return appliedCount;
}

function toNumberOrText(value: any) {
  const text = inputText(value);
  if (!text && text !== '0') return undefined;
  const num = Number(text);
  return Number.isFinite(num) ? num : (text as any);
}

function calcRecordResult() {
  return detailRows.value.some((row) => normalizeResultFlag(row.resultFlag) === 'NG') ? 'NG' : 'OK';
}

async function saveImportedStationRecord(attachment?: ImportAttachment) {
  if (!currentRecord.value?.id) return;
  const result = calcRecordResult();
  await updateStationRecord({
    ...currentRecord.value,
    headerDataJson: JSON.stringify(buildHeaderDataWithAttachment(attachment)),
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
  currentRecord.value = { ...detail, items: detail.items || [] };
}

function buildIntermediateExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  const record = intermediateRecord.value;
  return [
    { editable: false, label: '计划号', value: currentRecord.value?.planNo || '' },
    { editable: false, label: '当前工序', value: currentRecord.value?.operationName || '粘胶1' },
    { bindField: 'headerData', bindKey: 'modelCode', editable: true, label: '型号', value: record?.modelCode || '' },
    { bindField: 'headerData', bindKey: 'materialCode', editable: true, label: '料号', value: record?.materialCode || '' },
    { bindField: 'headerData', bindKey: 'batchNo', editable: true, label: '批号', value: record?.batchNo || '' },
    { bindField: 'headerData', bindKey: 'processLength', editable: true, label: '加工米数/m', value: record?.processLength ?? '' },
    { bindField: 'headerData', bindKey: 'productWidthMm', editable: true, label: '产品宽幅mm', value: record?.productWidthMm ?? '' },
    { bindField: 'headerData', bindKey: 'productionDate', editable: true, label: '生产日期', value: record?.productionDate || '' },
    { bindField: 'headerData', bindKey: 'recorderName', editable: true, label: '记录人', value: record?.recorderName || '' },
    { bindField: 'headerData', bindKey: 'recordTime', editable: true, label: '记录时间', value: record?.recordTime || '' },
    { bindField: 'headerData', bindKey: 'confirmerName', editable: true, label: '确认人', value: record?.confirmerName || '' },
  ];
}

function buildExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isIntermediateForm.value) {
    return intermediateRows.value.map((row, index) => ({
      cells: ADHESIVE_MIDDLE_PRODUCT_COLUMNS.value.map((column, colIndex) =>
        createRoughLayoutCell(colIndex, row[column.key], { bindField: column.bindField || column.key, bindKey: String(index), editable: true }),
      ),
    }));
  }
  if (isProcessCheckForm.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      const nodeSpan = getFieldRowSpan(index, 'stepNode');
      if (categorySpan > 0) cells.push(createRoughLayoutCell(0, row.itemCategory || '', { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(createRoughLayoutCell(1, row.stepNode || '', { rowSpan: nodeSpan }));
      cells.push(createRoughLayoutCell(2, row.itemName || ''));
      cells.push(createRoughLayoutCell(3, row.standardText || ''));
      cells.push(createRoughLayoutCell(4, row.actualValue || '', { bindField: 'actualValue', bindKey: String(index), editable: true }));
      cells.push(createRoughLayoutCell(5, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey: String(index), editable: true }));
      return { cells };
    });
  }
  return detailRows.value.map((row, index) => ({
    cells: detailColumns.value.map((column: any, colIndex) =>
      createRoughLayoutCell(colIndex, column.key === 'itemSeq' ? row.itemSeq || index + 1 : (row as any)[column.key] || '', {
        bindField: column.bindField,
        bindKey: column.bindField ? String(index) : undefined,
        editable: !!column.bindField,
      }),
    ),
  }));
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = getAdhesiveDisplayFormName(currentRecord.value) || '粘胶1记录';
  if (isIntermediateForm.value) {
    return {
      columns: toRoughExcelColumns(ADHESIVE_MIDDLE_PRODUCT_COLUMNS.value),
      detailTitle: '中间品记录明细',
      fileName: `${formatText(currentRecord.value?.planNo, '粘胶1')}_${formName}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
      headerItems: buildIntermediateExcelHeaderItems(),
      importStopPrefixes: ['填写要求：', '修订信息：', '版权声明：'],
    footerNotes: buildAdhesiveIntermediateFooterNotes(intermediateRecord.value?.formNotes),
      rows: buildExcelRows(),
      sheetName: '粘胶1中间品记录',
      title: formName,
      visualMode: 'adhesive1-middle',
    };
  }
  return {
    columns: toRoughExcelColumns(detailColumns.value as any),
    detailTitle: '明细项目',
    fileName: `${formatText(currentRecord.value?.planNo, '粘胶1')}_${formName}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_'),
    headerItems: [
      { editable: false, label: '计划号', value: currentRecord.value?.planNo || '' },
      { editable: false, label: '当前工序', value: currentRecord.value?.operationName || '' },
      { editable: false, label: '执行时机', value: currentRecord.value?.triggerTimingName || '' },
      ...headerFields.value.map((field) => ({ editable: false, label: field.label, value: field.value || '' })),
    ],
    rows: buildExcelRows(),
    sheetName: '粘胶1过站记录',
    title: formName,
    visualMode: 'rough-pass-work',
  };
}

async function handleExportExcel() {
  if (!currentRecord.value) return;
  excelLoading.value = true;
  try {
    const layout = buildExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || '粘胶1记录.xlsx', source: data });
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
    const layout = buildExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    if (isIntermediateForm.value) {
      const appliedCount = applyImportedIntermediateExcel(resp);
      if (!intermediateRecord.value?.id) {
        message.warning('未找到粘胶1中间品记录单');
        return;
      }
      const uploaded = await uploadFile({ directory: 'mes/adhesive1-intermediate', file });
      const attachment = buildImportedAttachment(file, uploaded);
      const latest = intermediateRecord.value;
      const recordId = await saveAdhesiveConsoleIntermediate({
        ...latest,
        details: latest.details || [],
        extraJson: JSON.stringify(buildIntermediateExtraWithAttachment(attachment)),
        id: latest.id,
        recordStatus: 'RECORDED',
      });
      await refreshIntermediateRecord(recordId);
      message.success(`导入成功，已回填 ${appliedCount} 个有值单元格并挂接原始附件`);
      return;
    }
    applyImportedStationExcel(resp);
    const uploaded = await uploadFile({ directory: 'mes/adhesive1-station-record', file });
    await saveImportedStationRecord(buildImportedAttachment(file, uploaded));
    message.success(`导入成功，已回填 ${resp.totalCellCount || 0} 个单元格并挂接原始附件`);
  } finally {
    excelLoading.value = false;
  }
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="adhesive-station-record-page">
    <div class="adhesive-record-layout">
      <Grid v-if="!props.viewerOnly" @cell-dblclick="({ row }) => openDetail(row)">
        <template #formName="{ row }">
          <button class="adhesive-record-link" type="button" @click.stop="openDetail(row)">
            {{ getAdhesiveDisplayFormName(row) || '-' }}
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
      wrap-class-name="rough-station-record-modal adhesive-station-record-modal"
      @cancel="closeDetail"
    >
      <RoughMiddleProductRecordSheet
        v-if="currentRecord && isIntermediateForm"
        v-model:page="intermediatePage"
        :attachments="intermediateAttachments"
        :columns="[...ADHESIVE_MIDDLE_PRODUCT_COLUMNS]"
        detail-title="中间品记录明细"
        :editable="false"
        :head-fields="intermediateHeadFields"
        :meta-items="intermediateMetaItems"
        :page-size="MIDDLE_PRODUCT_PAGE_SIZE"
        :rows="intermediateRows"
        :signature-fields="intermediateSignatureFields"
        :title="dialogTitle"
        @open-attachment="downloadImportAttachment"
      >
        <template #actions>
          <input ref="excelImportInputRef" accept=".xls,.xlsx,.xlsm" hidden type="file" @change="handleExcelImportChange" />
          <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
          <Button size="small" :loading="excelLoading" v-if="!props.viewerOnly" @click="triggerImportExcel">导入Excel</Button>
          <Button size="small" @click="closeDetail">关闭</Button>
        </template>
        <template #extra-sections>
          <div v-for="note in intermediateNotes" :key="note.key" style="white-space: pre-wrap; padding: 4px 8px;"><strong>{{ note.label }}：</strong>{{ note.text }}</div>
        </template>
      </RoughMiddleProductRecordSheet>

      <div v-else class="adhesive-detail-modal">
        <div class="adhesive-detail-toolbar">
          <div class="adhesive-detail-toolbar__title">
            <span class="adhesive-detail-toolbar__main">{{ dialogTitle }}</span>
            <div class="adhesive-detail-toolbar__meta">
              <span v-if="isDailyForm">记录范围：设备日记录</span>
              <span v-else>计划号：{{ formatText(currentRecord?.planNo) }}</span>
              <span>当前工序：{{ formatText(currentRecord?.operationName) }}</span>
              <span>执行时机：{{ formatText(currentRecord?.triggerTimingName) }}</span>
            </div>
          </div>
          <div class="adhesive-detail-toolbar__actions">
            <input ref="excelImportInputRef" accept=".xls,.xlsx,.xlsm" hidden type="file" @change="handleExcelImportChange" />
            <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
            <Button size="small" :loading="excelLoading" v-if="!props.viewerOnly" @click="triggerImportExcel">导入Excel</Button>
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="adhesive-detail-body">
          <fieldset class="adhesive-fieldset">
            <legend>表单信息</legend>
            <div class="adhesive-head-grid">
              <div v-for="item in headerFields" :key="item.field" class="adhesive-head-item">
                <span class="adhesive-head-item__label">{{ item.label }}</span>
                <span class="adhesive-head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachment" class="adhesive-import-attachment">
              <span class="adhesive-import-attachment__label">导入附件</span>
              <Button size="small" type="link" @click="downloadImportAttachment(currentImportAttachment)">
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>导入时间：{{ formatDateTime(currentImportAttachment.uploadTime) }}</span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="adhesive-detail-panel">
            <div class="adhesive-detail-panel__header">明细项目</div>
            <div class="adhesive-table-wrap">
              <table class="adhesive-grid">
                <thead>
                  <tr>
                    <th v-for="column in detailColumns" :key="column.key" :width="column.width">{{ column.title }}</th>
                  </tr>
                </thead>
                <tbody v-if="isProcessCheckForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td v-if="getFieldRowSpan(index, 'itemCategory') > 0" :rowspan="getFieldRowSpan(index, 'itemCategory')">{{ formatText(item.itemCategory) }}</td>
                    <td v-if="getFieldRowSpan(index, 'stepNode') > 0" :rowspan="getFieldRowSpan(index, 'stepNode')">{{ formatText(item.stepNode) }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length"><td :colspan="detailColumnCount" class="adhesive-empty-cell" align="center">暂无明细数据</td></tr>
                </tbody>
                <tbody v-else-if="isCleaningForm">
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td v-if="getFieldRowSpan(index, 'itemCategory') > 0" :rowspan="getFieldRowSpan(index, 'itemCategory')">{{ formatText(item.itemCategory) }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length"><td :colspan="detailColumnCount" class="adhesive-empty-cell" align="center">暂无明细数据</td></tr>
                </tbody>
                <tbody v-else>
                  <tr v-for="(item, index) in detailRows" :key="item.id || index">
                    <td align="center">{{ item.itemSeq || index + 1 }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="!detailRows.length"><td :colspan="detailColumnCount" class="adhesive-empty-cell" align="center">暂无明细数据</td></tr>
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
.adhesive-station-record-page,
.adhesive-record-layout,
.adhesive-detail-modal,
.adhesive-detail-body,
.adhesive-detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.adhesive-record-layout {
  gap: 8px;
  height: 100%;
}

.adhesive-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.adhesive-record-link:hover {
  text-decoration: underline;
}

.adhesive-detail-modal {
  height: 100vh;
  background: #f5f7fa;
}

.adhesive-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.adhesive-detail-toolbar__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: baseline;
  min-width: 0;
}

.adhesive-detail-toolbar__main {
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.adhesive-detail-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 13px;
}

.adhesive-detail-toolbar__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.adhesive-detail-body {
  flex: 1;
  gap: 10px;
  padding: 10px;
  overflow: hidden;
}

.adhesive-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.adhesive-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 800;
}

.adhesive-head-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid #d8e0ea;
  border-left: 1px solid #d8e0ea;
}

.adhesive-head-item {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #d8e0ea;
  border-bottom: 1px solid #d8e0ea;
}

.adhesive-head-item__label,
.adhesive-head-item__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.adhesive-head-item__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: #eef2f7;
  border-right: 1px solid #d8e0ea;
}

.adhesive-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.adhesive-import-attachment__label {
  color: #1677ff;
  font-weight: 800;
}

.adhesive-detail-panel {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.adhesive-detail-panel__header {
  flex-shrink: 0;
  min-height: 34px;
  padding: 7px 10px;
  color: #1677ff;
  font-weight: 800;
  background: #eef2f7;
  border-bottom: 1px solid #d8e0ea;
}

.adhesive-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.adhesive-grid {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: fixed;
}

.adhesive-grid th,
.adhesive-grid td {
  min-height: 30px;
  padding: 4px 6px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #d8e0ea;
}

.adhesive-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-weight: 800;
  text-align: center;
  background: #eef2f7;
}

.adhesive-empty-cell {
  color: #94a3b8;
}

:global(.adhesive-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.adhesive-station-record-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}

:global(.adhesive-station-record-modal .ant-modal-body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:global(.adhesive-station-record-modal .rough-middle-sheet) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}
</style>
