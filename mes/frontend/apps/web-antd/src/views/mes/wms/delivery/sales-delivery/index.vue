<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal, confirm } from '@vben/common-ui';
import { Tag, Progress, message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { IconifyIcon } from '@vben/icons';

import { useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/detail-modal.vue';

const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: DetailModalForm, destroyOnClose: true });

// 模拟数据库
const mockData = ref([
  { id: '1', deliveryNo: 'SD-20260219-001', customerName: '宁德时代 (CATL)', customerCode: 'CUST-001', totalPlanQty: 5000, totalActualQty: 0, status: 'DRAFT', contactName: '张经理', deliveryDate: '2026-02-20' },
  { id: '2', deliveryNo: 'SD-20260219-002', customerName: '比亚迪 (BYD)', customerCode: 'CUST-002', totalPlanQty: 2000, totalActualQty: 1500, status: 'PICKING', contactName: '李采购', deliveryDate: '2026-02-19' },
  { id: '3', deliveryNo: 'SD-20260218-005', customerName: '特斯拉 (Tesla)', customerCode: 'CUST-003', totalPlanQty: 1000, totalActualQty: 1000, status: 'SHIPPED', contactName: 'Mike', deliveryDate: '2026-02-18' },
]);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(), collapsed: false },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockData.value;
          if (formValues?.deliveryNo) filtered = filtered.filter(i => i.deliveryNo.includes(formValues.deliveryNo));
          if (formValues?.customerName) filtered = filtered.filter(i => i.customerName.includes(formValues.customerName));
          if (formValues?.status) filtered = filtered.filter(i => i.status === formValues.status);
          const start = (page.currentPage - 1) * page.pageSize;
          return { list: filtered.slice(start, start + page.pageSize), total: filtered.length };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleViewDetail(row?: any) {
  // 传空代表新增，传 row 代表查看/编辑
  detailModalApi.setData(row || { isNew: true }).open();
}

function handleRelease(row: any) {
  confirm({
    title: '确认下发装车任务？',
    content: `即将下发单据 ${row.deliveryNo}。下发后，现场发货操作台终端将可见此单据，允许装车扫码。`,
    okText: '确认下发',
    onOk: () => {
      row.status = 'PICKING';
      gridApi.query();
      message.success('已下发至现场作业台！');
    }
  });
}
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />

    <Grid table-title="销售发货单据池 (Delivery Orders)">
      <template #toolbar-tools>
        <TableAction :actions="[{ label: '手工建单', type: 'primary', icon: ACTION_ICON.ADD, onClick: () => handleViewDetail() }]" />
      </template>

      <template #deliveryNo="{ row }">
        <a class="font-mono font-bold text-indigo-700 hover:underline" @click="handleViewDetail(row)">{{ row.deliveryNo }}</a>
      </template>

      <template #customer="{ row }">
        <div class="flex flex-col">
          <span class="font-bold text-slate-800 text-sm truncate">{{ row.customerName }}</span>
          <span class="font-mono text-slate-400 text-[10px]">{{ row.customerCode }}</span>
        </div>
      </template>

      <template #totalPlanQty="{ row }">
        <span class="font-mono font-bold text-slate-600">{{ row.totalPlanQty }}</span>
      </template>

      <template #totalActualQty="{ row }">
        <span :class="row.totalActualQty >= row.totalPlanQty ? 'text-green-600' : 'text-orange-500'" class="font-mono font-bold">
          {{ row.totalActualQty }}
        </span>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 'DRAFT' ? 'default' : (row.status === 'PICKING' ? 'processing' : 'success')" class="!m-0 font-bold border-transparent">
          {{ row.status === 'DRAFT' ? '草稿 (待下发)' : (row.status === 'PICKING' ? '拣货装车中' : '已完成过账') }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '明细', type: 'link', icon: ACTION_ICON.PREVIEW, onClick: handleViewDetail.bind(null, row) },
            { label: '下发', type: 'link', icon: ACTION_ICON.UPLOAD, ifShow: () => row.status === 'DRAFT', onClick: handleRelease.bind(null, row) }
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
