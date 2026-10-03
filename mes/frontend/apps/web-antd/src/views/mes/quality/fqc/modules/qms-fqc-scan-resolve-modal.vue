<script lang="ts" setup>
import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Table, Tag } from 'ant-design-vue';

import { getFqcDetail, resolveFqcScan } from '#/api/mes/quality/fqc';

import { formatReportQty } from '../data';

defineOptions({ name: 'QmsFqcScanResolveModal' });

const props = withDefaults(
  defineProps<{
    currentFqcId?: number;
    open: boolean;
    scene?: MesFqcApi.FqcScanReq['scanScene'];
  }>(),
  {
    scene: 'LEDGER_TOOLBAR',
  },
);

const emit = defineEmits<{
  resolved: [resp: MesFqcApi.FqcScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesFqcApi.FqcScanResp>();

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

const candidateColumns = [
  { dataIndex: 'fqcNo', title: 'FQC单号', width: 150 },
  { dataIndex: 'workOrderNo', title: '工单号', width: 130 },
  { dataIndex: 'productModel', title: '产品型号', width: 120 },
  { dataIndex: 'productBatchNo', title: '产品批次', width: 130 },
  {
    key: 'produceQty',
    title: '报检数量',
    width: 110,
    customRender: ({ record }: { record: MesFqcApi.FqcRecord }) =>
      formatReportQty(record),
  },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'currentStepCode', title: '当前步骤', width: 120 },
  { dataIndex: 'action', title: '操作', width: 100 },
];

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
    const resp = await resolveFqcScan({
      clientType: 'PC',
      currentFqcId: props.currentFqcId,
      scanCode: code,
      scanScene: props.scene,
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
  const record = await getFqcDetail(candidate.id);
  emit('resolved', {
    ...(lastResp.value as MesFqcApi.FqcScanResp),
    matchedFqcId: record.id,
    matchedFqcNo: record.fqcNo,
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
    title="扫码填写成品质检表"
    width="760px"
    :footer="null"
    destroy-on-close
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        placeholder="请扫描 Fqc 单、工单、批次或检验项编码"
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
