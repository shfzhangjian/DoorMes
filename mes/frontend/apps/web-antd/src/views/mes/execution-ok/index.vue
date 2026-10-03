<script lang="ts" setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { Card, Input, Row, Col, Button, Avatar, Divider, message, Modal, Form, FormItem, Select } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useRouter } from 'vue-router';

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

const router = useRouter();
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
function simulateSwipe() { isSwiping.value = true; setTimeout(() => { doLogin(STAFF_DB[0]); isSwiping.value = false; }, 1500); }
function doLogin(staff: any) { currentUser.value = { ...staff, avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${staff.seed}`, loginTime: new Date().toLocaleTimeString() }; isOnline.value = true; loginModalVisible.value = false; message.success(`欢迎上班，${staff.userName}！`); }
function handleManualLogin() { const staff = STAFF_DB.find(u => u.userId === loginForm.value.userId && u.pwd === loginForm.value.password); if (staff) doLogin(staff); else message.error('工号或密码错误'); }
function handleClockOut() { Modal.confirm({ title: '确认下班', content: '下班后将解绑当前工位的所有操作权限，是否继续？', onOk: () => { isOnline.value = false; currentUser.value = null; viewMode.value = 'dashboard'; activeProcess.value = null; message.warn('已下班，工位已释放'); } }); }

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
];

const activeProcess = ref<any>(null);
const activeOrder = ref({ id: 'WO-20260225-001', product: '电子显示高透抛光垫 (Pro版)', qty: 1000, spec: 'T01-0.15' });

function handleEnterProcess(proc: any) {
  if (!isOnline.value) return openLoginModal();
  activeProcess.value = proc;
  viewMode.value = 'process';
}
function handleBack() { viewMode.value = 'dashboard'; activeProcess.value = null; }

// ==================== [迁移] 切换工单逻辑 ====================
const switchOrderModalVisible = ref(false);
const switchScanInput = ref('');
const pendingOrders = ref([
  { id: 'WO-20260225-001', product: '电子显示高透抛光垫 (Pro版)', qty: 1000, spec: 'T01-0.15' },
  { id: 'WO-20260225-002', product: '柔性打磨垫 (定制版)', qty: 300, spec: 'FLEX-0.12' },
  { id: 'WO-20260225-003', product: '高透抛光垫 (Ultra版)', qty: 1500, spec: 'ULT-0.20' }
]);

function openSwitchOrderModal() { switchScanInput.value = ''; switchOrderModalVisible.value = true; }
function handleSwitchScan() { if (!switchScanInput.value) return message.warning('请先输入工单条码！'); confirmSwitchOrder(switchScanInput.value.toUpperCase()); }
function confirmSwitchOrder(newWo: string) {
  if (newWo === activeOrder.value.id) return message.warning('目标工单与当前执行工单相同！');
  Modal.confirm({
    title: '⚠️ 确认挂起当前工单？',
    content: '切换将挂起当前生产任务，并清空当前未提交的采集项，是否继续？',
    okText: '确认强制切换', cancelText: '取消',
    onOk: () => {
      const matched = pendingOrders.value.find(o => o.id === newWo);
      activeOrder.value = matched || { id: newWo, product: '临时新排产物料', qty: 500, spec: '标准' };
      switchOrderModalVisible.value = false;
      message.success(`✅ 已成功切换至工单：${newWo}，请按标准重新开机点检！`);
    }
  });
}

// ==================== [迁移] 异常拉灯与交接班（作为父级方法供子视图调用） ====================
const abnormalModalVisible = ref(false);
const abnormalForm = reactive({ reason: null, desc: '' });
function openAbnormalModal() { abnormalForm.reason = null; abnormalForm.desc = ''; abnormalModalVisible.value = true; }
function submitAbnormalStop() {
  if (!abnormalForm.reason) return message.warning('请选择停工原因！');
  message.error(`🚨 已拉响安灯！机台已被锁定。停工原因: ${abnormalForm.reason}`);
  abnormalModalVisible.value = false;
}
function goToHandover() {
  router.push({ path: '/mes/production/shop-floor/handover', query: { workstation: activeProcess.value?.station } });
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-50 overflow-hidden">

    <header   class="shrink-0 h-[90px] bg-white border-b border-slate-200 flex items-center px-6 justify-between shadow-sm z-50 relative">

      <div class="flex items-center gap-4 shrink-0 w-[280px]">
        <div class="w-14 h-14 bg-indigo-50 rounded-full flex items-center justify-center text-2xl font-black text-indigo-600 border border-indigo-200 shadow-inner">
          {{ currentUser ? currentUser.userName.charAt(0) : '未' }}
        </div>
        <div class="flex flex-col">
          <span class="text-xl font-black text-slate-800 tracking-wide truncate">{{ activeProcess?.station }}</span>
          <span class="text-sm font-bold text-slate-500 mt-1 truncate">{{ activeProcess?.device }} ({{ currentUser?.userName || '未登录' }})</span>
        </div>
      </div>

      <div class="flex-1 px-8 flex justify-center min-w-0">
        <div class="bg-slate-50 border border-slate-200 hover:border-indigo-400 hover:shadow-md cursor-pointer rounded-2xl h-[64px] flex items-center px-6 shadow-inner transition-all w-full max-w-5xl" @click="openSwitchOrderModal">
          <div class="flex items-center w-full gap-4">
            <span class="bg-indigo-600 text-white text-sm font-bold px-3 py-1.5 rounded-md shadow-sm shrink-0">当前工单</span>
            <span class="text-3xl font-mono font-black text-slate-800 tracking-tighter shrink-0">{{ activeOrder.id }}</span>
            <div class="w-[3px] h-8 bg-slate-200 shrink-0 mx-2"></div>
            <span class="text-2xl font-black text-indigo-700 truncate flex-1 min-w-0">{{ activeOrder.product }}</span>
            <span class="text-lg font-bold text-slate-500 bg-white border border-slate-200 px-3 py-1.5 rounded-lg shadow-sm shrink-0 whitespace-nowrap">
              目标: <span class="text-indigo-600 font-black">{{ activeOrder.qty }}</span> | 规格: {{ activeOrder.spec }}
            </span>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-6 shrink-0 w-[240px] justify-end">
        <div class="text-right leading-tight">
          <div class="text-xl font-mono font-black text-slate-800 tracking-tighter">{{ currentTime.split(' ')[1] }}</div>
          <div class="text-slate-400 font-bold text-xs mt-1">{{ currentTime.split(' ')[0] }}</div>
        </div>
        <template v-if="!isOnline">
          <Button type="primary" size="large" @click="openLoginModal" class="h-14 bg-indigo-600 border-none px-6 rounded-xl shadow-lg text-lg font-bold">
            上班刷卡
          </Button>
        </template>
        <template v-else>
          <div class="flex items-center gap-3 h-14 bg-white px-3 rounded-xl border-2 border-slate-200 shadow-sm cursor-pointer hover:border-red-300 transition-colors group" @click="handleClockOut" title="点击下班">
            <Avatar shape="square" :src="currentUser?.avatar" :size="40" class="border border-indigo-100 shadow-sm" />
            <IconifyIcon icon="lucide:log-out" class="text-slate-300 group-hover:text-red-500 ml-1 text-2xl transition-colors" />
          </div>
        </template>
      </div>
    </header>

    <main class="flex-1 overflow-hidden relative">
      <transition name="fade-slide">
        <div v-if="viewMode === 'dashboard'" class="absolute inset-0 overflow-y-auto no-scrollbar p-6">
          <Row :gutter="[24, 24]">
            <Col v-for="proc in PROCESS_JSON" :key="proc.id" :xs="24" :sm="12" :md="8" :lg="6">
              <Card hoverable @click="handleEnterProcess(proc)" class="process-card border-none shadow-sm rounded-3xl overflow-hidden group bg-white">
                <div :class="`h-2 bg-${proc.color}-500 w-full`" />
                <div class="p-8 flex flex-col items-center" :style="!isOnline ? 'opacity: 0.5; filter: grayscale(1);' : ''">
                  <div :class="`w-24 h-24 rounded-[2rem] bg-${proc.color}-50 text-${proc.color}-600 flex items-center justify-center mb-6 transition-transform group-hover:scale-110 shadow-inner`">
                    <IconifyIcon :icon="proc.icon" class="text-5xl" />
                  </div>
                  <div class="text-center w-full">
                    <h3 class="font-black text-2xl mb-2 text-slate-800">{{ proc.name }}</h3>
                    <p class="text-slate-400 text-sm mb-6">{{ proc.desc }}</p>
                    <Divider class="my-4" />
                    <div class="flex justify-between items-center px-2">
                      <span class="text-xs text-slate-300 font-mono font-bold uppercase">{{ proc.station }}</span>
                      <div class="flex items-center gap-1 text-indigo-600 font-bold text-sm">进入 <IconifyIcon icon="lucide:arrow-right" /></div>
                    </div>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>
        </div>

        <div v-else class="absolute inset-0 bg-white">
          <MixingProcessTerminal v-if="activeProcess?.code === 'MIXING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <CoatingProcessTerminal v-else-if="activeProcess?.code === 'COATING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <GrindingProcessTerminal v-else-if="['GRINDING_1'].includes(activeProcess?.code)" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <FineGrindingProcessTerminal v-else-if="activeProcess?.code === 'GRINDING_2'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <Taping1ProcessTerminal v-else-if="activeProcess?.code === 'TAPING_1'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <SlittingProcessTerminal v-else-if="activeProcess?.code === 'SLITTING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <StampingProcessTerminal v-else-if="activeProcess?.code === 'STAMPING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <Taping2ProcessTerminal v-else-if="activeProcess?.code === 'TAPING_2'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <CuttingProcessTerminal v-else-if="activeProcess?.code === 'CUTTING'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
          <QcProcessTerminal v-else-if="activeProcess?.code === 'QC'" :operator="currentUser" :order="activeOrder" :process="activeProcess" @close="handleBack" @abnormal="openAbnormalModal" @handover="goToHandover" />
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

    <Modal v-model:open="switchOrderModalVisible" title="🔄 切换生产工单" :width="650" :footer="false" centered>
      <div class="p-6 bg-slate-50">
        <div class="mb-6 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <div class="text-sm font-bold text-slate-600 mb-3 flex items-center"><IconifyIcon icon="lucide:barcode" class="mr-2 text-lg"/> 方式一：直接扫码 / 输入条码切换</div>
          <Input.Search
            v-model:value="switchScanInput"
            placeholder="使用扫码枪扫入工单条码..."
            size="large"
            enter-button="确认切换"
            @search="handleSwitchScan"
            class="h-12 text-lg"
          />
          <p class="text-center text-slate-400 mt-4 text-sm">支持 USB/蓝牙扫码枪直接输入，回车键自动确认</p>
        </div>

        <div class="flex items-center gap-4 mb-4">
          <div class="h-[1px] bg-slate-200 flex-1"></div>
          <div class="text-slate-400 font-bold text-sm">或者</div>
          <div class="h-[1px] bg-slate-200 flex-1"></div>
        </div>

        <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <div class="text-sm font-bold text-slate-600 mb-3 flex items-center"><IconifyIcon icon="lucide:list-todo" class="mr-2 text-lg"/> 方式二：从车间排产池选择</div>
          <div class="flex flex-col gap-3 max-h-[250px] overflow-y-auto pr-2">
            <div v-for="wo in pendingOrders" :key="wo.id" @click="confirmSwitchOrder(wo.id)"
                 class="border-2 border-slate-100 p-4 rounded-xl flex justify-between items-center hover:border-indigo-500 hover:bg-indigo-50/50 cursor-pointer transition-all shadow-sm group">
              <div class="flex flex-col">
                <span class="font-black text-lg text-slate-800 group-hover:text-indigo-700">{{ wo.id }}</span>
                <span class="text-slate-500 text-sm mt-1">{{ wo.product }} <span class="mx-2 text-slate-300">|</span> 计划量: <span class="font-bold text-slate-700">{{ wo.qty }} PCS</span></span>
              </div>
              <Button type="primary" class="bg-indigo-600 font-bold opacity-0 group-hover:opacity-100 transition-opacity">选中切换</Button>
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="abnormalModalVisible" title="🚨 设备异常停工提报" :width="500" :footer="false" centered>
      <div class="p-6">
        <div class="mb-4">
          <span class="block text-sm font-bold text-slate-600 mb-2">异常原因分类</span>
          <Select
            v-model:value="abnormalForm.reason"
            class="w-full" size="large"
            :options="[{label: '设备机械故障', value: '设备机械故障'}, {label: '工艺品质异常', value: '工艺品质异常'}, {label: '缺料等待', value: '缺料等待'}]"
            placeholder="请选择停机主要原因"
          />
        </div>
        <div class="mb-8">
          <span class="block text-sm font-bold text-slate-600 mb-2">详细情况说明 (选填)</span>
          <Input.TextArea v-model:value="abnormalForm.desc" :rows="4" placeholder="请简要描述异常现象，方便机修/品质人员排查..." class="bg-slate-50" />
        </div>
        <div class="flex gap-4">
          <Button size="large" class="flex-1 font-bold" @click="abnormalModalVisible = false">取消</Button>
          <Button size="large" type="primary" danger class="flex-1 font-bold" @click="submitAbnormalStop">
            <IconifyIcon icon="lucide:siren" class="mr-1" /> 确认停工拉灯
          </Button>
        </div>
      </div>
    </Modal>

  </div>
</template>

<style scoped>
.process-card { transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1); border: 1px solid #f1f5f9 !important; }
.process-card:hover { transform: translateY(-8px); box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.05); }
.no-scrollbar::-webkit-scrollbar { display: none; }
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease-out; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(15px); }
</style>
