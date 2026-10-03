<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Empty, Table, Tag } from 'ant-design-vue';

import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';
import { processText } from '#/views/mes/hc/base/tooling-consumable-ledger/data';

defineOptions({ name: 'MesHcAdhesiveGlueBoardEdgeConsumableTab' });

const ADHESIVE1_PROCESS_CODE = 'ADHESIVE1';
const GLUE_BOARD_TYPE = 'GLUE_BOARD';

const props = withDefaults(
  defineProps<{
    planNo?: string;
    productionBatchNo?: string;
    readonly?: boolean;
    tableHeight?: number | string;
  }>(),
  {
    planNo: '',
    productionBatchNo: '',
    readonly: false,
    tableHeight: '100%',
  },
);

const loading = ref(false);
const rows = ref<MesHcToolingConsumableLedgerApi.Balance[]>([]);

const processName = computed(() => processText(ADHESIVE1_PROCESS_CODE) || '粘胶1');
const tableScrollY = computed(() => {
  if (typeof props.tableHeight === 'number') return props.tableHeight;
  if (!props.tableHeight || ['100%', 'auto', 'full'].includes(String(props.tableHeight))) {
    return 'calc(100% - 42px)';
  }
  const parsedHeight = Number(props.tableHeight);
  return Number.isFinite(parsedHeight) ? parsedHeight : props.tableHeight;
});

const columns: TableColumnsType<MesHcToolingConsumableLedgerApi.Balance> = [
  { dataIndex: 'model', key: 'model', title: '胶板型号', width: 130 },
  { dataIndex: 'batchNo', key: 'batchNo', title: '胶板批次', width: 160 },
  { dataIndex: 'erpMaterialCode', key: 'erpMaterialCode', title: 'ERP料号', width: 140 },
  { dataIndex: 'receiveTime', key: 'receiveTime', title: '领用时间', width: 160 },
  { align: 'right', dataIndex: 'balanceQty', key: 'balanceQty', title: '剩余量', width: 110 },
  { dataIndex: 'uomName', key: 'uomName', title: '计量单位', width: 96 },
];

function displayText(value?: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function numberText(value?: number) {
  if (value === undefined || value === null) return '0';
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

function unitText(row: MesHcToolingConsumableLedgerApi.Balance) {
  return row.uomName || row.uomCode || row.uom || '-';
}

async function loadData() {
  loading.value = true;
  try {
    const page = await getToolingConsumableBalancePage({
      consumableType: GLUE_BOARD_TYPE,
      onlyPositiveBalance: true,
      pageNo: 1,
      pageSize: 200,
      processCode: ADHESIVE1_PROCESS_CODE,
    });
    rows.value = (page?.list || (page as any)?.items || []).filter(
      (item: MesHcToolingConsumableLedgerApi.Balance) => Number(item.balanceQty || 0) > 0,
    );
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.planNo, props.productionBatchNo],
  () => {
    void loadData();
  },
);

onMounted(() => {
  void loadData();
});

defineExpose({ reload: loadData });
</script>

<template>
  <div class="adhesive-glue-board-edge-tab">
    <div class="adhesive-glue-board-edge-tab__toolbar">
      <div class="adhesive-glue-board-edge-tab__context">
        <Tag color="blue">{{ processName }}</Tag>
        <Tag color="purple">胶板</Tag>
        <span>计划号：{{ planNo || '-' }}</span>
        <span>批次号：{{ productionBatchNo || '-' }}</span>
      </div>
      <div class="adhesive-glue-board-edge-tab__actions">
        <span class="text-xs text-gray-500">领用、消耗和用完标记请在“粘胶1耗材领用台账”维护</span>
        <Button :loading="loading" size="small" @click="loadData">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新
        </Button>
      </div>
    </div>

    <Table
      bordered
      class="adhesive-glue-board-edge-tab__table"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="ledgerId"
      size="small"
      :scroll="{ x: 850, y: tableScrollY }"
    >
      <template #emptyText>
        <Empty description="暂无可用胶板领用台账" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'model'">
          {{ displayText(record.model) }}
        </template>
        <template v-else-if="column.key === 'batchNo'">
          <Tag color="blue">{{ displayText(record.batchNo) }}</Tag>
        </template>
        <template v-else-if="column.key === 'erpMaterialCode'">
          {{ displayText(record.erpMaterialCode) }}
        </template>
        <template v-else-if="column.key === 'receiveTime'">
          {{ displayText(record.receiveTime) }}
        </template>
        <template v-else-if="column.key === 'balanceQty'">
          <span class="adhesive-glue-board-edge-tab__balance">
            {{ numberText(record.balanceQty) }}
          </span>
        </template>
        <template v-else-if="column.key === 'uomName'">
          {{ unitText(record) }}
        </template>
      </template>
    </Table>
  </div>
</template>

<style scoped>
.adhesive-glue-board-edge-tab {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
  padding: 8px 0 0;
}

.adhesive-glue-board-edge-tab__toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
}

.adhesive-glue-board-edge-tab__context,
.adhesive-glue-board-edge-tab__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.adhesive-glue-board-edge-tab__context span {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.adhesive-glue-board-edge-tab__table {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.adhesive-glue-board-edge-tab__balance {
  color: #dc2626;
  font-weight: 900;
}

.adhesive-glue-board-edge-tab__row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.adhesive-glue-board-edge-tab :deep(.ant-table-thead > tr > th) {
  color: #334155;
  font-weight: 800;
  background: #f8fafc;
}

.adhesive-glue-board-edge-tab :deep(.ant-table-cell) {
  vertical-align: middle;
}

.adhesive-glue-board-edge-tab :deep(.ant-table-wrapper),
.adhesive-glue-board-edge-tab :deep(.ant-spin-nested-loading),
.adhesive-glue-board-edge-tab :deep(.ant-spin-container),
.adhesive-glue-board-edge-tab :deep(.ant-table),
.adhesive-glue-board-edge-tab :deep(.ant-table-container) {
  height: 100%;
}

.adhesive-glue-board-edge-tab :deep(.ant-table-container) {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.adhesive-glue-board-edge-tab :deep(.ant-table-body) {
  flex: 1 1 0;
  max-height: none !important;
}
</style>
