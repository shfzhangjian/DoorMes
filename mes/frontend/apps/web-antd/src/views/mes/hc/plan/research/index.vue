<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcResearchTaskApi } from '#/api/mes/hc/researchtask';

import { computed, onMounted, reactive, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
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
  archiveResearchTaskToModel,
  confirmResearchTask,
  deleteResearchTask,
  deleteResearchTaskList,
  exportResearchTask,
  getResearchTaskPage,
} from '#/api/mes/hc/researchtask';
import {
  getModelRuleDetail,
  getModelRuleSelectOptions,
} from '#/api/mes/hc/modelrule';
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';

import {
  FORMULA_CODE_OPTIONS,
  POST_PROCESS_CODE_OPTIONS,
  PRODUCT_CLASS_OPTIONS,
  SINGLE_PROCESS_CODE_OPTIONS,
  formatNumber,
  getTaskStatusMeta,
  useGridColumns,
} from './data';
import Form from './modules/form.vue';
import '../../package-fg/shared/cut-round-board.css';

type ResearchQuery = {
  baseFormulaCode: string;
  grindingProcessCode: string;
  keyword: string;
  postProcessCode: string;
  productClassCode?: string;
  rdModelCode: string;
  researchDateEnd: Dayjs | null;
  researchDateStart: Dayjs | null;
  routeCode: string;
  taskNo: string;
  wetProcessCode: string;
};

type QueryFieldKey = Exclude<keyof ResearchQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  options?: { label: string; value: string }[];
  type?: 'date';
};
type FormulaOption = { label: string; text?: string; value: string };

const checkedIds = ref<number[]>([]);
const activeTab = ref<'all' | 'archived' | 'confirmed' | 'draft'>('all');
const advancedQueryVisible = ref(false);
const formulaQueryOptions = ref<FormulaOption[]>(FORMULA_CODE_OPTIONS);
const tabCounts = ref({
  all: 0,
  archived: 0,
  confirmed: 0,
  draft: 0,
});

const query = reactive<ResearchQuery>({
  baseFormulaCode: '',
  grindingProcessCode: '',
  keyword: '',
  postProcessCode: '',
  productClassCode: undefined,
  rdModelCode: '',
  researchDateEnd: null,
  researchDateStart: null,
  routeCode: '',
  taskNo: '',
  wetProcessCode: '',
});

const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'taskNo', label: '研发任务号' },
  { key: 'rdModelCode', label: '研发型号' },
  { key: 'productClassCode', label: '型号类型', options: PRODUCT_CLASS_OPTIONS },
  { key: 'baseFormulaCode', label: '基准配方', options: formulaQueryOptions.value },
  { key: 'wetProcessCode', label: '湿法工艺', options: SINGLE_PROCESS_CODE_OPTIONS },
  { key: 'grindingProcessCode', label: '磨皮工艺', options: SINGLE_PROCESS_CODE_OPTIONS },
  { key: 'postProcessCode', label: '后工艺', options: POST_PROCESS_CODE_OPTIONS },
  { key: 'routeCode', label: '路线编码' },
  { key: 'researchDateStart', label: '研发日期起', type: 'date' },
  { key: 'researchDateEnd', label: '研发日期止', type: 'date' },
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

function resolveTabStatuses() {
  switch (activeTab.value) {
    case 'archived': {
      return ['ARCHIVED'];
    }
    case 'confirmed': {
      return ['CONFIRMED'];
    }
    case 'draft': {
      return ['DRAFT'];
    }
    default: {
      return undefined;
    }
  }
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

async function loadFormulaDictOptions() {
  try {
    const rules = await getModelRuleSelectOptions();
    const targetRule = rules.find((item) => item.code === 'MODEL-RD')
      || rules.find((item) => item.code === 'MODEL-WHITE')
      || rules[0];
    if (!targetRule?.value) return;
    const detail = await getModelRuleDetail(Number(targetRule.value));
    const formulaDicts = (detail.modelRuleDicts || []).filter(isFormulaDict);
    if (formulaDicts.length === 0) return;
    const options = formulaDicts.map((item) => ({
      label: item.dictCode || item.dictValue || '',
      text: item.dictValue || item.dictCode || '',
      value: item.dictCode || item.dictValue || '',
    })).filter((item) => item.value);
    formulaQueryOptions.value = options;
    const config = getQueryFieldConfig('baseFormulaCode');
    if (config) config.options = options;
  } catch {
    formulaQueryOptions.value = FORMULA_CODE_OPTIONS;
  }
}

function isFormulaDict(item: MesHcModelRuleApi.ModelRuleDict) {
  const key = `${item.itemCode || ''}|${item.dictCode || ''}|${item.dictValue || ''}`.toLowerCase();
  return key.includes('formula') || key.includes('recipe') || key.includes('配方');
}

function getQueryFieldRawValue(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  if (getQueryFieldConfig(key)?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  switch (key) {
    case 'researchDateEnd':
    case 'researchDateStart': {
      (query as Record<string, any>)[key] = value ? dayjs(value) : null;
      break;
    }
    case 'productClassCode': {
      (query as Record<string, any>)[key] = value || undefined;
      break;
    }
    default: {
      (query as Record<string, any>)[key] = value || '';
    }
  }
}

function appendQueryValue(params: Record<string, any>, key: QueryFieldKey) {
  const value = getQueryFieldRawValue(key);
  if (value) params[key] = value;
}

function buildQueryParams(pageNo: number, pageSize: number) {
  const params: Record<string, any> = {
    pageNo,
    pageSize,
    taskStatuses: resolveTabStatuses(),
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  queryFieldConfigs.forEach((config) => appendQueryValue(params, config.key));
  return params as MesHcResearchTaskApi.PageReqVO;
}

function clearAdvancedQueryValues() {
  queryFieldConfigs.forEach((config) => setQueryFieldValue(config.key));
}

function clearAdvancedQuery() {
  clearAdvancedQueryValues();
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
  handleSearch();
}

async function fetchTabCounts() {
  const [all, draft, confirmed, archived] = await Promise.all([
    getResearchTaskPage({ pageNo: 1, pageSize: 1 }),
    getResearchTaskPage({ pageNo: 1, pageSize: 1, taskStatuses: ['DRAFT'] }),
    getResearchTaskPage({ pageNo: 1, pageSize: 1, taskStatuses: ['CONFIRMED'] }),
    getResearchTaskPage({ pageNo: 1, pageSize: 1, taskStatuses: ['ARCHIVED'] }),
  ]);
  tabCounts.value = {
    all: Number(all.total || 0),
    archived: Number(archived.total || 0),
    confirmed: Number(confirmed.total || 0),
    draft: Number(draft.total || 0),
  };
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

function handleEdit(row: MesHcResearchTaskApi.ResearchTask) {
  formModalApi.setData(row).open();
}

async function handleConfirm(row: MesHcResearchTaskApi.ResearchTask) {
  await confirm(`确认研发型号 ${row.rdModelCode} 吗？确认后型号组成字段将锁定。`);
  await confirmResearchTask(Number(row.id));
  message.success('研发型号已确认');
  handleRefresh();
}

async function handleArchive(row: MesHcResearchTaskApi.ResearchTask) {
  await confirm(`确认将 ${row.rdModelCode} 转入产品型号字典吗？`);
  await archiveResearchTaskToModel(Number(row.id));
  message.success('已转入产品型号字典');
  handleRefresh();
}

async function handleDelete(row: MesHcResearchTaskApi.ResearchTask) {
  const hideLoading = message.loading({ content: '正在删除研发型号...', duration: 0 });
  try {
    await deleteResearchTask(Number(row.id));
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的研发型号草稿吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteResearchTaskList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({
  records,
}: {
  records: MesHcResearchTaskApi.ResearchTask[];
}) {
  checkedIds.value = records.map((item) => Number(item.id));
}

async function handleExport() {
  const data = await exportResearchTask(buildQueryParams(1, 9999));
  downloadFileFromBlobPart({ fileName: '研发管理台账.xls', source: data });
}

function buildActions(row: MesHcResearchTaskApi.ResearchTask) {
  const status = String(row.taskStatus || 'DRAFT').toUpperCase();
  const actions: any[] = [
    {
      label: status === 'DRAFT' ? '办理' : '详情',
      type: 'link',
      icon: status === 'DRAFT' ? ACTION_ICON.EDIT : ACTION_ICON.PREVIEW,
      auth: ['mes:rd:research-task:update'],
      onClick: handleEdit.bind(null, row),
    },
  ];

  if (status === 'DRAFT') {
    actions.push({
      label: '确认',
      type: 'link',
      icon: 'lucide:check-circle-2',
      auth: ['mes:rd:research-task:update'],
      onClick: handleConfirm.bind(null, row),
    });
    actions.push({
      label: '删除',
      type: 'link',
      danger: true,
      icon: ACTION_ICON.DELETE,
      auth: ['mes:rd:research-task:delete'],
      popConfirm: {
        title: '确认删除当前研发型号吗？',
        confirm: handleDelete.bind(null, row),
      },
    });
  }

  if (status === 'CONFIRMED') {
    actions.push({
      label: '转型号字典',
      type: 'link',
      icon: 'lucide:archive',
      auth: ['mes:rd:research-task:archive'],
      onClick: handleArchive.bind(null, row),
    });
  }

  return actions;
}

function handleTabChange(key: string) {
  activeTab.value = key as 'all' | 'archived' | 'confirmed' | 'draft';
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
  class: 'research-task-vben-grid',
  gridClass: 'research-task-vxe-grid',
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
          const result = await getResearchTaskPage(
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
  } as VxeTableGridOptions<MesHcResearchTaskApi.ResearchTask>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

onMounted(() => {
  loadFormulaDictOptions();
  fetchTabCounts();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="package-fg-console research-task-list-page">
      <section class="prototype-banner research-task-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:flask-conical" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">研发管理</h2>
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
              <span class="console-meta-label">确认</span>
              <span class="console-meta-value">{{ tabCounts.confirmed }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">归档</span>
              <span class="console-meta-value">{{ tabCounts.archived }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">已选</span>
              <span class="console-meta-value">{{ checkedIds.length }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group research-task-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button v-access:code="['mes:rd:research-task:create']" class="action-tile" type="button" @click="handleCreate">
            <IconifyIcon icon="lucide:flask-conical" />
            <span>新建研发</span>
          </button>
          <button v-access:code="['mes:rd:research-task:export']" class="action-tile" type="button" @click="handleExport">
            <IconifyIcon icon="lucide:download" />
            <span>导出</span>
          </button>
          <button
            v-access:code="['mes:rd:research-task:delete']"
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

      <section class="package-fg-filter-bar research-task-query-panel">
        <div class="research-task-simple-query">
          <label class="research-task-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="任务号 / 研发型号 / 配方 / 路线 / 研发目的"
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
        </div>
        <div v-if="activeQueryTags.length > 0" class="research-task-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="research-task-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="research-task-tabs" @change="handleTabChange">
        <Tabs.TabPane key="all" :tab="`全部研发 (${tabCounts.all})`" />
        <Tabs.TabPane key="draft" :tab="`草稿 (${tabCounts.draft})`" />
        <Tabs.TabPane key="confirmed" :tab="`已确认 (${tabCounts.confirmed})`" />
        <Tabs.TabPane key="archived" :tab="`已归档 (${tabCounts.archived})`" />
      </Tabs>

      <div class="research-task-list-page__content">
        <div class="research-task-list-page__grid-host">
          <Grid>
            <template #taskNo="{ row }">
              <a class="research-task-list-page__task-link" @click="handleEdit(row)">
                {{ row.taskNo }}
              </a>
            </template>

            <template #taskStatus="{ row }">
              <Tag :color="getTaskStatusMeta(row.taskStatus).color" class="!m-0 border-none font-bold">
                {{ getTaskStatusMeta(row.taskStatus).label }}
              </Tag>
            </template>

            <template #rdModelCode="{ row }">
              <span class="research-task-model-code">{{ row.rdModelCode || '-' }}</span>
            </template>

            <template #reuseSeq="{ row }">
              <span class="research-task-reuse">R{{ row.reuseSeq || 1 }}</span>
            </template>

            <template #targetQty="{ row }">
              <span class="text-blue-600 font-bold text-base">
                {{ formatNumber(row.targetQty, 3) }}
              </span>
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
        width="760px"
        wrap-class-name="research-task-advanced-query-modal"
      >
        <div class="research-task-advanced-query-body">
          <div class="research-task-advanced-query-grid">
            <div class="research-task-query-item">
              <label>研发任务号</label>
              <Input v-model:value="query.taskNo" allow-clear placeholder="研发任务号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="research-task-query-item">
              <label>研发型号</label>
              <Input v-model:value="query.rdModelCode" allow-clear placeholder="研发型号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="research-task-query-item">
              <label>型号类型</label>
              <Select v-model:value="query.productClassCode" allow-clear :options="PRODUCT_CLASS_OPTIONS" placeholder="型号类型" />
            </div>
            <div class="research-task-query-item">
              <label>基准配方</label>
              <Select v-model:value="query.baseFormulaCode" allow-clear :options="formulaQueryOptions" placeholder="基准配方" />
            </div>
            <div class="research-task-query-item">
              <label>湿法工艺</label>
              <Select v-model:value="query.wetProcessCode" allow-clear :options="SINGLE_PROCESS_CODE_OPTIONS" placeholder="湿法" />
            </div>
            <div class="research-task-query-item">
              <label>磨皮工艺</label>
              <Select v-model:value="query.grindingProcessCode" allow-clear :options="SINGLE_PROCESS_CODE_OPTIONS" placeholder="磨皮" />
            </div>
            <div class="research-task-query-item">
              <label>后工艺</label>
              <Select v-model:value="query.postProcessCode" allow-clear :options="POST_PROCESS_CODE_OPTIONS" placeholder="后工艺" />
            </div>
            <div class="research-task-query-item">
              <label>路线编码</label>
              <Input v-model:value="query.routeCode" allow-clear placeholder="路线编码" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="research-task-query-item">
              <label>研发日期起</label>
              <DatePicker v-model:value="query.researchDateStart" placeholder="研发日期起" />
            </div>
            <div class="research-task-query-item">
              <label>研发日期止</label>
              <DatePicker v-model:value="query.researchDateEnd" placeholder="研发日期止" />
            </div>
          </div>
          <div class="research-task-advanced-footer">
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
.research-task-list-page {
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

.research-task-banner {
  min-height: 78px;
  max-height: 90px;
}

.research-task-action-group {
  flex-wrap: nowrap;
}

.research-task-tabs {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.research-task-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.research-task-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.research-task-query-panel {
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

.research-task-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.research-task-simple-query-label {
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

.research-task-simple-query :deep(.ant-input-affix-wrapper),
.research-task-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.research-task-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.research-task-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.research-task-query-tag {
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

.research-task-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.research-task-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.research-task-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.research-task-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.research-task-list-page__content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.research-task-list-page__grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.research-task-list-page__grid-host :deep(.research-task-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.research-task-list-page__task-link,
.research-task-model-code {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgb(67 56 202);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 700;
}

.research-task-list-page__task-link:hover {
  text-decoration: underline;
}

.research-task-reuse {
  color: #075985;
  font-weight: 900;
}

.research-task-advanced-query-body {
  display: grid;
  gap: 10px;
}

.research-task-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.research-task-query-item {
  display: grid;
  gap: 4px;
}

.research-task-query-item label {
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.research-task-query-item :deep(.ant-input),
.research-task-query-item :deep(.ant-input-affix-wrapper),
.research-task-query-item :deep(.ant-picker),
.research-task-query-item :deep(.ant-select-selector) {
  border-radius: 0;
}

.research-task-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.research-task-advanced-footer :deep(.ant-btn) {
  border-radius: 0;
  font-weight: 800;
}
</style>
