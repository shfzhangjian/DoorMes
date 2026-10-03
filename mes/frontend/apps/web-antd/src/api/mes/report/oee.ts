export namespace MesOeeApi {
  export interface OeeOverview {
    avgOee: number;
    avgAvailability: number;
    avgPerformance: number;
    avgQuality: number;
    trend: {
      dates: string[];
      oee: number[];
      availability: number[];
      performance: number[];
      quality: number[];
    };
  }

  export interface OeeRecord {
    id: string;
    equipCode: string;
    equipName: string;
    plannedTime: number; // 计划运行时间(H)
    actualRunTime: number; // 实际运行时间(H)
    downTime: number; // 停机时间(H)
    totalOutput: number; // 总产量
    goodOutput: number; // 合格产量
    availability: number; // 时间开动率(%)
    performance: number; // 性能开动率(%)
    quality: number; // 合格率(%)
    oee: number; // 综合效率(%)
  }
}

// 模拟获取 OEE 看板聚合数据与趋势图
export async function getOeeOverview(params: any): Promise<MesOeeApi.OeeOverview> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        avgOee: 82.4,
        avgAvailability: 88.5,
        avgPerformance: 94.2,
        avgQuality: 98.8,
        trend: {
          dates: ['02-19', '02-20', '02-21', '02-22', '02-23', '02-24', '02-25'],
          oee: [81.2, 80.5, 82.1, 81.8, 83.5, 82.0, 82.4],
          availability: [87.5, 86.8, 88.2, 88.0, 89.5, 88.1, 88.5],
          performance: [93.8, 93.5, 94.0, 93.9, 94.5, 94.1, 94.2],
          quality: [98.9, 98.8, 99.0, 98.9, 98.7, 99.1, 98.8]
        }
      });
    }, 300);
  });
}

// 模拟获取设备 OEE 明细表格分页
export async function getOeePage(params: any): Promise<{ items: MesOeeApi.OeeRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        items: [
          { id: '1', equipCode: 'COAT-01', equipName: '一号精密涂布机', plannedTime: 24, actualRunTime: 21.5, downTime: 2.5, totalOutput: 12000, goodOutput: 11850, availability: 89.58, performance: 95.20, quality: 98.75, oee: 84.22 },
          { id: '2', equipCode: 'POL-02', equipName: '二号双面精抛机', plannedTime: 24, actualRunTime: 20.0, downTime: 4.0, totalOutput: 8500, goodOutput: 8450, availability: 83.33, performance: 92.50, quality: 99.41, oee: 76.63 },
          { id: '3', equipCode: 'MIX-01', equipName: '高剪切配料釜', plannedTime: 24, actualRunTime: 22.8, downTime: 1.2, totalOutput: 5000, goodOutput: 4980, availability: 95.00, performance: 98.00, quality: 99.60, oee: 92.73 },
          { id: '4', equipCode: 'CLEAN-03', equipName: '超声波清洗线', plannedTime: 24, actualRunTime: 21.0, downTime: 3.0, totalOutput: 11500, goodOutput: 11300, availability: 87.50, performance: 91.00, quality: 98.26, oee: 78.24 }
        ],
        total: 12
      });
    }, 400);
  });
}
