<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcBomApi } from '#/api/mes/hc/bom';

import { h, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { Modal, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteBom, deleteBomList, exportBom, getBomPage, importProductBom } from '#/api/mes/hc/bom';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const checkedIds = ref<number[]>([]);
const importInputRef = ref<HTMLInputElement>();
const importing = ref(false);

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcBomApi.Bom) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: MesHcBomApi.Bom) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteBom(row.id);
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
    await deleteBomList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({ records }: { records: MesHcBomApi.Bom[] }) {
  checkedIds.value = records.map((item) => item.id);
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  const data = await exportBom(formValues);
  downloadFileFromBlobPart({ fileName: '产品BOM.xlsx', source: data });
}

function handleImportClick() {
  importInputRef.value?.click();
}

function showImportFailures(resp: MesHcBomApi.ProductImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 20).join('\n');
  const moreText = failures.length > 20 ? `\n... 还有 ${failures.length - 20} 条未显示` : '';
  Modal.warning({
    title: '产品BOM导入校验未通过',
    width: 720,
    content: h('div', { class: 'space-y-2' }, [
      h('div', `读取 ${resp.totalRows || 0} 行，跳过 ${resp.skippedRows || 0} 行，失败 ${resp.failureCount || 0} 条。`),
      h(
        'pre',
        {
          style: 'white-space: pre-wrap; margin: 0; max-height: 320px; overflow: auto; font-size: 12px;',
        },
        text + moreText,
      ),
    ]),
  });
}

async function handleImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;

  const hideLoading = message.loading({ content: '正在导入产品BOM...', duration: 0 });
  importing.value = true;
  try {
    const resp = await importProductBom(file, true);
    if ((resp.failureCount || 0) > 0) {
      showImportFailures(resp);
      return;
    }
    message.success(`导入成功：BOM ${resp.successCount || 0} 条，明细 ${resp.itemCount || 0} 条，跳过 ${resp.skippedRows || 0} 行`);
    handleRefresh();
  } finally {
    importing.value = false;
    hideLoading();
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getBomPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
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
  } as VxeTableGridOptions<MesHcBomApi.Bom>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <input ref="importInputRef" type="file" accept=".xlsx,.xls" class="hidden" @change="handleImportFileChange" />
    <Grid table-title="产品BOM列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:bom:create'],
              onClick: handleCreate,
            },
            {
              label: '导入产品BOM',
              type: 'primary',
              icon: ACTION_ICON.UPLOAD,
              auth: ['mes:md:bom:create'],
              disabled: importing,
              onClick: handleImportClick,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:bom:export'],
              onClick: handleExport,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:bom:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:bom:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:bom:delete'],
              popConfirm: {
                title: '确认删除当前记录吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
