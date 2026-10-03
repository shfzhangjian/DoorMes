import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProductionReportApi } from '#/api/mes/hc/production-report';

import { PLAN_STATUS_META, PLAN_STATUS_OPTIONS } from '../plan-order/data';

export const STATUS_OPTIONS = [
  { label: '全部状态', value: '' },
  ...PLAN_STATUS_OPTIONS,
];

export const PROCESS_STAGES = [
  { code: 'FORMULA', label: '配料' },
  { code: 'WET', label: '湿法' },
  { code: 'GRINDING', label: '磨皮' },
  { code: 'ADHESIVE1', label: '粘胶1' },
  { code: 'SLITTING', label: '分切' },
  { code: 'PRESS_SLOT', label: '压槽' },
  { code: 'ADHESIVE2', label: '粘胶2' },
  { code: 'CUT_ROUND', label: '裁切' },
];

export function planStatusMeta(status?: string) {
  return (
    PLAN_STATUS_META[String(status || '').toUpperCase()] || {
      color: 'default',
      label: status || '-',
    }
  );
}

export function useGridColumns(): VxeTableGridOptions<MesHcProductionReportApi.OverviewRow>['columns'] {
  return [
    {
      field: 'planNo',
      title: '计划 / 批号',
      width: 210,
      fixed: 'left',
      align: 'left',
      showOverflow: false,
      slots: { default: 'planNo' },
    },
    {
      field: 'actualModelCode',
      title: '产品型号 / 料号',
      width: 175,
      align: 'left',
      showOverflow: false,
      slots: { default: 'model' },
    },
    {
      field: 'planStatus',
      title: '计划状态',
      width: 105,
      slots: { default: 'planStatus' },
    },
    ...PROCESS_STAGES.map((stage) => ({
      field: `stages.${stage.code}`,
      title: stage.label,
      minWidth: 130,
      showOverflow: false,
      slots: { default: stage.code },
    })),
    {
      field: 'actions',
      title: '查看',
      width: 100,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
