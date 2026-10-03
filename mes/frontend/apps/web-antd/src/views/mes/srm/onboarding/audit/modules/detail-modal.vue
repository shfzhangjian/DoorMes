<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, InputNumber, Select, Radio, Divider, message, Drawer, Alert, Upload, Modal as AModal, Table as ATable } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 💡 极其硬核：引入前置业务的详情组件，用于实现“无极穿透查阅”
import ApplyDetailForm from '../../apply/modules/detail-modal.vue';
import SurveyDetailForm from '../../survey/modules/detail-modal.vue';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formRef = ref();

const formData = ref<any>({ otherFileList: [] });
const isEditable = computed(() => isNew.value || formData.value.status === 'DRAFT');

// ============================================================================
// 💡 挂接选择器弹窗逻辑 (单据关联引擎)
// ============================================================================
const selectorVisible = ref(false);
const selectorType = ref<'apply' | 'survey'>('apply');
const selectedRows = ref<any[]>([]);

// 挂接选择器的表格列定义
const selectorColumns = [
  { title: '单据流水号', dataIndex: 'no', width: 180 },
  { title: '供应商名称', dataIndex: 'supplier' },
  { title: '关联物料/项目', dataIndex: 'material' },
  { title: '审批状态', dataIndex: 'status', width: 100 }
];

// 模拟数据库里可供挂接的已通过单据
const mockApplyData = [
  { key: '1', no: 'SRM-IN-260219-01', supplier: '江苏某高分子材料公司', material: '光学级PET树脂', status: '已批准' },
  { key: '2', no: 'SRM-IN-260301-88', supplier: '浙江某新材料科技', material: '特种交联剂', status: '已批准' }
];
const mockSurveyData = [
  { key: '1', no: 'SRM-SV-260219-01', supplier: '江苏某高分子材料公司', material: '原厂基本情况调查', status: '初评合格' },
  { key: '2', no: 'SRM-SV-260301-99', supplier: '浙江某新材料科技', material: '代理商基本情况调查', status: '初评合格' }
];

// 打开挂接选择器
function openSelector(type: 'apply' | 'survey') {
  selectorType.value = type;
  selectedRows.value = [];
  selectorVisible.value = true;
}

// 确认挂接
function handleSelectConfirm() {
  if (selectedRows.value.length === 0) return message.warning('请勾选一条需要关联的单据记录！');
  const selected = selectedRows.value[0];
  if (selectorType.value === 'apply') {
    formData.value.applyNo = selected.no;
    // 智能联动：自动带出供应商和物料名称
    formData.value.supplierName = selected.supplier;
    formData.value.materialName = selected.material;
  } else {
    formData.value.surveyNo = selected.no;
  }
  selectorVisible.value = false;
  message.success('单据血缘关联成功！');
}

// ============================================================================
// 💡 无极穿透查阅引擎 (注册并实例化前置组件)
// ============================================================================
const [ApplyModal, applyModalApi] = useVbenModal({ connectedComponent: ApplyDetailForm });
const [SurveyModal, surveyModalApi] = useVbenModal({ connectedComponent: SurveyDetailForm });

function jumpToRef(type: 'apply' | 'survey') {
  if (type === 'apply' && formData.value.applyNo) {
    // 携带单号，以“只读(processed)”状态打开申请单弹窗
    applyModalApi.setData({ applyNo: formData.value.applyNo, tabType: 'processed', status: 'PASSED' }).open();
  } else if (type === 'survey' && formData.value.surveyNo) {
    // 携带单号，以“只读(processed)”状态打开调查表弹窗
    surveyModalApi.setData({ surveyNo: formData.value.surveyNo, nature: 'MANUFACTURER', tabType: 'processed', status: 'PASSED' }).open();
  }
}

// ============================================================================
// 💡 综合定标矩阵与办理区
// ============================================================================
const selectionMatrix = ref([
  { id: 1, item: '技术能力', weight: 35, remark: '', score: null },
  { id: 2, item: '价格优势', weight: 15, remark: '', score: null },
  { id: 3, item: '交货周期', weight: 10, remark: '', score: null },
  { id: 4, item: '付款条件', weight: 10, remark: '', score: null },
  { id: 5, item: '认证水平', weight: 10, remark: '', score: null },
  { id: 6, item: '企业规模', weight: 10, remark: '', score: null },
  { id: 7, item: '配合度', weight: 10, remark: '', score: null },
]);

const totalFinalScore = computed(() => selectionMatrix.value.reduce((sum, row) => sum + (row.score || 0), 0));
function handleScoreChange(row: any) { if (row.score > row.weight) { row.score = row.weight; } }

const approvalForm = ref({ result: 'PASS', opinion: '', nextStep: 'DEFAULT', customStepId: undefined, assignees: [] });
const drawerVisible = ref(false);
const processSteps = ref<any[]>([]);

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 新建供应商选择书' : '📑 供应商选择与定标单'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;
      approvalForm.value = { result: 'PASS', opinion: '', nextStep: 'DEFAULT', customStepId: undefined, assignees: [] };

      if (isNew.value) {
        formData.value = { status: 'DRAFT', supplierName: '', materialName: '', applyNo: '', surveyNo: '', finalSummary: '', otherFileList: [] };
        selectionMatrix.value.forEach(r => { r.score = null; r.remark = ''; });
        processSteps.value = [{ title: '拟定选择表', desc: '当前状态: 草稿', status: 'process' }];
        modalApi.setState({ showConfirmButton: true, confirmText: '提交定标申请', cancelText: '保存草稿' });
      } else {
        formData.value = {
          ...data,
          applyNo: 'SRM-IN-260219-01', surveyNo: 'SRM-SV-260219-01', finalSummary: '各维度表现均衡，符合战略要求。',
          otherFileList: [{uid: '1', name: '第三方资信调查报告.pdf', status: 'done', url: '#'}]
        };
        processSteps.value = [
          { title: '拟定选择表', desc: '张采购 - 提交申请', status: 'finish' },
          { title: '部门联合定标', desc: '当前节点', status: data.status === 'PASSED' ? 'finish' : 'process' },
          { title: '总经理批示', desc: '等待流转', status: data.status === 'PASSED' ? 'finish' : 'wait' }
        ];

        // 模拟打分回显
        selectionMatrix.value[0].score = 32; selectionMatrix.value[1].score = 12; selectionMatrix.value[2].score = 9;
        selectionMatrix.value[3].score = 10; selectionMatrix.value[4].score = 10; selectionMatrix.value[5].score = 8; selectionMatrix.value[6].score = 8.5;

        if (data.tabType === 'todo') {
          modalApi.setState({ showConfirmButton: true, confirmText: '提交审批意见并流转', cancelText: '暂不处理' });
        } else if (data.status === 'DRAFT') {
          modalApi.setState({ showConfirmButton: true, confirmText: '提交定标申请', cancelText: '保存草稿' });
        } else {
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭阅览' });
        }
      }
    }
  },
  async onConfirm() {
    if (isEditable.value) {
      if(selectionMatrix.value.some(r => r.score === null)) return message.warning('请为所有 7 个评估维度打分！');
      message.success('供应商选择书已成功提交进入审批流！');
    } else {
      if (approvalForm.value.result === 'REJECT' && !approvalForm.value.opinion) return message.error('驳回必须填写审批意见！');
      message.success('审批意见提交成功！任务已推送到下一节点。');
    }
    emit('success');
    modalApi.close();
  }
});
const beforeUpload = (file: any) => { message.success(`${file.name} 附件已挂接！`); return false; };
</script>

<template>
  <Modal class="w-[1200px]">

    <ApplyModal />
    <SurveyModal />

    <AModal v-model:open="selectorVisible" :title="selectorType === 'apply' ? '🔗 选择挂接《寻源导入申请单》' : '🔗 选择挂接《调查与初评单》'" width="800px" @ok="handleSelectConfirm" okText="确认选中并关联" cancelText="取消">
      <div class="mb-4 text-slate-500 text-sm">请从下表中勾选一条已通过审核的历史单据进行关联，关联后系统将自动提取相关数据。</div>
      <ATable
        :dataSource="selectorType === 'apply' ? mockApplyData : mockSurveyData"
        :columns="selectorColumns"
        size="middle" bordered
        :rowSelection="{ type: 'radio', onChange: (keys, rows) => { selectedRows = rows; } }"
      />
    </AModal>

    <div class="flex flex-col h-full min-h-[75vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden relative">
      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6 flex flex-col gap-6 relative">
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12 mt-2">

          <div class="text-center mb-10 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">供应商选择表</h2>
            <div v-if="formData.status === 'PASSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">准入核准</div>
          </div>

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isEditable">

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200"><span class="text-slate-500 font-bold text-base">一、采购项目与前置评估材料挂接</span></Divider>

            <div class="grid grid-cols-2 gap-6 mb-8">
              <Form.Item label="供应商名称" required class="mb-0">
                <Input v-model:value="formData.supplierName" placeholder="被评估供应商" size="large" />
              </Form.Item>
              <Form.Item label="采购项目 (物料范围)" required class="mb-0">
                <Input v-model:value="formData.materialName" placeholder="如：树脂/辅料" size="large" />
              </Form.Item>

              <div class="col-span-2 bg-blue-50/50 border border-blue-200 rounded-lg p-6 mt-2">
                <div class="text-sm font-bold text-indigo-800 mb-5 flex items-center gap-2">
                  <IconifyIcon icon="lucide:link-2" class="text-indigo-600 text-lg"/> 关联前置档案与补充证明材料
                </div>

                <div class="grid grid-cols-2 gap-8">
                  <div class="flex flex-col gap-4">
                    <Form.Item label="1. 关联《寻源导入申请单》" class="mb-0 font-bold text-slate-600">
                      <Input.Search v-if="isEditable" v-model:value="formData.applyNo" placeholder="点击右侧按钮选择关联单据..." enter-button="选择挂接" @search="openSelector('apply')" readonly />
                      <div v-else class="flex items-center justify-between bg-white p-2 px-3 border border-slate-200 rounded shadow-sm">
                        <span class="font-mono font-bold text-indigo-700">{{ formData.applyNo || '未关联' }}</span>
                        <Button type="primary" ghost size="small" @click="jumpToRef('apply')" :disabled="!formData.applyNo">穿透查阅</Button>
                      </div>
                    </Form.Item>

                    <Form.Item label="2. 关联《基本情况调查与初评单》" class="mb-0 font-bold text-slate-600">
                      <Input.Search v-if="isEditable" v-model:value="formData.surveyNo" placeholder="点击右侧按钮选择关联单据..." enter-button="选择挂接" @search="openSelector('survey')" readonly />
                      <div v-else class="flex items-center justify-between bg-white p-2 px-3 border border-slate-200 rounded shadow-sm">
                        <span class="font-mono font-bold text-indigo-700">{{ formData.surveyNo || '未关联' }}</span>
                        <Button type="primary" ghost size="small" @click="jumpToRef('survey')" :disabled="!formData.surveyNo">穿透查阅</Button>
                      </div>
                    </Form.Item>
                  </div>

                  <div class="border-l border-blue-200 pl-8 flex flex-col justify-center">
                    <Alert message="如有特批文件、现场考察报告、样品测试结论等佐证资料，请在此上传。" type="info" class="bg-white mb-4 shadow-sm" />
                    <Form.Item label="3. 补充证明材料附件" class="mb-0 font-bold text-slate-600">
                      <Upload v-model:file-list="formData.otherFileList" :before-upload="beforeUpload" :disabled="!isEditable">
                        <Button :disabled="!isEditable" class="flex items-center gap-1 font-bold border-indigo-300 text-indigo-600 bg-white"><IconifyIcon icon="lucide:paperclip"/> 上传补充证明附件</Button>
                      </Upload>
                    </Form.Item>
                  </div>
                </div>
              </div>
            </div>
          </Form>

          <Divider orientation="left" class="!m-0 !mb-6 border-indigo-200">
             <span class="text-indigo-800 font-black text-lg flex items-center gap-2">
                <IconifyIcon icon="lucide:pie-chart"/> 供应商定标综合评价矩阵
             </span>
          </Divider>

          <div class="mb-8 relative">
            <div class="absolute right-4 -top-12 flex flex-col items-center justify-center w-24 h-24 rounded-full border-4 border-indigo-600 text-indigo-700 bg-white shadow-lg z-10 rotate-12">
              <span class="text-xs font-bold mb-[-4px]">综合总分</span>
              <span class="text-3xl font-black font-mono">{{ totalFinalScore }}</span>
            </div>

            <table class="w-full border-collapse text-sm text-left bg-white shadow-sm border border-slate-300">
              <thead class="bg-slate-100 text-slate-700 font-bold">
              <tr>
                <th class="border border-slate-300 p-3 w-[15%] text-center">评估维度</th>
                <th class="border border-slate-300 p-3 w-[12%] text-center">权重 / 满分</th>
                <th class="border border-slate-300 p-3 flex-1">情况说明及评语</th>
                <th class="border border-slate-300 p-3 w-[15%] text-center">综合得分</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="row in selectionMatrix" :key="row.id" class="hover:bg-slate-50 transition-colors">
                <td class="border border-slate-300 p-3 text-center font-bold text-slate-700">{{ row.item }}</td>
                <td class="border border-slate-300 p-3 text-center text-indigo-600 font-bold bg-slate-50">{{ row.weight }} 分</td>

                <td class="border border-slate-300 p-2">
                  <Input v-if="isEditable" v-model:value="row.remark" placeholder="请填写具体说明..." size="small" class="w-full !border-transparent hover:!border-indigo-300" />
                  <span v-else class="text-xs text-slate-600 px-2 block leading-relaxed">{{ row.remark || '-' }}</span>
                </td>

                <td class="border border-slate-300 p-2 text-center" :class="isEditable ? 'bg-orange-50/30' : ''">
                  <div v-if="isEditable" class="flex items-center justify-center gap-1">
                    <InputNumber v-model:value="row.score" :min="0" :max="row.weight" :step="0.5" size="small" class="w-20 font-bold text-center" @change="handleScoreChange(row)"/>
                  </div>
                  <span v-else class="font-black text-base font-mono" :class="row.score === row.weight ? 'text-green-600' : 'text-slate-700'">{{ row.score ?? '-' }}</span>
                </td>
              </tr>

              <tr class="bg-slate-50 border-t-2 border-slate-200">
                <td class="border border-slate-300 p-4 font-black text-slate-800 text-center">选择总结</td>
                <td colspan="3" class="border border-slate-300 p-3">
                  <Input.TextArea v-if="isEditable" v-model:value="formData.finalSummary" :rows="2" placeholder="根据上述评分得出最终总结建议..." class="w-full bg-white" />
                  <span v-else class="text-sm font-bold text-slate-700">{{ formData.finalSummary }}</span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div v-if="formData.tabType === 'todo'" class="mt-8 pt-6 border-t-2 border-dashed border-indigo-200">
            <div class="font-black text-indigo-800 mb-4 flex items-center gap-2 text-lg">
              <IconifyIcon icon="lucide:clipboard-check"/> 审批流转与办理区
            </div>

            <div class="bg-indigo-50/50 p-6 rounded-xl border border-indigo-100">
              <div class="grid grid-cols-2 gap-6">
                <div class="flex flex-col gap-4">
                  <Form.Item label="办理结论" class="mb-0 font-bold">
                    <Radio.Group v-model:value="approvalForm.result" button-style="solid" class="w-full flex">
                      <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">同意签署</Radio.Button>
                      <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">驳回</Radio.Button>
                    </Radio.Group>
                  </Form.Item>
                  <Form.Item label="审批意见" class="mb-0 font-bold">
                    <Input.TextArea v-model:value="approvalForm.opinion" :rows="3" placeholder="请填写审批批示..." />
                  </Form.Item>
                </div>

                <div class="flex flex-col gap-4 pl-6 border-l border-indigo-100">
                  <Form.Item label="下步流转策略" class="mb-0 font-bold">
                    <Select v-model:value="approvalForm.nextStep" class="w-full">
                      <Select.Option value="DEFAULT">系统默认流转下一节点</Select.Option>
                      <Select.Option value="CUSTOM">强制异常流转干预</Select.Option>
                    </Select>
                  </Form.Item>
                  <Form.Item label="加签 / 知会人员" class="mb-0 font-bold">
                    <Select mode="multiple" v-model:value="approvalForm.assignees" :options="[{label:'李建国(总经理)',value:'U02'}]" placeholder="选择需额外知会的人员..." class="w-full" />
                  </Form.Item>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!isNew" class="flex justify-center pb-2 mt-6">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看单据审批流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="审批流转记录" placement="right" :width="450">
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
