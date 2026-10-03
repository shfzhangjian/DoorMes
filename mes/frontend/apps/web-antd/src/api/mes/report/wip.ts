// src/api/mes/report/wip.ts
export namespace MesWipApi {
  export interface WipOverview {
    totalQty: number;
    bottleneckProcess: string;
    bottleneckQty: number;
    holdLotCount: number;
    avgQueueTime: number; // 平均滞留时间(小时)
  }

  export interface ProcessDistribution {
    processName: string;
    waitQty: number; // 等待加工
    runQty: number;  // 正在加工
  }

  export interface WipLotRecord {
    id: string;
    batchNo: string;
    materialName: string;
    processName: string;
    equipCode: string;
    status: 'WAIT' | 'RUN' | 'HOLD';
    qty: number;
    unit: string;
    queueTime: number; // 滞留时长(H)
  }
}

// 模拟获取 WIP 聚合看板数据
export async function getWipOverview(): Promise<{ overview: MesWipApi.WipOverview, distribution: MesWipApi.ProcessDistribution[] }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        overview: {
          totalQty: 12540,
          bottleneckProcess: '表面精抛',
          bottleneckQty: 4200,
          holdLotCount: 5,
          avgQueueTime: 4.2
        },
        distribution: [
          { processName: '原料配制', waitQty: 500, runQty: 1200 },
          { processName: '基材涂布', waitQty: 800, runQty: 1500 },
          { processName: '表面精抛', waitQty: 3200, runQty: 1000 }, // 瓶颈工序
          { processName: '精密清洗', waitQty: 400, runQty: 800 },
          { processName: 'FQC全检', waitQty: 2500, runQty: 640 }
        ]
      });
    }, 300);
  });
}

// 模拟获取 WIP 批次明细分页数据
export async function getWipPage(params: any): Promise<{ items: MesWipApi.WipLotRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const mockData: MesWipApi.WipLotRecord[] = [
        { id: '1', batchNo: 'LOT-260225-001', materialName: '300mm CMP 精抛垫', processName: '表面精抛', equipCode: 'POL-01', status: 'RUN', qty: 200, unit: 'PCS', queueTime: 1.5 },
        { id: '2', batchNo: 'LOT-260225-002', materialName: '300mm CMP 精抛垫', processName: '表面精抛', equipCode: '-', status: 'WAIT', qty: 500, unit: 'PCS', queueTime: 6.2 },
        { id: '3', batchNo: 'LOT-260224-088', materialName: '200mm 特种抛光垫', processName: '基材涂布', equipCode: 'COAT-02', status: 'HOLD', qty: 150, unit: 'PCS', queueTime: 24.5 },
        { id: '4', batchNo: 'LOT-260225-005', materialName: '高纯抛光液原液', processName: '原料配制', equipCode: 'MIX-03', status: 'RUN', qty: 1000, unit: 'KG', queueTime: 0.5 },
      ];
      resolve({ items: mockData, total: 45 });
    }, 400);
  });
}
