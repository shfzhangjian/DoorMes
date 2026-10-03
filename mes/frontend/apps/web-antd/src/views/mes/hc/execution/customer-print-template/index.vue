<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';
import type { MesHcCustomerPrintTemplateApi } from '#/api/mes/hc/customerprinttemplate';

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
  Pagination,
  Select,
  Space,
  Switch,
  Table as ATable,
  Tag,
  Tooltip,
  Upload,
  message,
} from 'ant-design-vue';

import { uploadFile } from '#/api/infra/file';
import {
  createCustomerPrintTemplate,
  deleteCustomerPrintTemplate,
  getCustomerPrintTemplateDetail,
  getCustomerPrintTemplateFieldOptions,
  getCustomerPrintTemplatePage,
  parseCustomerPrintTemplateVariables,
  updateCustomerPrintTemplate,
} from '#/api/mes/hc/customerprinttemplate';

defineOptions({ name: 'MesHcExecutionCustomerPrintTemplate' });

type TemplateRow = MesHcCustomerPrintTemplateApi.CustomerPrintTemplate;
type FieldOption = MesHcCustomerPrintTemplateApi.FieldOption;
type VarRow = MesHcCustomerPrintTemplateApi.CustomerPrintTemplateVar & { _rowKey: string };

const templateTypeOptions = [
  { label: '整包装', value: 'PACKAGE' },
  { label: '片', value: 'PIECE' },
];
const templateFormatOptions = [
  { label: 'ZPL', value: 'ZPL' },
  { label: 'NLBL', value: 'NLBL' },
];
const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 },
];
const scopeOptions = [
  { label: '头表字段', value: 'HEAD' },
  { label: '明细字段', value: 'DETAIL' },
  { label: '打印上下文', value: 'PRINT' },
  { label: '常量默认值', value: 'CONST' },
];

const queryForm = reactive({
  customerName: '',
  status: undefined as number | undefined,
  templateName: '',
  templateType: undefined as string | undefined,
});
const loading = ref(false);
const formVisible = ref(false);
const formLoading = ref(false);
const saving = ref(false);
const uploadLoading = ref(false);
const parsing = ref(false);
const rows = ref<TemplateRow[]>([]);
const fieldOptions = ref<FieldOption[]>([]);
const vars = ref<VarRow[]>([]);
const varSeed = ref(1);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const pageSizeOptions = ['10', '20', '50', '100'];
const formState = reactive<TemplateRow>({
  customerCode: '',
  customerName: '',
  status: 0,
  templateCode: '',
  templateContent: '',
  templateFormat: 'ZPL',
  templateName: '',
  templateType: 'PACKAGE',
});

const isCreateMode = computed(() => !formState.id);
const isNlblTemplate = computed(() => String(formState.templateFormat || 'ZPL').toUpperCase() === 'NLBL');
const modalTitle = computed(() => (isCreateMode.value ? '新增客户打印模板' : '编辑客户打印模板'));
const variableCountText = computed(() => `${vars.value.length} 个变量`);
const templateColumns = [
  { dataIndex: 'templateName', fixed: 'left', title: '模板名称', width: 220 },
  { dataIndex: 'templateCode', title: '模板编码', width: 180 },
  { dataIndex: 'customerName', title: '客户', width: 220 },
  { dataIndex: 'templateType', title: '类型', width: 100 },
  { dataIndex: 'templateFormat', title: '格式', width: 100 },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'fileName', title: '模板文件', minWidth: 220 },
  { dataIndex: 'createTime', title: '创建时间', width: 180 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 150 },
];
const variableColumns = [
  { dataIndex: 'sort', title: '顺序', width: 74 },
  { dataIndex: 'variableName', title: '模板字段', minWidth: 180 },
  { dataIndex: 'variableScope', title: '范围', width: 140 },
  { dataIndex: 'sourceField', title: '来源字段', width: 230 },
  { dataIndex: 'defaultValue', title: '默认值', width: 150 },
  { dataIndex: 'required', title: '必填', width: 76 },
  { dataIndex: 'variableExpr', title: '映射表达式', width: 200 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 76 },
];

function nextRowKey() {
  return `customer-print-var-${Date.now()}-${++varSeed.value}`;
}

function normalizeVar(row?: Partial<VarRow>, index = vars.value.length): VarRow {
  return {
    defaultValue: '',
    required: false,
    sort: index + 1,
    sourceField: '',
    sourceLabel: '',
    variableExpr: '',
    variableName: '',
    variableScope: 'CONST',
    ...(row || {}),
    _rowKey: row?._rowKey || nextRowKey(),
  };
}

function resetForm() {
  Object.assign(formState, {
    customerCode: '',
    customerName: '',
    fileName: '',
    fileSize: undefined,
    fileUrl: '',
    id: undefined,
    remark: '',
    status: 0,
    templateCode: '',
    templateContent: '',
    templateFormat: 'ZPL',
    templateName: '',
    templateType: 'PACKAGE',
  });
  vars.value = [];
}

function getTemplateTypeMeta(templateType?: string) {
  if (templateType === 'PIECE') return { color: 'purple', text: '片' };
  return { color: 'blue', text: '整包装' };
}

function getTemplateFormatMeta(templateFormat?: string) {
  if (String(templateFormat || '').toUpperCase() === 'NLBL') return { color: 'orange', text: 'NLBL' };
  return { color: 'geekblue', text: 'ZPL' };
}

function getStatusMeta(status?: number) {
  if (status === 1) return { color: 'default', text: '停用' };
  return { color: 'green', text: '启用' };
}

function formatBytes(size?: number) {
  const value = Number(size || 0);
  if (value <= 0) return '-';
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(2)} MB`;
}

async function loadFieldOptions() {
  fieldOptions.value = await getCustomerPrintTemplateFieldOptions();
}

async function ensureFieldOptionsLoaded() {
  if (!fieldOptions.value.length) {
    await loadFieldOptions();
  }
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getCustomerPrintTemplatePage({
      customerName: queryForm.customerName,
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      status: queryForm.status,
      templateName: queryForm.templateName,
      templateType: queryForm.templateType,
    });
    rows.value = result.list || [];
    pagination.total = result.total || 0;
  } finally {
    loading.value = false;
  }
}

function formatPaginationTotal(total: number, range: [number, number]) {
  if (!total) return '共 0 条';
  return `第 ${range[0]}-${range[1]} 条 / 共 ${total} 条`;
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page;
  pagination.pageSize = pageSize;
  void loadData();
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  queryForm.customerName = '';
  queryForm.status = undefined;
  queryForm.templateName = '';
  queryForm.templateType = undefined;
  handleSearch();
}

async function handleCreate() {
  resetForm();
  formVisible.value = true;
  formLoading.value = true;
  try {
    await ensureFieldOptionsLoaded();
    initializeVarsForTemplateType();
  } finally {
    formLoading.value = false;
  }
}

async function handleEdit(row: TemplateRow) {
  if (!row.id) return;
  resetForm();
  Object.assign(formState, {
    customerCode: row.customerCode,
    customerName: row.customerName,
    fileName: row.fileName,
    fileSize: row.fileSize,
    id: row.id,
    status: row.status ?? 0,
    templateCode: row.templateCode,
    templateFormat: row.templateFormat || 'ZPL',
    templateName: row.templateName,
    templateType: row.templateType || 'PACKAGE',
  });
  formVisible.value = true;
  formLoading.value = true;
  try {
    const [detail] = await Promise.all([getCustomerPrintTemplateDetail(row.id), ensureFieldOptionsLoaded()]);
    Object.assign(formState, {
      customerCode: detail.customerCode,
      customerName: detail.customerName,
      fileName: detail.fileName,
      fileSize: detail.fileSize,
      fileUrl: detail.fileUrl,
      id: detail.id,
      remark: detail.remark,
      status: detail.status ?? 0,
      templateCode: detail.templateCode,
      templateContent: detail.templateContent,
      templateFormat: detail.templateFormat || 'ZPL',
      templateName: detail.templateName,
      templateType: detail.templateType || 'PACKAGE',
    });
    initializeVarsForTemplateType(detail.vars || []);
  } finally {
    formLoading.value = false;
  }
}

function resequenceVars() {
  vars.value.forEach((item, index) => {
    item.sort = index + 1;
  });
}

function getSourceFieldOptions(scope?: string) {
  const normalizedScope = scope || 'CONST';
  if (normalizedScope === 'CONST') return [];
  return fieldOptions.value
    .filter((item) => item.scope === normalizedScope)
    .map((item) => ({
      label: `${item.sourceLabel} (${item.sourceField})`,
      value: item.sourceField,
    }));
}

function getFieldOption(scope?: string, sourceField?: string) {
  return fieldOptions.value.find((item) => item.scope === scope && item.sourceField === sourceField);
}

function getAllowedVariableScopes(templateType = formState.templateType) {
  return templateType === 'PIECE' ? ['DETAIL', 'HEAD', 'PRINT'] : ['HEAD', 'PRINT'];
}

function buildInitializedVars(templateType = formState.templateType) {
  return getAllowedVariableScopes(templateType).flatMap((scope) =>
    fieldOptions.value
      .filter((item) => item.scope === scope)
      .map((item) =>
        normalizeVar({
          required: false,
          sourceField: item.sourceField,
          sourceLabel: item.sourceLabel,
          variableExpr: item.variableExpr,
          variableName: '',
          variableScope: item.scope,
        }),
      ),
  );
}

function getMappingKey(row: Partial<VarRow>) {
  const scope = String(row.variableScope || '').trim().toUpperCase();
  const sourceField = String(row.sourceField || '').trim();
  if (!scope || !sourceField || scope === 'CONST') return '';
  return `${scope}:${sourceField}`;
}

function initializeVarsForTemplateType(sourceVars: MesHcCustomerPrintTemplateApi.CustomerPrintTemplateVar[] = vars.value) {
  const initializedRows = buildInitializedVars();
  const rowMap = new Map(initializedRows.map((item) => [getMappingKey(item), item]));
  const extraRows: VarRow[] = [];
  for (const sourceVar of sourceVars || []) {
    const row = normalizeVar(sourceVar);
    const key = getMappingKey(row);
    const initializedRow = key ? rowMap.get(key) : undefined;
    if (initializedRow) {
      Object.assign(initializedRow, {
        defaultValue: row.defaultValue,
        id: row.id,
        remark: row.remark,
        required: row.required,
        variableName: row.variableName,
      });
    } else if (row.variableName?.trim()) {
      extraRows.push(row);
    }
  }
  vars.value = [...initializedRows, ...extraRows];
  resequenceVars();
}

function handleVariableScopeChange(row: VarRow) {
  row.sourceField = '';
  row.sourceLabel = '';
  row.variableExpr = row.variableScope === 'CONST' ? `const.${row.variableName || ''}` : '';
}

function handleSourceFieldChange(row: VarRow) {
  const option = getFieldOption(row.variableScope, row.sourceField);
  row.sourceLabel = option?.sourceLabel || '';
  row.variableExpr = option?.variableExpr || '';
}

async function handleTemplateTypeChange(value: string) {
  formState.templateType = value;
  await ensureFieldOptionsLoaded();
  if (!isNlblTemplate.value && formState.templateContent?.trim()) {
    await parseCurrentTemplate({ silentEmpty: true });
    return;
  }
  initializeVarsForTemplateType();
}

function getDefaultVariableScope() {
  return formState.templateType === 'PIECE' ? 'DETAIL' : 'HEAD';
}

function handleAddVariable() {
  vars.value = [
    ...vars.value,
    normalizeVar({
      required: false,
      sort: vars.value.length + 1,
      variableScope: getDefaultVariableScope(),
    }),
  ];
}

function handleRemoveVariable(row: VarRow) {
  vars.value = vars.value.filter((item) => item._rowKey !== row._rowKey);
  resequenceVars();
}

async function handleTemplateFormatChange(value: string) {
  formState.templateFormat = value;
  await ensureFieldOptionsLoaded();
  if (value === 'NLBL') {
    formState.templateContent = '';
    initializeVarsForTemplateType();
    return;
  }
  await parseCurrentTemplate({ silentEmpty: true });
}

function readFileAsBase64(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener('error', () => reject(reader.error || new Error('文件读取失败')));
    reader.addEventListener('load', () => {
      const result = String(reader.result || '');
      resolve(result.includes(',') ? result.split(',')[1] || '' : result);
    });
    reader.readAsDataURL(file);
  });
}

async function parseCurrentTemplate(options: { silentEmpty?: boolean } = {}) {
  await ensureFieldOptionsLoaded();
  if (isNlblTemplate.value) {
    initializeVarsForTemplateType();
    message.info('NLBL 模板请手工维护变量映射');
    return;
  }
  if (!formState.templateContent?.trim()) {
    initializeVarsForTemplateType();
    if (!options.silentEmpty) {
      message.warning('请先上传或粘贴斑马模板内容');
    }
    return;
  }
  parsing.value = true;
  try {
    const result = await parseCustomerPrintTemplateVariables({
      templateContent: formState.templateContent,
      templateFormat: formState.templateFormat || 'ZPL',
      templateType: formState.templateType || 'PACKAGE',
    });
    initializeVarsForTemplateType(result || []);
    message.success(`解析完成，识别 ${result?.length || 0} 个变量，并已合并到字段清单`);
  } finally {
    parsing.value = false;
  }
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  const rawFile = file as File;
  const fileName = rawFile.name.toLowerCase();
  const isNlblFile = fileName.endsWith('.nlbl');
  const isZplFile = ['.zpl', '.txt', '.prn'].some((suffix) => fileName.endsWith(suffix));
  if (!isNlblFile && !isZplFile) {
    message.warning('请上传 .zpl、.txt、.prn 或 .nlbl 模板文件');
    return false;
  }
  uploadLoading.value = true;
  const hideLoading = message.loading({ content: isNlblFile ? '正在上传 NLBL 模板...' : '正在上传并解析模板...', duration: 0 });
  try {
    const [templateContent, uploadResult] = await Promise.all([
      isNlblFile ? readFileAsBase64(rawFile) : rawFile.text(),
      uploadFile({ directory: 'customer-print-template', file: rawFile }),
    ]);
    formState.fileName = rawFile.name;
    formState.fileSize = rawFile.size;
    formState.fileUrl = String((uploadResult as any)?.url || uploadResult || '');
    formState.templateFormat = isNlblFile ? 'NLBL' : 'ZPL';
    formState.templateContent = templateContent;
    if (isZplFile) {
      await parseCurrentTemplate();
    } else if (!vars.value.length) {
      message.success('NLBL 模板上传完成，请维护变量映射');
    }
  } catch (error: any) {
    message.error(error?.message || '模板上传失败');
  } finally {
    hideLoading();
    uploadLoading.value = false;
  }
  return false;
};

function validateBeforeSave() {
  if (!formState.templateCode?.trim()) {
    message.warning('请填写模板编码');
    return false;
  }
  if (!formState.templateName?.trim()) {
    message.warning('请填写模板名称');
    return false;
  }
  if (!formState.customerCode?.trim() || !formState.customerName?.trim()) {
    message.warning('请填写客户编号和客户名称');
    return false;
  }
  if (isNlblTemplate.value && !formState.fileUrl?.trim() && !formState.templateContent?.trim()) {
    message.warning('请上传 NLBL 模板文件');
    return false;
  }
  if (!isNlblTemplate.value && !formState.templateContent?.trim()) {
    message.warning('请上传或粘贴斑马模板内容');
    return false;
  }
  const filledVars = getFilledVariableRows();
  if (!filledVars.length) {
    message.warning('请至少填写一个模板字段');
    return false;
  }
  const variableNames = filledVars.map((item) => normalizeTemplateFieldName(item.variableName)).filter(Boolean);
  if (new Set(variableNames).size !== variableNames.length) {
    message.warning('变量名不能重复');
    return false;
  }
  const invalidIndex = filledVars.findIndex(
    (item) => item.variableScope !== 'CONST' && !item.sourceField?.trim(),
  );
  if (invalidIndex >= 0) {
    message.warning(`已填写模板字段的第 ${invalidIndex + 1} 行请选择来源字段`);
    return false;
  }
  return true;
}

function getFilledVariableRows() {
  return vars.value.filter((item) => !!normalizeTemplateFieldName(item.variableName));
}

function normalizeTemplateFieldName(value?: string) {
  return String(value || '')
    .trim()
    .replace(/^\{\{\s*/, '')
    .replace(/\s*}}$/, '')
    .trim();
}

async function handleSave() {
  if (!validateBeforeSave()) return;
  saving.value = true;
  try {
    const filledVars = getFilledVariableRows();
    const payload: TemplateRow = {
      ...formState,
      vars: filledVars.map(({ _rowKey, ...rest }, index) => ({
        ...rest,
        variableExpr:
          rest.variableScope === 'CONST' ? `const.${normalizeTemplateFieldName(rest.variableName)}` : rest.variableExpr,
        variableName: normalizeTemplateFieldName(rest.variableName),
        sort: index + 1,
      })),
    };
    if (formState.id) {
      await updateCustomerPrintTemplate(payload);
    } else {
      await createCustomerPrintTemplate(payload);
    }
    message.success('保存成功');
    formVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row: TemplateRow) {
  if (!row.id) {
    message.warning('请选择要删除的模板');
    return;
  }
  Modal.confirm({
    content: `删除后，模板 ${row.templateName} 将不再可用于发货包装外部标签打印。`,
    onOk: async () => {
      await deleteCustomerPrintTemplate(row.id as number);
      message.success('删除成功');
      await loadData();
    },
    title: '确认删除客户打印模板',
  });
}

void loadFieldOptions();
void loadData();
</script>

<template>
  <Page auto-content-height>
    <div class="customer-print-page">
      <div class="customer-print-filter">
        <Form class="customer-print-filter-form" layout="vertical">
          <FormItem label="模板类型">
            <Select v-model:value="queryForm.templateType" allow-clear :options="templateTypeOptions" placeholder="全部" />
          </FormItem>
          <FormItem label="模板名称">
            <Input v-model:value="queryForm.templateName" allow-clear placeholder="输入模板名称" />
          </FormItem>
          <FormItem label="客户名称">
            <Input v-model:value="queryForm.customerName" allow-clear placeholder="输入客户名称" />
          </FormItem>
          <FormItem label="状态">
            <Select v-model:value="queryForm.status" allow-clear :options="statusOptions" placeholder="全部" />
          </FormItem>
          <FormItem class="customer-print-filter-actions">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </FormItem>
        </Form>
      </div>

      <section class="template-list-panel">
        <div class="section-toolbar">
          <div>
            <strong>客户打印模板</strong>
            <span>共 {{ pagination.total }} 套</span>
          </div>
          <Space>
            <Tooltip title="刷新">
              <Button size="small" @click="loadData">
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
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1360, y: '100%' }"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'templateName'">
              <div class="template-name-cell">
                <strong>{{ record.templateName }}</strong>
                <span>{{ record.templateCode }}</span>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'customerName'">
              <div class="template-name-cell">
                <strong>{{ record.customerName }}</strong>
                <span>{{ record.customerCode }}</span>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'templateType'">
              <Tag :color="getTemplateTypeMeta(record.templateType).color">
                {{ getTemplateTypeMeta(record.templateType).text }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'templateFormat'">
              <Tag :color="getTemplateFormatMeta(record.templateFormat).color">
                {{ getTemplateFormatMeta(record.templateFormat).text }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <Tag :color="getStatusMeta(record.status).color">
                {{ getStatusMeta(record.status).text }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'fileName'">
              <div class="file-cell">
                <strong>{{ record.fileName || '-' }}</strong>
                <span v-if="record.fileSize">{{ formatBytes(record.fileSize) }}</span>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Space :size="2">
                <Button size="small" type="link" @click="handleEdit(record)">编辑</Button>
                <Button danger size="small" type="link" @click="handleDelete(record)">删除</Button>
              </Space>
            </template>
          </template>
        </ATable>

        <div class="template-pagination">
          <Pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :page-size-options="pageSizeOptions"
            show-quick-jumper
            show-size-changer
            :show-total="formatPaginationTotal"
            :total="pagination.total"
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </section>

      <Modal
        v-model:open="formVisible"
        :body-style="{ padding: 0 }"
        cancel-text="取消"
        :confirm-loading="saving"
        destroy-on-close
        ok-text="保存"
        :title="modalTitle"
        width="100vw"
        wrap-class-name="customer-print-fullscreen-modal"
        @ok="handleSave"
      >
        <div class="template-modal-body" :class="{ 'is-loading': formLoading }">
          <div class="modal-summary">
            <div>
              <strong>{{ formState.templateName || modalTitle }}</strong>
              <span>{{ variableCountText }}</span>
            </div>
            <Button v-if="!isNlblTemplate" :loading="parsing" size="small" @click="parseCurrentTemplate">
              <template #icon><IconifyIcon icon="lucide:scan-text" /></template>
              解析变量
            </Button>
            <Button v-else size="small" @click="handleAddVariable">
              <template #icon><IconifyIcon icon="lucide:plus" /></template>
              新增变量
            </Button>
          </div>

          <div class="modal-content-grid">
            <section class="modal-section">
              <div class="panel-title">
                <strong>模板信息</strong>
              </div>
              <Form class="template-form" :label-col="{ style: { width: '88px' } }" :model="formState" size="small">
                <FormItem label="模板编码" required>
                  <Input v-model:value="formState.templateCode" :disabled="!isCreateMode" placeholder="如 CUS_A_PACKAGE" />
                </FormItem>
                <FormItem label="模板名称" required>
                  <Input v-model:value="formState.templateName" placeholder="如 A客户整包装外标" />
                </FormItem>
                <FormItem label="客户编号" required>
                  <Input v-model:value="formState.customerCode" placeholder="手工录入客户编号" />
                </FormItem>
                <FormItem label="客户名称" required>
                  <Input v-model:value="formState.customerName" placeholder="手工录入客户名称" />
                </FormItem>
                <FormItem label="模板类型" required>
                  <Select v-model:value="formState.templateType" :options="templateTypeOptions" @change="handleTemplateTypeChange" />
                </FormItem>
                <FormItem label="模板格式" required>
                  <Select v-model:value="formState.templateFormat" :options="templateFormatOptions" @change="handleTemplateFormatChange" />
                </FormItem>
                <FormItem label="状态">
                  <Select v-model:value="formState.status" :options="statusOptions" />
                </FormItem>
                <FormItem label="模板文件">
                  <Space direction="vertical" style="width: 100%">
                    <Upload accept=".zpl,.txt,.prn,.nlbl" :before-upload="beforeUpload" :show-upload-list="false">
                      <Button :loading="uploadLoading">
                        <template #icon><IconifyIcon icon="lucide:upload" /></template>
                        上传模板
                      </Button>
                    </Upload>
                    <div v-if="formState.fileName" class="file-summary">
                      <strong>{{ formState.fileName }}</strong>
                      <span>{{ formatBytes(formState.fileSize) }}</span>
                    </div>
                  </Space>
                </FormItem>
                <FormItem label="备注">
                  <Input v-model:value="formState.remark" placeholder="内部备注" />
                </FormItem>
              </Form>
            </section>

            <section v-if="!isNlblTemplate" class="modal-section zpl-section">
              <div class="panel-title">
                <strong>ZPL 模板内容</strong>
                <span>上传后自动读取，也可直接粘贴维护</span>
              </div>
              <Input.TextArea
                v-model:value="formState.templateContent"
                class="template-content-input"
                placeholder="上传模板后自动读取，也可粘贴 ZPL 文本"
              />
            </section>
            <section v-else class="modal-section nlbl-section">
              <div class="panel-title">
                <strong>NLBL 模板文件</strong>
                <span>{{ formState.fileName || '未上传' }}</span>
              </div>
              <div class="nlbl-file-box">
                <IconifyIcon icon="lucide:file-box" />
                <strong>{{ formState.fileName || '请上传 .nlbl 文件' }}</strong>
                <span>{{ formatBytes(formState.fileSize) }}</span>
              </div>
            </section>
          </div>

          <section class="modal-section variables-panel">
            <div class="variables-toolbar">
              <div>
                <strong>变量映射</strong>
                <span>已按模板类型初始化发货需求单字段，填写客户模板中的字段/变量名即可</span>
              </div>
              <Button size="small" @click="handleAddVariable">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                新增变量
              </Button>
            </div>
            <ATable
              bordered
              class="variable-table"
              :columns="variableColumns"
              :data-source="vars"
              :pagination="false"
              row-key="_rowKey"
              :scroll="{ x: 1340, y: 'calc(100vh - 558px)' }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sort'">
                  <InputNumber v-model:value="record.sort" :min="1" :precision="0" class="full-input" @change="resequenceVars" />
                </template>
                <template v-else-if="column.dataIndex === 'variableName'">
                  <Input v-model:value="record.variableName" placeholder="填写客户模板字段" />
                </template>
                <template v-else-if="column.dataIndex === 'variableScope'">
                  <Select v-model:value="record.variableScope" :options="scopeOptions" @change="handleVariableScopeChange(record)" />
                </template>
                <template v-else-if="column.dataIndex === 'sourceField'">
                  <Select
                    v-if="record.variableScope !== 'CONST'"
                    v-model:value="record.sourceField"
                    allow-clear
                    :options="getSourceFieldOptions(record.variableScope)"
                    show-search
                    @change="handleSourceFieldChange(record)"
                  />
                  <span v-else class="muted-text">使用默认值</span>
                </template>
                <template v-else-if="column.dataIndex === 'defaultValue'">
                  <Input v-model:value="record.defaultValue" placeholder="空值时使用" />
                </template>
                <template v-else-if="column.dataIndex === 'required'">
                  <Switch v-model:checked="record.required" checked-children="是" un-checked-children="否" />
                </template>
                <template v-else-if="column.dataIndex === 'variableExpr'">
                  <span class="expr-text">{{ record.variableExpr || '-' }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Button danger size="small" type="link" @click="handleRemoveVariable(record)">删除</Button>
                </template>
              </template>
            </ATable>
          </section>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.customer-print-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
}

.customer-print-filter,
.template-list-panel,
.modal-section {
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.customer-print-filter {
  flex: 0 0 auto;
  padding: 10px 12px 0;
}

.customer-print-filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
  gap: 10px;
  align-items: end;
}

.customer-print-filter-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}

.customer-print-filter-form :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.customer-print-filter-actions {
  min-width: 132px;
}

.template-list-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.section-toolbar,
.variables-toolbar,
.panel-title,
.modal-summary {
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
.variables-toolbar strong,
.panel-title strong,
.modal-summary strong {
  color: #1f2937;
  font-size: 14px;
}

.section-toolbar span,
.variables-toolbar span,
.panel-title span,
.modal-summary span {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.template-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.template-table :deep(.ant-spin-nested-loading),
.template-table :deep(.ant-spin-container),
.template-table :deep(.ant-table),
.template-table :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.template-table :deep(.ant-table-header) {
  flex: 0 0 auto;
}

.template-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  max-height: none !important;
  min-height: 0;
}

.template-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 8px 10px 10px;
  border-top: 1px solid #e5edf7;
  background: #fff;
}

.template-name-cell,
.file-cell {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.template-name-cell strong,
.file-cell strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-name-cell span,
.file-cell span {
  color: #64748b;
  font-size: 12px;
}

.template-modal-body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 110px);
  min-height: 0;
  background: #f5f7fb;
}

.template-modal-body.is-loading {
  opacity: 0.65;
  pointer-events: none;
}

.modal-content-grid {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: minmax(420px, 44%) minmax(0, 1fr);
  gap: 10px;
  min-height: 260px;
  padding: 10px;
}

.template-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 10px;
  padding: 10px;
}

.template-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.zpl-section {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.nlbl-section {
  min-width: 0;
  min-height: 0;
}

.nlbl-file-box {
  display: grid;
  place-items: center;
  min-height: 214px;
  padding: 18px;
  color: #475569;
  text-align: center;
}

.nlbl-file-box svg {
  width: 46px;
  height: 46px;
  color: #2563eb;
}

.nlbl-file-box strong {
  max-width: 100%;
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-content-input {
  flex: 1 1 auto;
  min-height: 214px;
  border: 0;
  border-radius: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  resize: none;
}

.file-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  color: #334155;
}

.variables-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin: 0 10px 10px;
  overflow: hidden;
}

.variable-table {
  flex: 1 1 auto;
  min-height: 0;
}

.variable-name,
.expr-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expr-text {
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.muted-text {
  color: #94a3b8;
}

.full-input {
  width: 100%;
}

:global(.customer-print-fullscreen-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  height: 100vh;
  margin: 0;
  padding-bottom: 0;
}

:global(.customer-print-fullscreen-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
}

:global(.customer-print-fullscreen-modal .ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

:global(.customer-print-fullscreen-modal .ant-modal-footer) {
  flex: 0 0 auto;
}

@media (max-width: 1180px) {
  .customer-print-filter-form,
  .modal-content-grid,
  .template-form {
    grid-template-columns: 1fr;
  }

  .template-modal-body {
    height: calc(100vh - 108px);
    overflow: auto;
  }

  .variables-panel {
    min-height: 360px;
  }
}
</style>
