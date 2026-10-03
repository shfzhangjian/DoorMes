<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import type { MesHcWetReportApi } from '#/api/mes/hc/execution/wet-report';

import { computed, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Empty, Table as ATable, Tag, Tooltip, message } from 'ant-design-vue';

import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';
import {
  consumableTypeOptionsByProcess,
  CONSUMABLE_TYPE_OPTIONS,
} from '#/views/mes/hc/base/tooling-consumable-ledger/data';
import ToolingConsumableUsedUpMarkModal from '#/views/mes/hc/base/tooling-consumable-ledger/components/ToolingConsumableUsedUpMarkModal.vue';
import ConsumeForm from '#/views/mes/hc/base/tooling-consumable-ledger/modules/consume-form.vue';
import LedgerForm from '#/views/mes/hc/base/tooling-consumable-ledger/modules/ledger-form.vue';

defineOptions({ name: 'MesHcWetEdgeConsumableRegisterTab' });

const WET_PROCESS_CODE = 'WET';

const props = withDefaults(
  defineProps<{
    readonly?: boolean;
    tableHeight?: number | string;
    task?: MesHcWetReportApi.TaskItem | Record<string, any> | null;
  }>(),
  {
    readonly: false,
    tableHeight: '100%',
    task: null,
  },
);

const emit = defineEmits<{
  consumed: [];
}>();

const loading = ref(false);
const rows = ref<MesHcToolingConsumableLedgerApi.Balance[]>([]);
const usedUpMarkModalRef = ref<InstanceType<typeof ToolingConsumableUsedUpMarkModal>>();

const planNo = computed(() => String(props.task?.planNo || props.task?.id || '').trim());
const productionBatchNo = computed(() =>
  String(props.task?.productionBatchNo || props.task?.batchNo || props.task?.parentProductionBatchNo || '').trim(),
);
const isReadonly = computed(() => props.readonly || props.task?.status === 'COMPLETED');
const wetConsumableTypeOptions = computed(() => consumableTypeOptionsByProcess(WET_PROCESS_CODE));
const tableScrollY = computed(() => {
  if (typeof props.tableHeight === 'number') return props.tableHeight;
  if (!props.tableHeight || ['100%', 'auto', 'full'].includes(String(props.tableHeight))) return 420;
  const parsedHeight = Number(props.tableHeight);
  return Number.isFinite(parsedHeight) ? parsedHeight : 420;
});

const columns: TableColumnsType<MesHcToolingConsumableLedgerApi.Balance> = [
  { dataIndex: 'consumableType', key: 'consumableType', title: '耗材种类', width: 110 },
  { dataIndex: 'model', key: 'model', title: '型号', width: 140 },
  { dataIndex: 'batchNo', key: 'batchNo', title: '耗材批次', width: 180 },
  { dataIndex: 'erpMaterialCode', key: 'erpMaterialCode', title: 'ERP料号', width: 140 },
  { align: 'right', dataIndex: 'balanceQty', key: 'balanceQty', title: '剩余量', width: 110 },
  { dataIndex: 'uomName', key: 'uomName', title: '单位', width: 80 },
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
  loading.value = true;
  try {
    const page = await getToolingConsumableBalancePage({
      onlyPositiveBalance: true,
      pageNo: 1,
      pageSize: 200,
      processCode: WET_PROCESS_CODE,
    });
    rows.value = (page?.list || (page as any)?.items || []).filter(
      (item: MesHcToolingConsumableLedgerApi.Balance) => Number(item.balanceQty || 0) > 0,
    );
  } finally {
    loading.value = false;
  }
}

function handleCreateLedger() {
  ledgerFormModalApi
    .setData({
      fixedProcessCode: WET_PROCESS_CODE,
      processCode: WET_PROCESS_CODE,
    })
    .open();
}

function handleRegister(row: MesHcToolingConsumableLedgerApi.Balance) {
  if (isReadonly.value) {
    message.warning('当前湿法报工状态不允许登记消耗');
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
      fixedProcessCode: WET_PROCESS_CODE,
      ledgerId: row.ledgerId,
      model: row.model,
      planNo: planNo.value,
      processCode: WET_PROCESS_CODE,
      productionBatchNo: productionBatchNo.value,
    })
    .open();
}

function handleMarkComplete(row: MesHcToolingConsumableLedgerApi.Balance) {
  if (isReadonly.value) {
    message.warning('当前湿法报工状态不允许标记完成');
    return;
  }
  if (!row.ledgerId) {
    message.warning('当前耗材批次缺少领用台账ID');
    return;
  }
  usedUpMarkModalRef.value?.open(row);
}

async function handleConsumeSuccess() {
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
  () => [props.task?.planNo, props.task?.productionBatchNo, props.task?.batchNo],
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
  <div class="wet-edge-consumable">
    <ConsumeFormModal @success="handleConsumeSuccess" />
    <LedgerFormModal @success="handleLedgerSuccess" />
    <ToolingConsumableUsedUpMarkModal
      ref="usedUpMarkModalRef"
      @success="handleMarkCompleteSuccess"
    />

    <div class="wet-edge-consumable__toolbar">
      <div class="wet-edge-consumable__context">
        <Tag color="blue">湿法</Tag>
        <span>计划号：{{ planNo || '-' }}</span>
        <span>母批批号：{{ productionBatchNo || '-' }}</span>
        <span v-if="wetConsumableTypeOptions.length">
          耗材：{{ wetConsumableTypeOptions.map((item) => item.label).join('、') }}
        </span>
      </div>
      <div class="wet-edge-consumable__actions">
        <Button :disabled="isReadonly" size="small" type="primary" @click="handleCreateLedger">
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

    <div class="wet-edge-consumable__table-host">
      <ATable
        bordered
        class="wet-edge-consumable__table"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        row-key="ledgerId"
        size="small"
        :scroll="{ x: 880, y: tableScrollY }"
      >
        <template #emptyText>
          <Empty description="暂无湿法边库耗材" />
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
              <span class="wet-edge-consumable__batch">{{ record.batchNo || '-' }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'erpMaterialCode'">
            {{ record.erpMaterialCode || '-' }}
          </template>
          <template v-else-if="column.key === 'balanceQty'">
            <span class="wet-edge-consumable__balance">{{ numberText(record.balanceQty) }}</span>
          </template>
          <template v-else-if="column.key === 'uomName'">
            {{ unitText(record) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="wet-edge-consumable__row-actions">
              <Button
                :disabled="isReadonly || Number(record.balanceQty || 0) <= 0"
                size="small"
                type="link"
                @click="handleRegister(record)"
              >
                登记消耗
              </Button>
              <Button
                danger
                :disabled="isReadonly"
                size="small"
                type="link"
                @click="handleMarkComplete(record)"
              >
                标记完成
              </Button>
            </div>
          </template>
        </template>
      </ATable>
    </div>
  </div>
</template>

<style scoped>
.wet-edge-consumable {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 8px;
  background: #f5f7fa;
}

.wet-edge-consumable__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  margin-bottom: 8px;
  padding: 0 2px;
}

.wet-edge-consumable__context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  min-width: 0;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.wet-edge-consumable__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.wet-edge-consumable__table-host {
  flex: 1 1 auto;
  min-height: 320px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d9e2ec;
}

.wet-edge-consumable__table {
  height: 100%;
}

.wet-edge-consumable__batch {
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.wet-edge-consumable__balance {
  color: #cf1322;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
}

.wet-edge-consumable__row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.wet-edge-consumable :deep(.ant-table-thead > tr > th) {
  color: #334155;
  font-weight: 800;
  background: #f8fafc;
}

.wet-edge-consumable :deep(.ant-table-cell) {
  vertical-align: middle;
}

.wet-edge-consumable :deep(.ant-table-placeholder .ant-empty) {
  margin: 96px 0;
}
</style>
