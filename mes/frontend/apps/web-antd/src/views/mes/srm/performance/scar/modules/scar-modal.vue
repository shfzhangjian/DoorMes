<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, Select, Radio, Divider, message, Drawer, Upload, Alert, Descriptions } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formData = ref<any>({});
const formRef = ref();
const drawerVisible = ref(false);

const isEditable = computed(() => isNew.value || formData.value.status === 'DRAFT');

// 审批控制
const approvalForm = ref({ result: 'PASS', opinion: '', nextStep: 'DEFAULT' });
const processSteps = ref<any[]>([]);

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 发起 SCAR 供方异常整改' : '📝 SCAR 与 8D 报告评审工作台'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;
      approvalForm.value = { result: 'PASS', opinion: '', nextStep: 'DEFAULT' };

      if (isNew.value) {
        formData.value = { status: 'DRAFT', applyDate: dayjs().format('YYYY-MM-DD'), level: 'Major (严重)', fileList: [], eightD: {} };
        processSteps.value = [{ title: '拟定 SCAR 申请', desc: '当前状态: 草稿', status: 'process' }];
        modalApi.setState({ showConfirmButton: true, confirmText: '下发给供应商', cancelText: '保存草稿' });
      } else {
        // Mock 赋予初始数据
        formData.value = {
          ...data,
          eightD: data.eightD || { d1_team: '品质总监', d3_containment: '冻结库存', d4_rootCause: '模具磨损', d5_action: '修模' },
          fileList: [{uid: '1', name: '异常客诉联络单.pdf', status: 'done', url: '#'}]
        };

        if (data.status === 'CLOSED') {
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
        } else if (data.tabType === 'todo') {
          modalApi.setState({ showConfirmButton: true, confirmText: '提交评审结论', cancelText: '暂不处理' });
        } else {
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
        }

        processSteps.value = [
          { title: '发起 SCAR', desc: 'SQE 工程师', status: 'finish' },
          { title: '供方提交 8D', desc: data.status === 'WAIT_SUPPLIER' ? '当前节点' : '已提交', status: data.status === 'WAIT_SUPPLIER' ? 'process' : 'finish' },
          { title: 'SQE 方案评审', desc: data.status === 'QA_REVIEW' ? '当前节点' : (data.status === 'VERIFYING' || data.status === 'CLOSED' ? '已通过' : '等待流转'), status: data.status === 'QA_REVIEW' ? 'process' : (data.status === 'VERIFYING' || data.status === 'CLOSED' ? 'finish' : 'wait') },
          { title: '现场/下批验证', desc: data.status === 'VERIFYING' ? '当前节点' : '等待流转', status: data.status === 'VERIFYING' ? 'process' : (data.status === 'CLOSED' ? 'finish' : 'wait') },
          { title: '闭环归档', desc: '流程结束', status: data.status === 'CLOSED' ? 'finish' : 'wait' }
        ];
      }
    }
  },
  async onConfirm() {
    if (isNew.value || formData.value.status === 'DRAFT') {
      await formRef.value?.validate();
      message.success('SCAR 异常单已下发给供方，进入限期整改流程！');
    } else {
      if (approvalForm.value.result === 'REJECT' && !approvalForm.value.opinion) {
        return message.warning('驳回必须填写评审意见！');
      }
      message.success(`评审流转成功！单据状态已更新。`);
    }
    emit('success');
    modalApi.close();
  }
});

const beforeUpload = (file: any) => { message.success(`${file.name} 附件已挂接！`); return false; };
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden">
      <div class="max-w-6xl mx-auto w-full flex flex-col gap-6 relative flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6">

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12 mt-2">

          <div class="text-center mb-8 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">SCAR 异常整改单 (8D Report)</h2>
            <div v-if="formData.status === 'CLOSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">已验证闭环</div>
            <div v-if="formData.isOverdue" class="absolute -top-4 left-10 text-6xl text-red-500 opacity-20 border-4 border-red-500 rounded-full px-4 py-2 -rotate-12 font-black tracking-widest">逾期违约</div>
          </div>

          <Alert v-if="formData.isOverdue" message="该 SCAR 单已逾期！系统已自动扣除该供方当月绩效【品质配合度】得分！" type="error" show-icon class="mb-6 font-bold" />

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isEditable">

            <Divider orientation="left" class="!m-0 !mb-4 border-slate-200"><span class="text-slate-500 font-bold">一、 异常基础信息 (D2 描述)</span></Divider>
            <div class="bg-slate-50 p-6 rounded-lg border border-slate-100 mb-6">
              <div class="grid grid-cols-3 gap-x-6 gap-y-4">
                <Form.Item label="供方名称" required name="supplierName" class="col-span-2 mb-0"><Input v-model:value="formData.supplierName" placeholder="被考核的供应商全称" size="large"/></Form.Item>
                <Form.Item label="异常来源" required name="source" class="mb-0">
                  <Select v-model:value="formData.source" :options="[{label:'IQC检验', value:'IQC检验'}, {label:'产线抛料', value:'产线抛料'}, {label:'季度绩效', value:'季度绩效'}]" size="large"/>
                </Form.Item>

                <Form.Item label="发起部门" required name="applyDept" class="mb-0"><Input v-model:value="formData.applyDept" placeholder="如：品质部 / PMC部"/></Form.Item>
                <Form.Item label="严重缺陷等级" required name="level" class="mb-0">
                  <Radio.Group v-model:value="formData.level" button-style="solid" class="flex">
                    <Radio.Button value="Minor (轻微)" class="flex-1 text-center bg-slate-50 border-slate-200 !text-slate-600">轻微</Radio.Button>
                    <Radio.Button value="Major (严重)" class="flex-1 text-center bg-orange-50 border-orange-200 !text-orange-600">严重</Radio.Button>
                    <Radio.Button value="Critical (致命)" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700">致命</Radio.Button>
                  </Radio.Group>
                </Form.Item>
                <Form.Item label="要求回复期限" required name="dueDate" class="mb-0"><Input v-model:value="formData.dueDate" type="date" /></Form.Item>

                <Form.Item label="详细异常现象描述" required name="issueDesc" class="col-span-3 mb-0">
                  <Input.TextArea v-model:value="formData.issueDesc" :rows="2" placeholder="详细陈述发生的时间、地点、批号及不良现象..." />
                </Form.Item>
              </div>
            </div>

            <div class="mb-8">
              <Form.Item label="上传异常佐证附件" class="mb-0 font-bold text-slate-600">
                <Upload v-model:file-list="formData.fileList" :before-upload="beforeUpload" :disabled="!isEditable">
                  <Button :disabled="!isEditable" class="flex items-center gap-1 font-bold border-indigo-300 text-indigo-600 bg-white"><IconifyIcon icon="lucide:paperclip"/> 上传不良图片/NCR单</Button>
                </Upload>
              </Form.Item>
            </div>
          </Form>

          <template v-if="!isNew">
            <Divider orientation="left" class="!m-0 !mb-6 border-indigo-200">
              <span class="text-indigo-800 font-black text-lg flex items-center gap-2">
                  <IconifyIcon icon="lucide:clipboard-signature"/> 二、 供方 8D 整改方案答复
              </span>
            </Divider>

            <div v-if="formData.status === 'WAIT_SUPPLIER'" class="text-center py-10 bg-slate-50 border border-dashed border-slate-300 rounded-lg mb-8">
              <IconifyIcon icon="lucide:clock-4" class="text-5xl text-slate-300 mb-3" />
              <div class="text-slate-500 font-bold">供应商尚未提交 8D 报告，请耐心等待或在线催办。</div>
            </div>

            <div v-else class="mb-8">
              <Descriptions bordered size="small" :column="1" class="bg-white shadow-sm">
                <Descriptions.Item label="D1: 成立改善团队 (Team)"><span class="font-bold">{{ formData.eightD.d1_team }}</span></Descriptions.Item>
                <Descriptions.Item label="D3: 临时围堵措施 (Containment)">
                  <span class="text-orange-600 font-bold">{{ formData.eightD.d3_containment }}</span>
                </Descriptions.Item>
                <Descriptions.Item label="D4: 根本原因分析 (Root Cause)">
                  <div class="bg-red-50 text-red-700 p-2 rounded border border-red-100">{{ formData.eightD.d4_rootCause }}</div>
                </Descriptions.Item>
                <Descriptions.Item label="D5: 永久纠正措施 (Corrective)">
                  <div class="bg-green-50 text-green-700 p-2 rounded border border-green-100 font-bold">{{ formData.eightD.d5_action }}</div>
                </Descriptions.Item>
                <Descriptions.Item label="举证附件 (Evidence)">
                  <a class="text-blue-500 hover:underline font-bold"><IconifyIcon icon="lucide:paperclip" class="inline"/> 8D_SOP更新及培训记录.pdf</a>
                </Descriptions.Item>
              </Descriptions>
            </div>

            <div v-if="formData.tabType === 'todo' && (formData.status === 'QA_REVIEW' || formData.status === 'VERIFYING')" class="bg-blue-50/40 p-6 rounded-lg border border-blue-200 mb-6">
              <div class="font-black text-blue-800 mb-4 flex items-center gap-2 text-lg">
                <IconifyIcon icon="lucide:check-square-2" /> {{ formData.status === 'QA_REVIEW' ? '三、 SQE / 采购工程 方案评审' : '三、 SQE 纠正措施效果验证' }}
              </div>

              <div class="flex items-center gap-4 mb-4">
                <span class="font-bold text-slate-700">处理结论：</span>
                <Radio.Group v-model:value="approvalForm.result" size="large" button-style="solid" class="flex w-[400px]">
                  <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">
                    {{ formData.status === 'QA_REVIEW' ? '方案可行 (转下批验证)' : '未再复发 (同意闭环)' }}
                  </Radio.Button>
                  <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">驳回重写/验证失败</Radio.Button>
                </Radio.Group>
              </div>
              <Input.TextArea v-model:value="approvalForm.opinion" :rows="3" placeholder="填写验证记录及结论结论，如驳回需说明要求补充的方向..." class="mb-4" />
            </div>
          </template>

          <div v-if="!isNew" class="flex justify-center pb-2 mt-4">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看完整 SCAR 流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="SCAR 审批流转记录" placement="right" :width="450">
      <div class="bg-indigo-50 border border-indigo-100 p-3 rounded text-indigo-800 text-sm mb-6 flex items-start gap-2">
        <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
        <span>当前停留在 <b>{{ formData.node || '流程处理中' }}</b>。</span>
      </div>
      <Steps direction="vertical" :current="processSteps.findIndex(s => s.status === 'process') === -1 ? 4 : processSteps.findIndex(s => s.status === 'process')" size="small" class="px-2">
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
