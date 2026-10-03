<script lang="ts" setup>
import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Form, Input, message, Spin, Tabs, Upload } from 'ant-design-vue';
import dayjs from 'dayjs';

import { executeTask, getTask } from '#/api/mes/resource/device/maint-task-exec';

import '../../qms-detail-style.css';
import PartList from './part-list.vue';
import StandardItemList from './standard-item-list.vue';

defineOptions({ name: 'MesMaintTaskExecModal' });

const emit = defineEmits(['success']);

const formRef = ref();
const formData = ref<any>({ fileList: [] });
const isTodo = ref(false);
const loading = ref(false);
const partListRef = ref<InstanceType<typeof PartList>>();
const standardItemListRef = ref<InstanceType<typeof StandardItemList>>();

const titleText = computed(() =>
  isTodo.value ? '保养任务执行与反馈' : '维保执行记录档案',
);
const statusText = computed(() => {
  if (formData.value.rawStatus === 'WAIT_CONFIRM') return '待确认';
  if (formData.value.rawStatus === 'ABNORMAL') return '异常完成';
  if (formData.value.status === 'DONE') return '已闭环';
  return isTodo.value ? '待执行反馈' : formData.value.status || '-';
});

const [Modal, modalApi] = useVbenModal({
  title: '',
  class: 'qms-product-event-detail-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  header: false,
  fullscreen: true,
  fullscreenButton: false,
  showCancelButton: false,
  showConfirmButton: false,
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    isTodo.value = data.status === 'TODO' || data.status === 'WAIT_CONFIRM';

    loading.value = true;
    try {
      const res = await getTask(data.id);
      formData.value = {
        ...res,
        actualDate: res.actualDate || dayjs().format('YYYY-MM-DD HH:mm:ss'),
        executor: res.executor || '当前用户',
        fileList:
          res.photos && res.photos.length > 0
            ? [{ name: '现场记录照片.jpg', status: 'done', uid: '1', url: '#' }]
            : [],
      };
      await nextTick();
    } finally {
      loading.value = false;
    }
  },
});

function handleClose() {
  modalApi.close();
}

async function handleSubmit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const payload = { ...formData.value };
    payload.items = standardItemListRef.value?.getData();
    payload.parts = partListRef.value?.getData();
    payload.status = standardItemListRef.value?.hasAbnormal() ? 'ABNORMAL' : 'DONE';
    await executeTask(payload);
    message.success('保养任务已提交反馈，状态已进入待确认');
    emit('success');
    modalApi.close();
  } finally {
    loading.value = false;
  }
}

function beforeUpload(file: any) {
  message.success(`照片 ${file.name} 已附加`);
  return false;
}
</script>

<template>
  <Modal>
    <div class="qms-ncr-detail">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <IconifyIcon icon="lucide:clipboard-check" class="text-lg text-sky-700" />
          <span class="text-xs font-bold text-slate-600">{{ statusText }}</span>
        </div>

        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">{{ titleText }}</div>
          <div class="qms-ncr-title-panel__subtitle">
            <span class="qms-ncr-title-panel__subtitle-item">
              任务单号：{{ formData.taskNo || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              设备：{{ formData.deviceName || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              周期：{{ formData.planPeriod || '-' }}
            </span>
          </div>
        </div>

        <div class="qms-ncr-toolbar__actions">
          <Button
            v-if="isTodo"
            class="qms-ncr-toolbar-action"
            size="small"
            type="primary"
            @click="handleSubmit"
          >
            <IconifyIcon icon="lucide:check" class="mr-1" />
            <span>提交反馈</span>
          </Button>
          <Button class="qms-ncr-toolbar-action" size="small" @click="handleClose">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            <span>关闭</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading" class="detail-spin">
        <div class="detail-content">
          <div class="qms-exception-workbench">
            <Form
              ref="formRef"
              class="qms-exception-form"
              :model="formData"
              layout="vertical"
            >
              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>一、基础任务要求</strong>
                    <span class="qms-exception-section-subtitle">
                      设备保养任务来源与执行要求
                    </span>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>任务单号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ formData.taskNo || '-' }}
                    </div>
                  </div>
                  <label>排程周期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.planPeriod || '-' }}
                    </div>
                  </div>
                  <label>维保级别</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.maintType || '-' }}
                    </div>
                  </div>
                  <label>保养设备</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.deviceCode || '-' }} - {{ formData.deviceName || '-' }}
                    </div>
                  </div>
                  <label>保养标准</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.standardName || '-' }}
                    </div>
                  </div>
                  <label>应检日期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.dueDate || '-' }}
                    </div>
                  </div>
                  <label>要求完成日期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ formData.planDate || '-' }}
                    </div>
                  </div>
                  <label class="erp-form-label--tall">维保要求说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <div class="qms-exception-readonly-value qms-exception-readonly-value--multiline">
                      {{ formData.taskDesc || '-' }}
                    </div>
                  </div>
                </div>
              </section>

              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>二、执行反馈与明细</strong>
                    <span class="qms-exception-section-subtitle">
                      现场反馈、标准明细与更换备件清单
                    </span>
                  </div>
                </div>
                <Tabs class="qms-exception-tabs" type="card">
                  <Tabs.TabPane key="feedback" tab="现场执行反馈">
                    <div class="erp-form-grid">
                      <label>实际执行人</label>
                      <div class="erp-form-value">
                        <Form.Item class="qms-exception-form-item" name="executor">
                          <Input v-model:value="formData.executor" :disabled="!isTodo" />
                        </Form.Item>
                      </div>
                      <label>执行完成时间</label>
                      <div class="erp-form-value">
                        <Form.Item class="qms-exception-form-item" name="actualDate">
                          <Input v-model:value="formData.actualDate" :disabled="!isTodo" />
                        </Form.Item>
                      </div>
                      <label>执行状态</label>
                      <div class="erp-form-value">
                        <div class="qms-exception-readonly-value">{{ statusText }}</div>
                      </div>
                      <label class="erp-form-label--tall">执行情况说明</label>
                      <div class="erp-form-value erp-form-value--span-5">
                        <Form.Item
                          class="qms-exception-form-item"
                          name="executeRemark"
                          :rules="[{ required: true, message: '请填写维保执行情况说明' }]"
                        >
                          <Input.TextArea
                            v-model:value="formData.executeRemark"
                            :disabled="!isTodo"
                            placeholder="记录设备状态、发现的隐患及处理动作"
                            :rows="3"
                          />
                        </Form.Item>
                      </div>
                      <label class="erp-form-label--tall">现场留痕照片</label>
                      <div class="erp-form-value erp-form-value--span-5">
                        <Form.Item class="qms-exception-form-item" name="fileList">
                          <Upload
                            v-model:file-list="formData.fileList"
                            :before-upload="beforeUpload"
                            :disabled="!isTodo"
                            list-type="picture-card"
                          >
                            <div v-if="isTodo" class="flex flex-col items-center">
                              <IconifyIcon icon="lucide:camera" class="text-2xl text-slate-400" />
                              <div class="mt-2 text-xs text-slate-500">上传照片</div>
                            </div>
                          </Upload>
                        </Form.Item>
                      </div>
                    </div>
                  </Tabs.TabPane>
                  <Tabs.TabPane key="items" tab="标准明细">
                    <StandardItemList
                      ref="standardItemListRef"
                      :disabled="!isTodo"
                      :task-id="formData.id"
                    />
                  </Tabs.TabPane>
                  <Tabs.TabPane key="parts" tab="更换备件清单">
                    <PartList ref="partListRef" :disabled="!isTodo" :task-id="formData.id" />
                  </Tabs.TabPane>
                </Tabs>
              </section>
            </Form>
          </div>
        </div>
      </Spin>
    </div>
  </Modal>
</template>
