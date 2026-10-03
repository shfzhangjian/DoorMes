<script lang="ts" setup>
import type { MesCostWageTeamApi } from '#/api/mes/cost/wage/team-weight';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createTeamMember, getTeamMember, updateTeamMember } from '#/api/mes/cost/wage/team-weight';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const isUpdate = ref(false);

const getTitle = computed(() => isUpdate.value ? '编辑班组成员' : '添加班组成员');

const [Form, formApi] = useVbenForm({
  commonConfig: { formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-4',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesCostWageTeamApi.TeamMember;

    try {
      if (isUpdate.value) {
        await updateTeamMember(data);
      } else {
        await createTeamMember(data);
      }
      await modalApi.close();
      emit('success');
      message.success('操作成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;
    const data = modalApi.getData<{ id?: number, teamId?: string }>();

    isUpdate.value = !!data?.id;

    if (data?.id) {
      modalApi.lock();
      try {
        const res = await getTeamMember(data.id) as any;
        await formApi.setValues(res);
      } finally {
        modalApi.unlock();
      }
    } else {
      await formApi.resetForm();
      // 传递左侧选中的班组ID
      if (data?.teamId) {
        await formApi.setValues({ teamId: data.teamId });
      }
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[700px]">
    <Form class="mx-4 mt-4" />
  </Modal>
</template>
