<script lang="ts" setup>
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Tag } from 'ant-design-vue';

import { resolveCutRoundFqcScan } from '#/api/mes/quality/cut-round-fqc';

defineOptions({ name: 'QmsCutRoundFqcScanResolveModal' });

const props = withDefaults(
  defineProps<{
    currentFqcId?: number;
    open: boolean;
    scene?: string;
  }>(),
  {
    scene: 'LEDGER_TOOLBAR',
  },
);

const emit = defineEmits<{
  'candidate-selected': [candidate: MesCutRoundFqcApi.ScanCandidate];
  resolved: [resp: MesCutRoundFqcApi.ScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesCutRoundFqcApi.ScanResp>();

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

async function submitScan() {
  const code = scanCode.value.trim();
  if (!code) {
    message.warning('请扫描或输入条码');
    return;
  }
  resolving.value = true;
  try {
    const resp = await resolveCutRoundFqcScan({
      clientType: 'PC',
      currentFqcId: props.currentFqcId,
      scanCode: code,
      scanScene: props.scene,
    });
    lastResp.value = resp;
    if (resp.matchResult === 'MATCHED_SINGLE' && resp.record) {
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

function selectCandidate(candidate: MesCutRoundFqcApi.ScanCandidate) {
  emit('candidate-selected', candidate);
  visible.value = false;
  message.success(`已选择片号 ${candidate.productionBatchNo || '-'}，正在打开检验工作台`);
}
</script>

<template>
  <Modal
    v-model:open="visible"
    destroy-on-close
    :footer="null"
    title="扫码填写裁切成品检验"
    width="720px"
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        placeholder="请扫描 FQC 单、报检任务、计划号、父批次或裁切片号"
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

        <div
          v-if="lastResp.matchResult === 'MATCHED_MULTIPLE'"
          class="mt-3 space-y-2"
        >
          <div class="text-xs text-slate-500">
            同一父批次、计划号或报检任务存在多片时，必须选择实际检验片号。
          </div>
          <Button
            v-for="candidate in lastResp.candidates || []"
            :key="`${candidate.fqcId}-${candidate.submissionDetailId || 'order'}`"
            block
            class="!h-auto !py-2 text-left"
            @click="selectCandidate(candidate)"
          >
            <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
              <span class="font-semibold">
                {{ candidate.productionBatchNo || candidate.fqcNo || '-' }}
              </span>
              <span class="text-xs text-slate-500">
                FQC：{{ candidate.fqcNo || '-' }} / {{ candidate.fqcStatus || '-' }}
              </span>
              <span v-if="candidate.parentProductionBatchNo" class="text-xs text-slate-500">
                父批次：{{ candidate.parentProductionBatchNo }}
              </span>
              <span v-if="candidate.planNo" class="text-xs text-slate-500">
                计划：{{ candidate.planNo }}
              </span>
            </div>
          </Button>
        </div>
      </div>
    </div>
  </Modal>
</template>
