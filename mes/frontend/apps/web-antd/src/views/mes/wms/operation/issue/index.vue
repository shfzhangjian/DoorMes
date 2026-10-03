<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Radio, Select,
  message, Modal, Divider, Form, Row, Col, Progress, Drawer, Avatar, Badge, Pagination, Popconfirm, Descriptions
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
  userName: '孙发料', userId: 'W8033', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Sun', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 业务模式与任务上下文 (发料 Issue vs 退料 Return)
// ==================================================================================
const operationMode = ref<'ISSUE' | 'RETURN'>('ISSUE');
const orderScanCode = ref('');

const activeOrderInfo = ref<any>(null);
const activeLineId = ref<string | null>(null);

const locationInputRef = ref();
const itemInputRef = ref();
const locationCode = ref('');
const isLocationLocked = ref(false);

const itemScanCode = ref('');
const issueQty = ref<number>(1);
const scannedItems = ref<any[]>([]);

const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

function handleModeChange() {
  if (scannedItems.value.length > 0) {
    message.warning('当前有未过账的数据，请先挂起或过账后再切换模式！');
    setTimeout(() => { operationMode.value = operationMode.value === 'ISSUE' ? 'RETURN' : 'ISSUE'; }, 10);
    return;
  }
  activeOrderInfo.value = null;
  orderScanCode.value = '';
}

function handleFetchOrder() {
  if (!orderScanCode.value) return message.warning('请输入领料单号或生产工单号');

  message.loading({ content: '拉取明细需求...', key: 'issue', duration: 0.5 }).then(() => {
    if (operationMode.value === 'ISSUE') {
      activeOrderInfo.value = {
        orderNo: 'REQ-' + orderScanCode.value.toUpperCase(),
        workOrder: 'WO20260219-001',
        targetStation: '配料站-01 / 涂布线-02',
        lines: [
          { id: 'L01', productCode: 'RM-RESIN-01', productName: '光学级PET树脂', targetQty: 1000, processedQty: 0, unit: 'KG' },
          { id: 'L02', productCode: 'RM-SOLV-05', productName: '特种交联剂', targetQty: 50, processedQty: 0, unit: 'KG' }
        ]
      };
    } else {
      activeOrderInfo.value = {
        orderNo: 'RET-' + orderScanCode.value.toUpperCase(),
        workOrder: 'WO20260219-001',
        targetStation: '原材料总仓 / 废品隔离库',
        lines: [
          { id: 'L01', productCode: 'RM-RESIN-01', productName: '光学级PET树脂(尾料)', targetQty: 150, processedQty: 0, unit: 'KG' }
        ]
      };
    }
    activeLineId.value = activeOrderInfo.value.lines[0].id;
    message.success({ content: `锁定单据: ${activeOrderInfo.value.orderNo}`, key: 'issue' });
    focusLocationScanner();
  });
}

function handleSuspendTask() {
  if (!activeOrderInfo.value) return;
  suspendedTasks.value.push({
    id: Date.now(), mode: operationMode.value, orderInfo: clone(activeOrderInfo.value), activeLineId: activeLineId.value,
    scannedItems: clone(scannedItems.value), locationCode: locationCode.value, isLocationLocked: isLocationLocked.value, suspendTime: new Date().toLocaleTimeString()
  });
  activeOrderInfo.value = null; scannedItems.value = []; locationCode.value = ''; isLocationLocked.value = false; orderScanCode.value = '';
  message.success('任务已挂起！');
}

function handleResumeTask(index: number) {
  if (activeOrderInfo.value) return message.warning('当前有进行中的任务，请先挂起！');
  const task = suspendedTasks.value[index];
  operationMode.value = task.mode; activeOrderInfo.value = clone(task.orderInfo); activeLineId.value = task.activeLineId;
  scannedItems.value = clone(task.scannedItems); locationCode.value = task.locationCode; isLocationLocked.value = task.isLocationLocked;
  suspendedTasks.value.splice(index, 1); suspendedDrawerVisible.value = false;
  message.success(`任务 ${activeOrderInfo.value.orderNo} 已恢复！`);
  focusItemScanner();
}

function handleClearOrder() {
  if (scannedItems.value.length > 0) {
    return Modal.confirm({
      title: '防错拦截', content: '当前有未过账数据。确认要强制清空吗？',
      onOk: () => { activeOrderInfo.value = null; scannedItems.value = []; orderScanCode.value = ''; }
    });
  }
  activeOrderInfo.value = null; orderScanCode.value = '';
}

const isOrderCompleted = computed(() => {
  if (!activeOrderInfo.value) return false;
  return activeOrderInfo.value.lines.every((line: any) => line.processedQty >= line.targetQty);
});

// ==================================================================================
// 3. 扫码校验与扣减记账
// ==================================================================================
const itemColumns: TableColumnsType = [
  { title: '物料/批次条码', dataIndex: 'barcode', width: 170 },
  { title: '物料名称', dataIndex: 'productName', width: 140 },
  { title: '关联库位', dataIndex: 'location', width: 100 },
  { title: '本次数量', dataIndex: 'qty', width: 80, align: 'right' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleLockLocation() {
  if (!locationCode.value.trim()) return message.warning('请扫描货架/库位条码');
  isLocationLocked.value = true;
  message.success(`已锁定库位: ${locationCode.value}`);
  focusItemScanner();
}

function handleUnlockLocation() {
  isLocationLocked.value = false; locationCode.value = ''; focusLocationScanner();
}

function handleScanItemEnter() {
  let code = itemScanCode.value.trim();
  if (!activeOrderInfo.value) { itemScanCode.value = ''; return message.warning('请先锁定领料单据！'); }
  if (!isLocationLocked.value) { itemScanCode.value = ''; return message.error('请先扫描并锁定相关库位！'); }
  if (!activeLineId.value) { itemScanCode.value = ''; return message.error('请选择当前操作的物料行！'); }
  if (!code) return message.warning('请输入物料条码');

  const currentLine = activeOrderInfo.value.lines.find((l: any) => l.id === activeLineId.value);
  if (scannedItems.value.some(b => b.barcode === code)) {
    itemScanCode.value = ''; return message.error(`条码已被扫描，严禁重复！`);
  }

  const currentQty = issueQty.value || 1;
  if (currentLine.processedQty + currentQty > currentLine.targetQty) {
    itemScanCode.value = ''; return message.error(`🚨 超发预警：超出领料单需求数量！`);
  }

  scannedItems.value.unshift({
    id: Date.now() + Math.random(), barcode: code, productId: currentLine.productCode, productName: currentLine.productName,
    location: locationCode.value, qty: currentQty, unit: currentLine.unit, time: new Date().toLocaleTimeString()
  });

  currentLine.processedQty += currentQty;
  itemScanCode.value = '';

  if (isOrderCompleted.value) {
    Modal.success({ title: '操作完成', content: '单据内所有物料均已齐套，请点击底部执行过账。' });
  } else { focusItemScanner(); }
}

function handleRemoveItem(id: number) {
  const item = scannedItems.value.find(b => b.id === id);
  if (item) {
    scannedItems.value = scannedItems.value.filter(b => b.id !== id);
    const line = activeOrderInfo.value.lines.find((l: any) => l.productCode === item.productId);
    if (line) line.processedQty -= item.qty;
    focusItemScanner();
  }
}

function focusLocationScanner() { nextTick(() => { locationInputRef.value?.focus(); }); }
function focusItemScanner() { nextTick(() => { itemInputRef.value?.focus(); }); }

// ==================================================================================
// 4. 写账逻辑、流水凭证与交接单打印 (💡 核心升级区)
// ==================================================================================
const historyDrawerVisible = ref(false);
const issueHistoryLogs = ref<any[]>([]);
const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });
const historyDetailVisible = ref(false);
const currentHistoryDetail = ref<any>(null);

// 打印弹窗状态
const printModalVisible = ref(false);
const currentSlipData = ref<any>(null);

function handleConfirmTransaction() {
  if (scannedItems.value.length === 0) return message.warning('操作明细为空！');

  let totalProcessed = 0;
  activeOrderInfo.value.lines.forEach((l: any) => totalProcessed += l.processedQty);
  const actionName = operationMode.value === 'ISSUE' ? '发料出库 (-)' : '退料入库 (+)';

  Modal.confirm({
    title: `确认执行【${actionName}】过账？`,
    content: `即将向『出入库流水账』写入 ${scannedItems.value.length} 笔事务记录。`,
    onOk: () => {
      // 💡 1. 生成唯一流水号 (交易号)
      const slipPrefix = operationMode.value === 'ISSUE' ? 'TRX-IS' : 'TRX-RT';
      const newSlipNo = `${slipPrefix}-20260219-${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`;

      // 💡 2. 构建过账日志与凭证数据
      const logData = {
        id: Date.now(),
        slipNo: newSlipNo, // 流水号存入历史
        orderNo: activeOrderInfo.value.orderNo,
        mode: operationMode.value === 'ISSUE' ? '生产发料' : '余料退库',
        workOrder: activeOrderInfo.value.workOrder,
        targetStation: activeOrderInfo.value.targetStation,
        totalQty: totalProcessed,
        itemCount: scannedItems.value.length,
        time: new Date().toLocaleString(),
        user: currentUser.value.userName,
        snapshotList: clone(scannedItems.value)
      };

      issueHistoryLogs.value.unshift(logData);

      // 💡 3. 呼出“交接单打印”弹窗
      currentSlipData.value = logData;
      printModalVisible.value = true;

      // 4. 清空工作台
      scannedItems.value = [];
      activeOrderInfo.value = null;
      orderScanCode.value = '';
      handleUnlockLocation();
    }
  });
}

const filteredHistory = computed(() => {
  let filtered = issueHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.orderNo.toLowerCase().includes(kw) || log.slipNo.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});

function viewHistoryDetail(log: any) { currentHistoryDetail.value = log; historyDetailVisible.value = true; }

function printSlip() {
  message.success('已下发打印指令！交接单打印中...');
  printModalVisible.value = false;
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-indigo-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:shopping-cart" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 生产领退料工作台</span>
      </div>
      <div class="flex items-center gap-5 shrink-0">
        <Radio.Group v-model:value="operationMode" button-style="solid" @change="handleModeChange" class="mr-4">
          <Radio.Button value="ISSUE"><IconifyIcon icon="lucide:arrow-right-from-line" class="mr-1 inline-block"/> 生产发料 (出库)</Radio.Button>
          <Radio.Button value="RETURN"><IconifyIcon icon="lucide:arrow-left-to-line" class="mr-1 inline-block"/> 余单退料 (入库)</Radio.Button>
        </Radio.Group>
        <Badge :count="suspendedTasks.length" :offset="[-5, 5]">
          <Button type="primary" ghost class="font-bold border-indigo-200" @click="suspendedDrawerVisible = true"><IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列</Button>
        </Badge>
        <Button type="dashed" class="border-indigo-200 text-indigo-600 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;"><IconifyIcon icon="lucide:history" class="mr-1" /> 过账流水</Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-indigo-100" />
            <div class="flex flex-col"><span class="text-xs font-bold leading-none">{{ currentUser.userName }}</span></div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">

      <div class="shrink-0 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden flex flex-col transition-all">
        <div class="bg-slate-50 border-b border-slate-100 p-2 px-4 flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-2"><IconifyIcon :icon="operationMode === 'ISSUE' ? 'lucide:clipboard-minus' : 'lucide:clipboard-plus'" /> {{ operationMode === 'ISSUE' ? '当前发料指令明细 (BOM)' : '当前退料交接单明细' }}</span>
          <div class="flex gap-2" v-if="activeOrderInfo">
            <Button type="primary" class="bg-orange-500 border-none shadow-sm font-bold text-xs h-7" @click="handleSuspendTask"><IconifyIcon icon="lucide:pause-circle" class="mr-1" /> 临时挂起</Button>
            <Button type="default" danger size="small" class="h-7" @click="handleClearOrder">清空单据</Button>
          </div>
        </div>

        <div class="p-3 flex items-start gap-6">
          <div v-if="!activeOrderInfo" class="flex-1 flex items-center gap-4 py-2">
            <Input.Search v-model:value="orderScanCode" :placeholder="operationMode === 'ISSUE' ? '扫描生产领料单/工单号...' : '扫描车间退料单...'" enter-button="锁定任务" size="large" @search="handleFetchOrder" class="max-w-md shadow-sm" />
          </div>
          <div v-else class="flex-1 flex w-full gap-4">
            <div class="w-64 shrink-0 flex flex-col gap-3 pr-4 border-r border-slate-100">
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">单号</span><span class="font-mono font-black text-indigo-700 text-base">{{ activeOrderInfo.orderNo }}</span></div>
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">工单 / 目标车间</span><span class="font-bold text-slate-800 text-xs">{{ activeOrderInfo.workOrder }} ({{ activeOrderInfo.targetStation }})</span></div>
            </div>

            <div class="flex-1 grid grid-cols-2 md:grid-cols-3 gap-3 overflow-y-auto max-h-24 custom-scrollbar pr-2">
              <div v-for="line in activeOrderInfo.lines" :key="line.id" :class="['p-2 rounded border text-xs relative overflow-hidden transition-colors cursor-pointer', line.processedQty >= line.targetQty ? 'bg-green-50 border-green-200' : (activeLineId === line.id ? 'bg-indigo-50 border-indigo-400 shadow-sm' : 'bg-slate-50 border-slate-200 hover:border-indigo-300')]" @click="activeLineId = line.id">
                <div v-if="activeLineId === line.id && line.processedQty < line.targetQty" class="absolute top-0 right-0 bg-indigo-500 text-white text-[9px] px-1 rounded-bl">执行中</div>
                <div class="font-bold text-slate-800 truncate mb-1" :title="line.productName">{{ line.productName }}</div>
                <div class="flex justify-between items-center font-mono text-slate-500 mb-1"><span class="text-[10px]">{{ line.productCode }}</span><span :class="line.processedQty >= line.targetQty ? 'text-green-600 font-bold' : 'text-indigo-600 font-bold'">{{ line.processedQty }} / {{ line.targetQty }}</span></div>
                <Progress :percent="Number(((line.processedQty / line.targetQty) * 100).toFixed(1))" :strokeColor="line.processedQty >= line.targetQty ? '#16a34a' : '#4f46e5'" size="small" :showInfo="false" class="m-0" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex gap-3 min-h-0 relative">
        <div v-if="!activeOrderInfo" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 rounded-lg border border-slate-200">
          <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-indigo-500" />
          <span class="font-bold text-lg text-slate-700">请先锁定业务单据</span>
        </div>

        <div class="w-[420px] flex flex-col gap-3 min-h-0">
          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
            <div class="p-3 border-b shrink-0 bg-indigo-50 flex items-center justify-between">
              <span class="text-sm font-black text-indigo-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 库位防错与物料扫码</span>
            </div>
            <div class="flex-1 p-4 flex flex-col overflow-y-auto custom-scrollbar">

              <div class="mb-5">
                <div class="text-xs font-bold text-slate-600 mb-2 flex items-center gap-1"><IconifyIcon icon="lucide:map-pin" class="text-orange-500"/> 步骤 1: 扫描锁定【{{ operationMode === 'ISSUE' ? '下架(出)来源' : '上架(入)目标' }}】库位</div>
                <div v-if="!isLocationLocked" class="flex gap-2">
                  <Input ref="locationInputRef" v-model:value="locationCode" placeholder="扫描库位条码" size="large" @pressEnter="handleLockLocation" class="font-mono border-orange-300"><template #prefix><IconifyIcon icon="lucide:barcode" class="text-orange-400" /></template></Input>
                  <Button type="primary" class="bg-orange-500 border-none font-bold" size="large" @click="handleLockLocation">锁定</Button>
                </div>
                <div v-else class="p-3 bg-green-50 border border-green-300 rounded-lg flex justify-between items-center shadow-inner">
                  <div class="flex flex-col"><span class="text-[10px] text-green-600 font-bold mb-1">当前锁定的关联库位</span><span class="font-mono font-black text-green-700 text-xl">{{ locationCode }}</span></div>
                  <Button type="link" danger size="small" @click="handleUnlockLocation">解锁更换</Button>
                </div>
              </div>

              <Divider class="my-2 border-slate-100" />

              <div :class="['p-4 rounded-lg border shadow-inner transition-all', isLocationLocked ? 'bg-indigo-50 border-indigo-200' : 'bg-slate-50 border-slate-200 opacity-50 pointer-events-none']">
                <div class="text-xs font-bold text-indigo-800 mb-4 flex items-center gap-1"><IconifyIcon icon="lucide:box" class="text-indigo-500"/> 步骤 2: 选择物料并扫码执行</div>
                <Form layout="vertical">
                  <Form.Item label="当前执行明细行 (Line Item)" class="font-bold text-slate-700 mb-4">
                    <Select v-model:value="activeLineId" size="large" class="w-full shadow-sm" :options="activeOrderInfo?.lines.map(l => ({ value: l.id, label: `${l.productCode} | ${l.productName} (待处理:${l.targetQty - l.processedQty})`, disabled: l.processedQty >= l.targetQty }))" />
                  </Form.Item>
                  <div class="mb-4">
                    <div class="text-xs font-bold text-slate-600 mb-2">本次条码包含数量</div>
                    <InputNumber v-model:value="issueQty" size="large" class="w-full font-bold shadow-sm" :min="1" />
                  </div>
                  <Form.Item class="mb-0 mt-2">
                    <div class="text-[10px] text-slate-500 mb-1">扫描物料标签 (自动校验BOM防错)</div>
                    <Input ref="itemInputRef" v-model:value="itemScanCode" size="large" class="font-mono text-xl border-indigo-400 py-3 shadow-sm" placeholder="扫描实物条码..." @pressEnter="handleScanItemEnter" :disabled="isOrderCompleted"><template #prefix><IconifyIcon icon="lucide:barcode" class="text-indigo-500 text-xl mr-2" /></template></Input>
                  </Form.Item>
                </Form>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> {{ operationMode === 'ISSUE' ? '发料防错校验流水' : '退库明细流水' }}</div><span class="text-[10px] text-slate-400">支持撤销解绑</span>
          </div>
          <div class="flex-1 flex flex-col">
            <Table :columns="itemColumns" :dataSource="scannedItems" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 230px)' }" class="vben-schema-table flex-1">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold text-slate-700">{{ record.barcode }}</span></template>
                <template v-if="column.dataIndex === 'productName'"><div class="text-[10px] text-slate-500 truncate" :title="record.productName">{{ record.productName }}</div></template>
                <template v-if="column.dataIndex === 'location'"><span class="font-mono text-orange-600 font-bold">{{ record.location }}</span></template>
                <template v-if="column.dataIndex === 'qty'"><span class="font-bold text-indigo-600">{{ record.qty }}</span> <span class="text-[10px] text-slate-400">{{ record.unit }}</span></template>
                <template v-if="column.dataIndex === 'action'"><Popconfirm title="确定撤销这笔明细吗？" @confirm="handleRemoveItem(record.id)"><Button type="text" danger size="small">撤销</Button></Popconfirm></template>
              </template>
              <template #emptyText>
                <div class="py-20 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:scan-face" class="text-5xl mb-3 opacity-30" /><span>锁定库位后扫码执行。</span></div>
              </template>
            </Table>
          </div>
        </div>
      </div>
    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4"><Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出工作台</Button></div>
      <div class="flex gap-3 items-center">
        <Button type="primary" class="bg-indigo-700 border-none font-bold px-10 shadow-lg" :disabled="!activeOrderInfo || scannedItems.length === 0" @click="handleConfirmTransaction">
          <IconifyIcon icon="lucide:database-zap" class="mr-2" /> {{ operationMode === 'ISSUE' ? '确认发料过账 (扣减库存)' : '确认退库过账 (增加库存)' }}
        </Button>
      </div>
    </footer>

    <Modal v-model:open="printModalVisible" :title="currentSlipData?.mode === '生产发料' ? '🖨️ 打印发料交接凭证' : '🖨️ 打印退料接收凭证'" :footer="null" width="550px" centered>
      <div class="py-4">
        <div class="bg-white border-2 border-slate-800 p-6 rounded-lg shadow-sm mx-auto w-full font-mono relative">
          <div class="absolute top-4 right-4 text-4xl opacity-10"><IconifyIcon icon="lucide:stamp" /></div>
          <div class="text-center text-lg font-black tracking-widest mb-4 border-b-2 border-slate-800 pb-2">
            【 禾臣新材料 - {{ currentSlipData?.mode }}凭证 】
          </div>

          <Descriptions size="small" :column="1" bordered :labelStyle="{ fontWeight: 'bold', width: '120px' }">
            <Descriptions.Item label="交易流水号"><span class="text-indigo-700 font-bold text-base">{{ currentSlipData?.slipNo }}</span></Descriptions.Item>
            <Descriptions.Item label="关联工单">{{ currentOrderNo }} {{ currentSlipData?.workOrder }}</Descriptions.Item>
            <Descriptions.Item label="目标车间">{{ currentSlipData?.targetStation }}</Descriptions.Item>
            <Descriptions.Item label="过账时间">{{ currentSlipData?.time }}</Descriptions.Item>
          </Descriptions>

          <div class="mt-4 border border-slate-300">
            <div class="bg-slate-100 font-bold text-xs p-2 border-b border-slate-300 flex justify-between">
              <span>物料明细记录</span><span>共 {{ currentSlipData?.itemCount }} 箱/件</span>
            </div>
            <div class="max-h-32 overflow-y-auto custom-scrollbar p-2 text-[10px]">
              <div v-for="(item, idx) in currentSlipData?.snapshotList" :key="idx" class="flex justify-between border-b border-dashed border-slate-200 py-1">
                <span>{{ item.barcode }}</span>
                <span class="font-bold">{{ item.qty }} {{ item.unit }}</span>
              </div>
            </div>
          </div>

          <div class="flex justify-between mt-6 pt-4 border-t-2 border-slate-800 text-xs font-bold">
            <span>仓管发货人：{{ currentSlipData?.user }}</span>
            <span>车间接收人签字：___________</span>
          </div>
        </div>

        <div class="flex gap-4 mt-6">
          <Button block size="large" @click="printModalVisible = false">跳过打印</Button>
          <Button block type="primary" size="large" class="bg-indigo-600 font-bold shadow-md" @click="printSlip">
            <IconifyIcon icon="lucide:printer" class="mr-2"/> 确认打印纸质交接单
          </Button>
        </div>
      </div>
    </Modal>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ 任务挂起队列" placement="left" width="400">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400"><IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>无挂起任务</span></div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-indigo-200 bg-indigo-50 shadow-sm hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-2"><div class="font-mono font-bold text-indigo-800 text-base">{{ task.orderInfo.orderNo }}</div><Tag color="orange" class="!m-0">已挂起</Tag></div>
          <Button type="primary" size="small" class="bg-indigo-600 w-full" @click="handleResumeTask(idx)">恢复任务</Button>
        </Card>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 过账流水与凭证" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <Input.Search v-model:value="historySearchForm.keyword" placeholder="搜索流水号/单号..." class="mb-3 shrink-0" allow-clear @search="historyPagination.current = 1" />
        <div class="flex-1 overflow-y-auto pr-2 flex flex-col gap-3">
          <Card v-for="log in filteredHistory" :key="log.id" size="small" class="border border-slate-200 cursor-pointer group hover:border-indigo-400 hover:shadow-md transition-all" @click="viewHistoryDetail(log)">
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-indigo-700 flex items-center">{{ log.slipNo }}</div>
              <Tag :color="log.mode === '生产发料' ? 'orange' : 'green'" class="!m-0">{{ log.mode }}</Tag>
            </div>
            <div class="text-xs text-slate-600 mb-1">源单据: <span class="font-bold">{{ log.orderNo }}</span></div>
            <div class="text-[10px] text-slate-400 mt-2 pt-2 border-t flex justify-between items-center">
              <span>{{ log.time }}</span>
              <span class="text-indigo-600 font-bold opacity-0 group-hover:opacity-100">查看交接单 <IconifyIcon icon="lucide:chevron-right" /></span>
            </div>
          </Card>
        </div>
        <Pagination v-model:current="historyPagination.current" :total="historyPagination.total" :pageSize="historyPagination.pageSize" size="small" class="mt-2 text-right" />
      </div>
    </Drawer>

    <Drawer v-model:open="historyDetailVisible" :title="`事务明细: ${currentHistoryDetail?.slipNo}`" placement="right" width="600">
      <div class="h-full flex flex-col">
        <div class="mb-3 flex justify-between items-center bg-indigo-50 p-2 rounded border border-indigo-100">
          <span class="text-xs font-bold text-indigo-800">业务凭证如需重打，请点击右侧按钮</span>
          <Button size="small" type="primary" class="bg-indigo-600" @click="printModalVisible = true; historyDetailVisible = false">
            <IconifyIcon icon="lucide:printer" class="mr-1"/> 补打交接凭证
          </Button>
        </div>
        <Table :columns="itemColumns.filter(c=>c.dataIndex!=='action')" :dataSource="currentHistoryDetail?.snapshotList" :pagination="false" size="small" class="vben-schema-table flex-1 overflow-y-auto" />
      </div>
    </Drawer>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2) !important; border-color: #4f46e5 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }
</style>
