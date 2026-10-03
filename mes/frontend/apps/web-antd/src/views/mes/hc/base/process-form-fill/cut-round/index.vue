<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, message, Modal, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { uploadFile } from '#/api/infra/file';
import {
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  importProcessFormRecordLayout,
  updateProcessFormRecord,
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

defineOptions({ name: 'MesHcProcessFormFillCutRound' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type StationRecord = MesHcStationRecordApi.Record & {
  processRecordId?: number;
  sourceKind?: 'process-form' | 'station-record';
};
type ImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

const CUT_ROUND_FORM_CODE_PREFIX = 'CUT_ROUND_';
const CUT_ROUND_PROCESS_CODE = 'CUT_ROUND';
const PROCESS_FORM_ROW_OFFSET = 1_000_000_000;
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const currentRecord = ref<null | StationRecord>(null);
const currentProcessFormRecord = ref<MesHcProcessFormApi.Record | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const excelLoading = ref(false);
const excelImportInputRef = ref<HTMLInputElement>();

const detailRows = computed(() => currentRecord.value?.items || []);
const headerData = computed(() =>
  parseJsonObject(currentRecord.value?.headerDataJson),
);
const isStartupForm = computed(() =>
  isCutRoundStartupForm(currentRecord.value),
);
const isCleaningForm = computed(() =>
  isCutRoundCleaningForm(currentRecord.value),
);
const isProcessCheckForm = computed(() =>
  isCutRoundProcessCheckForm(currentRecord.value),
);
const isDailyForm = computed(() => isStartupForm.value || isCleaningForm.value);
const dialogTitle = computed(
  () => `查看${getCutRoundDisplayFormName(currentRecord.value) || '裁切记录'}`,
);
const currentImportAttachment = computed(
  () =>
    normalizeAttachments(
      headerData.value.attachments || headerData.value.importAttachment,
    )[0],
);

const detailColumns = computed(() => {
  if (isProcessCheckForm.value) {
    return [
      { key: 'itemCategory', title: '类别', width: 130 },
      { key: 'stepNode', title: '确认节点', width: 120 },
      { key: 'itemName', title: '点检项目', width: 220 },
      { key: 'standardText', title: '标准', width: 260 },
      {
        bindField: 'actualValue',
        key: 'actualValue',
        title: '实际/记录',
        width: 180,
      },
      {
        bindField: 'abnormalRemark',
        key: 'abnormalRemark',
        title: '异常备注',
        width: 240,
      },
    ];
  }
  if (isCleaningForm.value) {
    return [
      { key: 'itemCategory', title: '检验项目', width: 120 },
      { key: 'itemName', title: '清洁部位', width: 220 },
      { key: 'standardText', title: '检查内容', width: 420 },
      {
        bindField: 'actualValue',
        key: 'actualValue',
        title: '实际/记录',
        width: 220,
      },
      {
        bindField: 'abnormalRemark',
        key: 'abnormalRemark',
        title: '备注',
        width: 240,
      },
    ];
  }
  return [
    { key: 'itemSeq', title: '序号', width: 80 },
    { key: 'itemName', title: '检查项目', width: 260 },
    { key: 'standardText', title: '检查标准', width: 420 },
    {
      bindField: 'actualValue',
      key: 'actualValue',
      title: '实际/记录',
      width: 220,
    },
    {
      bindField: 'abnormalRemark',
      key: 'abnormalRemark',
      title: '备注',
      width: 240,
    },
  ];
});
const detailColumnCount = computed(() => detailColumns.value.length);

const headerFields = computed(() => {
  const record = currentRecord.value;
  const header = headerData.value;
  return [
    { field: 'planNo', label: '计划号', value: record?.planNo || '-' },
    {
      field: 'batchNo',
      label: '批次号',
      value:
        header.productionBatchNo || header.batchNo || record?.batchNo || '-',
    },
    {
      field: 'modelCode',
      label: '型号',
      value: header.modelCode || record?.modelName || record?.modelCode || '-',
    },
    {
      field: 'equipment',
      label: '设备',
      value: record?.equipmentCode || record?.equipmentName || '-',
    },
    {
      field: 'recordUserName',
      label: '记录人',
      value: record?.recordUserName || '-',
    },
    {
      field: 'recordTime',
      label: '记录时间',
      value: formatDateTime(record?.recordTime),
    },
    {
      field: 'confirmUserName',
      label: '确认人',
      value: record?.confirmUserName || '-',
    },
    {
      field: 'confirmTime',
      label: '确认时间',
      value: formatDateTime(record?.confirmTime),
    },
  ];
});

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { placeholder: '计划号 / 表单 / 批次 / 设备' },
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
  {
    field: 'formName',
    title: '表单名称',
    minWidth: 260,
    slots: { default: 'formName' },
  },
  {
    field: 'planNo',
    title: '计划号',
    minWidth: 150,
    formatter: ({ cellValue }) => formatText(cellValue),
  },
  {
    field: 'batchNo',
    title: '批次号',
    minWidth: 160,
    formatter: ({ row }) => formatRecordBatch(row),
  },
  {
    field: 'modelCode',
    title: '型号',
    minWidth: 130,
    formatter: ({ row }) => formatText(row.modelName || row.modelCode),
  },
  {
    field: 'equipmentName',
    title: '设备',
    minWidth: 140,
    formatter: ({ row }) => formatText(row.equipmentCode || row.equipmentName),
  },
  {
    field: 'recordDate',
    title: '记录日期',
    width: 120,
    formatter: ({ cellValue }) => formatDate(cellValue),
  },
  {
    field: 'recordScope',
    title: '记录范围',
    width: 120,
    formatter: ({ cellValue }) => scopeText(cellValue),
  },
  {
    field: 'docStatus',
    title: '状态',
    width: 110,
    align: 'center',
    slots: { default: 'docStatus' },
  },
  {
    field: 'resultStatus',
    title: '结果',
    width: 90,
    align: 'center',
    slots: { default: 'resultStatus' },
  },
  {
    field: 'recordUserName',
    title: '记录人',
    width: 110,
    formatter: ({ cellValue }) => formatText(cellValue),
  },
  {
    field: 'recordTime',
    title: '记录时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  {
    field: 'confirmUserName',
    title: '确认人',
    width: 110,
    formatter: ({ cellValue }) => formatText(cellValue),
  },
  {
    field: 'confirmTime',
    title: '确认时间',
    width: 170,
    formatter: ({ cellValue }) => formatDateTime(cellValue),
  },
  {
    title: '操作',
    width: 100,
    fixed: 'right',
    align: 'center',
    slots: { default: 'actions' },
  },
];

const [Grid] = useVbenVxeGrid({
  formOptions: { schema: gridFormSchema },
  gridOptions: {
    columns: recordColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: { ajax: { query: queryCutRoundRecordPage } },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<StationRecord>,
});

async function queryCutRoundRecordPage(
  { page }: any,
  formValues: Record<string, any>,
) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  const queryPageSize = pageNo * pageSize;
  const params = { ...formValues, pageNo: 1, pageSize: queryPageSize };
  const [stationPage, processFormPage] = await Promise.all([
    getStationRecordPage({
      ...params,
      formCodePrefix: CUT_ROUND_FORM_CODE_PREFIX,
    }),
    getProcessFormRecordPage({
      ...params,
      processCode: CUT_ROUND_PROCESS_CODE,
    }),
  ]);
  const processRows = (
    (processFormPage?.list || []) as MesHcProcessFormApi.Record[]
  )
    .map((row) => normalizeProcessFormRow(row))
    .filter((row) => matchesProcessFormListFilters(row, formValues));
  const list = [
    ...((stationPage?.list || []) as StationRecord[]).map((row) => ({
      ...row,
      sourceKind: 'station-record' as const,
    })),
    ...processRows,
  ].toSorted((a, b) => getRecordTimeValue(b) - getRecordTimeValue(a));
  const start = (pageNo - 1) * pageSize;
  return {
    list: list.slice(start, start + pageSize),
    total: Number(stationPage?.total || 0) + processRows.length,
  };
}

function matchesProcessFormListFilters(
  row: StationRecord,
  filters: Record<string, any>,
) {
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
      row.recordUserName,
      row.confirmUserName,
    ];
    if (
      !values.some((value) =>
        String(value || '')
          .toLowerCase()
          .includes(keyword),
      )
    ) {
      return false;
    }
  }
  const formName = inputText(filters.formName).toLowerCase();
  if (
    formName &&
    !String(row.formName || '')
      .toLowerCase()
      .includes(formName)
  ) {
    return false;
  }
  const equipmentName = inputText(filters.equipmentName).toLowerCase();
  if (
    equipmentName &&
    ![row.equipmentCode, row.equipmentName].some((value) =>
      String(value || '')
        .toLowerCase()
        .includes(equipmentName),
    )
  ) {
    return false;
  }
  return true;
}

function normalizeProcessFormRow(
  row: MesHcProcessFormApi.Record,
): StationRecord {
  const header = parseJsonObject(row.headerDataJson);
  const id = parseRecordId(row.id);
  return {
    batchNo:
      header.motherBatchNo ||
      header.parentProductionBatchNo ||
      header.batchNo ||
      row.batchNo,
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
    inspectionResult: row.resultStatus,
    modelCode: row.modelCode,
    modelName: row.modelName,
    operationName: row.processName || '裁切',
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    processRecordId: id,
    recordDate: row.recordDate,
    recordScope: 'PROCESS_DETAIL',
    recordTime: row.fillTime || row.createTime,
    recordUserName: row.fillUserName,
    resultStatus: row.resultStatus,
    sourceKind: 'process-form',
    triggerTimingCode: row.formType,
    triggerTimingName: row.formTypeName,
    updateTime: (row as any).updateTime || row.createTime,
  };
}

function normalizeProcessFormDetail(
  row: MesHcProcessFormApi.Record,
): StationRecord {
  return {
    ...normalizeProcessFormRow(row),
    items: (row.items || []).map((item, index) => ({
      abnormalRemark: item.abnormalRemark,
      actualValue: item.actualValue,
      actualValue2: item.actualValue2,
      id: item.id,
      itemCategory: item.itemCategory,
      itemName: item.fieldLabel,
      itemSeq: item.itemSeq || index + 1,
      resultFlag: item.resultFlag,
      standardText: item.standardText,
      stepNode: item.stepNode,
      valueMode: item.valueMode,
    })),
  };
}

function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, any>;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, any>)
      : {};
  } catch {
    return {};
  }
}

function parseRecordId(value?: unknown) {
  const id = Number(String(value ?? '').trim());
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function getRecordTimeValue(record?: null | StationRecord) {
  const value =
    record?.recordTime ||
    record?.createTime ||
    record?.updateTime ||
    record?.confirmTime;
  const time = value ? dayjs(value).valueOf() : 0;
  return Number.isFinite(time) ? time : 0;
}

function formatText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
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
    return date.isValid()
      ? date.format('YYYY-MM-DD')
      : text.replaceAll(',', '-');
  }
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD') : text;
}

function stripModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  const leftIndex = text.search(/[（(]/u);
  if (leftIndex < 0) return text;
  const closeIndexes = [
    text.indexOf('）', leftIndex + 1),
    text.indexOf(')', leftIndex + 1),
  ].filter((index) => index >= 0);
  if (closeIndexes.length === 0) return text;
  return text.slice(Math.min(...closeIndexes) + 1).trim() || text;
}

function formatRecordBatch(record?: null | StationRecord) {
  const header = parseJsonObject(record?.headerDataJson);
  return formatText(
    header.productionBatchNo || header.batchNo || record?.batchNo,
  );
}

function isCutRoundStartupForm(record?: null | StationRecord) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return code.includes('START') || name.includes('开机点检');
}

function isCutRoundCleaningForm(record?: null | StationRecord) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return (
    code.includes('CLEAN') ||
    name.includes('清洁点检') ||
    name.includes('设备清洁')
  );
}

function isCutRoundProcessCheckForm(record?: null | StationRecord) {
  const code = String(record?.formCode || '').toUpperCase();
  const name = String(record?.formName || '');
  return (
    record?.sourceKind === 'process-form' ||
    code.includes('PRODUCTION_CHECK') ||
    name.includes('生产点检') ||
    name.includes('工艺参数')
  );
}

function getCutRoundDisplayFormName(record?: null | StationRecord) {
  if (isCutRoundStartupForm(record)) return '裁切开机点检表';
  if (isCutRoundCleaningForm(record)) return '裁切设备清洁点检表';
  if (isCutRoundProcessCheckForm(record))
    return stripModelPrefix(record?.formName) || '裁切生产点检表';
  return stripModelPrefix(record?.formName);
}

function normalizeDocStatus(status?: string) {
  const text = String(status || '')
    .trim()
    .toUpperCase();
  if (text === 'SUBMITTED') return 'RECORDED';
  return text || status;
}

function scopeText(scope?: string) {
  const map: Record<string, string> = {
    BUSINESS_RECORD: '业务记录',
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
    FILLED: { color: 'processing', text: '已填写' },
    PENDING_CHECK: { color: 'default', text: '待执行' },
    RECORDED: { color: 'processing', text: '已填写' },
    SUBMITTED: { color: 'processing', text: '已填写' },
  };
  return (
    map[String(status || '').toUpperCase()] || {
      color: 'default',
      text: status || '-',
    }
  );
}

function resultMeta(result?: string) {
  if (result === 'OK' || result === 'PASS')
    return { color: 'success', text: result };
  if (result === 'NG' || result === 'FAIL')
    return { color: 'error', text: result };
  return { color: 'default', text: result || '-' };
}

function normalizeResultFlag(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (!text) return '';
  if (text.includes('NG') || text.includes('不合格')) return 'NG';
  if (text.includes('NA') || text.includes('N/A') || text.includes('不适用'))
    return 'NA';
  if (text.includes('OK') || text.includes('合格')) return 'OK';
  return text;
}

function normalizeAttachments(
  value?: ImportAttachment | ImportAttachment[] | unknown,
): ImportAttachment[] {
  const list: unknown[] = Array.isArray(value) ? value : [];
  if (!Array.isArray(value) && value) {
    list.push(value);
  }
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as ImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(
        raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`,
      );
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
    .slice(-1) as ImportAttachment[];
}

function formatAttachmentSize(size?: number) {
  if (size === undefined || size <= 0) return '';
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
  const uploadedSize = Number(uploaded?.size);
  return {
    name: uploaded?.name || file.name,
    path: uploaded?.path,
    size: uploadedSize > 0 ? uploadedSize : file.size,
    type: uploaded?.type || file.type,
    uploadTime: dayjs().format(DATETIME_FORMAT),
    url: url || uploaded?.path,
  };
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

function buildHeaderDataWithAttachment(attachment?: ImportAttachment) {
  const header = { ...headerData.value };
  if (attachment) {
    header.attachments = [attachment];
    header.importAttachment = attachment;
    header.importTime = attachment.uploadTime;
  }
  return header;
}

function applyImportedExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  (resp.cellValues || []).forEach((item) => {
    const row = detailRows.value[Number(item.bindKey)];
    if (!row || !item.bindField) return;
    if (item.bindField === 'resultFlag') {
      return;
    } else if (
      ['abnormalRemark', 'actualValue', 'actualValue2'].includes(item.bindField)
    ) {
      (row as any)[item.bindField] = inputText(item.value);
    }
  });
}

function calcRecordResult() {
  return detailRows.value.some(
    (row) => normalizeResultFlag(row.resultFlag) === 'NG',
  )
    ? 'NG'
    : 'OK';
}

async function saveImportedRecord(attachment?: ImportAttachment) {
  if (!currentRecord.value?.id) return;
  const result = calcRecordResult();
  if (
    currentRecord.value.sourceKind === 'process-form' &&
    currentRecord.value.processRecordId
  ) {
    const source: MesHcProcessFormApi.Record =
      currentProcessFormRecord.value || {};
    await updateProcessFormRecord({
      ...source,
      batchNo: currentRecord.value.batchNo,
      confirmTime: currentRecord.value.confirmTime,
      confirmUserName: currentRecord.value.confirmUserName,
      equipmentCode: currentRecord.value.equipmentCode,
      equipmentId: currentRecord.value.equipmentId,
      equipmentName: currentRecord.value.equipmentName,
      headerDataJson: JSON.stringify(buildHeaderDataWithAttachment(attachment)),
      id: currentRecord.value.processRecordId,
      items: (source.items || []).map((item, index) => {
        const edited = detailRows.value[index] || {};
        return {
          ...item,
          abnormalRemark: edited.abnormalRemark,
          actualValue: edited.actualValue,
          actualValue2: edited.actualValue2,
          itemSeq: edited.itemSeq || item.itemSeq || index + 1,
          resultFlag:
            normalizeResultFlag(edited.resultFlag) ||
            edited.resultFlag ||
            item.resultFlag,
        };
      }),
      planId: currentRecord.value.planId,
      planNo: currentRecord.value.planNo,
      planOperationId: currentRecord.value.planOperationId,
      recordDate: currentRecord.value.recordDate,
      recordStatus: 'RECORDED',
      resultStatus: result,
    });
    const detail = await getProcessFormRecordDetail(
      currentRecord.value.processRecordId,
    );
    currentProcessFormRecord.value = detail;
    currentRecord.value = normalizeProcessFormDetail(detail);
    return;
  }
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
  const detail = await getStationRecordDetail(Number(currentRecord.value.id));
  currentRecord.value = {
    ...detail,
    items: detail.items || [],
    sourceKind: 'station-record',
  };
}

function buildExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isProcessCheckForm.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, 'itemCategory');
      const nodeSpan = getFieldRowSpan(index, 'stepNode');
      if (categorySpan > 0)
        cells.push(
          createRoughLayoutCell(0, row.itemCategory || '', {
            rowSpan: categorySpan,
          }),
        );
      if (nodeSpan > 0)
        cells.push(
          createRoughLayoutCell(1, row.stepNode || '', { rowSpan: nodeSpan }),
        );
      cells.push(
        createRoughLayoutCell(2, row.itemName || ''),
        createRoughLayoutCell(3, row.standardText || ''),
        createRoughLayoutCell(4, row.actualValue || '', {
          bindField: 'actualValue',
          bindKey: String(index),
          editable: true,
        }),
        createRoughLayoutCell(5, row.abnormalRemark || '', {
          bindField: 'abnormalRemark',
          bindKey: String(index),
          editable: true,
        }),
      );
      return { cells };
    });
  }
  return detailRows.value.map((row, index) => ({
    cells: detailColumns.value.map((column: any, colIndex) =>
      createRoughLayoutCell(
        colIndex,
        column.key === 'itemSeq'
          ? row.itemSeq || index + 1
          : (row as any)[column.key] || '',
        {
          bindField: column.bindField,
          bindKey: column.bindField ? String(index) : undefined,
          editable: !!column.bindField,
        },
      ),
    ),
  }));
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName =
    getCutRoundDisplayFormName(currentRecord.value) || '裁切记录';
  return {
    columns: toRoughExcelColumns(detailColumns.value as any),
    detailTitle: '明细项目',
    fileName:
      `${formatText(currentRecord.value?.planNo, '裁切')}_${formName}.xlsx`.replaceAll(
        /[\\/:*?"<>|]/gu,
        '_',
      ),
    headerItems: [
      {
        editable: false,
        label: '计划号',
        value: currentRecord.value?.planNo || '',
      },
      {
        editable: false,
        label: '当前工序',
        value: currentRecord.value?.operationName || '',
      },
      {
        editable: false,
        label: '执行时机',
        value: currentRecord.value?.triggerTimingName || '',
      },
      ...headerFields.value.map((field) => ({
        editable: false,
        label: field.label,
        value: field.value || '',
      })),
    ],
    rows: buildExcelRows(),
    sheetName: '裁切记录',
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
    downloadFileFromBlobPart({
      fileName: layout.fileName || '裁切记录.xlsx',
      source: data,
    });
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
    applyImportedExcel(resp);
    const uploaded = await uploadFile({
      directory: 'mes/cut-round-station-record',
      file,
    });
    await saveImportedRecord(buildImportedAttachment(file, uploaded));
    message.success(
      `导入成功，已回填 ${resp.totalCellCount || 0} 个单元格并挂接原始附件`,
    );
  } finally {
    excelLoading.value = false;
  }
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="cut-round-station-record-page">
    <div class="cut-round-record-layout">
      <Grid>
        <template #formName="{ row }">
          <button
            class="cut-round-record-link"
            type="button"
            @click.stop="openDetail(row)"
          >
            {{ getCutRoundDisplayFormName(row) || row.formName || '-' }}
          </button>
        </template>
        <template #docStatus="{ row }">
          <Tag :color="statusMeta(row.docStatus).color">
            {{ statusMeta(row.docStatus).text }}
          </Tag>
        </template>
        <template #resultStatus="{ row }">
          <Tag
            :color="resultMeta(row.resultStatus || row.inspectionResult).color"
          >
            {{ resultMeta(row.resultStatus || row.inspectionResult).text }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <Button
            :loading="detailLoading"
            size="small"
            type="link"
            @click.stop="openDetail(row)"
          >
            查看
          </Button>
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      destroy-on-close
      width="100vw"
      wrap-class-name="rough-station-record-modal cut-round-station-record-modal"
      @cancel="closeDetail"
    >
      <div class="cut-round-detail-modal">
        <div class="cut-round-detail-toolbar">
          <div class="cut-round-detail-toolbar__title">
            <span class="cut-round-detail-toolbar__main">
              {{ dialogTitle }}
            </span>
            <div class="cut-round-detail-toolbar__meta">
              <span v-if="isDailyForm">记录范围：设备日记录</span>
              <span v-else>
                计划号：{{ formatText(currentRecord?.planNo) }}
              </span>
              <span>
                当前工序：{{ formatText(currentRecord?.operationName) }}
              </span>
              <span>
                执行时机：{{ formatText(currentRecord?.triggerTimingName) }}
              </span>
            </div>
          </div>
          <div class="cut-round-detail-toolbar__actions">
            <input
              ref="excelImportInputRef"
              accept=".xls,.xlsx,.xlsm"
              hidden
              type="file"
              @change="handleExcelImportChange"
            />
            <Button
              size="small"
              :loading="excelLoading"
              @click="handleExportExcel"
            >
              导出Excel
            </Button>
            <Button
              size="small"
              :loading="excelLoading"
              v-if="!props.viewerOnly" @click="triggerImportExcel"
            >
              导入Excel
            </Button>
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="cut-round-detail-body">
          <fieldset class="cut-round-fieldset">
            <legend>表单信息</legend>
            <div class="cut-round-head-grid">
              <div
                v-for="item in headerFields"
                :key="item.field"
                class="cut-round-head-item"
              >
                <span class="cut-round-head-item__label">{{ item.label }}</span>
                <span class="cut-round-head-item__value">{{
                  item.value || '-'
                }}</span>
              </div>
            </div>
            <div
              v-if="currentImportAttachment"
              class="cut-round-import-attachment"
            >
              <span class="cut-round-import-attachment__label">导入附件</span>
              <Button
                size="small"
                type="link"
                @click="downloadImportAttachment(currentImportAttachment)"
              >
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>
                导入时间：{{
                  formatDateTime(currentImportAttachment.uploadTime)
                }}
              </span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="cut-round-detail-panel">
            <div class="cut-round-detail-panel__header">明细项目</div>
            <div class="cut-round-table-wrap">
              <table class="cut-round-grid">
                <thead>
                  <tr>
                    <th
                      v-for="column in detailColumns"
                      :key="column.key"
                      :width="column.width"
                    >
                      {{ column.title }}
                    </th>
                  </tr>
                </thead>
                <tbody v-if="isProcessCheckForm">
                  <tr
                    v-for="(item, index) in detailRows"
                    :key="item.id || index"
                  >
                    <td
                      v-if="getFieldRowSpan(index, 'itemCategory') > 0"
                      :rowspan="getFieldRowSpan(index, 'itemCategory')"
                    >
                      {{ formatText(item.itemCategory) }}
                    </td>
                    <td
                      v-if="getFieldRowSpan(index, 'stepNode') > 0"
                      :rowspan="getFieldRowSpan(index, 'stepNode')"
                    >
                      {{ formatText(item.stepNode) }}
                    </td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="detailRows.length === 0">
                    <td
                      :colspan="detailColumnCount"
                      class="cut-round-empty-cell"
                      align="center"
                    >
                      暂无明细数据
                    </td>
                  </tr>
                </tbody>
                <tbody v-else-if="isCleaningForm">
                  <tr
                    v-for="(item, index) in detailRows"
                    :key="item.id || index"
                  >
                    <td
                      v-if="getFieldRowSpan(index, 'itemCategory') > 0"
                      :rowspan="getFieldRowSpan(index, 'itemCategory')"
                    >
                      {{ formatText(item.itemCategory) }}
                    </td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="detailRows.length === 0">
                    <td
                      :colspan="detailColumnCount"
                      class="cut-round-empty-cell"
                      align="center"
                    >
                      暂无明细数据
                    </td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr
                    v-for="(item, index) in detailRows"
                    :key="item.id || index"
                  >
                    <td align="center">{{ item.itemSeq || index + 1 }}</td>
                    <td>{{ formatText(item.itemName) }}</td>
                    <td>{{ formatText(item.standardText) }}</td>
                    <td>{{ formatText(item.actualValue) }}</td>
                    <td>{{ formatText(item.abnormalRemark) }}</td>
                  </tr>
                  <tr v-if="detailRows.length === 0">
                    <td
                      :colspan="detailColumnCount"
                      class="cut-round-empty-cell"
                      align="center"
                    >
                      暂无明细数据
                    </td>
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
.cut-round-station-record-page,
.cut-round-record-layout,
.cut-round-detail-modal,
.cut-round-detail-body,
.cut-round-detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cut-round-record-layout {
  gap: 8px;
  height: 100%;
}

.cut-round-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.cut-round-record-link:hover {
  text-decoration: underline;
}

.cut-round-detail-modal {
  height: 100vh;
  background: #f5f7fa;
}

.cut-round-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.cut-round-detail-toolbar__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: baseline;
  min-width: 0;
}

.cut-round-detail-toolbar__main {
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.cut-round-detail-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 13px;
}

.cut-round-detail-toolbar__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.cut-round-detail-body {
  flex: 1;
  gap: 10px;
  padding: 10px;
  overflow: hidden;
}

.cut-round-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.cut-round-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 800;
}

.cut-round-head-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid #d8e0ea;
  border-left: 1px solid #d8e0ea;
}

.cut-round-head-item {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #d8e0ea;
  border-bottom: 1px solid #d8e0ea;
}

.cut-round-head-item__label,
.cut-round-head-item__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.cut-round-head-item__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: #eef2f7;
  border-right: 1px solid #d8e0ea;
}

.cut-round-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.cut-round-import-attachment__label {
  color: #1677ff;
  font-weight: 800;
}

.cut-round-detail-panel {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.cut-round-detail-panel__header {
  flex-shrink: 0;
  min-height: 34px;
  padding: 7px 10px;
  color: #1677ff;
  font-weight: 800;
  background: #eef2f7;
  border-bottom: 1px solid #d8e0ea;
}

.cut-round-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.cut-round-grid {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: fixed;
}

.cut-round-grid th,
.cut-round-grid td {
  min-height: 30px;
  padding: 4px 6px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #d8e0ea;
}

.cut-round-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-weight: 800;
  text-align: center;
  background: #eef2f7;
}

.cut-round-empty-cell {
  color: #94a3b8;
}

:global(.cut-round-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.cut-round-station-record-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}

:global(.cut-round-station-record-modal .ant-modal-body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}
</style>
