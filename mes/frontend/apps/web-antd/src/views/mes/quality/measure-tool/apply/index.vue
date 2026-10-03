<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { h, onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Input, Modal, message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveMeasureToolApply,
  deleteMeasureToolApply,
  exportMeasureToolApply,
  getMeasureToolApplyPage,
  getMeasureToolCategoryList,
  rejectMeasureToolApply,
  submitMeasureToolApply,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';
import { APPLY_STATUS_OPTIONS, loadCategoryOptions, optionColor, optionLabel } from '../shared';

defineOptions({ name: 'MesQmsMeasureToolApply' });

const categoryOptions = ref<Array<{ label?: string; value?: number }>>([]);
const checkedIds = ref<number[]>([]);
const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: QmsMeasureToolApi.Apply) {
  formModalApi.setData(row).open();
}

async function handleDelete(row: QmsMeasureToolApi.Apply) {
  await confirm(`确认删除申请单 [${row.applyNo}] 吗？`);
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteMeasureToolApply(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const hideLoading = message.loading({ content: $t('ui.actionMessage.deletingBatch'), duration: 0 });
  try {
    await Promise.all(checkedIds.value.map((id) => deleteMeasureToolApply(id)));
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleSubmit(row: QmsMeasureToolApi.Apply) {
  await confirm(`确认提交申请单 [${row.applyNo}] 吗？`);
  await submitMeasureToolApply({ id: row.id! });
  message.success('已提交审批');
  handleRefresh();
}

function handleApprove(row: QmsMeasureToolApi.Apply) {
  openOpinionModal('审批通过', row, async (opinion) => {
    const ledgerId = await approveMeasureToolApply({ id: row.id!, opinion });
    message.success(`审批通过，已写入台账 ID：${ledgerId}`);
    handleRefresh();
  });
}

function handleReject(row: QmsMeasureToolApi.Apply) {
  openOpinionModal('驳回申请', row, async (opinion) => {
    await rejectMeasureToolApply({ id: row.id!, opinion });
    message.success('已驳回');
    handleRefresh();
  });
}

function openOpinionModal(
  title: string,
  row: QmsMeasureToolApi.Apply,
  submitter: (opinion?: string) => Promise<void>,
) {
  let opinion = '';
  Modal.confirm({
    title: `${title}：${row.applyNo}`,
    content: () =>
      h(Input.TextArea, {
        rows: 3,
        placeholder: '请输入审批意见',
        'onUpdate:value': (value: string) => (opinion = value),
      }),
    async onOk() {
      await submitter(opinion.trim() || undefined);
    },
  });
}

async function handleExport() {
  const data = await exportMeasureToolApply(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '量检具新增申请.xls', source: data });
}

function handleRowCheckboxChange({ records }: { records: QmsMeasureToolApi.Apply[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getMeasureToolApplyPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<QmsMeasureToolApi.Apply>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});

onMounted(async () => {
  categoryOptions.value = await loadCategoryOptions(getMeasureToolCategoryList);
  await gridApi.formApi.updateSchema(useGridFormSchema(categoryOptions.value));
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="量检具新增申请列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['量检具新增申请']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:qms-measure-tool-apply:create'],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:qms-measure-tool-apply:export'],
              onClick: handleExport,
            },
            {
              label: $t('ui.actionTitle.deleteBatch'),
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:qms-measure-tool-apply:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>

      <template #status="{ row }">
        <Tag :color="optionColor(APPLY_STATUS_OPTIONS, row.status)">
          {{ optionLabel(APPLY_STATUS_OPTIONS, row.status) }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '提交',
              type: 'link',
              icon: ACTION_ICON.AUDIT,
              auth: ['mes:qms-measure-tool-apply:submit'],
              ifShow: row.status === 'DRAFT' || row.status === 'REJECTED',
              onClick: () => handleSubmit(row),
            },
            {
              label: '通过',
              type: 'link',
              icon: ACTION_ICON.AUDIT,
              auth: ['mes:qms-measure-tool-apply:approve'],
              ifShow: row.status === 'APPROVING',
              onClick: () => handleApprove(row),
            },
            {
              label: '驳回',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.CLOSE,
              auth: ['mes:qms-measure-tool-apply:reject'],
              ifShow: row.status === 'APPROVING',
              onClick: () => handleReject(row),
            },
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:qms-measure-tool-apply:update'],
              ifShow: row.status === 'DRAFT' || row.status === 'REJECTED',
              onClick: () => handleEdit(row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:qms-measure-tool-apply:delete'],
              ifShow: row.status !== 'APPROVED',
              onClick: () => handleDelete(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
