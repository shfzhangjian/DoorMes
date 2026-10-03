<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcMassStockApi } from '#/api/mes/hc/plan/mass-stock';

import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Input, Modal, Switch, Table, Tag, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportMassStock,
  getGoodStockList,
  getMassStockPage,
  getShippingDetailList,
  updateMassStockManual,
} from '#/api/mes/hc/plan/mass-stock';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesHcPlanMassStock' });

type MassStock = MesHcMassStockApi.Stock;
type QueryState = Omit<MesHcMassStockApi.PageReqVO, 'pageNo' | 'pageSize'>;
type ShippingDetailMetric = MesHcMassStockApi.ShippingDetailMetric;
type LocationParts = {
  locationArea?: string;
  locationLayer?: string;
  locationPosition?: string;
  locationShelf?: string;
};
type StockPageResult = {
  list?: MassStock[];
  total?: number;
};

const advancedQueryVisible = ref(false);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const exportLoading = ref(false);
const goodStockLoading = ref(false);
const goodStockRows = ref<MesHcMassStockApi.GoodStock[]>([]);
const goodStockTitle = ref('');
const goodStockVisible = ref(false);
const manualSaving = ref(false);
const manualVisible = ref(false);
const pageRows = ref<MassStock[]>([]);
const pageTotal = ref(0);
const shippingDetailLoading = ref(false);
const shippingDetailRows = ref<MesHcMassStockApi.ShippingDetail[]>([]);
const shippingDetailTitle = ref('');
const shippingDetailVisible = ref(false);
let currentTimer: ReturnType<typeof setInterval> | null = null;
let queryFirstPageChain: Promise<void> = Promise.resolve();
let queryFirstPageSeq = 0;

const queryParams = reactive<QueryState>({
  keyword: '',
  modelCode: '',
  motherBatchNo: '',
  motherSegmentBatchNo: '',
  onlyAdhesive2: true,
});

const advancedQuery = reactive<QueryState>({ ...queryParams });

const manualForm = reactive<MesHcMassStockApi.ManualSaveReqVO>({
  modelCode: '',
  motherBatchNo: '',
  motherSegmentBatchNo: '',
  remark: '',
  semResult: '',
});

const resultStatusMeta: Record<string, { color: string; text: string }> = {
  FAIL: { color: 'red', text: '异常' },
  NG: { color: 'red', text: 'NG' },
  OK: { color: 'green', text: 'OK' },
  PASS: { color: 'green', text: '通过' },
  PENDING: { color: 'orange', text: '待判定' },
};

const pageGoodStockQty = computed(() =>
  pageRows.value.reduce((sum, row) => sum + Number(row.goodStockQty || 0), 0),
);

const pageShortageRows = computed(
  () => pageRows.value.filter((row) => Number(row.demandStockQty || 0) > Number(row.goodStockQty || 0)).length,
);

const goodStockTableRows = computed(() =>
  goodStockRows.value.map((row) => ({ ...row, ...splitLocationParts(row.locationName, row.locationCode) })),
);

const shippingDetailTableRows = computed(() =>
  shippingDetailRows.value.map((row) => ({ ...row, ...splitLocationParts(row.locationName, row.locationCode) })),
);

const activeQueryTags = computed(() => {
  const tags: Array<{ key: keyof QueryState; label: string; value: string }> = [];
  const addTag = (key: keyof QueryState, label: string, value?: boolean | string) => {
    if (value === undefined || value === null || value === '' || value === false) return;
    tags.push({ key, label, value: value === true ? '是' : String(value) });
  };
  addTag('keyword', '关键词', queryParams.keyword);
  addTag('modelCode', '型号', queryParams.modelCode);
  addTag('motherBatchNo', '母卷批号', queryParams.motherBatchNo);
  addTag('motherSegmentBatchNo', '母卷段号', queryParams.motherSegmentBatchNo);
  addTag('onlyAdhesive2', '只看有已粘胶2的', queryParams.onlyAdhesive2);
  return tags;
});

const quantityFormatter = ({ cellValue }: { cellValue?: number | string }) => formatQuantity(cellValue);
const meterFormatter = ({ cellValue }: { cellValue?: number | string }) => formatQuantity(cellValue, 3);
const dateFormatter = ({ cellValue }: { cellValue?: number | string }) => formatDate(cellValue);

const goodStockColumns = [
  { dataIndex: 'sliceBatchNo', ellipsis: true, title: '片号', width: 150 },
  { dataIndex: 'modelCode', ellipsis: true, title: '型号', width: 115 },
  { dataIndex: 'batchNo', ellipsis: true, title: '母卷段号', width: 130 },
  { align: 'right', dataIndex: 'qty', title: '数量', width: 80 },
  { dataIndex: 'locationShelf', ellipsis: true, title: '货架', width: 100 },
  { dataIndex: 'locationLayer', ellipsis: true, title: '层', width: 80 },
  { dataIndex: 'locationArea', ellipsis: true, title: '区', width: 80 },
  { dataIndex: 'locationPosition', ellipsis: true, title: '托盘位', width: 105 },
  { dataIndex: 'inboundTime', ellipsis: true, title: '入库时间', width: 160 },
];

const shippingDetailColumns = [
  { dataIndex: 'noticeNo', ellipsis: true, fixed: 'left', title: '需求单号', width: 145 },
  { dataIndex: 'customerName', ellipsis: true, title: '客户', width: 120 },
  { dataIndex: 'requiredBatchNo', ellipsis: true, title: '需求批号', width: 120 },
  { dataIndex: 'requiredSliceRange', ellipsis: true, title: '需求片号', width: 110 },
  {
    customRender: ({ record }: { record: MesHcMassStockApi.ShippingDetail }) =>
      textValue(record.customerProductBatchNo || record.batchNo),
    dataIndex: 'customerProductBatchNo',
    ellipsis: true,
    title: '客户片号',
    width: 130,
  },
  { dataIndex: 'actualSliceBatchNo', ellipsis: true, title: '实际片号', width: 145 },
  { dataIndex: 'shippingInspectionResult', ellipsis: true, title: '检验结果', width: 95 },
  { dataIndex: 'shippingInspectorName', ellipsis: true, title: '检验人', width: 95 },
  { dataIndex: 'shippingInspectionTime', ellipsis: true, title: '检验时间', width: 160 },
  { dataIndex: 'locationShelf', ellipsis: true, title: '货架', width: 100 },
  { dataIndex: 'locationLayer', ellipsis: true, title: '层', width: 80 },
  { dataIndex: 'locationArea', ellipsis: true, title: '区', width: 80 },
  { dataIndex: 'locationPosition', ellipsis: true, title: '托盘位', width: 105 },
  { dataIndex: 'remark', ellipsis: true, title: '备注', width: 180 },
];

const columns: VxeTableGridOptions<MassStock>['columns'] = [
  { fixed: 'left', type: 'seq', width: 50 },
  { field: 'modelCode', fixed: 'left', minWidth: 120, showOverflow: 'tooltip', title: '型号' },
  { field: 'motherBatchNo', fixed: 'left', minWidth: 135, showOverflow: 'tooltip', title: '母卷批号' },
  { field: 'motherSegmentBatchNo', fixed: 'left', minWidth: 150, showOverflow: 'tooltip', title: '母卷段号' },
  { align: 'right', field: 'wetMeter', formatter: meterFormatter, title: '湿法（m）', width: 105 },
  { align: 'right', field: 'grindingFirstMeter', formatter: meterFormatter, title: '一次磨皮（m）', width: 125 },
  { align: 'right', field: 'grindingSecondMeter', formatter: meterFormatter, title: '二次磨皮（m）', width: 125 },
  { align: 'center', field: 'hcNapResult', slots: { default: 'hcNapResult' }, title: 'HC-NAP层结果', width: 120 },
  { align: 'center', field: 'csNapResult', slots: { default: 'csNapResult' }, title: 'CS-NAP层结果', width: 120 },
  { align: 'center', field: 'shippingCoaResult', slots: { default: 'shippingCoaResult' }, title: '出货COA', width: 105 },
  { field: 'semResult', minWidth: 120, showOverflow: 'tooltip', slots: { default: 'semResult' }, title: 'SEM结果' },
  { align: 'right', field: 'adhesive1UnprocessedMeter', formatter: meterFormatter, title: '粘胶1（m）', width: 115 },
  { align: 'right', field: 'pressSlotQty', formatter: quantityFormatter, title: '压槽', width: 85 },
  { align: 'right', field: 'adhesive2Qty', formatter: quantityFormatter, title: '粘胶2', width: 85 },
  { align: 'right', field: 'cutRoundQty', formatter: quantityFormatter, title: '裁切', width: 85 },
  { align: 'right', field: 'pendingInspectionQty', formatter: quantityFormatter, title: '待检验', width: 95 },
  { align: 'right', field: 'qualifiedInspectionStockQty', formatter: quantityFormatter, title: '检验合格库存', width: 125 },
  { align: 'right', field: 'goodStockQty', slots: { default: 'goodStockQty' }, title: '良品库存', width: 100 },
  { align: 'center', field: 'earliestProductionDate', formatter: dateFormatter, title: '最早生产日期', width: 120 },
  { align: 'center', field: 'latestProductionDate', formatter: dateFormatter, title: '最晚生产日期', width: 120 },
  { align: 'right', field: 'demandStockQty', slots: { default: 'demandStockQty' }, title: '需备货数量', width: 115 },
  { align: 'right', field: 'shippingPickedQty', slots: { default: 'shippingPickedQty' }, title: '订单发货配货数量', width: 150 },
  { align: 'right', field: 'shippingInspectionQty', slots: { default: 'shippingInspectionQty' }, title: '订单检验数量', width: 125 },
  { field: 'remark', minWidth: 180, showOverflow: 'tooltip', slots: { default: 'remark' }, title: '备注' },
  { align: 'center', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 90 },
];

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'process-output-balance-vben-grid',
  gridClass: 'process-output-balance-vxe-grid',
  gridOptions: {
    border: true,
    columns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const pageSize = Number(page.pageSize || 20);
          const pageNo = Math.max(1, Number(page.currentPage || 1));
          try {
            const result = normalizePageResult(await getMassStockPage({
              ...buildQueryParams(),
              pageNo,
              pageSize,
            }));
            pageRows.value = result.list;
            pageTotal.value = result.total;
            return result;
          } catch (error) {
            message.warning(getRequestMessage(error) || '量产备货库存查询失败，已保留当前表格数据。');
            return { list: pageRows.value, total: pageTotal.value };
          }
        },
      },
      autoLoad: false,
    },
    rowConfig: {
      height: 46,
      isHover: true,
      keyField: 'rowKey',
    },
    toolbarConfig: { enabled: false },
  } as VxeTableGridOptions<MassStock>,
});

function buildQueryParams(): MesHcMassStockApi.PageReqVO {
  return { ...queryParams };
}

function getRequestMessage(error: unknown) {
  const data = (error as any)?.response?.data || (error as any)?.data || error || {};
  return String(
    data?.msg
      || data?.message
      || data?.data?.msg
      || data?.data?.message
      || (error as any)?.msg
      || (error as any)?.message
      || '',
  );
}

function normalizePageResult(value: unknown): Required<StockPageResult> {
  const result = value as StockPageResult;
  if (result && Array.isArray(result.list) && Number.isFinite(Number(result.total ?? 0))) {
    return {
      list: result.list || [],
      total: Number(result.total || 0),
    };
  }
  throw new Error(getRequestMessage(value) || '量产备货库存加载失败：后台返回的数据结构不正确。');
}

function queryFirstPage() {
  const seq = ++queryFirstPageSeq;
  queryFirstPageChain = queryFirstPageChain
    .catch(() => undefined)
    .then(async () => {
      if (seq !== queryFirstPageSeq) return;
      await nextTick();
      await (gridApi.grid as any)?.setCurrentPage?.(1);
      await gridApi.query();
    });
  return queryFirstPageChain;
}

function applyQuery() {
  void queryFirstPage();
}

function resetQuery() {
  Object.assign(queryParams, {
    keyword: '',
    modelCode: '',
    motherBatchNo: '',
    motherSegmentBatchNo: '',
    onlyAdhesive2: true,
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

function removeQueryCondition(key: keyof QueryState) {
  (queryParams[key] as any) = key === 'onlyAdhesive2' ? false : '';
  Object.assign(advancedQuery, queryParams);
  applyQuery();
}

function formatQuantity(value?: number | string, precision = 0) {
  if (value === null || value === undefined || value === '') return '-';
  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) return String(value);
  return numberValue.toFixed(precision);
}

function formatDate(value?: number | string) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value).slice(0, 10);
}

function statusMeta(status?: string) {
  const value = String(status || '').trim();
  if (!value) return { color: 'default', text: '-' };
  return resultStatusMeta[value] || { color: value.includes('/') ? 'blue' : 'default', text: value };
}

function textValue(value?: string) {
  return String(value || '').trim() || '-';
}

function normalizeLocationNamePart(value?: string) {
  return String(value || '').trim() || undefined;
}

function normalizeLocationCodePart(value?: string, suffix = '') {
  const text = String(value || '').trim();
  return text ? `${text}${suffix}` : undefined;
}

function splitLocationParts(locationName?: string, locationCode?: string): LocationParts {
  const nameParts = String(locationName || '')
    .split('-')
    .map((item) => item.trim())
    .filter(Boolean);
  const codeParts = String(locationCode || '')
    .split('-')
    .map((item) => item.trim())
    .filter(Boolean);
  return {
    locationArea: normalizeLocationNamePart(nameParts[2]) || normalizeLocationCodePart(codeParts[2], '区'),
    locationLayer: normalizeLocationNamePart(nameParts[1]) || normalizeLocationCodePart(codeParts[1], '层'),
    locationPosition:
      normalizeLocationNamePart(nameParts.slice(3).join('-')) || normalizeLocationCodePart(codeParts.slice(3).join('-'), '托盘位'),
    locationShelf: normalizeLocationNamePart(nameParts[0]) || normalizeLocationCodePart(codeParts[0], '#货架'),
  };
}

function shippingMetricLabel(metric: ShippingDetailMetric) {
  if (metric === 'demand') return '备货需求明细';
  if (metric === 'picked') return '配货明细';
  return '发货检验明细';
}

function shippingMetricQty(row: MassStock, metric: ShippingDetailMetric) {
  if (metric === 'demand') return Number(row.demandStockQty || 0);
  if (metric === 'picked') return Number(row.shippingPickedQty || 0);
  return Number(row.shippingInspectionQty || 0);
}

function openManual(row: MassStock) {
  manualForm.modelCode = row.modelCode || '';
  manualForm.motherBatchNo = row.motherBatchNo || '';
  manualForm.motherSegmentBatchNo = row.motherSegmentBatchNo || '';
  manualForm.semResult = row.semResult || '';
  manualForm.remark = row.remark || '';
  manualVisible.value = true;
}

async function handleExport() {
  exportLoading.value = true;
  try {
    const data = await exportMassStock(buildQueryParams());
    downloadFileFromBlobPart({ fileName: '量产备货库存.xls', source: data });
  } finally {
    exportLoading.value = false;
  }
}

async function openGoodStock(row: MassStock) {
  if (!Number(row.goodStockQty || 0)) return;
  goodStockTitle.value = `${row.modelCode || '-'} / ${row.motherSegmentBatchNo || '-'}`;
  goodStockVisible.value = true;
  goodStockLoading.value = true;
  try {
    goodStockRows.value = await getGoodStockList({
      modelCode: row.modelCode,
      motherBatchNo: row.motherBatchNo || '',
      motherSegmentBatchNo: row.motherSegmentBatchNo || '',
    });
  } finally {
    goodStockLoading.value = false;
  }
}

async function openShippingDetail(row: MassStock, metric: ShippingDetailMetric) {
  if (!shippingMetricQty(row, metric)) return;
  shippingDetailTitle.value = `${shippingMetricLabel(metric)} - ${row.modelCode || '-'} / ${row.motherSegmentBatchNo || '-'}`;
  shippingDetailVisible.value = true;
  shippingDetailLoading.value = true;
  try {
    shippingDetailRows.value = await getShippingDetailList({
      metric,
      modelCode: row.modelCode,
      motherBatchNo: row.motherBatchNo || '',
      motherSegmentBatchNo: row.motherSegmentBatchNo || '',
    });
  } finally {
    shippingDetailLoading.value = false;
  }
}

async function handleManualSave() {
  manualSaving.value = true;
  try {
    await updateMassStockManual({ ...manualForm });
    message.success('已保存');
    manualVisible.value = false;
    await gridApi.query();
  } finally {
    manualSaving.value = false;
  }
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  void queryFirstPage();
});

onBeforeUnmount(() => {
  if (currentTimer) clearInterval(currentTimer);
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console process-output-balance-console mass-stock-console">
      <section class="prototype-banner process-output-balance-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:boxes" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">量产备货库存</h2>
            <Tag color="processing" class="console-title-tag">计划排程</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ pageTotal }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页良品</span>
              <span class="console-meta-value">{{ formatQuantity(pageGoodStockQty) }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页缺口</span>
              <span class="console-meta-value">{{ pageShortageRows }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">过滤项</span>
              <span class="console-meta-value">{{ activeQueryTags.length }}</span>
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
          <button class="action-tile" type="button" :disabled="exportLoading" @click="handleExport">
            <IconifyIcon icon="lucide:file-down" />
            <span>导出</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-output-balance-query-panel">
        <div class="process-output-balance-simple-query mass-stock-simple-query">
          <label class="process-output-balance-query-label">查询</label>
          <Input
            v-model:value="queryParams.keyword"
            allow-clear
            placeholder="型号 / 母卷批号 / 母卷段号 / SEM / 备注"
            @press-enter="applyQuery"
          />
          <Button type="primary" @click="applyQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <div class="process-output-balance-switch">
            <span>只看有已粘胶2的</span>
            <Switch
              v-model:checked="queryParams.onlyAdhesive2"
              checked-children="是"
              un-checked-children="否"
              @change="applyQuery"
            />
          </div>
        </div>
        <div v-if="activeQueryTags.length > 0" class="process-output-balance-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="process-output-balance-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <div class="process-output-balance-body">
        <div class="process-output-balance-grid-host">
          <Grid>
            <template #hcNapResult="{ row }">
              <Tag :color="statusMeta(row.hcNapResult).color">{{ statusMeta(row.hcNapResult).text }}</Tag>
            </template>

            <template #csNapResult="{ row }">
              <Tag :color="statusMeta(row.csNapResult).color">{{ statusMeta(row.csNapResult).text }}</Tag>
            </template>

            <template #shippingCoaResult="{ row }">
              <Tag :color="statusMeta(row.shippingCoaResult).color">{{ statusMeta(row.shippingCoaResult).text }}</Tag>
            </template>

            <template #semResult="{ row }">
              <span>{{ textValue(row.semResult) }}</span>
            </template>

            <template #goodStockQty="{ row }">
              <button
                v-if="Number(row.goodStockQty || 0) > 0"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openGoodStock(row)"
              >
                {{ formatQuantity(row.goodStockQty) }}
              </button>
              <span v-else>-</span>
            </template>

            <template #demandStockQty="{ row }">
              <button
                v-if="Number(row.demandStockQty || 0) > 0"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openShippingDetail(row, 'demand')"
              >
                {{ formatQuantity(row.demandStockQty) }}
              </button>
              <span v-else>-</span>
            </template>

            <template #shippingPickedQty="{ row }">
              <button
                v-if="Number(row.shippingPickedQty || 0) > 0"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openShippingDetail(row, 'picked')"
              >
                {{ formatQuantity(row.shippingPickedQty) }}
              </button>
              <span v-else>-</span>
            </template>

            <template #shippingInspectionQty="{ row }">
              <button
                v-if="Number(row.shippingInspectionQty || 0) > 0"
                class="process-output-balance-qty-drill"
                type="button"
                @click.stop="openShippingDetail(row, 'inspection')"
              >
                {{ formatQuantity(row.shippingInspectionQty) }}
              </button>
              <span v-else>-</span>
            </template>

            <template #remark="{ row }">
              <span>{{ textValue(row.remark) }}</span>
            </template>

            <template #action="{ row }">
              <TableAction
                :actions="[
                  {
                    auth: ['mes:hc:mass-stock:update'],
                    icon: ACTION_ICON.EDIT,
                    label: '维护',
                    onClick: () => openManual(row),
                    type: 'link',
                  },
                ]"
              />
            </template>
          </Grid>
        </div>
      </div>

      <Modal v-model:open="advancedQueryVisible" title="多条件查询" width="760px" @ok="applyAdvancedQuery">
        <div class="process-output-balance-advanced-body">
          <div class="process-output-balance-advanced-grid">
            <label class="process-output-balance-query-item">
              <span>型号</span>
              <Input v-model:value="advancedQuery.modelCode" allow-clear placeholder="请输入型号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>母卷批号</span>
              <Input v-model:value="advancedQuery.motherBatchNo" allow-clear placeholder="请输入母卷批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>母卷段号</span>
              <Input v-model:value="advancedQuery.motherSegmentBatchNo" allow-clear placeholder="请输入母卷段号" />
            </label>
            <label class="process-output-balance-query-item mass-stock-switch-query-item">
              <span>只看有已粘胶2的</span>
              <div class="mass-stock-switch-control">
                <Switch
                  v-model:checked="advancedQuery.onlyAdhesive2"
                  checked-children="是"
                  un-checked-children="否"
                />
              </div>
            </label>
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="manualVisible"
        :confirm-loading="manualSaving"
        title="维护量产备货库存"
        width="520px"
        @ok="handleManualSave"
      >
        <div class="mass-stock-manual">
          <div class="mass-stock-manual__keys">
            <div>
              <span>型号</span>
              <strong>{{ manualForm.modelCode }}</strong>
            </div>
            <div>
              <span>母卷批号</span>
              <strong>{{ manualForm.motherBatchNo }}</strong>
            </div>
            <div>
              <span>母卷段号</span>
              <strong>{{ manualForm.motherSegmentBatchNo }}</strong>
            </div>
          </div>
          <label>
            <span>SEM结果</span>
            <Input v-model:value="manualForm.semResult" :maxlength="255" placeholder="请输入SEM结果" />
          </label>
          <label>
            <span>备注</span>
            <Input.TextArea
              v-model:value="manualForm.remark"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :maxlength="500"
              placeholder="请输入备注"
            />
          </label>
        </div>
      </Modal>

      <Modal v-model:open="goodStockVisible" :footer="null" :title="`良品库存明细 - ${goodStockTitle}`" width="940px">
        <Table
          :columns="goodStockColumns"
          :data-source="goodStockTableRows"
          :loading="goodStockLoading"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :row-key="(row) => row.id"
          :scroll="{ x: 1000 }"
          bordered
          size="small"
        />
      </Modal>

      <Modal v-model:open="shippingDetailVisible" :footer="null" :title="shippingDetailTitle" width="1100px">
        <Table
          :columns="shippingDetailColumns"
          :data-source="shippingDetailTableRows"
          :loading="shippingDetailLoading"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :row-key="(row) => `${row.sourceType || 'DETAIL'}-${row.id}`"
          :scroll="{ x: 1665 }"
          bordered
          size="small"
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.mass-stock-console {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.process-output-balance-banner {
  min-height: 78px;
  max-height: 90px;
}

.process-output-balance-action-group {
  flex-wrap: nowrap;
}

.process-output-balance-action-group .action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.48;
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
  align-items: stretch;
  min-width: 0;
}

.mass-stock-simple-query {
  grid-template-columns: 86px minmax(260px, 1fr) 86px 106px 190px;
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

.process-output-balance-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
  border: 1px solid #c6d3df;
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

.process-output-balance-query-item :deep(.ant-input-affix-wrapper),
.process-output-balance-query-item :deep(.ant-select-selector) {
  min-height: 34px;
  border: 0;
  border-radius: 0;
}

.mass-stock-switch-control {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  background: #fff;
}

.mass-stock-manual {
  display: grid;
  gap: 14px;
}

.mass-stock-manual__keys {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.mass-stock-manual__keys div {
  min-width: 0;
  padding: 8px;
  background: rgb(246 248 250);
  border: 1px solid rgb(229 231 235);
  border-radius: 6px;
}

.mass-stock-manual span {
  display: block;
  margin-bottom: 4px;
  color: rgb(100 116 139);
  font-size: 12px;
}

.mass-stock-manual strong {
  display: block;
  overflow: hidden;
  color: rgb(15 23 42);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .mass-stock-simple-query {
    grid-template-columns: 72px minmax(220px, 1fr) 82px 102px 138px;
  }
}

@media (max-width: 960px) {
  .mass-stock-simple-query {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .mass-stock-simple-query :deep(.ant-btn),
  .process-output-balance-switch {
    grid-column: span 2;
  }

  .process-output-balance-query-tags {
    padding-left: 0;
  }

  .process-output-balance-advanced-grid,
  .mass-stock-manual__keys {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
