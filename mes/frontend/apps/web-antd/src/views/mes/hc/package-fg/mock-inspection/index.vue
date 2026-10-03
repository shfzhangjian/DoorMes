<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Input,
  Modal,
  Select,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  completeFgMockInspection,
  getFgMockInspectionPage,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgMockInspection' });

type InspectionSlice = MesHcFinishedPackagingApi.InspectionSlice;
type MockInspectionQuery = {
  inspectionTaskNo: string;
  keyword: string;
  parentProductionBatchNo: string;
  productionBatchNo: string;
  productionDateEnd: Dayjs | null;
  productionDateStart: Dayjs | null;
};
type QueryFieldKey = Exclude<keyof MockInspectionQuery, 'keyword'>;
type QueryFieldConfig = {
  key: QueryFieldKey;
  label: string;
  type?: 'date';
};
type QueryTemplate = {
  name: string;
  values: Partial<Record<keyof MockInspectionQuery, string>>;
};

const QUERY_TEMPLATE_STORAGE_KEY = 'mes:fg-mock-inspection:query-templates';

const userStore = useUserStore();
const rows = ref<InspectionSlice[]>([]);
const total = ref(0);
const submitting = ref(false);
const advancedQueryVisible = ref(false);
const selectedRowKeys = ref<number[]>([]);
const selectedRows = ref<InspectionSlice[]>([]);
const detailRecord = ref<InspectionSlice | null>(null);
const completeVisible = ref(false);
const queryTemplateName = ref('');
const selectedTemplateName = ref<string>();
const queryTemplates = ref<QueryTemplate[]>(loadQueryTemplates());
const query = reactive<MockInspectionQuery>({
  inspectionTaskNo: '',
  keyword: '',
  parentProductionBatchNo: '',
  productionBatchNo: '',
  productionDateStart: null as Dayjs | null,
  productionDateEnd: null as Dayjs | null,
});
const completeForm = reactive({
  inspectionResult: 'OK',
  inspectionRemark: '',
});

const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统');
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const okSelectedCount = computed(() => selectedRows.value.filter((item) => item.inspectionResult === 'OK').length);
const ngSelectedCount = computed(() => selectedRows.value.filter((item) => item.inspectionResult === 'NG').length);

const resultOptions = [
  { label: 'OK 合格', value: 'OK' },
  { label: 'NG 不合格', value: 'NG' },
];
const queryFieldConfigs: QueryFieldConfig[] = [
  { key: 'parentProductionBatchNo', label: '分段批次' },
  { key: 'inspectionTaskNo', label: '报检单号' },
  { key: 'productionBatchNo', label: '片号' },
  { key: 'productionDateStart', label: '生产日期起', type: 'date' },
  { key: 'productionDateEnd', label: '生产日期止', type: 'date' },
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

function statusText(status?: string) {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    INSPECTING: '待检测',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    COMPLETED: 'green',
    INSPECTING: 'processing',
  };
  return map[status || ''] || 'default';
}

function resultColor(result?: string) {
  if (result === 'OK') return 'green';
  if (result === 'NG') return 'red';
  return 'default';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function displayText(value?: number | string) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
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
  if (getQueryFieldConfig(key)?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function getQueryFieldRawValue(key: QueryFieldKey) {
  const value = query[key];
  if (!value) return '';
  if (getQueryFieldConfig(key)?.type === 'date') return (value as Dayjs).format('YYYY-MM-DD');
  return String(value).trim();
}

function setQueryFieldValue(key: QueryFieldKey, value?: string) {
  switch (key) {
    case 'inspectionTaskNo': {
      query.inspectionTaskNo = value || '';
      break;
    }
    case 'parentProductionBatchNo': {
      query.parentProductionBatchNo = value || '';
      break;
    }
    case 'productionBatchNo': {
      query.productionBatchNo = value || '';
      break;
    }
    case 'productionDateEnd': {
      query.productionDateEnd = value ? dayjs(value) : null;
      break;
    }
    case 'productionDateStart': {
      query.productionDateStart = value ? dayjs(value) : null;
      break;
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

function buildQueryParams(page?: { currentPage?: number; pageSize?: number }) {
  const params: Record<string, any> = {
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  };
  if (query.keyword.trim()) params.keyword = query.keyword.trim();
  if (query.inspectionTaskNo.trim()) params.inspectionTaskNo = query.inspectionTaskNo.trim();
  if (query.parentProductionBatchNo.trim()) params.parentProductionBatchNo = query.parentProductionBatchNo.trim();
  if (query.productionBatchNo.trim()) params.productionBatchNo = query.productionBatchNo.trim();
  if (query.productionDateStart) params.productionDateStart = query.productionDateStart.format('YYYY-MM-DD');
  if (query.productionDateEnd) params.productionDateEnd = query.productionDateEnd.format('YYYY-MM-DD');
  return params;
}

function handleSearch() {
  gridApi.query();
}

function handleReset() {
  query.keyword = '';
  query.inspectionTaskNo = '';
  query.parentProductionBatchNo = '';
  query.productionBatchNo = '';
  query.productionDateStart = null;
  query.productionDateEnd = null;
  queryTemplateName.value = '';
  selectedTemplateName.value = undefined;
  advancedQueryVisible.value = false;
  gridApi.query();
}

function handleRowCheckboxChange({ records }: { records: InspectionSlice[] }) {
  selectedRows.value = records;
  selectedRowKeys.value = records.map((item) => Number(item.sourceCutRoundReportId)).filter(Boolean);
}

function openDetail(row: InspectionSlice) {
  detailRecord.value = row;
  detailModalApi.open();
}

function openCompleteModal() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择待检测片');
    return;
  }
  completeVisible.value = true;
}

function submitComplete() {
  Modal.confirm({
    cancelText: '取消',
    content: `将 ${selectedRowKeys.value.length} 片更新为 ${completeForm.inspectionResult}，并同步裁切报检单明细。`,
    okText: '确认完成',
    onOk: async () => {
      submitting.value = true;
      try {
        const count = await completeFgMockInspection({
          cutRoundReportIds: selectedRowKeys.value,
          inspectionRemark: completeForm.inspectionRemark,
          inspectionResult: completeForm.inspectionResult,
          inspectorName: currentUserName.value,
        });
        message.success(`已完成 ${count} 片检测`);
        completeForm.inspectionRemark = '';
        await gridApi.query();
        completeVisible.value = false;
      } finally {
        submitting.value = false;
      }
    },
    title: '确认批量完成检测',
  });
}

const [DetailModal, detailModalApi] = useVbenModal({
  class: 'package-fg-detail-modal',
  closeOnClickModal: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
});

const columns = [
  { fixed: 'left', type: 'checkbox', width: 46 },
  { field: 'inspectionTaskNo', fixed: 'left', title: '报检单号', width: 170 },
  { field: 'parentProductionBatchNo', title: '分段批次', width: 170 },
  { field: 'productionBatchNo', title: '片号', width: 190 },
  { field: 'productionDate', title: '生产日期', width: 120 },
  { field: 'expiryDate', title: '有效期', width: 120 },
  { field: 'inspectionStatus', slots: { default: 'inspectionStatus' }, title: '检验状态', width: 110 },
  { field: 'inspectionResult', slots: { default: 'inspectionResult' }, title: '检验结果', width: 110 },
  { field: 'inspectionRemark', showOverflow: 'tooltip', title: '备注说明', width: 220 },
  { field: 'modelCode', title: '产品型号', width: 120 },
  { field: 'materialCode', title: '产品料号', width: 150 },
  { field: 'planNo', title: '计划号', width: 160 },
  { field: 'action', fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 96 },
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    checkboxConfig: {
      highlight: true,
    },
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
          const result = await getFgMockInspectionPage(buildQueryParams(page));
          rows.value = result.list || [];
          total.value = Number(result.total || 0);
          selectedRowKeys.value = [];
          selectedRows.value = [];
          return result;
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'sourceCutRoundReportId',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<InspectionSlice>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console package-fg-query-board">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:flask-conical" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">模拟检测</h2>
            <Tag class="console-title-tag" color="cyan">测试工具</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">待检测</span>
              <span class="console-meta-value">{{ total }}</span>
              <span class="console-meta-sub">片</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">已选</span>
              <span class="console-meta-value">{{ selectedRowKeys.length }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">选中OK</span>
              <span class="console-meta-value">{{ okSelectedCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">选中NG</span>
              <span class="console-meta-value">{{ ngSelectedCount }}</span>
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
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button
            class="action-tile"
            :class="{ 'is-disabled': selectedRowKeys.length === 0 || submitting }"
            :disabled="selectedRowKeys.length === 0 || submitting"
            type="button"
            @click="openCompleteModal"
          >
            <IconifyIcon icon="lucide:check-check" />
            <span>更新结果</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar inbound-query-panel">
        <div class="inbound-simple-query">
          <label class="inbound-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="分段批次 / 报检单号 / 片号 / 料号 / 型号 / 产品名称"
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
            :options="queryTemplateOptions"
            allow-clear
            class="inbound-query-template-select"
            placeholder="查询条件模板"
            @change="handleLoadQueryTemplate"
          />
        </div>
        <div v-if="activeQueryTags.length > 0" class="inbound-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="inbound-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="860px"
        wrap-class-name="inbound-advanced-query-modal"
      >
        <div class="inbound-advanced-query-body">
          <div class="inbound-advanced-query-grid">
            <div class="inbound-query-item">
              <label>分段批次</label>
              <Input
                v-model:value="query.parentProductionBatchNo"
                allow-clear
                placeholder="分段批次"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="inbound-query-item">
              <label>报检单号</label>
              <Input
                v-model:value="query.inspectionTaskNo"
                allow-clear
                placeholder="报检单号"
                @press-enter="applyAdvancedQuery"
              />
            </div>
            <div class="inbound-query-item">
              <label>片号</label>
              <Input v-model:value="query.productionBatchNo" allow-clear placeholder="片号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="inbound-query-item">
              <label>生产日期起</label>
              <DatePicker v-model:value="query.productionDateStart" placeholder="生产日期起" />
            </div>
            <div class="inbound-query-item">
              <label>生产日期止</label>
              <DatePicker v-model:value="query.productionDateEnd" placeholder="生产日期止" />
            </div>
          </div>
          <div class="inbound-template-row">
            <label>模板名称</label>
            <Input v-model:value="queryTemplateName" allow-clear placeholder="填写名称后可保存当前查询条件" />
            <Button @click="saveQueryTemplate">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存模板
            </Button>
          </div>
          <div class="inbound-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>

      <section class="package-fg-grid-panel">
        <Grid table-title="待模拟检测片列表">
          <template #inspectionStatus="{ row }">
            <Tag :color="statusColor(row.inspectionStatus)">{{ statusText(row.inspectionStatus) }}</Tag>
          </template>
          <template #inspectionResult="{ row }">
            <Tag :color="resultColor(row.inspectionResult)">{{ row.inspectionResult || '待判定' }}</Tag>
          </template>
          <template #actions="{ row }">
            <Button size="small" type="link" @click="openDetail(row)">详情</Button>
          </template>
        </Grid>
      </section>
    </div>

    <DetailModal>
      <div v-if="detailRecord" class="package-fg-detail-workbench">
        <div class="detail-toolbar">
          <div>
            <strong>待检测片详情</strong>
            <span>{{ detailRecord.productionBatchNo || '-' }}</span>
          </div>
          <Button size="small" @click="() => detailModalApi.close()">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            关闭窗口
          </Button>
        </div>
        <div class="detail-body">
          <fieldset class="detail-fieldset">
            <legend>1. 报检与生产信息</legend>
            <div class="detail-form-grid">
              <label>报检单号</label><strong>{{ displayText(detailRecord.inspectionTaskNo) }}</strong>
              <label>分段批次</label><strong>{{ displayText(detailRecord.parentProductionBatchNo) }}</strong>
              <label>片号</label><strong>{{ displayText(detailRecord.productionBatchNo) }}</strong>
              <label>生产日期</label><strong>{{ displayText(detailRecord.productionDate) }}</strong>
              <label>有效期</label><strong>{{ displayText(detailRecord.expiryDate) }}</strong>
              <label>计划号</label><strong>{{ displayText(detailRecord.planNo) }}</strong>
              <label>产品型号</label><strong>{{ displayText(detailRecord.modelCode) }}</strong>
              <label>产品料号</label><strong>{{ displayText(detailRecord.materialCode) }}</strong>
              <label>物料名称</label><strong>{{ displayText(detailRecord.materialName) }}</strong>
            </div>
          </fieldset>

          <fieldset class="detail-fieldset">
            <legend>2. 检测信息</legend>
            <div class="detail-form-grid">
              <label>检验状态</label><strong><Tag :color="statusColor(detailRecord.inspectionStatus)">{{ statusText(detailRecord.inspectionStatus) }}</Tag></strong>
              <label>检验结果</label><strong><Tag :color="resultColor(detailRecord.inspectionResult)">{{ displayText(detailRecord.inspectionResult || '待判定') }}</Tag></strong>
              <label>检验时间</label><strong>{{ formatDateTime(detailRecord.inspectionTime) }}</strong>
              <label>检验人</label><strong>{{ displayText(detailRecord.inspectorName) }}</strong>
              <label>备注说明</label><strong class="is-span-5">{{ displayText(detailRecord.inspectionRemark) }}</strong>
            </div>
          </fieldset>
        </div>
      </div>
    </DetailModal>

    <Modal
      v-model:open="completeVisible"
      :confirm-loading="submitting"
      title="批量更新检验结果"
      width="560px"
      @ok="submitComplete"
    >
      <div class="complete-modal-body">
        <div class="complete-summary">
          <span>已选择</span>
          <strong>{{ selectedRowKeys.length }}</strong>
          <span>片待检测片</span>
        </div>
        <div class="complete-form-grid">
          <label>检验结果</label>
          <Select v-model:value="completeForm.inspectionResult" :options="resultOptions" />
          <label>备注说明</label>
          <Textarea
            v-model:value="completeForm.inspectionRemark"
            :auto-size="{ minRows: 4, maxRows: 6 }"
            placeholder="请输入检验备注，可为空"
          />
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.package-fg-query-board {
  gap: 6px;
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.package-fg-query-board .prototype-banner {
  min-height: 78px;
  max-height: 86px;
}

.inbound-query-panel {
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

.inbound-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px 210px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.inbound-simple-query-label {
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

.inbound-simple-query :deep(.ant-input-affix-wrapper),
.inbound-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.inbound-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.inbound-query-template-select {
  min-width: 0;
}

.inbound-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.inbound-query-tag {
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

.inbound-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.inbound-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inbound-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.inbound-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.inbound-query-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.inbound-query-item label {
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

.inbound-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.inbound-query-item :deep(.ant-input),
.inbound-query-item :deep(.ant-input-affix-wrapper),
.inbound-query-item :deep(.ant-picker),
.inbound-query-item :deep(.ant-select),
.inbound-query-item :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.inbound-advanced-query-body {
  display: grid;
  gap: 10px;
}

.inbound-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.inbound-template-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: stretch;
  padding-top: 2px;
}

.inbound-template-row label {
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

.inbound-template-row :deep(.ant-input-affix-wrapper),
.inbound-template-row :deep(.ant-btn) {
  min-height: 32px;
  border-radius: 0;
}

.inbound-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.complete-modal-body {
  display: grid;
  gap: 14px;
}

.complete-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  color: #075985;
  background: #eef6ff;
  border: 1px solid #bfdbfe;
}

.complete-summary strong {
  font-size: 22px;
  font-weight: 900;
}

.complete-form-grid {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.complete-form-grid label {
  height: 32px;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  line-height: 32px;
  text-align: right;
}

.package-fg-grid-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.package-fg-grid-panel :deep(.vben-vxe-grid),
.package-fg-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.package-fg-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.package-fg-detail-workbench {
  display: grid;
  grid-template-rows: 48px minmax(0, 1fr);
  height: 100vh;
  overflow: hidden;
  color: #172033;
  background: #eef3f8;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.detail-toolbar strong {
  margin-right: 12px;
  font-size: 18px;
  font-weight: 900;
}

.detail-toolbar span {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
}

.detail-body {
  display: grid;
  grid-template-rows: max-content max-content;
  gap: 10px;
  min-height: 0;
  padding: 12px;
  overflow: auto;
}

.detail-fieldset {
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.detail-fieldset legend {
  padding: 0 8px;
  color: #075985;
  font-size: 13px;
  font-weight: 900;
}

.detail-form-grid {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) 100px minmax(0, 1fr) 100px minmax(0, 1fr);
  border-top: 1px solid #cbd5e1;
  border-left: 1px solid #cbd5e1;
}

.detail-form-grid label,
.detail-form-grid strong {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 6px 8px;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.detail-form-grid label {
  justify-content: flex-end;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
}

.detail-form-grid strong {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
}

.detail-form-grid .is-span-5 {
  grid-column: span 5;
}

@media (max-width: 1500px) {
  .inbound-simple-query {
    grid-template-columns: 86px minmax(240px, 1fr) 88px 110px;
  }

  .inbound-query-template-select {
    grid-column: 2 / span 3;
  }

  .inbound-advanced-query-grid {
    grid-template-columns: 1fr;
  }

  .inbound-template-row {
    grid-template-columns: 1fr;
  }

  .inbound-query-tags {
    padding-left: 0;
  }
}
</style>
