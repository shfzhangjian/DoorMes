<script lang="ts" setup>
import type { PropType } from 'vue';

import { computed, defineComponent, h, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, InputNumber, message, Select, Switch, Tabs, Tag } from 'ant-design-vue';

import { isPressSlotProductionCheck } from '../../shared/pressSlotProductionCheck';

type RuntimeNodeType = 'attachment' | 'column' | 'control' | 'field' | 'layout' | 'root' | 'row' | 'section';

type RuntimeNode = {
  index?: number;
  key: string;
  nodeId?: string;
  sectionKey?: string;
  type: RuntimeNodeType;
};

type RuntimeRenderNode = {
  binding?: Record<string, any>;
  children?: RuntimeRenderNode[];
  componentName?: string;
  component?: string;
  id: string;
  nodeKind?: 'component' | 'layout';
  props?: Record<string, any>;
  slots?: Record<string, RuntimeRenderNode[]>;
  type: string;
};

type RuntimeSection = Record<string, any> & {
  columns?: Array<Record<string, any>> | number;
  fields?: Array<Record<string, any>>;
  key?: string;
  title?: string;
  type?: string;
};

type RuntimeClipboardPayload = {
  data: Record<string, any>;
  label: string;
  type: RuntimeNodeType;
};

type PaletteTool = {
  binding?: Record<string, any>;
  component?: string;
  defaultField?: string;
  defaultLabel?: string;
  fieldPrefix?: string;
  icon: string;
  key: string;
  kind: 'attachment' | 'column' | 'container' | 'control' | 'field' | 'row' | 'section';
  nodeType?: string;
  props?: Record<string, any>;
  sectionKey?: string;
  sectionType?: string;
  title?: string;
  unique?: boolean;
  width?: number;
};

const props = defineProps<{
  formName?: string;
  headerData: Record<string, any>;
  items: Array<Record<string, any>>;
  schema: Record<string, any>;
}>();

const emit = defineEmits<{
  'update:headerData': [value: Record<string, any>];
  'update:schema': [value: Record<string, any>];
}>();

const componentOptions = [
  { label: '文本显示', value: 'Text' },
  { label: '文本输入', value: 'Input' },
  { label: '数字输入', value: 'InputNumber' },
  { label: '下拉选择', value: 'Select' },
  { label: '附件列表', value: 'AttachmentList' },
];
const layoutScrollModeOptions = [
  { label: '自动', value: '' },
  { label: '页面自然滚动', value: 'page' },
  { label: '整张表单滚动', value: 'body' },
  { label: '明细区域滚动', value: 'detail' },
];
const businessProviderOptions = [
  { label: '无业务绑定', value: '' },
  { label: '压槽中间品记录', value: 'PRESS_SLOT_INTERMEDIATE' },
  { label: '粘胶1中间品记录', value: 'ADHESIVE1_INTERMEDIATE' },
  { label: '粘胶2中间品记录', value: 'ADHESIVE2_INTERMEDIATE' },
  { label: '配料过站表单', value: 'FORMULA_PASS_WORK' },
];
const businessLoadModeOptions = [
  { label: '查询或初始化', value: 'getOrInit' },
  { label: '只查询已有记录', value: 'get' },
  { label: '列表查询', value: 'list' },
];
const modelScopeOptions = [
  { label: '通用兜底', value: 'COMMON' },
  { label: '指定型号', value: 'MODEL' },
  { label: '型号前缀', value: 'PREFIX' },
];
const runtimeComponentRegistry: Record<string, { componentName: string; nodeKind: 'component' | 'layout' }> = {
  AttachmentList: { componentName: 'RuntimeAttachmentList', nodeKind: 'component' },
  Border: { componentName: 'RuntimeBorder', nodeKind: 'layout' },
  EditableTable: { componentName: 'RuntimeEditableTable', nodeKind: 'component' },
  Field: { componentName: 'RuntimeField', nodeKind: 'component' },
  Grid: { componentName: 'RuntimeGrid', nodeKind: 'layout' },
  Page: { componentName: 'RuntimePage', nodeKind: 'layout' },
  Panel: { componentName: 'RuntimePanel', nodeKind: 'layout' },
  SignatureGrid: { componentName: 'RuntimeSignatureGrid', nodeKind: 'component' },
  Split: { componentName: 'RuntimeSplit', nodeKind: 'layout' },
  TableRegion: { componentName: 'RuntimeTableRegion', nodeKind: 'layout' },
  Toolbar: { componentName: 'RuntimeToolbar', nodeKind: 'layout' },
};
const containerTools: PaletteTool[] = [
  { icon: 'lucide:panel-top', key: 'Border', kind: 'container', nodeType: 'Border', props: { title: '边框容器' }, title: '边框容器' },
  { icon: 'lucide:grid-3x3', key: 'Grid', kind: 'container', nodeType: 'Grid', props: { columns: 3, gap: 0 }, title: '网格容器' },
  { icon: 'lucide:panel-bottom', key: 'Panel', kind: 'container', nodeType: 'Panel', props: { title: '面板容器' }, title: '面板容器' },
  { icon: 'lucide:columns-3', key: 'Split', kind: 'container', nodeType: 'Split', props: { columns: '1fr 1fr' }, title: '分栏容器' },
  { icon: 'lucide:table-2', key: 'TableRegion', kind: 'container', nodeType: 'TableRegion', props: { title: '表格区域' }, title: '表格区域' },
  { icon: 'lucide:panel-top-open', key: 'Toolbar', kind: 'container', nodeType: 'Toolbar', props: { title: '工具条' }, title: '工具条' },
];
const controlTools: PaletteTool[] = [
  { component: 'Text', defaultLabel: '只读文本', fieldPrefix: 'textField', icon: 'lucide:type', key: 'TextField', kind: 'control', nodeType: 'Field', title: '只读文本' },
  { component: 'Input', defaultLabel: '文本输入', fieldPrefix: 'inputField', icon: 'lucide:text-cursor-input', key: 'InputField', kind: 'control', nodeType: 'Field', title: '文本输入' },
  { component: 'InputNumber', defaultLabel: '数字输入', fieldPrefix: 'numberField', icon: 'lucide:hash', key: 'NumberField', kind: 'control', nodeType: 'Field', title: '数字输入' },
  { component: 'Select', defaultLabel: '下拉选择', fieldPrefix: 'selectField', icon: 'lucide:list-filter', key: 'SelectField', kind: 'control', nodeType: 'Field', title: '下拉选择' },
  { component: 'EditableTable', icon: 'lucide:table-2', key: 'EditableTable', kind: 'control', nodeType: 'EditableTable', props: { title: '明细表' }, title: '明细表' },
  { component: 'AttachmentList', icon: 'lucide:paperclip', key: 'AttachmentList', kind: 'control', nodeType: 'AttachmentList', props: { title: '附件列表' }, title: '附件列表' },
  { component: 'SignatureGrid', icon: 'lucide:signature', key: 'SignatureGrid', kind: 'control', nodeType: 'SignatureGrid', props: { title: '签名区' }, title: '签名区' },
];
const layoutTools: PaletteTool[] = [
  { icon: 'lucide:table-properties', key: 'formInfo', kind: 'section', sectionKey: 'formInfo', sectionType: 'headerGrid', title: '表单信息' },
  { icon: 'lucide:grid-2x2', key: 'firstSlotDepth', kind: 'section', sectionKey: 'firstSlotDepth', sectionType: 'matrix', title: '首件槽深/mm' },
  { icon: 'lucide:paperclip', key: 'attachments', kind: 'section', sectionKey: 'attachments', sectionType: 'attachmentList', title: '原始导入附件' },
  { icon: 'lucide:table-2', key: 'intermediateDetails', kind: 'section', sectionKey: 'intermediateDetails', sectionType: 'editableTable', title: '中间品记录明细' },
  { icon: 'lucide:signature', key: 'signature', kind: 'section', sectionKey: 'signature', sectionType: 'signatureGrid', title: '签名区' },
  { icon: 'lucide:panel-top-open', key: 'customSection', kind: 'section', sectionKey: 'customSection', sectionType: 'headerGrid', title: '自定义分区', unique: true },
];
const fieldTools: PaletteTool[] = [
  { component: 'Text', defaultLabel: '只读文本', fieldPrefix: 'textField', icon: 'lucide:type', key: 'textField', kind: 'field' },
  { component: 'Input', defaultLabel: '文本输入', fieldPrefix: 'inputField', icon: 'lucide:text-cursor-input', key: 'inputField', kind: 'field' },
  { component: 'InputNumber', defaultLabel: '数字输入', fieldPrefix: 'numberField', icon: 'lucide:hash', key: 'numberField', kind: 'field' },
  { component: 'Input', defaultLabel: '日期字段', fieldPrefix: 'dateField', icon: 'lucide:calendar-days', key: 'dateField', kind: 'field' },
  { component: 'Select', defaultLabel: '下拉字段', fieldPrefix: 'selectField', icon: 'lucide:list-filter', key: 'selectField', kind: 'field' },
];
const tableTools: PaletteTool[] = [
  { component: 'Text', defaultField: 'samplePositionName', defaultLabel: '采样段', icon: 'lucide:map-pin', key: 'samplePositionName', kind: 'column', width: 100 },
  { component: 'Input', defaultField: 'sliceBatchNo', defaultLabel: '片号', icon: 'lucide:scan-line', key: 'sliceBatchNo', kind: 'column', width: 180 },
  { component: 'InputNumber', defaultField: 'widthMm', defaultLabel: '宽幅/mm', icon: 'lucide:ruler', key: 'widthMm', kind: 'column', width: 120 },
  { component: 'InputNumber', defaultLabel: '厚度/mm', fieldPrefix: 'thickness', icon: 'lucide:gauge', key: 'thickness', kind: 'column', width: 190 },
  { component: 'Input', defaultLabel: '备注', defaultField: 'remark', icon: 'lucide:notebook-pen', key: 'remark', kind: 'column', width: 180 },
  { icon: 'lucide:rows-3', key: 'detailRow', kind: 'row', title: '预览行' },
  { icon: 'lucide:paperclip', key: 'attachment', kind: 'attachment', title: '附件示例' },
];

const selectedNode = ref<RuntimeNode>({ key: 'runtime-form', type: 'root' });
const inspectorTab = ref('props');
const leftPanelTab = ref('palette');
const clipboardPayload = ref<RuntimeClipboardPayload>();
const schemaJsonText = ref('{}');
const headerJsonText = ref('{}');
const businessBindingJsonText = ref('{}');
const excelImportColumnsJsonText = ref('[]');

const runtimeLayout = computed(() => props.schema?.runtimeLayout || {});
const excelImportConfig = computed<Record<string, any>>(() => {
  const config = runtimeLayout.value.excelImport || {};
  return config && typeof config === 'object' && !Array.isArray(config) ? config : {};
});
const excelImportDetailConfig = computed<Record<string, any>>(() => {
  const detail = excelImportConfig.value.detail || {};
  return detail && typeof detail === 'object' && !Array.isArray(detail) ? detail : {};
});
const businessBinding = computed<Record<string, any>>(() => props.schema?.businessBinding || {});
const isProductionCheck = computed(() => isPressSlotProductionCheck(props.schema || {}));
const modelMatch = computed<Record<string, any>>(() => props.schema?.modelMatch || createModelMatchShell());
const renderTree = computed<RuntimeRenderNode>(() => runtimeLayout.value.renderTree || createDefaultRenderTree());
const flatRenderTreeNodes = computed(() => flattenRenderTree(renderTree.value));
const runtimeSections = computed<RuntimeSection[]>(() =>
  Array.isArray(runtimeLayout.value.sections) ? runtimeLayout.value.sections : [],
);
const formInfoSection = computed(() => findSection('formInfo', 'headerGrid'));
const depthSection = computed(() => findSection('firstSlotDepth', 'matrix'));
const attachmentSection = computed(() => findSection('attachments', 'attachmentList'));
const detailSection = computed(() => findSection('intermediateDetails', 'editableTable'));
const signatureSection = computed(() => findSection('signature', 'signatureGrid'));
const formInfoFields = computed(() => ensureArray(formInfoSection.value?.fields));
const depthFields = computed(() => ensureArray(depthSection.value?.fields));
const detailColumns = computed(() => ensureArray(detailSection.value?.columns));
const signatureFields = computed(() => ensureArray(signatureSection.value?.fields));
const detailRows = computed(() => {
  const rows = ensureArray(props.headerData?.previewDetails);
  if (rows.length) return rows;
  return props.items.map((item, index) => ({
    actualValue: item.defaultResult && item.defaultResult !== 'OK' ? item.defaultResult : '-',
    itemCategory: item.itemCategory || '',
    itemName: item.itemName || '',
    remark: item.remark || '-',
    resultFlag: item.defaultResult || 'OK',
    seq: item.itemSeq || index + 1,
    standardText: item.standardText || '-',
    stepNode: item.stepNode || '',
  }));
});
const attachments = computed(() => ensureArray(props.headerData?.attachments));
const selectedSection = computed(() => findSection(selectedNode.value.sectionKey || selectedNode.value.key));
const selectedField = computed(() => {
  if (selectedNode.value.type !== 'field' || selectedNode.value.index === undefined) return undefined;
  return ensureArray(selectedSection.value?.fields)[selectedNode.value.index];
});
const selectedColumn = computed(() => {
  if (selectedNode.value.type !== 'column' || selectedNode.value.index === undefined) return undefined;
  return detailColumns.value[selectedNode.value.index];
});
const selectedRow = computed(() => {
  if (selectedNode.value.type !== 'row' || selectedNode.value.index === undefined) return undefined;
  return detailRows.value[selectedNode.value.index];
});
const selectedAttachment = computed(() => {
  if (selectedNode.value.type !== 'attachment' || selectedNode.value.index === undefined) return undefined;
  return attachments.value[selectedNode.value.index];
});
const selectedRenderTreeNode = computed(() =>
  selectedNode.value.nodeId ? findRenderTreeNode(renderTree.value, selectedNode.value.nodeId)?.node : undefined,
);
const selectedNodeTitle = computed(() => {
  if (selectedNode.value.type === 'root') return '运行时表单';
  if (selectedNode.value.type === 'layout' || selectedNode.value.type === 'control') {
    return selectedRenderTreeNode.value?.props?.title || selectedRenderTreeNode.value?.props?.label || selectedRenderTreeNode.value?.id || '渲染节点';
  }
  if (selectedNode.value.type === 'section') return selectedSection.value?.title || selectedNode.value.key;
  if (selectedNode.value.type === 'field') return selectedField.value?.label || selectedField.value?.field || '字段';
  if (selectedNode.value.type === 'column') return selectedColumn.value?.label || selectedColumn.value?.field || '列';
  if (selectedNode.value.type === 'row') return selectedRow.value?.samplePositionName || `数据行${(selectedNode.value.index || 0) + 1}`;
  return selectedAttachment.value?.name || `附件${(selectedNode.value.index || 0) + 1}`;
});
const selectedNodeKind = computed(() => {
  if (selectedNode.value.type === 'root') return 'Vue JSON 组件树';
  if (selectedNode.value.type === 'layout' || selectedNode.value.type === 'control') {
    const node = selectedRenderTreeNode.value;
    const kindName = resolveRenderNodeKind(node) === 'layout' ? '布局容器' : '业务组件';
    return `${kindName} / ${resolveRenderNodeComponentName(node) || '组件'}`;
  }
  if (selectedNode.value.type === 'section') return `兼容区块 / ${selectedSection.value?.type || '区块'}`;
  if (selectedNode.value.type === 'field') return `兼容字段 / ${selectedField.value?.component || '字段'}`;
  if (selectedNode.value.type === 'column') return `明细列 / ${selectedColumn.value?.component || '列'}`;
  if (selectedNode.value.type === 'row') return '预览数据行';
  return '附件示例';
});
const clipboardLabel = computed(() => clipboardPayload.value?.label || '空');
const canDeleteSelectedElement = computed(() => selectedNode.value.type !== 'root');
const canCopySelectedElement = computed(() => selectedNode.value.type !== 'root');
const canPasteClipboardElement = computed(() => !!clipboardPayload.value);
const thicknessStandard = computed(() => {
  const thicknessItem = props.items.find((item) => `${item.itemName || ''}${item.standardText || ''}`.includes('厚度'));
  return extractStandard(thicknessItem?.standardText) || '-';
});
const depthStandard = computed(() => {
  const depthItem = props.items.find((item) => `${item.itemCategory || ''}${item.itemName || ''}`.includes('槽深'));
  return depthItem?.standardText || '-';
});

watch(
  () => props.schema,
  () => {
    schemaJsonText.value = JSON.stringify(props.schema || {}, null, 2);
    businessBindingJsonText.value = JSON.stringify(props.schema?.businessBinding || {}, null, 2);
  },
  { deep: true, immediate: true },
);

watch(
  () => props.headerData,
  () => {
    headerJsonText.value = JSON.stringify(props.headerData || {}, null, 2);
  },
  { deep: true, immediate: true },
);

watch(
  () => excelImportDetailConfig.value.columns,
  (value) => {
    excelImportColumnsJsonText.value = JSON.stringify(Array.isArray(value) ? value : [], null, 2);
  },
  { deep: true, immediate: true },
);

function selectNode(node: RuntimeNode) {
  selectedNode.value = node;
  inspectorTab.value = 'props';
}

function isNodeActive(node: RuntimeNode) {
  return (
    selectedNode.value.type === node.type &&
    selectedNode.value.key === node.key &&
    selectedNode.value.nodeId === node.nodeId &&
    selectedNode.value.sectionKey === node.sectionKey &&
    selectedNode.value.index === node.index
  );
}

function handlePaletteTool(tool: PaletteTool) {
  if (tool.kind === 'container' || tool.kind === 'control') {
    addRenderTreeNode(tool);
  } else if (tool.kind === 'section') {
    addPresetRenderSection(tool);
  } else if (tool.kind === 'field') {
    addRuntimeFieldFromTool(tool);
  } else if (tool.kind === 'column') {
    addRuntimeColumn(undefined, tool);
  } else if (tool.kind === 'row') {
    insertRuntimeRow();
  } else if (tool.kind === 'attachment') {
    addAttachment();
  }
}

function renderTreeNodeObjectNode(node: RuntimeRenderNode): RuntimeNode {
  const isContainer = isRenderContainer(node);
  return {
    key: node.id,
    nodeId: node.id,
    type: isContainer ? 'layout' : 'control',
  };
}

function renderCanvasNodeObjectNode(node: RuntimeRenderNode): RuntimeNode {
  const runtimeField = resolveRenderNodeRuntimeField(node);
  if (runtimeField) {
    return fieldNode(runtimeField.section, runtimeField.field, runtimeField.index);
  }
  return renderTreeNodeObjectNode(node);
}

function resolveRenderCanvasChildren(node: RuntimeRenderNode) {
  const children = resolveRenderNodeChildren(node);
  if (node.props?.sectionReference === true) {
    return [...createLegacySectionRenderChildren(node), ...children.filter((child) => child.type !== 'Field')];
  }
  return children.length ? children : createLegacySectionRenderChildren(node);
}

function createLegacySectionRenderChildren(node: RuntimeRenderNode) {
  if (!['Border', 'Grid', 'Panel', 'Split'].includes(node.type)) return [];
  const sectionKey = resolveRuntimeSectionKeyFromRenderNode(node);
  if (!['firstSlotDepth', 'formInfo', 'signature'].includes(sectionKey)) return [];
  const section = findRuntimeSectionByRenderKey(sectionKey);
  const fields = ensureArray(section?.fields);
  if (!fields.length) return [];
  return fields.map((field, index) => createLegacyFieldRenderNode(sectionKey, field, index));
}

function createLegacyFieldRenderNode(sectionKey: string, field: Record<string, any>, index: number): RuntimeRenderNode {
  const fieldKey = field.field || field.key || `${sectionKey}Field${index + 1}`;
  const safeFieldKey = String(fieldKey).replace(/[^a-zA-Z0-9_]/g, '_') || `field${index + 1}`;
  return withRenderNodeMetadata({
    binding: { path: fieldKey, source: 'header', type: 'value' },
    component: field.component || 'Input',
    id: `legacy_${sectionKey}_${safeFieldKey}`,
    props: {
      editable: field.editable,
      height: field.height,
      label: field.label || fieldKey,
      runtimeFieldIndex: index,
      runtimeSectionKey: sectionKey,
      width: field.width,
    },
    type: 'Field',
  });
}

function resolveRenderNodeRuntimeField(node: RuntimeRenderNode) {
  const sectionKey = node.props?.runtimeSectionKey;
  const index = Number(node.props?.runtimeFieldIndex);
  if (!sectionKey || !Number.isInteger(index) || index < 0) return undefined;
  const section = findRuntimeSectionByRenderKey(sectionKey);
  const field = ensureArray(section?.fields)[index];
  if (!section || !field) return undefined;
  return { field, index, section };
}

function findSection(key: string, type?: string) {
  return runtimeSections.value.find((section) => section.key === key || (!!type && section.type === type));
}

function findRuntimeSectionByRenderKey(sectionKey: string) {
  const typeMap: Record<string, string> = {
    firstSlotDepth: 'matrix',
    formInfo: 'headerGrid',
    signature: 'signatureGrid',
  };
  return findSection(sectionKey, typeMap[sectionKey]);
}

function sectionNode(section: RuntimeSection): RuntimeNode {
  const key = section.key || section.type || 'section';
  return { key, sectionKey: key, type: 'section' };
}

function fieldNode(section: RuntimeSection, field: Record<string, any>, index: number): RuntimeNode {
  const sectionKey = section.key || section.type || '';
  return {
    index,
    key: `${sectionKey}.${field.field || index}`,
    sectionKey,
    type: 'field',
  };
}

function columnNode(column: Record<string, any>, index: number): RuntimeNode {
  return {
    index,
    key: `intermediateDetails.${column.field || column.key || index}`,
    sectionKey: 'intermediateDetails',
    type: 'column',
  };
}

function rowNode(row: Record<string, any>, index: number): RuntimeNode {
  return {
    index,
    key: `previewDetails.${row.seq || index}`,
    sectionKey: 'intermediateDetails',
    type: 'row',
  };
}

function attachmentNode(attachment: Record<string, any>, index: number): RuntimeNode {
  return {
    index,
    key: `attachments.${attachment.uid || attachment.name || index}`,
    sectionKey: 'attachments',
    type: 'attachment',
  };
}

function updateSchema(updater: (schema: Record<string, any>) => void) {
  const next = cloneRecord(props.schema);
  ensureRuntimeLayout(next);
  updater(next);
  emit('update:schema', next);
}

function updateHeaderData(updater: (headerData: Record<string, any>) => void) {
  const next = cloneRecord(props.headerData);
  updater(next);
  emit('update:headerData', next);
}

function updateRootProp(prop: string, value: any) {
  updateSchema((schema) => {
    if (value === undefined || value === '') {
      delete schema[prop];
    } else {
      schema[prop] = value;
    }
    if (prop === 'displayName') {
      ensureRuntimeLayout(schema).title = value;
    }
  });
}

function updateLayoutTitle(value: string) {
  updateSchema((schema) => {
    const layout = ensureRuntimeLayout(schema);
    layout.title = value;
    schema.displayName = value;
  });
}

function updateRuntimeLayoutProp(prop: string, value: any) {
  updateSchema((schema) => {
    const layout = ensureRuntimeLayout(schema);
    if (value === undefined || value === '') {
      delete layout[prop];
    } else {
      layout[prop] = value;
    }
  });
}

function createDefaultExcelImportColumns() {
  return detailColumns.value.map((column, index) => ({
    column: index + 1,
    field: columnKey(column, index),
    label: columnLabel(column, index),
  }));
}

function ensureExcelImportConfig(schema: Record<string, any>) {
  const layout = ensureRuntimeLayout(schema);
  if (!layout.excelImport || typeof layout.excelImport !== 'object' || Array.isArray(layout.excelImport)) {
    layout.excelImport = {};
  }
  if (!layout.excelImport.detail || typeof layout.excelImport.detail !== 'object' || Array.isArray(layout.excelImport.detail)) {
    layout.excelImport.detail = {};
  }
  return layout.excelImport;
}

function updateExcelImportEnabled(checked: boolean) {
  updateSchema((schema) => {
    const config = ensureExcelImportConfig(schema);
    config.enabled = checked;
    config.fillDetailFromSource = checked;
    config.source = config.source || 'original-excel';
    if (checked) {
      config.detail.startRow = config.detail.startRow || 7;
      config.detail.skipHeaderRows = config.detail.skipHeaderRows || 0;
      config.detail.stopPrefixes = Array.isArray(config.detail.stopPrefixes)
        ? config.detail.stopPrefixes
        : ['注：', '制定/修订部门', '本资料为'];
      config.detail.columns = Array.isArray(config.detail.columns) && config.detail.columns.length
        ? config.detail.columns
        : createDefaultExcelImportColumns();
    }
  });
}

function updateExcelImportDetailProp(prop: string, value: any) {
  updateSchema((schema) => {
    const config = ensureExcelImportConfig(schema);
    if (value === undefined || value === null || value === '') {
      delete config.detail[prop];
    } else {
      config.detail[prop] = value;
    }
  });
}

function formatExcelImportStopPrefixes() {
  const prefixes = excelImportDetailConfig.value.stopPrefixes;
  return Array.isArray(prefixes) ? prefixes.join('，') : '';
}

function updateExcelImportStopPrefixes(value: string) {
  updateExcelImportDetailProp(
    'stopPrefixes',
    String(value || '')
      .split(/[,，;；/、|\n]+/)
      .map((item) => item.trim())
      .filter(Boolean),
  );
}

function applyExcelImportColumnsJson() {
  try {
    const parsed = JSON.parse(excelImportColumnsJsonText.value);
    if (!Array.isArray(parsed)) throw new Error('列映射必须是 JSON 数组');
    updateExcelImportDetailProp('columns', parsed);
    message.success('Excel 导入列映射已应用');
  } catch (error: any) {
    message.error(error?.message || 'Excel 导入列映射 JSON 格式不正确');
  }
}

function resetExcelImportColumnsFromTable() {
  const columns = createDefaultExcelImportColumns();
  excelImportColumnsJsonText.value = JSON.stringify(columns, null, 2);
  updateExcelImportDetailProp('columns', columns);
  message.success('已按当前明细列生成 Excel 导入映射');
}

function updateSectionProp(prop: string, value: any) {
  const node = selectedNode.value;
  if (!node.sectionKey) return;
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, node.sectionKey);
    if (!section) return;
    section[prop] = value;
  });
}

function updateSelectedFieldProp(prop: string, value: any) {
  const node = selectedNode.value;
  if (!node.sectionKey || node.index === undefined) return;
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, node.sectionKey);
    const field = ensureArray(section?.fields)[node.index];
    if (!field) return;
    field[prop] = value;
    if (prop === 'field') {
      field.storage = `presetHeaderDataJson.${value}`;
    }
  });
}

function updateSelectedFieldValue(value: any) {
  const fieldKey = selectedField.value?.field;
  if (!fieldKey) return;
  updateHeaderData((headerData) => {
    headerData[fieldKey] = value;
  });
}

function updateSelectedColumnProp(prop: string, value: any) {
  const node = selectedNode.value;
  if (node.index === undefined) return;
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, 'intermediateDetails');
    const column = ensureArray(section?.columns)[node.index];
    if (!column) return;
    column[prop] = value;
  });
}

function updateSelectedRowValue(field: string, value: any) {
  const rowIndex = selectedNode.value.index;
  if (rowIndex === undefined) return;
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'previewDetails');
    rows[rowIndex] = {
      ...(rows[rowIndex] || {}),
      [field]: value,
    };
    normalizeDetailSeq(rows);
  });
}

function updateSelectedAttachmentValue(field: string, value: any) {
  const attachmentIndex = selectedNode.value.index;
  if (attachmentIndex === undefined) return;
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'attachments');
    rows[attachmentIndex] = {
      ...(rows[attachmentIndex] || {}),
      [field]: value,
    };
  });
}

function updateSelectedRenderNodeProp(prop: string, value: any) {
  const nodeId = selectedNode.value.nodeId;
  if (!nodeId) return;
  updateSchema((schema) => {
    const node = findRenderTreeNode(ensureRenderTree(schema), nodeId)?.node;
    if (!node) return;
    if (prop === 'component') {
      node.component = value;
      return;
    }
    node.props = {
      ...(node.props || {}),
      [prop]: value,
    };
  });
}

function updateSelectedRenderNodeBinding(prop: string, value: any) {
  const nodeId = selectedNode.value.nodeId;
  if (!nodeId) return;
  updateSchema((schema) => {
    const node = findRenderTreeNode(ensureRenderTree(schema), nodeId)?.node;
    if (!node) return;
    node.binding = {
      ...(node.binding || {}),
      [prop]: value,
    };
  });
}

function copySelectedElement() {
  const payload = buildClipboardPayload();
  if (!payload) {
    message.warning('当前对象不能复制');
    return;
  }
  clipboardPayload.value = payload;
  message.success(`已复制：${payload.label}`);
}

function pasteClipboardElement() {
  const payload = clipboardPayload.value;
  if (!payload) {
    message.warning('请先复制一个对象');
    return;
  }
  if (payload.type === 'layout' || payload.type === 'control') {
    pasteRenderTreeElement(payload);
  } else if (payload.type === 'section') {
    pasteRuntimeSection(payload);
  } else if (payload.type === 'field') {
    pasteRuntimeField(payload);
  } else if (payload.type === 'column') {
    pasteRuntimeColumn(payload);
  } else if (payload.type === 'row') {
    pasteRuntimeRow(payload);
  } else if (payload.type === 'attachment') {
    pasteRuntimeAttachment(payload);
  } else {
    message.warning('当前剪贴板对象暂不支持粘贴');
  }
}

function deleteSelectedElement() {
  const type = selectedNode.value.type;
  if (type === 'root') {
    message.warning('运行时表单根节点不能删除');
    return;
  }
  if (type === 'layout' || type === 'control') {
    removeSelectedRenderTreeNode();
  } else if (type === 'section') {
    removeRuntimeSection();
  } else if (type === 'field') {
    removeRuntimeField();
  } else if (type === 'column') {
    removeRuntimeColumn();
  } else if (type === 'row') {
    removeRuntimeRow();
  } else if (type === 'attachment') {
    removeAttachment();
  }
}

function buildClipboardPayload(): RuntimeClipboardPayload | undefined {
  const type = selectedNode.value.type;
  if (type === 'layout' || type === 'control') {
    if (!selectedRenderTreeNode.value || selectedRenderTreeNode.value.id === renderTree.value.id) return undefined;
    return {
      data: cloneRecord(selectedRenderTreeNode.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  if (type === 'section' && selectedSection.value) {
    return {
      data: cloneRecord(selectedSection.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  if (type === 'field' && selectedField.value) {
    return {
      data: cloneRecord(selectedField.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  if (type === 'column' && selectedColumn.value) {
    return {
      data: cloneRecord(selectedColumn.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  if (type === 'row' && selectedRow.value) {
    return {
      data: cloneRecord(selectedRow.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  if (type === 'attachment' && selectedAttachment.value) {
    return {
      data: cloneRecord(selectedAttachment.value),
      label: selectedNodeTitle.value,
      type,
    };
  }
  return undefined;
}

function pasteRenderTreeElement(payload: RuntimeClipboardPayload) {
  let insertedId = '';
  const valueBindingPaths: string[] = [];
  let blocked = false;
  updateSchema((schema) => {
    const tree = ensureRenderTree(schema);
    const parent = resolveRenderTreeInsertParent(schema, tree, payload.type === 'layout' ? 'container' : 'control');
    if (!parent) {
      blocked = true;
      return;
    }
    const pastedNode = preparePastedRenderTreeNode(tree, payload.data as RuntimeRenderNode, valueBindingPaths);
    const children = resolveRenderNodeChildren(parent);
    children.push(pastedNode);
    assignRenderNodeChildren(parent, children);
    syncRuntimeSectionFromInsertedNode(schema, pastedNode, parent);
    insertedId = pastedNode.id;
  });
  valueBindingPaths.forEach((path) => createHeaderBindingPlaceholder(path));
  if (blocked) {
    message.warning('请先选择可容纳该对象的布局容器');
    return;
  }
  if (insertedId) {
    selectedNode.value = {
      key: insertedId,
      nodeId: insertedId,
      type: payload.type === 'layout' ? 'layout' : 'control',
    };
  }
}

function preparePastedRenderTreeNode(
  tree: RuntimeRenderNode,
  sourceNode: RuntimeRenderNode,
  valueBindingPaths: string[],
) {
  const usedIds = new Set(flattenRenderTree(tree).map((item) => item.node.id));
  const usedBindings = new Set(
    flattenRenderTree(tree)
      .map((item) => item.node.binding?.path)
      .filter(Boolean) as string[],
  );

  const visit = (source: RuntimeRenderNode): RuntimeRenderNode => {
    const next = cloneRecord(source) as RuntimeRenderNode;
    const baseId = `${String(source.id || source.type || 'node').replace(/[^a-zA-Z0-9_]/g, '') || 'node'}Copy`;
    next.id = createUniqueNameWithSet(usedIds, baseId);
    next.componentName = next.componentName || resolveRenderNodeComponentName(next);
    next.nodeKind = next.nodeKind || resolveRenderNodeKind(next);
    if (next.binding?.path && next.binding.type === 'value') {
      const basePath = `${String(next.binding.path).replace(/[^a-zA-Z0-9_]/g, '') || 'field'}Copy`;
      next.binding = {
        ...next.binding,
        path: createUniqueNameWithSet(usedBindings, basePath),
      };
      valueBindingPaths.push(next.binding.path);
    }
    const children = resolveRenderNodeChildren(source).map((child) => visit(child));
    if (children.length || resolveRenderNodeKind(next) === 'layout') {
      assignRenderNodeChildren(next, children);
    }
    return next;
  };
  return visit(sourceNode);
}

function pasteRuntimeSection(payload: RuntimeClipboardPayload) {
  let nextSectionKey = '';
  updateSchema((schema) => {
    const layout = ensureRuntimeLayout(schema);
    const source = cloneRecord(payload.data) as RuntimeSection;
    const baseKey = `${source.key || source.type || 'section'}Copy`;
    nextSectionKey = createUniqueSectionKey(layout.sections, baseKey);
    source.key = nextSectionKey;
    source.title = `${source.title || payload.label || '分区'} 副本`;
    if (Array.isArray(source.fields)) {
      const existingFields = layout.sections.flatMap((section: RuntimeSection) => ensureArray(section.fields));
      source.fields = source.fields.map((field) => {
        const nextField = cloneRecord(field);
        const fieldKey = createUniqueFieldKey(existingFields, `${field.field || field.key || 'field'}Copy`);
        nextField.field = fieldKey;
        nextField.storage = `presetHeaderDataJson.${fieldKey}`;
        existingFields.push(nextField);
        return nextField;
      });
    }
    if (Array.isArray(source.columns)) {
      source.columns = source.columns.map((column, index) => ({
        ...cloneRecord(column),
        field: `${column.field || column.key || `column${index + 1}`}Copy`,
      }));
    }
    layout.sections.push(source);
  });
  if (nextSectionKey) {
    selectedNode.value = { key: nextSectionKey, sectionKey: nextSectionKey, type: 'section' };
  }
}

function pasteRuntimeField(payload: RuntimeClipboardPayload) {
  let insertedFieldKey = '';
  let targetSectionKey = resolveFieldTargetSectionKey();
  updateSchema((schema) => {
    const section = ensureRuntimeSection(schema, targetSectionKey, {
      icon: 'lucide:table-properties',
      key: targetSectionKey,
      kind: 'section',
      sectionKey: targetSectionKey,
      sectionType: targetSectionKey === 'signature' ? 'signatureGrid' : targetSectionKey === 'firstSlotDepth' ? 'matrix' : 'headerGrid',
      title: targetSectionKey === 'signature' ? '签名区' : targetSectionKey === 'firstSlotDepth' ? '首件槽深/mm' : '表单信息',
    });
    targetSectionKey = section.key || targetSectionKey;
    const fields = ensureSchemaArray(section, 'fields');
    const field = cloneRecord(payload.data);
    insertedFieldKey = createUniqueFieldKey(fields, `${field.field || field.key || 'field'}Copy`);
    field.field = insertedFieldKey;
    field.label = `${field.label || payload.label || '字段'} 副本`;
    field.storage = `presetHeaderDataJson.${insertedFieldKey}`;
    const targetIndex = selectedNode.value.type === 'field' && selectedNode.value.sectionKey === targetSectionKey
      ? Math.min((selectedNode.value.index || 0) + 1, fields.length)
      : fields.length;
    fields.splice(targetIndex, 0, field);
    selectedNode.value = { index: targetIndex, key: `${targetSectionKey}.${insertedFieldKey}`, sectionKey: targetSectionKey, type: 'field' };
  });
  if (insertedFieldKey) {
    updateHeaderData((headerData) => {
      headerData[insertedFieldKey] = '';
    });
  }
}

function pasteRuntimeColumn(payload: RuntimeClipboardPayload) {
  let insertedFieldKey = '';
  let targetIndex = detailColumns.value.length;
  updateSchema((schema) => {
    const section = ensureRuntimeSection(schema, 'intermediateDetails', {
      icon: 'lucide:table-2',
      key: 'intermediateDetails',
      kind: 'section',
      sectionKey: 'intermediateDetails',
      sectionType: 'editableTable',
      title: '中间品记录明细',
    });
    const columns = ensureSchemaArray(section, 'columns');
    const column = cloneRecord(payload.data);
    insertedFieldKey = createUniqueFieldKey(columns, `${column.field || column.key || 'customColumn'}Copy`);
    column.field = insertedFieldKey;
    column.label = `${column.label || payload.label || '新列'} 副本`;
    targetIndex = selectedNode.value.type === 'column'
      ? Math.min((selectedNode.value.index || 0) + 1, columns.length)
      : columns.length;
    columns.splice(targetIndex, 0, column);
    selectedNode.value = { index: targetIndex, key: `intermediateDetails.${insertedFieldKey}`, sectionKey: 'intermediateDetails', type: 'column' };
  });
  if (insertedFieldKey) {
    updateHeaderData((headerData) => {
      ensureHeaderArray(headerData, 'previewDetails').forEach((row) => {
        row[insertedFieldKey] = '';
      });
    });
  }
}

function pasteRuntimeRow(payload: RuntimeClipboardPayload) {
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'previewDetails');
    const targetIndex = selectedNode.value.type === 'row'
      ? Math.min((selectedNode.value.index || 0) + 1, rows.length)
      : rows.length;
    rows.splice(targetIndex, 0, cloneRecord(payload.data));
    normalizeDetailSeq(rows);
    selectedNode.value = { index: targetIndex, key: `previewDetails.${targetIndex + 1}`, sectionKey: 'intermediateDetails', type: 'row' };
  });
}

function pasteRuntimeAttachment(payload: RuntimeClipboardPayload) {
  updateHeaderData((headerData) => {
    const attachments = ensureHeaderArray(headerData, 'attachments');
    const targetIndex = selectedNode.value.type === 'attachment'
      ? Math.min((selectedNode.value.index || 0) + 1, attachments.length)
      : attachments.length;
    const attachment = cloneRecord(payload.data);
    attachment.uid = `${attachment.uid || attachment.name || 'attachment'}Copy${Date.now()}`;
    attachment.name = `${attachment.name || payload.label || '附件'} 副本`;
    attachments.splice(targetIndex, 0, attachment);
    selectedNode.value = { index: targetIndex, key: `attachments.${attachment.uid}`, sectionKey: 'attachments', type: 'attachment' };
  });
}

function removeSelectedRenderTreeNode() {
  const nodeId = selectedNode.value.nodeId;
  if (!nodeId || nodeId === renderTree.value.id) {
    message.warning('根组件不能删除');
    return;
  }
  let parentId = '';
  let removedBindingPaths: string[] = [];
  let removed = false;
  updateSchema((schema) => {
    const tree = ensureRenderTree(schema);
    const found = findRenderTreeNode(tree, nodeId);
    if (!found?.parent) return;
    removedBindingPaths = collectValueBindingPaths(found.node);
    const children = resolveRenderNodeChildren(found.parent).filter((child) => child.id !== nodeId);
    assignRenderNodeChildren(found.parent, children);
    parentId = found.parent.id;
    removed = true;
  });
  if (!removed) {
    message.warning('未找到可删除的组件节点');
    return;
  }
  if (removedBindingPaths.length) {
    updateHeaderData((headerData) => {
      removedBindingPaths.forEach((path) => delete headerData[path]);
    });
  }
  selectedNode.value = parentId
    ? { key: parentId, nodeId: parentId, type: 'layout' }
    : { key: 'runtime-form', type: 'root' };
}

function collectValueBindingPaths(node: RuntimeRenderNode) {
  const paths: string[] = [];
  const visit = (current: RuntimeRenderNode) => {
    if (current.binding?.type === 'value' && current.binding.path) {
      paths.push(current.binding.path);
    }
    resolveRenderNodeChildren(current).forEach((child) => visit(child));
  };
  visit(node);
  return paths;
}

function removeRuntimeSection() {
  const sectionKey = selectedNode.value.sectionKey || selectedNode.value.key;
  if (!sectionKey) return;
  let removedSection: RuntimeSection | undefined;
  updateSchema((schema) => {
    const layout = ensureRuntimeLayout(schema);
    const index = layout.sections.findIndex((section: RuntimeSection) => section.key === sectionKey);
    if (index < 0) return;
    removedSection = layout.sections[index];
    layout.sections.splice(index, 1);
    removeRenderTreeNodeById(ensureRenderTree(schema), resolvePresetRenderSectionRootId(sectionKey));
    removeRenderTreeNodeById(ensureRenderTree(schema), sectionKey);
  });
  if (removedSection) {
    updateHeaderData((headerData) => {
      cleanupSectionHeaderData(headerData, removedSection!);
    });
  }
  selectedNode.value = { key: 'runtime-form', type: 'root' };
}

function removeRuntimeField() {
  const sectionKey = selectedNode.value.sectionKey;
  const index = selectedNode.value.index;
  if (!sectionKey || index === undefined) return;
  let removedFieldKey = '';
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, sectionKey);
    const fields = ensureArray(section?.fields);
    removedFieldKey = fields[index]?.field || '';
    fields.splice(index, 1);
    if (section) section.fields = fields;
  });
  if (removedFieldKey) {
    updateHeaderData((headerData) => {
      delete headerData[removedFieldKey];
    });
  }
  selectedNode.value = { key: sectionKey, sectionKey, type: 'section' };
}

function addRenderTreeNode(tool: PaletteTool) {
  let insertedId = '';
  let insertedBindingPath = '';
  let blocked = false;
  updateSchema((schema) => {
    const tree = ensureRenderTree(schema);
    const parent = resolveRenderTreeInsertParent(schema, tree, tool);
    if (!parent) {
      blocked = true;
      return;
    }
    const baseId = String(tool.nodeType || tool.key || tool.kind).replace(/[^a-zA-Z0-9_]/g, '');
    const id = createUniqueRenderNodeId(tree, baseId.charAt(0).toLowerCase() + baseId.slice(1));
    const node = createRenderTreeNodeTemplate(tool, id);
    insertedBindingPath = node.binding?.type === 'value' ? node.binding.path : '';
    const children = resolveRenderNodeChildren(parent);
    children.push(node);
    assignRenderNodeChildren(parent, children);
    syncRuntimeSectionFromInsertedNode(schema, node, parent);
    insertedId = id;
    selectedNode.value = {
      key: id,
      nodeId: id,
      type: resolveRenderNodeKind(node) === 'layout' ? 'layout' : 'control',
    };
  });
  if (blocked) {
    message.warning('请先选择 Border、Grid、Panel、Toolbar、TableRegion 等布局容器，再放入业务组件');
    return;
  }
  if (tool.kind === 'control') {
    createHeaderBindingPlaceholder(insertedBindingPath);
  }
}

function createHeaderBindingPlaceholder(bindingPath: string) {
  if (!bindingPath) return;
  updateHeaderData((headerData) => {
    if (headerData[bindingPath] === undefined) {
      headerData[bindingPath] = '';
    }
  });
}
function addPresetRenderSection(tool: PaletteTool) {
  const sectionKey = tool.sectionKey || tool.key;
  let selectedId = '';
  let existed = false;
  updateSchema((schema) => {
    const tree = ensureRenderTree(schema);
    ensureRuntimeSection(schema, sectionKey, tool);
    const existingId = resolvePresetRenderSectionRootId(sectionKey);
    const existing = findRenderTreeNode(tree, existingId);
    if (existing) {
      selectedId = existing.node.id;
      existed = true;
      return;
    }
    const parent = resolveRenderTreeInsertParent(schema, tree, 'container') || tree;
    const node = createPresetRenderSectionNode(sectionKey, tree);
    const children = resolveRenderNodeChildren(parent);
    children.push(node);
    assignRenderNodeChildren(parent, children);
    selectedId = node.id;
  });
  if (selectedId) {
    selectedNode.value = { key: selectedId, nodeId: selectedId, type: 'layout' };
  }
  if (existed) {
    message.info(`${tool.title || sectionKey} 已存在，已为你选中`);
  }
}

function resolvePresetRenderSectionRootId(sectionKey: string) {
  const idMap: Record<string, string> = {
    attachments: 'attachmentsPanel',
    firstSlotDepth: 'firstSlotDepthBorder',
    formInfo: 'formInfoBorder',
    intermediateDetails: 'detailsRegion',
    signature: 'signatureBorder',
  };
  return idMap[sectionKey] || sectionKey;
}

function createPresetRenderSectionNode(sectionKey: string, tree: RuntimeRenderNode): RuntimeRenderNode {
  if (sectionKey === 'formInfo') {
    return withRenderNodeMetadata({
      children: [
        {
          children: [
            createBoundFieldNode(tree, 'productionDateField', '生产日期', 'productionDate', 'Text'),
            createBoundFieldNode(tree, 'modelCodeField', '型号', 'modelCode', 'Text'),
            createBoundFieldNode(tree, 'materialCodeField', '料号', 'materialCode', 'Text'),
            createBoundFieldNode(tree, 'batchNoField', '批号', 'batchNo', 'Text'),
          ],
          id: 'formInfoGrid',
          props: { columns: 3, gap: 0 },
          type: 'Grid',
        },
      ],
      id: 'formInfoBorder',
      props: { title: '表单信息' },
      type: 'Border',
    });
  }
  if (sectionKey === 'firstSlotDepth') {
    return withRenderNodeMetadata({
      children: [
        {
          children: [
            createBoundFieldNode(tree, 'firstSlotDepthMinField', 'XY最小值', 'firstSlotDepthMin', 'Input'),
            createBoundFieldNode(tree, 'firstSlotDepthMaxField', 'XY最大值', 'firstSlotDepthMax', 'Input'),
            createBoundFieldNode(tree, 'firstSlotDepthAvgField', 'XY平均值', 'firstSlotDepthAvg', 'Input'),
          ],
          id: 'firstSlotDepthGrid',
          props: { columns: 3, gap: 0 },
          type: 'Grid',
        },
      ],
      id: 'firstSlotDepthBorder',
      props: { title: '首件槽深/mm' },
      type: 'Border',
    });
  }
  if (sectionKey === 'attachments') {
    return withRenderNodeMetadata({
      children: [
        {
          binding: { path: 'attachments', source: 'header', type: 'array' },
          component: 'AttachmentList',
          id: 'attachmentList',
          props: { title: '原始导入附件' },
          type: 'AttachmentList',
        },
      ],
      id: 'attachmentsPanel',
      props: { title: '原始导入附件' },
      type: 'Panel',
    });
  }
  if (sectionKey === 'intermediateDetails') {
    return withRenderNodeMetadata({
      children: [
        {
          binding: { path: 'previewDetails', source: 'header', type: 'array' },
          component: 'EditableTable',
          id: 'intermediateDetailsTable',
          props: { title: '中间品记录明细' },
          type: 'EditableTable',
        },
      ],
      id: 'detailsRegion',
      props: { title: '中间品记录明细' },
      type: 'TableRegion',
    });
  }
  if (sectionKey === 'signature') {
    return withRenderNodeMetadata({
      children: [
        {
          binding: { path: 'signature', source: 'header', type: 'object' },
          component: 'SignatureGrid',
          id: 'signatureGrid',
          props: { title: '签名区' },
          type: 'SignatureGrid',
        },
      ],
      id: 'signatureBorder',
      props: { title: '签名区' },
      type: 'Border',
    });
  }
  return withRenderNodeMetadata({
    children: [],
    id: createUniqueRenderNodeId(tree, sectionKey || 'section'),
    props: { title: '自定义分区' },
    type: 'Border',
  });
}

function createBoundFieldNode(tree: RuntimeRenderNode, id: string, label: string, path: string, widget: string): RuntimeRenderNode {
  return withRenderNodeMetadata({
    binding: { path, source: 'header', type: 'value' },
    component: widget,
    id: createUniqueRenderNodeId(tree, id),
    props: { label },
    type: 'Field',
  });
}

function withRenderNodeMetadata(node: RuntimeRenderNode): RuntimeRenderNode {
  const registry = runtimeComponentRegistry[node.type] || {
    componentName: node.componentName || node.component || node.type,
    nodeKind: node.children || node.slots?.default ? 'layout' : 'component',
  };
  node.componentName = node.componentName || registry.componentName;
  node.nodeKind = node.nodeKind || registry.nodeKind;
  const children = resolveRenderNodeChildren(node).map((child) => withRenderNodeMetadata(child));
  if (children.length || node.nodeKind === 'layout') {
    assignRenderNodeChildren(node, children);
  }
  return node;
}

function addRuntimeSection(tool: PaletteTool) {
  updateSchema((schema) => {
    const layout = ensureRuntimeLayout(schema);
    const sections = layout.sections;
    const fixedKey = tool.sectionKey || tool.key;
    const existing = !tool.unique
      ? sections.find((section: RuntimeSection) => section.key === fixedKey || section.type === tool.sectionType)
      : undefined;
    if (existing) {
      selectedNode.value = { key: existing.key || fixedKey, sectionKey: existing.key || fixedKey, type: 'section' };
      message.info(`${existing.title || tool.title || fixedKey} 已存在，已为你选中`);
      return;
    }
    const sectionKey = tool.unique ? createUniqueSectionKey(sections, fixedKey) : fixedKey;
    const section = createRuntimeSectionTemplate(tool, sectionKey);
    sections.push(section);
    selectedNode.value = { key: sectionKey, sectionKey, type: 'section' };
  });
}

function addRuntimeFieldFromTool(tool: PaletteTool) {
  let insertedFieldKey = '';
  let targetSectionKey = resolveFieldTargetSectionKey();
  updateSchema((schema) => {
    const section = ensureRuntimeSection(schema, targetSectionKey, {
      icon: 'lucide:table-properties',
      key: targetSectionKey,
      kind: 'section',
      sectionKey: targetSectionKey,
      sectionType: targetSectionKey === 'signature' ? 'signatureGrid' : targetSectionKey === 'firstSlotDepth' ? 'matrix' : 'headerGrid',
      title: targetSectionKey === 'signature' ? '签名区' : targetSectionKey === 'firstSlotDepth' ? '首件槽深/mm' : '表单信息',
    });
    targetSectionKey = section.key || targetSectionKey;
    const fields = ensureSchemaArray(section, 'fields');
    const fieldKey = createUniqueFieldKey(fields, tool.defaultField || tool.fieldPrefix || 'field');
    insertedFieldKey = fieldKey;
    fields.push({
      component: tool.component || 'Input',
      field: fieldKey,
      label: tool.defaultLabel || '新字段',
      storage: `presetHeaderDataJson.${fieldKey}`,
    });
    selectedNode.value = { index: fields.length - 1, key: `${targetSectionKey}.${fieldKey}`, sectionKey: targetSectionKey, type: 'field' };
  });
  if (insertedFieldKey) {
    updateHeaderData((headerData) => {
      headerData[insertedFieldKey] = headerData[insertedFieldKey] ?? '';
    });
  }
}

function addRuntimeField(sectionKey: string) {
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, sectionKey);
    if (!section) return;
    const fields = ensureSchemaArray(section, 'fields');
    const fieldKey = `${sectionKey}Field${fields.length + 1}`;
    fields.push({
      component: 'Input',
      field: fieldKey,
      label: '新字段',
      storage: `presetHeaderDataJson.${fieldKey}`,
    });
    selectedNode.value = { index: fields.length - 1, key: `${sectionKey}.${fieldKey}`, sectionKey, type: 'field' };
  });
}

function addRuntimeColumn(insertIndex = detailColumns.value.length, tool?: PaletteTool) {
  let insertedFieldKey = '';
  updateSchema((schema) => {
    const section = ensureRuntimeSection(schema, 'intermediateDetails', {
      icon: 'lucide:table-2',
      key: 'intermediateDetails',
      kind: 'section',
      sectionKey: 'intermediateDetails',
      sectionType: 'editableTable',
      title: '中间品记录明细',
    });
    const columns = ensureSchemaArray(section, 'columns');
    const targetIndex = Math.max(0, Math.min(insertIndex, columns.length));
    const fieldKey =
      tool?.fieldPrefix === 'thickness'
        ? createNextThicknessFieldKey(columns)
        : createUniqueFieldKey(columns, tool?.defaultField || tool?.fieldPrefix || 'customColumn');
    insertedFieldKey = fieldKey;
    const label =
      tool?.fieldPrefix === 'thickness'
        ? `${resolveThicknessColumnIndex(fieldKey) * Number(props.schema.thicknessIntervalCm || 100)}cm${tool.defaultLabel || '厚度/mm'}`
        : tool?.defaultLabel || '新列';
    columns.splice(targetIndex, 0, {
      component: tool?.component || 'Input',
      field: fieldKey,
      label,
      width: tool?.width || 160,
    });
    selectedNode.value = { index: targetIndex, key: `intermediateDetails.${fieldKey}`, sectionKey: 'intermediateDetails', type: 'column' };
  });
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'previewDetails');
    rows.forEach((row) => {
      row[insertedFieldKey] = row[insertedFieldKey] ?? '';
    });
  });
}

function removeRuntimeColumn() {
  const index = selectedNode.value.index;
  if (index === undefined) return;
  let removedField = '';
  updateSchema((schema) => {
    const section = findSectionInSchema(schema, 'intermediateDetails');
    const columns = ensureArray(section?.columns);
    removedField = columns[index]?.field || columns[index]?.key || '';
    columns.splice(index, 1);
    if (section) section.columns = columns;
  });
  if (removedField) {
    updateHeaderData((headerData) => {
      ensureHeaderArray(headerData, 'previewDetails').forEach((row) => delete row[removedField]);
    });
  }
  selectedNode.value = { key: 'intermediateDetails', sectionKey: 'intermediateDetails', type: 'section' };
}

function insertRuntimeRow(index = detailRows.value.length) {
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'previewDetails');
    const targetIndex = Math.max(0, Math.min(index, rows.length));
    rows.splice(targetIndex, 0, createDetailRow(targetIndex));
    normalizeDetailSeq(rows);
    selectedNode.value = { index: targetIndex, key: `previewDetails.${targetIndex + 1}`, sectionKey: 'intermediateDetails', type: 'row' };
  });
}

function removeRuntimeRow() {
  const index = selectedNode.value.index;
  if (index === undefined) return;
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'previewDetails');
    rows.splice(index, 1);
    normalizeDetailSeq(rows);
  });
  selectedNode.value = { key: 'intermediateDetails', sectionKey: 'intermediateDetails', type: 'section' };
}

function addAttachment() {
  updateHeaderData((headerData) => {
    const rows = ensureHeaderArray(headerData, 'attachments');
    rows.push({
      name: `原始导入附件${rows.length + 1}.xlsx`,
      size: 0,
      uploadTime: '',
    });
    selectedNode.value = { index: rows.length - 1, key: `attachments.${rows.length}`, sectionKey: 'attachments', type: 'attachment' };
  });
}

function removeAttachment() {
  const index = selectedNode.value.index;
  if (index === undefined) return;
  updateHeaderData((headerData) => {
    ensureHeaderArray(headerData, 'attachments').splice(index, 1);
  });
  selectedNode.value = { key: 'attachments', sectionKey: 'attachments', type: 'section' };
}

function applySchemaJson() {
  try {
    const parsed = JSON.parse(schemaJsonText.value);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('组件配置必须是 JSON 对象');
    emit('update:schema', parsed);
    message.success('组件配置 JSON 已应用');
  } catch (error: any) {
    message.error(error?.message || '组件配置 JSON 格式不正确');
  }
}

function applyHeaderJson() {
  try {
    const parsed = JSON.parse(headerJsonText.value);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('预览数据必须是 JSON 对象');
    emit('update:headerData', parsed);
    message.success('预览数据 JSON 已应用');
  } catch (error: any) {
    message.error(error?.message || '预览数据 JSON 格式不正确');
  }
}
function createModelMatchShell() {
  return {
    fallback: 'COMMON',
    priority: ['MODEL', 'PREFIX', 'COMMON'],
    source: '$route.modelCode',
  };
}

function getModelScope(schema: Record<string, any> | undefined) {
  const configuredScope = String(schema?.modelScope || '').trim().toUpperCase();
  if (['COMMON', 'MODEL', 'PREFIX'].includes(configuredScope)) return configuredScope;
  if (schema?.modelPrefix) return 'PREFIX';
  return schema?.modelCode && schema.modelCode !== 'COMMON' ? 'MODEL' : 'COMMON';
}

function getModelScopeValue(schema: Record<string, any> | undefined) {
  return getModelScope(schema) === 'PREFIX'
    ? String(schema?.modelPrefix || schema?.modelCode || '')
    : String(schema?.modelCode || 'COMMON');
}

function updateModelScope(value: string) {
  updateSchema((schema) => {
    const scope = ['COMMON', 'MODEL', 'PREFIX'].includes(String(value || '').toUpperCase())
      ? String(value).toUpperCase()
      : 'COMMON';
    schema.modelScope = scope;
    if (scope === 'COMMON') {
      schema.modelCode = 'COMMON';
      schema.modelName = '通用';
      delete schema.modelPrefix;
    } else if (scope === 'PREFIX') {
      const prefix = String(schema.modelPrefix || schema.modelCode || '').trim();
      if (prefix && prefix !== 'COMMON') {
        schema.modelPrefix = prefix;
      } else {
        delete schema.modelPrefix;
      }
      delete schema.modelCode;
      if (schema.modelName === '通用') schema.modelName = '';
    } else {
      const modelCode = String(schema.modelCode || schema.modelPrefix || '').trim();
      if (modelCode && modelCode !== 'COMMON') {
        schema.modelCode = modelCode;
      } else {
        delete schema.modelCode;
      }
      delete schema.modelPrefix;
      if (schema.modelName === '通用') schema.modelName = '';
    }
    if (!schema.modelMatch || typeof schema.modelMatch !== 'object' || Array.isArray(schema.modelMatch)) {
      schema.modelMatch = createModelMatchShell();
    }
  });
}

function updateModelScopeValue(value: string) {
  updateSchema((schema) => {
    const scope = getModelScope(schema);
    const matchValue = String(value || '').trim();
    if (scope === 'PREFIX') {
      if (matchValue) {
        schema.modelPrefix = matchValue;
      } else {
        delete schema.modelPrefix;
      }
      delete schema.modelCode;
      return;
    }
    if (scope === 'COMMON') {
      schema.modelCode = 'COMMON';
      delete schema.modelPrefix;
      return;
    }
    if (matchValue) {
      schema.modelCode = matchValue;
    } else {
      delete schema.modelCode;
    }
    delete schema.modelPrefix;
  });
}

function updateModelMatchProp(prop: string, value: any) {
  updateSchema((schema) => {
    if (!schema.modelMatch || typeof schema.modelMatch !== 'object' || Array.isArray(schema.modelMatch)) {
      schema.modelMatch = createModelMatchShell();
    }
    if (value === undefined || value === '') {
      delete schema.modelMatch[prop];
    } else {
      schema.modelMatch[prop] = value;
    }
  });
}

function updateModelMatchPriority(value: string) {
  updateModelMatchProp(
    'priority',
    String(value || '')
      .split(/[,，;；/、|\s]+/)
      .map((item) => item.trim().toUpperCase())
      .filter(Boolean),
  );
}

function formatModelMatchPriority(priority: any) {
  return Array.isArray(priority) ? priority.join(',') : String(priority || 'MODEL,PREFIX,COMMON');
}
function createBusinessBindingShell() {
  return {
    context: {
      batchNo: '$route.batchNo',
      modelCode: '$route.modelCode',
      planId: '$route.planId',
      planOperationId: '$route.planOperationId',
      recordDate: '$route.recordDate',
    },
    load: {
      mode: 'getOrInit',
      params: {},
      provider: '',
    },
    mappings: {},
    save: {
      mode: 'upsert',
      payload: {},
      provider: '',
    },
  };
}

function updateBusinessBinding(updater: (binding: Record<string, any>) => void) {
  updateSchema((schema) => {
    if (!schema.businessBinding || typeof schema.businessBinding !== 'object' || Array.isArray(schema.businessBinding)) {
      schema.businessBinding = createBusinessBindingShell();
    }
    updater(schema.businessBinding);
  });
}

function updateBusinessBindingProp(prop: string, value: any) {
  updateBusinessBinding((binding) => {
    binding[prop] = value;
  });
}

function updateBusinessBindingLoadProp(prop: string, value: any) {
  updateBusinessBinding((binding) => {
    binding.load = {
      ...(binding.load || {}),
      [prop]: value,
    };
    if (prop === 'provider') {
      binding.save = {
        ...(binding.save || {}),
        provider: value,
      };
    }
  });
}

function updateBusinessBindingSaveProp(prop: string, value: any) {
  updateBusinessBinding((binding) => {
    binding.save = {
      ...(binding.save || {}),
      [prop]: value,
    };
  });
}

function updateBusinessBindingContext(prop: string, value: any) {
  updateBusinessBinding((binding) => {
    binding.context = {
      ...(binding.context || {}),
      [prop]: value,
    };
  });
}

function applyBusinessBindingJson() {
  try {
    const parsed = JSON.parse(businessBindingJsonText.value);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('业务绑定必须是 JSON 对象');
    updateSchema((schema) => {
      schema.businessBinding = parsed;
    });
    message.success('业务绑定 JSON 已应用');
  } catch (error: any) {
    message.error(error?.message || '业务绑定 JSON 格式不正确');
  }
}

function clearBusinessBinding() {
  updateSchema((schema) => {
    delete schema.businessBinding;
  });
  businessBindingJsonText.value = '{}';
  message.success('业务绑定已清空');
}

function applyPressSlotIntermediateBusinessBindingTemplate() {
  updateSchema((schema) => {
    schema.businessBinding = createPressSlotIntermediateBusinessBindingTemplate();
    schema.modelMatch = schema.modelMatch || createModelMatchShell();
    schema.modelScope = schema.modelScope || 'COMMON';
    schema.modelCode = schema.modelCode || 'COMMON';
    schema.modelName = schema.modelName || '通用';
  });
  message.success('已套用压槽中间品读写绑定模板');
}

function createPressSlotIntermediateBusinessBindingTemplate() {
  return {
    description: '压槽中间品记录：从压槽报工/中间品业务表读取，新建时按前段/中段/后段初始化明细，保存时回写主表和明细表。',
    context: {
      batchNo: '$route.batchNo',
      modelCode: '$route.modelCode',
      planId: '$route.planId',
      planOperationId: '$route.planOperationId',
      recordDate: '$route.recordDate',
    },
    load: {
      mode: 'getOrInit',
      params: {
        batchNo: '$context.batchNo',
        modelCode: '$context.modelCode',
        planId: '$context.planId',
        planOperationId: '$context.planOperationId',
        recordDate: '$context.recordDate',
      },
      provider: 'PRESS_SLOT_INTERMEDIATE',
    },
    mappings: {
      details: {
        fields: {
          pressSlotReportId: '$row.pressSlotReportId',
          samplePosition: '$row.samplePosition',
          samplePositionName: '$row.samplePositionName',
          sliceBatchNo: '$row.sliceBatchNo',
          sortNo: '$row.sortNo',
          thickness1: '$row.thickness1',
          thickness10: '$row.thickness10',
          thickness2: '$row.thickness2',
          thickness3: '$row.thickness3',
          thickness4: '$row.thickness4',
          thickness5: '$row.thickness5',
          thickness6: '$row.thickness6',
          thickness7: '$row.thickness7',
          thickness8: '$row.thickness8',
          thickness9: '$row.thickness9',
          widthMm: '$row.widthMm',
        },
        source: '$source.details',
        target: 'previewDetails',
      },
      header: {
        batchNo: '$source.batchNo',
        confirmerName: '$source.confirmerName',
        confirmTime: '$source.confirmTime',
        endSliceNo: '$source.endSliceNo',
        firstSlotDepthAvg: '$source.firstSlotDepthAvg',
        firstSlotDepthMax: '$source.firstSlotDepthMax',
        firstSlotDepthMin: '$source.firstSlotDepthMin',
        frontSliceNo: '$source.frontSliceNo',
        id: '$source.id',
        inputQty: '$source.inputQty',
        materialCode: '$source.materialCode',
        middleSliceNo: '$source.middleSliceNo',
        modelCode: '$source.modelCode',
        outputQty: '$source.outputQty',
        planId: '$source.planId',
        planNo: '$source.planNo',
        planOperationId: '$source.planOperationId',
        productionDate: '$source.recordDate',
        recorderName: '$source.recorderName',
        recordStatus: '$source.recordStatus',
        recordTime: '$source.fillTime',
      },
    },
    initRules: [
      {
        rows: [
          { samplePosition: 'FRONT', samplePositionName: '前段' },
          { samplePosition: 'MIDDLE', samplePositionName: '中段' },
          { samplePosition: 'END', samplePositionName: '后段' },
        ],
        target: 'previewDetails',
        type: 'seedRows',
        when: 'recordNotExists',
      },
      {
        assign: {
          pressSlotReportId: '$row.id',
          sliceBatchNo: '$row.productionBatchNo',
          widthMm: '$row.widthMm',
        },
        key: 'samplePosition',
        source: '$source.middleReports',
        sourceKey: 'reportType',
        target: 'previewDetails',
        type: 'mergeRowsByKey',
      },
      {
        from: 'previewDetails',
        key: 'samplePosition',
        target: 'header',
        type: 'copyDetailToHeader',
        valueMap: {
          END: { from: 'sliceBatchNo', to: 'endSliceNo' },
          FRONT: { from: 'sliceBatchNo', to: 'frontSliceNo' },
          MIDDLE: { from: 'sliceBatchNo', to: 'middleSliceNo' },
        },
      },
    ],
    save: {
      mode: 'upsert',
      payload: {
        batchNo: '$header.batchNo',
        confirmerName: '$header.confirmerName',
        details: '$details',
        endSliceNo: "$details[?samplePosition=='END'].sliceBatchNo",
        firstSlotDepthAvg: '$header.firstSlotDepthAvg',
        firstSlotDepthMax: '$header.firstSlotDepthMax',
        firstSlotDepthMin: '$header.firstSlotDepthMin',
        frontSliceNo: "$details[?samplePosition=='FRONT'].sliceBatchNo",
        id: '$header.id',
        materialCode: '$header.materialCode',
        middleSliceNo: "$details[?samplePosition=='MIDDLE'].sliceBatchNo",
        modelCode: '$header.modelCode',
        planId: '$context.planId',
        planNo: '$header.planNo',
        planOperationId: '$context.planOperationId',
        recordDate: '$header.productionDate',
        recorderName: '$header.recorderName',
        recordStatus: '$header.recordStatus',
      },
      provider: 'PRESS_SLOT_INTERMEDIATE',
    },
  };
}

function fieldValue(field: Record<string, any>) {
  const value = props.headerData?.[field.field];
  return value === undefined || value === null || value === '' ? '-' : value;
}

function columnKey(column: Record<string, any>, index: number) {
  return String(column.field || column.key || `column${index + 1}`);
}

function columnLabel(column: Record<string, any>, index: number) {
  const label = String(column.label || column.title || columnKey(column, index));
  if (columnKey(column, index).startsWith('thickness') && thicknessStandard.value !== '-' && !label.includes(thicknessStandard.value)) {
    return `${label}(${thicknessStandard.value})`;
  }
  return label;
}

function cloneRecord(value: Record<string, any> | undefined) {
  return JSON.parse(JSON.stringify(value || {}));
}

function ensureArray(value: any): Array<Record<string, any>> {
  return Array.isArray(value) ? value : [];
}

function ensureRuntimeLayout(schema: Record<string, any>) {
  if (!schema.runtimeLayout || typeof schema.runtimeLayout !== 'object') {
    schema.runtimeLayout = {};
  }
  if (!Array.isArray(schema.runtimeLayout.sections)) {
    schema.runtimeLayout.sections = [];
  }
  return schema.runtimeLayout;
}

function ensureRenderTree(schema: Record<string, any>) {
  const layout = ensureRuntimeLayout(schema);
  layout.componentModel = layout.componentModel || 'VbenVueJsonComponentTree';
  if (!layout.renderTree || typeof layout.renderTree !== 'object') {
    layout.renderTree = createDefaultRenderTree();
  }
  const tree = layout.renderTree as RuntimeRenderNode;
  tree.nodeKind = tree.nodeKind || 'layout';
  tree.componentName = tree.componentName || resolveRenderNodeComponentName(tree);
  const children = resolveRenderNodeChildren(tree);
  assignRenderNodeChildren(tree, children);
  return tree;
}

function createDefaultRenderTree(): RuntimeRenderNode {
  return {
    children: [],
    componentName: 'RuntimePage',
    id: 'runtimePage',
    nodeKind: 'layout',
    props: {
      title: props.schema?.displayName || props.formName || '压槽中间品记录表',
    },
    type: 'Page',
  };
}

function createRenderTreeNodeTemplate(tool: PaletteTool, id: string): RuntimeRenderNode {
  const nodeType = tool.nodeType || tool.key;
  const registry = runtimeComponentRegistry[nodeType] || {
    componentName: tool.component || nodeType,
    nodeKind: tool.kind === 'container' ? 'layout' : 'component',
  };
  const isContainer = registry.nodeKind === 'layout';
  const props = {
    ...(tool.props || {}),
    label: tool.defaultLabel,
    title: tool.title || tool.defaultLabel || tool.props?.title,
  };
  const node: RuntimeRenderNode = {
    componentName: registry.componentName,
    id,
    nodeKind: registry.nodeKind,
    props,
    type: nodeType,
  };
  if (!isContainer) {
    node.component = tool.component || (nodeType === 'Field' ? 'Input' : nodeType);
  }
  if (isContainer) {
    node.children = [];
    node.slots = { default: node.children };
  } else {
    const fieldKey = tool.defaultField || createUniqueRenderBindingPath(renderTree.value, tool.fieldPrefix || 'field');
    node.binding =
      nodeType === 'EditableTable'
        ? { path: 'previewDetails', source: 'header', type: 'array' }
        : nodeType === 'AttachmentList'
          ? { path: 'attachments', source: 'header', type: 'array' }
          : nodeType === 'SignatureGrid'
            ? { path: 'signature', source: 'header', type: 'object' }
            : { path: fieldKey, source: 'header', type: 'value' };
    if (nodeType === 'Field') {
      node.props = {
        ...node.props,
        label: tool.defaultLabel || '字段',
      };
    }
  }
  return node;
}

function resolveRenderTreeInsertParent(
  schema: Record<string, any>,
  tree: RuntimeRenderNode,
  toolOrKind: PaletteTool | PaletteTool['kind'],
) {
  const toolKind = typeof toolOrKind === 'string' ? toolOrKind : toolOrKind.kind;
  const selected = selectedNode.value.nodeId ? findRenderTreeNode(tree, selectedNode.value.nodeId) : undefined;
  const sectionParent = resolveSelectedSectionRenderParent(schema, tree, toolKind);
  if (sectionParent) return sectionParent;
  if (toolKind === 'container') {
    if (selected?.node && isRenderContainer(selected.node)) return selected.node;
    if (selected?.parent && isRenderContainer(selected.parent)) return selected.parent;
    return tree;
  }
  if (selected?.node && isRenderContainer(selected.node)) return selected.node;
  if (selected?.parent && isRenderContainer(selected.parent)) return selected.parent;
  return tree;
}

function resolveSelectedSectionRenderParent(
  schema: Record<string, any>,
  tree: RuntimeRenderNode,
  toolKind: PaletteTool['kind'],
) {
  const sectionKey = resolveSelectedSectionKeyForInsert();
  if (!sectionKey) return undefined;

  const preferredId = resolveSectionRenderInsertTargetId(sectionKey, toolKind);
  const preferred = preferredId ? findRenderTreeNode(tree, preferredId)?.node : undefined;
  if (preferred && isRenderContainer(preferred)) return preferred;

  const rootId = resolvePresetRenderSectionRootId(sectionKey);
  const existingRoot = findRenderTreeNode(tree, rootId)?.node;
  if (existingRoot && isRenderContainer(existingRoot)) return existingRoot;

  const section = findSectionInSchema(schema, sectionKey);
  if (!section) return undefined;

  const sectionNode = createPresetRenderSectionNode(sectionKey, tree);
  const children = resolveRenderNodeChildren(tree);
  children.push(sectionNode);
  assignRenderNodeChildren(tree, children);

  const createdPreferred = preferredId ? findRenderTreeNode(sectionNode, preferredId)?.node : undefined;
  if (createdPreferred && isRenderContainer(createdPreferred)) return createdPreferred;
  return isRenderContainer(sectionNode) ? sectionNode : undefined;
}

function resolveSelectedSectionKeyForInsert() {
  if (selectedNode.value.sectionKey) return selectedNode.value.sectionKey;
  if (selectedNode.value.type === 'section') return selectedNode.value.key;
  return '';
}

function resolveSectionRenderInsertTargetId(sectionKey: string, toolKind: PaletteTool['kind']) {
  if (toolKind === 'container') return resolvePresetRenderSectionRootId(sectionKey);
  const idMap: Record<string, string> = {
    attachments: 'attachmentsPanel',
    firstSlotDepth: 'firstSlotDepthGrid',
    formInfo: 'formInfoGrid',
    intermediateDetails: 'detailsRegion',
    signature: 'signatureBorder',
  };
  return idMap[sectionKey] || resolvePresetRenderSectionRootId(sectionKey);
}

function resolveRenderNodeKind(node?: RuntimeRenderNode) {
  if (!node) return 'component';
  return node.nodeKind || runtimeComponentRegistry[node.type]?.nodeKind || 'component';
}

function resolveRenderNodeComponentName(node?: RuntimeRenderNode) {
  if (!node) return '';
  return node.componentName || runtimeComponentRegistry[node.type]?.componentName || node.component || node.type;
}

function resolveRenderNodeChildren(node?: RuntimeRenderNode) {
  if (!node) return [];
  return ensureArray(node.slots?.default || node.children) as RuntimeRenderNode[];
}

function assignRenderNodeChildren(node: RuntimeRenderNode, children: RuntimeRenderNode[]) {
  node.children = children;
  node.slots = {
    ...(node.slots || {}),
    default: children,
  };
}

function isRenderContainer(node?: RuntimeRenderNode) {
  return resolveRenderNodeKind(node) === 'layout';
}

function normalizeRenderNodeSize(value: any) {
  if (value === undefined || value === null || value === '') return undefined;
  if (typeof value === 'number') return `${value}px`;
  const text = String(value).trim();
  if (!text) return undefined;
  return /^\d+(\.\d+)?$/u.test(text) ? `${text}px` : text;
}

function resolveRenderNodeStyle(node: RuntimeRenderNode, baseStyle: Record<string, any> = {}) {
  const width = normalizeRenderNodeSize(node.props?.width);
  const height = normalizeRenderNodeSize(node.props?.height);
  return {
    ...baseStyle,
    ...(width ? { width } : {}),
    ...(height ? { height } : {}),
  };
}

function resolveRenderNodeClasses(node: RuntimeRenderNode, classList: any[]) {
  const className = String(node.props?.className || '').trim();
  return className ? [...classList, className] : classList;
}

function flattenRenderTree(root: RuntimeRenderNode) {
  const rows: Array<{ level: number; node: RuntimeRenderNode; parent?: RuntimeRenderNode }> = [];
  const visit = (node: RuntimeRenderNode, level: number, parent?: RuntimeRenderNode) => {
    rows.push({ level, node, parent });
    resolveRenderNodeChildren(node).forEach((child) => visit(child, level + 1, node));
  };
  visit(root, 0);
  return rows;
}

function findRenderTreeNode(root: RuntimeRenderNode, nodeId: string): { node: RuntimeRenderNode; parent?: RuntimeRenderNode } | undefined {
  if (root.id === nodeId) return { node: root };
  for (const child of resolveRenderNodeChildren(root)) {
    const found = findRenderTreeNode(child, nodeId);
    if (found) {
      return found.parent ? found : { ...found, parent: root };
    }
  }
  return undefined;
}

function createUniqueRenderNodeId(root: RuntimeRenderNode, baseId: string) {
  const ids = new Set(flattenRenderTree(root).map((item) => item.node.id));
  if (!ids.has(baseId)) return baseId;
  let index = 2;
  while (ids.has(`${baseId}${index}`)) {
    index += 1;
  }
  return `${baseId}${index}`;
}

function createUniqueRenderBindingPath(root: RuntimeRenderNode, basePath: string) {
  const bindings = new Set(
    flattenRenderTree(root)
      .map((item) => item.node.binding?.path)
      .filter(Boolean),
  );
  if (!bindings.has(basePath)) return basePath;
  let index = 2;
  while (bindings.has(`${basePath}${index}`)) {
    index += 1;
  }
  return `${basePath}${index}`;
}

function createUniqueNameWithSet(usedNames: Set<string>, baseName: string) {
  const normalizedBase = String(baseName || 'item').replace(/[^a-zA-Z0-9_]/g, '') || 'item';
  if (!usedNames.has(normalizedBase)) {
    usedNames.add(normalizedBase);
    return normalizedBase;
  }
  let index = 2;
  while (usedNames.has(`${normalizedBase}${index}`)) {
    index += 1;
  }
  const value = `${normalizedBase}${index}`;
  usedNames.add(value);
  return value;
}

function removeRenderTreeNodeById(root: RuntimeRenderNode, nodeId: string) {
  const found = findRenderTreeNode(root, nodeId);
  if (!found?.parent) return false;
  const children = resolveRenderNodeChildren(found.parent).filter((child) => child.id !== nodeId);
  assignRenderNodeChildren(found.parent, children);
  return true;
}

function cleanupSectionHeaderData(headerData: Record<string, any>, section: RuntimeSection) {
  ensureArray(section.fields).forEach((field) => {
    if (field.field) delete headerData[field.field];
  });
  if (section.type === 'attachmentList') {
    delete headerData.attachments;
  }
  if (section.type === 'editableTable') {
    delete headerData.previewDetails;
  }
}

function ensureRuntimeSection(schema: Record<string, any>, sectionKey: string, tool: PaletteTool) {
  const layout = ensureRuntimeLayout(schema);
  let section = layout.sections.find((item: RuntimeSection) => item.key === sectionKey || item.type === tool.sectionType);
  if (!section) {
    section = createRuntimeSectionTemplate(tool, sectionKey);
    layout.sections.push(section);
  } else {
    hydrateRuntimeSectionDefaults(section, sectionKey, tool.sectionType);
  }
  return section;
}

function syncRuntimeSectionFromInsertedNode(
  schema: Record<string, any>,
  node: RuntimeRenderNode,
  parent: RuntimeRenderNode,
) {
  const sectionKey = resolveRuntimeSectionKeyForInsertedNode(node, parent);
  if (node.type === 'Field') {
    if (sectionKey === 'intermediateDetails') {
      syncRuntimeDetailColumnFromRenderNode(schema, node);
      return;
    }
    syncRuntimeFieldFromRenderNode(schema, node, resolveFieldSectionKey(sectionKey));
    return;
  }
  if (node.type === 'EditableTable') {
    ensureRuntimeSectionByKey(schema, 'intermediateDetails', 'editableTable');
    return;
  }
  if (node.type === 'AttachmentList') {
    ensureRuntimeSectionByKey(schema, 'attachments', 'attachmentList');
    return;
  }
  if (node.type === 'SignatureGrid') {
    ensureRuntimeSectionByKey(schema, 'signature', 'signatureGrid');
    return;
  }
  resolveRenderNodeChildren(node).forEach((child) => {
    syncRuntimeSectionFromInsertedNode(schema, child, node);
  });
}

function syncRuntimeFieldFromRenderNode(
  schema: Record<string, any>,
  node: RuntimeRenderNode,
  sectionKey: string,
) {
  const fieldKey = String(node.binding?.path || node.props?.field || node.id || '').trim();
  if (!fieldKey) return;
  const sectionType = resolveRuntimeSectionType(sectionKey);
  const section = ensureRuntimeSectionByKey(schema, sectionKey, sectionType);
  const fields = ensureSchemaArray(section, 'fields');
  const nextField = {
    component: node.component || node.props?.component || 'Input',
    editable: node.props?.editable,
    field: fieldKey,
    label: node.props?.label || node.props?.title || fieldKey,
    storage: `presetHeaderDataJson.${fieldKey}`,
    width: node.props?.width,
  };
  const existingIndex = fields.findIndex((field) => field.field === fieldKey);
  if (existingIndex >= 0) {
    fields[existingIndex] = { ...fields[existingIndex], ...nextField };
  } else {
    fields.push(nextField);
  }
}

function syncRuntimeDetailColumnFromRenderNode(schema: Record<string, any>, node: RuntimeRenderNode) {
  const fieldKey = String(node.binding?.path || node.props?.field || node.id || '').trim();
  if (!fieldKey) return;
  const section = ensureRuntimeSectionByKey(schema, 'intermediateDetails', 'editableTable');
  const columns = ensureSchemaArray(section, 'columns');
  const nextColumn = {
    component: node.component || node.props?.component || 'Input',
    field: fieldKey,
    label: node.props?.label || node.props?.title || fieldKey,
    width: node.props?.width || 160,
  };
  const existingIndex = columns.findIndex((column) => column.field === fieldKey);
  if (existingIndex >= 0) {
    columns[existingIndex] = { ...columns[existingIndex], ...nextColumn };
  } else {
    columns.push(nextColumn);
  }
}

function ensureRuntimeSectionByKey(
  schema: Record<string, any>,
  sectionKey: string,
  sectionType = resolveRuntimeSectionType(sectionKey),
) {
  const layout = ensureRuntimeLayout(schema);
  let section = layout.sections.find((item: RuntimeSection) => item.key === sectionKey);
  const tool = createRuntimeSectionTool(schema, sectionKey, sectionType);
  if (!section) {
    section = createRuntimeSectionTemplate(tool, sectionKey);
    layout.sections.push(section);
  } else {
    hydrateRuntimeSectionDefaults(section, sectionKey, sectionType);
  }
  return section;
}

function createRuntimeSectionTool(
  schema: Record<string, any>,
  sectionKey: string,
  sectionType = resolveRuntimeSectionType(sectionKey),
): PaletteTool {
  const preset = layoutTools.find((tool) => tool.sectionKey === sectionKey || tool.key === sectionKey);
  const section = findSectionInSchema(schema, sectionKey);
  return {
    icon: preset?.icon || 'lucide:table-properties',
    key: sectionKey,
    kind: 'section',
    sectionKey,
    sectionType: section?.type || preset?.sectionType || sectionType,
    title: section?.title || preset?.title || resolveRuntimeSectionTitle(sectionKey),
  };
}

function resolveRuntimeSectionKeyForInsertedNode(node: RuntimeRenderNode, parent: RuntimeRenderNode) {
  const selectedSectionKey = resolveSelectedSectionKeyForInsert();
  if (selectedSectionKey) return selectedSectionKey;
  const parentSectionKey = resolveRuntimeSectionKeyFromRenderNode(parent);
  if (parentSectionKey) return parentSectionKey;
  if (node.type === 'EditableTable') return 'intermediateDetails';
  if (node.type === 'AttachmentList') return 'attachments';
  if (node.type === 'SignatureGrid') return 'signature';
  return 'formInfo';
}

function resolveRuntimeSectionKeyFromRenderNode(node?: RuntimeRenderNode) {
  if (!node) return '';
  const marker = `${node.id || ''} ${node.props?.title || ''} ${node.props?.label || ''}`.toLowerCase();
  if (node.type === 'TableRegion' || marker.includes('detail') || marker.includes('明细')) return 'intermediateDetails';
  if (node.type === 'Panel' && (marker.includes('attachment') || marker.includes('附件'))) return 'attachments';
  if (marker.includes('signature') || marker.includes('签名')) return 'signature';
  if (marker.includes('firstslotdepth') || marker.includes('槽深')) return 'firstSlotDepth';
  if (marker.includes('forminfo') || marker.includes('表单信息')) return 'formInfo';
  return '';
}

function resolveFieldSectionKey(sectionKey: string) {
  return ['firstSlotDepth', 'formInfo', 'signature'].includes(sectionKey) ? sectionKey : 'formInfo';
}

function resolveRuntimeSectionType(sectionKey: string) {
  const typeMap: Record<string, string> = {
    attachments: 'attachmentList',
    firstSlotDepth: 'matrix',
    formInfo: 'headerGrid',
    intermediateDetails: 'editableTable',
    signature: 'signatureGrid',
  };
  return typeMap[sectionKey] || 'headerGrid';
}

function resolveRuntimeSectionTitle(sectionKey: string) {
  const titleMap: Record<string, string> = {
    attachments: '原始导入附件',
    firstSlotDepth: '首件槽深/mm',
    formInfo: '表单信息',
    intermediateDetails: '明细项目',
    signature: '签名区',
  };
  return titleMap[sectionKey] || '自定义分区';
}

function createRuntimeSectionTemplate(tool: PaletteTool, sectionKey: string): RuntimeSection {
  const sectionType = tool.sectionType || 'headerGrid';
  const section: RuntimeSection = {
    key: sectionKey,
    title: tool.title || '新分区',
    type: sectionType,
  };
  if (sectionType === 'headerGrid') {
    section.columns = 3;
    section.fields = [];
  } else if (sectionType === 'matrix' || sectionType === 'signatureGrid') {
    section.fields = [];
  } else if (sectionType === 'attachmentList') {
    section.storage = 'presetHeaderDataJson.attachments';
  } else if (sectionType === 'editableTable') {
    section.columns = [];
    section.rowKey = 'samplePositionName';
    section.storage = 'presetHeaderDataJson.previewDetails';
  }
  hydrateRuntimeSectionDefaults(section, sectionKey, sectionType);
  return section;
}

function hydrateRuntimeSectionDefaults(section: RuntimeSection, sectionKey: string, sectionType?: string) {
  const type = sectionType || section.type || 'headerGrid';
  if (sectionKey === 'formInfo' && type === 'headerGrid' && !ensureArray(section.fields).length) {
    section.columns = section.columns || 3;
    section.fields = [
      createRuntimeFieldTemplate('productionDate', '生产日期', 'Text'),
      createRuntimeFieldTemplate('modelCode', '型号', 'Text'),
      createRuntimeFieldTemplate('materialCode', '料号', 'Text'),
      createRuntimeFieldTemplate('batchNo', '批号', 'Text'),
    ];
  }
  if (sectionKey === 'firstSlotDepth' && type === 'matrix' && !ensureArray(section.fields).length) {
    section.fields = [
      createRuntimeFieldTemplate('firstSlotDepthMin', 'XY最小值', 'Input'),
      createRuntimeFieldTemplate('firstSlotDepthMax', 'XY最大值', 'Input'),
      createRuntimeFieldTemplate('firstSlotDepthAvg', 'XY平均值', 'Input'),
    ];
  }
  if (sectionKey === 'signature' && type === 'signatureGrid' && !ensureArray(section.fields).length) {
    section.fields = [
      createRuntimeFieldTemplate('recorder', '记录人', 'Text'),
      createRuntimeFieldTemplate('recorderTime', '记录时间', 'Text'),
      createRuntimeFieldTemplate('confirmer', '确认人', 'Text'),
      createRuntimeFieldTemplate('confirmerTime', '确认时间', 'Text'),
    ];
  }
  if (sectionKey === 'intermediateDetails' && type === 'editableTable' && !ensureArray(section.columns).length) {
    section.columns = [
      { component: 'Text', field: 'itemName', label: '项目名称', width: 220 },
      { component: 'Text', field: 'standardText', label: '标准', width: 260 },
      { component: 'Input', field: 'actualValue', label: '实际/记录', width: 180 },
      { component: 'Select', field: 'resultFlag', label: '结果', width: 120 },
      { component: 'Input', field: 'remark', label: '备注', width: 180 },
    ];
  }
}

function createRuntimeFieldTemplate(field: string, label: string, component: string) {
  return {
    component,
    field,
    label,
    storage: `presetHeaderDataJson.${field}`,
  };
}

function resolveFieldTargetSectionKey() {
  const sectionKey = selectedNode.value.sectionKey || selectedNode.value.key;
  const section = findSection(sectionKey);
  if (section && ['headerGrid', 'matrix', 'signatureGrid'].includes(section.type || '')) {
    return section.key || sectionKey;
  }
  if (selectedNode.value.type === 'field' && selectedNode.value.sectionKey) {
    return selectedNode.value.sectionKey;
  }
  return 'formInfo';
}

function findSectionInSchema(schema: Record<string, any>, sectionKey: string) {
  const sections = ensureRuntimeLayout(schema).sections;
  return sections.find((section: RuntimeSection) => section.key === sectionKey || section.type === sectionKey);
}

function ensureSchemaArray(target: Record<string, any> | undefined, key: string) {
  if (!target) return [];
  if (!Array.isArray(target[key])) {
    target[key] = [];
  }
  return target[key];
}

function ensureHeaderArray(headerData: Record<string, any>, key: string) {
  if (!Array.isArray(headerData[key])) {
    headerData[key] = [];
  }
  return headerData[key];
}

function createUniqueSectionKey(sections: RuntimeSection[], baseKey: string) {
  const keys = new Set(sections.map((section) => section.key).filter(Boolean));
  if (!keys.has(baseKey)) return baseKey;
  let index = 2;
  while (keys.has(`${baseKey}${index}`)) {
    index += 1;
  }
  return `${baseKey}${index}`;
}

function createUniqueFieldKey(items: Array<Record<string, any>>, preferredKey: string) {
  const normalizedPreferred = preferredKey || 'field';
  const existing = new Set(items.map((item) => item.field || item.key).filter(Boolean));
  if (!existing.has(normalizedPreferred)) return normalizedPreferred;
  let index = 2;
  while (existing.has(`${normalizedPreferred}${index}`)) {
    index += 1;
  }
  return `${normalizedPreferred}${index}`;
}

function createNextThicknessFieldKey(columns: Array<Record<string, any>>) {
  const maxIndex = columns.reduce((max, column) => {
    const match = String(column.field || column.key || '').match(/^thickness(\d+)$/u);
    return match?.[1] ? Math.max(max, Number(match[1])) : max;
  }, 0);
  return `thickness${maxIndex + 1}`;
}

function resolveThicknessColumnIndex(fieldKey: string) {
  const match = fieldKey.match(/^thickness(\d+)$/u);
  return match?.[1] ? Number(match[1]) : 1;
}

function createDetailRow(index: number) {
  const row: Record<string, any> = {
    samplePositionName: `采样段${index + 1}`,
    seq: index + 1,
  };
  detailColumns.value.forEach((column, columnIndex) => {
    const key = columnKey(column, columnIndex);
    if (!(key in row)) row[key] = key === 'samplePositionName' ? row.samplePositionName : '-';
  });
  return row;
}

function normalizeDetailSeq(rows: Array<Record<string, any>>) {
  rows.forEach((row, index) => {
    row.seq = index + 1;
  });
}

function extractStandard(text?: unknown) {
  const value = String(text ?? '').trim();
  const match = value.match(/[（(]\s*([^）)]+?)\s*[）)]/u);
  return match?.[1]?.trim() || value;
}

function resolveRenderNodeValue(node: RuntimeRenderNode) {
  const binding = node.binding;
  if (!binding || binding.type !== 'value') return '-';
  if (binding.source === 'header') {
    const value = props.headerData?.[binding.path];
    return value === undefined || value === null || value === '' ? '-' : value;
  }
  return '-';
}

function resolveRenderNodeTitle(node: RuntimeRenderNode) {
  return node.props?.title || node.props?.label || node.id;
}

function isDepthRenderNode(node?: RuntimeRenderNode) {
  const text = `${node?.id || ''}${node?.props?.title || ''}`.toLowerCase();
  return text.includes('depth') || text.includes('槽深');
}

function renderDesignerIcon(node: RuntimeRenderNode) {
  return isRenderContainer(node) ? 'lucide:box' : 'lucide:component';
}

const RuntimeRenderCanvas = defineComponent({
  name: 'RuntimeRenderCanvas',
  props: {
    node: {
      required: true,
      type: Object as PropType<RuntimeRenderNode>,
    },
    parentId: {
      default: '',
      type: String,
    },
    parentType: {
      default: '',
      type: String,
    },
  },
  setup(componentProps) {
    const selectCurrentNode = (event: MouseEvent) => {
      event.stopPropagation();
      selectNode(renderCanvasNodeObjectNode(componentProps.node));
    };

    const selectableClass = () => [
      'runtime-selectable',
      isNodeActive(renderCanvasNodeObjectNode(componentProps.node)) ? 'runtime-selectable--active' : '',
    ];

    const renderChildren = () =>
      resolveRenderCanvasChildren(componentProps.node).map((child) =>
        h(RuntimeRenderCanvas, {
          key: child.id,
          node: child,
          parentId: componentProps.node.id,
          parentType: componentProps.node.type,
        }),
      );

    const renderDesignerDropHint = (node: RuntimeRenderNode) =>
      h(
        'div',
        {
          class: 'runtime-render-drop-hint',
        },
        [
          h(IconifyIcon, { icon: 'lucide:mouse-pointer-click' }),
          h('span', `选中 ${resolveRenderNodeTitle(node)} 后，从左侧组件库添加子组件`),
        ],
      );

    const renderContainerChildren = (node: RuntimeRenderNode, children: any[]) =>
      children.length ? children : [renderDesignerDropHint(node)];

    const renderToolbarField = () =>
      h(
        'button',
          {
          class: resolveRenderNodeClasses(componentProps.node, ['runtime-toolbar-field', ...selectableClass()]),
          style: resolveRenderNodeStyle(componentProps.node),
          type: 'button',
          onClick: selectCurrentNode,
        },
        [
          h('span', resolveRenderNodeTitle(componentProps.node)),
          h('strong', String(resolveRenderNodeValue(componentProps.node))),
        ],
      );

    const renderGridField = () => {
      const isDepth = componentProps.parentId.toLowerCase().includes('depth');
      const runtimeField = resolveRenderNodeRuntimeField(componentProps.node);
      const selectFieldNode = (event: MouseEvent) => {
        if (!runtimeField) {
          selectCurrentNode(event);
          return;
        }
        event.stopPropagation();
        selectNode(fieldNode(runtimeField.section, runtimeField.field, runtimeField.index));
      };
      return h(
        'button',
          {
          class: resolveRenderNodeClasses(componentProps.node, [isDepth ? 'runtime-depth-cell' : 'runtime-header-cell', ...selectableClass()]),
          style: resolveRenderNodeStyle(componentProps.node),
          type: 'button',
          onClick: selectFieldNode,
        },
        [
          h('span', resolveRenderNodeTitle(componentProps.node)),
          h('strong', String(resolveRenderNodeValue(componentProps.node))),
        ],
      );
    };

    const renderAttachmentList = () =>
      h(
        'div',
        {
          class: resolveRenderNodeClasses(componentProps.node, ['runtime-attachment-list', ...selectableClass()]),
          style: resolveRenderNodeStyle(componentProps.node),
          onClick: selectCurrentNode,
        },
        attachments.value.length
          ? attachments.value.map((attachment, index) =>
              h(
                'button',
                {
                  key: attachment.uid || attachment.name || index,
                  class: 'runtime-attachment-item',
                  type: 'button',
                  onClick: (event: MouseEvent) => {
                    event.stopPropagation();
                    selectNode(attachmentNode(attachment, index));
                  },
                },
                [
                  h(IconifyIcon, { icon: 'lucide:paperclip' }),
                  h('span', attachment.name || '原始导入附件'),
                  attachment.uploadTime ? h('small', `导入时间：${attachment.uploadTime}`) : null,
                ],
              ),
            )
          : [h('span', { class: 'runtime-empty-text' }, '暂无附件配置')],
      );

    const renderEditableTable = () =>
      h(
        'div',
        {
          class: resolveRenderNodeClasses(componentProps.node, ['runtime-table-wrap', ...selectableClass()]),
          style: resolveRenderNodeStyle(componentProps.node),
          onClick: selectCurrentNode,
        },
        [
          h('table', { class: 'runtime-grid' }, [
            h(
              'thead',
              [
                h(
                  'tr',
                  detailColumns.value.map((column, index) =>
                    h(
                      'th',
                      {
                        key: column.field || column.key || index,
                        class: [
                          'runtime-selectable',
                          isNodeActive(columnNode(column, index)) ? 'runtime-selectable--active' : '',
                        ],
                        onClick: (event: MouseEvent) => {
                          event.stopPropagation();
                          selectNode(columnNode(column, index));
                        },
                      },
                      columnLabel(column, index),
                    ),
                  ),
                ),
              ],
            ),
            h(
              'tbody',
              detailRows.value.map((row, rowIndex) =>
                h(
                  'tr',
                  {
                    key: row.seq || rowIndex,
                    class: [
                      'runtime-selectable',
                      isNodeActive(rowNode(row, rowIndex)) ? 'runtime-selectable--active' : '',
                    ],
                    onClick: (event: MouseEvent) => {
                      event.stopPropagation();
                      selectNode(rowNode(row, rowIndex));
                    },
                  },
                  detailColumns.value.map((column, columnIndex) =>
                    h('td', { key: column.field || column.key || columnIndex }, row[columnKey(column, columnIndex)] || '-'),
                  ),
                ),
              ),
            ),
          ]),
        ],
      );

    const renderSignatureGrid = () =>
      h(
        'div',
        {
          class: resolveRenderNodeClasses(componentProps.node, ['runtime-signature-row', ...selectableClass()]),
          style: resolveRenderNodeStyle(componentProps.node),
          onClick: selectCurrentNode,
        },
        signatureFields.value.map((field, index) =>
          h(
            'button',
            {
              key: field.field || index,
              class: [
                'runtime-signature-cell',
                'runtime-selectable',
                isNodeActive(fieldNode(signatureSection.value || {}, field, index)) ? 'runtime-selectable--active' : '',
              ],
              type: 'button',
              onClick: (event: MouseEvent) => {
                event.stopPropagation();
                selectNode(fieldNode(signatureSection.value || {}, field, index));
              },
            },
            [h('span', field.label || field.field), h('strong', fieldValue(field))],
          ),
        ),
      );

    return () => {
      const node = componentProps.node;
      if (node.type === 'Page') {
        return h(
          'div',
          {
            class: resolveRenderNodeClasses(node, ['runtime-wysiwyg-page', ...selectableClass()]),
            style: resolveRenderNodeStyle(node),
            onClick: selectCurrentNode,
          },
          renderChildren(),
        );
      }
      if (node.type === 'Toolbar') {
        return h(
          'div',
          {
            class: resolveRenderNodeClasses(node, ['runtime-sheet-toolbar', ...selectableClass()]),
            style: resolveRenderNodeStyle(node),
            onClick: selectCurrentNode,
          },
          [
            h('strong', runtimeLayout.value.title || props.schema.displayName || '压槽中间品记录表'),
            ...renderChildren(),
            h('span', { class: 'runtime-node-kind' }, node.type),
          ],
        );
      }
      if (node.type === 'Border') {
        const children = renderChildren();
        return h(
          'fieldset',
          {
            class: resolveRenderNodeClasses(node, ['runtime-fieldset', 'runtime-layout-outline', 'runtime-layout-outline--border', ...selectableClass()]),
            style: resolveRenderNodeStyle(node),
            onClick: selectCurrentNode,
          },
          [h('legend', resolveRenderNodeTitle(node)), ...renderContainerChildren(node, children)],
        );
      }
      if (node.type === 'Grid') {
        const columns = node.props?.columns || 3;
        const isDepth = isDepthRenderNode(node) || componentProps.parentId.toLowerCase().includes('depth');
        const children = renderChildren();
        return h(
          'div',
          {
            class: resolveRenderNodeClasses(node, [
              'runtime-layout-outline',
              'runtime-layout-outline--grid',
              isDepth ? 'runtime-depth-grid' : 'runtime-header-grid',
              ...selectableClass(),
            ]),
            style: resolveRenderNodeStyle(node, {
              gridTemplateColumns:
                typeof columns === 'number' || /^\d+$/u.test(String(columns))
                  ? `repeat(${Number(columns)}, minmax(0, 1fr))`
                  : String(columns),
            }),
            onClick: selectCurrentNode,
          },
          [h('div', { class: 'runtime-layout-outline__badge' }, `Grid ${columns}`), ...renderContainerChildren(node, children)],
        );
      }
      if (node.type === 'Panel' || node.type === 'TableRegion') {
        const children = renderChildren();
        return h(
          'section',
          {
            class: resolveRenderNodeClasses(node, [
              'runtime-layout-outline',
              node.type === 'TableRegion' ? 'runtime-table-panel' : 'runtime-attachment-panel',
              node.type === 'TableRegion' ? 'runtime-layout-outline--table-region' : 'runtime-layout-outline--panel',
              ...selectableClass(),
            ]),
            style: resolveRenderNodeStyle(node),
            onClick: selectCurrentNode,
          },
          [
            h('div', { class: 'runtime-panel-title' }, [
              h('span', resolveRenderNodeTitle(node)),
              h('small', node.type),
            ]),
            ...renderContainerChildren(node, children),
          ],
        );
      }
      if (node.type === 'Split') {
        const children = renderChildren();
        return h(
          'div',
          {
            class: resolveRenderNodeClasses(node, ['runtime-layout-outline', 'runtime-layout-outline--split', 'runtime-render-split', ...selectableClass()]),
            style: resolveRenderNodeStyle(node, { gridTemplateColumns: node.props?.columns || '1fr 1fr' }),
            onClick: selectCurrentNode,
          },
          [h('div', { class: 'runtime-layout-outline__badge' }, `Split ${node.props?.columns || '1fr 1fr'}`), ...renderContainerChildren(node, children)],
        );
      }
      if (node.type === 'Field') {
        return componentProps.parentType === 'Toolbar' ? renderToolbarField() : renderGridField();
      }
      if (node.type === 'AttachmentList') {
        return renderAttachmentList();
      }
      if (node.type === 'EditableTable') {
        return renderEditableTable();
      }
      if (node.type === 'SignatureGrid') {
        return renderSignatureGrid();
      }
      return h(
        'div',
        {
          class: resolveRenderNodeClasses(node, ['runtime-render-unknown', ...selectableClass()]),
          style: resolveRenderNodeStyle(node),
          onClick: selectCurrentNode,
        },
        [h(IconifyIcon, { icon: renderDesignerIcon(node) }), h('span', resolveRenderNodeTitle(node)), ...renderChildren()],
      );
    };
  },
});
</script>

<template>
  <div class="press-slot-runtime-ide">
    <aside class="runtime-ide-tree">
      <Tabs v-model:activeKey="leftPanelTab" size="small" class="runtime-side-tabs">
        <Tabs.TabPane key="palette" tab="组件库">
          <div class="runtime-toolbox">
            <div class="runtime-toolbox-group">
              <span>布局容器</span>
              <button
                v-for="tool in containerTools"
                :key="tool.key"
                class="runtime-tool"
                type="button"
                @click="handlePaletteTool(tool)"
              >
                <IconifyIcon :icon="tool.icon" />
                <strong>{{ isProductionCheck && tool.key === 'intermediateDetails' ? '生产点检明细' : tool.title }}</strong>
              </button>
            </div>
            <div class="runtime-toolbox-group">
              <span>业务控件</span>
              <button
                v-for="tool in controlTools"
                :key="tool.key"
                class="runtime-tool"
                type="button"
                @click="handlePaletteTool(tool)"
              >
                <IconifyIcon :icon="tool.icon" />
                <strong>{{ isProductionCheck && tool.key === 'intermediateDetails' ? '生产点检明细' : tool.title }}</strong>
              </button>
            </div>
            <div class="runtime-toolbox-group">
              <span>预置区块</span>
              <button
                v-for="tool in (isProductionCheck ? layoutTools.filter((item) => ['formInfo', 'intermediateDetails'].includes(item.key)) : layoutTools)"
                :key="tool.key"
                class="runtime-tool"
                type="button"
                @click="handlePaletteTool(tool)"
              >
                <IconifyIcon :icon="tool.icon" />
                <strong>{{ isProductionCheck && tool.key === 'intermediateDetails' ? '生产点检明细' : tool.title }}</strong>
              </button>
            </div>
            <div class="runtime-toolbox-group">
              <span>表格数据</span>
              <button
                v-for="tool in tableTools"
                :key="tool.key"
                class="runtime-tool"
                type="button"
                @click="handlePaletteTool(tool)"
              >
                <IconifyIcon :icon="tool.icon" />
                <strong>{{ tool.title || tool.defaultLabel }}</strong>
              </button>
            </div>
          </div>
        </Tabs.TabPane>

        <Tabs.TabPane key="tree" tab="对象树">
          <button
            class="runtime-node runtime-node--root"
            :class="{ 'runtime-node--active': isNodeActive({ key: 'runtime-form', type: 'root' }) }"
            type="button"
            @click="selectNode({ key: 'runtime-form', type: 'root' })"
          >
            <IconifyIcon icon="lucide:panel-top" />
            <span>运行时表单</span>
          </button>

          <div class="runtime-tree-group">
            <span class="runtime-tree-caption">组件树</span>
            <button
              v-for="item in flatRenderTreeNodes"
              :key="item.node.id"
              class="runtime-node"
              :class="{
                'runtime-node--active': isNodeActive(renderTreeNodeObjectNode(item.node)),
                'runtime-node--child': item.level > 0,
              }"
              :style="{ paddingLeft: `${8 + item.level * 18}px` }"
              type="button"
              @click="selectNode(renderTreeNodeObjectNode(item.node))"
            >
              <IconifyIcon :icon="isRenderContainer(item.node) ? 'lucide:box' : 'lucide:component'" />
              <span>{{ item.node.props?.title || item.node.props?.label || item.node.id }}</span>
            </button>
          </div>

          <div v-for="section in runtimeSections" :key="section.key || section.type" class="runtime-tree-group">
            <button
              class="runtime-node"
              :class="{ 'runtime-node--active': isNodeActive(sectionNode(section)) }"
              type="button"
              @click="selectNode(sectionNode(section))"
            >
              <IconifyIcon icon="lucide:layout-template" />
              <span>{{ section.title || section.key || section.type }}</span>
            </button>

            <template v-if="section.type === 'editableTable'">
              <span class="runtime-tree-caption">明细列</span>
              <button
                v-for="(column, index) in ensureArray(section.columns)"
                :key="column.field || column.key || index"
                class="runtime-node runtime-node--child"
                :class="{ 'runtime-node--active': isNodeActive(columnNode(column, index)) }"
                type="button"
                @click="selectNode(columnNode(column, index))"
              >
                <IconifyIcon icon="lucide:columns-3" />
                <span>{{ column.label || column.title || column.field || `列${index + 1}` }}</span>
              </button>
              <span class="runtime-tree-caption">预览行</span>
              <button
                v-for="(row, index) in detailRows"
                :key="row.seq || index"
                class="runtime-node runtime-node--child"
                :class="{ 'runtime-node--active': isNodeActive(rowNode(row, index)) }"
                type="button"
                @click="selectNode(rowNode(row, index))"
              >
                <IconifyIcon icon="lucide:rows-3" />
                <span>{{ row.samplePositionName || `数据行${index + 1}` }}</span>
              </button>
            </template>

            <template v-else-if="section.type === 'attachmentList'">
              <button
                v-for="(attachment, index) in attachments"
                :key="attachment.uid || attachment.name || index"
                class="runtime-node runtime-node--child"
                :class="{ 'runtime-node--active': isNodeActive(attachmentNode(attachment, index)) }"
                type="button"
                @click="selectNode(attachmentNode(attachment, index))"
              >
                <IconifyIcon icon="lucide:paperclip" />
                <span>{{ attachment.name || `附件${index + 1}` }}</span>
              </button>
            </template>

            <template v-else>
              <button
                v-for="(field, index) in ensureArray(section.fields)"
                :key="field.field || index"
                class="runtime-node runtime-node--child"
                :class="{ 'runtime-node--active': isNodeActive(fieldNode(section, field, index)) }"
                type="button"
                @click="selectNode(fieldNode(section, field, index))"
              >
                <IconifyIcon icon="lucide:text-cursor-input" />
                <span>{{ field.label || field.field || `字段${index + 1}` }}</span>
              </button>
            </template>
          </div>
        </Tabs.TabPane>
      </Tabs>
    </aside>
    <section class="runtime-ide-workspace">
      <div class="runtime-ide-toolbar">
        <div>
          <span>表单设计器</span>
          <strong>{{ runtimeLayout.title || schema.displayName || formName || '压槽中间品记录表' }}</strong>
        </div>
        <Tag color="blue">{{ isProductionCheck ? '生产点检布局' : 'DEV 组件树布局' }}</Tag>
      </div>

      <div class="runtime-design-surface runtime-design-surface--wysiwyg">
        <RuntimeRenderCanvas :node="renderTree" />
      </div>
    </section>
    <aside class="runtime-ide-inspector">
      <div class="runtime-ide-pane-title">属性面板</div>
      <div class="runtime-selected-object">
        <strong>{{ selectedNodeTitle }}</strong>
        <span>{{ selectedNodeKind }}</span>
      </div>
      <div class="runtime-selected-actions">
        <Button :disabled="!canCopySelectedElement" size="small" @click="copySelectedElement">
          <IconifyIcon icon="lucide:copy" class="mr-1" />
          复制
        </Button>
        <Button :disabled="!canPasteClipboardElement" size="small" @click="pasteClipboardElement">
          <IconifyIcon icon="lucide:clipboard-paste" class="mr-1" />
          粘贴
        </Button>
        <Button :disabled="!canDeleteSelectedElement" danger size="small" @click="deleteSelectedElement">
          <IconifyIcon icon="lucide:trash-2" class="mr-1" />
          删除
        </Button>
      </div>
      <div class="runtime-clipboard-tip">剪贴板：{{ clipboardLabel }}</div>

      <Tabs v-model:activeKey="inspectorTab" size="small" class="runtime-inspector-tabs">
        <Tabs.TabPane key="props" tab="属性">
          <div class="runtime-property-grid">
            <template v-if="selectedNode.type === 'root'">
              <label>表单标题</label>
              <Input :value="runtimeLayout.title || schema.displayName" @update:value="updateLayoutTitle" />
              <label>显示名称</label>
              <Input :value="schema.displayName" @update:value="(value) => updateRootProp('displayName', value)" />
              <label>版本</label>
              <Input :value="schema.version" @update:value="(value) => updateRootProp('version', value)" />
              <label>来源型号</label>
              <Input :value="schema.sourceModelCode" @update:value="(value) => updateRootProp('sourceModelCode', value)" />
              <label>厚度列数</label>
              <InputNumber
                :min="1"
                :precision="0"
                :value="schema.thicknessColumnCount"
                class="w-full"
                @update:value="(value) => updateRootProp('thicknessColumnCount', value)"
              />
              <label>厚度间隔/cm</label>
              <InputNumber
                :min="1"
                :precision="0"
                :value="schema.thicknessIntervalCm"
                class="w-full"
                @update:value="(value) => updateRootProp('thicknessIntervalCm', value)"
              />
              <label>预览占位值</label>
              <Input :value="schema.previewPlaceholder" @update:value="(value) => updateRootProp('previewPlaceholder', value)" />
              <label>滚动策略</label>
              <Select
                :options="layoutScrollModeOptions"
                :value="runtimeLayout.scrollMode || ''"
                @update:value="(value) => updateRuntimeLayoutProp('scrollMode', value)"
              />
              <label>明细最小高度</label>
              <Input
                placeholder="如：240px"
                :value="runtimeLayout.detailMinHeight"
                @update:value="(value) => updateRuntimeLayoutProp('detailMinHeight', value)"
              />
              <label>原始Excel填明细</label>
              <Switch
                :checked="excelImportConfig.enabled === true || excelImportConfig.fillDetailFromSource === true"
                @update:checked="updateExcelImportEnabled"
              />
              <label>导入起始行</label>
              <InputNumber
                :min="1"
                :precision="0"
                :value="excelImportDetailConfig.startRow"
                class="w-full"
                placeholder="如：7"
                @update:value="(value) => updateExcelImportDetailProp('startRow', value)"
              />
              <label>跳过表头行数</label>
              <InputNumber
                :min="0"
                :precision="0"
                :value="excelImportDetailConfig.skipHeaderRows"
                class="w-full"
                placeholder="如：2"
                @update:value="(value) => updateExcelImportDetailProp('skipHeaderRows', value)"
              />
              <label>最大导入行</label>
              <InputNumber
                :min="1"
                :precision="0"
                :value="excelImportDetailConfig.maxRows"
                class="w-full"
                placeholder="留空=导入所有有效行"
                @update:value="(value) => updateExcelImportDetailProp('maxRows', value)"
              />
              <label>停止前缀</label>
              <Input
                :value="formatExcelImportStopPrefixes()"
                placeholder="如：注：，制定/修订部门"
                @update:value="updateExcelImportStopPrefixes"
              />
              <div class="runtime-property-grid__wide">
                <div class="runtime-property-grid__title">
                  <strong>Excel列映射</strong>
                  <Button size="small" @click="resetExcelImportColumnsFromTable">按当前明细列生成</Button>
                </div>
                <Input.TextArea v-model:value="excelImportColumnsJsonText" :rows="5" />
                <Button block size="small" type="primary" @click="applyExcelImportColumnsJson">应用列映射</Button>
              </div>
              <label>仅开发实验</label>
              <Switch :checked="schema.devOnly === true" disabled />
            </template>

            <template v-else-if="selectedNode.type === 'layout' && selectedRenderTreeNode">
              <label>组件ID</label>
              <Input :value="selectedRenderTreeNode.id" readonly />
              <label>节点类型</label>
              <Input :value="selectedRenderTreeNode.type" readonly />
              <label>Vue组件</label>
              <Input :value="resolveRenderNodeComponentName(selectedRenderTreeNode)" readonly />
              <label>节点类别</label>
              <Input :value="resolveRenderNodeKind(selectedRenderTreeNode)" readonly />
              <label>标题</label>
              <Input
                :value="selectedRenderTreeNode.props?.title"
                @update:value="(value) => updateSelectedRenderNodeProp('title', value)"
              />
              <label>列数/栅格</label>
              <Input
                :value="selectedRenderTreeNode.props?.columns"
                placeholder="如：3 或 1fr 1fr"
                @update:value="(value) => updateSelectedRenderNodeProp('columns', value)"
              />
              <label>间距</label>
              <Input
                :value="selectedRenderTreeNode.props?.gap"
                placeholder="如：8"
                @update:value="(value) => updateSelectedRenderNodeProp('gap', value)"
              />
              <label>宽度</label>
              <Input
                :value="selectedRenderTreeNode.props?.width"
                placeholder="如：320px / 100%"
                @update:value="(value) => updateSelectedRenderNodeProp('width', value)"
              />
              <label>高度</label>
              <Input
                :value="selectedRenderTreeNode.props?.height"
                placeholder="如：160px / auto"
                @update:value="(value) => updateSelectedRenderNodeProp('height', value)"
              />
              <label>样式类</label>
              <Input
                :value="selectedRenderTreeNode.props?.className"
                @update:value="(value) => updateSelectedRenderNodeProp('className', value)"
              />
              <Button block size="small" type="dashed" @click="handlePaletteTool(containerTools[0])">
                <IconifyIcon icon="lucide:box" class="mr-1" />
                添加子容器
              </Button>
              <Button block size="small" type="dashed" @click="handlePaletteTool(controlTools[1])">
                <IconifyIcon icon="lucide:text-cursor-input" class="mr-1" />
                添加字段控件
              </Button>
            </template>

            <template v-else-if="selectedNode.type === 'control' && selectedRenderTreeNode">
              <label>组件ID</label>
              <Input :value="selectedRenderTreeNode.id" readonly />
              <label>节点类型</label>
              <Input :value="selectedRenderTreeNode.type" readonly />
              <label>Vue组件</label>
              <Input :value="resolveRenderNodeComponentName(selectedRenderTreeNode)" readonly />
              <label>节点类别</label>
              <Input :value="resolveRenderNodeKind(selectedRenderTreeNode)" readonly />
              <label>控件类型</label>
              <Select
                :options="componentOptions"
                :value="selectedRenderTreeNode.component"
                @update:value="(value) => updateSelectedRenderNodeProp('component', value)"
              />
              <label>显示标题</label>
              <Input
                :value="selectedRenderTreeNode.props?.label || selectedRenderTreeNode.props?.title"
                @update:value="(value) => updateSelectedRenderNodeProp(selectedRenderTreeNode.type === 'Field' ? 'label' : 'title', value)"
              />
              <label>宽度</label>
              <Input
                :value="selectedRenderTreeNode.props?.width"
                placeholder="如：180px / 100%"
                @update:value="(value) => updateSelectedRenderNodeProp('width', value)"
              />
              <label>高度</label>
              <Input
                :value="selectedRenderTreeNode.props?.height"
                placeholder="如：34px / auto"
                @update:value="(value) => updateSelectedRenderNodeProp('height', value)"
              />
              <label>数据源</label>
              <Input
                :value="selectedRenderTreeNode.binding?.source"
                placeholder="header / record / row"
                @update:value="(value) => updateSelectedRenderNodeBinding('source', value)"
              />
              <label>数据字段</label>
              <Input
                :value="selectedRenderTreeNode.binding?.path"
                placeholder="如：productionDate 或 previewDetails"
                @update:value="(value) => updateSelectedRenderNodeBinding('path', value)"
              />
              <label>绑定类型</label>
              <Select
                :options="[
                  { label: '单值', value: 'value' },
                  { label: '数组', value: 'array' },
                  { label: '对象', value: 'object' },
                ]"
                :value="selectedRenderTreeNode.binding?.type"
                @update:value="(value) => updateSelectedRenderNodeBinding('type', value)"
              />
              <label>可编辑</label>
              <Switch
                :checked="selectedRenderTreeNode.props?.editable !== false"
                @update:checked="(value) => updateSelectedRenderNodeProp('editable', value)"
              />
            </template>

            <template v-else-if="selectedNode.type === 'section'">
              <label>配置键</label>
              <Input :value="selectedSection?.key" readonly />
              <label>节点类型</label>
              <Input :value="selectedSection?.type" readonly />
              <label>标题</label>
              <Input :value="selectedSection?.title" @update:value="(value) => updateSectionProp('title', value)" />
              <template v-if="selectedSection?.type === 'headerGrid'">
                <label>列数/栅格</label>
                <InputNumber
                  :min="1"
                  :precision="0"
                  :value="selectedSection?.columns"
                  class="w-full"
                  @update:value="(value) => updateSectionProp('columns', value)"
                />
              </template>
              <Button
                v-if="selectedSection?.type !== 'editableTable' && selectedSection?.type !== 'attachmentList'"
                block
                size="small"
                type="dashed"
                @click="addRuntimeField(selectedNode.sectionKey || selectedNode.key)"
              >
                <IconifyIcon icon="lucide:plus" class="mr-1" />
                新增字段
              </Button>
              <Button v-if="selectedSection?.type === 'editableTable'" block size="small" type="dashed" @click="addRuntimeColumn()">
                <IconifyIcon icon="lucide:columns-3" class="mr-1" />
                新增明细列
              </Button>
              <Button v-if="selectedSection?.type === 'editableTable'" block size="small" type="dashed" @click="insertRuntimeRow()">
                <IconifyIcon icon="lucide:rows-3" class="mr-1" />
                新增预览行
              </Button>
              <Button v-if="selectedSection?.type === 'attachmentList'" block size="small" type="dashed" @click="addAttachment">
                <IconifyIcon icon="lucide:paperclip" class="mr-1" />
                新增附件示例
              </Button>
            </template>

            <template v-else-if="selectedNode.type === 'field' && selectedField">
              <label>字段名</label>
              <Input :value="selectedField.field" @update:value="(value) => updateSelectedFieldProp('field', value)" />
              <label>显示名</label>
              <Input :value="selectedField.label" @update:value="(value) => updateSelectedFieldProp('label', value)" />
              <label>控件类型</label>
              <Select
                :options="componentOptions"
                :value="selectedField.component"
                @update:value="(value) => updateSelectedFieldProp('component', value)"
              />
              <label>存储路径</label>
              <Input :value="selectedField.storage" @update:value="(value) => updateSelectedFieldProp('storage', value)" />
              <label>预览值</label>
              <Input :value="fieldValue(selectedField)" @update:value="updateSelectedFieldValue" />
            </template>

            <template v-else-if="selectedNode.type === 'column' && selectedColumn">
              <label>字段名</label>
              <Input :value="selectedColumn.field" @update:value="(value) => updateSelectedColumnProp('field', value)" />
              <label>显示名</label>
              <Input :value="selectedColumn.label" @update:value="(value) => updateSelectedColumnProp('label', value)" />
              <label>控件类型</label>
              <Select
                :options="componentOptions"
                :value="selectedColumn.component"
                @update:value="(value) => updateSelectedColumnProp('component', value)"
              />
              <label>宽度</label>
              <InputNumber
                :min="60"
                :precision="0"
                :value="selectedColumn.width"
                class="w-full"
                @update:value="(value) => updateSelectedColumnProp('width', value)"
              />
              <Button block size="small" @click="addRuntimeColumn(selectedNode.index)">向左插入列</Button>
              <Button block size="small" @click="addRuntimeColumn((selectedNode.index || 0) + 1)">向右插入列</Button>
              <Button block danger size="small" @click="removeRuntimeColumn">删除列</Button>
            </template>

            <template v-else-if="selectedNode.type === 'row' && selectedRow">
              <template v-for="(column, index) in detailColumns" :key="column.field || column.key || index">
                <label>{{ column.label || column.title || column.field || column.key }}</label>
                <Input
                  :value="selectedRow[columnKey(column, index)]"
                  @update:value="(value) => updateSelectedRowValue(columnKey(column, index), value)"
                />
              </template>
              <Button block size="small" @click="insertRuntimeRow(selectedNode.index)">向上插入行</Button>
              <Button block size="small" @click="insertRuntimeRow((selectedNode.index || 0) + 1)">向下插入行</Button>
              <Button block danger size="small" @click="removeRuntimeRow">删除行</Button>
            </template>

            <template v-else-if="selectedNode.type === 'attachment' && selectedAttachment">
              <label>文件名</label>
              <Input :value="selectedAttachment.name" @update:value="(value) => updateSelectedAttachmentValue('name', value)" />
              <label>导入时间</label>
              <Input :value="selectedAttachment.uploadTime" @update:value="(value) => updateSelectedAttachmentValue('uploadTime', value)" />
              <label>文件大小</label>
              <InputNumber
                :min="0"
                :precision="0"
                :value="selectedAttachment.size"
                class="w-full"
                @update:value="(value) => updateSelectedAttachmentValue('size', value)"
              />
              <Button block danger size="small" @click="removeAttachment">删除附件示例</Button>
            </template>
          </div>
        </Tabs.TabPane>

        <Tabs.TabPane key="business" tab="业务绑定">
          <div v-if="isProductionCheck" class="runtime-business-binding">
            <div class="runtime-binding-note">生产点检通过压槽送检记录加载和保存；每片对应一份检验记录，加检使用另送的片号。</div>
            <div class="runtime-match-summary"><strong>型号前缀匹配</strong><span>仅匹配已发布的型号前缀模板，优先选择最长前缀；未匹配时提示配置，不使用通用模板。</span></div>
            <div class="runtime-property-grid">
              <label>适用范围</label><Input value="型号前缀" disabled />
              <label>匹配前缀</label><Input :value="schema.modelPrefix" @update:value="updateModelScopeValue" />
              <label>型号显示名</label><Input :value="schema.modelName" @update:value="(value) => updateRootProp('modelName', value)" />
            </div>
          </div>
          <div v-else class="runtime-business-binding">
            <div class="runtime-binding-note">
              配置这个表单从哪个业务适配器读取数据、新建时如何初始化，以及保存时回写到哪里。字段控件仍通过绑定路径显示数据。
            </div>
            <div class="runtime-binding-actions">
              <Button size="small" type="primary" @click="applyPressSlotIntermediateBusinessBindingTemplate">
                套用压槽中间品模板
              </Button>
              <Button danger size="small" @click="clearBusinessBinding">清空绑定</Button>
            </div>
            <div class="runtime-match-summary">
              <strong>模板适用过滤</strong>
              <span>同一工序同一表单类型下，按指定型号优先，其次型号前缀，最后通用兜底。</span>
            </div>
            <div class="runtime-property-grid">
              <label>适用范围</label>
              <Select
                :options="modelScopeOptions"
                :value="getModelScope(schema)"
                @update:value="updateModelScope"
              />
              <label>匹配型号/前缀</label>
              <Input
                :value="getModelScopeValue(schema)"
                placeholder="COMMON / W33P0300 / W33，多个用逗号分隔"
                @update:value="updateModelScopeValue"
              />
              <label>型号显示名</label>
              <Input
                :value="schema.modelName || (getModelScope(schema) === 'COMMON' ? '通用' : getModelScopeValue(schema))"
                placeholder="例：W33P0300 专用"
                @update:value="(value) => updateRootProp('modelName', value)"
              />
              <label>当前型号来源</label>
              <Input
                :value="modelMatch.source || '$route.modelCode'"
                placeholder="$route.modelCode"
                @update:value="(value) => updateModelMatchProp('source', value)"
              />
              <label>未匹配兜底</label>
              <Input
                :value="modelMatch.fallback || 'COMMON'"
                placeholder="COMMON"
                @update:value="(value) => updateModelMatchProp('fallback', value)"
              />
              <label>匹配优先级</label>
              <Input
                :value="formatModelMatchPriority(modelMatch.priority)"
                placeholder="MODEL,PREFIX,COMMON"
                @update:value="updateModelMatchPriority"
              />
            </div>
            <div class="runtime-property-grid">
              <label>绑定说明</label>
              <Input.TextArea
                :rows="3"
                :value="businessBinding.description"
                placeholder="例：从压槽中间品业务表读取，保存时回写主表和明细表"
                @update:value="(value) => updateBusinessBindingProp('description', value)"
              />
              <label>读取适配器</label>
              <Select
                :options="businessProviderOptions"
                :value="businessBinding.load?.provider || ''"
                @update:value="(value) => updateBusinessBindingLoadProp('provider', value)"
              />
              <label>加载方式</label>
              <Select
                :options="businessLoadModeOptions"
                :value="businessBinding.load?.mode || 'getOrInit'"
                @update:value="(value) => updateBusinessBindingLoadProp('mode', value)"
              />
              <label>保存适配器</label>
              <Select
                :options="businessProviderOptions"
                :value="businessBinding.save?.provider || businessBinding.load?.provider || ''"
                @update:value="(value) => updateBusinessBindingSaveProp('provider', value)"
              />
              <label>保存方式</label>
              <Input
                :value="businessBinding.save?.mode || 'upsert'"
                placeholder="upsert / update / insert"
                @update:value="(value) => updateBusinessBindingSaveProp('mode', value)"
              />
              <label>当前型号来源</label>
              <Input
                :value="businessBinding.context?.modelCode"
                placeholder="$route.modelCode"
                @update:value="(value) => updateBusinessBindingContext('modelCode', value)"
              />
              <label>计划ID来源</label>
              <Input
                :value="businessBinding.context?.planId"
                placeholder="$route.planId"
                @update:value="(value) => updateBusinessBindingContext('planId', value)"
              />
              <label>工序ID来源</label>
              <Input
                :value="businessBinding.context?.planOperationId"
                placeholder="$route.planOperationId"
                @update:value="(value) => updateBusinessBindingContext('planOperationId', value)"
              />
              <label>记录日期来源</label>
              <Input
                :value="businessBinding.context?.recordDate"
                placeholder="$route.recordDate"
                @update:value="(value) => updateBusinessBindingContext('recordDate', value)"
              />
              <label>批号来源</label>
              <Input
                :value="businessBinding.context?.batchNo"
                placeholder="$route.batchNo"
                @update:value="(value) => updateBusinessBindingContext('batchNo', value)"
              />
            </div>
            <div class="runtime-advanced-json__header">
              <strong>业务绑定 JSON</strong>
              <Button size="small" type="primary" @click="applyBusinessBindingJson">应用</Button>
            </div>
            <Input.TextArea v-model:value="businessBindingJsonText" :rows="16" />
          </div>
        </Tabs.TabPane>

        <Tabs.TabPane key="advanced" tab="高级 JSON">
          <div class="runtime-advanced-json">
            <div class="runtime-advanced-json__header">
              <strong>组件配置 JSON</strong>
              <Button size="small" type="primary" @click="applySchemaJson">应用</Button>
            </div>
            <Input.TextArea v-model:value="schemaJsonText" :rows="12" />
            <div class="runtime-advanced-json__header">
              <strong>预览数据</strong>
              <Button size="small" type="primary" @click="applyHeaderJson">应用</Button>
            </div>
            <Input.TextArea v-model:value="headerJsonText" :rows="10" />
          </div>
        </Tabs.TabPane>
      </Tabs>
    </aside>
  </div>
</template>

<style>
.press-slot-runtime-ide {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr) 300px;
  height: calc(100vh - 172px);
  min-height: 620px;
  overflow: hidden;
  color: #172033;
  background: #d9e0e8;
  border-top: 1px solid #9aa7b6;
}

.runtime-ide-tree,
.runtime-ide-inspector {
  min-height: 0;
  overflow: auto;
  background: #eef2f6;
  border-right: 1px solid #9aa7b6;
}

.runtime-ide-inspector {
  border-right: 0;
  border-left: 1px solid #9aa7b6;
}
.runtime-side-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.runtime-side-tabs > .ant-tabs-nav {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 0 8px;
  margin: 0;
  background: linear-gradient(180deg, #4b5563 0%, #334155 100%);
  border-bottom: 1px solid #1f2937;
}

.runtime-side-tabs > .ant-tabs-nav .ant-tabs-tab {
  color: #dbe4ef;
  font-size: 12px;
  font-weight: 800;
}

.runtime-side-tabs > .ant-tabs-nav .ant-tabs-tab-active .ant-tabs-tab-btn {
  color: #ffffff;
}

.runtime-side-tabs > .ant-tabs-content-holder {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.runtime-side-tabs .ant-tabs-content,
.runtime-side-tabs .ant-tabs-tabpane {
  height: 100%;
}

.runtime-side-tabs .ant-tabs-tabpane {
  overflow: auto;
}

.runtime-ide-pane-title {
  display: flex;
  align-items: center;
  height: 30px;
  padding: 0 10px;
  color: #f8fafc;
  font-size: 12px;
  font-weight: 800;
  background: linear-gradient(180deg, #4b5563 0%, #334155 100%);
  border-bottom: 1px solid #1f2937;
}

.runtime-toolbox {
  padding: 8px;
  background: #e5ebf2;
  border-bottom: 1px solid #9aa7b6;
}

.runtime-toolbox-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 10px;
}

.runtime-toolbox-group:last-child {
  margin-bottom: 0;
}

.runtime-toolbox-group > span {
  grid-column: 1 / -1;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.runtime-tool {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 6px 4px;
  color: #1f2937;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #dbe4ef 100%);
  border: 1px solid #aab7c6;
}

.runtime-tool:hover {
  color: #0f172a;
  background: #d9ecff;
  border-color: #4096ff;
}

.runtime-tool strong {
  max-width: 100%;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-tool svg {
  font-size: 18px;
}

.runtime-tree-group {
  padding: 2px 0 4px;
}

.runtime-tree-caption {
  display: block;
  padding: 6px 10px 2px 28px;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.runtime-node {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
  min-height: 28px;
  padding: 4px 8px;
  overflow: hidden;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.runtime-node span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-node--root {
  font-weight: 800;
}

.runtime-node--child {
  padding-left: 26px;
}

.runtime-node:hover,
.runtime-node--active {
  color: #111827;
  background: #cfe3ff;
}

.runtime-ide-workspace {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(51, 65, 85, 0.08) 1px, transparent 1px) 0 0 / 24px 24px,
    linear-gradient(0deg, rgba(51, 65, 85, 0.08) 1px, transparent 1px) 0 0 / 24px 24px,
    #e7edf4;
}

.runtime-ide-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 6px 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #cfd8e3 100%);
  border-bottom: 1px solid #9aa7b6;
}

.runtime-ide-toolbar div {
  display: flex;
  gap: 10px;
  align-items: baseline;
  min-width: 0;
}

.runtime-ide-toolbar span {
  color: #475569;
  font-size: 12px;
}

.runtime-ide-toolbar strong {
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-design-surface {
  flex: 1;
  min-height: 0;
  padding: 10px;
  overflow: auto;
}

.runtime-design-surface--wysiwyg {
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.runtime-wysiwyg-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 1160px;
  min-height: 100%;
  padding: 0;
}

.runtime-render-designer {
  margin-bottom: 8px;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.runtime-render-root {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.runtime-render-container {
  min-height: 96px;
  padding: 8px;
  background:
    linear-gradient(90deg, rgba(51, 65, 85, 0.06) 1px, transparent 1px) 0 0 / 18px 18px,
    linear-gradient(0deg, rgba(51, 65, 85, 0.06) 1px, transparent 1px) 0 0 / 18px 18px,
    #ffffff;
  border: 1px solid #9aa7b6;
}

.runtime-layout-outline {
  position: relative;
  outline: 1px dashed rgba(37, 99, 235, 0.55);
  outline-offset: -3px;
}

.runtime-layout-outline::after {
  position: absolute;
  right: 6px;
  bottom: 4px;
  z-index: 1;
  padding: 1px 5px;
  color: #2563eb;
  font-size: 10px;
  font-weight: 800;
  line-height: 14px;
  pointer-events: none;
  content: attr(data-layout-kind);
  background: rgba(239, 246, 255, 0.92);
  border: 1px solid rgba(96, 165, 250, 0.55);
}

.runtime-layout-outline--border::after {
  content: 'Border';
}

.runtime-layout-outline--grid::after {
  content: none;
}

.runtime-layout-outline--split::after {
  content: none;
}

.runtime-layout-outline--panel::after {
  content: 'Panel';
}

.runtime-layout-outline--table-region::after {
  content: 'TableRegion';
}

.runtime-layout-outline__badge {
  position: absolute;
  top: 3px;
  right: 6px;
  z-index: 2;
  padding: 1px 6px;
  color: #0f4c81;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  pointer-events: none;
  background: rgba(219, 234, 254, 0.95);
  border: 1px solid rgba(96, 165, 250, 0.8);
}

.runtime-render-drop-hint {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
  background: rgba(239, 246, 255, 0.88);
  border: 1px dashed rgba(37, 99, 235, 0.7);
}

.runtime-header-grid > .runtime-render-drop-hint,
.runtime-depth-grid > .runtime-render-drop-hint,
.runtime-render-split > .runtime-render-drop-hint {
  grid-column: 1 / -1;
}

.runtime-render-container__title {
  display: flex;
  gap: 6px;
  align-items: center;
  min-height: 28px;
  padding: 0 8px;
  margin: -8px -8px 8px;
  background: linear-gradient(180deg, #edf2f7 0%, #d7dee7 100%);
  border-bottom: 1px solid #c6d0dc;
}

.runtime-render-container__title strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-render-container__title span {
  margin-left: auto;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.runtime-render-children {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: stretch;
  min-height: 48px;
}

.runtime-render-node {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  justify-content: center;
  min-width: 112px;
  min-height: 48px;
  padding: 6px 10px;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #c6d0dc;
}

.runtime-render-node span {
  max-width: 160px;
  overflow: hidden;
  color: #1f2937;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-render-node small {
  color: #64748b;
}

.runtime-render-empty {
  padding: 16px;
  color: #475569;
  font-weight: 700;
  text-align: center;
  background: #fff;
  border: 1px dashed #9aa7b6;
}

.runtime-sheet-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  align-items: center;
  min-height: 38px;
  padding: 8px 10px;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.runtime-sheet-toolbar strong {
  font-size: 16px;
}

.runtime-sheet-toolbar span {
  color: #334155;
}

.runtime-toolbar-field {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  min-height: 24px;
  padding: 0;
  color: #334155;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.runtime-toolbar-field span {
  color: #475569;
  font-weight: 500;
}

.runtime-toolbar-field strong {
  color: #172033;
  font-size: 13px;
}

.runtime-node-kind {
  margin-left: auto;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.runtime-fieldset {
  padding: 8px 10px 10px;
  margin: 8px 0 0;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.runtime-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 13px;
  font-weight: 800;
}

.runtime-header-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.runtime-header-cell,
.runtime-signature-cell {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  min-height: 34px;
  padding: 0;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 0;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.runtime-header-cell span,
.runtime-header-cell strong,
.runtime-signature-cell span,
.runtime-signature-cell strong {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.runtime-header-cell span,
.runtime-signature-cell span {
  justify-content: flex-end;
  font-weight: 800;
  background: #d7dee7;
  border-right: 1px solid #c6d0dc;
}

.runtime-header-cell strong,
.runtime-signature-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
}

.runtime-depth-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.runtime-depth-cell {
  display: grid;
  grid-template-rows: 32px 32px;
  padding: 0;
  text-align: center;
  cursor: pointer;
  background: #fff;
  border: 0;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.runtime-depth-cell span,
.runtime-depth-cell strong {
  display: flex;
  align-items: center;
  justify-content: center;
}

.runtime-depth-cell span {
  font-weight: 800;
  background: #d7dee7;
  border-bottom: 1px solid #c6d0dc;
}

.runtime-attachment-panel,
.runtime-table-panel {
  background: #fff;
  border: 1px solid #8794a4;
}

.runtime-panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  color: #075985;
  font-weight: 800;
  background: #d7dee7;
  border-bottom: 1px solid #8794a4;
}

.runtime-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 42px;
  padding: 8px 10px;
}

.runtime-attachment-item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  min-height: 28px;
  padding: 4px 8px;
  color: #1677ff;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.runtime-attachment-item small {
  color: #64748b;
}

.runtime-table-wrap {
  overflow: auto;
}

.runtime-grid {
  min-width: 1320px;
  width: 100%;
  border-collapse: collapse;
}

.runtime-grid th,
.runtime-grid td {
  min-width: 120px;
  height: 34px;
  padding: 5px 8px;
  white-space: nowrap;
  border: 1px solid #c6d0dc;
}

.runtime-grid th {
  font-weight: 800;
  text-align: center;
  cursor: pointer;
  background: #d7dee7;
}

.runtime-grid tbody tr {
  cursor: pointer;
  background: #f8fafc;
}

.runtime-grid tbody tr:hover {
  background: #edf6ff;
}

.runtime-signature-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.runtime-render-split {
  display: grid;
  gap: 8px;
}

.runtime-render-unknown {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  min-height: 32px;
  padding: 6px 8px;
  background: #fff;
  border: 1px dashed #9aa7b6;
}

.runtime-selectable {
  outline: 1px solid transparent;
  outline-offset: -1px;
}

.runtime-selectable:hover {
  outline-color: #4096ff;
}

.runtime-selectable--active {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
  box-shadow: inset 0 0 0 1px #ffffff;
}

.runtime-empty-text {
  display: inline-flex;
  align-items: center;
  color: #64748b;
}

.runtime-selected-object {
  display: grid;
  gap: 2px;
  padding: 8px 10px;
  background: #fff;
  border-bottom: 1px solid #d7dee7;
}

.runtime-selected-object strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-selected-object span {
  color: #64748b;
  font-size: 12px;
}

.runtime-selected-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 8px 10px 0;
  background: #fff;
}

.runtime-selected-actions :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.runtime-clipboard-tip {
  padding: 4px 10px 8px;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
  border-bottom: 1px solid #d7dee7;
}

.runtime-inspector-tabs {
  padding: 8px;
}

.runtime-property-grid {
  display: grid;
  grid-template-columns: 106px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.runtime-property-grid label {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  text-align: right;
}

.runtime-property-grid :deep(.ant-btn) {
  grid-column: 1 / -1;
}

.runtime-property-grid__wide {
  display: grid;
  grid-column: 1 / -1;
  gap: 6px;
}

.runtime-property-grid__title {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  color: #334155;
  font-size: 12px;
}

.runtime-business-binding {
  display: grid;
  gap: 10px;
}

.runtime-binding-note {
  padding: 8px 10px;
  color: #34516d;
  background: #f5f9ff;
  border: 1px solid #c6d7ea;
}

.runtime-binding-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.runtime-match-summary {
  display: grid;
  gap: 2px;
  padding: 8px 10px;
  color: #18324a;
  background: #eef6ff;
  border: 1px solid #b8cee5;
}

.runtime-match-summary strong {
  font-size: 13px;
}

.runtime-match-summary span {
  font-size: 12px;
}

.runtime-business-binding .runtime-property-grid {
  padding-top: 2px;
}

.runtime-advanced-json {
  display: grid;
  gap: 8px;
}

.runtime-advanced-json__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

@media (max-width: 1280px) {
  .press-slot-runtime-ide {
    grid-template-columns: 200px minmax(0, 1fr) 280px;
  }

  .runtime-header-grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}
</style>
