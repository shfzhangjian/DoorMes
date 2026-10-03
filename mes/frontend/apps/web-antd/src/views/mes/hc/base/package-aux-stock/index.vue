<script lang="ts" setup>
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { useQRCode } from '@vueuse/integrations/useQRCode';

import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Select,
  Table as ATable,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getPackageAuxTodayList,
  getPackageAuxStockPage,
  printPackageAuxStock,
  savePackageAuxStock,
} from '#/api/mes/hc/package-fg/finished-packaging';

defineOptions({ name: 'MesHcBasePackageAuxStock' });

type PackageAuxStock = MesHcFinishedPackagingApi.PackageAuxStock;
type PackageAuxConsumeRecord = MesHcFinishedPackagingApi.PackageAuxConsumeRecord;
type SavePackageAuxStockReq = MesHcFinishedPackagingApi.SavePackageAuxStockReq;

const userStore = useUserStore();
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统');

const loading = ref(false);
const saving = ref(false);
const createVisible = ref(false);
const printVisible = ref(false);
const ledgerVisible = ref(false);
const ledgerLoading = ref(false);
const rows = ref<PackageAuxStock[]>([]);
const ledgerRows = ref<PackageAuxConsumeRecord[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const qrRow = ref<PackageAuxStock | null>(null);
const qrValue = ref('');
const qrCodeDataUrl = useQRCode(qrValue, { margin: 1, width: 180 });
const ledgerQuery = reactive({
  bizType: undefined as string | undefined,
  recordDate: dayjs().format('YYYY-MM-DD'),
});

const query = reactive({
  batchNo: '',
  materialCode: '',
  materialName: '',
  receiveDate: undefined as string | undefined,
  stockStatus: undefined as string | undefined,
});

const form = reactive<SavePackageAuxStockReq>({
  auxCategory: 'PACKAGING_BAG',
  auxCategoryName: '包装袋',
  batchNo: '',
  edgeWarehouseName: '包装辅材边库',
  materialCode: 'PKG-PACKAGING-BAG',
  materialName: '包装袋',
  receiveDate: dayjs().format('YYYY-MM-DD'),
  receiveQty: 0,
  receiveTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  receiverName: currentUserName.value,
  transferUnit: '包',
  unpackUnit: '个',
});

const statusOptions = [
  { label: '可用', value: 'ACTIVE' },
  { label: '已用完', value: 'USED_UP' },
  { label: '已锁定', value: 'LOCKED' },
];

const auxCategoryOptions = [
  { label: '包装袋', materialCode: 'PKG-PACKAGING-BAG', value: 'PACKAGING_BAG' },
  { label: '隔离膜', materialCode: 'PKG-ISOLATION-FILM', value: 'ISOLATION_FILM' },
  { label: '纸盒', materialCode: 'PKG-PAPER-BOX', value: 'PAPER_BOX' },
  { label: '纸板', materialCode: 'PKG-PAPER-BOARD', value: 'PAPER_BOARD' },
];

const ledgerBizTypeOptions = [
  { label: '成品内包装', value: 'FG_INNER_PACKAGING' },
  { label: '发货外包装', value: 'FG_SHIPPING_OUTER_PACKAGING' },
  { label: '手工补录', value: 'FG_INBOUND' },
];

const columns = [
  { dataIndex: 'auxCategoryName', title: '辅材类别', width: 120 },
  { dataIndex: 'materialCode', fixed: 'left', title: '辅材料号', width: 150 },
  { dataIndex: 'materialName', fixed: 'left', title: '辅材名称', width: 130 },
  { dataIndex: 'auxSpec', title: '规格/型号', width: 120 },
  { dataIndex: 'batchNo', fixed: 'left', title: '辅材批次', width: 180 },
  { dataIndex: 'receiveQty', title: '入库数量', width: 110 },
  { dataIndex: 'usedQty', title: '已消耗', width: 110 },
  { dataIndex: 'availableQty', title: '可用数量', width: 110 },
  { dataIndex: 'stockStatus', title: '库存状态', width: 110 },
  { dataIndex: 'edgeWarehouseName', title: '边库', width: 140 },
  { dataIndex: 'erpTransferNo', title: 'ERP移库单号', width: 160 },
  { dataIndex: 'receiverName', title: '领料人', width: 110 },
  { dataIndex: 'receiveDate', title: '领料日期', width: 120 },
  { dataIndex: 'printCount', title: '打印次数', width: 100 },
  { dataIndex: 'remark', title: '备注', width: 180 },
  { dataIndex: 'action', fixed: 'right', title: '操作', width: 110 },
];

const ledgerColumns = [
  { dataIndex: 'bizType', title: '领用环节', width: 140 },
  { dataIndex: 'bizNo', title: '包装单号', width: 180 },
  { dataIndex: 'materialName', title: '辅材名称', width: 130 },
  { dataIndex: 'auxSpec', title: '规格/型号', width: 120 },
  { dataIndex: 'batchNo', title: '辅材批次', width: 170 },
  { dataIndex: 'consumeQty', title: '领用数量', width: 110 },
  { dataIndex: 'beforeAvailableQty', title: '领用前可用', width: 120 },
  { dataIndex: 'afterAvailableQty', title: '领用后可用', width: 120 },
  { dataIndex: 'recorderName', title: '领用人', width: 110 },
  { dataIndex: 'recorderTime', title: '领用时间', width: 170 },
  { dataIndex: 'remark', title: '说明', width: 180 },
];

function statusText(status?: string) {
  const map: Record<string, string> = {
    ACTIVE: '可用',
    LOCKED: '已锁定',
    USED_UP: '已用完',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    ACTIVE: 'green',
    LOCKED: 'gold',
    USED_UP: 'default',
  };
  return map[status || ''] || 'default';
}

function formatQty(value?: number) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toFixed(3) : '-';
}

function resetForm() {
  Object.assign(form, {
    auxCategory: 'PACKAGING_BAG',
    auxCategoryName: '包装袋',
    auxSpec: '',
    batchNo: '',
    edgeWarehouseCode: '',
    edgeWarehouseName: '包装辅材边库',
    erpTransferNo: '',
    materialCode: 'PKG-PACKAGING-BAG',
    materialName: '包装袋',
    receiveDate: dayjs().format('YYYY-MM-DD'),
    receiveQty: 0,
    receiveTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    receiverName: currentUserName.value,
    remark: '',
    sourceWarehouseCode: '',
    sourceWarehouseName: '',
    transferQty: undefined,
    transferUnit: '包',
    unpackQty: undefined,
    unpackUnit: '个',
  });
}

function handleAuxCategoryChange(value?: string) {
  const category = auxCategoryOptions.find((item) => item.value === value);
  if (!category) return;
  form.auxCategory = category.value;
  form.auxCategoryName = category.label;
  form.materialCode = category.materialCode;
  form.materialName = category.label;
}

function ledgerBizTypeText(value?: string) {
  return ledgerBizTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function buildQuery() {
  return {
    batchNo: query.batchNo.trim() || undefined,
    materialCode: query.materialCode.trim() || undefined,
    materialName: query.materialName.trim() || undefined,
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    receiveDate: query.receiveDate,
    stockStatus: query.stockStatus,
  };
}

async function fetchRows() {
  loading.value = true;
  try {
    const result = await getPackageAuxStockPage(buildQuery());
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  fetchRows();
}

function handleReset() {
  query.batchNo = '';
  query.materialCode = '';
  query.materialName = '';
  query.receiveDate = undefined;
  query.stockStatus = undefined;
  pageNo.value = 1;
  fetchRows();
}

function handlePageChange(current: number, size: number) {
  pageNo.value = current;
  pageSize.value = size;
  fetchRows();
}

function openCreate() {
  resetForm();
  createVisible.value = true;
}

async function fetchLedgerRows() {
  ledgerLoading.value = true;
  try {
    const result = await getPackageAuxTodayList(ledgerQuery.recordDate);
    ledgerRows.value = ledgerQuery.bizType
      ? result.filter((item) => item.bizType === ledgerQuery.bizType)
      : result;
  } finally {
    ledgerLoading.value = false;
  }
}

function openLedger() {
  ledgerVisible.value = true;
  void fetchLedgerRows();
}

async function submitCreate() {
  if (!form.materialCode.trim()) {
    message.warning('请填写辅材料号');
    return;
  }
  if (!form.materialName.trim()) {
    message.warning('请填写辅材名称');
    return;
  }
  if (!form.batchNo.trim()) {
    message.warning('请填写辅材批次');
    return;
  }
  if (Number(form.receiveQty || 0) <= 0) {
    message.warning('请填写大于0的入边库数量');
    return;
  }
  saving.value = true;
  try {
    await savePackageAuxStock({
      ...form,
      batchNo: form.batchNo.trim(),
      materialCode: form.materialCode.trim(),
      materialName: form.materialName.trim(),
      receiveQty: Number(form.receiveQty || 0),
      unpackQty: Number(form.unpackQty || form.receiveQty || 0),
    });
    message.success('包装辅材边库批次已登记');
    createVisible.value = false;
    fetchRows();
  } finally {
    saving.value = false;
  }
}

function openPrint(row: PackageAuxStock) {
  qrRow.value = row;
  qrValue.value = row.batchNo || '';
  printVisible.value = true;
}

async function submitPrint() {
  if (!qrRow.value?.id) {
    return;
  }
  await nextTick();
  const row = await printPackageAuxStock({ id: qrRow.value.id, operatorName: currentUserName.value });
  const qrImage = qrCodeDataUrl.value || '';
  const popup = window.open('', '_blank', 'width=520,height=620');
  if (!popup) {
    message.warning('浏览器阻止了打印窗口');
    return;
  }
  popup.document.write(`
    <html>
      <head>
        <title>${row.batchNo || ''} 辅材二维码</title>
        <style>
          body { margin: 0; padding: 24px; font-family: Arial, "Microsoft YaHei", sans-serif; color: #111827; }
          .label { width: 360px; border: 1px solid #111827; padding: 18px; }
          .title { margin-bottom: 14px; font-size: 18px; font-weight: 800; text-align: center; }
          .body { display: flex; gap: 16px; align-items: center; }
          img { width: 140px; height: 140px; }
          .kv { font-size: 13px; line-height: 1.8; }
          .batch { font-size: 15px; font-weight: 800; }
        </style>
      </head>
      <body>
        <div class="label">
          <div class="title">包装辅材边库二维码</div>
          <div class="body">
            <img src="${qrImage}" />
            <div class="kv">
              <div>辅材：${row.materialName || '-'}</div>
              <div>料号：${row.materialCode || '-'}</div>
              <div class="batch">批次：${row.batchNo || '-'}</div>
              <div>可用：${formatQty(row.availableQty)} ${row.unpackUnit || '个'}</div>
              <div>边库：${row.edgeWarehouseName || '-'}</div>
            </div>
          </div>
        </div>
      </body>
    </html>
  `);
  popup.document.close();
  setTimeout(() => {
    popup.focus();
    popup.print();
  }, 200);
  printVisible.value = false;
  fetchRows();
}

onMounted(fetchRows);
</script>

<template>
  <Page auto-content-height>
    <div class="aux-stock-page">
      <section class="aux-head">
        <div class="aux-title">
          <span><IconifyIcon icon="lucide:package-plus" /></span>
          <div>
            <h2>库存耗材边库管理</h2>
            <p>统一管理包装袋、隔离膜、纸盒、纸板的批次库存与领用追溯</p>
          </div>
        </div>
        <div class="aux-head-actions">
          <Button @click="openLedger">
            <template #icon><IconifyIcon icon="lucide:clipboard-list" /></template>
            领用台账
          </Button>
          <Button type="primary" @click="openCreate">
            <template #icon><IconifyIcon icon="lucide:plus" /></template>
            新增批次
          </Button>
        </div>
      </section>

      <section class="aux-filter">
        <Input v-model:value="query.materialCode" allow-clear placeholder="辅材料号" />
        <Input v-model:value="query.materialName" allow-clear placeholder="辅材名称" />
        <Input v-model:value="query.batchNo" allow-clear placeholder="辅材批次" />
        <DatePicker v-model:value="query.receiveDate" value-format="YYYY-MM-DD" placeholder="领料日期" />
        <Select v-model:value="query.stockStatus" :options="statusOptions" allow-clear placeholder="库存状态" />
        <Button type="primary" @click="handleSearch">
          <template #icon><IconifyIcon icon="lucide:search" /></template>
          查询
        </Button>
        <Button @click="handleReset">
          <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
          重置
        </Button>
      </section>

      <section class="aux-table">
        <ATable
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="false"
          bordered
          row-key="id"
          size="small"
          :scroll="{ x: 1820, y: 'calc(100vh - 310px)' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="['receiveQty', 'usedQty', 'availableQty'].includes(String(column.dataIndex))">
              {{ formatQty(record[column.dataIndex]) }}
            </template>
            <Tag v-else-if="column.dataIndex === 'stockStatus'" :color="statusColor(record.stockStatus)">
              {{ statusText(record.stockStatus) }}
            </Tag>
            <Button v-else-if="column.dataIndex === 'action'" size="small" type="link" @click="openPrint(record)">
              二维码
            </Button>
          </template>
        </ATable>
        <div class="aux-pagination">
          <Pagination
            :current="pageNo"
            :page-size="pageSize"
            :show-total="(count: number) => `共 ${count} 条`"
            :total="total"
            show-size-changer
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </section>

      <Modal
        v-model:open="createVisible"
        :confirm-loading="saving"
        title="新增包装辅材批次"
        width="920px"
        @ok="submitCreate"
      >
        <div class="aux-form">
          <div class="aux-form-item">
            <label>耗材类别</label>
            <Select
              v-model:value="form.auxCategory"
              :options="auxCategoryOptions"
              @change="handleAuxCategoryChange"
            />
          </div>
          <div class="aux-form-item">
            <label>辅材料号</label>
            <Input v-model:value="form.materialCode" />
          </div>
          <div class="aux-form-item">
            <label>辅材名称</label>
            <Input v-model:value="form.materialName" />
          </div>
          <div class="aux-form-item">
            <label>规格/型号</label>
            <Input v-model:value="form.auxSpec" />
          </div>
          <div class="aux-form-item">
            <label>辅材批次</label>
            <Input v-model:value="form.batchNo" placeholder="可扫码录入" />
          </div>
          <div class="aux-form-item">
            <label>边库</label>
            <Input v-model:value="form.edgeWarehouseName" />
          </div>
          <div class="aux-form-item">
            <label>入边库数量</label>
            <InputNumber v-model:value="form.receiveQty" :min="0" :precision="3" class="w-full" />
          </div>
          <div class="aux-form-item">
            <label>ERP移库单号</label>
            <Input v-model:value="form.erpTransferNo" />
          </div>
          <div class="aux-form-item">
            <label>移库数量</label>
            <InputNumber v-model:value="form.transferQty" :min="0" :precision="3" class="w-full" />
          </div>
          <div class="aux-form-item">
            <label>移库单位</label>
            <Input v-model:value="form.transferUnit" />
          </div>
          <div class="aux-form-item">
            <label>拆包量</label>
            <InputNumber v-model:value="form.unpackQty" :min="0" :precision="3" class="w-full" />
          </div>
          <div class="aux-form-item">
            <label>拆包单位</label>
            <Input v-model:value="form.unpackUnit" />
          </div>
          <div class="aux-form-item">
            <label>领料日期</label>
            <DatePicker v-model:value="form.receiveDate" value-format="YYYY-MM-DD" class="w-full" />
          </div>
          <div class="aux-form-item">
            <label>领料人</label>
            <Input v-model:value="form.receiverName" />
          </div>
          <div class="aux-form-item aux-form-item--full">
            <label>备注</label>
            <Textarea v-model:value="form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" />
          </div>
        </div>
      </Modal>

      <Modal v-model:open="printVisible" title="包装辅材二维码" width="480px" @ok="submitPrint">
        <div v-if="qrRow" class="qr-preview">
          <img :src="qrCodeDataUrl" alt="辅材批次二维码" />
          <strong>{{ qrRow.batchNo }}</strong>
          <span>{{ qrRow.materialName }} / 可用 {{ formatQty(qrRow.availableQty) }} {{ qrRow.unpackUnit || '个' }}</span>
        </div>
      </Modal>

      <Modal v-model:open="ledgerVisible" footer="" title="包装辅材领用台账" width="1260px">
        <div class="aux-ledger-filter">
          <DatePicker
            v-model:value="ledgerQuery.recordDate"
            value-format="YYYY-MM-DD"
            @change="fetchLedgerRows"
          />
          <Select
            v-model:value="ledgerQuery.bizType"
            allow-clear
            :options="ledgerBizTypeOptions"
            placeholder="领用环节"
            @change="fetchLedgerRows"
          />
          <Button type="primary" @click="fetchLedgerRows">查询</Button>
        </div>
        <ATable
          bordered
          :columns="ledgerColumns"
          :data-source="ledgerRows"
          :loading="ledgerLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ x: 1450, y: 460 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="['consumeQty', 'beforeAvailableQty', 'afterAvailableQty'].includes(String(column.dataIndex))">
              {{ formatQty(record[column.dataIndex]) }}
            </template>
            <template v-else-if="column.dataIndex === 'bizType'">
              {{ ledgerBizTypeText(record.bizType) }}
            </template>
          </template>
        </ATable>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.aux-stock-page {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
  color: #1f2937;
}

.aux-head,
.aux-filter,
.aux-table {
  border: 1px solid #d7dee8;
  border-radius: 6px;
  background: #fff;
}

.aux-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
}

.aux-head-actions {
  display: flex;
  gap: 8px;
}

.aux-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.aux-title > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 6px;
  color: #155e75;
  background: #e0f2fe;
}

.aux-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
}

.aux-title p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.aux-filter {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr)) 92px 92px;
  gap: 8px;
  padding: 12px;
}

.aux-table {
  min-height: 0;
  flex: 1;
  padding: 10px;
}

.aux-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}

.aux-ledger-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.aux-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.aux-form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.aux-form-item--full {
  grid-column: 1 / -1;
}

.aux-form-item label {
  color: #4b5563;
  font-size: 13px;
}

.qr-preview {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}

.qr-preview img {
  width: 180px;
  height: 180px;
}

@media (max-width: 1180px) {
  .aux-filter,
  .aux-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
