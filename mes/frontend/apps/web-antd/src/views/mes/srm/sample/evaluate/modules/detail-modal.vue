<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Tag, Button, Steps, Form, Input, Select, Radio, Checkbox, Divider, message, Drawer, Row, Col } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formRef = ref();

// 基础表单数据
const formData = ref<any>({ verifyType: [], testItems: [] });

// ============================================================================
// 💡 核心机制 1：样品测试实测数据记录表 (5次测量)
// ============================================================================
const testDataList = ref<any[]>([]);

function addTestData() {
  testDataList.value.push({ id: Date.now(), item: '', spec: '', t1: '', t2: '', t3: '', t4: '', t5: '', result: 'PASS' });
}
function removeTestData(idx: number) {
  testDataList.value.splice(idx, 1);
}

// ============================================================================
// 💡 核心机制 2：五部门联合评价意见表 (严格对应PDF要求)
// ============================================================================
const currentUserDept = ref('技术部'); // 模拟当前登录人部门

const internalReviews = ref([
  { id: 'D1', dept: '技术部', result: null, opinion: '', reviewer: '', time: '', editable: true },
  { id: 'D2', dept: '品质部', result: null, opinion: '', reviewer: '', time: '', editable: false },
  { id: 'D3', dept: '生产部', result: null, opinion: '', reviewer: '', time: '', editable: false },
  { id: 'D4', dept: '采购部', result: null, opinion: '', reviewer: '', time: '', editable: false },
  { id: 'D5', dept: '市场部', result: null, opinion: '', reviewer: '', time: '', editable: false },
]);

// 最终批准
const finalApproval = ref({ result: null, opinion: '', reviewer: '', time: '', editable: false });
const approvalAction = ref({ nextStep: 'DEFAULT', customStepId: undefined });

const drawerVisible = ref(false);
const processSteps = [
  { title: '发起送样需求', desc: '采购部下达任务 - 2026-02-19 10:00', status: 'finish' },
  { title: '供应商送样', desc: '样品已签收入库 - 2026-02-20 08:30', status: 'finish' },
  { title: '各部门测试与会签', desc: '当前节点 (技术部测定中)', status: 'process' },
  { title: '最终判定批准', desc: '等待流转', status: 'wait' }
];

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 下发首批样品评价任务' : '🔬 采购首批样品评价工作台'),
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      drawerVisible.value = false;

      if (isNew.value) {
        formData.value = { verifyType: [], testItems: [] };
        testDataList.value = [
          { id: 1, item: '', spec: '', t1: '', t2: '', t3: '', t4: '', t5: '', result: 'PASS' }
        ];
        modalApi.setState({ showConfirmButton: true, confirmText: '生成送样验证单', cancelText: '取消' });
      } else {
        formData.value = {
          ...data,
          applyDept: '研发中心', applyDate: '2026-02-19', sampleCount: '第 1 次',
          verifyType: ['新品开发'], testItems: ['尺寸检验', '性能检验', '表面检验']
        };

        // 模拟已有的测试数据
        testDataList.value = [
          { id: 1, item: '厚度 (mm)', spec: '0.05 ±0.002', t1: '0.051', t2: '0.049', t3: '0.050', t4: '0.051', t5: '0.050', result: 'PASS' },
          { id: 2, item: '透光率 (%)', spec: '≥ 92%', t1: '93.5', t2: '94.1', t3: '93.8', t4: '94.0', t5: '93.9', result: 'PASS' }
        ];

        if (data.tabType === 'todo') {
          modalApi.setState({ showConfirmButton: true, confirmText: '提交测试数据与评估结论', cancelText: '暂存' });
        } else {
          modalApi.setState({ showConfirmButton: false, cancelText: '关闭阅览' });
        }
      }
    }
  },
  async onConfirm() {
    if (isNew.value) {
      await formRef.value?.validate();
      message.success('送样验证单已下发给相关部门！');
    } else {
      const myReview = internalReviews.value.find(r => r.dept === currentUserDept.value);
      if (myReview && !myReview.result) {
        return message.warning(`请先完成您所在部门（${currentUserDept.value}）的评估结论！`);
      }
      message.success(`【${currentUserDept.value}】评估数据已录入！系统将流转至下一部门。`);
    }
    emit('success');
    modalApi.close();
  }
});
</script>

<template>
  <Modal class="w-[1200px]">
    <div class="flex flex-col h-full min-h-[70vh] w-full max-w-7xl mx-auto px-6 py-2 bg-[#f4f6f8] overflow-hidden relative">

      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6 flex flex-col gap-6 relative">

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 md:px-12 mt-2">

          <div class="text-center mb-10 relative">
            <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0">采购首批样品评价表</h2>
            <div v-if="formData.status === 'PASSED'" class="absolute -top-4 right-10 text-6xl text-green-500 opacity-20 border-4 border-green-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">样品合格</div>
            <div v-if="formData.status === 'REJECTED'" class="absolute -top-4 right-10 text-6xl text-red-500 opacity-20 border-4 border-red-500 rounded-full px-4 py-2 rotate-12 font-black tracking-widest">判定拒收</div>
          </div>

          <Form ref="formRef" :model="formData" layout="vertical" :disabled="!isNew && formData.tabType !== 'todo'">

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200"><span class="text-slate-500 font-bold text-base">一、样品概况与验证目的</span></Divider>
            <div class="grid grid-cols-4 gap-6 mb-6">
              <Form.Item label="供应商名称" required class="col-span-2 mb-0"><Input v-model:value="formData.supplierName" size="large"/></Form.Item>
              <Form.Item label="样品品名" required class="mb-0"><Input v-model:value="formData.materialName" size="large"/></Form.Item>
              <Form.Item label="规格/型号" class="mb-0"><Input v-model:value="formData.materialSpec" size="large"/></Form.Item>

              <Form.Item label="样品数量" required class="mb-0"><Input v-model:value="formData.sampleQty" /></Form.Item>
              <Form.Item label="送样次数" class="mb-0"><Input v-model:value="formData.sampleCount" placeholder="如: 第1次" /></Form.Item>
              <Form.Item label="申请部门" required class="mb-0"><Input v-model:value="formData.applyDept" /></Form.Item>
              <Form.Item label="申请日期" class="mb-0"><Input v-model:value="formData.applyDate" type="date" /></Form.Item>
            </div>

            <div class="bg-blue-50/50 p-5 rounded-lg border border-blue-100 mb-8 grid grid-cols-2 gap-6">
              <Form.Item label="验证类别 (可多选)" class="mb-0 font-bold text-slate-700">
                <Checkbox.Group v-model:value="formData.verifyType">
                  <Row :gutter="[16, 12]">
                    <Col><Checkbox value="产品改进">产品改进</Checkbox></Col>
                    <Col><Checkbox value="新品开发">新品开发</Checkbox></Col>
                    <Col><Checkbox value="增加业务范围">增加业务范围</Checkbox></Col>
                    <Col><Checkbox value="增加供应商">增加供应商</Checkbox></Col>
                    <Col><Checkbox value="其他">其他</Checkbox></Col>
                  </Row>
                </Checkbox.Group>
              </Form.Item>
              <Form.Item label="要求检验项目 (可多选)" class="mb-0 font-bold text-slate-700 border-l border-blue-200 pl-6">
                <Checkbox.Group v-model:value="formData.testItems">
                  <Row :gutter="[16, 12]">
                    <Col><Checkbox value="尺寸检验">尺寸检验</Checkbox></Col>
                    <Col><Checkbox value="性能检验">性能检验</Checkbox></Col>
                    <Col><Checkbox value="功能检验">功能检验</Checkbox></Col>
                    <Col><Checkbox value="安规检验">安规检验</Checkbox></Col>
                    <Col><Checkbox value="表面检验">表面检验</Checkbox></Col>
                  </Row>
                </Checkbox.Group>
              </Form.Item>
            </div>

            <Divider orientation="left" class="!m-0 !mb-6 border-slate-200">
                <span class="text-slate-500 font-bold text-base flex items-center gap-2">
                  二、核心性能测试数据实录 (5个样本)
                  <Button v-if="formData.tabType === 'todo' || isNew" size="small" type="primary" ghost @click="addTestData" class="ml-4">+ 追加测试项</Button>
                </span>
            </Divider>

            <div class="mb-8 overflow-x-auto">
              <table class="w-full border-collapse text-sm text-left bg-white shadow-sm border border-slate-300">
                <thead class="bg-slate-100 text-slate-700 font-bold text-center">
                <tr>
                  <th class="border border-slate-300 p-2 w-[5%]">序号</th>
                  <th class="border border-slate-300 p-2 w-[18%]">检验项目</th>
                  <th class="border border-slate-300 p-2 w-[18%]">技术要求 (标准值)</th>
                  <th class="border border-slate-300 p-2 w-[9%]">样本1</th>
                  <th class="border border-slate-300 p-2 w-[9%]">样本2</th>
                  <th class="border border-slate-300 p-2 w-[9%]">样本3</th>
                  <th class="border border-slate-300 p-2 w-[9%]">样本4</th>
                  <th class="border border-slate-300 p-2 w-[9%]">样本5</th>
                  <th class="border border-slate-300 p-2 w-[10%]">单项判定</th>
                  <th v-if="formData.tabType === 'todo' || isNew" class="border border-slate-300 p-2 w-[4%]"></th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(row, idx) in testDataList" :key="row.id" class="hover:bg-slate-50 transition-colors">
                  <td class="border border-slate-300 p-2 text-center text-slate-500">{{ idx + 1 }}</td>
                  <td class="border border-slate-300 p-1">
                    <Input v-if="formData.tabType === 'todo' || isNew" v-model:value="row.item" size="small" class="w-full !border-transparent hover:!border-indigo-300 focus:!border-indigo-500"/>
                    <span v-else class="px-2">{{ row.item }}</span>
                  </td>
                  <td class="border border-slate-300 p-1">
                    <Input v-if="formData.tabType === 'todo' || isNew" v-model:value="row.spec" size="small" class="w-full !border-transparent hover:!border-indigo-300"/>
                    <span v-else class="px-2 font-mono text-xs text-slate-600">{{ row.spec }}</span>
                  </td>
                  <td v-for="i in 5" :key="i" class="border border-slate-300 p-1">
                    <Input v-if="formData.tabType === 'todo' || isNew" v-model:value="row[`t${i}`]" size="small" class="w-full text-center font-mono !border-transparent hover:!border-indigo-300 bg-slate-50/50"/>
                    <span v-else class="block text-center font-mono text-xs font-bold text-indigo-700">{{ row[`t${i}`] }}</span>
                  </td>
                  <td class="border border-slate-300 p-1 text-center">
                    <Select v-if="formData.tabType === 'todo' || isNew" v-model:value="row.result" size="small" class="w-full" :options="[{label:'合格',value:'PASS'},{label:'不合格',value:'REJECT'}]" />
                    <Tag v-else :color="row.result === 'PASS' ? 'success' : 'error'" class="!m-0 border-none font-bold block">{{ row.result === 'PASS' ? 'OK' : 'NG' }}</Tag>
                  </td>
                  <td v-if="formData.tabType === 'todo' || isNew" class="border border-slate-300 p-1 text-center">
                    <Button type="text" danger size="small" @click="removeTestData(idx)"><IconifyIcon icon="lucide:trash-2"/></Button>
                  </td>
                </tr>
                <tr v-if="testDataList.length === 0">
                  <td colspan="10" class="border border-slate-300 p-6 text-center text-slate-400">尚未录入任何实测数据，请点击上方按钮追加</td>
                </tr>
                </tbody>
              </table>
            </div>
          </Form>

          <template v-if="!isNew">
            <Divider orientation="left" class="!m-0 !mb-6 border-indigo-200">
              <span class="text-indigo-800 font-black text-lg flex items-center gap-2">
                  <IconifyIcon icon="lucide:users-2"/> 各部门联合确认意见
              </span>
            </Divider>

            <div class="mb-6">
              <table class="w-full border-collapse text-sm text-left bg-white shadow-sm border border-slate-300">
                <thead class="bg-slate-100 text-slate-700 font-bold">
                <tr>
                  <th class="border border-slate-300 p-3 w-[15%] text-center">确认部门</th>
                  <th class="border border-slate-300 p-3 w-[20%] text-center">综合结论</th>
                  <th class="border border-slate-300 p-3 flex-1">问题说明 / 详细意见</th>
                  <th class="border border-slate-300 p-3 w-[12%] text-center">签名与日期</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="review in internalReviews" :key="review.id"
                    :class="[review.dept === currentUserDept && formData.tabType === 'todo' ? 'bg-orange-50/60' : 'hover:bg-slate-50 transition-colors']">

                  <td class="border border-slate-300 p-3 text-center">
                    <span class="font-bold text-slate-700 block">{{ review.dept }}</span>
                    <Tag v-if="review.dept === currentUserDept && formData.tabType === 'todo'" color="processing" class="!m-0 mt-1 scale-90">当前处理</Tag>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <div v-if="review.dept === currentUserDept && formData.tabType === 'todo'">
                      <Radio.Group v-model:value="review.result" size="small" button-style="solid" class="flex w-full">
                        <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">合格</Radio.Button>
                        <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">不合格</Radio.Button>
                      </Radio.Group>
                    </div>
                    <div v-else>
                      <Tag v-if="review.result === 'PASS'" color="success" class="!m-0 font-bold border-none px-4">评估合格</Tag>
                      <Tag v-else-if="review.result === 'REJECT'" color="error" class="!m-0 font-bold border-none px-4">不合格</Tag>
                      <span v-else class="text-slate-400 text-xs">- 等待签署 -</span>
                    </div>
                  </td>

                  <td class="border border-slate-300 p-3">
                    <div v-if="review.dept === currentUserDept && formData.tabType === 'todo'">
                      <Input.TextArea v-model:value="review.opinion" :rows="2" placeholder="请详细说明测试发现的问题..." class="w-full !border-orange-300 bg-white" />
                    </div>
                    <div v-else class="text-xs text-slate-600 leading-relaxed">{{ review.opinion || '-' }}</div>
                  </td>

                  <td class="border border-slate-300 p-3 text-center">
                    <span class="text-xs font-bold text-slate-800 block">{{ review.reviewer || '-' }}</span>
                    <span class="text-[10px] text-slate-400">{{ review.time ? review.time.substring(5,10) : '' }}</span>
                  </td>
                </tr>

                <tr class="bg-indigo-50/50 border-t-2 border-indigo-200">
                  <td class="border border-slate-300 p-4 text-center font-black text-indigo-800">最终批准</td>
                  <td class="border border-slate-300 p-4 text-center">
                    <div v-if="finalApproval.editable">
                      <Radio.Group v-model:value="finalApproval.result" size="small" button-style="solid" class="flex w-full shadow-sm">
                        <Radio.Button value="PASS" class="flex-1 text-center bg-green-50 border-green-200 !text-green-700 font-bold">准予量产导入</Radio.Button>
                        <Radio.Button value="REJECT" class="flex-1 text-center bg-red-50 border-red-200 !text-red-700 font-bold">彻底否决</Radio.Button>
                      </Radio.Group>
                    </div>
                    <div v-else>
                      <Tag v-if="finalApproval.result === 'PASS'" color="success" class="!m-0 font-bold border-none px-4">批准</Tag>
                      <Tag v-else-if="finalApproval.result === 'REJECT'" color="error" class="!m-0 font-bold border-none px-4">否决</Tag>
                      <span v-else class="text-slate-400 text-xs">- 等待前置会签 -</span>
                    </div>
                  </td>
                  <td class="border border-slate-300 p-4 text-xs font-bold text-indigo-700">{{ finalApproval.opinion || '综合各部门测试意见定夺' }}</td>
                  <td class="border border-slate-300 p-4 text-center text-xs font-bold text-slate-800">{{ finalApproval.reviewer || '-' }}</td>
                </tr>
                </tbody>
              </table>
            </div>

            <div v-if="formData.tabType === 'todo'" class="bg-slate-50 p-4 rounded-lg flex items-center gap-4 justify-between border border-slate-200 mb-6">
              <div class="text-sm font-bold text-slate-600 flex items-center gap-2">
                <IconifyIcon icon="lucide:settings-2"/> 流程干预
              </div>
              <div class="flex items-center gap-4 flex-1 max-w-lg">
                <Select v-model:value="approvalAction.nextStep" class="flex-1" size="small">
                  <Select.Option value="DEFAULT">系统自动流转下一审核部门</Select.Option>
                  <Select.Option value="CUSTOM">测试严重不合格，强制中断流程</Select.Option>
                </Select>
              </div>
            </div>
          </template>

          <div v-if="!isNew" class="flex justify-center pb-2 mt-6">
            <Button type="dashed" size="large" class="w-[300px] border-indigo-300 text-indigo-600 font-bold hover:bg-indigo-50 shadow-sm" @click="drawerVisible = true">
              <IconifyIcon icon="lucide:history" class="mr-2" /> 查看评价单流转记录
            </Button>
          </div>

        </div>
      </div>
    </div>

    <Drawer v-model:open="drawerVisible" title="送样单据流转日志" placement="right" :width="450">
      <div class="bg-indigo-50 border border-indigo-100 p-3 rounded text-indigo-800 text-sm mb-6 flex items-start gap-2">
        <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
        <span>当前停留在 <b>多部门测试与会签</b> 环节。</span>
      </div>
      <Steps direction="vertical" :current="2" size="small" class="px-2">
        <Steps.Step v-for="(step, i) in processSteps" :key="i" :title="step.title" :status="step.status as any">
          <template #description>
            <div class="text-xs mt-2 mb-6 p-3 rounded-lg shadow-sm border" :class="step.status === 'process' ? 'bg-white border-indigo-200' : 'bg-slate-50 border-slate-100 text-slate-500'">
              <div class="font-bold text-slate-600">{{ step.desc }}</div>
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
