<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, h, ref } from 'vue';
import { useRoute } from 'vue-router';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Modal as AModal, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteToolingConsumableConsume,
  deleteToolingConsumableConsumeList,
  deleteToolingConsumableLedger,
  deleteToolingConsumableLedgerList,
  exportToolingConsumableLedger,
  getToolingConsumableConsumePage,
  getToolingConsumableLedgerPage,
  importToolingConsumableLedger,
} from '#/api/mes/hc/tooling-consumable-ledger';

import {
  isAdhesiveProcess,
  processText,
  resolveLedgerProcessCode,
  useConsumeGridColumns,
  useConsumeGridFormSchema,
  useLedgerGridColumns,
  useLedgerGridFormSchema,
} from './data';
import ToolingConsumableUsedUpMarkModal from './components/ToolingConsumableUsedUpMarkModal.vue';
import ToolingConsumableReturnModal from './components/ToolingConsumableReturnModal.vue';
import ConsumeForm from './modules/consume-form.vue';
import LedgerForm from './modules/ledger-form.vue';

const route = useRoute();
const checkedLedgerIds = ref<number[]>([]);
const checkedConsumeIds = ref<number[]>([]);
const exporting = ref(false);
const importing = ref(false);
const importInputRef = ref<HTMLInputElement>();
const selectedLedger = ref<MesHcToolingConsumableLedgerApi.Ledger>();
const consumeDetailVisible = ref(false);
const usedUpMarkModalRef = ref<InstanceType<typeof ToolingConsumableUsedUpMarkModal>>();
const returnModalRef = ref<InstanceType<typeof ToolingConsumableReturnModal>>();
const fixedProcessCode = computed(() => resolveLedgerProcessCode(route));
const fixedProcessName = computed(() => processText(fixedProcessCode.value));
const ledgerTableTitle = computed(() =>
  fixedProcessName.value
    ? `${fixedProcessName.value}边库耗材领用台账`
    : '边库耗材领用台账',
);

const consumeTableTitle = computed(() => {
  if (!selectedLedger.value?.id) {
    return isAdhesiveProcess(fixedProcessCode.value)
      ? '研发样品消耗明细（请先点击上方领用台账）'
      : '消耗明细（请先点击上方领用台账）';
  }
  const title = isAdhesiveProcess(fixedProcessCode.value)
    ? '研发样品消耗明细'
    : '消耗明细';
  return `${title}：${selectedLedger.value.batchNo || ''}`;
});
const consumeActionLabel = computed(() =>
  isAdhesiveProcess(fixedProcessCode.value) ? '研发样品消耗' : '消耗登记',
);

const [LedgerFormModal, ledgerFormModalApi] = useVbenModal({
  connectedComponent: LedgerForm,
  destroyOnClose: true,
});

const [ConsumeFormModal, consumeFormModalApi] = useVbenModal({
  connectedComponent: ConsumeForm,
  destroyOnClose: true,
});

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '操作失败，请稍后重试';
}

function handleLedgerRefresh() {
  ledgerGridApi.query();
  if (consumeDetailVisible.value && selectedLedger.value?.id) {
    consumeGridApi.query();
  }
}

function handleConsumeRefresh() {
  ledgerGridApi.query();
  if (consumeDetailVisible.value) {
    consumeGridApi.query();
  }
}

function isLedgerActionColumn(column?: any) {
  return column?.type === 'checkbox' || column?.slots?.default === 'actions';
}

function handleCreateLedger() {
  ledgerFormModalApi
    .setData({
      fixedProcessCode: fixedProcessCode.value,
      processCode: fixedProcessCode.value,
    })
    .open();
}

function handleEditLedger(row: MesHcToolingConsumableLedgerApi.Ledger) {
  ledgerFormModalApi
    .setData({ ...row, fixedProcessCode: fixedProcessCode.value })
    .open();
}

function handleMarkLedgerUsedUp(row: MesHcToolingConsumableLedgerApi.Ledger) {
  usedUpMarkModalRef.value?.open(row);
}

function handleReturnLedger(row: MesHcToolingConsumableLedgerApi.Ledger) {
  returnModalRef.value?.open(row);
}

async function handleDeleteLedger(row: MesHcToolingConsumableLedgerApi.Ledger) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteToolingConsumableLedger(row.id!);
    if (selectedLedger.value?.id === row.id) {
      selectedLedger.value = undefined;
      checkedConsumeIds.value = [];
      consumeGridApi.query();
    }
    checkedLedgerIds.value = checkedLedgerIds.value.filter(
      (id) => id !== row.id,
    );
    message.success('删除成功');
    ledgerGridApi.query();
  } finally {
    hideLoading();
  }
}

function handleConsumeDetailCancel() {
  consumeDetailVisible.value = false;
  checkedConsumeIds.value = [];
}

async function handleDeleteLedgerBatch() {
  await confirm('确认删除选中的领用台账及其消耗明细吗？');
  const ids = [...checkedLedgerIds.value];
  const hideLoading = message.loading({
    content: '正在批量删除...',
    duration: 0,
  });
  try {
    await deleteToolingConsumableLedgerList(ids);
    if (selectedLedger.value?.id && ids.includes(selectedLedger.value.id)) {
      selectedLedger.value = undefined;
      checkedConsumeIds.value = [];
      consumeGridApi.query();
    }
    checkedLedgerIds.value = [];
    message.success('删除成功');
    ledgerGridApi.query();
  } finally {
    hideLoading();
  }
}

function handleLedgerCheckboxChange({
  records,
}: {
  records: MesHcToolingConsumableLedgerApi.Ledger[];
}) {
  checkedLedgerIds.value = records.map((item) => item.id!).filter(Boolean);
}

async function handleSelectLedger(row: MesHcToolingConsumableLedgerApi.Ledger) {
  selectedLedger.value = row;
  checkedConsumeIds.value = [];
  await consumeGridApi.formApi.setValues({ ledgerId: row.id });
  consumeGridApi.query();
}

async function handleLedgerCellClick({
  column,
  row,
}: {
  column?: any;
  row: MesHcToolingConsumableLedgerApi.Ledger;
}) {
  if (isLedgerActionColumn(column)) {
    return;
  }
  await handleSelectLedger(row);
  consumeDetailVisible.value = true;
}

function openConsumeForm(
  ledger?: MesHcToolingConsumableLedgerApi.Ledger,
) {
  if (!ledger?.id) {
    message.warning('请先点击选择一条领用台账');
    return;
  }
  if (ledger.usageStatus !== 'ACTIVE') {
    message.warning('当前领用台账已结束，不能继续登记消耗');
    return;
  }
  consumeFormModalApi
    .setData({
      batchNo: ledger.batchNo,
      consumableType: ledger.consumableType,
      fixedProcessCode: fixedProcessCode.value,
      ledgerId: ledger.id,
      model: ledger.model,
      processCode: ledger.processCode,
    })
    .open();
}

function handleCreateConsume() {
  openConsumeForm(selectedLedger.value);
}

function handleRegisterConsume(
  row: MesHcToolingConsumableLedgerApi.Ledger,
) {
  selectedLedger.value = row;
  checkedConsumeIds.value = [];
  openConsumeForm(row);
}

function handleEditConsume(row: MesHcToolingConsumableLedgerApi.Consume) {
  consumeFormModalApi
    .setData({ ...row, fixedProcessCode: fixedProcessCode.value })
    .open();
}

async function handleDeleteConsume(
  row: MesHcToolingConsumableLedgerApi.Consume,
) {
  const hideLoading = message.loading({ content: '正在删除...', duration: 0 });
  try {
    await deleteToolingConsumableConsume(row.id!);
    checkedConsumeIds.value = checkedConsumeIds.value.filter(
      (id) => id !== row.id,
    );
    message.success('删除成功');
    consumeGridApi.query();
  } finally {
    hideLoading();
  }
}

async function handleDeleteConsumeBatch() {
  await confirm('确认删除选中的消耗明细吗？');
  const hideLoading = message.loading({
    content: '正在批量删除...',
    duration: 0,
  });
  try {
    await deleteToolingConsumableConsumeList(checkedConsumeIds.value);
    checkedConsumeIds.value = [];
    message.success('删除成功');
    consumeGridApi.query();
  } finally {
    hideLoading();
  }
}

function handleConsumeCheckboxChange({
  records,
}: {
  records: MesHcToolingConsumableLedgerApi.Consume[];
}) {
  checkedConsumeIds.value = records.map((item) => item.id!).filter(Boolean);
}

async function handleExport() {
  const formValues = await ledgerGridApi.formApi.getValues();
  exporting.value = true;
  try {
    const params = {
      ...formValues,
      processCode: fixedProcessCode.value || formValues.processCode,
    };
    const data = await exportToolingConsumableLedger(params);
    downloadFileFromBlobPart({
      fileName: `${fixedProcessName.value || '边库'}耗材领用台账.xlsx`,
      source: data,
    });
  } finally {
    exporting.value = false;
  }
}

function handleImportClick() {
  importInputRef.value?.click();
}

function showImportFailures(resp: MesHcToolingConsumableLedgerApi.ImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 20).join('\n');
  const moreText =
    failures.length > 20 ? `\n... 还有 ${failures.length - 20} 条未显示` : '';
  AModal.warning({
    title: '边库耗材领用台账导入校验未通过',
    width: 760,
    content: h('div', { class: 'space-y-2' }, [
      h(
        'div',
        `读取 ${resp.totalRows || 0} 行，跳过 ${resp.skippedRows || 0} 行，失败 ${
          resp.failureCount || 0
        } 条。未写入任何数据。`,
      ),
      h(
        'pre',
        {
          style:
            'white-space: pre-wrap; margin: 0; max-height: 320px; overflow: auto; font-size: 12px;',
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

  const hideLoading = message.loading({
    content: '正在导入边库耗材领用台账...',
    duration: 0,
  });
  importing.value = true;
  try {
    const resp = await importToolingConsumableLedger(
      file,
      fixedProcessCode.value,
    );
    if ((resp.failureCount || 0) > 0) {
      showImportFailures(resp);
      return;
    }
    const extraMessage = (resp.messages || []).join('；');
    message.success(
      extraMessage ||
        `导入成功：新增领用 ${resp.createdLedgerCount || 0} 条，更新领用 ${
          resp.updatedLedgerCount || 0
        } 条，新增消耗 ${resp.createdConsumeCount || 0} 条，更新消耗 ${resp.updatedConsumeCount || 0} 条`,
    );
    checkedLedgerIds.value = [];
    checkedConsumeIds.value = [];
    handleLedgerRefresh();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    importing.value = false;
    hideLoading();
  }
}

const [LedgerGrid, ledgerGridApi] = useVbenVxeGrid({
  class: 'tooling-ledger-vben-grid',
  formOptions: {
    schema: useLedgerGridFormSchema(fixedProcessCode.value),
  },
  gridClass: 'tooling-ledger-vxe-grid',
  gridOptions: {
    columns: useLedgerGridColumns(fixedProcessCode.value),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getToolingConsumableLedgerPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            processCode: fixedProcessCode.value || formValues.processCode,
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
  } as VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Ledger>,
  gridEvents: {
    cellClick: handleLedgerCellClick,
    checkboxAll: handleLedgerCheckboxChange,
    checkboxChange: handleLedgerCheckboxChange,
  },
});

const [ConsumeGrid, consumeGridApi] = useVbenVxeGrid({
  class: 'tooling-consume-vben-grid',
  formOptions: {
    schema: useConsumeGridFormSchema(fixedProcessCode.value),
  },
  gridClass: 'tooling-consume-vxe-grid',
  gridOptions: {
    columns: useConsumeGridColumns(fixedProcessCode.value),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const ledgerId = selectedLedger.value?.id || formValues.ledgerId;
          if (!ledgerId) {
            return { list: [], total: 0 };
          }
          return await getToolingConsumableConsumePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            ledgerId,
            processCode: fixedProcessCode.value || formValues.processCode,
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
      search: true,
    },
  } as VxeTableGridOptions<MesHcToolingConsumableLedgerApi.Consume>,
  gridEvents: {
    checkboxAll: handleConsumeCheckboxChange,
    checkboxChange: handleConsumeCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height content-class="tooling-ledger-page">
    <LedgerFormModal @success="handleLedgerRefresh" />
    <ConsumeFormModal @success="handleConsumeRefresh" />
    <ToolingConsumableUsedUpMarkModal
      ref="usedUpMarkModalRef"
      @success="handleLedgerRefresh"
    />
    <ToolingConsumableReturnModal
      ref="returnModalRef"
      @success="handleLedgerRefresh"
    />
    <AModal
      v-model:open="consumeDetailVisible"
      :body-style="{ height: '70vh', overflow: 'hidden', padding: '12px' }"
      :footer="null"
      :title="consumeTableTitle"
      width="1180px"
      @cancel="handleConsumeDetailCancel"
    >
      <div class="tooling-consume-modal-grid">
        <ConsumeGrid :table-title="consumeTableTitle">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  label: '新增消耗',
                  type: 'primary',
                  icon: ACTION_ICON.ADD,
                  disabled: !selectedLedger?.id || selectedLedger?.usageStatus !== 'ACTIVE',
                  onClick: handleCreateConsume,
                },
                {
                  label: '批量删除',
                  type: 'primary',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  disabled: isEmpty(checkedConsumeIds),
                  onClick: handleDeleteConsumeBatch,
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
                  disabled: selectedLedger?.usageStatus !== 'ACTIVE',
                  onClick: handleEditConsume.bind(null, row),
                },
                {
                  label: '删除',
                  type: 'link',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  disabled: selectedLedger?.usageStatus !== 'ACTIVE',
                  popConfirm: {
                    title: '确认删除当前消耗明细吗？',
                    confirm: handleDeleteConsume.bind(null, row),
                  },
                },
              ]"
            />
          </template>
        </ConsumeGrid>
      </div>
    </AModal>
    <input
      ref="importInputRef"
      accept=".xlsx,.xls"
      class="hidden"
      type="file"
      @change="handleImportChange"
    />

    <div class="tooling-ledger-grid-host">
      <LedgerGrid :table-title="ledgerTableTitle">
        <template #toolbar-tools>
          <TableAction
            :actions="[
              {
                label: '新增',
                type: 'primary',
                icon: ACTION_ICON.ADD,
                auth: ['mes:md:tooling-consumable-ledger:create'],
                onClick: handleCreateLedger,
              },
              {
                label: '导出',
                type: 'primary',
                icon: ACTION_ICON.DOWNLOAD,
                disabled: exporting,
                onClick: handleExport,
              },
              {
                label: '导入',
                type: 'primary',
                icon: ACTION_ICON.UPLOAD,
                disabled: importing,
                onClick: handleImportClick,
              },
              {
                label: '批量删除',
                type: 'primary',
                danger: true,
                icon: ACTION_ICON.DELETE,
                disabled: isEmpty(checkedLedgerIds),
                onClick: handleDeleteLedgerBatch,
              },
            ]"
          />
        </template>
        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: consumeActionLabel,
                type: 'link',
                icon: 'lucide:clipboard-pen-line',
                disabled: row.usageStatus !== 'ACTIVE',
                onClick: handleRegisterConsume.bind(null, row),
              },
              {
                label: '编辑',
                type: 'link',
                icon: ACTION_ICON.EDIT,
                auth: ['mes:md:tooling-consumable-ledger:update'],
                disabled: row.usageStatus === 'RETURNED'
                  || (isAdhesiveProcess(row.processCode) && row.usageStatus !== 'ACTIVE'),
                onClick: handleEditLedger.bind(null, row),
              },
              {
                label: '标记完成',
                type: 'link',
                icon: 'lucide:check-circle-2',
                auth: ['mes:md:tooling-consumable-ledger:mark-used-up'],
                disabled: row.usageStatus !== 'ACTIVE',
                onClick: handleMarkLedgerUsedUp.bind(null, row),
              },
              {
                label: '退库',
                type: 'link',
                danger: true,
                icon: 'lucide:undo-2',
                auth: ['mes:md:tooling-consumable-ledger:return'],
                disabled: row.usageStatus !== 'ACTIVE' || Number(row.balanceQty || 0) <= 0,
                onClick: handleReturnLedger.bind(null, row),
              },
              {
                label: '删除',
                type: 'link',
                danger: true,
                icon: ACTION_ICON.DELETE,
                disabled: row.usageStatus === 'RETURNED'
                  || (isAdhesiveProcess(row.processCode) && row.usageStatus !== 'ACTIVE'),
                popConfirm: {
                  title: '确认删除当前领用台账及其消耗明细吗？',
                  confirm: handleDeleteLedger.bind(null, row),
                },
              },
            ]"
          />
        </template>
      </LedgerGrid>
    </div>
  </Page>
</template>

<style scoped>
.tooling-consume-modal-grid {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

:global(.tooling-ledger-page) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.tooling-ledger-grid-host {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.tooling-ledger-grid-host :deep(.tooling-ledger-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-width: 0 !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.tooling-ledger-grid-host :deep(.tooling-ledger-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  max-height: 100% !important;
  min-width: 0 !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.tooling-ledger-grid-host :deep(.vxe-grid--form-wrapper) {
  grid-row: 1;
  min-height: 0;
  overflow: visible;
}

.tooling-ledger-grid-host :deep(.vxe-grid--toolbar-wrapper) {
  grid-row: 2;
  min-height: 0;
}

.tooling-ledger-grid-host :deep(.vxe-grid--top-wrapper) {
  grid-row: 3;
  min-height: 0;
  overflow: visible;
}

.tooling-ledger-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.tooling-ledger-grid-host :deep(.vxe-grid--bottom-wrapper) {
  grid-row: 5;
  min-height: 0;
}

.tooling-ledger-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  min-height: 0;
  background: #fff;
}

.tooling-ledger-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.tooling-ledger-grid-host :deep(.vxe-pager) {
  min-height: 36px;
}

.tooling-consume-modal-grid :deep(.tooling-consume-vben-grid),
.tooling-consume-modal-grid :deep(.tooling-consume-vxe-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.tooling-consume-modal-grid :deep(.tooling-consume-vxe-grid) {
  display: flex !important;
  flex-direction: column;
}

.tooling-consume-modal-grid :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0 !important;
  overflow: hidden !important;
}

.tooling-consume-modal-grid :deep(.vxe-grid--bottom-wrapper),
.tooling-consume-modal-grid :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.tooling-consume-modal-grid :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}
</style>
