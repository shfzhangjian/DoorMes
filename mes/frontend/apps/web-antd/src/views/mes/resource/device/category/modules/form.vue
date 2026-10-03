<script lang="ts" setup>
import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createCategory,
  getCategory,
  getCategoryList,
  updateCategory,
} from '#/api/mes/resource/device/category';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'MesResourceDeviceCategoryForm' });

const emit = defineEmits(['success']);
const formType = ref('');
const formData = ref<MesDeviceCategoryApi.Category>({});

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看设备分类';
  return formData.value.id
    ? $t('ui.actionTitle.edit', ['设备分类'])
    : $t('ui.actionTitle.create', ['设备分类']);
});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 90,
  },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2',
});

async function buildParentOptions(currentId?: number) {
  const rows = await getCategoryList();
  return (rows || [])
    .filter((item) => item.id !== currentId)
    .map((item) => ({
      label: item.categoryCode ? `${item.categoryCode} / ${item.categoryName}` : item.categoryName,
      value: item.id,
    }));
}

const [BaseModal, modalApi] = useVbenModal({
  class: 'w-[720px]',
  title: getTitle,
  async onConfirm() {
    if (formType.value === 'detail') {
      modalApi.close();
      return;
    }
    const { valid } = await formApi.validate();
    if (!valid) return;
    modalApi.setState({ loading: true });
    try {
      const values = (await formApi.getValues()) as MesDeviceCategoryApi.Category;
      if (values.id) {
        await updateCategory(values);
        message.success('更新成功');
      } else {
        await createCategory(values);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<MesDeviceCategoryApi.Category & { type?: string }>();
    formType.value = data?.type || (data?.id ? 'edit' : 'create');
    formData.value = {};
    await formApi.updateSchema(useFormSchema(await buildParentOptions(data?.id), formType.value));
    await formApi.resetForm();
    await nextTick();
    if (data?.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getCategory(data.id);
        await formApi.setValues(formData.value);
      } finally {
        modalApi.setState({ loading: false });
      }
    } else {
      await formApi.setValues({ parentId: data?.parentId ?? 0, status: 1, sort: 0 });
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <div class="px-2 pb-4">
      <BaseForm />
    </div>
  </BaseModal>
</template>
