<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';
import { message, Tag } from 'ant-design-vue';
import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteMountRecordList, getMountRecordPage } from '#/api/mes/resource/mold/mount-record';

import { useGridColumns, useGridFormSchema } from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesMoldMountRecord' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: FormComponent, destroyOnClose: true });
const checkedIds = ref<any[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: { ajax: { query: async ({ page }, formValues) => await getMountRecordPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues }) } },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<any>,
  gridEvents: { checkboxAll: ({ records }) => checkedIds.value = records.map(r => r.id), checkboxChange: ({ records }) => checkedIds.value = records.map(r => r.id) },
});

// 上模动作
function handleMount() { formModalApi.setData({ action: 'mount' }).open(); }

// 下模动作 (仅针对在机状态的记录)
function handleTeardown(row: any) { formModalApi.setData({ action: 'teardown', id: row.id }).open(); }

async function handleDeleteBatch() {
  await confirm('危险操作：删除履历可能导致该模具的追溯链断裂，确认继续？');
  await deleteMountRecordList(checkedIds.value);
  checkedIds.value = [];
  message.success('履历已清除');
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="gridApi.query()" />
    <div class="h-full flex flex-col bg-white">
      <div class="flex-1 min-h-0 relative">
        <BaseGrid table-title="工装上下模流转控制台">
          <template #toolbar-tools>
            <TableAction :actions="[
              { label: '执行上模装机', type: 'primary', icon: ACTION_ICON.ADD, onClick: handleMount },
              { label: '删除履历', type: 'primary', danger: true, icon: ACTION_ICON.DELETE, disabled: isEmpty(checkedIds), onClick: handleDeleteBatch }
            ]" />
          </template>

          <template #recordStatus="{ row }">
            <Tag v-if="row.recordStatus === 'MOUNTED'" color="processing" class="!m-0 font-bold border-none animate-pulse">在机运转中</Tag>
            <Tag v-else-if="row.recordStatus === 'TEARDOWN'" color="default" class="!m-0 font-bold border-none">已剥离下线</Tag>
          </template>

          <template #actions="{ row }">
            <TableAction :actions="[
              { label: '执行下模 (完工/异常)', type: 'link', icon: ACTION_ICON.EDIT, ifShow: () => row.recordStatus === 'MOUNTED', onClick: handleTeardown.bind(null, row) },
            ]" />
          </template>
        </BaseGrid>
      </div>
    </div>
  </Page>
</template>
