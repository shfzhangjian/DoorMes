<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, InputNumber, Select, Radio, Divider, message, Drawer, Upload, Alert, Modal as AModal, Table as ATable } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

// 💡 极其硬核：直接引入前置的《首批样品评价表》组件，用于实现无缝穿透查阅
import SampleDetailForm from '../../evaluate/modules/detail-modal.vue';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formData = ref<any>({ fileList: [] });
const formRef = ref();
const drawerVisible = ref(false);

const isEditable = computed(() => isNew.value || formData.value.status === 'DRAFT');

// 审批流转控制
const approvalForm = ref({ result: 'PASS', opinion: '', nextStep: 'DEFAULT', customStepId: undefined, assignees: [] });
const stepList = [{ label: '退回生产部重新验证', value: 'STEP_PROD' }, { label: '直接否决供应商', value: 'STEP_REJECT' }];

// 内部会签台账
const currentUserDept = ref('工艺部');
const internalReviews = ref<any[]>([]);
const processSteps = ref<any[]>([]);

// 💡 穿透引擎实例化
const [SampleModal, sampleModalApi] = useVbenModal({ connectedComponent: SampleDetailForm });

// ============================================================================
// 💡 SRM-MES 跨系统挂接与追溯引擎 (单选工单/样品，多选批次)
// ============================================================================
const selectorVisible = ref(false);
const selectorType = ref<'sample' | 'wo' | 'batch'>('sample');
const selectedRowKeys = ref<string[]>([]);
const selectedRows = ref<any[]>([]);

// 已挂接的试产批次明细表 (支持多个批次)
const linkedBatchList = ref<any[]>([]);

// 选择器列定义
const columnsMap = {
  sample: [
    { title: '送样单号', dataIndex: 'no', width: 160 },
    { title: '供应商', dataIndex: 'supplier' },
    { title: '样品名称', dataIndex: 'material' },
    { title: '综合结论', dataIndex: 'status', width: 100 }
  ],
  wo: [
    { title: 'MES试产工单号', dataIndex: 'no', width: 180 },
    { title: '生产产品', dataIndex: 'product' },
    { title: '计划排产数', dataIndex: 'qty', width: 100 },
    { title: '工单状态', dataIndex: 'status', width: 100 }
  ],
  batch: [
    { title: 'MES产出批次号', dataIndex: 'batchNo', width: 200 },
    { title: '产出数量', dataIndex: 'qty', width: 100 },
    { title: '批次良率', dataIndex: 'yield', width: 100 },
    { title: '质检状态', dataIndex: 'qcStatus', width: 100 }
  ]
};

// 模拟数据库数据
const mockDataMap = {
  sample: [
    { key: '1', no: 'SMP-260220-001', supplier: '江苏某高分子材料公司', material: '光学级PET树脂', status: '评估合格' },
    { key: '2', no: 'SMP-260301-088', supplier: '浙江某新材料科技', material: '特种交联剂', status: '评估合格' }
  ],
  wo: [
    { key: '1', no: 'MO-260305-T01', product: '光学级PET树脂(试产)', qty: '500 KG', status: '已完工' },
    { key: '2', no: 'MO-260308-T02', product: '特种交联剂(试产)', qty: '200 L', status: '执行中' }
  ],
  batch: [
    { key: '1', batchNo: 'BAT-OUT-260305-X01', qty: '250 KG', yield: '98.5%', qcStatus: '合格放行' },
    { key: '2', batchNo: 'BAT-OUT-260305-X02', qty: '242 KG', yield: '99.1%', qcStatus: '合格放行' },
    { key: '3', batchNo: 'BAT-OUT-260305-X03', qty: '8 KG', yield: '40.0%', qcStatus: '隔离待判' }
  ]
};

// 打开选择器
function openSelector(type: 'sample' | 'wo' | 'batch') {
  selectorType.value = type;
  selectedRowKeys.value = [];
  selectedRows.value = [];
  selectorVisible.value = true;
}

// 确认挂接
function handleSelectConfirm() {
  if (selectedRows.value.length === 0) return message.warning('请至少勾选一条记录！');

  if (selectorType.value === 'sample') {
    formData.value.sampleNo = selectedRows.value[0].no;
    formData.value.supplierName = selectedRows.value[0].supplier;
    formData.value.materialName = selectedRows.value[0].material;
    message.success('首批样品单关联成功！');
  } else if (selectorType.value === 'wo') {
    formData.value.workOrderNo = selectedRows.value[0].no;
    formData.value.trialQty = selectedRows.value[0].qty;
    message.success('MES试产工单关联成功！');
  } else if (selectorType.value === 'batch') {
    // 追加多个批次并去重
    selectedRows.value.forEach(row => {
      if (!linkedBatchList.value.some(b => b.batchNo === row.batchNo)) {
        linkedBatchList.value.push({ ...row });
      }
    });
    message.success(`成功提取 ${selectedRows.value.length} 个生产批次！`);
  }
  selectorVisible.value = false;
}

// 移除批次
function removeBatch(idx: number) { linkedBatchList.value.splice(idx, 1); }

// 穿透跳转模拟
function jumpToRef(type: string, refNo: string) {
  if (type === 'sample') {
    sampleModalApi.setData({ sampleNo: refNo, tabType: 'processed', status: 'PASSED' }).open();
  } else {
    // MES 工单和批次属于外部模块，模拟跨系统调用
    message.loading(`正在跨系统请求 MES，准备渲染【${refNo}】的追溯详情...`, 1.5).then(() => {
      message.success('MES 追溯档案已就绪 (此处应弹出对应的MES单据组件)');
    });
  }
}

// ============================================================================
// 💡 主体弹窗控制
// ============================================================================
const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 下发小批量试产指令与挂接' : '🔬 试产总结与放行核准台'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;
      approvalForm.value = { result: 'PASS', opinion: '', nextStep: 'DEFAULT', customStepId: undefined, assignees: [] };

      if (isNew.value) {
        formData.value = { status: 'DRAFT', trialDate: dayjs().format('YYYY-MM-DD'), sampleNo: '', workOrderNo: '', fileList: [] };
        linkedBatchList.value = [];
        internalReviews.value = [];
        processSteps.value = [{ title: '拟定试产计划', desc: '当前状态: 草稿', status: 'process' }];
        modalApi.setState({ showConfirmButton: true, confirmText: '下发试产工单', cancelText: '保存草稿' });
      } else {
        formData.value = {
          ...data,
          productLine: '一车间 2号涂布线',
          sampleNo: 'SMP-260220-001',
          workOrderNo: 'MO-260305-T01',
          defectSummary: '试产顺利，极少部分出现黑点疵病，已通知供应商加强静电除尘。',
          fileList: [{uid: '1', name: '小批量试产总结报告(含工艺参数记录).pdf', status: 'done', url: '#'}]
        };

        linkedBatchList.value = [
          { batchNo: 'BAT-OUT-260305-X01', qty: '250 KG', yield: '98.5%', qcStatus: '合格放行' },
          { batchNo: 'BAT-OUT-260305-X02', qty: '242 KG', yield: '99.1%', qcStatus: '合格放行' }
        ];

        if (data.status === 'PASSED') {
          internalReviews.value = [
            { id: 'R1', project: '生产现场实操与效率评估', dept: '生产部', result: 'PASS', opinion: '无卡机停机现象，作业效率同现有物料。', reviewer: '车间主任', time: '2026-03-05' },
            { id: 'R2', project: '工艺参数适配与稳定性', dept: '工艺部', result: 'PASS', opinion: '无需大幅调整现有设备参数，工艺窗口宽容度良好。', reviewer: '李工艺', time: '2026-03-05' },
            { id: 'R3', project: '试产成品综合良率与质检', dept: '品质部', result: 'PASS', opinion: '实测良率98.5%，满足≥98%的量产要求。', reviewer: '赵品质', time: '2026-03-06' },
            { id: 'R4', project: '最终放行准入批准', dept: '研发部/高管', result: 'PASS', opinion: '同意批准放行，转为正常量产物料。', reviewer: '张总监', time: '2026-03-06' },
          ];
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
        } else if (data.tabType === 'todo') {
          internalReviews.value = [
            { id: 'R1', project: '生产现场实操与效率评估', dept: '生产部', result: 'PASS', opinion: '作业效率同现有物料，生产顺利。', reviewer: '车间主任', time: '2026-03-05', editable: false },
            { id: 'R2', project: '工艺参数适配与稳定性', dept: '工艺部', result: null, opinion: '', reviewer: '', time: '', editable: true },
            { id: 'R3', project: '试产成品综合良率与质检', dept: '品质部', result: null, opinion: '', reviewer: '', time: '', editable: false },
            { id: 'R4', project: '最终放行准入批准', dept: '研发部/高管', result: null, opinion: '', reviewer: '', time: '', editable: false },
          ];
          modalApi.setState({ showConfirmButton: true, confirmText: '提交评估结论并流转', cancelText: '暂不处理' });
        } else {
          internalReviews.value = [];
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
        }

        processSteps.value = [
          { title: '下发试产通知', desc: data.applicant, status: 'finish' },
          { title: '车间/工艺/品质联合评估', desc: '当前节点', status: data.status === 'PASSED' ? 'finish' : 'process' },
          { title: '正式放行量产', desc: '等待流转', status: data.status === 'PASSED' ? 'finish' : 'wait' }
        ];
      }
    }
  },
  async onConfirm() {
    if (isEditable.value) {
      await formRef.value?.validate();
      if(!formData.value.workOrderNo || !formData.value.sampleNo) return message.warning('请挂接关联的样品单与MES试产工单！');
      message.success('小批量试产通知已下发车间执行！');
    } else {
      const myReview = internalReviews.value.find(r => r.editable);
      if (myReview && !myReview.result) return message.warning('请完成您的评估结论！');
      message.success(`评估报告提交成功！系统将自动流转。`);
    }
    emit('success');
    modalApi.close();
  }
});
const beforeUpload = (file: any) => { message.success(`${file.name} 报告已上传！`); return false; };
</script>

<template>
  <Modal class="w-[1200px]">

    <SampleModal />

    <AModal v-model:open="selectorVisible" :title="`🔗 挂接提取平台`" width="800px" @ok="handleSelectConfirm" okText="确认选中并提取" cancelText="取消">
      <div class="mb-4 text-slate-500 text-sm">请从下表中勾选需要挂接追溯的业务单据/实体。</div>
      <ATable
        :dataSource="mockDataMap[selectorType]"
        :columns="columnsMap[selectorType]"
        size="middle" bordered
        :rowSelection="{
           type: selectorType === 'batch' ? 'checkbox' : 'radio',
           selectedRowKeys: selectedRowKeys,
           onChange: (keys, rows) => { selectedRowKeys = keys as string[]; selectedRows = rows; }
         }"
      />
    </AModal>

    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden relative">
      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6 flex flex-col gap-6 relative">

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12 mt-2">

          <div class="text-center mb-8 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">小批量试产验证与放行报告</h2>
            <div v-if="formData.status === 'PASSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">放行量产</div>
            <div v-if="formData.status === 'REJECTED'" class="absolute -top-4 right-10 text-6xl text-red-500 opacity-20 border-4 border-red-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">试产失败</div>
          </div>

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isEditable && formData.tabType !== 'todo'">

            <Divider orientation="left" class="!m-0 !mb-4 border-slate-200"><span class="text-slate-500 font-bold">一、 试产计划与前置业务关联</span></Divider>

            <div class="grid grid-cols-4 gap-6 mb-4">
              <Form.Item label="供应商名称" required name="supplierName" class="col-span-2 mb-0"><Input v-model:value="formData.supplierName" size="large"/></Form.Item>
              <Form.Item label="试产物料名称" required name="materialName" class="col-span-2 mb-0"><Input v-model:value="formData.materialName" size="large"/></Form.Item>

              <Form.Item label="计划试产数量" required name="trialQty" class="mb-0"><Input v-model:value="formData.trialQty" placeholder="如：500 KG" /></Form.Item>
              <Form.Item label="计划试产线别/机台" required name="productLine" class="col-span-2 mb-0"><Input v-model:value="formData.productLine" placeholder="如：一车间涂布线" /></Form.Item>
              <Form.Item label="下达/执行日期" required name="trialDate" class="mb-0"><Input v-model:value="formData.trialDate" type="date" /></Form.Item>
            </div>

            <div class="bg-indigo-50/50 p-6 rounded-lg border border-indigo-200 mb-8 mt-2">
              <div class="text-sm font-bold text-indigo-800 mb-5 flex items-center gap-2">
                <IconifyIcon icon="lucide:git-commit-horizontal" class="text-indigo-600 text-lg"/> SRM 业务流与 MES 制造流 穿透追溯看板
              </div>

              <div class="grid grid-cols-2 gap-8">
                <div class="flex flex-col gap-4 border-r border-indigo-200 pr-8">
                  <Form.Item label="SRM追溯：关联前置《首批样品评价表》" required class="mb-0 font-bold text-slate-600">
                    <Input.Search v-if="isEditable" v-model:value="formData.sampleNo" placeholder="绑定前置送样评价单..." enter-button="选择挂接" @search="openSelector('sample')" readonly />
                    <div v-else class="flex items-center justify-between bg-white p-2 px-3 border border-slate-200 rounded shadow-sm">
                      <span class="font-mono font-bold text-indigo-700">{{ formData.sampleNo || '未关联' }}</span>
                      <Button type="primary" ghost size="small" @click="jumpToRef('sample', formData.sampleNo)" :disabled="!formData.sampleNo">穿透查阅首样数据</Button>
                    </div>
                  </Form.Item>
                  <div class="text-xs text-indigo-500 leading-relaxed">
                    * 根据 IATF16949 要求，试产物料必须拥有合格的样品评价报告作为前置放行依据。
                  </div>
                </div>

                <div class="flex flex-col gap-4 justify-center">
                  <Form.Item label="MES追溯：绑定现场制造工单 (MO)" required class="mb-0 font-bold text-slate-600">
                    <Input.Search v-if="isEditable" v-model:value="formData.workOrderNo" placeholder="提取MES下发的试产工单号..." enter-button="提取工单" @search="openSelector('wo')" readonly />
                    <div v-else class="flex items-center justify-between bg-white p-2 px-3 border border-slate-200 rounded shadow-sm">
                      <span class="font-mono font-bold text-indigo-700">{{ formData.workOrderNo || '未关联' }}</span>
                      <Button type="primary" ghost size="small" @click="jumpToRef('wo', formData.workOrderNo)" :disabled="!formData.workOrderNo">钻取工单详情</Button>
                    </div>
                  </Form.Item>
                </div>
              </div>

              <div class="mt-6 pt-5 border-t border-indigo-200 border-dashed">
                <div class="flex items-center justify-between mb-3">
                  <div class="text-sm font-bold text-slate-700">本次试产产出成品批次 (支持多批次挂接)</div>
                  <Button v-if="isEditable || formData.tabType === 'todo'" size="small" type="primary" @click="openSelector('batch')">
                    <IconifyIcon icon="lucide:plus" class="mr-1"/> 提取MES批次
                  </Button>
                </div>

                <table class="w-full border-collapse text-xs text-left bg-white shadow-sm border border-slate-300">
                  <thead class="bg-slate-100 text-slate-600 font-bold">
                  <tr>
                    <th class="border border-slate-300 p-2 w-[5%] text-center">序</th>
                    <th class="border border-slate-300 p-2">成品批次号 (Batch No)</th>
                    <th class="border border-slate-300 p-2 w-[15%] text-center">产出数量</th>
                    <th class="border border-slate-300 p-2 w-[15%] text-center">综合良率</th>
                    <th class="border border-slate-300 p-2 w-[15%] text-center">MES质检状态</th>
                    <th class="border border-slate-300 p-2 w-[15%] text-center">操作追溯</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="(batch, idx) in linkedBatchList" :key="batch.batchNo" class="hover:bg-slate-50 transition-colors">
                    <td class="border border-slate-300 p-2 text-center text-slate-400">{{ idx + 1 }}</td>
                    <td class="border border-slate-300 p-2 font-mono font-bold text-indigo-700">{{ batch.batchNo }}</td>
                    <td class="border border-slate-300 p-2 text-center">{{ batch.qty }}</td>
                    <td class="border border-slate-300 p-2 text-center" :class="parseFloat(batch.yield) >= 98 ? 'text-green-600' : 'text-orange-500 font-bold'">{{ batch.yield }}</td>
                    <td class="border border-slate-300 p-2 text-center">
                      <Tag :color="batch.qcStatus === '合格放行' ? 'success' : 'warning'" class="!m-0 border-none scale-90">{{ batch.qcStatus }}</Tag>
                    </td>
                    <td class="border border-slate-300 p-2 text-center">
                      <Button type="link" size="small" class="!px-1" @click="jumpToRef('batch', batch.batchNo)">钻取</Button>
                      <Button v-if="isEditable || formData.tabType === 'todo'" type="link" danger size="small" class="!px-1" @click="removeBatch(idx)">移除</Button>
                    </td>
                  </tr>
                  <tr v-if="linkedBatchList.length === 0">
                    <td colspan="6" class="border border-slate-300 p-4 text-center text-slate-400">尚未挂接任何 MES 产出批次</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <template v-if="!isNew && formData.status !== 'DRAFT'">
              <Divider orientation="left" class="!m-0 !mb-4 border-slate-200"><span class="text-slate-500 font-bold">二、 试产综合评估与报告附件</span></Divider>
              <div class="grid grid-cols-2 gap-8 mb-8">
                <div class="flex flex-col gap-4 border border-slate-200 rounded-lg p-5 bg-slate-50">
                  <Form.Item label="试产产线实际总良率 (%)" class="mb-0 font-bold text-indigo-700">
                    <InputNumber v-model:value="formData.lineYield" class="w-full text-lg font-mono" :min="0" :max="100" />
                  </Form.Item>
                  <Form.Item label="试产异常与主要缺陷汇总" class="mb-0 mt-2">
                    <Input.TextArea v-model:value="formData.defectSummary" :rows="2" placeholder="填写过程中的异常状况..." />
                  </Form.Item>
                </div>

                <div class="flex flex-col justify-center border border-slate-200 rounded-lg p-5 bg-slate-50">
                  <Alert message="请打包上传首件巡检单、工艺参数比对表及品质实验室全维度测试数据。" type="info" class="bg-white shadow-sm mb-4 py-1" />
                  <Form.Item label="《详细试产总结评估报告》" class="mb-0 font-bold text-slate-600">
                    <Upload v-model:file-list="formData.fileList" :before-upload="beforeUpload" :disabled="formData.tabType !== 'todo'">
                      <Button :disabled="formData.tabType !== 'todo'" class="flex items-center gap-1 font-bold border-indigo-300 text-indigo-600 bg-white"><IconifyIcon icon="lucide:paperclip"/> 上传完整试产报告</Button>
                    </Upload>
                  </Form.Item>
                </div>
              </div>
            </template>

          </Form>

          <template v-if="internalReviews.length > 0">
            <Divider orientation="left" class="!m-0 !mb-6 border-indigo-200">
              <span class="text-indigo-800 font-black text-lg flex items-center gap-2">
                  <IconifyIcon icon="lucide:users-2"/> 车间现场与多部门联合放行结论
              </span>
            </Divider>

            <div class="mb-8">
              <table class="w-full border-collapse text-sm text-left bg-white shadow-sm border border-slate-300">
                <thead class="bg-slate-100 text-slate-700 font-bold">
                <tr>
                  <th class="border border-slate-300 p-3 w-[22%]">评估维度</th>
                  <th class="border border-slate-300 p-3 w-[12%] text-center">责任部门</th>
                  <th class="border border-slate-300 p-3 w-[20%] text-center">评估结论</th>
                  <th class="border border-slate-300 p-3 flex-1">状况说明 / 改善建议</th>
                  <th class="border border-slate-300 p-3 w-[12%] text-center">经办人</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(review, idx) in internalReviews" :key="review.id"
                    :class="[review.editable ? 'bg-orange-50/60' : 'hover:bg-slate-50 transition-colors', review.dept.includes('研发') ? 'bg-indigo-50/50 border-t-2 border-indigo-200' : '']">

                  <td class="border border-slate-300 p-3 font-bold text-slate-700">
                    <span class="text-slate-400 mr-1">{{ idx + 1 }}.</span>{{ review.project }}
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <Tag :color="review.dept.includes('研发') ? 'purple' : (review.editable ? 'processing' : 'default')" class="!m-0 font-bold">{{ review.dept }}</Tag>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <div v-if="review.editable">
                      <Radio.Group v-model:value="review.result" size="small" button-style="solid" class="flex w-full">
                        <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">评估通过</Radio.Button>
                        <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">异常退回</Radio.Button>
                      </Radio.Group>
                    </div>
                    <div v-else>
                      <Tag v-if="review.result === 'PASS'" color="success" class="!m-0 font-bold border-none px-4">验证通过</Tag>
                      <Tag v-else-if="review.result === 'REJECT'" color="error" class="!m-0 font-bold border-none px-4">否决退回</Tag>
                      <span v-else class="text-slate-400 text-xs">- 待评估 -</span>
                    </div>
                  </td>

                  <td class="border border-slate-300 p-3">
                    <div v-if="review.editable">
                      <Input v-model:value="review.opinion" placeholder="请填写详细说明..." size="small" class="w-full !border-orange-300 bg-white" />
                    </div>
                    <div v-else class="text-xs text-slate-600 font-bold leading-relaxed">
                      {{ review.opinion || '-' }}
                    </div>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <span class="text-xs font-bold text-slate-800 block">{{ review.reviewer || '-' }}</span>
                    <span class="text-[10px] text-slate-400">{{ review.time || '' }}</span>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </template>

          <div v-if="formData.tabType === 'todo'" class="bg-slate-50 p-4 rounded-lg flex items-center gap-4 justify-between border border-slate-200 mb-6">
            <div class="text-sm font-bold text-slate-600 flex items-center gap-2">
              <IconifyIcon icon="lucide:settings-2"/> 流程干预路由
            </div>
            <div class="flex items-center gap-4 flex-1 max-w-lg">
              <Select v-model:value="approvalForm.nextStep" class="flex-1" size="small">
                <Select.Option value="DEFAULT">系统默认流转下一评估部门</Select.Option>
                <Select.Option value="CUSTOM">重大异常强制中断试产</Select.Option>
              </Select>
            </div>
          </div>

          <div v-if="!isNew" class="flex justify-center pb-2 mt-4">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看试产放行流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="试产流转日志" placement="right" :width="450">
      <div class="bg-indigo-50 border border-indigo-100 p-3 rounded text-indigo-800 text-sm mb-6 flex items-start gap-2">
        <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
        <span>当前停留在 <b>{{ formData.node || '草稿状态' }}</b>。</span>
      </div>
      <Steps direction="vertical" :current="1" size="small" class="px-2">
        <Steps.Step v-for="(step, i) in processSteps" :key="i" :title="step.title" :status="step.status as any">
          <template #description>
            <div class="text-xs mt-2 mb-6 p-3 rounded-lg shadow-sm border" :class="step.status === 'process' ? 'bg-white border-indigo-200' : 'bg-slate-50 border-slate-100 text-slate-500'">
              <div class="font-bold flex justify-between">
                <span :class="step.status === 'process' ? 'text-indigo-600' : 'text-slate-600'">{{ step.desc }}</span>
              </div>
            </div>
          </template>
        </Steps.Step>
      </Steps>
    </Drawer>
  </Modal>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
:deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)) { border-color: currentColor !important; }
</style>
