<script lang="ts" setup>
import type { MesCostRuleApi } from '#/api/mes/cost/base/allocation-rule';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createCostRule, getCostRule, updateCostRule } from '#/api/mes/cost/base/allocation-rule';
import { $t } from '#/locales';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesCostRuleApi.Rule>();

const getTitle = computed(() => {
  return formData.value?.id ? '编辑分摊规则' : '新增分摊规则';
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1', // 大部分字段占半行
    labelWidth: 100,
  },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-4', // 两列布局
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesCostRuleApi.Rule;
    try {
      await (formData.value?.id ? updateCostRule(data) : createCostRule(data));
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    const data = modalApi.getData<MesCostRuleApi.Rule>();
    if (!data || !data.id) {
      await formApi.setValues(data || {});
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getCostRule(data.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[800px]">
    <Form class="mx-4 mt-4">
      <template #methodDescSlot="{ values }">
        <Alert
          v-if="ALLOCATION_METHOD_DESC[Number(values.method)]"
          type="info"
          show-icon
        >
          <template #message>
            {{ ALLOCATION_METHOD_DESC[Number(values.method)].title }} - 计算模型说明
          </template>
          <template #description>
            <div class="text-sm mt-1 leading-relaxed">
              <div>
                <strong>适用场景：</strong>
                {{ ALLOCATION_METHOD_DESC[Number(values.method)].scene }}
              </div>
              <div class="mt-1">
                <strong>计算逻辑：</strong>
                {{ ALLOCATION_METHOD_DESC[Number(values.method)].desc }}
              </div>
              <div class="mt-2 p-2 bg-blue-50 text-blue-800 rounded border border-blue-200 font-mono">
                <strong>🧮 公式：</strong>
                {{ ALLOCATION_METHOD_DESC[Number(values.method)].formula }}
              </div>
            </div>
          </template>
        </Alert>
      </template>
    </Form>
  </Modal>
</template>
