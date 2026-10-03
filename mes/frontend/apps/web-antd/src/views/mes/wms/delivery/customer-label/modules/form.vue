<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Form, Input, Select, Radio, InputNumber, Divider, message, Switch } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const formRef = ref();

const formData = ref({
  id: undefined,
  customerCode: '',
  customerName: '',
  ruleName: '',
  prefix: 'CUST-',
  includeDate: true,
  dateFmt: 'yyyyMMdd',
  includeInternalBatch: false,
  seqLen: 4,
  barcodeType: 'QR_CODE',
  printTemplate: 'TPL-STD-100x100',
  remark: '',
  status: 0
});

// 💡 实时条码映射引擎：根据开关动态拼接结果
const livePreview = computed(() => {
  let res = formData.value.prefix || '';
  if (formData.value.includeDate) {
    res += formData.value.dateFmt === 'yyyyMMdd' ? '20260219' : '260219';
  }
  if (formData.value.includeInternalBatch) {
    res += '-INT260219'; // 模拟原厂内部批次
  }
  if (formData.value.seqLen) {
    res += '-' + '1'.padStart(formData.value.seqLen, '0');
  }
  return res;
});

const [Modal, modalApi] = useVbenModal({
  title: computed(() => formData.value.id ? '编辑客户专属标签规则' : '新增客户专属标签规则'),
  fullscreenButton: true,
  async onConfirm() {
    await formRef.value?.validate();
    message.success('规则保存成功！');
    emit('success');
    modalApi.close();
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      if (data) {
        formData.value = { ...data };
      } else {
        formData.value = { id: undefined, customerCode: '', customerName: '', ruleName: '', prefix: 'CUST-', includeDate: true, dateFmt: 'yyyyMMdd', includeInternalBatch: false, seqLen: 4, barcodeType: 'QR_CODE', printTemplate: 'TPL-STD-100x100', remark: '', status: 0 };
      }
    }
  }
});
</script>

<template>
  <Modal class="w-[800px]">
    <div class="p-2">

      <div class="mb-6 bg-slate-100 border-2 border-dashed border-slate-300 rounded-xl p-6 flex items-center justify-center relative shadow-inner">
        <div class="absolute top-2 left-3 text-xs font-bold tracking-widest text-slate-400">LABEL MAPPING PREVIEW</div>

        <div class="bg-white border border-slate-200 shadow-md p-4 rounded flex items-center gap-6 min-w-[400px]">
          <IconifyIcon v-if="formData.barcodeType === 'QR_CODE'" icon="lucide:qr-code" class="text-7xl text-slate-800" />
          <IconifyIcon v-else icon="lucide:barcode" class="text-7xl text-slate-800" />

          <div class="flex flex-col gap-1">
            <span class="font-bold text-slate-500 text-sm tracking-wider">{{ formData.customerName || '客户名称 (CUSTOMER)' }}</span>
            <span class="font-mono text-2xl text-slate-800 font-black tracking-widest">{{ livePreview || '------------' }}</span>
          </div>
        </div>
      </div>

      <Form ref="formRef" :model="formData" layout="vertical">
        <Divider class="my-2 border-slate-200"><span class="text-xs text-slate-400 font-bold">1. 基础映射信息</span></Divider>
        <div class="grid grid-cols-2 gap-x-6">
          <Form.Item label="目标客户" required name="customerName">
            <Input v-model:value="formData.customerName" placeholder="如 宁德时代" />
          </Form.Item>
          <Form.Item label="规则名称" required name="ruleName">
            <Input v-model:value="formData.ruleName" placeholder="如 CATL外箱码规则" />
          </Form.Item>
        </div>

        <Divider class="my-2 border-slate-200"><span class="text-xs text-slate-400 font-bold">2. 标签编码逻辑拼接 (Mapping Logic)</span></Divider>
        <div class="bg-indigo-50/50 p-4 rounded-lg border border-indigo-100 grid grid-cols-2 gap-x-6 gap-y-4">

          <Form.Item label="客户强制前缀 (Prefix)" name="prefix" class="mb-0">
            <Input v-model:value="formData.prefix" placeholder="如 CATL-V001-" class="font-mono" />
          </Form.Item>

          <Form.Item label="尾部流水号长度 (SN)" required name="seqLen" class="mb-0">
            <InputNumber v-model:value="formData.seqLen" :min="1" :max="8" class="w-full" />
          </Form.Item>

          <div class="col-span-2 grid grid-cols-2 gap-4 mt-2 p-3 bg-white rounded border border-slate-200">
            <div class="flex items-center justify-between">
              <span class="font-bold text-slate-700">条码中是否包含日期？</span>
              <Switch v-model:checked="formData.includeDate" />
            </div>
            <Form.Item v-if="formData.includeDate" label="日期格式" name="dateFmt" class="mb-0">
              <Select v-model:value="formData.dateFmt" :options="[{label:'四位年份 (yyyyMMdd)', value:'yyyyMMdd'}, {label:'两位年份 (yyMMdd)', value:'yyMMdd'}]" size="small"/>
            </Form.Item>
          </div>

          <div class="col-span-2 flex items-center justify-between p-3 bg-white rounded border border-slate-200">
            <div>
              <div class="font-bold text-slate-700">是否拼接内部原始批次号？</div>
              <div class="text-[10px] text-slate-400 mt-1">若开启，条码中将直接暴露我厂内部批次信息以供联合追溯。</div>
            </div>
            <Switch v-model:checked="formData.includeInternalBatch" />
          </div>

        </div>

        <Divider class="my-4 border-slate-200"><span class="text-xs text-slate-400 font-bold">3. 打印指令控制</span></Divider>
        <div class="grid grid-cols-2 gap-x-6">
          <Form.Item label="生成载体类型" required name="barcodeType">
            <Radio.Group v-model:value="formData.barcodeType" button-style="solid" class="w-full flex">
              <Radio.Button value="QR_CODE" class="flex-1 text-center"><IconifyIcon icon="lucide:qr-code" class="inline mr-1"/>生成二维码</Radio.Button>
              <Radio.Button value="CODE_128" class="flex-1 text-center"><IconifyIcon icon="lucide:barcode" class="inline mr-1"/>生成一维码</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="绑定打印模板 (ZPL/TSPL指令)" name="printTemplate">
            <Select v-model:value="formData.printTemplate" :options="[{label:'标准成品标-100*100', value:'TPL-STD-100x100'}, {label:'客户定制防伪标-80*60', value:'TPL-BYD-80x60'}]" />
          </Form.Item>
        </div>

        <Form.Item label="业务备注说明" name="remark" class="mt-2 mb-0">
          <Input.TextArea v-model:value="formData.remark" :rows="2" placeholder="发货贴签注意事项..." />
        </Form.Item>
      </Form>
    </div>
  </Modal>
</template>
