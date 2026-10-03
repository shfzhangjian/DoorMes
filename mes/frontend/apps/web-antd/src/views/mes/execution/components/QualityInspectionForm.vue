<script lang="ts" setup>
import { ref, computed } from 'vue';
import { Form, Select, InputNumber, Radio, Button, Table, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps({
  target: { type: Object, required: true }, // 检验对象(如: 中段 500米)
  type: { type: String, default: '自检' }
});
const emit = defineEmits(['submit', 'cancel']);

// 模拟读取系统的质量标准库
const standardOptions = [
  { value: 'STD-T01-01', label: 'T01 通用聚氨酯涂布质检标准 (V1.0)' },
  { value: 'STD-T01-02', label: 'T01 高透光学膜严格质检标准 (V2.0)' }
];
const selectedStandard = ref<string | null>(null);

// 模拟根据标准加载的检验指标 (参考上传源码结构)
const inspectionItems = ref<any[]>([]);

function handleStandardChange(val: string) {
  // 模拟接口请求加载指标
  inspectionItems.value = [
    { id: 'I1', name: '表面外观缺陷', type: 'QUALITATIVE', standard: '无气泡、无划痕', value: null, result: null },
    { id: 'I2', name: '涂层厚度 (mm)', type: 'QUANTITATIVE', standard: '0.15 ± 0.02', min: 0.13, max: 0.17, value: null, result: null },
    { id: 'I3', name: '剥离强度 (N/cm)', type: 'QUANTITATIVE', standard: '≥ 0.8', min: 0.8, max: 999, value: null, result: null }
  ];
}

const columns = [
  { title: '检验项目', dataIndex: 'name', width: 150 },
  { title: '标准要求', dataIndex: 'standard', width: 150 },
  { title: '实测数据录入', dataIndex: 'value', minWidth: 200 },
  { title: '单项判定', dataIndex: 'result', width: 100, align: 'center' }
];

// 自动判定逻辑
function evaluateItem(record: any) {
  if (record.type === 'QUANTITATIVE' && record.value !== null) {
    record.result = (record.value >= record.min && record.value <= record.max) ? 'PASS' : 'FAIL';
  } else if (record.type === 'QUALITATIVE' && record.value !== null) {
    record.result = record.value;
  }
}

// 综合判定
const overallResult = computed(() => {
  if (inspectionItems.value.length === 0) return null;
  const hasFail = inspectionItems.value.some(item => item.result === 'FAIL');
  const hasNull = inspectionItems.value.some(item => item.result === null);
  if (hasFail) return '不合格';
  if (hasNull) return null;
  return '合格';
});

const remark = ref('');

function handleSubmit() {
  if (overallResult.value === null) return;
  emit('submit', {
    detail: props.target.detail,
    result: overallResult.value,
    feedback: remark.value || '各项指标均符合标准',
    metrics: inspectionItems.value // 将指标快照一并提交给台账
  });
}
</script>

<template>
  <div class="flex flex-col h-full bg-white rounded-xl border border-slate-200 shadow-sm p-6 relative">
    <div class="flex justify-between items-center mb-6 border-b pb-4 shrink-0">
      <div>
        <div class="text-xl font-black text-slate-800 flex items-center gap-2">
          <IconifyIcon icon="lucide:clipboard-check" class="text-indigo-600"/> 执行详细检验
        </div>
        <div class="text-slate-500 mt-1">检验对象: <Tag color="blue">{{ target.detail }}</Tag></div>
      </div>
      <Button @click="emit('cancel')" size="large" class="font-bold">返回待检列表</Button>
    </div>

    <Form layout="vertical" class="shrink-0 mb-4">
      <Form.Item label="调用质量检验标准" required>
        <Select v-model:value="selectedStandard" :options="standardOptions" size="large" class="w-full text-lg font-bold" @change="handleStandardChange" placeholder="请选择检验标准模板..." />
      </Form.Item>
    </Form>

    <div class="flex-1-table-container border border-slate-200 rounded-lg overflow-hidden mb-6" v-if="inspectionItems.length > 0">
      <Table :columns="columns" :dataSource="inspectionItems" :pagination="false" :scroll="{ y: '100%' }" class="full-height-table text-base">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'value'">
            <InputNumber v-if="record.type === 'QUANTITATIVE'" v-model:value="record.value" class="w-full h-10 text-lg font-bold" placeholder="输入数值..." @change="evaluateItem(record)" />
            <Radio.Group v-else v-model:value="record.value" button-style="solid" class="flex" @change="evaluateItem(record)">
              <Radio.Button value="PASS" class="flex-1 text-center font-bold text-emerald-600">正常</Radio.Button>
              <Radio.Button value="FAIL" class="flex-1 text-center font-bold text-red-600">异常</Radio.Button>
            </Radio.Group>
          </template>
          <template v-if="column.dataIndex === 'result'">
            <Tag v-if="record.result === 'PASS'" color="success" class="text-sm font-bold">合格</Tag>
            <Tag v-else-if="record.result === 'FAIL'" color="error" class="text-sm font-bold">异常</Tag>
            <span v-else class="text-slate-300">-</span>
          </template>
        </template>
      </Table>
    </div>

    <div class="shrink-0 bg-slate-50 p-4 rounded-xl border border-slate-200">
      <div class="flex items-center justify-between mb-4">
        <span class="text-lg font-bold text-slate-700">系统综合判定结论：</span>
        <Tag v-if="overallResult === '合格'" color="success" class="text-2xl font-black px-6 py-1 !m-0">✔️ 合格</Tag>
        <Tag v-else-if="overallResult === '不合格'" color="error" class="text-2xl font-black px-6 py-1 !m-0">❌ 不合格</Tag>
        <Tag v-else color="default" class="text-lg font-bold px-6 py-1 !m-0 text-slate-400">待完善数据</Tag>
      </div>
      <Input.TextArea v-model:value="remark" placeholder="检验情况补充说明 (选填)..." :rows="2" class="text-lg mb-4" />
      <Button type="primary" size="large" class="w-full h-14 text-xl font-black rounded-xl bg-indigo-600 shadow-md" :disabled="!overallResult" @click="handleSubmit">
        确认提交并归档检验报告
      </Button>
    </div>
  </div>
</template>

<style scoped>
.flex-1-table-container { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column;}
.full-height-table { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }
.full-height-table :deep(.ant-table-wrapper), .full-height-table :deep(.ant-spin-nested-loading), .full-height-table :deep(.ant-spin-container), .full-height-table :deep(.ant-table), .full-height-table :deep(.ant-table-container) { height: 100%; display: flex; flex-direction: column; min-height: 0; }
.full-height-table :deep(.ant-table-body) { flex: 1; overflow-y: auto !important; min-height: 0; }
.full-height-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #64748b !important; font-size: 14px; font-weight: bold; position: sticky; top: 0; z-index: 10;}
</style>
