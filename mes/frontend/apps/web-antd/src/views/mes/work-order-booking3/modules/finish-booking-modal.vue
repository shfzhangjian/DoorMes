<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Form, FormItem, InputNumber, Select, Textarea, message } from 'ant-design-vue';
import { submitFinishBooking } from '#/api/mes/work-order-booking/mock';

const emit = defineEmits(['success']);
const formState = ref({ goodQty: 0, scrapQty: 0, scrapReason: undefined, remark: '' });
const currentTask = ref<any>(null);

const [Modal, modalApi] = useVbenModal({
  title: '✅ 结束完工报工',
  width: 500,
  async onConfirm() {
    if (formState.value.goodQty + formState.value.scrapQty <= 0) return message.warning('报工总数需大于0');
    if (formState.value.scrapQty > 0 && !formState.value.scrapReason) return message.warning('有报废数量时必须选择报废原因');

    modalApi.lock();
    try {
      await submitFinishBooking(formState.value);
      message.success('报工过站成功');
      emit('success');
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      currentTask.value = modalApi.getData<any>();
      formState.value = { goodQty: currentTask.value.planQty - currentTask.value.goodQty, scrapQty: 0, scrapReason: undefined, remark: '' };
    }
  }
});
</script>

<template>
  <Modal>
    <div class="p-6">
      <div class="bg-indigo-50 p-3 rounded-lg border border-indigo-100 mb-6 font-bold text-indigo-800 flex justify-between">
        <span>当前工单：{{ currentTask?.id }}</span>
        <span>待产数量：{{ currentTask?.planQty - currentTask?.goodQty }}</span>
      </div>
      <Form layout="vertical">
        <div class="grid grid-cols-2 gap-4">
          <FormItem label="良品产出数量">
            <InputNumber v-model:value="formState.goodQty" :min="0" class="w-full text-emerald-600 font-bold text-lg h-10" />
          </FormItem>
          <FormItem label="报废/不良数量">
            <InputNumber v-model:value="formState.scrapQty" :min="0" class="w-full text-red-600 font-bold text-lg h-10" />
          </FormItem>
        </div>
        <FormItem label="不良原因" v-if="formState.scrapQty > 0" required>
          <Select v-model:value="formState.scrapReason" :options="[{label:'尺寸超差',value:'R1'}, {label:'表面划伤',value:'R2'}]" placeholder="请选择报废原因" />
        </FormItem>
        <FormItem label="作业备注">
          <Textarea v-model:value="formState.remark" :rows="3" placeholder="如有异常请在此备注..." />
        </FormItem>
      </Form>
    </div>
  </Modal>
</template>
