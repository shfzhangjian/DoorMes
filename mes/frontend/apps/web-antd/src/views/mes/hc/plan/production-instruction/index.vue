<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';
import type { PickerOption } from '#/components/picker';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  Modal,
  Tabs,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmProductionInstruction,
  deleteProductionInstruction,
  exportProductionInstruction,
  getProductionInstructionPage,
  issueProductionInstruction,
  revokeProductionInstruction,
  updateProductionInstruction,
} from '#/api/mes/hc/production-instruction';
import { PickerModal, processPickerConfig } from '#/components/picker';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesHcPlanProductionInstruction' });

type QueryState = Omit<MesHcProductionInstructionApi.PageReqVO, 'pageNo' | 'pageSize'> & {
  issuedTimeRange?: [Dayjs, Dayjs];
};
type InstructionTabKey = 'all' | 'confirmed' | 'issued' | 'revoked';
type QueryTagKey = 'batchNo' | 'issuedTimeRange' | 'operationCode' | 'operationName' | 'planNo';

const STATUS_OPTIONS = [
  { color: 'processing', label: '下达', value: 'ISSUED' },
  { color: 'green', label: '确认', value: 'CONFIRMED' },
  { color: 'red', label: '撤销', value: 'REVOKED' },
];

const activeTab = ref<InstructionTabKey>('all');
const advancedQueryVisible = ref(false);
const formVisible = ref(false);
const detailVisible = ref(false);
const processPickerOpen = ref(false);
const revokeVisible = ref(false);
const submitLoading = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const currentRow = ref<MesHcProductionInstructionApi.Instruction>();
const revokeReason = ref('');
const pageRows = ref<MesHcProductionInstructionApi.Instruction[]>([]);
const tabCounts = ref({
  all: 0,
  confirmed: 0,
  issued: 0,
  revoked: 0,
});

const query = reactive<QueryState>({
  batchNo: '',
  keyword: '',
  operationCode: '',
  operationName: '',
  planNo: '',
  status: undefined,
});

const formState = reactive<MesHcProductionInstructionApi.Instruction>({
  batchNo: '',
  instructionContent: '',
  issuedTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  operationCode: '',
  operationName: '',
  planNo: '',
  planOperationId: undefined,
  processCode: '',
  processId: undefined,
  processName: '',
  remark: '',
});

const activeQueryTags = computed(() =>
  [
    { key: 'planNo', label: '计划号', value: query.planNo },
    { key: 'batchNo', label: '批次号', value: query.batchNo },
    { key: 'operationName', label: '工序', value: query.operationName },
    { key: 'operationCode', label: '工序编码', value: query.operationCode },
    {
      key: 'issuedTimeRange',
      label: '下达时间',
      value: query.issuedTimeRange
        ? `${query.issuedTimeRange[0]?.format('YYYY-MM-DD HH:mm')} 至 ${query.issuedTimeRange[1]?.format('YYYY-MM-DD HH:mm')}`
        : '',
    },
  ].filter((item) => String(item.value || '').trim()) as {
    key: QueryTagKey;
    label: string;
    value: string;
  }[],
);

function statusMeta(status?: string) {
  return STATUS_OPTIONS.find((item) => item.value === status) || STATUS_OPTIONS[0];
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function resolveTabStatus() {
  switch (activeTab.value) {
    case 'confirmed': {
      return 'CONFIRMED';
    }
    case 'issued': {
      return 'ISSUED';
    }
    case 'revoked': {
      return 'REVOKED';
    }
    default: {
      return undefined;
    }
  }
}

function buildBaseQueryParams() {
  const issuedTimeRange = query.issuedTimeRange;
  return {
    batchNo: query.batchNo || undefined,
    issuedTimeEnd: issuedTimeRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
    issuedTimeStart: issuedTimeRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
    keyword: query.keyword || undefined,
    operationCode: query.operationCode || undefined,
    operationName: query.operationName || undefined,
    planNo: query.planNo || undefined,
  };
}

function buildQueryParams(): MesHcProductionInstructionApi.PageReqVO {
  return {
    ...buildBaseQueryParams(),
    status: resolveTabStatus(),
  };
}

async function fetchTabCounts() {
  const baseParams = buildBaseQueryParams();
  const [all, issued, confirmed, revoked] = await Promise.all([
    getProductionInstructionPage({ ...baseParams, pageNo: 1, pageSize: 1 }),
    getProductionInstructionPage({ ...baseParams, pageNo: 1, pageSize: 1, status: 'ISSUED' }),
    getProductionInstructionPage({ ...baseParams, pageNo: 1, pageSize: 1, status: 'CONFIRMED' }),
    getProductionInstructionPage({ ...baseParams, pageNo: 1, pageSize: 1, status: 'REVOKED' }),
  ]);
  tabCounts.value = {
    all: Number(all.total || 0),
    confirmed: Number(confirmed.total || 0),
    issued: Number(issued.total || 0),
    revoked: Number(revoked.total || 0),
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'production-instruction-vben-grid',
  gridClass: 'production-instruction-vxe-grid',
  gridOptions: {
    border: true,
    columns: [
      { field: 'instructionNo', fixed: 'left', minWidth: 160, title: '指令号' },
      { field: 'status', slots: { default: 'status' }, title: '状态', width: 92 },
      { field: 'batchNo', minWidth: 150, title: '批次号' },
      { field: 'planNo', minWidth: 150, title: '计划号' },
      { field: 'operationName', minWidth: 130, title: '工序' },
      { field: 'instructionContent', minWidth: 260, showOverflow: 'tooltip', title: '指令内容' },
      { field: 'issuerName', minWidth: 110, title: '下达人' },
      { field: 'issuedTime', formatter: ({ cellValue }) => formatDateTime(cellValue), minWidth: 150, title: '下达时间' },
      { field: 'confirmerName', minWidth: 120, title: '确认人' },
      { field: 'confirmTime', formatter: ({ cellValue }) => formatDateTime(cellValue), minWidth: 150, title: '确认时间' },
      { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 260 },
    ],
    height: '100%',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getProductionInstructionPage({
            ...buildQueryParams(),
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          });
          pageRows.value = result.list || [];
          fetchTabCounts();
          return result;
        },
      },
    },
    rowConfig: {
      height: 46,
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: { enabled: false },
  } as VxeTableGridOptions<MesHcProductionInstructionApi.Instruction>,
});

function queryFirstPage() {
  void (async () => {
    await (gridApi.grid as any)?.setCurrentPage?.(1);
    await gridApi.query();
  })();
}

function handleSearch() {
  queryFirstPage();
}

function clearAdvancedQuery() {
  query.batchNo = '';
  query.issuedTimeRange = undefined;
  query.operationCode = '';
  query.operationName = '';
  query.planNo = '';
  query.status = undefined;
}

function resetQuery() {
  query.keyword = '';
  clearAdvancedQuery();
  advancedQueryVisible.value = false;
  queryFirstPage();
}

function applyAdvancedQuery() {
  advancedQueryVisible.value = false;
  queryFirstPage();
}

function removeQueryCondition(key: QueryTagKey) {
  if (key === 'issuedTimeRange') {
    query.issuedTimeRange = undefined;
  } else {
    query[key] = '';
  }
  queryFirstPage();
}

function handleRefresh() {
  gridApi.query();
  fetchTabCounts();
}

function handleTabChange(key: string) {
  activeTab.value = key as InstructionTabKey;
  queryFirstPage();
}

function resetForm() {
  Object.assign(formState, {
    batchNo: '',
    id: undefined,
    instructionContent: '',
    issuedTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    operationCode: '',
    operationName: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    processCode: '',
    processId: undefined,
    processName: '',
    remark: '',
  });
}

function openCreate() {
  formMode.value = 'create';
  resetForm();
  formVisible.value = true;
}

function openEdit(row: MesHcProductionInstructionApi.Instruction) {
  formMode.value = 'edit';
  resetForm();
  Object.assign(formState, row);
  formVisible.value = true;
}

function openDetail(row: MesHcProductionInstructionApi.Instruction) {
  currentRow.value = row;
  detailVisible.value = true;
}

function handlePickProcess(option: PickerOption) {
  formState.processId = Number(option.id) || undefined;
  formState.processCode = option.code;
  formState.processName = option.name;
  formState.operationCode = option.code;
  formState.operationName = option.name;
  processPickerOpen.value = false;
}

async function submitForm() {
  if (!formState.batchNo?.trim()) {
    message.warning('请填写批次号');
    return;
  }
  if (!formState.operationName?.trim() && !formState.processName?.trim()) {
    message.warning('请选择工序');
    return;
  }
  if (!formState.instructionContent?.trim()) {
    message.warning('请填写指令内容');
    return;
  }
  submitLoading.value = true;
  try {
    const payload = { ...formState };
    if (formMode.value === 'create') {
      await issueProductionInstruction(payload);
      message.success('生产指令已下达');
    } else {
      await updateProductionInstruction(payload);
      message.success('生产指令已更新');
    }
    formVisible.value = false;
    await gridApi.query();
  } finally {
    submitLoading.value = false;
  }
}

function handleConfirm(row: MesHcProductionInstructionApi.Instruction) {
  Modal.confirm({
    title: '确认生产指令',
    content: `确认后将记录当前确认人和确认时间。批次：${row.batchNo || '-'}`,
    async onOk() {
      await confirmProductionInstruction(row.id!);
      message.success('生产指令已确认');
      await gridApi.query();
    },
  });
}

function openRevoke(row: MesHcProductionInstructionApi.Instruction) {
  currentRow.value = row;
  revokeReason.value = '';
  revokeVisible.value = true;
}

async function submitRevoke() {
  if (!currentRow.value?.id) return;
  await revokeProductionInstruction({
    id: currentRow.value.id,
    revokeReason: revokeReason.value,
  });
  message.success('生产指令已撤下');
  revokeVisible.value = false;
  await gridApi.query();
}

function handleDelete(row: MesHcProductionInstructionApi.Instruction) {
  Modal.confirm({
    title: '删除生产指令',
    content: `确认删除指令 ${row.instructionNo || row.id}？`,
    async onOk() {
      await deleteProductionInstruction(row.id!);
      message.success('生产指令已删除');
      await gridApi.query();
    },
  });
}

async function handleExport() {
  const blob = await exportProductionInstruction(buildQueryParams());
  downloadFileFromBlobPart({ fileName: '生产指令.xls', source: blob });
}

onMounted(() => {
  fetchTabCounts();
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console production-instruction-list-page">
      <section class="prototype-banner production-instruction-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:megaphone" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">生产指令</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">全部</span>
              <span class="console-meta-value">{{ tabCounts.all }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">下达</span>
              <span class="console-meta-value">{{ tabCounts.issued }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">确认</span>
              <span class="console-meta-value">{{ tabCounts.confirmed }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">撤下</span>
              <span class="console-meta-value">{{ tabCounts.revoked }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">当前页</span>
              <span class="console-meta-value">{{ pageRows.length }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group production-instruction-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button
            v-access:code="['mes:pp:production-instruction:issue']"
            class="action-tile"
            type="button"
            @click="openCreate"
          >
            <IconifyIcon icon="lucide:send" />
            <span>下达</span>
          </button>
          <button
            v-access:code="['mes:pp:production-instruction:export']"
            class="action-tile"
            type="button"
            @click="handleExport"
          >
            <IconifyIcon icon="lucide:download" />
            <span>导出</span>
          </button>
          <button class="action-tile" type="button" @click="handleRefresh">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar production-instruction-query-panel">
        <div class="production-instruction-simple-query">
          <label class="production-instruction-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="指令号 / 计划号 / 批次号 / 工序 / 内容"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="advancedQueryVisible = true">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
        </div>
        <div v-if="activeQueryTags.length > 0" class="production-instruction-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="production-instruction-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="production-instruction-tabs" @change="handleTabChange">
        <Tabs.TabPane key="all" :tab="`全部指令 (${tabCounts.all})`" />
        <Tabs.TabPane key="issued" :tab="`已下达 (${tabCounts.issued})`" />
        <Tabs.TabPane key="confirmed" :tab="`已确认 (${tabCounts.confirmed})`" />
        <Tabs.TabPane key="revoked" :tab="`已撤下 (${tabCounts.revoked})`" />
      </Tabs>

      <div class="production-instruction-list-page__content">
        <div class="production-instruction-list-page__grid-host">
          <Grid>
            <template #status="{ row }">
              <Tag :color="statusMeta(row.status).color" class="!m-0 border-none font-bold">
                {{ statusMeta(row.status).label }}
              </Tag>
            </template>
            <template #action="{ row }">
              <div class="production-instruction-row-actions">
                <Button size="small" type="link" @click="openDetail(row)">
                  <template #icon><IconifyIcon icon="lucide:eye" /></template>
                </Button>
                <Button
                  v-if="row.status === 'ISSUED'"
                  v-access:code="['mes:pp:production-instruction:update']"
                  size="small"
                  type="link"
                  @click="openEdit(row)"
                >
                  <template #icon><IconifyIcon icon="lucide:pencil" /></template>
                </Button>
                <Button
                  v-if="row.status === 'ISSUED'"
                  v-access:code="['mes:pp:production-instruction:confirm']"
                  size="small"
                  type="link"
                  @click="handleConfirm(row)"
                >
                  确认
                </Button>
                <Button
                  v-if="row.status !== 'REVOKED'"
                  v-access:code="['mes:pp:production-instruction:revoke']"
                  danger
                  size="small"
                  type="link"
                  @click="openRevoke(row)"
                >
                  撤下
                </Button>
                <Button
                  v-if="row.status !== 'CONFIRMED'"
                  v-access:code="['mes:pp:production-instruction:delete']"
                  danger
                  size="small"
                  type="link"
                  @click="handleDelete(row)"
                >
                  <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                </Button>
              </div>
            </template>
          </Grid>
        </div>
      </div>

      <Modal
        v-model:open="advancedQueryVisible"
        :footer="null"
        title="多条件查询"
        width="720px"
        wrap-class-name="production-instruction-advanced-query-modal"
      >
        <div class="production-instruction-advanced-query-body">
          <div class="production-instruction-advanced-query-grid">
            <div class="production-instruction-query-item">
              <label>计划号</label>
              <Input v-model:value="query.planNo" allow-clear placeholder="计划号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="production-instruction-query-item">
              <label>批次号</label>
              <Input v-model:value="query.batchNo" allow-clear placeholder="批次号" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="production-instruction-query-item">
              <label>工序</label>
              <Input v-model:value="query.operationName" allow-clear placeholder="工序名称" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="production-instruction-query-item">
              <label>工序编码</label>
              <Input v-model:value="query.operationCode" allow-clear placeholder="工序编码" @press-enter="applyAdvancedQuery" />
            </div>
            <div class="production-instruction-query-item production-instruction-query-item--wide">
              <label>下达时间</label>
              <DatePicker.RangePicker
                v-model:value="query.issuedTimeRange"
                format="YYYY-MM-DD HH:mm:ss"
                show-time
              />
            </div>
          </div>
          <div class="production-instruction-advanced-footer">
            <Button @click="advancedQueryVisible = false">关闭</Button>
            <Button @click="clearAdvancedQuery">清空条件</Button>
            <Button type="primary" @click="applyAdvancedQuery">应用查询</Button>
          </div>
        </div>
      </Modal>
    </div>

    <Modal
      v-model:open="formVisible"
      :confirm-loading="submitLoading"
      :title="formMode === 'create' ? '指令下达' : '编辑生产指令'"
      width="820px"
      @ok="submitForm"
    >
      <div class="production-instruction-form">
        <label>计划号</label>
        <Input v-model:value="formState.planNo" allow-clear placeholder="可选" />
        <label>计划工序ID</label>
        <InputNumber v-model:value="formState.planOperationId" :min="1" class="w-full" placeholder="可选" />
        <label>批次号</label>
        <Input v-model:value="formState.batchNo" allow-clear placeholder="请输入批次号" />
        <label>下达时间</label>
        <DatePicker
          v-model:value="formState.issuedTime"
          format="YYYY-MM-DD HH:mm:ss"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
          class="w-full"
        />
        <label>工序</label>
        <div class="production-instruction-process-field">
          <Input
            :value="formState.operationName || formState.processName"
            readonly
            placeholder="请选择工序"
          />
          <Button @click="processPickerOpen = true">
            <template #icon><IconifyIcon icon="lucide:list-tree" /></template>
          </Button>
        </div>
        <label>工序编码</label>
        <Input v-model:value="formState.operationCode" allow-clear placeholder="选择工序后自动带入" />
        <label>指令内容</label>
        <Textarea
          v-model:value="formState.instructionContent"
          :auto-size="{ minRows: 4, maxRows: 7 }"
          class="production-instruction-form__wide"
          placeholder="请输入指令内容"
        />
        <label>备注</label>
        <Textarea
          v-model:value="formState.remark"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          class="production-instruction-form__wide"
          placeholder="可选"
        />
      </div>
    </Modal>

    <Modal v-model:open="detailVisible" :footer="null" title="生产指令详情" width="720px">
      <div v-if="currentRow" class="production-instruction-detail">
        <div><span>指令号</span><strong>{{ currentRow.instructionNo || '-' }}</strong></div>
        <div><span>状态</span><Tag :color="statusMeta(currentRow.status).color">{{ statusMeta(currentRow.status).label }}</Tag></div>
        <div><span>计划号</span><strong>{{ currentRow.planNo || '-' }}</strong></div>
        <div><span>批次号</span><strong>{{ currentRow.batchNo || '-' }}</strong></div>
        <div><span>工序</span><strong>{{ currentRow.operationName || currentRow.processName || '-' }}</strong></div>
        <div><span>下达人</span><strong>{{ currentRow.issuerName || '-' }}</strong></div>
        <div><span>下达时间</span><strong>{{ formatDateTime(currentRow.issuedTime) }}</strong></div>
        <div><span>确认人</span><strong>{{ currentRow.confirmerName || '-' }}</strong></div>
        <div><span>确认时间</span><strong>{{ formatDateTime(currentRow.confirmTime) }}</strong></div>
        <p>{{ currentRow.instructionContent }}</p>
      </div>
    </Modal>

    <Modal v-model:open="revokeVisible" title="撤下生产指令" width="520px" @ok="submitRevoke">
      <Textarea
        v-model:value="revokeReason"
        :auto-size="{ minRows: 3, maxRows: 5 }"
        placeholder="撤下原因"
      />
    </Modal>

    <PickerModal
      :config="processPickerConfig"
      :open="processPickerOpen"
      @close="processPickerOpen = false"
      @pick="handlePickProcess"
    />
  </Page>
</template>

<style scoped>
.production-instruction-list-page {
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

.production-instruction-banner {
  min-height: 78px;
  max-height: 90px;
}

.production-instruction-action-group {
  flex-wrap: nowrap;
}

.production-instruction-query-panel {
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

.production-instruction-simple-query {
  display: grid;
  grid-template-columns: 86px minmax(320px, 1fr) 94px 118px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.production-instruction-simple-query-label {
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

.production-instruction-simple-query :deep(.ant-input-affix-wrapper),
.production-instruction-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.production-instruction-simple-query :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.production-instruction-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.production-instruction-query-tag {
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

.production-instruction-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.production-instruction-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.production-instruction-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.production-instruction-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.production-instruction-tabs {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.production-instruction-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.production-instruction-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.production-instruction-list-page__content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.production-instruction-list-page__grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.production-instruction-list-page__grid-host :deep(.production-instruction-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.production-instruction-list-page__grid-host :deep(.production-instruction-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--form-wrapper),
.production-instruction-list-page__grid-host :deep(.vxe-grid--toolbar-wrapper) {
  display: none !important;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 2;
  min-height: 0 !important;
  overflow: hidden !important;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 3;
  min-height: 0;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 4;
  min-height: 0;
  background: #fff;
}

.production-instruction-list-page__grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.production-instruction-list-page__grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.production-instruction-row-actions,
.production-instruction-process-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.production-instruction-advanced-query-body {
  display: grid;
  gap: 10px;
}

.production-instruction-advanced-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.production-instruction-query-item {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.production-instruction-query-item--wide {
  grid-column: 1 / -1;
}

.production-instruction-query-item label {
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

.production-instruction-query-item > :not(label) {
  min-width: 0;
  background: #f8fafc;
}

.production-instruction-query-item :deep(.ant-input),
.production-instruction-query-item :deep(.ant-input-affix-wrapper),
.production-instruction-query-item :deep(.ant-picker) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.production-instruction-advanced-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #d7e0ea;
}

.production-instruction-form {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.production-instruction-form label {
  color: #536173;
  text-align: right;
}

.production-instruction-form__wide {
  grid-column: span 3;
}

.production-instruction-process-field {
  width: 100%;
}

.production-instruction-process-field :deep(.ant-input-affix-wrapper),
.production-instruction-process-field :deep(.ant-input) {
  flex: 1;
}

.production-instruction-detail {
  display: grid;
  gap: 10px;
}

.production-instruction-detail div {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.production-instruction-detail span {
  color: #64748b;
}

.production-instruction-detail p {
  padding: 12px;
  margin: 4px 0 0;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 1300px) {
  .production-instruction-banner {
    max-height: none;
    flex-wrap: wrap;
  }

  .production-instruction-action-group {
    width: 100%;
    justify-content: flex-end;
    padding-left: 0 !important;
    border-left: 0 !important;
  }

  .production-instruction-simple-query {
    grid-template-columns: 86px minmax(240px, 1fr) 88px 110px;
  }
}

@media (max-width: 760px) {
  .production-instruction-simple-query,
  .production-instruction-advanced-query-grid,
  .production-instruction-form {
    grid-template-columns: 1fr;
  }

  .production-instruction-query-tags {
    padding-left: 0;
  }

  .production-instruction-query-item {
    grid-template-columns: 86px minmax(0, 1fr);
  }

  .production-instruction-simple-query-label,
  .production-instruction-query-item label {
    justify-content: flex-start;
  }

  .production-instruction-form label {
    text-align: left;
  }

  .production-instruction-form__wide {
    grid-column: auto;
  }
}
</style>
