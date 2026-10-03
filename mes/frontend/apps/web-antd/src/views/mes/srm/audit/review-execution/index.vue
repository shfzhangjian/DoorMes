<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSupplierReviewPlanApi } from '#/api/mes/srm/supplier-review-plan';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccess } from '@vben/access';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Spin,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createSupplierReviewReply,
  getSupplierReviewExecutionPage,
  getSupplierReviewMonthPlan,
} from '#/api/mes/srm/supplier-review-plan';
import SrmAttachmentPanel from '#/views/mes/srm/shared/SrmAttachmentPanel.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmAuditReviewExecution' });

const props = withDefaults(
  defineProps<{
    pageTitle?: string;
    readonly?: boolean;
  }>(),
  {
    pageTitle: '评审计划执行',
    readonly: false,
  },
);

type ExecutionItem = SrmSupplierReviewPlanApi.ExecutionItem;
type MonthPlan = SrmSupplierReviewPlanApi.MonthPlan;

const CURRENT_YEAR = new Date().getFullYear();
const MODAL_Z_INDEX = 5200;

const statusOptions = [
  { label: '计划', value: 'PLAN', color: 'blue' },
  { label: '执行', value: 'EXECUTING', color: 'orange' },
  { label: '变更', value: 'CHANGED', color: 'purple' },
  { label: '取消', value: 'CANCELED', color: 'red' },
  { label: '完成', value: 'COMPLETED', color: 'green' },
  { label: '归档', value: 'ARCHIVED', color: 'success' },
];

const attachmentCategoryOptions = [
  { label: '评审执行资料', value: 'REVIEW_EXECUTION_REPLY' },
  { label: '评审计划说明', value: 'REVIEW_PLAN_DESC' },
  { label: '月度调整附件', value: 'REVIEW_PLAN_CHANGE' },
  { label: '其他附件', value: 'OTHER' },
];

const VIEW_ALL_PERMISSION = 'mes:srm-audit-review-record:view-all';
const { hasAccessByCodes } = useAccess();
const canViewAll = computed(() => hasAccessByCodes([VIEW_ALL_PERMISSION]));
const scopeOptions = computed(() => [
  { label: '我相关的计划', value: 'MINE' },
  ...(canViewAll.value ? [{ label: '全部计划', value: 'ALL' }] : []),
]);

const submitting = ref(false);
const detailLoading = ref(false);
const detailModalOpen = ref(false);
const replyModalOpen = ref(false);
const activeItem = ref<ExecutionItem>();
const monthDetail = ref<MonthPlan>();
const userStore = useUserStore();

const replyForm = reactive({
  recorderUserName: '',
  remark: '',
  reviewDate: '',
  reviewResult: '',
});

const statusSelectOptions = computed(() => [
  { label: '全部', value: '' },
  ...statusOptions.map((item) => ({ label: item.label, value: item.value })),
]);

const currentOperatorName = computed(() => {
  const user = userStore.userInfo;
  return user?.nickname || user?.username || '当前用户';
});

const detailSubtitleItems = computed(() => {
  if (!activeItem.value) {
    return [];
  }
  return [
    monthTitle(activeItem.value),
    activeItem.value.supplierName,
    activeItem.value.materialCode,
  ]
    .filter(Boolean)
    .map(String);
});

const currentMonthStatusName = computed(() => {
  return (
    monthDetail.value?.executionStatusName ||
    activeItem.value?.executionStatusName ||
    statusMeta(
      monthDetail.value?.executionStatus || activeItem.value?.executionStatus,
    ).label
  );
});

const currentMonthStatusColor = computed(() => {
  return statusMeta(
    monthDetail.value?.executionStatus || activeItem.value?.executionStatus,
  ).color;
});

function statusMeta(status?: string) {
  return (
    statusOptions.find((item) => item.value === status) || {
      color: 'default',
      label: status || '-',
      value: status || '',
    }
  );
}

function monthTitle(record?: ExecutionItem) {
  if (!record) {
    return '-';
  }
  return `${record.planYear || '-'}年${record.planMonth || '-'}月`;
}

function todayText() {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function getPopupContainer(trigger?: HTMLElement) {
  return trigger?.parentElement || document.body;
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'RadioGroup',
        componentProps: {
          optionType: 'button',
          options: scopeOptions.value,
        },
        defaultValue: 'MINE',
        fieldName: 'scope',
        label: '查看范围',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入年度' },
        defaultValue: CURRENT_YEAR,
        fieldName: 'planYear',
        label: '年度',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: Array.from({ length: 12 }, (_, index) => ({
            label: `${index + 1}月`,
            value: index + 1,
          })),
          placeholder: '请选择月份',
        },
        fieldName: 'planMonth',
        label: '月份',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: statusSelectOptions.value,
          placeholder: '请选择状态',
        },
        fieldName: 'executionStatus',
        label: '执行状态',
      },
      {
        component: 'Input',
        componentProps: {
          allowClear: true,
          placeholder: '请输入供应商代码或名称',
        },
        fieldName: 'supplierKeyword',
        label: '供应商信息',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料代码' },
        fieldName: 'materialCode',
        label: '物料代码',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'planYear',
        minWidth: 90,
        title: '年度',
      },
      {
        field: 'planMonth',
        minWidth: 110,
        slots: { default: 'planMonth' },
        title: '月份',
      },
      { field: 'supplierCode', minWidth: 150, title: '供应商代码' },
      { field: 'supplierName', minWidth: 180, title: '供应商名称' },
      { field: 'materialCode', minWidth: 150, title: '物料代码' },
      { field: 'materialName', minWidth: 180, title: '物料名称' },
      { field: 'model', minWidth: 150, title: '型号' },
      { field: 'applicableProduct', minWidth: 180, title: '适用产品' },
      { field: 'leadUserName', minWidth: 130, title: '牵头执行人' },
      { field: 'relatedUserNames', minWidth: 220, title: '相关人员' },
      {
        align: 'center',
        field: 'executionStatus',
        minWidth: 110,
        slots: { default: 'executionStatus' },
        title: '执行状态',
      },
      { field: 'auditDate', minWidth: 120, title: '审核时间' },
      { field: 'updateTime', minWidth: 170, title: '更新时间' },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: props.readonly ? 110 : 180,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const values = formValues || {};
          const result = await getSupplierReviewExecutionPage({
            executionStatus: values.executionStatus || undefined,
            materialCode: values.materialCode || undefined,
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
            planMonth: values.planMonth,
            planYear: Number(values.planYear || CURRENT_YEAR),
            scope: values.scope || 'MINE',
            supplierName: values.supplierKeyword || undefined,
          });
          return {
            list: result?.list || [],
            total: result?.total || 0,
          };
        },
      },
    },
    rowConfig: { isHover: true, keyField: 'monthPlanId' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<ExecutionItem>,
});

async function loadMonthDetail(monthPlanId?: number) {
  if (!monthPlanId) {
    monthDetail.value = undefined;
    return;
  }
  detailLoading.value = true;
  try {
    monthDetail.value = await getSupplierReviewMonthPlan(monthPlanId);
  } finally {
    detailLoading.value = false;
  }
}

async function openDetail(record: ExecutionItem) {
  activeItem.value = record;
  monthDetail.value = undefined;
  detailModalOpen.value = true;
  await loadMonthDetail(record.monthPlanId);
}

async function openReply(record: ExecutionItem) {
  if (props.readonly) {
    await openDetail(record);
    return;
  }
  activeItem.value = record;
  replyForm.reviewDate = todayText();
  replyForm.recorderUserName = currentOperatorName.value;
  replyForm.reviewResult = '';
  replyForm.remark = '';
  replyModalOpen.value = true;
  await loadMonthDetail(record.monthPlanId);
}

async function handleCreateReply() {
  if (!activeItem.value?.monthPlanId) {
    message.warning('请选择评审月份计划');
    return;
  }
  if (!replyForm.reviewDate) {
    message.warning('请填写评审日期');
    return;
  }
  if (!replyForm.recorderUserName?.trim()) {
    message.warning('请填写经办人');
    return;
  }
  if (!replyForm.reviewResult?.trim()) {
    message.warning('请填写评审结果说明');
    return;
  }
  submitting.value = true;
  try {
    await createSupplierReviewReply({
      monthPlanId: activeItem.value.monthPlanId,
      recorderUserName: replyForm.recorderUserName,
      remark: replyForm.remark,
      reviewDate: replyForm.reviewDate,
      reviewResult: replyForm.reviewResult,
    });
    message.success('执行回复已上报');
    replyModalOpen.value = false;
    await gridApi.query();
  } finally {
    submitting.value = false;
  }
}

function closeDetailModal() {
  detailModalOpen.value = false;
  activeItem.value = undefined;
  monthDetail.value = undefined;
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <span class="srm-review-list-title">{{ props.pageTitle }}</span>
      </template>
      <template #planMonth="{ row }">
        <a class="srm-crud-link" @click="openDetail(row)">
          {{ monthTitle(row) }}
        </a>
      </template>
      <template #executionStatus="{ row }">
        <Tag :color="statusMeta(row.executionStatus).color">
          {{ row.executionStatusName || statusMeta(row.executionStatus).label }}
        </Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              icon: ACTION_ICON.PREVIEW,
              label: '查看',
              onClick: openDetail.bind(null, row),
              type: 'link',
            },
            ...(!props.readonly
              ? [
                  {
                    icon: ACTION_ICON.EDIT,
                    label: '上报回复',
                    onClick: openReply.bind(null, row),
                    type: 'link',
                  },
                ]
              : []),
          ]"
        />
      </template>
    </Grid>

    <Modal
      v-model:open="detailModalOpen"
      :closable="false"
      destroy-on-close
      :footer="null"
      :keyboard="false"
      :mask-closable="false"
      :title="null"
      width="100vw"
      wrap-class-name="srm-review-month-modal-wrap srm-erp-crud-modal srm-independent-detail-modal"
      :z-index="MODAL_Z_INDEX"
      @cancel="closeDetailModal"
    >
      <Spin :spinning="detailLoading" class="detail-spin">
        <div v-if="activeItem" class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetailModal">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">供方评审月份计划</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in detailSubtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button class="qms-ncr-toolbar-action" @click="closeDetailModal">
                <IconifyIcon icon="lucide:x" />
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>计划摘要</strong>
                    </div>
                    <Tag :color="currentMonthStatusColor">
                      {{ currentMonthStatusName }}
                    </Tag>
                  </div>
                  <div class="erp-form-grid srm-review-month-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">计划年度</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.planYear || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">计划月份</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{
                            activeItem.planMonth
                              ? `${activeItem.planMonth}月`
                              : '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商代码</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.supplierCode || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商名称</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料代码</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.materialCode || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">当前状态</label>
                      <div class="erp-form-value">
                        <Tag :color="currentMonthStatusColor">
                          {{ currentMonthStatusName }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">适用产品</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.applicableProduct || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料名称</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.materialName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">型号</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ activeItem.model || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>月份计划维护</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid srm-review-month-edit-grid">
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">牵头执行人</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{
                            monthDetail?.leadUserName ||
                            activeItem.leadUserName ||
                            '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">相关人员</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{
                            monthDetail?.relatedUserNames ||
                            activeItem.relatedUserNames ||
                            '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div
                      class="erp-form-item srm-review-month-form-item--span-2"
                    >
                      <label class="erp-form-label">审批意见</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ monthDetail?.approvalOpinion || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">审核时间</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{
                            monthDetail?.auditDate ||
                            activeItem.auditDate ||
                            '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">审核人</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ monthDetail?.approverUserName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        月份计划说明
                      </label>
                      <div class="erp-form-value">
                        <span
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{
                            monthDetail?.planDesc || activeItem.planDesc || '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注说明
                      </label>
                      <div class="erp-form-value">
                        <span
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ monthDetail?.remark || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <SrmAttachmentPanel
                  biz-type="SRM_SUPPLIER_REVIEW_PLAN"
                  :biz-id="activeItem.monthPlanId"
                  :category-options="attachmentCategoryOptions"
                  mode="detail"
                  :modal-z-index="MODAL_Z_INDEX + 100"
                />

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>执行回复列表</strong>
                    </div>
                  </div>
                  <div class="srm-review-approval-table-wrap">
                    <table class="srm-review-approval-table">
                      <thead>
                        <tr>
                          <th>记录时间</th>
                          <th>评审日期</th>
                          <th>经办人</th>
                          <th>评审结果说明</th>
                        </tr>
                      </thead>
                      <tbody v-if="monthDetail?.replies?.length">
                        <tr
                          v-for="item in monthDetail.replies"
                          :key="item.id || item.replyTime || item.reviewDate"
                        >
                          <td>{{ item.replyTime || '-' }}</td>
                          <td>{{ item.reviewDate || '-' }}</td>
                          <td>{{ item.recorderUserName || '-' }}</td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.reviewResult || '-' }}
                          </td>
                        </tr>
                      </tbody>
                      <tbody v-else>
                        <tr>
                          <td class="srm-review-empty-cell" colspan="4">
                            暂无执行回复
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>

                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>状态调整日志</strong>
                    </div>
                  </div>
                  <div class="srm-review-approval-table-wrap">
                    <table
                      class="srm-review-approval-table srm-review-status-log-table"
                    >
                      <thead>
                        <tr>
                          <th>时间</th>
                          <th>操作人</th>
                          <th>调整前</th>
                          <th>调整后</th>
                          <th>状态备注</th>
                          <th>更新说明</th>
                        </tr>
                      </thead>
                      <tbody v-if="monthDetail?.statusLogs?.length">
                        <tr
                          v-for="item in monthDetail.statusLogs"
                          :key="item.id || item.createTime || item.toStatusName"
                        >
                          <td>{{ item.createTime || '-' }}</td>
                          <td>{{ item.operatorUserName || '-' }}</td>
                          <td>{{ item.fromStatusName || '-' }}</td>
                          <td>{{ item.toStatusName || '-' }}</td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.reason || '-' }}
                          </td>
                          <td class="srm-review-approval-table__wrap-cell">
                            {{ item.updateDescription || '-' }}
                          </td>
                        </tr>
                      </tbody>
                      <tbody v-else>
                        <tr>
                          <td class="srm-review-empty-cell" colspan="6">
                            暂无状态调整日志
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </Modal>

    <Modal
      v-model:open="replyModalOpen"
      centered
      destroy-on-close
      :body-style="{ maxHeight: '68vh', overflowY: 'auto' }"
      title="上报执行回复"
      :mask-closable="false"
      :width="960"
      :z-index="MODAL_Z_INDEX"
      @ok="handleCreateReply"
    >
      <div class="srm-review-reply-form" v-if="activeItem">
        <div
          class="srm-review-reply-summary-grid srm-review-reply-summary-grid--four"
        >
          <div class="srm-review-reply-summary-grid__label">计划年度</div>
          <div>{{ activeItem.planYear || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">计划月份</div>
          <div>
            {{ activeItem.planMonth ? `${activeItem.planMonth}月` : '-' }}
          </div>
          <div class="srm-review-reply-summary-grid__label">供应商代码</div>
          <div>{{ activeItem.supplierCode || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">供应商名称</div>
          <div>{{ activeItem.supplierName || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">物料代码</div>
          <div>{{ activeItem.materialCode || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">物料名称</div>
          <div>{{ activeItem.materialName || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">适用产品</div>
          <div>{{ activeItem.applicableProduct || '-' }}</div>
          <div class="srm-review-reply-summary-grid__label">型号</div>
          <div>{{ activeItem.model || '-' }}</div>
        </div>
        <div class="srm-review-reply-inline-grid">
          <label>评审日期</label>
          <DatePicker
            v-model:value="replyForm.reviewDate"
            class="w-full"
            :get-popup-container="getPopupContainer"
            value-format="YYYY-MM-DD"
          />
          <label>经办人</label>
          <Input
            v-model:value="replyForm.recorderUserName"
            allow-clear
            placeholder="请输入经办人"
          />
        </div>
        <label>评审结果说明</label>
        <Input.TextArea
          v-model:value="replyForm.reviewResult"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          placeholder="请输入评审结果说明"
        />
        <SrmAttachmentPanel
          class="srm-review-reply-attachment"
          :biz-id="activeItem.monthPlanId"
          biz-type="SRM_SUPPLIER_REVIEW_REPLY"
          :category-options="attachmentCategoryOptions"
          default-category="REVIEW_EXECUTION_REPLY"
          :modal-z-index="MODAL_Z_INDEX + 100"
          mode="edit"
        />
      </div>
      <template #footer>
        <Button @click="replyModalOpen = false">取消</Button>
        <Button :loading="submitting" type="primary" @click="handleCreateReply">
          确认上报
        </Button>
      </template>
    </Modal>
  </Page>
</template>

<style scoped>
.srm-review-list-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.srm-crud-link {
  color: #1677ff;
  cursor: pointer;
}

.srm-crud-link:hover {
  color: #0958d9;
}

.srm-review-month-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.srm-review-month-edit-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.srm-review-month-form-item--span-2 {
  grid-column: span 2;
}

.srm-review-approval-table-wrap {
  overflow-x: auto;
  border: 1px solid #dbe3ee;
  border-top: 0;
  background: #fff;
}

.srm-review-approval-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.srm-review-status-log-table {
  min-width: 1080px;
}

.srm-review-approval-table th,
.srm-review-approval-table td {
  padding: 11px 14px;
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  color: #24364f;
  line-height: 20px;
  text-align: left;
  vertical-align: middle;
}

.srm-review-approval-table th {
  color: #162338;
  font-weight: 600;
  white-space: nowrap;
  background: #f7f9fc;
}

.srm-review-approval-table th:last-child,
.srm-review-approval-table td:last-child {
  border-right: 0;
}

.srm-review-approval-table tbody tr:last-child td {
  border-bottom: 0;
}

.srm-review-approval-table__wrap-cell {
  white-space: normal;
  word-break: break-word;
}

.srm-review-empty-cell {
  padding: 18px 0;
  color: #7a8799;
  text-align: center;
}

.srm-review-reply-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.srm-review-reply-form label {
  color: #1f3149;
  font-weight: 700;
}

.srm-review-reply-inline-grid {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  align-items: center;
  gap: 10px 12px;
}

.srm-review-reply-inline-grid label {
  text-align: right;
}

.srm-review-reply-inline-grid :deep(.ant-input),
.srm-review-reply-inline-grid :deep(.ant-picker) {
  width: 100%;
}

.srm-review-reply-summary-grid {
  display: grid;
  overflow: hidden;
  grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  border: 1px solid #d9e2ef;
  border-bottom: 0;
  color: #1f3149;
}

.srm-review-reply-summary-grid--four {
  grid-template-columns: repeat(4, 88px minmax(0, 1fr));
}

.srm-review-reply-summary-grid > div {
  min-height: 40px;
  padding: 9px 12px;
  border-right: 1px solid #d9e2ef;
  border-bottom: 1px solid #d9e2ef;
}

.srm-review-reply-summary-grid__label {
  color: #33465f;
  font-weight: 700;
  text-align: center;
  background: #edf3f9;
}

.srm-review-reply-attachment {
  overflow: hidden;
  border: 1px solid #d7e1ee;
  background: #fff;
}

.srm-review-reply-attachment :deep(.detail-list-head) {
  min-height: 46px;
  padding: 10px 14px;
  background: #f8fbff;
  border-bottom: 1px solid #d7e1ee;
}

.srm-review-reply-attachment :deep(.srm-attachment-panel__current-list) {
  min-height: 150px;
  padding: 12px;
}

.srm-review-reply-attachment :deep(.ant-empty) {
  margin: 20px 0;
}

@media (max-width: 1100px) {
  .srm-review-month-edit-grid,
  .srm-review-month-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .srm-review-month-form-item--span-2 {
    grid-column: 1 / -1;
  }

  .srm-review-reply-summary-grid--four {
    grid-template-columns: repeat(2, 88px minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .srm-review-month-edit-grid,
  .srm-review-month-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .srm-review-reply-summary-grid,
  .srm-review-reply-summary-grid--four {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .srm-review-reply-inline-grid {
    grid-template-columns: 88px minmax(0, 1fr);
  }
}
</style>
