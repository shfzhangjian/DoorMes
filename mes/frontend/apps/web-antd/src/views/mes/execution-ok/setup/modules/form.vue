<script lang="ts" setup>
import type { MesSetupRuleApi } from '#/api/mes/execution/setup';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createRule, getRule, updateRule } from '#/api/mes/execution/setup';

import { useFormSchema } from '../data';
import ItemList from './item-list.vue';

defineOptions({ name: 'SetupRuleFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');
const itemListRef = ref<InstanceType<typeof ItemList>>();
const formData = ref<MesSetupRuleApi.Rule>({} as MesSetupRuleApi.Rule);

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看防错规则';
  return isUpdate.value ? '编辑防错规则' : '新增防错规则';
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
      const data = (await formApi.getValues()) as MesSetupRuleApi.Rule;
      data.items = itemListRef.value?.getData() || [];

      if (data.items.length === 0) {
        return message.warning('请至少添加一条 BOM 投料明细！');
      }

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateRule(data);
        message.success('更新成功');
      } else {
        await createRule(data);
        message.success('创建成功');
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
    formData.value = {} as MesSetupRuleApi.Rule;

    await nextTick();
    itemListRef.value?.loadData([]);

    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getRule(data.id);
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
