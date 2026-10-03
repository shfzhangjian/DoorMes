<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal } from '@vben/common-ui';
import { Tag } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';

import { useGridColumns, useGridFormSchema } from './data';
import ExecuteModalForm from './modules/execute-modal.vue';

// 注册执行终端弹窗
const [ExecuteModal, executeModalApi] = useVbenModal({ connectedComponent: ExecuteModalForm, destroyOnClose: true });

// 💡 模拟底层映射数据库 (完美伏笔：对应我们在 [8533] 追溯中看到的数据)
const mockData = ref([
  { id: '1', clientName: '宁德时代 (CATL)', ruleName: 'CATL标准箱码规则', internalSn: 'INT-260219-001', clientSn: 'CATL-V001-260219-0001', productName: '高透光学复合膜', operator: '系统自动', createTime: '2026-02-19 15:30:00', printStatus: 'PRINTED' },
  { id: '2', clientName: '宁德时代 (CATL)', ruleName: 'CATL标准箱码规则', internalSn: 'INT-260219-002', clientSn: 'CATL-V001-260219-0002', productName: '高透光学复合膜', operator: '系统自动', createTime: '2026-02-19 15:30:05', printStatus: 'PRINTED' },
  { id: '3', clientName: '比亚迪 (BYD)', ruleName: 'BYD电芯膜规则', internalSn: 'INT-260218-088', clientSn: 'BYD-M-260218-0055', productName: '特种交联剂', operator: '王发货', createTime: '2026-02-18 09:10:00', printStatus: 'REPRINT' },
]);

for (let i = 1; i <= 20; i++) {
  mockData.value.push({
    id: `10${i}`, clientName: '通用客户', ruleName: '默认客户发货规则', internalSn: `INT-OLD-${i}`, clientSn: `CUST-SN-${i}`,
    productName: '常规辅料', operator: '张三', createTime: '2026-02-10 10:00:00', printStatus: 'PRINTED'
  });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockData.value;
          if (formValues?.clientSn) filtered = filtered.filter(i => i.clientSn.includes(formValues.clientSn));
          if (formValues?.internalSn) filtered = filtered.filter(i => i.internalSn.includes(formValues.internalSn));
          if (formValues?.clientName) filtered = filtered.filter(i => i.clientName.includes(formValues.clientName));

          const start = (page.currentPage - 1) * page.pageSize;
          const end = start + page.pageSize;
          return { list: filtered.slice(start, end), total: filtered.length };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleExecute() { executeModalApi.open(); }

// 模拟置换成功后的回调刷新
function handleExecuteSuccess() {
  mockData.value.unshift({
    id: Date.now().toString(),
    clientName: '宁德时代 (CATL)',
    ruleName: '现场手工置换',
    internalSn: 'INT-NEW-SCAN',
    clientSn: 'CATL-V001-NEW-0001',
    productName: '现场扫描物料',
    operator: '当前用户',
    createTime: new Date().toLocaleString(),
    printStatus: 'PRINTED'
  });
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <ExecuteModal @success="handleExecuteSuccess" />

    <Grid table-title="客户专属标签置换与映射历史">
      <template #toolbar-tools>
        <TableAction :actions="[{ label: '执行标签置换作业', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleExecute }]" />
      </template>

      <template #clientName="{ row }">
        <span class="font-bold text-slate-800">{{ row.clientName }}</span>
      </template>

      <template #internalSn="{ row }">
        <span class="font-mono text-slate-500 bg-slate-100 px-2 py-0.5 rounded text-xs border border-slate-200">{{ row.internalSn }}</span>
      </template>

      <template #clientSn="{ row }">
        <span class="font-mono font-bold text-indigo-700 bg-indigo-50 px-2 py-1 rounded shadow-sm border border-indigo-100">{{ row.clientSn }}</span>
      </template>

      <template #printStatus="{ row }">
        <Tag :color="row.printStatus === 'PRINTED' ? 'green' : 'orange'" class="!m-0 font-bold border-transparent">
          {{ row.printStatus === 'PRINTED' ? '已打印贴签' : '补打重签' }}
        </Tag>
      </template>
    </Grid>
  </Page>
</template>
