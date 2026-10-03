<script lang="ts" setup>
import type { MesQmsCoaApi } from '#/api/mes/quality/coa';

import { reactive, ref, watch } from 'vue';

import { Button, Input, Modal, Table } from 'ant-design-vue';

import { getCoaMotherBatchPage } from '#/api/mes/quality/coa';

const props = defineProps<{
  open: boolean;
  templateId?: number;
}>();

const emit = defineEmits<{
  close: [];
  select: [batch: MesQmsCoaApi.MotherBatch];
}>();

const loading = ref(false);
const rows = ref<MesQmsCoaApi.MotherBatch[]>([]);
const selected = ref<MesQmsCoaApi.MotherBatch>();
const total = ref(0);
const query = reactive({ keyword: '', pageNo: 1, pageSize: 20 });

const columns = [
  { title: '母批号', dataIndex: 'productionBatchNo', width: 210 },
  { title: '产品料号', dataIndex: 'materialCode', width: 150 },
  { title: '产品名称', dataIndex: 'materialName', width: 200 },
  { title: '产品型号', dataIndex: 'productModelCode', width: 150 },
  { title: '生产日期', dataIndex: 'manufactureDate', width: 120 },
  { title: '最近检验时间', dataIndex: 'latestInspectionTime', width: 175 },
  { title: '检验记录数', dataIndex: 'inspectionCount', width: 105 },
];

async function load() {
  if (!props.templateId) return;
  loading.value = true;
  try {
    const data = await getCoaMotherBatchPage({
      ...query,
      templateId: props.templateId,
    });
    rows.value = data.list || [];
    total.value = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
}

function confirm() {
  if (!selected.value) return;
  emit('select', selected.value);
  emit('close');
}

watch(
  () => props.open,
  (open) => {
    if (!open) return;
    query.keyword = '';
    query.pageNo = 1;
    selected.value = undefined;
    void load();
  },
);
</script>

<template>
  <Modal
    :confirm-loading="loading"
    :open="open"
    title="选择母批号"
    width="calc(100vw - 260px)"
    @cancel="emit('close')"
    @ok="confirm"
  >
    <div class="mother-batch-modal-body">
      <div class="mother-batch-query">
        <Input
          v-model:value="query.keyword"
          allow-clear
          placeholder="母批号 / 产品料号 / 产品名称"
          @press-enter="load"
        />
        <Button type="primary" @click="load">查询</Button>
      </div>
      <div class="mother-batch-table">
        <Table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="{
            current: query.pageNo,
            pageSize: query.pageSize,
            showSizeChanger: true,
            total,
            onChange: (page: number, size: number) => {
              query.pageNo = page;
              query.pageSize = size;
              void load();
            },
          }"
          :row-selection="{
            selectedRowKeys: selected ? [selected.productionBatchNo] : [],
            type: 'radio',
            onChange: (_keys: string[], records: MesQmsCoaApi.MotherBatch[]) => {
              selected = records[0];
            },
          }"
          :scroll="{ x: 1110, y: 520 }"
          bordered
          row-key="productionBatchNo"
          size="small"
          @row-click="(row: MesQmsCoaApi.MotherBatch) => (selected = row)"
        />
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.mother-batch-modal-body {
  min-height: 620px;
}

.mother-batch-query {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.mother-batch-query :deep(.ant-input) {
  flex: 1;
}

</style>
