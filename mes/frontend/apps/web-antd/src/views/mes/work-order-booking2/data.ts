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
  { title: '点检项目', dataIndex: 'item', key: 'item', width: 220 },
  { title: '标准要求', dataIndex: 'standard', key: 'standard', minWidth: 220 },
  { title: '实测/判定', dataIndex: 'actual', key: 'actual', width: 180 },
  { title: '结果', dataIndex: 'result', key: 'result', width: 100, align: 'center' },
];

export const mockCheckData = [
  { id: 1, type: 'check', item: '气源压力', standard: '0.5-0.7MPa，开关正常', actual: null, result: null },
  { id: 2, type: 'check', item: '安全护罩', standard: '安装到位，无松动', actual: null, result: null },
  { id: 3, type: 'number', item: '主轴转速', standard: '1200-1500 rpm', min: 1200, max: 1500, actual: null, result: null },
  { id: 4, type: 'number', item: '工艺温度', standard: '22-26 ℃', min: 22, max: 26, actual: null, result: null },
];
