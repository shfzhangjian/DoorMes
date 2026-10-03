<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { formSchema } from '../data';
import { message } from 'ant-design-vue';

const emit = defineEmits(['success']);
const isUpdate = ref(false);

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
  layout: 'horizontal',
});

const [Modal, modalApi] = useVbenModal({
  title: '考核指标定义',
  onCancel() { modalApi.close(); },
  async onConfirm() {
    await formApi.validate();
    message.success(isUpdate.value ? '修改成功' : '新增指标成功');
    emit('success');
    modalApi.close();
  },
  onOpenChange(isOpen: boolean) {
    if (isOpen) {
      const { record, isUpdate: isUpdateVal } = modalApi.getData();
      isUpdate.value = !!isUpdateVal;
      modalApi.setState({ title: isUpdate.value ? '编辑考核题库' : '新建考核题库' });
      isUpdate.value && record ? formApi.setValues(record) : formApi.resetForm();
    }
  },
});
</script>

<template>
  <Modal>
    <Form />
  </Modal>
</template>
