import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'workOrderNo', label: '工单号', component: 'Input', componentProps: { placeholder: '支持模糊查询' } },
    { fieldName: 'trackMode', label: '报工模式', component: 'Select',
      componentProps: { options: [{ label: '单件条码', value: 'PIECE' }, { label: '批量报数', value: 'BATCH' }] }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 60, align: 'center' },
    { field: 'trackNo', title: '过站流水号', width: 160 },
    { field: 'workOrderNo', title: '执行工单', width: 150 },
    { field: 'productName', title: '加工产品', minWidth: 180 },
    { field: 'workstation', title: '工作站', width: 150 },
    { field: 'operator', title: '操作人', width: 120, align: 'center' },
    { field: 'trackMode', title: '报工模式', width: 100, align: 'center', slots: { default: 'trackMode' } },
    { field: 'qty', title: '报工数量', width: 100, align: 'right', formatter: ({ cellValue }) => `${cellValue} PCS` },
    { field: 'trackTime', title: '过站时间', width: 160 },
    { title: '操作', field: 'action', fixed: 'right', width: 120, align: 'center', slots: { default: 'actions' } },
  ];
}

export function useFormSchema(): FormSchema[] {
  return [
    { fieldName: 'trackNo', label: '过站流水号', component: 'Input' },
    { fieldName: 'workOrderNo', label: '关联工单', component: 'Input' },
    { fieldName: 'productName', label: '产品名称', component: 'Input' },
    { fieldName: 'workstation', label: '作业工位', component: 'Input' },
    { fieldName: 'operator', label: '报工人', component: 'Input' },
    { fieldName: 'trackTime', label: '过站时间', component: 'Input' },
    { fieldName: 'trackMode', label: '报工模式', component: 'Select',
      componentProps: { options: [{ label: '单件一物一码', value: 'PIECE' }, { label: '批量无码', value: 'BATCH' }] }
    },
    { fieldName: 'qty', label: '总数量', component: 'Input', componentProps: { addonAfter: 'PCS' } },
  ];
}
