<script lang="ts" setup>
import type { OaEcologyApi } from '#/api/oa/ecology';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { formatDate } from '@vben/utils';

import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Space,
  Spin,
  Switch,
  Tag,
  Textarea,
  Tooltip,
} from 'ant-design-vue';

import {
  getOaEcologyConfigStatus,
  sendOaEcologyTestMessage,
} from '#/api/oa/ecology';

defineOptions({ name: 'OaEcologyMessageTest' });

type ReceiveStatus = 'NOT_RECEIVED' | 'PENDING' | 'RECEIVED';

interface SendLog {
  id: number;
  receiveStatus: ReceiveStatus;
  request: OaEcologyApi.SendMessageReq;
  response: OaEcologyApi.SendMessageResp;
  time: number;
}

const configLoading = ref(false);
const sendLoading = ref(false);
const configModalOpen = ref(false);
const configStatus = ref<OaEcologyApi.ConfigStatus>();
const sendLogs = ref<SendLog[]>([]);

const formState = reactive({
  receiverEmployeeId: '1300822861024346113',
  receiverName: 'MES测试',
  receiverTenantKey: 't7akvdnf84',
  receiverWorkCode: '',
  title: 'MES测试消息',
  text: '这是一条来自 MES 的 EMobile10 即时通讯测试消息。MES访问地址：http://192.168.2.21:8080/',
  entityId: '',
  entityName: 'MES消息MVP',
  pcUrl: 'http://192.168.2.21:8080/',
  h5Url: 'http://192.168.2.21:8080/',
  todo: false,
});

const configReady = computed(() => {
  const value = configStatus.value;
  return Boolean(
    value?.enabled &&
      (value.accessTokenConfigured ||
        (value.appKeyConfigured && value.appSecretConfigured)),
  );
});

const configTag = computed(() => {
  if (!configStatus.value) {
    return { color: 'default', text: '未读取' };
  }
  if (configReady.value) {
    return { color: 'success', text: '已就绪' };
  }
  if (configStatus.value.enabled) {
    return { color: 'warning', text: '待补全' };
  }
  return { color: 'error', text: '未启用' };
});

function displayUser(user?: OaEcologyApi.User) {
  if (!user) {
    return '-';
  }
  return [
    user.username,
    user.employeeId ? `ID:${user.employeeId}` : undefined,
    user.workCode ? `工号:${user.workCode}` : undefined,
  ]
    .filter(Boolean)
    .join(' / ');
}

async function loadConfig() {
  configLoading.value = true;
  try {
    configStatus.value = await getOaEcologyConfigStatus();
  } finally {
    configLoading.value = false;
  }
}

function buildRequest(): OaEcologyApi.SendMessageReq {
  return {
    channels: [1],
    entityId: formState.entityId.trim() || undefined,
    entityName: formState.entityName.trim() || undefined,
    h5Url: formState.h5Url.trim() || undefined,
    pcUrl: formState.pcUrl.trim() || undefined,
    receiverEmployeeId: formState.receiverEmployeeId.trim() || undefined,
    receiverName: formState.receiverName.trim() || undefined,
    receiverTenantKey: formState.receiverTenantKey.trim() || undefined,
    receiverWorkCode: formState.receiverWorkCode.trim() || undefined,
    text: formState.text.trim(),
    title: formState.title.trim(),
    todo: formState.todo,
  };
}

async function handleSend() {
  if (!formState.title.trim()) {
    message.warning('请输入消息标题');
    return;
  }
  if (!formState.text.trim()) {
    message.warning('请输入消息内容');
    return;
  }
  if (
    !formState.receiverEmployeeId.trim() &&
    !formState.receiverWorkCode.trim()
  ) {
    message.warning('请填写 OA 人员 ID 或 OA 工号');
    return;
  }

  sendLoading.value = true;
  try {
    const request = buildRequest();
    const response = await sendOaEcologyTestMessage(request);
    sendLogs.value.unshift({
      id: Date.now(),
      receiveStatus: 'PENDING',
      request,
      response,
      time: Date.now(),
    });
    if (response.success) {
      message.success('OA 消息已提交');
    } else {
      message.warning(response.message || 'OA 返回未成功');
    }
  } finally {
    sendLoading.value = false;
  }
}

function updateReceiveStatus(log: SendLog, status: ReceiveStatus) {
  log.receiveStatus = status;
}

function receiveStatusTag(status: ReceiveStatus) {
  if (status === 'RECEIVED') {
    return { color: 'success', text: '已收到' };
  }
  if (status === 'NOT_RECEIVED') {
    return { color: 'error', text: '未收到' };
  }
  return { color: 'processing', text: '待确认' };
}

function sendStatusTag(resp: OaEcologyApi.SendMessageResp) {
  return resp.success
    ? { color: 'success', text: '已提交' }
    : { color: 'error', text: '提交失败' };
}

function formatChannels(value?: number[]) {
  if (!value || value.length === 0) {
    return '-';
  }
  return value.map((item) => (item === 1 ? 'IM' : String(item))).join('、');
}

onMounted(() => {
  void loadConfig();
});
</script>

<template>
  <Page auto-content-height>
    <div class="oa-message-test">
      <section class="oa-message-test__toolbar">
        <div class="oa-message-test__title">
          <span class="oa-message-test__title-icon">
            <IconifyIcon icon="lucide:message-square-text" />
          </span>
          <div>
            <h2>泛微消息测试</h2>
            <p>泛微 OA 工作消息直发联调</p>
          </div>
        </div>
        <Space>
          <Button
            :loading="configLoading"
            @click="
              configModalOpen = true;
              loadConfig();
            "
          >
            <template #icon>
              <IconifyIcon icon="lucide:settings" />
            </template>
            配置状态
          </Button>
          <Button type="primary" :loading="sendLoading" @click="handleSend">
            <template #icon>
              <IconifyIcon icon="lucide:send-horizontal" />
            </template>
            发送测试
          </Button>
        </Space>
      </section>

      <div class="oa-message-test__grid">
        <section class="oa-message-test__left">
          <Card :bordered="false" class="oa-message-test__panel oa-message-test__panel--full">
            <template #title>
              <div class="oa-message-test__card-title">
                <IconifyIcon icon="lucide:send-horizontal" />
                <span>工作消息发送</span>
                <Tag :color="configTag.color">{{ configTag.text }}</Tag>
              </div>
            </template>
            <Alert
              class="oa-message-test__alert"
              message="消息会直接调用泛微 OA sendMsg 工作消息接口，接收人登录 EMobile10.exe 后在左侧消息入口查看。"
              type="info"
              show-icon
            />
            <Form :model="formState" layout="vertical">
              <div class="oa-message-test__dialog">
                <div class="oa-message-test__dialog-meta">
                  <span>发送方</span>
                  <b>{{ configStatus?.senderName || 'MES消息' }}</b>
                  <small>
                    {{ configStatus?.senderEmployeeId || '-' }} /
                    {{ configStatus?.senderTenantKey || '-' }}
                  </small>
                </div>
                <div class="oa-message-test__dialog-arrow">
                  <IconifyIcon icon="lucide:arrow-right" />
                </div>
                <div class="oa-message-test__dialog-meta">
                  <span>接收方</span>
                  <b>{{ formState.receiverName || 'OA接收人' }}</b>
                  <small>
                    {{ formState.receiverEmployeeId || formState.receiverWorkCode || '-' }}
                    /
                    {{ formState.receiverTenantKey || '-' }}
                  </small>
                </div>
              </div>

              <div class="oa-message-test__form-grid oa-message-test__form-grid--triple">
                <Form.Item label="人员 ID" name="receiverEmployeeId">
                  <Input
                    v-model:value="formState.receiverEmployeeId"
                    allow-clear
                    placeholder="OA employeeId，优先使用"
                  />
                </Form.Item>
                <Form.Item label="团队标识" name="receiverTenantKey">
                  <Input
                    v-model:value="formState.receiverTenantKey"
                    allow-clear
                    placeholder="tenantKey"
                  />
                </Form.Item>
                <Form.Item label="待办">
                  <Switch
                    v-model:checked="formState.todo"
                    checked-children="是"
                    un-checked-children="否"
                  />
                </Form.Item>
              </div>
              <div class="oa-message-test__form-grid">
                <Form.Item label="OA 工号" name="receiverWorkCode">
                  <Input
                    v-model:value="formState.receiverWorkCode"
                    allow-clear
                    placeholder="没有 employeeId 时可填写"
                  />
                </Form.Item>
                <Form.Item label="显示姓名" name="receiverName">
                  <Input
                    v-model:value="formState.receiverName"
                    allow-clear
                    placeholder="仅作为 OA 消息显示名"
                  />
                </Form.Item>
              </div>

              <Form.Item label="标题" name="title">
                <Input
                  v-model:value="formState.title"
                  allow-clear
                  placeholder="输入消息标题"
                />
              </Form.Item>
              <Form.Item label="发送消息内容" name="text">
                <Textarea
                  v-model:value="formState.text"
                  :auto-size="{ minRows: 6, maxRows: 9 }"
                  allow-clear
                  placeholder="输入要推送到泛微 OA 工作消息里的内容"
                />
              </Form.Item>
              <div class="oa-message-test__form-grid">
                <Form.Item label="PC 地址" name="pcUrl">
                  <Input v-model:value="formState.pcUrl" allow-clear />
                </Form.Item>
                <Form.Item label="H5 地址" name="h5Url">
                  <Input v-model:value="formState.h5Url" allow-clear />
                </Form.Item>
              </div>
              <div class="oa-message-test__form-grid">
                <Form.Item label="事项 ID" name="entityId">
                  <Input
                    v-model:value="formState.entityId"
                    allow-clear
                    placeholder="为空时后端自动生成"
                  />
                </Form.Item>
                <Form.Item label="事项名称" name="entityName">
                  <Input v-model:value="formState.entityName" allow-clear />
                </Form.Item>
              </div>
              <div class="oa-message-test__send-row">
                <span>推送位置：EMobile10.exe 左侧消息入口中的工作消息会话</span>
                <Button type="primary" :loading="sendLoading" @click="handleSend">
                  <template #icon>
                    <IconifyIcon icon="lucide:send-horizontal" />
                  </template>
                  发送工作消息
                </Button>
              </div>
            </Form>
          </Card>
        </section>

        <section class="oa-message-test__right">
          <Card :bordered="false" class="oa-message-test__panel oa-message-test__panel--full">
            <template #title>
              <div class="oa-message-test__card-title">
                <IconifyIcon icon="lucide:inbox" />
                <span>发送与接收</span>
                <Tag v-if="sendLogs.length">{{ sendLogs.length }} 条</Tag>
              </div>
            </template>
            <Alert
              class="oa-message-test__alert"
              message="查看位置：打开 EMobile10.exe，进入左侧消息入口，在“MES消息”或对应工作消息会话中查看；接收确认以客户端实际显示为准。"
              type="info"
              show-icon
            />
            <Empty v-if="sendLogs.length === 0" description="暂无测试记录" />
            <div v-else class="oa-message-test__logs">
              <article
                v-for="log in sendLogs"
                :key="log.id"
                class="oa-message-test__log"
              >
                <div class="oa-message-test__log-head">
                  <div>
                    <b>{{ log.request.title }}</b>
                    <span>{{ formatDate(log.time) }}</span>
                  </div>
                  <Space>
                    <Tag :color="sendStatusTag(log.response).color">
                      {{ sendStatusTag(log.response).text }}
                    </Tag>
                    <Tag :color="receiveStatusTag(log.receiveStatus).color">
                      {{ receiveStatusTag(log.receiveStatus).text }}
                    </Tag>
                  </Space>
                </div>

                <div class="oa-message-test__log-body">
                  <div>
                    <small>接收人</small>
                    <span>{{ displayUser(log.response.resolvedReceiver) }}</span>
                  </div>
                  <div>
                    <small>OA 返回</small>
                    <span>
                      {{ log.response.code || '-' }} /
                      {{ log.response.message || '-' }}
                    </span>
                  </div>
                  <div>
                    <small>HTTP</small>
                    <span>{{ log.response.httpStatus || '-' }}</span>
                  </div>
                  <div>
                    <small>消息 ID</small>
                    <span>{{ log.response.messageId || '-' }}</span>
                  </div>
                </div>

                <p class="oa-message-test__log-text">{{ log.request.text }}</p>

                <div class="oa-message-test__log-actions">
                  <Button
                    size="small"
                    type="primary"
                    @click="updateReceiveStatus(log, 'RECEIVED')"
                  >
                    <template #icon>
                      <IconifyIcon icon="lucide:check" />
                    </template>
                    已收到
                  </Button>
                  <Button
                    danger
                    size="small"
                    @click="updateReceiveStatus(log, 'NOT_RECEIVED')"
                  >
                    <template #icon>
                      <IconifyIcon icon="lucide:x" />
                    </template>
                    未收到
                  </Button>
                  <Tooltip :title="log.response.rawResponse || '-'">
                    <Button size="small">
                      <template #icon>
                        <IconifyIcon icon="lucide:file-json" />
                      </template>
                      原始响应
                    </Button>
                  </Tooltip>
                </div>
              </article>
            </div>
          </Card>
        </section>
      </div>

      <Modal
        v-model:open="configModalOpen"
        :footer="null"
        title="泛微 OA 配置状态"
        width="720px"
      >
        <Spin :spinning="configLoading">
          <Descriptions :column="2" size="small" bordered>
            <Descriptions.Item label="启用">
              <Tag :color="configStatus?.enabled ? 'success' : 'error'">
                {{ configStatus?.enabled ? '是' : '否' }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="凭据">
              <Tag
                :color="
                  configStatus?.appKeyConfigured &&
                  configStatus?.appSecretConfigured
                    ? 'success'
                    : 'warning'
                "
              >
                {{
                  configStatus?.appKeyConfigured && configStatus?.appSecretConfigured
                    ? '已配置'
                    : '待配置'
                }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="授权配置">
              <Tag :color="configStatus?.corpIdConfigured ? 'success' : 'warning'">
                {{ configStatus?.corpIdConfigured ? '已配置' : '待填写' }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="OpenAPI">
              <Tooltip :title="configStatus?.openApiOrigin || '-'">
                <span class="oa-message-test__ellipsis">
                  {{ configStatus?.openApiOrigin || '-' }}
                </span>
              </Tooltip>
            </Descriptions.Item>
            <Descriptions.Item label="发送人">
              {{ configStatus?.senderName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="发送人 ID">
              {{ configStatus?.senderEmployeeId || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="团队标识">
              {{ configStatus?.senderTenantKey || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="事件/模块">
              {{ configStatus?.eventId || '-' }} /
              {{ configStatus?.moduleId || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="渠道">
              {{ formatChannels(configStatus?.channels) }}
            </Descriptions.Item>
          </Descriptions>
          <Alert
            class="oa-message-test__alert oa-message-test__alert--compact"
            message="配置参数从后端配置文件读取；页面只展示状态，不允许覆盖。"
            type="info"
            show-icon
          />
          <div class="oa-message-test__modal-actions">
            <Button :loading="configLoading" @click="loadConfig">
              <template #icon>
                <IconifyIcon icon="lucide:refresh-cw" />
              </template>
              刷新
            </Button>
          </div>
        </Spin>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.oa-message-test {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f6f8fb;
}

.oa-message-test__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #e7eaf0;
  background: #fff;
}

.oa-message-test__title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.oa-message-test__title h2 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 650;
  line-height: 24px;
}

.oa-message-test__title p {
  margin: 1px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.oa-message-test__title-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #2563eb;
  background: #eaf1ff;
  border: 1px solid #cfe0ff;
  border-radius: 6px;
}

.oa-message-test__grid {
  display: grid;
  grid-template-columns: minmax(520px, 0.98fr) minmax(420px, 1.02fr);
  gap: 12px;
  min-height: 0;
  padding: 12px;
  overflow: hidden;
}

.oa-message-test__left,
.oa-message-test__right {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.oa-message-test__panel {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.oa-message-test__panel--grow,
.oa-message-test__panel--full {
  min-height: 0;
  overflow: hidden;
}

.oa-message-test__panel--full {
  flex: 1;
}

.oa-message-test__card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  font-weight: 650;
}

.oa-message-test__ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.oa-message-test__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.oa-message-test__form-grid--triple {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 96px;
  align-items: end;
}

.oa-message-test__dialog {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.oa-message-test__dialog-meta {
  min-width: 0;
}

.oa-message-test__dialog-meta span,
.oa-message-test__send-row span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.oa-message-test__dialog-meta b {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: #111827;
  font-size: 15px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.oa-message-test__dialog-meta small {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: #4b5563;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.oa-message-test__dialog-arrow {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #2563eb;
  background: #eef4ff;
  border-radius: 6px;
}

.oa-message-test__send-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;
}

.oa-message-test__modal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.oa-message-test__form-actions {
  display: flex;
  width: 100%;
  margin-bottom: 14px;
}

.oa-message-test__user-select {
  min-width: 0;
  flex: 1;
}

.oa-message-test__alert {
  margin-bottom: 12px;
}

.oa-message-test__alert--compact {
  margin-top: 12px;
}

.oa-message-test__logs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 252px);
  overflow: auto;
  padding-right: 4px;
}

.oa-message-test__log {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.oa-message-test__log-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.oa-message-test__log-head b {
  display: block;
  color: #111827;
  font-size: 14px;
}

.oa-message-test__log-head span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.oa-message-test__log-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  background: #f9fafb;
  border-radius: 6px;
}

.oa-message-test__log-body small {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.oa-message-test__log-body span {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.oa-message-test__log-text {
  margin: 10px 0;
  color: #374151;
  line-height: 20px;
  white-space: pre-wrap;
}

.oa-message-test__log-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1180px) {
  .oa-message-test {
    overflow: auto;
  }

  .oa-message-test__grid {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .oa-message-test__logs {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .oa-message-test__toolbar,
  .oa-message-test__log-head {
    align-items: stretch;
    flex-direction: column;
  }

  .oa-message-test__form-grid,
  .oa-message-test__form-grid--triple,
  .oa-message-test__dialog,
  .oa-message-test__log-body {
    grid-template-columns: 1fr;
  }

  .oa-message-test__dialog-arrow {
    transform: rotate(90deg);
  }

  .oa-message-test__form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .oa-message-test__send-row {
    align-items: stretch;
    flex-direction: column;
  }

  .oa-message-test__user-select {
    width: 100%;
  }
}
</style>
