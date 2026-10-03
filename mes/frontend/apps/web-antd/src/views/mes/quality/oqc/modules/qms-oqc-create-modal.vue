<script lang="ts" setup>
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';
import type { MesOqcApi } from '#/api/mes/quality/oqc';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { DatePicker, Form, Input, InputNumber, message, Select, Spin } from 'ant-design-vue';

import { getStandardPage } from '#/api/mes/quality/base/standard';
import { createOqcRecord } from '#/api/mes/quality/oqc';

defineOptions({ name: 'QmsOqcCreateModal' });

const emit = defineEmits<{
  success: [record: MesOqcApi.OqcRecord];
}>();

type FormState = {
  batchNo?: string;
  checkedBy?: string;
  customerCode?: string;
  inspectionDate?: string;
  productName?: string;
  remark?: string;
  shippingPieceQty?: number;
  standardId?: number;
};

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
  inspectionDate: formatLocalDateTime(new Date()),
});

const formState = reactive<FormState>(defaultFormState());
const standards = ref<MesQualityStandardApi.Standard[]>([]);
const standardLoading = ref(false);

const selectedStandard = computed(() =>
  standards.value.find((item) => item.id === formState.standardId),
);

const standardOptions = computed(() =>
  standards.value.map((item) => ({
    label: [
      item.standardNo,
      item.standardName,
      item.version ? `V${item.version}` : '',
      item.productModelName || item.productModelCode,
      item.materialName || item.materialCode,
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
    const page = await getStandardPage({ pageNo: 1, pageSize: 200 }, 'OQC');
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
  formState.productName =
    standard.productModelName ||
    standard.productModelCode ||
    standard.materialName ||
    standard.materialCode ||
    formState.productName;
}

function validateForm() {
  if (!selectedStandard.value) {
    message.warning('请选择 OQC 出货检查标准');
    return false;
  }
  if (!formState.productName?.trim()) {
    message.warning('请填写产品名称');
    return false;
  }
  if (!formState.batchNo?.trim()) {
    message.warning('请填写批次号');
    return false;
  }
  if (!formState.customerCode?.trim()) {
    message.warning('请填写客户代码/客户');
    return false;
  }
  return true;
}

function buildPayload(): MesOqcApi.OqcRecord {
  const standard = selectedStandard.value!;
  const batchNo = formState.batchNo!.trim();
  const customerCode = formState.customerCode!.trim();
  const productName = formState.productName!.trim();
  const pieceQty = formState.shippingPieceQty;
  return {
    oqcNo: '',
    shippingNo: batchNo,
    noticeNo: batchNo,
    customerCode,
    customerName: customerCode,
    materialId: standard.materialId,
    materialCode: standard.materialCode || '',
    materialName: productName,
    modelCode: standard.productModelCode || standard.productModelName,
    specification: standard.specification,
    batchNo,
    shippingQty: pieceQty ?? 0,
    shippingPieceQty: pieceQty,
    standardId: standard.id,
    status: 'PENDING',
    judgment: 'PENDING',
    inspectorName: formState.checkedBy?.trim(),
    inspectionTime: formState.inspectionDate,
    entryMode: 'MANUAL',
    entryLayout: 'PROGRAM_FORM',
    remark: formState.remark?.trim(),
  };
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!validateForm()) return;
    modalApi.lock();
    try {
      const record = await createOqcRecord(buildPayload());
      message.success('OQC 出货检验单已新增');
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
  <Modal class="w-[860px]" title="新增出货检验单 (OQC)">
    <Spin :spinning="standardLoading">
      <Form
        :model="formState"
        :label-col="{ style: { width: '132px' } }"
        layout="horizontal"
      >
        <div class="grid grid-cols-2 gap-x-4">
          <Form.Item class="col-span-2" label="OQC 标准" required>
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
              placeholder="请选择出货检验(OQC)标准"
              @change="handleStandardChange"
            />
          </Form.Item>

          <Form.Item label="产品名称" required>
            <Input
              v-model:value="formState.productName"
              placeholder="产品名称"
            />
          </Form.Item>
          <Form.Item label="批次号" required>
            <Input v-model:value="formState.batchNo" placeholder="批次号" />
          </Form.Item>
          <Form.Item label="客户代码/客户" required>
            <Input
              v-model:value="formState.customerCode"
              placeholder="客户代码/客户"
            />
          </Form.Item>
          <Form.Item label="日期">
            <DatePicker
              v-model:value="formState.inspectionDate"
              class="w-full"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="日期"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </Form.Item>
          <Form.Item label="片，总计">
            <InputNumber
              v-model:value="formState.shippingPieceQty"
              class="w-full"
              :min="0"
              placeholder="片，总计"
            />
          </Form.Item>
          <Form.Item label="最终判定">
            <Input value="待判定" disabled />
          </Form.Item>
          <Form.Item label="检验员">
            <Input v-model:value="formState.checkedBy" placeholder="检验员" />
          </Form.Item>
          <Form.Item class="col-span-2" label="备注/异常说明">
            <Input v-model:value="formState.remark" placeholder="备注/异常说明" />
          </Form.Item>
        </div>
      </Form>
    </Spin>
  </Modal>
</template>
