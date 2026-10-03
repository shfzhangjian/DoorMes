<script lang="ts" setup>
import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createPlan, getPlan, updatePlan } from '#/api/mes/resource/device/check';
import { usePlanFormSchema } from '../data';

// 【防递归命名】
defineOptions({ name: 'MaintPlanFormModal' });
const emit = defineEmits(['success']);

const isUpdate = ref(false);
const formType = ref('');
const formData = ref<any>({});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-2',
  schema: usePlanFormSchema('').map(item => {
    // 跨两列保证文本框够长
    if (item.fieldName === 'content') return { ...item, formItemClass: 'col-span-2' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: computed(() => formType.value === 'detail' ? '查看维保策略' : (isUpdate.value ? '编辑维保策略' : '新增维保策略')),
  class: 'w-[850px]', // 【留白设计】控制物理宽度
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = await formApi.getValues();
      if (isUpdate.value) await updatePlan({ ...data, id: formData.value.id } as any);
      else await createPlan(data as any);
      message.success('保存成功');
      emit('success');
      modalApi.close();
    } finally { modalApi.setState({ loading: false }); }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';
    isUpdate.value = formType.value === 'edit';

    // 【洗盘重置】
    formApi.updateSchema(usePlanFormSchema(formType.value).map(item => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' || item.componentProps?.disabled };
      if (item.fieldName === 'content') return { ...item, formItemClass: 'col-span-2' };
      return item;
    }));
    await formApi.resetForm();
    await nextTick();

    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getPlan(data.id);
        await formApi.setValues(formData.value);
      } finally { modalApi.setState({ loading: false }); }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <BaseForm class="mx-6 mt-4 mb-6" />
  </BaseModal>
</template>
