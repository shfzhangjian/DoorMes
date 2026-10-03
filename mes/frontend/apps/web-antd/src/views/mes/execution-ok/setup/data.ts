import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'productName', label: '适用产品', component: 'Input', componentProps: { placeholder: '请输入产品名称' } },
    { fieldName: 'processCode', label: '适用工序', component: 'Select',
      componentProps: {
        options: [
          { label: '配料 (MIXING)', value: 'MIXING' },
          { label: '涂布 (COATING)', value: 'COATING' },
          { label: '分切 (SLITTING)', value: 'SLITTING' },
        ]
      }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'ruleNo', title: '规则编号', width: 160 },
    { field: 'productName', title: '适用产品名称', minWidth: 180 },
    { field: 'processCode', title: '适用工序', width: 140, slots: { default: 'processCode' } },
    { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
    { field: 'createTime', title: '创建时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 200, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'ruleNo', label: '规则编号', component: 'Input', componentProps: { disabled: true, placeholder: '保存后自动生成' } },
    { fieldName: 'productName', label: '适用产品', component: 'Input', rules: 'required' },
    { fieldName: 'processCode', label: '适用工序', component: 'Select', rules: 'required',
      componentProps: {
        options: [
          { label: 'MIXING - 配料', value: 'MIXING' },
          { label: 'COATING - 涂布', value: 'COATING' },
          { label: 'SLITTING - 分切', value: 'SLITTING' },
        ]
      }
    },
    { fieldName: 'status', label: '状态', component: 'RadioGroup', defaultValue: 1,
      componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] }
    },
    { fieldName: 'remark', label: '备注说明', component: 'Textarea' },
  ];
}
