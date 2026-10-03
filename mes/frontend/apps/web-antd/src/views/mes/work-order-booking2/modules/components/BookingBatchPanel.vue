<script lang="ts" setup>
import { Form, FormItem, InputNumber, RadioGroup, RadioButton, Select, message } from 'ant-design-vue';
import { DEFECT_CODES } from '../../data';

const props = defineProps<{ startForm: any, bookingForm: any }>();

// 暴露给父组件的验证与取值方法
const validate = () => {
  if (props.bookingForm.scrapQty > 0 && !props.bookingForm.scrapReason) {
    message.error('防呆拦截：批量模式下存在不良数，必须选择不良原因！');
    return null;
  }
  return { finalGood: props.bookingForm.goodQty, finalScrap: props.bookingForm.scrapQty };
};

defineExpose({ validate });
</script>

<template>
  <div class="flex flex-col w-full h-full overflow-y-auto pr-4">
    <Form layout="vertical" class="w-full">
      <div class="grid grid-cols-2 gap-8 mb-6">
        <FormItem :label="startForm.process === '配料' ? '良品产出总量 (kg)' : '良品产出总量 (米)'" class="font-bold">
          <InputNumber v-model:value="bookingForm.goodQty" :min="0" class="w-full text-emerald-600 font-black text-3xl h-14 text-center custom-huge-input" />
        </FormItem>
        <FormItem label="不良报废总量" class="font-bold"><InputNumber v-model:value="bookingForm.scrapQty" :min="0" class="w-full text-red-500 font-black text-3xl h-14 text-center custom-huge-input" /></FormItem>
      </div>
      <div class="grid grid-cols-2 gap-8 mb-6">
        <FormItem label="取样损耗数量" class="font-bold"><InputNumber v-model:value="bookingForm.lossQty" :min="0" class="w-full text-amber-500 font-black text-3xl h-14 text-center custom-huge-input" /></FormItem>
        <FormItem label="整体自检判定" class="font-bold mb-0">
          <RadioGroup v-model:value="bookingForm.batchSelfInspect" button-style="solid" size="large" class="w-full flex custom-radio-group h-14 items-center">
            <RadioButton value="OK" class="flex-1 text-center font-bold text-lg">自检合格 OK</RadioButton>
            <RadioButton value="NG" class="flex-1 text-center font-bold text-lg">自检异常 NG</RadioButton>
          </RadioGroup>
        </FormItem>
      </div>
      <div class="grid grid-cols-1 gap-8">
        <FormItem v-if="bookingForm.scrapQty > 0" label="强制必填：不良代码" required class="font-bold mb-0">
          <Select v-model:value="bookingForm.scrapReason" size="large" class="w-full h-12" :options="DEFECT_CODES" placeholder="拦截：发现不良品必须说明原因" />
        </FormItem>
      </div>
    </Form>
  </div>
</template>

<style scoped>
:deep(.custom-huge-input .ant-input) { height: 100%; text-align: center; }
:deep(.custom-radio-group .ant-radio-button-wrapper) { border-radius: 6px !important; border: 1px solid #d9d9d9 !important; transition: all 0.2s; }
:deep(.custom-radio-group .ant-radio-button-wrapper::before) { display: none !important; }
:deep(.custom-radio-group .ant-radio-button-wrapper-checked) { background: #4f46e5 !important; color: white !important; border-color: #4f46e5 !important; box-shadow: 0 2px 4px rgba(79, 70, 229, 0.2) !important; }
</style>
