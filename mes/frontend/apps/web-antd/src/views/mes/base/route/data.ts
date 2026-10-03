// 文件路径：src/views/mes/work-order-booking/data.ts

export const STATUS_MAP: Record<string, { text: string; color: string; badge: string }> = {
  'PENDING': { text: '待开工', color: 'default', badge: 'bg-slate-300' },
  'IN_PROGRESS': { text: '生产中', color: 'processing', badge: 'bg-blue-500 animate-pulse' },
  'COMPLETED': { text: '已完工', color: 'success', badge: 'bg-emerald-500' },
};

export const taskColumns = [
  { field: 'id', title: '工单号', width: 140, fixed: 'left' },
  { field: 'product', title: '加工产品', minWidth: 160 },
  { field: 'process', title: '执行工序', width: 100 },
  { field: 'planQty', title: '计划量', width: 80, align: 'right' },
  { field: 'goodQty', title: '已报良品', width: 90, align: 'right', slots: { default: 'goodSlot' } },
  { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'statusSlot' } },
  { field: 'startTime', title: '开工时间', width: 150 },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } }
];

// 设备点检与参数默认数据
export const deviceCheckColumns = [
  { title: '检查/参数项', dataIndex: 'item', key: 'item', width: 200 },
  { title: '标准要求', dataIndex: 'standard', key: 'standard', width: 150 },
  { title: '实测值/状态录入', dataIndex: 'actual', key: 'actual' },
  { title: '判定', dataIndex: 'result', key: 'result', width: 100, align: 'center' }
];

export const PROCESS_OPTIONS = [
  '配料', '温法', '粗磨', '精磨', '粘双面胶', '分切', '单片压槽', '单片背胶', '裁圆'
];

export const DEFECT_CODES = [
  { label: 'D01-尺寸超差', value: 'D01' },
  { label: 'D02-表面划痕', value: 'D02' },
  { label: 'D03-厚度不均', value: 'D03' },
  { label: 'D04-杂质污染', value: 'D04' },
  { label: 'D05-其他异常', value: 'D05' }
];
