import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workOrderNo', label: '工单编号', component: 'Input' },
    {
      fieldName: 'result',
      label: '判定结果',
      component: 'Select',
      componentProps: {
        options: [
          { label: '合格', value: 'PASS' },
          { label: '异常', value: 'FAIL' },
        ],
      },
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 60, align: 'center' },
    { field: 'workOrderNo', title: '工单编号', width: 160 },
    { field: 'productName', title: '产品名称', minWidth: 180 },
    { field: 'paramName', title: '采集项', width: 120 },
    { field: 'standardRange', title: '工艺标准', width: 140 },
    {
      field: 'actualValue',
      title: '实测值',
      width: 120,
      slots: { default: 'actualValue' },
    },
    {
      field: 'result',
      title: '结果',
      width: 100,
      align: 'center',
      slots: { default: 'result' },
    },
    { field: 'collectTime', title: '采集时间', width: 160 },
    { field: 'operator', title: '操作人', width: 100 },
    { title: '操作', field: 'action', fixed: 'right', width: 100, slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workOrderNo', label: '工单编号', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'equipmentCode', label: '采集设备', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'productName', label: '产品名称', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'paramName', label: '参数名称', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'standardRange', label: '标准范围', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'actualValue', label: '实际数值', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'result', label: '判定结果', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'collectTime', label: '采集时间', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'operator', label: '采集人员', component: 'Input', componentProps: { disabled: true } },
  ];
}
