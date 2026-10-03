import { requestClient } from '#/api/request';

export namespace MesAqlApi {
  // AQL 子表：批量范围与抽样判定规则
  export interface AqlRule {
    id?: number;
    aqlId?: number;
    minBatchSize: number; // 批量下限
    maxBatchSize: number; // 批量上限
    sampleCode: string;   // 样本量字码 (如: J, K, L)
    sampleSize: number;   // 抽样数量
    acValue: number;      // 接收数 (Accept)
    reValue: number;      // 拒收数 (Reject)
  }

  // AQL 主表
  export interface AqlStandard {
    id?: number;
    code: string;         // 方案代码
    name: string;         // 方案名称
    standardType: string; // 检验标准 (如 GB/T 2828.1)
    inspectLevel: string; // 检验水平 (如 I, II, III, S-1)
    aqlValue: string;     // AQL值 (如 0.65, 1.0, 2.5)
    status: number;       // 状态
    remark?: string;
    rules?: AqlRule[];
  }
}

// ==========================================
// 逼真的 Mock 数据 (基于 GB/T 2828.1 - 正常检验一次抽样方案)
// ==========================================
let nextId = 10;
let mockAqlData: MesAqlApi.AqlStandard[] = [
  {
    id: 1, code: 'AQL-II-2.5', name: '一般检验水平 II, AQL 2.5', standardType: 'GB/T 2828.1', inspectLevel: 'II', aqlValue: '2.5', status: 1, remark: 'COA出货常规抽样标准',
    rules: [
      { id: 101, minBatchSize: 2, maxBatchSize: 8, sampleCode: 'A', sampleSize: 2, acValue: 0, reValue: 1 },
      { id: 102, minBatchSize: 9, maxBatchSize: 15, sampleCode: 'B', sampleSize: 3, acValue: 0, reValue: 1 },
      { id: 103, minBatchSize: 16, maxBatchSize: 25, sampleCode: 'C', sampleSize: 5, acValue: 0, reValue: 1 },
      { id: 104, minBatchSize: 26, maxBatchSize: 50, sampleCode: 'D', sampleSize: 8, acValue: 0, reValue: 1 },
      { id: 105, minBatchSize: 51, maxBatchSize: 90, sampleCode: 'E', sampleSize: 13, acValue: 1, reValue: 2 },
      { id: 106, minBatchSize: 91, maxBatchSize: 150, sampleCode: 'F', sampleSize: 20, acValue: 1, reValue: 2 },
      { id: 107, minBatchSize: 151, maxBatchSize: 280, sampleCode: 'G', sampleSize: 32, acValue: 2, reValue: 3 },
      { id: 108, minBatchSize: 281, maxBatchSize: 500, sampleCode: 'H', sampleSize: 50, acValue: 3, reValue: 4 },
      { id: 109, minBatchSize: 501, maxBatchSize: 1200, sampleCode: 'J', sampleSize: 80, acValue: 5, reValue: 6 },
      { id: 110, minBatchSize: 1201, maxBatchSize: 3200, sampleCode: 'K', sampleSize: 125, acValue: 7, reValue: 8 },
      { id: 111, minBatchSize: 3201, maxBatchSize: 10000, sampleCode: 'L', sampleSize: 200, acValue: 10, reValue: 11 },
    ]
  },
  {
    id: 2, code: 'AQL-II-0.65', name: '一般检验水平 II, AQL 0.65', standardType: 'GB/T 2828.1', inspectLevel: 'II', aqlValue: '0.65', status: 1, remark: '进料高精密度物料抽检',
    rules: [
      { id: 201, minBatchSize: 151, maxBatchSize: 280, sampleCode: 'G', sampleSize: 32, acValue: 0, reValue: 1 },
      { id: 202, minBatchSize: 281, maxBatchSize: 500, sampleCode: 'H', sampleSize: 50, acValue: 1, reValue: 2 },
      { id: 203, minBatchSize: 501, maxBatchSize: 1200, sampleCode: 'J', sampleSize: 80, acValue: 1, reValue: 2 },
    ]
  }
];

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getAqlPage(params: any) {
  await sleep(300);
  let list = [...mockAqlData];
  if (params.name) list = list.filter(item => item.name.includes(params.name) || item.code.includes(params.name));
  return { list, total: list.length };
}

export async function getAql(id: number) {
  await sleep(150);
  const data = mockAqlData.find(item => item.id === id);
  if (!data) throw new Error('不存在');
  return JSON.parse(JSON.stringify(data));
}

export async function createAql(data: MesAqlApi.AqlStandard) {
  await sleep(300);
  mockAqlData.push({ ...data, id: nextId++ });
  return true;
}

export async function updateAql(data: MesAqlApi.AqlStandard) {
  await sleep(300);
  const index = mockAqlData.findIndex(item => item.id === data.id);
  if (index > -1) { mockAqlData[index] = { ...data }; return true; }
  throw new Error('更新失败');
}

export async function deleteAqlList(ids: number[]) {
  await sleep(200);
  mockAqlData = mockAqlData.filter(item => !ids.includes(item.id!));
  return true;
}
