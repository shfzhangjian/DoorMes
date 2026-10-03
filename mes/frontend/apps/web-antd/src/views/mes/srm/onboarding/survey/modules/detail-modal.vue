<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, Select, Radio, InputNumber, Divider, message, Drawer, Upload, Alert } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formRef = ref();

// 💡 动态表单 JSON 配置引擎
const manufacturerSchema = [
  { field: 'area', label: '占地面积 (㎡)', type: 'InputNumber', placeholder: '如: 15000', span: 1 },
  { field: 'employeeCount', label: '员工总数 (人)', type: 'InputNumber', placeholder: '如: 120', span: 1 },
  { field: 'industryRank', label: '行业排名/地位评估', type: 'Input', placeholder: '如: 国际前三 / 国内前五', span: 1 },
  { field: 'rdRatio', label: '研发人员数量/占比', type: 'Input', placeholder: '如: 18人 (15%)', span: 1 },
  { field: 'qaRatio', label: '品质人员数量/占比', type: 'Input', placeholder: '如: 12人 (10%)', span: 1 },
  { field: 'annualCapacity', label: '核心产品年产能概况', type: 'Input', placeholder: '如: 年产量5000吨', span: 1 }
];

const agentSchema = [
  { field: 'mainBrand', label: '主打优势品牌及代理层级', type: 'Input', placeholder: '如: 某化工 (一级代理)', span: 1 },
  { field: 'coopYears', label: '与被代理厂家合作年限', type: 'Select', options: [{label:'1年以内',value:'1'}, {label:'1-3年',value:'3'}, {label:'3-5年',value:'5'}, {label:'5年以上',value:'10'}], span: 1 },
  { field: 'capitalScale', label: '企业流动垫资金额规模', type: 'Select', options: [{label:'200万以下',value:'1'}, {label:'200-500万',value:'2'}, {label:'500-1000万',value:'3'}, {label:'1000万以上',value:'4'}], span: 1 },
  { field: 'agentDelivery', label: '仓储与送货方式', type: 'Input', placeholder: '如: 自有仓库 / 转包第三方物流', span: 3 }
];

// 表单数据载体
const formData = ref<any>({
  surveyFileList: [], licenseFileList: [], certFileList: [], hsfFileList: [], agencyFileList: [], equipmentFileList: [], customerFileList: []
});

const currentSchema = computed(() => formData.value.nature === 'MANUFACTURER' ? manufacturerSchema : agentSchema);
const isFilling = computed(() => isNew.value || formData.value.status === 'FILLING');

// ============================================================================
// 💡 核心机制：基于角色的多部门协同审核
// ============================================================================
const currentUserDept = ref('技术部'); // 模拟当前登录人部门
const currentUserName = ref('王工');   // 模拟当前登录人姓名

const internalReviews = ref<any[]>([]);
const gmReview = ref({ result: null, opinion: '', reviewer: '', time: '' });

// 抽屉与流程控制
const drawerVisible = ref(false);
const processSteps = [
  { title: '下发调查任务', desc: '系统自动 - 2026-02-18 10:00', status: 'finish' },
  { title: '供应商信息填报', desc: '对方已提交 - 2026-02-19 08:30', status: 'finish' },
  { title: '各部门评审', desc: '当前节点 (审核中)', status: 'process' },
  { title: '总经理终审', desc: '等待流转', status: 'wait' }
];

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 发起供应商基本情况调查' : '📑 供应商基本情况调查与初评台'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;

      if (isNew.value) {
        formData.value = { nature: 'MANUFACTURER', supplierName: '', status: 'FILLING', surveyFileList: [] };
        modalApi.setState({ showConfirmButton: true, confirmText: '保存并提交填报', cancelText: '保存草稿' });
        internalReviews.value = [];
      } else {
        formData.value = {
          ...data,
          surveyFileList: [{uid: '1', name: '已填写的基本情况调查表.pdf', status: 'done', url: '#'}],
          licenseFileList: [{uid: '2', name: '营业执照副本扫描件.jpg', status: 'done', url: '#'}]
        };

        // 💡 动态模拟单据处于不同状态时的审核表格数据
        if (data.tabType === 'todo') {
          // 状态1：等待处理。采购部已经审完了，现在轮到技术部和品质部审。
          internalReviews.value = [
            { id: 'R1', project: '供应商规模和资历', dept: '采购部', result: 'PASS', opinion: '规模符合我司战略要求，资质齐全。', reviewer: '张采购', time: '2026-02-19 10:00' },
            { id: 'R2', project: '供应商交货保证', dept: '技术部', result: null, opinion: '', reviewer: '', time: '' }, // 💡 这一行是我的任务
            { id: 'R3', project: '供应商品质保证能力', dept: '品质部', result: null, opinion: '', reviewer: '', time: '' },
            { id: 'R4', project: '供应商服务状况', dept: '品质部', result: null, opinion: '', reviewer: '', time: '' },
          ];
          gmReview.value = { result: null, opinion: '', reviewer: '', time: '' };
          modalApi.setState({ showConfirmButton: true, confirmText: '提交我的评审结论', cancelText: '暂不处理' });

        } else if (data.tabType === 'processed' || data.status === 'PASSED') {
          // 状态2：已处理（或者已归档）。表格全部填满，只读。
          internalReviews.value = [
            { id: 'R1', project: '供应商规模和资历', dept: '采购部', result: 'PASS', opinion: '规模符合战略要求。', reviewer: '张采购', time: '2026-02-15 09:00' },
            { id: 'R2', project: '供应商交货保证', dept: '技术部', result: 'PASS', opinion: '产能满足交期要求，设备先进。', reviewer: '王工', time: '2026-02-15 11:30' },
            { id: 'R3', project: '供应商品质保证能力', dept: '品质部', result: 'PASS', opinion: '体系认证齐全，来料良率预估达标。', reviewer: '刘品质', time: '2026-02-15 14:00' },
            { id: 'R4', project: '供应商服务状况', dept: '品质部', result: 'PASS', opinion: '有专职售后人员对接。', reviewer: '刘品质', time: '2026-02-15 14:00' },
          ];
          gmReview.value = { result: 'PASS', opinion: '同意引入为合格供应商，转样品验证。', reviewer: '李建国', time: '2026-02-16 10:00' };
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });

        } else if (data.status === 'FILLING') {
          // 状态3：供应商还在填，审核表格为空
          internalReviews.value = [];
          modalApi.setState({ showConfirmButton: true, confirmText: '提交填报数据', cancelText: '取消' });
        }
      }
    }
  },
  async onConfirm() {
    if (isFilling.value) {
      await formRef.value?.validate();
      if(formData.value.surveyFileList.length === 0) return message.warning('必须上传调查表扫描件！');
      message.success('调查表数据已提交！');
    } else if (formData.value.tabType === 'todo') {
      // 校验我自己的那行填了没有
      const myReview = internalReviews.value.find(r => r.dept === currentUserDept.value);
      if (myReview && !myReview.result) {
        return message.warning(`请先完成您所在部门（${currentUserDept.value}）的评估结论！`);
      }
      message.success(`【${currentUserDept.value}】的评审意见已提交成功！系统将自动流转。`);
    }
    emit('success');
    modalApi.close();
  }
});

const beforeUpload = (file: any) => { message.success(`${file.name} 附件已缓存！`); return false; };
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden relative">

      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6 flex flex-col gap-6 relative">

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12 mt-2">

          <div class="text-center mb-10 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">供应商基本情况调查表</h2>
            <div v-if="formData.status === 'PASSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">审核通过</div>
          </div>

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isFilling">

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200"><span class="text-slate-500 font-bold text-base">一、企业基础概况</span></Divider>

            <div class="grid grid-cols-3 gap-6 mb-8">
              <Form.Item label="企业全称" required name="supplierName" class="col-span-2 mb-0"><Input v-model:value="formData.supplierName" placeholder="营业执照上的全称" size="large"/></Form.Item>
              <Form.Item label="经营性质分类" required name="nature" class="mb-0">
                <Select v-model:value="formData.nature" :options="[{label:'生产厂家 (原厂型)', value:'MANUFACTURER'},{label:'贸易/代理商 (代理型)', value:'AGENT'}]" class="font-bold text-indigo-700" size="large" />
              </Form.Item>

              <Form.Item label="企业注册地址" class="col-span-2 mb-0"><Input v-model:value="formData.registerAddress" /></Form.Item>
              <Form.Item label="成立时间" class="mb-0"><Input v-model:value="formData.establishDate" type="date" /></Form.Item>

              <Form.Item label="法人代表" class="mb-0"><Input v-model:value="formData.legalPerson" /></Form.Item>
              <Form.Item label="注册资金(万)" class="mb-0"><InputNumber v-model:value="formData.registeredCapital" class="w-full" /></Form.Item>
              <Form.Item label="业务联系人" required class="mb-0"><Input v-model:value="formData.contact" /></Form.Item>
            </div>

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200"><span class="text-slate-500 font-bold text-base">二、资质证照与专项清单上传</span></Divider>
            <div class="bg-blue-50/50 p-6 rounded-lg border border-blue-100 mb-8">
              <Alert message="系统规范要求：设备清单、客户名单等长篇幅内容不适合直接在线录入，请务必整理成 Excel 或 PDF 附件在此处分类上传。" type="warning" show-icon class="mb-6 bg-white shadow-sm font-bold text-orange-700" />

              <div class="grid grid-cols-2 gap-8">
                <Form.Item label="1. 《基本情况调查表》填妥盖章件 (必传)" required class="mb-0">
                  <Upload v-model:file-list="formData.surveyFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" type="primary" class="flex items-center gap-1 font-bold bg-indigo-600"><IconifyIcon icon="lucide:file-up"/> 上传表单原件扫描</Button>
                  </Upload>
                </Form.Item>

                <Form.Item label="2. 营业执照副本 (必传)" required class="mb-0">
                  <Upload v-model:file-list="formData.licenseFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" class="flex items-center gap-1 font-bold"><IconifyIcon icon="lucide:upload"/> 上传营业执照</Button>
                  </Upload>
                </Form.Item>

                <Form.Item label="3. 体系认证证书 (ISO9001/14001/IATF16949等)" class="mb-0">
                  <Upload v-model:file-list="formData.certFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" class="flex items-center gap-1"><IconifyIcon icon="lucide:award"/> 上传体系证书</Button>
                  </Upload>
                </Form.Item>

                <Form.Item label="4. HSF环保检测报告 (RoHS/REACH/卤素等)" class="mb-0">
                  <Upload v-model:file-list="formData.hsfFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" class="flex items-center gap-1"><IconifyIcon icon="lucide:leaf"/> 上传环保报告</Button>
                  </Upload>
                </Form.Item>

                <Form.Item v-if="formData.nature === 'MANUFACTURER'" label="5. 主要生产机台与检测设备清单 (原厂必传)" required class="mb-0 col-span-2">
                  <Upload v-model:file-list="formData.equipmentFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" type="dashed" class="flex items-center gap-1 border-indigo-400 text-indigo-700 font-bold bg-indigo-50"><IconifyIcon icon="lucide:settings"/> 上传设备资产台账 (Excel/PDF)</Button>
                  </Upload>
                </Form.Item>

                <Form.Item v-if="formData.nature === 'AGENT'" label="5. 原厂代理授权书 (代理商必传)" required class="mb-0 col-span-2">
                  <Upload v-model:file-list="formData.agencyFileList" :before-upload="beforeUpload" :disabled="!isFilling">
                    <Button :disabled="!isFilling" type="dashed" class="flex items-center gap-1 border-orange-400 text-orange-600 font-bold bg-orange-50"><IconifyIcon icon="lucide:shield-check"/> 上传代理授权证明</Button>
                  </Upload>
                </Form.Item>
              </div>
            </div>

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200">
              <span class="text-slate-500 font-bold text-base">三、专项指标数据提取 (用于系统检索与画像打分)</span>
            </Divider>

            <div class="p-6 rounded-lg bg-slate-50 border border-slate-200 mb-8">
              <div class="grid grid-cols-3 gap-6">
                <Form.Item
                  v-for="item in currentSchema"
                  :key="item.field"
                  :label="item.label"
                  :class="item.span === 3 ? 'col-span-3' : (item.span === 2 ? 'col-span-2' : 'col-span-1')"
                  class="mb-0"
                >
                  <Input v-if="item.type === 'Input'" v-model:value="formData[item.field]" :placeholder="item.placeholder" />
                  <InputNumber v-else-if="item.type === 'InputNumber'" v-model:value="formData[item.field]" class="w-full" :placeholder="item.placeholder" />
                  <Select v-else-if="item.type === 'Select'" v-model:value="formData[item.field]" :options="item.options" class="w-full" />
                  <Input.TextArea v-else-if="item.type === 'TextArea'" v-model:value="formData[item.field]" :rows="2" :placeholder="item.placeholder" />
                </Form.Item>
              </div>
            </div>
          </Form>

          <template v-if="internalReviews.length > 0">
            <Divider orientation="left" class="!m-0 !mb-6 border-indigo-200">
              <span class="text-indigo-800 font-black text-lg flex items-center gap-2">
                  <IconifyIcon icon="lucide:users"/> 禾臣内部审查结论表
              </span>
            </Divider>

            <div class="mb-6">
              <table class="w-full border-collapse text-sm text-left bg-white shadow-sm border border-slate-300">
                <thead class="bg-slate-100 text-slate-700 font-bold">
                <tr>
                  <th class="border border-slate-300 p-3 w-[22%]">评估项目</th>
                  <th class="border border-slate-300 p-3 w-[12%] text-center">责任部门</th>
                  <th class="border border-slate-300 p-3 w-[22%] text-center">评估结论</th>
                  <th class="border border-slate-300 p-3 w-[34%]">评估意见及理由</th>
                  <th class="border border-slate-300 p-3 w-[10%] text-center">经办人</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(review, idx) in internalReviews" :key="review.id"
                    :class="[review.dept === currentUserDept && formData.tabType === 'todo' ? 'bg-orange-50/60' : 'hover:bg-slate-50 transition-colors']">

                  <td class="border border-slate-300 p-3 font-bold text-slate-700">
                    <span class="text-slate-400 mr-1">{{ idx + 1 }}.</span>{{ review.project }}
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <Tag :color="review.dept === currentUserDept ? 'processing' : 'default'" class="!m-0 font-bold">{{ review.dept }}</Tag>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <div v-if="review.dept === currentUserDept && formData.tabType === 'todo'">
                      <Radio.Group v-model:value="review.result" size="small" button-style="solid" class="flex w-full">
                        <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">合格</Radio.Button>
                        <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">不合格</Radio.Button>
                      </Radio.Group>
                    </div>
                    <div v-else>
                      <Tag v-if="review.result === 'PASS'" color="success" class="!m-0 font-bold border-none px-4">合格</Tag>
                      <Tag v-else-if="review.result === 'REJECT'" color="error" class="!m-0 font-bold border-none px-4">不合格</Tag>
                      <span v-else class="text-slate-400 text-xs">- 待审 -</span>
                    </div>
                  </td>

                  <td class="border border-slate-300 p-3">
                    <div v-if="review.dept === currentUserDept && formData.tabType === 'todo'">
                      <Input v-model:value="review.opinion" placeholder="请填写结论及详细理由..." size="small" class="w-full !border-orange-300 bg-white" />
                    </div>
                    <div v-else class="text-xs text-slate-600 block leading-relaxed" :title="review.opinion">
                      {{ review.opinion || '-' }}
                    </div>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <span class="text-xs font-bold text-slate-800 block">{{ review.reviewer || '-' }}</span>
                    <span class="text-[10px] text-slate-400">{{ review.time ? review.time.substring(5,10) : '' }}</span>
                  </td>
                </tr>

                <tr class="bg-indigo-50/50 border-t-2 border-indigo-200">
                  <td class="border border-slate-300 p-4 font-black text-indigo-800">
                    <IconifyIcon icon="lucide:stamp" class="inline mr-1"/> 总经理最终核准
                  </td>
                  <td class="border border-slate-300 p-4 text-center">
                    <Tag color="purple" class="!m-0 font-bold">高管层</Tag>
                  </td>
                  <td class="border border-slate-300 p-4 text-center">
                    <div v-if="formData.status === 'PASSED' || gmReview.result">
                      <Tag v-if="gmReview.result === 'PASS'" color="success" class="!m-0 font-bold border-none px-4">合格批准</Tag>
                      <Tag v-else-if="gmReview.result === 'REJECT'" color="error" class="!m-0 font-bold border-none px-4">否决</Tag>
                    </div>
                    <div v-else>
                      <span class="text-slate-400 text-xs">- 等待前置完成 -</span>
                    </div>
                  </td>
                  <td class="border border-slate-300 p-4 text-xs font-bold text-indigo-700">
                    {{ gmReview.opinion || '-' }}
                  </td>
                  <td class="border border-slate-300 p-4 text-center text-xs font-bold text-slate-800">
                    {{ gmReview.reviewer || '-' }}
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </template>

          <div v-if="!isNew" class="flex justify-center pb-2 mt-6">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看单据审核流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="单据流转与审核日志" placement="right" :width="450">
      <div class="bg-indigo-50 border border-indigo-100 p-3 rounded text-indigo-800 text-sm mb-6 flex items-start gap-2">
        <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
        <span>当前处理阶段： <b>多部门协同初评</b>。</span>
      </div>
      <Steps direction="vertical" :current="2" size="small" class="px-2">
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
