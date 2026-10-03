<script lang="ts" setup>
import { computed, ref, nextTick } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createConfig, updateConfig, getConfig } from '#/api/mes/execution/process-config';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');
const recordId = ref<number>();

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看工艺参数';
  return isUpdate.value ? '编辑工艺参数' : '新增工艺参数';
});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 120 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3', // 💡 对齐规范：三列布局
  schema: useFormSchema().map(item => {
    // 💡 对齐规范：remark 跨三列
    if (item.fieldName === 'remark' || item.fieldName === 'productName') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: getTitle,
  class: 'w-[900px]',
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    const values = await formApi.getValues();
    try {
      modalApi.setState({ loading: true });
      isUpdate.value ? await updateConfig({ ...values, id: recordId.value }) : await createConfig(values);
      message.success('保存成功');
      emit('success');
      modalApi.close();
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';
    isUpdate.value = formType.value === 'edit';
    recordId.value = data?.id;

    // 💡 对齐规范：动态处理禁用和跨列
    formApi.updateSchema(useFormSchema().map((item) => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' };
      if (item.fieldName === 'remark' || item.fieldName === 'productName') return { ...item, formItemClass: 'col-span-3' };
      return item;
    }));

    await formApi.resetForm();
    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        const info = await getConfig(data.id);
        await formApi.setValues(info);
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
  </BaseModal>
</template>
