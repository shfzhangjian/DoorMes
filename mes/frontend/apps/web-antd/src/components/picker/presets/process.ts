import { getProcessPage } from '#/api/mes/base/process';
import type { MesProcessApi } from '#/api/mes/base/process';

import type { PickerEntityConfig } from '../types';

const processTypeOptions = [
  { label: '制造', value: 'manufacture' },
  { label: '检验', value: 'test' },
];

function formatProcessType(value?: string) {
  return processTypeOptions.find((item) => item.value === value)?.label ?? (value ? String(value) : '-');
}

export const processPickerConfig: PickerEntityConfig<MesProcessApi.Process> = {
  entityKey: 'process',
  title: '选择标准工序',
  tableTitle: '标准工序列表',
  modalWidth: 1080,
  inlinePanelWidth: 620,
  queryFields: [
    { field: 'name', label: '工序名称', placeholder: '请输入工序名称' },
    { field: 'code', label: '工序编码', placeholder: '请输入工序编码' },
    {
      field: 'processType',
      label: '工序类型',
      type: 'select',
      options: processTypeOptions,
      placeholder: '请选择工序类型',
      defaultHidden: true,
    },
  ],
  columns: [
    { field: 'code', title: '工序编码', minWidth: 150 },
    { field: 'name', title: '工序名称', minWidth: 180 },
    { field: 'workshopName', title: '默认车间', minWidth: 160 },
    {
      field: 'processType',
      title: '工序类型',
      width: 100,
      formatter: (v) => formatProcessType(v),
    },
    {
      field: 'status',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: (v) =>
        ({ 0: '启用', 1: '停用' } as Record<number, string>)[Number(v)] ?? '-',
    },
    { field: 'remark', title: '备注', minWidth: 200 },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getProcessPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      code: params.filters?.code || undefined,
      name: params.filters?.name || keyword,
      processType: params.filters?.processType || undefined,
      status: 0,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id ?? '',
    code: row.code ?? '',
    name: row.name ?? '',
    label: row.code ?? '',
    status: row.status,
    extra: {
      processType: row.processType,
      workshopId: row.workshopId,
      workshopCode: row.workshopCode,
      workshopName: row.workshopName,
    },
    raw: row as any,
  }),
};
