<script lang="ts" setup>
import type { SrmTrialValidationApi } from '#/api/mes/srm/trial-validation';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Spin,
} from 'ant-design-vue';

import {
  archiveConfirm,
  completeProduction,
  getTrialValidation,
  trialExecute,
} from '#/api/mes/srm/trial-validation';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';
import SrmTrialValidationInlineDetail from './inline-detail.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmTrialValidationDetailModal' });

const emit = defineEmits<{ close: []; success: [] }>();

type WorkflowAction = 'archive' | 'complete' | 'trial';

interface ModalData {
  id?: number | string;
  processInstanceId?: number | string;
  taskId?: number | string;
}

const BIZ_TYPE = 'SRM_TRIAL_TRACK';
const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '确认归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  NOTICE_SENT: { color: 'blue', text: '发起试生产通知' },
  PRODUCTION_COMPLETE: { color: 'processing', text: '完成生产' },
  TRIAL_EXECUTION: { color: 'processing', text: '试生产执行' },
};
const attachmentCategoryOptions = [
  { label: '试生产通知', value: 'TRIAL_NOTICE' },
  { label: '试生产执行记录', value: 'TRIAL_EXECUTION_RECORD' },
  { label: '完成生产记录', value: 'PRODUCTION_COMPLETE_RECORD' },
  { label: '其他附件', value: 'OTHER' },
];

const loading = ref(false);
const saving = ref(false);
const record = ref<SrmTrialValidationApi.TrialValidation>();
const modalData = ref<ModalData>({});
const detailRefreshKey = ref(0);
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('trial');
const workflowForm = ref({
  materialBatchNo: '',
  opinion: '',
  quantity: undefined as number | undefined,
});
const workflowAttachmentPanelRef =
  ref<InstanceType<typeof SrmAttachmentPanel>>();

const statusMeta = computed(
  () =>
    statusMetaMap[String(record.value?.status || '')] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);
const subtitleItems = computed(() =>
  [
    record.value?.trialNo || '',
    record.value?.supplierName || '',
    record.value?.materialName || '',
    statusMeta.value.text,
  ].filter(Boolean),
);
const availableActions = computed(() => {
  const actions: Array<{ action: WorkflowAction; label: string }> = [];
  if (record.value?.canTrialExecute) {
    actions.push({ action: 'trial', label: '试生产执行' });
  }
  if (record.value?.canCompleteProduction) {
    actions.push({ action: 'complete', label: '完成生产' });
  }
  if (record.value?.canArchiveConfirm) {
    actions.push({ action: 'archive', label: '确认归档' });
  }
  return actions;
});
const workflowTitle = computed(
  () =>
    availableActions.value.find((item) => item.action === workflowAction.value)
      ?.label || '办理动作',
);
const workflowSubmitText = computed(() => {
  if (workflowAction.value === 'trial') {
    return '提交执行';
  }
  if (workflowAction.value === 'complete') {
    return '提交完成';
  }
  return '确认归档';
});
const workflowAttachmentDefaultCategory = computed(() =>
  workflowAction.value === 'complete'
    ? 'PRODUCTION_COMPLETE_RECORD'
    : 'TRIAL_EXECUTION_RECORD',
);
const shouldEditBatchAndQuantity = computed(
  () => workflowAction.value !== 'archive',
);

const [Modal, modalApi] = useVbenModal({
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
  async onOpenChange(isOpen) {
    if (!isOpen) {
      workflowModalOpen.value = false;
      record.value = undefined;
      return;
    }
    modalData.value = modalApi.getData<ModalData>() || {};
    await loadDetail();
  },
});
const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});

async function loadDetail() {
  const id = normalizeId(modalData.value.id);
  if (!id) {
    message.warning('缺少试产验证跟踪单 ID');
    return;
  }
  loading.value = true;
  try {
    record.value = await getTrialValidation(id);
    if (modalData.value.processInstanceId && !record.value.processInstanceId) {
      record.value.processInstanceId = String(
        modalData.value.processInstanceId,
      );
    }
    detailRefreshKey.value += 1;
  } finally {
    loading.value = false;
  }
}

function closeDetail() {
  emit('close');
  void modalApi.close();
}

function openAuditLog() {
  if (!record.value?.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      id: record.value.processInstanceId,
      processInstanceId: record.value.processInstanceId,
      title: `审批日志 - ${record.value.trialNo || '试产验证跟踪'}`,
    })
    .open();
}

function openWorkflow(action: WorkflowAction) {
  workflowAction.value = action;
  workflowForm.value = {
    materialBatchNo: record.value?.materialBatchNo || '',
    opinion: '',
    quantity: record.value?.quantity,
  };
  workflowModalOpen.value = true;
}

function openOnlyAction() {
  const action = availableActions.value[0];
  if (action) {
    openWorkflow(action.action);
  }
}

async function submitWorkflow() {
  if (!record.value?.id) {
    return;
  }
  if (shouldEditBatchAndQuantity.value) {
    if (!workflowForm.value.materialBatchNo.trim()) {
      message.warning('请填写物料批号');
      return;
    }
    if (
      !workflowForm.value.quantity ||
      Number(workflowForm.value.quantity) <= 0
    ) {
      message.warning('请填写大于 0 的数量');
      return;
    }
  }
  saving.value = true;
  try {
    if (shouldEditBatchAndQuantity.value) {
      await workflowAttachmentPanelRef.value?.syncAttachments({
        bizId: record.value.id,
        bizType: BIZ_TYPE,
      });
    }
    if (workflowAction.value === 'trial') {
      await trialExecute({
        id: record.value.id,
        materialBatchNo: workflowForm.value.materialBatchNo.trim(),
        opinion: workflowForm.value.opinion.trim(),
        quantity: Number(workflowForm.value.quantity),
      });
    } else if (workflowAction.value === 'complete') {
      await completeProduction({
        id: record.value.id,
        materialBatchNo: workflowForm.value.materialBatchNo.trim(),
        opinion: workflowForm.value.opinion.trim(),
        quantity: Number(workflowForm.value.quantity),
      });
    } else {
      await archiveConfirm({
        id: record.value.id,
        opinion: workflowForm.value.opinion.trim(),
      });
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    await loadDetail();
    emit('success');
  } finally {
    saving.value = false;
  }
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}
</script>

<template>
  <Modal>
    <ProcessAuditModal />
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
            <div class="qms-ncr-title-panel__name">试产验证跟踪</div>
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
              v-if="record?.processInstanceId"
              class="qms-ncr-toolbar-action"
              title="审批日志"
              @click="openAuditLog"
            >
              <IconifyIcon icon="ant-design:profile-outlined" />
            </Button>
            <Button
              v-if="availableActions.length === 1"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openOnlyAction"
            >
              {{ availableActions[0]?.label }}
            </Button>
            <Dropdown v-else-if="availableActions.length > 1">
              <Button class="qms-ncr-toolbar-action" type="primary">
                办理动作
                <IconifyIcon icon="ant-design:down-outlined" />
              </Button>
              <template #overlay>
                <Menu>
                  <Menu.Item
                    v-for="item in availableActions"
                    :key="item.action"
                    @click="openWorkflow(item.action)"
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
            <div class="qms-exception-form">
              <SrmTrialValidationInlineDetail
                :key="detailRefreshKey"
                :id="record?.id"
              />
            </div>
          </div>
        </div>
      </div>
    </Spin>

    <AntModal
      v-model:open="workflowModalOpen"
      :confirm-loading="saving"
      :mask-closable="false"
      :ok-text="workflowSubmitText"
      :title="workflowTitle"
      :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
      width="820px"
      @ok="submitWorkflow"
    >
      <Form layout="vertical">
        <template v-if="shouldEditBatchAndQuantity">
          <Form.Item label="物料批号" required>
            <Input
              v-model:value="workflowForm.materialBatchNo"
              placeholder="请输入物料批号"
            />
          </Form.Item>
          <Form.Item label="数量" required>
            <InputNumber
              v-model:value="workflowForm.quantity"
              class="!w-full"
              :min="0"
              :precision="6"
              placeholder="请输入数量"
            />
          </Form.Item>
        </template>
        <Form.Item label="办理意见">
          <Input.TextArea
            v-model:value="workflowForm.opinion"
            :maxlength="500"
            placeholder="请输入办理意见"
            :rows="4"
            show-count
          />
        </Form.Item>
        <SrmAttachmentPanel
          v-if="shouldEditBatchAndQuantity"
          ref="workflowAttachmentPanelRef"
          :biz-id="record?.id"
          :biz-type="BIZ_TYPE"
          :category-options="attachmentCategoryOptions"
          :default-category="workflowAttachmentDefaultCategory"
          mode="edit"
          :modal-z-index="SRM_CONFIRM_MODAL_Z_INDEX + 100"
        />
      </Form>
    </AntModal>
  </Modal>
</template>
