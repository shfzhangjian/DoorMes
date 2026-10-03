<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcPressSlotSpareApi } from '#/api/mes/hc/press-slot-spare';

import { h, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';
import { message, Modal as AModal } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportPressSlotSpare,
  getPressSlotSparePage,
  getPressSlotSpareRecordPage,
  importPressSlotSpare,
} from '#/api/mes/hc/press-slot-spare';

import {
  useRecordGridColumns,
  useRecordGridFormSchema,
  useStateGridColumns,
  useStateGridFormSchema,
} from './data';
import Form from './modules/form.vue';

const recordModalVisible = ref(false);
const recordModalTitle = ref('压槽备件流水');
const recordFixedParams = ref<Record<string, any>>({});
const stateExporting = ref(false);
const stateImporting = ref(false);
const stateImportInputRef = ref<HTMLInputElement>();

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  stateGridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: MesHcPressSlotSpareApi.Spare) {
  formModalApi.setData(row).open();
}

async function handleViewRecords(row: MesHcPressSlotSpareApi.Spare) {
  const typeText = row.spareType === 'BEARING' ? '轴承' : row.spareType === 'PRESS_ROLLER' ? '压槽辊' : '压槽备件';
  recordModalTitle.value = `${row.equipmentCode || ''} ${row.batchNo || ''} ${typeText}流水`;
  recordFixedParams.value = {
    equipmentId: row.equipmentId,
    spareId: row.id,
    spareType: row.spareType,
  };
  recordModalVisible.value = true;
  await recordGridApi.formApi.setValues({ spareType: row.spareType });
  recordGridApi.query();
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

async function handleExportState() {
  const formValues = await stateGridApi.formApi.getValues();
  stateExporting.value = true;
  try {
    const data = await exportPressSlotSpare(formValues);
    downloadFileFromBlobPart({ fileName: '压槽辊轴承当前状态.xlsx', source: data });
  } finally {
    stateExporting.value = false;
  }
}

function handleImportStateClick() {
  AModal.confirm({
    title: '导入初始化确认',
    content: '导入将先物理删除当前租户下的压槽辊/轴承当前状态和使用/更换/清洗流水，再按 Excel 数据初始化。此操作不可撤销，请确认已导出备份。',
    okText: '确认清空并导入',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => stateImportInputRef.value?.click(),
  });
}

function showImportFailures(resp: MesHcPressSlotSpareApi.ImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 20).join('\n');
  const moreText = failures.length > 20 ? `\n... 还有 ${failures.length - 20} 条未显示` : '';
  AModal.warning({
    title: '压槽辊/轴承当前状态导入校验未通过',
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

async function handleImportStateChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  const hideLoading = message.loading({ content: '正在清空并导入初始化...', duration: 0 });
  stateImporting.value = true;
  try {
    const resp = await importPressSlotSpare(file, true);
    if ((resp.failureCount || 0) > 0) {
      showImportFailures(resp);
      return;
    }
    const extraMessage = (resp.messages || []).join('；');
    message.success(
      extraMessage ||
        `导入成功：初始化 ${resp.successCount || 0} 条，清空状态 ${resp.clearedStateCount || 0} 条，清空流水 ${
          resp.clearedRecordCount || 0
        } 条`,
    );
    handleRefresh();
    if (recordModalVisible.value) {
      recordGridApi.query();
    }
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    stateImporting.value = false;
    hideLoading();
  }
}

const [StateGrid, stateGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useStateGridFormSchema(),
  },
  gridOptions: {
    columns: useStateGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getPressSlotSparePage({
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
  } as VxeTableGridOptions<MesHcPressSlotSpareApi.Spare>,
});

const [RecordGrid, recordGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      { fieldName: 'spareId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
      ...useRecordGridFormSchema(),
    ],
  },
  gridOptions: {
    columns: useRecordGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getPressSlotSpareRecordPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            ...recordFixedParams.value,
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
  } as VxeTableGridOptions<MesHcPressSlotSpareApi.Record>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <input
      ref="stateImportInputRef"
      accept=".xlsx,.xls"
      class="hidden"
      type="file"
      @change="handleImportStateChange"
    />
    <StateGrid table-title="压槽辊/轴承当前状态">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '登记备件',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:md:press-slot-spare:update'],
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:md:press-slot-spare:query'],
              disabled: stateExporting,
              onClick: handleExportState,
            },
            {
              label: '导入初始化',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.UPLOAD,
              auth: ['mes:md:press-slot-spare:update'],
              disabled: stateImporting,
              onClick: handleImportStateClick,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '维护',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['mes:md:press-slot-spare:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '流水',
              type: 'link',
              auth: ['mes:md:press-slot-spare:query'],
              onClick: handleViewRecords.bind(null, row),
            },
          ]"
        />
      </template>
    </StateGrid>

    <AModal
      v-model:open="recordModalVisible"
      :footer="null"
      :title="recordModalTitle"
      class="press-slot-spare-record-modal"
      destroy-on-close
      width="1180px"
    >
      <div class="press-slot-spare-record-grid">
        <RecordGrid table-title="压槽备件使用/更换/清洗流水" />
      </div>
    </AModal>
  </Page>
</template>

<style scoped>
.press-slot-spare-record-grid {
  height: 620px;
}
</style>
