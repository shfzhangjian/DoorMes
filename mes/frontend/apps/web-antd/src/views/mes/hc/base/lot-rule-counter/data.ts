import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

function formatDateTime(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-';
  const date = typeof value === 'number' ? new Date(value) : new Date(String(value));
  if (Number.isNaN(date.getTime())) return String(value);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mi = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'ruleCode',
      label: '规则编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入规则编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'counterType',
      label: '计数类型',
      component: 'Input',
      componentProps: {
        placeholder: '如 ANNUAL_BATCH',
        allowClear: true,
      },
    },
    {
      fieldName: 'bizDimensionKey',
      label: '业务维度',
      component: 'Input',
      componentProps: {
        placeholder: '如 2026',
        allowClear: true,
      },
    },
    {
      fieldName: 'counterKey',
      label: '计数键',
      component: 'Input',
      componentProps: {
        placeholder: '请输入计数键',
        allowClear: true,
      },
    },
    {
      fieldName: 'resetKey',
      label: '重置键',
      component: 'Input',
      componentProps: {
        placeholder: '请输入重置键',
        allowClear: true,
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<Record<string, any>>['columns'] {
  return [
    { type: 'checkbox', width: 48, align: 'center' },
    { field: 'ruleCode', title: '规则编码', minWidth: 180 },
    { field: 'ruleName', title: '规则名称', minWidth: 220 },
    { field: 'counterType', title: '计数类型', width: 150 },
    { field: 'bizDimensionKey', title: '业务维度', width: 140 },
    { field: 'counterKey', title: '计数键', minWidth: 260 },
    { field: 'resetKey', title: '重置键', minWidth: 120 },
    { field: 'currentSeq', title: '当前已使用计数', width: 140, align: 'center' },
    { field: 'lastLotNo', title: '最后批号', minWidth: 220 },
    { field: 'updateTime', title: '更新时间', width: 180, formatter: ({ cellValue }) => formatDateTime(cellValue) },
    {
      title: '操作',
      width: 280,
      fixed: 'right',
      align: 'center',
      slots: { default: 'actions' },
    },
  ];
}
