import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

export type SrmCrudRecord = Record<string, any>;

export class SrmSilentCancelError extends Error {}

export type SrmReferenceType =
  | 'material'
  | 'metric'
  | 'performancePlan'
  | 'productBom'
  | 'project'
  | 'sampleRequest'
  | 'supplier'
  | 'template';

export interface SrmReferenceMapping {
  source: string;
  target: string;
}

export interface SrmReferenceConfig {
  displayField?: string;
  mappings?: SrmReferenceMapping[];
  supplierSource?: 'candidate' | 'roster';
  title?: string;
  type: SrmReferenceType;
}

export interface SrmCrudOption {
  disabled?: boolean;
  label: string;
  value: any;
}

export interface SrmCrudField {
  allowManualInput?: boolean;
  component?:
    | 'AutoComplete'
    | 'DatePicker'
    | 'DateTimePicker'
    | 'Input'
    | 'InputNumber'
    | 'JsonTextarea'
    | 'RadioGroup'
    | 'ReferencePicker'
    | 'Select'
    | 'Textarea'
    | 'UrlInput';
  field: string;
  getOptions?: (record: SrmCrudRecord) => SrmCrudOption[];
  label: string;
  labelWhen?: (record: SrmCrudRecord) => string;
  joinSeparator?: string;
  manualClearFields?: string[];
  mode?: 'multiple';
  onChange?: (value: any, record: SrmCrudRecord) => void;
  options?: SrmCrudOption[];
  reference?: SrmReferenceConfig;
  readonly?: boolean;
  readonlyWhen?: (record: SrmCrudRecord) => boolean;
  rows?: number;
  span?: 1 | 2 | 3;
  visibleWhen?: (record: SrmCrudRecord) => boolean;
}

export type SrmCrudColumn = NonNullable<VxeTableGridOptions['columns']>[number];

const workflowFieldSet = new Set([
  'currentNode',
  'node',
  'tabType',
  'workflowNode',
]);

const computedFieldSet = new Set(['actions', 'compliance']);
const hiddenDetailFieldSet = new Set([
  'extraJson',
  'fileStatus',
  'payloadJson',
  'status',
]);

const longFieldPattern =
  /desc|detail|opinion|reason|remark|requirement|method|plan|summary|action|source|rule|content|scope/i;

const dateTimeFieldPattern = /(?:time|dateTime)$/i;
const dateFieldPattern =
  /(?:date|deadline|expiry|effect|publish|trial|require|import|apply)$/i;

export function cloneSrmRecord<T = SrmCrudRecord>(value: T): T {
  return structuredClone(value || {}) as T;
}

export function sanitizeSrmRecord(record: SrmCrudRecord): SrmCrudRecord {
  const cloned = cloneSrmRecord(record);
  return sanitizeValue(cloned) as SrmCrudRecord;
}

export function sanitizeSrmRecords(records: SrmCrudRecord[]) {
  return records.map((record) => sanitizeSrmRecord(record));
}

export function inferSrmCrudFields(
  columns: VxeTableGridOptions['columns'] = [],
  formSchema: VbenFormSchema[] = [],
): SrmCrudField[] {
  const schemaMap = new Map<string, VbenFormSchema>();
  formSchema.forEach((schema) => {
    if (schema.fieldName) {
      schemaMap.set(schema.fieldName, schema);
    }
  });

  const fields: SrmCrudField[] = [];
  columns.forEach((column) => {
    const field = String((column as any).field || '');
    if (
      !field ||
      workflowFieldSet.has(field) ||
      computedFieldSet.has(field) ||
      isHiddenSrmDetailField(field)
    ) {
      return;
    }
    if (
      (column as any).type ||
      String((column as any).title || '') === '操作'
    ) {
      return;
    }

    const schema = schemaMap.get(field);
    fields.push({
      component: inferComponent(
        field,
        String((column as any).title || ''),
        schema,
      ),
      field,
      label: String((column as any).title || schema?.label || field),
      options: inferOptions(schema),
      rows: longFieldPattern.test(field) ? 3 : undefined,
      span: shouldSpanFull(field, String((column as any).title || '')) ? 3 : 1,
    });
  });

  return fields;
}

export function isHiddenSrmDetailField(field: string) {
  const normalizedField = String(field || '');
  return (
    hiddenDetailFieldSet.has(normalizedField) || /json$/i.test(normalizedField)
  );
}

export function normalizeSrmColumns(
  columns: VxeTableGridOptions['columns'] = [],
): VxeTableGridOptions['columns'] {
  return columns
    .filter((column) => {
      const field = String((column as any).field || '');
      return !workflowFieldSet.has(field) && !computedFieldSet.has(field);
    })
    .map((column) => {
      if ((column as any).type) {
        return column;
      }
      if (String((column as any).title || '') === '操作') {
        return {
          ...column,
          slots: { default: 'actions' },
          width: 120,
        };
      }
      const field = String((column as any).field || '');
      if (!field) {
        return column;
      }
      return {
        ...column,
        slots: { default: field },
      };
    });
}

export function getSrmSlotFields(columns: VxeTableGridOptions['columns'] = []) {
  return (
    normalizeSrmColumns(columns)
      ?.map((column) => String((column as any).field || ''))
      .filter(Boolean) || []
  );
}

export function filterSrmRecords(
  records: SrmCrudRecord[],
  formValues: SrmCrudRecord = {},
) {
  return records.filter((record) =>
    Object.entries(formValues || {}).every(([key, rawValue]) => {
      if (rawValue === undefined || rawValue === null || rawValue === '') {
        return true;
      }
      if (Array.isArray(rawValue) && rawValue.length === 0) {
        return true;
      }
      if (Array.isArray(rawValue) && rawValue.length === 2) {
        return true;
      }

      const value = String(rawValue).trim();
      if (!value) {
        return true;
      }
      const directValue = record[key];
      if (directValue === undefined || key.endsWith('Info')) {
        return Object.values(record).some((item) =>
          stringifyValue(item).includes(value),
        );
      }
      return stringifyValue(directValue).includes(value);
    }),
  );
}

export function getSrmStatusColor(value: any) {
  const stringValue = String(value);
  if (
    [
      '0',
      'ACTIVE',
      'CLOSED',
      'CONVERTED',
      'ENABLED',
      'FINISHED',
      'PASS',
      'PASSED',
      'QUALIFIED',
      'VALID',
    ].includes(stringValue)
  ) {
    return 'success';
  }
  if (
    [
      '1',
      'APPROVING',
      'FILLING',
      'PENDING',
      'QA_REVIEW',
      'TESTING',
      'VERIFYING',
      'WAIT_SUPPLIER',
      'WARNING',
    ].includes(stringValue)
  ) {
    return 'processing';
  }
  if (['DRAFT', 'EXITED', 'REGISTERED', 'RESTRICTED'].includes(stringValue)) {
    return 'default';
  }
  if (
    [
      '2',
      'DISABLED',
      'ELIMINATED',
      'EXPIRED',
      'FROZEN',
      'INVALID',
      'REJECT',
      'REJECTED',
      'UNQUALIFIED',
    ].includes(stringValue)
  ) {
    return 'error';
  }
  return 'blue';
}

export function getSrmValueLabel(
  value: any,
  valueLabels: Record<string, string> = {},
) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  const key = String(value);
  const defaults: Record<string, string> = {
    0: '启用',
    1: '进行中',
    2: '停用',
    ACTIVE: '合作中',
    AGENT: '代理贸易',
    APPROVING: '处理中',
    CLOSED: '已关闭',
    CONVERTED: '已转正式',
    DRAFT: '草稿',
    DISABLED: '停用',
    EXPIRED: '已过期',
    ENABLED: '启用',
    FILLING: '填报中',
    FINISHED: '已归档',
    FROZEN: '冻结停供',
    ELIMINATED: '淘汰',
    EXITED: '退出',
    MANUFACTURER: '原厂生产',
    PENDING: '待入库',
    PASSED: '已归档',
    PASS: '通过',
    QUALIFIED: '合格',
    QA_REVIEW: '质量审核',
    REJECT: '驳回',
    REJECTED: '已退回',
    RESTRICTED: '限制交易',
    REGISTERED: '已入库',
    TESTING: '测试评估中',
    VALID: '有效',
    VERIFYING: '验证中',
    WAIT_SUPPLIER: '待供应商回复',
    WARNING: '临期预警',
    UNQUALIFIED: '不合格',
  };
  return valueLabels[key] || defaults[key] || key;
}

export function stringifyValue(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  if (Array.isArray(value)) {
    return value.map((item) => stringifyValue(item)).join(' ');
  }
  if (typeof value === 'object') {
    return Object.values(value as Record<string, unknown>)
      .map((item) => stringifyValue(item))
      .join(' ');
  }
  return String(value);
}

function sanitizeValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeValue(item));
  }
  if (value && typeof value === 'object') {
    Object.keys(value as Record<string, unknown>).forEach((key) => {
      (value as Record<string, unknown>)[key] = sanitizeValue(
        (value as Record<string, unknown>)[key],
      );
    });
    return value;
  }
  if (typeof value !== 'string') {
    return value;
  }
  return value
    .replaceAll('我(当前用户)', '当前经办人')
    .replaceAll('当前用户', '当前经办人')
    .replaceAll('我下发的', '下发的');
}

function inferComponent(
  field: string,
  label: string,
  schema?: VbenFormSchema,
): SrmCrudField['component'] {
  const schemaComponent = String(schema?.component || '');
  if (schemaComponent === 'Select' || inferOptions(schema).length > 0) {
    return 'Select';
  }
  if (schemaComponent === 'Textarea') {
    return 'Textarea';
  }
  if (schemaComponent === 'InputNumber') {
    return 'InputNumber';
  }
  if (dateTimeFieldPattern.test(field) || label.includes('时间')) {
    return 'DateTimePicker';
  }
  if (dateFieldPattern.test(field) || label.includes('日期')) {
    return 'DatePicker';
  }
  if (longFieldPattern.test(field)) {
    return 'Textarea';
  }
  return 'Input';
}

function inferOptions(schema?: VbenFormSchema) {
  return ((schema?.componentProps as any)?.options || []) as Array<{
    label: string;
    value: any;
  }>;
}

function shouldSpanFull(field: string, label: string) {
  return longFieldPattern.test(field) || label.length >= 9;
}
