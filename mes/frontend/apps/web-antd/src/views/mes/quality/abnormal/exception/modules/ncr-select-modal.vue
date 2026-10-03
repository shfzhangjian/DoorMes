<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, message, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getNcrCandidatePage,
  getRawMaterialNcrCandidatePage,
} from '#/api/mes/quality/abnormal/exception';

defineOptions({ name: 'QmsNcrPicker' });

const emit = defineEmits(['select']);
const pickerMode = ref<'PRODUCT' | 'RAW_MATERIAL'>('PRODUCT');
const statusFilter = ref<string>();

const productStatusOptions = [
  { label: '全部', value: undefined },
  { label: '待MRB', value: 'PENDING' },
  { label: '返工', value: 'REWORK' },
  { label: '报废', value: 'SCRAP' },
  { label: '特采', value: 'CONCESSION' },
];
const rawMaterialStatusOptions = [
  { label: '全部', value: undefined },
  { label: '退货', value: 'RETURN' },
  { label: '报废', value: 'SCRAP' },
  { label: '挑选', value: 'PICK' },
  { label: '特采', value: 'CONCESSION' },
];

const dispositionLabelMap: Record<string, string> = {
  CONCESSION: '特采',
  PENDING: '待MRB',
  PICK: '挑选',
  RECUT: '改切',
  RETURN: '退货',
  REWORK: '返工',
  SCRAP: '报废',
};
const dispositionColorMap: Record<string, string> = {
  CONCESSION: 'success',
  PENDING: 'warning',
  PICK: 'processing',
  RECUT: 'processing',
  RETURN: 'error',
  REWORK: 'warning',
  SCRAP: 'error',
};

const pickerTitle = computed(() =>
  pickerMode.value === 'RAW_MATERIAL'
    ? '关联系统已有原材料不合格处置单'
    : '关联系统已有不合格品处置单 (NCR)',
);
const statusOptions = computed(() =>
  pickerMode.value === 'RAW_MATERIAL'
    ? rawMaterialStatusOptions
    : productStatusOptions,
);
const statusTitle = computed(() =>
  pickerMode.value === 'RAW_MATERIAL' ? '处置结论' : '处置状态',
);

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-ncr-picker-vben-grid',
  gridClass: 'qms-ncr-picker-vxe-grid',
  formOptions: {
    schema: [
      {
        fieldName: 'ncrNo',
        label: 'NCR单号',
        component: 'Input',
        componentProps: { placeholder: '请输入 NCR 单号' },
      },
      {
        fieldName: 'batchNo',
        label: '异常批号',
        component: 'Input',
        componentProps: { placeholder: '请输入批号' },
      },
      {
        fieldName: 'defectCode',
        label: '缺陷代码',
        component: 'Input',
        componentProps: { placeholder: '请输入缺陷代码' },
      },
    ],
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 50, align: 'center' },
      { field: 'ncrNo', title: '处置单号', minWidth: 180 },
      { field: 'batchNo', title: '异常批号', width: 150 },
      { field: 'defectCode', title: '缺陷代码', width: 130 },
      {
        field: 'materialName',
        title: '摘要',
        minWidth: 150,
        showOverflow: 'tooltip',
      },
      {
        field: 'status',
        title: 'MRB决策',
        width: 110,
        align: 'center',
        slots: { default: 'status' },
      },
    ],
    height: '100%',
    radioConfig: { highlight: true, trigger: 'row' },
    rowConfig: { isHover: true, isCurrent: true, keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await (pickerMode.value === 'RAW_MATERIAL'
            ? getRawMaterialNcrCandidatePage
            : getNcrCandidatePage)({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            status: statusFilter.value,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

const [Modal, modalApi] = useVbenModal({
  title: '关联系统已有不合格品处置单 (NCR)',
  class: 'w-[980px] max-w-[88vw]',
  onOpenChange: (isOpen) => {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData() || {};
    pickerMode.value =
      data.ncrType === 'RAW_MATERIAL_NCR' || data.sourceType === 'RAW_MATERIAL'
        ? 'RAW_MATERIAL'
        : 'PRODUCT';
    statusFilter.value = undefined;
    modalApi.setState({ title: pickerTitle.value });
    gridApi.query();
  },
  onConfirm: () => {
    const row =
      gridApi.grid?.getRadioRecord() || gridApi.grid?.getCurrentRecord();
    if (!row) {
      return message.warning('请先选中一行处置单');
    }
    emit('select', row);
    modalApi.close();
  },
});

function handleStatusFilter(value?: string) {
  statusFilter.value = value;
  gridApi.query();
}

function getDispositionValue(row: any) {
  return row.status || row.finalDisposition || row.mrbDecision;
}

function getDispositionLabel(row: any) {
  const value = getDispositionValue(row);
  const normalized = value?.toUpperCase?.();
  return dispositionLabelMap[normalized || ''] || value || '-';
}

function getDispositionColor(row: any) {
  const value = getDispositionValue(row);
  const normalized = value?.toUpperCase?.();
  return dispositionColorMap[normalized || ''] || 'default';
}
</script>

<template>
  <Modal>
    <div class="qms-ncr-picker">
      <aside class="qms-ncr-picker__side">
        <div class="qms-ncr-picker__side-title">{{ statusTitle }}</div>
        <Button
          v-for="item in statusOptions"
          :key="item.label"
          block
          :type="statusFilter === item.value ? 'primary' : 'default'"
          class="mb-2 text-left"
          @click="handleStatusFilter(item.value)"
        >
          {{ item.label }}
        </Button>
      </aside>
      <main class="qms-ncr-picker__main">
        <Grid>
          <template #status="{ row }">
            <Tag :color="getDispositionColor(row)">
              {{ getDispositionLabel(row) }}
            </Tag>
          </template>
        </Grid>
      </main>
    </div>
  </Modal>
</template>

<style scoped>
.qms-ncr-picker {
  display: flex;
  gap: 10px;
  height: 560px;
  min-height: 0;
}

.qms-ncr-picker__side,
.qms-ncr-picker__main {
  border: 1px solid #d7e3f2;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(15 23 42 / 6%);
}

.qms-ncr-picker__side {
  width: 180px;
  flex-shrink: 0;
  padding: 12px;
}

.qms-ncr-picker__side-title {
  margin-bottom: 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.qms-ncr-picker__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 8px;
}

.qms-ncr-picker__main :deep(.qms-ncr-picker-vben-grid),
.qms-ncr-picker__main :deep(.qms-ncr-picker-vxe-grid) {
  height: 100% !important;
  min-height: 0 !important;
}
</style>
