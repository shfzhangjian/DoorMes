<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal, confirm } from '@vben/common-ui';
import { Tag, Progress, message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { useGridColumns, useGridFormSchema } from './data';
import CreateForm from './modules/create-form.vue';
import DetailForm from './modules/detail-form.vue';

const [CreateModal, createModalApi] = useVbenModal({ connectedComponent: CreateForm, destroyOnClose: true });
const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: DetailForm, destroyOnClose: true });

// 模拟后端数据库
const mockData = ref([
  { id: '1', planNo: 'CC-20260219-001', type: '动态盘点 (动盘)', scopeDesc: '按库位: A区-树脂存放区', status: 'DRAFT', progress: 0, creator: '张主管', createTime: '2026-02-19 08:00:00' },
  { id: '2', planNo: 'CC-20260218-005', type: '月底静态全盘', scopeDesc: '按维度: 全仓盲盘', status: 'COUNTING', progress: 65, creator: '李账务', createTime: '2026-02-18 17:30:00' }
]);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 自动充满剩余高度，分页沉底
    keepSource: true,
    pagerConfig: { enabled: true },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockData.value;
          if (formValues?.planNo) filtered = filtered.filter(item => item.planNo.includes(formValues.planNo));
          if (formValues?.status) filtered = filtered.filter(item => item.status === formValues.status);

          return {
            items: filtered, // 兼容标准 Vben
            list: filtered,  // 🌟 兼容 RuoYi-Vue-Pro 底层，彻底解决表格空白！
            total: filtered.length
          };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleCreate() { createModalApi.open(); }
function handleViewDetail(row: any) { detailModalApi.setData(row).open(); }

function handleRelease(row: any) {
  confirm({
    title: '⚠️ 强警告：确认下发盘点任务？',
    content: `系统将立即锁定单据 ${row.planNo} 的草稿明细，并【冻结相关库区账务】！`,
    okText: '确认冻结并下发', okType: 'danger',
    onOk: async () => {
      row.status = 'COUNTING';
      gridApi.query();
      message.success('计划已下发执行！');
    },
  });
}

function handleCreateSuccess() {
  mockData.value.unshift({
    id: Date.now().toString(), planNo: `CC-20260219-${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`,
    type: '动态盘点', scopeDesc: '按配置策略生成', status: 'DRAFT', creator: '当前用户', createTime: new Date().toLocaleString(), progress: 0
  });
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <CreateModal @success="handleCreateSuccess" />
    <DetailModal @success="gridApi.query()" />

    <Grid table-title="盘点计划单据池">
      <template #toolbar-tools>
        <TableAction :actions="[{ label: '生成盘点策略快照', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate }]" />
      </template>
      <template #planNo="{ row }"><a class="font-mono font-bold text-indigo-600 cursor-pointer" @click="handleViewDetail(row)">{{ row.planNo }}</a></template>
      <template #status="{ row }">
        <Tag :color="row.status === 'DRAFT' ? 'default' : (row.status === 'COUNTING' ? 'processing' : 'success')">{{ row.status === 'DRAFT' ? '草稿(待维护)' : (row.status === 'COUNTING' ? '盘点中' : '已完成') }}</Tag>
      </template>
      <template #progress="{ row }"><Progress :percent="row.progress" size="small" /></template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '明细与维护', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleViewDetail.bind(null, row) },
            { label: '下发冻结', type: 'link', danger: true, icon: ACTION_ICON.UPLOAD, ifShow: () => row.status === 'DRAFT', onClick: handleRelease.bind(null, row) }
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
