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
  Spin,
  TabPane,
  Tabs,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  exportFormulaProductionRecord,
  getFormulaProductionRecordPage,
} from '#/api/mes/hc/production-record';

import ProductionRecordRevisionModal, {
  type ProductionRecordRevisionField,
} from '../components/ProductionRecordRevisionModal.vue';

import '../../../package-fg/shared/cut-round-board.css';
import '../../../package-fg/shared/production-record-ledger.css';

defineOptions({ name: 'MesHcFormulaProductionRecord' });

const loading = ref(false);
const exporting = ref(false);
const { hasAccessByCodes } = useAccess();
const rows = ref<MesHcProductionRecordApi.FormulaRecordRow[]>([]);
const revisionOpen = ref(false);
const revisionRow = ref<MesHcProductionRecordApi.FormulaRecordRow>();
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
const query = reactive({
  batchNo: '',
  filterBatchNo: '',
  materialCode: '',
  modelCode: '',
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
  recorderName: '',
  reportDateRange: [] as string[],
});

const canRevise = computed(() =>
  hasAccessByCodes(['mes:pp:formula-production-record:revise']),
);
const REVISION_FIELDS: ProductionRecordRevisionField[] = [
  { key: 'reportDate', label: '日期', type: 'date' },
  { key: 'modelCode', label: '型号', type: 'text' },
  { key: 'materialCode', label: '料号', type: 'text' },
  { key: 'batchNo', label: '批号', type: 'text' },
  { key: 'filterBatchNo', label: '滤网批号', type: 'text' },
  { key: 'inputWeight', label: '投料重量(kg)', type: 'number' },
  { key: 'outputWeight', label: '产出重量(kg)', type: 'number' },
  { key: 'mixerEquipmentCode', label: '搅拌机机台编号', type: 'text' },
  { key: 'batchingTankNo', label: '配料罐罐号', type: 'text' },
  { key: 'foamingEquipmentCode', label: '脱泡机机台编号', type: 'text' },
  { key: 'defoamingTankNo', label: '脱泡罐罐号', type: 'text' },
  { key: 'recorderName', label: '记录人', type: 'text' },
  { key: 'recordTime', label: '实际完工时间', type: 'datetime' },
  { key: 'remark', label: '备注', type: 'text' },
];

const currentInputWeight = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.inputWeight || 0), 0),
);
const currentOutputWeight = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.outputWeight || 0), 0),
);

function buildQuery(extra: Record<string, any> = {}) {
  return {
    batchNo: query.batchNo || undefined,
    filterBatchNo: query.filterBatchNo || undefined,
    materialCode: query.materialCode || undefined,
    modelCode: query.modelCode || undefined,
    padType: query.padType || undefined,
    recorderName: query.recorderName || undefined,
    reportDateEnd: query.reportDateRange[1] || undefined,
    reportDateStart: query.reportDateRange[0] || undefined,
    ...extra,
  };
}

async function load() {
  loading.value = true;
  try {
    const result = await getFormulaProductionRecordPage(
      buildQuery({ pageNo: pageNo.value, pageSize: pageSize.value }),
    );
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  pageNo.value = 1;
  void load();
}

function reset() {
  query.batchNo = '';
  query.filterBatchNo = '';
  query.materialCode = '';
  query.modelCode = '';
  query.padType = '';
  activePadTab.value = 'ALL';
  query.recorderName = '';
  query.reportDateRange = [];
  search();
}

function handlePadTabChange(tab: string | number) {
  const value = String(tab);
  activePadTab.value = value;
  query.padType = value === 'ALL' ? '' : value as 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
  search();
}

function padTypeLabel(padType?: string) {
  return padType === 'BLACK_PAD' ? '黑垫' : padType === 'WHITE_PAD' ? '白垫' : '未归类';
}

function padTypeColor(padType?: string) {
  return padType === 'BLACK_PAD' ? 'default' : padType === 'WHITE_PAD' ? 'blue' : 'warning';
}

function changePage(nextPageNo: number, nextPageSize: number) {
  pageNo.value = nextPageNo;
  pageSize.value = nextPageSize;
  void load();
}

async function exportExcel() {
  exporting.value = true;
  try {
    const data = await exportFormulaProductionRecord(buildQuery());
    downloadFileFromBlobPart({
      fileName: '配料生产记录表.xlsx',
      source: data,
    });
    message.success('配料生产记录表已导出');
  } finally {
    exporting.value = false;
  }
}

function openRevision(row: MesHcProductionRecordApi.FormulaRecordRow) {
  revisionRow.value = row;
  revisionOpen.value = true;
}

function text(value?: null | number | string, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function numberText(value?: number) {
  return value === null || value === undefined
    ? '-'
    : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

function compact(value?: string, maxLength = 18) {
  const content = text(value, '');
  return content.length > maxLength
    ? `${content.slice(0, maxLength)}...`
    : content || '-';
}

function rowKey(row: MesHcProductionRecordApi.FormulaRecordRow, index: number) {
  return row.id || `${row.reportDate}|${row.batchNo}|${index}`;
}

onMounted(load);
</script>

<template>
  <Page
    auto-content-height
    class="formula-production-record-page"
    content-class="formula-production-record-content"
  >
    <div class="package-fg-console formula-production-record-report">
      <section class="prototype-banner formula-production-record-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:flask-conical" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">配料生产记录表</h2>
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
              ><span class="console-meta-label">投料</span
              ><span class="console-meta-value">{{
                numberText(currentInputWeight)
              }}</span
              ><span class="console-meta-sub">kg</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">产出</span
              ><span class="console-meta-value">{{
                numberText(currentOutputWeight)
              }}</span
              ><span class="console-meta-sub">kg</span></span
            >
          </div>
        </div>
        <div
          class="console-action-group formula-production-record-action-group"
        >
          <button class="action-tile" type="button" @click="load">
            <IconifyIcon icon="lucide:refresh-cw" /><span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="exporting"
            @click="exportExcel"
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

      <section
        class="package-fg-filter-bar formula-production-record-query-panel"
      >
        <div class="formula-production-record-simple-query">
          <label>日期</label>
          <DatePicker.RangePicker
            v-model:value="query.reportDateRange"
            value-format="YYYY-MM-DD"
          />
          <label>型号</label>
          <Input
            v-model:value="query.modelCode"
            allow-clear
            placeholder="输入型号"
            @press-enter="search"
          />
          <label>料号</label>
          <Input
            v-model:value="query.materialCode"
            allow-clear
            placeholder="输入料号"
            @press-enter="search"
          />
          <label>批号</label>
          <Input
            v-model:value="query.batchNo"
            allow-clear
            placeholder="输入批号"
            @press-enter="search"
          />
          <label>滤网批号</label>
          <Input
            v-model:value="query.filterBatchNo"
            allow-clear
            placeholder="输入滤网批号"
            @press-enter="search"
          />
          <label>记录人</label>
          <Input
            v-model:value="query.recorderName"
            allow-clear
            placeholder="输入记录人"
            @press-enter="search"
          />
          <Button type="primary" @click="search"
            ><template #icon><IconifyIcon icon="lucide:search" /></template
            >查询</Button
          >
          <Button @click="reset"
            ><template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template
            >重置</Button
          >
        </div>
      </section>

      <div class="record-body">
        <Spin :spinning="loading">
          <div class="record-table-scroll">
            <table class="formula-production-record-table">
              <thead>
                <tr>
                  <th class="record-date">日期</th>
                  <th>型号</th>
                  <th>类型</th>
                  <th>料号</th>
                  <th>批号</th>
                  <th>滤网批号</th>
                  <th>投料重量(kg)</th>
                  <th>产出重量(kg)</th>
                  <th>搅拌机机台编号</th>
                  <th>配料罐罐号</th>
                  <th>脱泡机机台编号</th>
                  <th>脱泡罐罐号</th>
                  <th>记录人</th>
                  <th>备注</th>
                  <th v-if="canRevise">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in rows" :key="rowKey(row, index)">
                  <td class="record-date">{{ text(row.reportDate) }}</td>
                  <td>{{ text(row.modelCode) }}</td>
                  <td><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td>
                  <td>{{ text(row.materialCode) }}</td>
                  <td>
                    <Tooltip :title="row.batchNo"
                      ><span>{{ compact(row.batchNo) }}</span></Tooltip
                    >
                  </td>
                  <td>{{ text(row.filterBatchNo) }}</td>
                  <td class="is-number">{{ numberText(row.inputWeight) }}</td>
                  <td class="is-number">{{ numberText(row.outputWeight) }}</td>
                  <td>{{ text(row.mixerEquipmentCode) }}</td>
                  <td>{{ text(row.batchingTankNo) }}</td>
                  <td>{{ text(row.foamingEquipmentCode) }}</td>
                  <td>{{ text(row.defoamingTankNo) }}</td>
                  <td>{{ text(row.recorderName) }}</td>
                  <td>
                    <Tooltip :title="row.remark"
                      ><span>{{ compact(row.remark, 20) }}</span></Tooltip
                    >
                  </td>
                  <td v-if="canRevise">
                    <Button size="small" type="link" @click="openRevision(row)"
                      >修订</Button
                    >
                  </td>
                </tr>
                <tr v-if="!rows.length">
                  <td class="empty-cell" :colspan="canRevise ? 15 : 14">
                    暂无配料生产记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Spin>
      </div>

      <footer class="record-pagination">
        <span>展示全部有效的配料 END 报工记录</span>
        <Pagination
          v-model:current="pageNo"
          v-model:page-size="pageSize"
          :page-size-options="['10', '20', '50', '100']"
          :show-total="(count) => `共 ${count} 条`"
          :total="total"
          show-size-changer
          @change="changePage"
          @show-size-change="changePage"
        />
      </footer>
    </div>
    <ProductionRecordRevisionModal
      v-model:open="revisionOpen"
      :fields="REVISION_FIELDS"
      module-code="FORMULA"
      :row="revisionRow"
      @saved="load"
    />
  </Page>
</template>

<style scoped>
.formula-production-record-page {
  min-height: 0;
  overflow: hidden;
}

:global(.formula-production-record-content) {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px !important;
  overflow: hidden !important;
}

.formula-production-record-report {
  display: grid;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr) 48px;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.formula-production-record-banner {
  min-height: 78px;
}

.formula-production-record-action-group {
  flex-wrap: nowrap;
}

.formula-production-record-action-group .action-tile {
  min-width: 66px;
}

.formula-production-record-query-panel {
  box-sizing: border-box;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.formula-production-record-simple-query {
  display: grid;
  grid-template-columns:
    48px minmax(210px, 1.5fr) repeat(4, 48px minmax(130px, 1fr))
    60px minmax(130px, 1fr) 82px 82px;
  gap: 8px;
  align-items: stretch;
}

.formula-production-record-simple-query > label {
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

.formula-production-record-simple-query :deep(.ant-input-affix-wrapper),
.formula-production-record-simple-query :deep(.ant-picker),
.formula-production-record-simple-query :deep(.ant-btn) {
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

.formula-production-record-table {
  width: 100%;
  min-width: 1820px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  color: #263241;
  font-size: 12px;
}

.formula-production-record-table th,
.formula-production-record-table td {
  height: 38px;
  padding: 6px 8px;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
  border-right: 1px solid #d2dae5;
  border-bottom: 1px solid #d2dae5;
}

.formula-production-record-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #334155;
  font-weight: 900;
  background: #eef2f7;
}

.formula-production-record-table tbody tr:nth-child(even) td {
  background: #f7f9fc;
}

.formula-production-record-table tbody tr:hover td {
  background: #eef6ff;
}

.record-date {
  width: 130px;
}

.formula-production-record-table .is-number {
  text-align: center;
  font-variant-numeric: tabular-nums;
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

@media (max-width: 1600px) {
  .formula-production-record-simple-query {
    grid-template-columns: repeat(3, 48px minmax(150px, 1fr)) 82px 82px;
  }
}
</style>
