<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

// 🔥 修复点 1：精确匹配您的真实 API 导出名 getRoutePage
import { getRoutePage } from '#/api/mes/base/route/index';

const emit = defineEmits(['select']);

const [Modal, modalApi] = useVbenModal({
  title: '选择工艺路线',
  class: 'w-[800px]',
  draggable: true,
  onConfirm: () => {
    // 兼容 radio 勾选和单击高亮行
    const selected = gridApi.grid?.getRadioRecord() || gridApi.grid?.getCurrentRecord();
    if (selected) {
      emit('select', selected);
    }
    modalApi.close();
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      // 🔥 修复点 2：匹配您的 Route 接口字段 (code, name)
      { fieldName: 'code', label: '路线编码', component: 'Input', componentProps: { allowClear: true } },
      { fieldName: 'name', label: '路线名称', component: 'Input', componentProps: { allowClear: true } },
    ],
    wrapperClass: 'grid-cols-2',
    actionWrapperClass: 'col-span-2 text-right',
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 60 },
      // 🔥 修复点 3：精确匹配接口里的返回字段
      { field: 'code', title: '路线编码', minWidth: 120 },
      { field: 'name', title: '路线名称', minWidth: 180 },
      { field: 'productName', title: '关联产品', minWidth: 150 },
      { field: 'version', title: '版本号', width: 80 },
      { field: 'remark', title: '备注', minWidth: 120 },
    ],
    height: 400,
    pagerConfig: { enabled: true }, // 保留底部分页
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          // 🔥 修复点 4：调用正确的 API 方法
          return await getRoutePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues
          });
        },
      },
    },
    radioConfig: { highlight: true, trigger: 'row' },
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    toolbarConfig: { search: true, refresh: true },
  },
});

defineExpose({
  open: () => modalApi.open(),
  close: () => modalApi.close()
});
</script>

<template>
  <Modal>
    <div class="flex h-full flex-col p-2">
      <Grid @cell-dblclick="({ row }) => { emit('select', row); modalApi.close(); }" />
    </div>
  </Modal>
</template>
