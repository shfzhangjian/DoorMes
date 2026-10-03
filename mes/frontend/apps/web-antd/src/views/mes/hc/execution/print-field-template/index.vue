<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';
import type { MesHcPrintFieldTemplateApi } from '#/api/mes/hc/printfieldtemplate';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table as ATable,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';

import {
  createPrintFieldTemplate,
  deletePrintFieldTemplate,
  getPrintFieldTemplateDetail,
  getPrintFieldTemplatePage,
  updatePrintFieldTemplate,
} from '#/api/mes/hc/printfieldtemplate';

defineOptions({ name: 'MesHcExecutionPrintFieldTemplate' });

type TemplateRow = MesHcPrintFieldTemplateApi.PrintFieldTemplate;
type FieldRow = MesHcPrintFieldTemplateApi.PrintFieldTemplateItem & { _rowKey: string };

const processOptions = [
  { label: '耗材管理（各工序共用）', value: 'CONSUMABLE' },
  { label: '配料', value: 'FORMULA' },
  { label: '湿法', value: 'WET' },
  { label: '磨皮', value: 'ROUGH_GRINDING' },
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '分切', value: 'SLITTING' },
  { label: '压槽', value: 'PRESS_SLOT' },
  { label: '粘胶2', value: 'ADHESIVE2' },
  { label: '裁切', value: 'CUT_ROUND' },
  { label: '成品包装', value: 'PACKAGING' },
];

const documentTypeOptions = [
  { label: '退库入库单', value: 'CONSUMABLE_RETURN_LABEL' },
  { label: '工艺流转单', value: 'TRANSFER_TICKET' },
  { label: '检验流转单', value: 'INSPECTION_TICKET' },
  { label: '首检送检单', value: 'FAI_TICKET' },
  { label: '成品包装二维码', value: 'PACKAGE_QR' },
  { label: '片号标签', value: 'PIECE_LABEL' },
];

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 },
];

const formatTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '日期', value: 'DATE' },
  { label: '数值', value: 'NUMBER' },
  { label: '小数', value: 'DECIMAL' },
];

const sampleContext: Record<string, string> = {
  batchNo: 'W26F059AP',
  boxNo: 'BX20260618001',
  currentProcessName: '分切',
  documentNo: '20260618-001',
  equipmentName: '1#设备',
  expiryDate: '2027-04-17',
  glueBoardNo: 'GB-W26F059AP',
  inspectionNo: 'FAI20260618001',
  materialCode: '03.13.10055',
  materialName: 'CMP软垫',
  model: 'W33P0300',
  modelCode: 'W33P0300',
  operatorName: '张三',
  packageNo: 'PK20260618001',
  planNo: '20260618-001',
  processName: '分切',
  productBatchNo: 'W26F059A',
  reportTime: '2026-06-18 10:30:00',
  segmentBatchNo: 'W26F059AP',
  sliceNo: 'W26F059AP001',
  submitTime: '2026-06-18 10:30:00',
  taskNo: '20260618-001',
};

const consumableReturnSampleContext: Record<string, string> = {
  ledgerId: '1024',
  processName: '磨皮',
  consumableTypeName: '砂纸',
  erpMaterialCode: 'HC-HC-001',
  model: 'P120',
  batchNo: 'HC20260923001',
  returnQty: '12.5',
  uom: '米',
  returnAuthUserName: '张三',
  returnAuthTime: '2026-09-23 10:30:00',
  returnReason: '本批生产结束，剩余耗材退库',
};

const queryForm = reactive({
  documentType: undefined as string | undefined,
  processCode: undefined as string | undefined,
  status: undefined as number | undefined,
  templateName: '',
});

const loading = ref(false);
const saving = ref(false);
const rows = ref<TemplateRow[]>([]);
const selectedTemplateId = ref<number>();
const fields = ref<FieldRow[]>([]);
const fieldSeed = ref(1);
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const formState = reactive<TemplateRow>({
  documentName: '',
  documentType: 'TRANSFER_TICKET',
  processCode: '',
  processName: '',
  status: 0,
  templateCode: '',
  templateName: '',
  usageScene: '',
});

const selectedTemplate = computed(() => rows.value.find((item) => item.id === selectedTemplateId.value));
const isCreateMode = computed(() => !formState.id);
const previewFields = computed(() =>
  fields.value
    .filter((item) => item.visible !== false)
    .slice()
    .sort((a, b) => (a.sort || 0) - (b.sort || 0)),
);
const previewTitle = computed(() => formState.documentName || '工艺流转单');

const templateColumns = [
  { dataIndex: 'templateName', title: '模板名称', minWidth: 190 },
  { dataIndex: 'processName', title: '工序', width: 90 },
  { dataIndex: 'documentName', title: '单据', width: 130 },
  { dataIndex: 'status', title: '状态', width: 76 },
];

const fieldColumns = [
  { dataIndex: 'sort', title: '顺序', width: 74 },
  { dataIndex: 'visible', title: '显示', width: 74 },
  { dataIndex: 'fieldLabel', title: '显示名称', width: 150 },
  { dataIndex: 'valueKey', title: '取值字段', width: 170 },
  { dataIndex: 'fieldKey', title: '字段编码', width: 160 },
  { dataIndex: 'defaultValue', title: '空值显示', width: 130 },
  { dataIndex: 'suffix', title: '后缀', width: 90 },
  { dataIndex: 'formatType', title: '格式', width: 110 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 150 },
];

function nextRowKey() {
  return `print-field-${Date.now()}-${++fieldSeed.value}`;
}

function normalizeField(row?: Partial<FieldRow>, index = fields.value.length): FieldRow {
  return {
    defaultValue: '',
    fieldKey: '',
    fieldLabel: '',
    formatType: 'TEXT',
    sort: index + 1,
    suffix: '',
    valueKey: '',
    visible: true,
    ...(row || {}),
    _rowKey: row?._rowKey || nextRowKey(),
  };
}

function resetForm() {
  Object.assign(formState, {
    documentName: '',
    documentType: 'TRANSFER_TICKET',
    id: undefined,
    processCode: '',
    processName: '',
    remark: '',
    status: 0,
    templateCode: '',
    templateName: '',
    usageScene: '',
  });
  fields.value = [];
}

function getStatusMeta(status?: number) {
  if (status === 1) return { color: 'default', text: '停用' };
  return { color: 'green', text: '启用' };
}

function getTemplateRowClassName(record: TemplateRow) {
  return record.id === selectedTemplateId.value ? 'is-selected-row' : '';
}

function buildTemplateRowEvents(record: TemplateRow) {
  return {
    onClick: () => selectTemplate(record),
  };
}

async function loadData(keepSelection = false) {
  loading.value = true;
  try {
    const result = await getPrintFieldTemplatePage({
      documentType: queryForm.documentType,
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      processCode: queryForm.processCode,
      status: queryForm.status,
      templateName: queryForm.templateName,
    });
    rows.value = result.list || [];
    pagination.total = result.total || 0;
    if (!keepSelection || !rows.value.some((item) => item.id === selectedTemplateId.value)) {
      const first = rows.value[0];
      if (first?.id) {
        await selectTemplate(first);
      } else {
        selectedTemplateId.value = undefined;
        resetForm();
      }
    }
  } finally {
    loading.value = false;
  }
}

async function selectTemplate(row: TemplateRow) {
  if (!row.id) return;
  selectedTemplateId.value = row.id;
  const detail = await getPrintFieldTemplateDetail(row.id);
  Object.assign(formState, {
    documentName: detail.documentName,
    documentType: detail.documentType,
    id: detail.id,
    processCode: detail.processCode,
    processName: detail.processName,
    remark: detail.remark,
    status: detail.status ?? 0,
    templateCode: detail.templateCode,
    templateName: detail.templateName,
    usageScene: detail.usageScene,
  });
  fields.value = (detail.items || []).map((item, index) => normalizeField(item, index));
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current || 1;
  pagination.pageSize = nextPagination.pageSize || 10;
  void loadData(true);
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  queryForm.documentType = undefined;
  queryForm.processCode = undefined;
  queryForm.status = undefined;
  queryForm.templateName = '';
  handleSearch();
}

function handleCreate() {
  selectedTemplateId.value = undefined;
  resetForm();
  addFieldRow();
}

function addFieldRow() {
  fields.value.push(normalizeField());
  resequenceFields();
}

function copyFieldRow(index: number) {
  const source = fields.value[index];
  if (!source) return;
  fields.value.splice(index + 1, 0, normalizeField({ ...source, id: undefined, _rowKey: undefined }, index + 1));
  resequenceFields();
}

function removeFieldRow(index: number) {
  fields.value.splice(index, 1);
  resequenceFields();
}

function moveFieldRow(index: number, direction: 'down' | 'up') {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= fields.value.length) return;
  const nextRows = [...fields.value];
  const current = nextRows[index];
  nextRows[index] = nextRows[targetIndex]!;
  nextRows[targetIndex] = current!;
  fields.value = nextRows;
  resequenceFields();
}

function resequenceFields() {
  fields.value.forEach((item, index) => {
    item.sort = index + 1;
  });
}

function applyProcessName() {
  const option = processOptions.find((item) => item.value === formState.processCode);
  if (option) formState.processName = option.label;
}

function applyDocumentName() {
  const option = documentTypeOptions.find((item) => item.value === formState.documentType);
  if (option && !formState.documentName) formState.documentName = option.label;
}

function validateBeforeSave() {
  if (!formState.templateCode?.trim()) {
    message.warning('请填写模板编码');
    return false;
  }
  if (!formState.templateName?.trim()) {
    message.warning('请填写模板名称');
    return false;
  }
  if (!formState.processCode?.trim() || !formState.processName?.trim()) {
    message.warning('请选择工序');
    return false;
  }
  if (!formState.documentType?.trim() || !formState.documentName?.trim()) {
    message.warning('请选择单据类型并填写单据名称');
    return false;
  }
  const invalidIndex = fields.value.findIndex(
    (item) => !item.fieldKey?.trim() || !item.fieldLabel?.trim() || !item.valueKey?.trim(),
  );
  if (invalidIndex >= 0) {
    message.warning(`字段明细第 ${invalidIndex + 1} 行请完善显示名称、取值字段和字段编码`);
    return false;
  }
  const fieldKeys = new Set<string>();
  const duplicateIndex = fields.value.findIndex((item) => {
    const fieldKey = item.fieldKey?.trim();
    if (!fieldKey) return false;
    if (fieldKeys.has(fieldKey)) return true;
    fieldKeys.add(fieldKey);
    return false;
  });
  if (duplicateIndex >= 0) {
    message.warning(`字段明细第 ${duplicateIndex + 1} 行字段编码重复，请调整后再保存`);
    return false;
  }
  return true;
}

async function handleSave() {
  if (!validateBeforeSave()) return;
  saving.value = true;
  try {
    const payload: TemplateRow = {
      ...formState,
      items: fields.value.map(({ _rowKey, ...rest }, index) => ({
        ...rest,
        sort: index + 1,
        visible: rest.visible !== false,
      })),
    };
    if (formState.id) {
      await updatePrintFieldTemplate(payload);
    } else {
      const id = await createPrintFieldTemplate(payload);
      selectedTemplateId.value = id;
    }
    message.success('保存成功');
    await loadData(true);
  } finally {
    saving.value = false;
  }
}

function handleDelete() {
  if (!formState.id) {
    message.warning('请选择要删除的模板');
    return;
  }
  Modal.confirm({
    content: `删除后，模板 ${formState.templateName} 不再可用于集中打印字段配置。`,
    onOk: async () => {
      await deletePrintFieldTemplate(formState.id as number);
      message.success('删除成功');
      selectedTemplateId.value = undefined;
      await loadData();
    },
    title: '确认删除打印字段模板',
  });
}

function resolvePreviewValue(row: FieldRow) {
  const context = formState.documentType === 'CONSUMABLE_RETURN_LABEL'
    ? consumableReturnSampleContext : sampleContext;
  const value = row.valueKey ? context[row.valueKey] : undefined;
  const normalized = value || row.defaultValue || '-';
  return `${normalized}${row.suffix || ''}`;
}

void loadData();
</script>

<template>
  <Page auto-content-height>
    <div class="print-field-page">
      <div class="print-field-filter">
        <Form class="print-field-filter-form" layout="vertical">
          <FormItem label="工序">
            <Select
              v-model:value="queryForm.processCode"
              allow-clear
              :options="processOptions"
              placeholder="全部"
            />
          </FormItem>
          <FormItem label="单据类型">
            <Select
              v-model:value="queryForm.documentType"
              allow-clear
              :options="documentTypeOptions"
              placeholder="全部"
            />
          </FormItem>
          <FormItem label="状态">
            <Select
              v-model:value="queryForm.status"
              allow-clear
              :options="statusOptions"
              placeholder="全部"
            />
          </FormItem>
          <FormItem label="模板名称">
            <Input v-model:value="queryForm.templateName" allow-clear placeholder="输入模板名称" />
          </FormItem>
          <FormItem class="print-field-filter-actions">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </FormItem>
        </Form>
      </div>

      <div class="print-field-body">
        <section class="template-list">
          <div class="section-toolbar">
            <div>
              <strong>打印字段模板</strong>
              <span>共 {{ pagination.total }} 套</span>
            </div>
            <Space>
              <Tooltip title="刷新">
                <Button size="small" @click="loadData(true)">
                  <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
                </Button>
              </Tooltip>
              <Button size="small" type="primary" @click="handleCreate">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                新增
              </Button>
            </Space>
          </div>
          <ATable
            bordered
            class="template-table"
            :columns="templateColumns"
            :data-source="rows"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            :row-class-name="getTemplateRowClassName"
            :scroll="{ x: 520, y: 'calc(100vh - 330px)' }"
            size="small"
            @change="handleTableChange"
            :custom-row="buildTemplateRowEvents"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'templateName'">
                <div class="template-name-cell">
                  <strong>{{ record.templateName }}</strong>
                  <span>{{ record.templateCode }}</span>
                </div>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <Tag :color="getStatusMeta(record.status).color">
                  {{ getStatusMeta(record.status).text }}
                </Tag>
              </template>
            </template>
          </ATable>
        </section>

        <section class="template-detail">
          <div class="section-toolbar">
            <div>
              <strong>{{ isCreateMode ? '新增模板' : selectedTemplate?.templateName || '模板详情' }}</strong>
              <span>只配置左侧打印字段内容</span>
            </div>
            <Space>
              <Button :disabled="!formState.id" danger @click="handleDelete">删除</Button>
              <Button :loading="saving" type="primary" @click="handleSave">保存</Button>
            </Space>
          </div>

          <div class="detail-grid">
            <div class="template-form-panel">
              <Form
                class="template-form"
                :label-col="{ style: { width: '84px' } }"
                :model="formState"
                size="small"
              >
                <FormItem label="模板编码" required>
                  <Input
                    v-model:value="formState.templateCode"
                    :disabled="!isCreateMode"
                    placeholder="如 SLITTING_TRANSFER"
                  />
                </FormItem>
                <FormItem label="模板名称" required>
                  <Input v-model:value="formState.templateName" placeholder="如 分切工艺流转单" />
                </FormItem>
                <FormItem label="工序" required>
                  <Select
                    v-model:value="formState.processCode"
                    :options="processOptions"
                    placeholder="请选择"
                    @change="applyProcessName"
                  />
                </FormItem>
                <FormItem label="工序名称" required>
                  <Input v-model:value="formState.processName" placeholder="自动带入，也可维护" />
                </FormItem>
                <FormItem label="单据类型" required>
                  <Select
                    v-model:value="formState.documentType"
                    :options="documentTypeOptions"
                    placeholder="请选择"
                    @change="applyDocumentName"
                  />
                </FormItem>
                <FormItem label="单据名称" required>
                  <Input v-model:value="formState.documentName" placeholder="打印标题" />
                </FormItem>
                <FormItem label="状态">
                  <Select v-model:value="formState.status" :options="statusOptions" />
                </FormItem>
                <FormItem label="场景说明">
                  <Input v-model:value="formState.usageScene" placeholder="说明哪个按钮/场景使用" />
                </FormItem>
                <FormItem class="template-form-remark" label="备注">
                  <Input v-model:value="formState.remark" placeholder="内部备注" />
                </FormItem>
              </Form>
            </div>

            <div class="label-preview-panel">
              <div class="preview-head">
                <strong>打印预览</strong>
                <span>固定标签样式，仅预览左侧字段</span>
              </div>
              <div class="label-preview">
                <div class="label-left">
                  <h3>{{ previewTitle }}</h3>
                  <div class="preview-line" />
                  <div class="preview-fields">
                    <div v-for="item in previewFields" :key="item._rowKey" class="preview-row">
                      <span>{{ item.fieldLabel }}</span>
                      <strong>{{ resolvePreviewValue(item) }}</strong>
                    </div>
                  </div>
                </div>
                <div class="label-right">
                  <div class="preview-code">{{ sampleContext.planNo }}</div>
                  <div class="preview-qr">QR</div>
                  <div class="preview-batch">{{ sampleContext.sliceNo }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="fields-panel">
            <div class="fields-toolbar">
              <div>
                <strong>字段明细</strong>
                <span>按顺序生成打印 payload.fields</span>
              </div>
              <Button size="small" type="primary" @click="addFieldRow">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                新增字段
              </Button>
            </div>
            <ATable
              bordered
              class="field-table"
              :columns="fieldColumns"
              :data-source="fields"
              :pagination="false"
              row-key="_rowKey"
              :scroll="{ x: 1200, y: 'calc(100vh - 640px)' }"
              size="small"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.dataIndex === 'sort'">
                  <InputNumber v-model:value="record.sort" :min="1" :precision="0" class="full-input" />
                </template>
                <template v-else-if="column.dataIndex === 'visible'">
                  <Switch v-model:checked="record.visible" checked-children="是" un-checked-children="否" />
                </template>
                <template v-else-if="column.dataIndex === 'fieldLabel'">
                  <Input v-model:value="record.fieldLabel" placeholder="如 料号" />
                </template>
                <template v-else-if="column.dataIndex === 'valueKey'">
                  <Input v-model:value="record.valueKey" placeholder="如 materialCode" />
                </template>
                <template v-else-if="column.dataIndex === 'fieldKey'">
                  <Input v-model:value="record.fieldKey" placeholder="如 materialCode" />
                </template>
                <template v-else-if="column.dataIndex === 'defaultValue'">
                  <Input v-model:value="record.defaultValue" placeholder="如 -" />
                </template>
                <template v-else-if="column.dataIndex === 'suffix'">
                  <Input v-model:value="record.suffix" placeholder="后缀" />
                </template>
                <template v-else-if="column.dataIndex === 'formatType'">
                  <Select v-model:value="record.formatType" :options="formatTypeOptions" />
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space :size="2">
                    <Tooltip title="复制">
                      <Button type="text" size="small" @click="copyFieldRow(index)">
                        <template #icon><IconifyIcon icon="lucide:copy" /></template>
                      </Button>
                    </Tooltip>
                    <Tooltip title="上移">
                      <Button type="text" size="small" @click="moveFieldRow(index, 'up')">
                        <template #icon><IconifyIcon icon="lucide:arrow-up" /></template>
                      </Button>
                    </Tooltip>
                    <Tooltip title="下移">
                      <Button type="text" size="small" @click="moveFieldRow(index, 'down')">
                        <template #icon><IconifyIcon icon="lucide:arrow-down" /></template>
                      </Button>
                    </Tooltip>
                    <Tooltip title="删除">
                      <Button danger type="text" size="small" @click="removeFieldRow(index)">
                        <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                      </Button>
                    </Tooltip>
                  </Space>
                </template>
              </template>
            </ATable>
          </div>
        </section>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.print-field-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
}

.print-field-filter,
.template-list,
.template-detail {
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.print-field-filter {
  flex: 0 0 auto;
  padding: 10px 12px 0;
}

.print-field-filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
  gap: 10px;
  align-items: end;
}

.print-field-filter-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}

.print-field-filter-form :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.print-field-filter-actions {
  min-width: 132px;
}

.print-field-body {
  display: grid;
  flex: 1 1 auto;
  grid-template-columns: minmax(420px, 35%) minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
}

.template-list,
.template-detail {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.section-toolbar,
.fields-toolbar,
.preview-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 42px;
  padding: 8px 10px;
  border-bottom: 1px solid #e5edf7;
  background: #f8fafc;
}

.section-toolbar strong,
.fields-toolbar strong,
.preview-head strong {
  color: #1f2937;
  font-size: 14px;
}

.section-toolbar span,
.fields-toolbar span,
.preview-head span {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.template-table {
  flex: 1 1 auto;
  min-height: 0;
}

.template-table :deep(.ant-table-row) {
  cursor: pointer;
}

.template-table :deep(.is-selected-row td) {
  background: #eaf4ff !important;
}

.template-name-cell {
  display: grid;
  gap: 2px;
}

.template-name-cell strong {
  color: #0f172a;
}

.template-name-cell span {
  color: #64748b;
  font-size: 12px;
}

.detail-grid {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 10px;
  padding: 10px;
  border-bottom: 1px solid #e5edf7;
}

.template-form-panel,
.label-preview-panel,
.fields-panel {
  min-width: 0;
}

.template-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 10px;
}

.template-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.template-form-remark {
  grid-column: 1 / -1;
}

.label-preview-panel {
  border: 1px solid #d9e2ef;
  border-radius: 4px;
  overflow: hidden;
}

.preview-head {
  min-height: 34px;
  padding: 6px 8px;
}

.label-preview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 10px;
  margin: 10px;
  padding: 10px;
  border: 1px solid #111827;
  background: #fff;
  color: #111827;
}

.label-left {
  min-width: 0;
  border-right: 1px solid #111827;
  padding-right: 10px;
}

.label-left h3 {
  margin: 0;
  overflow: hidden;
  font-size: 20px;
  font-weight: 800;
  line-height: 26px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-line {
  height: 1px;
  margin: 4px 0 8px;
  background: #111827;
}

.preview-fields {
  display: grid;
  gap: 5px;
}

.preview-row {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 8px;
  font-size: 13px;
  line-height: 18px;
}

.preview-row span {
  font-weight: 700;
}

.preview-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-right {
  display: grid;
  grid-template-rows: 24px 92px 24px;
  gap: 6px;
  align-items: center;
  justify-items: center;
  min-width: 0;
}

.preview-code,
.preview-batch {
  overflow: hidden;
  max-width: 100%;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-qr {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92px;
  height: 92px;
  border: 2px solid #111827;
  background:
    linear-gradient(90deg, #111827 8px, transparent 8px) 0 0 / 16px 16px,
    linear-gradient(#111827 8px, transparent 8px) 0 0 / 16px 16px,
    #fff;
  color: #111827;
  font-size: 18px;
  font-weight: 900;
}

.fields-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.field-table {
  flex: 1 1 auto;
  min-height: 0;
}

.field-table :deep(.ant-input),
.field-table :deep(.ant-input-number),
.field-table :deep(.ant-select-selector) {
  border-radius: 3px;
}

.full-input {
  width: 100%;
}

@media (max-width: 1280px) {
  .print-field-body,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .print-field-filter-form,
  .template-form {
    grid-template-columns: 1fr;
  }
}
</style>
