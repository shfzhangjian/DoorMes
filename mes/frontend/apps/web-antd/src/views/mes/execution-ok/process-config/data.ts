import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'productName', label: '产品名称', component: 'Input' },
    { fieldName: 'paramName', label: '参数名称', component: 'Input' },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'productName', title: '适用产品', minWidth: 200 },
    { field: 'paramName', title: '参数名称', width: 140 },
    { field: 'range', title: '标准判定范围', width: 200, slots: { default: 'range' } },
    { field: 'isRequired', title: '必填', width: 80, align: 'center', slots: { default: 'isRequired' } },
    { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'status' } },
    { field: 'createTime', title: '创建时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 180, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'productName', label: '适用产品', component: 'Input', rules: 'required' },
    { fieldName: 'paramName', label: '参数名称', component: 'Input', rules: 'required' },
    { fieldName: 'paramUnit', label: '计量单位', component: 'Input', rules: 'required' },
    { fieldName: 'minValue', label: '下限(Min)', component: 'InputNumber', rules: 'required' },
    { fieldName: 'maxValue', label: '上限(Max)', component: 'InputNumber', rules: 'required' },
    {
      fieldName: 'isRequired', label: '是否必填', component: 'Select', defaultValue: 1,
      componentProps: { options: [{ label: '是', value: 1 }, { label: '否', value: 0 }] }
    },
    {
      fieldName: 'status', label: '状态', component: 'RadioGroup', defaultValue: 1,
      componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] }
    },
    { fieldName: 'remark', label: '备注说明', component: 'Textarea' },
  ];
}
