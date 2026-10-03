<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Input, InputNumber, Select, Switch, Tooltip, message } from 'ant-design-vue';

import { VxeColumn, VxeTable } from '#/adapter/vxe-table';

type ParamFieldOption = {
  label: string;
  value: string;
};

type ParamField = {
  _rowKey: string;
  groupCode?: string;
  groupName?: string;
  seqNo?: number;
  paramCode?: string;
  paramName?: string;
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
  optionText?: string;
  remark?: string;
};

type ParamTemplate = {
  version: string;
  templateType: string;
  templateName: string;
  formStyle: string;
  source: {
    operationCode?: string;
    operationName?: string;
  };
  layout: {
    columns: number;
    compact: boolean;
    labelWidth: number;
  };
  validation: {
    showJudgeResult: boolean;
    allowOutOfSpecSubmit: boolean;
  };
  sections: Array<{
    sectionCode: string;
    sectionName: string;
    seqNo: number;
    fields: Array<Record<string, any>>;
  }>;
};

const emit = defineEmits<{
  success: [
    {
      value: string;
      summary: string;
    },
  ];
}>();

const rowSeed = ref(0);
const invalidRowKeys = ref<string[]>([]);
const currentRowKey = ref('');
const templateName = ref('');
const formStyle = ref('参数表');
const formColumns = ref(2);
const showJudgeResult = ref(true);
const allowOutOfSpecSubmit = ref(false);
const fields = ref<ParamField[]>([]);
const operationCode = ref('');
const operationName = ref('');
const fieldTableRef = ref<any>();

const formStyleOptions = [
  { label: '参数表', value: '参数表' },
  { label: '点检表', value: '点检表' },
  { label: '检验表', value: '检验表' },
  { label: '记录表', value: '记录表' },
];

const controlTypeOptions = [
  { label: '数值框', value: 'number' },
  { label: '文本框', value: 'text' },
  { label: '下拉框', value: 'select' },
  { label: '开关', value: 'switch' },
  { label: '日期', value: 'date' },
  { label: '时间', value: 'time' },
  { label: '多行文本', value: 'textarea' },
  { label: '只读显示', value: 'readonly' },
];

const valueTypeOptions = [
  { label: '字符串', value: 'string' },
  { label: '整数', value: 'integer' },
  { label: '小数', value: 'decimal' },
  { label: '布尔', value: 'boolean' },
  { label: '日期', value: 'date' },
  { label: '时间', value: 'time' },
  { label: '枚举', value: 'enum' },
];

const collectModeOptions = [
  { label: '手工录入', value: 'MANUAL' },
  { label: '设备采集', value: 'DEVICE' },
  { label: '系统带出', value: 'SYSTEM' },
  { label: '自动计算', value: 'DERIVED' },
];

const judgeModeOptions = [
  { label: '不判定', value: 'NONE' },
  { label: '区间判定', value: 'RANGE' },
  { label: '枚举判定', value: 'ENUM' },
  { label: '文本判定', value: 'TEXT' },
];

const getTitle = computed(() => {
  const title = operationName.value || operationCode.value || '工序';
  return `配置参数模板 - ${title}`;
});

function nextRowKey() {
  rowSeed.value += 1;
  return `param-field-${Date.now()}-${rowSeed.value}`;
}

function normalizeField(row?: Partial<ParamField>, index = 0): ParamField {
  return {
    _rowKey: row?._rowKey || nextRowKey(),
    groupCode: row?.groupCode || 'BASE',
    groupName: row?.groupName || '基础参数',
    seqNo: row?.seqNo || (index + 1) * 10,
    paramCode: row?.paramCode || '',
    paramName: row?.paramName || '',
    controlType: row?.controlType || 'number',
    valueType: row?.valueType || 'decimal',
    required: row?.required ?? true,
    readonly: row?.readonly ?? false,
    collectMode: row?.collectMode || 'MANUAL',
    judgeMode: row?.judgeMode || 'RANGE',
    uom: row?.uom || '',
    precision: row?.precision ?? 2,
    defaultValue: row?.defaultValue || '',
    targetValue: row?.targetValue || '',
    lowerLimit: row?.lowerLimit || '',
    upperLimit: row?.upperLimit || '',
    placeholder: row?.placeholder || '',
    optionText: row?.optionText || '',
    remark: row?.remark || '',
  };
}

function isEmptyField(row: Partial<ParamField>) {
  return ![
    row.groupCode,
    row.groupName,
    row.paramCode,
    row.paramName,
    row.uom,
    row.defaultValue,
    row.targetValue,
    row.lowerLimit,
    row.upperLimit,
    row.optionText,
    row.remark,
  ].some((value) => value !== undefined && value !== null && String(value).trim() !== '');
}

function buildSummary(fieldCount: number, sectionCount: number) {
  return `${formStyle.value || '参数表'} · ${sectionCount}组/${fieldCount}项`;
}

function legacyLabel(code: string) {
  const map: Record<string, string> = {
    solid_content: '固含量',
    viscosity: '粘度',
    coat_temp: '涂布温度',
    water_temp: '水温',
    thickness: '厚度',
    piece_qty: '片数',
    groove_depth: '槽深',
    back_glue: '背胶确认',
    diameter: '外径',
    appearance: '外观',
    inner_pack: '内包装确认',
    outer_pack: '外包装确认',
    double_adhesive: '双面胶确认',
    shipment_check: '出货检查',
    warehouse_in: '入库确认',
  };
  return map[code] || code;
}

function convertLegacyTemplate(raw: Record<string, any>) {
  const legacyFields = Object.entries(raw).map(([key, value], index) => {
    const item = typeof value === 'object' && value !== null ? value : { defaultValue: String(value ?? '') };
    const isBoolean = typeof value === 'boolean' || item.required === true;
    return normalizeField(
      {
        groupCode: 'BASE',
        groupName: '基础参数',
        seqNo: (index + 1) * 10,
        paramCode: key,
        paramName: legacyLabel(key),
        controlType: isBoolean ? 'switch' : 'number',
        valueType: isBoolean ? 'boolean' : 'decimal',
        judgeMode: item.min !== undefined || item.max !== undefined ? 'RANGE' : 'NONE',
        required: item.required ?? false,
        lowerLimit: item.min !== undefined ? String(item.min) : '',
        upperLimit: item.max !== undefined ? String(item.max) : '',
        targetValue: item.target !== undefined ? String(item.target) : '',
        defaultValue: item.defaultValue !== undefined ? String(item.defaultValue) : '',
        remark: item.source ? `来源：${item.source}` : '',
      },
      index,
    );
  });
  return {
    version: '1.0',
    templateType: 'PROCESS_PARAM',
    templateName: `${operationName.value || operationCode.value || '工序'}参数模板`,
    formStyle: '参数表',
    source: {
      operationCode: operationCode.value,
      operationName: operationName.value,
    },
    layout: {
      columns: 2,
      compact: true,
      labelWidth: 120,
    },
    validation: {
      showJudgeResult: true,
      allowOutOfSpecSubmit: false,
    },
    sections: [
      {
        sectionCode: 'BASE',
        sectionName: '基础参数',
        seqNo: 10,
        fields: legacyFields.map(({ _rowKey, ...field }) => field),
      },
    ],
  } satisfies ParamTemplate;
}

function loadTemplate(value?: string) {
  const fallback = {
    version: '1.0',
    templateType: 'PROCESS_PARAM',
    templateName: `${operationName.value || operationCode.value || '工序'}参数模板`,
    formStyle: '参数表',
    source: {
      operationCode: operationCode.value,
      operationName: operationName.value,
    },
    layout: {
      columns: 2,
      compact: true,
      labelWidth: 120,
    },
    validation: {
      showJudgeResult: true,
      allowOutOfSpecSubmit: false,
    },
    sections: [],
  } satisfies ParamTemplate;

  if (!value?.trim()) {
    templateName.value = fallback.templateName;
    formStyle.value = fallback.formStyle;
    formColumns.value = fallback.layout.columns;
    showJudgeResult.value = fallback.validation.showJudgeResult;
    allowOutOfSpecSubmit.value = fallback.validation.allowOutOfSpecSubmit;
    fields.value = [];
    return;
  }

  try {
    const parsed = JSON.parse(value);
    const normalized =
      parsed?.version && Array.isArray(parsed?.sections) ? parsed : convertLegacyTemplate(parsed || {});
    templateName.value = normalized.templateName || fallback.templateName;
    formStyle.value = normalized.formStyle || fallback.formStyle;
    formColumns.value = normalized.layout?.columns || fallback.layout.columns;
    showJudgeResult.value = normalized.validation?.showJudgeResult ?? true;
    allowOutOfSpecSubmit.value = normalized.validation?.allowOutOfSpecSubmit ?? false;
    fields.value = (normalized.sections || []).flatMap((section: any, sectionIndex: number) =>
      (section.fields || []).map((field: any, fieldIndex: number) =>
        normalizeField(
          {
            groupCode: section.sectionCode || 'BASE',
            groupName: section.sectionName || '基础参数',
            seqNo: field.seqNo || (fieldIndex + 1) * 10,
            paramCode: field.paramCode,
            paramName: field.paramName,
            controlType: field.controlType,
            valueType: field.valueType,
            required: field.required,
            readonly: field.readonly,
            collectMode: field.collectMode,
            judgeMode: field.judgeMode,
            uom: field.uom,
            precision: field.precision,
            defaultValue: field.defaultValue,
            targetValue: field.targetValue,
            lowerLimit: field.lowerLimit,
            upperLimit: field.upperLimit,
            placeholder: field.placeholder,
            optionText: Array.isArray(field.options)
              ? field.options.map((item: any) => `${item.label}:${item.value}`).join('\n')
              : '',
            remark: field.remark,
          },
          sectionIndex * 100 + fieldIndex,
        ),
      ),
    );
  } catch {
    const normalized = convertLegacyTemplate({});
    templateName.value = normalized.templateName;
    formStyle.value = normalized.formStyle;
    formColumns.value = normalized.layout.columns;
    showJudgeResult.value = true;
    allowOutOfSpecSubmit.value = false;
    fields.value = [];
  }
}

function validateFields() {
  const invalidIndex = fields.value.findIndex((row) => {
    if (isEmptyField(row)) return true;
    return !row.paramCode?.trim() || !row.paramName?.trim();
  });
  invalidRowKeys.value = invalidIndex >= 0 ? [fields.value[invalidIndex]?._rowKey || ''] : [];
  if (invalidIndex >= 0) {
    currentRowKey.value = fields.value[invalidIndex]?._rowKey || '';
    fieldTableRef.value?.scrollToRow?.(fields.value[invalidIndex]);
    message.warning(`参数项第 ${invalidIndex + 1} 行请填写参数编码和参数名称`);
    return false;
  }
  return true;
}

function addField() {
  const row = normalizeField({}, fields.value.length);
  fields.value.push(row);
  currentRowKey.value = row._rowKey;
  invalidRowKeys.value = [];
  fieldTableRef.value?.scrollToRow?.(row);
}

function removeField(index: number) {
  fields.value.splice(index, 1);
}

function copyField(index: number) {
  const source = fields.value[index];
  if (!source) return;
  const row = normalizeField({ ...source }, index + 1);
  fields.value.splice(index + 1, 0, row);
  currentRowKey.value = row._rowKey;
  fieldTableRef.value?.scrollToRow?.(row);
}

function moveField(index: number, direction: 'up' | 'down') {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= fields.value.length) return;
  const current = fields.value[index];
  fields.value[index] = fields.value[targetIndex]!;
  fields.value[targetIndex] = current!;
  currentRowKey.value = fields.value[targetIndex]?._rowKey || '';
}

function buildTemplateJson() {
  const validFields = fields.value
    .filter((row) => !isEmptyField(row))
    .map((row, index) => ({
      ...row,
      seqNo: (index + 1) * 10,
    }));
  const sectionMap = new Map<string, { sectionCode: string; sectionName: string; seqNo: number; fields: any[] }>();
  validFields.forEach((field, index) => {
    const code = field.groupCode?.trim() || 'BASE';
    const name = field.groupName?.trim() || '基础参数';
    const section = sectionMap.get(code) || {
      sectionCode: code,
      sectionName: name,
      seqNo: sectionMap.size * 10 + 10,
      fields: [],
    };
    const options: ParamFieldOption[] = String(field.optionText || '')
      .split(/\r?\n/)
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => {
        const parts = item.includes(':') ? item.split(':', 2) : [item, item];
        const label = parts[0] || '';
        const value = parts[1] || label;
        return { label: label.trim(), value: value.trim() };
      });
    section.fields.push({
      seqNo: field.seqNo || (index + 1) * 10,
      paramCode: field.paramCode?.trim(),
      paramName: field.paramName?.trim(),
      controlType: field.controlType || 'number',
      valueType: field.valueType || 'decimal',
      required: !!field.required,
      readonly: !!field.readonly,
      collectMode: field.collectMode || 'MANUAL',
      judgeMode: field.judgeMode || 'NONE',
      uom: field.uom?.trim() || '',
      precision: field.precision ?? 2,
      defaultValue: field.defaultValue?.trim() || '',
      targetValue: field.targetValue?.trim() || '',
      lowerLimit: field.lowerLimit?.trim() || '',
      upperLimit: field.upperLimit?.trim() || '',
      placeholder: field.placeholder?.trim() || '',
      options,
      remark: field.remark?.trim() || '',
    });
    sectionMap.set(code, section);
  });

  const template: ParamTemplate = {
    version: '1.0',
    templateType: 'PROCESS_PARAM',
    templateName: templateName.value.trim() || `${operationName.value || operationCode.value || '工序'}参数模板`,
    formStyle: formStyle.value,
    source: {
      operationCode: operationCode.value,
      operationName: operationName.value,
    },
    layout: {
      columns: formColumns.value,
      compact: true,
      labelWidth: 120,
    },
    validation: {
      showJudgeResult: showJudgeResult.value,
      allowOutOfSpecSubmit: allowOutOfSpecSubmit.value,
    },
    sections: Array.from(sectionMap.values()),
  };
  return {
    value: JSON.stringify(template),
    summary: buildSummary(validFields.length, template.sections.length),
  };
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[1400px]',
  async onConfirm() {
    if (!validateFields()) return;
    emit('success', buildTemplateJson());
    await modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;
    const data = modalApi.getData<{ value?: string; operationCode?: string; operationName?: string }>();
    operationCode.value = data?.operationCode || '';
    operationName.value = data?.operationName || '';
    invalidRowKeys.value = [];
    currentRowKey.value = '';
    loadTemplate(data?.value);
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="param-template-modal">
      <div class="param-template-meta">
        <div class="param-template-meta__item">
          <div class="param-template-meta__label">模板名称</div>
          <Input v-model:value="templateName" placeholder="请输入模板名称" />
        </div>
        <div class="param-template-meta__item">
          <div class="param-template-meta__label">表单样式</div>
          <Select v-model:value="formStyle" :options="formStyleOptions" />
        </div>
        <div class="param-template-meta__item">
          <div class="param-template-meta__label">表单列数</div>
          <InputNumber v-model:value="formColumns" :min="1" :max="4" class="w-full" />
        </div>
        <div class="param-template-meta__item param-template-meta__item--switch">
          <div class="param-template-meta__label">显示判定结果</div>
          <Switch v-model:checked="showJudgeResult" />
        </div>
        <div class="param-template-meta__item param-template-meta__item--switch">
          <div class="param-template-meta__label">超规范允许提交</div>
          <Switch v-model:checked="allowOutOfSpecSubmit" />
        </div>
      </div>

      <div class="param-template-toolbar">
        <div class="param-template-toolbar__title">参数项配置</div>
        <Button type="primary" size="small" @click="addField">新增参数项</Button>
      </div>

      <div class="param-template-table-wrap">
        <VxeTable
          ref="fieldTableRef"
          :data="fields"
          auto-resize
          border
          stripe
          :round="false"
          size="small"
          height="100%"
          show-overflow
          row-id="_rowKey"
          :row-class-name="({ row }) => (invalidRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentRowKey === row._rowKey ? 'is-current-row' : '')"
          @cell-click="({ row }) => (currentRowKey = row._rowKey)"
        >
          <VxeColumn field="groupName" title="分组" min-width="120" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.groupName" placeholder="如：工艺参数" />
            </template>
          </VxeColumn>
          <VxeColumn field="paramCode" title="*参数编码" min-width="140" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.paramCode" placeholder="如：groove_depth" />
            </template>
          </VxeColumn>
          <VxeColumn field="paramName" title="*参数名称" min-width="150" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.paramName" placeholder="如：槽深" />
            </template>
          </VxeColumn>
          <VxeColumn field="controlType" title="控件" width="110" header-align="center">
            <template #default="{ row }">
              <Select v-model:value="row.controlType" :options="controlTypeOptions" class="w-full" />
            </template>
          </VxeColumn>
          <VxeColumn field="valueType" title="数据类型" width="110" header-align="center">
            <template #default="{ row }">
              <Select v-model:value="row.valueType" :options="valueTypeOptions" class="w-full" />
            </template>
          </VxeColumn>
          <VxeColumn field="collectMode" title="采集方式" width="120" header-align="center">
            <template #default="{ row }">
              <Select v-model:value="row.collectMode" :options="collectModeOptions" class="w-full" />
            </template>
          </VxeColumn>
          <VxeColumn field="judgeMode" title="判定方式" width="120" header-align="center">
            <template #default="{ row }">
              <Select v-model:value="row.judgeMode" :options="judgeModeOptions" class="w-full" />
            </template>
          </VxeColumn>
          <VxeColumn field="uom" title="单位" width="90" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.uom" placeholder="单位" />
            </template>
          </VxeColumn>
          <VxeColumn field="targetValue" title="标准值" width="110" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.targetValue" placeholder="标准值" />
            </template>
          </VxeColumn>
          <VxeColumn field="lowerLimit" title="下限" width="100" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.lowerLimit" placeholder="下限" />
            </template>
          </VxeColumn>
          <VxeColumn field="upperLimit" title="上限" width="100" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.upperLimit" placeholder="上限" />
            </template>
          </VxeColumn>
          <VxeColumn field="defaultValue" title="默认值" width="100" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.defaultValue" placeholder="默认值" />
            </template>
          </VxeColumn>
          <VxeColumn field="required" title="必填" width="80" align="center" header-align="center">
            <template #default="{ row }">
              <Switch v-model:checked="row.required" />
            </template>
          </VxeColumn>
          <VxeColumn field="readonly" title="只读" width="80" align="center" header-align="center">
            <template #default="{ row }">
              <Switch v-model:checked="row.readonly" />
            </template>
          </VxeColumn>
          <VxeColumn field="optionText" title="选项定义" min-width="160" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.optionText" placeholder="每行一个选项，格式：标签:值" />
            </template>
          </VxeColumn>
          <VxeColumn field="remark" title="备注" min-width="140" header-align="center">
            <template #default="{ row }">
              <Input v-model:value="row.remark" placeholder="备注" />
            </template>
          </VxeColumn>
          <VxeColumn title="操作" width="150" fixed="right" align="center" header-align="center">
            <template #default="{ $rowIndex }">
              <div class="param-template-actions">
                <Tooltip title="复制">
                  <Button type="text" @click="copyField($rowIndex)">
                    <IconifyIcon icon="lucide:copy" />
                  </Button>
                </Tooltip>
                <Tooltip title="上移">
                  <Button type="text" @click="moveField($rowIndex, 'up')">
                    <IconifyIcon icon="lucide:arrow-up" />
                  </Button>
                </Tooltip>
                <Tooltip title="下移">
                  <Button type="text" @click="moveField($rowIndex, 'down')">
                    <IconifyIcon icon="lucide:arrow-down" />
                  </Button>
                </Tooltip>
                <Tooltip title="删除">
                  <Button danger type="text" @click="removeField($rowIndex)">
                    <IconifyIcon icon="lucide:trash-2" />
                  </Button>
                </Tooltip>
              </div>
            </template>
          </VxeColumn>
        </VxeTable>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.param-template-modal {
  display: flex;
  height: 620px;
  min-height: 620px;
  flex-direction: column;
  gap: 12px;
}

.param-template-meta {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.param-template-meta__item {
  min-width: 0;
}

.param-template-meta__item--switch {
  display: flex;
  align-items: end;
}

.param-template-meta__label {
  margin-bottom: 6px;
  color: #1f2329;
  font-size: 12px;
}

.param-template-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.param-template-toolbar__title {
  font-size: 15px;
  font-weight: 600;
}

.param-template-table-wrap {
  min-height: 0;
  flex: 1;
}

.param-template-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.param-template-table-wrap :deep(.is-invalid-row) {
  background-color: #fff1f0;
}

.param-template-table-wrap :deep(.is-current-row) {
  background-color: #e6f4ff;
}
</style>
