<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Form, Input, message, Table } from 'ant-design-vue';

import { getRawMaterialNcrSourceInspectionPage } from '#/api/mes/quality/abnormal/raw-material-ncr';

type IqcSourceRow = MesNcrApi.RawMaterialInspection & {
  key: string;
};

defineOptions({ name: 'RawMaterialNcrIqcSourceSelectModal' });

const emit = defineEmits<{
  select: [row: MesNcrApi.RawMaterialInspection];
}>();

const loading = ref(false);
const rows = ref<IqcSourceRow[]>([]);
const selectedRowKey = ref<string>();

const searchState = reactive({
  inspectionNo: '',
  lotNo: '',
  materialCode: '',
  supplierName: '',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const columns: TableColumnsType<IqcSourceRow> = [
  { dataIndex: 'inspectionNo', title: 'IQC单号', width: 170 },
  { dataIndex: 'inspectionTime', title: '检验时间', width: 170 },
  { dataIndex: 'supplierName', title: '供应商', width: 170 },
  { dataIndex: 'materialName', title: '物料', width: 260 },
  { dataIndex: 'lotNo', title: '批号', width: 150 },
  { align: 'right', dataIndex: 'quantity', title: '到货数量', width: 120 },
  { dataIndex: 'qaInspectorName', title: '审核人', width: 120 },
  { dataIndex: 'qaTime', title: '审核时间', width: 170 },
];

const selectedRow = computed(() =>
  rows.value.find((row) => row.key === selectedRowKey.value),
);

const rowSelection = computed(() => ({
  getCheckboxProps: (record: IqcSourceRow) => ({
    disabled: Boolean(record.ncrGenerated),
  }),
  onChange: (keys: Array<number | string>) => {
    selectedRowKey.value = keys[0] === undefined ? undefined : String(keys[0]);
  },
  selectedRowKeys: selectedRowKey.value ? [selectedRowKey.value] : [],
  type: 'radio' as const,
}));

const [Modal, modalApi] = useVbenModal({
  class: 'w-[1360px] max-w-[96vw]',
  closeOnClickModal: false,
  title: '选择 IQC 进料检验记录',
  async onConfirm() {
    if (!selectedRow.value) {
      message.warning('请选择一条 IQC 进料检验记录');
      return;
    }
    emit('select', selectedRow.value);
    await modalApi.close();
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      resetState();
      return;
    }
    resetState();
    await loadRows();
  },
});

function resetState() {
  rows.value = [];
  selectedRowKey.value = undefined;
  pagination.current = 1;
  pagination.pageSize = 10;
  pagination.total = 0;
  searchState.inspectionNo = '';
  searchState.supplierName = '';
  searchState.materialCode = '';
  searchState.lotNo = '';
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await getRawMaterialNcrSourceInspectionPage({
      inspectionType: 'IQC',
      ncrStatus: 'PENDING',
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 10),
      ...searchState,
    });
    rows.value = (result.list || []).map((item) => ({
      ...item,
      key: `${item.inspectionType || 'IQC'}:${item.inspectionId}`,
    }));
    pagination.total = result.total || 0;
    if (!rows.value.some((row) => row.key === selectedRowKey.value)) {
      selectedRowKey.value = undefined;
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadRows();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current || 1;
  pagination.pageSize = nextPagination.pageSize || 10;
  void loadRows();
}

function handlePick(row: IqcSourceRow) {
  if (row.ncrGenerated) {
    message.warning(`该 IQC 已生成处置单：${row.ncrNo || '-'}`);
    return;
  }
  emit('select', row);
  void modalApi.close();
}

function handleRowClick(row: IqcSourceRow) {
  if (row.ncrGenerated) {
    return;
  }
  selectedRowKey.value = row.key;
}

function getCustomRow(record: IqcSourceRow) {
  return {
    onClick: () => handleRowClick(record),
    onDblclick: () => handlePick(record),
  };
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function getCellValue(record: IqcSourceRow, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex as keyof IqcSourceRow];
}
</script>

<template>
  <Modal>
    <div class="qms-iqc-source-select">
      <Form class="qms-iqc-source-select__search" layout="inline">
        <Form.Item label="IQC单号">
          <Input
            v-model:value="searchState.inspectionNo"
            allow-clear
            autocomplete="off"
            placeholder="请输入IQC单号"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="供应商">
          <Input
            v-model:value="searchState.supplierName"
            allow-clear
            autocomplete="off"
            placeholder="请输入供应商"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="物料编码">
          <Input
            v-model:value="searchState.materialCode"
            allow-clear
            autocomplete="off"
            placeholder="请输入物料编码"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="批号">
          <Input
            v-model:value="searchState.lotNo"
            allow-clear
            autocomplete="off"
            placeholder="请输入批号"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" @click="handleSearch">查询</Button>
        </Form.Item>
      </Form>

      <Table
        bordered
        class="qms-iqc-source-select__table"
        :columns="columns"
        :custom-row="getCustomRow"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="key"
        :row-selection="rowSelection"
        :scroll="{ x: 1330, y: 440 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'inspectionNo'">
            <span class="font-mono font-bold text-blue-700">
              {{ displayValue(record.inspectionNo) }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'materialName'">
            <div class="leading-tight">
              <div class="font-bold text-slate-800">
                {{ displayValue(record.materialName) }}
              </div>
              <div class="mt-1 text-xs text-slate-400">
                {{ displayValue(record.materialCode) }} /
                {{ displayValue(record.specification) }}
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'quantity'">
            {{ displayValue(record.quantity) }} {{ record.unitCode || '' }}
          </template>
          <template v-else>
            {{ displayValue(getCellValue(record, column.dataIndex)) }}
          </template>
        </template>
      </Table>
    </div>
  </Modal>
</template>

<style scoped>
.qms-iqc-source-select {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: min(620px, calc(100vh - 210px));
  min-height: 520px;
  overflow: hidden;
}

.qms-iqc-source-select__search {
  flex: 0 0 auto;
  row-gap: 8px;
}

.qms-iqc-source-select__table {
  flex: 1 1 auto;
  min-height: 0;
}

.qms-iqc-source-select__table :deep(.ant-spin-nested-loading),
.qms-iqc-source-select__table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.qms-iqc-source-select__table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.qms-iqc-source-select__table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.qms-iqc-source-select__table :deep(.ant-table-content),
.qms-iqc-source-select__table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.qms-iqc-source-select__table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: 100%;
  max-height: none !important;
}

.qms-iqc-source-select__table :deep(.ant-pagination) {
  flex: 0 0 auto;
  margin: 10px 0 0 !important;
}
</style>
