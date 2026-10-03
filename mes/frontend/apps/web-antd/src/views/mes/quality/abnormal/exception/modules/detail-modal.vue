<script lang="ts" setup>
import type { MesExceptionApi } from '#/api/mes/quality/abnormal/exception';
import type { SystemDeptApi } from '#/api/system/dept';
import type { SystemUserApi } from '#/api/system/user';

import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';

import { useVbenModal } from '@vben/common-ui';
import { BpmNodeTypeEnum, BpmTaskStatusEnum } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';
import { getDictOptions } from '@vben/hooks';
import { useUserStore } from '@vben/stores';
import { handleTree } from '@vben/utils';

import {
  Button,
  Checkbox,
  DatePicker,
  Dropdown,
  Form,
  Input,
  Menu,
  message,
  Modal as AntModal,
  Radio,
  Select,
  Spin,
  Table,
  Tag,
  Tooltip,
  TreeSelect,
} from 'ant-design-vue';

import {
  closeExceptionEvent,
  confirmExceptionGroupTaskMember,
  createExceptionEvent,
  delegateExceptionGroupTaskMember,
  getExceptionEvent,
  handleExceptionEvent,
  reviewExceptionGroupTask,
  saveExceptionGroupTasks,
  returnExceptionEvent,
} from '#/api/mes/quality/abnormal/exception';
import { getSimpleDeptList } from '#/api/system/dept';
import {
  BusinessLogDrawer,
  type BusinessLogMode,
} from '#/components/business-log';
import { FileUpload } from '#/components/upload';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import { QMS_EXCEPTION_DICT } from '../data';
import NcrSelectModal from './ncr-select-modal.vue';

const emit = defineEmits(['success']);

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const RELATION_TYPE_NCR = 'NCR';
const RELATION_TYPE_RAW_MATERIAL_NCR = 'RAW_MATERIAL_NCR';
const ATTACHMENT_RELATION_TYPES = {
  confirmRelated: 'ATTACHMENT_CONFIRM_RELATED',
  containment: 'ATTACHMENT_CONTAINMENT',
  effect: 'ATTACHMENT_EFFECT',
  result: 'ATTACHMENT_RESULT',
  rootCause: 'ATTACHMENT_ROOT_CAUSE',
} as const;
const ATTACHMENT_RELATION_TYPE_LIST = Object.values(ATTACHMENT_RELATION_TYPES);
type ExceptionModalData = MesExceptionApi.ExceptionRecord & {
  defaults?: Partial<MesExceptionApi.ExceptionRecord>;
  isNew?: boolean;
};
const TASK_TYPE_INVESTIGATION = 'INVESTIGATION_GROUP';
const TASK_TYPE_ROOT_CAUSE = 'ROOT_CAUSE_PREVENTIVE';
const MEMBER_CONFIRM_PENDING = 'PENDING';
const MEMBER_CONFIRM_SUBMITTED = 'SUBMITTED';
const MEMBER_CONFIRM_CONFIRMED = 'CONFIRMED';
const MEMBER_CONFIRM_DISAGREED = 'DISAGREED';
const MEMBER_CONFIRM_EXPIRED = 'EXPIRED';
const MEMBER_CONFIRM_ACTION_CONFIRM = 'CONFIRM';
const MEMBER_CONFIRM_ACTION_DISAGREE = 'DISAGREE';
const durationClock = ref(Date.now());
const durationClockTimer = setInterval(() => {
  durationClock.value = Date.now();
}, 60_000);

onBeforeUnmount(() => {
  clearInterval(durationClockTimer);
});
const TASK_REVIEW_ACTION_ACCEPT = 'ACCEPT';
const TASK_REVIEW_ACTION_RETURN = 'RETURN';
type ExceptionTaskType =
  | typeof TASK_TYPE_INVESTIGATION
  | typeof TASK_TYPE_ROOT_CAUSE;
type TaskReviewAction =
  | typeof MEMBER_CONFIRM_ACTION_CONFIRM
  | typeof MEMBER_CONFIRM_ACTION_DISAGREE
  | typeof TASK_REVIEW_ACTION_ACCEPT
  | typeof TASK_REVIEW_ACTION_RETURN;
type AuditUser = {
  avatar?: string;
  id?: number | string;
  nickname: string;
};
type AuditTask = {
  assigneeUser: AuditUser;
  id: number | string;
  reason?: string;
  status: BpmTaskStatusEnum;
};
type AuditNode = {
  endTime?: string;
  id: string;
  name: string;
  nodeType: BpmNodeTypeEnum;
  startTime?: string;
  status: BpmTaskStatusEnum;
  tasks: AuditTask[];
};
const attachmentAcceptTypes = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'png',
  'jpg',
  'jpeg',
  'zip',
  'rar',
];
const isNew = ref(false);
const loading = ref(false);
const formRef = ref();
const formData = ref<MesExceptionApi.ExceptionRecord>({});
const userStore = useUserStore();
const router = useRouter();
const deptOptions = ref<SystemDeptApi.Dept[]>([]);
const activeUserSelectTarget = ref<{
  index?: number;
  memberIndex?: number;
  memberId?: number;
  taskIndex?: number;
  taskId?: number;
  type:
    | 'actionOwner'
    | 'confirmer'
    | 'copyUsers'
    | 'discoverer'
    | 'groupRootCauseOwner'
    | 'groupTaskDelegate'
    | 'groupTaskMember'
    | 'groupTaskMembersMulti'
    | 'nextHandler'
    | 'qaConfirmer';
}>();
const confirmAttachmentUrls = ref<string[]>([]);
const containmentAttachmentUrls = ref<string[]>([]);
const rootCauseAttachmentUrls = ref<string[]>([]);
const resultAttachmentUrls = ref<string[]>([]);
const effectAttachmentUrls = ref<string[]>([]);
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const approvalForm = ref({
  copyUserIds: [] as number[],
  copyUserNames: [] as string[],
  nextHandlerUserId: undefined as number | undefined,
  nextHandlerUserName: '',
  opinion: '',
});
const resultUploadCopyUsersInitialized = ref(false);
const taskReplyOpen = ref(false);
const taskReplyTarget = ref<MesExceptionApi.GroupTask>();
const taskReplyForm = ref<MesExceptionApi.GroupTask>({});
const investigationSignConfirmOpen = ref(false);
const investigationSignConfirmTarget = ref<MesExceptionApi.GroupTask>();
const investigationSignConfirmMember = ref<MesExceptionApi.GroupTaskMember>();
const investigationSignConfirmOpinion = ref('');
const taskReviewOpen = ref(false);
const taskReviewTarget = ref<MesExceptionApi.GroupTask>();
const taskReviewAction = ref<TaskReviewAction>(MEMBER_CONFIRM_ACTION_CONFIRM);
const taskReviewMode = ref<'DISPATCHER' | 'MEMBER'>('MEMBER');
const taskReviewOpinion = ref('');
const returnModalOpen = ref(false);
const returnOpinion = ref('');
const returnSubmitting = ref(false);
const expandedTaskMap = ref<Record<string, boolean>>({});
const investigationSummaryExpandedMap = ref<Record<string, boolean>>({});
const CONFIRM_ACTION_CONTINUE = 'HANDLE';
const CONFIRM_ACTION_MISREPORT_CLOSE = 'MISREPORT_CLOSE';
const confirmActionCode = ref(CONFIRM_ACTION_CONTINUE);
const confirmActionOptions = [
  { label: '继续处理', value: CONFIRM_ACTION_CONTINUE },
  { label: '误报关闭', value: CONFIRM_ACTION_MISREPORT_CLOSE },
];

const taskReviewIsPositive = computed(
  () =>
    taskReviewAction.value === MEMBER_CONFIRM_ACTION_CONFIRM ||
    taskReviewAction.value === TASK_REVIEW_ACTION_ACCEPT,
);

const taskReviewModalTitle = computed(() => {
  if (taskReviewMode.value === 'DISPATCHER') {
    return taskReviewAction.value === TASK_REVIEW_ACTION_ACCEPT
      ? '发起人确认任务'
      : '退回填写人重写';
  }
  return taskReviewAction.value === MEMBER_CONFIRM_ACTION_CONFIRM
    ? '确认任务资料'
    : '不同意任务资料';
});

const taskReviewOkText = computed(() =>
  taskReviewIsPositive.value ? '确认' : '提交',
);

const returnTargetName = computed(() => getReturnTargetByStatus().name);

const taskReviewPlaceholder = computed(() => {
  if (taskReviewMode.value === 'DISPATCHER') {
    return taskReviewAction.value === TASK_REVIEW_ACTION_ACCEPT
      ? '填写接收备注，可为空'
      : '请填写退回原因';
  }
  return taskReviewAction.value === MEMBER_CONFIRM_ACTION_CONFIRM
    ? '填写确认备注，可为空'
    : '请填写不同意意见';
});

const MAX_VISIBLE_TOOLBAR_ACTIONS = 5;
const TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW = MAX_VISIBLE_TOOLBAR_ACTIONS - 2;
const exceptionTypeFallbackOptions = [
  { label: '生产异常', value: 'PRODUCTION' },
  { label: '工艺异常', value: 'PROCESS' },
  { label: '设备异常', value: 'EQUIPMENT' },
  { label: '厂务系统异常', value: 'UTILITY_SYSTEM' },
  { label: 'IT系统异常', value: 'IT_SYSTEM' },
  { label: '安全事故', value: 'SAFETY_ACCIDENT' },
  { label: '检验异常', value: 'INSPECTION' },
  { label: '其它', value: 'OTHER' },
  { label: '误报', value: 'MISREPORT' },
];
const exceptionLevelFallbackOptions = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'MAJOR' },
  { label: '严重', value: 'CRITICAL' },
];

interface ToolbarActionItem {
  danger?: boolean;
  key: string;
  label: string;
  onClick: () => Promise<void> | void;
  title: string;
  type?: 'default' | 'primary';
}

const [NcrModal, ncrModalApi] = useVbenModal({
  connectedComponent: NcrSelectModal,
});
const [UserSelectModalComp, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});
const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});

const deptTreeData = computed(() =>
  handleTree(
    deptOptions.value.map((dept) => ({ ...dept })),
  ) as SystemDeptApi.Dept[],
);

const currentUserId = computed(() =>
  normalizeNumber(userStore.userInfo?.id ?? userStore.userInfo?.userId),
);

const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '当前用户',
);

const currentDeptId = computed(() => normalizeNumber(userStore.userInfo?.deptId));

const currentUserCode = computed(() =>
  String((userStore.userInfo as any)?.username || currentUserId.value || ''),
);

const currentDeptCode = computed(() =>
  currentDeptId.value ? String(currentDeptId.value) : '',
);

const currentDeptName = computed(() => {
  const deptId = currentDeptId.value;
  if (deptId) {
    const dept = deptOptions.value.find((item) => item.id === deptId);
    if (dept?.name) {
      return dept.name;
    }
  }
  return userStore.userInfo?.deptName || '当前部门';
});

const isTerminalStatus = computed(() =>
  ['CANCELLED', 'CLOSED'].includes(formData.value.status || ''),
);

const isCurrentHandler = computed(
  () =>
    !!currentUserId.value &&
    Number(formData.value.currentHandlerUserId) === currentUserId.value,
);

const legacyTodoContext = computed(() => formData.value.tabType === 'todo');

const canOperateMainFlow = computed(
  () =>
    (formData.value.canHandle === true &&
      formData.value.currentUserTaskTodo !== true) ||
    (formData.value.canHandle === undefined &&
      legacyTodoContext.value &&
      isCurrentHandler.value),
);

const canMainFlowOperateStatus = computed(
  () => !['CONTAINMENT_SIGN', 'ROOT_CAUSE_SIGN'].includes(formData.value.status || ''),
);

const canHandleMainFlow = computed(
  () =>
    !isNew.value &&
    !isTerminalStatus.value &&
    canMainFlowOperateStatus.value &&
    canOperateMainFlow.value,
);

const canFlow = computed(() => canHandleMainFlow.value);

const isQaClosureStep = computed(() =>
  ['QA_CLOSURE', 'VERIFYING'].includes(formData.value.status || ''),
);

const canEditDiscoverStep = computed(
  () =>
    isNew.value ||
    (!isTerminalStatus.value &&
      canOperateMainFlow.value &&
      ['RETURNED'].includes(formData.value.status || '')),
);

const canEditConfirmStep = computed(
  () => !isNew.value && canFlow.value && formData.value.status === 'CONFIRMING',
);

const canEditContainmentOwnerStep = computed(
  () => !isNew.value && canFlow.value && formData.value.status === 'CONTAINMENT',
);

const canEditResponsibilityStep = computed(
  () =>
    !isNew.value &&
    canFlow.value &&
    formData.value.status === 'RESPONSIBILITY_CONFIRM',
);

const canEditRootCauseOwnerStep = computed(
  () => !isNew.value && canFlow.value && formData.value.status === 'ROOT_CAUSE',
);

const canEditResultUploaderAssignStep = computed(
  () =>
    !isNew.value &&
    canFlow.value &&
    formData.value.status === 'RESULT_UPLOADER_ASSIGN',
);

const canEditResultUploadStep = computed(
  () => !isNew.value && canFlow.value && formData.value.status === 'RESULT_UPLOAD',
);

const canEditQaClosureStep = computed(
  () => !isNew.value && canFlow.value && isQaClosureStep.value,
);

const confirmStepDisabled = computed(() => !canEditConfirmStep.value);
const relatedNcrLockMessage = '关联NCR只能在确认步骤由确认人选择，确认提交后不允许修改';
const canEditContainmentDeadline = computed(() => canEditConfirmStep.value);
const canEditConfirmRelatedAttachment = computed(() => canEditConfirmStep.value);
const canEditContainmentAttachment = computed(() => canEditContainmentOwnerStep.value);

const canDispatchGroupTasks = computed(() => canFlow.value);

const canMaintainGroupTasks = computed(() => isNew.value || canDispatchGroupTasks.value);

const canMaintainInvestigationTasks = computed(() => canEditContainmentOwnerStep.value);

const canMaintainRootCauseTasks = computed(() => canEditRootCauseOwnerStep.value);

const visibleStepIndex = computed(() => {
  if (isNew.value) {
    return 1;
  }
  const status = formData.value.status || '';
  if (['QA_CLOSURE', 'VERIFYING', 'CLOSED'].includes(status)) {
    return 10;
  }
  if (status === 'RESULT_UPLOAD') {
    return 9;
  }
  if (status === 'RESULT_UPLOADER_ASSIGN') {
    return 8;
  }
  if (status === 'ROOT_CAUSE_SIGN') {
    return 7;
  }
  if (status === 'ROOT_CAUSE') {
    return 6;
  }
  if (status === 'RESPONSIBILITY_CONFIRM') {
    return 5;
  }
  if (status === 'CONTAINMENT_SIGN') {
    return 4;
  }
  if (status === 'CONTAINMENT') {
    return 3;
  }
  if (status === 'CONFIRMING') {
    return 2;
  }
  return 1;
});

const showConfirmStep = computed(() => visibleStepIndex.value >= 2);
const showContainmentStep = computed(() => visibleStepIndex.value >= 3);
const showResponsibilityStep = computed(() => visibleStepIndex.value >= 5);
const showRootCauseStep = computed(() => visibleStepIndex.value >= 6);
const showResultUploadStep = computed(() => visibleStepIndex.value >= 8);
const showQaClosureStep = computed(() => visibleStepIndex.value >= 10);
const showInvestigationAssignmentRows = computed(
  () => canMaintainInvestigationTasks.value || visibleStepIndex.value >= 2,
);
const collapseInvestigationSignDetail = computed(() => visibleStepIndex.value >= 4);
const autoExpandInvestigationSignSummary = computed(() => visibleStepIndex.value >= 5);

const investigationTasks = computed(() =>
  (formData.value.groupTasks || []).filter(
    (task) => normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION,
  ),
);

const investigationTaskReturnOpinions = computed(() =>
  investigationTasks.value
    .map((task) => ({
      key: getTaskKey(task),
      ...getTaskReturnInfo(task),
    }))
    .filter((item) => item.opinion),
);
const showInvestigationReturnOpinions = computed(
  () => visibleStepIndex.value < 5 && investigationTaskReturnOpinions.value.length > 0,
);

const rootCauseTasks = computed(() =>
  (formData.value.groupTasks || []).filter(
    (task) => normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE,
  ),
);

const currentUserPendingSubmitTasks = computed(() =>
  (formData.value.groupTasks || []).filter((task) => canSubmitGroupTask(task)),
);

const currentUserPendingConfirmTasks = computed(() =>
  (formData.value.groupTasks || []).filter((task) => canConfirmGroupTask(task)),
);

const currentUserTaskTodoText = computed(() => {
  const tasks =
    currentUserPendingSubmitTasks.value.length > 0
      ? currentUserPendingSubmitTasks.value
      : currentUserPendingConfirmTasks.value;
  if (tasks.length === 0) {
    return '';
  }
  const hasConfirmTask =
    currentUserPendingSubmitTasks.value.length === 0 &&
    currentUserPendingConfirmTasks.value.length > 0;
  const hasInvestigation = tasks.some(
    (task) => normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION,
  );
  const hasRootCause = tasks.some(
    (task) => normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE,
  );
  const label = hasConfirmTask
    ? getTaskConfirmTodoActionLabel(hasInvestigation, hasRootCause)
    : getTaskTodoActionLabel(hasInvestigation, hasRootCause);
  return tasks.length > 1 ? `${label}（${tasks.length}项）` : label;
});

const primaryNcrRelation = computed(() => {
  const relations = formData.value.relations || [];
  return (
    relations.find(
      (relation) =>
        relation.primaryFlag &&
        [RELATION_TYPE_NCR, RELATION_TYPE_RAW_MATERIAL_NCR].includes(
          relation.relationType || '',
        ),
    ) ||
    relations.find(
      (relation) =>
        [RELATION_TYPE_NCR, RELATION_TYPE_RAW_MATERIAL_NCR].includes(
          relation.relationType || '',
        ) &&
        (!formData.value.relatedNcrNo ||
          relation.relatedObjectNo === formData.value.relatedNcrNo),
    )
  );
});
const relatedNcrLocked = computed(
  () => !canEditConfirmStep.value || !!primaryNcrRelation.value,
);
const linkedNcrType = computed(() =>
  primaryNcrRelation.value?.relationType === RELATION_TYPE_RAW_MATERIAL_NCR ||
  formData.value.isRelatedProduct === false
    ? RELATION_TYPE_RAW_MATERIAL_NCR
    : RELATION_TYPE_NCR,
);

const exceptionTypeOptions = computed(() =>
  mergeDictOptions(QMS_EXCEPTION_DICT.type, exceptionTypeFallbackOptions),
);

const exceptionLevelOptions = computed(() =>
  mergeDictOptions(QMS_EXCEPTION_DICT.level, exceptionLevelFallbackOptions),
);

const exceptionLevelLabel = computed(() =>
  getOptionLabel(exceptionLevelOptions.value, formData.value.exceptionLevel),
);

const effectAttachmentFlag = computed(() =>
  effectAttachmentUrls.value.length > 0 ? '有' : '无',
);

const investigationAssignmentTask = computed(() =>
  getAssignmentTask(TASK_TYPE_INVESTIGATION),
);

const investigationTaskDeadline = computed<string | undefined>({
  get: () => investigationAssignmentTask.value?.containmentDeadline,
  set: (value) => {
    ensureAssignmentTask(TASK_TYPE_INVESTIGATION).containmentDeadline = value || undefined;
  },
});

const containmentDeadlineValue = computed<string | undefined>({
  get: () => investigationTaskDeadline.value || formData.value.containmentDeadline,
  set: (value) => {
    const deadline = value || undefined;
    formData.value.containmentDeadline = deadline;
  },
});

const rootCauseAssignmentTask = computed(() =>
  getAssignmentTask(TASK_TYPE_ROOT_CAUSE),
);

const responsibilityDeptDisplayName = computed(
  () =>
    formData.value.actionDeptName ||
    findDeptName(formData.value.actionDeptId) ||
    rootCauseAssignmentTask.value?.containmentDeptName ||
    '',
);

const responsibilityDeptOptions = computed(() =>
  deptOptions.value
    .filter((dept) => dept.id && dept.name)
    .map((dept) => ({
      label: dept.name,
      value: dept.id!,
    })),
);

const responsibilityDeptValues = computed<number[]>({
  get: () => {
    const deptIds = splitDeptNames(responsibilityDeptDisplayName.value)
      .map((name) => findDeptIdByName(name))
      .filter((id): id is number => !!id);
    if (deptIds.length > 0) {
      return uniqueDeptIds(deptIds);
    }
    return formData.value.actionDeptId ? [formData.value.actionDeptId] : [];
  },
  set: (values) => {
    const deptIds = uniqueDeptIds(values);
    const deptNames = deptIds
      .map((id) => findDeptName(id))
      .filter(Boolean);
    const deptName = deptNames.join('、');
    formData.value.actionDeptId = deptIds.length === 1 ? deptIds[0] : undefined;
    formData.value.actionDeptName = deptName || undefined;
    syncRootCauseAssignmentTaskMeta(getAssignmentTask(TASK_TYPE_ROOT_CAUSE), deptName);
  },
});

const rootCauseTaskDeadline = computed<string | undefined>({
  get: () => rootCauseAssignmentTask.value?.containmentDeadline,
  set: (value) => {
    const task = ensureAssignmentTask(TASK_TYPE_ROOT_CAUSE);
    task.containmentDeadline = value || undefined;
    syncRootCauseAssignmentTaskMeta(task);
  },
});

function splitDeptNames(value?: string) {
  return (value || '')
    .split(/[、,，;]/)
    .map((name) => name.trim())
    .filter(Boolean);
}

function uniqueDeptNames(values?: Array<number | string>) {
  return [
    ...new Set(
      (values || [])
        .map((value) => String(value || '').trim())
        .filter(Boolean),
    ),
  ];
}

function uniqueDeptIds(values?: Array<number | string>) {
  return [
    ...new Set(
      (values || [])
        .map((value) => normalizeNumber(value))
        .filter((value): value is number => !!value),
    ),
  ];
}

const [Modal, modalApi] = useVbenModal({
  title: '',
  class: 'qms-product-event-detail-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  header: false,
  fullscreen: true,
  fullscreenButton: false,
  showCancelButton: false,
  showConfirmButton: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<ExceptionModalData>();
    isNew.value = !!data.isNew;
    approvalForm.value = {
      copyUserIds: [],
      copyUserNames: [],
      nextHandlerUserId: undefined,
      nextHandlerUserName: '',
      opinion: '',
    };
    resultUploadCopyUsersInitialized.value = false;
    confirmActionCode.value = CONFIRM_ACTION_CONTINUE;
    resetTaskDialogs();
    expandedTaskMap.value = {};
    investigationSummaryExpandedMap.value = {};
    await loadDeptOptions();
    if (isNew.value) {
      formData.value = {
        discoverDeptCode: currentDeptCode.value,
        discoverDeptId: currentDeptId.value,
        discoverDeptName: currentDeptName.value,
        discoverTime: formatDateTime(),
        discovererCode: currentUserCode.value,
        discovererId: currentUserId.value,
        discovererName: currentUserName.value,
        groupTasks: [],
        teamMembers: [],
        ...data.defaults,
      };
      syncAttachmentUrls(formData.value.relations);
      return;
    }
    if (data.id) {
      await loadDetail(data.id, data.tabType);
      ensureStepDefaultValues();
    }
  },
});

const toolbarActions = computed<ToolbarActionItem[]>(() => {
  const actions: ToolbarActionItem[] = [];
  if (!isNew.value && !canFlow.value && currentUserPendingSubmitTasks.value.length > 0) {
    actions.push({
      key: 'submitTask',
      label: currentUserTaskTodoText.value || '处理子任务',
      onClick: () => handleSubmitGroupTask(currentUserPendingSubmitTasks.value[0]!),
      title: '办理当前分配给我的异常子任务',
      type: 'primary',
    });
  } else if (
    !isNew.value &&
    !canFlow.value &&
    currentUserPendingConfirmTasks.value.length > 0
  ) {
    actions.push({
      key: 'confirmGroupTask',
      label: currentUserTaskTodoText.value || '确认小组提交',
      onClick: () =>
        handleConfirmGroupTaskMember(currentUserPendingConfirmTasks.value[0]!),
      title: '确认会签办理人已提交的异常资料',
      type: 'primary',
    });
  }
  if (isNew.value || canFlow.value) {
    actions.push({
      key: 'primary',
      label: isNew.value ? '提交' : isQaClosureStep.value ? '关闭归档' : '办理',
      onClick: isQaClosureStep.value ? handleClose : handleSubmit,
      title: isNew.value
        ? '提交异常事件'
        : isQaClosureStep.value
          ? '闭环确认并关闭归档'
          : '完成当前办理',
      type: 'primary',
    });
  }
  if (canFlow.value) {
    actions.push({
      danger: true,
      key: 'return',
      label: '退回',
      onClick: handleReturn,
      title: '退回补充',
    });
  }
  actions.push({
    key: 'windowClose',
    label: '关闭',
    onClick: () => modalApi.close(),
    title: '关闭窗口',
  });
  return actions;
});

const leadingToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return toolbarActions.value;
  }
  return toolbarActions.value.slice(0, TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW);
});

const overflowToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return [];
  }
  return toolbarActions.value.slice(TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW, -1);
});

const trailingToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return [];
  }
  return toolbarActions.value.slice(-1);
});

async function loadDetail(id: number, tabType?: string) {
  loading.value = true;
  try {
    const detail = await getExceptionEvent(id);
    formData.value = { ...detail, tabType };
    syncAttachmentUrls(detail.relations);
  } finally {
    loading.value = false;
  }
  await promptPendingGroupMemberConfirm(tabType);
}

function syncResponsibilityConfirmFields() {
  if (formData.value.status !== 'RESPONSIBILITY_CONFIRM') {
    return;
  }
  const deptIds = uniqueDeptIds(responsibilityDeptValues.value);
  const deptName = deptIds
    .map((id) => findDeptName(id))
    .filter(Boolean)
    .join('、');
  formData.value.actionDeptId = deptIds.length === 1 ? deptIds[0] : undefined;
  formData.value.actionDeptName = deptName || undefined;
  syncRootCauseAssignmentTaskMeta();
}

async function handleSubmit() {
  await formRef.value?.validate();
  syncResponsibilityConfirmFields();
  const payload = buildExceptionPayload();
  const isMisreportClose =
    !isNew.value &&
    formData.value.status === 'CONFIRMING' &&
    confirmActionCode.value === CONFIRM_ACTION_MISREPORT_CLOSE;
  if (!isNew.value && formData.value.status === 'CONFIRMING' && !validateConfirmClassification()) {
    return;
  }
  if (
    isNew.value &&
    (payload.groupTasks || []).length > 0 &&
    !validateGroupTasksBeforeDispatch(payload.groupTasks || [])
  ) {
    return;
  }
  if (!isMisreportClose && !isNew.value && formData.value.status === 'CONFIRMING') {
    if (!validateConfirmContainmentPlan()) {
      return;
    }
    if (!approvalForm.value.nextHandlerUserId) {
      message.warning('请选择临时小组负责人');
      return;
    }
  }
  if (!isNew.value && formData.value.status === 'CONTAINMENT') {
    if (!formData.value.containmentAction?.trim()) {
      message.warning('请填写围堵措施内容');
      return;
    }
    const tasks = (payload.groupTasks || []).filter(
      (task) => normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION,
    );
    if (!validateGroupTasksBeforeDispatch(tasks, { requireContainmentSuggestion: false })) {
      return;
    }
  }
  if (!isNew.value && formData.value.status === 'RESPONSIBILITY_CONFIRM') {
    if (!responsibilityDeptDisplayName.value) {
      message.warning('请选择责任单位');
      return;
    }
    if (!approvalForm.value.nextHandlerUserId) {
      message.warning('请选择负责人');
      return;
    }
  }
  if (!isNew.value && formData.value.status === 'ROOT_CAUSE') {
    if (!formData.value.rootCause?.trim() && !formData.value.preventiveAction?.trim()) {
      message.warning('请填写根因分析和纠正预防措施');
      return;
    }
    const tasks = (payload.groupTasks || []).filter(
      (task) => normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE,
    );
    if (!validateGroupTasksBeforeDispatch(tasks)) {
      return;
    }
  }
  if (!isNew.value && formData.value.status === 'RESULT_UPLOADER_ASSIGN') {
    if (!approvalForm.value.nextHandlerUserId) {
      message.warning('请选择执行结果上传人');
      return;
    }
  }
  if (!isNew.value && formData.value.status === 'RESULT_UPLOAD') {
    if (!formData.value.correctivePreventiveResult?.trim()) {
      message.warning('请填写纠正预防措施执行成果');
      return;
    }
  }
  const willAutoCreateNcr =
    !isNew.value &&
    formData.value.status === 'CONFIRMING' &&
    !isMisreportClose &&
    payload.isRelatedProduct &&
    !payload.relatedNcrNo;
  const confirmContent = isNew.value
    ? '确认提交异常事件？'
    : isMisreportClose
      ? '确认选择误报？提交后异常事件将直接关闭归档。'
      : willAutoCreateNcr
        ? '关联产品已选择“是”，且未选择已有 NCR。办理后将自动生成关联 NCR，确认提交？'
        : '确认完成当前异常事件办理？';
  const confirmed = await confirmAction(confirmContent);
  if (!confirmed) {
    return;
  }
  modalApi.setState({ loading: true });
  try {
    if (isNew.value) {
      const created = await createExceptionEvent(payload);
      formData.value = { ...created, tabType: formData.value.tabType };
      syncAttachmentUrls(created.relations);
      if (willAutoCreateNcr && created.relatedNcrNo) {
        message.success(`异常事件已提报，并自动生成关联 NCR：${created.relatedNcrNo}`);
      } else {
        message.success('异常事件已提报');
      }
    } else if (formData.value.id) {
      await handleExceptionEvent({
        ...payload,
        id: formData.value.id,
        actionCode: isMisreportClose ? CONFIRM_ACTION_MISREPORT_CLOSE : 'HANDLE',
        nextHandlerUserId: approvalForm.value.nextHandlerUserId,
        nextHandlerUserName: approvalForm.value.nextHandlerUserName,
        opinion: approvalForm.value.opinion,
        copyToUserIds: approvalForm.value.copyUserIds,
        copyToUserNames: approvalForm.value.copyUserNames,
      });
      message.success(isMisreportClose ? '异常事件已按误报关闭' : '异常事件已办理');
    }
    emit('success');
    modalApi.close();
  } finally {
    modalApi.setState({ loading: false });
  }
}

function validateConfirmClassification() {
  if (!formData.value.exceptionType) {
    message.warning('请选择二、确认中的异常类别');
    return false;
  }
  if (!formData.value.exceptionLevel) {
    message.warning('请选择二、确认中的异常等级');
    return false;
  }
  if (
    formData.value.isRelatedProduct === undefined ||
    formData.value.isRelatedProduct === null
  ) {
    message.warning('请选择二、确认中的关联产品');
    return false;
  }
  return true;
}

function validateConfirmContainmentPlan() {
  if (!containmentDeadlineValue.value) {
    message.warning('请选择二、确认中的处理期限');
    return false;
  }
  return true;
}

function ensureGroupTasks() {
  if (!formData.value.groupTasks) {
    formData.value.groupTasks = [];
  }
  return formData.value.groupTasks;
}

function createDefaultAssignmentTask(taskType: ExceptionTaskType) {
  const rootCauseDeptName = formData.value.actionDeptName;
  return {
    containmentDeptId:
      taskType === TASK_TYPE_ROOT_CAUSE ? formData.value.actionDeptId : undefined,
    containmentDeptName:
      taskType === TASK_TYPE_ROOT_CAUSE ? rootCauseDeptName : undefined,
    containmentSuggestion:
      taskType === TASK_TYPE_INVESTIGATION ? formData.value.containmentAction || '' : undefined,
    executorUserId:
      taskType === TASK_TYPE_ROOT_CAUSE ? formData.value.actionOwnerId : undefined,
    executorUserName:
      taskType === TASK_TYPE_ROOT_CAUSE ? formData.value.actionOwnerName : undefined,
    groupName:
      taskType === TASK_TYPE_ROOT_CAUSE
        ? rootCauseDeptName || '责任部门'
        : '临时小组',
    members: [],
    rootCauseOwnerId:
      taskType === TASK_TYPE_ROOT_CAUSE ? formData.value.actionOwnerId : undefined,
    rootCauseOwnerName:
      taskType === TASK_TYPE_ROOT_CAUSE ? formData.value.actionOwnerName : undefined,
    taskStatus: 'DISPATCHED',
    taskType,
  } as MesExceptionApi.GroupTask;
}

function getAssignmentTask(taskType: ExceptionTaskType) {
  return (formData.value.groupTasks || []).find(
    (task) => normalizeTaskType(task.taskType) === taskType,
  );
}

function ensureAssignmentTask(taskType: ExceptionTaskType) {
  const tasks = ensureGroupTasks();
  let task = tasks.find((item) => normalizeTaskType(item.taskType) === taskType);
  if (!task) {
    task = createDefaultAssignmentTask(taskType);
    tasks.push(task);
  }
  return task;
}

function syncRootCauseAssignmentTaskMeta(
  task = getAssignmentTask(TASK_TYPE_ROOT_CAUSE),
  deptName = responsibilityDeptDisplayName.value,
) {
  if (!task) {
    return;
  }
  task.containmentDeptId = undefined;
  task.containmentDeptName = deptName || undefined;
  task.groupName = deptName || '责任部门';
  task.executorUserId =
    approvalForm.value.nextHandlerUserId || formData.value.actionOwnerId || task.executorUserId;
  task.executorUserName =
    approvalForm.value.nextHandlerUserName || formData.value.actionOwnerName || task.executorUserName;
  task.rootCauseOwnerId = task.executorUserId;
  task.rootCauseOwnerName = task.executorUserName;
}

function openAssignmentMemberMultiSelector(taskType: ExceptionTaskType) {
  const task = ensureAssignmentTask(taskType);
  if (taskType === TASK_TYPE_ROOT_CAUSE) {
    syncRootCauseAssignmentTaskMeta(task);
  }
  const taskIndex = ensureGroupTasks().indexOf(task);
  openUserSelector('groupTaskMembersMulti', undefined, taskIndex);
}

function resetTaskDialogs() {
  taskReplyOpen.value = false;
  taskReplyTarget.value = undefined;
  taskReplyForm.value = {};
  investigationSignConfirmOpen.value = false;
  investigationSignConfirmTarget.value = undefined;
  investigationSignConfirmMember.value = undefined;
  investigationSignConfirmOpinion.value = '';
  taskReviewOpen.value = false;
  taskReviewTarget.value = undefined;
  taskReviewAction.value = MEMBER_CONFIRM_ACTION_CONFIRM;
  taskReviewMode.value = 'MEMBER';
  taskReviewOpinion.value = '';
}

async function loadDeptOptions() {
  if (deptOptions.value.length > 0) {
    return;
  }
  deptOptions.value = await getSimpleDeptList();
}

function formatDateTime(date = new Date()) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function normalizeNumber(value: unknown) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined;
}

function splitTextList(value?: string) {
  return (value || '')
    .split(/[、,，;]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function collectResultUploadCopyUsers() {
  const users = new Map<number, string>();
  const addUser = (userId?: number, userName?: string) => {
    const normalizedUserId = normalizeNumber(userId);
    if (!normalizedUserId) {
      return;
    }
    const normalizedUserName = String(userName || '').trim() || String(normalizedUserId);
    const existedName = users.get(normalizedUserId);
    if (!existedName || existedName === String(normalizedUserId)) {
      users.set(normalizedUserId, normalizedUserName);
    }
  };

  addUser(formData.value.discovererId, formData.value.discovererName);
  addUser(formData.value.confirmerId, formData.value.confirmerName);
  addUser(formData.value.containmentOwnerId, formData.value.containmentOwnerName);
  addUser(formData.value.actionOwnerId, formData.value.actionOwnerName);
  addUser(formData.value.resultUploaderId, formData.value.resultUploaderName);
  addUser(formData.value.currentHandlerUserId, formData.value.currentHandlerUserName);
  addUser(currentUserId.value, currentUserName.value);

  (formData.value.teamMembers || []).forEach((member) => {
    addUser(member.userId, member.userName);
  });

  (formData.value.groupTasks || []).forEach((task) => {
    addUser(task.dispatcherUserId, task.dispatcherUserName);
    addUser(task.executorUserId, task.executorUserName);
    addUser(task.rootCauseOwnerId, task.rootCauseOwnerName);
    addUser(task.submitterUserId, task.submitterUserName);
    addUser(task.reviewerUserId, task.reviewerUserName);
    (task.members || []).forEach((member) => {
      addUser(member.userId, member.userName);
      addUser(member.delegateUserId, member.delegateUserName);
      addUser(member.actualHandlerUserId, member.actualHandlerUserName);
    });
    (task.replies || []).forEach((reply) => {
      addUser(reply.memberUserId, reply.memberUserName);
      addUser(reply.submitterUserId, reply.submitterUserName);
      addUser(reply.reviewerUserId, reply.reviewerUserName);
    });
    (task.confirmLogs || []).forEach((log) => {
      addUser(log.userId, log.userName);
    });
  });

  (formData.value.flowLogs || []).forEach((log) => {
    addUser(log.handlerUserId, log.handlerUserName);
  });

  const copyUserIds = splitTextList(formData.value.copyUserIds)
    .map((item) => normalizeNumber(item))
    .filter((item): item is number => !!item);
  const copyUserNames = splitTextList(formData.value.copyUserNames);
  copyUserIds.forEach((userId, index) => addUser(userId, copyUserNames[index]));

  return [...users.entries()].map(([id, name]) => ({ id, name }));
}

function ensureResultUploadCopyUsers() {
  if (resultUploadCopyUsersInitialized.value) {
    return;
  }
  const users = collectResultUploadCopyUsers();
  approvalForm.value.copyUserIds = users.map((user) => user.id);
  approvalForm.value.copyUserNames = users.map((user) => user.name);
  resultUploadCopyUsersInitialized.value = true;
}

function findDeptName(deptId?: number) {
  if (!deptId) {
    return '';
  }
  return deptOptions.value.find((dept) => dept.id === deptId)?.name || '';
}

function findDeptIdByName(deptName?: string) {
  if (!deptName) {
    return undefined;
  }
  return deptOptions.value.find((dept) => dept.name === deptName)?.id;
}

function findDeptById(deptId?: number) {
  if (!deptId) {
    return undefined;
  }
  return deptOptions.value.find((dept) => dept.id === deptId);
}

function resolveGroupMemberBusinessDept(deptId?: number) {
  const dept = findDeptById(deptId);
  if (!dept) {
    return { deptId, deptName: findDeptName(deptId) };
  }
  const parent = findDeptById(dept.parentId);
  const shouldUseParent = !!parent && !!normalizeNumber(parent.parentId);
  const displayDept = shouldUseParent ? parent : dept;
  return { deptId: displayDept.id || deptId, deptName: displayDept.name || dept.name || '' };
}

function getGroupMemberBusinessDeptName(member: MesExceptionApi.GroupTaskMember) {
  return resolveGroupMemberBusinessDept(member.deptId).deptName || member.deptName || '';
}

function handleDiscoverDeptChange(value?: number) {
  formData.value.discoverDeptCode = value ? String(value) : undefined;
  formData.value.discoverDeptName = findDeptName(value);
}

function handleConfirmDeptChange(value?: number) {
  formData.value.confirmDeptCode = value ? String(value) : undefined;
  formData.value.confirmDeptName = findDeptName(value);
}

function handleGroupTaskMemberDeptChange(
  taskIndex: number,
  memberIndex: number,
  value?: number,
) {
  const member = formData.value.groupTasks?.[taskIndex]?.members?.[memberIndex];
  if (!member) {
    return;
  }
  const dept = resolveGroupMemberBusinessDept(value);
  member.deptId = dept.deptId;
  member.deptName = dept.deptName;
}

function ensureStepDefaultValues() {
  if (!canFlow.value) {
    return;
  }
  if (formData.value.status === 'CONFIRMING') {
    formData.value.confirmDeptId = formData.value.confirmDeptId || currentDeptId.value;
    formData.value.confirmDeptCode =
      formData.value.confirmDeptCode || currentDeptCode.value;
    formData.value.confirmDeptName =
      formData.value.confirmDeptName || currentDeptName.value;
    formData.value.confirmTime = formData.value.confirmTime || formatDateTime();
    formData.value.confirmerId = formData.value.confirmerId || currentUserId.value;
    formData.value.confirmerCode =
      formData.value.confirmerCode || currentUserCode.value;
    formData.value.confirmerName =
      formData.value.confirmerName || currentUserName.value;
  }
  if (formData.value.status === 'CONTAINMENT') {
    approvalForm.value.nextHandlerUserId = formData.value.containmentOwnerId;
    approvalForm.value.nextHandlerUserName = formData.value.containmentOwnerName || '';
  }
  if (formData.value.status === 'RESPONSIBILITY_CONFIRM') {
    approvalForm.value.nextHandlerUserId = formData.value.actionOwnerId;
    approvalForm.value.nextHandlerUserName = formData.value.actionOwnerName || '';
  }
  if (formData.value.status === 'RESULT_UPLOADER_ASSIGN') {
    approvalForm.value.nextHandlerUserId = formData.value.resultUploaderId;
    approvalForm.value.nextHandlerUserName = formData.value.resultUploaderName || '';
  }
  if (formData.value.status === 'RESULT_UPLOAD') {
    formData.value.resultUploadTime = formData.value.resultUploadTime || formatDateTime();
    ensureResultUploadCopyUsers();
  }
  if (
    formData.value.status === 'QA_CLOSURE' ||
    formData.value.status === 'VERIFYING'
  ) {
    formData.value.qaConfirmerId =
      formData.value.qaConfirmerId || currentUserId.value;
    formData.value.qaConfirmerName =
      formData.value.qaConfirmerName || currentUserName.value;
    formData.value.finishTime = formData.value.finishTime || formatDateTime();
  }
}

function openUserSelector(
  type:
    | 'actionOwner'
    | 'confirmer'
    | 'copyUsers'
    | 'discoverer'
    | 'groupRootCauseOwner'
    | 'groupTaskDelegate'
    | 'groupTaskMember'
    | 'groupTaskMembersMulti'
    | 'nextHandler'
    | 'qaConfirmer',
  index?: number,
  taskIndex?: number,
  memberIndex?: number,
) {
  activeUserSelectTarget.value = { index, memberIndex, taskIndex, type };
  const userId = getSelectedUserId(type, index, taskIndex, memberIndex);
  const multiple = type === 'copyUsers' || type === 'groupTaskMembersMulti';
  userSelectModalApi
    .setData({
      modalZIndex: 5000,
      multiple,
      userIds:
        type === 'copyUsers'
          ? approvalForm.value.copyUserIds
          : type === 'groupTaskMembersMulti' && taskIndex !== undefined
            ? (formData.value.groupTasks?.[taskIndex]?.members || [])
                .map((member) => member.userId)
                .filter((id): id is number => !!id)
            : userId
              ? [userId]
              : [],
    })
    .open();
}

function getSelectedUserId(
  type:
    | 'actionOwner'
    | 'confirmer'
    | 'copyUsers'
    | 'discoverer'
    | 'groupRootCauseOwner'
    | 'groupTaskDelegate'
    | 'groupTaskMember'
    | 'groupTaskMembersMulti'
    | 'nextHandler'
    | 'qaConfirmer',
  index?: number,
  taskIndex?: number,
  memberIndex?: number,
) {
  if (type === 'discoverer') {
    return formData.value.discovererId;
  }
  if (type === 'confirmer') {
    return formData.value.confirmerId;
  }
  if (type === 'actionOwner') {
    return formData.value.actionOwnerId;
  }
  if (type === 'qaConfirmer') {
    return formData.value.qaConfirmerId;
  }
  if (type === 'groupRootCauseOwner' && taskIndex !== undefined) {
    return formData.value.groupTasks?.[taskIndex]?.rootCauseOwnerId;
  }
  if (type === 'groupTaskDelegate') {
    return findGroupTaskMemberByIds(
      activeUserSelectTarget.value?.taskId,
      activeUserSelectTarget.value?.memberId,
    )?.delegateUserId;
  }
  if (
    type === 'groupTaskMember' &&
    taskIndex !== undefined &&
    memberIndex !== undefined
  ) {
    return formData.value.groupTasks?.[taskIndex]?.members?.[memberIndex]?.userId;
  }
  if (type === 'nextHandler') {
    return approvalForm.value.nextHandlerUserId;
  }
  return undefined;
}

function findGroupTaskMemberByIds(taskId?: number, memberId?: number) {
  if (!taskId || !memberId) {
    return undefined;
  }
  const task = (formData.value.groupTasks || []).find(
    (item) => Number(item.id) === Number(taskId),
  );
  return (task?.members || []).find(
    (member) => Number(member.id) === Number(memberId),
  );
}

function openGroupTaskDelegateSelector(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  if (!task.id || !member.id) {
    message.warning('未找到可委托的会签行');
    return;
  }
  activeUserSelectTarget.value = {
    memberId: member.id,
    taskId: task.id,
    type: 'groupTaskDelegate',
  };
  userSelectModalApi
    .setData({
      modalZIndex: 5000,
      multiple: false,
      userIds: member.delegateUserId ? [member.delegateUserId] : [],
    })
    .open();
}

async function handleDelegateGroupTaskMember(
  taskId: number | undefined,
  memberId: number | undefined,
  delegateUserId: number,
  delegateUserName: string,
) {
  if (!taskId || !memberId) {
    message.warning('未找到可委托的会签行');
    return;
  }
  await delegateExceptionGroupTaskMember({
    delegateUserId,
    delegateUserName,
    memberId,
    taskId,
  });
  if (formData.value.id) {
    await loadDetail(formData.value.id, formData.value.tabType);
  }
  emit('success');
  message.success('委托代办已设置');
}

async function handleUserConfirm(userList: SystemUserApi.User[]) {
  const target = activeUserSelectTarget.value;
  if (!target) {
    return;
  }
  if (target.type === 'copyUsers') {
    approvalForm.value.copyUserIds = userList
      .map((item) => item.id)
      .filter((id): id is number => !!id);
    approvalForm.value.copyUserNames = userList
      .filter((item) => item.id)
      .map((item) => item.nickname || item.username || String(item.id));
    activeUserSelectTarget.value = undefined;
    return;
  }
  if (target.type === 'groupTaskMembersMulti') {
    const task = target.taskIndex === undefined
      ? undefined
      : formData.value.groupTasks?.[target.taskIndex];
    if (task) {
      task.members = userList
        .filter((item) => item.id)
        .map((item, index) => {
          const dept = resolveGroupMemberBusinessDept(item.deptId);
          return {
            deptId: dept.deptId,
            deptName: dept.deptName,
            memberRole: 'MEMBER',
            sortNo: index + 1,
            userId: item.id,
            userName: item.nickname || item.username || String(item.id),
          };
        });
    }
    activeUserSelectTarget.value = undefined;
    return;
  }
  const user = userList.find((item) => item.id);
  if (!user) {
    return;
  }
  const userName = user.nickname || user.username || String(user.id);
  const userCode = user.username || String(user.id);
  if (target.type === 'discoverer') {
    formData.value.discovererId = user.id;
    formData.value.discovererCode = userCode;
    formData.value.discovererName = userName;
    if (user.deptId) {
      formData.value.discoverDeptId = user.deptId;
      formData.value.discoverDeptCode = String(user.deptId);
      formData.value.discoverDeptName = findDeptName(user.deptId);
    }
  } else if (target.type === 'confirmer') {
    formData.value.confirmerId = user.id;
    formData.value.confirmerCode = userCode;
    formData.value.confirmerName = userName;
    if (user.deptId) {
      formData.value.confirmDeptId = user.deptId;
      formData.value.confirmDeptCode = String(user.deptId);
      formData.value.confirmDeptName = findDeptName(user.deptId);
    }
  } else if (target.type === 'actionOwner') {
    formData.value.actionOwnerId = user.id;
    formData.value.actionOwnerName = userName;
  } else if (target.type === 'qaConfirmer') {
    formData.value.qaConfirmerId = user.id;
    formData.value.qaConfirmerName = userName;
  } else if (target.type === 'nextHandler') {
    approvalForm.value.nextHandlerUserId = user.id;
    approvalForm.value.nextHandlerUserName = userName;
  } else if (target.type === 'groupTaskDelegate') {
    await handleDelegateGroupTaskMember(
      target.taskId,
      target.memberId,
      user.id!,
      userName,
    );
  } else if (
    target.type === 'groupRootCauseOwner' &&
    target.taskIndex !== undefined
  ) {
    const task = formData.value.groupTasks?.[target.taskIndex];
    if (task) {
      task.rootCauseOwnerId = user.id;
      task.rootCauseOwnerName = userName;
    }
  } else if (
    target.type === 'groupTaskMember' &&
    target.taskIndex !== undefined &&
    target.memberIndex !== undefined
  ) {
    const member =
      formData.value.groupTasks?.[target.taskIndex]?.members?.[
        target.memberIndex
      ];
    if (member) {
      member.userId = user.id;
      member.userName = userName;
      if (user.deptId) {
        const dept = resolveGroupMemberBusinessDept(user.deptId);
        member.deptId = dept.deptId;
        member.deptName = dept.deptName;
      }
    }
  }
  activeUserSelectTarget.value = undefined;
}

function syncAttachmentUrls(relations?: MesExceptionApi.Relation[]) {
  confirmAttachmentUrls.value = getAttachmentUrls(
    relations,
    ATTACHMENT_RELATION_TYPES.confirmRelated,
  );
  containmentAttachmentUrls.value = getAttachmentUrls(
    relations,
    ATTACHMENT_RELATION_TYPES.containment,
  );
  rootCauseAttachmentUrls.value = getAttachmentUrls(
    relations,
    ATTACHMENT_RELATION_TYPES.rootCause,
  );
  resultAttachmentUrls.value = getAttachmentUrls(
    relations,
    ATTACHMENT_RELATION_TYPES.result,
  );
  effectAttachmentUrls.value = getAttachmentUrls(
    relations,
    ATTACHMENT_RELATION_TYPES.effect,
  );
}

function getAttachmentUrls(
  relations: MesExceptionApi.Relation[] | undefined,
  relationType: string,
) {
  return (relations || [])
    .filter((relation) => relation.relationType === relationType)
    .map((relation) => relation.remark || relation.relatedObjectNo || '')
    .filter(Boolean);
}

function buildExceptionPayload() {
  return {
    ...formData.value,
    groupTasks: buildGroupTasksPayload(),
    relations: buildRelationsPayload(),
  } as MesExceptionApi.ExceptionRecord;
}

function buildRelationsPayload() {
  const sourceRelations = (formData.value.relations || []).filter(
    (relation) =>
      !ATTACHMENT_RELATION_TYPE_LIST.includes(
        relation.relationType as (typeof ATTACHMENT_RELATION_TYPE_LIST)[number],
      ),
  );
  return [
    ...sourceRelations,
    ...buildAttachmentRelations(
      ATTACHMENT_RELATION_TYPES.confirmRelated,
      confirmAttachmentUrls.value,
    ),
    ...buildAttachmentRelations(
      ATTACHMENT_RELATION_TYPES.containment,
      containmentAttachmentUrls.value,
    ),
    ...buildAttachmentRelations(
      ATTACHMENT_RELATION_TYPES.rootCause,
      rootCauseAttachmentUrls.value,
    ),
    ...buildAttachmentRelations(
      ATTACHMENT_RELATION_TYPES.result,
      resultAttachmentUrls.value,
    ),
    ...buildAttachmentRelations(
      ATTACHMENT_RELATION_TYPES.effect,
      effectAttachmentUrls.value,
    ),
  ];
}

function buildAttachmentRelations(relationType: string, urls: string[]) {
  return urls.filter(Boolean).map((url, index) => ({
    relationStatus: 'ACTIVE',
    relationType,
    relatedObjectName: getAttachmentName(url, index),
    relatedObjectNo: getAttachmentName(url, index),
    remark: url,
  }));
}

function getAttachmentName(url: string, index: number) {
  const cleanUrl = String(url || '').split('?')[0] || '';
  const fileName = cleanUrl.split('/').pop();
  if (!fileName) {
    return `附件${index + 1}`;
  }
  try {
    return decodeURIComponent(fileName);
  } catch {
    return fileName;
  }
}

function handleAttachmentPreview(file: any) {
  const url =
    file?.url ||
    file?.response?.url ||
    file?.response?.data ||
    file?.response ||
    '';
  if (url) {
    window.open(String(url), '_blank');
  }
}

function openNcrSelector() {
  if (relatedNcrLocked.value) {
    message.warning(relatedNcrLockMessage);
    return;
  }
  if (!canEditConfirmStep.value) {
    return;
  }
  ncrModalApi
    .setData({
      ncrType: formData.value.isRelatedProduct
        ? RELATION_TYPE_NCR
        : RELATION_TYPE_RAW_MATERIAL_NCR,
      sourceType: formData.value.isRelatedProduct ? 'PRODUCT' : 'RAW_MATERIAL',
    })
    .open();
}

async function handleSelectNcr(row: MesExceptionApi.NcrCandidate) {
  if (relatedNcrLocked.value) {
    message.warning(relatedNcrLockMessage);
    return;
  }
  if (!row.ncrNo) {
    return;
  }
  const ncrType =
    row.ncrType ||
    (formData.value.isRelatedProduct
      ? RELATION_TYPE_NCR
      : RELATION_TYPE_RAW_MATERIAL_NCR);
  formData.value.relatedNcrNo = row.ncrNo;
  formData.value.isRelatedProduct = ncrType !== RELATION_TYPE_RAW_MATERIAL_NCR;
  message.success(
    ncrType === RELATION_TYPE_RAW_MATERIAL_NCR
      ? `已关联原材料处置单：${row.ncrNo}`
      : `已关联 NCR：${row.ncrNo}`,
  );
}

function openLinkedNcr() {
  if (!formData.value.relatedNcrNo && !primaryNcrRelation.value?.relatedObjectId) {
    return;
  }
  const relation = primaryNcrRelation.value;
  void router.push({
    path:
      linkedNcrType.value === RELATION_TYPE_RAW_MATERIAL_NCR
        ? '/mes/quality/abnormal/raw-material-ncr'
        : '/mes/quality/abnormal/ncr',
    query: {
      id: relation?.relatedObjectId || undefined,
      ncNo: relation?.relatedObjectNo || formData.value.relatedNcrNo,
      tabType: 'monitor',
    },
  });
}

function handleReturn() {
  if (!formData.value.id) {
    return;
  }
  returnOpinion.value = approvalForm.value.opinion || '';
  returnModalOpen.value = true;
}

async function handleReturnSave() {
  if (!formData.value.id) {
    return;
  }
  const opinion = returnOpinion.value.trim();
  if (!opinion) {
    message.warning('请填写退回意见');
    return;
  }
  const confirmed = await confirmAction('确认退回当前异常事件？');
  if (!confirmed) {
    return;
  }
  returnSubmitting.value = true;
  try {
    const returnTarget = getReturnTargetByStatus();
    approvalForm.value.opinion = opinion;
    await returnExceptionEvent({
      id: formData.value.id,
      opinion,
      targetNodeCode: returnTarget.code,
      targetNodeName: returnTarget.name,
    });
    message.success('异常事件已退回');
    returnModalOpen.value = false;
    emit('success');
    modalApi.close();
  } finally {
    returnSubmitting.value = false;
  }
}

function getReturnTargetByStatus() {
  if (formData.value.status === 'RESPONSIBILITY_CONFIRM') {
    return { code: 'CONTAINMENT', name: '围堵措施' };
  }
  if (
    formData.value.status === 'QA_CLOSURE' ||
    formData.value.status === 'VERIFYING'
  ) {
    return {
      code: 'ROOT_CAUSE',
      name: '根因分析和纠正预防措施与标准化',
    };
  }
  return { code: 'RETURNED', name: '退回补充' };
}

async function handleClose() {
  if (!formData.value.id) {
    return;
  }
  if (!formData.value.effectConfirm?.trim()) {
    message.warning('请填写闭环确认');
    return;
  }
  if (!formData.value.qaConfirmValid) {
    message.warning('请勾选确认有效');
    return;
  }
  const confirmed = await confirmAction('确认关闭当前异常事件？');
  if (!confirmed) {
    return;
  }
  await closeExceptionEvent({
    id: formData.value.id,
    effectConfirm: formData.value.effectConfirm,
    finishTime: formData.value.finishTime,
    opinion: approvalForm.value.opinion,
    qaConfirmValid: formData.value.qaConfirmValid,
    relations: buildRelationsPayload(),
    qaConfirmerId: formData.value.qaConfirmerId,
    qaConfirmerName: formData.value.qaConfirmerName,
  });
  message.success('异常事件已关闭');
  emit('success');
  modalApi.close();
}

function buildGroupTasksPayload() {
  return (formData.value.groupTasks || []).map((task) => {
    const taskType = normalizeTaskType(task.taskType);
    return {
      ...task,
      groupName:
        task.groupName ||
        (taskType === TASK_TYPE_ROOT_CAUSE ? '责任部门' : '临时小组'),
      containmentSuggestion:
        taskType === TASK_TYPE_INVESTIGATION
          ? task.containmentSuggestion || formData.value.containmentAction
          : task.containmentSuggestion,
      containmentDeadline:
        taskType === TASK_TYPE_INVESTIGATION
          ? task.containmentDeadline || formData.value.containmentDeadline
          : task.containmentDeadline,
      actionDescription:
        taskType === TASK_TYPE_INVESTIGATION
          ? task.actionDescription || formData.value.containmentAction
          : task.actionDescription,
      attachmentUrls:
        taskType === TASK_TYPE_INVESTIGATION
          ? task.attachmentUrls || containmentAttachmentUrls.value
          : task.attachmentUrls,
      preventiveAction:
        taskType === TASK_TYPE_ROOT_CAUSE
          ? task.preventiveAction || formData.value.preventiveAction
          : task.preventiveAction,
      rootCause:
        taskType === TASK_TYPE_ROOT_CAUSE
          ? task.rootCause || formData.value.rootCause
          : task.rootCause,
      rootCauseCategory:
        taskType === TASK_TYPE_ROOT_CAUSE
          ? task.rootCauseCategory || formData.value.rootCauseCategory
          : task.rootCauseCategory,
      rootCauseOwnerId: task.rootCauseOwnerId,
      rootCauseOwnerName: task.rootCauseOwnerName,
      members: (task.members || []).map((member, memberIndex) => ({
        ...member,
        sortNo: member.sortNo ?? memberIndex + 1,
      })),
      taskType,
    };
  });
}

function normalizeTaskType(taskType?: string): ExceptionTaskType {
  return taskType === TASK_TYPE_ROOT_CAUSE
    ? TASK_TYPE_ROOT_CAUSE
    : TASK_TYPE_INVESTIGATION;
}

function getTaskTodoActionLabel(hasInvestigation: boolean, hasRootCause: boolean) {
  if (hasInvestigation && hasRootCause) {
    return '处理子任务';
  }
  if (hasRootCause) {
    return '确认会签';
  }
  if (hasInvestigation) {
    return '确认会签';
  }
  return '';
}

function getTaskConfirmTodoActionLabel(
  hasInvestigation: boolean,
  hasRootCause: boolean,
) {
  if (hasInvestigation && hasRootCause) {
    return '确认子任务';
  }
  if (hasRootCause) {
    return '确认根因纠正';
  }
  if (hasInvestigation) {
    return '确认小组提交';
  }
  return '';
}

function canEditGroupTask(task: MesExceptionApi.GroupTask) {
  return (
    canMaintainGroupTasks.value &&
    !['ACCEPTED', 'SIGNED', 'SUBMITTED'].includes(task.taskStatus || '')
  );
}

function canSubmitGroupTask(task: MesExceptionApi.GroupTask) {
  return (
    !isNew.value &&
    isTaskTodoContext() &&
    !isTerminalStatus.value &&
    ['DISPATCHED', 'RETURNED', 'SUBMITTED'].includes(task.taskStatus || '') &&
    task.currentUserCanSubmit === true
  );
}

function canConfirmGroupTask(task: MesExceptionApi.GroupTask) {
  return false;
}

function canReviewGroupTask(task: MesExceptionApi.GroupTask) {
  return (
    !isNew.value &&
    !isTerminalStatus.value &&
    task.taskStatus === 'SIGNED' &&
    (task.currentUserCanReview === true || canMaintainGroupTasks.value)
  );
}

function isTaskTodoContext() {
  return (
    formData.value.tabType === 'todo' ||
    formData.value.currentUserTaskTodo === true
  );
}

function getGroupTaskStatusMeta(status?: string) {
  const statusMap: Record<string, { color: string; label: string }> = {
    ACCEPTED: { color: 'success', label: '已完成' },
    DISPATCHED: { color: 'processing', label: '待填写' },
    RETURNED: { color: 'error', label: '待重写' },
    SIGNED: { color: 'warning', label: '待确认' },
    SUBMITTED: { color: 'warning', label: '部分已办理' },
  };
  return statusMap[status || ''] || { color: 'default', label: '未下达' };
}

function validateGroupTasksBeforeDispatch(
  tasks: MesExceptionApi.GroupTask[],
  options: { requireContainmentSuggestion?: boolean } = {},
) {
  if (tasks.length === 0) {
    message.warning('请先分配任务');
    return false;
  }
  const taskTypes = new Set<string>();
  for (const task of tasks) {
    const taskType = normalizeTaskType(task.taskType);
    if (taskTypes.has(taskType)) {
      message.warning('临时小组和责任部门各只能分配一个任务');
      return false;
    }
    taskTypes.add(taskType);
  }
  const invalidTask = tasks.find((task) => {
    const confirmerRows = getValidConfirmers(task);
    const hasIncompleteConfirmers = (task.members || []).some(
      (member) =>
        member.userId ||
        member.userName,
    ) && (task.members || []).some(
      (member) => !(member.userId || member.userName),
    );
    const hasDuplicatedUser = hasDuplicateConfirmUser(task);
    if (normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE) {
      return (
        confirmerRows.length === 0 ||
        hasIncompleteConfirmers ||
        hasDuplicatedUser
      );
    }
    return (
      (options.requireContainmentSuggestion && !task.containmentSuggestion?.trim()) ||
      confirmerRows.length === 0 ||
      hasIncompleteConfirmers ||
      hasDuplicatedUser
    );
  });
  if (invalidTask) {
    message.warning(
      normalizeTaskType(invalidTask.taskType) === TASK_TYPE_ROOT_CAUSE
        ? '请选择根因纠正会签人，且同一人不能重复'
        : options.requireContainmentSuggestion
          ? '请填写围堵措施，并选择临时小组会签人'
          : '请选择临时小组会签人，且同一人不能重复',
    );
    return false;
  }
  return true;
}

function getValidConfirmers(task: MesExceptionApi.GroupTask) {
  return (task.members || []).filter(
    (member) => member.userId || member.userName,
  );
}

function hasDuplicateConfirmUser(task: MesExceptionApi.GroupTask) {
  const seen = new Set<string>();
  for (const member of task.members || []) {
    const userKey = member.userId ? String(member.userId) : member.userName;
    if (!userKey) {
      continue;
    }
    if (seen.has(userKey)) {
      return true;
    }
    seen.add(userKey);
  }
  return false;
}

async function handleDispatchGroupTasks() {
  if (!formData.value.id) {
    message.warning('请先提交异常事件，再下达临时小组任务');
    return;
  }
  const groupTasks = buildGroupTasksPayload();
  if (!validateGroupTasksBeforeDispatch(groupTasks)) {
    return;
  }
  const confirmed = await confirmAction('确认下达/保存临时小组任务？');
  if (!confirmed) {
    return;
  }
  await saveExceptionGroupTasks({
    exceptionId: formData.value.id,
    groupTasks,
    opinion: approvalForm.value.opinion,
  });
  await loadDetail(formData.value.id, formData.value.tabType);
  emit('success');
  message.success('临时小组任务已下达');
}

function handleSubmitGroupTask(
  task: MesExceptionApi.GroupTask,
  member?: MesExceptionApi.GroupTaskMember,
) {
  if (!task.id) {
    return;
  }
  if (normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION) {
    void handleSubmitInvestigationSign(task, member);
    return;
  }
  if (normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE) {
    void handleSubmitRootCauseSign(task, member);
    return;
  }
  const currentMember = member || getCurrentUserTaskMember(task);
  taskReplyTarget.value = task;
  taskReplyForm.value = {
    actionDescription: currentMember?.actionDescription,
    attachmentUrls: [...(currentMember?.attachmentUrls || [])],
    preventiveAction: currentMember?.preventiveAction,
    rootCause: currentMember?.rootCause,
    rootCauseCategory: currentMember?.rootCauseCategory,
    taskType: normalizeTaskType(task.taskType),
  };
  taskReplyOpen.value = true;
}

async function handleSubmitInvestigationSign(
  task: MesExceptionApi.GroupTask,
  signMember?: MesExceptionApi.GroupTaskMember,
) {
  const member = signMember || getCurrentUserTaskMember(task);
  if (!task.id || !member) {
    message.warning('未找到当前账号可办理的临时小组会签行');
    return;
  }
  investigationSignConfirmTarget.value = task;
  investigationSignConfirmMember.value = member;
  investigationSignConfirmOpinion.value =
    member.confirmRemark || member.actionDescription || '';
  investigationSignConfirmOpen.value = true;
}

async function handleInvestigationSignConfirmSave() {
  const task = investigationSignConfirmTarget.value;
  const member = investigationSignConfirmMember.value;
  const opinion = investigationSignConfirmOpinion.value.trim();
  if (!task?.id || !member?.id) {
    message.warning('未找到当前账号可确认的会签行');
    return;
  }
  if (!opinion) {
    message.warning('请填写确认意见');
    return;
  }
  const actualFinishTime = formatDateTime();
  await confirmExceptionGroupTaskMember({
    actionCode: MEMBER_CONFIRM_ACTION_CONFIRM,
    actionDescription: opinion,
    actualFinishTime,
    attachmentUrls: member.attachmentUrls || [],
    memberId: member.id,
    remark: opinion,
    taskId: task.id,
  });
  await loadDetail(formData.value.id!, formData.value.tabType);
  investigationSignConfirmOpen.value = false;
  investigationSignConfirmTarget.value = undefined;
  investigationSignConfirmMember.value = undefined;
  investigationSignConfirmOpinion.value = '';
  emit('success');
  message.success('确认会签已提交');
}

async function handleSubmitRootCauseSign(
  task: MesExceptionApi.GroupTask,
  signMember?: MesExceptionApi.GroupTaskMember,
) {
  const member = signMember || getCurrentUserTaskMember(task);
  if (!task.id || !member) {
    message.warning('未找到当前账号可办理的责任单位会签行');
    return;
  }
  investigationSignConfirmTarget.value = task;
  investigationSignConfirmMember.value = member;
  investigationSignConfirmOpinion.value =
    member.confirmRemark || member.actionDescription || '';
  investigationSignConfirmOpen.value = true;
}

async function handleTaskReplySave() {
  const task = taskReplyTarget.value;
  if (!task?.id) {
    return;
  }
  if (
    normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION &&
    !taskReplyForm.value.actionDescription?.trim()
  ) {
    message.warning('请填写措施说明');
    return;
  }
  if (
    normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE &&
    !taskReplyForm.value.rootCause?.trim() &&
    !taskReplyForm.value.preventiveAction?.trim()
  ) {
    message.warning('请填写根因分析或纠正预防措施');
    return;
  }
  const confirmed = await confirmAction(
    normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE
      ? '确认提交本责任单位的根因分析和纠正预防措施与标准化？'
      : '确认提交本部门的围堵措施？',
  );
  if (!confirmed) {
    return;
  }
  const currentMember = getCurrentUserTaskMember(task);
  const actualFinishTime = formatDateTime();
  await confirmExceptionGroupTaskMember({
    actionCode: MEMBER_CONFIRM_ACTION_CONFIRM,
    actionDescription: taskReplyForm.value.actionDescription,
    actualFinishTime,
    attachmentUrls: taskReplyForm.value.attachmentUrls || [],
    preventiveAction: taskReplyForm.value.preventiveAction,
    rootCause: taskReplyForm.value.rootCause,
    rootCauseCategory: taskReplyForm.value.rootCauseCategory,
    memberId: currentMember?.id,
    remark: taskReplyForm.value.remark,
    taskId: task.id,
  });
  await loadDetail(formData.value.id!, formData.value.tabType);
  taskReplyOpen.value = false;
  emit('success');
  message.success('本部门办理内容已提交');
}

async function promptPendingGroupMemberConfirm(tabType?: string) {
  if (tabType && tabType !== 'todo' && formData.value.currentUserTaskTodo !== true) {
    return;
  }
  const task = currentUserPendingConfirmTasks.value[0];
  if (!task) {
    return;
  }
  await handleConfirmGroupTaskMember(task);
}

async function handleConfirmGroupTaskMember(
  task: MesExceptionApi.GroupTask,
  actionCode: TaskReviewAction = MEMBER_CONFIRM_ACTION_CONFIRM,
) {
  if (!task.id) {
    return;
  }
  if (actionCode === MEMBER_CONFIRM_ACTION_DISAGREE) {
    taskReviewTarget.value = task;
    taskReviewAction.value = MEMBER_CONFIRM_ACTION_DISAGREE;
    taskReviewMode.value = 'MEMBER';
    taskReviewOpinion.value = '';
    taskReviewOpen.value = true;
    return;
  }
  const overdue = isContainmentTaskOverdue(task);
  const confirmed = await confirmAction(
    overdue
      ? '已超时未确认，自动转入过期，请在质量管理/异常事件提报查询'
      : '提交资料已上传，请确认',
  );
  if (!confirmed) {
    return;
  }
  modalApi.setState({ loading: true });
  try {
    const result = await confirmExceptionGroupTaskMember({
      actionCode: MEMBER_CONFIRM_ACTION_CONFIRM,
      remark: overdue ? undefined : '确认提交资料',
      taskId: task.id,
    });
    emit('success');
    modalApi.close();
    if (result.overdue || overdue) {
      message.warning(
        result.message ||
          '已超时未确认，自动转入过期，请在质量管理/异常事件提报查询',
      );
      await router.push({
        path: '/mes/quality/abnormal/exception',
        query: {
          id: String(result.exceptionId || formData.value.id || ''),
          tabType: 'containmentOverdue',
        },
      });
      return;
    }
    message.success(result.message || '已确认提交资料');
  } finally {
    modalApi.setState({ loading: false });
  }
}

function handleReviewGroupTask(
  task: MesExceptionApi.GroupTask,
  actionCode: typeof TASK_REVIEW_ACTION_ACCEPT | typeof TASK_REVIEW_ACTION_RETURN,
) {
  if (!task.id) {
    return;
  }
  taskReviewTarget.value = task;
  taskReviewAction.value = actionCode;
  taskReviewMode.value = 'DISPATCHER';
  taskReviewOpinion.value = '';
  taskReviewOpen.value = true;
}

async function handleTaskReviewSave() {
  const task = taskReviewTarget.value;
  if (!task?.id) {
    return;
  }
  if (
    (taskReviewAction.value === MEMBER_CONFIRM_ACTION_DISAGREE ||
      taskReviewAction.value === TASK_REVIEW_ACTION_RETURN) &&
    !taskReviewOpinion.value.trim()
  ) {
    message.warning(
      taskReviewMode.value === 'DISPATCHER'
        ? '请填写退回原因'
        : '请填写不同意意见',
    );
    return;
  }
  const confirmed = await confirmAction(
    taskReviewIsPositive.value
      ? taskReviewMode.value === 'DISPATCHER'
        ? '确认接收该任务并标记完成？'
        : '确认通过该任务资料？'
      : taskReviewMode.value === 'DISPATCHER'
        ? '确认退回填写人重写？'
        : '确认不同意并退回填写人重写？',
  );
  if (!confirmed) {
    return;
  }
  if (taskReviewMode.value === 'DISPATCHER') {
    const dispatcherAction =
      taskReviewAction.value === TASK_REVIEW_ACTION_RETURN
        ? TASK_REVIEW_ACTION_RETURN
        : TASK_REVIEW_ACTION_ACCEPT;
    await reviewExceptionGroupTask({
      actionCode: dispatcherAction,
      id: task.id,
      opinion: taskReviewOpinion.value,
    });
  } else {
    const memberAction =
      taskReviewAction.value === MEMBER_CONFIRM_ACTION_DISAGREE
        ? MEMBER_CONFIRM_ACTION_DISAGREE
        : MEMBER_CONFIRM_ACTION_CONFIRM;
    await confirmExceptionGroupTaskMember({
      actionCode: memberAction,
      remark: taskReviewOpinion.value,
      taskId: task.id,
    });
  }
  await loadDetail(formData.value.id!, formData.value.tabType);
  taskReviewOpen.value = false;
  emit('success');
  message.success(
    taskReviewIsPositive.value ? '任务已确认' : '已退回填写人重写',
  );
}

function getTaskKey(task: MesExceptionApi.GroupTask) {
  return `${normalizeTaskType(task.taskType)}-${task.id || task.groupName || task.dispatchTime || 'new'}`;
}

function isTaskExpanded(task: MesExceptionApi.GroupTask) {
  return !!expandedTaskMap.value[getTaskKey(task)];
}

function toggleTaskExpanded(task: MesExceptionApi.GroupTask) {
  const key = getTaskKey(task);
  expandedTaskMap.value = {
    ...expandedTaskMap.value,
    [key]: !expandedTaskMap.value[key],
  };
}

function getTaskMemberKey(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return `${getTaskKey(task)}-${member.id || member.userId || member.deptName || 'member'}`;
}

function isTaskMemberExpanded(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return !!expandedTaskMap.value[getTaskMemberKey(task, member)];
}

function toggleTaskMemberExpanded(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  const key = getTaskMemberKey(task, member);
  expandedTaskMap.value = {
    ...expandedTaskMap.value,
    [key]: !expandedTaskMap.value[key],
  };
}

function isInvestigationSummaryMemberExpanded(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return !!investigationSummaryExpandedMap.value[getTaskMemberKey(task, member)];
}

function toggleInvestigationSummaryMemberExpanded(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  if (autoExpandInvestigationSignSummary.value) {
    return;
  }
  const key = getTaskMemberKey(task, member);
  investigationSummaryExpandedMap.value = {
    ...investigationSummaryExpandedMap.value,
    [key]: !investigationSummaryExpandedMap.value[key],
  };
}

function shouldShowInvestigationSummaryMemberDetail(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    autoExpandInvestigationSignSummary.value ||
    isInvestigationSummaryMemberExpanded(task, member)
  );
}

function getInvestigationSummaryExpandIcon(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return shouldShowInvestigationSummaryMemberDetail(task, member)
    ? 'lucide:chevron-up'
    : 'lucide:chevron-down';
}

function shouldShowInvestigationSignDetail(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    getInvestigationMemberConfirmOpinion(task, member) !== '-' ||
    !collapseInvestigationSignDetail.value ||
    isTaskMemberExpanded(task, member)
  );
}

function getLatestTaskReply(task: MesExceptionApi.GroupTask) {
  const replies = task.replies || [];
  return replies.length > 0 ? replies[replies.length - 1] : undefined;
}

function getTaskReplyCount(task: MesExceptionApi.GroupTask) {
  return task.replyCount ?? task.replies?.length ?? (task.submitTime ? 1 : 0);
}

function getTaskLastSubmitTime(task: MesExceptionApi.GroupTask) {
  return getLatestTaskReply(task)?.submitTime || task.submitTime || '-';
}

function getTaskLastSubmitter(task: MesExceptionApi.GroupTask) {
  return getLatestTaskReply(task)?.submitterUserName || task.submitterUserName || '-';
}

async function confirmPendingSignMembersBeforeFlow(taskType: ExceptionTaskType) {
  const pendingMembers = getPendingSignMembers(taskType);
  if (pendingMembers.length === 0) {
    return true;
  }
  const pendingText = pendingMembers
    .slice(0, 8)
    .map((member) =>
      getGroupMemberBusinessDeptName(member)
        ? `${getGroupMemberBusinessDeptName(member)}：${member.userName || '-'}`
        : member.userName || '-',
    )
    .join('、');
  const moreText =
    pendingMembers.length > 8 ? ` 等${pendingMembers.length}人` : '';
  const stepName =
    taskType === TASK_TYPE_ROOT_CAUSE
      ? '根因分析和纠正预防措施与标准化'
      : '临时小组围堵措施';
  return confirmAction(
    `${stepName}仍有会签人未完成：${pendingText}${moreText}。是否继续推送？`,
  );
}

function getPendingSignMembers(taskType: ExceptionTaskType) {
  return (formData.value.groupTasks || [])
    .filter((task) => normalizeTaskType(task.taskType) === taskType)
    .flatMap((task) =>
      (task.members || []).filter(
        (member) =>
          (member.deptName || member.deptId || member.userName || member.userId) &&
          !isTaskMemberSigned(task, member),
      ),
    );
}

function isTaskMemberSigned(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    !!getInvestigationMemberFinishTimeValue(task, member) ||
    getMemberReplies(task, member).length > 0 ||
    [MEMBER_CONFIRM_SUBMITTED, MEMBER_CONFIRM_CONFIRMED].includes(
      member.confirmStatus || '',
    )
  );
}

function isGroupTaskMemberPending(member: MesExceptionApi.GroupTaskMember) {
  return (member.confirmStatus || MEMBER_CONFIRM_PENDING) === MEMBER_CONFIRM_PENDING;
}

function isCurrentUserOriginalGroupTaskMember(
  member: MesExceptionApi.GroupTaskMember,
) {
  const userId = currentUserId.value;
  return !!userId && Number(member.userId) === userId;
}

function isCurrentUserGroupTaskMember(member: MesExceptionApi.GroupTaskMember) {
  const userId = currentUserId.value;
  return (
    !!userId &&
    (Number(member.userId) === userId || Number(member.delegateUserId) === userId)
  );
}

function canDelegateGroupTaskMember(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    canSubmitGroupTask(task) &&
    isGroupTaskMemberPending(member) &&
    isCurrentUserOriginalGroupTaskMember(member)
  );
}

function canEditInvestigationSignMember(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    normalizeTaskType(task.taskType) === TASK_TYPE_INVESTIGATION &&
    canSubmitGroupTask(task) &&
    isGroupTaskMemberPending(member) &&
    isCurrentUserGroupTaskMember(member)
  );
}

function canEditRootCauseSignMember(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    normalizeTaskType(task.taskType) === TASK_TYPE_ROOT_CAUSE &&
    canSubmitGroupTask(task) &&
    isGroupTaskMemberPending(member) &&
    isCurrentUserGroupTaskMember(member)
  );
}

function getMemberHandlerIdSet(member: MesExceptionApi.GroupTaskMember) {
  return new Set(
    [member.userId, member.delegateUserId, member.actualHandlerUserId]
      .filter((value): value is number => value !== undefined && value !== null)
      .map((value) => Number(value)),
  );
}

function getMemberReplies(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  const handlerIds = getMemberHandlerIdSet(member);
  return (task.replies || [])
    .filter((reply) => {
      if (member.id && reply.memberId) {
        return Number(reply.memberId) === Number(member.id);
      }
      if (reply.memberUserId) {
        return Number(reply.memberUserId) === Number(member.userId);
      }
      return !!reply.submitterUserId && handlerIds.has(Number(reply.submitterUserId));
    })
    .sort(compareTaskReplyTime);
}

function getMemberHandlerDisplay(member: MesExceptionApi.GroupTaskMember) {
  const assignedName = member.userName || '-';
  if (
    member.actualHandlerUserId &&
    member.userId &&
    Number(member.actualHandlerUserId) !== Number(member.userId)
  ) {
    return `${member.actualHandlerUserName || '-'}代${assignedName}办理`;
  }
  return assignedName;
}

function getMemberDelegateText(member: MesExceptionApi.GroupTaskMember) {
  return member.delegateUserName ? `委托：${member.delegateUserName}` : '';
}

function getReplySubmitterDisplay(
  reply: MesExceptionApi.GroupTaskReply,
  member: MesExceptionApi.GroupTaskMember,
) {
  const memberUserId = reply.memberUserId || member.userId;
  const memberUserName = reply.memberUserName || member.userName || '-';
  if (
    reply.submitterUserId &&
    memberUserId &&
    Number(reply.submitterUserId) !== Number(memberUserId)
  ) {
    return `${reply.submitterUserName || '-'}代${memberUserName}办理`;
  }
  return reply.submitterUserName || memberUserName || '-';
}

function compareTaskReplyTime(
  left: MesExceptionApi.GroupTaskReply,
  right: MesExceptionApi.GroupTaskReply,
) {
  const leftReplyNo = Number(left.replyNo || 0);
  const rightReplyNo = Number(right.replyNo || 0);
  if (leftReplyNo !== rightReplyNo) {
    return leftReplyNo - rightReplyNo;
  }
  const leftTime =
    parseDateTimeValue(left.submitTime || left.actualFinishTime) || 0;
  const rightTime =
    parseDateTimeValue(right.submitTime || right.actualFinishTime) || 0;
  if (leftTime !== rightTime) {
    return leftTime - rightTime;
  }
  return Number(left.id || 0) - Number(right.id || 0);
}

function getTaskReturnInfo(task: MesExceptionApi.GroupTask) {
  const candidates = [
    ...(task.replies || [])
      .filter((reply) => reply.replyStatus === 'RETURNED' && reply.reviewOpinion)
      .map((reply) => ({
        opinion: reply.reviewOpinion,
        returnTime: reply.reviewTime || reply.submitTime || '',
        returnUserName: reply.reviewerUserName || task.reviewerUserName || '-',
        timeValue: parseDateTimeValue(reply.reviewTime || reply.submitTime) || 0,
      })),
    ...(task.confirmLogs || [])
      .filter(
        (log) =>
          log.confirmAction === MEMBER_CONFIRM_ACTION_DISAGREE &&
          log.confirmOpinion,
      )
      .map((log) => ({
        opinion: log.confirmOpinion,
        returnTime: log.confirmTime || '',
        returnUserName: log.userName || '-',
        timeValue: parseDateTimeValue(log.confirmTime) || 0,
      })),
    {
      opinion: task.reviewOpinion,
      returnTime: task.reviewTime || '',
      returnUserName: task.reviewerUserName || '-',
      timeValue: parseDateTimeValue(task.reviewTime) || 0,
    },
  ].filter((item) => item.opinion);
  candidates.sort((left, right) => right.timeValue - left.timeValue);
  const latest = candidates[0];
  return {
    opinion: latest?.opinion || '',
    returnTime: latest?.returnTime || '-',
    returnUserName: latest?.returnUserName || '-',
  };
}

function getLatestMemberReply(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return [...(task.replies || [])]
    .reverse()
    .find((reply) => Number(reply.submitterUserId) === Number(member.userId));
}

function getConfirmHandleOpinion() {
  const logs = [...(formData.value.flowLogs || [])].reverse();
  const confirmLog = logs.find(
    (log) =>
      log.actionCode === 'HANDLE' &&
      !!log.opinion &&
      (log.fromStatus === 'CONFIRMING' ||
        log.fromNodeCode === 'QUALITY_CONFIRM' ||
        log.toStatus === 'CONTAINMENT' ||
        log.toNodeCode === 'CONTAINMENT'),
  );
  return confirmLog?.opinion || '';
}

function getInvestigationMemberConfirmOpinion(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  const latestReply = getLatestMemberReply(task, member);
  return (
    member.confirmRemark ||
    member.actionDescription ||
    latestReply?.remark ||
    latestReply?.actionDescription ||
    '-'
  );
}

function getInvestigationMemberFinishTime(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return getInvestigationMemberFinishTimeValue(task, member) || '-';
}

function getInvestigationMemberFinishTimeValue(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  return (
    member.actualFinishTime ||
    getLatestMemberReply(task, member)?.actualFinishTime
  );
}

function getSignMemberDuration(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  const startTime = parseDateTimeValue(task.dispatchTime);
  if (startTime === undefined) {
    return '-';
  }
  const finishTime = parseDateTimeValue(
    getInvestigationMemberFinishTimeValue(task, member),
  );
  const endTime = finishTime ?? durationClock.value;
  return formatDuration(Math.max(0, endTime - startTime));
}

function isSignMemberOverdue(
  task: MesExceptionApi.GroupTask,
  member: MesExceptionApi.GroupTaskMember,
) {
  const deadlineTime = parseDateTimeValue(task.containmentDeadline);
  if (deadlineTime === undefined) {
    return false;
  }
  const finishTime = parseDateTimeValue(
    getInvestigationMemberFinishTimeValue(task, member),
  );
  return (finishTime ?? durationClock.value) > deadlineTime;
}

function getReplyDuration(
  task: MesExceptionApi.GroupTask,
  reply: MesExceptionApi.GroupTaskReply,
) {
  const startTime = parseDateTimeValue(task.dispatchTime);
  if (startTime === undefined) {
    return '-';
  }
  const finishTime = parseDateTimeValue(reply.actualFinishTime || reply.submitTime);
  if (finishTime === undefined) {
    return '-';
  }
  return formatDuration(Math.max(0, finishTime - startTime));
}

function isReplyOverdue(
  task: MesExceptionApi.GroupTask,
  reply: MesExceptionApi.GroupTaskReply,
) {
  const deadlineTime = parseDateTimeValue(task.containmentDeadline);
  const finishTime = parseDateTimeValue(reply.actualFinishTime || reply.submitTime);
  return (
    deadlineTime !== undefined &&
    finishTime !== undefined &&
    finishTime > deadlineTime
  );
}

function getTaskLastRootCause(task: MesExceptionApi.GroupTask) {
  return getLatestTaskReply(task)?.rootCause || task.rootCause || '-';
}

function getTaskLastPreventiveAction(task: MesExceptionApi.GroupTask) {
  return getLatestTaskReply(task)?.preventiveAction || task.preventiveAction || '-';
}

function getTaskAttachmentCount(task: MesExceptionApi.GroupTask) {
  return getLatestTaskReply(task)?.attachmentUrls?.length || task.attachmentUrls?.length || 0;
}

function getTaskHandlingDuration(task: MesExceptionApi.GroupTask) {
  const startTime = parseDateTimeValue(task.dispatchTime);
  if (startTime === undefined) {
    return '-';
  }
  const finishTime = parseDateTimeValue(
    getLatestTaskReply(task)?.actualFinishTime || task.actualFinishTime,
  );
  return formatDuration(Math.max(0, (finishTime ?? durationClock.value) - startTime));
}

function isTaskHandlingOverdue(task: MesExceptionApi.GroupTask) {
  const deadlineTime = parseDateTimeValue(task.containmentDeadline);
  if (deadlineTime === undefined) {
    return false;
  }
  const finishTime = parseDateTimeValue(
    getLatestTaskReply(task)?.actualFinishTime || task.actualFinishTime,
  );
  return (finishTime ?? durationClock.value) > deadlineTime;
}

function isContainmentTaskOverdue(task: MesExceptionApi.GroupTask) {
  const deadlineTime = parseDateTimeValue(task.containmentDeadline);
  return deadlineTime !== undefined && durationClock.value > deadlineTime;
}

function formatDuration(durationMs: number) {
  const totalMinutes = Math.max(0, Math.floor(durationMs / 60_000));
  if (totalMinutes <= 0) {
    return '不足1分钟';
  }
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;
  const parts: string[] = [];
  if (days > 0) {
    parts.push(`${days}天`);
  }
  if (hours > 0) {
    parts.push(`${hours}小时`);
  }
  if (minutes > 0 || parts.length === 0) {
    parts.push(`${minutes}分钟`);
  }
  return parts.join('');
}

function parseDateTimeValue(value?: string) {
  if (!value) {
    return undefined;
  }
  const normalized = value.includes('T') ? value : value.replace(' ', 'T');
  const time = new Date(normalized).getTime();
  return Number.isFinite(time) ? time : undefined;
}

function getTaskMemberNames(task: MesExceptionApi.GroupTask) {
  const names = (task.members || []).map((member) => member.userName).filter(Boolean);
  return names.length > 0 ? names.join('、') : '-';
}

function getCurrentUserTaskMember(task: MesExceptionApi.GroupTask) {
  const userId = currentUserId.value;
  return (
    (task.members || []).find(
      (member) =>
        userId &&
        isCurrentUserGroupTaskMember(member) &&
        isGroupTaskMemberPending(member),
    ) ||
    (task.members || []).find(
      (member) => userId && isCurrentUserGroupTaskMember(member),
    )
  );
}

function getTaskDeptSignerNames(task: MesExceptionApi.GroupTask) {
  const names = (task.members || [])
    .map((member) =>
      getGroupMemberBusinessDeptName(member)
        ? `${getGroupMemberBusinessDeptName(member)}：${member.userName || '-'}`
        : member.userName,
    )
    .filter(Boolean);
  return names.length > 0 ? names.join('、') : '-';
}

function getTaskSignerNames(task: MesExceptionApi.GroupTask) {
  const names = (task.members || [])
    .map((member) => member.userName)
    .filter(Boolean);
  return names.length > 0 ? names.join('、') : '-';
}

function getMemberConfirmStatusMeta(
  status?: string,
  task?: MesExceptionApi.GroupTask,
) {
  if (
    task?.taskStatus === 'ACCEPTED' &&
    (status || MEMBER_CONFIRM_PENDING) === MEMBER_CONFIRM_PENDING
  ) {
    return { color: 'success', label: '随组完成' };
  }
  const statusMap: Record<string, { color: string; label: string }> = {
    [MEMBER_CONFIRM_CONFIRMED]: { color: 'success', label: '已办理' },
    [MEMBER_CONFIRM_DISAGREED]: { color: 'error', label: '不同意' },
    [MEMBER_CONFIRM_EXPIRED]: { color: 'error', label: '已过期' },
    [MEMBER_CONFIRM_PENDING]: { color: 'default', label: '待办理' },
    [MEMBER_CONFIRM_SUBMITTED]: { color: 'processing', label: '已提交' },
  };
  return statusMap[status || MEMBER_CONFIRM_PENDING] || statusMap[MEMBER_CONFIRM_PENDING]!;
}

function getRootTaskExecutor(task: MesExceptionApi.GroupTask) {
  return task.rootCauseOwnerName || getTaskDeptSignerNames(task);
}

function openLogDrawer(mode: 'audit' | 'operation') {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
}

function openBusinessAuditLog() {
  openLogDrawer('audit');
}

function openAuditLog() {
  if (!formData.value.processInstanceId) {
    message.warning('当前异常事件未发起审批流程，暂无工作流');
    return;
  }
  processAuditModalApi
    .setData({
      extraActivityNodes: buildExceptionAuditSupplementNodes(),
      processInstanceId: formData.value.processInstanceId,
      title: `工作流 - ${formData.value.exceptionNo || '异常事件'}`,
    })
    .open();
}

function buildExceptionAuditSupplementNodes(): AuditNode[] {
  const nodes: AuditNode[] = [];
  appendAuditNode(nodes, {
    id: 'qms-exception-team-members',
    name: '临时小组成员',
    reason: '异常事件临时小组成员',
    users: uniqueAuditUsers(
      (formData.value.teamMembers || []).map((member) => ({
        id: member.userId,
        nickname: member.userName || '',
      })),
    ),
  });
  appendAuditNode(nodes, {
    id: 'qms-exception-investigation-signers',
    name: '临时小组围堵措施会签',
    reason: '围堵措施会签人',
    time: latestGroupMemberTime(investigationTasks.value),
    users: buildGroupTaskAuditUsers(investigationTasks.value),
  });
  appendAuditNode(nodes, {
    id: 'qms-exception-root-cause-signers',
    name: '根因分析纠正预防会签',
    reason: '根因纠正会签人',
    time: latestGroupMemberTime(rootCauseTasks.value),
    users: buildGroupTaskAuditUsers(rootCauseTasks.value),
  });
  appendAuditNode(nodes, {
    id: 'qms-exception-result-uploader',
    name: '执行结果上传人',
    reason: formData.value.correctivePreventiveResult || '执行结果上传',
    time: formData.value.resultUploadTime,
    users: uniqueAuditUsers([
      {
        id: formData.value.resultUploaderId,
        nickname: formData.value.resultUploaderName || '',
      },
    ]),
  });
  appendAuditNode(nodes, {
    id: 'qms-exception-copy-users',
    name: '执行结果抄送人',
    reason: '执行结果抄送',
    time: formData.value.resultUploadTime,
    users: buildCommaSeparatedAuditUsers(
      formData.value.copyUserIds,
      formData.value.copyUserNames,
    ),
  });
  return nodes;
}

function appendAuditNode(
  nodes: AuditNode[],
  options: {
    id: string;
    name: string;
    reason?: string;
    time?: string;
    users: AuditUser[];
  },
) {
  const users = uniqueAuditUsers(options.users);
  if (users.length === 0) {
    return;
  }
  nodes.push({
    endTime: options.time,
    id: options.id,
    name: options.name,
    nodeType: BpmNodeTypeEnum.USER_TASK_NODE,
    startTime: options.time,
    status: BpmTaskStatusEnum.APPROVE,
    tasks: users.map((user, index) => ({
      assigneeUser: user,
      id: `${options.id}-${user.id || user.nickname || index}`,
      reason: options.reason,
      status: BpmTaskStatusEnum.APPROVE,
    })),
  });
}

function buildGroupTaskAuditUsers(tasks: MesExceptionApi.GroupTask[] = []) {
  const users: AuditUser[] = [];
  tasks.forEach((task) => {
    (task.members || []).forEach((member) => {
      users.push({
        id: member.actualHandlerUserId || member.userId,
        nickname: member.actualHandlerUserName || member.userName || '',
      });
    });
    (task.confirmLogs || []).forEach((log) => {
      users.push({
        id: log.userId,
        nickname: log.userName || '',
      });
    });
  });
  return uniqueAuditUsers(users);
}

function latestGroupMemberTime(tasks: MesExceptionApi.GroupTask[] = []) {
  const times = tasks
    .flatMap((task) => [
      task.reviewTime,
      task.submitTime,
      ...(task.members || []).map((member) => member.confirmTime),
      ...(task.confirmLogs || []).map((log) => log.confirmTime),
    ])
    .filter(Boolean)
    .map((time) => String(time));
  const sortedTimes = times.sort();
  return sortedTimes[sortedTimes.length - 1];
}

function buildCommaSeparatedAuditUsers(ids?: string, names?: string) {
  const idList = splitCommaText(ids);
  return splitCommaText(names).map((name, index) => ({
    id: idList[index] || name,
    nickname: name,
  }));
}

function splitCommaText(value?: string) {
  return (value || '')
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function uniqueAuditUsers(users: AuditUser[] = []) {
  const seen = new Set<string>();
  return users.reduce<AuditUser[]>((result, user) => {
    const nickname = (user.nickname || '').trim();
    if (!nickname) {
      return result;
    }
    const key = String(user.id || nickname);
    if (seen.has(key)) {
      return result;
    }
    seen.add(key);
    result.push({ ...user, nickname });
    return result;
  }, []);
}

function confirmAction(content: string) {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      centered: true,
      content,
      okText: '确定',
      title: '操作确认',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
    });
  });
}

function mergeDictOptions(
  dictType: string,
  fallback: Array<{ label: string; value: string }>,
) {
  const dictOptions = getDictOptions(dictType, 'string') as Array<{
    label?: string;
    value?: string;
  }>;
  const optionMap = new Map<string, { label: string; value: string }>();
  const labelSet = new Set<string>();
  [...dictOptions, ...fallback]
    .filter((item) => item.value)
    .forEach((item) => {
      const label = item.label || String(item.value);
      const value = String(item.value);
      if (optionMap.has(value) || labelSet.has(label)) {
        return;
      }
      optionMap.set(value, { label, value });
      labelSet.add(label);
    });
  return Array.from(optionMap.values());
}

function getOptionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  if (!value) {
    return '-';
  }
  return options.find((item) => item.value === value)?.label || value;
}

</script>

<template>
  <Modal>
    <NcrModal @select="handleSelectNcr" />
    <UserSelectModalComp
      class="w-3/5"
      :modal-z-index="5000"
      :multiple="false"
      title="选择人员"
      @confirm="handleUserConfirm"
    />
    <ProcessAuditModal />
    <BusinessLogDrawer
      v-model:open="logDrawerVisible"
      :logs="formData.flowLogs || []"
      :mode="logDrawerMode"
      :title="logDrawerMode === 'operation' ? '操作日志' : '审批日志'"
    />
    <AntModal
      v-model:open="returnModalOpen"
      :confirm-loading="returnSubmitting"
      ok-text="确认退回"
      title="退回意见"
      width="520px"
      @ok="handleReturnSave"
    >
      <div class="qms-exception-modal-grid">
        <label>退回节点</label>
        <div class="qms-exception-readonly-value">
          {{ returnTargetName }}
        </div>
        <label class="qms-exception-modal-label--tall">退回意见</label>
        <Input.TextArea
          v-model:value="returnOpinion"
          placeholder="请填写退回意见"
          :rows="4"
        />
      </div>
    </AntModal>
    <AntModal
      v-model:open="taskReplyOpen"
      :title="
        normalizeTaskType(taskReplyTarget?.taskType) === TASK_TYPE_ROOT_CAUSE
          ? '提交本责任单位根因纠正'
          : '提交本部门围堵措施'
      "
      width="760px"
      @ok="handleTaskReplySave"
    >
      <div class="qms-exception-modal-grid">
        <template
          v-if="normalizeTaskType(taskReplyTarget?.taskType) === TASK_TYPE_ROOT_CAUSE"
        >
          <label>4M1E归类</label>
          <Select
            v-model:value="taskReplyForm.rootCauseCategory"
            allow-clear
            :options="getDictOptions(QMS_EXCEPTION_DICT.rootCauseCategory, 'string')"
            placeholder="请选择分类"
          />
          <label class="qms-exception-modal-label--tall">根因分析</label>
          <Input.TextArea
            v-model:value="taskReplyForm.rootCause"
            placeholder="请填写根因分析"
            :rows="2"
          />
          <label class="qms-exception-modal-label--tall">纠正预防</label>
          <Input.TextArea
            v-model:value="taskReplyForm.preventiveAction"
            placeholder="请填写纠正预防措施与标准化"
            :rows="2"
          />
        </template>
        <template v-else>
          <label class="qms-exception-modal-label--tall">措施说明</label>
          <Input.TextArea
            v-model:value="taskReplyForm.actionDescription"
            placeholder="请填写围堵措施说明"
            :rows="2"
          />
        </template>
        <label>附件证明</label>
        <FileUpload
          v-model="taskReplyForm.attachmentUrls"
          :accept="attachmentAcceptTypes"
          directory="mes/qms/exception"
          :max-number="20"
          :max-size="20"
          multiple
          show-description
          @preview="handleAttachmentPreview"
        />
      </div>
    </AntModal>
    <AntModal
      v-model:open="investigationSignConfirmOpen"
      ok-text="确认会签"
      title="确认会签"
      width="520px"
      @ok="handleInvestigationSignConfirmSave"
    >
      <div class="qms-exception-modal-grid">
        <label class="qms-exception-modal-label--tall">确认意见</label>
        <Input.TextArea
          v-model:value="investigationSignConfirmOpinion"
          placeholder="请填写确认意见"
          :rows="4"
        />
      </div>
    </AntModal>

    <AntModal
      v-model:open="taskReviewOpen"
      :ok-text="taskReviewOkText"
      :title="taskReviewModalTitle"
      width="520px"
      @ok="handleTaskReviewSave"
    >
      <Input.TextArea
        v-model:value="taskReviewOpinion"
        :placeholder="taskReviewPlaceholder"
        :rows="4"
      />
    </AntModal>

    <div class="qms-ncr-detail qms-exception-detail" :class="{ 'is-loading': loading }">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <Tag color="blue" class="!m-0">
            {{ formData.currentNodeName || formData.status || '待提报' }}
          </Tag>
          <Tag v-if="currentUserTaskTodoText" color="warning" class="!m-0">
            {{ currentUserTaskTodoText }}
          </Tag>
        </div>
        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">异常事件提报单</div>
          <div class="qms-ncr-title-panel__subtitle">
            <div class="qms-ncr-title-panel__subtitle-item">
              单号：{{ formData.exceptionNo || '新建中' }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              状态/节点：{{ formData.currentNodeName || formData.status || '待提报' }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              异常等级：{{ exceptionLevelLabel }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              关联NCR：{{ formData.relatedNcrNo || '-' }}
            </div>
          </div>
        </div>
        <div class="qms-ncr-toolbar__actions">
          <div class="qms-ncr-toolbar__log-icons">
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--audit"
              size="small"
              title="审批日志"
              @click="openBusinessAuditLog"
            >
              <IconifyIcon icon="lucide:clipboard-check" />
            </Button>
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--workflow"
              size="small"
              title="工作流流程图"
              @click="openAuditLog"
            >
              <IconifyIcon icon="lucide:git-fork" />
            </Button>
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--operation"
              size="small"
              title="操作日志"
              @click="openLogDrawer('operation')"
            >
              <IconifyIcon icon="lucide:history" />
            </Button>
          </div>
          <Button
            v-for="action in leadingToolbarActions"
            :key="action.key"
            class="qms-ncr-toolbar-action"
            :danger="action.danger"
            size="small"
            :title="action.title"
            :type="action.type"
            @click="action.onClick"
          >
            <IconifyIcon v-if="action.key === 'primary'" icon="lucide:check" class="mr-1" />
            <IconifyIcon v-else-if="action.key === 'return'" icon="lucide:rotate-ccw" class="mr-1" />
            <IconifyIcon v-else-if="action.key === 'windowClose'" icon="lucide:x" class="mr-1" />
            <span>{{ action.label }}</span>
          </Button>
          <Dropdown
            v-if="overflowToolbarActions.length > 0"
            placement="bottomRight"
            :trigger="['click']"
          >
            <Button
              class="qms-ncr-toolbar-action"
              size="small"
              title="更多"
            >
              <span>更多</span>
            </Button>
            <template #overlay>
              <Menu>
                <Menu.Item
                  v-for="action in overflowToolbarActions"
                  :key="action.key"
                  :danger="action.danger"
                  @click="action.onClick"
                >
                  {{ action.label }}
                </Menu.Item>
              </Menu>
            </template>
          </Dropdown>
          <Button
            v-for="action in trailingToolbarActions"
            :key="action.key"
            class="qms-ncr-toolbar-action"
            :danger="action.danger"
            size="small"
            :title="action.title"
            :type="action.type"
            @click="action.onClick"
          >
            <IconifyIcon v-if="action.key === 'primary'" icon="lucide:check" class="mr-1" />
            <IconifyIcon v-else-if="action.key === 'return'" icon="lucide:rotate-ccw" class="mr-1" />
            <IconifyIcon v-else-if="action.key === 'windowClose'" icon="lucide:x" class="mr-1" />
            <span>{{ action.label }}</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading" class="detail-spin">
        <div class="detail-content">
          <div class="qms-exception-workbench">
        <Form
          ref="formRef"
          class="qms-exception-form"
          :model="formData"
          layout="vertical"
        >
          <section class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>一、发现</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label>发现部门</label>
              <div class="erp-form-value">
                <TreeSelect
                  v-if="canEditDiscoverStep"
                  v-model:value="formData.discoverDeptId"
                  allow-clear
                  class="w-full"
                  :field-names="{ children: 'children', label: 'name', value: 'id' }"
                  placeholder="请选择发现部门"
                  show-search
                  :tree-data="deptTreeData"
                  tree-default-expand-all
                  @change="handleDiscoverDeptChange"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.discoverDeptName || '-' }}
                </div>
              </div>
              <label>发现时间</label>
              <div class="erp-form-value">
                <DatePicker
                  v-if="canEditDiscoverStep"
                  v-model:value="formData.discoverTime"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择发现时间"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.discoverTime || '-' }}
                </div>
              </div>
              <label>发现人</label>
              <div class="erp-form-value">
                <template v-if="canEditDiscoverStep">
                  <Input
                    v-model:value="formData.discovererName"
                    placeholder="当前用户"
                    readonly
                  />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('discoverer')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.discovererName || '-' }}
                </div>
              </div>

              <label class="erp-form-label--tall">异常描述</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Form.Item
                  class="qms-exception-form-item"
                  name="description"
                  :rules="[{ required: true, message: '请填写异常描述' }]"
                >
                  <Input.TextArea
                    v-if="canEditDiscoverStep"
                    v-model:value="formData.description"
                    placeholder="请描述异常现象、发生经过和当前状态"
                    :rows="4"
                  />
                  <div
                    v-else
                    class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                  >
                    {{ formData.description || '-' }}
                  </div>
                </Form.Item>
              </div>
            </div>
          </section>

          <section v-if="showConfirmStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>二、确认</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label>确认部门</label>
              <div class="erp-form-value">
                <TreeSelect
                  v-if="canEditConfirmStep"
                  v-model:value="formData.confirmDeptId"
                  allow-clear
                  class="w-full"
                  :field-names="{ children: 'children', label: 'name', value: 'id' }"
                  placeholder="请选择确认部门"
                  show-search
                  :tree-data="deptTreeData"
                  tree-default-expand-all
                  @change="handleConfirmDeptChange"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.confirmDeptName || '-' }}
                </div>
              </div>
              <label>确认时间</label>
              <div class="erp-form-value">
                <DatePicker
                  v-if="canEditConfirmStep"
                  v-model:value="formData.confirmTime"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择确认时间"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.confirmTime || '-' }}
                </div>
              </div>
              <label>确认人</label>
              <div class="erp-form-value">
                <template v-if="canEditConfirmStep">
                  <Input
                    v-model:value="formData.confirmerName"
                    placeholder="请选择确认人"
                    readonly
                  />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('confirmer')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.confirmerName || '-' }}
                </div>
              </div>
              <template v-if="canEditConfirmStep">
                <label>确认结论</label>
                <div class="erp-form-value erp-form-value--span-5">
                  <Radio.Group
                    v-model:value="confirmActionCode"
                    :options="confirmActionOptions"
                  />
                </div>
              </template>
              <label>异常类别</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Form.Item
                  class="qms-exception-form-item"
                  name="exceptionType"
                  :rules="[{ required: true, message: '请选择异常类别' }]"
                >
                  <Radio.Group
                    v-model:value="formData.exceptionType"
                    class="qms-exception-radio-wrap"
                    :disabled="confirmStepDisabled"
                    :options="exceptionTypeOptions"
                  />
                </Form.Item>
              </div>

              <label>异常等级</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Form.Item
                  class="qms-exception-form-item"
                  name="exceptionLevel"
                  :rules="[{ required: true, message: '请选择异常等级' }]"
                >
                  <Radio.Group
                    v-model:value="formData.exceptionLevel"
                    class="qms-exception-radio-line"
                    :disabled="confirmStepDisabled"
                    :options="exceptionLevelOptions"
                  />
                </Form.Item>
              </div>
              <label>关联产品</label>
              <div class="erp-form-value">
                <Form.Item
                  class="qms-exception-form-item"
                  name="isRelatedProduct"
                  :rules="[{ required: true, message: '请选择关联产品' }]"
                >
                  <Radio.Group
                    v-model:value="formData.isRelatedProduct"
                    :disabled="confirmStepDisabled || relatedNcrLocked"
                  >
                    <Radio :value="false">否</Radio>
                    <Radio :value="true">是</Radio>
                  </Radio.Group>
                </Form.Item>
              </div>

              <label>关联NCR</label>
              <div class="erp-form-value erp-form-value--span-3">
                <div
                  v-if="canEditConfirmStep && !relatedNcrLocked"
                  class="qms-exception-linked-ncr"
                >
                  <Input
                    v-model:value="formData.relatedNcrNo"
                    :placeholder="
                      formData.isRelatedProduct
                        ? '选择已有产品 NCR'
                        : '选择已有原材料处置单'
                    "
                    readonly
                  >
                    <template #suffix>
                      <Button
                        class="qms-exception-inline-icon-btn"
                        size="small"
                        :title="relatedNcrLocked ? relatedNcrLockMessage : '选择关联处置单'"
                        type="text"
                        @click.stop="openNcrSelector"
                      >
                        <IconifyIcon icon="lucide:search" />
                      </Button>
                    </template>
                  </Input>
                  <Button
                    v-if="formData.relatedNcrNo"
                    class="qms-exception-inline-icon-btn"
                    size="small"
                    title="查看关联处置单"
                    type="text"
                    @click.stop="openLinkedNcr"
                  >
                    <IconifyIcon icon="lucide:external-link" />
                  </Button>
                </div>
                <div v-else class="qms-exception-linked-ncr">
                  <div class="qms-exception-readonly-value qms-exception-readonly-value--link">
                    {{ formData.relatedNcrNo || '-' }}
                  </div>
                  <Button
                    v-if="formData.relatedNcrNo"
                    class="qms-exception-inline-icon-btn"
                    size="small"
                    title="查看关联处置单"
                    type="text"
                    @click.stop="openLinkedNcr"
                  >
                    <IconifyIcon icon="lucide:external-link" />
                  </Button>
                </div>
                <div v-if="relatedNcrLocked && formData.id" class="qms-exception-field-tip">
                  {{ relatedNcrLockMessage }}
                </div>
              </div>
              <label>临时小组负责人</label>
              <div class="erp-form-value erp-form-value--span-5">
                <template v-if="canEditConfirmStep">
                  <Input v-model:value="approvalForm.nextHandlerUserName" readonly />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('nextHandler')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.containmentOwnerName || approvalForm.nextHandlerUserName || '-' }}
                </div>
              </div>
              <template v-if="showInvestigationAssignmentRows">
                <label>临时小组成员</label>
                <div class="erp-form-value erp-form-value--span-5">
                  <div
                    v-if="canMaintainInvestigationTasks"
                    class="qms-exception-linked-ncr"
                  >
                    <Input
                      :value="
                        investigationAssignmentTask
                          ? getTaskSignerNames(investigationAssignmentTask)
                          : ''
                      "
                      placeholder="请选择临时小组成员"
                      readonly
                    />
                    <Button
                      class="qms-exception-inline-select"
                      size="small"
                      type="link"
                      @click="openAssignmentMemberMultiSelector(TASK_TYPE_INVESTIGATION)"
                    >
                      多选临时小组成员
                    </Button>
                  </div>
                  <div v-else class="qms-exception-readonly-value">
                    {{
                      investigationAssignmentTask
                        ? getTaskSignerNames(investigationAssignmentTask)
                        : '-'
                    }}
                  </div>
                </div>
              </template>
              <label>处理期限</label>
              <div class="erp-form-value erp-form-value--span-5">
                <DatePicker
                  v-if="canEditContainmentDeadline"
                  v-model:value="containmentDeadlineValue"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择处理期限"
                />
                <span v-else>{{ containmentDeadlineValue || '-' }}</span>
              </div>
              <label>相关附件</label>
              <div class="erp-form-value erp-form-value--span-5">
                <FileUpload
                  v-if="canEditConfirmRelatedAttachment"
                  v-model="confirmAttachmentUrls"
                  :accept="attachmentAcceptTypes"
                  directory="mes/qms/exception"
                  :max-number="20"
                  :max-size="20"
                  multiple
                  show-description
                  @preview="handleAttachmentPreview"
                />
                <div v-else class="qms-exception-attachment-list">
                  <template v-if="confirmAttachmentUrls.length > 0">
                    <Button
                      v-for="(url, index) in confirmAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </template>
                  <span v-else>-</span>
                </div>
              </div>
              <label class="erp-form-label--tall">确认意见</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditConfirmStep"
                  v-model:value="approvalForm.opinion"
                  placeholder="请填写确认意见"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ getConfirmHandleOpinion() || '-' }}
                </div>
              </div>
            </div>
          </section>

          <section v-if="showContainmentStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>三、临时小组围堵措施会签</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label class="erp-form-label--tall">围堵措施内容</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditContainmentOwnerStep"
                  v-model:value="formData.containmentAction"
                  placeholder="请填写围堵措施内容"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ formData.containmentAction || '-' }}
                </div>
              </div>
              <label>围堵措施附件</label>
              <div class="erp-form-value erp-form-value--span-5">
                <FileUpload
                  v-if="canEditContainmentAttachment"
                  v-model="containmentAttachmentUrls"
                  :accept="attachmentAcceptTypes"
                  directory="mes/qms/exception"
                  :max-number="20"
                  :max-size="20"
                  multiple
                  show-description
                  @preview="handleAttachmentPreview"
                />
                <div v-else class="qms-exception-attachment-list">
                  <template v-if="containmentAttachmentUrls.length > 0">
                    <Button
                      v-for="(url, index) in containmentAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </template>
                  <span v-else>-</span>
                </div>
              </div>
            </div>
            <div
              v-if="showInvestigationReturnOpinions"
              class="erp-form-grid qms-exception-return-opinion-grid"
            >
              <label>退回意见</label>
              <div class="erp-form-value erp-form-value--span-5">
                <table class="qms-exception-return-opinion-table">
                  <thead>
                    <tr>
                      <th>退回意见</th>
                      <th>退回人</th>
                      <th>退回时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in investigationTaskReturnOpinions" :key="item.key">
                      <td>{{ item.opinion }}</td>
                      <td>{{ item.returnUserName }}</td>
                      <td>{{ item.returnTime }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-if="investigationTasks.length === 0" class="qms-exception-empty-panel">
              暂无临时小组
            </div>
            <div
              v-else-if="collapseInvestigationSignDetail"
              class="qms-exception-sign-summary"
            >
              <table class="qms-exception-sign-summary-table">
                <thead>
                  <tr>
                    <th>部门</th>
                    <th>名称</th>
                    <th>填写日期</th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="task in investigationTasks" :key="getTaskKey(task)">
                    <tr v-if="!task.members || task.members.length === 0">
                      <td colspan="3" class="qms-exception-sign-summary-empty">
                        暂无临时小组办理人
                      </td>
                    </tr>
                    <template v-else>
                      <template
                        v-for="member in task.members"
                        :key="member.id || `${task.id}-${member.userId}-${member.deptName}`"
                      >
                        <tr
                          class="qms-exception-sign-summary-row"
                          @click="toggleInvestigationSummaryMemberExpanded(task, member)"
                        >
                          <td>{{ getGroupMemberBusinessDeptName(member) || '-' }}</td>
                          <td>{{ member.userName || '-' }}</td>
                          <td>
                            <div
                              class="qms-exception-sign-summary-date"
                              :class="{
                                'qms-exception-sign-summary-date--danger':
                                  isSignMemberOverdue(task, member),
                              }"
                            >
                              <span>{{ getInvestigationMemberFinishTime(task, member) }}</span>
                              <em>办理时长：{{ getSignMemberDuration(task, member) }}</em>
                              <IconifyIcon
                                :icon="getInvestigationSummaryExpandIcon(task, member)"
                              />
                            </div>
                          </td>
                        </tr>
                        <tr
                          v-if="shouldShowInvestigationSummaryMemberDetail(task, member)"
                          class="qms-exception-sign-summary-detail-row"
                        >
                          <td colspan="3">
                            <div class="qms-exception-sign-summary-detail">
                              <div class="qms-exception-sign-summary-detail__head">
                                <strong>确认意见</strong>
                              </div>
                              <div
                                class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                              >
                                {{ getInvestigationMemberConfirmOpinion(task, member) }}
                              </div>
                            </div>
                          </td>
                        </tr>
                      </template>
                    </template>
                  </template>
                </tbody>
              </table>
            </div>
            <div v-else class="qms-exception-sign-grid">
              <template v-for="task in investigationTasks" :key="getTaskKey(task)">
                <div
                  v-if="!task.members || task.members.length === 0"
                  class="qms-exception-empty-panel"
                >
                  暂无临时小组办理人
                </div>
                <template v-else>
                  <div
                    v-for="member in task.members"
                    :key="member.id || `${task.id}-${member.userId}-${member.deptName}`"
                    class="qms-exception-sign-cell"
                    :class="{
                      'qms-exception-sign-cell--active': canEditInvestigationSignMember(
                        task,
                        member,
                      ),
                    }"
                  >
                    <div class="qms-exception-sign-cell__dept">
                      {{ getGroupMemberBusinessDeptName(member) || '-' }}
                    </div>
                    <div class="qms-exception-sign-cell__body">
                      <div class="qms-exception-sign-cell__tools">
                        <Tooltip title="展开/收起会签内容">
                          <Button
                            size="small"
                            type="link"
                            @click="toggleTaskMemberExpanded(task, member)"
                          >
                            <IconifyIcon
                              :icon="
                                isTaskMemberExpanded(task, member)
                                  ? 'lucide:chevron-up'
                                  : 'lucide:chevron-down'
                              "
                            />
                          </Button>
                        </Tooltip>
                        <Tooltip title="确认会签">
                          <Button
                            v-if="canEditInvestigationSignMember(task, member)"
                            size="small"
                            type="link"
                            @click="handleSubmitGroupTask(task, member)"
                          >
                            <IconifyIcon icon="lucide:check" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="委托代办">
                          <Button
                            v-if="canDelegateGroupTaskMember(task, member)"
                            size="small"
                            type="link"
                            @click="openGroupTaskDelegateSelector(task, member)"
                          >
                            <IconifyIcon icon="lucide:user-plus" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="不同意退回">
                          <Button
                            v-if="canReviewGroupTask(task)"
                            danger
                            size="small"
                            type="link"
                            @click="handleReviewGroupTask(task, 'RETURN')"
                          >
                            <IconifyIcon icon="lucide:rotate-ccw" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="确认完成">
                          <Button
                            v-if="canReviewGroupTask(task)"
                            size="small"
                            type="link"
                            @click="handleReviewGroupTask(task, 'ACCEPT')"
                          >
                            <IconifyIcon icon="lucide:check" />
                          </Button>
                        </Tooltip>
                      </div>
                      <div
                        v-if="shouldShowInvestigationSignDetail(task, member)"
                        class="qms-exception-sign-main"
                      >
                        <div class="qms-exception-sign-detail-title">确认意见</div>
                        <div
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                        >
                          {{ getInvestigationMemberConfirmOpinion(task, member) }}
                        </div>
                      </div>
                      <div class="qms-exception-sign-meta qms-exception-sign-meta--three">
                        <div class="qms-exception-sign-meta-item">
                          <span>部门</span>
                          <b>{{ getGroupMemberBusinessDeptName(member) || '-' }}</b>
                        </div>
                        <div class="qms-exception-sign-meta-item">
                          <span>成员名称</span>
                          <div class="qms-exception-sign-meta-value">
                            <b>{{ getMemberHandlerDisplay(member) }}</b>
                            <em v-if="getMemberDelegateText(member)">
                              {{ getMemberDelegateText(member) }}
                            </em>
                          </div>
                        </div>
                        <div
                          class="qms-exception-sign-meta-item"
                          :class="{
                            'qms-exception-sign-meta-item--danger': isSignMemberOverdue(
                              task,
                              member,
                            ),
                          }"
                        >
                          <span>填写日期</span>
                          <div
                            class="qms-exception-sign-meta-value"
                            :class="{
                              'qms-exception-sign-meta-value--danger':
                                isSignMemberOverdue(task, member),
                            }"
                          >
                            <b>{{ getInvestigationMemberFinishTime(task, member) }}</b>
                            <em>办理时长：{{ getSignMemberDuration(task, member) }}</em>
                          </div>
                        </div>
                      </div>
                      <div
                        v-if="isTaskMemberExpanded(task, member)"
                        class="qms-exception-sign-history"
                      >
                        <div
                          v-if="getMemberReplies(task, member).length === 0"
                          class="qms-exception-empty-panel"
                        >
                          暂无回复历史
                        </div>
                        <div v-else class="qms-exception-reply-history">
                          <table>
                            <thead>
                              <tr>
                                <th>次数</th>
                                <th>回复时间</th>
                                <th>回复人</th>
                                <th>填写日期</th>
                                <th>办理时长</th>
                                <th>确认意见</th>
                                <th>附件</th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr
                                v-for="(reply, replyIndex) in getMemberReplies(task, member)"
                                :key="reply.id || reply.replyNo"
                              >
                                <td>{{ replyIndex + 1 }}</td>
                                <td>{{ reply.submitTime || '-' }}</td>
                                <td>{{ getReplySubmitterDisplay(reply, member) }}</td>
                                <td>{{ reply.actualFinishTime || '-' }}</td>
                                <td
                                  :class="{
                                    'qms-exception-overdue-text': isReplyOverdue(
                                      task,
                                      reply,
                                    ),
                                  }"
                                >
                                  {{ getReplyDuration(task, reply) }}
                                </td>
                                <td class="qms-exception-tracking-text">
                                  {{ reply.remark || reply.actionDescription || '-' }}
                                </td>
                                <td>
                                  {{
                                    reply.attachmentUrls?.length
                                      ? reply.attachmentUrls.length + '个'
                                      : '-'
                                  }}
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </template>
            </div>
          </section>

          <section v-if="showResponsibilityStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>四、确认责任部门</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label>责任单位</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Select
                  v-if="canEditResponsibilityStep"
                  v-model:value="responsibilityDeptValues"
                  allow-clear
                  class="w-full"
                  mode="multiple"
                  :max-tag-count="8"
                  :options="responsibilityDeptOptions"
                  placeholder="请选择责任单位"
                  show-search
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ responsibilityDeptDisplayName || '-' }}
                </div>
              </div>
              <label>负责人</label>
              <div class="erp-form-value erp-form-value--span-5">
                <template v-if="canEditResponsibilityStep">
                  <Input v-model:value="approvalForm.nextHandlerUserName" readonly />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('nextHandler')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.actionOwnerName || '-' }}
                </div>
              </div>
              <label>处理期限</label>
              <div class="erp-form-value erp-form-value--span-5">
                <DatePicker
                  v-if="canEditResponsibilityStep"
                  v-model:value="rootCauseTaskDeadline"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择处理期限（可不填）"
                />
                <span v-else>{{ rootCauseTaskDeadline || '-' }}</span>
              </div>
            </div>
          </section>

          <section v-if="showRootCauseStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>五、根因分析和纠正预防措施与标准化</strong>
                <span class="qms-exception-section-subtitle">
                  责任单位根因分析和纠正预防措施与标准化会签
                </span>
              </div>
            </div>
            <div class="erp-form-grid">
              <label class="erp-form-label--tall">根因分析</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditRootCauseOwnerStep"
                  v-model:value="formData.rootCause"
                  placeholder="请填写根因分析"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ formData.rootCause || '-' }}
                </div>
              </div>
              <label class="erp-form-label--tall">纠正预防措施</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditRootCauseOwnerStep"
                  v-model:value="formData.preventiveAction"
                  placeholder="请填写纠正预防措施"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ formData.preventiveAction || '-' }}
                </div>
              </div>
              <label>根因附件</label>
              <div class="erp-form-value erp-form-value--span-5">
                <FileUpload
                  v-if="canEditRootCauseOwnerStep"
                  v-model="rootCauseAttachmentUrls"
                  :accept="attachmentAcceptTypes"
                  directory="mes/qms/exception"
                  :max-number="20"
                  :max-size="20"
                  multiple
                  show-description
                  @preview="handleAttachmentPreview"
                />
                <div v-else class="qms-exception-attachment-list">
                  <template v-if="rootCauseAttachmentUrls.length > 0">
                    <Button
                      v-for="(url, index) in rootCauseAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </template>
                  <span v-else>-</span>
                </div>
              </div>
              <label>会签人</label>
              <div class="erp-form-value erp-form-value--span-5">
                <div
                  v-if="canEditRootCauseOwnerStep"
                  class="qms-exception-linked-ncr"
                >
                  <Input
                    :value="
                      rootCauseAssignmentTask
                        ? getTaskSignerNames(rootCauseAssignmentTask)
                        : ''
                    "
                    placeholder="请选择会签人"
                    readonly
                  />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openAssignmentMemberMultiSelector(TASK_TYPE_ROOT_CAUSE)"
                  >
                    多选会签人
                  </Button>
                </div>
                <div v-else class="qms-exception-readonly-value">
                  {{
                    rootCauseAssignmentTask
                      ? getTaskSignerNames(rootCauseAssignmentTask)
                      : '-'
                  }}
                </div>
              </div>
            </div>
            <div v-if="rootCauseTasks.length === 0" class="qms-exception-empty-panel">
              暂无根因分析与纠正预防任务
            </div>
            <div v-else class="qms-exception-sign-grid qms-exception-sign-grid--root-cause">
              <template v-for="task in rootCauseTasks" :key="getTaskKey(task)">
                <div
                  v-if="!task.members || task.members.length === 0"
                  class="qms-exception-empty-panel"
                >
                  暂无会签人
                </div>
                <template v-else>
                  <div
                    v-for="member in task.members"
                    :key="member.id || `${task.id}-${member.userId}-${member.deptName}`"
                    class="qms-exception-sign-cell qms-exception-sign-cell--root-cause"
                    :class="{
                      'qms-exception-sign-cell--active': canEditRootCauseSignMember(
                        task,
                        member,
                      ),
                    }"
                  >
                    <div class="qms-exception-sign-cell__dept">
                      {{ getGroupMemberBusinessDeptName(member) || '-' }}
                    </div>
                    <div class="qms-exception-sign-cell__body">
                      <div class="qms-exception-sign-cell__tools">
                        <Tooltip title="展开回复历史">
                          <Button
                            size="small"
                            type="link"
                            @click="toggleTaskMemberExpanded(task, member)"
                          >
                            <IconifyIcon
                              :icon="
                                isTaskMemberExpanded(task, member)
                                  ? 'lucide:chevron-up'
                                  : 'lucide:chevron-down'
                              "
                            />
                          </Button>
                        </Tooltip>
                        <Tooltip title="确认会签">
                          <Button
                            v-if="canEditRootCauseSignMember(task, member)"
                            size="small"
                            type="link"
                            @click="handleSubmitGroupTask(task, member)"
                          >
                            <IconifyIcon icon="lucide:check" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="委托代办">
                          <Button
                            v-if="canDelegateGroupTaskMember(task, member)"
                            size="small"
                            type="link"
                            @click="openGroupTaskDelegateSelector(task, member)"
                          >
                            <IconifyIcon icon="lucide:user-plus" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="不同意退回">
                          <Button
                            v-if="canReviewGroupTask(task)"
                            danger
                            size="small"
                            type="link"
                            @click="handleReviewGroupTask(task, 'RETURN')"
                          >
                            <IconifyIcon icon="lucide:rotate-ccw" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="确认完成">
                          <Button
                            v-if="canReviewGroupTask(task)"
                            size="small"
                            type="link"
                            @click="handleReviewGroupTask(task, 'ACCEPT')"
                          >
                            <IconifyIcon icon="lucide:check" />
                          </Button>
                        </Tooltip>
                      </div>
                      <div
                        v-if="shouldShowInvestigationSignDetail(task, member)"
                        class="qms-exception-sign-main"
                      >
                        <div class="qms-exception-sign-detail-title">确认意见</div>
                        <div
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                        >
                          {{ getInvestigationMemberConfirmOpinion(task, member) }}
                        </div>
                      </div>
                      <div class="qms-exception-sign-meta qms-exception-sign-meta--three">
                        <div class="qms-exception-sign-meta-item">
                          <span>部门</span>
                          <b>{{ getGroupMemberBusinessDeptName(member) || '-' }}</b>
                        </div>
                        <div class="qms-exception-sign-meta-item">
                          <span>成员名称</span>
                          <div class="qms-exception-sign-meta-value">
                            <b>{{ getMemberHandlerDisplay(member) }}</b>
                            <em v-if="getMemberDelegateText(member)">
                              {{ getMemberDelegateText(member) }}
                            </em>
                          </div>
                        </div>
                        <div
                          class="qms-exception-sign-meta-item"
                          :class="{
                            'qms-exception-sign-meta-item--danger': isSignMemberOverdue(
                              task,
                              member,
                            ),
                          }"
                        >
                          <span>填写日期</span>
                          <div
                            class="qms-exception-sign-meta-value"
                            :class="{
                              'qms-exception-sign-meta-value--danger':
                                isSignMemberOverdue(task, member),
                            }"
                          >
                            <b>{{ getInvestigationMemberFinishTime(task, member) }}</b>
                            <em>办理时长：{{ getSignMemberDuration(task, member) }}</em>
                          </div>
                        </div>
                      </div>
                      <div
                        v-if="isTaskMemberExpanded(task, member)"
                        class="qms-exception-sign-history"
                      >
                        <div
                          v-if="getMemberReplies(task, member).length === 0"
                          class="qms-exception-empty-panel"
                        >
                          暂无回复历史
                        </div>
                        <div v-else class="qms-exception-reply-history">
                          <table>
                            <thead>
                              <tr>
                                <th>次数</th>
                                <th>回复时间</th>
                                <th>回复人</th>
                                <th>填写日期</th>
                                <th>办理时长</th>
                                <th>确认意见</th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr
                                v-for="(reply, replyIndex) in getMemberReplies(task, member)"
                                :key="reply.id || reply.replyNo"
                              >
                                <td>{{ replyIndex + 1 }}</td>
                                <td>{{ reply.submitTime || '-' }}</td>
                                <td>{{ getReplySubmitterDisplay(reply, member) }}</td>
                                <td>{{ reply.actualFinishTime || '-' }}</td>
                                <td
                                  :class="{
                                    'qms-exception-overdue-text': isReplyOverdue(
                                      task,
                                      reply,
                                    ),
                                  }"
                                >
                                  {{ getReplyDuration(task, reply) }}
                                </td>
                                <td class="qms-exception-tracking-text">
                                  {{ reply.remark || reply.actionDescription || '-' }}
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </template>
            </div>
          </section>

          <section v-if="showResultUploadStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>六、执行结果上传</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label>执行结果上传人</label>
              <div class="erp-form-value erp-form-value--span-5">
                <template v-if="canEditResultUploaderAssignStep">
                  <Input v-model:value="approvalForm.nextHandlerUserName" readonly />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('nextHandler')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.resultUploaderName || '-' }}
                </div>
              </div>
              <label class="erp-form-label--tall">纠正预防措施执行成果</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditResultUploadStep"
                  v-model:value="formData.correctivePreventiveResult"
                  placeholder="请填写纠正预防措施执行成果"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ formData.correctivePreventiveResult || '-' }}
                </div>
              </div>
              <label>上传时间</label>
              <div class="erp-form-value">
                <DatePicker
                  v-if="canEditResultUploadStep"
                  v-model:value="formData.resultUploadTime"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择上传时间"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.resultUploadTime || '-' }}
                </div>
              </div>
              <label>执行附件</label>
              <div class="erp-form-value erp-form-value--span-3">
                <FileUpload
                  v-if="canEditResultUploadStep"
                  v-model="resultAttachmentUrls"
                  :accept="attachmentAcceptTypes"
                  directory="mes/qms/exception"
                  :max-number="20"
                  :max-size="20"
                  multiple
                  show-description
                  @preview="handleAttachmentPreview"
                />
                <div v-else class="qms-exception-attachment-list">
                  <template v-if="resultAttachmentUrls.length > 0">
                    <Button
                      v-for="(url, index) in resultAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </template>
                  <span v-else>-</span>
                </div>
              </div>
              <label>抄送人</label>
              <div class="erp-form-value erp-form-value--span-5">
                <template v-if="canEditResultUploadStep">
                  <Input
                    :value="approvalForm.copyUserNames.join('、')"
                    placeholder="可选择抄送人"
                    readonly
                  />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('copyUsers')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.copyUserNames || '-' }}
                </div>
              </div>
            </div>
          </section>

          <section v-if="showQaClosureStep" class="erp-basic-form">
            <div class="detail-list-head">
              <div class="detail-list-title">
                <strong>七、品质部门闭环确认</strong>
              </div>
            </div>
            <div class="erp-form-grid">
              <label class="erp-form-label--tall">闭环确认</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Input.TextArea
                  v-if="canEditQaClosureStep"
                  v-model:value="formData.effectConfirm"
                  placeholder="请填写闭环确认说明"
                  :rows="3"
                />
                <div
                  v-else
                  class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                >
                  {{ formData.effectConfirm || '-' }}
                </div>
              </div>
              <label>是否有效</label>
              <div class="erp-form-value erp-form-value--span-5">
                <Checkbox
                  v-if="canEditQaClosureStep"
                  v-model:checked="formData.qaConfirmValid"
                >
                  确认有效
                </Checkbox>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.qaConfirmValid ? '确认有效' : '未确认有效' }}
                </div>
              </div>
              <label>品质确认人</label>
              <div class="erp-form-value">
                <template v-if="canEditQaClosureStep">
                  <Input v-model:value="formData.qaConfirmerName" readonly />
                  <Button
                    class="qms-exception-inline-select"
                    size="small"
                    type="link"
                    @click="openUserSelector('qaConfirmer')"
                  >
                    选择
                  </Button>
                </template>
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.qaConfirmerName || '-' }}
                </div>
              </div>
              <label>确认日期</label>
              <div class="erp-form-value">
                <DatePicker
                  v-if="canEditQaClosureStep"
                  v-model:value="formData.finishTime"
                  allow-clear
                  class="w-full"
                  :format="DATETIME_FORMAT"
                  :show-time="{ format: 'HH:mm:ss' }"
                  :value-format="DATETIME_FORMAT"
                  placeholder="请选择确认日期"
                />
                <div v-else class="qms-exception-readonly-value">
                  {{ formData.finishTime || '-' }}
                </div>
              </div>
              <label>附件</label>
              <div class="erp-form-value">
                <Radio.Group :value="effectAttachmentFlag" disabled>
                  <Radio value="有">有</Radio>
                  <Radio value="无">无</Radio>
                </Radio.Group>
              </div>
              <label>闭环附件</label>
              <div class="erp-form-value erp-form-value--span-5">
                <FileUpload
                  v-if="canEditQaClosureStep"
                  v-model="effectAttachmentUrls"
                  :accept="attachmentAcceptTypes"
                  directory="mes/qms/exception"
                  :max-number="20"
                  :max-size="20"
                  multiple
                  show-description
                  @preview="handleAttachmentPreview"
                />
                <div v-else class="qms-exception-attachment-list">
                  <template v-if="effectAttachmentUrls.length > 0">
                    <Button
                      v-for="(url, index) in effectAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </template>
                  <span v-else>-</span>
                </div>
              </div>
            </div>
          </section>

        </Form>
          </div>
        </div>
      </Spin>
    </div>
  </Modal>
</template>

<style>
.qms-product-event-detail-modal [class*='modal__header'],
.qms-product-event-detail-modal .ant-modal-header {
  display: none !important;
}

.qms-product-event-detail-modal [class*='modal__body'],
.qms-product-event-detail-modal .ant-modal-body {
  height: 100dvh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #eef2f6;
}

.qms-product-event-detail-modal [class*='modal__content'],
.qms-product-event-detail-modal .ant-modal-content {
  height: 100dvh !important;
  padding: 0 !important;
  overflow: hidden !important;
}

.qms-product-event-detail-modal .ant-spin-nested-loading,
.qms-product-event-detail-modal .ant-spin-container {
  height: 100%;
  min-height: 0;
}

.qms-product-event-detail-modal .ant-spin-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
<style scoped>
.qms-ncr-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #1f2937;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    #f5f7fa;
}

.qms-ncr-toolbar {
  display: grid;
  grid-template-columns:
    minmax(260px, 1fr)
    minmax(360px, 720px)
    minmax(320px, 1fr);
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 8px 14px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
  border-bottom: 1px solid #cbd5e1;
}

.qms-ncr-toolbar__placeholder {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.qms-ncr-title-panel {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 4px;
  text-align: center;
}

.qms-ncr-title-panel__name {
  overflow: hidden;
  color: #075985;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-title-panel__subtitle {
  display: flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.qms-ncr-title-panel__subtitle-item {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-toolbar__actions {
  display: flex;
  min-width: max-content;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 6px;
  justify-self: end;
}

.qms-ncr-toolbar__log-icons {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 4px;
}

.qms-ncr-toolbar-icon-btn.ant-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-width: 1px;
  box-shadow: 0 2px 8px rgb(15 23 42 / 6%);
  transition:
    border-color 0.16s ease,
    background-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.qms-ncr-toolbar-icon-btn.ant-btn:hover {
  transform: translateY(-1px);
}

.qms-ncr-toolbar-icon-btn :deep(svg) {
  width: 18px;
  height: 18px;
}

.qms-ncr-toolbar-icon-btn--audit.ant-btn {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.qms-ncr-toolbar-icon-btn--audit.ant-btn:hover {
  border-color: #2563eb;
  background: #dbeafe;
  color: #1e40af;
}

.qms-ncr-toolbar-icon-btn--workflow.ant-btn {
  border-color: #c4b5fd;
  background: #f5f3ff;
  color: #6d28d9;
}

.qms-ncr-toolbar-icon-btn--workflow.ant-btn:hover {
  border-color: #7c3aed;
  background: #ede9fe;
  color: #5b21b6;
}

.qms-ncr-toolbar-icon-btn--operation.ant-btn {
  border-color: #fdba74;
  background: #fff7ed;
  color: #c2410c;
}

.qms-ncr-toolbar-icon-btn--operation.ant-btn:hover {
  border-color: #f97316;
  background: #ffedd5;
  color: #9a3412;
}

.qms-ncr-toolbar-action.ant-btn {
  display: inline-flex;
  min-width: 56px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  line-height: 1;
}

.qms-ncr-toolbar-action span {
  max-width: 100%;
  overflow: hidden;
  font-size: 13px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-spin {
  display: block;
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
}

.detail-spin :deep(.ant-spin-nested-loading),
.detail-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.detail-content {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.qms-exception-workbench {
  flex: 1 1 0%;
  min-height: 0;
  overflow: auto;
}

.qms-exception-form {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 12px;
}

.erp-basic-form {
  flex: 0 1 auto;
  margin: 0;
  overflow: auto;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.detail-list-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px 12px;
}

.detail-list-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.detail-list-title strong {
  color: #0f172a;
  font-size: 14px;
}

.qms-exception-section-subtitle {
  color: #10233d;
  font-size: 13px;
  font-weight: 800;
}

.erp-form-grid {
  display: grid;
  grid-template-columns: repeat(3, 110px minmax(0, 1fr));
  border-top: 1px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
}

.erp-form-grid > label,
.erp-form-label,
.erp-form-value {
  min-height: 38px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 9px 10px;
  font-size: 13px;
  line-height: 20px;
}

.erp-form-grid > label,
.erp-form-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #475569;
  font-weight: 700;
  overflow-wrap: anywhere;
  text-align: center;
  white-space: normal;
  word-break: break-word;
}

.erp-form-label--tall {
  min-height: 88px !important;
}

.erp-form-value {
  display: flex;
  align-items: center;
  min-width: 0;
  background: #fff;
  color: #111827;
  overflow-wrap: anywhere;
}

.erp-form-value--span-3 {
  grid-column: span 3;
}

.erp-form-value--span-5 {
  grid-column: span 5;
}

.erp-form-value :deep(.ant-input),
.erp-form-value :deep(.ant-select),
.erp-form-value :deep(.ant-input-search),
.erp-form-value :deep(.ant-picker),
.erp-form-value :deep(.ant-tree-select),
.erp-form-value :deep(.ant-upload-wrapper) {
  width: 100%;
}

.erp-form-value :deep(.ant-input),
.erp-form-value :deep(.ant-select-selector),
.erp-form-value :deep(.ant-picker) {
  border-radius: 4px;
}

.qms-exception-form-item {
  width: 100%;
  margin-bottom: 0;
}

.qms-exception-radio-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  width: 100%;
}

.qms-exception-inline-select {
  flex-shrink: 0;
  margin-left: 6px;
  padding: 0 4px;
}

.qms-exception-text-action {
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 0;
  text-align: left;
  white-space: pre-wrap;
}

.qms-exception-text-action:hover {
  color: #1677ff;
}

.qms-exception-linked-ncr {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.qms-exception-linked-ncr :deep(.ant-input-affix-wrapper) {
  flex: 1;
  min-width: 0;
}

.qms-exception-linked-ncr > .ant-btn {
  flex-shrink: 0;
}

.qms-exception-field-tip {
  margin-top: 4px;
  color: #d48806;
  font-size: 12px;
  line-height: 18px;
}

.qms-exception-inline-icon-btn.ant-btn {
  width: 22px;
  min-width: 22px;
  height: 22px;
  padding: 0;
  color: #64748b;
}

.qms-exception-inline-icon-btn :deep(svg) {
  width: 13px;
  height: 13px;
}

.qms-exception-radio-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 26px;
  align-items: center;
}

.qms-exception-checkbox-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 6px 12px;
}

.qms-exception-checkbox-grid :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
}

.qms-exception-readonly-value {
  width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-exception-readonly-value--multiline {
  white-space: pre-wrap;
  word-break: break-word;
}

.qms-exception-subtable-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 14px 0 8px;
  color: #10233d;
  font-weight: 800;
}

.qms-exception-subtable-title--compact {
  margin-top: 10px;
  font-size: 13px;
}

.qms-exception-subtable-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.qms-exception-empty-panel {
  border: 1px dashed #cddbea;
  background: #fbfdff;
  color: #8a9aab;
  padding: 16px;
  text-align: center;
}

.qms-exception-return-opinion-grid {
  margin-top: -1px;
}

.qms-exception-return-opinion-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #ffd591;
  background: #fff7e6;
  color: #873800;
  table-layout: fixed;
}

.qms-exception-return-opinion-table th,
.qms-exception-return-opinion-table td {
  border: 1px solid #ffd591;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
  word-break: break-word;
}

.qms-exception-return-opinion-table th {
  color: #ad6800;
  font-weight: 800;
  white-space: nowrap;
}

.qms-exception-return-opinion-table td {
  font-weight: 600;
  white-space: pre-wrap;
}

.qms-exception-sign-grid {
  display: flex;
  overflow: hidden;
  flex-direction: column;
  border-top: 1px solid #cddbea;
  border-left: 1px solid #cddbea;
  background: #fff;
}

.qms-exception-sign-summary {
  overflow: hidden;
  margin-top: -1px;
  border-top: 0;
  border-left: 1px solid #cddbea;
  background: #fff;
}

.qms-exception-sign-summary-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.qms-exception-sign-summary-table th,
.qms-exception-sign-summary-table td {
  border-right: 1px solid #cddbea;
  border-bottom: 1px solid #cddbea;
  padding: 10px 12px;
  color: #10233d;
  font-size: 13px;
}

.qms-exception-sign-summary-table th {
  background: #eef4fa;
  font-weight: 800;
  text-align: center;
}

.qms-exception-sign-summary-row {
  cursor: pointer;
}

.qms-exception-sign-summary-row:hover td {
  background: #f8fbff;
}

.qms-exception-sign-summary-empty {
  color: #8a9aab;
  text-align: center;
}

.qms-exception-sign-summary-date {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, auto) minmax(0, 1fr) 18px;
  align-items: center;
  gap: 8px;
}

.qms-exception-sign-summary-date span,
.qms-exception-sign-summary-date em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-exception-sign-summary-date em {
  color: #64748b;
  font-style: normal;
  font-weight: 600;
}

.qms-exception-sign-summary-date--danger span,
.qms-exception-sign-summary-date--danger em {
  color: #dc2626;
}

.qms-exception-sign-summary-detail-row > td {
  padding: 0;
  background: #fbfdff;
}

.qms-exception-sign-summary-detail {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.qms-exception-sign-summary-detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #10233d;
}

.qms-exception-sign-summary-detail__head strong {
  font-weight: 800;
}

.qms-exception-sign-cell {
  display: grid;
  min-height: 138px;
  grid-template-columns: 110px minmax(0, 1fr);
  border-right: 1px solid #cddbea;
  border-bottom: 1px solid #cddbea;
  background: #fff;
}

.qms-exception-sign-cell--active {
  box-shadow: inset 3px 0 0 #1677ff;
}

.qms-exception-sign-cell--root-cause {
  min-height: auto;
}

.qms-exception-sign-cell__dept {
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #cddbea;
  background: #eef4fa;
  color: #10233d;
  font-size: 14px;
  font-weight: 800;
  padding: 8px;
  text-align: center;
}

.qms-exception-sign-cell__body {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  padding: 8px 86px 8px 8px;
}

.qms-exception-sign-cell__tools {
  position: absolute;
  top: 4px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.qms-exception-sign-main {
  min-width: 0;
}

.qms-exception-sign-detail-title {
  margin-bottom: 4px;
  color: #10233d;
  font-size: 13px;
  font-weight: 800;
}

.qms-exception-attachment-list {
  display: flex;
  min-height: 32px;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  color: #334155;
}

.qms-exception-sign-detail {
  min-height: 66px;
  resize: vertical;
}

.qms-exception-sign-detail-readonly {
  width: 100%;
  min-height: 66px;
  border: 1px solid #dbe5ef;
  background: #fbfdff;
  padding: 6px 8px;
}

.qms-exception-sign-meta {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.qms-exception-sign-meta--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.qms-exception-sign-meta-item {
  display: grid;
  min-width: 0;
  min-height: 32px;
  grid-template-columns: 72px minmax(0, 1fr);
  border: 1px solid #dbe5ef;
  background: #fff;
}

.qms-exception-sign-meta-item--danger {
  border-color: #fca5a5;
  background: #fff7f7;
}

.qms-exception-sign-meta-item span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 8px;
  background: #eef4fa;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.qms-exception-sign-meta-item b {
  overflow: hidden;
  min-width: 0;
  align-self: center;
  padding: 0 8px;
  color: #10233d;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-exception-sign-meta-value {
  display: flex;
  overflow: hidden;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: 4px 8px;
}

.qms-exception-sign-meta-value b {
  padding: 0;
}

.qms-exception-sign-meta-value em {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-exception-sign-meta-value--danger b,
.qms-exception-sign-meta-value--danger em {
  color: #dc2626;
}

.qms-exception-sign-date {
  width: 100%;
}

.qms-exception-sign-history {
  margin-top: 2px;
}

.qms-exception-sign-cell :deep(.ant-btn-link) {
  padding: 0 3px;
}

.qms-exception-sign-cell :deep(.ant-input),
.qms-exception-sign-cell :deep(.ant-picker),
.qms-exception-sign-cell :deep(.ant-picker-input > input) {
  border-color: #dbe5ef;
  border-radius: 0;
  font-size: 13px;
}

.qms-exception-group-task {
  border: 1px solid #cddbea;
  background: #fbfdff;
  margin-top: 10px;
  padding: 10px;
}

.qms-exception-group-task__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.qms-exception-group-task__name {
  max-width: 220px;
}

.qms-exception-task-meta {
  overflow: hidden;
  color: #52677f;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.erp-form-grid--inner {
  margin-top: 8px;
}

.qms-exception-team-table {
  overflow-x: auto;
}

.qms-exception-team-table table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  border: 1px solid #cddbea;
  font-size: 14px;
}

.qms-exception-team-table th,
.qms-exception-team-table td {
  border: 1px solid #cddbea;
  padding: 8px;
}

.qms-exception-team-table th {
  background: #eef4fa;
  color: #10233d;
  font-weight: 800;
}

.qms-exception-empty-cell {
  color: #8a9aab;
  text-align: center;
}

.qms-exception-modal-grid {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 10px 12px;
  align-items: center;
}

.qms-exception-modal-grid > label {
  color: #10233d;
  font-weight: 700;
  text-align: right;
}

.qms-exception-modal-label--tall {
  align-self: start;
  padding-top: 6px;
}

.qms-exception-modal-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.erp-form-value--modal-span {
  min-width: 0;
}

.qms-exception-review-depts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  min-height: 32px;
  align-items: center;
}

.qms-exception-review-depts :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
}

.qms-exception-member-role-table,
.qms-exception-tracking-table,
.qms-exception-reply-history {
  overflow-x: auto;
}

.qms-exception-subtable {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  background: #fff;
}

.qms-exception-member-role-table {
  margin-top: 8px;
}

.qms-exception-user-option {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.qms-exception-user-option__name {
  min-width: 0;
  overflow: hidden;
  color: #10233d;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-exception-user-option__code {
  flex: 0 0 auto;
  color: #64748b;
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
  font-size: 12px;
}

.qms-exception-table-strong {
  color: #10233d;
  font-weight: 800;
}

.qms-exception-table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
}

.qms-exception-member-role-table table,
.qms-exception-tracking-table table,
.qms-exception-reply-history table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #cddbea;
  font-size: 13px;
}

.qms-exception-tracking-table table {
  min-width: 1320px;
}

.qms-exception-reply-history table {
  min-width: 860px;
}

.qms-exception-member-role-table th,
.qms-exception-member-role-table td,
.qms-exception-tracking-table th,
.qms-exception-tracking-table td,
.qms-exception-reply-history th,
.qms-exception-reply-history td {
  border: 1px solid #cddbea;
  padding: 8px;
  vertical-align: top;
}

.qms-exception-member-role-table th,
.qms-exception-tracking-table th,
.qms-exception-reply-history th {
  background: #eef4fa;
  color: #10233d;
  font-weight: 800;
  white-space: nowrap;
}

.qms-exception-task-main {
  min-width: 180px;
}

.qms-exception-task-main__name {
  color: #10233d;
  font-weight: 800;
}

.qms-exception-tracking-text {
  max-width: 260px;
  white-space: pre-wrap;
  word-break: break-word;
}

.qms-exception-overdue-text {
  color: #dc2626;
  font-weight: 800;
}

@media (max-width: 1200px) {
  .qms-exception-sign-cell {
    grid-template-columns: 110px minmax(0, 1fr);
  }

  .qms-exception-sign-cell__body {
    padding-right: 8px;
    padding-top: 34px;
  }

  .qms-exception-sign-meta--three {
    grid-template-columns: 1fr;
  }
}

.qms-exception-member-confirm-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 130px;
}

.qms-exception-member-confirm-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  white-space: nowrap;
}

.qms-exception-icon-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.qms-exception-icon-actions :deep(.ant-btn) {
  width: 26px;
  padding: 0;
}

@media (max-width: 1180px) {
  .qms-ncr-toolbar {
    grid-template-columns: 1fr;
    padding: 10px 12px;
  }

  .qms-ncr-toolbar__placeholder,
  .qms-ncr-title-panel,
  .qms-ncr-toolbar__actions {
    justify-content: center;
    text-align: center;
  }

  .qms-ncr-toolbar__actions {
    flex-wrap: wrap;
    min-width: 0;
  }

  .erp-form-grid {
    grid-template-columns: 96px minmax(0, 1fr) 96px minmax(0, 1fr);
  }

  .qms-exception-sign-cell {
    grid-template-columns: 96px minmax(0, 1fr);
  }

  .erp-form-value--span-3,
  .erp-form-value--span-5 {
    grid-column: span 3;
  }
}
</style>
