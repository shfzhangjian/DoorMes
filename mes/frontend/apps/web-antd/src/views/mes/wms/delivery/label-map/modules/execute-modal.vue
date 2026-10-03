<script lang="ts" setup>
import { ref, nextTick } from 'vue';
import { useVbenModal, confirm } from '@vben/common-ui';
import { Card, Input, Select, Button, message, Table, Tag, Descriptions, Divider } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);

const selectedRule = ref('CATL-STD');
const scanInput = ref('');
const inputRef = ref();

const ruleOptions = [
  { label: '宁德时代标准箱码规则 (CATL-V001-日期-流水)', value: 'CATL-STD' },
  { label: '比亚迪电芯膜码规则 (BYD-M-日期-流水)', value: 'BYD-STD' }
];

// 待置换清单
const mappedList = ref<any[]>([]);

const columns = [
  { title: '工厂内部原码', dataIndex: 'internalSn', width: 160 },
  { title: '解析物料信息', dataIndex: 'productName', width: 140 },
  { title: '映射生成的客户新码', dataIndex: 'clientSn', width: 200 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

// 核心：防错扫码与规则映射引擎
function handleScan() {
  const code = scanInput.value.trim();
  if (!code) return;
  if (!selectedRule.value) {
    message.warning('请先在左侧选择客户标签置换规则！');
    return;
  }
  if (mappedList.value.some(item => item.internalSn === code)) {
    message.error(`条码 ${code} 已在置换队列中！`);
    scanInput.value = '';
    return;
  }

  // 💡 模拟底层规则引擎进行条码解析与生成
  let newSn = '';
  const dateStr = new Date().toISOString().substring(2, 10).replace(/-/g, ''); // 260219
  const serial = String(mappedList.value.length + 1).padStart(4, '0');

  if (selectedRule.value === 'CATL-STD') {
    newSn = `CATL-V001-${dateStr}-${serial}`;
  } else {
    newSn = `BYD-M-${dateStr}-${serial}`;
  }

  mappedList.value.unshift({
    id: Date.now(),
    internalSn: code,
    productName: code.includes('001') ? '光学级PET树脂' : '特种交联剂',
    clientSn: newSn,
    status: 'PENDING'
  });

  message.success('置换解析成功！');
  scanInput.value = '';
  nextTick(() => inputRef.value?.focus());
}

function handleRemove(id: number) {
  mappedList.value = mappedList.value.filter(item => item.id !== id);
}

// 弹窗控制器
const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true,
  title: '🖨️ 客户专属标签置换与发货打印工作台',
  onOpenChange(isOpen) {
    if (isOpen) {
      mappedList.value = [];
      scanInput.value = '';
      nextTick(() => inputRef.value?.focus());
    }
  },
  onConfirm() {
    if (mappedList.value.length === 0) {
      message.warning('没有可执行置换的条码明细！');
      return;
    }
    confirm({
      title: '确认提交并驱动打印机？',
      content: `系统将在后台建立 ${mappedList.value.length} 组条码映射关系，并驱动现场打印机输出客户专属标签。`,
      onOk: () => {
        message.success('置换关系已建立！打印机正在输出...');
        emit('success');
        modalApi.close();
      }
    });
  }
});
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden">

      <div class="w-[320px] flex flex-col gap-4 shrink-0">
        <Card size="small" class="border-none shadow-sm rounded-lg" title="1. 选定发货客户规则">
          <Select v-model:value="selectedRule" :options="ruleOptions" size="large" class="w-full" />
          <div class="mt-4 p-3 bg-blue-50 border border-blue-100 rounded text-xs text-blue-700 leading-relaxed">
            <IconifyIcon icon="lucide:info" class="mr-1"/>
            此规则将决定新条码的前缀、日期格式及流水号长度。选定后，系统将自动对内部码进行解绑重组。
          </div>
        </Card>

        <Card size="small" class="border-none shadow-sm rounded-lg flex-1">
          <template #title><span class="text-indigo-700 font-bold flex items-center gap-2"><IconifyIcon icon="lucide:scan-barcode"/> 2. 扫入待换装实物</span></template>
          <Input ref="inputRef" v-model:value="scanInput" size="large" placeholder="请扫描工厂内部原条码" class="font-mono text-lg border-indigo-200" @pressEnter="handleScan">
            <template #prefix><IconifyIcon icon="lucide:barcode" class="text-indigo-400 mr-2" /></template>
          </Input>

          <Divider class="my-4 border-slate-100" />

          <Descriptions size="small" :column="1" layout="vertical" class="opacity-70">
            <Descriptions.Item label="今日已置换量"><span class="font-bold text-slate-800 text-lg">1,208</span> 签</Descriptions.Item>
            <Descriptions.Item label="当前在线打印机"><span class="text-green-600 font-bold">PRT-LINE-02 (就绪)</span></Descriptions.Item>
          </Descriptions>
        </Card>
      </div>

      <Card size="small" class="flex-1 border-none shadow-sm rounded-lg overflow-hidden flex flex-col" :bodyStyle="{ padding: 0, flex: 1, display: 'flex', flexDirection: 'column' }">
        <div class="p-3 bg-slate-50 border-b border-slate-100 flex justify-between items-center shrink-0">
          <span class="font-bold text-slate-700">3. 映射结果台账 (Mapping Preview)</span>
          <Tag color="blue" class="!m-0 border-none font-bold">待打印数: {{ mappedList.length }}</Tag>
        </div>

        <div class="flex-1 overflow-y-auto p-2">
          <Table :columns="columns" :dataSource="mappedList" :pagination="false" size="small" class="vben-schema-table">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'internalSn'">
                <span class="font-mono text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded text-xs">{{ record.internalSn }}</span>
              </template>
              <template v-if="column.dataIndex === 'clientSn'">
                <span class="font-mono font-bold text-indigo-700 bg-indigo-50 px-2 py-1 rounded border border-indigo-100 shadow-sm">{{ record.clientSn }}</span>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Button type="text" danger size="small" @click="handleRemove(record.id)">移除</Button>
              </template>
            </template>
            <template #emptyText>
              <div class="py-16 text-slate-400 flex flex-col items-center">
                <IconifyIcon icon="lucide:arrow-right-left" class="text-4xl mb-2 opacity-30" />
                <span>左侧扫码后，此处将生成客户新码预览</span>
              </div>
            </template>
          </Table>
        </div>
      </Card>

    </div>
  </Modal>
</template>

<style scoped>
.vben-schema-table :deep(.ant-table-thead > tr > th) { background: #f8fafc !important; font-size: 12px; font-weight: bold; }
.vben-schema-table :deep(.ant-table-cell) { font-size: 13px; padding: 10px !important; }
</style>
