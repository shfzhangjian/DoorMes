import { getMaterialPage } from '#/api/mes/hc/material';
import type { MesHcMaterialApi } from '#/api/mes/hc/material';
import { getMaterialCategoryTree } from '#/api/mes/hc/materialcategory';
import type { MesHcMaterialCategoryApi } from '#/api/mes/hc/materialcategory';

import type { PickerEntityConfig } from '../types';

const inlineKeywordFields = [
  'materialCode',
  'materialName',
  'materialShortName',
  'specModel',
  'modelCode',
] as const;

function normalizeKeyword(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function buildCommonParams(params: {
  filters?: Record<string, any>;
  pageNo: number;
  pageSize: number;
}) {
  const filters = params.filters ?? {};
  return {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    defaultRecipeCode: filters.recipeKeyword || undefined,
    defaultRecipeName: filters.recipeKeyword || undefined,
    materialCategoryId: filters.materialCategoryId || undefined,
  };
}

export const materialPickerConfig: PickerEntityConfig<MesHcMaterialApi.Material> = {
  entityKey: 'material',
  title: '选择物料',
  tableTitle: '物料列表',
  modalWidth: 1180,
  inlinePanelWidth: 820,
  treeFilter: {
    title: '物料分类',
    filterField: 'materialCategoryId',
    loadTree: getMaterialCategoryTree,
    allNode: {
      id: 0,
      categoryCode: 'ALL',
      categoryName: '全部',
      children: [],
    } as MesHcMaterialCategoryApi.TreeNode,
    fieldNames: { title: 'categoryName', key: 'id', children: 'children' },
    codeField: 'categoryCode',
    nameField: 'categoryName',
    keywordPlaceholder: '请输入分类名称/编码',
  },
  queryFields: [
    { field: 'materialCode', label: '物料编码', placeholder: '请输入物料编码' },
    { field: 'modelCode',    label: '型号编码', placeholder: '请输入型号编码' },
    { field: 'specModel',    label: '型号描述', placeholder: '请输入型号描述', defaultHidden: true },
    { field: 'materialName', label: '物料名称', placeholder: '请输入物料名称', defaultHidden: true },
    { field: 'recipeKeyword',label: '配方关键字', placeholder: '请输入配方编码或名称', defaultHidden: true },
  ],
  columns: [
    { field: 'materialCode',      title: '物料编码', minWidth: 150 },
    { field: 'materialName',      title: '物料名称', minWidth: 200 },
    { field: 'materialShortName', title: '物料简称', minWidth: 120 },
    { field: 'specModel',         title: '型号描述', minWidth: 180 },
    { field: 'modelCode',         title: '型号编码', minWidth: 150 },
    { field: 'defaultRecipeCode', title: '配方编码', minWidth: 150 },
    {
      field: 'materialType',
      title: '物料类型',
      width: 100,
      formatter: (v) =>
        ({ 1: '成品', 2: '半成品', 3: '原料', 4: '辅料', 5: '包材', 6: '备件' } as Record<number, string>)[Number(v)] ?? '-',
    },
    {
      field: 'materialStatus',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: (v) =>
        ({ 0: '草稿', 1: '启用', 2: '停用' } as Record<number, string>)[Number(v)] ?? '-',
    },
  ],
  fetchPage: async (params) => {
    const filters = params.filters ?? {};
    const baseParams = buildCommonParams(params);
    const res = await getMaterialPage({
      ...baseParams,
      materialCode:      filters.materialCode    || undefined,
      materialName:      filters.materialName    || undefined,
      specModel:         filters.specModel       || undefined,
      modelCode:         filters.modelCode       || undefined,
    });
    return { total: res.total, list: res.list };
  },
  inlineFetchPage: async (params) => {
    const filters = params.filters ?? {};
    const keyword = normalizeKeyword(filters.keyword) || undefined;
    const baseParams = buildCommonParams(params);
    const hasExplicitFieldFilter = inlineKeywordFields.some(
      (field) => !!filters[field],
    );
    if (keyword && !hasExplicitFieldFilter) {
      const materialCodeRes = await getMaterialPage({
        ...baseParams,
        materialCode: keyword,
      });
      if (materialCodeRes.list?.length) {
        return { total: materialCodeRes.total, list: materialCodeRes.list };
      }
      const list: MesHcMaterialApi.Material[] = [];
      const seen = new Set<number | string>();
      let total = 0;
      for (const field of inlineKeywordFields.filter(
        (field) => field !== 'materialCode',
      )) {
        const res = await getMaterialPage({
          ...baseParams,
          pageNo: 1,
          [field]: keyword,
        } as Record<string, any>);
        total += res.total || 0;
        for (const row of res.list || []) {
          const key = row.id ?? row.materialCode;
          if (key === undefined || seen.has(key)) {
            continue;
          }
          seen.add(key);
          list.push(row);
          if (list.length >= params.pageSize) {
            break;
          }
        }
        if (list.length >= params.pageSize) {
          break;
        }
      }
      return { total: Math.max(total, list.length), list };
    }
    const res = await getMaterialPage({
      ...baseParams,
      materialCode:      filters.materialCode    || undefined,
      materialName:      filters.materialName    || keyword,
      specModel:         filters.specModel       || undefined,
      modelCode:         filters.modelCode       || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id:     row.id,
    code:   row.materialCode  ?? '',
    name:   row.materialName  ?? '',
    label:  row.materialCode  ?? '',
    status: row.materialStatus,
    extra: {
      materialShortName: row.materialShortName,
      specModel:         row.specModel,
      modelCode:         row.modelCode,
      defaultRecipeCode: row.defaultRecipeCode,
      defaultRecipeName: row.defaultRecipeName,
      defaultBomCode:    row.defaultBomCode,
      defaultRouteCode:  row.defaultRouteCode,
      baseUom:           row.baseUom,
    },
    raw: row as any,
  }),
};
