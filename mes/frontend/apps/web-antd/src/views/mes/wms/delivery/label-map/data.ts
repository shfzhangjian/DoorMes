import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

// ==========================================
// 1. 置换记录搜索表单
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'clientName',
      label: '目标客户',
      component: 'Select',
      componentProps: {
        options: [
          { label: '宁德时代 (CATL)', value: 'CATL' },
          { label: '比亚迪 (BYD)', value: 'BYD' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'clientSn',
      label: '客户外箱码 (新)',
      component: 'Input',
      componentProps: { placeholder: '精准追溯客户条码', allowClear: true },
    },
    {
      fieldName: 'internalSn',
      label: '工厂内部码 (旧)',
      component: 'Input',
      componentProps: { placeholder: '精准追溯内部条码', allowClear: true },
    },
    {
      fieldName: 'createTime',
      label: '置换时间',
      component: 'RangePicker',
      componentProps: { ...getRangePickerDefaultProps(), allowClear: true },
    },
  ];
}

// ==========================================
// 2. 置换记录表格列定义
// ==========================================
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'clientName', title: '目标客户', minWidth: 140, slots: { default: 'clientName' } },
    { field: 'ruleName', title: '应用转换规则', minWidth: 160 },
    { field: 'internalSn', title: '工厂内部原条码 (Old)', minWidth: 180, slots: { default: 'internalSn' } },
    { field: 'clientSn', title: '客户专属新条码 (New)', minWidth: 200, slots: { default: 'clientSn' } },
    { field: 'productName', title: '对应物料', minWidth: 150 },
    { field: 'operator', title: '置换操作人', minWidth: 100, align: 'center' },
    { field: 'createTime', title: '置换与打印时间', minWidth: 160, align: 'center' },
    { field: 'printStatus', title: '贴签状态', minWidth: 100, align: 'center', slots: { default: 'printStatus' } },
  ];
}
