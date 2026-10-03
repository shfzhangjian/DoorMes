<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Checkbox, DatePicker, Input, message, Modal, Select, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getFgStockHistoryLedgerPage,
  returnManualOutboundFgStockForRepack,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgStockHistory' });

type HistoryLedger = MesHcFinishedPackagingApi.FgStockHistoryLedger;
type HistoryQuery = {
  afterStockStatus: string;
  batchNo: string;
  keyword: string;
  locationCode: string;
  materialCode: string;
  modelCode: string;
  qualityStatus: string;
  sliceBatchNo: string;
  txnTimeEnd: string;
  txnTimeStart: string;
  txnType: string;
  warehouseCode: string;
};

const rows = ref<HistoryLedger[]>([]);
const total = ref(0);
const advancedQueryVisible = ref(false);
const repackReturnVisible = ref(false);
const repackReturning = ref(false);
const repackReturnTarget = ref<HistoryLedger | null>(null);
const repackReturnReason = ref('');
const physicalReturned = ref(false);
const query = reactive<HistoryQuery>({
  afterStockStatus: '',
  batchNo: '',
  keyword: '',
  locationCode: '',
  materialCode: '',
  modelCode: '',
  qualityStatus: '',
  sliceBatchNo: '',
  txnTimeEnd: '',
  txnTimeStart: '',
  txnType: '',
  warehouseCode: '',
});

const txnTypeOptions = [
  { label: '成品入库', value: 'FG_INBOUND' },
  { label: 'COA冻结', value: 'FG_COA_FREEZE' },
  { label: 'COA解除冻结', value: 'FG_COA_UNFREEZE' },
  { label: '成品下架', value: 'FG_UNSHELF' },
  { label: '手工成品出库', value: 'FG_MANUAL_OUTBOUND' },
  { label: '合格品待上架直接出库', value: 'FG_PACKAGE_DIRECT_OUTBOUND' },
  { label: '发货配货锁定', value: 'FG_OUTBOUND_LOCK' },
  { label: '解除发货锁定', value: 'FG_OUTBOUND_UNLOCK' },
  { label: '发货包装分配', value: 'FG_OUTBOUND_ALLOCATE' },
  { label: '成品出库/发货', value: 'FG_SHIP' },
  { label: '发货检验退回', value: 'FG_SHIPPING_RETURN' },
  { label: '发货取消', value: 'FG_SHIPPING_CANCEL' },
  { label: '包装撤销', value: 'FG_PACKAGING_CANCEL' },
  { label: '拆包退回待包装', value: 'FG_PACKAGE_SPLIT_RETURN' },
];
const stockStatusOptions = [
  { label: '待上架', value: 'INBOUND_LOCKED' },
  { label: '可用在库', value: 'AVAILABLE' },
  { label: '发货锁定', value: 'OUTBOUND_LOCKED' },
  { label: '已分配发货包装', value: 'ALLOCATED' },
  { label: '已出库', value: 'SHIPPED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '退回待返工', value: 'RETURNED_NG' },
  { label: '退回待重新包装', value: 'RETURNED_FOR_REPACK' },
];
const qualityStatusOptions = [
  { label: 'OK 合格', value: 'OK' },
  { label: 'NG 不合格', value: 'NG' },
];

const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const pageInboundCount = computed(() => rows.value.filter((row) => row.txnType === 'FG_INBOUND').length);
const pageOutboundCount = computed(() => rows.value.filter((row) => ['FG_MANUAL_OUTBOUND', 'FG_PACKAGE_DIRECT_OUTBOUND', 'FG_SHIP'].includes(row.txnType || '')).length);
const pagePieceCount = computed(() => new Set(rows.value.map((row) => row.sliceBatchNo || row.stockNo).filter(Boolean)).size);

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, number | string> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  (Object.keys(query) as Array<keyof HistoryQuery>).forEach((key) => {
    const value = query[key].trim();
    if (value) params[key] = value;
  });
  return params;
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  Object.assign(query, {
    afterStockStatus: '',
    batchNo: '',
    keyword: '',
    locationCode: '',
    materialCode: '',
    modelCode: '',
    qualityStatus: '',
    sliceBatchNo: '',
    txnTimeEnd: '',
    txnTimeStart: '',
    txnType: '',
    warehouseCode: '',
  });
  advancedQueryVisible.value = false;
  handleSearch();
}

function operationText(value?: string) {
  return txnTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function operationColor(value?: string) {
  if (value === 'FG_INBOUND') return 'green';
  if (['FG_MANUAL_OUTBOUND', 'FG_PACKAGE_DIRECT_OUTBOUND', 'FG_SHIP'].includes(value || '')) return 'volcano';
  if (value?.includes('RETURN') || value?.includes('CANCEL')) return 'gold';
  if (value?.includes('LOCK') || value === 'FG_OUTBOUND_ALLOCATE') return 'blue';
  return 'default';
}

function statusText(value?: string) {
  return stockStatusOptions.find((item) => item.value === value)?.label || value || '-';
}

function statusColor(value?: string) {
  if (value === 'AVAILABLE') return 'green';
  if (value === 'SHIPPED') return 'volcano';
  if (value === 'OUTBOUND_LOCKED' || value === 'ALLOCATED') return 'blue';
  if (value?.startsWith('RETURNED')) return 'gold';
  return 'default';
}

function formatDateTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-';
}

function openRepackReturn(row: HistoryLedger) {
  if (!row.manualOutboundRepackReturnable) {
    message.warning('该手工出库流水已不是可退回状态，请刷新后重试');
    return;
  }
  repackReturnTarget.value = row;
  repackReturnReason.value = '';
  physicalReturned.value = false;
  repackReturnVisible.value = true;
}

async function confirmRepackReturn() {
  const target = repackReturnTarget.value;
  const reason = repackReturnReason.value.trim();
  if (!target?.id) {
    message.warning('请选择需要退回的手工出库流水');
    return;
  }
  if (!reason) {
    message.warning('请填写退回原因');
    return;
  }
  if (!physicalReturned.value) {
    message.warning('请确认实物已退回并已拆开原包装');
    return;
  }
  repackReturning.value = true;
  try {
    const result = await returnManualOutboundFgStockForRepack({
      physicalReturned: true,
      reason,
      txnLogId: target.id,
    });
    message.success(`片号 ${result.sliceBatchNo} 已退回待重新包装，请在包装台重新包装并生成新标签`);
    repackReturnVisible.value = false;
    repackReturnTarget.value = null;
    await gridApi.query();
  } finally {
    repackReturning.value = false;
  }
}

const columns = [
  { field: 'txnTime', formatter: ({ row }: { row: HistoryLedger }) => formatDateTime(row.txnTime), title: '操作时间', width: 170 },
  { field: 'txnType', slots: { default: 'txnType' }, title: '操作类型', width: 150 },
  { field: 'stockNo', showOverflow: 'tooltip', title: '库存号', width: 170 },
  { field: 'innerUnitNo', showOverflow: 'tooltip', title: '包装编号', width: 170 },
  { field: 'sliceBatchNo', showOverflow: 'tooltip', title: '片号', width: 190 },
  { field: 'materialCode', showOverflow: 'tooltip', title: '料号', width: 135 },
  { field: 'modelCode', showOverflow: 'tooltip', title: '型号', width: 135 },
  { field: 'batchNo', showOverflow: 'tooltip', title: '分段批号', width: 160 },
  { field: 'qty', title: '数量', width: 85 },
  { field: 'qualityStatus', slots: { default: 'qualityStatus' }, title: '质量', width: 90 },
  { field: 'warehouseName', showOverflow: 'tooltip', title: '操作仓库', width: 145 },
  { field: 'locationCode', showOverflow: 'tooltip', title: '操作后库位', width: 150 },
  { field: 'afterStockStatus', slots: { default: 'afterStockStatus' }, title: '操作后状态', width: 150 },
  { field: 'refDocNo', showOverflow: 'tooltip', title: '关联单据', width: 170 },
  { field: 'operatorName', title: '操作人', width: 110 },
  { field: 'remark', minWidth: 220, showOverflow: 'tooltip', title: '操作说明 / 原因' },
  { field: 'actions', fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 152 },
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
          const result = await getFgStockHistoryLedgerPage(buildQueryParams(page));
          rows.value = result.list || [];
          total.value = Number(result.total || 0);
          return result;
        },
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
  } as VxeTableGridOptions<HistoryLedger>,
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console fg-stock-history-board">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:history" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">成品库存出入库记录</h2>
            <Tag class="console-title-tag" color="blue">操作流水留存</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"><span class="console-meta-label">流水总数</span><span class="console-meta-value">{{ total }}</span><span class="console-meta-sub">条</span></span>
            <span class="console-meta-item"><span class="console-meta-label">本页入库</span><span class="console-meta-value">{{ pageInboundCount }}</span></span>
            <span class="console-meta-item"><span class="console-meta-label">本页出库</span><span class="console-meta-value">{{ pageOutboundCount }}</span></span>
            <span class="console-meta-item"><span class="console-meta-label">本页片数</span><span class="console-meta-value">{{ pagePieceCount }}</span></span>
          </div>
        </div>
        <div class="work-time-card"><div>{{ currentDateText }}</div><strong>{{ currentTimeText }}</strong></div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="handleSearch"><IconifyIcon icon="lucide:search" /><span>查询</span></button>
          <button class="action-tile" type="button" @click="handleReset"><IconifyIcon icon="lucide:rotate-ccw" /><span>重置</span></button>
        </div>
      </section>

      <section class="package-fg-filter-bar history-query-panel">
        <div class="history-simple-query">
          <label class="history-simple-query-label">关键词</label>
          <Input v-model:value="query.keyword" allow-clear placeholder="库存号 / 包装编号 / 片号 / 分段批号 / 关联单据" @press-enter="handleSearch" />
          <Button type="primary" @click="handleSearch"><template #icon><IconifyIcon icon="lucide:search" /></template>查询</Button>
          <Button @click="advancedQueryVisible = true"><template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>多条件查询</Button>
        </div>
      </section>

      <Modal v-model:open="advancedQueryVisible" :footer="null" title="多条件查询" width="920px" wrap-class-name="stock-advanced-query-modal">
        <div class="history-advanced-query-body">
          <div class="history-advanced-query-grid">
            <div class="history-query-item"><label>操作类型</label><Select v-model:value="query.txnType" :options="txnTypeOptions" allow-clear placeholder="请选择" /></div>
            <div class="history-query-item"><label>操作后状态</label><Select v-model:value="query.afterStockStatus" :options="stockStatusOptions" allow-clear placeholder="请选择" /></div>
            <div class="history-query-item"><label>质量状态</label><Select v-model:value="query.qualityStatus" :options="qualityStatusOptions" allow-clear placeholder="请选择" /></div>
            <div class="history-query-item"><label>操作仓库</label><Input v-model:value="query.warehouseCode" allow-clear placeholder="仓库编码" /></div>
            <div class="history-query-item"><label>操作后库位</label><Input v-model:value="query.locationCode" allow-clear placeholder="库位编码" /></div>
            <div class="history-query-item"><label>片号</label><Input v-model:value="query.sliceBatchNo" allow-clear placeholder="片号" /></div>
            <div class="history-query-item"><label>分段批号</label><Input v-model:value="query.batchNo" allow-clear placeholder="分段批号" /></div>
            <div class="history-query-item"><label>料号</label><Input v-model:value="query.materialCode" allow-clear placeholder="料号" /></div>
            <div class="history-query-item"><label>型号</label><Input v-model:value="query.modelCode" allow-clear placeholder="型号" /></div>
            <div class="history-query-item"><label>操作时间起</label><DatePicker v-model:value="query.txnTimeStart" show-time value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择" /></div>
            <div class="history-query-item"><label>操作时间止</label><DatePicker v-model:value="query.txnTimeEnd" show-time value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择" /></div>
          </div>
          <div class="history-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="handleReset">清空条件</Button>
            <Button type="primary" @click="advancedQueryVisible = false; handleSearch()">应用查询</Button>
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="repackReturnVisible"
        :confirm-loading="repackReturning"
        cancel-text="取消"
        ok-text="确认退回待重新包装"
        title="手工成品出库单片退回待重新包装"
        @ok="confirmRepackReturn"
      >
        <div v-if="repackReturnTarget" class="history-repack-return-confirm">
          <p>片号：<strong>{{ repackReturnTarget.sliceBatchNo || repackReturnTarget.stockNo || '-' }}</strong></p>
          <p>原内包装：{{ repackReturnTarget.innerUnitNo || '-' }}</p>
          <p>原出库流水：{{ repackReturnTarget.txnNo || '-' }}</p>
          <p class="history-repack-return-confirm__warning">
            确认后仅拆出这一片，原包装若仍有其它片则保留其余片；该片会回到包装台“待包装”，必须重新包装并生成新内包装/标签后，才能再次进入待上架流程。
          </p>
          <Input.TextArea
            v-model:value="repackReturnReason"
            :maxlength="200"
            :rows="3"
            allow-clear
            placeholder="请填写退回原因（必填）"
            show-count
          />
          <Checkbox v-model:checked="physicalReturned" class="history-repack-return-confirm__check">
            我已确认实物已退回，并已将该片从原内包装中拆出
          </Checkbox>
        </div>
      </Modal>

      <section class="package-fg-grid-panel history-ledger-panel">
        <Grid table-title="成品库存操作流水">
          <template #txnType="{ row }"><Tag :color="operationColor(row.txnType)">{{ operationText(row.txnType) }}</Tag></template>
          <template #qualityStatus="{ row }"><Tag :color="row.qualityStatus === 'OK' ? 'green' : row.qualityStatus === 'NG' ? 'red' : 'default'">{{ row.qualityStatus || '-' }}</Tag></template>
          <template #afterStockStatus="{ row }"><Tag :color="statusColor(row.afterStockStatus)">{{ statusText(row.afterStockStatus) }}</Tag></template>
          <template #actions="{ row }">
            <Button
              v-if="row.manualOutboundRepackReturnable"
              v-access:code="['mes:inv:fg-stock-history:manual-outbound-repack-return']"
              danger
              size="small"
              type="link"
              @click.stop="openRepackReturn(row)"
            >
              退回待重新包装
            </Button>
            <span v-else>-</span>
          </template>
        </Grid>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.fg-stock-history-board { grid-template-rows: max-content max-content minmax(0, 1fr); gap: 8px; }
.history-query-panel { display: flex; box-sizing: border-box; flex-direction: column; gap: 8px; padding: 8px 10px; overflow: hidden; background: #eef3f8; border: 1px solid #8794a4; }
.history-simple-query { display: grid; grid-template-columns: max-content minmax(300px, 1fr) max-content max-content; gap: 8px; align-items: center; min-width: 0; }
.history-simple-query-label { color: #243142; font-size: 13px; font-weight: 600; white-space: nowrap; }
.history-query-panel :deep(.ant-input), .history-query-item :deep(.ant-input), .history-query-item :deep(.ant-picker), .history-query-item :deep(.ant-select-selector) { width: 100%; min-height: 34px; border-radius: 6px; }
.history-advanced-query-body { display: flex; flex-direction: column; gap: 14px; }
.history-advanced-query-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.history-query-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.history-query-item label { color: #475569; font-size: 12px; font-weight: 600; }
.history-advanced-footer { display: flex; justify-content: flex-end; gap: 8px; }
.history-repack-return-confirm { display: flex; flex-direction: column; gap: 10px; }
.history-repack-return-confirm p { margin: 0; color: #334155; }
.history-repack-return-confirm__warning { padding: 8px 10px; color: #9a3412 !important; line-height: 1.65; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 6px; }
.history-repack-return-confirm__check { color: #334155; }
.history-ledger-panel { display: flex; min-height: 0; flex-direction: column; padding: 8px; overflow: hidden; background: #f8fafc; border: 1px solid #8794a4; }
.history-ledger-panel :deep(.vben-vxe-grid), .history-ledger-panel :deep(.vxe-grid) { height: 100%; min-height: 0; }
.history-ledger-panel :deep(.vxe-grid) { display: flex; flex-direction: column; }
.history-ledger-panel :deep(.vxe-grid--table-wrapper) { flex: 1 1 auto; min-height: 0; }
.history-ledger-panel :deep(.vxe-grid--pager-wrapper) { flex: 0 0 auto; }
.history-ledger-panel :deep(.vxe-pager) { margin: 0; border-top: 1px solid #d8e0ea; }
@media (max-width: 1280px) { .history-advanced-query-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 720px) { .history-simple-query, .history-advanced-query-grid { grid-template-columns: 1fr; } }
</style>
