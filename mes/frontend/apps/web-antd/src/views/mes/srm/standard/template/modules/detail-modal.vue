<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Form, Input, Select, InputNumber, Button, Table, Popconfirm, message, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getTemplateDetail } from '#/api/mes/srm/standard/template';
import IndicatorSelectModal from './indicator-select-modal.vue'; // 引入新建的多选弹窗

const emit = defineEmits(['success']);
const isNew = ref(false);
const formRef = ref();
const formData = ref<any>({ items: [] });

// 实例化多选弹窗
const [SelectModal, selectModalApi] = useVbenModal({ connectedComponent: IndicatorSelectModal });

// 实时计算当前配置的总权重
const currentTotalWeight = computed(() => {
  return formData.value.items?.reduce((sum: number, item: any) => sum + (Number(item.weightPercent) || 0), 0) || 0;
});

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isNew.value ? '✨ 新建评分模板' : '📐 设计评分模板与权重'),
  class: 'w-[1200px]', // 超宽弹窗适配主子表
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();
      isNew.value = !!data.isNew;
      if (isNew.value) {
        formData.value = { periodType: 'QUARTER', totalScore: 100, status: 0, items: [] };
      } else {
        const detail: any = await getTemplateDetail(data.id);
        formData.value = { ...detail, items: detail.items || [] };
      }
    }
  },
  onConfirm: async () => {
    await formRef.value?.validate();
    if (formData.value.items.length === 0) {
      return message.warning('请至少添加一项考核指标！');
    }
    if (currentTotalWeight.value !== 100) {
      return message.error(`当前总权重为 ${currentTotalWeight.value}%，必须等于 100% 才能保存！`);
    }

    console.log('提交的模板数据:', formData.value);
    message.success(isNew.value ? '模板创建成功' : '模板更新成功');
    emit('success');
    modalApi.close();
  }
});

// 打开多选弹窗
function openIndicatorSelect() {
  selectModalApi.open();
}

// 接收多选弹窗传回的指标列表
function handleIndicatorsSelected(selectedRows: any[]) {
  let addCount = 0;
  selectedRows.forEach(row => {
    // 防重复：判断是否已经存在
    const isExist = formData.value.items.some((item: any) => item.indicatorId === row.id);
    if (!isExist) {
      formData.value.items.push({
        indicatorId: row.id,
        indicatorCode: row.code,
        indicatorName: row.name,
        category: row.category,
        weightPercent: 0, // 默认权重0，待用户分配
        maxScore: 0,
        scoringDept: '品质部' // 默认给品质部，业务可下拉改
      });
      addCount++;
    }
  });
  if (addCount > 0) {
    message.success(`成功导入 ${addCount} 项指标，请为其分配权重。`);
  } else {
    message.info('选中的指标已存在于列表中，无需重复添加。');
  }
}

function handleRemoveItem(index: number) {
  formData.value.items.splice(index, 1);
}

// 子表列定义
const itemColumns = [
  { title: '序号', dataIndex: 'seq', width: 60, align: 'center' },
  { title: '指标维度', dataIndex: 'category', width: 80, align: 'center' },
  { title: '考核指标', dataIndex: 'indicatorName', minWidth: 180 },
  // 💡 新增：目标值基准列
  { title: '考核目标值', dataIndex: 'targetValue', width: 120, align: 'center' },
  { title: '阅卷部门', dataIndex: 'scoringDept', width: 120 },
  { title: '权重(%)', dataIndex: 'weightPercent', width: 100, align: 'center' },
  { title: '满分', dataIndex: 'maxScore', width: 80, align: 'center' },
  { title: '操作', dataIndex: 'action', width: 60, align: 'center' }
];

</script>

<template>
  <Modal>
    <div class="bg-[#f4f6f8] min-h-[60vh] p-6 -mx-6 -mt-4">
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col gap-6">

        <Form ref="formRef" :model="formData" layout="vertical">
          <div class="grid grid-cols-2 gap-x-8 gap-y-4">
            <Form.Item label="模板名称" required name="name" class="mb-0">
              <Input v-model:value="formData.name" placeholder="例如：A类材料年度考核模板" />
            </Form.Item>
            <Form.Item label="适用物料分类" required name="materialType" class="mb-0">
              <Select v-model:value="formData.materialType" mode="multiple" :options="[{label: 'A类(主材)', value: 'A类'}, {label: 'B类(辅材)', value: 'B类'}, {label: 'C类(耗材)', value: 'C类'}, {label: 'D类(设备)', value: 'D类'}]" placeholder="可多选" />
            </Form.Item>

            <Form.Item label="考核场景/周期" required name="periodType" class="mb-0">
              <Select v-model:value="formData.periodType" :options="[{label: '季度考核', value: 'QUARTER'}, {label: '年度考核', value: 'YEAR'}, {label: '准入/审厂', value: 'AUDIT'}]" />
            </Form.Item>
            <Form.Item label="总分基准" required name="totalScore" class="mb-0">
              <InputNumber v-model:value="formData.totalScore" class="w-full" disabled />
            </Form.Item>

            <Form.Item label="模板说明" name="remark" class="col-span-2 mb-0">
              <Input.TextArea v-model:value="formData.remark" :rows="2" placeholder="填写模板适用范围与说明..." />
            </Form.Item>
          </div>
        </Form>

        <div class="border-t border-dashed border-slate-300 pt-6">
          <div class="flex justify-between items-center mb-4">
            <div class="text-lg font-bold text-slate-800 flex items-center gap-2">
              <IconifyIcon icon="lucide:list-checks" class="text-indigo-600" />
              考核项与权重配置
            </div>
            <div class="flex items-center gap-4">
              <div class="text-sm font-bold bg-slate-100 px-4 py-1.5 rounded-full" :class="currentTotalWeight === 100 ? 'text-green-600' : 'text-red-500'">
                当前总权重: {{ currentTotalWeight }}% <span v-if="currentTotalWeight !== 100" class="text-xs font-normal">(必须等于100%)</span>
              </div>
              <Button type="primary" @click="openIndicatorSelect">
                <IconifyIcon icon="lucide:search-check" class="mr-1" /> 从指标库批量选取
              </Button>
            </div>
          </div>

          <Table :dataSource="formData.items" :columns="itemColumns" :pagination="false" size="small" bordered class="bg-white">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'seq'">{{ index + 1 }}</template>

              <template v-if="column.dataIndex === 'category'">
                <Tag color="blue" class="!m-0">{{ record.category }}</Tag>
              </template>

              <template v-if="column.dataIndex === 'indicatorName'">
                <div class="font-bold text-slate-700">{{ record.indicatorName }}</div>
                <div class="text-xs text-slate-400 font-mono">{{ record.indicatorCode }}</div>
              </template>

              <template v-if="column.dataIndex === 'scoringDept'">
                <Select v-model:value="record.scoringDept" class="w-full" size="small" :options="[{label:'品质部', value:'品质部'}, {label:'采购部', value:'采购部'}, {label:'技术部', value:'技术部'}, {label:'仓管部', value:'仓管部'}, {label:'生产部', value:'生产部'}]" />
              </template>

              <template v-if="column.dataIndex === 'weightPercent'">
                <InputNumber
                  v-model:value="record.weightPercent"
                  :min="0" :max="100" :precision="2"
                  size="small" class="w-full text-center"
                  @change="(val) => record.maxScore = (formData.totalScore * (val||0) / 100).toFixed(2)"
                />
              </template>

              <template v-if="column.dataIndex === 'maxScore'">
                <span class="font-bold text-indigo-600">{{ record.maxScore }}</span>
              </template>
              <template v-if="column.dataIndex === 'targetValue'">
                <Input
                  v-model:value="record.targetValue"
                  size="small"
                  class="w-full text-center"
                  placeholder="如: 98%"
                />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <Popconfirm title="确定要移除此指标吗？" @confirm="handleRemoveItem(index)">
                  <Button type="link" danger size="small" class="px-2"><IconifyIcon icon="lucide:trash-2" /></Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </div>

      </div>
    </div>

    <SelectModal @select="handleIndicatorsSelected" />
  </Modal>
</template>
