// 文件路径：src/api/mes/cost/wage/monthly/index.ts

export namespace MesCostWageMonthApi {
  export interface MonthlyWage {
    id: number;
    period: string;
    empNo: string;
    empName: string;
    teamName: string;
    directPiece: number;
    teamPiece: number;
    timeBased: number;
    overtime: number;
    subsidy: number;          // 可人工修正
    reward: number;           // 可人工修正
    punish: number;           // 可人工修正
    grossWage: number;
    status: number;
  }
}

let mockData: MesCostWageMonthApi.MonthlyWage[] = [
  { id: 1, period: '2026-02', empNo: 'EMP-0012', empName: '张建国', teamName: '组装A线', directPiece: 850.00, teamPiece: 4120.50, timeBased: 2200.00, overtime: 850.00, subsidy: 0, reward: 200.00, punish: 0, grossWage: 8220.50, status: 0 },
  { id: 2, period: '2026-02', empNo: 'EMP-0056', empName: '李秀兰', teamName: '组装A线', directPiece: 600.00, teamPiece: 3550.00, timeBased: 2100.00, overtime: 420.00, subsidy: 0, reward: 0, punish: 50.00, grossWage: 6620.00, status: 0 },
  { id: 3, period: '2026-02', empNo: 'EMP-0033', empName: '赵铁柱', teamName: 'CNC白班组', directPiece: 5400.00, teamPiece: 0, timeBased: 1800.00, overtime: 1200.00, subsidy: 300.00, reward: 100.00, punish: 0, grossWage: 8800.00, status: 1 },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getMonthlyWagePage(params: any) {
  await delay(400);
  let list = [...mockData];

  if (params.period) list = list.filter(item => item.period === params.period);
  if (params.teamName) list = list.filter(item => item.teamName.includes(params.teamName));
  if (params.empName) list = list.filter(item => item.empName.includes(params.empName) || item.empNo.includes(params.empName));
  if (params.status !== undefined && params.status !== '') list = list.filter(item => String(item.status) === String(params.status));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

export async function calculateMonthlyWage(period: string) {
  await delay(1200);
  return { code: 200, data: true, msg: `${period} 期间生产薪酬重算完成！` };
}

export async function auditMonthlyWage(period: string, ids: number[]) {
  await delay(400);
  let count = 0;
  mockData = mockData.map(item => {
    if (item.period === period && ids.includes(item.id) && item.status === 0) {
      count++;
      return { ...item, status: 1 };
    }
    return item;
  });
  return { code: 200, data: count, msg: `成功封账锁定 ${count} 条薪资记录。` };
}

/** 新增：单行字段手工修正 */
export async function updateMonthlyWageItem(id: number, field: string, value: number) {
  await delay(100);
  const item = mockData.find(i => i.id === id);
  if (item && item.status === 0) {
    (item as any)[field] = value || 0;
    // 后端同步重算该条记录的总薪资
    item.grossWage = item.directPiece + item.teamPiece + item.timeBased + item.overtime + item.subsidy + item.reward - item.punish;
  }
  return { code: 200, data: true, msg: 'success' };
}
