<script lang="ts" setup>
import type { MesHcFormTemplateApi } from '#/api/mes/hc/formtemplate';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Button, Input, message, Switch } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createFormTemplate, getFormTemplateDetail, updateFormTemplate } from '#/api/mes/hc/formtemplate';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcFormTemplateApi.FormTemplate>();
const templateVersions = ref<Array<MesHcFormTemplateApi.FormTemplateVersion & { _rowKey: string }>>([]);
const rowSeed = ref(1);

const getTitle = computed(() => (formData.value?.id ? '编辑表单模板' : '新增表单模板'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

function nextRowKey() {
  rowSeed.value += 1;
  return `form-template-row-${Date.now()}-${rowSeed.value}`;
}

function normalizeDateText(value: any) {
  if (!value) return '';
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  return String(value);
}

function normalizeRow(row?: Partial<MesHcFormTemplateApi.FormTemplateVersion>) {
  return {
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    isCurrent: row?.isCurrent ?? (templateVersions.value.length === 0),
    effectiveDate: normalizeDateText((row as any)?.effectiveDate),
  } as MesHcFormTemplateApi.FormTemplateVersion & { _rowKey: string };
}

function isEmptyVersionRow(row: Partial<MesHcFormTemplateApi.FormTemplateVersion>) {
  return ![row.versionNo, row.effectiveDate, row.sourceFileName, row.remark].some(
    (value) => value !== undefined && value !== null && String(value).trim() !== '',
  );
}

function validateVersionRows(items: MesHcFormTemplateApi.FormTemplateVersion[]) {
  const invalidIndex = items.findIndex(
    (item) => !item.versionNo?.trim() || !item.effectiveDate?.trim(),
  );
  if (invalidIndex >= 0) {
    message.warning(`模板版本第 ${invalidIndex + 1} 行请填写版本号和生效日期`);
    return false;
  }
  return true;
}

function addVersion() {
  templateVersions.value.push(normalizeRow({ effectiveDate: new Date().toISOString().slice(0, 10) }));
}

function removeVersion(index: number) {
  templateVersions.value.splice(index, 1);
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[1200px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcFormTemplateApi.FormTemplate;
    try {
      const validVersions = templateVersions.value.filter((row) => !isEmptyVersionRow(row)).map((row) => {
        const { _rowKey, ...rest } = row as any;
        return {
          ...rest,
          templateCode: data.templateCode,
        };
      });
      if (!validateVersionRows(validVersions)) return;
      data.templateVersions = validVersions;
      await (formData.value?.id ? updateFormTemplate(data) : createFormTemplate(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      templateVersions.value = [];
      await formApi.resetForm();
      return;
    }

    const data = modalApi.getData<MesHcFormTemplateApi.FormTemplate>();
    if (!data?.id) {
      formData.value = undefined;
      templateVersions.value = [];
      await formApi.resetForm();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getFormTemplateDetail(data.id);
      await formApi.setValues(formData.value);
      templateVersions.value = (formData.value.templateVersions || []).map((item) => normalizeRow(item));
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-2">
      <Form />
    </div>
    <div class="px-2 pb-4">
      <div class="mb-3 flex items-center justify-between">
        <div class="text-base font-medium">模板版本</div>
        <Button type="dashed" @click="addVersion">新增版本</Button>
      </div>
      <div v-if="templateVersions.length === 0" class="rounded border border-dashed border-gray-300 px-4 py-6 text-center text-sm text-text-secondary">
        暂无版本，请点击“新增版本”。
      </div>
      <div v-for="(row, index) in templateVersions" :key="row._rowKey" class="mb-3 rounded border border-gray-200 p-4">
        <div class="mb-3 flex items-center justify-between">
          <div class="font-medium">版本 {{ index + 1 }}</div>
          <Button type="link" danger @click="removeVersion(index)">删除</Button>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <div class="mb-1 text-xs text-text-secondary">版本号</div>
            <Input v-model:value="row.versionNo" placeholder="请输入版本号" />
          </div>
          <div class="flex items-center gap-2 pt-6">
            <Switch v-model:checked="row.isCurrent" checked-children="当前" un-checked-children="历史" />
          </div>
          <div>
            <div class="mb-1 text-xs text-text-secondary">生效日期</div>
            <Input v-model:value="row.effectiveDate" placeholder="请输入 YYYY-MM-DD" />
          </div>
          <div>
            <div class="mb-1 text-xs text-text-secondary">来源文件名</div>
            <Input v-model:value="row.sourceFileName" placeholder="请输入来源文件名" />
          </div>
          <div class="col-span-2">
            <div class="mb-1 text-xs text-text-secondary">备注</div>
            <Input v-model:value="row.remark" placeholder="请输入备注" />
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>


