<script lang="ts" setup>
import { reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Form, Input, InputNumber, message } from 'ant-design-vue';

defineOptions({ name: 'QmsSourceLotPicker' });

const emit = defineEmits(['select']);

const formRef = ref();
const sourceForm = reactive({
  lotNo: '',
  materialCode: '',
  materialName: '',
  processName: '',
  sourceId: undefined as number | undefined,
  sourceNo: '',
  specification: '',
  unitCode: '',
});

const [Modal, modalApi] = useVbenModal({
  title: '来源批次/工单信息回填',
  class: 'w-[760px] max-w-[86vw]',
  async onConfirm() {
    if (!sourceForm.lotNo && !sourceForm.sourceNo) {
      return message.warning('请至少填写追溯批号或来源单号');
    }
    emit('select', { ...sourceForm });
    modalApi.close();
  },
});
</script>

<template>
  <Modal>
    <Form ref="formRef" :model="sourceForm" class="p-2" layout="vertical">
      <div class="grid grid-cols-2 gap-x-5">
        <Form.Item label="追溯批号" required>
          <Input
            v-model:value="sourceForm.lotNo"
            placeholder="请输入生产批次/检验批次"
          />
        </Form.Item>
        <Form.Item label="来源单号">
          <Input
            v-model:value="sourceForm.sourceNo"
            placeholder="工单号、检验单号或客诉单号"
          />
        </Form.Item>
        <Form.Item label="来源对象ID">
          <InputNumber
            v-model:value="sourceForm.sourceId"
            class="w-full"
            placeholder="可选"
          />
        </Form.Item>
        <Form.Item label="发生工序">
          <Input
            v-model:value="sourceForm.processName"
            placeholder="请输入工序"
          />
        </Form.Item>
        <Form.Item label="物料编码">
          <Input
            v-model:value="sourceForm.materialCode"
            placeholder="请输入物料编码"
          />
        </Form.Item>
        <Form.Item label="物料名称" required>
          <Input
            v-model:value="sourceForm.materialName"
            placeholder="请输入物料名称"
          />
        </Form.Item>
        <Form.Item label="规格型号">
          <Input
            v-model:value="sourceForm.specification"
            placeholder="请输入规格"
          />
        </Form.Item>
        <Form.Item label="单位">
          <Input v-model:value="sourceForm.unitCode" placeholder="PCS/kg/m" />
        </Form.Item>
      </div>
    </Form>
  </Modal>
</template>
