// 文件路径：src/api/mes/cost/base/allocation-rule/index.ts

import { requestClient } from '#/api/request';

export namespace MesCostRuleApi {
  /** 成本分摊规则信息 */
  export interface Rule {
    id: number;
    ruleCode: string; // 规则编号
    ruleName: string; // 规则名称
    costCenterId: number; // 关联成本中心ID
    method: number; // 分摊方法(1-生产工时法, 2-机器工时法, 3-按投入套数, 4-标准成本/计划分配率)
    isDefault: boolean; // 是否默认规则
    status: number; // 状态(0正常, 1停用)
    remark?: string; // 备注说明
    createTime?: string;
  }
}

// ================= Mock 数据模拟区域 =================
// 注意：costCenterId 严格对应之前定义的成本中心 Mock 数据的 ID
const mockData: MesCostRuleApi.Rule[] = [
  {
    id: 1,
    ruleCode: 'R-MACH-01',
    ruleName: 'CNC机器工时分摊',
    costCenterId: 1111, // 对应: CNC柔性加工中心
    method: 2, // 机器工时比例法
    isDefault: true,
    status: 0,
    remark: '按设备稼动时间比例自动分摊车间折旧与电费',
    createTime: '2026-02-27 11:00:00',
  },
  {
    id: 2,
    ruleCode: 'R-ASSY-01',
    ruleName: 'A线人工工时分摊',
    costCenterId: 1121, // 对应: 人工组装A线
    method: 1, // 生产工时比例法
    isDefault: true,
    status: 0,
    remark: '按员工实际报工时间分摊人工成本',
    createTime: '2026-02-27 11:05:00',
  },
  {
    id: 3,
    ruleCode: 'R-MIX-01',
    ruleName: '制浆标准成本分摊',
    costCenterId: 1211, // 对应: 制浆连续产线
    method: 4, // 标准成本/计划分配率
    isDefault: true,
    status: 0,
    remark: '年度预算费率，期末调整差异',
    createTime: '2026-02-27 11:10:00',
  },
  {
    id: 4,
    ruleCode: 'R-SMT-01',
    ruleName: 'SMT折旧工时分摊',
    costCenterId: 1221, // 对应: SMT高速贴片1线
    method: 2, // 机器工时比例法
    isDefault: true,
    status: 0,
    createTime: '2026-02-27 11:15:00',
  },
];

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** 分页查询分摊规则列表 (Mock) */
export async function getCostRulePage(params: any) {
  await delay(300);
  let list = [...mockData];

  // 条件过滤 (处理类型转化的强校验)
  if (params.ruleName) {
    list = list.filter((item) => item.ruleName.includes(params.ruleName));
  }
  if (params.ruleCode) {
    list = list.filter((item) => item.ruleCode.includes(params.ruleCode));
  }
  if (params.costCenterId) {
    list = list.filter((item) => String(item.costCenterId) === String(params.costCenterId));
  }
  if (params.method) {
    list = list.filter((item) => String(item.method) === String(params.method));
  }

  // 分页计算
  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  const pageList = list.slice(start, end);

  // 【核心修复】：必须返回 list 而不是 items！
  return { list: pageList, total };
}

/** 查询分摊规则详情 (Mock) */
export async function getCostRule(id: number) {
  await delay(200);
  return mockData.find((item) => item.id === id) || null;
}

/** 新增分摊规则 (Mock) */
export async function createCostRule(data: MesCostRuleApi.Rule) {
  await delay(300);
  console.log('Mock API -> Create Cost Rule:', data);
  return { code: 200, data: true, msg: 'success' };
}

/** 修改分摊规则 (Mock) */
export async function updateCostRule(data: MesCostRuleApi.Rule) {
  await delay(300);
  console.log('Mock API -> Update Cost Rule:', data);
  return { code: 200, data: true, msg: 'success' };
}

/** 删除分摊规则 (Mock) */
export async function deleteCostRule(id: number) {
  await delay(300);
  console.log('Mock API -> Delete Cost Rule ID:', id);
  return { code: 200, data: true, msg: 'success' };
}

/** 导出分摊规则 (Mock) */
export async function exportCostRule(_params: any) {
  await delay(500);
  return new Blob(['mock excel data'], { type: 'application/vnd.ms-excel' });
}
