<script lang="ts" setup>
import { getGrindingConsumptionDefault, newGrindingConsumption, validateGrindingConsumption } from '#/api/mes/hc/grinding-consumption';

import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import type { MesHcProductionRecordApi } from '#/api/mes/hc/production-record';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, h, onMounted, reactive, ref } from 'vue';
import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Alert, Button, Checkbox, DatePicker, Input, InputNumber, message, Modal,
  Pagination, Radio, RadioGroup, Select, Spin, TabPane, Tabs, Tag, Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  confirmGrindingProductionRecord,
  createGrindingProductionRecord,
  deleteGrindingProductionRecord,
  exportGrindingProductionRecord,
  getGrindingProductionRecord,
  getGrindingProductionRecordConsumableDefault,
  getGrindingProductionRecordConsumableSyncPreview,
  getGrindingProductionRecordPage,
  importGrindingProductionRecord,
  syncGrindingProductionRecordConsumableState,
  updateGrindingProductionRecord,
  updateGrindingProductionRecordLife,
} from '#/api/mes/hc/production-record';
import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import ConsumableLedgerSwitchModal from '../../../base/tooling-consumable-ledger/components/ConsumableLedgerSwitchModal.vue';
import '../../../package-fg/shared/cut-round-board.css';
import '../../../package-fg/shared/production-record-ledger.css';

const WAIT_CONFIRM = 'WAIT_CONFIRM';
const CONFIRMED = 'CONFIRMED';
const GUIDE_CLOTH_USE_INCREMENT = 2;
const PASS_OPTIONS = [
  { label: '一次磨皮', value: 'FIRST' },
  { label: '二次磨皮', value: 'SECOND' },
  { label: '三次磨皮', value: 'THIRD' },
  { label: '四次磨皮', value: 'FOURTH' },
];
const PAD_TYPE_OPTIONS = [
  { label: '白垫', value: 'WHITE_PAD' },
  { label: '黑垫', value: 'BLACK_PAD' },
];
const PAD_TAB_OPTIONS = [
  { key: 'ALL', label: '全部记录' },
  { key: 'WHITE_PAD', label: '白垫' },
  { key: 'BLACK_PAD', label: '黑垫' },
  { key: 'UNCLASSIFIED', label: '未归类' },
];
const GRINDING_EQUIPMENT_NAMES = new Set(['CMP白垫磨皮机', 'CMP黑垫磨皮机']);
const LIFE_UPDATE_PERMISSION = 'mes:pp:rough-grinding-production-record:life-update';
const RECORD_QUERY_PERMISSION = 'mes:pp:rough-grinding-production-record:query';
const CONSUMABLE_SYNC_PERMISSION = 'mes:pp:rough-grinding-production-record:consumable-sync';
const userStore = useUserStore();
const { hasAccessByCodes } = useAccess();
const currentUserName = computed(() =>
  userStore.userInfo?.nickname || userStore.userInfo?.username ||
  (userStore.userInfo as any)?.realName || '系统',
);
const canUpdateLife = computed(() => hasAccessByCodes([LIFE_UPDATE_PERMISSION]));
// 同步入口与页面访问权限保持一致，避免新按钮菜单尚未分配角色时被前端误隐藏。
const canSyncConsumableState = computed(() =>
  hasAccessByCodes([CONSUMABLE_SYNC_PERMISSION]) || hasAccessByCodes([RECORD_QUERY_PERMISSION]),
);

const loading = ref(false);
const saving = ref(false);
const exporting = ref(false);
const importing = ref(false);
const lifeEditSaving = ref(false);
const consumableSyncLoading = ref(false);
const consumableSyncSaving = ref(false);
const rows = ref<MesHcProductionRecordApi.GrindingRecordRow[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const activePadTab = ref('ALL');
const selectedIds = ref<number[]>([]);
const importInputRef = ref<HTMLInputElement>();
const formVisible = ref(false);
const lifeEditVisible = ref(false);
const consumableSyncVisible = ref(false);
const lifeEditTitle = ref('修正耗材寿命');
const authVisible = ref(false);
const consumableLedgerModalRef = ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const consumableTarget = ref<'GUIDE_CLOTH' | 'SANDPAPER'>('SANDPAPER');
const consumptionBalances = reactive<{ sandpaper?: number; guideCloth?: number }>({});
const formMode = ref<'create' | 'edit'>('create');
const confirmIds = ref<number[]>([]);
const equipmentOptions = ref<MesHcEquipmentApi.SelectOption[]>([]);
const consumableDefault = ref<MesHcProductionRecordApi.GrindingConsumableDefault>();
const consumableSyncEquipmentId = ref<number>();
const consumableSyncPreview = ref<MesHcProductionRecordApi.GrindingConsumableSyncRespVO>();
const defaultLoading = ref(false);
const query = reactive({
  batchNo: '',
  modelCode: '',
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
});
const form = reactive<MesHcProductionRecordApi.GrindingRecordRow>(defaultForm());
const lifeEditForm = reactive<MesHcProductionRecordApi.GrindingLifeUpdateReqVO>({
  id: 0,
});
type LifeField = 'guideClothLife' | 'replaceReason' | 'sandpaperLife' | 'sandpaperLifeDays';

const availableEquipmentOptions = computed(() => equipmentOptions.value.filter((option) => {
  const optionPadType = String(option.applicablePadType || '').trim().toUpperCase();
  return !form.padType || optionPadType === 'COMMON' || optionPadType === form.padType;
}));

function openConsumableLedger(target: 'GUIDE_CLOTH' | 'SANDPAPER') {
  consumableTarget.value = target;
  consumableLedgerModalRef.value?.open({
    consumableType: target,
    processCode: 'ROUGH_GRINDING',
    selectionMode: 'DIRECT',
    title: target === 'SANDPAPER' ? '选择磨皮砂纸领用台账' : '选择磨皮导布领用台账',
  });
}

function handleConsumableSelected(row: MesHcToolingConsumableLedgerApi.Ledger) {
  const prefix = consumableTarget.value === 'SANDPAPER' ? 'sandpaper' : 'guideCloth';
  form[`${prefix}BatchNo`] = row.batchNo || '';
  // 未选择更换时仍可沿用历史批号维护，不产生实物消耗。
  if (!form[`${prefix}Changed`]) return;
  if (!form.consumption) form.consumption = newGrindingConsumption();
  form.consumption[`${prefix}LedgerId`] = row.id;
  const defaults = getGrindingConsumptionDefault(consumableTarget.value, row);
  form.consumption[`${prefix}Qty`] = defaults.qty;
  if (defaults.warning) message.warning(defaults.warning);
  form.consumption[`${prefix}Unit`] = row.uomName || row.uomCode;
  consumptionBalances[prefix] = row.balanceQty;
}

async function loadEquipmentOptions() {
  if (equipmentOptions.value.length > 0) return;
  const options = await getEquipmentSelectOptions();
  equipmentOptions.value = options.filter((option) =>
    GRINDING_EQUIPMENT_NAMES.has(option.name || option.label),
  );
}

async function openConsumableSync() {
  if (!canSyncConsumableState.value) return;
  await loadEquipmentOptions();
  consumableSyncEquipmentId.value = undefined;
  consumableSyncPreview.value = undefined;
  consumableSyncVisible.value = true;
}

async function loadConsumableSyncPreview() {
  const equipmentId = consumableSyncEquipmentId.value;
  consumableSyncPreview.value = undefined;
  if (!equipmentId) return;
  consumableSyncLoading.value = true;
  try {
    const preview = await getGrindingProductionRecordConsumableSyncPreview(equipmentId);
    if (consumableSyncEquipmentId.value === equipmentId) {
      consumableSyncPreview.value = preview;
    }
  } finally {
    consumableSyncLoading.value = false;
  }
}

async function saveConsumableSync() {
  const equipmentId = consumableSyncEquipmentId.value;
  if (!equipmentId) {
    message.warning('请选择需要同步的设备');
    return;
  }
  if (!consumableSyncPreview.value) {
    message.warning('请先获取最近已确认记录');
    return;
  }
  consumableSyncSaving.value = true;
  try {
    const resp = await syncGrindingProductionRecordConsumableState({ equipmentId });
    message.success(resp.message || '已同步至磨皮报工耗材状态');
    consumableSyncVisible.value = false;
    consumableSyncPreview.value = undefined;
  } finally {
    consumableSyncSaving.value = false;
  }
}

function clearConsumableDefaultFields() {
  if (!form.sandpaperChanged) {
    form.sandpaperLife = undefined;
    form.sandpaperLifeDays = undefined;
    form.sandpaperBatchNo = undefined;
  }
  if (!form.guideClothChanged) {
    form.guideClothLife = undefined;
    form.guideClothBatchNo = undefined;
  }
}

async function refreshConsumableDefault() {
  if (formMode.value !== 'create' || !form.equipmentId) return;
  const completionTime = dayjs(form.recordTime);
  if (!completionTime.isValid()) return;
  const normalizedCompletionTime = completionTime.format('YYYY-MM-DD HH:mm:ss');
  const equipmentId = form.equipmentId;
  defaultLoading.value = true;
  try {
    const snapshot = await getGrindingProductionRecordConsumableDefault({
      equipmentId,
      completionTime: normalizedCompletionTime,
    });
    if (formMode.value !== 'create' || form.equipmentId !== equipmentId || form.recordTime !== normalizedCompletionTime) return;
    consumableDefault.value = snapshot.sourceRecordId ? snapshot : undefined;
    if (!snapshot.sourceRecordId) {
      clearConsumableDefaultFields();
      if (!form.guideClothChanged) {
        form.guideClothLife = snapshot.guideClothLife ?? GUIDE_CLOTH_USE_INCREMENT;
      }
      return;
    }
    if (!form.sandpaperChanged) {
      form.sandpaperLife = snapshot.sandpaperLife;
      form.sandpaperLifeDays = snapshot.sandpaperLifeDays;
      form.sandpaperBatchNo = snapshot.sandpaperBatchNo;
    }
    if (!form.guideClothChanged) {
      form.guideClothLife = snapshot.guideClothLife ?? GUIDE_CLOTH_USE_INCREMENT;
      form.guideClothBatchNo = snapshot.guideClothBatchNo;
    }
  } finally { defaultLoading.value = false; }
}

function defaultSourceMessage() {
  const source = consumableDefault.value;
  if (!source?.sourceRecordId) return '';
  const state = source.sourceStatus === WAIT_CONFIRM ? '待确认' : '已确认';
  return `已参考设备最近完工记录（${source.sourceCompletionTime || '-'}，${state}）。`;
}

const productionSummaryRows = computed(() => {
  const reportedSources = new Set<string>();
  return rows.value.filter((row) => {
    const sourceKey = productionSourceKey(row);
    if (!sourceKey) return true;
    if (reportedSources.has(sourceKey)) return false;
    reportedSources.add(sourceKey);
    return true;
  });
});
const currentInput = computed(() => productionSummaryRows.value.reduce((sum, row) => sum + Number(row.inputLength || 0), 0));
const currentOutput = computed(() => productionSummaryRows.value.reduce((sum, row) => sum + Number(row.outputLength || 0), 0));
const pendingRows = computed(() => rows.value.filter((row) => row.status !== CONFIRMED));
const selectedPendingIds = computed(() => {
  const pending = new Set(pendingRows.value.map((row) => row.id).filter(Boolean) as number[]);
  return selectedIds.value.filter((id) => pending.has(id));
});
const allSelected = computed(() => pendingRows.value.length > 0 && selectedPendingIds.value.length === pendingRows.value.length);
const someSelected = computed(() => selectedPendingIds.value.length > 0 && !allSelected.value);

function defaultForm(): MesHcProductionRecordApi.GrindingRecordRow {
  return { passType: 'FIRST', recordTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    recorderName: currentUserName.value, reportDate: dayjs().format('YYYY-MM-DD'),
    sandpaperChanged: false, guideClothChanged: false };
}
function filters(extra: Record<string, any> = {}) {
  return {
    batchNo: query.batchNo || undefined,
    modelCode: query.modelCode || undefined,
    padType: query.padType || undefined,
    ...extra,
  };
}
function pageParams(extra: Record<string, any> = {}) {
  return { ...filters(), pageNo: pageNo.value, pageSize: pageSize.value, ...extra };
}
async function fetchData() {
  loading.value = true;
  try {
    const page = await getGrindingProductionRecordPage(pageParams());
    rows.value = page.list || [];
    total.value = Number(page.total || 0);
    selectedIds.value = selectedIds.value.filter((id) => rows.value.some((row) => row.id === id && row.status !== CONFIRMED));
  } finally { loading.value = false; }
}
function search() { pageNo.value = 1; void fetchData(); }
function reset() {
  query.batchNo = '';
  query.modelCode = '';
  query.padType = '';
  activePadTab.value = 'ALL';
  search();
}
function handlePadTabChange(tab: string | number) {
  const value = String(tab);
  activePadTab.value = value;
  query.padType = value === 'ALL'
    ? ''
    : value as 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
  search();
}
function padTypeLabel(padType?: string) {
  if (padType === 'BLACK_PAD') return '黑垫';
  if (padType === 'WHITE_PAD') return '白垫';
  return '未归类';
}
function padTypeColor(padType?: string) {
  if (padType === 'BLACK_PAD') return 'default';
  if (padType === 'WHITE_PAD') return 'blue';
  return 'warning';
}
function handlePadTypeChange() {
  if (form.equipmentId && !availableEquipmentOptions.value.some((item) => item.value === form.equipmentId)) {
    form.equipmentId = undefined;
  }
}
function changePage(current: number, size: number) { pageNo.value = current; pageSize.value = size; void fetchData(); }
function text(value?: null | number | string, fallback = '-') { return String(value ?? '').trim() || fallback; }
function short(value?: string, length = 18) { const valueText = text(value, ''); return valueText.length > length ? `${valueText.slice(0, length)}...` : valueText || '-'; }
function number(value?: number) { return value === null || value === undefined ? '-' : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 }); }
function status(statusValue?: string) { return statusValue === CONFIRMED ? { color: 'success', label: '已确认' } : { color: 'processing', label: '待确认' }; }
function productionSourceKey(row: MesHcProductionRecordApi.GrindingRecordRow) {
  if (row.sourceDetailId) {
    const sourceBizType = row.sourceBizType || (row.passType === 'SECOND' ? 'SECOND' : 'FIRST_ORIGINAL');
    return `${sourceBizType}:${row.sourceDetailId}`;
  }
  return row.sourceType === 'MANUAL' && row.manualSplitGroupNo
    ? `MANUAL:${row.manualSplitGroupNo}` : '';
}
function recordRoleLabel(row: MesHcProductionRecordApi.GrindingRecordRow) {
  const role = row.recordRole || row.sourceBizType;
  if (role === 'FIRST_ALLOCATION' || role === 'FIRST_ORIGINAL' || row.passType === 'FIRST') return '一磨';
  if (role === 'SECOND' || row.passType === 'SECOND') return '二磨';
  if (row.passType === 'THIRD') return '三磨';
  if (row.passType === 'FOURTH') return '四磨';
  return '手工记录';
}
function isReportGenerated(row: MesHcProductionRecordApi.GrindingRecordRow) {
  return row.sourceType === 'REPORT_AUTO' || row.sourceType === 'REPORT_INIT';
}
function isManualSandpaperSplit(row: MesHcProductionRecordApi.GrindingRecordRow) {
  return row.sourceType === 'MANUAL' && Boolean(row.manualSplitGroupNo);
}
function sandpaperSplitRole(row: MesHcProductionRecordApi.GrindingRecordRow) {
  const sourceKey = productionSourceKey(row);
  if (!sourceKey) return '';
  const hasSibling = rows.value.some((candidate) => candidate.id !== row.id
    && productionSourceKey(candidate) === sourceKey);
  if (!hasSibling) return '';
  return row.sourceSegmentNo === 2 ? '新砂纸' : '旧砂纸';
}
function lifeFieldLabel(field: LifeField) {
  if (field === 'sandpaperLife') return '砂纸累计寿命(m)';
  if (field === 'sandpaperLifeDays') return '砂纸累计天数';
  if (field === 'replaceReason') return '更换原因';
  return '导布累计寿命(次)';
}
function toOptionalNumber(value?: null | number) {
  if (value === null || value === undefined) return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}
function toOptionalText(value?: null | string) {
  return value === null || value === undefined ? undefined : value.trim();
}
function openLifeEdit(row: MesHcProductionRecordApi.GrindingRecordRow, field: LifeField) {
  if (!canUpdateLife.value) return;
  if (isManualSandpaperSplit(row)) {
    message.warning('砂纸更换生成的研发双记录不能单独修正耗材寿命，请整组删除后重新新增');
    return;
  }
  if (!row.id) {
    message.warning('磨皮生产记录缺少ID，无法修正');
    return;
  }
  lifeEditTitle.value = `修正${lifeFieldLabel(field)}`;
  lifeEditForm.id = row.id;
  lifeEditForm.sandpaperLife = toOptionalNumber(row.sandpaperLife);
  lifeEditForm.sandpaperLifeDays = toOptionalNumber(row.sandpaperLifeDays);
  lifeEditForm.guideClothLife = toOptionalNumber(row.guideClothLife);
  lifeEditForm.replaceReason = row.replaceReason ?? undefined;
  lifeEditVisible.value = true;
}
async function saveLifeEdit() {
  const payload: MesHcProductionRecordApi.GrindingLifeUpdateReqVO = {
    id: lifeEditForm.id,
    sandpaperLife: toOptionalNumber(lifeEditForm.sandpaperLife),
    sandpaperLifeDays: toOptionalNumber(lifeEditForm.sandpaperLifeDays),
    guideClothLife: toOptionalNumber(lifeEditForm.guideClothLife),
    replaceReason: toOptionalText(lifeEditForm.replaceReason),
  };
  const values = [payload.sandpaperLife, payload.sandpaperLifeDays, payload.guideClothLife];
  if (!payload.id) {
    message.warning('磨皮生产记录缺少ID，无法修正');
    return;
  }
  if (values.every((value) => value === undefined) && payload.replaceReason === undefined) {
    message.warning('至少填写一项耗材寿命或更换原因修正值');
    return;
  }
  if (values.some((value) => value !== undefined && value < 0)) {
    message.warning('耗材寿命修正值不能为负数');
    return;
  }
  lifeEditSaving.value = true;
  try {
    await updateGrindingProductionRecordLife(payload);
    message.success('耗材信息已修正');
    lifeEditVisible.value = false;
    await fetchData();
  } finally { lifeEditSaving.value = false; }
}
function selected(id?: number) { return !!id && selectedIds.value.includes(id); }
function toggleRow(row: MesHcProductionRecordApi.GrindingRecordRow, event: Event) {
  if (!row.id || row.status === CONFIRMED) return;
  const checked = Boolean((event.target as HTMLInputElement | null)?.checked);
  selectedIds.value = checked ? [...new Set([...selectedIds.value, row.id])] : selectedIds.value.filter((id) => id !== row.id);
}
function toggleAll(event: Event) {
  const checked = Boolean((event.target as HTMLInputElement | null)?.checked);
  selectedIds.value = checked ? pendingRows.value.map((row) => row.id!).filter(Boolean) : [];
}
function resetForm(value?: MesHcProductionRecordApi.GrindingRecordRow) {
  Object.keys(form).forEach((key) => delete (form as any)[key]);
  Object.assign(form, value || defaultForm());
  if (!value) form.consumption = newGrindingConsumption();
  consumptionBalances.sandpaper = undefined;
  consumptionBalances.guideCloth = undefined;
}
function positiveNumber(value?: null | number | string) {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : 0;
}
function applySandpaperReplacementDefaults(force = false) {
  if (formMode.value !== 'create' || !form.sandpaperChanged) return;
  const outputLength = positiveNumber(form.outputLength);
  const currentLife = positiveNumber(form.sandpaperLife);
  if (force || currentLife === 0) {
    form.sandpaperLife = outputLength;
  }
  if (outputLength > 0 && Number(form.sandpaperLifeDays ?? 0) < 1) {
    form.sandpaperLifeDays = 1;
  } else if (force && outputLength === 0) {
    form.sandpaperLifeDays = 0;
  }
}
function handleOutputLengthChange() {
  applySandpaperReplacementDefaults();
}
function handleManualConsumableReplaceChange(target: 'GUIDE_CLOTH' | 'SANDPAPER') {
  if (!form.consumption) form.consumption = newGrindingConsumption();
  const prefix = target === 'SANDPAPER' ? 'sandpaper' : 'guideCloth';
  form.consumption[`${prefix}LedgerId`] = undefined;
  form.consumption[`${prefix}Qty`] = undefined;
  form.consumption[`${prefix}Unit`] = undefined;
  consumptionBalances[prefix] = undefined;
  if (target === 'SANDPAPER') {
    if (form.sandpaperChanged) {
      applySandpaperReplacementDefaults(true);
      form.sandpaperBatchNo = '';
      form.sandpaperReplaceReason = '';
      return;
    }
  } else if (form.guideClothChanged) {
    form.guideClothLife = GUIDE_CLOTH_USE_INCREMENT;
    form.guideClothBatchNo = '';
    form.guideClothReplaceReason = '';
    return;
  }
  void refreshConsumableDefault();
}
async function create() {
  formMode.value = 'create';
  consumableDefault.value = undefined;
  resetForm();
  try { await loadEquipmentOptions(); }
  catch { message.error('设备选项加载失败，请重试'); }
  formVisible.value = true;
}
async function edit(row: MesHcProductionRecordApi.GrindingRecordRow) {
  if (isReportGenerated(row)) { message.warning('自动生成记录请在磨皮报工看板修订'); return; }
  if (isManualSandpaperSplit(row)) { message.warning('砂纸更换生成的研发双记录请整组删除后重新新增'); return; }
  if (!row.id || row.status === CONFIRMED) { message.warning('已确认记录不能修改'); return; }
  loading.value = true;
  try { formMode.value = 'edit'; resetForm(await getGrindingProductionRecord(row.id)); formVisible.value = true; }
  finally { loading.value = false; }
}
function validForm() {
  const required: Array<[keyof MesHcProductionRecordApi.GrindingRecordRow, string]> = [
    ['modelCode', '研发型号'], ['padType', '类型'], ['materialCode', '料号'], ['batchNo', '批号'],
    ['passType', '磨皮次数'], ['recorderName', '记录人'], ['recordTime', '完工日期'],
  ];
  if (formMode.value === 'create' && !form.equipmentId) { message.warning('请选择设备'); return false; }
  for (const [field, label] of required) if (!String(form[field] ?? '').trim()) { message.warning(`请填写${label}`); return false; }
  for (const field of ['inputLength', 'outputLength', 'sandpaperLife', 'sandpaperLifeDays', 'guideClothLife'] as const)
    if (Number(form[field] ?? 0) < 0) { message.warning('投入、产出和耗材寿命不能为负数'); return false; }
  if (formMode.value === 'create' && form.sandpaperChanged
    && (!String(form.sandpaperBatchNo || '').trim() || !String(form.sandpaperReplaceReason || '').trim())) {
    message.warning('更换砂纸时请填写新批号和更换原因');
    return false;
  }
  if (formMode.value === 'create' && form.guideClothChanged
    && (!String(form.guideClothBatchNo || '').trim() || !String(form.guideClothReplaceReason || '').trim())) {
    message.warning('更换导布时请填写新批号和更换原因');
    return false;
  }
  return true;
}
function normalizeCompletionTime() {
  const completionTime = dayjs(form.recordTime);
  if (!completionTime.isValid()) { message.warning('完工日期格式应为yyyy-MM-dd HH:mm:ss'); return false; }
  form.recordTime = completionTime.format('YYYY-MM-DD HH:mm:ss');
  form.reportDate = completionTime.format('YYYY-MM-DD');
  return true;
}
async function save() {
  if (saving.value) return;
  if (form.consumption) {
    const error = validateGrindingConsumption(form.consumption, !!form.sandpaperChanged, !!form.guideClothChanged);
    if (error) { message.warning(error); return; }
  }
  applySandpaperReplacementDefaults();
  if (!validForm() || !normalizeCompletionTime()) return;
  saving.value = true;
  try {
    await (form.id ? updateGrindingProductionRecord(form) : createGrindingProductionRecord(form));
    message.success('保存成功'); formVisible.value = false; await fetchData();
  } finally { saving.value = false; }
}
function remove(row: MesHcProductionRecordApi.GrindingRecordRow) {
  if (isReportGenerated(row)) { message.warning('自动生成记录请在磨皮报工看板删除来源报工'); return; }
  if (!row.id || row.status === CONFIRMED) { message.warning('已确认记录不能删除'); return; }
  const splitRecord = isManualSandpaperSplit(row);
  Modal.confirm({ title: splitRecord ? '确认删除本次砂纸更换生成的两条研发记录吗？' : '确认物理删除当前待确认记录吗？',
    content: splitRecord ? '将同时物理删除旧砂纸和新砂纸两条记录，删除后不可恢复。' : '物理删除后不可恢复。',
    okButtonProps: { danger: true }, okText: '确认删除', onOk: async () => {
      await deleteGrindingProductionRecord(row.id!); message.success('删除成功'); await fetchData();
    } });
}
function openConfirm() {
  const ids = selectedPendingIds.value.length ? selectedPendingIds.value : pendingRows.value.map((row) => row.id!).filter(Boolean);
  if (!ids.length) { message.warning('当前页没有待确认记录'); return; }
  confirmIds.value = ids; authVisible.value = true;
}
async function authSuccess(user: any) {
  const name = user?.empName || user?.nickname || user?.username || user?.empNo || currentUserName.value;
  const count = await confirmGrindingProductionRecord({ confirmerName: name, ids: confirmIds.value });
  message.success(`认证通过，已确认 ${count} 条磨皮生产记录`); selectedIds.value = []; confirmIds.value = []; await fetchData();
}
function confirmAsync(options: Parameters<typeof Modal.confirm>[0]) {
  return new Promise<boolean>((resolve) => Modal.confirm({ ...options, onCancel: () => resolve(false), onOk: () => resolve(true) }));
}
async function exportExcel() {
  exporting.value = true;
  try {
    const confirmed = await getGrindingProductionRecordPage(pageParams({ pageNo: 1, pageSize: 1, status: CONFIRMED }));
    if (Number(confirmed.total || 0) > 0 && !await confirmAsync({ title: '当前筛选范围包含已确认记录',
      content: '已确认记录导出后再次导入不会覆盖原数据。', okText: '继续导出', cancelText: '取消' })) return;
    const data = await exportGrindingProductionRecord(filters());
    downloadFileFromBlobPart({ fileName: '磨皮生产记录表.xlsx', source: data });
    message.success('磨皮生产记录表已导出');
  } finally { exporting.value = false; }
}
function importClick() { importInputRef.value?.click(); }
function importFailures(resp: MesHcProductionRecordApi.ImportRespVO) {
  const failures = resp.failures || [];
  Modal.warning({ title: '磨皮生产记录导入校验未通过', width: 760,
    content: h('pre', { style: 'white-space:pre-wrap;max-height:360px;overflow:auto;font-size:12px' }, failures.slice(0, 20).join('\n')) });
}
async function importChange(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''; if (!file) return;
  importing.value = true; const hide = message.loading({ content: '正在导入磨皮生产记录...', duration: 0 });
  try { const resp = await importGrindingProductionRecord(file); if (resp.failureCount) { importFailures(resp); return; }
    message.success((resp.messages || []).join('；')); await fetchData(); }
  finally { importing.value = false; hide(); }
}
onMounted(fetchData);
</script>

<template>
  <Page auto-content-height class="production-record-page" content-class="production-record-content">
    <div class="package-fg-console production-record-report">
      <input ref="importInputRef" accept=".xlsx,.xls" class="hidden" type="file" @change="importChange" />
      <section class="prototype-banner production-record-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:clipboard-list" /></span>
        <div class="console-title-block">
          <div class="console-title-row"><h2 class="console-title-text">磨皮生产记录表</h2><Tag color="processing" class="console-title-tag">生产报表</Tag></div>
          <div class="console-meta-row">
            <span class="console-meta-item"><span class="console-meta-label">当前页</span><span class="console-meta-value">{{ rows.length }}</span><span class="console-meta-sub">行</span></span>
            <span class="console-meta-item"><span class="console-meta-label">总数</span><span class="console-meta-value">{{ total }}</span><span class="console-meta-sub">行</span></span>
            <span class="console-meta-item"><span class="console-meta-label">投入</span><span class="console-meta-value">{{ number(currentInput) }}</span><span class="console-meta-sub">m</span></span>
            <span class="console-meta-item"><span class="console-meta-label">产出</span><span class="console-meta-value">{{ number(currentOutput) }}</span><span class="console-meta-sub">m</span></span>
          </div>
        </div>
        <div class="console-action-group production-record-actions">
          <button class="action-tile" type="button" @click="create"><IconifyIcon icon="lucide:plus" /><span>新增</span></button>
          <button class="action-tile" type="button" @click="fetchData"><IconifyIcon icon="lucide:refresh-cw" /><span>刷新</span></button>
          <button v-if="canSyncConsumableState" class="action-tile" type="button" @click="openConsumableSync"><IconifyIcon icon="lucide:send" /><span>同步</span></button>
          <button class="action-tile" type="button" :disabled="exporting" @click="exportExcel"><IconifyIcon icon="lucide:file-spreadsheet" /><span>{{ exporting ? '导出中' : '导出' }}</span></button>
          <button class="action-tile" type="button" :disabled="importing" @click="importClick"><IconifyIcon icon="lucide:upload" /><span>{{ importing ? '导入中' : '导入' }}</span></button>
          <button class="action-tile success" type="button" :disabled="!pendingRows.length" @click="openConfirm"><IconifyIcon icon="lucide:badge-check" /><span>一键确认</span></button>
        </div>
      </section>

      <Tabs :active-key="activePadTab" class="production-record-pad-tabs" @change="handlePadTabChange">
        <TabPane v-for="tab in PAD_TAB_OPTIONS" :key="tab.key" :tab="tab.label" />
      </Tabs>

      <section class="package-fg-filter-bar query-panel"><div class="query-grid">
        <label>型号</label><Input v-model:value="query.modelCode" allow-clear placeholder="输入型号" @press-enter="search" />
        <label>批号</label><Input v-model:value="query.batchNo" allow-clear placeholder="输入母批或生产批号" @press-enter="search" />
        <Button type="primary" @click="search"><template #icon><IconifyIcon icon="lucide:search" /></template>查询</Button>
        <Button @click="reset"><template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>重置</Button>
      </div></section>

      <div class="record-body"><Spin :spinning="loading"><div class="table-scroll"><table class="record-table">
        <thead><tr>
          <th class="check"><Checkbox :checked="allSelected" :disabled="!pendingRows.length" :indeterminate="someSelected" @change="toggleAll" /></th><th class="status">状态</th>
          <th class="completion-time">完工日期</th><th>型号</th><th>类型</th><th>料号</th><th>生产批号</th><th>记录角色</th><th>投入(m)</th><th>产出(m)</th><th>磨皮次数</th>
          <th>砂纸累计寿命(m)</th><th>砂纸累计天数</th><th>砂纸批号</th><th>导布累计寿命(次)</th><th>导布批号</th><th>更换原因</th>
          <th>记录人</th><th>确认人</th><th>备注</th><th class="actions">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id" :class="{ confirmed: row.status === CONFIRMED }">
            <td class="check"><Checkbox :checked="selected(row.id)" :disabled="row.status === CONFIRMED" @change="(event) => toggleRow(row, event)" /></td>
            <td class="status"><Tag :color="status(row.status).color">{{ status(row.status).label }}</Tag></td>
            <td class="completion-time">{{ text(row.recordTime) }}</td><td>{{ text(row.modelCode) }}</td><td class="center"><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td><td>{{ text(row.materialCode) }}</td>
            <td><Tooltip :title="row.batchNo"><span>{{ short(row.batchNo) }}</span></Tooltip></td>
            <td class="center">{{ recordRoleLabel(row) }}</td>
            <td class="num">{{ number(row.inputLength) }}</td><td class="num">{{ number(row.outputLength) }}</td><td class="center">{{ text(row.passName) }}</td>
            <td class="num">
              <Tooltip v-if="canUpdateLife" :title="isManualSandpaperSplit(row) ? '砂纸更换双记录不可单独修正' : '点击修正砂纸累计寿命'">
                <button class="life-cell-button" type="button" @click="openLifeEdit(row, 'sandpaperLife')">{{ number(row.sandpaperLife) }}</button>
              </Tooltip>
              <span v-else>{{ number(row.sandpaperLife) }}</span>
            </td>
            <td class="num">
              <Tooltip v-if="canUpdateLife" :title="isManualSandpaperSplit(row) ? '砂纸更换双记录不可单独修正' : '点击修正砂纸累计天数'">
                <button class="life-cell-button" type="button" @click="openLifeEdit(row, 'sandpaperLifeDays')">{{ text(row.sandpaperLifeDays) }}</button>
              </Tooltip>
              <span v-else>{{ text(row.sandpaperLifeDays) }}</span>
            </td>
            <td><span>{{ text(row.sandpaperBatchNo) }}</span><div v-if="row.consumption?.sandpaperQty">消耗 {{ row.consumption.sandpaperQty }} {{ row.consumption.sandpaperUnit }}</div><Tag v-if="sandpaperSplitRole(row)" :color="row.sourceSegmentNo === 2 ? 'green' : 'orange'">{{ sandpaperSplitRole(row) }}</Tag></td>
            <td class="num">
              <Tooltip v-if="canUpdateLife" :title="isManualSandpaperSplit(row) ? '砂纸更换双记录不可单独修正' : '点击修正导布累计寿命'">
                <button class="life-cell-button" type="button" @click="openLifeEdit(row, 'guideClothLife')">{{ text(row.guideClothLife) }}</button>
              </Tooltip>
              <span v-else>{{ text(row.guideClothLife) }}</span>
            </td>
            <td>{{ text(row.guideClothBatchNo) }}<div v-if="row.consumption?.guideClothQty">消耗 {{ row.consumption.guideClothQty }} {{ row.consumption.guideClothUnit }}</div></td>
            <td>
              <Tooltip v-if="canUpdateLife" :title="isManualSandpaperSplit(row) ? '砂纸更换双记录不可单独修正' : '点击修正更换原因'">
                <button class="life-cell-button text-left" type="button" @click="openLifeEdit(row, 'replaceReason')">{{ short(row.replaceReason, 20) }}</button>
              </Tooltip>
              <Tooltip v-else :title="row.replaceReason"><span>{{ short(row.replaceReason, 20) }}</span></Tooltip>
            </td>
            <td>{{ text(row.recorderName) }}</td><td>{{ text(row.confirmerName) }}</td>
            <td><Tooltip :title="row.remark"><span>{{ short(row.remark, 20) }}</span></Tooltip></td>
            <td class="actions"><Button size="small" type="link" :disabled="row.status === CONFIRMED || isReportGenerated(row) || isManualSandpaperSplit(row)" @click="edit(row)">编辑</Button><Button size="small" type="link" danger :disabled="row.status === CONFIRMED || isReportGenerated(row)" @click="remove(row)">删除</Button></td>
          </tr>
          <tr v-if="!rows.length"><td class="empty" colspan="21">暂无磨皮生产记录</td></tr>
        </tbody>
      </table></div></Spin></div>

      <footer class="pagination"><span>默认按完工日期倒序</span>
        <Pagination v-model:current="pageNo" v-model:page-size="pageSize" :total="total" :show-total="(count) => `共 ${count} 条`" show-size-changer @change="changePage" @show-size-change="changePage" />
      </footer>

      <Modal v-model:open="formVisible" :confirm-loading="saving" :title="formMode === 'create' ? '新增磨皮研发记录' : '维护磨皮研发记录'" :mask-closable="false" cancel-text="取消" centered ok-text="保存" width="1000px" wrap-class-name="production-record-ledger-modal" @ok="save">
        <div class="production-record-ledger-form">
          <label>完工日期</label><DatePicker v-model:value="form.recordTime" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" @change="() => void refreshConsumableDefault()" />
          <label>类型</label><Select v-model:value="form.padType" :options="PAD_TYPE_OPTIONS" placeholder="请选择黑垫或白垫" @change="handlePadTypeChange" />
          <template v-if="formMode === 'create'">
            <label>设备</label><Select v-model:value="form.equipmentId" :disabled="!form.padType" :loading="defaultLoading" :options="availableEquipmentOptions" placeholder="请先选择类型" @change="() => void refreshConsumableDefault()" />
          </template>
          <div v-if="consumableDefault?.sourceRecordId" class="consumable-default-tip">
            <Alert :message="defaultSourceMessage()" :type="consumableDefault.sourceStatus === WAIT_CONFIRM ? 'warning' : 'info'" show-icon />
          </div>
          <label>研发型号</label><Input v-model:value="form.modelCode" placeholder="请输入研发样型号" />
          <label>料号</label><Input v-model:value="form.materialCode" />
          <label>批号</label><Input v-model:value="form.batchNo" />
          <label>投入米数(m)</label><InputNumber v-model:value="form.inputLength" :min="0" :precision="3" />
          <label>产出米数(m)</label><InputNumber v-model:value="form.outputLength" :min="0" :precision="3" @change="handleOutputLengthChange" />
          <label>磨皮次数</label><Select v-model:value="form.passType" :options="PASS_OPTIONS" />
          <template v-if="formMode === 'create'">
            <label>是否更换砂纸</label>
            <RadioGroup v-model:value="form.sandpaperChanged" @change="handleManualConsumableReplaceChange('SANDPAPER')">
              <Radio :value="false">否</Radio><Radio :value="true">是</Radio>
            </RadioGroup>
          </template>
          <label>砂纸累计寿命(m)</label><InputNumber v-model:value="form.sandpaperLife" :min="0" :precision="3" :placeholder="formMode === 'create' && form.sandpaperChanged ? '默认按本条产出米数累计' : undefined" />
          <label>砂纸累计天数</label><InputNumber v-model:value="form.sandpaperLifeDays" :min="0" :precision="0" :placeholder="formMode === 'create' && form.sandpaperChanged ? '首次使用默认1天' : undefined" />
          <label>{{ formMode === 'create' && form.sandpaperChanged ? '新砂纸批号' : '砂纸批号' }}</label>
          <div class="ledger-picker-control">
            <Input v-model:value="form.sandpaperBatchNo" :readonly="!!form.consumption?.sandpaperLedgerId" />
            <Tooltip title="从磨皮砂纸边库耗材领用台账选择">
              <Button class="ledger-picker-button" @click="openConsumableLedger('SANDPAPER')"><IconifyIcon icon="lucide:database" /></Button>
            </Tooltip>
          </div>
          <template v-if="form.sandpaperChanged && form.consumption">
            <label>砂纸本次消耗量（{{ form.consumption.sandpaperUnit || '台账单位' }}）</label>
            <div><InputNumber v-model:value="form.consumption.sandpaperQty" :min="0.001" :precision="3" /><span class="ml-2">可用余额：{{ consumptionBalances.sandpaper ?? '-' }}</span></div>
          </template>
          <template v-if="formMode === 'create' && form.sandpaperChanged">
            <label>砂纸更换原因</label><Input v-model:value="form.sandpaperReplaceReason" />
          </template>
          <template v-if="formMode === 'create'">
            <label>是否更换导布</label>
            <RadioGroup v-model:value="form.guideClothChanged" @change="handleManualConsumableReplaceChange('GUIDE_CLOTH')">
              <Radio :value="false">否</Radio><Radio :value="true">是</Radio>
            </RadioGroup>
          </template>
          <label>导布累计寿命(次)</label><InputNumber v-model:value="form.guideClothLife" :min="0" :precision="0" :placeholder="formMode === 'create' && form.guideClothChanged ? '更换后默认2' : undefined" />
          <label>{{ formMode === 'create' && form.guideClothChanged ? '新导布批号' : '导布批号' }}</label>
          <div class="ledger-picker-control">
            <Input v-model:value="form.guideClothBatchNo" :readonly="!!form.consumption?.guideClothLedgerId" />
            <Tooltip title="从磨皮导布边库耗材领用台账选择">
              <Button class="ledger-picker-button" @click="openConsumableLedger('GUIDE_CLOTH')"><IconifyIcon icon="lucide:database" /></Button>
            </Tooltip>
          </div>
          <template v-if="form.guideClothChanged && form.consumption">
            <label>导布本次消耗量（{{ form.consumption.guideClothUnit || '台账单位' }}）</label>
            <div><InputNumber v-model:value="form.consumption.guideClothQty" :min="0.001" :precision="3" /><span class="ml-2">可用余额：{{ consumptionBalances.guideCloth ?? '-' }}</span></div>
          </template>
          <template v-if="formMode === 'create' && form.guideClothChanged">
            <label>导布更换原因</label><Input v-model:value="form.guideClothReplaceReason" />
          </template>
          <label>记录人</label><Input v-model:value="form.recorderName" />
          <span></span><span></span>
          <template v-if="formMode !== 'create' || (!form.sandpaperChanged && !form.guideClothChanged)">
            <label>更换原因</label><Input.TextArea v-model:value="form.replaceReason" :rows="3" />
          </template>
          <label>备注</label><Input.TextArea v-model:value="form.remark" :rows="3" />
        </div>
      </Modal>
      <Modal v-model:open="lifeEditVisible" :confirm-loading="lifeEditSaving" :title="lifeEditTitle" :mask-closable="false" cancel-text="取消" centered ok-text="保存" width="520px" wrap-class-name="production-record-ledger-modal" @ok="saveLifeEdit">
        <div class="production-record-ledger-form life-correction-form">
          <label>砂纸累计寿命(m)</label><InputNumber v-model:value="lifeEditForm.sandpaperLife" :min="0" :precision="3" />
          <label>砂纸累计天数</label><InputNumber v-model:value="lifeEditForm.sandpaperLifeDays" :min="0" :precision="0" />
          <label>导布累计寿命(次)</label><InputNumber v-model:value="lifeEditForm.guideClothLife" :min="0" :precision="0" />
          <label>更换原因</label><Input.TextArea v-model:value="lifeEditForm.replaceReason" :maxlength="512" :rows="3" show-count />
        </div>
      </Modal>
      <Modal v-model:open="consumableSyncVisible" :confirm-loading="consumableSyncSaving" :title="'同步最新已确认记录至报工耗材'" :mask-closable="false" cancel-text="取消" centered ok-text="同步" width="560px" wrap-class-name="production-record-ledger-modal" @ok="saveConsumableSync">
        <Spin :spinning="consumableSyncLoading">
          <div class="production-record-ledger-form consumable-sync-form">
            <label>设备</label>
            <Select v-model:value="consumableSyncEquipmentId" :options="equipmentOptions" allow-clear placeholder="请选择CMP软垫磨皮设备" @change="() => void loadConsumableSyncPreview()" />
            <template v-if="consumableSyncPreview">
              <label>来源记录</label>
              <span>#{{ consumableSyncPreview.sourceRecordId || '-' }} · {{ text(consumableSyncPreview.sourceRecordTime) }}</span>
              <label>砂纸累计寿命(m)</label>
              <strong>{{ number(consumableSyncPreview.sandpaperLife) }}</strong>
              <label>砂纸累计天数</label>
              <strong>{{ text(consumableSyncPreview.sandpaperLifeDays) }} 天</strong>
              <label>导布累计寿命(次)</label>
              <strong>{{ text(consumableSyncPreview.guideClothLife) }}</strong>
              <div class="consumable-default-tip">
                <Alert message="仅同步所选设备最近一条已确认记录；砂纸累计天数统一按当天为第1天计算。若报工耗材已有更晚事件或批号冲突，系统会阻止覆盖。" type="info" show-icon />
              </div>
            </template>
          </div>
        </Spin>
      </Modal>
      <ConsumableLedgerSwitchModal ref="consumableLedgerModalRef" @selected="handleConsumableSelected" />
      <AuthModal v-model:visible="authVisible" action-name="磨皮生产记录一键确认" auth-mode="username" title="磨皮生产记录确认认证" @success="authSuccess" />
    </div>
  </Page>
</template>

<style scoped>
.production-record-page { min-height: 0; overflow: hidden; }
:global(.production-record-content) { box-sizing: border-box; display: flex; width: 100%; height: 100%; min-height: 0; padding: 8px !important; overflow: hidden !important; }
.production-record-report { display: grid; grid-template-rows: max-content max-content max-content minmax(0, 1fr) 48px; gap: 8px; width: 100%; height: 100%; min-width: 0; min-height: 0; overflow: hidden; }
.production-record-banner { min-height: 78px; }
.production-record-actions { flex-wrap: nowrap; }
.production-record-actions .action-tile { min-width: 62px; }
.production-record-actions .action-tile:disabled { cursor: not-allowed; opacity: .55; }
.consumable-default-tip { grid-column: 1 / -1; }
.query-panel { padding: 8px 10px; overflow: hidden; background: #eef3f8; border: 1px solid #8794a4; }
.query-grid { display: grid; grid-template-columns: 54px minmax(180px, 1fr) 54px minmax(220px, 1fr) 82px 82px; gap: 8px; }
.query-grid > label { display: flex; align-items: center; justify-content: center; min-height: 34px; color: #334155; font-size: 12px; font-weight: 800; background: #dbe3ed; border: 1px solid #c6d3df; }
.query-grid :deep(.ant-picker), .query-grid :deep(.ant-select-selector), .query-grid :deep(.ant-input-affix-wrapper), .query-grid :deep(.ant-btn) { width: 100%; min-height: 34px; border-radius: 2px; }
.record-body { min-width: 0; min-height: 0; overflow: hidden; background: #fff; border: 1px solid #d2dae5; }
.record-body :deep(.ant-spin-nested-loading), .record-body :deep(.ant-spin-container) { width: 100%; height: 100%; min-height: 0; }
.table-scroll { width: 100%; height: 100%; overflow: auto; scrollbar-gutter: stable; }
.record-table { width: 100%; min-width: 2440px; table-layout: fixed; border-collapse: separate; border-spacing: 0; color: #263241; font-size: 12px; }
.record-table th, .record-table td { height: 38px; padding: 6px 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; border-right: 1px solid #d2dae5; border-bottom: 1px solid #d2dae5; }
.record-table th { position: sticky; top: 0; z-index: 2; color: #334155; font-weight: 900; text-align: center; background: #eef2f7; }
.record-table tbody tr:nth-child(even) td { background: #f7f9fc; }
.record-table tbody tr:hover td { background: #eef6ff; }
.record-table tr.confirmed td { color: #64748b; }
.check { width: 46px; text-align: center; } .status { width: 86px; text-align: center; } .completion-time { width: 168px; min-width: 168px; font-variant-numeric: tabular-nums; } .actions { width: 118px; text-align: center; }
.num { text-align: center; font-variant-numeric: tabular-nums; } .center { text-align: center; } .empty { height: 120px !important; color: #667085; text-align: center; }
.life-cell-button { width: 100%; max-width: 100%; height: 26px; padding: 0 6px; overflow: hidden; color: #0f766e; font: inherit; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; background: #ecfdf5; border: 1px solid #99f6e4; border-radius: 4px; }
.life-cell-button.text-left { text-align: left; }
.life-cell-button:hover { color: #0f4f47; background: #d1fae5; border-color: #5eead4; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; color: #667085; font-size: 12px; background: #fff; border: 1px solid #d2dae5; }
.form-grid { display: grid; grid-template-columns: 118px minmax(0,1fr) 118px minmax(0,1fr); gap: 12px; align-items: center; padding: 12px 4px; }
.form-grid > label { color: #334155; font-weight: 700; text-align: right; }
.form-grid :deep(.ant-picker), .form-grid :deep(.ant-input-number), .form-grid :deep(.ant-select), .form-grid :deep(.ant-input) { width: 100%; }
.life-correction-form { grid-template-columns: 148px minmax(0, 1fr); }
.life-correction-form :deep(.ant-input-number) { width: 100%; }
.consumable-sync-form { grid-template-columns: 148px minmax(0, 1fr); }
.consumable-sync-form > span, .consumable-sync-form > strong { display: flex; align-items: center; min-height: 32px; color: #334155; }
.consumable-sync-form > strong { color: #0f766e; font-size: 14px; }
@media (max-width: 1440px) { .production-record-actions .action-tile { min-width: 56px; padding-inline: 5px; } }
</style>
