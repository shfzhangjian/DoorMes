<script lang="ts" setup>
import type { MesOqcApi } from '#/api/mes/quality/oqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Table, Tag } from 'ant-design-vue';

import {
  createOqcFromShippingNotice,
  getOqcDetail,
  resolveOqcScan,
} from '#/api/mes/quality/oqc';

defineOptions({ name: 'QmsOqcScanResolveModal' });

const props = withDefaults(
  defineProps<{
    currentOqcId?: number;
    open: boolean;
    scene?: MesOqcApi.OqcScanReq['scanScene'];
  }>(),
  {
    scene: 'LEDGER_TOOLBAR',
  },
);

const emit = defineEmits<{
  resolved: [resp: MesOqcApi.OqcScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesOqcApi.OqcScanResp>();

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
  { dataIndex: 'oqcNo', title: 'OQC 单号', width: 150 },
  { dataIndex: 'shippingNo', title: '发货通知单', width: 150 },
  { dataIndex: 'customerName', title: '客户', width: 140 },
  { dataIndex: 'materialCode', title: '物料编码', width: 120 },
  { dataIndex: 'batchNo', title: '批次', width: 130 },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'action', title: '操作', width: 90 },
];

function statusColor(status?: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REJECTED' || status === 'CANCELED') return 'error';
  if (status === 'WAITING_QA') return 'purple';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

function statusLabel(status?: string) {
  if (status === 'COMPLETED') return '已完成';
  if (status === 'REJECTED') return '已拦截';
  if (status === 'CANCELED') return '已取消';
  if (status === 'WAITING_QA') return '待审核';
  if (status === 'INSPECTING') return '检验中';
  if (status === 'PENDING') return '待检';
  if (status === 'SUSPENDED') return '已挂起';
  return status || '待生成';
}

function matchResultLabel(matchResult?: MesOqcApi.OqcScanResp['matchResult']) {
  if (matchResult === 'CREATED') return '已新建';
  if (matchResult === 'MATCHED_MULTIPLE') return '匹配多条';
  if (matchResult === 'MATCHED_SINGLE') return '匹配单条';
  if (matchResult === 'NOT_FOUND') return '未找到';
  if (matchResult === 'STATUS_BLOCKED') return '状态受限';
  return '-';
}

function shouldEmit(resp: MesOqcApi.OqcScanResp) {
  return (
    resp.matchResult === 'MATCHED_SINGLE' ||
    resp.matchResult === 'CREATED' ||
    resp.openTarget === 'REPORT' ||
    resp.openTarget === 'ENTRY'
  );
}

async function submitScan() {
  const code = scanCode.value.trim();
  if (!code) {
    message.warning('请扫描或输入条码');
    return;
  }
  resolving.value = true;
  try {
    const resp = await resolveOqcScan({
      clientType: 'PC',
      currentOqcId: props.currentOqcId,
      scanCode: code,
      scanScene: props.scene,
    });
    lastResp.value = resp;
    if (shouldEmit(resp) && resp.record?.id) {
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

async function selectCandidate(candidate: MesOqcApi.OqcScanCandidate) {
  let record: MesOqcApi.OqcRecord | undefined;
  if (candidate.id) {
    record = await getOqcDetail(candidate.id);
  } else if (candidate.shippingNoticeItemId) {
    record = await createOqcFromShippingNotice(candidate.shippingNoticeItemId);
  }
  if (!record) {
    return;
  }
  emit('resolved', {
    ...(lastResp.value as MesOqcApi.OqcScanResp),
    matchedOqcId: record.id,
    matchedOqcNo: record.oqcNo,
    openTarget: ['COMPLETED', 'REJECTED', 'CANCELED'].includes(record.status)
      ? 'REPORT'
      : 'ENTRY',
    record,
  });
  visible.value = false;
}
</script>

<template>
  <Modal
    v-model:open="visible"
    title="扫码填写 OQC 出货检验单"
    width="780px"
    :footer="null"
    destroy-on-close
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        placeholder="请扫描或输入 OQC 单号、发货通知单、物料、批次"
        @press-enter="submitScan"
      >
        <template #prefix>
          <IconifyIcon icon="lucide:scan-line" class="text-slate-400" />
        </template>
        <template #addonAfter>
          <Button type="link" :loading="resolving" @click="submitScan">
            解析
          </Button>
        </template>
      </Input>

      <div v-if="lastResp" class="rounded border bg-slate-50 p-3 text-sm">
        <div class="flex items-center justify-between gap-2">
          <span class="font-bold text-slate-700">{{ lastResp.message }}</span>
          <Tag class="!m-0">{{ matchResultLabel(lastResp.matchResult) }}</Tag>
        </div>
      </div>

      <Table
        v-if="lastResp?.matchResult === 'MATCHED_MULTIPLE'"
        bordered
        :columns="candidateColumns"
        :data-source="lastResp.candidates || []"
        :pagination="false"
        :row-key="
          (record) =>
            record.id || `shipping-${record.shippingNoticeItemId || record.shippingNo}`
        "
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <Tag :color="statusColor(record.status)" class="!m-0">
              {{ statusLabel(record.status) }}
            </Tag>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <Button size="small" type="link" @click="selectCandidate(record)">
              进入
            </Button>
          </template>
        </template>
      </Table>
    </div>
  </Modal>
</template>
