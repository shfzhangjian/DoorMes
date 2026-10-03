export namespace MesScrapApi {
  export interface DefectReason {
    reason: string;
    qty: number;
    cumulativeRate: number; // 累计占比，用于绘制柏拉图
  }

  export interface ScrapOverview {
    avgScrapRate: number;
    totalScrapQty: number;
    topDefectReason: string;
    estScrapCost: number; // 预估报废成本
    trend: {
      dates: string[];
      scrapQty: number[];
      scrapRate: number[];
    };
    pareto: DefectReason[];
  }

  export interface ScrapRecord {
    id: string;
    productionDate: string;
    workOrderNo: string;
    materialCode: string;
    materialName: string;
    processName: string;
    totalQty: number;
    scrapQty: number;
    scrapRate: number;
    mainDefect: string;
  }
}

// 模拟获取看板聚合数据与图表数据
export async function getScrapOverview(params: any): Promise<MesScrapApi.ScrapOverview> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        avgScrapRate: 1.25,
        totalScrapQty: 425,
        topDefectReason: '表面划伤',
        estScrapCost: 12500,
        trend: {
          dates: ['02-19', '02-20', '02-21', '02-22', '02-23', '02-24', '02-25'],
          scrapQty: [55, 62, 48, 70, 65, 50, 75],
          scrapRate: [1.1, 1.2, 0.9, 1.4, 1.3, 1.0, 1.5]
        },
        pareto: [
          { reason: '表面划伤', qty: 180, cumulativeRate: 42.3 },
          { reason: '尺寸超差', qty: 110, cumulativeRate: 68.2 },
          { reason: '厚度偏薄', qty: 75, cumulativeRate: 85.8 },
          { reason: '涂层气泡', qty: 40, cumulativeRate: 95.2 },
          { reason: '其他', qty: 20, cumulativeRate: 100.0 }
        ]
      });
    }, 300);
  });
}

// 模拟获取废品明细表格分页
export async function getScrapPage(params: any): Promise<{ items: MesScrapApi.ScrapRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        items: [
          { id: '1', productionDate: '2026-02-25', workOrderNo: 'WO-260225-001', materialCode: 'HC-PAD-300', materialName: 'CMP精抛垫 300mm', processName: '表面精抛', totalQty: 1000, scrapQty: 25, scrapRate: 2.50, mainDefect: '表面划伤' },
          { id: '2', productionDate: '2026-02-25', workOrderNo: 'WO-260225-002', materialCode: 'HC-PAD-200', materialName: 'CMP精抛垫 200mm', processName: '基材涂布', totalQty: 2500, scrapQty: 30, scrapRate: 1.20, mainDefect: '涂层气泡' },
          { id: '3', productionDate: '2026-02-24', workOrderNo: 'WO-260224-015', materialCode: 'HC-PAD-300', materialName: 'CMP精抛垫 300mm', processName: '精密分切', totalQty: 1200, scrapQty: 42, scrapRate: 3.50, mainDefect: '尺寸超差' },
          { id: '4', productionDate: '2026-02-24', workOrderNo: 'WO-260224-018', materialCode: 'HC-CLO-150', materialName: '精密阻尼布', processName: '压延成型', totalQty: 800, scrapQty: 8, scrapRate: 1.00, mainDefect: '厚度偏薄' }
        ],
        total: 58
      });
    }, 400);
  });
}
