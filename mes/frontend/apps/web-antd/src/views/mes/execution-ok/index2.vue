<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Card, Tag, Input, Row, Col, Button, Avatar, Popconfirm, Divider, message, Badge, Popover, List, Modal, Form, FormItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import MixingProcessTerminal from './MixingProcessTerminal.vue';
import CoatingProcessTerminal from './CoatingProcessTerminal.vue';
import GrindingProcessTerminal from './GrindingProcessTerminal.vue';
import FineGrindingProcessTerminal from './FineGrindingProcessTerminal.vue';
import Taping1ProcessTerminal from './Taping1ProcessTerminal.vue';
import SlittingProcessTerminal from './SlittingProcessTerminal.vue';
import StampingProcessTerminal from './StampingProcessTerminal.vue';
import Taping2ProcessTerminal from './Taping2ProcessTerminal.vue';
import CuttingProcessTerminal from './CuttingProcessTerminal.vue'; // 💡 引入单片裁圆工序
import QcProcessTerminal from './QcProcessTerminal.vue';
// ================= 1. 基础状态与动态时间 =================
const viewMode = ref<'dashboard' | 'process'>('dashboard');
const currentTime = ref(new Date().toLocaleString());
let timer: any = null;

onMounted(() => {
  timer = setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000);
});
onUnmounted(() => clearInterval(timer));

// ================= 2. 刷卡/下班弹窗逻辑 (业务安全增强) =================
const isOnline = ref(false);
const loginModalVisible = ref(false);
const currentUser = ref<any>(null);
const loginForm = ref({ userId: '', password: '' });
const isSwiping = ref(false); // 模拟刷卡动画状态

// 模拟人员数据库
const STAFF_DB = [
  { userId: 'U1001', userName: '张伟', role: '高级主操', seed: 'Felix', pwd: '123' }
];

function openLoginModal() {
  loginForm.value = { userId: '', password: '' };
  loginModalVisible.value = true;
}

function simulateSwipe() {
  isSwiping.value = true;
  setTimeout(() => {
    const staff = STAFF_DB[0];
    doLogin(staff);
    isSwiping.value = false;
  }, 1500);
}

function doLogin(staff: any) {
  currentUser.value = {
    ...staff,
    avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${staff.seed}`,
    loginTime: new Date().toLocaleTimeString()
  };
  isOnline.value = true;
  loginModalVisible.value = false;
  message.success(`欢迎上班，${staff.userName}！`);
}

function handleManualLogin() {
  const staff = STAFF_DB.find(u => u.userId === loginForm.value.userId && u.pwd === loginForm.value.password);
  if (staff) {
    doLogin(staff);
  } else {
    message.error('工号或密码错误');
  }
}

function handleClockOut() {
  Modal.confirm({
    title: '确认下班',
    content: '下班后将解绑当前工位的所有操作权限，是否继续？',
    onOk: () => {
      isOnline.value = false;
      currentUser.value = null;
      viewMode.value = 'dashboard';
      activeProcess.value = null;
      message.warn('已下班，工位已释放');
    }
  });
}

// ================= 3. 动态工序与工单数据 =================
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
  { id: 'WO20260218002', product: '特种薄膜', batch: 'B260218-B', status: '作业前准备', qty: '2000kg', spec: 'T01-0.20' },
  { id: 'WO20260218003', product: '反射膜', batch: 'B260218-C', status: '配方投料', qty: '3000kg', spec: 'T01-0.10' },
  { id: 'WO20260218004', product: '导电膜', batch: 'B260218-D', status: '作业中', qty: '1500kg', spec: 'T01-0.30' },
  { id: 'WO20260218005', product: '保护膜', batch: 'B260218-E', status: '完成作业', qty: '4000kg', spec: 'T01-0.05' }
]);

const activeProcess = ref<any>(null);
const activeOrder = ref(workOrders.value[0]);
const orderListVisible = ref(false);

function handleEnterProcess(proc: any) {
  if (!isOnline.value) return openLoginModal();
  activeProcess.value = proc;
  viewMode.value = 'process';
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-slate-50 overflow-hidden">

    <header class="flex shrink-0 h-14 items-center justify-between border-b bg-white px-4 shadow-sm z-50">
      <div class="flex items-center gap-4 flex-1 min-w-0">
        <div class="flex items-center gap-2 cursor-pointer shrink-0 hover:bg-slate-50 px-2 py-1 rounded transition-colors" @click="viewMode = 'dashboard'">
          <IconifyIcon icon="lucide:layout-panel-top" class="text-indigo-600 text-2xl" />
          <span class="font-black text-lg tracking-tighter uppercase text-slate-800">工位执行看板</span>
        </div>

        <Divider type="vertical" class="bg-slate-300 h-6 shrink-0" />

        <div class="flex flex-col justify-center min-w-[100px] shrink-0">
          <span class="text-xs font-bold uppercase text-slate-700">{{ activeProcess ? activeProcess.station : '请选择工序' }}</span>
          <span class="text-slate-400 text-[10px]">{{ activeProcess ? activeProcess.device : '等待选择设备' }}</span>
        </div>

        <Divider type="vertical" class="bg-slate-300 h-6 shrink-0" />

        <div class="flex-1 max-w-2xl px-2">
          <Popover v-model:open="orderListVisible" trigger="click" placement="bottomLeft">
            <template #content>
              <div class="w-80 p-1">
                <div class="mb-3"><Input.Search placeholder="输入/扫描派工单..." size="small" /></div>
                <List :data-source="workOrders" size="small" class="max-h-60 overflow-y-auto">
                  <template #renderItem="{ item }">
                    <List.Item class="cursor-pointer hover:bg-indigo-50 p-2 rounded-lg transition-all" @click="activeOrder = item; orderListVisible = false">
                      <div class="flex flex-col w-full text-xs">
                        <div class="flex justify-between items-center font-bold">
                          <span>{{ item.id }}</span>
                          <Tag :color="item.status === 'pending' ? 'default' : (item.status === 'finished' ? 'success' : 'processing')">
                            {{ item.status }}
                          </Tag>
                        </div>
                        <span class="text-slate-400">{{ item.product }}</span>
                      </div>
                    </List.Item>
                  </template>
                </List>
              </div>
            </template>
            <div class="flex items-center gap-4 bg-slate-50 border border-slate-200 rounded-lg p-1.5 hover:border-indigo-400 cursor-pointer transition-all">
              <div class="bg-indigo-600 text-white p-1.5 rounded flex items-center justify-center shrink-0">
                <IconifyIcon icon="lucide:clipboard-list" class="text-lg" />
              </div>
              <div class="grid grid-cols-3 flex-1 gap-x-4 min-w-0">
                <div class="flex flex-col min-w-0">
                  <span class="text-[9px] text-slate-400 uppercase font-bold truncate">工单编号</span>
                  <span class="text-xs font-mono font-bold text-slate-800 truncate">{{ activeOrder.id }}</span>
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-[9px] text-slate-400 uppercase font-bold truncate">产品/状态</span>
                  <span class="text-xs font-bold text-slate-800 truncate">{{ activeOrder.product }} / {{ activeOrder.status }}</span>
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-[9px] text-slate-400 uppercase font-bold truncate">计划数量</span>
                  <span class="text-xs font-bold text-slate-800 truncate">{{ activeOrder.qty }}</span>
                </div>
              </div>
              <IconifyIcon icon="lucide:chevrons-up-down" class="text-slate-300 mr-2 shrink-0" />
            </div>
          </Popover>
        </div>
      </div>

      <div class="flex items-center gap-6 shrink-0">
        <div class="text-right leading-tight hidden xl:block">
          <div class="text-xs font-mono font-bold text-slate-800">{{ currentTime.split(' ')[1] }}</div>
          <div class="text-slate-400 text-[9px]">{{ currentTime.split(' ')[0] }}</div>
        </div>

        <div class="h-10 flex items-center bg-white px-2 rounded-xl border border-slate-200 shadow-sm">
          <template v-if="!isOnline">
            <Button type="primary" size="small" @click="openLoginModal" class="bg-indigo-600 border-none px-4">
              <template #icon><IconifyIcon icon="lucide:id-card" /></template> 上班刷卡
            </Button>
          </template>
          <template v-else>
            <div class="flex items-center gap-3 pr-2">
              <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-indigo-100 shadow-sm" />
              <div class="flex flex-col">
                <span class="text-xs font-bold leading-none text-slate-800">{{ currentUser.userName }}</span>
                <span class="text-indigo-600 text-[9px] mt-1 font-bold">在线: {{ currentUser.loginTime }}</span>
              </div>
              <IconifyIcon icon="lucide:log-out" class="text-slate-300 hover:text-red-500 cursor-pointer ml-2" @click="handleClockOut" />
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

          <MixingProcessTerminal
            v-if="activeProcess?.code === 'MIXING'"
            :operator="currentUser"
            :order="activeOrder"
            :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <CoatingProcessTerminal
            v-else-if="activeProcess?.code === 'COATING'"
            :operator="currentUser"
            :order="activeOrder"
            :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />
          <GrindingProcessTerminal
            v-else-if="['GRINDING_1'].includes(activeProcess?.code)"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <FineGrindingProcessTerminal
            v-else-if="activeProcess?.code === 'GRINDING_2'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <Taping1ProcessTerminal
            v-else-if="activeProcess?.code === 'TAPING_1'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <SlittingProcessTerminal
            v-else-if="activeProcess?.code === 'SLITTING'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />
          <StampingProcessTerminal
            v-else-if="activeProcess?.code === 'STAMPING'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />
          <Taping2ProcessTerminal
            v-else-if="activeProcess?.code === 'TAPING_2'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <CuttingProcessTerminal
            v-else-if="activeProcess?.code === 'CUTTING'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <QcProcessTerminal
            v-else-if="activeProcess?.code === 'QC'"
            :operator="currentUser" :order="activeOrder" :process="activeProcess"
            @close="viewMode = 'dashboard'"
          />

          <div v-else class="flex h-full flex-col items-center justify-center bg-slate-50">
            <IconifyIcon icon="lucide:hammer" class="text-6xl text-slate-300 mb-4" />
            <h2 class="text-xl font-bold text-slate-500">【{{ activeProcess?.name }}】终端正在开发中...</h2>
            <Button type="primary" class="mt-4 bg-blue-600" @click="viewMode = 'dashboard'">返回看板</Button>
          </div>

        </div>




      </transition>
    </main>

    <Modal v-model:open="loginModalVisible" title="操作员上班身份验证" :footer="null" centered width="400px">
      <div class="flex flex-col items-center py-6">
        <div @click="simulateSwipe" class="w-full mb-8 cursor-pointer relative group">
          <div :class="['h-40 rounded-2xl border-2 border-dashed flex flex-col items-center justify-center transition-all', isSwiping ? 'border-indigo-500 bg-indigo-50' : 'border-slate-200 bg-slate-50 group-hover:border-indigo-300']">
            <IconifyIcon :icon="isSwiping ? 'lucide:refresh-cw' : 'lucide:id-card'" :class="['text-5xl mb-3', isSwiping ? 'animate-spin text-indigo-500' : 'text-slate-300']" />
            <span class="text-sm font-bold text-slate-500">{{ isSwiping ? '正在读取卡片信息...' : '请将工卡靠近感应区或点击模拟刷卡' }}</span>
          </div>
          <div v-if="isSwiping" class="absolute inset-0 bg-white/20 backdrop-blur-[1px] rounded-2xl flex items-center justify-center"></div>
        </div>

        <Divider>或者手动输入</Divider>

        <Form layout="vertical" class="w-full px-4">
          <FormItem label="工号 (User ID)">
            <Input v-model:value="loginForm.userId" placeholder="请输入工号" size="large"><template #prefix><IconifyIcon icon="lucide:user" /></template></Input>
          </FormItem>
          <FormItem label="登录密码">
            <Input.Password v-model:value="loginForm.password" placeholder="请输入密码" size="large"><template #prefix><IconifyIcon icon="lucide:lock" /></template></Input.Password>
          </FormItem>
          <Button type="primary" block size="large" class="bg-indigo-600 mt-2" @click="handleManualLogin">验证并上班</Button>
        </Form>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
/* 卡片悬浮特效 */
.process-card { transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1); border: 1px solid #f1f5f9 !important; }
.process-card:hover { transform: translateY(-8px); box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.05); }

/* 隐藏外部滚动条，只保留内部容器滚动 */
.no-scrollbar::-webkit-scrollbar { display: none; }

/* 路由级切换动画 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s ease-out; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(20px); }
</style>
