<script lang="ts" setup>
import type { MesCostWageApi } from '#/api/mes/cost/base/wage-quota';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createWageQuota, getWageQuota, updateWageQuota } from '#/api/mes/cost/base/wage-quota';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const isNewVersion = ref(false);

const getTitle = computed(() => {
  if (isNewVersion.value) return '基于当前定额拟定新版本';
  return isUpdate.value ? '编辑定额草稿' : '新增薪酬定额';
});

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
    const data = (await formApi.getValues()) as MesCostWageApi.WageQuota;

    try {
      if (isUpdate.value && !isNewVersion.value) {
        await updateWageQuota(data);
      } else {
        // 新增或生成新版本，走 Create 接口
        await createWageQuota(data);
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
    const data = modalApi.getData<{ id?: number, action?: string }>();

    isUpdate.value = !!data?.id;
    isNewVersion.value = data?.action === 'newVersion';

    if (data?.id) {
      modalApi.lock();
      try {
        const res = await getWageQuota(data.id) as any;
        if (isNewVersion.value) {
          // 变更新版本：清除ID，自动升级版本号，清空生效日期
          delete res.id;
          const currentVerNum = parseFloat(res.version.replace('V', ''));
          res.version = `V${(currentVerNum + 1.0).toFixed(1)}`;
          res.effectiveDate = null;
          res.remark = '基于旧版升级';
        }
        await formApi.setValues(res);
      } finally {
        modalApi.unlock();
      }
    } else {
      await formApi.resetForm();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[700px]">
    <div v-if="isNewVersion" class="mb-4 px-4 py-2 bg-orange-50 text-orange-600 border border-orange-200 rounded text-sm mx-4">
      <span class="i-ep:warning mr-1"></span> 拟定新版本将创建一条草稿记录。待其发布生效时，原版本会自动标记为失效。
    </div>
    <Form class="mx-4 mt-2" />
  </Modal>
</template>
