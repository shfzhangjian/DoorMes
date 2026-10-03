<script lang="ts" setup>
import type { MesAqlApi } from '#/api/mes/quality/base/aql';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message, Button, InputNumber, Input } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

import { createAql, getAql, updateAql } from '#/api/mes/quality/base/aql';
import { useFormSchema } from '../data';

defineOptions({ name: 'AqlFormModal' });
const emit = defineEmits(['success']);

const isUpdate = ref(false);
const formData = ref<MesAqlApi.AqlStandard>({} as MesAqlApi.AqlStandard);

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: useFormSchema().map(i => i.fieldName === 'remark' ? { ...i, formItemClass: 'col-span-3' } : i),
  showDefaultActions: false,
});

// =========== 子表配置 (VxeGrid) ===========
const [RuleGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true, showOverflow: true, keepSource: true, height: 350,
    rowConfig: { keyField: '_tempId', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false }, toolbarConfig: { enabled: false },
    columns: [
      { title: '批量范围', align: 'center', children: [
          { field: 'minBatchSize', title: '批量下限(≥)', width: 110, slots: { default: 'min' } },
          { field: 'maxBatchSize', title: '批量上限(≤)', width: 110, slots: { default: 'max' } },
        ]},
      { field: 'sampleCode', title: '样本字码', width: 90, slots: { default: 'code' }, align: 'center' },
      { field: 'sampleSize', title: '抽样数量(PCS)', width: 120, slots: { default: 'size' } },
      { title: '合格判定基准', align: 'center', children: [
          { field: 'acValue', title: 'Ac(接收)', width: 100, slots: { default: 'ac' } },
          { field: 'reValue', title: 'Re(拒收)', width: 100, slots: { default: 're' } },
        ]},
      { title: '操作', width: 70, align: 'center', slots: { default: 'action' } },
    ],
    data: [],
  },
});

async function handleAddRule() {
  if (gridApi.grid) {
    const { row } = await gridApi.grid.insertAt({ _tempId: Date.now(), minBatchSize: 1, maxBatchSize: 10, sampleCode: 'A', sampleSize: 1, acValue: 0, reValue: 1 }, -1);
    await gridApi.grid.scrollToRow(row);
  }
}

// =========== 弹窗控制 ===========
const [BaseModal, modalApi] = useVbenModal({
  title: computed(() => isUpdate.value ? '编辑抽样方案与判定规则' : '新增抽样方案'),
  class: 'w-[900px]',
  onCancel() { modalApi.close(); },
  onConfirm: async () => {
    const { valid } = await formApi.validate();
    if (!valid) return;

    // 获取子表数据
    const { fullData } = gridApi.grid?.getTableData() || { fullData: [] };
    if (fullData.length === 0) return message.warning('请配置批量抽样规则表！');

    modalApi.setState({ loading: true });
    try {
      const data = (await formApi.getValues()) as MesAqlApi.AqlStandard;
      data.rules = fullData.map((item: any) => { const { _tempId, ...rest } = item; return rest; }); // 清理临时ID

      if (isUpdate.value) {
        await updateAql({ ...data, id: formData.value.id });
        message.success('更新成功');
      } else {
        await createAql(data);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } finally { modalApi.setState({ loading: false }); }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    isUpdate.value = data?.type === 'edit';

    await formApi.resetForm();
    gridApi.setGridOptions({ data: [] });

    await nextTick();
    if (isUpdate.value && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getAql(data.id);
        await formApi.setValues(formData.value);
        // 赋予临时唯一ID用于 VxeTable 追踪渲染
        const rules = (formData.value.rules || []).map(r => ({ ...r, _tempId: r.id || Date.now() + Math.random() }));
        gridApi.setGridOptions({ data: rules });
      } finally { modalApi.setState({ loading: false }); }
    }
  },
});
</script>

<template>
  <BaseModal>
    <BaseForm class="mx-4 mt-4" />
    <div class="mx-4 mt-2 mb-4 border border-slate-200 bg-white rounded-md overflow-hidden shadow-sm flex flex-col">
      <div class="flex items-center justify-between px-3 py-2 border-b bg-slate-50">
        <span class="font-bold text-slate-700 text-sm border-l-4 border-indigo-600 pl-2">抽样方案对照表 (AQL Rules)</span>
        <Button type="primary" size="small" @click="handleAddRule"><IconifyIcon icon="lucide:plus" /> 增加区间行</Button>
      </div>
      <RuleGrid class="p-2">
        <template #min="{ row }"><InputNumber v-model:value="row.minBatchSize" size="small" class="w-full text-center" :min="1"/></template>
        <template #max="{ row }"><InputNumber v-model:value="row.maxBatchSize" size="small" class="w-full text-center" :min="row.minBatchSize"/></template>
        <template #code="{ row }"><Input v-model:value="row.sampleCode" size="small" class="w-full text-center" /></template>
        <template #size="{ row }"><InputNumber v-model:value="row.sampleSize" size="small" class="w-full text-center" :min="1" /></template>
        <template #ac="{ row }"><InputNumber v-model:value="row.acValue" size="small" class="w-full text-center" :min="0" /></template>
        <template #re="{ row }"><InputNumber v-model:value="row.reValue" size="small" class="w-full text-center" :min="row.acValue + 1" /></template>
        <template #action="{ row }"><Button type="link" danger size="small" @click="gridApi.grid?.remove(row)">移除</Button></template>
      </RuleGrid>
    </div>
  </BaseModal>
</template>

<style scoped>
:deep(.ant-input-number-input) { text-align: center; }
</style>
