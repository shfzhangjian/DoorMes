<script lang="ts" setup>
import type { MesHcProductionRecordApi } from '#/api/mes/hc/production-record';

import { computed, onMounted, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
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
  exportSlittingPressProductionRecord,
  getSlittingPressProductionRecordPage,
} from '#/api/mes/hc/production-record';

import ProductionRecordRevisionModal, {
  type ProductionRecordRevisionField,
} from '../components/ProductionRecordRevisionModal.vue';

import '../../../package-fg/shared/cut-round-board.css';
import '../../../package-fg/shared/production-record-ledger.css';

import RndConsumeForm from './modules/rnd-consume-form.vue';

const [RndConsumeModal, rndConsumeModalApi] = useVbenModal({
  connectedComponent: RndConsumeForm,
  destroyOnClose: true,
});
const canRegisterRnd = computed(() =>
  hasAccessByCodes(['mes:md:press-slot-spare:update']),
);

const RangePicker = DatePicker.RangePicker;
const queryDateRange = ref<[string, string] | null>(null);

const loading = ref(false);
const exporting = ref(false);
const { hasAccessByCodes } = useAccess();
const rows = ref<MesHcProductionRecordApi.SlittingPressRecordRow[]>([]);
const revisionOpen = ref(false);
const revisionRow = ref<MesHcProductionRecordApi.SlittingPressRecordRow>();
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
  modelCode: '',
  padType: '' as '' | 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED',
});

const canRevise = computed(() =>
  hasAccessByCodes(['mes:pp:slitting-press-production-record:revise']),
);
const REVISION_FIELDS: ProductionRecordRevisionField[] = [
  { key: 'reportDate', label: '日期', type: 'date' },
  { key: 'modelCode', label: '型号', type: 'text' },
  { key: 'materialCode', label: '料号', type: 'text' },
  { key: 'batchNo', label: '批号', type: 'text' },
  { key: 'slittingInputM', label: '分切投入(m)', type: 'number' },
  { key: 'slittingOutputPcs', label: '分切确认产出(pcs)', type: 'integer' },
  { key: 'slittingNgPcs', label: '分切NG(pcs)', type: 'integer' },
  {
    key: 'pressSlotActualInputPcs',
    label: '压槽实际投入(pcs)',
    type: 'number',
  },
  {
    key: 'pressSlotActualOutputPcs',
    label: '压槽实际产出(pcs)',
    type: 'number',
  },
  { key: 'pressSlotOutputPcs', label: '压槽合格产出(pcs)', type: 'number' },
  {
    key: 'rollerCleanAccumulatedPcs',
    label: '压槽清洗累计片数',
    type: 'integer',
  },
  { key: 'rollerCleanUseDays', label: '压槽辊累计使用天数', type: 'integer' },
  {
    key: 'bearingReplaceAccumulatedPcs',
    label: '轴承更换累计片数',
    type: 'integer',
  },
  { key: 'bearingReplaceUseDays', label: '轴承累计使用天数', type: 'integer' },
  { key: 'recorderName', label: '记录人', type: 'text' },
  { key: 'recordTime', label: '记录时间', type: 'datetime' },
  { key: 'remark', label: '备注', type: 'text' },
];

const slittingInput = computed(() =>
  rows.value.some((row) => row.slittingInputM == null)
    ? undefined
    : rows.value.reduce((sum, row) => sum + Number(row.slittingInputM), 0),
);
const slittingConfirmedOutput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.slittingOutputPcs || 0), 0),
);
const slittingNg = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.slittingNgPcs || 0), 0),
);
const pressActualInput = computed(() =>
  rows.value.reduce(
    (sum, row) => sum + Number(row.pressSlotActualInputPcs || 0),
    0,
  ),
);
const pressActualOutput = computed(() =>
  rows.value.reduce(
    (sum, row) => sum + Number(row.pressSlotActualOutputPcs || 0),
    0,
  ),
);
const pressQualifiedOutput = computed(() =>
  rows.value.reduce((sum, row) => sum + Number(row.pressSlotOutputPcs || 0), 0),
);

function filters(extra: Record<string, any> = {}) {
  return {
    reportDateStart: queryDateRange.value?.[0] || undefined,
    reportDateEnd: queryDateRange.value?.[1] || undefined,
    batchNo: query.batchNo || undefined,
    modelCode: query.modelCode || undefined,
    padType: query.padType || undefined,
    ...extra,
  };
}

async function load() {
  loading.value = true;
  try {
    const result = await getSlittingPressProductionRecordPage(
      filters({ pageNo: pageNo.value, pageSize: pageSize.value }),
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
  queryDateRange.value = null;
  query.batchNo = '';
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
    const data = await exportSlittingPressProductionRecord(filters());
    downloadFileFromBlobPart({
      fileName: '分切&压槽生产记录表.xlsx',
      source: data,
    });
    message.success('分切&压槽生产记录表已导出');
  } finally {
    exporting.value = false;
  }
}

function openRevision(row: MesHcProductionRecordApi.SlittingPressRecordRow) {
  revisionRow.value = row;
  revisionOpen.value = true;
}

function text(value?: null | number | string, fallback = '-') {
  return String(value ?? '').trim() || fallback;
}

function numberText(value?: number, digits = 3) {
  return value === null || value === undefined
    ? '-'
    : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: digits });
}

function compact(value?: string, maxLength = 18) {
  const content = text(value, '');
  return content.length > maxLength
    ? `${content.slice(0, maxLength)}...`
    : content || '-';
}

function rowKey(
  row: MesHcProductionRecordApi.SlittingPressRecordRow,
  index: number,
) {
  return [
    row.reportDate,
    row.modelCode,
    row.materialCode,
    row.batchNo,
    index,
  ].join('|');
}

onMounted(load);
</script>

<template>
  <Page
    auto-content-height
    class="page"
    content-class="slitting-press-record-content"
  >
    <div class="package-fg-console report">
      <section class="prototype-banner banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:between-horizontal-start" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">分切&压槽生产记录表</h2>
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
              ><span class="console-meta-label">分切</span
              ><span class="console-meta-value">{{
                numberText(slittingInput)
              }}</span
              ><span class="console-meta-sub"
                >m / {{ numberText(slittingConfirmedOutput, 0) }}确认 /
                {{ numberText(slittingNg, 0) }}NG</span
              ></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">压槽</span
              ><span class="console-meta-value">{{
                numberText(pressActualInput)
              }}</span
              ><span class="console-meta-sub"
                >实际投入 / {{ numberText(pressActualOutput) }}实产 /
                {{ numberText(pressQualifiedOutput) }}合格</span
              ></span
            >
          </div>
        </div>
        <div class="console-action-group action-group">
          <button v-if="canRegisterRnd" class="action-tile" type="button" @click="rndConsumeModalApi.open()">
            <IconifyIcon icon="lucide:plus" /><span>研发消耗登记</span>
          </button>
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

      <section class="package-fg-filter-bar query-panel">
        <div class="daily-date-query">
          <span>生产日期</span>
          <RangePicker v-model:value="queryDateRange" value-format="YYYY-MM-DD" allow-clear />
          <span class="daily-record-note">按生产日期汇总已确认报工，同一分段跨天分别显示。 原汇总修订保留，不自动带入每日记录。</span>
        </div>
        <div class="query-grid">
          <label>型号</label
          ><Input
            v-model:value="query.modelCode"
            allow-clear
            placeholder="输入型号"
            @press-enter="search"
          />
          <label>批号</label
          ><Input
            v-model:value="query.batchNo"
            allow-clear
            placeholder="输入批号"
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

      <div class="body">
        <Spin :spinning="loading">
          <div class="scroll">
            <table class="table">
              <thead>
                <tr>
                  <th class="completion-date">完工日期</th>
                  <th>型号</th>
                  <th>类型</th>
                  <th>批号</th>
                  <th>分切投入(m)</th>
                  <th>分切确认产出(pcs)</th>
                  <th>分切NG(pcs)</th>
                  <th>压槽实际投入(pcs)</th>
                  <th>压槽实际产出(pcs)</th>
                  <th>压槽合格产出(pcs)</th>
                  <th>压槽清洗累计片数</th>
                  <th>压槽辊累计使用天数</th>
                  <th>轴承更换累计片数</th>
                  <th>轴承累计使用天数</th>
                  <th>来源</th>
                  <th>记录人</th>
                  <th>备注</th>
                  <th v-if="canRevise">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in rows" :key="rowKey(row, index)">
                  <td class="completion-date">
                    <Tooltip :title="`生产归属日期：${text(row.reportDate)}`">{{ row.recordTime || `${text(row.reportDate)}（未记录具体时间）` }}</Tooltip>
                  </td>
                  <td>{{ text(row.modelCode) }}</td>
                  <td><Tag :color="padTypeColor(row.padType)">{{ padTypeLabel(row.padType) }}</Tag></td>
                  <td>
                    <Tooltip :title="row.batchNo"
                      ><span>{{ compact(row.batchNo) }}</span></Tooltip
                    >
                  </td>
                  <td class="number">{{ numberText(row.slittingInputM) }}</td>
                  <td class="number">
                    {{ numberText(row.slittingOutputPcs, 0) }}
                  </td>
                  <td class="number">{{ numberText(row.slittingNgPcs, 0) }}</td>
                  <td class="number">
                    {{ numberText(row.pressSlotActualInputPcs) }}
                  </td>
                  <td class="number">
                    {{ numberText(row.pressSlotActualOutputPcs) }}
                  </td>
                  <td class="number">
                    {{ numberText(row.pressSlotOutputPcs) }}
                  </td>
                  <td class="number">
                    {{ text(row.rollerCleanAccumulatedPcs) }}
                  </td>
                  <td class="number">{{ text(row.rollerCleanUseDays) }}</td>
                  <td class="number">
                    {{ text(row.bearingReplaceAccumulatedPcs) }}
                  </td>
                  <td class="number">{{ text(row.bearingReplaceUseDays) }}</td>
                  <td>{{ text(row.recordSource) }}</td>
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
                  <td class="empty" :colspan="canRevise ? 18 : 17">
                    暂无分切&压槽生产记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Spin>
      </div>

      <footer class="pagination">
        <span>默认显示全部有效报工记录</span>
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
    <RndConsumeModal @success="load" />
    <ProductionRecordRevisionModal
      v-model:open="revisionOpen"
      :fields="REVISION_FIELDS"
      module-code="SLITTING_PRESS"
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
:global(.slitting-press-record-content) {
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
    54px minmax(160px, 1fr) 54px minmax(180px, 1fr)
    82px 82px;
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
.query-grid :deep(.ant-picker),
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
  min-width: 2050px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  color: #263241;
  font-size: 12px;
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
.completion-date {
  width: 180px;
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
</style>
