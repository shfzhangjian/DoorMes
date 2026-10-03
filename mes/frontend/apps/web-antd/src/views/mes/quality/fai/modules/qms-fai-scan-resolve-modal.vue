<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Table, Tag } from 'ant-design-vue';

import { getFaiDetail, resolveFaiScan } from '#/api/mes/quality/fai';

import { formatInspectionQty } from '../data';

defineOptions({ name: 'QmsFaiScanResolveModal' });

const props = withDefaults(
  defineProps<{
    currentFaiId?: number;
    orderNoTitle?: string;
    open: boolean;
    placeholder?: string;
    scene?: MesFaiApi.FaiScanReq['scanScene'];
    sourceModule?: string;
    title?: string;
  }>(),
  {
    orderNoTitle: '首检单号',
    placeholder: '请扫描 FAI 单、工单、批次或检验项编码',
    scene: 'LEDGER_TOOLBAR',
    title: '扫码填写首件质检表',
  },
);

const emit = defineEmits<{
  resolved: [resp: MesFaiApi.FaiScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesFaiApi.FaiScanResp>();

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
});

watch(
  () => props.open,
  (open) => {
    if (open) {
      scanCode.value = '';
      lastResp.value = undefined;
    }
  },
);

const candidateColumns = computed(() => [
  { dataIndex: 'faiNo', title: props.orderNoTitle, width: 150 },
  { dataIndex: 'workOrderNo', title: '工单号', width: 130 },
  { dataIndex: 'productModel', title: '产品型号', width: 120 },
  { dataIndex: 'productBatchNo', title: '产品批次', width: 130 },
  {
    key: 'inspectionQty',
    title: '送检数量',
    width: 110,
    customRender: ({ record }: { record: MesFaiApi.FaiRecord }) =>
      formatInspectionQty(record),
  },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'currentStepCode', title: '当前步骤', width: 120 },
  { dataIndex: 'action', title: '操作', width: 100 },
]);

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REJECTED' || status === 'CANCELED') return 'error';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

async function submitScan() {
  const code = scanCode.value.trim();
  if (!code) {
    message.warning('请扫描或输入条码');
    return;
  }
  resolving.value = true;
  try {
    const resp = await resolveFaiScan({
      clientType: 'PC',
      currentFaiId: props.currentFaiId,
      scanCode: code,
      scanScene: props.scene,
      sourceModule: props.sourceModule,
    });
    lastResp.value = resp;
    if (
      resp.matchResult === 'MATCHED_SINGLE' ||
      resp.openTarget === 'REPORT' ||
      resp.openTarget === 'ITEM_MODAL'
    ) {
      emit('resolved', resp);
      visible.value = false;
      message.success(resp.message || '扫码命中成功');
    } else if (resp.message) {
      message.info(resp.message);
    }
  } finally {
    resolving.value = false;
  }
}

async function selectCandidate(candidate: Record<string, any>) {
  if (!candidate.id) return;
  const record = await getFaiDetail(candidate.id);
  emit('resolved', {
    ...(lastResp.value as MesFaiApi.FaiScanResp),
    matchedFaiId: record.id,
    matchedFaiNo: record.faiNo,
    openTarget: ['COMPLETED', 'REJECTED', 'CANCELED'].includes(record.status)
      ? 'REPORT'
      : 'WORKBENCH',
    record,
  });
  visible.value = false;
}
</script>

<template>
  <Modal
    v-model:open="visible"
    :title="props.title"
    width="760px"
    :footer="null"
    destroy-on-close
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        :placeholder="props.placeholder"
        @press-enter="submitScan"
      >
        <template #prefix>
          <IconifyIcon icon="lucide:scan-line" class="text-slate-400" />
        </template>
        <template #addonAfter>
          <Button type="link" :loading="resolving" @click="submitScan"
            >解析</Button
          >
        </template>
      </Input>

      <div v-if="lastResp" class="rounded border bg-slate-50 p-3 text-sm">
        <div class="flex items-center justify-between gap-2">
          <span class="font-bold text-slate-700">{{ lastResp.message }}</span>
          <Tag class="!m-0">{{ lastResp.matchResult }}</Tag>
        </div>
      </div>

      <Table
        v-if="lastResp?.matchResult === 'MATCHED_MULTIPLE'"
        bordered
        :columns="candidateColumns"
        :data-source="lastResp.candidates || []"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <Tag :color="statusColor(record.status)" class="!m-0">{{
              record.status
            }}</Tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <Button size="small" type="link" @click="selectCandidate(record)"
              >进入</Button
            >
          </template>
        </template>
      </Table>
    </div>
  </Modal>
</template>
