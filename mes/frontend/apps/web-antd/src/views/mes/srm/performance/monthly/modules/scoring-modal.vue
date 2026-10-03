<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Table, InputNumber, Input, Button, Tag, Upload, Drawer, Timeline, message, Tooltip } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['success']);
const formData = ref<any>({ items: [] });
const isReadOnly = ref(false);

// 系统数据穿透抽屉控制
const sysDataDrawerVisible = ref(false);
const currentSysData = ref<any>({});

// 实时计算已得总分
const currentScore = computed(() => {
  return formData.value.items?.reduce((sum: number, item: any) => sum + (Number(item.actualScore) || 0), 0) || 0;
});

// 实时计算等级 (按照准则文件)
const currentGrade = computed(() => {
  const s = currentScore.value;
  if (s >= 90) return { grade: 'A', color: 'text-green-600', desc: '首选供应商，增加采购量' };
  if (s >= 80) return { grade: 'B', color: 'text-blue-600', desc: '正常采购，稳定订单' };
  if (s >= 70) return { grade: 'C', color: 'text-orange-500', desc: '限期整改，减少订单' };
  return { grade: 'D', color: 'text-red-500', desc: '停单整改，考虑淘汰' };
});

const [Modal, modalApi] = useVbenModal({
  title: computed(() => isReadOnly.value ? '📑 供应商考核' : '📝 供应商绩效评估'),
  class: 'w-[1300px]', // 采用超宽弹窗容纳全部数据列
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();
      isReadOnly.value = !!data.isReadOnly;

      // 模拟后端拉取详细数据 (合并了目标值、实际值、附件数组等)
      formData.value = {
        id: data.id,
        supplierName: '江苏某高分子材料公司',
        planTitle: '2026年Q1常规考核',
        recordNo: 'EV-26Q1-001',
        items: [
          { indicatorCode: 'KPI-Q-01', indicatorName: '到料批合格率(LAR)', category: '质量', type: 1, isAuto: true, maxScore: 15, targetValue: '98', actualValue: '98.5', actualScore: 15, scoringDept: '品质部', attachments: [] },
          { indicatorCode: 'KPI-D-01', indicatorName: '交期达成率', category: '交付', type: 1, isAuto: true, maxScore: 15, targetValue: '100', actualValue: '90', actualScore: 13.5, scoringDept: '仓管部', attachments: [] },
          { indicatorCode: 'KPI-S-01', indicatorName: '合同履行情况', category: '服务', type: 2, isAuto: false, maxScore: 5, targetValue: '履约优良', actualValue: '', actualScore: null, scoringDept: '采购部', attachments: [] },
          { indicatorCode: 'KPI-Q-03', indicatorName: '品质改善配合度', category: '质量', type: 2, isAuto: false, maxScore: 10, targetValue: '响应及时', actualValue: '', actualScore: null, scoringDept: '品质部', attachments: [] },
        ]
      };
      modalApi.setState({ showConfirmButton: !isReadOnly.value, confirmText: '正式交卷发布成绩' });
    }
  },
  onConfirm: async () => {
    // 校验：所有主观题必须打分
    const missing = formData.value.items.find((i: any) => i.actualScore === undefined || i.actualScore === null);
    if (missing) {
      return message.warning(`指标 [${missing.indicatorName}] 尚未完成算分，请完成所有阅卷再交卷！`);
    }
    console.log('交卷数据:', { id: formData.value.id, totalScore: currentScore.value, grade: currentGrade.value.grade, items: formData.value.items });
    message.success('交卷成功！实际值、最终得分及所有附件已归档。');
    emit('success');
    modalApi.close();
  }
});

// 💡 核心一：自动算分引擎 (当实际值改变时)
function handleActualValueChange(record: any) {
  if (!record.actualValue) {
    record.actualScore = null;
    return;
  }
  const actual = parseFloat(record.actualValue);
  const target = parseFloat(record.targetValue);

  if (!isNaN(actual) && !isNaN(target) && target > 0) {
    let score = (actual / target) * record.maxScore;
    record.actualScore = score > record.maxScore ? record.maxScore : Number(score.toFixed(1));
  }
}

// 💡 核心二：查看系统底层数据抽屉
function handleViewSysData(record: any) {
  currentSysData.value = {
    indicatorName: record.indicatorName,
    code: record.indicatorCode,
    type: record.indicatorCode.includes('Q') ? 'quality' : 'delivery'
  };
  sysDataDrawerVisible.value = true;
}

// 💡 核心三：模拟上传前的拦截
const beforeUpload = (file: any) => {
  message.success(`${file.name} 附件上传成功 (前端模拟)`);
  return false;
};

// 表格列定义
const itemColumns = [
  { title: '序号', dataIndex: 'seq', width: 50, align: 'center' },
  { title: '维度', dataIndex: 'category', width: 70, align: 'center' },
  { title: '考核指标', dataIndex: 'indicatorName', minWidth: 160 },
  { title: '目标基准', dataIndex: 'targetValue', width: 90, align: 'center' },
  { title: '实际达成值', dataIndex: 'actualValue', width: 140, align: 'center' },
  { title: '折算满分', dataIndex: 'maxScore', width: 80, align: 'center' },
  { title: '最终得分', dataIndex: 'actualScore', width: 120, align: 'center' },
  { title: '举证材料与附件', dataIndex: 'evidenceRemark', minWidth: 260 }
];
</script>

<template>
  <Modal>
    <div class="bg-[#f4f6f8] min-h-[60vh] p-6 -mx-6 -mt-4 flex flex-col gap-6">

      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex justify-between items-center relative overflow-hidden">
        <div class="z-10">
          <h2 class="text-2xl font-black text-slate-800 tracking-widest m-0 mb-2">{{ formData.supplierName }}</h2>
          <div class="text-slate-500 flex gap-4">
            <span><IconifyIcon icon="lucide:file-text" class="inline mr-1"/> 考核计划：{{ formData.planTitle }}</span>
            <span><IconifyIcon icon="lucide:hash" class="inline mr-1"/> 评分单号：{{ formData.recordNo }}</span>
          </div>
        </div>
        <div class="absolute -top-4 right-10 text-8xl opacity-10 border-4 rounded-full px-4 py-2 rotate-12 font-black tracking-widest" :class="isReadOnly ? 'border-green-500 text-green-500' : 'border-indigo-500 text-indigo-500'">
          {{ isReadOnly ? currentGrade.grade + '级' : '打分中' }}
        </div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex-1">
        <div class="text-lg font-bold text-slate-800 flex items-center mb-4">
          <IconifyIcon icon="lucide:check-square" class="text-indigo-600 mr-2" />
          数据采集与成绩核算区
        </div>

        <Table :dataSource="formData.items" :columns="itemColumns" :pagination="false" size="small" bordered>
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'seq'">{{ index + 1 }}</template>
            <template v-if="column.dataIndex === 'category'"><Tag color="blue" class="!m-0">{{ record.category }}</Tag></template>

            <template v-if="column.dataIndex === 'indicatorName'">
              <div class="font-bold text-slate-700">{{ record.indicatorName }}</div>
              <div class="mt-1 flex gap-2 items-center">
                <Tag v-if="record.isAuto" color="processing" size="small" class="!text-[10px] border-none !m-0">系统取数</Tag>
                <Tag v-else color="warning" size="small" class="!text-[10px] border-none !m-0">人工填报</Tag>

                <Button type="link" size="small" class="!px-0 !text-[11px]" @click="handleViewSysData(record)">
                  <IconifyIcon icon="lucide:database" class="mr-1"/>台账追溯
                </Button>
              </div>
            </template>

            <template v-if="column.dataIndex === 'targetValue'">
              <span class="font-bold text-slate-500">{{ record.targetValue }}{{ record.isAuto ? '%' : '' }}</span>
            </template>

            <template v-if="column.dataIndex === 'actualValue'">
              <div v-if="record.isAuto" class="text-blue-600 font-bold bg-blue-50 border border-blue-200 py-1 rounded text-center">
                {{ record.actualValue }}%
              </div>
              <Input
                v-else
                v-model:value="record.actualValue"
                placeholder="录入实际情况"
                @change="handleActualValueChange(record)"
                :disabled="isReadOnly"
                class="text-center"
              />
            </template>

            <template v-if="column.dataIndex === 'actualScore'">
              <Tooltip title="基于实际值自动折算，也可人工微调">
                <InputNumber
                  v-model:value="record.actualScore"
                  :min="0" :max="record.maxScore" :precision="1"
                  class="w-full text-center font-bold text-indigo-700 bg-indigo-50"
                  :disabled="isReadOnly || record.isAuto"
                />
              </Tooltip>
            </template>

            <template v-if="column.dataIndex === 'evidenceRemark'">
              <div class="flex flex-col gap-2">
                <Input.TextArea v-model:value="record.evidenceRemark" :rows="1" placeholder="填写扣分说明或具体举证..." :disabled="isReadOnly" />

                <Upload
                  v-if="!isReadOnly"
                  v-model:file-list="record.attachments"
                  action="#"
                  :before-upload="beforeUpload"
                  multiple
                  :maxCount="3"
                >
                  <Button size="small" class="text-slate-500 text-xs"><IconifyIcon icon="lucide:paperclip" class="mr-1"/>上传附件证据</Button>
                </Upload>

                <div v-else-if="record.attachments?.length" class="bg-slate-50 p-2 rounded">
                  <a v-for="file in record.attachments" :key="file.name" class="block text-xs text-blue-500 hover:underline mb-1">
                    <IconifyIcon icon="lucide:download"/> {{ file.name }}
                  </a>
                </div>
              </div>
            </template>

          </template>
        </Table>
      </div>

      <div class="bg-indigo-50 border-t-4 border-indigo-400 rounded-xl p-6 flex justify-between items-center shadow-sm">
        <div>
          <div class="text-slate-500 mb-1 font-bold">各项得分汇总核算：</div>
          <div class="text-4xl font-black text-indigo-700">{{ currentScore.toFixed(1) }} <span class="text-lg text-slate-400 font-normal">/ 100 分</span></div>
        </div>
        <div class="text-right">
          <div class="text-slate-500 mb-1 font-bold">根据准则，系统判定当前级别预测为：</div>
          <div class="text-3xl font-black" :class="currentGrade.color">
            {{ currentGrade.grade }} 级
            <span class="text-sm font-normal text-slate-500 bg-white px-2 py-1 rounded-full shadow-sm ml-2 align-middle">{{ currentGrade.desc }}</span>
          </div>
        </div>
      </div>
    </div>

    <Drawer v-model:open="sysDataDrawerVisible" title="📊 关联系统原始台账参考" placement="right" :width="500">
      <div class="bg-blue-50 border border-blue-100 p-3 rounded text-blue-800 text-sm mb-6 flex flex-col gap-1">
        <span class="font-bold">被查供应商：{{ formData.supplierName }}</span>
        <span>查询指标：{{ currentSysData.indicatorName }} ({{ currentSysData.code }})</span>
        <span class="text-xs text-blue-500">数据源：实时从底层核心业务系统抓取聚合</span>
      </div>

      <template v-if="currentSysData.type === 'quality'">
        <div class="mb-4 font-bold text-slate-700">考核期内 IQC 检验批次记录</div>
        <Timeline>
          <Timeline.Item color="green">
            <div class="font-bold text-slate-600">2026-03-15 <Tag color="success" class="ml-2">合格</Tag></div>
            <div class="text-xs text-slate-400">物料：PC/ABS合金外壳 | 批次号：BATCH-0315-01 | 检验量：5000</div>
          </Timeline.Item>
          <Timeline.Item color="green">
            <div class="font-bold text-slate-600">2026-03-02 <Tag color="success" class="ml-2">合格</Tag></div>
            <div class="text-xs text-slate-400">物料：阻燃PC外壳 | 批次号：BATCH-0302-05 | 检验量：2000</div>
          </Timeline.Item>
          <Timeline.Item color="red">
            <div class="font-bold text-slate-600">2026-02-18 <Tag color="error" class="ml-2">拒收 (尺寸超差)</Tag></div>
            <div class="text-xs text-slate-400">物料：阻燃PC外壳 | 批次号：BATCH-0218-02 | 退货量：1500</div>
            <div class="text-xs text-red-500 mt-1 cursor-pointer hover:underline">关联异常单：NCR-260218-001 (已触发8D)</div>
          </Timeline.Item>
        </Timeline>
        <div class="mt-6 bg-slate-50 p-4 rounded text-center border border-slate-200">
          <div class="text-slate-500 text-sm mb-1">系统核算期内 LAR (到料批合格率)</div>
          <div class="text-3xl font-black text-green-600">98.5%</div>
        </div>
      </template>

      <template v-else>
        <div class="mb-4 font-bold text-slate-700">考核期内采购订单交期达成记录</div>
        <Table
          size="small"
          :pagination="false"
          :columns="[{title:'采购单号', dataIndex:'po'}, {title:'约定交期', dataIndex:'date1'}, {title:'入库日期', dataIndex:'date2'}, {title:'状态', dataIndex:'status'}]"
          :dataSource="[
            {po: 'PO-2603-011', date1: '03-10', date2: '03-09', status: '提前交货'},
            {po: 'PO-2602-085', date1: '02-25', date2: '02-28', status: '逾期3天'},
            {po: 'PO-2601-032', date1: '01-15', date2: '01-15', status: '按时交货'}
          ]"
        >
          <template #bodyCell="{ column, record }">
            <span v-if="column.dataIndex === 'status'" :class="record.status.includes('逾期') ? 'text-red-500 font-bold' : 'text-green-600'">{{ record.status }}</span>
          </template>
        </Table>
      </template>
    </Drawer>

  </Modal>
</template>
