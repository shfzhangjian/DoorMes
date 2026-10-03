<script lang="ts" setup>
import type { SrmSupplierExitApprovalApi } from '#/api/mes/srm/supplier-exit-approval';
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
  DatePicker,
  Input,
  message,
  Radio,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';

import { getMaterialPage } from '#/api/mes/hc/material';
import { getSupplierCandidateSelectPage } from '#/api/mes/srm/supplier-candidate';
import {
  createSupplierExitApproval,
  entrySupplierExitApproval,
  generalManagerReviewSupplierExitApproval,
  getSupplierExitApproval,
  purchaseIntakeSupplierExitApproval,
  purchaseReviewSupplierExitApproval,
  purchaseTransferSupplierExitApproval,
  qualityReviewSupplierExitApproval,
  signSupplierExitApproval,
  submitSupplierExitApproval,
  techReviewSupplierExitApproval,
  updateSupplierExitApproval,
  useDeptReviewSupplierExitApproval,
} from '#/api/mes/srm/supplier-exit-approval';
import { getUserProfile } from '#/api/system/user/profile';
import { BusinessLogDrawer } from '#/components/business-log';
import { materialPickerConfig, PickerModal } from '#/components/picker';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '../../../shared/SrmReferenceSelectModal.vue';
import SrmSupplierExitApprovalInlineDetail from './inline-detail.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSupplierExitApprovalDetailModal' });

const emit = defineEmits<{ success: [] }>();
type DetailMode = 'create' | 'detail' | 'edit';
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
const BIZ_TYPE = 'SUPPLIER_EXIT_APPROVAL';
const DEFAULT_ATTACHMENT_CATEGORY = 'SUPPLIER_EXIT_APPROVAL_FORM';
const QUALIFIED_SUPPLIER_STATUS = 'QUALIFIED';
const SUPPLIER_EXIT_APPLY_DEPARTMENT_DICT =
  'mes_srm_supplier_exit_apply_department';

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
const signDeptOptions = [
  '品质部',
  '技术研发部',
  '生产部',
  '采购部',
  '市场部',
].map((item) => ({ label: item, value: item }));
const supplierExitApplyDepartmentFallbackOptions = [
  '采购部',
  '研发中心',
  '生产部',
  '品质部',
].map((item) => ({ label: item, value: item }));
const transferActionOptions = [
  { label: '发送总经理审核', value: 'GENERAL_MANAGER' },
  { label: '交办办理', value: 'ASSIGN_ENTRY' },
  { label: '完成归档', value: 'ARCHIVE' },
];
const replacementSupplierOptions = [
  { label: '已确定', value: 'CONFIRMED' },
  { label: '未确定', value: 'UNCONFIRMED' },
];
const stockStatusOptions = [
  { label: '剩余库存', value: 'REMAINING' },
  { label: '无剩余库存', value: 'NONE' },
];
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
const signUsers = ref<SrmSupplierExitApprovalApi.SignUser[]>([]);
const activeSignDept = ref('');
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const detailRefreshKey = ref(0);
const materialPickerOpen = ref(false);
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();

const form =
  reactive<SrmSupplierExitApprovalApi.SupplierExitApproval>(createEmptyForm());

const applyDepartmentOptions = computed(() => {
  const dictOptions = getDictOptions(SUPPLIER_EXIT_APPLY_DEPARTMENT_DICT).map(
    (option) => ({
      label: String(option.label ?? option.value),
      value: String(option.label ?? option.value),
    }),
  );
  return dictOptions.length > 0
    ? dictOptions
    : supplierExitApplyDepartmentFallbackOptions;
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
  [form.exitNo || '', form.currentNodeName || statusText(form.status)]
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
const materialPickerInitialFilters = computed(() => {
  return {
    materialCode: form.materialCode || undefined,
    materialName: form.materialName || undefined,
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
      node: '合格供方物料清单移除',
      opinion: displayValue(form.materialEntryOpinion),
      result: form.materialCodeCreated ? '已完成' : '-',
      time: displayValue(form.materialEntryHandleTime),
    },
    {
      handler: displayValue(form.supplierRosterEntryUserName),
      key: 'supplierRosterEntry',
      node: '库存/账务处理',
      opinion: displayValue(form.supplierRosterEntryOpinion),
      result: form.supplierRosterCreated ? '已完成' : '-',
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

function createEmptyForm(): SrmSupplierExitApprovalApi.SupplierExitApproval {
  return {
    materialCodeCreated: false,
    reasonBusinessAdjustment: false,
    reasonOther: false,
    reasonQualityDeliveryService: false,
    reasonSupplierInitiated: false,
    status: 'DRAFT',
    stockDisposalConsume: false,
    stockDisposalOther: false,
    stockDisposalReturn: false,
    stockDisposalScrap: false,
    supplierRosterCreated: false,
  };
}

function resetForm(
  record: Partial<SrmSupplierExitApprovalApi.SupplierExitApproval> = {},
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
    message.warning('缺少退出审批 ID');
    return;
  }
  loading.value = true;
  try {
    const detail = await getSupplierExitApproval(id);
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
      await updateSupplierExitApproval({ ...form });
    } else {
      const id = await createSupplierExitApproval({ ...form });
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
  if (
    !form.supplierId ||
    !String(form.supplierCode || '').trim() ||
    !String(form.supplierName || '').trim()
  ) {
    message.warning('请先选择合格供方');
    return false;
  }
  if (!String(form.materialCode || '').trim()) {
    message.warning('合格供方未带出物料编码，请先维护供应商物料信息');
    return false;
  }
  if (!String(form.materialName || '').trim()) {
    message.warning('合格供方未带出物料名称，请先维护物料主数据');
    return false;
  }
  return true;
}

function submitWithConfirm() {
  if (!form.id) {
    message.warning('请先保存退出审批');
    return;
  }
  AntModal.confirm({
    content: '确认提交退出审批并启动审批流程？',
    okText: '确认提交',
    title: '提交确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await submitSupplierExitApproval(Number(form.id));
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
    message.warning('缺少退出审批 ID');
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
      message.warning('请选择清单移除人和库存/账务处理人');
      return false;
    }
  }
  if (workflowAction.value === 'entry') {
    if (
      form.canMaterialEntry &&
      !String(form.materialCodeCompleteDate || '').trim()
    ) {
      message.warning('请填写合格供方物料清单移除完成日期');
      return false;
    }
    if (
      form.canSupplierRosterEntry &&
      !String(form.supplierRosterCompleteDate || '').trim()
    ) {
      message.warning('请填写库存/账务处理完成日期');
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
        await entrySupplierExitApproval({
          id: Number(form.id),
          materialCodeCompleteDate: form.materialCodeCompleteDate,
          materialEntryOpinion:
            form.materialEntryOpinion || workflowOpinion.value,
          supplierRosterCompleteDate: form.supplierRosterCompleteDate,
          supplierRosterEntryOpinion:
            form.supplierRosterEntryOpinion || workflowOpinion.value,
        });

        break;
      }
      case 'purchase': {
        await purchaseReviewSupplierExitApproval(payload);

        break;
      }
      case 'purchaseIntake': {
        await purchaseIntakeSupplierExitApproval({
          id: Number(form.id),
          opinion: workflowOpinion.value,
          signUsers: signUsers.value,
          useDeptReviewerUserId: Number(form.useDeptReviewerUserId),
          useDeptReviewerUserName: form.useDeptReviewerUserName,
        });

        break;
      }
      case 'purchaseTransfer': {
        await purchaseTransferSupplierExitApproval({
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
        await qualityReviewSupplierExitApproval(payload);

        break;
      }
      case 'sign': {
        await signSupplierExitApproval({
          id: Number(form.id),
          opinion: workflowOpinion.value,
          result: workflowResult.value,
          signId: form.currentSignId || currentSign.value?.id,
        });

        break;
      }
      case 'tech': {
        await techReviewSupplierExitApproval(payload);

        break;
      }
      case 'useDept': {
        await useDeptReviewSupplierExitApproval(payload);

        break;
      }
      default: {
        await generalManagerReviewSupplierExitApproval(payload);
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

function openSupplierPicker(keyword?: string) {
  const supplierCodeKeyword = firstText(keyword, form.supplierCode);
  supplierModalApi
    .setData({
      fixedSearchParams: {
        distinctSupplier: false,
        status: QUALIFIED_SUPPLIER_STATUS,
      },
      initialSearchParams: supplierCodeKeyword
        ? { supplierCode: supplierCodeKeyword }
        : undefined,
      keyword: supplierCodeKeyword ? undefined : form.supplierName,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'roster',
      title: '选择合格供方',
    })
    .open();
}

async function resolveSupplierByCode(value?: string) {
  const supplierCode = firstText(value, form.supplierCode);
  if (!supplierCode) {
    openSupplierPicker();
    return;
  }
  const result = await getSupplierCandidateSelectPage({
    distinctSupplier: false,
    pageNo: 1,
    pageSize: 2,
    status: QUALIFIED_SUPPLIER_STATUS,
    supplierCode,
  });
  const list = result.list || [];
  const exactRows = list.filter(
    (item) => firstText(item.supplierCode) === supplierCode,
  );
  let selectedRow = exactRows.length === 1 ? exactRows[0] : undefined;
  if (!selectedRow && list.length === 1) {
    selectedRow = list[0];
  }
  if (selectedRow) {
    await handleSupplierSelect(selectedRow);
    return;
  }
  if (list.length === 0) {
    message.warning('未在合格供方中找到该供应商编码');
  }
  openSupplierPicker(supplierCode);
}

function handleSupplierCodeInputChange() {
  form.supplierId = undefined;
  form.supplierName = '';
  clearSupplierMaterialFields();
}

async function handleSupplierSelect(row: Record<string, any>) {
  form.supplierId = normalizeId(row.supplierId);
  form.supplierCode = firstText(row.supplierCode);
  form.supplierName = firstText(row.supplierName);
  await hydrateMaterialBySupplier(row);
}

async function hydrateMaterialBySupplier(row: Record<string, any>) {
  const materialCode = firstText(row.materialCode);
  form.materialCode = materialCode;
  form.materialName = firstText(
    row.materialName,
    row.providedProduct,
    row.mainProducts,
    row.applicableProduct,
  );
  form.materialModel = firstText(row.materialModel, row.specModel, row.model);
  if (!materialCode) {
    return;
  }
  try {
    const materialPage = await getMaterialPage({
      materialCode,
      pageNo: 1,
      pageSize: 5,
    });
    const material =
      (materialPage.list || []).find(
        (item) => firstText(item.materialCode) === materialCode,
      ) || materialPage.list?.[0];
    if (material) {
      form.materialName = firstText(material.materialName, form.materialName);
      form.materialModel = firstText(
        material.specModel,
        material.modelCode,
        form.materialModel,
      );
    }
  } catch {
    message.warning('物料主数据查询失败，请确认物料编码');
  }
}

function openMaterialPicker() {
  if (!form.supplierId) {
    message.warning('请先选择合格供方');
    return;
  }
  materialPickerOpen.value = true;
}

function handleMaterialPickerClose() {
  materialPickerOpen.value = false;
}

function handleMaterialPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  const materialCode = firstText(row.materialCode, row.code);
  const materialName = firstText(row.materialName, row.name, option.label);
  form.materialCode = materialCode;
  form.materialName = materialName;
  form.materialModel = firstText(
    row.materialModel,
    row.specModel,
    row.specification,
    row.model,
    form.materialModel,
  );
  materialPickerOpen.value = false;
}

function clearSupplierMaterialFields() {
  form.materialCode = '';
  form.materialName = '';
  form.materialModel = '';
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
    materialEntry: '选择清单移除办理人',
    purchase: '选择采购部审批人',
    quality: '选择品质部审批人',
    sign: '选择会签人员',
    supplierRoster: '选择库存/账务处理人',
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
      title: `审批日志 - ${form.exitNo || '退出审批'}`,
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

function yesNo(value?: boolean) {
  if (value === true) return '是';
  if (value === false) return '否';
  return '-';
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
    <SupplierModal @select="handleSupplierSelect" />
    <UserModal @confirm="handleUserSelect" />
    <ProcessAuditModal />
    <BusinessLogDrawer
      v-model:open="logDrawerVisible"
      :items="businessLogs"
      :mode="logDrawerMode"
      title="退出审批流程日志"
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
            <div class="qms-ncr-title-panel__name">物料退出申请表</div>
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
              <span>{{ displayValue(form.supplierName) }}</span>
              <span>{{ displayValue(form.materialName) }}</span>
              <span>{{ displayValue(form.materialModel) }}</span>
            </Space>
          </div>
          <div v-if="isReadonly" class="qms-exception-workbench">
            <SrmSupplierExitApprovalInlineDetail
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
                    <label class="erp-form-label">申请部门</label>
                    <div class="erp-form-value">
                      <AutoComplete
                        v-model:value="form.applyDept"
                        allow-clear
                        :options="applyDepartmentOptions"
                        placeholder="请选择或输入申请部门"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">供应商代码</label>
                    <div class="erp-form-value">
                      <Input.Search
                        v-model:value="form.supplierCode"
                        allow-clear
                        placeholder="输入供应商编码后回车/点击搜索"
                        @change="handleSupplierCodeInputChange"
                        @search="resolveSupplierByCode"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">供应商名称</label>
                    <div class="erp-form-value">
                      <Input.Search
                        v-model:value="form.supplierName"
                        readonly
                        placeholder="由合格供方带出或点击选择"
                        @search="() => openSupplierPicker()"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">物料编码</label>
                    <div class="erp-form-value">
                      <Input.Search
                        v-model:value="form.materialCode"
                        readonly
                        placeholder="由合格供方带出"
                        @search="openMaterialPicker"
                      >
                        <template #enterButton>
                          <Button>
                            <IconifyIcon icon="lucide:search" />
                          </Button>
                        </template>
                      </Input.Search>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">物料名称</label>
                    <div class="erp-form-value">
                      <Input
                        v-model:value="form.materialName"
                        readonly
                        placeholder="由物料主数据带出"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">物料型号</label>
                    <div class="erp-form-value">
                      <Input
                        v-model:value="form.materialModel"
                        readonly
                        placeholder="由物料主数据带出"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">退出原因</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Checkbox
                          v-model:checked="form.reasonQualityDeliveryService"
                        >
                          质量/交期/服务持续性不满足要求（附绩效报告）
                        </Checkbox>
                        <Checkbox
                          v-model:checked="form.reasonSupplierInitiated"
                        >
                          供应商主动退出
                        </Checkbox>
                        <Checkbox
                          v-model:checked="form.reasonBusinessAdjustment"
                        >
                          公司业务调整（产品停产/项目取消）
                        </Checkbox>
                        <Checkbox v-model:checked="form.reasonOther">
                          其他
                        </Checkbox>
                        <Input
                          v-if="form.reasonOther"
                          v-model:value="form.reasonOtherText"
                          allow-clear
                          class="srm-exit-inline-input"
                          placeholder="请注明其他退出原因"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">
                      退出原因详细说明
                    </label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-model:value="form.exitReasonDesc"
                        :rows="4"
                        placeholder="请填写具体原因，如质量、交期、服务等问题，或业务调整背景"
                      />
                    </div>
                  </div>
                </div>
              </section>

              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>退出影响评估</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">替代供应商</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Radio.Group
                          v-model:value="form.replacementSupplierStatus"
                          :options="replacementSupplierOptions"
                        />
                        <Input
                          v-model:value="form.replacementSupplierName"
                          allow-clear
                          class="srm-exit-inline-input srm-exit-inline-input--wide"
                          placeholder="请输入替代供应商名称"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">当前物料库存</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Radio.Group
                          v-model:value="form.stockStatus"
                          :options="stockStatusOptions"
                        />
                        <Input
                          v-model:value="form.remainingStockDesc"
                          allow-clear
                          class="srm-exit-inline-input srm-exit-inline-input--wide"
                          placeholder="请输入库存数量或说明"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">库存处理方式</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Checkbox v-model:checked="form.stockDisposalReturn">
                          需退换货
                        </Checkbox>
                        <Checkbox v-model:checked="form.stockDisposalScrap">
                          报废处理
                        </Checkbox>
                        <Checkbox v-model:checked="form.stockDisposalConsume">
                          消耗完毕
                        </Checkbox>
                        <Checkbox v-model:checked="form.stockDisposalOther">
                          其他
                        </Checkbox>
                        <Input
                          v-if="form.stockDisposalOther"
                          v-model:value="form.stockDisposalOtherText"
                          allow-clear
                          class="srm-exit-inline-input"
                          placeholder="请注明库存处理方式"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">合同终止条款</label>
                    <div class="erp-form-value">
                      <div class="srm-import-type-line">
                        <Checkbox v-model:checked="form.contractPaymentCleared">
                          票款全部结清
                        </Checkbox>
                        <Input
                          v-model:value="form.unpaidAmount"
                          allow-clear
                          class="srm-exit-amount-input"
                          placeholder="尚有未结货款"
                        />
                        <span>元</span>
                        <Input
                          v-model:value="form.uninvoicedAmount"
                          allow-clear
                          class="srm-exit-amount-input"
                          placeholder="尚有未开发票"
                        />
                        <span>元</span>
                      </div>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">
                      其他业务/生产风险影响
                    </label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-model:value="form.businessRiskImpact"
                        :rows="3"
                        placeholder="请输入其他业务或生产风险影响"
                      />
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">
                      影响评估详细说明
                    </label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-model:value="form.impactDesc"
                        :rows="4"
                        placeholder="请填写退出影响评估详细说明"
                      />
                    </div>
                  </div>
                </div>
              </section>

              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>后续闭环要求</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">清单移除</label>
                    <div class="erp-form-value srm-import-closure-line">
                      <span>
                        合格供方物料清单移除完成：{{
                          yesNo(form.materialCodeCreated)
                        }}
                      </span>
                      <span>
                        完成日期：{{
                          displayValue(form.materialCodeCompleteDate)
                        }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label">库存/账务</label>
                    <div class="erp-form-value srm-import-closure-line">
                      <span>
                        库存/账务处理完成：{{
                          yesNo(form.supplierRosterCreated)
                        }}
                      </span>
                      <span>
                        完成日期：{{
                          displayValue(form.supplierRosterCompleteDate)
                        }}
                      </span>
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
                  category-dict-type="mes_srm_supplier_exit_approval_attachment_category"
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
              <label>清单移除办理人</label>
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
              <label>库存/账务处理人</label>
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
            <label>合格供方物料清单移除完成日期</label>
            <DatePicker
              v-model:value="form.materialCodeCompleteDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
            <Input.TextArea
              v-model:value="form.materialEntryOpinion"
              :rows="2"
              placeholder="请输入清单移除办理说明（可选）"
            />
          </div>
          <div
            v-if="form.canSupplierRosterEntry"
            class="srm-import-workflow-field"
          >
            <label>库存/账务处理完成日期</label>
            <DatePicker
              v-model:value="form.supplierRosterCompleteDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
            <Input.TextArea
              v-model:value="form.supplierRosterEntryOpinion"
              :rows="2"
              placeholder="请输入库存/账务处理说明（可选）"
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

.srm-exit-inline-input {
  flex: 1 1 320px;
  min-width: min(100%, 280px);
}

.srm-exit-inline-input--wide {
  flex-basis: 520px;
}

.srm-exit-amount-input {
  width: 180px;
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
