<script lang="ts" setup>
import type { MesHcLotInstanceApi } from '#/api/mes/hc/lotinstance';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Descriptions, DescriptionsItem, Empty, Spin } from 'ant-design-vue';

import { getLotInstanceDetail } from '#/api/mes/hc/lotinstance';

const detail = ref<MesHcLotInstanceApi.LotInstance>();
const loading = ref(false);
const instanceStatusOptions = [
  { label: '已预占', value: 'RESERVED' },
  { label: '已生成', value: 'GENERATED' },
  { label: '已生成（历史）', value: 'ACTIVE' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已取消', value: 'CANCELLED' },
];
const batchLevelOptions = [
  { label: '主批', value: 'ROOT' },
  { label: '工序批', value: 'OPERATION' },
  { label: '片号', value: 'PIECE' },
];
const generateSourceOptions = [
  { label: '计划预览', value: 'PLAN_PREVIEW' },
  { label: '配方开工', value: 'FORMULA_REPORT' },
  { label: '工序报工', value: 'OPERATION_REPORT' },
];

const ruleFormatEntries = computed(() => toEntries(detail.value?.ruleFormatSnapshot));
const segmentEntries = computed(() => toEntries(detail.value?.segmentValues));
const contextEntries = computed(() => toEntries(detail.value?.generationContext));
const attributeEntries = computed(() => toEntries(detail.value?.productionAttributes));
const formatSegments = computed(() => {
  const value = detail.value?.ruleFormatSnapshot?.segments;
  return Array.isArray(value) ? value : [];
});

function toEntries(source?: Record<string, any>) {
  if (!source) return [];
  return Object.entries(source)
    .filter(([key]) => key !== 'segments')
    .map(([key, value]) => ({ key, value: formatValue(value) }));
}

function formatValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  if (Array.isArray(value)) return value.map((item) => String(item)).join('、');
  if (typeof value === 'object') return `包含 ${Object.keys(value as Record<string, unknown>).length} 项`;
  return String(value);
}

function labelOf(options: Array<{ label: string; value: string }>, value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-lot-instance-detail-modal',
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<MesHcLotInstanceApi.LotInstance>();
    if (!data?.id) return;
    loading.value = true;
    try {
      detail.value = await getLotInstanceDetail(data.id);
    } finally {
      loading.value = false;
    }
  },
  onClosed() {
    detail.value = undefined;
  },
});
</script>

<template>
  <Modal :title="`${detail?.productionBatchNo || detail?.lotNo || '批次实例'} - 详情`">
    <Spin :spinning="loading" class="hc-lot-instance-detail">
      <template v-if="detail">
        <section>
          <h3>批次与规则</h3>
          <Descriptions bordered :column="3" size="small">
            <DescriptionsItem label="生产批号">{{ detail.productionBatchNo || detail.lotNo || '-' }}</DescriptionsItem>
            <DescriptionsItem label="批次层级">{{ labelOf(batchLevelOptions, detail.batchLevel) }}</DescriptionsItem>
            <DescriptionsItem label="生成状态">{{ labelOf(instanceStatusOptions, detail.instanceStatus) }}</DescriptionsItem>
            <DescriptionsItem label="规则名称">{{ detail.ruleName || detail.ruleCode || '-' }}</DescriptionsItem>
            <DescriptionsItem label="规则版本">{{ detail.ruleVersion ? `V${detail.ruleVersion}` : '-' }}</DescriptionsItem>
            <DescriptionsItem label="规则格式">{{ detail.ruleFormatSummary || '-' }}</DescriptionsItem>
            <DescriptionsItem label="垫型">{{ detail.productCategoryCode || '-' }}</DescriptionsItem>
            <DescriptionsItem label="生产类型">{{ detail.prodType || '-' }}</DescriptionsItem>
            <DescriptionsItem label="产品型号">{{ detail.modelCode || '-' }}</DescriptionsItem>
          </Descriptions>
        </section>

        <section>
          <h3>关联业务</h3>
          <Descriptions bordered :column="3" size="small">
            <DescriptionsItem label="生产计划号">{{ detail.planNo || '-' }}</DescriptionsItem>
            <DescriptionsItem label="物料">{{ [detail.materialCode, detail.materialName].filter(Boolean).join(' / ') || '-' }}</DescriptionsItem>
            <DescriptionsItem label="关联工序">{{ [detail.operationCode, detail.operationName].filter(Boolean).join(' / ') || '-' }}</DescriptionsItem>
            <DescriptionsItem label="上游批号">{{ detail.parentProductionBatchNo || detail.parentLotNo || '-' }}</DescriptionsItem>
            <DescriptionsItem label="生产线">{{ [detail.lineCode, detail.lineName].filter(Boolean).join(' / ') || '-' }}</DescriptionsItem>
            <DescriptionsItem label="生成来源">{{ labelOf(generateSourceOptions, detail.generateSource) }}</DescriptionsItem>
            <DescriptionsItem label="生成时间">{{ detail.generatedTime || '-' }}</DescriptionsItem>
            <DescriptionsItem label="操作人">{{ detail.operatorName || '-' }}</DescriptionsItem>
            <DescriptionsItem label="生成时机 / 范围">{{ [detail.generationTrigger, detail.generationScope].filter(Boolean).join(' / ') || '-' }}</DescriptionsItem>
          </Descriptions>
        </section>

        <section>
          <h3>批号组成解析</h3>
          <Descriptions v-if="segmentEntries.length" bordered :column="4" size="small">
            <DescriptionsItem v-for="item in segmentEntries" :key="item.key" :label="item.key">{{ item.value }}</DescriptionsItem>
          </Descriptions>
          <Empty v-else description="该历史实例尚未记录规则段取值" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </section>

        <section>
          <h3>规则快照</h3>
          <Descriptions v-if="ruleFormatEntries.length" bordered :column="3" size="small">
            <DescriptionsItem v-for="item in ruleFormatEntries" :key="item.key" :label="item.key">{{ item.value }}</DescriptionsItem>
          </Descriptions>
          <div v-if="formatSegments.length" class="hc-lot-instance-segment-list">
            <div v-for="(segment, index) in formatSegments" :key="`${segment.code || 'segment'}-${index}`" class="hc-lot-instance-segment-card">
              <strong>{{ segment.name || segment.code || `规则段${index + 1}` }}</strong>
              <span>{{ segment.type || '-' }}</span>
              <span>{{ segment.value || (segment.length ? `${segment.length} 位` : '-') }}</span>
            </div>
          </div>
          <Empty v-if="!ruleFormatEntries.length && !formatSegments.length" description="该历史实例尚未记录规则格式快照" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </section>

        <section class="hc-lot-instance-detail__two-column">
          <div>
            <h3>生成上下文</h3>
            <Descriptions v-if="contextEntries.length" bordered :column="1" size="small">
              <DescriptionsItem v-for="item in contextEntries" :key="item.key" :label="item.key">{{ item.value }}</DescriptionsItem>
            </Descriptions>
            <Empty v-else description="无生成上下文" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
          <div>
            <h3>生产属性快照</h3>
            <Descriptions v-if="attributeEntries.length" bordered :column="1" size="small">
              <DescriptionsItem v-for="item in attributeEntries" :key="item.key" :label="item.key">{{ item.value }}</DescriptionsItem>
            </Descriptions>
            <Empty v-else description="无生产属性快照" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </section>
      </template>
    </Spin>
  </Modal>
</template>

<style scoped>
.hc-lot-instance-detail {
  display: block;
  min-height: 100%;
}
.hc-lot-instance-detail :deep(.ant-spin-container) {
  display: grid;
  gap: 18px;
}
.hc-lot-instance-detail h3 {
  margin: 0 0 8px;
  color: var(--ant-color-text);
  font-size: 14px;
}
.hc-lot-instance-detail__two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.hc-lot-instance-segment-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
  margin-top: 10px;
}
.hc-lot-instance-segment-card {
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
  background: var(--ant-color-fill-quaternary);
}
.hc-lot-instance-segment-card span {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
</style>
