<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcBomApi } from '#/api/mes/hc/bom';
import type { MesQmsCoaApi } from '#/api/mes/quality/coa';
import type { PickerOption } from '#/components/picker';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useDictStore } from '@vben/stores';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  SelectOption,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditCoaTemplate,
  changeCoaTemplateStatus,
  createCoaTemplate,
  getCoaTemplate,
  getCoaTemplatePage,
  submitCoaTemplate,
  updateCoaTemplate,
  upgradeCoaTemplate,
} from '#/api/mes/quality/coa';
import { getBomProductModelOptions } from '#/api/mes/hc/bom';
import {
  PickerInline,
  PickerModal,
  productBomPickerConfig,
} from '#/components/picker';

import CoaStandardItemMultiSelect from '../components/CoaStandardItemMultiSelect.vue';
import CoaSpecFormulaModal from '../components/CoaSpecFormulaModal.vue';
import {
  isCoaSpecFormula,
  parseCoaSpecFormula,
} from '../components/coa-spec-formula';

import '../../../hc/package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesQualityCoaTemplate' });

const saving = ref(false);
const modalOpen = ref(false);
const detailItemsMaximized = ref(false);
const rows = ref<MesQmsCoaApi.Template[]>([]);
const standardPickerOpen = ref(false);
const specFormulaEditorOpen = ref(false);
const specFormulaItem = ref<MesQmsCoaApi.TemplateItem>();
const productBomPickerOpen = ref(false);
const productModelOptions = ref<MesHcBomApi.ProductModelOption[]>([]);
const productModelValue = ref<number | undefined>();
const dictStore = useDictStore();
const query = reactive({
  keyword: '',
  auditStatus: undefined as string | undefined,
});
const pageNo = ref(1);
const pageSize = ref(20);
const templateTotal = ref(0);
const itemGroupOptions = computed(() => {
  const options = dictStore.getDictOptions('mes_qms_coa_item_group').map((option) => ({
    label: option.label,
    value: option.value,
  }));
  return options.length > 0
    ? options
    : [
        { label: '绒毛层（未磨皮）', value: '绒毛层（未磨皮）' },
        { label: '绒毛层', value: '绒毛层' },
        { label: '上层胶不压槽', value: '上层胶不压槽' },
        { label: '上层胶压槽', value: '上层胶压槽' },
        { label: '成品', value: '成品' },
      ];
});
const existingStandardItemIds = computed(() =>
  form.items
    .map((item) => item.sourceStandardItemId)
    .filter((id): id is number => typeof id === 'number'),
);

const emptyForm = (): MesQmsCoaApi.Template => ({
  items: [],
  languageType: 'ZH_CN_EN_US',
  reportTitle: 'Certificate of Analysis / 检验报告',
  templateName: '',
  versionNo: 'V1.0',
});
const form = reactive<MesQmsCoaApi.Template>(emptyForm());
const templateReadonly = computed(
  () =>
    Boolean(form.id) && !['DRAFT', 'REJECTED'].includes(form.auditStatus || ''),
);
const approvedCount = computed(
  () => rows.value.filter((item) => item.auditStatus === 'APPROVED').length,
);
const enabledCount = computed(
  () => rows.value.filter((item) => item.status === 1).length,
);

const columns = [
  {
    field: 'templateCode',
    fixed: 'left',
    showOverflow: 'tooltip',
    title: '模板编码',
    width: 170,
  },
  {
    field: 'templateName',
    fixed: 'left',
    showOverflow: 'tooltip',
    title: '模板名称',
    width: 200,
  },
  { field: 'customerName', showOverflow: 'tooltip', title: '客户', width: 150 },
  {
    field: 'customerProductCode',
    showOverflow: 'tooltip',
    title: '客户产品',
    width: 150,
  },
  {
    field: 'productModelCode',
    showOverflow: 'tooltip',
    title: '内部型号',
    width: 140,
  },
  { field: 'versionNo', title: '版本', width: 90 },
  {
    field: 'auditStatus',
    slots: { default: 'auditStatus' },
    title: '审核状态',
    width: 110,
  },
  { field: 'status', slots: { default: 'status' }, title: '启用', width: 80 },
  { field: 'auditorName', title: '审核人', width: 110 },
  {
    field: 'actions',
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 300,
  },
];

function itemGroupRowSpan(index: number) {
  if (!templateReadonly.value) return 1;
  const current = form.items[index]?.itemGroup || '';
  if (!current || form.items[index - 1]?.itemGroup === current) return 0;
  let span = 1;
  while (form.items[index + span]?.itemGroup === current) span += 1;
  return span;
}

const itemColumns = computed(() => [
  { title: '序号', key: 'rowNo', fixed: 'left', width: 62 },
  {
    title: '项目分类',
    key: 'itemGroup',
    width: 150,
    customCell: (_record: MesQmsCoaApi.TemplateItem, index: number) => ({
      rowSpan: itemGroupRowSpan(index),
    }),
  },
  { title: 'ITEM 项目名称', key: 'itemNameCn', width: 180 },
  { title: 'Unit 单位', key: 'unit', width: 100 },
  { title: '内控 Spec', key: 'specText', width: 210 },
  { title: '目标值', key: 'targetValue', width: 110 },
  { title: 'COA Spec', key: 'coaSpecText', width: 190 },
  { title: '测试方法', key: 'inspectionMethod', width: 190 },
  { title: '取值方式', key: 'valueSourceType', width: 130 },
  { title: '取值标准', key: 'sourceStandard', width: 180 },
  { title: '检验工序', key: 'sourceProcess', width: 130 },
  { title: '取值项目', key: 'sourceInspectionItem', width: 180 },
  { title: '取值规则', key: 'valueStrategy', width: 135 },
  { title: '操作', key: 'actions', fixed: 'right', width: 78 },
]);

async function queryTemplatePage(page?: {
  currentPage?: number;
  pageSize?: number;
}) {
  pageNo.value = page?.currentPage || pageNo.value;
  pageSize.value = page?.pageSize || pageSize.value;
  const data = await getCoaTemplatePage({
    ...query,
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  });
  rows.value = data.list || [];
  templateTotal.value = Number(data.total || 0);
  return data;
}

async function load() {
  await templateGridApi.query();
}

function resetForm() {
  // reactive 对象不会因 Object.assign 自动移除旧字段，先清空避免新建继承详情的 ID 和审核状态。
  Object.keys(form).forEach((key) => Reflect.deleteProperty(form, key));
  Object.assign(form, emptyForm());
  productBomPickerOpen.value = false;
  productModelValue.value = undefined;
}

async function openCreate() {
  resetForm();
  detailItemsMaximized.value = false;
  await loadProductModelOptions();
  modalOpen.value = true;
}

async function openEdit(row: MesQmsCoaApi.Template) {
  const detail = await getCoaTemplate(row.id!);
  resetForm();
  Object.assign(form, detail, {
    items: (detail.items || []).map((item) => ({
      ...item,
      valueSourceType:
        item.valueSourceType ||
        (item.valueStrategy === 'FIXED' ? 'MANUAL_ENTRY' : 'PROCESS_INSPECTION'),
      valueStrategy: item.valueStrategy === 'FIXED' ? 'MANUAL' : item.valueStrategy,
    })),
  });
  detailItemsMaximized.value = false;
  productModelValue.value = form.productModelId;
  await loadProductModelOptions(form.productModelCode);
  modalOpen.value = true;
}

function importStandardItems(selected: MesQmsCoaApi.StandardItem[]) {
  const existed = new Set(form.items.map((item) => item.sourceStandardItemId));
  for (const source of selected) {
    if (existed.has(source.id)) continue;
    form.items.push({
      allowCorrectionFlag: true,
      coaDisplayFlag: true,
      decimalPlaces: 4,
      itemGroup: source.processName || '检验项目',
      itemNameCn: source.inspectionItem,
      metricCode: source.sheetMetricCode || `STD_ITEM_${source.id}`,
      requiredFlag: true,
      sortNo: (form.items.length + 1) * 10,
      sourceInspectionItem: source.inspectionItem,
      sourceItemType: source.itemType,
      sourceProcessId: source.processId,
      sourceProcessCode: source.processCode,
      sourceProcessName: source.processName,
      sourceStandardApplyType: source.standardApplyType,
      sourceStandardId: source.standardId,
      sourceStandardItemId: source.id,
      sourceStandardNo: source.standardNo,
      sourceStandardVersion: source.standardVersion,
      specSource: 'STANDARD_SNAPSHOT',
      specText: source.standardDesc,
      targetValue: source.targetValue,
      lowerLimit: source.minValue,
      upperLimit: source.maxValue,
      unit: source.unit,
      inspectionMethod: source.inspectionMethod,
      valueSourceType: 'PROCESS_INSPECTION',
      valueStrategy: source.itemType === 'QUALITATIVE' ? 'QA_RESULT' : 'QA_AVG',
    });
  }
  message.success(`已引入 ${selected.length} 个标准项目`);
}

function addManualItem(valueSourceType: 'MANUAL_ENTRY' | 'PHOTO_UPLOAD' = 'MANUAL_ENTRY') {
  form.items.push({
    allowCorrectionFlag: true,
    coaDisplayFlag: true,
    decimalPlaces: 4,
    itemNameCn: valueSourceType === 'PHOTO_UPLOAD' ? '图片项目' : '人工填写项目',
    metricCode: `${valueSourceType === 'PHOTO_UPLOAD' ? 'PHOTO' : 'MANUAL'}_${Date.now()}`,
    requiredFlag: true,
    sortNo: (form.items.length + 1) * 10,
    specSource: 'CUSTOMER',
    valueSourceType,
    valueStrategy: valueSourceType === 'PHOTO_UPLOAD' ? 'PHOTO' : 'MANUAL',
  });
}

function openSpecFormulaEditor(item: MesQmsCoaApi.TemplateItem) {
  specFormulaItem.value = item;
  specFormulaEditorOpen.value = true;
}

function applySpecFormula(payload: {
  formula: string;
  lowerLimit?: number;
  targetValue?: number;
  upperLimit?: number;
}) {
  if (!specFormulaItem.value) return;
  Object.assign(specFormulaItem.value, {
    lowerLimit: payload.lowerLimit,
    specText: payload.formula,
    upperLimit: payload.upperLimit,
  });
  if (payload.targetValue !== undefined) {
    specFormulaItem.value.targetValue = payload.targetValue;
  }
  specFormulaEditorOpen.value = false;
  message.success('已应用内控判定公式');
}

function validateSpecFormulas() {
  for (const item of form.items) {
    if (!isCoaSpecFormula(item.specText)) continue;
    const parsed = parseCoaSpecFormula(item.specText);
    if (!parsed.valid) {
      message.warning(`项目“${item.itemNameCn}”的内控公式无效，请通过公式编辑器修正`);
      return false;
    }
    item.specText = parsed.formula;
    item.lowerLimit = parsed.lowerLimit;
    item.upperLimit = parsed.upperLimit;
    if (parsed.targetValue !== undefined) item.targetValue = parsed.targetValue;
  }
  return true;
}

function changeValueSourceType(
  item: MesQmsCoaApi.TemplateItem,
  valueSourceType: MesQmsCoaApi.TemplateItem['valueSourceType'],
) {
  item.valueSourceType = valueSourceType;
  if (valueSourceType === 'PROCESS_INSPECTION') {
    item.valueStrategy = item.valueStrategy === 'MANUAL' || item.valueStrategy === 'PHOTO'
      ? 'QA_AVG'
      : item.valueStrategy;
    return;
  }
  // 人工和照片项目不能保留标准项目外键，防止后续 COA 从错误的检验记录取值。
  item.sourceStandardApplyType = undefined;
  item.sourceStandardId = undefined;
  item.sourceStandardNo = undefined;
  item.sourceStandardVersion = undefined;
  item.sourceStandardItemId = undefined;
  item.sourceInspectionItem = undefined;
  item.sourceItemType = undefined;
  item.sourceProcessId = undefined;
  item.sourceProcessCode = undefined;
  item.sourceProcessName = undefined;
  item.valueStrategy = valueSourceType === 'PHOTO_UPLOAD' ? 'PHOTO' : 'MANUAL';
}

async function loadProductModelOptions(keyword?: string) {
  productModelOptions.value = await getBomProductModelOptions({
    keyword,
    productMaterialId: form.materialId,
  });
}

async function confirmClearSelectedItems(): Promise<boolean> {
  if (form.items.length === 0) {
    return true;
  }
  return new Promise((resolve) => {
    Modal.confirm({
      cancelText: '取消',
      content:
        '当前模板已选择标准明细。修改内部产品型号或内部物料编码会清空全部已选项目，需按新的型号、料号重新选择。是否继续？',
      okText: '清空并继续',
      onCancel: () => resolve(false),
      onOk: () => {
        form.items.splice(0, form.items.length);
        resolve(true);
      },
      title: '变更模板关联信息',
    });
  });
}

async function selectProductModel(value?: number | string) {
  const numericValue = value ? Number(value) : undefined;
  const productModelId =
    numericValue && Number.isFinite(numericValue) && numericValue > 0 ? numericValue : undefined;
  const modelChanged = form.productModelId !== productModelId;
  if (modelChanged && !(await confirmClearSelectedItems())) {
    return;
  }
  productModelValue.value = productModelId;
  if (!productModelId) {
    form.productModelId = undefined;
    form.productModelCode = '';
    form.productModelName = '';
    form.materialId = undefined;
    form.materialCode = '';
    form.materialName = '';
    return;
  }
  if (!productModelOptions.value.some((item) => Number(item.value) === productModelId)) {
    await loadProductModelOptions();
  }
  const option = productModelOptions.value.find((item) => Number(item.value) === productModelId);
  if (!option) {
    message.warning('未找到所选产品型号，请重新选择');
    return;
  }
  if (form.productModelId && form.productModelId !== productModelId) {
    form.materialId = undefined;
    form.materialCode = '';
    form.materialName = '';
  }
  form.productModelId = productModelId;
  form.productModelCode = option.productModelCode || option.code || option.label;
  form.productModelName = option.productModelName || option.modelName || '';
}

function updateProductMaterialCode(value: string) {
  form.materialCode = value;
  form.materialId = undefined;
  form.materialName = '';
}

async function selectProductMaterial(option: PickerOption) {
  const extra = option.extra || {};
  const nextMaterialId = Number(extra.productMaterialId || option.id) || undefined;
  const nextProductModelId = Number(extra.productModelId || form.productModelId || 0) || undefined;
  const headerChanged =
    form.materialId !== nextMaterialId || form.productModelId !== nextProductModelId;
  if (headerChanged && !(await confirmClearSelectedItems())) {
    return;
  }
  form.materialId = nextMaterialId;
  form.materialCode = String(extra.productMaterialCode || option.code || '');
  form.materialName = String(extra.productMaterialName || option.name || '');
  form.productModelId = nextProductModelId;
  form.productModelCode = String(extra.productModelCode || form.productModelCode || '');
  form.productModelName = String(extra.productModelName || form.productModelName || '');
  productModelValue.value = nextProductModelId;
  productBomPickerOpen.value = false;
  await loadProductModelOptions(form.productModelCode);
}

function openProductMaterialPicker() {
  if (!templateReadonly.value) {
    productBomPickerOpen.value = true;
  }
}

function openStandardPicker() {
  if (!form.productModelCode || !form.productModelId) {
    message.warning('请先从选择组件选定内部产品型号');
    return;
  }
  standardPickerOpen.value = true;
}

async function save() {
  if (
    !form.templateName ||
    !form.reportTitle ||
    !form.versionNo ||
    !form.productModelId ||
    !form.materialId ||
    form.items.length === 0
  ) {
    message.warning('请填写模板名称、报告标题、版本，并从选择组件选择型号、料号和至少一个项目');
    return;
  }
  if (!validateSpecFormulas()) return;
  saving.value = true;
  try {
    await (form.id ? updateCoaTemplate(form) : createCoaTemplate(form));
    message.success('模板保存成功');
    modalOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function submit(row: MesQmsCoaApi.Template) {
  await submitCoaTemplate(row.id!);
  message.success('已提交审核');
  await load();
}

async function audit(row: MesQmsCoaApi.Template, result: 'PASS' | 'REJECT') {
  await auditCoaTemplate({
    id: row.id!,
    result,
    opinion: result === 'PASS' ? '审核通过' : '审核驳回',
  });
  message.success(result === 'PASS' ? '审核通过' : '已驳回');
  await load();
}

async function toggleStatus(row: MesQmsCoaApi.Template) {
  await changeCoaTemplateStatus({
    id: row.id!,
    status: row.status === 1 ? 0 : 1,
  });
  message.success(row.status === 1 ? '已停用' : '已启用');
  await load();
}

async function upgradeVersion(row: MesQmsCoaApi.Template) {
  const id = await upgradeCoaTemplate(row.id!);
  await load();
  await openEdit({ id } as MesQmsCoaApi.Template);
  message.success('已生成新版本草稿，原版本已停用');
}

function statusColor(status?: string) {
  return (
    {
      APPROVED: 'success',
      DRAFT: 'default',
      PENDING_REVIEW: 'processing',
      REJECTED: 'error',
    }[status || ''] || 'default'
  );
}

function statusText(status?: string) {
  return (
    {
      APPROVED: '已审核',
      DRAFT: '草稿',
      PENDING_REVIEW: '待审核',
      REJECTED: '已驳回',
    }[status || ''] ||
    status ||
    '-'
  );
}

function resetQuery() {
  query.keyword = '';
  query.auditStatus = undefined;
  pageNo.value = 1;
  load();
}

const [TemplateGrid, templateGridApi] = useVbenVxeGrid({
  gridOptions: {
    autoResize: true,
    border: true,
    columns,
    height: 'auto',
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => queryTemplatePage(page),
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions<MesQmsCoaApi.Template>,
});
</script>

<template>
  <Page auto-content-height>
    <!-- eslint-disable vue/html-closing-bracket-newline vue/multiline-html-element-content-newline -->
    <div class="package-fg-console coa-template-board">
      <section class="prototype-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:files" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">COA模板管理</h2>
            <Tag class="console-title-tag" color="blue">报告配置</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">模板总数</span>
              <span class="console-meta-value">{{ templateTotal }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页审核</span>
              <span class="console-meta-value">{{ approvedCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页启用</span>
              <span class="console-meta-value">{{ enabledCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页记录</span>
              <span class="console-meta-value">{{ rows.length }}</span>
            </span>
          </div>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="load">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
          <button
            v-access:code="['mes:qms-coa-template:create']"
            class="action-tile"
            type="button"
            @click="openCreate"
          >
            <IconifyIcon icon="lucide:file-plus-2" />
            <span>新建模板</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar coa-query-panel">
        <div class="coa-simple-query">
          <label>关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="模板编码 / 模板名称 / 客户 / 产品"
            @press-enter="load"
          />
          <label>审核状态</label>
          <Select
            v-model:value="query.auditStatus"
            allow-clear
            placeholder="审核状态"
          >
            <SelectOption value="DRAFT">草稿</SelectOption
            ><SelectOption value="PENDING_REVIEW">待审核</SelectOption>
            <SelectOption value="APPROVED">已审核</SelectOption
            ><SelectOption value="REJECTED">已驳回</SelectOption>
          </Select>
          <Button type="primary" @click="load">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
        </div>
      </section>

      <section class="package-fg-grid-panel shipping-notice-grid-panel">
        <TemplateGrid table-title="COA模板台账">
          <template #auditStatus="{ row }">
            <Tag :color="statusColor(row.auditStatus)">
              {{ statusText(row.auditStatus) }}
            </Tag>
          </template>
          <template #status="{ row }">
            <Tag :color="row.status === 1 ? 'success' : 'default'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </Tag>
          </template>
          <template #actions="{ row }">
            <div class="row-actions">
              <Button
                v-if="['DRAFT', 'REJECTED'].includes(row.auditStatus)"
                v-access:code="['mes:qms-coa-template:update']"
                size="small"
                type="link"
                @click="openEdit(row)"
                >编辑</Button
              >
              <Button
                v-else
                v-access:code="['mes:qms-coa-template:query']"
                size="small"
                type="link"
                @click="openEdit(row)"
                >详情</Button
              >
              <Button
                v-if="['DRAFT', 'REJECTED'].includes(row.auditStatus)"
                v-access:code="['mes:qms-coa-template:submit']"
                size="small"
                type="link"
                @click="submit(row)"
                >提交</Button
              >
              <template v-if="row.auditStatus === 'PENDING_REVIEW'">
                <Button
                  v-access:code="['mes:qms-coa-template:audit']"
                  size="small"
                  type="link"
                  @click="audit(row, 'PASS')"
                  >通过</Button
                >
                <Popconfirm
                  title="确认驳回该模板？"
                  @confirm="audit(row, 'REJECT')"
                >
                  <Button
                    v-access:code="['mes:qms-coa-template:audit']"
                    danger
                    size="small"
                    type="link"
                    >驳回</Button
                  >
                </Popconfirm>
              </template>
              <Button
                v-if="row.auditStatus === 'APPROVED'"
                v-access:code="['mes:qms-coa-template:status']"
                size="small"
                type="link"
                @click="toggleStatus(row)"
                >{{ row.status === 1 ? '停用' : '启用' }}</Button
              >
              <Popconfirm
                v-if="row.auditStatus === 'APPROVED' && row.status === 1"
                title="确认升级版本？将复制明细为新草稿并停用当前版本。"
                @confirm="upgradeVersion(row)"
              >
                <Button
                  v-access:code="['mes:qms-coa-template:update']"
                  size="small"
                  type="link"
                  >版本升级</Button
                >
              </Popconfirm>
            </div>
          </template>
        </TemplateGrid>
      </section>

      <Modal
        v-model:open="modalOpen"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal"
        @cancel="modalOpen = false"
      >
        <div
          class="report-modal-body inspection-task-modal-body"
          :class="{ 'shipping-detail-maximized-body': detailItemsMaximized }"
        >
          <div
            class="shipping-notice-form-top"
            :class="{
              'shipping-notice-form-top--compact': detailItemsMaximized,
            }"
          >
            <div class="shipping-notice-form-title-row">
              <span></span>
              <div class="inspection-form-title shipping-notice-title">
                {{
                  form.id
                    ? templateReadonly
                      ? 'COA模板详情'
                      : '编辑COA模板'
                    : '新建COA模板'
                }}
              </div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="modalOpen = false">关闭</Button>
                <Button
                  v-if="!templateReadonly"
                  :loading="saving"
                  size="small"
                  type="primary"
                  @click="save"
                  >保存模板</Button
                >
              </div>
            </div>
            <div class="shipping-form-sections">
              <fieldset class="shipping-form-fieldset">
                <legend>基本信息</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>模板名称</label>
                  <div class="inspection-form-control">
                    <Input
                      v-model:value="form.templateName"
                      :disabled="templateReadonly"
                    />
                  </div>
                  <label>模板编码</label>
                  <div class="inspection-form-control">
                    <Input
                      v-model:value="form.templateCode"
                      :disabled="templateReadonly"
                      placeholder="留空自动生成"
                    />
                  </div>
                  <label>版本</label>
                  <div class="inspection-form-control">
                    <Input
                      v-model:value="form.versionNo"
                      :disabled="templateReadonly"
                    />
                  </div>
                  <label>语言</label>
                  <div class="inspection-form-control">
                    <Select
                      v-model:value="form.languageType"
                      :disabled="templateReadonly"
                    >
                      <SelectOption value="ZH_CN">中文</SelectOption
                      ><SelectOption value="EN_US">英文</SelectOption
                      ><SelectOption value="ZH_CN_EN_US">中英双语</SelectOption>
                    </Select>
                  </div>
                </div>
              </fieldset>
              <fieldset class="shipping-form-fieldset">
                <legend>客户与产品匹配</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>客户名称</label>
                  <div class="inspection-form-control">
                    <Input
                      v-model:value="form.customerName"
                      :disabled="templateReadonly"
                    />
                  </div>
                  <label>客户产品编码</label>
                  <div class="inspection-form-control">
                    <Input
                      v-model:value="form.customerProductCode"
                      :disabled="templateReadonly"
                    />
                  </div>
                  <label>内部物料编码</label>
                  <div class="inspection-form-control">
                    <PickerInline
                      :config="productBomPickerConfig"
                      :disabled="templateReadonly"
                      :model-value="form.materialCode"
                      placeholder="输入产品料号或点击选择"
                      @pick="selectProductMaterial"
                      @search="openProductMaterialPicker"
                      @update:model-value="updateProductMaterialCode"
                    />
                  </div>
                  <label>内部产品型号</label>
                  <div class="inspection-form-control">
                    <Select
                      v-if="!templateReadonly"
                      :value="productModelValue"
                      show-search
                      allow-clear
                      :filter-option="false"
                      :options="productModelOptions.map((item) => ({ label: item.label, value: item.value }))"
                      placeholder="请选择BOM中的产品型号"
                      @search="loadProductModelOptions"
                      @change="selectProductModel"
                    />
                    <Input v-else :value="form.productModelCode || '-'" readonly />
                  </div>
                </div>
              </fieldset>
              <fieldset class="shipping-form-fieldset">
                <legend>报告呈现</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>报告标题</label>
                  <div
                    class="inspection-form-control shipping-form-control--full-row"
                  >
                    <Input
                      v-model:value="form.reportTitle"
                      :disabled="templateReadonly"
                    />
                  </div>
                  <label>页脚声明</label>
                  <div
                    class="inspection-form-control shipping-form-control--full-row"
                  >
                    <Textarea
                      v-model:value="form.footerStatement"
                      :disabled="templateReadonly"
                      :rows="2"
                    />
                  </div>
                </div>
              </fieldset>
            </div>
          </div>

          <Tabs class="shipping-detail-tabs">
            <template #rightExtra>
              <Button
                size="small"
                @click="detailItemsMaximized = !detailItemsMaximized"
              >
                <template #icon>
                  <IconifyIcon
                    :icon="
                      detailItemsMaximized
                        ? 'lucide:minimize-2'
                        : 'lucide:maximize-2'
                    "
                  />
                </template>
                {{ detailItemsMaximized ? '还原' : '最大化' }}
              </Button>
            </template>
            <TabPane key="items" :tab="`报告项目配置 (${form.items.length})`">
              <div class="inspection-form-subtitle coa-item-heading">
                <div>
                  <span>报告项目配置</span>
                  <em>内容列按客户 COA 表维护；取值配置只用于报告填写和来源追溯</em>
                </div>
                <div v-if="!templateReadonly" class="item-toolbar">
                  <Button size="small" type="primary" @click="openStandardPicker">选择检验标准明细</Button>
                  <Button size="small" @click="() => addManualItem()">新建人工填写</Button>
                  <Button size="small" @click="() => addManualItem('PHOTO_UPLOAD')">新建上传照片</Button>
                </div>
              </div>
              <div
                class="inspection-task-detail-table shipping-vxe-table"
                :class="{
                  'shipping-vxe-table--maximized': detailItemsMaximized,
                }"
              >
                <Table
                  :columns="itemColumns"
                  :data-source="form.items"
                  :pagination="false"
                  bordered
                  class="coa-detail-table"
                  row-key="metricCode"
                  size="small"
                  :scroll="{
                    x: 2400,
                    y: detailItemsMaximized ? 'calc(100vh - 170px)' : 330,
                  }"
                >
                  <template #bodyCell="{ column, record, index }">
                    <span v-if="column.key === 'rowNo'">{{ index + 1 }}</span>
                    <Select
                      v-if="column.key === 'itemGroup' && !templateReadonly"
                      v-model:value="record.itemGroup"
                      :options="itemGroupOptions"
                      allow-clear
                      style="width: 140px"
                    />
                    <span v-else-if="column.key === 'itemGroup'">{{ record.itemGroup || '-' }}</span>
                    <Input
                      v-else-if="column.key === 'itemNameCn'"
                      v-model:value="record.itemNameCn"
                      :disabled="templateReadonly"
                    />
                    <Input
                      v-else-if="column.key === 'unit'"
                      v-model:value="record.unit"
                      :disabled="templateReadonly"
                    />
                    <div v-else-if="column.key === 'specText'" class="coa-spec-editor">
                      <Input
                        v-model:value="record.specText"
                        :disabled="templateReadonly"
                        placeholder="如 39.7~55.7、50±2、≥39.7"
                      />
                      <Button
                        :disabled="templateReadonly"
                        size="small"
                        type="text"
                        @click="openSpecFormulaEditor(record)"
                      >
                        <template #icon><IconifyIcon icon="lucide:calculator" /></template>
                      </Button>
                    </div>
                    <InputNumber
                      v-else-if="column.key === 'targetValue'"
                      v-model:value="record.targetValue"
                      :disabled="templateReadonly"
                      style="width: 95px"
                    />
                    <Input
                      v-else-if="column.key === 'coaSpecText'"
                      v-model:value="record.coaSpecText"
                      :disabled="templateReadonly"
                    />
                    <Input
                      v-else-if="column.key === 'inspectionMethod'"
                      v-model:value="record.inspectionMethod"
                      :disabled="templateReadonly"
                    />
                    <Select
                      v-else-if="column.key === 'valueSourceType'"
                      v-model:value="record.valueSourceType"
                      :disabled="templateReadonly"
                      style="width: 120px"
                      @change="(value) => changeValueSourceType(record, value)"
                    >
                      <SelectOption value="PROCESS_INSPECTION">过程检验</SelectOption>
                      <SelectOption value="PHOTO_UPLOAD">上传照片</SelectOption>
                      <SelectOption value="MANUAL_ENTRY">人工填写</SelectOption>
                    </Select>
                    <span v-else-if="column.key === 'sourceStandard'">
                      {{ record.sourceStandardApplyType || '-' }} / {{ record.sourceStandardNo || '-' }}
                    </span>
                    <span v-else-if="column.key === 'sourceProcess'">{{ record.sourceProcessName || '-' }}</span>
                    <span v-else-if="column.key === 'sourceInspectionItem'">{{ record.sourceInspectionItem || '-' }}</span>
                    <Select
                      v-else-if="column.key === 'valueStrategy'"
                      v-model:value="record.valueStrategy"
                      :disabled="templateReadonly"
                      style="width: 130px"
                    >
                      <SelectOption value="QA_AVG">QA平均值</SelectOption
                      ><SelectOption value="QA_MIN">QA最小值</SelectOption
                      ><SelectOption value="QA_MAX">QA最大值</SelectOption
                      ><SelectOption value="QA_RESULT">QA定性结果</SelectOption
                      ><SelectOption value="LATEST_SAMPLE"
                        >最新样本</SelectOption
                      ><SelectOption value="VARIANCE">方差</SelectOption>
                      ><SelectOption value="MANUAL">人工结果</SelectOption>
                      ><SelectOption value="PHOTO">图片结果</SelectOption>
                    </Select>
                    <Button
                      v-else-if="column.key === 'actions' && !templateReadonly"
                      danger
                      size="small"
                      type="link"
                      @click="form.items.splice(index, 1)"
                      >删除</Button
                    >
                  </template>
                </Table>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </Modal>
      <PickerModal
        :config="productBomPickerConfig"
        :initial-filters="form.productModelCode ? { productModelCode: form.productModelCode } : undefined"
        :open="productBomPickerOpen"
        title="选择产品料号"
        :z-index="6200"
        @close="productBomPickerOpen = false"
        @pick="selectProductMaterial"
      />
      <CoaStandardItemMultiSelect
        :existing-item-ids="existingStandardItemIds"
        :open="standardPickerOpen"
        :product-model-code="form.productModelCode"
        @close="standardPickerOpen = false"
        @select="importStandardItems"
      />
      <CoaSpecFormulaModal
        :formula="specFormulaItem?.specText"
        :item-name="specFormulaItem?.itemNameCn"
        :open="specFormulaEditorOpen"
        :target-value="specFormulaItem?.targetValue"
        :unit="specFormulaItem?.unit"
        @apply="applySpecFormula"
        @close="specFormulaEditorOpen = false"
      />
    </div>
  </Page>
</template>

<style scoped>
.coa-template-board {
  gap: 6px;
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.coa-template-board .prototype-banner {
  min-height: 66px;
  max-height: 74px;
}

.coa-query-panel {
  min-width: 0;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.coa-query-panel {
  padding: 8px;
}

.coa-simple-query {
  display: grid;
  grid-template-columns: 76px minmax(260px, 1fr) 88px minmax(160px, 280px) 88px;
  align-items: stretch;
  gap: 8px;
}

.coa-simple-query > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.coa-simple-query :deep(.ant-input),
.coa-simple-query :deep(.ant-input-affix-wrapper),
.coa-simple-query :deep(.ant-select),
.coa-simple-query :deep(.ant-select-selector),
.coa-simple-query :deep(.ant-btn) {
  width: 100%;
  min-height: 32px;
  border-radius: 0;
}

.shipping-notice-grid-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.shipping-notice-grid-panel :deep(.vben-vxe-grid),
.shipping-notice-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.shipping-notice-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.shipping-notice-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.shipping-notice-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.report-modal-body {
  display: flex;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
  color: #1f2937;
  background:
    linear-gradient(90deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    linear-gradient(0deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    #f5f7fa;
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-title-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.inspection-form-title {
  color: #1677ff;
  font-size: 15px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}

.shipping-notice-title {
  color: #0f5fcf;
  font-size: 22px;
  font-weight: 950;
  line-height: 30px;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections {
  display: none;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.inspection-form-head {
  display: grid;
  grid-template-columns:
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #f3f4f6;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.inspection-form-control :deep(.ant-input),
.inspection-form-control :deep(.ant-input-affix-wrapper),
.inspection-form-control :deep(.ant-select),
.inspection-form-control :deep(.ant-select-selector),
.inspection-form-control :deep(textarea.ant-input) {
  width: 100%;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.shipping-form-control--full-row {
  grid-column: span 7;
}

.inspection-form-subtitle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #334155;
  font-size: 12px;
}

.inspection-form-subtitle span {
  font-weight: 900;
}

.inspection-form-subtitle em {
  margin-left: 10px;
  color: #64748b;
  font-style: normal;
}

.coa-item-heading,
.item-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-toolbar {
  margin-left: auto;
}

.shipping-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 0 8px 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-detail-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content-holder),
.shipping-detail-tabs :deep(.ant-tabs-content),
.shipping-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.shipping-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.inspection-task-detail-table {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #8794a4;
}

.inspection-task-detail-table :deep(.ant-table-wrapper),
.inspection-task-detail-table :deep(.ant-spin-nested-loading),
.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table),
.inspection-task-detail-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.inspection-task-detail-table :deep(.ant-spin-container),
.inspection-task-detail-table :deep(.ant-table-container),
.inspection-task-detail-table :deep(.ant-table) {
  display: flex;
  flex-direction: column;
}

.inspection-task-detail-table :deep(.ant-table-body) {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow: auto !important;
}

.inspection-task-detail-table :deep(.ant-table-thead > tr > th) {
  color: #263445;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-color: #a2adba;
}

.coa-detail-table :deep(.ant-table-thead > tr > th) {
  background: transparent;
}

.inspection-task-detail-table :deep(.ant-table-tbody > tr > td) {
  color: #172033;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border-color: #d7dee8;
}

.shipping-detail-maximized-body .shipping-detail-tabs,
.shipping-vxe-table--maximized {
  flex: 1 1 auto;
  min-height: 0;
}

.source-cell {
  display: flex;
  flex-direction: column;
}

.source-cell span {
  color: #64748b;
  font-size: 12px;
}

.coa-spec-editor {
  display: flex;
  align-items: center;
  gap: 2px;
}

.coa-spec-editor :deep(.ant-input) {
  min-width: 0;
}

@media (max-width: 1200px) {
  .inspection-form-head {
    grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  }

  .shipping-form-control--full-row {
    grid-column: span 3;
  }
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  display: flex;
  width: 100vw !important;
  height: 100vh !important;
  flex-direction: column;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  width: 100vw !important;
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  flex: 1 1 auto;
  width: 100vw;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    linear-gradient(0deg, rgb(30 41 59 / 4%) 1px, transparent 1px) 0 0 / 28px
      28px,
    #f5f7fa;
}

.rough-report-work-modal .report-modal-toolbar {
  flex: 0 0 auto;
}

.rough-report-work-modal .modal-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}
</style>
