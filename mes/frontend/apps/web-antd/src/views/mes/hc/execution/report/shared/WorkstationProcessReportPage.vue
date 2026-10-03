<script lang="ts" setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  Button,
  DatePicker,
  Input,
  Modal as AModal,
  Select,
  Table as ATable,
  Tabs,
  TabPane,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTaskList } from '#/api/mes/work-order-booking/mock';
import { STATUS_MAP as BASE_STATUS_MAP, taskColumns } from '#/views/mes/work-order-booking/data';
import TaskDetailModal from './WorkstationTaskDetailModal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import QuickCheckModalComponent from '#/views/mes/work-order-booking/modules/components/quick-check-modal.vue';
import EdgeConsumableRegisterTab from '#/views/mes/hc/base/tooling-consumable-ledger/components/EdgeConsumableRegisterTab.vue';
import '../../../package-fg/shared/cut-round-board.css';
import {
  buildSegmentChainSampleLockCandidates,
  ensureSampleAbnormalUnlocked,
  findActiveSampleAbnormalLock,
} from './sampleAbnormalLockGuard';
import { useExecutionFullscreenClock } from './useExecutionFullscreenClock';
import { useRoute } from 'vue-router';

interface SceneConfig {
  title: string;
  processLabel: string;
  queryProcess?: string;
  detailComponent?: any;
  compactHeader?: boolean;
  consoleIcon?: string;
  enablePager?: boolean;
  industrialConsole?: boolean;
  workstationName: string;
  deviceRole: string;
  deviceId: string;
  deviceName: string;
  batchNoLabel?: string;
  keywordPlaceholder?: string;
  edgeConsumableProcessCode?: string;
  fetchTasks?: (params: {
    process: string;
    product: string;
    motherMaterial: string;
    motherModel: string;
    productionDate: string;
    statusTab: string;
    taskKeyword: string;
  }) => Promise<any>;
  submitReport?: (payload: {
    endTime?: string;
    goodQty: number;
    laborHours?: number;
    planId: number;
    planOperationId: number;
    remark?: string;
    scrapQty: number;
    scrapReason?: string;
    startTime?: string;
  }) => Promise<unknown>;
  columns?: any[];
  enableProductionDateQuery?: boolean;
  enableScanEntry?: boolean;
  hideDeviceLoginAction?: boolean;
  hideBannerPersonnel?: boolean;
  hideQuickCheckActions?: boolean;
  scanEntryCloseOnly?: boolean;
  scanEntryGlobalCapture?: boolean;
  scanEntryActivePath?: string | string[];
  scanPlanMinLength?: number;
  refreshTaskListOnActivated?: boolean;
}

const props = defineProps<{
  scene: SceneConfig;
}>();
const route = useRoute();
const WORKSTATION_STATUS_MAP: Record<string, { text: string; color: string; badge: string }> = {
  ...BASE_STATUS_MAP,
  CANCELED: { text: '已取消', color: 'error', badge: 'bg-red-500' },
  CANCELLED: { text: '作废取消', color: 'error', badge: 'bg-red-500' },
  PAUSED: { text: '已暂停', color: 'warning', badge: 'bg-orange-500' },
  RELEASED: { text: '待开工', color: 'default', badge: 'bg-slate-300' },
  RUNNING: { text: '生产中', color: 'processing', badge: 'bg-blue-500 animate-pulse' },
};

type QueryFieldKey = 'motherMaterial' | 'motherModel' | 'productionDate' | 'product';
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<'id' | QueryFieldKey, string>>;
};

const processValue = computed(() => props.scene.queryProcess || props.scene.processLabel);
const batchNoLabel = computed(() => props.scene.batchNoLabel || '批次号');
const keywordPlaceholder = computed(
  () => props.scene.keywordPlaceholder || `计划号 / ${batchNoLabel.value} / 工序任务`,
);
const isIndustrialConsole = computed(() => props.scene.industrialConsole === true);
const showDeviceLoginAction = computed(() => props.scene.hideDeviceLoginAction !== true);
const showQuickCheckActions = computed(() => props.scene.hideQuickCheckActions !== true);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
let currentTimer: ReturnType<typeof setInterval> | null = null;
let hasBeenActivated = false;
const taskRows = ref<any[]>([]);
const headerTaskRows = ref<any[]>([]);

const activeRunningRows = computed(() =>
  headerTaskRows.value.filter((item: any) => item?.status === 'IN_PROGRESS'),
);

const statusTabCounts = computed(() => {
  const sourceRows = headerTaskRows.value.length > 0 ? headerTaskRows.value : taskRows.value;
  return sourceRows.reduce(
    (counts, row: any) => {
      counts.ALL += 1;
      if (row?.status === 'PENDING') counts.PENDING += 1;
      if (row?.status === 'IN_PROGRESS') counts.IN_PROGRESS += 1;
      if (row?.status === 'COMPLETED') counts.COMPLETED += 1;
      return counts;
    },
    {
      ALL: 0,
      COMPLETED: 0,
      IN_PROGRESS: 0,
      PENDING: 0,
    },
  );
});

function formatTabCount(count: number) {
  return count > 100 ? '99+' : String(count);
}

const activeMachineSummary = computed(() => {
  const codes = new Set<string>();
  activeRunningRows.value.forEach((item: any) => {
    if (item?.mixerEquipmentCode) codes.add(item.mixerEquipmentCode);
    if (item?.foamingEquipmentCode) codes.add(item.foamingEquipmentCode);
  });
  return Array.from(codes);
});

const workstationStatusText = computed(() =>
  activeMachineSummary.value.length > 0 ? '生产中' : '空闲待机',
);

function normalizeTaskRows(payload: any): any[] {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.items)) return source.items;
  return [];
}

function buildEquipmentStatusMeta(status?: string) {
  if (status === 'IN_PROGRESS') return { className: 'is-producing', text: '生产中' };
  if (status === 'COMPLETED') return { className: 'is-completed', text: '已完工' };
  if (status === 'PAUSED') return { className: 'is-paused', text: '已暂停' };
  if (status === 'CANCELLED' || status === 'CANCELED') return { className: 'is-cancelled', text: '作废取消' };
  return { className: 'is-pending', text: '待开工' };
}

function getTaskStatusMeta(rowOrStatus?: string | Record<string, any>) {
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.status : rowOrStatus;
  const sampleAbnormalLocked = typeof rowOrStatus === 'object' && !!rowOrStatus?.sampleAbnormalLocked;
  const key = String(status || '').toUpperCase();
  if (key === 'IN_PROGRESS' && sampleAbnormalLocked) {
    return { text: '品质异常', color: 'error', badge: 'bg-red-500' };
  }
  return WORKSTATION_STATUS_MAP[key] || { text: status || '-', color: 'default', badge: 'bg-slate-300' };
}

const equipmentStatusPills = computed(() => {
  const sourceRows = (headerTaskRows.value || []).filter((item: any) => item?.mixerEquipmentCode || item?.foamingEquipmentCode || item?.equipmentCode);
  const sortedRows = [...sourceRows].sort((a: any, b: any) => {
    const weight = (status?: string) => (status === 'IN_PROGRESS' ? 3 : status === 'PENDING' ? 2 : status === 'COMPLETED' ? 1 : 0);
    return weight(b?.status) - weight(a?.status);
  });
  const map = new Map<string, any>();
  const put = (key: string, payload: any) => {
    if (!payload?.code || map.has(key)) return;
    map.set(key, payload);
  };
  sortedRows.forEach((row: any) => {
    const statusMeta = buildEquipmentStatusMeta(row?.status);
    const hasFormulaEquipment = !!(row?.mixerEquipmentCode || row?.foamingEquipmentCode);
    put(`mixer-${row?.mixerEquipmentCode || ''}`, {
      code: row?.mixerEquipmentCode,
      planNo: row?.planNo,
      role: '搅拌',
      statusMeta,
    });
    put(`foaming-${row?.foamingEquipmentCode || ''}`, {
      code: row?.foamingEquipmentCode,
      planNo: row?.planNo,
      role: '脱泡',
      statusMeta,
    });
    if (!hasFormulaEquipment) {
      put(`single-${row?.equipmentCode || ''}`, {
        code: row?.equipmentCode,
        planNo: row?.planNo,
        role: '机台',
        statusMeta,
      });
    }
  });
  return Array.from(map.values()).slice(0, 6);
});

const workstationInfo = ref({
  name: props.scene.workstationName,
  status: '空闲待机',
  temp: '24.5',
  humidity: '55',
  process: props.scene.processLabel,
  devices: [
    {
      role: props.scene.deviceRole,
      id: props.scene.deviceId,
      name: props.scene.deviceName,
    },
  ],
});

const operatorHistoryList = ref([
  {
    time: dayjs().subtract(2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    type: '上线打卡',
    user: '李师傅(8802)',
  },
]);
const lastOperator = ref<string>('李师傅(8802)');
const operatorHistoryVisible = ref(false);
const isOnline = ref(true);
const authVisible = ref(false);
const authAction = ref('');
const scanVisible = ref(false);
const scanPlanNo = ref('');
const scanInputRef = ref<any>();
const scanSearching = ref(false);
const lastScanSubmitted = ref('');
const scanErrorText = ref('');
const advancedQueryVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string>();
const queryTemplates = ref<QueryTemplate[]>([]);
const queryTemplateStorageKey = computed(
  () => `mes:workstation-report:${props.scene.processLabel}:query-templates`,
);
const queryFieldConfigs = computed<QueryFieldConfig[]>(() => {
  const fields: QueryFieldConfig[] = [
    { key: 'product', label: '产品料号' },
    { key: 'motherMaterial', label: '母料料号' },
    { key: 'motherModel', label: '母料型号' },
  ];
  if (props.scene.enableProductionDateQuery) {
    fields.push({ key: 'productionDate', label: '生产日期', type: 'date' });
  }
  return fields;
});
const activeQueryTags = computed(() =>
  queryFieldConfigs.value
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);
const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(queryTemplateStorageKey.value);
    const parsedTemplates = rawTemplates ? JSON.parse(rawTemplates) : [];
    if (!Array.isArray(parsedTemplates)) return [];
    return parsedTemplates
      .filter((item) => item?.name && item?.values)
      .map((item) => ({
        name: String(item.name),
        values: item.values,
      })) as QueryTemplate[];
  } catch {
    return [];
  }
}

function persistQueryTemplates() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(queryTemplateStorageKey.value, JSON.stringify(queryTemplates.value));
}

function getQueryFieldText(key: QueryFieldKey) {
  const value = queryParams.value[key];
  if (!value) return '';
  return String(value).trim();
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.value.forEach((config) => {
    queryParams.value[config.key] = '';
  });
}

function openAdvancedQuery() {
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  void loadData();
}

function snapshotQuery() {
  const values: QueryTemplate['values'] = {};
  if (queryParams.value.id.trim()) values.id = queryParams.value.id.trim();
  queryFieldConfigs.value.forEach((config) => {
    const value = String(queryParams.value[config.key] || '').trim();
    if (value) values[config.key] = value;
  });
  return values;
}

function saveQueryTemplate() {
  const name = queryTemplateName.value.trim();
  if (!name) {
    message.warning('请填写查询模板名称');
    return;
  }
  const values = snapshotQuery();
  if (Object.keys(values).length === 0) {
    message.warning('请先填写查询条件');
    return;
  }
  const nextTemplates = queryTemplates.value.filter((item) => item.name !== name);
  nextTemplates.unshift({ name, values });
  queryTemplates.value = nextTemplates.slice(0, 20);
  selectedTemplateName.value = name;
  persistQueryTemplates();
  message.success('查询模板已保存');
}

function removeQueryCondition(key: QueryFieldKey) {
  queryParams.value[key] = '';
  selectedTemplateName.value = undefined;
  void loadData();
}

function loadQueryTemplate(name?: string) {
  if (!name) return;
  const template = queryTemplates.value.find((item) => item.name === name);
  if (!template) return;
  queryParams.value.id = '';
  clearAdvancedQueryValues();
  Object.entries(template.values).forEach(([key, value]) => {
    if (key === 'id') {
      queryParams.value.id = value || '';
      return;
    }
    queryParams.value[key as QueryFieldKey] = value || '';
  });
  queryTemplateName.value = template.name;
  void loadData();
}

function handleLoadQueryTemplate(value?: string | number) {
  loadQueryTemplate(value ? String(value) : undefined);
}

const handleLoginAction = (type: 'login' | 'logout') => {
  authAction.value = type === 'login' ? '员工刷卡上线' : '员工下线登出';
  authVisible.value = true;
};

const onAuthSuccess = ({ empName }: any) => {
  const isLoginMode = authAction.value.includes('上线');
  const time = dayjs().format('YYYY-MM-DD HH:mm:ss');
  const actionText = isLoginMode ? '上线打卡' : '下线打卡';

  operatorHistoryList.value.unshift({ time, type: actionText, user: empName });

  if (isLoginMode) {
    lastOperator.value = empName;
    isOnline.value = true;
    workstationInfo.value.status = workstationStatusText.value;
  } else {
    lastOperator.value = '暂无 (已下线)';
    isOnline.value = false;
    workstationInfo.value.status = workstationStatusText.value;
  }

  message.success(`安全认证通过：${empName} 已${isLoginMode ? '上线' : '下线'}`);
  authVisible.value = false;
};

const [QuickCheckModal, quickCheckModalApi] = useVbenModal({ connectedComponent: QuickCheckModalComponent });
const openQuickCheck = (type: 'STARTUP' | 'CLEANING') => {
  quickCheckModalApi
    .setData({
      process: workstationInfo.value.process,
      type,
      workstation: workstationInfo.value.name,
      operator: lastOperator.value,
    })
    .open();
};

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: props.scene.detailComponent || TaskDetailModal,
});
const activeTab = ref('PENDING');
const showEdgeConsumableBoardTab = computed(() => !!props.scene.edgeConsumableProcessCode);
const isEdgeConsumableBoardTab = computed(() => activeTab.value === 'EDGE_CONSUMABLE');
const edgeConsumableProcessCode = computed(() => props.scene.edgeConsumableProcessCode || processValue.value);
const edgeConsumableContextRow = computed(() => activeRunningRows.value[0] || headerTaskRows.value[0] || taskRows.value[0] || {});
const edgeConsumablePlanNo = computed(() => String(edgeConsumableContextRow.value?.planNo || '').trim());
const edgeConsumableProductionBatchNo = computed(() =>
  String(
    edgeConsumableContextRow.value?.productionBatchNo ||
      edgeConsumableContextRow.value?.sourceProductionBatchNo ||
      edgeConsumableContextRow.value?.batchNo ||
      edgeConsumableContextRow.value?.parentProductionBatchNo ||
      '',
  ).trim(),
);
const queryParams = ref({
  id: '',
  product: '',
  motherMaterial: '',
  motherModel: '',
  productionDate: '',
  process: processValue.value,
});

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: props.scene.columns || taskColumns,
    pagerConfig: props.scene.enablePager
      ? { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] }
      : { enabled: false },
    height: 'auto',
    border: true,
    rowConfig: { isHover: true, height: 46 },
    toolbarConfig: props.scene.industrialConsole
      ? { enabled: false }
      : { refresh: true, zoom: true, custom: true },
  },
});

const scanPlanMinLength = computed(() => props.scene.scanPlanMinLength ?? 10);
const scanEntryCloseOnly = computed(() => props.scene.scanEntryCloseOnly === true);
const scanEntryGlobalCapture = computed(
  () => props.scene.enableScanEntry === true && props.scene.scanEntryGlobalCapture === true,
);
const scanEntryActivePaths = computed(() => {
  const rawPaths = props.scene.scanEntryActivePath;
  if (Array.isArray(rawPaths)) return rawPaths.filter(Boolean);
  return rawPaths ? [rawPaths] : [];
});
const normalizeRoutePath = (path?: string) => `/${String(path || '').replace(/^\/+|\/+$/g, '')}`;
const isCurrentScanEntryRoute = computed(() => {
  if (!scanEntryGlobalCapture.value) return false;
  const paths = scanEntryActivePaths.value;
  if (paths.length === 0) return true;
  const currentPath = normalizeRoutePath(route.path);
  return paths.some((path) => currentPath === normalizeRoutePath(path));
});
const SCAN_PLAN_SEPARATOR_REGEXP = /[，,；;]/;
const GLOBAL_SCANNER_MAX_GAP_MS = 80;
const GLOBAL_SCANNER_IDLE_FLUSH_MS = 160;
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;

const hasScanPlanDelimiter = (value?: string) => SCAN_PLAN_SEPARATOR_REGEXP.test(value || '');
const normalizeScanPlanNo = (value?: string) =>
  (value || '').trim().split(SCAN_PLAN_SEPARATOR_REGEXP)[0]?.trim() || '';

const focusScanInput = () => {
  nextTick(() => {
    window.setTimeout(() => {
      scanInputRef.value?.focus?.();
      scanInputRef.value?.input?.focus?.();
    }, 0);
  });
};

const isEventTargetInInputRef = (target: EventTarget | null, inputRef: { value?: any }) => {
  const element = inputRef.value?.input || inputRef.value?.$el || inputRef.value;
  return !!(element && target instanceof Node && element.contains?.(target));
};

const isEventFromScanInput = (event: KeyboardEvent) => isEventTargetInInputRef(event.target, scanInputRef);

const clearGlobalScannerBuffer = () => {
  globalScannerBuffer = '';
  globalScannerLastAt = 0;
  if (globalScannerTimer) {
    clearTimeout(globalScannerTimer);
    globalScannerTimer = null;
  }
};

const isGlobalScannerCandidate = (value: string) => {
  const text = value.trim();
  if (!text) return false;
  return hasScanPlanDelimiter(text) || normalizeScanPlanNo(text).length >= scanPlanMinLength.value;
};

const routeGlobalScannerInput = (value: string) => {
  if (!isCurrentScanEntryRoute.value) return;
  const planNo = normalizeScanPlanNo(value);
  if (!planNo) return;
  if (!scanVisible.value) {
    resetScanState();
    scanVisible.value = true;
  }
  handleScanInputChange(value);
  focusScanInput();
  nextTick(() => void handleScanSearch(planNo));
};

const flushGlobalScannerBuffer = () => {
  const scanned = globalScannerBuffer.trim();
  clearGlobalScannerBuffer();
  if (!isGlobalScannerCandidate(scanned)) return false;
  routeGlobalScannerInput(scanned);
  return true;
};

const scheduleGlobalScannerFlush = () => {
  if (globalScannerTimer) clearTimeout(globalScannerTimer);
  globalScannerTimer = setTimeout(() => {
    void flushGlobalScannerBuffer();
  }, GLOBAL_SCANNER_IDLE_FLUSH_MS);
};

const handleGlobalScannerKeydown = (event: KeyboardEvent) => {
  if (
    !isCurrentScanEntryRoute.value ||
    event.ctrlKey ||
    event.altKey ||
    event.metaKey ||
    event.isComposing ||
    isEventFromScanInput(event)
  ) {
    return;
  }
  if (event.key === 'Enter') {
    if (flushGlobalScannerBuffer()) {
      event.preventDefault();
      event.stopPropagation();
    }
    return;
  }
  if (event.key.length !== 1) return;
  const now = Date.now();
  const gap = globalScannerLastAt ? now - globalScannerLastAt : 0;
  if (!globalScannerLastAt || gap > GLOBAL_SCANNER_MAX_GAP_MS) {
    clearGlobalScannerBuffer();
  }
  globalScannerBuffer += event.key;
  globalScannerLastAt = now;
  scheduleGlobalScannerFlush();
  if (globalScannerBuffer.length > 1 && gap > 0 && gap <= GLOBAL_SCANNER_MAX_GAP_MS) {
    event.preventDefault();
  }
};

const attachGlobalScannerListener = () => {
  if (globalScannerListenerAttached || !isCurrentScanEntryRoute.value) return;
  window.addEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = true;
};

const detachGlobalScannerListener = () => {
  if (!globalScannerListenerAttached) return;
  window.removeEventListener('keydown', handleGlobalScannerKeydown, true);
  globalScannerListenerAttached = false;
  clearGlobalScannerBuffer();
};

const isSampleAbnormalLockGuardEnabled = computed(() => {
  const text = `${props.scene.title || ''}${props.scene.processLabel || ''}${props.scene.queryProcess || ''}`;
  if (/配料|投料/.test(text)) return false;
  return /湿法|磨皮|粗磨|精磨|粘胶|粘双面胶|背胶|分切|压槽|裁切|裁圆|包装/.test(text);
});

function normalizeSampleLockBatchNo(value?: unknown) {
  return String(value || '').trim();
}

function resolveTaskSegmentBatchNo(row: any) {
  return normalizeSampleLockBatchNo(
    row?.sourceProductionBatchNo ||
      row?.segmentBatchNo ||
      row?.parentProductionBatchNo ||
      row?.productionBatchNo ||
      row?.batchNo,
  );
}

function resolveTaskMotherBatchNo(row: any) {
  return normalizeSampleLockBatchNo(
    row?.sourceMotherBatchNo ||
      row?.sourceBatchNo ||
      row?.motherBatchNo ||
      row?.parentProductionBatchNo ||
      row?.batchNo ||
      row?.productionBatchNo,
  );
}

function resolveSceneSampleLockSourceProcessCodes() {
  const text = `${props.scene.title || ''}${props.scene.processLabel || ''}${props.scene.queryProcess || ''}`;
  const codes = ['WET'];
  if (/磨皮|粗磨|精磨|粘胶|粘双面胶|背胶|分切|压槽|裁切|裁圆|包装/.test(text)) {
    codes.push('ROUGH_GRINDING');
  }
  if (/粘胶|粘双面胶|背胶|分切|压槽|裁切|裁圆|包装/.test(text)) {
    codes.push('ADHESIVE1');
  }
  // 湿法留样NG仅在裁切报检/完工阻断，湿法任务仍可进入和继续报工。
  return new Set(props.scene.processLabel === '湿法' ? codes.filter((code) => code !== 'WET') : codes);
}

async function ensureTaskSampleAbnormalUnlocked(row: any, actionName: string) {
  if (!isSampleAbnormalLockGuardEnabled.value) return true;
  const sourceProcessCodes = resolveSceneSampleLockSourceProcessCodes();
  const candidates = buildSegmentChainSampleLockCandidates({
    motherBatchNo: resolveTaskMotherBatchNo(row),
    segmentBatchNo: resolveTaskSegmentBatchNo(row),
  }).filter((candidate) => sourceProcessCodes.has(String(candidate.sourceProcessCode || '')));
  if (candidates.length === 0) return true;
  return ensureSampleAbnormalUnlocked(candidates, actionName);
}

async function markTaskRowsSampleLockStatus(rows: any[]) {
  if (!isSampleAbnormalLockGuardEnabled.value) return rows;
  const sourceProcessCodes = resolveSceneSampleLockSourceProcessCodes();
  await Promise.all(
    rows.map(async (row) => {
      const status = String(row?.status || '').toUpperCase();
      if (status !== 'IN_PROGRESS') {
        row.sampleAbnormalLocked = false;
        return;
      }
      const candidates = buildSegmentChainSampleLockCandidates({
        motherBatchNo: resolveTaskMotherBatchNo(row),
        segmentBatchNo: resolveTaskSegmentBatchNo(row),
      }).filter((candidate) => sourceProcessCodes.has(String(candidate.sourceProcessCode || '')));
      row.sampleAbnormalLocked = !!(await findActiveSampleAbnormalLock(candidates));
    }),
  );
  return rows;
}

const openTaskDetail = async (row: any) => {
  if (!(await ensureTaskSampleAbnormalUnlocked(row, '加载报工'))) {
    return false;
  }
  detailModalApi
    .setData({
      ...row,
      batchNoLabel: batchNoLabel.value,
      isFormulaReport: props.scene.processLabel === '配料',
      submitBooking: props.scene.submitReport
        ? async (payload: any) => {
            if (!(await ensureTaskSampleAbnormalUnlocked(row, '提交报工'))) {
              throw new Error('留样异常锁定，已阻止提交报工');
            }
            return props.scene.submitReport?.({
              ...payload,
              batchNo: row.batchNo,
              planId: row.planId,
              planOperationId: row.planOperationId,
            });
          }
        : undefined,
    })
    .open();
  return true;
};

const resetScanState = () => {
  scanPlanNo.value = '';
  scanSearching.value = false;
  lastScanSubmitted.value = '';
  scanErrorText.value = '';
};

const openScanModal = () => {
  resetScanState();
  scanVisible.value = true;
  focusScanInput();
};

const searchTaskByPlanNo = async (planNo: string) => {
  if (!props.scene.fetchTasks) return [];
  const data = await props.scene.fetchTasks({
    process: processValue.value,
    product: '',
    motherMaterial: '',
    motherModel: '',
    productionDate: '',
    statusTab: 'ALL',
    taskKeyword: planNo,
  });
  return normalizeTaskRows(data);
};

const handleScanSearch = async (planNoText?: string) => {
  const planNo = normalizeScanPlanNo(planNoText ?? scanPlanNo.value);
  if (!planNo || planNo.length < scanPlanMinLength.value || scanSearching.value) return;
  if (scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  lastScanSubmitted.value = planNo;
  scanErrorText.value = '';
  scanSearching.value = true;
  try {
    const localMatched =
      headerTaskRows.value.find((item: any) => item?.planNo === planNo) ||
      taskRows.value.find((item: any) => item?.planNo === planNo);
    const matched = localMatched || (await searchTaskByPlanNo(planNo))?.[0];
    if (matched) {
      const opened = await openTaskDetail(matched);
      if (opened) {
        scanVisible.value = false;
        resetScanState();
      } else {
        focusScanInput();
      }
      return;
    }
    scanErrorText.value = '未找到对应计划，请重新扫描或重新输入计划号。';
    focusScanInput();
  } finally {
    scanSearching.value = false;
  }
};

const handleScanInputChange = (value: string) => {
  const normalizedPlanNo = normalizeScanPlanNo(value);
  scanPlanNo.value = hasScanPlanDelimiter(value) ? normalizedPlanNo : value;
  if (!normalizedPlanNo || normalizedPlanNo.length < scanPlanMinLength.value) {
    lastScanSubmitted.value = '';
    return;
  }
  if (normalizedPlanNo === lastScanSubmitted.value) return;
  void handleScanSearch(normalizedPlanNo);
};

const loadData = async () => {
  queryParams.value.process = processValue.value;
  gridApi.setGridOptions({ loading: true });
  const statusTab = isEdgeConsumableBoardTab.value ? 'ALL' : activeTab.value;
  const data = props.scene.fetchTasks
    ? await props.scene.fetchTasks({
        process: processValue.value,
        product: queryParams.value.product,
        motherMaterial: queryParams.value.motherMaterial,
        motherModel: queryParams.value.motherModel,
        productionDate: queryParams.value.productionDate,
        statusTab,
        taskKeyword: queryParams.value.id,
      })
    : await getTaskList(statusTab, queryParams.value);
  const rows = normalizeTaskRows(data);
  await markTaskRowsSampleLockStatus(rows);
  taskRows.value = rows;
  if (props.scene.compactHeader && props.scene.fetchTasks) {
    const headerRows = normalizeTaskRows(await props.scene.fetchTasks({
        process: processValue.value,
        product: queryParams.value.product,
        motherMaterial: queryParams.value.motherMaterial,
        motherModel: queryParams.value.motherModel,
        productionDate: queryParams.value.productionDate,
        statusTab: 'ALL',
        taskKeyword: queryParams.value.id,
      }));
    await markTaskRowsSampleLockStatus(headerRows);
    headerTaskRows.value = headerRows;
  } else {
    headerTaskRows.value = taskRows.value;
  }
  workstationInfo.value.status = workstationStatusText.value;
  gridApi.setGridOptions({ data: taskRows.value, loading: false });
};

const handleActiveTabChange = (key: string | number) => {
  if (String(key) === 'EDGE_CONSUMABLE') return;
  void loadData();
};

const mergeChangedTask = (rows: any[], changedTask: any) =>
  rows.map((item: any) => {
    if (
      (changedTask?.planOperationId && item?.planOperationId === changedTask.planOperationId) ||
      (changedTask?.id && item?.id === changedTask.id)
    ) {
      return { ...item, ...changedTask };
    }
    return item;
  });

const handleTaskChange = (changedTask: any) => {
  taskRows.value = mergeChangedTask(taskRows.value, changedTask);
  headerTaskRows.value = mergeChangedTask(headerTaskRows.value, changedTask);
  workstationInfo.value.status = workstationStatusText.value;
  gridApi.setGridOptions({ data: taskRows.value });
};

const resetQuery = () => {
  queryParams.value = {
    id: '',
    product: '',
    motherMaterial: '',
    motherModel: '',
    productionDate: '',
    process: processValue.value,
  };
  selectedTemplateName.value = undefined;
  queryTemplateName.value = '';
  advancedQueryVisible.value = false;
  loadData();
};

const processSelectOptions = computed(() => [
  {
    label: props.scene.processLabel,
    value: processValue.value,
  },
]);

watch(
  () => route.path,
  () => {
    if (isCurrentScanEntryRoute.value) {
      attachGlobalScannerListener();
    } else {
      detachGlobalScannerListener();
    }
  },
);

onMounted(() => {
  queryTemplates.value = loadQueryTemplates();
  attachGlobalScannerListener();
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  loadData();
});

onActivated(() => {
  attachGlobalScannerListener();
  if (hasBeenActivated && props.scene.refreshTaskListOnActivated) {
    void loadData();
  }
  hasBeenActivated = true;
});

onDeactivated(() => {
  detachGlobalScannerListener();
});

onBeforeUnmount(() => {
  if (currentTimer) {
    clearInterval(currentTimer);
  }
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height>
    <div :class="isIndustrialConsole ? 'package-fg-console workstation-report-console' : 'h-full flex flex-col bg-slate-100 gap-3 p-3 relative'">
      <slot
        v-if="isIndustrialConsole"
        name="industrial-banner"
        :clock="currentDateTime"
        :counts="statusTabCounts"
        :machineSummary="activeMachineSummary"
        :openScan="openScanModal"
        :refresh="loadData"
        :reset="resetQuery"
        :scene="props.scene"
        :showClock="showExecutionClock"
        :statusText="workstationStatusText"
      >
        <section class="prototype-banner workstation-report-banner">
          <span class="console-main-icon"><IconifyIcon :icon="props.scene.consoleIcon || 'lucide:factory'" /></span>
          <div class="console-title-block">
            <div class="console-title-row">
              <h2 class="console-title-text">{{ props.scene.title }}</h2>
              <Tag :color="activeMachineSummary.length > 0 ? 'processing' : 'default'" class="console-title-tag">
                {{ workstationStatusText }}
              </Tag>
            </div>
            <div class="console-meta-row">
              <span class="console-meta-item">
                <span class="console-meta-label">工序</span>
                <span class="console-meta-value">{{ props.scene.processLabel }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">待开工</span>
                <span class="console-meta-value">{{ statusTabCounts.PENDING }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">生产中</span>
                <span class="console-meta-value">{{ statusTabCounts.IN_PROGRESS }}</span>
              </span>
              <span class="console-meta-item">
                <span class="console-meta-label">已完工</span>
                <span class="console-meta-value">{{ statusTabCounts.COMPLETED }}</span>
              </span>
              <span
                v-if="!props.scene.hideBannerPersonnel"
                class="console-meta-item"
                @click="operatorHistoryVisible = true"
              >
                <span class="console-meta-label">人员</span>
                <span class="console-meta-value">{{ lastOperator }}</span>
              </span>
            </div>
          </div>
          <div v-if="showExecutionClock" class="work-time-card">
            <div>{{ currentDateTime.slice(0, 10) }}</div>
            <strong>{{ currentDateTime.slice(11) }}</strong>
          </div>
          <div class="console-action-group workstation-action-group">
            <button class="action-tile" type="button" @click="loadData">
              <IconifyIcon icon="lucide:search" />
              <span>查询</span>
            </button>
            <button class="action-tile" type="button" @click="resetQuery">
              <IconifyIcon icon="lucide:rotate-ccw" />
              <span>重置</span>
            </button>
            <button v-if="props.scene.enableScanEntry" class="action-tile" type="button" @click="openScanModal">
              <IconifyIcon icon="lucide:scan-line" />
              <span>扫码进入</span>
            </button>
            <button v-if="showQuickCheckActions" class="action-tile" type="button" @click="openQuickCheck('STARTUP')">
              <IconifyIcon icon="lucide:power" />
              <span>开机点检</span>
            </button>
            <button v-if="showQuickCheckActions" class="action-tile" type="button" @click="openQuickCheck('CLEANING')">
              <IconifyIcon icon="lucide:spray-can" />
              <span>清洁点检</span>
            </button>
            <button
              v-if="showDeviceLoginAction"
              class="action-tile"
              :class="{ 'is-danger': isOnline }"
              type="button"
              @click="handleLoginAction(isOnline ? 'logout' : 'login')"
            >
              <IconifyIcon :icon="isOnline ? 'lucide:log-out' : 'lucide:scan-face'" />
              <span>{{ isOnline ? '设备下线' : '设备上线' }}</span>
            </button>
          </div>
        </section>
      </slot>

      <div
        v-else-if="props.scene.compactHeader"
        class="shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex items-center justify-between gap-4 px-4 py-3 relative overflow-hidden"
      >
        <div class="flex items-center gap-3 min-w-0">
          <div class="flex h-[42px] w-[42px] items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-indigo-600 text-white shadow-sm">
            <IconifyIcon icon="lucide:factory" class="text-[20px]" />
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <span class="text-lg font-black text-slate-800">{{ props.scene.processLabel }}工位</span>
              <Tag :color="activeMachineSummary.length > 0 ? 'processing' : 'default'" class="!m-0 !border-none text-[11px] font-bold">{{ workstationStatusText }}</Tag>
            </div>
            <div v-if="equipmentStatusPills.length > 0" class="workstation-device-pills">
              <div
                v-for="pill in equipmentStatusPills"
                :key="`${pill.role}-${pill.code}`"
                class="workstation-device-pill"
                :title="`${pill.role} ${pill.code} / ${pill.planNo || '-'}`"
              >
                <span class="workstation-device-pill__role">{{ pill.role }}</span>
                <span class="workstation-device-pill__code">{{ pill.code }}</span>
                <span class="workstation-device-pill__status" :class="pill.statusMeta.className">{{ pill.statusMeta.text }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-3 text-xs text-slate-400">
          <div v-if="showExecutionClock" class="text-right">
            <div class="font-semibold tracking-wide">当前工位时间</div>
            <div class="mt-1 font-mono text-sm font-bold text-slate-700">{{ currentDateTime }}</div>
          </div>
          <div>
            <button
              v-if="props.scene.enableScanEntry"
              type="button"
              class="scan-entry-button"
              @click="openScanModal"
            >
              <span class="scan-entry-button__icon">
                <IconifyIcon icon="lucide:scan-line" />
              </span>
              <span class="scan-entry-button__text">扫码进入</span>
            </button>
          </div>
        </div>
      </div>

      <div v-else class="shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div class="w-[60px] h-[60px] bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white">
            <IconifyIcon icon="lucide:monitor-play" class="text-[32px]" />
          </div>

          <div class="flex flex-col justify-center gap-1.5 min-w-0">
            <div class="flex items-center w-max">
              <span class="text-xl font-black text-slate-800 tracking-wide truncate">{{ workstationInfo.name }}</span>
              <Tag color="processing" class="ml-3 !border-none font-bold shadow-sm h-5 flex items-center">
                {{ workstationInfo.process }}
              </Tag>
            </div>

            <div class="flex items-center gap-2 flex-wrap">
              <div
                v-for="dev in workstationInfo.devices"
                :key="dev.id"
                class="flex items-center text-xs bg-slate-50 px-2 py-1 rounded border border-slate-200"
              >
                <span class="font-bold text-indigo-600 mr-1.5">{{ dev.role }}</span>
                <span class="font-mono text-slate-500 mr-1.5">{{ dev.id }}</span>
                <span class="font-bold text-slate-700">{{ dev.name }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col justify-center px-6 border-l border-slate-100 shrink-0">
          <div class="text-[10px] text-slate-400 font-bold mb-1.5 uppercase tracking-wider">页面定位</div>
          <div class="text-sm font-bold text-indigo-700">{{ props.scene.title }}</div>
        </div>

        <div class="flex flex-col justify-center px-6 border-l border-slate-100 shrink-0">
          <div class="text-[10px] text-slate-400 font-bold mb-1.5 uppercase tracking-wider">最后环境温湿</div>
          <div class="flex gap-2">
            <span class="font-mono font-black text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded text-sm border border-indigo-100">
              {{ workstationInfo.temp }}<span class="text-xs ml-0.5">℃</span>
            </span>
            <span class="font-mono font-black text-blue-500 bg-blue-50 px-1.5 py-0.5 rounded text-sm border border-blue-100">
              {{ workstationInfo.humidity }}<span class="text-xs ml-0.5">%RH</span>
            </span>
          </div>
        </div>

        <div class="flex flex-col justify-center px-6 border-l border-slate-100 shrink-0 cursor-pointer group" @click="operatorHistoryVisible = true">
          <div class="text-[10px] text-slate-400 font-bold mb-1.5 uppercase tracking-wider flex items-center">
            最后操作人员
            <IconifyIcon icon="lucide:history" class="ml-1 text-indigo-300 group-hover:text-indigo-500" />
          </div>
          <div class="flex items-center">
            <IconifyIcon icon="lucide:user-check" class="mr-1.5 text-base" :class="isOnline ? 'text-emerald-500' : 'text-slate-300'" />
            <span class="font-black text-[15px]" :class="isOnline ? 'text-emerald-700' : 'text-slate-400'">{{ lastOperator }}</span>
          </div>
        </div>

        <div
          v-if="showQuickCheckActions || showDeviceLoginAction"
          class="flex items-center gap-2 pl-5 border-l border-slate-100 shrink-0"
        >
          <div
            v-if="showQuickCheckActions"
            class="w-[64px] h-[64px] bg-cyan-50 border border-cyan-200 text-cyan-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-cyan-100 hover:shadow-md transition-all active:scale-95"
            @click="openQuickCheck('STARTUP')"
          >
            <IconifyIcon icon="lucide:power" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">开机点检表</span>
          </div>

          <div
            v-if="showQuickCheckActions"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            @click="openQuickCheck('CLEANING')"
          >
            <IconifyIcon icon="lucide:spray-can" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">清洁点检表</span>
          </div>

          <div v-if="showQuickCheckActions && showDeviceLoginAction" class="w-px h-10 bg-slate-200 mx-1"></div>

          <div
            v-if="showDeviceLoginAction && !isOnline"
            class="w-[64px] h-[64px] bg-indigo-600 text-white rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-indigo-500 hover:shadow-md transition-all shadow-sm active:scale-95"
            @click="handleLoginAction('login')"
          >
            <IconifyIcon icon="lucide:scan-face" class="text-[22px] mb-1 opacity-90" />
            <span class="text-[11px] font-bold tracking-wide">设备上线</span>
          </div>

          <div
            v-else-if="showDeviceLoginAction"
            class="w-[64px] h-[64px] bg-red-500 text-white rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-red-400 hover:shadow-md transition-all shadow-sm active:scale-95"
            @click="handleLoginAction('logout')"
          >
            <IconifyIcon icon="lucide:log-out" class="text-[22px] mb-1 opacity-90" />
            <span class="text-[11px] font-bold tracking-wide">设备下线</span>
          </div>
        </div>
      </div>

      <section v-if="isIndustrialConsole" class="package-fg-filter-bar workstation-query-panel">
        <div class="workstation-simple-query">
          <label class="workstation-simple-query-label">关键词</label>
          <Input
            v-model:value="queryParams.id"
            allow-clear
            :placeholder="keywordPlaceholder"
            @press-enter="loadData"
          />
          <Button type="primary" @click="loadData">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            allow-clear
            class="workstation-query-template-select"
            :options="queryTemplateOptions"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="workstation-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="workstation-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <div :class="isIndustrialConsole ? 'workstation-report-body' : 'flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden'">
        <Tabs
          v-model:activeKey="activeTab"
          :class="isIndustrialConsole ? 'workstation-report-tabs' : 'custom-list-tabs px-4 pt-2 shrink-0'"
          @change="handleActiveTabChange"
        >
          <TabPane v-if="showEdgeConsumableBoardTab" key="EDGE_CONSUMABLE" tab="边库耗材登记" />
          <TabPane key="PENDING">
            <template #tab>
              <span class="status-tab-label">
                待开工
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.PENDING) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="IN_PROGRESS">
            <template #tab>
              <span class="status-tab-label">
                生产中
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.IN_PROGRESS) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="COMPLETED">
            <template #tab>
              <span class="status-tab-label">
                已完工
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.COMPLETED) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="ALL">
            <template #tab>
              <span class="status-tab-label">
                全部
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.ALL) }}</span>
              </span>
            </template>
          </TabPane>
        </Tabs>

        <div
          v-if="isEdgeConsumableBoardTab"
          :class="isIndustrialConsole ? 'workstation-report-grid-host' : 'flex-1 relative p-2 pt-0 min-h-0 bg-slate-50/50 custom-toolbar-grid'"
        >
          <EdgeConsumableRegisterTab
            :plan-no="edgeConsumablePlanNo"
            :process-code="edgeConsumableProcessCode"
            :production-batch-no="edgeConsumableProductionBatchNo"
            :readonly="false"
            :table-height="isIndustrialConsole ? 360 : 420"
          />
        </div>

        <div v-else :class="isIndustrialConsole ? 'workstation-report-grid-host' : 'flex-1 relative p-2 pt-0 min-h-0 bg-slate-50/50 custom-toolbar-grid'">
          <Grid class="h-full">
            <template v-if="!isIndustrialConsole" #toolbar-tools>
              <div class="flex items-center gap-2 pr-4 border-r border-slate-200">
                <Input v-model:value="queryParams.id" allow-clear placeholder="计划号" class="w-36" />
                <Input v-model:value="queryParams.motherMaterial" allow-clear placeholder="母料料号" class="w-36" />
                <Input v-model:value="queryParams.motherModel" allow-clear placeholder="母料型号" class="w-36" />
                <Input v-model:value="queryParams.product" allow-clear placeholder="产品料号" class="w-36" />
                <DatePicker
                  v-if="props.scene.enableProductionDateQuery"
                  v-model:value="queryParams.productionDate"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  placeholder="生产日期"
                  class="w-36"
                />
                <Select v-model:value="queryParams.process" disabled :options="processSelectOptions" class="w-28" />
                <Button type="primary" @click="loadData" class="bg-indigo-600 shadow-sm font-bold ml-1">
                  <IconifyIcon icon="lucide:search" class="mr-1" />
                  检索
                </Button>
                <Button @click="resetQuery">重置</Button>
              </div>
            </template>
            <template #goodSlot="{ row }">
              <span class="text-emerald-600 font-bold">{{ row.goodQty }} {{ row.uom }}</span>
            </template>
            <template #statusSlot="{ row }">
              <div class="flex items-center justify-center gap-1.5">
                <span class="w-2 h-2 rounded-full shadow-sm" :class="getTaskStatusMeta(row).badge"></span>
                <span
                  class="text-xs font-bold"
                  :class="getTaskStatusMeta(row).color === 'error' ? 'text-red-600' : row.status === 'IN_PROGRESS' || row.status === 'RUNNING' ? 'text-blue-600' : row.status === 'PAUSED' || row.status === 'CANCELLED' || row.status === 'CANCELED' ? 'text-red-600' : 'text-slate-600'"
                >
                  {{ getTaskStatusMeta(row).text }}
                </span>
              </div>
            </template>
            <template #actionSlot="{ row }">
              <Button
                type="primary"
                size="small"
                class="bg-indigo-600 text-xs font-bold shadow-sm"
                @click="openTaskDetail(row)"
              >
                <IconifyIcon icon="lucide:log-in" class="mr-1" />
                进站执行
              </Button>
            </template>
          </Grid>
        </div>
      </div>

      <AModal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="760px"
        wrap-class-name="workstation-advanced-query-modal"
      >
        <div class="workstation-advanced-query-body">
          <div class="workstation-advanced-query-grid">
            <div class="workstation-query-item">
              <label>产品料号</label>
              <Input v-model:value="queryParams.product" allow-clear placeholder="产品料号或名称" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="workstation-query-item">
              <label>母料料号</label>
              <Input v-model:value="queryParams.motherMaterial" allow-clear placeholder="母料料号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="workstation-query-item">
              <label>母料型号</label>
              <Input v-model:value="queryParams.motherModel" allow-clear placeholder="母料型号" @press-enter="applyAdvancedQuery" />
            </div>
            <div v-if="props.scene.enableProductionDateQuery" class="workstation-query-item">
              <label>生产日期</label>
              <DatePicker
                v-model:value="queryParams.productionDate"
                placeholder="生产日期"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
              />
            </div>
          </div>
          <div class="workstation-template-row">
            <label>模板名称</label>
            <Input
              v-model:value="queryTemplateName"
              allow-clear
              placeholder="填写名称后可保存当前查询条件"
            />
            <Button @click="saveQueryTemplate">保存模板</Button>
          </div>
          <div class="workstation-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQueryValues">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </AModal>

      <DetailModal @refresh="loadData" @task-change="handleTaskChange" />
      <QuickCheckModal />
      <AuthModal
        v-model:visible="authVisible"
        :actionName="authAction"
        :workstation="workstationInfo.name"
        @success="onAuthSuccess"
      />

      <AModal
        v-model:open="scanVisible"
        title="扫码进入详情"
        :closable="!scanEntryCloseOnly"
        :confirmLoading="scanSearching"
        :keyboard="!scanEntryCloseOnly"
        :mask-closable="!scanEntryCloseOnly"
        okText="查询进入"
        cancelText="关闭"
        width="520px"
        centered
        @ok="handleScanSearch()"
        @cancel="resetScanState"
      >
        <div class="pt-4">
          <div class="scan-entry-guide">
            <div class="scan-entry-guide__icon">
              <IconifyIcon icon="lucide:scan-search" />
            </div>
            <div class="scan-entry-guide__content">
              <div class="scan-entry-guide__title">请使用扫码枪扫描计划号</div>
              <div class="scan-entry-guide__desc">也支持人工直接输入；满足长度后会自动查询并进入报工详情。</div>
            </div>
          </div>
          <Input
            ref="scanInputRef"
            :value="scanPlanNo"
            allow-clear
            autofocus
            placeholder="请扫描或输入计划号"
            size="large"
            @update:value="handleScanInputChange"
            @pressEnter="handleScanSearch()"
          />
          <div v-if="scanErrorText" class="scan-entry-error">{{ scanErrorText }}</div>
        </div>
      </AModal>

      <AModal v-model:open="operatorHistoryVisible" title="当天工位人员操作履历" :footer="null" :width="500" centered>
        <div class="pt-4 pb-2">
          <ATable :dataSource="operatorHistoryList" :pagination="false" size="small" bordered>
            <ATable.Column title="发生时间" dataIndex="time" />
            <ATable.Column title="动作类型" dataIndex="type">
              <template #default="{ text }">
                <Tag :color="text.includes('上线') ? 'blue' : 'default'">{{ text }}</Tag>
              </template>
            </ATable.Column>
            <ATable.Column title="最后操作员工" dataIndex="user" />
          </ATable>
        </div>
      </AModal>
    </div>
  </Page>
</template>

<style scoped>
.workstation-report-console {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  box-sizing: border-box;
  padding: 8px;
}

.workstation-report-banner {
  min-height: 78px;
  max-height: 90px;
}

.workstation-action-group {
  flex-wrap: nowrap;
}

.workstation-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.workstation-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px 210px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.workstation-simple-query-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.workstation-simple-query :deep(.ant-input-affix-wrapper),
.workstation-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.workstation-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.workstation-query-template-select {
  min-width: 0;
}

.workstation-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.workstation-query-tag {
  display: inline-flex;
  align-items: center;
  max-width: 360px;
  min-height: 24px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
}

.workstation-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.workstation-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workstation-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.workstation-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.workstation-report-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.workstation-report-tabs {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.workstation-report-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.workstation-report-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.workstation-report-grid-host {
  position: relative;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #8794a4;
}

.workstation-report-grid-host :deep(.vxe-grid) {
  height: 100% !important;
  min-height: 0 !important;
}

.workstation-report-grid-host :deep(.vxe-grid--table-wrapper) {
  min-height: 0 !important;
  overflow: hidden !important;
}

.workstation-advanced-query-body {
  display: grid;
  gap: 10px;
}

.workstation-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.workstation-query-item {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.workstation-query-item label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  background: #dbe3ed;
  border-right: 1px solid #c6d3df;
}

.workstation-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.workstation-query-item :deep(.ant-input),
.workstation-query-item :deep(.ant-input-affix-wrapper),
.workstation-query-item :deep(.ant-picker),
.workstation-query-item :deep(.ant-select),
.workstation-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.workstation-template-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: stretch;
  padding-top: 2px;
}

.workstation-template-row label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.workstation-template-row :deep(.ant-input-affix-wrapper),
.workstation-template-row :deep(.ant-btn) {
  min-height: 32px;
  border-radius: 0;
}

.workstation-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

:deep(.vxe-body--column) { padding: 6px 0 !important; }
:deep(.custom-list-tabs .ant-tabs-nav) { margin-bottom: 0 !important; border-bottom: 1px solid #f1f5f9; }
.custom-toolbar-grid :deep(.vxe-toolbar) { padding: 8px 16px; background-color: #ffffff; border-bottom: 1px solid #f1f5f9; border-radius: 8px 8px 0 0; }

.status-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-tab-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #eef2ff;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 800;
  line-height: 18px;
  text-align: center;
}

:deep(.ant-tabs-tab-active) .status-tab-badge {
  background: #1677ff;
  color: #fff;
}

.scan-entry-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px 0 8px;
  border: 1px solid #bfdbfe;
  border-radius: 9999px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  color: #1d4ed8;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.14);
  transition: all 0.2s ease;
}

.scan-entry-button:hover {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.18);
}

.scan-entry-button__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 9999px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
}

.scan-entry-button__text {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.workstation-device-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.workstation-device-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 4px 10px;
  border: 1px solid #dbeafe;
  border-radius: 9999px;
  background: #f8fbff;
  color: #334155;
  font-size: 12px;
  line-height: 1;
}

.workstation-device-pill__role {
  color: #2563eb;
  font-weight: 700;
}

.workstation-device-pill__code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-weight: 600;
}

.workstation-device-pill__status {
  padding: 2px 6px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
}

.workstation-device-pill__status.is-producing {
  background: #dbeafe;
  color: #1d4ed8;
}

.workstation-device-pill__status.is-pending {
  background: #f3f4f6;
  color: #4b5563;
}

.workstation-device-pill__status.is-completed {
  background: #dcfce7;
  color: #15803d;
}

.workstation-device-pill__status.is-paused {
  background: #ffedd5;
  color: #c2410c;
}

.workstation-device-pill__status.is-cancelled {
  background: #fee2e2;
  color: #b91c1c;
}

.scan-entry-guide {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border: 1px dashed #93c5fd;
  border-radius: 12px;
  background: linear-gradient(135deg, #eff6ff, #f8fbff);
}

.scan-entry-guide__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: #2563eb;
  color: #fff;
  font-size: 28px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.18);
}

.scan-entry-guide__content {
  min-width: 0;
}

.scan-entry-guide__title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.scan-entry-guide__desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.scan-entry-error {
  margin-top: 10px;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

@media (max-width: 1280px) {
  .workstation-report-banner {
    max-height: none;
    flex-wrap: wrap;
  }

  .workstation-action-group {
    width: 100%;
    padding-left: 0 !important;
    border-left: 0 !important;
  }

  .workstation-simple-query {
    grid-template-columns: 86px minmax(240px, 1fr) 88px 110px;
  }

  .workstation-query-template-select {
    grid-column: 2 / -1;
  }
}

@media (max-width: 760px) {
  .workstation-simple-query,
  .workstation-advanced-query-grid,
  .workstation-template-row {
    grid-template-columns: 1fr;
  }

  .workstation-query-tags {
    padding-left: 0;
  }

  .workstation-query-item {
    grid-template-columns: 86px minmax(0, 1fr);
  }

  .workstation-simple-query-label,
  .workstation-template-row label {
    justify-content: flex-start;
  }
}
</style>
