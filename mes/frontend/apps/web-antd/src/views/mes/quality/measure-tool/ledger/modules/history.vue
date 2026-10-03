<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Pagination, Spin, Table, Tabs, Tag } from 'ant-design-vue';

import {
  getMeasureToolCalibrationRecordPage,
  getMeasureToolLedgerStatusRecordPage,
  getMeasureToolMsaRecordPage,
} from '#/api/mes/quality/measure-tool';

import {
  CALIBRATION_RESULT_OPTIONS,
  formatCycleMonths,
  MSA_RESULT_OPTIONS,
  optionLabel,
  TOOL_STATUS_OPTIONS,
  WARNING_STATUS_OPTIONS,
} from '../../shared';

import '../../../../resource/device/qms-detail-style.css';

defineOptions({ name: 'QmsMeasureToolLedgerDetailModal' });

const emit = defineEmits<{
  adjustStatus: [ledger: QmsMeasureToolApi.Ledger];
  calibrate: [ledger: QmsMeasureToolApi.Ledger];
  maintainMsa: [ledger: QmsMeasureToolApi.Ledger];
}>();

const { hasAccessByCodes } = useAccess();
const canManageExternalOpen = hasAccessByCodes([
  'mes:qms-measure-tool-ledger:internal-admin',
]);

const ledger = ref<QmsMeasureToolApi.Ledger>();
const loading = ref(false);
const calibrationRows = ref<QmsMeasureToolApi.CalibrationRecord[]>([]);
const msaRows = ref<QmsMeasureToolApi.MsaRecord[]>([]);
const statusRecordRows = ref<QmsMeasureToolApi.StatusRecord[]>([]);
const calibrationPagination = ref({ current: 1, pageSize: 10, total: 0 });
const msaPagination = ref({ current: 1, pageSize: 10, total: 0 });
const statusRecordPagination = ref({ current: 1, pageSize: 10, total: 0 });
const activeHistoryTab = ref('calibration');
const calibrationTableElement = ref<HTMLElement>();
const msaTableElement = ref<HTMLElement>();
const statusRecordTableElement = ref<HTMLElement>();
const calibrationTableHeight = ref(250);
const msaTableHeight = ref(250);
const statusRecordTableHeight = ref(250);
let tableResizeObserver: ResizeObserver | undefined;

const calibrationColumns = [
  { dataIndex: 'recordNo', title: '记录编号', width: 150 },
  { dataIndex: 'calibrationDate', title: '校准日期', width: 110 },
  { dataIndex: 'calibrationMethod', title: '校准方式', width: 120 },
  { dataIndex: 'calibrationOrg', title: '校准机构', width: 170 },
  { dataIndex: 'calibrator', title: '校准人', width: 100 },
  { dataIndex: 'calibrationResult', title: '结果', width: 90 },
  { dataIndex: 'nextCalibrationDate', title: '下次校准', width: 110 },
  { dataIndex: 'missedCount', title: '累计漏检', width: 90 },
  { dataIndex: 'overdueDueDate', title: '最近漏检日期', width: 120 },
  { dataIndex: 'remark', title: '说明', minWidth: 180 },
];
const msaColumns = [
  { dataIndex: 'recordNo', title: '记录编号', width: 150 },
  { dataIndex: 'msaDate', title: '分析日期', width: 110 },
  { dataIndex: 'analyst', title: '分析人', width: 100 },
  { dataIndex: 'msaResult', title: '结果', width: 90 },
  { dataIndex: 'nextMsaDate', title: '下次分析', width: 110 },
  { dataIndex: 'missedCount', title: '累计漏检', width: 90 },
  { dataIndex: 'overdueDueDate', title: '最近漏检日期', width: 120 },
  { dataIndex: 'remark', title: '说明', minWidth: 180 },
];
const statusRecordColumns = [
  { dataIndex: 'previousStatus', title: '调整前状态', width: 120 },
  { dataIndex: 'status', title: '调整后状态', width: 120 },
  { dataIndex: 'handler', title: '处理人', width: 110 },
  { dataIndex: 'handleTime', title: '处理时间', width: 170 },
  { dataIndex: 'oaProcessNo', title: 'OA处理单号', width: 160 },
  { dataIndex: 'handleRemark', title: '处理说明', minWidth: 220 },
  { dataIndex: 'attachments', title: '附件', width: 150 },
];

const reminderSeal = computed(() => {
  const current = ledger.value;
  if (!current) return undefined;
  const overdueItems = [
    current.calibrationStatus === 'OVERDUE' ? '校准' : '',
    current.msaStatus === 'OVERDUE' ? 'MSA' : '',
  ].filter(Boolean);
  const dueItems = [
    current.calibrationStatus === 'DUE_SOON' ? '校准' : '',
    current.msaStatus === 'DUE_SOON' ? 'MSA' : '',
  ].filter(Boolean);
  const dueDates = [
    current.calibrationStatus === 'OVERDUE' ||
    current.calibrationStatus === 'DUE_SOON'
      ? `校准 ${current.nextCalibrationDate || '-'}`
      : '',
    current.msaStatus === 'OVERDUE' || current.msaStatus === 'DUE_SOON'
      ? `MSA ${current.nextMsaDate || '-'}`
      : '',
  ].filter(Boolean);
  if (overdueItems.length > 0) {
    return {
      detail: dueDates.join(' · '),
      text: `${overdueItems.join(' / ')}过期`,
      tone: 'overdue',
    };
  }
  if (dueItems.length > 0) {
    return {
      detail: dueDates.join(' · '),
      text: `${dueItems.join(' / ')}到期`,
      tone: 'due',
    };
  }
  return undefined;
});

async function queryCalibrationHistory() {
  if (!ledger.value?.id) return;
  const calibrationPage = await getMeasureToolCalibrationRecordPage({
    ledgerId: ledger.value.id,
    pageNo: calibrationPagination.value.current,
    pageSize: calibrationPagination.value.pageSize,
  });
  calibrationRows.value = calibrationPage.list || [];
  calibrationPagination.value.total = calibrationPage.total || 0;
}

async function queryMsaHistory() {
  if (!ledger.value?.id) return;
  const msaPage = await getMeasureToolMsaRecordPage({
    ledgerId: ledger.value.id,
    pageNo: msaPagination.value.current,
    pageSize: msaPagination.value.pageSize,
  });
  msaRows.value = msaPage.list || [];
  msaPagination.value.total = msaPage.total || 0;
}

async function queryStatusRecordHistory() {
  if (!ledger.value?.id) return;
  const statusRecordPage = await getMeasureToolLedgerStatusRecordPage({
    ledgerId: ledger.value.id,
    pageNo: statusRecordPagination.value.current,
    pageSize: statusRecordPagination.value.pageSize,
  });
  statusRecordRows.value = statusRecordPage.list || [];
  statusRecordPagination.value.total = statusRecordPage.total || 0;
}

async function loadCalibrationHistory() {
  loading.value = true;
  try {
    await queryCalibrationHistory();
  } finally {
    loading.value = false;
  }
}

async function loadMsaHistory() {
  loading.value = true;
  try {
    await queryMsaHistory();
  } finally {
    loading.value = false;
  }
}

async function loadStatusRecordHistory() {
  loading.value = true;
  try {
    await queryStatusRecordHistory();
  } finally {
    loading.value = false;
  }
}

async function loadHistory() {
  loading.value = true;
  try {
    await Promise.all([
      queryCalibrationHistory(),
      queryMsaHistory(),
      queryStatusRecordHistory(),
    ]);
  } finally {
    loading.value = false;
  }
}

function handleCalibrationPageChange(pageNo: number, pageSize: number) {
  calibrationPagination.value.current = pageNo;
  calibrationPagination.value.pageSize = pageSize;
  void loadCalibrationHistory();
}

function handleMsaPageChange(pageNo: number, pageSize: number) {
  msaPagination.value.current = pageNo;
  msaPagination.value.pageSize = pageSize;
  void loadMsaHistory();
}

function handleStatusRecordPageChange(pageNo: number, pageSize: number) {
  statusRecordPagination.value.current = pageNo;
  statusRecordPagination.value.pageSize = pageSize;
  void loadHistory();
}

function getAttachmentUrls(attachments?: string) {
  return (
    attachments
      ?.split(',')
      .map((item) => item.trim())
      .filter(Boolean) || []
  );
}

function handleAdjustStatus() {
  if (ledger.value) emit('adjustStatus', ledger.value);
}

function handleCalibrate() {
  if (ledger.value) emit('calibrate', ledger.value);
}

function handleMaintainMsa() {
  if (ledger.value) emit('maintainMsa', ledger.value);
}

function formatCell(record: Record<string, any>, field?: string) {
  return field && record[field] ? record[field] : '-';
}

function closeDetail() {
  void modalApi.close();
}

function calculateTableHeight(element?: HTMLElement) {
  if (!element) return 250;
  const headerHeight =
    element.querySelector<HTMLElement>('.ant-table-thead')?.offsetHeight || 42;
  return Math.max(160, Math.floor(element.clientHeight - headerHeight));
}

function refreshTableHeight() {
  calibrationTableHeight.value = calculateTableHeight(
    calibrationTableElement.value,
  );
  msaTableHeight.value = calculateTableHeight(msaTableElement.value);
  statusRecordTableHeight.value = calculateTableHeight(
    statusRecordTableElement.value,
  );
}

function observeTableHeight() {
  tableResizeObserver?.disconnect();
  tableResizeObserver = new ResizeObserver(refreshTableHeight);
  for (const element of [
    calibrationTableElement.value,
    msaTableElement.value,
    statusRecordTableElement.value,
  ]) {
    if (element) tableResizeObserver.observe(element);
  }
  refreshTableHeight();
}

async function syncTableHeight() {
  await nextTick();
  observeTableHeight();
}

const [Modal, modalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal',
  closable: false,
  contentClass: 'overflow-hidden p-0',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      tableResizeObserver?.disconnect();
      ledger.value = undefined;
      calibrationRows.value = [];
      msaRows.value = [];
      statusRecordRows.value = [];
      calibrationPagination.value.total = 0;
      msaPagination.value.total = 0;
      statusRecordPagination.value.total = 0;
      return;
    }
    ledger.value = modalApi.getData<QmsMeasureToolApi.Ledger>();
    calibrationPagination.value.current = 1;
    msaPagination.value.current = 1;
    statusRecordPagination.value.current = 1;
    await loadHistory();
    await syncTableHeight();
  },
});

watch(activeHistoryTab, (tab) => {
  if (tab === 'status') {
    void loadStatusRecordHistory();
  }
  void syncTableHeight();
});

onBeforeUnmount(() => tableResizeObserver?.disconnect());

defineExpose({ reloadHistory: loadHistory });
</script>

<template>
  <Modal>
    <div class="qms-ncr-detail measure-tool-detail">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <IconifyIcon icon="lucide:ruler" class="text-lg text-sky-700" />
          <span class="text-xs font-bold text-slate-600">量检具档案</span>
        </div>

        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">量检具台账详情</div>
          <div class="qms-ncr-title-panel__subtitle">
            <span class="qms-ncr-title-panel__subtitle-item">
              本厂编号：{{ ledger?.toolCode || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              设备名称：{{ ledger?.toolName || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              位置：{{ ledger?.storageLocation || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              区域：{{ ledger?.categoryName || '-' }}
            </span>
          </div>
        </div>

        <div class="qms-ncr-toolbar__actions">
          <Button
            v-access:code="['mes:qms-measure-tool-ledger:update']"
            class="qms-ncr-toolbar-action"
            size="small"
            @click="handleAdjustStatus"
          >
            <IconifyIcon icon="lucide:rotate-cw" class="mr-1" />
            <span>调整状态</span>
          </Button>
          <Button
            v-access:code="['mes:qms-measure-tool-ledger:calibrate']"
            class="qms-ncr-toolbar-action"
            size="small"
            type="primary"
            @click="handleCalibrate"
          >
            <IconifyIcon icon="lucide:clipboard-check" class="mr-1" />
            <span>登记校准</span>
          </Button>
          <Button
            v-access:code="['mes:qms-measure-tool-ledger:calibrate']"
            class="qms-ncr-toolbar-action"
            :disabled="ledger?.msaEnabled !== 1"
            size="small"
            @click="handleMaintainMsa"
          >
            <IconifyIcon icon="lucide:chart-line" class="mr-1" />
            <span>维护MSA分析</span>
          </Button>
          <Button
            class="qms-ncr-toolbar-action"
            size="small"
            @click="closeDetail"
          >
            <IconifyIcon icon="lucide:x" class="mr-1" />
            <span>关闭</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading" class="detail-spin">
        <div class="detail-content">
          <div class="qms-exception-workbench measure-tool-detail__workbench">
            <div class="qms-exception-form measure-tool-detail__form">
              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>一、量检具基础信息</strong>
                    <span class="qms-exception-section-subtitle">
                      台账信息、校准周期、MSA 周期与到期提醒
                    </span>
                  </div>
                  <div
                    v-if="reminderSeal"
                    class="measure-tool-detail__seal"
                    :class="`is-${reminderSeal.tone}`"
                  >
                    <strong>{{ reminderSeal.text }}</strong>
                    <span>{{ reminderSeal.detail }}</span>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>本厂编号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ ledger?.toolCode || '-' }}
                    </div>
                  </div>
                  <label>机身号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ ledger?.bodyNo || '-' }}
                    </div>
                  </div>
                  <label>设备名称</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.toolName || '-' }}
                    </div>
                  </div>

                  <label>位置</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.storageLocation || '-' }}
                    </div>
                  </div>
                  <label>区域</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.categoryName || '-' }}
                    </div>
                  </div>
                  <label>规格型号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.model || '-' }}
                    </div>
                  </div>
                  <label>精度等级</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.accuracy || '-' }}
                    </div>
                  </div>

                  <label>量程</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.measureRange || '-' }}
                    </div>
                  </div>
                  <label>生产厂家</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.manufacturer || '-' }}
                    </div>
                  </div>
                  <label>购入日期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.purchaseDate || '-' }}
                    </div>
                  </div>

                  <label>使用部门</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.usingDepartment || '-' }}
                    </div>
                  </div>
                  <label>保养人</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.maintainerName || '-' }}
                    </div>
                  </div>
                  <label>责任人</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.responsiblePerson || '-' }}
                    </div>
                  </div>
                  <template v-if="canManageExternalOpen">
                    <label>对外开放</label>
                    <div class="erp-form-value">
                      <div class="qms-exception-readonly-value">
                        {{ ledger?.externalOpen === 0 ? '否' : '是' }}
                      </div>
                    </div>
                  </template>

                  <label>设备状态</label>
                  <div class="erp-form-value">
                    <Tag
                      :color="
                        ledger?.status === 'EXPIRED' ? 'error' : 'default'
                      "
                    >
                      {{ optionLabel(TOOL_STATUS_OPTIONS, ledger?.status) }}
                    </Tag>
                  </div>
                  <label>校准周期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formatCycleMonths(ledger?.calibrationCycleMonths) }}
                    </div>
                  </div>
                  <label>下次校准</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.nextCalibrationDate || '-' }}
                    </div>
                  </div>

                  <label>校准提醒</label>
                  <div class="erp-form-value">
                    <Tag
                      :color="
                        ledger?.calibrationStatus === 'OVERDUE'
                          ? 'error'
                          : ledger?.calibrationStatus === 'DUE_SOON'
                            ? 'warning'
                            : 'default'
                      "
                    >
                      {{
                        optionLabel(
                          WARNING_STATUS_OPTIONS,
                          ledger?.calibrationStatus,
                        )
                      }}
                    </Tag>
                  </div>
                  <label>纳入MSA</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ ledger?.msaEnabled === 1 ? '纳入' : '不纳入' }}
                    </div>
                  </div>
                  <label>MSA周期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{
                        ledger?.msaEnabled === 1
                          ? formatCycleMonths(ledger?.msaCycleMonths)
                          : '-'
                      }}
                    </div>
                  </div>

                  <label>下次MSA</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{
                        ledger?.msaEnabled === 1
                          ? ledger?.nextMsaDate || '-'
                          : '-'
                      }}
                    </div>
                  </div>
                  <label>MSA提醒</label>
                  <div class="erp-form-value">
                    <Tag
                      :color="
                        ledger?.msaStatus === 'OVERDUE'
                          ? 'error'
                          : ledger?.msaStatus === 'DUE_SOON'
                            ? 'warning'
                            : 'default'
                      "
                    >
                      {{
                        optionLabel(WARNING_STATUS_OPTIONS, ledger?.msaStatus)
                      }}
                    </Tag>
                  </div>
                  <label class="erp-form-label--tall">备注</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <div
                      class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                    >
                      {{ ledger?.remark || '-' }}
                    </div>
                  </div>
                </div>
              </section>

              <section
                class="erp-basic-form measure-tool-detail__history-panel"
              >
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>二、处理历史</strong>
                    <span class="qms-exception-section-subtitle">
                      校准、MSA分析与状态调整处理记录
                    </span>
                  </div>
                </div>
                <Tabs
                  v-model:active-key="activeHistoryTab"
                  class="qms-exception-tabs measure-tool-detail__tabs"
                  type="card"
                >
                  <Tabs.TabPane key="calibration" tab="校准历史">
                    <div class="measure-tool-detail__tab-pane">
                      <div
                        ref="calibrationTableElement"
                        class="measure-tool-detail__table"
                      >
                        <Table
                          :columns="calibrationColumns"
                          :data-source="calibrationRows"
                          :loading="loading"
                          :pagination="false"
                          row-key="id"
                          size="small"
                          :scroll="{ x: 1080, y: calibrationTableHeight }"
                        >
                          <template #bodyCell="{ column, record }">
                            <Tag
                              v-if="column.dataIndex === 'calibrationResult'"
                              :color="
                                record.calibrationResult === 'QUALIFIED'
                                  ? 'success'
                                  : record.calibrationResult === 'UNQUALIFIED'
                                    ? 'error'
                                    : 'warning'
                              "
                            >
                              {{
                                optionLabel(
                                  CALIBRATION_RESULT_OPTIONS,
                                  record.calibrationResult,
                                )
                              }}
                            </Tag>
                            <Tag
                              v-else-if="
                                column.dataIndex === 'missedCount' &&
                                Number(record.missedCount) > 0
                              "
                              color="error"
                            >
                              {{ record.missedCount }} 次
                            </Tag>
                            <template v-else>
                              {{ formatCell(record, String(column.dataIndex)) }}
                            </template>
                          </template>
                        </Table>
                      </div>
                      <div class="measure-tool-detail__pagination">
                        <Pagination
                          :current="calibrationPagination.current"
                          :page-size="calibrationPagination.pageSize"
                          :show-total="(total) => `共 ${total} 条`"
                          :total="calibrationPagination.total"
                          show-quick-jumper
                          show-size-changer
                          size="small"
                          @change="handleCalibrationPageChange"
                          @show-size-change="handleCalibrationPageChange"
                        />
                      </div>
                    </div>
                  </Tabs.TabPane>
                  <Tabs.TabPane key="msa" tab="MSA分析历史">
                    <div class="measure-tool-detail__tab-pane">
                      <div
                        ref="msaTableElement"
                        class="measure-tool-detail__table"
                      >
                        <Table
                          :columns="msaColumns"
                          :data-source="msaRows"
                          :loading="loading"
                          :pagination="false"
                          row-key="id"
                          size="small"
                          :scroll="{ x: 960, y: msaTableHeight }"
                        >
                          <template #bodyCell="{ column, record }">
                            <Tag
                              v-if="column.dataIndex === 'msaResult'"
                              :color="
                                record.msaResult === 'QUALIFIED'
                                  ? 'success'
                                  : 'error'
                              "
                            >
                              {{
                                optionLabel(
                                  MSA_RESULT_OPTIONS,
                                  record.msaResult,
                                )
                              }}
                            </Tag>
                            <Tag
                              v-else-if="
                                column.dataIndex === 'missedCount' &&
                                Number(record.missedCount) > 0
                              "
                              color="error"
                            >
                              {{ record.missedCount }} 次
                            </Tag>
                            <template v-else>
                              {{ formatCell(record, String(column.dataIndex)) }}
                            </template>
                          </template>
                        </Table>
                      </div>
                      <div class="measure-tool-detail__pagination">
                        <Pagination
                          :current="msaPagination.current"
                          :page-size="msaPagination.pageSize"
                          :show-total="(total) => `共 ${total} 条`"
                          :total="msaPagination.total"
                          show-quick-jumper
                          show-size-changer
                          size="small"
                          @change="handleMsaPageChange"
                          @show-size-change="handleMsaPageChange"
                        />
                      </div>
                    </div>
                  </Tabs.TabPane>
                  <Tabs.TabPane key="status" tab="状态处理记录">
                    <div class="measure-tool-detail__tab-pane">
                      <div
                        ref="statusRecordTableElement"
                        class="measure-tool-detail__table"
                      >
                        <Table
                          :columns="statusRecordColumns"
                          :data-source="statusRecordRows"
                          :loading="loading"
                          :pagination="false"
                          row-key="id"
                          size="small"
                          :scroll="{ x: 1050, y: statusRecordTableHeight }"
                        >
                          <template #bodyCell="{ column, record }">
                            <Tag
                              v-if="
                                column.dataIndex === 'previousStatus' ||
                                column.dataIndex === 'status'
                              "
                            >
                              {{
                                optionLabel(
                                  TOOL_STATUS_OPTIONS,
                                  record[column.dataIndex],
                                )
                              }}
                            </Tag>
                            <div
                              v-else-if="column.dataIndex === 'attachments'"
                              class="measure-tool-detail__attachments"
                            >
                              <a
                                v-for="(url, index) in getAttachmentUrls(
                                  record.attachments,
                                )"
                                :key="url"
                                :href="url"
                                rel="noopener noreferrer"
                                target="_blank"
                              >
                                附件{{ index + 1 }}
                              </a>
                              <span
                                v-if="
                                  getAttachmentUrls(record.attachments)
                                    .length === 0
                                "
                              >
                                -
                              </span>
                            </div>
                            <template v-else>
                              {{ formatCell(record, String(column.dataIndex)) }}
                            </template>
                          </template>
                        </Table>
                      </div>
                      <div class="measure-tool-detail__pagination">
                        <Pagination
                          :current="statusRecordPagination.current"
                          :page-size="statusRecordPagination.pageSize"
                          :show-total="(total) => `共 ${total} 条`"
                          :total="statusRecordPagination.total"
                          show-quick-jumper
                          show-size-changer
                          size="small"
                          @change="handleStatusRecordPageChange"
                          @show-size-change="handleStatusRecordPageChange"
                        />
                      </div>
                    </div>
                  </Tabs.TabPane>
                </Tabs>
              </section>
            </div>
          </div>
        </div>
      </Spin>
    </div>
  </Modal>
</template>

<style scoped>
.measure-tool-detail__workbench {
  display: flex;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
  overflow: hidden;
}

.measure-tool-detail__form {
  display: flex;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
}

.measure-tool-detail__history-panel {
  display: flex;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
  overflow: hidden;
}

.measure-tool-detail__seal {
  display: grid;
  flex: 0 0 auto;
  min-width: 138px;
  gap: 2px;
  padding: 6px 10px;
  border: 2px solid currentColor;
  border-radius: 4px;
  background: transparent;
  opacity: 0.58;
  pointer-events: none;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  transform: rotate(-10deg);
}

.measure-tool-detail__seal strong {
  font-size: 16px;
  font-weight: 700;
}

.measure-tool-detail__seal.is-due {
  color: #d48806;
}

.measure-tool-detail__seal.is-overdue {
  color: #cf1322;
}

.measure-tool-detail__tabs {
  display: flex;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
}

.measure-tool-detail__tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin-bottom: 8px;
}

.measure-tool-detail__tabs :deep(.ant-tabs-content-holder) {
  min-height: 0;
  flex: 1;
}

.measure-tool-detail__tabs :deep(.ant-tabs-content),
.measure-tool-detail__tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.measure-tool-detail__tab-pane {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.measure-tool-detail__table {
  width: 100%;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.measure-tool-detail__table :deep(.ant-table-wrapper),
.measure-tool-detail__table :deep(.ant-spin-container),
.measure-tool-detail__table :deep(.ant-spin-nested-loading) {
  height: 100%;
}

.measure-tool-detail__pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 10px 0;
}

.measure-tool-detail__attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
