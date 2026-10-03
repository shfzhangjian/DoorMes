<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Descriptions, Tag, Button, Upload, Form, Input, Select, message, Alert, Divider } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

// 引入供应商选择弹窗
import SupplierSelectModalVue from './supplier-select-modal.vue';

const emit = defineEmits(['success']);
const mode = ref<'create' | 'approve' | 'view'>('view');
const recordData = ref<any>({});
const fileList = ref<any[]>([]);

// 注册供应商选择器
const [SupplierSelectModal, supplierSelectModalApi] = useVbenModal({ connectedComponent: SupplierSelectModalVue });

const formData = reactive({
  supplierCode: '',
  supplierName: '',
  protocolType: undefined,
  protocolName: ''
});

// 类型中文翻译映射字典
const typeNameMap: Record<string, string> = {
  'AGREEMENT': '商业协议 (AGREEMENT)',
  'NDA': '保密协议 (NDA)',
  'QAA': '质量协议 (QAA)',
  'ISO9001': '体系认证 (ISO/IATF)',
  'ROHS': '环保声明 (ROHS/REACH)'
};

const [Modal, modalApi] = useVbenModal({
  title: computed(() => mode.value === 'create' ? '✨ 登记新合规档案' : (mode.value === 'approve' ? '⚖️ 合规档案核准工作台' : '📜 档案详情与维护')),
  onOpenChange: (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<any>();
      mode.value = data.mode;
      recordData.value = data;
      fileList.value = [];

      if (mode.value === 'create') {
        formData.supplierCode = '';
        formData.supplierName = '';
        formData.protocolType = undefined;
        formData.protocolName = '';
        modalApi.setState({ showConfirmButton: true, confirmText: '提交归档审批', cancelText: '取消' });
      } else if (mode.value === 'view') {
        modalApi.setState({ showConfirmButton: false, cancelText: '关闭视窗' });
      } else if (mode.value === 'approve') {
        modalApi.setState({ showConfirmButton: false, cancelText: '暂不处理' });
      }
    }
  },
  onConfirm: async () => {
    if (mode.value === 'create') {
      if (!formData.supplierName || !formData.protocolType || !formData.protocolName) {
        message.warning('请完整填写供方及档案信息！');
        return;
      }
      if (fileList.value.length === 0) {
        message.warning('必须上传协议或资质的扫描件！');
        return;
      }
      message.success('档案登记成功，已流转至 SQE/法务 审批节点！');
      emit('success', { action: 'create', payload: formData });
      modalApi.close();
    }
  }
});

// 处理供应商被选中后的回填
function handleSupplierSelected(row: any) {
  formData.supplierCode = row.supplierCode;
  formData.supplierName = row.supplierName;
  message.success(`已关联：${row.supplierName}`);
}

function handleApprovePass() {
  message.success('审批通过！档案已正式生效归档。');
  emit('success', { action: 'approve', payload: recordData.value });
  modalApi.close();
}

function handleApproveReject() {
  message.error('已驳回！要求供应商重新上传文件。');
  modalApi.close();
}

function handleRenew() {
  if (fileList.value.length === 0) {
    message.warning('请先选择需要更新的扫描件！');
    return;
  }
  message.success('续签文件上传成功，等待重新审核！');
  emit('success', { action: 'renew', payload: recordData.value });
  modalApi.close();
}
</script>

<template>
  <Modal class="w-[1000px]">
    <SupplierSelectModal @select="handleSupplierSelected" />

    <div class="flex flex-col h-full min-h-[60vh] w-full bg-[#f4f6f8] p-6 overflow-hidden">
      <div class="max-w-5xl mx-auto w-full flex flex-col gap-4 relative">
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-10 relative overflow-hidden">

          <div class="text-center mb-8 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">
              {{ mode === 'create' ? '供应商合规资质登记表' : '合规与协议详情档案' }}
            </h2>
            <div v-if="mode !== 'create'" class="absolute right-0 top-0 text-sm text-slate-400 font-mono">
              内部档案号：PRT-{{ recordData.id?.padStart(6, '0') || '***' }}
            </div>
          </div>

          <div v-if="recordData.status === 'VALID'" class="absolute top-10 right-16 text-5xl text-green-500 opacity-10 border-[6px] border-green-500 rounded-full px-6 py-3 rotate-12 font-black tracking-widest pointer-events-none">生效中</div>
          <div v-else-if="recordData.status === 'EXPIRED'" class="absolute top-10 right-16 text-5xl text-red-500 opacity-10 border-[6px] border-red-500 rounded-full px-6 py-3 rotate-12 font-black tracking-widest pointer-events-none">已失效</div>

          <Alert v-if="recordData.status === 'EXPIRED'" type="error" show-icon class="mb-6 font-bold border-red-300">
            <template #message>系统交易阻断警告 (Trade Blocked)</template>
            <template #description>该档案已于 <span class="underline">{{ recordData.expiryDate }}</span> 过期，系统已自动限制 ERP 向该供方下达新采购订单！</template>
          </Alert>
          <Alert v-else-if="recordData.status === 'WARNING'" type="warning" show-icon class="mb-6 font-bold border-orange-300">
            <template #message>资质临期预警 (Expiring Soon)</template>
            <template #description>距离失效仅剩 <span class="text-red-600 text-lg mx-1">{{ recordData.daysLeft }}</span> 天，请立即推进换证续签，以免影响后续合作。</template>
          </Alert>

          <Form v-if="mode === 'create'" layout="vertical">
            <Divider orientation="left" class="!m-0 !mb-5 border-slate-200"><span class="text-slate-500 font-bold">一、 归档主体与内容</span></Divider>

            <div class="grid grid-cols-2 gap-x-8 gap-y-2 mb-4">
              <Form.Item label="关联供应商主体 (从名录中选择)" required class="col-span-2">
                <Input.Search
                  v-model:value="formData.supplierName"
                  placeholder="点击右侧按钮选择关联主体..."
                  size="large"
                  readonly
                  enter-button="选择主体"
                  @search="supplierSelectModalApi.open()"
                />
              </Form.Item>
              <Form.Item label="档案分类" required>
                <Select v-model:value="formData.protocolType" placeholder="请选择类型" size="large" :options="[
                  { label: '商业协议 (AGREEMENT)', value: 'AGREEMENT' },
                  { label: '保密协议 (NDA)', value: 'NDA' },
                  { label: '质量协议 (QAA)', value: 'QAA' },
                  { label: '体系认证 (ISO9001/14001)', value: 'ISO9001' },
                  { label: '环保声明 (ROHS/REACH)', value: 'ROHS' }
                ]" />
              </Form.Item>
              <Form.Item label="档案名称/摘要" required>
                <Input v-model:value="formData.protocolName" placeholder="例如：2026版环保承诺书" size="large" />
              </Form.Item>
            </div>

            <Divider orientation="left" class="!m-0 !mb-5 border-slate-200"><span class="text-slate-500 font-bold">二、 电子扫描件上传</span></Divider>
            <Form.Item required>
              <Upload.Dragger v-model:file-list="fileList" action="/api/upload" :max-count="1" class="bg-slate-50 border-dashed">
                <p class="ant-upload-drag-icon flex justify-center text-5xl text-indigo-400 mt-4"><IconifyIcon icon="lucide:cloud-upload" /></p>
                <p class="ant-upload-text font-bold text-slate-600 mt-3 text-lg">点击或拖拽文件到此处上传</p>
                <p class="ant-upload-hint text-sm text-slate-400 mb-4">支持 PDF/JPG 格式，要求必须包含双方清晰签章</p>
              </Upload.Dragger>
            </Form.Item>
          </Form>

          <div v-else>
            <Divider orientation="left" class="!m-0 !mb-5 border-slate-200"><span class="text-slate-500 font-bold">一、 供方主体确认</span></Divider>
            <Descriptions :column="2" bordered size="small" class="mb-8">
              <Descriptions.Item label="供应商名称"><span class="font-bold text-slate-800">{{ recordData.supplierName }}</span></Descriptions.Item>
              <Descriptions.Item label="供应商代码"><span class="font-mono text-slate-500">{{ recordData.supplierCode }}</span></Descriptions.Item>
            </Descriptions>

            <Divider orientation="left" class="!m-0 !mb-5 border-slate-200"><span class="text-slate-500 font-bold">二、 档案效期明细</span></Divider>
            <Descriptions :column="2" bordered size="small" class="mb-8">
              <Descriptions.Item label="档案类型"><Tag color="blue" class="border-none font-bold">{{ typeNameMap[recordData.protocolType] || recordData.protocolType }}</Tag></Descriptions.Item>
              <Descriptions.Item label="档案摘要"><span class="font-bold">{{ recordData.protocolName }}</span></Descriptions.Item>

              <Descriptions.Item label="生效日期">
                <span class="font-mono">{{ recordData.effectDate !== '-' ? recordData.effectDate : '核准后生成' }}</span>
              </Descriptions.Item>
              <Descriptions.Item label="失效日期">
                <span class="font-mono font-bold" :class="recordData.status === 'EXPIRED' ? 'text-red-600' : ''">
                  {{ recordData.expiryDate !== '-' ? recordData.expiryDate : '核准后生成' }}
                </span>
              </Descriptions.Item>

              <Descriptions.Item label="在档扫描件" :span="2">
                <a class="flex items-center gap-2 text-indigo-600 font-bold hover:underline w-max">
                  <IconifyIcon icon="lucide:file-pdf" class="text-red-500 text-lg" />
                  {{ recordData.attachment }}
                  <span class="text-xs font-normal text-slate-400 ml-2">(点击在线预览)</span>
                </a>
              </Descriptions.Item>
            </Descriptions>

            <div v-if="mode === 'approve'" class="bg-indigo-50 border border-indigo-100 p-6 rounded-lg">
              <div class="font-black text-indigo-800 mb-4 flex items-center gap-2 text-base">
                <IconifyIcon icon="lucide:pen-tool"/> 合规核准与归档操作
              </div>
              <div class="text-sm text-indigo-600 mb-6">请确认随附的扫描件已清晰盖章并符合法规要求，核准后将自动激活该资质的效期计算。</div>
              <div class="flex justify-end gap-4">
                <Button danger size="large" class="w-32" @click="handleApproveReject"><IconifyIcon icon="lucide:x" class="mr-1" /> 驳回重传</Button>
                <Button type="primary" size="large" class="w-40 bg-green-600 hover:bg-green-500 border-none" @click="handleApprovePass"><IconifyIcon icon="lucide:check-check" class="mr-1" /> 确认生效归档</Button>
              </div>
            </div>

            <div v-if="mode === 'view' && (recordData.status === 'WARNING' || recordData.status === 'EXPIRED')" class="bg-orange-50 p-6 rounded-lg border border-orange-100">
              <div class="font-black text-orange-800 mb-4 flex items-center gap-2 text-base">
                <IconifyIcon icon="lucide:refresh-cw" /> 发起换证续签流程
              </div>
              <div class="text-sm text-orange-700 mb-4">请将供应商提供的最新有效签章文件上传至此，提交后将重新发起 SQE/法务 核准流程。</div>
              <div class="flex items-center gap-4">
                <Upload v-model:file-list="fileList" action="/api/upload" :max-count="1" class="flex-1">
                  <Button type="default" size="large" class="w-full text-left bg-white border-orange-200 text-slate-500 hover:text-orange-600 hover:border-orange-400">
                    <IconifyIcon icon="lucide:paperclip" class="mr-2" /> 点击选择最新的 PDF/JPG 文件...
                  </Button>
                </Upload>
                <Button type="primary" size="large" class="bg-orange-500 hover:bg-orange-400 border-none" @click="handleRenew">提交续签审核</Button>
              </div>
            </div>

          </div>

        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
:deep(.ant-descriptions-bordered .ant-descriptions-item-label) {
  background-color: #f8fafc; color: #475569; font-weight: bold; width: 140px; text-align: center;
}
</style>
