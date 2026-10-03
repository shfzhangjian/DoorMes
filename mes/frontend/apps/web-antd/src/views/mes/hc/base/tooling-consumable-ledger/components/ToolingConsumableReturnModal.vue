<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { ref } from 'vue';

import { Modal as AModal, Textarea, message } from 'ant-design-vue';

import { returnToolingConsumableLedger } from '#/api/mes/hc/tooling-consumable-ledger';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

defineOptions({ name: 'MesHcToolingConsumableReturnModal' });

type ReturnTarget = MesHcToolingConsumableLedgerApi.Ledger;

const emit = defineEmits<{
  success: [payload: { ledgerId: number; target: ReturnTarget }];
}>();

const returnModalVisible = ref(false);
const returnAuthVisible = ref(false);
const returnSubmitting = ref(false);
const returnTarget = ref<ReturnTarget>();
const returnReason = ref('');

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '操作失败，请稍后重试';
}

function resolveAuthUserId(userInfo: any) {
  const id = userInfo?.userId ?? userInfo?.id ?? userInfo?.empId;
  return id === undefined || id === null || id === '' ? undefined : Number(id);
}

function resolveAuthUserName(userInfo: any) {
  const name = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || '';
  return String(name).trim();
}

function resetReturnForm() {
  returnAuthVisible.value = false;
  returnTarget.value = undefined;
  returnReason.value = '';
}

function open(row: ReturnTarget) {
  if (row.usageStatus !== 'ACTIVE') {
    message.info('仅使用中的边库耗材台账可以退库');
    return;
  }
  if (!row.id) {
    message.warning('当前耗材批次缺少领用台账ID');
    return;
  }
  returnTarget.value = row;
  returnReason.value = '';
  returnModalVisible.value = true;
}

function requestReturnAuth() {
  if (returnSubmitting.value) {
    return;
  }
  if (!returnTarget.value?.id) {
    message.warning('请先选择需要退库的领用台账');
    return;
  }
  if (!returnReason.value.trim()) {
    message.warning('请填写退库原因');
    return;
  }
  returnAuthVisible.value = true;
}

async function submitReturn(userInfo: any) {
  const target = returnTarget.value;
  const authUserName = resolveAuthUserName(userInfo);
  if (!target?.id) {
    message.warning('请先选择需要退库的领用台账');
    return;
  }
  if (!authUserName) {
    message.warning('请先完成用户认证');
    return;
  }
  returnSubmitting.value = true;
  try {
    await returnToolingConsumableLedger({
      id: target.id,
      returnAuthUserId: resolveAuthUserId(userInfo),
      returnAuthUserName: authUserName,
      returnReason: returnReason.value.trim(),
    });
    returnModalVisible.value = false;
    message.success('退库登记成功');
    emit('success', { ledgerId: target.id, target });
    resetReturnForm();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    returnSubmitting.value = false;
  }
}

async function handleReturnAuthSuccess(userInfo: any) {
  await submitReturn(userInfo);
}

defineExpose({ open, reset: resetReturnForm });
</script>

<template>
  <AuthModal
    v-model:visible="returnAuthVisible"
    action-name="边库耗材退库确认"
    auth-mode="username"
    title="退库用户认证"
    @success="handleReturnAuthSuccess"
  />
  <AModal
    v-model:open="returnModalVisible"
    :confirm-loading="returnSubmitting"
    :mask-closable="!returnSubmitting"
    cancel-text="取消"
    ok-text="确认退库"
    title="边库耗材退库"
    width="520px"
    @cancel="resetReturnForm"
    @ok="requestReturnAuth"
  >
    <div class="space-y-4">
      <div class="text-sm text-gray-500">
        批次：{{ returnTarget?.batchNo || '-' }}
      </div>
      <div class="text-sm text-gray-500">
        本次退库量：{{ returnTarget?.balanceQty ?? 0 }} {{ returnTarget?.uomName || returnTarget?.uomCode || returnTarget?.uom || '' }}
      </div>
      <div class="text-sm text-amber-600">
        退库登记仅关闭边库台账，不回补中心仓库存；退库量以服务端实时余额为准。
      </div>
      <div class="space-y-1">
        <div class="text-sm font-medium">退库原因</div>
        <Textarea
          v-model:value="returnReason"
          :maxlength="500"
          :rows="4"
          placeholder="请输入退库原因"
          show-count
        />
      </div>
    </div>
  </AModal>
</template>
