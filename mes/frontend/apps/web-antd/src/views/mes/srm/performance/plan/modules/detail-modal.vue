<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Form, Input, Select, DatePicker, Button, Table, Popconfirm, message, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getPlanDetail } from '#/api/mes/srm/performance/plan';
import dayjs from 'dayjs';

const emit = defineEmits(['success']);
const isNew = ref(false);
const formRef = ref();
const formData = ref<any>({ suppliers: [] });
const isReadOnly = computed(() => !isNew.value && formData.value.status !== 0); // 已发布的计划只读

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 制定供方评审计划' : (isReadOnly.value ? '👁️ 查看评审计划' : '📝 编辑评审计划')),
  class: 'w-[1000px]',
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      if (isNew.value) {
        formData.value = {
          periodType: 'QUARTER',
          evalYear: dayjs().year(),
          evalQuarter: Math.ceil((dayjs().month() + 1) / 3),
          status: 0,
          suppliers: []
        };
        modalApi.setState({ showConfirmButton: true, confirmText: '保存草稿' });
      } else {
        const detail: any = await getPlanDetail(data.id);
        formData.value = { ...detail, suppliers: detail.suppliers || [] };
        modalApi.setState({ showConfirmButton: !isReadOnly.value, confirmText: '保存修改' });
      }
    }
  },
  onConfirm: async () => {
    await formRef.value?.validate();
    if (formData.value.suppliers.length === 0) {
      return message.warning('请至少添加一家参评的供应商！');
    }
    console.log('保存的计划数据:', formData.value);
    message.success('计划保存成功！');
    emit('success');
    modalApi.close();
  }
});

// 模拟选择供应商
function handleAddSupplier() {
  formData.value.suppliers.push({
    id: Date.now().toString(),
    supplierId: `S${Date.now().toString().slice(-4)}`,
    supplierName: '新导入的测试供应商',
    materialType: 'A类',
    evalStatus: 0,
    totalScore: null
  });
  message.info('已添加参评供应商。');
}

function handleRemoveSupplier(index: number) {
  formData.value.suppliers.splice(index, 1);
}

const supplierColumns = [
  { title: '序号', dataIndex: 'seq', width: 60, align: 'center' },
  { title: '供应商名称', dataIndex: 'supplierName', minWidth: 200 },
  { title: '主供物料分类', dataIndex: 'materialType', width: 120, align: 'center' },
  { title: '打分进度', dataIndex: 'evalStatus', width: 120, align: 'center' },
  { title: '当前总分', dataIndex: 'totalScore', width: 100, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' }
];

// 模拟模板数据字典
const templateOptions = [
  { label: 'A/B类物料季度综合考核模板', value: '1' },
  { label: 'C类辅材年度考核模板', value: '2' },
  { label: '新供应商准入专项审查卷', value: '3' }
];
</script>

<template>
  <Modal>
    <div class="bg-[#f4f6f8] min-h-[60vh] p-6 -mx-6 -mt-4">
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col gap-6">

        <div v-if="isReadOnly" class="bg-blue-50 border border-blue-200 p-3 rounded text-blue-800 text-sm flex items-start gap-2">
          <IconifyIcon icon="lucide:info" class="text-lg mt-0.5 shrink-0" />
          <span>此计划已发布并正在执行中（或已完结），当前处于<b>只读模式</b>。各部门正在进行阅卷打分作业。</span>
        </div>

        <Form ref="formRef" :model="formData" layout="vertical" :disabled="isReadOnly">
          <div class="grid grid-cols-3 gap-x-6 gap-y-4">
            <Form.Item label="计划单号" name="planNo" class="col-span-1 mb-0">
              <Input v-model:value="formData.planNo" disabled placeholder="保存后系统自动生成" />
            </Form.Item>
            <Form.Item label="计划标题" required name="title" class="col-span-2 mb-0">
              <Input v-model:value="formData.title" placeholder="如：2026年第一季度A类供应商考评计划" />
            </Form.Item>

            <Form.Item label="考核场景/周期" required name="periodType" class="mb-0">
              <Select v-model:value="formData.periodType" :options="[{label: '季度考核', value: 'QUARTER'}, {label: '年度考核', value: 'YEAR'}, {label: '准入/专项稽核', value: 'AUDIT'}]" />
            </Form.Item>

            <template v-if="formData.periodType === 'QUARTER'">
              <Form.Item label="考核年份" required name="evalYear" class="mb-0">
                <Select v-model:value="formData.evalYear" :options="[{label: '2025年', value: 2025}, {label: '2026年', value: 2026}]" />
              </Form.Item>
              <Form.Item label="考核季度" required name="evalQuarter" class="mb-0">
                <Select v-model:value="formData.evalQuarter" :options="[{label: '第一季度 (Q1)', value: 1}, {label: '第二季度 (Q2)', value: 2}, {label: '第三季度 (Q3)', value: 3}, {label: '第四季度 (Q4)', value: 4}]" />
              </Form.Item>
            </template>
            <template v-else-if="formData.periodType === 'YEAR'">
              <Form.Item label="考核年份" required name="evalYear" class="mb-0">
                <Select v-model:value="formData.evalYear" :options="[{label: '2025年', value: 2025}, {label: '2026年', value: 2026}]" />
              </Form.Item>
              <div class="col-span-1"></div> </template>
            <template v-else>
              <div class="col-span-2"></div> </template>

            <Form.Item label="采用的评分模板 (考卷)" required name="templateId" class="col-span-2 mb-0">
              <Select v-model:value="formData.templateId" :options="templateOptions" placeholder="请选择从配置中心生成的模板" />
            </Form.Item>
            <Form.Item label="打分截止日期" required name="deadline" class="col-span-1 mb-0">
              <DatePicker v-model:value="formData.deadline" valueFormat="YYYY-MM-DD" class="w-full" />
            </Form.Item>

            <Form.Item label="计划备注说明" name="remark" class="col-span-3 mb-0">
              <Input.TextArea v-model:value="formData.remark" :rows="2" placeholder="填写给各打分部门的注意事项..." />
            </Form.Item>
          </div>
        </Form>

        <div class="border-t border-dashed border-slate-300 pt-6">
          <div class="flex justify-between items-center mb-4">
            <div class="text-lg font-bold text-slate-800 flex items-center gap-2">
              <IconifyIcon icon="lucide:users" class="text-indigo-600" />
              参评供应商圈选
            </div>
            <Button v-if="!isReadOnly" type="primary" @click="handleAddSupplier">
              <IconifyIcon icon="lucide:user-plus" class="mr-1" /> 选取待考核供应商
            </Button>
          </div>

          <Table :dataSource="formData.suppliers" :columns="supplierColumns" :pagination="false" size="small" bordered class="bg-white">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'seq'">{{ index + 1 }}</template>

              <template v-if="column.dataIndex === 'supplierName'">
                <div class="font-bold text-slate-700">{{ record.supplierName }}</div>
                <div class="text-xs text-slate-400 font-mono">{{ record.supplierId }}</div>
              </template>

              <template v-if="column.dataIndex === 'materialType'">
                <Tag color="cyan" class="!m-0">{{ record.materialType }}</Tag>
              </template>

              <template v-if="column.dataIndex === 'evalStatus'">
                <Tag v-if="record.evalStatus === 0" color="default" class="!m-0 border-none">待生成任务</Tag>
                <Tag v-else-if="record.evalStatus === 1" color="processing" class="!m-0 border-none">部门打分中</Tag>
                <Tag v-else-if="record.evalStatus === 2" color="success" class="!m-0 border-none">已出成绩</Tag>
              </template>

              <template v-if="column.dataIndex === 'totalScore'">
                <span v-if="record.totalScore" class="font-bold text-lg" :class="record.evalGrade === 'A' ? 'text-green-600' : (record.evalGrade === 'D' ? 'text-red-500' : 'text-indigo-600')">
                  {{ record.totalScore }}
                </span>
                <span v-else class="text-slate-300">-</span>
              </template>

              <template v-if="column.dataIndex === 'action'">
                <Popconfirm v-if="!isReadOnly" title="确定移除此供应商吗？" @confirm="handleRemoveSupplier(index)">
                  <Button type="link" danger size="small"><IconifyIcon icon="lucide:trash-2" /></Button>
                </Popconfirm>
                <Button v-else type="link" size="small" class="px-2" title="查看该供应商详细答卷"><IconifyIcon icon="lucide:file-text" /></Button>
              </template>
            </template>
          </Table>
        </div>

      </div>
    </div>
  </Modal>
</template>
