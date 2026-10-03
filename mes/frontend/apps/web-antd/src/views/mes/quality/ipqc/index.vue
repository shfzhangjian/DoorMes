<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { Segmented, Tag, Button } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getIpqcPage } from '#/api/mes/quality/ipqc';

import { useGridColumns, useGridFormSchema } from './data';
import WorkbenchMode from './modules/workbench.vue';
import DetailModalVue from './modules/detail-modal.vue';

defineOptions({ name: 'MesQualityIpqc' });

const viewMode = ref<'LEDGER' | 'WORKBENCH'>('LEDGER');
const [DetailModal, detailModalApi] = useVbenModal({ connectedComponent: DetailModalVue, destroyOnClose: true });

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(), height: 'auto', keepSource: true, rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getIpqcPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

function handleViewDetail(row: any) { detailModalApi.setData({ id: row.id }).open(); }
</script>

<template>
  <Page auto-content-height class="relative">
    <DetailModal />
    <template #extra>
      <Segmented v-model:value="viewMode" :options="[{label:'历史巡检台账', value:'LEDGER'}, {label:'进入车间巡检雷达', value:'WORKBENCH'}]" class="font-bold shadow-sm" />
    </template>

    <div v-show="viewMode === 'LEDGER'" class="h-full flex flex-col">
      <Grid table-title="过程抽检 (IPQC) 记录表">
        <template #toolbar-tools>
          <Button type="primary" class="bg-indigo-600" @click="viewMode = 'WORKBENCH'"><IconifyIcon icon="lucide:external-link" class="mr-1"/> 开启车间巡检模式</Button>
        </template>
        <template #ipqcNo="{ row }"><span class="font-mono font-bold text-indigo-700">{{ row.ipqcNo }}</span></template>
        <template #machineCode="{ row }"><Tag color="blue" class="font-mono !m-0">{{ row.machineCode }}</Tag></template>
        <template #inspectionType="{ row }"><Tag :color="row.inspectionType === 'ROUTINE' ? 'default' : 'orange'">{{ row.inspectionType === 'ROUTINE' ? '常规巡检' : '异常复测' }}</Tag></template>
        <template #judgment="{ row }">
          <Tag v-if="row.judgment === 'OK'" color="success" class="!m-0 font-bold border-none">受控 (OK)</Tag>
          <Tag v-else color="error" class="!m-0 font-bold border-none">异常 (NG)</Tag>
        </template>
        <template #actions="{ row }">
          <TableAction :actions="[{ label: '查看详情', type: 'link', icon: ACTION_ICON.VIEW, onClick: () => handleViewDetail(row) }]" />
        </template>
      </Grid>
    </div>

    <WorkbenchMode v-if="viewMode === 'WORKBENCH'" @back-to-ledger="viewMode = 'LEDGER'; gridApi.query()" />
  </Page>
</template>
