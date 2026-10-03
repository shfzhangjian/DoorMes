<script lang="ts" setup>
import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createMold, getMold, updateMold } from '#/api/mes/resource/mold/ledger';
import { useFormSchema } from '../data';
import ProductList from './product-list.vue';

defineOptions({ name: 'MoldLedgerFormModal' });
const emit = defineEmits(['success']);

const formType = ref('');
const productListRef = ref<InstanceType<typeof ProductList>>();
const formData = ref<any>({});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 120 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: useFormSchema('').map(item => {
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: computed(() => formType.value === 'detail' ? '模具履历档案' : '编辑模具台账'),
  class: 'w-[1000px]',
  fullscreenButton: true,
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = await formApi.getValues();
      data.products = productListRef.value?.getData();
      data.id = formData.value.id;

      if (data.id) await updateMold(data);
      else await createMold(data);

      message.success('档案保存成功');
      emit('success');
      modalApi.close();
    } finally { modalApi.setState({ loading: false }); }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';

    formApi.updateSchema(useFormSchema(formType.value).map(item => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' || item.componentProps?.disabled };
      if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
      return item;
    }));
    await formApi.resetForm();
    await nextTick();

    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getMold(data.id);
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
      <ProductList ref="productListRef" :mold-id="formData?.id" :disabled="formType === 'detail'" />
    </div>
  </BaseModal>
</template>
