<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';
import type { MesMaintPlanMatrixApi } from '#/api/mes/resource/device/maint-plan';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Spin,
  Tag,
  Upload,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getCategoryList } from '#/api/mes/resource/device/category';
import { getDevicePage } from '#/api/mes/resource/device/ledger';
import {
  copyPlanYear,
  exportPlanExcel,
  getAnnualPlanMatrix,
  getPlanList,
  importPlanExcel,
  savePlanWeek,
} from '#/api/mes/resource/device/maint-plan';

import { useListGridColumns, useListGridSchema } from './data';
import CellEditModal from './modules/cell-edit-modal.vue';

defineOptions({ name: 'MesMaintPlan' });

const months = Array.from({ length: 12 }, (_, index) => index + 1);
const currentYear = ref<dayjs.Dayjs>(dayjs());
const viewMode = ref<'actual' | 'list' | 'plan'>('plan');
const matrixRows = ref<MesMaintPlanMatrixApi.MatrixRow[]>([]);
const matrixLoading = ref(false);
const deviceRows = ref<MesDeviceLedgerApi.Device[]>([]);
const plannedDeviceCodes = ref<Set<string>>(new Set());
const selectedDeviceId = ref<number>();
const deviceLoading = ref(false);
const deviceNameFilter = ref('');
const deviceCategoryIdFilter = ref<number>();
const categoryOptions = ref<Array<{ label: string; value: number }>>([]);

const [CellModal, cellModalApi] = useVbenModal({
  connectedComponent: CellEditModal,
  destroyOnClose: true,
});

const [ListGrid, listGridApi] = useVbenVxeGrid({
  formOptions: { schema: useListGridSchema(), collapsed: false },
  gridOptions: {
    columns: useListGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getPlanList({
            ...formValues,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            deviceCode: selectedDevice.value?.deviceCode,
            planYear: currentYear.value.year(),
          }),
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<any>,
});

const stats = computed(() => {
  const result = {
    deviceCount: deviceRows.value.length,
    done: 0,
    overdue: 0,
    pending: 0,
    planCount: 0,
    standardCount: matrixRows.value.length,
  };
  matrixRows.value.forEach((row) => {
    row.items?.forEach((item) => {
      months.forEach((month) => {
        const cell = getMonthCell(item, month);
        if (!cell) return;
        result.planCount += 1;
        if (cell.cellStatus === 'DONE') result.done += 1;
        else if (cell.cellStatus === 'OVERDUE') result.overdue += 1;
        else result.pending += 1;
      });
    });
  });
  return result;
});

const selectedDevice = computed(() =>
  deviceRows.value.find((item) => item.id === selectedDeviceId.value),
);

onMounted(async () => {
  await Promise.all([loadCategoryOptions(), loadPlannedDeviceCodes()]);
  await loadDeviceList();
});

async function loadMatrixData() {
  if (viewMode.value === 'list') return;
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

async function loadPlannedDeviceCodes() {
  const res = await getAnnualPlanMatrix(currentYear.value.year());
  plannedDeviceCodes.value = new Set(
    (res.list || []).map((row) => row.deviceCode).filter(Boolean),
  );
}

async function loadCategoryOptions() {
  const rows = await getCategoryList();
  categoryOptions.value = rows
    .filter((item) => item.id && item.parentId && item.parentId > 0)
    .map((item) => ({ label: item.categoryName || '-', value: item.id! }));
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
    if (!deviceRows.value.some((item) => item.id === selectedDeviceId.value)) {
      selectedDeviceId.value = deviceRows.value[0]?.id;
    }
    await loadMatrixData();
  } finally {
    deviceLoading.value = false;
  }
}

function handleDeviceSelect(row: MesDeviceLedgerApi.Device) {
  selectedDeviceId.value = row.id;
  if (viewMode.value === 'list') {
    listGridApi.query();
    return;
  }
  loadMatrixData();
}

function handleViewChange(mode: 'actual' | 'list' | 'plan') {
  viewMode.value = mode;
  if (mode === 'list') {
    listGridApi.query();
  } else {
    loadMatrixData();
  }
}

async function handleYearChange() {
  await loadPlannedDeviceCodes();
  if (viewMode.value === 'list') {
    listGridApi.query();
    return;
  }
  loadMatrixData();
}

function getMonthCell(item: MesMaintPlanMatrixApi.MatrixItem, month: number) {
  return item.months?.[month];
}

function openWeekEditor(
  row: MesMaintPlanMatrixApi.MatrixRow,
  item: MesMaintPlanMatrixApi.MatrixItem,
  monthNo: number,
) {
  if (viewMode.value !== 'plan') return;
  const cell = getMonthCell(item, monthNo);
  cellModalApi
    .setData({
      planYear: currentYear.value.year(),
      monthNo,
      weekNo: cell?.weekNo || 1,
      deviceId: row.deviceId,
      deviceCode: row.deviceCode,
      deviceName: row.deviceName,
      standardId: row.standardId,
      standardCode: row.standardCode,
      standardName: row.standardName,
      standardItemId: item.standardItemId,
      itemGroup: item.itemGroup,
      itemName: item.itemName,
      method: item.method,
      requirement: item.requirement,
      frequency: item.frequency,
      maintType: item.maintType,
      sourceRule: row.standardName,
      remark: '年度计划矩阵维护',
    })
    .open();
}

async function handleWeekSave(payload: MesMaintPlanMatrixApi.WeekSaveReq) {
  await savePlanWeek(payload);
  message.success('计划周次已保存');
  await loadPlannedDeviceCodes();
  await loadMatrixData();
  listGridApi.query();
}

function openExecution(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (!cell) {
    return;
  }
  message.info('执行和确认请到“执行计划”菜单处理');
}

function handleAutoCalculate() {
  const targetYear = currentYear.value.year();
  Modal.confirm({
    title: `按 ${targetYear - 1} 年模板生成 ${targetYear} 年计划`,
    content:
      '将复制上一年的设备、保养标准明细和月/周安排；如当前年份已存在计划，会先覆盖后重建。',
    async onOk() {
      const count = await copyPlanYear({
        overwrite: true,
        sourceYear: targetYear - 1,
        targetYear,
      });
      message.success(`已生成 ${count} 条年度保养计划`);
      await loadPlannedDeviceCodes();
      await loadMatrixData();
      listGridApi.query();
    },
  });
}

async function handleExport() {
  const data = await exportPlanExcel({
    ...(await listGridApi.formApi.getValues()),
    planYear: currentYear.value.year(),
  });
  downloadFileFromBlobPart({ fileName: '设备年度保养计划.xls', source: data });
}

async function handleImport(file: File) {
  const res = await importPlanExcel(file, currentYear.value.year(), false);
  if (res.failureCount > 0) {
    message.warning(
      `导入完成：成功 ${res.successCount} 条，失败 ${res.failureCount} 条`,
    );
  } else {
    message.success(`导入成功：${res.successCount} 条计划`);
  }
  await loadPlannedDeviceCodes();
  await loadMatrixData();
  listGridApi.query();
  return false;
}

function getActualCellClass(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (!cell) return 'matrix-cell matrix-cell--blank';
  const status = getOrderStatus(cell);
  if (['ABNORMAL', 'DONE'].includes(status))
    return 'matrix-cell matrix-cell--done';
  if (status === 'WAIT_CONFIRM') return 'matrix-cell matrix-cell--wait-confirm';
  if (status === 'OVERDUE') return 'matrix-cell matrix-cell--overdue';
  return 'matrix-cell matrix-cell--pending';
}

function getActualText(cell?: MesMaintPlanMatrixApi.MonthCell) {
  if (!cell) return '';
  const status = getOrderStatus(cell);
  if (cell.actualDate) return cell.actualDate.slice(0, 10);
  if (status === 'WAIT_CONFIRM') return '待确认';
  if (status === 'OVERDUE') return '逾期';
  return '未执行';
}

function getPlanCellText(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return cell?.weekLabel || '';
}

function getItemGroupText(item: MesMaintPlanMatrixApi.MatrixItem) {
  return item.itemGroup || '-';
}

function getOrderStatus(cell?: MesMaintPlanMatrixApi.MonthCell) {
  return (
    cell?.executeStatus || cell?.orderStatus || cell?.cellStatus || 'PENDING'
  );
}

function hasDevicePlan(device: MesDeviceLedgerApi.Device) {
  return !!device.deviceCode && plannedDeviceCodes.value.has(device.deviceCode);
}

function getItemGroupRowSpan(
  items: MesMaintPlanMatrixApi.MatrixItem[] = [],
  index: number,
) {
  const current = items[index];
  if (!current) return 0;
  const group = getItemGroupText(current);
  if (index > 0 && getItemGroupText(items[index - 1]) === group) {
    return 0;
  }
  let count = 1;
  for (let i = index + 1; i < items.length; i += 1) {
    if (getItemGroupText(items[i]) !== group) break;
    count += 1;
  }
  return count;
}
</script>

<template>
  <Page auto-content-height>
    <CellModal @save="handleWeekSave" />

    <div class="flex h-full flex-col bg-white p-4">
      <div
        class="mb-3 flex shrink-0 items-center justify-between rounded border border-slate-200 bg-slate-50 p-3"
      >
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2">
            <IconifyIcon
              icon="lucide:calendar-days"
              class="text-xl text-sky-700"
            />
            <span class="text-base font-bold text-slate-800">
              设备年度保养计划
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
          <div class="flex rounded bg-slate-200/70 p-1">
            <button
              class="mode-button"
              :class="{ 'mode-button--active': viewMode === 'plan' }"
              @click="handleViewChange('plan')"
            >
              <IconifyIcon icon="lucide:table-2" />
              <span>年计划明细</span>
            </button>
            <button
              class="mode-button"
              :class="{ 'mode-button--active': viewMode === 'actual' }"
              @click="handleViewChange('actual')"
            >
              <IconifyIcon icon="lucide:clipboard-check" />
              <span>年计划执行台账</span>
            </button>
            <button
              class="mode-button"
              :class="{ 'mode-button--active': viewMode === 'list' }"
              @click="handleViewChange('list')"
            >
              <IconifyIcon icon="lucide:list" />
              <span>列表明细</span>
            </button>
          </div>
          <Button
            v-if="viewMode === 'actual' || viewMode === 'plan'"
            ghost
            type="primary"
            @click="handleAutoCalculate"
          >
            <IconifyIcon icon="lucide:copy-plus" class="mr-1" />
            按上一年生成
          </Button>
        </div>

        <div class="flex items-center gap-2">
          <template v-if="viewMode === 'list'">
            <Upload
              :show-upload-list="false"
              accept=".xls,.xlsx"
              :before-upload="handleImport"
            >
              <Button>
                <IconifyIcon icon="lucide:upload" class="mr-1" />
                导入
              </Button>
            </Upload>
            <Button @click="handleExport">
              <IconifyIcon icon="lucide:download" class="mr-1" />
              导出
            </Button>
          </template>
          <template v-else>
            <div class="summary-pill">
              设备 <strong>{{ stats.deviceCount }}</strong>
            </div>
            <div class="summary-pill">
              计划 <strong>{{ stats.planCount }}</strong>
            </div>
            <div class="summary-pill summary-pill--green">
              已执行 <strong>{{ stats.done }}</strong>
            </div>
            <div class="summary-pill summary-pill--red">
              逾期 <strong>{{ stats.overdue }}</strong>
            </div>
            <div class="summary-pill summary-pill--yellow">
              未执行 <strong>{{ stats.pending }}</strong>
            </div>
          </template>
        </div>
      </div>

      <div class="annual-plan-layout">
        <aside class="device-list-panel">
          <div class="device-list-panel__head">
            <div class="flex items-center gap-2 font-bold text-slate-800">
              <IconifyIcon icon="lucide:monitor-cog" />
              <span>设备列表</span>
            </div>
            <Tag color="blue" class="!m-0">{{ deviceRows.length }}</Tag>
          </div>
          <div class="device-list-panel__filters">
            <Input
              v-model:value="deviceNameFilter"
              allow-clear
              placeholder="按设备名称过滤"
              @press-enter="loadDeviceList"
            >
              <template #prefix>
                <IconifyIcon
                  icon="lucide:search"
                  class="size-4 text-slate-400"
                />
              </template>
            </Input>
            <Select
              v-model:value="deviceCategoryIdFilter"
              allow-clear
              class="w-full"
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
                v-for="device in deviceRows"
                :key="device.id"
                class="device-list-item"
                :class="{
                  'device-list-item--active': device.id === selectedDeviceId,
                  'device-list-item--planned': hasDevicePlan(device),
                }"
                @click="handleDeviceSelect(device)"
              >
                <span class="device-list-item__line">
                  <span class="font-mono text-xs font-bold text-slate-700">{{
                    device.deviceCode
                  }}</span>
                  <IconifyIcon
                    v-if="hasDevicePlan(device)"
                    icon="lucide:calendar-check"
                    class="device-list-item__plan-icon"
                    title="已生成年度保养计划"
                  />
                </span>
                <span class="truncate font-semibold text-slate-900">{{
                  device.deviceName
                }}</span>
                <span class="truncate text-xs text-slate-500">{{
                  device.categoryName || '-'
                }}</span>
              </button>
              <div
                v-if="deviceRows.length === 0 && !deviceLoading"
                class="py-6 text-center text-sm text-slate-500"
              >
                暂无设备
              </div>
            </div>
          </Spin>
        </aside>

        <div
          v-if="viewMode === 'list'"
          class="annual-plan-main annual-plan-main--list"
        >
          <ListGrid>
            <template #maintType="{ row }">
              <Tag
                v-if="row.maintType?.includes('月')"
                color="green"
                class="!m-0 border-none"
              >
                {{ row.maintType }}
              </Tag>
              <Tag
                v-else-if="row.maintType?.includes('季')"
                color="orange"
                class="!m-0 border-none"
              >
                {{ row.maintType }}
              </Tag>
              <Tag
                v-else-if="row.maintType?.includes('年')"
                color="red"
                class="!m-0 border-none"
              >
                {{ row.maintType }}
              </Tag>
              <Tag v-else color="blue" class="!m-0 border-none">
                {{ row.maintType }}
              </Tag>
            </template>

            <template #published="{ row }">
              <Tag
                v-if="row.published"
                color="success"
                class="!m-0 border-none font-bold"
              >
                已生成
              </Tag>
              <Tag v-else color="default" class="!m-0 border-none">未生成</Tag>
            </template>

            <template #actions="{ row }">
              <TableAction
                :actions="[
                  {
                    label: row.published ? '已生成' : '待生成',
                    type: 'link',
                    icon: row.published ? ACTION_ICON.VIEW : ACTION_ICON.EDIT,
                    disabled: true,
                  },
                ]"
              />
            </template>
          </ListGrid>
        </div>

        <div
          v-else
          class="annual-plan-main"
          :class="{ 'opacity-60': matrixLoading }"
        >
          <div
            v-if="!selectedDeviceId"
            class="flex h-full items-center justify-center text-sm text-slate-500"
          >
            请先在左侧选择设备
          </div>
          <div
            v-else-if="matrixRows.length === 0"
            class="flex h-full items-center justify-center text-sm text-slate-500"
          >
            当前设备在 {{ currentYear.year() }} 年暂无年度保养计划
          </div>
          <div v-else class="plan-sections p-3">
            <section
              v-for="row in matrixRows"
              :key="row.id"
              class="plan-section"
            >
              <div class="plan-section__head">
                <div>
                  <span class="head-label">设备编号：</span>
                  <span class="font-mono font-bold text-slate-900">{{
                    row.deviceCode
                  }}</span>
                </div>
                <div>
                  <span class="head-label">设备名称：</span>
                  <span class="font-bold text-slate-900">{{
                    row.deviceName
                  }}</span>
                </div>
                <div>
                  <span class="head-label">设备分类：</span>
                  <span class="text-slate-800">{{
                    selectedDevice?.categoryName || row.categoryName || '-'
                  }}</span>
                </div>
                <div>
                  <span class="head-label">保养标准：</span>
                  <span class="text-slate-800">{{
                    row.standardName || '-'
                  }}</span>
                </div>
                <div>
                  <span class="head-label">年份：</span>
                  <span class="font-semibold text-slate-900">{{
                    currentYear.year()
                  }}</span>
                </div>
              </div>

              <div class="matrix-scroll">
                <table class="annual-table">
                  <thead>
                    <tr>
                      <th class="col-seq">序号</th>
                      <th class="col-item-group">保养项目</th>
                      <th class="col-item">保养部位</th>
                      <th class="col-method">保养方法</th>
                      <th class="col-standard">保养标准</th>
                      <th class="col-frequency">保养周期</th>
                      <th
                        v-for="month in months"
                        :key="month"
                        class="col-month"
                      >
                        {{ month }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, index) in row.items" :key="item.id">
                      <td class="text-center font-mono text-xs text-slate-500">
                        {{ index + 1 }}
                      </td>
                      <td
                        v-if="getItemGroupRowSpan(row.items, index) > 0"
                        class="item-group-cell"
                        :rowspan="getItemGroupRowSpan(row.items, index)"
                      >
                        {{ getItemGroupText(item) }}
                      </td>
                      <td class="font-semibold text-slate-800">
                        {{ item.itemName }}
                      </td>
                      <td>{{ item.method || '-' }}</td>
                      <td>{{ item.requirement || '-' }}</td>
                      <td class="text-center">
                        <Tag color="blue" class="!m-0">
                          {{ item.frequency || '-' }}
                        </Tag>
                      </td>
                      <td
                        v-for="month in months"
                        :key="month"
                        class="p-1 text-center"
                      >
                        <button
                          v-if="viewMode === 'plan'"
                          class="matrix-cell matrix-cell--plan"
                          :class="{
                            'matrix-cell--empty': !getMonthCell(item, month),
                          }"
                          @click="openWeekEditor(row, item, month)"
                        >
                          {{ getPlanCellText(getMonthCell(item, month)) }}
                        </button>
                        <button
                          v-else-if="getMonthCell(item, month)"
                          :class="getActualCellClass(getMonthCell(item, month))"
                          @click="openExecution(getMonthCell(item, month))"
                        >
                          {{ getActualText(getMonthCell(item, month)) }}
                        </button>
                        <div
                          v-else
                          class="matrix-cell matrix-cell--blank"
                        ></div>
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
  </Page>
</template>

<style scoped>
.mode-button {
  align-items: center;
  border-radius: 4px;
  color: #475569;
  display: inline-flex;
  font-size: 13px;
  font-weight: 700;
  gap: 4px;
  height: 30px;
  padding: 0 12px;
}

.mode-button--active {
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

.summary-pill--red strong {
  color: #b91c1c;
}

.summary-pill--yellow strong {
  color: #a16207;
}

.annual-plan-layout {
  display: grid;
  flex: 1;
  gap: 12px;
  grid-template-columns: 280px minmax(0, 1fr);
  height: 100%;
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

.annual-plan-main {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.plan-sections {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: auto;
}

.annual-plan-main--list {
  padding: 0;
}

.monthly-ledger {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
}

.monthly-ledger__meta {
  border: 2px solid #1d4ed8;
  border-bottom: 0;
  display: grid;
  gap: 10px 20px;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  min-height: 46px;
  padding: 10px 12px;
}

.plan-section {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.plan-section__head {
  background: #f8fafc;
  border-bottom: 1px solid #cbd5e1;
  display: grid;
  gap: 8px 20px;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  padding: 10px 12px;
}

.head-label {
  color: #64748b;
  font-size: 13px;
}

.matrix-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.matrix-scroll--fill {
  flex: 1;
  min-height: 0;
}

.annual-table {
  border-collapse: collapse;
  height: 100%;
  min-width: 1680px;
  table-layout: fixed;
  width: 100%;
}

.annual-table th {
  background: #9bbadd;
  border: 1px solid #334155;
  color: #0f172a;
  font-weight: 700;
  height: 38px;
  text-align: center;
}

.annual-table td {
  border: 1px solid #94a3b8;
  color: #1e293b;
  font-size: 13px;
  height: 42px;
  line-height: 1.35;
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

.col-month {
  width: 82px;
}

.col-week {
  width: 128px;
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

.item-group-cell {
  background: #f8fafc;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
}

.matrix-cell {
  align-items: center;
  border: 1px solid transparent;
  border-radius: 4px;
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  height: 28px;
  justify-content: center;
  min-width: 68px;
  padding: 0 4px;
  white-space: nowrap;
}

.matrix-cell--plan {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  cursor: pointer;
}

.matrix-cell--plan:hover {
  background: #dbeafe;
}

.matrix-cell--empty {
  background: #fff;
  border-color: #e2e8f0;
  color: transparent;
}

.matrix-cell--done {
  background: #dcfce7;
  border-color: #86efac;
  color: #166534;
}

.matrix-cell--wait-confirm {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #075985;
}

.matrix-cell--overdue {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.matrix-cell--pending {
  background: #fef3c7;
  border-color: #fcd34d;
  color: #92400e;
}

.matrix-cell--blank {
  background: #fff;
  color: transparent;
}

.monthly-exec-table {
  border-collapse: collapse;
  min-width: 1380px;
  table-layout: fixed;
  width: 100%;
}

.monthly-exec-table th {
  background: #fff;
  border: 1px solid #111827;
  color: #0f172a;
  font-weight: 700;
  height: 42px;
  text-align: center;
}

.monthly-exec-table th.sticky-seq {
  background: #fff;
}

.monthly-exec-table td {
  border: 1px solid #111827;
  color: #1e293b;
  font-size: 13px;
  line-height: 1.4;
  min-height: 56px;
  padding: 6px 8px;
  vertical-align: middle;
  word-break: break-word;
}

.monthly-exec-table .col-result {
  width: 270px;
}

.monthly-exec-table .col-person {
  width: 90px;
}

.monthly-exec-table .col-date {
  width: 112px;
}

.monthly-result-cell {
  min-width: 260px;
}

.monthly-result-cell__head {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  margin-bottom: 6px;
}

.monthly-result-textarea {
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  color: #1e293b;
  display: block;
  font-size: 13px;
  height: 48px;
  line-height: 1.35;
  margin-bottom: 6px;
  padding: 5px 7px;
  resize: vertical;
  width: 100%;
}

.monthly-result-textarea:disabled {
  background: #f8fafc;
  color: #475569;
}

.monthly-person {
  display: grid;
  gap: 2px;
}

.monthly-person small {
  color: #64748b;
  font-size: 11px;
}

:deep(.vben-vxe-grid) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>
