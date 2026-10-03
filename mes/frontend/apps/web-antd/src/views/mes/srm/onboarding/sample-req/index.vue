<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSampleEvaluationApi } from '#/api/mes/srm/sample-evaluation';
import type { SrmSampleRequestApi } from '#/api/mes/srm/sample-req';
import type { SystemUserApi } from '#/api/system/user';
import type {
  BusinessLogItem,
  BusinessLogMode,
} from '#/components/business-log';
import type { PickerOption } from '#/components/picker';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Modal as AntModal,
  AutoComplete,
  Button,
  DatePicker,
  Dropdown,
  Input,
  InputNumber,
  Menu,
  message,
  Radio,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSampleEvaluationHistoryPage } from '#/api/mes/srm/sample-evaluation';
import {
  archiveConfirmSampleRequest,
  createSampleRequest,
  deleteSampleRequest,
  finalApproveSampleRequest,
  getSampleRequest,
  getSampleRequestPage,
  initiatorDecisionSampleRequest,
  projectReviewSampleRequest,
  purchaseReviewSampleRequest,
  submitSampleRequest,
  updateSampleRequest,
} from '#/api/mes/srm/sample-req';
import { getUserProfile } from '#/api/system/user/profile';
import { BusinessLogDrawer } from '#/components/business-log';
import {
  materialPickerConfig,
  PickerModal,
  productBomPickerConfig,
} from '#/components/picker';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmSampleEvaluationDetailModal from '../../certification/sample-evaluation/modules/detail-modal.vue';
import TrialValidationDetailModal from '../../certification/trial-validation/modules/detail-modal.vue';
import SrmAttachmentPanel from '../../shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmCertificationSampleDemand' });

type DetailMode = 'create' | 'detail' | 'edit';
type CompactBasicRow = { full?: boolean; label: string; value: string };
type ApprovalRow = {
  handler: string;
  node: string;
  opinion: string;
  time: string;
};
type CreateEntryMode = 'archive' | 'workflow';
type UserPickTarget = 'final' | 'project' | 'purchase' | 'workflowPurchase';
type WorkflowAction =
  | 'archive'
  | 'final'
  | 'initiator'
  | 'project'
  | 'purchase';

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const SRM_USER_SELECT_MODAL_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 200;
const BIZ_TYPE = 'SRM_SAMPLE_REQUEST';
const DIRECT_ARCHIVE_FORM_TEXT = '直接归档表单，具体内容见附件';

const applyTypeOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '紧急', value: 'URGENT' },
];
const specifiedSupplierOptions = [
  { label: '有', value: 'HAS' },
  { label: '无指定厂家', value: 'NONE' },
  { label: '其它', value: 'OTHER' },
];
const purchaseDifficultyVisibleStatuses = new Set([
  'ARCHIVE_CONFIRM',
  'ARCHIVED',
  'FINAL_APPROVAL',
  'INITIATOR_CONFIRM',
  'PURCHASE_REVIEW',
]);
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
const flowActionTextMap: Record<string, string> = {
  ARCHIVE_CONFIRM: '归档确认',
  ARCHIVE: '归档',
  CREATE: '创建',
  FINAL_APPROVE: '最终批准',
  INITIATOR_ARCHIVE: '直接归档',
  INITIATOR_SEND_APPROVAL: '送最终批准',
  PROJECT_REVIEW: '项目负责人审核',
  PURCHASE_REVIEW: '采购负责人审核',
  SUBMIT: '提交',
  UPDATE: '保存',
};
const attachmentCategoryButtons = [
  { label: '产品技术要求说明', value: 'TECH_REQUIREMENT' },
  { label: '样品需求单线下表单', value: 'SAMPLE_REQUEST_OFFLINE_FORM' },
];

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const currentUserProfile = ref<Record<string, any>>();

const detailMode = ref<DetailMode>('detail');
const detailLoading = ref(false);
const saving = ref(false);
const basicExpanded = ref(true);
const createChoiceModalOpen = ref(false);
const activeUserPickTarget = ref<UserPickTarget>('project');
const materialPickerOpen = ref(false);
const productBomPickerOpen = ref(false);
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('project');
const workflowOpinion = ref('');
const workflowDirectArchive = ref(true);
const workflowPurchaseDifficulty = ref('');
const workflowPurchaseReviewerUserId = ref<number>();
const workflowPurchaseReviewerUserName = ref('');
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const sampleEvaluationLoading = ref(false);
const sampleEvaluationRows = ref<SrmSampleEvaluationApi.SampleEvaluation[]>([]);

const form = reactive<SrmSampleRequestApi.SampleRequest>(createEmptyForm());

const applyDepartmentOptions = computed(() =>
  getDictOptions('mes_srm_apply_department').map((option) => ({
    label: String(option.label ?? option.value),
    value: String(option.label ?? option.value),
  })),
);
const isReadonly = computed(() => detailMode.value === 'detail');
const isCreateMode = computed(() => detailMode.value === 'create');
const canEditForm = computed(() => !isReadonly.value);
const shouldShowPurchaseDifficulty = computed(
  () =>
    Boolean(form.processInstanceId) &&
    purchaseDifficultyVisibleStatuses.has(String(form.status || '')),
);
const subtitleItems = computed(() => [form.requestNo || ''].filter(Boolean));
const businessLogs = computed<BusinessLogItem[]>(() =>
  (form.logs || []).map((log) => ({
    actionCode: log.action,
    actionName: flowActionText(log.action),
    fromNodeName: statusText(log.fromStatus),
    fromStatus: log.fromStatus,
    handleTime: log.createTime,
    handlerUserName: log.operatorName,
    id: log.id,
    opinion: log.actionDescription,
    toNodeName: statusText(log.toStatus),
    toStatus: log.toStatus,
  })),
);
const trialValidationRows = computed(() => form.trialValidations || []);
const shouldShowSupplierInput = computed(
  () =>
    form.specifiedSupplierType === 'HAS' ||
    form.specifiedSupplierType === 'OTHER',
);
const supplierValueText = computed(() =>
  [form.supplierName, form.supplierCode]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' / '),
);
const supplierSummaryText = computed(() => {
  const typeText = supplierTypeText(form.specifiedSupplierType);
  if (shouldShowSupplierInput.value && supplierValueText.value) {
    return `${typeText}：${supplierValueText.value}`;
  }
  return typeText;
});
const compactBasicRows = computed<CompactBasicRow[]>(() => [
  { label: '物料名称', value: displayValue(form.materialName) },
  { label: '型号', value: displayValue(form.materialModel) },
  { label: '申请类型', value: applyTypeText(form.applyType) },
  { label: '申请部门', value: displayValue(form.applyDept) },
  { label: '使用产品', value: displayValue(form.usedProduct) },
  { label: '需求数量', value: displayValue(form.requireQty) },
  { label: '申请人', value: displayValue(form.applicantName) },
  { label: '申请日期', value: displayValue(form.applyDate) },
  { label: '需求日期', value: displayValue(form.requireDate) },
  { full: true, label: '有无指定供应商', value: supplierSummaryText.value },
  { full: true, label: 'OA审批链接', value: displayValue(form.oaApprovalUrl) },
  { label: '项目负责人', value: displayValue(form.projectLeaderUserName) },
  { label: '采购负责人', value: displayValue(form.purchaseOwnerUserName) },
  { label: '最终批准人', value: displayValue(form.finalApproverUserName) },
]);
const approvalRows = computed<ApprovalRow[]>(() => [
  {
    handler: displayValue(form.projectLeaderUserName),
    node: '项目负责人审核',
    opinion: displayValue(form.projectLeaderOpinion),
    time: displayValue(form.projectLeaderHandleTime),
  },
  {
    handler: displayValue(form.purchaseOwnerUserName),
    node: '采购负责人审核',
    opinion: displayValue(form.purchaseOwnerOpinion),
    time: displayValue(form.purchaseOwnerHandleTime),
  },
  {
    handler: displayValue(form.finalApproverUserName),
    node: '最终批准人审核',
    opinion: displayValue(form.finalApproverOpinion),
    time: displayValue(form.finalApproverHandleTime),
  },
  {
    handler: displayValue(form.applicantName),
    node: '归档确认',
    opinion: displayValue(form.archiveOpinion),
    time: displayValue(form.archiveTime),
  },
]);
const isDirectArchivedForm = computed(
  () =>
    form.status === 'ARCHIVED' &&
    !form.processInstanceId &&
    form.archiveOpinion === DIRECT_ARCHIVE_FORM_TEXT,
);
const workflowModalTitle = computed(() => {
  if (workflowAction.value === 'project') {
    return '项目负责人审核';
  }
  if (workflowAction.value === 'purchase') {
    return '采购负责人审核';
  }
  if (workflowAction.value === 'final') {
    return '最终批准人审核';
  }
  if (workflowAction.value === 'archive') {
    return '发起人归档确认';
  }
  return '发起人确认';
});
const workflowOkText = computed(() => {
  if (workflowAction.value !== 'initiator') {
    return '确认提交';
  }
  return workflowDirectArchive.value ? '直接归档' : '发送批准';
});
const materialPickerInitialFilters = computed(() => ({
  materialName: form.materialName || undefined,
  specModel: form.materialModel || undefined,
}));
const productBomPickerInitialFilters = computed(() => ({
  productMaterialKeyword: form.usedProduct || undefined,
}));

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入申请编号' },
        fieldName: 'requestNo',
        label: '申请编号',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料名称' },
        fieldName: 'materialName',
        label: '物料名称',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入使用产品' },
        fieldName: 'usedProduct',
        label: '使用产品',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: applyTypeOptions,
          placeholder: '请选择申请类型',
        },
        fieldName: 'applyType',
        label: '申请类型',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: Object.entries(statusMetaMap).map(([value, meta]) => ({
            label: meta.text,
            value,
          })),
          placeholder: '请选择当前状态',
        },
        fieldName: 'status',
        label: '当前状态',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
        fieldName: 'supplierName',
        label: '供应商名称',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'requestNo',
        fixed: 'left',
        minWidth: 190,
        slots: { default: 'requestNo' },
        title: '申请编号',
      },
      { field: 'materialName', minWidth: 180, title: '物料名称' },
      { field: 'materialModel', minWidth: 150, title: '型号' },
      {
        align: 'center',
        field: 'applyType',
        minWidth: 110,
        slots: { default: 'applyType' },
        title: '申请类型',
      },
      { field: 'applyDept', minWidth: 140, title: '申请部门' },
      { field: 'usedProduct', minWidth: 180, title: '使用产品' },
      { align: 'right', field: 'requireQty', minWidth: 110, title: '需求数量' },
      { field: 'supplierName', minWidth: 180, title: '指定供应商' },
      {
        align: 'center',
        field: 'status',
        minWidth: 150,
        slots: { default: 'status' },
        title: '当前状态',
      },
      { field: 'applicantName', minWidth: 130, title: '申请人' },
      { field: 'applyDate', minWidth: 120, title: '申请日期' },
      { field: 'requireDate', minWidth: 120, title: '需求日期' },
      { field: 'updateTime', minWidth: 170, title: '更新时间' },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 132,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSampleRequestPage({
            ...formValues,
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
          });
          return {
            list: result.list || [],
            total: result.total || 0,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmSampleRequestApi.SampleRequest>,
});

const [DetailModal, detailModalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal srm-erp-crud-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
});

const [SupplierModal, supplierModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});

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

function createEmptyForm(): SrmSampleRequestApi.SampleRequest {
  return {
    applyDate: todayText(),
    applyType: 'NORMAL',
    directArchive: false,
    materialName: '',
    specifiedSupplierType: 'NONE',
    status: 'DRAFT',
    trialValidations: [],
  };
}

function resetForm(record: Partial<SrmSampleRequestApi.SampleRequest> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record);
  form.status = form.status || 'DRAFT';
  form.applyType = normalizeApplyType(form.applyType);
  form.specifiedSupplierType = normalizeSpecifiedSupplierType(
    form.specifiedSupplierType,
  );
}

function openCreate() {
  createChoiceModalOpen.value = true;
}

async function beginCreate(mode: CreateEntryMode) {
  createChoiceModalOpen.value = false;
  const currentUser = await getCurrentUser();
  sampleEvaluationRows.value = [];
  resetForm({
    applicantId: currentUser.userId,
    applicantName: currentUser.userName,
    directArchive: mode === 'archive',
  });
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmSampleRequestApi.SampleRequest) {
  await openDetailById(Number(row.id), 'detail');
}

async function openEdit(row: SrmSampleRequestApi.SampleRequest) {
  await openDetailById(Number(row.id), 'edit');
}

async function openDetailById(id: number, mode: DetailMode = 'detail') {
  if (!id) {
    return;
  }
  detailLoading.value = true;
  detailModalApi.open();
  try {
    const result = await getSampleRequest(id);
    resetForm(result);
    detailMode.value =
      mode === 'edit' && result.canEdit !== false ? 'edit' : 'detail';
    basicExpanded.value = detailMode.value !== 'detail';
    await loadSampleEvaluations(id);
    await attachmentPanelRef.value?.loadAttachments();
  } finally {
    detailLoading.value = false;
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

async function loadTrialValidations(sampleRequestId: number) {
  const result = await getSampleRequest(sampleRequestId);
  form.trialValidations = result.trialValidations || [];
}

function switchToEdit() {
  if (form.canEdit === false) {
    message.warning('当前状态不能编辑');
    return;
  }
  detailMode.value = 'edit';
  basicExpanded.value = true;
}

async function closeDetail() {
  logDrawerVisible.value = false;
  await detailModalApi.close();
  if (isRouteEntryMode()) {
    router.back();
  }
}

async function saveDraft() {
  if (!validateBaseForm()) {
    return;
  }
  saving.value = true;
  try {
    const payload = buildSavePayload();
    const savedId = isCreateMode.value
      ? await createSampleRequest(payload)
      : (await updateSampleRequest(payload), Number(form.id));
    await attachmentPanelRef.value?.syncAttachments({
      bizId: savedId,
      bizType: BIZ_TYPE,
    });
    message.success('保存成功');
    await openDetailById(savedId, 'detail');
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

function validateBaseForm() {
  if (!form.materialName?.trim()) {
    message.warning('请填写物料名称');
    return false;
  }
  if (!form.applyType) {
    message.warning('请选择申请类型');
    return false;
  }
  return true;
}

function buildSavePayload(): SrmSampleRequestApi.SampleRequest {
  const specifiedSupplierType = normalizeSpecifiedSupplierType(
    form.specifiedSupplierType,
  );
  const directArchive = Boolean(form.directArchive);
  const hasSupplierMaster = specifiedSupplierType === 'HAS';
  const shouldKeepSupplierName = specifiedSupplierType !== 'NONE';
  return {
    ...form,
    applyDept: form.applyDept?.trim(),
    finalApproverUserId: directArchive ? undefined : form.finalApproverUserId,
    finalApproverUserName: form.finalApproverUserName?.trim(),
    materialModel: form.materialModel?.trim(),
    materialName: form.materialName?.trim(),
    oaApprovalUrl: form.oaApprovalUrl?.trim(),
    projectLeaderUserId: directArchive ? undefined : form.projectLeaderUserId,
    projectLeaderUserName: form.projectLeaderUserName?.trim(),
    purchaseOwnerUserId: directArchive ? undefined : form.purchaseOwnerUserId,
    purchaseOwnerUserName: form.purchaseOwnerUserName?.trim(),
    requestNo: form.requestNo?.trim(),
    directArchive,
    purchaseDifficulty: undefined,
    specifiedSupplierType,
    supplierCode: hasSupplierMaster ? form.supplierCode?.trim() : '',
    supplierId: hasSupplierMaster ? form.supplierId : undefined,
    supplierName: shouldKeepSupplierName ? form.supplierName?.trim() : '',
    usedProduct: form.usedProduct?.trim(),
  };
}

function getRowDropDownActions(row: SrmSampleRequestApi.SampleRequest) {
  const actions: any[] = [];
  if (row.canEdit !== false && row.status === 'DRAFT') {
    actions.push(
      {
        icon: ACTION_ICON.EDIT,
        label: '编辑',
        onClick: openEdit.bind(null, row),
        type: 'link',
      },
      {
        danger: true,
        icon: ACTION_ICON.DELETE,
        label: '删除',
        onClick: confirmDelete.bind(null, row),
        type: 'link',
      },
    );
  }
  return actions;
}

function confirmDelete(row: SrmSampleRequestApi.SampleRequest) {
  AntModal.confirm({
    content: `确认删除样品需求单 ${row.requestNo || ''} 吗？`,
    okText: '确认删除',
    okType: 'danger',
    title: '删除确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await deleteSampleRequest(Number(row.id));
      message.success('删除成功');
      await gridApi.query();
    },
  });
}

function openSupplierPicker() {
  if (isReadonly.value) {
    return;
  }
  supplierModalApi
    .setData({
      keyword: form.supplierName || form.supplierCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'roster',
      title: '选择指定供应商',
    })
    .open();
}

function handleSupplierSelect(row: Record<string, any>) {
  form.supplierId = normalizeId(row.supplierId || row.id);
  form.supplierCode = firstText(row.supplierCode, row.code);
  form.supplierName = firstText(row.supplierName, row.name);
  if (form.supplierName) {
    form.specifiedSupplierType = 'HAS';
  }
}

function openMaterialPicker() {
  if (!isReadonly.value) {
    materialPickerOpen.value = true;
  }
}

function handleMaterialPickerClose() {
  materialPickerOpen.value = false;
}

function handleMaterialPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  form.materialName = firstText(row.materialName, row.name, option.label);
  form.materialModel = firstText(row.specModel, row.model, form.materialModel);
  materialPickerOpen.value = false;
}

function openProductBomPicker() {
  if (!isReadonly.value) {
    productBomPickerOpen.value = true;
  }
}

function handleProductBomPickerClose() {
  productBomPickerOpen.value = false;
}

function handleProductBomPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  form.usedProduct = firstText(
    row.productMaterialName,
    row.bomName,
    row.materialName,
    option.label,
  );
  productBomPickerOpen.value = false;
}

function openUserPicker(target: UserPickTarget) {
  if (isReadonly.value && target !== 'final' && target !== 'workflowPurchase') {
    return;
  }
  activeUserPickTarget.value = target;
  const currentIdMap: Record<UserPickTarget, number | undefined> = {
    final: form.finalApproverUserId,
    project: form.projectLeaderUserId,
    purchase: form.purchaseOwnerUserId,
    workflowPurchase: workflowPurchaseReviewerUserId.value,
  };
  const titleMap: Record<UserPickTarget, string> = {
    final: '选择最终批准人',
    project: '选择项目负责人',
    purchase: '选择采购负责人',
    workflowPurchase: '选择采购审核办理人',
  };
  const currentId = currentIdMap[target];
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_USER_SELECT_MODAL_Z_INDEX,
      multiple: false,
      title: titleMap[target],
      userIds: currentId ? [currentId] : [],
    })
    .open();
}

function handleUserSelect(users: SystemUserApi.User[]) {
  const user = users[0];
  if (!user?.id) {
    return;
  }
  const name = firstText(user.nickname, user.username, user.name);
  switch (activeUserPickTarget.value) {
    case 'project': {
      form.projectLeaderUserId = Number(user.id);
      form.projectLeaderUserName = name;
      break;
    }
    case 'purchase': {
      form.purchaseOwnerUserId = Number(user.id);
      form.purchaseOwnerUserName = name;
      break;
    }
    case 'workflowPurchase': {
      workflowPurchaseReviewerUserId.value = Number(user.id);
      workflowPurchaseReviewerUserName.value = name;
      break;
    }
    default: {
      form.finalApproverUserId = Number(user.id);
      form.finalApproverUserName = name;
    }
  }
}

function handleProjectLeaderNameInput() {
  form.projectLeaderUserId = undefined;
}

function handlePurchaseOwnerNameInput() {
  form.purchaseOwnerUserId = undefined;
}

function handleFinalApproverNameInput() {
  form.finalApproverUserId = undefined;
}

function clearSupplierIfNone() {
  if (form.specifiedSupplierType === 'NONE') {
    form.supplierId = undefined;
    form.supplierCode = '';
    form.supplierName = '';
  } else if (form.specifiedSupplierType === 'OTHER') {
    form.supplierId = undefined;
    form.supplierCode = '';
  }
}

function openAttachmentUpload(category: string) {
  attachmentPanelRef.value?.openUploadModal(category);
}

function openLogDrawer(mode: BusinessLogMode) {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
}

function openAuditLog() {
  if (!form.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      processInstanceId: form.processInstanceId,
      title: `审批日志 - ${form.requestNo || '样品需求单'}`,
    })
    .open();
}

function copyOaUrl() {
  const url = String(form.oaApprovalUrl || '').trim();
  if (!url) {
    message.warning('请先填写 OA 审批链接');
    return;
  }
  void navigator.clipboard?.writeText(url);
  message.success('OA 审批链接已复制');
}

function openOaUrl() {
  const url = String(form.oaApprovalUrl || '').trim();
  if (!url) {
    message.warning('请先填写 OA 审批链接');
    return;
  }
  window.open(url, '_blank', 'noopener,noreferrer');
}

function confirmSubmit() {
  if (!form.id) {
    message.warning('请先保存样品需求单');
    return;
  }
  if (!form.projectLeaderUserId || !form.purchaseOwnerUserName?.trim()) {
    message.warning('请先选择项目负责人并填写采购负责人名称');
    return;
  }
  AntModal.confirm({
    content: '确认提交样品需求单并启动审批流程吗？',
    okText: '确认提交',
    title: '提交确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await submitSampleRequest(Number(form.id));
      message.success('提交成功');
      await openDetailById(Number(form.id), 'detail');
      await gridApi.query();
    },
  });
}

function openWorkflowAction(action: WorkflowAction) {
  workflowAction.value = action;
  workflowOpinion.value = '';
  workflowDirectArchive.value = action === 'initiator';
  workflowPurchaseDifficulty.value = form.purchaseDifficulty || '';
  workflowPurchaseReviewerUserId.value = form.purchaseOwnerUserId;
  workflowPurchaseReviewerUserName.value = form.purchaseOwnerUserId
    ? form.purchaseOwnerUserName || ''
    : '';
  workflowModalOpen.value = true;
}

async function confirmWorkflowAction() {
  if (!form.id) {
    message.warning('缺少样品需求单 ID');
    return;
  }
  if (
    workflowAction.value === 'initiator' &&
    !workflowDirectArchive.value &&
    !form.finalApproverUserId
  ) {
    message.warning('请选择最终批准人');
    return;
  }
  if (
    workflowAction.value === 'project' &&
    !workflowPurchaseReviewerUserId.value
  ) {
    message.warning('请选择采购审核办理人');
    return;
  }
  saving.value = true;
  try {
    switch (workflowAction.value) {
      case 'archive': {
        await archiveConfirmSampleRequest({
          id: Number(form.id),
          opinion: workflowOpinion.value,
        });
        break;
      }
      case 'final': {
        await finalApproveSampleRequest({
          id: Number(form.id),
          opinion: workflowOpinion.value,
        });
        break;
      }
      case 'project': {
        await projectReviewSampleRequest({
          id: Number(form.id),
          nextPurchaseOwnerUserId: workflowPurchaseReviewerUserId.value,
          nextPurchaseOwnerUserName: workflowPurchaseReviewerUserName.value,
          opinion: workflowOpinion.value,
        });
        break;
      }
      case 'purchase': {
        await purchaseReviewSampleRequest({
          id: Number(form.id),
          opinion: workflowOpinion.value,
          purchaseDifficulty: workflowPurchaseDifficulty.value,
        });
        break;
      }
      default: {
        await initiatorDecisionSampleRequest({
          directArchive: workflowDirectArchive.value,
          finalApproverUserId: workflowDirectArchive.value
            ? undefined
            : form.finalApproverUserId,
          finalApproverUserName: workflowDirectArchive.value
            ? undefined
            : form.finalApproverUserName,
          id: Number(form.id),
          opinion: workflowOpinion.value,
        });
      }
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    await openDetailById(Number(form.id), 'detail');
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function getCurrentUser() {
  const storeUser = (userStore.userInfo || {}) as Record<string, any>;
  let profile = currentUserProfile.value;
  if (!profile) {
    try {
      profile = (await getUserProfile()) as Record<string, any>;
      currentUserProfile.value = profile;
    } catch {
      profile = {};
    }
  }
  const mergedUser = { ...profile, ...storeUser };
  return {
    userId: normalizeId(mergedUser.id || mergedUser.userId),
    userName:
      firstText(
        mergedUser.nickname,
        mergedUser.realName,
        mergedUser.empName,
        mergedUser.username,
      ) || '',
  };
}

function statusText(status?: string) {
  return statusMetaMap[status || '']?.text || displayValue(status);
}

function statusColor(status?: string) {
  return statusMetaMap[status || '']?.color || 'default';
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
  const id = normalizeId(form.id);
  if (id) {
    void loadSampleEvaluations(id);
    void loadTrialValidations(id);
  }
}

function handleTrialValidationSuccess() {
  const id = normalizeId(form.id);
  if (id) {
    void loadTrialValidations(id);
  }
}

function applyTypeText(value?: string) {
  return (
    applyTypeOptions.find((item) => item.value === normalizeApplyType(value))
      ?.label || displayValue(value)
  );
}

function supplierTypeText(value?: string) {
  return (
    specifiedSupplierOptions.find(
      (item) => item.value === normalizeSpecifiedSupplierType(value),
    )?.label || displayValue(value)
  );
}

function flowActionText(action?: string) {
  return flowActionTextMap[action || ''] || displayValue(action);
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

function firstText(...values: unknown[]) {
  return (
    values
      .map((value) => String(value ?? '').trim())
      .find((value) => value.length > 0) || ''
  );
}

function normalizeApplyType(value?: string) {
  if (value === '1') {
    return 'NORMAL';
  }
  if (value === '2') {
    return 'URGENT';
  }
  return value || 'NORMAL';
}

function normalizeSpecifiedSupplierType(value?: string) {
  if (value === '1') {
    return 'HAS';
  }
  if (value === '0') {
    return 'NONE';
  }
  return value || 'NONE';
}

function todayText() {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate(),
  )}`;
}

function isRouteEntryMode() {
  return Boolean(
    firstText(
      route.query.id,
      route.query.sampleRequestId,
      route.query.businessKey,
      route.query.bizId,
    ),
  );
}

onMounted(() => {
  const entryId = normalizeId(
    firstText(
      route.query.id,
      route.query.sampleRequestId,
      route.query.businessKey,
      route.query.bizId,
    ),
  );
  if (entryId) {
    void openDetailById(entryId, 'detail');
  }
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <Button type="primary" @click="openCreate">
          <IconifyIcon icon="lucide:plus" />
          新增样品需求单
        </Button>
      </template>
      <template #requestNo="{ row }">
        <a class="srm-crud-link" @click="openDetail(row)">
          {{ row.requestNo || '-' }}
        </a>
      </template>
      <template #applyType="{ row }">
        <Tag
          :color="
            normalizeApplyType(row.applyType) === 'URGENT' ? 'error' : 'default'
          "
        >
          {{ applyTypeText(row.applyType) }}
        </Tag>
      </template>
      <template #status="{ row }">
        <Tag :color="statusColor(row.status)">
          {{ statusText(row.status) }}
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
          ]"
          :drop-down-actions="getRowDropDownActions(row)"
        />
      </template>
    </Grid>

    <AntModal
      v-model:open="createChoiceModalOpen"
      :footer="null"
      :mask-closable="false"
      title="新建样品需求单"
      :width="520"
    >
      <div class="sample-req-create-choice">
        <Button block size="large" @click="beginCreate('archive')">
          归档表单
        </Button>
        <Button
          block
          size="large"
          type="primary"
          @click="beginCreate('workflow')"
        >
          发起新流程
        </Button>
      </div>
    </AntModal>

    <DetailModal>
      <PickerModal
        :config="materialPickerConfig"
        :initial-filters="materialPickerInitialFilters"
        :open="materialPickerOpen"
        title="选择物料"
        @close="handleMaterialPickerClose"
        @pick="handleMaterialPick"
      />
      <PickerModal
        :config="productBomPickerConfig"
        :initial-filters="productBomPickerInitialFilters"
        :open="productBomPickerOpen"
        title="选择使用产品"
        @close="handleProductBomPickerClose"
        @pick="handleProductBomPick"
      />
      <SupplierModal @select="handleSupplierSelect" />
      <UserModal @confirm="handleUserSelect" />
      <ProcessAuditModal />
      <SampleEvaluationDetailModal @success="handleSampleEvaluationSuccess" />
      <TrialValidationDetailModalHost @success="handleTrialValidationSuccess" />

      <Spin :spinning="detailLoading || saving" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">样品需求单</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="
                  isReadonly &&
                  form.canEdit !== false &&
                  form.status === 'DRAFT'
                "
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑
              </Button>
              <Button
                v-if="!isReadonly"
                class="qms-ncr-toolbar-action"
                :loading="saving"
                type="primary"
                @click="saveDraft"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button
                v-if="form.canSubmit"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="confirmSubmit"
              >
                <IconifyIcon icon="lucide:send" />
                提交
              </Button>
              <Button
                v-if="form.canProjectReview"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openWorkflowAction('project')"
              >
                办理
              </Button>
              <Button
                v-if="form.canPurchaseReview"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openWorkflowAction('purchase')"
              >
                办理
              </Button>
              <Button
                v-if="form.canInitiatorDecision"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openWorkflowAction('initiator')"
              >
                确认流转
              </Button>
              <Button
                v-if="form.canFinalApprove"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openWorkflowAction('final')"
              >
                办理
              </Button>
              <Button
                v-if="form.canArchiveConfirm"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openWorkflowAction('archive')"
              >
                归档确认
              </Button>
              <Dropdown>
                <Button class="qms-ncr-toolbar-action">
                  <IconifyIcon icon="lucide:history" />
                  日志
                </Button>
                <template #overlay>
                  <Menu>
                    <Menu.Item
                      key="operation"
                      @click="openLogDrawer('operation')"
                    >
                      操作日志
                    </Menu.Item>
                    <Menu.Item key="audit" @click="openLogDrawer('audit')">
                      流程日志
                    </Menu.Item>
                    <Menu.Item key="process" @click="openAuditLog">
                      审批日志
                    </Menu.Item>
                  </Menu>
                </template>
              </Dropdown>
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
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
                      <strong>样品需求采购申请单</strong>
                    </div>
                    <Space :size="8">
                      <Tag :color="statusColor(form.status)">
                        {{ statusText(form.status) }}
                      </Tag>
                      <Button
                        size="small"
                        type="link"
                        @click="basicExpanded = !basicExpanded"
                      >
                        <IconifyIcon
                          :icon="
                            basicExpanded
                              ? 'lucide:chevron-up'
                              : 'lucide:chevron-down'
                          "
                        />
                        {{ basicExpanded ? '收起' : '展开' }}
                      </Button>
                    </Space>
                  </div>
                  <div
                    v-show="basicExpanded"
                    class="erp-form-grid sample-req-form-grid"
                  >
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料名称</label>
                      <div class="erp-form-value">
                        <Input.Search
                          v-if="canEditForm"
                          v-model:value="form.materialName"
                          placeholder="可输入物料名称或点击右侧选择"
                          @search="openMaterialPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.materialName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">型号</label>
                      <div class="erp-form-value">
                        <Input.Search
                          v-if="canEditForm"
                          v-model:value="form.materialModel"
                          placeholder="可输入型号或点击右侧选择"
                          @search="openMaterialPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.materialModel) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">申请类型</label>
                      <div class="erp-form-value">
                        <Radio.Group
                          v-if="canEditForm"
                          v-model:value="form.applyType"
                          :options="applyTypeOptions"
                        />
                        <Tag
                          v-else
                          :color="
                            normalizeApplyType(form.applyType) === 'URGENT'
                              ? 'error'
                              : 'default'
                          "
                        >
                          {{ applyTypeText(form.applyType) }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">申请部门</label>
                      <div class="erp-form-value">
                        <AutoComplete
                          v-if="canEditForm"
                          v-model:value="form.applyDept"
                          allow-clear
                          :options="applyDepartmentOptions"
                          placeholder="请选择或输入申请部门"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.applyDept) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">使用产品</label>
                      <div class="erp-form-value">
                        <Input.Search
                          v-if="canEditForm"
                          v-model:value="form.usedProduct"
                          placeholder="可输入使用产品或点击右侧选择"
                          @search="openProductBomPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.usedProduct) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">需求数量</label>
                      <div class="erp-form-value">
                        <InputNumber
                          v-if="canEditForm"
                          v-model:value="form.requireQty"
                          class="w-full"
                          :min="0"
                          placeholder="请输入需求数量"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.requireQty) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">申请人</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ displayValue(form.applicantName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">申请日期</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="canEditForm"
                          v-model:value="form.applyDate"
                          format="YYYY-MM-DD"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.applyDate) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">需求日期</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="canEditForm"
                          v-model:value="form.requireDate"
                          format="YYYY-MM-DD"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.requireDate) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label">有无指定供应商</label>
                      <div class="erp-form-value sample-req-supplier-line">
                        <Radio.Group
                          v-if="canEditForm"
                          v-model:value="form.specifiedSupplierType"
                          :options="specifiedSupplierOptions"
                          @change="clearSupplierIfNone"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value sample-req-supplier-type"
                        >
                          {{ supplierTypeText(form.specifiedSupplierType) }}
                        </span>
                        <Input
                          v-if="
                            canEditForm &&
                            form.specifiedSupplierType === 'OTHER'
                          "
                          v-model:value="form.supplierName"
                          class="sample-req-supplier-input"
                          placeholder="请输入其它供应商"
                        />
                        <Input.Search
                          v-else-if="
                            canEditForm && form.specifiedSupplierType === 'HAS'
                          "
                          v-model:value="form.supplierName"
                          class="sample-req-supplier-input sample-req-supplier-name-input"
                          placeholder="请输入供应商名称或点击右侧选择"
                          @search="openSupplierPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <Input
                          v-if="
                            canEditForm && form.specifiedSupplierType === 'HAS'
                          "
                          v-model:value="form.supplierCode"
                          class="sample-req-supplier-input sample-req-supplier-code-input"
                          placeholder="请输入供应商代码"
                        />
                        <span
                          v-if="!canEditForm && shouldShowSupplierInput"
                          class="qms-exception-readonly-value sample-req-supplier-name"
                        >
                          {{ displayValue(supplierValueText) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        产品技术要求说明
                      </label>
                      <div class="erp-form-value sample-req-textarea-value">
                        <Input.TextArea
                          v-if="canEditForm"
                          v-model:value="form.technicalRequirement"
                          :rows="4"
                          placeholder="请填写产品具体情况，如温度、耐酸碱、防水、硬度、宽幅、合格后采购计划等"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value--multiline"
                        >
                          {{ displayValue(form.technicalRequirement) }}
                        </span>
                        <Button
                          v-if="canEditForm"
                          size="small"
                          type="link"
                          @click="openAttachmentUpload('TECH_REQUIREMENT')"
                        >
                          上传产品技术要求说明附件
                        </Button>
                      </div>
                    </div>
                    <div
                      v-if="shouldShowPurchaseDifficulty"
                      class="erp-form-item erp-form-item--full"
                    >
                      <label class="erp-form-label erp-form-label--tall">
                        采购开发难点
                      </label>
                      <div class="erp-form-value sample-req-textarea-value">
                        <span class="qms-exception-readonly-value--multiline">
                          {{ displayValue(form.purchaseDifficulty) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        研发样品必要性
                      </label>
                      <div class="erp-form-value sample-req-textarea-value">
                        <Input.TextArea
                          v-if="canEditForm"
                          v-model:value="form.rdSampleNecessity"
                          :rows="4"
                          placeholder="请填写研发样品必要性"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value--multiline"
                        >
                          {{ displayValue(form.rdSampleNecessity) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label">OA审批链接</label>
                      <div class="erp-form-value">
                        <div class="srm-url-input">
                          <Input
                            v-if="canEditForm"
                            v-model:value="form.oaApprovalUrl"
                            class="srm-url-input__field"
                            placeholder="请输入 OA 审批链接（http/https）"
                          />
                          <span v-else class="qms-exception-readonly-value">
                            {{ displayValue(form.oaApprovalUrl) }}
                          </span>
                          <Button
                            :disabled="!form.oaApprovalUrl"
                            @click="copyOaUrl"
                          >
                            <IconifyIcon icon="lucide:copy" />
                            复制
                          </Button>
                          <Button
                            :disabled="!form.oaApprovalUrl"
                            @click="openOaUrl"
                          >
                            <IconifyIcon icon="lucide:external-link" />
                            打开
                          </Button>
                        </div>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">项目负责人</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm && form.directArchive"
                          v-model:value="form.projectLeaderUserName"
                          placeholder="请输入项目负责人姓名"
                          @change="handleProjectLeaderNameInput"
                        />
                        <Input.Search
                          v-else-if="canEditForm"
                          v-model:value="form.projectLeaderUserName"
                          placeholder="请选择项目负责人"
                          readonly
                          @search="openUserPicker('project')"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:user-plus" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.projectLeaderUserName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">采购负责人</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm && form.directArchive"
                          v-model:value="form.purchaseOwnerUserName"
                          placeholder="请输入采购负责人姓名"
                          @change="handlePurchaseOwnerNameInput"
                        />
                        <Input.Search
                          v-else-if="canEditForm"
                          v-model:value="form.purchaseOwnerUserName"
                          placeholder="请输入采购负责人名称或点击右侧选择"
                          @change="handlePurchaseOwnerNameInput"
                          @search="openUserPicker('purchase')"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:user-plus" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.purchaseOwnerUserName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">最终批准人</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm && form.directArchive"
                          v-model:value="form.finalApproverUserName"
                          placeholder="请输入最终批准人姓名"
                          @change="handleFinalApproverNameInput"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ displayValue(form.finalApproverUserName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注
                      </label>
                      <div class="erp-form-value sample-req-textarea-value">
                        <Input.TextArea
                          v-if="canEditForm"
                          v-model:value="form.remark"
                          :rows="3"
                          placeholder="请输入备注"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value--multiline"
                        >
                          {{ displayValue(form.remark) }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div
                    v-show="!basicExpanded"
                    class="erp-form-grid sample-req-form-grid sample-req-summary-grid"
                  >
                    <div
                      v-for="item in compactBasicRows"
                      :key="item.label"
                      class="erp-form-item"
                      :class="{ 'erp-form-item--full': item.full }"
                    >
                      <label class="erp-form-label">{{ item.label }}</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ item.value }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section v-if="form.id" class="erp-basic-form">
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
                              {{
                                displayValue(
                                  item.reportTime || item.evaluationDate,
                                )
                              }}
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
                            <td class="sample-req-empty-cell" colspan="5">
                              暂无样品评价
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </Spin>
                </section>

                <section v-if="form.id" class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>试产验证跟踪</strong>
                    </div>
                  </div>
                  <div class="sample-req-approval-table-wrap">
                    <table
                      class="sample-req-approval-table sample-req-trial-table"
                    >
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
                          <td>
                            {{ displayValue(item.sourceSampleEvaluationNo) }}
                          </td>
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
                          <td class="sample-req-empty-cell" colspan="10">
                            暂无试产验证跟踪
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>

                <section
                  v-if="form.id && isDirectArchivedForm"
                  class="erp-basic-form"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>归档说明</strong>
                    </div>
                  </div>
                  <div class="sample-req-direct-archive">
                    {{ DIRECT_ARCHIVE_FORM_TEXT }}
                  </div>
                </section>

                <section v-else-if="form.id" class="erp-basic-form">
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

                <div class="sample-req-attachment-actions" v-if="canEditForm">
                  <Button
                    v-for="item in attachmentCategoryButtons"
                    :key="item.value"
                    @click="openAttachmentUpload(item.value)"
                  >
                    <IconifyIcon icon="lucide:upload" />
                    {{ item.label }}
                  </Button>
                </div>
                <SrmAttachmentPanel
                  ref="attachmentPanelRef"
                  :category-options="attachmentCategoryButtons"
                  category-dict-type="mes_srm_sample_request_attachment_category"
                  :biz-id="form.id"
                  :biz-type="BIZ_TYPE"
                  default-category="SAMPLE_REQUEST_OFFLINE_FORM"
                  :mode="detailMode"
                />
              </div>
            </div>
          </div>
        </div>
      </Spin>

      <BusinessLogDrawer
        v-model:open="logDrawerVisible"
        :logs="businessLogs"
        :mode="logDrawerMode"
        :title="logDrawerMode === 'operation' ? '操作日志' : '流程日志'"
      />

      <AntModal
        v-model:open="workflowModalOpen"
        :confirm-loading="saving"
        :mask-closable="false"
        :ok-text="workflowOkText"
        :title="workflowModalTitle"
        :width="640"
        @ok="confirmWorkflowAction"
      >
        <div class="sample-req-workflow-modal">
          <template v-if="workflowAction === 'initiator'">
            <Radio.Group v-model:value="workflowDirectArchive">
              <Radio :value="true">直接归档</Radio>
              <Radio :value="false">发送最终批准人</Radio>
            </Radio.Group>
            <div v-if="!workflowDirectArchive" class="sample-req-workflow-user">
              <span>最终批准人</span>
              <Input.Search
                v-model:value="form.finalApproverUserName"
                placeholder="请选择最终批准人"
                readonly
                @search="openUserPicker('final')"
              >
                <template #enterButton>
                  <Button>
                    <IconifyIcon icon="lucide:user-plus" />
                  </Button>
                </template>
              </Input.Search>
            </div>
          </template>
          <template v-else-if="workflowAction === 'project'">
            <div class="sample-req-workflow-user">
              <span>采购审核办理人</span>
              <Input.Search
                v-model:value="workflowPurchaseReviewerUserName"
                placeholder="请选择采购审核办理人"
                readonly
                @search="openUserPicker('workflowPurchase')"
              >
                <template #enterButton>
                  <Button>
                    <IconifyIcon icon="lucide:user-plus" />
                  </Button>
                </template>
              </Input.Search>
            </div>
          </template>
          <template v-else-if="workflowAction === 'purchase'">
            <label class="sample-req-workflow-field">
              <span>采购开发难点</span>
              <Input.TextArea
                v-model:value="workflowPurchaseDifficulty"
                :maxlength="2000"
                placeholder="请填写采购开发难点"
                :rows="5"
                show-count
              />
            </label>
          </template>
          <Input.TextArea
            v-model:value="workflowOpinion"
            :maxlength="1000"
            :placeholder="
              workflowAction === 'initiator'
                ? '请填写归档或流转说明'
                : '请填写审核意见'
            "
            :rows="5"
            show-count
          />
        </div>
      </AntModal>
    </DetailModal>
  </Page>
</template>

<style scoped>
.sample-req-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.sample-req-summary-grid {
  border-top: 0;
}

.sample-req-create-choice {
  display: grid;
  gap: 12px;
}

.sample-req-supplier-line {
  display: flex;
  min-width: 0;
  flex-wrap: nowrap;
  gap: 14px;
}

.sample-req-supplier-line :deep(.ant-radio-group) {
  flex: 0 0 auto;
  white-space: nowrap;
}

.sample-req-supplier-input {
  width: 100%;
  min-width: 0;
  flex: 1 1 auto;
}

.sample-req-supplier-line :deep(.ant-input-search) {
  min-width: 0;
  flex: 1 1 auto;
}

.sample-req-supplier-name-input {
  flex: 1 1 420px;
  min-width: 260px;
}

.sample-req-supplier-code-input {
  flex: 0 1 260px;
  min-width: 180px;
}

.sample-req-supplier-type {
  max-width: 180px;
  white-space: nowrap;
}

.sample-req-supplier-name {
  min-width: 0;
  flex: 1 1 auto;
}

.sample-req-textarea-value {
  display: grid !important;
  align-items: stretch !important;
  gap: 6px;
}

.sample-req-approval-table-wrap {
  overflow-x: auto;
  border: 1px solid #dbe3ee;
  border-top: 0;
  background: #fff;
}

.sample-req-approval-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.sample-req-approval-table th,
.sample-req-approval-table td {
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  padding: 11px 14px;
  color: #24364f;
  text-align: left;
  vertical-align: middle;
}

.sample-req-approval-table th {
  background: #f7f9fc;
  color: #162338;
  font-weight: 600;
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
  color: #7a8799;
  text-align: center;
}

.sample-req-link-button {
  height: auto;
  padding: 0;
}

.sample-req-direct-archive {
  border: 1px solid #dbe3ee;
  border-top: 0;
  background: #fff;
  color: #24364f;
  padding: 16px 18px;
}

.sample-req-attachment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid #dbe3ee;
  border-bottom: 0;
  background: #fff;
  padding: 10px 12px;
}

.sample-req-attachment-actions .ant-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.sample-req-workflow-modal {
  display: grid;
  gap: 16px;
}

.sample-req-workflow-user {
  display: grid;
  gap: 8px;
}

.sample-req-workflow-field {
  display: grid;
  gap: 8px;
}

@media (max-width: 1100px) {
  .sample-req-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .sample-req-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .sample-req-supplier-line {
    flex-wrap: wrap;
  }

  .sample-req-supplier-input {
    min-width: 0;
    max-width: none;
  }

  .sample-req-supplier-code-input,
  .sample-req-supplier-name-input {
    flex: 1 1 100%;
  }
}
</style>
