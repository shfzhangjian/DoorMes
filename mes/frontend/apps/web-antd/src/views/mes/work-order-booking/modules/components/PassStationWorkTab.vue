<script lang="ts" setup>
import { multiFields, fieldLabel, readFormulaValue, writeFormulaValue, multiFieldError } from '#/views/mes/hc/stationform/modules/formula-multi-fields';
import type { MesHcFormulaReportApi } from '#/api/mes/hc/execution/formula-report';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';

import dayjs from 'dayjs';
import { computed, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Button, Form, FormItem, Input, Modal as AModal, Radio, RadioGroup, Tag, message } from 'ant-design-vue';

import { uploadFile } from '#/api/infra/file';
import {
  exportProcessFormRecordLayout,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import {
  confirmFormulaPassWork,
  getFormulaPassWorkList,
  saveFormulaPassWork,
} from '#/api/mes/hc/execution/formula-report';

import AuthModal from './AuthModal.vue';

const props = defineProps<{ task: any; startForm: any }>();

const CHECK_RESULT_REMARK_TIP = '开机、清洁保养时遇到问题请记录在备注列说明情况。';

type WorkAction = 'confirm' | 'edit' | 'submit' | 'view';
type ExcelAttachment = MesHcFormulaReportApi.ExcelAttachment;
type WorkItem = {
  needConfirm?: boolean;
  complete?: boolean;
  completionMessage?: string;
  formulaCategory?: string;
  matchReason?: string;
  frozen?: boolean;
  recordId?: number;
  formId?: number;
  formCode?: string;
  id: string;
  name: string;
  displayName?: string;
  timing: string;
  status: string;
  result: string;
  inspectionResult?: string;
  recorder?: string;
  recorderTime?: string;
  confirmer?: string;
  confirmerTime?: string;
  formRemark?: string;
  confirmRemark?: string;
  importAttachment?: ExcelAttachment;
  details?: any[];
};

const loading = ref(false);
const rows = ref<WorkItem[]>([]);
const modalMode = ref<WorkAction>('view');
const currentRecord = ref<WorkItem | null>(null);
const excelImportInputRef = ref<HTMLInputElement>();
const excelLoading = ref(false);
const confirmAuthVisible = ref(false);
const actionPanelExpanded = ref(false);
const actionForm = ref({
  result: 'OK',
  inspectionResult: 'OK',
  formRemark: '',
  recorder: '',
  recorderTime: '',
  confirmer: '',
  confirmerTime: '',
  confirmRemark: '',
});

function normalizeDetailItems(details: any[] = []) {
  return details.map((item) => ({
    ...item,
    status: item.status || (item.requiredFlag ? '' : 'OK'),
    actualValue: item.actualValue || '',
    actualValue2: item.actualValue2 || '',
    remark: item.remark || '',
  }));
}

const detailRows = computed(() => currentRecord.value?.details ?? []);
const readOnly = computed(() => modalMode.value === 'view');
const currentImportAttachment = computed(() => normalizeImportAttachment(currentRecord.value?.importAttachment));
const currentTaskModelText = computed(() =>
  displayText(props.task?.motherModelCode || props.task?.modelCode || props.task?.motherModelName || props.task?.modelName),
);
const currentTaskBatchText = computed(() =>
  displayText(props.task?.productionBatchNo || props.task?.batchNo || props.task?.parentProductionBatchNo || props.task?.batchingNo || props.startForm?.batchingNo),
);
const isStartupForm = computed(() => currentRecord.value?.formulaCategory === 'startup' || getWorkDisplayName(currentRecord.value).includes('开机点检表'));
const isCleaningForm = computed(() => {
  const name = getWorkDisplayName(currentRecord.value);
  return currentRecord.value?.formulaCategory === 'cleaning' || name.includes('清洁保养表') || name.includes('清洁点检表');
});
const isProductionCheckForm = computed(() => currentRecord.value?.formulaCategory === 'production' || getWorkDisplayName(currentRecord.value).includes('生产点检表'));
const productionDetailRows = computed(() =>
  detailRows.value.flatMap((record, index) => {
    if (record.valueMode === 'MULTI_FIELDS') {
      return multiFields(record).map((field) => ({
        source: record, id: record.id, category: record.category, node: record.node,
        item: record.item, standard: record.standard, sourceIndex: index,
        sourceRowId: record.id || `row-${index}`, actualField: `multi:${field.key}`,
        recordLabel: fieldLabel(field),
      }));
    }
    if (!isDualValueMode(record)) {
      return [
        {
          source: record,
          id: record.id,
          category: record.category,
          node: record.node,
          item: record.item,
          standard: record.standard,
          sourceIndex: index,
          sourceRowId: record.id || `row-${index}`,
          actualField: 'actualValue',
          recordLabel: '',
        },
      ];
    }
    return [
      {
        source: record,
        id: record.id,
        category: record.category,
        node: record.node,
        item: record.item,
        standard: record.standard,
        sourceIndex: index,
        sourceRowId: record.id || `row-${index}`,
        actualField: 'actualValue',
        recordLabel: record.dualLabel1 || '重量',
      },
      {
        source: record,
        id: record.id,
        category: record.category,
        node: record.node,
        item: record.item,
        standard: record.standard,
        sourceIndex: index,
        sourceRowId: record.id || `row-${index}`,
        actualField: 'actualValue2',
        recordLabel: record.dualLabel2 || '批号',
      },
    ];
  }),
);
const dialogTitle = computed(() =>
  `${modalMode.value === 'edit' ? '填写' : modalMode.value === 'confirm' ? '确认' : modalMode.value === 'submit' ? '提交' : '查看'}${getWorkDisplayName(currentRecord.value) || '记录单'}`,
);
const passWorkAuthActionName = computed(() =>
  `${modalMode.value === 'confirm' ? '确认' : modalMode.value === 'submit' ? '提交完成' : '保存草稿'}${getWorkDisplayName(currentRecord.value) || '过站记录单'}`,
);
const passWorkAuthTitle = computed(() =>
  modalMode.value === 'confirm' ? '配料过站工作确认认证' : '配料过站工作保存认证',
);
const passWorkHeaderFields = computed(() => [
  {
    field: 'mixerEquipmentCode',
    label: '搅拌机台号',
    value: props.task?.mixerEquipmentCode || props.task?.equipmentCode || '-',
  },
  {
    field: 'foamingEquipmentCode',
    label: '脱泡机台号',
    value: props.task?.foamingEquipmentCode || '-',
  },
  {
    field: 'recorder',
    label: '记录人',
    value: actionForm.value.recorder || currentRecord.value?.recorder || '-',
  },
  {
    field: 'confirmer',
    label: '确认人',
    value: actionForm.value.confirmer || currentRecord.value?.confirmer || '-',
  },
  {
    field: 'recorderTime',
    label: '记录时间',
    value: actionForm.value.recorderTime || currentRecord.value?.recorderTime || '-',
  },
]);

function getWorkDisplayName(record?: Pick<WorkItem, 'displayName' | 'name'> | null) {
  return String(record?.displayName || record?.name || '');
}

function getStatusMeta(status?: string) {
  const map: Record<string, { color: string; text: string }> = {
    PENDING_CHECK: { color: 'default', text: '待执行' },
    DRAFT: { color: 'default', text: '草稿' },
    RECORDED: { color: 'processing', text: '已填写' },
    CONFIRMED: { color: 'success', text: '已确认' },
  };
  return map[status || ''] || { color: 'default', text: status || '-' };
}

function getItemRowSpan(index: number) {
  const rowsValue = detailRows.value;
  const current = rowsValue[index];
  if (!current) return 1;
  if (index > 0 && rowsValue[index - 1]?.item === current.item) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rowsValue.length; cursor += 1) {
    if (rowsValue[cursor]?.item === current.item) {
      span += 1;
    } else {
      break;
    }
  }
  return span;
}

function getFieldRowSpan(index: number, field: 'category' | 'node') {
  const rowsValue = detailRows.value;
  const current = rowsValue[index];
  if (!current) return 1;
  if (index > 0 && rowsValue[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rowsValue.length; cursor += 1) {
    if (rowsValue[cursor]?.[field] === current[field]) {
      span += 1;
    } else {
      break;
    }
  }
  return span;
}

function getProductionFieldRowSpan(index: number, field: 'category' | 'node' | 'item') {
  const rowsValue = productionDetailRows.value;
  const current = rowsValue[index];
  if (!current) return 1;
  if (index > 0 && rowsValue[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rowsValue.length; cursor += 1) {
    if (rowsValue[cursor]?.[field] === current[field]) {
      span += 1;
    } else {
      break;
    }
  }
  return span;
}

function getProductionSourceRowSpan(index: number) {
  const rowsValue = productionDetailRows.value;
  const current = rowsValue[index];
  if (!current) return 1;
  if (index > 0 && rowsValue[index - 1]?.sourceRowId === current.sourceRowId) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rowsValue.length; cursor += 1) {
    if (rowsValue[cursor]?.sourceRowId === current.sourceRowId) {
      span += 1;
    } else {
      break;
    }
  }
  return span;
}

async function loadRows() {
  if (!props.task?.planId || !props.task?.planOperationId) {
    rows.value = [];
    return;
  }
  loading.value = true;
  rows.value = [];
  try {
    const data = await getFormulaPassWorkList(props.task.planId, props.task.planOperationId);
    rows.value = (data || [])
      .map((row) => ({
        needConfirm: row.needConfirm !== false,
        complete: row.complete,
        completionMessage: row.completionMessage,
        formulaCategory: row.formulaCategory,
        matchReason: row.matchReason,
        frozen: row.frozen,
        recordId: row.recordId,
        formId: row.formId,
        formCode: row.formCode,
        id: row.id,
        name: row.name,
        displayName: row.displayName,
        timing: row.timing || '-',
        status: row.status || 'PENDING_CHECK',
        result: row.result || '未执行',
        inspectionResult: row.inspectionResult || row.result || 'OK',
        recorder: row.recorder || '',
        recorderTime: row.recorderTime || '',
        confirmer: row.confirmer || '',
        confirmerTime: row.confirmerTime || '',
        formRemark: row.formRemark || '',
        confirmRemark: row.confirmRemark || '',
        importAttachment: normalizeImportAttachment(row.importAttachment),
        details: normalizeDetailItems(row.details || []),
      }));
  } finally {
    loading.value = false;
  }
}

function openAction(record: WorkItem, action: WorkAction) {
  if (!props.task?.startTime && props.task?.status !== 'IN_PROGRESS' && props.task?.status !== 'COMPLETED') {
    AModal.warning({
      title: '请先执行开工确认',
      content: '当前配料工位尚未开工，必须先完成开工确认后才能填写或确认过站工作。',
    });
    return;
  }
  if (props.task?.status === 'COMPLETED' && action !== 'view') {
    AModal.warning({
      title: '当前工位已报工关闭',
      content: '过站工作已关闭，只允许查看，不能继续填写或确认。',
    });
    return;
  }
  if (record.status === 'CONFIRMED' && action !== 'view') {
    AModal.warning({
      title: '当前过站工作已关闭',
      content: '该过站工作已确认关闭，只允许查看。',
    });
    return;
  }
  currentRecord.value = JSON.parse(JSON.stringify(record));
  modalMode.value = action;
  actionPanelExpanded.value = false;
  actionForm.value = {
    result: record.result === '未执行' || record.result === '待录入' ? 'OK' : (record.result || 'OK'),
    inspectionResult: record.inspectionResult || record.result || 'OK',
    formRemark: record.formRemark || '',
    recorder: record.recorder || '',
    recorderTime: record.recorderTime || '',
    confirmer: record.confirmer || '',
    confirmerTime: record.confirmerTime || '',
    confirmRemark: record.confirmRemark || '',
  };
  modalApi.open();
}

function buildNowText() {
  const now = new Date();
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function isTimeOnlyText(value?: string) {
  if (!value) return false;
  return /^\d{2}:\d{2}(:\d{2})?$/.test(String(value).trim());
}

function isPlaceholderDateTimeText(value?: string) {
  if (!value) return false;
  const date = dayjs(value);
  return date.isValid() && (date.year() === 1970 || date.year() === 1900);
}

function shouldResetRuntimeDateTime(value?: string) {
  return !value || isTimeOnlyText(value) || isPlaceholderDateTimeText(value);
}

function isDualValueMode(record: any) {
  return String(record?.valueMode || '').toUpperCase() === 'DUAL_TEXT' || (!record?.explicitValueMode && String(record?.item || '').includes('加入'));
}

function isTimeValueMode(record: any) {
  return String(record?.valueMode || '').toUpperCase() === 'TIME';
}

function getProductionInputPlaceholder(record: any, actualField: string) {
  if (actualField.startsWith('multi:')) return multiFields(record).find((field) => `multi:${field.key}` === actualField)?.label || '';
  if (isDualValueMode(record)) {
    if (actualField === 'actualValue') return record.dualLabel1 || '重量';
    if (actualField === 'actualValue2') return record.dualLabel2 || '批号';
  }
  if (isTimeValueMode(record)) {
    return 'HH:mm';
  }
  return record?.standard || '';
}

function isProductionStepStart(index: number) {
  const rowsValue = productionDetailRows.value;
  const current = rowsValue[index];
  if (!current) return false;
  return index === 0 || rowsValue[index - 1]?.node !== current.node;
}

function getDetailSeq(record: any, index: number) {
  return record?.seq || record?.itemSeq || record?.id || index + 1;
}

function inputText(value?: null | number | string) {
  return String(value ?? '').trim();
}

function displayText(value?: null | number | string, fallback = '-') {
  const text = inputText(value);
  return text || fallback;
}

function normalizeDetailResult(value?: null | number | string) {
  const text = inputText(value).toUpperCase();
  if (['NG', 'NOK', 'NO', 'FAIL', 'FAILED', '异常', '不合格'].includes(text)) return 'NG';
  if (['OK', 'PASS', 'PASSED', 'YES', '正常', '合格'].includes(text)) return 'OK';
  return text || 'OK';
}

function detailResult(row: any) {
  if (row?.requiredFlag && !inputText(row?.status)) return '';
  return normalizeDetailResult(row?.status || row?.resultFlag || row?.result || row?.checkResult);
}

function updateDetailResult(row: any, value: string) {
  const result = row.requiredFlag && !inputText(value) ? '' : normalizeDetailResult(value);
  row.status = result;
  row.resultFlag = result;
  row.result = result;
  row.checkResult = result;
}

function normalizeImportAttachment(value?: ExcelAttachment | ExcelAttachment[] | null): ExcelAttachment | undefined {
  const raw = Array.isArray(value) ? value[value.length - 1] : value;
  if (!raw || typeof raw !== 'object') return undefined;
  const name = inputText(raw.name);
  const path = inputText(raw.path);
  const size = Number(raw.size);
  const type = inputText(raw.type);
  const uploadTime = inputText(raw.uploadTime);
  const url = inputText(raw.url);
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

async function uploadImportAttachment(file: File): Promise<ExcelAttachment> {
  const uploaded = (await uploadFile({
    directory: 'mes/formula-pass-work',
    file,
  })) as any;
  if (typeof uploaded === 'string') {
    return {
      name: file.name,
      size: file.size,
      type: file.type,
      uploadTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      url: uploaded,
    };
  }
  return {
    name: uploaded?.name || file.name,
    path: uploaded?.path,
    size: uploaded?.size || file.size,
    type: uploaded?.type || file.type,
    uploadTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    url: uploaded?.url,
  };
}

function formatImportAttachmentTime(value?: string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value;
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: ExcelAttachment) {
  const url = inputText(attachment?.url || attachment?.path);
  if (!url) {
    message.warning('未找到附件下载地址');
    return;
  }
  window.open(url, '_blank');
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}

function itemBindKey(index: number) {
  return String(index);
}

function excelCell(
  colIndex: number,
  text?: null | number | string,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    colIndex,
    colSpan: options.colSpan ?? 1,
    editable: options.editable ?? false,
    rowSpan: options.rowSpan ?? 1,
    text: String(text ?? ''),
    ...(options.bindField ? { bindField: options.bindField } : {}),
    ...(options.bindKey ? { bindKey: options.bindKey } : {}),
  };
}

function buildPassWorkExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  return [
    { editable: false, label: '计划号', value: props.task?.planNo || '' },
    { editable: false, label: '当前工序', value: props.task?.process || props.startForm?.process || '配料' },
    { editable: false, label: '执行时机', value: currentRecord.value?.timing || '' },
    ...passWorkHeaderFields.value.map((field) => ({
      editable: false,
      label: field.label,
      value: field.value || '',
    })),
  ];
}

function buildPassWorkExcelColumns(): MesHcProcessFormApi.LayoutColumn[] {
  if (isStartupForm.value) {
    return [
      { title: '序号', width: 90 },
      { title: '点检项目', width: 260 },
      { title: '标准', width: 360 },
      { title: 'OK/NG', width: 120 },
      { title: '备注', width: 260 },
    ];
  }
  if (isCleaningForm.value) {
    return [
      { title: '工序', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '检查标准', width: 420 },
      { title: 'OK/NG', width: 120 },
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

function buildStartupPassWorkExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  return detailRows.value.map((row, index) => {
    const bindKey = itemBindKey(index);
    return {
      cells: [
        excelCell(0, getDetailSeq(row, index), { editable: false }),
        excelCell(1, displayText(row.item), { editable: false }),
        excelCell(2, displayText(row.standard), { editable: false }),
        excelCell(3, detailResult(row), { bindField: 'status', bindKey, editable: !readOnly.value }),
        excelCell(4, row.remark || '', { bindField: 'remark', bindKey, editable: !readOnly.value }),
      ],
    };
  });
}

function buildCleaningPassWorkExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  return detailRows.value.map((row, index) => {
    const bindKey = itemBindKey(index);
    const cells: MesHcProcessFormApi.LayoutCell[] = [];
    const categorySpan = getFieldRowSpan(index, 'category');
    if (categorySpan > 0) {
      cells.push(excelCell(0, displayText(row.category), { editable: false, rowSpan: categorySpan }));
    }
    cells.push(excelCell(1, displayText(row.item), { editable: false }));
    cells.push(excelCell(2, displayText(row.standard), { editable: false }));
    cells.push(excelCell(3, detailResult(row), { bindField: 'status', bindKey, editable: !readOnly.value }));
    cells.push(excelCell(4, row.remark || '', { bindField: 'remark', bindKey, editable: !readOnly.value }));
    return { cells };
  });
}

function buildProductionPassWorkExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  return productionDetailRows.value.map((row, index) => {
    const cells: MesHcProcessFormApi.LayoutCell[] = [];
    const sourceSpan = getProductionSourceRowSpan(index);
    const categorySpan = getProductionFieldRowSpan(index, 'category');
    const nodeSpan = getProductionFieldRowSpan(index, 'node');
    const itemSpan = getProductionFieldRowSpan(index, 'item');
    if (sourceSpan > 0) {
      cells.push(excelCell(0, displayText(row.id || row.sourceIndex + 1), { editable: false, rowSpan: sourceSpan }));
    }
    if (categorySpan > 0) {
      cells.push(excelCell(1, displayText(row.category), { editable: false, rowSpan: categorySpan }));
    }
    if (nodeSpan > 0) {
      cells.push(excelCell(2, displayText(row.node), { editable: false, rowSpan: nodeSpan }));
    }
    if (itemSpan > 0) {
      cells.push(excelCell(3, displayText(row.item), { editable: false, rowSpan: itemSpan }));
    }
    if (sourceSpan > 0) {
      cells.push(excelCell(4, displayText(row.standard), { editable: false, rowSpan: sourceSpan }));
    }
    cells.push(excelCell(5, displayText(row.recordLabel, ''), { editable: false }));
    cells.push(
      excelCell(6, readFormulaValue(row.source, row.actualField), {
        bindField: row.actualField,
        bindKey: itemBindKey(row.sourceIndex),
        editable: !readOnly.value,
      }),
    );
    if (sourceSpan > 0) {
      cells.push(
        excelCell(7, row.source.remark || '', {
          bindField: 'remark',
          bindKey: itemBindKey(row.sourceIndex),
          editable: !readOnly.value,
          rowSpan: sourceSpan,
        }),
      );
    }
    return { cells };
  });
}

function buildPassWorkExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isStartupForm.value) return buildStartupPassWorkExcelRows();
  if (isCleaningForm.value) return buildCleaningPassWorkExcelRows();
  return buildProductionPassWorkExcelRows();
}

function sanitizePassWorkExcelFileName(name?: string) {
  return `${displayText(name, '配料过站工作')}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_');
}

function getPassWorkExcelVisualMode() {
  if (isStartupForm.value) return 'formula-startup-check';
  if (isCleaningForm.value) return 'formula-cleaning-check';
  if (isProductionCheckForm.value) return 'formula-production-check';
  return 'formula-pass-work';
}

function buildPassWorkExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = getWorkDisplayName(currentRecord.value) || '配料过站工作';
  const planNo = inputText(props.task?.planNo);
  const fileTitle = planNo ? `${planNo}_${formName}` : formName;
  return {
    columns: buildPassWorkExcelColumns(),
    detailTitle: '明细项目',
    fileName: sanitizePassWorkExcelFileName(fileTitle),
    headerItems: buildPassWorkExcelHeaderItems(),
    rows: buildPassWorkExcelRows(),
    sheetName: '过站工作',
    title: formName,
    visualMode: getPassWorkExcelVisualMode(),
  };
}

function normalizePassWorkResult(value?: string) {
  const text = inputText(value).toUpperCase();
  if (!text) return '';
  if (text.includes('NG') || text.includes('不合格')) return 'NG';
  if (text.includes('NA') || text.includes('N/A') || text.includes('不适用')) return 'NA';
  if (text.includes('OK') || text.includes('合格')) return 'OK';
  return text;
}

function stripImportedPassWorkLabel(record: any, field: string, value?: string) {
  let text = inputText(value);
  const labels =
    field === 'actualValue'
      ? [record?.dualLabel1, '重量']
      : field === 'actualValue2'
        ? [record?.dualLabel2, '批号']
        : [];
  for (const labelValue of labels) {
    const label = inputText(labelValue);
    if (!label) continue;
    const pattern = new RegExp(`^${escapeRegExp(label)}\\s*[:：]?\\s*`, 'u');
    const stripped = text.replace(pattern, '').trim();
    if (stripped !== text || text === label) {
      text = stripped;
      break;
    }
  }
  return text;
}

function applyImportedPassWorkExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  const details = currentRecord.value?.details || [];
  (resp.cellValues || []).forEach((item) => {
    if (!item.bindField) return;
    const rowIndex = Number(item.bindKey);
    if (!Number.isInteger(rowIndex)) return;
    const row = details[rowIndex];
    if (!row) return;
    if (item.bindField.startsWith('multi:')) {
      writeFormulaValue(row, item.bindField, item.value);
      return;
    }
    if (item.bindField === 'status') {
      if (isStartupForm.value || isCleaningForm.value) {
        updateDetailResult(row, String(item.value ?? ''));
      }
      return;
    }
    if (item.bindField === 'actualValue' || item.bindField === 'actualValue2' || item.bindField === 'remark') {
      row[item.bindField] = stripImportedPassWorkLabel(row, item.bindField, item.value);
    }
  });
}

async function handlePassWorkExportExcel() {
  if (!currentRecord.value) return;
  excelLoading.value = true;
  try {
    const layout = buildPassWorkExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({ fileName: layout.fileName || '配料过站工作.xlsx', source: data });
  } finally {
    excelLoading.value = false;
  }
}

function triggerPassWorkImportExcel() {
  if (readOnly.value) {
    message.warning('查看模式下不能导入Excel');
    return;
  }
  excelImportInputRef.value?.click();
}

async function handlePassWorkExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xlsx?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  if (readOnly.value) {
    message.warning('查看模式下不能导入Excel');
    return;
  }
  excelLoading.value = true;
  try {
    const resp = await importProcessFormRecordLayout(file, buildPassWorkExcelLayout());
    const attachment = await uploadImportAttachment(file);
    applyImportedPassWorkExcel(resp);
    if (currentRecord.value) {
      currentRecord.value.importAttachment = attachment;
    }
    message.success(`导入成功，已回填 ${resp.totalCellCount || 0} 个单元格并记录原始附件，请检查后保存或确认。`);
  } finally {
    excelLoading.value = false;
  }
}

async function submitAction() {
  if (!currentRecord.value) return;
  const target = rows.value.find((item) => item.id === currentRecord.value?.id);
  if (!target) return;
  const now = buildNowText();

  if (modalMode.value === 'edit' || modalMode.value === 'submit') {
    if (shouldResetRuntimeDateTime(actionForm.value.recorderTime)) {
      actionForm.value.recorderTime = now;
    }
    await saveFormulaPassWork({
      submit: modalMode.value === 'submit',
      recordId: currentRecord.value.recordId,
      planId: props.task.planId,
      planOperationId: props.task.planOperationId,
      formCode: currentRecord.value.formCode,
      result: actionForm.value.result,
      inspectionResult: actionForm.value.inspectionResult,
      formRemark: actionForm.value.formRemark,
      recorder: actionForm.value.recorder || undefined,
      recorderTime: actionForm.value.recorderTime || undefined,
      confirmer: actionForm.value.confirmer,
      confirmerTime: actionForm.value.confirmerTime || undefined,
      confirmRemark: actionForm.value.confirmRemark,
      mixerEquipmentId: props.task?.mixerEquipmentId,
      mixerEquipmentCode: props.task?.mixerEquipmentCode,
      mixerEquipmentName: props.task?.mixerEquipmentName || props.task?.equipmentName,
      foamingEquipmentId: props.task?.foamingEquipmentId,
      foamingEquipmentCode: props.task?.foamingEquipmentCode,
      foamingEquipmentName: props.task?.foamingEquipmentName,
      importAttachment: currentRecord.value.importAttachment,
      details: currentRecord.value.details || [],
    });
    message.success(modalMode.value === 'submit' ? '过站工作已提交完成。' : '草稿已保存，可继续填写。');
  }

  if (modalMode.value === 'confirm') {
    if (shouldResetRuntimeDateTime(actionForm.value.recorderTime)) {
      actionForm.value.recorderTime =
        !shouldResetRuntimeDateTime(target.recorderTime) ? (target.recorderTime ?? now) : now;
    }
    if (shouldResetRuntimeDateTime(actionForm.value.confirmerTime)) {
      actionForm.value.confirmerTime = now;
    }
    await confirmFormulaPassWork({
      recordId: currentRecord.value.recordId,
      planId: props.task.planId,
      planOperationId: props.task.planOperationId,
      formCode: currentRecord.value.formCode,
      result: actionForm.value.result,
      inspectionResult: actionForm.value.inspectionResult,
      formRemark: actionForm.value.formRemark,
      recorder: actionForm.value.recorder || target.recorder || undefined,
      recorderTime: actionForm.value.recorderTime || target.recorderTime || undefined,
      confirmer: actionForm.value.confirmer || undefined,
      confirmerTime: actionForm.value.confirmerTime || undefined,
      confirmRemark: actionForm.value.confirmRemark,
      mixerEquipmentId: props.task?.mixerEquipmentId,
      mixerEquipmentCode: props.task?.mixerEquipmentCode,
      mixerEquipmentName: props.task?.mixerEquipmentName || props.task?.equipmentName,
      foamingEquipmentId: props.task?.foamingEquipmentId,
      foamingEquipmentCode: props.task?.foamingEquipmentCode,
      foamingEquipmentName: props.task?.foamingEquipmentName,
      importAttachment: currentRecord.value.importAttachment,
      details: currentRecord.value.details || [],
    });
    message.success('过站工作已确认。');
  }

  await loadRows();
  modalApi.close();
}

function handleSubmitActionClick() {
  const complete = modalMode.value === 'confirm' || modalMode.value === 'submit';
  for (const item of detailRows.value) {
    const error = multiFieldError(item, complete);
    if (error) { message.warning(`${item.itemSeq}.${item.item}：${error}`); return; }
  }
  if (modalMode.value === 'confirm' || modalMode.value === 'submit') {
    const missing = detailRows.value.filter((item) => item.valueMode !== 'MULTI_FIELDS' && item.requiredFlag && (
      item.valueMode === 'OK_NG' ? !['OK', 'NG'].includes(item.status) :
      isDualValueMode(item) ? !inputText(item.actualValue) || !inputText(item.actualValue2) : !inputText(item.actualValue)
    ));
    if (missing.length) {
      message.warning(`请填写必填项目：${missing.map((item) => `${item.itemSeq}.${item.item}`).join('、')}`);
      return;
    }
    if (detailRows.value.some((item) => item.status === 'NG') || actionForm.value.result === 'NG' || actionForm.value.inspectionResult === 'NG') {
      message.warning('存在 NG，请先保存并处理异常后再完成表单');
      return;
    }
  }
  if (modalMode.value === 'edit' || modalMode.value === 'confirm' || modalMode.value === 'submit') {
    confirmAuthVisible.value = true;
    return;
  }
  void submitAction();
}

async function handleConfirmAuthSuccess(userInfo: any) {
  const operatorName = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || '';
  if (modalMode.value === 'edit' || modalMode.value === 'submit') {
    if (operatorName) {
      actionForm.value.recorder = operatorName;
    }
    actionForm.value.recorderTime = buildNowText();
  }
  if (modalMode.value === 'confirm') {
    if (operatorName) {
      actionForm.value.confirmer = operatorName;
    }
    actionForm.value.confirmerTime = buildNowText();
  }
  confirmAuthVisible.value = false;
  await submitAction();
}

function closeDialog() {
  modalApi.close();
}

function validateBeforeFinish() {
  if (!rows.value.length) {
    return { valid: false, message: '当前未生成任何过站工作，不能执行过站报工。' };
  }
  const unconfirmedRows = rows.value.filter((item) => item.complete !== true);
  if (unconfirmedRows.length === 0) {
    return { valid: true, message: '' };
  }
  return {
    valid: false,
    message: `以下过站工作尚未完成：${unconfirmedRows.map((item) => `${getWorkDisplayName(item)}（${item.completionMessage || '待完成'}）`).join('、')}`,
  };
}

defineExpose({
  validateBeforeFinish,
});

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  footer: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'hc-pass-work-modal',
  onClosed() {
    currentRecord.value = null;
  },
});

watch(
  () => props.startForm.process,
  () => {
    loadRows();
  },
);

onMounted(() => {
  loadRows();
});
</script>

<template>
  <div class="pass-work-panel">
    <div class="pp-panel">
      <div class="pp-panel__header">
        <div class="pp-panel__title">
          <IconifyIcon icon="lucide:clipboard-check" class="mr-2 text-[#1677ff]" />
          过站工作
        </div>
        <div class="pp-panel__desc">配料设备开机点检表、配料设备清洁保养表、配料生产点检表合并展示</div>
      </div>

      <div class="pp-table-wrap pass-work-list-wrap">
        <table class="pp-grid">
          <thead>
            <tr>
              <th width="260">表单名称</th>
              <th width="130">执行时机</th>
              <th width="120">单据状态</th>
              <th width="180">记录人/时间</th>
              <th width="180">确认人/确认时间</th>
              <th width="180">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in rows" :key="record.id">
              <td>{{ getWorkDisplayName(record) || '-' }}</td>
              <td align="center">{{ record.timing || '-' }}</td>
              <td align="center">
                <Tag :color="getStatusMeta(record.status).color" class="!m-0">{{ record.complete ? '已完成' : getStatusMeta(record.status).text }}</Tag>
              </td>
              <td>
                <div>{{ record.recorder || '-' }}</div>
                <div class="pp-grid__sub">{{ record.recorderTime || '-' }}</div>
              </td>
              <td>
                <div>{{ record.confirmer || '-' }}</div>
                <div class="pp-grid__sub">{{ record.confirmerTime || '-' }}</div>
              </td>
              <td align="center">
                <div class="pass-work-actions">
                  <Button
                    size="small"
                    type="link"
                    :disabled="record.status === 'CONFIRMED' || task?.status === 'COMPLETED'"
                    @click="openAction(record, 'edit')"
                  >
                    填写
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    :disabled="record.status === 'CONFIRMED' || task?.status === 'COMPLETED'"
                    @click="openAction(record, record.needConfirm === false ? 'submit' : 'confirm')"
                  >
                    {{ record.needConfirm === false ? '提交完成' : '确认' }}
                  </Button>
                  <Button size="small" type="link" @click="openAction(record, 'view')">查看</Button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6" class="pp-empty-cell" align="center">
                {{ loading ? '正在加载过站工作...' : '暂无过站工作数据' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <Modal>
      <div class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title pass-work-dialog-title">
            <span class="pp-plan-toolbar__main">{{ dialogTitle }}</span>
            <div class="toolbar-meta">
              <span>计划号：{{ task?.planNo || '-' }}</span>
              <span>型号：{{ currentTaskModelText }}</span>
              <span>批号：{{ currentTaskBatchText }}</span>
              <span>当前工序：{{ task?.process || startForm?.process || '配料' }}</span>
              <span>执行时机：{{ currentRecord?.timing || '-' }}</span>
              <span class="pass-work-plan-check-tip">请核实型号与批号</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <input
              ref="excelImportInputRef"
              accept=".xlsx,.xls"
              class="pass-work-excel-input"
              type="file"
              @change="handlePassWorkExcelImportChange"
            />
            <Button size="small" :loading="excelLoading" @click="handlePassWorkExportExcel">导出Excel</Button>
            <Button v-if="!readOnly" size="small" :loading="excelLoading" @click="triggerPassWorkImportExcel">导入Excel</Button>
            <Button v-if="!readOnly" size="small" type="primary" :danger="modalMode === 'confirm'" @click="handleSubmitActionClick">
              {{ modalMode === 'confirm' ? '确认' : modalMode === 'submit' ? '提交完成' : '保存草稿' }}
            </Button>
            <Button size="small" @click="closeDialog">关闭</Button>
          </div>
        </div>

        <div class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div class="pp-form-grid pass-work-form-grid">
              <div v-for="item in passWorkHeaderFields" :key="item.field" class="head-item">
                <span class="head-item__label">{{ item.label }}</span>
                <span class="head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachment" class="pass-work-import-attachment">
              <span class="pass-work-import-attachment__label">导入附件</span>
              <Button size="small" type="link" @click="downloadImportAttachment(currentImportAttachment)">
                {{ currentImportAttachment.name || '原始导入文件' }}
              </Button>
              <span>导入时间：{{ formatImportAttachmentTime(currentImportAttachment.uploadTime) }}</span>
              <span v-if="formatAttachmentSize(currentImportAttachment.size)">
                大小：{{ formatAttachmentSize(currentImportAttachment.size) }}
              </span>
            </div>
          </fieldset>

          <div class="pp-panel pass-work-detail-panel">
            <div class="pp-panel__header">
              <span>明细项目 · * 为必填，草稿可暂存空值</span>
              <span>{{ currentRecord?.frozen ? '已固定任务版本' : '历史记录或开工前预览' }} · {{ currentRecord?.matchReason }}</span>
            </div>
            <div class="pp-table-wrap pass-work-detail-wrap">
              <div class="pass-work-detail-table-body">
              <div v-if="isStartupForm || isCleaningForm" class="pass-work-check-tip">
                {{ CHECK_RESULT_REMARK_TIP }}
              </div>
              <table class="pp-grid">
                <thead v-if="isStartupForm">
                  <tr>
                    <th width="90">序号</th>
                    <th width="260">点检项目</th>
                    <th width="360">标准</th>
                    <th width="120">OK/NG</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <thead v-else-if="isCleaningForm">
                  <tr>
                    <th width="120">工序</th>
                    <th width="220">点检项目</th>
                    <th width="420">检查标准</th>
                    <th width="120">OK/NG</th>
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
                  <tr v-for="(record, index) in detailRows" :key="record.id || index">
                    <td align="center">{{ getDetailSeq(record, index) }}</td>
                    <td><span v-if="record.requiredFlag" style="color:#cf1322">* </span>{{ record.item || '-' }}</td>
                    <td>{{ record.standard || '-' }}</td>
                    <td>
                      <RadioGroup
                        v-if="!readOnly"
                        :value="detailResult(record)"
                        class="pp-radio-group"
                        size="small"
                        @update:value="(value) => updateDetailResult(record, value)"
                      >
                        <Radio value="OK">OK</Radio>
                        <Radio value="NG">NG</Radio>
                      </RadioGroup>
                      <span v-else>{{ detailResult(record) || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="!readOnly" v-model:value="record.remark" size="small" />
                      <span v-else>{{ record.remark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="5" class="pp-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else-if="isCleaningForm">
                  <tr v-for="(record, index) in detailRows" :key="record.id || index">
                    <td v-if="getFieldRowSpan(index, 'category') > 0" :rowspan="getFieldRowSpan(index, 'category')">
                      {{ record.category || '-' }}
                    </td>
                    <td><span v-if="record.requiredFlag" style="color:#cf1322">* </span>{{ record.item || '-' }}</td>
                    <td>{{ record.standard || '-' }}</td>
                    <td>
                      <RadioGroup
                        v-if="!readOnly"
                        :value="detailResult(record)"
                        class="pp-radio-group"
                        size="small"
                        @update:value="(value) => updateDetailResult(record, value)"
                      >
                        <Radio value="OK">OK</Radio>
                        <Radio value="NG">NG</Radio>
                      </RadioGroup>
                      <span v-else>{{ detailResult(record) || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="!readOnly" v-model:value="record.remark" size="small" />
                      <span v-else>{{ record.remark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="5" class="pp-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr
                    v-for="(record, index) in productionDetailRows"
                    :key="`${record.sourceRowId}-${record.actualField}`"
                    :class="{ 'production-row--step-start': isProductionStepStart(index) }"
                  >
                    <td v-if="getProductionSourceRowSpan(index) > 0" align="center" :rowspan="getProductionSourceRowSpan(index)">
                      {{ record.id || record.sourceIndex + 1 }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'category') > 0" :rowspan="getProductionFieldRowSpan(index, 'category')">
                      {{ record.category || '-' }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'node') > 0" :rowspan="getProductionFieldRowSpan(index, 'node')">
                      {{ record.node || '-' }}
                    </td>
                    <td v-if="getProductionFieldRowSpan(index, 'item') > 0" :rowspan="getProductionFieldRowSpan(index, 'item')">
                      <span v-if="record.source.requiredFlag && record.source.valueMode !== 'MULTI_FIELDS'" style="color:#cf1322">* </span>{{ record.item || '-' }}
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">{{ record.standard || '-' }}</td>
                    <td class="production-record-label-cell">{{ record.recordLabel || '-' }}</td>
                    <td :class="['production-record-cell', { 'production-record-cell--time': isTimeValueMode(record.source) }]">
                      <Input
                        v-if="!readOnly"
                        :value="readFormulaValue(record.source, record.actualField)"
                        @update:value="(value) => writeFormulaValue(record.source, record.actualField, value)"
                        size="small"
                        :placeholder="getProductionInputPlaceholder(record.source, record.actualField)"
                      />
                      <span v-else>{{ readFormulaValue(record.source, record.actualField) || '-' }}</span>
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">
                      <Input v-if="!readOnly" v-model:value="record.source.remark" size="small" />
                      <span v-else>{{ record.source.remark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td colspan="8" class="pp-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
              </div>
            </div>
          </div>

          <fieldset class="pp-fieldset pp-fieldset--plain">
            <Form layout="vertical" class="detail-form__body">
              <div class="form-section">
                <div class="form-section__title form-section__title--toggle" @click="actionPanelExpanded = !actionPanelExpanded">
                  <span>执行与验证记录</span>
                  <IconifyIcon :icon="actionPanelExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
                </div>
                <div v-show="actionPanelExpanded" class="form-section-grid">
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">执行记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="执行结果" class="form-grid__inline">
                        <div v-if="!readOnly" class="choice-field">
                          <RadioGroup v-model:value="actionForm.result" size="small" class="pp-radio-group">
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">{{ actionForm.result || '-' }}</div>
                      </FormItem>
                      <FormItem label="记录人">
                        <Input v-if="modalMode === 'edit' || modalMode === 'submit'" v-model:value="actionForm.recorder" size="small" />
                        <div v-else class="head-item__value">{{ actionForm.recorder || '-' }}</div>
                      </FormItem>
                      <FormItem label="记录时间">
                        <Input v-if="modalMode === 'edit' || modalMode === 'submit'" v-model:value="actionForm.recorderTime" size="small" :placeholder="buildNowText()" />
                        <div v-else class="head-item__value">{{ actionForm.recorderTime || '-' }}</div>
                      </FormItem>
                      <FormItem label="执行备注">
                        <Input v-if="modalMode === 'edit' || modalMode === 'submit'" v-model:value="actionForm.formRemark" size="small" />
                        <div v-else class="head-item__value">{{ actionForm.formRemark || '-' }}</div>
                      </FormItem>
                    </div>
                  </div>
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">验证记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="检验结果" class="form-grid__inline">
                        <div v-if="!readOnly" class="choice-field">
                          <RadioGroup v-model:value="actionForm.inspectionResult" size="small" class="pp-radio-group">
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">{{ actionForm.inspectionResult || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认人">
                        <Input v-if="modalMode === 'confirm'" v-model:value="actionForm.confirmer" size="small" />
                        <div v-else class="head-item__value">{{ actionForm.confirmer || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认时间">
                        <Input v-if="modalMode === 'confirm'" v-model:value="actionForm.confirmerTime" size="small" :placeholder="buildNowText()" />
                        <div v-else class="head-item__value">{{ actionForm.confirmerTime || '-' }}</div>
                      </FormItem>
                      <FormItem label="确认备注">
                        <Input v-if="modalMode === 'confirm'" v-model:value="actionForm.confirmRemark" size="small" />
                        <div v-else class="head-item__value">{{ actionForm.confirmRemark || '-' }}</div>
                      </FormItem>
                    </div>
                  </div>
                </div>
              </div>
            </Form>
          </fieldset>

        </div>
      </div>
    </Modal>

    <AuthModal
      v-model:visible="confirmAuthVisible"
      :action-name="passWorkAuthActionName"
      :title="passWorkAuthTitle"
      auth-mode="username"
      @success="handleConfirmAuthSuccess"
    />
  </div>
</template>

<style scoped>
.pass-work-panel {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f5f7fa;
  padding: 8px;
}

.pp-plan-modal {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.pp-plan-toolbar__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pass-work-excel-input {
  display: none;
}

.pp-plan-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.pp-fieldset {
  margin: 0;
  padding: 8px 12px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

.pp-fieldset--plain {
  padding-top: 10px;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pass-work-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  align-items: center;
  margin-top: 8px;
  color: #374151;
  font-size: 12px;
}

.pass-work-import-attachment__label {
  color: #4b5563;
  font-weight: 700;
}

.pp-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  background: #fff;
  overflow: hidden;
}

.pp-panel__header {
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-weight: 700;
}

.pp-panel__title {
  display: flex;
  align-items: center;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.pp-panel__desc {
  font-size: 12px;
  color: #6b7280;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  border: 1px solid #e5e7eb;
  vertical-align: middle;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  font-weight: 400;
  text-align: center;
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-grid td[rowspan] {
  vertical-align: middle;
}

.pp-grid__sub {
  color: #9ca3af;
  font-size: 12px;
}

.pp-empty-cell {
  color: #9ca3af;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #4b5563;
}

.pass-work-list-wrap,
.pass-work-detail-wrap {
  height: 100%;
}

.pass-work-detail-wrap {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pass-work-check-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.pass-work-form-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.head-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.head-item__label {
  color: #4b5563;
  font-weight: 700;
}

.head-item__value {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
}

.pass-work-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-form__body {
  padding: 8px 10px 0;
}

.form-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-section {
  min-width: 0;
  padding: 0 0 2px;
}

.form-section--inner {
  padding: 0;
}

.form-section__title {
  margin-bottom: 10px;
  padding: 0 0 6px;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.form-section__title--toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.form-section__subtitle {
  margin-bottom: 10px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.form-grid {
  display: grid;
  gap: 8px 12px;
}

.form-grid--section {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-form__body :deep(.ant-form-item) {
  margin-bottom: 8px;
  min-width: 0;
}

.detail-form__body :deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: #4b5563;
  font-weight: 700;
}

.detail-form__body :deep(.ant-form-item-control-input) {
  min-height: 32px;
}

.detail-form__body :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

.detail-form__body :deep(.ant-input),
.detail-form__body :deep(.ant-input-affix-wrapper),
.detail-form__body :deep(.ant-picker),
.detail-form__body :deep(.ant-select-selector) {
  min-height: 32px !important;
  height: 32px !important;
}

.detail-form__body :deep(.ant-input) {
  line-height: 30px;
}

.pp-radio-group {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  min-height: 32px;
  padding: 0 6px;
}

.choice-field {
  min-height: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 5px;
  border: 1px solid #d9d9d9;
  background: #fff;
}

.pass-work-detail__footer {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 8px 10px;
}

.production-record-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}

.production-record-label-cell {
  color: #4b5563;
  font-weight: 700;
  text-align: center;
  font-size: 12px;
}

.pass-work-plan-check-tip {
  color: #b45309;
  font-weight: 700;
}

.production-record-cell--time :deep(.ant-input) {
  font-family: Consolas, 'Courier New', monospace;
}

.production-row--step-start td {
  border-top-width: 2px;
}

.pp-grid tbody tr.production-row--step-start td[rowspan] {
  background: #fafcff;
}

.footer-tip {
  font-size: 12px;
  color: #6b7280;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 0 !important;
}
</style>

<style>
.hc-pass-work-modal [class*='modal__header'],
.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal [class*='modal__close'],
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal [class*='modal__body'],
.hc-pass-work-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal [class*='modal__content'],
.hc-pass-work-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}
</style>
