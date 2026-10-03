<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';

import { nextTick, ref, shallowRef } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { BpmFieldPermissionType, BpmModelFormType } from '@vben/constants';

import { Alert, Empty, Spin } from 'ant-design-vue';

import {
  getApprovalDetail,
  getProcessInstance,
} from '#/api/bpm/processInstance';
import { setConfAndFields2 } from '#/components/form-create';
import { registerComponent } from '#/utils';
import ExceptionInlineDetail from '#/views/mes/quality/abnormal/exception/modules/inline-detail.vue';
import NcrInlineDetail from '#/views/mes/quality/abnormal/ncr/modules/inline-detail.vue';
import OnboardingApplyInlineDetail from '#/views/mes/srm/certification/import-application/modules/inline-detail.vue';
import PreliminaryEvaluationInlineDetail from '#/views/mes/srm/certification/preliminary-evaluation/modules/inline-detail.vue';
import SampleEvaluationInlineDetail from '#/views/mes/srm/certification/sample-evaluation/modules/inline-detail.vue';
import SampleRequestInlineDetail from '#/views/mes/srm/onboarding/sample-req/modules/inline-detail.vue';
import SupplierExitApprovalInlineDetail from '#/views/mes/srm/archive/exit/modules/inline-detail.vue';

defineOptions({ name: 'BpmBusinessFormModal' });

interface BusinessFormModalData {
  activityId?: string;
  businessKey?: number | string;
  formVariables?: Record<string, any>;
  processInstanceId?: number | string;
  processDefinition?: Record<string, any>;
  processInstance?: BpmProcessInstanceApi.ProcessInstance;
  taskId?: number | string;
  title?: string;
}

const loading = ref(false);
const errorMessage = ref('');
const processInstance = ref<BpmProcessInstanceApi.ProcessInstance>();
const processDefinition = ref<Record<string, any>>({});
const BusinessFormComponent = shallowRef<any>();
const fApi = ref<any>();
const detailForm = ref({
  rule: [],
  option: {},
  value: {},
});
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

const [Modal, modalApi] = useVbenModal({
  class: 'bpm-business-form-modal',
  contentClass: '!min-h-0 !overflow-hidden !p-0',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  title: '',
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetState();
      return;
    }
    await loadBusinessForm();
  },
});

function resetState() {
  loading.value = false;
  errorMessage.value = '';
  processInstance.value = undefined;
  processDefinition.value = {};
  BusinessFormComponent.value = undefined;
  detailForm.value = {
    rule: [],
    option: {},
    value: {},
  };
}

function closeBusinessForm() {
  modalApi.close();
}

async function loadBusinessForm() {
  const data = modalApi.getData<BusinessFormModalData>();
  if (!data?.processInstanceId) {
    errorMessage.value = '缺少流程实例编号，无法查看单据详情';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  modalApi.lock();
  try {
    const directProcessInstance = buildDirectProcessInstance(data);
    if (
      hasUsableFormDefinition(data.processDefinition) &&
      resolveBusinessKey(directProcessInstance)
    ) {
      await applyBusinessFormData(
        directProcessInstance,
        data.processDefinition,
      );
      return;
    }

    const instance = await getProcessInstance(String(data.processInstanceId));
    if (
      hasUsableFormDefinition(instance?.processDefinition) &&
      resolveBusinessKey(instance)
    ) {
      await applyBusinessFormData(
        instance,
        instance.processDefinition as Record<string, any>,
      );
      return;
    }

    const detail = await getApprovalDetail({
      activityId: data.activityId,
      processInstanceId: data.processInstanceId,
      taskId: data.taskId,
    });
    if (!detail?.processDefinition || !detail?.processInstance) {
      errorMessage.value = '查询不到流程单据信息';
      return;
    }
    await applyBusinessFormData(
      detail.processInstance,
      detail.processDefinition,
      detail.formFieldsPermission,
    );
  } catch {
    errorMessage.value = '获取单据详情失败';
  } finally {
    modalApi.unlock();
    loading.value = false;
  }
}

function hasUsableFormDefinition(definition?: Record<string, any>) {
  if (!definition?.formType) {
    return false;
  }
  if (definition.formType === BpmModelFormType.CUSTOM) {
    return !!definition.formCustomViewPath;
  }
  return definition.formType === BpmModelFormType.NORMAL;
}

function buildDirectProcessInstance(data: BusinessFormModalData) {
  if (data.processInstance) {
    return data.processInstance;
  }
  const businessKey = resolveBusinessKey({
    businessKey: data.businessKey,
    formVariables: data.formVariables || {},
  } as BpmProcessInstanceApi.ProcessInstance);
  if (!businessKey) {
    return undefined;
  }
  return {
    businessKey: String(businessKey),
    formVariables: data.formVariables || {},
    id: data.processInstanceId,
    name: data.title || '',
  } as BpmProcessInstanceApi.ProcessInstance;
}

function resolveBusinessKey(instance?: BpmProcessInstanceApi.ProcessInstance) {
  return (
    instance?.businessKey ||
    instance?.formVariables?.exceptionId ||
    instance?.formVariables?.ncRecordId ||
    instance?.formVariables?.sampleEvaluationId ||
    instance?.formVariables?.evaluationId ||
    instance?.formVariables?.sampleRequestId ||
    instance?.formVariables?.onboardingApplyId ||
    instance?.formVariables?.supplierExitApprovalId ||
    instance?.formVariables?.businessKey ||
    instance?.formVariables?.id ||
    instance?.formVariables?.exceptionNo
  );
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

async function applyBusinessFormData(
  instance: BpmProcessInstanceApi.ProcessInstance,
  definition: Record<string, any>,
  formFieldsPermission?: Record<string, string>,
) {
  const businessKey = resolveBusinessKey(instance);
  processInstance.value = {
    ...instance,
    businessKey: businessKey ? String(businessKey) : instance.businessKey,
  };
  processDefinition.value = definition;

  if (processDefinition.value.formType === BpmModelFormType.NORMAL) {
    setConfAndFields2(
      detailForm,
      processDefinition.value.formConf,
      processDefinition.value.formFields || [],
      processInstance.value.formVariables || {},
    );
    await nextTick();
    fApi.value?.btn.show(false);
    fApi.value?.resetBtn.show(false);
    fApi.value?.disabled(true);
    setReadonlyFieldPermission(formFieldsPermission);
    return;
  }

  if (processDefinition.value.formType === BpmModelFormType.CUSTOM) {
    if (!processInstance.value.businessKey) {
      errorMessage.value = '历史流程未记录业务单据编号，无法打开业务单据详情';
      return;
    }
    const viewPath = processDefinition.value.formCustomViewPath || '';
    if (!viewPath) {
      errorMessage.value = '当前流程未配置业务单据查看路径';
      return;
    }
    if (NCR_FORM_PATHS.has(viewPath)) {
      BusinessFormComponent.value = NcrInlineDetail;
      return;
    }
    if (isExceptionProcess(processDefinition.value, processInstance.value)) {
      BusinessFormComponent.value = ExceptionInlineDetail;
      return;
    }
    if (
      isPreliminaryEvaluationProcess(
        processDefinition.value,
        processInstance.value,
      )
    ) {
      BusinessFormComponent.value = PreliminaryEvaluationInlineDetail;
      return;
    }
    if (
      isSampleEvaluationProcess(processDefinition.value, processInstance.value)
    ) {
      BusinessFormComponent.value = SampleEvaluationInlineDetail;
      return;
    }
    if (
      isSampleRequestProcess(processDefinition.value, processInstance.value)
    ) {
      BusinessFormComponent.value = SampleRequestInlineDetail;
      return;
    }
    if (
      isOnboardingApplyProcess(processDefinition.value, processInstance.value)
    ) {
      BusinessFormComponent.value = OnboardingApplyInlineDetail;
      return;
    }
    if (
      isSupplierExitApprovalProcess(
        processDefinition.value,
        processInstance.value,
      )
    ) {
      BusinessFormComponent.value = SupplierExitApprovalInlineDetail;
      return;
    }
    BusinessFormComponent.value = registerComponent(viewPath);
    if (!BusinessFormComponent.value) {
      errorMessage.value = `未找到业务单据查看组件：${viewPath}`;
    }
    return;
  }

  errorMessage.value = '当前流程未配置可查看的单据表单';
}

function setReadonlyFieldPermission(
  formFieldsPermission?: Record<string, string>,
) {
  if (!formFieldsPermission) {
    return;
  }
  Object.keys(formFieldsPermission).forEach((field) => {
    if (formFieldsPermission[field] === BpmFieldPermissionType.NONE) {
      fApi.value?.hidden(true, field);
    } else {
      fApi.value?.disabled(true, field);
    }
  });
}
</script>

<template>
  <Modal>
    <Spin class="bpm-business-form-modal__spin" :spinning="loading">
      <div class="bpm-business-form-modal__body">
        <Alert
          v-if="errorMessage"
          :message="errorMessage"
          show-icon
          type="warning"
        />
        <template v-else>
          <form-create
            v-if="processDefinition?.formType === BpmModelFormType.NORMAL"
            v-model="detailForm.value"
            v-model:api="fApi"
            :option="detailForm.option"
            :rule="detailForm.rule"
          />
          <component
            :is="BusinessFormComponent"
            v-else-if="
              processDefinition?.formType === BpmModelFormType.CUSTOM &&
              BusinessFormComponent
            "
            class="bpm-business-form-modal__content"
            :id="processInstance?.businessKey"
            @close="closeBusinessForm"
          />
          <Empty v-else description="暂无可查看的单据详情" />
        </template>
      </div>
    </Spin>
  </Modal>
</template>

<style lang="scss" scoped>
.bpm-business-form-modal__spin,
.bpm-business-form-modal__spin :deep(.ant-spin-nested-loading),
.bpm-business-form-modal__spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.bpm-business-form-modal__spin :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.bpm-business-form-modal__body {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.bpm-business-form-modal__body > .bpm-business-form-modal__content {
  min-height: 0;
  flex: 1;
}
</style>
