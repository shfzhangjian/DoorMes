<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page, useVbenModal, confirm } from '@vben/common-ui';
import { Tag, message } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { IconifyIcon } from '@vben/icons';

import { useGridColumns, useGridFormSchema } from './data';
import RuleForm from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: RuleForm, destroyOnClose: true });

// 模拟数据库数据
const mockData = ref([
  { id: 1, customerCode: 'CATL', customerName: '宁德时代新能源科技股份有限公司', ruleName: 'CATL标准箱码规则', prefix: 'CATL-V001-', includeDate: true, dateFmt: 'yyyyMMdd', includeInternalBatch: false, seqLen: 4, barcodeType: 'QR_CODE', printTemplate: 'TPL-CATL-100x100', status: 0 },
  { id: 2, customerCode: 'BYD', customerName: '比亚迪汽车工业有限公司', ruleName: 'BYD电芯膜规则', prefix: 'BYD-M-', includeDate: true, dateFmt: 'yyMMdd', includeInternalBatch: true, seqLen: 3, barcodeType: 'CODE_128', printTemplate: 'TPL-BYD-80x60', status: 0 },
]);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(), collapsed: false },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto', // 撑满剩余高度，分页沉底
    keepSource: true,
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          let filtered = mockData.value;
          if (formValues?.customerName) filtered = filtered.filter(i => i.customerName.includes(formValues.customerName) || i.customerCode.includes(formValues.customerName));
          if (formValues?.ruleName) filtered = filtered.filter(i => i.ruleName.includes(formValues.ruleName));
          if (formValues?.status !== undefined) filtered = filtered.filter(i => i.status === formValues.status);

          return { list: filtered, total: filtered.length };
        }
      }
    }
  } as VxeTableGridOptions<any>,
});

function handleCreate() { formModalApi.open(); }
function handleEdit(row: any) { formModalApi.setData(row).open(); }
function handleDelete(row: any) {
  confirm({
    title: '确认删除该客户规则？',
    content: '删除后发货控制台将无法使用此规则进行标签置换。',
    onOk: () => { message.success('删除成功'); gridApi.query(); }
  });
}

// 模拟生成外箱码预览
function generatePreview(row: any) {
  let res = row.prefix || '';
  if (row.includeDate) {
    res += row.dateFmt === 'yyyyMMdd' ? '20260219' : '260219';
  }
  if (row.includeInternalBatch) res += '-INT260219';
  res += '-' + '1'.padStart(row.seqLen || 4, '0');
  return res;
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="gridApi.query()" />

    <Grid table-title="客户专属标签置换规则管理">
      <template #toolbar-tools>
        <TableAction :actions="[{ label: '新增客户规则', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate }]" />
      </template>

      <template #customer="{ row }">
        <div class="flex flex-col">
          <span class="font-bold text-slate-700 text-sm truncate">{{ row.customerName }}</span>
          <span class="font-mono text-slate-400 text-[10px]">{{ row.customerCode }}</span>
        </div>
      </template>

      <template #ruleName="{ row }">
        <span class="font-bold text-indigo-700">{{ row.ruleName }}</span>
      </template>

      <template #barcodeType="{ row }">
        <Tag :color="row.barcodeType === 'QR_CODE' ? 'purple' : 'blue'" class="!m-0 font-bold border-transparent">
          <IconifyIcon :icon="row.barcodeType === 'QR_CODE' ? 'lucide:qr-code' : 'lucide:barcode'" class="mr-1" />
          {{ row.barcodeType === 'QR_CODE' ? '二维码' : '一维码' }}
        </Tag>
      </template>

      <template #preview="{ row }">
        <div class="font-mono bg-indigo-50 text-indigo-800 px-2 py-1 rounded border border-indigo-100 shadow-sm text-xs inline-flex items-center gap-1">
          <IconifyIcon icon="lucide:tag" class="text-indigo-400" /> {{ generatePreview(row) }}
        </div>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 0 ? 'success' : 'error'">{{ row.status === 0 ? '启用' : '禁用' }}</Tag>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) },
            { label: '删除', type: 'link', danger: true, icon: ACTION_ICON.DELETE, onClick: handleDelete.bind(null, row) }
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
