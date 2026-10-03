<script lang="ts" setup>
import type { MesDefectCodeApi } from '#/api/mes/quality/base/defect-code';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createDefectCode,
  getDefectCode,
  getDefectCodeList,
  updateDefectCode,
} from '#/api/mes/quality/base/defect-code';

import { useFormSchema } from '../data';
import CauseList from './cause-list.vue';

defineOptions({ name: 'DefectCodeFormModal' });

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref<'create' | 'detail' | 'edit'>('create');
const formData = ref<MesDefectCodeApi.DefectCode>(
  {} as MesDefectCodeApi.DefectCode,
);
const nodeType = ref<'CATEGORY' | 'ITEM'>('ITEM');
const causeListRef = ref<InstanceType<typeof CauseList>>();

const getTitle = computed(() => {
  if (formType.value === 'detail') return '查看缺陷节点';
  return isUpdate.value ? '编辑缺陷节点' : '新增缺陷节点';
});

function handleTypeChange(value: 'CATEGORY' | 'ITEM') {
  nodeType.value = value;
  if (value === 'CATEGORY') {
    causeListRef.value?.loadData([]);
  }
}

const [BaseForm, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, labelWidth: 100 },
  schema: useFormSchema(handleTypeChange),
  showDefaultActions: false,
});

const defaultTreeConfig = { id: 'id', pid: 'parentId', children: 'children' };

// 💡 补充：本地实现的平铺转树形结构工具函数
function listToTree(list: any[], config = defaultTreeConfig) {
  const { id, pid, children } = config;
  const map: Record<string, any> = {};
  const tree: any[] = [];

  // 初始化 Map
  for (const item of list) {
    map[item[id]] = { ...item, [children]: [] };
  }

  // 构建树
  for (const item of list) {
    const node = map[item[id]];
    if (item[pid] && map[item[pid]]) {
      map[item[pid]][children].push(node);
    } else {
      tree.push(node);
    }
  }
  return tree;
}

const [BaseModal, modalApi] = useVbenModal({
  title: '缺陷节点',
  class: 'w-[960px]',
  onCancel() {
    modalApi.close();
  },
  onConfirm: async () => {
    if (formType.value === 'detail') {
      modalApi.close();
      return;
    }

    const { valid } = await formApi.validate();
    if (!valid) return;

    try {
      const data = (await formApi.getValues()) as MesDefectCodeApi.DefectCode;
      if (data.type === 'ITEM') {
        const causes = causeListRef.value?.getData() || [];
        if (causes.some((item) => !item.reasonName?.trim())) {
          message.warning('请至少填写发生原因名称');
          return;
        }
        data.causes = causes.map((item) => ({
          ...item,
          reasonCode: item.reasonCode?.trim() || undefined,
          reasonName: item.reasonName.trim(),
          reasonDesc: item.reasonDesc?.trim() || undefined,
        }));
      } else {
        data.causes = [];
      }

      modalApi.setState({ loading: true });
      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateDefectCode(data);
        message.success('更新成功');
      } else {
        await createDefectCode(data);
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
    modalApi.setState({ title: getTitle.value });

    await formApi.resetForm();
    formData.value = {} as MesDefectCodeApi.DefectCode;
    nodeType.value = data?.nodeType || 'ITEM';
    causeListRef.value?.loadData([]);

    // 1. 获取全量列表用于上级分类树下拉
    const allList = await getDefectCodeList();
    // 仅保留分类节点作为可选父级
    const categoryList = allList.filter((item) => item.type === 'CATEGORY');

    // 💡 使用本地 listToTree 函数
    const treeData = [
      { id: 0, name: '主类目 (无上级)' },
      ...listToTree(categoryList, {
        id: 'id',
        pid: 'parentId',
        children: 'children',
      }),
    ];

    const isDetail = formType.value === 'detail';
    formApi.updateSchema(
      useFormSchema(handleTypeChange).map((item) => {
        const componentProps = item.componentProps as
          | Record<string, any>
          | undefined;
        return {
          ...item,
          componentProps: {
            ...componentProps,
            ...(item.fieldName === 'parentId' ? { treeData } : {}),
            disabled: isDetail || componentProps?.disabled,
          },
        };
      }),
    );

    await nextTick();

    // 2. 数据回显
    if (formType.value !== 'create' && data.id) {
      modalApi.setState({ loading: true });
      try {
        formData.value = await getDefectCode(data.id);
        nodeType.value = formData.value.type;
        modalApi.setState({ title: getTitle.value });
        await formApi.setValues(formData.value);
        await nextTick();
        causeListRef.value?.loadData(formData.value.causes || []);
      } finally {
        modalApi.setState({ loading: false });
      }
    } else {
      // 传递进来的 parentId (如果是从表格“新增子项”点进来的)
      await formApi.setValues({
        parentId: data.parentId || 0,
        type: data?.nodeType || 'ITEM',
        status: 1,
      });
      await nextTick();
      causeListRef.value?.loadData([]);
    }
  },
});
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <div class="space-y-4 p-6">
      <BaseForm />
      <CauseList
        v-if="nodeType === 'ITEM'"
        ref="causeListRef"
        :disabled="formType === 'detail'"
      />
    </div>
  </BaseModal>
</template>
