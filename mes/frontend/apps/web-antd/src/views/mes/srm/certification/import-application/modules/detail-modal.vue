<script lang="ts" setup>
import type { SrmOnboardingApplyApi } from '#/api/mes/srm/onboarding-apply';
import type { SystemUserApi } from '#/api/system/user';
import type {
  BusinessLogItem,
  BusinessLogMode,
} from '#/components/business-log';
import type { PickerOption } from '#/components/picker';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Modal as AntModal,
  AutoComplete,
  Button,
  Checkbox,
  Input,
  message,
  Radio,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';

import {
  createOnboardingApply,
  entryOnboardingApply,
  generalManagerReviewOnboardingApply,
  getOnboardingApply,
  purchaseIntakeOnboardingApply,
  purchaseReviewOnboardingApply,
  purchaseTransferOnboardingApply,
  qualityReviewOnboardingApply,
  signOnboardingApply,
  submitOnboardingApply,
  techReviewOnboardingApply,
  updateOnboardingApply,
  useDeptReviewOnboardingApply,
} from '#/api/mes/srm/onboarding-apply';
import { getUserProfile } from '#/api/system/user/profile';
import { BusinessLogDrawer } from '#/components/business-log';
import {
  materialPickerConfig,
  PickerModal,
  productBomPickerConfig,
} from '#/components/picker';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '../../../shared/SrmReferenceSelectModal.vue';
import SrmOnboardingApplyInlineDetail from './inline-detail.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmOnboardingApplyDetailModal' });

const emit = defineEmits<{ success: [] }>();
type DetailMode = 'create' | 'detail' | 'edit';
type MaterialPickTarget = 'customer' | 'replace';
type UserPickTarget =
  | 'generalManager'
  | 'materialEntry'
  | 'purchase'
  | 'quality'
  | 'sign'
  | 'supplierRoster'
  | 'tech'
  | 'useDept';
type WorkflowAction =
  | 'entry'
  | 'generalManager'
  | 'purchase'
  | 'purchaseIntake'
  | 'purchaseTransfer'
  | 'quality'
  | 'sign'
  | 'tech'
  | 'useDept';

interface ModalData {
  id?: number | string;
  mode?: DetailMode;
  processInstanceId?: number | string;
  tabType?: string;
  taskId?: number | string;
}

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const SRM_USER_SELECT_MODAL_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 200;
const BIZ_TYPE = 'ONBOARDING_APPLY';
const DEFAULT_ATTACHMENT_CATEGORY = 'IMPORT_APPLICATION_FORM';
const IMPORT_USE_DEPARTMENT_DICT = 'mes_srm_import_use_department';

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
const importTypeOptions = [
  { label: '全新物料', value: 'NEW' },
  { label: '替代现有物料', value: 'REPLACE' },
  { label: '客户指定物料', value: 'CUSTOMER_SPECIFIED' },
];
const importUseDepartmentFallbackOptions = [
  '材料事业部',
  'MASK事业部',
  'CMP项目办',
].map((item) => ({ label: item, value: item }));
const signDeptOptions = [
  '品质部',
  '技术研发部',
  '生产部',
  '采购部',
  '市场部',
].map((item) => ({ label: item, value: item }));
const transferActionOptions = [
  { label: '发送总经理审核', value: 'GENERAL_MANAGER' },
  { label: '交办办理', value: 'ASSIGN_ENTRY' },
  { label: '完成归档', value: 'ARCHIVE' },
];
const importTypeTextMap: Record<string, string> = {
  CUSTOMER_SPECIFIED: '客户指定物料',
  NEW: '全新物料',
  REPLACE: '替代现有物料',
};
const actionTextMap: Record<WorkflowAction, string> = {
  entry: '交办办理',
  generalManager: '总经理审核',
  purchase: '采购部审批',
  purchaseIntake: '采购部门办理',
  purchaseTransfer: '采购部转办',
  quality: '品质部审批',
  sign: '部门会签',
  tech: '技术研发部审批',
  useDept: '使用部门负责人审核',
};
const flowActionTextMap: Record<string, string> = {
  ARCHIVE: '归档',
  CREATE: '创建',
  DEPT_SIGN: '部门会签',
  ENTRY_PROCESS: '交办办理',
  GENERAL_MANAGER_REVIEW: '总经理审核',
  PURCHASE_INTAKE: '采购部门办理',
  PURCHASE_REVIEW: '采购部审批',
  PURCHASE_TRANSFER: '采购部转办',
  QUALITY_REVIEW: '品质部审批',
  REJECT: '不通过',
  SUBMIT: '提交',
  TECH_REVIEW: '技术研发部审批',
  UPDATE: '保存',
  USE_DEPT_REVIEW: '使用部门负责人审核',
};

const userStore = useUserStore();
const currentUserProfile = ref<Record<string, any>>();
const loading = ref(false);
const saving = ref(false);
const detailMode = ref<DetailMode>('detail');
const modalData = ref<ModalData>({});
const activeUserPickTarget = ref<UserPickTarget>('useDept');
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('useDept');
const workflowResult = ref<'PASS' | 'REJECT'>('PASS');
const workflowOpinion = ref('');
const workflowTransferAction = ref<
  'ARCHIVE' | 'ASSIGN_ENTRY' | 'GENERAL_MANAGER'
>('ASSIGN_ENTRY');
const selectedSignDepts = ref<string[]>([]);
const signUsers = ref<SrmOnboardingApplyApi.SignUser[]>([]);
const activeSignDept = ref('');
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const detailRefreshKey = ref(0);
const productBomPickerOpen = ref(false);
const materialPickerOpen = ref(false);
const activeMaterialPickTarget = ref<MaterialPickTarget>('replace');
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();

const form = reactive<SrmOnboardingApplyApi.OnboardingApply>(createEmptyForm());

const applyDepartmentOptions = computed(() => {
  const dictOptions = getDictOptions(IMPORT_USE_DEPARTMENT_DICT).map(
    (option) => ({
      label: String(option.label ?? option.value),
      value: String(option.label ?? option.value),
    }),
  );
  return dictOptions.length > 0
    ? dictOptions
    : importUseDepartmentFallbackOptions;
});
const isReadonly = computed(() => detailMode.value === 'detail');
const statusMeta = computed(
  () =>
    statusMetaMap[form.status || ''] || {
      color: 'default',
      text: displayValue(form.status),
    },
);
const subtitleItems = computed(() =>
  [form.applyNo || '', form.currentNodeName || statusText(form.status)]
    .map((item) => item.trim())
    .filter(Boolean),
);
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
const workflowActions = computed(() => {
  const actions: Array<{ action: WorkflowAction; label: string }> = [];
  if (form.canPurchaseIntake) {
    actions.push({ action: 'purchaseIntake', label: '采购部门办理' });
  }
  if (form.canSign) actions.push({ action: 'sign', label: '部门会签' });
  if (form.canPurchaseTransfer) {
    actions.push({ action: 'purchaseTransfer', label: '采购部转办' });
  }
  if (form.canMaterialEntry || form.canSupplierRosterEntry) {
    actions.push({ action: 'entry', label: '交办办理' });
  }
  if (form.canGeneralManagerReview) {
    actions.push({ action: 'generalManager', label: '总经理审核' });
  }
  if (form.canUseDeptReview) actions.push({ action: 'useDept', label: '办理' });
  if (form.canQualityReview) actions.push({ action: 'quality', label: '办理' });
  if (form.canTechReview) actions.push({ action: 'tech', label: '办理' });
  if (form.canPurchaseReview)
    actions.push({ action: 'purchase', label: '办理' });
  return actions;
});
const productBomPickerInitialFilters = computed(() => ({
  productMaterialKeyword: form.applicableProduct || undefined,
}));
const materialPickerInitialFilters = computed(() => {
  if (activeMaterialPickTarget.value === 'customer') {
    return {
      materialCode: form.customerMaterialCode || undefined,
      materialName: form.customerMaterialName || undefined,
    };
  }
  return {
    materialCode: form.replacedMaterialCode || undefined,
    materialName: form.replacedMaterialName || undefined,
  };
});
const processRows = computed(() => {
  const rows = [
    {
      handler: displayValue(
        form.purchaseHandlerUserName || form.purchaseReviewerUserName,
      ),
      key: 'purchaseIntake',
      node: '采购部门办理',
      opinion: displayValue(form.purchaseIntakeOpinion),
      result: form.purchaseIntakeTime ? '已办理' : '-',
      time: displayValue(form.purchaseIntakeTime),
    },
    {
      handler: displayValue(form.generalManagerUserName),
      key: 'generalManager',
      node: '总经理审核',
      opinion: displayValue(form.generalManagerOpinion),
      result: reviewResultText(form.generalManagerResult),
      time: displayValue(form.generalManagerHandleTime),
    },
    {
      handler: displayValue(form.materialEntryUserName),
      key: 'materialEntry',
      node: '物料编码录入',
      opinion: displayValue(form.materialEntryOpinion),
      result: form.materialEntryCode ? '已完成' : '-',
      time: displayValue(form.materialEntryHandleTime),
    },
    {
      handler: displayValue(form.supplierRosterEntryUserName),
      key: 'supplierRosterEntry',
      node: '供方清单录入',
      opinion: displayValue(form.supplierRosterEntryOpinion),
      result: form.supplierRosterEntryCode ? '已完成' : '-',
      time: displayValue(form.supplierRosterEntryHandleTime),
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
const signRows = computed(() => form.signs || []);
const workflowRecordRows = computed(() => {
  const signRecordRows = signRows.value.map((item) => {
    let result = '-';
    if (item.signResult) {
      result = reviewResultText(item.signResult);
    } else if (item.signStatus === 'PENDING') {
      result = '待会签';
    }
    return {
      handler: displayValue(item.userName),
      key: `sign-${item.id || `${item.deptName}-${item.userId}`}`,
      node: displayValue(item.deptName),
      opinion: displayValue(item.signOpinion),
      result,
      time: displayValue(item.signTime),
    };
  });
  return [...processRows.value, ...signRecordRows];
});
const currentSign = computed(() => {
  const rows = form.signs || [];
  if (form.currentSignId) {
    const matched = rows.find((item) => item.id === form.currentSignId);
    if (matched) return matched;
  }
  return rows.find((item) => item.signStatus === 'PENDING');
});

const [Modal, modalApi] = useVbenModal({
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
  async onOpenChange(isOpen) {
    if (!isOpen) {
      workflowModalOpen.value = false;
      resetForm();
      return;
    }
    modalData.value = modalApi.getData<ModalData>() || {};
    detailMode.value = modalData.value.mode || 'detail';
    if (detailMode.value === 'create') {
      await openCreateForm();
      return;
    }
    await loadDetail(normalizeId(modalData.value.id));
  },
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

function createEmptyForm(): SrmOnboardingApplyApi.OnboardingApply {
  return {
    importType: 'NEW',
    materialCodeCreated: false,
    status: 'DRAFT',
    supplierRosterCreated: false,
  };
}

function resetForm(
  record: Partial<SrmOnboardingApplyApi.OnboardingApply> = {},
) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record);
}

async function openCreateForm() {
  const currentUser = await getCurrentUser();
  resetForm({
    applicantName: currentUser.userName,
    applyTime: nowText(),
  });
  detailRefreshKey.value += 1;
}

async function loadDetail(id?: number) {
  if (!id) {
    message.warning('缺少导入申请 ID');
    return;
  }
  loading.value = true;
  try {
    const detail = await getOnboardingApply(id);
    if (modalData.value.processInstanceId && !detail.processInstanceId) {
      detail.processInstanceId = String(modalData.value.processInstanceId);
    }
    resetForm(detail || {});
    detailRefreshKey.value += 1;
  } finally {
    loading.value = false;
  }
}

function closeDetail() {
  void modalApi.close();
}

function switchToEdit() {
  detailMode.value = 'edit';
}

async function saveRecord() {
  if (!validateForm()) {
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateOnboardingApply({ ...form });
    } else {
      const id = await createOnboardingApply({ ...form });
      form.id = Number(id);
    }
    await attachmentPanelRef.value?.syncAttachments({
      bizId: form.id,
      bizType: BIZ_TYPE,
    });
    message.success('保存成功');
    emit('success');
    detailMode.value = 'edit';
    await loadDetail(Number(form.id));
  } finally {
    saving.value = false;
  }
}

function validateForm() {
  if (!String(form.supplierName || '').trim()) {
    message.warning('请填写或选择供应商名称');
    return false;
  }
  if (!String(form.materialName || '').trim()) {
    message.warning('请填写物料名称');
    return false;
  }
  if (!String(form.importType || '').trim()) {
    message.warning('请选择导入类型');
    return false;
  }
  if (
    form.importType === 'REPLACE' &&
    !String(form.replacedMaterialCode || '').trim()
  ) {
    message.warning('请填写被替代旧物料编码');
    return false;
  }
  if (
    form.importType === 'CUSTOMER_SPECIFIED' &&
    !String(form.customerMaterialCode || '').trim() &&
    !String(form.customerMaterialName || '').trim()
  ) {
    message.warning('请填写客户指定物料编码或物料名称');
    return false;
  }
  return true;
}

function submitWithConfirm() {
  if (!form.id) {
    message.warning('请先保存导入申请');
    return;
  }
  AntModal.confirm({
    content: '确认提交导入申请并启动审批流程？',
    okText: '确认提交',
    title: '提交确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await submitOnboardingApply(Number(form.id));
      message.success('提交成功');
      emit('success');
      await loadDetail(Number(form.id));
      detailMode.value = 'detail';
    },
  });
}

function openWorkflowAction(action: WorkflowAction) {
  workflowAction.value = action;
  workflowResult.value = 'PASS';
  workflowOpinion.value = '';
  workflowTransferAction.value = form.canArchive ? 'ARCHIVE' : 'ASSIGN_ENTRY';
  selectedSignDepts.value = [];
  signUsers.value = [];
  activeSignDept.value = '';
  if (action === 'purchaseIntake') {
    const existedSigns = (form.signs || []).filter((item) => item.userId);
    const existedSignUsers = existedSigns.map((item) => ({
      deptCode: item.deptCode,
      deptName: String(item.deptName || ''),
      userId: Number(item.userId),
      userName: item.userName,
    }));
    signUsers.value = existedSignUsers.filter((item) => item.deptName);
    selectedSignDepts.value = [
      ...new Set(signUsers.value.map((item) => item.deptName)),
    ];
  }
  workflowModalOpen.value = true;
}

function confirmWorkflowAction() {
  if (!form.id) {
    message.warning('缺少导入申请 ID');
    return;
  }
  if (!validateWorkflowAction()) {
    return;
  }
  AntModal.confirm({
    content: `确认提交${actionTextMap[workflowAction.value]}办理结果吗？`,
    okText: '确认提交',
    title: '办理确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await submitWorkflowAction();
    },
  });
}

function validateWorkflowAction() {
  if (workflowAction.value === 'purchaseIntake') {
    if (!form.useDeptReviewerUserId) {
      message.warning('请选择使用部门负责人');
      return false;
    }
    if (signUsers.value.length === 0) {
      message.warning('请选择至少一个会签部门人员');
      return false;
    }
  }
  if (workflowAction.value === 'purchaseTransfer') {
    if (workflowTransferAction.value === 'GENERAL_MANAGER') {
      return true;
    }
    if (
      workflowTransferAction.value === 'ASSIGN_ENTRY' &&
      (!form.materialEntryUserId || !form.supplierRosterEntryUserId)
    ) {
      message.warning('请选择物料编码录入人和供方清单录入人');
      return false;
    }
  }
  if (workflowAction.value === 'entry') {
    if (form.canMaterialEntry && !String(form.materialEntryCode || '').trim()) {
      message.warning('请填写录入的物料编码');
      return false;
    }
    if (
      form.canSupplierRosterEntry &&
      !String(form.supplierRosterEntryCode || '').trim()
    ) {
      message.warning('请填写录入的供方编码');
      return false;
    }
  }
  return true;
}

async function submitWorkflowAction() {
  if (!form.id) return;
  saving.value = true;
  try {
    const payload = {
      id: Number(form.id),
      opinion: workflowOpinion.value,
      result: workflowResult.value,
    };
    switch (workflowAction.value) {
      case 'entry': {
        await entryOnboardingApply({
          id: Number(form.id),
          materialEntryCode: form.materialEntryCode,
          materialEntryOpinion:
            form.materialEntryOpinion || workflowOpinion.value,
          supplierRosterEntryCode: form.supplierRosterEntryCode,
          supplierRosterEntryOpinion:
            form.supplierRosterEntryOpinion || workflowOpinion.value,
        });

        break;
      }
      case 'purchase': {
        await purchaseReviewOnboardingApply(payload);

        break;
      }
      case 'purchaseIntake': {
        await purchaseIntakeOnboardingApply({
          id: Number(form.id),
          opinion: workflowOpinion.value,
          signUsers: signUsers.value,
          useDeptReviewerUserId: Number(form.useDeptReviewerUserId),
          useDeptReviewerUserName: form.useDeptReviewerUserName,
        });

        break;
      }
      case 'purchaseTransfer': {
        await purchaseTransferOnboardingApply({
          generalManagerUserId: form.generalManagerUserId,
          generalManagerUserName: form.generalManagerUserName,
          id: Number(form.id),
          materialEntryUserId: form.materialEntryUserId,
          materialEntryUserName: form.materialEntryUserName,
          opinion: workflowOpinion.value,
          supplierRosterEntryUserId: form.supplierRosterEntryUserId,
          supplierRosterEntryUserName: form.supplierRosterEntryUserName,
          transferAction: workflowTransferAction.value,
        });

        break;
      }
      case 'quality': {
        await qualityReviewOnboardingApply(payload);

        break;
      }
      case 'sign': {
        await signOnboardingApply({
          id: Number(form.id),
          opinion: workflowOpinion.value,
          result: workflowResult.value,
          signId: form.currentSignId || currentSign.value?.id,
        });

        break;
      }
      case 'tech': {
        await techReviewOnboardingApply(payload);

        break;
      }
      case 'useDept': {
        await useDeptReviewOnboardingApply(payload);

        break;
      }
      default: {
        await generalManagerReviewOnboardingApply(payload);
      }
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    emit('success');
    await modalApi.close();
  } finally {
    saving.value = false;
  }
}

function openSupplierPicker() {
  supplierModalApi
    .setData({
      keyword: form.supplierName || form.supplierCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'candidate',
      title: '选择供应商',
    })
    .open();
}

function handleSupplierSelect(row: Record<string, any>) {
  form.supplierId = normalizeId(row.supplierId);
  form.supplierCode = firstText(row.supplierCode);
  form.supplierName = firstText(row.supplierName);
}

function openProductBomPicker() {
  productBomPickerOpen.value = true;
}

function handleProductBomPickerClose() {
  productBomPickerOpen.value = false;
}

function handleProductBomPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  form.applicableProduct = firstText(
    row.productMaterialName,
    row.bomName,
    row.materialName,
    option.label,
  );
  productBomPickerOpen.value = false;
}

function openMaterialPicker(target: MaterialPickTarget) {
  activeMaterialPickTarget.value = target;
  materialPickerOpen.value = true;
}

function handleMaterialPickerClose() {
  materialPickerOpen.value = false;
}

function handleMaterialPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  const materialCode = firstText(row.materialCode, row.code);
  const materialName = firstText(row.materialName, row.name, option.label);
  if (activeMaterialPickTarget.value === 'customer') {
    form.customerMaterialCode = materialCode;
    form.customerMaterialName = materialName;
  } else {
    form.replacedMaterialCode = materialCode;
    form.replacedMaterialName = materialName;
  }
  materialPickerOpen.value = false;
}

function openUserPicker(target: UserPickTarget) {
  activeUserPickTarget.value = target;
  activeSignDept.value = '';
  const fieldMap = {
    generalManager: form.generalManagerUserId,
    materialEntry: form.materialEntryUserId,
    purchase: form.purchaseReviewerUserId,
    quality: form.qualityReviewerUserId,
    sign: undefined,
    supplierRoster: form.supplierRosterEntryUserId,
    tech: form.techReviewerUserId,
    useDept: form.useDeptReviewerUserId,
  };
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_USER_SELECT_MODAL_Z_INDEX,
      multiple: target === 'sign',
      title: userPickerTitle(target),
      userIds: fieldMap[target] ? [fieldMap[target]] : [],
    })
    .open();
}

function openSignUserPicker(deptName: string) {
  activeUserPickTarget.value = 'sign';
  activeSignDept.value = deptName;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_USER_SELECT_MODAL_Z_INDEX,
      multiple: true,
      title: `选择${deptName}会签人员`,
      userIds: signUsers.value
        .filter((item) => item.deptName === deptName)
        .map((item) => item.userId)
        .filter((id): id is number => !!id),
    })
    .open();
}

function userPickerTitle(target: UserPickTarget) {
  const titleMap: Record<UserPickTarget, string> = {
    generalManager: '选择总经理审核人',
    materialEntry: '选择物料编码录入人',
    purchase: '选择采购部审批人',
    quality: '选择品质部审批人',
    sign: '选择会签人员',
    supplierRoster: '选择供方清单录入人',
    tech: '选择技术研发部审批人',
    useDept: '选择使用部门负责人',
  };
  return titleMap[target];
}

function handleUserSelect(users: SystemUserApi.User[]) {
  if (activeUserPickTarget.value === 'sign') {
    const deptName = activeSignDept.value;
    signUsers.value = signUsers.value.filter(
      (item) => item.deptName !== deptName,
    );
    users.forEach((user) => {
      if (!user?.id) return;
      signUsers.value.push({
        deptCode: deptName,
        deptName,
        userId: Number(user.id),
        userName: firstText(user.nickname, user.username, user.name),
      });
    });
    return;
  }
  const user = users[0];
  if (!user?.id) {
    return;
  }
  const userId = Number(user.id);
  const userName = firstText(user.nickname, user.username, user.name);
  switch (activeUserPickTarget.value) {
    case 'materialEntry': {
      form.materialEntryUserId = userId;
      form.materialEntryUserName = userName;

      break;
    }
    case 'purchase': {
      form.purchaseReviewerUserId = userId;
      form.purchaseReviewerUserName = userName;

      break;
    }
    case 'quality': {
      form.qualityReviewerUserId = userId;
      form.qualityReviewerUserName = userName;

      break;
    }
    case 'supplierRoster': {
      form.supplierRosterEntryUserId = userId;
      form.supplierRosterEntryUserName = userName;

      break;
    }
    case 'tech': {
      form.techReviewerUserId = userId;
      form.techReviewerUserName = userName;

      break;
    }
    case 'useDept': {
      form.useDeptReviewerUserId = userId;
      form.useDeptReviewerUserName = userName;

      break;
    }
    default: {
      form.generalManagerUserId = userId;
      form.generalManagerUserName = userName;
    }
  }
}

function handleSignDeptChange() {
  signUsers.value = signUsers.value.filter((item) =>
    selectedSignDepts.value.includes(String(item.deptName)),
  );
}

function clearUser(target: UserPickTarget) {
  switch (target) {
    case 'materialEntry': {
      form.materialEntryUserId = undefined;
      form.materialEntryUserName = '';

      break;
    }
    case 'purchase': {
      form.purchaseReviewerUserId = undefined;
      form.purchaseReviewerUserName = '';

      break;
    }
    case 'quality': {
      form.qualityReviewerUserId = undefined;
      form.qualityReviewerUserName = '';

      break;
    }
    case 'supplierRoster': {
      form.supplierRosterEntryUserId = undefined;
      form.supplierRosterEntryUserName = '';

      break;
    }
    case 'tech': {
      form.techReviewerUserId = undefined;
      form.techReviewerUserName = '';

      break;
    }
    case 'useDept': {
      form.useDeptReviewerUserId = undefined;
      form.useDeptReviewerUserName = '';

      break;
    }
    default: {
      form.generalManagerUserId = undefined;
      form.generalManagerUserName = '';
    }
  }
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
      title: `审批日志 - ${form.applyNo || '导入申请'}`,
    })
    .open();
}

function openLogDrawer(mode: BusinessLogMode) {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
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

function importTypeText(value?: string) {
  return importTypeTextMap[value || ''] || displayValue(value);
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

function reviewResultText(value?: unknown) {
  if (value === 'PASS') return '同意';
  if (value === 'REJECT') return '不同意';
  return displayValue(value);
}

function reviewResultColor(value?: unknown) {
  if (value === '同意' || value === 'PASS') return 'success';
  if (value === '不同意' || value === 'REJECT') return 'error';
  return 'default';
}

function firstText(...values: unknown[]) {
  return values.map((item) => String(item || '').trim()).find(Boolean) || '';
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function todayText() {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function nowText() {
  const date = new Date();
  return `${todayText()} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}
</script>

<template>
  <Modal>
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
      title="选择适用产品"
      @close="handleProductBomPickerClose"
      @pick="handleProductBomPick"
    />
    <SupplierModal @select="handleSupplierSelect" />
    <UserModal @confirm="handleUserSelect" />
    <ProcessAuditModal />
    <BusinessLogDrawer
      v-model:open="logDrawerVisible"
      :items="businessLogs"
      :mode="logDrawerMode"
      title="导入申请流程日志"
    />
    <Spin :spinning="loading || saving" class="detail-spin">
      <div class="qms-ncr-detail">
        <div class="qms-ncr-toolbar">
          <div class="qms-ncr-toolbar__placeholder">
            <Button class="qms-ncr-toolbar-action" @click="closeDetail">
              <IconifyIcon icon="ant-design:arrow-left-outlined" />
              返回
            </Button>
          </div>
          <div class="qms-ncr-title-panel">
            <div class="qms-ncr-title-panel__name">新物料导入申请表</div>
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
              v-if="form.processInstanceId"
              class="qms-ncr-toolbar-action"
              @click="openAuditLog"
            >
              <IconifyIcon icon="ant-design:profile-outlined" />
            </Button>
            <Button
              v-if="form.logs?.length"
              class="qms-ncr-toolbar-action"
              @click="openLogDrawer('audit')"
            >
              <IconifyIcon icon="lucide:history" />
            </Button>
            <Button
              v-if="isReadonly && form.canEdit"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="switchToEdit"
            >
              <IconifyIcon icon="ant-design:edit-outlined" />
              编辑
            </Button>
            <Button
              v-if="!isReadonly"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="saveRecord"
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
              v-for="item in workflowActions"
              :key="item.action"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction(item.action)"
            >
              {{ item.label }}
            </Button>
            <Button class="qms-ncr-toolbar-action" @click="closeDetail">
              关闭
            </Button>
          </div>
        </div>

        <div class="detail-content">
          <div class="srm-import-modal-status">
            <Space :size="8">
              <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
              <span>{{ importTypeText(form.importType) }}</span>
              <span>{{ displayValue(form.supplierName) }}</span>
              <span>{{ displayValue(form.materialName) }}</span>
            </Space>
          </div>
          <div v-if="isReadonly" class="qms-exception-workbench">
            <SrmOnboardingApplyInlineDetail
              v-if="form.id"
              :key="detailRefreshKey"
              :id="form.id"
            />
          </div>
          <div v-else class="qms-exception-workbench">
            <div class="qms-exception-form">
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
                        {{ displayValue(form.applicantName) }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">申请时间</label>
                    <div class="erp-form-value">
                      <span class="qms-exception-readonly-value">
                        {{ displayValue(form.applyTime) }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">使用部门</label>
                    <div class="erp-form-value">
                      <AutoComplete
                        v-model:value="form.applyDept"
                        allow-clear
                        :options="applyDepartmentOptions"
                        placeholder="请选择或输入使用部门"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">供应商名称</label>
                    <div class="erp-form-value">
                      <Input.Search
                        v-model:value="form.supplierName"
                        allow-clear
                        placeholder="可手工填写或点击选择"
                        @search="openSupplierPicker"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">物料名称</label>
                    <div class="erp-form-value">
                      <Input
                        v-model:value="form.materialName"
                        allow-clear
                        placeholder="请输入物料名称"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">物料型号</label>
                    <div class="erp-form-value">
                      <Input
                        v-model:value="form.materialModel"
                        allow-clear
                        placeholder="请输入物料型号"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">适用产品</label>
                    <div class="erp-form-value">
                      <Input.Search
                        v-model:value="form.applicableProduct"
                        allow-clear
                        placeholder="可输入适用产品或点击右侧选择"
                        @search="openProductBomPicker"
                      >
                        <template #enterButton>
                          <Button>
                            <IconifyIcon icon="lucide:search" />
                          </Button>
                        </template>
                      </Input.Search>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">导入类型</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Radio.Group
                          v-model:value="form.importType"
                          :options="importTypeOptions"
                        />
                        <div
                          v-if="form.importType === 'REPLACE'"
                          class="srm-import-material-pair"
                        >
                          <Input.Search
                            v-model:value="form.replacedMaterialCode"
                            allow-clear
                            placeholder="旧物料编码"
                            @search="openMaterialPicker('replace')"
                          >
                            <template #enterButton>
                              <Button>
                                <IconifyIcon icon="lucide:search" />
                              </Button>
                            </template>
                          </Input.Search>
                          <Input
                            v-model:value="form.replacedMaterialName"
                            allow-clear
                            placeholder="旧物料名称"
                          />
                        </div>
                        <template
                          v-if="form.importType === 'CUSTOMER_SPECIFIED'"
                        >
                          <div class="srm-import-material-pair">
                            <Input.Search
                              v-model:value="form.customerMaterialCode"
                              allow-clear
                              placeholder="客户指定物料编码"
                              @search="openMaterialPicker('customer')"
                            >
                              <template #enterButton>
                                <Button>
                                  <IconifyIcon icon="lucide:search" />
                                </Button>
                              </template>
                            </Input.Search>
                            <Input
                              v-model:value="form.customerMaterialName"
                              allow-clear
                              placeholder="客户指定物料名称"
                            />
                          </div>
                        </template>
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">
                      供应商优势说明
                    </label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-model:value="form.supplierAdvantageDesc"
                        :rows="4"
                        placeholder="请输入供应商优势说明（技术、价格、服务等）"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">
                      其他补充说明
                    </label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-model:value="form.supplementDesc"
                        :rows="3"
                        placeholder="请输入其他补充说明"
                      />
                    </div>
                  </div>
                </div>
              </section>

              <section
                v-if="workflowRecordRows.length > 0 || form.processInstanceId"
                class="erp-basic-form"
              >
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>会签与办理记录</strong>
                  </div>
                </div>
                <div class="srm-import-reviewer-table-wrap">
                  <table class="srm-import-reviewer-table">
                    <thead>
                      <tr>
                        <th>节点/部门</th>
                        <th>办理人</th>
                        <th>办理结果</th>
                        <th>办理意见</th>
                        <th>办理时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="workflowRecordRows.length === 0">
                        <td colspan="5" class="text-center text-slate-400">
                          暂无会签与办理记录
                        </td>
                      </tr>
                      <tr v-for="item in workflowRecordRows" :key="item.key">
                        <td>{{ item.node }}</td>
                        <td>{{ item.handler }}</td>
                        <td>
                          <Tag
                            v-if="item.result !== '-'"
                            :color="
                              item.result === '已完成' ||
                              item.result === '已办理'
                                ? 'success'
                                : reviewResultColor(item.result)
                            "
                            class="!m-0"
                          >
                            {{ item.result }}
                          </Tag>
                          <span v-else>-</span>
                        </td>
                        <td>{{ item.opinion }}</td>
                        <td>{{ item.time }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section class="srm-import-attachment-section">
                <SrmAttachmentPanel
                  ref="attachmentPanelRef"
                  :biz-id="form.id"
                  :biz-type="BIZ_TYPE"
                  category-dict-type="mes_srm_onboarding_apply_attachment_category"
                  :default-category="DEFAULT_ATTACHMENT_CATEGORY"
                  :modal-z-index="SRM_NESTED_MODAL_Z_INDEX"
                  :mode="detailMode"
                />
              </section>
            </div>
          </div>
        </div>
      </div>
    </Spin>

    <AntModal
      v-model:open="workflowModalOpen"
      :confirm-loading="saving"
      :mask-closable="false"
      :title="actionTextMap[workflowAction]"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      ok-text="提交"
      @ok="confirmWorkflowAction"
    >
      <div class="srm-import-workflow-modal">
        <template v-if="workflowAction === 'purchaseIntake'">
          <div class="srm-import-workflow-field">
            <label>使用部门负责人</label>
            <div class="srm-import-user-pick-line">
              <span>{{ displayValue(form.useDeptReviewerUserName) }}</span>
              <Space :size="6">
                <Button size="small" @click="openUserPicker('useDept')">
                  选择人员
                </Button>
                <Button danger size="small" @click="clearUser('useDept')">
                  清空
                </Button>
              </Space>
            </div>
          </div>
          <div class="srm-import-workflow-field">
            <label>会签部门</label>
            <Checkbox.Group
              v-model:value="selectedSignDepts"
              :options="signDeptOptions"
              @change="handleSignDeptChange"
            />
          </div>
          <div class="srm-import-sign-pick-box">
            <div
              v-for="dept in selectedSignDepts"
              :key="dept"
              class="srm-import-sign-pick-row"
            >
              <span class="srm-import-sign-pick-row__dept">{{ dept }}</span>
              <span class="srm-import-sign-pick-row__users">
                {{
                  signUsers
                    .filter((item) => item.deptName === dept)
                    .map((item) => item.userName)
                    .join('、') || '未选择'
                }}
              </span>
              <Button size="small" @click="openSignUserPicker(String(dept))">
                选择人员
              </Button>
            </div>
          </div>
          <Input.TextArea
            v-model:value="workflowOpinion"
            :rows="3"
            placeholder="请输入采购部门办理意见（可选）"
          />
        </template>

        <template v-else-if="workflowAction === 'purchaseTransfer'">
          <Radio.Group
            v-model:value="workflowTransferAction"
            :options="transferActionOptions"
            button-style="solid"
            option-type="button"
          />
          <div
            v-if="workflowTransferAction === 'GENERAL_MANAGER'"
            class="srm-import-workflow-field"
          >
            <label>总经理审核人</label>
            <div class="srm-import-user-pick-line">
              <span>
                {{
                  form.generalManagerUserName
                    ? form.generalManagerUserName
                    : '未选择时按总经理角色带入'
                }}
              </span>
              <Space :size="6">
                <Button size="small" @click="openUserPicker('generalManager')">
                  选择人员
                </Button>
                <Button
                  danger
                  size="small"
                  @click="clearUser('generalManager')"
                >
                  清空
                </Button>
              </Space>
            </div>
          </div>
          <template v-if="workflowTransferAction === 'ASSIGN_ENTRY'">
            <div class="srm-import-workflow-field">
              <label>物料编码录入人</label>
              <div class="srm-import-user-pick-line">
                <span>{{ displayValue(form.materialEntryUserName) }}</span>
                <Space :size="6">
                  <Button size="small" @click="openUserPicker('materialEntry')">
                    选择人员
                  </Button>
                  <Button
                    danger
                    size="small"
                    @click="clearUser('materialEntry')"
                  >
                    清空
                  </Button>
                </Space>
              </div>
            </div>
            <div class="srm-import-workflow-field">
              <label>供方清单录入人</label>
              <div class="srm-import-user-pick-line">
                <span>{{
                  displayValue(form.supplierRosterEntryUserName)
                }}</span>
                <Space :size="6">
                  <Button
                    size="small"
                    @click="openUserPicker('supplierRoster')"
                  >
                    选择人员
                  </Button>
                  <Button
                    danger
                    size="small"
                    @click="clearUser('supplierRoster')"
                  >
                    清空
                  </Button>
                </Space>
              </div>
            </div>
          </template>
          <Input.TextArea
            v-model:value="workflowOpinion"
            :rows="3"
            placeholder="请输入采购转办意见（可选）"
          />
        </template>

        <template v-else-if="workflowAction === 'entry'">
          <div v-if="form.canMaterialEntry" class="srm-import-workflow-field">
            <label>录入物料编码</label>
            <Input
              v-model:value="form.materialEntryCode"
              allow-clear
              placeholder="请输入具体录入的物料编码"
            />
            <Input.TextArea
              v-model:value="form.materialEntryOpinion"
              :rows="2"
              placeholder="请输入物料编码录入说明（可选）"
            />
          </div>
          <div
            v-if="form.canSupplierRosterEntry"
            class="srm-import-workflow-field"
          >
            <label>录入供方编码</label>
            <Input
              v-model:value="form.supplierRosterEntryCode"
              allow-clear
              placeholder="请输入具体录入的供方编码"
            />
            <Input.TextArea
              v-model:value="form.supplierRosterEntryOpinion"
              :rows="2"
              placeholder="请输入供方清单录入说明（可选）"
            />
          </div>
        </template>

        <template v-else>
          <div
            v-if="workflowAction === 'sign' && currentSign"
            class="text-slate-600"
          >
            当前会签：{{ currentSign.deptName }} / {{ currentSign.userName }}
          </div>
          <Radio.Group v-model:value="workflowResult" button-style="solid">
            <Radio.Button value="PASS">同意</Radio.Button>
            <Radio.Button value="REJECT">不同意</Radio.Button>
          </Radio.Group>
          <Input.TextArea
            v-model:value="workflowOpinion"
            :rows="4"
            placeholder="请输入办理意见（可选）"
          />
        </template>
      </div>
    </AntModal>
  </Modal>
</template>

<style scoped>
.srm-import-modal-status {
  padding: 12px 20px 0;
}

.srm-import-type-line {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.srm-import-type-line :deep(.ant-radio-wrapper) {
  white-space: nowrap;
}

.srm-import-material-pair {
  display: grid;
  flex: 1 1 520px;
  min-width: min(100%, 520px);
  grid-template-columns: minmax(220px, 0.9fr) minmax(260px, 1.1fr);
  gap: 8px;
}

.srm-import-closure-line {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: minmax(260px, auto) minmax(180px, 320px);
  gap: 12px;
}

.srm-import-workflow-modal {
  display: grid;
  gap: 12px;
}

.srm-import-workflow-field {
  display: grid;
  gap: 8px;
}

.srm-import-workflow-field > label {
  color: #26364f;
  font-weight: 700;
}

.srm-import-user-pick-line {
  display: flex;
  min-height: 36px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 10px;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.srm-import-user-pick-line > span {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-import-sign-pick-box {
  display: grid;
  overflow: hidden;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
}

.srm-import-sign-pick-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 42px;
  padding: 6px 10px;
  border-bottom: 1px solid #edf2f7;
}

.srm-import-sign-pick-row:last-child {
  border-bottom: 0;
}

.srm-import-sign-pick-row__dept {
  color: #26364f;
  font-weight: 700;
}

.srm-import-sign-pick-row__users {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-import-reviewer-table-wrap {
  overflow-x: auto;
  border: 1px solid #d9e2ef;
}

.srm-import-reviewer-table {
  width: 100%;
  min-width: 1120px;
  border-collapse: collapse;
  background: #fff;
  table-layout: fixed;
}

.srm-import-reviewer-table th,
.srm-import-reviewer-table td {
  height: 42px;
  padding: 8px 12px;
  border-right: 1px solid #e5edf7;
  border-bottom: 1px solid #e5edf7;
  color: #334155;
  text-align: left;
  vertical-align: middle;
  white-space: nowrap;
}

.srm-import-reviewer-table th:nth-child(1),
.srm-import-reviewer-table td:nth-child(1) {
  width: 15%;
}

.srm-import-reviewer-table th:nth-child(2),
.srm-import-reviewer-table td:nth-child(2) {
  width: 13%;
}

.srm-import-reviewer-table th:nth-child(3),
.srm-import-reviewer-table td:nth-child(3) {
  width: 10%;
}

.srm-import-reviewer-table th:nth-child(4),
.srm-import-reviewer-table td:nth-child(4) {
  width: 34%;
  white-space: normal;
  word-break: break-word;
}

.srm-import-reviewer-table th:nth-child(5),
.srm-import-reviewer-table td:nth-child(5) {
  width: 18%;
}

.srm-import-reviewer-table th {
  background: #f8fafc;
  color: #1f2d3d;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .srm-import-material-pair,
  .srm-import-closure-line {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
