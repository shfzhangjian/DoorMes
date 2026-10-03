// 文件路径：src/api/mes/cost/wage/distribution/index.ts

export namespace MesCostWageDistApi {
  export interface DistRecord {
    id: number;
    period: string;          // 核算期间
    teamId: string;          // 班组ID
    empNo: string;           // 员工工号
    empName: string;         // 员工姓名
    role: string;            // 角色
    baseHours: number;       // 有效核算工时 (A)
    weight: number;          // 分配权重 (B)
    weightedHours: number;   // 加权工时 (C = A * B)
    calcAmount: number;      // 系统分配金额 (D)
    adjustAmount: number;    // 手工微调金额 (E)
    finalAmount: number;     // 最终分配金额 (F = D + E)
    remark?: string;         // 备注
  }
}

// 模拟初始状态（尚未执行分配运算）
let mockData: MesCostWageDistApi.DistRecord[] = [
  { id: 1, period: '2026-02', teamId: 'T-1121-A', empNo: 'EMP-0012', empName: '张建国', role: '线长', baseHours: 208, weight: 1.2, weightedHours: 249.6, calcAmount: 0, adjustAmount: 0, finalAmount: 0 },
  { id: 2, period: '2026-02', teamId: 'T-1121-A', empNo: 'EMP-0056', empName: '李秀兰', role: '熟练操作工', baseHours: 200, weight: 1.0, weightedHours: 200.0, calcAmount: 0, adjustAmount: 0, finalAmount: 0 },
  { id: 3, period: '2026-02', teamId: 'T-1121-A', empNo: 'EMP-0088', empName: '王小明', role: '学徒', baseHours: 180, weight: 0.8, weightedHours: 144.0, calcAmount: 0, adjustAmount: 0, finalAmount: 0 },

  { id: 4, period: '2026-02', teamId: 'T-1111-01', empNo: 'EMP-0033', empName: '赵铁柱', role: 'CNC调机员', baseHours: 220, weight: 1.5, weightedHours: 330.0, calcAmount: 0, adjustAmount: 0, finalAmount: 0 },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getDistTeams() {
  await delay(200);
  return [
    { teamId: 'T-1121-A', teamName: '组装A线', poolAmount: 25000.00, isCalculated: false },
    { teamId: 'T-1111-01', teamName: 'CNC白班组', poolAmount: 18500.00, isCalculated: false },
  ];
}

export async function getDistPage(params: any) {
  await delay(300);
  let list = [...mockData];

  // 1. 过滤逻辑保持不变
  if (params.period) list = list.filter(item => item.period === params.period);
  if (params.teamId) list = list.filter(item => item.teamId === params.teamId);

  const total = list.length;

  // 2. 修复点：判断是否传入了分页参数。如果没有传，则直接返回全量 list
  const pageNo = params.pageNo || params.page; // 兼容可能叫 page 的情况
  const pageSize = params.pageSize;

  if (pageNo && pageSize) {
    const start = (pageNo - 1) * pageSize;
    const end = start + pageSize;
    return { list: list.slice(start, end), total };
  }

  // 禁用分页时，直接返回全部过滤后的数据
  return { list, total };
}

/** 核心计算引擎：执行班组计件二次分配 */
export async function executeDistribution(period: string, teamId: string, poolAmount: number) {
  await delay(600);

  // 1. 过滤出该班组当期人员
  const teamMembers = mockData.filter(item => item.period === period && item.teamId === teamId);
  if (teamMembers.length === 0) return { code: 500, msg: '该班组无考勤人员数据' };

  // 2. 算总加权工时
  const totalWeightedHours = teamMembers.reduce((sum, item) => sum + item.weightedHours, 0);

  // 3. 执行切分
  mockData = mockData.map(item => {
    if (item.period === period && item.teamId === teamId) {
      // 个人分配额 = 总奖金池 * (个人加权工时 / 总加权工时)
      const allocated = (poolAmount * (item.weightedHours / totalWeightedHours));
      return {
        ...item,
        calcAmount: Number(allocated.toFixed(2)),
        finalAmount: Number((allocated + item.adjustAmount).toFixed(2)) // 如果之前有微调，保留微调
      };
    }
    return item;
  });

  return { code: 200, data: true, msg: '分配计算完成' };
}

/** 行内保存手工微调金额 */
export async function updateAdjustAmount(id: number, adjustAmount: number) {
  await delay(100);
  const item = mockData.find(i => i.id === id);
  if (item) {
    item.adjustAmount = adjustAmount || 0;
    item.finalAmount = item.calcAmount + item.adjustAmount;
  }
  return { code: 200, data: true, msg: 'success' };
}
