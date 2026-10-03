import type { PageParam, PageResult } from '@vben/request';

export namespace MesMaintPlanApi {
  // 维保策略/计划
  export interface Plan {
    id?: number;
    planNo?: string;         // 计划编号
    deviceCode?: string;     // 设备编号
    deviceName?: string;     // 设备名称
    pmLevel?: string;        // 维保级别 (一级/二级)
    cycleDays?: number;      // 周期(天)
    content?: string;        // 维保内容
    status?: number;         // 状态 (1启用 0停用)
    createTime?: string;
  }

  // 维保执行工单
  export interface Order {
    id?: number;
    orderNo?: string;        // 工单号
    planNo?: string;         // 关联计划编号
    deviceCode?: string;
    deviceName?: string;
    pmLevel?: string;
    operator?: string;       // 执行人
    status?: number;         // 状态: 10待执行, 20执行中, 30已完成
    planDate?: string;       // 计划执行日期
    actualDate?: string;     // 实际执行日期
    remark?: string;         // 总结与备注
    items?: OrderItem[];     // 消耗的备件明细
  }

  // 工单消耗备件
  export interface OrderItem {
    id?: string | number;
    orderId?: number;
    partCode?: string;
    partName?: string;
    spec?: string;
    quantity?: number;
    unit?: string;
    remark?: string;
    sort?: number;
  }
}

// ================= Mock 数据池 =================
const mockPlans: MesMaintPlanApi.Plan[] = [
  { id: 1, planNo: 'PM-MIX-001', deviceCode: 'EQ-MIX-01', deviceName: '1#真空搅拌机', pmLevel: '一级保养', cycleDays: 30, content: '检查并紧固所有螺栓，清洁电机外壳，检查主轴密封圈是否磨损。', status: 1, createTime: '2026-01-10 09:00:00' },
  { id: 2, planNo: 'PM-MIX-002', deviceCode: 'EQ-MIX-01', deviceName: '1#真空搅拌机', pmLevel: '二级保养', cycleDays: 180, content: '更换减速机润滑油，深度探伤搅拌桨叶，更换真空密封圈。', status: 1, createTime: '2026-01-10 09:15:00' },
  { id: 3, planNo: 'PM-COA-001', deviceCode: 'EQ-COA-02', deviceName: '2#精密涂布机', pmLevel: '一级保养', cycleDays: 15, content: '背辊陶瓷轴承注油，烘箱循环风机滤网清洗。', status: 1, createTime: '2026-01-15 14:00:00' },
];

const mockOrderItems: MesMaintPlanApi.OrderItem[] = [
  { id: 101, orderId: 2, partCode: 'PT-MIX-002', partName: '减速机润滑油', spec: 'VG220 合成油', quantity: 10, unit: 'L', remark: '全部更换', sort: 1 },
  { id: 102, orderId: 2, partCode: 'PT-MIX-001', partName: '主轴机械密封', spec: 'Φ120mm PTFE', quantity: 2, unit: '套', remark: '', sort: 2 },
];

const mockOrders: MesMaintPlanApi.Order[] = [
  { id: 1, orderNo: 'WO-PM-260210-01', planNo: 'PM-MIX-001', deviceCode: 'EQ-MIX-01', deviceName: '1#真空搅拌机', pmLevel: '一级保养', operator: '王五', status: 30, planDate: '2026-02-10', actualDate: '2026-02-10 16:30:00', remark: '紧固螺栓3处，设备状态良好' },
  { id: 2, orderNo: 'WO-PM-260220-01', planNo: 'PM-MIX-002', deviceCode: 'EQ-MIX-01', deviceName: '1#真空搅拌机', pmLevel: '二级保养', operator: '机修班组长', status: 20, planDate: '2026-02-20', actualDate: '', remark: '正在进行换油作业' },
  { id: 3, orderNo: 'WO-PM-260225-02', planNo: 'PM-COA-001', deviceCode: 'EQ-COA-02', deviceName: '2#精密涂布机', pmLevel: '一级保养', operator: '-', status: 10, planDate: '2026-02-25', actualDate: '', remark: '等待排期' },
];

// ================= API 模拟 =================
export function getPlanPage(params: PageParam) { return Promise.resolve({ list: mockPlans, total: mockPlans.length } as PageResult<MesMaintPlanApi.Plan>); }
export function getPlan(id: number) { return Promise.resolve(mockPlans.find(i => i.id === id) as MesMaintPlanApi.Plan); }
export function createPlan(data: MesMaintPlanApi.Plan) { return Promise.resolve({ success: true, data }); }
export function updatePlan(data: MesMaintPlanApi.Plan) { return Promise.resolve({ success: true, data }); }
export function deletePlanList(ids: number[]) { return Promise.resolve({ success: true }); }

export function getOrderPage(params: PageParam) { return Promise.resolve({ list: mockOrders, total: mockOrders.length } as PageResult<MesMaintPlanApi.Order>); }
export function getOrder(id: number) {
  const order = mockOrders.find(i => i.id === id);
  const items = mockOrderItems.filter(i => i.orderId === id);
  return Promise.resolve(JSON.parse(JSON.stringify({ ...order, items })) as MesMaintPlanApi.Order);
}
export function createOrder(data: MesMaintPlanApi.Order) { return Promise.resolve({ success: true, data }); }
export function updateOrder(data: MesMaintPlanApi.Order) { return Promise.resolve({ success: true, data }); }
export function deleteOrderList(ids: number[]) { return Promise.resolve({ success: true }); }

export function getOrderItemListById(orderId: number) {
  const items = mockOrderItems.filter(i => i.orderId === orderId);
  return Promise.resolve(JSON.parse(JSON.stringify(items)) as MesMaintPlanApi.OrderItem[]);
}
