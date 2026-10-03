<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesQmsYieldTargetConfigApi } from '#/api/mes/quality/statistics/yield-target-config';

import { computed, onMounted, reactive, ref } from 'vue';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  createYieldTargetConfig,
  deleteYieldTargetConfig,
  exportYieldTargetConfigExcel,
  getYieldTargetConfig,
  getYieldTargetConfigPage,
  updateYieldTargetConfig,
} from '#/api/mes/quality/statistics/yield-target-config';

defineOptions({ name: 'MesQmsYieldTargetConfig' });

type TargetConfig = MesQmsYieldTargetConfigApi.TargetConfig;

const processOptions = [
  { label: '配料', targetType: 'OUTPUT_QTY', unit: 'kg', value: 'FORMULA' },
  { label: '湿法', targetType: 'OUTPUT_QTY', unit: 'm', value: 'WET' },
];

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '禁用', value: 1 },
];

const targetTypeOptions = [
  { label: '产出值绝对值', value: 'OUTPUT_QTY' },
  { label: '良品率百分比', value: 'YIELD_RATE' },
];
const fixedFormulaProcessCodes = new Set(['FORMULA', 'WET']);

const columns: TableColumnsType<TargetConfig> = [
  { dataIndex: 'modelCode', fixed: 'left', title: '产品型号', width: 110 },
  { dataIndex: 'segmentCount', title: '配置口径', width: 120 },
  { dataIndex: 'processName', fixed: 'left', title: '工序', width: 120 },
  { dataIndex: 'processCode', title: '工序编码', width: 150 },
  { dataIndex: 'targetType', title: '目标类型', width: 140 },
  { dataIndex: 'targetQualifiedQty', title: '理论产量', width: 120 },
  { dataIndex: 'measureUnit', title: '计量单位', width: 100 },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'sort', title: '排序', width: 90 },
  { dataIndex: 'remark', title: '备注', width: 220 },
  { dataIndex: 'action', fixed: 'right', title: '操作', width: 150 },
];

const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const modalTitle = computed(() =>
  formState.id ? '编辑基础理论产量' : '新增基础理论产量',
);

const query = reactive({
  modelCode: '',
  pageNo: 1,
  pageSize: 20,
  processCode: undefined as string | undefined,
  status: undefined as number | undefined,
});

const rows = ref<TargetConfig[]>([]);
const total = ref(0);

const formState = reactive<TargetConfig>({
  measureUnit: 'kg',
  modelCode: '',
  processCode: 'FORMULA',
  processName: '配料',
  remark: '',
  segmentCount: 0,
  sort: 0,
  status: 0,
  targetType: 'OUTPUT_QTY',
  targetQualifiedQty: 0,
});

onMounted(() => {
  loadPage();
});

async function loadPage() {
  loading.value = true;
  try {
    const page = await getYieldTargetConfigPage(cleanParams(query));
    rows.value = page.list || [];
    total.value = Number(page.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  query.pageNo = 1;
  loadPage();
}

function handleReset() {
  query.modelCode = '';
  query.processCode = undefined;
  query.status = undefined;
  query.pageNo = 1;
  loadPage();
}

function handlePageChange(pageNo: number, pageSize: number) {
  query.pageNo = pageNo;
  query.pageSize = pageSize;
  loadPage();
}

function openCreate() {
  Object.assign(formState, {
    id: undefined,
    measureUnit: 'kg',
    modelCode: '',
    processCode: 'FORMULA',
    processName: '配料',
    remark: '',
    segmentCount: 0,
    sort: 0,
    status: 0,
    targetType: 'OUTPUT_QTY',
    targetQualifiedQty: 0,
  });
  modalOpen.value = true;
}

async function openEdit(row: TargetConfig) {
  if (!row.id) return;
  const detail = await getYieldTargetConfig(row.id);
  Object.assign(formState, detail);
  modalOpen.value = true;
}

async function handleSave() {
  const modelCode = formState.modelCode?.trim();
  if (!modelCode) {
    message.warning('请输入产品型号');
    return;
  }
  if (!formState.processCode) {
    message.warning('请选择工序');
    return;
  }
  if (!formState.measureUnit?.trim()) {
    message.warning('请输入计量单位');
    return;
  }
  if (!formState.targetType) {
    message.warning('请选择目标类型');
    return;
  }
  if (!fixedFormulaProcessCodes.has(String(formState.processCode || ''))) {
    message.warning('理论产量只需维护配料和湿法，后续工序由母卷统计公式自动计算');
    return;
  }
  if (Number(formState.targetQualifiedQty || 0) <= 0) {
    message.warning('理论产量必须大于0');
    return;
  }
  saving.value = true;
  try {
    const processOption = processOptions.find(
      (item) => item.value === formState.processCode,
    );
    const payload = {
      ...formState,
      measureUnit: processOption?.unit || formState.measureUnit,
      modelCode,
      processName: processOption?.label || formState.processName,
      segmentCount: 0,
      targetType: processOption?.targetType || formState.targetType,
    };
    if (payload.id) {
      await updateYieldTargetConfig(payload);
      message.success('目标配置已更新');
    } else {
      await createYieldTargetConfig(payload);
      message.success('目标配置已新增');
    }
    modalOpen.value = false;
    await loadPage();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: TargetConfig) {
  if (!row.id) return;
  await deleteYieldTargetConfig(row.id);
  message.success('目标配置已删除');
  await loadPage();
}

async function handleExport() {
  const data = await exportYieldTargetConfigExcel(cleanParams(query));
  downloadFileFromBlobPart({ fileName: '基础理论产量配置.xls', source: data });
}

function handleProcessChange(value: string) {
  const option = processOptions.find((item) => item.value === value);
  formState.processName = option?.label || value;
  formState.measureUnit = option?.unit || formState.measureUnit;
  formState.targetType = option?.targetType || formState.targetType;
}

function getStatusLabel(status?: number) {
  return status === 0 ? '启用' : '禁用';
}

function getTargetTypeLabel(targetType?: string) {
  return targetType === 'YIELD_RATE' ? '良品率百分比' : '产出值绝对值';
}

function getSegmentCountLabel(row: TargetConfig) {
  return fixedFormulaProcessCodes.has(String(row.processCode || ''))
    ? '基础配置'
    : '公式自动计算';
}

function getSegmentCountColor(row: TargetConfig) {
  return fixedFormulaProcessCodes.has(String(row.processCode || '')) ? 'green' : 'blue';
}

function cleanParams<T extends Record<string, any>>(params: T) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== '' && value !== undefined && value !== null,
    ),
  ) as T;
}
</script>

<template>
  <Page auto-content-height class="target-config-page">
    <div class="target-toolbar">
      <Space wrap>
        <Input
          v-model:value="query.modelCode"
          allow-clear
          class="target-filter"
          placeholder="产品型号"
        />
        <Select
          v-model:value="query.processCode"
          allow-clear
          :options="processOptions"
          class="target-filter"
          placeholder="工序"
        />
        <Select
          v-model:value="query.status"
          allow-clear
          :options="statusOptions"
          class="target-filter"
          placeholder="状态"
        />
        <Button type="primary" :loading="loading" @click="handleQuery">
          <template #icon><IconifyIcon icon="lucide:search" /></template>
          查询
        </Button>
        <Button @click="handleReset">
          <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
          重置
        </Button>
        <Button type="primary" @click="openCreate">
          <template #icon><IconifyIcon icon="lucide:plus" /></template>
          新增配置
        </Button>
        <Button @click="handleExport">
          <template #icon><IconifyIcon icon="lucide:download" /></template>
          导出
        </Button>
      </Space>
    </div>

    <div class="target-content">
      <section class="target-panel">
        <div class="target-panel__head">
          <IconifyIcon icon="lucide:table-2" />
          <span>产品型号-基础理论产量配置</span>
        </div>
        <Table
          bordered
          class="target-table"
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="{
            current: query.pageNo,
            pageSize: query.pageSize,
            pageSizeOptions: ['10', '20', '50', '100'],
            showSizeChanger: true,
            showTotal: (value: number) => `共 ${value} 条`,
            total,
          }"
          row-key="id"
          :scroll="{ x: 1410, y: 'calc(100vh - 360px)' }"
          size="small"
          @change="
            (pagination: any) =>
              handlePageChange(pagination.current, pagination.pageSize)
          "
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="record.status === 0 ? 'success' : 'default'">
                {{ getStatusLabel(record.status) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'segmentCount'">
              <Tag :color="getSegmentCountColor(record)">
                {{ getSegmentCountLabel(record) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'targetType'">
              <Tag
                :color="record.targetType === 'YIELD_RATE' ? 'blue' : 'cyan'"
              >
                {{ getTargetTypeLabel(record.targetType) }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'targetQualifiedQty'">
              <b>{{ record.targetQualifiedQty }}</b>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <Button size="small" type="link" @click="openEdit(record)">
                编辑
              </Button>
              <Popconfirm
                title="确认删除这条目标配置？"
                @confirm="handleDelete(record)"
              >
                <Button danger size="small" type="link">删除</Button>
              </Popconfirm>
            </template>
          </template>
        </Table>
      </section>
    </div>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      centered
      cancel-text="取消"
      destroy-on-close
      ok-text="保存"
      width="760px"
      wrap-class-name="target-config-erp-modal"
      @ok="handleSave"
    >
      <Form
        :label-col="{ flex: '88px' }"
        :model="formState"
        :wrapper-col="{ flex: 1 }"
        class="target-form"
        label-align="right"
      >
        <section class="target-form__section">
          <div class="target-form__section-head">
            <span class="target-form__section-mark"></span>
            <span>基础信息</span>
            <small>用于维护产品型号的配料、湿法理论产量</small>
          </div>
          <div class="target-form__grid">
            <Form.Item label="产品型号" required>
              <Input
                v-model:value="formState.modelCode"
                allow-clear
                placeholder="例如 W26、W33"
              />
            </Form.Item>
            <Form.Item label="工序" required>
              <Select
                v-model:value="formState.processCode"
                :options="processOptions"
                placeholder="请选择工序"
                @change="handleProcessChange"
              />
            </Form.Item>
            <Form.Item label="状态">
              <Select
                v-model:value="formState.status"
                :options="statusOptions"
                placeholder="请选择状态"
              />
            </Form.Item>
            <Form.Item label="排序">
              <InputNumber
                v-model:value="formState.sort"
                class="target-form__number"
                :min="0"
                :precision="0"
                placeholder="数值越小越靠前"
              />
            </Form.Item>
          </div>
        </section>

        <section class="target-form__section">
          <div class="target-form__section-head">
            <span class="target-form__section-mark"></span>
            <span>目标设置</span>
            <small>所有母卷产品仅配置配料、湿法；后段由母卷统计公式按分段数自动计算</small>
          </div>
          <div class="target-form__grid">
            <Form.Item label="目标类型" required>
              <Select
                v-model:value="formState.targetType"
                :options="targetTypeOptions"
                placeholder="请选择目标类型"
              />
            </Form.Item>
            <Form.Item label="理论产量" required>
              <InputNumber
                v-model:value="formState.targetQualifiedQty"
                class="target-form__number"
                :min="0"
                :precision="3"
                placeholder="请输入大于 0 的目标值"
              />
            </Form.Item>
            <Form.Item label="计量单位" required>
              <Input
                v-model:value="formState.measureUnit"
                allow-clear
                placeholder="kg / m / 片 / %"
              />
            </Form.Item>
            <Form.Item class="target-form__item--full" label="备注">
              <Input.TextArea
                v-model:value="formState.remark"
                :auto-size="{ minRows: 2, maxRows: 3 }"
                allow-clear
                placeholder="可填写目标口径或适用范围说明"
              />
            </Form.Item>
          </div>
        </section>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.target-config-page {
  height: 100%;
  background: #f6f8fb;
}

.target-toolbar {
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
  padding: 12px 16px;
}

.target-filter {
  width: 150px;
}

.target-content {
  height: 100%;
  min-height: 0;
  padding: 12px;
}

.target-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.target-panel__head {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #edf1f7;
  padding: 0 12px;
  color: #1e293b;
  font-weight: 600;
}

.target-table {
  flex: 1 1 auto;
  min-height: 0;
}

.target-table :deep(.ant-table-cell) {
  height: 38px;
  border-color: #e5edf6 !important;
  padding: 0 8px !important;
  text-align: center;
  white-space: nowrap;
}

.target-table :deep(.ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #475569;
  font-weight: 600;
  text-align: center !important;
}

.target-table :deep(.ant-table-cell-fix-left),
.target-table :deep(.ant-table-cell-fix-right) {
  background: #ffffff;
}

.target-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.target-form__section {
  overflow: hidden;
  border: 1px solid #d9e2ef;
  border-radius: 3px;
  background: #ffffff;
}

.target-form__section-head {
  display: flex;
  min-height: 36px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #d9e2ef;
  background: #f5f8fc;
  padding: 0 12px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
}

.target-form__section-head small {
  margin-left: auto;
  color: #8492a6;
  font-size: 12px;
  font-weight: 400;
}

.target-form__section-mark {
  width: 3px;
  height: 15px;
  border-radius: 1px;
  background: #1677ff;
}

.target-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
  padding: 14px 14px 2px;
}

.target-form__grid :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.target-form__grid :deep(.ant-form-item-label) {
  padding-right: 8px;
}

.target-form__grid :deep(.ant-form-item-label > label) {
  color: #465568;
}

.target-form__grid :deep(.ant-input),
.target-form__grid :deep(.ant-input-number),
.target-form__grid :deep(.ant-select-selector) {
  border-radius: 2px !important;
}

.target-form__grid :deep(.ant-input-number),
.target-form__grid :deep(.ant-select) {
  width: 100%;
}

.target-form__item--full {
  grid-column: 1 / -1;
}

.target-form__number {
  width: 100%;
}

:global(.target-config-erp-modal .ant-modal-content) {
  overflow: hidden;
  border: 1px solid #c9d5e5;
  border-radius: 4px;
  padding: 0;
  box-shadow: 0 12px 36px rgb(15 23 42 / 22%);
}

:global(.target-config-erp-modal .ant-modal-header) {
  margin-bottom: 0;
  border-bottom: 1px solid #d9e2ef;
  background: #f7f9fc;
  padding: 14px 18px;
}

:global(.target-config-erp-modal .ant-modal-title) {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

:global(.target-config-erp-modal .ant-modal-close) {
  top: 10px;
  right: 12px;
}

:global(.target-config-erp-modal .ant-modal-body) {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: 16px 18px 10px;
}

:global(.target-config-erp-modal .ant-modal-footer) {
  margin-top: 0;
  border-top: 1px solid #d9e2ef;
  background: #f7f9fc;
  padding: 10px 18px;
}

:global(.target-config-erp-modal .ant-modal-footer .ant-btn) {
  min-width: 72px;
  border-radius: 2px;
}

@media (max-width: 760px) {
  .target-form__grid {
    grid-template-columns: 1fr;
  }

  .target-form__item--full {
    grid-column: auto;
  }

  .target-form__section-head small {
    display: none;
  }
}
</style>
