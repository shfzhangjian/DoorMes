import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

export function isPressSlotProductionCheck(schema: Record<string, any>, processCode?: string) {
  return (processCode || schema.processCode) === 'PRESS_SLOT'
    && (schema.formType || schema.pressSlotFormType) === 'PRODUCTION_CHECK';
}

/** 兼容旧记录的 Java 日期数组；表单状态和再次提交始终使用字符串。 */
export function normalizeProductionCheckHeader(source: Record<string, any>) {
  const header = { ...source };
  for (const key of ['productionDate', 'recordDate', 'submitTime']) {
    const value = header[key];
    if (!Array.isArray(value)) continue;
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    const pad = (part: unknown) => String(part).padStart(2, '0');
    const date = `${year}-${pad(month)}-${pad(day)}`;
    header[key] = key === 'submitTime' ? `${date} ${pad(hour)}:${pad(minute)}:${pad(second)}` : date;
  }
  return header;
}

export function productionCheckRows(items: MesHcStationFormApi.StationFormItem[], presets: Record<string, any>[] = []) {
  return items.map((item, index) => {
    const preset = presets.find((row) => item.id && row.templateItemId === item.id)
      || presets.find((row) => row.itemName === item.itemName && Number(row.seq) === Number(item.itemSeq || index + 1));
    return ({
    templateItemId: item.id,
    fieldKey: `station_item_${item.id}`,
    seq: item.itemSeq || index + 1,
    sortNo: item.itemSeq || index + 1,
    itemCategory: item.itemCategory || '',
    itemName: item.itemName || '',
    standardText: item.standardText || '',
    valueMode: item.valueMode || 'TEXT',
    requiredFlag: item.requiredFlag,
    dualLabel1: item.dualLabel1,
    dualLabel2: item.dualLabel2,
    actualValue: preset?.actualValue ?? '', actualValue2: preset?.actualValue2 ?? '', abnormalRemark: preset?.abnormalRemark ?? '',
    resultFlag: preset?.resultFlag ?? (['OK', 'NG'].includes(item.defaultResult || '') ? item.defaultResult : ''),
  }); });
}

export function productionCheckSchema(source: Record<string, any> = {}) {
  const headerFields = [
    ['batchNo', '压槽片号', 'Input'], ['submitTime', '送检时间', 'DateTimePicker'],
    ['inspectionResult', '送检结果', 'Input'], ['planNo', '计划号', 'Input'],
    ['modelCode', '型号', 'Input'], ['materialCode', '料号', 'Input'],
    ['productionDate', '生产日期', 'DatePicker'], ['equipmentCode', '设备', 'Input'],
  ].map(([field, label, component]) => ({ field, label, component, editable: true }));
  const columns = [
    ['seq', '序号', false], ['itemCategory', '项目类别', false], ['itemName', '点检项目', false],
    ['standardText', '标准', false], ['actualValue', '记录值', true],
    ['resultFlag', '结果', true], ['abnormalRemark', '异常说明', true],
  ].map(([field, label, editable]) => ({ field, label, editable, component: field === 'resultFlag' ? 'Select' : editable ? 'Input' : 'text' }));
  const runtimeLayout = {
    ...source.runtimeLayout,
  };
  delete runtimeLayout.renderTree;
  delete runtimeLayout.componentModel;
  // 普通配置以表头字段、标题及布局为准；运行区域只承担填写展示。
  const configuredFields = Array.isArray(source.headerFields)
    ? source.headerFields
    : typeof source.headerFields === 'string'
      ? source.headerFields.split(/[\n,，]/).map((field: string) => field.trim()).filter(Boolean)
      : headerFields.map((item) => item.field);
  const oldHeader = (runtimeLayout.sections || []).find((section: Record<string, any>) => section.key === 'formInfo');
  const fields = configuredFields.map((field: string) => {
    const old = oldHeader?.fields?.find((item: Record<string, any>) => item.field === field);
    const defaults = headerFields.find((item) => item.field === field);
    return { ...defaults, ...old, field, label: source.headerLabels?.[field] || old?.label || defaults?.label || field };
  });
  if (runtimeLayout.sections) {
    runtimeLayout.sections = runtimeLayout.sections.map((section: Record<string, any>) => section.key === 'formInfo'
      ? { ...section, fields, columns: Number(String(source.headerLayout || '').replace('GRID_', '')) || section.columns || 4 }
      : section);
  }
  return {
    ...source,
    headerFields: configuredFields,
    modelScope: source.modelScope || 'PREFIX', allowCommonFallback: true,
    modelMatch: { source: '$route.modelCode', ...source.modelMatch, fallback: 'COMMON', priority: ['MODEL', 'PREFIX', 'COMMON'] },
    processCode: 'PRESS_SLOT', formType: 'PRODUCTION_CHECK', runtimeEngine: 'STATION_FORM_RUNTIME_LAYOUT',
    runtimeLayout: {
      title: '压槽生产点检表',
      sections: [
        { key: 'formInfo', type: 'headerGrid', title: '生产点检', fields, columns: Number(String(source.headerLayout || '').replace('GRID_', '')) || 4 },
        { key: 'intermediateDetails', type: 'editableTable', title: '生产点检明细', columns, allowInsert: false, allowDelete: false },
      ],
      ...runtimeLayout,
    },
  };
}
