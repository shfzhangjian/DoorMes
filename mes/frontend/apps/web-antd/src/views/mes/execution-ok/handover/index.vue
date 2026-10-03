<script lang="ts" setup>
import { ref, computed, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Button, Input, message, Spin, Modal, InputSearch, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const route = useRoute();
const router = useRouter();
const isStarted = ref(false);
const globalLoading = ref(false);
const stationScanInput = ref('');

watch(
  () => route.query.workstation,
  (newVal) => {
    if (newVal && !isStarted.value) {
      stationScanInput.value = newVal as string;
      setTimeout(() => {
        handleStartHandover();
      }, 300);
    }
  },
  { immediate: true }
);

function goToHistory() {
  router.push('/mes/report/handover-history');
}

// ==================== 0. 全局状态与启动逻辑 ====================
const hoContext = reactive({
  workstation: '',
  shiftName: '',
  handoverUser: '等待验证',
  takeoverUser: '等待验证',
  wipQty: 0,
  status: '待交班'
});

async function handleStartHandover() {
  if (!stationScanInput.value) {
    message.warning('请先扫描工作站条码或输入编号！');
    return;
  }

  globalLoading.value = true;
  setTimeout(() => {
    hoContext.workstation = stationScanInput.value.toUpperCase();
    hoContext.shiftName = '早班 08:00 ➡ 中班 16:00';
    hoContext.status = '交接进行中';

    isStarted.value = true;
    globalLoading.value = false;
    message.success(`工作站 ${hoContext.workstation} 锁定，开始执行标准交接流程。`);
    stationScanInput.value = '';
  }, 600);
}

// ==================== 1. 交接班 SOP 流程状态机 ====================
const steps = [
  { id: 1, title: '交班人身份确认', icon: '👤', desc: '刷卡确认交班' },
  { id: 2, title: '现场 5S 与点检', icon: '🧹', desc: '环境与设备核查' },
  { id: 3, title: '在制品结存盘点', icon: '📦', desc: '线边仓明细核对' },
  { id: 4, title: '异常与事项交代', icon: '⚠️', desc: '遗留问题备注' },
  { id: 5, title: '接班人接管确认', icon: '🤝', desc: '刷卡签字并接管' }
];

const currentStep = ref(1);
const maxUnlockedStep = ref(1);

function isStepUnlocked(stepId: number) {
  return stepId <= maxUnlockedStep.value;
}

function isStepDone(stepId: number) {
  return maxUnlockedStep.value > stepId;
}

function goToStep(stepId: number) {
  if (!isStepUnlocked(stepId)) {
    message.warning('防错拦截：必须按顺序完成交接项目。');
    return;
  }
  currentStep.value = stepId;
}

function unlockNextStep(nextStepId: number, successMsg?: string) {
  if (successMsg) message.success(successMsg);
  if (maxUnlockedStep.value < nextStepId) {
    maxUnlockedStep.value = nextStepId;
  }
  currentStep.value = nextStepId;
}

// ==================== 2. 各步骤业务数据 ====================

// [步骤 1] 交班人确认
const handoverScan = ref('');
function verifyHandoverUser() {
  if (!handoverScan.value) return;
  hoContext.handoverUser = '张伟 (工号: 8801)';
  unlockNextStep(2, '交班人身份验证成功。');
}

// [步骤 2] 5S与点检
const checklist = ref([
  { id: 'C1', name: '机台表面是否已清理干净，无粉尘积水？', checked: false },
  { id: 'C2', name: '量具、工具是否已归位并清点无误？', checked: false },
  { id: 'C3', name: '设备是否处于正常待机/运行状态？', checked: false }
]);
const allChecked = computed(() => checklist.value.every(item => item.checked));

// [步骤 3] 在制品结存 (重构为列表明细记录)
const wipRecords = ref<{ id: string, type: string, batch: string, qty: number }[]>([]);
const wipForm = reactive({ type: '', batch: '', qty: null as number | null });

function addWipRecord() {
  if (!wipForm.type || !wipForm.batch || !wipForm.qty) {
    message.warning('请填写完整的制品种类、批次号和结存数量。');
    return;
  }
  wipRecords.value.unshift({
    id: Date.now().toString(),
    type: wipForm.type,
    batch: wipForm.batch,
    qty: wipForm.qty
  });
  wipForm.type = '';
  wipForm.batch = '';
  wipForm.qty = null;
}

function removeWipRecord(id: string) {
  wipRecords.value = wipRecords.value.filter(item => item.id !== id);
}

function confirmWip() {
  if (wipRecords.value.length === 0) {
    Modal.confirm({
      title: '确认无在制品结存？',
      content: '当前列表为空，确认线边无任何遗留物料吗？',
      onOk: () => {
        hoContext.wipQty = 0;
        unlockNextStep(4, '在制品盘点已确认（无结存）。');
      }
    });
  } else {
    hoContext.wipQty = wipRecords.value.reduce((acc, cur) => acc + (cur.qty || 0), 0);
    unlockNextStep(4, `线边结存共 ${hoContext.wipQty} 件，明细已记录入账。`);
  }
}

// [步骤 4] 异常交代
const exceptionTags = ref([
  { label: '一切正常，无遗留', type: 'success', selected: true },
  { label: '设备有异响/轻微故障', type: 'warning', selected: false },
  { label: '线边物料即将耗尽', type: 'warning', selected: false },
  { label: '上工序来料品质异常', type: 'error', selected: false }
]);
const customRemark = ref('');
function selectTag(tag: any) {
  if (tag.label === '一切正常，无遗留') {
    exceptionTags.value.forEach(t => t.selected = false);
    tag.selected = true;
  } else {
    exceptionTags.value[0].selected = false;
    tag.selected = !tag.selected;
  }
}
function submitExceptions() {
  const hasSelection = exceptionTags.value.some(t => t.selected) || customRemark.value;
  if (!hasSelection) {
    message.error('请至少选择一项状态或填写备注。');
    return;
  }
  unlockNextStep(5, '交接班事项已登记归档。');
}

// [步骤 5] 接班人确认
const takeoverScan = ref('');
function verifyTakeoverUser() {
  if (!takeoverScan.value) return;

  globalLoading.value = true;
  setTimeout(() => {
    hoContext.takeoverUser = '刘洋 (工号: 9902)';
    hoContext.status = '交接完成';
    globalLoading.value = false;
    Modal.success({
      title: '交接班记录完成',
      content: `机台控制权已正式移交至接班人：${hoContext.takeoverUser}。数据已归档。`,
      okText: '返回工作台',
      onOk: () => {
        // 替换为路由跳转，返回终端看板
        router.push({
          path: '/mes/production/shop-floor/terminal-hmi',
          query: { workstation: hoContext.workstation }
        });
      }
    });
  }, 800);
}
</script>

<template>
  <div class="absolute inset-0 flex flex-col bg-slate-50 text-slate-800 font-sans select-none overflow-hidden antialiased">

    <div v-if="globalLoading" class="absolute inset-0 bg-white/70 z-[100] flex items-center justify-center backdrop-blur-sm">
      <Spin size="large" tip="系统处理中..." />
    </div>

    <div v-if="!isStarted" class="flex-1 flex flex-col items-center justify-center p-8 bg-slate-50 relative overflow-hidden">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-50 via-slate-50 to-slate-100 pointer-events-none"></div>

      <div class="z-10 flex flex-col items-center max-w-2xl w-full bg-white p-12 rounded-3xl border border-slate-200 shadow-2xl">
        <div class="text-8xl mb-8 drop-shadow-md">🔄</div>
        <h1 class="text-4xl font-black text-slate-800 mb-4 tracking-wider">交接班工作台</h1>
        <p class="text-xl text-slate-500 mb-12 text-center">请使用扫码枪扫描机台 <span class="text-indigo-600 font-bold">工位条码</span> 发起交班</p>

        <div class="w-full">
          <InputSearch
            v-model:value="stationScanInput"
            placeholder="光标置于此处，扫入工位号 (如: WS-01)"
            size="large"
            enter-button="发起交班"
            @search="handleStartHandover"
            class="h-20 text-2xl custom-huge-input-start shadow-sm"
          >
            <template #prefix><span class="text-3xl mr-2">📍</span></template>
          </InputSearch>
          <p class="text-center text-slate-400 mt-4 text-sm">必须在当前机台物理终端上进行交接操作</p>
        </div>
      </div>
    </div>

    <div v-else class="flex-1 flex flex-col min-h-0 overflow-hidden">
      <header class="shrink-0 h-[90px] bg-white border-b border-slate-200 flex items-center px-6 justify-between shadow-sm z-30 relative">
        <div class="flex items-center gap-4 shrink-0">
          <div class="w-12 h-12 bg-indigo-50 rounded-full flex items-center justify-center text-xl font-bold text-indigo-600 border border-indigo-200">
            班
          </div>
          <div class="flex flex-col">
            <span class="text-lg font-bold text-slate-800 tracking-wide">{{ hoContext.workstation }}</span>
            <span class="text-sm font-bold text-indigo-600">{{ hoContext.shiftName }}</span>
          </div>
        </div>

        <div class="flex-1 px-10 overflow-hidden">
          <div class="bg-slate-50 border border-slate-200 rounded-xl h-[60px] flex items-center px-8 shadow-inner w-full justify-between">
            <div class="flex items-center gap-4">
              <span class="text-slate-400 font-bold">交班方:</span>
              <span class="text-2xl font-black" :class="hoContext.handoverUser.includes('验证') ? 'text-amber-500' : 'text-slate-800'">{{ hoContext.handoverUser }}</span>
            </div>
            <div class="w-12 border-t-4 border-dashed border-slate-300 relative">
              <div class="absolute -top-3 right-0 text-xl text-slate-300">▶</div>
            </div>
            <div class="flex items-center gap-4">
              <span class="text-slate-400 font-bold">接班方:</span>
              <span class="text-2xl font-black" :class="hoContext.takeoverUser.includes('验证') ? 'text-amber-500' : 'text-slate-800'">{{ hoContext.takeoverUser }}</span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-6 shrink-0">
          <Button size="large" class="flex items-center gap-2 border-slate-300 text-slate-600 hover:text-indigo-600 rounded-xl font-bold bg-slate-50" @click="goToHistory">
            <IconifyIcon icon="lucide:history" class="text-xl" /> 查看交接历史
          </Button>
          <div class="h-12 w-[2px] bg-slate-200"></div>
          <div class="flex flex-col items-end">
            <span class="text-sm text-slate-500 mb-1">交接班状态</span>
            <div class="flex items-center gap-2 bg-white px-3 py-1 rounded-full border border-slate-200 shadow-sm">
                <span class="relative flex h-3 w-3">
                  <span class="relative inline-flex rounded-full h-3 w-3"
                        :class="{'bg-indigo-500': hoContext.status === '交接进行中', 'bg-emerald-500': hoContext.status === '交接完成'}">
                  </span>
                </span>
              <span class="text-lg font-bold text-slate-700">{{ hoContext.status }}</span>
            </div>
          </div>
        </div>
      </header>

      <div class="flex-1 flex min-h-0 overflow-hidden bg-slate-100 relative z-10">
        <aside class="shrink-0 w-[300px] h-full bg-white border-r border-slate-200 flex flex-col p-4 overflow-y-auto shadow-sm z-20">
          <div class="text-sm font-bold text-slate-500 mb-6 uppercase tracking-widest pl-2 border-l-4 border-indigo-500">Handover SOP</div>
          <div class="flex flex-col gap-2 relative">
            <div class="absolute left-[38px] top-10 bottom-10 w-[3px] bg-slate-100 z-0 rounded-full"></div>
            <div v-for="step in steps" :key="step.id" @click="goToStep(step.id)"
                 class="relative z-10 flex items-center p-4 rounded-xl transition-all border-2 group bg-white"
                 :class="[
                   currentStep === step.id ? 'border-indigo-500 shadow-md scale-[1.02] bg-indigo-50/30' :
                   isStepUnlocked(step.id) ? 'border-slate-200 hover:border-indigo-300 cursor-pointer' :
                   'border-slate-100 opacity-60 cursor-not-allowed bg-slate-50'
                 ]">
              <div class="w-12 h-12 rounded-full flex items-center justify-center text-2xl flex-shrink-0 mr-3 shadow-sm border-2 transition-transform group-hover:scale-110"
                   :class="[
                     currentStep === step.id ? 'bg-indigo-500 border-indigo-500 text-white shadow-indigo-200' :
                     isStepDone(step.id) ? 'bg-emerald-50 border-emerald-200 text-emerald-600' :
                     isStepUnlocked(step.id) ? 'bg-slate-100 border-slate-200 text-slate-500' :
                     'bg-slate-100 border-slate-200 text-slate-300'
                   ]">
                <span v-if="isStepDone(step.id) && currentStep !== step.id" class="text-xl font-black">✓</span>
                <span v-else>{{ step.icon }}</span>
              </div>
              <div class="flex flex-col">
                <span class="text-[1.05rem] font-bold transition-colors" :class="currentStep === step.id ? 'text-indigo-700' : 'text-slate-700'">{{ step.title }}</span>
                <span class="text-[11px] transition-colors" :class="currentStep === step.id ? 'text-indigo-400' : 'text-slate-400'">{{ step.desc }}</span>
              </div>
            </div>
          </div>
        </aside>

        <main class="flex-1 flex flex-col h-full bg-slate-50 relative overflow-hidden min-h-0">

          <div v-if="currentStep === 1" class="flex-1 flex flex-col min-h-0 w-full animate-fade-in">
            <div class="shrink-0 h-[80px] px-8 bg-white border-b border-slate-200 shadow-sm flex items-center gap-4 z-20">
              <span class="bg-indigo-100 text-indigo-600 p-2 rounded-xl text-xl">🪪</span>
              <h2 class="text-2xl font-black text-slate-800 m-0">交班员工身份核对</h2>
            </div>
            <div class="flex-1 min-h-0 flex flex-col items-center justify-center p-8 bg-slate-50">
              <div class="text-[100px] mb-6">🪪</div>
              <p class="text-2xl text-slate-500 mb-12">请即将下班的员工扫描工牌/厂牌条码完成身份确认</p>
              <InputSearch v-model:value="handoverScan" placeholder="请刷厂牌..." size="large" enter-button="确认身份" @search="verifyHandoverUser" class="h-20 text-2xl custom-huge-input-start shadow-xl mx-auto max-w-2xl" />
            </div>
            <div class="shrink-0 h-[100px] p-6 bg-white border-t border-slate-200 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] z-20 flex justify-center">
              <div class="text-slate-400 text-lg flex items-center">等待扫码数据验证...</div>
            </div>
          </div>

          <div v-if="currentStep === 2" class="flex-1 flex flex-col min-h-0 w-full animate-fade-in">
            <div class="shrink-0 h-[80px] px-8 bg-white border-b border-slate-200 shadow-sm flex items-center gap-4 z-20">
              <span class="bg-amber-100 text-amber-600 p-2 rounded-xl text-xl">🧹</span>
              <h2 class="text-2xl font-black text-slate-800 m-0">现场 5S 与设备环境点检</h2>
            </div>
            <div class="flex-1 min-h-0 overflow-y-auto p-8 bg-slate-50">
              <div class="flex flex-col gap-4 max-w-5xl mx-auto">
                <div v-for="item in checklist" :key="item.id" @click="item.checked = !item.checked"
                     class="flex items-center justify-between bg-white p-6 rounded-2xl border-2 cursor-pointer shadow-sm transition-all"
                     :class="item.checked ? 'border-emerald-500 bg-emerald-50/50' : 'border-slate-200 hover:border-indigo-300'">
                  <span class="text-xl font-bold" :class="item.checked ? 'text-emerald-800' : 'text-slate-700'">{{ item.name }}</span>
                  <div class="w-12 h-12 rounded-xl border-4 flex items-center justify-center bg-white transition-all" :class="item.checked ? 'border-emerald-500 scale-110' : 'border-slate-200'">
                    <span v-if="item.checked" class="text-emerald-500 text-3xl font-black">✓</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="shrink-0 h-[100px] p-6 bg-white border-t border-slate-200 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] z-20 flex justify-center">
              <Button size="large" type="primary" class="w-full max-w-5xl h-full text-2xl font-bold rounded-xl shadow-md border-none"
                      :class="allChecked ? 'bg-indigo-600 hover:bg-indigo-500' : 'bg-slate-300 text-slate-500'"
                      :disabled="!allChecked" @click="unlockNextStep(3, '点检完成，进入数量盘点')">
                {{ allChecked ? '点检项全部合格，进入下一步' : '🛑 请完成所有环境与设备检查' }}
              </Button>
            </div>
          </div>

          <div v-if="currentStep === 3" class="flex-1 flex flex-col min-h-0 w-full animate-fade-in">
            <div class="shrink-0 h-[80px] px-8 bg-white border-b border-slate-200 shadow-sm flex items-center gap-4 z-20">
              <span class="bg-blue-100 text-blue-600 p-2 rounded-xl text-xl">📦</span>
              <h2 class="text-2xl font-black text-slate-800 m-0">遗留线边在制品盘点</h2>
            </div>
            <div class="flex-1 min-h-0 p-8 bg-slate-50 flex gap-8 w-full max-w-7xl mx-auto">
              <div class="w-[400px] bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col gap-5 shrink-0">
                <div class="text-lg font-bold text-slate-700 border-b pb-2 mb-2">添加明细记录</div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold text-slate-500">制品种类/名称</label>
                  <Input v-model:value="wipForm.type" placeholder="输入物料名称或编码" class="h-12 text-lg border-2 border-slate-200 rounded-lg" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold text-slate-500">生产批次号</label>
                  <Input v-model:value="wipForm.batch" placeholder="输入批次条码号" class="h-12 text-lg font-mono border-2 border-slate-200 rounded-lg" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-bold text-slate-500">结存数量 (PCS)</label>
                  <Input type="number" v-model:value="wipForm.qty" placeholder="输入数量" class="h-12 text-xl font-bold text-center border-2 border-slate-200 rounded-lg" />
                </div>
                <Button size="large" type="primary" class="h-14 text-xl font-bold bg-blue-600 hover:bg-blue-500 rounded-xl mt-4 border-none shadow-md" @click="addWipRecord">
                  ➕ 追加到盘点列表
                </Button>
              </div>

              <div class="flex-1 flex flex-col bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden min-h-0">
                <div class="bg-slate-100 p-4 font-bold text-slate-700 border-b border-slate-200 shrink-0 flex justify-between items-center">
                  <span>已盘点在制品列表</span>
                  <Tag color="blue" class="!m-0 text-sm">共 {{ wipRecords.length }} 项记录</Tag>
                </div>
                <div class="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50">
                  <div v-if="wipRecords.length === 0" class="h-full flex flex-col items-center justify-center text-slate-400">
                    <IconifyIcon icon="lucide:inbox" class="text-6xl mb-2 opacity-50" />
                    <span class="text-lg font-bold">暂无数据，请在左侧录入</span>
                  </div>
                  <div v-for="(rec, idx) in wipRecords" :key="rec.id" class="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-sm hover:border-blue-300 transition-all">
                    <div class="flex gap-4 items-center">
                      <div class="w-8 h-8 rounded-full bg-slate-800 text-white flex items-center justify-center font-bold text-sm">{{ wipRecords.length - idx }}</div>
                      <div class="flex flex-col">
                        <span class="text-lg font-bold text-slate-800">{{ rec.type }}</span>
                        <span class="text-sm text-slate-500 font-mono mt-1">批次: {{ rec.batch }}</span>
                      </div>
                    </div>
                    <div class="flex items-center gap-6">
                      <div class="text-2xl font-black text-blue-600">{{ rec.qty }} <span class="text-sm text-slate-400 font-normal">PCS</span></div>
                      <Button danger type="text" @click="removeWipRecord(rec.id)" title="删除"><IconifyIcon icon="lucide:trash-2" class="text-xl" /></Button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="shrink-0 h-[100px] p-6 bg-white border-t border-slate-200 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] z-20 flex justify-center">
              <Button size="large" type="primary" class="w-full max-w-7xl h-full text-2xl font-bold rounded-xl shadow-md bg-blue-600 hover:bg-blue-500 border-none" @click="confirmWip">
                💾 确认盘点明细，进入下一步
              </Button>
            </div>
          </div>

          <div v-if="currentStep === 4" class="flex-1 flex flex-col min-h-0 w-full animate-fade-in">
            <div class="shrink-0 h-[80px] px-8 bg-white border-b border-slate-200 shadow-sm flex items-center gap-4 z-20">
              <span class="bg-red-100 text-red-600 p-2 rounded-xl text-xl">⚠️</span>
              <h2 class="text-2xl font-black text-slate-800 m-0">异常与遗留事项交代</h2>
            </div>
            <div class="flex-1 min-h-0 p-8 bg-slate-50 flex flex-col max-w-5xl mx-auto w-full">
              <div class="grid grid-cols-2 gap-6 mb-8 shrink-0">
                <div v-for="tag in exceptionTags" :key="tag.label" @click="selectTag(tag)"
                     class="p-5 rounded-2xl border-4 text-center cursor-pointer font-bold text-xl transition-all shadow-sm flex items-center justify-center h-20"
                     :class="tag.selected ? (tag.type === 'success' ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-red-500 bg-red-50 text-red-700') : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300'">
                  {{ tag.label }}
                </div>
              </div>
              <Input.TextArea v-model:value="customRemark" placeholder="点击此处输入其他特殊交代事项详情..." :rows="6" class="text-lg p-6 rounded-2xl border-2 border-slate-200 shadow-inner flex-1 min-h-[150px] bg-white" />
            </div>
            <div class="shrink-0 h-[100px] p-6 bg-white border-t border-slate-200 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] z-20 flex justify-center">
              <Button size="large" type="primary" class="w-full max-w-5xl h-full text-2xl font-bold rounded-xl shadow-md border-none bg-indigo-600 hover:bg-indigo-500" @click="submitExceptions">
                📝 登记事项，呼叫接班人
              </Button>
            </div>
          </div>

          <div v-if="currentStep === 5" class="flex-1 flex flex-col min-h-0 w-full animate-fade-in">
            <div class="shrink-0 h-[80px] px-8 bg-white border-b border-slate-200 shadow-sm flex items-center gap-4 z-20">
              <span class="bg-cyan-100 text-cyan-600 p-2 rounded-xl text-xl">🤝</span>
              <h2 class="text-2xl font-black text-slate-800 m-0">接班人确认与接管机台</h2>
            </div>
            <div class="flex-1 min-h-0 flex flex-col items-center justify-center p-8 bg-slate-50">
              <div class="text-[100px] mb-6">🤝</div>
              <p class="text-2xl text-slate-500 mb-12">请接班员工核实上述所有交接情况，刷卡完成控制权移交</p>
              <InputSearch v-model:value="takeoverScan" placeholder="接班人请刷厂牌..." size="large" enter-button="签字接管" @search="verifyTakeoverUser" class="h-20 text-2xl custom-huge-input-start shadow-xl mx-auto max-w-2xl" />
            </div>
            <div class="shrink-0 h-[100px] p-6 bg-white border-t border-slate-200 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] z-20 flex justify-center">
              <div class="text-slate-400 text-lg flex items-center">接班扫描验证后将自动归档记录并复位机台</div>
            </div>
          </div>

        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.custom-huge-input-start .ant-input) {
  height: 100% !important;
  font-size: 1.5rem !important;
  background-color: #ffffff !important;
  color: #1e293b !important;
  border-color: #cbd5e1 !important;
  border-width: 2px !important;
  border-radius: 1rem 0 0 1rem !important;
  padding-left: 1.5rem !important;
}
:deep(.custom-huge-input-start .ant-input:focus) {
  border-color: #4f46e5 !important;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1) !important;
}
:deep(.custom-huge-input-start .ant-input-search-button) {
  height: 100% !important;
  width: 160px !important;
  font-size: 1.4rem !important;
  font-weight: bold !important;
  background-color: #4f46e5 !important;
  border-color: #4f46e5 !important;
  border-radius: 0 1rem 1rem 0 !important;
  transition: all 0.2s;
}
:deep(.custom-huge-input-start .ant-input-search-button:hover) {
  background-color: #4338ca !important;
  border-color: #4338ca !important;
}

.animate-fade-in { animation: fadeIn 0.3s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
</style>
