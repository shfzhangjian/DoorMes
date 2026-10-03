<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import dayjs from 'dayjs';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Alert, Button, Checkbox, DatePicker, Modal, Select, Space, Table, Tag, Tree, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchConfirmMeasureToolCalibrationTasks,
  deleteMeasureToolCalibrationRecord,
  exportMeasureToolCalibrationRecord,
  generateMeasureToolCalibrationTasks,
  getMeasureToolCalibrationDueHint,
  getMeasureToolCalibrationMonthlySummary,
  getMeasureToolCalibrationRecordPage,
  getMeasureToolCalibrationTaskCandidates,
  getMeasureToolCalibrationTaskPage,
  getMeasureToolCategoryList,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { getCalibrationRecordMonthRange, useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';
import {
  CALIBRATION_RESULT_OPTIONS,
  RECORD_SOURCE_OPTIONS,
  TASK_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
  mapCategoryOptions,
  optionColor,
  optionLabel,
} from '../shared';

defineOptions({ name: 'MesQmsMeasureToolCalibrationRecord' });

const ALL_CATEGORY_KEY = 'all';
const CATEGORY_KEY_PREFIX = 'category:';
const MONTHLY_TASK_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 200;

type CategoryTreeNode = {
  children?: CategoryTreeNode[];
  key: string;
  title: string;
};

const categoryOptions = ref<Array<{ label?: string; value?: number }>>([]);
const categoryRows = ref<QmsMeasureToolApi.Category[]>([]);
const checkedIds = ref<number[]>([]);
const activeTab = ref('detail');
const advancedSearchVisible = ref(false);
const selectedMonth = ref(dayjs().format('YYYY-MM'));
const recordLedgerMonth = ref(dayjs().format('YYYY-MM'));
const monthlyTaskLoading = ref(false);
const monthlyTaskRows = ref<QmsMeasureToolApi.CalibrationTask[]>([]);
const monthlyTaskPageNo = ref(1);
const monthlyTaskPageSize = ref(MONTHLY_TASK_PAGE_SIZE);
const monthlyTaskTotal = ref(0);
const monthlyTaskSelectedRowKeys = ref<number[]>([]);
const dueHintLoading = ref(false);
const dueHint = ref<QmsMeasureToolApi.CalibrationDueHint>({});
const candidateModalOpen = ref(false);
const candidateLoading = ref(false);
const candidateConfirmLoading = ref(false);
const candidateIncludeNonMonthDue = ref(false);
const candidateSelectedCategoryKeys = ref<string[]>([ALL_CATEGORY_KEY]);
const candidateExpandedCategoryKeys = ref<string[]>([ALL_CATEGORY_KEY]);
const candidateSelectedRowKeys = ref<number[]>([]);
const candidateRows = ref<QmsMeasureToolApi.CalibrationTaskCandidate[]>([]);
const summaryLoading = ref(false);
const summaryRows = ref<QmsMeasureToolApi.MonthlySummary[]>([]);
const summarySearch = reactive<{ calibrationDate?: string[]; categoryId?: number; usingDepartment?: string }>({
  calibrationDate: [dayjs().startOf('year').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')],
});
const candidateModalBodyStyle = {
  display: 'flex',
  height: 'min(680px, calc(100vh - 220px))',
  minHeight: '520px',
  overflow: 'hidden',
};

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

const selectedMonthLabel = computed(() => dayjs(`${selectedMonth.value}-01`).format('YYYY年MM月'));
const recordLedgerMonthLabel = computed(() => dayjs(`${recordLedgerMonth.value}-01`).format('YYYY年MM月'));
const selectedMonthRange = computed(() => {
  const month = dayjs(`${selectedMonth.value}-01`);
  return [month.startOf('month').format('YYYY-MM-DD'), month.endOf('month').format('YYYY-MM-DD')];
});
const selectedCandidateRows = computed(() => {
  const selectedIds = new Set(candidateSelectedRowKeys.value);
  return candidateRows.value.filter((item) => item.ledgerId && selectedIds.has(item.ledgerId));
});
const candidateRowSelection = computed(() => ({
  selectedRowKeys: candidateSelectedRowKeys.value,
  onChange: (keys: Array<number | string>) => {
    candidateSelectedRowKeys.value = keys.map(Number).filter((item) => Number.isFinite(item));
  },
}));
const selectedMonthlyTaskRows = computed(() => {
  const selectedIds = new Set(monthlyTaskSelectedRowKeys.value);
  return monthlyTaskRows.value.filter((item) => item.id && selectedIds.has(item.id));
});
const monthlyTaskRowSelection = computed(() => ({
  selectedRowKeys: monthlyTaskSelectedRowKeys.value,
  getCheckboxProps: (record: QmsMeasureToolApi.CalibrationTask) => ({
    disabled: record.taskStatus !== 'IN_PROGRESS' || !record.recordId,
  }),
  onChange: (keys: Array<number | string>) => {
    monthlyTaskSelectedRowKeys.value = keys.map(Number).filter((item) => Number.isFinite(item));
  },
}));
const monthlyStats = computed(() => ({
  completedCount: monthlyTaskRows.value.filter((item) => item.taskStatus === 'COMPLETED').length,
  overdueCount: monthlyTaskRows.value.filter((item) => item.taskStatus === 'OVERDUE').length,
  pendingCount: monthlyTaskRows.value.filter((item) => item.taskStatus === 'PENDING').length,
  waitConfirmCount: monthlyTaskRows.value.filter((item) => item.taskStatus === 'IN_PROGRESS').length,
}));

function toCategoryTreeKey(id?: number) {
  return id ? `${CATEGORY_KEY_PREFIX}${id}` : ALL_CATEGORY_KEY;
}

function parseCategoryTreeKey(key?: string | number) {
  const normalized = String(key || ALL_CATEGORY_KEY);
  if (normalized === ALL_CATEGORY_KEY) {
    return undefined;
  }
  const id = Number(normalized.replace(CATEGORY_KEY_PREFIX, ''));
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function buildCategoryNodes(rows: QmsMeasureToolApi.Category[], parentId = 0): CategoryTreeNode[] {
  return rows
    .filter((item) => item.status !== 0 && Number(item.parentId || 0) === parentId && item.id)
    .sort((a, b) => (a.sort || 0) - (b.sort || 0) || String(a.categoryCode || '').localeCompare(String(b.categoryCode || '')))
    .map((item) => {
      const children = buildCategoryNodes(rows, item.id);
      return {
        children: children.length > 0 ? children : undefined,
        key: toCategoryTreeKey(item.id),
        title: item.categoryName || item.categoryCode || '-',
      };
    });
}

function collectExpandedCategoryKeys(nodes: CategoryTreeNode[]): string[] {
  return nodes.flatMap((node) => [
    node.key,
    ...collectExpandedCategoryKeys(node.children ?? []),
  ]);
}

const candidateCategoryTreeData = computed<CategoryTreeNode[]>(() => [
  {
    children: buildCategoryNodes(categoryRows.value),
    key: ALL_CATEGORY_KEY,
    title: '全部分类',
  },
]);

function getSelectedCandidateCategoryId() {
  return parseCategoryTreeKey(candidateSelectedCategoryKeys.value[0]);
}

async function loadMonthlyDetail() {
  monthlyTaskLoading.value = true;
  try {
    const pageSize = Math.min(monthlyTaskPageSize.value, MAX_PAGE_SIZE);
    monthlyTaskPageSize.value = pageSize;
    const result = await getMeasureToolCalibrationTaskPage({
      dueDate: selectedMonthRange.value,
      pageNo: monthlyTaskPageNo.value,
      pageSize,
    });
    monthlyTaskRows.value = result.list || [];
    monthlyTaskTotal.value = result.total || 0;
    const visibleConfirmableIds = new Set(
      monthlyTaskRows.value
        .filter((item) => item.taskStatus === 'IN_PROGRESS' && item.recordId && item.id)
        .map((item) => item.id!),
    );
    monthlyTaskSelectedRowKeys.value = monthlyTaskSelectedRowKeys.value.filter((id) => visibleConfirmableIds.has(id));
  } finally {
    monthlyTaskLoading.value = false;
  }
}

async function loadDueHint() {
  dueHintLoading.value = true;
  try {
    dueHint.value = await getMeasureToolCalibrationDueHint({ month: selectedMonth.value });
  } finally {
    dueHintLoading.value = false;
  }
}

async function loadCandidateRows(autoSelect = false) {
  candidateLoading.value = true;
  try {
    const rows = await getMeasureToolCalibrationTaskCandidates({
      categoryId: getSelectedCandidateCategoryId(),
      includeNonMonthDue: candidateIncludeNonMonthDue.value,
      month: selectedMonth.value,
    });
    candidateRows.value = rows || [];
    const availableIds = new Set(candidateRows.value.map((item) => item.ledgerId).filter(Boolean) as number[]);
    if (autoSelect) {
      candidateSelectedRowKeys.value = [...availableIds];
    } else {
      candidateSelectedRowKeys.value = candidateSelectedRowKeys.value.filter((id) => availableIds.has(id));
    }
  } finally {
    candidateLoading.value = false;
  }
}

async function refreshMonthlyView() {
  await Promise.all([loadMonthlyDetail(), loadDueHint()]);
}

function handleTaskComplete(row: QmsMeasureToolApi.CalibrationTask) {
  formModalApi
    .setData(row.recordId
      ? { id: row.recordId }
      : { ledgerId: row.ledgerId, sourceType: 'TASK', taskId: row.id })
    .open();
}

function handleMonthlyTaskTableChange(pagination: { current?: number; pageSize?: number }) {
  monthlyTaskPageNo.value = pagination.current || 1;
  monthlyTaskPageSize.value = Math.min(pagination.pageSize || MONTHLY_TASK_PAGE_SIZE, MAX_PAGE_SIZE);
  void loadMonthlyDetail();
}

function openCandidateModal() {
  candidateModalOpen.value = true;
  candidateIncludeNonMonthDue.value = false;
  candidateSelectedCategoryKeys.value = [ALL_CATEGORY_KEY];
  candidateExpandedCategoryKeys.value = collectExpandedCategoryKeys(candidateCategoryTreeData.value);
  void loadCandidateRows(true);
}

async function handleCandidateCategorySelect(keys: Array<number | string>) {
  candidateSelectedCategoryKeys.value = [String(keys[0] || ALL_CATEGORY_KEY)];
  await loadCandidateRows(false);
}

function handleCandidateCategoryExpand(keys: Array<number | string>) {
  candidateExpandedCategoryKeys.value = keys.map(String);
}

async function handleCandidateIncludeChange() {
  await loadCandidateRows(false);
}

async function handleGenerateMonthlyTasks() {
  if (selectedCandidateRows.value.length === 0) {
    message.warning('请选择需要追加到本月检验的量检具');
    return;
  }
  await confirm(`确认将 ${selectedCandidateRows.value.length} 台量检具追加为 ${selectedMonthLabel.value} 本月检验任务吗？`);
  candidateConfirmLoading.value = true;
  try {
    const created = await generateMeasureToolCalibrationTasks({
      ledgerIds: selectedCandidateRows.value.map((item) => item.ledgerId!),
      month: selectedMonth.value,
    });
    message.success(`已追加 ${created} 条本月检验任务`);
    candidateModalOpen.value = false;
    candidateSelectedRowKeys.value = [];
    monthlyTaskPageNo.value = 1;
    await refreshMonthlyView();
  } finally {
    candidateConfirmLoading.value = false;
  }
}

function handleRefresh() {
  gridApi.query();
  void refreshMonthlyView();
}

async function handleBatchConfirmMonthlyTasks() {
  const taskIds = selectedMonthlyTaskRows.value.map((item) => item.id!).filter(Boolean);
  if (taskIds.length === 0) {
    message.warning('请选择待确认记录');
    return;
  }
  await confirm(`确认批量确认 ${taskIds.length} 条待确认记录吗？确认后将关闭任务并反写台账。`);
  const confirmedCount = await batchConfirmMeasureToolCalibrationTasks({ ids: taskIds });
  message.success(`已确认 ${confirmedCount} 条记录`);
  monthlyTaskSelectedRowKeys.value = [];
  await refreshMonthlyView();
  gridApi.query();
}

async function handleConfirmMonthlyTask(row: QmsMeasureToolApi.CalibrationTask) {
  if (!row.id) return;
  await confirm(`确认关闭任务 [${row.taskNo || row.toolCode}] 并反写台账吗？`);
  const confirmedCount = await batchConfirmMeasureToolCalibrationTasks({ ids: [row.id] });
  message.success(`已确认 ${confirmedCount} 条记录`);
  monthlyTaskSelectedRowKeys.value = monthlyTaskSelectedRowKeys.value.filter((id) => id !== row.id);
  await refreshMonthlyView();
  gridApi.query();
}

async function syncRecordLedgerMonthRange(query = true) {
  const calibrationDate = getCalibrationRecordMonthRange(recordLedgerMonth.value);
  await gridApi.formApi.setValues({
    calibrationDate,
  });
  const formValues = {
    ...(await gridApi.formApi.getValues()),
    calibrationDate,
  };
  gridApi.formApi.setLatestSubmissionValues(formValues);
  if (query) {
    gridApi.query(formValues);
  }
}

function handleRecordLedgerMonthChange(value?: string) {
  recordLedgerMonth.value = value || dayjs().format('YYYY-MM');
  void syncRecordLedgerMonthRange();
}

function moveRecordLedgerMonth(offset: number) {
  recordLedgerMonth.value = dayjs(`${recordLedgerMonth.value}-01`).add(offset, 'month').format('YYYY-MM');
  void syncRecordLedgerMonthRange();
}

function resetRecordLedgerMonth() {
  recordLedgerMonth.value = dayjs().format('YYYY-MM');
  void syncRecordLedgerMonthRange();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: QmsMeasureToolApi.CalibrationRecord) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: QmsMeasureToolApi.CalibrationRecord) {
  await confirm(`确认删除校准记录 [${row.recordNo}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteMeasureToolCalibrationRecord(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const hideLoading = message.loading({ content: $t('ui.actionMessage.deletingBatch'), duration: 0 });
  try {
    await Promise.all(checkedIds.value.map((id) => deleteMeasureToolCalibrationRecord(id)));
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportMeasureToolCalibrationRecord(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '量检具校准记录台账.xls', source: data });
}

function handleRowCheckboxChange({ records }: { records: QmsMeasureToolApi.CalibrationRecord[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'measure-tool-calibration-vben-grid',
  gridClass: 'measure-tool-calibration-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getMeasureToolCalibrationRecordPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<QmsMeasureToolApi.CalibrationRecord>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(categoryOptions.value, advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

const monthlyTaskColumns: TableColumnsType<QmsMeasureToolApi.CalibrationTask> = [
  { dataIndex: 'taskNo', fixed: 'left', title: '任务号', width: 170 },
  { dataIndex: 'toolCode', fixed: 'left', title: '量检具编码', width: 150 },
  { dataIndex: 'toolName', fixed: 'left', title: '量检具名称', width: 180 },
  { dataIndex: 'categoryName', title: '分类', width: 130 },
  { dataIndex: 'usingDepartment', title: '使用部门', width: 130 },
  { dataIndex: 'keeperName', title: '保管人', width: 100 },
  { align: 'center', dataIndex: 'dueDate', title: '应执行日期', width: 120 },
  { align: 'center', dataIndex: 'warningStatus', key: 'warningStatus', title: '预警状态', width: 110 },
  { align: 'center', dataIndex: 'taskStatus', key: 'taskStatus', title: '任务状态', width: 110 },
  { align: 'center', dataIndex: 'completedTime', title: '完成时间', width: 170 },
  { dataIndex: 'handlerName', title: '处理人', width: 110 },
  { dataIndex: 'remark', title: '备注', width: 220 },
  { fixed: 'right', key: 'actions', title: '操作', width: 150 },
];

const monthlyTaskPagination = computed(() => ({
  current: monthlyTaskPageNo.value,
  pageSize: monthlyTaskPageSize.value,
  showSizeChanger: false,
  total: monthlyTaskTotal.value,
  showTotal: (total: number) => `共 ${total} 条`,
}));

const candidateColumns: TableColumnsType<QmsMeasureToolApi.CalibrationTaskCandidate> = [
  { dataIndex: 'toolCode', fixed: 'left', title: '量检具编码', width: 145 },
  { dataIndex: 'toolName', fixed: 'left', title: '量检具名称', width: 170 },
  { dataIndex: 'categoryName', title: '分类', width: 130 },
  { dataIndex: 'usingDepartment', title: '使用部门', width: 120 },
  { dataIndex: 'keeperName', title: '保管人', width: 100 },
  { align: 'center', dataIndex: 'lastCalibrationDate', title: '上次检验时间', width: 130 },
  { align: 'center', dataIndex: 'nextCalibrationDate', title: '台账下次校准', width: 130 },
  { align: 'center', dataIndex: 'taskDueDate', title: '本月任务日期', width: 130 },
  { align: 'center', dataIndex: 'warningStatus', key: 'warningStatus', title: '预警状态', width: 110 },
  { align: 'center', dataIndex: 'dueInSelectedMonth', key: 'dueInSelectedMonth', title: '本月到期', width: 100 },
];

const candidatePagination = {
  pageSize: 10,
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 条`,
};

const summaryColumns: TableColumnsType<QmsMeasureToolApi.MonthlySummary> = [
  { dataIndex: 'month', title: '月份', width: 100 },
  { dataIndex: 'categoryName', title: '分类', width: 150 },
  { dataIndex: 'usingDepartment', title: '使用部门', width: 140 },
  { align: 'right', dataIndex: 'taskCount', title: '应校准任务数', width: 130 },
  { align: 'right', dataIndex: 'recordCount', title: '已校准记录数', width: 130 },
  { align: 'right', dataIndex: 'qualifiedCount', title: '合格数', width: 100 },
  { align: 'right', dataIndex: 'unqualifiedCount', title: '不合格数', width: 100 },
  { align: 'right', dataIndex: 'limitedCount', title: '限用数', width: 100 },
  { align: 'right', dataIndex: 'overdueCompletedCount', title: '逾期完成数', width: 120 },
  { align: 'right', dataIndex: 'completionRate', title: '完成率(%)', width: 110 },
];

async function loadSummary() {
  summaryLoading.value = true;
  try {
    summaryRows.value = await getMeasureToolCalibrationMonthlySummary({ ...summarySearch });
  } finally {
    summaryLoading.value = false;
  }
}

function resetSummary() {
  Object.assign(summarySearch, {
    calibrationDate: [dayjs().startOf('year').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')],
    categoryId: undefined,
    usingDepartment: undefined,
  });
  void loadSummary();
}

onMounted(async () => {
  categoryRows.value = await getMeasureToolCategoryList();
  categoryOptions.value = mapCategoryOptions(categoryRows.value);
  candidateExpandedCategoryKeys.value = collectExpandedCategoryKeys(candidateCategoryTreeData.value);
  await gridApi.formApi.updateSchema(useGridFormSchema(categoryOptions.value, advancedSearchVisible.value));
  await syncRecordLedgerMonthRange(false);
  await refreshMonthlyView();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Modal
      v-model:open="candidateModalOpen"
      :body-style="candidateModalBodyStyle"
      :confirm-loading="candidateConfirmLoading"
      :title="`追加${selectedMonthLabel}到期/过期设备`"
      cancel-text="取消"
      ok-text="追加到本月检验"
      width="1120px"
      @ok="handleGenerateMonthlyTasks"
    >
      <div class="measure-tool-candidate-modal">
        <div class="measure-tool-candidate-modal__tree">
          <div class="measure-tool-candidate-modal__tree-scroll">
            <Tree
              class="measure-tool-candidate-modal__category-tree"
              :expanded-keys="candidateExpandedCategoryKeys"
              :selected-keys="candidateSelectedCategoryKeys"
              :show-line="{ showLeafIcon: false }"
              :tree-data="candidateCategoryTreeData"
              @expand="handleCandidateCategoryExpand"
              @select="handleCandidateCategorySelect"
            />
          </div>
        </div>
        <div class="measure-tool-candidate-modal__content">
          <div class="measure-tool-candidate-modal__toolbar">
            <Checkbox
              v-model:checked="candidateIncludeNonMonthDue"
              @change="handleCandidateIncludeChange"
            >
              包含非本月到期
            </Checkbox>
            <Space size="small">
              <span class="measure-tool-candidate-modal__count">
                已选择 {{ selectedCandidateRows.length }} 台
              </span>
              <Button size="small" @click="loadCandidateRows(false)">
                <IconifyIcon icon="lucide:refresh-cw" />
                刷新
              </Button>
            </Space>
          </div>
          <Alert
            show-icon
            type="info"
            class="measure-tool-candidate-modal__hint"
            :message="`已选择 ${selectedCandidateRows.length} 台量检具；默认列表为${selectedMonthLabel}到期或过期未检台账。`"
          />
          <Table
            class="measure-tool-fill-table measure-tool-candidate-modal__table"
            bordered
            size="small"
            :columns="candidateColumns"
            :data-source="candidateRows"
            :loading="candidateLoading"
            :pagination="candidatePagination"
            :row-key="(row) => row.ledgerId || row.toolCode"
            :row-selection="candidateRowSelection"
            :scroll="{ x: 1180, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'warningStatus'">
                <Tag :color="optionColor(WARNING_STATUS_OPTIONS, record.warningStatus)">
                  {{ optionLabel(WARNING_STATUS_OPTIONS, record.warningStatus) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'dueInSelectedMonth'">
                <Tag :color="record.overdue ? 'error' : record.dueInSelectedMonth ? 'warning' : 'default'">
                  {{ record.overdue ? '过期未检' : record.dueInSelectedMonth ? '是' : '否' }}
                </Tag>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </Modal>
    <div class="measure-tool-calibration-page">
      <a-tabs v-model:active-key="activeTab" class="measure-tool-tabs">
        <a-tab-pane key="detail" tab="本月检验">
          <div class="measure-tool-monthly-pane">
            <div class="measure-tool-monthly-toolbar">
              <div class="measure-tool-monthly-title">
                <strong>{{ selectedMonthLabel }}本月检验</strong>
                <span>固定展示当前月份，历史记录请进入记录台账查询。</span>
              </div>
              <Space wrap>
                <Button type="primary" @click="handleCreate">
                  <IconifyIcon icon="lucide:plus" />
                  人工新增检验记录
                </Button>
                <Button @click="openCandidateModal">
                  <IconifyIcon icon="lucide:list-plus" />
                  到期/过期追加
                </Button>
                <Button
                  :disabled="selectedMonthlyTaskRows.length === 0"
                  type="primary"
                  @click="handleBatchConfirmMonthlyTasks"
                >
                  <IconifyIcon icon="lucide:check-check" />
                  批量确认
                </Button>
                <Button :loading="monthlyTaskLoading" @click="loadMonthlyDetail">
                  <IconifyIcon icon="lucide:refresh-cw" />
                  刷新
                </Button>
              </Space>
            </div>
            <Alert
              show-icon
              type="info"
              class="measure-tool-monthly-hint"
              :message="`${selectedMonthLabel}检验记录 ${monthlyTaskTotal} 条，即将到期 ${dueHint.dueSoonCount || 0} 台，过期未检 ${dueHint.overdueCount || 0} 台`"
              :description="`当前页待执行 ${monthlyStats.pendingCount} 条，待确认 ${monthlyStats.waitConfirmCount} 条，已完成 ${monthlyStats.completedCount} 条，逾期 ${monthlyStats.overdueCount} 条。待确认记录批量确认后进入记录台账。`"
            >
              <template #action>
                <Button size="small" type="link" :loading="dueHintLoading" @click="openCandidateModal">
                  查看明细
                </Button>
              </template>
            </Alert>
            <Table
              class="measure-tool-fill-table"
              bordered
              size="middle"
              :columns="monthlyTaskColumns"
              :data-source="monthlyTaskRows"
              :loading="monthlyTaskLoading"
              :pagination="monthlyTaskPagination"
              :row-key="(row) => row.id || row.taskNo"
              :row-selection="monthlyTaskRowSelection"
              :scroll="{ x: 1500, y: '100%' }"
              @change="handleMonthlyTaskTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'warningStatus'">
                  <Tag :color="optionColor(WARNING_STATUS_OPTIONS, record.warningStatus)">
                    {{ optionLabel(WARNING_STATUS_OPTIONS, record.warningStatus) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'taskStatus'">
                  <Tag :color="optionColor(TASK_STATUS_OPTIONS, record.taskStatus)">
                    {{ optionLabel(TASK_STATUS_OPTIONS, record.taskStatus) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <TableAction
                    :actions="[
                      {
                        label: record.recordId ? '查看/维护详情' : '填写检验详情',
                        type: 'link',
                        icon: ACTION_ICON.AUDIT,
                        auth: ['mes:qms-measure-tool-calibration-task:complete'],
                        ifShow: !!record.recordId || (record.taskStatus !== 'COMPLETED' && record.taskStatus !== 'CANCELLED'),
                        onClick: () => handleTaskComplete(record),
                      },
                      {
                        label: '确认',
                        type: 'link',
                        icon: ACTION_ICON.AUDIT,
                        ifShow: record.taskStatus === 'IN_PROGRESS' && !!record.recordId,
                        onClick: () => handleConfirmMonthlyTask(record),
                      },
                    ]"
                  />
                </template>
              </template>
            </Table>
          </div>
        </a-tab-pane>
        <a-tab-pane key="record" tab="记录台账">
          <div class="measure-tool-calibration-grid-host">
            <Grid table-title="量检具校准记录台账">
              <template #toolbar-tools>
                <div class="measure-tool-record-toolbar">
                  <Space class="measure-tool-record-month-toolbar" size="small">
                    <Button @click="moveRecordLedgerMonth(-1)">
                      <IconifyIcon icon="lucide:chevron-left" />
                    </Button>
                    <DatePicker
                      v-model:value="recordLedgerMonth"
                      :allow-clear="false"
                      :title="recordLedgerMonthLabel"
                      picker="month"
                      value-format="YYYY-MM"
                      format="YYYY-MM"
                      class="measure-tool-record-month-toolbar__picker"
                      @change="handleRecordLedgerMonthChange"
                    />
                    <Button @click="moveRecordLedgerMonth(1)">
                      <IconifyIcon icon="lucide:chevron-right" />
                    </Button>
                    <Button @click="resetRecordLedgerMonth">本月</Button>
                  </Space>
                  <TableAction
                    :actions="[
                      {
                        label: $t('ui.actionTitle.create', ['校准记录']),
                        type: 'primary',
                        icon: ACTION_ICON.ADD,
                        auth: ['mes:qms-measure-tool-calibration-record:create'],
                        onClick: handleCreate,
                      },
                      {
                        label: $t('ui.actionTitle.export'),
                        type: 'primary',
                        icon: ACTION_ICON.DOWNLOAD,
                        auth: ['mes:qms-measure-tool-calibration-record:export'],
                        onClick: handleExport,
                      },
                      {
                        label: $t('ui.actionTitle.deleteBatch'),
                        type: 'primary',
                        danger: true,
                        icon: ACTION_ICON.DELETE,
                        auth: ['mes:qms-measure-tool-calibration-record:delete'],
                        disabled: isEmpty(checkedIds),
                        onClick: handleDeleteBatch,
                      },
                    ]"
                  />
                </div>
              </template>

              <template #expand-after>
                <a class="measure-tool-calibration-page__expand-link" @click="toggleAdvancedSearch">
                  {{ advancedSearchVisible ? '收起' : '展开' }}
                  <IconifyIcon
                    :icon="advancedSearchVisible ? 'lucide:chevron-up' : 'lucide:chevron-down'"
                    class="measure-tool-calibration-page__expand-icon"
                  />
                </a>
              </template>

              <template #calibrationResult="{ row }">
                <Tag :color="optionColor(CALIBRATION_RESULT_OPTIONS, row.calibrationResult)">
                  {{ optionLabel(CALIBRATION_RESULT_OPTIONS, row.calibrationResult) }}
                </Tag>
              </template>

              <template #sourceType="{ row }">
                <Tag :color="optionColor(RECORD_SOURCE_OPTIONS, row.sourceType)">
                  {{ optionLabel(RECORD_SOURCE_OPTIONS, row.sourceType) }}
                </Tag>
              </template>

              <template #actions="{ row }">
                <TableAction
                  :actions="[
                    {
                      label: $t('common.edit'),
                      type: 'link',
                      icon: ACTION_ICON.EDIT,
                      auth: ['mes:qms-measure-tool-calibration-record:update'],
                      onClick: () => handleEdit(row),
                    },
                    {
                      label: $t('common.delete'),
                      type: 'link',
                      danger: true,
                      icon: ACTION_ICON.DELETE,
                      auth: ['mes:qms-measure-tool-calibration-record:delete'],
                      onClick: () => handleDelete(row),
                    },
                  ]"
                />
              </template>
            </Grid>
          </div>
        </a-tab-pane>
        <a-tab-pane v-if="false" key="summary" tab="月度汇总">
          <div class="measure-tool-summary-pane">
            <div class="summary-toolbar">
              <Space wrap>
                <DatePicker.RangePicker
                  v-model:value="summarySearch.calibrationDate"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                />
                <Select
                  v-model:value="summarySearch.categoryId"
                  allow-clear
                  show-search
                  :options="categoryOptions"
                  placeholder="分类"
                  class="w-[220px]"
                />
                <a-input
                  v-model:value="summarySearch.usingDepartment"
                  allow-clear
                  placeholder="使用部门"
                  class="w-[180px]"
                />
                <Button type="primary" @click="loadSummary">查询</Button>
                <Button @click="resetSummary">重置</Button>
              </Space>
            </div>
            <Table
              bordered
              size="middle"
              :columns="summaryColumns"
              :data-source="summaryRows"
              :loading="summaryLoading"
              :pagination="false"
              :row-key="(row) => `${row.month}-${row.categoryId || 'all'}-${row.usingDepartment || 'all'}`"
              :scroll="{ x: 1180, y: 'calc(100vh - 390px)' }"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </Page>
</template>

<style scoped>
.measure-tool-calibration-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.measure-tool-tabs :deep(.ant-tabs-content-holder),
.measure-tool-tabs :deep(.ant-tabs-content),
.measure-tool-tabs :deep(.ant-tabs-tabpane),
.measure-tool-tabs :deep(.ant-tabs-tabpane-active) {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}

.measure-tool-monthly-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-monthly-toolbar {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
}

.measure-tool-monthly-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #1f2937;
}

.measure-tool-monthly-title strong {
  font-size: 15px;
}

.measure-tool-monthly-title span,
.measure-tool-candidate-modal__count {
  color: #64748b;
  font-size: 12px;
}

.measure-tool-monthly-toolbar :deep(.ant-btn),
.measure-tool-candidate-modal__toolbar :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.measure-tool-monthly-hint {
  flex: 0 0 auto;
  margin-bottom: 10px;
}

.measure-tool-fill-table {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-fill-table :deep(.ant-spin-nested-loading),
.measure-tool-fill-table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.measure-tool-fill-table :deep(.ant-spin-container) {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.measure-tool-fill-table :deep(.ant-table) {
  min-height: 0;
  overflow: hidden;
}

.measure-tool-fill-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.measure-tool-fill-table :deep(.ant-table-content),
.measure-tool-fill-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}

.measure-tool-fill-table :deep(.ant-table-placeholder) {
  height: 100%;
}

.measure-tool-fill-table :deep(.ant-table-pagination) {
  flex: 0 0 auto;
  margin: 12px 0 0;
  padding-right: 2px;
}

.measure-tool-candidate-modal {
  display: flex;
  flex: 1 1 auto;
  height: 100%;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.measure-tool-candidate-modal__tree {
  flex: 0 0 260px;
  min-width: 260px;
  height: 100%;
  min-height: 0;
  padding: 12px;
  overflow: hidden;
  background: #fafafa;
  border-right: 1px solid #e5e7eb;
}

.measure-tool-candidate-modal__tree-scroll {
  height: 100%;
  overflow: auto;
}

.measure-tool-candidate-modal__category-tree {
  min-width: max-content;
  background: transparent;
}

.measure-tool-candidate-modal__category-tree :deep(.ant-tree-indent-unit::before) {
  border-color: hsl(var(--border));
}

.measure-tool-candidate-modal__content {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px;
  overflow: hidden;
}

.measure-tool-candidate-modal__toolbar {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.measure-tool-candidate-modal__hint {
  flex: 0 0 auto;
  margin-bottom: 8px;
}

.measure-tool-candidate-modal__table {
  flex: 1 1 0;
  min-height: 0;
  margin-top: 0;
}

.measure-tool-record-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.measure-tool-record-month-toolbar {
  flex: 0 0 auto;
}

.measure-tool-record-month-toolbar :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.measure-tool-record-month-toolbar__picker {
  width: 120px;
}

.measure-tool-calibration-grid-host {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-calibration-grid-host :deep(.measure-tool-calibration-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-calibration-grid-host :deep(.measure-tool-calibration-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 5;
  min-height: 0;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  min-height: 0;
  background: #fff;
}

.measure-tool-calibration-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.measure-tool-calibration-grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.measure-tool-calibration-page__expand-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
}

.measure-tool-calibration-page__expand-link:hover {
  text-decoration: underline;
}

.measure-tool-calibration-page__expand-icon {
  font-size: 14px;
}

.measure-tool-summary-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.summary-toolbar {
  padding: 12px 0;
}

.measure-tool-summary-pane :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
