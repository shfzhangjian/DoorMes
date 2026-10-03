<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { BpmTaskApi } from '#/api/bpm/task';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProcessInstance } from '#/api/bpm/processInstance';
import { getTaskDonePage, withdrawTask } from '#/api/bpm/task';
import { DictTag } from '#/components/dict-tag';
import NcrDetailModal from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';
import SupplierExitApprovalDetailModal from '#/views/mes/srm/archive/exit/modules/detail-modal.vue';
import OnboardingApplyDetailModal from '#/views/mes/srm/certification/import-application/modules/detail-modal.vue';
import SampleEvaluationDetailModal from '#/views/mes/srm/certification/sample-evaluation/modules/detail-modal.vue';
import TrialValidationDetailModal from '#/views/mes/srm/certification/trial-validation/modules/detail-modal.vue';
import SampleRequestDetailModal from '#/views/mes/srm/onboarding/sample-req/modules/detail-modal.vue';

import BusinessFormModal from '../../processInstance/detail/modules/business-form-modal.vue';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'BpmDoneTask' });

const NCR_PROCESS_KEYS = [
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
];
const NCR_FORM_PATHS = new Set([
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
]);
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

/** 查看业务单据详情 */
async function handleBusinessFormDetail(row: BpmTaskApi.Task) {
  const instance = await resolveBusinessDetailInstance(row);
  if (tryOpenNcrDetail(instance)) {
    return;
  }
  if (tryOpenTrialValidationDetail(instance, row.id)) {
    return;
  }
  if (tryOpenSampleEvaluationDetail(instance, row.id)) {
    return;
  }
  if (tryOpenSampleRequestDetail(instance, row.id)) {
    return;
  }
  if (tryOpenOnboardingApplyDetail(instance, row.id)) {
    return;
  }
  if (tryOpenSupplierExitApprovalDetail(instance, row.id)) {
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

function tryOpenNcrDetail(instance?: BpmProcessInstanceApi.ProcessInstance) {
  const businessKey = resolveBusinessKey(instance);
  if (!isNcrProcess(instance) || !businessKey) {
    return false;
  }
  ncrDetailModalApi.setData({ id: Number(businessKey) }).open();
  return true;
}

function tryOpenTrialValidationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
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
      tabType: 'processed',
    })
    .open();
  return true;
}

function tryOpenSampleRequestDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
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
      tabType: 'processed',
    })
    .open();
  return true;
}

function tryOpenSampleEvaluationDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
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
      tabType: 'processed',
    })
    .open();
  return true;
}

function tryOpenOnboardingApplyDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
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
      tabType: 'processed',
    })
    .open();
  return true;
}

function tryOpenSupplierExitApprovalDetail(
  instance?: BpmProcessInstanceApi.ProcessInstance,
  taskId?: string,
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
      tabType: 'processed',
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
    instance?.formVariables?.supplierExitApprovalId ||
    instance?.formVariables?.onboardingApplyId ||
    instance?.formVariables?.sampleEvaluationId ||
    instance?.formVariables?.sampleRequestId ||
    instance?.formVariables?.ncRecordId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id
  );
}

/** 撤回任务 */
async function handleWithdraw(row: BpmTaskApi.Task) {
  const hideLoading = message.loading({
    content: '正在撤回中...',
    duration: 0,
  });
  try {
    await withdrawTask(row.id);
    message.success('撤回成功');
    await gridApi.query();
  } finally {
    hideLoading();
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
          return await getTaskDonePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
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

    <Grid table-title="已办任务">
      <template #processStatus="{ row }">
        <DictTag
          :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS"
          :value="row.processInstance?.status ?? row.status"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '撤回',
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              popConfirm: {
                title: '确定要撤回该任务吗？',
                confirm: handleWithdraw.bind(null, row),
              },
            },
            {
              label: '详情',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              onClick: handleBusinessFormDetail.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
