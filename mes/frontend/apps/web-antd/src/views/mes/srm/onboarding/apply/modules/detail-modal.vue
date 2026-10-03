<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, Select, Checkbox, Radio, Row, Col, Divider, message, Drawer } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formData = ref<any>({});
const formRef = ref();

// 💡 抽屉控制器
const drawerVisible = ref(false);

// 💡 审批流转控制表单
const approvalForm = ref({
  result: 'PASS',
  opinion: '',
  nextStep: 'DEFAULT', // DEFAULT=按流程图默认, CUSTOM=自定义跳转
  customStepId: undefined,
  assignees: [] // 指定下个节点办理人 (可多选)
});

const userList = [
  { label: '张海波 (采购总监)', value: 'U01' }, { label: '李建国 (总经理)', value: 'U02' },
  { label: '王大雷 (研发主管)', value: 'U03' }, { label: '刘品质 (QA主管)', value: 'U04' }
];

const stepList = [
  { label: '采购总监审核', value: 'STEP_CGZJ' },
  { label: '总经理批准', value: 'STEP_ZJL' },
  { label: '退回至发起人修改', value: 'STEP_REJECT_INIT' }
];

// 模拟流程审批历史
const processSteps = [
  { title: '发起申请', desc: '张采购 - 2026-02-19 09:00', status: 'finish' },
  { title: '品质部 / 研发部 (并行会签)', desc: '当前节点 (品质部已批 / 研发部待批)', status: 'process' },
  { title: '采购总监审核', desc: '等待流转', status: 'wait' },
  { title: '总经理批准', desc: '等待流转', status: 'wait' },
];

// 💡 核心：使用 !w-[90vw] 强制约束宽度为 90% 并利用 Vben 框架原生居中特性
const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 发起新供应商导入申请' : '📑 供应商准入审批工作台'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;

      // 重置流转表单
      approvalForm.value = { result: 'PASS', opinion: '', nextStep: 'DEFAULT', customStepId: undefined, assignees: [] };

      if (isNew.value) {
        formData.value = { applyReason: [], specReq: '', supplierName: '', nature: undefined, scale: undefined, certs: [], origin: undefined, advantage: '' };
        modalApi.setState({ showConfirmButton: true, confirmText: '提交审批流', cancelText: '保存草稿' });
      } else {
        formData.value = { ...data };
        if (data.tabType === 'todo') {
          modalApi.setState({ showConfirmButton: true, confirmText: '确认办理并流转', cancelText: '暂不处理' });
        } else {
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
        }
      }
    }
  },
  async onConfirm() {
    if (isNew.value) {
      await formRef.value?.validate();
      message.success('申请单已提交，流程启动！');
    } else {
      if (!approvalForm.value.opinion && approvalForm.value.result === 'REJECT') {
        message.error('驳回操作必须填写审批意见！');
        return;
      }
      message.success(`审批流转成功！任务已推送到下一节点。`);
    }
    emit('success');
    modalApi.close();
  }
});
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden">

      <div class="max-w-6xl mx-auto w-full flex flex-col gap-6 relative">

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12">

          <div class="text-center mb-8 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">新供应商导入申请表</h2>

            <div v-if="formData.status === 'PASSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">已批准</div>
          </div>

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isNew && formData.tabType !== 'todo'">
            <Divider orientation="left" class="!m-0 !mb-4 border-slate-200"><span class="text-slate-500 font-bold">一、 寻源背景与申请原因</span></Divider>
            <div class="bg-blue-50/50 p-6 rounded-lg border border-blue-100 mb-6">
              <Form.Item label="新供应商申请原因 (可多选)" required name="applyReason">
                <Checkbox.Group v-model:value="formData.applyReason" class="w-full">
                  <Row :gutter="[16, 16]">
                    <Col :span="8"><Checkbox value="1">生产能力不满足，提升产能</Checkbox></Col>
                    <Col :span="8"><Checkbox value="2">品质状况不满足，提升品质</Checkbox></Col>
                    <Col :span="8"><Checkbox value="3">缩短交期</Checkbox></Col>
                    <Col :span="8"><Checkbox value="4">现价格无优势，价格需下调</Checkbox></Col>
                    <Col :span="8"><Checkbox value="5">采购需开发二供三供防断料</Checkbox></Col>
                  </Row>
                </Checkbox.Group>
              </Form.Item>
              <Form.Item label="特殊加工工艺要求 (详情)" class="mb-0">
                <Input.TextArea v-model:value="formData.specReq" :rows="2" placeholder="填写对新供应商的技术期许..." />
              </Form.Item>
            </div>

            <Divider orientation="left" class="!m-0 !mb-4 border-slate-200"><span class="text-slate-500 font-bold">二、 意向供应商概况要求</span></Divider>
            <div class="grid grid-cols-2 gap-x-8 gap-y-2">
              <Form.Item label="意向供应商名称" required name="supplierName"><Input v-model:value="formData.supplierName" placeholder="全称..."/></Form.Item>
              <Form.Item label="合作产品/服务" required><Input v-model:value="formData.materialName" placeholder="如：特种树脂"/></Form.Item>

              <Form.Item label="企业性质要求">
                <Select v-model:value="formData.nature" :options="[{label:'国有企业', value:'国有'},{label:'民营企业', value:'民营'},{label:'外资合资', value:'外资'}]" />
              </Form.Item>
              <Form.Item label="目标体系认证情况">
                <Checkbox.Group v-model:value="formData.certs" :options="[{label:'ISO9001', value:'iso9001'},{label:'ISO14001', value:'iso14001'},{label:'IATF16949', value:'iatf'}]" />
              </Form.Item>
            </div>

            <div v-if="!isNew && formData.tabType === 'todo'" class="mt-8 pt-6 border-t-2 border-dashed border-indigo-200">
              <div class="font-black text-indigo-800 mb-4 flex items-center gap-2 text-lg">
                <IconifyIcon icon="lucide:arrow-right-circle"/> 工作流办理与流转配置 (Process Handling)
              </div>

              <div class="bg-indigo-50/50 p-6 rounded-xl border border-indigo-100">
                <div class="grid grid-cols-2 gap-6">
                  <div class="flex flex-col gap-4">
                    <Form.Item label="办理结论" class="mb-0 font-bold">
                      <Radio.Group v-model:value="approvalForm.result" button-style="solid" class="w-full flex">
                        <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">同意</Radio.Button>
                        <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">驳回 / 拒绝</Radio.Button>
                      </Radio.Group>
                    </Form.Item>
                    <Form.Item label="审批意见" class="mb-0 font-bold">
                      <Input.TextArea v-model:value="approvalForm.opinion" :rows="3" placeholder="请填写办理意见..." />
                    </Form.Item>
                  </div>

                  <div class="flex flex-col gap-4 pl-6 border-l border-indigo-100">
                    <Form.Item label="下步流转节点" class="mb-0 font-bold">
                      <Select v-model:value="approvalForm.nextStep" class="w-full">
                        <Select.Option value="DEFAULT">走默认流程 (交由下一节点人员办理)</Select.Option>
                        <Select.Option value="CUSTOM">手动更改下个环节 (跳转或退回)</Select.Option>
                      </Select>
                    </Form.Item>

                    <Form.Item v-if="approvalForm.nextStep === 'CUSTOM'" label="请选择目标节点" class="mb-0">
                      <Select v-model:value="approvalForm.customStepId" :options="stepList" placeholder="选择具体节点" class="w-full" />
                    </Form.Item>

                    <Form.Item label="加签/指定下步办理人" class="mb-0 font-bold" extra="如果不选，则由系统根据岗位角色自动分配。">
                      <Select mode="multiple" v-model:value="approvalForm.assignees" :options="userList" placeholder="可选择一个或多个办理人..." class="w-full" />
                    </Form.Item>
                  </div>
                </div>
              </div>
            </div>

          </Form>

          <div v-if="!isNew" class="mt-8 flex justify-center">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看完整审批流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="📜 单据流转追踪与审批日志" placement="right" :width="450">
      <div class="bg-indigo-50 border border-indigo-100 p-3 rounded text-indigo-800 text-sm mb-6 flex items-start gap-2">
        <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
        <span>当前流程已耗时 <b>2小时30分</b>，正处于 <b>品质部/研发部(并行会签)</b> 节点。</span>
      </div>

      <Steps direction="vertical" :current="1" size="small" class="px-2">
        <Steps.Step v-for="(step, i) in processSteps" :key="i" :title="step.title" :status="step.status as any">
          <template #description>
            <div class="text-xs mt-2 mb-6 p-3 rounded-lg shadow-sm border" :class="step.status === 'process' ? 'bg-white border-indigo-200' : 'bg-slate-50 border-slate-100 text-slate-500'">
              <div class="font-bold flex justify-between">
                <span :class="step.status === 'process' ? 'text-indigo-600' : 'text-slate-600'">{{ step.desc }}</span>
              </div>
              <div v-if="step.status === 'finish'" class="mt-1 text-slate-400">审批意见：情况属实，同意引入评估。</div>
              <div v-if="step.status === 'process'" class="mt-1 text-orange-500">提示：研发部 [王工] 尚未处理，请督办。</div>
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
:deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)) {
  border-color: currentColor !important;
}
</style>
