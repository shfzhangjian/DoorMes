<script lang="ts" setup>
import type { SrmSampleRequestApi } from '#/api/mes/srm/sample-req';
import type { SystemUserApi } from '#/api/system/user';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  message,
  Modal as AntModal,
  Radio,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';

import {
  archiveConfirmSampleRequest,
  finalApproveSampleRequest,
  getSampleRequest,
  initiatorDecisionSampleRequest,
  projectReviewSampleRequest,
  purchaseReviewSampleRequest,
} from '#/api/mes/srm/sample-req';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmSampleRequestInlineDetail from './inline-detail.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSampleRequestDetailModal' });

type WorkflowAction = 'archive' | 'final' | 'initiator' | 'project' | 'purchase';
type UserPickTarget = 'final' | 'workflowPurchase';

interface ModalData {
  id?: number | string;
  processInstanceId?: number | string;
  tabType?: string;
  taskId?: number | string;
}

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const SRM_USER_SELECT_MODAL_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 200;

const emit = defineEmits<{ success: [] }>();

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'processing', text: '待归档确认' },
  ARCHIVED: { color: 'success', text: '已归档' },
  DRAFT: { color: 'default', text: '草稿' },
  FINAL_APPROVAL: { color: 'warning', text: '最终批准人审核' },
  INITIATOR_CONFIRM: { color: 'processing', text: '发起人确认' },
  PROJECT_REVIEW: { color: 'processing', text: '项目负责人审核' },
  PURCHASE_REVIEW: { color: 'processing', text: '采购负责人审核' },
};

const loading = ref(false);
const saving = ref(false);
const record = ref<SrmSampleRequestApi.SampleRequest>();
const modalData = ref<ModalData>({});
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('project');
const workflowOpinion = ref('');
const workflowDirectArchive = ref(true);
const workflowPurchaseDifficulty = ref('');
const workflowPurchaseReviewerUserId = ref<number>();
const workflowPurchaseReviewerUserName = ref('');
const activeUserPickTarget = ref<UserPickTarget>('final');
const detailRefreshKey = ref(0);

const statusMeta = computed(
  () =>
    statusMetaMap[record.value?.status || ''] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);

const subtitleItems = computed(() =>
  [record.value?.requestNo || '', record.value?.currentNodeName || '']
    .map((item) => item.trim())
    .filter(Boolean),
);

const actionTitle = computed(() => {
  if (workflowAction.value === 'project') return '项目负责人审核';
  if (workflowAction.value === 'purchase') return '采购负责人审核';
  if (workflowAction.value === 'final') return '最终批准人审核';
  if (workflowAction.value === 'archive') return '归档确认';
  return '发起人确认流转';
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
      record.value = undefined;
      return;
    }
    modalData.value = modalApi.getData<ModalData>() || {};
    await loadDetail();
  },
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});

async function loadDetail() {
  const id = normalizeId(modalData.value.id);
  if (!id) {
    message.warning('缺少样品需求单 ID');
    return;
  }
  loading.value = true;
  try {
    record.value = await getSampleRequest(id);
    if (modalData.value.processInstanceId && !record.value.processInstanceId) {
      record.value.processInstanceId = String(modalData.value.processInstanceId);
    }
    detailRefreshKey.value += 1;
  } finally {
    loading.value = false;
  }
}

function closeDetail() {
  void modalApi.close();
}

function openWorkflowAction(action: WorkflowAction) {
  workflowAction.value = action;
  workflowOpinion.value = '';
  workflowDirectArchive.value = action === 'initiator';
  workflowPurchaseDifficulty.value = record.value?.purchaseDifficulty || '';
  workflowPurchaseReviewerUserId.value = record.value?.purchaseOwnerUserId;
  workflowPurchaseReviewerUserName.value = record.value?.purchaseOwnerUserId
    ? record.value?.purchaseOwnerUserName || ''
    : '';
  workflowModalOpen.value = true;
}

function openFinalApproverPicker() {
  openUserPicker('final');
}

function openWorkflowPurchaseReviewerPicker() {
  openUserPicker('workflowPurchase');
}

function openUserPicker(target: UserPickTarget) {
  activeUserPickTarget.value = target;
  const currentId =
    target === 'workflowPurchase'
      ? workflowPurchaseReviewerUserId.value
      : record.value?.finalApproverUserId;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_USER_SELECT_MODAL_Z_INDEX,
      multiple: false,
      title:
        target === 'workflowPurchase'
          ? '选择采购审核办理人'
          : '选择最终批准人',
      userIds: currentId ? [currentId] : [],
    })
    .open();
}

function handleUserSelect(users: SystemUserApi.User[]) {
  const user = users[0];
  if (!user?.id) {
    return;
  }
  const name = firstText(user.nickname, user.username, user.name) || '';
  if (activeUserPickTarget.value === 'workflowPurchase') {
    workflowPurchaseReviewerUserId.value = Number(user.id);
    workflowPurchaseReviewerUserName.value = name;
    return;
  }
  if (record.value) {
    record.value.finalApproverUserId = Number(user.id);
    record.value.finalApproverUserName = name;
  }
}

function confirmWorkflowAction() {
  if (!record.value?.id) {
    message.warning('缺少样品需求单 ID');
    return;
  }
  if (
    workflowAction.value === 'initiator' &&
    !workflowDirectArchive.value &&
    !record.value.finalApproverUserId
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
  AntModal.confirm({
    content: `确认提交${actionTitle.value}办理结果吗？`,
    okText: '确认提交',
    title: '办理确认',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
    async onOk() {
      await submitWorkflowAction();
    },
  });
}

async function submitWorkflowAction() {
  if (!record.value?.id) return;
  saving.value = true;
  try {
    const id = Number(record.value.id);
    const opinion = workflowOpinion.value;
    if (workflowAction.value === 'project') {
      await projectReviewSampleRequest({
        id,
        nextPurchaseOwnerUserId: workflowPurchaseReviewerUserId.value,
        nextPurchaseOwnerUserName: workflowPurchaseReviewerUserName.value,
        opinion,
      });
    } else if (workflowAction.value === 'purchase') {
      await purchaseReviewSampleRequest({
        id,
        opinion,
        purchaseDifficulty: workflowPurchaseDifficulty.value,
      });
    } else if (workflowAction.value === 'final') {
      await finalApproveSampleRequest({ id, opinion });
    } else if (workflowAction.value === 'archive') {
      await archiveConfirmSampleRequest({ id, opinion });
    } else {
      await initiatorDecisionSampleRequest({
        directArchive: workflowDirectArchive.value,
        finalApproverUserId: workflowDirectArchive.value
          ? undefined
          : record.value.finalApproverUserId,
        finalApproverUserName: workflowDirectArchive.value
          ? undefined
          : record.value.finalApproverUserName,
        id,
        opinion,
      });
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    emit('success');
    await modalApi.close();
  } finally {
    saving.value = false;
  }
}

function openAuditLog() {
  if (!record.value?.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      processInstanceId: record.value.processInstanceId,
      title: `审批日志 - ${record.value.requestNo || '样品需求单'}`,
    })
    .open();
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

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}
</script>

<template>
  <Modal>
    <UserModal @confirm="handleUserSelect" />
    <ProcessAuditModal />
    <Spin :spinning="loading || saving" class="detail-spin">
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
              v-if="record?.canProjectReview"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction('project')"
            >
              办理
            </Button>
            <Button
              v-if="record?.canPurchaseReview"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction('purchase')"
            >
              办理
            </Button>
            <Button
              v-if="record?.canInitiatorDecision"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction('initiator')"
            >
              确认流转
            </Button>
            <Button
              v-if="record?.canFinalApprove"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction('final')"
            >
              办理
            </Button>
            <Button
              v-if="record?.canArchiveConfirm"
              class="qms-ncr-toolbar-action"
              type="primary"
              @click="openWorkflowAction('archive')"
            >
              归档确认
            </Button>
            <Button class="qms-ncr-toolbar-action" @click="openAuditLog">
              <IconifyIcon icon="mdi:clipboard-check-outline" />
              审批日志
            </Button>
            <Button class="qms-ncr-toolbar-action" @click="closeDetail">
              关闭
            </Button>
          </div>
        </div>

        <div class="detail-content">
          <div v-if="record" class="sample-req-modal-status">
            <Space :size="8">
              <Tag :color="statusMeta.color">{{ statusMeta.text }}</Tag>
              <span>{{ displayValue(record.materialName) }}</span>
              <span>{{ displayValue(record.materialModel) }}</span>
            </Space>
          </div>
          <SrmSampleRequestInlineDetail
            v-if="record?.id"
            :key="detailRefreshKey"
            :id="record.id"
          />
        </div>
      </div>
    </Spin>

    <AntModal
      v-model:open="workflowModalOpen"
      :confirm-loading="saving"
      :mask-closable="false"
      :title="actionTitle"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="confirmWorkflowAction"
    >
      <div class="sample-req-workflow-modal">
        <div v-if="workflowAction === 'initiator'">
          <Radio.Group v-model:value="workflowDirectArchive">
            <Radio :value="true">直接归档</Radio>
            <Radio :value="false">发送最终批准人</Radio>
          </Radio.Group>
          <div v-if="!workflowDirectArchive" class="sample-req-workflow-user">
            <Input
              :value="record?.finalApproverUserName"
              placeholder="请选择最终批准人"
              readonly
            />
            <Button @click="openFinalApproverPicker">选择人员</Button>
          </div>
        </div>
        <div
          v-else-if="workflowAction === 'project'"
          class="sample-req-workflow-field"
        >
          <span>采购审核办理人</span>
          <div class="sample-req-workflow-user">
            <Input
              :value="workflowPurchaseReviewerUserName"
              placeholder="请选择采购审核办理人"
              readonly
            />
            <Button @click="openWorkflowPurchaseReviewerPicker">选择人员</Button>
          </div>
        </div>
        <label
          v-else-if="workflowAction === 'purchase'"
          class="sample-req-workflow-field"
        >
          <span>采购开发难点</span>
          <Input.TextArea
            v-model:value="workflowPurchaseDifficulty"
            :maxlength="2000"
            placeholder="请填写采购开发难点"
            :rows="5"
            show-count
          />
        </label>
        <Input.TextArea
          v-model:value="workflowOpinion"
          :rows="4"
          placeholder="请输入办理意见（可选）"
        />
      </div>
    </AntModal>
  </Modal>
</template>

<style scoped>
.sample-req-modal-status {
  padding: 12px 20px 0;
}

.sample-req-workflow-modal {
  display: grid;
  gap: 12px;
}

.sample-req-workflow-user {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 12px;
}

.sample-req-workflow-field {
  display: grid;
  gap: 8px;
}
</style>
