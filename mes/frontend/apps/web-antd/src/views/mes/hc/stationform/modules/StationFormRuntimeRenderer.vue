<script lang="ts" setup>
import { multiFields, fieldLabel, readFormulaValue, writeFormulaValue } from '#/views/mes/hc/stationform/modules/formula-multi-fields';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, ref, watch } from 'vue';

import {
  Button,
  DatePicker,
  Empty,
  Input,
  InputNumber,
  Radio,
  RadioGroup,
  Select,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { isPressSlotProductionCheck, normalizeProductionCheckHeader } from '#/views/mes/hc/shared/pressSlotProductionCheck';

defineOptions({ name: 'HcStationFormRuntimeRenderer' });

const props = withDefaults(
  defineProps<{
    compact?: boolean;
    formName?: string;
    headerData?: Record<string, any>;
    items?: MesHcStationFormApi.StationFormItem[];
    readonly?: boolean;
    readonlyPassWorkHeader?: boolean;
    recordMeta?: string[];
    schema?: Record<string, any>;
  }>(),
  {
    compact: false,
    formName: '',
    headerData: () => ({}),
    items: () => [],
    readonly: false,
    readonlyPassWorkHeader: false,
    recordMeta: () => [],
    schema: () => ({}),
  },
);

const emit = defineEmits<{
  openAttachment: [attachment: SheetAttachment];
  'update:headerData': [value: Record<string, any>];
}>();

type RuntimeField = Record<string, any> & {
  component?: string;
  editable?: boolean;
  field?: string;
  key?: string;
  label?: string;
  standard?: string;
  type?: string;
};

type RuntimeSection = Record<string, any> & {
  columns?: number | RuntimeField[];
  fields?: RuntimeField[];
  key?: string;
  rowKey?: string;
  title?: string;
  type?: string;
};

type RuntimeRenderNode = Record<string, any> & {
  binding?: Record<string, any>;
  children?: RuntimeRenderNode[];
  component?: string;
  id?: string;
  props?: Record<string, any>;
  slots?: Record<string, RuntimeRenderNode[]>;
  type?: string;
};

type RuntimeRecordItem = MesHcProcessFormApi.RecordItem & {
  sourceKind?: 'detail' | 'field';
};

type ExcelLayoutPurpose = 'export' | 'import';

type ImportApplySummary = {
  appliedCellCount: number;
  rowCount: number;
  skippedRowCount: number;
  totalCellCount: number;
};

type SheetAttachment = {
  name?: string;
  path?: string;
  size?: number;
  uploadTime?: string;
  url?: string;
};

type FormulaProductionVisualRow = {
  actualField: string;
  category: string;
  id: number | string;
  item: string;
  node: string;
  recordLabel: string;
  source: Record<string, any>;
  sourceIndex: number;
  sourceRowId: string;
  standard: string;
};

const HIDDEN_RESULT_DETAIL_FIELD_KEYS = new Set([
  'checkResult',
  'inspectionResult',
  'judgementResult',
  'judgeResult',
  'judgmentResult',
  'result',
  'resultFlag',
  'status',
]);
const HIDDEN_RESULT_DETAIL_LABELS = new Set([
  '判定',
  '判定结果',
  '检查结果',
  '检验结果',
  '点检结果',
  '结果',
]);
const RESULT_OPTIONS = [
  { label: 'OK', value: 'OK' },
  { label: 'NG', value: 'NG' },
];
const DEFAULT_DATE_TIME_VALUE = dayjs('2000-01-01 00:00:00');
const CHECK_RESULT_DETAIL_FIELD: RuntimeField = {
  component: 'Select',
  defaultValue: 'OK',
  editable: true,
  field: 'resultFlag',
  label: 'OK/NG',
  width: 120,
};
const CHECK_FORM_TYPES = new Set([
  'CLEANING_CHECK',
  'MAINTENANCE_CHECK',
  'STARTUP_CHECK',
]);
const CHECK_FORM_KEYWORDS = [
  '开机点检',
  '清洁点检',
  '清洁保养',
  '设备清洁点检',
  '保养点检',
];
const CHECK_RESULT_REMARK_TIP =
  '开机、清洁保养时遇到问题请记录在备注列说明情况。';

const localHeader = ref<Record<string, any>>(cloneHeaderData(props.headerData));

watch(
  () => props.headerData,
  (value) => {
    localHeader.value = cloneHeaderData(value || {});
  },
  { deep: true, immediate: true },
);

const isPressSlotProduction = computed(() => props.schema?.processCode === 'PRESS_SLOT' && props.schema?.formType === 'PRODUCTION_CHECK');

const runtimeLayout = computed<Record<string, any>>(
  () => props.schema?.runtimeLayout || {},
);
const sourceExcelImport = computed<Record<string, any>>(() => {
  const config =
    runtimeLayout.value.excelImport ||
    runtimeLayout.value.sourceExcelImport ||
    {};
  return config && typeof config === 'object' && !Array.isArray(config)
    ? config
    : {};
});
const sourceExcelImportDetail = computed<Record<string, any>>(() => {
  const detail =
    sourceExcelImport.value.detail || sourceExcelImport.value.detailTable || {};
  return detail && typeof detail === 'object' && !Array.isArray(detail)
    ? detail
    : sourceExcelImport.value;
});
const shouldUseSourceExcelImport = computed(
  () =>
    sourceExcelImport.value.enabled === true ||
    sourceExcelImport.value.fillDetailFromSource === true,
);
const runtimeSections = computed<RuntimeSection[]>(() =>
  mergeRuntimeSectionsWithRenderTree(
    Array.isArray(runtimeLayout.value.sections)
      ? runtimeLayout.value.sections
      : [],
    deriveRuntimeSectionsFromRenderTree(runtimeLayout.value.renderTree),
  ),
);
const displayTitle = computed(
  () =>
    runtimeLayout.value.title ||
    props.schema?.displayName ||
    props.formName ||
    '动态表单',
);
const displayMeta = computed(() => (props.recordMeta || []).filter(Boolean));
const formInfoSection = computed(() => findSection('formInfo', 'headerGrid'));
const formInfoColumns = computed(() => {
  const configured = formInfoSection.value?.columns;
  const columns = typeof configured === 'number' || typeof configured === 'string'
    ? Number(configured) : NaN;
  if (Number.isInteger(columns) && columns > 0) return columns;
  const layout = /^GRID_(\d+)$/i.exec(String(props.schema?.headerLayout || ''));
  const fallback = Number(layout?.[1]);
  return Number.isInteger(fallback) && fallback > 0 ? fallback : 3;
});
const depthSection = computed(() => findSection('firstSlotDepth', 'matrix'));
const attachmentSection = computed(() =>
  findSection('attachments', 'attachmentList'),
);
const detailSection = computed(() =>
  findSection('intermediateDetails', 'editableTable'),
);
const signatureSection = computed(() =>
  findSection('signature', 'signatureGrid'),
);
const formInfoFields = computed(() =>
  ensureArray<RuntimeField>(formInfoSection.value?.fields),
);
const depthFields = computed(() =>
  ensureArray<RuntimeField>(depthSection.value?.fields),
);
const signatureFields = computed(() =>
  ensureArray<RuntimeField>(signatureSection.value?.fields),
);
const isStartupCleaningMaintenanceRuntime = computed(() => {
  const schema = props.schema || {};
  const formType = firstText(
    schema.formType,
    schema.processFormType,
    schema.formTypeCode,
    localHeader.value.formType,
    localHeader.value.processFormType,
  );
  if (CHECK_FORM_TYPES.has(formType)) return true;
  const text = [
    props.formName,
    schema.displayName,
    schema.formName,
    schema.formTypeName,
    schema.templateName,
    schema.title,
    localHeader.value.formName,
    localHeader.value.formTypeName,
    localHeader.value.templateName,
    detailSection.value?.title,
  ]
    .filter(Boolean)
    .join(' ');
  return CHECK_FORM_KEYWORDS.some((keyword) => text.includes(keyword));
});
const detailColumns = computed(() => {
  const columns = ensureArray<RuntimeField>(
    detailSection.value?.columns,
  ).filter((column) => !shouldHideDetailColumn(column));
  // 压槽生产点检只显示记录值和异常说明，历史结果保留在行数据中。
  if (isPressSlotProduction.value) return columns.filter((column) => !isResultDetailField(column));
  if (
    !isStartupCleaningMaintenanceRuntime.value ||
    columns.some((column) => isResultDetailField(column))
  ) {
    return columns;
  }
  const remarkIndex = columns.findIndex((column) =>
    isRemarkDetailField(column),
  );
  const resultColumn = { ...CHECK_RESULT_DETAIL_FIELD };
  if (remarkIndex !== -1) {
    return [
      ...columns.slice(0, remarkIndex),
      resultColumn,
      ...columns.slice(remarkIndex),
    ];
  }
  return [...columns, resultColumn];
});
const detailRows = computed<Record<string, any>[]>(() => {
  const rows = ensureHeaderArray<Record<string, any>>('previewDetails');
  if (rows.length > 0) return rows;
  const fallbackRows = buildDetailRowsFromItems();
  if (!props.readonly && fallbackRows.length > 0) {
    rows.push(...fallbackRows);
    return rows;
  }
  return fallbackRows;
});
const attachments = computed<SheetAttachment[]>(() =>
  ensureHeaderArray('attachments'),
);
const shouldShowAttachmentSection = computed(
  () =>
    !!attachmentSection.value &&
    (attachments.value.length > 0 ||
      attachmentSection.value?.showWhenEmpty === true),
);
const runtimeLayoutClass = computed(() =>
  String(runtimeLayout.value.layoutClass || '').trim(),
);
const isFaiSelfCheckRuntime = computed(() =>
  runtimeLayoutClass.value
    .split(/\s+/u)
    .some((className) =>
      ['cut-round-fqc-self-check-runtime', 'fai-self-check-runtime'].includes(
        className,
      ),
    ),
);
const formulaVisualMode = computed(() => {
  const mode = firstText(
    props.schema?.formulaCategory,
    props.schema?.wetCategory,
    props.schema?.roughCategory,
    runtimeLayout.value.visualMode,
  ).toLowerCase();
  if (mode.includes('startup')) return 'startup-check';
  if (mode.includes('cleaning')) return 'cleaning-check';
  if (
    mode.includes('production') ||
    mode.includes('process') ||
    mode.includes('param') ||
    mode.includes('工艺')
  ) {
    return 'production-check';
  }
  return mode;
});
const isFormulaPassWorkRuntime = computed(() =>
  ['cleaning-check', 'production-check', 'startup-check'].includes(
    formulaVisualMode.value,
  ),
);
const runtimeScrollMode = computed(() => {
  const mode = firstText(
    runtimeLayout.value.scrollMode,
    runtimeLayout.value.scroll?.mode,
  ).toLowerCase();
  if (['body', 'detail', 'page'].includes(mode)) return mode;
  return isFormulaPassWorkRuntime.value ? 'detail' : 'page';
});
const runtimeRootClass = computed(() => [
  runtimeLayoutClass.value,
  `station-form-runtime--scroll-${runtimeScrollMode.value}`,
  { 'station-form-runtime--compact': props.compact },
]);
const runtimeRootStyle = computed(() =>
  buildRuntimeStyle(runtimeLayout.value.rootStyle, {
    height: firstText(
      runtimeLayout.value.height,
      runtimeLayout.value.scroll?.height,
    ),
    minHeight: firstText(
      runtimeLayout.value.minHeight,
      runtimeLayout.value.scroll?.minHeight,
    ),
  }),
);
const runtimeBodyStyle = computed(() =>
  buildRuntimeStyle(runtimeLayout.value.bodyStyle, {
    maxHeight: firstText(
      runtimeLayout.value.bodyMaxHeight,
      runtimeLayout.value.scroll?.bodyMaxHeight,
    ),
    minHeight: firstText(
      runtimeLayout.value.bodyMinHeight,
      runtimeLayout.value.scroll?.bodyMinHeight,
    ),
  }),
);
const runtimeDetailWrapStyle = computed(() =>
  buildRuntimeStyle(runtimeLayout.value.detailStyle, {
    height: firstText(
      runtimeLayout.value.detailHeight,
      runtimeLayout.value.scroll?.detailHeight,
    ),
    maxHeight: firstText(
      runtimeLayout.value.detailMaxHeight,
      runtimeLayout.value.scroll?.detailMaxHeight,
    ),
    minHeight: firstText(
      runtimeLayout.value.detailMinHeight,
      runtimeLayout.value.scroll?.detailMinHeight,
    ),
  }),
);
const runtimeFormulaDetailStyle = computed(() =>
  buildRuntimeStyle(runtimeLayout.value.detailFieldsetStyle, {
    height: firstText(
      runtimeLayout.value.detailFieldsetHeight,
      runtimeLayout.value.detailHeight,
      runtimeLayout.value.scroll?.detailHeight,
    ),
    maxHeight: firstText(
      runtimeLayout.value.detailFieldsetMaxHeight,
      runtimeLayout.value.detailMaxHeight,
      runtimeLayout.value.scroll?.detailMaxHeight,
    ),
    minHeight: firstText(
      runtimeLayout.value.detailFieldsetMinHeight,
      runtimeLayout.value.detailMinHeight,
      runtimeLayout.value.scroll?.detailMinHeight,
    ),
  }),
);
const runtimeFormulaTableWrapStyle = computed(() =>
  buildRuntimeStyle(runtimeLayout.value.detailStyle, {
    height: firstText(
      runtimeLayout.value.detailTableHeight,
      runtimeLayout.value.scroll?.detailTableHeight,
    ),
    maxHeight: firstText(
      runtimeLayout.value.detailTableMaxHeight,
      runtimeLayout.value.scroll?.detailTableMaxHeight,
    ),
    minHeight: firstText(
      runtimeLayout.value.detailTableMinHeight,
      runtimeLayout.value.scroll?.detailTableMinHeight,
    ),
  }),
);
const isFormulaDetailFixed = computed(() =>
  runtimeBoolean(
    firstConfigValue(
      runtimeLayout.value.detailFixed,
      runtimeLayout.value.detailFieldsetFixed,
      runtimeLayout.value.scroll?.detailFixed,
    ),
    false,
  ),
);
const hasFormulaDetailHeight = computed(
  () =>
    !!firstText(
      runtimeLayout.value.detailFieldsetHeight,
      runtimeLayout.value.detailHeight,
      runtimeLayout.value.detailFieldsetMaxHeight,
      runtimeLayout.value.detailMaxHeight,
      runtimeLayout.value.scroll?.detailHeight,
      runtimeLayout.value.scroll?.detailMaxHeight,
    ),
);
const runtimeAttachmentStyle = computed(() => {
  const emptyHeight = firstText(
    runtimeLayout.value.emptyAttachmentHeight,
    runtimeLayout.value.attachmentHeight,
    runtimeLayout.value.scroll?.emptyAttachmentHeight,
    runtimeLayout.value.scroll?.attachmentHeight,
    '112px',
  );
  return buildRuntimeStyle(runtimeLayout.value.attachmentStyle, {
    height:
      attachments.value.length > 0
        ? firstText(
            runtimeLayout.value.attachmentHeight,
            runtimeLayout.value.scroll?.attachmentHeight,
          )
        : emptyHeight,
    maxHeight: firstText(
      runtimeLayout.value.attachmentMaxHeight,
      runtimeLayout.value.scroll?.attachmentMaxHeight,
    ),
    minHeight: firstText(
      runtimeLayout.value.attachmentMinHeight,
      runtimeLayout.value.scroll?.attachmentMinHeight,
    ),
  });
});
const isFormulaStartupRuntime = computed(
  () => formulaVisualMode.value === 'startup-check',
);
const isFormulaCleaningRuntime = computed(
  () => formulaVisualMode.value === 'cleaning-check',
);
const formulaPassWorkHeaderFields = computed(() => [
  {
    field: 'mixerEquipmentCode',
    component: 'Text',
    editable: false,
    label: '搅拌机台号',
    value: firstText(
      localHeader.value.mixerEquipmentCode,
      localHeader.value.equipmentCode,
      localHeader.value.equipmentName,
    ),
  },
  {
    field: 'foamingEquipmentCode',
    component: 'Text',
    editable: false,
    label: '脱泡机台号',
    value: firstText(
      localHeader.value.foamingEquipmentCode,
      localHeader.value.defoamingEquipmentCode,
    ),
  },
  {
    field: 'recorder',
    component: 'Text',
    editable: false,
    label: '记录人',
    value: firstText(
      localHeader.value.recorder,
      localHeader.value.recorderName,
      localHeader.value.checkerName,
    ),
  },
  {
    field: 'confirmer',
    component: 'Text',
    editable: false,
    label: '确认人',
    value: firstText(
      localHeader.value.confirmer,
      localHeader.value.confirmerName,
    ),
  },
  {
    field: 'recorderTime',
    component: 'Text',
    editable: false,
    label: '记录时间',
    value: firstText(
      localHeader.value.recorderTime,
      localHeader.value.recordTime,
      localHeader.value.checkerTime,
    ),
  },
]);
const passWorkHeaderFields = computed(() => {
  const configuredFields = ensureArray<RuntimeField>(
    runtimeLayout.value.passWorkHeaderFields,
  );
  if (configuredFields.length === 0) return formulaPassWorkHeaderFields.value;
  return configuredFields.map((field) => {
    const key = fieldKey(field);
    return {
      ...field,
      field: key || String(field.label || ''),
      label: fieldLabel(field),
      value: readPassWorkHeaderValue(field),
    };
  });
});
const passWorkHeaderTitle = computed(() =>
  firstText(runtimeLayout.value.passWorkHeaderTitle, '表单信息'),
);
const passWorkHeaderColumns = computed(() => {
  const raw = Number(
    firstText(
      runtimeLayout.value.passWorkHeaderColumns,
      runtimeLayout.value.passWorkHeader?.columns,
      5,
    ),
  );
  return Number.isFinite(raw) && raw > 0 ? Math.floor(raw) : 5;
});
const passWorkHeaderCompact = computed(() => {
  const value =
    runtimeLayout.value.passWorkHeaderCompact ??
    runtimeLayout.value.passWorkHeader?.compact;
  return (
    value === true || value === 1 || String(value).toLowerCase() === 'true'
  );
});
const passWorkActionMode = computed(() =>
  firstText(
    runtimeLayout.value.passWorkActionMode,
    runtimeLayout.value.actionMode,
    'default',
  ).toLowerCase(),
);
const isCompactPassWorkAction = computed(
  () => passWorkActionMode.value === 'execution-confirm',
);
const showPassWorkConfirmAction = computed(
  () => runtimeLayout.value.showConfirmAction !== false,
);
const passWorkActionCollapsible = computed(() =>
  runtimeBoolean(runtimeLayout.value.passWorkActionCollapsible, false),
);
const passWorkActionDefaultCollapsed = computed(() =>
  runtimeBoolean(runtimeLayout.value.passWorkActionDefaultCollapsed, false),
);
const passWorkActionCollapsed = ref(false);
watch(
  passWorkActionDefaultCollapsed,
  (value) => {
    passWorkActionCollapsed.value = value;
  },
  { immediate: true },
);
const passWorkDetailTitle = computed(() =>
  firstText(
    runtimeLayout.value.detailTitle,
    detailSection.value?.title,
    '明细项目',
  ),
);
const passWorkProductionColumnLabels = computed<Record<string, string>>(() => {
  const configured = runtimeLayout.value.passWorkProductionColumns;
  return configured &&
    typeof configured === 'object' &&
    !Array.isArray(configured)
    ? (configured as Record<string, string>)
    : {};
});
const passWorkProductionColumnWidths = computed<Record<string, unknown>>(() => {
  const configured = runtimeLayout.value.passWorkProductionColumnWidths;
  return configured &&
    typeof configured === 'object' &&
    !Array.isArray(configured)
    ? (configured as Record<string, unknown>)
    : {};
});
const showFormulaRecordLabelColumn = computed(
  () => runtimeLayout.value.showRecordLabelColumn !== false,
);
const formulaProductionRows = computed<FormulaProductionVisualRow[]>(() =>
  detailRows.value.flatMap((row, index) => {
    const sourceRowId = String(
      row.id || row.templateItemId || row.seq || row.itemSeq || `row-${index}`,
    );
    const base = {
      category: formulaRowCategory(row),
      id: formulaRowSeq(row, index),
      item: formulaRowItem(row),
      node: formulaRowNode(row),
      source: row,
      sourceIndex: index,
      sourceRowId,
      standard: formulaRowStandard(row),
    };
    if (row.valueMode === 'MULTI_FIELDS') {
      return multiFields(row).map((field) => ({ ...base, actualField: `multi:${field.key}`, recordLabel: fieldLabel(field) }));
    }
    if (!isFormulaDualValueRow(row)) {
      return [
        {
          ...base,
          actualField: 'actualValue' as const,
          recordLabel: '',
        },
      ];
    }
    return [
      {
        ...base,
        actualField: 'actualValue' as const,
        recordLabel: firstText(row.dualLabel1, '重量'),
      },
      {
        ...base,
        actualField: 'actualValue2' as const,
        recordLabel: firstText(row.dualLabel2, '批号'),
      },
    ];
  }),
);
const canInsertDetailRows = computed(
  () => !props.readonly && detailSection.value?.allowInsert !== false,
);
const canDeleteDetailRows = computed(
  () => !props.readonly && detailSection.value?.allowDelete !== false,
);
const showDetailActions = computed(
  () => canInsertDetailRows.value || canDeleteDetailRows.value,
);
const hasRuntimeLayout = computed(
  () =>
    formInfoFields.value.length > 0 ||
    depthFields.value.length > 0 ||
    shouldShowAttachmentSection.value ||
    detailColumns.value.length > 0 ||
    signatureFields.value.length > 0,
);

function cloneHeaderData(value: Record<string, any>) {
  const header = cloneObject(value || {});
  return isPressSlotProductionCheck(props.schema || {})
    ? normalizeProductionCheckHeader(header)
    : header;
}

function cloneObject<T>(value: T): T {
  if (value === undefined || value === null) return {} as T;
  try {
    return structuredClone(value) as T;
  } catch {
    return value;
  }
}

function emitHeaderData() {
  emit('update:headerData', cloneObject(localHeader.value));
}

function ensureArray<T = any>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

function mergeRuntimeSectionsWithRenderTree(
  configuredSections: RuntimeSection[],
  derivedSections: RuntimeSection[],
) {
  if (derivedSections.length === 0) return configuredSections;
  if (configuredSections.length === 0) return derivedSections;
  const merged = configuredSections.map((section) => ({ ...section }));
  derivedSections.forEach((derived) => {
    const index = merged.findIndex(
      (section) => section.key === derived.key || section.type === derived.type,
    );
    if (index === -1) {
      merged.push(derived);
      return;
    }
    const target = merged[index];
    if (
      ensureArray(target.fields).length === 0 &&
      ensureArray(derived.fields).length > 0
    ) {
      target.fields = derived.fields;
    }
    if (
      ensureArray(target.columns).length === 0 &&
      ensureArray(derived.columns).length > 0
    ) {
      target.columns = derived.columns;
    }
    target.title = target.title || derived.title;
    target.columns = target.columns || derived.columns;
    target.storage = target.storage || derived.storage;
    target.rowKey = target.rowKey || derived.rowKey;
  });
  return merged;
}

function deriveRuntimeSectionsFromRenderTree(root?: RuntimeRenderNode) {
  if (!root || typeof root !== 'object') return [];
  const sections: RuntimeSection[] = [];
  const consumedFieldPaths = new Set<string>();

  const visit = (node: RuntimeRenderNode) => {
    const section = createRuntimeSectionFromRenderNode(node);
    if (section) {
      sections.push(section);
      ensureArray<RuntimeField>(section.fields).forEach((field) => {
        const key = fieldKey(field);
        if (key) consumedFieldPaths.add(key);
      });
      return;
    }
    renderNodeChildren(node).forEach((child) => visit(child));
  };
  visit(root);

  const looseFields = collectRenderFieldNodes(root)
    .map((node, index) => renderFieldNodeToRuntimeField(node, index))
    .filter(
      (field) => fieldKey(field) && !consumedFieldPaths.has(fieldKey(field)),
    );
  if (
    looseFields.length > 0 &&
    !sections.some(
      (section) => section.key === 'formInfo' || section.type === 'headerGrid',
    )
  ) {
    sections.unshift({
      columns: 3,
      fields: looseFields,
      key: 'formInfo',
      title: '表单信息',
      type: 'headerGrid',
    });
  }
  return sections;
}

function createRuntimeSectionFromRenderNode(
  node: RuntimeRenderNode,
): RuntimeSection | undefined {
  const type = String(node.type || '');
  if (!['Border', 'Grid', 'Panel', 'TableRegion'].includes(type))
    return undefined;
  const directChildren = renderNodeChildren(node);
  if (
    type === 'Grid' &&
    !directChildren.some((child) => child.type === 'Field') &&
    directChildren.some((child) =>
      ['Border', 'Panel', 'TableRegion'].includes(String(child.type || '')),
    )
  ) {
    return undefined;
  }
  const descendants = collectRenderNodes(node);
  const title = firstText(node.props?.title, node.props?.label, node.id);
  const attachment = descendants.find((item) => item.type === 'AttachmentList');
  if (attachment || title.includes('附件')) {
    return {
      key: 'attachments',
      showWhenEmpty: true,
      storage: 'presetHeaderDataJson.attachments',
      title: title || '原始导入附件',
      type: 'attachmentList',
    };
  }

  const editableTable = descendants.find(
    (item) => item.type === 'EditableTable',
  );
  if (editableTable || type === 'TableRegion') {
    return {
      columns: defaultEditableTableColumns(),
      key: 'intermediateDetails',
      rowKey: 'itemName',
      storage: 'presetHeaderDataJson.previewDetails',
      title: title || '明细项目',
      type: 'editableTable',
    };
  }

  const signature = descendants.find((item) => item.type === 'SignatureGrid');
  if (signature || title.includes('签名')) {
    return {
      fields: defaultSignatureFields(),
      key: 'signature',
      title: title || '签名区',
      type: 'signatureGrid',
    };
  }

  const fields = (type === 'Grid' ? directChildren : descendants)
    .filter((item) => item.type === 'Field')
    .map((field, index) => renderFieldNodeToRuntimeField(field, index))
    .filter((field) => fieldKey(field));
  if (fields.length === 0) return undefined;
  const isMatrix = title.includes('槽深') || title.includes('矩阵');
  let sectionKey = firstText(node.id, `section${sectionsHash(title)}`);
  if (isMatrix) {
    sectionKey = 'firstSlotDepth';
  } else if (title.includes('表单')) {
    sectionKey = 'formInfo';
  }
  return {
    columns: Number(node.props?.columns) || 3,
    fields,
    key: sectionKey,
    title: title || (isMatrix ? '矩阵区' : '表单信息'),
    type: isMatrix ? 'matrix' : 'headerGrid',
  };
}

function renderNodeChildren(node?: RuntimeRenderNode) {
  return ensureArray<RuntimeRenderNode>(node?.slots?.default || node?.children);
}

function collectRenderNodes(root: RuntimeRenderNode) {
  const nodes: RuntimeRenderNode[] = [];
  const visit = (node: RuntimeRenderNode) => {
    nodes.push(node);
    renderNodeChildren(node).forEach((child) => visit(child));
  };
  visit(root);
  return nodes;
}

function collectRenderFieldNodes(root: RuntimeRenderNode) {
  return collectRenderNodes(root).filter((node) => node.type === 'Field');
}

function renderFieldNodeToRuntimeField(
  node: RuntimeRenderNode,
  index: number,
): RuntimeField {
  const field = firstText(
    node.binding?.path,
    node.props?.field,
    node.id,
    `field${index + 1}`,
  );
  return {
    component: firstText(node.component, node.props?.component, 'Input'),
    editable: node.props?.editable,
    field,
    label: firstText(node.props?.label, node.props?.title, field),
    storage: `presetHeaderDataJson.${field}`,
    width: node.props?.width,
  };
}

function defaultEditableTableColumns(): RuntimeField[] {
  return [
    { component: 'Text', field: 'itemName', label: '项目名称', width: 220 },
    { component: 'Text', field: 'standardText', label: '标准', width: 260 },
    {
      component: 'Input',
      field: 'actualValue',
      label: '实际/记录',
      width: 180,
    },
    { component: 'Input', field: 'remark', label: '备注', width: 180 },
  ];
}

function defaultSignatureFields(): RuntimeField[] {
  return [
    { component: 'Text', field: 'recorder', label: '记录人' },
    { component: 'Text', field: 'recorderTime', label: '记录时间' },
    { component: 'Text', field: 'confirmer', label: '确认人' },
    { component: 'Text', field: 'confirmerTime', label: '确认时间' },
  ];
}

function sectionsHash(value: string) {
  return (
    String(value || 'section')
      .replaceAll(/\W/g, '')
      .slice(0, 24) || 'section'
  );
}

function ensureHeaderArray<T = any>(key: string): T[] {
  if (!Array.isArray(localHeader.value[key])) {
    localHeader.value[key] = [];
  }
  return localHeader.value[key] as T[];
}

function buildDetailRowsFromItems() {
  return props.items.map((item, index) => ({
    actualValue:
      item.defaultResult && item.defaultResult !== 'OK'
        ? item.defaultResult
        : '',
    actualValue2: '',
    abnormalRemark: '',
    category: item.itemCategory || '',
    dualLabel1: item.dualLabel1 || '',
    dualLabel2: item.dualLabel2 || '',
    fieldDefinitionsJson: item.fieldDefinitionsJson,
    fieldValuesJson: item.fieldValuesJson,
    id: item.id,
    item: item.itemName || '',
    itemCategory: item.itemCategory || '',
    itemName: item.itemName || '',
    node: item.stepNode || '',
    remark: item.remark || '',
    resultFlag: item.defaultResult || (isPressSlotProduction.value ? '' : 'OK'),
    seq: item.itemSeq || index + 1,
    sortNo: item.itemSeq || index + 1,
    standard: item.standardText || '-',
    standardText: item.standardText || '-',
    status: item.defaultResult || (isPressSlotProduction.value ? '' : 'OK'),
    stepNode: item.stepNode || '',
    templateItemId: item.id,
    fieldKey: `station_item_${item.id}`,
    requiredFlag: item.requiredFlag,
    valueMode: item.valueMode || 'TEXT',
  }));
}

function formulaRowSeq(row: Record<string, any>, index: number) {
  return row.seq || row.itemSeq || row.id || index + 1;
}

function formulaRowCategory(row: Record<string, any>) {
  return firstText(row.itemCategory, row.category);
}

function formulaRowNode(row: Record<string, any>) {
  return firstText(row.stepNode, row.node);
}

function formulaRowItem(row: Record<string, any>) {
  return firstText(row.itemName, row.item, row.fieldLabel);
}

function formulaRowStandard(row: Record<string, any>) {
  return firstText(row.standardText, row.standard);
}

function formulaRowActual(
  row: Record<string, any>,
  field: string = 'actualValue',
) {
  return firstText(
    readFormulaValue(row, field),
    field === 'actualValue' ? row.recordValue : undefined,
    field === 'actualValue' ? row.value : undefined,
  );
}

function formulaRowResult(row: Record<string, any>) {
  return firstText(row.resultFlag, row.status, row.result, 'OK');
}

function formulaRowRemark(row: Record<string, any>) {
  return firstText(row.abnormalRemark, row.remark);
}

function updateFormulaActual(
  row: Record<string, any>,
  field: string,
  value: any,
) {
  writeFormulaValue(row, field, value);
  emitHeaderData();
}

function updateFormulaResult(row: Record<string, any>, value: any) {
  row.resultFlag = value || 'OK';
  row.status = value || 'OK';
  row.result = value || 'OK';
  row.checkResult = value || 'OK';
  emitHeaderData();
}

function updateFormulaRemark(row: Record<string, any>, value: any) {
  row.abnormalRemark = value ?? '';
  row.remark = value ?? '';
  emitHeaderData();
}

function readHeaderKey(key: string, fallback = '') {
  return firstText(localHeader.value[key], fallback);
}

function writeHeaderKey(key: string, value: any) {
  localHeader.value[key] = value ?? '';
  emitHeaderData();
}

function isFormulaDualValueRow(row: Record<string, any>) {
  const valueMode = String(row.valueMode || '').toUpperCase();
  return (
    valueMode === 'DUAL_TEXT' ||
    formulaRowItem(row).includes('加入') ||
    !!row.actualValue2
  );
}

function isFormulaTimeValueRow(row: Record<string, any>) {
  return String(row.valueMode || '').toUpperCase() === 'TIME';
}

function formulaInputPlaceholder(
  row: Record<string, any>,
  field: string,
) {
  if (field.startsWith('multi:')) return multiFields(row).find((f) => `multi:${f.key}` === field)?.label || '';
  if (isFormulaDualValueRow(row)) {
    return field === 'actualValue'
      ? firstText(row.dualLabel1, '重量')
      : firstText(row.dualLabel2, '批号');
  }
  if (isFormulaTimeValueRow(row)) return 'HH:mm';
  return formulaRowStandard(row);
}

function getFormulaCleaningCategoryRowSpan(index: number) {
  const rows = detailRows.value;
  const current = rows[index];
  if (!current) return 1;
  const currentValue = formulaRowCategory(current);
  if (index > 0 && formulaRowCategory(rows[index - 1]) === currentValue)
    return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (formulaRowCategory(rows[cursor]) === currentValue) span += 1;
    else break;
  }
  return span;
}

function getFormulaProductionSourceRowSpan(index: number) {
  const rows = formulaProductionRows.value;
  const current = rows[index];
  if (!current) return 1;
  if (index > 0 && rows[index - 1]?.sourceRowId === current.sourceRowId)
    return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (rows[cursor]?.sourceRowId === current.sourceRowId) span += 1;
    else break;
  }
  return span;
}

function getFormulaProductionFieldRowSpan(
  index: number,
  field: 'category' | 'item' | 'node',
) {
  const rows = formulaProductionRows.value;
  const current = rows[index];
  if (!current) return 1;
  if (index > 0 && rows[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < rows.length; cursor += 1) {
    if (rows[cursor]?.[field] === current[field]) span += 1;
    else break;
  }
  return span;
}

function isFormulaProductionStepStart(index: number) {
  const rows = formulaProductionRows.value;
  const current = rows[index];
  return !!current && (index === 0 || rows[index - 1]?.node !== current.node);
}

function findSection(key: string, type?: string) {
  return runtimeSections.value.find(
    (section) =>
      section.key === key ||
      section.type === key ||
      (type && section.type === type),
  );
}

function fieldKey(field?: RuntimeField) {
  return String(field?.field || field?.key || field?.bindKey || '').trim();
}

function isHiddenResultDetailField(field?: RuntimeField) {
  const label = String(field?.label || field?.title || '')
    .replaceAll(/\s+/gu, '')
    .trim();
  return (
    HIDDEN_RESULT_DETAIL_FIELD_KEYS.has(fieldKey(field)) ||
    HIDDEN_RESULT_DETAIL_LABELS.has(label)
  );
}

function isResultDetailField(field?: RuntimeField) {
  return isHiddenResultDetailField(field);
}

function isActualRecordDetailField(field?: RuntimeField) {
  const key = fieldKey(field);
  const label = String(field?.label || field?.title || '')
    .replaceAll(/\s+/gu, '')
    .trim();
  return (
    ['actualText', 'actualValue', 'actualValue2', 'recordValue'].includes(
      key,
    ) || ['实际/记录', '记录值'].includes(label)
  );
}

function isRemarkDetailField(field?: RuntimeField) {
  const key = fieldKey(field);
  const label = String(field?.label || field?.title || '')
    .replaceAll(/\s+/gu, '')
    .trim();
  return (
    ['abnormalRemark', 'remark'].includes(key) ||
    ['备注', '异常备注', '异常说明'].includes(label)
  );
}

function shouldHideDetailColumn(field?: RuntimeField) {
  if (isPressSlotProduction.value) return false;
  if (isStartupCleaningMaintenanceRuntime.value)
    return isActualRecordDetailField(field);
  return isHiddenResultDetailField(field);
}

function isHiddenResultImportField(key?: string, label?: string) {
  const normalizedLabel = String(label || '')
    .replaceAll(/\s+/gu, '')
    .trim();
  return (
    HIDDEN_RESULT_DETAIL_FIELD_KEYS.has(String(key || '').trim()) ||
    HIDDEN_RESULT_DETAIL_LABELS.has(normalizedLabel)
  );
}

function shouldHideImportField(key?: string, label?: string) {
  if (isPressSlotProduction.value) return false;
  if (isStartupCleaningMaintenanceRuntime.value) {
    return isActualRecordDetailField({ field: key, label });
  }
  return isHiddenResultImportField(key, label);
}

function fieldLabel(field?: RuntimeField) {
  return String(field?.label || field?.title || fieldKey(field) || '字段');
}

function displayText(value: unknown, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function readObjectPath(source: Record<string, any>, path?: unknown) {
  const keys = String(path ?? '')
    .trim()
    .split('.')
    .filter(Boolean);
  if (keys.length === 0) return undefined;
  let value: unknown = source;
  for (const key of keys) {
    if (!value || typeof value !== 'object') return undefined;
    value = (value as Record<string, any>)[key];
  }
  return value;
}

function readPassWorkHeaderValue(field?: RuntimeField) {
  const sourceKeys = ensureArray<string>(field?.sources);
  const values = [
    ...sourceKeys.map((key) => readObjectPath(localHeader.value, key)),
    fieldKey(field)
      ? readObjectPath(localHeader.value, fieldKey(field))
      : undefined,
    field?.value,
    field?.defaultValue,
  ];
  return firstText(...values);
}

function passWorkColumnLabel(key: string, fallback: string) {
  return firstText(passWorkProductionColumnLabels.value[key], fallback);
}

function passWorkColumnWidth(key: string, fallback: number) {
  const configured = passWorkProductionColumnWidths.value[key];
  if (configured === undefined || configured === null || configured === '')
    return fallback;
  if (
    typeof configured === 'number' &&
    Number.isFinite(configured) &&
    configured > 0
  )
    return configured;
  const parsed = Number(String(configured).replace(/px$/i, '').trim());
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function togglePassWorkActionCollapsed() {
  passWorkActionCollapsed.value = !passWorkActionCollapsed.value;
}

function cssLength(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  if (typeof value === 'number') return `${value}px`;
  const text = String(value).trim();
  return text || undefined;
}

function buildRuntimeStyle(
  baseStyle?: Record<string, any>,
  sizeStyle: Record<string, unknown> = {},
) {
  const style: Record<string, string> = {};
  if (baseStyle && typeof baseStyle === 'object' && !Array.isArray(baseStyle)) {
    Object.entries(baseStyle).forEach(([key, value]) => {
      const normalized = cssLength(value);
      if (normalized) style[key] = normalized;
    });
  }
  Object.entries(sizeStyle).forEach(([key, value]) => {
    const normalized = cssLength(value);
    if (normalized) style[key] = normalized;
  });
  return style;
}

function readHeaderValue(field?: RuntimeField) {
  const key = fieldKey(field);
  if (!key) return '';
  return localHeader.value[key] ?? field?.value ?? field?.defaultValue ?? '';
}

function writeHeaderValue(field: RuntimeField, value: any) {
  const key = fieldKey(field);
  if (!key) return;
  localHeader.value[key] = value;
  emitHeaderData();
}

function isNumberField(field?: RuntimeField) {
  const component = String(
    field?.component || field?.controlType || field?.type || '',
  ).toLowerCase();
  return component.includes('number') || component === '数字输入';
}

function isSelectField(field?: RuntimeField) {
  const component = String(
    field?.component || field?.controlType || field?.type || '',
  ).toLowerCase();
  return component.includes('select') || component === '下拉选择';
}

function isDatePickerField(field?: RuntimeField) {
  const component = String(
    field?.component || field?.controlType || field?.type || '',
  ).toLowerCase();
  const valueType = String(field?.valueType || field?.mode || '').toUpperCase();
  return component.includes('date') || ['DATE', 'DATETIME'].includes(valueType);
}

function isDateTimePickerField(field?: RuntimeField) {
  const valueType = String(field?.valueType || field?.mode || '').toUpperCase();
  const key = fieldKey(field).toLowerCase();
  if (
    isFaiSelfCheckRuntime.value &&
    ['confirmertime', 'confirmtime'].includes(key)
  ) {
    return false;
  }
  return valueType === 'DATETIME' || key.includes('time');
}

function datePickerValueFormat(field?: RuntimeField) {
  return isDateTimePickerField(field) ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD';
}

function datePickerShowTime(field?: RuntimeField) {
  return isDateTimePickerField(field)
    ? { defaultValue: DEFAULT_DATE_TIME_VALUE, format: 'HH:mm:ss' }
    : false;
}

function normalizeDateArray(field: RuntimeField, value: unknown[]) {
  if (value.length < 3) return '';
  const [year, month, day, hour = 0, minute = 0, second = 0] = value;
  if (![year, month, day].every((item) => Number.isFinite(Number(item)))) {
    return '';
  }
  const dateText = `${String(year).padStart(4, '0')}-${String(month).padStart(
    2,
    '0',
  )}-${String(day).padStart(2, '0')}`;
  if (!isDateTimePickerField(field)) return dateText;
  return `${dateText} ${String(hour).padStart(2, '0')}:${String(
    minute,
  ).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
}

function normalizeHeaderDatePickerValue(
  field: RuntimeField,
  value: unknown,
  dateString?: string | string[],
) {
  const rawValue = Array.isArray(dateString)
    ? firstText(...dateString)
    : firstText(dateString);
  if (Array.isArray(value)) return normalizeDateArray(field, value);
  if (
    value &&
    typeof value === 'object' &&
    typeof (value as { format?: unknown }).format === 'function'
  ) {
    return (value as { format: (format: string) => string }).format(
      datePickerValueFormat(field),
    );
  }
  const text = firstText(rawValue, value).replace('T', ' ');
  if (!text) return '';
  const parts =
    /^(\d{4})[-,/](\d{1,2})[-,/](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/u.exec(
      text,
    );
  if (!parts) return text;
  const dateText = `${parts[1]}-${parts[2]!.padStart(2, '0')}-${parts[3]!.padStart(
    2,
    '0',
  )}`;
  if (!isDateTimePickerField(field)) return dateText;
  return `${dateText} ${(parts[4] || '0').padStart(2, '0')}:${(
    parts[5] || '0'
  ).padStart(2, '0')}:${(parts[6] || '0').padStart(2, '0')}`;
}

function writeHeaderDatePickerValue(
  field: RuntimeField,
  value: unknown,
  dateString?: string | string[],
  action = 'write',
) {
  const key = fieldKey(field);
  if (!key) return;
  const normalizedValue = normalizeHeaderDatePickerValue(
    field,
    value,
    dateString,
  );
  localHeader.value[key] = normalizedValue;
  if (isFaiSelfCheckRuntime.value) {
    ensureArray<string>(field.sources).forEach((sourceKey) => {
      if (!sourceKey || sourceKey === key || sourceKey.includes('.')) return;
      localHeader.value[sourceKey] = normalizedValue;
    });
  }
  emitHeaderData();
  if (isFaiSelfCheckRuntime.value) {
    console.warn('[自检记录日期选择] selected', {
      action,
      field: key,
      value: normalizedValue,
    });
  }
}

function getRuntimePopupContainer(triggerNode?: HTMLElement) {
  return (
    (triggerNode?.closest(
      '.station-form-runtime-fullscreen-modal .ant-modal-content',
    ) as HTMLElement | null) ||
    (triggerNode?.closest('.ant-modal-content') as HTMLElement | null) ||
    document.body
  );
}

function fieldOptions(field?: RuntimeField) {
  if (Array.isArray(field?.options)) return field.options;
  const key = fieldKey(field);
  if (key === 'resultFlag' || key === 'inspectionResult') return RESULT_OPTIONS;
  return [];
}

function isReadonlyField(field?: RuntimeField) {
  const component = String(
    field?.component || field?.controlType || field?.type || '',
  ).toLowerCase();
  return (
    props.readonly ||
    field?.editable === false ||
    component === 'text' ||
    component === 'readonly' ||
    component === '只读文本'
  );
}

function isEditablePassWorkHeaderField(field?: RuntimeField) {
  if (
    props.readonly ||
    props.readonlyPassWorkHeader ||
    field?.editable !== true
  ) {
    return false;
  }
  const component = String(
    field?.component || field?.controlType || field?.type || '',
  ).toLowerCase();
  return (
    component !== 'text' && component !== 'readonly' && component !== '只读文本'
  );
}

function updateDetailCell(
  row: Record<string, any>,
  column: RuntimeField,
  value: any,
) {
  const key = fieldKey(column);
  if (!key) return;
  row[key] = value;
  if (key === 'resultFlag') {
    row.status = value || 'OK';
    row.result = value || 'OK';
    row.checkResult = value || 'OK';
  }
  emitHeaderData();
}

function createEmptyDetailRow(index: number) {
  const row: Record<string, any> = {
    seq: index + 1,
    sortNo: index + 1,
  };
  detailColumns.value.forEach((column) => {
    const key = fieldKey(column);
    if (key) row[key] = column.defaultValue ?? '';
  });
  return row;
}

function addDetailRow(index = detailRows.value.length) {
  const rows = ensureHeaderArray<Record<string, any>>('previewDetails');
  const row = createEmptyDetailRow(index);
  rows.splice(Math.max(0, Math.min(index, rows.length)), 0, row);
  normalizeDetailRows(rows);
  emitHeaderData();
}

function removeDetailRow(index: number) {
  const rows = ensureHeaderArray<Record<string, any>>('previewDetails');
  rows.splice(index, 1);
  normalizeDetailRows(rows);
  emitHeaderData();
}

function normalizeDetailRows(rows = detailRows.value) {
  rows.forEach((row, index) => {
    row.seq = index + 1;
    row.sortNo = index + 1;
  });
}

function ensureImportedDetailRow(index: number) {
  const rows = ensureHeaderArray<Record<string, any>>('previewDetails');
  while (rows.length <= index) {
    rows.push(createEmptyDetailRow(rows.length));
  }
  return rows[index];
}

function columnStyle(column: RuntimeField) {
  const width = Number(column.width) || 140;
  return {
    minWidth: `${width}px`,
    textAlign: column.align || undefined,
    width: `${width}px`,
  };
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function excelCell(
  colIndex: number,
  text: unknown,
  options: Partial<MesHcProcessFormApi.LayoutCell> = {},
): MesHcProcessFormApi.LayoutCell {
  return {
    colIndex,
    editable: !props.readonly,
    text: displayText(text, ''),
    ...options,
  };
}

function firstConfigValue(...values: unknown[]) {
  for (const value of values) {
    if (value === undefined || value === null) continue;
    if (typeof value === 'string' && !value.trim()) continue;
    return value;
  }
  return undefined;
}

function toExcelRowIndex(value: unknown, fallback = 0) {
  const row = Number(value);
  return Number.isFinite(row) && row > 0 ? Math.floor(row) - 1 : fallback;
}

function toExcelColumnIndex(value: unknown, fallback = 0) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.max(Math.floor(value) - 1, 0);
  }
  const text = String(value ?? '').trim();
  if (!text) return fallback;
  if (/^\d+$/u.test(text)) {
    return Math.max(Number(text) - 1, 0);
  }
  const letters = text.toUpperCase().replaceAll(/[^A-Z]/gu, '');
  if (!letters) return fallback;
  let letterIndex = 0;
  for (const char of letters) {
    letterIndex = letterIndex * 26 + (char.codePointAt(0) ?? 64) - 64;
  }
  return Math.max(letterIndex - 1, 0);
}

function resolveSourceImportStartRow() {
  const detail = sourceExcelImportDetail.value;
  const zeroBased = firstConfigValue(
    detail.importBodyStartRow,
    sourceExcelImport.value.importBodyStartRow,
  );
  if (zeroBased !== undefined) {
    const row = Number(zeroBased);
    return Number.isFinite(row) && row >= 0 ? Math.floor(row) : 0;
  }
  return toExcelRowIndex(
    firstConfigValue(detail.startRow, sourceExcelImport.value.startRow),
    0,
  );
}

function resolveSourceImportSkipHeaderRows() {
  const detail = sourceExcelImportDetail.value;
  const configured = Number(
    firstConfigValue(
      detail.skipHeaderRows,
      detail.skipRows,
      detail.dataRowOffset,
      sourceExcelImport.value.skipHeaderRows,
      sourceExcelImport.value.skipRows,
      sourceExcelImport.value.dataRowOffset,
    ),
  );
  return Number.isFinite(configured) && configured > 0
    ? Math.floor(configured)
    : 0;
}

function resolveSourceImportMaxRows() {
  const detail = sourceExcelImportDetail.value;
  const rawValue = firstConfigValue(
    detail.maxRows,
    sourceExcelImport.value.maxRows,
  );
  if (rawValue === undefined) return undefined;
  const configured = Number(rawValue);
  if (Number.isFinite(configured) && configured > 0) {
    return Math.floor(configured);
  }
  return undefined;
}

function buildSourceImportColumns() {
  const detail = sourceExcelImportDetail.value;
  const configured = ensureArray<Record<string, any>>(
    detail.columns || sourceExcelImport.value.columns,
  );
  const columns =
    configured.length > 0
      ? configured
      : detailColumns.value.map((column, index) => ({
          column: index + 1,
          field: fieldKey(column),
          label: fieldLabel(column),
          width: column.width,
        }));
  return columns
    .map((column, index) => {
      const field = String(
        column.field || column.key || column.bindField || '',
      ).trim();
      const label = String(column.label || column.title || field);
      if (!field || shouldHideImportField(field, label)) return null;
      const importColIndex =
        column.importColIndex === undefined
          ? toExcelColumnIndex(
              firstConfigValue(
                column.column,
                column.sourceColumn,
                column.excelColumn,
              ),
              index,
            )
          : Math.max(Number(column.importColIndex) || 0, 0);
      return {
        bindField: field,
        importColIndex,
        label,
        width:
          Number(column.width) ||
          Number(detailColumns.value[index]?.width) ||
          160,
      };
    })
    .filter(Boolean) as Array<{
    bindField: string;
    importColIndex: number;
    label: string;
    width: number;
  }>;
}

function buildSourceImportExcelLayout(
  fileName?: string,
): MesHcProcessFormApi.LayoutExcelReq | null {
  const columns = buildSourceImportColumns();
  if (columns.length === 0) return null;
  const startRow =
    resolveSourceImportStartRow() + resolveSourceImportSkipHeaderRows();
  const maxRows = resolveSourceImportMaxRows();
  const templateRowCount = 1;
  const stopPrefixes = ensureArray<string>(
    sourceExcelImportDetail.value.stopPrefixes ||
      sourceExcelImport.value.stopPrefixes,
  );
  const layout: MesHcProcessFormApi.LayoutExcelReq = {
    columns: columns.map((column) => ({
      title: column.label,
      width: column.width,
    })),
    detailTitle: detailSection.value?.title || '明细',
    fileName:
      `${displayText(fileName || displayTitle.value, '动态表单')}.xlsx`.replaceAll(
        /[\\/:*?"<>|]/gu,
        '_',
      ),
    headerItems: [],
    importBodyStartRow: startRow,
    importStopPrefixes: stopPrefixes,
    rows: Array.from({ length: templateRowCount }, (_, rowIndex) => ({
      cells: columns.map((column, colIndex) =>
        excelCell(
          colIndex,
          detailRows.value[rowIndex]?.[column.bindField] ?? '',
          {
            bindField: column.bindField,
            bindKey: String(rowIndex),
            editable: true,
            importColIndex: column.importColIndex,
          },
        ),
      ),
    })),
    sheetName:
      sourceExcelImport.value.sheetName || detailSection.value?.title || '明细',
    title: displayTitle.value,
    visualMode:
      runtimeLayout.value.visualMode || 'runtime-layout-source-import',
  };
  if (maxRows !== undefined) {
    layout.importBodyMaxRows = maxRows;
  }
  return layout;
}

function buildHeaderExcelItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  if (isFormulaPassWorkRuntime.value) {
    return buildFormulaHeaderExcelItems();
  }
  return [
    ...formInfoFields.value,
    ...depthFields.value,
    ...signatureFields.value,
  ]
    .map((field) => {
      const key = fieldKey(field);
      if (!key) return null;
      return {
        bindField: 'headerData',
        bindKey: key,
        editable: !isReadonlyField(field),
        label: fieldLabel(field),
        value: displayText(localHeader.value[key], ''),
      } as MesHcProcessFormApi.LayoutHeaderItem;
    })
    .filter(Boolean) as MesHcProcessFormApi.LayoutHeaderItem[];
}

function buildFormulaHeaderExcelItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  return [
    {
      editable: false,
      label: '计划号',
      value: firstText(localHeader.value.planNo),
    },
    {
      editable: false,
      label: '型号',
      value: firstText(localHeader.value.modelCode),
    },
    {
      editable: false,
      label: '批号',
      value: firstText(
        localHeader.value.batchNo,
        localHeader.value.motherBatchNo,
      ),
    },
    {
      editable: false,
      label: '当前工序',
      value: firstText(
        localHeader.value.processName,
        props.schema?.processName,
      ),
    },
    {
      editable: false,
      label: '执行时机',
      value: firstText(
        localHeader.value.triggerTimingName,
        props.schema?.triggerTimingName,
      ),
    },
    ...passWorkHeaderFields.value.map((field) => ({
      bindField: 'headerData',
      bindKey: field.field,
      editable: false,
      label: field.label,
      value: field.value || '',
    })),
  ];
}

function buildFormulaExcelColumns(): MesHcProcessFormApi.LayoutColumn[] {
  if (isFormulaStartupRuntime.value) {
    return [
      { title: '序号', width: 90 },
      { title: '点检项目', width: 260 },
      { title: '标准', width: 360 },
      { title: 'OK/NG', width: 120 },
      { title: '备注', width: 260 },
    ];
  }
  if (isFormulaCleaningRuntime.value) {
    return [
      { title: '工序', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '检查标准', width: 420 },
      { title: 'OK/NG', width: 120 },
      { title: '备注', width: 260 },
    ];
  }
  return [
    {
      title: passWorkColumnLabel('seq', '序号'),
      width: passWorkColumnWidth('seq', 70),
    },
    {
      title: passWorkColumnLabel('category', '项目类别'),
      width: passWorkColumnWidth('category', 110),
    },
    {
      title: passWorkColumnLabel('node', '步骤节点'),
      width: passWorkColumnWidth('node', 110),
    },
    {
      title: passWorkColumnLabel('item', '点检项目'),
      width: passWorkColumnWidth('item', 220),
    },
    {
      title: passWorkColumnLabel('standard', '标准'),
      width: passWorkColumnWidth('standard', 260),
    },
    ...(showFormulaRecordLabelColumn.value
      ? [
          {
            title: passWorkColumnLabel('recordLabel', '记录项'),
            width: passWorkColumnWidth('recordLabel', 90),
          },
        ]
      : []),
    {
      title: passWorkColumnLabel('actual', '记录值'),
      width: passWorkColumnWidth('actual', 220),
    },
    {
      title: passWorkColumnLabel('remark', '异常说明'),
      width: passWorkColumnWidth('remark', 280),
    },
  ];
}

function buildFormulaExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  if (isFormulaStartupRuntime.value) {
    return detailRows.value.map((row, index) => ({
      cells: [
        excelCell(0, formulaRowSeq(row, index), { editable: false }),
        excelCell(1, formulaRowItem(row), { editable: false }),
        excelCell(2, formulaRowStandard(row), { editable: false }),
        excelCell(3, formulaRowResult(row), {
          bindField: 'resultFlag',
          bindKey: String(index),
          editable: !props.readonly,
        }),
        excelCell(4, formulaRowRemark(row), {
          bindField: 'abnormalRemark',
          bindKey: String(index),
          editable: !props.readonly,
        }),
      ],
    }));
  }
  if (isFormulaCleaningRuntime.value) {
    return detailRows.value.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getFormulaCleaningCategoryRowSpan(index);
      if (categorySpan > 0) {
        cells.push(
          excelCell(0, formulaRowCategory(row), {
            editable: false,
            rowSpan: categorySpan,
          }),
        );
      }
      cells.push(
        excelCell(1, formulaRowItem(row), { editable: false }),
        excelCell(2, formulaRowStandard(row), { editable: false }),
        excelCell(3, formulaRowResult(row), {
          bindField: 'resultFlag',
          bindKey: String(index),
          editable: !props.readonly,
        }),
        excelCell(4, formulaRowRemark(row), {
          bindField: 'abnormalRemark',
          bindKey: String(index),
          editable: !props.readonly,
        }),
      );
      return { cells };
    });
  }
  return formulaProductionRows.value.map((row, index) => {
    const cells: MesHcProcessFormApi.LayoutCell[] = [];
    const sourceSpan = getFormulaProductionSourceRowSpan(index);
    const categorySpan = getFormulaProductionFieldRowSpan(index, 'category');
    const nodeSpan = getFormulaProductionFieldRowSpan(index, 'node');
    const itemSpan = getFormulaProductionFieldRowSpan(index, 'item');
    if (sourceSpan > 0)
      cells.push(
        excelCell(0, row.id, { editable: false, rowSpan: sourceSpan }),
      );
    if (categorySpan > 0)
      cells.push(
        excelCell(1, row.category, { editable: false, rowSpan: categorySpan }),
      );
    if (nodeSpan > 0)
      cells.push(
        excelCell(2, row.node, { editable: false, rowSpan: nodeSpan }),
      );
    if (itemSpan > 0)
      cells.push(
        excelCell(3, row.item, { editable: false, rowSpan: itemSpan }),
      );
    if (sourceSpan > 0)
      cells.push(
        excelCell(4, row.standard, { editable: false, rowSpan: sourceSpan }),
      );
    let colIndex = 5;
    if (showFormulaRecordLabelColumn.value) {
      cells.push(excelCell(colIndex, row.recordLabel, { editable: false }));
      colIndex += 1;
    }
    cells.push(
      excelCell(colIndex, formulaRowActual(row.source, row.actualField), {
        bindField: row.actualField,
        bindKey: String(row.sourceIndex),
        editable: !props.readonly,
      }),
    );
    colIndex += 1;
    if (sourceSpan > 0) {
      cells.push(
        excelCell(colIndex, formulaRowRemark(row.source), {
          bindField: 'abnormalRemark',
          bindKey: String(row.sourceIndex),
          editable: !props.readonly,
          rowSpan: sourceSpan,
        }),
      );
    }
    return { cells };
  });
}

function buildExcelLayout(
  fileName?: string,
  purpose: ExcelLayoutPurpose = 'export',
): MesHcProcessFormApi.LayoutExcelReq | null {
  if (purpose === 'import' && shouldUseSourceExcelImport.value) {
    const sourceLayout = buildSourceImportExcelLayout(fileName);
    if (sourceLayout) return sourceLayout;
  }
  if (isFormulaPassWorkRuntime.value) {
    return {
      columns: buildFormulaExcelColumns(),
      detailTitle: passWorkDetailTitle.value,
      fileName:
        `${displayText(fileName || displayTitle.value, '配料过站工作')}.xlsx`.replaceAll(
          /[\\/:*?"<>|]/gu,
          '_',
        ),
      headerItems: buildHeaderExcelItems(),
      rows: buildFormulaExcelRows(),
      sheetName: '过站工作',
      title: displayTitle.value,
      visualMode: runtimeLayout.value.visualMode || 'formula-pass-work',
    };
  }
  return {
    columns: detailColumns.value.map((column) => ({
      title: fieldLabel(column),
      width: Number(column.width) || 160,
    })),
    detailTitle: detailSection.value?.title || '明细',
    fileName:
      `${displayText(fileName || displayTitle.value, '动态表单')}.xlsx`.replaceAll(
        /[\\/:*?"<>|]/gu,
        '_',
      ),
    headerItems: buildHeaderExcelItems(),
    rows: detailRows.value.map((row, rowIndex) => ({
      cells: detailColumns.value.map((column, colIndex) => {
        const key = fieldKey(column);
        return excelCell(colIndex, key ? row[key] : '', {
          bindField: key,
          bindKey: String(rowIndex),
          editable: !props.readonly && !isReadonlyField(column),
        });
      }),
    })),
    sheetName: detailSection.value?.title || '明细',
    title: displayTitle.value,
    visualMode: runtimeLayout.value.visualMode || 'runtime-layout',
  };
}

function normalizeImportText(value: unknown) {
  return String(value ?? '')
    .trim()
    .replaceAll(/\s+/gu, '')
    .replaceAll(/[（）]/gu, (char) => (char === '（' ? '(' : ')'))
    .toLowerCase();
}

function isBlankImportText(value: unknown) {
  const text = String(value ?? '').trim();
  return !text || text === '-' || text === '－' || text === '--';
}

function getConfiguredSkipRowKeywords() {
  const detail = sourceExcelImportDetail.value;
  const configured = ensureArray<string>(
    detail.skipRowKeywords ||
      detail.headerKeywords ||
      sourceExcelImport.value.skipRowKeywords ||
      sourceExcelImport.value.headerKeywords,
  );
  return [
    ...configured,
    displayTitle.value,
    detailSection.value?.title,
    '表单信息',
    '原始导入附件',
    '签名区',
    '记录明细',
  ]
    .map((item) => normalizeImportText(item))
    .filter(Boolean);
}

function shouldSkipSourceImportRow(row: Record<string, any>) {
  const values = Object.values(row).map((value) => String(value ?? '').trim());
  const meaningfulValues = values.filter((value) => !isBlankImportText(value));
  if (meaningfulValues.length === 0) return true;

  const rowText = normalizeImportText(meaningfulValues.join('|'));
  if (
    getConfiguredSkipRowKeywords().some(
      (keyword) => keyword && rowText.includes(keyword),
    )
  ) {
    return true;
  }

  const columnLabels = detailColumns.value
    .map((column) => normalizeImportText(fieldLabel(column)))
    .filter(Boolean);
  let matchedLabelCount = 0;
  for (const value of meaningfulValues) {
    const normalized = normalizeImportText(value);
    if (
      columnLabels.some(
        (label) =>
          normalized === label ||
          normalized.includes(label) ||
          label.includes(normalized),
      )
    ) {
      matchedLabelCount += 1;
    }
  }
  return matchedLabelCount >= Math.min(2, columnLabels.length || 2);
}

function applySourceImportedLayout(
  resp?: MesHcProcessFormApi.LayoutImportResp,
): ImportApplySummary {
  const groupedRows = new Map<number, Record<string, any>>();
  (resp?.cellValues || []).forEach((item) => {
    const rowIndex = Number(item.bindKey ?? item.bodyRowIndex ?? 0);
    const key = item.bindField;
    if (!key || shouldHideImportField(key) || !Number.isFinite(rowIndex))
      return;
    const row = groupedRows.get(rowIndex) || {};
    row[key] = item.value ?? '';
    groupedRows.set(rowIndex, row);
  });

  const nextRows: Record<string, any>[] = [];
  let skippedRowCount = 0;
  [...groupedRows.entries()]
    .toSorted(([left], [right]) => left - right)
    .forEach(([, row]) => {
      if (shouldSkipSourceImportRow(row)) {
        skippedRowCount += 1;
        return;
      }
      nextRows.push({
        ...createEmptyDetailRow(nextRows.length),
        ...row,
      });
    });

  localHeader.value.previewDetails = nextRows;
  normalizeDetailRows(nextRows);
  return {
    appliedCellCount: nextRows.length * buildSourceImportColumns().length,
    rowCount: nextRows.length,
    skippedRowCount,
    totalCellCount: resp?.totalCellCount || 0,
  };
}

function applyImportedLayout(
  resp?: MesHcProcessFormApi.LayoutImportResp,
): ImportApplySummary {
  (resp?.headerValues || []).forEach((item) => {
    const key = item.bindField === 'headerData' ? item.bindKey : item.bindField;
    if (!key) return;
    localHeader.value[key] = item.value ?? '';
  });

  if (shouldUseSourceExcelImport.value) {
    const summary = applySourceImportedLayout(resp);
    emitHeaderData();
    return summary;
  }

  let appliedCellCount = 0;
  (resp?.cellValues || []).forEach((item) => {
    const rowIndex = Number(item.bindKey ?? item.bodyRowIndex ?? 0);
    const row = ensureImportedDetailRow(
      Number.isFinite(rowIndex) ? rowIndex : 0,
    );
    const key = item.bindField;
    if (!row || !key || shouldHideImportField(key)) return;
    if (key.startsWith('multi:')) writeFormulaValue(row, key, item.value);
    else row[key] = item.value ?? '';
    appliedCellCount += 1;
  });
  normalizeDetailRows();
  emitHeaderData();
  return {
    appliedCellCount,
    rowCount: detailRows.value.length,
    skippedRowCount: 0,
    totalCellCount: resp?.totalCellCount || 0,
  };
}

function getHeaderData() {
  return cloneObject(localHeader.value);
}

function setHeaderData(value: Record<string, any>) {
  localHeader.value = cloneHeaderData(value || {});
  emitHeaderData();
}

function findTemplateItemByLabel(label: string) {
  const normalized = label.trim();
  return props.items.find(
    (item) => String(item.itemName || '').trim() === normalized,
  );
}

function fieldToRecordItem(
  field: RuntimeField,
  index: number,
): null | RuntimeRecordItem {
  const key = fieldKey(field);
  if (!key) return null;
  const label = fieldLabel(field);
  const templateItem = findTemplateItemByLabel(label);
  return {
    actualValue: displayText(localHeader.value[key], ''),
    fieldKey: key,
    fieldLabel: label,
    itemCategory: field.itemCategory || field.sectionTitle || '表单信息',
    itemSeq: Number(field.itemSeq) || index + 1,
    resultFlag: 'OK',
    sourceKind: 'field',
    sourceRowJson: JSON.stringify({
      field: key,
      label,
      value: localHeader.value[key] ?? '',
    }),
    standardText:
      field.standard || field.standardText || templateItem?.standardText,
    stepNode: field.stepNode || '',
    templateItemId: templateItem?.id,
    valueMode:
      templateItem?.valueMode || (isReadonlyField(field) ? 'READONLY' : 'TEXT'),
  };
}

function detailRowToRecordItem(
  row: Record<string, any>,
  index: number,
): RuntimeRecordItem {
  const isInspectionRow = firstText(
    row.itemName,
    row.item,
    row.standardText,
    row.standard,
    row.actualValue,
    row.actualValue2,
    row.resultFlag,
    row.status,
    row.abnormalRemark,
    row.remark,
  );
  if (isInspectionRow) {
    const label = firstText(row.itemName, row.fieldLabel, `明细${index + 1}`);
    const templateItem = findTemplateItemByLabel(label);
    return {
      actualValue: firstText(row.actualValue, row.recordValue, row.value, '-'),
      actualValue2: firstText(row.actualValue2, ''),
      abnormalRemark: firstText(row.abnormalRemark, row.remark, ''),
      fieldKey: firstText(row.fieldKey, `runtime_detail_${index + 1}`),
      fieldLabel: label,
      itemCategory: firstText(
        row.itemCategory,
        row.category,
        detailSection.value?.title,
        '明细',
      ),
      itemSeq: Number(row.sortNo || row.seq) || index + 1,
      resultFlag: firstText(row.resultFlag, row.status, row.result, 'OK'),
      sourceKind: 'detail',
      sourceRowJson: JSON.stringify({
        ...row,
        seq: Number(row.seq || index + 1),
        sortNo: Number(row.sortNo || index + 1),
      }),
      standardText: firstText(
        row.standardText,
        row.standard,
        templateItem?.standardText,
      ),
      stepNode: firstText(row.stepNode, row.node, ''),
      templateItemId: templateItem?.id,
      valueMode: firstText(row.valueMode, templateItem?.valueMode, 'TEXT'),
    };
  }

  const rowLabel = firstText(
    row.sliceBatchNo,
    row.productionBatchNo,
    row.samplePositionName,
    `明细${index + 1}`,
  );
  return {
    actualValue: firstText(row.sliceBatchNo, row.productionBatchNo, rowLabel),
    abnormalRemark: displayText(row.remark, ''),
    fieldKey: `runtime_detail_${index + 1}`,
    fieldLabel: rowLabel,
    itemCategory: detailSection.value?.title || '明细',
    itemSeq: Number(row.sortNo || row.seq) || index + 1,
    resultFlag: 'OK',
    sourceKind: 'detail',
    sourceRowJson: JSON.stringify({
      ...row,
      seq: Number(row.seq || index + 1),
      sortNo: Number(row.sortNo || index + 1),
    }),
    stepNode: displayText(row.samplePositionName, ''),
    valueMode: 'TEXT',
  };
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function runtimeBoolean(value: unknown, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  const text = String(value).trim().toLowerCase();
  if (['1', 'on', 'true', 'y', 'yes'].includes(text)) return true;
  if (['0', 'false', 'n', 'no', 'off'].includes(text)) return false;
  return fallback;
}

function buildRecordItems(): MesHcProcessFormApi.RecordItem[] {
  const fieldItems = [
    ...formInfoFields.value,
    ...depthFields.value,
    ...signatureFields.value,
  ]
    .map((field, index) => fieldToRecordItem(field, index))
    .filter(Boolean) as RuntimeRecordItem[];
  const detailItems = detailRows.value.map((row, index) =>
    detailRowToRecordItem(row, index),
  );
  return [...fieldItems, ...detailItems].map(
    ({ sourceKind: _sourceKind, ...item }) => item,
  );
}

defineExpose({
  applyImportedLayout,
  buildExcelLayout,
  buildRecordItems,
  getHeaderData,
  setHeaderData,
});
</script>

<template>
  <div
    class="station-form-runtime"
    :class="runtimeRootClass"
    :style="runtimeRootStyle"
  >
    <div class="station-form-runtime__toolbar">
      <div class="station-form-runtime__title">
        <strong>{{ displayTitle }}</strong>
        <span v-for="item in displayMeta" :key="item">{{ item }}</span>
      </div>
      <slot name="actions"></slot>
    </div>

    <div
      v-if="hasRuntimeLayout"
      class="station-form-runtime__body"
      :style="runtimeBodyStyle"
    >
      <fieldset
        v-if="isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__formula-info"
      >
        <legend>{{ passWorkHeaderTitle }}</legend>
        <div
          class="station-form-runtime__formula-head-grid"
          :class="{
            'station-form-runtime__formula-head-grid--compact':
              passWorkHeaderCompact,
          }"
          :style="{
            gridTemplateColumns: `repeat(${passWorkHeaderColumns}, minmax(0, 1fr))`,
          }"
        >
          <div
            v-for="field in passWorkHeaderFields"
            :key="field.field"
            class="station-form-runtime__formula-head-item"
          >
            <span class="station-form-runtime__formula-head-label">{{
              field.label
            }}</span>
            <span class="station-form-runtime__formula-head-value">
              <DatePicker
                v-if="
                  isFaiSelfCheckRuntime &&
                  isEditablePassWorkHeaderField(field) &&
                  isDatePickerField(field)
                "
                v-model:value="localHeader[fieldKey(field)]"
                :allow-clear="true"
                :format="datePickerValueFormat(field)"
                :get-popup-container="getRuntimePopupContainer"
                :show-time="datePickerShowTime(field)"
                :value-format="datePickerValueFormat(field)"
                @change="
                  (value, dateString) =>
                    writeHeaderDatePickerValue(
                      field,
                      value,
                      dateString,
                      'change',
                    )
                "
              />
              <InputNumber
                v-else-if="
                  isEditablePassWorkHeaderField(field) && isNumberField(field)
                "
                :controls="false"
                :value="readHeaderValue(field)"
                @change="(value) => writeHeaderValue(field, value)"
              />
              <Select
                v-else-if="
                  isEditablePassWorkHeaderField(field) && isSelectField(field)
                "
                :options="fieldOptions(field)"
                :value="readHeaderValue(field)"
                @change="(value) => writeHeaderValue(field, value)"
              />
              <Input
                v-else-if="isEditablePassWorkHeaderField(field)"
                :value="readHeaderValue(field)"
                @change="(event) => writeHeaderValue(field, event.target.value)"
              />
              <strong v-else>{{ displayText(field.value) }}</strong>
            </span>
          </div>
        </div>
      </fieldset>

      <fieldset
        v-if="formInfoFields.length > 0 && !isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__header-fieldset"
      >
        <legend>{{ formInfoSection?.title || '表单信息' }}</legend>
        <div
          class="station-form-runtime__head-grid"
          :style="{ gridTemplateColumns: `repeat(${formInfoColumns}, minmax(180px, 1fr))` }"
        >
          <div
            v-for="field in formInfoFields"
            :key="fieldKey(field)"
            class="station-form-runtime__head-cell"
          >
            <span class="station-form-runtime__label">{{
              fieldLabel(field)
            }}</span>
            <span class="station-form-runtime__value">
              <DatePicker
                v-if="isPressSlotProduction && !isReadonlyField(field) && isDatePickerField(field)"
                :value="readHeaderValue(field) || undefined"
                :format="datePickerValueFormat(field)"
                :value-format="datePickerValueFormat(field)"
                :show-time="datePickerShowTime(field)"
                :get-popup-container="getRuntimePopupContainer"
                @change="(value, text) => writeHeaderDatePickerValue(field, value, text, 'change')"
              />
              <InputNumber
                v-else-if="!isReadonlyField(field) && isNumberField(field)"
                :controls="false"
                :value="readHeaderValue(field)"
                @change="(value) => writeHeaderValue(field, value)"
              />
              <Select
                v-else-if="!isReadonlyField(field) && isSelectField(field)"
                :options="fieldOptions(field)"
                :value="readHeaderValue(field)"
                @change="(value) => writeHeaderValue(field, value)"
              />
              <Input
                v-else-if="!isReadonlyField(field)"
                :value="readHeaderValue(field)"
                @change="(event) => writeHeaderValue(field, event.target.value)"
              />
              <strong v-else>{{ displayText(readHeaderValue(field)) }}</strong>
            </span>
          </div>
        </div>
      </fieldset>

      <fieldset
        v-if="depthFields.length > 0"
        class="station-form-runtime__fieldset"
      >
        <legend>{{ depthSection?.title || '矩阵区' }}</legend>
        <div
          class="station-form-runtime__matrix"
          :style="{
            gridTemplateColumns: `repeat(${Math.max(depthFields.length, 1)}, minmax(120px, 1fr))`,
          }"
        >
          <div
            v-for="field in depthFields"
            :key="`${fieldKey(field)}-head`"
            class="station-form-runtime__matrix-head"
          >
            {{ fieldLabel(field) }}
          </div>
          <div
            v-for="field in depthFields"
            :key="fieldKey(field)"
            class="station-form-runtime__matrix-cell"
          >
            <InputNumber
              v-if="!isReadonlyField(field) && isNumberField(field)"
              :controls="false"
              :value="readHeaderValue(field)"
              @change="(value) => writeHeaderValue(field, value)"
            />
            <Input
              v-else-if="!isReadonlyField(field)"
              :value="readHeaderValue(field)"
              @change="(event) => writeHeaderValue(field, event.target.value)"
            />
            <strong v-else>{{ displayText(readHeaderValue(field)) }}</strong>
          </div>
        </div>
      </fieldset>

      <fieldset
        v-if="shouldShowAttachmentSection"
        class="station-form-runtime__fieldset station-form-runtime__attachments"
        :style="runtimeAttachmentStyle"
      >
        <legend>{{ attachmentSection.title || '附件' }}</legend>
        <div
          v-if="attachments.length > 0"
          class="station-form-runtime__attachment-list"
        >
          <button
            v-for="attachment in attachments"
            :key="attachment.url || attachment.path || attachment.name"
            class="station-form-runtime__attachment"
            type="button"
            @click="emit('openAttachment', attachment)"
          >
            <span>{{
              attachment.name || attachment.url || attachment.path || '附件'
            }}</span>
            <em>{{ formatAttachmentSize(attachment.size) }}</em>
          </button>
        </div>
        <div v-else class="station-form-runtime__attachment-empty">
          <Empty description="暂无附件" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </div>
      </fieldset>

      <fieldset
        v-if="isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__formula-detail"
        :class="{
          'station-form-runtime__formula-detail--fixed':
            hasFormulaDetailHeight && isFormulaDetailFixed,
        }"
        :style="runtimeFormulaDetailStyle"
      >
        <legend>{{ passWorkDetailTitle }}</legend>
        <div
          v-if="isFormulaStartupRuntime || isFormulaCleaningRuntime"
          class="station-form-runtime__check-tip"
        >
          {{ CHECK_RESULT_REMARK_TIP }}
        </div>
        <div
          class="station-form-runtime__formula-table-wrap"
          :style="runtimeFormulaTableWrapStyle"
        >
          <table class="station-form-runtime__formula-table">
            <thead v-if="isFormulaStartupRuntime">
              <tr>
                <th width="90">序号</th>
                <th width="260">点检项目</th>
                <th width="360">标准</th>
                <th width="120">OK/NG</th>
                <th width="260">备注</th>
              </tr>
            </thead>
            <thead v-else-if="isFormulaCleaningRuntime">
              <tr>
                <th width="120">工序</th>
                <th width="220">点检项目</th>
                <th width="420">检查标准</th>
                <th width="120">OK/NG</th>
                <th width="260">备注</th>
              </tr>
            </thead>
            <thead v-else>
              <tr>
                <th :width="passWorkColumnWidth('seq', 70)">
                  {{ passWorkColumnLabel('seq', '序号') }}
                </th>
                <th :width="passWorkColumnWidth('category', 110)">
                  {{ passWorkColumnLabel('category', '项目类别') }}
                </th>
                <th :width="passWorkColumnWidth('node', 110)">
                  {{ passWorkColumnLabel('node', '步骤节点') }}
                </th>
                <th :width="passWorkColumnWidth('item', 220)">
                  {{ passWorkColumnLabel('item', '点检项目') }}
                </th>
                <th :width="passWorkColumnWidth('standard', 260)">
                  {{ passWorkColumnLabel('standard', '标准') }}
                </th>
                <th
                  v-if="showFormulaRecordLabelColumn"
                  :width="passWorkColumnWidth('recordLabel', 90)"
                >
                  {{ passWorkColumnLabel('recordLabel', '记录项') }}
                </th>
                <th :width="passWorkColumnWidth('actual', 220)">
                  {{ passWorkColumnLabel('actual', '记录值') }}
                </th>
                <th :width="passWorkColumnWidth('remark', 280)">
                  {{ passWorkColumnLabel('remark', '异常说明') }}
                </th>
              </tr>
            </thead>
            <tbody v-if="isFormulaStartupRuntime">
              <tr
                v-for="(row, rowIndex) in detailRows"
                :key="`startup-${row.seq || rowIndex}`"
              >
                <td align="center">{{ formulaRowSeq(row, rowIndex) }}</td>
                <td>{{ displayText(formulaRowItem(row)) }}</td>
                <td>{{ displayText(formulaRowStandard(row)) }}</td>
                <td>
                  <RadioGroup
                    v-if="!readonly"
                    :value="formulaRowResult(row)"
                    class="station-form-runtime__radio-group"
                    size="small"
                    @update:value="(value) => updateFormulaResult(row, value)"
                  >
                    <Radio value="OK">OK</Radio>
                    <Radio value="NG">NG</Radio>
                  </RadioGroup>
                  <span v-else>{{ displayText(formulaRowResult(row)) }}</span>
                </td>
                <td>
                  <Input
                    v-if="!readonly"
                    :value="formulaRowRemark(row)"
                    size="small"
                    @change="
                      (event) => updateFormulaRemark(row, event.target.value)
                    "
                  />
                  <span v-else>{{ displayText(formulaRowRemark(row)) }}</span>
                </td>
              </tr>
              <tr v-if="detailRows.length === 0">
                <td colspan="5" class="station-form-runtime__empty">
                  暂无明细数据
                </td>
              </tr>
            </tbody>
            <tbody v-else-if="isFormulaCleaningRuntime">
              <tr
                v-for="(row, rowIndex) in detailRows"
                :key="`cleaning-${row.seq || rowIndex}`"
              >
                <td
                  v-if="getFormulaCleaningCategoryRowSpan(rowIndex) > 0"
                  :rowspan="getFormulaCleaningCategoryRowSpan(rowIndex)"
                >
                  {{ displayText(formulaRowCategory(row)) }}
                </td>
                <td>{{ displayText(formulaRowItem(row)) }}</td>
                <td>{{ displayText(formulaRowStandard(row)) }}</td>
                <td>
                  <RadioGroup
                    v-if="!readonly"
                    :value="formulaRowResult(row)"
                    class="station-form-runtime__radio-group"
                    size="small"
                    @update:value="(value) => updateFormulaResult(row, value)"
                  >
                    <Radio value="OK">OK</Radio>
                    <Radio value="NG">NG</Radio>
                  </RadioGroup>
                  <span v-else>{{ displayText(formulaRowResult(row)) }}</span>
                </td>
                <td>
                  <Input
                    v-if="!readonly"
                    :value="formulaRowRemark(row)"
                    size="small"
                    @change="
                      (event) => updateFormulaRemark(row, event.target.value)
                    "
                  />
                  <span v-else>{{ displayText(formulaRowRemark(row)) }}</span>
                </td>
              </tr>
              <tr v-if="detailRows.length === 0">
                <td colspan="5" class="station-form-runtime__empty">
                  暂无明细数据
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr
                v-for="(row, rowIndex) in formulaProductionRows"
                :key="`${row.sourceRowId}-${row.actualField}`"
                :class="{
                  'station-form-runtime__formula-step-start':
                    isFormulaProductionStepStart(rowIndex),
                }"
              >
                <td
                  v-if="getFormulaProductionSourceRowSpan(rowIndex) > 0"
                  align="center"
                  :rowspan="getFormulaProductionSourceRowSpan(rowIndex)"
                >
                  {{ row.id || row.sourceIndex + 1 }}
                </td>
                <td
                  v-if="
                    getFormulaProductionFieldRowSpan(rowIndex, 'category') > 0
                  "
                  :rowspan="
                    getFormulaProductionFieldRowSpan(rowIndex, 'category')
                  "
                >
                  {{ displayText(row.category) }}
                </td>
                <td
                  v-if="getFormulaProductionFieldRowSpan(rowIndex, 'node') > 0"
                  :rowspan="getFormulaProductionFieldRowSpan(rowIndex, 'node')"
                >
                  {{ displayText(row.node) }}
                </td>
                <td
                  v-if="getFormulaProductionFieldRowSpan(rowIndex, 'item') > 0"
                  :rowspan="getFormulaProductionFieldRowSpan(rowIndex, 'item')"
                >
                  {{ displayText(row.item) }}
                </td>
                <td
                  v-if="getFormulaProductionSourceRowSpan(rowIndex) > 0"
                  :rowspan="getFormulaProductionSourceRowSpan(rowIndex)"
                >
                  {{ displayText(row.standard) }}
                </td>
                <td
                  v-if="showFormulaRecordLabelColumn"
                  class="station-form-runtime__formula-record-label"
                >
                  {{ displayText(row.recordLabel, '-') }}
                </td>
                <td>
                  <Input
                    v-if="!readonly"
                    :value="formulaRowActual(row.source, row.actualField)"
                    size="small"
                    :placeholder="
                      formulaInputPlaceholder(row.source, row.actualField)
                    "
                    @change="
                      (event) =>
                        updateFormulaActual(
                          row.source,
                          row.actualField,
                          event.target.value,
                        )
                    "
                  />
                  <span v-else>{{
                    displayText(formulaRowActual(row.source, row.actualField))
                  }}</span>
                </td>
                <td
                  v-if="getFormulaProductionSourceRowSpan(rowIndex) > 0"
                  :rowspan="getFormulaProductionSourceRowSpan(rowIndex)"
                >
                  <Input
                    v-if="!readonly"
                    :value="formulaRowRemark(row.source)"
                    size="small"
                    @change="
                      (event) =>
                        updateFormulaRemark(row.source, event.target.value)
                    "
                  />
                  <span v-else>{{
                    displayText(formulaRowRemark(row.source))
                  }}</span>
                </td>
              </tr>
              <tr v-if="detailRows.length === 0">
                <td
                  :colspan="showFormulaRecordLabelColumn ? 8 : 7"
                  class="station-form-runtime__empty"
                >
                  暂无明细数据
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </fieldset>

      <fieldset
        v-if="detailColumns.length > 0 && !isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__detail"
      >
        <legend>{{ detailSection?.title || '明细' }}</legend>
        <div
          v-if="isStartupCleaningMaintenanceRuntime"
          class="station-form-runtime__check-tip"
        >
          {{ CHECK_RESULT_REMARK_TIP }}
        </div>
        <div
          v-if="canInsertDetailRows"
          class="station-form-runtime__row-actions"
        >
          <Button size="small" @click="addDetailRow()">新增行</Button>
        </div>
        <div
          class="station-form-runtime__table-wrap"
          :style="runtimeDetailWrapStyle"
        >
          <table class="station-form-runtime__table">
            <thead>
              <tr>
                <th
                  v-for="column in detailColumns"
                  :key="fieldKey(column)"
                  :style="columnStyle(column)"
                >
                  {{ fieldLabel(column) }}
                </th>
                <th
                  v-if="showDetailActions"
                  class="station-form-runtime__action-col"
                >
                  操作
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, rowIndex) in detailRows"
                :key="`${row.seq || rowIndex}-${row.sliceBatchNo || row.samplePositionName || rowIndex}`"
              >
                <td
                  v-for="column in detailColumns"
                  :key="fieldKey(column)"
                  :style="columnStyle(column)"
                >
                  <template v-if="isPressSlotProduction && fieldKey(column) === 'actualValue'">
                    <span v-if="row.requiredFlag" class="text-red-500">* </span>
                    <span v-if="readonly">{{ row.actualValue || (row.valueMode === 'OK_NG' ? row.resultFlag : '-') }}{{ row.actualValue2 ? ` / ${row.actualValue2}` : '' }}</span>
                    <Select v-else-if="row.valueMode === 'OK_NG'" :options="RESULT_OPTIONS" :value="row.resultFlag"
                      @change="(value) => { row.resultFlag = value; row.actualValue = value; emitHeaderData(); }" />
                    <InputNumber v-else-if="row.valueMode === 'NUMBER'" :value="row.actualValue === '' ? null : Number(row.actualValue)"
                      @change="(value) => updateDetailCell(row, column, value == null ? '' : String(value))" />
                    <template v-else>
                      <Input :disabled="isReadonlyField(column)" :addon-before="row.dualLabel1 || undefined" :value="row.actualValue"
                        @update:value="(value) => updateDetailCell(row, column, value)" />
                      <Input v-if="String(row.valueMode).includes('DUAL')" :disabled="isReadonlyField(column)" :addon-before="row.dualLabel2 || undefined" :value="row.actualValue2"
                        @update:value="(value) => { row.actualValue2 = value; emitHeaderData(); }" />
                    </template>
                  </template>
                  <InputNumber
                    v-else-if="!isReadonlyField(column) && isNumberField(column)"
                    :controls="false"
                    :value="row[fieldKey(column)]"
                    @change="(value) => updateDetailCell(row, column, value)"
                  />
                  <Select
                    v-else-if="
                      !isReadonlyField(column) && isSelectField(column)
                    "
                    :options="fieldOptions(column)"
                    :value="row[fieldKey(column)]"
                    @change="(value) => updateDetailCell(row, column, value)"
                  />
                  <Input
                    v-else-if="!isReadonlyField(column)"
                    :value="row[fieldKey(column)]"
                    @change="
                      (event) =>
                        updateDetailCell(row, column, event.target.value)
                    "
                  />
                  <span v-else>{{ displayText(row[fieldKey(column)]) }}</span>
                </td>
                <td
                  v-if="showDetailActions"
                  class="station-form-runtime__row-op"
                >
                  <Button
                    v-if="canInsertDetailRows"
                    size="small"
                    type="link"
                    @click="addDetailRow(rowIndex + 1)"
                  >
                    插入
                  </Button>
                  <Button
                    v-if="canDeleteDetailRows"
                    danger
                    size="small"
                    type="link"
                    @click="removeDetailRow(rowIndex)"
                  >
                    删除
                  </Button>
                </td>
              </tr>
              <tr v-if="detailRows.length === 0">
                <td
                  :colspan="detailColumns.length + (showDetailActions ? 1 : 0)"
                  class="station-form-runtime__empty"
                >
                  暂无明细
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </fieldset>

      <fieldset
        v-if="isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__formula-action"
        :class="{
          'station-form-runtime__formula-action--collapsed':
            passWorkActionCollapsed,
        }"
      >
        <legend class="station-form-runtime__legend">
          <span>执行与验证记录</span>
          <button
            v-if="passWorkActionCollapsible"
            class="station-form-runtime__legend-toggle"
            type="button"
            @click="togglePassWorkActionCollapsed"
          >
            {{ passWorkActionCollapsed ? '展开' : '收起' }}
          </button>
        </legend>
        <template v-if="!passWorkActionCollapsed">
          <div
            v-if="isCompactPassWorkAction"
            class="station-form-runtime__formula-action-grid station-form-runtime__formula-action-grid--compact"
          >
            <div class="station-form-runtime__formula-action-cell">
              <label>执行结果</label>
              <span class="station-form-runtime__formula-action-control">
                <RadioGroup
                  v-if="!readonly"
                  :value="readHeaderKey('result', 'OK')"
                  class="station-form-runtime__radio-group"
                  size="small"
                  @update:value="(value) => writeHeaderKey('result', value)"
                >
                  <Radio value="OK">OK</Radio>
                  <Radio value="NG">NG</Radio>
                </RadioGroup>
                <strong v-else>{{ readHeaderKey('result', '-') }}</strong>
              </span>
            </div>
            <div class="station-form-runtime__formula-action-cell">
              <label>记录人</label>
              <span class="station-form-runtime__formula-action-control">
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('recorder')"
                  size="small"
                  @change="
                    (event) => writeHeaderKey('recorder', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('recorder', '-') }}</strong>
              </span>
            </div>
            <div class="station-form-runtime__formula-action-cell">
              <label>记录时间</label>
              <span class="station-form-runtime__formula-action-control">
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('recorderTime')"
                  size="small"
                  @change="
                    (event) =>
                      writeHeaderKey('recorderTime', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('recorderTime', '-') }}</strong>
              </span>
            </div>
            <template v-if="showPassWorkConfirmAction">
              <div class="station-form-runtime__formula-action-cell">
                <label>确认结果</label>
                <span class="station-form-runtime__formula-action-control">
                  <RadioGroup
                    v-if="!readonly"
                    :value="readHeaderKey('inspectionResult', 'OK')"
                    class="station-form-runtime__radio-group"
                    size="small"
                    @update:value="
                      (value) => writeHeaderKey('inspectionResult', value)
                    "
                  >
                    <Radio value="OK">OK</Radio>
                    <Radio value="NG">NG</Radio>
                  </RadioGroup>
                  <strong v-else>{{
                    readHeaderKey('inspectionResult', '-')
                  }}</strong>
                </span>
              </div>
              <div class="station-form-runtime__formula-action-cell">
                <label>确认人</label>
                <span class="station-form-runtime__formula-action-control">
                  <Input
                    v-if="!readonly"
                    :value="readHeaderKey('confirmer')"
                    size="small"
                    @change="
                      (event) => writeHeaderKey('confirmer', event.target.value)
                    "
                  />
                  <strong v-else>{{ readHeaderKey('confirmer', '-') }}</strong>
                </span>
              </div>
              <div class="station-form-runtime__formula-action-cell">
                <label>确认时间</label>
                <span class="station-form-runtime__formula-action-control">
                  <Input
                    v-if="!readonly"
                    :value="readHeaderKey('confirmerTime')"
                    size="small"
                    @change="
                      (event) =>
                        writeHeaderKey('confirmerTime', event.target.value)
                    "
                  />
                  <strong v-else>{{
                    readHeaderKey('confirmerTime', '-')
                  }}</strong>
                </span>
              </div>
            </template>
          </div>
          <div v-else class="station-form-runtime__formula-action-grid">
            <div class="station-form-runtime__formula-action-section">
              <div class="station-form-runtime__formula-action-title">
                执行记录
              </div>
              <div class="station-form-runtime__formula-action-fields">
                <label>执行结果</label>
                <RadioGroup
                  v-if="!readonly"
                  :value="readHeaderKey('result', 'OK')"
                  class="station-form-runtime__radio-group"
                  size="small"
                  @update:value="(value) => writeHeaderKey('result', value)"
                >
                  <Radio value="OK">OK</Radio>
                  <Radio value="NG">NG</Radio>
                </RadioGroup>
                <strong v-else>{{ readHeaderKey('result', '-') }}</strong>
                <label>记录人</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('recorder')"
                  size="small"
                  @change="
                    (event) => writeHeaderKey('recorder', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('recorder', '-') }}</strong>
                <label>记录时间</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('recorderTime')"
                  size="small"
                  @change="
                    (event) =>
                      writeHeaderKey('recorderTime', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('recorderTime', '-') }}</strong>
                <label>执行备注</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('formRemark')"
                  size="small"
                  @change="
                    (event) => writeHeaderKey('formRemark', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('formRemark', '-') }}</strong>
              </div>
            </div>
            <div class="station-form-runtime__formula-action-section">
              <div class="station-form-runtime__formula-action-title">
                验证记录
              </div>
              <div class="station-form-runtime__formula-action-fields">
                <label>检验结果</label>
                <RadioGroup
                  v-if="!readonly"
                  :value="readHeaderKey('inspectionResult', 'OK')"
                  class="station-form-runtime__radio-group"
                  size="small"
                  @update:value="
                    (value) => writeHeaderKey('inspectionResult', value)
                  "
                >
                  <Radio value="OK">OK</Radio>
                  <Radio value="NG">NG</Radio>
                </RadioGroup>
                <strong v-else>{{
                  readHeaderKey('inspectionResult', '-')
                }}</strong>
                <label>确认人</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('confirmer')"
                  size="small"
                  @change="
                    (event) => writeHeaderKey('confirmer', event.target.value)
                  "
                />
                <strong v-else>{{ readHeaderKey('confirmer', '-') }}</strong>
                <label>确认时间</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('confirmerTime')"
                  size="small"
                  @change="
                    (event) =>
                      writeHeaderKey('confirmerTime', event.target.value)
                  "
                />
                <strong v-else>{{
                  readHeaderKey('confirmerTime', '-')
                }}</strong>
                <label>确认备注</label>
                <Input
                  v-if="!readonly"
                  :value="readHeaderKey('confirmRemark')"
                  size="small"
                  @change="
                    (event) =>
                      writeHeaderKey('confirmRemark', event.target.value)
                  "
                />
                <strong v-else>{{
                  readHeaderKey('confirmRemark', '-')
                }}</strong>
              </div>
            </div>
          </div>
        </template>
      </fieldset>

      <fieldset
        v-if="signatureFields.length > 0 && !isFormulaPassWorkRuntime"
        class="station-form-runtime__fieldset station-form-runtime__signature"
      >
        <legend>{{ signatureSection?.title || '签名区' }}</legend>
        <div class="station-form-runtime__signature-grid">
          <div
            v-for="field in signatureFields"
            :key="fieldKey(field)"
            class="station-form-runtime__signature-cell"
          >
            <span class="station-form-runtime__label">{{
              fieldLabel(field)
            }}</span>
            <Select
              v-if="!isReadonlyField(field) && isSelectField(field)"
              :options="fieldOptions(field)"
              :value="readHeaderValue(field)"
              @change="(value) => writeHeaderValue(field, value)"
            />
            <Input
              v-else-if="!isReadonlyField(field)"
              :value="readHeaderValue(field)"
              @change="(event) => writeHeaderValue(field, event.target.value)"
            />
            <strong v-else>{{ displayText(readHeaderValue(field)) }}</strong>
          </div>
        </div>
      </fieldset>
    </div>

    <div v-else class="station-form-runtime__fallback">
      <Tag color="warning">未配置运行布局</Tag>
      <span>请先在 DEV 表单设计器中配置 runtimeLayout.sections。</span>
    </div>
  </div>
</template>

<style scoped>
.station-form-runtime {
  display: flex;
  flex-direction: column;
  min-width: 960px;
  min-height: 0;
  color: #001a3a;
  background: #f2f6fb;
  border: 1px solid #8fa2b8;
}

.station-form-runtime--compact {
  min-width: 0;
}

.station-form-runtime--scroll-body,
.station-form-runtime--scroll-detail {
  height: 100%;
  overflow: hidden;
}

.station-form-runtime__toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 8px 12px;
  background: #d8e2ee;
  border-bottom: 1px solid #9aaec4;
}

.station-form-runtime__title {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  font-size: 15px;
}

.station-form-runtime__title strong {
  font-size: 18px;
}

.station-form-runtime__body {
  display: grid;
  min-height: 0;
  gap: 10px;
  padding: 10px;
}

.station-form-runtime--scroll-body .station-form-runtime__body {
  flex: 1;
  /* 正文整体滚动时，各区块按内容高度排列，不能压缩表头来容纳明细。 */
  grid-auto-rows: max-content;
  align-content: start;
  overflow: auto;
}

.station-form-runtime--scroll-body .station-form-runtime__header-fieldset {
  /* 横向和纵向溢出均交给正文，避免表头形成第二个滚动容器。 */
  overflow: visible;
}

.station-form-runtime--scroll-detail .station-form-runtime__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.station-form-runtime__fieldset {
  box-sizing: border-box;
  padding: 14px 12px 12px;
  margin: 0;
  background: #eef4fa;
  border: 1px solid #8fa2b8;
}

.station-form-runtime--scroll-detail .station-form-runtime__fieldset {
  flex-shrink: 0;
}

.station-form-runtime--scroll-detail .station-form-runtime__detail,
.station-form-runtime--scroll-detail .station-form-runtime__formula-detail {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 160px;
  overflow: hidden;
}

.station-form-runtime--scroll-detail
  .station-form-runtime__formula-detail--fixed {
  flex: 0 0 auto;
}

.station-form-runtime__fieldset legend {
  padding: 0 10px;
  font-weight: 700;
  color: #005b89;
}

.station-form-runtime__legend {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.station-form-runtime__legend-toggle {
  height: 22px;
  padding: 0 8px;
  color: #1677ff;
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #9ec7ff;
  border-radius: 3px;
}

.station-form-runtime__legend-toggle:hover {
  color: #0958d9;
  border-color: #1677ff;
}

.station-form-runtime__header-fieldset {
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
}

.station-form-runtime__head-grid {
  display: grid;
  border-top: 1px solid #bfd0e0;
  border-left: 1px solid #bfd0e0;
}

.station-form-runtime__formula-head-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.station-form-runtime__formula-head-grid--compact {
  gap: 0;
  border-top: 1px solid #bfd0e0;
  border-left: 1px solid #bfd0e0;
}

.station-form-runtime__formula-head-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.station-form-runtime__formula-head-grid--compact
  .station-form-runtime__formula-head-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  min-height: 36px;
  gap: 0;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__formula-head-label {
  color: #4b5563;
  font-weight: 700;
}

.station-form-runtime__formula-head-grid--compact
  .station-form-runtime__formula-head-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  padding: 4px 10px;
  color: #001f3f;
  background: #d0d9e6;
}

.station-form-runtime__formula-head-value {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
  background: #fafafa;
  border: 1px solid #d9d9d9;
}

.station-form-runtime__formula-head-value :deep(.ant-input),
.station-form-runtime__formula-head-value :deep(.ant-input-number),
.station-form-runtime__formula-head-value :deep(.ant-select) {
  width: 100%;
}

.station-form-runtime__formula-head-grid--compact
  .station-form-runtime__formula-head-value {
  min-width: 0;
  min-height: 34px;
  padding: 4px 10px;
  overflow: hidden;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
  border: 0;
}

.station-form-runtime__head-cell,
.station-form-runtime__signature-cell {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  min-height: 42px;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 6px 10px;
  font-weight: 700;
  background: #d0d9e6;
}

.station-form-runtime__value,
.station-form-runtime__signature-cell strong {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 6px 10px;
  background: #fff;
}

.station-form-runtime :deep(.ant-input),
.station-form-runtime :deep(.ant-input-number),
.station-form-runtime :deep(.ant-select) {
  width: 100%;
  border-radius: 0;
}

.station-form-runtime__matrix {
  display: grid;
  border-top: 1px solid #bfd0e0;
  border-left: 1px solid #bfd0e0;
}

.station-form-runtime__matrix-head,
.station-form-runtime__matrix-cell {
  min-height: 40px;
  padding: 8px 10px;
  text-align: center;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__matrix-head {
  font-weight: 700;
  background: #d0d9e6;
}

.station-form-runtime__matrix-cell {
  background: #fff;
}

.station-form-runtime__attachments {
  overflow: hidden;
}

.station-form-runtime__attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 100%;
  overflow: auto;
}

.station-form-runtime__attachment-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 0;
}

.station-form-runtime__attachment-empty :deep(.ant-empty) {
  margin: 0;
}

.station-form-runtime__attachment {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 6px 10px;
  color: #1677ff;
  cursor: pointer;
  background: #fff;
  border: 1px solid #bfd0e0;
}

.station-form-runtime__attachment em {
  font-style: normal;
  color: #66788a;
}

.station-form-runtime__row-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.station-form-runtime__check-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.station-form-runtime__table-wrap {
  overflow: auto;
  background: #fff;
  border: 1px solid #bfd0e0;
}

.station-form-runtime--scroll-detail .station-form-runtime__table-wrap {
  flex: 1;
  min-height: 0;
}

.station-form-runtime__table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.station-form-runtime__table th,
.station-form-runtime__table td {
  height: 38px;
  padding: 6px 8px;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__table th {
  font-weight: 700;
  text-align: center;
  background: #d0d9e6;
}

.station-form-runtime__formula-table-wrap {
  max-width: 100%;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  background: #fff;
  border: 1px solid #bfd0e0;
}

.station-form-runtime__formula-detail--fixed
  .station-form-runtime__formula-table-wrap {
  flex: 1 1 auto;
  height: auto;
  min-height: 0;
}

.station-form-runtime--scroll-detail .station-form-runtime__formula-table-wrap {
  flex: 1;
  min-height: 0;
}

.station-form-runtime__formula-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
}

.station-form-runtime__formula-table th,
.station-form-runtime__formula-table td {
  min-height: 30px;
  padding: 4px 6px;
  vertical-align: middle;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__formula-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-weight: 700;
  text-align: center;
  background: #d0d9e6;
}

.station-form-runtime__formula-record-label {
  color: #4b5563;
  font-weight: 700;
  text-align: center;
}

.station-form-runtime__formula-step-start td {
  border-top: 2px solid #b8c7d8;
}

.station-form-runtime__radio-group {
  display: inline-flex;
  flex-wrap: nowrap;
  justify-content: center;
  white-space: nowrap;
}

.station-form-runtime__formula-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.station-form-runtime__formula-action--collapsed {
  padding-bottom: 8px;
}

.station-form-runtime__formula-action-grid--compact {
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 0;
  border-top: 1px solid #bfd0e0;
  border-left: 1px solid #bfd0e0;
}

.station-form-runtime__formula-action-cell {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  min-height: 38px;
  border-right: 1px solid #bfd0e0;
  border-bottom: 1px solid #bfd0e0;
}

.station-form-runtime__formula-action-cell label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  padding: 4px 10px;
  color: #001f3f;
  font-weight: 700;
  background: #d0d9e6;
}

.station-form-runtime__formula-action-control {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 4px 10px;
  background: #fff;
}

.station-form-runtime__formula-action-control strong {
  overflow: hidden;
  color: #374151;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-form-runtime__formula-action-title {
  margin-bottom: 8px;
  color: #1677ff;
  font-weight: 700;
}

.station-form-runtime__formula-action-fields {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 8px 12px;
  align-items: center;
}

.station-form-runtime__formula-action-fields label {
  color: #4b5563;
  font-weight: 700;
  text-align: right;
}

.station-form-runtime__formula-action-fields strong {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 4px 11px;
  color: #374151;
  font-weight: 400;
  background: #fafafa;
  border: 1px solid #d9d9d9;
}

.station-form-runtime__action-col {
  width: 120px;
}

.station-form-runtime__row-op {
  white-space: nowrap;
}

.station-form-runtime__empty,
.station-form-runtime__fallback {
  padding: 24px;
  color: #66788a;
  text-align: center;
}

.station-form-runtime__signature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  border-top: 1px solid #bfd0e0;
  border-left: 1px solid #bfd0e0;
}

@media (max-width: 1280px) {
  .station-form-runtime__head-grid,
  .station-form-runtime__signature-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .station-form-runtime {
    min-width: 0;
  }

  .station-form-runtime__head-grid,
  .station-form-runtime__signature-grid {
    grid-template-columns: 1fr;
  }

  .station-form-runtime__head-cell,
  .station-form-runtime__signature-cell {
    grid-template-columns: 110px minmax(0, 1fr);
  }
}
</style>
