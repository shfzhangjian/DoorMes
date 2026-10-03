import { getRoutePage } from '#/api/mes/hc/route';
import type { MesHcRouteApi } from '#/api/mes/hc/route';

import type { PickerEntityConfig } from '../types';

export const routePickerConfig: PickerEntityConfig<MesHcRouteApi.Route> = {
  entityKey: 'route',
  title: '选择工艺路线',
  tableTitle: '工艺路线列表',
  modalWidth: 1180,
  inlinePanelWidth: 520,
  queryFields: [
    { field: 'routeCode', label: '工艺路线编码', placeholder: '请输入工艺路线编码' },
    { field: 'routeName', label: '工艺路线名称', placeholder: '请输入工艺路线名称' },
  ],
  columns: [
    { field: 'routeCode', title: '工艺路线编码', minWidth: 220 },
    { field: 'routeName', title: '工艺路线名称', minWidth: 260 },
    { field: 'versionNo', title: '版本号',        width: 120, align: 'center' },
    {
      field: 'status',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: (v) =>
        ({ 0: '草稿', 1: '启用', 2: '停用' } as Record<number, string>)[Number(v)] ?? '-',
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getRoutePage({
      pageNo:    params.pageNo,
      pageSize:  params.pageSize,
      routeCode: params.filters?.routeCode || undefined,
      routeName: params.filters?.routeName || keyword,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id:    row.id,
    code:  row.routeCode ?? '',
    name:  row.routeName ?? '',
    label: row.routeCode ?? '',
    status: row.status,
    extra: { versionNo: row.versionNo },
    raw: row as any,
  }),
};
