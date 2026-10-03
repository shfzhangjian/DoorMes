<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcNgInventoryApi } from '#/api/mes/hc/ng-inventory';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getNgLocationGrid,
  getNgPieceLabels,
  getNgPiecePage,
  manualOutboundNgPieces,
  markNgPieceLabelsPrinted,
} from '#/api/mes/hc/ng-inventory';

import { padTypeNameOf } from '../../base/pad-type-options';
import { buildSlittingTransferTicketPayload } from '../../execution/report/shared/slittingTransferTicketPrint';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesNgStockQuery' });

type StockLedger = MesHcNgInventoryApi.NgPiece;
type NgLocation = MesHcNgInventoryApi.NgLocationGrid;
type StockQuery = {
  keyword: string;
  locationKey: string;
  materialCode: string;
  modelNo: string;
  padType: string;
  processType: string;
  shelvedDateEnd: string;
  shelvedDateStart: string;
  status: string;
  warehouseCode: string;
};
type QueryFieldKey = Exclude<keyof StockQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: Array<{ label: string; value: string }>;
  type?: 'date' | 'location' | 'warehouse';
};
type QueryTemplate = {
  name: string;
  values: Partial<StockQuery>;
};

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:ng-stock-query:query-templates';
const PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PRINT_BATCH_MAX_COUNT = 50;

const rows = ref<StockLedger[]>([]);
const total = ref(0);
const locations = ref<NgLocation[]>([]);
const selectedRowMap = ref<Map<number, StockLedger>>(new Map());
const loadingLocations = ref(false);
const advancedQueryVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string | undefined>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());
const outboundVisible = ref(false);
const outboundReason = ref('');
const outboundLoading = ref(false);
const printLoading = ref(false);
const query = reactive<StockQuery>({
  keyword: '',
  locationKey: '',
  materialCode: '',
  modelNo: '',
  padType: '',
  processType: '',
  shelvedDateEnd: '',
  shelvedDateStart: '',
  status: '',
  warehouseCode: '',
});

const processTypeOptions = [
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
];
const stockStatusOptions = [
  { label: '正常在库', value: 'STORED' },
  { label: '冻结在库', value: 'FROZEN' },
];
const padTypeOptions = [
  { label: '黑垫', value: 'BLACK_PAD' },
  { label: '白垫', value: 'WHITE_PAD' },
];
const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'warehouseCode', label: '仓库', type: 'warehouse' },
  { key: 'locationKey', label: '固定库', type: 'location' },
  { key: 'processType', label: '工序', options: processTypeOptions },
  { key: 'status', label: '库存状态', options: stockStatusOptions },
  { key: 'padType', label: '垫型', options: padTypeOptions },
  { key: 'modelNo', label: '型号' },
  { key: 'materialCode', label: '料号' },
  { key: 'shelvedDateStart', label: '入库日期起', type: 'date' },
  { key: 'shelvedDateEnd', label: '入库日期止', type: 'date' },
];

const warehouseOptions = computed(() => {
  const optionMap = new Map<string, string>();
  locations.value.forEach((item) => {
    if (!item.warehouseCode) return;
    optionMap.set(
      item.warehouseCode,
      item.warehouseName
        ? `${item.warehouseName}（${item.warehouseCode}）`
        : item.warehouseCode,
    );
  });
  return Array.from(optionMap, ([value, label]) => ({ label, value }));
});
const locationOptions = computed(() =>
  locations.value
    .filter(
      (item) =>
        !query.warehouseCode || item.warehouseCode === query.warehouseCode,
    )
    .map((item) => {
      const warehouseText =
        item.warehouseName || item.warehouseCode || '未命名仓库';
      const rackText = '';
      const locationText = item.locationName || item.locationCode;
      return {
        label: [warehouseText, rackText, locationText]
          .filter(Boolean)
          .join(' · '),
        value: item.locationKey,
      };
    }),
);
const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);
const activeQueryTags = computed(() =>
  queryFieldConfigs
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const selectedRows = computed(() => [...selectedRowMap.value.values()]);
const selectedCount = computed(() => selectedRows.value.length);
const canOutbound = computed(
  () =>
    selectedRows.value.length > 0 &&
    selectedRows.value.every((row) =>
      ['FROZEN', 'STORED'].includes(String(row.status || '')),
    ),
);
const pageWarehouseCount = computed(
  () =>
    new Set(rows.value.map((item) => item.currentWarehouseName).filter(Boolean))
      .size,
);
const pageLocationCount = computed(
  () =>
    new Set(rows.value.map((item) => item.currentLocationKey).filter(Boolean))
      .size,
);
const pagePieceQty = computed(() =>
  rows.value.reduce(
    (totalQty, item) => totalQty + Number(item.pieceQty || 0),
    0,
  ),
);

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(
      QUERY_TEMPLATE_STORAGE_KEY,
    );
    const parsedTemplates = rawTemplates ? JSON.parse(rawTemplates) : [];
    if (!Array.isArray(parsedTemplates)) return [];
    return parsedTemplates
      .filter((item) => item?.name && item?.values)
      .map((item) => ({
        name: String(item.name),
        values: item.values,
      })) as QueryTemplate[];
  } catch {
    return [];
  }
}

function persistQueryTemplates() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(
    QUERY_TEMPLATE_STORAGE_KEY,
    JSON.stringify(queryTemplates.value),
  );
}

function getQueryFieldConfig(key: QueryFieldKey) {
  return queryFieldConfigs.find((item) => item.key === key);
}

function getQueryFieldText(key: QueryFieldKey) {
  const value = query[key].trim();
  if (!value) return '';
  const config = getQueryFieldConfig(key);
  if (config?.type === 'warehouse') {
    return (
      warehouseOptions.value.find((item) => item.value === value)?.label ||
      value
    );
  }
  if (config?.type === 'location') {
    return (
      locationOptions.value.find((item) => item.value === value)?.label || value
    );
  }
  return config?.options?.find((item) => item.value === value)?.label || value;
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  query[key] = value || '';
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
  selectedTemplateName.value = undefined;
  queryTemplateName.value = '';
}

function snapshotQuery() {
  const values: QueryTemplate['values'] = {};
  if (query.keyword.trim()) values.keyword = query.keyword.trim();
  queryFieldConfigs.forEach((config) => {
    const value = query[config.key].trim();
    if (value) values[config.key] = value;
  });
  return values;
}

function openAdvancedQuery() {
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  handleSearch();
}

function removeQueryCondition(key: QueryFieldKey) {
  setQueryFieldValue(key);
  selectedTemplateName.value = undefined;
  handleSearch();
}

function saveQueryTemplate() {
  const name = queryTemplateName.value.trim();
  if (!name) {
    message.warning('请填写查询模板名称');
    return;
  }
  const values = snapshotQuery();
  if (Object.keys(values).length === 0) {
    message.warning('请先填写查询条件');
    return;
  }
  const nextTemplates = queryTemplates.value.filter(
    (item) => item.name !== name,
  );
  nextTemplates.unshift({ name, values });
  queryTemplates.value = nextTemplates.slice(0, 20);
  selectedTemplateName.value = name;
  persistQueryTemplates();
  message.success('查询模板已保存');
}

function loadQueryTemplate(name?: string) {
  if (!name) return;
  const template = queryTemplates.value.find((item) => item.name === name);
  if (!template) return;
  query.keyword = '';
  clearAdvancedQueryValues();
  Object.entries(template.values).forEach(([key, value]) => {
    if (key === 'keyword') {
      query.keyword = value || '';
      return;
    }
    if (queryFieldConfigs.some((item) => item.key === key)) {
      setQueryFieldValue(key as QueryFieldKey, value);
    }
  });
  queryTemplateName.value = template.name;
  handleSearch();
}

function handleLoadQueryTemplate(value?: number | string) {
  loadQueryTemplate(value ? String(value) : undefined);
}

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, number | string> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  (Object.keys(query) as Array<keyof StockQuery>).forEach((key) => {
    const value = query[key].trim();
    if (value) params[key] = value;
  });
  return params;
}

function clearSelectedRows() {
  selectedRowMap.value = new Map();
}

function toggleRowSelection(row: StockLedger, checked: boolean) {
  const nextMap = new Map(selectedRowMap.value);
  if (checked) nextMap.set(row.id, row);
  else nextMap.delete(row.id);
  selectedRowMap.value = nextMap;
}

function togglePageSelection(checked: boolean) {
  const nextMap = new Map(selectedRowMap.value);
  rows.value.forEach((row) => {
    if (checked) nextMap.set(row.id, row);
    else nextMap.delete(row.id);
  });
  selectedRowMap.value = nextMap;
}

function isAllPageSelected() {
  return (
    rows.value.length > 0 &&
    rows.value.every((row) => selectedRowMap.value.has(row.id))
  );
}

function isSomePageSelected() {
  return rows.value.some((row) => selectedRowMap.value.has(row.id));
}

async function handleSearch() {
  clearSelectedRows();
  await gridApi.query();
}

function handleReset() {
  query.keyword = '';
  clearAdvancedQuery();
  advancedQueryVisible.value = false;
  handleSearch();
}

function statusColor(value?: string) {
  if (value === 'STORED') return 'blue';
  if (value === 'FROZEN') return 'purple';
  return 'default';
}

function statusText(value?: string) {
  if (value === 'STORED') return '正常在库';
  if (value === 'FROZEN') return '冻结在库';
  return value || '-';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function ngPiecePrintTicketId(pieceId: number) {
  return `ng-piece-${pieceId}`;
}

async function sendNgPieceLabelsToAgent(
  labels: MesHcNgInventoryApi.NgPieceLabel[],
) {
  const printTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  const payload = await buildSlittingTransferTicketPayload(
    labels.map((label) => ({
      hideMaterialCodeWhenEmpty: true,
      materialCode: label.materialCode,
      modelCode: label.modelCode,
      planNo: label.planNo,
      processName: label.processName,
      recorderName: label.recorderName,
      segmentBatchNo: label.segmentBatchNo,
      sliceSerialNo: label.pieceNo,
      ticketId: ngPiecePrintTicketId(label.pieceId),
      workTime: label.workTime
        ? String(label.workTime).replace('T', ' ')
        : printTime,
    })),
    printTime,
  );
  const endpoint =
    labels.length > 1 ? '/print/transfer-tickets' : '/print/transfer-ticket';
  const response = await fetch(`${PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return result;
}

async function printSelectedRows() {
  const targets = [...selectedRows.value];
  if (targets.length === 0) {
    message.warning('请先勾选需要打印的不合格品');
    return;
  }
  printLoading.value = true;
  let acceptedCount = 0;
  const acceptedPieceIds = new Set<number>();
  const failures: string[] = [];
  try {
    for (
      let start = 0;
      start < targets.length;
      start += PRINT_BATCH_MAX_COUNT
    ) {
      const batchTargets = targets.slice(start, start + PRINT_BATCH_MAX_COUNT);
      const labelResult = await getNgPieceLabels(
        batchTargets.map((row) => row.id),
      );
      const labels = labelResult.labels || [];
      failures.push(...(labelResult.failures || []));
      if (labels.length === 0) continue;

      const result = await sendNgPieceLabelsToAgent(labels);
      const itemResults = Array.isArray(result?.itemResults)
        ? result.itemResults
        : [];
      const acceptedLabels = labels.filter((label) => {
        const itemResult = itemResults.find(
          (item: any) => item?.ticketId === ngPiecePrintTicketId(label.pieceId),
        );
        return itemResult
          ? itemResult.success === true && itemResult.accepted !== false
          : result?.success === true;
      });
      itemResults
        .filter(
          (item: any) => item?.success !== true || item?.accepted === false,
        )
        .forEach((item: any) =>
          failures.push(
            item?.message || `标签 ${item?.ticketId || '-'} 未进入打印队列`,
          ),
        );
      if (acceptedLabels.length === 0) continue;

      await markNgPieceLabelsPrinted(
        acceptedLabels.map((label) => ({
          labelContentJson: JSON.stringify(label),
          pieceId: label.pieceId,
          printerName: result?.printerName || 'slitting',
        })),
      );
      acceptedLabels.forEach((label) => acceptedPieceIds.add(label.pieceId));
      acceptedCount += acceptedLabels.length;
    }

    if (acceptedCount === 0) {
      throw new Error(failures[0] || '本机打印服务未确认任何标签进入打印队列');
    }
    const nextMap = new Map(selectedRowMap.value);
    acceptedPieceIds.forEach((pieceId) => nextMap.delete(pieceId));
    selectedRowMap.value = nextMap;
    const failedCount = targets.length - acceptedCount;
    if (failedCount > 0 || failures.length > 0) {
      Modal.warning({
        content: `已打印并完成审计 ${acceptedCount} 张；另有 ${failedCount} 张未完成。${failures[0] || ''}`,
        title: '不合格品标签批量打印部分完成',
      });
    } else {
      message.success(`已打印并完成审计 ${acceptedCount} 张不合格品标签`);
    }
  } catch (error: any) {
    const nextMap = new Map(selectedRowMap.value);
    acceptedPieceIds.forEach((pieceId) => nextMap.delete(pieceId));
    selectedRowMap.value = nextMap;
    Modal.warning({
      content:
        acceptedCount > 0
          ? `已有 ${acceptedCount} 张标签完成打印及审计；${error?.message || error}`
          : `未能完成不合格品标签打印：${error?.message || error}。请确认 HC-MES-PrintAgent 已启动且分切标签打印机配置正确。`,
      title:
        acceptedCount > 0
          ? '不合格品标签批量打印部分完成'
          : '不合格品标签打印失败',
    });
  } finally {
    printLoading.value = false;
  }
}

function openOutboundModal() {
  if (!canOutbound.value) {
    message.warning('请先勾选正常在库或冻结在库的不合格品');
    return;
  }
  outboundReason.value = '';
  outboundVisible.value = true;
}

async function confirmOutbound() {
  const reason = outboundReason.value.trim();
  if (!reason) {
    message.warning('请填写出库原因');
    return;
  }
  if (!canOutbound.value) {
    message.warning('所选不合格品状态已不支持出库，请刷新后重试');
    return;
  }
  const targets = [...selectedRows.value];
  outboundLoading.value = true;
  try {
    await manualOutboundNgPieces({
      pieceIds: targets.map((row) => row.id),
      reason,
    });
    message.success(`已完成 ${targets.length} 条不合格品出库`);
    outboundVisible.value = false;
    clearSelectedRows();
    await gridApi.query();
  } finally {
    outboundLoading.value = false;
  }
}

async function loadLocations() {
  loadingLocations.value = true;
  try {
    locations.value = await getNgLocationGrid();
  } catch {
    message.warning('库位筛选项加载失败，仍可使用其他条件查询');
  } finally {
    loadingLocations.value = false;
  }
}

const columns = [
  {
    field: 'selected',
    fixed: 'left',
    slots: { default: 'selection', header: 'selectionHeader' },
    title: '选择',
    width: 62,
  },
  {
    field: 'currentWarehouseName',
    showOverflow: 'tooltip',
    title: '仓库',
    width: 160,
  },
  {
    field: 'currentLocationName',
    showOverflow: 'tooltip',
    title: '固定库',
    width: 180,
  },
  { field: 'processName', title: '工序', width: 90 },
  {
    field: 'status',
    slots: { default: 'status' },
    title: '库存状态',
    width: 110,
  },
  {
    field: 'padType',
    formatter: ({ row }: { row: StockLedger }) => padTypeNameOf(row.padType),
    title: '垫型',
    width: 90,
  },
  { field: 'materialCode', showOverflow: 'tooltip', title: '料号', width: 140 },
  { field: 'modelNo', showOverflow: 'tooltip', title: '型号', width: 140 },
  {
    field: 'sourceParentBatchNo',
    showOverflow: 'tooltip',
    title: '母批 / 段批次',
    width: 180,
  },
  {
    field: 'sourceBatchNo',
    showOverflow: 'tooltip',
    title: '来源批号',
    width: 180,
  },
  { field: 'pieceNo', showOverflow: 'tooltip', title: '片号', width: 180 },
  { field: 'pieceQty', title: '数量', width: 90 },
  {
    field: 'defectSummary',
    minWidth: 220,
    showOverflow: 'tooltip',
    title: 'NG原因',
  },
  {
    field: 'shelvedTime',
    formatter: ({ row }: { row: StockLedger }) =>
      formatDateTime(row.shelvedTime),
    title: '入库时间',
    width: 170,
  },
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
          const result = await getNgPiecePage(buildQueryParams(page));
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
  } as VxeTableGridOptions<StockLedger>,
});

onMounted(() => {
  void loadLocations();
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console ng-stock-query-board">
      <section class="prototype-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:package-search" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">分切压槽不合格品库存查询</h2>
            <Tag class="console-title-tag" color="volcano">在库逐片台账</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">在库总数</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">片</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页仓库</span>
              <span class="console-meta-value">{{ pageWarehouseCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页仓库</span>
              <span class="console-meta-value">{{ pageLocationCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页数量</span>
              <span class="console-meta-value">{{ pagePieceQty }}</span>
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
          <button
            v-access:code="['mes:inv:ng-stock-query:print']"
            class="action-tile"
            :disabled="selectedCount === 0 || printLoading"
            type="button"
            @click="printSelectedRows"
          >
            <IconifyIcon icon="lucide:printer" />
            <span>{{
              printLoading
                ? '打印中...'
                : `打印${selectedCount > 0 ? `（${selectedCount}）` : ''}`
            }}</span>
          </button>
          <button
            v-access:code="['mes:inv:ng-stock-query:outbound']"
            class="action-tile"
            :disabled="!canOutbound || outboundLoading"
            type="button"
            @click="openOutboundModal"
          >
            <IconifyIcon icon="lucide:log-out" />
            <span>
              出库{{ selectedCount > 0 ? `（${selectedCount}）` : '' }}
            </span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar stock-query-panel">
        <div class="stock-simple-query">
          <label class="stock-simple-query-label">关键词</label>
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
          <Button @click="openAdvancedQuery">
            <template #icon>
              <IconifyIcon icon="lucide:sliders-horizontal" />
            </template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            :options="queryTemplateOptions"
            allow-clear
            class="stock-query-template-select"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="stock-query-tags">
          <span
            v-for="tag in activeQueryTags"
            :key="tag.key"
            class="stock-query-tag"
          >
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">
              x
            </button>
          </span>
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="920px"
        wrap-class-name="stock-advanced-query-modal"
      >
        <div class="stock-advanced-query-body">
          <div class="stock-advanced-query-grid">
            <div class="stock-query-item">
              <label>仓库</label>
              <Select
                v-model:value="query.warehouseCode"
                :loading="loadingLocations"
                :options="warehouseOptions"
                allow-clear
                placeholder="全部仓库"
              />
            </div>
            <div class="stock-query-item">
              <label>固定库</label>
              <Select
                v-model:value="query.locationKey"
                :loading="loadingLocations"
                :options="locationOptions"
                allow-clear
                show-search
                placeholder="全部固定库"
              />
            </div>
            <div class="stock-query-item">
              <label>工序</label>
              <Select
                v-model:value="query.processType"
                :options="processTypeOptions"
                allow-clear
                placeholder="全部工序"
              />
            </div>
            <div class="stock-query-item">
              <label>库存状态</label>
              <Select
                v-model:value="query.status"
                :options="stockStatusOptions"
                allow-clear
                placeholder="全部在库"
              />
            </div>
            <div class="stock-query-item">
              <label>垫型</label>
              <Select
                v-model:value="query.padType"
                :options="padTypeOptions"
                allow-clear
                placeholder="全部垫型"
              />
            </div>
            <div class="stock-query-item">
              <label>型号</label>
              <Input
                v-model:value="query.modelNo"
                allow-clear
                placeholder="支持模糊匹配"
              />
            </div>
            <div class="stock-query-item">
              <label>料号</label>
              <Input
                v-model:value="query.materialCode"
                allow-clear
                placeholder="支持模糊匹配"
              />
            </div>
            <div class="stock-query-item">
              <label>入库日期起</label>
              <DatePicker
                v-model:value="query.shelvedDateStart"
                placeholder="请选择"
                value-format="YYYY-MM-DD"
              />
            </div>
            <div class="stock-query-item">
              <label>入库日期止</label>
              <DatePicker
                v-model:value="query.shelvedDateEnd"
                placeholder="请选择"
                value-format="YYYY-MM-DD"
              />
            </div>
          </div>
          <div class="stock-template-row">
            <label>模板名称</label>
            <Input
              v-model:value="queryTemplateName"
              allow-clear
              placeholder="填写名称后可保存当前查询条件"
            />
            <Button @click="saveQueryTemplate">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存模板
            </Button>
          </div>
          <div class="stock-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel stock-ledger-panel">
        <Grid
          :table-title="`在库不合格品逐片台账${selectedCount > 0 ? `（已选 ${selectedCount} 条）` : ''}`"
        >
          <template #selection="{ row }">
            <input
              :checked="selectedRowMap.has(row.id)"
              type="checkbox"
              @change="toggleRowSelection(row, $event.target.checked)"
            />
          </template>
          <template #selectionHeader>
            <input
              :checked="isAllPageSelected()"
              :indeterminate="isSomePageSelected() && !isAllPageSelected()"
              type="checkbox"
              @change="togglePageSelection($event.target.checked)"
            />
          </template>
          <template #status="{ row }">
            <Tag :color="statusColor(row.status)">
              {{ statusText(row.status) }}
            </Tag>
          </template>
        </Grid>
      </section>

      <Modal
        v-model:open="outboundVisible"
        :confirm-loading="outboundLoading"
        title="分切压槽不合格品批量出库"
        @ok="confirmOutbound"
      >
        <p>
          将出库
          {{ selectedCount }}
          条正常或冻结在库记录。出库后将扣减对应库存数量，并保留库存操作流水。
        </p>
        <Input.TextArea
          v-model:value="outboundReason"
          :maxlength="500"
          :rows="4"
          placeholder="请填写出库原因"
          show-count
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.ng-stock-query-board {
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
}

.stock-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.stock-simple-query {
  display: grid;
  grid-template-columns:
    max-content minmax(300px, 1fr)
    max-content max-content minmax(190px, 0.65fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.stock-simple-query-label {
  color: #243142;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.stock-query-template-select {
  width: 100%;
  min-width: 0;
}

.stock-query-panel :deep(.ant-input),
.stock-query-panel :deep(.ant-input-affix-wrapper),
.stock-query-panel :deep(.ant-picker),
.stock-query-panel :deep(.ant-select-selector),
.stock-query-item :deep(.ant-input),
.stock-query-item :deep(.ant-input-affix-wrapper),
.stock-query-item :deep(.ant-picker),
.stock-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 34px;
  border-radius: 6px;
}

.stock-query-panel :deep(.ant-input-affix-wrapper > input.ant-input),
.stock-query-item :deep(.ant-input-affix-wrapper > input.ant-input) {
  height: 24px;
}

.stock-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 64px;
  overflow: auto;
}

.stock-query-tag {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  max-width: 100%;
  padding: 4px 8px;
  color: #243142;
  background: #ffffff;
  border: 1px solid #c8d3df;
  border-radius: 6px;
}

.stock-query-tag span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-query-tag button {
  width: 18px;
  height: 18px;
  padding: 0;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.stock-advanced-query-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stock-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stock-query-item,
.stock-template-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.stock-query-item label,
.stock-template-row label {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.stock-template-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) max-content;
  align-items: center;
}

.stock-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stock-ledger-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.stock-ledger-panel :deep(.vben-vxe-grid),
.stock-ledger-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.stock-ledger-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.stock-ledger-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.stock-ledger-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.stock-ledger-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

@media (max-width: 1280px) {
  .stock-simple-query {
    grid-template-columns: max-content minmax(0, 1fr) max-content max-content;
  }

  .stock-query-template-select {
    grid-column: 2 / -1;
  }

  .stock-advanced-query-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stock-simple-query,
  .stock-advanced-query-grid,
  .stock-template-row {
    grid-template-columns: 1fr;
  }

  .stock-query-template-select {
    grid-column: auto;
  }
}
</style>
