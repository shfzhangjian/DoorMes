<script lang="ts" setup>
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';
import type { MesMaintPlanMatrixApi } from '#/api/mes/resource/device/maint-plan';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Spin,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getCategoryList } from '#/api/mes/resource/device/category';
import { getDevicePage } from '#/api/mes/resource/device/ledger';
import {
  confirmPlan,
  executePlan,
  getAnnualPlanMatrix,
} from '#/api/mes/resource/device/maint-plan';

defineOptions({ name: 'MesMaintExecution' });

type MonthlyLedgerRow = {
  cell: MesMaintPlanMatrixApi.MonthCell;
  item: MesMaintPlanMatrixApi.MatrixItem;
  row: MesMaintPlanMatrixApi.MatrixRow;
};

const months = Array.from({ length: 12 }, (_, index) => index + 1);
const monthOptions = months.map((month) => ({
  label: `${month}月`,
  value: month,
}));
const currentYear = ref<dayjs.Dayjs>(dayjs());
const selectedMonth = ref(dayjs().month() + 1);
const executionFilter = ref<'all' | 'monthPlan' | 'overdue'>('monthPlan');
const viewStyle = ref<'detail' | 'list'>('detail');
const listPageNo = ref(1);
const listPageSize = ref(20);
const matrixRows = ref<MesMaintPlanMatrixApi.MatrixRow[]>([]);
const yearMatrixRows = ref<MesMaintPlanMatrixApi.MatrixRow[]>([]);
const matrixLoading = ref(false);
const deviceRows = ref<MesDeviceLedgerApi.Device[]>([]);
const plannedDeviceCodes = ref<Set<string>>(new Set());
const selectedDeviceId = ref<number>();
const deviceLoading = ref(false);
const deviceNameFilter = ref('');
const deviceCategoryIdFilter = ref<number>();
const categoryOptions = ref<Array<{ label: string; value: number }>>([]);
const detailOpen = ref(false);
const detailRecord = ref<MonthlyLedgerRow>();
const detailRemark = ref('');
const detailSubmitting = ref(false);
const confirmAllSubmitting = ref(false);

const selectedDevice = computed(() =>
  deviceRows.value.find((item) => item.id === selectedDeviceId.value),
);

const filterOptions = [
  { label: '全部设备', value: 'all' },
  { label: '只看本月有计划', value: 'monthPlan' },
  { label: '未执行计划', value: 'overdue' },
];
const executionTipText =
  '提示：点击计划行或操作按钮执行、确认；弹窗可完成并进入下一项。';

const monthPlannedDeviceCodes = computed(
  () =>
    new Set(
      yearMatrixRows.value
        .filter((row) =>
          (row.items || []).some((item) =>
            getMonthCell(item, selectedMonth.value),
          ),
        )
        .map((row) => row.deviceCode)
        .filter(Boolean),
    ),
);

const overdueDeviceCodes = computed(
  () =>
    new Set(
      yearMatrixRows.value
        .filter((row) =>
          flattenMatrixRows([row]).some(
            ({ cell }) => isPlanOverdue(cell) && !isConfirmed(cell),
          ),
        )
        .map((row) => row.deviceCode)
        .filter(Boolean),
    ),
);

const filteredDeviceRows = computed(() => {
  if (executionFilter.value === 'monthPlan') {
    return deviceRows.value.filter((device) =>
      monthPlannedDeviceCodes.value.has(device.deviceCode || ''),
    );
  }
  if (executionFilter.value === 'overdue') {
    return deviceRows.value.filter((device) =>
      overdueDeviceCodes.value.has(device.deviceCode || ''),
    );
  }
  return deviceRows.value;
});

const monthlyRows = computed<MonthlyLedgerRow[]>(() =>
  matrixRows.value.flatMap(
    (row) =>
      (row.items || [])
        .map((item) => {
          const cell = getMonthCell(item, selectedMonth.value);
          return cell ? { cell, item, row } : undefined;
        })
        .filter(Boolean) as MonthlyLedgerRow[],
  ),
);

const detailRows = computed<MonthlyLedgerRow[]>(() => {
  if (executionFilter.value === 'overdue') {
    return flattenMatrixRows(matrixRows.value).filter(
      ({ cell }) => isPlanOverdue(cell) && !isConfirmed(cell),
    );
  }
  return monthlyRows.value;
});

const confirmableDetailRows = computed(() =>
  detailRows.value.filter(({ cell }) => canConfirm(cell)),
);

const executionListRows = computed<MonthlyLedgerRow[]>(() => {
  const rows = flattenMatrixRows(yearMatrixRows.value);
  if (executionFilter.value === 'overdue') {
    return rows.filter(({ cell }) => isPlanOverdue(cell));
  }
  return rows.filter(({ cell }) => cell.monthNo === selectedMonth.value);
});

const pagedExecutionListRows = computed(() => {
  const start = (listPageNo.value - 1) * listPageSize.value;
  return executionListRows.value.slice(start, start + listPageSize.value);
});

const stats = computed(() => {
  const result = {
    deviceCount: filteredDeviceRows.value.length,
    done: 0,
    overdue: 0,
    pending: 0,
    planCount:
      viewStyle.value === 'list'
        ? executionListRows.value.length
        : detailRows.value.length,
  };
  const rows =
    viewStyle.value === 'list' ? executionListRows.value : detailRows.value;
  rows.forEach(({ cell }) => {
    if (isPlanOverdue(cell)) result.overdue += 1;
    if (isConfirmed(cell)) result.done += 1;
    else result.pending += 1;
  });
  return result;
});

const detailCell = computed(() => detailRecord.value?.cell);

onMounted(async () => {
  await Promise.all([loadCategoryOptions(), loadPlannedDeviceCodes()]);
  await loadDeviceList();
});

async function loadCategoryOptions() {
  const rows = await getCategoryList();
  categoryOptions.value = rows
    .filter((item) => item.id && item.parentId && item.parentId > 0)
    .map((item) => ({ label: item.categoryName || '-', value: item.id! }));
}

async function loadPlannedDeviceCodes() {
  const res = await getAnnualPlanMatrix(currentYear.value.year());
  yearMatrixRows.value = res.list || [];
  plannedDeviceCodes.value = new Set(
    yearMatrixRows.value.map((row) => row.deviceCode).filter(Boolean),
  );
}

async function loadDeviceList() {
  deviceLoading.value = true;
  try {
    const res = await getDevicePage({
      categoryId: deviceCategoryIdFilter.value,
      deviceName: deviceNameFilter.value || undefined,
      pageNo: 1,
      pageSize: 200,
      status: 1,
    } as any);
    deviceRows.value = res.list || [];
    syncSelectedDevice();
    await loadMatrixData();
  } finally {
    deviceLoading.value = false;
  }
}

async function loadMatrixData() {
  if (!selectedDeviceId.value) {
    matrixRows.value = [];
    return;
  }
  matrixLoading.value = true;
  try {
    const res = await getAnnualPlanMatrix(
      currentYear.value.year(),
      selectedDeviceId.value,
    );
    matrixRows.value = res.list || [];
  } finally {
    matrixLoading.value = false;
  }
}

async function handleYearChange() {
  listPageNo.value = 1;
  await loadPlannedDeviceCodes();
  syncSelectedDevice();
  await loadMatrixData();
}

async function handleMonthStep(step: number) {
  listPageNo.value = 1;
  const nextMonth = dayjs(
    `${currentYear.value.year()}-${String(selectedMonth.value).padStart(2, '0')}-01`,
  ).add(step, 'month');
  const yearChanged = nextMonth.year() !== currentYear.value.year();
  currentYear.value = nextMonth;
  selectedMonth.value = nextMonth.month() + 1;
  if (yearChanged) {
    await loadPlannedDeviceCodes();
    syncSelectedDevice();
  }
  await loadMatrixData();
}

async function handleMonthSelect() {
  listPageNo.value = 1;
  syncSelectedDevice();
  await loadMatrixData();
}

async function handleFilterChange() {
  listPageNo.value = 1;
  syncSelectedDevice();
  await loadMatrixData();
}

function handleViewStyleChange(style: 'detail' | 'list') {
  listPageNo.value = 1;
  viewStyle.value = style;
}

function handleDeviceSelect(row: MesDeviceLedgerApi.Device) {
  selectedDeviceId.value = row.id;
  loadMatrixData();
}

function getMonthCell(item: MesMaintPlanMatrixApi.MatrixItem, month: number) {
  return item.months?.[month];
}

function getItemGroupText(item: MesMaintPlanMatrixApi.MatrixItem) {
  return item.itemGroup || '-';
}

function getStatus(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return (
    cell?.executeStatus || cell?.cellStatus || cell?.orderStatus || 'PENDING'
  );
}

function isSubmitted(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return (
    ['ABNORMAL', 'DONE', 'WAIT_CONFIRM'].includes(getStatus(cell)) ||
    !!cell?.actualDate
  );
}

function isConfirmed(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return ['ABNORMAL', 'DONE'].includes(getStatus(cell));
}

function isOverdue(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return getStatus(cell) === 'OVERDUE' || !!cell?.overdue;
}

function isPlanOverdue(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (!cell) return false;
  if (cell.planEndDate) {
    return dayjs(cell.planEndDate).isBefore(dayjs(), 'day');
  }
  return isOverdue(cell);
}

function getExecutionStatusText(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (isConfirmed(cell)) return '已确认';
  if (isSubmitted(cell)) return '执行';
  return '未执行';
}

function getExecutionStatusColor(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (isConfirmed(cell)) return 'success';
  if (isSubmitted(cell)) return 'processing';
  return 'default';
}

function canComplete(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return !!cell?.id && !isSubmitted(cell);
}

function canConfirm(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return !!cell?.id && getStatus(cell) === 'WAIT_CONFIRM';
}

function getExecutionRowClass(record: MonthlyLedgerRow) {
  if (canConfirm(record.cell)) return 'execution-row--wait-confirm';
  if (isPlanOverdue(record.cell) && !isConfirmed(record.cell)) {
    return 'execution-row--overdue';
  }
  return '';
}

function openExecutionDetail(record: MonthlyLedgerRow) {
  detailRecord.value = record;
  detailRemark.value = record.cell.executeRemark || '';
  detailOpen.value = true;
}

function closeExecutionDetail() {
  detailOpen.value = false;
  detailRecord.value = undefined;
  detailRemark.value = '';
}

async function submitDetailComplete(advanceNext: boolean) {
  const cell = detailCell.value;
  if (!cell || !canComplete(cell)) return;
  const executeRemark = detailRemark.value.trim();
  if (!executeRemark) {
    message.warning('请先填写完成情况');
    return;
  }
  detailSubmitting.value = true;
  try {
    const currentCellId = cell.id;
    await executePlan({ executeRemark, id: cell.id });
    await reloadExecutionView();
    if (advanceNext) {
      const nextRecord = findNextCompletableRecord(currentCellId);
      if (nextRecord) {
        openExecutionDetail(nextRecord);
        message.success('已提交完成，已进入下一项');
      } else {
        closeExecutionDetail();
        message.success('已提交完成，当前无下一项待执行');
      }
      return;
    }
    message.success('已提交完成，等待确认');
    closeExecutionDetail();
  } finally {
    detailSubmitting.value = false;
  }
}

async function handleDetailComplete() {
  await submitDetailComplete(false);
}

async function handleDetailCompleteAndNext() {
  await submitDetailComplete(true);
}

async function handleDetailConfirm() {
  const cell = detailCell.value;
  if (!cell || !canConfirm(cell)) return;
  detailSubmitting.value = true;
  try {
    await confirmPlan([cell.id]);
    message.success('已确认');
    closeExecutionDetail();
    await reloadExecutionView();
  } finally {
    detailSubmitting.value = false;
  }
}

async function handleConfirmAllSelectedDevice() {
  const ids = confirmableDetailRows.value
    .map(({ cell }) => cell.id)
    .filter((id): id is number => !!id);
  if (ids.length === 0) {
    message.info('当前设备暂无待确认保养项目');
    return;
  }
  confirmAllSubmitting.value = true;
  try {
    await confirmPlan(ids);
    closeExecutionDetail();
    await reloadExecutionView();
    message.success(`已确认 ${ids.length} 项`);
  } finally {
    confirmAllSubmitting.value = false;
  }
}

async function reloadExecutionView() {
  await loadPlannedDeviceCodes();
  syncSelectedDevice();
  await loadMatrixData();
}

function getActiveExecutionRows() {
  return viewStyle.value === 'list'
    ? executionListRows.value
    : detailRows.value;
}

function findNextCompletableRecord(currentCellId?: number) {
  const rows = getActiveExecutionRows();
  const currentIndex = rows.findIndex(({ cell }) => cell.id === currentCellId);
  const candidates = currentIndex === -1 ? rows : rows.slice(currentIndex + 1);
  return candidates.find(({ cell }) => canComplete(cell));
}

function getGroupRowSpan(index: number) {
  const rows = detailRows.value;
  const current = rows[index];
  if (!current) return 0;
  const group = getItemGroupText(current.item);
  if (index > 0 && getItemGroupText(rows[index - 1].item) === group) {
    return 0;
  }
  let count = 1;
  for (let i = index + 1; i < rows.length; i += 1) {
    if (getItemGroupText(rows[i].item) !== group) break;
    count += 1;
  }
  return count;
}

function getGroupSeq(index: number) {
  let seq = 0;
  for (let i = 0; i <= index; i += 1) {
    if (getGroupRowSpan(i) > 0) {
      seq += 1;
    }
  }
  return seq;
}

function hasDevicePlan(device: MesDeviceLedgerApi.Device) {
  return !!device.deviceCode && plannedDeviceCodes.value.has(device.deviceCode);
}

function getDeviceLocation() {
  const device = selectedDevice.value as any;
  return device?.location || device?.specificLocation || '-';
}

function flattenMatrixRows(rows: MesMaintPlanMatrixApi.MatrixRow[]) {
  return rows.flatMap((row) =>
    (row.items || []).flatMap((item) =>
      Object.values(item.months || {})
        .filter(Boolean)
        .map((cell) => ({ cell, item, row })),
    ),
  );
}

function syncSelectedDevice() {
  if (
    selectedDeviceId.value &&
    filteredDeviceRows.value.some((item) => item.id === selectedDeviceId.value)
  ) {
    return;
  }
  selectedDeviceId.value = filteredDeviceRows.value[0]?.id;
}

function getRecordMonthText(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (!cell?.monthNo) return '-';
  return `${cell.monthNo}月${cell.weekLabel ? ` ${cell.weekLabel}` : ''}`;
}

function getRecordActionText(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (canConfirm(cell)) return '确认';
  if (canComplete(cell)) return '执行';
  return '查看';
}

function getRecordActionIcon(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (canConfirm(cell)) return 'lucide:badge-check';
  if (canComplete(cell)) return 'lucide:square-pen';
  return 'lucide:eye';
}
</script>

<template>
  <Page auto-content-height>
    <div class="execution-page">
      <div class="execution-toolbar">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2">
            <IconifyIcon
              icon="lucide:calendar-check"
              class="text-xl text-sky-700"
            />
            <span class="text-base font-bold text-slate-800">
              设备保养执行记录
            </span>
          </div>
          <div class="h-6 w-px bg-slate-300"></div>
          <DatePicker
            v-model:value="currentYear"
            picker="year"
            :allow-clear="false"
            class="w-28"
            @change="handleYearChange"
          />
          <div class="month-stepper">
            <Button size="small" title="上一月" @click="handleMonthStep(-1)">
              <IconifyIcon icon="lucide:chevron-left" />
            </Button>
            <Select
              v-model:value="selectedMonth"
              class="w-24"
              :options="monthOptions"
              @change="handleMonthSelect"
            />
            <Button size="small" title="下一月" @click="handleMonthStep(1)">
              <IconifyIcon icon="lucide:chevron-right" />
            </Button>
          </div>
          <Select
            v-model:value="executionFilter"
            class="w-36"
            :options="filterOptions"
            @change="handleFilterChange"
          />
          <div class="view-switch">
            <button
              class="view-switch__button"
              :class="{ 'view-switch__button--active': viewStyle === 'detail' }"
              type="button"
              @click="handleViewStyleChange('detail')"
            >
              <IconifyIcon icon="lucide:panel-left" />
              <span>设备明细</span>
            </button>
            <button
              class="view-switch__button"
              :class="{ 'view-switch__button--active': viewStyle === 'list' }"
              type="button"
              @click="handleViewStyleChange('list')"
            >
              <IconifyIcon icon="lucide:list-checks" />
              <span>未执行列表</span>
            </button>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <div class="summary-pill">
            设备 <strong>{{ stats.deviceCount }}</strong>
          </div>
          <div class="summary-pill">
            计划 <strong>{{ stats.planCount }}</strong>
          </div>
          <div class="summary-pill summary-pill--green">
            已确认 <strong>{{ stats.done }}</strong>
          </div>
          <div class="summary-pill summary-pill--yellow">
            逾期 <strong>{{ stats.overdue }}</strong>
          </div>
          <div class="summary-pill">
            待处理 <strong>{{ stats.pending }}</strong>
          </div>
        </div>
      </div>

      <div v-if="viewStyle === 'list'" class="execution-list-main">
        <div
          v-if="executionListRows.length === 0"
          class="flex h-full items-center justify-center text-sm text-slate-500"
        >
          当前暂无保养计划记录
        </div>
        <div v-else class="execution-scroll execution-scroll--list">
          <table class="execution-table execution-table--list">
            <thead>
              <tr>
                <th class="col-seq sticky-list sticky-list--seq">序号</th>
                <th class="col-device-code sticky-list sticky-list--code">
                  设备编号
                </th>
                <th class="col-device-name sticky-list sticky-list--name">
                  设备名称
                </th>
                <th class="col-date sticky-list sticky-list--month">月份</th>
                <th class="col-item-group">保养项目</th>
                <th class="col-item">保养部位</th>
                <th class="col-method">保养方法</th>
                <th class="col-standard">保养标准</th>
                <th class="col-frequency">保养周期</th>
                <th class="col-person">保养人</th>
                <th class="col-time">保养时间</th>
                <th class="col-person">确认人</th>
                <th class="col-time">确认时间</th>
                <th class="col-status">状态</th>
                <th class="col-overdue">逾期</th>
                <th class="col-action sticky-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(record, index) in pagedExecutionListRows"
                :key="`list-${record.row.id}-${record.item.id}-${record.cell.id}`"
                class="execution-row"
                :class="getExecutionRowClass(record)"
                @click="openExecutionDetail(record)"
              >
                <td
                  class="sticky-list sticky-list--seq text-center font-mono text-xs text-slate-500"
                >
                  {{ (listPageNo - 1) * listPageSize + index + 1 }}
                </td>
                <td
                  class="sticky-list sticky-list--code font-mono text-xs font-bold text-slate-700"
                >
                  {{ record.row.deviceCode }}
                </td>
                <td
                  class="sticky-list sticky-list--name font-semibold text-slate-800"
                >
                  {{ record.row.deviceName }}
                </td>
                <td class="sticky-list sticky-list--month text-center">
                  {{ getRecordMonthText(record.cell) }}
                </td>
                <td class="item-group-cell">
                  {{ getItemGroupText(record.item) }}
                </td>
                <td>{{ record.item.itemName }}</td>
                <td>{{ record.item.method || '-' }}</td>
                <td>{{ record.item.requirement || '-' }}</td>
                <td class="text-center">
                  <Tag color="blue" class="!m-0">
                    {{ record.item.frequency || '-' }}
                  </Tag>
                </td>
                <td class="text-center">
                  {{ record.cell.executor || '-' }}
                </td>
                <td class="text-center">
                  {{ record.cell.actualDate || '-' }}
                </td>
                <td class="text-center">
                  {{ record.cell.confirmer || '-' }}
                </td>
                <td class="text-center">
                  {{ record.cell.confirmTime || '-' }}
                </td>
                <td class="text-center">
                  <Tag
                    :color="getExecutionStatusColor(record.cell)"
                    class="!m-0"
                  >
                    {{ getExecutionStatusText(record.cell) }}
                  </Tag>
                </td>
                <td class="text-center">
                  <Tag
                    v-if="isPlanOverdue(record.cell)"
                    color="warning"
                    class="!m-0"
                  >
                    逾期
                  </Tag>
                  <span v-else>-</span>
                </td>
                <td class="sticky-action text-center">
                  <Button
                    size="small"
                    :type="
                      canConfirm(record.cell) || canComplete(record.cell)
                        ? 'primary'
                        : 'default'
                    "
                    @click.stop="openExecutionDetail(record)"
                  >
                    <IconifyIcon
                      :icon="getRecordActionIcon(record.cell)"
                      class="mr-1"
                    />
                    {{ getRecordActionText(record.cell) }}
                  </Button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="executionListRows.length > 0" class="list-pagination">
          <div class="execution-tip">
            <IconifyIcon icon="lucide:info" />
            <span>{{ executionTipText }}</span>
          </div>
          <Pagination
            v-model:current="listPageNo"
            v-model:page-size="listPageSize"
            :page-size-options="['10', '20', '50', '100']"
            :show-size-changer="true"
            :show-total="(total) => `共 ${total} 条`"
            :total="executionListRows.length"
            size="small"
          />
        </div>
      </div>

      <div v-else class="execution-layout">
        <aside class="device-list-panel">
          <div class="device-list-panel__head">
            <div class="flex items-center gap-2 font-bold text-slate-800">
              <IconifyIcon icon="lucide:monitor-cog" />
              <span>设备列表</span>
            </div>
            <Tag color="blue" class="!m-0">{{ filteredDeviceRows.length }}</Tag>
          </div>
          <div class="device-list-panel__filters">
            <Input
              v-model:value="deviceNameFilter"
              allow-clear
              placeholder="按设备名称过滤"
              @press-enter="loadDeviceList"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:search" class="text-slate-400" />
              </template>
            </Input>
            <Select
              v-model:value="deviceCategoryIdFilter"
              allow-clear
              placeholder="按设备分类过滤"
              :options="categoryOptions"
              @change="loadDeviceList"
            />
            <Button block @click="loadDeviceList">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
              刷新
            </Button>
          </div>
          <Spin :spinning="deviceLoading">
            <div class="device-list-panel__body">
              <button
                v-for="device in filteredDeviceRows"
                :key="device.id"
                class="device-list-item"
                :class="{
                  'device-list-item--active': selectedDeviceId === device.id,
                  'device-list-item--planned': hasDevicePlan(device),
                }"
                type="button"
                @click="handleDeviceSelect(device)"
              >
                <span class="device-list-item__line">
                  <span class="font-mono text-xs font-bold text-slate-600">
                    {{ device.deviceCode || 'NO-CODE' }}
                  </span>
                  <IconifyIcon
                    v-if="hasDevicePlan(device)"
                    icon="lucide:calendar-check"
                    class="device-list-item__plan-icon"
                  />
                </span>
                <span class="truncate text-sm font-bold text-slate-900">
                  {{ device.deviceName }}
                </span>
                <span class="truncate text-xs text-slate-500">
                  {{ device.categoryName || '-' }}
                </span>
              </button>
            </div>
          </Spin>
        </aside>

        <main class="execution-main" :class="{ 'opacity-60': matrixLoading }">
          <div
            v-if="!selectedDeviceId"
            class="flex h-full items-center justify-center text-sm text-slate-500"
          >
            请先在左侧选择设备
          </div>
          <div
            v-else-if="detailRows.length === 0"
            class="flex h-full items-center justify-center text-sm text-slate-500"
          >
            <span v-if="executionFilter === 'overdue'">
              当前设备暂无逾期未执行计划
            </span>
            <span v-else>
              当前设备在 {{ currentYear.year() }} 年
              {{ selectedMonth }} 月暂无保养计划
            </span>
          </div>
          <section v-else class="execution-section">
            <div class="execution-section__head">
              <div>
                <span class="head-label">设备编号：</span>
                <span class="font-mono font-bold text-slate-900">{{
                  selectedDevice?.deviceCode
                }}</span>
              </div>
              <div>
                <span class="head-label">设备名称：</span>
                <span class="font-bold text-slate-900">{{
                  selectedDevice?.deviceName
                }}</span>
              </div>
              <div>
                <span class="head-label">设备位置：</span>
                <span class="font-semibold text-slate-900">{{
                  getDeviceLocation()
                }}</span>
              </div>
              <div>
                <span class="head-label">执行月份：</span>
                <span class="font-semibold text-slate-900">
                  {{ currentYear.year() }}年{{ selectedMonth }}月
                </span>
              </div>
              <div class="execution-section__actions">
                <Button
                  size="small"
                  type="primary"
                  :disabled="confirmableDetailRows.length === 0"
                  :loading="confirmAllSubmitting"
                  @click="handleConfirmAllSelectedDevice"
                >
                  <IconifyIcon icon="lucide:badge-check" class="mr-1" />
                  全部确认
                  <span v-if="confirmableDetailRows.length > 0">
                    ({{ confirmableDetailRows.length }})
                  </span>
                </Button>
              </div>
            </div>
            <div class="execution-scroll">
              <table class="execution-table execution-table--detail">
                <thead>
                  <tr>
                    <th class="col-seq sticky-seq">序号</th>
                    <th class="col-item-group">保养项目</th>
                    <th class="col-item">保养部位</th>
                    <th class="col-method">保养方法</th>
                    <th class="col-standard">保养标准</th>
                    <th class="col-frequency">保养周期</th>
                    <th class="col-result">完成情况</th>
                    <th class="col-person">维保人</th>
                    <th class="col-person">确认人</th>
                    <th class="col-date">完成日期</th>
                    <th class="col-action">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(record, index) in detailRows"
                    :key="`${record.row.id}-${record.item.id}-${record.cell.id}`"
                    class="execution-row"
                    :class="getExecutionRowClass(record)"
                    @click="openExecutionDetail(record)"
                  >
                    <td
                      v-if="getGroupRowSpan(index) > 0"
                      class="sticky-seq text-center font-mono text-xs text-slate-500"
                      :rowspan="getGroupRowSpan(index)"
                    >
                      {{ getGroupSeq(index) }}
                    </td>
                    <td
                      v-if="getGroupRowSpan(index) > 0"
                      class="item-group-cell"
                      :rowspan="getGroupRowSpan(index)"
                    >
                      {{ getItemGroupText(record.item) }}
                    </td>
                    <td class="font-semibold text-slate-800">
                      {{ record.item.itemName }}
                    </td>
                    <td>{{ record.item.method || '-' }}</td>
                    <td>{{ record.item.requirement || '-' }}</td>
                    <td class="text-center">
                      <Tag color="blue" class="!m-0">
                        {{ record.item.frequency || '-' }}
                      </Tag>
                    </td>
                    <td class="result-cell">
                      <div class="result-cell__head">
                        <Tag
                          v-if="
                            isOverdue(record.cell) && !isSubmitted(record.cell)
                          "
                          color="warning"
                          class="!m-0"
                        >
                          逾期
                        </Tag>
                        <Tag
                          v-else-if="canConfirm(record.cell)"
                          color="processing"
                          class="!m-0"
                        >
                          待确认
                        </Tag>
                        <Tag
                          v-else-if="isConfirmed(record.cell)"
                          color="success"
                          class="!m-0"
                        >
                          已确认
                        </Tag>
                        <Tag v-else color="default" class="!m-0">未完成</Tag>
                        <span class="text-xs text-slate-500">
                          {{ getRecordMonthText(record.cell) }}
                        </span>
                      </div>
                      <span class="result-summary">
                        {{ record.cell.executeRemark || '未填写完成情况' }}
                      </span>
                    </td>
                    <td class="text-center">
                      <span class="font-semibold text-slate-800">
                        {{ record.cell.executor || '-' }}
                      </span>
                    </td>
                    <td class="text-center">
                      <Button
                        v-if="canConfirm(record.cell)"
                        size="small"
                        type="primary"
                        @click.stop="handleConfirm(record.cell)"
                      >
                        确认
                      </Button>
                      <div v-else class="person-cell">
                        <span>{{ record.cell.confirmer || '-' }}</span>
                        <small v-if="record.cell.confirmTime">
                          {{ record.cell.confirmTime }}
                        </small>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="font-mono text-xs text-slate-700">
                        {{
                          record.cell.actualDate
                            ? record.cell.actualDate.slice(0, 10)
                            : '-'
                        }}
                      </span>
                    </td>
                    <td class="text-center">
                      <Button
                        size="small"
                        :type="
                          canConfirm(record.cell) || canComplete(record.cell)
                            ? 'primary'
                            : 'default'
                        "
                        @click.stop="openExecutionDetail(record)"
                      >
                        <IconifyIcon
                          :icon="getRecordActionIcon(record.cell)"
                          class="mr-1"
                        />
                        {{ getRecordActionText(record.cell) }}
                      </Button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="execution-tip execution-tip--section">
              <IconifyIcon icon="lucide:info" />
              <span>{{ executionTipText }}</span>
            </div>
          </section>
        </main>
      </div>

      <Modal
        v-model:open="detailOpen"
        destroy-on-close
        title="保养执行详情"
        width="760px"
        @cancel="closeExecutionDetail"
      >
        <div v-if="detailRecord" class="detail-dialog">
          <div class="detail-grid">
            <div>
              <span class="head-label">设备编号：</span>
              <strong class="font-mono">{{
                detailRecord.row.deviceCode
              }}</strong>
            </div>
            <div>
              <span class="head-label">设备名称：</span>
              <strong>{{ detailRecord.row.deviceName }}</strong>
            </div>
            <div>
              <span class="head-label">执行月份：</span>
              <strong>{{ currentYear.year() }}年{{ selectedMonth }}月</strong>
            </div>
            <div>
              <span class="head-label">计划周次：</span>
              <strong>{{ detailRecord.cell.weekLabel || '-' }}</strong>
            </div>
            <div>
              <span class="head-label">保养项目：</span>
              <strong>{{ getItemGroupText(detailRecord.item) }}</strong>
            </div>
            <div>
              <span class="head-label">保养部位：</span>
              <strong>{{ detailRecord.item.itemName }}</strong>
            </div>
          </div>

          <div class="detail-status-row">
            <Tag
              v-if="
                isOverdue(detailRecord.cell) && !isSubmitted(detailRecord.cell)
              "
              color="warning"
              class="!m-0"
            >
              逾期
            </Tag>
            <Tag
              v-else-if="canConfirm(detailRecord.cell)"
              color="processing"
              class="!m-0"
            >
              待确认
            </Tag>
            <Tag
              v-else-if="isConfirmed(detailRecord.cell)"
              color="success"
              class="!m-0"
            >
              已确认
            </Tag>
            <Tag v-else color="default" class="!m-0">未完成</Tag>
            <span>{{ detailRecord.item.frequency || '-' }}</span>
          </div>

          <div class="detail-block">
            <h4>保养方法</h4>
            <p>{{ detailRecord.item.method || '-' }}</p>
          </div>
          <div class="detail-block">
            <h4>保养标准</h4>
            <p>{{ detailRecord.item.requirement || '-' }}</p>
          </div>

          <div class="detail-block">
            <h4>完成情况</h4>
            <textarea
              v-model="detailRemark"
              class="detail-textarea"
              :disabled="!canComplete(detailRecord.cell)"
              placeholder="填写本次保养完成情况"
            ></textarea>
          </div>

          <div class="detail-grid detail-grid--audit">
            <div>
              <span class="head-label">维保人：</span>
              <strong>{{ detailRecord.cell.executor || '-' }}</strong>
            </div>
            <div>
              <span class="head-label">完成时间：</span>
              <strong>{{ detailRecord.cell.actualDate || '-' }}</strong>
            </div>
            <div>
              <span class="head-label">确认人：</span>
              <strong>{{ detailRecord.cell.confirmer || '-' }}</strong>
            </div>
            <div>
              <span class="head-label">确认时间：</span>
              <strong>{{ detailRecord.cell.confirmTime || '-' }}</strong>
            </div>
          </div>
        </div>
        <template #footer>
          <Button @click="closeExecutionDetail">关闭</Button>
          <Button
            v-if="detailRecord && canComplete(detailRecord.cell)"
            type="primary"
            :loading="detailSubmitting"
            @click="handleDetailComplete()"
          >
            提交完成
          </Button>
          <Button
            v-if="detailRecord && canComplete(detailRecord.cell)"
            type="primary"
            :loading="detailSubmitting"
            @click="handleDetailCompleteAndNext"
          >
            完成并下一项
          </Button>
          <Button
            v-if="detailRecord && canConfirm(detailRecord.cell)"
            type="primary"
            :loading="detailSubmitting"
            @click="handleDetailConfirm"
          >
            确认
          </Button>
        </template>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.execution-page {
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px;
}

.execution-toolbar {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  display: flex;
  flex-shrink: 0;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 12px;
}

.month-stepper {
  align-items: center;
  display: inline-flex;
  gap: 6px;
}

.view-switch {
  background: #e2e8f0;
  border-radius: 4px;
  display: inline-flex;
  padding: 3px;
}

.view-switch__button {
  align-items: center;
  border-radius: 4px;
  color: #475569;
  display: inline-flex;
  font-size: 13px;
  font-weight: 700;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
}

.view-switch__button--active {
  background: #fff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 12%);
  color: #0369a1;
}

.summary-pill {
  align-items: center;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #475569;
  display: inline-flex;
  font-size: 13px;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
}

.summary-pill strong {
  color: #0f172a;
}

.summary-pill--green strong {
  color: #15803d;
}

.summary-pill--yellow strong {
  color: #a16207;
}

.execution-layout {
  display: grid;
  flex: 1;
  gap: 12px;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
}

.execution-list-main {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.device-list-panel {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.device-list-panel__head {
  align-items: center;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  height: 44px;
  justify-content: space-between;
  padding: 0 12px;
}

.device-list-panel__filters {
  border-bottom: 1px solid #e2e8f0;
  display: grid;
  gap: 8px;
  padding: 10px;
}

.device-list-panel__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.device-list-panel :deep(.ant-spin-nested-loading),
.device-list-panel :deep(.ant-spin-container) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.device-list-item {
  border: 1px solid transparent;
  border-radius: 4px;
  display: grid;
  gap: 2px;
  min-height: 64px;
  padding: 8px 10px;
  text-align: left;
  width: 100%;
}

.device-list-item__line {
  align-items: center;
  display: flex;
  gap: 6px;
  justify-content: space-between;
  min-width: 0;
}

.device-list-item__plan-icon {
  color: #16a34a;
  flex: 0 0 auto;
  font-size: 16px;
}

.device-list-item:hover {
  background: #f8fafc;
}

.device-list-item--planned {
  border-color: #bfdbfe;
}

.device-list-item--active {
  background: #eff6ff;
  border-color: #60a5fa;
}

.execution-main {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.execution-section {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.execution-section__head {
  align-items: center;
  background: #f8fafc;
  border-bottom: 1px solid #334155;
  display: grid;
  gap: 8px 20px;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) auto;
  padding: 12px;
}

.execution-section__actions {
  display: flex;
  justify-content: flex-end;
  min-width: 120px;
}

.head-label {
  color: #64748b;
  font-size: 13px;
}

.execution-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.execution-scroll--list {
  border-bottom: 1px solid #e2e8f0;
}

.list-pagination {
  align-items: center;
  background: #fff;
  display: flex;
  flex-shrink: 0;
  gap: 12px;
  height: 44px;
  justify-content: space-between;
  padding: 0 12px;
}

.execution-tip {
  align-items: center;
  color: #64748b;
  display: flex;
  flex: 1;
  font-size: 12px;
  gap: 6px;
  min-width: 0;
  white-space: nowrap;
}

.execution-tip--section {
  background: #fff;
  border-top: 1px solid #e2e8f0;
  flex: 0 0 auto;
  height: 36px;
  padding: 0 12px;
}

.execution-table {
  border-collapse: collapse;
  min-width: 1580px;
  table-layout: fixed;
  width: 100%;
}

.execution-table--detail,
.execution-table--list {
  border-collapse: separate;
  border-spacing: 0;
}

.execution-table--detail {
  min-width: 1680px;
}

.execution-table--list {
  min-width: 2240px;
}

.execution-table th {
  background: #9bbadd;
  border: 1px solid #334155;
  color: #0f172a;
  font-weight: 700;
  height: 38px;
  position: sticky;
  text-align: center;
  top: 0;
  z-index: 4;
}

.execution-table td {
  border: 1px solid #94a3b8;
  color: #1e293b;
  font-size: 13px;
  line-height: 1.35;
  min-height: 56px;
  padding: 6px 8px;
  vertical-align: middle;
  word-break: break-word;
}

.col-seq {
  width: 54px;
}

.col-item-group {
  width: 126px;
}

.col-item {
  width: 150px;
}

.col-method {
  width: 260px;
}

.col-standard {
  width: 280px;
}

.col-frequency {
  width: 110px;
}

.col-device-code {
  width: 118px;
}

.col-device-name {
  width: 160px;
}

.col-time {
  width: 150px;
}

.col-status {
  width: 86px;
}

.col-overdue {
  width: 72px;
}

.col-result {
  width: 270px;
}

.col-person {
  width: 92px;
}

.col-date {
  width: 92px;
}

.col-action {
  width: 104px;
}

.sticky-seq {
  background: #fff;
  left: 0;
  position: sticky;
  z-index: 3;
}

th.sticky-seq {
  background: #9bbadd;
  z-index: 5;
}

.sticky-list {
  background: #fff;
  position: sticky;
  z-index: 3;
}

th.sticky-list {
  background: #9bbadd;
  z-index: 7;
}

.sticky-list--seq {
  left: 0;
}

.sticky-list--code {
  left: 54px;
}

.sticky-list--name {
  left: 172px;
}

.sticky-list--month {
  left: 332px;
}

.execution-table--list .sticky-action {
  background: #fff;
  box-shadow: -6px 0 10px rgb(15 23 42 / 10%);
  position: sticky;
  right: 0;
  z-index: 5;
}

.execution-table--list th.sticky-action {
  background: #9bbadd;
  box-shadow: none;
  z-index: 8;
}

.item-group-cell {
  background: #f8fafc;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
}

.execution-table tbody tr.execution-row {
  cursor: pointer;
}

.execution-table tbody tr.execution-row--overdue > td {
  background: #fffbeb;
}

.execution-table tbody tr.execution-row--wait-confirm > td {
  background: #eff6ff;
}

.execution-table tbody tr.execution-row--overdue > .sticky-list,
.execution-table tbody tr.execution-row--overdue > .sticky-seq,
.execution-table tbody tr.execution-row--overdue > .sticky-action {
  background: #fffbeb;
}

.execution-table tbody tr.execution-row--wait-confirm > .sticky-list,
.execution-table tbody tr.execution-row--wait-confirm > .sticky-seq,
.execution-table tbody tr.execution-row--wait-confirm > .sticky-action {
  background: #eff6ff;
}

.execution-table tbody tr:hover > td,
.execution-table tbody tr:hover > .sticky-list,
.execution-table tbody tr:hover > .sticky-seq,
.execution-table tbody tr:hover > .sticky-action {
  background: #f8fafc;
}

.result-cell {
  display: grid;
  gap: 6px;
}

.result-cell__head {
  align-items: center;
  display: flex;
  gap: 6px;
}

.result-summary {
  color: #475569;
  display: block;
  font-size: 12px;
  line-height: 1.35;
  max-height: 34px;
  overflow: hidden;
}

.result-textarea {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  min-height: 42px;
  outline: none;
  padding: 6px 8px;
  resize: vertical;
  width: 100%;
}

.result-textarea:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgb(96 165 250 / 15%);
}

.result-textarea:disabled {
  background: #f8fafc;
  color: #475569;
}

.person-cell {
  display: grid;
  gap: 2px;
}

.person-cell small {
  color: #64748b;
  font-size: 11px;
}

.detail-dialog {
  display: grid;
  gap: 12px;
}

.detail-grid {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: grid;
  gap: 10px 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 12px;
}

.detail-grid--audit {
  background: #f8fafc;
}

.detail-status-row {
  align-items: center;
  display: flex;
  gap: 8px;
}

.detail-block {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 12px;
}

.detail-block h4 {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 6px;
}

.detail-block p {
  color: #334155;
  line-height: 1.5;
  margin: 0;
}

.detail-textarea {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  min-height: 96px;
  outline: none;
  padding: 8px 10px;
  resize: vertical;
  width: 100%;
}

.detail-textarea:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgb(96 165 250 / 15%);
}

.detail-textarea:disabled {
  background: #f8fafc;
  color: #475569;
}
</style>
