<script lang="ts" setup>
import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Badge, Button, Empty, List, Modal, Tag, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  confirmProductionInstruction,
  getOperationInstructionList,
} from '#/api/mes/hc/production-instruction';

const props = withDefaults(
  defineProps<{
    alwaysVisible?: boolean;
    autoRefreshMs?: number;
    batchNo?: string;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    processCode?: string;
    processName?: string;
  }>(),
  {
    alwaysVisible: false,
    autoRefreshMs: 60_000,
  },
);

const emit = defineEmits<{
  confirmed: [MesHcProductionInstructionApi.Instruction];
}>();

const loading = ref(false);
const listVisible = ref(false);
const detailVisible = ref(false);
const instructions = ref<MesHcProductionInstructionApi.Instruction[]>([]);
const selectedInstruction = ref<MesHcProductionInstructionApi.Instruction>();
let refreshTimer: ReturnType<typeof setInterval> | null = null;

const hasQueryContext = computed(() =>
  Boolean(
    props.planOperationId ||
      props.planId ||
      props.planNo ||
      props.batchNo ||
      props.processCode ||
      props.processName ||
      props.operationCode ||
      props.operationName,
  ),
);

const shouldRender = computed(
  () => instructions.value.length > 0 || props.alwaysVisible,
);

const unconfirmedCount = computed(
  () => instructions.value.filter((item) => item.status === 'ISSUED').length,
);

const rollingItems = computed(() => {
  const items = instructions.value.length > 0 ? instructions.value : [];
  return items.length > 1 ? [...items, ...items] : items;
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

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function buildQuery(): MesHcProductionInstructionApi.OperationReq {
  return {
    batchNo: props.batchNo || undefined,
    includeConfirmed: true,
    operationCode: props.operationCode || undefined,
    operationName: props.operationName || undefined,
    planId: props.planId || undefined,
    planNo: props.planNo || undefined,
    planOperationId: props.planOperationId || undefined,
    processCode: props.processCode || undefined,
    processName: props.processName || undefined,
  };
}

async function loadInstructions() {
  if (!hasQueryContext.value) {
    instructions.value = [];
    return;
  }
  loading.value = true;
  try {
    instructions.value = await getOperationInstructionList(buildQuery());
  } finally {
    loading.value = false;
  }
}

function openDetail(item: MesHcProductionInstructionApi.Instruction) {
  selectedInstruction.value = item;
  detailVisible.value = true;
}

async function handleConfirm(item: MesHcProductionInstructionApi.Instruction) {
  if (!item.id || item.status !== 'ISSUED') return;
  await confirmProductionInstruction(item.id);
  message.success('生产指令已确认');
  emit('confirmed', item);
  await loadInstructions();
}

function setupTimer() {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
  if (props.autoRefreshMs > 0) {
    refreshTimer = setInterval(() => {
      void loadInstructions();
    }, props.autoRefreshMs);
  }
}

watch(
  () => [
    props.planOperationId,
    props.planId,
    props.planNo,
    props.batchNo,
    props.processCode,
    props.processName,
    props.operationCode,
    props.operationName,
  ],
  () => {
    void loadInstructions();
  },
);

onMounted(() => {
  void loadInstructions();
  setupTimer();
});

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer);
});

defineExpose({ reload: loadInstructions });
</script>

<template>
  <section v-if="shouldRender" class="production-instruction-banner">
    <div class="production-instruction-banner__icon">
      <Badge :count="unconfirmedCount" :offset="[2, -2]">
        <IconifyIcon icon="lucide:megaphone" />
      </Badge>
    </div>
    <div class="production-instruction-banner__track" :class="{ 'is-empty': instructions.length === 0 }">
      <template v-if="instructions.length > 0">
        <div class="production-instruction-banner__roll" :class="{ 'is-static': instructions.length === 1 }">
          <button
            v-for="(item, index) in rollingItems"
            :key="`${item.id || 'instruction'}-${index}`"
            class="production-instruction-banner__item"
            type="button"
            @click="openDetail(item)"
          >
            <Tag :color="statusColor(item.status)" class="!m-0">{{ statusLabel(item.status) }}</Tag>
            <span class="production-instruction-banner__batch">{{ item.batchNo || '-' }}</span>
            <span class="production-instruction-banner__content">{{ item.instructionContent }}</span>
            <span class="production-instruction-banner__time">{{ formatDateTime(item.issuedTime) }}</span>
          </button>
        </div>
      </template>
      <span v-else class="production-instruction-banner__placeholder">暂无生产指令</span>
    </div>
    <div class="production-instruction-banner__actions">
      <Button :loading="loading" size="small" type="text" @click="loadInstructions">
        <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
      </Button>
      <Button size="small" type="link" @click="listVisible = true">更多</Button>
    </div>
  </section>

  <Modal v-model:open="listVisible" :footer="null" title="生产指令" width="760px">
    <List :data-source="instructions" item-layout="vertical">
      <template #renderItem="{ item }">
        <List.Item>
          <div class="production-instruction-list-item">
            <div class="production-instruction-list-item__main" @click="openDetail(item)">
              <div>
                <Tag :color="statusColor(item.status)">{{ statusLabel(item.status) }}</Tag>
                <strong>{{ item.batchNo || '-' }}</strong>
                <span>{{ item.operationName || item.processName || '-' }}</span>
              </div>
              <p>{{ item.instructionContent }}</p>
              <small>{{ item.issuerName || '-' }} · {{ formatDateTime(item.issuedTime) }}</small>
            </div>
            <Button
              v-if="item.status === 'ISSUED'"
              size="small"
              type="primary"
              @click="handleConfirm(item)"
            >
              确认
            </Button>
          </div>
        </List.Item>
      </template>
      <template #empty>
        <Empty description="暂无生产指令" />
      </template>
    </List>
  </Modal>

  <Modal v-model:open="detailVisible" :footer="null" title="指令详情" width="620px">
    <div v-if="selectedInstruction" class="production-instruction-detail">
      <div><span>状态</span><Tag :color="statusColor(selectedInstruction.status)">{{ statusLabel(selectedInstruction.status) }}</Tag></div>
      <div><span>批次号</span><strong>{{ selectedInstruction.batchNo || '-' }}</strong></div>
      <div><span>计划号</span><strong>{{ selectedInstruction.planNo || '-' }}</strong></div>
      <div><span>工序</span><strong>{{ selectedInstruction.operationName || selectedInstruction.processName || '-' }}</strong></div>
      <div><span>下达人</span><strong>{{ selectedInstruction.issuerName || '-' }}</strong></div>
      <div><span>下达时间</span><strong>{{ formatDateTime(selectedInstruction.issuedTime) }}</strong></div>
      <div><span>确认人</span><strong>{{ selectedInstruction.confirmerName || '-' }}</strong></div>
      <div><span>确认时间</span><strong>{{ formatDateTime(selectedInstruction.confirmTime) }}</strong></div>
      <p>{{ selectedInstruction.instructionContent }}</p>
      <Button
        v-if="selectedInstruction.status === 'ISSUED'"
        type="primary"
        @click="handleConfirm(selectedInstruction)"
      >
        确认
      </Button>
    </div>
  </Modal>
</template>

<style scoped>
.production-instruction-banner {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) max-content;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 8px 12px;
  overflow: hidden;
  color: #17324d;
  background: #f0f7ff;
  border: 1px solid #badbff;
  border-radius: 8px;
}

.production-instruction-banner__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #0958d9;
  background: #d9ecff;
  border-radius: 8px;
}

.production-instruction-banner__track {
  position: relative;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.production-instruction-banner__track.is-empty {
  color: #6b7280;
}

.production-instruction-banner__roll {
  display: inline-flex;
  gap: 24px;
  min-width: max-content;
  animation: production-instruction-roll 28s linear infinite;
}

.production-instruction-banner__roll.is-static {
  animation: none;
}

.production-instruction-banner__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0;
  font: inherit;
  color: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.production-instruction-banner__batch {
  font-weight: 700;
  color: #0f172a;
}

.production-instruction-banner__content {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.production-instruction-banner__time,
.production-instruction-banner__placeholder {
  color: #64748b;
}

.production-instruction-banner__actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.production-instruction-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.production-instruction-list-item__main {
  min-width: 0;
  cursor: pointer;
}

.production-instruction-list-item__main div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.production-instruction-list-item__main p {
  margin: 8px 0 4px;
  color: #1f2937;
}

.production-instruction-list-item__main small {
  color: #64748b;
}

.production-instruction-detail {
  display: grid;
  gap: 12px;
}

.production-instruction-detail div {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.production-instruction-detail span {
  color: #64748b;
}

.production-instruction-detail p {
  padding: 12px;
  margin: 4px 0 0;
  line-height: 1.7;
  color: #111827;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@keyframes production-instruction-roll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@media (max-width: 720px) {
  .production-instruction-banner {
    grid-template-columns: 36px minmax(0, 1fr);
  }

  .production-instruction-banner__actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
