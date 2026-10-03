<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, InputNumber, Tree,
  message, Modal, Select, Divider, Form, Row, Col, Statistic, Popconfirm, Progress, Drawer, Avatar, Badge, Pagination
} from 'ant-design-vue';
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
  userName: '李包装', userId: 'U2055', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 包装任务上下文 & 多任务挂起机制
// ==================================================================================
const orderScanCode = ref('');
const activeOrderInfo = ref<any>(null);

const packMode = ref<'INNER' | 'OUTER'>('INNER');
const capacityRule = ref(2);
const currentParentBarcode = ref('');
const scanInputRef = ref();
const scanCode = ref('');
const packTreeData = ref<any[]>([]);

const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

function handleFetchOrder() {
  if (!orderScanCode.value) return message.warning('请输入或扫描包装指令单/工单号');

  message.loading({ content: '解析包装任务中...', key: 'order', duration: 0.5 }).then(() => {
    activeOrderInfo.value = {
      orderNo: orderScanCode.value.toUpperCase(),
      customer: '宁德时代 (CATL)',
      productCode: 'MAT-T01-009',
      productName: 'T01_Film_背胶单片 (高透型)',
      spec: '125mm x 98mm / 0.2mm厚',
      targetQty: 10,
      packedQty: 0
    };
    message.success({ content: `成功锁定包装任务: ${activeOrderInfo.value.orderNo}`, key: 'order' });
  });
}

function handleSuspendTask() {
  if (!activeOrderInfo.value) return;
  suspendedTasks.value.push({
    id: Date.now(),
    orderInfo: clone(activeOrderInfo.value),
    treeData: clone(packTreeData.value),
    mode: packMode.value,
    capacity: capacityRule.value,
    parentBarcode: currentParentBarcode.value,
    suspendTime: new Date().toLocaleTimeString()
  });

  activeOrderInfo.value = null;
  packTreeData.value = [];
  currentParentBarcode.value = '';
  orderScanCode.value = '';
  message.success('当前包装任务已成功挂起，工作台已清空备用！');
}

function handleResumeTask(index: number) {
  if (activeOrderInfo.value) return message.warning('当前已有进行中的任务，请先挂起或完结！');
  const task = suspendedTasks.value[index];
  activeOrderInfo.value = clone(task.orderInfo);
  packTreeData.value = clone(task.treeData);
  packMode.value = task.mode;
  capacityRule.value = task.capacity;
  currentParentBarcode.value = task.parentBarcode;
  suspendedTasks.value.splice(index, 1);
  suspendedDrawerVisible.value = false;
  message.success(`任务 ${activeOrderInfo.value.orderNo} 已恢复！`);
  focusScanner();
}

function handleClearOrder() {
  if (packTreeData.value.length > 0) {
    return Modal.confirm({
      title: '防错拦截：当前有未完结数据',
      content: '强制清空将丢失当前未封箱的包装数据！请使用【挂起任务】功能。',
      okText: '挂起任务',
      cancelText: '强制清空',
      onOk: handleSuspendTask,
      onCancel: () => {
        activeOrderInfo.value = null;
        packTreeData.value = [];
        currentParentBarcode.value = '';
        message.info('任务已强制清空');
      }
    });
  }
  activeOrderInfo.value = null;
  orderScanCode.value = '';
}

const packProgress = computed(() => {
  if (!activeOrderInfo.value) return 0;
  return Number(((activeOrderInfo.value.packedQty / activeOrderInfo.value.targetQty) * 100).toFixed(1));
});

// ==================================================================================
// 3. 核心执行与封箱逻辑
// ==================================================================================
function handleCreateNewBox() {
  if (!activeOrderInfo.value) return message.warning('请先锁定包装任务！');
  const prefix = packMode.value === 'INNER' ? 'IN-' : 'BOX-';
  const seq = String(Math.floor(Math.random() * 10000)).padStart(4, '0');
  const newBarcode = `${prefix}20260219-${seq}`;
  currentParentBarcode.value = newBarcode;

  packTreeData.value.unshift({ title: `${newBarcode} (${packMode.value === 'INNER' ? '内包装' : '外箱'})`, key: newBarcode, type: packMode.value, capacity: capacityRule.value, children: [] });
  message.success(`已开启新箱：${newBarcode}`);
  focusScanner();
}

function handleScanEnter() {
  const code = scanCode.value.trim();
  if (!code) return;
  if (!activeOrderInfo.value) { scanCode.value = ''; return message.warning('请先锁定订单上下文！'); }
  if (!currentParentBarcode.value) { scanCode.value = ''; return message.warning('请先开启新包装！'); }

  const currentBox = packTreeData.value.find(node => node.key === currentParentBarcode.value);
  if (!currentBox) return;

  if (currentBox.children.length >= currentBox.capacity) { scanCode.value = ''; return message.error(`当前包装已满，请开启新箱！`); }
  if (checkDuplicateBarcode(packTreeData.value, code)) { scanCode.value = ''; return message.error(`条码 [${code}] 已被装箱，严禁重复扫描！`); }

  currentBox.children.push({ title: code, key: code, type: packMode.value === 'INNER' ? 'PIECE' : 'INNER', isLeaf: true, scannedAt: new Date().toLocaleTimeString() });
  if (packMode.value === 'INNER') activeOrderInfo.value.packedQty += 1;
  message.success(`条码 ${code} 成功装入 ${currentBox.key}`);
  scanCode.value = '';

  if (currentBox.children.length === currentBox.capacity) handleSealBox(currentBox);
  else focusScanner();
}

function checkDuplicateBarcode(tree: any[], targetCode: string): boolean {
  for (const node of tree) {
    if (node.key === targetCode) return true;
    if (node.children && node.children.length > 0) {
      if (checkDuplicateBarcode(node.children, targetCode)) return true;
    }
  }
  return false;
}

// ==================================================================================
// 4. 包装历史查询与分页 (全新升级)
// ==================================================================================
const historyDrawerVisible = ref(false);
const packHistoryLogs = ref<any[]>([]); // 原始历史库

const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });

// 历史下钻详情状态
const historyDetailVisible = ref(false);
const currentHistoryDetail = ref<any>(null);

function handleSealBox(boxNode: any) {
  // 💡 封箱时，保存一整个箱子的快照结构 (TreeData)
  packHistoryLogs.value.unshift({
    id: Date.now(),
    boxCode: boxNode.key,
    orderNo: activeOrderInfo.value.orderNo,
    qty: boxNode.children.length,
    time: new Date().toLocaleString(),
    user: currentUser.value.userName,
    snapshotTree: [clone(boxNode)] // 封存只读结构
  });

  Modal.success({
    title: '✅ 满箱自动封箱与打印',
    content: `目标箱号: ${boxNode.key}\n装箱数量: ${boxNode.children.length} PCS\n\n系统已记录包装台账，并向斑马打印机发送流转标签打印指令。`,
    okText: '继续开启下一箱',
    onOk: () => { currentParentBarcode.value = ''; handleCreateNewBox(); }
  });
}

// 计算过滤与分页结果
const filteredHistory = computed(() => {
  let filtered = packHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.boxCode.toLowerCase().includes(kw) || log.orderNo.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});

// 查看只读历史详情
function viewHistoryDetail(log: any) {
  currentHistoryDetail.value = log;
  historyDetailVisible.value = true;
}

function handleRemoveItem(boxKey: string, itemKey: string) {
  const box = packTreeData.value.find(node => node.key === boxKey);
  if (box) {
    box.children = box.children.filter((child: any) => child.key !== itemKey);
    if (packMode.value === 'INNER') activeOrderInfo.value.packedQty -= 1;
    message.success(`已将 ${itemKey} 从包装中剔除`);
    focusScanner();
  }
}

function focusScanner() { nextTick(() => { scanInputRef.value?.focus(); }); }

const stats = computed(() => {
  let innerCount = 0; let pieceCount = 0;
  packTreeData.value.forEach(node => {
    if (node.type === 'INNER') { innerCount++; pieceCount += node.children.length; }
    else if (node.type === 'OUTER') { innerCount += node.children.length; }
  });
  return { innerCount, pieceCount, totalBoxes: packTreeData.value.length };
});
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-indigo-600 text-white p-1.5 rounded-lg flex items-center justify-center shadow-inner">
          <IconifyIcon icon="lucide:package-search" class="text-xl" />
        </div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 包装装箱工作台</span>
      </div>

      <div class="flex items-center gap-5 shrink-0">
        <Badge :count="suspendedTasks.length" :offset="[-5, 5]">
          <Button type="primary" ghost class="font-bold border-indigo-200" @click="suspendedDrawerVisible = true">
            <IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列
          </Button>
        </Badge>
        <Button type="dashed" class="border-slate-300 text-slate-600 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;">
          <IconifyIcon icon="lucide:history" class="mr-1" /> 包装历史
        </Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="text-right leading-tight hidden md:block">
          <div class="text-xs font-mono font-bold text-slate-800">{{ currentTime.split(' ')[1] }}</div>
          <div class="text-slate-400 text-[9px]">{{ currentTime.split(' ')[0] }}</div>
        </div>
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-indigo-100 shadow-sm" />
            <div class="flex flex-col">
              <span class="text-xs font-bold leading-none text-slate-800">{{ currentUser.userName }}</span>
              <span class="text-indigo-600 text-[9px] mt-1 font-bold">在线: {{ currentUser.loginTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex flex-col min-h-0 p-3 overflow-hidden gap-3">
      <div class="shrink-0 bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden flex flex-col transition-all">
        <div class="bg-slate-50 border-b border-slate-100 p-2 px-4 flex justify-between items-center">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-2"><IconifyIcon icon="lucide:clipboard-check" /> 当前包装作业指令</span>
          <div class="flex gap-2" v-if="activeOrderInfo">
            <Button type="primary" class="bg-orange-500 border-none shadow-sm shadow-orange-200 font-bold text-xs h-7" @click="handleSuspendTask">
              <IconifyIcon icon="lucide:pause-circle" class="mr-1" /> 临时挂起任务
            </Button>
            <Button type="default" danger size="small" class="h-7" @click="handleClearOrder">完结/清空</Button>
          </div>
        </div>
        <div class="p-4 flex items-center gap-6">
          <div v-if="!activeOrderInfo" class="flex-1 flex items-center gap-4">
            <Input.Search v-model:value="orderScanCode" placeholder="扫描/输入 包装指令单或工单号..." enter-button="锁定任务" size="large" @search="handleFetchOrder" class="max-w-md shadow-sm" />
            <span class="text-xs text-slate-400">※ 请锁定单据获取产品要求。若需继续之前的任务，请点击右上角【挂起队列】恢复。</span>
          </div>
          <div v-else class="flex-1 flex items-center justify-between animate-fade-in w-full">
            <div class="flex gap-8 items-center">
              <div class="flex flex-col">
                <span class="text-[10px] text-slate-400 font-bold mb-1">关联单据 / 客户</span>
                <span class="font-mono font-black text-indigo-700 text-lg">{{ activeOrderInfo.orderNo }} <span class="text-xs text-slate-500 font-normal ml-2">({{ activeOrderInfo.customer }})</span></span>
              </div>
              <Divider type="vertical" class="h-10 bg-slate-200" />
              <div class="flex flex-col">
                <span class="text-[10px] text-slate-400 font-bold mb-1">包装产品</span>
                <span class="font-bold text-slate-800 text-sm">{{ activeOrderInfo.productName }}</span>
                <span class="text-xs text-slate-500">{{ activeOrderInfo.spec }}</span>
              </div>
            </div>
            <div class="w-64 bg-slate-50 p-2 rounded-lg border border-slate-100 shadow-inner">
              <div class="flex justify-between text-xs font-bold mb-1">
                <span class="text-slate-500">包装总进度</span>
                <span class="text-indigo-600">{{ activeOrderInfo.packedQty }} / {{ activeOrderInfo.targetQty }} PCS</span>
              </div>
              <Progress :percent="packProgress" :strokeColor="{'0%': '#818cf8', '100%': '#4f46e5'}" size="small" :showInfo="false" />
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 flex gap-3 min-h-0 relative">
        <div v-if="!activeOrderInfo" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 rounded-lg border border-slate-200">
          <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-indigo-500" />
          <span class="font-bold text-lg text-slate-700">请先在上方锁定包装任务，或从挂起队列中恢复任务</span>
        </div>

        <div class="w-[420px] flex flex-col gap-3 min-h-0">
          <Row :gutter="12" class="shrink-0">
            <Col :span="12"><Card size="small" class="bg-blue-50 border-blue-100 shadow-sm"><Statistic title="当前批次主箱" :value="stats.totalBoxes" valueStyle="color: #2563eb; font-weight: bold; font-size: 18px" /></Card></Col>
            <Col :span="12"><Card size="small" class="bg-green-50 border-green-100 shadow-sm"><Statistic title="累计装入单品" :value="stats.pieceCount" valueStyle="color: #16a34a; font-weight: bold; font-size: 18px" /></Card></Col>
          </Row>

          <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
            <div class="p-3 border-b shrink-0 bg-indigo-50 flex items-center justify-between">
              <span class="text-sm font-black text-indigo-800 flex items-center gap-2"><IconifyIcon icon="lucide:settings-2" /> 规则设定与扫码执行</span>
            </div>

            <div class="flex-1 overflow-y-auto p-4 custom-scrollbar flex flex-col">
              <Form layout="vertical">
                <Row :gutter="12" class="mb-4">
                  <Col :span="12"><Form.Item label="当前包装层级" class="font-bold text-slate-700 mb-0"><Select v-model:value="packMode" size="large" class="w-full bg-slate-50"><Select.Option value="INNER">单品 → 装内包</Select.Option><Select.Option value="OUTER">内包 → 装外箱</Select.Option></Select></Form.Item></Col>
                  <Col :span="12"><Form.Item label="满额封箱规则" class="font-bold text-slate-700 mb-0"><InputNumber v-model:value="capacityRule" size="large" class="w-full bg-slate-50" :min="1" /></Form.Item></Col>
                </Row>
                <Divider class="my-3 border-slate-200" />
                <div class="mb-5">
                  <div class="text-xs font-bold text-slate-500 mb-2">当前目标箱号：</div>
                  <div v-if="currentParentBarcode" class="p-3 bg-green-50 border border-green-300 rounded-lg flex justify-between items-center shadow-inner">
                    <span class="font-mono font-black text-green-700 text-lg">{{ currentParentBarcode }}</span><Tag color="green" class="!m-0">接收中</Tag>
                  </div>
                  <div v-else class="p-4 border-2 border-dashed border-slate-300 rounded-lg text-center text-slate-400 bg-slate-50">尚未开启目标包装</div>
                </div>
                <Button type="primary" size="large" class="bg-indigo-600 font-bold w-full shadow-md mb-6 hover:bg-indigo-500" @click="handleCreateNewBox">
                  <IconifyIcon icon="lucide:package-plus" class="mr-2" /> 开启新箱并生成箱码
                </Button>
                <Form.Item class="mb-0 bg-blue-50 p-3 rounded-lg border border-blue-200">
                  <div class="text-xs font-bold text-blue-800 mb-2">扫描投入物料 (自动回车装箱)</div>
                  <Input ref="scanInputRef" v-model:value="scanCode" size="large" class="shadow-inner font-mono text-xl border-blue-400 py-3" placeholder="请使用扫码枪..." @pressEnter="handleScanEnter">
                    <template #prefix><IconifyIcon icon="lucide:barcode" class="text-blue-500 text-xl mr-2" /></template>
                  </Input>
                </Form.Item>
              </Form>
            </div>
          </div>
        </div>

        <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
          <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2"><IconifyIcon icon="lucide:network" /> 实时包装层级拓扑树</div><span class="text-[10px] text-slate-400">支持追踪与解绑</span>
          </div>
          <div class="flex-1 overflow-y-auto p-4 custom-scrollbar bg-slate-50/50">
            <div v-if="packTreeData.length === 0" class="h-full flex flex-col items-center justify-center text-slate-400">
              <IconifyIcon icon="lucide:boxes" class="text-5xl mb-3 opacity-30" /><span>暂无装箱数据，请开启新箱后扫码装入。</span>
            </div>
            <Tree v-else :tree-data="packTreeData" defaultExpandAll blockNode class="bg-transparent font-mono">
              <template #title="{ dataRef }">
                <div v-if="dataRef.children" class="flex items-center justify-between w-full py-1 pr-2">
                  <div class="flex items-center gap-2">
                    <IconifyIcon :icon="dataRef.type === 'OUTER' ? 'lucide:package' : 'lucide:box'" class="text-indigo-600" />
                    <span class="font-bold text-slate-800 text-sm">{{ dataRef.title }}</span>
                    <Tag :color="dataRef.children.length >= dataRef.capacity ? 'green' : 'orange'" class="ml-2 !text-[10px]">{{ dataRef.children.length }} / {{ dataRef.capacity }}</Tag>
                  </div>
                  <div v-if="dataRef.children.length >= dataRef.capacity" class="text-green-600 text-xs flex items-center gap-1 font-bold"><IconifyIcon icon="lucide:check-circle-2" /> 已封箱</div>
                  <div v-else class="text-orange-500 text-xs animate-pulse">正在装箱中...</div>
                </div>
                <div v-else class="flex items-center justify-between w-full py-1 pr-2 group">
                  <div class="flex items-center gap-2 text-slate-600"><IconifyIcon icon="lucide:barcode" class="text-slate-400" /><span>{{ dataRef.title }}</span><span class="text-[10px] text-slate-400 ml-2 border-l pl-2">{{ dataRef.scannedAt }}</span></div>
                  <Popconfirm title="确定要将该条码解绑剔除吗？" @confirm="handleRemoveItem(packTreeData.find(b => b.children.some(c => c.key === dataRef.key)).key, dataRef.key)">
                    <Button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 transition-opacity">解绑剔除</Button>
                  </Popconfirm>
                </div>
              </template>
            </Tree>
          </div>
        </div>
      </div>
    </main>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ 包装任务挂起队列" placement="left" width="400">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400">
        <IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>当前没有被挂起的任务。</span>
      </div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-indigo-200 bg-indigo-50 shadow-sm hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-2">
            <div class="font-mono font-bold text-indigo-700 text-base">{{ task.orderInfo.orderNo }}</div><Tag color="orange" class="!m-0">已挂起</Tag>
          </div>
          <div class="text-xs text-slate-600 mb-1">产品: <span class="font-bold">{{ task.orderInfo.productName }}</span></div>
          <div class="text-xs text-slate-600 mb-3">进度保留 <span class="font-bold text-indigo-600">{{ task.orderInfo.packedQty }}</span> PCS | 未封箱 <span class="font-bold text-indigo-600">{{ task.treeData.length }}</span> 箱</div>
          <div class="flex justify-between items-center pt-2 border-t border-indigo-100">
            <span class="text-[10px] text-slate-400">挂起时间: {{ task.suspendTime }}</span>
            <Button type="primary" size="small" class="bg-indigo-600 text-xs font-bold" @click="handleResumeTask(idx)">恢复此任务</Button>
          </div>
        </Card>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDrawerVisible" title="📦 包装封箱台账记录" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <div class="mb-3 shrink-0">
          <Input.Search
            v-model:value="historySearchForm.keyword"
            placeholder="搜索箱号、订单号..."
            allow-clear
            @search="historyPagination.current = 1"
          />
        </div>

        <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 flex flex-col gap-3">
          <div v-if="filteredHistory.length === 0" class="flex flex-col items-center justify-center h-48 text-slate-400">
            <IconifyIcon icon="lucide:search-x" class="text-4xl mb-2 opacity-50" /><span>未找到相关包装记录</span>
          </div>

          <Card
            v-for="log in filteredHistory"
            :key="log.id"
            size="small"
            class="border border-slate-200 bg-white shadow-sm hover:border-indigo-300 hover:shadow-md transition-all cursor-pointer group"
            @click="viewHistoryDetail(log)"
          >
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-slate-800 text-[15px] flex items-center">
                <IconifyIcon icon="lucide:package-check" class="text-green-600 mr-2 text-lg"/>{{ log.boxCode }}
              </div>
              <Tag color="green" class="!m-0">已封存</Tag>
            </div>
            <div class="text-xs text-slate-600 mb-1">关联单号: <span class="font-bold">{{ log.orderNo }}</span></div>
            <div class="text-xs text-slate-600 mb-3">装入数量: <span class="font-bold text-indigo-600">{{ log.qty }}</span> PCS</div>
            <div class="flex justify-between items-center pt-2 border-t border-slate-100">
              <span class="text-[10px] text-slate-400">{{ log.time }} / {{ log.user }}</span>
              <span class="text-xs text-indigo-600 font-bold flex items-center opacity-0 group-hover:opacity-100 transition-opacity">查看追溯 <IconifyIcon icon="lucide:chevron-right" /></span>
            </div>
          </Card>
        </div>

        <div class="shrink-0 pt-3 border-t mt-2 text-right">
          <Pagination
            v-model:current="historyPagination.current"
            :total="historyPagination.total"
            :pageSize="historyPagination.pageSize"
            size="small"
            show-less-items
          />
        </div>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDetailVisible" :title="`装箱明细: ${currentHistoryDetail?.boxCode}`" placement="right" width="400">
      <div class="bg-slate-50 p-4 rounded-lg border border-slate-200 h-full overflow-y-auto custom-scrollbar shadow-inner pointer-events-none">
        <Tree v-if="currentHistoryDetail" :tree-data="currentHistoryDetail.snapshotTree" defaultExpandAll blockNode class="bg-transparent font-mono opacity-80">
          <template #title="{ dataRef }">
            <div v-if="dataRef.children" class="flex items-center gap-2 py-1">
              <IconifyIcon :icon="dataRef.type === 'OUTER' ? 'lucide:package' : 'lucide:box'" class="text-indigo-600" />
              <span class="font-bold text-slate-800 text-sm">{{ dataRef.title }}</span>
              <Tag color="default" class="ml-2 !text-[10px]">满箱: {{ dataRef.children.length }}</Tag>
            </div>
            <div v-else class="flex items-center gap-2 text-slate-600 py-1">
              <IconifyIcon icon="lucide:barcode" class="text-slate-400" />
              <span>{{ dataRef.title }}</span>
              <span class="text-[10px] text-slate-400 ml-2 border-l pl-2">{{ dataRef.scannedAt }}</span>
            </div>
          </template>
        </Tree>
      </div>
    </Drawer>

  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }

:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2) !important; border-color: #6366f1 !important; }
:deep(.ant-tree-treenode) { padding: 4px 0 !important; }
:deep(.ant-tree-node-content-wrapper) { width: 100%; border-radius: 6px; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
.animate-fade-in { animation: fadeIn 0.2s ease-out forwards; }
</style>
