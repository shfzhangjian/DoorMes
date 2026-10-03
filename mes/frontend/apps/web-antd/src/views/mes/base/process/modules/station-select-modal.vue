<script lang="ts" setup>
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getWorkshopList } from '#/api/mes/base/workshop';
import { useVbenModal } from '@vben/common-ui';
import { handleTree } from '@vben/utils';
import { nextTick } from 'vue'; // [关键] 引入 nextTick 用于等待 DOM 渲染

const emit = defineEmits(['select']);

const [Modal, modalApi] = useVbenModal({
  title: '选择关联工位',
  class: 'w-[900px]',
  draggable: true,
  onConfirm: () => {
    const selected = gridApi.grid?.getCheckboxRecords();
    if (selected && selected.length > 0) {
      emit('select', selected);
      modalApi.close();
    }
  },
  // [关键修复] 监听打开事件，等待 DOM 渲染后再赋值
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();

      // 1. 等待弹窗 DOM 挂载和 Grid 初始化
      await nextTick();

      // 2. 防御性判断：确保 formApi 已存在
      if (gridApi && gridApi.formApi) {
        if (data?.workshopId) {
          // 设置搜索表单默认值
          await gridApi.formApi.setValues({ workshopId: data.workshopId });
        } else {
          // 清空
          await gridApi.formApi.setValues({ workshopId: null });
        }
        // 3. 立即触发查询
        await gridApi.query();
      }
    }
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    // 搜索表单配置
    schema: [
      {
        fieldName: 'workshopId',
        label: '所属车间',
        component: 'ApiTreeSelect',
        componentProps: {
          api: async () => {
            // 仅加载工厂、车间、产线用于筛选 (type < 4)
            const data = await getWorkshopList({});
            return handleTree(data.filter((item: any) => item.type < 4));
          },
          labelField: 'name',
          valueField: 'id',
          childrenField: 'children',
          placeholder: '请选择车间',
          treeDefaultExpandAll: true,
          allowClear: true,
        },
      },
      {
        fieldName: 'code',
        label: '工位编码',
        component: 'Input',
        componentProps: { allowClear: true },
      },
      {
        fieldName: 'name',
        label: '工位名称',
        component: 'Input',
        componentProps: { allowClear: true },
      },
    ],
    wrapperClass: 'grid-cols-3',
  },
  gridOptions: {
    columns: [
      { type: 'checkbox', width: 50 },
      { field: 'code', title: '工位编码', width: 120 },
      { field: 'name', title: '工位名称', minWidth: 150 },
      // [新增] 所属车间列
      // 注意：这里依赖后端 API 返回 parentName 或 workshopName
      // 如果后端只返回了 parentId，这里可能需要额外处理，目前先尝试展示通用字段
      {
        field: 'parentName', // 尝试取父级名称
        title: '所属车间',
        width: 120,
        formatter: ({ row }) => row.workshopName || row.parentName || ''
      },
      { field: 'manager', title: '负责人', width: 100 },
      { field: 'remark', title: '备注' },
    ],
    height: 450,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          // 处理搜索参数
          const params = {
            ...formValues,
            type: 4, // 固定查询工位
            status: 0, // 仅查询启用
          };

          // 如果选了车间，传递给 parentId
          if (formValues.workshopId) {
            params.parentId = formValues.workshopId;
          }

          return await getWorkshopList(params);
        },
      },
    },
    toolbarConfig: { search: true, refresh: true },
  },
});

defineExpose({
  open: modalApi.open,
});
</script>

<template>
  <Modal>
    <div class="h-full p-2">
      <Grid />
    </div>
  </Modal>
</template>
