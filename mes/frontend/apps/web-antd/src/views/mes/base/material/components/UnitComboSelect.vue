<script lang="ts" setup>
import { ref, watch } from 'vue';
import { Space, Select, Button, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getUnitPage } from '#/api/mes/base/unit';

const props = defineProps({
  value: { type: String, default: undefined },
});

const emit = defineEmits(['update:value', 'change']);

const innerValue = ref(props.value);
const options = ref<any[]>([]);

watch(
  () => props.value,
  (newVal) => {
    innerValue.value = newVal;
    if (newVal && options.value.length === 0) {
      options.value = [{ label: newVal, value: newVal }];
    }
  },
  { immediate: true },
);

async function handleSearch(keyword: string) {
  if (!keyword) return;
  try {
    const res = await getUnitPage({ name: keyword, pageNo: 1, pageSize: 20 });
    options.value = res.list.map((item: any) => ({
      label: item.name,
      value: item.name,
      raw: item,
    }));
  } catch (error) {
    console.error('搜索计量单位失败', error);
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
  const targetName = selected.name;
  options.value = [{ label: targetName, value: targetName, raw: selected }];

  innerValue.value = targetName;
  emit('update:value', targetName);
  emit('change', selected);
  modalApi.close();
}

// ================= 多条件查询弹窗逻辑 =================
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      { fieldName: 'code', label: '单位符号', component: 'Input', componentProps: { allowClear: true } },
      { fieldName: 'name', label: '单位名称', component: 'Input', componentProps: { allowClear: true } },
    ],
    showCollapseButton: false,
    submitOnEnter: true,
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 50 },
      { field: 'code', title: '单位符号', minWidth: 100 },
      { field: 'name', title: '单位名称', minWidth: 150 },
      { field: 'category', title: '维度', minWidth: 100 },
      { field: 'remark', title: '备注', minWidth: 120 },
    ],
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getUnitPage({
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
        gridApi.grid.setRadioRow(row);
        handleConfirmSelection(row);
      }
    },
  },
});

const [Modal, modalApi] = useVbenModal({
  title: '选择计量单位',
  onConfirm() {
    const selected = gridApi.grid.getRadioRecord();
    handleConfirmSelection(selected);
  },
});

function openComplexSearch() {
  modalApi.open();
}
</script>

<template>
  <div>
    <Space.Compact style="width: 100%">
      <Select
        v-model:value="innerValue"
        :options="options"
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
