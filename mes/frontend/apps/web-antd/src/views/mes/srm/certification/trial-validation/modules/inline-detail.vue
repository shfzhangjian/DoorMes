<script lang="ts" setup>
import type { SrmTrialValidationApi } from '#/api/mes/srm/trial-validation';

import { computed, ref, watch } from 'vue';

import { Empty, Spin, Table, Tag } from 'ant-design-vue';

import { getTrialValidation } from '#/api/mes/srm/trial-validation';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmTrialValidationInlineDetail' });

const props = defineProps<{ id?: number | string }>();

const BIZ_TYPE = 'SRM_TRIAL_TRACK';
const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '确认归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  NOTICE_SENT: { color: 'blue', text: '发起试生产通知' },
  PRODUCTION_COMPLETE: { color: 'processing', text: '完成生产' },
  TRIAL_EXECUTION: { color: 'processing', text: '试生产执行' },
};
const attachmentCategoryOptions = [
  { label: '试生产通知', value: 'TRIAL_NOTICE' },
  { label: '试生产执行记录', value: 'TRIAL_EXECUTION_RECORD' },
  { label: '完成生产记录', value: 'PRODUCTION_COMPLETE_RECORD' },
  { label: '其他附件', value: 'OTHER' },
];
const logColumns = [
  { dataIndex: 'actionName', title: '动作', width: 150 },
  { dataIndex: 'operatorName', title: '办理人', width: 130 },
  { dataIndex: 'actionDescription', title: '说明' },
  { dataIndex: 'createTime', title: '时间', width: 180 },
];

const loading = ref(false);
const record = ref<SrmTrialValidationApi.TrialValidation>();

const statusMeta = computed(
  () =>
    statusMetaMap[String(record.value?.status || '')] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);
const processRows = computed(() => [
  {
    handler: record.value?.initiatorUserName,
    key: 'notice',
    node: '发起试生产通知',
    opinion: '下达试生产通知',
    time: record.value?.noticeTime,
  },
  {
    handler: record.value?.trialExecutionUserName,
    key: 'trialExecution',
    node: '试生产执行',
    opinion: record.value?.trialExecutionOpinion,
    time: record.value?.trialExecutionTime,
  },
  {
    handler: record.value?.productionCompleteUserName,
    key: 'productionComplete',
    node: '完成生产',
    opinion: record.value?.productionCompleteOpinion,
    time: record.value?.productionCompleteTime,
  },
  {
    handler: record.value?.archiveUserName,
    key: 'archive',
    node: '确认归档',
    opinion: record.value?.archiveOpinion,
    time: record.value?.archiveTime,
  },
]);
const logs = computed(() =>
  (record.value?.logs || []).map((item) => ({
    ...item,
    actionName: item.actionName || flowActionText(item.action),
  })),
);

watch(
  () => props.id,
  () => {
    void loadDetail();
  },
  { immediate: true },
);

async function loadDetail() {
  const id = normalizeId(props.id);
  if (!id) {
    record.value = undefined;
    return;
  }
  loading.value = true;
  try {
    record.value = await getTrialValidation(id);
  } finally {
    loading.value = false;
  }
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function flowActionText(action?: string) {
  return (
    (
      {
        ARCHIVE: '确认归档',
        CREATE: '创建',
        ISSUE_NOTICE: '下达试生产通知',
        PRODUCTION_COMPLETE: '完成生产',
        TRIAL_EXECUTE: '试生产执行',
        UPDATE: '更新',
      } as Record<string, string>
    )[String(action || '').toUpperCase()] ||
    action ||
    '-'
  );
}
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="record" class="srm-trial-inline srm-erp-crud-modal">
      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>试产验证跟踪</strong>
          </div>
          <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
        </div>
        <div class="erp-form-grid srm-trial-inline__grid">
          <div class="erp-form-item">
            <label class="erp-form-label">跟踪单号</label>
            <div class="erp-form-value">
              {{ displayValue(record.trialNo) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">来源样品评价单</label>
            <div class="erp-form-value">
              {{ displayValue(record.sourceSampleEvaluationNo) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">供应商编码</label>
            <div class="erp-form-value">
              {{ displayValue(record.supplierCode) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">供应商名称</label>
            <div class="erp-form-value">
              {{ displayValue(record.supplierName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料编码</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialCode) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料名称</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">规格型号</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialModel) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料批号</label>
            <div class="erp-form-value">
              {{ displayValue(record.materialBatchNo) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">数量</label>
            <div class="erp-form-value">
              {{ displayValue(record.quantity) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">发起人</label>
            <div class="erp-form-value">
              {{ displayValue(record.initiatorUserName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">当前节点</label>
            <div class="erp-form-value">
              {{ displayValue(record.currentNodeName) }}
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">下达时间</label>
            <div class="erp-form-value">
              {{ displayValue(record.noticeTime) }}
            </div>
          </div>
        </div>
      </section>

      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>流程办理记录</strong>
          </div>
        </div>
        <div class="srm-trial-inline__timeline">
          <div
            v-for="row in processRows"
            :key="row.key"
            class="srm-trial-inline__timeline-row"
          >
            <div class="srm-trial-inline__timeline-node">{{ row.node }}</div>
            <div class="srm-trial-inline__timeline-main">
              <span>办理人：{{ displayValue(row.handler) }}</span>
              <span>办理时间：{{ displayValue(row.time) }}</span>
              <span>意见：{{ displayValue(row.opinion) }}</span>
            </div>
          </div>
        </div>
      </section>

      <SrmAttachmentPanel
        :biz-id="record.id"
        :biz-type="BIZ_TYPE"
        :category-options="attachmentCategoryOptions"
        mode="detail"
      />

      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>业务日志</strong>
          </div>
        </div>
        <Table
          :columns="logColumns"
          :data-source="logs"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #emptyText>
            <Empty description="暂无日志" />
          </template>
        </Table>
      </section>
    </div>
    <Empty v-else description="暂无试产验证数据" />
  </Spin>
</template>

<style scoped>
.srm-trial-inline {
  display: grid;
  gap: 12px;
}

.srm-trial-inline__grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.srm-trial-inline__timeline {
  display: grid;
  gap: 0;
  border-top: 1px solid #dbe5f1;
  border-left: 1px solid #dbe5f1;
}

.srm-trial-inline__timeline-row {
  display: grid;
  min-width: 0;
  border-right: 1px solid #dbe5f1;
  border-bottom: 1px solid #dbe5f1;
  grid-template-columns: 170px minmax(0, 1fr);
}

.srm-trial-inline__timeline-node {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef3f9;
  color: #31465f;
  font-weight: 700;
  padding: 12px;
}

.srm-trial-inline__timeline-main {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding: 12px 14px;
  color: #24364f;
}

.srm-trial-inline__timeline-main span {
  min-width: 180px;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .srm-trial-inline__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .srm-trial-inline__grid,
  .srm-trial-inline__timeline-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
