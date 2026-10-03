<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesFgShippingAlignmentApi } from '#/api/mes/quality/fg-shipping-alignment';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getFgShippingAlignmentNoticePage } from '#/api/mes/quality/fg-shipping-alignment';

import { useGridColumns, useGridFormSchema } from './data';
import AlignmentOperationModal from './modules/alignment-operation-modal.vue';

defineOptions({ name: 'MesQualityFgShippingAlignment' });

const [AlignmentOperationModalComp, alignmentOperationModalApi] = useVbenModal({
  connectedComponent: AlignmentOperationModal,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          getFgShippingAlignmentNoticePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<MesFgShippingAlignmentApi.Notice>,
});

function resolveNoticeStatus(record: MesFgShippingAlignmentApi.Notice) {
  if (record.plannedCount > 0 && record.alignedCount >= record.plannedCount) {
    return { color: 'success', label: '已完成，可包装' };
  }
  if (record.candidateCount <= 0) {
    return { color: 'default', label: '待检验合格片' };
  }
  return { color: 'warning', label: '对齐中' };
}

function openAlignment(record: MesFgShippingAlignmentApi.Notice) {
  alignmentOperationModalApi
    .setData({
      shippingNoticeId: record.shippingNoticeId,
      onSuccess: () => gridApi.query(),
    })
    .open();
}
</script>

<template>
  <Page auto-content-height>
    <AlignmentOperationModalComp />

    <Grid table-title="发货客户批号对齐">
      <template #toolbar-tools>
        <div
          class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
        >
          <Button @click="gridApi.query()">
            <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
          </Button>
        </div>
      </template>
      <template #shippingNoticeNo="{ row }">
        <span class="font-mono font-bold text-indigo-700">
          {{ row.shippingNoticeNo }}
        </span>
      </template>
      <template #alignmentProgress="{ row }">
        <Tag
          :color="
            row.plannedCount > 0 && row.alignedCount >= row.plannedCount
              ? 'success'
              : 'warning'
          "
          class="!m-0"
        >
          已对齐 {{ row.alignedCount }}/{{ row.plannedCount }}
        </Tag>
        <div class="mt-1 text-xs text-slate-400">
          已审核合格 {{ row.candidateCount }} 片
        </div>
      </template>
      <template #alignmentStatus="{ row }">
        <Tag :color="resolveNoticeStatus(row).color" class="!m-0">
          {{ resolveNoticeStatus(row).label }}
        </Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '进入对齐',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:fg-shipping-alignment:save'],
              onClick: () => openAlignment(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
