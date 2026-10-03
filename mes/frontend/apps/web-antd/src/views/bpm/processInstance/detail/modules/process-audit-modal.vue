<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { BpmModelType, BpmNodeTypeEnum } from '@vben/constants';

import { Alert, Empty, Spin, TabPane, Tabs } from 'ant-design-vue';

import {
  getApprovalDetail,
  getProcessInstanceBpmnModelView,
} from '#/api/bpm/processInstance';

import ProcessInstanceBpmnViewer from './bpm-viewer.vue';
import ProcessInstanceSimpleViewer from './simple-bpm-viewer.vue';
import ProcessInstanceTimeline from './time-line.vue';

defineOptions({ name: 'BpmProcessAuditModal' });

interface ProcessAuditModalData {
  activityId?: string;
  extraActivityNodes?: BpmProcessInstanceApi.ApprovalNodeInfo[];
  processInstanceId?: number | string;
  taskId?: number | string;
  title?: string;
}

const loading = ref(false);
const errorMessage = ref('');
const modelViewLoading = ref(false);
const modelViewErrorMessage = ref('');
const activeTab = ref('timeline');
const showIdentifierMeta = ref(false);
const processInstance = ref<BpmProcessInstanceApi.ProcessInstance>();
const processDefinition = ref<Record<string, any>>({});
const processModelView = ref<Record<string, any>>({});
const activityNodes = ref<BpmProcessInstanceApi.ApprovalNodeInfo[]>([]);
const extraActivityNodes = ref<BpmProcessInstanceApi.ApprovalNodeInfo[]>([]);
const mergedActivityNodes = computed(() => {
  if (extraActivityNodes.value.length === 0) {
    return activityNodes.value;
  }
  const endIndex = activityNodes.value.findIndex(
    (node) => node.nodeType === BpmNodeTypeEnum.END_EVENT_NODE,
  );
  if (endIndex < 0) {
    return [...activityNodes.value, ...extraActivityNodes.value];
  }
  return [
    ...activityNodes.value.slice(0, endIndex),
    ...extraActivityNodes.value,
    ...activityNodes.value.slice(endIndex),
  ];
});

const [Modal, modalApi] = useVbenModal({
  class: 'bpm-process-audit-modal',
  footer: false,
  fullscreenButton: true,
  title: '审批日志',
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetState();
      return;
    }
    await loadAuditDetail();
  },
});

function resetState() {
  loading.value = false;
  errorMessage.value = '';
  modelViewLoading.value = false;
  modelViewErrorMessage.value = '';
  activeTab.value = 'timeline';
  showIdentifierMeta.value = false;
  processInstance.value = undefined;
  processDefinition.value = {};
  processModelView.value = {};
  activityNodes.value = [];
  extraActivityNodes.value = [];
}

function toggleIdentifierMeta() {
  showIdentifierMeta.value = !showIdentifierMeta.value;
}

async function loadAuditDetail() {
  const data = modalApi.getData<ProcessAuditModalData>();
  extraActivityNodes.value = data?.extraActivityNodes || [];
  if (!data?.processInstanceId) {
    errorMessage.value = '当前单据未发起审批流程，暂无审批日志';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  modalApi.lock();
  try {
    const detail = await getApprovalDetail({
      activityId: data.activityId,
      processInstanceId: data.processInstanceId,
      taskId: data.taskId,
    });
    if (!detail?.processDefinition || !detail?.processInstance) {
      errorMessage.value = '查询不到审批流程详情';
      return;
    }

    processInstance.value = detail.processInstance;
    processDefinition.value = detail.processDefinition as Record<string, any>;
    activityNodes.value = detail.activityNodes || [];
    modalApi.setState({
      title:
        data.title ||
        `审批日志 - ${detail.processInstance.name || detail.processInstance.businessKey || detail.processInstance.id}`,
    });
  } catch {
    errorMessage.value = '获取审批日志失败';
  } finally {
    modalApi.unlock();
    loading.value = false;
  }
}

async function loadProcessModelView() {
  const data = modalApi.getData<ProcessAuditModalData>();
  if (
    !data?.processInstanceId ||
    modelViewLoading.value ||
    Object.keys(processModelView.value).length > 0
  ) {
    return;
  }
  modelViewLoading.value = true;
  modelViewErrorMessage.value = '';
  try {
    const modelView = await getProcessInstanceBpmnModelView(
      String(data.processInstanceId),
    );
    processModelView.value = (modelView || {}) as Record<string, any>;
  } catch {
    modelViewErrorMessage.value = '获取流程图失败，不影响审批日志查看';
  } finally {
    modelViewLoading.value = false;
  }
}

watch(activeTab, (tab) => {
  if (tab === 'diagram' && processInstance.value) {
    void loadProcessModelView();
  }
});
</script>

<template>
  <Modal>
    <Spin :spinning="loading">
      <div class="bpm-process-audit-modal__body">
        <Alert
          v-if="errorMessage"
          :message="errorMessage"
          show-icon
          type="warning"
        />
        <template v-else>
          <div class="bpm-process-audit-modal__meta">
            <span>流程：{{ processInstance?.name || '-' }}</span>
            <span v-if="showIdentifierMeta">
              流程编号：{{ processInstance?.id || '-' }}
            </span>
            <span v-if="showIdentifierMeta">
              单据编号：{{ processInstance?.businessKey || '-' }}
            </span>
          </div>
          <Tabs
            v-model:active-key="activeTab"
            class="bpm-process-audit-modal__tabs"
          >
            <TabPane key="timeline">
              <template #tab>
                <span
                  class="bpm-process-audit-modal__tab-label"
                  @dblclick.stop="toggleIdentifierMeta"
                >
                  审批日志
                </span>
              </template>
              <div class="bpm-process-audit-modal__timeline-panel">
                <ProcessInstanceTimeline
                  v-if="mergedActivityNodes.length > 0"
                  :activity-nodes="mergedActivityNodes"
                />
                <Empty v-else description="暂无审批日志" />
              </div>
            </TabPane>
            <TabPane key="diagram" tab="流程图" force-render>
              <div class="bpm-process-audit-modal__diagram-panel">
                <Alert
                  v-if="modelViewErrorMessage"
                  :message="modelViewErrorMessage"
                  show-icon
                  type="warning"
                />
                <ProcessInstanceSimpleViewer
                  v-show="
                    !modelViewErrorMessage &&
                    processDefinition.modelType === BpmModelType.SIMPLE
                  "
                  :loading="modelViewLoading"
                  :model-view="processModelView"
                />
                <ProcessInstanceBpmnViewer
                  v-show="
                    !modelViewErrorMessage &&
                    processDefinition.modelType === BpmModelType.BPMN
                  "
                  :loading="modelViewLoading"
                  :model-view="processModelView"
                />
                <Empty
                  v-if="
                    processDefinition.modelType !== BpmModelType.SIMPLE &&
                    processDefinition.modelType !== BpmModelType.BPMN
                  "
                  description="暂无流程图"
                />
              </div>
            </TabPane>
          </Tabs>
        </template>
      </div>
    </Spin>
  </Modal>
</template>

<style lang="scss" scoped>
.bpm-process-audit-modal__body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 420px;
  overflow: hidden;
  padding: 4px 2px 12px;
}

.bpm-process-audit-modal__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  padding: 0 0 12px;
  margin-bottom: 12px;
  color: hsl(var(--muted-foreground));
  border-bottom: 1px solid hsl(var(--border));
}

.bpm-process-audit-modal__tab-label {
  user-select: none;
}

.bpm-process-audit-modal__tabs {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.bpm-process-audit-modal__timeline-panel,
.bpm-process-audit-modal__diagram-panel {
  height: 100%;
  min-height: 0;
}

.bpm-process-audit-modal__timeline-panel {
  overflow: auto;
  padding: 10px 8px 0 14px;
}

.bpm-process-audit-modal__diagram-panel {
  overflow: hidden;
}

:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  height: 100%;
}

:deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
}

:deep(.ant-tabs-content),
:deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

:deep(.simple-process-model-container) {
  height: 100%;
  min-height: 100%;
}

:deep(.simple-process-model) {
  min-height: 100%;
}
</style>
