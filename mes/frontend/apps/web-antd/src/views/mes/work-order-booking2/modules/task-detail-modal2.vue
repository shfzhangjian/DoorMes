<script lang="ts" setup>
import { ref } from 'vue';
import { Button, Tabs, TabPane, Tag, message, Table, Input, Switch, InputNumber, RadioGroup, RadioButton, Select, Modal as AModal, Form, FormItem, Descriptions, DescriptionsItem } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import dayjs from 'dayjs';

const emit = defineEmits(['refresh']);
const activeMainTab = ref('detail');
const task = ref<any>(null);
const viewMode = ref<'tabs' | 'booking'>('tabs');

// ==================== 1. 底部子 Tab 状态控制 ====================
const activeCheckTab = ref('input');
const activeBomTab = ref('scan');
const activeQualityTab = ref('fai');

// ==================== 2. 授权与全屏历史详情 ====================
const authModalVisible = ref(false);
const authActionType = ref('');
const authForm = ref({ cardNo: '', password: '' });

const detailModalVisible = ref(false);
const detailTitle = ref('');
const detailColumns = ref<any[]>([]);
const detailData = ref<any[]>([]);

const openAuth = (type: string) => {
  authActionType.value = type;
  authForm.value = { cardNo: '', password: '' };
  authModalVisible.value = true;
};

const handleAuthConfirm = () => {
  if (!authForm.value.cardNo) return message.warning('请输入员工工号以确认授权！');
  const operator = authForm.value.cardNo;
  const time = dayjs().format('YYYY-MM-DD HH:mm:ss');

  if (authActionType.value === 'check') {
    const currentDetails = checkList.value.map(item => ({
      item: item.item,
      actual: item.type === 'check' ? (item.actual ? '合格' : '异常') : item.actual
    }));
    checkHistory.value.unshift({ id: Date.now(), operator, time, status: 'OK', details: currentDetails });
    message.success('设备点检与参数记录签核成功！');
    activeCheckTab.value = 'history';
  } else if (authActionType.value === 'bom') {
    const currentDetails = bomList.value.map(item => ({ name: item.name, requireQty: item.requireQty, actualQty: item.actualQty }));
    bomHistory.value.unshift({ id: Date.now(), operator, time, action: '完成物料投料防错核对', details: currentDetails });
    message.success('物料防错核对签核成功！');
    activeBomTab.value = 'history';
  }
  authModalVisible.value = false;
};

const viewHistoryDetail = (record: any, type: 'check' | 'bom') => {
  if (type === 'check') {
    detailTitle.value = `点检与参数详情 [签核人: ${record.operator} | 时间: ${record.time}]`;
    detailColumns.value = [
      { title: '检查/参数项目', dataIndex: 'item' },
      { title: '实际确认值', dataIndex: 'actual', width: 250 }
    ];
  } else {
    detailTitle.value = `投料防错核对详情 [签核人: ${record.operator} | 时间: ${record.time}]`;
    detailColumns.value = [
      { title: '物料名称', dataIndex: 'name' },
      { title: '标准需求', dataIndex: 'requireQty', width: 200 },
      { title: '实际扫入', dataIndex: 'actualQty', width: 200 }
    ];
  }
  detailData.value = record.details || [];
  detailModalVisible.value = true;
};

// ==================== 3. 业务视图数据 ====================

// 🌟 补充的开工详情表单
const startForm = ref({
  planQty: 0,
  spec: '',
  batchNo: '',
  shift: '早班',
  operator: '张师傅 (8801)',
  remark: ''
});

const startTask = () => {
  if (task.value) {
    task.value.status = 'IN_PROGRESS';
    task.value.startTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    message.success('已确认开工！系统记录作业状态。');
  }
};

// 🌟 修复：必须加上 key 属性，否则 Antdv 的 #bodyCell 无法识别 column.key
const checkColumns = [
  { title: '检查/参数项目', dataIndex: 'item', key: 'item' },
  { title: '工艺标准要求', dataIndex: 'standard', key: 'standard', width: 180 },
  { title: '实测状态 / 数值录入', dataIndex: 'actual', key: 'actual', width: 280 }
];
const checkList = ref([
  { id: 1, type: 'check', item: '开机前5S清扫/劳保穿戴', standard: '符合规范', actual: false },
  { id: 2, type: 'input', item: '主轴气压 (MPa)', standard: '0.5 ~ 0.7', actual: null, min: 0.5, max: 0.7 },
  { id: 3, type: 'input', item: '槽液浓度 (%)', standard: '5.0 ~ 8.0', actual: null, min: 5.0, max: 8.0 }
]);
const checkHistory = ref<any[]>([
  { id: 1, operator: '8801', time: dayjs().subtract(2, 'hour').format('YYYY-MM-DD HH:mm:ss'), status: 'OK', details: [{item: '主轴气压 (MPa)', actual: '0.6'}] }
]);
const checkHistoryColumns = [
  { title: '确认时间', dataIndex: 'time', key: 'time' },
  { title: '操作员', dataIndex: 'operator', key: 'operator' },
  { title: '判定', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120, align: 'center' }
];

const bomScanInput = ref('');
const bomList = ref([
  { id: 'RM-001', name: '特种基材', requireQty: 100, unit: 'm', actualQty: 0, status: '待扫码' },
  { id: 'RM-002', name: '处理液A', requireQty: 15, unit: 'L', actualQty: 0, status: '待扫码' }
]);
const bomHistory = ref<any[]>([]);
const bomHistoryColumns = [
  { title: '签核时间', dataIndex: 'time', key: 'time' },
  { title: '操作员', dataIndex: 'operator', key: 'operator' },
  { title: '执行动作', dataIndex: 'action', key: 'action' },
  { title: '操作', key: 'action', width: 120, align: 'center' }
];

const handleBomScan = () => {
  if (!bomScanInput.value) return;
  const target = bomList.value.find(item => item.status === '待扫码');
  if (target) {
    target.actualQty = target.requireQty;
    target.status = '已齐套';
    message.success(`扫码成功: ${target.name} 防错通过！`);
  } else {
    message.warning(`当前工序无需补充新物料，或条码不匹配！`);
  }
  bomScanInput.value = '';
};

// ==================== 4. 报工表单与单件自检 ====================
const bookingMode = ref<'BATCH' | 'PIECE'>('BATCH');

// 🌟 扩展的完工报工表单数据
const bookingForm = ref({
  goodQty: 0,
  scrapQty: 0,
  reworkQty: 0,
  laborHours: 0, // 新增人工工时
  scrapReason: undefined,
  nextProcess: '包装入库',
  startTime: '',
  endTime: '',
  remark: ''
});

const barcodeList = ref<{sn: string, time: string, status: 'OK'|'NG', reason?: string, remark: string, inspectData?: any[]}[]>([]);
const pieceScanInput = ref('');

// 单件自检弹窗状态
const pieceInspectModalVisible = ref(false);
const currentPieceIdx = ref(-1);
const currentPieceInspectData = ref<any[]>([]);

const generateBarcode = () => {
  barcodeList.value.unshift({ sn: `SN${Date.now().toString().slice(-8)}`, time: dayjs().format('HH:mm:ss'), status: 'OK', remark: '' });
};
const scanPieceCode = () => {
  if (!pieceScanInput.value) return;
  barcodeList.value.unshift({ sn: pieceScanInput.value.toUpperCase(), time: dayjs().format('HH:mm:ss'), status: 'OK', remark: '' });
  pieceScanInput.value = '';
};
const removePiece = (index: number) => barcodeList.value.splice(index, 1);

const openPieceInspect = (index: number) => {
  currentPieceIdx.value = index;
  const piece = barcodeList.value[index];
  if (piece.inspectData) {
    currentPieceInspectData.value = JSON.parse(JSON.stringify(piece.inspectData));
  } else {
    currentPieceInspectData.value = [
      { id: 1, item: '外观无划痕/破损', type: 'check', actual: true },
      { id: 2, item: '单件厚度 (mm)', type: 'input', standard: '0.15 ~ 0.20', actual: null }
    ];
  }
  pieceInspectModalVisible.value = true;
};

const savePieceInspect = () => {
  barcodeList.value[currentPieceIdx.value].inspectData = currentPieceInspectData.value;
  message.success(`条码 [${barcodeList.value[currentPieceIdx.value].sn}] 自检参数已保存！`);
  pieceInspectModalVisible.value = false;
};

const submitBooking = () => {
  let finalGood = 0;
  let finalScrap = 0;

  if (bookingMode.value === 'BATCH') {
    finalGood = bookingForm.value.goodQty;
    finalScrap = bookingForm.value.scrapQty;
    if (finalScrap > 0 && !bookingForm.value.scrapReason) {
      return message.error('防呆拦截：批量模式下存在不良数，必须选择不良原因！');
    }
  } else {
    finalGood = barcodeList.value.filter(item => item.status === 'OK').length;
    finalScrap = barcodeList.value.filter(item => item.status === 'NG').length;
    const hasUnexplainedNG = barcodeList.value.some(item => item.status === 'NG' && !item.reason);
    if (hasUnexplainedNG) return message.error('防呆拦截：存在被标记为【NG】的单件未选择不良原因！');
  }

  if (finalGood + finalScrap + bookingForm.value.reworkQty <= 0) return message.warning('报工总产出(含良品/次品/返工)不能为0！');

  message.success(`✅ 报工过站成功！良品: ${finalGood}，不良: ${finalScrap}，返修: ${bookingForm.value.reworkQty}，投入工时: ${bookingForm.value.laborHours}H`);

  // 隐藏本身
  modalApi.close();
  emit('refresh');
};

const handleQualityInspect = (type: string) => message.success(`✅ 已向质检部提报【${type}】申请！`);

const [Modal, modalApi] = useVbenModal({
  title: '生产现场终端作业台 (SFC)',
  width: 1200,
  footer: false,
  onOpenChange(isOpen) {
    if (isOpen) {
      task.value = modalApi.getData<any>();
      viewMode.value = 'tabs';
      activeMainTab.value = 'detail';
      activeCheckTab.value = 'input';
      activeBomTab.value = 'scan';
      activeQualityTab.value = 'fai';

      // 初始化表单数据
      startForm.value.planQty = task.value.planQty;
      startForm.value.spec = task.value.spec || '标准公差版';
      startForm.value.batchNo = `BAT-${dayjs().format('YYYYMMDD')}-001`;

      bookingForm.value.goodQty = task.value.planQty - task.value.goodQty;
      bookingForm.value.scrapQty = 0;
      bookingForm.value.reworkQty = 0;
      bookingForm.value.laborHours = 8;
      bookingForm.value.startTime = task.value.startTime || dayjs().format('YYYY-MM-DD HH:mm:ss');
      bookingForm.value.endTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    }
  }
});
</script>

<template>
  <Modal>
    <div class="flex flex-col bg-slate-50 relative -m-4" style="height: 75vh; min-height: 600px;">

      <div class="h-16 bg-white flex items-center justify-between px-6 shrink-0 border-b border-slate-200 z-10 shadow-sm">
        <div class="flex items-center gap-6">
          <div class="flex flex-col leading-tight">
            <span class="text-[11px] text-slate-400 font-bold uppercase">执行工单 / Work Order</span>
            <span class="text-lg font-black text-indigo-700 font-mono tracking-wide">{{ task?.id }}</span>
          </div>
          <div class="h-8 w-px bg-slate-200"></div>
          <div class="flex flex-col leading-tight">
            <span class="text-[11px] text-slate-400 font-bold uppercase">加工产品 | 当前工序</span>
            <span class="text-base font-bold text-slate-800">
              {{ task?.product }}
              <Tag color="blue" class="ml-2 !m-0 font-bold">{{ task?.process }}</Tag>
            </span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <template v-if="viewMode === 'tabs'">
            <Button size="large" type="primary" class="bg-blue-600 hover:bg-blue-500 font-bold px-6 shadow-sm border-none" @click="startTask" :disabled="task?.status === 'IN_PROGRESS'">
              <IconifyIcon icon="lucide:play" class="mr-1"/> {{ task?.status === 'IN_PROGRESS' ? '已开工生产中' : '执行开工确认' }}
            </Button>
            <Button size="large" type="primary" class="bg-emerald-600 hover:bg-emerald-500 font-bold px-6 shadow-md border-none" @click="viewMode = 'booking'">
              <IconifyIcon icon="lucide:check-square" class="mr-1"/> 结束完工与报工
            </Button>
          </template>

          <template v-else-if="viewMode === 'booking'">
            <Button size="large" class="font-bold border-slate-300" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1"/> 取消返回
            </Button>
            <Button size="large" type="primary" class="w-48 font-black text-lg bg-emerald-600 hover:bg-emerald-500 border-none shadow-lg" @click="submitBooking">
              确认提交报工
            </Button>
          </template>
        </div>
      </div>

      <div v-if="viewMode === 'tabs'" class="flex-1 flex flex-col p-4 min-h-0 bg-slate-100">
        <div class="bg-white flex-1 min-h-0 rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden p-2">
          <Tabs v-model:activeKey="activeMainTab" class="h-full custom-main-tabs flex flex-col">

            <TabPane key="detail" tab="📄 工单详情与开工" class="h-full">
              <div class="h-full flex flex-col bg-slate-50 p-6 overflow-y-auto min-h-0">

                <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm mb-6 shrink-0">
                  <div class="text-lg font-black text-slate-800 border-b border-slate-100 pb-3 mb-4 flex items-center">
                    <IconifyIcon icon="lucide:file-text" class="mr-2 text-blue-500"/> 生产工单明细
                  </div>
                  <Descriptions bordered size="small" :column="2" class="custom-desc">
                    <DescriptionsItem label="生产工单号"><span class="font-mono font-bold">{{ task?.id }}</span></DescriptionsItem>
                    <DescriptionsItem label="关联计划号">{{ task?.planNo || '-' }}</DescriptionsItem>
                    <DescriptionsItem label="产品名称"><span class="text-indigo-600 font-bold">{{ task?.product }}</span></DescriptionsItem>
                    <DescriptionsItem label="工单状态">
                      <Tag :color="task?.status === 'IN_PROGRESS' ? 'processing' : task?.status === 'COMPLETED' ? 'success' : 'default'">{{ task?.status === 'IN_PROGRESS' ? '生产中' : task?.status === 'COMPLETED' ? '已完工' : '待开工' }}</Tag>
                    </DescriptionsItem>
                  </Descriptions>
                </div>

                <div class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm flex-1 flex flex-col min-h-0 shrink-0">
                  <div class="text-lg font-black text-slate-800 border-b border-slate-100 pb-3 mb-4 flex items-center shrink-0">
                    <IconifyIcon icon="lucide:play-circle" class="mr-2 text-emerald-500"/> 现场开工信息录入
                  </div>
                  <Form layout="vertical" class="flex-1 flex flex-col">
                    <div class="grid grid-cols-3 gap-6 shrink-0 mb-2">
                      <FormItem label="计划生产数量 (PCS)">
                        <InputNumber v-model:value="startForm.planQty" class="w-full font-bold text-lg" disabled />
                      </FormItem>
                      <FormItem label="生产产品规格">
                        <Input v-model:value="startForm.spec" class="w-full font-bold text-slate-600 bg-slate-50" />
                      </FormItem>
                      <FormItem label="生成的生产批次号">
                        <Input v-model:value="startForm.batchNo" class="w-full font-mono font-bold text-indigo-600 bg-indigo-50/50" />
                      </FormItem>
                    </div>
                    <div class="grid grid-cols-2 gap-6 shrink-0 mb-2">
                      <FormItem label="当班班次">
                        <Select v-model:value="startForm.shift" :options="[{label:'早班',value:'早班'},{label:'中班',value:'中班'},{label:'晚班',value:'晚班'}]" />
                      </FormItem>
                      <FormItem label="开工责任人">
                        <Input v-model:value="startForm.operator" disabled class="bg-slate-50 text-slate-500 font-bold" />
                      </FormItem>
                    </div>
                    <FormItem label="开工前交接备注说明" class="flex-1 flex flex-col mb-4 w-full full-height-form-item">
                      <Input.TextArea v-model:value="startForm.remark" class="w-full h-full resize-none bg-slate-50 p-3 rounded-lg border-slate-300" placeholder="如有上班次遗留问题或设备特殊情况请在此备注..." />
                    </FormItem>
                  </Form>
                </div>
              </div>
            </TabPane>

            <TabPane key="check" tab="🛠️ 设备点检与工艺参数" class="h-full">
              <div class="h-full flex flex-col bg-slate-50">
                <Tabs v-model:activeKey="activeCheckTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
                  <TabPane key="input" tab="📝 生产实测录入">
                    <div class="p-6 h-full flex flex-col min-h-0">
                      <div class="flex justify-between items-center mb-4 shrink-0">
                        <span class="font-bold text-slate-700 text-lg">待检项目与参数标准管控 ({{ checkList.length }})</span>
                        <Button type="primary" size="large" class="bg-blue-600 font-bold border-none shadow-sm" @click="openAuth('check')">
                          <IconifyIcon icon="lucide:pen-tool" class="mr-1"/> 签核提交
                        </Button>
                      </div>
                      <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
                        <Table :columns="checkColumns" :dataSource="checkList" :pagination="false" size="middle" class="w-full">
                          <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'actual'">
                              <Switch v-if="record.type === 'check'" v-model:checked="record.actual" checked-children="合规" un-checked-children="异常" class="bg-slate-300" />
                              <InputNumber v-else v-model:value="record.actual" class="w-full font-mono text-indigo-600 font-bold" placeholder="点检录入" />
                            </template>
                          </template>
                        </Table>
                      </div>
                    </div>
                  </TabPane>

                  <TabPane key="history" tab="🕰️ 历史确认查询">
                    <div class="p-6 h-full flex flex-col min-h-0">
                      <div class="font-bold text-slate-700 mb-4 text-lg shrink-0">点检与工艺参数确认履历台账</div>
                      <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
                        <Table :columns="checkHistoryColumns" :dataSource="checkHistory" :pagination="false" size="middle">
                          <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'status'"><Tag color="success" class="!m-0">OK</Tag></template>
                            <template v-if="column.key === 'action'">
                              <Button type="link" size="small" class="font-bold" @click="viewHistoryDetail(record, 'check')">查看详情</Button>
                            </template>
                          </template>
                        </Table>
                      </div>
                    </div>
                  </TabPane>
                </Tabs>
              </div>
            </TabPane>

            <TabPane key="bom" tab="📦 投料防错与核对" class="h-full">
              <div class="h-full flex flex-col bg-slate-50">
                <Tabs v-model:activeKey="activeBomTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
                  <TabPane key="scan" tab="🔫 扫码匹配投料">
                    <div class="p-6 h-full flex flex-col gap-4 min-h-0">
                      <div class="flex gap-4 shrink-0">
                        <Input.Search v-model:value="bomScanInput" placeholder="使用扫码枪扫描物料批次条码进行防错核对..." size="large" enter-button="核对投料" @search="handleBomScan" class="flex-1 custom-huge-input" />
                        <Button size="large" type="primary" class="w-48 font-bold bg-indigo-600 border-none shadow-sm" @click="openAuth('bom')">
                          <IconifyIcon icon="lucide:check-circle" class="mr-1"/> 完成核对签核
                        </Button>
                      </div>
                      <div class="flex-1 min-h-0 overflow-y-auto bg-white border border-slate-200 rounded-lg custom-table-wrapper">
                        <table class="w-full text-left border-collapse">
                          <thead class="bg-slate-100 text-slate-600 sticky top-0 z-10 shadow-sm">
                          <tr><th class="p-3 border-b">物料名称</th><th class="p-3 border-b">工艺需求数量</th><th class="p-3 border-b">实际扫入匹配量</th><th class="p-3 border-b text-center">防错状态</th></tr>
                          </thead>
                          <tbody>
                          <tr v-for="mat in bomList" :key="mat.id" class="hover:bg-slate-50 transition-colors">
                            <td class="p-3 border-b font-bold text-slate-700">{{ mat.name }} <span class="text-xs text-slate-400 block mt-0.5 font-mono">{{ mat.id }}</span></td>
                            <td class="p-3 border-b font-mono font-bold">{{ mat.requireQty }} {{ mat.unit }}</td>
                            <td class="p-3 border-b font-mono text-blue-600 font-black text-lg">{{ mat.actualQty }} <span class="text-sm font-normal">{{ mat.unit }}</span></td>
                            <td class="p-3 border-b text-center"><Tag :color="mat.status === '已齐套' ? 'success' : 'warning'" class="font-bold px-3 py-1">{{ mat.status }}</Tag></td>
                          </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </TabPane>

                  <TabPane key="history" tab="🕰️ 投料核对履历">
                    <div class="p-6 h-full flex flex-col min-h-0">
                      <div class="font-bold text-slate-700 mb-4 text-lg shrink-0">物料投料与防错核对历史</div>
                      <div class="flex-1 min-h-0 overflow-y-auto bg-white rounded-lg border border-slate-200 custom-table-wrapper">
                        <Table :columns="bomHistoryColumns" :dataSource="bomHistory" :pagination="false" size="middle">
                          <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'action'">
                              <Button type="link" size="small" class="font-bold" @click="viewHistoryDetail(record, 'bom')">查看详情</Button>
                            </template>
                          </template>
                        </Table>
                      </div>
                    </div>
                  </TabPane>
                </Tabs>
              </div>
            </TabPane>

            <TabPane key="quality" tab="🔬 质量管控提报" class="h-full">
              <div class="h-full flex flex-col bg-slate-50">
                <Tabs v-model:activeKey="activeQualityTab" tabPosition="bottom" class="h-full custom-bottom-tabs flex flex-col">
                  <TabPane key="fai" tab="首件检验 (FAI)">
                    <div class="h-full flex flex-col items-center justify-center bg-indigo-50/30">
                      <IconifyIcon icon="lucide:shield-check" class="text-[80px] text-indigo-300 mb-4 drop-shadow-sm" />
                      <div class="text-2xl font-black text-slate-700 mb-2">申请品质部进行首件判定</div>
                      <div class="text-slate-500 mb-8">前段产出的首件必须检验合格方可继续量产作业。</div>
                      <Button type="primary" size="large" class="bg-indigo-600 font-bold h-14 text-lg px-8 border-none shadow-md" @click="handleQualityInspect('首件检验')">一键下发首检任务</Button>
                    </div>
                  </TabPane>
                  <TabPane key="ipqc" tab="过程抽检 (IPQC)">
                    <div class="h-full flex flex-col items-center justify-center bg-cyan-50/30">
                      <IconifyIcon icon="lucide:scan-search" class="text-[80px] text-cyan-300 mb-4 drop-shadow-sm" />
                      <div class="text-2xl font-black text-slate-700 mb-8">制程突发异常，呼叫过程巡检</div>
                      <Button type="primary" size="large" class="bg-cyan-600 border-none font-bold h-14 text-lg px-8 shadow-md" @click="handleQualityInspect('过程抽检')">呼叫巡检 IPQC</Button>
                    </div>
                  </TabPane>
                  <TabPane key="fqc" tab="成品出厂检验 (FQC)">
                    <div class="h-full flex flex-col items-center justify-center bg-emerald-50/30">
                      <IconifyIcon icon="lucide:package-check" class="text-[80px] text-emerald-300 mb-4 drop-shadow-sm" />
                      <div class="text-2xl font-black text-slate-700 mb-8">完工交接入库前成品全检/抽检</div>
                      <Button type="primary" size="large" class="bg-emerald-600 border-none font-bold h-14 text-lg px-8 shadow-md" @click="handleQualityInspect('成品检验')">申请出厂质检</Button>
                    </div>
                  </TabPane>
                </Tabs>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </div>

      <div v-else-if="viewMode === 'booking'" class="flex-1 flex flex-col overflow-hidden bg-slate-100 animate-fade-in z-20">
        <div class="flex-1 min-h-0 overflow-hidden p-6 flex gap-6 bg-slate-50">

          <div class="w-[260px] shrink-0 bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col h-full">
            <h3 class="font-black text-slate-800 border-b border-slate-100 pb-3 mb-5 flex items-center shrink-0">
              <IconifyIcon icon="lucide:file-bar-chart-2" class="mr-2 text-indigo-500" /> 计划达成看板
            </h3>
            <div class="space-y-5 flex-1 min-h-0 overflow-y-auto pr-1">
              <div class="bg-slate-50 p-3 rounded-lg border border-slate-100">
                <div class="text-slate-400 text-xs font-bold mb-1">目标指令总数</div>
                <div class="text-2xl font-black font-mono text-slate-700">{{ task?.planQty }} <span class="text-sm font-normal text-slate-500">PCS</span></div>
              </div>
              <div class="bg-emerald-50 p-3 rounded-lg border border-emerald-100">
                <div class="text-emerald-600/70 text-xs font-bold mb-1">已累计良品产出</div>
                <div class="text-2xl font-black font-mono text-emerald-600">{{ task?.goodQty }} <span class="text-sm font-normal text-emerald-500">PCS</span></div>
              </div>
              <div class="bg-slate-50 p-3 rounded-lg border border-slate-100 mt-auto">
                <div class="text-slate-500 font-bold mb-1 text-xs">当前状态</div>
                <div class="font-bold">{{ task?.status === 'IN_PROGRESS' ? '生产中' : '已暂停/未开工' }}</div>
              </div>
            </div>
          </div>

          <div class="flex-1 flex flex-col h-full min-h-0 bg-white border border-slate-200 p-5 rounded-xl shadow-sm">
            <div class="flex items-center justify-between mb-4 shrink-0 pb-3 border-b border-slate-100">
              <span class="font-bold text-slate-700 flex items-center text-lg">
                <IconifyIcon icon="lucide:layers" class="mr-2 text-blue-500" /> 产出采集模式
              </span>
              <RadioGroup v-model:value="bookingMode" button-style="solid" size="middle">
                <RadioButton value="BATCH">📊 整体批量报工</RadioButton>
                <RadioButton value="PIECE">🏷️ 单件防伪扫码 (附自检)</RadioButton>
              </RadioGroup>
            </div>

            <Form v-if="bookingMode === 'BATCH'" layout="vertical" class="flex-1 flex flex-col min-h-0 w-full">
              <div class="grid grid-cols-4 gap-6 shrink-0 w-full mb-2">
                <FormItem label="良品产出数 (PCS)">
                  <InputNumber v-model:value="bookingForm.goodQty" :min="0" class="w-full text-emerald-600 font-black text-2xl h-12 text-center custom-huge-input" />
                </FormItem>
                <FormItem label="不良报废数 (PCS)">
                  <InputNumber v-model:value="bookingForm.scrapQty" :min="0" class="w-full text-red-500 font-black text-2xl h-12 text-center custom-huge-input" />
                </FormItem>
                <FormItem label="可返修数量 (PCS)">
                  <InputNumber v-model:value="bookingForm.reworkQty" :min="0" class="w-full text-amber-500 font-black text-2xl h-12 text-center custom-huge-input" />
                </FormItem>
                <FormItem label="投入人工工时 (H)">
                  <InputNumber v-model:value="bookingForm.laborHours" :min="0" :step="0.5" class="w-full text-indigo-600 font-black text-2xl h-12 text-center custom-huge-input" />
                </FormItem>
              </div>

              <div class="grid grid-cols-2 gap-6 shrink-0 w-full mb-2">
                <FormItem label="强制必填：次品原因" required>
                  <Select v-model:value="bookingForm.scrapReason" class="w-full" size="large" :options="[{label:'尺寸超差',value:'R1'}, {label:'表面划伤',value:'R2'}]" :disabled="bookingForm.scrapQty === 0" />
                </FormItem>
                <FormItem label="转入下道工序配置">
                  <Select v-model:value="bookingForm.nextProcess" class="w-full font-bold" size="large" :options="[{label:'包装入库',value:'包装入库'}, {label:'返修站',value:'返修站'}]"/>
                </FormItem>
              </div>

              <div class="grid grid-cols-2 gap-6 shrink-0 w-full mb-2">
                <FormItem label="实际开工时间">
                  <Input v-model:value="bookingForm.startTime" size="large" class="font-mono bg-slate-50" readonly />
                </FormItem>
                <FormItem label="实际完工时间">
                  <Input v-model:value="bookingForm.endTime" size="large" class="font-mono bg-slate-50" readonly />
                </FormItem>
              </div>

              <FormItem label="生产异常与交班备注说明" class="flex-1 flex flex-col mb-0 w-full full-height-form-item">
                <Input.TextArea v-model:value="bookingForm.remark" class="w-full h-full resize-none bg-slate-50 text-base p-3 rounded-lg border-slate-300" placeholder="请详细记录生产过程中的工装异常、物料差异等信息，此记录将随批次流转..." />
              </FormItem>
            </Form>

            <div v-if="bookingMode === 'PIECE'" class="flex-1 flex flex-col min-h-0 w-full">
              <div class="flex gap-4 mb-4 shrink-0">
                <Input.Search v-model:value="pieceScanInput" placeholder="在此扫入产品条码完成下线记录..." size="large" enter-button="扫码记录" @search="scanPieceCode" class="flex-1" />
                <Button type="default" size="large" class="font-bold border-slate-300" @click="generateBarcode">
                  <IconifyIcon icon="lucide:sparkles" class="mr-1 text-amber-500"/> 生成流水码
                </Button>
              </div>

              <div class="flex-1 border border-slate-200 rounded-lg overflow-y-auto bg-slate-50 p-2 flex flex-col relative">
                <div v-if="barcodeList.length === 0" class="absolute inset-0 flex flex-col items-center justify-center text-slate-400">
                  <IconifyIcon icon="lucide:scan-barcode" class="text-6xl mb-3 opacity-30" />
                  <span class="font-bold">等待单件条码扫入...</span>
                </div>

                <div v-for="(code, idx) in barcodeList" :key="idx" class="bg-white p-3 rounded-lg mb-2 border border-slate-200 flex flex-col shadow-sm hover:border-indigo-300 transition-colors gap-2 shrink-0">
                  <div class="flex justify-between items-center border-b border-slate-100 pb-2">
                    <div class="flex items-center gap-3">
                      <span class="bg-slate-200 text-slate-600 font-bold px-2 py-0.5 rounded text-xs">{{ barcodeList.length - idx }}</span>
                      <span class="font-mono font-black text-lg text-slate-700 tracking-wide">{{ code.sn }}</span>
                    </div>
                    <div class="flex items-center gap-3">
                      <span class="text-xs text-slate-400 font-mono">{{ code.time }}</span>
                      <Button type="text" danger class="px-2 h-6" @click="removePiece(idx)" title="移除此件">
                        <IconifyIcon icon="lucide:trash-2" class="text-lg"/>
                      </Button>
                    </div>
                  </div>
                  <div class="flex items-center gap-3 w-full">
                    <Button size="small" :type="code.inspectData ? 'primary' : 'default'" class="font-bold shrink-0" @click="openPieceInspect(idx)">
                      <IconifyIcon icon="lucide:clipboard-edit" class="mr-1" /> {{ code.inspectData ? '已填自检' : '填写自检' }}
                    </Button>
                    <RadioGroup v-model:value="code.status" size="small" button-style="solid" class="font-bold shrink-0">
                      <RadioButton value="OK" class="data-[state=checked]:!bg-emerald-500 data-[state=checked]:!border-emerald-500">良品</RadioButton>
                      <RadioButton value="NG" class="data-[state=checked]:!bg-red-500 data-[state=checked]:!border-red-500">不良</RadioButton>
                    </RadioGroup>
                    <Select v-if="code.status === 'NG'" v-model:value="code.reason" size="small" class="w-24 shrink-0" :options="[{label:'划伤',value:'R1'}, {label:'缺料',value:'R2'}]" placeholder="原因" />
                    <Input v-model:value="code.remark" size="small" placeholder="单件异常备注说明..." class="flex-1 min-w-[150px] bg-slate-50" />
                  </div>
                </div>
              </div>

              <div class="mt-4 shrink-0 flex items-center justify-end gap-6 bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span class="font-bold text-slate-600 text-lg">扫码总计：<span class="text-indigo-600 font-black font-mono mx-1 text-2xl">{{ barcodeList.length }}</span> 件</span>
                <div class="w-px h-6 bg-slate-300"></div>
                <span class="font-bold text-emerald-600 text-lg">良品：<span class="font-black font-mono">{{ barcodeList.filter(c => c.status === 'OK').length }}</span></span>
                <span class="font-bold text-red-500 text-lg">不良：<span class="font-black font-mono">{{ barcodeList.filter(c => c.status === 'NG').length }}</span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AModal v-model:open="pieceInspectModalVisible" :title="`填写自检参数 - ${barcodeList[currentPieceIdx]?.sn}`" @ok="savePieceInspect" :width="500" centered>
      <div class="pt-4 pb-2 max-h-[400px] overflow-y-auto">
        <Table :dataSource="currentPieceInspectData" :pagination="false" size="small" bordered>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'item'"><span class="font-bold">{{ record.item }}</span></template>
            <template v-if="column.key === 'actual'">
              <Switch v-if="record.type === 'check'" v-model:checked="record.actual" checked-children="合格" un-checked-children="异常" />
              <InputNumber v-else v-model:value="record.actual" class="w-full" :placeholder="record.standard ? `要求:${record.standard}` : '填入值'" />
            </template>
          </template>
          <ATable.Column title="检验项目" dataIndex="item" key="item" />
          <ATable.Column title="检验结果录入" dataIndex="actual" key="actual" />
        </Table>
      </div>
    </AModal>

    <AModal v-model:open="authModalVisible" title="🔒 操作动作现场授权确认" @ok="handleAuthConfirm" :width="420" centered>
      <div class="pt-4 pb-2">
        <Form layout="vertical">
          <FormItem label="授权人工号或员工卡" required>
            <Input v-model:value="authForm.cardNo" placeholder="请刷入员工卡或工号条码..." size="large" class="font-mono text-lg" auto-focus />
          </FormItem>
          <FormItem label="授权密码 (高级权限校验时填写)">
            <Input type="password" v-model:value="authForm.password" placeholder="输入密码" size="large" />
          </FormItem>
        </Form>
      </div>
    </AModal>

    <AModal v-model:open="detailModalVisible" :title="detailTitle" width="100%" wrapClassName="fullscreen-modal" :footer="null" destroyOnClose>
      <div class="p-6 h-full flex flex-col bg-slate-50">
        <div class="flex-1 min-h-0 bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden flex flex-col">
          <Table :columns="detailColumns" :dataSource="detailData" :pagination="false" size="middle" class="flex-1 overflow-y-auto" />
        </div>
        <div class="shrink-0 h-16 flex items-center justify-center mt-4">
          <Button size="large" type="primary" class="w-48 font-bold text-lg bg-slate-800 border-none shadow-md" @click="detailModalVisible = false">关闭详情视图</Button>
        </div>
      </div>
    </AModal>

  </Modal>
</template>

<style scoped>
/* 主工作区 Tabs */
:deep(.custom-main-tabs .ant-tabs-nav) { padding: 0 16px; margin-bottom: 0; background-color: #ffffff; border-radius: 8px 8px 0 0; }
:deep(.custom-main-tabs .ant-tabs-tab) { font-size: 15px; font-weight: bold; padding: 12px 16px !important; }
:deep(.custom-main-tabs .ant-tabs-content-holder) { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #f8fafc; }
:deep(.custom-main-tabs .ant-tabs-content) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-main-tabs .ant-tabs-tabpane) { flex: 1; overflow: hidden; display: flex; flex-direction: column; }

/* 底部固定子 Tabs */
:deep(.custom-bottom-tabs) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
:deep(.custom-bottom-tabs > .ant-tabs-nav) {
  order: 2; margin: 0 !important; background: #e2e8f0; border-top: 1px solid #cbd5e1; padding: 6px 16px; flex-shrink: 0 !important;
}
:deep(.custom-bottom-tabs > .ant-tabs-nav .ant-tabs-tab) {
  background: transparent; border: none; padding: 8px 24px !important; border-radius: 6px; font-weight: bold; color: #64748b; transition: all 0.2s;
}
:deep(.custom-bottom-tabs > .ant-tabs-nav .ant-tabs-tab-active) {
  background: #ffffff !important; color: #4f46e5 !important; box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
:deep(.custom-bottom-tabs > .ant-tabs-content-holder) {
  order: 1; flex: 1; min-height: 0 !important; display: flex; flex-direction: column; background: #ffffff;
}
:deep(.custom-bottom-tabs > .ant-tabs-content-holder > .ant-tabs-content) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
:deep(.custom-bottom-tabs .ant-tabs-tabpane) { flex: 1; min-height: 0; display: flex; flex-direction: column; }

/* 内部表格自适应滚动 */
.custom-table-wrapper { display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-wrapper), .custom-table-wrapper :deep(.ant-spin-nested-loading), .custom-table-wrapper :deep(.ant-spin-container), .custom-table-wrapper :deep(.ant-table) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.custom-table-wrapper :deep(.ant-table-container) { flex: 1; overflow-y: auto; }

/* 详情描述样式 */
:deep(.custom-desc .ant-descriptions-item-label) { background-color: #f8fafc; font-weight: bold; color: #64748b; width: 140px; }

/* 全屏最大化弹窗底层覆写 */
:deep(.fullscreen-modal .ant-modal) { max-width: 100%; top: 0; padding-bottom: 0; margin: 0; }
:deep(.fullscreen-modal .ant-modal-content) { height: 100vh; display: flex; flex-direction: column; border-radius: 0; padding: 0;}
:deep(.fullscreen-modal .ant-modal-header) { padding: 16px 24px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; margin-bottom: 0; }
:deep(.fullscreen-modal .ant-modal-body) { flex: 1; padding: 0; overflow: hidden; display: flex; flex-direction: column; }

/* 巨型输入框微调 */
:deep(.custom-huge-input .ant-input) { height: 100%; text-align: center; }

/* 🌟 表单备注高度彻底自适应与对齐 */
.full-height-form-item { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.full-height-form-item :deep(.ant-form-item-row) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.full-height-form-item :deep(.ant-form-item-control) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.full-height-form-item :deep(.ant-form-item-control-input) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.full-height-form-item :deep(.ant-form-item-control-input-content) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.full-height-form-item textarea { height: 100% !important; border: 1px solid #d9d9d9 !important; }

.animate-fade-in { animation: fadeIn 0.2s ease-out; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>
