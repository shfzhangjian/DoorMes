<script lang="ts" setup>
import type { MesHcParamRecordApi } from '#/api/mes/hc/paramrecord';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import dayjs from 'dayjs';

import { Page } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import {
  Button,
  DatePicker,
  Empty,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
} from 'ant-design-vue';

import { getParamRecordContext, getParamRecordPage, submitParamRecord } from '#/api/mes/hc/paramrecord';

type ParamTemplateField = {
  seqNo?: number;
  paramCode: string;
  paramName: string;
  controlType?: string;
  valueType?: string;
  required?: boolean;
  readonly?: boolean;
  collectMode?: string;
  judgeMode?: string;
  uom?: string;
  precision?: number;
  defaultValue?: string;
  targetValue?: string;
  lowerLimit?: string;
  upperLimit?: string;
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
  remark?: string;
};

type ParamTemplateSection = {
  sectionCode: string;
  sectionName: string;
  seqNo?: number;
  fields: ParamTemplateField[];
};

type ParamTemplate = {
  version?: string;
  templateType?: string;
  templateName?: string;
  formStyle?: string;
  source?: {
    operationCode?: string;
    operationName?: string;
  };
  layout?: {
    columns?: number;
    compact?: boolean;
    labelWidth?: number;
  };
  validation?: {
    showJudgeResult?: boolean;
    allowOutOfSpecSubmit?: boolean;
  };
  sections?: ParamTemplateSection[];
};

type FormItemState = {
  paramCode: string;
  paramName: string;
  controlType: string;
  valueType: string;
  required: boolean;
  readonly: boolean;
  collectMode: string;
  judgeMode: string;
  uom?: string;
  precision?: number;
  targetValue?: string;
  lowerLimit?: string;
  upperLimit?: string;
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
  remark?: string;
  value: any;
  judgeResult: string;
};

type SectionState = {
  sectionCode: string;
  sectionName: string;
  fields: FormItemState[];
};

const route = useRoute();

const loading = ref(false);
const saving = ref(false);
const context = ref<MesHcParamRecordApi.ParamRecordContext>();
const template = ref<ParamTemplate>();
const sections = ref<SectionState[]>([]);
const pageLoading = ref(false);
const recordList = ref<MesHcParamRecordApi.ParamRecord[]>([]);
const searchForm = reactive({
  reportNo: '',
  reportIdText: '',
});

const historyQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
});

const reportIdNumber = computed<number | undefined>(() => {
  const raw = searchForm.reportIdText?.trim();
  if (!raw) return undefined;
  const num = Number(raw);
  return Number.isFinite(num) ? num : undefined;
});

const canSubmit = computed(() => !!context.value?.reportId && !!context.value?.reportNo && sections.value.length > 0);
const showJudgeResult = computed(() => template.value?.validation?.showJudgeResult !== false);
const formColumns = computed(() => {
  const cols = Number(template.value?.layout?.columns || 2);
  return cols > 0 ? Math.min(cols, 3) : 2;
});
const formGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${formColumns.value}, minmax(0, 1fr))`,
}));

const recordColumns = [
  { title: '参数编码', dataIndex: 'paramCode', width: 140 },
  { title: '参数名称', dataIndex: 'paramName', width: 160 },
  { title: '参数值', dataIndex: 'paramValue', minWidth: 160 },
  { title: '单位', dataIndex: 'uom', width: 90 },
  {
    title: '判定结果',
    dataIndex: 'judgeResult',
    width: 120,
  },
  {
    title: '采集时间',
    dataIndex: 'createTime',
    width: 180,
    customRender: ({ text }: { text?: string }) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
  },
];

function parseTemplateJson(raw?: string | null): ParamTemplate {
  if (!raw?.trim()) {
    return {
      templateName: '',
      formStyle: '参数表',
      layout: { columns: 2, compact: true, labelWidth: 120 },
      validation: { showJudgeResult: true, allowOutOfSpecSubmit: false },
      sections: [],
    };
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed?.sections && Array.isArray(parsed.sections)) {
      return parsed;
    }
  } catch {}
  return {
    templateName: '',
    formStyle: '参数表',
    layout: { columns: 2, compact: true, labelWidth: 120 },
    validation: { showJudgeResult: true, allowOutOfSpecSubmit: false },
    sections: [],
  };
}

function normalizeField(field: ParamTemplateField, recordMap: Record<string, MesHcParamRecordApi.ParamRecord | undefined>): FormItemState {
  const record = recordMap[field.paramCode];
  let value: any = record?.paramValue ?? field.defaultValue ?? '';
  const valueType = field.valueType || 'string';
  const controlType = field.controlType || 'text';

  if (controlType === 'switch' || valueType === 'boolean') {
    value = value === true || value === 'true' || value === '1';
  } else if (controlType === 'number' || valueType === 'integer' || valueType === 'decimal') {
    value = value === '' || value === null || value === undefined ? undefined : Number(value);
    if (Number.isNaN(value)) value = undefined;
  } else if ((controlType === 'date' || valueType === 'date') && value) {
    value = dayjs(value);
  } else if ((controlType === 'time' || valueType === 'time') && value) {
    value = dayjs(`2000-01-01 ${value}`);
  }

  return {
    paramCode: field.paramCode,
    paramName: field.paramName,
    controlType,
    valueType,
    required: field.required !== false,
    readonly: !!field.readonly,
    collectMode: field.collectMode || 'MANUAL',
    judgeMode: field.judgeMode || 'NONE',
    uom: field.uom,
    precision: field.precision ?? 2,
    targetValue: field.targetValue,
    lowerLimit: field.lowerLimit,
    upperLimit: field.upperLimit,
    placeholder: field.placeholder,
    options: field.options || [],
    remark: field.remark,
    value,
    judgeResult: record?.judgeResult || '',
  };
}

function buildSections(ctx?: MesHcParamRecordApi.ParamRecordContext) {
  const parsed = parseTemplateJson(ctx?.paramTemplateJson);
  template.value = parsed;
  const recordMap = Object.fromEntries((ctx?.records || []).map((item) => [item.paramCode || '', item]));
  sections.value = (parsed.sections || [])
    .sort((a, b) => (a.seqNo || 0) - (b.seqNo || 0))
    .map((section) => ({
      sectionCode: section.sectionCode,
      sectionName: section.sectionName,
      fields: (section.fields || [])
        .sort((a, b) => (a.seqNo || 0) - (b.seqNo || 0))
        .map((field) => normalizeField(field, recordMap)),
    }));
  recalculateJudges();
}

function getNumericValue(value: any) {
  if (value === null || value === undefined || value === '') return undefined;
  const num = Number(value);
  return Number.isNaN(num) ? undefined : num;
}

function computeJudge(field: FormItemState) {
  const mode = (field.judgeMode || 'NONE').toUpperCase();
  if (mode === 'NONE') return '';
  if (field.value === undefined || field.value === null || field.value === '') return '';

  if (mode === 'RANGE') {
    const num = getNumericValue(field.value);
    const lower = getNumericValue(field.lowerLimit);
    const upper = getNumericValue(field.upperLimit);
    if (num === undefined) return '';
    if ((lower !== undefined && num < lower) || (upper !== undefined && num > upper)) {
      return '超限';
    }
    return '合格';
  }

  if (mode === 'ENUM') {
    const optionValues = (field.options || []).map((item) => String(item.value));
    return optionValues.includes(String(field.value)) ? '合格' : '超限';
  }

  if (mode === 'TEXT') {
    const target = (field.targetValue || '').trim();
    if (!target) return '';
    return String(field.value).trim() === target ? '合格' : '超限';
  }

  return '';
}

function recalculateJudges() {
  sections.value.forEach((section) => {
    section.fields.forEach((field) => {
      field.judgeResult = computeJudge(field);
    });
  });
}

function getDisplayValue(field: FormItemState) {
  if (field.value === undefined || field.value === null || field.value === '') return '-';
  if (field.controlType === 'switch' || field.valueType === 'boolean') return field.value ? '是' : '否';
  return String(field.value);
}

async function loadContext() {
  if (!reportIdNumber.value && !searchForm.reportNo.trim()) {
    message.warning('请输入报工ID或报工单号');
    return;
  }
  loading.value = true;
  try {
    const resp = await getParamRecordContext({
      reportId: reportIdNumber.value,
      reportNo: searchForm.reportNo.trim() || undefined,
    });
    context.value = resp;
    searchForm.reportIdText = resp.reportId ? String(resp.reportId) : searchForm.reportIdText;
    searchForm.reportNo = resp.reportNo || searchForm.reportNo;
    buildSections(resp);
    historyQuery.pageNo = 1;
    await queryRecordPage();
  } finally {
    loading.value = false;
  }
}

async function queryRecordPage() {
  if (!context.value?.reportId && !context.value?.reportNo) {
    recordList.value = [];
    historyQuery.total = 0;
    return;
  }
  pageLoading.value = true;
  try {
    const resp = await getParamRecordPage({
      pageNo: historyQuery.pageNo,
      pageSize: historyQuery.pageSize,
      reportId: context.value?.reportId,
      reportNo: context.value?.reportNo,
    });
    recordList.value = resp.list || [];
    historyQuery.total = resp.total || 0;
  } finally {
    pageLoading.value = false;
  }
}

function resetSearch() {
  searchForm.reportIdText = '';
  searchForm.reportNo = '';
  context.value = undefined;
  template.value = undefined;
  sections.value = [];
  recordList.value = [];
  historyQuery.total = 0;
}

function validateBeforeSubmit() {
  for (const section of sections.value) {
    for (const field of section.fields) {
      if (field.required && (field.value === '' || field.value === undefined || field.value === null)) {
        message.warning(`请填写参数【${field.paramName}】`);
        return false;
      }
      if ((field.controlType === 'number' || field.valueType === 'integer' || field.valueType === 'decimal')
        && field.value !== '' && field.value !== undefined && field.value !== null
        && Number.isNaN(Number(field.value))) {
        message.warning(`参数【${field.paramName}】必须为数值`);
        return false;
      }
      if (field.judgeResult === '超限' && template.value?.validation?.allowOutOfSpecSubmit === false) {
        message.warning(`参数【${field.paramName}】超出规范，不允许提交`);
        return false;
      }
    }
  }
  return true;
}

function buildSubmitItems(): MesHcParamRecordApi.ParamRecordItemSubmit[] {
  return sections.value.flatMap((section) =>
    section.fields.map((field) => {
      let paramValue = '';
      let valueNum: number | string | null | undefined = undefined;
      if (field.controlType === 'switch' || field.valueType === 'boolean') {
        paramValue = field.value ? 'true' : 'false';
      } else if (field.controlType === 'date' || field.valueType === 'date') {
        paramValue = field.value ? dayjs(field.value).format('YYYY-MM-DD') : '';
      } else if (field.controlType === 'time' || field.valueType === 'time') {
        paramValue = field.value ? dayjs(field.value).format('HH:mm:ss') : '';
      } else {
        paramValue = field.value === undefined || field.value === null ? '' : String(field.value);
      }
      if (field.controlType === 'number' || field.valueType === 'integer' || field.valueType === 'decimal') {
        valueNum = paramValue === '' ? null : Number(paramValue);
      }
      return {
        paramCode: field.paramCode,
        paramName: field.paramName,
        paramValue,
        valueNum,
        uom: field.uom,
        judgeResult: field.judgeResult || '',
      };
    }),
  );
}

async function handleSubmit() {
  if (!context.value?.reportId || !context.value?.reportNo) {
    message.warning('请先加载报工上下文');
    return;
  }
  recalculateJudges();
  if (!validateBeforeSubmit()) return;
  saving.value = true;
  try {
    await submitParamRecord({
      reportId: context.value.reportId,
      reportNo: context.value.reportNo,
      items: buildSubmitItems(),
    });
    message.success('工艺参数保存成功');
    await loadContext();
  } finally {
    saving.value = false;
  }
}

function handleFieldChange() {
  recalculateJudges();
}

onMounted(() => {
  const reportId = route.query.reportId;
  const reportNo = route.query.reportNo;
  if (reportId) searchForm.reportIdText = String(reportId);
  if (reportNo) searchForm.reportNo = String(reportNo);
  if (searchForm.reportIdText || searchForm.reportNo) {
    loadContext();
  }
});
</script>

<template>
  <Page auto-content-height title="工艺参数采集">
    <div class="hc-param-record-page">
      <div class="hc-param-panel">
        <div class="hc-param-panel__header">
          <span class="hc-param-panel__title-chip">报工上下文</span>
        </div>
        <div class="hc-param-panel__body">
          <div class="hc-param-query">
            <Input v-model:value="searchForm.reportIdText" placeholder="请输入报工ID" />
            <Input v-model:value="searchForm.reportNo" placeholder="请输入报工单号" />
            <div class="hc-param-query__actions">
              <Button type="primary" :loading="loading" @click="loadContext">加载</Button>
              <Button @click="resetSearch">重置</Button>
            </div>
          </div>

          <div v-if="context" class="hc-param-context">
            <div class="hc-param-context__item"><span>报工单号</span><strong>{{ context.reportNo || '-' }}</strong></div>
            <div class="hc-param-context__item"><span>工单号</span><strong>{{ context.workOrderNo || '-' }}</strong></div>
            <div class="hc-param-context__item"><span>工艺路线</span><strong>{{ context.routeName || context.routeCode || '-' }}</strong></div>
            <div class="hc-param-context__item"><span>工序</span><strong>{{ context.operationName || context.operationCode || '-' }}</strong></div>
            <div class="hc-param-context__item"><span>产品物料</span><strong>{{ context.productMaterialName || context.productMaterialCode || '-' }}</strong></div>
            <div class="hc-param-context__item"><span>批号</span><strong>{{ context.lotNo || '-' }}</strong></div>
          </div>
        </div>
      </div>

      <Spin :spinning="loading">
        <div class="hc-param-panel">
          <div class="hc-param-panel__header">
            <span class="hc-param-panel__title-chip">参数采集</span>
            <div class="hc-param-panel__header-spacer" />
            <Space>
              <Tag v-if="template?.templateName" color="blue">{{ template.templateName }}</Tag>
              <Button type="primary" :loading="saving" :disabled="!canSubmit" @click="handleSubmit">保存参数</Button>
            </Space>
          </div>
          <div class="hc-param-panel__body">
            <Empty v-if="!context" description="请先输入报工ID或报工单号加载上下文" />
            <Empty v-else-if="!sections.length" description="当前工序未配置参数模板" />
            <div v-else class="hc-param-sections">
              <div v-for="section in sections" :key="section.sectionCode" class="hc-param-section">
                <div class="hc-param-section__header">
                  <span class="hc-param-panel__title-chip">{{ section.sectionName }}</span>
                </div>
                <div class="hc-param-section__body">
                  <div class="hc-param-form-grid" :style="formGridStyle">
                    <div v-for="field in section.fields" :key="field.paramCode" class="hc-param-form-item">
                      <div class="hc-param-form-item__label">
                        <span>{{ field.paramName }}</span>
                        <span v-if="field.required" class="hc-param-required">*</span>
                        <span v-if="field.uom" class="hc-param-uom">({{ field.uom }})</span>
                      </div>

                      <InputNumber
                        v-if="field.controlType === 'number' || field.valueType === 'integer' || field.valueType === 'decimal'"
                        v-model:value="field.value"
                        class="w-full"
                        :precision="field.valueType === 'integer' ? 0 : field.precision ?? 2"
                        :readonly="field.readonly"
                        :placeholder="field.placeholder || `请输入${field.paramName}`"
                        @change="handleFieldChange"
                      />
                      <Select
                        v-else-if="field.controlType === 'select' || field.valueType === 'enum'"
                        v-model:value="field.value"
                        class="w-full"
                        :options="field.options || []"
                        :disabled="field.readonly"
                        :placeholder="field.placeholder || `请选择${field.paramName}`"
                        @change="handleFieldChange"
                      />
                      <Switch
                        v-else-if="field.controlType === 'switch' || field.valueType === 'boolean'"
                        v-model:checked="field.value"
                        :disabled="field.readonly"
                        checked-children="是"
                        un-checked-children="否"
                        @change="handleFieldChange"
                      />
                      <DatePicker
                        v-else-if="field.controlType === 'date' || field.valueType === 'date'"
                        v-model:value="field.value"
                        class="w-full"
                        :disabled="field.readonly"
                        @change="handleFieldChange"
                      />
                      <DatePicker
                        v-else-if="field.controlType === 'time' || field.valueType === 'time'"
                        v-model:value="field.value"
                        class="w-full"
                        :disabled="field.readonly"
                        picker="time"
                        format="HH:mm:ss"
                        @change="handleFieldChange"
                      />
                      <Input.TextArea
                        v-else-if="field.controlType === 'textarea'"
                        v-model:value="field.value"
                        :rows="3"
                        :readonly="field.readonly"
                        :placeholder="field.placeholder || `请输入${field.paramName}`"
                        @change="handleFieldChange"
                      />
                      <Input
                        v-else-if="field.controlType === 'readonly' || field.readonly"
                        :value="getDisplayValue(field)"
                        readonly
                      />
                      <Input
                        v-else
                        v-model:value="field.value"
                        :readonly="field.readonly"
                        :placeholder="field.placeholder || `请输入${field.paramName}`"
                        @change="handleFieldChange"
                      />

                      <div class="hc-param-form-item__meta">
                        <span v-if="field.targetValue">标准：{{ field.targetValue }}</span>
                        <span v-if="field.lowerLimit || field.upperLimit">范围：{{ field.lowerLimit || '-' }} ~ {{ field.upperLimit || '-' }}</span>
                        <Tag v-if="showJudgeResult && field.judgeResult" :color="field.judgeResult === '合格' ? 'green' : 'red'">
                          {{ field.judgeResult }}
                        </Tag>
                      </div>
                      <div v-if="field.remark" class="hc-param-form-item__remark">{{ field.remark }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="hc-param-panel">
          <div class="hc-param-panel__header">
            <span class="hc-param-panel__title-chip">采集记录</span>
          </div>
          <div class="hc-param-panel__body">
            <Table
              :columns="recordColumns"
              :data-source="recordList"
              :loading="pageLoading"
              :pagination="{
                current: historyQuery.pageNo,
                pageSize: historyQuery.pageSize,
                total: historyQuery.total,
                showSizeChanger: true,
                onChange: (page: number, pageSize: number) => { historyQuery.pageNo = page; historyQuery.pageSize = pageSize; queryRecordPage(); },
              }"
              bordered
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'judgeResult'">
                  <Tag v-if="record.judgeResult" :color="record.judgeResult === '合格' ? 'green' : record.judgeResult === '超限' ? 'red' : 'blue'">
                    {{ record.judgeResult }}
                  </Tag>
                  <span v-else>-</span>
                </template>
              </template>
            </Table>
          </div>
        </div>
      </Spin>
    </div>
  </Page>
</template>

<style scoped>
.hc-param-record-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hc-param-panel {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  overflow: visible;
  min-width: 0;
  padding: 0;
}

.hc-param-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 0;
}

.hc-param-panel__header-spacer {
  flex: 1;
}

.hc-param-panel__title-chip {
  display: inline-flex;
  align-items: center;
  min-width: 76px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 24px;
}

.hc-param-panel__body {
  padding: 12px;
}

.hc-param-query {
  display: grid;
  grid-template-columns: 220px 280px 1fr;
  gap: 12px;
  align-items: center;
}

.hc-param-query__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.hc-param-context {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 16px;
  margin-top: 12px;
}

.hc-param-context__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.hc-param-context__item span {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}

.hc-param-context__item strong {
  color: var(--ant-color-text);
  word-break: break-all;
}

.hc-param-sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hc-param-section {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.hc-param-section__header {
  padding: 8px 12px 0;
}

.hc-param-section__body {
  padding: 12px;
}

.hc-param-form-grid {
  display: grid;
  gap: 12px 16px;
}

.hc-param-form-item {
  min-width: 0;
}

.hc-param-form-item__label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
  color: var(--ant-color-text);
  font-weight: 500;
}

.hc-param-required {
  color: #ff4d4f;
}

.hc-param-uom {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}

.hc-param-form-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 22px;
  margin-top: 6px;
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}

.hc-param-form-item__remark {
  margin-top: 4px;
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
</style>
