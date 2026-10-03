// 文件路径：src/api/mes/cost/allocation/equivalent/index.ts

export namespace MesCostEquivalentApi {
  export interface EquivalentRecord {
    id: number;
    period: string;           // 核算期间
    workOrderNo: string;      // 生产工单号
    productCode: string;      // 产品编码
    productName: string;      // 产品名称
    costCenterId: number;     // 归属成本中心ID
    wipQuantity: number;      // 月末在制品物理数量
    completionRate: number;   // 系统评估/人工修正完工程度 (%)
    equivalentQty: number;    // 约当产量
    status: number;           // 状态(0-待折算, 1-已折算)
    updateTime?: string;
  }
}

let mockData: MesCostEquivalentApi.EquivalentRecord[] = [
  { id: 1, period: '2026-02', workOrderNo: 'WO-2602-001', productCode: 'P-CNC-A1', productName: '精密金属结构件', costCenterId: 1111, wipQuantity: 500, completionRate: 60, equivalentQty: 0, status: 0, updateTime: '-' },
  { id: 2, period: '2026-02', workOrderNo: 'WO-2602-002', productCode: 'P-BAT-S1', productName: '锂电池前段浆料', costCenterId: 1211, wipQuantity: 2000, completionRate: 45, equivalentQty: 0, status: 0, updateTime: '-' },
  { id: 3, period: '2026-02', workOrderNo: 'WO-2602-005', productCode: 'P-ASSY-X', productName: '成品智能终端', costCenterId: 1121, wipQuantity: 150, completionRate: 80, equivalentQty: 0, status: 0, updateTime: '-' },
  { id: 4, period: '2026-01', workOrderNo: 'WO-2601-088', productCode: 'P-CNC-A1', productName: '精密金属结构件', costCenterId: 1111, wipQuantity: 300, completionRate: 50, equivalentQty: 150, status: 1, updateTime: '2026-01-31 23:50:00' },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getEquivalentPeriods() {
  await delay(200);
  return [
    { period: '2026-02', label: '2026年 02月', tag: '当期', isCalculated: false },
    { period: '2026-01', label: '2026年 01月', tag: '已结账', isCalculated: true },
    { period: '2025-12', label: '2025年 12月', tag: '已结账', isCalculated: true },
  ];
}

export async function getEquivalentPage(params: any) {
  await delay(300);
  let list = [...mockData];

  if (params.period) {
    list = list.filter(item => item.period === params.period);
  }
  if (params.workOrderNo) {
    list = list.filter(item => item.workOrderNo.includes(params.workOrderNo));
  }
  if (params.productName) {
    list = list.filter(item => item.productName.includes(params.productName));
  }

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

/** [新增] 独立保存人工修正的完工程度 */
export async function updateCompletionRate(id: number, rate: number) {
  await delay(100);
  const item = mockData.find(i => i.id === id);
  if (item) {
    item.completionRate = rate;
  }
  return { code: 200, data: true, msg: 'success' };
}

export async function executeEquivalentCalc(period: string) {
  await delay(800);
  let calculatedCount = 0;

  mockData = mockData.map(item => {
    if (item.period === period && item.status === 0) {
      calculatedCount++;
      return {
        ...item,
        equivalentQty: Math.round(item.wipQuantity * (item.completionRate / 100)),
        status: 1,
        updateTime: new Date().toLocaleString().replace(/\//g, '-')
      };
    }
    return item;
  });

  return { code: 200, data: calculatedCount, msg: `成功完成 ${calculatedCount} 条在制工单的约当量折算` };
}
