import { getWorkCenterLineLabel, getWorkCenterPage } from '#/api/mes/hc/workcenter';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import type { PickerEntityConfig } from '../types';

const lineCodeOptions = [
  { label: '白垫线', value: 'WHITE' },
  { label: '黑垫线', value: 'BLACK' },
];

export const workcenterPickerConfig: PickerEntityConfig<MesHcWorkCenterApi.WorkCenter> = {
  entityKey: 'workcenter',
  title: '选择工作中心',
  tableTitle: '工作中心列表',
  modalWidth: 1080,
  inlinePanelWidth: 620,
  queryFields: [
    { field: 'wcCode', label: '工作中心编码', placeholder: '请输入工作中心编码' },
    { field: 'wcName', label: '工作中心名称', placeholder: '请输入工作中心名称' },
    { field: 'processName', label: '工序', placeholder: '请输入工序名称' },
    {
      field: 'lineCode',
      label: '所属线路',
      type: 'select',
      options: lineCodeOptions,
      placeholder: '请选择所属线路',
    },
  ],
  columns: [
    { field: 'wcCode',        title: '工作中心编码', minWidth: 160 },
    { field: 'wcName',        title: '工作中心名称', minWidth: 200 },
    {
      field: 'processName',
      title: '工序',
      minWidth: 120,
      formatter: (v, row) => v || row?.processStage || '-',
    },
    {
      field: 'lineCode',
      title: '所属线路',
      width: 120,
      formatter: (v, row) => getWorkCenterLineLabel(v, row?.lineName),
    },
    {
      field: 'status',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: (v) =>
        ({ 0: '启用', 1: '停用' } as Record<number, string>)[Number(v)] ?? '-',
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getWorkCenterPage({
      pageNo:   params.pageNo,
      pageSize: params.pageSize,
      wcCode:   params.filters?.wcCode    || undefined,
      wcName:   params.filters?.wcName    || keyword,
      processName: params.filters?.processName || undefined,
      lineCode: params.filters?.lineCode  || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id:    row.id,
    code:  row.wcCode ?? '',
    name:  row.wcName ?? '',
    label: row.wcCode ?? '',
    status: row.status,
    extra: {
      processStage: row.processStage,
      processId:    row.processId,
      processCode:  row.processCode,
      processName:  row.processName,
      lineCode:     row.lineCode,
      lineName:     getWorkCenterLineLabel(row.lineCode, row.lineName),
      lineShortCode: row.lineShortCode,
      batchLineCode: row.batchLineCode,
    },
    raw: row as any,
  }),
};
