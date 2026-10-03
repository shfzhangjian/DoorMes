<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { Space, Select, Button, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSupplierPage } from '#/api/mes/supplier';

const props = defineProps({
  value: { type: [Number, String], default: undefined },
  defaultLabel: { type: String, default: '' },
  materialCode: { type: String, default: '' },
  qualifiedOnly: { type: Boolean, default: false },
});

const emit = defineEmits(['update:value', 'change']);

const innerValue = ref(props.value);
const options = ref<any[]>([]);
const supplierPickerDisabled = computed(
  () => props.qualifiedOnly && !props.materialCode.trim(),
);

function toOption(item: any) {
  return {
    label: item.supplierCode
      ? `${item.supplierCode} / ${item.supplierName}`
      : item.supplierName,
    value: item.id,
    raw: item,
  };
}

function buildSupplierPageParams(keyword?: string) {
  return {
    materialCodeExact: props.materialCode.trim() || undefined,
    pageNo: 1,
    pageSize: 20,
    status: props.qualifiedOnly ? 'QUALIFIED' : undefined,
    supplierName: keyword || undefined,
  };
}

watch(
  () => props.value,
  (newVal) => {
    innerValue.value = newVal;
    if (newVal && props.defaultLabel && options.value.length === 0) {
      options.value = [{ label: props.defaultLabel, value: newVal }];
    }
  },
  { immediate: true },
);

watch(
  () => props.materialCode,
  () => {
    options.value = [];
  },
);

async function handleSearch(keyword: string) {
  if (!keyword || supplierPickerDisabled.value) return;
  try {
    const res = await getSupplierPage(buildSupplierPageParams(keyword));
    options.value = res.list.map(toOption);
  } catch (error) {
    console.error('搜索供应商失败', error);
  }
}

function handleSelectChange(val: any, option: any) {
  innerValue.value = val;
  emit('update:value', val);
  emit('change', option?.raw || null);
}

// ⭐️ 提取公共的确认选择逻辑
function handleConfirmSelection(selected: any) {
  if (!selected) {
    message.warning('请先选择一条数据');
    return;
  }
  options.value = [toOption(selected)];
  innerValue.value = selected.id;
  emit('update:value', selected.id);
  emit('change', selected);
  modalApi.close();
}

// ================= 多条件查询弹窗逻辑 =================
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      { fieldName: 'supplierCode', label: '编码', component: 'Input', componentProps: { allowClear: true } },
      { fieldName: 'supplierName', label: '名称', component: 'Input', componentProps: { allowClear: true } },
    ],
    showCollapseButton: false,
    submitOnEnter: true,
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 50 },
      { field: 'supplierCode', title: '供应商编码', minWidth: 120 },
      { field: 'supplierName', title: '供应商名称', minWidth: 150 },
      { field: 'materialCode', title: '物料代码', minWidth: 140 },
      {
        field: 'status',
        title: '供应商状态',
        width: 110,
        formatter: ({ cellValue }) =>
          cellValue === 'QUALIFIED'
            ? '合格'
            : cellValue === 'UNQUALIFIED'
              ? '不合格'
              : '-',
      },
      { field: 'contactPerson', title: '联系人', minWidth: 100 },
    ],
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSupplierPage({
            ...buildSupplierPageParams(),
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: { isHover: true, isCurrent: true },
  },
  // ⭐️ 新增：监听表格双击事件
  gridEvents: {
    cellDblclick: ({ row }) => {
      if (row) {
        // 双击时自动勾选该行的 radio 并执行确认
        gridApi.grid.setRadioRow(row);
        handleConfirmSelection(row);
      }
    },
  },
});

const [Modal, modalApi] = useVbenModal({
  title: '选择供应商',
  onConfirm() {
    const selected = gridApi.grid.getRadioRecord();
    handleConfirmSelection(selected);
  },
});

function openComplexSearch() {
  if (supplierPickerDisabled.value) {
    message.warning('请先选择物料编码');
    return;
  }
  modalApi.open();
}
</script>

<template>
  <div>
    <Space.Compact style="width: 100%">
      <Select
        v-model:value="innerValue"
        :options="options"
        :disabled="supplierPickerDisabled"
        show-search
        allow-clear
        placeholder="请选择或输入搜索"
        style="width: calc(100% - 32px)"
        :filter-option="false"
        @search="handleSearch"
        @change="handleSelectChange"
      />
      <Button @click="openComplexSearch" type="default">
        <template #icon><IconifyIcon icon="lucide:search" /></template>
      </Button>
    </Space.Compact>

    <Modal class="w-[800px]">
      <div class="flex h-[450px] flex-col">
        <Grid />
      </div>
    </Modal>
  </div>
</template>
