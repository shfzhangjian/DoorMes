import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { RouteLocationNormalizedLoaded } from 'vue-router';

import { GLUE_BOARD_MODEL_OPTIONS } from '../../../hc/execution/report/adhesive-glue-board/data';

export type StandardApplyType =
  | 'FAI'
  | 'FQC'
  | 'GLUE_BOARD_FAI'
  | 'IPQC'
  | 'IQC'
  | 'OQC';

export interface StandardMenuContext {
  applyType: StandardApplyType;
  title: string;
  permissionPrefix: string;
  processVisible: boolean;
  defaultApplyScope: Array<'MATERIAL' | 'PROCESS'>;
}

function isIqcMaterialOnlyContext(context?: StandardMenuContext) {
  return context?.applyType === 'IQC';
}

export function isUniversalIqcStandard(row: any, context?: StandardMenuContext) {
  return (
    (context?.applyType === 'IQC' || row?.applyType === 'IQC') &&
    !row?.materialCode &&
    !row?.materialName
  );
}

function isIqcMaterialScope(values: any, context?: StandardMenuContext) {
  return (
    isIqcMaterialOnlyContext(context) && values?.iqcScopeType !== 'UNIVERSAL'
  );
}

function isFixedFullScopeContext(context?: StandardMenuContext) {
  return (
    context?.applyType === 'FAI' ||
    context?.applyType === 'FQC'
  );
}

function isGlueBoardFaiContext(context?: StandardMenuContext) {
  return context?.applyType === 'GLUE_BOARD_FAI';
}

function isOqcOptionalDimensionContext(context?: StandardMenuContext) {
  return context?.applyType === 'OQC';
}

const STANDARD_CONTEXTS: Record<string, StandardMenuContext> = {
  IQC: {
    applyType: 'IQC',
    title: '进料检验标准定义',
    permissionPrefix: 'mes:quality-standard:iqc',
    processVisible: false,
    defaultApplyScope: [],
  },
  FAI: {
    applyType: 'FAI',
    title: '过程首检检验标准定义',
    permissionPrefix: 'mes:quality-standard:fai',
    processVisible: true,
    defaultApplyScope: [],
  },
  GLUE_BOARD_FAI: {
    applyType: 'GLUE_BOARD_FAI',
    title: '胶板检验标准定义',
    permissionPrefix: 'mes:quality-standard:glue-board-fai',
    processVisible: false,
    defaultApplyScope: [],
  },
  IPQC: {
    applyType: 'IPQC',
    title: '过程检验标准定义',
    permissionPrefix: 'mes:quality-standard:ipqc',
    processVisible: true,
    defaultApplyScope: [],
  },
  FQC: {
    applyType: 'FQC',
    title: '成品检验标准定义',
    permissionPrefix: 'mes:quality-standard:fqc',
    processVisible: true,
    defaultApplyScope: [],
  },
  OQC: {
    applyType: 'OQC',
    title: '出货检验标准定义',
    permissionPrefix: 'mes:quality-standard:oqc',
    processVisible: true,
    defaultApplyScope: [],
  },
};

export function resolveStandardMenuContext(
  route: RouteLocationNormalizedLoaded,
): StandardMenuContext | undefined {
  const source = `${String(route.name || '')}/${route.path}`.toLowerCase();
  if (source.includes('standard/iqc') || source.includes('standardiqc')) {
    return STANDARD_CONTEXTS.IQC;
  }
  if (source.includes('standard/fai') || source.includes('standardfai')) {
    return STANDARD_CONTEXTS.FAI;
  }
  if (
    source.includes('standard/glue-board-fai') ||
    source.includes('standardglueboardfai')
  ) {
    return STANDARD_CONTEXTS.GLUE_BOARD_FAI;
  }
  if (source.includes('standard/ipqc') || source.includes('standardipqc')) {
    return STANDARD_CONTEXTS.IPQC;
  }
  if (source.includes('standard/fqc') || source.includes('standardfqc')) {
    return STANDARD_CONTEXTS.FQC;
  }
  if (source.includes('standard/oqc') || source.includes('standardoqc')) {
    return STANDARD_CONTEXTS.OQC;
  }
  return undefined;
}

function isScopeEnabled(values: any, scope: 'MATERIAL' | 'PROCESS') {
  const scopes = Array.isArray(values?.applyScope)
    ? values.applyScope
    : ['MATERIAL', 'PROCESS'];
  return scopes.includes(scope);
}

export function useGridFormSchema(
  context?: StandardMenuContext,
): VbenFormSchema[] {
  const schema: VbenFormSchema[] = [
    {
      fieldName: 'standardName',
      label: '标准名称',
      component: 'Input',
      componentProps: { placeholder: '请输入标准名称/编号' },
    },
    {
      fieldName: 'materialCode',
      label: '关联物料',
      component: 'Input',
      componentProps: { placeholder: '请输入物料编码' },
      dependencies: {
        show: () => !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'glueBoardModel',
      label: '胶板型号',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: GLUE_BOARD_MODEL_OPTIONS,
        placeholder: '请选择胶板型号',
      },
      dependencies: {
        show: () => isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'productModelKeyword',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入型号编码/名称' },
      dependencies: {
        show: () =>
          !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'processKeyword',
      label: '工序',
      component: 'Input',
      componentProps: { placeholder: '请输入工序' },
      dependencies: {
        show: () =>
          context?.processVisible !== false && !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'applyType',
      label: '检验环节',
      component: 'Select',
      componentProps: {
        options: [
          { label: '进料检验 (IQC)', value: 'IQC' },
          { label: '首件检验 (FAI)', value: 'FAI' },
          { label: '胶板检验 (GLUE_BOARD_FAI)', value: 'GLUE_BOARD_FAI' },
          { label: '过程检验 (IPQC)', value: 'IPQC' },
          { label: '成品检验 (FQC)', value: 'FQC' },
          { label: '出货检验 (OQC)', value: 'OQC' },
        ],
      },
      dependencies: {
        show: () => !context,
        triggerFields: [''],
      },
    },
  ];
  return schema;
}

export function useGridColumns(
  context?: StandardMenuContext,
): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'standardNo', title: '标准编号', width: 150 },
    { field: 'standardName', title: '标准名称', minWidth: 150 },
    {
      field: 'glueBoardModel',
      title: '胶板型号',
      width: 140,
      visible: isGlueBoardFaiContext(context),
    },
    {
      field: 'materialCode',
      title: '物料编码',
      width: 140,
      visible: !isGlueBoardFaiContext(context),
      formatter: ({ cellValue, row }: any) =>
        isUniversalIqcStandard(row, context) ? '全部物料' : cellValue || '-',
    },
    {
      field: 'materialName',
      title: '物料品名',
      width: 180,
      visible: !isGlueBoardFaiContext(context),
      formatter: ({ cellValue, row }: any) =>
        isUniversalIqcStandard(row, context) ? '通用标准' : cellValue || '-',
    },
    {
      field: 'iqcScope',
      title: '适用范围',
      width: 110,
      visible: isIqcMaterialOnlyContext(context),
      slots: { default: 'iqcScope' },
    },
    {
      field: 'productModelCode',
      title: '产品型号',
      width: 150,
      visible:
        !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
    },
    {
      field: 'productModelName',
      title: '产品型号名称',
      width: 180,
      visible:
        !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
    },
    {
      field: 'prodTypeName',
      title: '生产类型',
      width: 120,
      visible:
        !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
      formatter: ({ cellValue, row }: any) => cellValue || row?.prodType || '-',
    },
    {
      field: 'processName',
      title: '工序',
      width: 150,
      visible:
        context?.processVisible !== false && !isGlueBoardFaiContext(context),
    },
    { field: 'version', title: '版本号', width: 100, align: 'center' },
    {
      field: 'applyType',
      title: '适用环节',
      width: 120,
      visible: !context,
      slots: { default: 'applyType' },
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    {
      field: 'auditStatus',
      title: '审核状态',
      width: 100,
      slots: { default: 'auditStatus' },
    },
    { field: 'auditorName', title: '审核人', width: 120 },
    { field: 'auditTime', title: '审核时间', width: 170 },
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 250,
      slots: { default: 'actions' },
    },
  ];
}

export function useFormSchema(
  formType = '',
  context?: StandardMenuContext,
): VbenFormSchema[] {
  const schema: any[] = [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
    },
    {
      fieldName: 'materialId',
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
    },
    {
      fieldName: 'processId',
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
    },
    {
      fieldName: 'productModelId',
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
    },
    {
      fieldName: 'prodType',
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
    },
    {
      fieldName: 'applyScope',
      label: '关联维度',
      component: 'CheckboxGroup',
      defaultValue: context?.defaultApplyScope || ['MATERIAL', 'PROCESS'],
      help: '可只按物料编码、只按工序，或同时限定物料与工序',
      componentProps: {
        options: [
          { label: '物料编码', value: 'MATERIAL' },
          { label: '工序', value: 'PROCESS' },
        ],
      },
      dependencies: {
        show: () =>
          !isIqcMaterialOnlyContext(context) &&
          !isFixedFullScopeContext(context) &&
          !isOqcOptionalDimensionContext(context) &&
          !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'iqcScopeType',
      label: '适用范围',
      component: 'RadioGroup',
      defaultValue: 'MATERIAL',
      help: '物料专用需选择物料；通用标准不限定物料，适用于全部进料送检物料',
      componentProps: {
        buttonStyle: 'solid',
        optionType: 'button',
        options: [
          { label: '物料专用', value: 'MATERIAL' },
          { label: '通用标准', value: 'UNIVERSAL' },
        ],
      },
      dependencies: {
        show: () => isIqcMaterialOnlyContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'standardNo',
      label: '标准编号',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入标准编号' },
    },
    {
      fieldName: 'standardName',
      label: '标准名称',
      component: 'Input',
      rules: 'required',
    },
    {
      fieldName: 'version',
      label: '版本号',
      component: 'Input',
      defaultValue: 'A/1',
      rules: 'required',
    },
    {
      fieldName: 'glueBoardModel',
      label: '胶板型号',
      component: 'Select',
      rules: 'required',
      componentProps: {
        allowClear: true,
        options: GLUE_BOARD_MODEL_OPTIONS,
        placeholder: '请选择胶板型号',
      },
      dependencies: {
        show: () => isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'materialCode',
      label: '关联物料编码',
      component: 'Input',
      slot: 'materialCode',
      help: '支持输入物料编码或名称联想选择，也可点击搜索图标打开物料选择弹窗',
      dependencies: {
        triggerFields: ['applyScope', 'iqcScopeType'],
        show: (values: any) =>
          !isGlueBoardFaiContext(context) &&
          (isIqcMaterialScope(values, context) ||
            isFixedFullScopeContext(context) ||
            isOqcOptionalDimensionContext(context) ||
            isScopeEnabled(values, 'MATERIAL')),
      },
    },
    {
      fieldName: 'materialName',
      label: '物料名称',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: {
        triggerFields: ['applyScope', 'iqcScopeType'],
        show: (values: any) =>
          !isGlueBoardFaiContext(context) &&
          (isIqcMaterialScope(values, context) ||
            isFixedFullScopeContext(context) ||
            isOqcOptionalDimensionContext(context) ||
            isScopeEnabled(values, 'MATERIAL')),
      },
    },
    {
      fieldName: 'specification',
      label: '规格型号',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: {
        triggerFields: ['applyScope', 'iqcScopeType'],
        show: (values: any) =>
          !isGlueBoardFaiContext(context) &&
          (isIqcMaterialScope(values, context) ||
            isFixedFullScopeContext(context) ||
            isOqcOptionalDimensionContext(context) ||
            isScopeEnabled(values, 'MATERIAL')),
      },
    },
    {
      fieldName: 'productModelCode',
      label: '产品型号编码',
      component: 'Input',
      slot: 'productModelCode',
      help: '支持输入产品型号编码或名称联想选择，也可点击搜索图标打开产品型号选择弹窗',
      dependencies: {
        show: () =>
          !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'productModelName',
      label: '产品型号名称',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: {
        show: () =>
          !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'prodTypeName',
      label: '生产类型',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: {
        show: () =>
          !isIqcMaterialOnlyContext(context) && !isGlueBoardFaiContext(context),
        triggerFields: [''],
      },
    },
    {
      fieldName: 'processCode',
      label: '关联工序',
      component: 'Input',
      slot: 'processCode',
      help: '选择工序时，工作中心编码回填到关联工序，数据来源为生产管理 > 基础资料 > 工作中心',
      dependencies: {
        triggerFields: ['applyScope'],
        show: (values: any) =>
          context?.processVisible !== false &&
          (isFixedFullScopeContext(context) ||
            isOqcOptionalDimensionContext(context) ||
            isScopeEnabled(values, 'PROCESS')),
      },
    },
    {
      fieldName: 'processName',
      label: '工序',
      component: 'Input',
      componentProps: { disabled: true },
      help: '选择工序时，工序名称回填到此字段',
      dependencies: {
        triggerFields: ['applyScope'],
        show: (values: any) =>
          context?.processVisible !== false &&
          (isFixedFullScopeContext(context) ||
            isOqcOptionalDimensionContext(context) ||
            isScopeEnabled(values, 'PROCESS')),
      },
    },
    {
      fieldName: 'applyType',
      label: '适用环节',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: [
          { label: 'IQC-进料检验', value: 'IQC' },
          { label: 'FAI-首件检验', value: 'FAI' },
          { label: 'GLUE_BOARD_FAI-胶板检验', value: 'GLUE_BOARD_FAI' },
          { label: 'IPQC-过程抽检', value: 'IPQC' },
          { label: 'FQC-成品检验', value: 'FQC' },
          { label: 'OQC-出货检验', value: 'OQC' },
        ],
      },
      dependencies: {
        show: () => !context,
        triggerFields: [''],
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
    },
    {
      fieldName: 'auditorName',
      label: '审核人',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: { show: () => formType === 'detail', triggerFields: [''] },
    },
    {
      fieldName: 'auditTime',
      label: '审核时间',
      component: 'Input',
      componentProps: { disabled: true },
      dependencies: { show: () => formType === 'detail', triggerFields: [''] },
    },
    {
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
    },
  ];
  return schema;
}
