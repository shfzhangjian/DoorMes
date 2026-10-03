<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, nextTick, ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createMeasureToolLedger,
  getMeasureToolCategoryList,
  getMeasureToolLedger,
  updateMeasureToolLedger,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import {
  loadPersonnelOptions,
  loadUsingDepartmentOptions,
  mapAreaOptions,
  mapLocationOptions,
  normalizeEditableSelectValue,
  toEditableSelectValue,
} from '../options';

const emit = defineEmits(['success']);
const { hasAccessByCodes } = useAccess();
const canManageExternalOpen = hasAccessByCodes([
  'mes:qms-measure-tool-ledger:internal-admin',
]);
const formData = ref<QmsMeasureToolApi.Ledger>();
const locationOptions = ref<Array<{ label?: string; value?: number | string }>>(
  [],
);
const areaOptions = ref<
  Array<{
    label?: string;
    parentId?: number;
    parentName?: string;
    value?: number | string;
  }>
>([]);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['量检具台账'])
    : $t('ui.actionTitle.create', ['量检具台账']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  layout: 'horizontal',
  schema: useFormSchema([], [], [], [], canManageExternalOpen),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[920px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const values = (await formApi.getValues()) as QmsMeasureToolApi.Ledger & {
      positionId?: number;
    };
    const { positionId, ...ledgerValues } = values;
    const selectedLocation = locationOptions.value.find(
      (item) => item.value === positionId,
    );
    const saveValues = {
      ...ledgerValues,
      maintainerName: normalizeEditableSelectValue(values.maintainerName),
      responsiblePerson: normalizeEditableSelectValue(values.responsiblePerson),
      storageLocation: selectedLocation?.label || values.storageLocation,
      usingDepartment: normalizeEditableSelectValue(values.usingDepartment),
    };
    try {
      await (formData.value?.id
        ? updateMeasureToolLedger({
            ...saveValues,
            id: formData.value.id,
            version: formData.value.version,
          })
        : createMeasureToolLedger(saveValues));
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }

    const data = modalApi.getData<{ id?: number }>() || {};
    const [categoryRows, usingDepartmentOptions, personnelOptions] =
      await Promise.all([
        getMeasureToolCategoryList(),
        loadUsingDepartmentOptions(),
        loadPersonnelOptions(),
      ]);
    locationOptions.value = mapLocationOptions(categoryRows);
    areaOptions.value = mapAreaOptions(categoryRows);
    await formApi.updateSchema(
      useFormSchema(
        locationOptions.value,
        areaOptions.value,
        usingDepartmentOptions,
        personnelOptions,
        canManageExternalOpen,
      ),
    );
    await formApi.resetForm();

    if (!data?.id) {
      formData.value = undefined;
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getMeasureToolLedger(data.id);
      const selectedArea = areaOptions.value.find(
        (item) => item.value === formData.value?.categoryId,
      );
      await nextTick();
      await formApi.setValues({
        ...formData.value,
        maintainerName: toEditableSelectValue(formData.value.maintainerName),
        responsiblePerson: toEditableSelectValue(
          formData.value.responsiblePerson,
        ),
        usingDepartment: toEditableSelectValue(formData.value.usingDepartment),
        positionId: selectedArea?.parentId,
      });
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form />
    </div>
  </Modal>
</template>
