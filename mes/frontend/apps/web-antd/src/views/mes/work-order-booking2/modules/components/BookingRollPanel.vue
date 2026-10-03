<script lang="ts" setup>
import { ref, computed } from 'vue';
import { Button, InputNumber, Select, message } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { DEFECT_CODES } from '../../data';

const props = defineProps<{ startForm: any }>();

const rollCuts = ref<{id: string, from: number, length: number, sn: string, wipBin: string, status: 'OK'|'NG', defectCode?: string, spec?: string}[]>([]);
const remainMotherLength = computed(() => (props.startForm.motherLength || 0) - rollCuts.value.reduce((sum, item) => sum + (item.length || 0), 0));

const addRollCut = () => {
  const lastFrom = rollCuts.value.length > 0 ? rollCuts.value[rollCuts.value.length - 1].from + rollCuts.value[rollCuts.value.length - 1].length : 0;
  rollCuts.value.push({ id: Date.now().toString(), from: lastFrom, length: 0, sn: `R-SN${Date.now().toString().slice(-6)}`, wipBin: `BIN-${Math.floor(Math.random()*100)}`, status: 'OK' });
};
const removeRollCut = (index: number) => rollCuts.value.splice(index, 1);

// 暴露给父组件的验证与取值方法
const validate = () => {
  if (rollCuts.value.some(c => c.status === 'NG' && !c.defectCode)) {
    message.error('防呆拦截：存在被标记为不良的卷材，必须选择不良代码！');
    return null;
  }
  return {
    finalGood: rollCuts.value.filter(c => c.status === 'OK').length,
    finalScrap: rollCuts.value.filter(c => c.status === 'NG').length
  };
};

defineExpose({ validate });
</script>

<template>
  <div class="flex flex-col h-full min-h-0 w-full pr-4">
    <div class="p-4 bg-slate-50 border border-slate-200 rounded-t-lg flex justify-between items-center shrink-0">
      <div class="text-sm text-slate-700">来源母卷: <span class="font-bold text-indigo-600">{{ startForm.motherBatchNo }}</span> | 总长: <span class="font-bold">{{ startForm.motherLength }}</span> 米 | 余卷剩余: <span class="text-red-500 font-bold text-lg mx-1">{{ remainMotherLength }}</span> 米</div>
      <div class="flex gap-2">
        <Button v-if="startForm.process === '分切'" type="primary" danger ghost size="small">一键余卷退回线边库</Button>
        <Button type="primary" @click="addRollCut">➕ 增开新的一段</Button>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto custom-table-wrapper border border-t-0 border-slate-200 rounded-b-lg bg-slate-50/50">
      <table class="w-full text-center border-collapse text-sm bg-white">
        <thead class="bg-slate-100 text-slate-600 sticky top-0 z-10 shadow-sm">
        <tr><th class="p-2 border-b">从(米)</th><th class="p-2 border-b">长度(米)</th><th class="p-2 border-b">流水号</th><th class="p-2 border-b">边库号</th><th v-if="['精磨','分切'].includes(startForm.process)" class="p-2 border-b w-28">型号</th><th class="p-2 border-b w-24">自检判定</th><th class="p-2 border-b w-32">不良代码</th><th class="p-2 border-b w-10">删</th></tr>
        </thead>
        <tbody>
        <tr v-for="(cut, idx) in rollCuts" :key="cut.id" class="border-b border-slate-100 hover:bg-slate-50">
          <td class="p-1"><InputNumber v-model:value="cut.from" size="small" class="w-full text-center font-bold"/></td>
          <td class="p-1"><InputNumber v-model:value="cut.length" size="small" class="w-full text-center text-indigo-600 font-bold"/></td>
          <td class="p-1 font-mono text-xs font-bold">{{ cut.sn }}</td>
          <td class="p-1 font-mono text-xs text-blue-500 font-bold">{{ cut.wipBin }}</td>
          <td v-if="['精磨','分切'].includes(startForm.process)" class="p-1"><Select v-model:value="cut.spec" size="small" class="w-full font-bold" :options="[{label:'P规格',value:'P'},{label:'Q规格',value:'Q'},{label:'研发损耗',value:'RD'}]" /></td>
          <td class="p-1"><Select v-model:value="cut.status" size="small" class="w-full font-bold text-center" :class="cut.status==='OK'?'text-emerald-600':'text-red-500'" :options="[{label:'OK',value:'OK'},{label:'NG',value:'NG'}]" /></td>
          <td class="p-1"><Select v-if="cut.status==='NG'" v-model:value="cut.defectCode" size="small" class="w-full" :options="DEFECT_CODES" placeholder="必填"/></td>
          <td class="p-1"><Button type="text" danger size="small" @click="removeRollCut(idx)"><IconifyIcon icon="lucide:trash"/></Button></td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.custom-table-wrapper { display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-wrapper), .custom-table-wrapper :deep(.ant-spin-nested-loading), .custom-table-wrapper :deep(.ant-spin-container), .custom-table-wrapper :deep(.ant-table) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-container) { flex: 1; overflow-y: auto; }
</style>
