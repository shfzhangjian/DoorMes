import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workstation', label: '交接工位', component: 'Input', componentProps: { placeholder: '支持模糊查询' } },
    { fieldName: 'status', label: '交接状态', component: 'Select',
      componentProps: {
        options: [
          { label: '正常完成', value: 'COMPLETED' },
          { label: '异常报备', value: 'ABNORMAL' },
        ]
      }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 60, align: 'center' },
    { field: 'handoverNo', title: '交接单号', width: 160 },
    { field: 'workstation', title: '交接工位/产线', minWidth: 180 },
    { field: 'shiftName', title: '交接班次', width: 130, align: 'center' },
    { field: 'handoverUser', title: '交班人', width: 120, align: 'center' },
    { field: 'takeoverUser', title: '接班人', width: 120, align: 'center' },
    { field: 'wipQty', title: '结存数量', width: 100, align: 'right', formatter: ({ cellValue }) => `${cellValue} 件` },
    { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'createTime', title: '交接完成时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 100, align: 'center', slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'handoverNo', label: '交接单号', component: 'Input' },
    { fieldName: 'workstation', label: '交接工位', component: 'Input' },
    { fieldName: 'shiftName', label: '交接班次', component: 'Input' },
    { fieldName: 'handoverUser', label: '交班人(原)', component: 'Input' },
    { fieldName: 'takeoverUser', label: '接班人(新)', component: 'Input' },
    { fieldName: 'wipQty', label: '实物结存数量', component: 'Input', componentProps: { addonAfter: 'PCS/件' } },
    { fieldName: 'createTime', label: '交接完成时间', component: 'Input' },
    // 占位，为了对齐三列布局
    { fieldName: 'placeholder', label: '', component: 'Input', componentProps: { style: { display: 'none' } } },
    { fieldName: 'remark', label: '遗留异常事项', component: 'Textarea' },
  ];
}
