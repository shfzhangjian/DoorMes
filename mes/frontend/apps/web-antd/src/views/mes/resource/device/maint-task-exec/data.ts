import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'taskNo', label: '任务单号', component: 'Input', componentProps: { placeholder: '输入任务单号', allowClear: true } },
    { fieldName: 'deviceName', label: '保养设备', component: 'Input', componentProps: { placeholder: '设备名称模糊检索', allowClear: true } },
    { fieldName: 'maintType', label: '维保类型', component: 'Select', componentProps: { options: [{ label: '日常巡检', value: '日常巡检' }, { label: '一级保养', value: '一级保养' }, { label: '二级保养', value: '二级保养' }, { label: '三级大修', value: '三级大修' }], allowClear: true } },
    { fieldName: 'planDate', label: '计划日期', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'taskNo', title: '保养任务单号', minWidth: 150, slots: { default: 'taskNo' }, fixed: 'left' },
    { field: 'planPeriod', title: '计划排程周期', width: 130, align: 'center' },
    { field: 'deviceName', title: '执行保养设备', minWidth: 160 },
    { field: 'maintType', title: '维保类型', width: 100, align: 'center', slots: { default: 'maintType' } },
    { field: 'status', title: '执行状态', width: 100, align: 'center', slots: { default: 'status' } },
    { field: 'dueDate', title: '应检日期', width: 120, align: 'center' },
    { field: 'planDate', title: '要求完成日期', width: 120, align: 'center' },
    { field: 'actualDate', title: '实际完成时间', width: 160, align: 'center' },
    { field: 'executor', title: '执行人', width: 100 },
    { title: '操作', width: 120, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useOrderFormSchema(
  deviceOptions: Array<{ label: string; value: number }> = [],
  standardOptions: Array<{ label: string; value: number }> = [],
): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'deviceId',
      label: '保养设备',
      component: 'Select',
      rules: 'required',
      componentProps: { options: deviceOptions, showSearch: true, allowClear: true, optionFilterProp: 'label' },
    },
    {
      fieldName: 'standardId',
      label: '保养标准',
      component: 'Select',
      rules: 'required',
      componentProps: { options: standardOptions, showSearch: true, allowClear: true, optionFilterProp: 'label' },
    },
    { fieldName: 'planPeriod', label: '计划周期', component: 'Input', componentProps: { placeholder: '如 2026-07 / 2026-W30' } },
    { fieldName: 'executor', label: '执行人', component: 'Input', componentProps: { placeholder: '请输入执行人' } },
    {
      fieldName: 'planDate',
      label: '计划日期',
      component: 'DatePicker',
      rules: 'required',
      componentProps: { valueFormat: 'YYYY-MM-DD', class: 'w-full' },
    },
    {
      fieldName: 'planTime',
      label: '计划时间',
      component: 'DatePicker',
      componentProps: { valueFormat: 'YYYY-MM-DD HH:mm:ss', showTime: true, class: 'w-full' },
    },
    {
      fieldName: 'maintType',
      label: '保养类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '日常巡检', value: '日常巡检' },
          { label: '一级保养', value: '一级保养' },
          { label: '二级保养', value: '二级保养' },
          { label: '三级大修', value: '三级大修' },
        ],
      },
    },
    { fieldName: 'taskDesc', label: '任务说明', component: 'Textarea', componentProps: { rows: 3, placeholder: '请输入本次保养要求' }, formItemClass: 'col-span-2' },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { rows: 2, placeholder: '请输入备注' }, formItemClass: 'col-span-2' },
  ];
}

export function usePartGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'sort', title: '序号', width: 60, align: 'center', slots: { default: 'sort' } },
    { field: 'partName', title: '消耗备件名称', minWidth: 160, slots: { default: 'partName' } },
    { field: 'partCode', title: '备件编码', width: 130, slots: { default: 'partCode' } },
    { field: 'quantity', title: '数量', width: 100, slots: { default: 'quantity' } },
    { field: 'unit', title: '单位', width: 80, align: 'center', slots: { default: 'unit' } },
    { field: 'remark', title: '更换说明', minWidth: 150, slots: { default: 'remark' } },
    { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}

export function useExecItemGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'sort', title: '序号', width: 60, align: 'center' },
    { field: 'itemName', title: '标准项目', minWidth: 180 },
    { field: 'method', title: '作业方法', minWidth: 150 },
    { field: 'requirement', title: '合格标准', minWidth: 220 },
    { field: 'normal', title: '正常', width: 80, align: 'center', slots: { default: 'normal' } },
    { field: 'remark', title: '执行备注', minWidth: 220, slots: { default: 'remark' } },
  ];
}
