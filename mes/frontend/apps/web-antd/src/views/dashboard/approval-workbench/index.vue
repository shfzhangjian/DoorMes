<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { BpmTaskApi } from '#/api/bpm/task';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import {
  BpmProcessInstanceStatus,
  BpmTaskStatusEnum,
  DICT_TYPE,
} from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  Input,
  message,
  Pagination,
  RangePicker,
  Select,
  Space,
  Spin,
} from 'ant-design-vue';

import {
  getProcessInstance,
  getProcessInstanceCopyPage,
  getProcessInstanceMyPage,
} from '#/api/bpm/processInstance';
import { getExceptionEvent } from '#/api/mes/quality/abnormal/exception';
import { getNcrRecord } from '#/api/mes/quality/abnormal/ncr';
import { getOrder as getDeviceExceptionOrder } from '#/api/mes/resource/device/fault-repair';
import { getOnboardingApply } from '#/api/mes/srm/onboarding-apply';
import { getSupplierExitApproval } from '#/api/mes/srm/supplier-exit-approval';
import { getTrialValidation } from '#/api/mes/srm/trial-validation';
import {
  getMesWorkflowTaskDonePage,
  getMesWorkflowTaskTodoPage,
} from '#/api/mes/workflow/task';
import { router } from '#/router';
import { QMS_EXCEPTION_DICT } from '#/views/mes/quality/abnormal/exception/data';
import ExceptionDetailModal from '#/views/mes/quality/abnormal/exception/modules/detail-modal.vue';
import NcrDetailModal from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';
import DeviceExceptionDetailModal from '#/views/mes/resource/device/fault-repair/modules/form.vue';
import SupplierExitApprovalDetailModal from '#/views/mes/srm/archive/exit/modules/detail-modal.vue';
import OnboardingApplyDetailModal from '#/views/mes/srm/certification/import-application/modules/detail-modal.vue';
import SampleEvaluationDetailModal from '#/views/mes/srm/certification/sample-evaluation/modules/detail-modal.vue';
import TrialValidationDetailModal from '#/views/mes/srm/certification/trial-validation/modules/detail-modal.vue';
import SampleRequestDetailModal from '#/views/mes/srm/onboarding/sample-req/modules/detail-modal.vue';

import BusinessFormModal from '../../bpm/processInstance/detail/modules/business-form-modal.vue';

defineOptions({ name: 'DashboardApprovalWorkbench' });

type WorkbenchTabKey = 'copy' | 'done' | 'mine' | 'todo';
type WorkbenchRowSource = WorkbenchTabKey;
type SummarySource = 'bpm' | 'fallback';
type SummaryTone = 'danger' | 'default' | 'strong' | 'warning';

const DATE_FORMAT = 'YYYY-MM-DD';
const COUNT_QUERY_PAGE_SIZE = 200;
const NCR_PROCESS_KEYS = [
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
];
const NCR_FORM_PATHS = new Set([
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
]);
const EXCEPTION_PROCESS_KEYS = new Set(['qms_exception_event']);
const EXCEPTION_FORM_PATHS = new Set(['/mes/quality/abnormal/exception']);
const DEVICE_EXCEPTION_PROCESS_KEYS = new Set(['resource_device_exception']);
const DEVICE_EXCEPTION_FORM_PATHS = new Set([
  '/mes/resource/device/fault-repair',
]);
const PRELIMINARY_PROCESS_KEYS = ['srm_preliminary_evaluation'];
const PRELIMINARY_FORM_PATHS = new Set([
  '/mes/srm/certification/preliminary-evaluation',
]);
const SAMPLE_EVALUATION_PROCESS_KEYS = ['srm_sample_evaluation'];
const SAMPLE_EVALUATION_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-evaluation',
]);
const TRIAL_VALIDATION_PROCESS_KEYS = ['srm_trial_validation'];
const TRIAL_VALIDATION_FORM_PATHS = new Set([
  '/mes/srm/certification/trial-validation',
]);
const SAMPLE_REQUEST_PROCESS_KEYS = ['srm_sample_request'];
const SAMPLE_REQUEST_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-req',
]);
const ONBOARDING_APPLY_PROCESS_KEYS = ['srm_onboarding_apply'];
const ONBOARDING_APPLY_FORM_PATHS = new Set([
  '/mes/srm/certification/import-application',
]);
const SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS = ['srm_supplier_exit_approval'];
const SUPPLIER_EXIT_APPROVAL_FORM_PATHS = new Set([
  '/mes/srm/certification/exit',
]);
const EXCEPTION_TYPE_FALLBACK_OPTIONS = [
  { label: '生产异常', value: 'PRODUCTION' },
  { label: '质量异常', value: 'QUALITY' },
  { label: '设备异常', value: 'EQUIPMENT' },
  { label: '交付异常', value: 'DELIVERY' },
];
const EXCEPTION_LEVEL_FALLBACK_OPTIONS = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'MAJOR' },
  { label: '一般', value: 'NORMAL' },
  { label: '严重', value: 'CRITICAL' },
  { label: '严重', value: 'SERIOUS' },
];

interface SummaryItem {
  key: string;
  tone?: SummaryTone;
  value: string;
}

interface SummaryBuildResult {
  businessNo?: string;
  items: SummaryItem[];
  source: SummarySource;
}

interface WorkbenchRow {
  activityId?: string;
  assigneeName?: string;
  businessNo?: string;
  businessId?: number;
  businessStatus?: string;
  categoryName?: string;
  currentNodeName?: string;
  endTime?: number | string;
  key: string;
  processInstanceId: number | string;
  processName: string;
  raw:
    | BpmProcessInstanceApi.ProcessInstance
    | BpmProcessInstanceApi.ProcessInstanceCopyRespVO
    | BpmTaskApi.Task;
  reason?: string;
  relationTime?: number | string;
  source: WorkbenchRowSource;
  startUserName?: string;
  statusDictType?: string;
  statusLabel?: string;
  statusValue?: number;
  summary: SummaryItem[];
  summarySource: SummarySource;
  taskId?: string;
}

interface StatusIconMeta {
  icon: string;
  label: string;
  tone: string;
}

const tabItems: {
  icon: string;
  key: WorkbenchTabKey;
  title: string;
}[] = [
  {
    icon: 'lucide:inbox',
    key: 'todo',
    title: '待办任务',
  },
  {
    icon: 'lucide:send',
    key: 'mine',
    title: '我的流程',
  },
  {
    icon: 'lucide:check-check',
    key: 'done',
    title: '已办任务',
  },
  {
    icon: 'lucide:copy-check',
    key: 'copy',
    title: '抄送我的',
  },
];

const activeTab = ref<WorkbenchTabKey>('todo');
const loading = ref(false);
const actionLoadingKey = ref('');
const rows = ref<WorkbenchRow[]>([]);
const requestSeq = ref(0);
const countRequestSeq = ref(0);
const selectedRowKey = ref('');
const counts = reactive<Record<WorkbenchTabKey, number>>({
  copy: 0,
  done: 0,
  mine: 0,
  todo: 0,
});
const filters = reactive<{
  createTime?: string[];
  keyword: string;
  status?: number;
}>({
  createTime: undefined,
  keyword: '',
  status: undefined,
});
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const processStatusOptions = getDictOptions(
  DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS,
  'number',
);
const taskStatusOptions = getDictOptions(DICT_TYPE.BPM_TASK_STATUS, 'number');

const statusLegendItems: StatusIconMeta[] = [
  { icon: 'lucide:clock-3', label: '审批中', tone: 'running' },
  { icon: 'lucide:hourglass', label: '待审批', tone: 'waiting' },
  { icon: 'lucide:circle-check', label: '审批通过', tone: 'success' },
  { icon: 'lucide:circle-x', label: '审批不通过', tone: 'danger' },
  { icon: 'lucide:undo-2', label: '已退回', tone: 'warning' },
  { icon: 'lucide:circle-slash', label: '已取消', tone: 'muted' },
  { icon: 'lucide:copy-check', label: '抄送', tone: 'copy' },
];

const activeTabMeta = computed(() => {
  return tabItems.find((item) => item.key === activeTab.value) || tabItems[0]!;
});

const activeStatusOptions = computed(() => {
  if (activeTab.value === 'copy') {
    return [];
  }
  return activeTab.value === 'done' ? taskStatusOptions : processStatusOptions;
});

function buildQueryParams(
  options: {
    pageNo?: number;
    pageSize?: number;
    tabKey?: WorkbenchTabKey;
    withStatus?: boolean;
  } = {},
) {
  const tabKey = options.tabKey || activeTab.value;
  const params: Record<string, any> = {
    pageNo: options.pageNo || pagination.current,
    pageSize: options.pageSize || pagination.pageSize,
  };
  const keyword = filters.keyword.trim();
  if (keyword) {
    params.name = keyword;
  }
  if (filters.createTime?.length === 2) {
    const [startDate, endDate] = filters.createTime;
    if (startDate && endDate) {
      params.createTime = [`${startDate} 00:00:00`, `${endDate} 23:59:59`];
    }
  }
  if (
    tabKey !== 'copy' &&
    filters.status !== undefined &&
    options.withStatus !== false
  ) {
    params.status = filters.status;
  }
  return params;
}

function buildCountQueryParams(tabKey: WorkbenchTabKey) {
  return buildQueryParams({
    pageNo: 1,
    pageSize: COUNT_QUERY_PAGE_SIZE,
    tabKey,
    withStatus: tabKey === activeTab.value,
  });
}

async function loadCounts() {
  const seq = ++countRequestSeq.value;
  const [todo, mine, done, copy] = await Promise.allSettled([
    getMesWorkflowTaskTodoPage(buildCountQueryParams('todo')),
    getProcessInstanceMyPage(buildCountQueryParams('mine')),
    getMesWorkflowTaskDonePage(buildCountQueryParams('done')),
    getProcessInstanceCopyPage(buildCountQueryParams('copy')),
  ]);
  if (seq !== countRequestSeq.value) {
    return;
  }
  counts.todo = resolveCountTotal(todo);
  counts.mine = resolveCountTotal(mine);
  counts.done = resolveCountTotal(done);
  counts.copy = resolveCountTotal(copy);
}

function resolveCountTotal(result: PromiseSettledResult<any>) {
  if (result.status !== 'fulfilled') {
    return 0;
  }
  return normalizeResultTotal(
    getResultListLength(result.value),
    result.value?.total,
    1,
    COUNT_QUERY_PAGE_SIZE,
  );
}

function getResultListLength(result: any) {
  return Array.isArray(result?.list) ? result.list.length : 0;
}

function normalizeResultTotal(
  listLength: number,
  total: number | undefined,
  pageNo: number,
  pageSize: number,
) {
  const visibleTotal = (Math.max(pageNo, 1) - 1) * pageSize + listLength;
  const normalizedTotal = Number(total) || 0;
  if (normalizedTotal < visibleTotal) {
    return visibleTotal;
  }
  if (listLength < pageSize && normalizedTotal > visibleTotal) {
    return visibleTotal;
  }
  return normalizedTotal;
}

async function loadTable() {
  const seq = ++requestSeq.value;
  loading.value = true;
  try {
    const params = buildQueryParams();
    if (activeTab.value === 'todo') {
      const result = await getMesWorkflowTaskTodoPage(params);
      const nextRows = await enrichBusinessRows(
        result.list.map((item) => mapTodoRow(item)),
      );
      syncRows(seq, nextRows, result.total);
      return;
    }
    if (activeTab.value === 'mine') {
      const result = await getProcessInstanceMyPage(params);
      const nextRows = await enrichBusinessRows(
        result.list.map((item) => mapMineRow(item)),
      );
      syncRows(seq, nextRows, result.total);
      return;
    }
    if (activeTab.value === 'done') {
      const result = await getMesWorkflowTaskDonePage(params);
      const nextRows = await enrichBusinessRows(
        result.list.map((item) => mapDoneRow(item)),
      );
      syncRows(seq, nextRows, result.total);
      return;
    }
    const result = await getProcessInstanceCopyPage(params);
    const nextRows = await enrichBusinessRows(
      result.list.map((item) => mapCopyRow(item)),
    );
    syncRows(seq, nextRows, result.total);
  } finally {
    if (seq === requestSeq.value) {
      loading.value = false;
    }
  }
}

function syncRows(seq: number, nextRows: WorkbenchRow[], total: number) {
  if (seq !== requestSeq.value) {
    return;
  }
  rows.value = nextRows;
  const displayTotal = normalizeResultTotal(
    nextRows.length,
    total,
    pagination.current,
    pagination.pageSize,
  );
  pagination.total = displayTotal;
  counts[activeTab.value] = displayTotal;
  if (
    nextRows.length > 0 &&
    !nextRows.some((item) => item.key === selectedRowKey.value)
  ) {
    selectedRowKey.value = nextRows[0]!.key;
  }
}

async function enrichBusinessRows(nextRows: WorkbenchRow[]) {
  return await Promise.all(
    nextRows.map(async (row) => {
      try {
        return await enrichBusinessRow(row);
      } catch {
        return row;
      }
    }),
  );
}

async function enrichBusinessRow(row: WorkbenchRow): Promise<WorkbenchRow> {
  const instance = await resolveSummaryInstance(row);
  const variables = collectFormVariables(
    instance,
    row.raw as Record<string, any>,
  );
  if (isNcrProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey || variables.ncRecordId || variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getNcrRecord(id);
    return applyBusinessSummary(row, instance, detail);
  }
  if (isDeviceExceptionProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey ||
        variables.deviceExceptionId ||
        variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getDeviceExceptionOrder(id);
    return applyBusinessSummary(row, instance, detail);
  }
  if (isExceptionProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey ||
        variables.exceptionEventId ||
        variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getExceptionEvent(id);
    return applyBusinessSummary(row, instance, detail);
  }
  if (isOnboardingApplyProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey ||
        variables.onboardingApplyId ||
        variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getOnboardingApply(id);
    return applyBusinessSummary(row, instance, detail);
  }
  if (isSupplierExitApprovalProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey ||
        variables.supplierExitApprovalId ||
        variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getSupplierExitApproval(id);
    return applyBusinessSummary(row, instance, detail);
  }
  if (isTrialValidationProcess(instance, variables)) {
    const id = toNumber(
      instance?.businessKey ||
        variables.trialValidationId ||
        variables.businessKey,
    );
    if (!id) {
      return row;
    }
    const detail = await getTrialValidation(id);
    return applyBusinessSummary(row, instance, detail);
  }
  return row;
}

async function resolveSummaryInstance(row: WorkbenchRow) {
  const raw = row.raw as Record<string, any>;
  if (raw.processInstance) {
    return raw.processInstance as BpmProcessInstanceApi.ProcessInstance;
  }
  if (raw.formVariables || raw.processDefinitionId || raw.processDefinition) {
    return raw as BpmProcessInstanceApi.ProcessInstance;
  }
  return await getProcessInstance(row.processInstanceId);
}

function applyBusinessSummary(
  row: WorkbenchRow,
  instance: BpmProcessInstanceApi.ProcessInstance | undefined,
  businessData: Record<string, any>,
): WorkbenchRow {
  const summary = buildWorkbenchSummary(
    instance,
    row.raw as Record<string, any>,
    businessData,
  );
  const isException = isExceptionProcess(
    instance,
    collectFormVariables(
      instance,
      row.raw as Record<string, any>,
      businessData,
    ),
  );
  const isDeviceException = isDeviceExceptionProcess(
    instance,
    collectFormVariables(
      instance,
      row.raw as Record<string, any>,
      businessData,
    ),
  );
  const exceptionTodoLabel = normalizeDisplayValue(
    businessData.currentUserTaskTodoLabel,
  );
  const exceptionDoneLabel = normalizeDisplayValue(
    businessData.currentUserTaskDoneLabel,
  );
  const currentNodeName =
    ((isException || isDeviceException) && row.source === 'todo'
      ? exceptionTodoLabel
      : '') ||
    ((isException || isDeviceException) && row.source === 'done'
      ? exceptionDoneLabel
      : '') ||
    normalizeDisplayValue(businessData.currentNodeName) ||
    row.currentNodeName;
  const statusLabel =
    ((isException || isDeviceException) && row.source === 'todo'
      ? exceptionTodoLabel
      : '') ||
    ((isException || isDeviceException) && row.source === 'done'
      ? exceptionDoneLabel
      : '') ||
    row.statusLabel;
  return {
    ...row,
    businessNo: summary.businessNo || row.businessNo,
    businessStatus: translateBusinessStatus(businessData.status),
    currentNodeName,
    statusLabel,
    summary: summary.items,
    summarySource: summary.source,
  };
}

function mapTodoRow(row: BpmTaskApi.Task): WorkbenchRow {
  const instance = row.processInstance;
  const summary = buildWorkbenchSummary(instance, row);
  return {
    assigneeName: row.assigneeUser?.nickname,
    businessNo: summary.businessNo,
    categoryName: instance?.categoryName || instance?.category,
    currentNodeName: row.name,
    key: `todo-${row.id}`,
    processInstanceId: row.processInstanceId,
    processName: instance?.name || row.name || '-',
    raw: row,
    relationTime: row.createTime,
    source: 'todo',
    startUserName: instance?.startUser?.nickname,
    statusDictType: DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS,
    statusValue: instance?.status ?? row.status,
    summary: summary.items,
    summarySource: summary.source,
    taskId: row.id,
  };
}

function mapMineRow(row: BpmProcessInstanceApi.ProcessInstance): WorkbenchRow {
  const firstTask = row.tasks?.[0];
  const summary = buildWorkbenchSummary(row);
  return {
    assigneeName: getTaskAssigneeText(row.tasks),
    businessNo: summary.businessNo,
    categoryName: row.categoryName || row.category,
    currentNodeName: firstTask?.name,
    endTime: row.endTime,
    key: `mine-${row.id}`,
    processInstanceId: row.id,
    processName: row.name || '-',
    raw: row,
    relationTime: row.startTime || row.createTime,
    source: 'mine',
    startUserName: row.startUser?.nickname,
    statusDictType: DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS,
    statusValue: row.status,
    summary: summary.items,
    summarySource: summary.source,
  };
}

function mapDoneRow(row: BpmTaskApi.Task): WorkbenchRow {
  const instance = row.processInstance;
  const summary = buildWorkbenchSummary(instance, row);
  return {
    assigneeName: row.assigneeUser?.nickname,
    businessNo: summary.businessNo,
    categoryName: instance?.categoryName || instance?.category,
    currentNodeName: row.name,
    endTime: row.endTime,
    key: `done-${row.id}`,
    processInstanceId: row.processInstanceId,
    processName: instance?.name || row.name || '-',
    raw: row,
    reason: row.reason,
    relationTime: row.endTime || row.createTime,
    source: 'done',
    startUserName: instance?.startUser?.nickname,
    statusDictType: DICT_TYPE.BPM_TASK_STATUS,
    statusValue: row.status,
    summary: summary.items,
    summarySource: summary.source,
    taskId: row.id,
  };
}

function mapCopyRow(
  row: BpmProcessInstanceApi.ProcessInstanceCopyRespVO,
): WorkbenchRow {
  const summary = buildWorkbenchSummary(undefined, row);
  return {
    activityId: row.activityId,
    assigneeName: row.createUser?.nickname,
    businessNo: summary.businessNo,
    currentNodeName: row.activityName,
    key: `copy-${row.id}`,
    processInstanceId: row.processInstanceId,
    processName: row.processInstanceName || '-',
    raw: row,
    reason: row.reason,
    relationTime: row.createTime,
    source: 'copy',
    startUserName: row.startUser?.nickname,
    statusLabel: '抄送',
    summary: summary.items,
    summarySource: summary.source,
    taskId: row.taskId,
  };
}

function buildWorkbenchSummary(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  row?: Record<string, any>,
  businessData?: Record<string, any>,
): SummaryBuildResult {
  const variables = collectFormVariables(instance, row, businessData);
  const items: SummaryItem[] = [];
  if (isNcrProcess(instance, variables)) {
    pushSummary(items, 'NCR单号', firstValue(variables, ['ncNo']), 'strong');
    pushSummary(
      items,
      '类型',
      firstValue(variables, ['sourceTypeName', 'sourceType']),
    );
    pushSummary(
      items,
      '发生日期',
      formatSummaryDate(firstValue(variables, ['happenTime'])),
    );
    pushSummary(items, '发生工序', firstValue(variables, ['processName']));
    pushSummary(items, '批次号', firstValue(variables, ['lotNo', 'batchNo']));
    pushSummary(
      items,
      '数量',
      firstValue(variables, ['defectQty', 'qty', 'quantity']),
    );
    pushSummary(
      items,
      '不合格等级',
      firstValue(variables, ['ncLevelName', 'ncLevel']),
      'warning',
    );
    pushSummary(
      items,
      '不良描述',
      firstValue(variables, ['ncDescription', 'description', 'defectName']),
    );
    return {
      businessNo: inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 8),
      source: 'fallback',
    };
  }

  if (isDeviceExceptionProcess(instance, variables)) {
    pushSummary(
      items,
      '设备异常单号',
      firstValue(variables, ['deviceExceptionNo', 'exceptionNo', 'orderNo']),
      'strong',
    );
    pushSummary(items, '设备编号', firstValue(variables, ['deviceCode']));
    pushSummary(items, '设备名称', firstValue(variables, ['deviceName']));
    pushSummary(
      items,
      '异常等级',
      formatExceptionLevel(firstValue(variables, ['exceptionLevel'])),
      'warning',
    );
    pushSummary(
      items,
      '提报时间',
      formatSummaryDate(firstValue(variables, ['reportTime']), true),
    );
    pushSummary(items, '异常现象', firstValue(variables, ['faultDesc']));
    return {
      businessNo: inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isExceptionProcess(instance, variables)) {
    pushSummary(
      items,
      '异常类别',
      formatExceptionType(
        firstValue(variables, [
          'exceptionTypeName',
          'exceptionTypeLabel',
          'exceptionType',
        ]),
      ),
    );
    pushSummary(
      items,
      '发现时间',
      formatSummaryDate(firstValue(variables, ['discoverTime']), true),
    );
    pushSummary(items, '发现部门', firstValue(variables, ['discoverDeptName']));
    pushSummary(
      items,
      '异常等级',
      formatExceptionLevel(
        firstValue(variables, [
          'exceptionLevelName',
          'exceptionLevelLabel',
          'exceptionLevel',
        ]),
      ),
      'warning',
    );
    pushSummary(items, '关联产品', resolveExceptionProduct(variables));
    pushSummary(
      items,
      '异常描述',
      firstValue(variables, ['description', 'exceptionDesc']),
    );
    return {
      businessNo: inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isTrialValidationProcess(instance, variables)) {
    const trialNo = firstValue(variables, ['trialNo']);
    pushSummary(items, '跟踪单号', trialNo, 'strong');
    pushSummary(items, '供应商编号', firstValue(variables, ['supplierCode']));
    pushSummary(items, '供应商名称', firstValue(variables, ['supplierName']));
    pushSummary(items, '物料编码', firstValue(variables, ['materialCode']));
    pushSummary(items, '物料名称', firstValue(variables, ['materialName']));
    pushSummary(items, '物料批号', firstValue(variables, ['materialBatchNo']));
    pushSummary(items, '数量', firstValue(variables, ['quantity']));
    return {
      businessNo:
        normalizeDisplayValue(trialNo) ||
        inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 7),
      source: 'fallback',
    };
  }

  if (isSampleEvaluationProcess(instance, variables)) {
    const evaluationNo = firstValue(variables, ['evaluationNo']);
    pushSummary(items, '评价单号', evaluationNo, 'strong');
    pushSummary(items, '供应商编号', firstValue(variables, ['supplierCode']));
    pushSummary(items, '供应商名称', firstValue(variables, ['supplierName']));
    pushSummary(items, '品名', firstValue(variables, ['materialName']));
    pushSummary(items, '规格/型号', firstValue(variables, ['materialModel']));
    pushSummary(
      items,
      '样品需求单',
      firstValue(variables, ['sampleRequestNo']),
    );
    return {
      businessNo:
        normalizeDisplayValue(evaluationNo) ||
        inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isPreliminaryEvaluationProcess(instance, variables)) {
    pushSummary(
      items,
      '初评单号',
      firstValue(variables, ['evaluationNo']),
      'strong',
    );
    pushSummary(items, '供应商编号', firstValue(variables, ['supplierCode']));
    pushSummary(items, '供应商名称', firstValue(variables, ['supplierName']));
    pushSummary(items, '评估模板', firstValue(variables, ['templateName']));
    pushSummary(items, '模板版本', firstValue(variables, ['templateVersion']));
    pushSummary(items, '最终得分', firstValue(variables, ['totalScore']));
    pushSummary(
      items,
      '最终判定',
      formatPreliminaryDecision(firstValue(variables, ['finalDecision'])),
      'warning',
    );
    return {
      businessNo: inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isSampleRequestProcess(instance, variables)) {
    const sampleRequestNo = firstValue(variables, [
      'requestNo',
      'sampleRequestNo',
    ]);
    pushSummary(items, '申请编号', sampleRequestNo, 'strong');
    pushSummary(items, '物料名称', firstValue(variables, ['materialName']));
    pushSummary(items, '型号', firstValue(variables, ['materialModel']));
    pushSummary(items, '使用产品', firstValue(variables, ['usedProduct']));
    pushSummary(items, '指定供应商', firstValue(variables, ['supplierName']));
    pushSummary(items, '申请部门', firstValue(variables, ['applyDept']));
    pushSummary(
      items,
      '申请类型',
      formatSampleRequestApplyType(firstValue(variables, ['applyType'])),
      'warning',
    );
    return {
      businessNo:
        normalizeDisplayValue(sampleRequestNo) ||
        inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isOnboardingApplyProcess(instance, variables)) {
    const applyNo = firstValue(variables, ['applyNo']);
    pushSummary(
      items,
      '导入类型',
      formatOnboardingImportType(firstValue(variables, ['importType'])),
      'warning',
    );
    pushSummary(items, '供应商名称', firstValue(variables, ['supplierName']));
    pushSummary(items, '物料名称', firstValue(variables, ['materialName']));
    pushSummary(items, '型号', firstValue(variables, ['materialModel']));
    pushSummary(
      items,
      '适用产品',
      firstValue(variables, ['applicableProduct']),
    );
    pushSummary(items, '使用部门', firstValue(variables, ['applyDept']));
    return {
      businessNo:
        normalizeDisplayValue(applyNo) ||
        inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  if (isSupplierExitApprovalProcess(instance, variables)) {
    const exitNo = firstValue(variables, ['exitNo']);
    pushSummary(items, '退出单号', exitNo, 'strong');
    pushSummary(items, '供应商名称', firstValue(variables, ['supplierName']));
    pushSummary(items, '供应商代码', firstValue(variables, ['supplierCode']));
    pushSummary(items, '物料名称', firstValue(variables, ['materialName']));
    pushSummary(items, '物料型号', firstValue(variables, ['materialModel']));
    pushSummary(items, '申请部门', firstValue(variables, ['applyDept']));
    return {
      businessNo:
        normalizeDisplayValue(exitNo) ||
        inferBusinessNo(instance, row, variables, items),
      items: limitSummary(items, 6),
      source: 'fallback',
    };
  }

  const bpmItems = normalizeSummary(instance?.summary || row?.summary);
  if (bpmItems.length > 0) {
    return {
      businessNo: inferBusinessNo(instance, row, variables, bpmItems),
      items: bpmItems.slice(0, 4),
      source: 'bpm',
    };
  }

  buildGenericSummary(items, variables);
  return {
    businessNo: inferBusinessNo(instance, row, variables, items),
    items: limitSummary(items),
    source: 'fallback',
  };
}

function normalizeSummary(summary?: { key: string; value: string }[]) {
  if (!Array.isArray(summary)) {
    return [];
  }
  return summary
    .map((item) => ({
      key: normalizeDisplayValue(item?.key),
      value: normalizeDisplayValue(item?.value),
    }))
    .filter((item) => item.key && item.value);
}

function collectFormVariables(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  row?: Record<string, any>,
  businessData?: Record<string, any>,
) {
  return Object.assign(
    {},
    instance?.formVariables,
    row?.formVariables,
    row?.processInstance?.formVariables,
    businessData,
  );
}

function buildGenericSummary(
  items: SummaryItem[],
  variables: Record<string, any>,
) {
  const noKey = findFirstKey(variables, [
    'businessNo',
    'formNo',
    'recordNo',
    'orderNo',
    'planNo',
    'sourceNo',
    'evaluationNo',
  ]);
  pushSummary(items, '单号', noKey ? variables[noKey] : undefined, 'strong');
  pushSummary(
    items,
    '类型',
    firstValue(variables, ['typeName', 'type', 'categoryName', 'category']),
  );
  pushSummary(
    items,
    '等级',
    firstValue(variables, ['levelName', 'level', 'gradeName', 'grade']),
    'warning',
  );
  pushSummary(
    items,
    '物料',
    firstValue(variables, ['materialCode', 'materialName', 'productName']),
  );
  pushSummary(
    items,
    '批次',
    firstValue(variables, ['batchNo', 'lotNo', 'productionBatchNo']),
  );
  pushSummary(
    items,
    '说明',
    firstValue(variables, ['description', 'remark', 'reason', 'content']),
  );
}

function resolveExceptionProduct(variables: Record<string, any>) {
  const relations = Array.isArray(variables.relations)
    ? variables.relations
    : [];
  const relation =
    relations.find((item: Record<string, any>) => item?.primaryFlag) ||
    relations[0];
  const relationText = [relation?.relatedObjectName, relation?.relatedObjectNo]
    .map((item) => normalizeDisplayValue(item))
    .filter(Boolean)
    .join(' / ');
  if (relationText) {
    return relationText;
  }
  const direct = firstValue(variables, [
    'relatedProductName',
    'relatedProductCode',
    'productName',
    'materialName',
    'sourceNo',
  ]);
  if (direct) {
    return direct;
  }
  if (variables.isRelatedProduct === true) {
    return '是';
  }
  if (variables.isRelatedProduct === false) {
    return '否';
  }
  return '';
}

function formatExceptionType(value: any) {
  return formatDictLabel(
    QMS_EXCEPTION_DICT.type,
    EXCEPTION_TYPE_FALLBACK_OPTIONS,
    value,
  );
}

function formatExceptionLevel(value: any) {
  return formatDictLabel(
    QMS_EXCEPTION_DICT.level,
    EXCEPTION_LEVEL_FALLBACK_OPTIONS,
    value,
  );
}

function formatPreliminaryDecision(value: any) {
  const text = normalizeDisplayValue(value);
  return (
    (
      {
        QUALIFIED: '合格',
        UNQUALIFIED: '不合格',
      } as Record<string, string>
    )[text] || text
  );
}

function formatSampleRequestApplyType(value: any) {
  const text = normalizeDisplayValue(value);
  return (
    (
      {
        1: '普通',
        2: '紧急',
        NORMAL: '普通',
        URGENT: '紧急',
      } as Record<string, string>
    )[text] || text
  );
}

function formatOnboardingImportType(value: any) {
  const text = normalizeDisplayValue(value);
  return (
    (
      {
        CUSTOMER_SPECIFIED: '客户指定物料',
        NEW: '全新物料',
        REPLACE: '替代现有物料',
      } as Record<string, string>
    )[text] || text
  );
}

function formatDictLabel(
  dictType: string,
  fallbackOptions: Array<{ label: string; value: string }>,
  value: any,
) {
  const text = normalizeDisplayValue(value);
  if (!text) {
    return '';
  }
  const dictOptions = getDictOptions(dictType, 'string') as Array<{
    label?: string;
    value?: number | string;
  }>;
  const matched = [...dictOptions, ...fallbackOptions].find(
    (item) => normalizeDisplayValue(item.value) === text,
  );
  return normalizeDisplayValue(matched?.label) || text;
}

function formatSummaryDate(value: any, keepTime = false) {
  const text = normalizeDisplayValue(value);
  if (!text || keepTime) {
    return text;
  }
  return /^\d{4}-\d{2}-\d{2}/.test(text) ? text.slice(0, 10) : text;
}

function toNumber(value: any) {
  const text = normalizeDisplayValue(value);
  if (!text) {
    return undefined;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function findFirstKey(variables: Record<string, any>, keys: string[]) {
  const exactKey = keys.find((key) => hasDisplayValue(variables[key]));
  if (exactKey) {
    return exactKey;
  }
  return Object.keys(variables).find(
    (key) => /(?:^|[A-Z_])no$/i.test(key) && hasDisplayValue(variables[key]),
  );
}

function inferBusinessNo(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  row?: Record<string, any>,
  variables: Record<string, any> = {},
  items: SummaryItem[] = [],
) {
  const direct = firstValue(variables, [
    'applyNo',
    'requestNo',
    'sampleRequestNo',
    'ncNo',
    'deviceExceptionNo',
    'exceptionNo',
    'businessNo',
    'formNo',
    'recordNo',
    'orderNo',
    'planNo',
    'sourceNo',
    'evaluationNo',
  ]);
  if (direct) {
    return direct;
  }
  const summaryNo = items.find((item) => isDocumentNoSummaryItem(item));
  if (summaryNo?.value) {
    return summaryNo.value;
  }
  return normalizeDisplayValue(row?.businessKey || instance?.businessKey);
}

function pushSummary(
  items: SummaryItem[],
  key: string,
  value: any,
  tone: SummaryTone = 'default',
) {
  const text = normalizeDisplayValue(value);
  if (!text || items.some((item) => item.key === key && item.value === text)) {
    return;
  }
  items.push({ key, tone, value: text });
}

function limitSummary(items: SummaryItem[], max = 4) {
  return items.filter((item) => item.value).slice(0, max);
}

function getDocumentNoText(row: WorkbenchRow) {
  const direct = normalizeDisplayValue(row.businessNo);
  if (direct) {
    return direct;
  }
  const summaryNo = row.summary.find((item) => isDocumentNoSummaryItem(item));
  return summaryNo?.value || '-';
}

function getDisplaySummaryItems(row: WorkbenchRow) {
  const businessNo = normalizeDisplayValue(row.businessNo);
  return row.summary
    .filter(
      (item) =>
        !(
          businessNo &&
          normalizeDisplayValue(item.value) === businessNo &&
          isDocumentNoSummaryItem(item)
        ),
    )
    .slice(0, 6);
}

function isDocumentNoSummaryItem(item: SummaryItem) {
  return (
    item.key.includes('单号') ||
    item.key.includes('编号') ||
    item.key === '单据'
  );
}

function firstValue(variables: Record<string, any>, keys: string[]) {
  for (const key of keys) {
    const value = normalizeDisplayValue(variables[key]);
    if (value) {
      return value;
    }
  }
  return '';
}

function hasDisplayValue(value: any) {
  return Boolean(normalizeDisplayValue(value));
}

function translateBusinessStatus(value: any) {
  const text = normalizeDisplayValue(value);
  if (!text) {
    return '';
  }
  const statusMap: Record<string, string> = {
    APPROVING: '审批中',
    CANCELLED: '已取消',
    CLOSE_CONFIRM: '关闭确认',
    CLOSED: '已关闭',
    CONTENT_CONFIRM: '再次确认',
    CONTAINMENT: '围堵处理中',
    DRAFT: '草稿',
    EFFECT_CONFIRM: '效果确认',
    EXECUTION_ASSIGN: '分派执行',
    FINAL_APPROVAL: '终审',
    MRB_REVIEW: 'MRB会签',
    PENDING_STOCK_DISPOSE: '待库存处置',
    QUALITY_CONFIRM: '品质确认',
    RETURNED: '已退回',
    REVIEW_ASSIGN: '分派责任单位',
    ROOT_CAUSE: '根因分析',
    SUBMITTED: '已提交',
    VERIFYING: '验证中',
  };
  return statusMap[text] || text;
}

function getStatusIconMeta(row: WorkbenchRow): StatusIconMeta {
  if (row.source === 'copy') {
    return {
      icon: 'lucide:copy-check',
      label: row.statusLabel || '抄送',
      tone: 'copy',
    };
  }
  const label = resolveStatusLabel(row);
  switch (row.statusValue) {
    case BpmProcessInstanceStatus.APPROVE: {
      return { icon: 'lucide:circle-check', label, tone: 'success' };
    }
    case BpmProcessInstanceStatus.CANCEL: {
      return { icon: 'lucide:circle-slash', label, tone: 'muted' };
    }
    case BpmProcessInstanceStatus.REJECT: {
      return { icon: 'lucide:circle-x', label, tone: 'danger' };
    }
    case BpmProcessInstanceStatus.RUNNING: {
      return { icon: 'lucide:clock-3', label, tone: 'running' };
    }
    case BpmTaskStatusEnum.APPROVING: {
      return { icon: 'lucide:check-check', label, tone: 'success' };
    }
    case BpmTaskStatusEnum.NOT_START: {
      return { icon: 'lucide:circle-dashed', label, tone: 'muted' };
    }
    case BpmTaskStatusEnum.RETURN: {
      return { icon: 'lucide:undo-2', label, tone: 'warning' };
    }
    case BpmTaskStatusEnum.SKIP: {
      return { icon: 'lucide:skip-forward', label, tone: 'muted' };
    }
    case BpmTaskStatusEnum.WAIT: {
      return { icon: 'lucide:hourglass', label, tone: 'waiting' };
    }
    default: {
      return { icon: 'lucide:circle-help', label, tone: 'unknown' };
    }
  }
}

function resolveStatusLabel(row: WorkbenchRow) {
  if (row.statusLabel) {
    return row.statusLabel;
  }
  const value = row.statusValue;
  if (value === undefined || value === null) {
    return '-';
  }
  const options =
    row.statusDictType === DICT_TYPE.BPM_TASK_STATUS
      ? taskStatusOptions
      : processStatusOptions;
  const matched = options.find((item) => Number(item.value) === Number(value));
  return normalizeDisplayValue(matched?.label) || getFallbackStatusLabel(value);
}

function getFallbackStatusLabel(value: number) {
  const statusMap: Record<number, string> = {
    [-2]: '跳过',
    [-1]: '未开始',
    0: '待审批',
    1: '审批中',
    2: '审批通过',
    3: '审批不通过',
    4: '已取消',
    5: '已退回',
    7: '审批通过中',
  };
  return statusMap[value] || String(value);
}

function normalizeDisplayValue(value: any): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => normalizeDisplayValue(item))
      .filter(Boolean)
      .join('、');
  }
  if (typeof value === 'object') {
    const direct =
      value.name || value.label || value.title || value.nickname || value.value;
    if (direct !== undefined && direct !== null) {
      return normalizeDisplayValue(direct);
    }
    return '';
  }
  return String(value).trim();
}

function getProcessDefinitionKey(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const key = instance?.processDefinition?.key;
  if (key) {
    return key;
  }
  return instance?.processDefinitionId?.split(':')[0] || '';
}

function isExceptionProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = {},
) {
  const key = getProcessDefinitionKey(instance);
  return (
    EXCEPTION_PROCESS_KEYS.has(key) ||
    EXCEPTION_FORM_PATHS.has(
      instance?.processDefinition?.formCustomViewPath || '',
    ) ||
    Boolean(variables.exceptionNo || variables.exceptionEventId)
  );
}

function isDeviceExceptionProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = {},
) {
  const key = getProcessDefinitionKey(instance);
  return (
    DEVICE_EXCEPTION_PROCESS_KEYS.has(key) ||
    DEVICE_EXCEPTION_FORM_PATHS.has(
      instance?.processDefinition?.formCustomViewPath || '',
    ) ||
    variables.mesWorkflowBusinessType === 'DEVICE_EXCEPTION' ||
    Boolean(variables.deviceExceptionId || variables.deviceExceptionNo)
  );
}

function getTaskAssigneeText(tasks?: BpmProcessInstanceApi.Task[]) {
  if (!tasks || tasks.length === 0) {
    return '';
  }
  const names = tasks
    .map((task) => task.assigneeUser?.nickname)
    .filter(Boolean);
  if (names.length === 0) {
    return '';
  }
  return names.length > 2
    ? `${names.slice(0, 2).join('、')}等`
    : names.join('、');
}

function handleTabChange(key: number | string) {
  activeTab.value = key as WorkbenchTabKey;
  selectedRowKey.value = '';
  filters.status = undefined;
  pagination.current = 1;
  void loadCounts();
  void loadTable();
}

function handleSearch() {
  pagination.current = 1;
  void loadCounts();
  void loadTable();
}

function handleRefresh() {
  void loadCounts();
  void loadTable();
}

function handlePaginationChange(current: number, pageSize: number) {
  pagination.current = current || 1;
  pagination.pageSize = pageSize || 10;
  void loadTable();
}

function formatPaginationTotal(total: number) {
  return `共 ${total} 条`;
}

function handleSelectRow(record: WorkbenchRow) {
  selectedRowKey.value = record.key;
}

function getRowClassName(record: WorkbenchRow) {
  return record.key === selectedRowKey.value
    ? 'approval-workbench__row--active'
    : '';
}

function isTodoActionRow(record: WorkbenchRow) {
  return record.source === 'todo';
}

async function handleOpenBusiness(row: WorkbenchRow) {
  actionLoadingKey.value = row.key;
  try {
    if (row.source === 'mine') {
      await openInstanceDetail(
        row.raw as BpmProcessInstanceApi.ProcessInstance,
      );
    } else if (row.source === 'copy') {
      await openCopyDetail(
        row.raw as BpmProcessInstanceApi.ProcessInstanceCopyRespVO,
      );
    } else {
      await openTaskDetail(
        row.raw as BpmTaskApi.Task,
        resolveBusinessDetailTabType(row.source),
      );
    }
  } finally {
    actionLoadingKey.value = '';
  }
}

function resolveBusinessDetailTabType(source: WorkbenchRowSource) {
  if (source === 'todo') {
    return 'todo';
  }
  if (source === 'done') {
    return 'processed';
  }
  return undefined;
}

async function openTaskDetail(row: BpmTaskApi.Task, tabType?: string) {
  const instance = await resolveTaskInstance(row);
  if (tryOpenNcrDetail(instance, tabType)) {
    return;
  }
  if (tryOpenDeviceExceptionDetail(instance, tabType)) {
    return;
  }
  if (tryOpenExceptionDetail(instance, tabType)) {
    return;
  }
  if (tryOpenTrialValidationDetail(instance, row.id, tabType)) {
    return;
  }
  if (await tryOpenSampleEvaluationDetail(instance, row.id, tabType)) {
    return;
  }
  if (await tryOpenSampleRequestDetail(instance, row.id, tabType)) {
    return;
  }
  if (tryOpenOnboardingApplyDetail(instance, row.id, tabType)) {
    return;
  }
  if (tryOpenSupplierExitApprovalDetail(instance, row.id, tabType)) {
    return;
  }
  businessFormModalApi
    .setData({
      businessKey: instance?.businessKey,
      formVariables: instance?.formVariables,
      processDefinition: instance?.processDefinition,
      processInstanceId: instance?.id || row.processInstanceId,
      processInstance: instance,
      taskId: row.id,
      title: `单据详情 - ${instance?.name || row.processInstanceId}`,
    })
    .open();
}

async function resolveTaskInstance(row: BpmTaskApi.Task) {
  const instance = row.processInstance;
  if (
    instance &&
    (!(
      isNcrProcess(instance) ||
      isDeviceExceptionProcess(instance) ||
      isExceptionProcess(instance) ||
      isTrialValidationProcess(instance) ||
      isSampleEvaluationProcess(instance) ||
      isSampleRequestProcess(instance) ||
      isOnboardingApplyProcess(instance) ||
      isSupplierExitApprovalProcess(instance)
    ) ||
      resolveBusinessKey(instance))
  ) {
    return instance;
  }
  return await getProcessInstance(row.processInstanceId);
}

async function openInstanceDetail(row: BpmProcessInstanceApi.ProcessInstance) {
  const instance =
    !(
      isNcrProcess(row) ||
      isDeviceExceptionProcess(row) ||
      isExceptionProcess(row) ||
      isTrialValidationProcess(row) ||
      isSampleEvaluationProcess(row) ||
      isSampleRequestProcess(row) ||
      isOnboardingApplyProcess(row) ||
      isSupplierExitApprovalProcess(row)
    ) || resolveBusinessKey(row)
      ? row
      : await getProcessInstance(row.id);
  if (tryOpenNcrDetail(instance)) {
    return;
  }
  if (tryOpenDeviceExceptionDetail(instance)) {
    return;
  }
  if (tryOpenExceptionDetail(instance)) {
    return;
  }
  if (tryOpenTrialValidationDetail(instance)) {
    return;
  }
  if (await tryOpenSampleEvaluationDetail(instance)) {
    return;
  }
  if (await tryOpenSampleRequestDetail(instance)) {
    return;
  }
  if (tryOpenOnboardingApplyDetail(instance)) {
    return;
  }
  if (tryOpenSupplierExitApprovalDetail(instance)) {
    return;
  }
  businessFormModalApi
    .setData({
      businessKey: instance.businessKey,
      formVariables: instance.formVariables,
      processDefinition: instance.processDefinition,
      processInstanceId: instance.id,
      processInstance: instance,
      title: `单据详情 - ${instance.name || instance.id}`,
    })
    .open();
}

async function openCopyDetail(
  row: BpmProcessInstanceApi.ProcessInstanceCopyRespVO,
) {
  const instance = await getProcessInstance(row.processInstanceId);
  if (tryOpenNcrDetail(instance)) {
    return;
  }
  if (tryOpenDeviceExceptionDetail(instance)) {
    return;
  }
  if (tryOpenExceptionDetail(instance)) {
    return;
  }
  if (tryOpenTrialValidationDetail(instance, row.taskId)) {
    return;
  }
  if (await tryOpenSampleEvaluationDetail(instance, row.taskId)) {
    return;
  }
  if (await tryOpenSampleRequestDetail(instance, row.taskId)) {
    return;
  }
  if (tryOpenOnboardingApplyDetail(instance, row.taskId)) {
    return;
  }
  if (tryOpenSupplierExitApprovalDetail(instance, row.taskId)) {
    return;
  }
  businessFormModalApi
    .setData({
      activityId: row.activityId,
      businessKey: instance?.businessKey,
      formVariables: instance?.formVariables,
      processDefinition: instance?.processDefinition,
      processInstanceId: row.processInstanceId,
      processInstance: instance,
      taskId: row.taskId,
      title: `单据详情 - ${row.processInstanceName || row.processInstanceId}`,
    })
    .open();
}

function tryOpenNcrDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isNcrProcess(instance) || !businessKey) {
    return false;
  }
  ncrDetailModalApi.setData({ id: Number(businessKey), tabType }).open();
  return true;
}

function tryOpenDeviceExceptionDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (
    !isDeviceExceptionProcess(instance, collectFormVariables(instance)) ||
    !businessKey
  ) {
    return false;
  }
  deviceExceptionDetailModalApi
    .setData({ id: Number(businessKey), tabType })
    .open();
  return true;
}

function tryOpenExceptionDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (
    !isExceptionProcess(instance, collectFormVariables(instance)) ||
    !businessKey
  ) {
    return false;
  }
  exceptionDetailModalApi.setData({ id: Number(businessKey), tabType }).open();
  return true;
}

function tryOpenTrialValidationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isTrialValidationProcess(instance) || !businessKey) {
    return false;
  }
  trialValidationDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

async function tryOpenSampleRequestDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isSampleRequestProcess(instance) || !businessKey) {
    return false;
  }
  sampleRequestDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

async function tryOpenSampleEvaluationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isSampleEvaluationProcess(instance) || !businessKey) {
    return false;
  }
  sampleEvaluationDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function tryOpenOnboardingApplyDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveOnboardingApplyBusinessKey(instance);
  if (!isOnboardingApplyProcess(instance) || !businessKey) {
    return false;
  }
  onboardingApplyDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function tryOpenSupplierExitApprovalDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveSupplierExitApprovalBusinessKey(instance);
  if (!isSupplierExitApprovalProcess(instance) || !businessKey) {
    return false;
  }
  supplierExitApprovalDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function isNcrProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    NCR_PROCESS_KEYS.includes(processKey) ||
    NCR_PROCESS_KEYS.some((key) => processDefinitionId.startsWith(`${key}:`)) ||
    NCR_FORM_PATHS.has(formPath) ||
    Boolean(variables.ncNo || variables.ncRecordId)
  );
}

function isPreliminaryEvaluationProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    PRELIMINARY_PROCESS_KEYS.includes(processKey) ||
    PRELIMINARY_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    PRELIMINARY_FORM_PATHS.has(formPath) ||
    Boolean(
      !variables.sampleEvaluationId &&
      !variables.trialValidationId &&
      (variables.evaluationId || variables.evaluationNo),
    )
  );
}

function isTrialValidationProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    TRIAL_VALIDATION_PROCESS_KEYS.includes(processKey) ||
    TRIAL_VALIDATION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    TRIAL_VALIDATION_FORM_PATHS.has(formPath) ||
    Boolean(variables.trialValidationId || variables.trialNo)
  );
}

function isSampleEvaluationProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SAMPLE_EVALUATION_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_EVALUATION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SAMPLE_EVALUATION_FORM_PATHS.has(formPath) ||
    Boolean(variables.sampleEvaluationId)
  );
}

function isSampleRequestProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SAMPLE_REQUEST_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_REQUEST_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SAMPLE_REQUEST_FORM_PATHS.has(formPath) ||
    Boolean(
      (variables.sampleRequestId && !variables.sampleEvaluationId) ||
      variables.requestNo,
    )
  );
}

function isOnboardingApplyProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    ONBOARDING_APPLY_PROCESS_KEYS.includes(processKey) ||
    ONBOARDING_APPLY_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    ONBOARDING_APPLY_FORM_PATHS.has(formPath) ||
    Boolean(variables.onboardingApplyId || variables.applyNo)
  );
}

function isSupplierExitApprovalProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  variables: Record<string, any> = collectFormVariables(instance),
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.includes(processKey) ||
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SUPPLIER_EXIT_APPROVAL_FORM_PATHS.has(formPath) ||
    Boolean(variables.supplierExitApprovalId || variables.exitNo)
  );
}

function resolveBusinessKey(instance?: BpmProcessInstanceApi.ProcessInstance) {
  return (
    instance?.businessKey ||
    instance?.formVariables?.deviceExceptionId ||
    instance?.formVariables?.trialValidationId ||
    instance?.formVariables?.onboardingApplyId ||
    instance?.formVariables?.supplierExitApprovalId ||
    instance?.formVariables?.sampleEvaluationId ||
    instance?.formVariables?.sampleRequestId ||
    instance?.formVariables?.exceptionEventId ||
    instance?.formVariables?.ncRecordId ||
    instance?.formVariables?.evaluationId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id
  );
}

function resolveOnboardingApplyBusinessKey(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  return (
    instance?.businessKey ||
    instance?.formVariables?.onboardingApplyId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id
  );
}

function resolveSupplierExitApprovalBusinessKey(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  return (
    instance?.businessKey ||
    instance?.formVariables?.supplierExitApprovalId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id
  );
}

async function handleOpenTrace(row: WorkbenchRow) {
  await router.push({
    name: 'BpmProcessInstanceDetail',
    query: {
      id: row.processInstanceId,
      ...(row.taskId && !isMesWorkflowVirtualTaskId(row.taskId)
        ? { taskId: row.taskId }
        : {}),
      ...(row.activityId ? { activityId: row.activityId } : {}),
    },
  });
}

function isMesWorkflowVirtualTaskId(taskId?: string) {
  return normalizeDisplayValue(taskId).startsWith('mes-');
}

function handleModalSuccess() {
  message.success('流程事项已刷新');
  handleRefresh();
}

const [BusinessFormDetailModal, businessFormModalApi] = useVbenModal({
  connectedComponent: BusinessFormModal,
  destroyOnClose: true,
});

const [NcrDetailModalHost, ncrDetailModalApi] = useVbenModal({
  connectedComponent: NcrDetailModal,
  destroyOnClose: true,
});

const [ExceptionDetailModalHost, exceptionDetailModalApi] = useVbenModal({
  connectedComponent: ExceptionDetailModal,
  destroyOnClose: true,
});

const [DeviceExceptionDetailModalHost, deviceExceptionDetailModalApi] =
  useVbenModal({
    connectedComponent: DeviceExceptionDetailModal,
    destroyOnClose: true,
  });

const [SampleRequestDetailModalHost, sampleRequestDetailModalApi] =
  useVbenModal({
    connectedComponent: SampleRequestDetailModal,
    destroyOnClose: true,
  });

const [SampleEvaluationDetailModalHost, sampleEvaluationDetailModalApi] =
  useVbenModal({
    connectedComponent: SampleEvaluationDetailModal,
    destroyOnClose: true,
  });

const [TrialValidationDetailModalHost, trialValidationDetailModalApi] =
  useVbenModal({
    connectedComponent: TrialValidationDetailModal,
    destroyOnClose: true,
  });

const [OnboardingApplyDetailModalHost, onboardingApplyDetailModalApi] =
  useVbenModal({
    connectedComponent: OnboardingApplyDetailModal,
    destroyOnClose: true,
  });

const [
  SupplierExitApprovalDetailModalHost,
  supplierExitApprovalDetailModalApi,
] = useVbenModal({
  connectedComponent: SupplierExitApprovalDetailModal,
  destroyOnClose: true,
});

onMounted(() => {
  void loadCounts();
  void loadTable();
});
</script>

<template>
  <Page auto-content-height class="approval-workbench-page">
    <BusinessFormDetailModal @success="handleModalSuccess" />
    <NcrDetailModalHost @success="handleModalSuccess" />
    <ExceptionDetailModalHost @success="handleModalSuccess" />
    <DeviceExceptionDetailModalHost @success="handleModalSuccess" />
    <SampleRequestDetailModalHost @success="handleModalSuccess" />
    <SampleEvaluationDetailModalHost @success="handleModalSuccess" />
    <TrialValidationDetailModalHost @success="handleModalSuccess" />
    <OnboardingApplyDetailModalHost @success="handleModalSuccess" />
    <SupplierExitApprovalDetailModalHost @success="handleModalSuccess" />

    <div class="approval-workbench">
      <Card :bordered="false" class="approval-workbench__panel">
        <div class="approval-workbench__layout">
          <section class="approval-workbench__main">
            <div class="approval-workbench__topbar">
              <div class="approval-workbench__heading">
                <div class="approval-workbench__title">
                  <IconifyIcon :icon="activeTabMeta.icon" />
                  <span>{{ activeTabMeta.title }}</span>
                </div>
              </div>
              <div class="approval-workbench__filters">
                <Input
                  v-model:value="filters.keyword"
                  allow-clear
                  class="approval-workbench__keyword"
                  placeholder="流程/任务名称"
                  @press-enter="handleSearch"
                />
                <Select
                  v-if="activeTab !== 'copy'"
                  v-model:value="filters.status"
                  allow-clear
                  class="approval-workbench__select"
                  :options="activeStatusOptions"
                  placeholder="状态"
                />
                <RangePicker
                  v-model:value="filters.createTime"
                  class="approval-workbench__range"
                  :placeholder="['开始日期', '结束日期']"
                  :format="DATE_FORMAT"
                  :value-format="DATE_FORMAT"
                />
                <Button type="primary" @click="handleSearch">
                  <IconifyIcon icon="lucide:search" />
                  查询
                </Button>
              </div>
              <div class="approval-workbench__pill-row">
                <div class="approval-workbench__pills">
                  <button
                    v-for="item in tabItems"
                    :key="item.key"
                    class="approval-workbench__pill"
                    :class="{ 'is-active': activeTab === item.key }"
                    type="button"
                    @click="handleTabChange(item.key)"
                  >
                    <IconifyIcon :icon="item.icon" />
                    <span>{{ item.title }}</span>
                    <b>{{ counts[item.key] }}</b>
                  </button>
                </div>
              </div>
            </div>

            <div class="approval-workbench__body">
              <div class="approval-workbench__table-shell">
                <Spin
                  :spinning="loading"
                  wrapper-class-name="approval-workbench__spin"
                >
                  <div class="approval-workbench__grid">
                    <div class="approval-workbench__grid-head">
                      <span>流程标题</span>
                      <span>单据编号</span>
                      <span>关键摘要</span>
                      <span>当前节点</span>
                      <span>状态</span>
                      <span>操作</span>
                    </div>

                    <div class="approval-workbench__grid-body">
                      <div
                        v-for="record in rows"
                        :key="record.key"
                        class="approval-workbench__grid-row"
                        :class="getRowClassName(record)"
                        role="button"
                        tabindex="0"
                        @click="handleSelectRow(record)"
                        @keydown.enter.space="handleSelectRow(record)"
                      >
                        <span class="approval-workbench__matter">
                          <span class="approval-workbench__matter-title">
                            {{ record.processName }}
                          </span>
                        </span>

                        <span class="approval-workbench__document-no">
                          {{ getDocumentNoText(record) }}
                        </span>

                        <span class="approval-workbench__summary-line">
                          <template
                            v-if="getDisplaySummaryItems(record).length > 0"
                          >
                            <span
                              v-for="item in getDisplaySummaryItems(record)"
                              :key="`${record.key}-${item.key}`"
                            >
                              <small>{{ item.key }}</small>
                              <b>{{ item.value }}</b>
                            </span>
                          </template>
                          <span v-else class="approval-workbench__empty-text">
                            未配置摘要
                          </span>
                        </span>

                        <span class="approval-workbench__single-cell">
                          {{ record.currentNodeName || '-' }}
                        </span>

                        <span
                          class="approval-workbench__status"
                          :aria-label="getStatusIconMeta(record).label"
                          :title="getStatusIconMeta(record).label"
                        >
                          <span
                            class="approval-workbench__status-icon"
                            :class="`is-${getStatusIconMeta(record).tone}`"
                          >
                            <IconifyIcon
                              :icon="getStatusIconMeta(record).icon"
                            />
                          </span>
                        </span>

                        <Space class="approval-workbench__actions">
                          <Button
                            :loading="actionLoadingKey === record.key"
                            :type="
                              isTodoActionRow(record) ? 'primary' : 'default'
                            "
                            size="small"
                            @click.stop="handleOpenBusiness(record)"
                          >
                            <IconifyIcon
                              :icon="
                                isTodoActionRow(record)
                                  ? 'lucide:circle-play'
                                  : 'lucide:eye'
                              "
                            />
                            {{ isTodoActionRow(record) ? '办理' : '查看' }}
                          </Button>
                          <Button
                            size="small"
                            @click.stop="handleOpenTrace(record)"
                          >
                            <IconifyIcon icon="lucide:route" />
                          </Button>
                        </Space>
                      </div>

                      <div
                        v-if="rows.length === 0 && !loading"
                        class="approval-workbench__grid-empty"
                      >
                        <Empty description="暂无流程事项" />
                      </div>
                    </div>

                    <div class="approval-workbench__grid-footer">
                      <div class="approval-workbench__status-legend">
                        <span class="approval-workbench__status-legend-title">
                          图标说明
                        </span>
                        <span
                          v-for="item in statusLegendItems"
                          :key="item.label"
                          class="approval-workbench__legend-item"
                        >
                          <span
                            class="approval-workbench__status-icon"
                            :class="`is-${item.tone}`"
                          >
                            <IconifyIcon :icon="item.icon" />
                          </span>
                          <span>{{ item.label }}</span>
                        </span>
                      </div>
                      <Pagination
                        :current="pagination.current"
                        :page-size="pagination.pageSize"
                        show-size-changer
                        :show-total="formatPaginationTotal"
                        :total="pagination.total"
                        @change="handlePaginationChange"
                      />
                    </div>
                  </div>
                </Spin>
              </div>
            </div>
          </section>
        </div>
      </Card>
    </div>
  </Page>
</template>

<style scoped>
.approval-workbench-page {
  height: 100%;
  min-height: 0;
  background: #f3f5f8;
}

.approval-workbench-page :deep(.vben-page-content),
.approval-workbench-page :deep(.vben-page-wrapper),
.approval-workbench-page :deep(.page-content),
.approval-workbench-page :deep(.page-wrapper) {
  padding: 0 !important;
}

.approval-workbench {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 118px);
  min-height: 520px;
  padding: 36px 44px;
}

.approval-workbench__panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
}

.approval-workbench__toolbar {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.approval-workbench__active {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
  line-height: 32px;
  white-space: nowrap;
}

.approval-workbench__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.approval-workbench__keyword {
  width: 256px;
}

.approval-workbench__select {
  width: 202px;
}

.approval-workbench__range {
  width: 350px;
}

.approval-workbench__tabs {
  flex: none;
  margin-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}

.approval-workbench__table-shell {
  flex: 1;
  min-height: 0;
}

.approval-workbench__matter {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.approval-workbench__matter-title {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__matter-title > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__source-tag {
  flex: none;
}

.approval-workbench__matter-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #64748b;
  font-size: 12px;
}

.approval-workbench__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.approval-workbench__summary-chip {
  display: flex;
  min-width: 0;
  min-height: 38px;
  align-items: center;
  padding: 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  background: #fbfcfe;
}

.approval-workbench__summary-chip small {
  flex: none;
  width: 82px;
  color: #64748b;
  font-size: 12px;
}

.approval-workbench__summary-chip b {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__summary-chip--strong {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.approval-workbench__summary-chip--warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.approval-workbench__summary-chip--danger {
  border-color: #fecaca;
  background: #fef2f2;
}

.approval-workbench__summary-empty {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  color: #94a3b8;
}

.approval-workbench__stage {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.approval-workbench__stage div,
.approval-workbench__status {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
}

.approval-workbench__stage small,
.approval-workbench__reason {
  color: #94a3b8;
}

.approval-workbench__status {
  width: 100%;
  justify-content: center;
}

.approval-workbench__status-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid currentcolor;
  border-radius: 999px;
  background: #fff;
  font-size: 14px;
}

.approval-workbench__status-icon.is-running {
  color: #1677ff;
  background: #eff6ff;
}

.approval-workbench__status-icon.is-waiting {
  color: #b7791f;
  background: #fffbeb;
}

.approval-workbench__status-icon.is-success {
  color: #15803d;
  background: #ecfdf3;
}

.approval-workbench__status-icon.is-danger {
  color: #dc2626;
  background: #fef2f2;
}

.approval-workbench__status-icon.is-warning {
  color: #d97706;
  background: #fff7ed;
}

.approval-workbench__status-icon.is-muted,
.approval-workbench__status-icon.is-unknown {
  color: #64748b;
  background: #f8fafc;
}

.approval-workbench__status-icon.is-copy {
  color: #7c3aed;
  background: #f5f3ff;
}

.approval-workbench__stage span {
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__reason {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  font-size: 12px;
  -webkit-line-clamp: 2;
}

.approval-workbench__actions {
  width: 100%;
}

.approval-workbench :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 22px;
}

.approval-workbench :deep(.ant-btn) {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

@media (max-width: 1280px) {
  .approval-workbench__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .approval-workbench__filters {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .approval-workbench {
    padding: 10px;
  }

  .approval-workbench__keyword,
  .approval-workbench__select,
  .approval-workbench__range {
    width: 100%;
  }

  .approval-workbench__summary-grid {
    grid-template-columns: 1fr;
  }
}

.approval-workbench {
  height: calc(100vh - 118px);
  min-height: 0;
  padding: 2px 4px;
}

.approval-workbench__panel {
  overflow: hidden;
  border: 1px solid #e5eaf0;
  border-radius: 6px;
  background: #fff;
}

.approval-workbench :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 0;
}

.approval-workbench__layout {
  display: grid;
  width: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(0, 1fr);
}

.approval-workbench__main {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.approval-workbench__topbar {
  display: grid;
  gap: 8px 14px;
  align-items: start;
  padding: 8px 10px 7px;
  border-bottom: 1px solid #e8edf3;
  grid-template-columns: auto 1fr;
}

.approval-workbench__heading {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 430px;
}

.approval-workbench__title {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
  white-space: nowrap;
}

.approval-workbench__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.approval-workbench__pill-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  grid-column: 1 / -1;
}

.approval-workbench__pill {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dce4ee;
  border-radius: 999px;
  background: #fff;
  color: #42526a;
  cursor: pointer;
  font-weight: 600;
}

.approval-workbench__pill:hover {
  color: #056de8;
  border-color: #9cc8ff;
  background: #f5f9ff;
}

.approval-workbench__pill.is-active {
  color: #056de8;
  border-color: #82bbff;
  background: #e8f2ff;
}

.approval-workbench__pill b {
  min-width: 18px;
  text-align: right;
}

.approval-workbench__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
  justify-content: flex-end;
  padding-top: 4px;
}

.approval-workbench__keyword {
  width: 210px;
}

.approval-workbench__select {
  width: 160px;
}

.approval-workbench__range {
  width: 292px;
}

.approval-workbench__body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.approval-workbench__table-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 4px;
}

.approval-workbench__matter {
  gap: 5px;
}

.approval-workbench__matter-title {
  color: #172033;
  font-weight: 700;
}

.approval-workbench__matter-meta {
  gap: 12px;
  color: #8b98aa;
}

.approval-workbench__summary-line {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 5px 14px;
  line-height: 1.55;
}

.approval-workbench__summary-line span {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
  gap: 5px;
}

.approval-workbench__summary-line small {
  flex: none;
  color: #8b98aa;
}

.approval-workbench__summary-line b {
  min-width: 0;
  overflow: hidden;
  color: #27364a;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__single-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #27364a;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__document-no {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #0f4c81;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-workbench__empty-text {
  color: #a3adbb;
}

.approval-workbench__actions {
  display: flex;
  justify-content: center;
}

.approval-workbench__spin,
.approval-workbench__spin :deep(.ant-spin-nested-loading),
.approval-workbench__spin :deep(.ant-spin-container) {
  display: flex;
  flex: 1;
  min-height: 0;
}

.approval-workbench__grid {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d7dee9;
  border-radius: 8px;
  background: #fff;
}

.approval-workbench__grid-head,
.approval-workbench__grid-row {
  display: grid;
  min-width: 0;
  grid-template-columns:
    minmax(190px, 0.95fr) minmax(150px, 0.78fr) minmax(0, 1.75fr)
    minmax(112px, 0.52fr) 68px 144px;
}

.approval-workbench__grid-head {
  flex: none;
  height: 42px;
  align-items: center;
  color: #667085;
  font-size: 14px;
  font-weight: 700;
  background: #f6f8fb;
  border-bottom: 1px solid #d7dee9;
}

.approval-workbench__grid-head span,
.approval-workbench__grid-row > span,
.approval-workbench__grid-row > .approval-workbench__actions {
  min-width: 0;
  padding: 0 12px;
  border-right: 1px solid #e1e7ef;
}

.approval-workbench__grid-head span:last-child,
.approval-workbench__grid-row > span:last-child,
.approval-workbench__grid-row > .approval-workbench__actions:last-child {
  border-right: 0;
}

.approval-workbench__grid-head span:nth-child(5),
.approval-workbench__grid-head span:last-child {
  text-align: center;
}

.approval-workbench__grid-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.approval-workbench__grid-row {
  width: 100%;
  min-height: 104px;
  align-items: center;
  border: 0;
  border-bottom: 1px solid #e1e7ef;
  background: #fff;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.approval-workbench__grid-row:hover {
  background: #f8fbff;
}

.approval-workbench__grid-row.approval-workbench__row--active {
  background: #f2f7ff;
}

.approval-workbench__grid-empty {
  display: flex;
  height: 100%;
  min-height: 260px;
  align-items: center;
  justify-content: center;
}

.approval-workbench__grid-footer {
  display: flex;
  flex: none;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  min-height: 46px;
  padding: 7px 10px;
  border-top: 1px solid #d7dee9;
  background: #fff;
}

.approval-workbench__status-legend {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  min-width: 0;
  color: #536176;
  font-size: 12px;
}

.approval-workbench__status-legend-title {
  color: #8b98aa;
  font-weight: 700;
}

.approval-workbench__legend-item {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  white-space: nowrap;
}

.approval-workbench__legend-item .approval-workbench__status-icon {
  width: 18px;
  height: 18px;
  font-size: 12px;
}

@media (max-width: 1360px) {
  .approval-workbench__range {
    width: 248px;
  }
}

@media (max-width: 1180px) {
  .approval-workbench__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .approval-workbench__topbar {
    grid-template-columns: 1fr;
  }

  .approval-workbench__heading {
    min-width: 0;
  }

  .approval-workbench__filters {
    justify-content: flex-start;
  }

  .approval-workbench__pill-row {
    align-items: stretch;
    flex-direction: column;
  }
}

.approval-workbench__panel,
.approval-workbench :deep(.ant-card-body),
.approval-workbench__layout,
.approval-workbench__main,
.approval-workbench__body,
.approval-workbench__table-shell,
.approval-workbench__spin,
.approval-workbench__spin :deep(.ant-spin-nested-loading),
.approval-workbench__spin :deep(.ant-spin-container),
.approval-workbench__grid {
  height: 100%;
  min-height: 0;
}

.approval-workbench__layout,
.approval-workbench__main,
.approval-workbench__body,
.approval-workbench__table-shell,
.approval-workbench__spin,
.approval-workbench__spin :deep(.ant-spin-nested-loading),
.approval-workbench__spin :deep(.ant-spin-container),
.approval-workbench__grid {
  flex: 1;
}

.approval-workbench__topbar {
  grid-template-columns: auto minmax(0, 1fr);
}

.approval-workbench__heading {
  min-width: 180px;
}

.approval-workbench__filters {
  flex-wrap: nowrap;
  gap: 8px;
  overflow: hidden;
  white-space: nowrap;
}

.approval-workbench__filters > * {
  flex: none;
}

.approval-workbench__keyword {
  width: 220px;
}

.approval-workbench__select {
  width: 164px;
}

.approval-workbench__range {
  width: 220px;
}

.approval-workbench__range :deep(.ant-picker-input) {
  min-width: 76px;
}

.approval-workbench__range :deep(.ant-picker-input > input) {
  text-align: center;
}

.approval-workbench__range :deep(.ant-picker-separator) {
  padding: 0 2px;
}

.approval-workbench__pills {
  flex: 1;
  min-width: 0;
}

.approval-workbench__pill-row .approval-workbench__pills {
  grid-column: 1 / -1;
  flex-wrap: nowrap;
  overflow-x: auto;
  padding-bottom: 2px;
}

.approval-workbench__grid-body {
  flex: 1 1 auto;
}

.approval-workbench__grid-footer {
  margin-top: auto;
}
</style>
