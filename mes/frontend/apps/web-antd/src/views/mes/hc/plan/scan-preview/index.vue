<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcScanPreviewApi } from '#/api/mes/hc/scan-preview';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useQRCode } from '@vueuse/integrations/useQRCode';
import {
  Button,
  Checkbox,
  DatePicker,
  Empty,
  Input,
  Pagination,
  Select,
  Spin,
  Table,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getScanPreviewPage } from '#/api/mes/hc/scan-preview';

defineOptions({ name: 'MesHcPlanScanPreview' });

type ScanRecord = MesHcScanPreviewApi.Record;

const DATE_FORMAT = 'YYYY-MM-DD';
const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const sourceTypeOptions = [
  { label: '配料报工', value: 'FORMULA_REPORT' },
  { label: '湿法报工', value: 'WET_REPORT' },
  { label: '一次磨皮', value: 'ROUGH_GRINDING_FIRST' },
  { label: '二次磨皮', value: 'ROUGH_GRINDING_SECOND' },
  { label: '粘胶1', value: 'ADHESIVE_REPORT' },
  { label: '分切切片', value: 'SLITTING_SLICE' },
  { label: '压槽报工', value: 'PRESS_SLOT_REPORT' },
  { label: '粘胶2', value: 'ADHESIVE2_REPORT' },
  { label: '裁圆报工', value: 'CUT_ROUND_REPORT' },
  { label: '包装入库', value: 'PACKAGING_REPORT' },
];

const sourceTypeColor: Record<string, string> = {
  ADHESIVE_REPORT: 'orange',
  ADHESIVE2_REPORT: 'volcano',
  CUT_ROUND_REPORT: 'cyan',
  FORMULA_REPORT: 'blue',
  PACKAGING_REPORT: 'purple',
  PRESS_SLOT_REPORT: 'geekblue',
  ROUGH_GRINDING_FIRST: 'green',
  ROUGH_GRINDING_SECOND: 'lime',
  SLITTING_SLICE: 'magenta',
  WET_REPORT: 'processing',
};

const reportStatusMeta: Record<string, { color: string; text: string }> = {
  CANCELED: { color: 'default', text: '已取消' },
  CONFIRMED: { color: 'green', text: '已确认' },
  DRAFT: { color: 'default', text: '草稿' },
  FINISHED: { color: 'green', text: '已完成' },
  PRINTED: { color: 'blue', text: '已打印' },
  SUBMITTED: { color: 'processing', text: '已提交' },
};

const inspectionStatusMeta: Record<string, { color: string; text: string }> = {
  APPROVED: { color: 'green', text: '已判定' },
  COMPLETED: { color: 'green', text: '已完成' },
  CREATED: { color: 'blue', text: '已创建' },
  INSPECTING: { color: 'processing', text: '检验中' },
  PENDING: { color: 'warning', text: '待检验' },
  REJECTED: { color: 'red', text: '不合格' },
};

const filters = reactive({
  keyword: '',
  operationName: '',
  onlyWithInspection: false,
  reportDateEnd: undefined as Dayjs | undefined,
  reportDateStart: undefined as Dayjs | undefined,
  sourceType: undefined as string | undefined,
});

const pager = reactive({
  current: 1,
  pageSize: 20,
});

const loading = ref(false);
const records = ref<ScanRecord[]>([]);
const selectedKey = ref('');
const total = ref(0);

const selectedRecord = computed(() => {
  if (!selectedKey.value) return records.value[0];
  return records.value.find((item) => recordKey(item) === selectedKey.value) || records.value[0];
});

const reportQrValue = computed(() => selectedRecord.value?.reportQrValue || '');
const inspectionQrValue = computed(() => selectedRecord.value?.inspectionQrValue || '');
const reportQrDataUrl = useQRCode(reportQrValue, {
  errorCorrectionLevel: 'M',
  margin: 1,
  width: 260,
});
const inspectionQrDataUrl = useQRCode(inspectionQrValue, {
  errorCorrectionLevel: 'M',
  margin: 1,
  width: 260,
});

const columns: TableColumnsType<ScanRecord> = [
  { dataIndex: 'sourceTypeName', fixed: 'left', key: 'sourceTypeName', title: '报工来源', width: 116 },
  { dataIndex: 'planNo', fixed: 'left', key: 'planNo', title: '计划号', width: 155 },
  { dataIndex: 'operationName', key: 'operationName', title: '工序', width: 110 },
  { dataIndex: 'productionBatchNo', key: 'productionBatchNo', title: '生产批号', width: 178 },
  { dataIndex: 'reportDate', key: 'reportDate', title: '报工日期', width: 112 },
  { dataIndex: 'goodQty', key: 'goodQty', title: '数量', width: 92 },
  { dataIndex: 'reportStatus', key: 'reportStatus', title: '报工状态', width: 96 },
  { dataIndex: 'inspectionNo', key: 'inspectionNo', title: '送检单号', width: 150 },
  { dataIndex: 'recorderName', key: 'recorderName', title: '报工人', width: 100 },
  { dataIndex: 'createTime', key: 'createTime', title: '创建时间', width: 158 },
];

function trimToUndefined(value?: string) {
  const text = String(value || '').trim();
  return text || undefined;
}

function formatDate(value?: string, format = DATE_FORMAT) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format(format) : String(value);
}

function formatQty(record?: ScanRecord) {
  if (!record) return '-';
  const qty = record.goodQty ?? record.reportQty;
  if (qty === null || qty === undefined) return '-';
  return `${qty}${record.reportUom ? ` ${record.reportUom}` : ''}`;
}

function formatMaterial(record?: ScanRecord) {
  if (!record) return '-';
  const code = record.materialCode || '';
  const name = record.materialName || '';
  return [code, name].filter(Boolean).join(' / ') || '-';
}

function formatModel(record?: ScanRecord) {
  if (!record) return '-';
  const code = record.modelCode || '';
  const name = record.modelName || '';
  return [code, name].filter(Boolean).join(' / ') || '-';
}

function statusMeta(status?: string, type: 'inspection' | 'report' = 'report') {
  const value = String(status || '').trim();
  if (!value) return { color: 'default', text: '-' };
  const dict = type === 'inspection' ? inspectionStatusMeta : reportStatusMeta;
  return dict[value] || { color: 'default', text: value };
}

function recordKey(record: ScanRecord) {
  return `${record.sourceType || 'REPORT'}-${record.id || record.operationReportId || record.bizNo || ''}`;
}

function tableRowClass(record: ScanRecord) {
  return selectedRecord.value && recordKey(record) === recordKey(selectedRecord.value)
    ? 'scan-preview-table-row--active'
    : '';
}

function buildCustomRow(record: ScanRecord) {
  return {
    onClick: () => selectRecord(record),
  };
}

function selectRecord(record: ScanRecord) {
  selectedKey.value = recordKey(record);
}

function buildQueryParams(): MesHcScanPreviewApi.PageReqVO {
  return {
    keyword: trimToUndefined(filters.keyword),
    onlyWithInspection: filters.onlyWithInspection || undefined,
    operationName: trimToUndefined(filters.operationName),
    pageNo: pager.current,
    pageSize: pager.pageSize,
    reportDateEnd: filters.reportDateEnd?.format(DATE_FORMAT),
    reportDateStart: filters.reportDateStart?.format(DATE_FORMAT),
    sourceType: filters.sourceType || undefined,
  };
}

async function fetchRecords(resetPage = false) {
  if (resetPage) pager.current = 1;
  loading.value = true;
  try {
    const result = await getScanPreviewPage(buildQueryParams());
    records.value = result?.list || [];
    total.value = Number(result?.total || 0);
    if (!records.value.length) {
      selectedKey.value = '';
      return;
    }
    const exists = records.value.some((item) => recordKey(item) === selectedKey.value);
    if (!exists) selectedKey.value = recordKey(records.value[0]);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  fetchRecords(true);
}

function handleReset() {
  filters.keyword = '';
  filters.operationName = '';
  filters.onlyWithInspection = false;
  filters.reportDateEnd = undefined;
  filters.reportDateStart = undefined;
  filters.sourceType = undefined;
  fetchRecords(true);
}

function handlePageChange(pageNo: number, pageSize: number) {
  pager.current = pageNo;
  pager.pageSize = pageSize;
  fetchRecords();
}

async function copyText(value?: string, label = '二维码内容') {
  if (!value) return;
  await navigator.clipboard.writeText(value);
  message.success(`${label}已复制`);
}

onMounted(() => fetchRecords(true));
</script>

<template>
  <Page auto-content-height>
    <div class="scan-preview-page">
      <section class="scan-preview-filter">
        <div class="scan-preview-filter__item scan-preview-filter__item--keyword">
          <label>关键词</label>
          <Input
            v-model:value="filters.keyword"
            allow-clear
            placeholder="计划号 / 批号 / 单号"
            @press-enter="handleSearch"
          />
        </div>
        <div class="scan-preview-filter__item">
          <label>报工来源</label>
          <Select
            v-model:value="filters.sourceType"
            allow-clear
            :options="sourceTypeOptions"
            placeholder="全部"
          />
        </div>
        <div class="scan-preview-filter__item">
          <label>工序</label>
          <Input v-model:value="filters.operationName" allow-clear placeholder="工序名称" @press-enter="handleSearch" />
        </div>
        <div class="scan-preview-filter__item scan-preview-filter__item--date">
          <label>报工日期</label>
          <div class="scan-preview-filter__date-range">
            <DatePicker v-model:value="filters.reportDateStart" class="scan-preview-filter__date" />
            <span>至</span>
            <DatePicker v-model:value="filters.reportDateEnd" class="scan-preview-filter__date" />
          </div>
        </div>
        <Checkbox v-model:checked="filters.onlyWithInspection" class="scan-preview-filter__checkbox">
          仅送检
        </Checkbox>
        <div class="scan-preview-filter__actions">
          <Tooltip title="查询">
            <Button type="primary" @click="handleSearch">
              <template #icon>
                <IconifyIcon icon="lucide:search" />
              </template>
              查询
            </Button>
          </Tooltip>
          <Tooltip title="重置">
            <Button @click="handleReset">
              <template #icon>
                <IconifyIcon icon="lucide:rotate-ccw" />
              </template>
              重置
            </Button>
          </Tooltip>
          <Tooltip title="刷新">
            <Button :loading="loading" @click="fetchRecords(false)">
              <template #icon>
                <IconifyIcon icon="lucide:refresh-cw" />
              </template>
            </Button>
          </Tooltip>
        </div>
      </section>

      <section class="scan-preview-content">
        <div class="scan-preview-list">
          <div class="scan-preview-section-head">
            <div>
              <strong>报工记录</strong>
              <span>{{ total }} 条</span>
            </div>
          </div>
          <Spin :spinning="loading" class="scan-preview-list__spin">
            <Table
              :columns="columns"
              :custom-row="buildCustomRow"
              :data-source="records"
              :pagination="false"
              :row-class-name="tableRowClass"
              :row-key="recordKey"
              :scroll="{ x: 1260, y: 'calc(100vh - 344px)' }"
              bordered
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'sourceTypeName'">
                  <Tag :color="sourceTypeColor[record.sourceType] || 'default'">
                    {{ record.sourceTypeName || record.sourceType || '-' }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'reportDate'">
                  {{ formatDate(record.reportDate) }}
                </template>
                <template v-else-if="column.key === 'goodQty'">
                  {{ formatQty(record) }}
                </template>
                <template v-else-if="column.key === 'reportStatus'">
                  <Tag :color="statusMeta(record.reportStatus).color">
                    {{ statusMeta(record.reportStatus).text }}
                  </Tag>
                </template>
                <template v-else-if="column.key === 'inspectionNo'">
                  <Tag v-if="record.inspectionNo" color="blue">{{ record.inspectionNo }}</Tag>
                  <span v-else class="scan-preview-muted">-</span>
                </template>
                <template v-else-if="column.key === 'createTime'">
                  {{ formatDate(record.createTime, DATETIME_FORMAT) }}
                </template>
              </template>
            </Table>
            <div class="scan-preview-pager">
              <Pagination
                :current="pager.current"
                :page-size="pager.pageSize"
                :page-size-options="['20', '50', '100', '200']"
                :total="total"
                show-less-items
                show-size-changer
                size="small"
                @change="handlePageChange"
                @show-size-change="handlePageChange"
              />
            </div>
          </Spin>
        </div>

        <aside class="scan-preview-detail">
          <template v-if="selectedRecord">
            <div class="scan-preview-detail__head">
              <div>
                <Tag :color="sourceTypeColor[selectedRecord.sourceType || ''] || 'default'">
                  {{ selectedRecord.sourceTypeName || selectedRecord.sourceType || '-' }}
                </Tag>
                <strong>{{ selectedRecord.planNo || selectedRecord.bizNo || '-' }}</strong>
              </div>
              <span>{{ selectedRecord.operationName || '-' }}</span>
            </div>

            <div class="scan-preview-ticket scan-preview-ticket--report">
              <header class="scan-preview-ticket__head">
                <div>
                  <IconifyIcon icon="lucide:file-text" />
                  <strong>报工流转单</strong>
                </div>
                <Tag :color="statusMeta(selectedRecord.reportStatus).color">
                  {{ statusMeta(selectedRecord.reportStatus).text }}
                </Tag>
              </header>
              <div class="scan-preview-qr">
                <img v-if="reportQrValue" :src="reportQrDataUrl" alt="报工二维码" />
                <Empty v-else description="暂无二维码" />
              </div>
              <div class="scan-preview-qr__value">
                <span>{{ reportQrValue || '-' }}</span>
                <Button size="small" type="link" :disabled="!reportQrValue" @click="copyText(reportQrValue, '报工二维码内容')">
                  <template #icon>
                    <IconifyIcon icon="lucide:copy" />
                  </template>
                  复制
                </Button>
              </div>
              <div class="scan-preview-ticket__grid">
                <span>计划号</span><strong>{{ selectedRecord.planNo || '-' }}</strong>
                <span>工序</span><strong>{{ selectedRecord.operationName || '-' }}</strong>
                <span>生产批号</span><strong>{{ selectedRecord.productionBatchNo || selectedRecord.sourceBatchNo || '-' }}</strong>
                <span>报工单号</span><strong>{{ selectedRecord.bizNo || '-' }}</strong>
                <span>物料</span><strong>{{ formatMaterial(selectedRecord) }}</strong>
                <span>型号</span><strong>{{ formatModel(selectedRecord) }}</strong>
                <span>数量</span><strong>{{ formatQty(selectedRecord) }}</strong>
                <span>报工日期</span><strong>{{ formatDate(selectedRecord.reportDate) }}</strong>
                <span>开始时间</span><strong>{{ formatDate(selectedRecord.startTime, DATETIME_FORMAT) }}</strong>
                <span>结束时间</span><strong>{{ formatDate(selectedRecord.endTime, DATETIME_FORMAT) }}</strong>
                <span>报工人</span><strong>{{ selectedRecord.recorderName || '-' }}</strong>
                <span>确认人</span><strong>{{ selectedRecord.confirmerName || '-' }}</strong>
              </div>
            </div>

            <div
              :class="[
                'scan-preview-ticket',
                'scan-preview-ticket--inspection',
                { 'scan-preview-ticket--empty': !inspectionQrValue },
              ]"
            >
              <header class="scan-preview-ticket__head">
                <div>
                  <IconifyIcon icon="lucide:clipboard-check" />
                  <strong>送检流转单</strong>
                </div>
                <Tag :color="statusMeta(selectedRecord.inspectionStatus, 'inspection').color">
                  {{ statusMeta(selectedRecord.inspectionStatus, 'inspection').text }}
                </Tag>
              </header>
              <div class="scan-preview-qr">
                <img v-if="inspectionQrValue" :src="inspectionQrDataUrl" alt="送检二维码" />
                <Empty v-else description="暂无送检单" />
              </div>
              <div class="scan-preview-qr__value">
                <span>{{ inspectionQrValue || '-' }}</span>
                <Button
                  size="small"
                  type="link"
                  :disabled="!inspectionQrValue"
                  @click="copyText(inspectionQrValue, '送检二维码内容')"
                >
                  <template #icon>
                    <IconifyIcon icon="lucide:copy" />
                  </template>
                  复制
                </Button>
              </div>
              <div class="scan-preview-ticket__grid">
                <span>送检单号</span><strong>{{ selectedRecord.inspectionNo || '-' }}</strong>
                <span>检验结果</span><strong>{{ selectedRecord.inspectionResult || '-' }}</strong>
                <span>申请时间</span><strong>{{ formatDate(selectedRecord.inspectionApplyTime, DATETIME_FORMAT) }}</strong>
                <span>回传时间</span><strong>{{ formatDate(selectedRecord.inspectionReturnTime, DATETIME_FORMAT) }}</strong>
                <span>生产批号</span><strong>{{ selectedRecord.productionBatchNo || selectedRecord.sourceBatchNo || '-' }}</strong>
                <span>备注</span><strong>{{ selectedRecord.inspectionRemark || '-' }}</strong>
              </div>
            </div>
          </template>
          <Empty v-else description="暂无报工记录" class="scan-preview-detail__empty" />
        </aside>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.scan-preview-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.scan-preview-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1.35fr) minmax(150px, 0.85fr) minmax(130px, 0.7fr) minmax(260px, 1.25fr) auto auto;
  gap: 10px;
  align-items: end;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.scan-preview-filter__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.scan-preview-filter__item label {
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.scan-preview-filter__date-range,
.scan-preview-filter__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.scan-preview-filter__date {
  min-width: 118px;
}

.scan-preview-filter__checkbox {
  align-self: center;
  white-space: nowrap;
}

.scan-preview-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.scan-preview-list,
.scan-preview-detail {
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.scan-preview-list {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.scan-preview-list__spin {
  min-height: 0;
  overflow: hidden;
}

.scan-preview-list__spin :deep(.ant-spin-container) {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  height: 100%;
  min-height: 0;
}

.scan-preview-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
}

.scan-preview-section-head div {
  display: flex;
  gap: 8px;
  align-items: center;
}

.scan-preview-section-head strong {
  color: #0f172a;
  font-size: 15px;
}

.scan-preview-section-head span {
  color: #64748b;
  font-size: 12px;
}

.scan-preview-pager {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid #edf2f7;
}

.scan-preview-muted {
  color: #94a3b8;
}

.scan-preview-list :deep(.scan-preview-table-row--active td) {
  background: #ecfdf3 !important;
}

.scan-preview-list :deep(.ant-table-row) {
  cursor: pointer;
}

.scan-preview-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  overflow: auto;
}

.scan-preview-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 8px 10px;
  border-bottom: 1px solid #edf2f7;
}

.scan-preview-detail__head > div {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.scan-preview-detail__head strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scan-preview-detail__head span {
  max-width: 120px;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scan-preview-ticket {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 8px;
}

.scan-preview-ticket--inspection {
  border-color: #bbf7d0;
}

.scan-preview-ticket--empty {
  background: #fafafa;
  border-color: #e5e7eb;
}

.scan-preview-ticket__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.scan-preview-ticket__head > div {
  display: flex;
  gap: 6px;
  align-items: center;
  color: #0f172a;
}

.scan-preview-ticket__head svg {
  color: #2563eb;
}

.scan-preview-qr {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 278px;
  padding: 12px;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.scan-preview-qr img {
  width: min(260px, 100%);
  height: auto;
  image-rendering: pixelated;
}

.scan-preview-qr__value {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  margin: 8px 0 10px;
  padding: 6px 8px;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.scan-preview-qr__value span {
  min-width: 0;
}

.scan-preview-ticket__grid {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 6px 10px;
  color: #334155;
  font-size: 13px;
}

.scan-preview-ticket__grid span {
  color: #64748b;
}

.scan-preview-ticket__grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
  font-weight: 600;
}

.scan-preview-detail__empty {
  margin-top: 18vh;
}

@media (max-width: 1280px) {
  .scan-preview-filter {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .scan-preview-filter__actions {
    justify-content: flex-end;
  }

  .scan-preview-content {
    grid-template-columns: minmax(0, 1fr) 390px;
  }
}

@media (max-width: 980px) {
  .scan-preview-page {
    overflow: auto;
  }

  .scan-preview-filter,
  .scan-preview-content {
    grid-template-columns: 1fr;
  }

  .scan-preview-content {
    overflow: visible;
  }

  .scan-preview-detail {
    max-height: none;
  }
}
</style>
