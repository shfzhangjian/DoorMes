import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

type PositionOption = { label?: string; value?: number };

export function useFormSchema(
  positionOptions: PositionOption[] = [],
): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'id',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: false,
        options: [{ label: '作为一级位置', value: 0 }, ...positionOptions],
        placeholder: '请选择所属位置',
        showSearch: true,
      },
      defaultValue: 0,
      fieldName: 'parentId',
      label: '所属位置',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入编码' },
      fieldName: 'categoryCode',
      label: '编码',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入位置或区域名称' },
      fieldName: 'categoryName',
      label: '位置/区域名称',
      rules: 'required',
    },
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      defaultValue: 1,
      fieldName: 'status',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: '排序',
    },
    {
      component: 'Textarea',
      componentProps: { placeholder: '请输入说明', rows: 3 },
      fieldName: 'description',
      formItemClass: 'col-span-2',
      label: '说明',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'categoryCode',
      label: '编码',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'categoryName',
      label: '位置/区域',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<QmsMeasureToolApi.Category>['columns'] {
  return [
    { align: 'left', field: 'categoryCode', minWidth: 160, title: '编码' },
    {
      align: 'left',
      field: 'categoryName',
      minWidth: 200,
      title: '位置/区域',
      treeNode: true,
    },
    { align: 'center', field: 'sort', title: '排序', width: 80 },
    { field: 'description', minWidth: 240, title: '说明' },
    {
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 170,
    },
  ];
}
