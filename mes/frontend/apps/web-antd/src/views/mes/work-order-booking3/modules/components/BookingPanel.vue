<script lang="ts" setup>
import { ref, computed } from 'vue';
import { Button, Input, message, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import BookingBatchPanel from './BookingBatchPanel.vue';
import BookingRollPanel from './BookingRollPanel.vue';
import BookingPiecePanel from './BookingPiecePanel.vue';

const props = defineProps<{ task: any, startForm: any, bookingForm: any }>();
const emit = defineEmits(['submit', 'cancel']);

// 动态引用具体的业务子组件实例
const dynamicPanelRef = ref();

// 自动判断报工模式
const bookingMode = computed(() => {
  if (['配料', '温法'].includes(props.startForm.process)) return 'BATCH';
  if (['粗磨', '精磨', '粘双面胶', '分切'].includes(props.startForm.process)) return 'ROLL';
  return 'PIECE';
});

// 暴露给父级窗口的提交统一入口
const handleSubmit = () => {
  if (!props.bookingForm.laborHours || props.bookingForm.laborHours <= 0) return message.warning('投入人工工时必须大于 0！');

  if (!dynamicPanelRef.value) return;

  // 调用内部真实子组件的验证与取值函数
  const result = dynamicPanelRef.value.validate();

  if (result) {
    emit('submit', result); // result 包含了 { finalGood, finalScrap }
  }
};

defineExpose({ handleSubmit });
</script>

<template>
  <div class="flex-1 flex flex-col h-full min-h-0 bg-white border border-slate-200 p-5 rounded-xl shadow-sm relative">

    <div class="absolute top-0 right-0 bg-blue-500 text-white text-xs font-bold px-4 py-1.5 rounded-bl-xl shadow-sm z-10">当前执行：{{ startForm.process }} 面板</div>

    <div class="flex items-center justify-between mb-6 shrink-0 pb-3 border-b border-slate-100">
      <span class="font-black text-slate-800 flex items-center text-lg">
        <IconifyIcon icon="lucide:layers" class="mr-2 text-blue-500" /> 产出明细采集区
      </span>
      <Tag color="processing" class="text-base font-bold px-4 py-1 border-none mr-24">
        {{ bookingMode === 'BATCH' ? '整体批量记录' : (bookingMode === 'ROLL' ? '卷材分段记录' : '单件溯源追踪') }}
      </Tag>
    </div>

    <div class="flex-1 flex min-h-0 w-full gap-6">

      <div class="flex-1 flex flex-col min-h-0 pr-2 border-r border-slate-100">
        <BookingBatchPanel v-if="bookingMode === 'BATCH'" ref="dynamicPanelRef" :startForm="startForm" :bookingForm="bookingForm" />
        <BookingRollPanel v-else-if="bookingMode === 'ROLL'" ref="dynamicPanelRef" :startForm="startForm" />
        <BookingPiecePanel v-else-if="bookingMode === 'PIECE'" ref="dynamicPanelRef" :startForm="startForm" />
      </div>

      <div class="w-[340px] shrink-0 flex flex-col h-full min-h-0 bg-white">
        <div class="bg-indigo-50 border border-indigo-100 rounded-t-xl px-4 py-3 font-black text-indigo-800 flex items-center shrink-0">
          <IconifyIcon icon="lucide:message-square-edit" class="mr-2 text-lg" />
          交接班与生产异常备注
        </div>
        <Input.TextArea
          v-model:value="bookingForm.remark"
          class="flex-1 w-full !h-full resize-none border border-indigo-100 border-t-0 bg-slate-50 focus:bg-white text-base p-4 rounded-b-xl shadow-inner focus:ring-0"
          placeholder="请详细记录生产过程中的工装异常、物料差异等信息，此记录将随批次流转并永远归档..."
        />
      </div>

    </div>

    <div class="h-16 shrink-0 border-t border-slate-100 flex items-center justify-end gap-4 mt-2 pt-2">
      <Button size="large" class="w-32 font-bold" @click="emit('cancel')">取消返回</Button>
      <Button size="large" type="primary" class="w-48 font-black text-lg bg-emerald-600 hover:bg-emerald-500 border-none shadow-md" @click="handleSubmit">确认提交全部报工</Button>
    </div>

  </div>
</template>
