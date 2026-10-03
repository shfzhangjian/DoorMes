export interface FormulaSubField {
  key: string;
  label: string;
  type: 'TEXT' | 'NUMBER';
  unit: string;
  required: boolean;
}
export interface MultiFieldItem {
  valueMode?: string;
  fieldDefinitionsJson?: string;
  fieldValuesJson?: string;
  [key: string]: any;
}
export const MAX_FORMULA_FIELDS = 30;
export function multiFields(item: MultiFieldItem): FormulaSubField[] {
  const parsed = JSON.parse(item.fieldDefinitionsJson || '[]');
  if (!Array.isArray(parsed)) throw new Error('子字段配置格式错误');
  return parsed;
}
export function fieldLabel(field: FormulaSubField) {
  return `${field.label}${field.unit ? `（${field.unit}）` : ''}${field.required ? ' *' : ''}`;
}
export function readFormulaValue(item: MultiFieldItem, field: string): string {
  if (!field.startsWith('multi:')) return String(item[field] ?? '');
  return String(JSON.parse(item.fieldValuesJson || '{}')[field.slice(6)] ?? '');
}
export function writeFormulaValue(item: MultiFieldItem, field: string, value: unknown) {
  if (!field.startsWith('multi:')) { item[field] = String(value ?? ''); return; }
  const key = field.slice(6);
  if (!multiFields(item).some((f) => f.key === key)) throw new Error('子字段已变化，请重新加载');
  item.fieldValuesJson = JSON.stringify({ ...JSON.parse(item.fieldValuesJson || '{}'), [key]: String(value ?? '') });
}
export function multiFieldError(item: MultiFieldItem, complete: boolean): string {
  if (item.valueMode !== 'MULTI_FIELDS') return '';
  const fields = multiFields(item);
  if (!fields.length || fields.length > MAX_FORMULA_FIELDS) return '请配置 1 至 30 个子字段';
  for (const field of fields) {
    const value = readFormulaValue(item, `multi:${field.key}`).trim();
    if (complete && field.required && !value) return `请填写${field.label}`;
    if (value && field.type === 'NUMBER' && (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(value))) return `${field.label}必须填写数字`;
  }
  return '';
}

export function multiDefinitionError(item: MultiFieldItem): string {
  if (item.valueMode !== 'MULTI_FIELDS') return '';
  try {
    const fields = multiFields(item);
    if (!fields.length || fields.length > MAX_FORMULA_FIELDS) return '请配置 1 至 30 个子字段';
    const keys = new Set<string>();
    for (const field of fields) {
      if (!field || !/^[A-Za-z][A-Za-z0-9_]{0,63}$/.test(field.key) || keys.has(field.key)) return '子字段标识无效或重复';
      keys.add(field.key);
      if (!String(field.label || '').trim() || field.label.length > 80) return '请填写子字段名称（最多 80 个字符）';
      if (!['TEXT', 'NUMBER'].includes(field.type)) return '子字段仅支持文本或数字';
      if (typeof field.required !== 'boolean') return '子字段必填配置无效';
      if (field.unit?.length > 20) return '单位最多 20 个字符';
    }
    return '';
  } catch { return '子字段配置格式错误'; }
}
