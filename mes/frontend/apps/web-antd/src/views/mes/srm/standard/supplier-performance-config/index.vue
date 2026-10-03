<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmPerformanceSupplierConfigApi } from '#/api/mes/srm/performance/supplier-config';
import type { SrmEvaluationTemplateApi } from '#/api/mes/srm/standard/template';
import type { SystemUserApi } from '#/api/system/user';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Input,
  InputNumber,
  message,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createSupplierConfig,
  deleteSupplierConfig,
  getSupplierConfigDetail,
  getSupplierConfigPage,
  getSupplierItemConfig,
  saveSupplierItemConfig,
  updateSupplierConfig,
} from '#/api/mes/srm/performance/supplier-config';
import { getPublishedTemplateList } from '#/api/mes/srm/standard/template';
import { UserSelectModal } from '#/views/system/user/components';

import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmSupplierPerformanceConfig' });

type DetailMode = 'create' | 'detail' | 'edit';
type FormulaValidationStatus = 'default' | 'error' | 'success';
type UserPurpose = 'reporter' | 'scorer';

type ConfigForm = SrmPerformanceSupplierConfigApi.Config & {
  items: SrmPerformanceSupplierConfigApi.Item[];
  selectedTemplateVersionId?: number;
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;
const SRM_CALC_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 800;
const SRM_CALC_DROPDOWN_Z_INDEX = SRM_CALC_MODAL_Z_INDEX + 10;

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const loadingConfig = ref(false);
const publishedTemplates = ref<SrmEvaluationTemplateApi.Template[]>([]);
const selectedPeopleItem = ref<SrmPerformanceSupplierConfigApi.Item>();
const userPurpose = ref<UserPurpose>('scorer');
const calcModalOpen = ref(false);
const calcModalMaximized = ref(false);
const selectedCalcItem = ref<SrmPerformanceSupplierConfigApi.Item>();
const calcFormulaValidation = reactive<{
  message: string;
  status: FormulaValidationStatus;
}>({
  message: '',
  status: 'default',
});
const calcDraft = reactive<SrmPerformanceSupplierConfigApi.CalcRule>({
  aggregateMethod: 'SUM',
  enabled: true,
  missingPolicy: 'BLOCK',
  nodes: [],
  periodScope: 'QUARTER',
});
const form = reactive<ConfigForm>(createEmptyForm());

const isReadonly = computed(() => detailMode.value === 'detail');
const templateOptions = computed(() =>
  publishedTemplates.value
    .map((template) => {
      const version = template.currentVersion;
      const versionId = normalizeId(template.currentVersionId || version?.id);
      if (!versionId) {
        return undefined;
      }
      return {
        label: [
          template.templateCode,
          template.templateName,
          template.currentVersionNo || version?.versionNo,
        ]
          .filter(Boolean)
          .join(' / '),
        template,
        value: versionId,
      };
    })
    .filter(
      (
        item,
      ): item is {
        label: string;
        template: SrmEvaluationTemplateApi.Template;
        value: number;
      } => !!item,
    ),
);
const selectedTemplateLabel = computed(() => {
  const selectedOption = templateOptions.value.find(
    (item) => item.value === form.selectedTemplateVersionId,
  );
  return selectedOption?.label || templateText(form);
});
const subtitleItems = computed(() =>
  [
    form.configNo ? `编号 ${form.configNo}` : '',
    form.supplierName || '',
    selectedTemplateLabel.value,
    statusText(form.status),
    detailModeText(),
  ].filter(Boolean),
);

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入配置编号' },
    fieldName: 'configNo',
    label: '配置编号',
  },
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
    fieldName: 'supplierName',
    label: '供应商名称',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: statusOptions(),
      placeholder: '请选择状态',
    },
    fieldName: 'status',
    label: '状态',
  },
];

const gridColumns: VxeTableGridOptions['columns'] = [
  { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
  {
    field: 'configNo',
    fixed: 'left',
    minWidth: 190,
    slots: { default: 'configNo' },
    title: '配置编号',
  },
  { field: 'supplierCode', minWidth: 170, title: '供应商编号' },
  {
    field: 'supplierName',
    minWidth: 260,
    slots: { default: 'supplierName' },
    title: '供应商名称',
  },
  {
    field: 'templateNameSnapshot',
    minWidth: 280,
    slots: { default: 'templateName' },
    title: '季度评价模板',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 100,
    slots: { default: 'status' },
    title: '状态',
  },
  { field: 'remark', minWidth: 320, title: '备注' },
  { align: 'center', field: 'updateTime', minWidth: 170, title: '更新时间' },
  {
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 138,
  },
];

const itemColumns: TableColumnsType = [
  { dataIndex: 'groupNameSnapshot', fixed: 'left', title: '维度', width: 120 },
  {
    dataIndex: 'indicatorCodeSnapshot',
    fixed: 'left',
    title: '指标编码',
    width: 130,
  },
  {
    dataIndex: 'indicatorNameSnapshot',
    fixed: 'left',
    title: '评估指标',
    width: 230,
  },
  { dataIndex: 'scoringRuleSnapshot', title: '评分规则', width: 330 },
  { align: 'center', dataIndex: 'maxScoreSnapshot', title: '满分', width: 80 },
  { dataIndex: 'indicatorType', title: '指标类型', width: 150 },
  { dataIndex: 'targetValue', title: '目标值', width: 130 },
  { dataIndex: 'targetUnit', title: '单位', width: 110 },
  { dataIndex: 'redlineScore', title: '红线分值', width: 120 },
  { dataIndex: 'defaultDeptNames', title: '评分部门', width: 150 },
  { dataIndex: 'scorerUserName', title: '评分人', width: 230 },
  { dataIndex: 'reporterUserName', title: '实际上报人', width: 230 },
  {
    align: 'center',
    dataIndex: 'calcRule',
    fixed: 'right',
    title: '计算规则',
    width: 110,
  },
];

const calcNodeColumns: TableColumnsType = [
  { dataIndex: 'nodeKey', title: '变量名', width: 150 },
  { dataIndex: 'nodeName', title: '节点名称', width: 170 },
  { dataIndex: 'sourceMetricCode', title: '来源指标编码', width: 180 },
  { dataIndex: 'sourceMetricName', title: '来源指标名称', width: 220 },
  { dataIndex: 'aggregateMethod', title: '汇总方式', width: 120 },
  { dataIndex: 'unit', title: '单位', width: 100 },
  { align: 'center', dataIndex: 'actions', title: '操作', width: 80 },
];

const itemTableScrollX = computed(() =>
  itemColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
);
const calcModalWidth = computed(() =>
  calcModalMaximized.value ? 'calc(100vw - 32px)' : '1120px',
);
const calcModalBodyStyle = computed(() => ({
  maxHeight: calcModalMaximized.value
    ? 'calc(100dvh - 128px)'
    : 'calc(100dvh - 210px)',
  overflow: 'auto',
  padding: '0',
}));
const calcNodeTableScrollY = computed(() =>
  calcModalMaximized.value ? 460 : 260,
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: gridFormSchema,
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSupplierConfigPage({
            ...formValues,
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
          });
          return {
            list: result.list || [],
            total: Number(result.total || 0),
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmPerformanceSupplierConfigApi.Config>,
});

const [DetailModal, detailModalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal srm-erp-crud-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
});

const [SupplierModal, supplierModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function createEmptyForm(): ConfigForm {
  return {
    items: [],
    status: 'ENABLED',
  };
}

function resetForm(record: Partial<ConfigForm> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record, {
    items: normalizeConfigRows(record.items || []),
  });
}

async function openCreate() {
  await loadPublishedTemplates();
  resetForm();
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmPerformanceSupplierConfigApi.Config) {
  if (!row.id) {
    return;
  }
  await loadPublishedTemplates();
  const result = await getSupplierConfigDetail(row.id);
  applyConfigDetail(result);
  detailMode.value = 'detail';
  basicExpanded.value = true;
  detailModalApi.open();
  if (normalizeId(form.selectedTemplateVersionId)) {
    await loadItemConfig({ silent: true });
  }
}

async function openEdit(row: SrmPerformanceSupplierConfigApi.Config) {
  await openDetail(row);
  detailMode.value = 'edit';
}

function switchToEdit() {
  detailMode.value = 'edit';
}

async function closeDetail() {
  selectedPeopleItem.value = undefined;
  selectedCalcItem.value = undefined;
  calcModalOpen.value = false;
  await detailModalApi.close();
}

function applyConfigDetail(result: SrmPerformanceSupplierConfigApi.Config) {
  resetForm({
    ...result,
    items: normalizeConfigRows(result.items || []),
    selectedTemplateVersionId: normalizeId(result.currentTemplateVersionId),
  });
}

async function loadPublishedTemplates() {
  if (publishedTemplates.value.length > 0) {
    return;
  }
  publishedTemplates.value = await getPublishedTemplateList('QUARTER');
}

async function saveBaseConfig(closeAfter = false) {
  const id = await saveConfig();
  if (!id) {
    return;
  }
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (templateVersionId && form.items.length > 0) {
    await persistItemConfig(id, templateVersionId);
    message.success('供应商季度评分配置已保存');
  } else if (templateVersionId) {
    await loadItemConfig({ silent: true });
    message.success('基础信息已保存，请维护指标人员和计算规则');
  } else {
    message.success('基础信息已保存');
  }
  detailMode.value = 'edit';
  await gridApi.query();
  if (closeAfter) {
    await closeDetail();
  }
}

async function saveConfig() {
  if (!normalizeId(form.supplierId) || !form.supplierName?.trim()) {
    message.warning('请选择供应商');
    return undefined;
  }
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!templateVersionId) {
    message.warning('请选择已发布的季度评价模板');
    return undefined;
  }
  saving.value = true;
  try {
    const payload: SrmPerformanceSupplierConfigApi.Config = {
      configNo: form.configNo,
      currentTemplateVersionId: templateVersionId,
      id: form.id,
      remark: form.remark,
      status: form.status || 'ENABLED',
      supplierCode: form.supplierCode,
      supplierId: normalizeId(form.supplierId),
      supplierName: form.supplierName?.trim(),
      supplierSourceType: form.supplierSourceType || 'REGISTERED',
      version: form.version,
    };
    if (form.id) {
      await updateSupplierConfig(payload);
    } else {
      form.id = await createSupplierConfig(payload);
    }
    form.currentTemplateVersionId = templateVersionId;
    applySelectedTemplateSnapshot();
    return form.id;
  } finally {
    saving.value = false;
  }
}

async function handleTemplateChange(value: number) {
  form.selectedTemplateVersionId = value;
  applySelectedTemplateSnapshot();
  form.items = [];
  if (form.id) {
    await loadItemConfig({ silent: true });
  }
}

async function loadItemConfig(options: { silent?: boolean } = {}) {
  const configId = normalizeId(form.id);
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!configId) {
    if (!options.silent) {
      message.warning('请先保存供应商配置基础信息');
    }
    return;
  }
  if (!templateVersionId) {
    if (!options.silent) {
      message.warning('请选择已发布的季度评价模板');
    }
    return;
  }
  loadingConfig.value = true;
  try {
    const result = await getSupplierItemConfig(configId, templateVersionId);
    form.items = normalizeConfigRows(result.items || []);
  } finally {
    loadingConfig.value = false;
  }
}

async function saveItemConfig() {
  const configId = normalizeId(form.id) || (await saveConfig());
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!configId || !templateVersionId) {
    message.warning('请先保存供应商并选择模板');
    return;
  }
  if (form.items.length === 0) {
    await loadItemConfig();
  }
  await persistItemConfig(configId, templateVersionId);
  message.success('指标人员与计算规则已保存');
}

async function persistItemConfig(configId: number, templateVersionId: number) {
  await saveSupplierItemConfig({
    configId,
    items: form.items.map((item) => ({
      calcRule: normalizeCalcRule(item),
      indicatorType: item.indicatorType || 'MANUAL_SCORE',
      redlineScore: normalizeNumber(item.redlineScore),
      remark: item.remark,
      reporterCandidateUserIds: getCandidateIds(item, 'reporter'),
      reporterUserId: normalizeId(item.reporterUserId),
      scorerCandidateUserIds: getCandidateIds(item, 'scorer'),
      scorerUserId: normalizeId(item.scorerUserId),
      targetUnit: item.targetUnit,
      targetValue: normalizeNumber(item.targetValue),
      templateItemId: Number(item.templateItemId),
    })),
    templateVersionId,
  });
  form.currentTemplateVersionId = templateVersionId;
  applySelectedTemplateSnapshot();
  await loadItemConfig({ silent: true });
}

function openSupplierPicker() {
  if (isReadonly.value) {
    return;
  }
  supplierModalApi
    .setData({
      keyword: form.supplierName || form.supplierCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'roster',
      title: '选择评价供应商',
    })
    .open();
}

function handleSupplierSelected(row: Record<string, any>) {
  Object.assign(form, {
    supplierCode: row.supplierCode,
    supplierId: normalizeId(row.supplierId || row.id),
    supplierName: row.supplierName,
    supplierSourceType: row.sourceType || 'REGISTERED',
  });
}

function chooseUser(
  item: SrmPerformanceSupplierConfigApi.Item,
  purpose: UserPurpose,
) {
  selectedPeopleItem.value = item;
  userPurpose.value = purpose;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: getCandidateIds(item, purpose),
    })
    .open();
}

function handleUserConfirm(users: SystemUserApi.User[]) {
  const item = selectedPeopleItem.value;
  const validUsers = users.filter(
    (user): user is SystemUserApi.User & { id: number } =>
      normalizeId(user.id) !== undefined,
  );
  if (!item || validUsers.length === 0) {
    return;
  }
  const purpose = userPurpose.value;
  const selectedId =
    getSelectedId(item, purpose) &&
    validUsers.some((user) => user.id === getSelectedId(item, purpose))
      ? getSelectedId(item, purpose)!
      : validUsers[0]!.id;
  const candidateIds = mergeIds(
    getCandidateIds(item, purpose),
    validUsers.map((user) => user.id),
  );
  applyUserAssignment(item, purpose, selectedId, candidateIds, validUsers);
  selectedPeopleItem.value = undefined;
}

function applyUserAssignment(
  item: SrmPerformanceSupplierConfigApi.Item,
  purpose: UserPurpose,
  selectedId: number,
  candidateIds: number[],
  selectedUsers: Array<SystemUserApi.User & { id: number }>,
) {
  const optionMap = new Map<number, string>();
  getCandidateOptions(item, purpose).forEach((option) =>
    optionMap.set(option.value, option.label),
  );
  selectedUsers.forEach((user) =>
    optionMap.set(user.id, user.nickname || user.username || String(user.id)),
  );
  const names = candidateIds
    .map((id) => optionMap.get(id) || String(id))
    .join('、');
  const selectedName = optionMap.get(selectedId) || String(selectedId);
  if (purpose === 'scorer') {
    item.scorerUserId = selectedId;
    item.scorerUserName = selectedName;
    item.scorerCandidateUserIds = candidateIds.join(',') || undefined;
    item.scorerCandidateUserNames = names || undefined;
  } else {
    item.reporterUserId = selectedId;
    item.reporterUserName = selectedName;
    item.reporterCandidateUserIds = candidateIds.join(',') || undefined;
    item.reporterCandidateUserNames = names || undefined;
  }
}

function openCalcRule(item: SrmPerformanceSupplierConfigApi.Item) {
  selectedCalcItem.value = item;
  calcModalMaximized.value = false;
  resetCalcFormulaValidation();
  Object.keys(calcDraft).forEach(
    (key) => delete (calcDraft as Record<string, any>)[key],
  );
  const rule = item.calcRule || {};
  const defaultNodeKey = toFormulaVariableKey(item.indicatorCodeSnapshot);
  const defaultNodes = [
    {
      aggregateMethod: 'SUM',
      nodeKey: defaultNodeKey,
      nodeName: item.indicatorNameSnapshot,
      nodeType: 'SOURCE_METRIC',
      requiredFlag: true,
      sourceMetricCode: item.indicatorCodeSnapshot,
      sourceMetricName: item.indicatorNameSnapshot,
      unit: item.targetUnit,
    },
  ];
  Object.assign(calcDraft, {
    aggregateMethod: rule.aggregateMethod || 'SUM',
    enabled: rule.enabled ?? true,
    formulaExpr: rule.formulaExpr || `#${defaultNodeKey}`,
    missingPolicy: rule.missingPolicy || 'BLOCK',
    nodes: rule.nodes?.length
      ? rule.nodes.map((node) => ({ ...node }))
      : defaultNodes,
    periodScope: rule.periodScope || 'QUARTER',
    remark: rule.remark,
    ruleCode: rule.ruleCode || item.indicatorCodeSnapshot,
    ruleName: rule.ruleName || item.indicatorNameSnapshot,
    scoreFormulaExpr: rule.scoreFormulaExpr,
  });
  calcModalOpen.value = true;
}

function addCalcNode() {
  calcDraft.nodes = [
    ...(calcDraft.nodes || []),
    {
      aggregateMethod: 'SUM',
      nodeKey: `v${(calcDraft.nodes || []).length + 1}`,
      nodeType: 'SOURCE_METRIC',
      requiredFlag: true,
      sortNo: (calcDraft.nodes || []).length + 1,
    },
  ];
  resetCalcFormulaValidation();
}

function removeCalcNode(index: number) {
  calcDraft.nodes = (calcDraft.nodes || []).filter(
    (_, itemIndex) => itemIndex !== index,
  );
  resetCalcFormulaValidation();
}

function toggleCalcModalMaximized() {
  calcModalMaximized.value = !calcModalMaximized.value;
}

function resetCalcFormulaValidation() {
  calcFormulaValidation.message = '';
  calcFormulaValidation.status = 'default';
}

function validateCalcFormula(notify = true) {
  const errors: string[] = [];
  const nodes = calcDraft.nodes || [];
  const nodeKeys = new Set<string>();

  nodes.forEach((node, index) => {
    const rowLabel = `来源指标第 ${index + 1} 行`;
    const nodeKey = node.nodeKey?.trim();
    if (!nodeKey) {
      errors.push(`${rowLabel}请填写变量名`);
    } else if (!isValidFormulaVariableKey(nodeKey)) {
      errors.push(
        `${rowLabel}变量名只能包含字母、数字、下划线，且不能以数字开头`,
      );
    } else if (nodeKeys.has(nodeKey)) {
      errors.push(`${rowLabel}变量名重复：${nodeKey}`);
    } else {
      nodeKeys.add(nodeKey);
    }
    if (!node.sourceMetricCode?.trim()) {
      errors.push(`${rowLabel}请填写来源指标编码`);
    }
  });

  const formulaExpr = calcDraft.formulaExpr?.trim();
  const scoreFormulaExpr = calcDraft.scoreFormulaExpr?.trim();
  if (!formulaExpr && nodeKeys.size > 1) {
    errors.push('多个来源指标节点时，请填写实际值公式');
  }
  validateExpression('实际值公式', formulaExpr, nodeKeys, errors);
  const scoreVariables = new Set(nodeKeys);
  ['maxScore', 'result', 'targetValue'].forEach((variable) =>
    scoreVariables.add(variable),
  );
  validateExpression('得分公式', scoreFormulaExpr, scoreVariables, errors);

  calcFormulaValidation.status = errors.length > 0 ? 'error' : 'success';
  calcFormulaValidation.message =
    errors.length > 0
      ? errors.join('；')
      : `公式校验通过。可用来源变量：${formatFormulaRefs([...nodeKeys]) || '无'}；得分公式可额外使用 #result、#maxScore、#targetValue。`;

  if (notify) {
    if (errors.length > 0) {
      message.warning(errors[0]);
    } else {
      message.success('公式校验通过');
    }
  }
  return errors.length === 0;
}

function validateExpression(
  label: string,
  expression: string | undefined,
  allowedVariables: Set<string>,
  errors: string[],
) {
  if (!expression) {
    return;
  }
  if (!hasBalancedParentheses(expression)) {
    errors.push(`${label}括号不成对`);
  }
  if (countChar(expression, '?') !== countChar(expression, ':')) {
    errors.push(`${label}三元表达式 ? 和 : 数量不一致`);
  }
  const unknownVariables = extractFormulaVariables(expression).filter(
    (variable) => !allowedVariables.has(variable),
  );
  if (unknownVariables.length > 0) {
    errors.push(
      `${label}引用了未定义变量：${formatFormulaRefs(unknownVariables)}`,
    );
  }
}

function extractFormulaVariables(expression: string) {
  return [...expression.matchAll(/#([a-z_]\w*)/gi)]
    .map((match) => match[1])
    .filter(
      (variable, index, array): variable is string =>
        !!variable && array.indexOf(variable) === index,
    );
}

function hasBalancedParentheses(expression: string) {
  let depth = 0;
  for (const char of expression) {
    if (char === '(') {
      depth += 1;
    } else if (char === ')') {
      depth -= 1;
      if (depth < 0) {
        return false;
      }
    }
  }
  return depth === 0;
}

function countChar(value: string, char: string) {
  return [...value].filter((item) => item === char).length;
}

function isValidFormulaVariableKey(value: string) {
  return /^[a-z_]\w*$/i.test(value);
}

function toFormulaVariableKey(value?: unknown) {
  const normalized = String(value || 'v1')
    .trim()
    .replaceAll(/\W/g, '_')
    .replaceAll(/_+/g, '_')
    .replaceAll(/^_+|_+$/g, '');
  if (!normalized) {
    return 'v1';
  }
  return /^[a-z_]/i.test(normalized) ? normalized : `v_${normalized}`;
}

function formatFormulaRefs(variables: string[]) {
  return variables.map((variable) => `#${variable}`).join('、');
}

function confirmCalcRule() {
  const item = selectedCalcItem.value;
  if (!item) {
    return;
  }
  if (!calcDraft.ruleCode?.trim() || !calcDraft.ruleName?.trim()) {
    message.warning('请填写规则编码和规则名称');
    return;
  }
  if (!validateCalcFormula(true)) {
    return;
  }
  if (item.indicatorType === 'MANUAL_SCORE') {
    item.indicatorType = 'CALCULATED_SCORE';
  }
  item.calcRule = {
    ...calcDraft,
    nodes: (calcDraft.nodes || []).map((node, index) => ({
      ...node,
      aggregateMethod: node.aggregateMethod || 'SUM',
      nodeName: node.nodeName || node.nodeKey,
      nodeType: node.nodeType || 'SOURCE_METRIC',
      requiredFlag: node.requiredFlag ?? true,
      sortNo: node.sortNo || index + 1,
    })),
  };
  calcModalOpen.value = false;
  selectedCalcItem.value = undefined;
  resetCalcFormulaValidation();
}

function normalizeConfigRows(rows: SrmPerformanceSupplierConfigApi.Item[]) {
  return rows.map((row) => ({
    ...row,
    indicatorType: row.indicatorType || 'MANUAL_SCORE',
    reporterCandidateUserIds:
      getCandidateIds(row, 'reporter').join(',') ||
      row.reporterCandidateUserIds,
    reporterUserId:
      normalizeId(row.reporterUserId) || getCandidateIds(row, 'reporter')[0],
    reporterUserName:
      row.reporterUserName || splitNameList(row.reporterCandidateUserNames)[0],
    scorerCandidateUserIds:
      getCandidateIds(row, 'scorer').join(',') || row.scorerCandidateUserIds,
    scorerUserId:
      normalizeId(row.scorerUserId) || getCandidateIds(row, 'scorer')[0],
    scorerUserName:
      row.scorerUserName || splitNameList(row.scorerCandidateUserNames)[0],
  }));
}

function normalizeCalcRule(item: SrmPerformanceSupplierConfigApi.Item) {
  if (
    item.indicatorType !== 'CALCULATED_SCORE' &&
    item.indicatorType !== 'MIXED'
  ) {
    return undefined;
  }
  return item.calcRule;
}

function getCandidateIds(
  item: Partial<SrmPerformanceSupplierConfigApi.Item>,
  purpose: UserPurpose,
) {
  return mergeIds(
    parseIdList(
      purpose === 'scorer'
        ? item.scorerCandidateUserIds
        : item.reporterCandidateUserIds,
    ),
    getSelectedId(item, purpose) ? [getSelectedId(item, purpose)!] : [],
  );
}

function getSelectedId(
  item: Partial<SrmPerformanceSupplierConfigApi.Item>,
  purpose: UserPurpose,
) {
  return normalizeId(
    purpose === 'scorer' ? item.scorerUserId : item.reporterUserId,
  );
}

function getCandidateOptions(
  item: Partial<SrmPerformanceSupplierConfigApi.Item>,
  purpose: UserPurpose,
) {
  const ids = getCandidateIds(item, purpose);
  const names = splitNameList(
    purpose === 'scorer'
      ? item.scorerCandidateUserNames
      : item.reporterCandidateUserNames,
  );
  const selectedName =
    purpose === 'scorer' ? item.scorerUserName : item.reporterUserName;
  return ids.map((id, index) => ({
    label:
      names[index] ||
      (id === getSelectedId(item, purpose) ? selectedName : '') ||
      String(id),
    value: id,
  }));
}

function displayUsers(
  item: Partial<SrmPerformanceSupplierConfigApi.Item>,
  purpose: UserPurpose,
) {
  const names = splitNameList(
    purpose === 'scorer'
      ? item.scorerCandidateUserNames
      : item.reporterCandidateUserNames,
  );
  if (names.length > 0) {
    return names.join('、');
  }
  return purpose === 'scorer'
    ? item.scorerUserName || ''
    : item.reporterUserName || '';
}

function handleDelete(row: SrmPerformanceSupplierConfigApi.Config) {
  AntModal.confirm({
    content: `确认删除供应商 ${row.supplierName || row.supplierCode} 的季度评分配置吗？已被季度评价引用的配置不能删除。`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteSupplierConfig(row.id!);
      message.success('供应商季度评分配置已删除');
      await gridApi.query();
    },
    title: '删除供应商季度评分配置',
  });
}

function parseIdList(value?: string) {
  return String(value || '')
    .split(/[,，;；\s]+/)
    .map(Number)
    .filter(
      (item, index, array) =>
        Number.isFinite(item) && item > 0 && array.indexOf(item) === index,
    );
}

function splitNameList(value?: string) {
  return String(value || '')
    .split(/[,，;；、\n\r]+/)
    .map((item) => sanitizeName(item))
    .filter(
      (item, index, array): item is string =>
        !!item && array.indexOf(item) === index,
    );
}

function mergeIds(...groups: number[][]) {
  const result: number[] = [];
  groups.flat().forEach((id) => {
    if (Number.isFinite(id) && id > 0 && !result.includes(id)) {
      result.push(id);
    }
  });
  return result;
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0
    ? numericValue
    : undefined;
}

function normalizeNumber(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function sanitizeName(value: unknown) {
  const text = String(value ?? '').trim();
  return text && text !== '-' && text !== '0' ? text : '';
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function getCellValue(record: Record<string, any>, dataIndex: unknown) {
  return typeof dataIndex === 'string' ? record[dataIndex] : undefined;
}

function statusOptions() {
  return [
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' },
  ];
}

function statusText(status?: string) {
  return status === 'DISABLED' ? '停用' : '启用';
}

function statusColor(status?: string) {
  return status === 'DISABLED' ? 'error' : 'success';
}

function indicatorTypeOptions() {
  return [
    { label: '人工评分', value: 'MANUAL_SCORE' },
    { label: '计算评分', value: 'CALCULATED_SCORE' },
    { label: '计算+人工修正', value: 'MIXED' },
  ];
}

function indicatorTypeText(type?: string) {
  return (
    indicatorTypeOptions().find((item) => item.value === type)?.label ||
    '人工评分'
  );
}

function aggregateOptions() {
  return [
    { label: '求和', value: 'SUM' },
    { label: '平均', value: 'AVG' },
    { label: '最大', value: 'MAX' },
    { label: '最小', value: 'MIN' },
  ];
}

function periodScopeOptions() {
  return [
    { label: '季度', value: 'QUARTER' },
    { label: '月度', value: 'MONTH' },
  ];
}

function detailModeText() {
  if (detailMode.value === 'create') {
    return '新增';
  }
  if (detailMode.value === 'edit') {
    return '编辑';
  }
  return '明细';
}

function templateText(config: Partial<SrmPerformanceSupplierConfigApi.Config>) {
  return [
    config.templateCodeSnapshot,
    config.templateNameSnapshot,
    config.templateVersionNoSnapshot,
  ]
    .filter(Boolean)
    .join(' / ');
}

function applySelectedTemplateSnapshot() {
  const selectedOption = templateOptions.value.find(
    (item) => item.value === form.selectedTemplateVersionId,
  );
  const template = selectedOption?.template;
  form.currentTemplateId = normalizeId(template?.id);
  form.currentTemplateVersionId = normalizeId(selectedOption?.value);
  form.templateCodeSnapshot = template?.templateCode;
  form.templateNameSnapshot = template?.templateName;
  form.templateVersionNoSnapshot =
    template?.currentVersionNo || template?.currentVersion?.versionNo;
}

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

function resolveBodyContainer() {
  return document.body;
}
</script>

<template>
  <Page auto-content-height>
    <SupplierModal title="选择供应商" @select="handleSupplierSelected" />
    <UserModal
      :title="userPurpose === 'scorer' ? '选择评分人' : '选择实际上报人'"
      @confirm="handleUserConfirm"
    />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">供应商季度评分配置</div>
      </div>
      <div class="srm-crud-page__body">
        <Grid>
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.ADD,
                  label: '新增配置',
                  onClick: openCreate,
                  type: 'primary',
                },
              ]"
            />
          </template>

          <template #configNo="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.configNo || '-' }}
            </a>
          </template>
          <template #supplierName="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.supplierName || '-' }}
            </a>
          </template>
          <template #templateName="{ row }">
            <span class="srm-crud-cell">
              {{ templateText(row) || '-' }}
            </span>
          </template>
          <template #status="{ row }">
            <Tag :color="statusColor(row.status)" class="!m-0">
              {{ statusText(row.status) }}
            </Tag>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.PREVIEW,
                  label: '查看',
                  onClick: openDetail.bind(null, row),
                  type: 'link',
                },
              ]"
              :drop-down-actions="[
                {
                  icon: ACTION_ICON.EDIT,
                  label: '编辑',
                  onClick: openEdit.bind(null, row),
                  type: 'link',
                },
                {
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  label: '删除',
                  onClick: handleDelete.bind(null, row),
                  type: 'link',
                },
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>

    <DetailModal>
      <Spin :spinning="saving || loadingConfig" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>

            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">供应商季度评分配置</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="isReadonly"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑
              </Button>
              <Button
                v-if="!isReadonly"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="saveBaseConfig(false)"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>配置基本信息</strong>
                    </div>
                    <Button
                      size="small"
                      type="link"
                      @click="basicExpanded = !basicExpanded"
                    >
                      <span class="srm-nowrap-action">
                        <IconifyIcon
                          :icon="
                            basicExpanded
                              ? 'lucide:chevron-up'
                              : 'lucide:chevron-down'
                          "
                        />
                        {{ basicExpanded ? '收起' : '展开' }}
                      </span>
                    </Button>
                  </div>
                  <div v-show="basicExpanded" class="erp-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">配置编号</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="form.configNo"
                          placeholder="不填则自动生成"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.configNo || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评价供应商</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          :value="form.supplierName"
                          placeholder="点击选择供应商"
                          readonly
                          @click="openSupplierPicker"
                        >
                          <template #suffix>
                            <Button
                              class="qms-exception-inline-icon-btn"
                              size="small"
                              title="选择供应商"
                              type="text"
                              @click.stop="openSupplierPicker"
                            >
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">季度评价模板</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="form.selectedTemplateVersionId"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="templateOptions"
                          placeholder="请选择已发布的季度评价模板"
                          show-search
                          @change="handleTemplateChange"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ selectedTemplateLabel || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">状态</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="form.status"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="statusOptions()"
                        />
                        <Tag
                          v-else
                          :color="statusColor(form.status)"
                          class="!m-0"
                        >
                          {{ statusText(form.status) }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--wide">
                      <label class="erp-form-label">备注</label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="form.remark"
                          :rows="2"
                          placeholder="请输入备注"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ form.remark || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section
                  v-if="form.id"
                  class="erp-basic-form srm-detail-section"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>
                        指标人员与计算配置（{{ form.items.length }} 项）
                      </strong>
                    </div>
                    <Space v-if="!isReadonly" :size="8">
                      <Button @click="loadItemConfig()">
                        <IconifyIcon icon="lucide:refresh-cw" />
                        重新加载
                      </Button>
                      <Button type="primary" @click="saveItemConfig">
                        <IconifyIcon icon="lucide:users-round" />
                        保存明细
                      </Button>
                    </Space>
                  </div>
                  <Table
                    :columns="itemColumns"
                    :data-source="form.items"
                    :pagination="false"
                    row-key="templateItemId"
                    :scroll="{ x: itemTableScrollX, y: 430 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'indicatorType'">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="record.indicatorType"
                          class="srm-type-select"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="indicatorTypeOptions()"
                        />
                        <Tag v-else class="!m-0">
                          {{ indicatorTypeText(record.indicatorType) }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'targetValue'">
                        <InputNumber
                          v-if="!isReadonly"
                          v-model:value="record.targetValue"
                          class="srm-number-input"
                          :precision="4"
                        />
                        <span v-else>{{
                          displayValue(record.targetValue)
                        }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'targetUnit'">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="record.targetUnit"
                          class="srm-unit-input"
                        />
                        <span v-else>{{
                          displayValue(record.targetUnit)
                        }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'redlineScore'">
                        <InputNumber
                          v-if="!isReadonly"
                          v-model:value="record.redlineScore"
                          class="srm-number-input"
                          :precision="2"
                        />
                        <span v-else>
                          {{ displayValue(record.redlineScore) }}
                        </span>
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'scorerUserName'"
                      >
                        <div
                          v-if="!isReadonly"
                          class="srm-inline-picker"
                          @click="chooseUser(record, 'scorer')"
                        >
                          <Input
                            :value="displayUsers(record, 'scorer')"
                            placeholder="点击选择评分人"
                            readonly
                          />
                          <Button size="small" type="text">
                            <IconifyIcon icon="lucide:users-round" />
                          </Button>
                        </div>
                        <span v-else>
                          {{ displayUsers(record, 'scorer') || '-' }}
                        </span>
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'reporterUserName'"
                      >
                        <div
                          v-if="!isReadonly"
                          class="srm-inline-picker"
                          @click="chooseUser(record, 'reporter')"
                        >
                          <Input
                            :value="displayUsers(record, 'reporter')"
                            placeholder="点击选择上报人"
                            readonly
                          />
                          <Button size="small" type="text">
                            <IconifyIcon icon="lucide:upload-cloud" />
                          </Button>
                        </div>
                        <span v-else>
                          {{ displayUsers(record, 'reporter') || '-' }}
                        </span>
                      </template>
                      <template v-else-if="column.dataIndex === 'calcRule'">
                        <Button
                          :disabled="
                            isReadonly ||
                            record.indicatorType === 'MANUAL_SCORE'
                          "
                          size="small"
                          type="link"
                          @click="openCalcRule(record)"
                        >
                          <IconifyIcon icon="lucide:git-branch" />
                          维护
                        </Button>
                      </template>
                      <template v-else>
                        {{
                          displayValue(getCellValue(record, column.dataIndex))
                        }}
                      </template>
                    </template>
                  </Table>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </DetailModal>

    <AntModal
      v-model:open="calcModalOpen"
      :body-style="calcModalBodyStyle"
      :destroy-on-close="false"
      :get-container="resolveBodyContainer"
      :mask-closable="false"
      :width="calcModalWidth"
      :wrap-class-name="
        calcModalMaximized
          ? 'srm-calc-modal-wrap srm-calc-modal-wrap--maximized'
          : 'srm-calc-modal-wrap'
      "
      :z-index="SRM_CALC_MODAL_Z_INDEX"
    >
      <template #title>
        <div class="srm-calc-modal-title">
          <div>
            <div class="srm-calc-modal-title__main">指标计算树</div>
            <div class="srm-calc-modal-title__sub">
              {{ selectedCalcItem?.indicatorNameSnapshot || '计算指标' }}
            </div>
          </div>
          <Button type="text" @click.stop="toggleCalcModalMaximized">
            <IconifyIcon
              :icon="
                calcModalMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'
              "
            />
            {{ calcModalMaximized ? '还原' : '最大化' }}
          </Button>
        </div>
      </template>
      <div class="srm-calc-form">
        <div class="srm-calc-grid">
          <div class="srm-calc-field">
            <label class="srm-calc-label">规则编码</label>
            <div class="srm-calc-value">
              <Input
                v-model:value="calcDraft.ruleCode"
                placeholder="如 RULE-KPI-Q-LAR"
              />
              <div class="srm-calc-help">
                规则唯一标识，建议与指标编码保持可追溯关系。
              </div>
            </div>
          </div>
          <div class="srm-calc-field">
            <label class="srm-calc-label">规则名称</label>
            <div class="srm-calc-value">
              <Input
                v-model:value="calcDraft.ruleName"
                placeholder="请输入计算规则名称"
              />
              <div class="srm-calc-help">
                用于在配置明细和计算追溯中识别本规则。
              </div>
            </div>
          </div>
          <div class="srm-calc-field">
            <label class="srm-calc-label">取数周期</label>
            <div class="srm-calc-value">
              <Select
                v-model:value="calcDraft.periodScope"
                :dropdown-style="{ zIndex: SRM_CALC_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveBodyContainer"
                :options="periodScopeOptions()"
              />
              <div class="srm-calc-help">
                控制实际值按月度或季度口径参与汇总。
              </div>
            </div>
          </div>
          <div class="srm-calc-field">
            <label class="srm-calc-label">状态</label>
            <div class="srm-calc-value">
              <Select
                v-model:value="calcDraft.enabled"
                :dropdown-style="{ zIndex: SRM_CALC_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveBodyContainer"
                :options="[
                  { label: '启用', value: true },
                  { label: '停用', value: false },
                ]"
              />
              <div class="srm-calc-help">
                停用后保留规则内容，但不作为有效计算规则使用。
              </div>
            </div>
          </div>
          <div class="srm-calc-field srm-calc-field--wide">
            <label class="srm-calc-label">备注</label>
            <div class="srm-calc-value">
              <Input.TextArea
                v-model:value="calcDraft.remark"
                :rows="2"
                placeholder="请输入规则备注"
              />
              <div class="srm-calc-help">
                记录公式口径、异常处理或人工修正约定。
              </div>
            </div>
          </div>
          <div class="srm-calc-field">
            <label class="srm-calc-label">缺失策略</label>
            <div class="srm-calc-value">
              <Select
                v-model:value="calcDraft.missingPolicy"
                :dropdown-style="{ zIndex: SRM_CALC_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveBodyContainer"
                :options="[
                  { label: '阻断计算', value: 'BLOCK' },
                  { label: '缺失按0', value: 'ZERO' },
                ]"
              />
              <div class="srm-calc-help">
                来源实际值缺失时阻断评价，或按 0 参与公式。
              </div>
            </div>
          </div>
          <div class="srm-calc-field">
            <label class="srm-calc-label">默认汇总</label>
            <div class="srm-calc-value">
              <Select
                v-model:value="calcDraft.aggregateMethod"
                :dropdown-style="{ zIndex: SRM_CALC_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveBodyContainer"
                :options="aggregateOptions()"
              />
              <div class="srm-calc-help">
                来源节点未单独设置时默认使用的汇总方式。
              </div>
            </div>
          </div>
          <div class="srm-calc-field srm-calc-field--full">
            <label class="srm-calc-label srm-calc-label--tall">
              实际值公式
            </label>
            <div class="srm-calc-value">
              <Input.TextArea
                v-model:value="calcDraft.formulaExpr"
                :rows="2"
                placeholder="SpEL 公式，例如 #qualifiedLots * 100 / #totalLots"
                @blur="validateCalcFormula(false)"
              />
              <div class="srm-calc-help">
                使用来源节点变量计算实际值；变量写法为 #变量名。
              </div>
            </div>
          </div>
          <div class="srm-calc-field srm-calc-field--full">
            <label class="srm-calc-label srm-calc-label--tall">得分公式</label>
            <div class="srm-calc-value">
              <Input.TextArea
                v-model:value="calcDraft.scoreFormulaExpr"
                :rows="2"
                placeholder="可选，例如 #result >= 98 ? #maxScore : (#result < 90 ? 0 : #maxScore * #result / 98)"
                @blur="validateCalcFormula(false)"
              />
              <div class="srm-calc-help">
                基于实际值结果计算得分，可额外使用
                #result、#maxScore、#targetValue。
              </div>
            </div>
          </div>
          <div class="srm-calc-field srm-calc-field--full">
            <label class="srm-calc-label">校验结果</label>
            <div class="srm-calc-value srm-calc-validation">
              <Button size="small" @click="validateCalcFormula(true)">
                <IconifyIcon icon="lucide:shield-check" />
                校验公式
              </Button>
              <span
                class="srm-calc-validation__text"
                :class="`srm-calc-validation__text--${calcFormulaValidation.status}`"
              >
                {{
                  calcFormulaValidation.message ||
                  '保存前会检查变量名、来源指标编码、公式引用、括号和三元表达式。'
                }}
              </span>
            </div>
          </div>
        </div>
        <div class="detail-list-head srm-calc-node-head">
          <div class="detail-list-title">
            <strong>来源指标节点</strong>
            <span class="srm-calc-node-desc">
              变量名用于公式中的 #变量；来源指标编码对应实际上报明细。
            </span>
          </div>
          <Button size="small" type="primary" @click="addCalcNode">
            <IconifyIcon icon="lucide:plus" />
            增加节点
          </Button>
        </div>
        <Table
          :columns="calcNodeColumns"
          :data-source="calcDraft.nodes || []"
          :pagination="false"
          row-key="nodeKey"
          :scroll="{ x: 1020, y: calcNodeTableScrollY }"
          size="small"
        >
          <template #bodyCell="{ column, index, record }">
            <template v-if="column.dataIndex === 'nodeKey'">
              <Input v-model:value="record.nodeKey" />
            </template>
            <template v-else-if="column.dataIndex === 'nodeName'">
              <Input v-model:value="record.nodeName" />
            </template>
            <template v-else-if="column.dataIndex === 'sourceMetricCode'">
              <Input v-model:value="record.sourceMetricCode" />
            </template>
            <template v-else-if="column.dataIndex === 'sourceMetricName'">
              <Input v-model:value="record.sourceMetricName" />
            </template>
            <template v-else-if="column.dataIndex === 'aggregateMethod'">
              <Select
                v-model:value="record.aggregateMethod"
                :dropdown-style="{ zIndex: SRM_CALC_DROPDOWN_Z_INDEX }"
                :get-popup-container="resolveBodyContainer"
                :options="aggregateOptions()"
              />
            </template>
            <template v-else-if="column.dataIndex === 'unit'">
              <Input v-model:value="record.unit" />
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Button
                danger
                size="small"
                type="link"
                @click="removeCalcNode(index)"
              >
                删除
              </Button>
            </template>
          </template>
        </Table>
      </div>
      <template #footer>
        <Space :size="8">
          <Button @click="validateCalcFormula(true)">
            <IconifyIcon icon="lucide:shield-check" />
            校验公式
          </Button>
          <Button @click="calcModalOpen = false">取消</Button>
          <Button type="primary" @click="confirmCalcRule">
            <IconifyIcon icon="lucide:check" />
            确定
          </Button>
        </Space>
      </template>
    </AntModal>
  </Page>
</template>

<style scoped>
.srm-crud-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.srm-crud-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-crud-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-crud-page__body {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-crud-link {
  color: #1d4ed8;
  cursor: pointer;
  font-weight: 700;
}

.srm-crud-link:hover {
  text-decoration: underline;
}

.srm-crud-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-detail-section {
  margin-top: 12px;
}

.srm-nowrap-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.srm-inline-picker {
  display: grid;
  min-width: 190px;
  align-items: center;
  gap: 4px;
  grid-template-columns: minmax(0, 1fr) 32px;
}

.srm-inline-picker :deep(.ant-input) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-number-input {
  width: 112px;
}

.srm-type-select {
  width: 132px;
}

.srm-unit-input {
  width: 92px;
}

.srm-calc-form {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
  background: #fff;
}

.srm-calc-node-head {
  border-top: 1px solid #dbe5f1;
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px 12px;
}

.srm-calc-node-head .detail-list-title {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 10px;
}

.srm-calc-node-head .detail-list-title strong {
  flex-shrink: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.srm-calc-node-desc {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-calc-modal-title {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.srm-calc-modal-title__main {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  line-height: 20px;
}

.srm-calc-modal-title__sub {
  max-width: 520px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-calc-grid {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
}

.srm-calc-field {
  display: grid;
  min-width: 0;
  grid-template-columns: 112px minmax(0, 1fr);
}

.srm-calc-field--wide {
  grid-column: span 2;
}

.srm-calc-field--full {
  grid-column: 1 / -1;
}

.srm-calc-label,
.srm-calc-value {
  min-height: 64px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 9px 10px;
  font-size: 13px;
  line-height: 20px;
}

.srm-calc-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #475569;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

.srm-calc-label--tall {
  min-height: 86px;
}

.srm-calc-value {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  background: #fff;
}

.srm-calc-value .ant-input,
.srm-calc-value .ant-select,
.srm-calc-value .ant-input-number {
  width: 100%;
}

.srm-calc-help {
  color: #64748b;
  font-size: 12px;
  line-height: 17px;
}

.srm-calc-validation {
  display: grid;
  align-items: center;
  gap: 8px;
  grid-template-columns: auto minmax(0, 1fr);
}

.srm-calc-validation__text {
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
}

.srm-calc-validation__text--success {
  color: #047857;
}

.srm-calc-validation__text--error {
  color: #dc2626;
}

:global(.srm-calc-modal-wrap .ant-modal) {
  top: 36px;
  max-width: calc(100vw - 32px);
  padding-bottom: 0;
}

:global(.srm-calc-modal-wrap--maximized .ant-modal) {
  top: 16px;
  width: calc(100vw - 32px) !important;
  max-width: none;
}

:global(.srm-calc-modal-wrap .ant-modal-content) {
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  box-shadow: 0 18px 42px rgb(15 23 42 / 24%);
}

:global(.srm-calc-modal-wrap .ant-modal-header) {
  border-bottom: 1px solid #cbd5e1;
  background: #f8fafc;
  padding: 10px 16px;
}

:global(.srm-calc-modal-wrap .ant-modal-body) {
  background: #fff;
}

:global(.srm-calc-modal-wrap .ant-modal-footer) {
  border-top: 1px solid #cbd5e1;
  background: #f8fafc;
  padding: 8px 12px;
}

@media (max-width: 1024px) {
  .srm-calc-grid {
    grid-template-columns: 1fr;
  }

  .srm-calc-field--wide,
  .srm-calc-field--full {
    grid-column: 1;
  }

  .srm-calc-validation {
    grid-template-columns: 1fr;
  }
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>
