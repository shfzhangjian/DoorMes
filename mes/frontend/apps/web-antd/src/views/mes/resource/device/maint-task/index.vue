<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';
import type { MesMaintTaskApi } from '#/api/mes/resource/device/maint-task';

import dayjs from 'dayjs';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Alert,
  Button,
  Checkbox,
  DatePicker,
  Input,
  message,
  Modal,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tree,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getCategoryList } from '#/api/mes/resource/device/category';
import {
  appendCurrentMonthMaintOrders,
  confirmMaintOrders,
  exportTask,
  getMaintCurrentMonthCandidates,
  getMaintOrderPage,
  getTaskPage,
} from '#/api/mes/resource/device/maint-task';
import { $t } from '#/locales';

import { mapTreeData } from '../ledger/data';
import { MAINT_ORDER_STATUS_OPTIONS, mapCategoryOptions, optionColor, optionLabel } from '../shared';
import {
  getMaintRecordMonthRange,
  MAINT_RESULT_OPTIONS,
  MAINT_TYPE_OPTIONS,
  useGridColumns,
  useGridFormSchema,
} from './data';
import ExecDetailModal from '../maint-task-exec/modules/detail-modal.vue';
import OrderFormComponent from '../maint-task-exec/modules/order-form.vue';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesMaintTaskRecord' });

const MONTHLY_TASK_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 200;
const ALL_CATEGORY_KEY = 0;

const activeTab = ref('current');
const advancedSearchVisible = ref(false);
const currentMonth = ref(dayjs().format('YYYY-MM'));
const recordLedgerMonth = ref(dayjs().format('YYYY-MM'));
const monthlyTaskLoading = ref(false);
const monthlyTaskRows = ref<MesMaintTaskApi.Order[]>([]);
const monthlyTaskPageNo = ref(1);
const monthlyTaskPageSize = ref(MONTHLY_TASK_PAGE_SIZE);
const monthlyTaskTotal = ref(0);
const monthlySelectedRowKeys = ref<number[]>([]);
const maintDueHintLoading = ref(false);
const maintDueHint = ref({ dueSoonCount: 0, overdueCount: 0 });
const categoryOptions = ref<Array<{ label?: string; value?: number }>>([]);
const categoryRows = ref<MesDeviceCategoryApi.Category[]>([]);

const candidateModalOpen = ref(false);
const candidateLoading = ref(false);
const candidateSubmitting = ref(false);
const candidateRows = ref<MesMaintTaskApi.MaintCandidate[]>([]);
const candidateSelectedKeys = ref<string[]>([]);
const candidateCategorySearchValue = ref('');
const candidateSelectedCategoryKeys = ref<Array<number | string>>([ALL_CATEGORY_KEY]);
const candidateExpandedCategoryKeys = ref<Array<number | string>>([]);
const candidateQuery = reactive({
  categoryId: undefined as number | undefined,
  deviceName: '',
  includeNormal: false,
});

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const [ExecModal, execModalApi] = useVbenModal({ connectedComponent: ExecDetailModal, destroyOnClose: true });
const [OrderModal, orderModalApi] = useVbenModal({ connectedComponent: OrderFormComponent, destroyOnClose: true });

const currentMonthLabel = computed(() => dayjs(`${currentMonth.value}-01`).format('YYYY年MM月'));
const recordLedgerMonthLabel = computed(() => dayjs(`${recordLedgerMonth.value}-01`).format('YYYY年MM月'));
const currentMonthRange = computed(() => {
  const month = dayjs(`${currentMonth.value}-01`);
  return [month.startOf('month').format('YYYY-MM-DD'), month.endOf('month').format('YYYY-MM-DD')];
});
const monthlyStats = computed(() => {
  const rows = monthlyTaskRows.value;
  return {
    doneCount: rows.filter((item) => ['DONE', 'ABNORMAL'].includes(item.rawStatus || '')).length,
    overdueCount: rows.filter((item) => item.rawStatus === 'OVERDUE').length,
    todoCount: rows.filter((item) =>
      ['WAIT_DISPATCH', 'WAIT_EXECUTE', 'EXECUTING', 'OVERDUE'].includes(item.rawStatus || ''),
    ).length,
    waitConfirmCount: rows.filter((item) => item.rawStatus === 'WAIT_CONFIRM').length,
  };
});
const confirmableSelectedIds = computed(() => {
  const selected = new Set(monthlySelectedRowKeys.value);
  return monthlyTaskRows.value
    .filter((item) => item.id && item.rawStatus === 'WAIT_CONFIRM' && selected.has(item.id))
    .map((item) => item.id!);
});
const monthlyRowSelection = computed(() => ({
  selectedRowKeys: monthlySelectedRowKeys.value,
  getCheckboxProps: (record: MesMaintTaskApi.Order) => ({
    disabled: !record.id || record.rawStatus !== 'WAIT_CONFIRM',
  }),
  onChange: (keys: Array<number | string>) => {
    monthlySelectedRowKeys.value = keys
      .map((key) => Number(key))
      .filter((key) => Number.isFinite(key));
  },
}));
const filteredCategoryRows = computed(() => {
  const keyword = candidateCategorySearchValue.value.trim().toLowerCase();
  if (!keyword) {
    return categoryRows.value;
  }
  return categoryRows.value.filter((item) =>
    `${item.categoryName || ''}${item.categoryCode || ''}`.toLowerCase().includes(keyword),
  );
});
const candidateTreeData = computed(() => mapTreeData(filteredCategoryRows.value));
const candidateSelectedRows = computed(() => {
  const selected = new Set(candidateSelectedKeys.value);
  return candidateRows.value.filter((row) => selected.has(candidateKey(row)));
});
const candidateRowSelection = computed(() => ({
  selectedRowKeys: candidateSelectedKeys.value,
  onChange: (keys: Array<number | string>) => {
    candidateSelectedKeys.value = keys.map((key) => String(key));
  },
}));

async function loadMonthlyDetail() {
  monthlyTaskLoading.value = true;
  try {
    const pageSize = Math.min(monthlyTaskPageSize.value, MAX_PAGE_SIZE);
    monthlyTaskPageSize.value = pageSize;
    const result = await getMaintOrderPage({
      pageNo: monthlyTaskPageNo.value,
      pageSize,
      planDate: currentMonthRange.value,
      tabType: 'all',
    });
    monthlyTaskRows.value = result.list || [];
    monthlyTaskTotal.value = result.total || 0;
    monthlySelectedRowKeys.value = [];
  } finally {
    monthlyTaskLoading.value = false;
  }
}

async function loadMaintDueHint() {
  maintDueHintLoading.value = true;
  try {
    const rows = await getMaintCurrentMonthCandidates({ includeNormal: false });
    maintDueHint.value = {
      dueSoonCount: rows.filter((item) => item.dueInCurrentMonth && !item.overdue).length,
      overdueCount: rows.filter((item) => item.overdue).length,
    };
  } finally {
    maintDueHintLoading.value = false;
  }
}

function handleMonthlyTaskTableChange(pagination: { current?: number; pageSize?: number }) {
  monthlyTaskPageNo.value = pagination.current || 1;
  monthlyTaskPageSize.value = Math.min(pagination.pageSize || MONTHLY_TASK_PAGE_SIZE, MAX_PAGE_SIZE);
  void loadMonthlyDetail();
}

function handleTaskOpen(row: MesMaintTaskApi.Order) {
  execModalApi.setData({ id: row.id, status: row.status }).open();
}

function handleCreateCurrentMonth() {
  orderModalApi.setData({ title: '人工新增本月检验' }).open();
}

async function handleBatchConfirm() {
  const ids = confirmableSelectedIds.value;
  if (!ids.length) {
    message.warning('请选择待确认的检验记录');
    return;
  }
  await confirm(`确认通过选中的 ${ids.length} 条待确认检验记录吗？`);
  const count = await confirmMaintOrders(ids);
  message.success(`已确认 ${count} 条检验记录`);
  await handleRefresh();
}

async function handleRefresh() {
  gridApi.query();
  await Promise.all([loadMonthlyDetail(), loadMaintDueHint()]);
}

async function syncRecordLedgerMonthRange(query = true) {
  await gridApi.formApi.setValues({
    actualTime: getMaintRecordMonthRange(recordLedgerMonth.value),
  });
  if (query) {
    gridApi.query();
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

function handleDetail(row: MesMaintTaskApi.Task) {
  formModalApi.setData({ type: 'detail', id: row.id }).open();
}

async function handleExport() {
  const data = await exportTask(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '设备保养记录台账.xls', source: data });
}

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(categoryOptions.value, advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

function collectExpandedKeys(nodes: any[]): Array<number | string> {
  return nodes.flatMap((node) => {
    const currentKey = node.key === undefined ? [] : [node.key];
    return [...currentKey, ...collectExpandedKeys(node.children ?? [])];
  });
}

async function loadCategories() {
  const categories = await getCategoryList();
  categoryRows.value = categories;
  categoryOptions.value = mapCategoryOptions(categories);
  candidateExpandedCategoryKeys.value = collectExpandedKeys(candidateTreeData.value);
}

async function openCandidateModal() {
  if (!categoryRows.value.length) {
    await loadCategories();
  }
  candidateModalOpen.value = true;
  candidateSelectedKeys.value = [];
  await loadCandidates();
}

async function loadCandidates() {
  candidateLoading.value = true;
  try {
    candidateRows.value = await getMaintCurrentMonthCandidates({
      categoryId: candidateQuery.categoryId,
      deviceName: candidateQuery.deviceName || undefined,
      includeNormal: candidateQuery.includeNormal,
    });
    const validKeys = new Set(candidateRows.value.map(candidateKey));
    candidateSelectedKeys.value = candidateSelectedKeys.value.filter((key) => validKeys.has(key));
  } finally {
    candidateLoading.value = false;
  }
}

async function handleCandidateTreeSelect(keys: Array<number | string>) {
  const key = Number(keys[0] ?? ALL_CATEGORY_KEY);
  candidateQuery.categoryId = key > 0 ? key : undefined;
  candidateSelectedCategoryKeys.value = [key > 0 ? key : ALL_CATEGORY_KEY];
  await loadCandidates();
}

async function handleCandidateCategorySearch(e: any) {
  candidateCategorySearchValue.value = e.target.value;
  await nextTick();
  candidateExpandedCategoryKeys.value = collectExpandedKeys(candidateTreeData.value);
}

async function handleCandidateSearch() {
  candidateSelectedKeys.value = [];
  await loadCandidates();
}

async function handleAppendCandidates() {
  if (!candidateSelectedRows.value.length) {
    message.warning('请选择需要追加到本月的设备检验');
    return;
  }
  await confirm(`确认追加选中的 ${candidateSelectedRows.value.length} 条设备检验到本月吗？`);
  candidateSubmitting.value = true;
  try {
    const candidates = candidateSelectedRows.value.map((item) => ({
      deviceId: item.deviceId!,
      standardId: item.standardId!,
      taskDueDate: item.taskDueDate,
    }));
    const count = await appendCurrentMonthMaintOrders({ candidates });
    message.success(`已追加 ${count} 条本月检验`);
    candidateModalOpen.value = false;
    await Promise.all([loadMonthlyDetail(), loadMaintDueHint()]);
  } finally {
    candidateSubmitting.value = false;
  }
}

function candidateKey(row: MesMaintTaskApi.MaintCandidate) {
  return `${row.deviceId || 0}-${row.standardId || 0}`;
}

function warningStatusLabel(row: MesMaintTaskApi.MaintCandidate) {
  if (row.overdue) return '过期未检';
  if (row.dueInCurrentMonth) return '本月到期';
  return '未到期';
}

function warningStatusColor(row: MesMaintTaskApi.MaintCandidate) {
  if (row.overdue) return 'error';
  if (row.dueInCurrentMonth) return 'warning';
  return 'default';
}

function currentActionLabel(row: MesMaintTaskApi.Order) {
  if (row.rawStatus === 'WAIT_CONFIRM') return '维护详情';
  return row.status === 'TODO' ? '维护详情' : '查看报告';
}

function currentActionIcon(row: MesMaintTaskApi.Order) {
  return row.status === 'TODO' || row.rawStatus === 'WAIT_CONFIRM' ? ACTION_ICON.EDIT : ACTION_ICON.VIEW;
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'device-maint-record-vben-grid',
  gridClass: 'device-maint-record-vxe-grid',
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
          await getTaskPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesMaintTaskApi.Task>,
});

const monthlyTaskColumns: TableColumnsType<MesMaintTaskApi.Order> = [
  { dataIndex: 'taskNo', fixed: 'left', title: '检验单号', width: 170 },
  { align: 'center', dataIndex: 'planPeriod', title: '月份', width: 110 },
  { dataIndex: 'deviceCode', fixed: 'left', title: '设备编码', width: 145 },
  { dataIndex: 'deviceName', fixed: 'left', title: '设备名称', width: 180 },
  { dataIndex: 'categoryName', title: '设备分类', width: 130 },
  { dataIndex: 'standardName', title: '保养标准', width: 180 },
  { align: 'center', dataIndex: 'maintType', key: 'maintType', title: '维保类型', width: 110 },
  { align: 'center', dataIndex: 'dueDate', title: '应检日期', width: 120 },
  { align: 'center', dataIndex: 'planDate', title: '计划日期', width: 120 },
  { align: 'center', dataIndex: 'planTime', title: '计划时间', width: 170 },
  { align: 'center', dataIndex: 'rawStatus', key: 'rawStatus', title: '确认状态', width: 120 },
  { align: 'center', dataIndex: 'actualDate', title: '反馈时间', width: 170 },
  { dataIndex: 'executor', title: '执行人', width: 110 },
  { dataIndex: 'remark', title: '备注', width: 220 },
  { fixed: 'right', key: 'actions', title: '操作', width: 150 },
];

const candidateColumns: TableColumnsType<MesMaintTaskApi.MaintCandidate> = [
  { dataIndex: 'deviceCode', fixed: 'left', title: '设备编码', width: 140 },
  { dataIndex: 'deviceName', fixed: 'left', title: '设备名称', width: 180 },
  { dataIndex: 'categoryName', title: '设备分类', width: 130 },
  { dataIndex: 'standardName', title: '保养标准', width: 190 },
  { align: 'center', dataIndex: 'maintType', key: 'maintType', title: '维保类型', width: 110 },
  { dataIndex: 'frequency', title: '周期', width: 110 },
  { align: 'center', dataIndex: 'lastPlanDate', title: '上次计划', width: 120 },
  { align: 'center', dataIndex: 'lastActualDate', title: '上次完成', width: 120 },
  { align: 'center', dataIndex: 'taskDueDate', title: '本次到期', width: 120 },
  { align: 'center', key: 'warningStatus', title: '预警状态', width: 110 },
];

const monthlyTaskPagination = computed(() => ({
  current: monthlyTaskPageNo.value,
  pageSize: monthlyTaskPageSize.value,
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 条`,
  total: monthlyTaskTotal.value,
}));

onMounted(async () => {
  await loadCategories();
  await gridApi.formApi.updateSchema(useGridFormSchema(categoryOptions.value, advancedSearchVisible.value));
  await syncRecordLedgerMonthRange(false);
  await Promise.all([loadMonthlyDetail(), loadMaintDueHint()]);
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <ExecModal @success="handleRefresh" />
    <OrderModal @success="handleRefresh" />

    <Modal
      v-model:open="candidateModalOpen"
      centered
      class="device-maint-candidate-modal"
      destroy-on-close
      title="从到期/过期设备台账追加"
      width="1180px"
    >
      <div class="device-maint-candidate-layout">
        <aside class="device-maint-candidate-tree">
          <Input
            v-model:value="candidateCategorySearchValue"
            allow-clear
            class="device-maint-candidate-tree__search"
            placeholder="搜索设备分类"
            @change="handleCandidateCategorySearch"
          >
            <template #prefix>
              <IconifyIcon icon="lucide:search" class="size-4" />
            </template>
          </Input>
          <Spin :spinning="false" wrapper-class-name="device-maint-candidate-tree__spin">
            <div class="device-maint-candidate-tree__scroll">
              <Tree
                v-if="candidateTreeData.length > 0"
                v-model:expanded-keys="candidateExpandedCategoryKeys"
                block-node
                class="device-maint-candidate-tree__tree"
                :field-names="{ title: 'title', key: 'key', children: 'children' }"
                :selected-keys="candidateSelectedCategoryKeys"
                :show-line="{ showLeafIcon: false }"
                :tree-data="candidateTreeData"
                @select="handleCandidateTreeSelect"
              />
              <div v-else class="py-4 text-center text-gray-500">暂无数据</div>
            </div>
          </Spin>
        </aside>
        <section class="device-maint-candidate-main">
          <div class="device-maint-candidate-toolbar">
            <Space wrap>
              <Input
                v-model:value="candidateQuery.deviceName"
                allow-clear
                class="w-[220px]"
                placeholder="设备名称模糊检索"
                @press-enter="handleCandidateSearch"
              />
              <Checkbox v-model:checked="candidateQuery.includeNormal" @change="handleCandidateSearch">
                包含未到期
              </Checkbox>
              <Button :loading="candidateLoading" type="primary" @click="handleCandidateSearch">
                <IconifyIcon icon="lucide:search" />
                查询
              </Button>
            </Space>
            <span class="device-maint-candidate-toolbar__count">
              已选 {{ candidateSelectedKeys.length }} 条
            </span>
          </div>
          <Table
            bordered
            size="middle"
            :columns="candidateColumns"
            :data-source="candidateRows"
            :loading="candidateLoading"
            :pagination="false"
            :row-key="candidateKey"
            :row-selection="candidateRowSelection"
            :scroll="{ x: 1330, y: 420 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'maintType'">
                <Tag :color="optionColor(MAINT_TYPE_OPTIONS, record.maintType)">
                  {{ optionLabel(MAINT_TYPE_OPTIONS, record.maintType) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'warningStatus'">
                <Tag :color="warningStatusColor(record)">
                  {{ warningStatusLabel(record) }}
                </Tag>
              </template>
            </template>
          </Table>
        </section>
      </div>
      <template #footer>
        <Button @click="candidateModalOpen = false">取消</Button>
        <Button
          :disabled="candidateSelectedRows.length === 0"
          :loading="candidateSubmitting"
          type="primary"
          @click="handleAppendCandidates"
        >
          追加到本月
        </Button>
      </template>
    </Modal>

    <div class="device-maint-record-page">
      <Tabs v-model:activeKey="activeTab" class="device-maint-tabs">
        <Tabs.TabPane key="current" tab="本月检验">
          <div class="device-maint-monthly-pane">
            <div class="device-maint-monthly-toolbar">
              <div class="device-maint-monthly-title">
                <strong>{{ currentMonthLabel }}本月检验</strong>
                <span>固定展示当前月份，历史记录请进入记录台账查询。</span>
              </div>
              <Space wrap>
                <Button type="primary" @click="handleCreateCurrentMonth">
                  <IconifyIcon icon="lucide:plus" />
                  人工新增检验
                </Button>
                <Button @click="openCandidateModal">
                  <IconifyIcon icon="lucide:list-plus" />
                  到期/过期追加
                </Button>
                <Button
                  :disabled="confirmableSelectedIds.length === 0"
                  type="primary"
                  @click="handleBatchConfirm"
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
              class="device-maint-monthly-hint"
              type="info"
              :message="`${currentMonthLabel}检验记录 ${monthlyTaskTotal} 条，即将到期 ${maintDueHint.dueSoonCount || 0} 台，过期未检 ${maintDueHint.overdueCount || 0} 台`"
              :description="`当前页待执行 ${monthlyStats.todoCount} 条，待确认 ${monthlyStats.waitConfirmCount} 条，已完成 ${monthlyStats.doneCount} 条，逾期 ${monthlyStats.overdueCount} 条。待确认记录批量确认后进入记录台账。`"
            >
              <template #action>
                <Button size="small" type="link" :loading="maintDueHintLoading" @click="openCandidateModal">
                  查看明细
                </Button>
              </template>
            </Alert>
            <Table
              bordered
              class="device-maint-fill-table"
              size="middle"
              :columns="monthlyTaskColumns"
              :data-source="monthlyTaskRows"
              :loading="monthlyTaskLoading"
              :pagination="monthlyTaskPagination"
              :row-key="(row) => row.id || row.taskNo"
              :row-selection="monthlyRowSelection"
              :scroll="{ x: 2260, y: '100%' }"
              @change="handleMonthlyTaskTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'maintType'">
                  <Tag :color="optionColor(MAINT_TYPE_OPTIONS, record.maintType)">
                    {{ optionLabel(MAINT_TYPE_OPTIONS, record.maintType) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'rawStatus'">
                  <Tag :color="optionColor(MAINT_ORDER_STATUS_OPTIONS, record.rawStatus)">
                    {{ optionLabel(MAINT_ORDER_STATUS_OPTIONS, record.rawStatus) }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <TableAction
                    :actions="[
                      {
                        label: currentActionLabel(record),
                        type: 'link',
                        icon: currentActionIcon(record),
                        onClick: () => handleTaskOpen(record),
                      },
                    ]"
                  />
                </template>
              </template>
            </Table>
          </div>
        </Tabs.TabPane>

        <Tabs.TabPane key="record" tab="记录台账">
          <div class="device-maint-record-grid-host">
            <Grid table-title="设备保养记录台账">
              <template #toolbar-tools>
                <div class="device-maint-record-toolbar">
                  <Space class="device-maint-record-month-toolbar" size="small">
                    <Button @click="moveRecordLedgerMonth(-1)">
                      <IconifyIcon icon="lucide:chevron-left" />
                    </Button>
                    <DatePicker
                      v-model:value="recordLedgerMonth"
                      :allow-clear="false"
                      :title="recordLedgerMonthLabel"
                      class="device-maint-record-month-toolbar__picker"
                      format="YYYY-MM"
                      picker="month"
                      value-format="YYYY-MM"
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
                        label: $t('ui.actionTitle.export'),
                        type: 'primary',
                        icon: ACTION_ICON.DOWNLOAD,
                        auth: ['mes:resource-device-maint-record:export'],
                        onClick: handleExport,
                      },
                    ]"
                  />
                </div>
              </template>

              <template #expand-after>
                <a class="device-maint-record-page__expand-link" @click="toggleAdvancedSearch">
                  {{ advancedSearchVisible ? '收起' : '展开' }}
                  <IconifyIcon
                    :icon="advancedSearchVisible ? 'lucide:chevron-up' : 'lucide:chevron-down'"
                    class="device-maint-record-page__expand-icon"
                  />
                </a>
              </template>

              <template #status="{ row }">
                <Tag :color="optionColor(MAINT_RESULT_OPTIONS, row.status)">
                  {{ optionLabel(MAINT_RESULT_OPTIONS, row.status) }}
                </Tag>
              </template>

              <template #actions="{ row }">
                <TableAction
                  :actions="[
                    {
                      label: $t('common.detail'),
                      type: 'link',
                      icon: ACTION_ICON.VIEW,
                      onClick: () => handleDetail(row),
                    },
                  ]"
                />
              </template>
            </Grid>
          </div>
        </Tabs.TabPane>
      </Tabs>
    </div>
  </Page>
</template>

<style scoped>
.device-maint-record-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.device-maint-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.device-maint-tabs :deep(.ant-tabs-content-holder),
.device-maint-tabs :deep(.ant-tabs-content),
.device-maint-tabs :deep(.ant-tabs-tabpane),
.device-maint-tabs :deep(.ant-tabs-tabpane-active) {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.device-maint-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}

.device-maint-monthly-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.device-maint-monthly-toolbar {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
}

.device-maint-monthly-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #1f2937;
}

.device-maint-monthly-title strong {
  font-size: 15px;
}

.device-maint-monthly-title span,
.device-maint-candidate-toolbar__count {
  color: #64748b;
  font-size: 12px;
}

.device-maint-monthly-toolbar :deep(.ant-btn),
.device-maint-record-month-toolbar :deep(.ant-btn),
.device-maint-candidate-toolbar :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.device-maint-monthly-hint {
  flex: 0 0 auto;
  margin-bottom: 10px;
}

.device-maint-fill-table {
  flex: 1 1 0;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.device-maint-fill-table :deep(.ant-spin-nested-loading),
.device-maint-fill-table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.device-maint-fill-table :deep(.ant-spin-container) {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.device-maint-fill-table :deep(.ant-table) {
  min-height: 0;
  overflow: hidden;
}

.device-maint-fill-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.device-maint-fill-table :deep(.ant-table-content),
.device-maint-fill-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}

.device-maint-fill-table :deep(.ant-table-placeholder) {
  height: 100%;
}

.device-maint-fill-table :deep(.ant-table-pagination) {
  flex: 0 0 auto;
  margin: 12px 0 0;
  padding-right: 2px;
}

.device-maint-record-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.device-maint-record-month-toolbar {
  flex: 0 0 auto;
}

.device-maint-record-month-toolbar__picker {
  width: 120px;
}

.device-maint-record-grid-host {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.device-maint-record-grid-host :deep(.device-maint-record-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.device-maint-record-grid-host :deep(.device-maint-record-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto minmax(0, 1fr) auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.device-maint-record-grid-host :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.device-maint-record-grid-host :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.device-maint-record-grid-host :deep(.vxe-grid--top-wrapper) {
  display: none !important;
}

.device-maint-record-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 3;
  min-height: 0 !important;
  overflow: hidden !important;
}

.device-maint-record-grid-host :deep(.vxe-grid--bottom-wrapper) {
  display: none !important;
}

.device-maint-record-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 4;
  min-height: 0;
  background: #fff;
}

.device-maint-record-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.device-maint-record-grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.device-maint-record-page__expand-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
}

.device-maint-record-page__expand-link:hover {
  text-decoration: underline;
}

.device-maint-record-page__expand-icon {
  font-size: 14px;
}

.device-maint-candidate-layout {
  display: flex;
  height: 520px;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.device-maint-candidate-tree {
  flex: 0 0 260px;
  min-width: 260px;
  height: 100%;
  padding: 12px;
  overflow: hidden;
  background: #fafafa;
  border-right: 1px solid #e5e7eb;
}

.device-maint-candidate-tree__search {
  margin-bottom: 10px;
}

.device-maint-candidate-tree__spin,
.device-maint-candidate-tree__spin :deep(.ant-spin-container) {
  height: calc(100% - 42px);
}

.device-maint-candidate-tree__scroll {
  height: 100%;
  overflow: auto;
}

.device-maint-candidate-tree__tree {
  background: transparent;
}

.device-maint-candidate-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding: 12px;
  overflow: hidden;
}

.device-maint-candidate-toolbar {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
</style>
