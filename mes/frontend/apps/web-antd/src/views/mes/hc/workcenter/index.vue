<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteWorkCenter, deleteWorkCenterList, exportWorkCenter, getWorkCenterPage } from '#/api/mes/hc/workcenter';

import { buildWorkCenterGroupRows, useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcWorkCenterApi.WorkCenter & { isGroup?: boolean }) {
  if ((row as any).isGroup) return;
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcWorkCenterApi.WorkCenter & { isGroup?: boolean }) {
  if ((row as any).isGroup) return;
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteWorkCenter(row.id);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的记录吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteWorkCenterList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: Array<MesHcWorkCenterApi.WorkCenter & { isGroup?: boolean }> }) {
  checkedIds.value = records.filter((item) => !(item as any).isGroup).map((item) => item.id);
}

async function handleExport() {
  const data = await exportWorkCenter(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '工作中心.xls', source: data });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    treeConfig: {
      transform: false,
      rowField: 'id',
      childrenField: 'children',
      expandAll: true,
      accordion: false,
    },
    checkboxConfig: {
      checkMethod: ({ row }) => !(row as any).isGroup,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getWorkCenterPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return {
            ...result,
            list: buildWorkCenterGroupRows(result.list || []),
          };
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcWorkCenterApi.WorkCenter>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="工作中心列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:work-center:create'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:work-center:export'],
              onClick: handleExport,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:work-center:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="
            row.isGroup
              ? []
              : [
                  {
                    label: '编辑',
                    type: 'link',
                    icon: ACTION_ICON.EDIT,
                    auth: ['mes:md:work-center:update'],
                    onClick: handleEdit.bind(null, row),
                  },
                  {
                    label: '删除',
                    type: 'link',
                    danger: true,
                    icon: ACTION_ICON.DELETE,
                    auth: ['mes:md:work-center:delete'],
                    popConfirm: {
                      title: '确认删除当前记录吗？',
                      confirm: handleDelete.bind(null, row),
                    },
                  },
                ]
          "
        />
      </template>
    </Grid>
  </Page>
</template>
