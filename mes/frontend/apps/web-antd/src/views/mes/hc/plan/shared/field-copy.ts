export const PLAN_FIELD_COPY = {
  productionMaterial: '生产料号',
  materialName: '物料名称',
  productModel: '产品型号',
  sizeSpec: '尺寸规格',
  saleOrderNo: '销售订单号',
  erpNo: 'ERP编号',
  customerName: '客户名称',
  orderLineNo: '订单行号',
  orderQty: '订单数量',
  plannedQty: '已排数量',
  deliveryDate: '交货日期',
  remark: '备注',
} as const;

export const PLAN_PLACEHOLDER_COPY = {
  productionMaterial: '请输入生产料号或快速选择',
  sizeSpec: '请选择尺寸规格',
  saleOrderNo: '请输入销售订单号或快速选择',
} as const;

export const SIZE_SPEC_OPTIONS = [
  { value: '775', label: '775mm', code: '775', name: '775mm' },
  { value: '740', label: '740mm', code: '740', name: '740mm' },
];
