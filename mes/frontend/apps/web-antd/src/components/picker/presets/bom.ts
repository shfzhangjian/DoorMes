import { getBomPage } from '#/api/mes/hc/bom';
import type { MesHcBomApi } from '#/api/mes/hc/bom';

import type { PickerEntityConfig } from '../types';

export const bomPickerConfig: PickerEntityConfig<MesHcBomApi.Bom> = {
  entityKey: 'bom',
  title: '选择领料单',
  tableTitle: '领料列表',
  modalWidth: 1180,
  inlinePanelWidth: 520,
  queryFields: [
    { field: 'bomCode', label: '领料编码', placeholder: '请输入领料编码' },
    { field: 'bomName', label: '领料名称', placeholder: '请输入领料名称' },
  ],
  columns: [
    { field: 'bomCode',  title: '领料编码', minWidth: 220 },
    { field: 'bomName',  title: '领料名称', minWidth: 260 },
    { field: 'versionNo', title: '版本号',  width: 120, align: 'center' },
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
    const res = await getBomPage({
      pageNo:  params.pageNo,
      pageSize: params.pageSize,
      bomCode: params.filters?.bomCode || undefined,
      bomName: params.filters?.bomName || keyword,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id:    row.id,
    code:  row.bomCode ?? '',
    name:  row.bomName ?? '',
    label: row.bomCode ?? '',
    status: row.status,
    extra: { versionNo: row.versionNo },
    raw: row as any,
  }),
};
