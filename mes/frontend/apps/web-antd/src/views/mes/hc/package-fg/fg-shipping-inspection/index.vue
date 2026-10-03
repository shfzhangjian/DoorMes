<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { Button, Input, message, Modal, Radio, Tabs, TabPane, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  getShippingNotice,
  getShippingNoticePage,
  inspectShippingNotice,
} from '#/api/mes/hc/package-fg/finished-packaging';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgShippingInspection' });

type ShippingNotice = MesHcFinishedPackagingApi.ShippingNotice;
type ShippingNoticeItem = MesHcFinishedPackagingApi.ShippingNoticeItem & { rowNo?: number };
type ShippingNoticePickItem = MesHcFinishedPackagingApi.ShippingNoticePickItem & { rowNo?: number };

const userStore = useUserStore();
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统');

const query = reactive({ keyword: '' });
const notices = ref<ShippingNotice[]>([]);
const noticeTotal = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailMaximized = ref(false);
const activeDetailTab = ref('pick');
const currentNotice = ref<ShippingNotice>();

const scanVisible = ref(false);
const scanLoading = ref(false);
const scanMatchedPick = ref<ShippingNoticePickItem>();
const selectedNoticeItemId = ref<number>();
const scanForm = reactive({
  actualSliceBatchNo: '',
  inspectionResult: 'OK',
  inspectionTime: '',
  inspectorName: '',
  remark: '',
});

const detailItems = computed<ShippingNoticeItem[]>(() =>
  (currentNotice.value?.items || []).map((item, index) => ({ ...item, rowNo: index + 1 })),
);
const pickItems = computed<ShippingNoticePickItem[]>(() =>
  (currentNotice.value?.pickItems || []).map((item, index) => ({ ...item, rowNo: index + 1 })),
);
const pickedQty = computed(() => pickItems.value.filter((item) => item.finishedStockId).length);
const inspectedQty = computed(() => pickItems.value.filter((item) => item.shippingInspectionResult).length);
const canInspect = computed(() => ['SHIP_CONFIRMED', 'INSPECTED'].includes(currentNotice.value?.noticeStatus || ''));

const selectedNoticeItem = computed(() => detailItems.value.find((item) => item.id === selectedNoticeItemId.value));

const noticeColumns = [
  { field: 'noticeNo', fixed: 'left', showOverflow: 'tooltip', title: '需求单号', width: 180 },
  { field: 'productType', slots: { default: 'productType' }, title: '类型', width: 90 },
  { field: 'customerName', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, showOverflow: 'tooltip', title: '客户', width: 150 },
  { field: 'externalProductModel', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, showOverflow: 'tooltip', title: '外部型号', width: 140 },
  { field: 'externalProductCode', formatter: ({ row, cellValue }) => row.productType === 'SAMPLE' ? '' : cellValue, showOverflow: 'tooltip', title: '外部编码', width: 140 },
  { field: 'erpOrderNo', showOverflow: 'tooltip', title: 'ERP订单号', width: 150 },
  { field: 'qty', slots: { default: 'qty' }, title: '配货/要求', width: 130 },
  { field: 'noticeStatus', slots: { default: 'noticeStatus' }, title: '状态', width: 110 },
  { field: 'shippingTime', slots: { default: 'shippingTime' }, title: '发货时间', width: 160 },
  { field: 'recorderName', showOverflow: 'tooltip', title: '记录人', width: 110 },
  { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 110 },
];

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function nowText() {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function productTypeText(value?: string) {
  const map: Record<string, string> = {
    MASS: '量产',
    RND: '研发',
    SAMPLE: '样品',
  };
  return map[value || ''] || value || '-';
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    CLOSED: '已发货完成',
    INSPECTED: '发货检验完成',
    PACKAGED: '发货包装完成',
    PICKED: '已下架配货',
    SHIP_CONFIRMED: '出货已确认',
    SHIPPED: '已发货完成',
    SUBMITTED: '待配货',
  };
  return map[status || ''] || status || '-';
}

function statusColor(status?: string) {
  const map: Record<string, string> = {
    CLOSED: 'green',
    INSPECTED: 'purple',
    PACKAGED: 'cyan',
    PICKED: 'blue',
    SHIP_CONFIRMED: 'geekblue',
    SHIPPED: 'green',
    SUBMITTED: 'gold',
  };
  return map[status || ''] || 'default';
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'green';
  if (value === 'NG') return 'red';
  return 'default';
}

function locationPart(row: ShippingNoticePickItem | undefined, key: 'area' | 'layer' | 'rack' | 'tray') {
  const parts = String(row?.locationCode || '').split('-');
  const map = {
    area: parts[2] || '-',
    layer: parts[1] || '-',
    rack: parts[0] || '-',
    tray: parts[3] || '-',
  };
  return map[key] || '-';
}

function requirementActualSlice(row: ShippingNoticeItem) {
  return selectedNoticeItemId.value === row.id && scanForm.actualSliceBatchNo
    ? scanForm.actualSliceBatchNo
    : row.actualSliceBatchNo || '-';
}

function pickMatchesRequirement(requirement: ShippingNoticeItem, pick?: ShippingNoticePickItem) {
  if (!pick) return false;
  const requiredModel = requirement.internalModelCode || requirement.customerModelCode || requirement.modelCode || '';
  const actualModel = pick.modelCode || pick.internalModelCode || '';
  if (requiredModel && actualModel && requiredModel.toLowerCase() !== actualModel.toLowerCase()) {
    return false;
  }
  const requiredPrefix = requirement.internalItemCode || requirement.customerSliceBatchNo || '';
  const actualSliceBatchNo = pick.actualSliceBatchNo || pick.sliceBatchNo || '';
  return !requiredPrefix || actualSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase());
}

function isRequirementDisabled(row: ShippingNoticeItem) {
  if (!scanMatchedPick.value) return true;
  if (row.actualSliceBatchNo && row.actualSliceBatchNo !== scanForm.actualSliceBatchNo) return true;
  return !pickMatchesRequirement(row, scanMatchedPick.value);
}

function selectRequirement(row: ShippingNoticeItem) {
  if (isRequirementDisabled(row)) return;
  selectedNoticeItemId.value = row.id;
}

function resolveDefaultNoticeItem(pick: ShippingNoticePickItem) {
  const source = detailItems.value.find((item) => item.id === pick.sourceNoticeItemId);
  if (source && !isRequirementDisabledForPick(source, pick)) return source;
  const actualLinked = detailItems.value.find((item) => item.actualSliceBatchNo === scanForm.actualSliceBatchNo);
  if (actualLinked && !isRequirementDisabledForPick(actualLinked, pick)) return actualLinked;
  const matchedEmpty = detailItems.value.find((item) => !item.actualSliceBatchNo && pickMatchesRequirement(item, pick));
  if (matchedEmpty) return matchedEmpty;
  return detailItems.value.find((item) => !item.actualSliceBatchNo);
}

function isRequirementDisabledForPick(row: ShippingNoticeItem, pick: ShippingNoticePickItem) {
  if (row.actualSliceBatchNo && row.actualSliceBatchNo !== scanForm.actualSliceBatchNo) return true;
  return !pickMatchesRequirement(row, pick);
}

async function queryNoticePage(page?: { currentPage?: number; pageSize?: number }) {
  pageNo.value = page?.currentPage || pageNo.value;
  pageSize.value = page?.pageSize || pageSize.value;
  const result = await getShippingNoticePage({
    keyword: query.keyword.trim() || undefined,
    noticeStatus: 'PENDING_SHIPPING_FQC',
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  });
  notices.value = result.list || [];
  noticeTotal.value = Number(result.total || 0);
  return result;
}

async function searchNotices() {
  pageNo.value = 1;
  await gridApi.query();
}

async function refreshDetail() {
  if (!currentNotice.value?.id) return;
  currentNotice.value = await getShippingNotice(currentNotice.value.id);
}

async function openDetail(row: ShippingNotice) {
  detailVisible.value = true;
  detailLoading.value = true;
  detailMaximized.value = false;
  activeDetailTab.value = 'pick';
  try {
    currentNotice.value = await getShippingNotice(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function resetScanForm(actualSliceBatchNo = '') {
  scanMatchedPick.value = undefined;
  selectedNoticeItemId.value = undefined;
  scanForm.actualSliceBatchNo = actualSliceBatchNo;
  scanForm.inspectionResult = 'OK';
  scanForm.inspectorName = currentUserName.value;
  scanForm.inspectionTime = nowText();
  scanForm.remark = '';
}

function openScan(row?: ShippingNoticePickItem) {
  if (!currentNotice.value?.id) {
    message.warning('请先打开发货需求单');
    return;
  }
  if (!canInspect.value) {
    message.warning('当前需求单状态不允许发货检验');
    return;
  }
  resetScanForm(row?.actualSliceBatchNo || row?.sliceBatchNo || '');
  scanVisible.value = true;
  if (scanForm.actualSliceBatchNo) {
    confirmScanCode(false);
  }
}

function confirmScanCode(showSuccess = true) {
  const actualSliceBatchNo = scanForm.actualSliceBatchNo.trim();
  if (!actualSliceBatchNo) {
    message.warning('请扫码或输入实际片号');
    return;
  }
  const matched = pickItems.value.find((item) =>
    [item.actualSliceBatchNo, item.sliceBatchNo].filter(Boolean).some((value) => String(value).trim() === actualSliceBatchNo),
  );
  if (!matched) {
    scanMatchedPick.value = undefined;
    selectedNoticeItemId.value = undefined;
    message.warning(`未在当前发货配货列表找到片号：${actualSliceBatchNo}`);
    return;
  }
  scanForm.actualSliceBatchNo = matched.actualSliceBatchNo || matched.sliceBatchNo || actualSliceBatchNo;
  scanMatchedPick.value = matched;
  selectedNoticeItemId.value = resolveDefaultNoticeItem(matched)?.id;
  if (showSuccess) {
    message.success('已匹配到发货配货片号，请选择客户要求片号并保存');
  }
}

async function submitScan() {
  if (!currentNotice.value?.id) return;
  if (!scanMatchedPick.value) {
    confirmScanCode(false);
  }
  if (!scanMatchedPick.value) return;
  if (!selectedNoticeItem.value) {
    message.warning('请选择客户要求片号行');
    return;
  }
  if (isRequirementDisabled(selectedNoticeItem.value)) {
    message.warning('所选客户要求与扫码片号不匹配，请重新选择');
    return;
  }
  scanLoading.value = true;
  try {
    currentNotice.value = await inspectShippingNotice({
      actualSliceBatchNo: scanForm.actualSliceBatchNo.trim(),
      inspectionResult: scanForm.inspectionResult,
      inspectorName: scanForm.inspectorName || currentUserName.value,
      noticeId: currentNotice.value.id,
      noticeItemId: selectedNoticeItem.value.id,
      remark: scanForm.remark.trim() || undefined,
    });
    scanVisible.value = false;
    activeDetailTab.value = 'plan';
    message.success('发货检验已保存，实际片号已回写发货明细');
    await searchNotices();
  } finally {
    scanLoading.value = false;
  }
}

const [NoticeGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: noticeColumns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryNoticePage(page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<ShippingNotice>,
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console fg-outbound-board fg-shipping-inspection-board">
      <section class="prototype-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:scan-line" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">发货检验</h2>
            <Tag class="console-title-tag" color="purple">扫码确认</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">待检需求单</span>
              <span class="console-meta-value">{{ noticeTotal }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="searchNotices">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar outbound-query-panel">
        <div class="outbound-simple-query">
          <label class="outbound-keyword-label">关键词</label>
          <Input v-model:value="query.keyword" allow-clear placeholder="需求单号 / 客户 / 型号 / 片号" @press-enter="searchNotices" />
          <Button type="primary" @click="searchNotices">查询</Button>
        </div>
      </section>

      <section class="package-fg-grid-panel outbound-grid-panel">
        <NoticeGrid table-title="待发货检验需求单">
          <template #productType="{ row }">
            <Tag>{{ productTypeText(row.productType) }}</Tag>
          </template>
          <template #qty="{ row }">{{ row.lockedQty || 0 }} / {{ row.requiredShipQty || row.noticeQty || 0 }}</template>
          <template #noticeStatus="{ row }"><Tag :color="statusColor(row.noticeStatus)">{{ statusText(row.noticeStatus) }}</Tag></template>
          <template #shippingTime="{ row }">{{ formatDateTime(row.shippingTime) }}</template>
          <template #action="{ row }"><Button size="small" type="link" @click="openDetail(row)">检验</Button></template>
        </NoticeGrid>
      </section>

      <Modal
        v-model:open="detailVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal outbound-work-modal"
        @cancel="detailVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body outbound-detail-body" :class="{ 'is-loading': detailLoading, 'shipping-detail-maximized-body': detailMaximized }">
          <div class="shipping-notice-form-top" :class="{ 'shipping-notice-form-top--compact': detailMaximized }">
            <div class="shipping-notice-form-title-row">
              <span></span>
              <div class="inspection-form-title">发货检验单</div>
              <div class="shipping-notice-form-actions">
                <Button :disabled="!canInspect" size="small" type="primary" @click="openScan()">
                  <template #icon><IconifyIcon icon="lucide:scan-line" /></template>
                  扫码检验
                </Button>
                <Button :loading="detailLoading" size="small" @click="refreshDetail">
                  <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
                  刷新
                </Button>
                <Button size="small" @click="detailVisible = false">关闭</Button>
              </div>
            </div>
            <div class="shipping-form-sections">
              <fieldset class="shipping-form-fieldset">
                <legend>基本信息</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <template v-if="currentNotice?.productType === 'SAMPLE'">
                    <label>样品数量</label>
                    <div class="inspection-form-control">{{ currentNotice?.requiredShipQty || currentNotice?.noticeQty || 0 }}</div>
                  </template>
                  <label>需求单号</label>
                  <div class="inspection-form-control">{{ currentNotice?.noticeNo || '-' }}</div>
                  <label>类型</label>
                  <div class="inspection-form-control">{{ productTypeText(currentNotice?.productType) }}</div>
                  <label>ERP订单号</label>
                  <div class="inspection-form-control">{{ currentNotice?.erpOrderNo || '-' }}</div>
                  <label>发货时间</label>
                  <div class="inspection-form-control">{{ formatDateTime(currentNotice?.shippingTime) }}</div>
                  <label>配货片数</label>
                  <div class="inspection-form-control">{{ pickedQty }}</div>
                  <label>已检片数</label>
                  <div class="inspection-form-control">{{ inspectedQty }}</div>
                  <label>记录人</label>
                  <div class="inspection-form-control">{{ currentNotice?.recorderName || '-' }}</div>
                  <label>状态</label>
                  <div class="inspection-form-control">
                    <Tag :color="statusColor(currentNotice?.noticeStatus)">{{ statusText(currentNotice?.noticeStatus) }}</Tag>
                  </div>
                  <label class="shipping-form-label--full-row">发货备注</label>
                  <div class="inspection-form-control shipping-form-control--full-row">{{ currentNotice?.remark || '-' }}</div>
                </div>
              </fieldset>
              <fieldset v-if="currentNotice?.productType !== 'SAMPLE'" class="shipping-form-fieldset">
                <legend>{{ currentNotice?.productType === 'SAMPLE' ? '样品要求' : '客户要求' }}</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>客户名称</label>
                  <div class="inspection-form-control">{{ currentNotice?.customerName || '-' }}</div>
                  <label>{{ currentNotice?.productType === 'SAMPLE' ? '样品型号' : '产品型号' }}</label>
                  <div class="inspection-form-control">{{ currentNotice?.externalProductModel || '-' }}</div>
                  <label>{{ currentNotice?.productType === 'SAMPLE' ? '样品批号' : '产品编码' }}</label>
                  <div class="inspection-form-control">{{ currentNotice?.productType === 'SAMPLE' ? currentNotice?.requiredBatchNo || '-' : currentNotice?.externalProductCode || '-' }}</div>
                  <label>{{ currentNotice?.productType === 'SAMPLE' ? '样品数量' : '发货数量' }}</label>
                  <div class="inspection-form-control">{{ currentNotice?.requiredShipQty || currentNotice?.noticeQty || 0 }}</div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'">片号范围</label>
                  <div v-if="currentNotice?.productType !== 'SAMPLE'" class="inspection-form-control">{{ currentNotice?.requiredSliceRange || '-' }}</div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'">生产批号</label>
                  <div v-if="currentNotice?.productType !== 'SAMPLE'" class="inspection-form-control">{{ currentNotice?.requiredBatchNo || '-' }}</div>
                  <label v-if="currentNotice?.productType !== 'SAMPLE'">包装要求</label>
                  <div v-if="currentNotice?.productType !== 'SAMPLE'" class="inspection-form-control shipping-form-control--span-2">{{ currentNotice?.packingRequirement || '-' }}</div>
                </div>
              </fieldset>
            </div>
          </div>

          <Tabs v-model:active-key="activeDetailTab" class="shipping-detail-tabs outbound-detail-tabs">
            <template #rightExtra>
              <Button class="shipping-tab-maximize-btn" size="small" @click="detailMaximized = !detailMaximized">
                <template #icon>
                  <IconifyIcon :icon="detailMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
                </template>
                {{ detailMaximized ? '还原' : '最大化' }}
              </Button>
            </template>
            <TabPane key="pick" tab="配货领用">
              <div class="inspection-form-subtitle">
                <span>发货配货列表</span>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable :data="pickItems" :loading="detailLoading" auto-resize border height="100%" row-id="id" show-overflow size="small" stripe>
                  <VxeColumn field="rowNo" fixed="left" title="序号" width="70" />
                  <VxeColumn field="actualSliceBatchNo" fixed="left" title="片号" width="190" />
                  <VxeColumn field="modelCode" fixed="left" title="产品型号" width="130" />
                  <VxeColumn field="materialCode" fixed="left" title="产品料号" width="150" />
                  <VxeColumn field="rackNo" title="货架" width="80">
                    <template #default="{ row }">{{ locationPart(row, 'rack') }}</template>
                  </VxeColumn>
                  <VxeColumn field="layerNo" title="层" width="80">
                    <template #default="{ row }">{{ locationPart(row, 'layer') }}</template>
                  </VxeColumn>
                  <VxeColumn field="areaNo" title="区" width="80">
                    <template #default="{ row }">{{ locationPart(row, 'area') }}</template>
                  </VxeColumn>
                  <VxeColumn field="trayNo" title="托盘" width="90">
                    <template #default="{ row }">{{ locationPart(row, 'tray') }}</template>
                  </VxeColumn>
                  <VxeColumn field="locationCode" title="货位编号" width="150" />
                  <VxeColumn field="packageNo" title="包装编号" width="160" />
                  <VxeColumn field="shippingInspectionResult" title="检验结论" width="110">
                    <template #default="{ row }"><Tag :color="qualityColor(row.shippingInspectionResult)">{{ row.shippingInspectionResult || '-' }}</Tag></template>
                  </VxeColumn>
                  <VxeColumn field="shippingInspectorName" title="检验人" width="110" />
                  <VxeColumn field="shippingInspectionTime" title="检验时间" width="160">
                    <template #default="{ row }">{{ formatDateTime(row.shippingInspectionTime) }}</template>
                  </VxeColumn>
                  <VxeColumn field="shippingInspectionRemark" title="备注" min-width="180" />
                  <VxeColumn field="action" fixed="right" title="操作" width="110" align="center">
                    <template #default="{ row }"><Button size="small" type="link" @click="openScan(row)">扫码检验</Button></template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>

            <TabPane key="plan" :tab="currentNotice?.productType === 'SAMPLE' ? '样品执行明细' : '客户批号表'">
              <div class="inspection-form-subtitle">
                <span>客户要求片号对应表</span>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable :data="detailItems" auto-resize border height="100%" row-id="id" show-overflow size="small" stripe>
                  <VxeColumn field="rowNo" fixed="left" title="序号" width="70" />
                  <VxeColumn field="internalModelCode" title="内部型号" width="140" />
                  <VxeColumn field="internalItemCode" title="内部编号" width="170" />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="customerProductBatchNo" title="产品批号" width="150" />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="packageSliceNo" title="包装片号" width="170" />
                  <VxeColumn field="actualSliceBatchNo" title="实际片号" width="190" />
                  <VxeColumn field="shippingInspectionResult" title="检验结论" width="110">
                    <template #default="{ row }"><Tag :color="qualityColor(row.shippingInspectionResult)">{{ row.shippingInspectionResult || '-' }}</Tag></template>
                  </VxeColumn>
                  <VxeColumn field="shippingInspectorName" title="检验人" width="110" />
                  <VxeColumn field="shippingInspectionTime" title="检验时间" width="160">
                    <template #default="{ row }">{{ formatDateTime(row.shippingInspectionTime) }}</template>
                  </VxeColumn>
                  <VxeColumn field="shippingInspectionRemark" title="备注" min-width="180" />
                </VxeTable>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </Modal>

      <Modal
        v-model:open="scanVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 120px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal outbound-pick-work-modal"
        @cancel="scanVisible = false"
      >
        <div class="report-modal-body inspection-task-modal-body outbound-pick-candidate shipping-scan-body">
          <fieldset class="erp-fieldset report-modal-toolbar inspection-form-sheet">
            <legend>扫码检验</legend>
            <div class="shipping-selector-title-row">
              <div></div>
              <div class="inspection-form-title">扫码检验</div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="scanVisible = false">关闭</Button>
                <Button :loading="scanLoading" size="small" type="primary" @click="submitScan">保存检验</Button>
              </div>
            </div>
            <div class="inspection-form-head shipping-selector-form-head shipping-scan-form-head">
              <label>实际片号</label>
              <div class="inspection-form-control shipping-scan-input-cell">
                <Input v-model:value="scanForm.actualSliceBatchNo" allow-clear autofocus placeholder="扫码或输入片号" @press-enter="confirmScanCode(true)" />
                <Button size="small" type="primary" @click="confirmScanCode(true)">确认片号</Button>
              </div>
              <label>检验结论</label>
              <div class="inspection-form-control">
                <Radio.Group v-model:value="scanForm.inspectionResult">
                  <Radio.Button value="OK">合格</Radio.Button>
                  <Radio.Button value="NG">不合格</Radio.Button>
                </Radio.Group>
              </div>
              <label>检验人</label>
              <div class="inspection-form-control">{{ scanForm.inspectorName }}</div>
              <label>检验时间</label>
              <div class="inspection-form-control">{{ scanForm.inspectionTime }}</div>
              <label class="shipping-form-label--full-row">备注</label>
              <div class="inspection-form-control shipping-form-control--full-row"><Input v-model:value="scanForm.remark" allow-clear placeholder="备注" /></div>
            </div>
          </fieldset>

          <div class="shipping-scan-match-row">
            <div class="shipping-scan-match-card" :class="{ 'is-empty': !scanMatchedPick }">
              <span class="shipping-scan-match-label">配货匹配</span>
              <strong>{{ scanMatchedPick?.actualSliceBatchNo || scanMatchedPick?.sliceBatchNo || '未匹配' }}</strong>
              <em>{{ scanMatchedPick ? `${scanMatchedPick.modelCode || '-'} / ${scanMatchedPick.materialCode || '-'} / ${scanMatchedPick.locationCode || '-'}` : '请先确认片号' }}</em>
            </div>
            <div class="shipping-scan-match-card" :class="{ 'is-empty': !selectedNoticeItem }">
              <span class="shipping-scan-match-label">客户要求</span>
              <strong>{{ selectedNoticeItem?.internalModelCode || '-' }} / {{ selectedNoticeItem?.internalItemCode || '-' }}</strong>
              <em v-if="currentNotice?.productType !== 'SAMPLE'">{{ selectedNoticeItem?.customerProductBatchNo || '-' }} / {{ selectedNoticeItem?.packageSliceNo || '-' }}</em>
            </div>
          </div>

          <div class="inspection-form-subtitle">
            <span>客户要求片号选择</span>
            <em>选择后会把扫码实际片号回写到发货需求单明细</em>
          </div>
          <div class="inspection-task-detail-table shipping-vxe-table outbound-candidate-vxe">
            <VxeTable :data="detailItems" auto-resize border height="100%" row-id="id" show-overflow size="small" stripe>
              <VxeColumn field="select" fixed="left" title="选择" width="80" align="center">
                <template #default="{ row }">
                  <Button
                    :disabled="isRequirementDisabled(row)"
                    :type="selectedNoticeItemId === row.id ? 'primary' : 'default'"
                    size="small"
                    @click="selectRequirement(row)"
                  >
                    {{ selectedNoticeItemId === row.id ? '已选' : isRequirementDisabled(row) ? '不可选' : '选择' }}
                  </Button>
                </template>
              </VxeColumn>
              <VxeColumn field="internalModelCode" fixed="left" title="内部型号" width="140" />
              <VxeColumn field="internalItemCode" fixed="left" title="内部编号" width="170" />
              <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="customerProductBatchNo" title="产品批号" width="150" />
              <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'" field="packageSliceNo" title="包装片号" width="170" />
              <VxeColumn field="actualSliceBatchNo" title="实际片号" width="190">
                <template #default="{ row }">{{ requirementActualSlice(row) }}</template>
              </VxeColumn>
              <VxeColumn field="shippingInspectionResult" title="检验结论" width="110">
                <template #default="{ row }">
                  <Tag :color="selectedNoticeItemId === row.id ? qualityColor(scanForm.inspectionResult) : qualityColor(row.shippingInspectionResult)">
                    {{ selectedNoticeItemId === row.id ? scanForm.inspectionResult : row.shippingInspectionResult || '-' }}
                  </Tag>
                </template>
              </VxeColumn>
              <VxeColumn field="shippingInspectorName" title="检验人" width="110">
                <template #default="{ row }">{{ selectedNoticeItemId === row.id ? scanForm.inspectorName : row.shippingInspectorName || '-' }}</template>
              </VxeColumn>
              <VxeColumn field="shippingInspectionTime" title="检验时间" width="160">
                <template #default="{ row }">{{ selectedNoticeItemId === row.id ? scanForm.inspectionTime : formatDateTime(row.shippingInspectionTime) }}</template>
              </VxeColumn>
            </VxeTable>
          </div>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.fg-outbound-board {
  --panel-border: #d8e0ec;
  --panel-blue: #1677ff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.outbound-query-panel {
  flex: 0 0 auto;
  padding: 8px;
}

.outbound-simple-query {
  display: grid;
  grid-template-columns: 72px minmax(220px, 420px) auto 1fr;
  gap: 8px;
  align-items: center;
}

.outbound-keyword-label {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.outbound-grid-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.outbound-grid-panel :deep(.vben-vxe-grid),
.outbound-grid-panel :deep(.vxe-grid) {
  height: 100%;
}

.outbound-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.outbound-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.outbound-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.outbound-detail-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: calc(100vw - 24px);
  height: calc(100vh - 24px);
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.outbound-detail-body.is-loading {
  cursor: progress;
}

.erp-fieldset {
  min-width: 0;
  padding: 8px;
  margin: 0;
  border: 1px solid var(--panel-border);
}

.erp-fieldset legend {
  padding: 0 8px;
  color: #075985;
  font-size: 14px;
  font-weight: 900;
}

.report-modal-toolbar {
  display: block;
  flex: 0 0 auto;
  min-height: 58px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.inspection-form-sheet {
  display: grid;
  gap: 8px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections {
  display: none;
}

.shipping-notice-form-title-row,
.shipping-selector-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: var(--panel-blue);
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.inspection-form-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.1;
  text-align: center;
}

.inspection-form-head {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #9fb6cd;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #e2e8f0;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.shipping-section-form-head,
.shipping-selector-form-head {
  grid-template-columns: 110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr);
}

.shipping-scan-form-head {
  grid-template-columns: 110px minmax(0, 1.4fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr);
}

.shipping-form-control--span-2 {
  grid-column: span 3;
}

.shipping-form-label--full-row {
  grid-column: 1;
}

.shipping-form-control--full-row {
  grid-column: span 7;
}

.inspection-form-control :deep(.ant-input),
.inspection-form-control :deep(.ant-input-affix-wrapper),
.inspection-form-control :deep(.ant-select),
.inspection-form-control :deep(.ant-select-selector) {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.inspection-form-subtitle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #334155;
  font-size: 12px;
}

.inspection-form-subtitle span {
  font-weight: 900;
}

.inspection-form-subtitle em {
  color: #64748b;
  font-style: normal;
}

.outbound-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0 8px 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.outbound-detail-tabs :deep(.ant-tabs-nav) {
  align-items: center;
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-tab-maximize-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.shipping-detail-maximized-body .outbound-detail-tabs {
  flex: 1 1 auto;
}

.outbound-detail-tabs :deep(.ant-tabs-content-holder),
.outbound-detail-tabs :deep(.ant-tabs-content),
.outbound-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
}

.outbound-detail-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
}

.outbound-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.outbound-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  overflow: hidden;
}

.outbound-detail-table,
.inspection-task-detail-table.shipping-vxe-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.shipping-vxe-table :deep(.vxe-table),
.shipping-vxe-table :deep(.vxe-table--render-wrapper),
.shipping-vxe-table :deep(.vxe-table--main-wrapper),
.shipping-vxe-table :deep(.vxe-table--body-wrapper) {
  min-height: 0;
}

.outbound-pick-candidate {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: calc(100vw - 120px);
  height: calc(100vh - 60px);
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.shipping-scan-input-cell {
  gap: 8px;
}

.shipping-scan-input-cell :deep(.ant-input-affix-wrapper) {
  flex: 1 1 auto;
}

.shipping-scan-match-row {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.shipping-scan-match-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #cbd5e1;
}

.shipping-scan-match-card.is-empty {
  color: #94a3b8;
  background: #f8fafc;
  border-style: dashed;
}

.shipping-scan-match-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.shipping-scan-match-card strong {
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.shipping-scan-match-card em {
  color: #475569;
  overflow: hidden;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .outbound-simple-query,
  .shipping-scan-match-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .shipping-notice-form-title-row,
  .shipping-selector-title-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }

  .shipping-notice-form-actions {
    justify-content: center;
  }
}
</style>
