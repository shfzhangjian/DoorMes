<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import type { MesExceptionApi } from '#/api/mes/quality/abnormal/exception';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Form, Input, message, Select, Table, Tag } from 'ant-design-vue';

import { getExceptionPage } from '#/api/mes/quality/abnormal/exception';

type ExceptionRow = MesExceptionApi.ExceptionRecord & {
  key: string;
};

defineOptions({ name: 'QmsNcrExceptionSelectModal' });

const emit = defineEmits<{
  select: [row: MesExceptionApi.ExceptionRecord];
}>();

const currentNcrNo = ref('');
const currentExceptionId = ref<number | string>();
const loading = ref(false);
const rows = ref<ExceptionRow[]>([]);
const selectedRowKey = ref<string>();

const searchState = reactive({
  exceptionNo: '',
  status: undefined as string | undefined,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待确认', value: 'PENDING_CONFIRM' },
  { label: '根因分析', value: 'ROOT_CAUSE_ANALYSIS' },
  { label: '执行结果上传', value: 'RESULT_UPLOAD' },
  { label: '关闭', value: 'CLOSED' },
];

const columns: TableColumnsType<ExceptionRow> = [
  { dataIndex: 'exceptionNo', title: '异常单号', width: 190 },
  { dataIndex: 'exceptionType', title: '异常类别', width: 120 },
  { dataIndex: 'exceptionLevel', title: '异常等级', width: 110 },
  { dataIndex: 'status', title: '状态', width: 130 },
  { dataIndex: 'discoverTime', title: '发现时间', width: 170 },
  { dataIndex: 'discovererName', title: '发现人', width: 110 },
  { dataIndex: 'relatedNcrNo', title: '已关联NCR', width: 180 },
  {
    dataIndex: 'description',
    ellipsis: true,
    title: '异常描述',
    width: 360,
  },
];

const selectedRow = computed(() =>
  rows.value.find((row) => row.key === selectedRowKey.value),
);

const rowSelection = computed(() => ({
  getCheckboxProps: (record: ExceptionRow) => ({
    disabled: isLinkedToOtherNcr(record),
  }),
  onChange: (keys: Array<number | string>) => {
    selectedRowKey.value = keys[0] === undefined ? undefined : String(keys[0]);
  },
  selectedRowKeys: selectedRowKey.value ? [selectedRowKey.value] : [],
  type: 'radio' as const,
}));

const [Modal, modalApi] = useVbenModal({
  class: 'w-[1260px] max-w-[94vw]',
  closeOnClickModal: false,
  title: '选择关联异常',
  async onConfirm() {
    if (!selectedRow.value) {
      message.warning('请选择一条异常事件');
      return;
    }
    if (isLinkedToOtherNcr(selectedRow.value)) {
      message.warning(`该异常事件已关联 NCR：${selectedRow.value.relatedNcrNo}`);
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
    const data = modalApi.getData() || {};
    resetState();
    currentNcrNo.value = String(data.currentNcrNo || '');
    currentExceptionId.value = data.currentExceptionId;
    await loadRows();
  },
});

function resetState() {
  rows.value = [];
  currentExceptionId.value = undefined;
  selectedRowKey.value = undefined;
  pagination.current = 1;
  pagination.pageSize = 10;
  pagination.total = 0;
  searchState.exceptionNo = '';
  searchState.status = undefined;
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await getExceptionPage({
      exceptionNo: searchState.exceptionNo,
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 10),
      status: searchState.status,
    });
    rows.value = (result.list || []).map((item) => ({
      ...item,
      key: String(item.id || item.exceptionNo || ''),
    }));
    pagination.total = result.total || 0;
    const currentKey =
      currentExceptionId.value === undefined || currentExceptionId.value === null
        ? undefined
        : String(currentExceptionId.value);
    if (currentKey && rows.value.some((row) => row.key === currentKey)) {
      selectedRowKey.value = currentKey;
      return;
    }
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

function handlePick(row: ExceptionRow) {
  if (isLinkedToOtherNcr(row)) {
    message.warning(`该异常事件已关联 NCR：${row.relatedNcrNo}`);
    return;
  }
  emit('select', row);
  void modalApi.close();
}

function handleRowClick(row: ExceptionRow) {
  if (isLinkedToOtherNcr(row)) {
    return;
  }
  selectedRowKey.value = row.key;
}

function getCustomRow(record: ExceptionRow) {
  return {
    onClick: () => handleRowClick(record),
    onDblclick: () => handlePick(record),
  };
}

function isLinkedToOtherNcr(row: ExceptionRow) {
  if (!row.relatedNcrNo) {
    return false;
  }
  if (!currentNcrNo.value) {
    return true;
  }
  return row.relatedNcrNo !== currentNcrNo.value;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}
</script>

<template>
  <Modal>
    <div class="qms-exception-select">
      <Form class="qms-exception-select__search" layout="inline">
        <Form.Item label="异常单号">
          <Input
            v-model:value="searchState.exceptionNo"
            allow-clear
            placeholder="请输入异常单号"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            v-model:value="searchState.status"
            allow-clear
            class="qms-exception-select__status"
            :options="statusOptions"
            placeholder="请选择状态"
          />
        </Form.Item>
        <Button type="primary" @click="handleSearch">查询</Button>
      </Form>
      <Table
        bordered
        :columns="columns"
        :custom-row="getCustomRow"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record) => record.key"
        :row-selection="rowSelection"
        :scroll="{ x: 1360, y: 520 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'exceptionNo'">
            <Button type="link" size="small" @click.stop="handlePick(record)">
              {{ displayValue(record.exceptionNo) }}
            </Button>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <Tag>{{ displayValue(record.status) }}</Tag>
          </template>
          <template v-else-if="column.dataIndex === 'relatedNcrNo'">
            <Tag v-if="record.relatedNcrNo" :color="isLinkedToOtherNcr(record) ? 'error' : 'blue'">
              {{ record.relatedNcrNo }}
            </Tag>
            <span v-else>-</span>
          </template>
        </template>
      </Table>
    </div>
  </Modal>
</template>

<style scoped>
.qms-exception-select {
  min-height: 620px;
}

.qms-exception-select__search {
  gap: 8px;
  margin-bottom: 12px;
}

.qms-exception-select__search :deep(.ant-form-item) {
  margin-right: 8px;
  margin-bottom: 8px;
}

.qms-exception-select__search :deep(.ant-input) {
  width: 220px;
}

.qms-exception-select__status {
  width: 180px;
}
</style>
