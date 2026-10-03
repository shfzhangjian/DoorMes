<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Form,
  Input,
  message,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import { getProductAbnormalEventPage } from '#/api/mes/quality/abnormal/product-event';

import {
  NCR_GENERATION_STATUS_OPTIONS,
  PRODUCT_ABNORMAL_EVENT_SOURCE_OPTIONS,
} from '../../product-event/data';

type ProductSourceRow = MesProductAbnormalEventApi.ProductAbnormalEvent & {
  key: string;
};

defineOptions({ name: 'QmsNcrProductSourceSelectModal' });

const emit = defineEmits<{
  select: [row: MesProductAbnormalEventApi.ProductAbnormalEvent];
}>();

const loading = ref(false);
const rows = ref<ProductSourceRow[]>([]);
const selectedRowKey = ref<string>();

const searchState = reactive({
  inspectionNo: '',
  ncrStatus: 'PENDING' as 'ALL' | 'GENERATED' | 'PENDING',
  operationName: '',
  productBatchNo: '',
  productModel: '',
  sourceType: undefined as MesProductAbnormalEventApi.SourceType | undefined,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const columns: TableColumnsType<ProductSourceRow> = [
  { dataIndex: 'sourceType', title: '检验类型', width: 140 },
  { dataIndex: 'inspectionNo', title: '来源单据', width: 180 },
  { dataIndex: 'inspectionTime', title: '检验时间', width: 170 },
  { dataIndex: 'operationName', title: '工序', width: 130 },
  { dataIndex: 'productBatchNo', title: '产品批次', width: 170 },
  { dataIndex: 'productModel', title: '产品型号', width: 190 },
  { dataIndex: 'specification', title: '规格', width: 160 },
  { align: 'right', dataIndex: 'inspectionQty', title: '检验数量', width: 110 },
  {
    align: 'right',
    dataIndex: 'unqualifiedQty',
    title: '不合格数量',
    width: 120,
  },
  { dataIndex: 'abnormalSummary', title: '不合格项总结', width: 360 },
  { dataIndex: 'ncrStatus', title: 'NCR状态', width: 140 },
];

const selectedRow = computed(() =>
  rows.value.find((row) => row.key === selectedRowKey.value),
);

const rowSelection = computed(() => ({
  getCheckboxProps: (record: ProductSourceRow) => ({
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
  title: '选择来源单据',
  async onConfirm() {
    if (!selectedRow.value) {
      message.warning('请选择一条来源单据');
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
  searchState.operationName = '';
  searchState.productBatchNo = '';
  searchState.productModel = '';
  searchState.sourceType = undefined;
  searchState.ncrStatus = 'PENDING';
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await getProductAbnormalEventPage({
      inspectionNo: searchState.inspectionNo,
      ncrStatus: searchState.ncrStatus,
      operationName: searchState.operationName,
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 10),
      productBatchNo: searchState.productBatchNo,
      productModel: searchState.productModel,
      sourceType: searchState.sourceType,
    });
    rows.value = (result.list || []).map((item) => ({
      ...item,
      key: `${item.sourceType || 'UNKNOWN'}:${item.inspectionId || item.inspectionNo || ''}`,
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

function handlePick(row: ProductSourceRow) {
  if (row.ncrGenerated) {
    message.warning(`该来源单据已生成处置单：${row.ncrNo || '-'}`);
    return;
  }
  emit('select', row);
  void modalApi.close();
}

function handleRowClick(row: ProductSourceRow) {
  if (row.ncrGenerated) {
    return;
  }
  selectedRowKey.value = row.key;
}

function getCustomRow(record: ProductSourceRow) {
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

function getCellValue(record: ProductSourceRow, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex as keyof ProductSourceRow];
}

function getSourceTypeLabel(value?: string) {
  return (
    PRODUCT_ABNORMAL_EVENT_SOURCE_OPTIONS.find((item) => item.value === value)
      ?.label ||
    value ||
    '-'
  );
}

function getNcrStatusText(record: ProductSourceRow) {
  return record.ncrGenerated ? `已生成 ${record.ncrNo || ''}` : '待生成';
}
</script>

<template>
  <Modal>
    <div class="qms-product-source-select">
      <Form class="qms-product-source-select__search" layout="inline">
        <Form.Item label="来源单据">
          <Input
            v-model:value="searchState.inspectionNo"
            allow-clear
            autocomplete="off"
            placeholder="请输入来源单据号"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="检验类型">
          <Select
            v-model:value="searchState.sourceType"
            allow-clear
            class="qms-product-source-select__select"
            :options="PRODUCT_ABNORMAL_EVENT_SOURCE_OPTIONS"
            placeholder="请选择检验类型"
          />
        </Form.Item>
        <Form.Item label="产品批次">
          <Input
            v-model:value="searchState.productBatchNo"
            allow-clear
            autocomplete="off"
            placeholder="请输入产品批次"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="工序">
          <Input
            v-model:value="searchState.operationName"
            allow-clear
            autocomplete="off"
            placeholder="请输入工序"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="NCR状态">
          <Select
            v-model:value="searchState.ncrStatus"
            class="qms-product-source-select__status"
            :options="NCR_GENERATION_STATUS_OPTIONS"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" @click="handleSearch">查询</Button>
        </Form.Item>
      </Form>

      <Table
        bordered
        class="qms-product-source-select__table"
        :columns="columns"
        :custom-row="getCustomRow"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="key"
        :row-selection="rowSelection"
        :scroll="{ x: 1870, y: 440 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'sourceType'">
            <Tag color="blue" class="!m-0">
              {{ getSourceTypeLabel(record.sourceType) }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'inspectionNo'">
            <span class="font-mono font-bold text-blue-700">
              {{ displayValue(record.inspectionNo) }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'productModel'">
            <div class="leading-tight">
              <div class="font-bold text-slate-800">
                {{ displayValue(record.productModel) }}
              </div>
              <div class="mt-1 text-xs text-slate-400">
                {{ displayValue(record.specification) }}
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'abnormalSummary'">
            <span class="qms-product-source-select__summary">
              {{ displayValue(record.abnormalSummary) }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'ncrStatus'">
            <Tag
              :color="record.ncrGenerated ? 'success' : 'warning'"
              class="!m-0"
            >
              {{ getNcrStatusText(record) }}
            </Tag>
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
.qms-product-source-select {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: min(620px, calc(100vh - 210px));
  min-height: 520px;
  overflow: hidden;
}

.qms-product-source-select__search {
  flex: 0 0 auto;
  row-gap: 8px;
}

.qms-product-source-select__select {
  width: 170px;
}

.qms-product-source-select__status {
  width: 140px;
}

.qms-product-source-select__table {
  flex: 1 1 auto;
  min-height: 0;
}

.qms-product-source-select__summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  line-height: 18px;
  overflow-wrap: anywhere;
  -webkit-line-clamp: 3;
}

.qms-product-source-select__table :deep(.ant-spin-nested-loading),
.qms-product-source-select__table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.qms-product-source-select__table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.qms-product-source-select__table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.qms-product-source-select__table :deep(.ant-table-content),
.qms-product-source-select__table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.qms-product-source-select__table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: 100%;
  max-height: none !important;
}

.qms-product-source-select__table :deep(.ant-pagination) {
  flex: 0 0 auto;
  margin: 10px 0 0 !important;
}
</style>
