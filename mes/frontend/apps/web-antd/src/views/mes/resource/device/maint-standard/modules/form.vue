<script lang="ts" setup>
import type { MesMaintStandardApi } from '#/api/mes/resource/device/maint-standard';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Form,
  Input,
  message,
  Radio,
  Select,
  Spin,
  Tabs,
} from 'ant-design-vue';

import { getCategoryList } from '#/api/mes/resource/device/category';
import {
  createStandard,
  getStandard,
  updateStandard,
} from '#/api/mes/resource/device/maint-standard';

import { mapCategoryOptions } from '../../shared';
import ItemList from './item-list.vue';

import '../../qms-detail-style.css';

defineOptions({ name: 'MaintStandardFormModal' });

const emit = defineEmits(['success']);

const formRef = ref();
const formType = ref('');
const formData = ref<MesMaintStandardApi.Standard>({
  status: 1,
} as MesMaintStandardApi.Standard);
const loading = ref(false);
const itemListRef = ref<InstanceType<typeof ItemList>>();
const categoryOptions = ref<Array<{ label?: string; value?: number }>>([]);

const frequencyOptions = [
  { label: '按明细项目', value: '按项目' },
  { label: '一个月', value: '一个月' },
  { label: '每季度', value: '每季度' },
  { label: '每半年', value: '每半年' },
  { label: '1年', value: '1年' },
  { label: '每次开机前', value: 'PRE_START' },
  { label: '每班班前', value: 'PRE_SHIFT' },
  { label: '每日一次', value: 'DAILY' },
  { label: '每周一次', value: 'WEEKLY' },
];
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const isDetail = computed(() => formType.value === 'detail');
const isUpdate = computed(() => formType.value === 'edit');
const titleText = computed(() => {
  if (isDetail.value) return '查看保养标准';
  return isUpdate.value ? '编辑保养标准' : '新增保养标准';
});

const [BaseModal, modalApi] = useVbenModal({
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
    formType.value = data?.type || 'create';
    formData.value = { status: 1 } as MesMaintStandardApi.Standard;

    loading.value = true;
    try {
      categoryOptions.value = mapCategoryOptions(await getCategoryList());
      if (formType.value !== 'create' && data.id) {
        formData.value = await getStandard(data.id);
      }
    } finally {
      loading.value = false;
    }
  },
});

function getOptionLabel(
  options: Array<{ label?: string; value?: number | string }>,
  value?: number | string,
) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

function getCategoryLabel() {
  return (
    formData.value.categoryName ||
    getOptionLabel(categoryOptions.value, formData.value.categoryId) ||
    '-'
  );
}

function handleClose() {
  modalApi.close();
}

async function handleSubmit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const data = {
      ...formData.value,
      items: itemListRef.value?.getData(),
    };
    if (isUpdate.value) {
      await updateStandard(data);
      message.success('更新成功');
    } else {
      await createStandard(data);
      message.success('创建成功');
    }
    emit('success');
    modalApi.close();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <BaseModal>
    <div class="qms-ncr-detail">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder">
          <IconifyIcon
            icon="lucide:file-check-2"
            class="text-lg text-sky-700"
          />
          <span class="text-xs font-bold text-slate-600">
            {{ isDetail ? '标准档案' : '标准维护' }}
          </span>
        </div>

        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">{{ titleText }}</div>
          <div class="qms-ncr-title-panel__subtitle">
            <span class="qms-ncr-title-panel__subtitle-item">
              标准编号：{{ formData.code || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              标准名称：{{ formData.name || '-' }}
            </span>
            <span class="qms-ncr-title-panel__subtitle-item">
              适用分类：{{ getCategoryLabel() }}
            </span>
          </div>
        </div>

        <div class="qms-ncr-toolbar__actions">
          <Button
            v-if="!isDetail"
            class="qms-ncr-toolbar-action"
            size="small"
            type="primary"
            @click="handleSubmit"
          >
            <IconifyIcon icon="lucide:check" class="mr-1" />
            <span>保存</span>
          </Button>
          <Button
            class="qms-ncr-toolbar-action"
            size="small"
            @click="handleClose"
          >
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
                    <strong>一、保养标准主信息</strong>
                    <span class="qms-exception-section-subtitle">
                      标准编码、适用分类、兜底频率与状态
                    </span>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <label>标准编号</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="code"
                      :rules="[{ required: true, message: '请输入标准编号' }]"
                    >
                      <Input
                        v-model:value="formData.code"
                        placeholder="如 MS-001"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value font-mono">
                      {{ formData.code || '-' }}
                    </div>
                  </div>
                  <label>标准名称</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="name"
                      :rules="[{ required: true, message: '请输入标准名称' }]"
                    >
                      <Input
                        v-model:value="formData.name"
                        placeholder="请输入标准名称"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value">
                      {{ formData.name || '-' }}
                    </div>
                  </div>
                  <label>适用设备分类</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="categoryId"
                      :rules="[{ required: true, message: '请选择设备分类' }]"
                    >
                      <Select
                        v-model:value="formData.categoryId"
                        allow-clear
                        :options="categoryOptions"
                        placeholder="请选择设备分类"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value">
                      {{ getCategoryLabel() }}
                    </div>
                  </div>
                  <label>兜底频率</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="frequency"
                      :rules="[{ required: true, message: '请选择执行频率' }]"
                    >
                      <Select
                        v-model:value="formData.frequency"
                        :options="frequencyOptions"
                        placeholder="请选择触发频率"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value">
                      {{ getOptionLabel(frequencyOptions, formData.frequency) }}
                    </div>
                  </div>
                  <label>保养等级</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="maintType"
                    >
                      <Input
                        v-model:value="formData.maintType"
                        placeholder="如 日常巡检/一级保养"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value">
                      {{ formData.maintType || '-' }}
                    </div>
                  </div>
                  <label>启用状态</label>
                  <div class="erp-form-value">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="status"
                      :rules="[{ required: true, message: '请选择启用状态' }]"
                    >
                      <Radio.Group
                        v-model:value="formData.status"
                        button-style="solid"
                        option-type="button"
                        :options="statusOptions"
                      />
                    </Form.Item>
                    <div v-else class="qms-exception-readonly-value">
                      {{ getOptionLabel(statusOptions, formData.status) }}
                    </div>
                  </div>
                  <label class="erp-form-label--tall">备注说明</label>
                  <div class="erp-form-value erp-form-value--span-5">
                    <Form.Item
                      v-if="!isDetail"
                      class="qms-exception-form-item"
                      name="remark"
                    >
                      <Input.TextArea
                        v-model:value="formData.remark"
                        placeholder="请输入指导说明或注意事项"
                        :rows="3"
                      />
                    </Form.Item>
                    <div
                      v-else
                      class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                    >
                      {{ formData.remark || '-' }}
                    </div>
                  </div>
                </div>
              </section>

              <section class="erp-basic-form">
                <div class="detail-list-head">
                  <div class="detail-list-title">
                    <strong>二、标准明细</strong>
                    <span class="qms-exception-section-subtitle">
                      检查清扫项目、作业方法、合格标准与工具
                    </span>
                  </div>
                </div>
                <Tabs class="qms-exception-tabs" type="card">
                  <Tabs.TabPane key="items" tab="标准明细">
                    <ItemList
                      ref="itemListRef"
                      :disabled="isDetail"
                      :standard-id="formData?.id"
                    />
                  </Tabs.TabPane>
                </Tabs>
              </section>
            </Form>
          </div>
        </div>
      </Spin>
    </div>
  </BaseModal>
</template>
