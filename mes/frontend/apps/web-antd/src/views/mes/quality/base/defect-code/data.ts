import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '名称/代码',
      component: 'Input',
      componentProps: { placeholder: '搜索缺陷名称或代码', allowClear: true },
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    {
      field: 'name',
      title: '缺陷名称/分类',
      minWidth: 200,
      treeNode: true,
      align: 'left',
    },
    { field: 'code', title: '代码', width: 160 },
    {
      field: 'type',
      title: '类型',
      width: 100,
      slots: { default: 'type' },
    },
    {
      field: 'level',
      title: '严重等级',
      width: 120,
      slots: { default: 'level' },
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    { field: 'remark', title: '备注', minWidth: 150 },
    {
      title: '操作',
      field: 'action',
      fixed: 'right',
      width: 280,
      slots: { default: 'actions' },
    },
  ];
}

export function useFormSchema(
  onTypeChange?: (value: 'CATEGORY' | 'ITEM') => void,
): VbenFormSchema[] {
  return [
    {
      fieldName: 'parentId',
      label: '上级分类',
      component: 'TreeSelect', // 使用树形下拉
      defaultValue: 0,
      componentProps: {
        treeData: [], // 将在表单弹窗内动态赋值
        fieldNames: { label: 'name', value: 'id' },
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'type',
      label: '节点类型',
      component: 'RadioGroup',
      defaultValue: 'ITEM',
      rules: 'required',
      componentProps: {
        options: [
          { label: '缺陷大类', value: 'CATEGORY' },
          { label: '具体缺陷项', value: 'ITEM' },
        ],
        onChange: (event: any) => {
          onTypeChange?.(event?.target?.value ?? event);
        },
      },
    },
    {
      fieldName: 'name',
      label: '缺陷名称',
      component: 'Input',
      rules: 'required',
    },
    {
      fieldName: 'code',
      label: '缺陷代码',
      component: 'Input',
      rules: 'required',
    },
    {
      fieldName: 'level',
      label: '严重等级',
      component: 'Select',
      dependencies: {
        triggerFields: ['type'],
        show: (values: any) => values.type === 'ITEM', // 仅具体缺陷项需要选择等级
      },
      componentProps: {
        options: [
          { label: '轻微 (MINOR)', value: 'MINOR' },
          { label: '一般 (MAJOR)', value: 'MAJOR' },
          { label: '严重 (CRITICAL)', value: 'CRITICAL' },
        ],
      },
    },
    {
      fieldName: 'referencePicUrls',
      label: '参考缺陷图片',
      component: 'ImageUpload',
      dependencies: {
        triggerFields: ['type'],
        show: (values: any) => values.type === 'ITEM',
      },
      componentProps: {
        multiple: true,
        maxNumber: 5,
        maxSize: 5,
        directory: 'mes/quality/defect-code',
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
      fieldName: 'remark',
      label: '备注说明',
      component: 'Textarea',
    },
  ];
}
