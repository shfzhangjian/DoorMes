<script lang="ts" setup>
import type { SrmSampleEvaluationApi } from '#/api/mes/srm/sample-evaluation';
import type { SrmSampleRequestApi } from '#/api/mes/srm/sample-req';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Empty, Spin, Tag } from 'ant-design-vue';

import { getSampleEvaluationHistoryPage } from '#/api/mes/srm/sample-evaluation';
import { getSampleRequest } from '#/api/mes/srm/sample-req';

import SrmSampleEvaluationDetailModal from '../../../certification/sample-evaluation/modules/detail-modal.vue';
import TrialValidationDetailModal from '../../../certification/trial-validation/modules/detail-modal.vue';
import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSampleRequestInlineDetail' });

const props = defineProps<{ id?: number | string }>();

type ApprovalRow = {
  handler: string;
  node: string;
  opinion: string;
  time: string;
};

const DIRECT_ARCHIVE_FORM_TEXT = '直接归档表单，具体内容见附件';

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'processing', text: '待归档确认' },
  ARCHIVED: { color: 'success', text: '已归档' },
  DRAFT: { color: 'default', text: '草稿' },
  FINAL_APPROVAL: { color: 'warning', text: '最终批准人审核' },
  INITIATOR_CONFIRM: { color: 'processing', text: '发起人确认' },
  PROJECT_REVIEW: { color: 'processing', text: '项目负责人审核' },
  PURCHASE_REVIEW: { color: 'processing', text: '采购负责人审核' },
};
const sampleEvaluationStatusMetaMap: Record<
  string,
  { color: string; text: string }
> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '归档确认' },
  ARCHIVED: { color: 'success', text: '已归档' },
  DEPT_SIGN: { color: 'processing', text: '部门确认' },
  DRAFT: { color: 'default', text: '草稿' },
  FINAL_APPROVAL: { color: 'warning', text: '最终批准' },
  INITIATOR_CONFIRM: { color: 'processing', text: '发起人确认' },
  INSPECTION_REPORT: { color: 'processing', text: '样品检验上报' },
  VALUE_CONFIRM: { color: 'warning', text: '检验值确认' },
};
const trialValidationStatusMetaMap: Record<
  string,
  { color: string; text: string }
> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '确认归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  NOTICE_SENT: { color: 'blue', text: '发起试生产通知' },
  PRODUCTION_COMPLETE: { color: 'processing', text: '完成生产' },
  TRIAL_EXECUTION: { color: 'processing', text: '试生产执行' },
};
const applyTypeTextMap: Record<string, string> = {
  NORMAL: '普通',
  URGENT: '紧急',
};
const supplierTypeTextMap: Record<string, string> = {
  HAS: '有',
  NONE: '无指定厂家',
  OTHER: '其它',
};
const purchaseDifficultyVisibleStatuses = new Set([
  'ARCHIVE_CONFIRM',
  'ARCHIVED',
  'FINAL_APPROVAL',
  'INITIATOR_CONFIRM',
  'PURCHASE_REVIEW',
]);

const loading = ref(false);
const sampleEvaluationLoading = ref(false);
const record = ref<SrmSampleRequestApi.SampleRequest>();
const sampleEvaluationRows = ref<SrmSampleEvaluationApi.SampleEvaluation[]>([]);

const [SampleEvaluationDetailModal, sampleEvaluationDetailModalApi] =
  useVbenModal({
    connectedComponent: SrmSampleEvaluationDetailModal,
    destroyOnClose: true,
  });

const [TrialValidationDetailModalHost, trialValidationDetailModalApi] =
  useVbenModal({
    connectedComponent: TrialValidationDetailModal,
    destroyOnClose: true,
  });

const statusMeta = computed(
  () =>
    statusMetaMap[record.value?.status || ''] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);
const approvalRows = computed<ApprovalRow[]>(() => [
  {
    handler: displayValue(record.value?.projectLeaderUserName),
    node: '项目负责人审核',
    opinion: displayValue(record.value?.projectLeaderOpinion),
    time: displayValue(record.value?.projectLeaderHandleTime),
  },
  {
    handler: displayValue(record.value?.purchaseOwnerUserName),
    node: '采购负责人审核',
    opinion: displayValue(record.value?.purchaseOwnerOpinion),
    time: displayValue(record.value?.purchaseOwnerHandleTime),
  },
  {
    handler: displayValue(record.value?.finalApproverUserName),
    node: '最终批准人审核',
    opinion: displayValue(record.value?.finalApproverOpinion),
    time: displayValue(record.value?.finalApproverHandleTime),
  },
  {
    handler: displayValue(record.value?.applicantName),
    node: '归档确认',
    opinion: displayValue(record.value?.archiveOpinion),
    time: displayValue(record.value?.archiveTime),
  },
]);
const isDirectArchivedForm = computed(
  () =>
    record.value?.directArchive === true ||
    record.value?.archiveOpinion === DIRECT_ARCHIVE_FORM_TEXT,
);
const shouldShowPurchaseDifficulty = computed(
  () =>
    Boolean(record.value?.processInstanceId) &&
    purchaseDifficultyVisibleStatuses.has(String(record.value?.status || '')),
);
const trialValidationRows = computed(
  () => record.value?.trialValidations || [],
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
    sampleEvaluationRows.value = [];
    return;
  }
  loading.value = true;
  try {
    record.value = await getSampleRequest(id);
    await loadSampleEvaluations(id);
  } finally {
    loading.value = false;
  }
}

async function loadSampleEvaluations(sampleRequestId: number) {
  sampleEvaluationLoading.value = true;
  try {
    const page = await getSampleEvaluationHistoryPage(sampleRequestId, {
      pageNo: 1,
      pageSize: 50,
    });
    sampleEvaluationRows.value = page?.list || [];
  } finally {
    sampleEvaluationLoading.value = false;
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

function applyTypeText(value?: string) {
  if (value === '1') {
    return '普通';
  }
  if (value === '2') {
    return '紧急';
  }
  return applyTypeTextMap[value || ''] || displayValue(value);
}

function supplierTypeText(value?: string) {
  if (value === '1') {
    return '有';
  }
  if (value === '0') {
    return '无指定厂家';
  }
  return supplierTypeTextMap[value || ''] || displayValue(value);
}

function sampleEvaluationStatusText(
  row: SrmSampleEvaluationApi.SampleEvaluation,
) {
  return (
    sampleEvaluationStatusMetaMap[row.status || '']?.text ||
    displayValue(row.currentNodeName || row.status)
  );
}

function sampleEvaluationStatusColor(
  row: SrmSampleEvaluationApi.SampleEvaluation,
) {
  return sampleEvaluationStatusMetaMap[row.status || '']?.color || 'default';
}

function trialValidationStatusText(row: SrmSampleRequestApi.TrialValidation) {
  return (
    trialValidationStatusMetaMap[row.status || '']?.text ||
    displayValue(row.currentNodeName || row.status)
  );
}

function trialValidationStatusColor(row: SrmSampleRequestApi.TrialValidation) {
  return trialValidationStatusMetaMap[row.status || '']?.color || 'default';
}

function openSampleEvaluationDetail(
  row: SrmSampleEvaluationApi.SampleEvaluation,
) {
  if (!row.id) {
    return;
  }
  sampleEvaluationDetailModalApi.setData({ id: row.id }).open();
}

function openTrialValidationDetail(row: SrmSampleRequestApi.TrialValidation) {
  if (!row.id) {
    return;
  }
  trialValidationDetailModalApi.setData({ id: row.id }).open();
}

function handleSampleEvaluationSuccess() {
  const id = normalizeId(record.value?.id);
  if (!id) {
    return;
  }
  void loadDetail();
}

function handleTrialValidationSuccess() {
  void loadDetail();
}
</script>

<template>
  <Spin :spinning="loading">
    <SampleEvaluationDetailModal @success="handleSampleEvaluationSuccess" />
    <TrialValidationDetailModalHost @success="handleTrialValidationSuccess" />
    <div v-if="record" class="srm-sample-inline-detail srm-erp-crud-modal">
      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>样品需求采购申请单</strong>
          </div>
          <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
        </div>
        <div class="erp-form-grid srm-sample-inline-detail__grid">
          <div class="erp-form-item">
            <label class="erp-form-label">申请编号</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.requestNo)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料名称</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.materialName)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">型号</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.materialModel)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请类型</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                applyTypeText(record.applyType)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请部门</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.applyDept)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">使用产品</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.usedProduct)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">需求数量</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.requireQty)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请日期</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.applyDate)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">需求日期</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">{{
                displayValue(record.requireDate)
              }}</span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">有无指定供应商</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ supplierTypeText(record.specifiedSupplierType) }}
                <template v-if="record.supplierName">
                  （{{ record.supplierName }}）
                </template>
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">
              产品技术要求说明
            </label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value--multiline">
                {{ displayValue(record.technicalRequirement) }}
              </span>
            </div>
          </div>
          <div
            v-if="shouldShowPurchaseDifficulty"
            class="erp-form-item erp-form-item--full"
          >
            <label class="erp-form-label erp-form-label--tall">
              采购开发难点
            </label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value--multiline">
                {{ displayValue(record.purchaseDifficulty) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">
              研发样品必要性
            </label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value--multiline">
                {{ displayValue(record.rdSampleNecessity) }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <section v-if="record.id" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>样品评价</strong>
          </div>
        </div>
        <Spin :spinning="sampleEvaluationLoading">
          <div class="sample-req-approval-table-wrap">
            <table
              class="sample-req-approval-table sample-req-evaluation-table"
            >
              <thead>
                <tr>
                  <th>检验时间</th>
                  <th>送样次数</th>
                  <th>样品数量</th>
                  <th>当前步骤</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody v-if="sampleEvaluationRows.length > 0">
                <tr
                  v-for="item in sampleEvaluationRows"
                  :key="item.id || item.evaluationNo"
                >
                  <td>
                    {{ displayValue(item.reportTime || item.evaluationDate) }}
                  </td>
                  <td>{{ displayValue(item.sampleSendCount) }}</td>
                  <td>{{ displayValue(item.sampleQty) }}</td>
                  <td>
                    <Tag :color="sampleEvaluationStatusColor(item)">
                      {{ sampleEvaluationStatusText(item) }}
                    </Tag>
                  </td>
                  <td>
                    <Button
                      class="sample-req-link-button"
                      type="link"
                      @click="openSampleEvaluationDetail(item)"
                    >
                      查看详情
                    </Button>
                  </td>
                </tr>
              </tbody>
              <tbody v-else>
                <tr>
                  <td colspan="5">
                    <div class="sample-req-empty-cell">
                      <Empty description="暂无样品评价" />
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Spin>
      </section>

      <section v-if="record.id" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>试产验证跟踪</strong>
          </div>
        </div>
        <div class="sample-req-approval-table-wrap">
          <table class="sample-req-approval-table sample-req-trial-table">
            <thead>
              <tr>
                <th>跟踪单号</th>
                <th>来源样品评价单</th>
                <th>供应商</th>
                <th>物料名称</th>
                <th>规格型号</th>
                <th>物料批号</th>
                <th>数量</th>
                <th>当前步骤</th>
                <th>下达时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody v-if="trialValidationRows.length > 0">
              <tr
                v-for="item in trialValidationRows"
                :key="item.id || item.trialNo"
              >
                <td>{{ displayValue(item.trialNo) }}</td>
                <td>{{ displayValue(item.sourceSampleEvaluationNo) }}</td>
                <td>
                  {{
                    displayValue(
                      [item.supplierName, item.supplierCode]
                        .filter(Boolean)
                        .join(' / '),
                    )
                  }}
                </td>
                <td>{{ displayValue(item.materialName) }}</td>
                <td>{{ displayValue(item.materialModel) }}</td>
                <td>{{ displayValue(item.materialBatchNo) }}</td>
                <td>{{ displayValue(item.quantity) }}</td>
                <td>
                  <Tag :color="trialValidationStatusColor(item)">
                    {{ trialValidationStatusText(item) }}
                  </Tag>
                </td>
                <td>{{ displayValue(item.noticeTime) }}</td>
                <td>
                  <Button
                    class="sample-req-link-button"
                    type="link"
                    @click="openTrialValidationDetail(item)"
                  >
                    查看详情
                  </Button>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td colspan="10">
                  <div class="sample-req-empty-cell">
                    <Empty description="暂无试产验证跟踪" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="record.id && isDirectArchivedForm" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>归档说明</strong>
          </div>
        </div>
        <div class="sample-req-direct-archive">
          {{ DIRECT_ARCHIVE_FORM_TEXT }}
        </div>
      </section>

      <section v-if="record.id && !isDirectArchivedForm" class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>审批意见</strong>
          </div>
        </div>
        <div class="sample-req-approval-table-wrap">
          <table class="sample-req-approval-table">
            <thead>
              <tr>
                <th>节点</th>
                <th>办理人</th>
                <th>意见/说明</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in approvalRows" :key="item.node">
                <td>{{ item.node }}</td>
                <td>{{ item.handler }}</td>
                <td>{{ item.opinion }}</td>
                <td>{{ item.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <SrmAttachmentPanel
        :biz-id="record.id"
        biz-type="SRM_SAMPLE_REQUEST"
        mode="detail"
      />
    </div>
    <Empty v-else description="暂无样品需求单详情" />
  </Spin>
</template>

<style scoped>
.srm-sample-inline-detail {
  display: grid;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
}

.srm-sample-inline-detail__grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.sample-req-approval-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.sample-req-approval-table {
  width: 100%;
  min-width: 760px;
  border-spacing: 0;
  border-collapse: separate;
}

.sample-req-approval-table th,
.sample-req-approval-table td {
  padding: 10px 12px;
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  text-align: left;
  vertical-align: top;
}

.sample-req-approval-table th {
  font-weight: 700;
  color: #26364f;
  background: #eef4fa;
}

.sample-req-approval-table th:last-child,
.sample-req-approval-table td:last-child {
  border-right: 0;
}

.sample-req-approval-table tbody tr:last-child td {
  border-bottom: 0;
}

.sample-req-evaluation-table {
  min-width: 900px;
}

.sample-req-trial-table {
  min-width: 1180px;
}

.sample-req-empty-cell {
  padding: 18px 0;
}

.sample-req-link-button {
  height: auto;
  padding: 0;
}

.sample-req-direct-archive {
  min-height: 72px;
  padding: 16px;
  color: #26364f;
  background: #fffbe6;
}

@media (max-width: 1100px) {
  .srm-sample-inline-detail__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .srm-sample-inline-detail__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
