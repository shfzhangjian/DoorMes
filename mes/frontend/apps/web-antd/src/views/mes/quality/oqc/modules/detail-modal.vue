<script lang="ts" setup>
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Button, Descriptions, Spin, Table, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getOqcDetail, type MesOqcApi } from '#/api/mes/quality/oqc';

defineOptions({ name: 'OqcDetailModal' });

const record = ref<MesOqcApi.OqcRecord | null>(null);
const loading = ref(false);
const showCustomerCoaPrint = false;

const columns = [
  { title: '类别', dataIndex: 'category', width: 120, align: 'center' },
  { title: '序号', dataIndex: 'sort', width: 70, align: 'center' },
  { title: '检验项目', dataIndex: 'inspectionItem', width: 180 },
  { title: '检验内容', dataIndex: 'standardDesc', minWidth: 260 },
  { title: '是/符合', dataIndex: 'yes', width: 70, align: 'center' },
  { title: '否/不符合', dataIndex: 'no', width: 80, align: 'center' },
  { title: '备注/异常说明', dataIndex: 'remark', minWidth: 180 },
];

const [Modal, modalApi] = useVbenModal({
  title: '出货检验详情 (OQC)',
  class: 'w-[980px]',
  onOpenChange: async (isOpen) => {
    if (!isOpen) return;
    loading.value = true;
    try {
      record.value = await getOqcDetail(modalApi.getData().id);
    } finally {
      loading.value = false;
    }
  },
});

function getChecklistResult(item: MesOqcApi.OqcItem) {
  const sample = item.sampleValues?.[0];
  const sampleResult =
    sample && typeof sample === 'object'
      ? sample.value ?? sample.qualitativeValue ?? sample.sampleResult
      : sample;
  return item.itemResult === '-' ? sampleResult : item.itemResult;
}

function getChecklistRemark(item: MesOqcApi.OqcItem) {
  const sample = item.sampleValues?.[0];
  return sample && typeof sample === 'object' ? sample.remark : undefined;
}

function categoryLabel(category?: string) {
  if (category === 'COA') return '随货 COA';
  if (category === 'LABEL') return '包装盒及产品标签';
  if (category === 'PACKING') return '打包';
  if (category === 'OTHER') return '其他检查项';
  if (category === 'PRODUCT') return '产品检查项';
  return '检查项';
}

function categoryColor(category?: string) {
  if (category === 'COA') return 'blue';
  if (category === 'LABEL') return 'cyan';
  if (category === 'PACKING') return 'orange';
  if (category === 'OTHER') return 'purple';
  if (category === 'PRODUCT') return 'geekblue';
  return 'default';
}

function checklistNo(item: MesOqcApi.OqcItem) {
  return item.sort ? Math.round(item.sort / 10) : '-';
}

function printCustomerCOA() {
  window.print();
}

function judgmentLabel(judgment?: MesOqcApi.Judgment) {
  if (judgment === 'OK') return '合格/允许发货';
  if (judgment === 'NG') return '异常/拦截';
  if (judgment === 'NA') return '不适用';
  return '待判定';
}
</script>

<template>
  <Modal :footer="false">
    <Spin :spinning="loading">
      <div v-if="record" class="p-2">
        <div class="mb-5 flex items-center justify-between border-b pb-4">
          <h2 class="m-0 flex items-center text-xl font-black text-slate-800">
            <IconifyIcon class="mr-2 text-indigo-600" icon="lucide:truck" />
            {{ record.oqcNo }}
          </h2>
          <div class="flex items-center gap-3">
            <Tag :color="record.judgment === 'OK' ? 'success' : record.judgment === 'NG' ? 'error' : 'default'" class="px-3 py-0.5 text-base font-bold">
              {{ record.judgment === 'OK' ? '准许发货' : record.judgment === 'NG' ? '发货拦截' : '待判定' }}
            </Tag>
            <Button v-if="showCustomerCoaPrint && record.judgment === 'OK'" type="primary" @click="printCustomerCOA">
              <IconifyIcon class="mr-1" icon="lucide:printer" />
              打印出货 COA
            </Button>
          </div>
        </div>

        <Descriptions bordered size="small" :column="3" class="mb-5 bg-white" :label-style="{ fontWeight: 'bold', width: '108px', backgroundColor: '#f8fafc' }">
          <Descriptions.Item v-if="record.productType !== 'SAMPLE'" label="收货客户" :span="2">
            <span>{{ record.customerName || record.customerCode || '-' }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="产品名称" :span="2">
            <span class="font-bold">{{ record.materialName || record.modelCode || '-' }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="批次号">
            <span class="border bg-slate-100 px-1 font-mono font-bold">{{ record.batchNo }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="片，总计">
            <span class="font-bold text-orange-600">{{ record.shippingPieceQty ?? record.shippingQty ?? '-' }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="标准版本">
            <span class="font-mono">{{ record.standardNo }} / {{ record.standardVersion }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="检验员">{{ record.inspectorName || '-' }}</Descriptions.Item>
          <Descriptions.Item label="检测时间">{{ record.inspectionTime || '-' }}</Descriptions.Item>
          <Descriptions.Item label="审核员">{{ record.qaInspectorName || '-' }}</Descriptions.Item>
          <Descriptions.Item label="审核时间">{{ record.qaTime || '-' }}</Descriptions.Item>
          <Descriptions.Item label="最终判定">
            <Tag :color="record.judgment === 'OK' ? 'success' : record.judgment === 'NG' ? 'error' : 'default'">
              {{ judgmentLabel(record.judgment) }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="放行结果">
            <Tag v-if="record.releaseResult === 'ALLOW_SHIPMENT'" color="success">允许发货</Tag>
            <Tag v-else-if="record.releaseResult === 'FREEZE_SHIPMENT'" color="error">冻结发货</Tag>
            <span v-else>-</span>
          </Descriptions.Item>
          <Descriptions.Item v-if="record.judgment === 'NG'" label="异常单号" :span="2">
            <span class="font-mono font-bold text-red-600">{{ record.relatedNcrNo || '-' }}</span>
          </Descriptions.Item>
          <Descriptions.Item v-if="record.judgment === 'NG'" label="NCR状态">
            {{ record.ncrStatus || '-' }}
          </Descriptions.Item>
        </Descriptions>

        <Table :data-source="record.items" :columns="columns" size="small" bordered :pagination="false" class="bg-white">
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.dataIndex === 'category'">
              <Tag :color="categoryColor(item.category)" class="!m-0 text-[10px]">
                {{ categoryLabel(item.category) }}
              </Tag>
            </template>
            <template v-if="column.dataIndex === 'sort'">
              <span class="font-mono font-bold">{{ checklistNo(item) }}</span>
            </template>
            <template v-if="column.dataIndex === 'yes'">
              <span class="font-bold text-green-600">{{ getChecklistResult(item) === 'OK' ? '√' : '' }}</span>
            </template>
            <template v-if="column.dataIndex === 'no'">
              <span class="font-bold text-red-600">{{ getChecklistResult(item) === 'NG' ? '√' : '' }}</span>
            </template>
            <template v-if="column.dataIndex === 'remark'">
              <span class="text-xs">{{ getChecklistRemark(item) || '-' }}</span>
            </template>
          </template>
        </Table>
      </div>
    </Spin>
  </Modal>
</template>
