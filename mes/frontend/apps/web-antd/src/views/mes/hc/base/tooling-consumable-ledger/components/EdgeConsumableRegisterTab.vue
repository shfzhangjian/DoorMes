<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Empty, Table, Tag, Tooltip, message } from 'ant-design-vue';

import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';

import {
  CONSUMABLE_TYPE_OPTIONS,
  normalizeProcessCode,
  processText,
} from '../data';
import ToolingConsumableUsedUpMarkModal from './ToolingConsumableUsedUpMarkModal.vue';
import ConsumeForm from '../modules/consume-form.vue';
import LedgerForm from '../modules/ledger-form.vue';

defineOptions({ name: 'MesHcEdgeConsumableRegisterTab' });

const props = withDefaults(
  defineProps<{
    planNo?: string;
    processCode: string;
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

const emit = defineEmits<{
  consumed: [];
}>();

const loading = ref(false);
const rows = ref<MesHcToolingConsumableLedgerApi.Balance[]>([]);
const usedUpMarkModalRef = ref<InstanceType<typeof ToolingConsumableUsedUpMarkModal>>();

const normalizedProcessCode = computed(() => normalizeProcessCode(props.processCode) || props.processCode);
const normalizedProcessName = computed(() => processText(normalizedProcessCode.value) || props.processCode);
const tableScrollY = computed(() => {
  if (typeof props.tableHeight === 'number') return props.tableHeight;
  if (!props.tableHeight || ['100%', 'auto', 'full'].includes(String(props.tableHeight))) {
    return 'calc(100% - 42px)';
  }
  const parsedHeight = Number(props.tableHeight);
  return Number.isFinite(parsedHeight) ? parsedHeight : props.tableHeight;
});

const columns: TableColumnsType<MesHcToolingConsumableLedgerApi.Balance> = [
  { dataIndex: 'consumableType', key: 'consumableType', title: '耗材种类', width: 110 },
  { dataIndex: 'model', key: 'model', title: '型号', width: 140 },
  { dataIndex: 'batchNo', key: 'batchNo', title: '耗材批次', width: 170 },
  { align: 'right', dataIndex: 'balanceQty', key: 'balanceQty', title: '剩余量', width: 110 },
  { dataIndex: 'uomName', key: 'uomName', title: '计量单位', width: 96 },
  { fixed: 'right', key: 'action', title: '操作', width: 184 },
];

const [ConsumeFormModal, consumeFormModalApi] = useVbenModal({
  connectedComponent: ConsumeForm,
  destroyOnClose: true,
});

const [LedgerFormModal, ledgerFormModalApi] = useVbenModal({
  connectedComponent: LedgerForm,
  destroyOnClose: true,
});

function optionText(value?: string) {
  return CONSUMABLE_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-';
}

function unitText(row: MesHcToolingConsumableLedgerApi.Balance) {
  return row.uomName || row.uomCode || row.uom || '-';
}

function numberText(value?: number) {
  if (value === undefined || value === null) return '0';
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 3 });
}

async function loadData() {
  if (!normalizedProcessCode.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const page = await getToolingConsumableBalancePage({
      onlyPositiveBalance: true,
      pageNo: 1,
      pageSize: 200,
      processCode: normalizedProcessCode.value,
    });
    rows.value = (page?.list || (page as any)?.items || []).filter(
      (item: MesHcToolingConsumableLedgerApi.Balance) => Number(item.balanceQty || 0) > 0,
    );
  } finally {
    loading.value = false;
  }
}

function handleRegister(row: MesHcToolingConsumableLedgerApi.Balance) {
  if (props.readonly) {
    message.warning('当前报工状态不允许登记消耗');
    return;
  }
  if (!row.ledgerId) {
    message.warning('当前耗材批次缺少领用台账ID');
    return;
  }
  consumeFormModalApi
    .setData({
      batchNo: row.batchNo,
      consumableType: row.consumableType,
      fixedProcessCode: normalizedProcessCode.value,
      ledgerId: row.ledgerId,
      model: row.model,
      planNo: props.planNo,
      processCode: row.processCode || normalizedProcessCode.value,
      productionBatchNo: props.productionBatchNo,
    })
    .open();
}

function handleCreateLedger() {
  if (!normalizedProcessCode.value) {
    message.warning('当前工序为空，无法新增领用');
    return;
  }
  ledgerFormModalApi
    .setData({
      fixedProcessCode: normalizedProcessCode.value,
      processCode: normalizedProcessCode.value,
    })
    .open();
}

function handleMarkComplete(row: MesHcToolingConsumableLedgerApi.Balance) {
  if (props.readonly) {
    message.warning('当前报工状态不允许标记完成');
    return;
  }
  if (!row.ledgerId) {
    message.warning('当前耗材批次缺少领用台账ID');
    return;
  }
  usedUpMarkModalRef.value?.open(row);
}

async function handleSuccess() {
  await loadData();
  emit('consumed');
}

async function handleMarkCompleteSuccess() {
  await loadData();
  emit('consumed');
}

async function handleLedgerSuccess() {
  await loadData();
}

watch(
  () => [props.processCode, props.planNo, props.productionBatchNo],
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
  <div class="edge-consumable-register">
    <ConsumeFormModal @success="handleSuccess" />
    <LedgerFormModal @success="handleLedgerSuccess" />
    <ToolingConsumableUsedUpMarkModal
      ref="usedUpMarkModalRef"
      @success="handleMarkCompleteSuccess"
    />
    <div class="edge-consumable-register__toolbar">
      <div class="edge-consumable-register__context">
        <Tag color="blue">{{ normalizedProcessName }}</Tag>
        <span>计划号：{{ planNo || '-' }}</span>
        <span>批次号：{{ productionBatchNo || '-' }}</span>
      </div>
      <div class="edge-consumable-register__actions">
        <Button
          :disabled="!normalizedProcessCode"
          size="small"
          type="primary"
          @click="handleCreateLedger"
        >
          <template #icon>
            <IconifyIcon icon="lucide:plus" />
          </template>
          新建领用
        </Button>
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
      class="edge-consumable-register__table"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="ledgerId"
      size="small"
      :scroll="{ y: tableScrollY }"
    >
      <template #emptyText>
        <Empty description="暂无可登记耗材" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'consumableType'">
          {{ optionText(record.consumableType) }}
        </template>
        <template v-else-if="column.key === 'model'">
          {{ record.model || '-' }}
        </template>
        <template v-else-if="column.key === 'batchNo'">
          <Tooltip :title="record.batchNo || '-'">
            <span class="edge-consumable-register__batch">{{ record.batchNo || '-' }}</span>
          </Tooltip>
        </template>
        <template v-else-if="column.key === 'balanceQty'">
          <span class="edge-consumable-register__balance">{{ numberText(record.balanceQty) }}</span>
        </template>
        <template v-else-if="column.key === 'uomName'">
          {{ unitText(record) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="edge-consumable-register__row-actions">
            <Button
              :disabled="readonly || Number(record.balanceQty || 0) <= 0"
              size="small"
              type="link"
              @click="handleRegister(record)"
            >
              登记消耗
            </Button>
            <Button
              danger
              :disabled="readonly"
              size="small"
              type="link"
              @click="handleMarkComplete(record)"
            >
              标记完成
            </Button>
          </div>
        </template>
      </template>
    </Table>
  </div>
</template>

<style scoped>
.edge-consumable-register {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 8px 0 0;
}

.edge-consumable-register__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  margin-bottom: 8px;
}

.edge-consumable-register__context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  min-width: 0;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.edge-consumable-register__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.edge-consumable-register__table {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.edge-consumable-register__batch {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.edge-consumable-register__balance {
  color: #cf1322;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
}

.edge-consumable-register__row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.edge-consumable-register :deep(.ant-table-thead > tr > th) {
  color: #334155;
  font-weight: 800;
  background: #f8fafc;
}

.edge-consumable-register :deep(.ant-table-cell) {
  vertical-align: middle;
}

.edge-consumable-register :deep(.ant-table-wrapper),
.edge-consumable-register :deep(.ant-spin-nested-loading),
.edge-consumable-register :deep(.ant-spin-container),
.edge-consumable-register :deep(.ant-table),
.edge-consumable-register :deep(.ant-table-container) {
  height: 100%;
}

.edge-consumable-register :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.edge-consumable-register :deep(.ant-table-body) {
  flex: 1 1 0;
  max-height: none !important;
}
</style>
