<script lang="ts" setup>
import { computed } from 'vue';
import { Modal, Button, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const props = defineProps({
  visible: Boolean,
  reportData: { type: Object, required: true }
});
const emit = defineEmits(['update:visible']);

const htmlContent = computed(() => {
  const d = props.reportData;
  const matRows = d.materials.map((m: any) => `<tr><td>${m.mixBatch}</td><td>${m.product}</td><td>${m.actualWeight} kg</td><td>${m.time}</td></tr>`).join('');
  const checkRows = d.checks.map((c: any) => `<tr><td>${c.type}</td><td>${c.item}</td><td>${c.req}</td><td style="color:green;font-weight:bold">${c.checked ? '✔️ 已确认' : '-'}</td></tr>`).join('');
  const qcRows = d.qc.map((q: any) => `<tr><td>${q.time}</td><td>${q.type}</td><td>${q.mode}</td><td>${q.detail}</td><td style="font-weight:bold; color:${q.result==='合格'?'green':'red'}">${q.result}</td></tr>`).join('');

  return `
    <html><head><title>制造执行全景报告 (e-DHR)</title>
    <style>
      body { font-family: "Microsoft YaHei", sans-serif; font-size: 14px; line-height: 1.6; padding: 30px; color: #1e293b;}
      h1 { text-align: center; border-bottom: 3px solid #4f46e5; padding-bottom: 15px; color: #312e81; font-size: 28px;}
      h3 { background: #eef2ff; padding: 10px 15px; border-left: 5px solid #4f46e5; margin-top: 30px; font-size: 18px;}
      .grid-info { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 15px; font-size: 15px;}
      .grid-info div { border-bottom: 1px dashed #cbd5e1; padding: 8px 0;}
      table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 14px; }
      th, td { border: 1px solid #94a3b8; padding: 10px; text-align: center; }
      th { background-color: #f8fafc; font-weight: bold; color: #475569;}
    </style></head><body>
      <h1>制造执行综合报告 (e-DHR)</h1>
      <h3>📋 基础工单与产出摘要</h3>
      <div class="grid-info">
        <div><b>执行工单：</b> ${d.order?.id}</div><div><b>加工产品：</b> ${d.order?.product}</div>
        <div><b>工作站：</b> ${d.process?.station}</div><div><b>操作员：</b> ${d.operator?.userName}</div>
        <div><b>开工时间：</b> ${d.startTime}</div><div><b>报告生成：</b> ${new Date().toLocaleString()}</div>
        <div><b>合格产出总数：</b> <span style="color:green; font-weight:bold">${d.summary?.good} 件</span></div>
        <div><b>综合良率：</b> <span style="color:blue; font-weight:bold">${d.summary?.yieldRate}</span></div>
      </div>
      <h3>✅ 开机准备与环境点检</h3>
      <table><tr><th>检查类型</th><th>项目</th><th>规范要求</th><th>核对结果</th></tr>${checkRows || '<tr><td colspan="4">无记录</td></tr>'}</table>
      <h3>📦 物料投料追溯</h3>
      <table><tr><th>批次条码</th><th>物料名称</th><th>实际投料量</th><th>投料时间</th></tr>${matRows || '<tr><td colspan="4">无投料记录</td></tr>'}</table>
      <h3>🔬 品质检验台账汇总</h3>
      <table><tr><th>时间</th><th>检验类型</th><th>检测方</th><th>对象明细</th><th>最终判定</th></tr>${qcRows || '<tr><td colspan="5">无检验记录</td></tr>'}</table>
      <div style="margin-top: 60px; display: flex; justify-content: space-between; font-weight: bold; font-size: 16px;">
        <span>操作员确认签字：____________________</span><span>现场主管复核签字：____________________</span>
      </div>
    </body></html>`;
});

function handlePrint() {
  message.success('报告已发送至车间主打印机！');
  emit('update:visible', false);
}
</script>

<template>
  <Modal :open="visible" @update:open="$emit('update:visible', $event)" title="📄 生产全景电子履历" :width="1000" :footer="false" centered>
    <div class="p-4 bg-slate-100 rounded-xl mb-4 h-[700px] overflow-y-auto shadow-inner">
      <iframe :srcdoc="htmlContent" class="w-full h-full bg-white border border-slate-300 rounded-lg"></iframe>
    </div>
    <div class="flex justify-end gap-4 mt-6">
      <Button size="large" @click="$emit('update:visible', false)" class="font-bold">关闭预览</Button>
      <Button size="large" type="primary" class="bg-indigo-600 font-bold px-10 shadow-md" @click="handlePrint">
        <IconifyIcon icon="lucide:printer" class="mr-2" /> 确认打印并出档
      </Button>
    </div>
  </Modal>
</template>
