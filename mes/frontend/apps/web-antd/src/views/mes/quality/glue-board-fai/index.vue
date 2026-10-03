<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps, VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Radio, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditGlueBoardFaiProgramEntry,
  getGlueBoardFaiPage,
  oneClickPassGlueBoardFai,
} from '#/api/mes/quality/fai';
import { getRangePickerDefaultProps } from '#/utils';

import {
  faiStatusOptions,
  processCategoryOptions,
  resolveFaiOperationLabel,
  sortFaiRecordsByStatus,
} from '../fai/data';
import DetailModalVue from '../fai/modules/detail-modal.vue';
import QmsFaiScanResolveModal from '../fai/modules/qms-fai-scan-resolve-modal.vue';
import WorkbenchMode from '../fai/modules/workbench.vue';
import QmsGlueBoardFaiCreateModal from './modules/qms-glue-board-fai-create-modal.vue';

defineOptions({ name: 'MesQualityGlueBoardFai' });

const GLUE_BOARD_SOURCE_MODULE = 'GLUE_BOARD_FAI';

const viewMode = ref<'LEDGER' | 'OVERVIEW'>('LEDGER');
const activeOverviewId = ref<number>();
const activeOverviewRecord = ref<MesFaiApi.FaiRecord>();
const scanModalOpen = ref(false);
const auditModalOpen = ref(false);
const auditTargetRecord = ref<MesFaiApi.FaiRecord>();
const auditResult = ref<MesFaiApi.AuditResult>('PASS');
const rejectReason = ref('');
const route = useRoute();
const handledRouteCreateKey = ref('');

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalVue,
  destroyOnClose: true,
});
const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: QmsGlueBoardFaiCreateModal,
  destroyOnClose: true,
});

const searchFormFields = [
  'faiNo',
  'processCategory',
  'glueBoardModel',
  'gluePlateBatchNo',
  'status',
  'judgment',
  'submissionTime',
  'inspectionTime',
  'qaTime',
  'submitterName',
];

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function useGlueBoardGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'faiNo',
      label: '检验单号',
      component: 'Input',
      componentProps: { placeholder: '输入单号' },
    },
    {
      fieldName: 'processCategory',
      label: '工序',
      component: 'Select',
      componentProps: { options: processCategoryOptions, allowClear: true },
    },
    {
      fieldName: 'glueBoardModel',
      label: '胶板型号',
      component: 'Input',
      componentProps: { placeholder: '输入胶板型号' },
    },
    {
      fieldName: 'gluePlateBatchNo',
      label: '胶板批次',
      component: 'Input',
      componentProps: { placeholder: '输入胶板批次' },
    },
    {
      fieldName: 'status',
      label: '单据状态',
      component: 'Select',
      componentProps: { options: faiStatusOptions, allowClear: true },
    },
    {
      fieldName: 'judgment',
      label: '判定结果',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '待判定', value: '-' },
          { label: '待判定', value: 'PENDING' },
          { label: '合格', value: 'OK' },
          { label: '不合格', value: 'NG' },
        ],
      },
    },
    {
      fieldName: 'submissionTime',
      label: '送检时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
    {
      fieldName: 'inspectionTime',
      label: '检验时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
    {
      fieldName: 'qaTime',
      label: '审核时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() },
    },
    {
      fieldName: 'submitterName',
      label: '送检人员',
      component: 'Input',
      componentProps: { placeholder: '输入送检人员' },
    },
  ];
}

function formatDateTimeText(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}

function glueBoardInspectionTime(row: MesFaiApi.FaiRecord) {
  return formatDateTimeText(row.inspectionTime || row.operatorTime);
}

function useGlueBoardGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    {
      field: 'faiNo',
      title: '检验单号',
      width: 160,
      fixed: 'left',
      slots: { default: 'faiNo' },
    },
    {
      field: 'processCategory',
      title: '工序',
      width: 100,
      align: 'center',
      slots: { default: 'processCategory' },
    },
    { field: 'glueBoardModel', title: '胶板型号', minWidth: 150 },
    { field: 'materialCode', title: '胶板料号', minWidth: 140 },
    { field: 'gluePlateBatchNo', title: '胶板批次', minWidth: 150 },
    { field: 'sampleLength', title: '送检米数(m)', width: 120, align: 'right' },
    {
      field: 'status',
      title: '单据状态',
      width: 120,
      align: 'center',
      slots: { default: 'inspectionStatus' },
    },
    {
      field: 'judgment',
      title: '判定结果',
      width: 110,
      align: 'center',
      slots: { default: 'qualifiedStatus' },
    },
    { field: 'submissionTime', title: '送检时间', width: 160, align: 'center' },
    { field: 'submitterName', title: '送检人员', width: 100, align: 'center' },
    {
      field: 'inspectionTime',
      title: '检验时间',
      width: 160,
      align: 'center',
      formatter: ({ row }) => glueBoardInspectionTime(row),
    },
    { field: 'operatorName', title: '检验人', width: 100, align: 'center' },
    {
      field: 'qaTime',
      title: '审核时间',
      width: 160,
      align: 'center',
      formatter: ({ row }) => formatDateTimeText(row.qaTime),
    },
    { field: 'qaInspectorName', title: '审核人', width: 100, align: 'center' },
    { field: 'remark', title: '备注', minWidth: 180 },
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 300,
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGlueBoardGridFormSchema().map((item) => ({
    ...item,
    hide: collapsed && !keepFields.has(item.fieldName),
  }));
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: useGlueBoardGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getGlueBoardFaiPage({
            ...formValues,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          });
          return {
            ...result,
            list: sortFaiRecordsByStatus(result.list || []),
          };
        },
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
}

function handleViewDetail(row: MesFaiApi.FaiRecord) {
  detailModalApi.setData({ apiMode: 'GLUE_BOARD', id: row.id }).open();
}

function handleOpenOverview(row: MesFaiApi.FaiRecord) {
  activeOverviewId.value = row.id;
  activeOverviewRecord.value = row;
  viewMode.value = 'OVERVIEW';
}

function handleScanResolved(resp: MesFaiApi.FaiScanResp) {
  if (!resp.record?.id) {
    if (resp.message) message.info(resp.message);
    return;
  }
  if (resp.record.sourceModule !== GLUE_BOARD_SOURCE_MODULE) {
    message.warning('当前扫码内容不是胶板检验单，请在首件检验(FAI)页面处理');
    return;
  }
  if (resp.openTarget === 'REPORT') {
    handleViewDetail(resp.record);
    return;
  }
  handleOpenOverview(resp.record);
}

function handleCreateSuccess(record: MesFaiApi.FaiRecord) {
  gridApi.query();
  handleOpenOverview(record);
}

function getQueryString(value: unknown) {
  if (Array.isArray(value)) return String(value[0] || '').trim();
  return String(value || '').trim();
}

function buildCreateModalDefaultsFromQuery() {
  const shouldAutoCreate =
    getQueryString(route.query.autoCreate) === '1' ||
    getQueryString(route.query.from) === 'adhesive2-glue-board';
  if (!shouldAutoCreate) return undefined;
  return {
    glueBoardModel: getQueryString(route.query.glueBoardModel),
    gluePlateBatchNo: getQueryString(route.query.gluePlateBatchNo),
    processCategory: getQueryString(route.query.processCategory),
    sampleLength: Number(getQueryString(route.query.sampleLength)) || undefined,
    submitterName: getQueryString(route.query.submitterName),
    workOrderNo: getQueryString(route.query.workOrderNo),
  };
}

watch(
  () => route.query,
  () => {
    const defaults = buildCreateModalDefaultsFromQuery();
    if (!defaults) return;
    const routeCreateKey = [
      defaults.glueBoardModel,
      defaults.gluePlateBatchNo,
      defaults.processCategory,
      defaults.sampleLength,
      defaults.workOrderNo,
    ].join('|');
    if (handledRouteCreateKey.value === routeCreateKey) return;
    handledRouteCreateKey.value = routeCreateKey;
    createModalApi.setData(defaults).open();
  },
  { immediate: true },
);

function canAudit(row: MesFaiApi.FaiRecord) {
  return row.status === 'WAITING_QA';
}

function canOneClickPass(row: MesFaiApi.FaiRecord) {
  return !['CANCELED', 'COMPLETED', 'REJECTED'].includes(row.status || '');
}

function handleOpenAudit(row: MesFaiApi.FaiRecord) {
  if (!canAudit(row)) {
    message.warning('胶板检验单提交检测结果后才允许审核');
    return;
  }
  auditTargetRecord.value = row;
  auditResult.value = 'PASS';
  rejectReason.value = '';
  auditModalOpen.value = true;
}

function resetAuditModal() {
  auditTargetRecord.value = undefined;
  auditResult.value = 'PASS';
  rejectReason.value = '';
}

async function handleAuditSubmit() {
  if (!auditTargetRecord.value?.id) return;
  if (auditResult.value === 'REJECT' && !rejectReason.value.trim()) {
    message.warning('请填写驳回原因');
    return;
  }
  await auditGlueBoardFaiProgramEntry({
    id: auditTargetRecord.value.id,
    auditResult: auditResult.value,
    rejectReason: ['FAIL', 'REJECT'].includes(auditResult.value)
      ? rejectReason.value.trim() || undefined
      : undefined,
  });
  message.success(resolveAuditSuccessMessage(auditResult.value));
  auditModalOpen.value = false;
  resetAuditModal();
  gridApi.query();
}

function handleOneClickPass(row: MesFaiApi.FaiRecord) {
  if (!row.id) return;
  if (!canOneClickPass(row)) {
    message.warning('当前胶板检验单已结束，不允许一键合格');
    return;
  }
  Modal.confirm({
    title: '一键合格',
    content: `确认将胶板检验单 ${row.faiNo || '-'} 的所有检测项置为合格，并同步更新胶板边库质量状态？`,
    okText: '确认合格',
    cancelText: '取消',
    onOk: async () => {
      await oneClickPassGlueBoardFai(row.id!);
      message.success('胶板检验单已一键合格');
      gridApi.query();
    },
  });
}

function resolveAuditSuccessMessage(result: MesFaiApi.AuditResult) {
  if (result === 'PASS') return '胶板检验单已审核通过';
  if (result === 'FAIL') return '胶板检验单已判定不合格';
  return '胶板检验单已驳回返工';
}

function statusLabel(row: MesFaiApi.FaiRecord) {
  if (row.status === 'REJECTED' && row.judgment === 'NG') return '不合格';
  return (
    faiStatusOptions.find((item) => item.value === row.status)?.label ||
    row.status ||
    '-'
  );
}

function statusColor(row: MesFaiApi.FaiRecord) {
  if (row.status === 'COMPLETED') return 'success';
  if (row.status === 'REJECTED' || row.status === 'CANCELED') return 'error';
  if (row.status === 'WAITING_QA') return 'purple';
  if (row.status === 'PENDING') return 'warning';
  return 'blue';
}

function qualifiedLabel(row: MesFaiApi.FaiRecord) {
  if (row.judgment === 'OK') return '合格';
  if (row.judgment === 'NG') return '不合格';
  return '待判定';
}
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <CreateModal @success="handleCreateSuccess" />
    <QmsFaiScanResolveModal
      v-model:open="scanModalOpen"
      order-no-title="检验单号"
      placeholder="请扫描胶板检验单、胶板批次、胶板料号或工单号"
      scene="LEDGER_TOOLBAR"
      :source-module="GLUE_BOARD_SOURCE_MODULE"
      title="扫码填写胶板检验单"
      @resolved="handleScanResolved"
    />
    <div v-show="viewMode === 'LEDGER'" class="flex h-full flex-col">
      <Grid table-title="胶板检验记录">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:glue-board-fai:create']"
              type="primary"
              @click="createModalApi.setData({}).open()"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增
            </Button>
            <Button
              v-access:code="['mes:glue-board-fai:workbench']"
              type="primary"
              @click="scanModalOpen = true"
            >
              <IconifyIcon icon="lucide:scan-line" class="mr-1" /> 扫码填写
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>
        <template #faiNo="{ row }">
          <span class="font-mono font-bold text-indigo-700">{{
            row.faiNo
          }}</span>
        </template>
        <template #processCategory="{ row }">
          <Tag color="blue" class="!m-0">
            {{ resolveFaiOperationLabel(row) }}
          </Tag>
        </template>
        <template #inspectionStatus="{ row }">
          <Tag :color="statusColor(row)" class="!m-0">
            {{ statusLabel(row) }}
          </Tag>
        </template>
        <template #qualifiedStatus="{ row }">
          <Tag
            :color="
              row.judgment === 'OK'
                ? 'success'
                : row.judgment === 'NG'
                  ? 'error'
                  : 'default'
            "
            class="!m-0"
          >
            {{ qualifiedLabel(row) }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:glue-board-fai:query'],
                onClick: () => handleViewDetail(row),
              },
              {
                label: '数据录入',
                type: 'link',
                icon: 'lucide:keyboard',
                auth: ['mes:glue-board-fai:workbench'],
                onClick: () => handleOpenOverview(row),
              },
              {
                label: '审核',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:glue-board-fai:confirm'],
                disabled: row.status !== 'WAITING_QA',
                onClick: () => handleOpenAudit(row),
              },
              {
                label: '一键合格',
                type: 'link',
                icon: 'lucide:check-check',
                auth: ['mes:glue-board-fai:confirm'],
                disabled: !canOneClickPass(row),
                onClick: () => handleOneClickPass(row),
              },
            ]"
          />
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="auditModalOpen"
      title="胶板检验整单审核"
      @cancel="resetAuditModal"
      @ok="handleAuditSubmit"
    >
      <div class="space-y-4">
        <div class="text-sm text-slate-600">
          {{ auditTargetRecord?.faiNo || '-' }}
        </div>
        <Radio.Group v-model:value="auditResult">
          <Radio value="PASS">通过</Radio>
          <Radio value="REJECT">驳回</Radio>
          <Radio value="FAIL">不合格</Radio>
        </Radio.Group>
        <Input.TextArea
          v-if="['FAIL', 'REJECT'].includes(auditResult)"
          v-model:value="rejectReason"
          :maxlength="300"
          :rows="4"
          :placeholder="
            auditResult === 'FAIL'
              ? '请输入不合格说明（选填）'
              : '请输入驳回原因'
          "
          show-count
        />
      </div>
    </Modal>

    <WorkbenchMode
      v-if="viewMode === 'OVERVIEW'"
      api-mode="GLUE_BOARD"
      :enable-import="false"
      :enable-scan="false"
      :initial-record-id="activeOverviewId"
      :initial-record="activeOverviewRecord"
      page-description="按胶板检验标准填写检测数据，保存、提交和审核流程复用 FAI 核心链路"
      page-title="胶板检验项明细概览"
      @back-to-ledger="
        viewMode = 'LEDGER';
        activeOverviewRecord = undefined;
        gridApi.query();
      "
    />
  </Page>
</template>
