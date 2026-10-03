<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesQmsSampleAbnormalRecheckApi } from '#/api/mes/quality/sample-abnormal-recheck';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createSampleAbnormalRecheck,
  getSampleAbnormalRecheckPage,
} from '#/api/mes/quality/sample-abnormal-recheck';
import { getRangePickerDefaultProps } from '#/utils';
import { printFaiInspectionTransferTicketById } from '#/views/mes/quality/fai/shared/faiPrint';

defineOptions({ name: 'MesQmsSampleAbnormalRecheck' });

const lockStatusOptions = [
  { label: '锁定中', value: 'LOCKED' },
  { label: '已解锁', value: 'RELEASED' },
  { label: '已关闭', value: 'COMPLETED' },
  { label: '已驳回', value: 'REJECTED' },
];

const objectTypeOptions = [
  { label: '母卷', value: 'MOTHER_ROLL' },
  { label: '分段', value: 'SEGMENT' },
];

const processOptions = [
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
];

const resultOptions = [
  { label: 'OK', value: 'OK' },
  { label: 'NG', value: 'NG' },
];

const recheckLoadingId = ref<number>();

function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'lockNo', label: '锁定单号', component: 'Input', componentProps: { placeholder: '输入锁定单号' } },
    { fieldName: 'objectNo', label: '母卷/分段', component: 'Input', componentProps: { placeholder: '输入母卷或分段号' } },
    { fieldName: 'sourceProcessCode', label: '异常工序', component: 'Select', componentProps: { options: processOptions, allowClear: true } },
    { fieldName: 'lockStatus', label: '锁定状态', component: 'Select', componentProps: { options: lockStatusOptions, allowClear: true } },
    { fieldName: 'objectType', label: '对象类型', component: 'Select', componentProps: { options: objectTypeOptions, allowClear: true } },
    { fieldName: 'abnormalInspectionNo', label: '异常单号', component: 'Input', componentProps: { placeholder: '输入异常检验单号' } },
    { fieldName: 'recheckInspectionNo', label: '复检单号', component: 'Input', componentProps: { placeholder: '输入复检检验单号' } },
    { fieldName: 'planNo', label: '工单/计划', component: 'Input', componentProps: { placeholder: '输入工单或计划号' } },
    { fieldName: 'abnormalFeedbackTime', label: '异常反馈时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps() } },
    { fieldName: 'recheckFeedbackTime', label: '复检反馈时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps() } },
  ];
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: [
      { type: 'seq', width: 50, align: 'center', fixed: 'left' },
      { field: 'lockStatus', title: '状态', width: 110, align: 'center', fixed: 'left', slots: { default: 'lockStatus' } },
      { field: 'objectNo', title: '母卷/分段', width: 160, fixed: 'left' },
      { field: 'objectType', title: '对象', width: 90, align: 'center', slots: { default: 'objectType' } },
      { field: 'sourceProcessName', title: '异常工序', width: 100, align: 'center', slots: { default: 'sourceProcess' } },
      { field: 'abnormalInspectionNo', title: '异常检验单号', minWidth: 170 },
      { field: 'abnormalResult', title: '异常结果', width: 100, align: 'center', slots: { default: 'abnormalResult' } },
      { field: 'abnormalFeedbackTime', title: '异常反馈时间', width: 170 },
      { field: 'recheckInspectionNo', title: '复检检验单号', minWidth: 170 },
      { field: 'recheckResult', title: '复检结果', width: 110, align: 'center', slots: { default: 'recheckResult' } },
      { field: 'recheckFeedbackTime', title: '复检反馈时间', width: 170 },
      { field: 'recheckCount', title: '复检次数', width: 90, align: 'center' },
      { field: 'planNo', title: '工单/计划', minWidth: 150 },
      { field: 'motherBatchNo', title: '母卷号', minWidth: 150 },
      { field: 'segmentNo', title: '分段号', minWidth: 150 },
      { field: 'lockReason', title: '锁定原因', minWidth: 300, slots: { default: 'lockReason' } },
      { field: 'releaseReason', title: '解锁原因', minWidth: 220 },
      { field: 'actions', title: '操作', width: 210, align: 'center', fixed: 'right', slots: { default: 'actions' } },
    ],
    height: 'auto',
    keepSource: true,
    rowClassName: ({ row }) => (row.lockStatus === 'LOCKED' ? 'sample-abnormal-lock-row' : ''),
    rowConfig: { isHover: true, keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getSampleAbnormalRecheckPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<MesQmsSampleAbnormalRecheckApi.Record>,
});

function optionText(options: Array<{ label: string; value: string }>, value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

function statusColor(status?: string) {
  if (status === 'LOCKED') return 'error';
  if (status === 'RELEASED') return 'success';
  if (status === 'COMPLETED') return 'blue';
  if (status === 'REJECTED') return 'default';
  return 'processing';
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'processing';
}

function isPendingRecheck(row: MesQmsSampleAbnormalRecheckApi.Record) {
  return !!row.recheckInspectionId && row.recheckResult !== 'OK' && row.recheckResult !== 'NG';
}

function canCreateRecheck(row: MesQmsSampleAbnormalRecheckApi.Record) {
  return row.lockStatus === 'LOCKED' && !isPendingRecheck(row);
}

function printTargetId(row: MesQmsSampleAbnormalRecheckApi.Record) {
  return row.recheckInspectionId || row.abnormalInspectionId;
}

async function refreshGrid() {
  await gridApi.query();
}

function handleCreateRecheck(row: MesQmsSampleAbnormalRecheckApi.Record) {
  if (!row.id) return;
  if (isPendingRecheck(row)) {
    message.warning('当前复检单尚未反馈结果，请刷新后查看。');
    return;
  }
  if (row.lockStatus !== 'LOCKED') {
    message.info('当前记录已解锁，无需创建复检单。');
    return;
  }
  Modal.confirm({
    content: `确认针对 ${row.objectNo || '-'} 创建一份留样异常复检送检单？`,
    async onOk() {
      recheckLoadingId.value = row.id;
      try {
        const result = await createSampleAbnormalRecheck(row.id!);
        message.success('复检送检单已创建');
        await gridApi.query();
        if (result.recheckInspectionId) {
          Modal.confirm({
            content: '是否立即打印复检送检流转单？',
            onOk: () => printFaiInspectionTransferTicketById(result.recheckInspectionId!),
            title: '复检单已创建',
          });
        }
      } finally {
        recheckLoadingId.value = undefined;
      }
    },
    title: '创建异常复检',
  });
}

function handlePrint(row: MesQmsSampleAbnormalRecheckApi.Record) {
  const id = printTargetId(row);
  if (!id) {
    message.warning('当前记录没有可打印的检验单。');
    return;
  }
  printFaiInspectionTransferTicketById(id);
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <Button @click="refreshGrid">
          <IconifyIcon class="mr-1" icon="lucide:refresh-cw" />
          刷新
        </Button>
      </template>

      <template #lockStatus="{ row }">
        <Tag :color="statusColor(row.lockStatus)">
          {{ optionText(lockStatusOptions, row.lockStatus) }}
        </Tag>
      </template>

      <template #objectType="{ row }">
        <Tag color="blue">{{ optionText(objectTypeOptions, row.objectType) }}</Tag>
      </template>

      <template #sourceProcess="{ row }">
        <Tag color="purple">
          {{ row.sourceProcessName || optionText(processOptions, row.sourceProcessCode) }}
        </Tag>
      </template>

      <template #abnormalResult="{ row }">
        <Tag :color="resultColor(row.abnormalResult)">
          {{ row.abnormalResult || '-' }}
        </Tag>
      </template>

      <template #recheckResult="{ row }">
        <Tag v-if="isPendingRecheck(row)" color="processing">待反馈</Tag>
        <Tag v-else :color="resultColor(row.recheckResult)">
          {{ row.recheckResult || '未复检' }}
        </Tag>
      </template>

      <template #lockReason="{ row }">
        <span class="lock-reason">{{ row.lockReason || '-' }}</span>
      </template>

      <template #actions="{ row }">
        <div class="sample-abnormal-actions">
          <Button
            v-access:code="['mes:qms-sample-abnormal-recheck:create-recheck']"
            :disabled="!canCreateRecheck(row)"
            :loading="recheckLoadingId === row.id"
            size="small"
            type="primary"
            @click="handleCreateRecheck(row)"
          >
            <IconifyIcon class="mr-1" icon="lucide:clipboard-plus" />
            异常复检
          </Button>
          <Button
            v-access:code="['mes:qms-sample-abnormal-recheck:print']"
            :disabled="!printTargetId(row)"
            size="small"
            @click="handlePrint(row)"
          >
            <IconifyIcon class="mr-1" icon="lucide:printer" />
            打印
          </Button>
        </div>
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.sample-abnormal-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.lock-reason {
  display: inline-block;
  line-height: 1.45;
  white-space: normal;
}

:deep(.sample-abnormal-lock-row td) {
  background-color: #fff1f0;
}
</style>
