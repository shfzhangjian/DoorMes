<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcEquipmentConsumableApi } from '#/api/mes/hc/equipment-consumable';
import type { MesHcGuideClothRecordApi } from '#/api/mes/hc/guideclothrecord';

import { h, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import { message, Modal as AModal } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getEquipmentConsumableEventPage,
} from '#/api/mes/hc/equipment-consumable';
import {
  deleteGuideClothRecord,
  deleteGuideClothRecordList,
  exportGuideClothRecord,
  getGuideClothRecordPage,
  importGuideClothRecord,
} from '#/api/mes/hc/guideclothrecord';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import RndConsumeForm from './modules/rnd-consume-form.vue';
import {
  useEventGridColumns,
} from '../base/equipment-consumable/data';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [RndConsumeModal, rndConsumeModalApi] = useVbenModal({
  connectedComponent: RndConsumeForm,
  destroyOnClose: true,
});

const checkedIds = ref<number[]>([]);
const exporting = ref(false);
const importing = ref(false);
const importInputRef = ref<HTMLInputElement>();
const eventModalVisible = ref(false);
const eventModalTitle = ref('湿法导布与PET流水');
const eventGuideClothRecordId = ref<number>();

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcGuideClothRecordApi.GuideClothRecord) {
  formModalApi.setData(row).open();
}

function handleRndConsume(row: MesHcGuideClothRecordApi.GuideClothRecord) {
  if (Number(row.currentFlag) !== 0) {
    message.warning('仅当前导布记录可以登记研发消耗');
    return;
  }
  rndConsumeModalApi.setData(row).open();
}

async function handleViewEvents(row: MesHcGuideClothRecordApi.GuideClothRecord) {
  eventModalTitle.value = `${row.lineName || row.lineCode || ''} 湿法导布与PET流水`;
  eventGuideClothRecordId.value = row.id;
  eventModalVisible.value = true;
  await eventGridApi.formApi.setValues({
    guideClothRecordId: row.id,
  });
  eventGridApi.query();
}

async function handleDelete(row: MesHcGuideClothRecordApi.GuideClothRecord) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteGuideClothRecord(row.id!);
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDeleteBatch() {
  await confirm('确认删除选中的记录吗？');
  const hideLoading = message.loading({ content: '正在批量删除...', duration: 0 });
  try {
    await deleteGuideClothRecordList(checkedIds.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

function handleRowCheckboxChange({ records }: { records: MesHcGuideClothRecordApi.GuideClothRecord[] }) {
  checkedIds.value = records.map((item) => item.id!).filter(Boolean);
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '操作失败，请稍后重试';
}

async function handleExport() {
  const formValues = await gridApi.formApi.getValues();
  exporting.value = true;
  try {
    const data = await exportGuideClothRecord(formValues);
    downloadFileFromBlobPart({ fileName: '导布更换记录.xlsx', source: data });
  } finally {
    exporting.value = false;
  }
}

function handleImportClick() {
  AModal.confirm({
    title: '导入初始化确认',
    content: '导入将先物理删除当前租户下的导布更换记录，再按 Excel 数据初始化。此操作不可撤销，请确认已导出备份。',
    okText: '确认清空并导入',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => importInputRef.value?.click(),
  });
}

function showImportFailures(resp: MesHcGuideClothRecordApi.ImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 20).join('\n');
  const moreText = failures.length > 20 ? `\n... 还有 ${failures.length - 20} 条未显示` : '';
  AModal.warning({
    title: '导布更换记录导入校验未通过',
    width: 760,
    content: h('div', { class: 'space-y-2' }, [
      h(
        'div',
        `读取 ${resp.totalRows || 0} 行，跳过 ${resp.skippedRows || 0} 行，失败 ${resp.failureCount || 0} 条。未清空也未写入任何数据。`,
      ),
      h(
        'pre',
        {
          style: 'white-space: pre-wrap; margin: 0; max-height: 320px; overflow: auto; font-size: 12px;',
        },
        text + moreText,
      ),
    ]),
  });
}

async function handleImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  const hideLoading = message.loading({ content: '正在清空并导入初始化...', duration: 0 });
  importing.value = true;
  try {
    const resp = await importGuideClothRecord(file, true);
    if ((resp.failureCount || 0) > 0) {
      showImportFailures(resp);
      return;
    }
    const extraMessage = (resp.messages || []).join('；');
    message.success(
      extraMessage || `导入成功：初始化 ${resp.successCount || 0} 条，清空记录 ${resp.clearedCount || 0} 条`,
    );
    checkedIds.value = [];
    handleRefresh();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    importing.value = false;
    hideLoading();
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getGuideClothRecordPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesHcGuideClothRecordApi.GuideClothRecord>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

const [EventGrid, eventGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        fieldName: 'guideClothRecordId',
        component: 'Input',
        dependencies: { show: false, triggerFields: [''] },
      },
    ],
  },
  gridOptions: {
    columns: useEventGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const guideClothRecordId = eventGuideClothRecordId.value;
          if (!guideClothRecordId) {
            return { list: [], total: 0 };
          }
          return await getEquipmentConsumableEventPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            // 导布更换记录的流水仅展示当前记录关联的湿法导布（PET快照随该流水展示）。
            guideClothRecordId,
            processCode: 'WET',
            consumableType: 'GUIDE_CLOTH',
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
    },
  } as VxeTableGridOptions<MesHcEquipmentConsumableApi.Event>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <RndConsumeModal @success="handleRefresh" />
    <input ref="importInputRef" accept=".xlsx,.xls" class="hidden" type="file" @change="handleImportChange" />
    <Grid table-title="导布更换记录">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:guide-cloth-record:create'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:guide-cloth-record:query'],
              disabled: exporting,
              onClick: handleExport,
            },
            {
              label: '导入初始化',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.UPLOAD,
              auth: ['mes:md:guide-cloth-record:create'],
              disabled: importing,
              onClick: handleImportClick,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:guide-cloth-record:delete'],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:guide-cloth-record:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '研发消耗登记',
              type: 'link',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:guide-cloth-record:update'],
              disabled: Number(row.currentFlag) !== 0,
              onClick: handleRndConsume.bind(null, row),
            },
            {
              label: '流水',
              type: 'link',
              auth: ['mes:md:guide-cloth-record:query'],
              onClick: handleViewEvents.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['mes:md:guide-cloth-record:delete'],
              popConfirm: {
                title: '确认删除当前记录吗？',
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>

    <AModal
      v-model:open="eventModalVisible"
      :footer="null"
      :title="eventModalTitle"
      class="guide-cloth-event-modal"
      destroy-on-close
      width="1180px"
    >
      <div class="guide-cloth-event-grid">
        <EventGrid table-title="湿法导布与PET流水" />
      </div>
    </AModal>
  </Page>
</template>

<style scoped>
.guide-cloth-event-grid {
  height: 620px;
}
</style>
