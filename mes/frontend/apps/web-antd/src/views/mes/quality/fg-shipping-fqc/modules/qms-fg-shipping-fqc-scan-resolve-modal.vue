<script lang="ts" setup>
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Tag } from 'ant-design-vue';

import { resolveFgShippingFqcScan } from '#/api/mes/quality/fg-shipping-fqc';

defineOptions({ name: 'QmsFgShippingFqcScanResolveModal' });

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
  resolved: [resp: MesFgShippingFqcApi.ScanResp];
  'update:open': [open: boolean];
}>();

const scanCode = ref('');
const resolving = ref(false);
const lastResp = ref<MesFgShippingFqcApi.ScanResp>();

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
    const resp = await resolveFgShippingFqcScan({
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
</script>

<template>
  <Modal
    v-model:open="visible"
    destroy-on-close
    :footer="null"
    title="扫码填写发货成品检验"
    width="720px"
  >
    <div class="space-y-3">
      <Input
        v-model:value="scanCode"
        allow-clear
        autofocus
        placeholder="请扫描 FQC 单、发货通知单、ERP订单或实际片号"
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
    </div>
  </Modal>
</template>
