<script lang="ts" setup>
import type { MesHcBomApi } from '#/api/mes/hc/bom';
import type { MesHcOwnerApi } from '#/api/mes/hc/owner';
import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';
import type { MesHcRouteApi } from '#/api/mes/hc/route';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';
import type { MesHcInvStockApi } from '#/api/mes/hc/inv-stock';
import type { MesSaleOrderApi } from '#/api/mes/sale-order';
import type {
  ProductionInstructionContext,
  ProductionInstructionSegmentOption,
} from '#/views/mes/hc/shared/production-instruction';

import dayjs from 'dayjs';
import { computed, defineComponent, h, onBeforeUnmount, reactive, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  Button,
  Checkbox,
  DatePicker,
  Input,
  InputNumber,
  Modal as AntModal,
  Pagination,
  Select,
  Table,
  Tooltip,
  message,
} from 'ant-design-vue';

import {
  getBom,
  getBomPage,
  getBomProductModelOptions,
  getBomSelectOptionsByMaterialId,
} from '#/api/mes/hc/bom';
import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';
import { getInvStockPage } from '#/api/mes/hc/inv-stock';
import { getLocationSelectOptions } from '#/api/mes/hc/location';
import {
  PickerInline,
  PickerModal,
  saleOrderPickerConfig,
} from '#/components/picker';
import type { PickerEntityConfig, PickerOption } from '#/components/picker';
import { getMaterialDetail } from '#/api/mes/hc/material';
import { getOwnerSelectOptions } from '#/api/mes/hc/owner';
import {
  createPlanOrder,
  getPlanOrderDetail,
  getPlanOrderStaticOptions,
  getPlanWipCandidatePage,
  getPlanStatusLogList,
  previewPlanRootBatchNo,
  releasePlanInventoryLock,
  updatePlanOperationStatus,
  updatePlanOrder,
  updatePlanStatus,
  withdrawPlanOrder,
} from '#/api/mes/hc/planorder';
import { getProductModelDetail } from '#/api/mes/hc/productmodel';
import { getRecipeSelectOptions } from '#/api/mes/hc/recipe';
import { createRoute, getRouteDetail, getRoutePage } from '#/api/mes/hc/route';
import { getWorkCenterSelectOptions } from '#/api/mes/hc/workcenter';
import { getSimpleUserList } from '#/api/system/user';
import { ProductionInstructionIssueModal } from '#/views/mes/hc/shared/production-instruction';

import {
  formatNumber,
  getPlanStatusMeta,
  OPERATION_STATUS_OPTIONS,
  PLAN_STATUS_OPTIONS,
} from '../data';
import { PLAN_FIELD_COPY, PLAN_PLACEHOLDER_COPY } from '../../shared/field-copy';

interface EditableOperation extends MesHcPlanOrderApi.Operation {
  localKey: string;
}

interface EditableInventoryLock extends MesHcPlanOrderApi.InventoryLock {
  localKey: string;
  operationKey?: string;
}

type EditablePlanOrder = MesHcPlanOrderApi.PlanOrder;

type SelectOption = {
  value: number;
  label: string;
  code?: string;
  name?: string;
  workCenterId?: number;
  workCenterCode?: string;
  workCenterName?: string;
  applicablePadType?: string;
  applicablePadTypeName?: string;
  status?: number;
  workStatus?: string;
};

type CodeNameOption = {
  value: string;
  label: string;
  id?: number;
  code: string;
  name: string;
};

type RecipeCodeOption = MesHcRecipeApi.SelectOption & {
  name?: string;
};

type OperationStatusAction = MesHcPlanOrderApi.OperationStatusReq['actionType'];
type PlanStatusValue = MesHcPlanOrderApi.PlanStatusReq['planStatus'];

function defaultProductionStartDate() {
  return dayjs().format('YYYY-MM-DD');
}

function defaultProductionEndDate() {
  return dayjs().endOf('week').format('YYYY-MM-DD');
}

const emit = defineEmits(['success']);
const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const saving = ref(false);
const activeLockTab = ref<'FG' | 'WIP'>('WIP');
const seed = ref(0);
const productBomPickerOpen = ref(false);
const saleOrderPickerOpen = ref(false);
const inventoryModalOpen = ref(false);
const inventoryModalType = ref<'FG' | 'WIP'>('WIP');
const inventoryLoading = ref(false);
const statusLogOpen = ref(false);
const statusLogLoading = ref(false);
const releaseModalOpen = ref(false);
const releaseSubmitting = ref(false);
const releaseTargetLock = ref<EditableInventoryLock>();
const productionInstructionOpen = ref(false);
const productionInstructionContext = ref<ProductionInstructionContext>();
const productionInstructionTitle = ref('下达生产指令');
const productionInstructionType = ref<'DAILY' | 'PAUSE' | 'RESUME'>('DAILY');
const productionInstructionScope = ref<'OPERATION' | 'PLAN'>('OPERATION');
const productionInstructionOperationOptions = ref<any[]>([]);
const productionInstructionSegmentOptions = ref<ProductionInstructionSegmentOption[]>([]);
const productionInstructionShowOperation = ref(true);
const productionInstructionShowSegment = ref(false);
const productionInstructionTypeOptions = [
  { label: '日常指令', value: 'DAILY' },
  { label: '暂停', value: 'PAUSE' },
  { label: '复工', value: 'RESUME' },
];
const openToken = ref(0);
const lockPanelCollapsed = ref(false);
const selectedInventoryLockKey = ref('');
const splitContainerRef = ref<HTMLElement>();
const splitLeftPercent = ref(58);
const splitDragging = ref(false);
const routeMaintenanceMode = ref(false);
const batchPreviewLoading = ref(false);
const batchPreviewWarning = ref('');
const manualBatchTouched = ref(false);
let batchPreviewTimer: ReturnType<typeof setTimeout> | undefined;
let splitResizeCleanup: (() => void) | undefined;

const routeOptions = ref<MesHcRouteApi.SelectOption[]>([]);
const recipeOptions = ref<RecipeCodeOption[]>([]);
const bomOptions = ref<MesHcBomApi.SelectOption[]>([]);
const workCenterOptions = ref<MesHcWorkCenterApi.SelectOption[]>([]);
const equipmentOptions = ref<SelectOption[]>([]);
const locationOptions = ref<SelectOption[]>([]);
const ownerOptions = ref<MesHcOwnerApi.SelectOption[]>([]);
const productModelOptions = ref<MesHcBomApi.ProductModelOption[]>([]);
const productModelValue = ref<number | undefined>();
const prodTypeOptions = ref<CodeNameOption[]>([]);
const materialCategoryOptions = ref<CodeNameOption[]>([]);
const sizeSpecOptions = ref<CodeNameOption[]>([]);
const operationRows = ref<EditableOperation[]>([]);
const lockRows = ref<EditableInventoryLock[]>([]);
const selectedOperationKey = ref('');
const checkedOperationKeys = ref<string[]>([]);
const userNameMap = ref<Record<string, string>>({});
const inventoryRows = ref<MesHcInvStockApi.Stock[]>([]);
const statusLogRows = ref<MesHcPlanOrderApi.PlanStatusLog[]>([]);
const statusLogColumns = [
  { title: '操作时间', dataIndex: 'operateTime', key: 'operateTime', width: 170 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: '动作', dataIndex: 'actionType', key: 'actionType', width: 100 },
  { title: '原状态', dataIndex: 'fromStatus', key: 'fromStatus', width: 90 },
  { title: '目标状态', dataIndex: 'toStatus', key: 'toStatus', width: 90 },
  { title: '备注说明', dataIndex: 'reasonRemark', key: 'reasonRemark' },
];
const inventoryPickQty = reactive<Record<string, number>>({});
const inventoryRowCache = reactive<Record<string, MesHcInvStockApi.Stock>>({});
const inventoryPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
});
const releaseForm = reactive({
  releaseQty: 0,
  releaseReason: '',
});
const inventoryQuery = reactive({
  recipeCode: '',
  specSize: '',
  opSeq: undefined as number | undefined,
  segmentCode: '',
  sourcePlanNo: '',
  sourceBatchNo: '',
  sourceParentBatchNo: '',
  modelNo: '',
  keyword: '',
});

const formState = reactive<EditablePlanOrder>({
  // 原型未直接体现的控制字段在界面上隐藏，只保留给后端持久化和状态流转使用。
  id: undefined,
  planNo: '',
  planDate: dayjs().format('YYYY-MM-DD'),
  planMode: 'MTO',
  planStatus: 'DRAFT',
  sourceType: 'MANUAL',
  salesOrderNo: '',
  salesOrderErpNo: '',
  salesOrderLineNo: '',
  customerName: '',
  orderDueQty: 0,
  orderDueUnitId: undefined,
  orderDueUnitCode: '',
  orderDueUnitName: '',
  salesOrderDeliveryDate: '',
  productionStartDate: defaultProductionStartDate(),
  productionEndDate: defaultProductionEndDate(),
  salesOrderSnapshotJson: '',
  materialId: undefined,
  materialCode: '',
  materialName: '',
  motherMaterialId: undefined,
  motherMaterialCode: '',
  motherMaterialName: '',
  categoryCode: '',
  categoryName: '',
  prodType: '',
  prodTypeName: '',
  modelId: undefined,
  modelName: '',
  modelCode: '',
  motherModelId: undefined,
  motherModelName: '',
  motherModelCode: '',
  recipeId: undefined,
  recipeCode: '',
  recipeName: '',
  bomId: undefined,
  bomVersion: '',
  routeId: undefined,
  routeCode: '',
  routeName: '',
  routeVersion: '',
  sizeSpec: '',
  sizeName: '',
  targetQty: 0,
  targetUnitId: undefined,
  targetUnitCode: '',
  targetUnitName: '',
  targetUom: 'PCS',
  fgDeductQty: 0,
  netPlanQty: 0,
  operationCount: 0,
  totalLockQty: 0,
  batchRuleId: undefined,
  batchRuleCode: '',
  batchNo: '',
  productionBatchNo: '',
  productionBatchContextJson: '',
  batchStatus: 'NOT_GEN',
  remark: '',
});

const productBomPickerConfig: PickerEntityConfig<MesHcBomApi.Bom> = {
  entityKey: 'productBom',
  title: '选择产品料号',
  tableTitle: 'BOM产品料号列表',
  modalWidth: 1180,
  inlinePanelWidth: 900,
  queryFields: [
    { field: 'productMaterialKeyword', label: '产品料号', placeholder: '请输入产品料号或名称' },
    { field: 'productModelCode', label: '产品型号', placeholder: '请输入产品型号' },
    { field: 'productSpec', label: '尺寸规格', placeholder: '请输入尺寸规格' },
    { field: 'bomCode', label: 'BOM编码', placeholder: '请输入BOM编码', defaultHidden: true },
  ],
  columns: [
    { field: 'productModelCode', title: '产品型号', minWidth: 130 },
    { field: 'productMaterialCode', title: '产品料号', minWidth: 150 },
    { field: 'productMaterialName', title: '产品名称', minWidth: 190 },
    { field: 'productSpec', title: '尺寸规格', minWidth: 110 },
    { field: 'bomCode', title: 'BOM编码', minWidth: 150 },
    { field: 'bomName', title: 'BOM名称', minWidth: 190 },
    { field: 'versionNo', title: '版本', width: 90 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      align: 'center',
      formatter: (value) => (Number(value) === 1 ? '启用' : '停用'),
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getBomPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      status: 1,
      bomCode: params.filters?.bomCode || undefined,
      productMaterialKeyword: params.filters?.productMaterialKeyword || keyword,
      productModelId: formState.modelId || undefined,
      productModelCode: formState.modelId ? undefined : params.filters?.productModelCode || undefined,
      productSpec: params.filters?.productSpec || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id,
    code: row.productMaterialCode || '',
    name: row.productMaterialName || '',
    label: row.productMaterialCode || '',
    status: Number(row.status),
    extra: {
      bomId: row.id,
      bomCode: row.bomCode,
      bomName: row.bomName,
      bomType: row.bomType,
      bomVersion: row.versionNo,
      productMaterialId: row.productMaterialId,
      productMaterialCode: row.productMaterialCode,
      productMaterialName: row.productMaterialName,
      productModelId: row.productModelId,
      productModelCode: row.productModelCode,
      productModelName: row.productModelName,
      productSpec: row.productSpec,
      routeId: row.routeId,
      routeCode: row.routeCode,
    },
    raw: row as any,
  }),
};

const selectedOperation = computed(() =>
  operationRows.value.find((item) => item.localKey === selectedOperationKey.value),
);

const firstOperation = computed(() =>
  [...operationRows.value].sort((a, b) =>
    Number(a.sort ?? a.opSeq ?? Number.MAX_SAFE_INTEGER) -
    Number(b.sort ?? b.opSeq ?? Number.MAX_SAFE_INTEGER),
  )[0],
);

const isFormulaStartPlan = computed(() => isFormulaOperation(firstOperation.value));

const isProductionBatchGenerated = computed(() =>
  Boolean(formState.productionBatchNo) ||
  String(formState.batchStatus || '').toUpperCase() === 'GENERATED',
);

const isRootBatchReserved = computed(() => {
  try {
    return JSON.parse(formState.productionBatchContextJson || '{}').rootBatchReserved === true;
  } catch {
    return false;
  }
});
const isBatchNoLocked = computed(() => isProductionBatchGenerated.value || (isRootBatchReserved.value && formState.planStatus !== 'DRAFT'));

const batchNoHint = computed(() => {
  if (isProductionBatchGenerated.value) return '已生成，后续工序按此批号流转';
  if (formState.planStatus === 'DRAFT' && formState.id) return '草稿可沿用原批号或人工修改；点击刷新可重新生成预览，下发时校验批号是否可用';
  if (isRootBatchReserved.value) return '母批批号已在下发时锁定；未开工可撤回修改，重新下发重新计算并占号';
  if (batchPreviewLoading.value) return '正在按当前批号规则预览...';
  if (batchPreviewWarning.value) return batchPreviewWarning.value;
  if (formState.batchNo && manualBatchTouched.value) {
    return '已人工调整；保存时将校验当前规则是否允许人工改号';
  }
  if (formState.batchNo && formState.id) {
    return '已加载计划原批号；未修改时将保持不变';
  }
  if (formState.batchNo) {
    return '草稿仅作预览；确认下发时正式占用并锁定母批批号，配料开工沿用。起始流水可在“基础资料 → 批号规则 → 流水设置”中调整';
  }
  return '仅配料起始计划可预览产品批号，确认下发时锁定批号；起始流水请在批号规则的“流水设置”中维护';
});

const visibleInventoryLockRows = computed(() =>
  lockRows.value.filter((item) =>
    isExternalSourceInventoryLock(item) &&
    (activeLockTab.value === 'FG'
      ? item.lockType === 'FG'
      : item.lockType !== 'FG' && item.operationKey === selectedOperationKey.value),
  ),
);

const selectedInventoryLock = computed(() =>
  visibleInventoryLockRows.value.find((item) => item.localKey === selectedInventoryLockKey.value),
);

const hasVisibleWipLockForSelectedOperation = computed(() =>
  lockRows.value.some((item) =>
    isExternalSourceInventoryLock(item) &&
    item.lockType !== 'FG' &&
    item.operationKey === selectedOperationKey.value,
  ),
);

const hasVisibleFgLock = computed(() =>
  lockRows.value.some((item) => isExternalSourceInventoryLock(item) && item.lockType === 'FG'),
);

const activeLockTotal = computed(() =>
  lockRows.value
    .filter(isExternalSourceInventoryLock)
    .filter((item) => !['CANCELLED', 'CONSUMED', 'RELEASED'].includes(String(item.lockStatus || 'ACTIVE').toUpperCase()))
    .reduce((sum, item) => sum + Number(item.lockQty || 0), 0),
);

const splitStyle = computed<Record<string, string>>(() => ({
  '--pp-operation-panel-width': `${splitLeftPercent.value}%`,
}));

const inventoryPickTotal = computed(() =>
  Object.values(inventoryPickQty).reduce((sum, value) => sum + normalizeNumber(value), 0),
);

const checkedOperations = computed(() =>
  operationRows.value.filter((item) => checkedOperationKeys.value.includes(item.localKey)),
);

const planStatusKey = computed(() => String(formState.planStatus || '').toUpperCase());

const isReadonly = computed(() =>
  ['CANCELLED', 'CLOSED', 'PAUSED', 'RELEASED'].includes(planStatusKey.value),
);

const isProductIdentityLocked = computed(() => Boolean(formState.id) && (
  String(formState.planStatus || '').toUpperCase() !== 'DRAFT'
  || isProductionBatchGenerated.value || hasExecutedOperationInDetail()
));

const isReleasedPlan = computed(() =>
  planStatusKey.value === 'RELEASED',
);

const isPausedPlan = computed(() => planStatusKey.value === 'PAUSED');

const isCancelledPlan = computed(() => ['CANCELLED', 'CANCELED'].includes(planStatusKey.value));

const canPausePlan = computed(() => Boolean(formState.id) && isReleasedPlan.value);

const canResumePlan = computed(() => Boolean(formState.id) && isPausedPlan.value);

const canWithdrawPlan = computed(() => Boolean(formState.id) && isReleasedPlan.value);

const canCancelPlan = computed(() =>
  Boolean(formState.id) && ['PAUSED', 'RELEASED'].includes(planStatusKey.value),
);

const canSetOperationStatus = computed(() =>
  ['PAUSED', 'RELEASED'].includes(planStatusKey.value),
);

const showOperationSelectColumn = computed(() => routeMaintenanceMode.value || canSetOperationStatus.value);

const showOperationSeqColumn = computed(() => !routeMaintenanceMode.value);

const showOperationMoveColumn = computed(() => false);

const canSetPlanStatus = computed(() =>
  Boolean(formState.id) && hasAccessByCodes(['mes:pp:plan:status-admin']),
);

const planHeaderDescription = computed(() => {
  const statusLabel = getPlanStatusMeta(formState.planStatus).label;
  const planNo = formState.planNo || '保存后生成';
  const creatorName = resolveCreatorName(formState.creator);
  const createTime = normalizeLocalDateTime(formState.createTime) || '-';
  return `${statusLabel}状态 · 生产计划编号：${planNo} · 创建人：${creatorName} · 创建时间：${createTime}`;
});

const readonlyReportDateColumns = computed(() => {
  const dateSet = new Set<string>();
  operationRows.value.forEach((row) => {
    const source = row as any;
    const reportQtyByDate = {
      ...(source.reportQtyByDate || {}),
      ...(source.dailyReportQtyMap || {}),
    };
    Object.keys(reportQtyByDate).forEach((date) => {
      if (dayjs(date).isValid()) {
        dateSet.add(dayjs(date).format('YYYY-MM-DD'));
      }
    });
  });
  return Array.from(dateSet).sort();
});

const operationReportDateColumns = computed(() => []);

function nextLocalKey(prefix: string) {
  seed.value += 1;
  return `${prefix}-${Date.now()}-${seed.value}`;
}

function optionLabel(options: SelectOption[], value?: number) {
  return options.find((item) => Number(item.value) === Number(value))?.label || '';
}

function optionCode(options: SelectOption[], value?: number) {
  return options.find((item) => Number(item.value) === Number(value))?.code || '';
}

function optionName(options: SelectOption[], value?: number) {
  const option = options.find((item) => Number(item.value) === Number(value));
  return option?.name || normalizeNameLabel(option?.label);
}

function normalizeNameLabel(label?: string) {
  const parts = String(label || '').split('/');
  return parts[parts.length - 1]?.trim() || String(label || '');
}

function toCodeNameOptions(options: MesHcPlanOrderApi.StaticOption[] = []) {
  return options.map((item) => ({
    value: item.code,
    label: item.name,
    id: item.id,
    code: item.code,
    name: item.name,
  }));
}

function toSizeSpecOptions(options: MesHcPlanOrderApi.StaticOption[] = []) {
  return [
    { value: '', label: '空', id: 0, code: '', name: '' },
    ...toCodeNameOptions(options).filter((item) => item.code !== ''),
  ];
}

function toRecipeCodeOptions(options: MesHcRecipeApi.SelectOption[] = []) {
  return options.map((item) => ({
    ...item,
    // 配方受控检索只展示配方编码；选中后同时回填 recipeId / recipeCode / recipeName。
    label: item.code || item.label,
    name: item.label,
  }));
}

function codeNameOptionName(options: CodeNameOption[], code?: string) {
  return options.find((item) => item.code === code)?.name || '';
}

function isFormulaOperation(operation?: MesHcPlanOrderApi.Operation) {
  const opCode = String(operation?.opCode || '').trim().toUpperCase();
  const opName = String(operation?.opName || '').trim();
  return opCode === 'FORMULA' || opName === '配料';
}

function normalizeManualBatchNo(value?: string) {
  return String(value || '').trim().toUpperCase();
}

function normalizeManualBatchNoInput() {
  formState.batchNo = normalizeManualBatchNo(formState.batchNo);
}

function markManualBatchTouched() {
  ++batchPreviewRequestSequence;
  clearBatchPreviewTimer();
  batchPreviewLoading.value = false;
  manualBatchTouched.value = true;
  batchPreviewWarning.value = '';
}

function clearBatchPreviewTimer() {
  if (batchPreviewTimer) {
    clearTimeout(batchPreviewTimer);
    batchPreviewTimer = undefined;
  }
}

function canAutoPreviewBatchNo() {
  return Boolean(
    isFormulaStartPlan.value &&
      !isReadonly.value &&
      !isBatchNoLocked.value &&
      !manualBatchTouched.value &&
      formState.productionStartDate &&
      (!formState.id || !formState.batchNo || formState.planStatus === 'DRAFT') &&
      firstOperation.value &&
      (formState.modelCode || formState.motherModelCode || formState.materialCode),
  );
}

function scheduleBatchNoPreview() {
  clearBatchPreviewTimer();
  if (!canAutoPreviewBatchNo()) return;
  batchPreviewTimer = setTimeout(() => {
    void previewAndFillBatchNo(false);
  }, 350);
}

let batchPreviewRequestSequence = 0;

async function previewAndFillBatchNo(force = false) {
  if (!isFormulaStartPlan.value || isReadonly.value || isBatchNoLocked.value) return;
  if (!formState.productionStartDate) {
    batchPreviewWarning.value = '请先填写计划开始日期';
    if (force) message.warning('请先填写计划开始日期');
    return;
  }
  if (!force && !canAutoPreviewBatchNo()) return;
  const operation = firstOperation.value;
  if (!operation) return;
  const currentToken = openToken.value;
  const requestSequence = ++batchPreviewRequestSequence;
  batchPreviewLoading.value = true;
  try {
    const result = await previewPlanRootBatchNo({
      id: formState.id,
      planDate: formState.planDate,
      productionStartDate: formState.productionStartDate,
      materialCode: formState.materialCode,
      categoryCode: formState.categoryCode,
      prodType: formState.prodType,
      modelCode: formState.modelCode,
      motherModelCode: formState.motherModelCode,
      batchRuleId: formState.batchRuleId,
      batchRuleCode: formState.batchRuleCode,
      useBoundRule: Boolean(formState.id && formState.batchRuleId && formState.planStatus !== 'DRAFT'),
      opCode: operation.opCode,
      opName: operation.opName,
      workCenterId: operation.workCenterId,
    });
    if (currentToken !== openToken.value || requestSequence !== batchPreviewRequestSequence) return;
    if (!force && !canAutoPreviewBatchNo()) return;
    batchPreviewWarning.value = result.warning || '';
    if (result.batchNo && (force || canAutoPreviewBatchNo())) {
      formState.batchNo = normalizeManualBatchNo(result.batchNo);
      formState.batchRuleId = result.ruleId;
      formState.batchRuleCode = result.ruleCode || formState.batchRuleCode;
      manualBatchTouched.value = false;
    }
  } catch {
    if (force) {
      message.warning('暂未能预览产品批号，请检查产品型号、工艺路线和批号规则');
    }
    if (currentToken === openToken.value && requestSequence === batchPreviewRequestSequence) {
      batchPreviewWarning.value = '';
    }
  } finally {
    if (currentToken === openToken.value && requestSequence === batchPreviewRequestSequence) {
      batchPreviewLoading.value = false;
    }
  }
}

function equipmentPadTypeName(option?: SelectOption) {
  if (!option?.applicablePadType) return '';
  return option.applicablePadTypeName || ({ WHITE_PAD: '白垫', BLACK_PAD: '黑垫', COMMON: '通用' } as Record<string, string>)[option.applicablePadType] || '';
}

function equipmentOptionDisplayLabel(option: SelectOption) {
  const base = option.code ? `${option.code} / ${normalizeNameLabel(option.label)}` : normalizeNameLabel(option.label);
  const padTypeName = equipmentPadTypeName(option);
  return padTypeName ? `${base}（${padTypeName}）` : base;
}

function isEquipmentCompatibleWithPlanPadType(option: SelectOption, categoryCode = formState.categoryCode) {
  const requiredPadType = String(categoryCode || '').trim().toUpperCase();
  if (!['WHITE_PAD', 'BLACK_PAD'].includes(requiredPadType)) return true;
  const equipmentPadType = String(option.applicablePadType || '').trim().toUpperCase();
  if (!equipmentPadType || equipmentPadType === 'COMMON') return true;
  return equipmentPadType === requiredPadType;
}

function syncMotherFieldsFromProduct() {
  formState.motherMaterialId = formState.materialId;
  formState.motherMaterialCode = formState.materialCode;
  formState.motherMaterialName = formState.materialName;
  formState.motherModelId = formState.modelId;
  formState.motherModelCode = formState.modelCode;
  formState.motherModelName = formState.modelName;
}

function resolveProdTypeByModel(modelCode?: string, bomType?: string, preferredCode?: string) {
  if (preferredCode && prodTypeOptions.value.some((item) => item.code === preferredCode)) {
    return preferredCode;
  }
  if (String(bomType || '').includes('量产') || /^W/i.test(String(modelCode || '').trim())) {
    return 'MASS';
  }
  return 'RND_TRIAL';
}

function applyProdTypeByModel(modelCode?: string, bomType?: string, preferredCode?: string, preferredName?: string) {
  const code = resolveProdTypeByModel(modelCode, bomType, preferredCode);
  formState.prodType = code;
  formState.prodTypeName = preferredName || codeNameOptionName(prodTypeOptions.value, code);
}

function buildProductModelOptionFromBom(bom: MesHcBomApi.Bom): MesHcBomApi.ProductModelOption | undefined {
  if (!bom.productModelId) return undefined;
  return {
    value: Number(bom.productModelId),
    label: bom.productModelCode || '',
    code: bom.productModelCode || '',
    modelName: bom.productModelName || '',
    productModelId: Number(bom.productModelId),
    productModelCode: bom.productModelCode,
    productModelName: bom.productModelName,
    bomType: bom.bomType,
    status: bom.status,
  };
}

function normalizeNumber(value?: number) {
  const result = Number(value || 0);
  return Number.isFinite(result) ? result : 0;
}

function normalizeLocalDate(value?: unknown) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    return '';
  }
  const date = dayjs(value as any);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function normalizeLocalDateTime(value?: unknown) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
    }
    return '';
  }
  const date = dayjs(value as any);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function resolveCreatorName(creator?: string | number) {
  if (creator == null || creator === '') return '-';
  return userNameMap.value[String(creator)] || String(creator);
}

function formatUnitLabel(code?: string, name?: string) {
  return [code, name].filter(Boolean).join('/') || code || name || '';
}

function orderDueUnitLabel() {
  return (
    formatUnitLabel(
      formState.orderDueUnitCode || formState.targetUnitCode || formState.targetUom,
      formState.orderDueUnitName || formState.targetUnitName,
    ) || '张'
  );
}

function operationStatusLabel(status?: string) {
  return OPERATION_STATUS_OPTIONS.find((item) => item.value === status)?.label || status || '未下达';
}

function operationStatusClass(status?: string) {
  return `pp-status-text pp-status-text--${String(status || 'NOT_RELEASED').toLowerCase()}`;
}

function operationStatusIconClass(status?: string) {
  return `pp-status-icon pp-status-icon--${String(status || 'NOT_RELEASED').toLowerCase()}`;
}

function normalizeOptionalNumber(value?: unknown) {
  if (value == null || value === '') return undefined;
  const result = Number(value);
  return Number.isFinite(result) ? result : undefined;
}

function formatOperationReportQty(value: number | undefined) {
  if (value == null) return '-';
  return formatNumber(value, 3);
}

function operationLockedQty(row: EditableOperation) {
  return lockRows.value
    .filter((item) =>
      isExternalSourceInventoryLock(item) &&
      item.lockType !== 'FG' &&
      item.operationKey === row.localKey,
    )
    .reduce((sum, item) => sum + Number(item.lockQty || 0), 0);
}

function isExternalSourceInventoryLock(row?: Pick<EditableInventoryLock, 'sourcePlanId' | 'sourcePlanNo'>) {
  if (!row) return false;
  const currentPlanNo = normalizePlanKey(formState.planNo);
  const sourcePlanNo = normalizePlanKey(row.sourcePlanNo);
  if (currentPlanNo && sourcePlanNo) {
    return currentPlanNo !== sourcePlanNo;
  }
  const currentPlanId = formState.id == null ? undefined : Number(formState.id);
  const sourcePlanId = row.sourcePlanId == null ? undefined : Number(row.sourcePlanId);
  if (
    currentPlanId !== undefined &&
    sourcePlanId !== undefined &&
    Number.isFinite(currentPlanId) &&
    Number.isFinite(sourcePlanId)
  ) {
    return currentPlanId !== sourcePlanId;
  }
  return true;
}

function normalizePlanKey(value?: unknown) {
  return String(value ?? '').trim().toUpperCase();
}

function operationDailyReportQty(row: EditableOperation, date: string) {
  const source = row as any;
  const value = source.dailyReportQtyMap?.[date] ?? source.reportQtyByDate?.[date];
  return normalizeOptionalNumber(value);
}

function operationCumulativeReportQty(row: EditableOperation) {
  const values = operationReportDateColumns.value
    .map((date) => operationDailyReportQty(row, date))
    .filter((value): value is number => value != null);
  if (values.length === 0) {
    return '-';
  }
  return formatOperationReportQty(values.reduce((sum, value) => sum + value, 0));
}

function checkedOperationIds() {
  const ids = checkedOperations.value
    .map((item) => Number(item.id))
    .filter((id) => Number.isFinite(id) && id > 0);
  if (ids.length !== checkedOperations.value.length) {
    message.warning('存在未保存的工序，不能设置状态');
    return [];
  }
  return ids;
}

function planBatchNo() {
  return formState.productionBatchNo || formState.batchNo || formState.parentProductionBatchNo || '';
}

function operationSegmentBatchNo(row: EditableOperation) {
  return row.productionBatchNo || row.batchNo || '';
}

function isSegmentedInstructionOperation(row: EditableOperation) {
  const name = `${row.opName || ''} ${row.opCode || ''}`.toUpperCase();
  if (!name.trim()) return false;
  return !['FORMULA', 'MIXING', '配料', '湿法', 'WET', '磨皮1', 'ROUGH1', 'GRINDING1'].some((keyword) =>
    name.includes(keyword),
  );
}

function selectedSavedOperationRows() {
  const rows = [...checkedOperations.value];
  const operationIds = rows
    .map((item) => Number(item.id))
    .filter((id) => Number.isFinite(id) && id > 0);
  if (operationIds.length !== rows.length) {
    message.warning('存在未保存的工序，不能下达指令');
    return [];
  }
  return rows;
}

function selectedOperationSegmentOptions(rows: EditableOperation[]) {
  const map = new Map<string, ProductionInstructionSegmentOption>();
  rows.forEach((row) => {
    if (!isSegmentedInstructionOperation(row)) return;
    const value = operationSegmentBatchNo(row);
    if (!value || map.has(value)) return;
    map.set(value, { label: value, value });
  });
  return [...map.values()];
}

function selectedOperationOption(rows: EditableOperation[]) {
  const operationIds = rows.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0);
  const names = rows.map((item) => item.opName || item.opCode || '').filter(Boolean);
  if (rows.length === 1) {
    const row = rows[0]!;
    return {
      label: row.opName || row.opCode || '选中工序',
      operationCode: row.opCode,
      operationIds,
      operationName: row.opName,
      planOperationId: Number(row.id),
      processCode: row.opCode,
      processId: row.routeOperationId,
      processName: row.opName,
      value: String(row.id || row.localKey),
    };
  }
  const label = names.join('、') || `已选 ${rows.length} 道工序`;
  return {
    label,
    operationIds,
    operationName: label,
    processName: label,
    value: `multi-${operationIds.join('-')}`,
  };
}

function instructionActionLabel(actionType: 'DAILY' | 'PAUSE' | 'RESUME') {
  const labels = {
    DAILY: '下达指令',
    PAUSE: '暂停',
    RESUME: '复工',
  };
  return labels[actionType];
}

function hasExecutedOperationInDetail() {
  return operationRows.value.some((row) => {
    const status = String(row.operationStatus || '').toUpperCase();
    const source = row as any;
    const reportValues = [
      ...Object.values(source.dailyReportQtyMap || {}),
      ...Object.values(source.reportQtyByDate || {}),
    ];
    return status === 'RUNNING'
      || status === 'FINISHED'
      || reportValues.some((value) => Number(value || 0) > 0);
  });
}

function showPlanWithdrawBlockedMessage() {
  message.warning('已有工序执行，不能撤回，如有异常请下达暂停指令！');
}

async function handleWithdrawPlan() {
  if (!formState.id) {
    message.warning('请先保存生产计划后再撤回');
    return;
  }
  if (!canWithdrawPlan.value) {
    message.warning('只有已下达状态的计划才能撤回');
    return;
  }
  if (hasExecutedOperationInDetail()) {
    showPlanWithdrawBlockedMessage();
    return;
  }
  const confirmed = await new Promise<boolean>((resolve) => {
    AntModal.confirm({
      title: '撤回计划',
      content: '撤回后恢复草稿，可重新选择产品、日期、路线及库存来源。原母批预约解除、库存占用释放；原批号仍可选用，也可手动修改，下发时重新校验。确认撤回计划？',
      okText: '确认撤回',
      cancelText: '取消',
      async onOk() {
        resolve(true);
      },
      onCancel: () => resolve(false),
    });
  });
  if (!confirmed) return;
  try {
    await withdrawPlanOrder(Number(formState.id));
    message.success('计划已撤回为草稿状态');
    await loadDetail(Number(formState.id), openToken.value);
  } catch (error: any) {
    const errorMessage = String(error?.message || error?.msg || '');
    if (errorMessage.includes('已有工序执行')) {
      showPlanWithdrawBlockedMessage();
    }
  }
}

async function openPlanInstruction(actionType: 'PAUSE' | 'RESUME') {
  if (!formState.id) {
    message.warning('请先保存生产计划后再下达指令');
    return;
  }
  if (actionType === 'PAUSE' && !canPausePlan.value) {
    message.warning('只有已下达计划才能暂停');
    return;
  }
  if (actionType === 'RESUME' && !canResumePlan.value) {
    message.warning('只有暂停状态的计划才能复工');
    return;
  }
  if (actionType === 'PAUSE') {
    const confirmed = await new Promise<boolean>((resolve) => {
      AntModal.confirm({
        title: '计划暂停',
        content: '计划暂停会向各工序下达暂停指令并锁住报工，确认后将打开指令下达界面。',
        okText: '继续下达',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      });
    });
    if (!confirmed) return;
  }
  const batchNo = planBatchNo();
  productionInstructionType.value = actionType;
  productionInstructionScope.value = 'PLAN';
  productionInstructionTitle.value = `计划${instructionActionLabel(actionType)}`;
  productionInstructionOperationOptions.value = [];
  productionInstructionSegmentOptions.value = [];
  productionInstructionShowOperation.value = false;
  productionInstructionShowSegment.value = false;
  productionInstructionContext.value = {
    batchNo,
    instructionType: actionType,
    planId: Number(formState.id),
    planNo: formState.planNo,
    productionBatchNo: batchNo,
    scopeType: 'PLAN',
  };
  productionInstructionOpen.value = true;
}

function openOperationInstruction(actionType: 'DAILY' | 'PAUSE' | 'RESUME') {
  if (!formState.id) {
    message.warning('请先保存生产计划后再下达指令');
    return;
  }
  const rows = selectedSavedOperationRows();
  if (rows.length === 0) {
    message.warning('请先勾选需要下达指令的工序');
    return;
  }
  if (actionType === 'RESUME' && rows.some((row) => row.operationStatus !== 'PAUSED')) {
    message.warning('只有暂停状态的工序才能复工');
    return;
  }
  if (actionType === 'PAUSE' && rows.some((row) => ['CANCELLED', 'FINISHED', 'PAUSED'].includes(String(row.operationStatus || '').toUpperCase()))) {
    message.warning('已暂停、已完工或已取消的工序不能暂停');
    return;
  }
  const option = selectedOperationOption(rows);
  const segments = selectedOperationSegmentOptions(rows);
  const firstRow = rows[0]!;
  const productionBatchNo = formState.productionBatchNo || firstRow.parentProductionBatchNo || formState.batchNo || '';
  productionInstructionType.value = actionType;
  productionInstructionScope.value = 'OPERATION';
  productionInstructionTitle.value = actionType === 'DAILY' ? '下达日常指令' : `工序${instructionActionLabel(actionType)}`;
  productionInstructionOperationOptions.value = [option];
  productionInstructionSegmentOptions.value = segments;
  productionInstructionShowOperation.value = true;
  productionInstructionShowSegment.value = segments.length > 0;
  productionInstructionContext.value = {
    batchNo: segments.length === 1 ? segments[0]!.value : productionBatchNo,
    instructionType: actionType,
    operationCode: rows.length === 1 ? firstRow.opCode : '',
    operationIds: rows.map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0),
    operationName: option.operationName,
    planId: Number(formState.id),
    planNo: formState.planNo,
    planOperationId: rows.length === 1 ? Number(firstRow.id) : undefined,
    processCode: rows.length === 1 ? firstRow.opCode : '',
    processId: rows.length === 1 ? firstRow.routeOperationId : undefined,
    processName: option.processName,
    productMaterialCode: formState.materialCode,
    productModelCode: formState.modelCode,
    productionBatchNo,
    scopeType: 'OPERATION',
    segmentBatchNo: segments.length === 1 ? segments[0]!.value : '',
  };
  productionInstructionOpen.value = true;
}

async function handleProductionInstructionSuccess() {
  message.success('指令已下达并同步状态');
  checkedOperationKeys.value = [];
  if (formState.id) {
    await loadDetail(Number(formState.id), openToken.value);
  }
}

function statusActionLabel(actionType: OperationStatusAction) {
  const labels: Record<OperationStatusAction, string> = {
    CANCEL: '取消',
    FINISH: '完工',
    PAUSE: '暂停',
    RESUME: '复工',
  };
  return labels[actionType];
}

function planStatusLabel(status?: string) {
  return getPlanStatusMeta(status).label;
}

function planStatusActionLabel(actionType?: string) {
  const labels: Record<string, string> = {
    CANCEL: '作废取消',
    CLOSE: '关闭',
    PAUSE: '暂停',
    RELEASE: '下达',
    RESUME: '复工',
    STATUS: '状态调整',
    WITHDRAW: '撤回计划',
  };
  return labels[String(actionType || '').toUpperCase()] || actionType || '-';
}

function buildPlanStatusContent(payload: MesHcPlanOrderApi.PlanStatusReq) {
  return h('div', { class: 'pp-status-action-modal' }, [
    h('div', { class: 'pp-status-action-modal__label' }, '目标状态'),
    h(Select, {
      class: 'pp-status-action-modal__control',
      defaultValue: payload.planStatus,
      options: PLAN_STATUS_OPTIONS,
      onChange: (value: PlanStatusValue) => {
        payload.planStatus = value;
      },
    }),
    h('div', { class: 'pp-status-action-modal__label' }, '状态操作备注'),
    h(Input.TextArea, {
      class: 'pp-status-action-modal__textarea',
      rows: 3,
      placeholder: '请输入本次状态调整原因或说明',
      onChange: (event: Event) => {
        payload.reasonRemark = (event.target as HTMLTextAreaElement).value;
      },
    }),
  ]);
}

async function handlePlanStatusAction() {
  if (!formState.id) {
    message.warning('请先保存生产计划后再设置状态');
    return;
  }
  const currentStatus = String(formState.planStatus || 'DRAFT').toUpperCase() as PlanStatusValue;
  const defaultStatus = (PLAN_STATUS_OPTIONS.find((item) => item.value !== currentStatus)?.value || 'RELEASED') as PlanStatusValue;
  const payload: MesHcPlanOrderApi.PlanStatusReq = {
    id: Number(formState.id),
    planStatus: defaultStatus,
    reasonRemark: '',
  };
  await new Promise<void>((resolve, reject) => {
    AntModal.confirm({
      title: '设置生产计划状态',
      content: () => buildPlanStatusContent(payload),
      okText: '确认设置',
      cancelText: '取消',
      async onOk() {
        if (!payload.planStatus) {
          message.warning('请选择目标状态');
          return Promise.reject(new Error('empty plan status'));
        }
        if (payload.planStatus === currentStatus) {
          message.warning('目标状态与当前状态一致');
          return Promise.reject(new Error('same plan status'));
        }
        if (!payload.reasonRemark?.trim()) {
          message.warning('请填写状态操作备注');
          return Promise.reject(new Error('empty plan status remark'));
        }
        await updatePlanStatus(payload);
        message.success(`生产计划状态已设置为${planStatusLabel(payload.planStatus)}`);
        await loadDetail(Number(formState.id), openToken.value);
        resolve();
      },
      onCancel: () => reject(new Error('cancel')),
    });
  }).catch(() => undefined);
}

async function openStatusLogModal() {
  if (!formState.id) {
    message.warning('请先保存生产计划后再查看操作日志');
    return;
  }
  statusLogOpen.value = true;
  statusLogLoading.value = true;
  try {
    statusLogRows.value = await getPlanStatusLogList(Number(formState.id));
  } finally {
    statusLogLoading.value = false;
  }
}

function buildStatusActionContent(
  actionType: OperationStatusAction,
  payload: MesHcPlanOrderApi.OperationStatusReq,
) {
  if (actionType === 'RESUME') {
    const ResumeActionContent = defineComponent({
      name: 'ResumeActionContent',
      setup() {
        const resumeDate = ref<string>();
        return () => h('div', { class: 'pp-status-action-modal' }, [
          h('div', { class: 'pp-status-action-modal__label' }, `确认将 ${checkedOperations.value.length} 道工序从暂停恢复为执行中？`),
          h('div', { class: 'pp-status-action-modal__label' }, '复工日期'),
          h(Select, {
            class: 'pp-status-action-modal__control',
            value: resumeDate.value,
            options: readonlyReportDateColumns.value.map((date) => ({ label: date, value: date })),
            placeholder: '请选择计划日期范围内的复工日期',
            onChange: (value: string) => {
              resumeDate.value = value;
              payload.resumeDate = value;
            },
          }),
          h('div', { class: 'pp-status-action-modal__label' }, '复工说明'),
          h(Input.TextArea, {
            class: 'pp-status-action-modal__textarea',
            rows: 3,
            placeholder: '复工说明（可选）',
            onChange: (event: Event) => {
              payload.reasonRemark = String((event.target as HTMLTextAreaElement).value || '').trim();
            },
          }),
        ]);
      },
    });
    return h(ResumeActionContent);
  }
  if (actionType === 'FINISH') {
    payload.finishTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    return h('div', { class: 'pp-status-action-modal' }, [
      h('div', { class: 'pp-status-action-modal__label' }, '实际完工日期'),
      h(DatePicker, {
        class: 'w-full',
        defaultValue: dayjs(),
        format: 'YYYY-MM-DD',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        onChange: (_value: unknown, dateString: string) => {
          payload.finishTime = dateString;
        },
      }),
      h('div', { class: 'pp-status-action-modal__label' }, '完工备注'),
      h(Input.TextArea, {
        class: 'pp-status-action-modal__textarea',
        rows: 3,
        placeholder: '请输入完工备注',
        onChange: (event: Event) => {
          payload.reasonRemark = String((event.target as HTMLTextAreaElement).value || '').trim();
        },
      }),
    ]);
  }
  if (actionType === 'PAUSE') {
    payload.pauseScope = 'DATES';
    const PauseActionContent = defineComponent({
      name: 'PauseActionContent',
      setup() {
        const selectedDates = ref<string[]>([]);
        return () => h('div', { class: 'pp-status-action-modal' }, [
          h('div', { class: 'pp-status-action-modal__label' }, '暂停日期'),
          h('div', { class: 'pp-status-action-modal__date-list' },
            readonlyReportDateColumns.value.map((date) =>
              h(Checkbox, {
                checked: selectedDates.value.includes(date),
                onChange: (event: any) => {
                  selectedDates.value = event.target.checked
                    ? [...selectedDates.value, date]
                    : selectedDates.value.filter((item) => item !== date);
                  payload.statusDates = selectedDates.value;
                },
              }, () => date),
            ),
          ),
          h('div', { class: 'pp-status-action-modal__label' }, '暂停说明'),
          h(Input.TextArea, {
            class: 'pp-status-action-modal__textarea',
            rows: 3,
            placeholder: '请输入暂停说明',
            onChange: (event: Event) => {
              payload.reasonRemark = String((event.target as HTMLTextAreaElement).value || '').trim();
            },
          }),
        ]);
      },
    });
    return h(PauseActionContent);
  }
  return h('div', { class: 'pp-status-action-modal' }, [
    h('div', { class: 'pp-status-action-modal__label' }, '取消后该工序不可再报工、不可复工，行记录将按取消状态展示。'),
    h('div', { class: 'pp-status-action-modal__label' }, '取消原因'),
    h(Input.TextArea, {
      class: 'pp-status-action-modal__textarea',
      rows: 3,
      placeholder: '请输入取消原因',
      onChange: (event: Event) => {
        payload.reasonRemark = String((event.target as HTMLTextAreaElement).value || '').trim();
      },
    }),
  ]);
}

function validateStatusActionPayload(payload: MesHcPlanOrderApi.OperationStatusReq) {
  if (payload.actionType === 'FINISH' && (!payload.finishTime || !payload.reasonRemark)) {
    message.warning('请填写完工时间和完工备注');
    return false;
  }
  if (payload.actionType === 'PAUSE') {
    if (!payload.reasonRemark) {
      message.warning('请填写暂停说明');
      return false;
    }
    if (!payload.statusDates?.length) {
      message.warning('请选择至少一个暂停日期');
      return false;
    }
  }
  if (payload.actionType === 'RESUME' && !payload.resumeDate) {
    message.warning('请选择复工日期');
    return false;
  }
  if (payload.actionType === 'CANCEL' && !payload.reasonRemark) {
    message.warning('请填写取消原因');
    return false;
  }
  return true;
}

async function handleOperationStatusAction(actionType: OperationStatusAction) {
  if (!canSetOperationStatus.value) return;
  const operationIds = checkedOperationIds();
  if (operationIds.length === 0) {
    message.warning('请先勾选需要设置状态的工序');
    return;
  }
  const payload: MesHcPlanOrderApi.OperationStatusReq = { actionType, operationIds };
  const label = statusActionLabel(actionType);
  const confirmed = await new Promise<boolean>((resolve) => {
    AntModal.confirm({
      title: `确认${label}工序`,
      content: () => buildStatusActionContent(actionType, payload),
      okText: `确认${label}`,
      cancelText: '取消',
      async onOk() {
        if (!validateStatusActionPayload(payload)) {
          return Promise.reject(new Error('invalid status payload'));
        }
        return resolve(true);
      },
      onCancel: () => resolve(false),
    });
  });
  if (!confirmed || !formState.id) return;
  await updatePlanOperationStatus(payload);
  message.success(`工序${label}成功`);
  checkedOperationKeys.value = [];
  await loadDetail(Number(formState.id), openToken.value);
}

function handleOperationChecked(row: EditableOperation, checked: boolean) {
  const keys = new Set(checkedOperationKeys.value);
  if (checked) {
    keys.add(row.localKey);
  } else {
    keys.delete(row.localKey);
  }
  checkedOperationKeys.value = [...keys];
}

function handleOperationRowClick(row: EditableOperation) {
  setSelectedOperation(row.localKey);
  if (canSetOperationStatus.value) {
    handleOperationChecked(row, !checkedOperationKeys.value.includes(row.localKey));
  }
}

function expandLockPanel() {
  if (lockPanelCollapsed.value) {
    lockPanelCollapsed.value = false;
  }
}

function collapseLockPanel() {
  lockPanelCollapsed.value = true;
}

function clampSplitPercent(value: number) {
  return Math.min(72, Math.max(32, value));
}

function updateSplitWidth(clientX: number) {
  const container = splitContainerRef.value;
  if (!container) return;
  const rect = container.getBoundingClientRect();
  if (rect.width <= 0) return;
  splitLeftPercent.value = clampSplitPercent(((clientX - rect.left) / rect.width) * 100);
}

function cleanupSplitResize() {
  if (!splitResizeCleanup) return;
  splitResizeCleanup();
  splitResizeCleanup = undefined;
}

function startSplitResize(event: PointerEvent) {
  if (lockPanelCollapsed.value) return;
  event.preventDefault();
  cleanupSplitResize();
  splitDragging.value = true;
  const previousCursor = document.body.style.cursor;
  const previousUserSelect = document.body.style.userSelect;
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';

  const handlePointerMove = (moveEvent: PointerEvent) => {
    updateSplitWidth(moveEvent.clientX);
  };
  const handlePointerEnd = () => {
    cleanupSplitResize();
  };
  document.addEventListener('pointermove', handlePointerMove);
  document.addEventListener('pointerup', handlePointerEnd);
  document.addEventListener('pointercancel', handlePointerEnd);
  splitResizeCleanup = () => {
    document.removeEventListener('pointermove', handlePointerMove);
    document.removeEventListener('pointerup', handlePointerEnd);
    document.removeEventListener('pointercancel', handlePointerEnd);
    document.body.style.cursor = previousCursor;
    document.body.style.userSelect = previousUserSelect;
    splitDragging.value = false;
  };
  updateSplitWidth(event.clientX);
}

function resetSplitResize() {
  splitLeftPercent.value = 58;
}

async function saveCheckedOperationsAsNewGlobalRoute() {
  if (isReadonly.value || checkedOperations.value.length === 0) return;
  if (!routeMaintenanceMode.value) {
    message.warning('请先进入路线维护模式');
    return;
  }
  let routeName = '';
  const selectedOperations = [...checkedOperations.value];
  await new Promise<void>((resolve) => {
    AntModal.confirm({
      title: '选择工序另存新工艺路线',
      content: () =>
        h('div', { class: 'pp-save-route-modal' }, [
          h('div', { class: 'pp-save-route-modal__tips' }, '已选择工序：'),
          h(
            'div',
            { class: 'pp-save-route-modal__chips' },
            selectedOperations.flatMap((row, index) => [
              index > 0 ? h('span', { class: 'pp-save-route-modal__comma' }, '，') : null,
              h('span', { class: 'pp-save-route-modal__chip' }, row.opName || row.opCode || '-'),
            ]),
          ),
          h(Input, {
            placeholder: '请输入新的通用标准路线名称',
            onChange: (event: Event) => {
              routeName = String((event.target as HTMLInputElement).value || '').trim();
            },
          }),
        ]),
      okText: '确认另存',
      cancelText: '取消',
      async onOk() {
        if (!routeName) {
          message.warning('请输入通用标准路线名称');
          return Promise.reject(new Error('route name required'));
        }
        const routeCode = `GLOBAL-${dayjs().format('YYYYMMDDHHmmss')}`;
        await createRoute({
          applicableScope: 'GLOBAL',
          routeCode,
          routeName,
          productLevel: 'FG',
          versionNo: 'V1.0',
          routeType: '标准路线',
          status: 1,
          remark: `由生产计划 ${formState.planNo || '未保存草稿'} 勾选工序另存`,
          routeOperations: selectedOperations.map((row, index) => ({
            routeCode,
            operationCode: row.opCode,
            operationName: row.opName,
            seqNo: index + 1,
            workCenterId: row.workCenterId,
            workCenterCode: row.workCenterCode,
            workCenterName: row.workCenterName,
            reportRequired: true,
            conversionRate: row.yieldRate || 1,
            outputUnitId: row.unitId,
            outputUnitCode: row.unitCode,
            outputUnitName: row.unitName,
            outputUom: row.uom || row.unitCode,
            remark: row.instructionText,
            id: 0,
          })),
          id: 0,
        });
        checkedOperationKeys.value = [];
        await loadMaterialDependencies(formState.materialId);
        const newRouteId = Number(routeOptions.value.find((item) => item.code === routeCode)?.value);
        if (!Number.isFinite(newRouteId) || newRouteId <= 0) {
          message.warning('新路线已保存，但未能在下拉框中定位，请重新选择');
          resolve();
          return;
        }
        formState.routeId = newRouteId;
        await handleRouteChange(newRouteId);
        message.success('已按选中工序另存为通用标准路线');
        resolve();
      },
      onCancel: () => resolve(),
    });
  });
}

function toggleRouteMaintenanceMode() {
  if (isReadonly.value) return;
  routeMaintenanceMode.value = !routeMaintenanceMode.value;
}

function clearRouteAndOperations() {
  formState.routeId = undefined;
  formState.routeCode = '';
  formState.routeName = '';
  formState.routeVersion = '';
  operationRows.value = [];
  lockRows.value = [];
  selectedOperationKey.value = '';
  checkedOperationKeys.value = [];
  recomputeSummary();
}

function moveOperation(localKey: string, direction: -1 | 1) {
  if (isReadonly.value || !routeMaintenanceMode.value) return;
  const currentIndex = operationRows.value.findIndex((item) => item.localKey === localKey);
  const targetIndex = currentIndex + direction;
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= operationRows.value.length) {
    return;
  }
  const rows = [...operationRows.value];
  const [movingRow] = rows.splice(currentIndex, 1);
  if (!movingRow) return;
  rows.splice(targetIndex, 0, movingRow);
  operationRows.value = rows;
  selectedOperationKey.value = movingRow.localKey;
  refreshOperationRequiredQty();
  recomputeSummary({ refreshOperations: false });
}

function deleteOperationNodesByKeys(keys: string[]) {
  const deleteKeys = new Set(keys);
  operationRows.value = operationRows.value.filter((item) => !deleteKeys.has(item.localKey));
  lockRows.value = lockRows.value.filter(
    (item) => item.lockType === 'FG' || !item.operationKey || !deleteKeys.has(item.operationKey),
  );
  checkedOperationKeys.value = checkedOperationKeys.value.filter((key) => !deleteKeys.has(key));
  if (deleteKeys.has(selectedOperationKey.value)) {
    selectedOperationKey.value = operationRows.value[0]?.localKey || '';
  }
  if (operationRows.value.length === 0) {
    formState.routeId = undefined;
    formState.routeCode = '';
    formState.routeName = '';
    formState.routeVersion = '';
    selectedOperationKey.value = '';
  }
  refreshOperationRequiredQty();
  recomputeSummary({ refreshOperations: false });
}

function clearSelectedRoute() {
  if (isReadonly.value || !routeMaintenanceMode.value) return;
  if (!formState.routeId && operationRows.value.length === 0) {
    message.info('当前没有已选择的工艺路线');
    return;
  }
  AntModal.confirm({
    title: '清除当前路线选择',
    content: '清除后会移除当前工位任务和对应利库挂接，仅影响本计划草稿，不删除基础工艺路线。',
    okText: '确认清除',
    cancelText: '取消',
    onOk: () => {
      clearRouteAndOperations();
      message.success('已清除当前路线选择');
    },
  });
}

async function deleteCurrentRoute() {
  if (isReadonly.value || !routeMaintenanceMode.value) return;
  if (!formState.routeId && operationRows.value.length === 0) {
    message.warning('当前计划没有可删除的工序节点');
    return;
  }
  const selectedKeys = checkedOperationKeys.value.filter((key) =>
    operationRows.value.some((item) => item.localKey === key),
  );
  if (selectedKeys.length > 0) {
    await new Promise<void>((resolve) => {
      AntModal.confirm({
        title: '删除选中工序',
        content: `将删除已勾选的 ${selectedKeys.length} 道工序，并移除这些工序对应的半成品利库挂接；不会删除基础工艺路线。确认删除？`,
        okText: '确认删除选中工序',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk() {
          deleteOperationNodesByKeys(selectedKeys);
          message.success('已删除选中工序');
          resolve();
        },
        onCancel: () => resolve(),
      });
    });
    return;
  }
  await new Promise<void>((resolve) => {
    AntModal.confirm({
      title: '清空当前计划路线',
      content: '当前没有勾选工序，将清空本计划中的全部工序节点和利库挂接；不会删除基础工艺路线。确认清空？',
      okText: '确认清空路线',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk() {
        clearRouteAndOperations();
        message.success('已清空当前计划路线');
        resolve();
      },
      onCancel: () => resolve(),
    });
  });
}

function applyPlanUnitFromMaterial(detail: { baseUnitCode?: string; baseUnitId?: number; baseUnitName?: string; baseUom?: string }) {
  // 订单欠交量单位按生产料号基础单位快照存储；原型不展示 ID/名称字段，仅在后端保存。
  const unitCode = detail.baseUnitCode || detail.baseUom || '';
  formState.orderDueUnitId = detail.baseUnitId;
  formState.orderDueUnitCode = unitCode;
  formState.orderDueUnitName = detail.baseUnitName || '';
  formState.targetUnitId = detail.baseUnitId;
  formState.targetUnitCode = unitCode;
  formState.targetUnitName = detail.baseUnitName || '';
  formState.targetUom = unitCode || formState.targetUom || 'PCS';
}

function setIfChanged<T extends keyof EditablePlanOrder>(field: T, value: EditablePlanOrder[T]) {
  if (formState[field] !== value) {
    formState[field] = value;
  }
}

function refreshOperationRequiredQty() {
  operationRows.value.forEach((item, index) => {
    const yieldRate = normalizeNumber(item.yieldRate) || 1;
    const opSeq = index + 1;
    const sort = index;
    const requiredQty =
      formState.netPlanQty > 0
        ? Number((formState.netPlanQty * yieldRate).toFixed(6))
        : 0;
    if (item.opSeq !== opSeq) item.opSeq = opSeq;
    if (item.sort !== sort) item.sort = sort;
    if (item.requiredQty !== requiredQty) item.requiredQty = requiredQty;
  });
}

function recomputeSummary(options: { refreshOperations?: boolean } = {}) {
  const targetQty = normalizeNumber(formState.targetQty);
  const fgDeductQty = normalizeNumber(formState.fgDeductQty);
  const netPlanQty = Math.max(targetQty - fgDeductQty, 0);
  setIfChanged('targetQty', targetQty);
  setIfChanged('fgDeductQty', fgDeductQty);
  setIfChanged('netPlanQty', netPlanQty);
  if (options.refreshOperations !== false) {
    refreshOperationRequiredQty();
  }
  setIfChanged('operationCount', operationRows.value.length);
  setIfChanged('totalLockQty', Number(activeLockTotal.value.toFixed(6)));
}

watch(
  () => [formState.targetQty, formState.fgDeductQty],
  () => recomputeSummary(),
);

watch(
  operationRows,
  () => {
    recomputeSummary({ refreshOperations: false });
    if (!operationRows.value.some((item) => item.localKey === selectedOperationKey.value)) {
      selectedOperationKey.value = operationRows.value[0]?.localKey || '';
    }
    checkedOperationKeys.value = checkedOperationKeys.value.filter((key) =>
      operationRows.value.some((item) => item.localKey === key),
    );
  },
  { deep: true },
);

watch(
  isFormulaStartPlan,
  (value) => {
    if (!value && !isProductionBatchGenerated.value) {
      clearBatchPreviewTimer();
      formState.batchNo = '';
      formState.batchRuleId = undefined;
      formState.batchRuleCode = '';
      batchPreviewWarning.value = '';
    } else {
      scheduleBatchNoPreview();
    }
  },
);

watch(
  () => [
    formState.planDate,
    formState.productionStartDate,
    formState.categoryCode,
    formState.prodType,
    formState.modelCode,
    formState.motherModelCode,
    formState.materialCode,
    firstOperation.value?.opCode,
    firstOperation.value?.opName,
    firstOperation.value?.workCenterId,
  ],
  () => {
    if (
      !formState.productionStartDate &&
      !manualBatchTouched.value &&
      !isProductionBatchGenerated.value &&
      !isBatchNoLocked.value
    ) {
      formState.batchNo = '';
      batchPreviewWarning.value = '请先填写计划开始日期';
    }
    scheduleBatchNoPreview();
  },
);

watch(
  lockRows,
  () => recomputeSummary({ refreshOperations: false }),
  { deep: true },
);

async function loadBaseOptions() {
  const [recipes, workCenters, equipments, locations, owners, staticOptions, users, productModels] = await Promise.all([
    getRecipeSelectOptions(),
    getWorkCenterSelectOptions(),
    getEquipmentSelectOptions(),
    getLocationSelectOptions(),
    getOwnerSelectOptions(),
    getPlanOrderStaticOptions(),
    getSimpleUserList(),
    getBomProductModelOptions(),
  ]);
  recipeOptions.value = toRecipeCodeOptions(recipes);
  workCenterOptions.value = workCenters.map((item) => ({
    ...item,
    label: normalizeNameLabel(item.label),
  }));
  equipmentOptions.value = (equipments || []).map((item) => ({
    ...item,
    label: equipmentOptionDisplayLabel(item),
  }));
  locationOptions.value = locations;
  ownerOptions.value = owners;
  prodTypeOptions.value = toCodeNameOptions(staticOptions.prodTypes);
  materialCategoryOptions.value = toCodeNameOptions(staticOptions.materialCategories);
  sizeSpecOptions.value = toSizeSpecOptions(staticOptions.sizeSpecs);
  productModelOptions.value = productModels || [];
  userNameMap.value = Object.fromEntries(
    users.map((item) => [String(item.id), item.nickname || item.username || String(item.id)]),
  );
  applyDefaultStaticOptions();
  await loadMaterialDependencies();
}

async function loadProductModelOptions(keyword?: string) {
  productModelOptions.value = await getBomProductModelOptions({
    keyword,
    productMaterialId: formState.materialId,
  });
}

function ensureProductModelOption(option?: Partial<MesHcBomApi.ProductModelOption> | null) {
  if (!option?.value) return;
  const existed = productModelOptions.value.some((item) => Number(item.value) === Number(option.value));
  if (existed) return;
  productModelOptions.value = [
    ...productModelOptions.value,
    {
      value: Number(option.value),
      label: option.label || option.code || '',
      code: option.code || option.label || '',
      modelName: option.modelName || '',
      recipeId: option.recipeId,
      recipeCode: option.recipeCode,
      recipeName: option.recipeName,
      sizeSpec: option.sizeSpec,
      sizeName: option.sizeName,
      status: option.status,
      productModelId: option.productModelId,
      productModelCode: option.productModelCode || option.code,
      productModelName: option.productModelName || option.modelName,
      bomType: option.bomType,
    },
  ];
}

function applyDefaultStaticOptions() {
  if (!formState.prodType && prodTypeOptions.value.length > 0) {
    formState.prodType = prodTypeOptions.value[0]!.code;
    handleProdTypeChange(formState.prodType);
  }
  if (!formState.categoryCode && materialCategoryOptions.value.length > 0) {
    formState.categoryCode = materialCategoryOptions.value[0]!.code;
    handleMaterialCategoryChange(formState.categoryCode);
  }
}

async function loadMaterialDependencies(materialId?: number) {
  const routeResp = await getRoutePage({
    pageNo: 1,
    pageSize: 200,
    applicableScope: 'GLOBAL',
  });
  routeOptions.value = routeResp.list.map((item) => ({
    value: Number(item.id),
    label: item.routeName || '',
    code: item.routeCode || '',
    status: item.status,
  }));
  if (!materialId) {
    bomOptions.value = [];
    return;
  }
  const bomList = await getBomSelectOptionsByMaterialId(materialId);
  bomOptions.value = bomList;
}

function getSaleOrderRemainQty(order: MesSaleOrderApi.MesSaleOrder) {
  const remainQty = Math.max(Number(order.quantity || 0) - Number(order.plannedQty || 0), 0);
  return Number(order.remainQty ?? remainQty);
}

function isPlanSelectableSaleOrder(order: MesSaleOrderApi.MesSaleOrder) {
  return ['APPROVED', 'PART_PLANNED'].includes(String(order.status || ''));
}

function hasPlanningData() {
  return Boolean(
    formState.materialId ||
      formState.routeId ||
      formState.bomId ||
      operationRows.value.length > 0 ||
      lockRows.value.length > 0,
  );
}

function salesOrderSnapshot(order: MesSaleOrderApi.MesSaleOrder) {
  return JSON.stringify({
    id: order.id,
    orderNo: order.orderNo,
    erpNo: order.erpNo,
    orderLineNo: order.orderLineNo,
    customerId: order.customerId,
    customerName: order.customerName,
    materialId: order.materialId,
    materialCode: order.materialCode,
    materialName: order.materialName,
    modelCode: order.modelCode,
    sizeSpec: order.sizeSpec,
    sizeName: order.sizeName,
    quantity: order.quantity,
    plannedQty: order.plannedQty,
    remainQty: order.remainQty,
    unitId: order.unitId,
    unitCode: order.unitCode,
    unitName: order.unitName,
    unit: order.unit,
    deliveryDate: normalizeLocalDate(order.deliveryDate),
    status: order.status,
    statusName: order.statusName,
  });
}

function parseSalesOrderSnapshot(value?: string) {
  if (!value) return undefined;
  try {
    return JSON.parse(value) as Partial<MesSaleOrderApi.MesSaleOrder>;
  } catch {
    return undefined;
  }
}

function confirmReplaceSaleOrder() {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      title: '确认更换销售订单',
      content: '更换销售订单将重置产品料号、路线工序与利库挂接，是否继续？',
      okText: '继续',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    });
  });
}

async function validateSaleOrderForPlan(order: MesSaleOrderApi.MesSaleOrder) {
  if (!isPlanSelectableSaleOrder(order)) {
    message.warning('只能挂接已审核、未完成且未关闭的销售订单');
    return false;
  }
  if (getSaleOrderRemainQty(order) <= 0) {
    message.warning('销售订单欠交量必须大于 0');
    return false;
  }
  if (!order.materialId || !order.materialCode) {
    message.warning('销售订单未维护产品料号，不能直接生成排产计划');
    return false;
  }
  return true;
}

async function applySaleOrder(order: MesSaleOrderApi.MesSaleOrder) {
  if (isProductIdentityLocked.value) {
    message.warning('请先撤回未开工计划，再修改产品型号和产品料号');
    return;
  }
  if (!(await validateSaleOrderForPlan(order))) return;

  const replacingOrder =
    formState.salesOrderId && Number(formState.salesOrderId) !== Number(order.id);
  if ((replacingOrder || !formState.salesOrderId) && hasPlanningData()) {
    const confirmed = await confirmReplaceSaleOrder();
    if (!confirmed) return;
  }

  const materialId = Number(order.materialId || 0);
  if (!materialId) {
    message.warning('销售订单未维护产品料号，不能直接生成排产计划');
    return;
  }
  const materialDetail = await getMaterialDetail(materialId);
  if (!materialDetail?.id || !materialDetail.mesSelectVisible) {
    message.warning('该物料未设置为 MES产品，请先到物料主数据维护');
    return;
  }

  const remainQty = getSaleOrderRemainQty(order);
  resetDependentSelections();
  formState.sourceType = 'SALES_ORDER';
  formState.planMode = 'MTO';
  formState.salesOrderId = order.id;
  formState.salesOrderNo = order.orderNo || '';
  formState.salesOrderErpNo = order.erpNo || '';
  formState.salesOrderLineNo = order.orderLineNo || '';
  formState.customerId = order.customerId;
  formState.customerName = order.customerName || '';
  formState.orderDueQty = remainQty;
  formState.orderDueUnitId = order.unitId;
  formState.orderDueUnitCode = order.unitCode || order.unit || '';
  formState.orderDueUnitName = order.unitName || '';
  const deliveryDate = normalizeLocalDate(order.deliveryDate);
  formState.salesOrderDeliveryDate = deliveryDate;
  formState.productionStartDate = formState.productionStartDate || dayjs().format('YYYY-MM-DD');
  formState.productionEndDate = formState.productionEndDate || deliveryDate;
  formState.targetQty = remainQty;
  formState.targetUnitId = order.unitId;
  formState.targetUnitCode = order.unitCode || order.unit || '';
  formState.targetUnitName = order.unitName || '';
  formState.targetUom = formState.targetUnitCode || formState.targetUom || 'PCS';
  if (!formState.modelId) {
    formState.modelCode = order.modelCode || order.productCode || '';
  }
  formState.sizeSpec = order.sizeSpec || order.productSpec || '';
  formState.sizeName = order.sizeName || codeNameOptionName(sizeSpecOptions.value, formState.sizeSpec);
  formState.salesOrderSnapshotJson = salesOrderSnapshot(order);
  await applyMaterialDefaults(materialId);
  if (!formState.modelId) {
    formState.modelCode = order.modelCode || formState.modelCode || order.productCode || '';
  }
  formState.sizeSpec = order.sizeSpec || formState.sizeSpec || order.productSpec || '';
  formState.sizeName = order.sizeName || codeNameOptionName(sizeSpecOptions.value, formState.sizeSpec);
  syncMotherFieldsFromProduct();
  recomputeSummary();
}

function resetFormState() {
  productModelValue.value = undefined;
  clearBatchPreviewTimer();
  manualBatchTouched.value = false;
  batchPreviewWarning.value = '';
  Object.assign(formState, {
    id: undefined,
    planNo: '',
    planDate: dayjs().format('YYYY-MM-DD'),
    planMode: 'MTO',
    planStatus: 'DRAFT',
    sourceType: 'MANUAL',
    salesOrderNo: '',
    salesOrderErpNo: '',
    salesOrderLineNo: '',
    customerName: '',
    orderDueQty: 0,
    orderDueUnitId: undefined,
    orderDueUnitCode: '',
    orderDueUnitName: '',
    salesOrderDeliveryDate: '',
    productionStartDate: defaultProductionStartDate(),
    productionEndDate: defaultProductionEndDate(),
    salesOrderSnapshotJson: '',
    materialId: undefined,
    materialCode: '',
    materialName: '',
    motherMaterialId: undefined,
    motherMaterialCode: '',
    motherMaterialName: '',
    categoryCode: '',
    categoryName: '',
    prodType: '',
    prodTypeName: '',
    modelId: undefined,
    modelName: '',
    modelCode: '',
    motherModelId: undefined,
    motherModelName: '',
    motherModelCode: '',
    recipeId: undefined,
    recipeCode: '',
    recipeName: '',
    bomId: undefined,
    bomVersion: '',
    routeId: undefined,
    routeCode: '',
    routeName: '',
    routeVersion: '',
    sizeSpec: '',
    sizeName: '',
    targetQty: 0,
    targetUnitId: undefined,
    targetUnitCode: '',
    targetUnitName: '',
    targetUom: 'PCS',
    fgDeductQty: 0,
    netPlanQty: 0,
    operationCount: 0,
    totalLockQty: 0,
    batchRuleId: undefined,
    batchRuleCode: '',
    batchNo: '',
    productionBatchNo: '',
    productionBatchContextJson: '',
    batchStatus: 'NOT_GEN',
    remark: '',
  });
  operationRows.value = [];
  lockRows.value = [];
  selectedOperationKey.value = '';
  activeLockTab.value = 'WIP';
  lockPanelCollapsed.value = false;
  routeMaintenanceMode.value = false;
  applyDefaultStaticOptions();
  recomputeSummary();
}

function resolveRouteOperationSeqNo(item: MesHcRouteApi.RouteOperation | undefined, index: number) {
  const seqNo = Number(item?.seqNo);
  return Number.isFinite(seqNo) && seqNo > 0 ? seqNo : index + 1;
}

function sortRouteOperationItemsBySeqNo(items: MesHcRouteApi.RouteOperation[] = []) {
  return items
    .map((item, index) => ({
      item,
      index,
      seqNo: resolveRouteOperationSeqNo(item, index),
    }))
    .sort((a, b) => a.seqNo - b.seqNo || a.index - b.index)
    .map(({ item }) => item);
}

function mapRouteOperations(routeDetail: MesHcRouteApi.Route) {
  const rows = sortRouteOperationItemsBySeqNo(routeDetail.routeOperations || []).map((item, index) => ({
    localKey: nextLocalKey('op'),
    opSeq: index + 1,
    opCode: item.operationCode,
    opName: item.operationName,
    routeOperationId: item.id,
    workCenterId: item.workCenterId,
    workCenterCode: item.workCenterCode,
    workCenterName: item.workCenterName,
    equipmentId: undefined,
    equipmentCode: '',
    equipmentName: '',
    yieldRate: (item as any).conversionRate || 1,
    requiredQty: formState.netPlanQty,
    lockedQty: 0,
    dispatchQty: 0,
    unitId: (item as any).outputUnitId,
    unitCode: (item as any).outputUnitCode || (item as any).outputUom || '',
    unitName: (item as any).outputUnitName || '',
    uom: (item as any).outputUnitCode || (item as any).outputUom || formState.targetUom,
    instructionText: item.remark || '',
    hasLock: false,
    operationStatus: formState.planStatus === 'RELEASED' ? 'RELEASED' : 'NOT_RELEASED',
    sort: index,
  }));
  rows.forEach((row) => applyDefaultEquipment(row));
  operationRows.value = rows;
  selectedOperationKey.value = rows[0]?.localKey || '';
  refreshOperationRequiredQty();
}

function getEquipmentOptionsByWorkCenter(workCenterId?: number) {
  const enabledOptions = equipmentOptions.value.filter((item) => item.status === undefined || item.status === 0);
  const padTypeOptions = enabledOptions.filter((item) => isEquipmentCompatibleWithPlanPadType(item));
  if (!workCenterId) {
    return padTypeOptions;
  }
  return padTypeOptions.filter((item) => Number(item.workCenterId) === Number(workCenterId));
}

function applyEquipmentChange(row: EditableOperation, equipmentId?: number) {
  const option = equipmentOptions.value.find((item) => Number(item.value) === Number(equipmentId));
  row.equipmentId = equipmentId;
  row.equipmentCode = option?.code || '';
  row.equipmentName = optionName(equipmentOptions.value, Number(equipmentId));
}

function refreshOperationEquipmentByPadType() {
  operationRows.value.forEach((row) => {
    if (!row.workCenterId) return;
    const options = getEquipmentOptionsByWorkCenter(Number(row.workCenterId));
    const current = equipmentOptions.value.find((item) => Number(item.value) === Number(row.equipmentId));
    const currentWorkCenterMatches = current && Number(current.workCenterId) === Number(row.workCenterId);
    if (current && currentWorkCenterMatches && isEquipmentCompatibleWithPlanPadType(current)) {
      applyEquipmentChange(row, Number(row.equipmentId));
      return;
    }
    const firstEquipment = options[0];
    if (firstEquipment) {
      applyEquipmentChange(row, Number(firstEquipment.value));
    } else {
      row.equipmentId = undefined;
      row.equipmentCode = '';
      row.equipmentName = '';
    }
  });
}

function applyDefaultEquipment(row: EditableOperation) {
  if (row.equipmentId) {
    applyEquipmentChange(row, Number(row.equipmentId));
    return;
  }
  const firstEquipment = getEquipmentOptionsByWorkCenter(Number(row.workCenterId))[0];
  if (firstEquipment) {
    applyEquipmentChange(row, Number(firstEquipment.value));
  } else {
    row.equipmentId = undefined;
    row.equipmentCode = '';
    row.equipmentName = '';
  }
}

function workCenterReadonlyLabel(row: EditableOperation) {
  return row.workCenterName || optionLabel(workCenterOptions.value, Number(row.workCenterId)) || '-';
}

async function applyRouteDetail(routeId?: number) {
  formState.routeId = routeId;
  if (!routeId) {
    formState.routeCode = '';
    formState.routeName = '';
    formState.routeVersion = '';
    operationRows.value = [];
    selectedOperationKey.value = '';
    return;
  }
  const detail = await getRouteDetail(Number(routeId));
  formState.routeCode = detail.routeCode || '';
  formState.routeName = detail.routeName || '';
  formState.routeVersion = detail.versionNo || '';
  mapRouteOperations(detail);
}

function applyRecipeOptionDetail(recipeId?: number) {
  const option = recipeOptions.value.find((item) => Number(item.value) === Number(recipeId));
  formState.recipeId = recipeId;
  formState.recipeCode = option?.code || '';
  formState.recipeName = option?.name || '';
}

async function applyBomDetail(bomId?: number) {
  formState.bomId = bomId;
  if (!bomId) {
    formState.bomVersion = '';
    return;
  }
  const detail = await getBom(Number(bomId));
  formState.bomVersion = detail.versionNo || '';
}

function resetDependentSelections() {
  formState.routeId = undefined;
  formState.routeCode = '';
  formState.routeName = '';
  formState.routeVersion = '';
  formState.recipeId = undefined;
  formState.recipeCode = '';
  formState.recipeName = '';
  formState.bomId = undefined;
  formState.bomVersion = '';
  operationRows.value = [];
  lockRows.value = [];
  selectedOperationKey.value = '';
}

async function applyMaterialDefaults(materialId?: number) {
  if (!materialId) {
    productModelValue.value = undefined;
    formState.materialCode = '';
    formState.materialName = '';
    formState.modelId = undefined;
    formState.modelName = '';
    formState.modelCode = '';
    formState.categoryCode = '';
    formState.categoryName = '';
    formState.sizeSpec = '';
    formState.sizeName = '';
    formState.orderDueUnitId = undefined;
    formState.orderDueUnitCode = '';
    formState.orderDueUnitName = '';
    formState.targetUnitId = undefined;
    formState.targetUnitCode = '';
    formState.targetUnitName = '';
    formState.targetUom = 'PCS';
    resetDependentSelections();
    syncMotherFieldsFromProduct();
    return;
  }
  const detail = await getMaterialDetail(materialId);
  if (detail.defaultBomId) {
    const bom = await getBom(Number(detail.defaultBomId));
    await applyProductBomDefaults(bom);
    return;
  }
  formState.materialId = detail.id;
  formState.materialCode = detail.materialCode || '';
  formState.materialName = detail.materialName || '';
  formState.categoryId = detail.materialCategoryId;
  // 物料主数据只作为默认值来源；若用户已按原型下拉选过，则不覆盖手工选择。
  formState.categoryName = formState.categoryName || detail.materialCategoryName || '';
  // 产品料号选择后，物料规格型号编码反写为产品型号；尺寸规格保持为页面下拉手工选择。
  productModelValue.value = detail.productModelId;
  formState.modelId = detail.productModelId;
  formState.modelName = detail.productModelName || '';
  formState.modelCode = detail.modelCode || detail.specModel || '';
  ensureProductModelOption(
    detail.productModelId
      ? {
          value: Number(detail.productModelId),
          label: detail.modelCode || detail.specModel || '',
          code: detail.modelCode || detail.specModel || '',
          modelName: detail.productModelName || '',
          recipeId: detail.defaultRecipeId,
          recipeCode: detail.defaultRecipeCode,
          recipeName: detail.defaultRecipeName,
        }
      : undefined,
  );
  applyPlanUnitFromMaterial(detail);
  await loadMaterialDependencies(materialId);

  if (!formState.routeId && detail.defaultRouteId) {
    formState.routeId = detail.defaultRouteId;
  }
  if (!formState.recipeId && detail.defaultRecipeId) {
    formState.recipeId = detail.defaultRecipeId;
  }
  if (!formState.bomId && detail.defaultBomId) {
    formState.bomId = detail.defaultBomId;
  }

  if (formState.routeId) {
    await applyRouteDetail(formState.routeId);
  }
  applyRecipeOptionDetail(formState.recipeId);
  await applyBomDetail(formState.bomId);
  syncMotherFieldsFromProduct();
}

async function applyProductBomDefaults(bom: MesHcBomApi.Bom) {
  if (!bom?.id) return;
  resetDependentSelections();
  formState.bomId = Number(bom.id);
  formState.bomVersion = bom.versionNo || '';
  formState.materialId = bom.productMaterialId;
  formState.materialCode = bom.productMaterialCode || '';
  formState.materialName = bom.productMaterialName || '';
  productModelValue.value = bom.productModelId ? Number(bom.productModelId) : undefined;
  formState.modelId = bom.productModelId;
  formState.modelCode = bom.productModelCode || '';
  formState.modelName = bom.productModelName || '';
  ensureProductModelOption(buildProductModelOptionFromBom(bom));
  if (bom.productSpec) {
    formState.sizeSpec = bom.productSpec;
    formState.sizeName = codeNameOptionName(sizeSpecOptions.value, bom.productSpec) || bom.productSpec;
  }
  applyProdTypeByModel(bom.productModelCode, bom.bomType);

  if (bom.productMaterialId) {
    const material = await getMaterialDetail(Number(bom.productMaterialId));
    applyPlanUnitFromMaterial(material);
    formState.categoryId = material.materialCategoryId;
    formState.categoryName = material.materialCategoryName || formState.categoryName;
    if (!bom.routeId && material.defaultRouteId) {
      formState.routeId = material.defaultRouteId;
    }
    if (!formState.recipeId && material.defaultRecipeId) {
      formState.recipeId = material.defaultRecipeId;
      formState.recipeCode = material.defaultRecipeCode || '';
      formState.recipeName = material.defaultRecipeName || '';
    }
  }
  await loadMaterialDependencies(bom.productMaterialId);
  if (bom.routeId) {
    formState.routeId = bom.routeId;
  }
  if (formState.routeId) {
    await applyRouteDetail(formState.routeId);
  }
  if (bom.productModelId) {
    const modelDetail = await getProductModelDetail(Number(bom.productModelId));
    formState.modelName = modelDetail.modelName || formState.modelName;
    formState.modelCode = modelDetail.modelCode || formState.modelCode;
    if (modelDetail.recipeId) {
      formState.recipeId = modelDetail.recipeId;
      formState.recipeCode = modelDetail.recipeCode || '';
      formState.recipeName = modelDetail.recipeName || '';
    }
    if (!formState.sizeSpec && modelDetail.sizeSpec) {
      formState.sizeSpec = modelDetail.sizeSpec;
      formState.sizeName = modelDetail.sizeName || codeNameOptionName(sizeSpecOptions.value, modelDetail.sizeSpec);
    }
    formState.categoryId = modelDetail.categoryId || formState.categoryId;
    formState.categoryCode = modelDetail.categoryCode || formState.categoryCode;
    formState.categoryName = modelDetail.categoryName || formState.categoryName;
    applyProdTypeByModel(formState.modelCode, bom.bomType, modelDetail.prodType, modelDetail.prodTypeName);
  }
  refreshOperationEquipmentByPadType();
  applyRecipeOptionDetail(formState.recipeId);
  syncMotherFieldsFromProduct();
}

function fillSnapshotFields() {
  operationRows.value = operationRows.value.map((item) => ({
    ...item,
    workCenterCode:
      item.workCenterCode || optionCode(workCenterOptions.value, Number(item.workCenterId)),
    workCenterName:
      item.workCenterName || optionLabel(workCenterOptions.value, Number(item.workCenterId)),
    equipmentCode:
      item.equipmentCode || optionCode(equipmentOptions.value, Number(item.equipmentId)),
    equipmentName:
      item.equipmentName || optionName(equipmentOptions.value, Number(item.equipmentId)),
    lockedQty: operationLockedQty(item),
    unitCode: item.unitCode || item.uom || formState.targetUom,
    uom: item.uom || item.unitCode || formState.targetUom,
  }));
  lockRows.value = lockRows.value.map((item) => ({
    ...item,
    targetPlanNo: item.targetPlanNo || formState.planNo,
    targetOpCode:
      item.targetOpCode ||
      operationRows.value.find((operation) => operation.localKey === item.operationKey)?.opCode,
    targetOpName:
      item.targetOpName ||
      operationRows.value.find((operation) => operation.localKey === item.operationKey)?.opName,
    sourceBatchNo: item.sourceBatchNo || item.batchNo || item.lotNo,
    consumedQty: item.consumedQty ?? 0,
    releasedQty: item.releasedQty ?? 0,
    remainingQty: getLockRemainingQty(item),
    locationName:
      item.locationName ||
      optionLabel(locationOptions.value, Number(item.locationId)) ||
      item.locationCode,
    ownerName:
      item.ownerName || optionLabel(ownerOptions.value, Number(item.ownerId)) || item.ownerCode,
    uom: item.uom || formState.targetUom,
  }));
}

function buildPayload(planStatus?: string): MesHcPlanOrderApi.PlanOrder {
  syncMotherFieldsFromProduct();
  fillSnapshotFields();
  recomputeSummary();
  const validInventoryLocks = lockRows.value
    .filter(isInventoryLockReadyForSave)
    .map(normalizeInventoryLockForSave);
  const normalizedPlanDate = formState.productionStartDate || undefined;
  return {
    id: formState.id,
    planNo: formState.planNo,
    planDate: normalizedPlanDate,
    planMode: formState.planMode,
    planStatus: planStatus || formState.planStatus,
    sourceType: formState.sourceType,
    salesOrderId: formState.salesOrderId,
    salesOrderNo: formState.salesOrderId ? formState.salesOrderNo : undefined,
    salesOrderErpNo: formState.salesOrderId ? formState.salesOrderErpNo : undefined,
    salesOrderLineNo: formState.salesOrderId ? formState.salesOrderLineNo : undefined,
    customerId: formState.customerId,
    customerName: formState.customerName,
    orderDueQty: formState.orderDueQty,
    orderDueUnitId: formState.orderDueUnitId,
    orderDueUnitCode: formState.orderDueUnitCode,
    orderDueUnitName: formState.orderDueUnitName,
    salesOrderDeliveryDate: formState.salesOrderDeliveryDate,
    productionStartDate: normalizedPlanDate,
    productionEndDate: formState.productionEndDate,
    materialId: formState.materialId,
    materialCode: formState.materialCode,
    materialName: formState.materialName,
    motherMaterialId: formState.motherMaterialId,
    motherMaterialCode: formState.motherMaterialCode,
    motherMaterialName: formState.motherMaterialName,
    categoryId: formState.categoryId,
    categoryCode: formState.categoryCode,
    categoryName: formState.categoryName,
    prodType: formState.prodType,
    prodTypeName: formState.prodTypeName,
    modelId: formState.modelId,
    modelName: formState.modelName,
    modelCode: formState.modelCode,
    motherModelId: formState.motherModelId,
    motherModelName: formState.motherModelName,
    motherModelCode: formState.motherModelCode,
    recipeId: formState.recipeId,
    recipeCode: formState.recipeCode,
    recipeName: formState.recipeName,
    bomId: formState.bomId,
    bomVersion: formState.bomVersion,
    routeId: formState.routeId,
    routeCode: formState.routeCode,
    routeName: formState.routeName,
    routeVersion: formState.routeVersion,
    sizeSpec: formState.sizeSpec,
    sizeName: formState.sizeName,
    targetQty: formState.targetQty,
    targetUnitId: formState.targetUnitId,
    targetUnitCode: formState.targetUnitCode,
    targetUnitName: formState.targetUnitName,
    targetUom: formState.targetUom,
    fgDeductQty: formState.fgDeductQty,
    netPlanQty: formState.netPlanQty,
    operationCount: formState.operationCount,
    totalLockQty: formState.totalLockQty,
    batchRuleId: formState.batchRuleId,
    batchRuleCode: formState.batchRuleCode,
    batchNo:
      isFormulaStartPlan.value && manualBatchTouched.value
        ? normalizeManualBatchNo(formState.batchNo) || undefined
        : undefined,
    batchStatus: formState.batchStatus || 'NOT_GEN',
    remark: formState.remark,
    salesOrderSnapshotJson: formState.salesOrderSnapshotJson || undefined,
    operations: operationRows.value.map(({ localKey, ...item }) => ({ ...item })),
    inventoryLocks: validInventoryLocks.map(({ localKey, operationKey, ...item }) => ({ ...item })),
  };
}

function validateBeforeSave(targetStatus?: string) {
  if (!formState.productionStartDate) {
    message.warning('请选择计划开始日期');
    return false;
  }
  if (
    formState.productionStartDate &&
    formState.productionEndDate &&
    dayjs(formState.productionStartDate).isAfter(dayjs(formState.productionEndDate))
  ) {
    message.warning('生产开始日期不能晚于生产结束日期');
    return false;
  }
  if (operationRows.value.length === 0) {
    message.warning('请至少维护一条工位任务');
    return false;
  }
  if (!formState.modelId && !formState.modelCode) {
    message.warning('请选择产品型号');
    return false;
  }
  const manualBatchNo = normalizeManualBatchNo(formState.batchNo);
  if (!isFormulaStartPlan.value) {
    if (!isProductionBatchGenerated.value) {
      formState.batchNo = '';
    }
    return true;
  }
  if (manualBatchNo && !/^[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z0-9]+$/.test(manualBatchNo)) {
    message.warning('产品批号格式不正确，例如 W26F055A');
    return false;
  }
  formState.batchNo = manualBatchNo;
  return true;
}

function getRequestErrorMessage(error: any) {
  const responseData = error?.response?.data ?? error?.data ?? {};
  return String(
    responseData?.error ??
      responseData?.message ??
      responseData?.msg ??
      error?.message ??
      '',
  );
}

function showBatchNoConflictModal(error: any) {
  const errorMessage = getRequestErrorMessage(error);
  if (!errorMessage.includes('产品批号已被其他生产计划使用')) {
    return false;
  }
  const conflictBatchNo =
    errorMessage.split('：')[1]?.trim() ||
    errorMessage.split(':')[1]?.trim() ||
    normalizeManualBatchNo(formState.batchNo);
  batchPreviewWarning.value = conflictBatchNo
    ? `产品批号 ${conflictBatchNo} 已被其他生产计划使用，请刷新预览或人工修改`
    : '当前产品批号已被其他生产计划使用，请刷新预览或人工修改';
  AntModal.error({
    title: '产品批号重复占用',
    content: conflictBatchNo
      ? `产品批号 ${conflictBatchNo} 已被其他生产计划使用，当前计划不能保存。请点击“刷新”重新获取可用批号，或人工修改产品批号后再保存。`
      : '当前产品批号已被其他生产计划使用，当前计划不能保存。请点击“刷新”重新获取可用批号，或人工修改产品批号后再保存。',
    okText: '知道了',
  });
  return true;
}

function isWipInventoryLock(item: EditableInventoryLock) {
  return item.lockType !== 'FG';
}

function isInventoryLockReadyForSave(item: EditableInventoryLock) {
  if (item.stockId == null || item.lockQty == null) {
    return false;
  }
  if (!isWipInventoryLock(item)) {
    return (
      item.materialId != null &&
      !!String(item.materialCode || '').trim() &&
      !!String(item.materialName || '').trim()
    );
  }
  return Boolean(item.operationKey || item.planOperationId || item.targetOpCode || item.targetOpName);
}

function normalizeInventoryLockForSave(item: EditableInventoryLock): EditableInventoryLock {
  return {
    ...item,
    materialCode: String(item.materialCode || ''),
    materialName: String(item.materialName || ''),
  };
}

async function savePlan(targetStatus?: string) {
  if (isReadonly.value) return;
  if (!validateBeforeSave(targetStatus)) return;
  const currentToken = openToken.value;
  if (targetStatus === 'RELEASED') {
    const confirmed = await new Promise<boolean>((resolve) => {
      AntModal.confirm({
        title: '确认下发生产计划',
        content: '确认下发后，该排产计划将进入下发状态；配料起始计划将锁定母批批号，开工时沿用。是否继续？',
        okText: '确认下发',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      });
    });
    if (!confirmed) return;
    if (currentToken !== openToken.value) return;
  }
  saving.value = true;
  try {
    const payload = buildPayload(targetStatus);
    if (formState.id) {
      await updatePlanOrder(payload);
      if (currentToken !== openToken.value) return;
      message.success(targetStatus === 'DRAFT' ? '草稿已保存' : '生产计划已更新');
      await loadDetail(Number(formState.id), currentToken);
    } else {
      const id = await createPlanOrder(payload);
      if (currentToken !== openToken.value) return;
      formState.id = id;
      message.success(targetStatus === 'DRAFT' ? '草稿已保存' : '生产计划已创建');
      await loadDetail(id, currentToken);
    }
    if (currentToken !== openToken.value) return;
    emit('success');
  } catch (error: any) {
    if (currentToken === openToken.value && showBatchNoConflictModal(error)) {
      return;
    }
    throw error;
  } finally {
    if (currentToken === openToken.value) {
      saving.value = false;
    }
  }
}

async function loadDetail(id: number, token?: number) {
  loading.value = true;
  try {
    const detail = await getPlanOrderDetail(id);
    if (token !== undefined && token !== openToken.value) return;
    Object.assign(formState, {
      ...detail,
      planDate: normalizeLocalDate(detail.planDate) || '',
      orderDueQty: normalizeNumber(detail.orderDueQty),
      targetQty: normalizeNumber(detail.targetQty),
      fgDeductQty: normalizeNumber(detail.fgDeductQty),
      netPlanQty: normalizeNumber(detail.netPlanQty),
      operationCount: Number(detail.operationCount || 0),
      totalLockQty: normalizeNumber(detail.totalLockQty),
      batchRuleId: detail.batchRuleId || detail.productionBatchRuleId,
      batchRuleCode: detail.batchRuleCode || detail.productionBatchRuleCode || '',
      batchNo: detail.batchNo || detail.productionBatchNo || '',
      productionBatchNo: detail.productionBatchNo || '',
      productionBatchContextJson: detail.productionBatchContextJson || '',
      batchStatus: detail.batchStatus || 'NOT_GEN',
    });
    // 保存过的人工选择跨回载保留；没有选定值时仍走自动预览，可重新输入撤回旧号。
    manualBatchTouched.value = detail.planStatus === 'DRAFT' && Boolean(formState.batchNo);
    batchPreviewWarning.value = '';
    formState.orderDueUnitCode = detail.orderDueUnitCode || detail.targetUnitCode || detail.targetUom || '';
    formState.orderDueUnitName = detail.orderDueUnitName || detail.targetUnitName || '';
    formState.targetUnitCode = detail.targetUnitCode || detail.targetUom || formState.orderDueUnitCode || '';
    formState.targetUnitName = detail.targetUnitName || formState.orderDueUnitName || '';
    formState.targetUom = detail.targetUom || formState.targetUnitCode || formState.orderDueUnitCode || 'PCS';
    const snapshot = parseSalesOrderSnapshot(detail.salesOrderSnapshotJson);
    formState.salesOrderErpNo = detail.salesOrderErpNo || snapshot?.erpNo || '';
    formState.salesOrderDeliveryDate =
      normalizeLocalDate(detail.salesOrderDeliveryDate || snapshot?.deliveryDate) || '';
    formState.productionStartDate = normalizeLocalDate(detail.productionStartDate) || '';
    formState.productionEndDate = normalizeLocalDate(detail.productionEndDate) || '';
    productModelValue.value = detail.modelId ? Number(detail.modelId) : undefined;
    ensureProductModelOption(
      detail.modelId
        ? {
            value: Number(detail.modelId),
            label: detail.modelCode || '',
            code: detail.modelCode || '',
            modelName: detail.modelName || '',
            recipeId: detail.recipeId,
            recipeCode: detail.recipeCode,
            recipeName: detail.recipeName,
            sizeSpec: detail.sizeSpec,
            sizeName: detail.sizeName,
        }
        : undefined,
    );
    syncMotherFieldsFromProduct();
    lockPanelCollapsed.value = ['RELEASED', 'CLOSED', 'CANCELLED'].includes(
      String(detail.planStatus || '').toUpperCase(),
    );
    await loadMaterialDependencies(detail.materialId);
    if (token !== undefined && token !== openToken.value) return;
    operationRows.value = (detail.operations || []).map((item) => ({
      ...item,
      unitCode: item.unitCode || item.uom || '',
      uom: item.uom || item.unitCode || formState.targetUom,
      localKey: nextLocalKey('op'),
    }));
    lockRows.value = (detail.inventoryLocks || []).map((item) => {
      const operation = operationRows.value.find(
        (op) =>
          Number(op.id) === Number(item.planOperationId) ||
          (!!item.targetOpCode && op.opCode === item.targetOpCode) ||
          (!!item.targetOpName && op.opName === item.targetOpName),
      );
      return {
        ...item,
        localKey: nextLocalKey('lock'),
        operationKey: operation?.localKey,
      };
    });
    selectedOperationKey.value = operationRows.value[0]?.localKey || '';
    recomputeSummary();
    scheduleBatchNoPreview();
  } finally {
    if (token === undefined || token === openToken.value) {
      loading.value = false;
    }
  }
}

async function handleRouteChange(routeId?: number) {
  if (isReadonly.value) return;
  if (!routeMaintenanceMode.value) {
    return;
  }
  lockRows.value = [];
  await applyRouteDetail(routeId);
}

function removeLock(localKey: string) {
  if (isReadonly.value) return;
  lockRows.value = lockRows.value.filter((item) => item.localKey !== localKey);
  if (selectedInventoryLockKey.value === localKey) {
    selectedInventoryLockKey.value = '';
  }
}

function setSelectedOperation(localKey: string) {
  selectedOperationKey.value = localKey;
  activeLockTab.value = 'WIP';
  selectedInventoryLockKey.value = '';
}

function selectInventoryLock(row: EditableInventoryLock) {
  selectedInventoryLockKey.value = row.localKey;
}

function handleProdTypeChange(code?: string) {
  formState.prodTypeName = codeNameOptionName(prodTypeOptions.value, code);
}

function handleMaterialCategoryChange(code?: string) {
  formState.categoryName = codeNameOptionName(materialCategoryOptions.value, code);
  refreshOperationEquipmentByPadType();
}

function handleSizeSpecChange(code?: string) {
  formState.sizeName = codeNameOptionName(sizeSpecOptions.value, code);
}

async function handleProductModelChange(value?: number | string) {
  if (isReadonly.value || isProductIdentityLocked.value) {
    message.warning('请先撤回未开工计划，再修改产品型号');
    return;
  }
  const numericValue = value ? Number(value) : undefined;
  const productModelId =
    numericValue && Number.isFinite(numericValue) && numericValue > 0 ? numericValue : undefined;
  productModelValue.value = productModelId;
  if (!productModelId) {
    formState.modelId = undefined;
    formState.modelName = '';
    formState.modelCode = '';
    formState.materialId = undefined;
    formState.materialCode = '';
    formState.materialName = '';
    resetDependentSelections();
    syncMotherFieldsFromProduct();
    return;
  }
  const currentBomModelId = formState.modelId;
  if (formState.bomId && currentBomModelId && Number(currentBomModelId) !== productModelId) {
    formState.materialId = undefined;
    formState.materialCode = '';
    formState.materialName = '';
    resetDependentSelections();
  }
  const detail = await getProductModelDetail(productModelId);
  const selectedOption = productModelOptions.value.find((item) => Number(item.value) === productModelId);
  ensureProductModelOption({
    value: productModelId,
    label: detail.modelCode || '',
    code: detail.modelCode || '',
    modelName: detail.modelName || '',
    recipeId: detail.recipeId,
    recipeCode: detail.recipeCode,
    recipeName: detail.recipeName,
    sizeSpec: detail.sizeSpec,
    sizeName: detail.sizeName,
    status: detail.status,
    bomType: selectedOption?.bomType,
  });
  formState.modelId = productModelId;
  formState.modelName = detail.modelName || '';
  formState.modelCode = detail.modelCode || '';
  formState.categoryId = detail.categoryId || formState.categoryId;
  formState.categoryCode = detail.categoryCode || formState.categoryCode;
  formState.categoryName = detail.categoryName || formState.categoryName;
  if (detail.recipeId) {
    formState.recipeId = detail.recipeId;
    formState.recipeCode = detail.recipeCode || '';
    formState.recipeName = detail.recipeName || '';
  }
  if (detail.sizeSpec) {
    formState.sizeSpec = detail.sizeSpec;
    formState.sizeName = detail.sizeName || codeNameOptionName(sizeSpecOptions.value, detail.sizeSpec);
  }
  applyProdTypeByModel(detail.modelCode, selectedOption?.bomType, detail.prodType, detail.prodTypeName);
  refreshOperationEquipmentByPadType();
  syncMotherFieldsFromProduct();
}

function handleProductionMaterialInput(value: string) {
  if (isReadonly.value || isProductIdentityLocked.value) {
    message.warning('请先撤回未开工计划，再修改产品料号');
    return;
  }
  formState.materialId = undefined;
  formState.materialCode = String(value || '');
  formState.materialName = '';
  resetDependentSelections();
  syncMotherFieldsFromProduct();
}

function openProductionMaterialPicker() {
  if (isReadonly.value || isProductIdentityLocked.value) {
    message.warning('请先撤回未开工计划，再修改产品料号');
    return;
  }
  productBomPickerOpen.value = true;
}

async function handleProductBomPick(option: PickerOption) {
  if (isReadonly.value || isProductIdentityLocked.value) {
    message.warning('请先撤回未开工计划，再修改产品料号');
    return;
  }
  productBomPickerOpen.value = false;
  await applyProductBomDefaults(option.raw as MesHcBomApi.Bom);
}

function handleSaleOrderInput(value: string) {
  if (isReadonly.value) return;
  // 手工输入仅作为搜索/临时文本，不形成正式销售订单挂接。
  formState.sourceType = 'MANUAL';
  formState.salesOrderId = undefined;
  formState.salesOrderNo = String(value || '');
  formState.salesOrderErpNo = '';
  formState.salesOrderLineNo = '';
  formState.customerId = undefined;
  formState.customerName = '';
  formState.orderDueQty = 0;
  formState.salesOrderDeliveryDate = '';
  formState.salesOrderSnapshotJson = '';
}

function openSaleOrderPicker() {
  if (isReadonly.value) return;
  saleOrderPickerOpen.value = true;
}

async function handleSaleOrderPick(option: PickerOption) {
  if (isReadonly.value) return;
  saleOrderPickerOpen.value = false;
  await applySaleOrder(option.raw as MesSaleOrderApi.MesSaleOrder);
}

function resetInventoryPickQty() {
  Object.keys(inventoryPickQty).forEach((key) => delete inventoryPickQty[key]);
}

function resetInventorySelection() {
  resetInventoryPickQty();
  Object.keys(inventoryRowCache).forEach((key) => delete inventoryRowCache[key]);
}

function getStockRowKey(row: MesHcInvStockApi.Stock) {
  return String(row.id || row.batchNo || row.materialCode || '');
}

function getRemainMonths(expiryDate?: string) {
  if (!expiryDate) return undefined;
  const end = dayjs(expiryDate);
  return end.isValid() ? end.diff(dayjs(), 'month') : undefined;
}

function getExpiryStatus(row: MesHcInvStockApi.Stock) {
  const months = getRemainMonths(row.expiryDate);
  if (months === undefined || months > 3) return '正常';
  return months < 0 ? '已过期' : '临期预警';
}

function getWipCandidateShareableQty(row: MesHcInvStockApi.Stock) {
  const candidate = row as MesHcPlanOrderApi.WipCandidate;
  return normalizeNumber(candidate.shareableQty ?? row.availableQty);
}

function getWipCandidateTxnSummary(row: MesHcInvStockApi.Stock) {
  const candidate = row as MesHcPlanOrderApi.WipCandidate;
  return candidate.txnSummary || row.businessRemark || '-';
}

function getLockRemainingQty(row: MesHcPlanOrderApi.InventoryLock) {
  return Math.max(
    normalizeNumber(row.lockQty) -
      normalizeNumber(row.consumedQty) -
      normalizeNumber(row.releasedQty),
    0,
  );
}

function getInventoryLockStatusText(row?: MesHcPlanOrderApi.InventoryLock) {
  const status = String(row?.lockStatus || 'ACTIVE').toUpperCase();
  const remainingQty = row ? getLockRemainingQty(row) : 0;
  if (status === 'CONSUMED') return '已消耗';
  if (status === 'PENDING') return '待下发占用';
  if (status === 'RELEASED') return '已释放';
  if (status === 'CANCELLED' || status === 'CANCELED') return '已取消';
  if (remainingQty <= 0 && normalizeNumber(row?.releasedQty) > 0) return '已释放';
  if (remainingQty <= 0 && normalizeNumber(row?.consumedQty) > 0) return '已消耗';
  return '有效';
}

function getInventoryLockStatusClass(row?: MesHcPlanOrderApi.InventoryLock) {
  const status = String(row?.lockStatus || 'ACTIVE').toUpperCase();
  if (status === 'CONSUMED') return 'is-consumed';
  if (status === 'RELEASED') return 'is-released';
  if (status === 'CANCELLED' || status === 'CANCELED') return 'is-cancelled';
  return 'is-active';
}

function getInventoryLockRemark(row?: MesHcPlanOrderApi.InventoryLock) {
  return row?.releaseReason || row?.remark || '-';
}

function syncLockRemainingQty(row: EditableInventoryLock) {
  row.remainingQty = getLockRemainingQty(row);
}

function canReleaseInventoryLock(row: EditableInventoryLock) {
  const lockStatus = String(row.lockStatus || 'ACTIVE').toUpperCase();
  return Boolean(
    isReleasedPlan.value &&
      row.id &&
      row.lockType !== 'FG' &&
      lockStatus === 'ACTIVE' &&
      getLockRemainingQty(row) > 0,
  );
}

function validateReleaseQty() {
  const maxQty = releaseTargetLock.value ? getLockRemainingQty(releaseTargetLock.value) : 0;
  releaseForm.releaseQty = Math.min(Math.max(normalizeNumber(releaseForm.releaseQty), 0), maxQty);
}

function openReleaseInventoryLock(row: EditableInventoryLock) {
  if (!canReleaseInventoryLock(row)) {
    message.warning('当前挂接记录没有可释放的剩余中间品');
    return;
  }
  releaseTargetLock.value = row;
  releaseForm.releaseQty = getLockRemainingQty(row);
  releaseForm.releaseReason = '';
  releaseModalOpen.value = true;
}

async function confirmReleaseInventoryLock() {
  const row = releaseTargetLock.value;
  if (!row?.id) return;
  validateReleaseQty();
  if (releaseForm.releaseQty <= 0) {
    message.warning('释放数量必须大于0');
    return;
  }
  const releaseReason = releaseForm.releaseReason.trim();
  if (!releaseReason) {
    message.warning('请填写释放原因');
    return;
  }
  releaseSubmitting.value = true;
  try {
    await releasePlanInventoryLock({
      lockId: Number(row.id),
      releaseQty: releaseForm.releaseQty,
      releaseReason,
    });
    message.success('已释放计划挂接剩余中间品');
    releaseModalOpen.value = false;
    releaseTargetLock.value = undefined;
    if (formState.id) {
      await loadDetail(Number(formState.id), openToken.value);
    }
  } finally {
    releaseSubmitting.value = false;
  }
}

function formatOperationLabel(operation?: EditableOperation) {
  if (!operation) return '-';
  return operation.opName || operation.opCode || '-';
}

function defaultSourceOpSeqForSelectedOperation() {
  const currentSeq = Number(selectedOperation.value?.opSeq);
  if (!Number.isFinite(currentSeq)) return undefined;
  const previous = operationRows.value
    .filter((item) => Number.isFinite(Number(item.opSeq)) && Number(item.opSeq) < currentSeq)
    .sort((left, right) => Number(right.opSeq) - Number(left.opSeq))[0];
  return previous?.opSeq;
}

function defaultWipModelQuery() {
  const modelNo = String(formState.modelCode || formState.modelName || '').trim();
  return modelNo.length > 4 ? modelNo.slice(0, 4) : modelNo;
}

async function loadInventoryRows(options?: { resetPage?: boolean; resetSelection?: boolean }) {
  if (options?.resetPage) {
    inventoryPagination.pageNo = 1;
  }
  if (options?.resetSelection) {
    resetInventorySelection();
  }
  inventoryLoading.value = true;
  try {
    const result = inventoryModalType.value === 'WIP'
      ? await getPlanWipCandidatePage({
          pageNo: inventoryPagination.pageNo,
          pageSize: inventoryPagination.pageSize,
          targetPlanId: formState.id,
          targetPlanNo: formState.planNo || undefined,
          targetOperationId: selectedOperation.value?.id,
          targetOpSeq: selectedOperation.value?.opSeq,
          targetOpCode: selectedOperation.value?.opCode,
          sourceOpSeq: inventoryQuery.opSeq,
          sourcePlanNo: inventoryQuery.sourcePlanNo || undefined,
          sourceBatchNo: inventoryQuery.sourceBatchNo || undefined,
          sourceParentBatchNo: inventoryQuery.sourceParentBatchNo || undefined,
          modelNo: inventoryQuery.modelNo || undefined,
        })
      : await getInvStockPage({
          pageNo: 1,
          pageSize: 100,
          stockType: inventoryModalType.value,
          recipeCode: inventoryQuery.recipeCode || undefined,
          specSize: inventoryQuery.specSize || undefined,
          keyword: inventoryQuery.keyword || undefined,
        });
    inventoryRows.value = result.list || [];
    inventoryPagination.total = Number(result.total || inventoryRows.value.length || 0);
    inventoryRows.value.forEach((row) => {
      inventoryRowCache[getStockRowKey(row)] = row;
      const existed = lockRows.value.find(
        (item) =>
          Number(item.stockId) === Number(row.id) &&
          item.lockType === inventoryModalType.value &&
          (inventoryModalType.value === 'FG' || item.operationKey === selectedOperationKey.value),
      );
      const rowKey = getStockRowKey(row);
      if (existed?.lockQty && inventoryPickQty[rowKey] == null) {
        inventoryPickQty[rowKey] = Number(existed.lockQty);
      }
    });
  } finally {
    inventoryLoading.value = false;
  }
}

async function refreshInventoryRows() {
  await loadInventoryRows({ resetPage: true, resetSelection: true });
}

async function handleInventoryPageChange(pageNo: number, pageSize: number) {
  inventoryPagination.pageNo = pageSize !== inventoryPagination.pageSize ? 1 : pageNo;
  inventoryPagination.pageSize = pageSize;
  await loadInventoryRows();
}

function showInventoryTotal(total: number) {
  return `共 ${total} 条`;
}

async function handleLockManage(type: 'FG' | 'WIP') {
  if (isReadonly.value) return;
  if (type === 'WIP' && !selectedOperation.value) {
    message.warning('请先点击左侧工序行，再挂接半成品利库');
    return;
  }
  inventoryModalType.value = type;
  inventoryQuery.recipeCode = formState.recipeCode || '';
  inventoryQuery.specSize = formState.sizeSpec || '';
  inventoryQuery.opSeq = type === 'WIP' ? defaultSourceOpSeqForSelectedOperation() : undefined;
  inventoryQuery.segmentCode = '';
  inventoryQuery.sourcePlanNo = '';
  inventoryQuery.sourceBatchNo = '';
  inventoryQuery.sourceParentBatchNo = '';
  inventoryQuery.modelNo = type === 'WIP' ? defaultWipModelQuery() : '';
  inventoryQuery.keyword = '';
  inventoryPagination.pageNo = 1;
  inventoryPagination.pageSize = type === 'WIP' ? 10 : 100;
  inventoryPagination.total = 0;
  inventoryModalOpen.value = true;
  await loadInventoryRows({ resetSelection: true });
}

function validatePickQty(row: MesHcInvStockApi.Stock) {
  const key = getStockRowKey(row);
  const maxQty = inventoryModalType.value === 'WIP'
    ? getWipCandidateShareableQty(row)
    : normalizeNumber(row.availableQty);
  inventoryPickQty[key] = Math.min(Math.max(normalizeNumber(inventoryPickQty[key]), 0), maxQty);
}

function buildLockFromStock(row: MesHcInvStockApi.Stock, lockQty: number): EditableInventoryLock {
  return {
    localKey: nextLocalKey('lock'),
    operationKey: inventoryModalType.value === 'WIP' ? selectedOperationKey.value : undefined,
    planOperationId: inventoryModalType.value === 'WIP' ? selectedOperation.value?.id : undefined,
    targetPlanNo: formState.planNo,
    targetOpCode: inventoryModalType.value === 'WIP' ? selectedOperation.value?.opCode : undefined,
    targetOpName: inventoryModalType.value === 'WIP' ? selectedOperation.value?.opName : undefined,
    lockType: inventoryModalType.value,
    stockId: row.id,
    stockType: row.stockType || inventoryModalType.value,
    sourceType: row.sourceType,
    sourceTable: row.sourceTable,
    sourceId: row.sourceId,
    sourcePlanId: row.sourcePlanId,
    sourcePlanNo: row.sourcePlanNo,
    sourcePlanOperationId: row.sourcePlanOperationId,
    sourceBatchNo: row.sourceBatchNo || row.batchNo,
    opSeq: row.opSeq,
    opCode: row.opCode,
    opName: row.opName,
    segmentCode: row.segmentCode,
    segmentName: row.segmentName,
    thickness: row.thickness,
    lotNo: row.batchNo,
    batchNo: row.batchNo,
    materialId: row.materialId,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelNo: row.modelNo,
    recipeCode: row.recipeCode,
    sizeSpec: row.specSize,
    productionDate: row.productionDate,
    expiryDate: row.expiryDate,
    remainMonths: getRemainMonths(row.expiryDate),
    locationCode: row.locationCode,
    locationName: row.locationName,
    ownerId: row.ownerId,
    ownerCode: row.ownerCode,
    ownerName: row.ownerName,
    availableQty: inventoryModalType.value === 'WIP' ? getWipCandidateShareableQty(row) : row.availableQty,
    lockQty,
    consumedQty: 0,
    releasedQty: 0,
    remainingQty: lockQty,
    unitCode: row.uom,
    uom: row.uom || formState.targetUom,
    lockStatus: 'ACTIVE',
    remark: row.businessRemark,
  };
}

function confirmInventorySelection() {
  if (isReadonly.value) return;
  const keepRows = lockRows.value.filter(
    (item) =>
      item.lockType !== inventoryModalType.value ||
      (inventoryModalType.value === 'WIP' && item.operationKey !== selectedOperationKey.value),
  );
  const pickedRows = Object.entries(inventoryPickQty)
    .map(([key, qty]) => ({ row: inventoryRowCache[key], qty: normalizeNumber(qty) }))
    .filter((item): item is { row: MesHcInvStockApi.Stock; qty: number } => !!item.row && item.qty > 0)
    .map((item) => buildLockFromStock(item.row, item.qty));
  lockRows.value = [...keepRows, ...pickedRows];
  activeLockTab.value = inventoryModalType.value;
  inventoryModalOpen.value = false;
  recomputeSummary({ refreshOperations: false });
}

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  fullscreenButton: false,
  closable: false,
  header: false,
  footer: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'hc-plan-order-modal',
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      openToken.value += 1;
      clearBatchPreviewTimer();
      productModelValue.value = undefined;
      productBomPickerOpen.value = false;
      saleOrderPickerOpen.value = false;
      inventoryModalOpen.value = false;
      statusLogOpen.value = false;
      releaseModalOpen.value = false;
      releaseTargetLock.value = undefined;
      routeMaintenanceMode.value = false;
      return;
    }
    const currentToken = ++openToken.value;
    routeMaintenanceMode.value = false;
    await loadBaseOptions();
    if (currentToken !== openToken.value) return;
    const data = modalApi.getData<MesHcPlanOrderApi.PlanOrder | null>();
    if (data?.id) {
      await loadDetail(Number(data.id), currentToken);
    } else {
      if (currentToken !== openToken.value) return;
      resetFormState();
    }
  },
});

onBeforeUnmount(() => {
  openToken.value += 1;
  clearBatchPreviewTimer();
  cleanupSplitResize();
});

</script>

<template>
  <Modal>
    <PickerModal
      :config="productBomPickerConfig"
      :open="productBomPickerOpen"
      title="选择产品料号"
      @close="productBomPickerOpen = false"
      @pick="handleProductBomPick"
    />
    <PickerModal
      :config="saleOrderPickerConfig"
      :open="saleOrderPickerOpen"
      title="选择销售订单"
      @close="saleOrderPickerOpen = false"
      @pick="handleSaleOrderPick"
    />
    <AntModal
      v-model:open="inventoryModalOpen"
      :title="inventoryModalType === 'FG' ? '检索成品库并挂接全局抵扣' : `检索线边半成品库（当前工序：${formatOperationLabel(selectedOperation)}）`"
      width="96vw"
      :footer="null"
      :destroy-on-close="false"
      wrap-class-name="pp-inv-modal-wrap"
    >
      <div class="pp-inv-modal">
        <fieldset class="pp-fieldset pp-inv-filter">
          <legend>过滤与检索条件</legend>
          <div class="pp-inv-filter-grid">
            <template v-if="inventoryModalType === 'WIP'">
              <div class="pp-form-item">
                <label>计划号</label>
                <Input v-model:value="inventoryQuery.sourcePlanNo" placeholder="如 20260528-001" />
              </div>
              <div class="pp-form-item">
                <label>型号</label>
                <Input v-model:value="inventoryQuery.modelNo" placeholder="默认当前计划型号" />
              </div>
              <div class="pp-form-item">
                <label>批次号</label>
                <Input v-model:value="inventoryQuery.sourceBatchNo" placeholder="产品批次号" />
              </div>
              <div class="pp-form-item">
                <label>工序</label>
                <Select
                  v-model:value="inventoryQuery.opSeq"
                  allow-clear
                  :options="operationRows.map((item) => ({ value: item.opSeq, label: formatOperationLabel(item) }))"
                />
              </div>
            </template>
            <template v-else>
            <div class="pp-form-item">
              <label>配方</label>
              <Input v-model:value="inventoryQuery.recipeCode" placeholder="留空查所有" />
            </div>
            <div class="pp-form-item">
              <label>尺寸规格</label>
              <Input v-model:value="inventoryQuery.specSize" placeholder="留空查所有" />
            </div>
            <div class="pp-form-item">
              <label>关键词</label>
              <Input v-model:value="inventoryQuery.keyword" placeholder="批号/物料/型号" />
            </div>
            </template>
          </div>
          <div class="pp-inv-filter-footer">
            <span>目标量参考：<b>{{ inventoryModalType === 'FG' ? formatNumber(formState.targetQty, 3) : formatNumber(selectedOperation?.requiredQty, 3) }}</b></span>
            <Button type="primary" :loading="inventoryLoading" @click="refreshInventoryRows">刷新检索</Button>
          </div>
        </fieldset>

        <div class="pp-inv-table-wrap">
          <table class="pp-grid">
            <thead>
              <tr v-if="inventoryModalType === 'FG'">
                <th width="120">物料号</th>
                <th>物料名</th>
                <th width="120">型号</th>
                <th width="80">尺寸</th>
                <th width="140">批号 / UID</th>
                <th width="90">生产日期</th>
                <th width="90">失效日期</th>
                <th width="80">到期情况</th>
                <th width="70">余效(月)</th>
                <th width="90">可用量</th>
                <th width="100">本次取用</th>
              </tr>
              <tr v-else>
                <th width="130">型号</th>
                <th width="150">批次号</th>
                <th width="90">厚度</th>
                <th width="100">可用量</th>
                <th>备注</th>
                <th width="100">本次取用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in inventoryRows" :key="getStockRowKey(row)">
                <template v-if="inventoryModalType === 'WIP'">
                  <td>{{ row.modelNo || '-' }}</td>
                  <td>{{ row.sourceBatchNo || row.batchNo || '-' }}</td>
                  <td align="right" class="pp-grid__number">{{ formatNumber(row.thickness, 3) }}</td>
                  <td align="right" class="pp-grid__number">{{ formatNumber(getWipCandidateShareableQty(row), 3) }} {{ row.uom || '' }}</td>
                  <td>{{ getWipCandidateTxnSummary(row) }}</td>
                  <td>
                    <InputNumber
                      v-model:value="inventoryPickQty[getStockRowKey(row)]"
                      :min="0"
                      :max="getWipCandidateShareableQty(row)"
                      :precision="3"
                      size="small"
                      class="w-full"
                      @change="() => validatePickQty(row)"
                    />
                  </td>
                </template>
                <template v-else>
                  <td class="pp-grid__code">{{ row.materialCode || '-' }}</td>
                  <td>{{ row.materialName || '-' }}</td>
                  <td>{{ row.modelNo || '-' }}</td>
                  <td>{{ row.specSize || '-' }}</td>
                  <td>
                    <div>{{ row.batchNo || '-' }}</div>
                    <div class="pp-grid__sub">{{ row.id || '-' }}</div>
                  </td>
                  <td>{{ normalizeLocalDate(row.productionDate) || '-' }}</td>
                  <td>{{ normalizeLocalDate(row.expiryDate) || '-' }}</td>
                  <td>{{ getExpiryStatus(row) }}</td>
                  <td align="right">{{ getRemainMonths(row.expiryDate) ?? '-' }}</td>
                  <td align="right" class="pp-grid__number">{{ formatNumber(row.availableQty, 3) }} {{ row.uom || '' }}</td>
                  <td>
                    <InputNumber
                      v-model:value="inventoryPickQty[getStockRowKey(row)]"
                      :min="0"
                      :max="Number(row.availableQty || 0)"
                      :precision="3"
                      size="small"
                      class="w-full"
                      @change="() => validatePickQty(row)"
                    />
                  </td>
                </template>
              </tr>
              <tr v-if="inventoryRows.length === 0">
                <td :colspan="inventoryModalType === 'FG' ? 11 : 6" class="pp-empty-cell" align="center">
                  {{ inventoryLoading ? '正在检索库存...' : '未发现匹配条件的库存' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pp-inv-footer">
          <div>勾选合计：<b>{{ formatNumber(inventoryPickTotal, 3) }}</b></div>
          <Pagination
            v-if="inventoryModalType === 'WIP'"
            size="small"
            :current="inventoryPagination.pageNo"
            :page-size="inventoryPagination.pageSize"
            :show-total="showInventoryTotal"
            :total="inventoryPagination.total"
            show-size-changer
            @change="handleInventoryPageChange"
          />
          <div class="pp-actionbar__buttons">
            <Button @click="inventoryModalOpen = false">取消</Button>
            <Button type="primary" @click="confirmInventorySelection">
              {{ inventoryModalType === 'WIP' ? '确认带回，保存计划后锁定' : '确认带回并锁定' }}
            </Button>
          </div>
        </div>
      </div>
    </AntModal>
    <AntModal
      v-model:open="statusLogOpen"
      title="生产计划状态操作日志"
      width="900px"
      :footer="null"
      :destroy-on-close="false"
      wrap-class-name="pp-status-log-modal-wrap"
    >
      <div class="pp-status-log-modal">
        <Table
          :columns="statusLogColumns"
          :data-source="statusLogRows"
          :loading="statusLogLoading"
          :pagination="false"
          :scroll="{ y: 360 }"
          row-key="id"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'operateTime'">
              {{ normalizeLocalDateTime(record.operateTime) || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'operatorName'">
              {{ record.operatorName || record.operatorId || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'actionType'">
              {{ planStatusActionLabel(record.actionType) }}
            </template>
            <template v-else-if="column.dataIndex === 'fromStatus'">
              {{ planStatusLabel(record.fromStatus) }}
            </template>
            <template v-else-if="column.dataIndex === 'toStatus'">
              {{ planStatusLabel(record.toStatus) }}
            </template>
            <template v-else-if="column.dataIndex === 'reasonRemark'">
              {{ record.reasonRemark || '-' }}
            </template>
          </template>
        </Table>
      </div>
    </AntModal>
    <AntModal
      v-model:open="releaseModalOpen"
      title="释放计划挂接剩余量"
      width="620px"
      :footer="null"
    >
      <div class="pp-release-modal">
        <div class="pp-release-summary">
          <div>
            <span>目标计划</span>
            <b>{{ releaseTargetLock?.targetPlanNo || formState.planNo || '-' }}</b>
          </div>
          <div>
            <span>目标工序</span>
            <b>{{ releaseTargetLock?.targetOpName || releaseTargetLock?.targetOpCode || '-' }}</b>
          </div>
          <div>
            <span>来源计划</span>
            <b>{{ releaseTargetLock?.sourcePlanNo || '-' }}</b>
          </div>
          <div>
            <span>产品批次</span>
            <b>{{ releaseTargetLock?.sourceBatchNo || releaseTargetLock?.batchNo || '-' }}</b>
          </div>
          <div>
            <span>锁定量</span>
            <b>{{ formatNumber(releaseTargetLock?.lockQty, 3) }}</b>
          </div>
          <div>
            <span>已消耗</span>
            <b>{{ formatNumber(releaseTargetLock?.consumedQty, 3) }}</b>
          </div>
          <div>
            <span>已释放</span>
            <b>{{ formatNumber(releaseTargetLock?.releasedQty, 3) }}</b>
          </div>
          <div>
            <span>剩余量</span>
            <b>{{ formatNumber(releaseTargetLock ? getLockRemainingQty(releaseTargetLock) : 0, 3) }}</b>
          </div>
        </div>
        <div class="pp-form-item">
          <label>释放数量</label>
          <InputNumber
            v-model:value="releaseForm.releaseQty"
            :min="0"
            :max="releaseTargetLock ? getLockRemainingQty(releaseTargetLock) : 0"
            :precision="3"
            class="w-full"
            @change="validateReleaseQty"
          />
        </div>
        <div class="pp-form-item">
          <label>释放原因</label>
          <Input.TextArea
            v-model:value="releaseForm.releaseReason"
            :rows="3"
            placeholder="请填写人工释放原因"
          />
        </div>
        <div class="pp-release-footer">
          <Button @click="releaseModalOpen = false">取消</Button>
          <Button type="primary" danger :loading="releaseSubmitting" @click="confirmReleaseInventoryLock">
            确认释放
          </Button>
        </div>
      </div>
    </AntModal>
    <div class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title">
            <span class="pp-plan-toolbar__main">生产计划详情</span>
            <span class="pp-plan-desc">{{ planHeaderDescription }}</span>
            <span v-if="isCancelledPlan" class="pp-plan-void-warning">
              <IconifyIcon icon="lucide:ban" />
              作废计划，请勿继续排产或报工
            </span>
            <span v-if="isReadonly" class="pp-plan-tag">查阅不可编辑</span>
        </div>
        <div class="pp-plan-toolbar__actions">
          <template v-if="!isReadonly">
            <Button size="small" :disabled="Boolean(formState.id)" @click="resetFormState">清空重设</Button>
            <Button size="small" @click="savePlan('DRAFT')" :loading="saving">保存草稿</Button>
            <!-- 完整 APS 生成推算尚未定版，暂隐藏；当前仅保留轻量前端计算。 -->
            <Button size="small" type="primary" danger @click="savePlan('RELEASED')" :loading="saving">确认下发</Button>
            <Button
              v-access:code="['mes:pp:plan:status-admin']"
              size="small"
              :disabled="!canSetPlanStatus"
              @click="handlePlanStatusAction"
            >
              设置状态
            </Button>
          </template>
          <template v-else-if="isReleasedPlan">
            <Button size="small" :disabled="!canWithdrawPlan" @click="handleWithdrawPlan">撤回计划</Button>
            <Button size="small" :disabled="!canPausePlan" @click="openPlanInstruction('PAUSE')">计划暂停</Button>
            <Button size="small" :disabled="!canResumePlan" @click="openPlanInstruction('RESUME')">计划复工</Button>
          </template>
          <template v-else-if="isPausedPlan">
            <Button size="small" :disabled="!canPausePlan" @click="openPlanInstruction('PAUSE')">计划暂停</Button>
            <Button size="small" type="primary" :disabled="!canResumePlan" @click="openPlanInstruction('RESUME')">计划复工</Button>
          </template>
          <Button size="small" :disabled="!formState.id" @click="openStatusLogModal">操作日志</Button>
          <Button size="small" @click="modalApi.close()">关闭窗口</Button>
        </div>
      </div>

      <div class="pp-plan-body">
        <fieldset class="pp-fieldset pp-fieldset--basic">
          <legend>1. 目标与基础属性</legend>
          <!-- 计划单号、计划日期、来源类型等为后端控制字段，原型未展示，禁止在此区直接铺开。 -->
          <div class="pp-form-grid pp-form-grid--basic">
            <!-- 排产模式设定为后端控制字段，当前版本按销售订单/手工来源自动判断，界面隐藏。 -->
            <div class="pp-form-item">
              <label>产品型号</label>
              <Select
                v-if="!isReadonly"
                :value="productModelValue"
                show-search
                allow-clear
                :disabled="isProductIdentityLocked"
                :filter-option="false"
                :options="productModelOptions.map((item) => ({ label: item.label, value: item.value }))"
                placeholder="请选择BOM中的产品型号"
                @search="loadProductModelOptions"
                @change="handleProductModelChange"
              />
              <div v-else class="pp-readonly-box pp-readonly-box--code" :title="formState.modelCode || '-'">
                {{ formState.modelCode || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>产品料号</label>
              <PickerInline
                :model-value="formState.materialCode"
                :config="productBomPickerConfig"
                :disabled="isReadonly || isProductIdentityLocked"
                placeholder="请选择BOM中的产品料号"
                @update:model-value="handleProductionMaterialInput"
                @search="openProductionMaterialPicker"
                @pick="handleProductBomPick"
              />
            </div>
            <div class="pp-form-item">
              <label>{{ PLAN_FIELD_COPY.sizeSpec }}</label>
              <Select
                v-model:value="formState.sizeSpec"
                allow-clear
                :options="sizeSpecOptions"
                :disabled="isReadonly"
                option-filter-prop="label"
                not-found-content="暂无尺寸规则，可留空"
                :placeholder="PLAN_PLACEHOLDER_COPY.sizeSpec"
                @change="handleSizeSpecChange"
              />
            </div>
            <div class="pp-form-item">
              <label>生产类型</label>
              <Select
                v-model:value="formState.prodType"
                allow-clear
                :options="prodTypeOptions"
                :disabled="isReadonly"
                option-filter-prop="label"
                placeholder="请选择生产类型"
                @change="handleProdTypeChange"
              />
            </div>
            <div class="pp-form-item">
              <label>物料类型</label>
              <Select
                v-model:value="formState.categoryCode"
                allow-clear
                :options="materialCategoryOptions"
                :disabled="isReadonly"
                option-filter-prop="label"
                placeholder="请选择物料类型"
                @change="handleMaterialCategoryChange"
              />
            </div>
            <div class="pp-form-item">
              <label>计划开始日期</label>
              <DatePicker
                v-model:value="formState.productionStartDate"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                :disabled="isReadonly"
                class="w-full"
                placeholder="请选择生产开始日期"
              />
            </div>
            <div class="pp-form-item">
              <label>计划结束日期</label>
              <DatePicker
                v-model:value="formState.productionEndDate"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                :disabled="isReadonly"
                class="w-full"
                placeholder="请选择生产结束日期"
              />
            </div>
            <div class="pp-form-item">
              <label>目标量</label>
              <InputNumber
                v-model:value="formState.targetQty"
                :min="0"
                :precision="3"
                :disabled="isReadonly"
                class="w-full"
              />
            </div>
            <div v-if="isFormulaStartPlan || isProductionBatchGenerated" class="pp-form-item">
              <label>产品批号</label>
              <div class="pp-batch-input-row">
                <Input
                  v-model:value="formState.batchNo"
                  :disabled="isReadonly || isBatchNoLocked"
                  placeholder="规则预览；修改后按规则校验"
                  @blur="normalizeManualBatchNoInput"
                  @change="markManualBatchTouched"
                  @input="markManualBatchTouched"
                />
                <Button
                  size="small"
                  :loading="batchPreviewLoading"
                  :disabled="isReadonly || isBatchNoLocked"
                  @click="previewAndFillBatchNo(true)"
                >
                  刷新
                </Button>
              </div>
              <span class="pp-form-hint">
                {{ batchNoHint }}
              </span>
            </div>
            <div class="pp-form-item pp-form-item--span-3">
              <label>排程执行要求</label>
              <Input v-model:value="formState.remark" :disabled="isReadonly" />
            </div>
          </div>
        </fieldset>

        <div
          ref="splitContainerRef"
          class="pp-split"
          :class="{ 'pp-split--lock-collapsed': lockPanelCollapsed, 'is-resizing': splitDragging }"
          :style="splitStyle"
        >
          <div class="pp-panel">
            <div class="pp-panel__header">
              <span>2. 工位任务列表</span>
            </div>
            <!-- 工艺路线下拉同时包含当前物料绑定路线和通用路线；BOM 字段仅保留隐藏持久化，不在工作台展示。 -->
            <div class="pp-route-toolbar">
              <span class="pp-route-toolbar__label">配置</span>
              <Select
                v-model:value="formState.routeId"
                size="small"
                show-search
                allow-clear
                :options="routeOptions"
                :disabled="isReadonly || !routeMaintenanceMode"
                placeholder="请选择工艺路线（含通用路线）"
                class="pp-route-toolbar__select"
                @change="handleRouteChange"
              />
              <Button
                v-if="!isReadonly"
                size="small"
                @click="toggleRouteMaintenanceMode"
              >
                {{ routeMaintenanceMode ? '退出维护' : '维护路线' }}
              </Button>
              <Button
                v-if="!isReadonly && routeMaintenanceMode"
                size="small"
                @click="clearSelectedRoute"
              >
                清除选择
              </Button>
              <Button
                v-if="!isReadonly && routeMaintenanceMode"
                size="small"
                :disabled="checkedOperations.length === 0"
                @click="saveCheckedOperationsAsNewGlobalRoute"
              >
                选择工序另存新工艺路线
              </Button>
              <Button
                v-if="!isReadonly && routeMaintenanceMode"
                size="small"
                danger
                :disabled="!formState.routeId && operationRows.length === 0"
                @click="deleteCurrentRoute"
              >
                {{ checkedOperations.length > 0 ? '删除选中工序' : '清空当前计划路线' }}
              </Button>
              <div v-if="canSetOperationStatus" class="pp-status-actions">
                <Button size="small" :disabled="checkedOperations.length === 0" @click="openOperationInstruction('PAUSE')">暂停</Button>
                <Button size="small" :disabled="checkedOperations.length === 0" @click="openOperationInstruction('RESUME')">复工</Button>
                <Button size="small" type="primary" :disabled="checkedOperations.length === 0" @click="openOperationInstruction('DAILY')">下达指令</Button>
              </div>
            </div>
            <div class="pp-table-wrap">
              <table class="pp-grid pp-grid--plain">
                <thead>
                  <tr>
                    <th v-if="showOperationSeqColumn" width="52">序号</th>
                    <th v-if="showOperationSelectColumn" width="42">选</th>
                    <th v-if="showOperationMoveColumn" width="78">顺序</th>
                    <th v-if="isReadonly" width="64">状态</th>
                    <th width="130">工序名称</th>
                    <th width="150" v-if="!isReadonly">默认设备</th>
                    <th width="96">利库量</th>
                    <!-- 执行要求字段仍随保存提交，当前表格按要求隐藏列展示。 -->
                    <th v-if="isReadonly" width="118">累计报工量</th>
                    <th
                      v-for="date in operationReportDateColumns"
                      :key="date"
                      width="96"
                    >
                      {{ date.slice(5) }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, rowIndex) in operationRows"
                    :key="row.localKey"
                    :class="{ 'is-selected': selectedOperationKey === row.localKey, 'is-cancelled': row.operationStatus === 'CANCELLED' }"
                    @click="handleOperationRowClick(row)"
                  >
                    <td v-if="showOperationSeqColumn" align="center">{{ row.opSeq || rowIndex + 1 }}</td>
                    <td v-if="showOperationSelectColumn" align="center" @click.stop>
                      <Checkbox
                        :checked="checkedOperationKeys.includes(row.localKey)"
                        @change="(event) => handleOperationChecked(row, event.target.checked)"
                      />
                    </td>
                    <td v-if="showOperationMoveColumn" @click.stop>
                      <div class="pp-row-move">
                        <Tooltip title="上移">
                          <Button
                            aria-label="上移工序"
                            size="small"
                            type="text"
                            :disabled="rowIndex === 0"
                            @click="moveOperation(row.localKey, -1)"
                          >
                            <IconifyIcon icon="lucide:arrow-up" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="下移">
                          <Button
                            aria-label="下移工序"
                            size="small"
                            type="text"
                            :disabled="rowIndex === operationRows.length - 1"
                            @click="moveOperation(row.localKey, 1)"
                          >
                            <IconifyIcon icon="lucide:arrow-down" />
                          </Button>
                        </Tooltip>
                      </div>
                    </td>
                    <td v-if="isReadonly">
                      <div class="pp-status-icon-cell">
                        <span :class="operationStatusIconClass(row.operationStatus)" :title="operationStatusLabel(row.operationStatus)"></span>
                      </div>
                    </td>
                    <td>
                      <div class="pp-grid-readonly" :title="row.opName || row.opCode || ''">
                        {{ row.opName || row.opCode || '-' }}
                      </div>
                    </td>
                    <td v-if="!isReadonly">
                      <Select
                        v-model:value="row.equipmentId"
                        size="small"
                        show-search
                        allow-clear
                        class="w-full"
                        :options="getEquipmentOptionsByWorkCenter(Number(row.workCenterId))"
                        :disabled="isReadonly"
                        option-filter-prop="label"
                        @change="(value) => applyEquipmentChange(row, value as number | undefined)"
                      />
                    </td>
                    <td align="right" class="pp-grid__number">{{ formatNumber(operationLockedQty(row), 3) }}</td>
                    <td v-if="isReadonly" align="right" class="pp-grid__number">
                      {{ operationCumulativeReportQty(row) }}
                    </td>
                    <td
                      v-for="date in operationReportDateColumns"
                      :key="`${row.localKey}-${date}`"
                      align="right"
                    >
                      <span v-if="operationDailyReportQty(row, date) == null" class="pp-empty-cell">-</span>
                      <span v-else class="pp-grid__number">{{ formatNumber(operationDailyReportQty(row, date), 3) }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pp-status-legend">
              <span v-for="item in OPERATION_STATUS_OPTIONS" :key="item.value" class="pp-status-legend__item">
                <i :class="operationStatusIconClass(item.value)"></i>{{ item.label }}
              </span>
            </div>
          </div>

          <div
            v-if="!lockPanelCollapsed"
            aria-label="调整工位任务列表和利库明细宽度"
            class="pp-split-resizer"
            role="separator"
            title="拖动调整宽度，双击恢复默认"
            @dblclick="resetSplitResize"
            @pointerdown="startSplitResize"
          ></div>

          <div
            class="pp-panel pp-panel--lock"
            :class="{ 'is-collapsed': lockPanelCollapsed }"
          >
            <div class="pp-panel__header" @click="expandLockPanel">
              <span>3. 利库明细</span>
              <div class="pp-tabbar" v-if="!lockPanelCollapsed">
                <button class="pp-tab" type="button" @click.stop="collapseLockPanel">
                  收起
                </button>
                <button
                  class="pp-tab"
                  :class="{ active: activeLockTab === 'WIP' }"
                  type="button"
                  @click.stop="activeLockTab = 'WIP'"
                >
                  半成品利库
                </button>
                <button
                  class="pp-tab"
                  :class="{ active: activeLockTab === 'FG' }"
                  type="button"
                  @click.stop="activeLockTab = 'FG'"
                >
                  成品抵扣
                </button>
              </div>
            </div>
            <template v-if="!lockPanelCollapsed">
            <div class="pp-side-meta">
              <div v-if="activeLockTab === 'WIP'">
                {{ selectedOperation ? `当前工序：${formatOperationLabel(selectedOperation)}` : '尚未选择工序节点' }}
              </div>
              <div v-else>直接抵扣最终订单/排产需求</div>
              <Button
                v-if="activeLockTab === 'WIP'"
                size="small"
                :disabled="isReadonly"
                @click="handleLockManage('WIP')"
              >
                + 挂接半成品
              </Button>
              <Button
                v-else
                size="small"
                :disabled="isReadonly"
                @click="handleLockManage('FG')"
              >
                管理成品抵扣
              </Button>
            </div>
            <div class="pp-side-layout">
              <div class="pp-side-block">
                <div class="pp-table-wrap">
                  <table class="pp-grid pp-grid--plain">
                    <thead>
                      <tr>
                        <th v-if="activeLockTab === 'WIP'" width="130">来源计划</th>
                        <th width="150">{{ activeLockTab === 'WIP' ? '产品批次' : '批次号 / UID' }}</th>
                        <th v-if="activeLockTab === 'WIP'" width="110">来源工位</th>
                        <th v-if="activeLockTab === 'WIP'" width="120">物料/型号</th>
                        <th v-if="activeLockTab === 'FG'">尺寸</th>
                        <th width="76">状态</th>
                        <th v-if="activeLockTab === 'WIP'" width="70" align="right">剩余</th>
                        <th width="82" align="right">已释放</th>
                        <th width="70" align="right">抵扣数</th>
                        <th v-if="activeLockTab === 'WIP'">说明</th>
                        <th width="70">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="row in visibleInventoryLockRows"
                        :key="row.localKey"
                        :class="{ 'is-selected': selectedInventoryLockKey === row.localKey }"
                        @click="selectInventoryLock(row)"
                      >
                        <td v-if="activeLockTab === 'WIP'">{{ row.sourcePlanNo || '-' }}</td>
                        <td>
                          <div>{{ activeLockTab === 'WIP' ? (row.sourceBatchNo || row.batchNo || row.lotNo || '-') : (row.batchNo || row.lotNo || '-') }}</div>
                          <div v-if="activeLockTab === 'FG'" class="pp-grid__sub">{{ row.stockId || '-' }}</div>
                        </td>
                        <td v-if="activeLockTab === 'WIP'">{{ row.opName || row.opCode || '-' }}</td>
                        <td v-if="activeLockTab === 'WIP'">
                          <div>{{ row.materialName || row.materialCode || '-' }}</div>
                          <div class="pp-grid__sub">{{ row.modelNo || '-' }}</div>
                        </td>
                        <td v-if="activeLockTab === 'FG'">{{ row.sizeSpec || '-' }}</td>
                        <td>
                          <span class="pp-lock-status" :class="getInventoryLockStatusClass(row)">
                            {{ getInventoryLockStatusText(row) }}
                          </span>
                        </td>
                        <td v-if="activeLockTab === 'WIP'" align="right" class="pp-grid__number">{{ formatNumber(getLockRemainingQty(row), 3) }}</td>
                        <td align="right" class="pp-grid__number">{{ formatNumber(row.releasedQty || 0, 3) }}</td>
                        <td><InputNumber v-model:value="row.lockQty" size="small" :min="0" :max="Number(row.availableQty || 0)" :precision="3" :disabled="isReadonly" class="w-full" @change="() => syncLockRemainingQty(row)" /></td>
                        <td v-if="activeLockTab === 'WIP'">
                          <span v-if="isReadonly" class="pp-lock-remark" :title="getInventoryLockRemark(row)">{{ getInventoryLockRemark(row) }}</span>
                          <Input v-else v-model:value="row.remark" size="small" :disabled="isReadonly" />
                        </td>
                        <td>
                          <Button
                            v-if="!isReadonly"
                            size="small"
                            danger
                            type="text"
                            @click="removeLock(row.localKey)"
                          >
                            删
                          </Button>
                          <Button
                            v-else-if="canReleaseInventoryLock(row)"
                            size="small"
                            type="link"
                            @click="openReleaseInventoryLock(row)"
                          >
                            释放
                          </Button>
                          <span v-else class="pp-empty-cell">-</span>
                        </td>
                      </tr>
                      <tr v-if="activeLockTab === 'WIP' && (!selectedOperation || !hasVisibleWipLockForSelectedOperation)">
                        <td colspan="10" align="center" class="pp-empty-cell">
                          {{ selectedOperation ? '当前工序尚未挂接任何库存。' : '请点击左侧工序行以管理节点利库' }}
                        </td>
                      </tr>
                      <tr v-if="activeLockTab === 'FG' && !hasVisibleFgLock">
                        <td colspan="6" align="center" class="pp-empty-cell">暂无成品库挂接抵扣。</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class="pp-lock-detail">
                  <template v-if="selectedInventoryLock">
                    <div class="pp-lock-detail__meta">
                      <div class="pp-lock-detail__meta-item">
                        <span>选中批次</span>
                        <b>{{ selectedInventoryLock.sourceBatchNo || selectedInventoryLock.batchNo || selectedInventoryLock.lotNo || '-' }}</b>
                      </div>
                      <div class="pp-lock-detail__meta-item">
                        <span>状态</span>
                        <b>{{ getInventoryLockStatusText(selectedInventoryLock) }}</b>
                      </div>
                      <div class="pp-lock-detail__meta-item">
                        <span>剩余</span>
                        <b>{{ formatNumber(getLockRemainingQty(selectedInventoryLock), 3) }}</b>
                      </div>
                      <div class="pp-lock-detail__meta-item">
                        <span>已释放</span>
                        <b>{{ formatNumber(selectedInventoryLock.releasedQty || 0, 3) }}</b>
                      </div>
                      <div class="pp-lock-detail__meta-item">
                        <span>抵扣数</span>
                        <b>{{ formatNumber(selectedInventoryLock.lockQty || 0, 3) }}</b>
                      </div>
                    </div>
                    <div class="pp-lock-detail__remark">
                      <span>说明</span>
                      <p>{{ getInventoryLockRemark(selectedInventoryLock) }}</p>
                    </div>
                  </template>
                  <span v-else class="pp-lock-detail__empty">选中利库明细行后查看完整说明。</span>
                </div>
              </div>
            </div>
            </template>
          </div>
        </div>
      </div>
    </div>
    <ProductionInstructionIssueModal
      v-model:open="productionInstructionOpen"
      :context="productionInstructionContext"
      :default-instruction-type="productionInstructionType"
      :instruction-type-options="productionInstructionTypeOptions"
      :operation-options="productionInstructionOperationOptions"
      :segment-options="productionInstructionSegmentOptions"
      :show-operation="productionInstructionShowOperation"
      :show-segment="productionInstructionShowSegment"
      :title="productionInstructionTitle"
      :z-index="4600"
      @success="handleProductionInstructionSuccess"
    />
  </Modal>
</template>

<style scoped>
.pp-plan-modal {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  color: #1f2937;
  font-size: 12px;
}

.pp-plan-toolbar {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.pp-plan-toolbar__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pp-plan-toolbar__main {
  color: #1677ff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.pp-plan-desc {
  color: #4b5563;
  font-size: 13px;
  font-weight: 700;
}

.pp-plan-tag {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.pp-fieldset--basic {
  overflow-x: auto;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-grid--basic {
  min-width: 980px;
  grid-template-columns: repeat(4, minmax(220px, 1fr));
}

.pp-form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pp-form-item--inline {
  flex-direction: row;
  align-items: center;
  overflow: visible;
  padding-bottom: 8px;
  margin-bottom: 2px;
  border-bottom: 1px dashed #d9d9d9;
}

.pp-form-item label {
  color: #4b5563;
  font-weight: 700;
}

.pp-form-hint {
  color: #6b7280;
  font-size: 12px;
  line-height: 16px;
}

.pp-batch-input-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pp-batch-input-row :deep(.ant-input) {
  min-width: 0;
  flex: 1;
}

.pp-batch-input-row :deep(.ant-btn) {
  flex-shrink: 0;
}

.pp-form-item--inline label {
  width: 95px;
  margin-right: 10px;
  text-align: right;
  flex-shrink: 0;
}

.pp-mode-group {
  display: flex;
  flex: 1;
  min-width: 520px;
  gap: 16px;
  align-items: center;
  height: 28px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.pp-mode-group :deep(.ant-radio-wrapper) {
  display: inline-flex;
  align-items: center;
  min-width: 210px;
  margin-inline-end: 0;
  white-space: nowrap;
}

.pp-inline-picker {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pp-inline-picker__btn {
  flex-shrink: 0;
  min-width: 38px;
}

.pp-inline-metric {
  display: flex;
  align-items: center;
  width: 100%;
}

.pp-inline-metric__value {
  flex: 1;
}

.pp-inline-metric__unit {
  height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  border-left: none;
  background: #f5f5f5;
  color: #1677ff;
  font-weight: 700;
}

.pp-form-item--wide {
  grid-column: span 2;
}

.pp-form-item--span-3 {
  grid-column: span 3;
}

.pp-form-item--full {
  grid-column: 1 / -1;
}

.pp-info-block {
  height: 28px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  background: #fff;
  font-weight: 700;
}

.pp-info-block--warn {
  background: #fffbe6;
  color: #ad6800;
}

.pp-info-block--code {
  color: #1677ff;
  background: #f5f7fa;
  font-family: Consolas, Monaco, monospace;
}

.pp-release-modal {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pp-release-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.pp-release-summary div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.pp-release-summary span {
  color: #64748b;
}

.pp-release-summary b {
  color: #1677ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-release-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.pp-readonly-box {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fafafa;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
}

.pp-readonly-box--code {
  color: #1677ff;
  font-family: Consolas, Monaco, monospace;
}

.pp-table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 6px;
}

.pp-split {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns:
    minmax(360px, var(--pp-operation-panel-width, 58%))
    8px
    minmax(420px, 1fr);
  gap: 0;
}

.pp-split--lock-collapsed {
  grid-template-columns: minmax(0, 1fr) 48px;
  gap: 8px;
}

.pp-split.is-resizing,
.pp-split.is-resizing * {
  cursor: col-resize !important;
}

.pp-split-resizer {
  position: relative;
  min-width: 8px;
  cursor: col-resize;
  background: #eef4fb;
}

.pp-split-resizer::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 3px;
  width: 2px;
  border-radius: 2px;
  background: #94a3b8;
  content: '';
}

.pp-split-resizer:hover,
.pp-split.is-resizing .pp-split-resizer {
  background: #dbeafe;
}

.pp-split-resizer:hover::before,
.pp-split.is-resizing .pp-split-resizer::before {
  background: #1677ff;
}

.pp-panel {
  min-width: 0;
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

.pp-route-toolbar {
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5e7eb;
}

.pp-route-toolbar__label {
  color: #1677ff;
  font-weight: 700;
  font-size: 13px;
}

.pp-route-toolbar__select {
  width: 280px;
}

.pp-status-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-table-wrap--small {
  max-height: 160px;
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

.pp-row-move {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.pp-row-move :deep(.ant-btn) {
  width: 24px;
  padding: 0;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  font-weight: 400;
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-grid tbody tr.is-selected {
  background: #bae0ff !important;
  outline: 1px solid #1677ff;
}

.pp-grid tbody tr.is-cancelled {
  color: #9ca3af;
  text-decoration: line-through;
}

.pp-grid.pp-grid--plain th,
.pp-grid.pp-grid--plain td,
.pp-grid.pp-grid--plain tbody tr:hover,
.pp-grid.pp-grid--plain tbody tr.is-selected {
  background: transparent !important;
}

.pp-grid.pp-grid--plain tbody tr.is-selected {
  outline: 1px solid #1677ff;
}

.pp-grid__number {
  color: #1677ff;
  font-weight: 700;
}

.pp-lock-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-weight: 700;
  line-height: 22px;
  white-space: nowrap;
}

.pp-lock-status.is-active {
  background: #ecfdf3;
  color: #15803d;
}

.pp-lock-status.is-consumed {
  background: #eef2ff;
  color: #1d4ed8;
}

.pp-lock-status.is-released {
  background: #fff7ed;
  color: #c2410c;
}

.pp-lock-status.is-cancelled {
  background: #fef2f2;
  color: #b91c1c;
}

.pp-lock-remark {
  display: block;
  max-width: 220px;
  overflow: hidden;
  color: #475569;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-lock-detail {
  flex-shrink: 0;
  min-height: 78px;
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
}

.pp-lock-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
}

.pp-lock-detail__meta-item {
  min-width: 92px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pp-lock-detail__meta-item span,
.pp-lock-detail__remark span {
  color: #64748b;
  font-weight: 700;
  white-space: nowrap;
}

.pp-lock-detail__meta-item b {
  min-width: 0;
  overflow: hidden;
  color: #1677ff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-lock-detail__remark {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.pp-lock-detail__remark p {
  margin: 0;
  color: #1f2937;
  line-height: 18px;
  word-break: break-all;
}

.pp-lock-detail__empty {
  display: flex;
  align-items: center;
  height: 60px;
  color: #64748b;
}

.pp-grid-readonly {
  min-height: 24px;
  display: flex;
  align-items: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-grid-readonly--multiline {
  min-height: 32px;
  align-items: flex-start;
  line-height: 1.25;
  font-size: 12px;
  overflow: visible;
  white-space: pre-line;
}

.pp-status-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  background: #f5f7fa;
  color: #4b5563;
}

.pp-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.pp-status-text--running {
  color: #1677ff;
  border-color: #91caff;
  background: #e6f4ff;
}

.pp-status-text--paused {
  color: #d48806;
  border-color: #ffe58f;
  background: #fffbe6;
}

.pp-status-text--finished {
  color: #389e0d;
  border-color: #b7eb8f;
  background: #f6ffed;
}

.pp-status-text--cancelled {
  color: #8c8c8c;
  border-color: #d9d9d9;
  background: #f5f5f5;
}

.pp-status-text--not_released {
  color: #595959;
}

.pp-status-icon {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid currentColor;
  border-radius: 50%;
  color: #595959;
  vertical-align: middle;
}

.pp-status-icon-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.pp-status-icon::after {
  width: 6px;
  height: 6px;
  content: '';
  border-radius: 50%;
  background: currentColor;
}

.pp-status-icon--running {
  color: #1677ff;
}

.pp-status-icon--paused {
  color: #d48806;
}

.pp-status-icon--finished {
  color: #389e0d;
}

.pp-status-icon--cancelled {
  color: #8c8c8c;
}

.pp-status-icon--not_released {
  color: #595959;
}

.pp-grid td.is-pause-date {
  background: #fff1f0;
  color: #cf1322;
  font-weight: 700;
}

.pp-status-legend {
  min-height: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 10px;
  border-top: 1px solid #e5e7eb;
  background: #fafafa;
  color: #4b5563;
  font-size: 12px;
}

.pp-status-legend__item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.pp-status-legend__tip {
  margin-left: auto;
  color: #cf1322;
}

.pp-grid__sub {
  color: #6b7280;
  font-size: 11px;
}

.pp-tabbar {
  display: flex;
  gap: 2px;
}

.pp-tab {
  height: 24px;
  padding: 0 10px;
  border: 1px solid #d9d9d9;
  background: #f5f7fa;
  cursor: pointer;
  font-size: 12px;
}

.pp-tab.active {
  background: #fff;
  color: #1677ff;
  font-weight: 700;
}

.pp-side-meta {
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  color: #4b5563;
}

.pp-side-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: 1fr;
}

.pp-panel--lock.is-collapsed .pp-panel__header {
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 8px 0;
  cursor: pointer;
}

.pp-panel--lock.is-collapsed .pp-panel__header > span {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  font-size: 12px;
}

.pp-side-block {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.pp-side-block + .pp-side-block {
  border-top: 1px solid #e5e7eb;
}

.pp-side-block__title {
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-weight: 700;
}

.pp-empty-cell {
  color: #9ca3af;
}

.pp-actionbar {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-actionbar__summary {
  color: #389e0d;
  font-weight: 700;
}

.pp-actionbar__buttons {
  display: flex;
  gap: 8px;
}

.pp-inv-modal {
  height: 78vh;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
}

.pp-inv-filter {
  flex-shrink: 0;
  background: #fff;
}

.pp-inv-filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-inv-filter .pp-form-item {
  min-width: 0;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.pp-inv-filter .pp-form-item label {
  width: 68px;
  flex-shrink: 0;
  margin: 0;
  text-align: right;
}

.pp-inv-filter .pp-form-item > :not(label) {
  min-width: 0;
  flex: 1;
}

.pp-inv-filter-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #d9d9d9;
}

.pp-inv-filter-footer b,
.pp-inv-footer b {
  color: #389e0d;
}

.pp-inv-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-inv-footer {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-grid__code {
  color: #1677ff;
  font-weight: 700;
}

.pp-save-route-modal {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pp-save-route-modal__tips {
  color: #4b5563;
  font-weight: 700;
}

.pp-save-route-modal__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 96px;
  overflow: auto;
}

.pp-save-route-modal__chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #91caff;
  border-radius: 12px;
  background: #e6f4ff;
  color: #1677ff;
  font-size: 12px;
}

.pp-save-route-modal__comma {
  display: inline-flex;
  align-items: center;
  color: #6b7280;
}

.pp-status-action-modal {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pp-status-log-modal {
  height: 420px;
  overflow: hidden;
}

.pp-status-log-modal-wrap .ant-modal-body {
  height: 468px;
  padding: 16px;
}

.pp-status-action-modal__label {
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.pp-status-action-modal__range {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.pp-status-action-modal__date-list {
  max-height: 136px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  overflow: auto;
  padding: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.pp-status-action-modal__control {
  width: 100%;
  height: 32px;
}

.pp-status-action-modal__textarea {
  width: 100%;
  min-height: 76px;
}

.pp-date-mark {
  color: #cf1322;
  font-size: 12px;
  font-weight: 700;
}

.is-resume-date .pp-date-mark {
  color: #389e0d;
}

.pp-plan-modal {
  color: #1e293b;
  background: #dfe7f0;
}

.pp-plan-toolbar {
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  border-bottom-color: #8794a4;
}

.pp-plan-toolbar__main {
  color: #0f172a;
}

.pp-plan-desc,
.pp-plan-tag,
.pp-release-summary span,
.pp-status-legend,
.pp-grid__sub,
.pp-empty-cell,
.pp-save-route-modal__comma {
  color: #64748b;
}

.pp-plan-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.pp-plan-void-warning {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 24px;
  padding: 0 8px;
  color: #b42318;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #fff1f3;
  border: 1px solid #fda29b;
}

.pp-fieldset,
.pp-panel {
  background: #eef3f8;
  border-color: #8794a4;
}

.pp-fieldset legend,
.pp-panel__header {
  color: #0f172a;
}

.pp-form-item label,
.pp-route-toolbar__label,
.pp-save-route-modal__tips,
.pp-status-action-modal__label {
  color: #334155;
}

.pp-form-item--inline,
.pp-inv-filter-footer {
  border-bottom-color: #cbd5e1;
}

.pp-inline-metric__unit,
.pp-info-block,
.pp-readonly-box,
.pp-status-text,
.pp-tab,
.pp-status-action-modal__date-list {
  color: #1e293b;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.pp-inline-metric__unit,
.pp-info-block--code,
.pp-readonly-box--code,
.pp-release-summary b,
.pp-grid__number,
.pp-grid__code,
.pp-status-text--running,
.pp-status-icon--running,
.pp-tab.active,
.pp-side-block__title {
  color: #075985;
}

.pp-info-block--code,
.pp-tab.active,
.pp-status-text--running,
.pp-save-route-modal__chip {
  background: #e0f2fe;
}

.pp-release-summary,
.pp-inv-table-wrap,
.pp-inv-footer,
.pp-actionbar {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.pp-panel__header {
  background: #dbe3ed;
  border-bottom-color: #8794a4;
}

.pp-route-toolbar,
.pp-side-meta,
.pp-side-block__title,
.pp-status-legend {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.pp-grid th,
.pp-grid td {
  color: #1e293b;
  border-color: #cbd5e1;
}

.pp-grid th {
  color: #334155;
  background: #e2e8f0;
}

.pp-grid tbody tr:hover,
.pp-grid tbody tr.is-selected {
  background: #eff6ff !important;
}

.pp-grid tbody tr.is-selected {
  outline-color: #7dd3fc;
}

.pp-grid-readonly,
.pp-status-action-modal__label {
  color: #1e293b;
}

.pp-save-route-modal__chip {
  color: #075985;
  border-color: #7dd3fc;
}

.pp-inv-modal {
  background: #fff;
}

.pp-fieldset.pp-inv-filter {
  background: #fff;
}

.pp-grid.pp-grid--plain th,
.pp-grid.pp-grid--plain td,
.pp-grid.pp-grid--plain tbody tr:hover,
.pp-grid.pp-grid--plain tbody tr.is-selected {
  background: transparent !important;
}

@media (max-width: 1400px) {
  .pp-form-grid:not(.pp-form-grid--basic) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
