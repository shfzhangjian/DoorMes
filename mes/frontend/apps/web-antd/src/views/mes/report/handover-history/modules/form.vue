<script lang="ts" setup>
import type { MesHandoverHistoryApi } from '#/api/mes/report/handover-history';

import { nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { getHistoryDetail } from '#/api/mes/report/handover-history';

import { useFormSchema } from '../data';
import ItemList from './item-list.vue';

defineOptions({ name: 'HandoverHistoryDetailModal' });

const itemListRef = ref<InstanceType<typeof ItemList>>();

const [BaseForm, formApi] = useVbenForm({
  // 全部强制 disabled，因为是历史审计页面
  commonConfig: { componentProps: { class: 'w-full', disabled: true }, formItemClass: 'col-span-1', labelWidth: 110 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3', // 三列布局
  schema: useFormSchema().map((item) => {
    // 遗留事项跨满 3 列
    if (item.fieldName === 'remark') return { ...item, formItemClass: 'col-span-3' };
    return item;
  }),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: '交接班记录追溯详情',
  class: 'w-[1000px]',
  fullscreenButton: true,
  onCancel() { modalApi.close(); },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();

    await formApi.resetForm();
    await nextTick();
    itemListRef.value?.loadData([]);

    if (data.id) {
      modalApi.setState({ loading: true });
      try {
        const detail = await getHistoryDetail(data.id);
        await formApi.setValues(detail);
        // 加载点检子表
        itemListRef.value?.loadData(detail.items || []);
      } finally {
        modalApi.setState({ loading: false });
      }
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="false" cancel-text="关闭">
    <BaseForm class="mx-4 mt-4" />

    <div class="mx-4 mt-2 mb-4 border border-slate-200 rounded-md bg-white overflow-hidden shadow-sm">
      <ItemList ref="itemListRef" :disabled="true" />
    </div>
  </BaseModal>
</template>
