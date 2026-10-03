<script lang="ts" setup>
import { ref, computed, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Button, Input, message, Spin, Modal, InputSearch } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const route = useRoute();
const router = useRouter(); // 👈 就是漏了这一行！必须实例化 router
const isStarted = ref(false);
const globalLoading = ref(false);
const stationScanInput = ref('');
watch(
  () => route.query.workstation,
  (newVal) => {
    if (newVal && !isStarted.value) {
      stationScanInput.value = newVal as string;
      // 延迟 300 毫秒执行，等待框架的路由淡入动画结束，让遮罩层平滑弹出
      setTimeout(() => {
        handleStartHandover();
      }, 300);
    }
  },
  { immediate: true } // 保证一进入页面就立刻执行一次
);

function goToHistory() {
  // 跳转到刚才配置的交接班历史路由
  router.push('/mes/report/handover-history');
}

// ==================== 0. 全局状态与启动逻辑 ====================

// 交接班上下文
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
    message.success(`✅ 工作站 ${hoContext.workstation} 锁定，开始执行标准交接流程！`);
    stationScanInput.value = '';
  }, 600);
}

// ==================== 1. 交接班 SOP 流程状态机 ====================
const steps = [
  { id: 1, title: '交班人身份确认', icon: '👤', desc: '刷卡确认交班' },
  { id: 2, title: '现场 5S 与点检', icon: '🧹', desc: '环境与设备核查' },
  { id: 3, title: '在制品结存盘点', icon: '📦', desc: '线边仓数量核对' },
  { id: 4, title: '异常与事项交代', icon: '⚠️', desc: '遗留问题备注' },
  { id: 5, title: '接班人接管确认', icon: '🤝', desc: '刷卡签字并接管' }
];

const currentStep = ref(1);
const maxUnlockedStep = ref(1);
const stepLoading = ref(false);

function goToStep(stepId: number) {
  if (stepId > maxUnlockedStep.value) {
    message.warning('🚨 防错拦截：必须按顺序完成交接项目！');
    return;
  }
  currentStep.value = stepId;
}

async function unlockNextStep(nextStepId: number, successMsg: string) {
  stepLoading.value = true;
  setTimeout(() => {
    stepLoading.value = false;
    message.success(successMsg);
    if (maxUnlockedStep.value < nextStepId) {
      maxUnlockedStep.value = nextStepId;
    }
    currentStep.value = nextStepId;
  }, 500);
}

// ==================== 2. 各步骤业务数据 ====================

// [步骤 1] 交班人确认
const handoverScan = ref('');
function verifyHandoverUser() {
  if (!handoverScan.value) return;
  hoContext.handoverUser = '张伟 (工号: 8801)';
  unlockNextStep(2, '交班人身份验证成功！');
}

// [步骤 2] 5S与点检
const checklist = ref([
  { id: 'C1', name: '机台表面是否已清理干净，无粉尘积水？', checked: false },
  { id: 'C2', name: '量具、工具是否已归位并清点无误？', checked: false },
  { id: 'C3', name: '设备是否处于正常待机/运行状态？', checked: false }
]);
const allChecked = computed(() => checklist.value.every(item => item.checked));

// [步骤 3] 在制品结存 (复用数字键盘)
const wipInput = ref('');
function pressKey(key: string) {
  if (key === 'C') wipInput.value = '';
  else if (key === 'DEL') wipInput.value = wipInput.value.slice(0, -1);
  else wipInput.value += key;
}
function confirmWip() {
  const qty = parseInt(wipInput.value);
  if (isNaN(qty) || qty < 0) {
    message.warning('请输入有效的结存数量！');
    return;
  }
  hoContext.wipQty = qty;
  unlockNextStep(4, `线边结存 ${qty} 件已记录入账！`);
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
    message.error('请至少选择一项状态或填写备注！');
    return;
  }
  unlockNextStep(5, '交接班事项已登记归档！');
}

// [步骤 5] 接班人确认
const takeoverScan = ref('');
function verifyTakeoverUser() {
  if (!takeoverScan.value) return;

  stepLoading.value = true;
  setTimeout(() => {
    hoContext.takeoverUser = '刘洋 (工号: 9902)';
    hoContext.status = '交接完成';
    stepLoading.value = false;
    Modal.success({
      title: '🎉 交接班圆满完成',
      content: `机台控制权已正式移交至接班人：${hoContext.takeoverUser}。请接班人安全作业！`,
      okText: '返回主控台',
      onOk: () => { window.location.reload(); } // 模拟重置终端
    });
  }, 800);
}
</script>

<template>
  <div class="h-full min-h-screen bg-slate-50 text-slate-800 flex flex-col font-sans select-none overflow-hidden relative antialiased">

    <div v-if="globalLoading || stepLoading" class="absolute inset-0 bg-white/70 z-[100] flex items-center justify-center backdrop-blur-sm">
      <Spin size="large" :tip="globalLoading ? '正在锁定工位...' : '数据同步中...'" />
    </div>

    <div v-if="!isStarted" class="flex-1 flex flex-col items-center justify-center p-8 bg-slate-50 relative overflow-hidden">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-50 via-slate-50 to-slate-100 pointer-events-none"></div>

      <div class="z-10 flex flex-col items-center max-w-2xl w-full bg-white p-12 rounded-3xl border border-slate-200 shadow-2xl">
        <div class="text-8xl mb-8 animate-pulse drop-shadow-md">🔄</div>
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
          <p class="text-center text-slate-400 mt-4 text-sm">必须在当前机台物理终端上进行交接，以确保现场核对无误</p>
        </div>
      </div>
    </div>

    <template v-else>
      <header class="h-[100px] bg-white border-b border-slate-200 flex items-center px-6 justify-between flex-shrink-0 shadow-sm z-20">
        <div class="flex items-center gap-4 shrink-0">
          <div class="w-12 h-12 bg-indigo-50 rounded-full flex items-center justify-center text-xl font-bold text-indigo-600 border border-indigo-200 shadow-sm">
            班
          </div>
          <div class="flex flex-col">
            <span class="text-lg font-bold text-slate-800 tracking-wide">{{ hoContext.workstation }}</span>
            <span class="text-sm text-slate-500 font-bold text-indigo-600">{{ hoContext.shiftName }}</span>
          </div>
        </div>

        <div class="flex-1 px-10 overflow-hidden">
          <div class="bg-slate-50 border border-slate-200 rounded-2xl h-[70px] flex items-center px-8 shadow-inner w-full justify-between">
            <div class="flex items-center gap-4">
              <span class="text-slate-400 font-bold">交班方:</span>
              <span class="text-2xl font-black" :class="hoContext.handoverUser.includes('验证') ? 'text-amber-500 animate-pulse' : 'text-slate-800'">{{ hoContext.handoverUser }}</span>
            </div>
            <div class="w-12 border-t-4 border-dashed border-slate-300 relative">
              <div class="absolute -top-3 right-0 text-xl text-slate-300">▶</div>
            </div>
            <div class="flex items-center gap-4">
              <span class="text-slate-400 font-bold">接班方:</span>
              <span class="text-2xl font-black" :class="hoContext.takeoverUser.includes('验证') ? 'text-amber-500 animate-pulse' : 'text-slate-800'">{{ hoContext.takeoverUser }}</span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-6 shrink-0">
          <Button size="large" class="flex items-center gap-2 border-slate-300 text-slate-600 hover:text-indigo-600 hover:border-indigo-500 rounded-xl font-bold" @click="goToHistory">
            <IconifyIcon icon="lucide:history" class="text-xl" /> 查看历史
          </Button>
          <div class="flex flex-col items-end">
            <span class="text-sm text-slate-500 mb-1">交接班状态</span>
            <div class="flex items-center gap-3 bg-white px-4 py-1.5 rounded-full border border-slate-200 shadow-sm">
                <span class="relative flex h-4 w-4">
                  <span v-if="hoContext.status === '交接进行中'" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-4 w-4 shadow-sm"
                        :class="{'bg-indigo-500': hoContext.status === '交接进行中', 'bg-emerald-500': hoContext.status === '交接完成'}">
                  </span>
                </span>
              <span class="text-xl font-bold text-slate-700">{{ hoContext.status }}</span>
            </div>
          </div>
        </div>
      </header>

      <div class="flex flex-1 min-h-0 relative z-10 bg-slate-100/50">
        <aside class="w-[320px] bg-white border-r border-slate-200 flex flex-col p-4 overflow-y-auto shadow-sm z-10">
          <div class="text-sm font-bold text-slate-500 mb-6 uppercase tracking-widest pl-2 border-l-4 border-indigo-500">Handover SOP</div>

          <div class="flex flex-col gap-4 relative">
            <div class="absolute left-[38px] top-10 bottom-10 w-[3px] bg-slate-100 z-0 rounded-full"></div>

            <div v-for="step in steps" :key="step.id"
                 @click="goToStep(step.id)"
                 class="relative z-10 flex items-center p-4 rounded-xl cursor-pointer transition-all border-2 group bg-white"
                 :class="[
                     currentStep === step.id ? 'border-indigo-500 shadow-md scale-[1.02] bg-indigo-50/30' :
                     step.id <= maxUnlockedStep ? 'border-slate-200 hover:border-indigo-300 hover:bg-slate-50' : 'border-slate-100 opacity-60 cursor-not-allowed bg-slate-50'
                   ]">
              <div class="w-12 h-12 rounded-full flex items-center justify-center text-2xl flex-shrink-0 mr-4 shadow-sm border-2 transition-transform group-hover:scale-110"
                   :class="step.id < maxUnlockedStep ? 'bg-emerald-50 border-emerald-200 text-emerald-600' : (currentStep === step.id ? 'bg-indigo-500 border-indigo-500 text-white shadow-indigo-200' : 'bg-slate-100 border-slate-200 text-slate-400')">
                <span v-if="step.id < maxUnlockedStep" class="text-xl font-black">✓</span>
                <span v-else>{{ step.icon }}</span>
              </div>
              <div class="flex flex-col">
                <span class="text-lg font-bold transition-colors" :class="currentStep === step.id ? 'text-indigo-700' : 'text-slate-700'">{{ step.title }}</span>
                <span class="text-xs transition-colors" :class="currentStep === step.id ? 'text-indigo-400' : 'text-slate-400'">{{ step.desc }}</span>
              </div>
            </div>
          </div>
        </aside>

        <main class="flex-1 p-10 overflow-y-auto relative flex flex-col">

          <div v-if="currentStep === 1" class="flex flex-col h-full max-w-4xl mx-auto w-full animate-fade-in text-center justify-center">
            <div class="text-[100px] mb-6">🪪</div>
            <h2 class="text-5xl font-black text-slate-800 mb-6">交班员工身份核对</h2>
            <p class="text-2xl text-slate-500 mb-12">请即将下班的员工，使用扫码枪扫描您的工牌/厂牌条码</p>
            <InputSearch v-model:value="handoverScan" placeholder="请刷厂牌..." size="large" enter-button="确认身份" @search="verifyHandoverUser" class="h-24 text-3xl custom-huge-input-start shadow-xl mx-auto max-w-2xl" />
          </div>

          <div v-if="currentStep === 2" class="flex flex-col h-full max-w-5xl mx-auto w-full animate-fade-in">
            <div class="text-4xl font-black text-slate-800 mb-4 flex items-center gap-4 pb-4 border-b border-slate-200">
              <span class="bg-amber-100 text-amber-600 p-3 rounded-2xl shadow-sm">🧹</span> 现场 5S 与设备环境点检
            </div>
            <div class="flex flex-col gap-5 flex-1 mt-6">
              <div v-for="item in checklist" :key="item.id" @click="item.checked = !item.checked"
                   class="flex items-center justify-between bg-white p-7 rounded-2xl border-2 cursor-pointer transition-all shadow-sm"
                   :class="item.checked ? 'border-emerald-500 bg-emerald-50/50' : 'border-slate-200'">
                <span class="text-2xl font-bold" :class="item.checked ? 'text-emerald-800' : 'text-slate-700'">{{ item.name }}</span>
                <div class="w-14 h-14 rounded-xl border-4 flex items-center justify-center transition-all shadow-sm bg-white" :class="item.checked ? 'border-emerald-500 scale-110' : 'border-slate-200'">
                  <span v-if="item.checked" class="text-emerald-500 text-4xl font-black animate-scale-in">✓</span>
                </div>
              </div>
            </div>
            <Button size="large" type="primary" class="w-full h-24 text-3xl font-bold rounded-2xl shadow-md border-none transition-all mt-10"
                    :class="allChecked ? 'bg-indigo-600 hover:bg-indigo-500 hover:scale-[1.01]' : 'bg-slate-300 text-slate-500 cursor-not-allowed'"
                    :disabled="!allChecked" @click="unlockNextStep(3, '点检完成，进入数量盘点')">
              {{ allChecked ? '点检项全部合格，进入下一步' : '🛑 请完成所有环境与设备检查' }}
            </Button>
          </div>

          <div v-if="currentStep === 3" class="flex flex-col h-full max-w-6xl mx-auto w-full animate-fade-in">
            <div class="text-4xl font-black text-slate-800 mb-8 flex items-center gap-4 pb-4 border-b border-slate-200">
              <span class="bg-blue-100 text-blue-600 p-3 rounded-2xl shadow-sm">📦</span> 遗留线边在制品盘点
            </div>
            <div class="flex gap-12 flex-1 h-full">
              <div class="flex-1 flex flex-col gap-8">
                <div class="bg-white border-4 border-slate-200 rounded-3xl p-10 h-48 flex items-center justify-end overflow-hidden shadow-inner relative">
                  <div class="absolute left-6 top-6 text-slate-400 text-xl font-bold">结存数量核对</div>
                  <span v-if="!wipInput" class="text-6xl font-black text-slate-300 font-mono animate-pulse">键盘输入</span>
                  <span v-else class="text-[120px] font-black text-slate-800 font-mono tracking-tight leading-none">{{ wipInput }}</span>
                  <span class="text-4xl text-slate-400 ml-6 font-bold self-end mb-4">PCS</span>
                </div>
                <Button class="mt-auto h-32 text-4xl font-black rounded-3xl bg-blue-600 hover:bg-blue-500 border-none text-white shadow-lg transition-all" @click="confirmWip">
                  确认实物结存数量
                </Button>
              </div>
              <div class="w-[450px] bg-white p-6 rounded-3xl border border-slate-200 shadow-md">
                <div class="grid grid-cols-3 gap-4 h-full">
                  <div v-for="key in ['1','2','3','4','5','6','7','8','9','C','0','DEL']" :key="key" @click="pressKey(key)"
                       class="bg-slate-50 border-2 border-slate-100 rounded-2xl flex items-center justify-center text-6xl font-black text-slate-700 cursor-pointer shadow-sm transition-all select-none active:bg-indigo-500 active:text-white active:scale-95"
                       :class="key === 'C' ? 'bg-red-50 text-red-500' : (key === 'DEL' ? 'bg-amber-50 text-amber-500' : '')">
                    {{ key === 'DEL' ? '⬅' : key }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="currentStep === 4" class="flex flex-col h-full max-w-5xl mx-auto w-full animate-fade-in">
            <div class="text-4xl font-black text-slate-800 mb-8 flex items-center gap-4 pb-4 border-b border-slate-200">
              <span class="bg-red-100 text-red-600 p-3 rounded-2xl shadow-sm">⚠️</span> 异常与遗留事项交代
            </div>
            <div class="grid grid-cols-2 gap-6 mb-8">
              <div v-for="tag in exceptionTags" :key="tag.label" @click="selectTag(tag)"
                   class="p-6 rounded-2xl border-4 text-center cursor-pointer font-bold text-2xl transition-all shadow-sm flex items-center justify-center h-24"
                   :class="tag.selected ? (tag.type === 'success' ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-red-500 bg-red-50 text-red-700') : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300'">
                {{ tag.label }}
              </div>
            </div>
            <Input.TextArea v-model:value="customRemark" placeholder="点击此处输入其他特殊交代事项..." :rows="4" class="text-2xl p-6 rounded-2xl border-2 border-slate-200 shadow-inner flex-1 bg-white" />
            <Button size="large" type="primary" class="w-full h-24 text-3xl font-bold rounded-2xl shadow-md border-none transition-all mt-8 bg-indigo-600 hover:bg-indigo-500" @click="submitExceptions">
              登记事项，呼叫接班人
            </Button>
          </div>

          <div v-if="currentStep === 5" class="flex flex-col h-full max-w-4xl mx-auto w-full animate-fade-in text-center justify-center">
            <div class="text-[100px] mb-6 animate-bounce">🤝</div>
            <h2 class="text-5xl font-black text-slate-800 mb-6">接班人确认与接管机台</h2>
            <p class="text-2xl text-slate-500 mb-12">请接班员工核实上述所有交接情况，刷卡完成控制权移交</p>
            <InputSearch v-model:value="takeoverScan" placeholder="接班人请刷厂牌..." size="large" enter-button="签字接管" @search="verifyTakeoverUser" class="h-24 text-3xl custom-huge-input-start shadow-xl mx-auto max-w-2xl" />
          </div>

        </main>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 深度定制 Ant Design Input 样式 (直接复用原样式) */
:deep(.custom-huge-input-start .ant-input) {
  height: 100% !important; font-size: 1.8rem !important; background-color: #ffffff !important; color: #1e293b !important; border-color: #cbd5e1 !important; border-width: 2px !important; border-radius: 1rem 0 0 1rem !important; padding-left: 1.5rem !important;
}
:deep(.custom-huge-input-start .ant-input:focus) { border-color: #4f46e5 !important; box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1) !important; }
:deep(.custom-huge-input-start .ant-input-search-button) { height: 100% !important; width: 180px !important; font-size: 1.5rem !important; font-weight: bold !important; background-color: #4f46e5 !important; border-color: #4f46e5 !important; border-radius: 0 1rem 1rem 0 !important; transition: all 0.2s; }
:deep(.custom-huge-input-start .ant-input-search-button:hover) { background-color: #4338ca !important; border-color: #4338ca !important; }

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.animate-scale-in { animation: scaleIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes scaleIn { from { opacity: 0; transform: scale(0.5); } to { opacity: 1; transform: scale(1); } }
</style>
