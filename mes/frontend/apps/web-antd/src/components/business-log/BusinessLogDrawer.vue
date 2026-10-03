<script lang="ts" setup>
import type { BusinessLogItem, BusinessLogMode } from './typing';

import { computed } from 'vue';

import { Drawer, Empty, Tag } from 'ant-design-vue';

interface BusinessLogChange {
  afterValue?: unknown;
  beforeValue?: unknown;
  field?: string;
  label?: string;
}

defineOptions({ name: 'BusinessLogDrawer' });

const props = withDefaults(
  defineProps<{
    logs?: BusinessLogItem[];
    mode?: BusinessLogMode;
    open?: boolean;
    title?: string;
  }>(),
  {
    logs: () => [],
    mode: 'audit',
    open: false,
    title: '',
  },
);

const emit = defineEmits<{
  'update:open': [open: boolean];
}>();

const visible = computed({
  get: () => props.open,
  set: (open: boolean) => emit('update:open', open),
});

const drawerTitle = computed(() => {
  if (props.title) {
    return props.title;
  }
  return props.mode === 'operation' ? '操作日志' : '审核日志';
});

const filteredLogs = computed(() => {
  const updateCodes = new Set(['SAVE', 'UPDATE']);
  return props.logs.filter((log) => {
    const actionCode = String(log.actionCode || '').toUpperCase();
    const isOperation = updateCodes.has(actionCode);
    return props.mode === 'operation' ? isOperation : !isOperation;
  });
});

function getLogChanges(log: BusinessLogItem) {
  const changes = log.businessSnapshot?.fieldChanges;
  return Array.isArray(changes) ? (changes as BusinessLogChange[]) : [];
}

function getActionTagColor(log: BusinessLogItem) {
  if (props.mode === 'operation') {
    return 'blue';
  }
  const actionCode = String(log.actionCode || '').toUpperCase();
  return actionCode.includes('RETURN') ? 'orange' : 'green';
}

function getOpinionLabel(log: BusinessLogItem) {
  const actionCode = String(log.actionCode || '').toUpperCase();
  return actionCode.includes('RETURN') ? '退回意见' : '办理意见';
}

function displayValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}
</script>

<template>
  <Drawer
    v-model:open="visible"
    :title="drawerTitle"
    placement="right"
    :width="520"
  >
    <div v-if="filteredLogs.length > 0" class="business-log-list">
      <article
        v-for="log in filteredLogs"
        :key="log.id"
        class="business-log-card"
      >
        <div class="business-log-card__head">
          <div>
            <Tag :color="getActionTagColor(log)" class="!m-0">
              {{ log.actionName || log.actionCode || '-' }}
            </Tag>
          </div>
          <span>{{ displayValue(log.handleTime) }}</span>
        </div>
        <div class="business-log-card__meta">
          <span>办理人：{{ displayValue(log.handlerUserName) }}</span>
          <span v-if="mode === 'audit'">
            节点：{{ displayValue(log.fromNodeName || log.fromStatus) }} ->
            {{ displayValue(log.toNodeName || log.toStatus) }}
          </span>
        </div>
        <div v-if="log.opinion" class="business-log-card__opinion">
          <span>{{ getOpinionLabel(log) }}：</span>
          {{ log.opinion }}
        </div>

        <div v-if="mode === 'operation'" class="business-log-change-list">
          <template v-if="getLogChanges(log).length > 0">
            <div
              v-for="change in getLogChanges(log)"
              :key="`${log.id}-${change.field || change.label}`"
              class="business-log-change"
            >
              <div class="business-log-change__label">
                {{ change.label || change.field || '字段' }}
              </div>
              <div class="business-log-change__values">
                <div>
                  <span>修改前</span>
                  <p>{{ displayValue(change.beforeValue) }}</p>
                </div>
                <div>
                  <span>修改后</span>
                  <p>{{ displayValue(change.afterValue) }}</p>
                </div>
              </div>
            </div>
          </template>
          <Empty
            v-else
            description="本次保存未检测到字段变化"
            image-style="height: 48px"
          />
        </div>
      </article>
    </div>
    <Empty
      v-else
      :description="mode === 'operation' ? '暂无操作日志' : '暂无审核日志'"
    />
  </Drawer>
</template>

<style scoped>
.business-log-list {
  display: grid;
  gap: 12px;
}

.business-log-card {
  border: 1px solid #d8e0ec;
  background: #fff;
  padding: 12px;
}

.business-log-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.business-log-card__meta {
  display: grid;
  gap: 4px;
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
}

.business-log-card__opinion {
  margin-top: 8px;
  border-left: 3px solid #bfdbfe;
  background: #f8fafc;
  padding: 6px 8px;
  color: #334155;
  font-size: 13px;
}

.business-log-card__opinion span {
  color: #64748b;
  font-weight: 700;
}

.business-log-change-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.business-log-change {
  border: 1px solid #e2e8f0;
}

.business-log-change__label {
  border-bottom: 1px solid #e2e8f0;
  background: #f1f5f9;
  padding: 6px 8px;
  color: #0f172a;
  font-weight: 700;
}

.business-log-change__values {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.business-log-change__values > div {
  min-width: 0;
  padding: 8px;
}

.business-log-change__values > div:first-child {
  border-right: 1px solid #e2e8f0;
}

.business-log-change__values span {
  color: #64748b;
  font-size: 12px;
}

.business-log-change__values p {
  margin: 4px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
