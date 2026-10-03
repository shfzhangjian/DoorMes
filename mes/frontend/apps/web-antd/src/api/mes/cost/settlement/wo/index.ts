// 文件路径：src/api/mes/cost/settlement/wo/index.ts

export namespace MesCostSettlementApi {
  export interface Settlement {
    id: number;
    period: string;
    workOrderNo: string;
    productName: string;
    costCenterName: string;
    completedQty: number;
    materialCost: number;     // 允许人工修正
    overheadCost: number;     // 允许人工修正
    totalCost: number;
    unitCost: number;
    status: number;
    settleTime?: string;
  }
}

// 模拟初始数据：状态为 0 时可以调整
let mockData: MesCostSettlementApi.Settlement[] = [
  { id: 1, period: '2026-02', workOrderNo: 'WO-2602-001', productName: '精密金属结构件', costCenterName: 'CNC柔性加工中心', completedQty: 300, materialCost: 15000.00, overheadCost: 0, totalCost: 15000.00, unitCost: 50.00, status: 0 },
  { id: 2, period: '2026-02', workOrderNo: 'WO-2602-002', productName: '锂电池前段浆料', costCenterName: '制浆连续产线', completedQty: 900, materialCost: 45000.00, overheadCost: 0, totalCost: 45000.00, unitCost: 50.00, status: 0 },
  { id: 3, period: '2026-02', workOrderNo: 'WO-2602-005', productName: '成品智能终端', costCenterName: '人工组装A线', completedQty: 120, materialCost: 120000.00, overheadCost: 0, totalCost: 120000.00, unitCost: 1000.00, status: 0 },
  { id: 4, period: '2026-01', workOrderNo: 'WO-2601-088', productName: '精密金属结构件', costCenterName: 'CNC柔性加工中心', completedQty: 150, materialCost: 7500.00, overheadCost: 4250.50, totalCost: 11750.50, unitCost: 78.34, status: 1, settleTime: '2026-01-31 23:55:00' },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getSettlementPeriods() {
  await delay(200);
  return [
    { period: '2026-02', label: '2026年 02月', tag: '当期', isClosed: false },
    { period: '2026-01', label: '2026年 01月', tag: '已结账', isClosed: true },
    { period: '2025-12', label: '2025年 12月', tag: '已结账', isClosed: true },
  ];
}

export async function getSettlementPage(params: any) {
  await delay(300);
  let list = [...mockData];
  if (params.period) list = list.filter(item => item.period === params.period);
  if (params.workOrderNo) list = list.filter(item => item.workOrderNo.includes(params.workOrderNo));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

/** [新增] 行内手工调整成本 (保存并重算) */
export async function updateSettlementCost(id: number, field: 'materialCost' | 'overheadCost', value: number) {
  await delay(100);
  const item = mockData.find(i => i.id === id);
  if (item && item.status === 0) {
    item[field] = value || 0;
    item.totalCost = item.materialCost + item.overheadCost;
    item.unitCost = item.totalCost / (item.completedQty || 1);
  }
  return { code: 200, data: true, msg: 'success' };
}

export async function executePeriodSettlement(period: string) {
  await delay(1200);
  let calculatedCount = 0;
  mockData = mockData.map(item => {
    if (item.period === period && item.status === 0) {
      calculatedCount++;
      // 如果未人工分配制费，则模拟系统分配；若已分配，则保留
      const finalOverhead = item.overheadCost > 0 ? item.overheadCost : Math.round(Math.random() * 10000 + 2000);
      const total = item.materialCost + finalOverhead;
      return {
        ...item,
        overheadCost: finalOverhead,
        totalCost: total,
        unitCost: total / (item.completedQty || 1),
        status: 1,
        settleTime: new Date().toLocaleString().replace(/\//g, '-')
      };
    }
    return item;
  });
  return { code: 200, data: calculatedCount, msg: `结转成功！锁定 ${calculatedCount} 笔数据` };
}

export async function exportSettlement(_params: any) {
  await delay(500); return new Blob([''], { type: 'application/vnd.ms-excel' });
}
