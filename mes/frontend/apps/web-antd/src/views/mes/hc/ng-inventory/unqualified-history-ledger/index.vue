<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, DatePicker, Input, Modal, Select, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getUnqualifiedHistoryLedgerPage } from '#/api/mes/hc/ng-inventory';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesUnqualifiedHistoryLedger' });

type HistoryLedger = MesHcNgInventoryApi.UnqualifiedHistoryLedger;
type HistoryQuery = {
  afterStatus: string;
  eventTimeEnd: string;
  eventTimeStart: string;
  eventType: string;
  inventoryArea: string;
  inventoryDirection: string;
  keyword: string;
  materialCode: string;
  modelCode: string;
  planNo: string;
  qualityStatus: string;
  segmentBatchNo: string;
  sliceBatchNo: string;
};

const rows = ref<HistoryLedger[]>([]);
const total = ref(0);
const advancedQueryVisible = ref(false);
const query = reactive<HistoryQuery>({
  afterStatus: '',
  eventTimeEnd: '',
  eventTimeStart: '',
  eventType: '',
  inventoryArea: '',
  inventoryDirection: '',
  keyword: '',
  materialCode: '',
  modelCode: '',
  planNo: '',
  qualityStatus: '',
  segmentBatchNo: '',
  sliceBatchNo: '',
});

const inventoryAreaOptions = [
  { label: '分切压槽不合格品库', value: 'NG_WAREHOUSE' },
  { label: '不合格待包装区', value: 'WAIT_PACKAGING' },
  { label: '成品仓不合格品', value: 'FG_WAREHOUSE' },
];
const inventoryDirectionOptions = [
  { label: '入库', value: 'IN' },
  { label: '出库', value: 'OUT' },
];
const eventTypeOptions = [
  { label: '不合格品上架', value: 'NG_SHELF' },
  { label: '冻结品上架', value: 'FREEZE_SHELF' },
  { label: '不合格品报废', value: 'NG_SCRAP' },
  { label: '不合格品手工出库', value: 'NG_MANUAL_OUTBOUND' },
  { label: '返工出库', value: 'NG_REWORK_OUT' },
  { label: '成品入库', value: 'FG_INBOUND' },
  { label: '成品手工出库', value: 'FG_MANUAL_OUTBOUND' },
  { label: '成品发货', value: 'FG_SHIP' },
  { label: '已包装未上架出库', value: 'PACKAGING_MANUAL_OUTBOUND' },
  { label: '合格品待上架直接出库', value: 'PACKAGING_DIRECT_OUTBOUND' },
  { label: '合格品待上架直接出库', value: 'FG_PACKAGE_DIRECT_OUTBOUND' },
];
const qualityStatusOptions = [
  { label: 'NG 不合格', value: 'NG' },
  { label: 'OK 已转合格', value: 'OK' },
];
const statusOptions = [
  { label: '待包装', value: 'WAIT_PACKAGING' },
  { label: '已包装', value: 'PACKED' },
  { label: '已入库', value: 'INBOUNDED' },
  { label: '可用在库', value: 'AVAILABLE' },
  { label: 'NG 已上架', value: 'STORED' },
  { label: 'NG 已冻结', value: 'FROZEN' },
  { label: '返工加工中', value: 'REWORKING' },
  { label: '移库出库', value: 'TRANSFER_OUT' },
  { label: 'NG 已出库', value: 'OUTBOUNDED' },
  { label: '已返工', value: 'RETURNED' },
  { label: '已出库', value: 'SHIPPED' },
  { label: '已报废', value: 'SCRAPPED' },
  { label: '已作废', value: 'VOID' },
];

const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const pageNgWarehouseCount = computed(
  () => rows.value.filter((row) => row.inventoryArea === 'NG_WAREHOUSE').length,
);
const pagePackagingCount = computed(
  () =>
    rows.value.filter((row) => row.inventoryArea === 'WAIT_PACKAGING').length,
);
const pageFgWarehouseCount = computed(
  () => rows.value.filter((row) => row.inventoryArea === 'FG_WAREHOUSE').length,
);

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, number | string> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  (Object.keys(query) as Array<keyof HistoryQuery>).forEach((key) => {
    const value = (query[key] || '').trim();
    if (value) params[key] = value;
  });
  return params;
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  Object.assign(query, {
    afterStatus: '',
    eventTimeEnd: '',
    eventTimeStart: '',
    eventType: '',
    inventoryArea: '',
    inventoryDirection: '',
    keyword: '',
    materialCode: '',
    modelCode: '',
    planNo: '',
    qualityStatus: '',
    segmentBatchNo: '',
    sliceBatchNo: '',
  });
  advancedQueryVisible.value = false;
  handleSearch();
}

function areaText(value?: string) {
  return (
    inventoryAreaOptions.find((item) => item.value === value)?.label ||
    value ||
    '-'
  );
}

function areaColor(value?: string) {
  if (value === 'NG_WAREHOUSE') return 'orange';
  if (value === 'WAIT_PACKAGING') return 'gold';
  if (value === 'FG_WAREHOUSE') return 'volcano';
  return 'default';
}

function eventText(value?: string) {
  return (
    eventTypeOptions.find((item) => item.value === value)?.label || value || '-'
  );
}

function eventColor(value?: string) {
  if (
    value?.includes('ENTER') ||
    value?.includes('INBOUND') ||
    value?.includes('SHELF')
  )
    return 'green';
  if (
    value?.includes('OUT') ||
    value?.includes('SCRAP') ||
    value?.includes('VOID') ||
    value?.includes('SHIP')
  )
    return 'volcano';
  if (value?.includes('RETURN') || value?.includes('CHANGED')) return 'gold';
  return 'blue';
}

function statusText(value?: string) {
  return (
    statusOptions.find((item) => item.value === value)?.label || value || '-'
  );
}

function statusColor(value?: string) {
  if (['AVAILABLE', 'INBOUNDED'].includes(value || '')) return 'green';
  if (
    ['OUTBOUNDED', 'SCRAPPED', 'SHIPPED', 'TRANSFER_OUT', 'VOID'].includes(
      value || '',
    )
  )
    return 'volcano';
  if (value?.startsWith('WAIT')) return 'gold';
  return 'default';
}

function formatDateTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-';
}

const columns = [
  {
    field: 'eventTime',
    formatter: ({ row }: { row: HistoryLedger }) =>
      formatDateTime(row.eventTime),
    title: '操作时间',
    width: 170,
  },
  {
    field: 'inventoryArea',
    slots: { default: 'inventoryArea' },
    title: '库存区域',
    width: 150,
  },
  {
    field: 'eventType',
    slots: { default: 'eventType' },
    title: '操作类型',
    width: 160,
  },
  { field: 'inventoryDirection', slots: { default: 'inventoryDirection' }, title: '出入库方向', width: 110 },
  { field: 'sliceBatchNo', showOverflow: 'tooltip', title: '片号', width: 185 },
  {
    field: 'segmentBatchNo',
    showOverflow: 'tooltip',
    title: '分段批号',
    width: 170,
  },
  { field: 'planNo', showOverflow: 'tooltip', title: '生产计划', width: 155 },
  { field: 'processName', title: '来源工序', width: 105 },
  { field: 'materialCode', showOverflow: 'tooltip', title: '料号', width: 140 },
  { field: 'modelCode', showOverflow: 'tooltip', title: '型号', width: 130 },
  { field: 'qty', title: '数量', width: 80 },
  {
    field: 'fqcResult',
    slots: { default: 'fqcResult' },
    title: 'FQC',
    width: 80,
  },
  {
    field: 'coaResult',
    slots: { default: 'coaResult' },
    title: 'COA',
    width: 80,
  },
  {
    field: 'qualityStatus',
    slots: { default: 'qualityStatus' },
    title: '质量',
    width: 90,
  },
  {
    field: 'warehouseName',
    showOverflow: 'tooltip',
    title: '操作仓库',
    width: 145,
  },
  {
    field: 'locationCode',
    showOverflow: 'tooltip',
    title: '操作后库位',
    width: 150,
  },
  {
    field: 'afterStatus',
    slots: { default: 'afterStatus' },
    title: '操作后状态',
    width: 140,
  },
  { field: 'refDocNo', showOverflow: 'tooltip', title: '关联单据', width: 160 },
  { field: 'operatorName', title: '操作人', width: 105 },
  {
    field: 'remark',
    minWidth: 220,
    showOverflow: 'tooltip',
    title: '操作说明 / 原因',
  },
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getUnqualifiedHistoryLedgerPage(
            buildQueryParams(page),
          );
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
    <div class="package-fg-console unqualified-history-board">
      <section class="prototype-banner">
        <span class="console-main-icon"
          ><IconifyIcon icon="lucide:history"
        /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">不合格品出入库记录</h2>
            <Tag class="console-title-tag" color="volcano"
              >三类库存统一追溯</Tag
            >
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"
              ><span class="console-meta-label">流水总数</span
              ><span class="console-meta-value">{{ total }}</span
              ><span class="console-meta-sub">条</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">本页 NG 仓</span
              ><span class="console-meta-value">{{
                pageNgWarehouseCount
              }}</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">本页待包装</span
              ><span class="console-meta-value">{{
                pagePackagingCount
              }}</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">本页成品仓</span
              ><span class="console-meta-value">{{
                pageFgWarehouseCount
              }}</span></span
            >
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" /><span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" /><span>重置</span>
          </button>
        </div>
      </section>

      <section class="history-query-panel">
        <div class="history-simple-query">
          <label class="history-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="片号 / 批次 / 计划 / 单据 / 包装编号"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch"
            ><template #icon><IconifyIcon icon="lucide:search" /></template
            >查询</Button
          >
          <Button @click="advancedQueryVisible = true"
            ><template #icon
              ><IconifyIcon icon="lucide:sliders-horizontal" /></template
            >多条件查询</Button
          >
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="不合格品出入库记录筛选"
        width="920px"
        wrap-class-name="stock-advanced-query-modal"
      >
        <div class="history-advanced-query-body">
          <div class="history-advanced-query-grid">
            <div class="history-query-item">
              <label>库存区域</label
              ><Select
                v-model:value="query.inventoryArea"
                :options="inventoryAreaOptions"
                allow-clear
                placeholder="请选择"
              />
            </div>
            <div class="history-query-item">
              <label>出入库方向</label>
              <Select
                v-model:value="query.inventoryDirection"
                :options="inventoryDirectionOptions"
                allow-clear
                placeholder="全部方向"
              />
            </div>
            <div class="history-query-item">
              <label>操作类型</label
              ><Select
                v-model:value="query.eventType"
                :options="eventTypeOptions"
                allow-clear
                placeholder="请选择"
              />
            </div>
            <div class="history-query-item">
              <label>操作后状态</label
              ><Select
                v-model:value="query.afterStatus"
                :options="statusOptions"
                allow-clear
                placeholder="请选择"
              />
            </div>
            <div class="history-query-item">
              <label>质量状态</label
              ><Select
                v-model:value="query.qualityStatus"
                :options="qualityStatusOptions"
                allow-clear
                placeholder="请选择"
              />
            </div>
            <div class="history-query-item">
              <label>片号</label
              ><Input
                v-model:value="query.sliceBatchNo"
                allow-clear
                placeholder="片号"
              />
            </div>
            <div class="history-query-item">
              <label>分段批号</label
              ><Input
                v-model:value="query.segmentBatchNo"
                allow-clear
                placeholder="分段批号"
              />
            </div>
            <div class="history-query-item">
              <label>生产计划</label
              ><Input
                v-model:value="query.planNo"
                allow-clear
                placeholder="生产计划号"
              />
            </div>
            <div class="history-query-item">
              <label>料号</label
              ><Input
                v-model:value="query.materialCode"
                allow-clear
                placeholder="料号"
              />
            </div>
            <div class="history-query-item">
              <label>型号</label
              ><Input
                v-model:value="query.modelCode"
                allow-clear
                placeholder="型号"
              />
            </div>
            <div class="history-query-item">
              <label>操作时间起</label
              ><DatePicker
                v-model:value="query.eventTimeStart"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择"
              />
            </div>
            <div class="history-query-item">
              <label>操作时间止</label
              ><DatePicker
                v-model:value="query.eventTimeEnd"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择"
              />
            </div>
          </div>
          <div class="history-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="handleReset">清空条件</Button>
            <Button
              type="primary"
              @click="
                advancedQueryVisible = false;
                handleSearch();
              "
              >应用查询</Button
            >
          </div>
        </div>
      </Modal>

      <section class="history-ledger-panel">
        <Grid table-title="不合格品统一历史操作流水">
          <template #inventoryArea="{ row }"
            ><Tag :color="areaColor(row.inventoryArea)">{{
              areaText(row.inventoryArea)
            }}</Tag></template
          >
          <template #eventType="{ row }"
            ><Tag :color="eventColor(row.eventType)">{{
              eventText(row.eventType)
            }}</Tag></template
          >
          <template #inventoryDirection="{ row }">
            <Tag v-if="row.inventoryDirection === 'IN'" color="green">入库</Tag>
            <Tag v-else-if="row.inventoryDirection === 'OUT'" color="red">出库</Tag>
            <span v-else-if="row.inventoryDirection === 'NONE'">—</span>
            <Tag v-else color="gold">待识别</Tag>
          </template>
          <template #fqcResult="{ row }"
            ><Tag
              v-if="row.fqcResult"
              :color="row.fqcResult === 'NG' ? 'red' : 'green'"
              >{{ row.fqcResult }}</Tag
            ><span v-else>-</span></template
          >
          <template #coaResult="{ row }"
            ><Tag
              v-if="row.coaResult"
              :color="row.coaResult === 'NG' ? 'red' : 'green'"
              >{{ row.coaResult }}</Tag
            ><span v-else>-</span></template
          >
          <template #qualityStatus="{ row }"
            ><Tag
              :color="
                row.qualityStatus === 'NG'
                  ? 'red'
                  : row.qualityStatus === 'OK'
                    ? 'green'
                    : 'default'
              "
              >{{ row.qualityStatus || '-' }}</Tag
            ></template
          >
          <template #afterStatus="{ row }"
            ><Tag :color="statusColor(row.afterStatus)">{{
              statusText(row.afterStatus)
            }}</Tag></template
          >
        </Grid>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.unqualified-history-board {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
}
.history-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.history-simple-query {
  display: grid;
  grid-template-columns: max-content minmax(300px, 1fr) max-content max-content;
  gap: 8px;
  align-items: center;
  min-width: 0;
}
.history-simple-query-label {
  color: #243142;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.history-query-panel :deep(.ant-input),
.history-query-item :deep(.ant-input),
.history-query-item :deep(.ant-picker),
.history-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
  border-radius: 6px;
}
.history-advanced-query-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.history-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.history-query-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.history-query-item label {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}
.history-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.history-ledger-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}
.history-ledger-panel :deep(.vben-vxe-grid),
.history-ledger-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}
.history-ledger-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}
.history-ledger-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}
.history-ledger-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}
.history-ledger-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}
@media (max-width: 1280px) {
  .history-advanced-query-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .history-simple-query,
  .history-advanced-query-grid {
    grid-template-columns: 1fr;
  }
}
</style>
