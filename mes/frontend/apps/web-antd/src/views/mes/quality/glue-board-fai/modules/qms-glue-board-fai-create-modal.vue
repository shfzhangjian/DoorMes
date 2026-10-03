<script lang="ts" setup>
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { DatePicker, Form, Input, InputNumber, message, Select, Spin } from 'ant-design-vue';

import { getStandardPage } from '#/api/mes/quality/base/standard';
import { createGlueBoardFaiRecord } from '#/api/mes/quality/fai';
import { GLUE_BOARD_MODEL_OPTIONS } from '../../../hc/execution/report/adhesive-glue-board/data';
import { processCategoryOptions } from '../../fai/data';

defineOptions({ name: 'QmsGlueBoardFaiCreateModal' });

const emit = defineEmits<{
  success: [record: MesFaiApi.FaiRecord];
}>();

type FormState = {
  glueBoardModel?: string;
  gluePlateBatchNo?: string;
  processCategory?: string;
  remark?: string;
  sampleLength?: number;
  standardId?: number;
  submissionTime?: string;
  submitterName?: string;
  workOrderNo?: string;
};

type CreateModalDefaults = Partial<FormState>;

function formatLocalDateTime(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hours = `${date.getHours()}`.padStart(2, '0');
  const minutes = `${date.getMinutes()}`.padStart(2, '0');
  const seconds = `${date.getSeconds()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

const defaultFormState = (): FormState => ({
  submissionTime: formatLocalDateTime(new Date()),
});

const formState = reactive<FormState>(defaultFormState());
const standards = ref<MesQualityStandardApi.Standard[]>([]);
const standardLoading = ref(false);

const selectedStandard = computed(() =>
  standards.value.find((item) => item.id === formState.standardId),
);
const selectedProcessOption = computed(() =>
  processCategoryOptions.find(
    (item) => item.value === formState.processCategory,
  ),
);

const currentGlueBoardModel = computed(() =>
  String(formState.glueBoardModel || '').trim(),
);
const matchedStandards = computed(() =>
  standards.value.filter(
    (item) =>
      !currentGlueBoardModel.value ||
      item.glueBoardModel === currentGlueBoardModel.value,
  ),
);
const standardOptions = computed(() =>
  matchedStandards.value.map((item) => ({
    label: [
      item.standardNo,
      item.standardName,
      item.version ? `V${item.version}` : '',
      item.glueBoardModel,
    ]
      .filter(Boolean)
      .join(' / '),
    value: item.id!,
  })),
);

function resetForm() {
  Object.assign(formState, defaultFormState());
}

function applyDefaults(defaults?: CreateModalDefaults) {
  if (!defaults) return;
  Object.assign(formState, {
    ...defaults,
    submissionTime: defaults.submissionTime || formState.submissionTime,
  });
}

async function loadStandards() {
  standardLoading.value = true;
  try {
    const page = await getStandardPage(
      { glueBoardModel: currentGlueBoardModel.value || undefined, pageNo: 1, pageSize: 200 },
      'GLUE_BOARD_FAI',
    );
    const list = page.list || [];
    const availableList = list.filter(
      (item) => item.status === 1 && item.auditStatus === 20,
    );
    standards.value = availableList.length > 0 ? availableList : list;
  } finally {
    standardLoading.value = false;
  }
}

function handleStandardChange(standardId?: number) {
  formState.standardId = standardId;
  const standard = standards.value.find((item) => item.id === standardId);
  if (!standard) return;
  if (
    standard.processCode &&
    processCategoryOptions.some((item) => item.value === standard.processCode)
  ) {
    formState.processCategory = standard.processCode;
  }
}

async function handleGlueBoardModelChange() {
  if (selectedStandard.value?.glueBoardModel !== formState.glueBoardModel) {
    formState.standardId = undefined;
  }
  await loadStandards();
}

function validateForm() {
  if (!selectedStandard.value) {
    message.warning('请选择胶板检验标准');
    return false;
  }
  if (!formState.glueBoardModel?.trim()) {
    message.warning('请填写胶板型号');
    return false;
  }
  if (!formState.gluePlateBatchNo?.trim()) {
    message.warning('请填写胶板批次');
    return false;
  }
  if (!Number.isFinite(Number(formState.sampleLength || 0)) || Number(formState.sampleLength || 0) <= 0) {
    message.warning('请填写送检米数');
    return false;
  }
  if (!formState.processCategory) {
    message.warning('请选择工序');
    return false;
  }
  if (selectedStandard.value.glueBoardModel !== formState.glueBoardModel?.trim()) {
    message.warning('请选择与当前胶板型号一致的胶板检验标准');
    return false;
  }
  return true;
}

function resolveOperationCode() {
  return selectedStandard.value?.processCode || formState.processCategory!;
}

function resolveOperationName() {
  return (
    selectedStandard.value?.processName ||
    selectedProcessOption.value?.label ||
    formState.processCategory ||
    ''
  );
}

function buildPayload() {
  return {
    glueBoardModel: formState.glueBoardModel!.trim(),
    gluePlateBatchNo: formState.gluePlateBatchNo!.trim(),
    operationCode: resolveOperationCode(),
    operationName: resolveOperationName(),
    processCategory: formState.processCategory,
    remark: formState.remark?.trim(),
    sampleLength: Number(formState.sampleLength || 0),
    standardId: selectedStandard.value!.id,
    submissionTime: formState.submissionTime,
    submitterName: formState.submitterName?.trim(),
    workOrderNo: formState.workOrderNo?.trim() || formState.gluePlateBatchNo!.trim(),
  } as MesFaiApi.FaiRecord;
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!validateForm()) return;
    modalApi.lock();
    try {
      const record = await createGlueBoardFaiRecord(buildPayload());
      message.success('胶板检验单已新增');
      emit('success', record);
      await modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetForm();
      return;
    }
    resetForm();
    applyDefaults(modalApi.getData() as CreateModalDefaults);
    await loadStandards();
  },
});
</script>

<template>
  <Modal class="w-[760px]" title="新增胶板检验单">
    <Spin :spinning="standardLoading">
      <Form
        :model="formState"
        :label-col="{ style: { width: '100px' } }"
        layout="horizontal"
      >
        <div class="grid grid-cols-2 gap-x-4">
          <Form.Item class="col-span-2" label="检验标准" required>
            <Select
              v-model:value="formState.standardId"
              allow-clear
              show-search
              :filter-option="
                (input, option) =>
                  String(option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              :options="standardOptions"
              placeholder="请选择胶板检验标准"
              @change="handleStandardChange"
            />
          </Form.Item>
          <Form.Item label="胶板型号" required>
            <Select
              v-model:value="formState.glueBoardModel"
              allow-clear
              :options="GLUE_BOARD_MODEL_OPTIONS"
              placeholder="请选择胶板型号"
              @change="handleGlueBoardModelChange"
            />
          </Form.Item>
          <Form.Item label="胶板批次" required>
            <Input
              v-model:value="formState.gluePlateBatchNo"
              placeholder="请输入胶板批次"
            />
          </Form.Item>
          <Form.Item label="送检米数" required>
            <InputNumber
              v-model:value="formState.sampleLength"
              class="w-full"
              :min="0"
              :precision="3"
              placeholder="请输入送检米数"
            />
          </Form.Item>
          <Form.Item label="生产工单">
            <Input
              v-model:value="formState.workOrderNo"
              placeholder="可选；为空时按胶板批次生成"
            />
          </Form.Item>
          <Form.Item label="工序" required>
            <Select
              v-model:value="formState.processCategory"
              :options="processCategoryOptions"
              placeholder="请选择工序"
            />
          </Form.Item>
          <Form.Item label="送检时间">
            <DatePicker
              v-model:value="formState.submissionTime"
              class="w-full"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择送检时间"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </Form.Item>
          <Form.Item label="送检人员">
            <Input
              v-model:value="formState.submitterName"
              placeholder="请输入送检人员"
            />
          </Form.Item>
          <Form.Item class="col-span-2" label="备注">
            <Input v-model:value="formState.remark" placeholder="请输入备注" />
          </Form.Item>
        </div>
      </Form>
    </Spin>
  </Modal>
</template>
