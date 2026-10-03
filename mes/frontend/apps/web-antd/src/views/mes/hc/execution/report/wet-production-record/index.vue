<script lang="ts" setup>
import type { MesHcWetProductionRecordApi } from '#/api/mes/hc/wetproductionrecord';

import { computed, onMounted, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
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
  exportWetProductionRecord,
  getWetProductionRecordPage,
} from '#/api/mes/hc/wetproductionrecord';

import ProductionRecordRevisionModal, {
  type ProductionRecordRevisionField,
} from '../../../plan/production-record/components/ProductionRecordRevisionModal.vue';

import '../../../package-fg/shared/cut-round-board.css';
import '../../../package-fg/shared/production-record-ledger.css';

const YES_NO_OPTIONS = [
  { label: '全部', value: undefined },
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' },
];

const loading = ref(false);
const exporting = ref(false);
const { hasAccessByCodes } = useAccess();
const rows = ref<MesHcWetProductionRecordApi.WetProductionRecord[]>([]);
const revisionOpen = ref(false);
const revisionRow = ref<MesHcWetProductionRecordApi.WetProductionRecord>();
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
  batchNo: '',
  changeDesc: '',
  guideClothChanged: undefined as string | undefined,
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
});

const canRevise = computed(() =>
  hasAccessByCodes(['mes:sfc:wet-production-record:revise']),
);
const REVISION_FIELDS: ProductionRecordRevisionField[] = [
  { key: 'recordDate', label: '日期', type: 'date' },
  { key: 'modelCode', label: '型号', type: 'text' },
  { key: 'materialCode', label: '料号', type: 'text' },
  { key: 'batchNo', label: '批次', type: 'text' },
  { key: 'inputKg', label: '投入(kg)', type: 'number' },
  { key: 'outputMeter', label: '产出(m)', type: 'number' },
  { key: 'petModel', label: 'PET型号', type: 'text' },
  { key: 'petBatchNo', label: 'PET批号', type: 'text' },
  { key: 'guideClothBatchNo', label: '导布批号', type: 'text' },
  { key: 'guideClothUseCount', label: '导布累计使用次数', type: 'integer' },
  {
    key: 'guideClothChanged',
    label: '导布更换',
    type: 'select',
    options: [
      { label: '是', value: 'Y' },
      { label: '否', value: 'N' },
    ],
  },
  { key: 'changeDesc', label: '更换说明', type: 'text' },
  { key: 'recorderName', label: '记录人', type: 'text' },
  { key: 'recordTime', label: '实际完工时间', type: 'datetime' },
  { key: 'remark', label: '备注', type: 'text' },
];

const currentInput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.inputKg || 0), 0),
);
const currentOutput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.outputMeter || 0), 0),
);

function buildQueryParams(extra: Record<string, any> = {}) {
  return {
    batchNo: queryForm.batchNo || undefined,
    changeDesc: queryForm.changeDesc || undefined,
    guideClothChanged: queryForm.guideClothChanged || undefined,
    padType: queryForm.padType || undefined,
    ...extra,
  };
}

async function fetchData() {
  loading.value = true;
  try {
    const result = await getWetProductionRecordPage(
      buildQueryParams({ pageNo: pageNo.value, pageSize: pageSize.value }),
    );
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  void fetchData();
}

function handleReset() {
  queryForm.batchNo = '';
  queryForm.changeDesc = '';
  queryForm.guideClothChanged = undefined;
  queryForm.padType = '';
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
    const data = await exportWetProductionRecord(buildQueryParams());
    downloadFileFromBlobPart({ fileName: '湿法生产记录表.xlsx', source: data });
    message.success('湿法生产记录表已导出');
  } finally {
    exporting.value = false;
  }
}

function openRevision(row: MesHcWetProductionRecordApi.WetProductionRecord) {
  revisionRow.value = row;
  revisionOpen.value = true;
}

function yesNoText(value?: string) {
  return value === 'Y' ? '是' : '否';
}

function displayText(value?: null | number | string, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function formatNumber(value?: number) {
  if (value === undefined || value === null) return '-';
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

function compactText(value?: string, maxLength = 18) {
  const text = displayText(value, '');
  return text.length > maxLength
    ? `${text.slice(0, maxLength)}...`
    : text || '-';
}

function rowKey(
  row: MesHcWetProductionRecordApi.WetProductionRecord,
  index: number,
) {
  return [
    row.recordDate,
    row.modelCode,
    row.materialCode,
    row.batchNo,
    row.petBatchNo,
    row.guideClothBatchNo,
    index,
  ].join('|');
}

onMounted(fetchData);
</script>

<template>
  <Page
    auto-content-height
    class="wet-production-record-page"
    content-class="wet-production-record-content"
  >
    <div class="package-fg-console wet-production-record-report">
      <section class="prototype-banner wet-production-record-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:droplets" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">湿法生产记录表</h2>
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
                formatNumber(currentInput)
              }}</span
              ><span class="console-meta-sub">kg</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">产出</span
              ><span class="console-meta-value">{{
                formatNumber(currentOutput)
              }}</span
              ><span class="console-meta-sub">m</span></span
            >
          </div>
        </div>
        <div class="console-action-group wet-production-record-action-group">
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

      <section class="package-fg-filter-bar wet-production-record-query-panel">
        <div class="wet-production-record-simple-query">
          <label>批号</label
          ><Input
            v-model:value="queryForm.batchNo"
            allow-clear
            placeholder="批次、PET或导布批号"
            @press-enter="handleSearch"
          />
          <label>导布更换</label
          ><Select
            v-model:value="queryForm.guideClothChanged"
            allow-clear
            :options="YES_NO_OPTIONS"
            placeholder="全部"
            @change="handleSearch"
          />
          <label>更换说明</label
          ><Input
            v-model:value="queryForm.changeDesc"
            allow-clear
            placeholder="输入更换说明"
            @press-enter="handleSearch"
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
            <table class="wet-production-record-table">
              <thead>
                <tr>
                  <th class="completion-date">完工日期</th>
                  <th>型号</th>
                  <th>类型</th>
                  <th>料号</th>
                  <th>批次</th>
                  <th>投入(kg)</th>
                  <th>产出(m)</th>
                  <th>PET型号</th>
                  <th class="pet-batch-no">PET批号</th>
                  <th>导布批号</th>
                  <th>导布累计使用次数</th>
                  <th>导布更换</th>
                  <th>更换说明</th>
                  <th>记录人</th>
                  <th>备注</th>
                  <th v-if="canRevise">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in rows" :key="rowKey(row, index)">
                  <td class="completion-date">
                    {{ displayText(row.recordTime || row.recordDate) }}
                  </td>
                  <td>{{ displayText(row.modelCode) }}</td>
                  <td class="is-center"><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td>
                  <td>{{ displayText(row.materialCode) }}</td>
                  <td>
                    <Tooltip :title="row.batchNo"
                      ><span>{{ compactText(row.batchNo) }}</span></Tooltip
                    >
                  </td>
                  <td class="is-number">{{ formatNumber(row.inputKg) }}</td>
                  <td class="is-number">{{ formatNumber(row.outputMeter) }}</td>
                  <td>{{ displayText(row.petModel) }}</td>
                  <td class="pet-batch-no">
                    {{ displayText(row.petBatchNo) }}
                  </td>
                  <td>{{ displayText(row.guideClothBatchNo) }}</td>
                  <td class="is-number">
                    {{ displayText(row.guideClothUseCount) }}
                  </td>
                  <td class="is-center">
                    <Tag
                      :color="
                        row.guideClothChanged === 'Y' ? 'orange' : 'default'
                      "
                      >{{ yesNoText(row.guideClothChanged) }}</Tag
                    >
                  </td>
                  <td>
                    <Tooltip :title="row.changeDesc"
                      ><span>{{
                        compactText(row.changeDesc, 20)
                      }}</span></Tooltip
                    >
                  </td>
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
                  <td class="empty-cell" :colspan="canRevise ? 16 : 15">
                    暂无湿法报工生产记录
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
      module-code="WET"
      :row="revisionRow"
      @saved="fetchData"
    />
  </Page>
</template>

<style scoped>
.wet-production-record-page {
  min-height: 0;
  overflow: hidden;
}
:global(.wet-production-record-content) {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px !important;
  overflow: hidden !important;
}
.wet-production-record-report {
  display: grid;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr) 48px;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.wet-production-record-banner {
  min-height: 78px;
}
.wet-production-record-action-group {
  flex-wrap: nowrap;
}
.wet-production-record-action-group .action-tile {
  min-width: 66px;
}
.wet-production-record-query-panel {
  box-sizing: border-box;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.wet-production-record-simple-query {
  display: grid;
  grid-template-columns:
    54px minmax(180px, 1fr) 76px 110px
    76px minmax(180px, 1fr) 82px 82px;
  gap: 8px;
  align-items: stretch;
}
.wet-production-record-simple-query > label {
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
.wet-production-record-simple-query :deep(.ant-input-affix-wrapper),
.wet-production-record-simple-query :deep(.ant-picker),
.wet-production-record-simple-query :deep(.ant-select-selector),
.wet-production-record-simple-query :deep(.ant-btn) {
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
.wet-production-record-table {
  width: 100%;
  min-width: 1900px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  color: #263241;
  font-size: 12px;
}
.wet-production-record-table th,
.wet-production-record-table td {
  height: 38px;
  padding: 6px 8px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-right: 1px solid #d2dae5;
  border-bottom: 1px solid #d2dae5;
}
.wet-production-record-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #334155;
  font-weight: 900;
  text-align: center;
  background: #eef2f7;
}
.completion-date {
  width: 180px;
}
.pet-batch-no {
  width: 240px;
}
.wet-production-record-table tbody tr:nth-child(even) td {
  background: #f7f9fc;
}
.wet-production-record-table tbody tr:hover td {
  background: #eef6ff;
}
.wet-production-record-table .is-number {
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.wet-production-record-table .is-center {
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
