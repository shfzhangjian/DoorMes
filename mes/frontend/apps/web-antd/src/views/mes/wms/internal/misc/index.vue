<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue';
import { Button, Card, Tag, Input, InputNumber, Table, Radio, Select, message, Modal, Form, Row, Col, Avatar, Drawer } from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const currentUser = ref({ userName: '李杂项', userId: 'W8077', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Li', loginTime: new Date().toLocaleTimeString() });

const miscMode = ref<'RECEIPT' | 'ISSUE'>('ISSUE'); // 杂收 (+) vs 杂发 (-)
const costCenter = ref('DEPT_RD'); // 归属成本中心
const reasonType = ref('RD_SAMPLE'); // 杂项类型
const remarks = ref(''); // 💡 新增：备注说明

const reasonMap: any = {
  'RECEIPT': [
    { value: 'INV_INIT', label: '期初库存导入 (盘盈)' },
    { value: 'CUST_RETURN', label: '客户特殊退货 (无单)' },
    { value: 'PROD_SCRAP_RECOV', label: '车间废料回收入库' }
  ],
  'ISSUE': [
    { value: 'RD_SAMPLE', label: '研发部门领料 (实验用)' },
    { value: 'DESTRUCT', label: '过期/破损报废销毁' },
    { value: 'INV_ADJUST', label: '盘亏出库调整' }
  ]
};

const locationCode = ref('');
const isLocationLocked = ref(false);
const itemScanCode = ref('');
const inputQty = ref<number>(1);
const scannedItems = ref<any[]>([]);
const historyLogs = ref<any[]>([]);
const historyDrawerVisible = ref(false);

const columns: TableColumnsType = [
  { title: '物料条码', dataIndex: 'barcode', width: 160 },
  { title: '物料编码', dataIndex: 'productCode', width: 120 }, // 💡 新增
  { title: '物料名称', dataIndex: 'productName', width: 140 }, // 💡 新增
  { title: '操作库位', dataIndex: 'location', width: 100, align: 'center' },
  { title: '数量', dataIndex: 'qty', width: 80, align: 'right' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

function handleModeChange() {
  scannedItems.value = [];
  reasonType.value = reasonMap[miscMode.value][0].value;
  remarks.value = ''; // 切换模式时清空备注
}

function handleLockLocation() {
  if (!locationCode.value) return message.warning('请锁定目标库位');
  isLocationLocked.value = true;
}

function handleScanItemEnter() {
  const code = itemScanCode.value.trim();
  if (!isLocationLocked.value) return message.error('请先锁定物理库位！');
  if (!code) return message.warning('扫描物料条码');
  if (scannedItems.value.some(b => b.barcode === code)) return message.error('条码已在列表中！');
  const mockCode = code.includes('RM') ? 'MAT-RM-001' : 'MAT-WIP-002';
  const mockName = code.includes('RM') ? 'PET光学基膜 (主材)' : '测试副料';

  scannedItems.value.unshift({
    id: Date.now(),
    barcode: code,
    productCode: mockCode,     // 💡 写入编码
    productName: mockName,     // 💡 写入名称
    location: locationCode.value,
    qty: inputQty.value,
    time: new Date().toLocaleTimeString()
  });

  message.success(`条码 ${code} 已录入`);
  itemScanCode.value = '';
}

function handleConfirmMisc() {
  if (scannedItems.value.length === 0) return message.warning('明细为空！');
  if (!remarks.value.trim()) return message.warning('请在左上角表单中填写备注说明，以便日后审计！'); // 💡 强制要求填写备注

  const actionName = miscMode.value === 'RECEIPT' ? '杂项入库 (+)' : '杂项发料出库 (-)';

  Modal.confirm({
    title: `确认执行【${actionName}】过账？`,
    content: `将生成无单据关联的底层账务流水。成本中心归集代码: ${costCenter.value}`,
    onOk: () => {
      historyLogs.value.unshift({
        id: Date.now(), trxNo: `MISC-${Date.now().toString().slice(-6)}`,
        mode: actionName, reason: reasonOptions.value.find((r:any) => r.value === reasonType.value)?.label,
        remarks: remarks.value, // 💡 记录备注
        itemCount: scannedItems.value.length, time: new Date().toLocaleString()
      });
      message.success('✅ 杂项过账成功！账务已强制更新。');

      // 清空工作台
      scannedItems.value = [];
      isLocationLocked.value = false;
      locationCode.value = '';
      remarks.value = '';
    }
  });
}

const reasonOptions = computed(() => reasonMap[miscMode.value]);
</script>

<template>
  <div class="flex flex-col absolute inset-0 bg-[#f4f6f8] overflow-hidden">
    <header class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4 shadow-sm z-20">
      <div class="flex items-center gap-3">
        <div class="bg-amber-600 text-white p-1.5 rounded-lg shadow-inner"><IconifyIcon icon="lucide:layers" class="text-xl" /></div>
        <span class="font-black text-lg tracking-tighter uppercase text-slate-800">WMS 杂项出入库工作台</span>
      </div>
      <div class="flex items-center gap-5">
        <Radio.Group v-model:value="miscMode" button-style="solid" @change="handleModeChange">
          <Radio.Button value="RECEIPT"><IconifyIcon icon="lucide:plus-circle" class="mr-1 inline-block"/> 杂项入账 (+)</Radio.Button>
          <Radio.Button value="ISSUE"><IconifyIcon icon="lucide:minus-circle" class="mr-1 inline-block"/> 杂发扣账 (-)</Radio.Button>
        </Radio.Group>
        <Button type="dashed" class="border-amber-200 text-amber-700 font-bold" @click="historyDrawerVisible = true">过账历史流水</Button>
        <div class="h-10 flex items-center bg-slate-50 px-2 rounded-xl border border-slate-200"><Avatar shape="square" :src="currentUser.avatar" size="small" /></div>
      </div>
    </header>

    <main class="flex-1 flex gap-3 p-3 overflow-hidden min-h-0">
      <div class="w-[420px] flex flex-col gap-3 min-h-0 shrink-0">

        <div class="bg-white border border-slate-200 rounded-lg shadow-sm p-4 shrink-0">
          <div class="text-xs font-bold text-slate-600 mb-3 flex items-center gap-2">
            <IconifyIcon icon="lucide:briefcase" class="text-amber-600"/> 业务归属与财务定性
          </div>
          <Form layout="vertical">
            <Row :gutter="12">
              <Col :span="12">
                <Form.Item label="成本归属中心" class="mb-3 font-bold text-slate-700">
                  <Select v-model:value="costCenter" class="w-full shadow-sm" :options="[{value:'DEPT_RD', label:'研发中心'}, {value:'DEPT_PROD', label:'生产一车间'}, {value:'DEPT_WH', label:'仓储物流部'}]"/>
                </Form.Item>
              </Col>
              <Col :span="12">
                <Form.Item label="杂项原因分类" class="mb-3 font-bold text-slate-700">
                  <Select v-model:value="reasonType" class="w-full shadow-sm" :options="reasonOptions"/>
                </Form.Item>
              </Col>
            </Row>
            <Form.Item label="备注说明 (必填)" required class="mb-0 font-bold text-slate-700">
              <Input.TextArea v-model:value="remarks" :rows="2" placeholder="请输入详细业务原因或关联特殊说明，以备审计核查..." class="shadow-sm border-slate-300" />
            </Form.Item>
          </Form>
        </div>

        <div class="bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col flex-1 overflow-hidden">
          <div class="p-3 border-b bg-amber-50 flex items-center gap-2"><span class="text-sm font-black text-amber-800 flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode" /> 库位锁定与扫码执行</span></div>
          <div class="flex-1 p-4 flex flex-col overflow-y-auto custom-scrollbar">
            <div class="mb-4">
              <div class="text-xs font-bold text-slate-600 mb-2">步骤 1: 扫描操作库位</div>
              <div v-if="!isLocationLocked" class="flex gap-2">
                <Input v-model:value="locationCode" placeholder="扫描库位条码" size="large" @pressEnter="handleLockLocation" class="border-amber-300 font-mono"><template #prefix><IconifyIcon icon="lucide:barcode" class="text-amber-500" /></template></Input>
                <Button type="primary" class="bg-amber-500 border-none font-bold" size="large" @click="handleLockLocation">锁定</Button>
              </div>
              <div v-else class="p-2 bg-amber-100 border border-amber-300 rounded flex justify-between items-center shadow-inner">
                <span class="font-mono font-bold text-amber-800 text-lg">{{ locationCode }}</span>
                <Button type="link" danger size="small" @click="isLocationLocked = false">解锁</Button>
              </div>
            </div>

            <Divider class="my-2" />

            <div :class="['p-4 rounded-lg border shadow-inner transition-all', isLocationLocked ? 'bg-amber-50 border-amber-200' : 'bg-slate-50 opacity-50 pointer-events-none']">
              <Form layout="vertical">
                <Form.Item label="当前条码包含数量" class="mb-3 font-bold text-slate-700"><InputNumber v-model:value="inputQty" size="large" class="w-full shadow-sm" :min="1" /></Form.Item>
                <Form.Item class="mb-0 mt-2">
                  <div class="text-[10px] text-slate-500 mb-1">扫描物料条码 (自动入账)</div>
                  <Input v-model:value="itemScanCode" size="large" class="font-mono border-amber-400 py-3 shadow-sm" placeholder="扫描实物..." @pressEnter="handleScanItemEnter"><template #prefix><IconifyIcon icon="lucide:barcode" class="text-amber-600 text-xl mr-2" /></template></Input>
                </Form.Item>
              </Form>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm flex flex-col">
        <div class="p-3 bg-slate-50 border-b text-xs font-bold text-slate-600 flex items-center justify-between">
          <div class="flex items-center gap-2"><IconifyIcon icon="lucide:list-checks" /> {{ miscMode === 'RECEIPT' ? '杂项入账明细表' : '杂发扣账明细表' }}</div>
        </div>
        <Table :columns="columns" :dataSource="scannedItems" :pagination="false" size="small" :scroll="{ y: 'calc(100vh - 190px)' }" class="vben-schema-table flex-1">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'barcode'"><span class="font-mono font-bold">{{ record.barcode }}</span></template>
            <template v-if="column.dataIndex === 'location'"><Tag color="orange" class="font-mono">{{ record.location }}</Tag></template>
            <template v-if="column.dataIndex === 'qty'"><span class="font-bold text-amber-600">{{ record.qty }}</span></template>
            <template v-if="column.dataIndex === 'action'"><Button type="text" danger size="small" @click="scannedItems = scannedItems.filter(b=>b.id!==record.id)">撤销</Button></template>
          </template>
          <template #emptyText>
            <div class="py-20 flex flex-col items-center text-slate-400"><IconifyIcon icon="lucide:scan-face" class="text-5xl mb-3 opacity-30" /><span>锁定库位后，进行杂项扫码。</span></div>
          </template>
        </Table>
        <div class="p-3 border-t bg-slate-50 flex justify-between items-center">
          <div class="text-xs text-slate-500">共计扫入 <span class="font-bold text-amber-600 text-base">{{ scannedItems.length }}</span> 笔</div>
          <Button type="primary" class="bg-amber-600 hover:bg-amber-500 border-none font-bold px-10 shadow-lg" size="large" :disabled="scannedItems.length === 0" @click="handleConfirmMisc">
            <IconifyIcon icon="lucide:saveAll" class="mr-2"/> 强制过账更新底账
          </Button>
        </div>
      </div>
    </main>

    <Drawer v-model:open="historyDrawerVisible" title="🧾 杂项流后台账记录" placement="right" width="400">
      <div v-for="log in historyLogs" :key="log.id" class="mb-3 border border-slate-200 rounded p-3 bg-white shadow-sm hover:border-amber-300 transition-colors">
        <div class="flex justify-between items-start mb-2"><span class="font-mono font-bold text-amber-700">{{ log.trxNo }}</span><Tag color="orange" class="!m-0">{{ log.mode }}</Tag></div>
        <div class="text-xs text-slate-600 mb-1">原因: <span class="font-bold text-slate-800">{{ log.reason }}</span></div>
        <div class="text-xs text-slate-600 mb-2">明细: <span class="font-bold text-amber-600">{{ log.itemCount }}</span> 笔</div>
        <div v-if="log.remarks" class="text-xs bg-amber-50 p-2 rounded border border-amber-100 text-amber-800 mt-1 mb-2 shadow-inner">
          <span class="font-bold opacity-70">备注说明：</span>{{ log.remarks }}
        </div>
        <div class="text-[10px] text-slate-400 pt-2 border-t flex justify-between items-center">
          <span>{{ log.time }}</span><span class="text-amber-600 font-bold">查看流水</span>
        </div>
      </div>
      <div v-if="historyLogs.length === 0" class="text-center text-slate-400 py-10">暂无杂项历史过账记录</div>
    </Drawer>
  </div>
</template>
<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-input-affix-wrapper-focused) { box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.2) !important; border-color: #d97706 !important; }
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 11px; padding: 6px 8px !important; border-bottom: 1px solid #e2e8f0;}
.vben-schema-table :deep(.ant-table-cell) { padding: 8px !important; font-size: 12px; }
</style>
