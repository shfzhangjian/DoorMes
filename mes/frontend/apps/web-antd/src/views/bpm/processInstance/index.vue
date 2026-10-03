<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';

import { h, reactive, ref } from 'vue';

import { DocAlert, Page, prompt, useVbenModal } from '@vben/common-ui';
import {
  BpmModelFormType,
  BpmProcessInstanceStatus,
  DICT_TYPE,
} from '@vben/constants';

import {
  Checkbox,
  Form,
  message,
  Modal as AntModal,
  Select,
  Textarea,
  Button,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProcessDefinition } from '#/api/bpm/definition';
import {
  adjustProcessInstanceStep,
  cancelProcessInstanceByStartUser,
  getAdjustableProcessInstanceNodes,
  getProcessInstance,
  getProcessInstanceMyPage,
} from '#/api/bpm/processInstance';
import { relaunchNcrRecord } from '#/api/mes/quality/abnormal/ncr';
import { DictTag } from '#/components/dict-tag';
import { $t } from '#/locales';
import { router } from '#/router';
import NcrDetailModal from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';

import { useGridColumns, useGridFormSchema } from './data';
import BusinessFormModal from './detail/modules/business-form-modal.vue';

defineOptions({ name: 'BpmProcessInstanceMy' });

const NCR_PROCESS_KEYS = [
  'qms_ncr_disposition',
  'qms_raw_material_ncr_disposition',
];
const NCR_FORM_PATHS = [
  '/mes/quality/abnormal/ncr',
  '/mes/quality/abnormal/raw-material-ncr',
];

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 查看业务单据详情 */
async function handleBusinessFormDetail(
  row: BpmProcessInstanceApi.ProcessInstance,
) {
  const detail = await resolveBusinessDetailInstance(row);
  if (tryOpenNcrDetail(detail)) {
    return;
  }
  businessFormModalApi
    .setData({
      businessKey: detail.businessKey,
      formVariables: detail.formVariables,
      processDefinition: detail.processDefinition,
      processInstanceId: detail.id,
      processInstance: detail,
      title: `单据详情 - ${detail.name || detail.id}`,
    })
    .open();
}

async function resolveBusinessDetailInstance(
  row: BpmProcessInstanceApi.ProcessInstance,
) {
  if (!isNcrProcess(row) || resolveBusinessKey(row)) {
    return row;
  }
  return await getProcessInstance(row.id);
}

function tryOpenNcrDetail(
  row: BpmProcessInstanceApi.ProcessInstance,
  tabType?: string,
) {
  const businessKey = resolveBusinessKey(row);
  if (!isNcrProcess(row) || !businessKey) {
    return false;
  }
  ncrDetailModalApi.setData({ id: Number(businessKey), tabType }).open();
  return true;
}

function isNcrProcess(row?: BpmProcessInstanceApi.ProcessInstance) {
  const processKey = row?.processDefinition?.key || '';
  const processDefinitionId = row?.processDefinitionId || '';
  const formPath = row?.processDefinition?.formCustomViewPath || '';
  return (
    NCR_PROCESS_KEYS.includes(processKey) ||
    NCR_PROCESS_KEYS.some((key) => processDefinitionId.startsWith(`${key}:`)) ||
    NCR_FORM_PATHS.includes(formPath)
  );
}

function resolveBusinessKey(row?: BpmProcessInstanceApi.ProcessInstance) {
  return (
    row?.businessKey ||
    row?.formVariables?.ncRecordId ||
    row?.formVariables?.businessKey ||
    row?.formVariables?.id
  );
}

/** 重新发起流程 */
async function handleCreate(row?: BpmProcessInstanceApi.ProcessInstance) {
  if (row?.id) {
    const businessKey = resolveBusinessKey(row);
    if (isNcrProcess(row) && businessKey) {
      const confirmed = await confirmByModal(
        '重新发起 NCR',
        '旧 NCR 将保持只读，系统会基于旧单生成一张新的草稿并记录旧单来源，确认继续？',
      );
      if (!confirmed) {
        return;
      }
      const newId = await relaunchNcrRecord(Number(businessKey));
      message.success('已基于旧 NCR 生成新草稿');
      ncrDetailModalApi.setData({ id: newId }).open();
      handleRefresh();
      return;
    }
    const processDefinitionDetail = await getProcessDefinition(
      row.processDefinitionId,
    );
    if (processDefinitionDetail?.formType === BpmModelFormType.CUSTOM) {
      if (!processDefinitionDetail.formCustomCreatePath) {
        message.error('未配置业务表单的提交路由，无法重新发起');
        return;
      }
      await router.push({
        path: processDefinitionDetail.formCustomCreatePath,
        query: {
          id: businessKey,
        },
      });
      return;
    } else if (processDefinitionDetail?.formType === BpmModelFormType.NORMAL) {
      await router.push({
        name: 'BpmProcessInstanceCreate',
        query: { processInstanceId: row.id },
      });
      return;
    }
  }
  await router.push({
    name: 'BpmProcessInstanceCreate',
    query: row?.id ? { processInstanceId: row.id } : {},
  });
}

const adjustModalOpen = ref(false);
const adjustLoading = ref(false);
const adjustSubmitting = ref(false);
const adjustTargetRow = ref<BpmProcessInstanceApi.ProcessInstance>();
const adjustNodes = ref<BpmProcessInstanceApi.AdjustableNode[]>([]);
const adjustForm = reactive({
  clearBusinessData: false,
  reason: '',
  targetTaskDefinitionKey: undefined as string | undefined,
});

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

async function handleOpenAdjust(row: BpmProcessInstanceApi.ProcessInstance) {
  adjustTargetRow.value = row;
  adjustForm.targetTaskDefinitionKey = undefined;
  adjustForm.reason = '';
  adjustForm.clearBusinessData = false;
  adjustNodes.value = [];
  adjustModalOpen.value = true;
  adjustLoading.value = true;
  try {
    adjustNodes.value = await getAdjustableProcessInstanceNodes(row.id);
    if (adjustNodes.value.length === 0) {
      message.warning('当前流程暂无可调整的已办理步骤');
    }
  } finally {
    adjustLoading.value = false;
  }
}

async function handleSubmitAdjust() {
  if (!adjustTargetRow.value?.id) {
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
    '调整步骤',
    adjustForm.clearBusinessData
      ? '确认将流程调整到所选步骤，并标记清空后续已填业务内容？'
      : '确认将流程调整到所选步骤，并保留已填业务内容？',
  );
  if (!confirmed) {
    return;
  }
  adjustSubmitting.value = true;
  try {
    await adjustProcessInstanceStep({
      id: adjustTargetRow.value.id,
      targetTaskDefinitionKey: adjustForm.targetTaskDefinitionKey,
      reason: adjustForm.reason.trim(),
      clearBusinessData: adjustForm.clearBusinessData,
    });
    message.success('流程步骤已调整');
    adjustModalOpen.value = false;
    handleRefresh();
  } finally {
    adjustSubmitting.value = false;
  }
}

/** 取消流程实例 */
function handleCancel(row: BpmProcessInstanceApi.ProcessInstance) {
  prompt({
    component: () => {
      return h(Textarea, {
        placeholder: '请输入取消原因',
        allowClear: true,
        rows: 2,
      });
    },
    content: '请输入取消原因',
    title: '取消流程',
    modelPropName: 'value',
  }).then(async (reason) => {
    if (reason) {
      await cancelProcessInstanceByStartUser(row.id, reason);
      message.success('取消成功');
      handleRefresh();
    }
  });
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
          return await getProcessInstanceMyPage({
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
  } as VxeTableGridOptions<BpmProcessInstanceApi.ProcessInstance>,
});

const [BusinessFormDetailModal, businessFormModalApi] = useVbenModal({
  connectedComponent: BusinessFormModal,
  destroyOnClose: true,
});

const [NcrDetailModalHost, ncrDetailModalApi] = useVbenModal({
  connectedComponent: NcrDetailModal,
  destroyOnClose: true,
});
</script>

<template>
  <Page auto-content-height>
    <BusinessFormDetailModal />
    <NcrDetailModalHost @success="gridApi.query()" />
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
            :options="
              adjustNodes.map((item) => ({
                label: `${item.taskName}${item.assigneeUserName ? ` / ${item.assigneeUserName}` : ''}`,
                value: item.taskDefinitionKey,
              }))
            "
            placeholder="请选择已办理步骤"
          />
        </Form.Item>
        <Form.Item label="调整原因" required>
          <Textarea
            v-model:value="adjustForm.reason"
            :rows="3"
            allow-clear
            placeholder="请填写调整原因，系统将保留在流程历史中"
          />
        </Form.Item>
        <Form.Item>
          <Checkbox v-model:checked="adjustForm.clearBusinessData">
            清空目标步骤之后已填写的业务内容
          </Checkbox>
        </Form.Item>
      </Form>
    </AntModal>
    <template #doc-disabled>
      <DocAlert
        title="流程发起、取消、重新发起"
        url="https://doc.iocoder.cn/bpm/process-instance"
      />
    </template>

    <Grid table-title="流程状态">
      <template #slot-summary="{ row }">
        <div
          class="flex flex-col py-2"
          v-if="row.summary && row.summary.length > 0"
        >
          <div v-for="(item, index) in row.summary" :key="index">
            <span class="text-gray-500">
              {{ item.key }} : {{ item.value }}
            </span>
          </div>
        </div>
        <div v-else>-</div>
      </template>
      <template #slot-status="{ row }">
        <template
          v-if="
            row.status === BpmProcessInstanceStatus.RUNNING &&
            row.tasks?.length! > 0
          "
        >
              <!-- 单人审批 -->
          <template v-if="row.tasks!.length === 1">
            <span>
              <Button type="link" @click="handleBusinessFormDetail(row)">
                {{ row.tasks![0]!.assigneeUser?.nickname }}
              </Button>
              ({{ row.tasks![0]!.name }}) 审批中
            </span>
          </template>
          <!-- 多人审批 -->
          <template v-else>
            <span>
              <Button type="link" @click="handleBusinessFormDetail(row)">
                {{ row.tasks![0]!.assigneeUser?.nickname }}
              </Button>
              等 {{ row.tasks!.length }} 人 ({{ row.tasks![0]!.name }})审批中
            </span>
          </template>
        </template>
        <!-- 非审批中状态 -->
        <template v-else>
          <DictTag
            :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS"
            :value="row.status"
          />
        </template>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.detail'),
              type: 'link',
              icon: ACTION_ICON.VIEW,
              auth: ['bpm:process-instance:query'],
              onClick: handleBusinessFormDetail.bind(null, row),
            },
            {
              label: $t('ui.actionTitle.cancel'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              ifShow: row.status === BpmProcessInstanceStatus.RUNNING,
              auth: ['bpm:process-instance:cancel'],
              onClick: handleCancel.bind(null, row),
            },
            {
              label: '调整步骤',
              type: 'link',
              ifShow: row.status === BpmProcessInstanceStatus.RUNNING,
              auth: ['bpm:process-instance:query'],
              onClick: handleOpenAdjust.bind(null, row),
            },
            {
              label: '重新发起',
              type: 'link',
              icon: ACTION_ICON.ADD,
              ifShow: row.status !== BpmProcessInstanceStatus.RUNNING,
              auth: ['bpm:process-instance:create'],
              onClick: handleCreate.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
