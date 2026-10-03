<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesCostReportApi } from '#/api/mes/cost/report/analysis';

import { onMounted } from 'vue';
import { Page } from '@vben/common-ui';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getCostAnalysisTree } from '#/api/mes/cost/report/analysis';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MesCostAnalysisReport' });

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    baseColProps: { span: 6 },
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    treeConfig: {
      transform: false,
      childrenField: 'children',
      expandAll: true,
      trigger: 'cell',
    },
    showFooter: true,
    footerMethod: ({ columns, data }) => {
      return [
        columns.map((column, columnIndex) => {
          if (columnIndex === 0) return '当期汇总合计';

          if (['materialCost', 'overheadCost', 'totalCost'].includes(column.field)) {
            const rootNodes = data.filter(row => row.id && row.id.startsWith('C-'));
            const sum = rootNodes.reduce((prev, curr) => prev + (Number(curr[column.field]) || 0), 0);
            return `¥ ${sum.toFixed(2)}`;
          }
          return '-';
        })
      ];
    },
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          return await getCostAnalysisTree(formValues);
        }
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesCostReportApi.CostNode>,
});

onMounted(() => {
  const currentMonth = new Date().toISOString().slice(0, 7);
  gridApi.formApi.setValues({ period: currentMonth });
});
</script>

<template>
  <Page auto-content-height>
    <div class="h-full flex flex-col bg-background rounded-md shadow-sm border border-border overflow-hidden">
      <BaseGrid table-title="多维成本交叉分析报表 (中心 -> 工单 -> 要素)">
        <template #toolbar-tools>
          <TableAction
            :actions="[
              { label: '展开全部', type: 'default', icon: 'ep:expand', onClick: () => gridApi.grid?.setAllTreeExpand(true) },
              { label: '折叠全部', type: 'default', icon: 'ep:fold', onClick: () => gridApi.grid?.clearTreeExpand() },
              { label: '导出报表', type: 'primary', icon: ACTION_ICON.DOWNLOAD, auth: ['mes:cost-report:export'] },
            ]"
          />
        </template>

        <template #itemName="{ row }">
          <span
            :class="{
              'font-bold text-blue-800 text-[15px]': row.id.startsWith('C-'),
              'font-semibold text-green-700': row.id.startsWith('WO-') && row.id.split('-').length === 2,
              'text-gray-500': row.id.startsWith('WO-') && row.id.split('-').length > 2
            }"
          >
            {{ row.itemName }}
          </span>
        </template>

        <template #materialCost="{ row }">
          <span v-if="row.materialCost !== undefined && row.materialCost !== null" class="text-gray-700">
            ¥ {{ Number(row.materialCost).toFixed(2) }}
          </span>
          <span v-else class="text-gray-300">-</span>
        </template>

        <template #overheadCost="{ row }">
          <span v-if="row.overheadCost !== undefined && row.overheadCost !== null" class="text-orange-600">
            ¥ {{ Number(row.overheadCost).toFixed(2) }}
          </span>
          <span v-else class="text-gray-300">-</span>
        </template>

        <template #totalCost="{ row }">
          <span v-if="row.totalCost !== undefined && row.totalCost !== null" class="text-blue-600 font-bold">
            ¥ {{ Number(row.totalCost).toFixed(2) }}
          </span>
          <span v-else class="text-gray-300">-</span>
        </template>

        <template #unitCost="{ row }">
          <span v-if="row.unitCost !== undefined && row.unitCost !== null" class="text-green-600 font-bold">
            ¥ {{ Number(row.unitCost).toFixed(2) }}
          </span>
          <span v-else class="text-gray-300">-</span>
        </template>

      </BaseGrid>
    </div>
  </Page>
</template>
