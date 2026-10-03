<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcAdhesiveConsoleApi } from '#/api/mes/hc/execution/adhesive-console';

import { computed, h, nextTick, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import { downloadFileFromBlobPart } from '@vben/utils';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import { Alert, Button, DatePicker, Input, InputNumber, Modal as AModal, Select, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createAdhesiveConsoleGlueBoardStock,
  exportAdhesiveConsoleGlueBoardStock,
  getAdhesiveConsoleGlueBoardStockPage,
  importAdhesiveConsoleGlueBoardStock,
  markAdhesiveConsoleGlueBoardStockPrinted,
} from '#/api/mes/hc/execution/adhesive-console';

import {
  GLUE_BOARD_MODEL_OPTIONS,
  LIFETIME_MODE_OPTIONS,
  getGlueBoardModelOption,
  loadGlueBoardModelOptions,
  useGridColumns,
  useGridFormSchema,
} from './data';

defineOptions({ name: 'MesExecutionAdhesiveGlueBoard' });

const userStore = useUserStore();
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || (userStore.userInfo as any)?.realName || '系统',
);

const receiveVisible = ref(false);
const qrVisible = ref(false);
const qrValue = ref('');
const qrRow = ref<MesHcAdhesiveConsoleApi.GlueBoardStock | null>(null);
const qrCodeDataUrl = useQRCode(qrValue, { margin: 1, width: 180 });
const receiveGlueBoardModelOptions = ref([...GLUE_BOARD_MODEL_OPTIONS]);
const receiveGlueBoardModelLoading = ref(false);
const stockExporting = ref(false);
const stockImporting = ref(false);
const stockImportInputRef = ref<HTMLInputElement>();
const DEFAULT_GLUE_BOARD_MATERIAL_CODE = '01.02.00002';

function getDefaultGlueBoardModel(options = GLUE_BOARD_MODEL_OPTIONS) {
  return options[0]?.value || '';
}

const receiveForm = reactive<MesHcAdhesiveConsoleApi.SaveGlueBoardStockReq>({
  accessoryCategory: 'GLUE_BOARD',
  accessoryCategoryName: '胶板',
  availableStartPosition: 0,
  edgeWarehouseName: '粘胶边库',
  erpTransferNo: '',
  glueBoardBatchNo: '',
  glueBoardMaterialCode: DEFAULT_GLUE_BOARD_MATERIAL_CODE,
  glueBoardMaterialName: '胶板',
  glueBoardModel: getDefaultGlueBoardModel(),
  receiveDate: dayjs().format('YYYY-MM-DD'),
  receiveTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  receiverName: currentUserName.value,
  stockMeasureMode: 'LENGTH',
  transferQty: 1,
  transferUnit: '卷',
  lifetimeMode: 'NONE',
  lifetimeLimitCount: undefined,
  lifetimeLimitLength: undefined,
  lifeUsedCount: 0,
  lifeUsedLength: 0,
  unpackQty: 0,
  unpackUnit: '米',
});

const needLifeLength = computed(() => ['LENGTH', 'LENGTH_OR_COUNT'].includes(receiveForm.lifetimeMode || ''));
const needLifeCount = computed(() => ['COUNT', 'LENGTH_OR_COUNT'].includes(receiveForm.lifetimeMode || ''));

function normalizeGlueBoardModel(value?: string, options = GLUE_BOARD_MODEL_OPTIONS) {
  const model = String(value || '').trim();
  return options.some((item) => item.value === model) ? model : getDefaultGlueBoardModel(options);
}

function applyGlueBoardModelOption(value?: string, options = receiveGlueBoardModelOptions.value) {
  const option = getGlueBoardModelOption(value, options);
  if (!option) {
    return;
  }
  receiveForm.glueBoardModel = option.value;
  if (option.extra?.glueBoardMaterialCode) {
    receiveForm.glueBoardMaterialCode = option.extra.glueBoardMaterialCode;
  }
  if (option.extra?.glueBoardMaterialName) {
    receiveForm.glueBoardMaterialName = option.extra.glueBoardMaterialName;
  } else {
    receiveForm.glueBoardMaterialName = '胶板';
  }
}

async function refreshGridGlueBoardModelOptions() {
  const options = await loadGlueBoardModelOptions();
  gridApi.formApi.updateSchema([
    {
      fieldName: 'glueBoardModel',
      componentProps: {
        allowClear: true,
        options,
        placeholder: '请选择胶板型号',
      },
    },
  ]);
}

async function refreshReceiveGlueBoardModelOptions() {
  receiveGlueBoardModelLoading.value = true;
  try {
    const materialCode = String(receiveForm.glueBoardMaterialCode || '').trim();
    const options = await loadGlueBoardModelOptions(materialCode ? { glueBoardMaterialCode: materialCode } : {});
    receiveGlueBoardModelOptions.value = options;
    receiveForm.glueBoardModel = normalizeGlueBoardModel(receiveForm.glueBoardModel, options);
    applyGlueBoardModelOption(receiveForm.glueBoardModel, options);
  } finally {
    receiveGlueBoardModelLoading.value = false;
  }
}

function resetReceiveForm() {
  Object.assign(receiveForm, {
    accessoryCategory: 'GLUE_BOARD',
    accessoryCategoryName: '胶板',
    availableLength: undefined,
    availableStartPosition: 0,
    edgeWarehouseCode: '',
    edgeWarehouseName: '粘胶边库',
    erpTransferNo: '',
    glueBoardBatchNo: '',
    glueBoardMaterialCode: DEFAULT_GLUE_BOARD_MATERIAL_CODE,
    glueBoardMaterialName: '胶板',
    glueBoardModel: getDefaultGlueBoardModel(receiveGlueBoardModelOptions.value),
    receiveDate: dayjs().format('YYYY-MM-DD'),
    receiveLength: undefined,
    receiveStartPosition: undefined,
    receiveTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    receiverName: currentUserName.value,
    remark: '',
    sourceWarehouseCode: '',
    sourceWarehouseName: '',
    stockMeasureMode: 'LENGTH',
    lifetimeMode: 'NONE',
    lifetimeLimitCount: undefined,
    lifetimeLimitLength: undefined,
    lifeUsedCount: 0,
    lifeUsedLength: 0,
    transferQty: 1,
    transferUnit: '卷',
    unpackQty: 0,
    unpackUnit: '米',
  });
}

function handleGlueBoardModelChange(value: string) {
  receiveForm.glueBoardModel = normalizeGlueBoardModel(value, receiveGlueBoardModelOptions.value);
  applyGlueBoardModelOption(receiveForm.glueBoardModel);
}

async function handleGlueBoardMaterialCodeBlur() {
  const beforeModel = receiveForm.glueBoardModel;
  await refreshReceiveGlueBoardModelOptions();
  if (beforeModel && beforeModel !== receiveForm.glueBoardModel) {
    message.warning('当前辅料料号下未维护原胶板型号，已按成品胶板对照表调整');
  }
}

async function handleCreate() {
  resetReceiveForm();
  await refreshReceiveGlueBoardModelOptions();
  receiveVisible.value = true;
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

async function handleExportStock() {
  const formValues = await gridApi.formApi.getValues();
  stockExporting.value = true;
  try {
    const data = await exportAdhesiveConsoleGlueBoardStock(formValues);
    downloadFileFromBlobPart({ fileName: '胶板边库当前库存.xlsx', source: data });
  } finally {
    stockExporting.value = false;
  }
}

function handleImportStockClick() {
  AModal.confirm({
    title: '导入初始化确认',
    content:
      '导入将先物理删除当前租户下的胶板边库库存，再按 Excel 数据初始化当前库存快照；历史报工/领用台账不会被删除。此操作不可撤销，请确认已导出备份。',
    okText: '确认清空并导入',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => stockImportInputRef.value?.click(),
  });
}

function showImportFailures(resp: MesHcAdhesiveConsoleApi.GlueBoardStockImportResp) {
  const failures = resp.failures || [];
  const text = failures.slice(0, 20).join('\n');
  const moreText = failures.length > 20 ? `\n... 还有 ${failures.length - 20} 条未显示` : '';
  AModal.warning({
    title: '胶板边库库存导入校验未通过',
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

async function handleImportStockChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }

  const hideLoading = message.loading({ content: '正在清空并导入初始化...', duration: 0 });
  stockImporting.value = true;
  try {
    const resp = await importAdhesiveConsoleGlueBoardStock(file, true);
    if ((resp.failureCount || 0) > 0) {
      showImportFailures(resp);
      return;
    }
    const extraMessage = (resp.messages || []).join('；');
    message.success(
      extraMessage ||
        `导入成功：初始化 ${resp.successCount || 0} 条，清空库存 ${resp.clearedStockCount || 0} 条`,
    );
    gridApi.query();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    stockImporting.value = false;
    hideLoading();
  }
}

function formatStockAvailable(row: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  if (row.stockMeasureMode === 'COUNT') {
    return `${Number(row.availableCount || 0).toFixed(3)} 个`;
  }
  const start = Number(row.availableStartPosition || 0);
  const length = Number(row.availableLength || 0);
  return `${start.toFixed(3)}-${(start + length).toFixed(3)} m`;
}

async function handleSubmitReceive() {
  if (!receiveForm.glueBoardMaterialCode?.trim()) {
    message.warning('请填写辅料料号');
    return;
  }
  if (!receiveForm.glueBoardModel?.trim()) {
    message.warning('请选择胶板型号');
    return;
  }
  const normalizedGlueBoardModel = normalizeGlueBoardModel(receiveForm.glueBoardModel, receiveGlueBoardModelOptions.value);
  if (!normalizedGlueBoardModel) {
    message.warning('当前辅料料号未维护可用胶板型号，请先维护成品胶板对照表');
    return;
  }
  receiveForm.glueBoardModel = normalizedGlueBoardModel;
  applyGlueBoardModelOption(normalizedGlueBoardModel);
  if (!receiveForm.glueBoardBatchNo?.trim()) {
    message.warning('请填写胶板批号');
    return;
  }
  if (!receiveForm.erpTransferNo?.trim()) {
    message.warning('请填写ERP移库单号');
    return;
  }
  if (Number(receiveForm.transferQty || 0) <= 0) {
    message.warning('请填写大于0的移库数量');
    return;
  }
  if (Number(receiveForm.unpackQty || 0) <= 0) {
    message.warning('请填写大于0的拆包量');
    return;
  }
  const availableStart = Number(receiveForm.availableStartPosition || 0);
  const availableLength = Number(receiveForm.availableLength || receiveForm.unpackQty || 0);
  if (availableLength <= 0) {
    message.warning('请填写大于0的可用长度');
    return;
  }
  if (needLifeLength.value && Number(receiveForm.lifetimeLimitLength || 0) <= 0) {
    message.warning('请填写大于0的寿命上限米数');
    return;
  }
  if (needLifeCount.value && Number(receiveForm.lifetimeLimitCount || 0) <= 0) {
    message.warning('请填写大于0的寿命上限次数');
    return;
  }
  await createAdhesiveConsoleGlueBoardStock({
    ...receiveForm,
    availableLength,
    availableStartPosition: availableStart,
    glueBoardBatchNo: receiveForm.glueBoardBatchNo.trim(),
    glueBoardMaterialCode: receiveForm.glueBoardMaterialCode.trim(),
    glueBoardModel: normalizedGlueBoardModel,
    accessoryCategory: 'GLUE_BOARD',
    accessoryCategoryName: '胶板',
    stockMeasureMode: 'LENGTH',
    availableCount: 0,
    receiveCount: 0,
    receiveLength: availableLength,
    receiveStartPosition: availableStart,
    transferUnit: receiveForm.transferUnit || '卷',
    unpackUnit: receiveForm.unpackUnit || '米',
  });
  receiveVisible.value = false;
  message.success('胶板边库领料已登记');
  gridApi.query();
}

function handleOpenPrint(row: MesHcAdhesiveConsoleApi.GlueBoardStock) {
  qrRow.value = row;
  qrValue.value = row.glueBoardBatchNo || '';
  qrVisible.value = true;
}

async function handlePrintQr() {
  if (!qrRow.value?.id || !qrRow.value.glueBoardBatchNo) {
    message.warning('胶板批号为空，不能打印二维码');
    return;
  }
  await nextTick();
  const qrImage = qrCodeDataUrl.value || '';
  const row = qrRow.value;
  const html = `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>胶板边库二维码</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; font-family: "Microsoft YaHei", Arial, sans-serif; color: #111827; }
    .label { width: 90mm; min-height: 56mm; padding: 8mm; border: 1px solid #111827; }
    .title { margin-bottom: 4mm; font-size: 18px; font-weight: 800; text-align: center; }
    .body { display: flex; gap: 6mm; align-items: center; }
    .qr { width: 30mm; height: 30mm; border: 1px solid #d1d5db; padding: 2mm; }
    .kv { flex: 1; font-size: 12px; line-height: 1.9; }
    .batch { font-size: 15px; font-weight: 800; }
    @media print { body { margin: 0; } .label { border-color: #111827; } }
  </style>
</head>
<body>
  <div class="label">
    <div class="title">胶板边库批次二维码</div>
    <div class="body">
      <img class="qr" src="${qrImage}" />
      <div class="kv">
        <div>辅料料号：${row.glueBoardMaterialCode || '-'}</div>
        <div>胶板型号：${row.glueBoardModel || '-'}</div>
        <div class="batch">胶板批号：${row.glueBoardBatchNo || '-'}</div>
        <div>当前可用：${formatStockAvailable(row)}</div>
        <div>边库：${row.edgeWarehouseName || '-'}</div>
      </div>
    </div>
  </div>
</body>
</html>`;
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.warning('浏览器阻止了打印窗口，请允许弹窗后重试');
    return;
  }
  printWindow.document.open();
  printWindow.document.write(html);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => printWindow.print(), 200);
  await markAdhesiveConsoleGlueBoardStockPrinted(qrRow.value.id);
  qrVisible.value = false;
  gridApi.query();
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
          await getAdhesiveConsoleGlueBoardStockPage({
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
  } as VxeTableGridOptions<MesHcAdhesiveConsoleApi.GlueBoardStock>,
});

onMounted(async () => {
  await refreshGridGlueBoardModelOptions();
  await refreshReceiveGlueBoardModelOptions();
});
</script>

<template>
  <Page auto-content-height>
    <Alert
      banner
      class="mb-3"
      message="胶板边库已调整为库存查询、二维码打印入口；胶板领用与消耗请分别在粘胶1、粘胶2耗材领用台账维护。"
      type="info"
    />
    <input
      ref="stockImportInputRef"
      accept=".xlsx,.xls"
      class="hidden"
      type="file"
      @change="handleImportStockChange"
    />
    <Grid table-title="胶板边库库存查询">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新建领料',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['mes:sfc:adhesive-glue-board:create'],
              disabled: true,
              onClick: handleCreate,
            },
            {
              label: '导出',
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['mes:sfc:adhesive-glue-board:query'],
              disabled: stockExporting,
              onClick: handleExportStock,
            },
            {
              label: '导入初始化',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.UPLOAD,
              auth: ['mes:sfc:adhesive-glue-board:create'],
              disabled: true,
              onClick: handleImportStockClick,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '二维码',
              type: 'link',
              auth: ['mes:sfc:adhesive-glue-board:create'],
              onClick: handleOpenPrint.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>

    <AModal
      v-model:open="receiveVisible"
      :footer="null"
      class="adhesive-glue-board-modal"
      destroy-on-close
      title="胶板边库领料"
      width="980px"
    >
      <div class="receive-form">
        <div class="receive-form__body">
          <section class="receive-section">
            <div class="receive-section__title">基础信息</div>
            <div class="receive-grid">
              <div class="receive-field">
                <label>辅料料号</label>
                <Input v-model:value="receiveForm.glueBoardMaterialCode" @blur="handleGlueBoardMaterialCodeBlur" />
              </div>
              <div class="receive-field">
                <label>胶板型号</label>
                <Select
                  v-model:value="receiveForm.glueBoardModel"
                  :loading="receiveGlueBoardModelLoading"
                  :not-found-content="receiveGlueBoardModelLoading ? '加载中...' : '当前料号未维护成品胶板对照'"
                  :options="receiveGlueBoardModelOptions"
                  class="w-full"
                  placeholder="请选择胶板型号"
                  show-search
                  @change="handleGlueBoardModelChange"
                />
              </div>
              <div class="receive-field">
                <label>辅料名称</label>
                <Input v-model:value="receiveForm.glueBoardMaterialName" />
              </div>
              <div class="receive-field">
                <label>胶板批号</label>
                <Input v-model:value="receiveForm.glueBoardBatchNo" placeholder="胶板边库批次/扫码批次" />
              </div>
              <div class="receive-field">
                <label>边库</label>
                <Input v-model:value="receiveForm.edgeWarehouseName" />
              </div>
            </div>
          </section>

          <section class="receive-section">
            <div class="receive-section__title">ERP移库与拆包</div>
            <div class="receive-grid">
              <div class="receive-field">
                <label>ERP移库单号</label>
                <Input v-model:value="receiveForm.erpTransferNo" placeholder="金蝶云ERP移库单号" />
              </div>
              <div class="receive-field">
                <label>移库数量</label>
                <InputNumber v-model:value="receiveForm.transferQty" :min="0" :precision="3" class="w-full" />
              </div>
              <div class="receive-field">
                <label>移库单位</label>
                <Input v-model:value="receiveForm.transferUnit" placeholder="卷" />
              </div>
              <div class="receive-field">
                <label>拆包量</label>
                <InputNumber v-model:value="receiveForm.unpackQty" :min="0" :precision="3" class="w-full" />
              </div>
              <div class="receive-field">
                <label>拆包单位</label>
                <Input v-model:value="receiveForm.unpackUnit" placeholder="米" />
              </div>
            </div>
          </section>

          <section class="receive-section">
            <div class="receive-section__title">边库可用余量</div>
            <div class="receive-grid">
              <div class="receive-field">
                <label>可用位置起(m)</label>
                <InputNumber v-model:value="receiveForm.availableStartPosition" :min="0" :precision="3" class="w-full" />
              </div>
              <div class="receive-field">
                <label>可用长度(m)</label>
                <InputNumber v-model:value="receiveForm.availableLength" :min="0" :precision="3" class="w-full" placeholder="默认等于拆包量" />
              </div>
            </div>
          </section>

          <section class="receive-section">
            <div class="receive-section__title">寿命与领料信息</div>
            <div class="receive-grid">
              <div class="receive-field">
                <label>寿命口径</label>
                <Select v-model:value="receiveForm.lifetimeMode" :options="LIFETIME_MODE_OPTIONS" class="w-full" />
              </div>
              <div v-if="needLifeLength" class="receive-field">
                <label>寿命上限(m)</label>
                <InputNumber v-model:value="receiveForm.lifetimeLimitLength" :min="0" :precision="3" class="w-full" />
              </div>
              <div v-if="needLifeCount" class="receive-field">
                <label>寿命上限(次)</label>
                <InputNumber v-model:value="receiveForm.lifetimeLimitCount" :min="0" :precision="0" class="w-full" />
              </div>
              <div v-if="needLifeLength" class="receive-field">
                <label>当前已用(m)</label>
                <InputNumber v-model:value="receiveForm.lifeUsedLength" :min="0" :precision="3" class="w-full" />
              </div>
              <div v-if="needLifeCount" class="receive-field">
                <label>当前已用(次)</label>
                <InputNumber v-model:value="receiveForm.lifeUsedCount" :min="0" :precision="0" class="w-full" />
              </div>
              <div class="receive-field">
                <label>领料日期</label>
                <DatePicker v-model:value="receiveForm.receiveDate" class="w-full" value-format="YYYY-MM-DD" />
              </div>
              <div class="receive-field">
                <label>领料人</label>
                <Input v-model:value="receiveForm.receiverName" />
              </div>
              <div class="receive-field receive-field--full">
                <label>备注</label>
                <Input v-model:value="receiveForm.remark" />
              </div>
            </div>
          </section>
        </div>
        <div class="modal-footer">
          <Button @click="receiveVisible = false">取消</Button>
          <Button type="primary" @click="handleSubmitReceive">确认领料</Button>
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="qrVisible"
      :footer="null"
      class="adhesive-glue-board-modal"
      destroy-on-close
      title="胶板边库二维码打印"
      width="520px"
    >
      <div class="qr-preview">
        <img :src="qrCodeDataUrl" alt="胶板批次二维码" />
        <div class="qr-preview__text">
          <strong>{{ qrRow?.glueBoardBatchNo || '-' }}</strong>
          <span>{{ qrRow?.glueBoardMaterialCode || '-' }}</span>
        </div>
        <div class="modal-footer">
          <Button @click="qrVisible = false">关闭</Button>
          <Button type="primary" @click="handlePrintQr">打印二维码</Button>
        </div>
      </div>
    </AModal>
  </Page>
</template>

<style scoped>
.receive-form {
  padding: 8px 4px 0;
}

.receive-form__body {
  display: flex;
  max-height: calc(100vh - 260px);
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.receive-section {
  border: 1px solid #dbe4ef;
  background: linear-gradient(180deg, #f9fbfd 0%, #fff 100%);
}

.receive-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #dbe4ef;
  padding: 8px 12px;
  color: #1f2f46;
  font-size: 14px;
  font-weight: 800;
}

.receive-section__title::before {
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: #2f6f9f;
  content: '';
}

.receive-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
  padding: 12px;
}

.receive-field {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.receive-field label {
  width: 112px;
  flex: 0 0 112px;
  padding: 6px 8px;
  border: 1px solid #d6dde8;
  background: #eef3f8;
  color: #27364a;
  font-weight: 700;
  text-align: right;
}

.receive-field :deep(.ant-input),
.receive-field :deep(.ant-input-number),
.receive-field :deep(.ant-picker),
.receive-field :deep(.ant-select-selector) {
  border-radius: 0;
}

.receive-field--full {
  grid-column: span 3;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
}

.qr-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
}

.qr-preview img {
  width: 180px;
  height: 180px;
  border: 1px solid #d6dde8;
  padding: 8px;
  background: #fff;
}

.qr-preview__text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #27364a;
}

.qr-preview__text strong {
  font-size: 18px;
}
</style>
