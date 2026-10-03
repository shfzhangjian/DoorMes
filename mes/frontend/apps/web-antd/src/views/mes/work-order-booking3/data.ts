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

export const mockTasks = [
  { id: 'WO-260305-001', planNo: 'P-20260305-1', product: '高透抛光垫 Pro', process: '压槽', planQty: 500, goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-260305-002', planNo: 'P-20260305-1', product: '高透抛光垫 Pro', process: '湿法', planQty: 500, goodQty: 200, scrapQty: 2, status: 'IN_PROGRESS', startTime: '2026-03-05 08:30:00' },
  { id: 'WO-260305-003', planNo: 'P-20260305-2', product: '柔性打磨垫', process: '分切', planQty: 1000, goodQty: 1000, scrapQty: 5, status: 'COMPLETED', startTime: '2026-03-04 13:00:00' },
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

export const deviceCheckColumns = [
  { title: '检查/参数项目', dataIndex: 'item', key: 'item' },
  { title: '工艺标准要求', dataIndex: 'standard', key: 'standard', width: 180 },
  { title: '实测状态 / 数值录入', dataIndex: 'actual', key: 'actual', width: 280 },
  { title: '判定结果', dataIndex: 'result', key: 'result', width: 120, align: 'center' },
];

export const mockCheckData = [
  { id: 1, type: 'check', item: '开机前5S清扫/劳保穿戴', standard: '符合规范', actual: null, result: null },
  { id: 2, type: 'input', item: '主轴气压 (MPa)', standard: '0.5 ~ 0.7', actual: null, min: 0.5, max: 0.7, result: null },
  { id: 3, type: 'input', item: '槽液浓度 (%)', standard: '5.0 ~ 8.0', actual: null, min: 5.0, max: 8.0, result: null },
];
