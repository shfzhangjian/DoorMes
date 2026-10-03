<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Radio, Divider, Form, Row, Col, Progress, Drawer, Avatar, Pagination, Modal, message
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 1. 基础状态
// ==================================================================================
const currentTime = ref(new Date().toLocaleString());
let timer: any = null;

onMounted(() => { timer = setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000); });
onUnmounted(() => clearInterval(timer));

const currentUser = ref({
  userName: '周盘点', userId: 'W8055', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Zhou', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 盘点任务上下文 (明盘模式)
// ==================================================================================
const planScanCode = ref('');
const activePlanInfo = ref<any>(null);

const locationInputRef = ref();
const itemInputRef = ref();
const locationCode = ref('');
const isLocationLocked = ref(false);

const itemScanCode = ref('');
const actualQty = ref<number | null>(null);
const inventoryList = ref<any[]>([]); // 账面应有库存清单

function handleFetchPlan() {
  if (!planScanCode.value) return message.warning('请输入盘点计划单号');

  message.loading({ content: '拉取盘点计划与账面库存...', key: 'count', duration: 0.5 }).then(() => {
    activePlanInfo.value = {
      planNo: 'CC-' + planScanCode.value.toUpperCase(),
      zone: 'A区-树脂存放区 (A01货架)',
      type: '动态动盘 (Cycle Count)',
      status: '盘点中'
    };

    // 模拟拉取该区域的账面库存数据
    inventoryList.value = [
      { id: '1', barcode: 'RM-A01-001', productName: 'PET光学基膜', location: 'BIN-A01-01', sysQty: 100, actualQty: null, variance: null, status: 'PENDING' },
      { id: '2', barcode: 'RM-A01-002', productName: 'PET光学基膜', location: 'BIN-A01-01', sysQty: 100, actualQty: null, variance: null, status: 'PENDING' },
      { id: '3', barcode: 'RM-B02-005', productName: '离型膜(高透)', location: 'BIN-A01-02', sysQty: 50,  actualQty: null, variance: null, status: 'PENDING' }
    ];

    message.success({ content: `锁定盘点单: ${activePlanInfo.value.planNo}`, key: 'count' });
    focusLocationScanner();
  });
}

function handleClearPlan() {
  Modal.confirm({
    title: '确认退出当前盘点？',
    content: '未提交的实盘数据将会丢失。',
    onOk: () => { activePlanInfo.value = null; inventoryList.value = []; planScanCode.value = ''; }
  });
}

// 进度计算 (已盘点行数 / 总行数)
const countProgress = computed(() => {
  if (!activePlanInfo.value || inventoryList.value.length === 0) return 0;
  const counted = inventoryList.value.filter(item => item.status !== 'PENDING').length;
  return Number(((counted / inventoryList.value.length) * 100).toFixed(1));
});

// ==================================================================================
// 3. 实盘扫码与差异核算
// ==================================================================================
const itemColumns: TableColumnsType = [
  { title: '储位', dataIndex: 'location', width: 100, align: 'center' },
  { title: '物料/批次条码', dataIndex: 'barcode', width: 160 },
  { title: '账面库存 (Sys)', dataIndex: 'sysQty', width: 100, align: 'right' },
  { title: '实盘数量 (Actual)', dataIndex: 'actualQty', width: 120, align: 'right' },
  { title: '差异 (Var)', dataIndex: 'variance', width: 100, align: 'right' },
  { title: '盘点结果', dataIndex: 'status', width: 120, align: 'center' },
];

function handleLockLocation() {
  if (!locationCode.value.trim()) return message.warning('请扫描货架/库位条码');
  isLocationLocked.value = true;
  message.success(`已锁定当前盘点库位: ${locationCode.value}`);
  focusItemScanner();
}

function handleUnlockLocation() {
  isLocationLocked.value = false; locationCode.value = ''; focusLocationScanner();
}

function handleScanItemEnter() {
  const code = itemScanCode.value.trim();
  if (!activePlanInfo.value) { itemScanCode.value = ''; return message.warning('请先锁定盘点单！'); }
  if (!isLocationLocked.value) { itemScanCode.value = ''; return message.error('请先锁定正在盘点的物理库位！'); }
  if (!code) return message.warning('请输入物料条码');

  const targetItem = inventoryList.value.find(item => item.barcode === code && item.location === locationCode.value);
  const qty = actualQty.value; // 如果为空，默认按账面数量确认 (快速盘点模式)

  if (targetItem) {
    // 💡 场景 A：系统里有这个码 -> 更新实盘数量
    const finalQty = qty !== null ? qty : targetItem.sysQty;
    targetItem.actualQty = finalQty;
    targetItem.variance = finalQty - targetItem.sysQty;

    if (targetItem.variance === 0) targetItem.status = 'MATCHED';
    else if (targetItem.variance > 0) targetItem.status = 'SURPLUS'; // 账面有，但实物变多了(极少见，通常是单位转换误差)
    else targetItem.status = 'SHORTAGE'; // 盘亏

    message.success(`条码 [${code}] 盘点记录已更新！差异: ${targetItem.variance}`);
  } else {
    // 💡 场景 B：系统里没有这个码，但在该货架上扫出来了 -> 盘盈 (Surplus)
    if (qty === null) {
      itemScanCode.value = '';
      return message.error('发现账外物料！作为【盘盈】入账必须手工输入实盘数量！');
    }

    inventoryList.value.unshift({
      id: Date.now(),
      barcode: code,
      productName: '未知物料 (待确认)',
      location: locationCode.value,
      sysQty: 0,
      actualQty: qty,
      variance: qty,
      status: 'SURPLUS' // 纯盘盈
    });
    message.success(`发现账外条码 [${code}]，已登记为【盘盈】。`);
  }

  // 重置扫描框
  itemScanCode.value = '';
  actualQty.value = null;
  focusItemScanner();
}

// 快捷操作：将未扫码的本库位物资一键标记为【盘亏】(0库存)
function markRemainingAsShortage() {
  let count = 0;
  inventoryList.value.forEach(item => {
    if (item.location === locationCode.value && item.status === 'PENDING') {
      item.actualQty = 0;
      item.variance = -item.sysQty;
      item.status = 'SHORTAGE';
      count++;
    }
  });
  if (count > 0) message.success(`已将该库位剩余未盘到的 ${count} 笔物料标记为盘亏(缺失)！`);
  else message.warning('该库位下没有待盘点项。');
}

function focusLocationScanner() { nextTick(() => { locationInputRef.value?.focus(); }); }
function focusItemScanner() { nextTick(() => { itemInputRef.value?.focus(); }); }

// ==================================================================================
// 4. 盘点过账与凭证生成
// ==================================================================================
const historyDrawerVisible = ref(false);
const countHistoryLogs = ref<any[]>([]);
const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });

function handleConfirmCount() {
  const pendingCount = inventoryList.value.filter(i => i.status === 'PENDING').length;
  if (pendingCount > 0) {
    return Modal.warning({
      title: '盘点未完成', content: `还有 ${pendingCount} 笔账面库存未盘点！\n请核实实物，或将其标记为盘亏(数量为0)后再提交。`
    });
  }

  const surplus = inventoryList.value.filter(i => i.status === 'SURPLUS').length;
  const shortage = inventoryList.value.filter(i => i.status === 'SHORTAGE').length;

  Modal.confirm({
    title: '确认提交盘点结果并过账？',
    content: `系统将生成调整凭证：\n盘盈(多出): ${surplus} 笔\n盘亏(短少): ${shortage} 笔\n\n注意：差异过大可能会触发财务复核流程。`,
    okText: '确认生成差异单',
    onOk: () => {
      countHistoryLogs.value.unshift({
        id: Date.now(),
        planNo: activePlanInfo.value.planNo,
        zone: activePlanInfo.value.zone,
        totalItems: inventoryList.value.length,
        surplus, shortage,
        time: new Date().toLocaleString(),
        user: currentUser.value.userName,
        snapshot: clone(inventoryList.value)
      });

      message.success('✅ 盘点结果已提交！库存差异单已推入审批流。');
      activePlanInfo.value = null;
      inventoryList.value = [];
      planScanCode.value = '';
      handleUnlockLocation();
    }
  });
}

const filteredHistory = computed(() => {
  let filtered = countHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.planNo.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-purple-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:clipboard-check" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 库存盘点审计台</span>
      </div>
      <div class="flex items-center gap-5 shrink-0">
        <Button type="dashed" class="border-purple-200 text-purple-700 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;"><IconifyIcon icon="lucide:history" class="mr-1" /> 盘点历史与差异单</Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="text-right leading-tight hidden md:block">
          <div class="text-xs font-mono font-bold text-slate-800">{{ currentTime.split(' ')[1] }}</div>
          <div class="text-slate-400 text-[9px]">{{ currentTime.split(' ')[0] }}</div>
        </div>
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-purple-100" />
            <div class="flex flex-col"><span class="text-xs font-bold leading-none">{{ currentUser.userName }}</span></div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">

      <div class="shrink-0 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden flex flex-col transition-all">
        <div class="bg-slate-50 border-b border-slate-100 p-2 px-4 flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-2"><IconifyIcon icon="lucide:target" /> 锁定盘点计划</span>
          <Button v-if="activePlanInfo" type="default" danger size="small" class="h-7" @click="handleClearPlan">退出盘点</Button>
        </div>

        <div class="p-3 flex items-start gap-6">
          <div v-if="!activePlanInfo" class="flex-1 flex items-center gap-4 py-2">
            <Input.Search v-model:value="planScanCode" placeholder="扫描或输入盘点计划单号 (如: CC-2026...)" enter-button="获取账面明细" size="large" @search="handleFetchPlan" class="max-w-md shadow-sm" />
            <span class="text-xs text-slate-400">※ 请锁定系统下发的盘点单，获取待盘的账面应有库存。</span>
          </div>
          <div v-else class="flex-1 flex w-full gap-4 items-center justify-between animate-fade-in">
            <div class="flex gap-8 items-center">
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">盘点计划单</span><span class="font-mono font-black text-purple-700 text-lg">{{ activePlanInfo.planNo }}</span></div>
              <Divider type="vertical" class="h-10 bg-slate-200" />
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">目标盘点库区</span><span class="font-bold text-slate-800 text-sm">{{ activePlanInfo.zone }}</span></div>
              <Divider type="vertical" class="h-10 bg-slate-200" />
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">作业类型</span><Tag color="purple" class="!m-0">{{ activePlanInfo.type }}</Tag></div>
            </div>

            <div class="w-72 bg-slate-50 p-2 rounded-lg border border-slate-100 shadow-inner">
              <div class="flex justify-between text-xs font-bold mb-1">
                <span class="text-slate-500">明细行盘点进度</span>
                <span :class="countProgress === 100 ? 'text-green-600' : 'text-purple-600'">{{ countProgress }}%</span>
              </div>
              <Progress :percent="countProgress" :strokeColor="{'0%': '#c084fc', '100%': '#9333ea'}" size="small" :showInfo="false" class="m-0" />
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex gap-3 min-h-0 relative">
        <div v-if="!activePlanInfo" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 rounded-lg border border-slate-200">
          <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-purple-500" />
          <span class="font-bold text-lg text-slate-700">请先在上方锁定盘点任务单</span>
        </div>

        <div class="w-[380px] flex flex-col gap-3 min-h-0">
          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
            <div class="p-3 border-b shrink-0 bg-purple-50 flex items-center justify-between">
              <span class="text-sm font-black text-purple-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 实盘数量录入</span>
            </div>
            <div class="flex-1 p-4 flex flex-col overflow-y-auto custom-scrollbar">

              <div class="mb-5">
                <div class="text-xs font-bold text-slate-600 mb-2 flex items-center gap-1"><IconifyIcon icon="lucide:map-pin" class="text-orange-500"/> 步骤 1: 扫描正在盘点的货架</div>
                <div v-if="!isLocationLocked" class="flex gap-2">
                  <Input ref="locationInputRef" v-model:value="locationCode" placeholder="扫描物理库位条码" size="large" @pressEnter="handleLockLocation" class="font-mono border-orange-300"><template #prefix><IconifyIcon icon="lucide:barcode" class="text-orange-400" /></template></Input>
                  <Button type="primary" class="bg-orange-500 border-none font-bold" size="large" @click="handleLockLocation">锁定</Button>
                </div>
                <div v-else class="p-3 bg-green-50 border border-green-300 rounded-lg flex justify-between items-center shadow-inner">
                  <div class="flex flex-col"><span class="text-[10px] text-green-600 font-bold mb-1">当前盘点物理库位</span><span class="font-mono font-black text-green-700 text-xl">{{ locationCode }}</span></div>
                  <Button type="link" danger size="small" @click="handleUnlockLocation">解锁更换</Button>
                </div>
              </div>

              <Divider class="my-2 border-slate-100" />

              <div :class="['p-4 rounded-lg border shadow-inner transition-all', isLocationLocked ? 'bg-purple-50 border-purple-200' : 'bg-slate-50 border-slate-200 opacity-50 pointer-events-none']">
                <div class="text-xs font-bold text-purple-800 mb-4 flex items-center gap-1"><IconifyIcon icon="lucide:box" class="text-purple-500"/> 步骤 2: 扫描物资标签与数量核对</div>

                <Form layout="vertical">
                  <div class="mb-4">
                    <div class="text-xs font-bold text-slate-600 mb-2 flex justify-between">
                      <span>实盘数量录入</span>
                      <span class="font-normal text-[10px] text-purple-600">留空则默认按账面应有数量确认</span>
                    </div>
                    <InputNumber v-model:value="actualQty" size="large" class="w-full font-bold shadow-sm" :min="0" placeholder="默认相符免填..." />
                  </div>

                  <Form.Item class="mb-0 mt-2">
                    <div class="text-[10px] text-slate-500 mb-1">扫描物料条码 (自动回车核对账务)</div>
                    <Input ref="itemInputRef" v-model:value="itemScanCode" size="large" class="font-mono text-xl border-purple-400 py-3 shadow-sm" placeholder="扫描实物条码..." @pressEnter="handleScanItemEnter">
                      <template #prefix><IconifyIcon icon="lucide:barcode" class="text-purple-500 text-xl mr-2" /></template>
                    </Input>
                  </Form.Item>
                </Form>

                <div class="mt-6 pt-4 border-t border-purple-100/50">
                  <Popconfirm title="确认当前库位(架)上的物资已全部扫完？未扫码的账面库存将被标记为盘亏！" @confirm="markRemainingAsShortage">
                    <Button type="primary" danger ghost block size="small" class="font-bold border-red-300">
                      <IconifyIcon icon="lucide:alert-circle" class="mr-1"/> 货架已清空，剩余未盘物料标为缺失
                    </Button>
                  </Popconfirm>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> 账实核对差异明细表 (Variance Ledger)</div>
          </div>
          <div class="flex-1 flex flex-col">
            <Table :columns="itemColumns" :dataSource="inventoryList" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 230px)' }" class="vben-schema-table flex-1">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'location'"><span class="font-mono text-slate-500 text-[10px]">{{ record.location }}</span></template>
                <template v-if="column.dataIndex === 'barcode'">
                  <div class="font-mono font-bold text-slate-800">{{ record.barcode }}</div>
                  <div class="text-[10px] text-slate-400 truncate">{{ record.productName }}</div>
                </template>
                <template v-if="column.dataIndex === 'sysQty'">
                  <span class="font-mono text-slate-500">{{ record.sysQty }}</span>
                </template>
                <template v-if="column.dataIndex === 'actualQty'">
                  <span class="font-mono font-bold text-slate-800">{{ record.actualQty !== null ? record.actualQty : '-' }}</span>
                </template>

                <template v-if="column.dataIndex === 'variance'">
                  <span v-if="record.variance === null">-</span>
                  <span v-else-if="record.variance === 0" class="font-mono font-bold text-green-600">0</span>
                  <span v-else-if="record.variance > 0" class="font-mono font-bold text-blue-600">+{{ record.variance }}</span>
                  <span v-else class="font-mono font-bold text-red-600">{{ record.variance }}</span>
                </template>

                <template v-if="column.dataIndex === 'status'">
                  <Tag v-if="record.status === 'PENDING'" color="default"><IconifyIcon icon="lucide:clock" class="mr-1"/>待盘点</Tag>
                  <Tag v-else-if="record.status === 'MATCHED'" color="green"><IconifyIcon icon="lucide:check" class="mr-1"/>账实相符</Tag>
                  <Tag v-else-if="record.status === 'SURPLUS'" color="blue"><IconifyIcon icon="lucide:arrow-up-circle" class="mr-1"/>盘盈入账</Tag>
                  <Tag v-else-if="record.status === 'SHORTAGE'" color="red"><IconifyIcon icon="lucide:arrow-down-circle" class="mr-1"/>盘亏缺失</Tag>
                </template>
              </template>
              <template #emptyText>
                <div class="py-20 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:target" class="text-5xl mb-3 opacity-30" /><span>锁定盘点单后加载账面数据。</span></div>
              </template>
            </Table>
          </div>
        </div>
      </div>
    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4"><Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出盘点台</Button></div>
      <div class="flex gap-3 items-center">
        <Button type="primary" class="bg-purple-700 border-none font-bold px-10 shadow-lg" :disabled="!activePlanInfo" @click="handleConfirmCount">
          <IconifyIcon icon="lucide:saveAll" class="mr-2" /> 生成盘点差异单并结束过账
        </Button>
      </div>
    </footer>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 历史盘点差异单" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <Input.Search v-model:value="historySearchForm.keyword" placeholder="搜索盘点单号..." class="mb-3 shrink-0" allow-clear />
        <div class="flex-1 overflow-y-auto pr-2 flex flex-col gap-3">
          <Card v-for="log in filteredHistory" :key="log.id" size="small" class="border border-slate-200">
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-purple-700">{{ log.planNo }}</div>
              <span class="text-[10px] text-slate-400">{{ log.time }}</span>
            </div>
            <div class="text-xs text-slate-600 mb-1">盘点库区: <span class="font-bold">{{ log.zone }}</span></div>
            <div class="text-xs mt-2 grid grid-cols-3 gap-2 bg-slate-50 p-2 rounded">
              <div class="text-center"><div class="text-slate-400 mb-1">总明细</div><div class="font-bold">{{ log.totalItems }}</div></div>
              <div class="text-center"><div class="text-slate-400 mb-1">盘盈(单)</div><div class="font-bold text-blue-600">{{ log.surplus }}</div></div>
              <div class="text-center"><div class="text-slate-400 mb-1">盘亏(单)</div><div class="font-bold text-red-600">{{ log.shortage }}</div></div>
            </div>
          </Card>
        </div>
      </div>
    </Drawer>

  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.2) !important; border-color: #a855f7 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 6px 8px !important; font-size: 12px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
.animate-fade-in { animation: fadeIn 0.2s ease-out forwards; }
</style>
