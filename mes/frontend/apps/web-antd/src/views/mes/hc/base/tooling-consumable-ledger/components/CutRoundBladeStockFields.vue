<script setup lang="ts">
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Alert, Button, InputNumber } from 'ant-design-vue';
import ConsumableLedgerSwitchModal from './ConsumableLedgerSwitchModal.vue';
import LedgerForm from '../modules/ledger-form.vue';

const ledger = defineModel<MesHcToolingConsumableLedgerApi.Ledger>('ledger');
const quantity = defineModel<number>('quantity', { default: 1 });
defineProps<{ disabled?: boolean }>();
const selector = ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const [ReceiveModal, receiveApi] = useVbenModal({ connectedComponent: LedgerForm, destroyOnClose: true });
function selectLedger() {
  selector.value?.open({ consumableType: 'BLADE', processCode: 'CUT_ROUND', selectionMode: 'DIRECT',
    onlyPositiveBalance: true, title: '选择裁切刀片领用记录' });
}
function receiveBlade() {
  receiveApi.setData({ consumableType: 'BLADE', fixedProcessCode: 'CUT_ROUND', processCode: 'CUT_ROUND' }).open();
}
</script>
<template>
  <div class="space-y-3 mb-4">
    <Alert type="info" show-icon message="更换刀片将扣减所选边库耗材；无可用余量时，请先完成领用。" />
    <div class="flex gap-2">
      <Button :disabled="disabled" @click="selectLedger">选择刀片领用记录</Button>
      <Button :disabled="disabled" @click="receiveBlade">领用刀片</Button>
    </div>
    <div v-if="ledger">型号：{{ ledger.model || '-' }}；领用批号：{{ ledger.batchNo || '-' }}；
      可用余量：{{ ledger.balanceQty ?? 0 }} {{ ledger.uomName || '片' }}</div>
    <div>本次更换数量：<InputNumber v-model:value="quantity" :disabled="disabled" :min="1" :precision="0" /></div>
  </div>
  <ConsumableLedgerSwitchModal ref="selector" @selected="ledger = $event" />
  <ReceiveModal @success="selectLedger" />
</template>
