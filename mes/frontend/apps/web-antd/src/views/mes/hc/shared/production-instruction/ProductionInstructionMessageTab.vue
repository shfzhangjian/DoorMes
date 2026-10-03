<script lang="ts" setup>
import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';
import type { ProductionInstructionContext } from './types';

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Empty, Input, Modal, Pagination, Select, Table as ATable, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getProductionInstructionMessagePage,
  getProductionInstructionUnreadCount,
} from '#/api/mes/hc/production-instruction';

const props = withDefaults(
  defineProps<{
    autoRefreshMs?: number;
    context?: ProductionInstructionContext;
    title?: string;
  }>(),
  {
    autoRefreshMs: 60_000,
    title: '指令消息',
  },
);

const emit = defineEmits<{
  'unread-change': [number];
}>();

const loading = ref(false);
const rows = ref<MesHcProductionInstructionApi.Instruction[]>([]);
const total = ref(0);
const unreadCount = ref(0);
const selectedInstruction = ref<MesHcProductionInstructionApi.Instruction>();
const detailVisible = ref(false);
let refreshTimer: ReturnType<typeof setInterval> | null = null;

const filters = reactive({
  instructionType: '',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
});

const hasContext = computed(() =>
  Boolean(
    props.context?.operationCode ||
      props.context?.operationName ||
      props.context?.processCode ||
      props.context?.processName,
  ),
);

const columns = [
  { dataIndex: 'instructionType', title: '类型', width: 92 },
  { dataIndex: 'status', title: '状态', width: 92 },
  { dataIndex: 'planNo', title: '计划号', width: 150 },
  { dataIndex: 'operationName', title: '工序', width: 110 },
  { dataIndex: 'instructionContent', title: '指令内容', width: 360 },
  { dataIndex: 'issuerName', title: '下达人', width: 110 },
  { dataIndex: 'issuedTime', title: '下达时间', width: 150 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 116 },
];

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已下达', value: 'ISSUED' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已撤下', value: 'REVOKED' },
];

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '日常指令', value: 'DAILY' },
  { label: '暂停', value: 'PAUSE' },
  { label: '复工', value: 'RESUME' },
  { label: '作废取消', value: 'CANCEL' },
  { label: '换型', value: 'CHANGEOVER' },
  { label: '冻结', value: 'FREEZE_STOCK' },
  { label: '解冻', value: 'UNFREEZE_STOCK' },
];

function instructionTypeMeta(type?: string) {
  if (type === 'PAUSE') return { color: 'warning', text: '暂停' };
  if (type === 'RESUME') return { color: 'success', text: '复工' };
  if (type === 'CANCEL') return { color: 'error', text: '作废取消' };
  if (type === 'CHANGEOVER') return { color: 'processing', text: '换型' };
  if (type === 'FREEZE_STOCK') return { color: 'purple', text: '冻结' };
  if (type === 'UNFREEZE_STOCK') return { color: 'cyan', text: '解冻' };
  return { color: 'processing', text: '日常' };
}

function instructionStatusMeta(status?: string) {
  if (status === 'CONFIRMED') return { color: 'green', text: '已确认' };
  if (status === 'REVOKED') return { color: 'red', text: '已撤下' };
  return { color: 'blue', text: '已下达' };
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function buildBaseParams(): MesHcProductionInstructionApi.MessagePageReqVO {
  const context = props.context || {};
  const params: MesHcProductionInstructionApi.MessagePageReqVO = {
    instructionType: filters.instructionType || undefined,
    keyword: filters.keyword || undefined,
    operationCode: context.operationCode || undefined,
    operationName: context.operationName || undefined,
    pageNo: filters.pageNo,
    pageSize: filters.pageSize,
    planId: context.planId || undefined,
    planNo: context.planNo || undefined,
    planOperationId: context.planOperationId || undefined,
    processCode: context.processCode || undefined,
    processName: context.processName || undefined,
    status: filters.status || undefined,
  };
  return params;
}

function buildUnreadParams(): MesHcProductionInstructionApi.MessagePageReqVO {
  const context = props.context || {};
  const params: MesHcProductionInstructionApi.MessagePageReqVO = {
    operationCode: context.operationCode || undefined,
    operationName: context.operationName || undefined,
    pageNo: 1,
    pageSize: 1,
    planId: context.planId || undefined,
    planNo: context.planNo || undefined,
    planOperationId: context.planOperationId || undefined,
    processCode: context.processCode || undefined,
    processName: context.processName || undefined,
  };
  return params;
}

async function loadUnreadCount() {
  if (!hasContext.value) {
    unreadCount.value = 0;
    emit('unread-change', 0);
    return;
  }
  const count = await getProductionInstructionUnreadCount(buildUnreadParams());
  unreadCount.value = Number(count || 0);
  emit('unread-change', unreadCount.value);
}

async function loadMessages() {
  if (!hasContext.value) {
    rows.value = [];
    total.value = 0;
    await loadUnreadCount();
    return;
  }
  loading.value = true;
  try {
    const result = await getProductionInstructionMessagePage(buildBaseParams());
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
    await loadUnreadCount();
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  filters.pageNo = 1;
  void loadMessages();
}

function resetFilters() {
  filters.instructionType = '';
  filters.keyword = '';
  filters.pageNo = 1;
  filters.status = '';
  void loadMessages();
}

function openDetail(item: MesHcProductionInstructionApi.Instruction) {
  selectedInstruction.value = item;
  detailVisible.value = true;
}

function setupTimer() {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
  if (props.autoRefreshMs > 0) {
    refreshTimer = setInterval(() => {
      void loadMessages();
    }, props.autoRefreshMs);
  }
}

watch(
  () => [
    props.context?.operationCode,
    props.context?.operationName,
    props.context?.processCode,
    props.context?.processName,
  ],
  () => {
    filters.pageNo = 1;
    void loadMessages();
  },
);

onMounted(() => {
  void loadMessages();
  setupTimer();
});

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer);
});

defineExpose({ loadMessages, loadUnreadCount });
</script>

<template>
  <section class="production-instruction-message-tab">
    <div class="production-instruction-message-tab__toolbar">
      <div class="production-instruction-message-tab__title">
        <IconifyIcon icon="lucide:message-square-text" />
        <span>{{ title }}</span>
        <Tag v-if="unreadCount > 0" color="red">{{ unreadCount }} 条指令</Tag>
      </div>
      <div class="production-instruction-message-tab__filters">
        <Select v-model:value="filters.instructionType" :options="typeOptions" size="small" />
        <Select v-model:value="filters.status" :options="statusOptions" size="small" />
        <Input
          v-model:value="filters.keyword"
          allow-clear
          placeholder="计划号/批号/内容"
          size="small"
          @press-enter="handleSearch"
        />
        <Button :loading="loading" size="small" type="primary" @click="handleSearch">查询</Button>
        <Button size="small" @click="resetFilters">重置</Button>
      </div>
    </div>

    <template v-if="hasContext">
      <ATable
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :row-key="(record) => record.id || record.instructionNo"
        :scroll="{ x: 1220, y: '100%' }"
        class="production-instruction-message-tab__table"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'instructionType'">
            <Tag :color="instructionTypeMeta(record.instructionType).color">
              {{ instructionTypeMeta(record.instructionType).text }}
            </Tag>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <Tag :color="instructionStatusMeta(record.status).color">
              {{ instructionStatusMeta(record.status).text }}
            </Tag>
          </template>
          <template v-if="column.dataIndex === 'instructionContent'">
            <button class="production-instruction-message-tab__content" type="button" @click="openDetail(record)">
              {{ record.instructionContent || '-' }}
            </button>
          </template>
          <template v-if="column.dataIndex === 'issuedTime'">
            {{ formatDateTime(record.issuedTime) }}
          </template>
          <template v-if="column.dataIndex === 'action'">
            <Button size="small" type="link" @click="openDetail(record)">详情</Button>
          </template>
        </template>
      </ATable>
      <div class="production-instruction-message-tab__pagination">
        <Pagination
          v-model:current="filters.pageNo"
          v-model:pageSize="filters.pageSize"
          :page-size-options="['10', '20', '50']"
          :show-total="(value) => `共 ${value} 条`"
          :total="total"
          show-less-items
          show-size-changer
          size="small"
          @change="loadMessages"
          @show-size-change="loadMessages"
        />
      </div>
    </template>
    <Empty v-else description="请先选择当前工序任务" />

    <Modal v-model:open="detailVisible" :footer="null" title="指令消息详情" width="680px">
      <div v-if="selectedInstruction" class="production-instruction-message-detail">
        <div><span>类型</span><Tag :color="instructionTypeMeta(selectedInstruction.instructionType).color">{{ instructionTypeMeta(selectedInstruction.instructionType).text }}</Tag></div>
        <div><span>计划号</span><strong>{{ selectedInstruction.planNo || '-' }}</strong></div>
        <div><span>批次号</span><strong>{{ selectedInstruction.batchNo || '-' }}</strong></div>
        <div><span>工序</span><strong>{{ selectedInstruction.operationName || selectedInstruction.processName || '-' }}</strong></div>
        <div><span>下达人</span><strong>{{ selectedInstruction.issuerName || '-' }}</strong></div>
        <div><span>下达时间</span><strong>{{ formatDateTime(selectedInstruction.issuedTime) }}</strong></div>
        <p>{{ selectedInstruction.instructionContent || '-' }}</p>
      </div>
    </Modal>
  </section>
</template>

<style scoped>
.production-instruction-message-tab {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.production-instruction-message-tab__toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0 6px;
}

.production-instruction-message-tab__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 150px;
  font-weight: 700;
  color: #0f172a;
}

.production-instruction-message-tab__filters {
  display: grid;
  grid-template-columns: 120px 110px minmax(180px, 260px) max-content max-content;
  gap: 8px;
  align-items: center;
}

.production-instruction-message-tab__table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #d7e3f1;
}

.production-instruction-message-tab__table :deep(.ant-spin-nested-loading),
.production-instruction-message-tab__table :deep(.ant-spin-container),
.production-instruction-message-tab__table :deep(.ant-table),
.production-instruction-message-tab__table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.production-instruction-message-tab__table :deep(.ant-table) {
  font-size: 12px;
}

.production-instruction-message-tab__table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto !important;
}

.production-instruction-message-tab__table :deep(.ant-table-thead > tr > th),
.production-instruction-message-tab__table :deep(.ant-table-tbody > tr > td) {
  padding: 6px 8px;
}

.production-instruction-message-tab__content {
  max-width: 330px;
  padding: 0;
  overflow: hidden;
  color: #075985;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.production-instruction-message-tab__pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 2px;
}

.production-instruction-message-tab > :deep(.ant-empty) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.production-instruction-message-detail {
  display: grid;
  gap: 12px;
}

.production-instruction-message-detail div {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.production-instruction-message-detail span {
  color: #64748b;
}

.production-instruction-message-detail p {
  padding: 12px;
  margin: 4px 0 0;
  line-height: 1.7;
  color: #111827;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 980px) {
  .production-instruction-message-tab__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .production-instruction-message-tab__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
