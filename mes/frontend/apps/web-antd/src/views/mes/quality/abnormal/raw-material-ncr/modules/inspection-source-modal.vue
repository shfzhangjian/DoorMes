<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Alert,
  Modal as AntModal,
  Button,
  Checkbox,
  Form,
  Input,
  InputNumber,
  message,
  Radio,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import { getNcrReviewConfigSimpleList } from '#/api/mes/quality/abnormal/ncr';
import {
  batchCreateRawMaterialNcrFromInspections,
  getRawMaterialNcrSourceInspectionPage,
} from '#/api/mes/quality/abnormal/raw-material-ncr';
import { getSimpleUserList } from '#/api/system/user';
import { DefectCodePicker } from '#/components/quality';

type SourceRow = MesNcrApi.RawMaterialInspection & {
  defectCode?: string;
  defectName?: string;
  defectQty?: number | string;
  key: string;
  ncDescription?: string;
};

interface UserSelectOption {
  label: string;
  searchText: string;
  username: string;
  value: number;
}

defineOptions({ name: 'RawMaterialNcrInspectionSourceModal' });

const emit = defineEmits<{
  success: [records: MesNcrApi.CreateFromInspectionResult[]];
}>();

const FINAL_APPROVER_UNIT_CODE = 'FINAL_APPROVER';

const ncrStatusOptions = [
  { label: '待生成', value: 'PENDING' },
  { label: '已生成', value: 'GENERATED' },
  { label: '全部', value: 'ALL' },
];
const ncLevelOptions = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'MAJOR' },
  { label: '严重', value: 'CRITICAL' },
];

const loading = ref(false);
const rows = ref<SourceRow[]>([]);
const selectedRowKeys = ref<string[]>([]);
const reviewConfigList = ref<MesNcrApi.ReviewConfig[]>([]);
const contentConfirmUserOptions = ref<UserSelectOption[]>([]);
const contentConfirmUserOptionsLoaded = ref(false);
const contentConfirmUserOptionsLoading = ref(false);
const batchDefectCode = ref<string>();
const batchDefectName = ref<string>();
const batchDefectPickerOpen = ref(false);

const searchState = reactive({
  inspectionType: 'IQC',
  inspectionNo: '',
  supplierName: '',
  materialCode: '',
  materialName: '',
  lotNo: '',
  ncrStatus: 'PENDING',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const formState = reactive({
  contentConfirmUserId: undefined as number | undefined,
  directSubmit: false,
  ncLevel: 'MAJOR',
  responsibleDeptNames: [] as string[],
});

const columns: TableColumnsType<SourceRow> = [
  { dataIndex: 'inspectionTypeName', title: '检验类型', width: 130 },
  { dataIndex: 'inspectionNo', title: '检验单号', width: 170 },
  { dataIndex: 'supplierName', title: '供应商', width: 160 },
  { dataIndex: 'materialName', title: '物料', width: 220 },
  { dataIndex: 'lotNo', title: '批号', width: 150 },
  { align: 'right', dataIndex: 'quantity', title: '数量', width: 100 },
  { dataIndex: 'defectCode', title: '缺陷代码', width: 320 },
  { dataIndex: 'defectName', title: '缺陷名称', width: 220 },
  { align: 'right', dataIndex: 'defectQty', title: '不合格数量', width: 150 },
  { dataIndex: 'ncDescription', title: '不合格描述', width: 360 },
  { dataIndex: 'ncrStatus', title: 'NCR状态', width: 140 },
];

const selectedRows = computed(() =>
  rows.value.filter((row) => selectedRowKeys.value.includes(row.key)),
);

const selectedCountText = computed(
  () => `已选择 ${selectedRows.value.length} 条 IQC 不合格来源`,
);

const responsibleDeptOptions = computed(() =>
  reviewConfigList.value
    .filter((item) => item.unitCode !== FINAL_APPROVER_UNIT_CODE)
    .map((item) => item.unitName)
    .filter(Boolean)
    .map((unitName) => ({ label: unitName!, value: unitName! })),
);

const rowSelection = computed(() => ({
  getCheckboxProps: (record: SourceRow) => ({
    disabled: Boolean(record.ncrGenerated),
  }),
  onChange: (keys: Array<number | string>) => {
    selectedRowKeys.value = keys.map(String);
  },
  selectedRowKeys: selectedRowKeys.value,
}));

const [Modal, modalApi] = useVbenModal({
  class: 'w-[1440px] max-w-[96vw]',
  closeOnClickModal: false,
  title: '从检验生成原材料不合格处置单',
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
      const result =
        await batchCreateRawMaterialNcrFromInspections(buildPayload());
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
    resetState();
    reviewConfigList.value = await getNcrReviewConfigSimpleList();
    void loadContentConfirmUserOptions();
    await loadRows();
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
  pagination.current = 1;
  pagination.pageSize = 10;
  pagination.total = 0;
  searchState.inspectionType = 'IQC';
  searchState.inspectionNo = '';
  searchState.supplierName = '';
  searchState.materialCode = '';
  searchState.materialName = '';
  searchState.lotNo = '';
  searchState.ncrStatus = 'PENDING';
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await getRawMaterialNcrSourceInspectionPage({
      pageNo: Number(pagination.current || 1),
      pageSize: Number(pagination.pageSize || 10),
      ...searchState,
    });
    rows.value = (result.list || []).map((item) => ({
      ...item,
      defectQty: item.quantity || undefined,
      key: `${item.inspectionType}:${item.inspectionId}`,
      ncDescription: item.abnormalSummary || 'IQC检验判定不合格',
    }));
    pagination.total = result.total || 0;
    selectedRowKeys.value = selectedRowKeys.value.filter((key) =>
      rows.value.some((row) => row.key === key && !row.ncrGenerated),
    );
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadRows();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current || 1;
  pagination.pageSize = nextPagination.pageSize || 10;
  void loadRows();
}

function validateForm() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选待生成的 IQC 不合格来源');
    return false;
  }
  const inspectionTypes = new Set(
    selectedRows.value.map((item) => item.inspectionType).filter(Boolean),
  );
  if (inspectionTypes.size > 1) {
    message.warning('批量生成时不能选择不同检验类型');
    return false;
  }
  const invalidQty = selectedRows.value.find(
    (item) => Number(item.defectQty || 0) <= 0,
  );
  if (invalidQty) {
    message.warning('请填写大于 0 的不合格数量');
    return false;
  }
  const invalidDesc = selectedRows.value.find(
    (item) => !item.ncDescription?.trim(),
  );
  if (invalidDesc) {
    message.warning('请填写不合格描述');
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
    const invalidDefectName = selectedRows.value.find((item) =>
      isBlank(item.defectName),
    );
    if (invalidDefectName) {
      message.warning('直接提交前请为每条来源填写缺陷名称');
      return false;
    }
  }
  return true;
}

function buildPayload(): MesNcrApi.BatchCreateFromInspectionReq {
  const inspectionType = selectedRows.value[0]?.inspectionType || 'IQC';
  return {
    contentConfirmUserId: formState.contentConfirmUserId,
    contentConfirmUserName: getContentConfirmUserName(),
    directSubmit: formState.directSubmit,
    inspectionType,
    items: selectedRows.value.map((item) => ({
      defectCode: item.defectCode?.trim() || undefined,
      defectName: item.defectName?.trim() || undefined,
      defectQty: item.defectQty,
      inspectionId: Number(item.inspectionId),
      inspectionType: item.inspectionType,
      ncDescription: item.ncDescription?.trim(),
    })),
    ncLevel: formState.ncLevel,
    remark: '从IQC检验单批量生成原材料不合格处置单',
    responsibleDeptNames: formState.responsibleDeptNames,
  };
}

function confirmBatchCreate() {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '取消',
      centered: true,
      content: formState.directSubmit
        ? `将生成 ${selectedRows.value.length} 张原材料不合格处置单，并立即提交到再次确认内容节点。确认继续吗？`
        : `将生成 ${selectedRows.value.length} 张原材料不合格处置单草稿，确认继续吗？`,
      okText: formState.directSubmit ? '生成并提交' : '生成草稿',
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: formState.directSubmit ? '确认生成并提交？' : '确认生成草稿？',
    });
  });
}

async function loadContentConfirmUserOptions() {
  if (
    contentConfirmUserOptionsLoaded.value ||
    contentConfirmUserOptionsLoading.value
  ) {
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

function filterContentConfirmUserOption(
  input: string,
  option?: UserSelectOption,
) {
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

function buildSuccessMessage(result: MesNcrApi.CreateFromInspectionResult[]) {
  const createdCount = result.filter((item) => !item.existed).length;
  const existedCount = result.length - createdCount;
  if (formState.directSubmit) {
    return existedCount > 0
      ? `新生成 ${createdCount} 张并已提交再次确认，${existedCount} 张已存在`
      : `已生成并提交再次确认 ${createdCount} 张原材料不合格处置单`;
  }
  return existedCount > 0
    ? `新生成 ${createdCount} 张草稿，${existedCount} 张已存在`
    : `已生成 ${createdCount} 张原材料不合格处置单草稿`;
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

function getCellValue(record: SourceRow, dataIndex?: unknown) {
  if (typeof dataIndex !== 'string') {
    return undefined;
  }
  return record[dataIndex as keyof SourceRow];
}

function handleRowDefectChange(row: SourceRow, value?: string) {
  row.defectCode = value;
}

function handleRowDefectSelect(
  row: SourceRow,
  defect: { code?: string; level?: string; name?: string },
) {
  row.defectCode = defect.code || row.defectCode;
  row.defectName = defect.name;
  if (!formState.ncLevel && defect.level) {
    formState.ncLevel = defect.level;
  }
}

function openBatchDefectPicker() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要批量设置缺陷的来源');
    return;
  }
  batchDefectCode.value = undefined;
  batchDefectName.value = undefined;
  batchDefectPickerOpen.value = true;
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
  selectedRows.value.forEach((row) => {
    row.defectCode = batchDefectCode.value?.trim() || undefined;
    row.defectName = defectName;
  });
  message.success(`已设置 ${selectedRows.value.length} 条缺陷`);
  batchDefectPickerOpen.value = false;
}
</script>

<template>
  <Modal>
    <div class="raw-material-source-modal">
      <Alert
        :message="selectedCountText"
        description="先筛选 IQC 不合格来源；批量生成时只能选择同一检验类型。默认生成草稿，勾选后可生成并直接提交。"
        show-icon
        type="info"
      />

      <section class="source-section">
        <Form class="source-search" layout="inline">
          <Form.Item label="检验单号">
            <Input
              v-model:value="searchState.inspectionNo"
              allow-clear
              placeholder="IQC单号"
            />
          </Form.Item>
          <Form.Item label="供应商">
            <Input
              v-model:value="searchState.supplierName"
              allow-clear
              placeholder="供应商名称"
            />
          </Form.Item>
          <Form.Item label="物料编码">
            <Input
              v-model:value="searchState.materialCode"
              allow-clear
              placeholder="物料编码"
            />
          </Form.Item>
          <Form.Item label="批号">
            <Input
              v-model:value="searchState.lotNo"
              allow-clear
              placeholder="批号"
            />
          </Form.Item>
          <Form.Item label="NCR状态">
            <Select
              v-model:value="searchState.ncrStatus"
              :options="ncrStatusOptions"
              class="source-search__select"
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" @click="handleSearch">查询</Button>
          </Form.Item>
        </Form>

        <div class="source-section-title">
          <span>来源检验明细</span>
          <Button size="small" type="primary" @click="openBatchDefectPicker">
            批量设置缺陷
          </Button>
        </div>
        <Table
          bordered
          class="source-table"
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="pagination"
          row-key="key"
          :row-selection="rowSelection"
          :scroll="{ x: 2120, y: 390 }"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'inspectionTypeName'">
              <Tag color="blue" class="!m-0">
                {{ displayValue(record.inspectionTypeName) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'materialName'">
              <div class="leading-tight">
                <div class="font-bold">
                  {{ displayValue(record.materialName) }}
                </div>
                <div class="mt-1 text-xs text-slate-400">
                  {{ displayValue(record.materialCode) }} /
                  {{ displayValue(record.specification) }}
                </div>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'defectCode'">
              <DefectCodePicker
                v-model:value="record.defectCode"
                :disabled="record.ncrGenerated"
                :dropdown-width="420"
                :list-height="320"
                :modal-z-index="5400"
                placeholder="选择缺陷代码（可选）"
                popup-class-name="raw-material-source-defect-dropdown"
                @change="(value) => handleRowDefectChange(record, value)"
                @select="(defect) => handleRowDefectSelect(record, defect)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'defectName'">
              <Input
                v-model:value="record.defectName"
                :disabled="record.ncrGenerated"
                placeholder="请输入缺陷名称"
              />
            </template>
            <template v-else-if="column.dataIndex === 'defectQty'">
              <InputNumber
                v-model:value="record.defectQty"
                class="w-full"
                :disabled="record.ncrGenerated"
                :min="0.000001"
                :precision="6"
              />
            </template>
            <template v-else-if="column.dataIndex === 'ncDescription'">
              <Input.TextArea
                v-model:value="record.ncDescription"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                :disabled="record.ncrGenerated"
                placeholder="请输入不合格描述"
              />
            </template>
            <template v-else-if="column.dataIndex === 'ncrStatus'">
              <Tag
                :color="record.ncrGenerated ? 'success' : 'warning'"
                class="!m-0"
              >
                {{
                  record.ncrGenerated
                    ? `已生成 ${record.ncrNo || ''}`
                    : '待生成'
                }}
              </Tag>
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
            :message="`将为 ${selectedRows.length} 条来源设置同一个缺陷`"
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
            popup-class-name="raw-material-source-batch-defect-dropdown"
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
.raw-material-source-modal {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: min(760px, calc(100vh - 170px));
  min-height: 620px;
  padding: 16px;
  overflow: hidden;
  background: #f8fafc;
}

.source-section,
.quality-section {
  border: 1px solid #cbd5e1;
  background: #fff;
}

.source-section {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.source-search {
  display: flex;
  flex: 0 0 auto;
  gap: 8px 0;
  padding: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.source-search__select {
  width: 160px;
}

.source-section-title {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  color: #0f172a;
  font-weight: 800;
}

.source-table {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 12px 12px;
}

.source-table :deep(.ant-spin-nested-loading),
.source-table :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.source-table :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.source-table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.source-table :deep(.ant-table-content),
.source-table :deep(.ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.source-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  height: 100%;
  max-height: none !important;
}

.source-table :deep(.ant-pagination) {
  flex: 0 0 auto;
  margin: 10px 0 0 !important;
}

.quality-section {
  display: grid;
  flex: 0 0 auto;
  gap: 0;
}

.quality-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  min-height: 42px;
  border-top: 1px solid #e2e8f0;
}

.quality-row:first-child {
  border-top: 0;
}

.quality-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 12px;
  color: #334155;
  font-weight: 700;
  background: #f1f5f9;
  border-right: 1px solid #e2e8f0;
}

.quality-row > :last-child {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 8px 12px;
}

.direct-submit-option {
  gap: 12px;
  color: #64748b;
}

.content-confirm-user-select {
  width: min(360px, calc(100% - 24px));
}

.content-confirm-user-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.content-confirm-user-option__name {
  color: #0f172a;
  font-weight: 600;
}

.content-confirm-user-option__code {
  color: #94a3b8;
  font-size: 12px;
}

.batch-defect-modal {
  display: grid;
  gap: 12px;
}
</style>
