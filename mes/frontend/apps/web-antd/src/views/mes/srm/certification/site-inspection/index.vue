<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSupplierReviewPlanApi } from '#/api/mes/srm/supplier-review-plan';

import { computed, reactive, ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Spin,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getAttachmentList } from '#/api/mes/srm/attachment';
import {
  getSupplierReviewExecutionPage,
  getSupplierReviewMonthPlan,
  saveSupplierReviewSiteInspection,
} from '#/api/mes/srm/supplier-review-plan';
import SrmAttachmentPanel from '#/views/mes/srm/shared/SrmAttachmentPanel.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmCertificationSiteInspection' });

type ExecutionItem = SrmSupplierReviewPlanApi.ExecutionItem;

const CURRENT_YEAR = new Date().getFullYear();
const MODAL_Z_INDEX = 5200;

const auditCategoryOptions = [
  { label: '认证审核', value: 'CERTIFICATION_AUDIT', color: 'blue' },
  { label: '年度审核', value: 'ANNUAL_AUDIT', color: 'orange' },
  { label: '不定期审核', value: 'IRREGULAR_AUDIT', color: 'purple' },
];

const auditCategoryMap = Object.fromEntries(
  auditCategoryOptions.map((item) => [item.value, item.label]),
);

const auditCategoryColorMap = Object.fromEntries(
  auditCategoryOptions.map((item) => [item.value, item.color]),
);

function auditCategoryName(value?: string) {
  if (!value) {
    return '-';
  }
  return auditCategoryMap[value] || value;
}

function auditCategoryColor(value?: string) {
  if (!value) {
    return 'default';
  }
  return auditCategoryColorMap[value] || 'default';
}

const VIEW_ALL_PERMISSION = 'mes:srm-audit-review-record:view-all';
const { hasAccessByCodes } = useAccess();
const canViewAll = computed(() => hasAccessByCodes([VIEW_ALL_PERMISSION]));
const scopeOptions = computed(() => [
  { label: '我相关的计划', value: 'MINE' },
  ...(canViewAll.value ? [{ label: '全部计划', value: 'ALL' }] : []),
]);

const statusSelectOptions = [
  { label: '执行', value: 'EXECUTING' },
  { label: '归档', value: 'ARCHIVED' },
  { label: '计划', value: 'PLAN' },
  { label: '变更', value: 'CHANGED' },
  { label: '取消', value: 'CANCELED' },
  { label: '完成', value: 'COMPLETED' },
];

const attachmentCategoryOptions = [
  { label: '现场考察资料', value: 'SITE_INSPECTION_MATERIAL' },
  { label: '其他附件', value: 'OTHER' },
];

const submitting = ref(false);
const formLoading = ref(false);
const formOpen = ref(false);
const activeItem = ref<ExecutionItem>();
const monthDetail = ref<SrmSupplierReviewPlanApi.MonthPlan>();
const attachmentRef = ref<InstanceType<typeof SrmAttachmentPanel>>();

const siteForm = reactive({
  auditCategory: undefined as string | undefined,
  auditDate: '',
  auditDesc: '',
});

function monthTitle(record?: ExecutionItem) {
  if (!record) {
    return '-';
  }
  return `${record.planYear || '-'}年${record.planMonth || '-'}月`;
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
          mode: 'multiple',
          options: statusSelectOptions,
          placeholder: '默认显示执行+归档',
        },
        defaultValue: ['EXECUTING', 'ARCHIVED'],
        fieldName: 'executionStatuses',
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
      { field: 'useDepartment', minWidth: 130, title: '使用部门' },
      { field: 'supplierCode', minWidth: 150, title: '供应商代码' },
      { field: 'supplierName', minWidth: 180, title: '供应商名称' },
      { field: 'materialCode', minWidth: 150, title: '物料代码' },
      { field: 'materialName', minWidth: 180, title: '物料名称' },
      { field: 'model', minWidth: 150, title: '型号' },
      {
        align: 'center',
        field: 'auditCategory',
        minWidth: 110,
        slots: { default: 'auditCategory' },
        title: '审核类别',
      },
      { field: 'auditDate', minWidth: 120, title: '审定日期' },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 180,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const values = formValues || {};
          let statuses: string[];
          if (Array.isArray(values.executionStatuses)) {
            statuses = values.executionStatuses.filter(Boolean);
          } else if (values.executionStatuses) {
            statuses = [values.executionStatuses];
          } else {
            statuses = ['EXECUTING', 'ARCHIVED'];
          }
          const result = await getSupplierReviewExecutionPage({
            executionStatus: statuses.join(','),
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

async function openSiteInspectionModal(record: ExecutionItem) {
  activeItem.value = record;
  siteForm.auditCategory = record.auditCategory;
  siteForm.auditDate = record.auditDate || '';
  siteForm.auditDesc = record.auditDesc || '';
  formOpen.value = true;
  formLoading.value = true;
  try {
    monthDetail.value = record.monthPlanId
      ? await getSupplierReviewMonthPlan(record.monthPlanId)
      : undefined;
  } finally {
    formLoading.value = false;
  }
  if (attachmentRef.value?.loadAttachments) {
    await attachmentRef.value.loadAttachments();
  }
}

async function handleSaveSiteInspection() {
  if (!activeItem.value?.monthPlanId) {
    message.warning('请选择计划');
    return;
  }
  submitting.value = true;
  try {
    await saveSupplierReviewSiteInspection({
      id: activeItem.value.monthPlanId,
      auditCategory: siteForm.auditCategory,
      auditDate: siteForm.auditDate || undefined,
      auditDesc: siteForm.auditDesc,
    });
    if (attachmentRef.value?.syncAttachments) {
      await attachmentRef.value.syncAttachments({
        bizId: activeItem.value.monthPlanId,
        bizType: 'SRM_SITE_INSPECTION',
      });
    }
    // 读取附件列表快照写回 auditAttachment（文件名拼接，便于列表/详情展示）
    const files = await getAttachmentList({
      bizId: activeItem.value.monthPlanId,
      bizType: 'SRM_SITE_INSPECTION',
      includeHistory: false,
    });
    const snapshot = (files || [])
      .map((item) => item.fileName)
      .filter(Boolean)
      .join('、');
    if (snapshot) {
      await saveSupplierReviewSiteInspection({
        id: activeItem.value.monthPlanId,
        auditAttachment: snapshot.slice(0, 1900),
      });
    }
    message.success('保存成功');
    formOpen.value = false;
    activeItem.value = undefined;
    monthDetail.value = undefined;
    await gridApi.query();
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <span class="srm-review-list-title">现场考察</span>
      </template>
      <template #planMonth="{ row }">
        <span class="srm-crud-link">{{ monthTitle(row) }}</span>
      </template>
      <template #auditCategory="{ row }">
        <Tag :color="auditCategoryColor(row.auditCategory)">
          {{ auditCategoryName(row.auditCategory) }}
        </Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              icon: ACTION_ICON.UPLOAD,
              label: '上传现场考察资料',
              onClick: openSiteInspectionModal.bind(null, row),
              type: 'link',
            },
          ]"
        />
      </template>
    </Grid>

    <Modal
      v-model:open="formOpen"
      :keyboard="false"
      :mask-closable="false"
      :z-index="MODAL_Z_INDEX"
      :footer="null"
      :title="null"
      width="100vw"
      wrap-class-name="srm-site-inspection-modal-wrap srm-erp-crud-modal srm-independent-detail-modal"
    >
      <Spin :spinning="formLoading" class="detail-spin">
        <div v-if="activeItem" class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="formOpen = false">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">现场考察记录</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{ monthTitle(activeItem) }}
                </span>
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{ activeItem.supplierName || '-' }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                class="qms-ncr-toolbar-action"
                type="primary"
                :loading="submitting"
                @click="handleSaveSiteInspection"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button class="qms-ncr-toolbar-action" @click="formOpen = false">
                <IconifyIcon icon="lucide:x" />
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <!-- 只读摘要 -->
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>计划信息（只读）</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid srm-site-inspection-readonly-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">计划年月</label>
                      <div class="erp-form-value">
                        {{ monthTitle(activeItem) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">使用部门</label>
                      <div class="erp-form-value">
                        {{ activeItem.useDepartment || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商代码</label>
                      <div class="erp-form-value">
                        {{ activeItem.supplierCode || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商名称</label>
                      <div class="erp-form-value">
                        {{ activeItem.supplierName || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料代码</label>
                      <div class="erp-form-value">
                        {{ activeItem.materialCode || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料名称</label>
                      <div class="erp-form-value">
                        {{ activeItem.materialName || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料型号</label>
                      <div class="erp-form-value">
                        {{ activeItem.model || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">执行状态</label>
                      <div class="erp-form-value">
                        {{ activeItem.executionStatusName || '-' }}
                      </div>
                    </div>
                  </div>
                </section>

                <!-- 可编辑区 -->
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>现场考察信息</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid srm-site-inspection-edit-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">审核类别</label>
                      <div class="erp-form-value">
                        <Select
                          v-model:value="siteForm.auditCategory"
                          :allow-clear="true"
                          :get-popup-container="getPopupContainer"
                          :options="auditCategoryOptions"
                          placeholder="请选择审核类别"
                          style="width: 100%"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">审定日期</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-model:value="siteForm.auditDate"
                          :get-popup-container="getPopupContainer"
                          format="YYYY-MM-DD"
                          placeholder="请选择审定日期"
                          style="width: 100%"
                          value-format="YYYY-MM-DD"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        审核说明
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-model:value="siteForm.auditDesc"
                          :rows="3"
                          placeholder="请输入审核说明"
                        />
                      </div>
                    </div>
                  </div>
                </section>

                <SrmAttachmentPanel
                  ref="attachmentRef"
                  biz-type="SRM_SITE_INSPECTION"
                  :biz-id="activeItem.monthPlanId"
                  :category-options="attachmentCategoryOptions"
                  default-category="SITE_INSPECTION_MATERIAL"
                  mode="edit"
                  :modal-z-index="MODAL_Z_INDEX + 200"
                />
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </Modal>
  </Page>
</template>

<style scoped>
.srm-review-list-title {
  display: inline-block;
  min-width: 200px;
  font-size: 15px;
  font-weight: 600;
}

.srm-crud-link {
  color: #1890ff;
  cursor: pointer;
}

/* 只读摘要：4 列表格风格（fieldset 边框紧贴，等价 cellspacing=0） */
.srm-site-inspection-readonly-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

/* 可编辑区：2 列；审核类别+审定日期一行，审核说明 --full 独立一行 */
.srm-site-inspection-edit-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

/* 审核说明文本域在表格单元格内铺满 */
.srm-site-inspection-edit-grid .erp-form-value .ant-input {
  resize: vertical;
}

.srm-site-inspection-modal-wrap .erp-form-value {
  overflow: visible;
}

@media (max-width: 1200px) {
  .srm-site-inspection-readonly-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .srm-site-inspection-readonly-grid,
  .srm-site-inspection-edit-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
