<script lang="ts" setup>
import type { MesMaintTaskExecApi } from '#/api/mes/resource/device/maint-task-exec';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { getDevicePage } from '#/api/mes/resource/device/ledger';
import { createTask, getTask, updateTask } from '#/api/mes/resource/device/maint-task-exec';
import { getStandardPage } from '#/api/mes/resource/device/maint-standard';

import { useOrderFormSchema } from '../data';

defineOptions({ name: 'MesMaintOrderFormModal' });

const emit = defineEmits(['success']);
const formData = ref<MesMaintTaskExecApi.Task>({});
const isUpdate = ref(false);
const modalTitle = ref('');
const deviceOptions = ref<Array<{ label: string; value: number }>>([]);
const standardOptions = ref<Array<{ label: string; value: number }>>([]);

const getTitle = computed(() => modalTitle.value || (isUpdate.value ? '编辑保养工单' : '新增保养工单'));

const [BaseForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 90,
  },
  layout: 'horizontal',
  schema: useOrderFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2',
});

async function loadOptions() {
  const [devicePage, standardPage] = await Promise.all([
    getDevicePage({ pageNo: 1, pageSize: 100, status: 1 } as any),
    getStandardPage({ pageNo: 1, pageSize: 100, status: 1 } as any),
  ]);
  deviceOptions.value = (devicePage.list || []).map((item) => ({
    label: `${item.deviceCode || '-'} / ${item.deviceName || '-'}`,
    value: item.id!,
  })).filter((item) => !!item.value);
  standardOptions.value = (standardPage.list || []).map((item) => ({
    label: `${item.code || '-'} / ${item.name || '-'}`,
    value: item.id!,
  })).filter((item) => !!item.value);
}

const [BaseModal, modalApi] = useVbenModal({
  class: 'w-[860px]',
  title: getTitle,
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    modalApi.setState({ loading: true });
    try {
      const values = (await formApi.getValues()) as MesMaintTaskExecApi.Task;
      if (isUpdate.value) {
        await updateTask({ ...formData.value, ...values });
        message.success('保养工单已更新');
      } else {
        await createTask(values);
        message.success('保养工单已创建，等待执行反馈');
      }
      emit('success');
      modalApi.close();
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      formData.value = {};
      modalTitle.value = '';
      return;
    }
    const data = modalApi.getData<MesMaintTaskExecApi.Task & { title?: string }>();
    isUpdate.value = !!data?.id;
    modalTitle.value = data?.title || '';
    modalApi.setState({ loading: true });
    try {
      await loadOptions();
      await formApi.updateSchema(useOrderFormSchema(deviceOptions.value, standardOptions.value));
      await formApi.resetForm();
      await nextTick();
      if (data?.id) {
        formData.value = await getTask(data.id);
        await formApi.setValues(formData.value);
      } else {
        await formApi.setValues({
          planDate: dayjs().format('YYYY-MM-DD'),
          planTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
          planPeriod: dayjs().format('YYYY-MM'),
          maintType: '日常巡检',
        });
      }
    } finally {
      modalApi.setState({ loading: false });
    }
  },
});
</script>

<template>
  <BaseModal>
    <BaseForm class="mx-4 mt-4" />
  </BaseModal>
</template>
