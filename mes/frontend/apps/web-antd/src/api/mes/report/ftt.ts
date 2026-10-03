export namespace MesFttApi {
  export interface FttOverview {
    avgFtt: number;           // 综合一次合格率
    totalInputQty: number;    // 总投入量
    firstPassQty: number;     // 一次合格总数
    reworkScrapQty: number;   // 返工与报废总数
    trend: {
      dates: string[];
      inputQty: number[];
      fttRate: number[];
    };
  }

  export interface FttRecord {
    id: string;
    productionDate: string;
    workOrderNo: string;
    materialCode: string;
    materialName: string;
    processName: string;
    inputQty: number;      // 投入数量
    firstPassQty: number;  // 一次合格数量
    reworkQty: number;     // 返工数量
    scrapQty: number;      // 报废数量
    fttRate: number;       // 一次合格率
  }
}

// 模拟获取 FTT 看板聚合数据与趋势图
export async function getFttOverview(params: any): Promise<MesFttApi.FttOverview> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        avgFtt: 97.5,
        totalInputQty: 58000,
        firstPassQty: 56550,
        reworkScrapQty: 1450,
        trend: {
          dates: ['02-19', '02-20', '02-21', '02-22', '02-23', '02-24', '02-25'],
          inputQty: [8000, 8200, 7800, 8500, 8400, 8600, 8500],
          fttRate: [96.5, 96.8, 97.2, 98.1, 97.5, 98.5, 97.9]
        }
      });
    }, 300);
  });
}

// 模拟获取 FTT 明细表格分页
export async function getFttPage(params: any): Promise<{ items: MesFttApi.FttRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        items: [
          { id: '1', productionDate: '2026-02-25', workOrderNo: 'WO-260225-001', materialCode: 'HC-PAD-300', materialName: 'CMP精抛垫 300mm', processName: '表面精抛', inputQty: 2000, firstPassQty: 1980, reworkQty: 15, scrapQty: 5, fttRate: 99.00 },
          { id: '2', productionDate: '2026-02-25', workOrderNo: 'WO-260225-002', materialCode: 'HC-PAD-200', materialName: 'CMP精抛垫 200mm', processName: '基材涂布', inputQty: 3500, firstPassQty: 3350, reworkQty: 120, scrapQty: 30, fttRate: 95.71 },
          { id: '3', productionDate: '2026-02-24', workOrderNo: 'WO-260224-015', materialCode: 'HC-SLU-050', materialName: '高纯度氧化硅抛光液', processName: '原料配制', inputQty: 5000, firstPassQty: 4950, reworkQty: 0, scrapQty: 50, fttRate: 99.00 },
          { id: '4', productionDate: '2026-02-24', workOrderNo: 'WO-260224-018', materialCode: 'HC-CLO-150', materialName: '精密阻尼布', processName: '压延成型', inputQty: 1500, firstPassQty: 1425, reworkQty: 50, scrapQty: 25, fttRate: 95.00 }
        ],
        total: 124
      });
    }, 400);
  });
}
