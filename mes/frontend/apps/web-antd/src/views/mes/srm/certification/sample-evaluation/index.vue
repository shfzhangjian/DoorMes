<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSampleEvaluationApi } from '#/api/mes/srm/sample-evaluation';
import type { SrmSampleRequestApi } from '#/api/mes/srm/sample-req';
import type { SystemUserApi } from '#/api/system/user';
import type {
  BusinessLogItem,
  BusinessLogMode,
} from '#/components/business-log';

import {
  computed,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  reactive,
  ref,
} from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Modal as AntModal,
  AutoComplete,
  Button,
  Checkbox,
  DatePicker,
  Dropdown,
  Input,
  InputNumber,
  Menu,
  message,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  archiveConfirmSampleEvaluation,
  createSampleEvaluation,
  deleteSampleEvaluation,
  finalApproveSampleEvaluation,
  getLatestSampleEvaluationItems,
  getSampleEvaluation,
  getSampleEvaluationAssignableInspectors,
  getSampleEvaluationHistoryPage,
  getSampleEvaluationPage,
  getSampleEvaluationProjectConfig,
  getSampleEvaluationProjectEnabledList,
  initiatorDecisionSampleEvaluation,
  inspectionReportSampleEvaluation,
  issueTrialValidation,
  signSampleEvaluation,
  submitSampleEvaluation,
  updateSampleEvaluation,
  valueConfirmSampleEvaluation,
  withdrawConfirmSampleEvaluation,
} from '#/api/mes/srm/sample-evaluation';
import { getUserProfile } from '#/api/system/user/profile';
import { BusinessLogDrawer } from '#/components/business-log';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmAttachmentPanel from '../../shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmCertificationSampleEvaluation' });

type DetailMode = 'create' | 'detail' | 'edit';
type UserPickTarget = 'approvedBy';
type WorkflowAction =
  | 'archive'
  | 'confirm'
  | 'final'
  | 'initiator'
  | 'report'
  | 'sign'
  | 'withdraw';
type DetailAction = 'issueTrial' | WorkflowAction;

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 10;
const SRM_USER_SELECT_MODAL_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 200;
const BIZ_TYPE = 'SRM_SAMPLE_EVALUATION';
const ITEM_BIZ_TYPE = 'SRM_SAMPLE_EVALUATION_ITEM';
const SIGN_BIZ_TYPE = 'SRM_SAMPLE_EVALUATION_SIGN';

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  DEPT_SIGN: { color: 'processing', text: '会签' },
  DRAFT: { color: 'default', text: '草稿' },
  FINAL_APPROVAL: { color: 'processing', text: '最终批准' },
  INITIATOR_CONFIRM: { color: 'warning', text: '发起人确认' },
  INSPECTION_REPORT: { color: 'processing', text: '执行检测' },
  VALUE_CONFIRM: { color: 'warning', text: '检测结果确认' },
};
const verificationOptions = [
  { label: '产品改进', value: 'PRODUCT_IMPROVE' },
  { label: '新品开发', value: 'NEW_PRODUCT' },
  { label: '增加业务范围', value: 'BUSINESS_SCOPE' },
  { label: '增加供应商', value: 'ADD_SUPPLIER' },
  { label: '其他', value: 'OTHER' },
];
const inspectionTypeOptions = [
  { label: '尺寸检验', value: 'DIMENSION' },
  { label: '性能检验', value: 'PERFORMANCE' },
  { label: '功能检验', value: 'FUNCTION' },
  { label: '安规检验', value: 'SAFETY' },
  { label: '表面检验', value: 'SURFACE' },
  { label: '其他', value: 'OTHER' },
];
const judgementOptions = [
  { label: 'OK', value: 'PASS' },
  { label: 'NG', value: 'FAIL' },
];
const signResultOptions = [
  { label: '同意', value: 'PASS' },
  { label: '不同意', value: 'FAIL' },
];
const attachmentCategoryOptions = [
  { label: '样品评价表', value: 'SAMPLE_EVALUATION_FORM' },
  { label: '检验照片', value: 'INSPECTION_PHOTO' },
  { label: '其他', value: 'OTHER_ATTACHMENT' },
];
const itemAttachmentCategoryOptions = [
  { label: '检验照片', value: 'INSPECTION_PHOTO' },
  { label: '其他', value: 'OTHER_ATTACHMENT' },
];

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const detailMode = ref<DetailMode>('detail');
const detailLoading = ref(false);
const saving = ref(false);
const basicExpanded = ref(true);
const activeUserPickTarget = ref<UserPickTarget>('approvedBy');
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('report');
const workflowOpinion = ref('');
const workflowPassed = ref(true);
const workflowSignResult = ref('PASS');
const projectOptions = ref<SrmSampleEvaluationApi.Project[]>([]);
const submitAssignModalOpen = ref(false);
const submitAssignLoading = ref(false);
const submitAssignableUsers = ref<SrmSampleEvaluationApi.AssignableInspector[]>(
  [],
);
const selectedInspectorUserId = ref<number>();
const historyModalOpen = ref(false);
const historyLoading = ref(false);
const historyRows = ref<SrmSampleEvaluationApi.SampleEvaluation[]>([]);
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const itemAttachmentModalOpen = ref(false);
const activeItemId = ref<number>();
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const itemAttachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const signAttachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const currentUserProfile = ref<Record<string, any>>();

const form =
  reactive<SrmSampleEvaluationApi.SampleEvaluation>(createEmptyForm());

const isReadonly = computed(() => detailMode.value === 'detail');
const canEditForm = computed(() => !isReadonly.value);
const canEditInspectionItems = computed(
  () => canEditForm.value || Boolean(form.canInspectionReport),
);
const canEditAttachments = computed(
  () => canEditForm.value || Boolean(form.canInspectionReport),
);
const attachmentDefaultCategory = computed(() =>
  form.canInspectionReport ? 'INSPECTION_PHOTO' : 'SAMPLE_EVALUATION_FORM',
);
const subtitleItems = computed(() => [form.evaluationNo || ''].filter(Boolean));
const statusMeta = computed(
  () => statusMetaMap[String(form.status || 'DRAFT')] || statusMetaMap.DRAFT,
);
const hasEnteredDepartmentConfirm = computed(() => {
  const status = String(form.status || '');
  return (
    [
      'ARCHIVE_CONFIRM',
      'ARCHIVED',
      'DEPT_SIGN',
      'FINAL_APPROVAL',
      'INITIATOR_CONFIRM',
    ].includes(status) || (form.signs || []).length > 0
  );
});
const hasSignSection = computed(() => {
  const status = String(form.status || '');
  return (
    [
      'ARCHIVE_CONFIRM',
      'ARCHIVED',
      'DEPT_SIGN',
      'FINAL_APPROVAL',
      'INITIATOR_CONFIRM',
      'VALUE_CONFIRM',
    ].includes(status) || (form.signs || []).length > 0
  );
});
const signSectionRows = computed(() => {
  if ((form.signs || []).length > 0) {
    return form.signs || [];
  }
  return (form.projectUsers || []).map((item) => ({
    deptName: item.deptName,
    id: `project-${item.id || item.userId}`,
    signStatus: 'PLAN',
    userName: item.userName,
  }));
});
const projectSelectOptions = computed(() =>
  projectOptions.value
    .filter((project) => project.id)
    .map((project) => ({
      label: project.projectName || project.projectCode,
      value: project.id,
    })),
);
const inspectorSelectOptions = computed(() =>
  submitAssignableUsers.value
    .filter((user) => user.userId)
    .map((user) => ({
      label: `${user.userName || user.userId}${user.deptName ? ` / ${user.deptName}` : ''}${user.recommended ? '（上次）' : ''}`,
      value: user.userId,
    })),
);
const applyDepartmentOptions = computed(() =>
  getDictOptions('mes_srm_apply_department').map((option) => ({
    label: String(option.label ?? option.value),
    value: String(option.label ?? option.value),
  })),
);
const currentSign = computed(() =>
  (form.signs || []).find((item) => item.id === form.currentSignId),
);
const currentSignRequiresAttachment = computed(
  () =>
    Boolean(currentSign.value?.requireAttachment) ||
    String(currentSign.value?.deptName || '').includes('生产'),
);
const businessLogs = computed<BusinessLogItem[]>(() =>
  (form.logs || []).map((log) => ({
    actionCode: log.action,
    actionName: log.actionName || flowActionText(log.action),
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
const workflowTitle = computed(() => {
  const titles: Record<WorkflowAction, string> = {
    archive: '归档',
    confirm: '检测结果确认',
    final: '最终批准',
    initiator: '提交最终审批',
    report: '执行检测',
    sign: '会签',
    withdraw: '会签前撤回',
  };
  return titles[workflowAction.value];
});
const workflowConfirmText = computed(() => {
  const titles: Record<WorkflowAction, string> = {
    archive: '完成归档',
    confirm: '确认',
    final: '批准',
    initiator: '提交最终审批',
    report: '提交检测结果',
    sign: '提交会签意见',
    withdraw: '确认撤回',
  };
  return titles[workflowAction.value];
});
const availableWorkflowActions = computed<
  Array<{ action: DetailAction; label: string }>
>(() => {
  const actions: Array<{ action: DetailAction; label: string }> = [];
  if (form.canInspectionReport) {
    actions.push({ action: 'report', label: '执行检测' });
  }
  if (form.canValueConfirm) {
    actions.push({ action: 'confirm', label: '检测结果确认' });
  }
  if (form.canSign) {
    actions.push({ action: 'sign', label: '会签' });
  }
  if (form.canWithdrawConfirm) {
    actions.push({ action: 'withdraw', label: '会签前撤回' });
  }
  if (form.canInitiatorDecision) {
    actions.push({ action: 'initiator', label: '提交最终审批' });
  }
  if (form.canFinalApprove) {
    actions.push({ action: 'final', label: '最终批准' });
  }
  if (form.canArchiveConfirm) {
    actions.push({ action: 'archive', label: '完成归档' });
  }
  if (form.canIssueTrialValidation) {
    actions.push({ action: 'issueTrial', label: '下达试生产通知' });
  }
  return actions;
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入单据编号' },
        fieldName: 'evaluationNo',
        label: '单据编号',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入品名' },
        fieldName: 'materialName',
        label: '品名',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入项目名称' },
        fieldName: 'projectName',
        label: '项目',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
        fieldName: 'supplierName',
        label: '供应商名称',
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
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'evaluationNo',
        fixed: 'left',
        minWidth: 190,
        slots: { default: 'evaluationNo' },
        title: '单据编号',
      },
      { field: 'projectName', minWidth: 120, title: '项目' },
      { field: 'supplierName', minWidth: 180, title: '供应商名称' },
      { field: 'materialName', minWidth: 180, title: '品名' },
      { field: 'materialModel', minWidth: 150, title: '规格/型号' },
      { align: 'right', field: 'sampleQty', minWidth: 110, title: '样品数量' },
      {
        align: 'center',
        field: 'sampleSendCount',
        minWidth: 110,
        title: '送样次数',
      },
      { field: 'applyDept', minWidth: 130, title: '申请部门' },
      { field: 'evaluationDate', minWidth: 120, title: '日期' },
      {
        align: 'center',
        field: 'status',
        minWidth: 150,
        slots: { default: 'status' },
        title: '当前状态',
      },
      { field: 'initiatorUserName', minWidth: 120, title: '发起人' },
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
          const result = await getSampleEvaluationPage({
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
  } as VxeTableGridOptions<SrmSampleEvaluationApi.SampleEvaluation>,
});

const [DetailShellModal, detailShellModalApi] = useVbenModal({
  class: 'qms-abnormal-workbench-modal qms-ncr-erp-modal srm-erp-crud-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange(isOpen) {
    if (isOpen) {
      return;
    }
    resetDetailRuntimeState();
  },
});
const [SampleRequestModal, sampleRequestModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
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

function createEmptyForm(): SrmSampleEvaluationApi.SampleEvaluation {
  return {
    evaluationDate: todayText(),
    inspectionTypes: [],
    items: [],
    projectUsers: [],
    sampleSendCount: 1,
    signs: [],
    status: 'DRAFT',
    verificationTypes: [],
  };
}

function resetForm(
  record: Partial<SrmSampleEvaluationApi.SampleEvaluation> = {},
) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record);
  form.items = (form.items || []).map((item, index) => ({
    ...item,
    rowNo: item.rowNo || index + 1,
  }));
  form.signs = form.signs || [];
  form.projectUsers = form.projectUsers || [];
  form.verificationTypes = form.verificationTypes || [];
  form.inspectionTypes = form.inspectionTypes || [];
}

function openCreate() {
  resetForm();
  detailMode.value = 'create';
  basicExpanded.value = true;
  void loadProjectOptions();
  detailShellModalApi.open();
}

async function openDetail(id: number, mode: DetailMode = 'detail') {
  detailShellModalApi.open();
  detailMode.value = mode;
  detailLoading.value = true;
  try {
    await loadProjectOptions();
    const record = await getSampleEvaluation(id);
    resetForm(record || {});
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  void detailShellModalApi.close();
  if (
    route.query.id ||
    route.query.businessKey ||
    route.query.sampleEvaluationId
  ) {
    void router.replace({ path: route.path, query: {} });
  }
}

function resetDetailRuntimeState() {
  workflowModalOpen.value = false;
  historyModalOpen.value = false;
  itemAttachmentModalOpen.value = false;
  logDrawerVisible.value = false;
  detailLoading.value = false;
  saving.value = false;
}

function closeDetailRuntimeForRouteSwitch() {
  resetDetailRuntimeState();
  void detailShellModalApi.close();
}

async function saveDetail() {
  if (!form.projectId) {
    message.warning('请选择项目');
    return;
  }
  if (!firstText(form.applyDept)) {
    message.warning('请选择或输入申请部门');
    return;
  }
  if (!form.materialName) {
    message.warning('请选择或填写品名');
    return;
  }
  if (!(await ensureCurrentUserCanSaveByProjectInitiator())) {
    return;
  }
  saving.value = true;
  try {
    const payload = sanitizePayload();
    if (form.id) {
      await updateSampleEvaluation(payload);
      message.success('保存成功');
      await openDetail(Number(form.id), 'edit');
    } else {
      const id = await createSampleEvaluation(payload);
      message.success('保存成功');
      await attachmentPanelRef.value?.syncAttachments({
        bizId: id,
        bizType: BIZ_TYPE,
      });
      await openDetail(id, 'edit');
    }
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function ensureCurrentUserCanSaveByProjectInitiator() {
  const projectId = normalizeId(form.projectId);
  if (!projectId) {
    return false;
  }
  const config = await getSampleEvaluationProjectConfig(projectId);
  form.projectUsers = config.users || [];
  const initiators = form.projectUsers.filter(
    (item) => item.canInitiate === true,
  );
  const projectName = firstText(
    config.projectName,
    form.projectName,
    form.projectCode,
  );
  if (initiators.length === 0) {
    showSaveInitiatorNotConfigured(projectName);
    return false;
  }
  const currentUser = await getCurrentUser();
  if (
    !currentUser.userId ||
    !initiators.some((item) => normalizeId(item.userId) === currentUser.userId)
  ) {
    showSaveInitiatorDenied(projectName, buildProjectUserNames(initiators));
    return false;
  }
  return true;
}

function showSaveInitiatorDenied(projectName: string, userNames: string) {
  message.destroy();
  AntModal.warning({
    content: `项目 ${projectName}，只有 ${userNames} 用户才能保存发起！`,
    okText: '知道了',
    title: '保存发起权限提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function showSaveInitiatorNotConfigured(projectName: string) {
  message.destroy();
  AntModal.warning({
    content: `项目 ${projectName}，未配置允许保存发起的责任人，请先维护样品评价项目配置！`,
    okText: '知道了',
    title: '保存发起权限提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function buildProjectUserNames(users: SrmSampleEvaluationApi.ProjectUser[]) {
  return (
    [
      ...new Set(
        users
          .map((item) => firstText(item.userName, item.userId))
          .filter(Boolean),
      ),
    ].join('、') || '已配置'
  );
}

async function loadProjectOptions() {
  if (projectOptions.value.length > 0) {
    return;
  }
  projectOptions.value = await getSampleEvaluationProjectEnabledList();
}

async function handleProjectChange(projectId?: unknown) {
  const selectedProjectId = Number(projectId);
  const project = projectOptions.value.find(
    (item) => item.id === selectedProjectId,
  );
  form.projectId = project?.id;
  form.projectCode = project?.projectCode;
  form.projectName = project?.projectName;
  form.projectUsers = [];
  if (!project?.id) {
    return;
  }
  const config = await getSampleEvaluationProjectConfig(project.id);
  form.projectUsers = config.users || [];
}

function sanitizePayload(): SrmSampleEvaluationApi.SampleEvaluation {
  return {
    ...form,
    items: (form.items || []).map((item, index) => ({
      ...item,
      rowNo: item.rowNo || index + 1,
    })),
  };
}

function openSampleRequestPicker() {
  if (isReadonly.value) {
    return;
  }
  sampleRequestModalApi
    .setData({
      keyword: form.materialName,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'sampleRequest',
      title: '选择样品需求单',
    })
    .open();
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
      title: '选择供应商',
    })
    .open();
}

function handleSupplierSelect(row: Record<string, any>) {
  form.supplierId = row.supplierId || row.id;
  form.supplierCode = firstText(row.supplierCode, row.code);
  form.supplierName = firstText(row.supplierName, row.name);
}

async function handleSampleRequestSelect(
  row: SrmSampleRequestApi.SampleRequest,
) {
  form.sampleRequestId = row.id;
  form.sampleRequestNo = row.requestNo;
  form.supplierId = row.supplierId;
  form.supplierCode = row.supplierCode;
  form.supplierName = row.supplierName;
  form.materialName = row.materialName;
  form.materialModel = row.materialModel;
  form.sampleQty = row.requireQty;
  form.applyDept = row.applyDept;
  form.applyDate = row.applyDate;
  form.approvedByUserId = row.finalApproverUserId;
  form.approvedByName = row.finalApproverUserName;
  form.sampleSendCount = Number(row.sampleEvaluationCount || 0) + 1;
  if (row.id && Number(row.sampleEvaluationCount || 0) > 0) {
    const latestItems = await getLatestSampleEvaluationItems(Number(row.id));
    if (latestItems.length > 0) {
      form.items = latestItems.map((item, index) => ({
        itemName: item.itemName,
        rowNo: index + 1,
        technicalRequirement: item.technicalRequirement,
      }));
    }
  }
}

function addItem() {
  form.items = [
    ...(form.items || []),
    {
      itemStatus: 'PENDING',
      rowNo: (form.items || []).length + 1,
    },
  ];
}

function removeItem(index: number) {
  form.items = (form.items || []).filter((_, itemIndex) => itemIndex !== index);
  form.items.forEach((item, itemIndex) => {
    item.rowNo = itemIndex + 1;
  });
}

function openItemAttachment(row: SrmSampleEvaluationApi.Item) {
  if (!row.id) {
    message.warning('请先保存检验项目后再上传附件');
    return;
  }
  activeItemId.value = row.id;
  itemAttachmentModalOpen.value = true;
}

async function openHistory() {
  if (!form.sampleRequestId) {
    message.warning('请先选择样品需求单');
    return;
  }
  historyModalOpen.value = true;
  historyLoading.value = true;
  try {
    const result = await getSampleEvaluationHistoryPage(
      Number(form.sampleRequestId),
      {
        pageNo: 1,
        pageSize: 20,
      },
    );
    historyRows.value = result.list || [];
  } finally {
    historyLoading.value = false;
  }
}

function openUserPicker(target: UserPickTarget) {
  if (isReadonly.value) {
    return;
  }
  activeUserPickTarget.value = target;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_USER_SELECT_MODAL_Z_INDEX,
      multiple: false,
      title: getUserPickerTitle(target),
      userIds: getInitialUserIds(target),
    })
    .open();
}

function getInitialUserIds(target: UserPickTarget) {
  if (target === 'approvedBy') {
    return form.approvedByUserId ? [form.approvedByUserId] : [];
  }
  return [];
}

function getUserPickerTitle(_target: UserPickTarget) {
  return '选择批准人';
}

function handleUserSelect(users: SystemUserApi.User[]) {
  const user = users[0];
  if (!user?.id) {
    return;
  }
  const userId = Number(user.id);
  const userName = firstText(user.nickname, user.username, user.name);
  form.approvedByUserId = userId;
  form.approvedByName = userName;
}

function openWorkflow(action: WorkflowAction) {
  if (!form.id) {
    message.warning('请先保存样品评价表');
    return;
  }
  workflowAction.value = action;
  workflowOpinion.value = '';
  workflowPassed.value = true;
  workflowSignResult.value = 'PASS';
  workflowModalOpen.value = true;
}

function openOnlyWorkflowAction() {
  const action = availableWorkflowActions.value[0];
  if (action) {
    handleDetailAction(action.action);
  }
}

function handleDetailAction(action: DetailAction) {
  if (action === 'issueTrial') {
    confirmIssueTrialValidation();
    return;
  }
  if (action === 'report') {
    void submitInspectionReportInline();
    return;
  }
  openWorkflow(action);
}

async function submitInspectionReportInline() {
  if (!form.id) {
    return;
  }
  saving.value = true;
  try {
    const submitted = await submitInspectionReportPayload();
    if (!submitted) {
      return;
    }
    message.success('办理成功');
    await openDetail(form.id, 'detail');
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function submitInspectionReportPayload() {
  if (!form.id) {
    return false;
  }
  const items = normalizeInspectionItems(form.items || []);
  if (!hasEffectiveInspectionItem(items)) {
    message.warning('请至少在检验项目表填写一项检验项目和测试数据');
    return false;
  }
  form.items = items;
  await attachmentPanelRef.value?.syncAttachments({
    bizId: form.id,
    bizType: BIZ_TYPE,
  });
  await inspectionReportSampleEvaluation({
    id: form.id,
    items,
  });
  return true;
}

function confirmIssueTrialValidation() {
  if (!form.id) {
    return;
  }
  AntModal.confirm({
    content: `确认从样品评价单 ${form.evaluationNo || ''} 下达试生产通知并生成试产验证跟踪单？`,
    okText: '确认下达',
    title: '下达试生产通知',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      const trialId = await issueTrialValidation(Number(form.id));
      message.success(`试产验证跟踪单已生成：${trialId}`);
      await openDetail(Number(form.id), 'detail');
      await gridApi.query();
    },
  });
}

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

async function openSubmitAssignment() {
  if (!form.id) {
    message.warning('请先保存样品评价表');
    return;
  }
  if (!form.projectId) {
    message.warning('请选择项目');
    return;
  }
  selectedInspectorUserId.value = undefined;
  submitAssignableUsers.value = [];
  submitAssignModalOpen.value = false;
  if (!(await ensureCurrentUserCanAssignInspector())) {
    return;
  }
  submitAssignLoading.value = true;
  try {
    const result = await getSampleEvaluationAssignableInspectors(
      Number(form.projectId),
    );
    submitAssignableUsers.value = result.users || [];
    selectedInspectorUserId.value =
      result.recommendedUserId ||
      submitAssignableUsers.value.find((item) => item.recommended)?.userId ||
      submitAssignableUsers.value[0]?.userId;
    if (submitAssignableUsers.value.length === 0) {
      showAssignInspectorNotFound();
      return;
    }
    submitAssignModalOpen.value = true;
  } finally {
    submitAssignLoading.value = false;
  }
}

async function confirmSubmitAssignment() {
  if (!form.id || !selectedInspectorUserId.value) {
    showInspectorRequired();
    return;
  }
  if (!(await ensureCurrentUserCanAssignInspector())) {
    return;
  }
  const selectedUser = submitAssignableUsers.value.find(
    (item) => item.userId === selectedInspectorUserId.value,
  );
  saving.value = true;
  try {
    await submitSampleEvaluation({
      id: Number(form.id),
      inspectorUserId: selectedInspectorUserId.value,
      inspectorUserName: selectedUser?.userName,
    });
    submitAssignModalOpen.value = false;
    message.success('提交成功');
    await openDetail(Number(form.id), 'detail');
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function ensureCurrentUserCanAssignInspector() {
  const projectId = normalizeId(form.projectId);
  if (!projectId) {
    return false;
  }
  const config = await getSampleEvaluationProjectConfig(projectId);
  form.projectUsers = config.users || [];
  const projectName = firstText(
    config.projectName,
    form.projectName,
    form.projectCode,
  );
  const assigners = form.projectUsers.filter(
    (item) => item.canInitiate === true && item.canAssign === true,
  );
  if (assigners.length === 0) {
    showAssignInspectorNotConfigured(projectName);
    return false;
  }
  const currentUser = await getCurrentUser();
  if (
    !currentUser.userId ||
    !assigners.some((item) => normalizeId(item.userId) === currentUser.userId)
  ) {
    showAssignInspectorDenied(projectName, buildProjectUserNames(assigners));
    return false;
  }
  return true;
}

function showAssignInspectorDenied(projectName: string, userNames: string) {
  submitAssignModalOpen.value = false;
  message.destroy();
  AntModal.warning({
    content: `项目 ${projectName}，只有 ${userNames} 用户才能分配检测人！`,
    okText: '知道了',
    title: '分配检测人权限提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function showAssignInspectorNotConfigured(projectName: string) {
  submitAssignModalOpen.value = false;
  message.destroy();
  AntModal.warning({
    content: `项目 ${projectName}，未配置允许分配检测人的责任人，请先维护样品评价项目配置！`,
    okText: '知道了',
    title: '分配检测人权限提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function showAssignInspectorNotFound() {
  submitAssignModalOpen.value = false;
  message.destroy();
  AntModal.warning({
    content: '当前项目没有可分配的检测办理人',
    okText: '知道了',
    title: '分配检测人提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function showInspectorRequired() {
  message.destroy();
  AntModal.warning({
    content: '请选择检测办理人',
    okText: '知道了',
    title: '分配检测人提醒',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

async function submitWorkflow() {
  if (!form.id) {
    return;
  }
  saving.value = true;
  try {
    switch (workflowAction.value) {
      case 'archive': {
        await archiveConfirmSampleEvaluation({
          id: form.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      case 'confirm': {
        await valueConfirmSampleEvaluation({
          id: form.id,
          opinion: workflowOpinion.value,
          passed: workflowPassed.value,
          signUsers: [],
        });

        break;
      }
      case 'final': {
        await finalApproveSampleEvaluation({
          id: form.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      case 'initiator': {
        await initiatorDecisionSampleEvaluation({
          directArchive: false,
          id: form.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      case 'report': {
        if (!(await submitInspectionReportPayload())) {
          return;
        }

        break;
      }
      case 'sign': {
        if (currentSignRequiresAttachment.value && currentSign.value?.id) {
          await signAttachmentPanelRef.value?.syncAttachments({
            bizId: currentSign.value.id,
            bizType: SIGN_BIZ_TYPE,
          });
        }
        await signSampleEvaluation({
          id: form.id,
          opinion: workflowOpinion.value,
          result: workflowSignResult.value,
          signId: form.currentSignId,
        });

        break;
      }
      case 'withdraw': {
        await withdrawConfirmSampleEvaluation({
          id: form.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      // No default
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    await openDetail(form.id, 'detail');
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

function submitWithConfirm() {
  if (!form.id) {
    message.warning('请先保存样品评价表');
    return;
  }
  void openSubmitAssignment();
}

function deleteWithConfirm(record: SrmSampleEvaluationApi.SampleEvaluation) {
  AntModal.confirm({
    content: `确认删除样品评价表 ${record.evaluationNo || ''}？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await deleteSampleEvaluation(Number(record.id));
      message.success('删除成功');
      await gridApi.query();
    },
  });
}

function openAuditLog() {
  if (!form.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      id: form.processInstanceId,
      processInstanceId: form.processInstanceId,
    })
    .open();
}

function openLogDrawer(mode: BusinessLogMode) {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
}

function statusText(value?: unknown) {
  return statusMetaMap[String(value || '')]?.text || displayValue(value);
}

function statusColor(value?: unknown) {
  return statusMetaMap[String(value || '')]?.color || 'default';
}

function flowActionText(action?: unknown) {
  return (
    (
      {
        ARCHIVE: '归档',
        CREATE: '创建',
        FINAL_APPROVE: '最终批准',
        INSPECTION_REPORT: '执行检测',
        INITIATOR_SEND_APPROVAL: '发起人送审',
        SIGN: '会签',
        SIGN_COMPLETE: '会签完成',
        SUBMIT: '提交',
        UPDATE: '保存',
        VALUE_CONFIRM: '检测结果确认',
        WITHDRAW_CONFIRM: '会签前撤回',
      } as Record<string, string>
    )[String(action || '')] || displayValue(action)
  );
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function itemFieldValue(item: SrmSampleEvaluationApi.Item, field: string) {
  return (item as Record<string, any>)[field];
}

function setItemFieldValue(
  item: SrmSampleEvaluationApi.Item,
  field: string,
  value: unknown,
) {
  (item as Record<string, any>)[field] = value;
}

function normalizeInspectionItems(items: SrmSampleEvaluationApi.Item[]) {
  return items
    .filter((item) => hasInspectionItemContent(item))
    .map((item, index) => ({
      ...item,
      itemName: firstText(item.itemName),
      rowNo: index + 1,
      technicalRequirement: firstText(item.technicalRequirement),
      testData1: firstText(item.testData1),
      testData2: firstText(item.testData2),
      testData3: firstText(item.testData3),
      testData4: firstText(item.testData4),
      testData5: firstText(item.testData5),
    }));
}

function hasInspectionItemContent(item: SrmSampleEvaluationApi.Item) {
  return [
    item.itemName,
    item.technicalRequirement,
    item.testData1,
    item.testData2,
    item.testData3,
    item.testData4,
    item.testData5,
    item.itemJudgement,
  ].some((value) => firstText(value));
}

function hasEffectiveInspectionItem(items: SrmSampleEvaluationApi.Item[]) {
  return items.some(
    (item) =>
      firstText(item.itemName) &&
      [
        item.testData1,
        item.testData2,
        item.testData3,
        item.testData4,
        item.testData5,
      ].some((value) => firstText(value)),
  );
}

function tableCellValue(record: Record<string, any>, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex];
}

function firstText(...values: unknown[]) {
  return values.map((item) => String(item || '').trim()).find(Boolean) || '';
}

function normalizeId(value: unknown) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
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
    userId: normalizeId(mergedUser.id ?? mergedUser.userId),
    userName:
      firstText(
        mergedUser.nickname,
        mergedUser.realName,
        mergedUser.empName,
        mergedUser.username,
      ) || '',
  };
}

function todayText() {
  const date = new Date();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

onMounted(() => {
  const queryId =
    route.query.sampleEvaluationId || route.query.id || route.query.businessKey;
  if (queryId) {
    const id = Number(Array.isArray(queryId) ? queryId[0] : queryId);
    if (Number.isFinite(id) && id > 0) {
      void openDetail(id, 'detail');
    }
  }
});

onBeforeRouteLeave(() => {
  closeDetailRuntimeForRouteSwitch();
});

onBeforeUnmount(() => {
  closeDetailRuntimeForRouteSwitch();
});

onDeactivated(() => {
  closeDetailRuntimeForRouteSwitch();
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <Button type="primary" @click="openCreate">
          <IconifyIcon icon="ant-design:plus-outlined" />
          新增样品评价
        </Button>
      </template>
      <template #evaluationNo="{ row }">
        <Button
          type="link"
          class="!px-0 font-bold"
          @click="openDetail(row.id, 'detail')"
        >
          {{ row.evaluationNo }}
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="statusColor(row.status)" class="!m-0">
          {{ statusText(row.status) }}
        </Tag>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看',
              onClick: () => openDetail(row.id, 'detail'),
            },
            {
              disabled: row.status !== 'DRAFT',
              label: '编辑',
              onClick: () => openDetail(row.id, 'edit'),
            },
          ]"
          :drop-down-actions="[
            {
              disabled: row.status !== 'DRAFT',
              label: '删除',
              onClick: () => deleteWithConfirm(row),
            },
          ]"
        />
      </template>
    </Grid>
    <DetailShellModal>
      <SampleRequestModal @select="handleSampleRequestSelect" />
      <SupplierModal @select="handleSupplierSelect" />
      <UserModal @confirm="handleUserSelect" />
      <ProcessAuditModal />
      <BusinessLogDrawer
        v-model:open="logDrawerVisible"
        :items="businessLogs"
        :mode="logDrawerMode"
        title="样品评价流程日志"
      />

      <Spin :spinning="detailLoading || saving" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="ant-design:arrow-left-outlined" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">样品评价表</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{ statusMeta.text }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="form.processInstanceId"
                class="qms-ncr-toolbar-action"
                @click="openAuditLog"
              >
                <IconifyIcon icon="ant-design:profile-outlined" />
              </Button>
              <Button
                class="qms-ncr-toolbar-action"
                @click="openLogDrawer('audit')"
              >
                <IconifyIcon icon="ant-design:history-outlined" />
              </Button>
              <Button
                v-if="detailMode === 'detail' && form.canEdit"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="detailMode = 'edit'"
              >
                <IconifyIcon icon="ant-design:edit-outlined" />
                编辑
              </Button>
              <Button
                v-if="canEditForm"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="saveDetail"
              >
                <IconifyIcon icon="ant-design:save-outlined" />
                保存
              </Button>
              <Button
                v-if="form.canSubmit"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="submitWithConfirm"
              >
                提交
              </Button>
              <Button
                v-if="availableWorkflowActions.length === 1"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="openOnlyWorkflowAction"
              >
                {{ availableWorkflowActions[0]?.label }}
              </Button>
              <Dropdown v-else-if="availableWorkflowActions.length > 1">
                <Button class="qms-ncr-toolbar-action">
                  办理动作
                  <IconifyIcon icon="ant-design:down-outlined" />
                </Button>
                <template #overlay>
                  <Menu>
                    <Menu.Item
                      v-for="item in availableWorkflowActions"
                      :key="item.action"
                      @click="handleDetailAction(item.action)"
                    >
                      {{ item.label }}
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
              <div class="qms-exception-form sample-eval-detail-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>基本信息</strong>
                      <Tag :color="statusMeta.color" class="!m-0">
                        {{ statusMeta.text }}
                      </Tag>
                    </div>
                    <Button type="link" @click="basicExpanded = !basicExpanded">
                      <IconifyIcon
                        :icon="
                          basicExpanded
                            ? 'ant-design:up-outlined'
                            : 'ant-design:down-outlined'
                        "
                      />
                      {{ basicExpanded ? '收起' : '展开' }}
                    </Button>
                  </div>
                  <div
                    v-show="basicExpanded"
                    class="erp-form-grid sample-eval-form-grid"
                  >
                    <div class="erp-form-item">
                      <div class="erp-form-label">项目</div>
                      <div class="erp-form-value">
                        <Select
                          v-if="canEditForm"
                          v-model:value="form.projectId"
                          allow-clear
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="projectSelectOptions"
                          placeholder="请选择项目"
                          @change="handleProjectChange"
                        />
                        <span v-else>{{ displayValue(form.projectName) }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">供应商名称</div>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm"
                          v-model:value="form.supplierName"
                          placeholder="可填写供应商名称或点击选择"
                        >
                          <template #suffix>
                            <IconifyIcon
                              class="cursor-pointer text-slate-500"
                              icon="ant-design:search-outlined"
                              @click.stop="openSupplierPicker"
                            />
                          </template>
                        </Input>
                        <span v-else>{{
                          displayValue(form.supplierName)
                        }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">品名</div>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm"
                          v-model:value="form.materialName"
                          placeholder="请选择样品需求单或填写品名"
                        >
                          <template #suffix>
                            <IconifyIcon
                              class="cursor-pointer text-slate-500"
                              icon="ant-design:search-outlined"
                              @click.stop="openSampleRequestPicker"
                            />
                          </template>
                        </Input>
                        <span v-else>{{
                          displayValue(form.materialName)
                        }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">规格/型号</div>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm"
                          v-model:value="form.materialModel"
                          placeholder="请输入规格/型号"
                        />
                        <span v-else>{{
                          displayValue(form.materialModel)
                        }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">样品数量</div>
                      <div class="erp-form-value">
                        <InputNumber
                          v-if="canEditForm"
                          v-model:value="form.sampleQty"
                          :min="0"
                          placeholder="请输入样品数量"
                        />
                        <span v-else>{{ displayValue(form.sampleQty) }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">日期</div>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="canEditForm"
                          v-model:value="form.evaluationDate"
                          :get-popup-container="resolveNestedPopupContainer"
                          :popup-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else>{{
                          displayValue(form.evaluationDate)
                        }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">送样次数</div>
                      <div class="erp-form-value">
                        <InputNumber
                          v-if="canEditForm"
                          v-model:value="form.sampleSendCount"
                          :min="1"
                          placeholder="请输入送样次数"
                        />
                        <span v-else>{{
                          displayValue(form.sampleSendCount)
                        }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <div class="erp-form-label">验证类别</div>
                      <div class="erp-form-value">
                        <Checkbox.Group
                          v-if="canEditForm"
                          v-model:value="form.verificationTypes"
                          :options="verificationOptions"
                        />
                        <Checkbox.Group
                          v-else
                          disabled
                          :options="verificationOptions"
                          :value="form.verificationTypes || []"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <div class="erp-form-label">要求检验项目</div>
                      <div class="erp-form-value">
                        <Checkbox.Group
                          v-if="canEditForm"
                          v-model:value="form.inspectionTypes"
                          :options="inspectionTypeOptions"
                        />
                        <Checkbox.Group
                          v-else
                          disabled
                          :options="inspectionTypeOptions"
                          :value="form.inspectionTypes || []"
                        />
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">申请部门</div>
                      <div class="erp-form-value">
                        <AutoComplete
                          v-if="canEditForm"
                          v-model:value="form.applyDept"
                          allow-clear
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="applyDepartmentOptions"
                          placeholder="请选择或输入申请部门"
                        />
                        <span v-else>{{ displayValue(form.applyDept) }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">申请日期</div>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="canEditForm"
                          v-model:value="form.applyDate"
                          :get-popup-container="resolveNestedPopupContainer"
                          :popup-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else>{{ displayValue(form.applyDate) }}</span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">批准</div>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditForm"
                          v-model:value="form.approvedByName"
                          placeholder="请输入或选择批准人"
                        >
                          <template #suffix>
                            <IconifyIcon
                              class="cursor-pointer text-slate-500"
                              icon="ant-design:user-outlined"
                              @click.stop="openUserPicker('approvedBy')"
                            />
                          </template>
                        </Input>
                        <span v-else>{{
                          displayValue(form.approvedByName)
                        }}</span>
                      </div>
                    </div>
                  </div>
                  <div
                    v-show="!basicExpanded"
                    class="erp-form-grid sample-eval-summary-grid"
                  >
                    <div class="erp-form-item">
                      <div class="erp-form-label">项目</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.projectName) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">供应商名称</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.supplierName) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">品名</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.materialName) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">规格/型号</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.materialModel) }}
                      </div>
                    </div>
                  </div>
                </section>

                <section class="erp-basic-form sample-eval-table-section">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>检验项目</strong>
                    </div>
                    <Space>
                      <Button size="small" @click="openHistory">
                        查看历史检验
                      </Button>
                      <Button
                        v-if="canEditInspectionItems"
                        size="small"
                        type="primary"
                        @click="addItem"
                      >
                        新增项目
                      </Button>
                    </Space>
                  </div>
                  <div class="sample-eval-table-wrap">
                    <table class="sample-eval-table">
                      <thead>
                        <tr>
                          <th style="width: 60px">序号</th>
                          <th style="width: 170px">检验项目</th>
                          <th style="min-width: 520px">技术要求</th>
                          <th
                            v-for="index in 5"
                            :key="index"
                            style="width: 130px"
                          >
                            {{ index }}
                          </th>
                          <th style="width: 130px">单项判定</th>
                          <th style="width: 110px">附件</th>
                          <th v-if="canEditInspectionItems" style="width: 80px">
                            操作
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr
                          v-for="(item, index) in form.items"
                          :key="item.id || index"
                        >
                          <td class="text-center">{{ index + 1 }}</td>
                          <td>
                            <Input
                              v-if="canEditInspectionItems"
                              v-model:value="item.itemName"
                              placeholder="检验项目"
                            />
                            <span v-else>{{
                              displayValue(item.itemName)
                            }}</span>
                          </td>
                          <td>
                            <Input.TextArea
                              v-if="canEditInspectionItems"
                              v-model:value="item.technicalRequirement"
                              :auto-size="{ minRows: 1, maxRows: 3 }"
                              placeholder="技术要求"
                            />
                            <span v-else>{{
                              displayValue(item.technicalRequirement)
                            }}</span>
                          </td>
                          <td
                            v-for="field in [
                              'testData1',
                              'testData2',
                              'testData3',
                              'testData4',
                              'testData5',
                            ]"
                            :key="field"
                          >
                            <Input
                              v-if="canEditInspectionItems"
                              :value="itemFieldValue(item, field)"
                              placeholder="测试数据"
                              @update:value="
                                (value) => setItemFieldValue(item, field, value)
                              "
                            />
                            <span v-else>{{
                              displayValue(itemFieldValue(item, field))
                            }}</span>
                          </td>
                          <td>
                            <Radio.Group
                              v-if="canEditInspectionItems"
                              v-model:value="item.itemJudgement"
                              :options="judgementOptions"
                              option-type="button"
                              size="small"
                            />
                            <Tag
                              v-else
                              :color="
                                item.itemJudgement === 'FAIL'
                                  ? 'error'
                                  : item.itemJudgement === 'PASS'
                                    ? 'success'
                                    : 'default'
                              "
                            >
                              {{
                                item.itemJudgement === 'PASS'
                                  ? 'OK'
                                  : item.itemJudgement === 'FAIL'
                                    ? 'NG'
                                    : '-'
                              }}
                            </Tag>
                          </td>
                          <td class="text-center">
                            <Button
                              type="link"
                              class="!px-0"
                              @click="openItemAttachment(item)"
                            >
                              附件
                            </Button>
                          </td>
                          <td v-if="canEditInspectionItems" class="text-center">
                            <Button
                              danger
                              type="link"
                              class="!px-0"
                              @click="removeItem(index)"
                            >
                              删除
                            </Button>
                          </td>
                        </tr>
                        <tr v-if="!form.items || form.items.length === 0">
                          <td
                            :colspan="canEditInspectionItems ? 10 : 9"
                            class="sample-eval-empty"
                          >
                            暂无检验项目
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>

                <SrmAttachmentPanel
                  ref="attachmentPanelRef"
                  :biz-id="form.id"
                  :biz-type="BIZ_TYPE"
                  :category-options="attachmentCategoryOptions"
                  :default-category="attachmentDefaultCategory"
                  :mode="canEditAttachments ? 'edit' : 'detail'"
                  :modal-z-index="SRM_CONFIRM_MODAL_Z_INDEX"
                  title="附件材料"
                />

                <section v-if="hasSignSection" class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>会签</strong>
                    </div>
                  </div>
                  <Table
                    bordered
                    :columns="[
                      { dataIndex: 'deptName', title: '部门', width: 120 },
                      { dataIndex: 'userName', title: '签名', width: 140 },
                      { dataIndex: 'signOpinion', title: '问题说明' },
                      { dataIndex: 'signTime', title: '日期', width: 180 },
                      { dataIndex: 'signStatus', title: '状态', width: 110 },
                    ]"
                    :data-source="signSectionRows"
                    :pagination="false"
                    row-key="id"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'signStatus'">
                        <Tag
                          :color="
                            record.signStatus === 'COMPLETED'
                              ? 'success'
                              : 'default'
                          "
                        >
                          {{
                            record.signStatus === 'PLAN'
                              ? '待会签'
                              : record.signStatus === 'COMPLETED'
                                ? '已确认'
                                : '待确认'
                          }}
                        </Tag>
                      </template>
                      <template v-else>
                        {{
                          displayValue(tableCellValue(record, column.dataIndex))
                        }}
                      </template>
                    </template>
                  </Table>
                </section>

                <section
                  v-if="hasEnteredDepartmentConfirm"
                  class="erp-basic-form"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>归档信息</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid sample-eval-form-grid">
                    <div class="erp-form-item">
                      <div class="erp-form-label">最终批准人</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.finalApproverUserName) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">批准时间</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.finalApproverHandleTime) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <div class="erp-form-label">归档时间</div>
                      <div class="erp-form-value">
                        {{ displayValue(form.archiveTime) }}
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <div class="erp-form-label erp-form-label--tall">
                        最终批准
                      </div>
                      <div class="erp-form-value">
                        {{ displayValue(form.finalApproverOpinion) }}
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <div class="erp-form-label erp-form-label--tall">
                        归档说明
                      </div>
                      <div class="erp-form-value">
                        {{ displayValue(form.archiveOpinion) }}
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>

      <AntModal
        v-model:open="workflowModalOpen"
        :confirm-loading="saving"
        :ok-text="workflowConfirmText"
        :title="workflowTitle"
        :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
        width="720px"
        @ok="submitWorkflow"
      >
        <div class="sample-eval-action-form">
          <template v-if="workflowAction === 'report'">
            <p class="text-xs text-slate-500">
              提交前请在检验项目表中填写至少一项检验项目和值；提交后自动发送给发起人做检测结果确认。
            </p>
          </template>
          <template v-else-if="workflowAction === 'confirm'">
            <Radio.Group v-model:value="workflowPassed">
              <Radio :value="true">确认通过</Radio>
              <Radio :value="false">退回执行检测</Radio>
            </Radio.Group>
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="3"
              class="mt-3"
              placeholder="请输入本部门会签意见"
            />
            <div v-if="workflowPassed" class="mt-4">
              <div class="mb-2 font-bold text-slate-700">项目会签配置</div>
              <div class="mt-3 rounded border border-slate-200">
                <div
                  v-for="item in form.projectUsers || []"
                  :key="item.id || `${item.deptName}-${item.userId}`"
                  class="sample-eval-sign-pick-row"
                >
                  <span class="font-bold">{{ item.deptName }}</span>
                  <span class="min-w-0 flex-1 truncate text-slate-600">
                    {{ item.userName }}
                  </span>
                </div>
                <div
                  v-if="!form.projectUsers || form.projectUsers.length === 0"
                  class="sample-eval-sign-pick-row text-slate-500"
                >
                  当前项目尚未配置会签人员
                </div>
              </div>
            </div>
          </template>
          <template v-else-if="workflowAction === 'sign'">
            <div v-if="currentSign" class="mb-3 text-slate-600">
              当前确认：{{ currentSign.deptName }} / {{ currentSign.userName }}
            </div>
            <Radio.Group
              v-model:value="workflowSignResult"
              :options="signResultOptions"
              class="mb-3 w-full"
              option-type="button"
              button-style="solid"
            />
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="4"
              :placeholder="
                currentSignRequiresAttachment
                  ? '生产部确认必须填写意见'
                  : '请输入确认意见'
              "
            />
            <div
              v-if="currentSignRequiresAttachment && currentSign?.id"
              class="mt-3"
            >
              <SrmAttachmentPanel
                ref="signAttachmentPanelRef"
                :biz-id="currentSign.id"
                :biz-type="SIGN_BIZ_TYPE"
                :category-options="attachmentCategoryOptions"
                default-category="INSPECTION_PHOTO"
                mode="edit"
                :modal-z-index="SRM_USER_SELECT_MODAL_Z_INDEX"
                title="生产部确认附件"
              />
            </div>
          </template>
          <template v-else-if="workflowAction === 'initiator'">
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="4"
              placeholder="请输入提交最终审批说明"
            />
          </template>
          <template v-else-if="workflowAction === 'archive'">
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="4"
              placeholder="请输入归档说明"
            />
          </template>
          <template v-else-if="workflowAction === 'final'">
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="4"
              placeholder="请输入最终批准意见"
            />
          </template>
          <template v-else>
            <Input.TextArea
              v-model:value="workflowOpinion"
              :rows="4"
              placeholder="请输入办理意见"
            />
          </template>
        </div>
      </AntModal>

      <AntModal
        v-model:open="submitAssignModalOpen"
        :confirm-loading="saving"
        ok-text="分配并提交"
        title="分配执行检测人"
        :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
        width="560px"
        @ok="confirmSubmitAssignment"
      >
        <Spin :spinning="submitAssignLoading">
          <div class="sample-eval-action-form">
            <div class="sample-eval-action-row">
              <span>当前项目</span>
              <strong>{{ displayValue(form.projectName) }}</strong>
            </div>
            <div class="sample-eval-action-row">
              <span>检测办理人</span>
              <Select
                v-model:value="selectedInspectorUserId"
                :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveNestedPopupContainer"
                :options="inspectorSelectOptions"
                placeholder="请选择检测办理人"
              />
            </div>
          </div>
        </Spin>
      </AntModal>

      <AntModal
        v-model:open="itemAttachmentModalOpen"
        :footer="null"
        title="检验项目附件"
        :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
        width="860px"
      >
        <SrmAttachmentPanel
          v-if="activeItemId"
          ref="itemAttachmentPanelRef"
          :biz-id="activeItemId"
          :biz-type="ITEM_BIZ_TYPE"
          :category-options="itemAttachmentCategoryOptions"
          default-category="INSPECTION_PHOTO"
          mode="edit"
          title="检验项目附件"
        />
      </AntModal>

      <AntModal
        v-model:open="historyModalOpen"
        :footer="null"
        title="历史检验"
        :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
        width="1100px"
      >
        <Table
          bordered
          :columns="[
            { dataIndex: 'evaluationNo', title: '单据编号', width: 190 },
            { dataIndex: 'sampleSendCount', title: '送样次数', width: 100 },
            { dataIndex: 'evaluationDate', title: '日期', width: 130 },
            { dataIndex: 'status', title: '状态', width: 130 },
            { dataIndex: 'reporterUserName', title: '检验上报人', width: 140 },
            { dataIndex: 'updateTime', title: '更新时间', width: 180 },
          ]"
          :data-source="historyRows"
          :loading="historyLoading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'evaluationNo'">
              <Button
                type="link"
                class="!px-0"
                @click="
                  openDetail(record.id, 'detail');
                  historyModalOpen = false;
                "
              >
                {{ record.evaluationNo }}
              </Button>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <Tag :color="statusColor(record.status)">
                {{ statusText(record.status) }}
              </Tag>
            </template>
            <template v-else>
              {{ displayValue(tableCellValue(record, column.dataIndex)) }}
            </template>
          </template>
        </Table>
      </AntModal>
    </DetailShellModal>
  </Page>
</template>

<style scoped>
.sample-eval-form-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.sample-eval-summary-grid {
  border-top: 0;
}

.sample-eval-detail-form {
  min-height: calc(100dvh - 112px);
}

.sample-eval-table-section {
  display: flex;
  height: clamp(430px, calc(100dvh - 500px), 820px);
  min-height: 430px;
  flex-direction: column;
  overflow: hidden !important;
}

.sample-eval-table-wrap {
  box-sizing: border-box;
  min-height: 0;
  flex: 1 1 auto;
  overflow-x: scroll;
  overflow-y: auto;
  background: #fff;
  scrollbar-gutter: stable both-edges;
}

.sample-eval-table {
  width: 100%;
  min-width: 1560px;
  border-collapse: collapse;
  table-layout: fixed;
}

.sample-eval-table th,
.sample-eval-table td {
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 7px 8px;
  font-size: 13px;
  line-height: 20px;
  vertical-align: middle;
}

.sample-eval-table tbody tr {
  height: 52px;
}

.sample-eval-table th {
  background: #f8fafc;
  color: #334155;
  font-weight: 800;
  text-align: left;
  white-space: nowrap;
}

.sample-eval-table td :deep(.ant-input),
.sample-eval-table td :deep(.ant-input-number),
.sample-eval-table td :deep(.ant-select) {
  width: 100%;
}

.sample-eval-table td :deep(.ant-radio-group) {
  display: flex;
  width: 100%;
  flex-wrap: nowrap;
}

.sample-eval-table td :deep(.ant-radio-button-wrapper) {
  flex: 1 1 0;
  padding-inline: 6px;
  text-align: center;
}

.sample-eval-empty {
  height: 312px;
  color: #94a3b8;
  text-align: center;
  vertical-align: middle !important;
}

.sample-eval-action-form {
  display: grid;
  gap: 12px;
}

.sample-eval-action-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.sample-eval-sign-pick-row {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 10px;
}

.sample-eval-sign-pick-row:last-child {
  border-bottom: 0;
}

@media (max-width: 1100px) {
  .sample-eval-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .sample-eval-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
