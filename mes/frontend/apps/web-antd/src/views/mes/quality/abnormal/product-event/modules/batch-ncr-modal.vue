<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';
import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Alert,
  Modal as AntModal,
  Button,
  Checkbox,
  Input,
  InputNumber,
  message,
  Radio,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  batchCreateNcrFromProductEvents,
  getNcrReviewConfigSimpleList,
} from '#/api/mes/quality/abnormal/ncr';
import { getSimpleUserList } from '#/api/system/user';
import { DefectCodePicker } from '#/components/quality';

interface BatchRow {
  defectCode?: string;
  defectName?: string;
  defectQty?: number | string;
  inspectionId: number;
  inspectionType?: string;
  key: string;
  ncDescription?: string;
  operationName?: string;
  productBatchNo?: string;
  sourceType: string;
  specification?: string;
}

interface UserSelectOption {
  label: string;
  searchText: string;
  username: string;
  value: number;
}

defineOptions({ name: 'ProductEventBatchNcrModal' });

const emit = defineEmits<{
  success: [records: MesNcrApi.CreateFromProductEventResult[]];
}>();

const FINAL_APPROVER_UNIT_CODE = 'FINAL_APPROVER';

const ncLevelOptions = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'MAJOR' },
  { label: '严重', value: 'CRITICAL' },
];

const rows = ref<BatchRow[]>([]);
const selectedRowKeys = ref<string[]>([]);
const reviewConfigList = ref<MesNcrApi.ReviewConfig[]>([]);
const contentConfirmUserOptions = ref<UserSelectOption[]>([]);
const contentConfirmUserOptionsLoaded = ref(false);
const contentConfirmUserOptionsLoading = ref(false);
const batchDefectCode = ref<string>();
const batchDefectName = ref<string>();
const batchDefectPickerOpen = ref(false);
const formState = reactive({
  contentConfirmUserId: undefined as number | undefined,
  directSubmit: false,
  ncLevel: 'MAJOR',
  responsibleDeptNames: [] as string[],
});

const columns: TableColumnsType<BatchRow> = [
  { dataIndex: 'inspectionType', title: '检验类型', width: 130 },
  { dataIndex: 'operationName', title: '工序', width: 120 },
  { dataIndex: 'productBatchNo', title: '批次号', width: 160 },
  { dataIndex: 'specification', title: '规格', width: 180 },
  {
    dataIndex: 'defectCode',
    title: '缺陷代码',
    width: 340,
  },
  {
    dataIndex: 'defectName',
    title: '缺陷名称',
    width: 220,
  },
  {
    align: 'right',
    dataIndex: 'defectQty',
    title: '数量',
    width: 150,
  },
  {
    dataIndex: 'ncDescription',
    title: '不良描述',
    width: 360,
  },
];

const selectedCountText = computed(
  () => `已选择 ${rows.value.length} 条异常事件`,
);

const selectedBatchRows = computed(() =>
  rows.value.filter((row) => selectedRowKeys.value.includes(row.key)),
);

const responsibleDeptOptions = computed(() =>
  reviewConfigList.value
    .filter((item) => item.unitCode !== FINAL_APPROVER_UNIT_CODE)
    .map((item) => item.unitName)
    .filter(Boolean)
    .map((unitName) => ({ label: unitName!, value: unitName! })),
);

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: Array<number | string>) => {
    selectedRowKeys.value = keys.map(String);
  },
}));

const [Modal, modalApi] = useVbenModal({
  class: 'w-[1320px] max-w-[94vw]',
  closeOnClickModal: false,
  title: '批量生成NCR',
  async onConfirm() {
    if (!validateForm()) {
      return;
    }
    const confirmed = await confirmBatchCreate();
    if (!confirmed) {
      return;
    }
    modalApi.lock();
    try {
      const result = await batchCreateNcrFromProductEvents(buildPayload());
      message.success(buildSuccessMessage(result));
      emit('success', result);
      await modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      resetState();
      return;
    }
    const data =
      modalApi.getData<{
        rows?: MesProductAbnormalEventApi.ProductAbnormalEvent[];
      }>() || {};
    resetState();
    reviewConfigList.value = await getNcrReviewConfigSimpleList();
    void loadContentConfirmUserOptions();
    rows.value = (data.rows || [])
      .filter((item) => item.sourceType && item.inspectionId)
      .map((item) => ({
        defectQty: item.unqualifiedQty ?? undefined,
        inspectionId: item.inspectionId!,
        inspectionType: item.inspectionType,
        key: `${item.sourceType}:${item.inspectionId}`,
        ncDescription: item.abnormalSummary || undefined,
        operationName: item.operationName,
        productBatchNo: item.productBatchNo,
        sourceType: item.sourceType!,
        specification: item.specification,
      }));
    modalApi.setState({ title: `批量生成NCR（${rows.value.length}条）` });
  },
});

function resetState() {
  rows.value = [];
  selectedRowKeys.value = [];
  batchDefectCode.value = undefined;
  batchDefectName.value = undefined;
  batchDefectPickerOpen.value = false;
  formState.contentConfirmUserId = undefined;
  formState.directSubmit = false;
  formState.ncLevel = 'MAJOR';
  formState.responsibleDeptNames = [];
}

function validateForm() {
  if (rows.value.length === 0) {
    message.warning('请先选择待生成 NCR 的产品异常事件');
    return false;
  }
  const invalidQty = rows.value.find(
    (item) =>
      item.defectQty !== undefined &&
      item.defectQty !== null &&
      item.defectQty !== '' &&
      Number(item.defectQty) <= 0,
  );
  if (invalidQty) {
    message.warning('请填写大于 0 的不合格数量');
    return false;
  }
  if (!formState.ncLevel) {
    message.warning('请选择不合格等级');
    return false;
  }
  if (formState.responsibleDeptNames.length === 0) {
    message.warning('请选择责任单位');
    return false;
  }
  if (formState.directSubmit) {
    if (!formState.contentConfirmUserId) {
      message.warning('直接提交前请选择再次确认人');
      return false;
    }
    const invalidLotNo = rows.value.find(
      (item) => !item.productBatchNo?.trim(),
    );
    if (invalidLotNo) {
      message.warning('直接提交前请确认批次号');
      return false;
    }
    const invalidDefectName = rows.value.find((item) =>
      isBlank(item.defectName),
    );
    if (invalidDefectName) {
      message.warning('直接提交前请为每条异常事件填写缺陷名称');
      return false;
    }
  }
  return true;
}

function buildPayload(): MesNcrApi.BatchCreateFromProductEventReq {
  return {
    contentConfirmUserId: formState.contentConfirmUserId,
    contentConfirmUserName: getContentConfirmUserName(),
    directSubmit: formState.directSubmit,
    items: rows.value.map((item) => ({
      defectCode: item.defectCode?.trim() || undefined,
      defectName: item.defectName?.trim() || undefined,
      defectQty: item.defectQty,
      inspectionId: item.inspectionId,
      ncDescription: item.ncDescription?.trim() || undefined,
      sourceType: item.sourceType,
    })),
    ncLevel: formState.ncLevel,
    remark: '从产品异常事件批量生成NCR',
    responsibleDeptNames: formState.responsibleDeptNames,
  };
}

function confirmBatchCreate() {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '取消',
      centered: true,
      content: formState.directSubmit
        ? `将生成 ${rows.value.length} 张 NCR，并立即提交到再次确认内容节点。提交后不能再按草稿方式修改，确认继续吗？`
        : `将生成 ${rows.value.length} 张 NCR 草稿并关联产品异常事件，确认继续吗？`,
      okText: formState.directSubmit ? '生成并提交' : '生成草稿',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: formState.directSubmit
        ? '确认生成并提交 NCR？'
        : '确认生成 NCR 草稿？',
    });
  });
}

async function loadContentConfirmUserOptions() {
  if (contentConfirmUserOptionsLoaded.value || contentConfirmUserOptionsLoading.value) {
    return;
  }
  contentConfirmUserOptionsLoading.value = true;
  try {
    const list = await getSimpleUserList();
    contentConfirmUserOptions.value = (list || [])
      .filter((user) => user.id)
      .map((user) => {
        const label = user.nickname || user.username || String(user.id);
        const username = user.username || '';
        return {
          label,
          searchText: `${label} ${username}`.toLowerCase(),
          username,
          value: user.id!,
        };
      });
    contentConfirmUserOptionsLoaded.value = true;
  } finally {
    contentConfirmUserOptionsLoading.value = false;
  }
}

function filterContentConfirmUserOption(input: string, option?: UserSelectOption) {
  const keyword = input.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return (option?.searchText || '').includes(keyword);
}

function getContentConfirmUserName() {
  if (!formState.contentConfirmUserId) {
    return undefined;
  }
  return contentConfirmUserOptions.value.find(
    (item) => String(item.value) === String(formState.contentConfirmUserId),
  )?.label;
}

function buildSuccessMessage(result: MesNcrApi.CreateFromProductEventResult[]) {
  const createdCount = result.filter((item) => !item.existed).length;
  const existedCount = result.length - createdCount;
  if (formState.directSubmit) {
    return existedCount > 0
      ? `新生成 ${createdCount} 张 NCR 并已提交再次确认，${existedCount} 张已存在仅关联返回`
      : `已生成并提交再次确认 ${createdCount} 张 NCR`;
  }
  return existedCount > 0
    ? `新生成 ${createdCount} 张 NCR 草稿，${existedCount} 张已存在`
    : `已生成 ${createdCount} 张 NCR 草稿`;
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function isBlank(value?: unknown) {
  return value === undefined || value === null || String(value).trim() === '';
}

function getCellValue(record: BatchRow, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex as keyof BatchRow];
}

function handleRowDefectChange(row: BatchRow, value?: string) {
  row.defectCode = value;
}

function handleRowDefectSelect(
  row: BatchRow,
  defect: { code?: string; level?: string; name?: string },
) {
  row.defectCode = defect.code || row.defectCode;
  row.defectName = defect.name;
  if (!formState.ncLevel && defect.level) {
    formState.ncLevel = defect.level;
  }
}

function openBatchDefectPicker() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选需要批量设置缺陷的异常事件');
    return;
  }
  blurActiveElement();
  batchDefectCode.value = undefined;
  batchDefectName.value = undefined;
  batchDefectPickerOpen.value = true;
}

function blurActiveElement() {
  if (typeof document === 'undefined') {
    return;
  }
  (document.activeElement as HTMLElement | null)?.blur?.();
}

function handleBatchDefectChange(value?: string) {
  batchDefectCode.value = value;
}

function handleBatchDefectSelect(defect: {
  code?: string;
  level?: string;
  name?: string;
}) {
  batchDefectCode.value = defect.code || batchDefectCode.value;
  batchDefectName.value = defect.name;
  if (!formState.ncLevel && defect.level) {
    formState.ncLevel = defect.level;
  }
}

function applyBatchDefect() {
  const defectName = batchDefectName.value?.trim();
  if (!defectName) {
    message.warning('请填写缺陷名称');
    return;
  }
  selectedBatchRows.value.forEach((row) => {
    row.defectCode = batchDefectCode.value?.trim() || undefined;
    row.defectName = defectName;
  });
  message.success(`已设置 ${selectedBatchRows.value.length} 条缺陷`);
  batchDefectPickerOpen.value = false;
}
</script>

<template>
  <Modal>
    <div class="product-event-batch-ncr">
      <Alert
        :message="selectedCountText"
        description="上方逐条确认数量、不良描述和缺陷名称；下方统一选择品质确认的不合格等级和责任单位。默认生成 NCR 草稿，勾选后可生成并直接提交。"
        show-icon
        type="info"
      />

      <section class="batch-section">
        <div class="batch-section-title">
          <span>异常事件明细</span>
          <Button size="small" type="primary" @click="openBatchDefectPicker">
            批量设置缺陷
          </Button>
        </div>
        <Table
          bordered
          :columns="columns"
          :data-source="rows"
          :pagination="false"
          row-key="key"
          :row-selection="rowSelection"
          :scroll="{ x: 1660, y: 300 }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'inspectionType'">
              <Tag color="blue" class="!m-0">
                {{ displayValue(record.inspectionType) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'defectQty'">
              <InputNumber
                v-model:value="record.defectQty"
                class="w-full"
                :min="0.000001"
                :precision="6"
              />
            </template>
            <template v-else-if="column.dataIndex === 'defectCode'">
              <div class="batch-defect-cell">
                <DefectCodePicker
                  v-model:value="record.defectCode"
                  :dropdown-width="420"
                  :list-height="360"
                  :modal-z-index="5400"
                  placeholder="选择缺陷代码（可选）"
                  popup-class-name="batch-defect-row-dropdown"
                  @change="(value) => handleRowDefectChange(record, value)"
                  @select="(defect) => handleRowDefectSelect(record, defect)"
                />
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'defectName'">
              <Input
                v-model:value="record.defectName"
                placeholder="请输入缺陷名称"
              />
            </template>
            <template v-else-if="column.dataIndex === 'ncDescription'">
              <Input.TextArea
                v-model:value="record.ncDescription"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                placeholder="请输入不良描述"
              />
            </template>
            <template v-else>
              {{ displayValue(getCellValue(record, column.dataIndex)) }}
            </template>
          </template>
        </Table>
      </section>

      <section class="quality-section">
        <div class="quality-row">
          <div class="quality-label">不合格等级</div>
          <Radio.Group v-model:value="formState.ncLevel">
            <Radio
              v-for="item in ncLevelOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </Radio>
          </Radio.Group>
        </div>
        <div class="quality-row">
          <div class="quality-label">责任单位</div>
          <Checkbox.Group v-model:value="formState.responsibleDeptNames">
            <Checkbox
              v-for="item in responsibleDeptOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </Checkbox>
          </Checkbox.Group>
        </div>
        <div class="quality-row">
          <div class="quality-label">提交方式</div>
          <div class="direct-submit-option">
            <Checkbox v-model:checked="formState.directSubmit">
              生成后直接提交
            </Checkbox>
            <span>直接提交会进入再次确认内容节点。</span>
          </div>
        </div>
        <div v-if="formState.directSubmit" class="quality-row">
          <div class="quality-label">再次确认人</div>
          <Select
            v-model:value="formState.contentConfirmUserId"
            class="content-confirm-user-select"
            :filter-option="filterContentConfirmUserOption"
            :loading="contentConfirmUserOptionsLoading"
            :options="contentConfirmUserOptions"
            placeholder="请选择再次确认人"
            show-search
          >
            <template #option="{ label, username }">
              <div class="content-confirm-user-option">
                <span class="content-confirm-user-option__name">
                  {{ label }}
                </span>
                <span class="content-confirm-user-option__code">
                  {{ username || '-' }}
                </span>
              </div>
            </template>
          </Select>
        </div>
      </section>

      <AntModal
        v-model:open="batchDefectPickerOpen"
        centered
        destroy-on-close
        title="批量设置缺陷"
        :width="620"
        :z-index="5600"
        @ok="applyBatchDefect"
      >
        <div class="batch-defect-modal">
          <Alert
            :message="`将为 ${selectedBatchRows.length} 条异常事件设置同一个缺陷`"
            show-icon
            type="info"
          />
          <DefectCodePicker
            v-model:value="batchDefectCode"
            :dropdown-width="480"
            :list-height="360"
            :modal-width="860"
            :modal-z-index="5900"
            modal-title="选择批量设置的缺陷代码"
            placeholder="选择缺陷代码（可选）"
            popup-class-name="batch-defect-modal-dropdown"
            @change="handleBatchDefectChange"
            @select="handleBatchDefectSelect"
          />
          <Input v-model:value="batchDefectName" placeholder="请输入缺陷名称" />
        </div>
      </AntModal>
    </div>
  </Modal>
</template>

<style scoped>
.product-event-batch-ncr {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
}

.batch-section,
.quality-section {
  border: 1px solid #cbd5e1;
  background: #fff;
}

.batch-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 36px;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.batch-defect-cell {
  min-width: 300px;
}

.batch-defect-modal {
  display: grid;
  gap: 12px;
}

.quality-section {
  display: flex;
  flex-direction: column;
}

.quality-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  min-height: 44px;
  border-bottom: 1px solid #e2e8f0;
}

.quality-row:last-child {
  border-bottom: 0;
}

.quality-label {
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e2e8f0;
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 700;
}

.quality-row :deep(.ant-radio-group),
.quality-row :deep(.ant-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 18px;
  padding: 8px 14px;
}

.quality-row :deep(.ant-radio-wrapper),
.quality-row :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
}

.direct-submit-option {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding: 8px 14px;
  color: #64748b;
}

.content-confirm-user-select {
  width: 320px;
  margin: 6px 14px;
}

.content-confirm-user-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.content-confirm-user-option__name {
  color: #0f172a;
  font-weight: 600;
}

.content-confirm-user-option__code {
  color: #94a3b8;
}

:global(.batch-defect-row-dropdown.ant-select-dropdown) {
  z-index: 5400;
}

:global(.batch-defect-modal-dropdown.ant-select-dropdown) {
  z-index: 5900;
}
</style>
