import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesDeviceCategoryApi } from '#/api/mes/resource/device/category';

import { DEVICE_CATEGORY_STATUS_OPTIONS, optionLabel } from '../shared';

export function useFormSchema(
  parentOptions: Array<{ label?: string; value?: number }> = [],
  formType = '',
): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'id',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        disabled: isDetail,
        options: [{ label: '无上级分类', value: 0 }, ...parentOptions],
      },
      defaultValue: 0,
      fieldName: 'parentId',
      label: '上级分类',
    },
    {
      component: 'Input',
      componentProps: { disabled: isDetail, placeholder: '如 MIXER' },
      fieldName: 'categoryCode',
      label: '分类编码',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { disabled: isDetail, placeholder: '如 搅拌设备' },
      fieldName: 'categoryName',
      label: '分类名称',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        disabled: isDetail,
        options: DEVICE_CATEGORY_STATUS_OPTIONS,
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', disabled: isDetail, min: 0, precision: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: '排序',
    },
    {
      component: 'Textarea',
      componentProps: { disabled: isDetail, placeholder: '分类说明', rows: 3 },
      fieldName: 'description',
      formItemClass: 'col-span-2',
      label: '分类说明',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'categoryCode',
      label: '分类编码',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'categoryName',
      label: '分类名称',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesDeviceCategoryApi.Category>['columns'] {
  return [
    { align: 'left', field: 'categoryCode', minWidth: 160, title: '分类编码' },
    { align: 'left', field: 'categoryName', minWidth: 200, title: '分类名称', treeNode: true },
    {
      align: 'center',
      field: 'status',
      formatter: ({ cellValue }) => optionLabel(DEVICE_CATEGORY_STATUS_OPTIONS, cellValue),
      title: '状态',
      width: 90,
    },
    { align: 'center', field: 'sort', title: '排序', width: 80 },
    { field: 'description', minWidth: 240, title: '分类说明' },
    { fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 180 },
  ];
}
