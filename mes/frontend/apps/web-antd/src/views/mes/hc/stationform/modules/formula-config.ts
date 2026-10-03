import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

type SchemaEditor = Record<string, unknown>;

// 只写入用户实际调整的配置；保留来源信息、运行布局及未显示的旧字段。
export function mergeFormulaSchema(
  original: string | undefined,
  baseline: SchemaEditor,
  current: SchemaEditor,
) {
  const changed = (key: string) => JSON.stringify(baseline[key]) !== JSON.stringify(current[key]);
  const modelChanged = ['modelScope', 'modelCode', 'modelPrefix'].some(changed);
  const managed = [
    'defaultGeneratedLength',
    'defaultRowCount',
    'headerFields',
    'headerLayout',
    'presetTemplate',
    'rowStep',
    'sourceModelCode',
    'version',
  ];
  if (!modelChanged && !managed.some(changed)) return original;
  // 配置损坏时拒绝覆写，不能把无法解析的配置当空对象保存。
  const schema = original ? JSON.parse(original) : {};
  if (!schema || typeof schema !== 'object' || Array.isArray(schema)) {
    throw new Error('原表单结构不是有效对象，请先在高级配置中核对');
  }
  const normalized = (value: unknown) =>
    String(value ?? '')
      .trim()
      .toUpperCase();
  if (modelChanged) {
    schema.modelScope = current.modelScope;
    for (const key of ['model', 'modelCode', 'modelCodePrefix', 'modelPrefix']) delete schema[key];
    if (current.modelScope === 'COMMON') schema.modelCode = 'COMMON';
    else if (current.modelScope === 'MODEL') schema.modelCode = normalized(current.modelCode);
    else schema.modelPrefix = normalized(current.modelPrefix);
  }
  for (const key of managed.filter(changed)) {
    let value = current[key];
    if (key === 'headerFields') {
      value = String(value ?? '')
        .split(/[\n,，]/)
        .map((field) => field.trim())
        .filter(Boolean);
    } else if (key === 'sourceModelCode') value = normalized(value);
    if (value === undefined || value === '' || (Array.isArray(value) && !value.length))
      delete schema[key];
    else schema[key] = value;
  }
  return JSON.stringify(schema, null, 2);
}

export function getFormulaCategory(form: MesHcStationFormApi.StationForm) {
  let schema: Record<string, unknown> = {};
  try {
    schema = JSON.parse(form.schemaJson || '{}');
  } catch {
    /* 保留旧编码兼容识别。 */
  }
  const code = String(form.formCode || '').toUpperCase();
  const name = form.formName || '';
  if (
    schema?.formulaCategory === 'production-check' ||
    code.startsWith('FORMULA_PROCESS_CHECK') ||
    name.includes('配料生产点检表')
  )
    return 'production';
  if (code.startsWith('FORMULA_STARTUP_CHECK') || name.includes('开机点检')) return 'startup';
  if (
    code.startsWith('FORMULA_CLEANING_CHECK') ||
    name.includes('清洁保养') ||
    name.includes('清洁点检')
  )
    return 'cleaning';
  return 'other';
}

export const FORMULA_VALUE_MODE_OPTIONS = [
  { label: '单值填写', value: 'TEXT' },
  { label: '双值填写', value: 'DUAL_TEXT' },
  { label: '多字段填写', value: 'MULTI_FIELDS' },
  { label: '时间文本', value: 'TIME' },
];

// 将旧“加入”隐式双值转换成界面可见的方式，之后用户可明确切换成单值。
export function normalizeFormulaInputModes(
  form: MesHcStationFormApi.StationForm,
  items: MesHcStationFormApi.StationFormItem[],
) {
  const schema = JSON.parse(form.schemaJson || '{}');
  if (schema.formulaInputModeVersion === 2 || getFormulaCategory(form) !== 'production')
    return items;
  return items.map((item) =>
    item.valueMode !== 'MULTI_FIELDS' && String(item.itemName || '').includes('加入') ? { ...item, valueMode: 'DUAL_TEXT' } : item,
  );
}
