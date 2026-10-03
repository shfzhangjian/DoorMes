<script lang="ts" setup>
import type { MesMaintTaskApi } from '#/api/mes/resource/device/maint-task';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Spin, Tabs, Tag } from 'ant-design-vue';

import { getTask } from '#/api/mes/resource/device/maint-task';

import '../../qms-detail-style.css';
import { optionColor, optionLabel } from '../../shared';
import { MAINT_RESULT_OPTIONS, MAINT_TYPE_OPTIONS } from '../data';
import ItemList from './item-list.vue';

defineOptions({ name: 'MaintTaskFormModal' });

const itemListRef = ref<InstanceType<typeof ItemList>>();
const formData = ref<MesMaintTaskApi.Task>({} as MesMaintTaskApi.Task);
const loading = ref(false);

const maintTypeText = computed(() =>
  optionLabel(MAINT_TYPE_OPTIONS, formData.value.maintType),
);
const maintTypeColor = computed(() =>
  optionColor(MAINT_TYPE_OPTIONS, formData.value.maintType),
);
const statusText = computed(() =>
  optionLabel(MAINT_RESULT_OPTIONS, formData.value.status),
);
const statusColor = computed(() =>
  optionColor(MAINT_RESULT_OPTIONS, formData.value.status),
);

const [BaseModal, modalApi] = useVbenModal({
  title: '',
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
  onOpenChange: async (isOpen) => {
    if (!isOpen) {
      formData.value = {} as MesMaintTaskApi.Task;
      loading.value = false;
      return;
    }
    const data = modalApi.getData<any>();
    formData.value = {} as MesMaintTaskApi.Task;
    await nextTick();

    if (data.id) {
      loading.value = true;
      try {
        formData.value = await getTask(data.id);
      } finally {
        loading.value = false;
      }
    }
  },
});

function displayValue(value?: number | string | null) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function handleClose() {
  modalApi.close();
}
</script>

<template>
  <BaseModal>
    <div class="qms-ncr-detail">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <IconifyIcon icon="lucide:clipboard-check" class="text-lg text-sky-700" />
          <span class="text-xs font-bold text-slate-600">点检档案</span>
        </div>

        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">设备点检打卡档案</div>
          <div class="qms-ncr-title-panel__subtitle">
            <span class="qms-ncr-title-panel__subtitle-item">
              记录编号：{{ displayValue(formData.recordNo) }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              任务单号：{{ displayValue(formData.taskNo) }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              设备：{{ displayValue(formData.deviceName || formData.deviceCode) }}
            </span>
          </div>
        </div>

        <div class="qms-ncr-toolbar__actions">
          <Button class="qms-ncr-toolbar-action" size="small" @click="handleClose">
            <IconifyIcon icon="lucide:x" class="mr-1" />
            <span>关闭</span>
          </Button>
        </div>
      </div>

      <Spin :spinning="loading" class="detail-spin">
        <div class="detail-content">
          <div class="qms-exception-workbench">
            <div class="qms-exception-form">
              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>一、点检任务基础信息</strong>
                    <span class="qms-exception-section-subtitle">
                      任务来源、设备、标准、计划与执行结果
                    </span>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>记录编号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ displayValue(formData.recordNo) }}
                    </div>
                  </div>
                  <label>任务单号</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ displayValue(formData.taskNo) }}
                    </div>
                  </div>
                  <label>设备编码</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value font-mono">
                      {{ displayValue(formData.deviceCode) }}
                    </div>
                  </div>
                  <label>设备名称</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.deviceName) }}
                    </div>
                  </div>
                  <label>设备分类</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.categoryName) }}
                    </div>
                  </div>
                  <label>应用标准</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.standardName) }}
                    </div>
                  </div>
                  <label>维保类型</label>
                  <div class="erp-form-value">
                    <Tag :color="maintTypeColor">{{ maintTypeText }}</Tag>
                  </div>
                  <label>执行人</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.operator) }}
                    </div>
                  </div>
                  <label>执行结果</label>
                  <div class="erp-form-value">
                    <Tag :color="statusColor">{{ statusText }}</Tag>
                  </div>
                  <label>应检日期</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.dueDate) }}
                    </div>
                  </div>
                  <label>计划时间</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.planTime) }}
                    </div>
                  </div>
                  <label>完成时间</label>
                  <div class="erp-form-value">
                    <div class="qms-exception-readonly-value">
                      {{ displayValue(formData.actualTime) }}
                    </div>
                  </div>
                  <label class="erp-form-label--tall">异常/备注</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <div
                      class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                    >
                      {{ displayValue(formData.remark) }}
                    </div>
                  </div>
                </div>
              </section>

              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>二、点检打卡明细</strong>
                    <span class="qms-exception-section-subtitle">
                      实际检查项目、作业方法、合格标准与结果
                    </span>
                  </div>
                </div>
                <Tabs class="qms-exception-tabs" type="card">
                  <Tabs.TabPane key="items" tab="实际打卡项">
                    <ItemList ref="itemListRef" :task-id="formData?.id" />
                  </Tabs.TabPane>
                </Tabs>
              </section>
            </div>
          </div>
        </div>
      </Spin>
    </div>
  </BaseModal>
</template>
