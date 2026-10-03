<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import {
  Button,
  DatePicker,
  Input,
  Modal,
  Select,
  Tabs,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deletePlanOrder,
  deletePlanOrderList,
  destroyPlanOrder,
  exportPlanOrder,
  getPlanOrderPage,
} from '#/api/mes/hc/planorder';
import { getSimpleUserList } from '#/api/system/user';

import {
  formatNumber,
  getPlanStatusMeta,
  useGridColumns,
} from './data';
import Form from './modules/form.vue';
import '../../package-fg/shared/cut-round-board.css';
const checkedIds = ref<number[]>([]);
const checkedRows = ref<MesHcPlanOrderApi.PlanOrder[]>([]);
const activeTab = ref<'all' | 'closed' | 'draft' | 'paused' | 'released'>('all');
const userNameMap = ref<Record<string, string>>({});
const route = useRoute();
const router = useRouter();
const tabCounts = ref({
  all: 0,
  draft: 0,
  paused: 0,
  released: 0,
  closed: 0,
});

type PlanQuery = {
  categoryCode: string;
  keyword: string;
  materialKeyword: string;
  modelCode: string;
  motherRollBatchNo: string;
  planDateEnd: Dayjs | null;
  planDateStart: Dayjs | null;
  planMode?: string;
  planNo: string;
  prodType: string;
  productionEndDateEnd: Dayjs | null;
  productionEndDateStart: Dayjs | null;
  productionStartDateEnd: Dayjs | null;
  productionStartDateStart: Dayjs | null;
  routeKeyword: string;
  salesOrderNo: string;
  sizeSpec: string;
  sourceType?: string;
};

type QueryFieldKey = Exclude<keyof PlanQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: { label: string; value: string }[];
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<keyof PlanQuery, string>>;
};

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:plan-order:query-templates';
const PLAN_SPLIT_OPEN_DETAIL_KEY = 'mes:plan-split:open-plan-detail';
const PLAN_SPLIT_PENDING_PLAN_KEY = 'mes:plan-split:pending-plan-no';

const advancedQueryVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());
const query = reactive<PlanQuery>({
  categoryCode: '',
  keyword: '',
  materialKeyword: '',
  modelCode: '',
  motherRollBatchNo: '',
  planDateEnd: null,
  planDateStart: null,
  planMode: undefined,
  planNo: '',
  prodType: '',
  productionEndDateEnd: null,
  productionEndDateStart: null,
  productionStartDateEnd: null,
  productionStartDateStart: null,
  routeKeyword: '',
  salesOrderNo: '',
  sizeSpec: '',
  sourceType: undefined,
});

const initialRoutePlanNo = resolveRoutePlanNo(route.query.planNo);
if (initialRoutePlanNo) {
  query.planNo = initialRoutePlanNo;
  query.keyword = '';
}

const planModeOptions = [
  { label: '按单生产 MTO', value: 'MTO' },
  { label: '按库生产 MTS', value: 'MTS' },
];
const sourceTypeOptions = [
  { label: '手工创建', value: 'MANUAL' },
  { label: '销售订单', value: 'SALES_ORDER' },
];
const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'planNo', label: '计划号' },
  { key: 'modelCode', label: '产品型号' },
  { key: 'materialKeyword', label: '产品料号' },
  { key: 'salesOrderNo', label: '销售订单' },
  { key: 'motherRollBatchNo', label: '生产批号' },
  { key: 'routeKeyword', label: '工艺路线' },
  { key: 'sizeSpec', label: '尺寸规格' },
  { key: 'prodType', label: '生产类型' },
  { key: 'categoryCode', label: '物料类型' },
  { key: 'planMode', label: '排产模式', options: planModeOptions },
  { key: 'sourceType', label: '来源类型', options: sourceTypeOptions },
  { key: 'planDateStart', label: '计划日期起', type: 'date' },
  { key: 'planDateEnd', label: '计划日期止', type: 'date' },
  { key: 'productionStartDateStart', label: '开始日期起', type: 'date' },
  { key: 'productionStartDateEnd', label: '开始日期止', type: 'date' },
  { key: 'productionEndDateStart', label: '结束日期起', type: 'date' },
  { key: 'productionEndDateEnd', label: '结束日期止', type: 'date' },
];

const activeQueryTags = computed(() =>
  queryFieldConfigs
    .map((config) => ({
      key: config.key,
      label: config.label,
      value: getQueryFieldText(config.key),
    }))
    .filter((item) => item.value),
);

const queryTemplateOptions = computed(() =>
  queryTemplates.value.map((item) => ({
    label: item.name,
    value: item.name,
  })),
);

function resolveCreatorName(creator?: string | number) {
  if (creator == null || creator === '') return '-';
  return userNameMap.value[String(creator)] || String(creator);
}

function resolveTabStatuses() {
  switch (activeTab.value) {
    case 'draft': {
      return ['DRAFT'];
    }
    case 'released': {
      return ['RELEASED'];
    }
    case 'paused': {
      return ['PAUSED'];
    }
    case 'closed': {
      return ['CLOSED'];
    }
    default: {
      return undefined;
    }
  }
}

function loadQueryTemplates() {
  if (typeof window === 'undefined') return [];
  try {
    const rawTemplates = window.localStorage.getItem(QUERY_TEMPLATE_STORAGE_KEY);
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

function resolveRoutePlanNo(value: unknown) {
  const raw = Array.isArray(value) ? value[0] : value;
  return String(raw || '').trim();
}

function persistQueryTemplates() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(QUERY_TEMPLATE_STORAGE_KEY, JSON.stringify(queryTemplates.value));
}

function getQueryFieldConfig(key: QueryFieldKey) {
  return queryFieldConfigs.find((item) => item.key === key);
}

function getQueryFieldText(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  const config = getQueryFieldConfig(key);
  if (config?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  const text = String(value).trim();
  if (!text) return '';
  return config?.options?.find((item) => item.value === text)?.label || text;
}

function getQueryFieldRawValue(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  if (getQueryFieldConfig(key)?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  switch (key) {
    case 'planDateEnd':
    case 'planDateStart':
    case 'productionEndDateEnd':
    case 'productionEndDateStart':
    case 'productionStartDateEnd':
    case 'productionStartDateStart': {
      (query as Record<string, any>)[key] = value ? dayjs(value) : null;
      break;
    }
    case 'planMode':
    case 'sourceType': {
      (query as Record<string, any>)[key] = value || undefined;
      break;
    }
    default: {
      (query as Record<string, any>)[key] = value || '';
    }
  }
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
    const value = getQueryFieldRawValue(config.key);
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
  const nextTemplates = queryTemplates.value.filter((item) => item.name !== name);
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
    setQueryFieldValue(key as QueryFieldKey, value);
  });
  queryTemplateName.value = template.name;
  handleSearch();
}

function handleLoadQueryTemplate(value?: string | number) {
  loadQueryTemplate(value ? String(value) : undefined);
}

function appendQueryValue(params: Record<string, any>, key: QueryFieldKey) {
  const value = getQueryFieldRawValue(key);
  if (value) params[key] = value;
}

function buildQueryParams(pageNo: number, pageSize: number) {
  const planStatuses = resolveTabStatuses();
  const params: Record<string, any> = {
    pageNo,
    pageSize,
    planStatuses,
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  queryFieldConfigs.forEach((config) => appendQueryValue(params, config.key));
  return params as MesHcPlanOrderApi.PageReqVO;
}

async function fetchTabCounts() {
  const [all, draft, released, paused, closed] = await Promise.all([
    getPlanOrderPage({ pageNo: 1, pageSize: 1 }),
    getPlanOrderPage({ pageNo: 1, pageSize: 1, planStatuses: ['DRAFT'] }),
    getPlanOrderPage({ pageNo: 1, pageSize: 1, planStatuses: ['RELEASED'] }),
    getPlanOrderPage({ pageNo: 1, pageSize: 1, planStatuses: ['PAUSED'] }),
    getPlanOrderPage({ pageNo: 1, pageSize: 1, planStatuses: ['CLOSED'] }),
  ]);
  tabCounts.value = {
    all: Number(all.total || 0),
    draft: Number(draft.total || 0),
    paused: Number(paused.total || 0),
    released: Number(released.total || 0),
    closed: Number(closed.total || 0),
  };
}

async function fetchUserNameMap() {
  const users = await getSimpleUserList();
  userNameMap.value = Object.fromEntries(
    users.map((item) => [String(item.id), item.nickname || item.username || String(item.id)]),
  );
}

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
  fetchTabCounts();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcPlanOrderApi.PlanOrder) {
  formModalApi.setData(row).open();
}

async function handleSplit(row: MesHcPlanOrderApi.PlanOrder) {
  const value = String(row.planNo || '').trim();
  if (!value) {
    message.warning('当前计划缺少计划号，无法进入拆批管理');
    return;
  }
  sessionStorage.setItem(PLAN_SPLIT_PENDING_PLAN_KEY, value);
  await router.push({
    name: 'MesHcExecutionPlanSplit',
  });
}

async function handleDelete(row: MesHcPlanOrderApi.PlanOrder) {
  if (!isCancelledPlan(row)) {
    message.warning('只有作废取消状态的生产计划允许删除');
    return;
  }
  if (!(await confirmPlanDeleteWarning(row, false))) return;
  const hideLoading = message.loading({ content: '正在删除生产计划...', duration: 0 });
  try {
    await deletePlanOrder(Number(row.id));
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  if (checkedRows.value.length === 0) {
    message.warning('请先勾选需要删除的作废计划');
    return;
  }
  const invalidRows = checkedRows.value.filter((item) => !isCancelledPlan(item));
  if (invalidRows.length > 0) {
    message.warning('批量删除前请确保选中计划全部为作废取消状态');
    return;
  }
  if (!(await confirmPlanDeleteWarning(undefined, true))) return;
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deletePlanOrderList(checkedIds.value);
    checkedIds.value = [];
    checkedRows.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDestroy(row: MesHcPlanOrderApi.PlanOrder) {
  if (!(await confirmPlanDeleteWarning(row, false, true))) return;
  const hideLoading = message.loading({ content: '正在销毁生产计划...', duration: 0 });
  try {
    await destroyPlanOrder(Number(row.id));
    message.success('销毁成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesHcPlanOrderApi.PlanOrder[];
}) {
  checkedIds.value = records.map((item) => Number(item.id));
  checkedRows.value = records;
}

function isCancelledStatus(status?: string) {
  return ['CANCELLED', 'CANCELED'].includes(String(status || '').toUpperCase());
}

function isCancelledPlan(row?: MesHcPlanOrderApi.PlanOrder) {
  return isCancelledStatus(row?.planStatus);
}

function confirmPlanDeleteWarning(row?: MesHcPlanOrderApi.PlanOrder, batch = false, destroy = false) {
  const title = destroy ? '确认销毁当前生产计划吗？' : batch ? '确认批量删除作废生产计划吗？' : '确认删除当前作废生产计划吗？';
  const planText = row?.planNo ? `计划号：${row.planNo}。` : batch ? `已选择 ${checkedIds.value.length} 条作废计划。` : '';
  const content = destroy
    ? `${planText}销毁会物理删除计划、工序、报工、送检、生产指令等关联信息，删除后无法在系统内恢复。请确认已经完成线下备份或留痕。`
    : `${planText}删除会逻辑删除计划、工序、报工、送检、生产指令等关联单据，相关数据将不再参与查询、排程和批次生成。`;
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      cancelText: '取消',
      content,
      okButtonProps: { danger: true },
      okText: destroy ? '确认销毁' : '确认删除',
      title,
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
    });
  });
}

async function handleExport() {
  const data = await exportPlanOrder(buildQueryParams(1, 9999));
  downloadFileFromBlobPart({ fileName: '生产计划台账.xls', source: data });
}

function buildActions(row: MesHcPlanOrderApi.PlanOrder) {
  const planStatus = String(row.planStatus || 'DRAFT').toUpperCase();
  const actions: any[] = [
    {
      label: activeTab.value === 'draft' || planStatus === 'DRAFT' ? '办理' : '详情',
      type: 'link',
      icon: planStatus === 'DRAFT' ? ACTION_ICON.EDIT : ACTION_ICON.PREVIEW,
      auth: ['mes:pp:plan:update'],
      onClick: handleEdit.bind(null, row),
    },
    {
      label: '拆批',
      type: 'link',
      icon: 'ant-design:split-cells-outlined',
      auth: ['mes:hc:plan-split:create'],
      onClick: handleSplit.bind(null, row),
    },
  ];

  if (isCancelledStatus(planStatus)) {
    actions.push({
      label: '删除',
      type: 'link',
      danger: true,
      icon: ACTION_ICON.DELETE,
      auth: ['mes:pp:plan:delete'],
      onClick: handleDelete.bind(null, row),
    });
  }

  actions.push({
    label: '销毁',
    type: 'link',
    danger: true,
    icon: 'ant-design:delete-filled',
    auth: ['mes:pp:plan:destroy'],
    onClick: handleDestroy.bind(null, row),
  });

  return actions;
}

function handleTabChange(key: string) {
  activeTab.value = key as 'all' | 'closed' | 'draft' | 'paused' | 'released';
  gridApi.query();
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  query.keyword = '';
  clearAdvancedQuery();
  advancedQueryVisible.value = false;
  gridApi.query();
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'plan-order-vben-grid',
  gridClass: 'plan-order-vxe-grid',
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getPlanOrderPage(
            buildQueryParams(page.currentPage, page.pageSize),
          );
          fetchTabCounts();
          return result;
        },
      },
    },
    toolbarConfig: {
      enabled: false,
    },
  } as VxeTableGridOptions<MesHcPlanOrderApi.PlanOrder>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

async function openPendingSplitPlanDetail() {
  const pendingPlanNo = String(sessionStorage.getItem(PLAN_SPLIT_OPEN_DETAIL_KEY) || '').trim();
  if (!pendingPlanNo) {
    return;
  }
  sessionStorage.removeItem(PLAN_SPLIT_OPEN_DETAIL_KEY);
  activeTab.value = 'all';
  checkedIds.value = [];
  checkedRows.value = [];
  query.planNo = pendingPlanNo;
  query.keyword = '';
  await nextTick();
  gridApi.query();
  const result = await getPlanOrderPage({
    pageNo: 1,
    pageSize: 1,
    planNo: pendingPlanNo,
  });
  const row = result.list?.[0];
  if (row) {
    handleEdit(row);
  } else {
    message.warning(`拆批新计划 ${pendingPlanNo} 已生成，但列表暂未查询到详情`);
  }
}

onMounted(() => {
  fetchUserNameMap();
  fetchTabCounts();
  void openPendingSplitPlanDetail();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="package-fg-console plan-order-list-page">
      <section class="prototype-banner plan-order-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:clipboard-list" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">生产计划</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">全部</span>
              <span class="console-meta-value">{{ tabCounts.all }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">草稿</span>
              <span class="console-meta-value">{{ tabCounts.draft }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">下达</span>
              <span class="console-meta-value">{{ tabCounts.released }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">暂停</span>
              <span class="console-meta-value">{{ tabCounts.paused }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">关闭</span>
              <span class="console-meta-value">{{ tabCounts.closed }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">已选</span>
              <span class="console-meta-value">{{ checkedIds.length }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group plan-order-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button v-access:code="['mes:pp:plan:create']" class="action-tile" type="button" @click="handleCreate">
            <IconifyIcon icon="lucide:calendar-plus" />
            <span>发起排产</span>
          </button>
          <button v-access:code="['mes:pp:plan:export']" class="action-tile" type="button" @click="handleExport">
            <IconifyIcon icon="lucide:download" />
            <span>导出</span>
          </button>
          <button
            v-access:code="['mes:pp:plan:delete']"
            class="action-tile is-danger"
            :class="{ 'is-disabled': isEmpty(checkedIds) }"
            :disabled="isEmpty(checkedIds)"
            type="button"
            @click="handleDeleteBatch"
          >
            <IconifyIcon icon="lucide:trash-2" />
            <span>批量删除</span>
          </button>
          <button class="action-tile" type="button" @click="handleRefresh">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar plan-order-query-panel">
        <div class="plan-order-simple-query">
          <label class="plan-order-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="计划号 / 产品料号 / 产品型号 / 销售单 / 批号 / 路线"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Select
            v-model:value="selectedTemplateName"
            allow-clear
            class="plan-order-query-template-select"
            :options="queryTemplateOptions"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="plan-order-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="plan-order-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="plan-order-tabs" @change="handleTabChange">
        <Tabs.TabPane key="all" :tab="`全部计划 (${tabCounts.all})`" />
        <Tabs.TabPane key="draft" :tab="`草稿 (${tabCounts.draft})`" />
        <Tabs.TabPane key="released" :tab="`下达 (${tabCounts.released})`" />
        <Tabs.TabPane key="paused" :tab="`暂停 (${tabCounts.paused})`" />
        <Tabs.TabPane key="closed" :tab="`关闭 (${tabCounts.closed})`" />
      </Tabs>

      <div class="plan-order-list-page__content">
        <div class="plan-order-list-page__grid-host">
          <Grid>
            <template #planNo="{ row }">
              <div class="plan-order-list-page__plan-no">
                <a
                  :class="['plan-order-list-page__plan-link', { 'is-cancelled': isCancelledPlan(row) }]"
                  @click="handleEdit(row)"
                >
                  {{ row.planNo }}
                </a>
                <span v-if="isCancelledPlan(row)" class="plan-order-list-page__void-mark">
                  <IconifyIcon icon="lucide:ban" />
                  作废计划
                </span>
              </div>
            </template>

            <template #planStatus="{ row }">
              <Tag :color="getPlanStatusMeta(row.planStatus).color" class="!m-0 border-none font-bold">
                {{ getPlanStatusMeta(row.planStatus).label }}
              </Tag>
            </template>

            <template #creator="{ row }">
              {{ resolveCreatorName(row.creator) }}
            </template>

            <template #targetQty="{ row }">
              <span class="text-blue-600 font-bold text-base">
                {{ formatNumber(row.targetQty, 3) }}
              </span>
              <span class="text-xs text-slate-500">{{ row.targetUnitCode || row.targetUom || '-' }}</span>
            </template>

            <template #actions="{ row }">
              <TableAction :actions="buildActions(row)" />
            </template>
          </Grid>
        </div>
      </div>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="860px"
        wrap-class-name="plan-order-advanced-query-modal"
      >
        <div class="plan-order-advanced-query-body">
          <div class="plan-order-advanced-query-grid">
            <div class="plan-order-query-item">
              <label>计划号</label>
              <Input v-model:value="query.planNo" allow-clear placeholder="计划号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>产品型号</label>
              <Input v-model:value="query.modelCode" allow-clear placeholder="产品型号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>产品料号</label>
              <Input v-model:value="query.materialKeyword" allow-clear placeholder="产品料号或名称" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>销售订单</label>
              <Input v-model:value="query.salesOrderNo" allow-clear placeholder="销售订单号/ERP号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>生产批号</label>
              <Input v-model:value="query.motherRollBatchNo" allow-clear placeholder="母卷/生产/主批号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>工艺路线</label>
              <Input v-model:value="query.routeKeyword" allow-clear placeholder="路线编码或名称" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>尺寸规格</label>
              <Input v-model:value="query.sizeSpec" allow-clear placeholder="尺寸规格" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>生产类型</label>
              <Input v-model:value="query.prodType" allow-clear placeholder="生产类型编码" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>物料类型</label>
              <Input v-model:value="query.categoryCode" allow-clear placeholder="物料类型编码" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="plan-order-query-item">
              <label>排产模式</label>
              <Select v-model:value="query.planMode" allow-clear :options="planModeOptions" placeholder="排产模式" />
            </div>
            <div class="plan-order-query-item">
              <label>来源类型</label>
              <Select v-model:value="query.sourceType" allow-clear :options="sourceTypeOptions" placeholder="来源类型" />
            </div>
            <div class="plan-order-query-item">
              <label>计划日期起</label>
              <DatePicker v-model:value="query.planDateStart" placeholder="计划日期起" />
            </div>
            <div class="plan-order-query-item">
              <label>计划日期止</label>
              <DatePicker v-model:value="query.planDateEnd" placeholder="计划日期止" />
            </div>
            <div class="plan-order-query-item">
              <label>开始日期起</label>
              <DatePicker v-model:value="query.productionStartDateStart" placeholder="计划开始起" />
            </div>
            <div class="plan-order-query-item">
              <label>开始日期止</label>
              <DatePicker v-model:value="query.productionStartDateEnd" placeholder="计划开始止" />
            </div>
            <div class="plan-order-query-item">
              <label>结束日期起</label>
              <DatePicker v-model:value="query.productionEndDateStart" placeholder="计划结束起" />
            </div>
            <div class="plan-order-query-item">
              <label>结束日期止</label>
              <DatePicker v-model:value="query.productionEndDateEnd" placeholder="计划结束止" />
            </div>
          </div>
          <div class="plan-order-template-row">
            <label>模板名称</label>
            <Input
              v-model:value="queryTemplateName"
              allow-clear
              placeholder="填写名称后可保存当前查询条件"
            />
            <Button @click="saveQueryTemplate">保存模板</Button>
          </div>
          <div class="plan-order-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.plan-order-list-page {
  display: grid;
  box-sizing: border-box;
  grid-template-rows: max-content max-content max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.plan-order-banner {
  min-height: 78px;
  max-height: 90px;
}

.plan-order-action-group {
  flex-wrap: nowrap;
}

.plan-order-tabs {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.plan-order-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.plan-order-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.plan-order-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.plan-order-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px 210px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.plan-order-simple-query-label {
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

.plan-order-simple-query :deep(.ant-input-affix-wrapper),
.plan-order-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.plan-order-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.plan-order-query-template-select {
  min-width: 0;
}

.plan-order-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.plan-order-query-tag {
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

.plan-order-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.plan-order-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-order-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.plan-order-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.plan-order-list-page__content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.plan-order-list-page__grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.plan-order-list-page__grid-host :deep(.plan-order-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.plan-order-list-page__plan-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgb(67 56 202);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 700;
}

.plan-order-list-page__plan-no {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.plan-order-list-page__plan-link.is-cancelled {
  color: #b42318;
}

.plan-order-list-page__void-mark {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
  padding: 1px 5px;
  color: #b42318;
  font-size: 11px;
  font-weight: 900;
  line-height: 18px;
  background: #fff1f3;
  border: 1px solid #fda29b;
}

.plan-order-list-page__plan-link:hover {
  text-decoration: underline;
}

.plan-order-list-page__expand-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  color: rgb(22 119 255);
  font-size: 13px;
  cursor: pointer;
}

.plan-order-list-page__expand-link:hover {
  text-decoration: underline;
}

.plan-order-list-page__expand-icon {
  font-size: 14px;
}

.plan-order-advanced-query-body {
  display: grid;
  gap: 10px;
}

.plan-order-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.plan-order-query-item {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.plan-order-query-item label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  background: #dbe3ed;
  border-right: 1px solid #c6d3df;
}

.plan-order-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.plan-order-query-item :deep(.ant-input),
.plan-order-query-item :deep(.ant-input-affix-wrapper),
.plan-order-query-item :deep(.ant-picker),
.plan-order-query-item :deep(.ant-select),
.plan-order-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.plan-order-template-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: stretch;
  padding-top: 2px;
}

.plan-order-template-row label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.plan-order-template-row :deep(.ant-input-affix-wrapper),
.plan-order-template-row :deep(.ant-btn) {
  min-height: 32px;
  border-radius: 0;
}

.plan-order-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.plan-order-list-page__node-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border: 1px solid rgb(226 232 240);
  border-radius: 999px;
  background: rgb(248 250 252);
  color: rgb(51 65 85);
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.05);
}

.plan-order-list-page__grid-host :deep(.plan-order-vxe-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  display: grid !important;
  grid-template-rows: auto minmax(0, 1fr) auto auto !important;
  overflow: hidden !important;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--form-wrapper) {
  display: none !important;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--toolbar-wrapper) {
  display: none !important;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 2;
  min-height: 0 !important;
  overflow: hidden !important;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.plan-order-list-page__grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 4;
  min-height: 0;
  background: rgb(255 255 255);
}

.plan-order-list-page__grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.plan-order-list-page__grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

@media (max-width: 1180px) {
  .plan-order-list-page {
    grid-template-rows: max-content max-content max-content minmax(0, 1fr);
  }

  .plan-order-banner {
    max-height: none;
    flex-wrap: wrap;
  }

  .plan-order-action-group {
    width: 100%;
    padding-left: 0 !important;
    border-left: 0 !important;
  }

  .plan-order-simple-query {
    grid-template-columns: 86px minmax(240px, 1fr) 88px 110px;
  }

  .plan-order-query-template-select {
    grid-column: 2 / -1;
  }
}

@media (max-width: 760px) {
  .plan-order-simple-query,
  .plan-order-advanced-query-grid,
  .plan-order-template-row {
    grid-template-columns: 1fr;
  }

  .plan-order-query-tags {
    padding-left: 0;
  }

  .plan-order-query-item {
    grid-template-columns: 86px minmax(0, 1fr);
  }

  .plan-order-template-row label,
  .plan-order-simple-query-label {
    justify-content: flex-start;
  }
}
</style>
