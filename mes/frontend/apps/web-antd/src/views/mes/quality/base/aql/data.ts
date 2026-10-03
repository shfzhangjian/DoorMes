import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'name', label: '方案名称/代码', component: 'Input', componentProps: { placeholder: '模糊搜索' } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 50, align: 'center' },
    { field: 'code', title: '方案代码', width: 150 },
    { field: 'name', title: '方案名称', minWidth: 200 },
    { field: 'standardType', title: '依据标准', width: 140 },
    { field: 'inspectLevel', title: '检验水平', width: 100, align: 'center' },
    { field: 'aqlValue', title: 'AQL值', width: 100, align: 'center' },
    { field: 'status', title: '状态', width: 100, slots: { default: 'status' }, align: 'center' },
    { field: 'remark', title: '备注', minWidth: 150 },
    { title: '操作', field: 'action', fixed: 'right', width: 200, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'code', label: '方案代码', component: 'Input', rules: 'required' },
    { fieldName: 'name', label: '方案名称', component: 'Input', rules: 'required' },
    {
      fieldName: 'standardType', label: '依据标准', component: 'Select', rules: 'required',
      componentProps: { options: [{ label: 'GB/T 2828.1', value: 'GB/T 2828.1' }, { label: 'MIL-STD-105E', value: 'MIL-STD-105E' }] }
    },
    {
      fieldName: 'inspectLevel', label: '检验水平', component: 'Select', rules: 'required',
      componentProps: { options: [{ label: 'II (一般)', value: 'II' }, { label: 'I (一般)', value: 'I' }, { label: 'III (一般)', value: 'III' }, { label: 'S-2 (特殊)', value: 'S-2' }] }
    },
    {
      fieldName: 'aqlValue', label: 'AQL值', component: 'Select', rules: 'required',
      componentProps: { options: [{ label: '0.65', value: '0.65' }, { label: '1.0', value: '1.0' }, { label: '1.5', value: '1.5' }, { label: '2.5', value: '2.5' }, { label: '4.0', value: '4.0' }] }
    },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', defaultValue: 1, componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] } },
    { fieldName: 'remark', label: '备注说明', component: 'Textarea' },
  ];
}
