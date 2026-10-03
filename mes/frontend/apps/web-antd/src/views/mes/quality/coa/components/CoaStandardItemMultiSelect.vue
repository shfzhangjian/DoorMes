<script lang="ts" setup>
import type { MesQmsCoaApi } from '#/api/mes/quality/coa';

import { reactive, ref, watch } from 'vue';

import { Button, Input, Modal, Select, SelectOption, Table, Tag } from 'ant-design-vue';

import { getProcessPage } from '#/api/mes/base/process';
import { getCoaStandardItemPage } from '#/api/mes/quality/coa';

const props = defineProps<{
  existingItemIds?: number[];
  open: boolean;
  productModelCode?: string;
}>();

const emit = defineEmits<{
  close: [];
  select: [items: MesQmsCoaApi.StandardItem[]];
}>();

const loading = ref(false);
const rows = ref<MesQmsCoaApi.StandardItem[]>([]);
const selectedKeys = ref<number[]>([]);
const selectedRows = ref<MesQmsCoaApi.StandardItem[]>([]);
const total = ref(0);
const processLoading = ref(false);
const processOptions = ref<Array<{ label: string; value: string }>>([]);
const query = reactive<MesQmsCoaApi.StandardItemPageReq>({
  keyword: '',
  pageNo: 1,
  pageSize: 100,
  processCode: '',
  productModelCode: '',
  standardApplyType: undefined,
});

const columns = [
  { title: '标准类别', dataIndex: 'standardApplyType', width: 100 },
  { title: '工序', dataIndex: 'processName', width: 130 },
  { title: '标准编号', dataIndex: 'standardNo', width: 170 },
  { title: '版本', dataIndex: 'standardVersion', width: 85 },
  { title: '项目名称', dataIndex: 'inspectionItem', width: 180 },
  { title: '内控 Spec', dataIndex: 'standardDesc', width: 220 },
  { title: '目标值', dataIndex: 'targetValue', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 90 },
  { title: '检验方法', dataIndex: 'inspectionMethod', width: 180 },
  { title: '取值规则', dataIndex: 'ruleDescription', width: 220 },
];

async function loadProcessOptions() {
  if (processLoading.value || processOptions.value.length > 0) {
    return;
  }
  processLoading.value = true;
  try {
    const data = await getProcessPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
    });
    processOptions.value = (data.list || []).map((item) => ({
      label: `${item.code || '-'} ｜ ${item.name || '-'}`,
      value: item.code || item.name || '',
    }));
  } finally {
    processLoading.value = false;
  }
}

async function load() {
  if (!props.productModelCode) return;
  loading.value = true;
  try {
    const data = await getCoaStandardItemPage({
      ...query,
      productModelCode: props.productModelCode,
    });
    const existing = new Set(props.existingItemIds || []);
    rows.value = (data.list || []).filter((item) => !existing.has(item.id));
    total.value = Number(data.total || 0);
    selectedKeys.value = selectedKeys.value.filter((key) =>
      rows.value.some((item) => item.id === key),
    );
    selectedRows.value = selectedRows.value.filter((item) =>
      rows.value.some((row) => row.id === item.id),
    );
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.keyword = '';
  query.processCode = '';
  query.standardApplyType = undefined;
  selectedKeys.value = [];
  selectedRows.value = [];
  void load();
}

function confirm() {
  if (selectedRows.value.length === 0) return;
  emit('select', selectedRows.value);
  emit('close');
}

function handleSelectionChange(
  keys: (number | string)[],
  records: MesQmsCoaApi.StandardItem[],
) {
  selectedKeys.value = keys.map(Number);
  selectedRows.value = records;
}

watch(
  () => props.open,
  (open) => {
    if (!open) return;
    query.productModelCode = props.productModelCode || '';
    selectedKeys.value = [];
    selectedRows.value = [];
    void loadProcessOptions();
    void load();
  },
);
</script>

<template>
  <Modal
    :confirm-loading="loading"
    :open="open"
    title="选择过程检验标准明细"
    width="calc(100vw - 160px)"
    @cancel="emit('close')"
    @ok="confirm"
  >
    <div class="standard-picker-query">
      <label>产品型号</label><Input :value="productModelCode" disabled />
      <label>标准类别</label>
      <Select v-model:value="query.standardApplyType" allow-clear placeholder="全部标准">
        <SelectOption value="FAI">过程首检</SelectOption>
        <SelectOption value="FQC">成品检验</SelectOption>
      </Select>
      <label>工序</label>
      <Select
        v-model:value="query.processCode"
        :loading="processLoading"
        :options="processOptions"
        allow-clear
        option-filter-prop="label"
        placeholder="请选择工序"
        show-search
      />
      <label>关键字</label>
      <Input v-model:value="query.keyword" allow-clear placeholder="标准编号 / 项目名称" @press-enter="load" />
      <div class="standard-picker-query__actions">
        <Button type="primary" @click="load">查询</Button>
        <Button @click="reset">重置</Button>
      </div>
    </div>
    <div class="standard-picker-hint">
      已按模板产品型号过滤；已存在的标准项目不再显示。共 {{ total }} 条候选。
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :row-selection="{
        selectedRowKeys: selectedKeys,
        onChange: handleSelectionChange,
      }"
      :scroll="{ x: 1570, y: 460 }"
      bordered
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <Tag v-if="column.dataIndex === 'standardApplyType'" :color="record.standardApplyType === 'FQC' ? 'green' : 'blue'">
          {{ record.standardApplyType === 'FQC' ? '成品检验' : '过程首检' }}
        </Tag>
      </template>
    </Table>
  </Modal>
</template>

<style scoped>
.standard-picker-query {
  display: grid;
  grid-template-columns: 74px minmax(180px, 1fr) 74px minmax(150px, 0.8fr) 52px minmax(180px, 1fr) 62px minmax(180px, 1fr) max-content;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.standard-picker-query > label {
  color: #334155;
  font-weight: 700;
  text-align: right;
}

.standard-picker-query__actions { display: flex; gap: 8px; }
.standard-picker-hint { margin: 0 0 8px; color: #64748b; font-size: 12px; }
</style>
