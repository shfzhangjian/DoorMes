<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';
import type { MesSupplierApi } from '#/api/mes/supplier';
import type { PickerOption } from '#/components/picker';
import type { UploadFile } from 'ant-design-vue';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  DatePicker,
  Form,
  Input,
  InputNumber,
  InputSearch,
  message,
  Select,
  Spin,
} from 'ant-design-vue';

import {
  createIqcRecord,
  getIqcStandardCandidatesByMaterial,
} from '#/api/mes/quality/iqc';
import { materialPickerConfig, PickerModal } from '#/components/picker';
import { FileUpload } from '#/components/upload';

import SupplierComboSelect from '../../../base/material/components/SupplierComboSelect.vue';

defineOptions({ name: 'QmsIqcCreateModal' });

const INSPECTION_ATTACHMENT_ACCEPTS = [
  'jpg',
  'jpeg',
  'png',
  'webp',
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'csv',
  'txt',
  'zip',
];

const emit = defineEmits<{
  success: [record: MesIqcApi.IqcRecord];
}>();

type FormState = {
  arrivalDate?: string;
  batchNo?: string;
  expiryDate?: string;
  inspectionApplyAttachmentUrls?: string[];
  inspectionApplyTime?: string;
  materialCode?: string;
  materialId?: number;
  materialName?: string;
  productionDate?: string;
  purchaseContractNo?: string;
  receiveQty?: number;
  receiverName?: string;
  remark?: string;
  specification?: string;
  supplierCode?: string;
  supplierId?: number;
  supplierName?: string;
  unit?: string;
};

function pad(value: number) {
  return `${value}`.padStart(2, '0');
}

function formatLocalDate(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function formatLocalDateTime(date: Date) {
  return `${formatLocalDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

const defaultFormState = (): FormState => ({
  arrivalDate: formatLocalDate(new Date()),
  inspectionApplyAttachmentUrls: [],
  inspectionApplyTime: formatLocalDateTime(new Date()),
  receiveQty: 1,
});

const formState = reactive<FormState>(defaultFormState());
const materialPickerOpen = ref(false);
const standardLoading = ref(false);
const selectedStandardId = ref<number>();
const standardCandidates = ref<MesIqcApi.StandardCandidate[]>([]);

function toNumber(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) ? num : undefined;
}

const selectedStandard = computed(() =>
  standardCandidates.value.find((item) => item.id === selectedStandardId.value),
);

function standardMatchModeLabel(value?: string) {
  return value === 'UNIVERSAL' ? '通用标准' : '物料专用';
}

const standardOptions = computed(() => {
  return standardCandidates.value.map((standard) => ({
    label: [
      standard.recommended ? '推荐' : '',
      standard.standardNo || `标准#${standard.id}`,
      standard.standardName,
      standard.version ? `V${standard.version}` : '',
      standardMatchModeLabel(standard.matchType),
      `${standard.itemCount ?? 0}项`,
    ]
      .filter(Boolean)
      .join(' / '),
    value: standard.id,
  }));
});

const standardSelectionHint = computed(() => {
  if (!formState.materialCode) return '选择物料后加载候选检验标准';
  if (standardLoading.value) return '正在加载候选检验标准';
  if (standardCandidates.value.length === 0) {
    return '未找到候选标准，创建后可在工作台选择';
  }
  if (!selectedStandard.value) return '请选择本次送检使用的检验标准';
  return (
    selectedStandard.value.matchReason ||
    `当前选择${standardMatchModeLabel(selectedStandard.value.matchType)}`
  );
});

function resetForm() {
  Object.keys(formState).forEach((key) => {
    delete formState[key as keyof FormState];
  });
  Object.assign(formState, defaultFormState());
  selectedStandardId.value = undefined;
  standardCandidates.value = [];
}

async function loadStandardCandidates(materialCode?: string) {
  selectedStandardId.value = undefined;
  standardCandidates.value = [];
  if (!materialCode) return;
  standardLoading.value = true;
  try {
    standardCandidates.value = await getIqcStandardCandidatesByMaterial({
      materialCode,
      materialId: formState.materialId,
    });
    const recommended = standardCandidates.value.find(
      (standard) => standard.recommended,
    );
    if (recommended) {
      selectedStandardId.value = recommended.id;
    } else if (standardCandidates.value.length === 1) {
      selectedStandardId.value = standardCandidates.value[0]!.id;
    }
  } catch {
    standardCandidates.value = [];
  } finally {
    standardLoading.value = false;
  }
}

async function handleMaterialPicked(option: PickerOption) {
  handleSupplierChange(null);
  selectedStandardId.value = undefined;
  standardCandidates.value = [];
  formState.materialId = toNumber(option.id);
  formState.materialCode = option.code;
  formState.materialName = option.name;
  formState.specification =
    option.extra?.specModel ||
    option.extra?.specification ||
    option.raw?.specModel;
  formState.unit = option.extra?.baseUom || option.raw?.baseUom;
  materialPickerOpen.value = false;
  await loadStandardCandidates(formState.materialCode);
}

function handleSupplierChange(supplier?: MesSupplierApi.Supplier | null) {
  if (!supplier) {
    formState.supplierId = undefined;
    formState.supplierCode = undefined;
    formState.supplierName = undefined;
    return;
  }
  formState.supplierId = supplier.id;
  formState.supplierCode = supplier.supplierCode;
  formState.supplierName = supplier.supplierName;
}

function handleAttachmentPreview(file: UploadFile) {
  const url = file.url || file.thumbUrl || file.preview;
  if (!url) {
    message.warning('当前附件尚未生成可访问地址');
    return;
  }
  const previewWindow = window.open(url, '_blank', 'noopener,noreferrer');
  if (previewWindow) previewWindow.opener = null;
}

function validateForm() {
  if (!formState.materialId || !formState.materialCode) {
    message.warning('请选择物料编码');
    return false;
  }
  if (!formState.materialName?.trim()) {
    message.warning('请填写物料名称');
    return false;
  }
  if (!formState.batchNo?.trim()) {
    message.warning('请填写批次号');
    return false;
  }
  if (
    formState.productionDate &&
    formState.expiryDate &&
    formState.expiryDate < formState.productionDate
  ) {
    message.warning('失效日期不能早于生产日期');
    return false;
  }
  if (!formState.receiveQty || formState.receiveQty <= 0) {
    message.warning('请填写大于0的数量');
    return false;
  }
  if (!formState.inspectionApplyTime) {
    message.warning('请选择报检时间');
    return false;
  }
  if (standardCandidates.value.length > 0 && !selectedStandardId.value) {
    message.warning('请选择本次送检使用的检验标准');
    return false;
  }
  if (!formState.supplierCode || !formState.supplierName) {
    message.warning('请选择供应商');
    return false;
  }
  return true;
}

function buildPayload(): MesIqcApi.IqcRecord {
  return {
    arrivalDate: formState.arrivalDate,
    batchNo: formState.batchNo!.trim(),
    expiryDate: formState.expiryDate,
    inspectionApplyAttachmentUrls:
      formState.inspectionApplyAttachmentUrls || [],
    inspectionApplyTime: formState.inspectionApplyTime,
    judgment: 'PENDING',
    materialCode: formState.materialCode!,
    materialId: formState.materialId,
    materialName: formState.materialName!.trim(),
    productionDate: formState.productionDate,
    purchaseContractNo: formState.purchaseContractNo,
    receiverName: formState.receiverName,
    receiveQty: formState.receiveQty!,
    remark: formState.remark,
    specification: formState.specification,
    standardId: selectedStandard.value?.id,
    standardName: selectedStandard.value?.standardName,
    standardNo: selectedStandard.value?.standardNo,
    standardVersion: selectedStandard.value?.version,
    status: 'PENDING',
    supplierId: formState.supplierId,
    supplierCode: formState.supplierCode,
    supplierName: formState.supplierName,
    unit: formState.unit,
  };
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!validateForm()) {
      return;
    }
    modalApi.lock();
    try {
      const record = await createIqcRecord(buildPayload());
      message.success('进料送检单已新增');
      emit('success', record);
      await modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetForm();
      return;
    }
    resetForm();
  },
});
</script>

<template>
  <Modal class="w-[860px]" title="新增送检">
    <Spin :spinning="standardLoading">
      <Form
        :model="formState"
        :label-col="{ style: { width: '112px' } }"
        layout="horizontal"
      >
        <div class="grid grid-cols-2 gap-x-4">
          <Form.Item class="col-span-2" label="物料编码" required>
            <InputSearch
              :value="formState.materialCode"
              placeholder="请选择物料编码"
              readonly
              enter-button="选择物料"
              @click="materialPickerOpen = true"
              @search="materialPickerOpen = true"
            />
          </Form.Item>

          <Form.Item class="col-span-2" label="匹配标准">
            <Select
              v-model:value="selectedStandardId"
              allow-clear
              :disabled="
                !formState.materialCode || standardCandidates.length === 0
              "
              :filter-option="true"
              :loading="standardLoading"
              :options="standardOptions"
              placeholder="请选择检验标准"
              show-search
            />
            <div class="mt-1 text-xs text-slate-500">
              {{ standardSelectionHint }}
            </div>
          </Form.Item>

          <Form.Item label="标准编号">
            <Input :value="selectedStandard?.standardNo" disabled />
          </Form.Item>
          <Form.Item label="标准版本">
            <Input :value="selectedStandard?.version" disabled />
          </Form.Item>
          <Form.Item label="物料名称" required>
            <Input
              v-model:value="formState.materialName"
              placeholder="选择物料后自动带出，可按送检单修正"
            />
          </Form.Item>
          <Form.Item label="规格型号">
            <Input
              v-model:value="formState.specification"
              placeholder="选择物料后自动带出"
            />
          </Form.Item>
          <Form.Item label="批次号" required>
            <Input
              v-model:value="formState.batchNo"
              placeholder="请输入来料批次号"
            />
          </Form.Item>
          <Form.Item label="来料日期">
            <DatePicker
              v-model:value="formState.arrivalDate"
              class="w-full"
              format="YYYY-MM-DD"
              placeholder="请选择来料日期"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="生产日期">
            <DatePicker
              v-model:value="formState.productionDate"
              class="w-full"
              format="YYYY-MM-DD"
              placeholder="请选择生产日期"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="失效日期">
            <DatePicker
              v-model:value="formState.expiryDate"
              class="w-full"
              format="YYYY-MM-DD"
              placeholder="请选择失效日期"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="报检时间" required>
            <DatePicker
              v-model:value="formState.inspectionApplyTime"
              class="w-full"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择报检时间"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </Form.Item>
          <Form.Item label="供应商" required>
            <SupplierComboSelect
              v-model:value="formState.supplierId"
              :default-label="formState.supplierName"
              :material-code="formState.materialCode"
              qualified-only
              @change="handleSupplierChange"
            />
          </Form.Item>
          <Form.Item label="收件人">
            <Input
              v-model:value="formState.receiverName"
              placeholder="请输入收件人"
            />
          </Form.Item>
          <Form.Item label="数量" required>
            <InputNumber
              v-model:value="formState.receiveQty"
              class="w-full"
              :min="0.0001"
              :precision="4"
              placeholder="请输入数量"
            />
          </Form.Item>
          <Form.Item label="单位">
            <Input v-model:value="formState.unit" placeholder="请输入单位" />
          </Form.Item>
          <Form.Item class="col-span-2" label="采购合同号">
            <Input
              v-model:value="formState.purchaseContractNo"
              placeholder="请输入采购合同号"
            />
          </Form.Item>
          <Form.Item class="col-span-2" label="送检附件">
            <FileUpload
              v-model="formState.inspectionApplyAttachmentUrls"
              :accept="INSPECTION_ATTACHMENT_ACCEPTS"
              directory="mes/qms/iqc/inspection-apply"
              help-text="支持图片、PDF、Word、Excel、CSV、TXT和ZIP；最多10个，单文件不超过20MB"
              list-type="picture"
              :max-number="10"
              :max-size="20"
              multiple
              show-description
              @preview="handleAttachmentPreview"
            />
          </Form.Item>
          <Form.Item class="col-span-2" label="备注">
            <Input v-model:value="formState.remark" placeholder="请输入备注" />
          </Form.Item>
        </div>
      </Form>
    </Spin>

    <PickerModal
      :config="materialPickerConfig"
      :open="materialPickerOpen"
      title="选择送检物料"
      @close="materialPickerOpen = false"
      @pick="handleMaterialPicked"
    />
  </Modal>
</template>
