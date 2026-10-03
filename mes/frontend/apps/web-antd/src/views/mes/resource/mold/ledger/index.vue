<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty, downloadFileFromBlobPart } from '@vben/utils';
import { message, Tag, Progress } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteMoldList, exportMold, getMoldPage } from '#/api/mes/resource/mold/ledger';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesMoldLedger' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<any[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: { ajax: { query: async ({ page }, formValues) => await getMoldPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) } },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<any>,
  gridEvents: { checkboxAll: ({ records }) => checkedIds.value = records.map(r => r.id), checkboxChange: ({ records }) => checkedIds.value = records.map(r => r.id) },
});

function handleCreate() { formModalApi.setData({ type: 'create' }).open(); }
function handleEdit(row: any) { formModalApi.setData({ type: 'edit', id: row.id }).open(); }
function handleDetail(row: any) { formModalApi.setData({ type: 'detail', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('确认要删除选中的模具台账吗？');
  await deleteMoldList(checkedIds.value);
  checkedIds.value = [];
  message.success('删除成功');
  gridApi.query();
}

async function handleExport() {
  const data = await exportMold(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '模具台账.xls', source: data });
}

// 进度条颜色计算
function getProgressColor(percent: number, warningRatio: number) {
  if (percent >= 100) return '#ff4d4f'; // 爆表(红)
  if (percent >= warningRatio) return '#faad14'; // 预警(橙)
  return '#52c41a'; // 健康(绿)
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="gridApi.query()" />
    <div class="h-full flex flex-col bg-white">
      <div class="flex-1 min-h-0 relative">
        <BaseGrid table-title="工装模具管理台账">
          <template #toolbar-tools>
            <TableAction :actions="[
              { label: '新模建档', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleCreate },
              { label: '导出台账', type: 'primary', icon: ACTION_ICON.DOWNLOAD, onClick: handleExport },
              { label: '批量删除', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch }
            ]" />
          </template>

          <template #lifeProgress="{ row }">
            <div class="flex items-center w-full px-2" title="当前模次 / 设计总寿命">
              <Progress
                :percent="Math.round((row.currentLife / row.designLife) * 100)"
                :stroke-color="getProgressColor(Math.round((row.currentLife / row.designLife) * 100), row.warningRatio)"
                :format="p => `${p}%`"
                size="small"
                class="m-0"
              />
            </div>
          </template>

          <template #status="{ row }">
            <Tag v-if="row.status === 10" color="default" class="!m-0 font-bold border-none">在库闲置</Tag>
            <Tag v-else-if="row.status === 20" color="processing" class="!m-0 font-bold border-none">在机生产</Tag>
            <Tag v-else-if="row.status === 30" color="warning" class="!m-0 font-bold border-none">维修保养</Tag>
            <Tag v-else-if="row.status === 40" color="error" class="!m-0 font-bold border-none">报废注销</Tag>
          </template>

          <template #actions="{ row }">
            <TableAction :actions="[
              { label: '档案', type: 'link', icon: ACTION_ICON.VIEW, onClick: handleDetail.bind(null, row) },
              { label: '编辑', type: 'link', icon: ACTION_ICON.EDIT, onClick: handleEdit.bind(null, row) }
            ]" />
          </template>
        </BaseGrid>
      </div>
    </div>
  </Page>
</template>
