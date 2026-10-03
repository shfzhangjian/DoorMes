<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSupplierExitApprovalApi } from '#/api/mes/srm/supplier-exit-approval';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Modal as AntModal, Tag, message } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSupplierExitApproval,
  getSupplierExitApprovalPage,
} from '#/api/mes/srm/supplier-exit-approval';

import SrmSupplierExitApprovalDetailModal from './modules/detail-modal.vue';

defineOptions({ name: 'SrmCertificationExitApproval' });

type DetailMode = 'create' | 'detail' | 'edit';

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVED: { color: 'success', text: '已归档' },
  DEPT_SIGN: { color: 'processing', text: '部门会签' },
  DRAFT: { color: 'default', text: '草稿' },
  ENTRY_PROCESSING: { color: 'processing', text: '交办办理' },
  GENERAL_MANAGER_REVIEW: { color: 'warning', text: '总经理审核' },
  PURCHASE_INTAKE: { color: 'processing', text: '采购部门办理' },
  PURCHASE_REVIEW: { color: 'processing', text: '采购部审批' },
  PURCHASE_TRANSFER: { color: 'warning', text: '采购部转办' },
  QUALITY_REVIEW: { color: 'processing', text: '品质部审批' },
  REJECTED: { color: 'error', text: '不通过' },
  TECH_REVIEW: { color: 'processing', text: '技术研发部审批' },
  USE_DEPT_REVIEW: { color: 'processing', text: '使用部门负责人审核' },
};

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: SrmSupplierExitApprovalDetailModal,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入单据编号' },
        fieldName: 'exitNo',
        label: '单据编号',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
        fieldName: 'supplierName',
        label: '供应商名称',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料名称' },
        fieldName: 'materialName',
        label: '物料名称',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料型号' },
        fieldName: 'materialModel',
        label: '物料型号',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: Object.entries(statusMetaMap).map(([value, meta]) => ({
            label: meta.text,
            value,
          })),
          placeholder: '请选择当前状态',
        },
        fieldName: 'status',
        label: '当前状态',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'exitNo',
        fixed: 'left',
        minWidth: 190,
        slots: { default: 'exitNo' },
        title: '单据编号',
      },
      { field: 'supplierName', minWidth: 220, title: '供应商名称' },
      { field: 'supplierCode', minWidth: 140, title: '供应商代码' },
      { field: 'materialName', minWidth: 220, title: '物料名称' },
      { field: 'materialCode', minWidth: 150, title: '物料编码' },
      { field: 'materialModel', minWidth: 160, title: '物料型号' },
      { field: 'exitReasonDesc', minWidth: 240, title: '退出原因说明' },
      {
        align: 'center',
        field: 'status',
        minWidth: 140,
        slots: { default: 'status' },
        title: '当前状态',
      },
      { field: 'applicantName', minWidth: 120, title: '申请人' },
      { field: 'applyTime', minWidth: 170, title: '申请时间' },
      { field: 'updateTime', minWidth: 170, title: '更新时间' },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 132,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSupplierExitApprovalPage({
            ...formValues,
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
          });
          return {
            list: result.list || [],
            total: result.total || 0,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmSupplierExitApprovalApi.SupplierExitApproval>,
});

function openCreate() {
  detailModalApi.setData({ mode: 'create' }).open();
}

function openDetail(
  row: SrmSupplierExitApprovalApi.SupplierExitApproval,
  mode: DetailMode = 'detail',
) {
  if (!row.id) {
    message.warning('缺少退出审批 ID');
    return;
  }
  detailModalApi.setData({ id: row.id, mode }).open();
}

function handleSuccess() {
  void gridApi.query();
}

function deleteWithConfirm(record: SrmSupplierExitApprovalApi.SupplierExitApproval) {
  AntModal.confirm({
    content: `确认删除退出审批 ${record.exitNo || ''}？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    async onOk() {
      await deleteSupplierExitApproval(Number(record.id));
      message.success('删除成功');
      await gridApi.query();
    },
  });
}

function statusText(status?: string) {
  return statusMetaMap[status || '']?.text || displayValue(status);
}

function statusColor(status?: string) {
  return statusMetaMap[status || '']?.color || 'default';
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <Button type="primary" @click="openCreate">
          <IconifyIcon icon="ant-design:plus-outlined" />
          新增退出审批
        </Button>
      </template>
      <template #exitNo="{ row }">
        <Button
          class="!px-0 font-bold"
          type="link"
          @click="openDetail(row, 'detail')"
        >
          {{ row.exitNo }}
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="statusColor(row.status)" class="!m-0">
          {{ statusText(row.status) }}
        </Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看',
              onClick: () => openDetail(row, 'detail'),
            },
            {
              disabled: row.status !== 'DRAFT',
              label: '编辑',
              onClick: () => openDetail(row, 'edit'),
            },
          ]"
          :drop-down-actions="[
            {
              disabled: row.status !== 'DRAFT',
              label: '删除',
              onClick: () => deleteWithConfirm(row),
            },
          ]"
        />
      </template>
    </Grid>
    <DetailModal @success="handleSuccess" />
  </Page>
</template>
