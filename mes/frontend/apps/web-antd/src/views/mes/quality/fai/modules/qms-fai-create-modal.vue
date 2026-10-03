<script lang="ts" setup>
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  DatePicker,
  Form,
  Input,
  InputNumber,
  message,
  Select,
  Spin,
} from 'ant-design-vue';

import { getStandardPage } from '#/api/mes/quality/base/standard';
import { createFaiRecord } from '#/api/mes/quality/fai';

import {
  processCategoryOptions,
  submissionTypeOptions,
} from '../data';

defineOptions({ name: 'QmsFaiCreateModal' });

const emit = defineEmits<{
  success: [record: MesFaiApi.FaiRecord];
}>();

type FormState = {
  gluePlateBatchNo?: string;
  inspectionQty?: number;
  processCategory?: string;
  productBatchNo?: string;
  productModel?: string;
  remark?: string;
  standardId?: number;
  submissionTime?: string;
  submissionType?: string;
  submitterName?: string;
  workOrderNo?: string;
};

const DEFAULT_MACHINE_CODE = 'MANUAL_CREATE';
const DEFAULT_TRIGGER_REASON: MesFaiApi.FaiRecord['triggerReason'] =
  'NEW_ORDER';

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
  inspectionQty: 1,
  submissionTime: formatLocalDateTime(new Date()),
  submissionType: 'MASS_SHIPMENT',
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

const standardOptions = computed(() =>
  standards.value.map((item) => ({
    label: [
      item.standardNo,
      item.standardName,
      item.version ? `V${item.version}` : '',
      item.productModelName || item.productModelCode,
      item.materialName || item.materialCode,
      item.processName || item.processCode,
    ]
      .filter(Boolean)
      .join(' / '),
    value: item.id!,
  })),
);

function resetForm() {
  Object.assign(formState, defaultFormState());
}

async function loadStandards() {
  standardLoading.value = true;
  try {
    const page = await getStandardPage({ pageNo: 1, pageSize: 200 }, 'FAI');
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
  if (!standard) {
    return;
  }
  formState.productModel =
    standard.productModelName ||
    standard.productModelCode ||
    standard.materialName ||
    standard.materialCode;
  if (
    standard.processCode &&
    processCategoryOptions.some((item) => item.value === standard.processCode)
  ) {
    formState.processCategory = standard.processCode;
  }
}

function validateForm() {
  if (!selectedStandard.value) {
    message.warning('请选择FAI检验标准');
    return false;
  }
  if (!formState.workOrderNo?.trim()) {
    message.warning('请填写生产工单号');
    return false;
  }
  if (!formState.productModel?.trim()) {
    message.warning('请填写产品型号');
    return false;
  }
  if (!formState.productBatchNo?.trim()) {
    message.warning('请填写产品批次');
    return false;
  }
  if (!formState.processCategory) {
    message.warning('请选择工序');
    return false;
  }
  if (!formState.submissionType) {
    message.warning('请选择送检类型');
    return false;
  }
  if (!formState.inspectionQty || Number(formState.inspectionQty) <= 0) {
    message.warning('请填写送检数量');
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
    ''
  );
}

function buildPayload(): MesFaiApi.FaiRecord {
  const standard = selectedStandard.value!;
  const productModel = formState.productModel!.trim();
  return {
    faiNo: '',
    workOrderNo: formState.workOrderNo!.trim(),
    operationCode: resolveOperationCode(),
    operationName: resolveOperationName(),
    machineCode: DEFAULT_MACHINE_CODE,
    materialId: standard.materialId,
    materialCode: standard.materialCode || '',
    materialName: standard.materialName || '',
    specification: standard.specification || '',
    productModel,
    productBatchNo: formState.productBatchNo!.trim(),
    gluePlateBatchNo: formState.gluePlateBatchNo?.trim(),
    inspectionQty: formState.inspectionQty,
    processCategory: formState.processCategory,
    submissionType: formState.submissionType,
    triggerReason: DEFAULT_TRIGGER_REASON,
    standardId: standard.id,
    judgment: 'PENDING',
    status: 'PENDING',
    submissionTime: formState.submissionTime,
    submitterName: formState.submitterName?.trim(),
    remark: formState.remark?.trim(),
  };
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!validateForm()) {
      return;
    }
    modalApi.lock();
    try {
      const record = await createFaiRecord(buildPayload());
      message.success('首检检验单已新增');
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
    await loadStandards();
  },
});
</script>

<template>
  <Modal class="w-[860px]" title="新增首检检验单">
    <Spin :spinning="standardLoading">
      <Form
        :model="formState"
        :label-col="{ style: { width: '110px' } }"
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
              placeholder="请选择FAI检验标准"
              @change="handleStandardChange"
            />
          </Form.Item>

          <Form.Item label="物料编码">
            <Input :value="selectedStandard?.materialCode" disabled />
          </Form.Item>
          <Form.Item label="物料名称">
            <Input :value="selectedStandard?.materialName" disabled />
          </Form.Item>
          <Form.Item label="规格型号">
            <Input :value="selectedStandard?.specification" disabled />
          </Form.Item>
          <Form.Item label="标准版本">
            <Input :value="selectedStandard?.version" disabled />
          </Form.Item>
          <Form.Item label="产品型号" required>
            <Input
              v-model:value="formState.productModel"
              placeholder="请输入产品型号"
            />
          </Form.Item>

          <Form.Item label="生产工单" required>
            <Input
              v-model:value="formState.workOrderNo"
              placeholder="请输入生产工单号"
            />
          </Form.Item>
          <Form.Item label="产品批次" required>
            <Input
              v-model:value="formState.productBatchNo"
              placeholder="请输入产品批次"
            />
          </Form.Item>
          <Form.Item label="工序" required>
            <Select
              v-model:value="formState.processCategory"
              :options="processCategoryOptions"
              placeholder="请选择工序"
            />
          </Form.Item>
          <Form.Item label="送检类型" required>
            <Select
              v-model:value="formState.submissionType"
              :options="submissionTypeOptions"
              placeholder="请选择送检类型"
            />
          </Form.Item>
          <Form.Item label="送检数量" required>
            <InputNumber
              v-model:value="formState.inspectionQty"
              class="w-full"
              :min="0.000001"
              :precision="6"
              placeholder="请输入送检数量"
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
          <Form.Item label="胶板批次">
            <Input
              v-model:value="formState.gluePlateBatchNo"
              placeholder="请输入胶板批次"
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
