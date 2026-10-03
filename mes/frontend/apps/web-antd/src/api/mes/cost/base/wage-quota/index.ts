// 文件路径：src/api/mes/cost/base/wage-quota/index.ts

export namespace MesCostWageApi {
  export interface WageQuota {
    id: number;
    productCode: string;     // 产品编码
    productName: string;     // 产品名称
    processCode: string;     // 工序编码
    processName: string;     // 工序名称
    wageType: number;        // 计薪方式 (1-计件单价, 2-标准计时费率)
    unitPrice: number;       // 定额单价 (元)
    version: string;         // 版本号 (如 V1.0, V2.0)
    effectiveDate: string;   // 生效日期 (YYYY-MM-DD)
    expireDate?: string;     // 失效日期 (YYYY-MM-DD, 留空代表长期有效)
    status: number;          // 状态 (0-草稿, 1-已生效, 2-历史版本)
    remark?: string;
  }
}

// 初始数据：包含一个已生效版本和一个历史版本
let mockData: MesCostWageApi.WageQuota[] = [
  { id: 1, productCode: 'P-CNC-A1', productName: '精密金属结构件', processCode: 'OP-10', processName: '粗加工', wageType: 1, unitPrice: 2.50, version: 'V2.0', effectiveDate: '2026-02-01', expireDate: '', status: 1, remark: '2026年工艺优化后提价' },
  { id: 2, productCode: 'P-CNC-A1', productName: '精密金属结构件', processCode: 'OP-10', processName: '粗加工', wageType: 1, unitPrice: 2.20, version: 'V1.0', effectiveDate: '2025-01-01', expireDate: '2026-01-31', status: 2, remark: '初版定额' },
  { id: 3, productCode: 'P-ASSY-X', productName: '成品智能终端', processCode: 'OP-90', processName: '总装', wageType: 2, unitPrice: 35.00, version: 'V1.0', effectiveDate: '2025-06-01', expireDate: '', status: 1, remark: '按小时计费，35元/H' },
  { id: 4, productCode: 'P-BAT-S1', productName: '锂电池前段浆料', processCode: 'OP-20', processName: '搅拌', wageType: 1, unitPrice: 15.00, version: 'V1.0', effectiveDate: '2026-03-01', expireDate: '', status: 0, remark: '下月拟执行新单价草稿' },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getWageQuotaPage(params: any) {
  await delay(300);
  let list = [...mockData];
  if (params.productName) list = list.filter(item => item.productName.includes(params.productName) || item.productCode.includes(params.productName));
  if (params.processName) list = list.filter(item => item.processName.includes(params.processName));
  if (params.status !== undefined && params.status !== '') list = list.filter(item => String(item.status) === String(params.status));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

export async function getWageQuota(id: number) {
  await delay(200);
  return mockData.find(item => item.id === id) || null;
}

export async function createWageQuota(data: MesCostWageApi.WageQuota) {
  await delay(300);
  data.id = Math.floor(Math.random() * 10000);
  data.status = 0; // 新增强制为草稿
  if (!data.version) data.version = 'V1.0';
  mockData.unshift(data);
  return { code: 200, data: true, msg: 'success' };
}

export async function updateWageQuota(data: MesCostWageApi.WageQuota) {
  await delay(300);
  const index = mockData.findIndex(item => item.id === data.id);
  if (index > -1 && mockData[index].status === 0) {
    mockData[index] = { ...mockData[index], ...data };
  }
  return { code: 200, data: true, msg: 'success' };
}

export async function deleteWageQuota(ids: number[]) {
  await delay(300);
  // 仅允许删除草稿
  mockData = mockData.filter(item => !(ids.includes(item.id) && item.status === 0));
  return { code: 200, data: true, msg: 'success' };
}

/** 核心业务：发布生效版本，并自动作废同产品同工序的历史版本 */
export async function enableWageQuota(id: number) {
  await delay(400);
  const target = mockData.find(item => item.id === id);
  if (!target || target.status !== 0) return { code: 500, msg: '仅草稿状态可发布' };

  const today = new Date().toISOString().slice(0, 10);

  // 查找同产品同工序的当前生效版本，将其截断
  mockData.forEach(item => {
    if (item.id !== id && item.productCode === target.productCode && item.processCode === target.processCode && item.status === 1) {
      item.status = 2; // 变为历史版本
      // 失效日期设为今天之前（逻辑简化，实际ERP中精确到秒或前一天）
      item.expireDate = today;
    }
  });

  target.status = 1; // 激活新版本
  return { code: 200, data: true, msg: '定额版本已生效，原版本已自动失效' };
}
