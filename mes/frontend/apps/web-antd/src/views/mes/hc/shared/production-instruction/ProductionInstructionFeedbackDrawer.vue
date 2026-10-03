<script lang="ts" setup>
import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';
import type { ProductionInstructionContext } from './types';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Drawer, Empty, List, Modal, Spin, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { getOperationInstructionList } from '#/api/mes/hc/production-instruction';

import {
  buildOperationInstructionQuery,
  hasInstructionContext,
  instructionContextBatchNo,
} from './types';

const props = withDefaults(
  defineProps<{
    context?: ProductionInstructionContext;
    open?: boolean;
    title?: string;
  }>(),
  {
    open: false,
    title: '指令反馈',
  },
);

const emit = defineEmits<{
  'update:open': [boolean];
  loaded: [MesHcProductionInstructionApi.Instruction[]];
}>();

const visible = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

const loading = ref(false);
const detailVisible = ref(false);
const instructions = ref<MesHcProductionInstructionApi.Instruction[]>([]);
const selectedInstruction = ref<MesHcProductionInstructionApi.Instruction>();

const contextReady = computed(() => hasInstructionContext(props.context));
const contextTitle = computed(() => ({
  batchNo: instructionContextBatchNo(props.context) || '-',
  operation: props.context?.operationName || props.context?.processName || '-',
  planNo: props.context?.planNo || '-',
}));

const summary = computed(() => {
  const issued = instructions.value.filter((item) => item.status === 'ISSUED').length;
  const confirmed = instructions.value.filter((item) => item.status === 'CONFIRMED').length;
  const revoked = instructions.value.filter((item) => item.status === 'REVOKED').length;
  return { confirmed, issued, revoked, total: instructions.value.length };
});

function statusLabel(status?: string) {
  if (status === 'CONFIRMED') return '确认';
  if (status === 'REVOKED') return '撤销';
  return '下达';
}

function statusColor(status?: string) {
  if (status === 'CONFIRMED') return 'green';
  if (status === 'REVOKED') return 'red';
  return 'processing';
}

function feedbackText(item: MesHcProductionInstructionApi.Instruction) {
  if (item.status === 'CONFIRMED') {
    return `${item.confirmerName || '-'} 于 ${formatDateTime(item.confirmTime)} 确认`;
  }
  if (item.status === 'REVOKED') {
    return `${item.revokedByName || '-'} 于 ${formatDateTime(item.revokedTime)} 撤下`;
  }
  return '等待工序确认';
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function openDetail(item: MesHcProductionInstructionApi.Instruction) {
  selectedInstruction.value = item;
  detailVisible.value = true;
}

async function loadInstructions() {
  if (!contextReady.value) {
    instructions.value = [];
    emit('loaded', []);
    return;
  }
  loading.value = true;
  try {
    instructions.value = await getOperationInstructionList(
      buildOperationInstructionQuery(props.context, true),
    );
    emit('loaded', instructions.value);
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) void loadInstructions();
  },
);

watch(
  () => props.context,
  () => {
    if (props.open) void loadInstructions();
  },
  { deep: true },
);

defineExpose({ reload: loadInstructions });
</script>

<template>
  <Drawer v-model:open="visible" :title="title" width="720px">
    <div class="production-instruction-feedback">
      <div class="production-instruction-feedback__context">
        <div>
          <span>计划号</span>
          <strong>{{ contextTitle.planNo }}</strong>
        </div>
        <div>
          <span>批次号</span>
          <strong>{{ contextTitle.batchNo }}</strong>
        </div>
        <div>
          <span>工序</span>
          <strong>{{ contextTitle.operation }}</strong>
        </div>
      </div>

      <div class="production-instruction-feedback__summary">
        <Tag color="blue">总数 {{ summary.total }}</Tag>
        <Tag color="processing">待确认 {{ summary.issued }}</Tag>
        <Tag color="green">已确认 {{ summary.confirmed }}</Tag>
        <Tag color="red">已撤下 {{ summary.revoked }}</Tag>
        <Button :loading="loading" size="small" @click="loadInstructions">
          <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
          刷新
        </Button>
      </div>

      <Spin :spinning="loading">
        <List v-if="instructions.length > 0" :data-source="instructions" item-layout="vertical">
          <template #renderItem="{ item }">
            <List.Item>
              <button class="production-instruction-feedback__item" type="button" @click="openDetail(item)">
                <div class="production-instruction-feedback__item-head">
                  <Tag :color="statusColor(item.status)">{{ statusLabel(item.status) }}</Tag>
                  <strong>{{ item.instructionNo || '-' }}</strong>
                  <span>{{ formatDateTime(item.issuedTime) }}</span>
                </div>
                <p>{{ item.instructionContent }}</p>
                <div class="production-instruction-feedback__meta">
                  <span>下达人：{{ item.issuerName || '-' }}</span>
                  <span>反馈：{{ feedbackText(item) }}</span>
                </div>
              </button>
            </List.Item>
          </template>
        </List>
        <Empty v-else :description="contextReady ? '暂无生产指令反馈' : '请选择计划工序后查看反馈'" />
      </Spin>
    </div>

    <Modal v-model:open="detailVisible" :footer="null" title="指令反馈详情" width="620px">
      <div v-if="selectedInstruction" class="production-instruction-feedback-detail">
        <div><span>状态</span><Tag :color="statusColor(selectedInstruction.status)">{{ statusLabel(selectedInstruction.status) }}</Tag></div>
        <div><span>指令号</span><strong>{{ selectedInstruction.instructionNo || '-' }}</strong></div>
        <div><span>计划号</span><strong>{{ selectedInstruction.planNo || '-' }}</strong></div>
        <div><span>批次号</span><strong>{{ selectedInstruction.batchNo || '-' }}</strong></div>
        <div><span>工序</span><strong>{{ selectedInstruction.operationName || selectedInstruction.processName || '-' }}</strong></div>
        <div><span>下达人</span><strong>{{ selectedInstruction.issuerName || '-' }}</strong></div>
        <div><span>下达时间</span><strong>{{ formatDateTime(selectedInstruction.issuedTime) }}</strong></div>
        <div><span>确认人</span><strong>{{ selectedInstruction.confirmerName || '-' }}</strong></div>
        <div><span>确认时间</span><strong>{{ formatDateTime(selectedInstruction.confirmTime) }}</strong></div>
        <div><span>撤下人</span><strong>{{ selectedInstruction.revokedByName || '-' }}</strong></div>
        <div><span>撤下时间</span><strong>{{ formatDateTime(selectedInstruction.revokedTime) }}</strong></div>
        <p>{{ selectedInstruction.instructionContent }}</p>
        <p v-if="selectedInstruction.revokeReason">撤下原因：{{ selectedInstruction.revokeReason }}</p>
      </div>
    </Modal>
  </Drawer>
</template>

<style scoped>
.production-instruction-feedback {
  display: grid;
  gap: 14px;
}

.production-instruction-feedback__context {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.production-instruction-feedback__context div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.production-instruction-feedback__context span,
.production-instruction-feedback-detail span {
  color: #64748b;
}

.production-instruction-feedback__context strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.production-instruction-feedback__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.production-instruction-feedback__item {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 0;
  font: inherit;
  color: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.production-instruction-feedback__item-head,
.production-instruction-feedback__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.production-instruction-feedback__item-head span,
.production-instruction-feedback__meta {
  color: #64748b;
}

.production-instruction-feedback__item p {
  margin: 0;
  line-height: 1.65;
  color: #111827;
}

.production-instruction-feedback-detail {
  display: grid;
  gap: 12px;
}

.production-instruction-feedback-detail div {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.production-instruction-feedback-detail p {
  padding: 12px;
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 720px) {
  .production-instruction-feedback__context {
    grid-template-columns: 1fr;
  }
}
</style>
