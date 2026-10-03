export namespace MesDailyApi {
  export interface DailySummary {
    plannedQty: { total: number; value: number };
    actualQty: { total: number; value: number };
    scrapQty: { total: number; value: number };
    yieldRate: { total: string; value: number };
  }

  export interface TrendData {
    dates: string[];
    actualOutput: number[];
    yieldRates: number[];
  }

  export interface DailyRecord {
    id: string;
    productionDate: string;
    shiftName: string;
    workOrderNo: string;
    itemCode: string;
    itemName: string;
    unit: string;
    plannedQty: number;
    actualQty: number;
    goodQty: number;
    scrapQty: number;
    yieldRate: number;
  }
}

// 模拟获取看板聚合数据与趋势图
export async function getDailyOverview(params: any): Promise<{ summary: MesDailyApi.DailySummary, trend: MesDailyApi.TrendData }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        summary: {
          plannedQty: { total: 4700, value: 5.2 },
          actualQty: { total: 4695, value: 4.8 },
          scrapQty: { total: 55, value: -1.2 },
          yieldRate: { total: '98.85%', value: 0.3 }
        },
        trend: {
          dates: ['02-19', '02-20', '02-21', '02-22', '02-23', '02-24', '02-25'],
          actualOutput: [4500, 4600, 4550, 4620, 4680, 4650, 4695],
          yieldRates: [98.2, 98.1, 98.5, 98.4, 98.6, 98.5, 98.85]
        }
      });
    }, 300);
  });
}

// 模拟获取表格分页明细
export async function getDailyPage(params: any): Promise<{ items: MesDailyApi.DailyRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        items: [
          { id: '1', productionDate: '2026-02-25', shiftName: '早班', workOrderNo: 'WO-260225-001', itemCode: 'HC-PAD-001', itemName: 'CMP精抛垫 300mm', unit: 'PCS', plannedQty: 1000, actualQty: 1020, goodQty: 1010, scrapQty: 10, yieldRate: 99.02 },
          { id: '2', productionDate: '2026-02-25', shiftName: '早班', workOrderNo: 'WO-260225-002', itemCode: 'HC-PAD-002', itemName: 'CMP精抛垫 200mm', unit: 'PCS', plannedQty: 2500, actualQty: 2480, goodQty: 2450, scrapQty: 30, yieldRate: 98.79 },
          { id: '3', productionDate: '2026-02-25', shiftName: '晚班', workOrderNo: 'WO-260225-003', itemCode: 'HC-PAD-001', itemName: 'CMP精抛垫 300mm', unit: 'PCS', plannedQty: 1200, actualQty: 1195, goodQty: 1180, scrapQty: 15, yieldRate: 98.74 }
        ],
        total: 45
      });
    }, 400);
  });
}
