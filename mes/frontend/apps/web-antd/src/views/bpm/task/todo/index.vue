<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { BpmTaskApi } from '#/api/bpm/task';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProcessInstance } from '#/api/bpm/processInstance';
import { getTaskTodoPage } from '#/api/bpm/task';
import { getExceptionEvent } from '#/api/mes/quality/abnormal/exception';
import ExceptionDetailModal from '#/views/mes/quality/abnormal/exception/modules/detail-modal.vue';
import NcrDetailModal from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';
import DeviceExceptionDetailModal from '#/views/mes/resource/device/fault-repair/modules/form.vue';
import SupplierExitApprovalDetailModal from '#/views/mes/srm/archive/exit/modules/detail-modal.vue';
import OnboardingApplyDetailModal from '#/views/mes/srm/certification/import-application/modules/detail-modal.vue';
import SampleEvaluationDetailModal from '#/views/mes/srm/certification/sample-evaluation/modules/detail-modal.vue';
import TrialValidationDetailModal from '#/views/mes/srm/certification/trial-validation/modules/detail-modal.vue';
import SampleRequestDetailModal from '#/views/mes/srm/onboarding/sample-req/modules/detail-modal.vue';

import BusinessFormModal from '../../processInstance/detail/modules/business-form-modal.vue';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'BpmTodoTask' });

const NCR_PROCESS_KEYS = [
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
];
const NCR_FORM_PATHS = new Set([
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
]);
const EXCEPTION_PROCESS_KEYS = ['qms_exception_event'];
const EXCEPTION_FORM_PATHS = new Set(['/mes/quality/abnormal/exception']);
const DEVICE_EXCEPTION_PROCESS_KEYS = ['resource_device_exception'];
const DEVICE_EXCEPTION_FORM_PATHS = new Set([
  '/mes/resource/device/fault-repair',
]);
const EXCEPTION_GROUP_BPM_TASK_KEYS = new Set(['containment', 'root_cause']);
const SAMPLE_REQUEST_PROCESS_KEYS = ['srm_sample_request'];
const SAMPLE_REQUEST_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-req',
]);
const SAMPLE_EVALUATION_PROCESS_KEYS = ['srm_sample_evaluation'];
const SAMPLE_EVALUATION_FORM_PATHS = new Set([
  '/mes/srm/certification/sample-evaluation',
]);
const TRIAL_VALIDATION_PROCESS_KEYS = ['srm_trial_validation'];
const TRIAL_VALIDATION_FORM_PATHS = new Set([
  '/mes/srm/certification/trial-validation',
]);
const ONBOARDING_APPLY_PROCESS_KEYS = ['srm_onboarding_apply'];
const ONBOARDING_APPLY_FORM_PATHS = new Set([
  '/mes/srm/certification/import-application',
]);
const SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS = ['srm_supplier_exit_approval'];
const SUPPLIER_EXIT_APPROVAL_FORM_PATHS = new Set([
  '/mes/srm/certification/exit',
]);

/** 办理任务 */
function handleAudit(row: BpmTaskApi.Task) {
  void openBusinessForm(row);
}

/** 查看业务单据详情 */
function handleBusinessFormDetail(row: BpmTaskApi.Task) {
  void openBusinessForm(row);
}

async function openBusinessForm(row: BpmTaskApi.Task) {
  const instance = await resolveBusinessDetailInstance(row);
  if (tryOpenNcrDetail(instance, 'todo')) {
    return;
  }
  if (tryOpenDeviceExceptionDetail(instance, 'todo')) {
    return;
  }
  if (tryOpenExceptionDetail(instance, 'todo')) {
    return;
  }
  if (tryOpenTrialValidationDetail(instance, row.id, 'todo')) {
    return;
  }
  if (await tryOpenSampleEvaluationDetail(instance, row.id, 'todo')) {
    return;
  }
  if (await tryOpenSampleRequestDetail(instance, row.id, 'todo')) {
    return;
  }
  if (tryOpenOnboardingApplyDetail(instance, row.id, 'todo')) {
    return;
  }
  if (tryOpenSupplierExitApprovalDetail(instance, row.id, 'todo')) {
    return;
  }
  businessFormModalApi
    .setData({
      businessKey: instance?.businessKey,
      formVariables: instance?.formVariables,
      processDefinition: instance?.processDefinition,
      processInstanceId: instance?.id || row.processInstanceId,
      processInstance: instance,
      taskId: row.id,
      title: `单据详情 - ${instance?.name || row.processInstanceId}`,
    })
    .open();
}

async function resolveBusinessDetailInstance(row: BpmTaskApi.Task) {
  const instance = row.processInstance;
  if (
    instance &&
    (!(
      isNcrProcess(instance) ||
      isDeviceExceptionProcess(instance) ||
      isExceptionProcess(instance) ||
      isTrialValidationProcess(instance) ||
      isSampleRequestProcess(instance) ||
      isSampleEvaluationProcess(instance) ||
      isOnboardingApplyProcess(instance) ||
      isSupplierExitApprovalProcess(instance)
    ) ||
      resolveBusinessKey(instance))
  ) {
    return instance;
  }
  return await getProcessInstance(row.processInstanceId);
}

function tryOpenNcrDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isNcrProcess(instance) || !businessKey) {
    return false;
  }
  ncrDetailModalApi.setData({ id: Number(businessKey), tabType }).open();
  return true;
}

function tryOpenDeviceExceptionDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isDeviceExceptionProcess(instance) || !businessKey) {
    return false;
  }
  deviceExceptionDetailModalApi
    .setData({ id: Number(businessKey), tabType })
    .open();
  return true;
}

function tryOpenExceptionDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isExceptionProcess(instance) || !businessKey) {
    return false;
  }
  exceptionDetailModalApi.setData({ id: Number(businessKey), tabType }).open();
  return true;
}

function tryOpenTrialValidationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isTrialValidationProcess(instance) || !businessKey) {
    return false;
  }
  trialValidationDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

async function tryOpenSampleRequestDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isSampleRequestProcess(instance) || !businessKey) {
    return false;
  }
  sampleRequestDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

async function tryOpenSampleEvaluationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isSampleEvaluationProcess(instance) || !businessKey) {
    return false;
  }
  sampleEvaluationDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function tryOpenOnboardingApplyDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isOnboardingApplyProcess(instance) || !businessKey) {
    return false;
  }
  onboardingApplyDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function tryOpenSupplierExitApprovalDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(instance);
  if (!isSupplierExitApprovalProcess(instance) || !businessKey) {
    return false;
  }
  supplierExitApprovalDetailModalApi
    .setData({
      id: Number(businessKey),
      ...(instance?.id ? { processInstanceId: String(instance.id) } : {}),
      ...(taskId ? { taskId: String(taskId) } : {}),
      ...(tabType ? { tabType } : {}),
    })
    .open();
  return true;
}

function isNcrProcess(instance?: BpmProcessInstanceApi.ProcessInstance) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    NCR_PROCESS_KEYS.includes(processKey) ||
    NCR_PROCESS_KEYS.some((key) => processDefinitionId.startsWith(`${key}:`)) ||
    NCR_FORM_PATHS.has(formPath)
  );
}

function isExceptionProcess(instance?: BpmProcessInstanceApi.ProcessInstance) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    EXCEPTION_PROCESS_KEYS.includes(processKey) ||
    EXCEPTION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    EXCEPTION_FORM_PATHS.has(formPath) ||
    Boolean(instance?.formVariables?.exceptionNo) ||
    Boolean(instance?.formVariables?.exceptionEventId)
  );
}

function isDeviceExceptionProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    DEVICE_EXCEPTION_PROCESS_KEYS.includes(processKey) ||
    DEVICE_EXCEPTION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    DEVICE_EXCEPTION_FORM_PATHS.has(formPath) ||
    instance?.formVariables?.mesWorkflowBusinessType === 'DEVICE_EXCEPTION' ||
    Boolean(
      instance?.formVariables?.deviceExceptionId ||
      instance?.formVariables?.deviceExceptionNo,
    )
  );
}

function isSampleRequestProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SAMPLE_REQUEST_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_REQUEST_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SAMPLE_REQUEST_FORM_PATHS.has(formPath) ||
    Boolean(
      (instance?.formVariables?.sampleRequestId &&
        !instance?.formVariables?.sampleEvaluationId) ||
      instance?.formVariables?.requestNo,
    )
  );
}

function isSampleEvaluationProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SAMPLE_EVALUATION_PROCESS_KEYS.includes(processKey) ||
    SAMPLE_EVALUATION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SAMPLE_EVALUATION_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.sampleEvaluationId ||
      instance?.formVariables?.evaluationNo,
    )
  );
}

function isTrialValidationProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    TRIAL_VALIDATION_PROCESS_KEYS.includes(processKey) ||
    TRIAL_VALIDATION_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    TRIAL_VALIDATION_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.trialValidationId ||
      instance?.formVariables?.trialNo,
    )
  );
}

function isOnboardingApplyProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    ONBOARDING_APPLY_PROCESS_KEYS.includes(processKey) ||
    ONBOARDING_APPLY_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    ONBOARDING_APPLY_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.onboardingApplyId ||
      instance?.formVariables?.applyNo,
    )
  );
}

function isSupplierExitApprovalProcess(
  instance?: BpmProcessInstanceApi.ProcessInstance,
) {
  const processKey = instance?.processDefinition?.key || '';
  const processDefinitionId = instance?.processDefinitionId || '';
  const formPath = instance?.processDefinition?.formCustomViewPath || '';
  return (
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.includes(processKey) ||
    SUPPLIER_EXIT_APPROVAL_PROCESS_KEYS.some((key) =>
      processDefinitionId.startsWith(`${key}:`),
    ) ||
    SUPPLIER_EXIT_APPROVAL_FORM_PATHS.has(formPath) ||
    Boolean(
      instance?.formVariables?.supplierExitApprovalId ||
      instance?.formVariables?.exitNo,
    )
  );
}

function resolveBusinessKey(instance?: BpmProcessInstanceApi.ProcessInstance) {
  return (
    instance?.businessKey ||
    instance?.formVariables?.trialValidationId ||
    instance?.formVariables?.deviceExceptionId ||
    instance?.formVariables?.supplierExitApprovalId ||
    instance?.formVariables?.onboardingApplyId ||
    instance?.formVariables?.sampleEvaluationId ||
    instance?.formVariables?.sampleRequestId ||
    instance?.formVariables?.exceptionEventId ||
    instance?.formVariables?.ncRecordId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id
  );
}

async function filterHiddenExceptionDoneTodoResult(result: any) {
  const list = Array.isArray(result?.list) ? result.list : [];
  if (list.length === 0) {
    return result;
  }
  const hiddenFlags = await Promise.all(
    list.map((item: BpmTaskApi.Task) => isHiddenExceptionDoneTodo(item)),
  );
  const nextList = list.filter((_: BpmTaskApi.Task, index: number) => {
    return hiddenFlags[index] !== true;
  });
  const hiddenCount = list.length - nextList.length;
  return {
    ...result,
    list: nextList,
    total: Math.max(0, Number(result?.total || 0) - hiddenCount),
  };
}

async function isHiddenExceptionDoneTodo(row: BpmTaskApi.Task) {
  if (!EXCEPTION_GROUP_BPM_TASK_KEYS.has(row.taskDefinitionKey)) {
    return false;
  }
  try {
    const instance = await resolveBusinessDetailInstance(row);
    if (!isExceptionProcess(instance)) {
      return false;
    }
    const businessKey = resolveBusinessKey(instance);
    if (!businessKey) {
      return false;
    }
    const detail = await getExceptionEvent(Number(businessKey));
    return (
      detail?.currentUserTaskDone === true &&
      detail?.currentUserTaskTodo !== true
    );
  } catch {
    return false;
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getTaskTodoPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return await filterHiddenExceptionDoneTodoResult(result);
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<BpmTaskApi.Task>,
});

const [BusinessFormDetailModal, businessFormModalApi] = useVbenModal({
  connectedComponent: BusinessFormModal,
  destroyOnClose: true,
});

const [NcrDetailModalHost, ncrDetailModalApi] = useVbenModal({
  connectedComponent: NcrDetailModal,
  destroyOnClose: true,
});

const [ExceptionDetailModalHost, exceptionDetailModalApi] = useVbenModal({
  connectedComponent: ExceptionDetailModal,
  destroyOnClose: true,
});

const [DeviceExceptionDetailModalHost, deviceExceptionDetailModalApi] =
  useVbenModal({
    connectedComponent: DeviceExceptionDetailModal,
    destroyOnClose: true,
  });

const [SampleRequestDetailModalHost, sampleRequestDetailModalApi] =
  useVbenModal({
    connectedComponent: SampleRequestDetailModal,
    destroyOnClose: true,
  });

const [SampleEvaluationDetailModalHost, sampleEvaluationDetailModalApi] =
  useVbenModal({
    connectedComponent: SampleEvaluationDetailModal,
    destroyOnClose: true,
  });

const [TrialValidationDetailModalHost, trialValidationDetailModalApi] =
  useVbenModal({
    connectedComponent: TrialValidationDetailModal,
    destroyOnClose: true,
  });

const [OnboardingApplyDetailModalHost, onboardingApplyDetailModalApi] =
  useVbenModal({
    connectedComponent: OnboardingApplyDetailModal,
    destroyOnClose: true,
  });

const [
  SupplierExitApprovalDetailModalHost,
  supplierExitApprovalDetailModalApi,
] = useVbenModal({
  connectedComponent: SupplierExitApprovalDetailModal,
  destroyOnClose: true,
});
</script>

<template>
  <Page auto-content-height>
    <BusinessFormDetailModal />
    <NcrDetailModalHost @success="gridApi.query()" />
    <ExceptionDetailModalHost @success="gridApi.query()" />
    <DeviceExceptionDetailModalHost @success="gridApi.query()" />
    <SampleRequestDetailModalHost @success="gridApi.query()" />
    <SampleEvaluationDetailModalHost @success="gridApi.query()" />
    <TrialValidationDetailModalHost @success="gridApi.query()" />
    <OnboardingApplyDetailModalHost @success="gridApi.query()" />
    <SupplierExitApprovalDetailModalHost @success="gridApi.query()" />
    <template #doc-disabled>
      <DocAlert
        title="审批通过、不通过、驳回"
        url="https://doc.iocoder.cn/bpm/task-todo-done/"
      />
      <DocAlert title="审批加签、减签" url="https://doc.iocoder.cn/bpm/sign/" />
      <DocAlert
        title="审批转办、委派、抄送"
        url="https://doc.iocoder.cn/bpm/task-delegation-and-cc/"
      />
      <DocAlert title="审批加签、减签" url="https://doc.iocoder.cn/bpm/sign/" />
    </template>

    <Grid table-title="待办任务">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '办理',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              auth: ['bpm:task:query'],
              onClick: handleAudit.bind(null, row),
            },
            {
              label: '单据详情',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              auth: ['bpm:task:query'],
              onClick: handleBusinessFormDetail.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
