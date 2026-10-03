<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Table, Tag } from 'ant-design-vue';

import { getFaiDetail, resolveFaiScan } from '#/api/mes/quality/fai';

import { formatInspectionQty } from '../data';

defineOptions({ name: 'QmsFaiScanEntryBox' });

const props = withDefaults(
  defineProps<{
    currentFaiId?: number;
    scene?: MesFaiApi.FaiScanReq['scanScene'];
  }>(),
  {
    scene: 'WORKBENCH_HEADER',
  },
);

const emit = defineEmits<{
  resolved: [resp: MesFaiApi.FaiScanResp];
}>();

const scanCode = ref('');
const resolving = ref(false);
const candidateOpen = ref(false);
const lastResp = ref<MesFaiApi.FaiScanResp>();

const candidateColumns = [
  { dataIndex: 'faiNo', title: '首检单号', width: 150 },
  { dataIndex: 'workOrderNo', title: '工单号', width: 130 },
  { dataIndex: 'productBatchNo', title: '产品批次', width: 130 },
  {
    key: 'inspectionQty',
    title: '送检数量',
    width: 110,
    customRender: ({ record }: { record: MesFaiApi.FaiRecord }) =>
      formatInspectionQty(record),
  },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'action', title: '操作', width: 90 },
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
    const resp = await resolveFaiScan({
      clientType: 'PC',
      currentFaiId: props.currentFaiId,
      scanCode: code,
      scanScene: props.scene,
    });
    lastResp.value = resp;
    if (resp.matchResult === 'MATCHED_MULTIPLE') {
      candidateOpen.value = true;
    } else {
      emit('resolved', resp);
      if (resp.message) message.info(resp.message);
      scanCode.value = '';
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
  candidateOpen.value = false;
  scanCode.value = '';
}
</script>

<template>
  <div class="flex w-[360px] items-center gap-2">
    <Input
      v-model:value="scanCode"
      allow-clear
      placeholder="扫码定位 FAI/工单/批次/检验项"
      size="small"
      @press-enter="submitScan"
    >
      <template #prefix>
        <IconifyIcon icon="lucide:scan-search" class="text-slate-400" />
      </template>
    </Input>
    <Button size="small" :loading="resolving" @click="submitScan">扫码</Button>

    <Modal
      v-model:open="candidateOpen"
      title="选择命中的首件检验任务"
      width="720px"
      :footer="null"
      destroy-on-close
    >
      <Table
        bordered
        :columns="candidateColumns"
        :data-source="lastResp?.candidates || []"
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
    </Modal>
  </div>
</template>
