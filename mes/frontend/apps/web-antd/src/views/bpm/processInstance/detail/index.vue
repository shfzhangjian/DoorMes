<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { SystemUserApi } from '#/api/system/user';

import {
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  shallowRef,
  watch,
} from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import {
  BpmFieldPermissionType,
  BpmModelFormType,
  BpmModelType,
  BpmProcessInstanceStatus,
  DICT_TYPE,
} from '@vben/constants';
import {
  IconifyIcon,
  SvgBpmApproveIcon,
  SvgBpmCancelIcon,
  SvgBpmRejectIcon,
  SvgBpmRunningIcon,
} from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { formatDateTime } from '@vben/utils';

import {
  Modal as AntModal,
  Avatar,
  Button,
  Card,
  Col,
  Form,
  message,
  Row,
  Select,
  TabPane,
  Tabs,
  Textarea,
} from 'ant-design-vue';

import {
  adjustProcessInstanceStep,
  getAdjustableProcessInstanceNodes,
  getApprovalDetail as getApprovalDetailApi,
  getProcessInstanceBpmnModelView,
} from '#/api/bpm/processInstance';
import { getSimpleUserList } from '#/api/system/user';
import DictTag from '#/components/dict-tag/dict-tag.vue';
import { setConfAndFields2 } from '#/components/form-create';
import { registerComponent } from '#/utils';
import ExceptionInlineDetail from '#/views/mes/quality/abnormal/exception/modules/inline-detail.vue';
import NcrInlineDetail from '#/views/mes/quality/abnormal/ncr/modules/inline-detail.vue';
import OnboardingApplyInlineDetail from '#/views/mes/srm/certification/import-application/modules/inline-detail.vue';
import PreliminaryEvaluationInlineDetail from '#/views/mes/srm/certification/preliminary-evaluation/modules/inline-detail.vue';
import SampleEvaluationInlineDetail from '#/views/mes/srm/certification/sample-evaluation/modules/inline-detail.vue';
import SampleRequestInlineDetail from '#/views/mes/srm/onboarding/sample-req/modules/inline-detail.vue';
import SupplierExitApprovalInlineDetail from '#/views/mes/srm/archive/exit/modules/inline-detail.vue';

import ProcessInstanceBpmnViewer from './modules/bpm-viewer.vue';
import ProcessInstanceOperationButton from './modules/operation-button.vue';
import ProcessssPrint from './modules/process-print.vue';
import ProcessInstanceSimpleViewer from './modules/simple-bpm-viewer.vue';
import BpmProcessInstanceTaskList from './modules/task-list.vue';
import ProcessInstanceTimeline from './modules/time-line.vue';

defineOptions({ name: 'BpmProcessInstanceDetail' });

const props = defineProps<{
  activityId?: string; // 流程活动编号，用于抄送查看
  id: string; // 流程实例的编号
  taskId?: string; // 任务编号
}>();

const processInstanceLoading = ref(false); // 流程实例的加载中
const processInstance = ref<BpmProcessInstanceApi.ProcessInstance>(); // 流程实例
const processDefinition = ref<any>({}); // 流程定义
const processModelView = ref<any>({}); // 流程模型视图
const operationButtonRef = ref(); // 操作按钮组件 ref
const activeTab = ref('form');
const taskListRef = ref();
const userStore = useUserStore();
const auditIconsMap: {
  [key: string]:
    | typeof SvgBpmApproveIcon
    | typeof SvgBpmCancelIcon
    | typeof SvgBpmRejectIcon
    | typeof SvgBpmRunningIcon;
} = {
  [BpmProcessInstanceStatus.RUNNING]: SvgBpmRunningIcon,
  [BpmProcessInstanceStatus.APPROVE]: SvgBpmApproveIcon,
  [BpmProcessInstanceStatus.REJECT]: SvgBpmRejectIcon,
  [BpmProcessInstanceStatus.CANCEL]: SvgBpmCancelIcon,
};
const activityNodes = ref<BpmProcessInstanceApi.ApprovalNodeInfo[]>([]); // 审批节点信息
const userOptions = ref<SystemUserApi.User[]>([]); // 用户列表
const NCR_PROCESS_KEYS = new Set([
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
]);
const NCR_FORM_PATHS = new Set([
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
]);
const EXCEPTION_PROCESS_KEYS = ['qms_exception_event'];
const EXCEPTION_FORM_PATHS = new Set(['/mes/quality/abnormal/exception']);
const PRELIMINARY_PROCESS_KEYS = ['srm_preliminary_evaluation'];
const PRELIMINARY_FORM_PATHS = new Set([
  '/mes/srm/certification/preliminary-evaluation',
]);
const SAMPLE_EVALUATION_PROCESS_KEYS = ['srm_sample_evaluation'];
const SAMPLE_EVALUATION_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-evaluation',
]);
const SAMPLE_REQUEST_PROCESS_KEYS = ['srm_sample_request'];
const SAMPLE_REQUEST_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-req',
]);
const ONBOARDING_APPLY_PROCESS_KEYS = ['srm_onboarding_apply'];
const ONBOARDING_APPLY_FORM_PATHS = new Set([
  '/mes/srm/certification/import-application',
]);
const SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS = ['srm_supplier_exit_approval'];
const SUPPLIER_EXIT_APPROVAL_FORM_PATHS = new Set([
  '/mes/srm/certification/exit',
]);

const fApi = ref<any>();
const detailForm = ref({
  rule: [],
  option: {},
  value: {},
}); // 流程实例的表单详情
const writableFields: Array<string> = []; // 表单可以编辑的字段

const BusinessFormComponent = shallowRef<any>(null); // 异步组件(业务表单）
const isNcrProcessDetail = computed(() =>
  isNcrProcess(processDefinition.value, processInstance.value),
);
const isExceptionProcessDetail = computed(() =>
  isExceptionProcess(processDefinition.value, processInstance.value),
);
const isPreliminaryEvaluationProcessDetail = computed(() =>
  isPreliminaryEvaluationProcess(
    processDefinition.value,
    processInstance.value,
  ),
);
const isSampleEvaluationProcessDetail = computed(() =>
  isSampleEvaluationProcess(processDefinition.value, processInstance.value),
);
const isSampleRequestProcessDetail = computed(() =>
  isSampleRequestProcess(processDefinition.value, processInstance.value),
);
const isOnboardingApplyProcessDetail = computed(() =>
  isOnboardingApplyProcess(processDefinition.value, processInstance.value),
);
const isSupplierExitApprovalProcessDetail = computed(() =>
  isSupplierExitApprovalProcess(processDefinition.value, processInstance.value),
);
const formTabLabel = computed(() =>
  isNcrProcessDetail.value ||
  isExceptionProcessDetail.value ||
  isPreliminaryEvaluationProcessDetail.value ||
  isSampleEvaluationProcessDetail.value ||
  isSampleRequestProcessDetail.value ||
  isOnboardingApplyProcessDetail.value ||
  isSupplierExitApprovalProcessDetail.value
    ? '业务详情'
    : '审批详情',
);
const businessFormId = computed(() => {
  const instance = processInstance.value;
  if (!instance) {
    return undefined;
  }
  if (isNcrProcessDetail.value) {
    return (
      instance.formVariables?.ncRecordId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  if (isExceptionProcessDetail.value) {
    return (
      instance.formVariables?.exceptionId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey ||
      instance.formVariables?.exceptionNo
    );
  }
  if (isPreliminaryEvaluationProcessDetail.value) {
    return (
      instance.formVariables?.evaluationId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  if (isSampleEvaluationProcessDetail.value) {
    return (
      instance.formVariables?.sampleEvaluationId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  if (isSampleRequestProcessDetail.value) {
    return (
      instance.formVariables?.sampleRequestId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  if (isOnboardingApplyProcessDetail.value) {
    return (
      instance.formVariables?.onboardingApplyId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  if (isSupplierExitApprovalProcessDetail.value) {
    return (
      instance.formVariables?.supplierExitApprovalId ||
      instance.formVariables?.businessKey ||
      instance.formVariables?.id ||
      instance.businessKey
    );
  }
  return instance.businessKey;
});
const currentUserId = computed(() =>
  Number(userStore.userInfo?.id ?? (userStore.userInfo as any)?.userId),
);
const isProcessStarter = computed(
  () =>
    !!processInstance.value?.startUser?.id &&
    Number(processInstance.value.startUser.id) === currentUserId.value,
);
const canAdjustStep = computed(
  () =>
    isProcessStarter.value &&
    processInstance.value?.status === BpmProcessInstanceStatus.RUNNING,
);

const adjustModalOpen = ref(false);
const adjustLoading = ref(false);
const adjustSubmitting = ref(false);
const adjustNodes = ref<BpmProcessInstanceApi.AdjustableNode[]>([]);
const adjustForm = reactive({
  clearBusinessData: false,
  reason: '',
  targetTaskDefinitionKey: undefined as string | undefined,
});
const adjustNodeOptions = computed(() =>
  adjustNodes.value.map((item) => ({
    label: `${item.taskName}${item.assigneeUserName ? ` / ${item.assigneeUserName}` : ''}${item.endTime ? ` / ${item.endTime}` : ''}`,
    value: item.taskDefinitionKey,
  })),
);

/** 获取详情 */
async function getDetail() {
  // 获得审批详情
  await getApprovalDetail();
  // 获得流程模型视图
  await getProcessModelView();
}

/** 获得审批详情 */
async function getApprovalDetail() {
  processInstanceLoading.value = true;
  try {
    const param = {
      processInstanceId: props.id,
      activityId: props.activityId,
      taskId: props.taskId,
    };
    const data = await getApprovalDetailApi(param);
    if (!data) {
      message.error('查询不到审批详情信息！');
    }
    if (!data.processDefinition || !data.processInstance) {
      message.error('查询不到流程信息！');
    }

    processInstance.value = normalizeProcessInstance(data.processInstance);
    processDefinition.value = data.processDefinition;

    // 设置表单信息
    if (processDefinition.value.formType === BpmModelFormType.NORMAL) {
      // 获取表单字段权限
      const formFieldsPermission = data.formFieldsPermission;
      // 清空可编辑字段为空
      writableFields.splice(0);
      if (detailForm.value.rule?.length > 0) {
        // 避免刷新 form-create 显示不了
        detailForm.value.value = processInstance.value.formVariables;
      } else {
        setConfAndFields2(
          detailForm,
          processDefinition.value.formConf,
          processDefinition.value.formFields,
          processInstance.value.formVariables,
        );
      }
      await nextTick();
      fApi.value?.btn.show(false);
      fApi.value?.resetBtn.show(false);
      fApi.value?.disabled(true);
      // 设置表单字段权限
      if (formFieldsPermission) {
        Object.keys(data.formFieldsPermission).forEach((item) => {
          setFieldPermission(item, formFieldsPermission[item]);
        });
      }
    } else if (
      processDefinition.value.formType === BpmModelFormType.CUSTOM &&
      data?.processDefinition?.formCustomViewPath
    ) {
      // 注意：data.processDefinition.formCustomViewPath 是组件的全路径，例如说：/crm/contract/detail/index.vue
      if (isNcrProcess(data.processDefinition, processInstance.value)) {
        BusinessFormComponent.value = NcrInlineDetail;
      } else if (
        isExceptionProcess(data.processDefinition, processInstance.value)
      ) {
        BusinessFormComponent.value = ExceptionInlineDetail;
      } else if (
        isPreliminaryEvaluationProcess(
          data.processDefinition,
          processInstance.value,
        )
      ) {
        BusinessFormComponent.value = PreliminaryEvaluationInlineDetail;
      } else if (
        isSampleEvaluationProcess(data.processDefinition, processInstance.value)
      ) {
        BusinessFormComponent.value = SampleEvaluationInlineDetail;
      } else if (
        isSampleRequestProcess(data.processDefinition, processInstance.value)
      ) {
        BusinessFormComponent.value = SampleRequestInlineDetail;
      } else if (
        isOnboardingApplyProcess(data.processDefinition, processInstance.value)
      ) {
        BusinessFormComponent.value = OnboardingApplyInlineDetail;
      } else if (
        isSupplierExitApprovalProcess(
          data.processDefinition,
          processInstance.value,
        )
      ) {
        BusinessFormComponent.value = SupplierExitApprovalInlineDetail;
      } else {
        BusinessFormComponent.value = registerComponent(
          data.processDefinition.formCustomViewPath,
        );
      }
    } else {
      BusinessFormComponent.value = null;
    }

    // 获取审批节点，显示 Timeline 的数据
    activityNodes.value = data.activityNodes;

    // 获取待办任务显示操作按钮
    operationButtonRef.value?.loadTodoTask(data.todoTask);
  } catch {
    message.error('获取审批详情失败！');
  } finally {
    processInstanceLoading.value = false;
  }
}

function normalizeProcessInstance(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  if (!instance) {
    return instance;
  }
  const businessKey =
    instance.businessKey ||
    instance.formVariables?.exceptionId ||
    instance.formVariables?.ncRecordId ||
    instance.formVariables?.sampleEvaluationId ||
    instance.formVariables?.evaluationId ||
    instance.formVariables?.sampleRequestId ||
    instance.formVariables?.onboardingApplyId ||
    instance.formVariables?.supplierExitApprovalId ||
    instance.formVariables?.businessKey ||
    instance.formVariables?.id ||
    instance.formVariables?.exceptionNo;
  return {
    ...instance,
    businessKey: businessKey ? String(businessKey) : instance.businessKey,
  };
}

function isExceptionProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    EXCEPTION_PROCESS_KEYS.includes(processKey) ||
    EXCEPTION_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    EXCEPTION_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.exceptionId ||
      instance?.formVariables?.exceptionNo ||
      String(instance?.businessKey || '').startsWith('YC-'),
    )
  );
}

function isPreliminaryEvaluationProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    PRELIMINARY_PROCESS_KEYS.includes(processKey) ||
    PRELIMINARY_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    PRELIMINARY_FORM_PATHS.has(formPath) ||
    Boolean(instance?.formVariables?.evaluationId)
  );
}

function isSampleRequestProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    SAMPLE_REQUEST_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_REQUEST_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    SAMPLE_REQUEST_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.sampleRequestId &&
      !instance?.formVariables?.sampleEvaluationId,
    )
  );
}

function isSampleEvaluationProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    SAMPLE_EVALUATION_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_EVALUATION_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    SAMPLE_EVALUATION_FORM_PATHS.has(formPath) ||
    Boolean(instance?.formVariables?.sampleEvaluationId)
  );
}

function isOnboardingApplyProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    ONBOARDING_APPLY_PROCESS_KEYS.includes(processKey) ||
    ONBOARDING_APPLY_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    ONBOARDING_APPLY_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.onboardingApplyId ||
      instance?.formVariables?.applyNo,
    )
  );
}

function isSupplierExitApprovalProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  const formPath =
    definition?.formCustomViewPath ||
    instance?.processDefinition?.formCustomViewPath ||
    '';
  return (
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.includes(processKey) ||
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.some((key) =>
      instance?.processDefinitionId?.startsWith(`${key}:`),
    ) ||
    SUPPLIER_EXIT_APPROVAL_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.supplierExitApprovalId ||
        instance?.formVariables?.exitNo,
    )
  );
}

function isNcrProcess(
  definition?: Record<string, any>,
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey =
    definition?.key ||
    instance?.processDefinition?.key ||
    instance?.processDefinitionId?.split(':')?.[0] ||
    '';
  return (
    NCR_PROCESS_KEYS.has(processKey) ||
    NCR_FORM_PATHS.has(definition?.formCustomViewPath) ||
    NCR_FORM_PATHS.has(instance?.processDefinition?.formCustomViewPath) ||
    Boolean(
      instance?.formVariables?.ncNo || instance?.formVariables?.ncRecordId,
    )
  );
}

/** 获取流程模型视图*/
async function getProcessModelView() {
  if (BpmModelType.BPMN === processDefinition.value?.modelType) {
    // 重置，解决 BPMN 流程图刷新不会重新渲染问题
    processModelView.value = {
      bpmnXml: '',
    };
  }
  try {
    const data = await getProcessInstanceBpmnModelView(props.id);
    if (data) {
      processModelView.value = data;
    }
  } catch {
    processModelView.value = {};
  }
}

/** 设置表单权限 */
function setFieldPermission(field: string, permission: string) {
  if (permission === BpmFieldPermissionType.READ) {
    fApi.value?.disabled(true, field);
  }
  if (permission === BpmFieldPermissionType.WRITE) {
    fApi.value?.disabled(false, field);
    // 加入可以编辑的字段
    writableFields.push(field);
  }
  if (permission === BpmFieldPermissionType.NONE) {
    fApi.value?.hidden(true, field);
  }
}

/** 操作成功后刷新 */
const refresh = () => {
  // 重新获取详情
  getDetail();
};

const [PrintModal, printModalApi] = useVbenModal({
  connectedComponent: ProcessssPrint,
  destroyOnClose: true,
});

/** 打开打印对话框 */
function handlePrint() {
  printModalApi.setData({ processInstanceId: props.id }).open();
}

function confirmByModal(title: string, content: string) {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      title,
      content,
      okText: '确定',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    });
  });
}

async function handleOpenAdjust() {
  if (!canAdjustStep.value) {
    message.warning('只有当前流程发起人可以调整运行中的流程步骤');
    return;
  }
  adjustForm.targetTaskDefinitionKey = undefined;
  adjustForm.reason = '';
  adjustForm.clearBusinessData = false;
  adjustNodes.value = [];
  adjustModalOpen.value = true;
  adjustLoading.value = true;
  try {
    adjustNodes.value = await getAdjustableProcessInstanceNodes(props.id);
    if (adjustNodes.value.length === 0) {
      message.warning('当前流程暂无可调整的已办理步骤');
    }
  } finally {
    adjustLoading.value = false;
  }
}

async function handleSubmitAdjust() {
  if (!props.id) {
    return;
  }
  if (!adjustForm.targetTaskDefinitionKey) {
    message.warning('请选择要调整到的已办理步骤');
    return;
  }
  if (!adjustForm.reason.trim()) {
    message.warning('请填写调整原因');
    return;
  }
  const confirmed = await confirmByModal(
    '调整流程步骤',
    '确认将流程调整到所选已办理步骤，并由该步骤对应办理人重新办理？',
  );
  if (!confirmed) {
    return;
  }
  adjustSubmitting.value = true;
  try {
    await adjustProcessInstanceStep({
      id: props.id,
      targetTaskDefinitionKey: adjustForm.targetTaskDefinitionKey,
      reason: adjustForm.reason.trim(),
      clearBusinessData: adjustForm.clearBusinessData,
    });
    message.success('流程步骤已调整');
    adjustModalOpen.value = false;
    await getDetail();
    taskListRef.value?.refresh();
  } finally {
    adjustSubmitting.value = false;
  }
}

/** 监听 Tab 切换，当切换到 "record" 标签时刷新任务列表 */
watch(
  () => activeTab.value,
  async (newVal) => {
    if (newVal === 'record') {
      // 如果切换到流转记录标签，刷新任务列表
      await nextTick();
      taskListRef.value?.refresh();
    }
  },
);
const loading = ref(false);
/** 初始化 */
onMounted(async () => {
  try {
    loading.value = true;
    await getDetail();
    // 获得用户列表
    userOptions.value = await getSimpleUserList();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <Page auto-content-height v-loading="loading">
    <Card
      class="flex h-full flex-col"
      :body-style="{
        flex: 1,
        overflowY: 'hidden',
        paddingTop: '12px',
      }"
    >
      <template #title>
        <div class="flex items-center gap-4">
          <span class="text-gray-500">编号：{{ id || '-' }}</span>
          <IconifyIcon
            icon="lucide:printer"
            class="cursor-pointer hover:text-primary"
            @click="handlePrint"
          />
        </div>
      </template>
      <template #extra>
        <Button
          v-if="canAdjustStep"
          ghost
          type="primary"
          :loading="adjustLoading"
          @click="handleOpenAdjust"
        >
          <IconifyIcon icon="lucide:route" />
          <span class="ml-1">调整步骤</span>
        </Button>
      </template>

      <div class="flex h-full flex-col">
        <!-- 流程基本信息 -->
        <div class="flex flex-col gap-2">
          <div class="mb-2.5 flex h-10 items-center gap-5">
            <div class="mb-1 text-2xl font-bold">
              {{ processInstance?.name }}
            </div>
            <DictTag
              v-if="processInstance?.status"
              :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS"
              :value="processInstance.status"
            />
          </div>

          <div class="mb-2.5 flex h-12 items-center gap-5 text-sm">
            <div
              class="flex items-center gap-2 rounded-3xl bg-gray-100 px-2.5 py-1 dark:bg-gray-600"
            >
              <Avatar
                :size="28"
                v-if="processInstance?.startUser?.avatar"
                :src="processInstance?.startUser?.avatar"
              />
              <Avatar
                :size="28"
                v-else-if="processInstance?.startUser?.nickname"
              >
                {{ processInstance?.startUser?.nickname.substring(0, 1) }}
              </Avatar>
              <span class="text-sm">
                {{ processInstance?.startUser?.nickname }}
              </span>
            </div>
            <div class="text-gray-500">
              {{ formatDateTime(processInstance?.startTime) }} 提交
            </div>
          </div>

          <component
            v-if="processInstance?.status"
            :is="auditIconsMap[processInstance?.status]"
            class="absolute right-5 top-2.5 size-36"
          />
        </div>

        <!-- 流程操作 -->
        <div class="flex h-full flex-1 flex-col">
          <Tabs v-model:active-key="activeTab">
            <TabPane :tab="formTabLabel" key="form" class="pb-20 pr-3">
              <Row :gutter="[48, 24]">
                <Col :xs="24" :sm="24" :md="18" :lg="18" :xl="16">
                  <!-- 流程表单 -->
                  <div
                    v-if="
                      processDefinition?.formType === BpmModelFormType.NORMAL
                    "
                  >
                    <form-create
                      v-model="detailForm.value"
                      v-model:api="fApi"
                      :option="detailForm.option"
                      :rule="detailForm.rule"
                    />
                  </div>
                  <div
                    v-else-if="
                      processDefinition?.formType === BpmModelFormType.CUSTOM &&
                      BusinessFormComponent
                    "
                  >
                    <component
                      :is="BusinessFormComponent"
                      :id="businessFormId"
                    />
                  </div>
                </Col>
                <Col :xs="24" :sm="24" :md="6" :lg="6" :xl="8">
                  <div class="mt-4">
                    <ProcessInstanceTimeline :activity-nodes="activityNodes" />
                  </div>
                </Col>
              </Row>
            </TabPane>
            <TabPane
              tab="流程图"
              key="diagram"
              class="pb-20 pr-3"
              :force-render="true"
            >
              <ProcessInstanceSimpleViewer
                v-show="
                  processDefinition.modelType &&
                  processDefinition.modelType === BpmModelType.SIMPLE
                "
                :loading="processInstanceLoading"
                :model-view="processModelView"
              />
              <ProcessInstanceBpmnViewer
                v-show="
                  processDefinition.modelType &&
                  processDefinition.modelType === BpmModelType.BPMN
                "
                :loading="processInstanceLoading"
                :model-view="processModelView"
              />
            </TabPane>
            <TabPane tab="流转记录" key="record" class="pb-20 pr-3">
              <BpmProcessInstanceTaskList
                ref="taskListRef"
                :loading="processInstanceLoading"
                :id="id"
              />
            </TabPane>
            <!-- TODO 待开发 -->
            <TabPane tab="流转评论" key="comment" v-if="false" class="pr-3">
              <div class="h-full">待开发</div>
            </TabPane>
          </Tabs>
        </div>
      </div>

      <template #actions>
        <div class="px-4">
          <ProcessInstanceOperationButton
            ref="operationButtonRef"
            :process-instance="processInstance"
            :process-definition="processDefinition"
            :user-options="userOptions"
            :normal-form="detailForm"
            :normal-form-api="fApi"
            :writable-fields="writableFields"
            @success="refresh"
          />
        </div>
      </template>
    </Card>
    <!-- 打印对话框 -->
    <PrintModal />
    <AntModal
      v-model:open="adjustModalOpen"
      :confirm-loading="adjustSubmitting"
      :mask-closable="false"
      title="调整流程步骤"
      width="560px"
      @ok="handleSubmitAdjust"
    >
      <Form layout="vertical">
        <Form.Item label="调整到已办理步骤" required>
          <Select
            v-model:value="adjustForm.targetTaskDefinitionKey"
            :loading="adjustLoading"
            :options="adjustNodeOptions"
            placeholder="请选择已办理步骤"
          />
        </Form.Item>
        <Form.Item label="调整原因" required>
          <Textarea
            v-model:value="adjustForm.reason"
            :rows="3"
            allow-clear
            placeholder="请填写调整原因，系统将记录调整时间和调整人"
          />
        </Form.Item>
      </Form>
    </AntModal>
  </Page>
</template>

<style lang="scss" scoped>
:deep(.ant-tabs) {
  display: flex;
  flex-direction: column;
  height: 100%;

  .ant-tabs-content {
    height: 100%;
  }
}

:deep(.ant-tabs-tabpane) {
  height: 100%;
  overflow-y: auto;
}
</style>
