<script lang="ts" setup>
import type { SrmCrudRecord } from '../../shared/crud';
import type { SrmPreliminaryEvaluationApi } from '#/api/mes/srm/preliminary-evaluation';
import type { SrmSupplierCandidateApi } from '#/api/mes/srm/supplier-candidate';
import type { SrmSupplierFileApi } from '#/api/mes/srm/supplier-file';

import { computed, ref, watch } from 'vue';

import { Button, Modal as AntModal, Table, TabPane, Tabs, Tag } from 'ant-design-vue';

import { getEvaluationPage } from '#/api/mes/srm/preliminary-evaluation';
import { getSupplierResourceStatusLogs } from '#/api/mes/srm/supplier-candidate';
import { getSupplierFilePage } from '#/api/mes/srm/supplier-file';
import PreliminaryEvaluationInlineDetail from '#/views/mes/srm/certification/preliminary-evaluation/modules/inline-detail.vue';

const props = defineProps<{
  record: SrmCrudRecord;
}>();

const SRM_RELATION_PREVIEW_Z_INDEX = 5700;

const activeTab = ref('lifecycle');
const loading = ref(false);
const supplierFiles = ref<SrmSupplierFileApi.SupplierFile[]>([]);
const preliminaryEvaluations = ref<SrmPreliminaryEvaluationApi.Evaluation[]>([]);
const resourceStatusLogs = ref<SrmSupplierCandidateApi.ResourceStatusLog[]>([]);
const preliminaryPreviewOpen = ref(false);
const preliminaryPreviewId = ref<number>();

const supplierCode = computed(() => String(props.record?.supplierCode || '').trim());
const supplierId = computed(() => {
  const id = props.record?.supplierId || props.record?.id;
  const numericId = Number(id);
  return Number.isFinite(numericId) && numericId > 0 ? numericId : undefined;
});
const canViewSensitiveResource = computed(() =>
  ['EDIT', 'FULL'].includes(String(props.record?.viewPermission || '')),
);

const statusTextMap: Record<string, string> = {
  ELIMINATED: '淘汰',
  EXITED: '退出',
  FROZEN: '冻结',
  PENDING: '考察中',
  QUALIFIED: '合格',
  UNQUALIFIED: '不合格',
};

const preliminaryStatusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  GM_REVIEW: '总经理办理',
  PENDING_CALCULATION: '待计算',
  PENDING_DECISION: '待最终判定',
  PENDING_PUBLISH: '待发布',
  PUBLISHED: '已发布',
  SCORING: '评分中',
};

const preliminaryStatusColorMap: Record<string, string> = {
  DRAFT: 'default',
  GM_REVIEW: 'processing',
  PENDING_CALCULATION: 'warning',
  PENDING_DECISION: 'warning',
  PENDING_PUBLISH: 'processing',
  PUBLISHED: 'success',
  SCORING: 'processing',
};

const decisionTextMap: Record<string, string> = {
  QUALIFIED: '合格',
  UNQUALIFIED: '不合格',
};

const lifecycleColumns = [
  { dataIndex: 'node', title: '节点', width: 160 },
  { dataIndex: 'reference', title: '业务编号/范围', width: 260 },
  { dataIndex: 'status', title: '状态', width: 140 },
  { dataIndex: 'remark', title: '说明' },
];

const businessColumns = [
  { dataIndex: 'bizType', title: '业务类型', width: 180 },
  { dataIndex: 'bizNo', title: '单据编号', width: 260 },
  { dataIndex: 'status', title: '状态', width: 160 },
  { dataIndex: 'remark', title: '说明' },
];

const preliminaryColumns = [
  { dataIndex: 'evaluationNo', title: '初评单号', width: 220 },
  { dataIndex: 'templateNameSnapshot', title: '评估模板', width: 220 },
  { dataIndex: 'templateVersionSnapshot', title: '模板版本', width: 120 },
  { dataIndex: 'status', title: '当前状态', width: 140 },
  { dataIndex: 'totalScoreDisplay', title: '最终得分', width: 120 },
  { dataIndex: 'finalDecision', title: '最终判定', width: 140 },
  { dataIndex: 'updateTime', title: '更新时间', width: 180 },
  { dataIndex: 'actions', fixed: 'right' as const, title: '操作', width: 100 },
];

const fileColumns = [
  { dataIndex: 'fileType', title: '档案类型', width: 160 },
  { dataIndex: 'fileName', title: '档案名称', width: 260 },
  { dataIndex: 'fileStatus', title: '过期状态', width: 120 },
  { dataIndex: 'expiryDate', title: '有效期至', width: 140 },
  { dataIndex: 'attachmentName', title: '附件', width: 260 },
  { dataIndex: 'updateTime', title: '更新时间', width: 180 },
  { dataIndex: 'remark', title: '档案摘要' },
];

const logColumns = [
  { dataIndex: 'createTime', title: '时间', width: 180 },
  { dataIndex: 'operatorUserName', title: '操作人', width: 160 },
  { dataIndex: 'fromStatus', title: '调整前', width: 120 },
  { dataIndex: 'toStatus', title: '调整后', width: 120 },
  { dataIndex: 'reason', title: '原因说明' },
];

const lifecycleRows = computed(() => [
  {
    key: 'master',
    node: '供应商主数据',
    reference: valueText(props.record?.supplierCode),
    remark: valueText(props.record?.supplierName),
    status: statusText(props.record?.status),
  },
  {
    key: 'scope',
    node: '名录范围',
    reference: valueText(props.record?.scopeName || props.record?.scopeCode),
    remark: '按供应商名录管理范围控制查询、查看和编辑权限',
    status: props.record?.scopeName || props.record?.scopeCode ? '已配置' : '未配置',
  },
  {
    key: 'survey',
    node: '基本情况调查',
    reference: valueText(props.record?.sourceSurveyNo),
    remark: props.record?.sourceSurveyNo ? '已关联调查记录' : '暂无关联调查记录',
    status: props.record?.sourceSurveyNo ? '已关联' : '未关联',
  },
]);

const businessRows = computed(() => [
  {
    key: 'supplier',
    bizNo: valueText(props.record?.supplierCode),
    bizType: '供应商档案',
    remark: valueText(props.record?.supplierName),
    status: statusText(props.record?.status),
  },
  ...preliminaryEvaluations.value.map((item) => ({
    businessId: item.id,
    bizNo: item.evaluationNo,
    bizType: '选择初评',
    key: `preliminary-${item.id}`,
    remark: valueText(item.templateNameSnapshot),
    status: preliminaryStatusText(item.status),
  })),
  ...supplierFiles.value.map((item) => ({
    bizNo: item.fileName,
    bizType: '合规资料',
    key: `file-${item.id}`,
    remark: valueText(item.attachmentName || item.remark),
    status: fileStatusText(item.fileStatus),
  })),
]);

const performanceRows = computed(() => [
  { key: 'quarter', node: '季度绩效评定', status: '待关联', remark: '后续由季度绩效评定单据汇总' },
  { key: 'annual', node: '年度绩效评定', status: '待关联', remark: '后续由年度绩效评定单据汇总' },
  { key: 'audit', node: '供方稽核', status: '待关联', remark: '后续由供方评审计划和记录汇总' },
]);

const performanceColumns = [
  { dataIndex: 'node', title: '业务项', width: 180 },
  { dataIndex: 'status', title: '状态', width: 120 },
  { dataIndex: 'remark', title: '说明' },
];

watch(
  () => [supplierId.value, supplierCode.value],
  () => {
    void loadRelations();
  },
  { immediate: true },
);

async function loadRelations() {
  supplierFiles.value = [];
  preliminaryEvaluations.value = [];
  resourceStatusLogs.value = [];
  if (!supplierCode.value && !supplierId.value) {
    return;
  }
  loading.value = true;
  try {
    const tasks: Array<Promise<unknown>> = [];
    if (supplierCode.value) {
      tasks.push(
        getSupplierFilePage({
          pageNo: 1,
          pageSize: 50,
          supplierCode: supplierCode.value,
        }).then((result) => {
          supplierFiles.value = result.list || [];
        }),
      );
      tasks.push(
        getEvaluationPage({
          pageNo: 1,
          pageSize: 100,
          supplierCode: supplierCode.value,
        }).then((result) => {
          preliminaryEvaluations.value = (result.list || []).filter(
            (item) => String(item.supplierCode || '').trim() === supplierCode.value,
          );
        }),
      );
    }
    if (supplierId.value) {
      tasks.push(
        getSupplierResourceStatusLogs({
          supplierId: supplierId.value,
        }).then((logs) => {
          resourceStatusLogs.value = logs || [];
        }),
      );
    }
    await Promise.all(tasks);
  } finally {
    loading.value = false;
  }
}

function openPreliminary(record: SrmPreliminaryEvaluationApi.Evaluation & Record<string, any>) {
  const evaluationId = record.id || record.businessId;
  if (!evaluationId) {
    return;
  }
  preliminaryPreviewId.value = Number(evaluationId);
  preliminaryPreviewOpen.value = true;
}

function valueText(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function statusText(status?: unknown) {
  return statusTextMap[String(status || '')] || valueText(status);
}

function preliminaryStatusText(status?: unknown) {
  return preliminaryStatusTextMap[String(status || '')] || valueText(status);
}

function preliminaryStatusColor(status?: unknown) {
  return preliminaryStatusColorMap[String(status || '')] || 'default';
}

function decisionText(value?: unknown) {
  return decisionTextMap[String(value || '')] || valueText(value);
}

function fileStatusText(status?: unknown) {
  const map: Record<string, string> = {
    EXPIRED: '已过期',
    VALID: '有效',
    WARNING: '即将到期',
  };
  return map[String(status || '')] || valueText(status);
}

function fileStatusColor(status?: unknown) {
  const map: Record<string, string> = {
    EXPIRED: 'error',
    VALID: 'success',
    WARNING: 'warning',
  };
  return map[String(status || '')] || 'default';
}
</script>

<template>
  <Tabs v-model:active-key="activeTab" class="shipping-detail-tabs srm-supplier-relation-tabs">
    <TabPane key="lifecycle" tab="生命周期记录">
      <Table
        :columns="lifecycleColumns"
        :data-source="lifecycleRows"
        :loading="loading"
        :pagination="false"
        row-key="key"
        size="small"
      />
    </TabPane>

    <TabPane key="business" tab="关联业务单据">
      <Table
        :columns="businessColumns"
        :data-source="businessRows"
        :loading="loading"
        :pagination="false"
        row-key="key"
        size="small"
      >
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.dataIndex === 'bizNo'">
            <a
              v-if="row.bizType === '选择初评' && row.businessId"
              class="srm-crud-link"
              @click="openPreliminary(row)"
            >
              {{ valueText(row.bizNo) }}
            </a>
            <span v-else>{{ valueText(row.bizNo) }}</span>
          </template>
          <template v-else>
            {{ valueText(row[column.dataIndex]) }}
          </template>
        </template>
      </Table>
    </TabPane>

    <TabPane key="preliminary" :tab="`选择初评(${preliminaryEvaluations.length})`">
      <Table
        :columns="preliminaryColumns"
        :data-source="preliminaryEvaluations"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1220 }"
        size="small"
      >
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.dataIndex === 'evaluationNo'">
            <a class="srm-crud-link" @click="openPreliminary(row)">
              {{ valueText(row.evaluationNo) }}
            </a>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <Tag :color="preliminaryStatusColor(row.status)" class="!m-0">
              {{ preliminaryStatusText(row.status) }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'finalDecision'">
            <Tag v-if="row.finalDecision" color="success" class="!m-0">
              {{ decisionText(row.finalDecision) }}
            </Tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'actions'">
            <Button size="small" type="link" @click="openPreliminary(row)">查看</Button>
          </template>
          <template v-else>
            {{ valueText(row[column.dataIndex]) }}
          </template>
        </template>
      </Table>
    </TabPane>

    <TabPane key="files" :tab="`合规资料(${supplierFiles.length})`">
      <Table
        :columns="fileColumns"
        :data-source="supplierFiles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1280 }"
        size="small"
      >
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.dataIndex === 'fileStatus'">
            <Tag :color="fileStatusColor(row.fileStatus)" class="!m-0">
              {{ fileStatusText(row.fileStatus) }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'attachmentName'">
            <a
              v-if="row.attachmentUrl"
              :href="row.attachmentUrl"
              rel="noopener noreferrer"
              target="_blank"
            >
              {{ row.attachmentName || '查看附件' }}
            </a>
            <span v-else>{{ valueText(row.attachmentName) }}</span>
          </template>
          <template v-else>
            {{ valueText(row[column.dataIndex]) }}
          </template>
        </template>
      </Table>
    </TabPane>

    <TabPane key="performance" tab="绩效与稽核">
      <Table
        :columns="performanceColumns"
        :data-source="performanceRows"
        :loading="loading"
        :pagination="false"
        row-key="key"
        size="small"
      />
    </TabPane>

    <TabPane key="logs" tab="操作日志">
      <Table
        :columns="logColumns"
        :data-source="resourceStatusLogs"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.dataIndex === 'fromStatus'">
            {{ statusText(row.fromStatus) }}
          </template>
          <template v-else-if="column.dataIndex === 'toStatus'">
            {{ statusText(row.toStatus) }}
          </template>
          <template v-else>
            {{ valueText(row[column.dataIndex]) }}
          </template>
        </template>
      </Table>
    </TabPane>
  </Tabs>

  <AntModal
    v-model:open="preliminaryPreviewOpen"
    centered
    :destroy-on-close="true"
    :footer="null"
    title="选择初评详情"
    :width="'calc(100vw - 96px)'"
    :z-index="SRM_RELATION_PREVIEW_Z_INDEX"
  >
    <PreliminaryEvaluationInlineDetail
      :hide-scoring-owner="true"
      :id="preliminaryPreviewId"
      :mask-sensitive-fields="!canViewSensitiveResource"
      :show-flow-log="canViewSensitiveResource"
    />
  </AntModal>
</template>

<style scoped>
.srm-supplier-relation-tabs {
  margin-top: 14px;
  border: 1px solid #d9e3f0;
  background: #fff;
}
</style>
