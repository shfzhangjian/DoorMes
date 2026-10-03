import { getSaleOrderPage } from '#/api/mes/sale-order';
import type { MesSaleOrderApi } from '#/api/mes/sale-order';
import dayjs from 'dayjs';
import {
  PLAN_FIELD_COPY,
  PLAN_PLACEHOLDER_COPY,
} from '#/views/mes/hc/plan/shared/field-copy';

import type { PickerEntityConfig } from '../types';

function formatDateSlash(value?: unknown) {
  const normalized = normalizeLocalDate(value);
  return normalized ? normalized.replaceAll('-', '/') : '-';
}

function normalizeLocalDate(value?: unknown) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    return '';
  }
  const date = dayjs(value as any);
  if (date.isValid()) {
    return date.format('YYYY-MM-DD');
  }
  return String(value);
}

export const saleOrderPickerConfig: PickerEntityConfig<MesSaleOrderApi.MesSaleOrder> = {
  entityKey: 'sale-order',
  title: '选择销售订单',
  tableTitle: '销售订单列表',
  modalWidth: 1180,
  inlinePanelWidth: 860,
  queryFields: [
    { field: 'orderNo', label: '销售订单号', placeholder: '请输入销售订单号' },
    { field: 'erpNo', label: 'ERP编号', placeholder: '请输入ERP编号' },
    { field: 'customerName', label: '客户名称', placeholder: '请输入客户名称' },
    { field: 'productCode', label: PLAN_FIELD_COPY.productionMaterial, placeholder: PLAN_PLACEHOLDER_COPY.productionMaterial },
    { field: 'productSpec', label: PLAN_FIELD_COPY.sizeSpec, placeholder: PLAN_PLACEHOLDER_COPY.sizeSpec, defaultHidden: true },
  ],
  columns: [
    { field: 'orderNo', title: '销售订单号', minWidth: 170 },
    { field: 'erpNo', title: 'ERP编号', minWidth: 140 },
    { field: 'customerName', title: '客户', minWidth: 140 },
    { field: 'materialCode', title: PLAN_FIELD_COPY.productionMaterial, minWidth: 150 },
    { field: 'materialName', title: PLAN_FIELD_COPY.materialName, minWidth: 210 },
    { field: 'modelCode', title: PLAN_FIELD_COPY.productModel, minWidth: 150 },
    { field: 'sizeName', title: PLAN_FIELD_COPY.sizeSpec, width: 100, align: 'center' },
    { field: 'remainQty', title: '欠交量', width: 100, align: 'right' },
    {
      field: 'deliveryDate',
      title: '交货日期',
      width: 120,
      align: 'center',
      formatter: (value) => formatDateSlash(value),
    },
    { field: 'statusName', title: '状态', width: 100, align: 'center' },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getSaleOrderPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      orderNo: params.filters?.orderNo || undefined,
      erpNo: params.filters?.erpNo || undefined,
      customerName: params.filters?.customerName || keyword,
      productCode: params.filters?.productCode || undefined,
      productSpec: params.filters?.productSpec || undefined,
      keyword,
      planSelectable: true,
    });
    return { total: res.total, list: res.list };
  },
  buildOption: (row) => ({
    id: row.id as number,
    code: row.orderNo ?? '',
    name: row.customerName ?? '',
    label: row.orderNo ?? '',
    status: row.status,
    extra: {
      orderLineNo: row.orderLineNo,
      erpNo: row.erpNo,
      customerId: row.customerId,
      productId: row.productId,
      productCode: row.materialCode || row.productCode,
      productName: row.materialName || row.productName,
      productSpec: row.sizeSpec || row.productSpec,
      materialId: row.materialId,
      materialCode: row.materialCode,
      materialName: row.materialName,
      modelCode: row.modelCode,
      sizeSpec: row.sizeSpec,
      sizeName: row.sizeName,
      quantity: row.quantity,
      plannedQty: row.plannedQty,
      remainQty: row.remainQty,
      unit: row.unit,
      deliveryDate: normalizeLocalDate(row.deliveryDate),
      statusName: row.statusName,
    },
    raw: {
      ...row,
      deliveryDate: normalizeLocalDate(row.deliveryDate),
    } as any,
  }),
};
