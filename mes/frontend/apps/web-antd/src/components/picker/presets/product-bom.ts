import { getBomPage } from '#/api/mes/hc/bom';
import type { MesHcBomApi } from '#/api/mes/hc/bom';

import type { PickerEntityConfig } from '../types';

export const productBomPickerConfig: PickerEntityConfig<MesHcBomApi.Bom> = {
  entityKey: 'productBom',
  title: '选择产品料号',
  tableTitle: 'BOM产品料号列表',
  modalWidth: 1180,
  inlinePanelWidth: 900,
  queryFields: [
    { field: 'productMaterialKeyword', label: '产品料号', placeholder: '请输入产品料号或名称' },
    { field: 'productModelCode', label: '产品型号', placeholder: '请输入产品型号' },
    { field: 'productSpec', label: '尺寸规格', placeholder: '请输入尺寸规格' },
    { field: 'bomCode', label: 'BOM编码', placeholder: '请输入BOM编码', defaultHidden: true },
  ],
  columns: [
    { field: 'productModelCode', title: '产品型号', minWidth: 130 },
    { field: 'productMaterialCode', title: '产品料号', minWidth: 150 },
    { field: 'productMaterialName', title: '产品名称', minWidth: 190 },
    { field: 'productSpec', title: '尺寸规格', minWidth: 110 },
    { field: 'bomCode', title: 'BOM编码', minWidth: 150 },
    { field: 'bomName', title: 'BOM名称', minWidth: 190 },
    { field: 'versionNo', title: '版本', width: 90 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      align: 'center',
      formatter: (value) => (Number(value) === 1 ? '启用' : '停用'),
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getBomPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      status: 1,
      bomCode: params.filters?.bomCode || undefined,
      productMaterialKeyword: params.filters?.productMaterialKeyword || keyword,
      productModelCode: params.filters?.productModelCode || undefined,
      productSpec: params.filters?.productSpec || undefined,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id,
    code: row.productMaterialCode || '',
    name: row.productMaterialName || '',
    label: row.productMaterialCode || '',
    status: Number(row.status),
    extra: {
      bomId: row.id,
      bomCode: row.bomCode,
      bomName: row.bomName,
      bomType: row.bomType,
      bomVersion: row.versionNo,
      productMaterialId: row.productMaterialId,
      productMaterialCode: row.productMaterialCode,
      productMaterialName: row.productMaterialName,
      productModelId: row.productModelId,
      productModelCode: row.productModelCode,
      productModelName: row.productModelName,
      productSpec: row.productSpec,
      routeId: row.routeId,
      routeCode: row.routeCode,
    },
    raw: row as any,
  }),
};
