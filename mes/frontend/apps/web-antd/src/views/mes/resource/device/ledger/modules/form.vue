<script lang="ts" setup>
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message, Tabs } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { getCategoryList } from '#/api/mes/resource/device/category';
import {
  createDevice,
  getDevice,
  updateDevice,
} from '#/api/mes/resource/device/ledger';

import { mapDeviceTypeOptions, mapLeafCategoryOptions } from '../../shared';
import { useFormSchema } from '../data';
import ItemList from './item-list.vue';
import ParamList from './param-list.vue'; // [新增] 引入技术参数组件

defineOptions({ name: 'DeviceLedgerFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref('');

const itemListRef = ref<InstanceType<typeof ItemList>>();
const paramListRef = ref<InstanceType<typeof ParamList>>(); // [新增]
const formData = ref<MesDeviceLedgerApi.Device>(
  {} as MesDeviceLedgerApi.Device,
);
const deviceTypeOptions = ref<Array<{ label: string; value: string }>>([]);
const categoryOptions = ref<
  Array<{ label?: string; parentName?: string; value?: number }>
>([]);

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看设备档案';
  return isUpdate.value ? '编辑设备档案' : '新增设备档案';
});

const [BaseForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 100,
  },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: useFormSchema('', deviceTypeOptions.value, categoryOptions.value).map(
    (item) => {
      if (item.fieldName === 'remark')
        return { ...item, formItemClass: 'col-span-3' };
      return item;
    },
  ),
  showDefaultActions: false,
});

const [BaseModal, modalApi] = useVbenModal({
  title: '设备档案',
  class: 'w-[1000px]',
  fullscreenButton: true,
  onConfirm: async () => {
    if (formType.value === 'detail') {
      modalApi.close();
      return;
    }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data = (await formApi.getValues()) as MesDeviceLedgerApi.Device;
      // [新增] 合并两个子表的数据
      data.parts = itemListRef.value?.getData();
      data.params = paramListRef.value?.getData();

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateDevice(data);
        message.success('更新成功');
      } else {
        await createDevice(data);
        message.success('建档成功');
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
    modalApi.setState({ title: getTitle.value });

    const categories = await getCategoryList();
    deviceTypeOptions.value = mapDeviceTypeOptions(categories);
    categoryOptions.value = mapLeafCategoryOptions(categories);
    formApi.updateSchema(
      useFormSchema(
        formType.value,
        deviceTypeOptions.value,
        categoryOptions.value,
      ).map((item) => {
        if (item.fieldName === 'remark')
          return { ...item, formItemClass: 'col-span-3' };
        return item;
      }),
    );
    await formApi.resetForm();
    formData.value = {} as MesDeviceLedgerApi.Device;

    await nextTick();

    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getDevice(data.id);
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
    <div class="device-erp-modal-body">
      <section class="device-erp-card">
        <div class="device-erp-card__title">设备档案主信息</div>
        <BaseForm />
      </section>

      <section class="device-erp-card device-erp-card--tabs">
        <Tabs class="device-erp-tabs" type="card">
          <Tabs.TabPane key="parts" tab="备件清单">
            <ItemList
              ref="itemListRef"
              :device-id="formData?.id"
              :disabled="formType === 'detail'"
            />
          </Tabs.TabPane>
          <Tabs.TabPane key="params" tab="技术参数">
            <ParamList
              ref="paramListRef"
              :device-id="formData?.id"
              :disabled="formType === 'detail'"
            />
          </Tabs.TabPane>
        </Tabs>
      </section>
    </div>
  </BaseModal>
</template>

<style scoped>
.device-erp-modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px 20px;
  background: #f4f6f8;
}

.device-erp-card {
  padding: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.device-erp-card--tabs {
  padding: 0;
}

.device-erp-card__title {
  margin-bottom: 12px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.device-erp-tabs :deep(.ant-tabs-nav) {
  padding: 8px 12px 0;
  margin-bottom: 0;
  background: #f8fafc;
}

.device-erp-tabs :deep(.ant-tabs-content-holder) {
  padding: 12px;
  background: #fff;
}
</style>
