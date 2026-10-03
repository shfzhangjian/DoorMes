<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, DatePicker, Input, Modal, Select, Switch, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getNgHistoryLedgerPage } from '#/api/mes/hc/ng-inventory';

import { padTypeNameOf } from '../../base/pad-type-options';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesNgHistoryLedger' });

type HistoryLedger = MesHcNgInventoryApi.NgHistoryLedger;
type HistoryQuery = {
  archivedOnly: boolean;
  currentStatus: string;
  keyword: string;
  materialCode: string;
  modelNo: string;
  padType: string;
  processType: string;
  txnTimeEnd: string;
  txnTimeStart: string;
  txnType: string;
  warehouseCode: string;
};

const rows = ref<HistoryLedger[]>([]);
const total = ref(0);
const advancedQueryVisible = ref(false);
const query = reactive<HistoryQuery>({
  archivedOnly: false,
  currentStatus: '',
  keyword: '',
  materialCode: '',
  modelNo: '',
  padType: '',
  processType: '',
  txnTimeEnd: '',
  txnTimeStart: '',
  txnType: '',
  warehouseCode: '',
});

const processTypeOptions = [
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
];
const padTypeOptions = [
  { label: '黑垫', value: 'BLACK_PAD' },
  { label: '白垫', value: 'WHITE_PAD' },
];
const txnTypeOptions = [
  { label: '不合格品入库', value: 'NG_SHELF' },
  { label: '冻结入库', value: 'FREEZE_SHELF' },
  { label: '不合格品下架', value: 'NG_UNSHELF' },
  { label: '历史冻结下架', value: 'HISTORY_FREEZE_UNSHELF' },
  { label: '移库出库', value: 'NG_TRANSFER_OUT' },
  { label: '移库入库', value: 'NG_TRANSFER_IN' },
  { label: '报废出库', value: 'NG_SCRAP' },
  { label: '返工下架', value: 'NG_REWORK_PICK' },
  { label: '返工出库', value: 'NG_REWORK_OUT' },
  { label: '历史冻结解除并关闭', value: 'HISTORY_UNFREEZE_CLOSE' },
  { label: '解冻退回', value: 'PLAN_UNFREEZE' },
];
const currentStatusOptions = [
  { label: '正常在库', value: 'STORED' },
  { label: '冻结在库', value: 'FROZEN' },
  { label: '待上架', value: 'WAIT_SHELF' },
  { label: '待上架冻结品', value: 'WAIT_FREEZE_SHELF' },
  { label: '返工加工中', value: 'REWORKING' },
  { label: '已报废', value: 'SCRAPPED' },
  { label: '已退回', value: 'RETURNED' },
  { label: '已解冻关闭', value: 'UNFROZEN_CLOSED' },
];

const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const pageInCount = computed(
  () => rows.value.filter((item) => item.txnDirection === 'IN').length,
);
const pageOutCount = computed(
  () => rows.value.filter((item) => item.txnDirection === 'OUT').length,
);
const pageScrapCount = computed(
  () => rows.value.filter((item) => item.txnType === 'NG_SCRAP').length,
);

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, boolean | number | string> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  (Object.keys(query) as Array<keyof HistoryQuery>).forEach((key) => {
    const value = query[key];
    if (typeof value === 'boolean') {
      if (value) params[key] = true;
      return;
    }
    const normalizedValue = value.trim();
    if (normalizedValue) params[key] = normalizedValue;
  });
  return params;
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  Object.assign(query, {
    archivedOnly: false,
    currentStatus: '',
    keyword: '',
    materialCode: '',
    modelNo: '',
    padType: '',
    processType: '',
    txnTimeEnd: '',
    txnTimeStart: '',
    txnType: '',
    warehouseCode: '',
  });
  advancedQueryVisible.value = false;
  handleSearch();
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  handleSearch();
}

function operationColor(direction?: string) {
  return direction === 'OUT' ? 'volcano' : 'blue';
}

function currentStatusColor(status?: string) {
  if (status === 'SCRAPPED') return 'red';
  if (status === 'FROZEN' || status === 'WAIT_FREEZE_SHELF') return 'purple';
  if (status === 'STORED') return 'blue';
  if (status === 'RETURNED') return 'cyan';
  if (status === 'UNFROZEN_CLOSED') return 'default';
  return 'gold';
}

function currentStatusText(status?: string) {
  return (
    currentStatusOptions.find((item) => item.value === status)?.label || status || '-'
  );
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

const columns = [
  {
    field: 'txnTime',
    formatter: ({ row }: { row: HistoryLedger }) => formatDateTime(row.txnTime),
    title: '操作时间',
    width: 170,
  },
  { field: 'operationName', slots: { default: 'operation' }, title: '操作类型', width: 150 },
  { field: 'pieceNo', showOverflow: 'tooltip', title: '片号', width: 180 },
  { field: 'sourceParentBatchNo', showOverflow: 'tooltip', title: '母批 / 段批次', width: 180 },
  { field: 'sourceBatchNo', showOverflow: 'tooltip', title: '来源批号', width: 180 },
  { field: 'processName', title: '工序', width: 90 },
  {
    field: 'padType',
    formatter: ({ row }: { row: HistoryLedger }) => padTypeNameOf(row.padType),
    title: '垫型',
    width: 90,
  },
  { field: 'materialCode', showOverflow: 'tooltip', title: '料号', width: 140 },
  { field: 'modelNo', showOverflow: 'tooltip', title: '型号', width: 140 },
  { field: 'txnQty', slots: { default: 'txnQty' }, title: '操作数量', width: 105 },
  { field: 'warehouseName', showOverflow: 'tooltip', title: '操作仓库', width: 150 },
  { field: 'locationCode', showOverflow: 'tooltip', title: '操作时货架 / 库位', width: 180 },
  { field: 'currentStatus', slots: { default: 'currentStatus' }, title: '当前状态', width: 125 },
  { field: 'creatorName', title: '操作人', width: 110 },
  { field: 'remark', minWidth: 240, showOverflow: 'tooltip', title: '操作说明 / 原因' },
  { field: 'txnNo', showOverflow: 'tooltip', title: '流水单号', width: 230 },
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getNgHistoryLedgerPage(buildQueryParams(page));
          rows.value = result.list || [];
          total.value = Number(result.total || 0);
          return result;
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<HistoryLedger>,
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console ng-history-ledger-board">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:history" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">分切压槽不合格品历史台账</h2>
            <Tag class="console-title-tag" color="volcano">操作流水留存</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">流水总数</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">条</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页入库</span>
              <span class="console-meta-value">{{ pageInCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页出库</span>
              <span class="console-meta-value">{{ pageOutCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页报废</span>
              <span class="console-meta-value">{{ pageScrapCount }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar history-filter-panel">
        <div class="history-simple-query">
          <label class="history-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="片号 / 来源批号 / 母批 / 来源计划"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="advancedQueryVisible = true">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <div class="history-archive-switch">
            <Switch v-model:checked="query.archivedOnly" @change="handleSearch" />
            <span>仅看当前已离库 / 已关闭</span>
          </div>
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="历史台账多条件查询"
        width="920px"
        wrap-class-name="stock-advanced-query-modal"
      >
        <div class="history-advanced-query-body">
          <div class="history-advanced-query-grid">
            <div class="history-query-item">
              <label>操作类型</label>
              <Select
                v-model:value="query.txnType"
                :options="txnTypeOptions"
                allow-clear
                placeholder="全部操作"
              />
            </div>
            <div class="history-query-item">
              <label>当前状态</label>
              <Select
                v-model:value="query.currentStatus"
                :options="currentStatusOptions"
                allow-clear
                placeholder="全部状态"
              />
            </div>
            <div class="history-query-item">
              <label>工序</label>
              <Select
                v-model:value="query.processType"
                :options="processTypeOptions"
                allow-clear
                placeholder="全部工序"
              />
            </div>
            <div class="history-query-item">
              <label>垫型</label>
              <Select
                v-model:value="query.padType"
                :options="padTypeOptions"
                allow-clear
                placeholder="全部垫型"
              />
            </div>
            <div class="history-query-item">
              <label>操作仓库编码</label>
              <Input v-model:value="query.warehouseCode" allow-clear placeholder="精确匹配" />
            </div>
            <div class="history-query-item">
              <label>型号</label>
              <Input v-model:value="query.modelNo" allow-clear placeholder="支持模糊匹配" />
            </div>
            <div class="history-query-item">
              <label>料号</label>
              <Input v-model:value="query.materialCode" allow-clear placeholder="支持模糊匹配" />
            </div>
            <div class="history-query-item">
              <label>操作时间起</label>
              <DatePicker
                v-model:value="query.txnTimeStart"
                show-time
                placeholder="请选择"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
            <div class="history-query-item">
              <label>操作时间止</label>
              <DatePicker
                v-model:value="query.txnTimeEnd"
                show-time
                placeholder="请选择"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </div>
          </div>
          <div class="history-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="handleReset">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel history-ledger-grid-panel">
        <div class="history-hint">
          “操作时货架 / 库位”是该笔流水发生时的位置；“当前状态”是该片当前最终状态，不表示历史时点状态。
        </div>
        <Grid table-title="不合格品逐片历史操作台账">
          <template #operation="{ row }">
            <Tag :color="operationColor(row.txnDirection)">
              {{ row.operationName || row.txnType || '-' }}
            </Tag>
          </template>
          <template #txnQty="{ row }">
            <span :class="row.txnDirection === 'OUT' ? 'history-out-qty' : 'history-in-qty'">
              {{ row.txnQty == null ? '-' : Number(row.txnQty).toFixed(2) }}
            </span>
          </template>
          <template #currentStatus="{ row }">
            <Tag :color="currentStatusColor(row.currentStatus)">
              {{ currentStatusText(row.currentStatus) }}
            </Tag>
          </template>
        </Grid>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.ng-history-ledger-board {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
}

.history-filter-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.history-simple-query {
  display: grid;
  grid-template-columns: max-content minmax(300px, 1fr) max-content max-content max-content;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.history-query-label {
  color: #243142;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.history-archive-switch {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  color: #45566b;
  font-size: 13px;
  white-space: nowrap;
}

.history-filter-panel :deep(.ant-input),
.history-filter-panel :deep(.ant-input-affix-wrapper),
.history-query-item :deep(.ant-input),
.history-query-item :deep(.ant-input-affix-wrapper),
.history-query-item :deep(.ant-picker),
.history-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
  border-radius: 6px;
}

.history-advanced-query-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.history-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 16px;
}

.history-query-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.history-query-item > label {
  color: #46566a;
  font-size: 13px;
  font-weight: 600;
}

.history-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.history-ledger-grid-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.history-hint {
  flex: 0 0 auto;
  padding: 7px 10px;
  color: #58677a;
  font-size: 12px;
  line-height: 18px;
  background: #f5f8fb;
  border: 1px solid #d8e1eb;
  border-bottom: 0;
}

.history-ledger-grid-panel :deep(.vben-vxe-grid),
.history-ledger-grid-panel :deep(.vxe-grid) {
  flex: 1 1 auto;
  height: 0;
  min-height: 0;
}

.history-ledger-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.history-ledger-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.history-ledger-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.history-ledger-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.history-out-qty {
  color: #cf1322;
  font-variant-numeric: tabular-nums;
}

.history-in-qty {
  color: #096dd9;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1180px) {
  .history-simple-query {
    grid-template-columns: max-content minmax(220px, 1fr) max-content max-content;
  }

  .history-archive-switch {
    grid-column: 2 / -1;
  }

  .history-advanced-query-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
