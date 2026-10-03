<script lang="ts" setup>
import type { MesPackagingCoaInspectionApi } from '#/api/mes/hc/execution/coa-inspection';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Input,
  message,
  Modal,
  Pagination,
  Table,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getPackagingCoaInspectionFaiPage,
  submitPackagingCoaInspection,
} from '#/api/mes/hc/execution/coa-inspection';
import {
  getFgInboundWaitSegmentPage,
  getFgInboundWaitSegmentPieceList,
} from '#/api/mes/hc/package-fg/finished-packaging';
import { applyPrintFieldTemplate } from '../report/shared/printFieldTemplate';
import {
  buildInspectionTransferTicketPayload,
  sendTransferTicketToPrintAgent,
} from '../report/shared/workOrderTicketPrint';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesExecutionCoaInspection' });

type InspectionSlice = MesHcFinishedPackagingApi.InspectionSlice;
type InspectionSliceSegment = MesHcFinishedPackagingApi.InspectionSliceSegment;
type CoaFaiRecord = MesPackagingCoaInspectionApi.FaiRecord;

const PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const WAIT_INSPECTION_TAB = 'wait-inspection';
const INSPECTION_RECORD_TAB = 'inspection-record';

const router = useRouter();
const userStore = useUserStore();
const activeTab = ref(WAIT_INSPECTION_TAB);
const keyword = ref('');
const sourceLoading = ref(false);
const recordLoading = ref(false);
const sampleLoading = ref(false);
const submitting = ref(false);
const dialogOpen = ref(false);
const sourceSegments = ref<InspectionSliceSegment[]>([]);
const faiRecords = ref<CoaFaiRecord[]>([]);
const faiRecordTotal = ref(0);
const activeSegment = ref<InspectionSliceSegment>();
const sampleRows = ref<InspectionSlice[]>([]);
const selectedSampleKeys = ref<string[]>([]);
const remark = ref('');
const printingFaiNos = ref<string[]>([]);
const sourcePage = reactive({ pageNo: 1, pageSize: 20, total: 0 });
const recordPage = reactive({ pageNo: 1, pageSize: 20, total: 0 });

const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统',
);
const currentDateText = computed(() => dayjs().format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs().format('HH:mm:ss'));
const sourceSegmentTotal = computed(() => sourcePage.total);
const selectedSamples = computed(() => {
  const keys = new Set(selectedSampleKeys.value);
  return sampleRows.value.filter((item) => keys.has(sampleKey(item)));
});

const sourceColumns = [
  {
    dataIndex: 'segmentBatchNo',
    key: 'segmentBatchNo',
    title: '段批次',
    width: 200,
  },
  {
    dataIndex: 'materialCode',
    key: 'materialCode',
    title: '产品料号',
    width: 150,
  },
  { dataIndex: 'modelCode', key: 'modelCode', title: '型号', width: 140 },
  {
    dataIndex: 'totalPieceCount',
    key: 'totalPieceCount',
    title: '待包装片数',
    width: 110,
  },
  {
    dataIndex: 'ngPieceCount',
    key: 'ngPieceCount',
    title: '不合格片数',
    width: 110,
  },
  { key: 'action', title: '操作', width: 150 },
];

const sampleColumns = [
  { dataIndex: 'sliceBatchNo', key: 'sliceBatchNo', title: '片号', width: 190 },
  { dataIndex: 'sourceType', key: 'sourceType', title: '来源', width: 110 },
  {
    dataIndex: 'inspectionResult',
    key: 'inspectionResult',
    title: 'FQC',
    width: 85,
  },
  {
    dataIndex: 'coaInspectionStatus',
    key: 'coaInspectionStatus',
    title: '最新COA状态',
    width: 130,
  },
  {
    dataIndex: 'coaInspectionResult',
    key: 'coaInspectionResult',
    title: '最新COA判定',
    width: 130,
  },
];

const recordColumns = [
  { dataIndex: 'faiNo', key: 'faiNo', title: '检验单号', width: 190 },
  { key: 'sampleBatchNo', title: 'COA样片', width: 170 },
  {
    dataIndex: 'productBatchNo',
    key: 'productBatchNo',
    title: '段批次',
    width: 180,
  },
  {
    dataIndex: 'inspectionQty',
    key: 'inspectionQty',
    title: '样片数',
    width: 90,
  },
  { dataIndex: 'status', key: 'status', title: 'FAI状态', width: 120 },
  { dataIndex: 'judgment', key: 'judgment', title: '检测判定', width: 110 },
  {
    dataIndex: 'submissionTime',
    key: 'submissionTime',
    title: '送检时间',
    width: 180,
  },
  { key: 'action', title: '操作', width: 190, fixed: 'right' as const },
];

function sampleKey(row: InspectionSlice) {
  return `${row.sourceType}:${sourceRecordId(row) || ''}`;
}

function sourceRecordId(row: InspectionSlice) {
  return row.sourceType === 'MANUAL_HISTORY'
    ? row.sourceManualPieceId
    : row.sourceCutRoundReportId;
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG' || value === 'REJECTED') return 'error';
  return 'processing';
}

function sourceLabel(value?: string) {
  return value === 'MANUAL_HISTORY' ? '历史补录' : '裁切报工';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value;
}

function resolveInspectionNo(record?: CoaFaiRecord | null) {
  return String(record?.faiNo || '').trim();
}

function resolveCoaSampleBatchNo(record?: CoaFaiRecord | null) {
  const explicitSampleBatchNo = String(record?.coaSampleBatchNo || '').trim();
  if (explicitSampleBatchNo) return explicitSampleBatchNo;
  const remark = String(record?.remark || '');
  const sampleMatch = remark.match(/(?:样片|片号)\s*[：:]\s*([^；;，,\s]+)/);
  if (sampleMatch?.[1]) return sampleMatch[1];
  const segmentBatchNo = String(record?.productBatchNo || '').trim();
  const sourceReportNo = String(record?.sourceReportNo || '').trim();
  const sourcePrefix = segmentBatchNo ? `PACKAGING-COA-${segmentBatchNo}-` : '';
  if (sourcePrefix && sourceReportNo.startsWith(sourcePrefix)) {
    const sampleBatchNo = sourceReportNo
      .slice(sourcePrefix.length)
      .replace(/-\d{17}$/, '');
    if (sampleBatchNo) return sampleBatchNo;
  }
  return segmentBatchNo || '-';
}

function formatInspectionStandard(record: CoaFaiRecord) {
  return record.standardNo
    ? `${record.standardNo}${record.standardVersion ? ` / ${record.standardVersion}` : ''}`
    : '-';
}

function isPrinting(record: CoaFaiRecord) {
  return printingFaiNos.value.includes(resolveInspectionNo(record));
}

async function loadSourceSegments(resetPage = false) {
  if (resetPage) sourcePage.pageNo = 1;
  sourceLoading.value = true;
  try {
    const result = await getFgInboundWaitSegmentPage({
      keyword: keyword.value.trim() || undefined,
      packagingQualityStatus: 'NG',
      pageNo: sourcePage.pageNo,
      pageSize: sourcePage.pageSize,
    });
    sourceSegments.value = result.list || [];
    sourcePage.total = result.total ?? 0;
  } finally {
    sourceLoading.value = false;
  }
}

function querySourceSegments() {
  void loadSourceSegments(true);
}

function changeSourcePage(pageNo: number, pageSize: number) {
  sourcePage.pageNo = pageNo;
  sourcePage.pageSize = pageSize;
  void loadSourceSegments();
}

async function loadFaiRecords(resetPage = false) {
  if (resetPage) recordPage.pageNo = 1;
  recordLoading.value = true;
  try {
    const result = await getPackagingCoaInspectionFaiPage({
      pageNo: recordPage.pageNo,
      pageSize: recordPage.pageSize,
    });
    faiRecords.value = result.list || [];
    faiRecordTotal.value = result.total ?? faiRecords.value.length;
    recordPage.total = faiRecordTotal.value;
  } finally {
    recordLoading.value = false;
  }
}

function refreshFaiRecords() {
  void loadFaiRecords(true);
}

function changeRecordPage(pageNo: number, pageSize: number) {
  recordPage.pageNo = pageNo;
  recordPage.pageSize = pageSize;
  void loadFaiRecords();
}

async function refresh() {
  await Promise.all([loadSourceSegments(), loadFaiRecords()]);
}

function openInspectionRecord() {
  activeTab.value = INSPECTION_RECORD_TAB;
  refreshFaiRecords();
}

async function openSampleDialog(segment: InspectionSliceSegment) {
  if (!segment.segmentBatchNo) return;
  activeSegment.value = segment;
  selectedSampleKeys.value = [];
  remark.value = '';
  sampleLoading.value = true;
  dialogOpen.value = true;
  try {
    sampleRows.value = await getFgInboundWaitSegmentPieceList({
      keyword: keyword.value.trim() || undefined,
      packagingQualityStatus: 'NG',
      segmentBatchNo: segment.segmentBatchNo,
    });
  } finally {
    sampleLoading.value = false;
  }
}

function handleSampleSelectionChange(keys: Array<number | string>) {
  selectedSampleKeys.value = keys.map(String);
}

async function loadSubmittedFaiRecords(faiNos: string[]) {
  const results = await Promise.all(
    faiNos.map(async (faiNo) => {
      const result = await getPackagingCoaInspectionFaiPage({
        faiNo,
        pageNo: 1,
        pageSize: 1,
      });
      return (
        result.list?.find((item) => item.faiNo === faiNo) || result.list?.[0]
      );
    }),
  );
  return results.filter((item): item is CoaFaiRecord => Boolean(item));
}

async function buildPackagingCoaInspectionPrintPayload(
  record: CoaFaiRecord,
) {
  const inspectionNo = resolveInspectionNo(record);
  const sampleBatchNo = resolveCoaSampleBatchNo(record);
  const applyTime = formatDateTime(record.submissionTime);
  const materialCode = record.materialCode || '-';
  const modelCode = record.productModel || '-';
  const segmentBatchNo = record.productBatchNo || '-';
  const printContext = {
    applyTime,
    coaSliceNo: sampleBatchNo,
    materialCode,
    modelCode,
    processName: '成品包装',
    sampleQty: record.inspectionQty ?? 1,
    segmentBatchNo,
  };
  const fields = await applyPrintFieldTemplate(
    'PACKAGING_COA_INSPECTION',
    [
      { label: '当前工序', value: printContext.processName },
      { label: '产品型号', value: printContext.modelCode },
      { label: '产品料号', value: printContext.materialCode },
      { label: '段批次', value: printContext.segmentBatchNo },
      { label: 'COA送检片号', value: printContext.coaSliceNo },
      { label: '送检时间', value: printContext.applyTime },
      { label: '送检数量', value: printContext.sampleQty },
    ],
    printContext,
  );
  return buildInspectionTransferTicketPayload({
    applicantName: record.submitterName || currentUserName.value,
    applyTime,
    extraFields: [
      { label: '检验状态', value: record.status || 'PENDING' },
      { label: '检测结论', value: record.judgment || 'PENDING' },
      { label: '检验标准', value: formatInspectionStandard(record) },
      { label: '来源单号', value: record.sourceReportNo || '-' },
      { label: '送检备注', value: record.remark || '-' },
    ],
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType: 'COA送检',
    materialCode,
    modelCode,
    planNo: record.workOrderNo || segmentBatchNo,
    processName: '成品包装',
    productionBatchNo: segmentBatchNo,
    sampleType: 'COA送检片',
    title: 'COA送检单',
  });
}

async function printCoaInspectionTickets(records: CoaFaiRecord[]) {
  const printableRecords = records.filter((record) =>
    Boolean(resolveInspectionNo(record)),
  );
  if (!printableRecords.length) {
    Modal.warning({
      content:
        '当前检验记录还没有检验单号，无法生成 COA 送检单二维码。请先完成送检后再打印。',
      title: '无法打印 COA 送检单',
    });
    return;
  }
  const printingNos = printableRecords.map(resolveInspectionNo);
  printingFaiNos.value = Array.from(
    new Set([...printingFaiNos.value, ...printingNos]),
  );
  try {
    const results = await Promise.allSettled(
      printableRecords.map(async (record) =>
        sendTransferTicketToPrintAgent(
          await buildPackagingCoaInspectionPrintPayload(record),
          PRINT_AGENT_URL,
        ),
      ),
    );
    const acceptedCount = results.filter(
      (item) => item.status === 'fulfilled',
    ).length;
    const failures = results
      .filter(
        (item): item is PromiseRejectedResult => item.status === 'rejected',
      )
      .map((item) =>
        String(item.reason?.message || item.reason || '打印服务未确认接收'),
      );
    if (failures.length > 0) {
      Modal.warning({
        content: acceptedCount
          ? `已发送 ${acceptedCount} 张 COA 送检单；${failures[0]}`
          : `未能连接或确认本机打印服务：${failures[0]}。请先启动工序流转单 Python 打印服务。`,
        title: 'COA送检单打印结果',
      });
      return;
    }
    const firstAccepted = results.find((item) => item.status === 'fulfilled');
    const printerName =
      firstAccepted?.status === 'fulfilled'
        ? firstAccepted.value?.printerName
        : '';
    Modal.success({
      content: `COA送检单已发送到 ${printerName || '配置打印机'}，数量：${acceptedCount}。`,
      okText: '知道了',
      title: '打印COA送检单',
    });
  } finally {
    const completedNos = new Set(printingNos);
    printingFaiNos.value = printingFaiNos.value.filter(
      (faiNo) => !completedNos.has(faiNo),
    );
  }
}

function printCoaInspectionTicket(record: CoaFaiRecord) {
  return printCoaInspectionTickets([record]);
}

function promptPrintSubmittedCoaTickets(records: CoaFaiRecord[]) {
  const inspectionNos = records.map(resolveInspectionNo).filter(Boolean);
  Modal.confirm({
    cancelText: '暂不打印',
    content: `COA送检已提交，共生成 ${inspectionNos.length} 张检验单${inspectionNos.length ? `：${inspectionNos.join('、')}` : ''}。是否立即打印 COA 送检单？`,
    okText: '打印',
    onOk: () => printCoaInspectionTickets(records),
    title: '打印COA送检单',
  });
}

async function submit() {
  const segmentBatchNo = activeSegment.value?.segmentBatchNo;
  if (!segmentBatchNo) return;
  if (selectedSamples.value.length === 0) {
    message.warning('请至少选择一片 COA 样片');
    return;
  }
  const samples = selectedSamples.value
    .map((item) => ({
      sourceRecordId: sourceRecordId(item),
      sourceType: item.sourceType,
    }))
    .filter(
      (item): item is MesPackagingCoaInspectionApi.SubmitSample =>
        Boolean(item.sourceRecordId) &&
        (item.sourceType === 'CUT_ROUND_REPORT' ||
          item.sourceType === 'MANUAL_HISTORY'),
    );
  if (samples.length !== selectedSamples.value.length) {
    message.error('存在无法识别来源的样片，请刷新后重新选择');
    return;
  }
  submitting.value = true;
  try {
    const result = await submitPackagingCoaInspection({
      remark: remark.value.trim() || undefined,
      samples,
      segmentBatchNo,
    });
    const faiNos =
      result.faiNos?.filter(Boolean) ?? (result.faiNo ? [result.faiNo] : []);
    message.success(
      `COA送检已提交，共生成 ${samples.length} 张检验单：${faiNos.join('、') || '-'}`,
    );
    dialogOpen.value = false;
    await refresh();
    try {
      const submittedRecords = await loadSubmittedFaiRecords(faiNos);
      if (submittedRecords.length > 0) {
        promptPrintSubmittedCoaTickets(submittedRecords);
      } else {
        message.warning(
          '送检已提交，但未读取到新建检验单；请在“COA送检记录”中刷新后打印。',
        );
      }
    } catch {
      message.warning(
        '送检已提交，但未读取到新建检验单；请在“COA送检记录”中刷新后打印。',
      );
    }
  } finally {
    submitting.value = false;
  }
}

function goFai(faiNo?: string) {
  router.push({
    path: '/mes/quality/fai',
    query: { faiNo: faiNo || undefined, processCategory: 'ADHESIVE2' },
  });
}

onMounted(() => {
  void refresh();
});
</script>

<template>
  <Page auto-content-height content-class="p-4">
    <div
      class="fg-packaging-workbench package-fg-console coa-inspection-console"
    >
      <section class="prototype-banner">
        <div class="console-main-icon" aria-hidden="true">
          <IconifyIcon icon="lucide:flask-conical" />
        </div>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">COA送检</h2>
            <Tag class="console-title-tag" color="blue">成品包装</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item"
              ><span class="console-meta-label">送检范围</span
              ><span class="console-meta-value">不合格待包装段</span></span
            >
            <span class="console-meta-item"
              ><span class="console-meta-label">送检规则</span
              ><span class="console-meta-sub"
                >每片独立生成一张 FAI 检验单</span
              ></span
            >
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateText }}</div>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="refresh">
            <IconifyIcon icon="lucide:refresh-cw" /><span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="activeTab = WAIT_INSPECTION_TAB"
          >
            <IconifyIcon icon="lucide:flask-conical" /><span>COA送检</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openInspectionRecord"
          >
            <IconifyIcon icon="lucide:clipboard-list" /><span>送检记录</span>
          </button>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="fg-packaging-tabs">
        <TabPane
          :key="WAIT_INSPECTION_TAB"
          :tab="`不合格待包装段(${sourceSegmentTotal})`"
        >
          <div class="fg-tab-panel coa-wait-panel">
            <div class="fg-query-bar">
              <Input
                v-model:value="keyword"
                allow-clear
                placeholder="段批次 / 片号 / 料号 / 型号"
                @press-enter="querySourceSegments"
              />
              <Button
                danger
                type="primary"
                :loading="sourceLoading"
                @click="querySourceSegments"
                ><template #icon><IconifyIcon icon="lucide:search" /></template
                >查询不合格段</Button
              >
            </div>
            <div class="coa-inspection-hint">
              <IconifyIcon icon="lucide:info" /><span
                >送检后样片立即从待包装队列扣除；仅最新 COA 为“完成且 OK”时，FQC
                OK 的剩余片才进入合格待包装段。</span
              >
            </div>
            <section class="package-fg-grid-panel">
              <Table
                class="coa-inspection-table"
                :columns="sourceColumns"
                :data-source="sourceSegments"
                :loading="sourceLoading"
                :pagination="false"
                row-key="segmentBatchNo"
                :scroll="{ y: 'max(180px, calc(100vh - 500px))' }"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'action'">
                    <Button
                      size="small"
                      type="primary"
                      @click="openSampleDialog(record)"
                      ><template #icon
                        ><IconifyIcon icon="lucide:flask-conical" /></template
                      >选择片号送检</Button
                    >
                  </template>
                </template>
              </Table>
            </section>
            <div class="coa-pagination-bar">
              <Pagination
                v-model:current="sourcePage.pageNo"
                v-model:page-size="sourcePage.pageSize"
                :page-size-options="['20', '50', '100']"
                :show-total="(total) => `共 ${total} 个待送检段`"
                :total="sourcePage.total"
                show-quick-jumper
                show-size-changer
                @change="changeSourcePage"
              />
            </div>
          </div>
        </TabPane>

        <TabPane
          :key="INSPECTION_RECORD_TAB"
          :tab="`COA送检记录(${faiRecordTotal})`"
        >
          <div class="fg-tab-panel coa-record-panel">
            <div class="fg-query-bar">
              <div class="coa-record-toolbar-text">
                可对已生成的 FAI 检验单重新打印 COA 送检单，打印不改变检验状态。
              </div>
              <Button
                type="primary"
                :loading="recordLoading"
                @click="refreshFaiRecords"
                ><template #icon
                  ><IconifyIcon icon="lucide:refresh-cw" /></template
                >刷新记录</Button
              >
            </div>
            <section class="package-fg-grid-panel">
              <Table
                class="coa-inspection-table"
                :columns="recordColumns"
                :data-source="faiRecords"
                :loading="recordLoading"
                :pagination="false"
                row-key="id"
                :scroll="{ x: 1250, y: 'max(180px, calc(100vh - 450px))' }"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'sampleBatchNo'"
                    ><strong>{{
                      resolveCoaSampleBatchNo(record)
                    }}</strong></template
                  >
                  <template
                    v-else-if="
                      column.key === 'status' || column.key === 'judgment'
                    "
                    ><Tag :color="qualityColor(record[column.key])">{{
                      record[column.key] || '-'
                    }}</Tag></template
                  >
                  <template v-else-if="column.key === 'submissionTime'">{{
                    formatDateTime(record.submissionTime)
                  }}</template>
                  <template v-else-if="column.key === 'action'">
                    <div class="fg-row-actions">
                      <Button
                        size="small"
                        type="link"
                        :loading="isPrinting(record)"
                        @click="printCoaInspectionTicket(record)"
                        ><template #icon
                          ><IconifyIcon icon="lucide:printer" /></template
                        >打印检验单</Button
                      >
                      <Button
                        size="small"
                        type="link"
                        @click="goFai(record.faiNo)"
                        >去FAI检测</Button
                      >
                    </div>
                  </template>
                </template>
              </Table>
            </section>
            <div class="coa-pagination-bar">
              <Pagination
                v-model:current="recordPage.pageNo"
                v-model:page-size="recordPage.pageSize"
                :page-size-options="['20', '50', '100']"
                :show-total="(total) => `共 ${total} 条送检记录`"
                :total="recordPage.total"
                show-quick-jumper
                show-size-changer
                @change="changeRecordPage"
              />
            </div>
          </div>
        </TabPane>
      </Tabs>

      <Modal
        v-model:open="dialogOpen"
        :confirm-loading="submitting"
        :width="980"
        title="选择 COA 送检样片"
        @ok="submit"
      >
        <div class="coa-sample-modal-hint">
          <span>段批次：</span
          ><strong>{{ activeSegment?.segmentBatchNo || '-' }}</strong
          ><span
            >。可一次选择多片，每片独立生成一张 FAI
            检验单；确认送检后，所选样片不再回到待包装队列。</span
          >
        </div>
        <Table
          :columns="sampleColumns"
          :data-source="sampleRows"
          :loading="sampleLoading"
          :pagination="false"
          :row-selection="{
            selectedRowKeys: selectedSampleKeys,
            onChange: handleSampleSelectionChange,
          }"
          :row-key="sampleKey"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sourceType'">{{
              sourceLabel(record.sourceType)
            }}</template>
            <template
              v-else-if="
                column.key === 'inspectionResult' ||
                column.key === 'coaInspectionResult'
              "
              ><Tag :color="qualityColor(record[column.key])">{{
                record[column.key] || '-'
              }}</Tag></template
            >
            <template v-else-if="column.key === 'coaInspectionStatus'"
              ><Tag :color="qualityColor(record.coaInspectionResult)">{{
                record.coaInspectionStatus || '未送检'
              }}</Tag></template
            >
          </template>
        </Table>
        <Input
          v-model:value="remark"
          class="mt-4"
          :maxlength="500"
          placeholder="送检备注（可选）"
        />
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.coa-inspection-console {
  grid-template-rows: max-content minmax(0, 1fr);
}
.coa-inspection-console .console-main-icon {
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
}
.coa-inspection-hint,
.coa-record-toolbar-text,
.coa-sample-modal-hint {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}
.coa-inspection-hint {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 10px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}
.coa-inspection-hint .iconify {
  flex: 0 0 auto;
  margin-top: 3px;
}
.coa-record-toolbar-text {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 0 4px;
}
.coa-inspection-table :deep(.ant-table-thead > tr > th) {
  color: #263445;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}
.coa-inspection-table :deep(.ant-table-tbody > tr > td) {
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
}
.coa-inspection-table :deep(.ant-table-tbody > tr:hover > td) {
  background: linear-gradient(180deg, #e0f2fe 0%, #dbeafe 100%);
}
.coa-sample-modal-hint {
  padding: 10px 12px;
  margin-bottom: 12px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
}
.fg-packaging-tabs {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.fg-packaging-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}
.fg-packaging-tabs :deep(.ant-tabs-content-holder),
.fg-packaging-tabs :deep(.ant-tabs-content),
.fg-packaging-tabs :deep(.ant-tabs-tabpane) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.fg-packaging-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
}
.fg-packaging-tabs :deep(.ant-tabs-content),
.fg-packaging-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}
.fg-query-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.fg-tab-panel {
  display: grid;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.package-fg-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}
.package-fg-grid-panel :deep(.ant-table-wrapper),
.package-fg-grid-panel :deep(.ant-spin-nested-loading),
.package-fg-grid-panel :deep(.ant-spin-container) {
  width: 100%;
  min-width: 0;
}
.coa-wait-panel {
  grid-template-rows: max-content max-content minmax(0, 1fr) max-content;
}
.coa-record-panel {
  grid-template-rows: max-content minmax(0, 1fr) max-content;
}
.coa-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 40px;
  padding: 6px 8px;
  flex: 0 0 auto;
  background: #eef3f8;
  border: 1px solid #8794a4;
}
.fg-row-actions {
  display: flex;
  gap: 4px;
  white-space: nowrap;
}
@media (max-width: 760px) {
  .coa-inspection-console .console-action-group {
    justify-content: flex-start;
  }
  .fg-query-bar {
    grid-template-columns: 1fr;
  }
}
</style>
