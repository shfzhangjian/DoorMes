<script lang="ts" setup>
import type { SrmOnboardingApplyApi } from '#/api/mes/srm/onboarding-apply';

import { computed, ref, watch } from 'vue';

import { Empty, Spin, Tag } from 'ant-design-vue';

import { getOnboardingApply } from '#/api/mes/srm/onboarding-apply';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmOnboardingApplyInlineDetail' });

const props = defineProps<{ id?: number | string }>();

type ApprovalRow = {
  handler: string;
  node: string;
  opinion: string;
  result: string;
  time: string;
};

const BIZ_TYPE = 'ONBOARDING_APPLY';

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVED: { color: 'success', text: '已归档' },
  DEPT_SIGN: { color: 'processing', text: '部门会签' },
  DRAFT: { color: 'default', text: '草稿' },
  ENTRY_PROCESSING: { color: 'processing', text: '交办办理' },
  GENERAL_MANAGER_REVIEW: { color: 'warning', text: '总经理审核' },
  PURCHASE_INTAKE: { color: 'processing', text: '采购部门办理' },
  PURCHASE_REVIEW: { color: 'processing', text: '采购部审批' },
  PURCHASE_TRANSFER: { color: 'warning', text: '采购部转办' },
  QUALITY_REVIEW: { color: 'processing', text: '品质部审批' },
  REJECTED: { color: 'error', text: '不通过' },
  TECH_REVIEW: { color: 'processing', text: '技术研发部审批' },
  USE_DEPT_REVIEW: { color: 'processing', text: '使用部门负责人审核' },
};
const importTypeTextMap: Record<string, string> = {
  CUSTOMER_SPECIFIED: '客户指定物料',
  NEW: '全新物料',
  REPLACE: '替代现有物料',
};
const reviewResultTextMap: Record<string, string> = {
  PASS: '同意',
  REJECT: '不同意',
};
const reviewResultColorMap: Record<string, string> = {
  PASS: 'success',
  REJECT: 'error',
};

const loading = ref(false);
const record = ref<SrmOnboardingApplyApi.OnboardingApply>();

const statusMeta = computed(
  () =>
    statusMetaMap[record.value?.status || ''] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);

const approvalRows = computed<ApprovalRow[]>(() => {
  const current = record.value;
  if (!current) return [];
  const rows: ApprovalRow[] = [
    {
      handler: displayValue(
        current.purchaseHandlerUserName || current.purchaseReviewerUserName,
      ),
      node: '采购部门办理',
      opinion: displayValue(current.purchaseIntakeOpinion),
      result: current.purchaseIntakeTime ? '已办理' : '-',
      time: displayValue(current.purchaseIntakeTime),
    },
    ...(current.signs || []).map((item) => ({
      handler: displayValue(item.userName),
      node: displayValue(item.deptName),
      opinion: displayValue(item.signOpinion),
      result: item.signResult
        ? displayReviewResult(item.signResult)
        : item.signStatus === 'PENDING'
          ? '待会签'
          : '-',
      time: displayValue(item.signTime),
    })),
    {
      handler: displayValue(current.generalManagerUserName),
      node: '总经理审核',
      opinion: displayValue(current.generalManagerOpinion),
      result: displayReviewResult(current.generalManagerResult),
      time: displayValue(current.generalManagerHandleTime),
    },
    {
      handler: displayValue(current.materialEntryUserName),
      node: '物料编码录入',
      opinion: displayValue(current.materialEntryOpinion),
      result: current.materialEntryCode ? '已完成' : '-',
      time: displayValue(current.materialEntryHandleTime),
    },
    {
      handler: displayValue(current.supplierRosterEntryUserName),
      node: '供方清单录入',
      opinion: displayValue(current.supplierRosterEntryOpinion),
      result: current.supplierRosterEntryCode ? '已完成' : '-',
      time: displayValue(current.supplierRosterEntryHandleTime),
    },
  ];
  return rows.filter(
    (row) =>
      row.handler !== '-' ||
      row.opinion !== '-' ||
      row.result !== '-' ||
      row.time !== '-',
  );
});

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
    record.value = await getOnboardingApply(id);
  } finally {
    loading.value = false;
  }
}

function importTypeText(value?: string) {
  return importTypeTextMap[value || ''] || displayValue(value);
}

function displayReviewResult(value?: string) {
  return reviewResultTextMap[value || ''] || displayValue(value);
}

function reviewResultColor(value?: string) {
  if (value === '已办理' || value === '已完成') return 'success';
  if (value === '待会签') return 'default';
  return reviewResultColorMap[value || ''] || 'default';
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function yesNo(value?: boolean) {
  if (value === true) return '是';
  if (value === false) return '否';
  return '-';
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}
</script>

<template>
  <Spin :spinning="loading">
    <div v-if="record" class="srm-import-inline-detail srm-erp-crud-modal">
      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>基本信息</strong>
          </div>
          <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
        </div>
        <div class="erp-form-grid">
          <div class="erp-form-item">
            <label class="erp-form-label">申请人</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.applicantName) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">申请时间</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.applyTime) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">使用部门</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.applyDept) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">供应商名称</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.supplierName) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料名称</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.materialName) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item">
            <label class="erp-form-label">物料型号</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.materialModel) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">适用产品</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ displayValue(record.applicableProduct) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">导入类型</label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value">
                {{ importTypeText(record.importType) }}
                <template v-if="record.importType === 'REPLACE'">
                  （旧物料编码：{{ displayValue(record.replacedMaterialCode) }}；
                  旧物料名称：{{ displayValue(record.replacedMaterialName) }}）
                </template>
                <template v-if="record.importType === 'CUSTOMER_SPECIFIED'">
                  （物料编码：{{ displayValue(record.customerMaterialCode) }}；
                  物料名称：{{ displayValue(record.customerMaterialName) }}）
                </template>
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">
              供应商优势说明
            </label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value--multiline">
                {{ displayValue(record.supplierAdvantageDesc) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label erp-form-label--tall">
              其他补充说明
            </label>
            <div class="erp-form-value">
              <span class="qms-exception-readonly-value--multiline">
                {{ displayValue(record.supplementDesc) }}
              </span>
            </div>
          </div>
          <div class="erp-form-item erp-form-item--full">
            <label class="erp-form-label">后续闭环要求</label>
            <div class="erp-form-value srm-import-inline-detail__closure">
              <span>
                物料编码系统创建完成：{{ yesNo(record.materialCodeCreated) }}
                <template v-if="record.materialEntryCode">
                  ，物料编码：{{ record.materialEntryCode }}
                </template>
                <template v-if="record.materialCodeCompleteDate">
                  ，完成日期：{{ record.materialCodeCompleteDate }}
                </template>
              </span>
              <span>
                供应商录入合格供方清单：{{ yesNo(record.supplierRosterCreated) }}
                <template v-if="record.supplierRosterEntryCode">
                  ，供方编码：{{ record.supplierRosterEntryCode }}
                </template>
                <template v-if="record.supplierRosterCompleteDate">
                  ，完成日期：{{ record.supplierRosterCompleteDate }}
                </template>
              </span>
            </div>
          </div>
        </div>
      </section>

      <section class="erp-basic-form">
        <div class="detail-list-head">
          <div class="detail-list-title">
            <strong>会签与办理记录</strong>
          </div>
        </div>
        <div class="srm-import-approval-table-wrap">
          <table class="srm-import-approval-table">
            <thead>
              <tr>
                <th>部门</th>
                <th>审批结果</th>
                <th>审批意见</th>
                <th>批准签字</th>
                <th>日期</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in approvalRows" :key="item.node">
                <td>{{ item.node }}</td>
                <td>
                  <Tag
                    v-if="item.result !== '-'"
                    :color="
                      reviewResultColor(
                        item.result === '同意'
                          ? 'PASS'
                          : item.result === '不同意'
                            ? 'REJECT'
                            : item.result,
                      )
                    "
                    class="!m-0"
                  >
                    {{ item.result }}
                  </Tag>
                  <span v-else>-</span>
                </td>
                <td>{{ item.opinion }}</td>
                <td>{{ item.handler }}</td>
                <td>{{ item.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <SrmAttachmentPanel
        :biz-id="record.id"
        :biz-type="BIZ_TYPE"
        category-dict-type="mes_srm_onboarding_apply_attachment_category"
        mode="detail"
      />
    </div>
    <Empty v-else description="暂无导入申请详情" />
  </Spin>
</template>

<style scoped>
.srm-import-inline-detail {
  display: grid;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
}

.srm-import-inline-detail__closure {
  display: grid;
  gap: 6px;
  align-items: start;
}

.srm-import-approval-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.srm-import-approval-table {
  width: 100%;
  min-width: 900px;
  border-spacing: 0;
  border-collapse: separate;
  table-layout: fixed;
}

.srm-import-approval-table th,
.srm-import-approval-table td {
  padding: 10px 12px;
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  text-align: left;
  vertical-align: top;
}

.srm-import-approval-table th:nth-child(1),
.srm-import-approval-table td:nth-child(1) {
  width: 16%;
}

.srm-import-approval-table th:nth-child(2),
.srm-import-approval-table td:nth-child(2) {
  width: 12%;
}

.srm-import-approval-table th:nth-child(3),
.srm-import-approval-table td:nth-child(3) {
  width: 38%;
  word-break: break-word;
}

.srm-import-approval-table th:nth-child(4),
.srm-import-approval-table td:nth-child(4) {
  width: 16%;
}

.srm-import-approval-table th:nth-child(5),
.srm-import-approval-table td:nth-child(5) {
  width: 18%;
}

.srm-import-approval-table th {
  font-weight: 700;
  color: #26364f;
  background: #eef4fa;
}

.srm-import-approval-table th:last-child,
.srm-import-approval-table td:last-child {
  border-right: 0;
}

.srm-import-approval-table tbody tr:last-child td {
  border-bottom: 0;
}
</style>
