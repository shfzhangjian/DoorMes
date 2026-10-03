import { getProductModelPage } from '#/api/mes/hc/productmodel';
import type { MesHcProductModelApi } from '#/api/mes/hc/productmodel';

import type { PickerEntityConfig } from '../types';

export const productModelPickerConfig: PickerEntityConfig<MesHcProductModelApi.ProductModel> = {
  entityKey: 'product-model',
  title: '选择型号编码',
  tableTitle: '产品型号列表',
  modalWidth: 1080,
  inlinePanelWidth: 720,
  queryFields: [
    { field: 'modelCode', label: '型号编码', placeholder: '请输入型号编码' },
    { field: 'modelName', label: '型号名称', placeholder: '请输入型号名称' },
    {
      field: 'status',
      label: '状态',
      type: 'select',
      options: [
        { label: '启用', value: 'ENABLE' },
        { label: '停用', value: 'DISABLE' },
      ],
      defaultValue: 'ENABLE',
      placeholder: '请选择状态',
    },
  ],
  columns: [
    { field: 'modelCode', title: '型号编码', minWidth: 180 },
    { field: 'modelName', title: '产品型号名称', minWidth: 220 },
    {
      field: 'modelLevel',
      title: '型号层级',
      minWidth: 110,
      formatter: (v) => (v === 'FAMILY' ? '系列型号' : '具体型号'),
    },
    { field: 'parentModelCode', title: '所属系列', minWidth: 110 },
    {
      field: 'prodTypeName',
      title: '生产类型',
      minWidth: 120,
      formatter: (v, row) => v || row?.prodType || '-',
    },
    { field: 'modelRuleName', title: '型号规则', minWidth: 180 },
    { field: 'sizeName', title: '尺寸规格', minWidth: 100 },
    { field: 'recipeCode', title: '默认配方', minWidth: 140 },
    {
      field: 'status',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: (v) => ({ ENABLE: '启用', DISABLE: '停用' } as Record<string, string>)[String(v)] ?? '-',
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getProductModelPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      modelCode: params.filters?.modelCode || keyword,
      modelName: params.filters?.modelName || undefined,
      status: params.filters?.status || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id ?? 0,
    code: row.modelCode ?? '',
    name: row.modelName ?? '',
    label: row.modelCode ?? '',
    extra: {
      modelName: row.modelName,
      modelLevel: row.modelLevel,
      parentModelId: row.parentModelId,
      parentModelCode: row.parentModelCode,
      modelRuleId: row.modelRuleId,
      modelRuleName: row.modelRuleName,
      prodType: row.prodType,
      prodTypeName: row.prodTypeName,
      recipeId: row.recipeId,
      recipeCode: row.recipeCode,
      recipeName: row.recipeName,
      sizeSpec: row.sizeSpec,
      sizeName: row.sizeName,
    },
    raw: row as any,
  }),
};
