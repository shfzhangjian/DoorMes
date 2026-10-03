import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

export function useListGridSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'deviceCode',
      label: '设备编号',
      component: 'Input',
      componentProps: { placeholder: '精确检索' },
    },
    {
      fieldName: 'deviceName',
      label: '设备名称',
      component: 'Input',
      componentProps: { placeholder: '模糊检索' },
    },
    {
      fieldName: 'maintType',
      label: '保养类型',
      component: 'Input',
      componentProps: { placeholder: '月度/季度/半年', allowClear: true },
    },
    {
      fieldName: 'published',
      label: '发布状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '已发布', value: true },
          { label: '未发布', value: false },
        ],
        allowClear: true,
      },
    },
  ];
}

export function useListGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40, fixed: 'left' },
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'deviceCode', title: '设备编号', width: 130 },
    { field: 'deviceName', title: '设备名称', minWidth: 160 },
    {
      field: 'itemGroup',
      title: '保养项目',
      width: 130,
      showOverflow: 'tooltip',
    },
    {
      field: 'itemName',
      title: '保养部位',
      minWidth: 160,
      showOverflow: 'tooltip',
    },
    {
      field: 'planPeriod',
      title: '计划周期',
      width: 130,
      align: 'center',
      formatter: ({ cellValue }) => {
        const match = cellValue?.match(/M0?(\d+)-W(\d+)/);
        return match ? `${match[1]}月 第${match[2]}周` : cellValue;
      },
    },
    { field: 'planStartDate', title: '提醒开始', width: 110, align: 'center' },
    { field: 'planEndDate', title: '逾期节点', width: 110, align: 'center' },
    { field: 'frequency', title: '保养周期', width: 100, align: 'center' },
    {
      field: 'maintType',
      title: '保养类型',
      width: 120,
      align: 'center',
      slots: { default: 'maintType' },
    },
    { field: 'sourceRule', title: '来源依据', minWidth: 150 },
    {
      field: 'published',
      title: '发布状态',
      width: 100,
      align: 'center',
      slots: { default: 'published' },
    },
    { field: 'generatedTaskNo', title: '生成工单', minWidth: 160 },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
