<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Table, Tag } from 'ant-design-vue';

import { getIqcDetail, resolveIqcScan } from '#/api/mes/quality/iqc';

defineOptions({ name: 'QmsIqcScanResolveModal' });

const props = withDefaults(
  defineProps<{
    open: boolean;
    scene?: MesIqcApi.IqcScanReq['scanScene'];
  }>(),
  {
    scene: 'LEDGER_TOOLBAR',
  },
);

const emit = defineEmits<{
  resolved: [resp: MesIqcApi.IqcScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesIqcApi.IqcScanResp>();

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
  { dataIndex: 'iqcNo', title: 'IQC单号', width: 150 },
  { dataIndex: 'receiptNo', title: '收料单号', width: 150 },
  { dataIndex: 'supplierName', title: '供应商', width: 150 },
  { dataIndex: 'materialCode', title: '物料编码', width: 130 },
  { dataIndex: 'batchNo', title: '批号', width: 130 },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'action', title: '操作', width: 90 },
];

function statusColor(status?: string) {
  if (status === 'COMPLETED' || status === 'FINISHED') return 'success';
  if (status === 'REJECTED' || status === 'CANCELED') return 'error';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING' || status === 'SUSPENDED') return 'warning';
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
    const resp = await resolveIqcScan({
      clientType: 'PC',
      scanCode: code,
      scanScene: props.scene,
    });
    lastResp.value = resp;
    if (resp.matchResult === 'MATCHED_SINGLE' || resp.openTarget === 'REPORT') {
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

async function selectCandidate(
  candidate: MesIqcApi.IqcScanCandidate | Record<string, any>,
) {
  if (!candidate.id) return;
  const record = await getIqcDetail(candidate.id);
  emit('resolved', {
    ...(lastResp.value as MesIqcApi.IqcScanResp),
    matchedIqcId: record.id,
    matchedIqcNo: record.iqcNo,
    openTarget: ['COMPLETED', 'FINISHED', 'REJECTED', 'CANCELED'].includes(
      record.status || '',
    )
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
    title="扫码填写进料检验单"
    width="780px"
    :footer="null"
    destroy-on-close
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        placeholder="请扫描 IQC 单、收料单、批号或物料编码"
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
            <Tag :color="statusColor(record.status)" class="!m-0">
              {{ record.status }}
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
