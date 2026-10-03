import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'ipqcNo', label: '巡检单号', component: 'Input', componentProps: { placeholder: '输入单号' } },
    { fieldName: 'machineCode', label: '生产机台', component: 'Input', componentProps: { placeholder: '机台号' } },
    { fieldName: 'workOrderNo', label: '生产工单', component: 'Input', componentProps: { placeholder: '工单号' } },
    { fieldName: 'judgment', label: '巡检判定', component: 'Select', componentProps: { options: [{label:'正常(OK)',value:'OK'},{label:'异常预警(NG)',value:'NG'}], allowClear: true } },
    { fieldName: 'inspectionTime', label: '巡检时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps() } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'seq', width: 50, align: 'center', fixed: 'left' },
    { field: 'ipqcNo', title: '过程巡检单号', width: 160, fixed: 'left', slots: { default: 'ipqcNo' } },
    { field: 'machineCode', title: '运行机台', width: 120, slots: { default: 'machineCode' } },
    { field: 'workOrderNo', title: '关联工单', width: 140 },
    { field: 'materialName', title: '在制产品', minWidth: 150 },
    { field: 'inspectionType', title: '巡检类型', width: 100, align: 'center', slots: { default: 'inspectionType' } },
    { field: 'inspector', title: '巡检人', width: 90, align: 'center' },
    { field: 'judgment', title: '判定结果', width: 90, align: 'center', slots: { default: 'judgment' } },
    { field: 'inspectionTime', title: '打卡时间', width: 150, align: 'center' },
    { title: '操作', field: 'action', fixed: 'right', width: 100, align: 'center', slots: { default: 'actions' } },
  ];
}
