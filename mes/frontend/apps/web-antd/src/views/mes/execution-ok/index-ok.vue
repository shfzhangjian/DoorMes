<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Card, Tag, Input, Row, Col, Button, Avatar, Popconfirm, Divider, message, Badge, Popover, List, Modal, Form, FormItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入所有工序子终端
import MixingProcessTerminal from './MixingProcessTerminal.vue';
import CoatingProcessTerminal from './CoatingProcessTerminal.vue';
import GrindingProcessTerminal from './GrindingProcessTerminal.vue';
import FineGrindingProcessTerminal from './FineGrindingProcessTerminal.vue';
import Taping1ProcessTerminal from './Taping1ProcessTerminal.vue';
import SlittingProcessTerminal from './SlittingProcessTerminal.vue';
import StampingProcessTerminal from './StampingProcessTerminal.vue';
import Taping2ProcessTerminal from './Taping2ProcessTerminal.vue';
import CuttingProcessTerminal from './CuttingProcessTerminal.vue';
import QcProcessTerminal from './QcProcessTerminal.vue';

// 🔥 新增：允许被外部作为组件全屏嵌入
const props = defineProps({ isModal: { type: Boolean, default: false } });
const emit = defineEmits(['closeModal']);

const viewMode = ref<'dashboard' | 'process'>('dashboard');
const currentTime = ref(new Date().toLocaleString());
let timer: any = null;

onMounted(() => { timer = setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000); });
onUnmounted(() => clearInterval(timer));

const isOnline = ref(false);
const loginModalVisible = ref(false);
const currentUser = ref<any>(null);
const loginForm = ref({ userId: '', password: '' });
const isSwiping = ref(false);

const STAFF_DB = [{ userId: 'U1001', userName: '张伟', role: '高级主操', seed: 'Felix', pwd: '123' }];

function openLoginModal() { loginForm.value = { userId: '', password: '' }; loginModalVisible.value = true; }

function simulateSwipe() {
  isSwiping.value = true;
  setTimeout(() => { doLogin(STAFF_DB[0]); isSwiping.value = false; }, 1500);
}

function doLogin(staff: any) {
  currentUser.value = { ...staff, avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${staff.seed}`, loginTime: new Date().toLocaleTimeString() };
  isOnline.value = true; loginModalVisible.value = false;
  message.success(`欢迎上班，${staff.userName}！`);
}

function handleManualLogin() {
  const staff = STAFF_DB.find(u => u.userId === loginForm.value.userId && u.pwd === loginForm.value.password);
  if (staff) doLogin(staff); else message.error('工号或密码错误');
}

function handleClockOut() {
  Modal.confirm({
    title: '确认下班', content: '下班后将解绑当前工位的所有操作权限，是否继续？',
    onOk: () => { isOnline.value = false; currentUser.value = null; viewMode.value = 'dashboard'; activeProcess.value = null; message.warn('已下班，工位已释放'); }
  });
}

const PROCESS_JSON = [
  { id: '01', name: '配料', code: 'MIXING', icon: 'lucide:flask-conical', color: 'blue', station: '配料站-01', device: '禾臣配料罐 A', desc: '原料核对与投料防错' },
  { id: '02', name: '湿法', code: 'COATING', icon: 'lucide:waves', color: 'green', station: '涂布线-02', device: '涂布挤压机 B', desc: '涂布工艺与参数记录' },
  { id: '03', name: '磨皮(一)', code: 'GRINDING_1', icon: 'lucide:layers', color: 'orange', station: '通用工位-03', device: '粗磨机', desc: '粗磨工艺监控' },
  { id: '04', name: '磨皮(二)', code: 'GRINDING_2', icon: 'lucide:maximize', color: 'orange', station: '通用工位-04', device: '精磨机', desc: '精磨分段产出' },
  { id: '05', name: '粘胶1', code: 'TAPING_1', icon: 'lucide:component', color: 'purple', station: '通用工位-05', device: '粘胶机', desc: '卷材复合粘胶' },
  { id: '06', name: '分切', code: 'SLITTING', icon: 'lucide:scissors', color: 'cyan', station: '分切站-06', device: '分切机', desc: '异常切除与单品赋码' },
  { id: '07', name: '压槽', code: 'STAMPING', icon: 'lucide:grid-3x3', color: 'indigo', station: '后道站-07', device: '压槽机', desc: '单片压槽与激光定位' },
  { id: '08', name: '粘胶2', code: 'TAPING_2', icon: 'lucide:combine', color: 'pink', station: '后道站-08', device: '背胶机', desc: '单片背胶与扫码上线' },
  { id: '09', name: '裁切', code: 'CUTTING', icon: 'lucide:circle-dot', color: 'rose', station: '通用工位-09', device: '裁切机', desc: '单片裁圆完工统计' },
  { id: '10', name: '检验', code: 'QC', icon: 'lucide:microscope', color: 'red', station: '质检站-10', device: '显微镜', desc: '分级判定记录' },
  { id: '11', name: '成品/出货', code: 'WH', icon: 'lucide:package-check', color: 'slate', station: '成品库-11', device: '包装机', desc: '出库销账关联' },
];

const workOrders = ref([
  { id: 'WO20260218001', product: 'PET光学薄膜', batch: '', status: '未开工', qty: '5000kg', spec: 'T01-0.15' },
]);

const activeProcess = ref<any>(null);
const activeOrder = ref(workOrders.value[0]);
const orderListVisible = ref(false);

function handleEnterProcess(proc: any) {
  if (!isOnline.value) return openLoginModal();
  activeProcess.value = proc;
  viewMode.value = 'process';
}

// 🔥 新增：处理退出的统一逻辑
function handleBack() {
  if (props.isModal) {
    emit('closeModal'); // 如果是被外部作为弹窗挂载的，直接关闭弹窗
  } else {
    viewMode.value = 'dashboard'; // 否则回到 11 宫格主页
  }
}

// ==========================================
// 🔥 核心新增：被外部工单列表调用时的调度接口
// ==========================================
function openFromExternal(processCodeZh: string, orderData: any) {
  // 1. 智能映射：把中文工序名映射到我们的标准终端 Code
  const procMap: Record<string, string> = {
    '配料': 'MIXING', '涂布': 'COATING', '分切': 'SLITTING', '裁切': 'CUTTING',
    '粗磨': 'GRINDING_1', '压铸': 'GRINDING_1', // 兼容演示
    '精磨': 'GRINDING_2', '抛光': 'GRINDING_2', // 兼容演示
    '底盘': 'TAPING_1', '总装': 'TAPING_2',     // 兼容组装演示
    '检验': 'QC', '质检': 'QC'
  };

  let targetCode = 'WH';
  for(let key in procMap) {
    if(processCodeZh.includes(key)) { targetCode = procMap[key]; break; }
  }

  const proc = PROCESS_JSON.find(p => p.code === targetCode) || PROCESS_JSON[0];
  activeProcess.value = proc;

  // 2. 将外部的工单数据格式转换为执行大屏所需的格式
  activeOrder.value = {
    id: orderData.workOrderNo,
    product: orderData.productName,
    batch: orderData.batchNo,
    status: orderData.status === 'COMPLETED' ? '完成作业' : '未开工',
    qty: `${orderData.quantity} ${orderData.unit}`,
    spec: orderData.spec || '标准规格'
  };

  viewMode.value = 'process';

  // 3. 自动静默登录，方便直接演示
  if (!isOnline.value) doLogin(STAFF_DB[0]);
}

// 暴露给父组件（工单列表）使用
defineExpose({ openFromExternal });
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-50 overflow-hidden">
    <header class="shrink-0 h-[90px] bg-white border-b border-slate-200 flex items-center px-6 justify-between shadow-sm z-50 relative">

      <div class="flex items-center gap-4 shrink-0">
        <div class="w-14 h-14 bg-indigo-50 rounded-full flex items-center justify-center text-2xl font-black text-indigo-600 border border-indigo-200 shadow-inner">
          {{ currentUser ? currentUser.userName.charAt(0) : '未' }}
        </div>
        <div class="flex flex-col">
          <span class="text-xl font-black text-slate-800 tracking-wide">
            {{ activeProcess ? activeProcess.station : '车间综合大屏终端' }}
          </span>
          <span class="text-sm font-bold text-slate-500 mt-1">
            {{ activeProcess ? activeProcess.device : (currentUser ? '操作员：' + currentUser.userName : '请先刷卡登录') }}
          </span>
        </div>
      </div>

      <div class="flex-1 px-12 overflow-hidden">
        <div class="bg-slate-50 border border-slate-200 rounded-2xl h-[64px] flex items-center px-6 shadow-inner w-full transition-all" :class="!activeOrder.id ? 'opacity-50' : ''">
          <div class="flex items-center gap-6 w-full whitespace-nowrap overflow-hidden">
            <span class="bg-indigo-600 text-white text-sm font-bold px-3 py-1.5 rounded-md shadow-sm">当前工单</span>
            <span class="text-3xl font-mono font-black text-slate-800 tracking-tighter">{{ activeOrder?.id || '--' }}</span>
            <div class="w-[3px] h-8 bg-slate-200 shrink-0"></div>
            <span class="text-2xl font-black text-indigo-700 truncate">{{ activeOrder?.product || '等待载入产品' }}</span>
            <span class="text-lg font-bold text-slate-500 bg-white border border-slate-200 px-3 py-1.5 rounded-lg ml-auto shadow-sm">
              目标量: <span class="text-indigo-600 font-black">{{ activeOrder?.qty || '--' }}</span> | 规格: {{ activeOrder?.spec || '--' }}
            </span>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-6 shrink-0">


        <div class="h-14 w-[2px] bg-slate-200"></div>

        <div class="flex flex-col items-end justify-center">
          <span class="text-sm font-bold text-slate-500 mb-1">执行状态</span>
          <div class="flex items-center gap-2 bg-white px-4 py-1.5 rounded-full border border-slate-200 shadow-sm">
            <span class="relative flex h-3 w-3">
              <span v-if="activeOrder?.status === '执行中'" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-3 w-3" :class="{'bg-emerald-500': activeOrder?.status === '执行中', 'bg-amber-500': activeOrder?.status === '未开工', 'bg-blue-500': activeOrder?.status === '完成作业'}"></span>
            </span>
            <span class="text-lg font-black text-slate-700">{{ activeOrder?.status || '未知' }}</span>
          </div>
        </div>

        <div class="h-14 w-[2px] bg-slate-200 mx-2"></div>

        <div class="flex items-center gap-6">
          <div class="text-right leading-tight">
            <div class="text-xl font-mono font-black text-slate-800 tracking-tighter">{{ currentTime.split(' ')[1] }}</div>
            <div class="text-slate-400 font-bold text-xs mt-1">{{ currentTime.split(' ')[0] }}</div>
          </div>

          <template v-if="!isOnline">
            <Button type="primary" size="large" @click="openLoginModal" class="h-14 bg-indigo-600 border-none px-6 rounded-xl shadow-lg text-lg font-bold transition-transform hover:scale-105 hover:bg-indigo-500">
              <IconifyIcon icon="lucide:id-card" class="mr-2 text-2xl" /> 上班刷卡
            </Button>
          </template>
          <template v-else>
            <div class="flex items-center gap-3 h-14 bg-white px-3 rounded-xl border-2 border-slate-200 shadow-sm cursor-pointer hover:border-red-300 transition-colors group" @click="handleClockOut" title="点击下班">
              <Avatar shape="square" :src="currentUser?.avatar" :size="40" class="border border-indigo-100 shadow-sm" />
              <div class="flex flex-col justify-center">
                <span class="text-sm font-black leading-none text-slate-800 group-hover:text-red-600 transition-colors">{{ currentUser?.userName }}</span>
                <span class="text-indigo-600 text-xs mt-1 font-bold">在线: {{ currentUser?.loginTime }}</span>
              </div>
              <IconifyIcon icon="lucide:log-out" class="text-slate-300 group-hover:text-red-500 ml-2 text-2xl transition-colors" />
            </div>
          </template>
        </div>

      </div>
    </header>
    <main class="flex-1 overflow-hidden relative">
      <transition name="fade-slide">
        <div v-if="viewMode === 'dashboard'" class="absolute inset-0 overflow-y-auto no-scrollbar p-4">
          <Row :gutter="[20, 20]">
            <Col v-for="proc in PROCESS_JSON" :key="proc.id" :xs="24" :sm="12" :md="8" :lg="6">
              <Card hoverable @click="handleEnterProcess(proc)" class="process-card border-none shadow-sm rounded-2xl overflow-hidden group bg-white">
                <div :class="`h-1.5 bg-${proc.color}-500 w-full`" />
                <div class="p-6 flex flex-col items-center" :style="!isOnline ? 'opacity: 0.5; filter: grayscale(1);' : ''">
                  <div :class="`w-20 h-20 rounded-3xl bg-${proc.color}-50 text-${proc.color}-600 flex items-center justify-center mb-5 transition-transform group-hover:scale-110 shadow-inner`">
                    <IconifyIcon :icon="proc.icon" class="text-4xl" />
                  </div>
                  <div class="text-center w-full">
                    <h3 class="font-bold text-lg mb-1 text-slate-800">{{ proc.id }} - {{ proc.name }}</h3>
                    <p class="text-slate-400 text-xs mb-4">{{ proc.desc }}</p>
                    <Divider class="my-3" />
                    <div class="flex justify-between items-center px-2">
                      <span class="text-[10px] text-slate-300 font-mono font-bold uppercase">{{ proc.code }}</span>
                      <div class="flex items-center gap-1 text-indigo-600 font-bold text-xs">进入作业 <IconifyIcon icon="lucide:chevron-right" /></div>
                    </div>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>
        </div>

        <div v-else class="absolute inset-0 bg-white">
          <MixingProcessTerminal v-if="activeProcess?.code === 'MIXING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <CoatingProcessTerminal v-else-if="activeProcess?.code === 'COATING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <GrindingProcessTerminal v-else-if="['GRINDING_1'].includes(activeProcess?.code)" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <FineGrindingProcessTerminal v-else-if="activeProcess?.code === 'GRINDING_2'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <Taping1ProcessTerminal v-else-if="activeProcess?.code === 'TAPING_1'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <SlittingProcessTerminal v-else-if="activeProcess?.code === 'SLITTING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <StampingProcessTerminal v-else-if="activeProcess?.code === 'STAMPING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <Taping2ProcessTerminal v-else-if="activeProcess?.code === 'TAPING_2'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <CuttingProcessTerminal v-else-if="activeProcess?.code === 'CUTTING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />
          <QcProcessTerminal v-else-if="activeProcess?.code === 'QC'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" />

          <div v-else class="flex h-full flex-col items-center justify-center bg-slate-50">
            <IconifyIcon icon="lucide:hammer" class="text-6xl text-slate-300 mb-4" />
            <h2 class="text-xl font-bold text-slate-500">【{{ activeProcess?.name }}】终端正在开发中...</h2>
            <Button type="primary" class="mt-4 bg-blue-600" @click="handleBack">返回</Button>
          </div>
        </div>
      </transition>
    </main>

    <Modal v-model:open="loginModalVisible" title="操作员上班身份验证" :footer="null" centered width="400px">
      <div class="flex flex-col items-center py-6">
        <div @click="simulateSwipe" class="w-full mb-8 cursor-pointer relative group">
          <div :class="['h-40 rounded-2xl border-2 border-dashed flex flex-col items-center justify-center transition-all', isSwiping ? 'border-indigo-500 bg-indigo-50' : 'border-slate-200 bg-slate-50 group-hover:border-indigo-300']">
            <IconifyIcon :icon="isSwiping ? 'lucide:refresh-cw' : 'lucide:id-card'" :class="['text-5xl mb-3', isSwiping ? 'animate-spin text-indigo-500' : 'text-slate-300']" />
            <span class="text-sm font-bold text-slate-500">{{ isSwiping ? '正在读取卡片...' : '点击模拟刷卡' }}</span>
          </div>
        </div>
        <Form layout="vertical" class="w-full px-4">
          <FormItem label="工号"><Input v-model:value="loginForm.userId" placeholder="请输入工号" size="large" /></FormItem>
          <FormItem label="密码"><Input.Password v-model:value="loginForm.password" placeholder="请输入密码" size="large" /></FormItem>
          <Button type="primary" block size="large" class="bg-indigo-600 mt-2" @click="handleManualLogin">验证并上班</Button>
        </Form>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.process-card { transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1); border: 1px solid #f1f5f9 !important; }
.process-card:hover { transform: translateY(-8px); box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.05); }
.no-scrollbar::-webkit-scrollbar { display: none; }
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s ease-out; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(20px); }
</style>
