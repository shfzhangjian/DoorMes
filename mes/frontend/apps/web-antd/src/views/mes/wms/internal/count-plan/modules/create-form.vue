<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Form, Select, Radio, TreeSelect, Divider, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const formRef = ref();
const planForm = ref({ type: 'DYNAMIC', dimension: 'LOCATION', locations: [], categories: [] });

const locationTreeData = [
  { title: '原材料总仓', value: 'WH-RM', key: 'WH-RM', children: [{ title: 'A区-树脂存放区', value: 'ZONE-A', children: [{ title: 'A01-货架', value: 'RACK-A01' }] }, { title: 'B区-辅料区', value: 'ZONE-B' }] }
];

const [Modal, modalApi] = useVbenModal({
  title: '🎯 盘点计划与策略配置',
  fullscreenButton: true,
  async onConfirm() {
    await formRef.value?.validate();
    if (planForm.value.dimension === 'LOCATION' && planForm.value.locations.length === 0) return message.warning('请勾选库位！');
    if (planForm.value.dimension === 'CATEGORY' && planForm.value.categories.length === 0) return message.warning('请选择物料分类！');
    message.success('截取快照成功，计划草稿已生成！');
    emit('success');
    modalApi.close();
  },
});
</script>

<template>
  <Modal class="w-[600px]">
    <Form ref="formRef" :model="planForm" layout="vertical" class="mt-2 p-1">
      <Form.Item label="盘点业务类型" required name="type" class="font-bold text-slate-700">
        <Select v-model:value="planForm.type" size="large" :options="[{value:'DYNAMIC', label:'动态盘点 (仅盘动碰库存)'}, {value:'STATIC', label:'静态全盘 (锁库盲盘)'}]" />
      </Form.Item>
      <Divider class="my-4 border-slate-200" />
      <Form.Item label="选择盘点范围与维度策略" required name="dimension" class="font-bold text-slate-700 mb-3">
        <Radio.Group v-model:value="planForm.dimension" button-style="solid" class="w-full text-center flex shadow-sm">
          <Radio.Button value="LOCATION" class="flex-1 py-1">按仓库/物理库位圈定</Radio.Button>
          <Radio.Button value="CATEGORY" class="flex-1 py-1">按物料分类属性圈定</Radio.Button>
        </Radio.Group>
      </Form.Item>

      <div class="p-4 bg-slate-50 border border-slate-200 rounded-lg shadow-inner min-h-[140px] transition-all">
        <template v-if="planForm.dimension === 'LOCATION'">
          <div class="text-xs font-bold text-slate-500 mb-2 flex items-center"><IconifyIcon icon="lucide:network" class="mr-1 text-indigo-500"/> 请勾选需要盘点的节点，支持多选与层级穿透：</div>
          <TreeSelect v-model:value="planForm.locations" :tree-data="locationTreeData" tree-checkable allow-clear show-search size="large" placeholder="下拉展开并勾选 仓库 / 库区 / 货架" style="width: 100%;" class="shadow-sm bg-white" tree-default-expand-all />
        </template>
        <template v-else>
          <div class="text-xs font-bold text-slate-500 mb-2 flex items-center"><IconifyIcon icon="lucide:tags" class="mr-1 text-indigo-500"/> 跨库区全仓搜索，请选择指定的物料类别：</div>
          <Select v-model:value="planForm.categories" mode="multiple" allow-clear size="large" placeholder="选择物料分类" style="width: 100%;" class="shadow-sm bg-white" :options="[{value:'CAT-A', label:'A类极重主材'}, {value:'CAT-CHEM', label:'危化品及溶剂'}]" />
        </template>
      </div>

      <div class="mt-5 p-3 bg-indigo-50 border border-indigo-200 rounded text-xs text-indigo-800 flex gap-2 shadow-sm">
        <IconifyIcon icon="lucide:info" class="text-lg shrink-0 mt-0.5 text-indigo-600"/>
        <span class="leading-relaxed">确认提交后，系统将按照上述策略提取实时库存快照，生成一张【草稿】状态的计划单。您可以在草稿详情中继续人工增减明细行。</span>
      </div>
    </Form>
  </Modal>
</template>
