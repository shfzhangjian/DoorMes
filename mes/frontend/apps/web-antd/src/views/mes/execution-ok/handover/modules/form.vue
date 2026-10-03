<script lang="ts" setup>
import type { MesHandoverApi } from '#/api/mes/execution/handover';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createHandover, getHandover, updateHandover } from '#/api/mes/execution/handover';

import { useFormSchema } from '../data';
import ItemList from './item-list.vue';

defineOptions({ name: 'HandoverFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');
const itemListRef = ref<InstanceType<typeof ItemList>>();
const formData = ref<MesHandoverApi.Handover>({} as MesHandoverApi.Handover);

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看交接班记录';
  return isUpdate.value ? '编辑交接班记录' : '发起交接班';
});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: useFormSchema().map((item) => {
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: getTitle,
  class: 'w-[1000px]',
  fullscreenButton: true,
  onCancel() { modalApi.close(); },
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = (await formApi.getValues()) as MesHandoverApi.Handover;
      data.items = itemListRef.value?.getData() || [];

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateHandover(data);
        message.success('更新成功');
      } else {
        await createHandover(data);
        message.success('交接班发起成功');
      }
      emit('success');
      modalApi.close();
    } catch (error) {
      console.error(error);
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';
    isUpdate.value = formType.value === 'edit';

    formApi.updateSchema(useFormSchema().map((item) => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' || item.componentProps?.disabled };
      if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
      return item;
    }));
    await formApi.resetForm();
    formData.value = {} as MesHandoverApi.Handover;

    await nextTick();
    itemListRef.value?.loadData([]);

    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getHandover(data.id);
        await formApi.setValues(formData.value);
        itemListRef.value?.loadData(formData.value.items || []);
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <BaseForm class="mx-4 mt-4" />

    <div class="mx-4 mt-2 mb-4 border border-slate-200 rounded-md bg-white overflow-hidden shadow-sm">
      <ItemList ref="itemListRef" :disabled="formType === 'detail'" />
    </div>
  </BaseModal>
</template>
