<script lang="ts" setup>
import type { MesHcLotRuleApi } from '#/api/mes/hc/lotrule';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Alert, Button, Form, FormItem, Input, InputNumber, message, Space } from 'ant-design-vue';

import {
  adjustLotRuleCounter,
  initializeLotRuleCounter,
  previewNextLotRuleCounter,
} from '#/api/mes/hc/lotrule';

type SequenceMode = 'adjust' | 'initialize';

const emit = defineEmits(['success']);

const formState = reactive({
  counterId: undefined as number | undefined,
  ruleId: undefined as number | undefined,
  ruleCode: 'LOT-CMP-WHITE-MASS',
  counterType: 'ANNUAL_BATCH',
  year: new Date().getFullYear(),
  bizDimensionKey: '',
  counterKey: '',
  resetKey: '',
  oldCurrentSeq: 0,
  currentSeq: 0,
  lastLotNo: '',
  batchLineCode: 'A',
  reason: '',
});

const currentMode = ref<SequenceMode>('initialize');
const previewLoading = ref(false);
const previewResult = ref<MesHcLotRuleApi.LotRuleCounterPreviewResp>();
const isAdjust = computed(() => currentMode.value === 'adjust');
const modalTitle = computed(() => (isAdjust.value ? '人工设置流水' : '初始化年度流水'));

function parseYear(record?: MesHcLotRuleApi.LotRuleCounter) {
  const candidates = [record?.bizDimensionKey, record?.resetKey, record?.counterKey?.split(':').pop()];
  const matched = candidates.find((item) => item && /^\d{4}$/.test(item));
  return matched ? Number(matched) : new Date().getFullYear();
}

function buildBizDate() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${formState.year}-${month}-${day}`;
}

function resetForm() {
  formState.counterId = undefined;
  formState.ruleId = undefined;
  formState.ruleCode = 'LOT-CMP-WHITE-MASS';
  formState.counterType = 'ANNUAL_BATCH';
  formState.year = new Date().getFullYear();
  formState.bizDimensionKey = '';
  formState.counterKey = '';
  formState.resetKey = '';
  formState.oldCurrentSeq = 0;
  formState.currentSeq = 0;
  formState.lastLotNo = '';
  formState.batchLineCode = 'A';
  formState.reason = '';
  previewResult.value = undefined;
}

function validateBase() {
  if (!formState.ruleCode.trim()) {
    message.warning('规则编码不能为空');
    return false;
  }
  if (!formState.year || formState.year < 2000) {
    message.warning('年度不能为空');
    return false;
  }
  if (formState.currentSeq === undefined || formState.currentSeq === null || Number(formState.currentSeq) < 0) {
    message.warning('当前已使用计数不能小于 0');
    return false;
  }
  if (!formState.reason.trim()) {
    message.warning(isAdjust.value ? '调整原因不能为空' : '初始化原因不能为空');
    return false;
  }
  return true;
}

async function handlePreview() {
  if (!formState.ruleCode.trim()) {
    message.warning('规则编码不能为空');
    return;
  }
  previewLoading.value = true;
  try {
    previewResult.value = await previewNextLotRuleCounter({
      ruleId: formState.ruleId,
      ruleCode: formState.ruleCode.trim(),
      counterType: formState.counterType.trim() || 'ANNUAL_BATCH',
      year: Number(formState.year),
      bizDate: buildBizDate(),
      bizDimensionKey: formState.bizDimensionKey.trim() || undefined,
      currentSeq: Number(formState.currentSeq || 0),
      batchLineCode: formState.batchLineCode.trim() || 'A',
    });
  } finally {
    previewLoading.value = false;
  }
}

async function handleSubmit() {
  if (!validateBase()) {
    return false;
  }
  if (isAdjust.value) {
    if (!formState.counterId) {
      message.warning('计数器ID不能为空');
      return false;
    }
    await adjustLotRuleCounter({
      counterId: formState.counterId,
      oldCurrentSeq: Number(formState.oldCurrentSeq || 0),
      newCurrentSeq: Number(formState.currentSeq || 0),
      lastLotNo: formState.lastLotNo.trim() || undefined,
      reason: formState.reason.trim(),
    });
    message.success('人工设置成功');
  } else {
    await initializeLotRuleCounter({
      ruleId: formState.ruleId,
      ruleCode: formState.ruleCode.trim(),
      counterType: formState.counterType.trim() || 'ANNUAL_BATCH',
      year: Number(formState.year),
      bizDimensionKey: formState.bizDimensionKey.trim() || undefined,
      currentSeq: Number(formState.currentSeq || 0),
      lastLotNo: formState.lastLotNo.trim() || undefined,
      reason: formState.reason.trim(),
    });
    message.success('初始化成功');
  }
  emit('success');
  await modalApi.close();
  return true;
}

const [Modal, modalApi] = useVbenModal({
  title: '年度流水',
  destroyOnClose: true,
  closeOnClickModal: false,
  showCancelButton: true,
  showConfirmButton: true,
  confirmText: '提交',
  onConfirm: handleSubmit,
  onOpenChange(isOpen) {
    if (!isOpen) return;
    resetForm();
    const data = modalApi.getData<{ mode?: SequenceMode; record?: MesHcLotRuleApi.LotRuleCounter }>() || {};
    currentMode.value = data.mode || 'initialize';
    const record = data.record;
    if (record) {
      formState.counterId = record.id;
      formState.ruleId = record.ruleId;
      formState.ruleCode = record.ruleCode || formState.ruleCode;
      formState.counterType = record.counterType || 'ANNUAL_BATCH';
      formState.year = parseYear(record);
      formState.bizDimensionKey = record.bizDimensionKey || formState.bizDimensionKey;
      formState.counterKey = record.counterKey || '';
      formState.resetKey = record.resetKey || '';
      formState.oldCurrentSeq = Number(record.currentSeq || 0);
      formState.currentSeq = Number(record.currentSeq || 0);
      formState.lastLotNo = record.lastLotNo || '';
    }
    modalApi.setState({ title: modalTitle.value });
  },
  onClosed: resetForm,
});
</script>

<template>
  <Modal :title="modalTitle" class="hc-counter-sequence-modal">
    <div class="sequence-modal-wrap">
      <Form :label-col="{ style: { width: '118px' } }" :model="formState" layout="horizontal">
        <FormItem label="规则编码" required>
          <Input v-model:value="formState.ruleCode" :disabled="isAdjust" placeholder="请输入规则编码" />
        </FormItem>
        <FormItem label="计数类型">
          <Input v-model:value="formState.counterType" :disabled="isAdjust" placeholder="ANNUAL_BATCH" />
        </FormItem>
        <FormItem label="年度" required>
          <InputNumber v-model:value="formState.year" :disabled="isAdjust" :min="2000" :precision="0" style="width: 100%" />
        </FormItem>
        <FormItem label="业务维度">
          <Input v-model:value="formState.bizDimensionKey" :disabled="isAdjust" placeholder="默认取年度，如 2026" />
        </FormItem>
        <FormItem v-if="isAdjust" label="计数键">
          <Input v-model:value="formState.counterKey" disabled />
        </FormItem>
        <FormItem v-if="isAdjust" label="调整前计数">
          <InputNumber v-model:value="formState.oldCurrentSeq" disabled style="width: 100%" />
        </FormItem>
        <FormItem label="当前计数" required>
          <InputNumber v-model:value="formState.currentSeq" :min="0" :precision="0" style="width: 100%" />
        </FormItem>
        <FormItem label="最后批号">
          <Input v-model:value="formState.lastLotNo" placeholder="可选，最后一个已生成批号" />
        </FormItem>
        <FormItem label="产线码">
          <Input v-model:value="formState.batchLineCode" maxlength="4" placeholder="默认 A" />
        </FormItem>
        <FormItem label="原因" required>
          <Input.TextArea v-model:value="formState.reason" :maxlength="500" :rows="3" placeholder="请填写初始化或人工设置原因" />
        </FormItem>
      </Form>

      <Space class="sequence-preview-actions">
        <Button :loading="previewLoading" @click="handlePreview">预览下一号</Button>
      </Space>

      <Alert
        v-if="previewResult"
        :message="`下一母批号：${previewResult.nextLotNo || '-'}；当前计数：${previewResult.currentSeq ?? 0}；下一流水：${previewResult.nextSeq ?? '-'}；已生成最大流水：${previewResult.maxUsedSeq ?? 0}`"
        :description="previewResult.warning"
        :type="previewResult.warning ? 'warning' : 'info'"
        show-icon
      />
    </div>
  </Modal>
</template>

<style scoped>
.sequence-modal-wrap {
  padding-top: 4px;
}
.sequence-preview-actions {
  width: 100%;
  margin: 2px 0 12px 118px;
}
</style>
