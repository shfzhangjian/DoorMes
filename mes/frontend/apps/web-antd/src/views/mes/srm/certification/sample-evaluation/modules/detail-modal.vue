<script lang="ts" setup>
import type { SrmSampleEvaluationApi } from '#/api/mes/srm/sample-evaluation';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Dropdown,
  Input,
  Menu,
  message,
  Radio,
  Spin,
} from 'ant-design-vue';

import {
  archiveConfirmSampleEvaluation,
  finalApproveSampleEvaluation,
  getSampleEvaluation,
  initiatorDecisionSampleEvaluation,
  inspectionReportSampleEvaluation,
  issueTrialValidation,
  signSampleEvaluation,
  valueConfirmSampleEvaluation,
  withdrawConfirmSampleEvaluation,
} from '#/api/mes/srm/sample-evaluation';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';

import SrmAttachmentPanel from '../../../shared/SrmAttachmentPanel.vue';
import SrmSampleEvaluationInlineDetail from './inline-detail.vue';

import '../../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSampleEvaluationDetailModal' });

const emit = defineEmits<{ success: [] }>();
type WorkflowAction =
  | 'archive'
  | 'confirm'
  | 'final'
  | 'initiator'
  | 'report'
  | 'sign'
  | 'withdraw';
type DetailAction = 'issueTrial' | WorkflowAction;

interface ModalData {
  id?: number | string;
  processInstanceId?: number | string;
  taskId?: number | string;
}

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_CONFIRM_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 200;
const SRM_USER_SELECT_MODAL_Z_INDEX = SRM_CONFIRM_MODAL_Z_INDEX + 200;
const BIZ_TYPE = 'SRM_SAMPLE_EVALUATION';
const SIGN_BIZ_TYPE = 'SRM_SAMPLE_EVALUATION_SIGN';

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

const loading = ref(false);
const saving = ref(false);
const record = ref<SrmSampleEvaluationApi.SampleEvaluation>();
const modalData = ref<ModalData>({});
const detailRefreshKey = ref(0);
const workflowModalOpen = ref(false);
const workflowAction = ref<WorkflowAction>('report');
const workflowOpinion = ref('');
const workflowPassed = ref(true);
const workflowSignResult = ref('PASS');
const inlineDetailRef =
  ref<InstanceType<typeof SrmSampleEvaluationInlineDetail>>();
const signAttachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();

const statusMeta = computed(
  () =>
    statusMetaMap[record.value?.status || ''] || {
      color: 'default',
      text: displayValue(record.value?.status),
    },
);
const subtitleItems = computed(() =>
  [record.value?.evaluationNo || '', statusMeta.value.text].filter(Boolean),
);
const availableActions = computed(() => {
  const actions: Array<{ action: DetailAction; label: string }> = [];
  if (record.value?.canInspectionReport)
    actions.push({ action: 'report', label: '执行检测' });
  if (record.value?.canValueConfirm)
    actions.push({ action: 'confirm', label: '检测结果确认' });
  if (record.value?.canSign) actions.push({ action: 'sign', label: '会签' });
  if (record.value?.canWithdrawConfirm)
    actions.push({ action: 'withdraw', label: '会签前撤回' });
  if (record.value?.canInitiatorDecision)
    actions.push({ action: 'initiator', label: '提交最终审批' });
  if (record.value?.canFinalApprove)
    actions.push({ action: 'final', label: '最终批准' });
  if (record.value?.canArchiveConfirm)
    actions.push({ action: 'archive', label: '完成归档' });
  if (record.value?.canIssueTrialValidation)
    actions.push({ action: 'issueTrial', label: '下达试生产通知' });
  return actions;
});
const currentSign = computed(() =>
  (record.value?.signs || []).find(
    (item) => item.id === record.value?.currentSignId,
  ),
);
const currentSignRequiresAttachment = computed(
  () =>
    Boolean(currentSign.value?.requireAttachment) ||
    String(currentSign.value?.deptName || '').includes('生产'),
);
const actionTitle = computed(
  () =>
    availableActions.value.find((item) => item.action === workflowAction.value)
      ?.label || '办理动作',
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
    message.warning('缺少样品评价表 ID');
    return;
  }
  loading.value = true;
  try {
    record.value = await getSampleEvaluation(id);
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
    })
    .open();
}

function openWorkflow(action: WorkflowAction) {
  workflowAction.value = action;
  workflowOpinion.value = '';
  workflowPassed.value = true;
  workflowSignResult.value = 'PASS';
  workflowModalOpen.value = true;
}

function openOnlyAction() {
  const action = availableActions.value[0];
  if (action) {
    handleAction(action.action);
  }
}

function handleAction(action: DetailAction) {
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

function addReportItem() {
  if (!record.value) return;
  record.value.items = [
    ...(record.value.items || []),
    { itemStatus: 'PENDING', rowNo: (record.value.items || []).length + 1 },
  ];
}

function removeReportItem(index: number) {
  if (!record.value) return;
  record.value.items = (record.value.items || []).filter(
    (_, itemIndex) => itemIndex !== index,
  );
  record.value.items.forEach((item, itemIndex) => {
    item.rowNo = itemIndex + 1;
  });
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

async function submitInspectionReportInline() {
  if (!record.value?.id) return;
  saving.value = true;
  try {
    const submitted = await submitInspectionReportPayload();
    if (!submitted) {
      return;
    }
    message.success('办理成功');
    await loadDetail();
    emit('success');
  } finally {
    saving.value = false;
  }
}

async function submitInspectionReportPayload() {
  if (!record.value?.id) {
    return false;
  }
  const items = normalizeInspectionItems(
    inlineDetailRef.value?.getInspectionItems() || record.value.items || [],
  );
  if (!hasEffectiveInspectionItem(items)) {
    message.warning('请至少在检验项目表填写一项检验项目和测试数据');
    return false;
  }
  record.value.items = items;
  await inlineDetailRef.value?.syncAttachments({
    bizId: record.value.id,
    bizType: BIZ_TYPE,
  });
  await inspectionReportSampleEvaluation({
    id: record.value.id,
    items,
  });
  return true;
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

async function submitWorkflow() {
  if (!record.value?.id) return;
  saving.value = true;
  try {
    switch (workflowAction.value) {
      case 'confirm': {
        await valueConfirmSampleEvaluation({
          id: record.value.id,
          opinion: workflowOpinion.value,
          passed: workflowPassed.value,
          signUsers: [],
        });

        break;
      }
      case 'final': {
        await finalApproveSampleEvaluation({
          id: record.value.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      case 'initiator': {
        await initiatorDecisionSampleEvaluation({
          directArchive: false,
          id: record.value.id,
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
          id: record.value.id,
          opinion: workflowOpinion.value,
          result: workflowSignResult.value,
          signId: record.value.currentSignId,
        });

        break;
      }
      case 'withdraw': {
        await withdrawConfirmSampleEvaluation({
          id: record.value.id,
          opinion: workflowOpinion.value,
        });

        break;
      }
      default: {
        await archiveConfirmSampleEvaluation({
          id: record.value.id,
          opinion: workflowOpinion.value,
        });
      }
    }
    workflowModalOpen.value = false;
    message.success('办理成功');
    await loadDetail();
    emit('success');
  } finally {
    saving.value = false;
  }
}

function confirmIssueTrialValidation() {
  if (!record.value?.id) {
    return;
  }
  AntModal.confirm({
    content: `确认从样品评价单 ${record.value.evaluationNo || ''} 下达试生产通知并生成试产验证跟踪单？`,
    okText: '确认下达',
    async onOk() {
      const trialId = await issueTrialValidation(record.value!.id!);
      message.success(`试产验证跟踪单已生成：${trialId}`);
      await loadDetail();
      emit('success');
    },
    title: '下达试生产通知',
    zIndex: SRM_CONFIRM_MODAL_Z_INDEX,
  });
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function firstText(...values: unknown[]) {
  return values.map((item) => String(item || '').trim()).find(Boolean) || '';
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
            <div class="qms-ncr-title-panel__name">样品评价表</div>
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
                    @click="handleAction(item.action)"
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
            <div class="qms-exception-form sample-eval-mini-form">
              <SrmSampleEvaluationInlineDetail
                ref="inlineDetailRef"
                :key="detailRefreshKey"
                :id="record?.id"
                :attachment-modal-z-index="SRM_USER_SELECT_MODAL_Z_INDEX"
                :editable-attachment="Boolean(record?.canInspectionReport)"
                :editable-inspection="Boolean(record?.canInspectionReport)"
              />
            </div>
          </div>
        </div>
      </div>
    </Spin>

    <AntModal
      v-model:open="workflowModalOpen"
      :confirm-loading="saving"
      ok-text="确认"
      :title="actionTitle"
      :z-index="SRM_CONFIRM_MODAL_Z_INDEX"
      width="760px"
      @ok="submitWorkflow"
    >
      <div class="sample-eval-action-form">
        <template v-if="workflowAction === 'report'">
          <p class="text-xs text-slate-500">
            提交前请维护检验项目和值；提交后自动发送给发起人做检测结果确认。
          </p>
          <div class="flex items-center justify-between">
            <strong>检验项目</strong>
            <Button size="small" type="primary" @click="addReportItem">
              新增项目
            </Button>
          </div>
          <div class="sample-eval-mini-table-wrap">
            <table class="sample-eval-mini-table">
              <thead>
                <tr>
                  <th style="width: 54px">序号</th>
                  <th style="width: 170px">检验项目</th>
                  <th style="width: 520px">技术要求</th>
                  <th v-for="index in 5" :key="index" style="width: 95px">
                    {{ index }}
                  </th>
                  <th style="width: 105px">判定</th>
                  <th style="width: 64px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(item, index) in record?.items || []"
                  :key="item.id || index"
                >
                  <td class="text-center">{{ index + 1 }}</td>
                  <td>
                    <Input
                      v-model:value="item.itemName"
                      placeholder="检验项目"
                    />
                  </td>
                  <td>
                    <Input.TextArea
                      v-model:value="item.technicalRequirement"
                      :auto-size="{ minRows: 1, maxRows: 3 }"
                    />
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
                      :value="itemFieldValue(item, field)"
                      @update:value="
                        (value) => setItemFieldValue(item, field, value)
                      "
                    />
                  </td>
                  <td>
                    <Radio.Group
                      v-model:value="item.itemJudgement"
                      :options="judgementOptions"
                      option-type="button"
                      size="small"
                    />
                  </td>
                  <td class="text-center">
                    <Button
                      danger
                      type="link"
                      class="!px-0"
                      @click="removeReportItem(index)"
                    >
                      删除
                    </Button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
                v-for="item in record?.projectUsers || []"
                :key="item.id || `${item.deptName}-${item.userId}`"
                class="sample-eval-sign-pick-row"
              >
                <span class="font-bold">{{ item.deptName }}</span>
                <span class="min-w-0 flex-1 truncate text-slate-600">
                  {{ item.userName }}
                </span>
              </div>
              <div
                v-if="!record?.projectUsers || record.projectUsers.length === 0"
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
          <SrmAttachmentPanel
            v-if="currentSignRequiresAttachment && currentSign?.id"
            ref="signAttachmentPanelRef"
            :biz-id="currentSign.id"
            :biz-type="SIGN_BIZ_TYPE"
            :category-options="attachmentCategoryOptions"
            default-category="INSPECTION_PHOTO"
            mode="edit"
            :modal-z-index="SRM_USER_SELECT_MODAL_Z_INDEX"
            title="生产部确认附件"
          />
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
  </Modal>
</template>

<style scoped>
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

.sample-eval-mini-table-wrap {
  box-sizing: border-box;
  height: 420px;
  min-height: 420px;
  overflow-x: scroll;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  scrollbar-gutter: stable both-edges;
}

.sample-eval-mini-table {
  width: 100%;
  min-width: 1420px;
  border-collapse: collapse;
  table-layout: fixed;
}

.sample-eval-mini-table th,
.sample-eval-mini-table td {
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 6px;
  font-size: 12px;
  vertical-align: middle;
}

.sample-eval-mini-table th {
  background: #f8fafc;
  color: #334155;
  font-weight: 800;
  white-space: nowrap;
}

.sample-eval-mini-table td :deep(.ant-input),
.sample-eval-mini-table td :deep(.ant-select) {
  width: 100%;
}

.sample-eval-mini-table td :deep(.ant-radio-group) {
  display: flex;
  width: 100%;
  flex-wrap: nowrap;
}

.sample-eval-mini-table td :deep(.ant-radio-button-wrapper) {
  flex: 1 1 0;
  padding-inline: 6px;
  text-align: center;
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
</style>
