import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

type Option = { label: string; value: number | string };

// ==========================================
// 1. 名录检索表单
// ==========================================
export function useGridFormSchema(_scopeOptions: Option[] = []): VbenFormSchema[] {
  return [
    { fieldName: 'supplierInfo', label: '供应商名称/代码', component: 'Input', componentProps: { placeholder: '模糊检索供应商', allowClear: true } },
    { fieldName: 'materialCode', label: '物料代码', component: 'Input', componentProps: { placeholder: '请输入物料代码', allowClear: true } },
    {
      fieldName: 'level', label: '评定级别', component: 'Select',
      componentProps: {
        options: [
          { label: 'A级 (优秀战略)', value: 'A' },
          { label: 'B级 (良好稳定)', value: 'B' },
          { label: 'C级 (限期整改)', value: 'C' },
          { label: 'D级 (淘汰预警)', value: 'D' },
        ],
        allowClear: true,
      }
    },
    {
      fieldName: 'companyNature', label: '企业性质', component: 'Select',
      componentProps: { options: [{ label: '生产厂家(原厂)', value: 'MANUFACTURER' }, { label: '贸易/代理商', value: 'AGENT' }], allowClear: true }
    },
    {
      fieldName: 'materialGrade', label: '物料等级', component: 'Select',
      componentProps: {
        options: [
          { label: 'A级', value: 'A' },
          { label: 'B级', value: 'B' },
          { label: 'C级', value: 'C' },
          { label: 'D级', value: 'D' },
        ],
        allowClear: true,
      }
    },
  ];
}

// ==========================================
// 2. 核心名录列定义 (深度还原《05合格供应商名录》要求)
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'supplierCode', title: '供应商代码', minWidth: 120, slots: { default: 'supplierCode' }, fixed: 'left' },
    { field: 'supplierName', title: '供应商名称', minWidth: 220, slots: { default: 'supplierName' } },
    { field: 'usingDepartment', title: '使用部门', minWidth: 120 },
    { field: 'scopeName', title: '名录范围', minWidth: 150, slots: { default: 'scopeName' } },
    { field: 'level', title: '评定级别', minWidth: 100, align: 'center', slots: { default: 'level' } },
    { field: 'companyNature', title: '企业性质', minWidth: 120, align: 'center', slots: { default: 'companyNature' } },
    { field: 'originPlace', title: '产地', minWidth: 120 },
    { field: 'originalFactoryInfo', title: '原厂信息', minWidth: 180 },
    { field: 'providedProduct', title: '供应/协作内容', minWidth: 200, slots: { default: 'providedProduct' } },
    { field: 'materialCode', title: '物料代码', minWidth: 140 },
    { field: 'model', title: '型号', minWidth: 140 },
    { field: 'applicableProduct', title: '适用产品', minWidth: 160 },
    { field: 'importDate', title: '导入日期', minWidth: 120, align: 'center' },
    { field: 'materialGrade', title: '物料等级', minWidth: 100, align: 'center', slots: { default: 'materialGrade' } },
    { field: 'remark', title: '备注', minWidth: 160 },
    { field: 'status', title: '资源状态', minWidth: 100, align: 'center', slots: { default: 'status' } },
    { title: '操作', width: 140, fixed: 'right', slots: { default: 'actions' } },
  ];
}
