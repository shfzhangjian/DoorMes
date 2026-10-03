<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Table, Switch, Radio, Select,
  message, Modal, Divider, Form, Row, Col, Progress, Drawer, Avatar, Badge, Pagination
} from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const clone = (data: any) => JSON.parse(JSON.stringify(data));

// ==================================================================================
// 1. 基础状态与动态时间
// ==================================================================================
const currentTime = ref(new Date().toLocaleString());
let timer: any = null;

onMounted(() => { timer = setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000); });
onUnmounted(() => clearInterval(timer));

const currentUser = ref({
  userName: '赵收货', userId: 'W8022', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Zhao', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 业务模式、主从表上下文与【挂起队列】
// ==================================================================================
const inboundMode = ref<'PURCHASE' | 'PRODUCTION'>('PURCHASE');
const orderScanCode = ref('');

const activeOrderInfo = ref<any>(null);
const activeLineId = ref<string | null>(null);

const locationInputRef = ref();
const itemInputRef = ref();
const locationCode = ref('');
const isLocationLocked = ref(false);

// 收货与拆包相关状态
const itemScanCode = ref('');
const isSplitMode = ref(false); // 💡 是否开启拆包模式
const receiveQty = ref<number>(100);
const splitTotalQty = ref<number>(500); // 拆包总数
const splitCount = ref<number>(5);      // 拆分包数
const printInternalLabel = ref(true);

const scannedItems = ref<any[]>([]);
const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

function handleModeChange() {
  if (scannedItems.value.length > 0) {
    message.warning('当前有未过账的收货数据，请先挂起或过账后再切换模式！');
    setTimeout(() => { inboundMode.value = inboundMode.value === 'PURCHASE' ? 'PRODUCTION' : 'PURCHASE'; }, 10);
    return;
  }
  activeOrderInfo.value = null;
  orderScanCode.value = '';
}

function handleFetchOrder() {
  if (!orderScanCode.value) return message.warning('请输入单号');
  message.loading({ content: '拉取入库指令与物料明细...', key: 'inbound', duration: 0.5 }).then(() => {
    if (inboundMode.value === 'PURCHASE') {
      activeOrderInfo.value = {
        orderNo: 'ASN-' + orderScanCode.value.toUpperCase(),
        source: '宁波某某新材料供应商',
        needIqc: true,
        lines: [
          { id: 'L01', productCode: 'RM-A01', productName: 'PET光学基膜 (主材)', targetQty: 2000, receivedQty: 0, unit: 'KG' },
          { id: 'L02', productCode: 'RM-B02', productName: '离型膜 (高透)', targetQty: 1000, receivedQty: 0, unit: 'KG' }
        ]
      };
    } else {
      activeOrderInfo.value = {
        orderNo: 'WO-RET-' + orderScanCode.value.toUpperCase(),
        source: '压槽工序 (车间产线)',
        needIqc: false,
        lines: [
          { id: 'L01', productCode: 'SFG-T01', productName: 'T01_Film_压槽单片(A款)', targetQty: 1000, receivedQty: 0, unit: 'PCS' }
        ]
      };
    }
    activeLineId.value = activeOrderInfo.value.lines[0].id;
    message.success({ content: `锁定入库单: ${activeOrderInfo.value.orderNo}`, key: 'inbound' });
    focusLocationScanner();
  });
}

function handleSuspendTask() {
  if (!activeOrderInfo.value) return;
  suspendedTasks.value.push({
    id: Date.now(),
    mode: inboundMode.value,
    orderInfo: clone(activeOrderInfo.value),
    activeLineId: activeLineId.value,
    scannedItems: clone(scannedItems.value),
    locationCode: locationCode.value,
    isLocationLocked: isLocationLocked.value,
    suspendTime: new Date().toLocaleTimeString()
  });
  activeOrderInfo.value = null; scannedItems.value = []; locationCode.value = ''; isLocationLocked.value = false; orderScanCode.value = '';
  message.success('当前收货任务已挂起！');
}

function handleResumeTask(index: number) {
  if (activeOrderInfo.value) return message.warning('当前已有进行中的任务，请先挂起！');
  const task = suspendedTasks.value[index];
  inboundMode.value = task.mode; activeOrderInfo.value = clone(task.orderInfo); activeLineId.value = task.activeLineId;
  scannedItems.value = clone(task.scannedItems); locationCode.value = task.locationCode; isLocationLocked.value = task.isLocationLocked;
  suspendedTasks.value.splice(index, 1); suspendedDrawerVisible.value = false;
  message.success(`任务 ${activeOrderInfo.value.orderNo} 已恢复！`);
  focusItemScanner();
}

function handleClearOrder() {
  if (scannedItems.value.length > 0) {
    return Modal.confirm({
      title: '操作拦截', content: '当前有未过账的数据，强制清空将丢失。建议使用【挂起任务】。',
      okText: '挂起任务', cancelText: '强制清空', onOk: handleSuspendTask,
      onCancel: () => { activeOrderInfo.value = null; scannedItems.value = []; }
    });
  }
  activeOrderInfo.value = null; orderScanCode.value = '';
}

const isOrderCompleted = computed(() => {
  if (!activeOrderInfo.value) return false;
  return activeOrderInfo.value.lines.every((line: any) => line.receivedQty >= line.targetQty);
});

// ==================================================================================
// 3. 库位绑定与扫码收货 (含拆包逻辑)
// ==================================================================================
const itemColumns: TableColumnsType = [
  { title: '物料/批次条码', dataIndex: 'barcode', width: 170 },
  { title: '物料名称', dataIndex: 'productName', width: 140 },
  { title: '上架库位', dataIndex: 'location', width: 100 },
  { title: '数量', dataIndex: 'qty', width: 80, align: 'right' },
  { title: '状态', dataIndex: 'iqcStatus', width: 80, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleLockLocation() {
  if (!locationCode.value.trim()) return message.warning('请扫描货架/库位条码');
  isLocationLocked.value = true;
  message.success(`已锁定上架库位: ${locationCode.value}`);
  focusItemScanner();
}

function handleUnlockLocation() {
  isLocationLocked.value = false; locationCode.value = ''; focusLocationScanner();
}

// 常规收货 (整包或车间成品单件)
function handleScanItemEnter() {
  if (!activeOrderInfo.value) { itemScanCode.value = ''; return message.warning('请锁定入库单据！'); }
  if (!isLocationLocked.value) { itemScanCode.value = ''; return message.error('请先锁定目标库位！'); }
  if (!activeLineId.value) { itemScanCode.value = ''; return message.error('请选择当前接收的物料行！'); }

  const currentLine = activeOrderInfo.value.lines.find((l: any) => l.id === activeLineId.value);
  let code = itemScanCode.value.trim();

  if (inboundMode.value === 'PURCHASE' && !code) {
    code = `RM-${currentLine.productCode.split('-')[1]}-${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`;
  }
  if (!code) return message.warning('请输入物料条码');

  if (scannedItems.value.some(b => b.barcode === code)) {
    itemScanCode.value = ''; return message.error(`条码已被扫描，严禁重复！`);
  }

  const currentQty = receiveQty.value || 1;
  if (currentLine.receivedQty + currentQty > currentLine.targetQty) {
    itemScanCode.value = ''; return message.error(`超收预警：当前物料加入后将超出计划数量！`);
  }

  executeReceive(code, currentQty, currentLine);

  itemScanCode.value = '';
  checkLineCompletion(currentLine);
}

// 💡 批量拆包收货逻辑
const splitUnitQty = computed(() => {
  if (!splitTotalQty.value || !splitCount.value) return 0;
  return Number((splitTotalQty.value / splitCount.value).toFixed(2));
});

function handleSplitReceive() {
  if (!activeOrderInfo.value || !isLocationLocked.value || !activeLineId.value) {
    return message.error('请确保单据已锁定、库位已锁定、物料行已选择！');
  }
  const currentLine = activeOrderInfo.value.lines.find((l: any) => l.id === activeLineId.value);

  if (currentLine.receivedQty + splitTotalQty.value > currentLine.targetQty) {
    return message.error(`超收预警：拆包总数加入后将超出该物料的计划数量！`);
  }

  // 批量生成条码并入账
  for (let i = 0; i < splitCount.value; i++) {
    const code = `RM-${currentLine.productCode.split('-')[1]}-S${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`;
    executeReceive(code, splitUnitQty.value, currentLine);
  }

  message.success(`成功将 ${splitTotalQty.value} ${currentLine.unit} 拆分为 ${splitCount.value} 包，已生成对应的批次条码！`);
  checkLineCompletion(currentLine);
}

// 底层写台账与扣减逻辑
function executeReceive(code: string, qty: number, line: any) {
  scannedItems.value.unshift({
    id: Date.now() + Math.random(),
    barcode: code,
    productId: line.productCode,
    productName: line.productName,
    location: locationCode.value,
    qty: qty,
    unit: line.unit,
    iqcStatus: activeOrderInfo.value.needIqc ? '待检(冻结)' : '免检(可用)',
    time: new Date().toLocaleTimeString()
  });
  line.receivedQty += qty;
}

function checkLineCompletion(line: any) {
  if (isOrderCompleted.value) {
    Modal.success({ title: '整单收货完成', content: '单据内所有物料均已收满，请执行入库过账。' });
  } else if (line.receivedQty >= line.targetQty) {
    message.success(`物料【${line.productName}】已收满，请在左侧切换其他物料！`);
  } else {
    focusItemScanner();
  }
}

function handleRemoveItem(id: number) {
  const item = scannedItems.value.find(b => b.id === id);
  if (item) {
    scannedItems.value = scannedItems.value.filter(b => b.id !== id);
    const line = activeOrderInfo.value.lines.find((l: any) => l.productCode === item.productId);
    if (line) line.receivedQty -= item.qty;
    focusItemScanner();
  }
}

function focusLocationScanner() { nextTick(() => { locationInputRef.value?.focus(); }); }
function focusItemScanner() { nextTick(() => { itemInputRef.value?.focus(); }); }

// ==================================================================================
// 4. 收货过账与高级历史记录 (强化详情查看与条码补打)
// ==================================================================================
const historyDrawerVisible = ref(false);
const inboundHistoryLogs = ref<any[]>([]);
const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });

const historyDetailVisible = ref(false);
const currentHistoryDetail = ref<any>(null);

function handleConfirmInbound() {
  if (scannedItems.value.length === 0) return message.warning('收货明细为空！');

  let totalReceived = 0;
  activeOrderInfo.value.lines.forEach((l: any) => totalReceived += l.receivedQty);

  Modal.confirm({
    title: '确认执行入库过账？',
    content: `即将写入 ${scannedItems.value.length} 笔明细至 WMS 库存台账。${activeOrderInfo.value.needIqc ? '\n⚠️ 包含待检物料，将自动触发 IQC 报检。' : ''}`,
    onOk: () => {
      inboundHistoryLogs.value.unshift({
        id: Date.now(),
        orderNo: activeOrderInfo.value.orderNo,
        mode: inboundMode.value === 'PURCHASE' ? '采购收货' : '完工入库',
        source: activeOrderInfo.value.source,
        totalQty: totalReceived,
        itemCount: scannedItems.value.length,
        time: new Date().toLocaleString(),
        user: currentUser.value.userName,
        snapshotList: clone(scannedItems.value)
      });

      message.success('入库过账成功！');
      scannedItems.value = [];
      activeOrderInfo.value = null;
      orderScanCode.value = '';
      handleUnlockLocation();
    }
  });
}

const filteredHistory = computed(() => {
  let filtered = inboundHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.orderNo.toLowerCase().includes(kw) || log.source.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});

// 打开明细并传递快照数据
function viewHistoryDetail(log: any) {
  currentHistoryDetail.value = log;
  historyDetailVisible.value = true;
}

function handleReprintBarcode(barcode: string) {
  message.success(`条码 [${barcode}] 补打指令已下发至默认打印机！`);
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-blue-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:download-cloud" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 入库收货工作台</span>
      </div>
      <div class="flex items-center gap-5 shrink-0">
        <Radio.Group v-model:value="inboundMode" button-style="solid" @change="handleModeChange" class="mr-4">
          <Radio.Button value="PURCHASE"><IconifyIcon icon="lucide:truck" class="mr-1 inline-block"/> 采购收货</Radio.Button>
          <Radio.Button value="PRODUCTION"><IconifyIcon icon="lucide:factory" class="mr-1 inline-block"/> 完工入库</Radio.Button>
        </Radio.Group>
        <Badge :count="suspendedTasks.length" :offset="[-5, 5]">
          <Button type="primary" ghost class="font-bold border-blue-200" @click="suspendedDrawerVisible = true"><IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列</Button>
        </Badge>
        <Button type="dashed" class="border-blue-200 text-blue-600 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;"><IconifyIcon icon="lucide:history" class="mr-1" /> 收货历史</Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-blue-100" />
            <div class="flex flex-col"><span class="text-xs font-bold leading-none">{{ currentUser.userName }}</span></div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">

      <div class="shrink-0 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden flex flex-col transition-all">
        <div class="bg-slate-50 border-b border-slate-100 p-2 px-4 flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-2"><IconifyIcon icon="lucide:clipboard-list" /> 当前入库单据指令</span>
          <div class="flex gap-2" v-if="activeOrderInfo">
            <Button type="primary" class="bg-orange-500 border-none shadow-sm font-bold text-xs h-7" @click="handleSuspendTask"><IconifyIcon icon="lucide:pause-circle" class="mr-1" /> 临时挂起</Button>
            <Button type="default" danger size="small" class="h-7" @click="handleClearOrder">清空单据</Button>
          </div>
        </div>

        <div class="p-3 flex items-start gap-6">
          <div v-if="!activeOrderInfo" class="flex-1 flex items-center gap-4 py-2">
            <Input.Search v-model:value="orderScanCode" :placeholder="inboundMode === 'PURCHASE' ? '扫描采购送货单(ASN)...' : '扫描车间完工交接单...'" enter-button="锁定任务" size="large" @search="handleFetchOrder" class="max-w-md shadow-sm" />
          </div>
          <div v-else class="flex-1 flex w-full gap-4">
            <div class="w-64 shrink-0 flex flex-col gap-3 pr-4 border-r border-slate-100">
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">入库关联单号</span><span class="font-mono font-black text-blue-700 text-base">{{ activeOrderInfo.orderNo }}</span></div>
              <div class="flex flex-col"><span class="text-[10px] text-slate-400 font-bold mb-1">{{ inboundMode === 'PURCHASE' ? '供应商信息' : '来源工序' }}</span><span class="font-bold text-slate-800 text-xs">{{ activeOrderInfo.source }}</span></div>
            </div>

            <div class="flex-1 grid grid-cols-2 md:grid-cols-3 gap-3 overflow-y-auto max-h-24 custom-scrollbar pr-2">
              <div v-for="line in activeOrderInfo.lines" :key="line.id"
                   :class="['p-2 rounded border text-xs relative overflow-hidden transition-colors cursor-pointer', line.receivedQty >= line.targetQty ? 'bg-green-50 border-green-200' : (activeLineId === line.id ? 'bg-blue-50 border-blue-400 shadow-sm' : 'bg-slate-50 border-slate-200 hover:border-blue-300')]"
                   @click="activeLineId = line.id">
                <div v-if="activeLineId === line.id && line.receivedQty < line.targetQty" class="absolute top-0 right-0 bg-blue-500 text-white text-[9px] px-1 rounded-bl">正在接收</div>
                <div class="font-bold text-slate-800 truncate mb-1" :title="line.productName">{{ line.productName }}</div>
                <div class="flex justify-between items-center font-mono text-slate-500 mb-1">
                  <span class="text-[10px]">{{ line.productCode }}</span>
                  <span :class="line.receivedQty >= line.targetQty ? 'text-green-600 font-bold' : 'text-blue-600 font-bold'">{{ line.receivedQty }} / {{ line.targetQty }}</span>
                </div>
                <Progress :percent="Number(((line.receivedQty / line.targetQty) * 100).toFixed(1))" :strokeColor="line.receivedQty >= line.targetQty ? '#16a34a' : '#3b82f6'" size="small" :showInfo="false" class="m-0" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex gap-3 min-h-0 relative">
        <div v-if="!activeOrderInfo" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 rounded-lg border border-slate-200">
          <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-blue-500" />
          <span class="font-bold text-lg text-slate-700">请先在上方锁定入库源单据</span>
        </div>

        <div class="w-[420px] flex flex-col gap-3 min-h-0">
          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
            <div class="p-3 border-b shrink-0 bg-blue-50 flex items-center justify-between">
              <span class="text-sm font-black text-blue-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 上架库位与物料扫码</span>
            </div>
            <div class="flex-1 p-4 flex flex-col overflow-y-auto custom-scrollbar">

              <div class="mb-5">
                <div class="text-xs font-bold text-slate-600 mb-2 flex items-center gap-1"><IconifyIcon icon="lucide:map-pin" class="text-orange-500"/> 步骤 1: 扫描锁定目标库位</div>
                <div v-if="!isLocationLocked" class="flex gap-2">
                  <Input ref="locationInputRef" v-model:value="locationCode" placeholder="扫描库位条码" size="large" @pressEnter="handleLockLocation" class="font-mono border-orange-300">
                    <template #prefix><IconifyIcon icon="lucide:barcode" class="text-orange-400" /></template>
                  </Input>
                  <Button type="primary" class="bg-orange-500 border-none font-bold" size="large" @click="handleLockLocation">锁定</Button>
                </div>
                <div v-else class="p-3 bg-green-50 border border-green-300 rounded-lg flex justify-between items-center shadow-inner">
                  <div class="flex flex-col"><span class="text-[10px] text-green-600 font-bold mb-1">当前指导上架库位</span><span class="font-mono font-black text-green-700 text-xl">{{ locationCode }}</span></div>
                  <Button type="link" danger size="small" @click="handleUnlockLocation">解锁更换</Button>
                </div>
              </div>

              <Divider class="my-2 border-slate-100" />

              <div :class="['p-4 rounded-lg border shadow-inner transition-all', isLocationLocked ? 'bg-blue-50 border-blue-200' : 'bg-slate-50 border-slate-200 opacity-50 pointer-events-none']">

                <div class="flex justify-between items-center mb-4">
                  <span class="text-xs font-bold text-blue-800 flex items-center gap-1"><IconifyIcon icon="lucide:box" class="text-blue-500"/> 步骤 2: 确认收货与打码</span>
                  <Radio.Group v-if="inboundMode === 'PURCHASE'" v-model:value="isSplitMode" size="small" button-style="solid">
                    <Radio.Button :value="false">单件收货</Radio.Button>
                    <Radio.Button :value="true">来料拆包</Radio.Button>
                  </Radio.Group>
                </div>

                <Form layout="vertical">
                  <template v-if="!isSplitMode">
                    <div v-if="inboundMode === 'PURCHASE'" class="mb-4">
                      <div class="flex items-center justify-between mb-2">
                        <span class="text-xs font-bold text-slate-600">单托/单箱接收数量</span>
                        <Switch v-model:checked="printInternalLabel" checked-children="自动打码" un-checked-children="不打码" size="small" />
                      </div>
                      <InputNumber v-model:value="receiveQty" size="large" class="w-full font-bold shadow-sm" :min="1" />
                    </div>
                    <Form.Item class="mb-0 mt-2">
                      <div class="text-[10px] text-slate-500 mb-1">扫描物料/箱条码 (自动回车确认接收)</div>
                      <Input ref="itemInputRef" v-model:value="itemScanCode" size="large" class="font-mono text-xl border-blue-400 py-3 shadow-sm" :placeholder="inboundMode === 'PURCHASE' ? '无码可回车自动生成...' : '扫描实物标签...'" @pressEnter="handleScanItemEnter" :disabled="isOrderCompleted">
                        <template #prefix><IconifyIcon icon="lucide:barcode" class="text-blue-500 text-xl mr-2" /></template>
                      </Input>
                    </Form.Item>
                  </template>

                  <template v-else>
                    <div class="p-3 bg-white border border-blue-100 rounded-lg mb-3 shadow-sm">
                      <Row :gutter="12" class="mb-3">
                        <Col :span="12">
                          <div class="text-xs text-slate-500 mb-1">接收总数</div>
                          <InputNumber v-model:value="splitTotalQty" size="large" class="w-full" :min="1" />
                        </Col>
                        <Col :span="12">
                          <div class="text-xs text-slate-500 mb-1">拆分包数 (生成条码数)</div>
                          <InputNumber v-model:value="splitCount" size="large" class="w-full" :min="2" />
                        </Col>
                      </Row>
                      <div class="flex justify-between items-center px-2 py-1 bg-slate-50 rounded border border-slate-100">
                        <span class="text-xs text-slate-500">每包均分数量：</span>
                        <span class="font-bold text-blue-600">{{ splitUnitQty }}</span>
                      </div>
                    </div>
                    <Button type="primary" size="large" block class="bg-blue-600 font-bold shadow-md hover:bg-blue-500" @click="handleSplitReceive" :disabled="isOrderCompleted">
                      <IconifyIcon icon="lucide:printer" class="mr-2"/> 生成拆包批次条码并接收
                    </Button>
                  </template>
                </Form>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> 入库清点明细台账</div><span class="text-[10px] text-slate-400">支持撤销解绑</span>
          </div>
          <div class="flex-1 flex flex-col">
            <Table :columns="itemColumns" :dataSource="scannedItems" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 230px)' }" class="vben-schema-table flex-1">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold text-slate-700">{{ record.barcode }}</span></template>
                <template v-if="column.dataIndex === 'productName'"><div class="text-[10px] text-slate-500 truncate" :title="record.productName">{{ record.productName }}</div></template>
                <template v-if="column.dataIndex === 'location'"><span class="font-mono text-orange-600 font-bold">{{ record.location }}</span></template>
                <template v-if="column.dataIndex === 'qty'"><span class="font-bold">{{ record.qty }}</span> <span class="text-[10px] text-slate-400">{{ record.unit }}</span></template>
                <template v-if="column.dataIndex === 'iqcStatus'"><Tag :color="record.iqcStatus === '免检(可用)' ? 'green' : 'orange'">{{ record.iqcStatus }}</Tag></template>
                <template v-if="column.dataIndex === 'action'"><Popconfirm title="确定撤销这笔明细吗？" @confirm="handleRemoveItem(record.id)"><Button type="text" danger size="small">撤销</Button></Popconfirm></template>
              </template>
            </Table>
          </div>
        </div>
      </div>
    </main>

    <footer class="flex h-14 shrink-0 items-center justify-between border-t bg-white px-4 shadow-[0_-2px_8px_rgba(0,0,0,0.05)] z-20">
      <div class="flex items-center gap-4"><Button @click="emit('close')" class="font-bold text-slate-600 border-slate-300">退出入库台</Button></div>
      <div class="flex gap-3 items-center">
        <Button type="primary" class="bg-blue-700 border-none font-bold px-10 shadow-lg" :disabled="!activeOrderInfo || scannedItems.length === 0" @click="handleConfirmInbound">
          <IconifyIcon icon="lucide:database-zap" class="mr-2" /> 正式入库过账 (写库存台账)
        </Button>
      </div>
    </footer>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ 入库挂起队列" placement="left" width="400">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400"><IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>暂无挂起任务</span></div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-blue-200 bg-blue-50 shadow-sm hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-2"><div class="font-mono font-bold text-blue-800 text-base">{{ task.orderInfo.orderNo }}</div><Tag color="orange" class="!m-0">已挂起</Tag></div>
          <Button type="primary" size="small" class="bg-blue-600 w-full" @click="handleResumeTask(idx)">恢复任务</Button>
        </Card>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 收货历史与快照追溯" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <Input.Search v-model:value="historySearchForm.keyword" placeholder="搜索单号..." class="mb-3 shrink-0" allow-clear @search="historyPagination.current = 1" />
        <div class="flex-1 overflow-y-auto pr-2 flex flex-col gap-3">
          <Card v-for="log in filteredHistory" :key="log.id" size="small" class="border border-slate-200 cursor-pointer group hover:border-blue-400 hover:shadow-md transition-all" @click="viewHistoryDetail(log)">
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-blue-700 flex items-center"><IconifyIcon icon="lucide:file-down" class="mr-1"/> {{ log.orderNo }}</div>
              <Tag color="green" class="!m-0">已入库</Tag>
            </div>
            <div class="text-xs text-slate-600 mb-1">包含明细: <span class="font-bold text-blue-600">{{ log.itemCount }}</span> 笔</div>
            <div class="flex justify-between items-center pt-2 border-t border-slate-100 mt-2">
              <span class="text-[10px] text-slate-400">{{ log.time }}</span>
              <span class="text-xs text-blue-600 font-bold opacity-0 group-hover:opacity-100 flex items-center">查看明细与补打 <IconifyIcon icon="lucide:chevron-right" /></span>
            </div>
          </Card>
        </div>
        <Pagination v-model:current="historyPagination.current" :total="historyPagination.total" :pageSize="historyPagination.pageSize" size="small" class="mt-2 text-right" />
      </div>
    </Drawer>

    <Drawer v-model:open="historyDetailVisible" :title="`收货明细下钻: ${currentHistoryDetail?.orderNo}`" placement="right" width="600">
      <div class="h-full flex flex-col">
        <div class="mb-3 text-xs text-slate-500 bg-blue-50 p-2 rounded border border-blue-100">
          提示：若现场条码标签污损或遗失，可在下方列表中找到对应条码执行【补打标签】。
        </div>
        <Table :columns="[...itemColumns.filter(c=>c.dataIndex!=='action'), {title:'补打', dataIndex:'reprint', width:70, align:'center'}]" :dataSource="currentHistoryDetail?.snapshotList" :pagination="false" size="small" class="vben-schema-table flex-1 overflow-y-auto">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold">{{ record.barcode }}</span></template>
            <template v-if="column.dataIndex === 'location'"><span class="font-mono text-orange-600">{{ record.location }}</span></template>
            <template v-if="column.dataIndex === 'iqcStatus'"><Tag :color="record.iqcStatus.includes('免检') ? 'green' : 'orange'">{{ record.iqcStatus }}</Tag></template>
            <template v-if="column.dataIndex === 'reprint'">
              <Button type="primary" ghost size="small" class="text-[10px] h-6 px-2" @click="handleReprintBarcode(record.barcode)">补打</Button>
            </template>
          </template>
        </Table>
      </div>
    </Drawer>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2) !important; border-color: #3b82f6 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }
</style>
