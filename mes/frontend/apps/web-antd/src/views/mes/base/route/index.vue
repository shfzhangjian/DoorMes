<!-- 完整路径: src/views/mes/base/route/index.vue -->
<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MesRouteApi } from '#/api/mes/base/route';
import { useRouter } from 'vue-router';
import { Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteRoute, deleteRouteList, getRoutePage } from '#/api/mes/base/route/index';
import { getRangePickerDefaultProps } from '#/utils';
import { Button, message, Popconfirm } from 'ant-design-vue';
import { ref } from 'vue';

import RouteForm from './modules/form.vue';

// 1. 定义搜索表单
const formSchema: VbenFormSchema[] = [
  { fieldName: 'code', label: '路线编号', component: 'Input', componentProps: { allowClear: true } },
  { fieldName: 'name', label: '路线名称', component: 'Input', componentProps: { allowClear: true } },
  { fieldName: 'productName', label: '产品名称', component: 'Input', componentProps: { allowClear: true } },
  {
    fieldName: 'active',
    label: '是否默认',
    component: 'Select',
    componentProps: {
      options: [
        { label: '是', value: true },
        { label: '否', value: false },
      ],
      allowClear: true,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      dictType: DICT_TYPE.COMMON_STATUS,
      allowClear: true,
    },
  },
  {
    fieldName: 'createTime',
    label: '创建时间',
    component: 'RangePicker',
    componentProps: getRangePickerDefaultProps(),
  },
];

// 2. 定义表格列
const gridColumns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60, fixed: 'left' },
  { field: 'code', title: '路线编号', minWidth: 120, fixed: 'left' },
  { field: 'name', title: '路线名称', minWidth: 150 },
  { field: 'productName', title: '关联产品', minWidth: 150 },
  { field: 'version', title: '版本号', width: 80 },
  {
    field: 'active',
    title: '默认',
    width: 80,
    formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } },
  },
  { field: 'createTime', title: '创建时间', width: 160, formatter: 'formatDateTime' },
  { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
];

const router = useRouter(); // 🛠️ 2. 初始化 router

// 🛠️ 3. 添加跳转到编排工作台的方法
function handleDesign(row: any) {
  router.push({
    path: '/mes/base/route-workspace', // 这里必须和菜单SQL里配置的 path 保持一致
    query: { id: row.id } // 携带路线 ID 传给工作台
  });
}

const checkedIds = ref<number[]>([]);

// 这里定义了弹窗 Modal，connectedComponent 关联了 form.vue
const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: RouteForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    wrapperClass: 'grid-cols-3',
  },
  gridOptions: {
    columns: gridColumns,
    border: true,
    keepSource: true,
    // [修改] 将 'auto' 改为 '100%'。配合外层容器 h-full，强制表格填满容器，
    // 这样滚动条会出现在表格内部，而不是整个页面右侧。
    height: '100%',
    pagerConfig: { autoHidden: false },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getRoutePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    toolbarConfig: {
      refresh: true,
      zoom: true,
      custom: true,
      slots: { buttons: 'toolbar_buttons' },
    },
  },
  gridEvents: {
    checkboxChange: ({ records }) => {
      checkedIds.value = records.map((item) => item.id!);
    },
    checkboxAll: ({ records }) => {
      checkedIds.value = records.map((item) => item.id!);
    },
  },
});

// [说明] 弹窗触发位置：handleCreate 调用 open()
function handleCreate() {
  formModalApi.setData({}).open();
}

// [说明] 弹窗触发位置：handleEdit 调用 open()
function handleEdit(row: MesRouteApi.Route) {
  formModalApi.setData({ id: row.id }).open();
}

async function handleDelete(row: MesRouteApi.Route) {
  await deleteRoute(row.id!);
  message.success('删除成功');
  gridApi.query();
}

async function handleDeleteBatch() {
  if (checkedIds.value.length === 0) return;
  await deleteRouteList(checkedIds.value);
  checkedIds.value = [];
  message.success('批量删除成功');
  gridApi.query();
}

function handleSuccess() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col overflow-hidden">
      <Grid>
        <template #toolbar_buttons>
          <Button type="primary" @click="handleCreate">新增工艺路线</Button>
          <Popconfirm title="确认删除选中?" @confirm="handleDeleteBatch" :disabled="checkedIds.length === 0">
            <Button type="primary" danger class="ml-2" :disabled="checkedIds.length === 0">批量删除</Button>
          </Popconfirm>
        </template>
        <template #actions="{ row }">
<!--          <Button type="link" size="small" @click="handleDesign(row)">-->
<!--            <template #icon><IconifyIcon icon="ep:set-up" /></template>-->
<!--            工艺编排-->
<!--          </Button>-->
          <Button type="link" size="small" @click="handleEdit(row)">编辑</Button>
          <Popconfirm title="确认删除?" @confirm="handleDelete(row)">
            <Button type="link" danger size="small">删除</Button>
          </Popconfirm>
        </template>
      </Grid>
    </div>
    <FormModal @success="handleSuccess" />
  </Page>
</template>
