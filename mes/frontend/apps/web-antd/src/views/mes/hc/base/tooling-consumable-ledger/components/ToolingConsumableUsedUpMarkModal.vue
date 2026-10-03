<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, ref } from 'vue';

import dayjs from 'dayjs';

import { DatePicker, InputNumber, Modal as AModal, Textarea, message } from 'ant-design-vue';

import { markToolingConsumableLedgerUsedUp } from '#/api/mes/hc/tooling-consumable-ledger';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

defineOptions({ name: 'MesHcToolingConsumableUsedUpMarkModal' });

type UsedUpMarkTarget = (MesHcToolingConsumableLedgerApi.Balance &
  MesHcToolingConsumableLedgerApi.Ledger) & {
  ledgerId?: number;
};

const emit = defineEmits<{
  success: [payload: { ledgerId: number; target: UsedUpMarkTarget }];
}>();

const usedUpModalVisible = ref(false);
const usedUpAuthVisible = ref(false);
const usedUpSubmitting = ref(false);
const usedUpTarget = ref<UsedUpMarkTarget>();
const usedUpRemainQty = ref<number>();
const usedUpActualDate = ref(dayjs().format('YYYY-MM-DD'));
const usedUpRemark = ref('');
const isAdhesiveCompletion = computed(() => usedUpTarget.value?.consumableType === 'GLUE_BOARD'
  && ['ADHESIVE', 'ADHESIVE1', 'ADHESIVE2'].includes(usedUpTarget.value?.processCode || ''));
const quantityLabel = computed(() => isAdhesiveCompletion.value ? '当前剩余量（米）' : '用完余料量');

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '操作失败，请稍后重试';
}

function resolveTargetLedgerId(target?: UsedUpMarkTarget) {
  const id = target?.id ?? target?.ledgerId;
  return id === undefined || id === null ? undefined : Number(id);
}

function resolveAuthUserId(userInfo: any) {
  const id = userInfo?.userId ?? userInfo?.id ?? userInfo?.empId;
  return id === undefined || id === null || id === '' ? undefined : Number(id);
}

function resolveAuthUserName(userInfo: any) {
  const name = userInfo?.empName || userInfo?.nickname || userInfo?.username || userInfo?.empNo || '';
  return String(name).trim();
}

function resetUsedUpMarkForm() {
  usedUpAuthVisible.value = false;
  usedUpTarget.value = undefined;
  usedUpRemainQty.value = undefined;
  usedUpActualDate.value = dayjs().format('YYYY-MM-DD');
  usedUpRemark.value = '';
}

function open(row: UsedUpMarkTarget) {
  if (row.usageStatus === 'USED_UP') {
    message.info('当前领用台账已标记为已用完');
    return;
  }
  if (!resolveTargetLedgerId(row)) {
    message.warning('当前耗材批次缺少领用台账ID');
    return;
  }
  usedUpTarget.value = row;
  usedUpRemainQty.value = isAdhesiveCompletion.value ? undefined : (row.usedUpRemainQty ?? row.balanceQty);
  usedUpActualDate.value = row.usedUpActualDate || dayjs().format('YYYY-MM-DD');
  usedUpRemark.value = row.usedUpRemark || '';
  usedUpModalVisible.value = true;
}

function requestMarkLedgerUsedUpAuth() {
  if (usedUpSubmitting.value) {
    return;
  }
  const ledgerId = resolveTargetLedgerId(usedUpTarget.value);
  if (!ledgerId) {
    message.warning('请先选择需要标记的领用台账');
    return;
  }
  if (usedUpRemainQty.value === undefined || usedUpRemainQty.value === null) {
    message.warning(`请输入${quantityLabel.value}`);
    return;
  }
  if (Number(usedUpRemainQty.value) < 0) {
    message.warning(`${quantityLabel.value}不能为负数`);
    return;
  }
  if (!isAdhesiveCompletion.value && !usedUpActualDate.value) {
    message.warning('请选择实际消耗日期');
    return;
  }
  usedUpAuthVisible.value = true;
}

async function submitMarkLedgerUsedUp(userInfo: any) {
  const target = usedUpTarget.value;
  const ledgerId = resolveTargetLedgerId(target);
  const authUserName = resolveAuthUserName(userInfo);
  if (!target || !ledgerId) {
    message.warning('请先选择需要标记的领用台账');
    return;
  }
  if (!authUserName) {
    message.warning('请先完成用户认证');
    return;
  }
  usedUpSubmitting.value = true;
  try {
    await markToolingConsumableLedgerUsedUp({
      id: ledgerId,
      usedUpActualDate: isAdhesiveCompletion.value ? undefined : usedUpActualDate.value,
      usedUpAuthUserId: resolveAuthUserId(userInfo),
      usedUpAuthUserName: authUserName,
      balanceQty: isAdhesiveCompletion.value ? Number(usedUpRemainQty.value) : undefined,
      usedUpRemainQty: isAdhesiveCompletion.value ? undefined : Number(usedUpRemainQty.value),
      usedUpRemark: isAdhesiveCompletion.value ? undefined : (usedUpRemark.value?.trim() || undefined),
    });
    usedUpModalVisible.value = false;
    message.success(isAdhesiveCompletion.value ? '标记完成成功' : '用完标记成功');
    emit('success', { ledgerId, target });
    resetUsedUpMarkForm();
  } catch (error) {
    message.error(getErrorMessage(error));
  } finally {
    usedUpSubmitting.value = false;
  }
}

async function handleUsedUpAuthSuccess(userInfo: any) {
  await submitMarkLedgerUsedUp(userInfo);
}

defineExpose({ open, reset: resetUsedUpMarkForm });
</script>

<template>
  <AuthModal
    v-model:visible="usedUpAuthVisible"
    action-name="边库耗材用完标记确认"
    auth-mode="username"
    title="用完标记用户认证"
    @success="handleUsedUpAuthSuccess"
  />
  <AModal
    v-model:open="usedUpModalVisible"
    :confirm-loading="usedUpSubmitting"
    :mask-closable="!usedUpSubmitting"
    cancel-text="取消"
    ok-text="确认标记"
    :title="isAdhesiveCompletion ? '标记完成' : '用完标记'"
    width="520px"
    @cancel="resetUsedUpMarkForm"
    @ok="requestMarkLedgerUsedUpAuth"
  >
    <div class="space-y-4">
      <div class="text-sm text-gray-500">
        批次：{{ usedUpTarget?.batchNo || '-' }}
      </div>
      <div class="space-y-1">
        <div class="text-sm font-medium">{{ quantityLabel }}</div>
        <InputNumber
          v-model:value="usedUpRemainQty"
          :min="0"
          :precision="3"
          class="w-full"
          :placeholder="`请输入${quantityLabel}`"
          :max="isAdhesiveCompletion ? usedUpTarget?.receiveQty : undefined"
        />
      </div>
      <div v-if="!isAdhesiveCompletion" class="space-y-1">
        <div class="text-sm font-medium">实际消耗日期</div>
        <DatePicker
          v-model:value="usedUpActualDate"
          class="w-full"
          value-format="YYYY-MM-DD"
        />
      </div>
      <div v-if="!isAdhesiveCompletion" class="space-y-1">
        <div class="text-sm font-medium">备注</div>
        <Textarea
          v-model:value="usedUpRemark"
          :maxlength="500"
          :rows="3"
          placeholder="请输入备注"
          show-count
        />
      </div>
    </div>
  </AModal>
</template>
