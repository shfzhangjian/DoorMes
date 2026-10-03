import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): FormSchema[] {
  return [
    {
      fieldName: 'dateRange',
      label: '生产日期',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps() }
    },
    {
      fieldName: 'workshopId',
      label: '生产车间',
      component: 'Select',
      defaultValue: 'W01', // 默认选定车间以规避跨量纲统计
      componentProps: {
        options: [{ label: '抛光材料一车间 (PCS)', value: 'W01' }, { label: '配液二车间 (KG)', value: 'W02' }],
        allowClear: false
      }
    },
    {
      fieldName: 'shiftId',
      label: '班别',
      component: 'Select',
      componentProps: {
        options: [{ label: '早班', value: 'S1' }, { label: '晚班', value: 'S2' }],
        allowClear: true
      }
    },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    { field: 'productionDate', title: '生产日期', width: 120, align: 'center', fixed: 'left' },
    { field: 'shiftName', title: '班别', width: 90, align: 'center' },
    { field: 'workOrderNo', title: '生产工单', minWidth: 160 },
    { field: 'itemName', title: '产品名称', minWidth: 180 },
    { field: 'plannedQty', title: '排产数量', width: 100, align: 'right' },
    { field: 'actualQty', title: '实际产出', width: 100, align: 'right', slots: { default: 'actualQty' } },
    { field: 'goodQty', title: '合格数量', width: 100, align: 'right' },
    { field: 'scrapQty', title: '报废数量', width: 100, align: 'right', slots: { default: 'scrapQty' } },
    { field: 'yieldRate', title: '合格率(%)', width: 100, align: 'right', slots: { default: 'yieldRate' } },
  ];
}
