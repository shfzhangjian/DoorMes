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
  exportAdhesive1ProductionRecord,
  exportAdhesive2ProductionRecord,
  getAdhesive1ProductionRecordPage,
  getAdhesive2ProductionRecordPage,
} from '#/api/mes/hc/production-record';

import ProductionRecordRevisionModal, {
  type ProductionRecordRevisionField,
} from '../../components/ProductionRecordRevisionModal.vue';

import '../../../../package-fg/shared/cut-round-board.css';
import '../../../../package-fg/shared/production-record-ledger.css';

type ProcessType = 'ADHESIVE1' | 'ADHESIVE2';

const props = defineProps<{ processType: ProcessType }>();

const config = computed(() => {
  const adhesive2 = props.processType === 'ADHESIVE2';
  return {
    emptyText: `暂无粘胶${adhesive2 ? '2' : '1'}生产或研发消耗记录`,
    fileName: `粘胶${adhesive2 ? '2' : '1'}生产记录表.xlsx`,
    icon: adhesive2 ? 'lucide:layers-3' : 'lucide:panels-top-left',
    inputLabel: adhesive2 ? '粘胶2投入(pcs)' : '投入米数(m)',
    outputLabel: adhesive2 ? '粘胶2产出(pcs)' : '产出米数(m)',
    processName: `粘胶${adhesive2 ? '2' : '1'}`,
    title: `粘胶${adhesive2 ? '2' : '1'}生产记录表`,
    unit: adhesive2 ? 'pcs' : 'm',
  };
});

const RangePicker = DatePicker.RangePicker;
const queryDateRange = ref<[string, string] | null>(null);

const loading = ref(false);
const exporting = ref(false);
const { hasAccessByCodes } = useAccess();
const rows = ref<MesHcProductionRecordApi.AdhesiveRecordRow[]>([]);
const revisionOpen = ref(false);
const revisionRow = ref<MesHcProductionRecordApi.AdhesiveRecordRow>();
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
  materialCode: '',
  modelCode: '',
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
});

const canRevise = computed(() =>
  hasAccessByCodes([
    props.processType === 'ADHESIVE2'
      ? 'mes:pp:adhesive2-production-record:revise'
      : 'mes:pp:adhesive1-production-record:revise',
  ]),
);
const REVISION_FIELDS: ProductionRecordRevisionField[] = [
  { key: 'reportDate', label: '日期', type: 'date' },
  { key: 'modelCode', label: '型号', type: 'text' },
  { key: 'materialCode', label: '料号', type: 'text' },
  { key: 'batchNo', label: '批号', type: 'text' },
  { key: 'inputQty', label: '投入数量', type: 'number' },
  { key: 'outputQty', label: '产出数量', type: 'number' },
  { key: 'glueBoardMaterialCode', label: '胶板料号', type: 'text' },
  { key: 'glueBoardBatchNo', label: '胶板批号', type: 'text' },
  { key: 'glueBoardConsumeQty', label: '胶板消耗量', type: 'number' },
  { key: 'recorderName', label: '记录人', type: 'text' },
  { key: 'recordTime', label: '记录时间', type: 'datetime' },
  { key: 'remark', label: '备注', type: 'text' },
];

const pageInput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.inputQty || 0), 0),
);
const pageOutput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.outputQty || 0), 0),
);

function filters(extra: Record<string, any> = {}) {
  return {
    reportDateStart: queryDateRange.value?.[0] || undefined,
    reportDateEnd: queryDateRange.value?.[1] || undefined,
    batchNo: query.batchNo || undefined,
    materialCode: query.materialCode || undefined,
    modelCode: query.modelCode || undefined,
    padType: query.padType || undefined,
    ...extra,
  };
}

async function load() {
  loading.value = true;
  try {
    const params = filters({ pageNo: pageNo.value, pageSize: pageSize.value });
    const result =
      props.processType === 'ADHESIVE2'
        ? await getAdhesive2ProductionRecordPage(params)
        : await getAdhesive1ProductionRecordPage(params);
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
  queryDateRange.value = null;
  query.batchNo = '';
  query.materialCode = '';
  query.modelCode = '';
  query.padType = '';
  activePadTab.value = 'ALL';
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
    const params = filters();
    const data =
      props.processType === 'ADHESIVE2'
        ? await exportAdhesive2ProductionRecord(params)
        : await exportAdhesive1ProductionRecord(params);
    downloadFileFromBlobPart({ fileName: config.value.fileName, source: data });
    message.success(`${config.value.title}已导出`);
  } finally {
    exporting.value = false;
  }
}

function openRevision(row: MesHcProductionRecordApi.AdhesiveRecordRow) {
  revisionRow.value = row;
  revisionOpen.value = true;
}

function text(value?: null | number | string, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function productionDateText(row: MesHcProductionRecordApi.AdhesiveRecordRow) {
  return row.recordTime || `${text(row.reportDate)}（未记录具体时间）`;
}

function numberText(value?: number) {
  if (value === null || value === undefined) return '-';
  return Number(value).toLocaleString('zh-CN', {
    maximumFractionDigits: props.processType === 'ADHESIVE2' ? 0 : 3,
  });
}

function glueBoardConsumeText(value?: number) {
  if (value === null || value === undefined) return '-';
  return Number(value).toLocaleString('zh-CN', {
    maximumFractionDigits: 3,
  });
}

function compact(value?: string, maxLength = 20) {
  const content = text(value, '');
  return content.length > maxLength
    ? `${content.slice(0, maxLength)}...`
    : content || '-';
}

function rowKey(
  row: MesHcProductionRecordApi.AdhesiveRecordRow,
  index: number,
) {
  return [
    row.reportDate,
    row.modelCode,
    row.materialCode,
    row.batchNo,
    row.glueBoardMaterialCode,
    row.glueBoardBatchNo,
    row.recordSource,
    index,
  ].join('|');
}

onMounted(load);
</script>

<template>
  <Page
    auto-content-height
    class="page"
    content-class="adhesive-production-record-content"
  >
    <div class="package-fg-console report">
      <section class="prototype-banner banner">
        <span class="console-main-icon">
          <IconifyIcon :icon="config.icon" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">{{ config.title }}</h2>
            <Tag class="console-title-tag" color="processing">生产报表</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">当前页</span>
              <span class="console-meta-value">{{ rows.length }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">行</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">投入</span>
              <span class="console-meta-value">{{
                numberText(pageInput)
              }}</span>
              <span class="console-meta-sub">{{ config.unit }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">产出</span>
              <span class="console-meta-value">{{
                numberText(pageOutput)
              }}</span>
              <span class="console-meta-sub">{{ config.unit }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group action-group">
          <button class="action-tile" type="button" @click="load">
            <IconifyIcon icon="lucide:refresh-cw" /><span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            :disabled="exporting"
            @click="exportExcel"
          >
            <IconifyIcon icon="lucide:file-spreadsheet" />
            <span>{{ exporting ? '导出中' : '导出' }}</span>
          </button>
        </div>
      </section>

      <Tabs :active-key="activePadTab" class="production-record-pad-tabs" @change="handlePadTabChange">
        <TabPane v-for="tab in PAD_TABS" :key="tab.key" :tab="tab.label" />
      </Tabs>

      <section class="package-fg-filter-bar query-panel">
        <div class="daily-date-query">
          <span>生产日期</span>
          <RangePicker v-model:value="queryDateRange" value-format="YYYY-MM-DD" allow-clear />
          <span class="daily-record-note">按生产日期汇总已确认报工，同一分段跨天分别显示。 原汇总修订保留，不自动带入每日记录。</span>
        </div>
        <div class="query-grid">
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
          <Button type="primary" @click="search">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="reset">
            <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
            重置
          </Button>
        </div>
      </section>

      <div class="body">
        <Spin :spinning="loading">
          <div class="scroll">
            <table class="table">
              <thead>
                <tr>
                  <th class="production-date">完工日期</th>
                  <th>型号</th>
                  <th>类型</th>
                  <th>料号</th>
                  <th>批号</th>
                  <th>{{ config.inputLabel }}</th>
                  <th>{{ config.outputLabel }}</th>
                  <th>胶板料号</th>
                  <th>胶板批号</th>
                  <th>胶板消耗量(m)</th>
                  <th>数据来源</th>
                  <th>记录人</th>
                  <th>备注</th>
                  <th v-if="canRevise">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in rows" :key="rowKey(row, index)">
                  <td class="production-date"><Tooltip :title="`生产归属日期：${text(row.reportDate)}`">{{ productionDateText(row) }}</Tooltip></td>
                  <td>{{ text(row.modelCode) }}</td>
                  <td><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td>
                  <td>{{ text(row.materialCode) }}</td>
                  <td>
                    <Tooltip :title="row.batchNo">
                      <span>{{ compact(row.batchNo) }}</span>
                    </Tooltip>
                  </td>
                  <td class="number">{{ numberText(row.inputQty) }}</td>
                  <td class="number">{{ numberText(row.outputQty) }}</td>
                  <td>{{ text(row.glueBoardMaterialCode) }}</td>
                  <td>
                    <Tooltip :title="row.glueBoardBatchNo">
                      <span>{{ compact(row.glueBoardBatchNo) }}</span>
                    </Tooltip>
                  </td>
                  <td class="number">
                    {{ glueBoardConsumeText(row.glueBoardConsumeQty) }}
                  </td>
                  <td>{{ text(row.recordSource) }}</td>
                  <td>{{ text(row.recorderName) }}</td>
                  <td>
                    <Tooltip :title="row.remark">
                      <span>{{ compact(row.remark) }}</span>
                    </Tooltip>
                  </td>
                  <td v-if="canRevise">
                    <Button size="small" type="link" @click="openRevision(row)"
                      >修订</Button
                    >
                  </td>
                </tr>
                <tr v-if="!rows.length">
                  <td class="empty" :colspan="canRevise ? 14 : 13">
                    {{ config.emptyText }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Spin>
      </div>

      <footer class="pagination">
        <span>默认显示有效量产报工及研发样品消耗记录</span>
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
      :module-code="props.processType"
      :row="revisionRow"
      @saved="load"
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

.page {
  min-height: 0;
  overflow: hidden;
}
:global(.adhesive-production-record-content) {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px !important;
  overflow: hidden !important;
}
.report {
  display: grid;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr) 48px;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.banner {
  min-height: 78px;
}
.action-group {
  flex-wrap: nowrap;
}
.action-group .action-tile {
  min-width: 66px;
}
.query-panel {
  box-sizing: border-box;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.query-grid {
  display: grid;
  grid-template-columns:
    54px minmax(130px, 1fr) 54px minmax(150px, 1fr)
    54px minmax(170px, 1fr) 82px 82px;
  gap: 8px;
  align-items: stretch;
}
.query-grid > label {
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
.query-grid :deep(.ant-input-affix-wrapper),
.query-grid :deep(.ant-btn) {
  width: 100%;
  min-height: 34px;
  border-radius: 2px;
}
.body {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d2dae5;
}
.body :deep(.ant-spin-nested-loading),
.body :deep(.ant-spin-container) {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.scroll {
  width: 100%;
  height: 100%;
  overflow: auto;
  scrollbar-gutter: stable;
}
.table {
  width: 100%;
  min-width: 1560px;
  color: #263241;
  font-size: 12px;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
}
.table th,
.table td {
  height: 38px;
  padding: 6px 8px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-right: 1px solid #d2dae5;
  border-bottom: 1px solid #d2dae5;
}
.table th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #334155;
  font-weight: 900;
  text-align: center;
  background: #eef2f7;
}
.production-date {
  width: 190px;
}
.table tbody tr:nth-child(even) td {
  background: #f7f9fc;
}
.table tbody tr:hover td {
  background: #eef6ff;
}
.number {
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.empty {
  height: 120px !important;
  color: #667085;
  text-align: center;
}
.pagination {
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
@media (max-width: 1360px) {
  .query-grid {
    grid-template-columns: 54px 1fr 54px 1fr 54px 1fr 72px 72px;
  }
}
</style>
