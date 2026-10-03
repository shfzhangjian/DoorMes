<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import {
  Button, Card, Tag, Input, Table, Radio, Divider, Form, Row, Col, Drawer, Avatar, Badge, Pagination, Popconfirm, Descriptions, message, Modal
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
  userName: '钱调拨', userId: 'W8044', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Qian', loginTime: new Date().toLocaleTimeString()
});

// ==================================================================================
// 2. 业务模式与挂起队列
// ==================================================================================
const transferMode = ref<'RELOCATE' | 'REPLENISH'>('RELOCATE'); // RELOCATE: 库内理货, REPLENISH: 线边补料

const fromLocationCode = ref('');
const toLocationCode = ref('');
const isFromLocked = ref(false);
const isToLocked = ref(false);

const fromLocationInputRef = ref();
const toLocationInputRef = ref();
const itemInputRef = ref();

const itemScanCode = ref('');
const scannedItems = ref<any[]>([]);

// 挂起队列
const suspendedTasks = ref<any[]>([]);
const suspendedDrawerVisible = ref(false);

function handleModeChange() {
  if (scannedItems.value.length > 0) {
    message.warning('当前有未过账的移库明细，请先挂起或过账后再切换模式！');
    setTimeout(() => { transferMode.value = transferMode.value === 'RELOCATE' ? 'REPLENISH' : 'RELOCATE'; }, 10);
    return;
  }
}

function handleSuspendTask() {
  if (scannedItems.value.length === 0 && !isFromLocked.value && !isToLocked.value) {
    return message.warning('当前工作台为空，无需挂起！');
  }
  suspendedTasks.value.push({
    id: Date.now(), mode: transferMode.value,
    fromLocationCode: fromLocationCode.value, isFromLocked: isFromLocked.value,
    toLocationCode: toLocationCode.value, isToLocked: isToLocked.value,
    scannedItems: clone(scannedItems.value), suspendTime: new Date().toLocaleTimeString()
  });

  handleClearAll();
  message.success('当前移库任务已挂起！');
}

function handleResumeTask(index: number) {
  if (scannedItems.value.length > 0) return message.warning('当前有进行中的任务，请先挂起！');
  const task = suspendedTasks.value[index];

  transferMode.value = task.mode;
  fromLocationCode.value = task.fromLocationCode; isFromLocked.value = task.isFromLocked;
  toLocationCode.value = task.toLocationCode; isToLocked.value = task.isToLocked;
  scannedItems.value = clone(task.scannedItems);

  suspendedTasks.value.splice(index, 1); suspendedDrawerVisible.value = false;
  message.success('挂起任务已恢复！');
  focusItemScanner();
}

function handleClearConfirm() {
  if (scannedItems.value.length > 0) {
    return Modal.confirm({
      title: '防错拦截', content: '当前有未过账数据。确认要强制清空吗？',
      onOk: handleClearAll
    });
  }
  handleClearAll();
}

function handleClearAll() {
  scannedItems.value = [];
  fromLocationCode.value = ''; isFromLocked.value = false;
  toLocationCode.value = ''; isToLocked.value = false;
  itemScanCode.value = '';
}

// ==================================================================================
// 3. 库位锁定与扫码移库执行
// ==================================================================================
const itemColumns: TableColumnsType = [
  { title: '物料/批次条码', dataIndex: 'barcode', width: 170 },
  { title: '物料名称', dataIndex: 'productName', width: 140 },
  { title: '移出库位(From)', dataIndex: 'fromLoc', width: 110, align: 'center' },
  { title: '移入库位(To)', dataIndex: 'toLoc', width: 110, align: 'center' },
  { title: '移库数量', dataIndex: 'qty', width: 90, align: 'right' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleLockFrom() {
  if (!fromLocationCode.value.trim()) return message.warning('请扫描来源库位条码');
  isFromLocked.value = true; message.success(`锁定来源库位: ${fromLocationCode.value}`);
  if (!isToLocked.value) nextTick(() => toLocationInputRef.value?.focus());
  else focusItemScanner();
}

function handleLockTo() {
  if (!toLocationCode.value.trim()) return message.warning('请扫描目标库位条码');
  isToLocked.value = true; message.success(`锁定目标库位: ${toLocationCode.value}`);
  if (!isFromLocked.value) nextTick(() => fromLocationInputRef.value?.focus());
  else focusItemScanner();
}

function handleScanItemEnter() {
  let code = itemScanCode.value.trim();
  if (!isFromLocked.value) { itemScanCode.value = ''; return message.error('请先锁定【移出来源】库位！'); }
  if (!isToLocked.value) { itemScanCode.value = ''; return message.error('请先锁定【移入目标】库位！'); }
  if (fromLocationCode.value === toLocationCode.value) { itemScanCode.value = ''; return message.error('来源库位不能与目标库位相同！'); }
  if (!code) return message.warning('请输入物料条码');

  if (scannedItems.value.some(b => b.barcode === code)) {
    itemScanCode.value = ''; return message.error(`条码已被扫描，严禁重复移库！`);
  }

  // 模拟从后台查询条码信息并校验是否在 fromLocation 内
  const mockProduct = code.includes('RM') ? 'PET光学基膜 (主材)' : '特种交联剂';
  const mockQty = Math.floor(Math.random() * 50) + 10;

  scannedItems.value.unshift({
    id: Date.now() + Math.random(),
    barcode: code,
    productName: mockProduct,
    fromLoc: fromLocationCode.value,
    toLoc: toLocationCode.value,
    qty: mockQty, unit: 'KG',
    time: new Date().toLocaleTimeString()
  });

  message.success(`条码 ${code} 记入移库账本`);
  itemScanCode.value = '';
  focusItemScanner();
}

function handleRemoveItem(id: number) {
  scannedItems.value = scannedItems.value.filter(b => b.id !== id);
  focusItemScanner();
}

function focusItemScanner() { nextTick(() => { itemInputRef.value?.focus(); }); }

// ==================================================================================
// 4. 写账过账与打印调拨单
// ==================================================================================
const historyDrawerVisible = ref(false);
const transferHistoryLogs = ref<any[]>([]);
const historySearchForm = ref({ keyword: '' });
const historyPagination = ref({ current: 1, pageSize: 6, total: 0 });
const historyDetailVisible = ref(false);
const currentHistoryDetail = ref<any>(null);

const printModalVisible = ref(false);
const currentSlipData = ref<any>(null);

function handleConfirmTransfer() {
  if (scannedItems.value.length === 0) return message.warning('移库明细为空！');

  let totalQty = 0;
  scannedItems.value.forEach((item: any) => totalQty += item.qty);

  Modal.confirm({
    title: `确认执行移库过账？`,
    content: `将生成 ${scannedItems.value.length} 笔调拨事务，底层执行：\n\n[-] 扣减 ${fromLocationCode.value} 账面库存\n[+] 增加 ${toLocationCode.value} 账面库存`,
    onOk: () => {
      const slipPrefix = transferMode.value === 'RELOCATE' ? 'TRX-MV' : 'TRX-RP';
      const newSlipNo = `${slipPrefix}-20260219-${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`;

      const logData = {
        id: Date.now(), slipNo: newSlipNo,
        mode: transferMode.value === 'RELOCATE' ? '库内理货移位' : '线边总仓补料',
        fromLoc: fromLocationCode.value, toLoc: toLocationCode.value,
        totalQty, itemCount: scannedItems.value.length,
        time: new Date().toLocaleString(), user: currentUser.value.userName,
        snapshotList: clone(scannedItems.value)
      };

      transferHistoryLogs.value.unshift(logData);

      currentSlipData.value = logData;
      printModalVisible.value = true;

      handleClearAll();
    }
  });
}

const filteredHistory = computed(() => {
  let filtered = transferHistoryLogs.value;
  if (historySearchForm.value.keyword) {
    const kw = historySearchForm.value.keyword.toLowerCase();
    filtered = filtered.filter(log => log.slipNo.toLowerCase().includes(kw) || log.fromLoc.toLowerCase().includes(kw));
  }
  historyPagination.value.total = filtered.length;
  const start = (historyPagination.value.current - 1) * historyPagination.value.pageSize;
  return filtered.slice(start, start + historyPagination.value.pageSize);
});

function viewHistoryDetail(log: any) { currentHistoryDetail.value = log; historyDetailVisible.value = true; }
function printSlip() { message.success('移库调拨单打印指令已下发！'); printModalVisible.value = false; }
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">

    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-cyan-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:arrow-right-left" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 库内移位与调拨台</span>
      </div>
      <div class="flex items-center gap-5 shrink-0">
        <Radio.Group v-model:value="transferMode" button-style="solid" @change="handleModeChange" class="mr-4">
          <Radio.Button value="RELOCATE"><IconifyIcon icon="lucide:arrow-down-up" class="mr-1 inline-block"/> 库内理货移库</Radio.Button>
          <Radio.Button value="REPLENISH"><IconifyIcon icon="lucide:truck-fast" class="mr-1 inline-block"/> 跨仓线边补料</Radio.Button>
        </Radio.Group>

        <Badge :count="suspendedTasks.length" :offset="[-5, 5]">
          <Button type="primary" ghost class="font-bold border-cyan-200 text-cyan-700" @click="suspendedDrawerVisible = true"><IconifyIcon icon="lucide:layers" class="mr-1" /> 挂起队列</Button>
        </Badge>
        <Button type="dashed" class="border-cyan-200 text-cyan-700 font-bold" @click="historyDrawerVisible = true; historySearchForm.keyword = ''; historyPagination.current = 1;"><IconifyIcon icon="lucide:history" class="mr-1" /> 调拨历史</Button>
        <Divider type="vertical" class="bg-slate-200 h-6 m-0" />
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-cyan-100" />
            <div class="flex flex-col"><span class="text-xs font-bold leading-none">{{ currentUser.userName }}</span></div>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex gap-3 p-3 overflow-hidden min-h-0">

      <div class="w-[420px] flex flex-col gap-3 min-h-0 shrink-0">

        <div class="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden shrink-0">
          <div class="p-3 border-b bg-cyan-50 flex justify-between items-center">
            <span class="text-sm font-black text-cyan-800 flex items-center gap-2"><IconifyIcon icon="lucide:map-pin" /> 步骤 1：锁定移库链路</span>
            <div class="flex gap-2">
              <Button type="default" danger size="small" class="text-xs" @click="handleClearConfirm">重置重来</Button>
              <Button type="primary" size="small" class="bg-orange-500 border-none font-bold" @click="handleSuspendTask">临时挂起</Button>
            </div>
          </div>
          <div class="p-4">
            <div class="mb-4">
              <div class="text-xs font-bold text-slate-500 mb-1 flex items-center gap-1">移出源库位 (From)</div>
              <div v-if="!isFromLocked" class="flex gap-2">
                <Input ref="fromLocationInputRef" v-model:value="fromLocationCode" placeholder="扫描移出库位" size="large" @pressEnter="handleLockFrom" class="font-mono border-slate-300"><template #prefix><IconifyIcon icon="lucide:log-out" class="text-orange-500" /></template></Input>
                <Button type="default" size="large" @click="handleLockFrom">锁定</Button>
              </div>
              <div v-else class="p-2 bg-orange-50 border border-orange-200 rounded flex justify-between items-center">
                <span class="font-mono font-bold text-orange-700 text-lg">{{ fromLocationCode }}</span>
                <Button type="link" danger size="small" @click="isFromLocked = false; fromLocationCode = ''">解锁</Button>
              </div>
            </div>

            <div class="flex justify-center -my-3 relative z-10">
              <div class="bg-white border border-slate-200 p-1 rounded-full text-slate-300"><IconifyIcon icon="lucide:arrow-down" class="text-xl" /></div>
            </div>

            <div class="mt-2">
              <div class="text-xs font-bold text-slate-500 mb-1 flex items-center gap-1">移入目标库位 (To)</div>
              <div v-if="!isToLocked" class="flex gap-2">
                <Input ref="toLocationInputRef" v-model:value="toLocationCode" placeholder="扫描目标库位" size="large" @pressEnter="handleLockTo" class="font-mono border-slate-300"><template #prefix><IconifyIcon icon="lucide:log-in" class="text-green-500" /></template></Input>
                <Button type="default" size="large" @click="handleLockTo">锁定</Button>
              </div>
              <div v-else class="p-2 bg-green-50 border border-green-200 rounded flex justify-between items-center">
                <span class="font-mono font-bold text-green-700 text-lg">{{ toLocationCode }}</span>
                <Button type="link" danger size="small" @click="isToLocked = false; toLocationCode = ''">解锁</Button>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 min-h-0">
          <div class="p-3 border-b bg-cyan-50 flex justify-between items-center">
            <span class="text-sm font-black text-cyan-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 步骤 2：扫描实物转移</span>
          </div>
          <div class="p-4 flex-1 flex flex-col relative">
            <div v-if="!isFromLocked || !isToLocked" class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center text-slate-500 border-t border-slate-100">
              <IconifyIcon icon="lucide:lock" class="text-4xl mb-3 opacity-50 text-cyan-500" />
              <span class="font-bold text-sm text-slate-700">请先在上方锁定完整的调拨移库链路</span>
            </div>

            <Form.Item class="mb-0 bg-cyan-50 p-3 rounded-lg border border-cyan-200 shadow-inner">
              <div class="text-xs font-bold text-cyan-800 mb-2">扫描被移动的物料标签 (自动入账)</div>
              <Input ref="itemInputRef" v-model:value="itemScanCode" size="large" class="font-mono text-xl border-cyan-400 py-3 shadow-sm" placeholder="扫描实物条码..." @pressEnter="handleScanItemEnter">
                <template #prefix><IconifyIcon icon="lucide:barcode" class="text-cyan-500 text-xl mr-2" /></template>
              </Input>
            </Form.Item>
            <div class="mt-4 text-xs text-slate-400">
              <div class="flex items-center gap-1 mb-1"><IconifyIcon icon="lucide:info" class="text-cyan-500"/> 系统将自动校验该条码是否存在于源库位。</div>
              <div class="flex items-center gap-1"><IconifyIcon icon="lucide:info" class="text-cyan-500"/> 整托转移时，只需扫描主托盘码即可带出所有子项。</div>
            </div>
          </div>
        </div>

      </div>

      <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
        <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
          <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> {{ transferMode === 'RELOCATE' ? '库内移位清单' : '线边调拨清单' }}</div>
          <span class="text-[10px] text-slate-400">核对无误后点击下方过账</span>
        </div>
        <div class="flex-1 flex flex-col">
          <Table :columns="itemColumns" :dataSource="scannedItems" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 190px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold text-slate-700">{{ record.barcode }}</span></template>
              <template v-if="column.dataIndex === 'fromLoc'"><Tag color="orange" class="font-mono">{{ record.fromLoc }}</Tag></template>
              <template v-if="column.dataIndex === 'toLoc'"><Tag color="green" class="font-mono">{{ record.toLoc }}</Tag></template>
              <template v-if="column.dataIndex === 'qty'"><span class="font-bold text-cyan-700">{{ record.qty }}</span> <span class="text-[10px] text-slate-400">{{ record.unit }}</span></template>
              <template v-if="column.dataIndex === 'action'"><Popconfirm title="确定撤销吗？" @confirm="handleRemoveItem(record.id)"><Button type="text" danger size="small">撤销</Button></Popconfirm></template>
            </template>
            <template #emptyText>
              <div class="py-20 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:scan-face" class="text-5xl mb-3 opacity-30" /><span>锁定链路后扫码移库。</span></div>
            </template>
          </Table>
        </div>

        <div class="p-3 border-t bg-slate-50 flex justify-between items-center shrink-0">
          <div class="text-xs text-slate-500">
            共计扫入 <span class="font-bold text-cyan-600 text-base">{{ scannedItems.length }}</span> 笔明细
          </div>
          <Button type="primary" class="bg-cyan-600 border-none font-bold px-10 shadow-lg" size="large" :disabled="scannedItems.length === 0" @click="handleConfirmTransfer">
            <IconifyIcon icon="lucide:arrow-right-left" class="mr-2" /> 确认执行移库过账
          </Button>
        </div>
      </div>
    </main>

    <Modal v-model:open="printModalVisible" title="🖨️ 打印移库调拨凭证" :footer="null" width="550px" centered>
      <div class="py-4">
        <div class="bg-white border-2 border-slate-800 p-6 rounded-lg shadow-sm mx-auto w-full font-mono relative">
          <div class="text-center text-lg font-black tracking-widest mb-4 border-b-2 border-slate-800 pb-2">【 禾臣新材料 - {{ currentSlipData?.mode }}单 】</div>
          <Descriptions size="small" :column="1" bordered :labelStyle="{ fontWeight: 'bold', width: '120px' }">
            <Descriptions.Item label="调拨流水号"><span class="text-cyan-700 font-bold text-base">{{ currentSlipData?.slipNo }}</span></Descriptions.Item>
            <Descriptions.Item label="移出区域 (From)"><span class="text-orange-600 font-bold">{{ currentSlipData?.fromLoc }}</span></Descriptions.Item>
            <Descriptions.Item label="移入区域 (To)"><span class="text-green-600 font-bold">{{ currentSlipData?.toLoc }}</span></Descriptions.Item>
            <Descriptions.Item label="过账时间">{{ currentSlipData?.time }}</Descriptions.Item>
          </Descriptions>
          <div class="mt-4 border border-slate-300">
            <div class="bg-slate-100 font-bold text-xs p-2 border-b border-slate-300 flex justify-between"><span>转移明细</span><span>共 {{ currentSlipData?.itemCount }} 项</span></div>
            <div class="max-h-32 overflow-y-auto custom-scrollbar p-2 text-[10px]">
              <div v-for="(item, idx) in currentSlipData?.snapshotList" :key="idx" class="flex justify-between border-b border-dashed border-slate-200 py-1">
                <span>{{ item.barcode }} ({{ item.productName }})</span><span class="font-bold">{{ item.qty }} {{ item.unit }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="flex gap-4 mt-6">
          <Button block size="large" @click="printModalVisible = false">关闭</Button>
          <Button block type="primary" size="large" class="bg-cyan-600 font-bold shadow-md" @click="printSlip"><IconifyIcon icon="lucide:printer" class="mr-2"/> 打印调拨单据</Button>
        </div>
      </div>
    </Modal>

    <Drawer v-model:open="suspendedDrawerVisible" title="⏸️ 移库任务挂起队列" placement="left" width="400">
      <div v-if="suspendedTasks.length === 0" class="flex flex-col items-center justify-center h-full text-slate-400"><IconifyIcon icon="lucide:coffee" class="text-5xl mb-3 opacity-30" /><span>无挂起任务</span></div>
      <div class="flex flex-col gap-3">
        <Card v-for="(task, idx) in suspendedTasks" :key="task.id" size="small" class="border border-cyan-200 bg-cyan-50 shadow-sm hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-2"><div class="font-mono font-bold text-cyan-800 text-base">{{ task.mode === 'RELOCATE' ? '库内移位' : '线边补料' }}</div><Tag color="orange" class="!m-0">已挂起</Tag></div>
          <div class="text-xs text-slate-600 mb-1">From: <span class="font-bold">{{ task.fromLocationCode || '未锁定' }}</span></div>
          <div class="text-xs text-slate-600 mb-2">To: <span class="font-bold">{{ task.toLocationCode || '未锁定' }}</span></div>
          <Button type="primary" size="small" class="bg-cyan-600 w-full" @click="handleResumeTask(idx)">恢复任务</Button>
        </Card>
      </div>
    </Drawer>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 调拨流水与凭证" placement="right" width="450">
      <div class="flex flex-col h-full overflow-hidden">
        <Input.Search v-model:value="historySearchForm.keyword" placeholder="搜索流水号/库位..." class="mb-3 shrink-0" allow-clear @search="historyPagination.current = 1" />
        <div class="flex-1 overflow-y-auto pr-2 flex flex-col gap-3 custom-scrollbar">
          <Card v-for="log in filteredHistory" :key="log.id" size="small" class="border border-slate-200 cursor-pointer group hover:border-cyan-400 hover:shadow-md transition-all" @click="viewHistoryDetail(log)">
            <div class="flex justify-between items-start mb-2">
              <div class="font-mono font-bold text-cyan-700 flex items-center">{{ log.slipNo }}</div><Tag color="blue" class="!m-0">{{ log.mode }}</Tag>
            </div>
            <div class="text-[10px] text-slate-500 mb-1 flex justify-between">
              <span><span class="text-orange-500">出:</span> {{ log.fromLoc }}</span> <IconifyIcon icon="lucide:arrow-right" /> <span><span class="text-green-500">入:</span> {{ log.toLoc }}</span>
            </div>
            <div class="text-[10px] text-slate-400 mt-2 pt-2 border-t flex justify-between items-center">
              <span>{{ log.time }}</span><span class="text-cyan-600 font-bold opacity-0 group-hover:opacity-100">查看调拨单 <IconifyIcon icon="lucide:chevron-right" /></span>
            </div>
          </Card>
        </div>
        <Pagination v-model:current="historyPagination.current" :total="historyPagination.total" :pageSize="historyPagination.pageSize" size="small" class="mt-2 text-right" />
      </div>
    </Drawer>

    <Drawer v-model:open="historyDetailVisible" :title="`事务明细: ${currentHistoryDetail?.slipNo}`" placement="right" width="600">
      <div class="h-full flex flex-col">
        <div class="mb-3 flex justify-between items-center bg-cyan-50 p-2 rounded border border-cyan-100">
          <span class="text-xs font-bold text-cyan-800">如需重打调拨凭证，请点击右侧按钮</span>
          <Button size="small" type="primary" class="bg-cyan-600" @click="printModalVisible = true; historyDetailVisible = false"><IconifyIcon icon="lucide:printer" class="mr-1"/> 补打凭证</Button>
        </div>
        <Table :columns="itemColumns.filter(c=>c.dataIndex!=='action')" :dataSource="currentHistoryDetail?.snapshotList" :pagination="false" size="small" class="vben-schema-table flex-1 overflow-y-auto custom-scrollbar" />
      </div>
    </Drawer>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.2) !important; border-color: #0891b2 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }
</style>
