<script lang="ts" setup>
import type {
  LedgerOption,
  MeasureToolAreaOption,
  MeasureToolLocationFilterOption,
} from './options';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { nextTick, onMounted, ref } from 'vue';

import { useAccess } from '@vben/access';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Button, message, Tooltip } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteMeasureToolLedger,
  exportMeasureToolLedger,
  getMeasureToolCategoryList,
  getMeasureToolLedgerPage,
  importMeasureToolLedger,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import {
  optionLabel,
  TOOL_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
} from '../shared';
import { useGridColumns, useGridFormSchema } from './data';
import AdjustStatusComponent from './modules/adjust-status.vue';
import FormComponent from './modules/form.vue';
import DetailComponent from './modules/history.vue';
import MaintainCalibrationComponent from './modules/maintain-calibration.vue';
import MaintainMsaComponent from './modules/maintain-msa.vue';
import {
  loadUsingDepartmentOptions,
  mapAreaOptions,
  mapLocationFilterOptions,
} from './options';

defineOptions({ name: 'MesQmsMeasureToolLedger' });

type GridFilter = { field?: string; values?: unknown[] };
type GridSort = { field?: string; order?: null | string };

const SORTABLE_FIELDS = new Set([
  'bodyNo',
  'calibrationCycleMonths',
  'calibrationResult',
  'categoryName',
  'externalOpen',
  'lastCalibrationDate',
  'lastMsaDate',
  'maintainerName',
  'manufacturer',
  'model',
  'nextCalibrationDate',
  'nextMsaDate',
  'purchaseDate',
  'responsiblePerson',
  'status',
  'storageLocation',
  'toolCode',
  'toolName',
]);

const locationFilterOptions = ref<MeasureToolLocationFilterOption[]>([]);
const areaOptions = ref<MeasureToolAreaOption[]>([]);
const usingDepartmentOptions = ref<LedgerOption[]>([]);
const checkedIds = ref<number[]>([]);
const advancedSearchVisible = ref(false);
const importInputRef = ref<HTMLInputElement>();
const { hasAccessByCodes } = useAccess();
const canManageExternalOpen = hasAccessByCodes([
  'mes:qms-measure-tool-ledger:internal-admin',
]);

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true,
});
const [AdjustStatusModal, adjustStatusModalApi] = useVbenModal({
  connectedComponent: AdjustStatusComponent,
  destroyOnClose: true,
  zIndex: 1040,
});
const [MaintainCalibrationModal, maintainCalibrationModalApi] = useVbenModal({
  connectedComponent: MaintainCalibrationComponent,
  destroyOnClose: true,
  zIndex: 1040,
});
const [MaintainMsaModal, maintainMsaModalApi] = useVbenModal({
  connectedComponent: MaintainMsaComponent,
  destroyOnClose: true,
  zIndex: 1040,
});
const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailComponent,
  destroyOnClose: true,
  zIndex: 1000,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ type: 'create' }).open();
}

function handleEdit(row: QmsMeasureToolApi.Ledger) {
  formModalApi.setData({ id: row.id, type: 'edit' }).open();
}

function handleAdjustStatus(row: QmsMeasureToolApi.Ledger) {
  adjustStatusModalApi.setData(row).open();
}

function handleCalibrate(row: QmsMeasureToolApi.Ledger) {
  maintainCalibrationModalApi.setData(row).open();
}

function handleMaintainMsa(row: QmsMeasureToolApi.Ledger) {
  maintainMsaModalApi.setData(row).open();
}

function handleView(row: QmsMeasureToolApi.Ledger) {
  detailModalApi.setData(row).open();
}

function handleDetailCalibrate(row: QmsMeasureToolApi.Ledger) {
  handleCalibrate(row);
}

function handleDetailMaintainMsa(row: QmsMeasureToolApi.Ledger) {
  handleMaintainMsa(row);
}

function handleDetailAdjustStatus(row: QmsMeasureToolApi.Ledger) {
  handleAdjustStatus(row);
}

async function handleDelete(row: QmsMeasureToolApi.Ledger) {
  await confirm(`确认删除量检具 [${row.toolName}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteMeasureToolLedger(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deletingBatch'),
    duration: 0,
  });
  try {
    await Promise.all(
      checkedIds.value.map((id) => deleteMeasureToolLedger(id)),
    );
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportMeasureToolLedger(
    buildLedgerQueryValues(await gridApi.formApi.getValues()),
  );
  downloadFileFromBlobPart({ fileName: '量检具台账.xls', source: data });
}

function handleImportClick() {
  importInputRef.value?.click();
}

async function handleImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const hideLoading = message.loading({
    content: '正在导入量检具台账...',
    duration: 0,
  });
  try {
    const count = await importMeasureToolLedger(file);
    message.success(`导入完成：${count} 条量检具台账`);
    const [categoryRows, departmentOptions] = await Promise.all([
      getMeasureToolCategoryList(),
      loadUsingDepartmentOptions(),
    ]);
    locationFilterOptions.value = mapLocationFilterOptions(categoryRows);
    areaOptions.value = mapAreaOptions(categoryRows);
    usingDepartmentOptions.value = departmentOptions;
    await gridApi.formApi.updateSchema(
      useGridFormSchema(
        locationFilterOptions.value,
        areaOptions.value,
        advancedSearchVisible.value,
        usingDepartmentOptions.value,
      ),
    );
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({
  records,
}: {
  records: QmsMeasureToolApi.Ledger[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

function buildLedgerQueryValues(formValues: Record<string, unknown>) {
  const { msaScope, reminderScope, ...queryValues } = formValues || {};
  const scopes = Array.isArray(reminderScope) ? reminderScope : [];
  const msaScopes = Array.isArray(msaScope) ? msaScope : [];
  return {
    ...queryValues,
    msaEnabled: msaScopes.includes('MSA_ENABLED') ? 1 : undefined,
    onlyCurrentMonthReminder: scopes.includes('CURRENT_MONTH') || undefined,
    onlyOverdue: scopes.includes('OVERDUE') || undefined,
  };
}

function selectedHeaderValues(filters: GridFilter[], field: string) {
  return (filters.find((item) => item.field === field)?.values ?? [])
    .map(String)
    .filter(Boolean);
}

function buildHeaderFilterValues(filters: GridFilter[] = []) {
  const externalOpenValues = selectedHeaderValues(filters, 'externalOpen').map(
    Number,
  );
  return {
    calibrationOverdue:
      selectedHeaderValues(filters, 'calibrationOverdue').includes('OVERDUE') ||
      undefined,
    calibrationStatusValues: selectedHeaderValues(filters, 'calibrationStatus'),
    msaEnabledValues: selectedHeaderValues(filters, 'msaEnabled').map(Number),
    msaStatusValues: selectedHeaderValues(filters, 'msaStatus'),
    statusValues: selectedHeaderValues(filters, 'status'),
    externalOpen:
      externalOpenValues.length === 1 ? externalOpenValues[0] : undefined,
  };
}

function buildSortValues(sorts: GridSort[] = []) {
  const sort = sorts.find(
    (item) =>
      item.field &&
      SORTABLE_FIELDS.has(item.field) &&
      ['asc', 'desc'].includes(String(item.order)),
  );
  return sort ? { sortField: sort.field, sortOrder: sort.order } : {};
}

function isOverdue(row: QmsMeasureToolApi.Ledger) {
  return row.calibrationStatus === 'OVERDUE' || row.msaStatus === 'OVERDUE';
}

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(
      locationFilterOptions.value,
      areaOptions.value,
      advancedSearchVisible.value,
      usingDepartmentOptions.value,
    ),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'measure-tool-ledger-vben-grid',
  gridClass: 'measure-tool-ledger-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columnConfig: {
      drag: true,
      isHover: true,
      resizable: true,
    },
    columnDragConfig: {
      animation: true,
      disabledMethod: ({ column }) =>
        Boolean(column.fixed) || column.type === 'checkbox',
      showDragTip: true,
      showGuidesStatus: true,
      showIcon: true,
      trigger: 'default',
    },
    columns: useGridColumns(canManageExternalOpen),
    customConfig: {
      allowResizable: true,
      allowSort: true,
      enabled: true,
      storage: true,
    },
    filterConfig: {
      multiple: true,
      remote: true,
      showIcon: true,
    },
    height: '100%',
    id: 'mes-qms-measure-tool-ledger-excel-grid',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ filters, page, sorts }, formValues) =>
          await getMeasureToolLedgerPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...buildLedgerQueryValues(formValues),
            ...buildHeaderFilterValues(filters),
            ...buildSortValues(sorts),
          }),
      },
      filter: true,
      sort: true,
    },
    resizableConfig: {
      isAllColumnDrag: true,
      isDblclickAutoWidth: true,
      showDragTip: true,
    },
    rowClassName: ({ row }: { row: QmsMeasureToolApi.Ledger }) =>
      isOverdue(row) ? 'measure-tool-ledger-row--overdue' : '',
    rowConfig: { isHover: true, keyField: 'id' },
    sortConfig: {
      allowClear: true,
      multiple: false,
      remote: true,
      showIcon: true,
      trigger: 'cell',
    },
    toolbarConfig: { custom: true, refresh: true, search: true },
  } as VxeTableGridOptions<QmsMeasureToolApi.Ledger>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
    cellDblclick: ({ row }) => handleView(row),
  },
});

onMounted(async () => {
  const [categoryRows, departmentOptions] = await Promise.all([
    getMeasureToolCategoryList(),
    loadUsingDepartmentOptions(),
  ]);
  locationFilterOptions.value = mapLocationFilterOptions(categoryRows);
  areaOptions.value = mapAreaOptions(categoryRows);
  usingDepartmentOptions.value = departmentOptions;
  await gridApi.formApi.updateSchema(
    useGridFormSchema(
      locationFilterOptions.value,
      areaOptions.value,
      advancedSearchVisible.value,
      usingDepartmentOptions.value,
    ),
  );
});
</script>

<template>
  <Page auto-content-height>
    <input
      ref="importInputRef"
      accept=".xlsx,.xls"
      class="hidden"
      type="file"
      @change="handleImportChange"
    />
    <FormModal @success="handleRefresh" />
    <AdjustStatusModal @success="handleRefresh" />
    <MaintainCalibrationModal @success="handleRefresh" />
    <MaintainMsaModal @success="handleRefresh" />
    <DetailModal
      @adjust-status="handleDetailAdjustStatus"
      @calibrate="handleDetailCalibrate"
      @maintain-msa="handleDetailMaintainMsa"
    />
    <div class="measure-tool-ledger-page">
      <Grid table-title="量检具台账列表">
        <template #toolbar-tools>
          <TableAction
            :actions="[
              {
                label: $t('ui.actionTitle.create', ['量检具台账']),
                type: 'primary',
                icon: ACTION_ICON.ADD,
                auth: ['mes:qms-measure-tool-ledger:create'],
                onClick: handleCreate,
              },
              {
                label: $t('ui.actionTitle.export'),
                type: 'primary',
                icon: ACTION_ICON.DOWNLOAD,
                auth: ['mes:qms-measure-tool-ledger:export'],
                onClick: handleExport,
              },
              {
                label: '导入Excel',
                type: 'primary',
                icon: ACTION_ICON.UPLOAD,
                onClick: handleImportClick,
              },
              {
                label: $t('ui.actionTitle.deleteBatch'),
                type: 'primary',
                danger: true,
                icon: ACTION_ICON.DELETE,
                auth: ['mes:qms-measure-tool-ledger:delete'],
                disabled: isEmpty(checkedIds),
                onClick: handleDeleteBatch,
              },
            ]"
          />
        </template>

        <template #expand-after>
          <a
            class="measure-tool-ledger-page__expand-link"
            @click="toggleAdvancedSearch"
          >
            {{ advancedSearchVisible ? '收起' : '展开' }}
            <IconifyIcon
              :icon="
                advancedSearchVisible
                  ? 'lucide:chevron-up'
                  : 'lucide:chevron-down'
              "
              class="measure-tool-ledger-page__expand-icon"
            />
          </a>
        </template>

        <template #status="{ row }">
          <span
            class="measure-tool-ledger-state"
            :class="`is-${String(row.displayStatus || row.status).toLowerCase()}`"
          >
            <IconifyIcon
              v-if="isOverdue(row)"
              class="measure-tool-ledger-state__overdue-icon"
              icon="lucide:triangle-alert"
            />
            {{ optionLabel(TOOL_STATUS_OPTIONS, row.status) }}
          </span>
        </template>

        <template #calibrationOverdue="{ row }">
          <span
            class="measure-tool-ledger-overdue"
            :class="row.calibrationOverdue ? 'is-overdue' : 'is-normal'"
          >
            {{ row.calibrationOverdue ? '已逾期' : '正常' }}
          </span>
        </template>

        <template #calibrationStatus="{ row }">
          <div
            class="measure-tool-ledger-reminder"
            :class="`is-${String(row.calibrationStatus).toLowerCase()}`"
          >
            <span>{{
              optionLabel(WARNING_STATUS_OPTIONS, row.calibrationStatus)
            }}</span>
            <small v-if="Number(row.calibrationMissedCount) > 0">
              累计漏检 {{ row.calibrationMissedCount }} 次，最近
              {{ row.recentCalibrationMissedDate }}
            </small>
          </div>
        </template>

        <template #msaStatus="{ row }">
          <div
            class="measure-tool-ledger-reminder"
            :class="`is-${String(row.msaStatus).toLowerCase()}`"
          >
            <span>{{
              optionLabel(WARNING_STATUS_OPTIONS, row.msaStatus)
            }}</span>
            <small v-if="Number(row.msaMissedCount) > 0">
              累计漏检 {{ row.msaMissedCount }} 次，最近
              {{ row.recentMsaMissedDate }}
            </small>
          </div>
        </template>

        <template #actions="{ row }">
          <TableAction
            :drop-down-actions="[
              {
                label: '登记校准',
                type: 'link',
                icon: ACTION_ICON.ADD,
                auth: ['mes:qms-measure-tool-ledger:calibrate'],
                onClick: () => handleCalibrate(row),
              },
              {
                label: '维护MSA',
                type: 'link',
                icon: ACTION_ICON.EDIT,
                auth: ['mes:qms-measure-tool-ledger:calibrate'],
                disabled: row.msaEnabled !== 1,
                onClick: () => handleMaintainMsa(row),
              },
              {
                label: '查看详情',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                onClick: () => handleView(row),
              },
              {
                label: $t('common.edit'),
                type: 'link',
                icon: ACTION_ICON.EDIT,
                auth: ['mes:qms-measure-tool-ledger:update'],
                onClick: () => handleEdit(row),
              },
              {
                label: '调整状态',
                type: 'link',
                icon: 'lucide:rotate-cw',
                auth: ['mes:qms-measure-tool-ledger:update'],
                onClick: () => handleAdjustStatus(row),
              },
              {
                label: $t('common.delete'),
                type: 'link',
                danger: true,
                icon: ACTION_ICON.DELETE,
                auth: ['mes:qms-measure-tool-ledger:delete'],
                onClick: () => handleDelete(row),
              },
            ]"
          >
            <template #more>
              <Tooltip title="操作">
                <Button aria-label="操作" size="small" type="link">
                  <template #icon>
                    <IconifyIcon icon="lucide:ellipsis" />
                  </template>
                </Button>
              </Tooltip>
            </template>
          </TableAction>
        </template>
      </Grid>
    </div>
  </Page>
</template>

<style scoped>
.measure-tool-ledger-page {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.measure-tool-ledger-page :deep(.measure-tool-ledger-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-ledger-page :deep(.measure-tool-ledger-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-ledger-page :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.measure-tool-ledger-page :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.measure-tool-ledger-page :deep(.vxe-grid--top-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.measure-tool-ledger-page :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.measure-tool-ledger-page :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 5;
  min-height: 0;
}

.measure-tool-ledger-page :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  min-height: 0;
  background: #fff;
}

.measure-tool-ledger-page :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.measure-tool-ledger-page :deep(.vxe-pager) {
  min-height: 36px;
}

.measure-tool-ledger-page :deep(.vxe-header--column) {
  background: #f5f8fc;
}

.measure-tool-ledger-page :deep(.vxe-header--column .vxe-cell) {
  display: flex;
  align-items: center;
  min-width: 0;
  white-space: nowrap;
}

.measure-tool-ledger-page :deep(.vxe-header--column .vxe-cell--title) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.measure-tool-ledger-page :deep(.vxe-header--column .vxe-cell--filter),
.measure-tool-ledger-page :deep(.vxe-header--column .vxe-cell--sort) {
  flex: 0 0 auto;
}

.measure-tool-ledger-page :deep(.vxe-header--column.is--sortable) {
  cursor: pointer;
}

.measure-tool-ledger-page
  :deep(.vxe-body--row.measure-tool-ledger-row--overdue > .vxe-body--column) {
  background-color: #fff7e6;
}

.measure-tool-ledger-page__expand-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
}

.measure-tool-ledger-page__expand-link:hover {
  text-decoration: underline;
}

.measure-tool-ledger-page__expand-icon {
  font-size: 14px;
}

.measure-tool-ledger-state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 52px;
  padding: 2px 8px;
  color: #303133;
  background: transparent;
}

.measure-tool-ledger-state__overdue-icon {
  color: #d46b08;
}

.measure-tool-ledger-state.is-calibrating {
  color: #1d5fbf;
  background: #d9ecff;
}

.measure-tool-ledger-state.is-idle,
.measure-tool-ledger-state.is-repairing,
.measure-tool-ledger-state.is-stopped,
.measure-tool-ledger-state.is-scrapped {
  color: #6b4e00;
  background: #fff3cd;
}

.measure-tool-ledger-overdue {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  padding: 2px 6px;
}

.measure-tool-ledger-overdue.is-overdue {
  color: #cf1322;
  background: #fff1f0;
}

.measure-tool-ledger-overdue.is-normal {
  color: #389e0d;
}

.measure-tool-ledger-reminder {
  display: grid;
  gap: 2px;
  justify-items: center;
  padding: 3px 4px;
  color: #303133;
}

.measure-tool-ledger-reminder small {
  font-size: 12px;
  line-height: 16px;
}

.measure-tool-ledger-reminder.is-due_soon {
  color: #1f1f1f;
  background: #fff3cd;
}

.measure-tool-ledger-reminder.is-overdue {
  color: #1f1f1f;
  background: #f8d7da;
}
</style>
