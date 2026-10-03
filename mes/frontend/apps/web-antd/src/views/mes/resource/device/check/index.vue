<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { Tabs, TabPane, Tag, message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deletePlanList, getPlanPage, deleteOrderList, getOrderPage } from '#/api/mes/resource/device/check';

import { usePlanGridColumns, useOrderGridColumns } from './data';
import PlanFormComponent from './modules/plan-form.vue';
import OrderFormComponent from './modules/order-form.vue';

defineOptions({ name: 'MesMaintCheck' });

const activeKey = ref('plan');

const [PlanFormModal, planModalApi] = useVbenModal({ connectedComponent: PlanFormComponent, destroyOnClose: true });
const [OrderFormModal, orderModalApi] = useVbenModal({ connectedComponent: OrderFormComponent, destroyOnClose: true });

// ================= Grid 1 =================
const [PlanGrid, planGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: usePlanGridColumns(),
    // Vben 5.x 规范: height 为 auto 时，会贪婪吸取父级的剩余高度，实现表头/分页固定，表体滚动
    height: 'auto',
    proxyConfig: { ajax: { query: async ({ page }) => await getPlanPage({ pageNo: page.currentPage, pageSize: page.pageSize }) } },
    rowConfig: { keyField: 'id' },
    toolbarConfig: { refresh: true },
  } as VxeTableGridOptions<any>,
});

function handlePlanCreate() { planModalApi.setData({ type: 'create' }).open(); }
function handlePlanEdit(row: any) { planModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handlePlanDetail(row: any) { planModalApi.setData({ type: 'detail', id: row.id }).open(); }
async function handlePlanDelete(row: any) { await deletePlanList([row.id]); message.success('删除成功'); planGridApi.query(); }

// ================= Grid 2 =================
const [OrderGrid, orderGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useOrderGridColumns(),
    height: 'auto',
    proxyConfig: { ajax: { query: async ({ page }) => await getOrderPage({ pageNo: page.currentPage, pageSize: page.pageSize }) } },
    rowConfig: { keyField: 'id' },
    toolbarConfig: { refresh: true },
  } as VxeTableGridOptions<any>,
});

function handleOrderCreate() { orderModalApi.setData({ type: 'create' }).open(); }
function handleOrderEdit(row: any) { orderModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleOrderDetail(row: any) { orderModalApi.setData({ type: 'detail', id: row.id }).open(); }
async function handleOrderDelete(row: any) { await deleteOrderList([row.id]); message.success('删除成功'); orderGridApi.query(); }
</script>

<template>
  <Page auto-content-height>
    <PlanFormModal @success="planGridApi.query()" />
    <OrderFormModal @success="orderGridApi.query()" />

    <div class="h-full bg-white flex flex-col px-4 pt-2">

      <Tabs v-model:activeKey="activeKey" class="shrink-0">
        <TabPane key="plan" tab="维保策略与计划" />
        <TabPane key="order" tab="周期性维保工单" />
      </Tabs>

      <div class="flex-1 overflow-hidden pb-4">

        <PlanGrid v-if="activeKey === 'plan'">
          <template #toolbar-tools>
            <a-button type="primary" @click="handlePlanCreate">创建策略</a-button>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: '详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: handlePlanDetail.bind(null, row) },
                { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, onClick: handlePlanEdit.bind(null, row) },
                { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认删除?', confirm: handlePlanDelete.bind(null, row) } },
              ]"
            />
          </template>
        </PlanGrid>

        <OrderGrid v-if="activeKey === 'order'">
          <template #toolbar-tools>
            <a-button type="primary" @click="handleOrderCreate">下发工单</a-button>
          </template>
          <template #status="{ row }">
            <Tag v-if="row.status === 10" color="default">待执行</Tag>
            <Tag v-else-if="row.status === 20" color="warning">执行中</Tag>
            <Tag v-else-if="row.status === 30" color="success">已完成</Tag>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: '详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleOrderDetail.bind(null, row) },
                { label: '执行反馈', type: 'link', icon: ACTION_ICON.EDIT, ifShow: () => row.status !== 30, onClick: handleOrderEdit.bind(null, row) },
                { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, popConfirm: { title: '确认删除?', confirm: handleOrderDelete.bind(null, row) } },
              ]"
            />
          </template>
        </OrderGrid>

      </div>
    </div>
  </Page>
</template>

<style scoped>
/* 可选：微调 AntD Tabs 的底边距，使其与表格衔接更紧凑 */
:deep(.ant-tabs-nav) {
  margin-bottom: 8px !important;
}
</style>
