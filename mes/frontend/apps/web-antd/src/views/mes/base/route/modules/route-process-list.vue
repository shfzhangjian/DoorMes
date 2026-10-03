<script lang="ts" setup>
import { ref, watch } from 'vue';
import { Button, Modal, InputNumber, Select } from 'ant-design-vue';
import { Plus, Trash2, ArrowUp, ArrowDown } from '@vben/icons';
import ProcessSelectModal from './process-select-modal.vue';
import { getProcess } from '#/api/mes/base/process';

const props = defineProps<{
  data: any[],
  currentId?: number | string
}>();

const emit = defineEmits(['update:data', 'row-click']);
const processSelectRef = ref();

watch(() => props.data, (val) => {
  if (val) {
    val.forEach((item, index) => {
      if (!item.id && !item._tempId) item._tempId = `temp_${Date.now()}_${index}`;
    });
  }
}, { deep: true, immediate: true });

function handleAdd() { processSelectRef.value?.open(); }

// 🔥 变更为 async 函数，以支持详情数据的等待拉取
async function handleProcessSelect(selectedList: any[]) {
  let maxSeq = 0;
  props.data.forEach(item => { if (item.sequence > maxSeq) maxSeq = item.sequence; });

  const newRows = [];

  // 🔥 改用 for...of 循环，逐个拉取选中工序的深度详情
  for (let i = 0; i < selectedList.length; i++) {
    const item = selectedList[i];

    try {
      // 🌟 核心修复：拿着浅层列表的 ID，去后端请求带有完整子表的详情数据
      const detail = await getProcess(item.id);

      newRows.push({
        _tempId: `new_${Date.now()}_${i}`,
        processId: detail.id,
        processCode: detail.code,
        processName: detail.name,
        processType: detail.processType,
        workshopName: detail.workshopName,

        splitRuleType: detail.splitRuleType,
        maxSplitQty: detail.maxSplitQty,
        bindStation: detail.bindStation,

        // 🌟 完美继承：将标准工序的子表数据深度克隆过来，注意去掉原有的主键ID避免冲突
        stations: (detail.stations || []).map((s:any) => ({...s, id: undefined})),
        params: (detail.params || []).map((p:any) => ({...p, id: undefined})),
        posts: (detail.posts || []).map((p:any) => ({...p, id: undefined})),
        sops: (detail.sops || []).map((s:any) => ({...s, id: undefined})),

        sequence: maxSeq + (i + 1) * 10,
        nodeType: (i === 0 && props.data.length === 0) ? 'START' : 'NORMAL',
        yieldRate: 100,
        keyNode: false,
      });
    } catch (error) {
      console.error(`拉取工序 [${item.name}] 详情失败`, error);
    }
  }

  emit('update:data', [...props.data, ...newRows]);
  if (newRows.length > 0) emit('row-click', newRows[0]);
}

function handleRemove(index: number) {
  Modal.confirm({
    title: '确认删除', content: '确定要删除该工序吗？对应的参数也将被移除。',
    onOk: () => {
      const newData = [...props.data];
      const item = newData[index];
      const isDeletingCurrent = (item.id && item.id === props.currentId) || (item._tempId && item._tempId === props.currentId);
      newData.splice(index, 1);
      emit('update:data', newData);
      if (isDeletingCurrent) emit('row-click', null);
    }
  });
}

function handleMove(index: number, dir: 'up' | 'down') {
  if (dir === 'up' && index === 0) return;
  if (dir === 'down' && index === props.data.length - 1) return;
  const newData = [...props.data];
  const targetIndex = dir === 'up' ? index - 1 : index + 1;
  const temp = newData[targetIndex];
  newData[targetIndex] = newData[index];
  newData[index] = temp;
  newData.forEach((item, idx) => { item.sequence = (idx + 1) * 10; });
  emit('update:data', newData);
}

function handleCardClick(item: any) { emit('row-click', item); }
</script>

<template>
  <div class="flex flex-col h-full bg-white border-r border-gray-200 overflow-hidden">
    <div class="flex justify-between items-center px-4 py-2 border-b flex-shrink-0 h-12 bg-gray-50">
      <span class="font-bold text-gray-700 text-[15px]">工序流程编排 ({{ data.length }})</span>
      <Button type="primary" size="small" @click="handleAdd" class="bg-indigo-600 border-none">
        <template #icon><Plus class="w-4 h-4" /></template> 添加工序
      </Button>
    </div>

    <div class="flex-1 overflow-y-auto p-3 space-y-4 bg-gray-100/40">
      <div
        v-for="(item, index) in data"
        :key="item.id || item._tempId"
        class="relative p-3.5 bg-white rounded-xl border cursor-pointer transition-all hover:shadow-md group flex flex-col gap-2.5"
        :class="[ (item.id && item.id === currentId) || (item._tempId && item._tempId === currentId) ? 'border-indigo-500 ring-2 ring-indigo-100 shadow-md' : 'border-gray-200' ]"
        @click="handleCardClick(item)"
      >
        <div class="absolute -top-2.5 -left-2.5 w-7 h-7 rounded-full bg-slate-700 text-white flex items-center justify-center text-sm font-bold shadow-sm z-10 font-mono border-2 border-white">
          {{ index + 1 }}
        </div>

        <div class="pl-2">
          <div class="font-black text-base text-gray-800 break-words mb-3 mt-1">{{ item.processName }}</div>

          <div class="flex text-xs text-slate-500 bg-slate-50 py-2 px-2.5 rounded mb-3 border border-slate-100 items-center justify-between">
            <div class="flex-1 truncate" :title="index > 0 ? data[index - 1].processName : '起始工序'">
              <span class="text-slate-400">前置工序:</span> <span class="font-bold text-slate-700 ml-1">{{ index > 0 ? data[index - 1].processName : '无 (起点)' }}</span>
            </div>
            <div class="text-slate-300 mx-2">|</div>
            <div class="flex-1 truncate text-right" :title="index < data.length - 1 ? data[index + 1].processName : '末道工序'">
              <span class="text-slate-400">后置工序:</span> <span class="font-bold text-slate-700 ml-1">{{ index < data.length - 1 ? data[index + 1].processName : '无 (终点)' }}</span>
            </div>
          </div>

          <div class="flex flex-wrap gap-2 text-xs mb-3">
            <span class="font-mono bg-slate-100 text-slate-600 px-2 py-1 rounded border border-slate-200">编码: <b>{{ item.processCode }}</b></span>
            <span class="bg-indigo-50 text-indigo-700 px-2 py-1 rounded border border-indigo-100">类型: <b>{{ item.processType || '未定义' }}</b></span>
            <span class="bg-amber-50 text-amber-700 px-2 py-1 rounded border border-amber-100">车间: <b>{{ item.workshopName || '未定义' }}</b></span>
          </div>

          <div class="bg-[#f0f7ff] p-2.5 rounded-lg border border-[#cce3ff] mb-3">
            <div class="flex items-center gap-2 mb-2">
              <span class="text-xs font-black text-blue-900">⚙️ APS 引擎拆解基准:</span>
              <span class="text-xs text-blue-700 font-bold bg-white px-2 py-0.5 rounded border border-blue-200 shadow-sm">
                {{ item.splitRuleType === 'CAPACITY' ? '📦 容量上限强切' : (item.splitRuleType === 'CARRIER' ? '🛒 载具满载强切' : (item.splitRuleType === 'CONTINUOUS' ? '🌊 连续流直通' : '未配置规则')) }}
                <span v-if="item.splitRuleType !== 'CONTINUOUS' && item.maxSplitQty" class="text-red-500 ml-1">({{ item.maxSplitQty }})</span>
              </span>
            </div>
            <div class="text-xs text-blue-800/80 leading-relaxed bg-white/50 p-1.5 rounded">
              <span class="text-amber-600 font-bold mr-1">💡 算力推演:</span>
              <span v-if="item.splitRuleType === 'CAPACITY'">受容量防错约束，该工序总产量将按最大 <b>{{ item.maxSplitQty || 'X' }}</b> 强制裂变为多张微观子单下发。</span>
              <span v-else-if="item.splitRuleType === 'CARRIER'">受物理载具约束，该工序总产量将按满载 <b>{{ item.maxSplitQty || 'X' }}</b> 强制裂变为多张子单。</span>
              <span v-else-if="item.splitRuleType === 'CONTINUOUS'">该工序适用卷对卷/流水线，引擎不主动拆分，生成一单到底的连续流指令。</span>
              <span v-else>请在【标准工序库】中维护该工序的拆单规则。</span>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-2.5">
            <div class="flex items-center gap-2 bg-slate-50 p-1.5 rounded border border-slate-200">
              <span class="text-xs font-bold text-slate-600 w-12 text-right shrink-0">流转号:</span>
              <InputNumber v-model:value="item.sequence" size="small" :min="10" :step="10" class="w-full text-sm font-bold" @click.stop />
            </div>
            <div class="flex items-center gap-2 bg-slate-50 p-1.5 rounded border border-slate-200">
              <span class="text-xs font-bold text-slate-600 w-10 text-right shrink-0">节点:</span>
              <Select v-model:value="item.nodeType" size="small" class="w-full text-sm font-bold" @click.stop>
                <Select.Option value="START"><span class="text-green-600">🟢 首工序</span></Select.Option>
                <Select.Option value="NORMAL"><span class="text-blue-600">🔵 常规流转</span></Select.Option>
                <Select.Option value="INSPECT"><span class="text-orange-600">🟠 质量控制</span></Select.Option>
                <Select.Option value="END"><span class="text-slate-600">🏁 末段入库</span></Select.Option>
              </Select>
            </div>
            <div class="flex items-center gap-2 bg-slate-50 p-1.5 rounded border border-slate-200 col-span-2">
              <span class="text-xs font-bold text-slate-600 w-12 text-right shrink-0">良率:</span>
              <InputNumber v-model:value="item.yieldRate" size="small" :min="0.1" :max="100" class="w-24 text-emerald-600 font-black text-sm" @click.stop />
              <span class="text-xs font-bold text-slate-500">% (排产逆向投料计算基准)</span>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center pl-2 pr-1 mt-3 pt-3 border-t border-slate-100">
          <div class="flex gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
            <Button size="small" type="default" class="text-xs text-gray-600 bg-gray-50 hover:bg-gray-100 border-gray-200" @click.stop="handleMove(index, 'up')" :disabled="index === 0" title="向上调整工序">
              <template #icon><ArrowUp class="w-3.5 h-3.5" /></template> 上移
            </Button>
            <Button size="small" type="default" class="text-xs text-gray-600 bg-gray-50 hover:bg-gray-100 border-gray-200" @click.stop="handleMove(index, 'down')" :disabled="index === data.length - 1" title="向下调整工序">
              <template #icon><ArrowDown class="w-3.5 h-3.5" /></template> 下移
            </Button>
            <Button size="small" danger type="primary" class="text-xs ml-2 shadow-sm" @click.stop="handleRemove(index)" title="删除此工序">
              <template #icon><Trash2 class="w-3.5 h-3.5" /></template> 移除
            </Button>
          </div>
        </div>
      </div>

      <div v-if="!data || data.length === 0" class="h-full flex flex-col items-center justify-center text-slate-400 py-10 opacity-50">
        <div class="text-5xl mb-3">📋</div>
        <div class="text-base font-bold">尚未编排工序流程</div>
        <div class="text-sm mt-1">请点击右上方按钮从标准工序库中引入</div>
      </div>
    </div>
    <ProcessSelectModal ref="processSelectRef" @select="handleProcessSelect" />
  </div>
</template>

<style scoped>
:deep(.ant-select-selector) { padding: 0 4px !important; }
:deep(.ant-input-number-input) { padding: 0 4px !important; font-weight: bold; }
</style>
