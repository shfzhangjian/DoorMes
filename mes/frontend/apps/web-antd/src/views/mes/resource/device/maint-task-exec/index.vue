<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { Tabs, Tag } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { exportTask, getTaskPage } from '#/api/mes/resource/device/maint-task-exec';

import { useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/detail-modal.vue';
import OrderForm from './modules/order-form.vue';

defineOptions({ name: 'MesMaintTaskExec' });

const activeTab = ref('todo');
const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: DetailModalForm, destroyOnClose: true });
const [OrderModal, orderModalApi] = useVbenModal({ connectedComponent: OrderForm, destroyOnClose: true });

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(), collapsed: false },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true },
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res: any = await getTaskPage({ pageNo: page.currentPage, pageSize: page.pageSize, tabType: activeTab.value, ...formValues });
          return { list: res.list, total: res.total };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleTabChange(key: string) { activeTab.value = key; gridApi.query(); }
function handleView(row: any) { detailModalApi.setData({ id: row.id, status: row.status }).open(); }
function handleCreate() { orderModalApi.setData({}).open(); }
async function handleExport() {
  const data = await exportTask({ ...(await gridApi.formApi.getValues()), tabType: activeTab.value });
  downloadFileFromBlobPart({ fileName: '设备保养工单.xls', source: data });
}
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />
    <OrderModal @success="gridApi.query()" />

    <div class="flex flex-col h-full bg-white">
      <div class="px-4 pt-4 border-b border-slate-200 bg-slate-50 shrink-0">
        <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
          <Tabs.TabPane key="todo" tab="⏳ 待执行任务" />
          <Tabs.TabPane key="done" tab="✅ 已完成台账" />
          <Tabs.TabPane key="all" tab="👁️ 全局保养任务" />
        </Tabs>
      </div>

      <div class="flex-1 min-h-0 relative">
        <Grid>
          <template #toolbar-tools>
            <TableAction
              :actions="[
                { label: '新增保养工单', type: 'primary', icon: ACTION_ICON.ADD, auth: ['mes:resource-device-maint-order:create'], onClick: handleCreate },
                { label: '导出', type: 'primary', icon: ACTION_ICON.DOWNLOAD, auth: ['mes:resource-device-maint-order:export'], onClick: handleExport }
              ]"
            />
          </template>

          <template #taskNo="{ row }">
            <a class="font-mono font-bold text-indigo-700 hover:underline" @click="handleView(row)">{{ row.taskNo }}</a>
          </template>

          <template #maintType="{ row }">
            <Tag v-if="row.maintType === '一级保养'" color="green" class="!m-0 border-none">{{ row.maintType }}</Tag>
            <Tag v-else-if="row.maintType === '二级保养'" color="orange" class="!m-0 border-none">{{ row.maintType }}</Tag>
            <Tag v-else-if="row.maintType === '三级大修'" color="red" class="!m-0 border-none">{{ row.maintType }}</Tag>
            <Tag v-else color="blue" class="!m-0 border-none">{{ row.maintType }}</Tag>
          </template>

          <template #status="{ row }">
            <Tag v-if="row.status === 'TODO'" color="error" class="!m-0 font-bold border-none">待执行</Tag>
            <Tag v-else-if="row.status === 'DONE'" color="success" class="!m-0 font-bold border-none">已完成</Tag>
          </template>

          <template #actions="{ row }">
            <TableAction
              :actions="[
                { label: row.status === 'TODO' ? '任务反馈' : '查看报告', type: 'link', icon: row.status === 'TODO' ? ACTION_ICON.EDIT : ACTION_ICON.VIEW, auth: row.status === 'TODO' ? ['mes:resource-device-maint-order:execute'] : undefined, onClick: handleView.bind(null, row) }
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>

<style scoped>
:deep(.vben-vxe-grid) { height: 100%; display: flex; flex-direction: column; }
:deep(.ant-tabs-nav) { margin-bottom: 0 !important; }
</style>
