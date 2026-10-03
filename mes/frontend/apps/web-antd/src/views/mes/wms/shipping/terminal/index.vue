<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, Table, Switch,
  message, Modal, Divider, Form, Row, Col, Statistic, Popconfirm, Progress, Drawer, Avatar, Badge, Pagination
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 1. 基础状态与动态时间、人员
// ==================================================================================
const currentTime = ref(new Date().toLocaleString());
let timer: any = null;

onMounted(() => { timer = setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000); });
onUnmounted(() => clearInterval(timer));

const currentUser = ref({
  userName: '王发货', userId: 'W8011', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=King', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 发货任务上下文 & 挂起队列机制
// ==================================================================================
const orderScanCode = ref('');
const activeDeliveryInfo = ref<any>(null);

const scanInputRef = ref();
const scanCode = ref('');
const isReplaceLabel = ref(true);
const scannedBoxes = ref<any[]>([]);

// 挂起任务队列
const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

function handleFetchDeliveryOrder() {
  if (!orderScanCode.value) return message.warning('请输入或扫描 发货通知单 / 销售订单号');
  message.loading({ content: '拉取发货指令与车辆信息...', key: 'delivery', duration: 0.5 }).then(() => {
    activeDeliveryInfo.value = {
      deliveryNo: 'DO-' + orderScanCode.value.toUpperCase(),
      customer: '宁德时代 (CATL) - 溧阳基地',
      productName: 'T01_Film_背胶单片 (高透型)',
      targetQty: 2000,
      shippedQty: 0,
      vehicleInfo: '苏D·88888 (李师傅)'
    };
    message.success({ content: `成功锁定发货单: ${activeDeliveryInfo.value.deliveryNo}`, key: 'delivery' });
  });
}

function handleSuspendTask() {
  if (!activeDeliveryInfo.value) return;
  suspendedTasks.value.push({
    id: Date.now(),
    deliveryInfo: clone(activeDeliveryInfo.value),
    scannedBoxes: clone(scannedBoxes.value),
    isReplaceLabel: isReplaceLabel.value,
    suspendTime: new Date().toLocaleTimeString()
  });

  activeDeliveryInfo.value = null;
  scannedBoxes.value = [];
  orderScanCode.value = '';
  message.success('当前发货任务已成功挂起，工作台已清空备用！');
}

function handleResumeTask(index: number) {
  if (activeDeliveryInfo.value) return message.warning('当前已有进行中的任务，请先挂起或完结！');
  const task = suspendedTasks.value[index];
  activeDeliveryInfo.value = clone(task.deliveryInfo);
  scannedBoxes.value = clone(task.scannedBoxes);
  isReplaceLabel.value = task.isReplaceLabel;

  suspendedTasks.value.splice(index, 1);
  suspendedDrawerVisible.value = false;
  message.success(`任务 ${activeDeliveryInfo.value.deliveryNo} 已恢复！`);
  focusScanner();
}

function handleClearOrder() {
  if (scannedBoxes.value.length > 0) {
    return Modal.confirm({
      title: '防错拦截：当前有未发货数据',
      content: '强制清空将丢失当前已扫码备货的数据！请使用【挂起任务】功能。',
      okText: '挂起任务',
      cancelText: '强制清空',
      onOk: handleSuspendTask,
      onCancel: () => {
        activeDeliveryInfo.value = null;
        scannedBoxes.value = [];
        message.info('发货任务已强制清空');
      }
    });
  }
  activeDeliveryInfo.value = null;
  orderScanCode.value = '';
}

const deliveryProgress = computed(() => {
  if (!activeDeliveryInfo.value) return 0;
  return Number(((activeDeliveryInfo.value.shippedQty / activeDeliveryInfo.value.targetQty) * 100).toFixed(1));
});

// ==================================================================================
// 3. 核心扫码发货逻辑
// ==================================================================================
const boxColumns: TableColumnsType = [
  { title: '序号', dataIndex: 'index', width: 60, align: 'center' },
  { title: '厂内箱码', dataIndex: 'boxCode', width: 180 },
  { title: '数量(PCS)', dataIndex: 'qty', width: 90, align: 'right' },
  { title: '客户置换标签', dataIndex: 'customerLabel', width: 160 },
  { title: 'OQC状态', dataIndex: 'oqcStatus', width: 90, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleScanEnter() {
  const code = scanCode.value.trim();
  if (!code) return;

  if (!activeDeliveryInfo.value) { scanCode.value = ''; return message.warning('请先锁定发货单！'); }
  if (activeDeliveryInfo.value.shippedQty >= activeDeliveryInfo.value.targetQty) {
    scanCode.value = ''; return message.error('本单发货数量已达标，严禁超发！');
  }
  if (scannedBoxes.value.some(b => b.boxCode === code)) {
    scanCode.value = ''; return message.error(`条码 [${code}] 已被扫描，严禁重复装车！`);
  }

  if (code.includes('NG') || code.includes('UN')) {
    Modal.error({ title: '🚨 OQC 拦截警告！', content: `系统检测到箱码 [${code}] 尚未通过出货检验(OQC)或被判定为不良品！\n严禁发货！` });
    scanCode.value = ''; return;
  }

  const boxQty = 100; // 模拟每箱100片
  if (activeDeliveryInfo.value.shippedQty + boxQty > activeDeliveryInfo.value.targetQty) {
    scanCode.value = ''; return message.error(`该箱加入后将超出订单需求，请拆箱或更换尾数箱！`);
  }

  let customerLabel = '-';
  if (isReplaceLabel.value) {
    customerLabel = `CATL-PN-${String(Math.floor(Math.random() * 1000)).padStart(4, '0')}`;
    message.success(`已打印客户标签: ${customerLabel}`);
  }

  scannedBoxes.value.unshift({
    id: Date.now(), index: scannedBoxes.value.length + 1, boxCode: code, qty: boxQty, customerLabel, oqcStatus: '合格', time: new Date().toLocaleTimeString()
  });

  activeDeliveryInfo.value.shippedQty += boxQty;
  scanCode.value = '';

  if (activeDeliveryInfo.value.shippedQty === activeDeliveryInfo.value.targetQty) {
    Modal.success({ title: '备货完成', content: '发货目标已达成，请点击底部按钮执行出库销账。' });
  } else { focusScanner(); }
}

function handleRemoveBox(id: number) {
  const box = scannedBoxes.value.find(b => b.id === id);
  if (box) {
    scannedBoxes.value = scannedBoxes.value.filter(b => b.id !== id);
    activeDeliveryInfo.value.shippedQty -= box.qty;
    message.success(`已剔除 ${box.boxCode}`);
    focusScanner();
  }
}

function focusScanner() { nextTick(() => { scanInputRef.value?.focus(); }); }

// ==================================================================================
// 4. 发货过账与历史查询 (增强版：分页与下钻追溯)
// ==================================================================================
const historyDrawerVisible = ref(false);
const shippingHistoryLogs = ref<any[]>([]);

const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });

const historyDetailVisible = ref(false);
const currentHistoryDetail = ref<any>(null);

function handleConfirmShipping() {
  if (scannedBoxes.value.length === 0) return message.warning('备货列表为空，无法发货！');

  Modal.confirm({
    title: '确认执行发货扣账？',
    content: `将扣减成品库库存，并过账至 ERP 生成应收账款凭证。\n本次共发货: ${activeDeliveryInfo.value.shippedQty} PCS (${scannedBoxes.value.length} 箱)`,
    okText: '确认发货销账',
    onOk: () => {
      // 💡 封存发货快照供历史下钻
      shippingHistoryLogs.value.unshift({
        id: Date.now(),
        deliveryNo: activeDeliveryInfo.value.deliveryNo,
        customer: activeDeliveryInfo.value.customer,
        qty: activeDeliveryInfo.value.shippedQty,
        boxCount: scannedBoxes.value.length,
        time: new Date().toLocaleString(),
        user: currentUser.value.userName,
        snapshotList: clone(scannedBoxes.value)
      });

      message.success('发货销账成功！同步 ERP 接口已触发。');
      scannedBoxes.value = [];
      activeDeliveryInfo.value = null;
      orderScanCode.value = '';
    }
  });
}

const filteredHistory = computed(() => {
  let filtered = shippingHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.deliveryNo.toLowerCase().includes(kw) || log.customer.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});

function viewHistoryDetail(log: any) {
  currentHistoryDetail.value = log;
  historyDetailVisible.value = true;
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-teal-600 text-white p-1.5 rounded-lg flex items-center justify-center shadow-inner">
          <IconifyIcon icon="lucide:truck" class="text-xl" />
        </div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">出货发货防错工作台</span>
      </div>

      <div class="flex items-center gap-5 shrink-0">
        <Badge :count="suspendedTasks.length" :offset="[-5, 5]">
          <Button type="primary" ghost class="font-bold border-teal-200 text-teal-700" @click="suspendedDrawerVisible = true">
            <IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列
          </Button>
        </Badge>

        <Button type="dashed" class="border-slate-300 text-slate-600 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;">
          <IconifyIcon icon="lucide:history" class="mr-1" /> 发货销账历史
        </Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="text-right leading-tight hidden md:block">
          <div class="text-xs font-mono font-bold text-slate-800">{{ currentTime.split(' ')[1] }}</div>
          <div class="text-slate-400 text-[9px]">{{ currentTime.split(' ')[0] }}</div>
        </div>
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-teal-100 shadow-sm" />
            <div class="flex flex-col">
              <span class="text-xs font-bold leading-none text-slate-800">{{ currentUser.userName }}</span>
              <span class="text-teal-600 text-[9px] mt-1 font-bold">在线: {{ currentUser.loginTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">

      <div class="shrink-0 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden flex flex-col transition-all">
        <div class="bg-slate-50 border-b border-slate-100 p-2 px-4 flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-2"><IconifyIcon icon="lucide:clipboard-check" /> 发货通知单指令</span>

          <div class="flex gap-2" v-if="activeDeliveryInfo">
            <Button type="primary" class="bg-orange-500 border-none shadow-sm shadow-orange-200 font-bold text-xs h-7" @click="handleSuspendTask">
              <IconifyIcon icon="lucide:pause-circle" class="mr-1" /> 临时挂起任务
            </Button>
            <Button type="default" danger size="small" class="h-7" @click="handleClearOrder">完结/清空</Button>
          </div>
        </div>

        <div class="p-4 flex items-center gap-6">
          <div v-if="!activeDeliveryInfo" class="flex-1 flex items-center gap-4">
            <Input.Search v-model:value="orderScanCode" placeholder="扫描发货单、提货单或销售单号..." enter-button="锁定发货任务" size="large" @search="handleFetchDeliveryOrder" class="max-w-md shadow-sm" />
            <span class="text-xs text-slate-400">※ 请锁定单据获取客户信息及目标。若需继续之前的任务，请点击右上角【挂起队列】。</span>
          </div>

          <div v-else class="flex-1 flex items-center justify-between animate-fade-in w-full">
            <div class="flex gap-8 items-center">
              <div class="flex flex-col">
                <span class="text-[10px] text-slate-400 font-bold mb-1">发货单号</span>
                <span class="font-mono font-black text-teal-700 text-lg">{{ activeDeliveryInfo.deliveryNo }}</span>
              </div>
              <Divider type="vertical" class="h-10 bg-slate-200" />
              <div class="flex flex-col">
                <span class="text-[10px] text-slate-400 font-bold mb-1">客户目的地 & 承运车辆</span>
                <span class="font-bold text-slate-800 text-sm">{{ activeDeliveryInfo.customer }}</span>
                <span class="text-xs text-slate-500 font-mono">{{ activeDeliveryInfo.vehicleInfo }}</span>
              </div>
            </div>
            <div class="w-72 bg-slate-50 p-2 rounded-lg border border-slate-100 shadow-inner">
              <div class="flex justify-between text-xs font-bold mb-1">
                <span class="text-slate-500">备货装车进度</span>
                <span :class="activeDeliveryInfo.shippedQty === activeDeliveryInfo.targetQty ? 'text-green-600' : 'text-teal-600'">{{ activeDeliveryInfo.shippedQty }} / {{ activeDeliveryInfo.targetQty }} PCS</span>
              </div>
              <Progress :percent="deliveryProgress" :strokeColor="{'0%': '#2dd4bf', '100%': '#0d9488'}" size="small" :showInfo="false" />
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex gap-3 min-h-0 relative">
        <div v-if="!activeDeliveryInfo" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 rounded-lg border border-slate-200">
          <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-teal-500" />
          <span class="font-bold text-lg text-slate-700">请先在上方锁定发货单指令，或从挂起队列中恢复</span>
        </div>

        <div class="w-[360px] flex flex-col gap-3 min-h-0">
          <Row :gutter="12" class="shrink-0">
            <Col :span="12"><Card size="small" class="bg-blue-50 border-blue-100 shadow-sm"><Statistic title="已扫外箱数" :value="scannedBoxes.length" valueStyle="color: #2563eb; font-weight: bold; font-size: 18px" /></Card></Col>
            <Col :span="12"><Card size="small" class="bg-green-50 border-green-100 shadow-sm"><Statistic title="累计备货件数" :value="activeDeliveryInfo?.shippedQty || 0" valueStyle="color: #16a34a; font-weight: bold; font-size: 18px" /></Card></Col>
          </Row>

          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
            <div class="p-3 border-b shrink-0 bg-teal-50 flex items-center justify-between">
              <span class="text-sm font-black text-teal-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 防错扫码装车</span>
            </div>
            <div class="flex-1 p-4 flex flex-col justify-between overflow-y-auto">
              <div>
                <div class="p-3 border border-orange-200 bg-orange-50 rounded-lg mb-6">
                  <div class="flex items-center justify-between mb-1">
                    <span class="font-bold text-orange-800 text-sm">标签置换 (厂内转客户)</span>
                    <Switch v-model:checked="isReplaceLabel" size="small" />
                  </div>
                  <p class="text-[10px] text-orange-600 leading-tight m-0">开启后，扫入厂内码将自动驱动斑马打印机输出客户专用的外箱标签格式。</p>
                </div>
                <Form.Item class="mb-0 bg-teal-50 p-3 rounded-lg border border-teal-200 shadow-inner">
                  <div class="text-xs font-bold text-teal-800 mb-2">扫描成品外箱码 (自动OQC校验)</div>
                  <Input ref="scanInputRef" v-model:value="scanCode" size="large" class="font-mono text-xl border-teal-400 py-3" placeholder="等待扫码..." @pressEnter="handleScanEnter" :disabled="activeDeliveryInfo && activeDeliveryInfo.shippedQty >= activeDeliveryInfo.targetQty">
                    <template #prefix><IconifyIcon icon="lucide:barcode" class="text-teal-500 text-xl mr-2" /></template>
                  </Input>
                </Form.Item>
                <div class="mt-4 text-xs text-slate-400 flex flex-col gap-1">
                  <span class="flex items-center gap-1"><IconifyIcon icon="lucide:shield-check" class="text-green-500"/> 自动拦截未过 OQC 质检的产品</span>
                  <span class="flex items-center gap-1"><IconifyIcon icon="lucide:shield-check" class="text-green-500"/> 自动拦截数量超发、错发产品</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> 备货装车明细台账</div><span class="text-[10px] text-slate-400">支持扫错解绑</span>
          </div>
          <div class="flex-1 flex flex-col">
            <Table :columns="boxColumns" :dataSource="scannedBoxes" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 230px)' }" class="vben-schema-table flex-1">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'boxCode'"><span class="font-mono font-bold text-slate-700">{{ record.boxCode }}</span></template>
                <template v-if="column.dataIndex === 'oqcStatus'"><Tag color="green"><IconifyIcon icon="lucide:check-circle" class="mr-1"/>合格</Tag></template>
                <template v-if="column.dataIndex === 'customerLabel'">
                  <Tag v-if="record.customerLabel !== '-'" color="purple" class="font-mono">{{ record.customerLabel }}</Tag>
                  <span v-else class="text-slate-300">-</span>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Popconfirm title="确定要将该箱退出发货队列吗？" @confirm="handleRemoveBox(record.id)">
                    <Button type="text" danger size="small">撤下装车</Button>
                  </Popconfirm>
                </template>
              </template>
              <template #emptyText>
                <div class="py-20 flex flex-col items-center text-slate-400">
                  <IconifyIcon icon="lucide:scan-face" class="text-5xl mb-3 opacity-30" /><span>暂无备货记录，请在左侧扫码装车。</span>
                </div>
              </template>
            </Table>
          </div>
        </div>
      </div>
    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4">
        <Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出发货台</Button>
      </div>
      <div class="flex gap-3 items-center">
        <Button type="primary" class="bg-teal-700 border-none font-bold px-10 shadow-lg hover:bg-teal-600" :disabled="!activeDeliveryInfo || activeDeliveryInfo.shippedQty === 0" @click="handleConfirmShipping">
          <IconifyIcon icon="lucide:send" class="mr-2" /> 确认发货出库 (ERP 销账)
        </Button>
      </div>
    </footer>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ 发货任务挂起队列" placement="left" width="400">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400">
        <IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>当前没有被挂起的发货任务。</span>
      </div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-teal-200 bg-teal-50 shadow-sm hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-2">
            <div class="font-mono font-bold text-teal-800 text-base">{{ task.deliveryInfo.deliveryNo }}</div><Tag color="orange" class="!m-0">已挂起</Tag>
          </div>
          <div class="text-xs text-slate-600 mb-1">客户: <span class="font-bold">{{ task.deliveryInfo.customer }}</span></div>
          <div class="text-xs text-slate-600 mb-3">进度保留 <span class="font-bold text-teal-600">{{ task.deliveryInfo.shippedQty }}</span> PCS | 已扫外箱 <span class="font-bold text-teal-600">{{ task.scannedBoxes.length }}</span> 箱</div>
          <div class="flex justify-between items-center pt-2 border-t border-teal-100">
            <span class="text-[10px] text-slate-400">挂起时间: {{ task.suspendTime }}</span>
            <Button type="primary" size="small" class="bg-teal-600 text-xs font-bold border-none" @click="handleResumeTask(idx)">恢复此任务</Button>
          </div>
        </Card>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 销售发货过账记录" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <div class="mb-3 shrink-0">
          <Input.Search v-model:value="historySearchForm.keyword" placeholder="搜索单号、客户..." allow-clear @search="historyPagination.current = 1" />
        </div>
        <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 flex flex-col gap-3">
          <div v-if="filteredHistory.length === 0" class="flex flex-col items-center justify-center h-48 text-slate-400">
            <IconifyIcon icon="lucide:search-x" class="text-4xl mb-2 opacity-50" /><span>未找到发货销账记录</span>
          </div>

          <Card v-for="log in filteredHistory" :key="log.id" size="small" class="border border-slate-200 bg-white shadow-sm hover:border-teal-300 hover:shadow-md transition-all cursor-pointer group" @click="viewHistoryDetail(log)">
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-slate-800 text-[15px] flex items-center"><IconifyIcon icon="lucide:file-check-2" class="text-teal-600 mr-2 text-lg"/>{{ log.deliveryNo }}</div>
              <Tag color="cyan" class="!m-0">已销账</Tag>
            </div>
            <div class="text-xs text-slate-600 mb-1">目标客户: <span class="font-bold">{{ log.customer }}</span></div>
            <div class="text-xs text-slate-600 mb-3">发货总数: <span class="font-bold text-teal-600">{{ log.qty }}</span> PCS (共 {{ log.boxCount }} 箱)</div>
            <div class="flex justify-between items-center pt-2 border-t border-slate-100">
              <span class="text-[10px] text-slate-400">{{ log.time }} / {{ log.user }}</span>
              <span class="text-xs text-teal-600 font-bold flex items-center opacity-0 group-hover:opacity-100 transition-opacity">查看装车明细 <IconifyIcon icon="lucide:chevron-right" /></span>
            </div>
          </Card>
        </div>
        <div class="shrink-0 pt-3 border-t mt-2 text-right">
          <Pagination v-model:current="historyPagination.current" :total="historyPagination.total" :pageSize="historyPagination.pageSize" size="small" show-less-items />
        </div>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDetailVisible" :title="`装车明细: ${currentHistoryDetail?.deliveryNo}`" placement="right" width="550">
      <div class="bg-white rounded-lg border border-slate-200 h-full overflow-hidden flex flex-col shadow-inner">
        <div class="p-3 bg-slate-50 border-b flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600">总计包含外箱：{{ currentHistoryDetail?.boxCount }} 箱</span>
          <span class="text-xs text-slate-400">发货时间：{{ currentHistoryDetail?.time }}</span>
        </div>
        <Table :columns="boxColumns.filter(c => c.dataIndex !== 'action')" :dataSource="currentHistoryDetail?.snapshotList" :pagination="false" size="small" class="vben-schema-table flex-1 overflow-y-auto" />
      </div>
    </Drawer>

  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }

:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.2) !important; border-color: #14b8a6 !important; }

.vben-schema-table :deep(.ant-table-wrapper), .vben-schema-table :deep(.ant-spin-nested-loading), .vben-schema-table :deep(.ant-spin-container), .vben-schema-table :deep(.ant-table) { height: 100%; display: flex; flex-direction: column; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; color: #475569 !important; font-size: 11px; font-weight: bold; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px 12px !important; font-size: 12px; }
.vben-schema-table :deep(.ant-table-row:hover > td) { background: #f0fdfa !important; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
.animate-fade-in { animation: fadeIn 0.2s ease-out forwards; }
</style>
