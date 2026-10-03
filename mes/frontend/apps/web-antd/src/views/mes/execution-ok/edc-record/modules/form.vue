<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { getEdcDetail } from '#/api/mes/execution/edc-record';
import { useFormSchema } from '../data';

defineOptions({ name: 'EdcRecordDetailModal' });

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-2',
  schema: useFormSchema().map(item => {
    if (item.fieldName === 'productName') return { ...item, formItemClass: 'col-span-2' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: '采集记录详情',
  class: 'w-[700px]',
  onCancel() { modalApi.close(); },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();

    await formApi.resetForm();
    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        const info = await getEdcDetail(data.id);
        await formApi.setValues(info);
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="false">
    <BaseForm class="p-4" />
  </BaseModal>
</template>
