<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { Tag, Descriptions } from 'ant-design-vue';
import { useLedgerColumns } from '../data';

// 记录当前弹窗上下文（即点击的那条库存记录）
const currentInv = ref<any>({});
// 存放完整模拟流水的底层数组
const fullLedgerList = ref<any[]>([]);

// ==============================================================================
// 1. Vben VxeGrid 核心配置 (带时间范围搜索和底部分页)
// ==============================================================================
const [Grid, gridApi] = useVbenVxeGrid({
  // 💡 弹窗内部自带的搜索表单
  formOptions: {
    schema: [
      {
        fieldName: 'timeRange',
        label: '发生时间范围',
        component: 'RangePicker',
        componentProps: { valueFormat: 'YYYY-MM-DD', allowClear: true },
      }
    ],
    showCollapseButton: false, // 只有一行，不需要折叠
    submitButtonOptions: { content: '检索流水' }
  },
  gridOptions: {
    columns: useLedgerColumns(),
    height: 'auto', // 自动撑满剩余高度，让分页组件沉底
    pagerConfig: { enabled: true, pageSize: 15 },
    rowConfig: { keyField: 'id', isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = fullLedgerList.value;

          // 💡 实现时间范围过滤逻辑
          if (formValues?.timeRange && formValues.timeRange.length === 2) {
            const startStr = formValues.timeRange[0] + ' 00:00:00';
            const endStr = formValues.timeRange[1] + ' 23:59:59';
            filtered = filtered.filter(item => item.trxTime >= startStr && item.trxTime <= endStr);
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

// ==============================================================================
// 2. 弹窗控制器
// ==============================================================================
const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true, // 💡 支持一键全屏
  title: '📜 库存出入库履历追溯 (Transaction Ledger)',
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      currentInv.value = data;

      // 💡 根据当前库存的物料、库位、当前总数，生成高度逼真的履历数据
      fullLedgerList.value = generateMockLedgers(data);

      // 触发内部 Grid 刷新
      gridApi.query();
    }
  },
});

// ==============================================================================
// 3. UI 渲染与数据模拟方法
// ==============================================================================
function getTypeColor(type: string) {
  if (type.includes('RECEIPT')) return 'blue';
  if (type.includes('ISSUE')) return 'orange';
  if (type.includes('COUNT')) return 'purple';
  if (type.includes('TRANSFER')) return 'cyan';
  return 'default';
}

/**
 * 💡 高级模拟算法：逆向推演生成历史流水
 * 保证最新的 `balanceQty` 严格等于外层界面的 `totalQty`
 */
function generateMockLedgers(invData: any) {
  const ledgers = [];
  let currentBalance = invData.totalQty; // 从现在的数量开始倒推
  let currentDate = new Date('2026-02-19T14:00:00'); // 模拟当前时间

  // 制造 45 条假数据撑开分页
  for (let i = 1; i <= 45; i++) {
    // 随机决定是出库还是入库 (稍微偏向于出库，保证库存是从大变小)
    const isIssue = Math.random() > 0.4;
    let qtyChange = Math.floor(Math.random() * 50) + 1; // 1~50 的变动量

    if (isIssue) qtyChange = -qtyChange;

    // 防止倒推出现负数库存的边界处理
    if (currentBalance - qtyChange < 0) qtyChange = currentBalance;

    let trxType = '';
    let typeLabel = '';

    if (qtyChange > 0) {
      trxType = Math.random() > 0.2 ? 'PO_RECEIPT' : 'MISC_RECEIPT';
      typeLabel = trxType === 'PO_RECEIPT' ? '采购入库' : '杂项入账';
    } else {
      trxType = Math.random() > 0.2 ? 'WO_ISSUE' : 'TRANSFER';
      typeLabel = trxType === 'WO_ISSUE' ? '生产领料' : '库内调出';
    }

    // 格式化时间 (每次倒推 1~5 小时)
    currentDate = new Date(currentDate.getTime() - (Math.floor(Math.random() * 5) + 1) * 3600000);
    const timeStr = currentDate.toISOString().replace('T', ' ').substring(0, 19);

    ledgers.push({
      id: `L_${i}`,
      trxTime: timeStr,
      trxType,
      typeLabel,
      trxNo: `TRX-${timeStr.substring(2,10).replace(/-/g,'')}-${String(i).padStart(4, '0')}`,
      refOrderNo: `SRC-DOC-${String(i).padStart(3, '0')}`,
      qty: qtyChange,
      balanceQty: currentBalance, // 当前这笔发生后的结存数
      operator: ['张仓管', '李财务', '赵发料', '王入库'][Math.floor(Math.random() * 4)]
    });

    // 💡 倒推上一笔流水发生后的余额
    currentBalance = currentBalance - qtyChange;
  }

  // 因为是最新的记录排在最前面，所以数组天然就是按时间倒序的
  return ledgers;
}
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden">

      <Descriptions size="small" :column="4" class="bg-white p-4 rounded-lg shadow-sm border border-slate-200 shrink-0 mb-4">
        <Descriptions.Item label="物料编码"><span class="font-bold text-indigo-700 font-mono text-sm">{{ currentInv.productCode }}</span></Descriptions.Item>
        <Descriptions.Item label="物料名称"><span class="font-bold text-slate-700">{{ currentInv.productName }}</span></Descriptions.Item>
        <Descriptions.Item label="所属库位"><span class="font-bold text-orange-600 font-mono flex items-center gap-1"><IconifyIcon icon="lucide:map-pin" class="text-[12px]"/>{{ currentInv.location }}</span></Descriptions.Item>
        <Descriptions.Item label="当前总结存"><span class="font-black text-slate-800 text-lg">{{ currentInv.totalQty }} <span class="text-xs font-normal text-slate-500">{{ currentInv.unit }}</span></span></Descriptions.Item>
      </Descriptions>

      <div class="flex-1 min-h-0 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col vben-card-table">
        <Grid class="absolute inset-0 flex flex-col">

          <template #trxType="{ row }">
            <Tag :color="getTypeColor(row.trxType)" class="font-bold border-transparent !m-0">{{ row.typeLabel }}</Tag>
          </template>

          <template #trxNo="{ row }">
            <span class="font-mono text-slate-500 text-xs">{{ row.trxNo }}</span>
          </template>

          <template #qty="{ row }">
            <span v-if="row.qty > 0" class="text-green-600 font-bold font-mono text-sm">+{{ row.qty }}</span>
            <span v-else-if="row.qty < 0" class="text-red-600 font-bold font-mono text-sm">{{ row.qty }}</span>
            <span v-else class="text-slate-400 font-mono">{{ row.qty }}</span>
          </template>

          <template #balanceQty="{ row }">
            <span class="font-mono font-black text-slate-700 text-sm">{{ row.balanceQty }}</span>
          </template>

        </Grid>
      </div>

    </div>
  </Modal>
</template>

<style scoped>
/* 强制表格容器撑满，让分页组件绝对沉底 */
.vben-card-table :deep(.ant-table-wrapper),
.vben-card-table :deep(.ant-spin-nested-loading),
.vben-card-table :deep(.ant-spin-container) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.vben-card-table :deep(.ant-table) {
  flex: 1;
  overflow: hidden;
}
.vben-card-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 13px; font-weight: bold; }
.vben-card-table :deep(.ant-table-cell) { font-size: 13px; padding: 8px 12px !important; border-bottom: 1px solid #f1f5f9; }

/* 🌟 分页组件沉底约束 */
.vben-card-table :deep(.ant-pagination) {
  margin: 0 !important;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}
</style>
