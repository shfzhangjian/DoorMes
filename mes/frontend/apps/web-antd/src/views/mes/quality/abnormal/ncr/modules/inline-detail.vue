<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import { computed, ref, watch } from 'vue';

import {
  Alert,
  Descriptions,
  Empty,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  getNcrRecord,
  getNcrPackagingFlowRecord,
  type MesNcrApi,
} from '#/api/mes/quality/abnormal/ncr';

defineOptions({ name: 'QmsNcrInlineDetail' });

const props = defineProps<{
  id?: number | string;
  packagingReadOnly?: boolean;
}>();

const loading = ref(false);
const loadError = ref(false);
let loadVersion = 0;
const record = ref<MesNcrApi.NcrRecord>();

const reviewRows = computed(() => record.value?.reviews || []);
const flowRows = computed(() => record.value?.flowLogs || []);
const relationRows = computed(() =>
  (record.value?.relations || []).filter(
    (item) => item.relationType !== 'ATTACHMENT',
  ),
);
const isRawMaterialNcr = computed(
  () => record.value?.sourceType === 'RAW_MATERIAL',
);

const reviewColumns: TableColumnsType = [
  { dataIndex: 'deptName', key: 'deptName', title: '会签单位', width: 130 },
  {
    customRender: ({ text }) => translateDisposition(text),
    dataIndex: 'suggestedDisposition',
    key: 'suggestedDisposition',
    title: '建议处置',
    width: 110,
  },
  { dataIndex: 'handlerUserName', key: 'handlerUserName', title: '办理人', width: 110 },
  {
    customRender: ({ text }) => translateReviewStatus(text),
    dataIndex: 'reviewStatus',
    key: 'reviewStatus',
    title: '状态',
    width: 110,
  },
  { dataIndex: 'causeAnalysis', key: 'causeAnalysis', title: '原因分析' },
  { dataIndex: 'reviewOpinion', key: 'reviewOpinion', title: '评审意见' },
];

const flowColumns: TableColumnsType = [
  { dataIndex: 'actionName', key: 'actionName', title: '动作', width: 110 },
  { dataIndex: 'fromNodeName', key: 'fromNodeName', title: '原节点', width: 120 },
  { dataIndex: 'toNodeName', key: 'toNodeName', title: '目标节点', width: 120 },
  { dataIndex: 'handlerUserName', key: 'handlerUserName', title: '办理人', width: 110 },
  { dataIndex: 'handleTime', key: 'handleTime', title: '办理时间', width: 170 },
  { dataIndex: 'opinion', key: 'opinion', title: '办理意见' },
];

const relationColumns: TableColumnsType = [
  { dataIndex: 'relationType', key: 'relationType', title: '关系类型', width: 120 },
  { dataIndex: 'relatedObjectNo', key: 'relatedObjectNo', title: '关联单号', width: 160 },
  { dataIndex: 'relatedObjectName', key: 'relatedObjectName', title: '关联对象' },
  { dataIndex: 'remark', key: 'remark', title: '备注' },
];

watch(
  () => props.id,
  () => {
    void loadDetail();
  },
  { immediate: true },
);

async function loadDetail() {
  const version = ++loadVersion;
  const id = Number(props.id || 0);
  record.value = undefined;
  loadError.value = false;
  if (!id) {
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const result = await (props.packagingReadOnly
      ? getNcrPackagingFlowRecord(id)
      : getNcrRecord(id));
    if (version === loadVersion) record.value = result;
  } catch {
    if (version === loadVersion) loadError.value = true;
  } finally {
    if (version === loadVersion) loading.value = false;
  }
}

function displayValue(value: any) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.filter(Boolean).join('、') || '-';
  }
  return String(value);
}

function translateStatus(value?: string) {
  const map: Record<string, { color: string; text: string }> = {
    APPROVING: { color: 'processing', text: '审批中' },
    CANCELLED: { color: 'default', text: '已取消' },
    CLOSE_CONFIRM: { color: 'processing', text: '关闭确认' },
    CLOSED: { color: 'success', text: '已关闭' },
    CONTAINMENT: { color: 'processing', text: '围堵处理中' },
    CONTENT_CONFIRM: { color: 'processing', text: '再次确认' },
    DRAFT: { color: 'default', text: '草稿' },
    EFFECT_CONFIRM: { color: 'processing', text: '效果确认' },
    EXECUTION_ASSIGN: { color: 'processing', text: '分派执行' },
    FINAL_APPROVAL: { color: 'processing', text: '终审' },
    MRB_REVIEW: { color: 'warning', text: 'MRB会签' },
    PENDING_STOCK_DISPOSE: { color: 'warning', text: '待库存处置' },
    QUALITY_CONFIRM: { color: 'processing', text: '品质确认' },
    RETURNED: { color: 'error', text: '已退回' },
    REVIEW_ASSIGN: { color: 'processing', text: '分派会签单位' },
    ROOT_CAUSE: { color: 'processing', text: '根因分析' },
    SUBMITTED: { color: 'processing', text: '已提交' },
    VERIFYING: { color: 'processing', text: '验证中' },
  };
  return map[value || ''] || { color: 'default', text: displayValue(value) };
}

function translateDisposition(value?: string) {
  const map: Record<string, string> = {
    CONCESSION: '特采',
    PICK: '挑选',
    RECUT: '改切',
    REWORK: '返工',
    RETURN: '退货',
    SCRAP: '报废',
  };
  return map[value || ''] || displayValue(value);
}

function translateReviewStatus(value?: string) {
  const map: Record<string, string> = {
    APPROVED: '已确认',
    DRAFT: '草稿',
    PENDING: '待办理',
    REJECTED: '已退回',
    SUBMITTED: '已提交',
  };
  return map[value || ''] || displayValue(value);
}

function displayResponsibilityDeptNames() {
  const names = displayValue(record.value?.responsibilityDeptNames);
  if (names !== '-') {
    return names;
  }
  return displayValue(record.value?.responsibilityDeptCodes);
}
</script>

<template>
  <Spin :spinning="loading">
    <Alert v-if="loadError" type="error" show-icon message="流转详情加载失败，请重新打开。" />
    <div v-if="record" class="qms-ncr-inline-detail">
      <section class="qms-ncr-inline-detail__section">
        <div class="qms-ncr-inline-detail__title">
          <span>{{
            record.ncNo ||
            (isRawMaterialNcr ? '原材料不合格处置单' : '不合格品处置单')
          }}</span>
          <Tag :color="translateStatus(record.status).color">
            {{ translateStatus(record.status).text }}
          </Tag>
        </div>
        <Descriptions :column="{ md: 2, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="类型">
            {{ displayValue(record.sourceTypeName || record.sourceType) }}
          </Descriptions.Item>
          <Descriptions.Item label="发生日期">
            {{ displayValue(record.happenTime) }}
          </Descriptions.Item>
          <Descriptions.Item
            :label="isRawMaterialNcr ? '发生部门' : '发生工序'"
          >
            {{
              displayValue(
                isRawMaterialNcr ? record.happenDeptName : record.processName,
              )
            }}
          </Descriptions.Item>
          <Descriptions.Item label="批号">
            {{ displayValue(record.lotNo) }}
          </Descriptions.Item>
          <Descriptions.Item label="物料">
            {{ displayValue(record.materialCode || record.materialName) }}
          </Descriptions.Item>
          <Descriptions.Item label="数量">
            {{ displayValue(record.defectQty) }} {{ record.unitCode || '' }}
          </Descriptions.Item>
          <Descriptions.Item label="不合格等级">
            {{ displayValue(record.ncLevelName || record.ncLevel) }}
          </Descriptions.Item>
          <Descriptions.Item label="责任部门">
            {{ displayResponsibilityDeptNames() }}
          </Descriptions.Item>
          <Descriptions.Item label="当前节点">
            {{ displayValue(record.currentNodeName) }}
          </Descriptions.Item>
          <Descriptions.Item v-if="isRawMaterialNcr" label="异常类别">
            {{
              displayValue(
                record.rawMaterialAbnormalCategoryName ||
                  record.rawMaterialAbnormalCategory,
              )
            }}
          </Descriptions.Item>
          <Descriptions.Item v-if="isRawMaterialNcr" label="是否隔离">
            {{ record.isolatedFlag ? '是' : '否' }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="不良描述">
            {{ displayValue(record.ncDescription) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="qms-ncr-inline-detail__section">
        <div class="qms-ncr-inline-detail__section-title">处置结论</div>
        <Descriptions :column="{ md: 2, sm: 1, xs: 1 }" bordered size="small">
          <Descriptions.Item label="MRB结论">
            {{ displayValue(record.mrbDecision) }}
          </Descriptions.Item>
          <Descriptions.Item label="最终处置">
            {{ translateDisposition(record.finalDisposition) }}
          </Descriptions.Item>
          <Descriptions.Item label="库存处置">
            {{ displayValue(record.stockDisposeStatus) }}
          </Descriptions.Item>
          <Descriptions.Item label="关闭时间">
            {{ displayValue(record.closeTime) }}
          </Descriptions.Item>
          <Descriptions.Item :span="2" label="最终意见">
            {{ displayValue(record.finalOpinion) }}
          </Descriptions.Item>
          <Descriptions.Item
            v-if="isRawMaterialNcr"
            :span="2"
            label="效果确认"
          >
            {{ displayValue(record.effectConfirmResult) }}
          </Descriptions.Item>
          <Descriptions.Item v-if="isRawMaterialNcr" label="确认人">
            {{ displayValue(record.effectConfirmUserName) }}
          </Descriptions.Item>
          <Descriptions.Item v-if="isRawMaterialNcr" label="确认日期">
            {{ displayValue(record.effectConfirmTime) }}
          </Descriptions.Item>
        </Descriptions>
      </section>

      <section class="qms-ncr-inline-detail__section">
        <div class="qms-ncr-inline-detail__section-title">会签单位会签</div>
        <Table
          :columns="reviewColumns"
          :data-source="reviewRows"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 900 }"
          size="small"
        />
      </section>

      <section v-if="relationRows.length > 0" class="qms-ncr-inline-detail__section">
        <div class="qms-ncr-inline-detail__section-title">来源与关联</div>
        <Table
          :columns="relationColumns"
          :data-source="relationRows"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 760 }"
          size="small"
        />
      </section>

      <section class="qms-ncr-inline-detail__section">
        <div class="qms-ncr-inline-detail__section-title">流转记录</div>
        <Table
          :columns="flowColumns"
          :data-source="flowRows"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 900 }"
          size="small"
        />
      </section>
    </div>
    <Empty v-else description="暂无 NCR 业务详情" />
  </Spin>
</template>

<style lang="scss" scoped>
.qms-ncr-inline-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  font-size: 14px;
  line-height: 20px;
}

.qms-ncr-inline-detail__section {
  min-width: 0;
}

.qms-ncr-inline-detail__title,
.qms-ncr-inline-detail__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #172033;
  font-weight: 700;
}

.qms-ncr-inline-detail__title {
  font-size: 17px;
}

.qms-ncr-inline-detail__section-title {
  font-size: 15px;
}

.qms-ncr-inline-detail :deep(.ant-descriptions-item-label) {
  width: 116px;
  color: #536176;
  font-size: 14px;
  font-weight: 600;
}

.qms-ncr-inline-detail :deep(.ant-descriptions-item-content) {
  font-size: 14px;
}

.qms-ncr-inline-detail :deep(.ant-table) {
  font-size: 14px;
}

.qms-ncr-inline-detail :deep(.ant-table-cell) {
  line-height: 20px;
}
</style>
