<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { downloadFileFromBlobPart, formatDateTime } from '@vben/utils';
import { Button, DatePicker, Input, Modal, Radio, RadioGroup, Select, Tag, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmProcessFormRecord,
  createProcessFormRecord,
  deleteProcessFormRecord,
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  importProcessFormRecordLayout,
  submitProcessFormRecord,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import { getStationFormDetail, getStationFormSimpleList, type MesHcStationFormApi } from '#/api/mes/hc/stationform';
import { uploadFile } from '#/api/infra/file';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';

import {
  formTypeNameOf,
  PROCESS_FORM_RECORD_STATUS_OPTIONS,
  PROCESS_FORM_TYPE_OPTIONS,
} from '../../process-form-options';

const props = defineProps<{
  viewerOnly?: boolean;
  processCode: string;
  processName: string;
}>();

const CHECK_RESULT_FORM_TYPES = new Set(['STARTUP_CHECK', 'CLEANING_CHECK', 'MAINTENANCE_CHECK']);
const CHECK_RESULT_FORM_KEYWORDS = ['开机点检', '清洁点检', '清洁保养', '设备清洁点检', '保养点检'];
const CHECK_RESULT_REMARK_TIP = '开机、清洁保养时遇到问题请记录在备注列说明情况。';

const templateOptions = ref<MesHcProcessFormApi.TemplateOption[]>([]);
const chooseVisible = ref(false);
const editorVisible = ref(false);
const editorMode = ref<'edit' | 'view'>('edit');
const confirmAuthVisible = ref(false);
const currentTemplate = ref<MesHcProcessFormApi.Template>();
const currentRecord = reactive<MesHcProcessFormApi.Record>({});
const recordItems = ref<MesHcProcessFormApi.RecordItem[]>([]);
const readonlyRuntimeSchema = computed(() =>
  safeParseJson<Record<string, any>>(currentRecord.contextJson)?.runtimeSchema,
);
const readonlyRuntimeHeader = computed(() => {
  const saved = safeParseJson<Record<string, any>>(currentRecord.headerDataJson) || {};
  return {
    ...saved,
    planNo: saved.planNo || currentRecord.planNo,
    modelCode: saved.modelCode || currentRecord.modelCode,
    batchNo: saved.batchNo || currentRecord.batchNo,
    previewDetails: Array.isArray(saved.previewDetails) ? saved.previewDetails : recordItems.value
      .filter((item) => String(item.fieldKey || '').startsWith('runtime_detail') || !item.sourceRowJson)
      .map((item) => ({
        ...safeParseJson<Record<string, any>>(item.sourceRowJson),
        itemName: item.fieldLabel,
        standardText: item.standardText,
        actualValue: item.actualValue ?? item.actualNumber ?? item.actualTime,
        actualValue2: item.actualValue2,
        resultFlag: item.resultFlag,
        abnormalRemark: item.abnormalRemark,
      })),
  };
});
const excelImportInputRef = ref<HTMLInputElement>();
const excelLoading = ref(false);
const templateKeyword = ref('');
const headerData = reactive<Record<string, string>>({});
const chooser = reactive({
  batchNo: '',
  equipmentName: '',
  formType: undefined as string | undefined,
  modelCode: undefined as string | undefined,
  planNo: '',
  recordDate: new Date().toISOString().slice(0, 10),
  templateId: undefined as number | undefined,
});

const modelOptions = computed(() => {
  const map = new Map<string, string>();
  templateOptions.value.forEach((item) => {
    if (item.modelCode) map.set(item.modelCode, item.modelName || item.modelCode);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ label, value }));
});

const availableFormTypeOptions = computed(() => {
  const values = new Set(templateOptions.value.map((item) => item.formType).filter(Boolean));
  const list = PROCESS_FORM_TYPE_OPTIONS.filter((item) => values.has(item.value));
  return list.length ? list : PROCESS_FORM_TYPE_OPTIONS;
});

const filteredTemplateOptions = computed(() => {
  const keyword = templateKeyword.value.trim().toLowerCase();
  return templateOptions.value
    .filter((item) => !chooser.modelCode || item.modelCode === chooser.modelCode || item.modelCode === 'COMMON')
    .filter((item) => !chooser.formType || item.formType === chooser.formType)
    .filter((item) => {
      if (!keyword) return true;
      return [
        item.formTypeName,
        formTypeNameOf(item.formType),
        item.modelCode,
        item.modelName,
        item.templateCode,
        item.templateName,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword));
    });
});

const selectedTemplateOption = computed(() =>
  templateOptions.value.find((item) => item.id === chooser.templateId),
);

const editorTitle = computed(() => {
  const action = editorMode.value === 'view' ? '查看' : currentRecord.id ? '编辑' : '填写';
  return `${action}${props.processName}${currentRecord.formTypeName || formTypeNameOf(currentRecord.formType) || '表单'}`;
});

const confirmAuthActionName = computed(() =>
  `确认${currentRecord.formTypeName || formTypeNameOf(currentRecord.formType) || currentTemplate.value?.templateName || '动态表单'}`,
);

const templateHeaderItems = computed(() =>
  (currentTemplate.value?.items || []).filter(
    (item) => item.areaType === 'HEADER' && item.fieldLabel && !isRawExcelHeaderItem(item),
  ),
);

const templateFooterItems = computed(() =>
  (currentTemplate.value?.items || []).filter((item) => item.areaType === 'FOOTER'),
);

const detailVisualMode = computed(() => {
  const type = currentRecord.formType || currentTemplate.value?.formType;
  if (props.processCode === 'WET' && isWetSemiFormType(type)) {
    return 'wet-semi';
  }
  if (['STARTUP_CHECK', 'CLEANING_CHECK', 'ELECTRIC_HOIST_CHECK', 'MAINTENANCE_CHECK', 'PURE_WATER_CHECK'].includes(type || '')) {
    return 'check';
  }
  if (type === 'PRODUCTION_CHECK' && props.processCode === 'FORMULA') {
    return 'formula-production-check';
  }
  if (['INNER_PACKAGING_CHECK', 'PRODUCTION_CHECK'].includes(type || '')) {
    return 'production-check';
  }
  if (['DMF_CHECK', 'GUIDE_CLOTH_CHANGE', 'PROCESS_PARAM', 'WATER_CHANGE'].includes(type || '')) {
    return 'param';
  }
  if (['APPEARANCE_RECORD', 'COAGULATION_SEMI_RECORD', 'INTERMEDIATE_RECORD', 'OVEN_SEMI_RECORD'].includes(type || '')) {
    return 'semi';
  }
  return 'record';
});

const isStartupCleaningMaintenanceRecord = computed(() => {
  const type = currentRecord.formType || currentTemplate.value?.formType || '';
  if (CHECK_RESULT_FORM_TYPES.has(type)) return true;
  const text = [
    currentRecord.formTypeName,
    currentRecord.templateName,
    currentTemplate.value?.formTypeName,
    currentTemplate.value?.templateName,
    formTypeNameOf(type),
  ]
    .filter(Boolean)
    .join(' ');
  return CHECK_RESULT_FORM_KEYWORDS.some((keyword) => text.includes(keyword));
});

const detailVisualTitle = computed(() => {
  if (detailVisualMode.value === 'wet-semi') return currentRecord.formTypeName || '半成品记录';
  if (detailVisualMode.value === 'check') return '点检/清洁明细';
  if (detailVisualMode.value === 'formula-production-check' || detailVisualMode.value === 'production-check') return '生产点检明细';
  if (detailVisualMode.value === 'param') return '工艺参数明细';
  if (detailVisualMode.value === 'semi') return '半成品/中间品记录';
  return '生产记录明细';
});

const excelAttachments = computed<ExcelAttachment[]>(() => {
  const attachments = readRecordContext().processFormExcelAttachments;
  return Array.isArray(attachments) ? attachments : [];
});

const wetSemiThicknessHeaders = computed(() => extractWetSemiThicknessHeaders(currentTemplate.value));

const wetSemiHeadFields = computed(() => {
  const isCoagulation = currentRecord.formType === 'COAGULATION_SEMI_RECORD';
  const fields = [
    { editable: false, key: 'productionDate', label: '生产日期：', type: 'text', value: currentRecord.recordDate || '-' },
    { editable: false, key: 'modelCode', label: '型号：', type: 'text', value: currentRecord.modelName || currentRecord.modelCode || '-' },
    { editable: false, key: 'batchNo', label: '产品批号：', type: 'text', value: currentRecord.batchNo || '-' },
    { editable: true, key: 'generatedLength', label: '半成品长度/m：', type: 'input', value: headerData.generatedLength || '' },
    { editable: true, key: 'semiWidth', label: `${getWetSemiWidthLabel(currentTemplate.value, isCoagulation)}：`, type: 'input', value: headerData.semiWidth || '' },
  ];
  if (isCoagulation) {
    fields.push({ editable: true, key: 'poreDevelopment', label: '泡孔发育：', type: 'radio', value: headerData.poreDevelopment || 'OK' });
    fields.push({ editable: true, key: 'finalResult', label: '综合判定：', type: 'radio', value: headerData.finalResult || 'OK' });
  } else {
    fields.push({ editable: true, key: 'finalResult', label: '判定：', type: 'radio', value: headerData.finalResult || 'OK' });
  }
  return fields;
});

const wetSemiHeaderClass = computed(() =>
  `production-check-head-grid ${
    currentRecord.formType === 'OVEN_SEMI_RECORD'
      ? 'production-check-head-grid--cols-4'
      : 'production-check-head-grid--cols-3'
  }`,
);

const formulaProductionHeadFields = computed(() => [
  {
    field: 'mixerEquipmentCode',
    label: '搅拌机台号',
    value: currentRecord.equipmentName || currentRecord.equipmentCode || headerData.mixerEquipmentCode || '-',
  },
  {
    field: 'foamingEquipmentCode',
    label: '脱泡机台号',
    value: headerData.foamingEquipmentCode || '-',
  },
  {
    field: 'recorder',
    label: '记录人',
    value: currentRecord.fillUserName || '-',
  },
  {
    field: 'confirmer',
    label: '确认人',
    value: currentRecord.confirmUserName || '-',
  },
  {
    field: 'recorderTime',
    label: '记录时间',
    value: currentRecord.fillTime || currentRecord.createTime || '-',
  },
]);

type ProductionActualField = 'actualValue' | 'actualValue2';

type ProductionDetailRenderRow = {
  actualField: ProductionActualField;
  category: string;
  id: number | string;
  item: string;
  node: string;
  recordLabel: string;
  source: MesHcProcessFormApi.RecordItem;
  sourceIndex: number;
  sourceRowId: string;
  standard: string;
};

type ExcelAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};

const productionDetailRows = computed<ProductionDetailRenderRow[]>(() =>
  recordItems.value.flatMap((row, index) => {
    const group = resolveProductionGroup(row);
    const base = {
      category: group.category,
      id: row.itemSeq || row.id || index + 1,
      item: row.fieldLabel || '-',
      node: group.node,
      source: row,
      sourceIndex: index,
      sourceRowId: String(row.templateItemId || row.id || row.fieldKey || `row-${index}`),
      standard: getStandardText(row),
    };
    if (!isProductionDualValue(row)) {
      return [
        {
          ...base,
          actualField: 'actualValue' as const,
          recordLabel: '',
        },
      ];
    }
    return [
      {
        ...base,
        actualField: 'actualValue' as const,
        recordLabel: getProductionDualLabel(row, 'actualValue'),
      },
      {
        ...base,
        actualField: 'actualValue2' as const,
        recordLabel: getProductionDualLabel(row, 'actualValue2'),
      },
    ];
  }),
);

const gridFormSchema: VbenFormSchema[] = [
  { fieldName: 'modelCode', label: '型号', component: 'Input' },
  {
    fieldName: 'formType',
    label: '类型',
    component: 'Select',
    componentProps: { allowClear: true, options: PROCESS_FORM_TYPE_OPTIONS },
  },
  {
    fieldName: 'recordStatus',
    label: '状态',
    component: 'Select',
    componentProps: { allowClear: true, options: PROCESS_FORM_RECORD_STATUS_OPTIONS },
  },
  {
    fieldName: 'recordDate',
    label: '日期',
    component: 'DatePicker',
    componentProps: { valueFormat: 'YYYY-MM-DD' },
  },
  { fieldName: 'planNo', label: '计划', component: 'Input' },
  { fieldName: 'batchNo', label: '批次', component: 'Input' },
];

const recordColumns: VxeTableGridOptions<MesHcProcessFormApi.Record>['columns'] = [
  { field: 'recordNo', title: '记录编号', minWidth: 190 },
  { field: 'templateName', title: '表单名称', minWidth: 280 },
  { field: 'modelName', title: '型号', minWidth: 130 },
  { field: 'formTypeName', title: '类型', minWidth: 140 },
  { field: 'recordDate', title: '填写日期', width: 120 },
  { field: 'planNo', title: '计划', minWidth: 150 },
  { field: 'batchNo', title: '批次', minWidth: 160 },
  { field: 'recordStatus', title: '状态', width: 110, slots: { default: 'recordStatus' } },
  { field: 'resultStatus', title: '结果', width: 90, slots: { default: 'resultStatus' } },
  { field: 'fillUserName', title: '填写人', width: 110 },
  { field: 'confirmUserName', title: '确认人', width: 110 },
  {
    field: 'createTime',
    title: '创建时间',
    minWidth: 170,
    formatter: ({ cellValue }) => (cellValue ? (formatDateTime(cellValue) as string) : '-'),
  },
  { title: '操作', width: 220, fixed: 'right', slots: { default: 'actions' } },
];

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: gridFormSchema },
  gridOptions: {
    columns: recordColumns,
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getProcessFormRecordPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            processCode: props.processCode,
            ...formValues,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesHcProcessFormApi.Record>,
});

function statusColor(status?: string) {
  if (status === 'CONFIRMED') return 'success';
  if (status === 'SUBMITTED') return 'processing';
  if (status === 'VOID') return 'default';
  return 'warning';
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'default';
}

function headerItemKey(item: MesHcProcessFormApi.TemplateItem) {
  return item.fieldKey || `item_${item.id || item.itemSeq || item.fieldLabel}`;
}

function isRawExcelHeaderItem(item: MesHcProcessFormApi.TemplateItem) {
  const label = String(item.fieldLabel || '').trim();
  const templateName = String(currentTemplate.value?.templateName || '').trim();
  const source = safeParseJson<{ cells?: any[]; row?: number }>(item.sourceRowJson);
  const headerTokenCount = ['型号', '料号', '批号', '生产日期', '日期', '担当', '确认', '宽幅']
    .filter((token) => label.includes(token)).length;
  if (!label) return true;
  if (label.includes('|')) return true;
  if (source?.cells && source.cells.length > 1) return true;
  if (headerTokenCount >= 2) return true;
  if (templateName && (label === templateName || templateName.includes(label) || label.includes(templateName))) return true;
  return /^(.*记录表|.*点检表|.*检查表|.*生产记录表)$/u.test(label);
}

function resetHeaderData(items: MesHcProcessFormApi.TemplateItem[] = [], json?: string) {
  Object.keys(headerData).forEach((key) => delete headerData[key]);
  let savedData: Record<string, string> = {};
  if (json) {
    try {
      savedData = JSON.parse(json) || {};
    } catch {
      savedData = {};
    }
  }
  items
    .filter((item) => item.areaType === 'HEADER' && item.fieldLabel)
    .forEach((item) => {
      const key = headerItemKey(item);
      headerData[key] = savedData[key] ?? item.defaultValue ?? '';
    });
}

function getHeaderValue(item: MesHcProcessFormApi.TemplateItem) {
  return headerData[headerItemKey(item)] || '-';
}

function getFieldRowSpan(index: number, fields: Array<keyof MesHcProcessFormApi.RecordItem>) {
  const rows = recordItems.value;
  const key = fields.map((field) => rows[index]?.[field] || '').join('|');
  const prevKey = fields.map((field) => rows[index - 1]?.[field] || '').join('|');
  if (index > 0 && key === prevKey) return 0;
  let span = 1;
  for (let i = index + 1; i < rows.length; i += 1) {
    const nextKey = fields.map((field) => rows[i]?.[field] || '').join('|');
    if (nextKey !== key) break;
    span += 1;
  }
  return span;
}

function getStandardText(row: MesHcProcessFormApi.RecordItem) {
  if (row.standardText && row.unit && !row.standardText.includes(row.unit)) {
    return `${row.standardText} ${row.unit}`;
  }
  return row.standardText || row.unit || '-';
}

function getActualPlaceholder(row: MesHcProcessFormApi.RecordItem) {
  if (row.valueMode === 'OK_NG') return '可填写实际情况';
  if (row.unit) return `请输入${row.unit}`;
  return '请输入';
}

function normalizeCheckResultFlag(value?: null | number | string) {
  const text = String(value ?? '').trim().toUpperCase();
  if (['NG', 'NOK', 'NO', 'FAIL', 'FAILED', '异常', '不合格'].includes(text)) return 'NG';
  if (['OK', 'PASS', 'PASSED', 'YES', '正常', '合格'].includes(text)) return 'OK';
  return text || 'OK';
}

function recordItemResult(row: MesHcProcessFormApi.RecordItem) {
  return normalizeCheckResultFlag(
    row.resultFlag || (row as any).status || (row as any).result || (row as any).checkResult,
  );
}

function updateRecordItemResult(row: MesHcProcessFormApi.RecordItem, value: string) {
  const result = normalizeCheckResultFlag(value);
  row.resultFlag = result;
  (row as any).status = result;
  (row as any).result = result;
  (row as any).checkResult = result;
}

function getSourceRowCellTexts(row: MesHcProcessFormApi.RecordItem) {
  const source = safeParseJson<{ cells?: Array<{ value?: string }> }>(row.sourceRowJson);
  return (source?.cells || []).map((cell) => String(cell.value || '').trim()).filter(Boolean);
}

function resolveProductionGroup(row: MesHcProcessFormApi.RecordItem) {
  const category = String(row.itemCategory || '').trim();
  const node = String(row.stepNode || '').trim();
  if (!node && category.includes('/')) {
    const parts = category.split(/\s*\/\s*/u).map((item) => item.trim()).filter(Boolean);
    return {
      category: parts[0] || category || '-',
      node: parts.slice(1).join(' / ') || '-',
    };
  }
  return {
    category: category || '-',
    node: node || '-',
  };
}

function isProductionDualValue(row: MesHcProcessFormApi.RecordItem) {
  const valueMode = String(row.valueMode || '').toUpperCase();
  const label = String(row.fieldLabel || '');
  if (valueMode === 'DUAL_TEXT' || label.includes('加入') || label.includes('添加')) return true;
  return getSourceRowCellTexts(row).some((value) => value.includes('批号'));
}

function isProductionTimeValue(row: MesHcProcessFormApi.RecordItem) {
  const valueMode = String(row.valueMode || '').toUpperCase();
  const label = String(row.fieldLabel || '');
  const standard = String(row.standardText || '');
  return valueMode === 'TIME' || label.includes('时间') || standard.includes('记录开始时间') || standard.includes('记录结束时间');
}

function getProductionDualLabel(row: MesHcProcessFormApi.RecordItem, actualField: ProductionActualField) {
  const sourceLabels = getSourceRowCellTexts(row);
  if (actualField === 'actualValue2') {
    const batchLabel = sourceLabels.find((value) => value.includes('批号'));
    return batchLabel?.replace(/[:：]/gu, '') || '批号';
  }
  return row.unit || '重量';
}

function getProductionInputPlaceholder(row: MesHcProcessFormApi.RecordItem, actualField: ProductionActualField) {
  if (isProductionDualValue(row)) {
    return getProductionDualLabel(row, actualField);
  }
  if (isProductionTimeValue(row)) {
    return 'HH:mm';
  }
  return getStandardText(row) === '-' ? '请输入' : getStandardText(row);
}

function getResolvedProductionRowSpan(index: number, field: 'category' | 'node') {
  const rows = recordItems.value;
  const current = resolveProductionGroup(rows[index] || {})[field];
  const prev = resolveProductionGroup(rows[index - 1] || {})[field];
  if (index > 0 && current === prev) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (resolveProductionGroup(rows[cursor] || {})[field] !== current) break;
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

function isProductionStepStart(index: number) {
  const rows = productionDetailRows.value;
  const current = rows[index];
  return !!current && (index === 0 || rows[index - 1]?.node !== current.node);
}

function safeParseJson<T = any>(value?: string | null): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function schemaText(schema: Record<string, any>, ...keys: string[]) {
  for (const key of keys) {
    const value = schema?.[key];
    if (Array.isArray(value)) {
      const joined = value.map((item) => String(item || '').trim()).filter(Boolean).join('_');
      if (joined) return joined;
      continue;
    }
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return undefined;
}

function resolveStationAreaType(item: MesHcStationFormApi.StationFormItem) {
  const category = String(item.itemCategory || '').toUpperCase();
  if (category.includes('HEADER') || item.itemCategory?.includes('表头')) return 'HEADER';
  if (category.includes('FOOTER') || item.itemCategory?.includes('页脚')) return 'FOOTER';
  if ((item.itemSeq || 0) >= 900) return 'FOOTER';
  return 'DETAIL';
}

function resolveStationControlType(valueMode?: string) {
  const mode = String(valueMode || 'TEXT').toUpperCase();
  if (['BOOLEAN', 'OK_NG', 'RADIO'].includes(mode)) return 'RADIO';
  if (mode === 'DATE') return 'DATE';
  if (mode === 'TIME') return 'TIME';
  return 'INPUT';
}

function isResultFlag(value?: string) {
  return ['OK', 'NG'].includes(String(value || '').toUpperCase());
}

function stationFormToTemplate(
  form: MesHcStationFormApi.StationForm,
  option?: MesHcProcessFormApi.TemplateOption,
): MesHcProcessFormApi.Template {
  const schema = safeParseJson<Record<string, any>>(form.schemaJson) || {};
  const formType =
    option?.formType ||
    schemaText(schema, 'processFormType', 'formType', 'adhesive1FormType', 'adhesive2FormType', 'pressSlotFormType') ||
    form.triggerTimingCode ||
    'OTHER';
  const modelScope = option?.modelScope || schemaText(schema, 'modelScope') || 'COMMON';
  const modelCode =
    option?.modelCode ||
    (modelScope === 'COMMON'
      ? 'COMMON'
      : schemaText(schema, 'modelCode', 'modelCodes', 'modelCodePrefix', 'modelPrefix', 'sourceModelCode')) ||
    'COMMON';
  const sourceItems = form.items?.length ? form.items : form.presetItems || [];
  return {
    createTime: form.createTime,
    currentVersion: {
      id: form.id,
      isCurrent: true,
      layoutJson: form.schemaJson,
      sheetJson: form.presetItems ? JSON.stringify(form.presetItems) : undefined,
      templateCode: form.formCode,
      templateId: form.id,
      versionNo: schemaText(schema, 'version', 'versionNo') || 'station-form',
    },
    currentVersionId: form.id,
    formType,
    formTypeName: option?.formTypeName || schemaText(schema, 'formTypeName', 'displayName') || formTypeNameOf(formType),
    id: form.id,
    items: sourceItems.map((item, index) => {
      const defaultResult = isResultFlag(item.defaultResult) ? String(item.defaultResult).toUpperCase() : 'OK';
      const defaultValue =
        item.defaultResult && !isResultFlag(item.defaultResult) && String(item.valueMode || '').toUpperCase() !== 'OK_NG'
          ? item.defaultResult
          : undefined;
      return {
        areaType: resolveStationAreaType(item),
        controlType: resolveStationControlType(item.valueMode),
        defaultResult,
        defaultValue,
        fieldKey: `station_${form.id || 'form'}_${item.id || item.itemSeq || index + 1}`,
        fieldLabel: item.itemName,
        id: item.id,
        itemCategory: item.itemCategory,
        itemSeq: item.itemSeq,
        remark: item.remark,
        requiredFlag: item.requiredFlag,
        standardText: item.standardText,
        stepNode: item.stepNode,
        templateId: form.id,
        valueMode: item.valueMode || 'TEXT',
        versionId: form.id,
      } as MesHcProcessFormApi.TemplateItem;
    }),
    modelCode,
    modelName: option?.modelName || schemaText(schema, 'modelName', 'sourceModelName') || (modelCode === 'COMMON' ? '通用' : modelCode),
    modelScope,
    needConfirm: form.needConfirm,
    processCode: form.processCode,
    processName: form.processName,
    remark: form.remark,
    sortNo: form.sortNo,
    status: form.status === 1 ? 'ACTIVE' : 'INACTIVE',
    templateCode: form.formCode,
    templateName: form.formName,
  };
}

function stationFormToTemplateOption(
  form: MesHcStationFormApi.SimpleItem | MesHcStationFormApi.StationForm,
): MesHcProcessFormApi.TemplateOption {
  const template = stationFormToTemplate(form as MesHcStationFormApi.StationForm);
  return {
    formType: template.formType,
    formTypeName: template.formTypeName,
    id: template.id,
    modelCode: template.modelCode,
    modelName: template.modelName,
    modelScope: template.modelScope,
    processCode: template.processCode,
    processName: template.processName,
    templateCode: template.templateCode,
    templateName: template.templateName,
  };
}

function readRecordContext() {
  return safeParseJson<Record<string, any>>(currentRecord.contextJson) || {};
}

function writeRecordContext(context: Record<string, any>) {
  currentRecord.contextJson = JSON.stringify(context);
}

function compactLabel(label?: string) {
  return String(label || '').replace(/[：:]\s*$/u, '');
}

function displayText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function inputText(value?: null | number | string) {
  return String(value ?? '');
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}

function stripImportedRecordLabel(
  row: MesHcProcessFormApi.RecordItem,
  bindField: string,
  value?: null | number | string,
) {
  const text = inputText(value).trim();
  if (!text || !['actualValue', 'actualValue2'].includes(bindField) || !isProductionDualValue(row)) {
    return text;
  }
  const labels = [
    getProductionDualLabel(row, bindField as ProductionActualField),
    bindField === 'actualValue2' ? '批号' : '重量',
  ].filter(Boolean);
  for (const label of Array.from(new Set(labels))) {
    const pattern = new RegExp(`^${escapeRegExp(label)}\\s*[:：]?\\s*`, 'u');
    const stripped = text.replace(pattern, '').trim();
    if (stripped !== text || text === label) {
      return stripped;
    }
  }
  return text;
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
    colSpan: options.colSpan || 1,
    editable: options.editable || false,
    rowSpan: options.rowSpan || 1,
    text: String(text ?? ''),
    ...(options.bindField ? { bindField: options.bindField } : {}),
    ...(options.bindKey ? { bindKey: options.bindKey } : {}),
  };
}

function buildExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  if (detailVisualMode.value === 'wet-semi') {
    return wetSemiHeadFields.value.map((field) => ({
      bindField: 'headerData',
      bindKey: field.key,
      editable: field.editable,
      label: compactLabel(field.label),
      value: headerData[field.key] || field.value || '',
    }));
  }
  if (detailVisualMode.value === 'formula-production-check') {
    return formulaProductionHeadFields.value.map((field) => ({
      editable: false,
      label: field.label,
      value: field.value || '',
    }));
  }
  return [
    { editable: false, label: '型号', value: currentRecord.modelName || currentRecord.modelCode || '' },
    { bindField: 'recordDate', bindKey: 'recordDate', editable: editorMode.value !== 'view', label: '填写日期', value: currentRecord.recordDate || '' },
    { bindField: 'planNo', bindKey: 'planNo', editable: editorMode.value !== 'view', label: '计划', value: currentRecord.planNo || '' },
    { bindField: 'batchNo', bindKey: 'batchNo', editable: editorMode.value !== 'view', label: '批次', value: currentRecord.batchNo || '' },
    { bindField: 'equipmentName', bindKey: 'equipmentName', editable: editorMode.value !== 'view', label: '设备', value: currentRecord.equipmentName || '' },
    { editable: false, label: '填写人', value: currentRecord.fillUserName || '' },
    { editable: false, label: '确认人', value: currentRecord.confirmUserName || '' },
    ...templateHeaderItems.value.map((item) => ({
      bindField: 'headerData',
      bindKey: headerItemKey(item),
      editable: editorMode.value !== 'view',
      label: compactLabel(item.fieldLabel),
      value: headerData[headerItemKey(item)] || '',
    })),
  ];
}

function buildExcelColumns(): MesHcProcessFormApi.LayoutColumn[] {
  if (detailVisualMode.value === 'wet-semi') {
    return [
      { title: '长度/m', width: 92 },
      ...wetSemiThicknessHeaders.value.map((title) => ({ title, width: 180 })),
      { title: '备注', width: 260 },
    ];
  }
  if (detailVisualMode.value === 'check' && currentRecord.formType !== 'CLEANING_CHECK') {
    return [
      { title: '序号', width: 80 },
      { title: '点检项目', width: 260 },
      { title: '标准', width: 420 },
      isStartupCleaningMaintenanceRecord.value
        ? { title: 'OK/NG', width: 120 }
        : { title: '实际/记录', width: 220 },
      { title: '备注', width: 260 },
    ];
  }
  if (detailVisualMode.value === 'check') {
    return [
      { title: '工序', width: 120 },
      { title: '点检项目', width: 260 },
      { title: '检查标准', width: 420 },
      isStartupCleaningMaintenanceRecord.value
        ? { title: 'OK/NG', width: 120 }
        : { title: '实际/记录', width: 220 },
      { title: '异常备注', width: 260 },
    ];
  }
  if (detailVisualMode.value === 'production-check') {
    return [
      { title: '物料/生产环节', width: 120 },
      { title: '确认节点', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '点检标准', width: 180 },
      { title: '实际/记录', width: 180 },
      { title: '异常备注', width: 220 },
    ];
  }
  if (detailVisualMode.value === 'formula-production-check') {
    return [
      { title: '序号', width: 70 },
      { title: '项目类别', width: 110 },
      { title: '步骤节点', width: 110 },
      { title: '点检项目', width: 220 },
      { title: '标准', width: 260 },
      { title: '记录值', width: 280 },
      { title: '异常说明', width: 280 },
    ];
  }
  if (detailVisualMode.value === 'param') {
    return [
      { title: '物料/生产环节', width: 130 },
      { title: '工艺参数项目', width: 280 },
      { title: '标准', width: 320 },
      { title: '实测值', width: 240 },
      { title: '备注', width: 260 },
    ];
  }
  if (detailVisualMode.value === 'semi') {
    return [
      { title: '分类', width: 120 },
      { title: '节点/位置', width: 150 },
      { title: '记录项目', width: 260 },
      { title: '标准/说明', width: 320 },
      { title: '实际值', width: 220 },
      { title: '备注', width: 260 },
    ];
  }
  return [
    { title: '序号', width: 72 },
    { title: '分类', width: 150 },
    { title: '节点', width: 170 },
    { title: '记录项目', width: 280 },
    { title: '标准/说明', width: 360 },
    { title: '实际/记录', width: 220 },
    { title: '备注', width: 260 },
  ];
}

function buildWetSemiExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  return recordItems.value.map((row, index) => ({
    cells: [
      excelCell(0, displayText(row.fieldLabel), { editable: false }),
      ...wetSemiThicknessHeaders.value.map((_, cellIndex) =>
        excelCell(cellIndex + 1, getWetSemiCellValue(row, cellIndex), {
          bindField: `wetValue:${cellIndex}`,
          bindKey: itemBindKey(index),
          editable: editorMode.value !== 'view',
        }),
      ),
      excelCell(wetSemiThicknessHeaders.value.length + 1, row.abnormalRemark || '', {
        bindField: 'abnormalRemark',
        bindKey: itemBindKey(index),
        editable: editorMode.value !== 'view',
      }),
    ],
  }));
}

function buildRecordItemExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (detailVisualMode.value === 'wet-semi') return buildWetSemiExcelRows();
  if (detailVisualMode.value === 'formula-production-check') {
    return productionDetailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const sourceSpan = getProductionSourceRowSpan(index);
      const categorySpan = getProductionFieldRowSpan(index, 'category');
      const nodeSpan = getProductionFieldRowSpan(index, 'node');
      const itemSpan = getProductionFieldRowSpan(index, 'item');
      if (sourceSpan > 0) cells.push(excelCell(0, displayText(row.id), { rowSpan: sourceSpan }));
      if (categorySpan > 0) cells.push(excelCell(1, displayText(row.category), { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(excelCell(2, displayText(row.node), { rowSpan: nodeSpan }));
      if (itemSpan > 0) cells.push(excelCell(3, displayText(row.item), { rowSpan: itemSpan }));
      if (sourceSpan > 0) cells.push(excelCell(4, displayText(row.standard), { rowSpan: sourceSpan }));
      const recordValue = row.source[row.actualField] || '';
      cells.push(
        excelCell(5, row.recordLabel ? `${row.recordLabel}${recordValue ? ` ${recordValue}` : ''}` : recordValue, {
          bindField: row.actualField,
          bindKey: itemBindKey(row.sourceIndex),
          editable: editorMode.value !== 'view',
        }),
      );
      if (sourceSpan > 0) {
        cells.push(
          excelCell(6, row.source.abnormalRemark || '', {
            bindField: 'abnormalRemark',
            bindKey: itemBindKey(row.sourceIndex),
            editable: editorMode.value !== 'view',
            rowSpan: sourceSpan,
          }),
        );
      }
      return { cells };
    });
  }
  return recordItems.value.map((row, index) => {
    const bindKey = itemBindKey(index);
    if (detailVisualMode.value === 'check' && currentRecord.formType !== 'CLEANING_CHECK') {
      return {
        cells: [
          excelCell(0, row.itemSeq || index + 1),
          excelCell(1, displayText(row.fieldLabel)),
          excelCell(2, displayText(getStandardText(row))),
          isStartupCleaningMaintenanceRecord.value
            ? excelCell(3, recordItemResult(row), { bindField: 'resultFlag', bindKey, editable: editorMode.value !== 'view' })
            : excelCell(3, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }),
          excelCell(4, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }),
        ],
      };
    }
    if (detailVisualMode.value === 'check') {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, ['itemCategory']);
      if (categorySpan > 0) cells.push(excelCell(0, displayText(row.itemCategory || row.stepNode), { rowSpan: categorySpan }));
      cells.push(excelCell(1, displayText(row.fieldLabel)));
      cells.push(excelCell(2, displayText(getStandardText(row))));
      cells.push(
        isStartupCleaningMaintenanceRecord.value
          ? excelCell(3, recordItemResult(row), { bindField: 'resultFlag', bindKey, editable: editorMode.value !== 'view' })
          : excelCell(3, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }),
      );
      cells.push(excelCell(4, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }));
      return { cells };
    }
    if (detailVisualMode.value === 'production-check') {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getResolvedProductionRowSpan(index, 'category');
      const nodeSpan = getResolvedProductionRowSpan(index, 'node');
      if (categorySpan > 0) cells.push(excelCell(0, displayText(resolveProductionGroup(row).category), { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(excelCell(1, displayText(resolveProductionGroup(row).node), { rowSpan: nodeSpan }));
      cells.push(excelCell(2, displayText(row.fieldLabel)));
      cells.push(excelCell(3, displayText(getStandardText(row))));
      cells.push(excelCell(4, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }));
      cells.push(excelCell(5, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }));
      return { cells };
    }
    if (detailVisualMode.value === 'param') {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, ['itemCategory']);
      if (categorySpan > 0) cells.push(excelCell(0, displayText(row.itemCategory), { rowSpan: categorySpan }));
      cells.push(excelCell(1, displayText(row.fieldLabel || row.stepNode)));
      cells.push(excelCell(2, displayText(getStandardText(row))));
      cells.push(excelCell(3, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }));
      cells.push(excelCell(4, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }));
      return { cells };
    }
    if (detailVisualMode.value === 'semi') {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFieldRowSpan(index, ['itemCategory']);
      const nodeSpan = getFieldRowSpan(index, ['itemCategory', 'stepNode']);
      if (categorySpan > 0) cells.push(excelCell(0, displayText(row.itemCategory), { rowSpan: categorySpan }));
      if (nodeSpan > 0) cells.push(excelCell(1, displayText(row.stepNode), { rowSpan: nodeSpan }));
      cells.push(excelCell(2, displayText(row.fieldLabel)));
      cells.push(excelCell(3, displayText(getStandardText(row))));
      cells.push(excelCell(4, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }));
      cells.push(excelCell(5, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }));
      return { cells };
    }
    return {
      cells: [
        excelCell(0, row.itemSeq || index + 1),
        excelCell(1, displayText(row.itemCategory)),
        excelCell(2, displayText(row.stepNode)),
        excelCell(3, displayText(row.fieldLabel)),
        excelCell(4, displayText(getStandardText(row))),
        excelCell(5, row.actualValue || '', { bindField: 'actualValue', bindKey, editable: editorMode.value !== 'view' }),
        excelCell(6, row.abnormalRemark || '', { bindField: 'abnormalRemark', bindKey, editable: editorMode.value !== 'view' }),
      ],
    };
  });
}

function sanitizeExcelFileName(name?: string) {
  return `${displayText(name, '工序表单')}.xlsx`.replace(/[\\/:*?"<>|]/gu, '_');
}

function buildExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const title = currentRecord.templateName || editorTitle.value;
  return {
    columns: buildExcelColumns(),
    detailTitle: detailVisualTitle.value,
    fileName: sanitizeExcelFileName(title),
    footerNotes: templateFooterItems.value.map((item) => item.fieldLabel || '').filter(Boolean),
    headerItems: buildExcelHeaderItems(),
    rows: buildRecordItemExcelRows(),
    sheetName: detailVisualTitle.value,
    title,
    visualMode: detailVisualMode.value,
  };
}

function applyImportedExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  (resp.headerValues || []).forEach((item) => {
    if (!item.bindField) return;
    if (item.bindField === 'headerData' && item.bindKey) {
      headerData[item.bindKey] = inputText(item.value);
      return;
    }
    (currentRecord as any)[item.bindField] = inputText(item.value);
  });
  (resp.cellValues || []).forEach((item) => {
    const row = recordItems.value[Number(item.bindKey)];
    if (!row || !item.bindField) return;
    if (item.bindField.startsWith('wetValue:')) {
      const index = Number(item.bindField.replace('wetValue:', ''));
      setWetSemiCellValue(row, index, inputText(item.value));
      return;
    }
    if (item.bindField === 'resultFlag') {
      if (isStartupCleaningMaintenanceRecord.value) {
        updateRecordItemResult(row, inputText(item.value));
      }
      return;
    }
    (row as any)[item.bindField] = stripImportedRecordLabel(row, item.bindField, item.value);
  });
}

async function attachImportedExcel(file: File) {
  const uploaded = (await uploadFile({
    directory: 'mes/process-form-record',
    file,
  })) as any;
  const context = readRecordContext();
  const attachments = Array.isArray(context.processFormExcelAttachments) ? context.processFormExcelAttachments : [];
  context.processFormExcelAttachments = [
    ...attachments,
    {
      name: uploaded?.name || file.name,
      path: uploaded?.path,
      size: uploaded?.size || file.size,
      type: uploaded?.type || file.type,
      uploadTime: new Date().toISOString(),
      url: typeof uploaded === 'string' ? uploaded : uploaded?.url,
    },
  ];
  writeRecordContext(context);
}

async function handleExportExcel() {
  const layout = buildExcelLayout();
  const data = await exportProcessFormRecordLayout(layout);
  downloadFileFromBlobPart({ fileName: layout.fileName || '工序表单.xlsx', source: data });
}

function triggerImportExcel() {
  excelImportInputRef.value?.click();
}

async function handleExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xlsx?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  excelLoading.value = true;
  try {
    const layout = buildExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    applyImportedExcel(resp);
    await attachImportedExcel(file);
    if (editorMode.value !== 'view') {
      await saveRecord(true);
    }
    message.success(`导入成功，已回填 ${resp.totalCellCount || 0} 个单元格并挂接原始附件`);
  } finally {
    excelLoading.value = false;
  }
}

function isWetSemiFormType(type?: string) {
  return type === 'COAGULATION_SEMI_RECORD' || type === 'OVEN_SEMI_RECORD';
}

function getPrimarySheet(template?: MesHcProcessFormApi.Template) {
  const sheetJson = safeParseJson<any>(template?.currentVersion?.sheetJson);
  const sheets = Array.isArray(sheetJson?.sheets) ? sheetJson.sheets : [];
  if (!sheets.length) return null;
  return sheets.find((item: any) => item.sheetName === sheetJson?.primarySheet) || sheets[0];
}

function getCellText(cell: any) {
  return String(cell?.value ?? '').trim();
}

function isNumericText(value?: string) {
  return !!value && /^-?\d+(\.\d+)?$/.test(value.trim());
}

function findWetSemiHeaderRow(template?: MesHcProcessFormApi.Template) {
  const sheet = getPrimarySheet(template);
  const rows = Array.isArray(sheet?.nonEmptyRows) ? sheet.nonEmptyRows : [];
  return rows.find((row: any) =>
    (row.cells || []).some((cell: any) => cell.col === 1 && getCellText(cell).includes('长度/m')),
  );
}

function extractWetSemiThicknessHeaders(template?: MesHcProcessFormApi.Template) {
  const headerRow = findWetSemiHeaderRow(template);
  const cells = [...(headerRow?.cells || [])].sort((a: any, b: any) => Number(a.col || 0) - Number(b.col || 0));
  const headers = cells
    .filter((cell: any) => Number(cell.col || 0) > 1)
    .map(getCellText)
    .filter((value: string) => value && !value.includes('备注'));
  if (headers.length) return headers;
  const firstSource = safeParseJson<{ headers?: string[] }>(recordItems.value[0]?.sourceRowJson);
  return firstSource?.headers?.length ? firstSource.headers : ['内侧10cm厚度', '内侧20cm厚度', '外侧10cm厚度', '外侧20cm厚度'];
}

function getWetSemiWidthLabel(template?: MesHcProcessFormApi.Template, isCoagulation = false) {
  const detailLabel = (template?.items || [])
    .find((item) => item.areaType === 'DETAIL' && item.fieldLabel?.includes('宽'))?.fieldLabel || '';
  const normalized = detailLabel.replace(/^长度\/m\s*/u, '').trim();
  if (normalized) return normalized;
  return isCoagulation ? '出槽宽幅/m' : '宽幅/m';
}

function buildWetSemiRecordItems(template: MesHcProcessFormApi.Template) {
  const sheet = getPrimarySheet(template);
  const rows = Array.isArray(sheet?.nonEmptyRows) ? sheet.nonEmptyRows : [];
  const headerRow = findWetSemiHeaderRow(template);
  const headerRowNo = Number(headerRow?.row || 0);
  const headers = extractWetSemiThicknessHeaders(template);
  const lengthRows = rows
    .filter((row: any) => Number(row.row || 0) > headerRowNo)
    .map((row: any) => ({
      length: getCellText((row.cells || []).find((cell: any) => cell.col === 1)),
      rowNo: Number(row.row || 0),
    }))
    .filter((row: any) => isNumericText(row.length));
  if (!lengthRows.length) return [];
  const category = template.formType === 'COAGULATION_SEMI_RECORD' ? '凝固半成品' : '烘箱半成品';
  return lengthRows.map((row: any, index: number) => {
    const source = {
      headers,
      length: row.length,
      mode: 'wet-semi',
      rowNo: row.rowNo,
      values: headers.map(() => ''),
    };
    return {
      abnormalRemark: '',
      actualValue: '',
      actualValue2: '',
      controlType: 'INPUT',
      fieldKey: `wet_semi_${template.id || 'template'}_${index + 1}`,
      fieldLabel: row.length,
      itemCategory: category,
      itemSeq: index + 1,
      resultFlag: 'OK',
      sourceRowJson: JSON.stringify(source),
      standardText: headers.join(' | '),
      stepNode: '长度/m',
      valueMode: 'TEXT',
    } as MesHcProcessFormApi.RecordItem;
  });
}

function buildRecordItemsFromTemplate(template: MesHcProcessFormApi.Template) {
  if (props.processCode === 'WET' && isWetSemiFormType(template.formType)) {
    const semiRows = buildWetSemiRecordItems(template);
    if (semiRows.length) return semiRows;
  }
  return (template.items || []).filter((item) => item.areaType === 'DETAIL').map((item) => ({
    actualValue: item.defaultValue || '',
    abnormalRemark: '',
    controlType: item.controlType,
    fieldKey: item.fieldKey,
    fieldLabel: item.fieldLabel,
    itemCategory: item.itemCategory,
    itemSeq: item.itemSeq,
    resultFlag: item.defaultResult || 'OK',
    sourceRowJson: item.sourceRowJson,
    standardText: item.standardText,
    stepNode: item.stepNode,
    templateItemId: item.id,
    unit: item.unit,
    valueMode: item.valueMode,
  }));
}

function ensureWetSemiHeaderData() {
  if (!(props.processCode === 'WET' && isWetSemiFormType(currentRecord.formType))) return;
  headerData.productionDate ||= currentRecord.recordDate || '';
  headerData.modelCode ||= currentRecord.modelName || currentRecord.modelCode || '';
  headerData.batchNo ||= currentRecord.batchNo || '';
  const lengthValues = recordItems.value.map((item) => Number(item.fieldLabel || 0)).filter((value) => !Number.isNaN(value));
  headerData.generatedLength ||= lengthValues.length ? String(Math.max(...lengthValues)) : '';
  headerData.semiWidth ||= '';
  if (currentRecord.formType === 'COAGULATION_SEMI_RECORD') {
    headerData.poreDevelopment ||= 'OK';
  }
  headerData.finalResult ||= 'OK';
}

function getWetSemiSource(row: MesHcProcessFormApi.RecordItem) {
  const parsed = safeParseJson<{ headers?: string[]; length?: string; values?: string[] }>(row.sourceRowJson);
  if (parsed?.values) return parsed;
  return {
    headers: wetSemiThicknessHeaders.value,
    length: row.fieldLabel || '',
    values: wetSemiThicknessHeaders.value.map((_, index) => (index === 0 ? row.actualValue || '' : index === 1 ? row.actualValue2 || '' : '')),
  };
}

function getWetSemiCellValue(row: MesHcProcessFormApi.RecordItem, index: number) {
  return getWetSemiSource(row).values?.[index] || '';
}

function setWetSemiCellValue(row: MesHcProcessFormApi.RecordItem, index: number, value: string) {
  const source = getWetSemiSource(row);
  const headers = source.headers?.length ? source.headers : wetSemiThicknessHeaders.value;
  const values = [...(source.values || [])];
  while (values.length < headers.length) values.push('');
  values[index] = value || '';
  row.actualValue = values[0] || '';
  row.actualValue2 = values[1] || '';
  row.sourceRowJson = JSON.stringify({
    ...source,
    headers,
    length: source.length || row.fieldLabel || '',
    mode: 'wet-semi',
    values,
  });
}

async function loadOptions() {
  templateOptions.value = (await getStationFormSimpleList(props.processCode)).map(stationFormToTemplateOption);
}

function openCreate() {
  Object.assign(chooser, {
    batchNo: '',
    equipmentName: '',
    formType: undefined,
    modelCode: undefined,
    planNo: '',
    recordDate: new Date().toISOString().slice(0, 10),
    templateId: undefined,
  });
  templateKeyword.value = '';
  chooseVisible.value = true;
}

async function openEditorFromChooser() {
  if (!chooser.templateId) {
    message.warning('请先选择要填写的表单');
    return;
  }
  const template = stationFormToTemplate(
    await getStationFormDetail(chooser.templateId),
    selectedTemplateOption.value,
  );
  const resolvedModelCode =
    template.modelCode === 'COMMON' && chooser.modelCode && chooser.modelCode !== 'COMMON'
      ? chooser.modelCode
      : template.modelCode || chooser.modelCode;
  const resolvedModelName =
    resolvedModelCode === template.modelCode
      ? template.modelName
      : modelOptions.value.find((item) => item.value === resolvedModelCode)?.label;
  currentTemplate.value = template;
  Object.keys(currentRecord).forEach((key) => delete (currentRecord as any)[key]);
  Object.assign(currentRecord, {
    batchNo: chooser.batchNo,
    equipmentName: chooser.equipmentName,
    formType: template.formType,
    formTypeName: template.formTypeName || formTypeNameOf(template.formType),
    headerDataJson: '{}',
    modelCode: resolvedModelCode,
    modelName: resolvedModelName || resolvedModelCode,
    planNo: chooser.planNo,
    processCode: props.processCode,
    processName: props.processName,
    recordDate: chooser.recordDate,
    recordStatus: 'DRAFT',
    templateId: template.id,
    versionId: template.currentVersion?.id,
  });
  resetHeaderData(template.items || []);
  recordItems.value = buildRecordItemsFromTemplate(template);
  ensureWetSemiHeaderData();
  editorMode.value = 'edit';
  chooseVisible.value = false;
  editorVisible.value = true;
}

async function openRecord(row: MesHcProcessFormApi.Record, mode: 'edit' | 'view') {
  const detail = await getProcessFormRecordDetail(row.id!);
  currentTemplate.value = detail.template;
  Object.keys(currentRecord).forEach((key) => delete (currentRecord as any)[key]);
  Object.assign(currentRecord, detail);
  resetHeaderData(detail.template?.items || [], detail.headerDataJson);
  recordItems.value = detail.items || [];
  if (
    detail.template &&
    props.processCode === 'WET' &&
    isWetSemiFormType(detail.formType) &&
    recordItems.value.length <= 1
  ) {
    recordItems.value = buildWetSemiRecordItems(detail.template);
  }
  ensureWetSemiHeaderData();
  editorMode.value = mode;
  editorVisible.value = true;
}

function buildPayload(): MesHcProcessFormApi.Record {
  const payload = { ...currentRecord } as MesHcProcessFormApi.Record;
  delete payload.template;
  payload.headerDataJson = JSON.stringify(headerData);
  return {
    ...payload,
    items: recordItems.value,
    processCode: props.processCode,
    processName: props.processName,
  };
}

function buildNowText() {
  const now = new Date();
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

async function saveRecord(silent = false) {
  const payload = buildPayload();
  if (payload.id) {
    await updateProcessFormRecord(payload);
  } else {
    payload.id = await createProcessFormRecord(payload);
    currentRecord.id = payload.id;
  }
  if (!silent) {
    message.success('保存成功');
  }
  await gridApi.query();
  return payload.id!;
}

async function saveAndClose() {
  await saveRecord();
  editorVisible.value = false;
}

async function submitCurrentRecord() {
  const id = await saveRecord();
  await submitProcessFormRecord(id);
  message.success('提交成功');
  editorVisible.value = false;
  await gridApi.query();
}

async function confirmCurrentRecord() {
  const id = currentRecord.id || (await saveRecord());
  await confirmProcessFormRecord(id);
  message.success('确认成功');
  editorVisible.value = false;
  await gridApi.query();
}

function openConfirmAuth() {
  if (editorMode.value === 'view') return;
  confirmAuthVisible.value = true;
}

async function handleConfirmAuthSuccess(userInfo: any) {
  const confirmer = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || '';
  const now = buildNowText();
  if (confirmer) {
    (currentRecord as any).confirmUserName = confirmer;
  }
  (currentRecord as any).confirmUserId = userInfo?.userId ?? (currentRecord as any).confirmUserId;
  (currentRecord as any).confirmTime = now;
  confirmAuthVisible.value = false;
  await confirmCurrentRecord();
}

async function deleteRecord(row: MesHcProcessFormApi.Record) {
  await deleteProcessFormRecord(row.id!);
  message.success('删除成功');
  await gridApi.query();
}

onMounted(async () => {
  if (!props.viewerOnly) await loadOptions();
});
defineExpose({ openRecord });
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height>
    <Grid v-if="!props.viewerOnly" :table-title="`${props.processName}表单填写记录`">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增填写',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: openCreate,
            },
          ]"
        />
      </template>
      <template #recordStatus="{ row }">
        <Tag :color="statusColor(row.recordStatus)">
          {{ PROCESS_FORM_RECORD_STATUS_OPTIONS.find((item) => item.value === row.recordStatus)?.label || row.recordStatus }}
        </Tag>
      </template>
      <template #resultStatus="{ row }">
        <Tag :color="resultColor(row.resultStatus)">{{ row.resultStatus || '-' }}</Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '查看', type: 'link', icon: ACTION_ICON.VIEW, onClick: openRecord.bind(null, row, 'view') },
            {
              label: '修改',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              disabled: row.recordStatus === 'CONFIRMED',
              onClick: openRecord.bind(null, row, 'edit'),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              disabled: row.recordStatus === 'CONFIRMED',
              popConfirm: { title: '确认删除当前填写记录吗？', confirm: deleteRecord.bind(null, row) },
            },
          ]"
        />
      </template>
    </Grid>

    <Modal v-model:open="chooseVisible" :footer="null" title="选择填写表单" width="860px" wrap-class-name="hc-process-form-choose-modal">
      <div class="choose-toolbar">
        <label class="choose-filter">
          <span>型号</span>
          <Select
            v-model:value="chooser.modelCode"
            allow-clear
            show-search
            option-filter-prop="label"
            :options="modelOptions"
            @change="chooser.templateId = undefined"
          />
        </label>
        <label class="choose-filter">
          <span>类型</span>
          <Select
            v-model:value="chooser.formType"
            allow-clear
            :options="availableFormTypeOptions"
            @change="chooser.templateId = undefined"
          />
        </label>
        <label class="choose-filter choose-filter--keyword">
          <span>关键词</span>
          <Input v-model:value="templateKeyword" allow-clear placeholder="输入表单名称、模板编码、型号或类型快速过滤" />
        </label>
      </div>
      <div class="template-pick-panel">
        <div class="template-pick-panel__header">
          <span>可填写表单</span>
          <em>共 {{ filteredTemplateOptions.length }} 张</em>
        </div>
        <div class="template-list">
          <div class="template-list__head">
            <span>类型</span>
            <span>型号</span>
            <span>表单名称</span>
            <span>模板编码</span>
          </div>
          <button
            v-for="item in filteredTemplateOptions"
            :key="item.id"
            type="button"
            class="template-row"
            :class="{ 'template-row--active': chooser.templateId === item.id }"
            @click="chooser.templateId = item.id"
            @dblclick="openEditorFromChooser"
          >
            <span class="template-row__type">{{ item.formTypeName || formTypeNameOf(item.formType) || '表单' }}</span>
            <span>{{ item.modelName || item.modelCode || '通用型号' }}</span>
            <strong>{{ item.templateName || item.templateCode || '-' }}</strong>
            <small>{{ item.templateCode || '-' }}</small>
          </button>
          <div v-if="!filteredTemplateOptions.length" class="template-empty">
            当前筛选条件下没有可填写表单
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <span class="template-selected">已选：{{ selectedTemplateOption?.templateName || selectedTemplateOption?.templateCode || '-' }}</span>
        <Button @click="chooseVisible = false">取消</Button>
        <Button type="primary" @click="openEditorFromChooser">开始填写</Button>
      </div>
    </Modal>

    <Modal
      v-model:open="editorVisible"
      :footer="null"
      :title="null"
      width="100vw"
      wrap-class-name="hc-process-form-fill-modal"
    >
      <StationFormRuntimeRenderer
        v-if="props.viewerOnly && readonlyRuntimeSchema?.runtimeLayout"
        :schema="readonlyRuntimeSchema"
        :header-data="readonlyRuntimeHeader"
        :readonly="true"
        :form-name="currentRecord.templateName"
      >
        <template #actions>
          <Button @click="editorVisible = false">关闭</Button>
        </template>
      </StationFormRuntimeRenderer>
      <div v-else class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title">
            <span class="pp-plan-toolbar__main">{{ editorTitle }}</span>
            <div v-if="detailVisualMode === 'formula-production-check'" class="toolbar-meta">
              <span>计划号：{{ currentRecord.planNo || '-' }}</span>
              <span>当前工序：{{ props.processName }}</span>
              <span>执行时机：{{ currentRecord.formTypeName || formTypeNameOf(currentRecord.formType) || '-' }}</span>
            </div>
            <div v-else class="toolbar-meta">
              <span>工序：{{ props.processName }}</span>
              <span>型号：{{ currentRecord.modelName || currentRecord.modelCode || '-' }}</span>
              <span>类型：{{ currentRecord.formTypeName || formTypeNameOf(currentRecord.formType) }}</span>
              <span>状态：{{ currentRecord.recordStatus || 'DRAFT' }}</span>
              <span>Excel：{{ currentTemplate?.currentVersion?.sourceFileName || '-' }}</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <input
              ref="excelImportInputRef"
              accept=".xlsx,.xls"
              class="excel-import-input"
              type="file"
              @change="handleExcelImportChange"
            />
            <Button size="small" :loading="excelLoading" @click="handleExportExcel">导出Excel</Button>
            <Button v-if="editorMode !== 'view'" size="small" :loading="excelLoading" @click="triggerImportExcel">导入Excel</Button>
            <Button v-if="editorMode !== 'view'" size="small" type="primary" @click="saveAndClose">保存</Button>
            <Button v-if="editorMode !== 'view'" size="small" type="primary" @click="submitCurrentRecord">提交</Button>
            <Button v-if="editorMode !== 'view'" size="small" danger @click="openConfirmAuth">确认</Button>
            <Button size="small" @click="editorVisible = false">关闭</Button>
          </div>
        </div>

        <div class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div v-if="detailVisualMode === 'wet-semi'" :class="wetSemiHeaderClass">
              <div v-for="field in wetSemiHeadFields" :key="field.key" class="production-check-head-cell">
                <span class="production-check-head-cell__label">{{ field.label }}</span>
                <div
                  v-if="field.type === 'input' && editorMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <Input v-model:value="headerData[field.key]" class="production-check-head-cell__input" size="small" />
                </div>
                <div
                  v-else-if="field.type === 'radio' && editorMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <RadioGroup v-model:value="headerData[field.key]" size="small" class="pp-radio-group">
                    <Radio value="OK">OK</Radio>
                    <Radio value="NG">NG</Radio>
                  </RadioGroup>
                </div>
                <span v-else class="production-check-head-cell__value">
                  {{ headerData[field.key] || field.value || '-' }}
                </span>
              </div>
            </div>
            <div v-else-if="detailVisualMode === 'formula-production-check'" class="pp-form-grid pp-form-grid--formula-pass-work">
              <div v-for="item in formulaProductionHeadFields" :key="item.field" class="head-item">
                <span class="head-item__label">{{ item.label }}</span>
                <span class="head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-else class="pp-form-grid">
              <div class="head-item">
                <span class="head-item__label">型号：</span>
                <span class="head-item__value">{{ currentRecord.modelName || currentRecord.modelCode || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">填写日期：</span>
                <DatePicker v-if="editorMode !== 'view'" v-model:value="currentRecord.recordDate" value-format="YYYY-MM-DD" size="small" />
                <span v-else class="head-item__value">{{ currentRecord.recordDate || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">计划：</span>
                <Input v-if="editorMode !== 'view'" v-model:value="currentRecord.planNo" size="small" />
                <span v-else class="head-item__value">{{ currentRecord.planNo || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">批次：</span>
                <Input v-if="editorMode !== 'view'" v-model:value="currentRecord.batchNo" size="small" />
                <span v-else class="head-item__value">{{ currentRecord.batchNo || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">设备：</span>
                <Input v-if="editorMode !== 'view'" v-model:value="currentRecord.equipmentName" size="small" />
                <span v-else class="head-item__value">{{ currentRecord.equipmentName || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">填写人：</span>
                <span class="head-item__value">{{ currentRecord.fillUserName || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">确认人：</span>
                <span class="head-item__value">{{ currentRecord.confirmUserName || '-' }}</span>
              </div>
              <div
                v-for="item in templateHeaderItems"
                :key="item.id || item.fieldKey"
                class="head-item"
              >
                <span class="head-item__label">{{ item.fieldLabel }}：</span>
                <Input
                  v-if="editorMode !== 'view'"
                  v-model:value="headerData[headerItemKey(item)]"
                  size="small"
                />
                <span v-else class="head-item__value">{{ getHeaderValue(item) }}</span>
              </div>
            </div>
            <div v-if="excelAttachments.length" class="excel-attachment-line">
              <span>导入附件：</span>
              <a
                v-for="(file, index) in excelAttachments"
                :key="`${file.name || 'excel'}-${index}`"
                :href="file.url || file.path"
                rel="noreferrer"
                target="_blank"
              >
                {{ file.name || file.url || file.path || 'Excel附件' }}
              </a>
            </div>
          </fieldset>

          <div class="pp-panel">
            <div class="pp-panel__header">
              <span>{{ detailVisualTitle }}</span>
              <em>共 {{ recordItems.length }} 项</em>
            </div>
            <div class="pp-table-wrap">
              <table v-if="detailVisualMode === 'wet-semi'" class="pp-grid pp-grid--wet-semi">
                <thead>
                  <tr>
                    <th width="92">长度/m</th>
                    <th v-for="header in wetSemiThicknessHeaders" :key="header" width="180">
                      {{ header }}
                    </th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.fieldKey || row.id || index}`">
                    <td align="center">{{ row.fieldLabel || '-' }}</td>
                    <td v-for="(header, cellIndex) in wetSemiThicknessHeaders" :key="`${row.fieldKey}-${header}`">
                      <Input
                        v-if="editorMode !== 'view'"
                        :value="getWetSemiCellValue(row, cellIndex)"
                        size="small"
                        @update:value="(value) => setWetSemiCellValue(row, cellIndex, value)"
                      />
                      <span v-else>{{ getWetSemiCellValue(row, cellIndex) || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td :colspan="wetSemiThicknessHeaders.length + 2" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'check' && currentRecord.formType !== 'CLEANING_CHECK'" class="pp-grid pp-grid--startup-check">
                <caption v-if="isStartupCleaningMaintenanceRecord" class="pp-check-tip">
                  {{ CHECK_RESULT_REMARK_TIP }}
                </caption>
                <thead>
                  <tr>
                    <th width="80">序号</th>
                    <th width="260">点检项目</th>
                    <th width="420">标准</th>
                    <th v-if="isStartupCleaningMaintenanceRecord" width="120">OK/NG</th>
                    <th v-else width="220">实际/记录</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td align="center">{{ row.itemSeq || index + 1 }}</td>
                    <td>{{ row.fieldLabel || '-' }}</td>
                    <td>{{ getStandardText(row) }}</td>
                    <td>
                      <RadioGroup
                        v-if="editorMode !== 'view' && isStartupCleaningMaintenanceRecord"
                        :value="recordItemResult(row)"
                        class="pp-radio-group"
                        size="small"
                        @update:value="(value) => updateRecordItemResult(row, value)"
                      >
                        <Radio value="OK">OK</Radio>
                        <Radio value="NG">NG</Radio>
                      </RadioGroup>
                      <span v-else-if="isStartupCleaningMaintenanceRecord">{{ recordItemResult(row) || '-' }}</span>
                      <Input
                        v-else-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="getActualPlaceholder(row)"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="5" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'check'" class="pp-grid pp-grid--check">
                <caption v-if="isStartupCleaningMaintenanceRecord" class="pp-check-tip">
                  {{ CHECK_RESULT_REMARK_TIP }}
                </caption>
                <thead>
                  <tr>
                    <th width="120">工序</th>
                    <th width="260">点检项目</th>
                    <th width="420">检查标准</th>
                    <th v-if="isStartupCleaningMaintenanceRecord" width="120">OK/NG</th>
                    <th v-else width="220">实际/记录</th>
                    <th width="260">异常备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td
                      v-if="getFieldRowSpan(index, ['itemCategory'])"
                      align="center"
                      class="pp-group-cell"
                      :rowspan="getFieldRowSpan(index, ['itemCategory'])"
                    >
                      {{ row.itemCategory || row.stepNode || '-' }}
                    </td>
                    <td>{{ row.fieldLabel || '-' }}</td>
                    <td>{{ getStandardText(row) }}</td>
                    <td>
                      <RadioGroup
                        v-if="editorMode !== 'view' && isStartupCleaningMaintenanceRecord"
                        :value="recordItemResult(row)"
                        class="pp-radio-group"
                        size="small"
                        @update:value="(value) => updateRecordItemResult(row, value)"
                      >
                        <Radio value="OK">OK</Radio>
                        <Radio value="NG">NG</Radio>
                      </RadioGroup>
                      <span v-else-if="isStartupCleaningMaintenanceRecord">{{ recordItemResult(row) || '-' }}</span>
                      <Input
                        v-else-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="getActualPlaceholder(row)"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="5" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'production-check'" class="pp-grid pp-grid--production-check-plain">
                <thead>
                  <tr>
                    <th width="120">物料/生产环节</th>
                    <th width="120">确认节点</th>
                    <th width="220">点检项目</th>
                    <th width="180">点检标准</th>
                    <th width="180">实际/记录</th>
                    <th width="220">异常备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td
                      v-if="getResolvedProductionRowSpan(index, 'category') > 0"
                      :rowspan="getResolvedProductionRowSpan(index, 'category')"
                    >
                      {{ resolveProductionGroup(row).category || '-' }}
                    </td>
                    <td
                      v-if="getResolvedProductionRowSpan(index, 'node') > 0"
                      :rowspan="getResolvedProductionRowSpan(index, 'node')"
                    >
                      {{ resolveProductionGroup(row).node || '-' }}
                    </td>
                    <td>{{ row.fieldLabel || '-' }}</td>
                    <td>{{ getStandardText(row) }}</td>
                    <td>
                      <Input
                        v-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="row.fieldLabel ? '请填写记录值' : ''"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="6" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'formula-production-check'" class="pp-grid pp-grid--production-check">
                <thead>
                  <tr>
                    <th width="70">序号</th>
                    <th width="110">项目类别</th>
                    <th width="110">步骤节点</th>
                    <th width="220">点检项目</th>
                    <th width="260">标准</th>
                    <th width="280">记录值</th>
                    <th width="280">异常说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in productionDetailRows"
                    :key="`${row.sourceRowId}-${row.actualField}`"
                    :class="{ 'production-row--step-start': isProductionStepStart(index) }"
                  >
                    <td
                      v-if="getProductionSourceRowSpan(index) > 0"
                      align="center"
                      :rowspan="getProductionSourceRowSpan(index)"
                    >
                      {{ row.id || row.sourceIndex + 1 }}
                    </td>
                    <td
                      v-if="getProductionFieldRowSpan(index, 'category') > 0"
                      :rowspan="getProductionFieldRowSpan(index, 'category')"
                    >
                      {{ row.category || '-' }}
                    </td>
                    <td
                      v-if="getProductionFieldRowSpan(index, 'node') > 0"
                      :rowspan="getProductionFieldRowSpan(index, 'node')"
                    >
                      {{ row.node || '-' }}
                    </td>
                    <td
                      v-if="getProductionFieldRowSpan(index, 'item') > 0"
                      :rowspan="getProductionFieldRowSpan(index, 'item')"
                    >
                      {{ row.item || '-' }}
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">
                      {{ row.standard || '-' }}
                    </td>
                    <td :class="['production-record-cell', { 'production-record-cell--time': isProductionTimeValue(row.source) }]">
                      <span v-if="row.recordLabel" class="production-record-label">{{ row.recordLabel }}</span>
                      <Input
                        v-if="editorMode !== 'view'"
                        v-model:value="row.source[row.actualField]"
                        :placeholder="getProductionInputPlaceholder(row.source, row.actualField)"
                        size="small"
                      />
                      <span v-else>{{ row.source[row.actualField] || '-' }}</span>
                    </td>
                    <td v-if="getProductionSourceRowSpan(index) > 0" :rowspan="getProductionSourceRowSpan(index)">
                      <Input v-if="editorMode !== 'view'" v-model:value="row.source.abnormalRemark" size="small" />
                      <span v-else>{{ row.source.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="7" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'param'" class="pp-grid pp-grid--param">
                <thead>
                  <tr>
                    <th width="130">物料/生产环节</th>
                    <th width="280">工艺参数项目</th>
                    <th width="320">标准</th>
                    <th width="240">实测值</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td
                      v-if="getFieldRowSpan(index, ['itemCategory'])"
                      align="center"
                      class="pp-group-cell"
                      :rowspan="getFieldRowSpan(index, ['itemCategory'])"
                    >
                      {{ row.itemCategory || '-' }}
                    </td>
                    <td>{{ row.fieldLabel || row.stepNode || '-' }}</td>
                    <td class="pp-standard-cell">{{ getStandardText(row) }}</td>
                    <td>
                      <Input
                        v-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="getActualPlaceholder(row)"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="5" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else-if="detailVisualMode === 'semi'" class="pp-grid pp-grid--semi">
                <thead>
                  <tr>
                    <th width="120">分类</th>
                    <th width="150">节点/位置</th>
                    <th width="260">记录项目</th>
                    <th width="320">标准/说明</th>
                    <th width="220">实际值</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td
                      v-if="getFieldRowSpan(index, ['itemCategory'])"
                      align="center"
                      class="pp-group-cell"
                      :rowspan="getFieldRowSpan(index, ['itemCategory'])"
                    >
                      {{ row.itemCategory || '-' }}
                    </td>
                    <td
                      v-if="getFieldRowSpan(index, ['itemCategory', 'stepNode'])"
                      align="center"
                      :rowspan="getFieldRowSpan(index, ['itemCategory', 'stepNode'])"
                    >
                      {{ row.stepNode || '-' }}
                    </td>
                    <td>{{ row.fieldLabel || '-' }}</td>
                    <td>{{ getStandardText(row) }}</td>
                    <td>
                      <Input
                        v-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="getActualPlaceholder(row)"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="6" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>

              <table v-else class="pp-grid pp-grid--record">
                <thead>
                  <tr>
                    <th width="72">序号</th>
                    <th width="150">分类</th>
                    <th width="170">节点</th>
                    <th width="280">记录项目</th>
                    <th width="360">标准/说明</th>
                    <th width="220">实际/记录</th>
                    <th width="260">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in recordItems" :key="`${row.templateItemId || row.id || index}`">
                    <td align="center">{{ row.itemSeq || index + 1 }}</td>
                    <td>{{ row.itemCategory || '-' }}</td>
                    <td>{{ row.stepNode || '-' }}</td>
                    <td>{{ row.fieldLabel || '-' }}</td>
                    <td>{{ getStandardText(row) }}</td>
                    <td>
                      <Input
                        v-if="editorMode !== 'view'"
                        v-model:value="row.actualValue"
                        :placeholder="getActualPlaceholder(row)"
                        size="small"
                      />
                      <span v-else>{{ row.actualValue || '-' }}</span>
                    </td>
                    <td>
                      <Input v-if="editorMode !== 'view'" v-model:value="row.abnormalRemark" size="small" />
                      <span v-else>{{ row.abnormalRemark || '-' }}</span>
                    </td>
                  </tr>
                  <tr v-if="!recordItems.length">
                    <td colspan="7" align="center" class="pp-empty-cell">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="templateFooterItems.length" class="pp-footer-notes">
            <div v-for="note in templateFooterItems" :key="note.id || note.fieldKey" class="pp-footer-note">
              {{ note.fieldLabel }}
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <AuthModal
      v-model:visible="confirmAuthVisible"
      :action-name="confirmAuthActionName"
      auth-mode="username"
      @success="handleConfirmAuthSuccess"
    />
  </component>
</template>

<style scoped>
.choose-toolbar {
  display: grid;
  grid-template-columns: 150px 170px minmax(0, 1fr);
  gap: 8px;
  align-items: end;
  margin-bottom: 8px;
}

.choose-filter {
  display: grid;
  gap: 4px;
  min-width: 0;
  color: #334155;
  font-size: 12px;
}

.choose-filter span {
  line-height: 18px;
}

.choose-filter :deep(.ant-select-selector),
.choose-filter :deep(.ant-input) {
  height: 30px !important;
  min-height: 30px !important;
  border-radius: 2px !important;
}

.choose-filter :deep(.ant-select-selection-search-input),
.choose-filter :deep(.ant-select-selection-item),
.choose-filter :deep(.ant-select-selection-placeholder) {
  line-height: 28px !important;
}

:global(.hc-process-form-choose-modal .ant-modal-content) {
  padding: 14px 16px 12px;
  border-radius: 4px;
}

:global(.hc-process-form-choose-modal .ant-modal-header) {
  margin-bottom: 8px;
}

:global(.hc-process-form-choose-modal .ant-modal-title) {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.modal-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}

.template-selected {
  min-width: 0;
  margin-right: auto;
  overflow: hidden;
  color: #475569;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-pick-panel {
  overflow: hidden;
  border: 1px solid #d6dee9;
  border-radius: 2px;
}

.template-pick-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  color: #172033;
  font-weight: 700;
  background: #f1f5f9;
  border-bottom: 1px solid #d6dee9;
}

.template-pick-panel__header em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.template-list {
  display: grid;
  max-height: 430px;
  overflow: auto;
  background: #f8fafc;
}

.template-list__head,
.template-row {
  display: grid;
  grid-template-columns: 110px 120px minmax(0, 1fr) 210px;
  align-items: center;
  min-width: 760px;
}

.template-list__head {
  position: sticky;
  top: 0;
  z-index: 1;
  height: 34px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  background: #e9eef5;
  border-bottom: 1px solid #cbd5e1;
}

.template-list__head span,
.template-row span,
.template-row strong,
.template-row small {
  min-width: 0;
  padding: 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-row {
  width: 100%;
  height: 34px;
  color: #172033;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 0;
  border-bottom: 1px solid #e5e7eb;
}

.template-row:hover {
  background: #eff6ff;
}

.template-row--active {
  background: #dbeafe;
  box-shadow: inset 3px 0 0 #1677ff;
}

.template-row__type {
  color: #075985;
  font-weight: 700;
}

.template-row strong {
  font-size: 13px;
}

.template-row small {
  color: #64748b;
}

.template-empty {
  grid-column: 1 / -1;
  padding: 40px 12px;
  color: #64748b;
  text-align: center;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

:global(.hc-process-form-fill-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.hc-process-form-fill-modal .ant-modal-content) {
  min-height: 100vh;
  padding: 0;
  background: #eef3f8;
  border-radius: 0;
}

.pp-plan-modal {
  min-height: 100vh;
  color: #172033;
  background: #eef3f8;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 10px 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #d7dee7 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-plan-toolbar__main {
  display: block;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
  line-height: 24px;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
}

.pp-plan-toolbar__actions {
  display: inline-flex;
  gap: 8px;
}

.excel-import-input {
  display: none;
}

.pp-plan-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  overflow: auto;
}

.pp-fieldset {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.pp-fieldset legend {
  padding: 0 8px;
  color: #0f172a;
  font-weight: 800;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-grid--formula-pass-work {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.head-item {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 4px 8px;
  background: #fff;
  border: 1px solid #d7dee8;
}

.head-item__label {
  flex: 0 0 auto;
  color: #64748b;
}

.head-item__value {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.excel-attachment-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
}

.excel-attachment-line a {
  color: #1677ff;
}

.production-check-head-grid {
  display: grid;
  background: #fff;
  border: 1px solid #d7dee8;
  border-right: 0;
  border-bottom: 0;
}

.production-check-head-grid--cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.production-check-head-grid--cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.production-check-head-cell {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 38px;
  background: #fff;
  border-right: 1px solid #d7dee8;
  border-bottom: 1px solid #d7dee8;
}

.production-check-head-cell__label {
  display: flex;
  flex: 0 0 122px;
  align-self: stretch;
  align-items: center;
  padding: 0 8px;
  color: #334155;
  background: #f3f6fa;
  border-right: 1px solid #d7dee8;
}

.production-check-head-cell__value {
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 0 8px;
  overflow: hidden;
  color: #172033;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.production-check-head-cell__input-wrap {
  padding: 2px 6px !important;
}

.production-check-head-cell__input {
  width: 100%;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
:deep(.production-check-head-cell__input.ant-input) {
  height: 28px !important;
  min-height: 28px !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}

.pp-radio-group {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
}

.pp-panel {
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.pp-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-weight: 800;
  background: linear-gradient(180deg, #e5edf5 0%, #d1dbe7 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-panel__header em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.pp-table-wrap {
  max-height: calc(100vh - 245px);
  overflow: auto;
}

.pp-check-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  text-align: left;
  caption-side: top;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.pp-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid--check,
.pp-grid--startup-check,
.pp-grid--param {
  min-width: 1370px;
}

.pp-grid--semi {
  min-width: 1500px;
}

.pp-grid--wet-semi {
  min-width: 1230px;
}

.pp-grid--production-check {
  min-width: 1430px;
}

.pp-grid--production-check-plain {
  min-width: 1240px;
}

.pp-grid--record {
  min-width: 1680px;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 7px 8px;
  color: #172033;
  text-align: center;
  background: linear-gradient(180deg, #e9eef5 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.pp-grid td {
  padding: 6px 8px;
  color: #172033;
  vertical-align: middle;
  background: #fff;
  border: 1px solid #cbd5e1;
}

.pp-group-cell {
  color: #0f172a;
  font-weight: 800;
  background: #f8fafc !important;
}

.pp-standard-cell {
  white-space: pre-wrap;
}

.pp-empty-cell {
  color: #64748b;
}

.production-record-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}

.production-record-label {
  width: 40px;
  flex-shrink: 0;
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
  text-align: right;
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

.pp-footer-notes {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.pp-footer-note {
  word-break: break-all;
}
</style>
