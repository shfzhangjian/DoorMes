<script lang="ts" setup>
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';
import type { PickerEntityConfig, PickerOption } from '#/components/picker';
import type { StandardMenuContext } from '../data';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, Empty, message, Spin, Tag } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  getWorkCenterLineLabel,
  getWorkCenterPage,
} from '#/api/mes/hc/workcenter';
import { getProductModelDetail } from '#/api/mes/hc/productmodel';
import {
  createStandard,
  getStandard,
  getStandardChangeLogs,
  updateStandard,
} from '#/api/mes/quality/base/standard';
import {
  materialPickerConfig,
  PickerInline,
  PickerModal,
  productModelPickerConfig,
} from '#/components/picker';

import { useFormSchema } from '../data';
import ItemList from './item-list.vue'; // 引入子表组件

defineOptions({ name: 'QualityStandardFormModal' });

type StandardFormType = 'copy' | 'create' | 'detail' | 'edit';

const emit = defineEmits(['success']);
const isUpdate = ref(false);
const formType = ref<StandardFormType>('create');
const standardContext = ref<StandardMenuContext>();
const itemListRef = ref<InstanceType<typeof ItemList>>();
const materialPickerOpen = ref(false);
const productModelPickerOpen = ref(false);
const selectedProductModelLevel = ref<'FAMILY' | 'MODEL'>();
const processPickerOpen = ref(false);
const formData = ref<MesQualityStandardApi.Standard>(
  {} as MesQualityStandardApi.Standard,
);
const changeLogs = ref<MesQualityStandardApi.ChangeLog[]>([]);
const changeLogLoading = ref(false);
const changeLogCollapsed = ref(true);
const emptyImage = Empty.PRESENTED_IMAGE_SIMPLE;
const DEFAULT_APPLY_SCOPE: Array<'MATERIAL' | 'PROCESS'> = [
  'MATERIAL',
  'PROCESS',
];
const IQC_SCOPE_MATERIAL = 'MATERIAL';
const IQC_SCOPE_UNIVERSAL = 'UNIVERSAL';
const fixedApplyType = computed(() => standardContext.value?.applyType);
const iqcMaterialOnly = computed(() => fixedApplyType.value === 'IQC');
const fixedFullScope = computed(
  () => fixedApplyType.value === 'FAI' || fixedApplyType.value === 'FQC',
);
const oqcOptionalDimension = computed(() => fixedApplyType.value === 'OQC');
const glueBoardFai = computed(() => fixedApplyType.value === 'GLUE_BOARD_FAI');
const familyProductModel = computed(
  () => selectedProductModelLevel.value === 'FAMILY',
);
const showChangeLog = computed(
  () => formType.value === 'detail' || formType.value === 'edit',
);
const VALUE_TEMPLATE_TEXT: Record<string, string> = {
  COMPRESSION_CALC: '压缩性能计算模板',
  DENSITY_CALC: '密度计算模板',
  SINGLE_VALUE: '单值实测模板',
};

type ProcessPickerRow = {
  id: number;
  processStage: string;
  processId?: number;
  processCode?: string;
  processName?: string;
  wcCode?: string;
  wcName?: string;
  lineCode?: string;
  lineName?: string;
  status?: number;
};

const getBaseTitle = computed(() => {
  const title = standardContext.value?.title || '检验标准';
  if (formType.value === 'detail') return `查看${title}`;
  if (formType.value === 'copy') return `复制新增${title}`;
  return isUpdate.value ? `编辑${title}` : `新增${title}`;
});

function getDefaultApplyScope() {
  if (glueBoardFai.value) return [];
  if (iqcMaterialOnly.value) return ['MATERIAL' as const];
  if (oqcOptionalDimension.value) return [];
  if (fixedFullScope.value) return [];
  return standardContext.value?.defaultApplyScope ?? DEFAULT_APPLY_SCOPE;
}

function getModalTitle() {
  if (formType.value !== 'detail') {
    return getBaseTitle.value;
  }
  if (formData.value.auditStatus !== 20) {
    return getBaseTitle.value;
  }
  return `${getBaseTitle.value}  审核时间：${formData.value.auditTime || '-'}`;
}

function normalizeApplyScope(value: any): Array<'MATERIAL' | 'PROCESS'> {
  if (glueBoardFai.value) return [];
  if (iqcMaterialOnly.value) return [];
  if (oqcOptionalDimension.value) return [];
  if (fixedFullScope.value) return [];
  if (!Array.isArray(value)) return [...getDefaultApplyScope()];
  return value.filter((item) => item === 'MATERIAL' || item === 'PROCESS');
}

function resolveApplyScope(data: Partial<MesQualityStandardApi.Standard>) {
  if (glueBoardFai.value) return [];
  const hasMaterial =
    !!data.materialId || !!data.materialCode || !!data.materialName;
  const hasProcess =
    !!data.processId || !!data.processCode || !!data.processName;
  if (iqcMaterialOnly.value) {
    return (data as any).iqcScopeType === IQC_SCOPE_UNIVERSAL
      ? []
      : (['MATERIAL'] as const);
  }
  if (fixedFullScope.value) {
    if (hasMaterial && hasProcess) return [...DEFAULT_APPLY_SCOPE];
    if (hasProcess) return ['PROCESS' as const];
    if (hasMaterial) return ['MATERIAL' as const];
    return [];
  }
  if (!oqcOptionalDimension.value) {
    const normalizedApplyScope = normalizeApplyScope((data as any).applyScope);
    const scopeSet = new Set<'MATERIAL' | 'PROCESS'>(normalizedApplyScope);
    if (hasMaterial) scopeSet.add('MATERIAL');
    if (hasProcess) scopeSet.add('PROCESS');
    const resolvedScope = [...scopeSet];
    if (resolvedScope.length > 0) return resolvedScope;
  }
  if (hasMaterial && hasProcess) return [...DEFAULT_APPLY_SCOPE];
  if (hasProcess) return ['PROCESS' as const];
  if (hasMaterial) return ['MATERIAL' as const];
  return [];
}

function resolveIqcScopeType(data?: Partial<MesQualityStandardApi.Standard>) {
  if (!iqcMaterialOnly.value) return undefined;
  const hasMaterial = !!data?.materialCode || !!data?.materialName;
  return hasMaterial ? IQC_SCOPE_MATERIAL : IQC_SCOPE_UNIVERSAL;
}

function isBlank(value?: string) {
  return !value || value.trim().length === 0;
}

function findMissingEntryRuleItem(items: MesQualityStandardApi.StandardItem[]) {
  return items.find(
    (item) =>
      item.itemType !== 'DATE' &&
      (isBlank(item.testFrequencyJudgement) || isBlank(item.templateParams)),
  );
}

function buildFormSchema() {
  return useFormSchema(formType.value, standardContext.value).map((item) => {
    const componentProps = item.componentProps as
      | Record<string, any>
      | undefined;
    item.componentProps = {
      ...componentProps,
      disabled: formType.value === 'detail' || componentProps?.disabled,
      ...(item.fieldName === 'applyScope'
        ? { onChange: handleApplyScopeChange }
        : {}),
      ...(item.fieldName === 'iqcScopeType'
        ? { onChange: handleIqcScopeTypeChange }
        : {}),
    };
    if (item.fieldName === 'remark')
      return { ...item, formItemClass: 'col-span-3' };
    return item;
  });
}

function clearRecordFields(record: Record<string, any>, fields: string[]) {
  fields.forEach((field) => {
    delete record[field];
  });
}

function buildCopyStandard(
  source: MesQualityStandardApi.Standard,
): MesQualityStandardApi.Standard {
  const copiedStandard = {
    ...source,
    standardNo: '',
    standardName: source.standardName
      ? `${source.standardName}副本`
      : source.standardName,
  } as MesQualityStandardApi.Standard & Record<string, any>;

  clearRecordFields(copiedStandard, [
    'id',
    'auditStatus',
    'auditorId',
    'auditorName',
    'auditTime',
    'auditRejectReason',
    'createTime',
    'updateTime',
    'creator',
    'updater',
    'deleted',
    'tenantId',
  ]);

  const tempItemIdBase = Date.now();
  copiedStandard.items = (source.items || []).map((item, index) => {
    const copiedItem = {
      ...item,
      id: tempItemIdBase + index,
    } as MesQualityStandardApi.StandardItem & Record<string, any>;
    clearRecordFields(copiedItem, [
      'standardId',
      'createTime',
      'updateTime',
      'creator',
      'updater',
      'deleted',
      'tenantId',
    ]);
    return copiedItem;
  });

  return copiedStandard;
}

const [BaseForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  layout: 'horizontal',
  wrapperClass: 'grid-cols-3',
  schema: buildFormSchema(),
  showDefaultActions: false,
});

const processPickerConfig = computed<PickerEntityConfig<ProcessPickerRow>>(
  () => ({
    entityKey: 'quality-process-stage',
    title: '选择工序',
    tableTitle: '工作中心工序列表',
    modalWidth: 980,
    inlinePanelWidth: 620,
    queryFields: [
      {
        field: 'keyword',
        label: '工序',
        placeholder: '请输入工序/工作中心编码/名称',
      },
    ],
    columns: [
      {
        field: 'processName',
        title: '工序',
        minWidth: 160,
        formatter: (value, row) => value || row?.processStage || '-',
      },
      { field: 'wcCode', title: '工作中心编码', minWidth: 150 },
      { field: 'wcName', title: '工作中心名称', minWidth: 180 },
      { field: 'lineName', title: '产线', minWidth: 120 },
      {
        field: 'status',
        title: '状态',
        width: 80,
        align: 'center',
        formatter: (value) => {
          if (value === undefined || value === null) return '-';
          return Number(value) === 0 ? '启用' : '停用';
        },
      },
    ],
    fetchPage: async ({ pageNo, pageSize, filters }) => {
      const keyword = String(filters?.keyword || '').trim();
      let res = await getWorkCenterPage({
        pageNo,
        pageSize,
        processName: keyword || undefined,
        status: 0,
      });
      if (keyword && res.list.length === 0) {
        res = await getWorkCenterPage({
          pageNo,
          pageSize,
          processStage: keyword,
          status: 0,
        });
      }
      if (keyword && res.list.length === 0) {
        res = await getWorkCenterPage({
          pageNo,
          pageSize,
          wcCode: keyword,
          status: 0,
        });
      }
      if (keyword && res.list.length === 0) {
        res = await getWorkCenterPage({
          pageNo,
          pageSize,
          wcName: keyword,
          status: 0,
        });
      }
      return {
        total: res.total,
        list: res.list.map((item: any) => ({
          id: item.id,
          processStage: item.processStage || '',
          processId: item.processId,
          processCode: item.processCode,
          processName: item.processName || item.processStage || '',
          wcCode: item.wcCode,
          wcName: item.wcName,
          lineCode: item.lineCode,
          lineName: getWorkCenterLineLabel(item.lineCode, item.lineName),
          status: item.status,
        })),
      };
    },
    buildOption: (row) => ({
      id: row.id,
      code: row.wcCode || '',
      name: row.processName || row.processStage || '',
      label: row.processName || row.processStage || row.wcName || '',
      status: row.status,
      extra: {
        workCenterId: row.id,
        wcCode: row.wcCode,
        wcName: row.wcName,
        lineCode: row.lineCode,
        lineName: row.lineName,
      },
      raw: row,
    }),
  }),
);

const [BaseModal, modalApi] = useVbenModal({
  title: '检验标准',
  class: 'quality-standard-form-modal',
  fullscreen: true,
  fullscreenButton: false,
  onCancel() {
    modalApi.close();
  },
  onConfirm: async () => {
    if (formType.value === 'detail') {
      modalApi.close();
      return;
    }
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.setState({ loading: true });
    try {
      const data =
        (await formApi.getValues()) as MesQualityStandardApi.Standard;
      if (fixedApplyType.value) {
        data.applyType = fixedApplyType.value;
      }
      if (familyProductModel.value) {
        data.materialId = undefined;
        data.materialCode = '';
        data.materialName = '';
        data.specification = '';
      }
      if (glueBoardFai.value) {
        if (!data.glueBoardModel?.trim()) {
          return message.warning('请选择胶板型号');
        }
        data.glueBoardModel = data.glueBoardModel.trim();
        data.materialId = undefined;
        data.materialCode = '';
        data.materialName = '';
        data.specification = '';
        data.productModelId = undefined;
        data.productModelCode = '';
        data.productModelName = '';
        data.prodType = '';
        data.prodTypeName = '';
        data.processId = undefined;
        data.processCode = '';
        data.processName = '';
      }
      if (iqcMaterialOnly.value) {
        data.productModelId = undefined;
        data.productModelCode = '';
        data.productModelName = '';
        data.prodType = '';
        data.prodTypeName = '';
        if ((data as any).iqcScopeType === IQC_SCOPE_UNIVERSAL) {
          data.materialId = undefined;
          data.materialCode = '';
          data.materialName = '';
          data.specification = '';
        }
      }
      const applyScope = resolveApplyScope(data);
      const needsMaterial = applyScope.includes('MATERIAL');
      const needsProcess = applyScope.includes('PROCESS');
      const hasMaterial =
        !!data.materialId || !!data.materialCode || !!data.materialName;
      const hasProcess =
        !!data.processId || !!data.processCode || !!data.processName;
      const hasCompleteMaterial =
        !!data.materialId && !!data.materialCode && !!data.materialName;
      if (hasMaterial && !hasCompleteMaterial) {
        return message.warning('请从候选列表中选择关联物料');
      }
      if (needsMaterial && !hasCompleteMaterial) {
        return message.warning('请选择关联物料');
      }
      if (
        needsProcess &&
        (!data.processId || !data.processCode || !data.processName)
      ) {
        return message.warning(
          hasProcess ? '请从候选列表中选择关联工序' : '请选择关联工序',
        );
      }
      const hasProductModel =
        !!data.productModelId ||
        !!data.productModelCode ||
        !!data.productModelName ||
        !!data.prodType ||
        !!data.prodTypeName;
      if (
        hasProductModel &&
        (!data.productModelId ||
          !data.productModelCode ||
          !data.productModelName)
      ) {
        return message.warning('请从候选列表中选择产品型号');
      }
      if (!needsMaterial) {
        data.materialId = undefined;
        data.materialCode = '';
        data.materialName = '';
        data.specification = '';
      }
      if (!needsProcess) {
        data.processId = undefined;
        data.processCode = '';
        data.processName = '';
      }
      delete (data as any).applyScope;
      delete (data as any).iqcScopeType;
      // 从子表组件获取检验项目明细数据
      data.items = itemListRef.value?.getData() || [];

      if (data.items.length === 0) {
        return message.warning('请至少添加一条检验项目明细！');
      }
      const missingEntryRuleItem = findMissingEntryRuleItem(data.items);
      if (missingEntryRuleItem) {
        return message.warning(
          `请配置检验项目“${
            missingEntryRuleItem.inspectionItem || '未命名'
          }”的位置/录入规则`,
        );
      }
      const invalidDateItem = data.items.find(
        (item) =>
          item.itemType === 'DATE' &&
          (item.expiryDays === undefined ||
            item.expiryDays === null ||
            item.expiryDays < 0),
      );
      if (invalidDateItem) {
        return message.warning(
          `请设置检验项目“${invalidDateItem.inspectionItem || '未命名'}”的过期天数`,
        );
      }

      if (isUpdate.value) {
        data.id = formData.value.id;
        await updateStandard(data, fixedApplyType.value);
        message.success('更新成功');
      } else {
        await createStandard(data, fixedApplyType.value);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } catch (error) {
      console.error(error);
    } finally {
      modalApi.setState({ loading: false });
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    const data = modalApi.getData<any>();
    formType.value = data?.type || 'create';
    standardContext.value = data?.standardContext;
    isUpdate.value = formType.value === 'edit';
    modalApi.setState({ title: getBaseTitle.value, fullscreen: true });

    formApi.updateSchema(buildFormSchema());
    await formApi.resetForm();
    const defaultApplyScope = [...getDefaultApplyScope()];
    formData.value = {
      applyScope: defaultApplyScope,
      applyType: fixedApplyType.value,
      iqcScopeType: iqcMaterialOnly.value ? IQC_SCOPE_MATERIAL : undefined,
    } as MesQualityStandardApi.Standard;
    changeLogs.value = [];
    changeLogCollapsed.value = true;
    selectedProductModelLevel.value = undefined;
    await formApi.setValues({
      applyScope: defaultApplyScope,
      applyType: fixedApplyType.value,
      iqcScopeType: iqcMaterialOnly.value ? IQC_SCOPE_MATERIAL : undefined,
    });

    await nextTick();
    itemListRef.value?.loadData([]);

    if (formType.value !== 'create' && data.id) {
      await loadStandardDetail(data.id);
    }
  },
});

async function loadStandardDetail(id: number) {
  modalApi.setState({ loading: true });
  try {
    const standard = await getStandard(id, fixedApplyType.value);
    formData.value =
      formType.value === 'copy' ? buildCopyStandard(standard) : standard;
    modalApi.setState({ title: getModalTitle() });
    const iqcScopeType = resolveIqcScopeType(formData.value);
    await formApi.setValues({
      ...formData.value,
      iqcScopeType,
      applyScope: resolveApplyScope({
        ...formData.value,
        iqcScopeType,
      }),
      ...(formType.value === 'detail'
        ? { auditorName: formData.value.auditorName || '未审核' }
        : {}),
    });
    itemListRef.value?.loadData(formData.value.items || []);
    await syncSelectedProductModelLevel(formData.value.productModelId);
    if (showChangeLog.value) await loadChangeLogs(id);
  } finally {
    modalApi.setState({ loading: false });
  }
}

async function loadChangeLogs(id: number) {
  if (!showChangeLog.value) return;
  changeLogLoading.value = true;
  try {
    changeLogs.value = await getStandardChangeLogs(id, fixedApplyType.value);
  } finally {
    changeLogLoading.value = false;
  }
}

function handleOpenMaterialSelect() {
  if (
    formType.value === 'detail' ||
    familyProductModel.value ||
    formData.value.iqcScopeType === IQC_SCOPE_UNIVERSAL
  )
    return;
  materialPickerOpen.value = true;
}

function handleOpenProductModelSelect() {
  if (formType.value === 'detail' || iqcMaterialOnly.value) return;
  productModelPickerOpen.value = true;
}

function handleOpenProcessSelect() {
  if (formType.value === 'detail') return;
  processPickerOpen.value = true;
}

function clearProcessFields() {
  formData.value.processId = undefined;
  formData.value.processCode = '';
  formData.value.processName = '';
  formApi.setValues({
    processId: undefined,
    processCode: '',
    processName: '',
  });
}

function clearMaterialFields() {
  formData.value.materialId = undefined;
  formData.value.materialCode = '';
  formData.value.materialName = '';
  formData.value.specification = '';
  formApi.setValues({
    materialId: undefined,
    materialCode: '',
    materialName: '',
    specification: '',
  });
}

function clearProductModelFields(productModelCode = '') {
  selectedProductModelLevel.value = undefined;
  formData.value.productModelId = undefined;
  formData.value.productModelCode = productModelCode;
  formData.value.productModelName = '';
  formData.value.prodType = '';
  formData.value.prodTypeName = '';
  formApi.setValues({
    productModelId: undefined,
    productModelCode,
    productModelName: '',
    prodType: '',
    prodTypeName: '',
  });
}

function handleApplyScopeChange(value: any) {
  const applyScope = normalizeApplyScope(value);
  formData.value.applyScope = applyScope;
  if (!applyScope.includes('MATERIAL')) {
    clearMaterialFields();
  }
  if (!applyScope.includes('PROCESS')) {
    clearProcessFields();
  }
}

function handleIqcScopeTypeChange(value: string) {
  const scopeType = String((value as any)?.target?.value ?? value);
  formData.value.iqcScopeType = scopeType as 'MATERIAL' | 'UNIVERSAL';
  if (scopeType === IQC_SCOPE_UNIVERSAL) {
    clearMaterialFields();
  }
}

function handleMaterialCodeInput(value: string) {
  if (formData.value.iqcScopeType === IQC_SCOPE_UNIVERSAL) return;
  const materialCode = String(value || '');
  formData.value.materialId = undefined;
  formData.value.materialCode = materialCode;
  formData.value.materialName = '';
  formData.value.specification = '';
  formApi.setValues({
    materialId: undefined,
    materialCode,
    materialName: '',
    specification: '',
  });
}

function handleMaterialSelected(option: PickerOption) {
  if (familyProductModel.value) {
    message.warning('系列通用标准不关联具体物料');
    return;
  }
  if (formData.value.iqcScopeType === IQC_SCOPE_UNIVERSAL) {
    message.warning('IQC 通用标准不关联具体物料');
    return;
  }
  const specification = option.extra?.specModel || option.raw?.specModel || '';
  formData.value.materialId = Number(option.id);
  formData.value.materialCode = option.code;
  formData.value.materialName = option.name;
  formData.value.specification = specification;
  formApi.setValues({
    materialId: Number(option.id),
    materialCode: option.code,
    materialName: option.name,
    specification,
  });
  materialPickerOpen.value = false;
}

function handleProductModelCodeInput(value: string) {
  clearProductModelFields(String(value || ''));
}

function handleProductModelSelected(option: PickerOption) {
  const prodType = option.extra?.prodType || option.raw?.prodType || '';
  const prodTypeName =
    option.extra?.prodTypeName || option.raw?.prodTypeName || prodType;
  selectedProductModelLevel.value =
    option.extra?.modelLevel || option.raw?.modelLevel || 'MODEL';
  if (selectedProductModelLevel.value === 'FAMILY') {
    clearMaterialFields();
  }
  formData.value.productModelId = Number(option.id);
  formData.value.productModelCode = option.code;
  formData.value.productModelName = option.name;
  formData.value.prodType = prodType;
  formData.value.prodTypeName = prodTypeName;
  formApi.setValues({
    productModelId: Number(option.id),
    productModelCode: option.code,
    productModelName: option.name,
    prodType,
    prodTypeName,
  });
  productModelPickerOpen.value = false;
}

async function syncSelectedProductModelLevel(productModelId?: number) {
  if (!productModelId) {
    selectedProductModelLevel.value = undefined;
    return;
  }
  try {
    const model = await getProductModelDetail(productModelId);
    selectedProductModelLevel.value = model.modelLevel || 'MODEL';
    if (selectedProductModelLevel.value === 'FAMILY') {
      clearMaterialFields();
    }
  } catch {
    selectedProductModelLevel.value = undefined;
  }
}

function handleProcessCodeInput(value: string) {
  const processCode = String(value || '');
  formData.value.processId = undefined;
  formData.value.processCode = processCode;
  formData.value.processName = '';
  formApi.setValues({
    processId: undefined,
    processCode,
    processName: '',
  });
}

function handleProcessSelected(option: PickerOption) {
  formData.value.processId = Number(option.id);
  formData.value.processCode = option.code;
  formData.value.processName = option.name;
  formApi.setValues({
    processId: Number(option.id),
    processCode: option.code,
    processName: option.name,
  });
  processPickerOpen.value = false;
}

function getScopeText(scope: MesQualityStandardApi.ChangeLog['changeScope']) {
  return scope === 'MAIN' ? '主表字段' : '检验项明细';
}

function getScopeColor(scope: MesQualityStandardApi.ChangeLog['changeScope']) {
  return scope === 'MAIN' ? 'blue' : 'purple';
}

function normalizeRuleCode(value?: string) {
  return String(value || '')
    .replace(/[-_]/g, '')
    .toLowerCase();
}

function parseLogJson(value: string) {
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, any>)
      : undefined;
  } catch {
    return undefined;
  }
}

function formatRuleField(field: any) {
  if (!field || typeof field !== 'object') return '';
  const label = field.name || field.code;
  if (!label) return '';
  return field.unit ? `${label}(${field.unit})` : String(label);
}

function formatJudgmentMetric(
  metric: any,
  resultFields: Array<Record<string, any>>,
) {
  if (!metric) return '';
  const metricText = String(metric);
  const metricCode = normalizeRuleCode(metricText);
  const matched = resultFields.find(
    (field) => normalizeRuleCode(field?.code) === metricCode,
  );
  if (!matched?.name) return metricText;
  return `${matched.name}(${metricText})`;
}

function formatTemplateParamsLogValue(value: string) {
  const params = parseLogJson(value);
  if (!params) return value;

  const dataRule =
    params.dataRule && typeof params.dataRule === 'object'
      ? params.dataRule
      : params;
  const positions = Array.isArray(params.positions) ? params.positions : [];
  const inputFields = Array.isArray(dataRule.inputFields)
    ? dataRule.inputFields
    : [];
  const resultFields = Array.isArray(dataRule.resultFields)
    ? dataRule.resultFields
    : [];
  const lines: string[] = [];
  const valueTemplate = params.valueTemplate
    ? VALUE_TEMPLATE_TEXT[String(params.valueTemplate)] ||
      String(params.valueTemplate)
    : '';
  if (valueTemplate) lines.push(`录入模板：${valueTemplate}`);
  if (positions.length > 0 || params.sampleSize || params.repeatCount) {
    const layoutParts: string[] = [];
    if (positions.length > 0 && params.repeatCount) {
      layoutParts.push(`${positions.length}点*${params.repeatCount}组`);
    }
    if (params.sampleSize) {
      layoutParts.push(`共${params.sampleSize}个样本`);
    }
    if (layoutParts.length > 0)
      lines.push(`检测布局：${layoutParts.join('，')}`);
  }
  const positionNames = positions
    .map((item: any) => item?.name || item?.code)
    .filter(Boolean);
  if (positionNames.length > 0) {
    lines.push(`检测点位：${positionNames.join(' / ')}`);
  }
  const inputText = inputFields.map(formatRuleField).filter(Boolean).join('、');
  if (inputText) lines.push(`录入字段：${inputText}`);
  const resultText = resultFields
    .map(formatRuleField)
    .filter(Boolean)
    .join('、');
  if (resultText) lines.push(`结果字段：${resultText}`);
  const judgmentMetric = formatJudgmentMetric(
    dataRule.judgmentMetric || params.judgmentMetric,
    resultFields,
  );
  if (judgmentMetric) lines.push(`判定指标：${judgmentMetric}`);
  return lines.length > 0 ? lines.join('\n') : value;
}

function displayLogValue(log: MesQualityStandardApi.ChangeLog, value?: string) {
  const text = value && value.trim() ? value.trim() : '';
  if (!text) return '-';
  if (log.fieldName === 'templateParams') {
    return formatTemplateParamsLogValue(text);
  }
  return text;
}
</script>

<template>
  <BaseModal :show-confirm-button="formType !== 'detail'">
    <div class="quality-standard-modal">
      <div class="quality-standard-modal__scroll">
        <div class="quality-standard-panel">
          <BaseForm>
            <template #materialCode>
              <PickerInline
                :model-value="formData.materialCode"
                :config="materialPickerConfig"
                placeholder="请输入物料编码或名称，或点击搜索选择"
                :disabled="formType === 'detail' || familyProductModel"
                @update:model-value="handleMaterialCodeInput"
                @search="handleOpenMaterialSelect"
                @pick="handleMaterialSelected"
              />
            </template>
            <template #productModelCode>
              <PickerInline
                v-if="!iqcMaterialOnly"
                :model-value="formData.productModelCode"
                :config="productModelPickerConfig"
                placeholder="请输入型号编码或名称，或点击搜索选择"
                :disabled="formType === 'detail'"
                @update:model-value="handleProductModelCodeInput"
                @search="handleOpenProductModelSelect"
                @pick="handleProductModelSelected"
              />
            </template>
            <template #processCode>
              <PickerInline
                :model-value="formData.processCode"
                :config="processPickerConfig"
                placeholder="请输入工序，或点击搜索选择"
                :disabled="formType === 'detail'"
                @update:model-value="handleProcessCodeInput"
                @search="handleOpenProcessSelect"
                @pick="handleProcessSelected"
              />
            </template>
          </BaseForm>
        </div>

        <div
          class="quality-standard-modal__content"
          :class="{
            'quality-standard-modal__content--single':
              !showChangeLog || changeLogCollapsed,
          }"
        >
          <Button
            v-if="showChangeLog && changeLogCollapsed"
            class="quality-standard-log__floating-toggle"
            size="small"
            @click="changeLogCollapsed = false"
          >
            修改内容日志 {{ changeLogs.length }} 条
          </Button>

          <div
            v-if="showChangeLog && !changeLogCollapsed"
            class="quality-standard-panel quality-standard-panel--logs"
          >
            <div class="quality-standard-log__header">
              <div class="flex items-center gap-2">
                <div class="h-3.5 w-1 rounded-sm bg-sky-600"></div>
                <span class="text-sm font-bold text-slate-700"
                  >修改内容日志</span
                >
              </div>
              <span class="text-xs text-slate-400">
                {{ changeLogs.length }} 条
              </span>
              <Button
                size="small"
                type="link"
                @click="changeLogCollapsed = true"
              >
                收起
              </Button>
            </div>

            <Spin :spinning="changeLogLoading">
              <Empty
                v-if="changeLogs.length === 0"
                description="暂无修改内容日志"
                :image="emptyImage"
              />
              <div v-else class="quality-standard-log__list">
                <div
                  v-for="log in changeLogs"
                  :key="log.id"
                  class="quality-standard-log__item"
                >
                  <div class="quality-standard-log__meta">
                    <Tag :color="getScopeColor(log.changeScope)">
                      {{ getScopeText(log.changeScope) }}
                    </Tag>
                    <span class="font-medium text-slate-700">
                      {{ log.fieldLabel }}
                    </span>
                  </div>
                  <div
                    v-if="log.changeScope === 'ITEM'"
                    class="quality-standard-log__item-label"
                  >
                    {{ log.itemLabel || '检验项明细' }}
                  </div>
                  <div class="quality-standard-log__diff">
                    <div>
                      <span>修改前</span>
                      <p>{{ displayLogValue(log, log.beforeValue) }}</p>
                    </div>
                    <div>
                      <span>修改后</span>
                      <p>{{ displayLogValue(log, log.afterValue) }}</p>
                    </div>
                  </div>
                  <div class="quality-standard-log__operator">
                    <span>{{ log.operatorName || '-' }}</span>
                    <span>{{ log.changeTime || '-' }}</span>
                  </div>
                </div>
              </div>
            </Spin>
          </div>

          <div class="quality-standard-panel quality-standard-panel--items">
            <ItemList
              ref="itemListRef"
              :apply-type="formData.applyType || fixedApplyType"
              :disabled="formType === 'detail'"
            />
          </div>
        </div>
      </div>
    </div>
  </BaseModal>

  <PickerModal
    :config="materialPickerConfig"
    :open="materialPickerOpen"
    title="选择物料"
    @close="materialPickerOpen = false"
    @pick="handleMaterialSelected"
  />

  <PickerModal
    v-if="!iqcMaterialOnly"
    :config="productModelPickerConfig"
    :open="productModelPickerOpen"
    title="选择产品型号"
    @close="productModelPickerOpen = false"
    @pick="handleProductModelSelected"
  />

  <PickerModal
    :config="processPickerConfig"
    :open="processPickerOpen"
    title="选择工序"
    @close="processPickerOpen = false"
    @pick="handleProcessSelected"
  />
</template>

<style>
.quality-standard-form-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.quality-standard-modal {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.quality-standard-modal__scroll {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding-right: 4px;
}

.quality-standard-panel {
  min-width: 0;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  padding: 16px 18px 14px;
}

.quality-standard-panel--items {
  display: flex;
  min-height: 420px;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.quality-standard-modal__content {
  position: relative;
  display: grid;
  min-height: 420px;
  flex: 1;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 12px;
}

.quality-standard-modal__content--single {
  grid-template-columns: minmax(0, 1fr);
}

.quality-standard-panel--logs {
  display: flex;
  min-height: 420px;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.quality-standard-log__floating-toggle {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 5;
  box-shadow: 0 2px 8px rgb(15 23 42 / 12%);
}

.quality-standard-log__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ant-color-border-secondary);
  padding: 10px 12px;
}

.quality-standard-log__list {
  max-height: 560px;
  overflow: auto;
  padding: 10px;
}

.quality-standard-log__item {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.quality-standard-log__item + .quality-standard-log__item {
  margin-top: 8px;
}

.quality-standard-log__meta {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.quality-standard-log__item-label {
  margin-top: 6px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quality-standard-log__diff {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.quality-standard-log__diff span {
  color: #94a3b8;
  font-size: 12px;
}

.quality-standard-log__diff p {
  min-height: 34px;
  max-height: 96px;
  margin: 4px 0 0;
  overflow: auto;
  border-radius: 4px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
  padding: 6px;
  white-space: pre-line;
  word-break: break-word;
}

.quality-standard-log__operator {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}
</style>
