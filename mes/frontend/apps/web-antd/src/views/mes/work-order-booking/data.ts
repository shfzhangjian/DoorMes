// 文件路径：src/views/mes/plan/production-task/data.ts

export const STATUS_MAP: Record<string, { text: string; color: string; badge: string }> = {
  'PENDING': { text: '待开工', color: 'default', badge: 'bg-slate-300' },
  'IN_PROGRESS': { text: '生产中', color: 'processing', badge: 'bg-blue-500 animate-pulse' },
  'COMPLETED': { text: '已完工', color: 'success', badge: 'bg-emerald-500' },
};

// 🌟 新增：基于工业常识的工序单位标准字典
export const PROCESS_UOM_MAP: Record<string, string> = {
  '配料': 'kg',
  '温法': 'm', '湿法': 'm', '粗磨': 'm', '精磨': 'm', '粘双面胶': 'm', '分切': 'm',
  '单片压槽': 'pcs', '单片背胶': 'pcs', '裁圆': 'pcs', '包装': 'pcs'
};

export const taskColumns = [
  { field: 'id', title: '工单号', width: 140, fixed: 'left' },
  { field: 'product', title: '加工产品', minWidth: 160 },
  { field: 'process', title: '执行工序', width: 100 },
  // 🌟 修改：在列表展示时，把单位带上
  { field: 'planQty', title: '计划量', width: 100, align: 'right', formatter: ({ row }: any) => `${row.planQty} ${row.uom || ''}` },
  { field: 'goodQty', title: '已报良品', width: 100, align: 'right', slots: { default: 'goodSlot' } },
  { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'statusSlot' } },
  { field: 'startTime', title: '开工时间', width: 150 },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } }
];

export const mockTasks = [
  { id: 'WO-260305-001', planNo: 'P-20260305-1', product: '高透抛光垫 Pro', process: '压槽', planQty: 500, uom: 'pcs', goodQty: 0, scrapQty: 0, status: 'PENDING', startTime: null },
  { id: 'WO-260305-002', planNo: 'P-20260305-1', product: '高透抛光垫 Pro', process: '湿法', planQty: 500, uom: 'm', goodQty: 200, scrapQty: 2, status: 'IN_PROGRESS', startTime: '2026-03-05 08:30:00' },
  { id: 'WO-260305-003', planNo: 'P-20260305-2', product: '柔性打磨垫', process: '分切', planQty: 1000, uom: 'm', goodQty: 1000, scrapQty: 5, status: 'COMPLETED', startTime: '2026-03-04 13:00:00' },
];

export const PROCESS_OPTIONS = [
  '配料', '温法', '粗磨', '精磨', '粘双面胶', '分切', '单片压槽', '单片背胶', '裁圆', '包装'
];

export const DEFECT_CODES = [
  { label: 'D01-尺寸超差', value: 'D01' },
  { label: 'D02-表面划痕', value: 'D02' },
  { label: 'D03-厚度不均', value: 'D03' },
  { label: 'D04-杂质污染', value: 'D04' },
  { label: 'D05-其他异常', value: 'D05' }
];
