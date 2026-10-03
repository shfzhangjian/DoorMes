<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcHistoryPieceMassStockApi } from '#/api/mes/hc/plan/history-piece-mass-stock';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, Modal, Table, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getHistoryPieceGoodStockList,
  getHistoryPieceMassStockPage,
} from '#/api/mes/hc/plan/history-piece-mass-stock';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesHcPlanHistoryPieceMassStock' });

type Stock = MesHcHistoryPieceMassStockApi.Stock;
type QueryState = Omit<
  MesHcHistoryPieceMassStockApi.PageReqVO,
  'pageNo' | 'pageSize'
>;
type StockPageResult = { list?: Stock[]; total?: number };
type QueryKey = keyof QueryState;
type LocationParts = {
  locationArea?: string;
  locationLayer?: string;
  locationPosition?: string;
  locationShelf?: string;
};

const queryParams = reactive<QueryState>({
  keyword: '',
  modelCode: '',
  segmentBatchNo: '',
});
const advancedQuery = reactive<QueryState>({ ...queryParams });
const advancedQueryVisible = ref(false);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const goodStockLoading = ref(false);
const goodStockRows = ref<MesHcHistoryPieceMassStockApi.GoodStock[]>([]);
const goodStockTitle = ref('');
const goodStockVisible = ref(false);
const pageRows = ref<Stock[]>([]);
const pageTotal = ref(0);
let currentTimer: ReturnType<typeof setInterval> | null = null;

const pageGoodStockQty = computed(() =>
  pageRows.value.reduce((sum, row) => sum + Number(row.goodStockQty || 0), 0),
);

const goodStockTableRows = computed(() =>
  goodStockRows.value.map((row) => ({
    ...row,
    ...splitLocationParts(
      row.locationName,
      row.locationCode,
      row.warehouseName,
    ),
  })),
);

const activeQueryTags = computed(() => {
  const tags: Array<{ key: QueryKey; label: string; value: string }> = [];
  const addTag = (key: QueryKey, label: string, value?: string) => {
    if (!value) return;
    tags.push({ key, label, value });
  };
  addTag('keyword', '关键词', queryParams.keyword);
  addTag('modelCode', '型号', queryParams.modelCode);
  addTag('segmentBatchNo', '分段批号', queryParams.segmentBatchNo);
  return tags;
});

const quantityFormatter = ({ cellValue }: { cellValue?: number | string }) => {
  if (cellValue === undefined || cellValue === null || cellValue === '')
    return '0';
  const value = Number(cellValue);
  return Number.isFinite(value)
    ? value.toLocaleString('zh-CN', { maximumFractionDigits: 3 })
    : String(cellValue);
};

const dateFormatter = ({ cellValue }: { cellValue?: string }) =>
  String(cellValue || '').slice(0, 10) || '-';

const goodStockColumns = [
  { dataIndex: 'sliceBatchNo', title: '片号', width: 300 },
  { dataIndex: 'modelCode', title: '型号', width: 220 },
  {
    dataIndex: 'segmentBatchNo',
    title: '分段批号',
    width: 300,
  },
  { align: 'right', dataIndex: 'qty', title: '数量', width: 80 },
  { dataIndex: 'warehouseName', title: '仓库', width: 130 },
  { dataIndex: 'locationShelf', title: '货架', width: 120 },
  { dataIndex: 'locationLayer', title: '层', width: 90 },
  { dataIndex: 'locationArea', title: '区', width: 90 },
  {
    dataIndex: 'locationPosition',
    title: '托盘位',
    width: 150,
  },
  { dataIndex: 'inboundTime', title: '入库时间', width: 170 },
];

const columns: VxeTableGridOptions<Stock>['columns'] = [
  { fixed: 'left', type: 'seq', width: 64, title: '序号' },
  {
    field: 'modelCode',
    fixed: 'left',
    minWidth: 150,
    showOverflow: 'tooltip',
    title: '型号',
  },
  {
    field: 'segmentBatchNo',
    fixed: 'left',
    minWidth: 180,
    showOverflow: 'tooltip',
    title: '分段批号',
  },
  {
    align: 'right',
    field: 'goodStockQty',
    formatter: quantityFormatter,
    slots: { default: 'goodStockQty' },
    title: '良品库存',
    width: 130,
  },
  {
    align: 'center',
    field: 'earliestProductionDate',
    formatter: dateFormatter,
    title: '最早生产日期',
    width: 140,
  },
  {
    align: 'center',
    field: 'latestProductionDate',
    formatter: dateFormatter,
    title: '最晚生产日期',
    width: 140,
  },
  { field: 'remark', minWidth: 220, showOverflow: 'tooltip', title: '备注' },
];

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'process-output-balance-vben-grid',
  gridClass: 'process-output-balance-vxe-grid',
  gridOptions: {
    border: true,
    columns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const response = await getHistoryPieceMassStockPage({
            ...queryParams,
            pageNo: Math.max(1, Number(page.currentPage || 1)),
            pageSize: Number(page.pageSize || 20),
          });
          const result = normalizePageResult(response);
          pageRows.value = result.list;
          pageTotal.value = result.total;
          return result;
        },
      },
    },
  },
});

function normalizePageResult(value: unknown): Required<StockPageResult> {
  const result = value as StockPageResult;
  return {
    list: Array.isArray(result?.list) ? result.list : [],
    total: Number(result?.total || 0),
  };
}

async function queryFirstPage() {
  await nextTick();
  await (gridApi.grid as any)?.setCurrentPage?.(1);
  await gridApi.query();
}

function applyQuery() {
  void queryFirstPage();
}

function resetQuery() {
  Object.assign(queryParams, {
    keyword: '',
    modelCode: '',
    segmentBatchNo: '',
  });
  Object.assign(advancedQuery, queryParams);
  applyQuery();
}

function openAdvancedQuery() {
  Object.assign(advancedQuery, queryParams);
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  Object.assign(queryParams, advancedQuery);
  advancedQueryVisible.value = false;
  applyQuery();
}

function removeQueryCondition(key: QueryKey) {
  queryParams[key] = '';
  Object.assign(advancedQuery, queryParams);
  applyQuery();
}

function normalizeLocationNamePart(value?: string) {
  const text = String(value || '').trim();
  return text || undefined;
}

function normalizeLocationCodePart(value?: string, suffix = '') {
  const text = String(value || '').trim();
  return text ? `${text}${suffix}` : undefined;
}

function splitLocationParts(
  locationName?: string,
  locationCode?: string,
  warehouseName?: string,
): LocationParts {
  const nameParts = String(locationName || '')
    .split('-')
    .map((item) => item.trim())
    .filter(Boolean);
  const codeParts = String(locationCode || '')
    .split('-')
    .map((item) => item.trim())
    .filter(Boolean);
  const normalizedWarehouseName = normalizeLocationNamePart(warehouseName);
  const nameOffset =
    normalizedWarehouseName && nameParts[0] === normalizedWarehouseName ? 1 : 0;
  const codeOffset = nameOffset > 0 && codeParts.length > 3 ? 1 : 0;
  return {
    locationArea:
      normalizeLocationNamePart(nameParts[nameOffset + 2]) ||
      normalizeLocationCodePart(codeParts[codeOffset + 2], '区'),
    locationLayer:
      normalizeLocationNamePart(nameParts[nameOffset + 1]) ||
      normalizeLocationCodePart(codeParts[codeOffset + 1], '层'),
    locationPosition:
      normalizeLocationNamePart(nameParts.slice(nameOffset + 3).join('-')) ||
      normalizeLocationCodePart(
        codeParts.slice(codeOffset + 3).join('-'),
        '托盘位',
      ),
    locationShelf:
      normalizeLocationNamePart(nameParts[nameOffset]) ||
      normalizeLocationCodePart(codeParts[codeOffset], '#货架'),
  };
}

async function openGoodStock(row: Stock) {
  if (!Number(row.goodStockQty || 0)) return;
  goodStockTitle.value = `${row.modelCode || '-'} / ${row.segmentBatchNo || '-'}`;
  goodStockVisible.value = true;
  goodStockLoading.value = true;
  try {
    goodStockRows.value = await getHistoryPieceGoodStockList({
      modelCode: row.modelCode,
      segmentBatchNo: row.segmentBatchNo || '',
    });
  } finally {
    goodStockLoading.value = false;
  }
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  applyQuery();
});

onBeforeUnmount(() => {
  if (currentTimer) clearInterval(currentTimer);
});
</script>

<template>
  <Page auto-content-height>
    <div
      class="package-fg-console process-output-balance-console history-piece-mass-stock-console"
    >
      <section class="prototype-banner process-output-balance-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:boxes" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">历史片量产备货库存</h2>
            <Tag color="processing" class="console-title-tag">计划排程</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ pageTotal }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页良品</span>
              <span class="console-meta-value">{{
                quantityFormatter({ cellValue: pageGoodStockQty })
              }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">过滤项</span>
              <span class="console-meta-value">{{
                activeQueryTags.length
              }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateTime.slice(0, 10) }}</div>
          <strong>{{ currentDateTime.slice(11) }}</strong>
        </div>
        <div class="console-action-group process-output-balance-action-group">
          <button class="action-tile" type="button" @click="applyQuery">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-output-balance-query-panel">
        <div
          class="process-output-balance-simple-query history-piece-mass-stock-simple-query"
        >
          <label class="process-output-balance-query-label">查询</label>
          <Input
            v-model:value="queryParams.keyword"
            allow-clear
            placeholder="型号 / 分段批号 / 备注"
            @press-enter="applyQuery"
          />
          <Button type="primary" @click="applyQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon
              ><IconifyIcon icon="lucide:sliders-horizontal"
            /></template>
            多条件查询
          </Button>
        </div>
        <div
          v-if="activeQueryTags.length > 0"
          class="process-output-balance-query-tags"
        >
          <span
            v-for="tag in activeQueryTags"
            :key="tag.key"
            class="process-output-balance-query-tag"
          >
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">
              x
            </button>
          </span>
        </div>
      </section>

      <div class="process-output-balance-body">
        <div class="process-output-balance-grid-host">
          <Grid>
            <template #goodStockQty="{ row }">
              <button
                v-if="Number(row.goodStockQty || 0) > 0"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openGoodStock(row)"
              >
                {{ quantityFormatter({ cellValue: row.goodStockQty }) }}
              </button>
              <span v-else>-</span>
            </template>
          </Grid>
        </div>
      </div>

      <Modal
        v-model:open="advancedQueryVisible"
        title="多条件查询"
        width="760px"
        @ok="applyAdvancedQuery"
      >
        <div class="process-output-balance-advanced-body">
          <div class="process-output-balance-advanced-grid">
            <label class="process-output-balance-query-item">
              <span>型号</span>
              <Input
                v-model:value="advancedQuery.modelCode"
                allow-clear
                placeholder="请输入型号"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>分段批号</span>
              <Input
                v-model:value="advancedQuery.segmentBatchNo"
                allow-clear
                placeholder="请输入分段批号"
              />
            </label>
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="goodStockVisible"
        :footer="null"
        :title="`良品库存明细 - ${goodStockTitle}`"
        width="1500px"
      >
        <Table
          :columns="goodStockColumns"
          :data-source="goodStockTableRows"
          :loading="goodStockLoading"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :row-key="(row) => row.id"
          :scroll="{ x: 1650 }"
          bordered
          size="small"
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.history-piece-mass-stock-console {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.process-output-balance-banner {
  min-height: 78px;
  max-height: 90px;
}

.process-output-balance-action-group {
  display: flex;
  flex-wrap: nowrap;
}

.process-output-balance-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.process-output-balance-simple-query {
  display: grid;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.history-piece-mass-stock-simple-query {
  grid-template-columns: 86px minmax(260px, 1fr) 86px 106px;
}

.process-output-balance-query-label {
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

.process-output-balance-simple-query :deep(.ant-input-affix-wrapper),
.process-output-balance-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.process-output-balance-simple-query :deep(.ant-btn) {
  height: 34px;
  font-weight: 800;
  border-radius: 0;
}

.process-output-balance-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.process-output-balance-query-tag {
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

.process-output-balance-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.process-output-balance-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.process-output-balance-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.process-output-balance-grid-host {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #8794a4;
}

.process-output-balance-grid-host :deep(.vxe-grid) {
  height: 100% !important;
  min-height: 0 !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper) {
  min-height: 0 !important;
  overflow: hidden !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.process-output-balance-qty-drill {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  max-width: 100%;
  min-height: 22px;
  padding: 0 2px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 900;
  text-align: right;
  text-decoration: underline;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.process-output-balance-qty-drill:hover {
  color: #1d4ed8;
}

.process-output-balance-advanced-body {
  display: grid;
  gap: 10px;
}

.process-output-balance-advanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.process-output-balance-query-item {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-query-item > span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-item :deep(.ant-input-affix-wrapper) {
  min-height: 34px;
  border: 0;
  border-radius: 0;
}

@media (max-width: 960px) {
  .history-piece-mass-stock-simple-query {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .history-piece-mass-stock-simple-query :deep(.ant-btn) {
    grid-column: span 2;
  }

  .process-output-balance-query-tags {
    padding-left: 0;
  }

  .process-output-balance-advanced-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
