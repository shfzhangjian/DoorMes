// 文件路径：src/views/mes/cost/wage/attendance/data.ts

import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import type { FormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const STATUS_OPTIONS = [
  { label: '待核对', value: 0, color: 'processing' },
  { label: '已核对锁定', value: 1, color: 'success' },
];

export function useGridFormSchema(): FormSchema[] {
  return [
    { fieldName: 'attendDate', label: '考勤日期', component: 'DatePicker', componentProps: { valueFormat: 'YYYY-MM-DD', allowClear: true } },
    { fieldName: 'teamName', label: '班组名称', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'empName', label: '工号/姓名', component: 'Input', componentProps: { allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { allowClear: true, options: STATUS_OPTIONS } },
  ];
}

export function useGridColumns(): VxeGridProps['columns'] {
  return [
    { type: 'checkbox', width: 60, align: 'center' },
    { field: 'attendDate', title: '考勤日期', width: 110, align: 'center', fixed: 'left' },
    { field: 'teamName', title: '归属班组', width: 130 },
    { field: 'empName', title: '员工姓名', width: 120, cellRender: ({ cellValue }) => h('span', { class: 'font-semibold' }, cellValue) },
    { field: 'shiftName', title: '班次', width: 80, align: 'center' },

    // --- 原始打卡区 (系统读取只读) ---
    { title: '原始打卡记录', align: 'center', children: [
        { field: 'clockIn', title: '上班打卡', width: 100, align: 'center', cellRender: ({ cellValue }) => h('span', { class: 'text-gray-500 font-mono' }, cellValue || '--:--') },
        {
          field: 'clockOut', title: '下班打卡', width: 100, align: 'center',
          cellRender: ({ cellValue, row }) => {
            if (!cellValue) return h('span', { class: 'text-red-500 font-bold' }, '缺卡');
            return h('span', { class: 'text-gray-500 font-mono' }, cellValue);
          }
        },
      ]},

    // --- 结算工时区 (允许车间主任微调) ---
    { title: '有效结算工时 (H)', align: 'center', children: [
        { field: 'adjRegularHours', title: '正班工时', width: 120, align: 'center', slots: { default: 'adjRegularHours' } },
        { field: 'adjOvertimeHours', title: '加班工时', width: 120, align: 'center', slots: { default: 'adjOvertimeHours' } },
      ]},

    { field: 'nightShiftSubsidy', title: '夜班补贴(次)', width: 110, align: 'center', slots: { default: 'nightShiftSubsidy' } },
    { field: 'status', title: '核对状态', width: 120, align: 'center', slots: { default: 'status' } },
    { field: 'remark', title: '异常/备注', minWidth: 150, align: 'left', cellRender: ({ cellValue }) => h('span', { class: 'text-orange-500 text-xs' }, cellValue) },
  ];
}
