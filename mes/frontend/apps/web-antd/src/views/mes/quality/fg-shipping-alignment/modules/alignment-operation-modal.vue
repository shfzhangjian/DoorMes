<script lang="ts" setup>
import type { MesFgShippingAlignmentApi } from '#/api/mes/quality/fg-shipping-alignment';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Table as ATable,
  Button,
  message,
  Select,
  Spin,
  Tag,
} from 'ant-design-vue';

import {
  getFgShippingAlignment,
  saveFgShippingAlignment,
} from '#/api/mes/quality/fg-shipping-alignment';

type DraftRow = MesFgShippingAlignmentApi.AlignmentRow;

defineOptions({ name: 'FgShippingAlignmentOperationModal' });

type OperationModalData = {
  onSuccess?: () => Promise<void> | void;
  shippingNoticeId?: number;
};

const loading = ref(false);
const saving = ref(false);
const detail = ref<MesFgShippingAlignmentApi.Detail>();
const draftRows = ref<DraftRow[]>([]);
let successCallback: OperationModalData['onSuccess'];

const alignmentColumns = [
  { dataIndex: 'customerProductBatchNo', title: '客户产品批号', width: 180 },
  { dataIndex: 'packageSliceNo', title: '计划包装片号', width: 170 },
  { dataIndex: 'modelCode', title: '内部型号', width: 150 },
  { dataIndex: 'internalItemCode', title: '内部编号', width: 160 },
  {
    dataIndex: 'shippingPickItemId',
    title: '已审核合格的实际片号',
    width: 340,
  },
  { dataIndex: 'fqcNo', title: '来源检验单', width: 180 },
  { dataIndex: 'alignmentStatus', title: '对齐状态', width: 120 },
  { dataIndex: 'mismatchReason', title: '异常说明', minWidth: 200 },
];

const alignedDraftCount = computed(
  () => draftRows.value.filter((item) => item.shippingPickItemId).length,
);

const [Modal, modalApi] = useVbenModal({
  class: 'fg-shipping-alignment-operation-modal',
  closeOnClickModal: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  title: '发货客户批号对齐操作',
  onOpenChange: async (isOpen) => {
    if (!isOpen) {
      detail.value = undefined;
      draftRows.value = [];
      return;
    }
    const modalData = modalApi.getData<OperationModalData>() || {};
    const shippingNoticeId = Number(modalData.shippingNoticeId);
    successCallback = modalData.onSuccess;
    if (!Number.isFinite(shippingNoticeId) || shippingNoticeId <= 0) {
      message.error('未获取到发货通知单，不能进行对齐');
      modalApi.close();
      return;
    }
    await loadDetail(shippingNoticeId);
  },
});

function alignmentTag(status?: MesFgShippingAlignmentApi.AlignmentStatus) {
  if (status === 'ALIGNED') return { color: 'success', label: '已对齐' };
  if (status === 'MISMATCH') return { color: 'error', label: '不匹配' };
  return { color: 'warning', label: '待对齐' };
}

function candidateLabel(
  candidate: MesFgShippingAlignmentApi.AlignmentCandidate,
) {
  return [
    candidate.actualSliceBatchNo || candidate.stockNo || '-',
    candidate.fqcNo,
  ]
    .filter(Boolean)
    .join(' / ');
}

function candidateOptions() {
  return (detail.value?.candidates || []).map((candidate) => ({
    label: candidateLabel(candidate),
    value: candidate.shippingPickItemId,
  }));
}

function applyCandidate(row: DraftRow, shippingPickItemId?: number) {
  const candidate = (detail.value?.candidates || []).find(
    (item) => item.shippingPickItemId === shippingPickItemId,
  );
  row.shippingPickItemId = shippingPickItemId;
  row.shippingDetailId = candidate?.shippingDetailId;
  row.actualSliceBatchNo = candidate?.actualSliceBatchNo;
  row.stockNo = candidate?.stockNo;
  row.sliceBatchNo = candidate?.sliceBatchNo;
  row.fqcId = candidate?.fqcId;
  row.fqcNo = candidate?.fqcNo;
  row.rowJudgment = shippingPickItemId ? 'OK' : 'PENDING';
  row.alignmentStatus = 'PENDING';
  row.mismatchReason = undefined;
}

function handleCandidateChange(row: DraftRow, shippingPickItemId?: number) {
  const previousPickItemId = row.shippingPickItemId;
  const occupiedRow = draftRows.value.find(
    (item) =>
      item.id !== row.id && item.shippingPickItemId === shippingPickItemId,
  );
  applyCandidate(row, shippingPickItemId);
  if (!occupiedRow) return;
  applyCandidate(occupiedRow, previousPickItemId);
  message.info(
    previousPickItemId
      ? '已交换两条客户批号的实际片号，请确认后保存'
      : '已将实际片号改配到当前客户批号，原对齐行已解除',
  );
}

async function loadDetail(shippingNoticeId: number) {
  loading.value = true;
  try {
    detail.value = await getFgShippingAlignment(shippingNoticeId);
    draftRows.value = (detail.value.rows || []).map((item) => ({ ...item }));
  } finally {
    loading.value = false;
  }
}

async function saveAlignment() {
  if (!detail.value) return;
  if (draftRows.value.length === 0) {
    message.warning('当前发货通知单没有客户批号明细');
    return;
  }
  saving.value = true;
  try {
    detail.value = await saveFgShippingAlignment({
      shippingNoticeId: detail.value.shippingNoticeId,
      details: draftRows.value.map((item) => ({
        shippingNoticeItemId: item.shippingNoticeItemId || item.id,
        shippingPickItemId: item.shippingPickItemId ?? null,
      })),
    });
    draftRows.value = (detail.value.rows || []).map((item) => ({ ...item }));
    await successCallback?.();
    message.success('对齐关系已保存；清空、换配和交换关系均已生效');
  } finally {
    saving.value = false;
  }
}

function closeModal() {
  modalApi.close();
}
</script>

<template>
  <Modal>
    <Spin :spinning="loading">
      <div class="flex h-screen min-h-0 flex-col bg-slate-100">
        <header
          class="min-h-18 flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-white px-5 py-3"
        >
          <div class="flex items-center gap-3">
            <Button @click="closeModal">返回列表</Button>
            <div>
              <div class="text-lg font-semibold text-slate-800">
                发货客户批号对齐操作
              </div>
              <div class="mt-1 text-sm text-slate-500">
                {{ detail?.shippingNoticeNo || '-' }} /
                {{ detail?.customerName || '-' }} /
                {{ detail?.erpOrderNo || '-' }}
              </div>
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <Tag :color="detail?.alignmentCompleted ? 'success' : 'warning'">
              {{
                detail?.alignmentCompleted
                  ? '全部对齐，可进入包装'
                  : '可分批保存，未全部对齐前禁止包装'
              }}
            </Tag>
            <Button :loading="saving" type="primary" @click="saveAlignment">
              保存当前对齐
            </Button>
          </div>
        </header>

        <main class="min-h-0 flex-1 overflow-auto p-5">
          <section class="rounded-lg bg-white p-4 shadow-sm">
            <div
              class="mb-4 flex flex-wrap gap-x-8 gap-y-2 text-sm text-slate-600"
            >
              <span>计划片数：{{ detail?.plannedCount || 0 }}</span>
              <span>已审核合格片：{{ detail?.candidateCount || 0 }}</span>
              <span>
                当前已保存对齐：{{ alignedDraftCount }}/{{
                  detail?.plannedCount || 0
                }}
              </span>
              <span>
                说明：可分批保存；可清空已对齐行，或直接选择已占用片号完成互换。
              </span>
            </div>
            <ATable
              :columns="alignmentColumns"
              :data-source="draftRows"
              :loading="loading"
              :pagination="false"
              :row-key="(record: DraftRow) => record.id"
              :scroll="{ x: 1500 }"
              size="middle"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'shippingPickItemId'">
                  <Select
                    :allow-clear="true"
                    :options="candidateOptions()"
                    :value="record.shippingPickItemId"
                    class="w-full"
                    placeholder="选择已审核合格的实际片号"
                    show-search
                    @change="(value) => handleCandidateChange(record, value)"
                  />
                  <div class="mt-1 text-xs text-slate-500">
                    {{
                      record.actualSliceBatchNo || '尚未对齐，可等待后续补配片'
                    }}
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'alignmentStatus'">
                  <Tag :color="alignmentTag(record.alignmentStatus).color">
                    {{ alignmentTag(record.alignmentStatus).label }}
                  </Tag>
                </template>
                <template v-else-if="column.dataIndex === 'mismatchReason'">
                  <span class="text-red-600">{{
                    record.mismatchReason || '-'
                  }}</span>
                </template>
              </template>
            </ATable>
          </section>
        </main>
      </div>
    </Spin>
  </Modal>
</template>

<style>
.fg-shipping-alignment-operation-modal [class*='modal__header'],
.fg-shipping-alignment-operation-modal .ant-modal-header {
  display: none !important;
}

.fg-shipping-alignment-operation-modal [class*='modal__body'],
.fg-shipping-alignment-operation-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
}

.fg-shipping-alignment-operation-modal [class*='modal__content'],
.fg-shipping-alignment-operation-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}

.fg-shipping-alignment-operation-modal .ant-spin-nested-loading,
.fg-shipping-alignment-operation-modal .ant-spin-container {
  height: 100%;
  min-height: 0;
}
</style>
