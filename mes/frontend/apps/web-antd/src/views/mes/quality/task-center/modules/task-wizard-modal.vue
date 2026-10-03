<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesQualityTaskCenterApi } from '#/api/mes/quality/task-center';
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';
import type { SystemUserApi } from '#/api/system/user';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { formatDate } from '@vben/utils';

import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Radio,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { getMesMaterialPage } from '#/api/mes/base/material';
import { getFinishedGlueBoardMapModelOptions } from '#/api/mes/hc/finishedglueboardmap';
import { getProductModelPage } from '#/api/mes/hc/productmodel';
import { getWorkCenterPage } from '#/api/mes/hc/workcenter';
import { parseEntryRuleParams } from '#/api/mes/quality/entry-rule';
import {
  rejectProductAbnormalEventRecheck,
  type MesProductAbnormalEventApi,
} from '#/api/mes/quality/abnormal/product-event';
import {
  createQualityTaskByWizard,
  getQualityTaskSourceItemTree,
  getQualityTaskSourcePage,
  getQualityTaskStandardItems,
  getQualityTaskStandardPage,
} from '#/api/mes/quality/task-center';
import { UserSelectModal } from '#/views/system/user/components';

defineOptions({ name: 'QmsQualityTaskWizardModal' });

const emit = defineEmits<{ success: [taskId: number] }>();

type CandidateItem = MesQualityTaskCenterApi.CandidateItem;
type CheckType = MesQualityTaskCenterApi.CheckType;
type SourceRecord = MesQualityTaskCenterApi.SourceRecord;
type StandardRecord = MesQualityTaskCenterApi.Standard;

interface ProductEventRecheckContext {
  abnormalSummary?: string;
  checkType: CheckType;
  inspectionId: number;
  inspectionNo?: string;
  inspectionScene: string;
  mode: 'PRODUCT_EVENT_RECHECK';
}

interface CandidateMeasuredSample {
  [key: string]: unknown;
}

interface CandidateMeasuredDataPayload {
  [key: string]: unknown;
  averageValue?: unknown;
  itemType?: string;
  maxValue?: unknown;
  maxValueLimit?: unknown;
  minValue?: unknown;
  minValueLimit?: unknown;
  renderType?: string;
  values: CandidateMeasuredSample[];
}

const MEASURED_DATA_RENDERED_KEYS = new Set([
  'value',
  'sampleValue',
  'actualValue',
  'measuredValue',
  'resultValue',
  'qualitativeValue',
  'rawValue',
  'samplePositionName',
  'samplePosition',
  'samplePositionCode',
  'sampleGroupNo',
  'sampleColumnNo',
  'sampleBarcode',
  'sampleResult',
  'remark',
]);
const MEASURED_DATA_RENDER_TYPE = 'IQC_SAMPLE_VALUES';
const measuredDataPayloadCache = new WeakMap<
  CandidateItem,
  { payload?: CandidateMeasuredDataPayload; raw: string }
>();

const open = ref(false);
const maximized = ref(false);
const loading = ref(false);
const step = ref(0);
const sourceLoading = ref(false);
const standardLoading = ref(false);
const itemLoading = ref(false);
const sourceRows = ref<SourceRecord[]>([]);
const standardRows = ref<StandardRecord[]>([]);
const itemRows = ref<CandidateItem[]>([]);
const selectedSourceId = ref<number>();
const selectedStandardId = ref<number>();
const selectedNodeKeys = ref<string[]>([]);
const expandedNodeKeys = ref<string[]>([]);
const itemKeyword = ref('');
const workCenters = ref<any[]>([]);
const materials = ref<any[]>([]);
const productModels = ref<any[]>([]);
const glueBoardMaps = ref<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[]>([]);
const productEventContext = ref<ProductEventRecheckContext>();

const productEventMode = computed(
  () => productEventContext.value?.mode === 'PRODUCT_EVENT_RECHECK',
);
const deferFaiProjectSelectionToWorkbench = computed(
  () =>
    productEventMode.value &&
    form.taskType === 'RECHECK' &&
    form.checkType === 'FAI',
);
const modalTitle = computed(() =>
  productEventMode.value ? '驳回重检生成复检单' : '新建质量检验任务',
);
const modalWidth = computed(() =>
  maximized.value ? '100vw' : 'calc(100vw - 32px)',
);
const modalWrapClassName = computed(() =>
  ['erp-quality-task-wizard', maximized.value ? 'is-maximized' : '']
    .filter(Boolean)
    .join(' '),
);
const supportedPath = computed(
  () =>
    form.checkType === 'FAI' ||
    form.checkType === 'FQC' ||
    form.checkType === 'GLUE_BOARD_FAI' ||
    form.checkType === 'IQC' ||
    form.checkType === 'OQC' ||
    productEventMode.value,
);

const form = reactive<MesQualityTaskCenterApi.WizardCreateReq>({
  taskType: 'RECHECK',
  checkType: 'FAI',
  sourceMode: 'INSPECTION_RECORD',
  objectMode: 'SOURCE_RECORD',
  sampleSelectionMode: 'SOURCE_ITEMS',
  selectedItemIds: [],
  selectedScopes: [],
  taskInstruction: '',
  priority: 'NORMAL',
  assigneeUserId: 0,
  assigneeUserName: '',
});

const sourceQuery = reactive({
  operationName: undefined as string | undefined,
  batchNo: '',
});
const standardQuery = reactive({
  operationName: undefined as string | undefined,
  productModel: '',
  keyword: '',
});
const sourcePage = reactive({ current: 1, pageSize: 10, total: 0 });
const standardPage = reactive({ current: 1, pageSize: 10, total: 0 });

const sourceSelected = computed(() =>
  sourceRows.value.find((row) => row.id === selectedSourceId.value),
);
const sourceInspectionQty = computed(() => positiveInteger(sourceSelected.value?.checkQty));
const recheckQuantityEditable = computed(
  () =>
    form.taskType === 'RECHECK' &&
    (form.checkType === 'FAI' || form.checkType === 'FQC' || form.checkType === 'OQC') &&
    !productEventMode.value,
);
const standardSelected = computed(() =>
  standardRows.value.find((row) => row.id === selectedStandardId.value),
);

const processOptions = computed(() => {
  const names = new Map<string, any>();
  for (const raw of workCenters.value) {
    const name = getWorkCenterProcessName(raw);
    if (name && !names.has(name)) names.set(name, raw);
  }
  return [...names.entries()].map(([name, raw]) => ({
    label: joinDistinct(getWorkCenterCode(raw), name),
    value: name,
  }));
});
const modelOptions = computed(() =>
  productModels.value
    .map((raw) => ({
      label: joinDistinct(raw.modelCode, raw.modelName),
      value: normalizeText(raw.modelCode || raw.modelName),
    }))
    .filter((item) => item.value),
);
const materialOptions = computed(() =>
  materials.value
    .map((raw) => ({
      label: joinDistinct(raw.code, raw.name),
      value: normalizeText(raw.code),
    }))
    .filter((item) => item.value),
);
const glueBoardModelOptions = computed(() =>
  uniqueOptions(
    glueBoardMaps.value.map((raw) => ({
      label: joinDistinct(raw.glueBoardModel, raw.glueProcessName),
      value: normalizeText(raw.glueBoardModel),
    })),
  ),
);
const glueBoardMaterialOptions = computed(() =>
  uniqueOptions(
    glueBoardMaps.value.map((raw) => ({
      label: joinDistinct(raw.glueBoardMaterialCode, raw.glueBoardMaterialName),
      value: normalizeText(raw.glueBoardMaterialCode),
    })),
  ),
);
const filteredGlueBoardMaterialOptions = computed(() => {
  if (!form.productModelCode) return glueBoardMaterialOptions.value;
  return uniqueOptions(
    glueBoardMaps.value
      .filter((raw) => normalizeText(raw.glueBoardModel) === form.productModelCode)
      .map((raw) => ({
        label: joinDistinct(raw.glueBoardMaterialCode, raw.glueBoardMaterialName),
        value: normalizeText(raw.glueBoardMaterialCode),
      })),
  );
});
const glueOperationOptions = [
  { label: '粘胶1', value: 'GLUE_1' },
  { label: '粘胶2', value: 'GLUE_2' },
];
const glueOperationNameOptions = [
  { label: '粘胶1', value: '粘胶1' },
  { label: '粘胶2', value: '粘胶2' },
];

const stepItems = computed(() =>
  productEventMode.value
    ? [{ title: '来源检验单' }, { title: '选择复检范围' }]
    : [
        { title: '选择检验方式' },
        { title: form.taskType === 'RECHECK' ? '选择检验记录' : '选择检验标准' },
        { title: '选择检验项目' },
        { title: '任务下达' },
      ],
);
const stepCurrent = computed(() => (productEventMode.value ? 1 : step.value));

const faiSourceColumns: TableColumnsType<SourceRecord> = [
  { dataIndex: 'operationName', title: '工序', width: 105 },
  { dataIndex: 'batchNo', title: '产品批次', width: 150 },
  { dataIndex: 'productModel', title: '产品型号', width: 125 },
  { dataIndex: 'checkQty', title: '送检片数', width: 95 },
  { dataIndex: 'judgment', title: '判定结果', width: 95 },
  { dataIndex: 'abnormalSummary', title: '不合格项总结', width: 230 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'inspectionTime',
    title: '检验时间',
    width: 125,
  },
  { dataIndex: 'inspectorName', title: '检验人', width: 100 },
  { dataIndex: 'auditorName', title: '审核人', width: 100 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '审核时间',
    width: 125,
  },
  { dataIndex: 'executionNo', fixed: 'right', title: '检验单号', width: 175 },
];
const fqcSourceColumns: TableColumnsType<SourceRecord> = [
  { dataIndex: 'executionNo', fixed: 'left', title: 'FQC单号', width: 175 },
  { dataIndex: 'workOrderNo', title: '工单号', width: 145 },
  { dataIndex: 'batchNo', title: '母批次号', width: 155 },
  { dataIndex: 'materialCode', title: '物料编码', width: 145 },
  { dataIndex: 'productModel', title: '产品型号', width: 125 },
  { dataIndex: 'checkQty', title: '送检片数', width: 95 },
  { dataIndex: 'okQty', title: 'OK数', width: 80 },
  { dataIndex: 'ngQty', title: 'NG数', width: 80 },
  { dataIndex: 'auditorName', title: '审核人', width: 100 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '审核时间',
    width: 125,
  },
];
const oqcSourceColumns: TableColumnsType<SourceRecord> = [
  { dataIndex: 'executionNo', fixed: 'left', title: 'FQC单号', width: 175 },
  { dataIndex: 'shippingNoticeNo', title: '发货通知单', width: 165 },
  { dataIndex: 'customerName', title: '客户', width: 150 },
  { dataIndex: 'materialCode', title: '物料编码', width: 145 },
  { dataIndex: 'productModel', title: '产品型号', width: 125 },
  { dataIndex: 'checkQty', title: '送检片数', width: 95 },
  { dataIndex: 'okQty', title: 'OK数', width: 80 },
  { dataIndex: 'ngQty', title: 'NG数', width: 80 },
  { dataIndex: 'auditorName', title: '审核人', width: 100 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '审核时间',
    width: 125,
  },
];
const glueBoardSourceColumns: TableColumnsType<SourceRecord> = [
  { dataIndex: 'executionNo', fixed: 'left', title: '检验单号', width: 175 },
  { dataIndex: 'operationName', title: '工序', width: 105 },
  { dataIndex: 'productModel', title: '胶板型号', width: 135 },
  { dataIndex: 'materialCode', title: '胶板料号', width: 150 },
  { dataIndex: 'batchNo', title: '胶板批次', width: 165 },
  { dataIndex: 'checkQty', title: '送检米数(m)', width: 115 },
  { dataIndex: 'judgment', title: '判定结果', width: 95 },
  { dataIndex: 'remark', title: '备注', width: 220 },
  { dataIndex: 'auditorName', title: '审核人', width: 100 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '审核时间',
    width: 125,
  },
];
const iqcSourceColumns: TableColumnsType<SourceRecord> = [
  { dataIndex: 'executionNo', fixed: 'left', title: 'IQC检验单号', width: 180 },
  { dataIndex: 'receiptNo', title: '关联收料单', width: 165 },
  { dataIndex: 'supplierName', title: '供应商名称', width: 170 },
  { dataIndex: 'materialName', title: '物料品名', width: 180 },
  { dataIndex: 'batchNo', title: '批次号', width: 155 },
  { dataIndex: 'arrivalDate', title: '来料日期', width: 115 },
  { dataIndex: 'productionDate', title: '生产日期', width: 115 },
  { dataIndex: 'checkQty', title: '到货量', width: 95 },
  { dataIndex: 'unit', title: '单位', width: 80 },
  { dataIndex: 'auditorName', title: '确认人', width: 105 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '确认时间',
    width: 145,
  },
];
const sourceColumns = computed(() =>
  form.checkType === 'GLUE_BOARD_FAI'
    ? glueBoardSourceColumns
    : form.checkType === 'IQC'
      ? iqcSourceColumns
    : form.checkType === 'OQC'
    ? oqcSourceColumns
    : form.checkType === 'FQC'
      ? fqcSourceColumns
      : faiSourceColumns,
);

const faiStandardColumns: TableColumnsType<StandardRecord> = [
  { dataIndex: 'standardNo', title: '标准编号', width: 170 },
  { dataIndex: 'standardName', title: '标准名称', width: 260 },
  { dataIndex: 'materialCode', title: '物料编号', width: 150 },
  { dataIndex: 'productModelCode', title: '产品型号', width: 145 },
  { dataIndex: 'operationName', title: '工序', width: 130 },
];
const fqcStandardColumns: TableColumnsType<StandardRecord> = [
  { dataIndex: 'standardNo', title: '标准编号', width: 180 },
  { dataIndex: 'standardName', title: '标准名称', width: 300 },
  { dataIndex: 'materialCode', title: '物料编号', width: 160 },
  { dataIndex: 'productModelCode', title: '产品型号', width: 160 },
];
const glueBoardStandardColumns: TableColumnsType<StandardRecord> = [
  { dataIndex: 'standardNo', title: '标准编号', width: 180 },
  { dataIndex: 'standardName', title: '标准名称', width: 320 },
  { dataIndex: 'glueBoardModel', title: '胶板型号', width: 180 },
];
const iqcStandardColumns: TableColumnsType<StandardRecord> = [
  { dataIndex: 'standardNo', title: '标准编号', width: 180 },
  { dataIndex: 'standardName', title: '标准名称', width: 300 },
  { dataIndex: 'materialCode', title: '物料编码', width: 170 },
  { dataIndex: 'materialName', title: '物料名称', width: 220 },
  { dataIndex: 'auditorName', title: '审核人', width: 110 },
  {
    customRender: ({ text }) => dateText(text),
    dataIndex: 'auditTime',
    title: '审核时间',
    width: 145,
  },
];
const standardColumns = computed(() =>
  form.checkType === 'GLUE_BOARD_FAI'
    ? glueBoardStandardColumns
    : form.checkType === 'IQC'
      ? iqcStandardColumns
    : form.checkType === 'FQC' || form.checkType === 'OQC'
    ? fqcStandardColumns
    : faiStandardColumns,
);

const treeColumns = computed<TableColumnsType<CandidateItem>>(() => {
  if (form.checkType === 'IQC' && form.taskType === 'RECHECK') {
    return [
      { dataIndex: 'sort', title: '序号', width: 70 },
      { dataIndex: 'inspectionItem', title: '检验项目', width: 190 },
      { dataIndex: 'unit', title: '单位', width: 80 },
      { dataIndex: 'standardDesc', title: '标准', width: 260 },
      { dataIndex: 'inspectionMethod', title: '检验方法', width: 140 },
      { dataIndex: 'testTool', title: '仪器', width: 140 },
      { dataIndex: 'sampleSize', title: '取样数', width: 90 },
      { dataIndex: 'measuredData', title: '实测数据记录', width: 300 },
      { dataIndex: 'currentResult', title: '判定', width: 90 },
    ];
  }
  if (form.checkType === 'GLUE_BOARD_FAI') {
    const columns: TableColumnsType<CandidateItem> = [
      { dataIndex: 'sort', title: '序号', width: 70 },
      { dataIndex: 'inspectionItem', title: '检验项目 / 层级', width: 190 },
      { dataIndex: 'standardDesc', title: '标准', width: 230 },
      { dataIndex: 'controlLimit', title: '控制限', width: 230 },
    ];
    if (form.taskType === 'RECHECK') {
      columns.push({ dataIndex: 'statistics', title: '统计结果', width: 190 });
    }
    columns.push(
      { dataIndex: 'positionName', title: '位置', width: 120 },
      { dataIndex: 'sampleGroupNo', title: '组别', width: 85 },
      { dataIndex: 'measuredValue', title: '实测值', width: 105 },
      { dataIndex: 'resultValue', title: '结果值', width: 105 },
      { dataIndex: 'currentResult', title: '判定', width: 90 },
    );
    return columns;
  }
  if (form.checkType === 'FAI' && form.taskType === 'RECHECK') {
    return [
      { dataIndex: 'sort', title: '序号', width: 70 },
      { dataIndex: 'inspectionItem', title: '检验项目 / 层级', width: 220 },
      { dataIndex: 'avgControl', title: '平均值内控', width: 150 },
      { dataIndex: 'stdControl', title: '标准差内控', width: 150 },
      { dataIndex: 'positionName', title: '位置', width: 130 },
      { dataIndex: 'sampleGroupNo', title: '组别', width: 85 },
      { dataIndex: 'averageValue', title: '实测值 / 平均值', width: 125 },
      { dataIndex: 'standardDeviation', title: '标准差', width: 105 },
      { dataIndex: 'currentResult', title: '判定', width: 90 },
    ];
  }
  if (form.checkType === 'FAI') {
    return [
      { dataIndex: 'sort', title: '序号', width: 70 },
      { dataIndex: 'inspectionItem', title: '检验项目', width: 180 },
      { dataIndex: 'standardDesc', title: '标准要求', width: 250 },
      { dataIndex: 'unit', title: '单位', width: 80 },
      { dataIndex: 'inspectionMethod', title: '检验方法', width: 145 },
      { dataIndex: 'avgControl', title: '平均内控', width: 140 },
      { dataIndex: 'stdControl', title: '标准内控', width: 140 },
      { dataIndex: 'valueLimit', title: '上下限', width: 145 },
      { dataIndex: 'positionName', title: '位置', width: 125 },
    ];
  }
  if (
    (form.checkType === 'FQC' || form.checkType === 'OQC') &&
    form.taskType === 'RECHECK'
  ) {
    return [
      { dataIndex: 'sort', title: '序号', width: 70 },
      { dataIndex: 'inspectionItem', title: '检验项目', width: 210 },
      { dataIndex: 'standardDesc', title: '标准', width: 300 },
      { dataIndex: 'pieceNo', title: '片号 / 片数', width: 175 },
      { dataIndex: 'currentResult', title: '判定统计 / 结果', width: 210 },
      { dataIndex: 'defectCode', title: '缺陷码', width: 170 },
    ];
  }
  return [
    { dataIndex: 'sort', title: '序号', width: 70 },
    { dataIndex: 'inspectionItem', title: '检验项目', width: 240 },
    { dataIndex: 'standardDesc', title: '标准要求', width: 420 },
    { dataIndex: 'unit', title: '单位', width: 100 },
  ];
});

const filteredItemRows = computed(() =>
  filterTree(sortCandidateTree(itemRows.value), itemKeyword.value),
);
const allItemSelected = computed(() => {
  const keys = collectSelectableKeys(itemRows.value);
  return keys.length > 0 && keys.every((key) => selectedNodeKeys.value.includes(key));
});
const selectionCount = computed(() => selectedNodeKeys.value.length);
const treeRowSelection = computed(() => ({
  checkStrictly: true,
  getCheckboxProps: (record: CandidateItem) => ({ disabled: record.selectable === false }),
  onChange: (keys: Array<number | string>) => handleTreeSelection(keys.map(String)),
  selectedRowKeys: selectedNodeKeys.value,
}));

const [UserSelectModalComp, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function reset() {
  maximized.value = false;
  Object.assign(form, {
    taskType: 'RECHECK',
    checkType: 'FAI',
    sourceMode: 'INSPECTION_RECORD',
    objectMode: 'SOURCE_RECORD',
    sampleSelectionMode: 'SOURCE_ITEMS',
    requiredSampleQty: undefined,
    samplePieceNos: [],
    selectedItemIds: [],
    selectedScopes: [],
    taskInstruction: '',
    priority: 'NORMAL',
    assigneeUserId: 0,
    assigneeUserName: '',
    assigneeDeptId: undefined,
    assigneeDeptName: undefined,
    requiredFinishTime: undefined,
    sourceExecutionId: undefined,
    sourceExecutionNo: undefined,
    triggerSource: undefined,
    inspectionScene: undefined,
    rejectReason: undefined,
    standardId: undefined,
    batchNo: undefined,
    workOrderNo: undefined,
    productModelId: undefined,
    productModelCode: undefined,
    productModelName: undefined,
    materialId: undefined,
    materialCode: undefined,
    materialName: undefined,
    specification: undefined,
    operationId: undefined,
    operationCode: undefined,
    operationName: undefined,
    checkQty: undefined,
    unit: undefined,
    receiptNo: undefined,
    supplierName: undefined,
    arrivalDate: undefined,
  });
  Object.assign(sourceQuery, { operationName: undefined, batchNo: '' });
  Object.assign(standardQuery, { operationName: undefined, productModel: '', keyword: '' });
  Object.assign(sourcePage, { current: 1, pageSize: 10, total: 0 });
  Object.assign(standardPage, { current: 1, pageSize: 10, total: 0 });
  step.value = 0;
  selectedSourceId.value = undefined;
  selectedStandardId.value = undefined;
  selectedNodeKeys.value = [];
  expandedNodeKeys.value = [];
  sourceRows.value = [];
  standardRows.value = [];
  itemRows.value = [];
  itemKeyword.value = '';
  productEventContext.value = undefined;
}

async function show(context?: ProductEventRecheckContext) {
  reset();
  open.value = true;
  await loadPickerOptions();
  if (context?.mode === 'PRODUCT_EVENT_RECHECK') {
    productEventContext.value = context;
    Object.assign(form, {
      taskType: 'RECHECK',
      checkType: context.checkType,
      sourceMode: 'INSPECTION_RECORD',
      objectMode: 'SOURCE_RECORD',
      sampleSelectionMode: 'SOURCE_ITEMS',
      triggerSource: 'PRODUCT_ABNORMAL_EVENT',
      inspectionScene: context.inspectionScene,
      sourceExecutionId: context.inspectionId,
      sourceExecutionNo: context.inspectionNo,
      taskInstruction: context.abnormalSummary
        ? `重点复检：${context.abnormalSummary}`
        : '请按选中项目执行复检',
    });
    selectedSourceId.value = context.inspectionId;
    step.value = 2;
    await loadSourceItems();
  }
}

defineExpose({ open: show });

async function loadPickerOptions() {
  const [workCenterPage, materialPage, modelPage, glue1Options, glue2Options] = await Promise.all([
    getWorkCenterPage({ pageNo: 1, pageSize: 200, status: 0 }),
    getMesMaterialPage({ pageNo: 1, pageSize: 200 } as any),
    getProductModelPage({ pageNo: 1, pageSize: 200, status: 'ENABLE' }),
    getFinishedGlueBoardMapModelOptions({ glueProcess: 'GLUE_1' }),
    getFinishedGlueBoardMapModelOptions({ glueProcess: 'GLUE_2' }),
  ]);
  workCenters.value = workCenterPage.list || [];
  materials.value = materialPage.list || [];
  productModels.value = modelPage.list || [];
  const seen = new Set<string>();
  glueBoardMaps.value = [...(glue1Options || []), ...(glue2Options || [])].filter((item) => {
    const key = [item.glueProcess, item.glueBoardModel, item.glueBoardMaterialCode]
      .map(normalizeText)
      .join('|');
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function preparePath() {
  selectedSourceId.value = undefined;
  selectedStandardId.value = undefined;
  selectedNodeKeys.value = [];
  expandedNodeKeys.value = [];
  itemRows.value = [];
  form.selectedItemIds = [];
  form.selectedScopes = [];
  form.checkQty = undefined;
  form.requiredSampleQty = undefined;
  form.receiptNo = undefined;
  form.supplierName = undefined;
  form.arrivalDate = undefined;
  if (form.taskType === 'RECHECK') {
    form.sourceMode = 'INSPECTION_RECORD';
    form.objectMode = 'SOURCE_RECORD';
    form.sampleSelectionMode = 'SOURCE_ITEMS';
  } else {
    form.sourceMode = 'MANUAL_BATCH';
    form.objectMode =
      form.checkType === 'FQC' || form.checkType === 'OQC'
        ? 'EXECUTION_PIECE'
        : 'BATCH';
    form.sampleSelectionMode = 'QUANTITY_ONLY';
  }
}

async function next() {
  if (step.value === 0) {
    if (!supportedPath.value) return void message.warning('该检验类型暂未开放任务向导');
    preparePath();
    step.value = 1;
    if (form.taskType === 'RECHECK') await loadSources();
    else await loadStandards();
    return;
  }
  if (step.value === 1) {
    if (form.taskType === 'RECHECK') {
      if (!selectedSourceId.value) return void message.warning('请选择一张已完成检验记录');
      form.sourceExecutionId = selectedSourceId.value;
      form.sourceExecutionNo = sourceSelected.value?.executionNo;
      applyRecheckSourceDefaults(sourceSelected.value);
      await loadSourceItems();
    } else {
      if (!selectedStandardId.value) return void message.warning('请选择一个已启用且已审核的检验标准');
      form.standardId = selectedStandardId.value;
      applyStandardDefaults(standardSelected.value);
      await loadStandardItems();
    }
    step.value = 2;
    return;
  }
  if (step.value === 2) {
    const payload = buildSelectionPayload();
    if (
      !deferFaiProjectSelectionToWorkbench.value &&
      !payload.selectedItemIds.length
    ) {
      return void message.warning('至少选择一个检验项目或位置');
    }
    if (productEventMode.value && !form.rejectReason?.trim()) {
      return void message.warning('请填写驳回说明');
    }
    if (productEventMode.value) {
      form.selectedItemIds = payload.selectedItemIds;
      form.selectedScopes = payload.selectedScopes;
      await submitProductEventRecheck();
      return;
    }
    if (!form.taskInstruction.trim()) return void message.warning('请填写检验注意事项');
    form.selectedItemIds = payload.selectedItemIds;
    form.selectedScopes = payload.selectedScopes;
    step.value = 3;
  }
}

function previous() {
  const minimumStep = productEventMode.value ? 2 : 0;
  if (step.value > minimumStep) step.value -= 1;
}

async function loadSources() {
  sourceLoading.value = true;
  try {
    const result = await getQualityTaskSourcePage({
      checkType: form.checkType,
      operationName: sourceQuery.operationName,
      batchNo: sourceQuery.batchNo,
      pageNo: sourcePage.current,
      pageSize: sourcePage.pageSize,
    });
    sourceRows.value = result.list || [];
    sourcePage.total = result.total || 0;
  } finally {
    sourceLoading.value = false;
  }
}

async function loadStandards() {
  standardLoading.value = true;
  try {
    const result = await getQualityTaskStandardPage({
      checkType: form.checkType,
      operationName: standardQuery.operationName,
      productModel: standardQuery.productModel,
      standardName: standardQuery.keyword,
      pageNo: standardPage.current,
      pageSize: standardPage.pageSize,
    });
    standardRows.value = result.list || [];
    standardPage.total = result.total || 0;
  } finally {
    standardLoading.value = false;
  }
}

async function loadSourceItems() {
  itemLoading.value = true;
  try {
    itemRows.value = await getQualityTaskSourceItemTree(
      form.checkType,
      selectedSourceId.value!,
    );
    selectedNodeKeys.value = deferFaiProjectSelectionToWorkbench.value
      ? []
      : collectSelectableKeys(itemRows.value);
    expandedNodeKeys.value = collectExpandableKeys(itemRows.value);
  } finally {
    itemLoading.value = false;
  }
}

async function loadStandardItems() {
  itemLoading.value = true;
  try {
    itemRows.value = await getQualityTaskStandardItems(selectedStandardId.value!);
    selectedNodeKeys.value = collectSelectableKeys(itemRows.value);
    expandedNodeKeys.value = collectExpandableKeys(itemRows.value);
  } finally {
    itemLoading.value = false;
  }
}

function handleSourceSelection(keys: Array<number | string>) {
  selectedSourceId.value = keys.length ? Number(keys[0]) : undefined;
  applyRecheckSourceDefaults(sourceSelected.value);
}

function sourceCustomRow(record: SourceRecord) {
  return {
    onClick: () => handleSourceSelection([record.id]),
    style: { cursor: 'pointer' },
  };
}

function handleStandardSelection(keys: Array<number | string>) {
  selectedStandardId.value = keys.length ? Number(keys[0]) : undefined;
}

function standardCustomRow(record: StandardRecord) {
  return {
    onClick: () => handleStandardSelection([record.id]),
    style: { cursor: 'pointer' },
  };
}

function handleTreeSelection(keys: string[]) {
  const previous = new Set(selectedNodeKeys.value);
  const normalized = new Set(keys);
  for (const item of itemRows.value) {
    const itemKey = nodeKey(item);
    const childKeys = collectSelectableKeys(item.children || []);
    const itemWasSelected = previous.has(itemKey);
    const itemIsSelected = normalized.has(itemKey);
    if (!itemWasSelected && itemIsSelected) {
      for (const childKey of childKeys) {
        normalized.add(childKey);
      }
    } else if (itemWasSelected && !itemIsSelected) {
      for (const childKey of childKeys) {
        normalized.delete(childKey);
      }
    } else if (
      itemWasSelected &&
      itemIsSelected &&
      childKeys.some((key) => previous.has(key) && !normalized.has(key))
    ) {
      normalized.delete(itemKey);
    }
  }
  selectedNodeKeys.value = [...normalized];
}

function toggleSelectAll(checked: boolean) {
  selectedNodeKeys.value = checked ? collectSelectableKeys(itemRows.value) : [];
}

function handleSelectAllChange(event: Event) {
  toggleSelectAll(Boolean((event.target as HTMLInputElement | null)?.checked));
}

function buildSelectionPayload() {
  const selected = new Set(selectedNodeKeys.value);
  const selectedItemIds: number[] = [];
  const selectedScopes: NonNullable<MesQualityTaskCenterApi.WizardCreateReq['selectedScopes']> = [];
  const pushScope = (
    itemId: number,
    scopeType: 'ITEM' | 'PIECE' | 'POSITION',
    positions?: string[],
  ) => {
    if (!itemId) return;
    selectedItemIds.push(itemId);
    const normalizedPositions = positions?.map(normalizeText).filter(Boolean);
    const existing = selectedScopes.find(
      (scope) => scope.itemId === itemId && scope.scopeType === scopeType,
    );
    if (existing && normalizedPositions?.length) {
      existing.positions = [...new Set([...(existing.positions || []), ...normalizedPositions])];
      return;
    }
    if (!existing) {
      selectedScopes.push(
        normalizedPositions?.length
          ? { itemId, positions: normalizedPositions, scopeType }
          : { itemId, scopeType },
      );
    }
  };
  for (const item of itemRows.value) {
    const itemId = Number(item.sourceItemId || item.id);
    if (selected.has(nodeKey(item))) {
      const sourceItemIds = item.sourceItemIds?.length ? item.sourceItemIds : [itemId];
      for (const sourceItemId of sourceItemIds) {
        pushScope(sourceItemId, 'ITEM');
      }
      continue;
    }
    const positions = (item.children || [])
      .filter((child) => child.nodeType === 'POSITION' && selected.has(nodeKey(child)))
      .map((child) => child.positionCode || child.positionName || '')
      .filter(Boolean);
    if (positions.length) {
      pushScope(itemId, 'POSITION', positions);
    }
    const pieces = (item.children || []).filter(
      (child) => child.nodeType === 'PIECE' && selected.has(nodeKey(child)),
    );
    for (const piece of pieces) {
      const pieceName =
        piece.pieceNo ||
        piece.positionCode ||
        piece.positionName ||
        piece.inspectionItem ||
        '未指定片号';
      const sourceItemIds = piece.sourceItemIds?.length
        ? piece.sourceItemIds
        : [Number(piece.sourceItemId || itemId)];
      for (const sourceItemId of sourceItemIds) {
        pushScope(sourceItemId, 'PIECE', [pieceName]);
      }
    }
  }
  return { selectedItemIds: [...new Set(selectedItemIds)], selectedScopes };
}

function collectSelectableKeys(nodes: CandidateItem[]): string[] {
  return nodes.flatMap((node) => [
    ...(node.selectable === false ? [] : [nodeKey(node)]),
    ...collectSelectableKeys(node.children || []),
  ]);
}

function collectExpandableKeys(nodes: CandidateItem[]): string[] {
  return nodes.flatMap((node) => [
    ...(node.children?.length ? [nodeKey(node)] : []),
    ...collectExpandableKeys(node.children || []),
  ]);
}

function sortCandidateTree(
  nodes: CandidateItem[],
  parent?: CandidateItem,
): CandidateItem[] {
  return [...nodes]
    .toSorted((left, right) => compareCandidateNodes(left, right, parent))
    .map((node) => ({
      ...node,
      children: node.children?.length
        ? sortCandidateTree(node.children, node)
        : node.children,
    }));
}

function compareCandidateNodes(
  left: CandidateItem,
  right: CandidateItem,
  parent?: CandidateItem,
) {
  if (
    parent?.nodeType === 'ITEM' &&
    left.nodeType === 'POSITION' &&
    right.nodeType === 'POSITION'
  ) {
    const leftIndex = candidatePositionIndex(parent, left);
    const rightIndex = candidatePositionIndex(parent, right);
    if (leftIndex !== rightIndex) return leftIndex - rightIndex;
    return candidatePositionText(left).localeCompare(
      candidatePositionText(right),
    );
  }
  if (left.nodeType === 'GROUP' && right.nodeType === 'GROUP') {
    const leftGroup = candidateGroupNo(left) ?? Number.MAX_SAFE_INTEGER;
    const rightGroup = candidateGroupNo(right) ?? Number.MAX_SAFE_INTEGER;
    if (leftGroup !== rightGroup) return leftGroup - rightGroup;
    return Number(left.id ?? 0) - Number(right.id ?? 0);
  }
  const typeDiff = candidateNodeTypeWeight(left) - candidateNodeTypeWeight(right);
  if (typeDiff !== 0) return typeDiff;
  const sortDiff = Number(left.sort ?? 0) - Number(right.sort ?? 0);
  if (sortDiff !== 0) return sortDiff;
  return Number(left.id ?? 0) - Number(right.id ?? 0);
}

function candidateNodeTypeWeight(record: CandidateItem) {
  if (record.nodeType === 'ITEM') return 1;
  if (record.nodeType === 'POSITION') return 2;
  if (record.nodeType === 'GROUP') return 3;
  if (record.nodeType === 'PIECE') return 4;
  return 9;
}

function candidatePositionIndex(item: CandidateItem, position: CandidateItem) {
  const positionText = normalizeText(
    position.positionCode || position.positionName,
  );
  const positions = configuredCandidatePositions(item);
  const index = positions.findIndex(
    (entry) =>
      normalizeText(entry.code).toLocaleLowerCase() ===
        positionText.toLocaleLowerCase() ||
      normalizeText(entry.name).toLocaleLowerCase() ===
        positionText.toLocaleLowerCase(),
  );
  return index >= 0 ? index : Number.MAX_SAFE_INTEGER;
}

function configuredCandidatePositions(item: CandidateItem) {
  const params = parseEntryRuleParams(item.templateParams);
  if (!Array.isArray(params.positions) || params.positions.length === 0) {
    return [];
  }
  return params.positions
    .map((position: any, index: number) => ({
      code: String(position?.code || position?.name || `P${index + 1}`),
      name: String(position?.name || position?.code || `${index + 1}`),
      sort: Number(position?.sort ?? index + 1),
    }))
    .toSorted((left, right) => left.sort - right.sort);
}

function candidatePositionText(record: CandidateItem) {
  return normalizeText(record.positionName || record.positionCode || '-');
}

function candidateGroupNo(record: CandidateItem) {
  const groupNo = Number(record.sampleGroupNo);
  return Number.isInteger(groupNo) && groupNo > 0 ? groupNo : undefined;
}

function candidateInspectionText(record: CandidateItem) {
  if (record.nodeType === 'POSITION') {
    return `位置：${candidatePositionText(record)}`;
  }
  if (record.nodeType === 'GROUP') {
    return `第${candidateGroupNo(record) || '-'}组`;
  }
  if (record.nodeType === 'PIECE') {
    return record.pieceNo || record.inspectionItem || '-';
  }
  return record.inspectionItem || '-';
}

function candidateInspectionClass(record: CandidateItem) {
  if (record.nodeType === 'ITEM') return 'font-bold text-slate-800';
  if (record.nodeType === 'POSITION') return 'font-bold text-indigo-700';
  if (record.nodeType === 'GROUP') return 'font-mono text-slate-700';
  return 'text-slate-700';
}

function candidatePositionCellText(record: CandidateItem) {
  return record.nodeType === 'ITEM' ? '-' : candidatePositionText(record);
}

function candidateGroupCellText(record: CandidateItem) {
  return record.nodeType === 'GROUP'
    ? `第${candidateGroupNo(record) || '-'}组`
    : '-';
}

function candidateAverageCellText(record: CandidateItem) {
  if (record.nodeType === 'GROUP') {
    return record.measuredValue ?? record.resultValue ?? '-';
  }
  return record.averageValue ?? record.measuredValue ?? '-';
}

function candidateMeasuredDataText(record: CandidateItem) {
  const raw = normalizeText(record.measuredData);
  if (!raw) return '-';
  return raw.replace(/\{[^{}]*\}/g, (segment) =>
    renderMeasuredDataSegment(segment),
  );
}

function renderMeasuredDataSegment(segment: string) {
  try {
    const parsed = JSON.parse(segment);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return segment;
    }
    return renderMeasuredDataObject(parsed as Record<string, unknown>) || segment;
  } catch {
    return segment;
  }
}

function renderMeasuredDataObject(raw: Record<string, unknown>) {
  const measured = firstText(
    raw['value'],
    raw['sampleValue'],
    raw['actualValue'],
    raw['measuredValue'],
    raw['resultValue'],
    raw['qualitativeValue'],
    raw['rawValue'],
  );
  const position = measuredPositionText(raw);
  if (position && measured) return `${position}：${measured}`;
  if (position) return position;
  if (measured) return measured;
  return Object.entries(raw)
    .filter(([key]) => !MEASURED_DATA_RENDERED_KEYS.has(key))
    .map(([key, value]) => {
      const text = normalizeText(value);
      return text ? `${key}=${text}` : '';
    })
    .filter(Boolean)
    .join('，');
}

function measuredPositionText(raw: Record<string, unknown>) {
  const position = firstText(raw['samplePositionName'], raw['samplePosition']);
  const code = normalizeText(raw['samplePositionCode']);
  if (position && code) return `位置${position}(${code})`;
  if (position) return `位置${position}`;
  return code ? `位置(${code})` : '';
}

function firstText(...values: unknown[]) {
  return values.map((value) => normalizeText(value)).find(Boolean) || '';
}

function candidateMeasuredDataPayload(record: CandidateItem) {
  const raw = normalizeText(record.measuredData);
  const cached = measuredDataPayloadCache.get(record);
  if (cached?.raw === raw) return cached.payload;
  const payload = parseCandidateMeasuredDataPayload(raw, record);
  measuredDataPayloadCache.set(record, { payload, raw });
  return payload;
}

function parseCandidateMeasuredDataPayload(
  raw: string,
  record: CandidateItem,
): CandidateMeasuredDataPayload | undefined {
  if (!raw.startsWith('{') || !raw.endsWith('}')) return undefined;
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return undefined;
    }
    const source = parsed as Record<string, unknown>;
    const values = Array.isArray(source['values'])
      ? source['values']
          .filter((sample) => sample !== null && sample !== undefined)
          .map((sample) =>
            typeof sample === 'object' && !Array.isArray(sample)
              ? (sample as CandidateMeasuredSample)
              : ({ value: sample } as CandidateMeasuredSample),
          )
      : [];
    if (source['renderType'] !== MEASURED_DATA_RENDER_TYPE && values.length === 0) {
      return undefined;
    }
    return {
      ...source,
      itemType: normalizeText(source['itemType'] || record.itemType),
      maxValueLimit: source['maxValueLimit'] ?? record.maxValueLimit,
      minValueLimit: source['minValueLimit'] ?? record.minValueLimit,
      values,
    };
  } catch {
    return undefined;
  }
}

function candidateMeasuredDataSamples(record: CandidateItem) {
  return candidateMeasuredDataPayload(record)?.values || [];
}

function candidateMeasuredSampleValue(sample: CandidateMeasuredSample) {
  return (
    firstText(
      sample['resultValue'],
      sample['value'],
      sample['measuredValue'],
      sample['qualitativeValue'],
    ) || '-'
  );
}

function candidateMeasuredSampleTitle(sample: CandidateMeasuredSample) {
  return (
    joinDistinct(
      normalizeText(sample['sampleSeq'])
        ? `第${normalizeText(sample['sampleSeq'])}组`
        : undefined,
      measuredSamplePositionText(sample),
      normalizeText(sample['sampleBarcode'])
        ? `条码 ${normalizeText(sample['sampleBarcode'])}`
        : undefined,
    ) || undefined
  );
}

function measuredSamplePositionText(sample: CandidateMeasuredSample) {
  const explicit = normalizeText(sample['positionText']);
  if (explicit) return explicit;
  const position = firstText(sample['samplePositionName'], sample['samplePosition']);
  const code = normalizeText(sample['samplePositionCode']);
  if (position && code) return `位置${position}(${code})`;
  if (position) return `位置${position}`;
  return code ? `位置(${code})` : '';
}

function candidateMeasuredSampleNg(
  record: CandidateItem,
  sample: CandidateMeasuredSample,
) {
  if (
    isMeasuredNgValue(sample['sampleResult']) ||
    isMeasuredNgValue(sample['qualitativeValue'])
  ) {
    return true;
  }
  const valueText = candidateMeasuredSampleValue(sample);
  if (isMeasuredNgValue(valueText)) return true;
  const payload = candidateMeasuredDataPayload(record);
  if (normalizeText(payload?.itemType || record.itemType).toUpperCase() !== 'QUANTITATIVE') {
    return false;
  }
  const value = Number(valueText);
  if (!Number.isFinite(value)) return false;
  const min = finiteNumber(payload?.minValueLimit ?? record.minValueLimit);
  const max = finiteNumber(payload?.maxValueLimit ?? record.maxValueLimit);
  return (min !== undefined && value < min) || (max !== undefined && value > max);
}

function isMeasuredNgValue(value: unknown) {
  const text = normalizeText(value).toUpperCase();
  return ['NG', 'NOK', 'FAIL', 'FAILED', '不合格'].includes(text);
}

function finiteNumber(value: unknown) {
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

function hasCandidateMeasuredSummary(record: CandidateItem) {
  const payload = candidateMeasuredDataPayload(record);
  return (
    normalizeText(payload?.itemType || record.itemType).toUpperCase() ===
      'QUANTITATIVE' &&
    Boolean(firstText(payload?.maxValue, payload?.minValue, payload?.averageValue))
  );
}

function candidateMeasuredSummaryValue(
  record: CandidateItem,
  key: 'averageValue' | 'maxValue' | 'minValue',
) {
  return normalizeText(candidateMeasuredDataPayload(record)?.[key]) || '-';
}

function applyStandardDefaults(standard?: StandardRecord) {
  if (!standard) return;
  if (form.checkType === 'GLUE_BOARD_FAI') {
    form.productModelCode = standard.glueBoardModel || standard.productModelCode;
    form.productModelName = form.productModelCode;
    form.materialCode = standard.materialCode;
    form.materialName = standard.materialName;
    if (form.productModelCode) handleGlueBoardModelChange(form.productModelCode);
    return;
  }
  form.productModelCode = standard.productModelCode || form.productModelCode;
  form.productModelName = standard.productModelName || form.productModelName;
  form.productModelId = standard.productModelId || form.productModelId;
  form.materialCode = standard.materialCode || form.materialCode;
  form.materialName = standard.materialName || form.materialName;
  form.materialId = standard.materialId || form.materialId;
  form.specification = standard.specification || form.specification;
  form.operationCode = standard.operationCode || form.operationCode;
  form.operationName = standard.operationName || form.operationName;
  if (form.checkType === 'IQC') form.unit = undefined;
}

function applyRecheckSourceDefaults(source?: SourceRecord) {
  if (!source) {
    form.checkQty = undefined;
    form.requiredSampleQty = undefined;
    return;
  }
  const continuousQuantity =
    form.checkType === 'GLUE_BOARD_FAI' || form.checkType === 'IQC';
  const quantity =
    continuousQuantity
      ? positiveNumber(source.checkQty)
      : positiveInteger(source.checkQty);
  form.checkQty = quantity;
  form.requiredSampleQty = continuousQuantity ? 1 : quantity;
  form.unit =
    form.checkType === 'GLUE_BOARD_FAI'
      ? 'm'
      : form.checkType === 'IQC'
        ? source.unit || '件'
        : source.unit || '片';
  form.receiptNo = source.receiptNo;
  form.supplierName = source.supplierName;
  form.arrivalDate = source.arrivalDate;
}

function handleProcessChange(name?: string) {
  const raw = workCenters.value.find((item) => getWorkCenterProcessName(item) === name);
  form.operationId = raw?.id;
  form.operationCode = getWorkCenterCode(raw);
  form.operationName = name;
  form.unit = inspectionUnit(name);
}

function handleModelChange(code?: string) {
  const raw = productModels.value.find(
    (item) => normalizeText(item.modelCode || item.modelName) === code,
  );
  form.productModelId = raw?.id;
  form.productModelCode = code;
  form.productModelName = raw?.modelName || code;
}

function handleMaterialChange(code?: string) {
  const raw = materials.value.find((item) => normalizeText(item.code) === code);
  form.materialId = raw?.id;
  form.materialCode = code;
  form.materialName = raw?.name;
  form.specification = form.specification || raw?.spec;
}

function handleGlueBoardModelChange(model?: string) {
  const normalizedModel = normalizeText(model);
  form.productModelCode = normalizedModel;
  form.productModelName = normalizedModel;
  const matches = glueBoardMaps.value.filter(
    (item) => normalizeText(item.glueBoardModel) === normalizedModel,
  );
  const currentMaterialValid = matches.some(
    (item) => normalizeText(item.glueBoardMaterialCode) === form.materialCode,
  );
  if (!currentMaterialValid) applyGlueBoardMaterial(matches[0]);
}

function handleGlueBoardMaterialChange(code?: string) {
  const normalizedCode = normalizeText(code);
  const match = glueBoardMaps.value.find(
    (item) =>
      normalizeText(item.glueBoardMaterialCode) === normalizedCode &&
      (!form.productModelCode ||
        normalizeText(item.glueBoardModel) === form.productModelCode),
  ) || glueBoardMaps.value.find(
    (item) => normalizeText(item.glueBoardMaterialCode) === normalizedCode,
  );
  applyGlueBoardMaterial(match);
  if (!form.productModelCode && match?.glueBoardModel) {
    form.productModelCode = match.glueBoardModel;
    form.productModelName = match.glueBoardModel;
  }
}

function applyGlueBoardMaterial(item?: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem) {
  form.materialId = item?.glueBoardMaterialId;
  form.materialCode = normalizeText(item?.glueBoardMaterialCode) || undefined;
  form.materialName = normalizeText(item?.glueBoardMaterialName) || undefined;
  form.specification = normalizeText(item?.glueBoardSpec) || form.specification;
}

function handleGlueOperationChange(code?: string) {
  const option = glueOperationOptions.find((item) => item.value === code);
  form.operationCode = code;
  form.operationName = option?.label;
  form.unit = 'm';
}

function openUserSelect() {
  userSelectModalApi
    .setData({ multiple: false, userIds: form.assigneeUserId ? [form.assigneeUserId] : [] })
    .open();
}

function handleUserConfirm(users: SystemUserApi.User[]) {
  const user = users[0] as any;
  if (!user?.id) return;
  form.assigneeUserId = user.id;
  form.assigneeUserName = user.nickname || user.username || String(user.id);
  form.assigneeDeptId = user.deptId;
  form.assigneeDeptName = user.deptName || user.dept?.name;
}

function validateDispatchStep() {
  if (!form.assigneeUserId) return '请选择实验室负责人';
  if (!form.requiredFinishTime) return '请选择要求完成时间';
  if (!form.taskInstruction.trim()) return '请填写检验注意事项';
  if (recheckQuantityEditable.value) {
    const quantity = positiveInteger(form.checkQty);
    if (!quantity) return '请填写大于零的复检片数';
    if (sourceInspectionQty.value && quantity > sourceInspectionQty.value) {
      return `复检片数不能超过原送检片数 ${sourceInspectionQty.value} 片`;
    }
  }
  if (form.taskType !== 'ADDITIONAL') return '';
  if (form.checkType === 'IQC') {
    if (!form.receiptNo?.trim()) return '请填写关联收料单';
    if (!form.batchNo?.trim()) return '请填写批次号';
    if (!form.materialCode?.trim() || !form.materialName?.trim()) {
      return '所选进料检验标准未绑定物料，请先维护标准';
    }
    if (!form.checkQty || Number(form.checkQty) <= 0) return '请填写大于零的到货数量';
    if (!form.arrivalDate) return '请选择来料日期';
    if (!form.supplierName?.trim()) return '请填写供应商';
    return '';
  }
  if (!form.batchNo?.trim()) {
    if (form.checkType === 'OQC') return '请填写发货通知单';
    if (form.checkType === 'GLUE_BOARD_FAI') return '请填写胶板批号';
    return form.checkType === 'FQC' ? '请填写母批次号' : '请填写产品批次';
  }
  if (form.checkType === 'OQC' && !form.workOrderNo?.trim()) return '请填写FQC单号';
  if (!form.productModelCode?.trim()) {
    return form.checkType === 'GLUE_BOARD_FAI' ? '请选择胶板型号' : '请选择产品型号';
  }
  if (form.checkType === 'GLUE_BOARD_FAI' && !form.materialCode?.trim()) {
    return '请选择胶板料号';
  }
  if (form.checkType === 'FAI' && !form.operationName?.trim()) return '请选择检测工序';
  if (form.checkType === 'GLUE_BOARD_FAI' && !form.operationCode?.trim()) {
    return '请选择检测工序';
  }
  if (
    (form.checkType === 'FQC' ||
      form.checkType === 'GLUE_BOARD_FAI' ||
      form.checkType === 'OQC') &&
    (!form.checkQty || Number(form.checkQty) <= 0)
  ) {
    return form.checkType === 'GLUE_BOARD_FAI'
      ? '请填写大于零的送检米数'
      : '请填写大于零的检验数量';
  }
  return '';
}

async function submitProductEventRecheck() {
  const payload = buildSelectionPayload();
  if (
    !deferFaiProjectSelectionToWorkbench.value &&
    !payload.selectedItemIds.length
  ) {
    return void message.warning('至少选择一个复检项目或片号');
  }
  if (!form.rejectReason?.trim()) return void message.warning('请填写驳回说明');
  if (!form.sourceExecutionId || !form.inspectionScene) {
    return void message.warning('缺少检验单来源信息，无法驳回复检');
  }
  form.selectedItemIds = deferFaiProjectSelectionToWorkbench.value
    ? []
    : payload.selectedItemIds;
  form.selectedScopes = deferFaiProjectSelectionToWorkbench.value
    ? []
    : payload.selectedScopes;
  const confirmed = await new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '确认驳回并生成复检单？',
      content: deferFaiProjectSelectionToWorkbench.value
        ? `将生成复检单 ${form.sourceExecutionNo || '-'}；具体检验项目将在复检单选择标准后由检验员确定。`
        : `将复制检验单 ${form.sourceExecutionNo || '-'}，仅清空所选复检范围；未选中的原检测值保持不变。`,
      okText: '确认生成',
      cancelText: '返回检查',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
    });
  });
  if (!confirmed) return;
  loading.value = true;
  try {
    const result = await rejectProductAbnormalEventRecheck({
      inspectionId: form.sourceExecutionId,
      rejectReason: form.rejectReason.trim(),
      selectedItemIds: deferFaiProjectSelectionToWorkbench.value
        ? []
        : payload.selectedItemIds,
      selectedScopes: deferFaiProjectSelectionToWorkbench.value
        ? []
        : payload.selectedScopes,
      sourceType: form.inspectionScene as MesProductAbnormalEventApi.SourceType,
    });
    message.success(`复检单已生成${result?.newInspectionNo ? `：${result.newInspectionNo}` : ''}`);
    open.value = false;
    emit('success', result?.newInspectionId || form.sourceExecutionId);
  } finally {
    loading.value = false;
  }
}

async function submit() {
  const validation = validateDispatchStep();
  if (validation) return void message.warning(validation);
  const payload = buildSelectionPayload();
  form.selectedItemIds = payload.selectedItemIds;
  form.selectedScopes = payload.selectedScopes;
  if (recheckQuantityEditable.value) {
    form.requiredSampleQty = Number(form.checkQty);
    form.unit = '片';
  }
  if (form.taskType === 'ADDITIONAL') {
    form.samplePieceNos = [];
    form.sampleSelectionMode = 'QUANTITY_ONLY';
    if (form.checkType === 'FAI') {
      form.objectMode = 'BATCH';
      form.checkQty = form.checkQty || 1;
      form.requiredSampleQty = 1;
      form.unit = inspectionUnit(form.operationName);
    } else if (form.checkType === 'GLUE_BOARD_FAI') {
      form.objectMode = 'BATCH';
      form.unit = 'm';
      form.requiredSampleQty = 1;
    } else if (form.checkType === 'IQC') {
      form.objectMode = 'BATCH';
      form.requiredSampleQty = 1;
      form.unit = form.unit || '件';
    } else {
      form.objectMode = 'EXECUTION_PIECE';
      form.unit = '片';
      form.requiredSampleQty = Number(form.checkQty);
    }
  }
  const sourceText =
    form.taskType === 'RECHECK'
      ? `复制检验单 ${form.sourceExecutionNo || '-'}，仅清空所选范围`
      : `按标准 ${standardSelected.value?.standardNo || '-'} 建立任务自有待检记录`;
  const confirmed = await new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '确认生成并提交质量任务？',
      content: `${sourceText}，并推送给 ${form.assigneeUserName} 执行。`,
      okText: '确认提交',
      cancelText: '返回检查',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
    });
  });
  if (!confirmed) return;
  loading.value = true;
  try {
    const taskId = await createQualityTaskByWizard({ ...form });
    message.success('质量检验任务已生成并推送执行人待办');
    open.value = false;
    emit('success', taskId);
  } finally {
    loading.value = false;
  }
}

function nodeKey(item: CandidateItem) {
  return item.nodeKey || `ITEM:${item.sourceItemId || item.id}`;
}

function filterTree(nodes: CandidateItem[], keyword: string): CandidateItem[] {
  const key = normalizeText(keyword).toLocaleLowerCase();
  if (!key) return nodes;
  return nodes.flatMap((node) => {
    const children = filterTree(node.children || [], keyword);
    const ownText = joinDistinct(
      node.inspectionItem,
      node.positionCode,
      node.positionName,
      node.pieceNo,
      node.currentResult,
      node.defectCode,
    ).toLocaleLowerCase();
    return ownText.includes(key) || children.length ? [{ ...node, children }] : [];
  });
}

function normalizeText(value?: unknown) {
  return String(value ?? '').trim();
}

function positiveInteger(value?: unknown) {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : undefined;
}

function positiveNumber(value?: unknown) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : undefined;
}

function toggleMaximized() {
  maximized.value = !maximized.value;
}

function joinDistinct(...values: Array<null | string | undefined>) {
  const seen = new Set<string>();
  return values
    .map(normalizeText)
    .filter((value) => {
      const key = value.toLocaleLowerCase();
      if (!value || seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .join(' / ');
}

function uniqueOptions(options: Array<{ label: string; value: string }>) {
  const unique = new Map<string, { label: string; value: string }>();
  for (const option of options) {
    if (option.value && !unique.has(option.value)) unique.set(option.value, option);
  }
  return [...unique.values()];
}

function controlLimitText(record: CandidateItem) {
  return joinDistinct(
    rangeText(record.avgMinLimit, record.avgMaxLimit, record.unit)
      ? `均值 ${rangeText(record.avgMinLimit, record.avgMaxLimit, record.unit)}`
      : '',
    rangeText(record.stdMinLimit, record.stdMaxLimit, record.unit)
      ? `标准差 ${rangeText(record.stdMinLimit, record.stdMaxLimit, record.unit)}`
      : '',
    rangeText(record.minValueLimit, record.maxValueLimit, record.unit)
      ? `单值 ${rangeText(record.minValueLimit, record.maxValueLimit, record.unit)}`
      : '',
  ) || '-';
}

function statisticsText(record: CandidateItem) {
  return joinDistinct(
    record.averageValue === undefined || record.averageValue === null
      ? ''
      : `均值 ${record.averageValue}`,
    record.standardDeviation === undefined || record.standardDeviation === null
      ? ''
      : `标准差 ${record.standardDeviation}`,
  ) || '-';
}

function getWorkCenterCode(raw?: any) {
  return normalizeText(raw?.processCode || raw?.code || raw?.wcCode);
}

function getWorkCenterProcessName(raw?: any) {
  return normalizeText(raw?.processName || raw?.processStage || raw?.name || raw?.wcName);
}

function inspectionUnit(operationName?: string) {
  const name = normalizeText(operationName);
  if (name.includes('配料')) return 'kg';
  return ['湿法', '磨皮', '粘胶1', '粘胶2', '胶板'].some((item) => name.includes(item))
    ? 'm'
    : '片';
}

function rangeText(min?: null | number, max?: null | number, unit?: string) {
  const hasMin = min !== undefined && min !== null;
  const hasMax = max !== undefined && max !== null;
  if (!hasMin && !hasMax) return '';
  if (hasMin && hasMax) return `${min} ~ ${max}${unit || ''}`;
  return hasMin ? `≥ ${min}${unit || ''}` : `≤ ${max}${unit || ''}`;
}

function dateText(value?: unknown) {
  const normalized = normalizeText(value);
  if (!normalized) return '';
  const dateValue = /^\d{11,}$/.test(normalized) ? Number(normalized) : normalized;
  return formatDate(dateValue, 'YYYY-MM-DD');
}

function sourceCellValue(record: SourceRecord, dataIndex?: string | number) {
  if (!dataIndex) return '-';
  const value = (record as Record<string, unknown>)[String(dataIndex)];
  return value ?? '-';
}

function judgmentText(value?: string) {
  const labels: Record<string, string> = {
    COMPLETED: '已完成',
    FAIL: '不合格',
    NG: '不合格',
    OK: '合格',
    PASS: '合格',
    PENDING: '待检',
    REJECTED: '已驳回',
  };
  return value ? labels[value.toUpperCase()] || value : '-';
}

function judgmentColor(value?: string) {
  const normalized = normalizeText(value).toUpperCase();
  if (['OK', 'PASS'].includes(normalized)) return 'success';
  if (['FAIL', 'NG', 'REJECTED'].includes(normalized)) return 'error';
  return 'default';
}
</script>

<template>
  <Modal
    v-model:open="open"
    :footer="null"
    :mask-closable="false"
    :style="{ top: maximized ? '0' : '12px', paddingBottom: 0 }"
    :width="modalWidth"
    destroy-on-close
    :wrap-class-name="modalWrapClassName"
  >
    <template #title>
      <div class="wizard-modal-title">
        <span>{{ modalTitle }}</span>
        <Tooltip :title="maximized ? '还原窗口' : '最大化窗口'">
          <Button class="maximize-button" size="small" type="text" @click.stop="toggleMaximized">
            <IconifyIcon :icon="maximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
          </Button>
        </Tooltip>
      </div>
    </template>

    <UserSelectModalComp
      class="w-3/5"
      title="选择实验室负责人"
      :z-index="6200"
      @confirm="handleUserConfirm"
    />

    <div class="erp-wizard" :class="{ 'is-maximized': maximized }">
      <div class="erp-step-bar">
        <Steps :current="stepCurrent" :items="stepItems" size="small" />
      </div>

      <div class="erp-content">
        <section v-if="step === 0" class="erp-section path-section">
          <div class="erp-section-title">一、选择检验方式与检验类型</div>
          <div class="erp-grid-form path-grid">
            <div class="erp-label required">检验方式</div>
            <div class="erp-value">
              <Radio.Group v-model:value="form.taskType" @change="preparePath">
                <Radio value="RECHECK">复检</Radio>
                <Radio value="ADDITIONAL">加检</Radio>
              </Radio.Group>
            </div>
            <div class="erp-label required">检验类型</div>
            <div class="erp-value">
              <Radio.Group v-model:value="form.checkType" @change="preparePath">
                <Radio value="FAI">过程首检</Radio>
                <Radio value="FQC">成品检验</Radio>
                <Radio value="OQC">出货检验</Radio>
                <Radio value="GLUE_BOARD_FAI">胶板检验</Radio>
                <Radio value="IQC">进料检验</Radio>
              </Radio.Group>
            </div>
          </div>
        </section>

        <section v-else-if="step === 1" class="erp-section table-section">
          <div class="erp-section-title">
            二、{{ form.taskType === 'RECHECK' ? '选择已完成检验记录' : '选择已启用且已审核的检验标准' }}
          </div>

          <template v-if="form.taskType === 'RECHECK'">
            <div class="erp-query-row">
              <template v-if="form.checkType === 'GLUE_BOARD_FAI'">
                <div class="erp-query-label">工序</div>
                <Select
                  v-model:value="sourceQuery.operationName"
                  allow-clear
                  :options="glueOperationNameOptions"
                  placeholder="粘胶1 / 粘胶2"
                />
                <div class="erp-query-label">关键词</div>
                <Input
                  v-model:value="sourceQuery.batchNo"
                  allow-clear
                  placeholder="检验单号 / 胶板型号 / 料号 / 批次"
                  @press-enter="sourcePage.current = 1; loadSources()"
                />
              </template>
              <template v-else-if="form.checkType === 'OQC'">
                <div class="erp-query-label">关键词</div>
                <Input
                  v-model:value="sourceQuery.batchNo"
                  allow-clear
                  placeholder="FQC单号 / 发货通知单 / 客户"
                  @press-enter="sourcePage.current = 1; loadSources()"
                />
              </template>
              <template v-else-if="form.checkType === 'IQC'">
                <div class="erp-query-label">关键词</div>
                <Input
                  v-model:value="sourceQuery.batchNo"
                  allow-clear
                  placeholder="IQC单号 / 收料单 / 供应商 / 物料 / 批次"
                  @press-enter="sourcePage.current = 1; loadSources()"
                />
              </template>
              <template v-else>
                <div class="erp-query-label">工序</div>
                <Select
                  v-model:value="sourceQuery.operationName"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="processOptions"
                  placeholder="选择或检索工序"
                />
                <div class="erp-query-label">产品批次</div>
                <Input
                  v-model:value="sourceQuery.batchNo"
                  allow-clear
                  placeholder="输入产品批次"
                  @press-enter="sourcePage.current = 1; loadSources()"
                />
              </template>
              <Button type="primary" @click="sourcePage.current = 1; loadSources()">查询</Button>
              <Button @click="Object.assign(sourceQuery, { operationName: undefined, batchNo: '' }); sourcePage.current = 1; loadSources()">重置</Button>
            </div>
            <div class="erp-table-wrap">
              <Table
                :columns="sourceColumns"
                :data-source="sourceRows"
                :loading="sourceLoading"
                :pagination="false"
                :row-key="(row) => row.id"
                :custom-row="sourceCustomRow"
                :row-selection="{
                  type: 'radio',
                  selectedRowKeys: selectedSourceId ? [selectedSourceId] : [],
                  onChange: handleSourceSelection,
                }"
                :scroll="{ x: form.checkType === 'GLUE_BOARD_FAI' ? 1390 : form.checkType === 'IQC' ? 1570 : form.checkType === 'OQC' ? 1410 : form.checkType === 'FQC' ? 1370 : 1580, y: 'calc(100vh - 360px)' }"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <Tag
                    v-if="column.dataIndex === 'judgment'"
                    :color="judgmentColor(record.judgment)"
                  >
                    {{ judgmentText(record.judgment) }}
                  </Tag>
                  <template v-else-if="['checkQty', 'okQty', 'ngQty'].includes(String(column.dataIndex))">
                    {{ sourceCellValue(record, column.dataIndex) }}
                  </template>
                  <template v-else-if="['inspectionTime', 'auditTime'].includes(String(column.dataIndex))">
                    {{ dateText(sourceCellValue(record, column.dataIndex)) }}
                  </template>
                </template>
              </Table>
            </div>
            <div class="erp-pagination">
              <Pagination
                v-model:current="sourcePage.current"
                v-model:page-size="sourcePage.pageSize"
                :show-total="(total) => `共 ${total} 条`"
                :total="sourcePage.total"
                show-size-changer
                @change="loadSources"
              />
            </div>
          </template>

          <template v-else>
            <div class="erp-query-row standard-query">
              <template v-if="form.checkType === 'FAI'">
                <div class="erp-query-label">工序</div>
                <Select
                  v-model:value="standardQuery.operationName"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="processOptions"
                  placeholder="工序"
                />
              </template>
              <div class="erp-query-label">
                {{ form.checkType === 'GLUE_BOARD_FAI' ? '胶板型号' : form.checkType === 'IQC' ? '物料/型号' : '产品型号' }}
              </div>
              <Select
                v-if="form.checkType === 'GLUE_BOARD_FAI'"
                v-model:value="standardQuery.productModel"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="glueBoardModelOptions"
                placeholder="选择或检索胶板型号"
              />
              <Input
                v-else
                v-model:value="standardQuery.productModel"
                allow-clear
                :placeholder="form.checkType === 'IQC' ? '物料编码 / 名称 / 型号' : '产品型号'"
              />
              <div class="erp-query-label">标准</div>
              <Input
                v-model:value="standardQuery.keyword"
                allow-clear
                :placeholder="form.checkType === 'GLUE_BOARD_FAI' ? '标准编号 / 名称 / 标准型号' : '标准编号或名称'"
                @press-enter="standardPage.current = 1; loadStandards()"
              />
              <Button type="primary" @click="standardPage.current = 1; loadStandards()">查询</Button>
            </div>
            <div class="erp-table-wrap">
              <Table
                :columns="standardColumns"
                :data-source="standardRows"
                :loading="standardLoading"
                :pagination="false"
                :row-key="(row) => row.id"
                :custom-row="standardCustomRow"
                :row-selection="{
                  type: 'radio',
                  selectedRowKeys: selectedStandardId ? [selectedStandardId] : [],
                  onChange: handleStandardSelection,
                }"
                :scroll="{ x: form.checkType === 'GLUE_BOARD_FAI' ? 850 : form.checkType === 'IQC' ? 1125 : 1000, y: 'calc(100vh - 360px)' }"
                size="small"
              />
            </div>
            <div class="erp-pagination">
              <Pagination
                v-model:current="standardPage.current"
                v-model:page-size="standardPage.pageSize"
                :show-total="(total) => `共 ${total} 条`"
                :total="standardPage.total"
                show-size-changer
                @change="loadStandards"
              />
            </div>
          </template>
        </section>

        <section v-else-if="step === 2" class="erp-section item-section">
          <div class="erp-section-title">三、选择检验项目与检验范围</div>
          <div v-if="productEventMode" class="erp-source-strip">
            <b>来源检验单：{{ productEventContext?.inspectionNo || '-' }}</b>
            <span v-if="deferFaiProjectSelectionToWorkbench">
              原检 NG 项仅作提示；具体复检项目将在生成复检单后、选择检验标准的后面确定。
            </span>
            <span v-else>提交后直接复制生成下一轮复检单，不进入质量任务流程。</span>
          </div>
          <div class="erp-item-toolbar">
            <Input
              v-model:value="itemKeyword"
              allow-clear
              :placeholder="['FAI', 'GLUE_BOARD_FAI'].includes(form.checkType) ? '检索检验项目或位置' : '检索检验项目'"
            />
            <span v-if="!deferFaiProjectSelectionToWorkbench" class="selection-summary">
              已选择 {{ selectionCount }} 个检验范围
            </span>
          </div>
          <div class="erp-tree-table">
            <Table
              v-model:expanded-row-keys="expandedNodeKeys"
              :columns="treeColumns"
              :data-source="filteredItemRows"
              :loading="itemLoading"
              :pagination="false"
              :row-key="nodeKey"
              :row-selection="
                deferFaiProjectSelectionToWorkbench
                  ? undefined
                  : treeRowSelection
              "
              :scroll="{ x: form.checkType === 'GLUE_BOARD_FAI' ? 1520 : form.checkType === 'IQC' && form.taskType === 'RECHECK' ? 1360 : form.checkType === 'FAI' ? 1220 : 980, y: 'calc(100vh - 470px)' }"
              children-column-name="children"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sort'">
                  {{ record.nodeType === 'ITEM' ? record.sort ?? '-' : '' }}
                </template>
                <template v-else-if="column.dataIndex === 'inspectionItem'">
                  <span :class="candidateInspectionClass(record)">
                    {{ candidateInspectionText(record) }}
                  </span>
                </template>
                <template v-else-if="column.dataIndex === 'avgControl'">
                  {{ rangeText(record.avgMinLimit, record.avgMaxLimit, record.unit) }}
                </template>
                <template v-else-if="column.dataIndex === 'stdControl'">
                  {{ rangeText(record.stdMinLimit, record.stdMaxLimit, record.unit) }}
                </template>
                <template v-else-if="column.dataIndex === 'valueLimit'">
                  {{ rangeText(record.minValueLimit, record.maxValueLimit, record.unit) }}
                </template>
                <template v-else-if="column.dataIndex === 'controlLimit'">
                  {{ controlLimitText(record) }}
                </template>
                <template v-else-if="column.dataIndex === 'statistics'">
                  {{ statisticsText(record) }}
                </template>
                <template v-else-if="column.dataIndex === 'positionName'">
                  {{ candidatePositionCellText(record) }}
                </template>
                <template v-else-if="column.dataIndex === 'averageValue'">
                  {{ candidateAverageCellText(record) }}
                </template>
                <template v-else-if="column.dataIndex === 'standardDeviation'">
                  {{ record.standardDeviation ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'sampleGroupNo'">
                  {{ candidateGroupCellText(record) }}
                </template>
                <template v-else-if="column.dataIndex === 'measuredValue'">
                  {{ record.measuredValue ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'resultValue'">
                  {{ record.resultValue ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'measuredData'">
                  <div
                    v-if="candidateMeasuredDataPayload(record)"
                    class="min-w-[150px]"
                  >
                    <div
                      v-if="candidateMeasuredDataSamples(record).length > 0"
                      class="mb-1 flex flex-wrap gap-1"
                    >
                      <Tag
                        v-for="(sample, index) in candidateMeasuredDataSamples(record)"
                        :key="`${record.id || record.nodeKey || 'sample'}-${index}`"
                        :color="
                          candidateMeasuredSampleNg(record, sample)
                            ? 'error'
                            : 'blue'
                        "
                        :title="candidateMeasuredSampleTitle(sample)"
                        class="!m-0 font-mono"
                      >
                        {{ candidateMeasuredSampleValue(sample) }}
                      </Tag>
                    </div>
                    <span v-else class="text-slate-400">-</span>

                    <div
                      v-if="hasCandidateMeasuredSummary(record)"
                      class="inline-block rounded border bg-slate-50 px-1 text-[10px] text-slate-500"
                    >
                      Max:
                      <span class="font-bold text-slate-700">{{
                        candidateMeasuredSummaryValue(record, 'maxValue')
                      }}</span>
                      | Min:
                      <span class="font-bold text-slate-700">{{
                        candidateMeasuredSummaryValue(record, 'minValue')
                      }}</span>
                      | Avg:
                      <span class="font-bold text-slate-700">{{
                        candidateMeasuredSummaryValue(record, 'averageValue')
                      }}</span>
                    </div>
                  </div>
                  <span v-else>{{ candidateMeasuredDataText(record) }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'pieceNo'">
                  <template v-if="['FQC', 'OQC'].includes(form.checkType) && record.nodeType === 'ITEM'">
                    共 {{ record.pieceCount || 0 }} 片
                  </template>
                  <template v-else>{{ record.pieceNo || '-' }}</template>
                </template>
                <template v-else-if="column.dataIndex === 'defectCode'">
                  {{ joinDistinct(record.defectCode, record.defectName) || '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'currentResult'">
                  <Space v-if="['FQC', 'OQC'].includes(form.checkType) && record.nodeType === 'ITEM'" :size="4">
                    <Tag color="success">OK {{ record.okPieceCount || 0 }}片</Tag>
                    <Tag color="error">NG {{ record.ngPieceCount || 0 }}片</Tag>
                  </Space>
                  <Tag v-else :color="judgmentColor(record.currentResult)">
                    {{ judgmentText(record.currentResult) }}
                  </Tag>
                </template>
              </template>
            </Table>
          </div>
          <div class="erp-item-footer">
            <Checkbox
              v-if="!deferFaiProjectSelectionToWorkbench"
              :checked="allItemSelected"
              @change="handleSelectAllChange"
            >
              全选检验项目
            </Checkbox>
            <div v-if="productEventMode" class="erp-note-field">
              <span class="required">驳回说明</span>
              <Input.TextArea
                v-model:value="form.rejectReason"
                :maxlength="500"
                :rows="2"
                placeholder="填写驳回原因和复检重点"
                show-count
              />
            </div>
            <div v-if="!productEventMode" class="erp-note-field">
              <span class="required">检验注意事项</span>
              <Input.TextArea
                v-model:value="form.taskInstruction"
                :maxlength="1000"
                :rows="2"
                placeholder="填写本次检验任务统一注意事项"
                show-count
              />
            </div>
          </div>
        </section>

        <section v-else class="erp-section dispatch-section">
          <div class="erp-section-title">四、任务下达与检验基础信息</div>

          <div class="erp-subtitle">任务分派</div>
          <div class="erp-grid-form dispatch-grid">
            <div class="erp-label required">实验室负责人</div>
            <div class="erp-value picker-field">
              <Input v-model:value="form.assigneeUserName" readonly placeholder="请选择负责人" />
              <Button type="link" @click="openUserSelect">选择</Button>
            </div>
            <div class="erp-label">优先级</div>
            <div class="erp-value">
              <Radio.Group v-model:value="form.priority">
                <Radio value="NORMAL">普通</Radio>
                <Radio value="URGENT">紧急</Radio>
              </Radio.Group>
            </div>
            <div class="erp-label required">要求完成时间</div>
            <div class="erp-value">
              <DatePicker
                v-model:value="form.requiredFinishTime"
                class="w-full"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
          </div>

          <div class="erp-subtitle">检验基础信息</div>
          <div
            v-if="form.taskType === 'RECHECK' && form.checkType === 'GLUE_BOARD_FAI'"
            class="erp-grid-form base-grid"
          >
            <div class="erp-label">检验单号</div><div class="erp-readonly">{{ sourceSelected?.executionNo || form.sourceExecutionNo || '-' }}</div>
            <div class="erp-label">胶板型号</div><div class="erp-readonly">{{ sourceSelected?.productModel || '-' }}</div>
            <div class="erp-label">胶板料号</div><div class="erp-readonly">{{ sourceSelected?.materialCode || '-' }}</div>
            <div class="erp-label">胶板批号</div><div class="erp-readonly">{{ sourceSelected?.batchNo || '-' }}</div>
            <div class="erp-label">送检米数</div><div class="erp-readonly">{{ sourceSelected?.checkQty ?? '-' }} m</div>
            <div class="erp-label">检测工序</div><div class="erp-readonly">{{ sourceSelected?.operationName || '-' }}</div>
            <div class="erp-label">检验标准</div><div class="erp-readonly span-three">{{ joinDistinct(sourceSelected?.standardNo, sourceSelected?.standardName) || '-' }}</div>
          </div>

          <div
            v-else-if="form.taskType === 'RECHECK' && form.checkType === 'IQC'"
            class="erp-grid-form base-grid"
          >
            <div class="erp-label">关联收料单</div><div class="erp-readonly">{{ sourceSelected?.receiptNo || '-' }}</div>
            <div class="erp-label">批次号</div><div class="erp-readonly">{{ sourceSelected?.batchNo || '-' }}</div>
            <div class="erp-label">物料编码</div><div class="erp-readonly">{{ sourceSelected?.materialCode || '-' }}</div>
            <div class="erp-label">物料名称</div><div class="erp-readonly">{{ sourceSelected?.materialName || '-' }}</div>
            <div class="erp-label">到货数量</div><div class="erp-readonly">{{ sourceSelected?.checkQty ?? '-' }} {{ sourceSelected?.unit || '' }}</div>
            <div class="erp-label">来料日期</div><div class="erp-readonly">{{ sourceSelected?.arrivalDate || '-' }}</div>
            <div class="erp-label">供应商</div><div class="erp-readonly">{{ sourceSelected?.supplierName || '-' }}</div>
            <div class="erp-label">检验标准</div><div class="erp-readonly">{{ joinDistinct(sourceSelected?.standardNo, sourceSelected?.standardName) || '-' }}</div>
          </div>

          <div
            v-else-if="form.taskType === 'RECHECK' && form.checkType === 'OQC'"
            class="erp-grid-form base-grid"
          >
            <div class="erp-label">FQC单号</div><div class="erp-readonly">{{ sourceSelected?.executionNo || form.sourceExecutionNo || '-' }}</div>
            <div class="erp-label">发货通知单</div><div class="erp-readonly">{{ sourceSelected?.shippingNoticeNo || sourceSelected?.batchNo || '-' }}</div>
            <div class="erp-label">客户</div><div class="erp-readonly">{{ sourceSelected?.customerName || '-' }}</div>
            <div class="erp-label">产品型号</div><div class="erp-readonly">{{ sourceSelected?.productModel || '-' }}</div>
            <div class="erp-label">物料编号</div><div class="erp-readonly">{{ sourceSelected?.materialCode || '-' }}</div>
            <div class="erp-label required">检验数量</div>
            <div class="erp-value recheck-qty-field">
              <InputNumber
                v-model:value="form.checkQty"
                :max="sourceInspectionQty"
                :min="1"
                :precision="0"
                addon-after="片"
              />
              <span v-if="sourceInspectionQty" class="field-hint">
                不超过原送检 {{ sourceInspectionQty }} 片
              </span>
            </div>
            <div class="erp-label">检验标准</div><div class="erp-readonly span-three">{{ joinDistinct(sourceSelected?.standardNo, sourceSelected?.standardName) || '-' }}</div>
          </div>

          <div v-else-if="form.taskType === 'RECHECK'" class="erp-grid-form base-grid">
            <div class="erp-label">检验单号</div><div class="erp-readonly">{{ sourceSelected?.executionNo || form.sourceExecutionNo || '-' }}</div>
            <div class="erp-label">工单号</div><div class="erp-readonly">{{ sourceSelected?.workOrderNo || sourceSelected?.planNo || '-' }}</div>
            <div class="erp-label">产品型号</div><div class="erp-readonly">{{ sourceSelected?.productModel || '-' }}</div>
            <div class="erp-label">检测工序</div><div class="erp-readonly">{{ sourceSelected?.operationName || '-' }}</div>
            <div class="erp-label">{{ form.checkType === 'FQC' ? '母批次号' : '产品批次' }}</div><div class="erp-readonly">{{ sourceSelected?.batchNo || '-' }}</div>
            <div
              class="erp-label"
              :class="{ required: recheckQuantityEditable }"
            >
              检验片数
            </div>
            <div
              v-if="recheckQuantityEditable"
              class="erp-value recheck-qty-field"
            >
              <InputNumber
                v-model:value="form.checkQty"
                :max="sourceInspectionQty"
                :min="1"
                :precision="0"
                addon-after="片"
              />
              <span v-if="sourceInspectionQty" class="field-hint">
                不超过原送检 {{ sourceInspectionQty }} 片
              </span>
            </div>
            <div v-else class="erp-readonly">
              {{ sourceSelected?.checkQty ?? '-' }} {{ sourceSelected?.unit || '片' }}
            </div>
            <div class="erp-label">规格</div><div class="erp-readonly">{{ sourceSelected?.specification || '-' }}</div>
            <div class="erp-label">物料编号</div><div class="erp-readonly">{{ sourceSelected?.materialCode || '-' }}</div>
            <div class="erp-label">检验标准</div><div class="erp-readonly span-three">{{ joinDistinct(sourceSelected?.standardNo, sourceSelected?.standardName) || '-' }}</div>
          </div>

          <div v-else-if="form.checkType === 'GLUE_BOARD_FAI'" class="erp-grid-form base-grid">
            <div class="erp-label">检验单号</div><div class="erp-readonly">提交后自动生成</div>
            <div class="erp-label required">胶板型号</div>
            <div class="erp-value">
              <Select
                v-model:value="form.productModelCode"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="glueBoardModelOptions"
                placeholder="从成品胶板对照表选择"
                @change="handleGlueBoardModelChange"
              />
            </div>
            <div class="erp-label required">胶板料号</div>
            <div class="erp-value">
              <Select
                v-model:value="form.materialCode"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="filteredGlueBoardMaterialOptions"
                placeholder="从成品胶板对照表选择"
                @change="handleGlueBoardMaterialChange"
              />
            </div>
            <div class="erp-label required">胶板批号</div><div class="erp-value"><Input v-model:value="form.batchNo" placeholder="输入胶板批号" /></div>
            <div class="erp-label required">送检米数</div><div class="erp-value"><InputNumber v-model:value="form.checkQty" :min="0.001" :precision="3" class="w-full" addon-after="m" /></div>
            <div class="erp-label required">检测工序</div>
            <div class="erp-value">
              <Select
                v-model:value="form.operationCode"
                allow-clear
                :options="glueOperationOptions"
                placeholder="选择粘胶1或粘胶2"
                @change="handleGlueOperationChange"
              />
            </div>
            <div class="erp-label">检验标准</div><div class="erp-readonly">{{ joinDistinct(standardSelected?.standardNo, standardSelected?.standardName) || '-' }}</div>
          </div>

          <div v-else-if="form.checkType === 'FAI'" class="erp-grid-form base-grid">
            <div class="erp-label required">产品批次</div><div class="erp-value"><Input v-model:value="form.batchNo" /></div>
            <div class="erp-label required">产品型号</div><div class="erp-value"><Select v-model:value="form.productModelCode" allow-clear show-search option-filter-prop="label" :options="modelOptions" @change="handleModelChange" /></div>
            <div class="erp-label required">检测工序</div><div class="erp-value"><Select v-model:value="form.operationName" allow-clear show-search option-filter-prop="label" :options="processOptions" @change="handleProcessChange" /></div>
            <div class="erp-label">工单号</div><div class="erp-value"><Input v-model:value="form.workOrderNo" placeholder="可人工填写" /></div>
            <div class="erp-label">检验标准</div><div class="erp-readonly span-three">{{ joinDistinct(standardSelected?.standardNo, standardSelected?.standardName) }}</div>
          </div>

          <div v-else-if="form.checkType === 'IQC'" class="erp-grid-form base-grid">
            <div class="erp-label required">关联收料单</div><div class="erp-value"><Input v-model:value="form.receiptNo" /></div>
            <div class="erp-label required">批次号</div><div class="erp-value"><Input v-model:value="form.batchNo" /></div>
            <div class="erp-label">物料编码</div><div class="erp-readonly">{{ form.materialCode || '-' }}</div>
            <div class="erp-label">物料名称</div><div class="erp-readonly">{{ form.materialName || '-' }}</div>
            <div class="erp-label required">到货数量</div><div class="erp-value"><InputNumber v-model:value="form.checkQty" :min="0.001" :precision="3" class="w-full" /></div>
            <div class="erp-label required">来料日期</div><div class="erp-value"><DatePicker v-model:value="form.arrivalDate" class="w-full" value-format="YYYY-MM-DD" /></div>
            <div class="erp-label required">供应商</div><div class="erp-value"><Input v-model:value="form.supplierName" /></div>
            <div class="erp-label">检验标准</div><div class="erp-readonly">{{ joinDistinct(standardSelected?.standardNo, standardSelected?.standardName) || '-' }}</div>
          </div>

          <div v-else-if="form.checkType === 'OQC'" class="erp-grid-form base-grid">
            <div class="erp-label required">发货通知单</div><div class="erp-value"><Input v-model:value="form.batchNo" /></div>
            <div class="erp-label required">FQC单号</div><div class="erp-value"><Input v-model:value="form.workOrderNo" /></div>
            <div class="erp-label required">产品型号</div><div class="erp-value"><Select v-model:value="form.productModelCode" allow-clear show-search option-filter-prop="label" :options="modelOptions" @change="handleModelChange" /></div>
            <div class="erp-label required">检验数量</div><div class="erp-value"><InputNumber v-model:value="form.checkQty" :min="1" :precision="0" class="w-full" addon-after="片" /></div>
            <div class="erp-label">物料编号</div><div class="erp-readonly">{{ standardSelected?.materialCode || '-' }}</div>
            <div class="erp-label">检验标准</div><div class="erp-readonly">{{ joinDistinct(standardSelected?.standardNo, standardSelected?.standardName) || '-' }}</div>
          </div>

          <div v-else class="erp-grid-form base-grid">
            <div class="erp-label required">母批次号</div><div class="erp-value"><Input v-model:value="form.batchNo" /></div>
            <div class="erp-label required">产品型号</div><div class="erp-value"><Select v-model:value="form.productModelCode" allow-clear show-search option-filter-prop="label" :options="modelOptions" @change="handleModelChange" /></div>
            <div class="erp-label required">检验数量</div><div class="erp-value"><InputNumber v-model:value="form.checkQty" :min="1" :precision="0" class="w-full" addon-after="片" /></div>
            <div class="erp-label">规格</div><div class="erp-value"><Select v-model:value="form.specification" allow-clear :options="[{ label: '775mm', value: '775mm' }, { label: '740mm', value: '740mm' }]" /></div>
            <div class="erp-label">物料编号</div><div class="erp-value"><Select v-model:value="form.materialCode" allow-clear show-search option-filter-prop="label" :options="materialOptions" @change="handleMaterialChange" /></div>
            <div class="erp-label">工单号</div><div class="erp-value"><Input v-model:value="form.workOrderNo" placeholder="可人工填写" /></div>
            <div class="erp-label">检验标准</div><div class="erp-readonly span-three">{{ joinDistinct(standardSelected?.standardNo, standardSelected?.standardName) }}</div>
          </div>

          <div class="erp-subtitle">任务要求</div>
          <div class="erp-grid-form instruction-grid">
            <div class="erp-label required">检验注意事项</div>
            <div class="erp-value instruction-value">
              <Input.TextArea
                v-model:value="form.taskInstruction"
                :maxlength="1000"
                :rows="4"
                class="instruction-textarea"
                show-count
              />
            </div>
          </div>
        </section>
      </div>

      <div class="erp-footer">
        <span class="footer-summary">
          {{ form.taskType === 'RECHECK' ? '复检' : '加检' }} / {{ form.checkType === 'FAI' ? '过程首检' : form.checkType === 'FQC' ? '成品检验' : form.checkType === 'OQC' ? '出货检验' : form.checkType === 'GLUE_BOARD_FAI' ? '胶板检验' : form.checkType === 'IQC' ? '进料检验' : form.checkType }}
        </span>
        <Space>
          <Button @click="open = false">关闭</Button>
          <Button v-if="step > (productEventMode ? 2 : 0)" @click="previous">上一步</Button>
          <Button
            v-if="productEventMode && step === 2"
            type="primary"
            :loading="loading"
            @click="submitProductEventRecheck"
          >
            确认驳回生成复检单
          </Button>
          <Button v-else-if="step < 3" type="primary" @click="next">下一步</Button>
          <Button v-else type="primary" :loading="loading" @click="submit">确认生成并提交</Button>
        </Space>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.erp-wizard {
  display: flex;
  height: calc(100vh - 96px);
  min-height: 0;
  max-height: 860px;
  flex-direction: column;
  color: #172b4d;
}

.erp-wizard.is-maximized {
  height: 100%;
  max-height: none;
}

.wizard-modal-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 34px;
}

.maximize-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.erp-step-bar {
  flex: 0 0 auto;
  padding: 10px 9%;
  border-bottom: 1px solid #c9d6e5;
  background: #f4f8fc;
}

.erp-content {
  min-height: 0;
  flex: 1;
  padding: 10px 12px;
  overflow: hidden;
  background: #fff;
}

.erp-section {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #c9d6e5;
}

.erp-section-title,
.erp-subtitle {
  flex: 0 0 auto;
  color: #0b6edb;
  font-weight: 700;
}

.erp-section-title {
  padding: 8px 14px;
  border-bottom: 1px solid #c9d6e5;
  font-size: 15px;
  background: #f7fbff;
}

.erp-subtitle {
  padding: 7px 12px;
  border-bottom: 1px solid #c9d6e5;
  background: #f7fbff;
}

.path-section {
  height: auto;
}

.path-grid {
  grid-template-columns: 140px 1fr;
}

.erp-grid-form {
  display: grid;
  border-top: 0;
  border-left: 0;
}

.erp-label,
.erp-value,
.erp-readonly {
  min-height: 43px;
  padding: 7px 10px;
  border-right: 1px solid #d8e1ec;
  border-bottom: 1px solid #d8e1ec;
}

.erp-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  background: #edf3f9;
  font-weight: 600;
}

.erp-value,
.erp-readonly {
  display: flex;
  min-width: 0;
  align-items: center;
  background: #fff;
}

.erp-readonly {
  word-break: break-all;
}

.recheck-qty-field {
  gap: 10px;
}

.recheck-qty-field :deep(.ant-input-number) {
  width: 80%;
}

.field-hint {
  flex: 0 0 auto;
  color: #6b778c;
  white-space: nowrap;
}

.required::before {
  margin-right: 4px;
  color: #ff4d4f;
  content: '*';
}

.erp-query-row {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: 90px 190px 100px minmax(220px, 1fr) auto auto;
  gap: 0;
  padding: 10px 12px;
  border-bottom: 1px solid #c9d6e5;
  background: #f8fafc;
}

.erp-query-row > * {
  border-radius: 0;
}

.standard-query {
  grid-template-columns: repeat(3, 90px minmax(160px, 1fr)) auto;
}

.erp-query-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 10px;
  border: 1px solid #d8e1ec;
  background: #edf3f9;
  font-weight: 600;
}

.erp-table-wrap,
.erp-tree-table {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.erp-table-wrap :deep(.ant-table-wrapper),
.erp-tree-table :deep(.ant-table-wrapper),
.erp-table-wrap :deep(.ant-spin-nested-loading),
.erp-tree-table :deep(.ant-spin-nested-loading),
.erp-table-wrap :deep(.ant-spin-container),
.erp-tree-table :deep(.ant-spin-container),
.erp-table-wrap :deep(.ant-table),
.erp-tree-table :deep(.ant-table),
.erp-table-wrap :deep(.ant-table-container),
.erp-tree-table :deep(.ant-table-container) {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.erp-table-wrap :deep(.ant-table-body),
.erp-tree-table :deep(.ant-table-body) {
  height: 100%;
  min-height: 0;
  max-height: none !important;
  flex: 1;
  overflow: auto !important;
}

.erp-pagination {
  display: flex;
  flex: 0 0 48px;
  align-items: center;
  justify-content: flex-end;
  padding: 0 12px;
  border-top: 1px solid #c9d6e5;
}

.erp-source-strip,
.erp-item-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding: 7px 12px;
  border-bottom: 1px solid #c9d6e5;
  background: #f8fbfe;
}

.erp-item-toolbar :deep(.ant-input-affix-wrapper) {
  width: 380px;
}

.selection-summary,
.footer-summary {
  color: #5b6b7f;
}

.erp-item-footer {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: 150px 1fr;
  align-items: start;
  gap: 8px 14px;
  padding: 8px 12px;
  border-top: 1px solid #c9d6e5;
  background: #f8fafc;
}

.erp-note-field {
  display: grid;
  grid-column: 2;
  grid-template-columns: 120px 1fr;
  align-items: start;
  gap: 8px;
}

.erp-note-field > span {
  padding-top: 6px;
  text-align: right;
  font-weight: 600;
}

.dispatch-section {
  overflow: auto;
}

.dispatch-grid {
  grid-template-columns: 130px 1fr 110px 1fr 130px 1fr;
}

.base-grid {
  grid-template-columns: 130px 1fr 130px 1fr;
}

.instruction-grid {
  display: grid;
  width: 100%;
  grid-template-columns: 130px minmax(0, 1fr);
}

.instruction-value {
  min-width: 0;
  grid-column: 2 / -1;
}

.instruction-textarea,
.instruction-value :deep(textarea.ant-input) {
  width: 100% !important;
  min-width: 0;
}

.span-three {
  grid-column: span 3;
}

.picker-field {
  padding: 0;
}

.picker-field :deep(.ant-input) {
  border: 0;
  border-radius: 0;
}

.erp-footer {
  display: flex;
  flex: 0 0 52px;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-top: 1px solid #c9d6e5;
  background: #f4f8fc;
}

:deep(.ant-radio-wrapper) {
  margin-inline-end: 24px;
  background: transparent;
}

.erp-wizard :deep(.ant-select) {
  width: 80%;
}

:global(.erp-quality-task-wizard.is-maximized .ant-modal) {
  top: 0 !important;
  width: 100vw !important;
  max-width: 100vw;
  margin: 0;
  padding-bottom: 0;
}

:global(.erp-quality-task-wizard.is-maximized .ant-modal-content) {
  display: flex;
  height: 100vh;
  flex-direction: column;
  overflow: hidden;
  border-radius: 0;
}

:global(.erp-quality-task-wizard.is-maximized .ant-modal-header) {
  flex: 0 0 auto;
}

:global(.erp-quality-task-wizard.is-maximized .ant-modal-body) {
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

:deep(.ant-table-wrapper),
:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  height: 100%;
}

:deep(.ant-table) {
  border-radius: 0;
}

:deep(.ant-table-thead > tr > th) {
  background: #edf3f9;
  font-weight: 700;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-picker),
:deep(.ant-input-number),
:deep(.ant-select-selector) {
  border-radius: 2px !important;
}

@media (max-width: 1100px) {
  .dispatch-grid,
  .base-grid {
    grid-template-columns: 120px 1fr 120px 1fr;
  }

  .standard-query {
    grid-template-columns: 90px 1fr 90px 1fr;
  }
}
</style>
