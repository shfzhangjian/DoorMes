<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue';
import { Button, Card, Tag, Input, Table, Select, message, Modal, Divider, Form, Drawer, Avatar, Pagination } from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const currentUser = ref({ userName: '吴品控', userId: 'W8066', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Wu', loginTime: new Date().toLocaleTimeString() });

const scanInputRef = ref();
const scanCode = ref('');
const targetStatus = ref<'HOLD' | 'ACTIVE'>('HOLD'); // 操作目标：冻结 还是 解冻
const reasonCode = ref('IQC_REJECT'); // 操作原因代码

const reasonOptions = [
  { value: 'IQC_REJECT', label: '来料检验不合格 (IQC)' },
  { value: 'OQC_REJECT', label: '出货检验拦截 (OQC)' },
  { value: 'EXPIRED', label: '物资超期/过期' },
  { value: 'PENDING_REVIEW', label: '待复判/隔离观察' },
  { value: 'RELEASE_REWORK', label: '返工合格放行 (解冻)' },
  { value: 'RELEASE_SPECIAL', label: '特采让步放行 (解冻)' }
];

const scannedItems = ref<any[]>([]);
const holdHistoryLogs = ref<any[]>([]);
const historyDrawerVisible = ref(false);

const columns: TableColumnsType = [
  { title: '物料/批次条码', dataIndex: 'barcode', width: 160 },
  { title: '物料名称', dataIndex: 'productName', width: 140 },
  { title: '原状态', dataIndex: 'currentStatus', width: 100, align: 'center' },
  { title: '目标操作', dataIndex: 'targetStatus', width: 100, align: 'center' },
  { title: '变动原因', dataIndex: 'reason', width: 160 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleScanEnter() {
  const code = scanCode.value.trim();
  if (!code) return message.warning('请输入物料条码');
  if (scannedItems.value.some(b => b.barcode === code)) {
    scanCode.value = ''; return message.error(`条码已被扫描，严禁重复添加！`);
  }

  // 模拟查库
  const isCurrentlyHold = code.includes('NG');
  const currentStatus = isCurrentlyHold ? 'HOLD' : 'ACTIVE';

  if (targetStatus.value === currentStatus) {
    scanCode.value = '';
    return message.error(`防错拦截：该条码当前已是 [${currentStatus === 'HOLD' ? '冻结' : '可用'}] 状态，无需重复操作！`);
  }

  const reasonLabel = reasonOptions.find(r => r.value === reasonCode.value)?.label;

  scannedItems.value.unshift({
    id: Date.now(), barcode: code, productName: 'PET光学基膜 (测试批)',
    currentStatus, targetStatus: targetStatus.value, reason: reasonLabel, time: new Date().toLocaleTimeString()
  });

  message.success(`条码 ${code} 已加入变更队列`);
  scanCode.value = '';
  nextTick(() => scanInputRef.value?.focus());
}

function handleRemoveItem(id: number) { scannedItems.value = scannedItems.value.filter(b => b.id !== id); }

function handleConfirmHold() {
  if (scannedItems.value.length === 0) return message.warning('变更明细为空！');

  Modal.confirm({
    title: '确认执行库存状态变更？',
    content: `将对 ${scannedItems.value.length} 笔库存条码执行【${targetStatus.value === 'HOLD' ? '冻结隔离' : '解除冻结'}】。`,
    okText: '确认变更过账',
    onOk: () => {
      holdHistoryLogs.value.unshift({
        id: Date.now(), batchNo: `QC-${Date.now().toString().slice(-6)}`,
        action: targetStatus.value === 'HOLD' ? '冻结库存' : '解除冻结',
        itemCount: scannedItems.value.length, time: new Date().toLocaleString(), user: currentUser.value.userName
      });
      message.success('✅ 库存状态变更已生效！');
      scannedItems.value = [];
    }
  });
}
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">
    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-rose-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:shield-alert" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 质量冻结与解冻管控台</span>
      </div>
      <div class="flex items-center gap-5">
        <Button type="dashed" class="border-rose-200 text-rose-700 font-bold" @click="historyDrawerVisible = true"><IconifyIcon icon="lucide:history" class="mr-1" /> 状态变更记录</Button>
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-3 pr-2">
            <Avatar shape="square" :src="currentUser.avatar" size="small" class="border border-rose-100" />
            <span class="text-xs font-bold leading-none">{{ currentUser.userName }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="flex-1 flex gap-3 p-3 overflow-hidden min-h-0">
      <div class="w-[400px] flex flex-col gap-3 min-h-0 shrink-0">
        <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
          <div class="p-3 border-b bg-rose-50 flex justify-between items-center">
            <span class="text-sm font-black text-rose-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 管控策略与扫码区</span>
          </div>
          <div class="flex-1 p-4 flex flex-col">
            <Form layout="vertical">
              <Form.Item label="目标操作动作" class="font-bold text-slate-700 mb-4">
                <Select v-model:value="targetStatus" size="large" class="w-full shadow-sm">
                  <Select.Option value="HOLD"><span class="text-rose-600 font-bold">🔴 冻结 (隔离不可用)</span></Select.Option>
                  <Select.Option value="ACTIVE"><span class="text-green-600 font-bold">🟢 解冻 (恢复可用)</span></Select.Option>
                </Select>
              </Form.Item>
              <Form.Item label="质量变更原因代码" class="font-bold text-slate-700 mb-6">
                <Select v-model:value="reasonCode" :options="reasonOptions" size="large" class="w-full shadow-sm" />
              </Form.Item>
              <Form.Item class="mb-0 mt-2 bg-rose-50 p-3 rounded-lg border border-rose-200 shadow-inner">
                <div class="text-[10px] font-bold text-rose-800 mb-1">扫描目标物料标签 (自动校验当前状态)</div>
                <Input ref="scanInputRef" v-model:value="scanCode" size="large" class="font-mono text-xl border-rose-400 py-3 shadow-sm" placeholder="扫描实物条码..." @pressEnter="handleScanEnter">
                  <template #prefix><IconifyIcon icon="lucide:barcode" class="text-rose-500 text-xl mr-2" /></template>
                </Input>
              </Form.Item>
            </Form>
          </div>
        </div>
      </div>

      <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col min-h-0">
        <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between shrink-0">
          <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> 库存状态变更清单</div>
        </div>
        <div class="flex-1 flex flex-col">
          <Table :columns="columns" :dataSource="scannedItems" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 190px)' }" class="vben-schema-table flex-1">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold text-slate-700">{{ record.barcode }}</span></template>
              <template v-if="column.dataIndex === 'currentStatus'">
                <Tag :color="record.currentStatus === 'ACTIVE' ? 'green' : 'red'">{{ record.currentStatus === 'ACTIVE' ? '可用' : '已冻结' }}</Tag>
              </template>
              <template v-if="column.dataIndex === 'targetStatus'">
                <Tag :color="record.targetStatus === 'HOLD' ? 'red' : 'green'" class="font-bold border-2">{{ record.targetStatus === 'HOLD' ? '变更为: 冻结' : '变更为: 可用' }}</Tag>
              </template>
              <template v-if="column.dataIndex === 'action'"><Button type="text" danger size="small" @click="handleRemoveItem(record.id)">撤销</Button></template>
            </template>
            <template #emptyText>
              <div class="py-20 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:scan-face" class="text-5xl mb-3 opacity-30" /><span>选择策略后扫码录入。</span></div>
            </template>
          </Table>
        </div>
        <div class="p-3 border-t bg-slate-50 flex justify-between items-center shrink-0">
          <div class="text-xs text-slate-500">共计变更 <span class="font-bold text-rose-600 text-base">{{ scannedItems.length }}</span> 笔明细</div>
          <Button type="primary" :class="targetStatus === 'HOLD' ? 'bg-rose-600 hover:bg-rose-500' : 'bg-green-600 hover:bg-green-500'" class="border-none font-bold px-10 shadow-lg" size="large" :disabled="scannedItems.length === 0" @click="handleConfirmHold">
            <IconifyIcon icon="lucide:shield-check" class="mr-2" /> 确认执行状态变更过账
          </Button>
        </div>
      </div>
    </main>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 状态变更历史流水" placement="right" width="400">
      <div v-for="log in holdHistoryLogs" :key="log.id" class="mb-3 border border-slate-200 rounded p-3 bg-white shadow-sm">
        <div class="flex justify-between items-start mb-2">
          <span class="font-mono font-bold text-slate-700">{{ log.batchNo }}</span>
          <Tag :color="log.action.includes('冻结') ? 'red' : 'green'">{{ log.action }}</Tag>
        </div>
        <div class="text-xs text-slate-500 mb-1">变更明细: <span class="font-bold text-slate-800">{{ log.itemCount }} 笔</span></div>
        <div class="text-[10px] text-slate-400 pt-2 border-t mt-2">{{ log.time }} / {{ log.user }}</div>
      </div>
    </Drawer>
  </div>
</template>
<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.2) !important; border-color: #e11d48 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; }
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }
</style>
