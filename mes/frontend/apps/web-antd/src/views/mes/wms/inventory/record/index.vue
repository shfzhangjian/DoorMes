<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { Tag, Button, message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { IconifyIcon } from '@vben/icons';

import { useGridColumns, useGridFormSchema } from './data';

// ==============================================================================
// 1. 模拟底层台账数据库 (串联我们之前开发的所有业务场景数据)
// ==============================================================================
const mockDatabase = ref([
  { id: '101', trxNo: 'TRX-260219-0001', trxType: 'PO_RECEIPT', typeLabel: '采购入库', refOrderNo: 'ASN-20260219', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', location: 'BIN-A01-01', qty: 1000, balanceQty: 1000, operator: '赵收货', trxTime: '2026-02-19 08:30:12' },
  { id: '102', trxNo: 'TRX-260219-0002', trxType: 'WO_ISSUE', typeLabel: '生产领料', refOrderNo: 'WO20260219-001', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', location: 'BIN-A01-01', qty: -200, balanceQty: 800, operator: '孙发料', trxTime: '2026-02-19 09:15:00' },
  { id: '103', trxNo: 'TRX-260219-0003', trxType: 'CYCLE_COUNT', typeLabel: '盘点调整', refOrderNo: 'CC-20260219-001', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', location: 'BIN-A01-01', qty: 50, balanceQty: 850, operator: '周盘点', trxTime: '2026-02-19 10:00:00' }, // 盘盈
  { id: '104', trxNo: 'TRX-260219-0004', trxType: 'TRANSFER', typeLabel: '调拨移出', refOrderNo: 'TRX-MV-260219', productCode: 'RM-SOLV-05', productName: '特种交联剂', location: 'BIN-A01-02', qty: -50, balanceQty: 150, operator: '钱调拨', trxTime: '2026-02-19 10:30:22' },
  { id: '105', trxNo: 'TRX-260219-0005', trxType: 'TRANSFER', typeLabel: '调拨移入', refOrderNo: 'TRX-MV-260219', productCode: 'RM-SOLV-05', productName: '特种交联剂', location: 'ZONE-WIP-01', qty: 50, balanceQty: 50, operator: '钱调拨', trxTime: '2026-02-19 10:30:22' },
  { id: '106', trxNo: 'TRX-260219-0006', trxType: 'MISC_ISSUE', typeLabel: '杂项发料', refOrderNo: 'MISC-260219', productCode: 'RM-SOLV-05', productName: '特种交联剂', location: 'ZONE-WIP-01', qty: -10, balanceQty: 40, operator: '李杂项', trxTime: '2026-02-19 11:05:40' },
]);

// 循环造点假数据以测试分页
for (let i = 1; i <= 30; i++) {
  mockDatabase.value.push({
    id: `200${i}`, trxNo: `TRX-HISTORY-${String(i).padStart(4, '0')}`, trxType: 'PO_RECEIPT', typeLabel: '采购入库',
    refOrderNo: `OLD-PO-${i}`, productCode: 'PKG-001', productName: '包装纸箱', location: 'BIN-B01',
    qty: 500, balanceQty: 5000 + (i * 500), operator: '系统导入', trxTime: '2026-02-18 10:00:00'
  });
}

// ==============================================================================
// 2. Vben VxeGrid 核心配置
// ==============================================================================
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: false, // 默认展开搜索项
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 自动撑满剩余高度，分页沉底
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 15 },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true, zoom: true, custom: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockDatabase.value;

          // 模拟条件过滤
          if (formValues?.productCode) {
            filtered = filtered.filter(i => i.productCode.includes(formValues.productCode) || i.productName.includes(formValues.productCode));
          }
          if (formValues?.trxType) {
            filtered = filtered.filter(i => i.trxType === formValues.trxType);
          }
          if (formValues?.refOrderNo) {
            filtered = filtered.filter(i => i.refOrderNo.includes(formValues.refOrderNo));
          }
          if (formValues?.location) {
            filtered = filtered.filter(i => i.location.includes(formValues.location));
          }

          const start = (page.currentPage - 1) * page.pageSize;
          const end = start + page.pageSize;

          return {
            list: filtered.slice(start, end),
            total: filtered.length
          };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

// 模拟导出功能
function handleExport() {
  message.loading({ content: '正在生成台账报表...', key: 'export' });
  setTimeout(() => {
    message.success({ content: '导出成功！文件已开始下载。', key: 'export' });
  }, 1000);
}

// 获取类型标签颜色
function getTypeColor(type: string) {
  if (type.includes('RECEIPT')) return 'blue';
  if (type.includes('ISSUE')) return 'orange';
  if (type === 'TRANSFER') return 'cyan';
  if (type === 'CYCLE_COUNT') return 'purple';
  return 'default';
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="库存明细流水台账 (Transaction Ledger)">

      <template #toolbar-tools>
        <Button type="primary" ghost @click="handleExport">
          <IconifyIcon icon="lucide:download" class="mr-1" /> 导出 Excel 台账
        </Button>
      </template>

      <template #trxNo="{ row }">
        <span class="font-mono text-slate-500 text-xs">{{ row.trxNo }}</span>
      </template>

      <template #trxType="{ row }">
        <Tag :color="getTypeColor(row.trxType)" class="font-bold border-transparent">
          {{ row.typeLabel }}
        </Tag>
      </template>

      <template #productCode="{ row }">
        <span class="font-mono text-indigo-700 font-bold">{{ row.productCode }}</span>
      </template>

      <template #location="{ row }">
        <span class="font-mono text-orange-600 font-bold">{{ row.location }}</span>
      </template>

      <template #qty="{ row }">
        <span v-if="row.qty > 0" class="text-green-600 font-bold font-mono text-base">+{{ row.qty }}</span>
        <span v-else-if="row.qty < 0" class="text-red-600 font-bold font-mono text-base">{{ row.qty }}</span>
        <span v-else class="text-slate-400 font-mono">{{ row.qty }}</span>
      </template>

      <template #balanceQty="{ row }">
        <span class="font-mono font-black text-slate-800">{{ row.balanceQty }}</span>
      </template>

    </Grid>
  </Page>
</template>

<style scoped>
/* 确保表格高度填充和底部分页沉底 */
:deep(.vben-vxe-grid) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
