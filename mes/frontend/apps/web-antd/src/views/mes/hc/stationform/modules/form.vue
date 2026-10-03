<script lang="ts" setup>
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, nextTick, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Button,
  Input,
  InputNumber,
  message,
  Radio,
  RadioGroup,
  Select,
  Switch,
  Tabs,
  Tag,
} from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createStationForm,
  exportStationFormPreviewExcel,
  getStationFormDetail,
  importStationFormPreviewExcel,
  updateStationForm,
} from '#/api/mes/hc/stationform';
import { ADHESIVE_INTERMEDIATE_HEADER_FIELDS, ADHESIVE_INTERMEDIATE_NOTES, buildAdhesiveIntermediateFooterNotes, getAdhesiveIntermediateRowCount, isAdhesiveIntermediateTemplate, resolveAdhesiveIntermediateColumns } from '#/views/mes/hc/shared/adhesiveIntermediateLayout';
import { exportProcessFormRecordLayout, importProcessFormRecordLayout } from '#/api/mes/hc/processform';
import { createRoughLayoutCell, toRoughExcelColumns } from '#/views/mes/hc/shared/roughGrindingFormLayout';
import { resolveRoughMiddleProductColumns } from '#/views/mes/hc/shared/roughGrindingFormLayout';
import RoughMiddleProductRecordSheet from '#/views/mes/hc/shared/RoughMiddleProductRecordSheet.vue';

import {
  HEADER_LAYOUT_OPTIONS,
  loadProcessOptions,
  PROCESS_OPTIONS,
  PRESET_TEMPLATE_OPTIONS,
  resolveProcessName,
  TRIGGER_TIMING_NAME_MAP,
  TRIGGER_TIMING_OPTIONS,
  VALUE_MODE_OPTIONS,
} from '../data';
import { multiDefinitionError } from './formula-multi-fields';
import FormulaMultiFieldEditor from './FormulaMultiFieldEditor.vue';
import FormulaSimpleEditor from './FormulaSimpleEditor.vue';
import {
  getFormulaCategory,
  mergeFormulaSchema,
  normalizeFormulaInputModes,
} from './formula-config';
import PressSlotRuntimeLayoutIde from './PressSlotRuntimeLayoutIde.vue';
import { productionCheckRows, productionCheckSchema, isPressSlotProductionCheck } from '#/views/mes/hc/shared/pressSlotProductionCheck';
import StationFormRuntimeRenderer from './StationFormRuntimeRenderer.vue';

const emit = defineEmits(['success']);

type DesignerNodeType = 'form' | 'header' | 'items' | 'item' | 'schema';
type StationFormModelScope = 'COMMON' | 'MODEL' | 'PREFIX';

const MODEL_SCOPE_OPTIONS = [
  { label: '通用', value: 'COMMON' },
  { label: '指定型号', value: 'MODEL' },
  { label: '型号前缀', value: 'PREFIX' },
];
const PRESS_SLOT_INTERMEDIATE_RUNTIME_DEV_FORM_CODE =
  'PRESS_SLOT_INTERMEDIATE_RECORD_11_2_94_DEV';
const RUNTIME_LAYOUT_ENGINE = 'STATION_FORM_RUNTIME_LAYOUT';
const LEGACY_RUNTIME_LAYOUT_DEV_ENGINE = 'STATION_FORM_RUNTIME_LAYOUT_DEV';

const formData = ref<MesHcStationFormApi.StationForm>();
const itemRows = ref<
  Array<MesHcStationFormApi.StationFormItem & { _rowKey: string }>
>([]);
const presetRows = ref<
  Array<MesHcStationFormApi.StationFormItem & { _rowKey: string }>
>([]);
const rowSeed = ref(0);
const activeTab = ref('basic');
const designerMode = ref<'design' | 'preview'>('design');
const guideVisible = ref(false);
const formulaAdvanced = ref(false);
const formulaSimpleValues = ref<MesHcStationFormApi.StationForm>({});
const schemaBaseline = ref<Record<string, unknown>>({});
const formulaHeaderEdited = ref(false);
const isFormulaForm = computed(
  () => String(formData.value?.processCode || '').toUpperCase() === 'FORMULA',
);
const isFormulaStandard = computed(
  () => isFormulaForm.value && !usesRuntimeLayoutEditor(),
);
const isFormulaSimple = computed(
  () => isFormulaStandard.value && !formulaAdvanced.value,
);

async function toggleFormulaAdvanced() {
  if (isFormulaSimple.value) {
    const values = { ...formulaSimpleValues.value };
    formulaAdvanced.value = true;
    designerMode.value = 'design';
    activeTab.value = 'basic';
    await nextTick();
    await formApi.setValues(values);
  } else {
    const values = { ...formData.value, ...(await formApi.getValues()) };
    if (values.processCode !== 'FORMULA') {
      message.info('当前已选择其他工序，请继续使用高级配置。');
      return;
    }
    formulaSimpleValues.value = values;
    formulaAdvanced.value = false;
    designerMode.value = 'design';
  }
}

function updateFormulaItem(
  index: number,
  patch: Partial<MesHcStationFormApi.StationFormItem>,
) {
  Object.assign(itemRows.value[index]!, patch);
}

function appendFormulaItem(sourceIndex?: number) {
  const source =
    sourceIndex === undefined ? undefined : itemRows.value[sourceIndex];
  const itemSeq =
    Math.max(0, ...itemRows.value.map((item) => Number(item.itemSeq) || 0)) + 1;
  itemRows.value.push(
    normalizeRow({
      ...source,
      id: undefined,
      itemSeq,
      ...{ _rowKey: undefined },
    }),
  );
}

const selectedDesignerNode = ref<{
  index?: number;
  key: string;
  type: DesignerNodeType;
}>({
  key: 'form',
  type: 'form',
});
const previewFormValues = ref<MesHcStationFormApi.StationForm>({});
const previewExcelInputRef = ref<HTMLInputElement>();
const previewExcelLoading = ref(false);
const pressSlotIntermediatePreviewPage = ref(1);
const presetHeaderDataText = ref('{}');
const presetHeaderDataMap = ref<Record<string, any>>({});
const headerFieldLabels = ref<Record<string, string>>({});
const headerEditableFields = ref<string[]>([]);
const headerEditableFieldsConfigured = ref(false);
const roughThicknessLabelsEdited = ref<string[] | null>(null);
const isAdhesiveMiddleTemplate = computed(() => isAdhesiveIntermediateTemplate(formData.value));
const adhesiveThicknessLabelsEdited = ref<string[] | null>(null);
const adhesiveThicknessLabels = computed(() => {
  const configured = adhesiveThicknessLabelsEdited.value ?? parseSchemaText(formData.value?.schemaJson).thicknessLabels;
  const legacy = [102, 103].map((seq) => itemRows.value.find((item) => item.itemSeq === seq)?.itemName);
  return resolveAdhesiveIntermediateColumns(configured ?? legacy).slice(1, 3).map((column) => column.title);
});
const adhesivePreviewColumns = computed(() => resolveAdhesiveIntermediateColumns(adhesiveThicknessLabels.value));
const adhesivePreviewRowCount = computed(() => getAdhesiveIntermediateRowCount(Number(presetHeaderDataMap.value.processLength)));
function updateAdhesiveThicknessLabel(index: number, value: string) {
  const labels = [...(adhesiveThicknessLabelsEdited.value ?? adhesiveThicknessLabels.value)];
  labels[index] = value;
  adhesiveThicknessLabelsEdited.value = labels;
}

const isRoughMiddleTemplate = computed(() =>
  formData.value?.processCode === 'ROUGH_GRINDING' &&
  formData.value?.formCode === 'ROUGH_MIDDLE_PRODUCT_RECORD',
);
const roughThicknessLabels = computed(() => resolveRoughMiddleProductColumns(
  roughThicknessLabelsEdited.value ?? parseSchemaText(formData.value?.schemaJson).thicknessLabels,
).filter((column) => column.key === 'innerThickness' || column.key === 'outerThickness').map((column) => column.title));

function updateRoughThicknessLabel(index: number, value: string) {
  const labels = [...(roughThicknessLabelsEdited.value ?? roughThicknessLabels.value)];
  labels[index] = value;
  roughThicknessLabelsEdited.value = labels;
}

const wetThicknessLabelsEdited = ref<string[] | null>(null);
const wetThicknessLabels = computed(() => {
  if (wetThicknessLabelsEdited.value) return wetThicknessLabelsEdited.value;
  const schema = parseSchemaText(formData.value?.schemaJson);
  if (Array.isArray(schema.thicknessLabels) && schema.thicknessLabels.length >= 4) {
    return schema.thicknessLabels.slice(0, 4).map(String);
  }
  const position = formData.value?.formName?.includes('凝固') ? '出槽' : '出箱';
  return ['左侧10cm', '左侧20cm', '右侧10cm', '右侧20cm'].map(
    (label) => `${label}${position}厚度/mm`,
  );
});

function updateWetThicknessLabel(index: number, value: string) {
  const labels = [...wetThicknessLabels.value];
  labels[index] = value;
  wetThicknessLabelsEdited.value = labels;
}
const schemaEditor = ref({
  defaultGeneratedLength: undefined as number | undefined,
  defaultRowCount: undefined as number | undefined,
  headerFields: '',
  headerLayout: 'GRID_4',
  modelCode: '',
  modelPrefix: '',
  modelScope: 'COMMON' as StationFormModelScope,
  presetTemplate: '',
  rowStep: undefined as number | undefined,
  sourceModelCode: '',
  version: '',
});

const WET_PRODUCTION_CHECK_HEADER_LABELS: Record<string, string> = {
  batchNo: '母批批号',
  endTime: '投料结束时间',
  inOvenTime: '入烘箱时间',
  inSolidifyTime: '入凝固槽时间',
  inWashTime: '入水洗槽时间',
  machine: '机台编号',
  materialCode: '产品料号',
  modelCode: '产品型号',
  outOvenTime: '出烘箱时间',
  outSolidifyTime: '出凝固槽时间',
  outWashTime: '出水洗槽时间',
  startTime: '投料开始时间',
};
const WET_PRODUCTION_CHECK_TIME_FIELDS = new Set([
  'endTime',
  'inOvenTime',
  'inSolidifyTime',
  'inWashTime',
  'outOvenTime',
  'outSolidifyTime',
  'outWashTime',
  'startTime',
]);

const PRESET_SCHEMA_MAP: Record<
  string,
  Partial<{
    defaultGeneratedLength: number;
    defaultRowCount: number;
    headerFields: string;
    headerLayout: string;
    rowStep: number;
    version: string;
  }>
> = {
  'wet-cleaning-v1': {
    headerFields: 'machine,recorder,recorderTime,confirmer,confirmerTime',
    headerLayout: 'TOP_FIELDS',
    version: 'wet-cleaning-v1',
  },
  'wet-oven-semi-v1': {
    defaultGeneratedLength: 600,
    defaultRowCount: 300,
    headerFields: 'productionDate,modelCode,batchNo,semiWidth,finalResult',
    headerLayout: 'GRID_3',
    rowStep: 2,
    version: 'wet-oven-semi-v1',
  },
  'wet-process-v1': {
    headerFields:
      'materialCode,modelCode,machine,batchNo,startTime,endTime,inWashTime,outWashTime,inSolidifyTime,outSolidifyTime,inOvenTime,outOvenTime',
    headerLayout: 'GRID_4',
    version: 'wet-process-v1',
  },
  'wet-solid-semi-v1': {
    defaultGeneratedLength: 130,
    defaultRowCount: 65,
    headerFields:
      'productionDate,materialCode,modelCode,machine,batchNo,generatedLength,semiWidth,poreDevelopment,finalResult',
    headerLayout: 'GRID_3',
    rowStep: 2,
    version: 'wet-solid-semi-v20260613',
  },
  'wet-startup-v1': {
    headerFields: 'machine,recorder,recorderTime,confirmer,confirmerTime',
    headerLayout: 'TOP_FIELDS',
    version: 'wet-startup-v1',
  },
  'adhesive1-intermediate-v1': {
    defaultGeneratedLength: 50,
    defaultRowCount: 50,
    headerFields:
      'productionDate,modelCode,materialCode,batchNo,processLength,productWidthMm,widthStart,widthMiddle,widthEnd,recorder,recorderTime,confirmer,confirmerTime',
    headerLayout: 'GRID_3',
    rowStep: 1,
    version: 'adhesive1-intermediate-v1',
  },
};

function isWetProductionCheckHeaderConfig() {
  if (String(formData.value?.processCode || '').toUpperCase() !== 'WET')
    return false;
  const schema = parseSchemaText(formData.value?.schemaJson);
  return (
    schema.wetCategory === 'production-check' ||
    schema.presetTemplate === 'wet-process-v1' ||
    schemaEditor.value.presetTemplate === 'wet-process-v1'
  );
}

function isWetSemiFinishedTemplateConfig() {
  if (String(formData.value?.processCode || '').toUpperCase() !== 'WET')
    return false;
  const schema = parseSchemaText(formData.value?.schemaJson);
  const presetTemplate =
    schema.presetTemplate || schemaEditor.value.presetTemplate;
  return (
    schema.wetCategory === 'semi-finished' ||
    presetTemplate === 'wet-solid-semi-v1' ||
    presetTemplate === 'wet-oven-semi-v1'
  );
}

function isWetSemiFinishedHeaderConfig() {
  return isWetSemiFinishedTemplateConfig();
}

function isWetHeaderCaptionConfig() {
  return isWetProductionCheckHeaderConfig() || isWetSemiFinishedHeaderConfig();
}

function isWetProductionCheckTimeHeader(field: string) {
  return (
    isWetProductionCheckHeaderConfig() &&
    WET_PRODUCTION_CHECK_TIME_FIELDS.has(field)
  );
}

function normalizeHeaderFieldLabels(value: unknown) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {};
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .map(([field, label]) => [field.trim(), String(label || '').trim()])
      .filter(([field, label]) => field && label),
  );
}

function normalizeHeaderEditableFields(value: unknown) {
  if (!Array.isArray(value)) return [];
  return [
    ...new Set(
      value.map((field) => String(field || '').trim()).filter(Boolean),
    ),
  ];
}

function getEffectiveDefaultRowCount() {
  if (!isWetSemiFinishedHeaderConfig()) {
    return schemaEditor.value.defaultRowCount;
  }
  const generatedLength = Number(schemaEditor.value.defaultGeneratedLength);
  const rowStep = Number(schemaEditor.value.rowStep);
  if (!Number.isFinite(generatedLength) || generatedLength <= 0) {
    return schemaEditor.value.defaultRowCount;
  }
  if (!Number.isFinite(rowStep) || rowStep <= 0) {
    return schemaEditor.value.defaultRowCount;
  }
  return Math.ceil(generatedLength / rowStep);
}

const getTitle = computed(() =>
  formData.value?.id ? '编辑动态表单' : '新增动态表单',
);
const effectiveDefaultRowCount = computed(() => getEffectiveDefaultRowCount());
const schemaPreview = computed(() => buildSchemaJson());
const presetHeaderFieldKeys = computed(() =>
  isAdhesiveMiddleTemplate.value ? ADHESIVE_INTERMEDIATE_HEADER_FIELDS : schemaEditor.value.headerFields
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean),
);
const presetHeaderPreview = computed(() =>
  JSON.stringify(presetHeaderDataMap.value, null, 2),
);
const isPressSlotProductionConfig = computed(() => isPressSlotProductionCheck(parseSchemaText(formData.value?.schemaJson), formData.value?.processCode));
const currentSchemaObject = computed(() => {
  const schema = parseSchemaText(schemaPreview.value);
  return isPressSlotProductionCheck(schema, formData.value?.processCode) ? productionCheckSchema(schema) : schema;
});
const previewHeaderFields = computed(() =>
  presetHeaderFieldKeys.value.map((field) => ({
    field,
    label: getPresetHeaderFieldMeta(field).label,
    value: presetHeaderDataMap.value[field] ?? '',
  })),
);
const previewRows = computed(() =>
  itemRows.value.map((row, index) => ({
    ...row,
    itemSeq: row.itemSeq || index + 1,
  })),
);
const previewHeaderClass = computed(() => {
  const layout = schemaEditor.value.headerLayout || 'GRID_4';
  return (
    {
      GRID_2: 'station-form-preview-header--cols-2',
      GRID_3: 'station-form-preview-header--cols-3',
      GRID_4: 'station-form-preview-header--cols-4',
      TOP_FIELDS: 'station-form-preview-header--top',
    }[layout] || 'station-form-preview-header--cols-4'
  );
});
const previewMeta = computed(() => {
  const current = previewFormValues.value || {};
  return [
    {
      label: '表单编码',
      value: current.formCode || formData.value?.formCode || '-',
    },
    {
      label: '业务工序',
      value: current.processName || formData.value?.processName || '-',
    },
    {
      label: '触发时机',
      value:
        current.triggerTimingName || formData.value?.triggerTimingName || '-',
    },
    {
      label: '需确认',
      value: (current.needConfirm ?? formData.value?.needConfirm) ? '是' : '否',
    },
  ];
});
const runtimePreviewMeta = computed(() => {
  const current = previewFormValues.value || formData.value || {};
  const header = presetHeaderDataMap.value || {};
  if (
    String(
      current.processCode || formData.value?.processCode || '',
    ).toUpperCase() === 'FORMULA'
  ) {
    return [
      `计划号：${header.planNo || '-'}`,
      `型号：${header.modelCode || '-'}`,
      `批号：${header.batchNo || header.motherBatchNo || '-'}`,
      `当前工序：${current.processName || formData.value?.processName || '-'}`,
      `执行时机：${current.triggerTimingName || formData.value?.triggerTimingName || '-'}`,
    ];
  }
  return previewMeta.value.map((item) => `${item.label}：${item.value}`);
});
const isPressSlotIntermediatePreview = computed(() => {
  const current = previewFormValues.value || formData.value || {};
  const formCode = String(current.formCode || formData.value?.formCode || '')
    .trim()
    .toUpperCase();
  return formCode === PRESS_SLOT_INTERMEDIATE_RUNTIME_DEV_FORM_CODE;
});
const isRuntimeLayoutDevPreview = computed(() => {
  const schema = currentSchemaObject.value || {};
  if (isPressSlotProductionConfig.value) return false;
  return (
    isPressSlotIntermediatePreview.value ||
    (isRuntimeLayoutEngine(schema.runtimeEngine) &&
      !!schema.runtimeLayout)
  );
});
const selectedItemIndex = computed(() =>
  itemRows.value.findIndex(
    (row) => row._rowKey === selectedDesignerNode.value.key,
  ),
);
const selectedItemRow = computed(() =>
  selectedDesignerNode.value.type === 'item' && selectedItemIndex.value >= 0
    ? itemRows.value[selectedItemIndex.value]
    : undefined,
);
const selectedHeaderFieldKey = computed(() =>
  selectedDesignerNode.value.type === 'header'
    ? selectedDesignerNode.value.key
    : '',
);
const selectedHeaderFieldMeta = computed(() =>
  selectedHeaderFieldKey.value
    ? getPresetHeaderFieldMeta(selectedHeaderFieldKey.value)
    : undefined,
);
const selectedDesignerNodeTitle = computed(() => {
  if (selectedDesignerNode.value.type === 'form') return 'Form1';
  if (selectedDesignerNode.value.type === 'schema') return '表单结构与模板';
  if (selectedDesignerNode.value.type === 'header') {
    return (
      selectedHeaderFieldMeta.value?.label ||
      selectedHeaderFieldKey.value ||
      'HeaderField'
    );
  }
  if (selectedDesignerNode.value.type === 'item') {
    return (
      selectedItemRow.value?.itemName ||
      `DetailItem${selectedItemIndex.value + 1}`
    );
  }
  return 'DetailGrid';
});
const selectedDesignerNodeKind = computed(() => {
  if (selectedDesignerNode.value.type === 'form') return 'TStationForm';
  if (selectedDesignerNode.value.type === 'schema')
    return '数据定义 / 非数据源';
  if (selectedDesignerNode.value.type === 'header') return 'THeaderField';
  if (selectedDesignerNode.value.type === 'item') return 'TDetailItem';
  return 'TDetailGrid';
});
const formPropertyRows = computed(() => {
  const current = previewFormValues.value || formData.value || {};
  return [
    { name: 'formCode', value: current.formCode || '-' },
    { name: 'formName', value: current.formName || '-' },
    {
      name: 'processName',
      value: current.processName || current.processCode || '-',
    },
    {
      name: 'triggerTiming',
      value: current.triggerTimingName || current.triggerTimingCode || '-',
    },
    {
      name: 'needConfirm',
      value:
        (current.needConfirm ?? formData.value?.needConfirm) ? 'True' : 'False',
    },
    { name: 'status', value: current.status === 0 ? '停用' : '启用' },
  ];
});
const schemaPropertyRows = computed(() => [
  { name: '配置性质', value: '表单结构定义（不是数据源）' },
  {
    name: '适用范围',
    value: getModelScopeLabel(schemaEditor.value.modelScope),
  },
  { name: '适用型号', value: getModelScopeValueText() },
  {
    name: '渲染方式',
    value:
      currentSchemaObject.value.pressSlotFormType ||
      currentSchemaObject.value.formCategory ||
      '普通动态表单',
  },
  {
    name: '运行模板',
    value: currentSchemaObject.value.runtimeTemplateCode || '-',
  },
  {
    name: '来源Excel样式',
    value: currentSchemaObject.value.sourceExcel || '-',
  },
  { name: '来源工作表', value: currentSchemaObject.value.sourceSheet || '-' },
  {
    name: '来源型号',
    value:
      schemaEditor.value.sourceModelCode ||
      currentSchemaObject.value.sourceModelCode ||
      '-',
  },
]);
const selectedDesignerGuide = computed(() => {
  if (selectedDesignerNode.value.type === 'form') {
    return {
      example:
        '例：压槽中间品记录表绑定 PRESS_SLOT 工序，触发时机选择报工后，启用需确认。',
      purpose:
        '定义整张动态表单的业务入口，决定它归属哪个工序、在什么时机出现，以及是否需要确认人复核。',
      tips: [
        '表单编码建议稳定且唯一，后续运行端和 Excel 导入导出会引用它。',
        '工序与触发时机要和报工页面实际读取逻辑一致。',
      ],
    };
  }
  if (selectedDesignerNode.value.type === 'schema') {
    return {
      example: isPressSlotIntermediatePreview.value
        ? '例：选择压槽中间品专用模板，适用范围选“指定型号”并填 W36P0100，系统会优先给该型号使用这张记录单。'
        : '例：选择湿法过程记录模板，适用范围选“通用”，表头字段填 materialCode,modelCode,batchNo，表头布局选四列。',
      purpose:
        '这是“表单结构定义”，不是数据源。它决定表单长什么样、适用于哪些产品型号、有哪些表头字段、默认生成多少行、是否引用专用运行模板；真实生产数据仍来自计划、批次、设备、人员和现场填写结果。',
      tips: [
        '通用会写入 modelCode=COMMON，作为没有命中具体型号时的兜底模板。',
        '指定型号会写入 modelCode，例如 W36P0100；型号前缀会写入 modelPrefix，例如 W36P。',
        '表头字段用后台字段编码保存，字段顺序会影响预览和运行端表头顺序。',
        '专用运行模板会按业务页面的固定记录单渲染，不按普通明细项逐行显示。',
      ],
    };
  }
  if (selectedDesignerNode.value.type === 'header') {
    const label =
      selectedHeaderFieldMeta.value?.label ||
      selectedHeaderFieldKey.value ||
      '表头字段';
    return {
      example: `例：${selectedHeaderFieldKey.value || 'batchNo'} = W26F069AQ，用于预览表头和运行端记录单抬头。`,
      purpose: `${label} 是记录单的表头数据，通常来自计划、批次、设备、人员或确认信息，用来帮助现场人员识别本张记录。`,
      tips: [
        '这里只维护预设值或预览值，真实运行时仍可由业务页面按计划数据回填。',
        '数字型字段会使用数字输入，判定类字段优先使用下拉。',
      ],
    };
  }
  if (selectedDesignerNode.value.type === 'item') {
    return {
      example:
        '例：项目名称=厚度，标准说明=1.183±0.045mm，值模式=NUMBER，默认结果留空，必填=是。',
      purpose:
        '定义一行明细检查或记录项目，决定现场填写什么、按什么标准判断，以及保存时是否必须录入。',
      tips: [
        'itemSeq 控制显示顺序，复制或插入后可继续在属性面板微调。',
        '双值模式可用标签1/标签2定义 X/Y、前/后等成对记录。',
      ],
    };
  }
  return {
    example:
      '例：先新增 DetailItem，再逐行配置分类、步骤节点、项目名称、标准说明和值模式。',
    purpose:
      '明细表区域用于批量维护检查项目，是普通动态表单运行时生成填写表格的核心配置。',
    tips: [
      '可以通过插入、复制、上移、下移快速整理表格结构。',
      '专用模板如压槽中间品会引用运行端结构，普通明细仅作为桥接说明。',
    ],
  };
});

const PRESS_SLOT_INTERMEDIATE_DETAIL_PAGE_SIZE = 50;
const PRESS_SLOT_INTERMEDIATE_DEFAULT_POSITIONS = ['前段', '中段', '后段'];
const pressSlotIntermediatePreviewLayout = computed(() =>
  buildPressSlotIntermediatePreviewLayout(),
);

const PRESET_HEADER_FIELD_META: Record<
  string,
  {
    label: string;
    type?: 'number' | 'select' | 'text';
    options?: Array<{ label: string; value: string }>;
  }
> = {
  batchNo: { label: '产品批号' },
  confirmer: { label: '确认人' },
  confirmerTime: { label: '确认时间' },
  finalResult: {
    label: '综合判定',
    options: [
      { label: 'OK', value: 'OK' },
      { label: 'NG', value: 'NG' },
    ],
    type: 'select',
  },
  generatedLength: { label: '半成品长度/m', type: 'number' },
  inOvenTime: { label: '入烘箱时间' },
  inSolidifyTime: { label: '入凝固槽时间' },
  inWashTime: { label: '入水洗槽时间' },
  machine: { label: '机台编号' },
  materialCode: { label: '母料料号' },
  modelCode: { label: '母料型号' },
  outOvenTime: { label: '出烘箱时间' },
  outSolidifyTime: { label: '出凝固槽时间' },
  outWashTime: { label: '出水洗槽时间' },
  poreDevelopment: {
    label: '泡孔发育',
    options: [
      { label: 'OK', value: 'OK' },
      { label: 'NG', value: 'NG' },
    ],
    type: 'select',
  },
  productionDate: { label: '生产日期' },
  recorder: { label: '记录人' },
  recorderTime: { label: '记录时间' },
  semiWidth: { label: '宽幅' },
  startTime: { label: '投料开始时间' },
  endTime: { label: '投料结束时间' },
};

function nextRowKey() {
  rowSeed.value += 1;
  return `station-form-item-${Date.now()}-${rowSeed.value}`;
}

function normalizeRow(
  row?: Partial<MesHcStationFormApi.StationFormItem>,
): MesHcStationFormApi.StationFormItem & { _rowKey: string } {
  return {
    defaultResult: row?.defaultResult || '',
    dualLabel1: row?.dualLabel1 || '',
    dualLabel2: row?.dualLabel2 || '',
    fieldDefinitionsJson: row?.fieldDefinitionsJson,
    id: row?.id,
    itemCategory: row?.itemCategory || '',
    itemName: row?.itemName || '',
    itemSeq: row?.itemSeq ?? itemRows.value.length + 1,
    remark: row?.remark || '',
    requiredFlag: row?.requiredFlag ?? false,
    standardText: row?.standardText || '',
    stepNode: row?.stepNode || '',
    valueMode: row?.valueMode || 'TEXT',
    _rowKey: (row as any)?._rowKey || nextRowKey(),
  };
}

function selectDesignerNode(
  type: DesignerNodeType,
  key = type,
  index?: number,
) {
  selectedDesignerNode.value = { index, key, type };
  if (type === 'form') {
    activeTab.value = 'basic';
    refreshPreviewFormValues();
  } else if (type === 'schema') {
    activeTab.value = 'schema';
  } else if (type === 'header') {
    activeTab.value = 'preset';
  } else if (type === 'items' || type === 'item') {
    activeTab.value = 'items';
  }
}

function resetDesignerNode() {
  selectedDesignerNode.value = { key: 'form', type: 'form' };
  activeTab.value = 'basic';
}

function isDesignerNodeActive(type: DesignerNodeType, key = type) {
  return (
    selectedDesignerNode.value.type === type &&
    selectedDesignerNode.value.key === key
  );
}

function selectItemRow(
  row: MesHcStationFormApi.StationFormItem & { _rowKey: string },
  index: number,
) {
  selectDesignerNode('item', row._rowKey, index);
}

function addItemRow() {
  insertItemRow(itemRows.value.length);
}

function addPresetRow() {
  syncPresetRowsFromItems();
}

function removeItemRow(index: number) {
  const removeKey = itemRows.value[index]?._rowKey;
  itemRows.value.splice(index, 1);
  normalizeItemSeq();
  if (
    selectedDesignerNode.value.type === 'item' &&
    selectedDesignerNode.value.key === removeKey
  ) {
    selectDesignerNode('items');
  }
}

function removePresetRow(index: number) {
  syncPresetRowsFromItems();
}

function insertItemRow(index: number) {
  const targetIndex = Math.max(0, Math.min(index, itemRows.value.length));
  const row = normalizeRow({ itemSeq: targetIndex + 1 });
  itemRows.value.splice(targetIndex, 0, row);
  normalizeItemSeq();
  selectDesignerNode('item', row._rowKey, targetIndex);
}

function copyItemRow(index: number) {
  const source = itemRows.value[index];
  if (!source) return;
  const row = normalizeRow({
    defaultResult: source.defaultResult,
    dualLabel1: source.dualLabel1,
    dualLabel2: source.dualLabel2,
    fieldDefinitionsJson: source.fieldDefinitionsJson,
    itemCategory: source.itemCategory,
    itemName: source.itemName,
    itemSeq: index + 2,
    remark: source.remark,
    requiredFlag: source.requiredFlag,
    standardText: source.standardText,
    stepNode: source.stepNode,
    valueMode: source.valueMode,
  });
  itemRows.value.splice(index + 1, 0, row);
  normalizeItemSeq();
  selectDesignerNode('item', row._rowKey, index + 1);
}

function moveItemRow(index: number, offset: -1 | 1) {
  const nextIndex = index + offset;
  if (nextIndex < 0 || nextIndex >= itemRows.value.length) return;
  const [row] = itemRows.value.splice(index, 1);
  if (!row) return;
  itemRows.value.splice(nextIndex, 0, row);
  normalizeItemSeq();
}

function normalizeItemSeq() {
  itemRows.value.forEach((row, index) => {
    row.itemSeq = index + 1;
  });
}

function syncPresetRowsFromItems() {
  if (isPressSlotProductionConfig.value) {
    presetHeaderDataMap.value.previewDetails = productionCheckRows(itemRows.value, presetHeaderDataMap.value.previewDetails || []);
  }
  presetRows.value = itemRows.value.map((row, index) =>
    normalizeRow({
      ...row,
      itemSeq: row.itemSeq ?? index + 1,
    }),
  );
  if (!presetRows.value.length && !isWetSemiFinishedTemplateConfig()) {
    presetRows.value = [normalizeRow()];
  }
}

function parsePresetHeaderDataMap(source?: string) {
  if (!source) return {};
  try {
    const parsed = JSON.parse(source);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed
      : {};
  } catch {
    return {};
  }
}

function syncPresetHeaderDataMap(source?: string) {
  const parsed = parsePresetHeaderDataMap(source);
  const nextMap: Record<string, any> = { ...parsed };
  presetHeaderFieldKeys.value.forEach((key) => {
    nextMap[key] = parsed[key] ?? presetHeaderDataMap.value[key] ?? '';
  });
  presetHeaderDataMap.value = nextMap;
  presetHeaderDataText.value = JSON.stringify(nextMap, null, 2);
}

function updatePresetHeaderField(field: string, value: any) {
  formulaHeaderEdited.value = true;
  presetHeaderDataMap.value = {
    ...presetHeaderDataMap.value,
    [field]: value,
  };
  presetHeaderDataText.value = JSON.stringify(
    presetHeaderDataMap.value,
    null,
    2,
  );
}

function handlePressSlotRuntimeSchemaUpdate(schema: Record<string, any>) {
  const schemaJson = JSON.stringify(schema || {}, null, 2);
  formData.value = {
    ...formData.value,
    schemaJson,
  };
  syncSchemaEditor(schemaJson);
}

function handlePressSlotRuntimeHeaderDataUpdate(
  headerData: Record<string, any>,
) {
  formulaHeaderEdited.value = true;
  if (isPressSlotProductionConfig.value && Array.isArray(headerData?.previewDetails)) {
    // 设计器修改明细定义时同步模板项目，保持原 ID；记录值仅保留为预设值。
    itemRows.value.forEach((item) => {
      const detail = headerData.previewDetails.find((row: Record<string, any>) => row.templateItemId === item.id);
      if (!item.id || !detail) return;
      for (const key of ['itemName', 'itemCategory', 'standardText', 'valueMode', 'requiredFlag', 'dualLabel1', 'dualLabel2', 'fieldDefinitionsJson'] as const) {
        if (detail[key] !== undefined) Object.assign(item, { [key]: detail[key] });
      }
    });
  }
  presetHeaderDataMap.value = {
    ...(headerData || {}),
  };
  presetHeaderDataText.value = JSON.stringify(
    presetHeaderDataMap.value,
    null,
    2,
  );
}

function getDefaultPresetHeaderFieldMeta(field: string) {
  return (
    PRESET_HEADER_FIELD_META[field] || { label: field, type: 'text' as const }
  );
}

function getPresetHeaderFieldLabel(field: string) {
  const configuredLabel = String(headerFieldLabels.value[field] || '').trim();
  if (configuredLabel) return configuredLabel;
  if (
    isWetProductionCheckHeaderConfig() &&
    WET_PRODUCTION_CHECK_HEADER_LABELS[field]
  ) {
    return WET_PRODUCTION_CHECK_HEADER_LABELS[field]!;
  }
  return getDefaultPresetHeaderFieldMeta(field).label;
}

function getPresetHeaderFieldMeta(field: string) {
  if (isPressSlotProductionConfig.value) {
    const labels: Record<string, string> = { batchNo: '压槽片号', submitTime: '送检时间', inspectionResult: '送检结果', planNo: '计划号', modelCode: '型号', materialCode: '料号', productionDate: '生产日期', equipmentCode: '设备' };
    return { ...getDefaultPresetHeaderFieldMeta(field), label: headerFieldLabels.value[field] || labels[field] || field };
  }
  if (isAdhesiveMiddleTemplate.value) {
    const labels: Record<string, string> = { processLength: '加工米数/m', productWidthMm: '产品宽幅/mm', widthStart: '开头宽幅/mm', widthMiddle: '中间宽幅/mm', widthEnd: '结尾宽幅/mm', fillInstructions: '填写要求', revisionInfo: '修订信息', copyrightNotice: '版权声明' };
    const numeric = ['processLength', 'productWidthMm', 'widthStart', 'widthMiddle', 'widthEnd'].includes(field);
    return { ...getDefaultPresetHeaderFieldMeta(field), label: labels[field] || getDefaultPresetHeaderFieldMeta(field).label, ...(numeric ? { type: 'number' } : {}) };
  }
  return {
    ...getDefaultPresetHeaderFieldMeta(field),
    label: getPresetHeaderFieldLabel(field),
  };
}

function updatePresetHeaderFieldLabel(field: string, value: unknown) {
  if (!isWetHeaderCaptionConfig() && !isPressSlotProductionConfig.value) return;
  const label = String(value || '').trim();
  if (!label) {
    message.warning('表头显示名称不能为空');
    return;
  }
  headerFieldLabels.value = {
    ...headerFieldLabels.value,
    [field]: label,
  };
}

function isPresetHeaderFieldEditable(field: string) {
  if (!isWetProductionCheckTimeHeader(field)) return false;
  return (
    !headerEditableFieldsConfigured.value ||
    headerEditableFields.value.includes(field)
  );
}

function updatePresetHeaderFieldEditable(field: string, checked: boolean) {
  if (!isWetProductionCheckTimeHeader(field)) return;
  const fields = headerEditableFieldsConfigured.value
    ? [...headerEditableFields.value]
    : [...WET_PRODUCTION_CHECK_TIME_FIELDS];
  headerEditableFieldsConfigured.value = true;
  headerEditableFields.value = checked
    ? [...new Set([...fields, field])]
    : fields.filter((item) => item !== field);
}

function getValueModeLabel(value?: string) {
  return (
    VALUE_MODE_OPTIONS.find((item) => item.value === value)?.label ||
    value ||
    '-'
  );
}

function getPreviewInputText(row: MesHcStationFormApi.StationFormItem) {
  const valueMode = String(row.valueMode || '').toUpperCase();
  if (valueMode === 'OK_NG') return row.defaultResult || 'OK / NG';
  if (valueMode === 'DUAL_LABEL') {
    return (
      [row.dualLabel1, row.dualLabel2].filter(Boolean).join(' / ') || '双值'
    );
  }
  if (valueMode === 'READONLY') return row.defaultResult || '-';
  return row.defaultResult || '';
}

function buildPressSlotIntermediatePreviewLayout() {
  const schema = currentSchemaObject.value;
  const formName =
    previewFormValues.value.formName || formData.value?.formName || '';
  const detailItems = getPressSlotIntermediateDetailItems();
  const thicknessItem = findPressSlotItem((row) =>
    matchPressSlotText(row.itemName, ['厚度']),
  );
  const depthFields = buildPressSlotIntermediateDepthFields();
  const title = firstPreviewText(
    schema.displayName,
    schema.formDisplayName,
    stripPressSlotPreviewModelPrefix(formName),
    '压槽中间品记录表',
  );
  const thicknessStandard = extractPressSlotStandard(
    thicknessItem?.standardText,
  );
  const detailStandardText = thicknessStandard
    ? `厚度标准：${thicknessStandard}`
    : '厚度标准按配置明细项执行';

  return {
    attachments: buildPressSlotIntermediateAttachments(),
    columns: buildPressSlotIntermediateColumns(thicknessItem),
    depthFields,
    depthHint: firstPreviewText(
      schema.depthHint,
      '首检批号对应的槽深最小值、最大值、平均值',
    ),
    depthTitle: `首件槽深/mm（标准：${formatPressSlotStandardList(depthFields)}）`,
    detailHint: firstPreviewText(
      schema.detailHint,
      `每片每 ${Number(schema.thicknessIntervalCm || schema.measureIntervalCm || 100)}cm 测量一次厚度，共 ${resolvePressSlotThicknessCount(thicknessItem)} 个测量点；${detailStandardText}`,
    ),
    detailTitle: firstPreviewText(
      schema.detailTitle,
      getPressSlotDetailCategoryText(detailItems),
      '中间品记录明细',
    ),
    headFields: buildPressSlotIntermediateHeadFields(),
    metaItems: buildPressSlotIntermediateMetaItems(),
    rows: buildPressSlotIntermediateRows(detailItems, thicknessItem),
    signatureFields: buildPressSlotIntermediateSignatureFields(),
    title,
  };
}

function buildPressSlotIntermediateHeadFields() {
  const runtimeFields = getPressSlotRuntimeSectionFields(
    'formInfo',
    'headerGrid',
  );
  if (runtimeFields.length) {
    return runtimeFields.map((field, index) => {
      const fieldKey = firstPreviewText(field.field, `field${index + 1}`);
      return {
        field: fieldKey,
        label: firstPreviewText(
          field.label,
          getPresetHeaderFieldMeta(fieldKey).label,
        ),
        value: resolvePressSlotPreviewValue(fieldKey, undefined, '待填写'),
      };
    });
  }

  const mainItems = previewRows.value.filter((row) =>
    matchPressSlotText(row.itemCategory, ['主表']),
  );
  const fallbackFields = [
    'productionDate',
    'modelCode',
    'materialCode',
    'batchNo',
  ];
  const rows = mainItems.length
    ? mainItems
    : fallbackFields.map(
        (field, index) =>
          ({
            itemName: getPresetHeaderFieldMeta(field).label,
            itemSeq: index + 1,
          }) as MesHcStationFormApi.StationFormItem,
      );

  return rows.map((row, index) => {
    const field = resolvePressSlotHeaderField(row.itemName, index);
    return {
      field,
      label: firstPreviewText(
        row.itemName,
        getPresetHeaderFieldMeta(field).label,
      ),
      value: resolvePressSlotPreviewValue(field, row.defaultResult, '待填写'),
    };
  });
}

function buildPressSlotIntermediateSignatureFields() {
  const runtimeFields = getPressSlotRuntimeSectionFields(
    'signature',
    'signatureGrid',
  );
  if (runtimeFields.length) {
    return runtimeFields.map((field, index) => {
      const fieldKey = firstPreviewText(field.field, `signature${index + 1}`);
      return {
        field: fieldKey,
        label: firstPreviewText(field.label, fieldKey),
        value: resolvePressSlotPreviewValue(fieldKey, undefined, '待填写'),
      };
    });
  }

  return [
    {
      field: 'recorderName',
      label: '填写人',
      value: resolvePressSlotPreviewValue('recorderName', undefined, '待填写'),
    },
    {
      field: 'recordTime',
      label: '填写时间',
      value: resolvePressSlotPreviewValue('recordTime', undefined, '待填写'),
    },
    {
      field: 'confirmerName',
      label: '确认人',
      value: resolvePressSlotPreviewValue('confirmerName', undefined, '待填写'),
    },
    {
      field: 'confirmTime',
      label: '确认时间',
      value: resolvePressSlotPreviewValue('confirmTime', undefined, '待填写'),
    },
  ];
}

function buildPressSlotIntermediateMetaItems() {
  return [
    `计划号：${resolvePressSlotPreviewValue('planNo', undefined, '-')}`,
    `压槽片号：${resolvePressSlotPreviewValue('pressSlotSliceNo', undefined, '-')}`,
    '状态：预览',
  ];
}

function buildPressSlotIntermediateDepthFields() {
  const depthItems = previewRows.value.filter((row) =>
    matchPressSlotText(`${row.itemCategory || ''} ${row.itemName || ''}`, [
      '槽深',
      '沟深',
      '最小值',
      '最大值',
      '平均值',
    ]),
  );
  const standardFallback = firstPreviewText(
    ...depthItems.map((row) => row.standardText),
    '-',
  );
  const runtimeFields = getPressSlotRuntimeSectionFields(
    'firstSlotDepth',
    'matrix',
  );
  if (runtimeFields.length) {
    return runtimeFields.map((field, index) => {
      const fieldKey = firstPreviewText(
        field.field,
        `firstSlotDepth${index + 1}`,
      );
      return {
        field: fieldKey,
        label: firstPreviewText(field.label, fieldKey),
        standard:
          findPressSlotDepthItem(
            depthItems,
            [field.label, fieldKey].filter(Boolean),
          )?.standardText || standardFallback,
        value: resolvePressSlotPreviewValue(fieldKey, undefined, '待填写'),
      };
    });
  }

  return [
    {
      field: 'firstSlotDepthMin',
      label:
        findPressSlotDepthItem(depthItems, ['最小'])?.itemName || 'XY最小值',
      standard:
        findPressSlotDepthItem(depthItems, ['最小'])?.standardText ||
        standardFallback,
      value: resolvePressSlotPreviewValue(
        'firstSlotDepthMin',
        undefined,
        '待填写',
      ),
    },
    {
      field: 'firstSlotDepthMax',
      label:
        findPressSlotDepthItem(depthItems, ['最大'])?.itemName || 'XY最大值',
      standard:
        findPressSlotDepthItem(depthItems, ['最大'])?.standardText ||
        standardFallback,
      value: resolvePressSlotPreviewValue(
        'firstSlotDepthMax',
        undefined,
        '待填写',
      ),
    },
    {
      field: 'firstSlotDepthAvg',
      label:
        findPressSlotDepthItem(depthItems, ['平均'])?.itemName || 'XY平均值',
      standard:
        findPressSlotDepthItem(depthItems, ['平均'])?.standardText ||
        standardFallback,
      value: resolvePressSlotPreviewValue(
        'firstSlotDepthAvg',
        undefined,
        '待填写',
      ),
    },
  ];
}

function buildPressSlotIntermediateColumns(
  thicknessItem?: MesHcStationFormApi.StationFormItem,
) {
  const schema = currentSchemaObject.value;
  const intervalCm = Number(
    schema.thicknessIntervalCm || schema.measureIntervalCm || 100,
  );
  const thicknessCount = resolvePressSlotThicknessCount(thicknessItem);
  const thicknessLabel = normalizePressSlotThicknessLabel(
    thicknessItem?.itemName,
  );
  const thicknessStandard = extractPressSlotStandard(
    thicknessItem?.standardText,
  );
  const runtimeColumns = getPressSlotRuntimeTableColumns();
  if (runtimeColumns.length) {
    return runtimeColumns.map((column, index) => {
      const key = firstPreviewText(
        column.field,
        column.key,
        `column${index + 1}`,
      );
      const title = firstPreviewText(column.label, column.title, key);
      return {
        key,
        title:
          key.startsWith('thickness') &&
          thicknessStandard &&
          !title.includes(thicknessStandard)
            ? `${title}(${thicknessStandard})`
            : title,
        width: Number(column.width) || resolvePressSlotColumnWidth(key, index),
      };
    });
  }

  return [
    { key: 'samplePositionName', title: '采样段', width: 100 },
    {
      key: 'sliceBatchNo',
      title: findPressSlotItemName(['片号']) || '片号',
      width: 180,
    },
    {
      key: 'widthMm',
      title: findPressSlotItemName(['宽幅']) || '宽幅/mm',
      width: 120,
    },
    ...Array.from({ length: thicknessCount }, (_, index) => ({
      key: `thickness${index + 1}`,
      title: `${(index + 1) * intervalCm}cm${thicknessLabel}${thicknessStandard ? `(${thicknessStandard})` : ''}`,
      width: 190,
    })),
    { key: 'remark', title: '备注', width: 180 },
  ];
}

function buildPressSlotIntermediateRows(
  detailItems: MesHcStationFormApi.StationFormItem[],
  thicknessItem?: MesHcStationFormApi.StationFormItem,
) {
  const placeholder = firstPreviewText(
    currentSchemaObject.value.previewPlaceholder,
    '待填写',
  );
  const thicknessCount = resolvePressSlotThicknessCount(thicknessItem);
  const previewDetails = presetHeaderDataMap.value.previewDetails;
  if (Array.isArray(previewDetails) && previewDetails.length) {
    return previewDetails.map((detail, index) => {
      const row: Record<string, any> = {
        ...detail,
        key: detail.key || `POSITION-${index + 1}`,
        samplePositionName: firstPreviewText(
          detail.samplePositionName,
          detail.positionName,
          `第${index + 1}段`,
        ),
        seq: detail.seq || index + 1,
      };
      for (let i = 1; i <= thicknessCount; i += 1) {
        row[`thickness${i}`] = firstPreviewText(
          row[`thickness${i}`],
          placeholder,
        );
      }
      row.sliceBatchNo = firstPreviewText(row.sliceBatchNo, placeholder);
      row.widthMm = firstPreviewText(row.widthMm, placeholder);
      return row;
    });
  }

  return resolvePressSlotSamplePositions(detailItems).map((label, index) => {
    const row: Record<string, any> = {
      key: `POSITION-${index + 1}`,
      remark: '',
      samplePositionName: label,
      seq: index + 1,
      sliceBatchNo: placeholder,
      widthMm: placeholder,
    };
    for (let i = 1; i <= thicknessCount; i += 1) {
      row[`thickness${i}`] = placeholder;
    }
    return row;
  });
}

function buildPressSlotIntermediateAttachments() {
  const attachments = presetHeaderDataMap.value.attachments;
  if (!Array.isArray(attachments)) return [];
  return attachments.map((attachment, index) => ({
    ...attachment,
    name: firstPreviewText(attachment.name, `原始导入附件${index + 1}`),
    uid: firstPreviewText(
      attachment.uid,
      attachment.url,
      attachment.path,
      `ATTACHMENT-${index + 1}`,
    ),
  }));
}

function getPressSlotRuntimeSectionFields(
  sectionKey: string,
  sectionType: string,
) {
  const section = getPressSlotRuntimeLayoutSection(sectionKey, sectionType);
  return Array.isArray(section?.fields) ? section.fields : [];
}

function getPressSlotRuntimeTableColumns() {
  const section = getPressSlotRuntimeLayoutSection(
    'intermediateDetails',
    'editableTable',
  );
  return Array.isArray(section?.columns) ? section.columns : [];
}

function getPressSlotRuntimeLayoutSection(
  sectionKey: string,
  sectionType: string,
) {
  const schema = currentSchemaObject.value;
  const sections = schema.runtimeLayout?.sections;
  if (!Array.isArray(sections)) return undefined;
  return sections.find(
    (section) => section?.key === sectionKey || section?.type === sectionType,
  );
}

function resolvePressSlotColumnWidth(key: string, index: number) {
  if (key === 'samplePositionName') return 100;
  if (key === 'sliceBatchNo') return 180;
  if (key === 'widthMm') return 120;
  if (key.startsWith('thickness')) return 190;
  return index < 3 ? 120 : 160;
}

function getPressSlotIntermediateDetailItems() {
  return previewRows.value.filter((row) =>
    matchPressSlotText(`${row.itemCategory || ''} ${row.itemName || ''}`, [
      '前段',
      '中段',
      '后段',
      '片号',
      '宽幅',
      '厚度',
    ]),
  );
}

function getPressSlotDetailCategoryText(
  detailItems: MesHcStationFormApi.StationFormItem[],
) {
  return firstPreviewText(
    detailItems
      .map((row) => row.itemCategory)
      .find((text) => matchPressSlotText(text, ['前段', '中段', '后段'])),
    '前段 / 中段 / 后段宽幅与厚度测量',
  );
}

function findPressSlotDepthItem(
  items: MesHcStationFormApi.StationFormItem[],
  keywords: string[],
) {
  return items.find((row) => matchPressSlotText(row.itemName, keywords));
}

function findPressSlotItem(
  predicate: (row: MesHcStationFormApi.StationFormItem) => boolean,
) {
  return previewRows.value.find(predicate);
}

function findPressSlotItemName(keywords: string[]) {
  return findPressSlotItem((row) => matchPressSlotText(row.itemName, keywords))
    ?.itemName;
}

function resolvePressSlotSamplePositions(
  detailItems: MesHcStationFormApi.StationFormItem[],
) {
  const schema = currentSchemaObject.value;
  if (Array.isArray(schema.samplePositions) && schema.samplePositions.length) {
    return schema.samplePositions
      .map((item) => firstPreviewText(item))
      .filter(Boolean);
  }
  const category = getPressSlotDetailCategoryText(detailItems);
  const prefix = category.split(/宽幅|厚度|测量/u)[0] || '';
  const parsed = prefix
    .split(/[\/／、,，]/u)
    .map((item) => item.trim())
    .filter((item) => matchPressSlotText(item, ['前段', '中段', '后段']));
  return parsed.length ? parsed : PRESS_SLOT_INTERMEDIATE_DEFAULT_POSITIONS;
}

function resolvePressSlotThicknessCount(
  thicknessItem?: MesHcStationFormApi.StationFormItem,
) {
  const schema = currentSchemaObject.value;
  const configuredCount = Number(
    schema.thicknessColumnCount || schema.measureColumnCount,
  );
  if (Number.isFinite(configuredCount) && configuredCount > 0)
    return configuredCount;
  const text = `${thicknessItem?.itemName || ''} ${thicknessItem?.standardText || ''}`;
  const rangeMatch = text.match(/(\d+)\s*[-~至到]\s*(\d+)/u);
  if (rangeMatch) {
    const start = Number(rangeMatch[1]);
    const end = Number(rangeMatch[2]);
    if (Number.isFinite(start) && Number.isFinite(end) && end >= start) {
      return Math.min(end - start + 1, 30);
    }
  }
  return 10;
}

function resolvePressSlotHeaderField(label?: string, index = 0) {
  const text = normalizePressSlotSearchText(label);
  if (text.includes('生产日期') || text === '日期') return 'productionDate';
  if (text.includes('型号')) return 'modelCode';
  if (text.includes('料号') || text.includes('物料')) return 'materialCode';
  if (text.includes('批号')) return 'batchNo';
  if (text.includes('投入')) return 'inputQty';
  if (text.includes('产出')) return 'outputQty';
  if (
    text.includes('填写人') ||
    text.includes('记录人') ||
    text.includes('担当')
  )
    return 'recorderName';
  if (text.includes('填写时间') || text.includes('记录时间'))
    return 'recordTime';
  if (text.includes('确认人')) return 'confirmerName';
  if (text.includes('确认时间')) return 'confirmTime';
  return `field${index + 1}`;
}

function resolvePressSlotPreviewValue(
  field: string,
  fallback?: unknown,
  emptyText = '-',
) {
  const header = presetHeaderDataMap.value || {};
  const aliases: Record<string, string[]> = {
    batchNo: ['batchNo', 'motherBatchNo', 'productionBatchNo'],
    confirmTime: ['confirmTime', 'confirmerTime'],
    confirmerName: ['confirmerName', 'confirmer'],
    firstSlotDepthAvg: [
      'firstSlotDepthAvg',
      'firstSlotDepthXAvg',
      'firstSlotDepthYAvg',
    ],
    firstSlotDepthMax: [
      'firstSlotDepthMax',
      'firstSlotDepthXMax',
      'firstSlotDepthYMax',
    ],
    firstSlotDepthMin: [
      'firstSlotDepthMin',
      'firstSlotDepthXMin',
      'firstSlotDepthYMin',
    ],
    materialCode: ['materialCode'],
    modelCode: ['modelCode', 'modelName'],
    outputQty: ['outputQty'],
    planNo: ['planNo'],
    pressSlotSliceNo: ['pressSlotSliceNo', 'productionBatchNo', 'sliceBatchNo'],
    productionDate: ['productionDate', 'recordDate'],
    recordTime: ['recordTime', 'recorderTime', 'fillTime'],
    recorderName: ['recorderName', 'recorder', 'fillUserName'],
    inputQty: ['inputQty'],
  };
  return firstPreviewText(
    ...(aliases[field] || [field]).map((key) => header[key]),
    fallback,
    emptyText,
  );
}

function normalizePressSlotThicknessLabel(label?: string) {
  const text = firstPreviewText(label, '厚度/mm')
    .replace(/\s*1\s*[-~至到]\s*\d+\s*/u, '')
    .replace(/㎜/gu, 'mm')
    .trim();
  return text || '厚度/mm';
}

function extractPressSlotStandard(text?: string) {
  const value = firstPreviewText(text, '');
  const match = value.match(/[（(]\s*([^）)]+?)\s*[）)]/u);
  if (match?.[1]) return match[1].trim();
  return value.replace(/^厚度\s*\/?\s*(mm|㎜)?/iu, '').trim();
}

function formatPressSlotStandardList(fields: Array<{ standard?: string }>) {
  const standards = Array.from(
    new Set(
      fields
        .map((field) => firstPreviewText(field.standard, ''))
        .filter(Boolean),
    ),
  );
  return standards.length ? standards.join(' / ') : '-';
}

function firstPreviewText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function normalizePressSlotSearchText(value?: unknown) {
  return String(value ?? '')
    .replace(/\s+/gu, '')
    .toUpperCase();
}

function matchPressSlotText(value: unknown, keywords: string[]) {
  const text = normalizePressSlotSearchText(value);
  return keywords.some((keyword) =>
    text.includes(normalizePressSlotSearchText(keyword)),
  );
}

function stripPressSlotPreviewModelPrefix(name?: string) {
  const text = firstPreviewText(name);
  return text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim() || text;
}

function validateItemRows() {
  for (const row of itemRows.value) {
    const error = multiDefinitionError(row);
    if (error) { message.warning(`${row.itemName || row.itemSeq}：${error}`); return false; }
  }
  if (isAdhesiveMiddleTemplate.value) return true;
  if (isWetSemiFinishedTemplateConfig()) {
    return true;
  }
  if (!itemRows.value.length) {
    message.warning('请至少维护一条明细项');
    return false;
  }
  if (
    isFormulaStandard.value &&
    new Set(itemRows.value.map((item) => item.itemSeq)).size !==
      itemRows.value.length
  ) {
    message.warning('配料明细序号不能重复，请核对高级配置中的序号');
    return false;
  }
  const invalidIndex = itemRows.value.findIndex(
    (item) => !item.itemSeq || !String(item.itemName || '').trim(),
  );
  if (invalidIndex >= 0) {
    message.warning(`明细项第 ${invalidIndex + 1} 行请填写序号和项目名称`);
    return false;
  }
  return true;
}

function validatePresetHeaderJson() {
  presetHeaderDataText.value = JSON.stringify(
    presetHeaderDataMap.value || {},
    null,
    2,
  );
  return true;
}

function validateSchemaApplicability() {
  if (isAdhesiveMiddleTemplate.value && (adhesiveThicknessLabelsEdited.value ?? adhesiveThicknessLabels.value).some((label) => !label.trim())) {
    message.warning('请填写完整的左右两个厚度列标题');
    return false;
  }
  if (isRoughMiddleTemplate.value && roughThicknessLabelsEdited.value?.some((label) => !label.trim())) {
    message.warning('请填写完整的左右两个厚度列标题');
    return false;
  }
  if (
    isWetSemiFinishedTemplateConfig() &&
    wetThicknessLabelsEdited.value?.some((label) => !label.trim())
  ) {
    message.warning('请填写完整的四个厚度列标题');
    return false;
  }
  if (
    schemaEditor.value.modelScope === 'MODEL' &&
    !normalizeSchemaModelValue(schemaEditor.value.modelCode)
  ) {
    activeTab.value = 'schema';
    selectDesignerNode('schema');
    message.warning('适用范围为指定型号时，请填写适用型号');
    return false;
  }
  if (
    schemaEditor.value.modelScope === 'PREFIX' &&
    !normalizeSchemaModelValue(schemaEditor.value.modelPrefix)
  ) {
    activeTab.value = 'schema';
    selectDesignerNode('schema');
    message.warning('适用范围为型号前缀时，请填写型号前缀');
    return false;
  }
  return true;
}

function validateBasicFormValues(values: MesHcStationFormApi.StationForm) {
  const requiredFields: Array<{
    field: keyof MesHcStationFormApi.StationForm;
    label: string;
  }> = [
    { field: 'formCode', label: '表单编码' },
    { field: 'formName', label: '表单名称' },
    { field: 'processCode', label: '业务工序' },
    { field: 'triggerTimingCode', label: '触发时机' },
    { field: 'needConfirm', label: '需确认' },
    { field: 'sortNo', label: '排序' },
    { field: 'status', label: '状态' },
  ];
  const missing = requiredFields.find(({ field }) => {
    const value = values[field];
    return value === undefined || value === null || String(value).trim() === '';
  });
  if (missing) {
    if (isFormulaSimple.value) void toggleFormulaAdvanced();
    activeTab.value = 'basic';
    selectDesignerNode('form');
    message.warning(`请填写${missing.label}`);
    return false;
  }
  if (Number(values.sortNo) <= 0) {
    if (isFormulaSimple.value) void toggleFormulaAdvanced();
    activeTab.value = 'basic';
    selectDesignerNode('form');
    message.warning('排序必须大于 0');
    return false;
  }
  const devOnly = isSchemaDevOnly(values.schemaJson || formData.value?.schemaJson);
  const devCode = isDevFormCode(values.formCode || formData.value?.formCode);
  if (devOnly && !devCode) {
    activeTab.value = 'basic';
    selectDesignerNode('form');
    message.warning('DEV 模式表单编码必须以 _DEV 结尾');
    return false;
  }
  if (!devOnly && devCode) {
    activeTab.value = 'basic';
    selectDesignerNode('form');
    message.warning('_DEV 表单必须在 Schema 中标记 devOnly=true');
    return false;
  }
  return true;
}

function isRuntimeLayoutEngine(value?: unknown) {
  return (
    value === RUNTIME_LAYOUT_ENGINE ||
    value === LEGACY_RUNTIME_LAYOUT_DEV_ENGINE
  );
}

function isRuntimeLayoutSchema(schemaText?: string) {
  const schema = parseSchemaText(schemaText);
  return isRuntimeLayoutEngine(schema.runtimeEngine) && !!schema.runtimeLayout;
}

function isSchemaDevOnly(schemaText?: string) {
  const value = parseSchemaText(schemaText).devOnly;
  return value === true || value === 'true' || value === 1 || value === '1';
}

function isDevFormCode(formCode?: string) {
  return String(formCode || '')
    .trim()
    .toUpperCase()
    .endsWith('_DEV');
}

function isDevStationFormConfig(values?: MesHcStationFormApi.StationForm) {
  const current = values || formData.value || {};
  return isDevFormCode(current.formCode || formData.value?.formCode)
    || isSchemaDevOnly(current.schemaJson || formData.value?.schemaJson);
}

function usesRuntimeLayoutEditor() {
  if (isPressSlotProductionConfig.value) return false;
  const current = formData.value || {};
  return isDevStationFormConfig(current)
    || isRuntimeLayoutSchema(current.schemaJson);
}

function hasStationFormItemContent(row: MesHcStationFormApi.StationFormItem) {
  return (
    [
      row.defaultResult,
      row.dualLabel1,
      row.dualLabel2,
      row.itemCategory,
      row.itemName,
      row.remark,
      row.standardText,
      row.stepNode,
    ].some((value) => String(value ?? '').trim()) || row.requiredFlag === true
  );
}

function isCompleteStationFormItem(row: MesHcStationFormApi.StationFormItem) {
  return !!row.itemSeq && !!String(row.itemName || '').trim();
}

function buildStationFormItemsPayload(options: { devLayout?: boolean } = {}) {
  if (isWetSemiFinishedTemplateConfig()) {
    return [];
  }
  const sourceRows = options.devLayout
    ? itemRows.value.filter((row) => isCompleteStationFormItem(row))
    : itemRows.value;
  return sourceRows.map(({ _rowKey, ...row }, index) => ({
    ...row,
    itemSeq: row.itemSeq || index + 1,
    requiredFlag: row.requiredFlag ?? false,
    valueMode: row.valueMode || 'TEXT',
  }));
}

function buildStationFormPayloadFromValues(
  values: MesHcStationFormApi.StationForm,
  processOptions = PROCESS_OPTIONS,
  options: { devLayout?: boolean } = {},
) {
  if (!isFormulaStandard.value && !isAdhesiveMiddleTemplate.value) normalizeItemSeq();
  const items = buildStationFormItemsPayload(options);
  const triggerTimingCode =
    values.triggerTimingCode || formData.value?.triggerTimingCode || '';
  const processCode = values.processCode || formData.value?.processCode || '';
  const payload = {
    ...formData.value,
    ...values,
    items,
    presetHeaderDataJson:
      isFormulaStandard.value && !formulaHeaderEdited.value
        ? formData.value?.presetHeaderDataJson
        : JSON.stringify(presetHeaderDataMap.value || {}, null, 2),
    presetItems: items,
    processName:
      values.processName ||
      formData.value?.processName ||
      resolveProcessName(processCode, processOptions),
    schemaJson: isPressSlotProductionConfig.value ? JSON.stringify({ ...productionCheckSchema(parseSchemaText(buildSchemaJson())), devOnly: false }) : buildSchemaJson(),
    triggerTimingName:
      values.triggerTimingName ||
      formData.value?.triggerTimingName ||
      TRIGGER_TIMING_NAME_MAP[triggerTimingCode] ||
      '',
  };
  console.log('[动态表单配置保存] buildPayload done', {
    formCode: payload.formCode,
    formId: payload.id,
    itemCount: payload.items?.length || 0,
    rawItemRowCount: itemRows.value.length,
    processCode: payload.processCode,
    schemaJsonLength: payload.schemaJson?.length || 0,
    status: payload.status,
    triggerTimingCode: payload.triggerTimingCode,
  });
  return payload;
}

function buildDevStationFormPayload(needValidate = false) {
  const values = {
    ...(formData.value || {}),
  } as MesHcStationFormApi.StationForm;
  console.warn(
    '[动态表单配置保存] DEV config branch: skip Vben formApi validate/getValues',
    {
      formCode: values.formCode,
      formId: values.id,
      needValidate,
    },
  );
  if (needValidate) {
    const basicFormValid = validateBasicFormValues(values);
    console.log('[动态表单配置保存] DEV validateBasicFormValues result', {
      basicFormValid,
      formCode: values.formCode,
      formName: values.formName,
      needConfirm: values.needConfirm,
      processCode: values.processCode,
      sortNo: values.sortNo,
      status: values.status,
      triggerTimingCode: values.triggerTimingCode,
    });
    const partialItemIndex = itemRows.value.findIndex(
      (item) =>
        hasStationFormItemContent(item) && !isCompleteStationFormItem(item),
    );
    const itemRowsValid = partialItemIndex < 0;
    if (!itemRowsValid) {
      message.warning(
        `明细项第 ${partialItemIndex + 1} 行已填写部分内容，请补充序号和项目名称或清空该行`,
      );
    }
    console.log('[动态表单配置保存] DEV validateItemRows result', {
      itemRowsValid,
      partialItemIndex,
      rowCount: itemRows.value.length,
      validItemCount: itemRows.value.filter((item) =>
        isCompleteStationFormItem(item),
      ).length,
    });
    const presetHeaderValid = validatePresetHeaderJson();
    console.log('[动态表单配置保存] DEV validatePresetHeaderJson result', {
      presetHeaderKeys: Object.keys(presetHeaderDataMap.value || {}),
      presetHeaderValid,
    });
    const schemaApplicabilityValid = validateSchemaApplicability();
    console.log('[动态表单配置保存] DEV validateSchemaApplicability result', {
      modelCode: schemaEditor.value.modelCode,
      modelPrefix: schemaEditor.value.modelPrefix,
      modelScope: schemaEditor.value.modelScope,
      schemaApplicabilityValid,
    });
    if (
      !basicFormValid ||
      !itemRowsValid ||
      !presetHeaderValid ||
      !schemaApplicabilityValid
    ) {
      console.warn(
        '[动态表单配置保存] DEV payload blocked by validation, no api request',
        {
          basicFormValid,
          itemRowsValid,
          presetHeaderValid,
          schemaApplicabilityValid,
        },
      );
      return undefined;
    }
  } else {
    validatePresetHeaderJson();
  }
  return buildStationFormPayloadFromValues(values, PROCESS_OPTIONS, {
    devLayout: true,
  });
}

async function buildStationFormPayload(needValidate = false) {
  console.log('[动态表单配置保存] buildPayload start', {
    formCode: formData.value?.formCode,
    formId: formData.value?.id,
    itemRows: itemRows.value.length,
    needValidate,
    schemaEditor: { ...schemaEditor.value },
  });
  if (usesRuntimeLayoutEditor()) {
    return buildDevStationFormPayload(needValidate);
  }
  console.log('[动态表单配置保存] before formApi.getValues');
  const values = isFormulaSimple.value
    ? { ...formulaSimpleValues.value }
    : ((await formApi.getValues()) as MesHcStationFormApi.StationForm);
  console.log('[动态表单配置保存] formApi.getValues result', values);
  if (needValidate) {
    console.warn(
      '[动态表单配置保存] skip formApi.validate, use manual validation to avoid validate promise blocking',
    );
    const basicFormValid = validateBasicFormValues(values);
    console.log('[动态表单配置保存] validateBasicFormValues result', {
      basicFormValid,
      formCode: values.formCode,
      formName: values.formName,
      needConfirm: values.needConfirm,
      processCode: values.processCode,
      sortNo: values.sortNo,
      status: values.status,
      triggerTimingCode: values.triggerTimingCode,
    });
    const itemRowsValid = validateItemRows();
    console.log('[动态表单配置保存] validateItemRows result', {
      invalidIndex: itemRows.value.findIndex(
        (item) => !item.itemSeq || !String(item.itemName || '').trim(),
      ),
      itemRowsValid,
      rowCount: itemRows.value.length,
    });
    const presetHeaderValid = validatePresetHeaderJson();
    console.log('[动态表单配置保存] validatePresetHeaderJson result', {
      presetHeaderKeys: Object.keys(presetHeaderDataMap.value || {}),
      presetHeaderValid,
    });
    const schemaApplicabilityValid = validateSchemaApplicability();
    console.log('[动态表单配置保存] validateSchemaApplicability result', {
      modelCode: schemaEditor.value.modelCode,
      modelPrefix: schemaEditor.value.modelPrefix,
      modelScope: schemaEditor.value.modelScope,
      schemaApplicabilityValid,
    });
    if (
      !basicFormValid ||
      !itemRowsValid ||
      !presetHeaderValid ||
      !schemaApplicabilityValid
    ) {
      console.warn(
        '[动态表单配置保存] payload blocked by validation, no api request',
        {
          basicFormValid,
          itemRowsValid,
          presetHeaderValid,
          schemaApplicabilityValid,
        },
      );
      return undefined;
    }
  } else {
    validatePresetHeaderJson();
  }

  const processOptions = await loadProcessOptions();
  return buildStationFormPayloadFromValues(values, processOptions);
}

async function applyStationFormDraft(form?: MesHcStationFormApi.StationForm) {
  if (!form) return;
  formData.value = {
    ...formData.value,
    ...form,
  };
  formulaSimpleValues.value = { ...formData.value };
  formulaHeaderEdited.value = false;
  syncSchemaEditor(form.schemaJson);
  presetHeaderDataText.value = form.presetHeaderDataJson || '{}';
  syncPresetHeaderDataMap(form.presetHeaderDataJson || '{}');
  itemRows.value = (form.items || form.presetItems || []).map((item) =>
    normalizeRow(item),
  );
  normalizeLoadedFormulaModes();
  if (!itemRows.value.length && !isWetSemiFinishedTemplateConfig()) {
    itemRows.value = [normalizeRow()];
  }
  syncPresetRowsFromItems();
  if (!isFormulaSimple.value) await formApi.setValues(formData.value);
  if (designerMode.value === 'preview') {
    await refreshPreviewFormValues();
  }
}

function buildAdhesivePreviewExcelLayout(formName?: string): MesHcProcessFormApi.LayoutExcelReq {
  return {
    title: formName || '粘胶1中间品记录表', sheetName: '粘胶1中间品记录',
    fileName: `${formName || '粘胶1中间品记录表'}.xlsx`, visualMode: 'adhesive1-middle', detailTitle: '中间品记录明细',
    columns: toRoughExcelColumns(adhesivePreviewColumns.value),
    headerItems: previewHeaderFields.value.filter((field) => !ADHESIVE_INTERMEDIATE_NOTES.some((note) => note.key === field.field)).map((field) => ({
      label: field.label, value: String(field.value ?? ''), bindKey: field.field, bindField: 'presetHeader',
      editable: !['recorder', 'recorderTime', 'confirmer', 'confirmerTime'].includes(field.field),
    })),
    importStopPrefixes: ['填写要求：', '修订信息：', '版权声明：'],
    footerNotes: buildAdhesiveIntermediateFooterNotes(presetHeaderDataMap.value),
    rows: Array.from({ length: adhesivePreviewRowCount.value }, (_, index) => ({ cells: adhesivePreviewColumns.value.map((column, colIndex) => createRoughLayoutCell(colIndex, column.key === 'lengthMark' ? index + 1 : '')) })),
  };
}

async function importAdhesivePreviewExcel(file: File, formName?: string) {
  const layout = buildAdhesivePreviewExcelLayout(formName);
  const headerRow = 3 + Math.ceil((layout.headerItems?.length || 0) / 2);
  layout.importBodyStartRow = headerRow;
  layout.importBodyMaxRows = 1;
  layout.rows = [{ cells: adhesivePreviewColumns.value.map((column, index) => ({
    colIndex: index, importRowIndex: headerRow, importColIndex: index,
    bindKey: String(index), bindField: column.key, editable: true,
  })) }];
  const resp = await importProcessFormRecordLayout(file, layout);
  const titles = [1, 2].map((index) => resp.cellValues?.find((cell) => cell.colIndex === index)?.value?.trim() || '');
  if (titles.some((title) => !title)) throw new Error('导入表格缺少左右厚度列标题，请使用本页面导出的预览Excel');
  if (resp.cellValues?.find((cell) => cell.colIndex === 0)?.value !== '长度/m'
      || resp.cellValues?.find((cell) => cell.colIndex === 3)?.value !== '备注') {
    throw new Error('长度/m和备注为固定列，请使用本页面导出的预览Excel');
  }
  for (const field of resp.headerValues || []) {
    if (field.bindKey && !['recorder', 'recorderTime', 'confirmer', 'confirmerTime'].includes(field.bindKey)) {
      const numeric = ['processLength', 'productWidthMm', 'widthStart', 'widthMiddle', 'widthEnd'].includes(field.bindKey);
      const value = numeric && field.value?.trim() ? Number(field.value) : field.value || '';
      if (typeof value === 'number' && (!Number.isFinite(value) || value < 0)) throw new Error('宽幅和加工米数必须为有效非负数');
      updatePresetHeaderField(field.bindKey, value);
    }
  }
  adhesiveThicknessLabelsEdited.value = titles;
  message.success('已导入主表预设值和左右厚度标题；静态说明仍在预设值预览中维护，保存后生效');
}

async function handleExportPreviewExcel() {
  const payload = await buildStationFormPayload(true);
  if (!payload) return;

  previewExcelLoading.value = true;
  try {
    const data = isAdhesiveMiddleTemplate.value
      ? await exportProcessFormRecordLayout(buildAdhesivePreviewExcelLayout(payload.formName))
      : await exportStationFormPreviewExcel(payload);
    downloadFileFromBlobPart({
      fileName: `${payload.formName || '动态表单预览'}.xlsx`,
      source: data,
    });
  } finally {
    previewExcelLoading.value = false;
  }
}

function handleImportPreviewExcelClick() {
  previewExcelInputRef.value?.click();
}

async function handleImportPreviewExcelChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  const payload = await buildStationFormPayload(true);
  if (!payload) {
    input.value = '';
    return;
  }

  previewExcelLoading.value = true;
  try {
    if (isAdhesiveMiddleTemplate.value) {
      await importAdhesivePreviewExcel(file, payload.formName);
      return;
    }
    const resp = await importStationFormPreviewExcel(file, payload);
    if ((resp.failureCount || 0) > 0 || resp.status === 'ERROR') {
      message.error(
        resp.failures?.[0] || resp.messages?.[0] || '预览 Excel 导入失败',
      );
      return;
    }
    await applyStationFormDraft(resp.form);
    message.success(`预览 Excel 已导入，明细 ${resp.successCount || 0} 行`);
  } finally {
    previewExcelLoading.value = false;
    input.value = '';
  }
}

async function refreshPreviewFormValues() {
  const values = await buildStationFormPayload(false);
  previewFormValues.value = {
    ...(values || {}),
  };
}

function parseSchemaText(schemaText?: string): Record<string, any> {
  if (!schemaText) return {};
  try {
    const parsed = JSON.parse(schemaText);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed
      : {};
  } catch {
    return {};
  }
}

function normalizeSchemaModelValue(value?: unknown) {
  return String(value ?? '')
    .trim()
    .toUpperCase();
}

function legacyFormulaModelPrefix(schema: Record<string, any>) {
  if (
    !isFormulaStandard.value ||
    schema.modelScope ||
    !formData.value ||
    getFormulaCategory(formData.value) !== 'production'
  )
    return '';
  const prefix = normalizeSchemaModelValue(schema.modelPrefix);
  if (prefix) return prefix;
  const code = normalizeSchemaModelValue(schema.modelCode);
  if (code === 'COMMON') return '';
  if (code) return code;
  return (
    `${formData.value.formCode || ''} ${formData.value.formName || ''}`
      .toUpperCase()
      .match(/[A-Z]+[0-9]+[A-Z]+[0-9]*/)?.[0] || ''
  );
}

function resolveSchemaModelScope(
  schema: Record<string, any>,
): StationFormModelScope {
  if (
    ['COMMON', 'MODEL', 'EXACT', 'PREFIX'].includes(schema.modelScope)
  ) {
    return schema.modelScope === 'EXACT' ? 'MODEL' : schema.modelScope;
  }
  if (isFormulaStandard.value && !schema.modelScope)
    return legacyFormulaModelPrefix(schema) ? 'PREFIX' : 'COMMON';

  const modelPrefix = normalizeSchemaModelValue(
    schema.modelPrefix || schema.modelCodePrefix,
  );
  const modelCode = normalizeSchemaModelValue(schema.modelCode || schema.model);
  if (modelPrefix) return 'PREFIX';
  if (modelCode && modelCode !== 'COMMON' && modelCode !== 'ALL')
    return 'MODEL';
  return 'COMMON';
}

function getModelScopeLabel(scope?: string) {
  return (
    MODEL_SCOPE_OPTIONS.find((item) => item.value === scope)?.label || '通用'
  );
}

function getModelScopeValueText() {
  if (schemaEditor.value.modelScope === 'MODEL') {
    return normalizeSchemaModelValue(schemaEditor.value.modelCode) || '-';
  }
  if (schemaEditor.value.modelScope === 'PREFIX') {
    return `${normalizeSchemaModelValue(schemaEditor.value.modelPrefix) || '-'}*`;
  }
  return '全部型号通用';
}

function normalizeLoadedFormulaModes() {
  if (!isFormulaStandard.value || !formData.value) return;
  itemRows.value = normalizeFormulaInputModes(
    formData.value,
    itemRows.value,
  ) as typeof itemRows.value;
}

function syncSchemaEditor(schemaText?: string) {
  roughThicknessLabelsEdited.value = null;
  adhesiveThicknessLabelsEdited.value = null;
  wetThicknessLabelsEdited.value = null;
  const loadedSchema = parseSchemaText(schemaText);
  // 普通配置先统一旧字符串和数组格式，再生成左侧表头及预设值输入框。
  const schema = isPressSlotProductionCheck(loadedSchema, formData.value?.processCode)
    ? productionCheckSchema(loadedSchema)
    : loadedSchema;
  const modelScope = resolveSchemaModelScope(schema);
  const modelCode = normalizeSchemaModelValue(schema.modelCode || schema.model);
  const modelPrefix = normalizeSchemaModelValue(
    schema.modelPrefix || schema.modelCodePrefix,
  );
  schemaEditor.value = {
    defaultGeneratedLength: schema.defaultGeneratedLength,
    defaultRowCount: schema.defaultRowCount,
    headerFields: Array.isArray(schema.headerFields)
      ? schema.headerFields.join(', ')
      : isAdhesiveMiddleTemplate.value && typeof schema.headerFields === 'string' ? schema.headerFields : '',
    headerLayout: schema.headerLayout || 'GRID_4',
    modelCode: modelScope === 'MODEL' ? modelCode : '',
    modelPrefix:
      modelScope === 'PREFIX'
        ? modelPrefix || legacyFormulaModelPrefix(schema) || modelCode
        : '',
    modelScope,
    presetTemplate: schema.presetTemplate || '',
    rowStep: schema.rowStep,
    sourceModelCode: normalizeSchemaModelValue(schema.sourceModelCode),
    version: schema.version || '',
  };
  headerFieldLabels.value = normalizeHeaderFieldLabels(schema.headerLabels);
  headerEditableFieldsConfigured.value = Array.isArray(
    schema.headerEditableFields,
  );
  headerEditableFields.value = normalizeHeaderEditableFields(
    schema.headerEditableFields,
  );
  schemaBaseline.value = { ...schemaEditor.value };
}

function buildSchemaJson() {
  if (isFormulaStandard.value) {
    const merged = mergeFormulaSchema(
      formData.value?.schemaJson,
      schemaBaseline.value,
      schemaEditor.value,
    );
    return JSON.stringify(
      { ...JSON.parse(merged || '{}'), formulaInputModeVersion: 2 },
      null,
      2,
    );
  }
  const headerFields = schemaEditor.value.headerFields
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
  const schema = parseSchemaText(formData.value?.schemaJson);
  delete schema.model;
  delete schema.modelCode;
  delete schema.modelCodePrefix;
  delete schema.modelPrefix;
  if (isAdhesiveMiddleTemplate.value) {
    schema.thicknessLabels = (adhesiveThicknessLabelsEdited.value ?? adhesiveThicknessLabels.value).map((label) => label.trim());
  }
  if (isRoughMiddleTemplate.value && roughThicknessLabelsEdited.value) {
    schema.thicknessLabels = roughThicknessLabelsEdited.value.map((label) => label.trim());
  }

  if (isWetSemiFinishedTemplateConfig() && wetThicknessLabelsEdited.value) {
    schema.thicknessLabels = wetThicknessLabelsEdited.value.map((label) => label.trim());
  }

  schema.modelScope = schemaEditor.value.modelScope;
  if (schemaEditor.value.modelScope === 'COMMON') {
    schema.modelCode = null;
  } else if (schemaEditor.value.modelScope === 'MODEL') {
    schema.modelCode =
      normalizeSchemaModelValue(schemaEditor.value.modelCode) || undefined;
  } else if (schemaEditor.value.modelScope === 'PREFIX') {
    schema.modelPrefix =
      normalizeSchemaModelValue(schemaEditor.value.modelPrefix) || undefined;
  }

  const managedSchema = {
    defaultGeneratedLength: isAdhesiveMiddleTemplate.value ? undefined : schemaEditor.value.defaultGeneratedLength,
    defaultRowCount: isAdhesiveMiddleTemplate.value ? undefined : getEffectiveDefaultRowCount(),
    headerEditableFields: isWetProductionCheckHeaderConfig()
      ? headerEditableFieldsConfigured.value
        ? headerEditableFields.value
        : [...WET_PRODUCTION_CHECK_TIME_FIELDS]
      : undefined,
    headerFields: isAdhesiveMiddleTemplate.value ? ADHESIVE_INTERMEDIATE_HEADER_FIELDS : headerFields.length ? headerFields : undefined,
    headerLayout: schemaEditor.value.headerLayout || undefined,
    headerLabels: (isWetHeaderCaptionConfig() || isPressSlotProductionConfig.value)
      ? Object.fromEntries(
          headerFields.map((field) => [
            field,
            getPresetHeaderFieldMeta(field).label,
          ]),
        )
      : undefined,
    presetTemplate: schemaEditor.value.presetTemplate || undefined,
    rowStep: isAdhesiveMiddleTemplate.value ? undefined : schemaEditor.value.rowStep,
    sourceModelCode:
      normalizeSchemaModelValue(schemaEditor.value.sourceModelCode) ||
      undefined,
    version: schemaEditor.value.version || undefined,
  };
  Object.entries(managedSchema).forEach(([key, value]) => {
    if (value === undefined || value === '') {
      delete schema[key];
    } else {
      schema[key] = value;
    }
  });
  return JSON.stringify(
    Object.fromEntries(
      Object.entries(schema).filter(
        ([, value]) => value !== undefined && value !== '',
      ),
    ),
    null,
    2,
  );
}

/**
 * 结构模板只在用户明确切换模板时写入默认值。
 * 编辑既有动态表单时，schemaJson 中已经保存的字段是唯一来源，不能被旧预设静默覆盖。
 */
function applySchemaPreset(presetTemplate?: string) {
  if (!presetTemplate || isFormulaStandard.value) return;
  const preset = PRESET_SCHEMA_MAP[presetTemplate];
  if (!preset) return;
  schemaEditor.value = {
    ...schemaEditor.value,
    ...preset,
    presetTemplate,
  };
}

async function submitStationForm() {
  let locked = false;
  try {
    console.log('[动态表单配置保存] submit start', {
      formCode: formData.value?.formCode,
      formId: formData.value?.id,
    });
    const values = await buildStationFormPayload(true);
    if (!values) {
      console.warn(
        '[动态表单配置保存] submit stopped: payload is empty, api request skipped',
      );
      return;
    }

    modalApi.lock();
    locked = true;
    console.log('[动态表单配置保存] call api', {
      action: formData.value?.id ? 'updateStationForm' : 'createStationForm',
      formCode: values.formCode,
      formId: values.id,
      itemCount: values.items?.length || 0,
    });
    const result = await (formData.value?.id
      ? updateStationForm(values)
      : createStationForm(values));
    console.log('[动态表单配置保存] api success', result);
    await modalApi.close();
    emit('success');
    message.success('保存成功');
  } catch (error: any) {
    console.error('[动态表单配置保存] submit failed', error);
    message.error(error?.message || '保存动态表单失败，请查看控制台或接口返回');
  } finally {
    if (locked) {
      modalApi.unlock();
    }
  }
}

function handleSaveClick() {
  console.log('[动态表单配置保存按钮] clicked', {
    activeTab: activeTab.value,
    designerMode: designerMode.value,
    formCode: formData.value?.formCode,
    formId: formData.value?.id,
    formName: formData.value?.formName,
    selectedDesignerNode: selectedDesignerNode.value,
    time: new Date().toISOString(),
  });
  void submitStationForm();
}

watch(
  () => schemaEditor.value.modelScope,
  (modelScope) => {
    if (modelScope === 'COMMON') {
      schemaEditor.value.modelCode = '';
      schemaEditor.value.modelPrefix = '';
    } else if (modelScope === 'MODEL') {
      schemaEditor.value.modelPrefix = '';
    } else if (modelScope === 'PREFIX') {
      schemaEditor.value.modelCode = '';
    }
  },
);

watch(
  presetHeaderFieldKeys,
  () => {
    syncPresetHeaderDataMap(presetHeaderDataText.value);
  },
  { deep: true },
);

watch(
  itemRows,
  () => {
    syncPresetRowsFromItems();
  },
  { deep: true },
);

watch(designerMode, (mode) => {
  if (mode === 'preview') {
    pressSlotIntermediatePreviewPage.value = 1;
    refreshPreviewFormValues();
  }
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'id',
      component: 'Input',
      hide: true,
    },
    {
      fieldName: 'formCode',
      label: '表单编码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入表单编码' },
    },
    {
      fieldName: 'formName',
      label: '表单名称',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入表单名称' },
    },
    {
      fieldName: 'processCode',
      label: '业务工序',
      component: 'ApiSelect',
      rules: 'required',
      componentProps: {
        api: loadProcessOptions,
        labelField: 'label',
        valueField: 'value',
        options: PROCESS_OPTIONS,
        placeholder: '请选择业务工序',
      },
    },
    {
      fieldName: 'triggerTimingCode',
      label: '触发时机',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: TRIGGER_TIMING_OPTIONS,
        placeholder: '请选择触发时机',
      },
    },
    {
      fieldName: 'needConfirm',
      label: '需确认',
      component: 'RadioGroup',
      rules: 'required',
      defaultValue: true,
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        optionType: 'button',
        buttonStyle: 'solid',
      },
    },
    {
      fieldName: 'sortNo',
      label: '排序',
      component: 'InputNumber',
      rules: 'required',
      defaultValue: 10,
      componentProps: {
        min: 1,
        max: 9999,
        precision: 0,
        style: { width: '100%' },
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      rules: 'required',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
        optionType: 'button',
        buttonStyle: 'solid',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { rows: 4, placeholder: '请输入备注' },
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: false,
  header: false,
  footer: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnPressEscape: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-station-form-modal',
  async onConfirm() {
    await submitStationForm();
  },
  destroyOnClose: true,
  onClosed() {
    // 关闭动画结束后再清理；表单组件随弹窗销毁，无需重置仍在渲染的表单。
    formulaAdvanced.value = false;
    formulaHeaderEdited.value = false;
    formulaSimpleValues.value = {};
    formData.value = undefined;
    itemRows.value = [];
    presetRows.value = [];
    presetHeaderDataMap.value = {};
    presetHeaderDataText.value = '{}';
    headerFieldLabels.value = {};
    headerEditableFields.value = [];
    headerEditableFieldsConfigured.value = false;
    previewFormValues.value = {};
    designerMode.value = 'design';
    resetDesignerNode();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;

    const data = modalApi.getData<MesHcStationFormApi.StationForm>();
    if (!data?.id) {
      const cloneSource =
        data &&
        (data.formCode ||
          data.formName ||
          data.schemaJson ||
          data.items?.length ||
          data.presetItems?.length)
          ? data
          : undefined;
      const cloneItems = (
        cloneSource?.items ||
        cloneSource?.presetItems ||
        []
      ).map((item) => ({
        ...item,
        formId: undefined,
        id: undefined,
      }));
      const draft = cloneSource
        ? {
            ...cloneSource,
            createTime: undefined,
            id: undefined,
            items: cloneItems,
            presetItems: cloneItems,
          }
        : undefined;
      formData.value = draft;
      formulaSimpleValues.value = { ...draft };
      itemRows.value = (draft?.items || draft?.presetItems || []).map((item) =>
        normalizeRow(item),
      );
      normalizeLoadedFormulaModes();
      if (!itemRows.value.length && !isWetSemiFinishedTemplateConfig()) {
        itemRows.value = [normalizeRow()];
      }
      presetHeaderDataText.value = draft?.presetHeaderDataJson || '{}';
      previewFormValues.value = draft || {};
      designerMode.value = 'design';
      resetDesignerNode();
      syncSchemaEditor(draft?.schemaJson || '');
      syncPresetRowsFromItems();
      syncPresetHeaderDataMap(draft?.presetHeaderDataJson || '{}');
      if (isFormulaSimple.value) return;
      await formApi.resetForm();
      await formApi.setValues(
        draft || {
          needConfirm: true,
          sortNo: 10,
          status: 1,
        },
      );
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getStationFormDetail(data.id);
      formulaSimpleValues.value = { ...formData.value };
      itemRows.value = (formData.value.items || []).map((item) =>
        normalizeRow(item),
      );
      normalizeLoadedFormulaModes();
      if (!itemRows.value.length && !isWetSemiFinishedTemplateConfig()) {
        itemRows.value = [normalizeRow()];
      }
      syncPresetRowsFromItems();
      presetHeaderDataText.value = formData.value.presetHeaderDataJson || '{}';
      previewFormValues.value = formData.value;
      designerMode.value = 'design';
      resetDesignerNode();
      syncSchemaEditor(formData.value.schemaJson);
      syncPresetHeaderDataMap(formData.value.presetHeaderDataJson || '{}');
      if (!isFormulaSimple.value) await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal>
    <div class="station-form-workbench">
      <div class="pp-plan-toolbar">
        <div class="pp-plan-toolbar__title">
          <span class="pp-plan-toolbar__main">{{ getTitle }}</span>
          <span class="pp-plan-toolbar__sub"
            >维护动态表单基础信息、表头结构、明细模板与预览效果</span
          >
        </div>
        <div class="pp-plan-toolbar__actions">
          <input
            ref="previewExcelInputRef"
            accept=".xlsx,.xls"
            class="station-form-preview-excel-input"
            type="file"
            @change="handleImportPreviewExcelChange"
          />
          <RadioGroup
            v-model:value="designerMode"
            size="small"
            button-style="solid"
          >
            <Radio.Button value="design">设计模式</Radio.Button>
            <Radio.Button value="preview">预览模式</Radio.Button>
          </RadioGroup>
          <Button
            v-if="isFormulaForm && !isDevStationFormConfig()"
            size="small"
            @click="toggleFormulaAdvanced"
            >{{ isFormulaSimple ? '高级配置' : '简洁配置' }}</Button
          >
          <Button
            v-if="!isFormulaSimple"
            size="small"
            :loading="previewExcelLoading"
            @click="handleExportPreviewExcel"
          >
            <IconifyIcon icon="lucide:download" class="mr-1" />
            导出预览Excel
          </Button>
          <Button
            v-if="!isFormulaSimple"
            size="small"
            :loading="previewExcelLoading"
            @click="handleImportPreviewExcelClick"
          >
            <IconifyIcon icon="lucide:upload" class="mr-1" />
            导入预览Excel
          </Button>
          <Button size="small" @click="modalApi.close">关闭</Button>
          <Button size="small" type="primary" @click="handleSaveClick"
            >保存</Button
          >
        </div>
      </div>

      <div class="pp-plan-body">
        <div
          v-if="
            isFormulaForm && (formulaAdvanced || designerMode === 'preview')
          "
          class="formula-config-boundary"
        >
          {{
            designerMode === 'preview'
              ? '配置预览：检查明细来自当前配置；表头、布局及必填效果不等同于配料报工现场。'
              : '高级配置：必填、填写方式和需确认控制新开工任务；在制任务与历史记录沿用自身快照。表头布局、预设值与运行布局不直接控制配料报工。'
          }}
        </div>
        <FormulaSimpleEditor
          v-if="designerMode === 'design' && isFormulaSimple"
          v-model:values="formulaSimpleValues"
          v-model:model-prefix="schemaEditor.modelPrefix"
          :items="itemRows"
          v-model:model-scope="schemaEditor.modelScope"
          v-model:model-code="schemaEditor.modelCode"
          @update-item="updateFormulaItem"
          @add="appendFormulaItem()"
          @copy="appendFormulaItem"
          @advanced="toggleFormulaAdvanced"
        />
        <PressSlotRuntimeLayoutIde
          v-else-if="designerMode === 'design' && isRuntimeLayoutDevPreview"
          :form-name="previewFormValues.formName || formData?.formName"
          :header-data="presetHeaderDataMap"
          :items="previewRows"
          :schema="currentSchemaObject"
          @update:header-data="handlePressSlotRuntimeHeaderDataUpdate"
          @update:schema="handlePressSlotRuntimeSchemaUpdate"
        />

        <div
          v-else-if="
            !isFormulaSimple && (designerMode === 'design' || isFormulaStandard)
          "
          v-show="designerMode === 'design'"
          class="station-form-ide"
        >
          <aside class="station-form-ide__objects">
            <div class="station-form-ide__pane-title">Object TreeView</div>
            <button
              class="station-form-object-node station-form-object-node--root"
              :class="{
                'station-form-object-node--active':
                  isDesignerNodeActive('form'),
              }"
              type="button"
              @click="selectDesignerNode('form')"
            >
              <IconifyIcon icon="lucide:panel-top" />
              <span>Form1</span>
            </button>
            <button
              class="station-form-object-node"
              :class="{
                'station-form-object-node--active':
                  isDesignerNodeActive('schema'),
              }"
              type="button"
              @click="selectDesignerNode('schema')"
            >
              <IconifyIcon icon="lucide:braces" />
              <span>表单结构与模板</span>
            </button>
            <div class="station-form-object-group">
              <span>表头字段</span>
              <button
                v-for="field in presetHeaderFieldKeys"
                :key="field"
                class="station-form-object-node station-form-object-node--child"
                :class="{
                  'station-form-object-node--active': isDesignerNodeActive(
                    'header',
                    field,
                  ),
                }"
                type="button"
                @click="selectDesignerNode('header', field)"
              >
                <IconifyIcon icon="lucide:text-cursor-input" />
                <span>{{ getPresetHeaderFieldMeta(field).label }}</span>
              </button>
            </div>
            <div class="station-form-object-group">
              <button
                class="station-form-object-node"
                :class="{
                  'station-form-object-node--active':
                    isDesignerNodeActive('items'),
                }"
                type="button"
                @click="selectDesignerNode('items')"
              >
                <IconifyIcon icon="lucide:table-2" />
                <span>DetailGrid</span>
              </button>
              <button
                v-for="(row, index) in (isAdhesiveMiddleTemplate ? [] : itemRows)"
                :key="row._rowKey"
                class="station-form-object-node station-form-object-node--child"
                :class="{
                  'station-form-object-node--active': isDesignerNodeActive(
                    'item',
                    row._rowKey,
                  ),
                }"
                type="button"
                @click="selectItemRow(row, index)"
              >
                <IconifyIcon icon="lucide:rows-3" />
                <span>{{ row.itemName || `DetailItem${index + 1}` }}</span>
              </button>
            </div>
          </aside>

          <section class="station-form-ide__workspace">
            <div class="station-form-ide-window">
              <div class="station-form-ide-window__titlebar">
                <span>Form Designer</span>
                <strong>{{ selectedDesignerNodeTitle }}</strong>
              </div>
              <Tabs
                v-model:activeKey="activeTab"
                class="station-form-tabs station-form-tabs--ide"
              >
                <Tabs.TabPane key="basic" tab="基础信息">
                  <div class="station-form-tab-pane">
                    <fieldset
                      class="pp-fieldset station-form-tab-fieldset station-form-designer-zone"
                      :class="{
                        'station-form-designer-zone--active':
                          isDesignerNodeActive('form'),
                      }"
                      @click.stop="selectDesignerNode('form')"
                    >
                      <legend>1. 基础信息</legend>
                      <div class="station-form-form-wrap">
                        <Form />
                      </div>
                    </fieldset>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="schema" tab="表单结构与模板">
                  <div class="station-form-tab-pane">
                    <fieldset
                      class="pp-fieldset station-form-tab-fieldset station-form-designer-zone"
                      :class="{
                        'station-form-designer-zone--active':
                          isDesignerNodeActive('schema'),
                      }"
                      @click.stop="selectDesignerNode('schema')"
                    >
                      <legend>2. 表单结构与模板配置</legend>
                      <div class="station-form-definition-note">
                        这里配置的是表单结构定义，不是数据源；它决定表头、布局、默认行、专用记录单模板和适用产品型号，真实数据来自计划、批次、设备、人员和现场填写。
                      </div>
                      <div class="station-form-schema-grid">
                        <div class="station-form-config-grid">
                          <div class="station-form-config-item">
                            <label>结构模板</label>
                            <Select
                              v-model:value="schemaEditor.presetTemplate"
                              :options="PRESET_TEMPLATE_OPTIONS"
                              allow-clear
                              @update:value="applySchemaPreset"
                            />
                          </div>
                          <div class="station-form-config-item">
                            <label>适用范围</label>
                            <Select
                              v-model:value="schemaEditor.modelScope"
                              :options="MODEL_SCOPE_OPTIONS"
                            />
                          </div>
                          <div
                            v-if="schemaEditor.modelScope === 'MODEL'"
                            class="station-form-config-item"
                          >
                            <label>适用型号</label>
                            <Input
                              v-model:value="schemaEditor.modelCode"
                              placeholder="如：W36P0100"
                            />
                          </div>
                          <div
                            v-else-if="schemaEditor.modelScope === 'PREFIX'"
                            class="station-form-config-item"
                          >
                            <label>型号前缀</label>
                            <Input
                              v-model:value="schemaEditor.modelPrefix"
                              placeholder="如：W36P"
                            />
                          </div>
                          <div v-else class="station-form-config-item">
                            <label>通用说明</label>
                            <Input
                              :value="'未命中指定型号或前缀时使用'"
                              readonly
                            />
                          </div>
                          <div class="station-form-config-item">
                            <label>表头布局</label>
                            <Select
                              v-model:value="schemaEditor.headerLayout"
                              :options="HEADER_LAYOUT_OPTIONS"
                            />
                          </div>
                          <div class="station-form-config-item">
                            <label>来源型号</label>
                            <Input
                              v-model:value="schemaEditor.sourceModelCode"
                              placeholder="如：W26P0100，可记录原始模板型号"
                            />
                          </div>
                          <div
                            class="station-form-config-item station-form-config-item--full"
                          >
                            <label>表头字段</label>
                            <Input
                              v-model:value="schemaEditor.headerFields"
                              placeholder="填写字段编码，多个用英文逗号分隔，如：materialCode,modelCode,batchNo"
                            />
                          </div>
                          <div class="station-form-config-item">
                            <label>模板版本</label>
                            <Input
                              v-model:value="schemaEditor.version"
                              placeholder="如：wet-process-v1"
                            />
                          </div>
                          <div v-if="!isAdhesiveMiddleTemplate" class="station-form-config-item">
                            <label>
                              {{
                                isWetSemiFinishedHeaderConfig()
                                  ? '默认行数（自动）'
                                  : '默认行数'
                              }}
                            </label>
                            <InputNumber
                              v-if="isWetSemiFinishedHeaderConfig()"
                              :value="effectiveDefaultRowCount"
                              :min="1"
                              :precision="0"
                              readonly
                              class="w-full"
                            />
                            <InputNumber
                              v-else
                              v-model:value="schemaEditor.defaultRowCount"
                              :min="1"
                              :precision="0"
                              class="w-full"
                            />
                          </div>
                          <div v-if="!isAdhesiveMiddleTemplate" class="station-form-config-item">
                            <label>行步长</label>
                            <InputNumber
                              v-model:value="schemaEditor.rowStep"
                              :min="1"
                              :precision="0"
                              class="w-full"
                            />
                          </div>
                          <div v-if="!isAdhesiveMiddleTemplate" class="station-form-config-item">
                            <label>默认生成长度</label>
                            <InputNumber
                              v-model:value="
                                schemaEditor.defaultGeneratedLength
                              "
                              :min="1"
                              :precision="3"
                              class="w-full"
                            />
                          </div>
                        </div>
                        <div v-if="isAdhesiveMiddleTemplate" class="station-form-preview-empty">按加工米数向上取整，每米一行，最多600行；无有效加工米数时默认50行。已有测量记录不因重新打开而重建。</div>
                        <div class="station-form-json-panel">
                          <div class="pp-panel__header">
                            <span>结构定义 JSON 预览（技术字段）</span>
                          </div>
                          <div class="station-form-json-panel__body">
                            <Input.TextArea
                              :value="schemaPreview"
                              :rows="13"
                              readonly
                            />
                          </div>
                        </div>
                      </div>
                    </fieldset>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="preset" tab="预设值预览">
                  <div
                    class="station-form-tab-pane station-form-tab-pane--preset"
                  >
                    <div class="station-form-preset-grid">
                      <div class="station-form-json-panel">
                        <div class="pp-panel__header">
                          <span>表头预设值</span>
                        </div>
                        <div class="station-form-json-panel__body">
                          <div class="station-form-header-form-grid">
                            <div
                              v-for="field in presetHeaderFieldKeys"
                              :key="field"
                              class="station-form-config-item station-form-designer-zone"
                              :class="{
                                'station-form-designer-zone--active':
                                  isDesignerNodeActive('header', field),
                              }"
                              @click.stop="selectDesignerNode('header', field)"
                            >
                              <template v-if="isWetHeaderCaptionConfig()">
                                <label>显示名称</label>
                                <Input
                                  :value="getPresetHeaderFieldMeta(field).label"
                                  @update:value="
                                    (value) =>
                                      updatePresetHeaderFieldLabel(field, value)
                                  "
                                />
                                <label>预设值</label>
                              </template>
                              <label v-else>{{
                                getPresetHeaderFieldMeta(field).label
                              }}</label>
                              <Select
                                v-if="
                                  getPresetHeaderFieldMeta(field).type ===
                                  'select'
                                "
                                :value="presetHeaderDataMap[field]"
                                :options="
                                  getPresetHeaderFieldMeta(field).options
                                "
                                allow-clear
                                @update:value="
                                  (value) =>
                                    updatePresetHeaderField(field, value)
                                "
                              />
                              <InputNumber
                                v-else-if="
                                  getPresetHeaderFieldMeta(field).type ===
                                  'number'
                                "
                                :value="presetHeaderDataMap[field]"
                                :controls="false"
                                class="w-full"
                                @update:value="
                                  (value) =>
                                    updatePresetHeaderField(field, value)
                                "
                              />
                              <Input.TextArea
                                v-else-if="isAdhesiveMiddleTemplate && ADHESIVE_INTERMEDIATE_NOTES.some((note) => note.key === field)"
                                :value="presetHeaderDataMap[field]" :rows="3"
                                @update:value="(value) => updatePresetHeaderField(field, value)"
                              />
                              <Input
                                v-else
                                :readonly="isAdhesiveMiddleTemplate && ['recorder', 'recorderTime', 'confirmer', 'confirmerTime'].includes(field)"
                                :placeholder="isAdhesiveMiddleTemplate && ['recorder', 'recorderTime', 'confirmer', 'confirmerTime'].includes(field) ? '由实际操作生成' : ''"
                                :value="presetHeaderDataMap[field]"
                                @update:value="
                                  (value) =>
                                    updatePresetHeaderField(field, value)
                                "
                              />
                            </div>
                          </div>
                        </div>
                      </div>

                      <div class="station-form-json-panel">
                        <div class="pp-panel__header">
                          <span>表头预设值 JSON 预览</span>
                        </div>
                        <div class="station-form-json-panel__body">
                          <Input.TextArea
                            :value="presetHeaderPreview"
                            :rows="14"
                            readonly
                          />
                        </div>
                      </div>

                      <div v-if="!isAdhesiveMiddleTemplate" class="pp-panel station-form-items-panel">
                        <div class="pp-panel__header">
                          <span>明细预览模板（根据明细项配置自动生成）</span>
                        </div>
                        <div class="pp-table-wrap station-form-table-wrap">
                          <table class="pp-grid station-form-item-table">
                            <thead>
                              <tr>
                                <th width="90">序号</th>
                                <th width="120">分类</th>
                                <th width="140">步骤节点</th>
                                <th width="220">项目名称</th>
                                <th width="240">标准说明</th>
                                <th width="140">值模式</th>
                                <th width="120">标签1</th>
                                <th width="120">标签2</th>
                                <th width="120">默认结果</th>
                                <th width="100">必填</th>
                                <th width="180">备注</th>
                                <th width="90">操作</th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr
                                v-for="(row, index) in presetRows"
                                :key="row._rowKey"
                              >
                                <td>{{ row.itemSeq || index + 1 }}</td>
                                <td>{{ row.itemCategory || '-' }}</td>
                                <td>{{ row.stepNode || '-' }}</td>
                                <td>{{ row.itemName || '-' }}</td>
                                <td>{{ row.standardText || '-' }}</td>
                                <td>{{ row.valueMode || '-' }}</td>
                                <td>{{ row.dualLabel1 || '-' }}</td>
                                <td>{{ row.dualLabel2 || '-' }}</td>
                                <td>{{ row.defaultResult || '-' }}</td>
                                <td
                                  class="station-form-item-table__switch-cell"
                                >
                                  {{ row.requiredFlag ? '是' : '否' }}
                                </td>
                                <td>{{ row.remark || '-' }}</td>
                                <td
                                  class="station-form-item-table__action-cell"
                                >
                                  自动生成
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="items" tab="明细项配置">
                  <div v-if="isAdhesiveMiddleTemplate" class="pp-panel">
                    <div class="pp-panel__header">粘胶1中间品厚度列标题</div>
                    <div class="pp-table-wrap">
                      <table class="pp-grid station-form-item-table">
                        <thead><tr><th>测量位置</th><th>列标题（含单位、公差）</th></tr></thead>
                        <tbody><tr v-for="(label, index) in (adhesiveThicknessLabelsEdited ?? adhesiveThicknessLabels)" :key="index">
                          <td>{{ index === 0 ? '左侧厚度' : '右侧厚度' }}</td>
                          <td><Input :value="label" @update:value="(value) => updateAdhesiveThicknessLabel(index, value)" /></td>
                        </tr></tbody>
                      </table>
                      <div class="station-form-preview-empty">长度/m、备注为固定列。公差只作标题显示，不自动判定超差；历史记录使用当前模板标题。</div>
                      <div class="station-form-preview-empty">按加工米数向上取整，每米一行，最多600行；无有效加工米数时默认50行。已有测量记录不因重新打开而重建。</div>
                    </div>
                  </div>
                  <div v-if="isRoughMiddleTemplate" class="pp-panel">
                    <div class="pp-panel__header">磨皮中间品厚度列标题</div>
                    <div class="pp-table-wrap">
                      <table class="pp-grid station-form-item-table">
                        <thead><tr><th>测量位置</th><th>列标题（含单位、公差）</th></tr></thead>
                        <tbody>
                          <tr v-for="(label, index) in (roughThicknessLabelsEdited ?? roughThicknessLabels)" :key="index">
                            <td>{{ index === 0 ? '左侧厚度' : '右侧厚度' }}</td>
                            <td><Input :value="label" :aria-label="index === 0 ? '左侧厚度列标题' : '右侧厚度列标题'" @update:value="(value) => updateRoughThicknessLabel(index, value)" /></td>
                          </tr>
                        </tbody>
                      </table>
                      <div class="station-form-preview-empty">公差用于标题显示，不自动判断超差。保存后重新打开记录可见新标题，历史记录也使用当前模板标题。</div>
                    </div>
                  </div>
                  <div
                    v-if="isWetSemiFinishedTemplateConfig()"
                    class="station-form-tab-pane station-form-tab-pane--items"
                  >
                    <div class="pp-panel station-form-items-panel">
                      <div class="pp-panel__header">
                        <span>3. 专用长度明细</span>
                      </div>
                      <div class="pp-table-wrap station-form-table-wrap">
                        <div class="station-form-preview-empty">
                          长度明细按“表单结构与模板”中的默认生成长度和行步长自动生成。
                          “长度/m”和“备注”为固定列；下方可维护四个厚度列的测量位置、单位和公差文案。
                        </div>
                        <table class="pp-grid station-form-item-table">
                          <thead>
                            <tr><th width="100">测量列</th><th>列标题（含单位、公差）</th></tr>
                          </thead>
                          <tbody>
                            <tr v-for="(label, index) in wetThicknessLabels" :key="index">
                              <td>第 {{ index + 1 }} 列</td>
                              <td>
                                <Input
                                  :value="label"
                                  :aria-label="`第 ${index + 1} 个厚度列标题`"
                                  placeholder="例如：左侧10cm出槽厚度/mm（1.150±0.040mm）"
                                  @update:value="(value) => updateWetThicknessLabel(index, value)"
                                />
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <div class="station-form-preview-empty">
                          公差为表头说明，保存后用于报工显示，不会自动判定厚度是否超差。
                          列顺序对应原有四个测量值，请保持测量位置与历史记录含义一致。
                        </div>
                      </div>
                    </div>
                  </div>
                  <div
                    v-else-if="!isAdhesiveMiddleTemplate"
                    class="station-form-tab-pane station-form-tab-pane--items"
                  >
                    <div
                      class="pp-panel station-form-items-panel station-form-designer-zone"
                      :class="{
                        'station-form-designer-zone--active':
                          isDesignerNodeActive('items'),
                      }"
                      @click.stop="selectDesignerNode('items')"
                    >
                      <div class="pp-panel__header">
                        <span>3. 明细项配置</span>
                        <Button
                          size="small"
                          type="dashed"
                          @click.stop="addItemRow"
                        >
                          <IconifyIcon icon="lucide:plus" class="mr-1" />
                          新增明细项
                        </Button>
                      </div>
                      <div class="pp-table-wrap station-form-table-wrap">
                        <table class="pp-grid station-form-item-table">
                          <thead>
                            <tr>
                              <th width="90">序号</th>
                              <th width="120">分类</th>
                              <th width="140">步骤节点</th>
                              <th width="220">项目名称</th>
                              <th width="240">标准说明</th>
                              <th width="140">值模式</th>
                              <th width="120">标签1</th>
                              <th width="120">标签2</th>
                              <th width="120">默认结果</th>
                              <th width="100">必填</th>
                              <th width="180">备注</th>
                              <th width="180">操作</th>
                            </tr>
                          </thead>
                          <tbody>
                            <template
                              v-for="(row, index) in (isAdhesiveMiddleTemplate ? [] : itemRows)"
                              :key="row._rowKey"
                            >
                            <tr
                              :class="{
                                'station-form-detail-row--active':
                                  isDesignerNodeActive('item', row._rowKey),
                              }"
                              @click="selectItemRow(row, index)"
                            >
                              <td>
                                <InputNumber
                                  v-model:value="row.itemSeq"
                                  :controls="false"
                                  :min="1"
                                  :precision="0"
                                  class="w-full"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.itemCategory"
                                  placeholder="如：放料/涂布"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.stepNode"
                                  placeholder="如：生产前/生产中"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.itemName"
                                  placeholder="请输入项目名称"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.standardText"
                                  placeholder="请输入标准说明"
                                />
                              </td>
                              <td>
                                <Select
                                  v-model:value="row.valueMode"
                                  :options="isFormulaStandard && getFormulaCategory(formData || {}) === 'production' ? [...VALUE_MODE_OPTIONS, { label: '多字段填写', value: 'MULTI_FIELDS' }] : VALUE_MODE_OPTIONS"
                                  placeholder="请选择值模式"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.dualLabel1"
                                  placeholder="标签1"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.dualLabel2"
                                  placeholder="标签2"
                                />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.defaultResult"
                                  placeholder="如：OK"
                                />
                              </td>
                              <td class="station-form-item-table__switch-cell">
                                <span v-if="row.valueMode === 'MULTI_FIELDS'">按子字段</span>
                                <Switch v-else v-model:checked="row.requiredFlag" />
                              </td>
                              <td>
                                <Input
                                  v-model:value="row.remark"
                                  placeholder="备注"
                                />
                              </td>
                              <td class="station-form-item-table__action-cell">
                                <div class="station-form-row-actions">
                                  <Button
                                    size="small"
                                    type="text"
                                    title="上移"
                                    :disabled="index === 0"
                                    @click.stop="moveItemRow(index, -1)"
                                  >
                                    <IconifyIcon icon="lucide:arrow-up" />
                                  </Button>
                                  <Button
                                    size="small"
                                    type="text"
                                    title="下移"
                                    :disabled="index === itemRows.length - 1"
                                    @click.stop="moveItemRow(index, 1)"
                                  >
                                    <IconifyIcon icon="lucide:arrow-down" />
                                  </Button>
                                  <Button
                                    size="small"
                                    type="text"
                                    title="向上插入"
                                    @click.stop="insertItemRow(index)"
                                  >
                                    <IconifyIcon icon="lucide:corner-left-up" />
                                  </Button>
                                  <Button
                                    size="small"
                                    type="text"
                                    title="复制到下方"
                                    @click.stop="copyItemRow(index)"
                                  >
                                    <IconifyIcon icon="lucide:copy" />
                                  </Button>
                                  <Button
                                    size="small"
                                    type="text"
                                    danger
                                    title="删除"
                                    @click.stop="removeItemRow(index)"
                                  >
                                    <IconifyIcon icon="lucide:trash-2" />
                                  </Button>
                                </div>
                              </td>
                            </tr>
                            <tr v-if="isFormulaStandard && row.valueMode === 'MULTI_FIELDS'" class="station-form-multi-detail" @click="selectItemRow(row, index)">
                              <td colspan="12">
                                <div class="station-form-multi-detail__content">
                                  <strong>{{ row.itemName || '当前项目' }} · 子字段配置</strong>
                                  <FormulaMultiFieldEditor v-model:value="row.fieldDefinitionsJson" />
                                </div>
                              </td>
                            </tr>
                            </template>
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                </Tabs.TabPane>
              </Tabs>
            </div>
          </section>

          <aside class="station-form-ide__inspector">
            <div class="station-form-ide__pane-title">
              <span>Object Inspector</span>
              <Button
                size="small"
                type="text"
                title="配置引导"
                @click="guideVisible = !guideVisible"
              >
                <IconifyIcon icon="lucide:circle-help" />
              </Button>
            </div>
            <div class="station-form-inspector__object">
              <strong>{{ selectedDesignerNodeTitle }}</strong>
              <span>{{ selectedDesignerNodeKind }}</span>
            </div>

            <div v-if="guideVisible" class="station-form-config-guide">
              <div class="station-form-config-guide__title">
                <IconifyIcon icon="lucide:lightbulb" />
                <span>配置引导</span>
              </div>
              <div class="station-form-config-guide__section">
                <strong>作用</strong>
                <p>{{ selectedDesignerGuide.purpose }}</p>
              </div>
              <div class="station-form-config-guide__section">
                <strong>例子</strong>
                <p>{{ selectedDesignerGuide.example }}</p>
              </div>
              <div class="station-form-config-guide__section">
                <strong>提示</strong>
                <ul>
                  <li v-for="tip in selectedDesignerGuide.tips" :key="tip">
                    {{ tip }}
                  </li>
                </ul>
              </div>
            </div>

            <div
              v-if="selectedDesignerNode.type === 'form'"
              class="station-form-property-grid"
            >
              <div
                v-for="prop in formPropertyRows"
                :key="prop.name"
                class="station-form-property-row"
              >
                <span>{{ prop.name }}</span>
                <strong>{{ prop.value }}</strong>
              </div>
              <Button size="small" block @click="selectDesignerNode('form')"
                >在设计器中编辑基础信息</Button
              >
            </div>

            <div
              v-else-if="selectedDesignerNode.type === 'schema'"
              class="station-form-property-grid"
            >
              <label>结构模板</label>
              <Select
                v-model:value="schemaEditor.presetTemplate"
                :options="PRESET_TEMPLATE_OPTIONS"
                allow-clear
              />
              <label>适用范围</label>
              <Select
                v-model:value="schemaEditor.modelScope"
                :options="MODEL_SCOPE_OPTIONS"
              />
              <template v-if="schemaEditor.modelScope === 'MODEL'">
                <label>适用型号</label>
                <Input
                  v-model:value="schemaEditor.modelCode"
                  placeholder="如：W36P0100"
                />
              </template>
              <template v-else-if="schemaEditor.modelScope === 'PREFIX'">
                <label>型号前缀</label>
                <Input
                  v-model:value="schemaEditor.modelPrefix"
                  placeholder="如：W36P"
                />
              </template>
              <template v-else>
                <label>通用说明</label>
                <Input :value="'未命中指定型号或前缀时使用'" readonly />
              </template>
              <label>来源型号</label>
              <Input
                v-model:value="schemaEditor.sourceModelCode"
                placeholder="如：W26P0100"
              />
              <label>表头布局</label>
              <Select
                v-model:value="schemaEditor.headerLayout"
                :options="HEADER_LAYOUT_OPTIONS"
              />
              <label>模板版本</label>
              <Input v-model:value="schemaEditor.version" />
              <label>表头字段</label>
              <Input.TextArea
                v-model:value="schemaEditor.headerFields"
                :rows="4"
              />
              <template v-if="!isAdhesiveMiddleTemplate">
              <label>
                {{
                  isWetSemiFinishedHeaderConfig()
                    ? '默认行数（自动）'
                    : '默认行数'
                }}
              </label>
              <InputNumber
                v-if="isWetSemiFinishedHeaderConfig()"
                :value="effectiveDefaultRowCount"
                :min="1"
                :precision="0"
                readonly
                class="w-full"
              />
              <InputNumber
                v-else
                v-model:value="schemaEditor.defaultRowCount"
                :min="1"
                :precision="0"
                class="w-full"
              />
              <label>行步长</label>
              <InputNumber
                v-model:value="schemaEditor.rowStep"
                :min="1"
                :precision="0"
                class="w-full"
              />
              <label>默认生成长度</label>
              <InputNumber
                v-model:value="schemaEditor.defaultGeneratedLength"
                :min="1"
                :precision="3"
                class="w-full"
              />
              </template>
              <div
                v-for="prop in schemaPropertyRows"
                :key="prop.name"
                class="station-form-property-row"
              >
                <span>{{ prop.name }}</span>
                <strong>{{ prop.value }}</strong>
              </div>
            </div>

            <div
              v-else-if="selectedDesignerNode.type === 'header'"
              class="station-form-property-grid"
            >
              <label>fieldKey</label>
              <Input :value="selectedHeaderFieldKey" readonly />
              <label>caption</label>
              <Input
                :value="
                  selectedHeaderFieldMeta?.label || selectedHeaderFieldKey
                "
                :readonly="!isWetHeaderCaptionConfig() && !isPressSlotProductionConfig"
                @update:value="
                  (value) =>
                    updatePresetHeaderFieldLabel(selectedHeaderFieldKey, value)
                "
              />
              <template v-if="isWetProductionCheckHeaderConfig()">
                <label>现场可编辑</label>
                <Switch
                  :checked="isPresetHeaderFieldEditable(selectedHeaderFieldKey)"
                  :disabled="
                    !isWetProductionCheckTimeHeader(selectedHeaderFieldKey)
                  "
                  checked-children="可编辑"
                  un-checked-children="只读"
                  @update:checked="
                    (checked) =>
                      updatePresetHeaderFieldEditable(
                        selectedHeaderFieldKey,
                        checked,
                      )
                  "
                />
              </template>
              <label>value</label>
              <Select
                v-if="selectedHeaderFieldMeta?.type === 'select'"
                :value="presetHeaderDataMap[selectedHeaderFieldKey]"
                :options="selectedHeaderFieldMeta.options"
                allow-clear
                @update:value="
                  (value) =>
                    updatePresetHeaderField(selectedHeaderFieldKey, value)
                "
              />
              <InputNumber
                v-else-if="selectedHeaderFieldMeta?.type === 'number'"
                :value="presetHeaderDataMap[selectedHeaderFieldKey]"
                :controls="false"
                class="w-full"
                @update:value="
                  (value) =>
                    updatePresetHeaderField(selectedHeaderFieldKey, value)
                "
              />
              <Input
                v-else
                :value="presetHeaderDataMap[selectedHeaderFieldKey]"
                @update:value="
                  (value) =>
                    updatePresetHeaderField(selectedHeaderFieldKey, value)
                "
              />
            </div>

            <div
              v-else-if="
                selectedDesignerNode.type === 'item' && selectedItemRow
              "
              class="station-form-property-grid"
            >
              <label>itemSeq</label>
              <InputNumber
                v-model:value="selectedItemRow.itemSeq"
                :min="1"
                :precision="0"
                class="w-full"
              />
              <label>itemCategory</label>
              <Input v-model:value="selectedItemRow.itemCategory" />
              <label>stepNode</label>
              <Input v-model:value="selectedItemRow.stepNode" />
              <label>itemName</label>
              <Input v-model:value="selectedItemRow.itemName" />
              <label>standardText</label>
              <Input.TextArea
                v-model:value="selectedItemRow.standardText"
                :rows="3"
              />
              <label>valueMode</label>
              <Select
                v-model:value="selectedItemRow.valueMode"
                :options="isFormulaStandard && getFormulaCategory(formData || {}) === 'production' ? [...VALUE_MODE_OPTIONS, { label: '多字段填写', value: 'MULTI_FIELDS' }] : VALUE_MODE_OPTIONS"
              />
              <FormulaMultiFieldEditor v-if="isFormulaStandard && selectedItemRow.valueMode === 'MULTI_FIELDS'"
                v-model:value="selectedItemRow.fieldDefinitionsJson" />
              <label>dualLabel1</label>
              <Input v-model:value="selectedItemRow.dualLabel1" />
              <label>dualLabel2</label>
              <Input v-model:value="selectedItemRow.dualLabel2" />
              <label>defaultResult</label>
              <Input v-model:value="selectedItemRow.defaultResult" />
              <label>requiredFlag</label>
              <Switch v-model:checked="selectedItemRow.requiredFlag" />
              <label>remark</label>
              <Input.TextArea
                v-model:value="selectedItemRow.remark"
                :rows="3"
              />
            </div>

            <div v-else class="station-form-property-grid">
              <div class="station-form-property-row">
                <span>rowCount</span>
                <strong>{{ itemRows.length }}</strong>
              </div>
              <Button size="small" block type="dashed" @click="addItemRow">
                <IconifyIcon icon="lucide:plus" class="mr-1" />
                新增 DetailItem
              </Button>
            </div>
          </aside>
        </div>

        <div
          v-if="designerMode === 'preview'"
          class="station-form-preview-shell"
        >
          <div
            class="station-form-preview-page"
            :class="{
              'station-form-preview-page--runtime': isRuntimeLayoutDevPreview,
            }"
          >
            <div
              v-if="!isRuntimeLayoutDevPreview"
              class="station-form-preview-title"
            >
              {{
                previewFormValues.formName ||
                formData?.formName ||
                '动态表单预览'
              }}
            </div>

            <div
              v-if="!isRuntimeLayoutDevPreview"
              class="station-form-preview-meta"
            >
              <div
                v-for="item in previewMeta"
                :key="item.label"
                class="station-form-preview-meta__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>

            <div
              v-if="!isRuntimeLayoutDevPreview && previewHeaderFields.length"
              class="station-form-preview-header"
              :class="previewHeaderClass"
            >
              <div
                v-for="field in previewHeaderFields"
                :key="field.field"
                class="station-form-preview-header__cell"
              >
                <span>{{ field.label }}</span>
                <strong>{{ field.value || '-' }}</strong>
              </div>
            </div>

            <RoughMiddleProductRecordSheet
              v-if="isPressSlotIntermediatePreview"
              v-model:page="pressSlotIntermediatePreviewPage"
              :attachments="pressSlotIntermediatePreviewLayout.attachments"
              :columns="pressSlotIntermediatePreviewLayout.columns"
              :detail-title="pressSlotIntermediatePreviewLayout.detailTitle"
              :editable="false"
              :head-fields="pressSlotIntermediatePreviewLayout.headFields"
              :meta-items="pressSlotIntermediatePreviewLayout.metaItems"
              :page-size="PRESS_SLOT_INTERMEDIATE_DETAIL_PAGE_SIZE"
              :rows="pressSlotIntermediatePreviewLayout.rows"
              :signature-fields="
                pressSlotIntermediatePreviewLayout.signatureFields
              "
              :title="pressSlotIntermediatePreviewLayout.title"
            >
              <template #extra-sections>
                <fieldset class="press-slot-preview-depth-fieldset">
                  <legend>
                    {{ pressSlotIntermediatePreviewLayout.depthTitle }}
                  </legend>
                  <div class="press-slot-preview-section-hint">
                    {{ pressSlotIntermediatePreviewLayout.depthHint }}
                  </div>
                  <div class="press-slot-preview-depth-matrix">
                    <template
                      v-for="field in pressSlotIntermediatePreviewLayout.depthFields"
                      :key="field.field"
                    >
                      <div
                        class="press-slot-preview-depth-cell press-slot-preview-depth-cell--head"
                      >
                        {{ field.label }}
                      </div>
                    </template>
                    <template
                      v-for="field in pressSlotIntermediatePreviewLayout.depthFields"
                      :key="`${field.field}-value`"
                    >
                      <div class="press-slot-preview-depth-cell">
                        {{ field.value || '待填写' }}
                      </div>
                    </template>
                  </div>
                </fieldset>
                <div
                  class="press-slot-preview-section-hint press-slot-preview-section-hint--detail"
                >
                  {{ pressSlotIntermediatePreviewLayout.detailHint }}
                </div>
              </template>
            </RoughMiddleProductRecordSheet>

            <div v-else-if="isAdhesiveMiddleTemplate" class="station-form-preview-table-wrap">
              <table class="station-form-preview-table">
                <thead><tr><th v-for="column in adhesivePreviewColumns" :key="column.key">{{ column.title }}</th></tr></thead>
                <tbody><tr v-for="row in adhesivePreviewRowCount" :key="row"><td>{{ row }}</td><td></td><td></td><td></td></tr></tbody>
              </table>
            </div>
            <StationFormRuntimeRenderer
              v-else-if="isRuntimeLayoutDevPreview"
              v-model:header-data="presetHeaderDataMap"
              :form-name="previewFormValues.formName || formData?.formName"
              :items="previewRows"
              :record-meta="runtimePreviewMeta"
              :schema="currentSchemaObject"
              compact
              :readonly="!isPressSlotProductionConfig"
              @update:header-data="(value) => { presetHeaderDataText = JSON.stringify(value); }"
            />

            <div v-else class="station-form-preview-table-wrap">
              <table class="station-form-preview-table">
                <thead>
                  <tr>
                    <th width="72">序号</th>
                    <th width="130">分类</th>
                    <th width="140">步骤节点</th>
                    <th width="260">项目名称</th>
                    <th width="340">标准说明</th>
                    <th width="150">值模式</th>
                    <th width="220">记录/默认值</th>
                    <th width="110">必填</th>
                    <th width="220">备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in previewRows" :key="row._rowKey">
                    <td align="center">{{ row.itemSeq }}</td>
                    <td>{{ row.itemCategory || '-' }}</td>
                    <td>{{ row.stepNode || '-' }}</td>
                    <td>{{ row.itemName || '-' }}</td>
                    <td>{{ row.standardText || '-' }}</td>
                    <td>
                      <Tag>{{ getValueModeLabel(row.valueMode) }}</Tag>
                    </td>
                    <td>
                      <span class="station-form-preview-input">{{
                        getPreviewInputText(row) || '待填写'
                      }}</span>
                    </td>
                    <td align="center">{{ row.requiredFlag ? '是' : '否' }}</td>
                    <td>{{ row.remark || '-' }}</td>
                  </tr>
                  <tr v-if="!previewRows.length">
                    <td colspan="9" class="station-form-preview-empty">
                      暂无明细项
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.formula-config-boundary {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #eff6ff;
  color: #475569;
  border-radius: 6px;
  font-size: 12px;
}
.station-form-workbench {
  height: calc(100vh - 48px);
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
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.pp-plan-toolbar__sub {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.2;
}

.pp-plan-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.station-form-preview-excel-input {
  display: none;
}

.pp-plan-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
}

.station-form-ide {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 300px;
  gap: 8px;
  overflow: hidden;
}

.station-form-ide__objects,
.station-form-ide__inspector,
.station-form-ide-window {
  min-height: 0;
  border: 1px solid #b8c2cc;
  background: #f3f4f6;
}

.station-form-ide__objects,
.station-form-ide__inspector {
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.station-form-ide__workspace {
  min-width: 0;
  min-height: 0;
  display: flex;
}

.station-form-ide-window {
  flex: 1;
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.station-form-ide-window__titlebar,
.station-form-ide__pane-title {
  height: 28px;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
  color: #0f172a;
  background: #dbe4ee;
  border-bottom: 1px solid #aab7c4;
  font-size: 12px;
  font-weight: 700;
}

.station-form-ide-window__titlebar strong {
  color: #1d4ed8;
  font-weight: 700;
}

.station-form-ide__pane-title :deep(.ant-btn) {
  width: 24px;
  height: 24px;
  padding: 0;
  color: #334155;
}

.station-form-object-group {
  display: flex;
  flex-direction: column;
  padding: 6px 0;
  border-top: 1px solid #d1d5db;
}

.station-form-object-group > span {
  padding: 4px 10px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.station-form-object-node {
  width: 100%;
  height: 28px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  overflow: hidden;
  color: #1f2937;
  background: transparent;
  border: 0;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.station-form-object-node span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-form-object-node--child {
  padding-left: 22px;
}

.station-form-object-node--root {
  margin-top: 6px;
}

.station-form-object-node:hover,
.station-form-object-node--active {
  color: #fff;
  background: #2563eb;
}

.station-form-inspector__object {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  background: #fff;
  border-bottom: 1px solid #d1d5db;
}

.station-form-inspector__object strong {
  color: #0f172a;
  font-size: 13px;
}

.station-form-inspector__object span {
  color: #64748b;
  font-size: 12px;
}

.station-form-config-guide {
  flex-shrink: 0;
  margin: 8px;
  padding: 8px;
  color: #1f2937;
  background: #fff;
  border: 1px solid #b8c2cc;
  font-size: 12px;
}

.station-form-config-guide__title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 6px;
  color: #1d4ed8;
  border-bottom: 1px solid #dbe4ee;
  font-weight: 700;
}

.station-form-config-guide__section {
  padding-top: 8px;
}

.station-form-config-guide__section strong {
  display: block;
  margin-bottom: 4px;
  color: #0f172a;
}

.station-form-config-guide__section p {
  margin: 0;
  line-height: 1.6;
}

.station-form-config-guide__section ul {
  margin: 0;
  padding-left: 16px;
  line-height: 1.6;
}

.station-form-property-grid {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 0;
  padding: 8px;
}

.station-form-property-grid > label,
.station-form-property-row span {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 8px;
  color: #334155;
  background: #eef2f7;
  border: 1px solid #cbd5e1;
  border-right: 0;
  font-size: 12px;
  font-weight: 700;
}

.station-form-property-grid :deep(.ant-input),
.station-form-property-grid :deep(.ant-input-number),
.station-form-property-grid :deep(.ant-select-selector),
.station-form-property-grid :deep(textarea.ant-input),
.station-form-property-row strong {
  min-height: 32px !important;
  border-color: #cbd5e1 !important;
}

.station-form-property-row {
  display: contents;
}

.station-form-property-row strong {
  display: flex;
  min-width: 0;
  align-items: center;
  padding: 4px 8px;
  overflow: hidden;
  color: #0f172a;
  background: #fff;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-form-property-grid > .ant-btn,
.station-form-property-grid :deep(.ant-btn) {
  grid-column: 1 / -1;
  margin-top: 8px;
}

.station-form-designer-zone {
  outline: 1px dashed transparent;
  outline-offset: -3px;
}

.station-form-designer-zone--active {
  outline-color: #2563eb;
  box-shadow: inset 0 0 0 1px #2563eb;
}

.station-form-detail-row--active td {
  background: #eff6ff;
}

.station-form-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.station-form-tabs--ide {
  padding: 0 8px 8px;
}

.station-form-tabs :deep(.ant-tabs-nav) {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.station-form-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.station-form-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.station-form-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  overflow: hidden;
}

.station-form-tab-pane {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.station-form-tab-pane--items {
  overflow: hidden;
}

.station-form-tab-pane--preset {
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
  font-size: 12px;
  font-weight: 700;
}

.station-form-definition-note {
  flex-shrink: 0;
  margin-bottom: 10px;
  padding: 8px 10px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  line-height: 1.6;
}

.station-form-form-wrap :deep(.vben-form) {
  background: transparent;
}

.station-form-form-wrap :deep(.grid) {
  gap: 8px 16px !important;
}

.station-form-tab-fieldset {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.station-form-schema-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 1fr);
  gap: 12px;
}

.station-form-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.station-form-config-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.station-form-config-item--full {
  grid-column: 1 / -1;
}

.station-form-config-item label {
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.station-form-json-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  background: #fff;
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

.station-form-json-panel__body {
  flex: 1;
  min-height: 0;
  padding: 10px;
}

.station-form-json-panel__body :deep(textarea.ant-input) {
  height: 100% !important;
  resize: none;
}

.station-form-preset-grid {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(320px, 1fr) minmax(0, 1.2fr);
  gap: 12px;
}

.station-form-header-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.station-form-items-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  background: #fff;
  overflow: hidden;
}

.station-form-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.station-form-multi-detail > td {
  background: #f5f8fc;
  padding: 12px 16px;
}

.station-form-multi-detail__content {
  width: 100%;
  max-width: 960px;
  min-width: 0;
}

.station-form-item-table {
  width: 100%;
  min-width: 1780px;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 6px 8px;
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

.station-form-item-table__switch-cell,
.station-form-item-table__action-cell {
  text-align: center;
}

.station-form-row-actions {
  display: inline-grid;
  grid-template-columns: repeat(5, 28px);
  gap: 4px;
  align-items: center;
  justify-content: center;
}

.station-form-row-actions :deep(.ant-btn) {
  width: 28px;
  height: 28px;
  padding: 0;
}

.station-form-preview-shell {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 10px;
}

.station-form-preview-page {
  min-width: 1080px;
  min-height: 100%;
  padding: 16px;
  color: #172033;
  background: #fff;
  border: 1px solid #d1d5db;
}

.station-form-preview-page--runtime {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1180px;
  min-height: 720px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #8794a4;
}

.station-form-preview-title {
  padding: 4px 0 12px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
  text-align: center;
}

.station-form-preview-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 10px;
  border-top: 1px solid #cbd5e1;
  border-left: 1px solid #cbd5e1;
}

.station-form-preview-meta__item,
.station-form-preview-header__cell {
  display: flex;
  min-width: 0;
  min-height: 36px;
  align-items: stretch;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.station-form-preview-meta__item span,
.station-form-preview-header__cell span {
  display: flex;
  flex: 0 0 92px;
  align-items: center;
  padding: 0 8px;
  color: #334155;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
}

.station-form-preview-meta__item strong,
.station-form-preview-header__cell strong {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  padding: 0 8px;
  overflow: hidden;
  color: #172033;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-form-preview-header {
  display: grid;
  margin-bottom: 12px;
  border-top: 1px solid #cbd5e1;
  border-left: 1px solid #cbd5e1;
}

.station-form-preview-header--cols-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.station-form-preview-header--cols-3,
.station-form-preview-header--top {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.station-form-preview-header--cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.station-form-preview-table-wrap {
  overflow: auto;
  border: 1px solid #cbd5e1;
}

.station-form-preview-table {
  width: 100%;
  min-width: 1460px;
  border-collapse: collapse;
  table-layout: fixed;
}

.station-form-preview-table th,
.station-form-preview-table td {
  padding: 7px 8px;
  border: 1px solid #cbd5e1;
  color: #172033;
  vertical-align: middle;
}

.station-form-preview-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #eef2f7;
  font-weight: 700;
  text-align: center;
}

.station-form-preview-input {
  display: inline-flex;
  min-width: 110px;
  min-height: 28px;
  align-items: center;
  padding: 0 8px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #d1d5db;
}

.station-form-preview-empty {
  height: 80px;
  color: #64748b;
  text-align: center;
}

.press-slot-preview-depth-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.press-slot-preview-depth-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
}

.press-slot-preview-section-hint {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.press-slot-preview-section-hint--detail {
  flex-shrink: 0;
  padding: 0 10px 8px;
  margin: -2px 0 0;
  color: #475569;
}

.press-slot-preview-depth-matrix {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.press-slot-preview-depth-cell {
  display: flex;
  min-width: 0;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  padding: 5px 8px;
  overflow: hidden;
  color: #172033;
  background: #fff;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.press-slot-preview-depth-cell--head {
  color: #334155;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  font-weight: 800;
}

:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-select-selector),
:deep(.ant-btn),
:deep(.ant-input-number-affix-wrapper),
:deep(.ant-input-affix-wrapper),
:deep(.ant-input-group-addon),
:deep(.ant-input-number-group-addon),
:deep(textarea.ant-input) {
  border-radius: 0 !important;
}

:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-select-selector),
:deep(textarea.ant-input) {
  min-height: 32px !important;
}

:deep(.ant-input-number),
:deep(.ant-select-selector) {
  width: 100%;
}

:deep(.ant-input-number-input) {
  height: 30px !important;
}

:deep(.ant-btn-link) {
  padding: 0;
}
</style>

<style>
.hc-station-form-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>
