import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workstation', label: '交接工位', component: 'Input', componentProps: { placeholder: '支持模糊查询工位' } },
    { fieldName: 'handoverUser', label: '交班人', component: 'Input', componentProps: { placeholder: '交班人姓名' } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'handoverNo', title: '交接单号', width: 160 },
    { field: 'workstation', title: '交接工位/产线', minWidth: 180 },
    { field: 'shiftName', title: '交接班次', width: 140, align: 'center' },
    { field: 'handoverUser', title: '交班人', width: 100, align: 'center' },
    { field: 'takeoverUser', title: '接班人', width: 100, align: 'center' },
    { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
    { field: 'handoverTime', title: '交接时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 200, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'handoverNo', label: '交接单号', component: 'Input', componentProps: { disabled: true, placeholder: '保存后系统自动生成' } },
    { fieldName: 'workstation', label: '交接工位', component: 'Input', rules: 'required' },
    { fieldName: 'shiftName', label: '交接班次', component: 'Input', rules: 'required', componentProps: { placeholder: '如: 早班 -> 中班' } },
    { fieldName: 'handoverUser', label: '交班人', component: 'Input', rules: 'required' },
    { fieldName: 'takeoverUser', label: '接班人', component: 'Input', rules: 'required' },
    { fieldName: 'status', label: '状态', component: 'Select', defaultValue: 'COMPLETED',
      componentProps: {
        options: [
          { label: '待接班', value: 'PENDING' },
          { label: '交接完成', value: 'COMPLETED' },
          { label: '异常报备', value: 'ABNORMAL' },
        ]
      }
    },
    { fieldName: 'remark', label: '交班备注', component: 'Textarea' },
  ];
}
