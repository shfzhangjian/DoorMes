export namespace MesShiftPerfApi {
  export interface PerfOverview {
    topTeam: string;
    avgAchieveRate: number;
    avgYieldRate: number;
    totalDefectQty: number;
    trend: {
      teams: string[];
      actualOutput: number[];
      achieveRates: number[];
    };
  }

  export interface PerfRecord {
    id: string;
    productionDate: string;
    shiftName: string;
    teamName: string;
    teamLeader: string;
    targetQty: number;
    actualQty: number;
    goodQty: number;
    defectQty: number;
    yieldRate: number;
    achieveRate: number;
  }
}

// 模拟获取看板聚合数据与班组横向对比图
export async function getShiftPerfOverview(params: any): Promise<MesShiftPerfApi.PerfOverview> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        topTeam: 'A班组 (早班)',
        avgAchieveRate: 98.5,
        avgYieldRate: 99.1,
        totalDefectQty: 142,
        trend: {
          teams: ['A班组', 'B班组', 'C班组', 'D班组'],
          actualOutput: [15200, 14800, 15050, 14600],
          achieveRates: [102.5, 96.8, 99.5, 95.4]
        }
      });
    }, 300);
  });
}

// 模拟获取班组绩效明细表格分页
export async function getShiftPerfPage(params: any): Promise<{ items: MesShiftPerfApi.PerfRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        items: [
          { id: '1', productionDate: '2026-02-25', shiftName: '早班', teamName: 'A班组', teamLeader: '张建国', targetQty: 5000, actualQty: 5120, goodQty: 5080, defectQty: 40, yieldRate: 99.21, achieveRate: 102.4 },
          { id: '2', productionDate: '2026-02-25', shiftName: '晚班', teamName: 'B班组', teamLeader: '李明', targetQty: 5000, actualQty: 4850, goodQty: 4790, defectQty: 60, yieldRate: 98.76, achieveRate: 97.0 },
          { id: '3', productionDate: '2026-02-24', shiftName: '早班', teamName: 'C班组', teamLeader: '王强', targetQty: 5000, actualQty: 5050, goodQty: 5015, defectQty: 35, yieldRate: 99.30, achieveRate: 101.0 },
          { id: '4', productionDate: '2026-02-24', shiftName: '晚班', teamName: 'D班组', teamLeader: '赵斌', targetQty: 5000, actualQty: 4900, goodQty: 4850, defectQty: 50, yieldRate: 98.97, achieveRate: 98.0 }
        ],
        total: 24
      });
    }, 400);
  });
}
