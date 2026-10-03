<script lang="ts" setup>
import type { MesHcProductionRecordApi } from '#/api/mes/hc/production-record';

import { computed, onMounted, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  DatePicker,
  Input,
  message,
  Pagination,
  Select,
  Spin,
  TabPane,
  Tabs,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  exportCutRoundProductionRecord,
  getCutRoundProductionRecordPage,
} from '#/api/mes/hc/production-record';

import ProductionRecordRevisionModal, {
  type ProductionRecordRevisionField,
} from './components/ProductionRecordRevisionModal.vue';

import '../../package-fg/shared/cut-round-board.css';
import '../../package-fg/shared/production-record-ledger.css';

const SIZE_OPTIONS = [
  { label: '775mm', value: '775' },
  { label: '740mm', value: '740' },
];

const RangePicker = DatePicker.RangePicker;
const queryDateRange = ref<[string, string] | null>(null);

const loading = ref(false);
const exporting = ref(false);
const { hasAccessByCodes } = useAccess();
const rows = ref<MesHcProductionRecordApi.RecordRow[]>([]);
const revisionOpen = ref(false);
const revisionRow = ref<MesHcProductionRecordApi.RecordRow>();
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const activePadTab = ref('ALL');
const PAD_TABS = [
  { key: 'ALL', label: '全部记录' },
  { key: 'WHITE_PAD', label: '白垫' },
  { key: 'BLACK_PAD', label: '黑垫' },
  { key: 'UNCLASSIFIED', label: '未归类' },
];
const queryForm = reactive({
  cutSizeMm: undefined as string | undefined,
  modelCode: '',
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
  productionBatchNo: '',
});

const canRevise = computed(() =>
  hasAccessByCodes(['mes:pp:cut-round-production-record:revise']),
);
const REVISION_FIELDS: ProductionRecordRevisionField[] = [
  { key: 'reportDate', label: '日期', type: 'date' },
  { key: 'modelCode', label: '型号', type: 'text' },
  { key: 'productionBatchNo', label: '生产批号', type: 'text' },
  { key: 'cutSizeMm', label: '裁切尺寸(mm)', type: 'text' },
  { key: 'inputQty', label: '投入(pcs)', type: 'number' },
  { key: 'outputQty', label: '产出(pcs)', type: 'number' },
  { key: 'bladeModel', label: '刀片型号', type: 'text' },
  { key: 'bladeBatchNo', label: '刀片批号', type: 'text' },
  { key: 'bladeUseCount', label: '刀片累计裁切(pcs)', type: 'integer' },
  { key: 'feltModel', label: '毛毡型号', type: 'text' },
  { key: 'feltBatchNo', label: '毛毡批号', type: 'text' },
  { key: 'feltUseCount', label: '裁切片数累计(pcs)', type: 'integer' },
  { key: 'feltUseDays', label: '毛毡累计使用天数', type: 'integer' },
  { key: 'bladeReplaceReason', label: '刀片更换原因', type: 'text' },
  { key: 'recorderName', label: '记录人', type: 'text' },
  { key: 'recordTime', label: '记录时间', type: 'datetime' },
  { key: 'remark', label: '备注', type: 'text' },
];

const currentInputQty = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.inputQty || 0), 0),
);
const currentOutputQty = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.outputQty || 0), 0),
);

function buildFilterParams(extra: Record<string, any> = {}) {
  return {
    reportDateStart: queryDateRange.value?.[0] || undefined,
    reportDateEnd: queryDateRange.value?.[1] || undefined,
    cutSizeMm: queryForm.cutSizeMm,
    modelCode: queryForm.modelCode || undefined,
    padType: queryForm.padType || undefined,
    productionBatchNo: queryForm.productionBatchNo || undefined,
    ...extra,
  };
}

async function fetchData() {
  loading.value = true;
  try {
    const page = await getCutRoundProductionRecordPage(
      buildFilterParams({ pageNo: pageNo.value, pageSize: pageSize.value }),
    );
    rows.value = page.list || [];
    total.value = Number(page.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  void fetchData();
}

function handleReset() {
  queryDateRange.value = null;
  queryForm.cutSizeMm = undefined;
  queryForm.modelCode = '';
  queryForm.padType = '';
  queryForm.productionBatchNo = '';
  activePadTab.value = 'ALL';
  handleSearch();
}

function handlePadTabChange(tab: string | number) {
  const value = String(tab);
  activePadTab.value = value;
  queryForm.padType = value === 'ALL' ? '' : value as 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
  handleSearch();
}

function padTypeLabel(padType?: string) {
  return padType === 'BLACK_PAD' ? '黑垫' : padType === 'WHITE_PAD' ? '白垫' : '未归类';
}

function padTypeColor(padType?: string) {
  return padType === 'BLACK_PAD' ? 'default' : padType === 'WHITE_PAD' ? 'blue' : 'warning';
}

function handlePageChange(nextPageNo: number, nextPageSize: number) {
  pageNo.value = nextPageNo;
  pageSize.value = nextPageSize;
  void fetchData();
}

async function handleExport() {
  exporting.value = true;
  try {
    const data = await exportCutRoundProductionRecord(buildFilterParams());
    downloadFileFromBlobPart({
      fileName: '裁切生产记录表.xlsx',
      source: data,
    });
    message.success('裁切生产记录表已导出');
  } finally {
    exporting.value = false;
  }
}

function openRevision(row: MesHcProductionRecordApi.RecordRow) {
  revisionRow.value = row;
  revisionOpen.value = true;
}

function formatQty(value?: number) {
  if (value === null || value === undefined || Number.isNaN(Number(value)))
    return '-';
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

function displayText(value?: null | number | string, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function compactText(value?: string, maxLength = 18) {
  const text = displayText(value, '');
  return text.length > maxLength
    ? `${text.slice(0, maxLength)}...`
    : text || '-';
}

function rowKey(row: MesHcProductionRecordApi.RecordRow, index: number) {
  return [
    row.reportDate,
    row.modelCode,
    row.productionBatchNo,
    row.cutSizeMm,
    index,
  ].join('|');
}

onMounted(fetchData);
</script>

<template>
  <Page
    auto-content-height
    class="production-record-page"
    content-class="production-record-content"
  >
    <div class="package-fg-console production-record-report">
      <section class="prototype-banner production-record-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:scissors" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">裁切生产记录表</h2>
            <Tag class="console-title-tag" color="processing">生产报表</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"
              ><span class="console-meta-label">当前页</span
              ><span class="console-meta-value">{{ rows.length }}</span
              ><span class="console-meta-sub">行</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">总数</span
              ><span class="console-meta-value">{{ total }}</span
              ><span class="console-meta-sub">行</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">投入</span
              ><span class="console-meta-value">{{
                formatQty(currentInputQty)
              }}</span
              ><span class="console-meta-sub">pcs</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">产出</span
              ><span class="console-meta-value">{{
                formatQty(currentOutputQty)
              }}</span
              ><span class="console-meta-sub">pcs</span></span
            >
          </div>
        </div>
        <div class="console-action-group production-record-action-group">
          <button class="action-tile" type="button" @click="fetchData">
            <IconifyIcon icon="lucide:refresh-cw" /><span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="exporting"
            @click="handleExport"
          >
            <IconifyIcon icon="lucide:file-spreadsheet" /><span>{{
              exporting ? '导出中' : '导出'
            }}</span>
          </button>
        </div>
      </section>

      <Tabs :active-key="activePadTab" class="production-record-pad-tabs" @change="handlePadTabChange">
        <TabPane v-for="tab in PAD_TABS" :key="tab.key" :tab="tab.label" />
      </Tabs>

      <section class="package-fg-filter-bar production-record-query-panel">
        <div class="daily-date-query">
          <span>生产日期</span>
          <RangePicker v-model:value="queryDateRange" value-format="YYYY-MM-DD" allow-clear />
          <span class="daily-record-note">按生产日期汇总已确认报工，同一分段跨天分别显示。</span>
        </div>
        <div class="production-record-query-grid">
          <label>型号</label>
          <Input
            v-model:value="queryForm.modelCode"
            allow-clear
            placeholder="输入型号"
            @press-enter="handleSearch"
          />
          <label>生产批号</label>
          <Input
            v-model:value="queryForm.productionBatchNo"
            allow-clear
            placeholder="输入生产批号"
            @press-enter="handleSearch"
          />
          <label>裁切尺寸</label>
          <Select
            v-model:value="queryForm.cutSizeMm"
            allow-clear
            :options="SIZE_OPTIONS"
            placeholder="全部"
            @change="handleSearch"
          />
          <Button type="primary" @click="handleSearch"
            ><template #icon><IconifyIcon icon="lucide:search" /></template
            >查询</Button
          >
          <Button @click="handleReset"
            ><template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template
            >重置</Button
          >
        </div>
      </section>

      <div class="record-body">
        <Spin :spinning="loading">
          <div class="record-table-scroll">
            <table class="production-record-table">
              <thead>
                <tr>
                  <th>完工日期</th>
                  <th>型号</th>
                  <th>类型</th>
                  <th>生产批号</th>
                  <th>裁切尺寸(mm)</th>
                  <th>投入(pcs)</th>
                  <th>产出(pcs)</th>
                  <th>刀片累计裁切(pcs)</th>
                  <th>裁切片数累计(≤2000pcs)</th>
                  <th>毛毡累计使用天数(≤90天)</th>
                  <th>刀片更换原因</th>
                  <th>来源</th>
                  <th>记录人</th>
                  <th>备注</th>
                  <th v-if="canRevise">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in rows" :key="rowKey(row, index)">
                  <td><Tooltip :title="`生产归属日期：${displayText(row.reportDate)}`">{{ row.recordTime || `${displayText(row.reportDate)}（未记录具体时间）` }}</Tooltip></td>
                  <td>
                    <Tooltip :title="row.modelCode"
                      ><span>{{
                        compactText(row.modelCode, 16)
                      }}</span></Tooltip
                    >
                  </td>
                  <td class="is-center"><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td>
                  <td>
                    <Tooltip :title="row.productionBatchNo"
                      ><span>{{
                        compactText(row.productionBatchNo, 18)
                      }}</span></Tooltip
                    >
                  </td>
                  <td class="is-center">{{ displayText(row.cutSizeMm) }}</td>
                  <td class="is-number">{{ formatQty(row.inputQty) }}</td>
                  <td class="is-number">{{ formatQty(row.outputQty) }}</td>
                  <td class="is-number">
                    {{ displayText(row.bladeUseCount) }}
                  </td>
                  <td class="is-number">{{ displayText(row.feltUseCount) }}</td>
                  <td class="is-number">{{ displayText(row.feltUseDays) }}</td>
                  <td>
                    <Tooltip :title="row.bladeReplaceReason"
                      ><span>{{
                        compactText(row.bladeReplaceReason, 20)
                      }}</span></Tooltip
                    >
                  </td>
                  <td>{{ displayText(row.recordSource) }}</td>
                  <td>{{ displayText(row.recorderName) }}</td>
                  <td>
                    <Tooltip :title="row.remark"
                      ><span>{{ compactText(row.remark, 20) }}</span></Tooltip
                    >
                  </td>
                  <td v-if="canRevise">
                    <Button size="small" type="link" @click="openRevision(row)"
                      >修订</Button
                    >
                  </td>
                </tr>
                <tr v-if="rows.length === 0">
                  <td class="empty-cell" :colspan="canRevise ? 15 : 14">
                    暂无裁切报工生产记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Spin>
      </div>

      <footer class="record-pagination">
        <span>默认显示全部有效报工记录</span>
        <Pagination
          v-model:current="pageNo"
          v-model:page-size="pageSize"
          :page-size-options="['10', '20', '50', '100']"
          :show-total="(count) => `共 ${count} 条`"
          :total="total"
          show-size-changer
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </footer>
    </div>
    <ProductionRecordRevisionModal
      v-model:open="revisionOpen"
      :fields="REVISION_FIELDS"
      module-code="CUT_ROUND"
      :row="revisionRow"
      @saved="fetchData"
    />
  </Page>
</template>

<style scoped>
.daily-date-query {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.daily-record-note {
  font-size: 12px;
  color: #64748b;
}

.production-record-page {
  min-height: 0;
  overflow: hidden;
}
:global(.production-record-content) {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px !important;
  overflow: hidden !important;
}
.production-record-report {
  display: grid;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr) 48px;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.production-record-banner {
  min-height: 78px;
}
.production-record-action-group {
  flex-wrap: nowrap;
}
.production-record-action-group .action-tile {
  min-width: 66px;
}
.production-record-query-panel {
  box-sizing: border-box;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.production-record-query-grid {
  display: grid;
  grid-template-columns:
    54px minmax(140px, 1fr) 76px minmax(180px, 1fr)
    76px 110px 82px 82px;
  gap: 8px;
  align-items: stretch;
}
.production-record-query-grid > label {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}
.production-record-query-grid :deep(.ant-input-affix-wrapper),
.production-record-query-grid :deep(.ant-picker),
.production-record-query-grid :deep(.ant-select-selector),
.production-record-query-grid :deep(.ant-btn) {
  width: 100%;
  min-height: 34px;
  border-radius: 2px;
}
.record-body {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d2dae5;
}
.record-body :deep(.ant-spin-nested-loading),
.record-body :deep(.ant-spin-container) {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.record-table-scroll {
  width: 100%;
  height: 100%;
  overflow: auto;
  scrollbar-gutter: stable;
}
.production-record-table {
  width: 100%;
  min-width: 1960px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  color: #263241;
  font-size: 12px;
}
.production-record-table th,
.production-record-table td {
  height: 38px;
  padding: 6px 8px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-right: 1px solid #d2dae5;
  border-bottom: 1px solid #d2dae5;
}
.production-record-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #334155;
  font-weight: 900;
  text-align: center;
  background: #eef2f7;
}
.production-record-table tbody tr:nth-child(even) td {
  background: #f7f9fc;
}
.production-record-table tbody tr:hover td {
  background: #eef6ff;
}
.production-record-table .is-number {
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.production-record-table .is-center {
  text-align: center;
}
.empty-cell {
  height: 120px !important;
  color: #667085;
  text-align: center;
}
.record-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 8px 10px;
  color: #667085;
  font-size: 12px;
  background: #fff;
  border: 1px solid #d2dae5;
}
</style>
