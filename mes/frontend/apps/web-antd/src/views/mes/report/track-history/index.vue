<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesTrackHistoryApi } from '#/api/mes/report/track-history';

import { onMounted, onActivated } from 'vue'; // 🌟 1. 引入 Vue 生命周期钩子
import { Page, useVbenModal } from '@vben/common-ui';
import { Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTrackPage } from '#/api/mes/report/track-history';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesTrackHistory' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });

function handleDetail(row: MesTrackHistoryApi.Record) {
  formModalApi.setData({ id: row.id, trackMode: row.trackMode }).open();
}

// 🌟 2. 将 gridApi 解构出来，用于手动控制表格
const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: { query: async ({ page }, formValues) => await getTrackPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesTrackHistoryApi.Record>,
});

// 🌟 3. 核心修复逻辑：避开路由动画期，强制重载
function forceLoadData() {
  setTimeout(() => {
    gridApi.query(); // 强制触发表格请求
  }, 300); // 延迟 300 毫秒，等 Vben Admin 的路由淡入动画彻底结束
}

// 在组件挂载时，和被 KeepAlive 缓存唤醒时，都执行一次强制重载
onMounted(() => forceLoadData());
onActivated(() => forceLoadData());
</script>

<template>
  <Page auto-content-height>
    <FormModal />
    <BaseGrid table-title="现场报工过站历史">
      <template #trackMode="{ row }">
        <Tag :color="row.trackMode === 'PIECE' ? 'purple' : 'default'">
          {{ row.trackMode === 'PIECE' ? '单件赋码' : '批量报数' }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '查看明细', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
          ]"
        />
      </template>
    </BaseGrid>
  </Page>
</template>
