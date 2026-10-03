<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { h, onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Input, Modal, message, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchConfirmMeasureToolCalibrationTasks,
  cancelMeasureToolCalibrationTask,
  exportMeasureToolCalibrationTask,
  generateMeasureToolCalibrationTasks,
  getMeasureToolCalibrationTaskPage,
  getMeasureToolCategoryList,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import RecordFormComponent from '../calibration-record/modules/form.vue';
import {
  TASK_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
  loadCategoryOptions,
  optionColor,
  optionLabel,
} from '../shared';

defineOptions({ name: 'MesQmsMeasureToolCalibrationTask' });

const categoryOptions = ref<Array<{ label?: string; value?: number }>>([]);
const [RecordModal, recordModalApi] = useVbenModal({ connectedComponent: RecordFormComponent, destroyOnClose: true });

function handleRefresh() {
  gridApi.query();
}

function handleComplete(row: QmsMeasureToolApi.CalibrationTask) {
  recordModalApi
    .setData(row.recordId
      ? { id: row.recordId }
      : { ledgerId: row.ledgerId, sourceType: 'TASK', taskId: row.id })
    .open();
}

async function handleGenerate() {
  const created = await generateMeasureToolCalibrationTasks({});
  message.success(`已生成 ${created} 条校准预警任务`);
  handleRefresh();
}

function handleCancel(row: QmsMeasureToolApi.CalibrationTask) {
  let reason = '';
  Modal.confirm({
    title: `取消校准任务：${row.taskNo}`,
    content: () =>
      h(Input.TextArea, {
        rows: 3,
        placeholder: '请输入取消原因',
        'onUpdate:value': (value: string) => (reason = value),
      }),
    async onOk() {
      await cancelMeasureToolCalibrationTask({ id: row.id!, reason: reason.trim() || undefined });
      message.success('任务已取消');
      handleRefresh();
    },
  });
}

async function handleConfirmTask(row: QmsMeasureToolApi.CalibrationTask) {
  if (!row.id) return;
  await confirm(`确认关闭任务 [${row.taskNo || row.toolCode}] 并反写台账吗？`);
  const confirmedCount = await batchConfirmMeasureToolCalibrationTasks({ ids: [row.id] });
  message.success(`已确认 ${confirmedCount} 条记录`);
  handleRefresh();
}

async function handleExport() {
  const data = await exportMeasureToolCalibrationTask(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '量检具校准预警任务.xls', source: data });
}

async function handleManualRefresh() {
  await confirm('将按台账下次校准日期重新生成到期/即将到期任务，确认继续？');
  await handleGenerate();
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
          await getMeasureToolCalibrationTaskPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<QmsMeasureToolApi.CalibrationTask>,
});

onMounted(async () => {
  categoryOptions.value = await loadCategoryOptions(getMeasureToolCategoryList);
  await gridApi.formApi.updateSchema(useGridFormSchema(categoryOptions.value));
});
</script>

<template>
  <Page auto-content-height>
    <RecordModal @success="handleRefresh" />
    <Grid table-title="量检具校准预警任务">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '生成预警任务',
              type: 'primary',
              icon: ACTION_ICON.REFRESH,
              auth: ['mes:qms-measure-tool-calibration-task:generate'],
              onClick: handleManualRefresh,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:qms-measure-tool-calibration-task:export'],
              onClick: handleExport,
            },
          ]"
        />
      </template>

      <template #warningStatus="{ row }">
        <Tag :color="optionColor(WARNING_STATUS_OPTIONS, row.warningStatus)">
          {{ optionLabel(WARNING_STATUS_OPTIONS, row.warningStatus) }}
        </Tag>
      </template>

      <template #taskStatus="{ row }">
        <Tag :color="optionColor(TASK_STATUS_OPTIONS, row.taskStatus)">
          {{ optionLabel(TASK_STATUS_OPTIONS, row.taskStatus) }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: row.recordId ? '查看/维护详情' : '提交校准详情',
              type: 'link',
              icon: ACTION_ICON.AUDIT,
              auth: ['mes:qms-measure-tool-calibration-task:complete'],
              ifShow: !!row.recordId || (row.taskStatus !== 'COMPLETED' && row.taskStatus !== 'CANCELLED'),
              onClick: () => handleComplete(row),
            },
            {
              label: '确认',
              type: 'link',
              icon: ACTION_ICON.AUDIT,
              ifShow: row.taskStatus === 'IN_PROGRESS' && !!row.recordId,
              onClick: () => handleConfirmTask(row),
            },
            {
              label: '取消',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.CLOSE,
              auth: ['mes:qms-measure-tool-calibration-task:cancel'],
              ifShow: row.taskStatus !== 'COMPLETED' && row.taskStatus !== 'CANCELLED',
              onClick: () => handleCancel(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
