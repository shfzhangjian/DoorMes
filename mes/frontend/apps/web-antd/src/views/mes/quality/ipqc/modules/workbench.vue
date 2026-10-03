<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Button, InputNumber, Tag, message, Modal, Card, Select, Drawer } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getStandardByMachine, submitIpqcRecord, getActiveMachines, type MesIpqcApi } from '#/api/mes/quality/ipqc';

const emit = defineEmits(['back-to-ledger']);
const clone = (data: any) => JSON.parse(JSON.stringify(data));

const activeRecord = ref<MesIpqcApi.IpqcRecord | null>(null);
const activeMachines = ref<MesIpqcApi.ActiveMachine[]>([]);
let radarTimer: any = null;
const currentTime = ref(Date.now());

// 💡 新增：多任务挂起队列 (临时保存)
const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

onMounted(async () => {
  activeMachines.value = await getActiveMachines();
  radarTimer = setInterval(() => { currentTime.value = Date.now(); }, 10000); // 每10秒刷新雷达时间
});
onUnmounted(() => { clearInterval(radarTimer); });

// 💡 挂起当前任务 (临时保存)
function handleSuspendTask() {
  if (!activeRecord.value) return;

  suspendedTasks.value.push({
    id: Date.now(),
    record: clone(activeRecord.value),
    gridData: clone(gridApi.grid!.getTableData().fullData),
    suspendTime: new Date().toLocaleTimeString()
  });

  // 挂起后从左侧雷达视图中暂时移除
  activeMachines.value = activeMachines.value.filter(m => m.machineCode !== activeRecord.value!.machineCode);

  activeRecord.value = null;
  gridApi.setGridOptions({ data: [] });
  message.success('当前巡检任务已临时保存挂起！您可以先去处理其他紧急机台。');
}

// 💡 恢复挂起任务
function handleResumeTask(index: number) {
  if (activeRecord.value) return message.warning('当前已有执行中的巡检，请先挂起或提交结案！');

  const task = suspendedTasks.value[index];
  activeRecord.value = clone(task.record);
  gridApi.setGridOptions({ data: task.gridData });

  // 恢复到左侧雷达中
  activeMachines.value.unshift(task.record as any);

  suspendedTasks.value.splice(index, 1);
  suspendedDrawerVisible.value = false;
  message.success(`机台 ${activeRecord.value!.machineCode} 的巡检任务已恢复！`);
}

// 计算机器 SLA 状态
function getSlaStatus(time: number) {
  const diffMinutes = Math.floor((time - currentTime.value) / 60000);
  if (diffMinutes < 0) return { color: 'error', text: `超时漏检 ${Math.abs(diffMinutes)} 分钟`, ring: 'ring-red-500 bg-red-50' };
  if (diffMinutes <= 30) return { color: 'warning', text: `即将超时 (剩${diffMinutes}分钟)`, ring: 'ring-orange-400 bg-orange-50' };
  return { color: 'success', text: `正常 (剩${diffMinutes}分钟)`, ring: 'border-slate-200 bg-white' };
}

async function handleSelectMachine(machine: MesIpqcApi.ActiveMachine) {
  if (activeRecord.value && activeRecord.value.machineCode !== machine.machineCode) {
    Modal.confirm({
      title: '切换机台',
      content: '未提交的数据将丢失，建议先点击右上角【临时挂起】。确认强制前往新机台？',
      onOk: () => loadData(machine)
    });
  } else loadData(machine);
}

async function loadData(machine: MesIpqcApi.ActiveMachine) {
  activeRecord.value = { ...machine, ipqcNo: '', inspectionType: 'ROUTINE', inspector: '当前登录人', judgment: '-', items: [] };
  const hide = message.loading(`拉取机台 ${machine.machineCode} 的实时工艺卡与检验标准...`, 0);
  try {
    const items = await getStandardByMachine(machine.machineCode, machine.materialCode);
    items.forEach((it: any) => { it.sampleValues = new Array(it.sampleSize).fill(undefined); });
    gridApi.setGridOptions({ data: items });
    evaluateOverall();
  } finally { hide(); }
}

const [ItemGrid, gridApi] = useVbenVxeGrid<MesIpqcApi.IpqcItem>({
  gridOptions: {
    border: true, showOverflow: true, keepSource: true, height: 'auto',
    pagerConfig: { enabled: false }, toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 50, align: 'center' },
      { field: 'category', title: '维度', width: 80, align: 'center', slots: { default: 'category' } },
      { field: 'inspectionItem', title: '检验项目 (点位/工艺参数)', minWidth: 160 },
      { field: 'standardDesc', title: '标准/公差要求', width: 140 },
      { field: 'sampleValues', title: '实测数据录入区', width: 220, slots: { default: 'sampleValues' } },
      { field: 'itemResult', title: '判定', width: 80, align: 'center', fixed: 'right', slots: { default: 'itemResult' } },
    ],
    data: [],
  }
});

function handleValueChange(row: MesIpqcApi.IpqcItem) {
  const validValues = row.sampleValues!.filter(v => v !== undefined && v !== null && v !== '');

  if (validValues.length < row.sampleSize) {
    row.itemResult = '-';
  } else {
    if (row.itemType === 'QUANTITATIVE') {
      const numVals = validValues.map(Number);
      row.maxValue = Math.max(...numVals);
      row.minValue = Math.min(...numVals);
      row.averageValue = Number((numVals.reduce((a, b) => a + b, 0) / numVals.length).toFixed(3));
      row.itemResult = (row.maxValue <= row.maxValueLimit! && row.minValue >= row.minValueLimit!) ? 'OK' : 'NG';
    } else {
      row.itemResult = validValues.includes('NG') ? 'NG' : 'OK';
    }
  }
  evaluateOverall();
}

function isOutOfRange(row: MesIpqcApi.IpqcItem, value: any) {
  if (row.itemType !== 'QUANTITATIVE') return value === 'NG';
  const numericValue = Number(value);
  return numericValue > (row.maxValueLimit ?? Number.POSITIVE_INFINITY) || numericValue < (row.minValueLimit ?? Number.NEGATIVE_INFINITY);
}

function evaluateOverall() {
  if (!gridApi.grid || !activeRecord.value) return;
  const allData = gridApi.grid.getTableData().fullData as MesIpqcApi.IpqcItem[];
  let hasNg = false; let hasPending = false;
  allData.forEach(item => { if (item.itemResult === 'NG') hasNg = true; if (item.itemResult === '-') hasPending = true; });
  activeRecord.value.judgment = hasNg ? 'NG' : (!hasPending ? 'OK' : '-');
}

// N次录入弹窗
const sampleModalVisible = ref(false);
const currentSampleRow = ref<MesIpqcApi.IpqcItem | null>(null);

function openSampleInput(row: MesIpqcApi.IpqcItem) { currentSampleRow.value = row; sampleModalVisible.value = true; }
function saveSampleInput() { handleValueChange(currentSampleRow.value!); sampleModalVisible.value = false; }

async function handleSubmit() {
  const rec = activeRecord.value!;
  rec.items = gridApi.grid!.getTableData().fullData;

  if (rec.judgment === '-') return message.warning('请录入所有实测项目数据！');

  if (rec.judgment === 'NG') {
    Modal.confirm({
      title: '🚨 IPQC 巡检发现异常！',
      content: '当前机台存在不合格项。提交后将自动联动【异常事件提报】并生成 NCR。是否需要系统同步下发指令【暂停当前机台的工单生产】？',
      okText: '停机并提报异常', cancelText: '仅提报异常 (不停机)', okType: 'danger',
      onOk: async () => { await doSubmit(true); },
      onCancel: async () => { await doSubmit(false); }
    });
  } else {
    await doSubmit(false);
  }
}

async function doSubmit(pauseMachine: boolean) {
  await submitIpqcRecord(activeRecord.value!);
  message.success(pauseMachine ? 'IPQC 提交成功！已锁定机台并生成异常单。' : 'IPQC 提交成功！机台运转受控。');

  const target = activeMachines.value.find(m => m.machineCode === activeRecord.value!.machineCode);
  if (target) { target.nextInspectTime = Date.now() + 120 * 60000; target.lastInspectStatus = activeRecord.value!.judgment as any; }

  activeRecord.value = null; gridApi.setGridOptions({ data: [] });
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] z-50 overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-indigo-600 text-white p-1.5 rounded-lg"><IconifyIcon icon="lucide:activity" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">IPQC 车间巡检雷达工作台</span>
      </div>

      <div class="flex gap-3">
        <Button type="primary" ghost class="font-bold border-indigo-200" @click="suspendedDrawerVisible = true">
          <IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列 <span v-if="suspendedTasks.length>0" class="ml-1 text-xs bg-indigo-100 px-1 rounded">{{suspendedTasks.length}}</span>
        </Button>
        <Button type="dashed" @click="emit('back-to-ledger')"><IconifyIcon icon="lucide:log-out" class="mr-1"/> 退出</Button>
      </div>
    </header>

    <main class="flex-1 flex gap-3 p-3 overflow-hidden min-h-0">
      <div class="w-[360px] flex flex-col gap-3 min-h-0 shrink-0">
        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden flex flex-col shadow-sm min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex justify-between items-center">
            <span class="flex items-center gap-1"><IconifyIcon icon="lucide:radar" class="text-indigo-600"/> 运行机台巡检雷达</span>
          </div>
          <div class="flex-1 overflow-y-auto p-3 custom-scrollbar flex flex-col gap-2 bg-slate-50/50">
            <Card v-for="m in activeMachines" :key="m.machineCode" size="small" class="cursor-pointer transition-all hover:shadow-md ring-1" :class="getSlaStatus(m.nextInspectTime).ring + (activeRecord?.machineCode === m.machineCode ? ' shadow-md border-indigo-500' : ' border-transparent')" @click="handleSelectMachine(m)">
              <div class="flex justify-between items-center mb-2">
                <span class="font-mono font-black text-slate-800 text-base flex items-center gap-1">
                  <span class="w-2 h-2 rounded-full animate-ping" :class="'bg-' + getSlaStatus(m.nextInspectTime).color.replace('error','red').replace('warning','orange').replace('success','green') + '-500'"></span>
                  {{ m.machineCode }}
                </span>
                <Tag :color="getSlaStatus(m.nextInspectTime).color" class="!m-0 font-bold border-none">{{ getSlaStatus(m.nextInspectTime).text }}</Tag>
              </div>
              <div class="text-[11px] font-bold text-slate-700 truncate mb-1">正在生产: {{ m.materialName }}</div>
              <div class="text-[10px] text-slate-500 font-mono flex justify-between"><span>工单: {{ m.workOrderNo }}</span> <span>上次: <span :class="m.lastInspectStatus === 'OK' ? 'text-green-600' : m.lastInspectStatus === 'NG' ? 'text-red-600' : ''">{{ m.lastInspectStatus === 'NONE' ? '无' : m.lastInspectStatus }}</span></span></div>
            </Card>
          </div>
        </div>
      </div>

      <div class="flex-1 flex flex-col gap-3 min-h-0 relative">
        <div v-if="!activeRecord" class="absolute inset-0 bg-white/80 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center">
          <IconifyIcon icon="lucide:radar" class="text-6xl mb-4 opacity-20 text-indigo-500" />
          <span class="font-bold text-xl text-slate-700">根据左侧雷达指示，前往目标机台打卡录入</span>
        </div>

        <div v-if="activeRecord" class="bg-white p-4 rounded-lg border shadow-sm flex items-center justify-between">
          <div class="flex items-center gap-6">
            <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">巡视机台</span><span class="font-black text-indigo-800 text-lg">{{ activeRecord.machineCode }}</span></div>
            <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">工单号</span><span class="font-mono text-slate-700 font-bold bg-slate-100 px-1 border">{{ activeRecord.workOrderNo }}</span></div>
            <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">在制产品</span><span class="font-bold text-slate-800 text-sm">{{ activeRecord.materialName }}</span></div>
          </div>
          <Button type="primary" class="bg-orange-500 border-none shadow-sm shadow-orange-200 font-bold text-xs" @click="handleSuspendTask">
            <IconifyIcon icon="lucide:pause-circle" class="mr-1" /> 临时保存 / 挂起本单
          </Button>
        </div>

        <div v-if="activeRecord" class="flex-1 bg-white rounded-lg border shadow-sm flex flex-col min-h-0">
          <ItemGrid class="flex-1 p-2">
            <template #category="{ row }">
              <Tag :color="row.category === 'PRODUCT' ? 'blue' : 'orange'" class="!m-0 text-[10px]">{{ row.category === 'PRODUCT' ? '产品质量' : '工艺参数' }}</Tag>
            </template>

            <template #sampleValues="{ row }">
              <div v-if="row.sampleSize === 1" class="flex gap-2 items-center">
                <InputNumber v-if="row.itemType === 'QUANTITATIVE'" v-model:value="row.sampleValues[0]" size="small" class="flex-1 text-center" @blur="handleValueChange(row)" placeholder="表盘读数" />
                <Select v-else v-model:value="row.sampleValues[0]" size="small" class="flex-1" :options="[{label:'合格(OK)',value:'OK'},{label:'异常(NG)',value:'NG'}]" @change="handleValueChange(row)" placeholder="目测判定" />
              </div>
              <div v-else class="w-full">
                <Button size="small" type="dashed" class="w-full text-indigo-600 bg-indigo-50 border-indigo-300" @click="openSampleInput(row)">
                  <IconifyIcon icon="lucide:keyboard" class="mr-1" /> 录入 {{row.sampleSize}} 个抽测件数据
                </Button>
                <div v-if="row.itemResult !== '-'" class="flex flex-wrap gap-1 mt-1 justify-center">
                  <div v-for="(v, i) in row.sampleValues" :key="i" class="min-w-[20px] px-1 h-4 flex items-center justify-center text-[10px] font-mono rounded" :class="isOutOfRange(row, v) ? 'bg-red-100 text-red-600' : 'bg-green-100 text-green-700'">
                    {{ v === 'OK' ? 'O' : v === 'NG' ? 'X' : v }}
                  </div>
                </div>
              </div>
            </template>
            <template #itemResult="{ row }"><span v-if="row.itemResult==='OK'" class="text-green-600 font-black">⭕ OK</span><span v-else-if="row.itemResult==='NG'" class="text-red-600 font-black animate-pulse">❌ NG</span><span v-else class="text-slate-300">-</span></template>
          </ItemGrid>
        </div>

        <div v-if="activeRecord" class="h-16 bg-white rounded-lg border shadow-sm flex items-center justify-between px-6">
          <div class="flex items-center gap-4">
            <span class="font-bold text-slate-500">机台巡检状态：</span>
            <Tag v-if="activeRecord.judgment === 'OK'" color="success" class="text-xl px-4 py-1 font-black">🟢 过程受控正常</Tag>
            <Tag v-else-if="activeRecord.judgment === 'NG'" color="error" class="text-xl px-4 py-1 font-black animate-pulse">🔴 发现过程异常</Tag>
            <Tag v-else color="default" class="text-lg px-4 py-1 text-slate-400">巡检数据录入中...</Tag>
          </div>
          <Button type="primary" size="large" class="w-64 font-bold text-lg" :class="activeRecord.judgment==='NG' ? 'bg-red-600 border-none' : 'bg-indigo-600 border-none'" @click="handleSubmit">
            {{ activeRecord.judgment === 'NG' ? '报告异常并干预机台' : '提交打卡，前往下一站' }}
          </Button>
        </div>
      </div>
    </main>

    <Modal v-model:open="sampleModalVisible" :title="`抽检多件实测: ${currentSampleRow?.inspectionItem}`" @ok="saveSampleInput" width="400px" centered>
      <div v-if="currentSampleRow" class="py-4">
        <div class="bg-indigo-50 p-2 rounded mb-4 text-xs text-indigo-700 font-bold border border-indigo-100">
          公差/标准: {{ currentSampleRow.standardDesc }} | 本次要求抽检: {{ currentSampleRow.sampleSize }} 件
        </div>
        <div class="max-h-64 overflow-y-auto custom-scrollbar pr-2">
          <div v-for="i in currentSampleRow.sampleSize" :key="i" class="flex items-center gap-4 mb-3">
            <span class="font-mono font-bold w-16 text-slate-500">抽样 #{{ i }}</span>
            <InputNumber v-if="currentSampleRow.itemType === 'QUANTITATIVE'" v-model:value="currentSampleRow.sampleValues![i-1]" class="flex-1" placeholder="实测数值" />
            <Select v-else v-model:value="currentSampleRow.sampleValues![i-1]" class="flex-1" :options="[{label:'合格 (OK)',value:'OK'},{label:'异常 (NG)',value:'NG'}]" placeholder="判断结果" />
          </div>
        </div>
      </div>
    </Modal>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ IPQC 挂起队列" placement="left" width="350">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400">
        <IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>无挂起保存的任务</span>
      </div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-orange-200 bg-orange-50 shadow-sm">
          <div class="font-mono font-bold text-indigo-700 mb-1">机台: {{ task.record.machineCode }}</div>
          <div class="text-xs text-slate-600 mb-2">工单: {{ task.record.workOrderNo }}</div>
          <div class="flex justify-between items-center pt-2 border-t border-orange-100">
            <span class="text-[10px] text-slate-400">挂起时间: {{ task.suspendTime }}</span>
            <Button type="primary" size="small" class="bg-indigo-600" @click="handleResumeTask(idx)">继续处理</Button>
          </div>
        </Card>
      </div>
    </Drawer>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-number-input) { text-align: center; font-family: monospace; font-weight: bold; }
:deep(.ant-select-dropdown) { z-index: 9999 !important; }
</style>
