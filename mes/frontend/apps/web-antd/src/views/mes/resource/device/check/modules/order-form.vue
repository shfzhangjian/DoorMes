<script lang="ts" setup>
import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createOrder, getOrder, updateOrder } from '#/api/mes/resource/device/check';
import { useOrderFormSchema } from '../data';
import OrderItemList from './order-item-list.vue';

// 【防递归命名】
defineOptions({ name: 'MaintOrderFormModal' });
const emit = defineEmits(['success']);

const formType = ref('');
const itemListRef = ref<InstanceType<typeof OrderItemList>>();
const formData = ref<any>({});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 90 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3', // 主表3列
  schema: useOrderFormSchema('').map(item => {
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: computed(() => formType.value === 'detail' ? '查看维保工单' : '工单执行与反馈'),
  class: 'w-[1000px]', // 【留白设计】
  fullscreenButton: true,
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = await formApi.getValues();
      data.items = itemListRef.value?.getData(); // 【提取法则】读取子表数据
      data.id = formData.value.id;

      await updateOrder(data as any);
      message.success('反馈成功');
      emit('success');
      modalApi.close();
    } finally { modalApi.setState({ loading: false }); }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';

    formApi.updateSchema(useOrderFormSchema(formType.value).map(item => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' || item.componentProps?.disabled };
      if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
      return item;
    }));
    await formApi.resetForm();
    await nextTick();

    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getOrder(data.id);
        await formApi.setValues(formData.value);
      } finally { modalApi.setState({ loading: false }); }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <BaseForm class="mx-6 mt-4" />

    <div class="mx-6 mt-2 mb-6 border border-slate-200 rounded-md bg-white overflow-hidden shadow-sm">
      <OrderItemList ref="itemListRef" :order-id="formData?.id" :disabled="formType === 'detail'" />
    </div>
  </BaseModal>
</template>
