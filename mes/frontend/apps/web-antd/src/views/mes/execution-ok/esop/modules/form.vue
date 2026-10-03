<script lang="ts" setup>
import type { MesEsopApi } from '#/api/mes/execution/esop';

import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createEsop, getEsop, updateEsop } from '#/api/mes/execution/esop';

import { useFormSchema } from '../data';

defineOptions({ name: 'EsopFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');
const formData = ref<MesEsopApi.Esop>({} as MesEsopApi.Esop);

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看作业指导书';
  return isUpdate.value ? '编辑作业指导书' : '上传作业指导书';
});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-2', // ESOP 字段较少，使用双列布局即可
  schema: useFormSchema().map((item) => {
    // 让备注和 URL 占满整行
    if (item.fieldName === 'remark' || item.fieldName === 'fileUrl') return { ...item, formItemClass: 'col-span-2' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: getTitle,
  class: 'w-[800px]',
  onCancel() { modalApi.close(); },
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = (await formApi.getValues()) as MesEsopApi.Esop;

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateEsop(data);
        message.success('更新成功');
      } else {
        await createEsop(data);
        message.success('上传成功');
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
      if (item.fieldName === 'remark' || item.fieldName === 'fileUrl') return { ...item, formItemClass: 'col-span-2' };
      return item;
    }));
    await formApi.resetForm();
    formData.value = {} as MesEsopApi.Esop;

    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getEsop(data.id);
        await formApi.setValues(formData.value);
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <BaseForm class="p-6" />
  </BaseModal>
</template>
