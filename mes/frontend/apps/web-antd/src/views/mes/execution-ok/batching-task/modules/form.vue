<script lang="ts" setup>
import type { MesBatchingTaskApi } from '#/api/mes/execution/batching-task';
import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createTask, getTask, updateTask } from '#/api/mes/execution/batching-task';
import { useFormSchema } from '../data';
import ItemList from './item-list.vue';

// 绝对切断同名组件渲染循环
defineOptions({ name: 'BatchingTaskFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');
const itemListRef = ref<InstanceType<typeof ItemList>>();
const formData = ref<MesBatchingTaskApi.Task>({} as MesBatchingTaskApi.Task);

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看配料任务';
  return isUpdate.value ? '编辑配料任务' : '下发配料任务';
});

// 严格排版: 3列布局，状态隐藏
const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 110 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: useFormSchema('').map((item) => {
    // 跨两列
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-2' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: getTitle,
  class: 'w-[1200px]',
  fullscreenButton: true,
  onCancel() { modalApi.close(); },
  onConfirm: async () => {
    if (formType.value === 'detail') { modalApi.close(); return; }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = (await formApi.getValues()) as MesBatchingTaskApi.Task;
      data.items = itemListRef.value?.getData();

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateTask(data);
        message.success('更新成功');
      } else {
        await createTask(data);
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

    // 安全禁用
    formApi.updateSchema(useFormSchema().map((item) => {
      item.componentProps = { ...item.componentProps, disabled: formType.value === 'detail' || item.componentProps?.disabled };
      if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-2' };
      return item;
    }));
    await formApi.resetForm();
    formData.value = {} as MesBatchingTaskApi.Task;

    await nextTick();

    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getTask(data.id);
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
    <BaseForm class="mx-4 mt-4" />

    <div class="mx-4 mt-2 mb-4 border border-slate-200 rounded-md bg-white overflow-hidden shadow-sm">
      <ItemList ref="itemListRef" :task-id="formData?.id" :disabled="formType === 'detail'" />
    </div>
  </BaseModal>
</template>
