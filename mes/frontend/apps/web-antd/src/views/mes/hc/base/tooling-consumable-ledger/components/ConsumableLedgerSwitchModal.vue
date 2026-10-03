<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, reactive, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Modal as AModal,
  Pagination,
  Table as ATable,
  Tag,
} from 'ant-design-vue';

import {
  getToolingConsumableLedgerPage,
  getToolingConsumableBalancePage,
} from '#/api/mes/hc/tooling-consumable-ledger';

import { CONSUMABLE_TYPE_OPTIONS, processText } from '../data';

type LedgerRow = MesHcToolingConsumableLedgerApi.Ledger;
type LedgerSelectedRow = LedgerRow & {
  nextUsageStatus?: 'ACTIVE' | 'USED_UP';
};

type OpenOptions = {
  consumableType: string;
  processCode: string;
  onlyPositiveBalance?: boolean;
  selectionMode?: 'DIRECT' | 'SWITCH';
  title?: string;
};

const emit = defineEmits<{
  selected: [row: LedgerSelectedRow];
}>();

const visible = ref(false);
const loading = ref(false);
const rows = ref<LedgerRow[]>([]);
const currentOptions = ref<OpenOptions>();
const pageState = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
});

const columns = [
  { dataIndex: 'receiveTime', key: 'receiveTime', title: '领用时间', width: 170 },
  { dataIndex: 'batchNo', key: 'batchNo', title: '耗材批号', width: 170 },
  { dataIndex: 'model', key: 'model', title: '型号', width: 140 },
  { dataIndex: 'erpMaterialCode', key: 'erpMaterialCode', title: 'ERP料号', width: 150 },
  { dataIndex: 'balanceQty', key: 'balanceQty', title: '可用余量', width: 110 },
  { key: 'action', title: '操作', width: 96 },
];

const consumableTypeName = computed(() => {
  const type = currentOptions.value?.consumableType;
  return CONSUMABLE_TYPE_OPTIONS.find((item) => item.value === type)?.label || type || '';
});

const modalTitle = computed(() => {
  if (currentOptions.value?.title) return currentOptions.value.title;
  return `${processText(currentOptions.value?.processCode)}${consumableTypeName.value}边库台账`;
});

const directSelection = computed(() => currentOptions.value?.selectionMode === 'DIRECT');

function displayText(value?: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

async function loadRows() {
  if (!currentOptions.value) return;
  loading.value = true;
  try {
    const params = {
      consumableType: currentOptions.value.consumableType,
      pageNo: pageState.pageNo,
      pageSize: pageState.pageSize,
      processCode: currentOptions.value.processCode,
      usageStatus: 'ACTIVE',
      onlyPositiveBalance: currentOptions.value.onlyPositiveBalance,
    };
    if (currentOptions.value.onlyPositiveBalance) {
      const result = await getToolingConsumableBalancePage(params);
      rows.value = (result.list || []).map((row) => ({ ...row, id: row.ledgerId, usageStatus: 'ACTIVE' }));
      pageState.total = Number(result.total || 0);
      return;
    }
    const result = await getToolingConsumableLedgerPage(params);
    rows.value = result.list || [];
    pageState.total = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function open(options: OpenOptions) {
  currentOptions.value = options;
  rows.value = [];
  pageState.pageNo = 1;
  pageState.total = 0;
  visible.value = true;
  void loadRows();
}

function handlePageChange(pageNo: number, pageSize: number) {
  pageState.pageNo = pageNo;
  pageState.pageSize = pageSize;
  void loadRows();
}

function handleSelectRow(row: LedgerRow) {
  if (directSelection.value) {
    emit('selected', row);
    visible.value = false;
    return;
  }
  emit('selected', { ...row, nextUsageStatus: 'ACTIVE' });
  visible.value = false;
}

defineExpose({ open });
</script>

<template>
  <AModal
    v-model:open="visible"
    :footer="null"
    :title="modalTitle"
    destroy-on-close
    width="760px"
  >
    <div class="consumable-switch-modal">
      <ATable
        class="consumable-switch-modal__table"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ y: 360 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receiveTime'">
            {{ displayText(record.receiveTime) }}
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <Tag color="blue">{{ displayText(record.batchNo) }}</Tag>
          </template>
          <template v-else-if="column.key === 'model'">
            {{ displayText(record.model) }}
          </template>
          <template v-else-if="column.key === 'erpMaterialCode'">
            {{ displayText(record.erpMaterialCode) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Button size="small" type="link" :disabled="currentOptions?.onlyPositiveBalance && Number(record.balanceQty || 0) < 1" @click="handleSelectRow(record)">
              <IconifyIcon :icon="directSelection ? 'lucide:check' : 'lucide:replace'" />
              {{ directSelection ? '选择' : '更换' }}
            </Button>
          </template>
        </template>
      </ATable>
      <div class="consumable-switch-modal__pagination">
        <Pagination
          :current="pageState.pageNo"
          :page-size="pageState.pageSize"
          :show-total="(total) => `共 ${total} 条`"
          :total="pageState.total"
          show-size-changer
          size="small"
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </div>
  </AModal>
</template>

<style scoped>
.consumable-switch-modal {
  display: flex;
  height: min(58vh, 520px);
  min-height: 440px;
  flex-direction: column;
}

.consumable-switch-modal__table {
  flex: 1;
  min-height: 0;
}

.consumable-switch-modal__table :deep(.ant-spin-nested-loading),
.consumable-switch-modal__table :deep(.ant-spin-container) {
  height: 100%;
}

.consumable-switch-modal__table :deep(.ant-table) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.consumable-switch-modal__table :deep(.ant-table-container) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.consumable-switch-modal__table :deep(.ant-table-header) {
  flex-shrink: 0;
}

.consumable-switch-modal__table :deep(.ant-table-body) {
  flex: 1;
  max-height: none !important;
  min-height: 0;
  overflow-y: auto !important;
}

.consumable-switch-modal__pagination {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
  background: #fff;
}

</style>
