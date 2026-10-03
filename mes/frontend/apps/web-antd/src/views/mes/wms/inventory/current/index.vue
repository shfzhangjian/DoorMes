<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, confirm, useVbenModal } from '@vben/common-ui';
import { Tag, Button, message, Tooltip } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { IconifyIcon } from '@vben/icons';

import { useGridColumns, useGridFormSchema } from './data';
import LedgerModalForm from './modules/ledger-modal.vue'; // 💡 引入明细弹窗

// 💡 注册明细弹窗
const [LedgerModal, ledgerModalApi] = useVbenModal({
  connectedComponent: LedgerModalForm,
  destroyOnClose: true,
});

// 模拟数据库
const mockDatabase = ref([
  { id: '1', location: 'BIN-A01-01', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', batchNo: 'B260219-001', unit: 'KG', totalQty: 850, lockedQty: 50, availableQty: 800, lastUpdate: '2026-02-19 10:00:00' },
  { id: '2', location: 'BIN-A01-02', productCode: 'RM-SOLV-05', productName: '特种交联剂', batchNo: 'B260218-055', unit: 'L', totalQty: 150, lockedQty: 0, availableQty: 150, lastUpdate: '2026-02-19 10:30:22' },
  { id: '3', location: 'ZONE-WIP-01', productCode: 'RM-SOLV-05', productName: '特种交联剂', batchNo: 'B260218-055', unit: 'L', totalQty: 40, lockedQty: 0, availableQty: 40, lastUpdate: '2026-02-19 11:05:40' },
]);

for (let i = 1; i <= 35; i++) {
  mockDatabase.value.push({
    id: `100${i}`, location: `BIN-B01-${String(i).padStart(2, '0')}`, productCode: 'PKG-001', productName: '包装纸箱', batchNo: `B260215-PKG`, unit: '件',
    totalQty: 500, lockedQty: i % 5 === 0 ? 500 : 0, availableQty: i % 5 === 0 ? 0 : 500, lastUpdate: '2026-02-18 10:00:00'
  });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(), collapsed: false },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true, zoom: true, custom: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockDatabase.value;
          if (formValues?.productCode) filtered = filtered.filter(i => i.productCode.includes(formValues.productCode) || i.productName.includes(formValues.productCode));
          if (formValues?.location) filtered = filtered.filter(i => i.location.includes(formValues.location));
          if (formValues?.batchNo) filtered = filtered.filter(i => i.batchNo.includes(formValues.batchNo));
          if (formValues?.hasStock === 'Y') filtered = filtered.filter(i => i.totalQty > 0);

          const start = (page.currentPage - 1) * page.pageSize;
          const end = start + page.pageSize;
          return { list: filtered.slice(start, end), total: filtered.length };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleExport() {
  message.loading({ content: '正在生成现时库存快照...', key: 'export' });
  setTimeout(() => { message.success({ content: '导出成功！库存清单已开始下载。', key: 'export' }); }, 1000);
}

// 💡 真实拉起流水弹窗
function handleViewLedger(row: any) {
  ledgerModalApi.setData(row).open();
}

function handleQuickAdjust(row: any) {
  confirm({
    title: '⚠️ 快捷盘点调整',
    content: `确认要将库位 ${row.location} 的 ${row.productCode} 强行清零吗？此操作将生成红字冲销流水。`,
    okText: '确认强行清零', okType: 'danger',
    onOk: async () => {
      row.totalQty = 0; row.availableQty = 0; gridApi.query();
      message.success('已清零并生成盘亏记录。');
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <LedgerModal />

    <Grid table-title="全局现时库存看板">
      <template #toolbar-tools>
        <Button type="primary" class="bg-indigo-600 font-bold" @click="handleExport">
          <IconifyIcon icon="lucide:file-down" class="mr-1" /> 导出库存快照
        </Button>
      </template>

      <template #location="{ row }">
        <span class="font-mono text-orange-600 font-bold flex items-center gap-1">
          <IconifyIcon icon="lucide:map-pin" class="text-[10px] text-orange-400"/> {{ row.location }}
        </span>
      </template>
      <template #productCode="{ row }"><span class="font-mono text-slate-700 font-bold">{{ row.productCode }}</span></template>
      <template #batchNo="{ row }"><Tag color="cyan" class="font-mono !m-0 border-cyan-200">{{ row.batchNo }}</Tag></template>
      <template #totalQty="{ row }"><span class="font-mono text-slate-500 font-bold text-sm">{{ row.totalQty }}</span></template>

      <template #lockedQty="{ row }">
        <Tooltip v-if="row.lockedQty > 0" title="此库存已被分配或质检锁定！" color="red">
          <span class="font-mono text-red-500 font-bold underline decoration-dashed cursor-help">{{ row.lockedQty }}</span>
        </Tooltip>
        <span v-else class="font-mono text-slate-300">0</span>
      </template>

      <template #availableQty="{ row }">
        <span v-if="row.availableQty > 0" class="text-green-600 font-bold font-mono text-base">{{ row.availableQty }}</span>
        <Tag v-else color="red" class="!m-0 font-bold">缺料 (0)</Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '查流水', type: 'link', icon: ACTION_ICON.PREVIEW, onClick: handleViewLedger.bind(null, row) },
            { label: '清零', type: 'link', danger: true, onClick: handleQuickAdjust.bind(null, row) }
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
:deep(.vben-vxe-grid) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
