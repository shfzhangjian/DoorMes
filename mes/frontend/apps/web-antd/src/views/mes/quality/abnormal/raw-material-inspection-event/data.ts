import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { getRangePickerDefaultProps } from '#/utils';

export const RAW_MATERIAL_NCR_STATUS_OPTIONS = [
  { label: '未生成NCR', value: 'PENDING' },
  { label: '已生成', value: 'GENERATED' },
] as const;

export function useGridFormSchema(advanced = false): FormSchema[] {
  const schema: FormSchema[] = [
    {
      fieldName: 'inspectionNo',
      label: '检验单号',
      component: 'Input',
      componentProps: { placeholder: '请输入IQC单号' },
    },
    {
      fieldName: 'supplierName',
      label: '供应商',
      component: 'Input',
      componentProps: { placeholder: '请输入供应商' },
    },
    {
      fieldName: 'materialCode',
      label: '物料编码',
      component: 'Input',
      componentProps: { placeholder: '请输入物料编码' },
    },
  ];
  if (advanced) {
    schema.push(
      {
        fieldName: 'materialName',
        label: '物料名称',
        component: 'Input',
        componentProps: { placeholder: '请输入物料名称' },
      },
      {
        fieldName: 'lotNo',
        label: '批号',
        component: 'Input',
        componentProps: { placeholder: '请输入批号' },
      },
      {
        fieldName: 'inspectionTime',
        label: '检验时间',
        component: 'RangePicker',
        componentProps: { ...getRangePickerDefaultProps() },
      },
    );
  }
  return schema;
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 46, align: 'center', fixed: 'left' },
    { type: 'seq', width: 52, align: 'center', fixed: 'left' },
    {
      field: 'inspectionNo',
      title: '进料检验单号',
      minWidth: 180,
      fixed: 'left',
      slots: { default: 'inspectionNo' },
    },
    {
      field: 'inspectionTypeName',
      title: '检验类型',
      width: 130,
      align: 'center',
      slots: { default: 'inspectionTypeName' },
    },
    { field: 'supplierName', title: '供应商', minWidth: 180 },
    {
      field: 'materialName',
      title: '物料',
      minWidth: 230,
      slots: { default: 'materialName' },
    },
    { field: 'lotNo', title: '批号', minWidth: 150 },
    {
      field: 'quantity',
      title: '检验数量',
      width: 110,
      align: 'right',
      slots: { default: 'quantity' },
    },
    { field: 'qaInspectorName', title: '确认人', width: 110 },
    {
      field: 'inspectionTime',
      title: '检验时间',
      width: 170,
      align: 'center',
    },
    {
      field: 'abnormalSummary',
      title: '异常摘要',
      minWidth: 360,
      slots: { default: 'abnormalSummary' },
    },
    {
      field: 'ncrNo',
      title: 'NCR单号',
      minWidth: 170,
      slots: { default: 'ncrNo' },
    },
    {
      field: 'ncrStatus',
      title: 'NCR状态',
      width: 110,
      align: 'center',
      slots: { default: 'ncrStatus' },
    },
    {
      field: 'rejectNextInspectionNo',
      title: '驳回单号',
      minWidth: 180,
      slots: { default: 'rejectNextInspectionNo' },
    },
    {
      field: 'recheckStatus',
      title: '复检状态',
      width: 110,
      align: 'center',
      slots: { default: 'recheckStatus' },
    },
    {
      title: '操作',
      width: 156,
      align: 'center',
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
