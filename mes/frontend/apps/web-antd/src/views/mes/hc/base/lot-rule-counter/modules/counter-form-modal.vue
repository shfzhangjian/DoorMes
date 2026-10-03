<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Form, FormItem, Input, InputNumber, message } from 'ant-design-vue';

import { createLotRuleCounter, updateLotRuleCounter } from '#/api/mes/hc/lotrule';

const emit = defineEmits(['success']);

const formState = reactive({
  id: undefined as number | undefined,
  ruleId: undefined as number | undefined,
  ruleCode: '',
  counterType: '',
  bizDimensionKey: '',
  bizDimensionJson: '',
  counterKey: '',
  resetKey: '',
  currentSeq: 0,
  lastLotNo: '',
});

const currentMode = ref<'create' | 'edit' | 'view'>('create');
const mode = computed(() => currentMode.value);
const readonly = computed(() => mode.value === 'view');
const modalTitle = computed(() => {
  if (mode.value === 'edit') return '修订实例台账';
  if (mode.value === 'view') return '查看实例台账';
  return '新增实例台账';
});

async function handleSubmit() {
  if (readonly.value) {
    await modalApi.close();
    return true;
  }
  if (!formState.ruleCode.trim()) {
    message.warning('规则编码不能为空');
    return false;
  }
  if (!formState.counterKey.trim()) {
    message.warning('计数键不能为空');
    return false;
  }
  if (!formState.resetKey.trim()) {
    message.warning('重置键不能为空');
    return false;
  }
  const payload = {
    id: formState.id,
    ruleId: formState.ruleId,
    ruleCode: formState.ruleCode.trim(),
    counterType: formState.counterType?.trim() || undefined,
    bizDimensionKey: formState.bizDimensionKey?.trim() || undefined,
    bizDimensionJson: formState.bizDimensionJson?.trim() || undefined,
    counterKey: formState.counterKey.trim(),
    resetKey: formState.resetKey.trim(),
    currentSeq: Number(formState.currentSeq || 0),
    lastLotNo: formState.lastLotNo?.trim() || undefined,
  };
  if (mode.value === 'edit') {
    await updateLotRuleCounter(payload);
    message.success('修订成功');
  } else {
    await createLotRuleCounter(payload);
    message.success('新增成功');
  }
  emit('success');
  await modalApi.close();
  return true;
}

const [Modal, modalApi] = useVbenModal({
  title: '实例台账',
  destroyOnClose: true,
  closeOnClickModal: false,
  showCancelButton: true,
  showConfirmButton: true,
  onConfirm: handleSubmit,
  onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<{ mode?: 'create' | 'edit' | 'view'; record?: any }>() || {};
    currentMode.value = data.mode || 'create';
    modalApi.setState({
      showConfirmButton: currentMode.value !== 'view',
      showCancelButton: true,
      cancelText: currentMode.value === 'view' ? '关闭' : '取消',
      confirmText: currentMode.value === 'view' ? undefined : '确定',
      title: modalTitle.value,
    });
    const record = data.record || {};
    formState.id = record.id;
    formState.ruleId = record.ruleId;
    formState.ruleCode = record.ruleCode || '';
    formState.counterType = record.counterType || '';
    formState.bizDimensionKey = record.bizDimensionKey || '';
    formState.bizDimensionJson = record.bizDimensionJson || '';
    formState.counterKey = record.counterKey || '';
    formState.resetKey = record.resetKey || '';
    formState.currentSeq = Number(record.currentSeq || 0);
    formState.lastLotNo = record.lastLotNo || '';
  },
  onClosed() {
    currentMode.value = 'create';
    formState.id = undefined;
    formState.ruleId = undefined;
    formState.ruleCode = '';
    formState.counterType = '';
    formState.bizDimensionKey = '';
    formState.bizDimensionJson = '';
    formState.counterKey = '';
    formState.resetKey = '';
    formState.currentSeq = 0;
    formState.lastLotNo = '';
  },
});
</script>

<template>
  <Modal :title="modalTitle" class="hc-counter-form-modal">
    <div class="counter-form-wrap">
      <Form :label-col="{ style: { width: '108px' } }" :model="formState" layout="horizontal">
        <FormItem label="规则编码" required>
          <Input v-model:value="formState.ruleCode" :disabled="readonly || mode === 'edit'" placeholder="请输入规则编码" />
        </FormItem>
        <FormItem label="计数类型">
          <Input v-model:value="formState.counterType" :disabled="readonly" placeholder="如 ANNUAL_BATCH" />
        </FormItem>
        <FormItem label="业务维度">
          <Input v-model:value="formState.bizDimensionKey" :disabled="readonly" placeholder="如 2026" />
        </FormItem>
        <FormItem label="计数键" required>
          <Input v-model:value="formState.counterKey" :disabled="readonly || mode === 'edit'" placeholder="请输入计数键" />
        </FormItem>
        <FormItem label="重置键" required>
          <Input v-model:value="formState.resetKey" :disabled="readonly" placeholder="如 202603 / 2026Q1" />
        </FormItem>
        <FormItem label="当前计数" required>
          <InputNumber v-model:value="formState.currentSeq" :controls="false" :disabled="readonly" :min="0" style="width: 100%" />
        </FormItem>
        <FormItem label="最后批号">
          <Input v-model:value="formState.lastLotNo" :disabled="readonly" placeholder="请输入最后批号" />
        </FormItem>
        <FormItem label="维度JSON">
          <Input v-model:value="formState.bizDimensionJson" :disabled="readonly" placeholder="可选，业务维度上下文 JSON" />
        </FormItem>
      </Form>
    </div>
  </Modal>
</template>

<style scoped>
.counter-form-wrap { padding-top: 4px; }
</style>
