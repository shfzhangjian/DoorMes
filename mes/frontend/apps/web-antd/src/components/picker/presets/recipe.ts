import { getRecipePage } from '#/api/mes/hc/recipe';
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';

import type { PickerEntityConfig } from '../types';

const recipeTypeOptions = [
  { label: '量产', value: 'mass' },
  { label: '研发', value: 'rd' },
];

function formatRecipeType(value?: string) {
  return recipeTypeOptions.find((item) => item.value === value)?.label ?? (value ? String(value) : '-');
}

export const recipePickerConfig: PickerEntityConfig<MesHcRecipeApi.Recipe> = {
  entityKey: 'recipe',
  title: '选择配方',
  tableTitle: '配方列表',
  modalWidth: 1460,
  inlinePanelWidth: 940,
  queryFields: [
    { field: 'recipeName', label: '配方名称', placeholder: '请输入配方名称' },
    { field: 'recipeCode', label: '配方编码', placeholder: '请输入配方编码' },
    { field: 'modelCode',  label: '型号编码', placeholder: '请输入型号编码' },
    {
      field: 'recipeType',
      label: '配方类型',
      type: 'select',
      options: recipeTypeOptions,
      placeholder: '请选择配方类型',
      defaultHidden: true,
    },
    { field: 'remark', label: '备注说明', placeholder: '请输入备注说明', defaultHidden: true },
  ],
  columns: [
    { field: 'recipeName', title: '配方名称', minWidth: 160 },
    { field: 'recipeCode', title: '配方编码', minWidth: 150 },
    {
      field: 'recipeType',
      title: '配方类型',
      width: 100,
      formatter: (v) => formatRecipeType(v),
    },
    { field: 'modelCode', title: '型号编码', minWidth: 150 },
    { field: 'remark',    title: '备注说明', minWidth: 220 },
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
    const res = await getRecipePage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      recipeName: params.filters?.recipeName || keyword,
      recipeCode: params.filters?.recipeCode || undefined,
      modelCode:  params.filters?.modelCode  || undefined,
      recipeType: params.filters?.recipeType || undefined,
      remark:     params.filters?.remark     || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id:    row.id,
    code:  row.recipeCode ?? '',
    name:  row.recipeName ?? '',
    label: row.modelCode  ?? row.recipeCode ?? '',
    status: row.status,
    extra: {
      recipeType: row.recipeType,
      modelCode:  row.modelCode,
      remark:     row.remark,
    },
    raw: row as any,
  }),
};
