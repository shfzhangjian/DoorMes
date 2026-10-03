<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Select, Tag } from 'ant-design-vue';

const emit = defineEmits(['save']);

const modalData = ref<any>({});
const weekNo = ref<number>(1);

const weekOptions = [
  { label: '第一周', value: 1 },
  { label: '第二周', value: 2 },
  { label: '第三周', value: 3 },
  { label: '第四周', value: 4 },
  { label: '第五周', value: 5 },
];

const [Modal, modalApi] = useVbenModal({
  title: '选择计划周',
  class: 'w-[520px]',
  onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>() || {};
    modalData.value = data;
    weekNo.value = data.weekNo || 1;
  },
  onConfirm() {
    emit('save', { ...modalData.value, weekNo: weekNo.value });
    modalApi.close();
  },
});
</script>

<template>
  <Modal>
    <div class="space-y-4 p-5">
      <div class="rounded border border-slate-200 bg-slate-50 p-4">
        <div
          class="grid grid-cols-[88px_minmax(0,1fr)] gap-x-3 gap-y-2 text-sm"
        >
          <span class="text-slate-500">设备编号</span>
          <span class="font-mono font-semibold text-slate-800">{{
            modalData.deviceCode
          }}</span>
          <span class="text-slate-500">设备名称</span>
          <span class="font-semibold text-slate-800">{{
            modalData.deviceName
          }}</span>
          <span class="text-slate-500">保养项目</span>
          <span class="font-semibold text-slate-800">{{
            modalData.itemGroup || '-'
          }}</span>
          <span class="text-slate-500">保养部位</span>
          <span class="text-slate-800">{{ modalData.itemName }}</span>
          <span class="text-slate-500">保养周期</span>
          <span>
            <Tag color="blue" class="!m-0">{{
              modalData.frequency || '-'
            }}</Tag>
          </span>
          <span class="text-slate-500">计划月份</span>
          <span class="font-semibold text-slate-800">
            {{ modalData.monthNo }} 月
          </span>
        </div>
      </div>

      <div>
        <div class="mb-2 text-sm font-semibold text-slate-700">执行周次</div>
        <Select v-model:value="weekNo" class="w-full" :options="weekOptions" />
      </div>
    </div>
  </Modal>
</template>
