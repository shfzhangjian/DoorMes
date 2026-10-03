// 文件路径：src/api/mes/cost/allocation/overhead/index.ts

export namespace MesCostOverheadApi {
  export interface Overhead {
    id: number;
    period: string;         // 核算期间 (YYYY-MM)
    costCenterId: number;   // 成本中心ID
    elementCode: number;    // 费用要素
    amount: number;         // 归集金额
    source: number;         // 数据来源
    status: number;         // 状态 (0-草稿, 1-已确认/过账)
    remark?: string;
    createTime?: string;
  }
}

// 模拟数据：增加 2026-01 和 2026-02 的数据以体现切换效果
const mockData: MesCostOverheadApi.Overhead[] = [
  // --- 2026-02 数据 ---
  { id: 1, period: '2026-02', costCenterId: 1111, elementCode: 1, amount: 85000.00, source: 2, status: 1, remark: 'CNC加工中心当月折旧分摊', createTime: '2026-02-28' },
  { id: 2, period: '2026-02', costCenterId: 1111, elementCode: 2, amount: 12500.50, source: 1, status: 0, remark: '独立电表抄表', createTime: '2026-02-28' },
  { id: 3, period: '2026-02', costCenterId: 1121, elementCode: 3, amount: 45000.00, source: 1, status: 0, remark: 'A线线长薪酬', createTime: '2026-02-28' },
  // --- 2026-01 数据 ---
  { id: 4, period: '2026-01', costCenterId: 1111, elementCode: 1, amount: 85000.00, source: 2, status: 1, remark: '1月折旧已结账', createTime: '2026-01-31' },
  { id: 5, period: '2026-01', costCenterId: 1121, elementCode: 3, amount: 42000.00, source: 1, status: 1, remark: '1月人工已结账', createTime: '2026-01-31' },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

/** [新增] 获取核算期间树 (按年份 -> 月份分组) */
export async function getOverheadPeriods() {
  await delay(200);
  return [
    { period: '2026-02', label: '2026年 02月', tag: '当期', isClosed: false },
    { period: '2026-01', label: '2026年 01月', tag: '已结账', isClosed: true },
    { period: '2025-12', label: '2025年 12月', tag: '已结账', isClosed: true },
    { period: '2025-11', label: '2025年 11月', tag: '已结账', isClosed: true },
    { period: '2025-10', label: '2025年 10月', tag: '已结账', isClosed: true },
    { period: '2025-09', label: '2025年 09月', tag: '已结账', isClosed: true },
  ];
}

/** 分页查询 */
export async function getCostOverheadPage(params: any) {
  await delay(300);
  let list = [...mockData];

  // 核心：强制按传入的核算期间过滤
  if (params.period) {
    list = list.filter(item => item.period === params.period);
  }
  if (params.costCenterId) {
    list = list.filter(item => String(item.costCenterId) === String(params.costCenterId));
  }
  if (params.elementCode) {
    list = list.filter(item => String(item.elementCode) === String(params.elementCode));
  }
  if (params.status !== undefined && params.status !== '') {
    list = list.filter(item => String(item.status) === String(params.status));
  }

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

export async function getCostOverhead(id: number) {
  await delay(200);
  return mockData.find(item => item.id === id) || null;
}
export async function createCostOverhead(data: MesCostOverheadApi.Overhead) {
  await delay(300); return { code: 200, data: true, msg: 'success' };
}
export async function updateCostOverhead(data: MesCostOverheadApi.Overhead) {
  await delay(300); return { code: 200, data: true, msg: 'success' };
}
export async function deleteCostOverheadList(ids: number[]) {
  await delay(300); return { code: 200, data: true, msg: 'success' };
}
export async function confirmCostOverhead(id: number) {
  await delay(400); return { code: 200, data: true, msg: 'success' };
}
