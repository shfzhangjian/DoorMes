<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { SrmSupplierCandidateApi } from '#/api/mes/srm/supplier-candidate';
import type { SrmSupplierReviewPlanApi } from '#/api/mes/srm/supplier-review-plan';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Empty,
  Input,
  message,
  Modal,
  Pagination,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  addSupplierReviewLine,
  adjustSupplierReviewMonthStatus,
  clearSupplierReviewMonthPlan,
  createSupplierReviewReply,
  deleteSupplierReviewAnnualPlans,
  getSupplierReviewMonthPlan,
  getSupplierReviewYearPlan,
  initSupplierReviewYearPlan,
  updateSupplierReviewLineContact,
  updateSupplierReviewMonthPlan,
} from '#/api/mes/srm/supplier-review-plan';
import SrmAttachmentPanel from '#/views/mes/srm/shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '#/views/mes/srm/shared/SrmReferenceSelectModal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmAuditReviewPlan' });

type Line = SrmSupplierReviewPlanApi.PlanLine;
type MonthPlan = SrmSupplierReviewPlanApi.MonthPlan;
type UserSnapshot = SrmSupplierReviewPlanApi.UserSnapshot;

const CURRENT_YEAR = new Date().getFullYear();
const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);
const MODAL_Z_INDEX = 5200;
const DELETE_ANNUAL_PLAN_PERMISSION = 'mes:srm-audit-review-plan:delete-year';

const statusOptions = [
  { label: '计划', value: 'PLAN', color: 'blue' },
  { label: '执行', value: 'EXECUTING', color: 'orange' },
  { label: '变更', value: 'CHANGED', color: 'purple' },
  { label: '取消', value: 'CANCELED', color: 'red' },
  { label: '完成', value: 'COMPLETED', color: 'green' },
  { label: '归档', value: 'ARCHIVED', color: 'success' },
];

const attachmentCategoryOptions = [
  { label: '评审计划说明', value: 'REVIEW_PLAN_DESC' },
  { label: '月度调整附件', value: 'REVIEW_PLAN_CHANGE' },
  { label: '评审执行资料', value: 'REVIEW_EXECUTION_REPLY' },
  { label: '其他附件', value: 'OTHER' },
];

const loading = ref(false);
const addingSuppliers = ref(false);
const savingMonth = ref(false);
const clearingMonthId = ref<number>();
const deletingAnnualPlans = ref(false);
const savingStatus = ref(false);
const savingReply = ref(false);
const currentYear = ref(CURRENT_YEAR);
const tableCardRef = ref<HTMLElement>();
const tableBodyHeight = ref(360);
const yearPlan = ref<SrmSupplierReviewPlanApi.YearPlan>({
  lines: [],
  planYear: CURRENT_YEAR,
});

const queryForm = reactive({
  executionStatus: '',
  materialCode: '',
  supplierKeyword: '',
});

const monthModalOpen = ref(false);
const statusModalOpen = ref(false);
const replyModalOpen = ref(false);
const currentLine = ref<Line>();
const monthDetail = ref<MonthPlan>();
const monthAttachmentRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const replyAttachmentRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const userStore = useUserStore();
const { hasAccessByCodes } = useAccess();

const monthForm = reactive({
  approvalOpinion: '',
  approverUserId: undefined as number | undefined,
  approverUserName: '',
  auditDate: '',
  leadUserId: undefined as number | undefined,
  leadUserName: '',
  planDesc: '',
  relatedUsers: [] as UserSnapshot[],
  remark: '',
});

const statusForm = reactive({
  executionStatus: 'PLAN',
  statusRemark: '',
  updateDescription: '',
});

const replyForm = reactive({
  recorderUserName: '',
  remark: '',
  reviewDate: '',
  reviewResult: '',
});

const userSelectPurpose = ref<'approver' | 'lead' | 'related'>('lead');

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

let tableResizeObserver: ResizeObserver | undefined;

type YearPlanTableRow = Line & Record<string, unknown>;

const selectedAnnualLineKeys = ref<Array<number | string>>([]);
const selectedAnnualLines = ref<YearPlanTableRow[]>([]);

const yearColumns = computed<TableColumnsType<YearPlanTableRow>>(() => {
  const monthColumns: TableColumnsType<YearPlanTableRow> = MONTHS.map(
    (month) => ({
      align: 'center' as const,
      dataIndex: `month${month}`,
      key: `month${month}`,
      title: `${month}月`,
      width: 116,
    }),
  );
  return [
    {
      align: 'center' as const,
      dataIndex: 'rowNo',
      fixed: 'left' as const,
      key: 'rowNo',
      title: '序号',
      width: 70,
    },
    {
      dataIndex: 'supplierCode',
      fixed: 'left' as const,
      key: 'supplierCode',
      title: '供应商代码',
      width: 150,
    },
    {
      dataIndex: 'supplierName',
      fixed: 'left' as const,
      key: 'supplierName',
      title: '供应商名称',
      width: 180,
    },
    {
      dataIndex: 'materialCode',
      fixed: 'left' as const,
      key: 'materialCode',
      title: '物料代码',
      width: 170,
    },
    {
      dataIndex: 'contactPerson',
      key: 'contactPerson',
      title: '联系人',
      width: 150,
    },
    ...monthColumns,
    {
      dataIndex: 'completionStatus',
      key: 'completionStatus',
      title: '审核完成情况',
      width: 160,
    },
    {
      dataIndex: 'latestAuditDate',
      key: 'latestAuditDate',
      title: '审核时间',
      width: 130,
    },
  ];
});

const filteredLines = computed(() => {
  const keyword = queryForm.supplierKeyword.trim().toLowerCase();
  const materialCode = queryForm.materialCode.trim().toLowerCase();
  return (yearPlan.value.lines || []).filter((line) => {
    const matchSupplier =
      !keyword ||
      [line.supplierCode, line.supplierName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword));
    const matchMaterial =
      !materialCode || getLineMaterialSearchText(line).includes(materialCode);
    const matchStatus =
      !queryForm.executionStatus ||
      (line.months || []).some(
        (month) =>
          month.plannedFlag &&
          month.executionStatus === queryForm.executionStatus,
      );
    return matchSupplier && matchMaterial && matchStatus;
  });
});

const linePagination = reactive({
  current: 1,
  pageSize: 10,
});

const pagedLines = computed(() => {
  const startIndex = (linePagination.current - 1) * linePagination.pageSize;
  return filteredLines.value.slice(
    startIndex,
    startIndex + linePagination.pageSize,
  );
});

const lineTotal = computed(() => filteredLines.value.length);

const selectedAnnualLine = computed(() => {
  const selectedKey = selectedAnnualLineKeys.value[0];
  if (selectedKey === undefined || selectedKey === null) {
    return undefined;
  }
  const keyText = String(selectedKey);
  return (
    selectedAnnualLines.value.find((line) => String(line.id) === keyText) ||
    yearPlan.value.lines?.find((line) => String(line.id) === keyText)
  );
});

const annualLineRowSelection = computed(() => ({
  columnWidth: 48,
  fixed: true,
  hideSelectAll: true,
  onChange: (
    selectedRowKeys: Array<number | string>,
    selectedRows: YearPlanTableRow[],
  ) => {
    if (selectedRowKeys.length === 0) {
      clearSelectedAnnualLine();
      return;
    }
    const previousKeys = new Set(selectedAnnualLineKeys.value.map(String));
    const latestKey =
      selectedRowKeys.find((key) => !previousKeys.has(String(key))) ||
      selectedRowKeys[selectedRowKeys.length - 1];
    const latestRow =
      selectedRows.find((row) => String(row.id) === String(latestKey)) ||
      yearPlan.value.lines?.find((row) => String(row.id) === String(latestKey));
    selectedAnnualLineKeys.value = latestKey === undefined ? [] : [latestKey];
    selectedAnnualLines.value = latestRow ? [latestRow] : [];
  },
  selectedRowKeys: selectedAnnualLineKeys.value,
  type: 'checkbox' as const,
}));

const statusMap = computed(() => {
  return new Map(statusOptions.map((item) => [item.value, item]));
});

const relatedUserNames = computed(() => {
  if (monthForm.relatedUsers.length === 0) {
    return '';
  }
  return monthForm.relatedUsers
    .map((item) => item.name)
    .filter(Boolean)
    .join('、');
});

const isCurrentMonthSaved = computed(() => {
  return Boolean(monthDetail.value?.plannedFlag);
});

const currentMonthStatusName = computed(() => {
  return statusName(monthDetail.value?.executionStatus || 'PLAN') || '计划';
});

const isCurrentMonthClearable = computed(() => {
  return canClearMonthPlan(monthDetail.value);
});

const currentOperatorName = computed(() => {
  const user = userStore.userInfo;
  return user?.nickname || user?.username || '当前用户';
});

const canDeleteAnnualPlans = computed(() =>
  hasAccessByCodes([DELETE_ANNUAL_PLAN_PERMISSION]),
);

function getLineMaterialCodes(line?: Line) {
  const codeSet = new Set<string>();
  const appendCode = (value?: null | string) => {
    String(value || '')
      .split(/[\n\r,，、;；]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => codeSet.add(item));
  };
  (line?.materialCodes || []).forEach((code) => appendCode(code));
  appendCode(line?.materialCode);
  return [...codeSet];
}

function getLineMaterialCodeText(line?: Line) {
  return getLineMaterialCodes(line).join('、');
}

function getLineMaterialSearchText(line?: Line) {
  return getLineMaterialCodes(line).join(' ').toLowerCase();
}

const monthSubtitleItems = computed(() => {
  if (!monthDetail.value) {
    return [];
  }
  const monthText = `${monthDetail.value.planYear || '-'}年${
    monthDetail.value.planMonth || '-'
  }月`;
  return [
    monthText,
    currentLine.value?.supplierName,
    getLineMaterialCodeText(currentLine.value),
  ]
    .filter(Boolean)
    .map(String);
});

function todayText() {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function normalizeUser(user: any): UserSnapshot {
  return {
    deptName: user?.deptName,
    id: Number(user?.id ?? user?.userId),
    name: user?.nickname ?? user?.name ?? user?.userName ?? user?.username,
  };
}

function getMonth(line: Line, month: number) {
  return (line.months || []).find((item) => item.planMonth === month);
}

function getMonthNumberFromColumnKey(columnKey: unknown) {
  return Number(String(columnKey).replace('month', ''));
}

function getColumnMonth(line: Line, columnKey: unknown) {
  return getMonth(line, getMonthNumberFromColumnKey(columnKey));
}

function isMonthColumn(columnKey: unknown) {
  return String(columnKey).startsWith('month');
}

function statusName(status?: string) {
  if (!status) {
    return '';
  }
  return statusMap.value.get(status)?.label || status;
}

function statusColor(status?: string) {
  return statusMap.value.get(status || '')?.color || 'default';
}

function monthCellClass(month?: MonthPlan) {
  if (!month?.plannedFlag) {
    return 'srm-review-month-cell srm-review-month-cell--empty';
  }
  return `srm-review-month-cell srm-review-month-cell--${month.executionStatus || 'PLAN'}`;
}

function monthCellText(month?: MonthPlan) {
  if (!month?.plannedFlag) {
    return '';
  }
  return statusName(month.executionStatus || 'PLAN');
}

function canClearMonthPlan(month?: MonthPlan) {
  return Boolean(
    month?.id &&
    month.plannedFlag &&
    (!month.executionStatus || month.executionStatus === 'PLAN'),
  );
}

function isMonthClearing(month?: MonthPlan) {
  return Boolean(month?.id && clearingMonthId.value === month.id);
}

function clearSelectedAnnualLine() {
  selectedAnnualLineKeys.value = [];
  selectedAnnualLines.value = [];
}

function handleClearCurrentMonthPlan() {
  if (!currentLine.value || !monthDetail.value) {
    return;
  }
  handleClearMonthPlan(currentLine.value, monthDetail.value, true);
}

function handleClearMonthPlan(
  line: Line,
  month: MonthPlan | undefined,
  closeDetail = false,
) {
  if (!month?.id) {
    return;
  }
  if (!month.plannedFlag) {
    message.info('当前月份还未安排计划');
    return;
  }
  if (!canClearMonthPlan(month)) {
    message.warning('仅“计划”状态且未上报执行回复的月份计划允许清空');
    return;
  }
  const monthId = month.id;
  const monthText = `${month.planYear || currentYear.value}年${month.planMonth || ''}月`;
  const supplierText = line.supplierName || line.supplierCode || '当前供应商';
  Modal.confirm({
    content: `确认清空 ${supplierText} ${monthText} 的月份计划？清空后会移除计划说明、执行人、相关人员、审核信息和计划附件。`,
    okButtonProps: { danger: true },
    okText: '清空',
    onOk: async () => {
      clearingMonthId.value = monthId;
      try {
        await clearSupplierReviewMonthPlan(monthId);
        message.success('月份计划已清空');
        if (closeDetail) {
          handleCloseMonthModal();
        }
        await loadYearPlan();
      } finally {
        clearingMonthId.value = undefined;
      }
    },
    title: '清空月份计划',
    zIndex: MODAL_Z_INDEX + 220,
  });
}

async function handleDeleteAnnualPlans() {
  const line = selectedAnnualLine.value;
  const lineId = Number(line?.id);
  if (!line || !Number.isFinite(lineId)) {
    message.warning('请先勾选年度评审日历具体行');
    return;
  }
  const planYear = Number(line.planYear || currentYear.value);
  if (!Number.isInteger(planYear) || planYear < 2000 || planYear > 2100) {
    message.warning('请确认当前计划年度');
    return;
  }
  const rowText = line.rowNo ? `第 ${line.rowNo} 行` : '当前选中行';
  const supplierText = `${line.supplierName || '-'}（${line.supplierCode || '-'}）`;
  const materialText = getLineMaterialCodeText(line) || '-';
  Modal.confirm({
    content: `确认删除年度评审日历 ${rowText} 的 ${planYear} 年全部供方评审计划？供应商：${supplierText}；物料代码：${materialText}。删除后会移除该供应商年度行、12个月计划、执行人、回复、状态日志和附件。`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    onOk: async () => {
      deletingAnnualPlans.value = true;
      try {
        const result = await deleteSupplierReviewAnnualPlans({
          lineId,
          planYear,
        });
        if (result.deletedLineCount) {
          message.success(
            `已删除 ${result.deletedLineCount} 条年度行、${result.deletedMonthCount || 0} 条月份计划`,
          );
        } else {
          message.warning('未找到已勾选的年度评审日历行');
        }
        clearSelectedAnnualLine();
        await loadYearPlan();
      } finally {
        deletingAnnualPlans.value = false;
      }
    },
    title: '删除年度计划确认',
    zIndex: MODAL_Z_INDEX + 220,
  });
}

async function loadYearPlan() {
  loading.value = true;
  try {
    yearPlan.value = await getSupplierReviewYearPlan(currentYear.value);
    yearPlan.value.lines ||= [];
    clearSelectedAnnualLine();
  } finally {
    loading.value = false;
    await nextTick();
    refreshTableBodyHeight();
  }
}

function refreshTableBodyHeight() {
  const cardTop = tableCardRef.value?.getBoundingClientRect().top || 0;
  const viewportLimit = Math.max(360, window.innerHeight - cardTop - 160);
  tableBodyHeight.value = Math.floor(Math.min(620, viewportLimit));
}

function setupTableResizeObserver() {
  refreshTableBodyHeight();
  if (typeof ResizeObserver !== 'undefined' && tableCardRef.value) {
    tableResizeObserver = new ResizeObserver(() => refreshTableBodyHeight());
    tableResizeObserver.observe(tableCardRef.value);
  }
  window.addEventListener('resize', refreshTableBodyHeight);
}

function resetLinePage() {
  linePagination.current = 1;
  clearSelectedAnnualLine();
}

function handleLinePageChange(page: number, pageSize?: number) {
  linePagination.current = page;
  if (pageSize) {
    linePagination.pageSize = pageSize;
  }
  clearSelectedAnnualLine();
  void nextTick(refreshTableBodyHeight);
}

async function handleQuery() {
  resetLinePage();
  await loadYearPlan();
}

async function handleInitYearPlan() {
  await initSupplierReviewYearPlan(currentYear.value);
  message.success('年度评审计划已初始化');
  resetLinePage();
  await loadYearPlan();
}

async function handleYearChange(delta: number) {
  currentYear.value += delta;
  resetLinePage();
  await loadYearPlan();
}

function handleResetQuery() {
  queryForm.executionStatus = '';
  queryForm.materialCode = '';
  queryForm.supplierKeyword = '';
  resetLinePage();
}

const [SupplierPickerModal, supplierPickerModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

function openSupplierPicker() {
  supplierPickerModalApi
    .setData({
      initialSearchParams: {
        supplierName: queryForm.supplierKeyword || undefined,
      },
      keyword: queryForm.supplierKeyword,
      modalZIndex: MODAL_Z_INDEX + 200,
      multiple: true,
      referenceType: 'supplier',
      supplierSource: 'candidate',
      title: '选择供应商',
    })
    .open();
}

async function handleSupplierSelect(rows: SrmSupplierCandidateApi.Candidate[]) {
  if (rows.length === 0) {
    message.warning('请至少勾选一个供应商');
    return;
  }
  addingSuppliers.value = true;
  try {
    await Promise.all(
      rows.map((row) =>
        addSupplierReviewLine({
          contactPerson: row.contactPerson,
          planYear: currentYear.value,
          supplierCode: row.supplierCode,
          supplierId: row.supplierId,
          supplierName: row.supplierName,
        }),
      ),
    );
    message.success(
      `已处理 ${rows.length} 个供应商，重复供应商自动沿用原计划行`,
    );
  } finally {
    addingSuppliers.value = false;
  }
  await loadYearPlan();
}

watch(
  () => [
    queryForm.executionStatus,
    queryForm.materialCode,
    queryForm.supplierKeyword,
  ],
  resetLinePage,
);

watch(lineTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / linePagination.pageSize));
  if (linePagination.current > maxPage) {
    linePagination.current = maxPage;
  }
  void nextTick(refreshTableBodyHeight);
});

watch(
  () => [pagedLines.value.length, linePagination.pageSize],
  () => nextTick(refreshTableBodyHeight),
  { flush: 'post' },
);

async function handleContactBlur(line: Line) {
  if (!line.id) {
    return;
  }
  await updateSupplierReviewLineContact({
    contactPerson: line.contactPerson,
    id: line.id,
    remark: line.remark,
  });
}

async function openMonthModal(line: Line, month: number) {
  const monthPlan = getMonth(line, month);
  if (!monthPlan?.id) {
    message.warning('请先追加供应商年度计划行');
    return;
  }
  currentLine.value = line;
  const detail = await getSupplierReviewMonthPlan(monthPlan.id);
  monthDetail.value = detail;
  monthForm.planDesc = detail.planDesc || '';
  monthForm.leadUserId = detail.leadUserId;
  monthForm.leadUserName = detail.leadUserName || '';
  monthForm.auditDate = detail.auditDate || '';
  monthForm.approverUserId = detail.approverUserId;
  monthForm.approverUserName = detail.approverUserName || '';
  monthForm.approvalOpinion = detail.approvalOpinion || '';
  monthForm.remark = detail.remark || '';
  monthForm.relatedUsers = (detail.participants || [])
    .filter((item) => item.relationType === 'RELATED' && item.userId)
    .map((item) => ({
      deptName: item.deptName,
      id: item.userId,
      name: item.userName,
    }));
  monthModalOpen.value = true;
  await nextTick();
  await monthAttachmentRef.value?.loadAttachments();
}

async function handleSaveMonth() {
  if (!monthDetail.value?.id) {
    return;
  }
  savingMonth.value = true;
  try {
    await updateSupplierReviewMonthPlan({
      approvalOpinion: monthForm.approvalOpinion,
      approverUserId: monthForm.approverUserId,
      approverUserName: monthForm.approverUserName,
      auditDate: monthForm.auditDate,
      id: monthDetail.value.id,
      leadUserId: monthForm.leadUserId,
      leadUserName: monthForm.leadUserName,
      planDesc: monthForm.planDesc,
      relatedUsers: monthForm.relatedUsers,
      remark: monthForm.remark,
    });
    await monthAttachmentRef.value?.syncAttachments({
      bizId: monthDetail.value.id,
      bizType: 'SRM_SUPPLIER_REVIEW_PLAN',
    });
    message.success('保存成功');
    monthModalOpen.value = false;
    monthDetail.value = undefined;
    currentLine.value = undefined;
    await loadYearPlan();
  } finally {
    savingMonth.value = false;
  }
}

async function reloadMonthDetail() {
  if (!monthDetail.value?.id) {
    return;
  }
  const detail = await getSupplierReviewMonthPlan(monthDetail.value.id);
  monthDetail.value = detail;
  await nextTick();
  await monthAttachmentRef.value?.loadAttachments();
  await replyAttachmentRef.value?.loadAttachments();
}

function openUserPicker(purpose: 'approver' | 'lead' | 'related') {
  userSelectPurpose.value = purpose;
  let userIds: Array<number | undefined> = [];
  if (purpose === 'related') {
    userIds = monthForm.relatedUsers.map((item) => item.id).filter(Boolean);
  } else if (purpose === 'lead' && monthForm.leadUserId) {
    userIds = [monthForm.leadUserId];
  } else if (purpose === 'approver' && monthForm.approverUserId) {
    userIds = [monthForm.approverUserId];
  }
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: MODAL_Z_INDEX + 300,
      multiple: purpose === 'related',
      userIds,
    })
    .open();
}

function handleUserConfirm(users: any[]) {
  const normalized = (users || [])
    .map((item) => normalizeUser(item))
    .filter((item) => item.id);
  if (userSelectPurpose.value === 'related') {
    const userMap = new Map<number, UserSnapshot>();
    normalized.forEach((item) => item.id && userMap.set(item.id, item));
    monthForm.relatedUsers = [...userMap.values()];
    return;
  }
  const user = normalized[0];
  if (!user?.id) {
    return;
  }
  if (userSelectPurpose.value === 'lead') {
    monthForm.leadUserId = user.id;
    monthForm.leadUserName = user.name || '';
  } else {
    monthForm.approverUserId = user.id;
    monthForm.approverUserName = user.name || '';
  }
}

function clearLeadUser() {
  monthForm.leadUserId = undefined;
  monthForm.leadUserName = '';
}

function clearApproverUser() {
  monthForm.approverUserId = undefined;
  monthForm.approverUserName = '';
}

function clearRelatedUsers() {
  monthForm.relatedUsers = [];
}

function openStatusModal() {
  if (!isCurrentMonthSaved.value) {
    message.warning('请先保存月份计划，再调整状态');
    return;
  }
  statusForm.executionStatus = monthDetail.value?.executionStatus || 'PLAN';
  statusForm.statusRemark = '';
  statusForm.updateDescription = '';
  statusModalOpen.value = true;
}

function getPopupContainer(trigger?: HTMLElement) {
  return trigger?.parentElement || document.body;
}

async function handleAdjustStatus() {
  if (!monthDetail.value?.id) {
    return;
  }
  if (!statusForm.statusRemark.trim()) {
    message.warning('请填写状态备注');
    return;
  }
  savingStatus.value = true;
  try {
    await adjustSupplierReviewMonthStatus({
      executionStatus: statusForm.executionStatus,
      id: monthDetail.value.id,
      statusRemark: statusForm.statusRemark,
      updateDescription: statusForm.updateDescription,
    });
    message.success('状态已调整');
    statusModalOpen.value = false;
    await reloadMonthDetail();
    await loadYearPlan();
  } finally {
    savingStatus.value = false;
  }
}

function openReplyModal() {
  if (!isCurrentMonthSaved.value) {
    message.warning('请先保存月份计划，再上报执行回复');
    return;
  }
  replyForm.reviewDate = todayText();
  replyForm.recorderUserName = currentOperatorName.value;
  replyForm.reviewResult = '';
  replyForm.remark = '';
  replyModalOpen.value = true;
  nextTick(() => replyAttachmentRef.value?.loadAttachments());
}

async function handleCreateReply() {
  if (!monthDetail.value?.id) {
    return;
  }
  if (!replyForm.reviewDate) {
    message.warning('请填写评审日期');
    return;
  }
  if (!replyForm.recorderUserName.trim()) {
    message.warning('请填写经办人');
    return;
  }
  if (!replyForm.reviewResult.trim()) {
    message.warning('请填写评审结果说明');
    return;
  }
  savingReply.value = true;
  try {
    await createSupplierReviewReply({
      monthPlanId: monthDetail.value.id,
      recorderUserName: replyForm.recorderUserName,
      remark: replyForm.remark,
      reviewDate: replyForm.reviewDate,
      reviewResult: replyForm.reviewResult,
    });
    message.success('执行回复已保存');
    replyModalOpen.value = false;
    await reloadMonthDetail();
    await loadYearPlan();
  } finally {
    savingReply.value = false;
  }
}

function handleCloseMonthModal() {
  monthModalOpen.value = false;
  monthDetail.value = undefined;
  currentLine.value = undefined;
}

function handleArchiveStatus() {
  if (!isCurrentMonthSaved.value) {
    message.warning('请先保存月份计划，再归档');
    return;
  }
  Modal.confirm({
    content: '确认将该月份计划标记为归档？',
    okText: '确认',
    onOk: async () => {
      statusForm.executionStatus = 'ARCHIVED';
      statusForm.statusRemark = '评审资料归档';
      statusForm.updateDescription = '月度供方评审完成归档';
      await handleAdjustStatus();
    },
    title: '归档确认',
    zIndex: MODAL_Z_INDEX + 220,
  });
}

onMounted(async () => {
  await loadYearPlan();
  await nextTick();
  setupTableResizeObserver();
});

onBeforeUnmount(() => {
  tableResizeObserver?.disconnect();
  window.removeEventListener('resize', refreshTableBodyHeight);
});
</script>

<template>
  <Page auto-content-height>
    <div class="srm-review-plan-page">
      <section class="srm-review-card">
        <div class="srm-review-card__title-row">
          <div>
            <h2>供方评审计划</h2>
            <p>
              {{ yearPlan.planNo || `SRM-RP-${currentYear}` }}
              <span>{{
                yearPlan.planTitle || `供应商${currentYear}年度评审计划`
              }}</span>
              <Tag color="blue">
                {{ yearPlan.completionSummary || '未安排' }}
              </Tag>
            </p>
          </div>
          <Space class="srm-review-title-actions">
            <Button @click="handleYearChange(-1)">上一年</Button>
            <Input
              v-model:value.number="currentYear"
              class="srm-review-year-input"
              @press-enter="loadYearPlan"
            />
            <Button @click="handleYearChange(1)">下一年</Button>
            <Button @click="loadYearPlan">刷新</Button>
            <Button type="primary" @click="handleInitYearPlan">
              初始化年度计划
            </Button>
            <Button
              type="primary"
              :loading="addingSuppliers"
              @click="openSupplierPicker"
            >
              追加供应商
            </Button>
            <Button
              v-if="canDeleteAnnualPlans"
              danger
              :loading="deletingAnnualPlans"
              :title="
                selectedAnnualLine
                  ? '删除已勾选的年度评审日历行'
                  : '请先勾选年度评审日历具体行'
              "
              @click="handleDeleteAnnualPlans"
            >
              <IconifyIcon icon="lucide:trash-2" />
              删除年度计划
            </Button>
          </Space>
        </div>

        <div class="srm-review-query">
          <div class="srm-review-query__item">
            <span>供应商信息</span>
            <Input
              v-model:value="queryForm.supplierKeyword"
              allow-clear
              placeholder="请输入供应商代码或名称"
            />
          </div>
          <div class="srm-review-query__item">
            <span>物料代码</span>
            <Input
              v-model:value="queryForm.materialCode"
              allow-clear
              placeholder="请输入物料代码"
            />
          </div>
          <div class="srm-review-query__item srm-review-query__item--status">
            <span>执行状态</span>
            <Select
              v-model:value="queryForm.executionStatus"
              allow-clear
              :options="statusOptions"
              placeholder="请选择执行状态"
            />
          </div>
          <Space class="srm-review-query__actions">
            <Button @click="handleResetQuery">重置</Button>
            <Button type="primary" @click="handleQuery">查询</Button>
          </Space>
        </div>
      </section>

      <section
        ref="tableCardRef"
        class="srm-review-card srm-review-card--table"
      >
        <div class="srm-review-card__section-title">
          <div>
            年度评审日历
            <span>点击月份维护计划说明、执行人、相关人员与执行回复</span>
          </div>
          <Space class="srm-review-legend">
            <span v-for="item in statusOptions" :key="item.value">
              <i
                :class="`srm-review-legend__dot srm-review-legend__dot--${item.value}`"
              ></i>
              {{ item.label }}
            </span>
          </Space>
        </div>

        <div class="srm-review-year-table">
          <Table
            bordered
            :columns="yearColumns"
            :data-source="pagedLines"
            :loading="loading"
            :pagination="false"
            :row-selection="
              canDeleteAnnualPlans ? annualLineRowSelection : undefined
            "
            row-key="id"
            :scroll="{ x: 2460, y: tableBodyHeight }"
            size="small"
          >
            <template #emptyText>
              <Empty
                description="暂无年度评审计划，点击“追加供应商”后自动生成 12 个月计划格"
              />
            </template>

            <template #bodyCell="{ column, record, text }">
              <template v-if="column.key === 'materialCode'">
                <div class="srm-review-material-code-cell">
                  <template v-if="getLineMaterialCodes(record).length > 0">
                    <span
                      v-for="code in getLineMaterialCodes(record)"
                      :key="code"
                    >
                      {{ code }}
                    </span>
                  </template>
                  <span v-else>-</span>
                </div>
              </template>
              <template v-else-if="column.key === 'contactPerson'">
                <Input
                  v-model:value="record.contactPerson"
                  allow-clear
                  placeholder="请输入联系人"
                  @blur="handleContactBlur(record)"
                />
              </template>
              <template v-else-if="isMonthColumn(column.key)">
                <div class="srm-review-month-cell-wrap">
                  <button
                    :class="monthCellClass(getColumnMonth(record, column.key))"
                    type="button"
                    @click="
                      openMonthModal(
                        record,
                        getMonthNumberFromColumnKey(column.key),
                      )
                    "
                  >
                    {{
                      monthCellText(getColumnMonth(record, column.key)) ||
                      '安排'
                    }}
                  </button>
                  <Button
                    v-if="canClearMonthPlan(getColumnMonth(record, column.key))"
                    class="srm-review-month-clear"
                    danger
                    :disabled="Boolean(clearingMonthId)"
                    :loading="
                      isMonthClearing(getColumnMonth(record, column.key))
                    "
                    size="small"
                    title="清空当前月份计划"
                    type="text"
                    @click.stop="
                      handleClearMonthPlan(
                        record,
                        getColumnMonth(record, column.key),
                      )
                    "
                  >
                    <IconifyIcon icon="lucide:trash-2" />
                  </Button>
                </div>
              </template>
              <template v-else-if="column.key === 'completionStatus'">
                <Tag>{{ record.completionStatus || '未安排' }}</Tag>
              </template>
              <template v-else>
                {{ text || '-' }}
              </template>
            </template>
          </Table>
        </div>
        <div class="srm-review-table-footer">
          <span>共 {{ lineTotal }} 条记录</span>
          <Pagination
            v-model:current="linePagination.current"
            v-model:page-size="linePagination.pageSize"
            show-size-changer
            :page-size-options="['10', '20', '50', '100']"
            :show-total="(total) => `共 ${total} 条`"
            :total="lineTotal"
            @change="handleLinePageChange"
            @show-size-change="handleLinePageChange"
          />
        </div>
      </section>
    </div>

    <Modal
      v-model:open="monthModalOpen"
      :closable="false"
      destroy-on-close
      :footer="null"
      :keyboard="false"
      :mask-closable="false"
      :title="null"
      width="100vw"
      wrap-class-name="srm-review-month-modal-wrap srm-erp-crud-modal srm-independent-detail-modal"
      :z-index="MODAL_Z_INDEX"
      @cancel="handleCloseMonthModal"
    >
      <Spin :spinning="savingMonth" class="detail-spin">
        <div v-if="monthDetail" class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button
                class="qms-ncr-toolbar-action"
                @click="handleCloseMonthModal"
              >
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">供方评审月份计划</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in monthSubtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="isCurrentMonthSaved"
                class="qms-ncr-toolbar-action"
                @click="openReplyModal"
              >
                <IconifyIcon icon="lucide:message-square-plus" />
                上报执行回复
              </Button>
              <Button
                v-if="isCurrentMonthSaved"
                class="qms-ncr-toolbar-action"
                @click="openStatusModal"
              >
                <IconifyIcon icon="lucide:sliders-horizontal" />
                调整状态
              </Button>
              <Button
                v-if="isCurrentMonthSaved"
                class="qms-ncr-toolbar-action"
                @click="handleArchiveStatus"
              >
                <IconifyIcon icon="lucide:archive" />
                归档
              </Button>
              <Button
                v-if="isCurrentMonthSaved"
                class="qms-ncr-toolbar-action"
                danger
                :disabled="!isCurrentMonthClearable"
                :loading="isMonthClearing(monthDetail)"
                title="仅计划状态且未上报执行回复时允许清空"
                @click="handleClearCurrentMonthPlan"
              >
                <IconifyIcon icon="lucide:trash-2" />
                清空计划
              </Button>
              <Button
                class="qms-ncr-toolbar-action"
                :loading="savingMonth"
                type="primary"
                @click="handleSaveMonth"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button
                class="qms-ncr-toolbar-action"
                @click="handleCloseMonthModal"
              >
                <IconifyIcon icon="lucide:x" />
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>计划摘要</strong>
                    </div>
                    <Tag
                      :color="
                        statusColor(monthDetail.executionStatus || 'PLAN')
                      "
                    >
                      {{ currentMonthStatusName }}
                    </Tag>
                  </div>
                  <div class="erp-form-grid srm-review-month-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">计划年度</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ monthDetail.planYear || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">计划月份</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{
                            monthDetail.planMonth
                              ? `${monthDetail.planMonth}月`
                              : '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商代码</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ currentLine?.supplierCode || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商名称</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ currentLine?.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料代码</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ getLineMaterialCodeText(currentLine) || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">当前状态</label>
                      <div class="erp-form-value">
                        <Tag
                          :color="
                            statusColor(monthDetail.executionStatus || 'PLAN')
                          "
                        >
                          {{ currentMonthStatusName }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">适用产品</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ currentLine?.applicableProduct || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料名称</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ currentLine?.materialName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">型号</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ currentLine?.model || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>月份计划维护</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid srm-review-month-edit-grid">
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">牵头执行人</label>
                      <div class="erp-form-value">
                        <div class="srm-review-user-picker">
                          <Input.Search
                            v-model:value="monthForm.leadUserName"
                            class="srm-review-user-picker__search"
                            placeholder="请选择牵头执行人"
                            readonly
                            @search="openUserPicker('lead')"
                          >
                            <template #enterButton>
                              <Button>
                                <IconifyIcon icon="lucide:user-plus" />
                              </Button>
                            </template>
                          </Input.Search>
                          <Button
                            class="srm-review-user-picker__clear"
                            danger
                            type="link"
                            @click="clearLeadUser"
                          >
                            清空
                          </Button>
                        </div>
                      </div>
                    </div>
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">相关人员</label>
                      <div class="erp-form-value">
                        <div class="srm-review-user-picker">
                          <Input.Search
                            :value="relatedUserNames"
                            class="srm-review-user-picker__search"
                            placeholder="请选择相关人员"
                            readonly
                            @search="openUserPicker('related')"
                          >
                            <template #enterButton>
                              <Button>
                                <IconifyIcon icon="lucide:users" />
                              </Button>
                            </template>
                          </Input.Search>
                          <Button
                            class="srm-review-user-picker__clear"
                            danger
                            type="link"
                            @click="clearRelatedUsers"
                          >
                            清空
                          </Button>
                        </div>
                      </div>
                    </div>
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">审批意见</label>
                      <div class="erp-form-value">
                        <Input
                          v-model:value="monthForm.approvalOpinion"
                          placeholder="请输入审批意见"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">审核时间</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-model:value="monthForm.auditDate"
                          class="w-full"
                          :get-popup-container="getPopupContainer"
                          value-format="YYYY-MM-DD"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">审核人</label>
                      <div class="erp-form-value">
                        <div class="srm-review-user-picker">
                          <Input.Search
                            v-model:value="monthForm.approverUserName"
                            class="srm-review-user-picker__search"
                            placeholder="请选择审核人"
                            readonly
                            @search="openUserPicker('approver')"
                          >
                            <template #enterButton>
                              <Button>
                                <IconifyIcon icon="lucide:user-check" />
                              </Button>
                            </template>
                          </Input.Search>
                          <Button
                            class="srm-review-user-picker__clear"
                            danger
                            type="link"
                            @click="clearApproverUser"
                          >
                            清空
                          </Button>
                        </div>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        月份计划说明
                      </label>
                      <div
                        class="erp-form-value srm-review-month-textarea-value"
                      >
                        <Input.TextArea
                          v-model:value="monthForm.planDesc"
                          placeholder="请输入具体计划评审月份计划说明"
                          :rows="4"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注说明
                      </label>
                      <div
                        class="erp-form-value srm-review-month-textarea-value"
                      >
                        <Input.TextArea
                          v-model:value="monthForm.remark"
                          placeholder="请输入备注说明"
                          :rows="3"
                        />
                      </div>
                    </div>
                  </div>
                </section>

                <SrmAttachmentPanel
                  ref="monthAttachmentRef"
                  biz-type="SRM_SUPPLIER_REVIEW_PLAN"
                  :biz-id="monthDetail.id"
                  :category-options="attachmentCategoryOptions"
                  default-category="REVIEW_PLAN_DESC"
                  mode="edit"
                  :modal-z-index="MODAL_Z_INDEX + 200"
                />

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>执行回复列表</strong>
                    </div>
                  </div>
                  <div class="srm-review-approval-table-wrap">
                    <table class="srm-review-approval-table">
                      <thead>
                        <tr>
                          <th>记录时间</th>
                          <th>评审日期</th>
                          <th>经办人</th>
                          <th>评审结果说明</th>
                        </tr>
                      </thead>
                      <tbody v-if="monthDetail.replies?.length">
                        <tr
                          v-for="item in monthDetail.replies"
                          :key="item.id || item.replyTime || item.reviewDate"
                        >
                          <td>{{ item.replyTime || '-' }}</td>
                          <td>{{ item.reviewDate || '-' }}</td>
                          <td>{{ item.recorderUserName || '-' }}</td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.reviewResult || '-' }}
                          </td>
                        </tr>
                      </tbody>
                      <tbody v-else>
                        <tr>
                          <td class="srm-review-empty-cell" colspan="4">
                            暂无执行回复
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>状态调整日志</strong>
                    </div>
                  </div>
                  <div class="srm-review-approval-table-wrap">
                    <table
                      class="srm-review-approval-table srm-review-status-log-table"
                    >
                      <thead>
                        <tr>
                          <th>时间</th>
                          <th>操作人</th>
                          <th>调整前</th>
                          <th>调整后</th>
                          <th>状态备注</th>
                          <th>更新说明</th>
                        </tr>
                      </thead>
                      <tbody v-if="monthDetail.statusLogs?.length">
                        <tr
                          v-for="item in monthDetail.statusLogs"
                          :key="item.id || item.createTime || item.toStatusName"
                        >
                          <td>{{ item.createTime || '-' }}</td>
                          <td>{{ item.operatorUserName || '-' }}</td>
                          <td>{{ item.fromStatusName || '-' }}</td>
                          <td>{{ item.toStatusName || '-' }}</td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.reason || '-' }}
                          </td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.updateDescription || '-' }}
                          </td>
                        </tr>
                      </tbody>
                      <tbody v-else>
                        <tr>
                          <td class="srm-review-empty-cell" colspan="6">
                            暂无状态调整日志
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </Modal>

    <Modal
      v-model:open="statusModalOpen"
      centered
      :confirm-loading="savingStatus"
      destroy-on-close
      title="调整执行状态"
      :z-index="MODAL_Z_INDEX + 120"
      @ok="handleAdjustStatus"
    >
      <div class="srm-review-action-form">
        <div class="srm-review-action-form__label">执行状态</div>
        <Radio.Group
          v-model:value="statusForm.executionStatus"
          option-type="button"
        >
          <Radio.Button
            v-for="item in statusOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </Radio.Button>
        </Radio.Group>
        <div class="srm-review-action-form__label">状态备注</div>
        <Input.TextArea
          v-model:value="statusForm.statusRemark"
          placeholder="请输入状态调整原因"
          :rows="3"
        />
        <div class="srm-review-action-form__label">更新说明</div>
        <Input.TextArea
          v-model:value="statusForm.updateDescription"
          placeholder="请输入更新说明"
          :rows="3"
        />
      </div>
    </Modal>

    <Modal
      v-model:open="replyModalOpen"
      centered
      :confirm-loading="savingReply"
      destroy-on-close
      :body-style="{ maxHeight: '68vh', overflowY: 'auto' }"
      title="上报执行回复"
      width="960px"
      :z-index="MODAL_Z_INDEX + 150"
      @ok="handleCreateReply"
    >
      <div class="srm-review-action-form">
        <div
          class="srm-review-reply-summary-grid srm-review-reply-summary-grid--four"
        >
          <div class="srm-review-reply-summary-grid__label">计划年度</div>
          <div>{{ monthDetail?.planYear || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">计划月份</div>
          <div>
            {{ monthDetail?.planMonth ? `${monthDetail.planMonth}月` : '-' }}
          </div>
          <div class="srm-review-reply-summary-grid__label">供应商代码</div>
          <div>{{ currentLine?.supplierCode || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">供应商名称</div>
          <div>{{ currentLine?.supplierName || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">物料代码</div>
          <div>{{ getLineMaterialCodeText(currentLine) || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">物料名称</div>
          <div>{{ currentLine?.materialName || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">适用产品</div>
          <div>{{ currentLine?.applicableProduct || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">型号</div>
          <div>{{ currentLine?.model || '-' }}</div>
        </div>
        <div class="srm-review-reply-inline-grid">
          <label>评审日期</label>
          <DatePicker
            v-model:value="replyForm.reviewDate"
            class="w-full"
            :get-popup-container="getPopupContainer"
            value-format="YYYY-MM-DD"
          />
          <label>经办人</label>
          <Input
            v-model:value="replyForm.recorderUserName"
            allow-clear
            placeholder="请输入经办人"
          />
        </div>
        <div class="srm-review-action-form__label">评审结果说明</div>
        <Input.TextArea
          v-model:value="replyForm.reviewResult"
          placeholder="请输入评审结果说明"
          :rows="4"
        />
        <SrmAttachmentPanel
          v-if="monthDetail?.id"
          ref="replyAttachmentRef"
          class="srm-review-reply-attachment"
          biz-type="SRM_SUPPLIER_REVIEW_REPLY"
          :biz-id="monthDetail.id"
          :category-options="attachmentCategoryOptions"
          default-category="REVIEW_EXECUTION_REPLY"
          mode="edit"
          :modal-z-index="MODAL_Z_INDEX + 260"
        />
      </div>
    </Modal>

    <SupplierPickerModal @select-multiple="handleSupplierSelect" />
    <UserModal title="选择人员" @confirm="handleUserConfirm" />
  </Page>
</template>

<style lang="scss" scoped>
.srm-review-plan-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.srm-review-card {
  flex: 0 0 auto;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #d7e1ee;

  &--table {
    display: flex;
    flex: 1 1 0;
    min-height: 420px;
    flex-direction: column;
    overflow: hidden;
    padding-bottom: 12px;
  }

  &__title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;

    h2 {
      margin: 0;
      color: #003b66;
      font-size: 22px;
      font-weight: 700;
    }

    p {
      display: flex;
      align-items: center;
      margin: 8px 0 0;
      color: #536b89;
      gap: 10px;
    }
  }

  &__section-title {
    display: flex;
    flex: 0 0 auto;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 12px;
    color: #001529;
    font-size: 18px;
    font-weight: 700;

    span {
      margin-left: 10px;
      color: #6b7f99;
      font-size: 13px;
      font-weight: 400;
    }
  }
}

.srm-review-query {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
  margin-top: 16px;
  gap: 14px 18px;

  &__item {
    display: grid;
    grid-template-columns: 104px minmax(0, 1fr);
    min-width: 0;
    align-items: center;
    gap: 8px;

    > span {
      flex: 0 0 104px;
      color: #1f2d3d;
      font-weight: 600;
      text-align: right;
      white-space: nowrap;
    }

    :deep(.ant-input),
    :deep(.ant-select) {
      width: 100%;
      min-width: 0;
    }

    &--status {
      min-width: 0;
    }
  }

  &__actions {
    grid-column: 1 / -1;
    justify-self: end;
    white-space: nowrap;
  }
}

.srm-review-title-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.srm-review-year-table {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.srm-review-table-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  margin-top: 10px;
  padding: 10px 2px 0;
  border-top: 1px solid #e5edf6;
  color: #536b89;
}

.srm-review-year-input {
  width: 96px;
  text-align: center;
}

.srm-review-material-code-cell {
  display: flex;
  max-height: 88px;
  flex-direction: column;
  gap: 2px;
  overflow: auto;
  line-height: 18px;

  span {
    color: #475569;
    font-family:
      ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
      'Courier New', monospace;
    white-space: nowrap;
  }
}

.srm-review-month-cell {
  width: 64px;
  height: 30px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  color: #64748b;
  cursor: pointer;
  font-weight: 600;
  background: #fff;

  &--empty {
    color: #8c8c8c;
    border-style: dashed;
  }

  &--PLAN {
    color: #1677ff;
    background: #e6f4ff;
    border-color: #91caff;
  }

  &--EXECUTING {
    color: #d46b08;
    background: #fff7e6;
    border-color: #ffd591;
  }

  &--CHANGED {
    color: #722ed1;
    background: #f9f0ff;
    border-color: #d3adf7;
  }

  &--CANCELED {
    color: #cf1322;
    background: #fff1f0;
    border-color: #ffa39e;
  }

  &--COMPLETED,
  &--ARCHIVED {
    color: #389e0d;
    background: #f6ffed;
    border-color: #b7eb8f;
  }
}

.srm-review-month-cell-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 92px;
  gap: 4px;
}

.srm-review-month-clear.ant-btn {
  width: 24px;
  min-width: 24px;
  height: 24px;
  padding: 0;
  line-height: 1;

  :deep(.ant-btn-icon) {
    display: inline-flex;
  }
}

.srm-review-legend {
  color: #607088;
  font-size: 13px;

  &__dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    margin-right: 4px;
    border-radius: 50%;
    background: #91caff;

    &--EXECUTING {
      background: #ffd591;
    }

    &--CHANGED {
      background: #d3adf7;
    }

    &--CANCELED {
      background: #ffa39e;
    }

    &--COMPLETED,
    &--ARCHIVED {
      background: #b7eb8f;
    }
  }
}

.srm-review-month-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.srm-review-month-edit-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.srm-review-month-form-item--span-2 {
  grid-column: span 2;
}

.srm-review-month-textarea-value {
  display: grid !important;
  align-items: stretch !important;
  gap: 6px;
}

.srm-review-user-picker {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.srm-review-user-picker__search {
  min-width: 0;
  flex: 1 1 auto;
}

.srm-review-user-picker__clear.ant-btn {
  flex: 0 0 auto;
  height: auto;
  padding: 0;
}

.srm-review-approval-table-wrap {
  overflow-x: auto;
  border: 1px solid #dbe3ee;
  border-top: 0;
  background: #fff;
}

.srm-review-approval-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.srm-review-status-log-table {
  min-width: 1080px;
}

.srm-review-approval-table th,
.srm-review-approval-table td {
  padding: 11px 14px;
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  color: #24364f;
  line-height: 20px;
  text-align: left;
  vertical-align: middle;
}

.srm-review-approval-table th {
  background: #f7f9fc;
  color: #162338;
  font-weight: 600;
  white-space: nowrap;
}

.srm-review-approval-table th:last-child,
.srm-review-approval-table td:last-child {
  border-right: 0;
}

.srm-review-approval-table tbody tr:last-child td {
  border-bottom: 0;
}

.srm-review-approval-table__wrap-cell {
  white-space: normal;
  word-break: break-word;
}

.srm-review-empty-cell {
  padding: 18px 0;
  color: #7a8799;
  text-align: center;
}

.srm-review-action-form {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__label {
    color: #1f2d3d;
    font-weight: 700;
  }
}

.srm-review-reply-inline-grid {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  align-items: center;
  gap: 10px 12px;

  label {
    color: #1f2d3d;
    font-weight: 700;
    text-align: right;
  }

  :deep(.ant-input),
  :deep(.ant-picker) {
    width: 100%;
  }
}

.srm-review-reply-attachment {
  overflow: hidden;
  border: 1px solid #d7e1ee;
  background: #fff;

  :deep(.detail-list-head) {
    min-height: 46px;
    padding: 10px 14px;
    background: #f8fbff;
    border-bottom: 1px solid #d7e1ee;
  }

  :deep(.srm-attachment-panel__current-list) {
    min-height: 150px;
    padding: 12px;
  }

  :deep(.ant-empty) {
    margin: 20px 0;
  }
}

:deep(.ant-table-wrapper) {
  min-height: 0;
}

.srm-review-year-table :deep(.ant-table-wrapper),
.srm-review-year-table :deep(.ant-spin-nested-loading),
.srm-review-year-table :deep(.ant-spin-container),
.srm-review-year-table :deep(.ant-table),
.srm-review-year-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.srm-review-year-table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.srm-review-year-table :deep(.ant-table) {
  flex: 1 1 0;
}

.srm-review-year-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
}

.srm-review-year-table :deep(.ant-table-header) {
  flex: 0 0 auto;
}

.srm-review-year-table :deep(.ant-table-body) {
  flex: 1 1 0;
  height: 100% !important;
  max-height: none !important;
  min-height: 0;
  overflow: auto !important;
}

.srm-review-year-table :deep(.ant-table-cell-fix-left),
.srm-review-year-table :deep(.ant-table-cell-fix-right) {
  z-index: 3;
  background: #fff;
}

.srm-review-year-table :deep(.ant-table-thead .ant-table-cell-fix-left),
.srm-review-year-table :deep(.ant-table-thead .ant-table-cell-fix-right) {
  z-index: 4;
  background: #fafafa;
}

.srm-review-year-table
  :deep(.ant-table-tbody > tr:hover > td.ant-table-cell-fix-left),
.srm-review-year-table
  :deep(.ant-table-tbody > tr:hover > td.ant-table-cell-fix-right) {
  background: #f5faff;
}

.srm-review-reply-summary-grid {
  display: grid;
  overflow: hidden;
  grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  border: 1px solid #d7e1ee;
  border-bottom: 0;
  color: #1f3149;

  > div {
    min-height: 40px;
    padding: 9px 12px;
    border-right: 1px solid #d7e1ee;
    border-bottom: 1px solid #d7e1ee;
  }

  &__label {
    color: #344767;
    font-weight: 700;
    text-align: center;
    background: #eef3f9;
  }

  &__value--wide {
    grid-column: span 3;
  }
}

.srm-review-reply-summary-grid--four {
  grid-template-columns: repeat(4, 88px minmax(0, 1fr));
}

:deep(.ant-table-cell) {
  white-space: nowrap;
}

@media (max-width: 1400px) {
  .srm-review-query {
    grid-template-columns: repeat(3, minmax(0, 1fr));

    &__actions {
      grid-column: 1 / -1;
    }
  }
}

@media (max-width: 1100px) {
  .srm-review-reply-summary-grid--four {
    grid-template-columns: repeat(2, 88px minmax(0, 1fr));
  }

  .srm-review-month-edit-grid,
  .srm-review-month-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .srm-review-month-form-item--span-2 {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .srm-review-reply-summary-grid,
  .srm-review-reply-summary-grid--four {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .srm-review-reply-inline-grid {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .srm-review-month-edit-grid,
  .srm-review-month-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .srm-review-user-picker {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
