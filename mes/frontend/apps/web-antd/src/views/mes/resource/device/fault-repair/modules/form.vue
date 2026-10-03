<script lang="ts" setup>
import type { MesFaultRepairApi } from '#/api/mes/resource/device/fault-repair';
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';
import type { SystemUserApi } from '#/api/system/user';

import { computed, nextTick, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  DatePicker,
  Form,
  Input,
  message,
  Radio,
  Select,
  Spin,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createOrder,
  getOrder,
  processOrder,
  updateOrder,
} from '#/api/mes/resource/device/fault-repair';
import { getDevicePage } from '#/api/mes/resource/device/ledger';
import { getUserProfile } from '#/api/system/user/profile';
import { UserSelectModal } from '#/views/system/user/components';

import DeviceSelectModal from './device-select-modal.vue';
import PartList from './part-list.vue';

defineOptions({ name: 'FaultRepairFormModal' });

const emit = defineEmits(['success']);

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const statusTextMap: Record<string, string> = {
  CLOSED: '已关闭归档',
  DISPATCHED: '维修执行',
  PENDING_ARCHIVE: '关闭归档',
  PENDING_CONFIRM: '完成确认',
  REPORTED: '响应分派',
};
const levelOptions = [
  { label: '一般', value: 'MINOR' },
  { label: '重大', value: 'MAJOR' },
  { label: '紧急', value: 'CRITICAL' },
];

interface ToolbarActionItem {
  danger?: boolean;
  key: string;
  label: string;
  onClick: () => Promise<void> | void;
  title: string;
  type?: 'default' | 'primary';
}

const form = reactive<MesFaultRepairApi.Order>({});
const processForm = reactive<MesFaultRepairApi.ProcessReq>({
  action: 'RESPOND',
  confirmResult: 'CONFIRMED',
  id: '',
  repairStatus: 'DONE',
  responseResult: 'NEED_REPAIR',
});
const formType = ref<'create' | 'detail' | 'edit'>('create');
const loading = ref(false);
const partListRef = ref<InstanceType<typeof PartList>>();
const deviceSelectModalRef = ref<InstanceType<typeof DeviceSelectModal>>();

const [UserSelectModalComp, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
});

const isCreate = computed(() => formType.value === 'create');
const isClosed = computed(() => form.status === 'CLOSED');
const currentAction = computed<
  MesFaultRepairApi.ProcessReq['action'] | undefined
>(() => {
  if (isCreate.value || isClosed.value || form.canHandle === false) {
    return undefined;
  }
  if (form.status === 'REPORTED') return 'RESPOND';
  if (form.status === 'DISPATCHED') return 'REPAIR';
  if (form.status === 'PENDING_CONFIRM') return 'CONFIRM';
  if (form.status === 'PENDING_ARCHIVE') return 'ARCHIVE';
  return undefined;
});
const canFlow = computed(() => !!currentAction.value);
const partEditable = computed(() => currentAction.value === 'REPAIR');
const currentNodeText = computed(
  () => form.currentNodeName || getStatusText(form.status) || '待提报',
);
const orderTitle = computed(
  () => form.orderNo || form.exceptionNo || (isCreate.value ? '新建中' : '-'),
);
const primaryActionText = computed(() => {
  if (isCreate.value) return '提交';
  if (currentAction.value === 'RESPOND') return '响应分派';
  if (currentAction.value === 'REPAIR') return '提交维修';
  if (currentAction.value === 'CONFIRM') return '完成确认';
  if (currentAction.value === 'ARCHIVE') return '关闭归档';
  return '保存';
});
const toolbarActions = computed<ToolbarActionItem[]>(() => {
  const actions: ToolbarActionItem[] = [];
  if (isCreate.value || canFlow.value) {
    actions.push({
      key: 'primary',
      label: primaryActionText.value,
      onClick: handleSubmit,
      title: isCreate.value ? '提交设备异常报修单' : '完成当前节点办理',
      type: 'primary',
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

const [BaseModal, modalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  title: '',
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>() || {};
    await openWithData(data);
  },
});

async function openWithData(data: any) {
  resetState();
  formType.value =
    data?.isNew || data?.type === 'create' ? 'create' : data?.type || 'edit';
  await nextTick();
  if (data.id) {
    loading.value = true;
    try {
      Object.assign(form, await getOrder(data.id));
      initProcessForm();
    } finally {
      loading.value = false;
    }
    return;
  }
  Object.assign(form, {
    ...data,
    exceptionLevel: data.exceptionLevel || 'MAJOR',
    reportTime: dayjs().format(DATETIME_FORMAT),
    status: 'REPORTED',
  });
  await fillCurrentUser();
  initProcessForm();
}

function resetState() {
  Object.keys(form).forEach((key) => delete (form as any)[key]);
  Object.assign(processForm, {
    action: 'RESPOND',
    archiveReason: undefined,
    assignee: undefined,
    assigneeId: undefined,
    confirmRemark: undefined,
    confirmResult: 'CONFIRMED',
    id: '',
    partChangeDesc: undefined,
    parts: undefined,
    preventiveAction: undefined,
    repairAction: undefined,
    repairPlanRemark: undefined,
    repairPlanTime: undefined,
    repairStatus: 'DONE',
    repairTime: undefined,
    responseRemark: undefined,
    responseResult: 'NEED_REPAIR',
    rootCause: undefined,
    unfixReason: undefined,
  });
}

function initProcessForm() {
  processForm.id = form.id || '';
  processForm.action = currentAction.value || 'RESPOND';
  processForm.responseResult = 'NEED_REPAIR';
  processForm.repairStatus = 'DONE';
  processForm.confirmResult = 'CONFIRMED';
  if (currentAction.value === 'REPAIR') {
    processForm.repairTime = dayjs().format(DATETIME_FORMAT);
  }
}

async function fillCurrentUser() {
  try {
    const user = await getUserProfile();
    form.reporterId = user.id;
    form.reporter = user.nickname || user.username;
  } catch {
    form.reporter = form.reporter || '当前登录人';
  }
}

function openDeviceSelect() {
  deviceSelectModalRef.value?.open();
}

function handleDeviceSelected(row: MesDeviceLedgerApi.Device) {
  form.deviceId = row.id;
  form.deviceCode = row.deviceCode;
  form.deviceName = row.deviceName;
}

async function handleDeviceCodeBlur() {
  const deviceCode = form.deviceCode?.trim();
  if (!deviceCode || !isCreate.value) return;
  const res = await getDevicePage({
    deviceCode,
    pageNo: 1,
    pageSize: 10,
    status: 1,
  } as any);
  const matched = (res.list || []).find(
    (item) => item.deviceCode === deviceCode,
  );
  if (matched) {
    handleDeviceSelected(matched);
  }
}

function openAssigneeSelect() {
  userSelectModalApi
    .setData({
      multiple: false,
      userIds: processForm.assigneeId ? [processForm.assigneeId] : [],
    })
    .open();
}

function handleAssigneeConfirm(users: SystemUserApi.User[]) {
  const user = users[0];
  if (!user?.id) return;
  processForm.assigneeId = user.id;
  processForm.assignee = user.nickname || user.username || String(user.id);
}

function validateCreate() {
  if (!form.deviceCode?.trim()) return '请选择或输入设备编号';
  if (!form.deviceName?.trim()) return '设备名称不能为空';
  if (!form.exceptionLevel) return '请选择紧急程度';
  if (!form.faultDesc?.trim()) return '请填写异常现象';
  return '';
}

function validateProcess() {
  if (currentAction.value === 'RESPOND') {
    if (!processForm.responseResult) return '请选择办理选项';
    if (!processForm.responseRemark?.trim()) return '请填写处理说明';
    if (
      processForm.responseResult === 'NEED_REPAIR' &&
      !processForm.assignee?.trim()
    ) {
      return '请选择维修人';
    }
  }
  if (currentAction.value === 'REPAIR') {
    if (!processForm.repairTime) return '请选择维修实际时间';
    if (!processForm.repairAction?.trim()) return '请填写维修说明';
    if (
      processForm.repairStatus === 'LEFTOVER' &&
      !processForm.unfixReason?.trim()
    ) {
      return '请填写遗留问题说明';
    }
  }
  if (currentAction.value === 'CONFIRM' && !processForm.confirmRemark?.trim()) {
    return '请填写确认说明';
  }
  if (currentAction.value === 'ARCHIVE' && !processForm.rootCause?.trim()) {
    return '请填写原因分析';
  }
  return '';
}

function confirmAction(content: string) {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '取消',
      content,
      okText: '确认',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: '操作确认',
    });
  });
}

async function handleSubmit() {
  const error = isCreate.value ? validateCreate() : validateProcess();
  if (error) {
    message.warning(error);
    return;
  }
  const confirmed = await confirmAction(
    isCreate.value
      ? '确认提交设备异常报修单？'
      : `确认完成【${primaryActionText.value}】？`,
  );
  if (!confirmed) return;
  loading.value = true;
  try {
    if (isCreate.value) {
      await createOrder(form);
      message.success('设备异常已提报，进入响应分派');
    } else if (currentAction.value) {
      await processOrder({
        ...processForm,
        action: currentAction.value,
        id: form.id!,
        parts:
          currentAction.value === 'REPAIR'
            ? partListRef.value?.getData()
            : undefined,
      });
      message.success('当前节点已办理');
    } else {
      await updateOrder(form);
      message.success('保存成功');
    }
    emit('success');
    modalApi.close();
  } finally {
    loading.value = false;
  }
}

function getStatusText(status?: string) {
  return status ? statusTextMap[status] || status : '待提报';
}

function getStatusColor(status?: string) {
  if (status === 'CLOSED') return 'success';
  if (status === 'PENDING_ARCHIVE') return 'purple';
  if (status === 'PENDING_CONFIRM') return 'warning';
  if (status === 'DISPATCHED') return 'processing';
  return 'error';
}

function getLevelText(level?: string) {
  return (
    levelOptions.find((item) => item.value === level)?.label || level || '-'
  );
}

function readonlyValue(value?: number | string) {
  return value === undefined || value === null || value === '' ? '-' : value;
}
</script>

<template>
  <BaseModal>
    <div
      class="qms-ncr-detail qms-exception-detail"
      :class="{ 'is-loading': loading }"
    >
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <Tag :color="getStatusColor(form.status)" class="!m-0">
            {{ currentNodeText }}
          </Tag>
          <Tag v-if="canFlow" color="warning" class="!m-0">
            {{ primaryActionText }}
          </Tag>
        </div>
        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">设备异常事件单</div>
          <div class="qms-ncr-title-panel__subtitle">
            <div class="qms-ncr-title-panel__subtitle-item">
              单号：{{ orderTitle }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              状态/节点：{{ currentNodeText }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              设备：{{ readonlyValue(form.deviceCode) }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              异常等级：{{ getLevelText(form.exceptionLevel) }}
            </div>
          </div>
        </div>
        <div class="qms-ncr-toolbar__actions">
          <div class="qms-ncr-toolbar__log-icons">
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--audit"
              size="small"
              title="流程轨迹"
              @click="message.info('流程轨迹见单据下方')"
            >
              <IconifyIcon icon="lucide:clipboard-check" />
            </Button>
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--workflow"
              size="small"
              title="业务流程"
              @click="message.info('设备异常流程按当前单据节点流转')"
            >
              <IconifyIcon icon="lucide:git-fork" />
            </Button>
            <Button
              class="qms-ncr-toolbar-icon-btn qms-ncr-toolbar-icon-btn--operation"
              size="small"
              title="操作记录"
              @click="message.info('操作记录见流程轨迹')"
            >
              <IconifyIcon icon="lucide:history" />
            </Button>
          </div>
          <Button
            v-for="action in toolbarActions"
            :key="action.key"
            class="qms-ncr-toolbar-action"
            :danger="action.danger"
            size="small"
            :title="action.title"
            :type="action.type"
            @click="action.onClick"
          >
            <IconifyIcon
              v-if="action.key === 'primary'"
              icon="lucide:check"
              class="mr-1"
            />
            <IconifyIcon
              v-else-if="action.key === 'windowClose'"
              icon="lucide:x"
              class="mr-1"
            />
            <span>{{ action.label }}</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading" class="detail-spin">
        <div class="detail-content">
          <div class="qms-exception-workbench">
            <Form class="qms-exception-form" :model="form" layout="vertical">
              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>一、发起异常</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>设备编号</label>
                  <div class="erp-form-value">
                    <template v-if="isCreate">
                      <Input
                        v-model:value="form.deviceCode"
                        placeholder="输入设备编号后自动带入设备名称"
                        @blur="handleDeviceCodeBlur"
                      />
                      <Button
                        class="qms-exception-inline-select"
                        size="small"
                        type="link"
                        @click="openDeviceSelect"
                      >
                        选择
                      </Button>
                    </template>
                    <div v-else class="qms-exception-readonly-value">
                      {{ readonlyValue(form.deviceCode) }}
                    </div>
                  </div>
                  <label>设备名称</label>
                  <div class="erp-form-value">
                    <div v-if="!isCreate" class="qms-exception-readonly-value">
                      {{ readonlyValue(form.deviceName) }}
                    </div>
                    <Input
                      v-else
                      v-model:value="form.deviceName"
                      disabled
                      placeholder="由设备编号带入"
                    />
                  </div>
                  <label>紧急程度</label>
                  <div class="erp-form-value">
                    <Select
                      v-if="isCreate"
                      v-model:value="form.exceptionLevel"
                      :options="levelOptions"
                    />
                    <div v-else class="qms-exception-readonly-value">
                      {{ getLevelText(form.exceptionLevel) }}
                    </div>
                  </div>
                  <label>提报人</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ readonlyValue(form.reporter) }}
                    </div>
                  </div>
                  <label>提报时间</label>
                  <div class="erp-form-value">
                    <DatePicker
                      v-if="isCreate"
                      v-model:value="form.reportTime"
                      class="w-full"
                      disabled
                      show-time
                      value-format="YYYY-MM-DD HH:mm:ss"
                    />
                    <div v-else class="qms-exception-readonly-value">
                      {{ readonlyValue(form.reportTime) }}
                    </div>
                  </div>
                  <label>当前办理人</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ readonlyValue(form.currentHandlerUserName) }}
                    </div>
                  </div>
                  <label class="erp-form-label--tall">异常现象</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Form.Item
                      class="qms-exception-form-item"
                      name="faultDesc"
                      :rules="[{ required: true, message: '请填写异常现象' }]"
                    >
                      <Input.TextArea
                        v-if="isCreate"
                        v-model:value="form.faultDesc"
                        placeholder="请描述设备异常现象、发生经过和当前状态"
                        :rows="4"
                      />
                      <div
                        v-else
                        class="qms-exception-readonly-value qms-exception-readonly-value--multiline qms-exception-sign-detail-readonly"
                      >
                        {{ readonlyValue(form.faultDesc) }}
                      </div>
                    </Form.Item>
                  </div>
                </div>
              </section>

              <section v-if="!isCreate" class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>二、流程办理信息</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>响应分派人</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.dispatcher) }}
                  </div>
                  <label>响应分派时间</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.dispatchTime) }}
                  </div>
                  <label>维修人</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.assignee) }}
                  </div>
                  <label>维修实际时间</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.repairTime) }}
                  </div>
                  <label>维修状态</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.repairStatus) }}
                  </div>
                  <label>确认人</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.confirmer) }}
                  </div>
                  <label>确认时间</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.confirmTime) }}
                  </div>
                  <label>归档人</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.archiver) }}
                  </div>
                  <label>归档时间</label>
                  <div class="erp-form-value">
                    {{ readonlyValue(form.archiveTime) }}
                  </div>
                </div>
              </section>

              <section
                v-if="currentAction === 'RESPOND'"
                class="erp-basic-form"
              >
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>三、响应分派</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>办理选项</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Radio.Group
                      v-model:value="processForm.responseResult"
                      class="qms-exception-radio-line"
                    >
                      <Radio.Button value="HANDLED">
                        已处理，无需维修
                      </Radio.Button>
                      <Radio.Button value="NEED_REPAIR">需要维修</Radio.Button>
                    </Radio.Group>
                  </div>
                  <template v-if="processForm.responseResult === 'NEED_REPAIR'">
                    <label>维修时间</label>
                    <div class="erp-form-value">
                      <DatePicker
                        v-model:value="processForm.repairPlanTime"
                        class="w-full"
                        show-time
                        value-format="YYYY-MM-DD HH:mm:ss"
                      />
                    </div>
                    <label>维修人</label>
                    <div class="erp-form-value erp-form-value--span-3">
                      <Input
                        v-model:value="processForm.assignee"
                        placeholder="请选择维修人"
                        readonly
                      />
                      <Button
                        class="qms-exception-inline-select"
                        size="small"
                        type="link"
                        @click="openAssigneeSelect"
                      >
                        选择
                      </Button>
                    </div>
                  </template>
                  <label class="erp-form-label--tall">处理说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.responseRemark"
                      placeholder="请填写处理说明"
                      :rows="3"
                    />
                  </div>
                  <template v-if="processForm.responseResult === 'NEED_REPAIR'">
                    <label class="erp-form-label--tall">维修说明</label>
                    <div class="erp-form-value erp-form-value--span-5">
                      <Input.TextArea
                        v-model:value="processForm.repairPlanRemark"
                        placeholder="请填写维修计划说明"
                        :rows="3"
                      />
                    </div>
                    <label class="erp-form-label--tall">换件说明</label>
                    <div class="erp-form-value erp-form-value--span-5">
                      <Input.TextArea
                        v-model:value="processForm.partChangeDesc"
                        placeholder="请填写换件说明"
                        :rows="2"
                      />
                    </div>
                  </template>
                </div>
              </section>

              <section v-if="currentAction === 'REPAIR'" class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>三、维修执行</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>维修实际时间</label>
                  <div class="erp-form-value">
                    <DatePicker
                      v-model:value="processForm.repairTime"
                      class="w-full"
                      show-time
                      value-format="YYYY-MM-DD HH:mm:ss"
                    />
                  </div>
                  <label>维修状态</label>
                  <div class="erp-form-value erp-form-value--span-3">
                    <Select
                      v-model:value="processForm.repairStatus"
                      :options="[
                        { label: '已完成', value: 'DONE' },
                        { label: '有遗留', value: 'LEFTOVER' },
                      ]"
                    />
                  </div>
                  <label class="erp-form-label--tall">维修说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.repairAction"
                      placeholder="请填写维修说明"
                      :rows="3"
                    />
                  </div>
                  <template v-if="processForm.repairStatus === 'LEFTOVER'">
                    <label class="erp-form-label--tall">遗留问题说明</label>
                    <div class="erp-form-value erp-form-value--span-5">
                      <Input.TextArea
                        v-model:value="processForm.unfixReason"
                        placeholder="请填写遗留问题说明"
                        :rows="3"
                      />
                    </div>
                  </template>
                  <label class="erp-form-label--tall">换件说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.partChangeDesc"
                      placeholder="请填写换件说明"
                      :rows="2"
                    />
                  </div>
                </div>
              </section>

              <section
                v-if="currentAction === 'CONFIRM'"
                class="erp-basic-form"
              >
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>三、完成确认</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label class="erp-form-label--tall">确认说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.confirmRemark"
                      placeholder="请填写确认说明"
                      :rows="3"
                    />
                  </div>
                </div>
              </section>

              <section
                v-if="currentAction === 'ARCHIVE'"
                class="erp-basic-form"
              >
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>三、关闭归档</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label class="erp-form-label--tall">原因分析</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.rootCause"
                      placeholder="请填写原因分析"
                      :rows="3"
                    />
                  </div>
                  <label class="erp-form-label--tall">预防措施</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.preventiveAction"
                      placeholder="请填写预防措施"
                      :rows="3"
                    />
                  </div>
                  <label class="erp-form-label--tall">归档说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Input.TextArea
                      v-model:value="processForm.archiveReason"
                      placeholder="请填写归档说明"
                      :rows="2"
                    />
                  </div>
                </div>
              </section>

              <section v-if="!isCreate" class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>四、流程轨迹</strong>
                  </div>
                </div>
                <div class="qms-exception-tracking-table">
                  <table>
                    <thead>
                      <tr>
                        <th>节点</th>
                        <th>办理人</th>
                        <th>办理时间</th>
                        <th>办理意见/说明</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="log in form.flowLogs || []"
                        :key="`${log.actionCode}-${log.handleTime}`"
                      >
                        <td>{{ log.actionName }}</td>
                        <td>{{ readonlyValue(log.handlerUserName) }}</td>
                        <td>{{ readonlyValue(log.handleTime) }}</td>
                        <td>{{ readonlyValue(log.opinion) }}</td>
                      </tr>
                      <tr v-if="!form.flowLogs?.length">
                        <td colspan="4" class="qms-exception-empty-cell">
                          暂无流程轨迹
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section v-if="!isCreate" class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>五、更换备件</strong>
                  </div>
                </div>
                <div class="qms-exception-subtable">
                  <PartList
                    ref="partListRef"
                    :disabled="!partEditable"
                    :order-id="form.id"
                  />
                </div>
              </section>
            </Form>
          </div>
        </div>
      </Spin>
    </div>

    <DeviceSelectModal
      ref="deviceSelectModalRef"
      @select="handleDeviceSelected"
    />
    <UserSelectModalComp @confirm="handleAssigneeConfirm" />
  </BaseModal>
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
  border-color: #ea580c;
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

.qms-exception-inline-select {
  flex-shrink: 0;
  margin-left: 6px;
  padding: 0 4px;
}

.qms-exception-radio-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 26px;
  align-items: center;
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

.qms-exception-tracking-table,
.qms-exception-subtable {
  padding: 10px;
}

.qms-exception-tracking-table table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.qms-exception-tracking-table th,
.qms-exception-tracking-table td {
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
  word-break: break-word;
}

.qms-exception-tracking-table th {
  background: #f1f5f9;
  color: #334155;
  font-weight: 800;
}

.qms-exception-empty-cell {
  color: #94a3b8;
  text-align: center !important;
}

@media (max-width: 1100px) {
  .qms-ncr-toolbar {
    grid-template-columns: 1fr;
  }

  .qms-ncr-toolbar__actions {
    justify-self: stretch;
  }

  .erp-form-grid {
    grid-template-columns: 110px minmax(0, 1fr);
  }

  .erp-form-value--span-3,
  .erp-form-value--span-5 {
    grid-column: span 1;
  }
}
</style>
