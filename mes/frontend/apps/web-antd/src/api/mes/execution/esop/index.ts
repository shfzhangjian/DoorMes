import { requestClient } from '#/api/request';

// ==================================================================================
// 1. 类型定义
// ==================================================================================
export namespace MesEsopApi {
  export interface Esop {
    id?: number;
    docNo: string;           // 文件编号
    docName: string;         // 文件名称
    version: string;         // 版本号
    productName: string;     // 适用产品
    processCode: string;     // 适用工序
    fileUrl: string;         // 附件地址
    status: number;          // 状态 (0:作废, 1:启用)
    remark?: string;         // 备注说明
    createTime?: string;
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    docName?: string;
    productName?: string;
    processCode?: string;
  }
}

// ==================================================================================
// 2. Mock 数据与接口模拟
// ==================================================================================
let nextId = 2000;
let mockData: MesEsopApi.Esop[] = Array.from({ length: 8 }).map((_, index) => ({
  id: nextId++,
  docNo: `SOP-2026-${String(index + 1).padStart(3, '0')}`,
  docName: index < 4 ? '高透垫切片作业规范' : '研磨液调配安全指导',
  version: `V1.${index % 3}`,
  productName: index < 4 ? '电子显示高透抛光垫 (Pro)' : '纳米级氧化铈抛光液',
  processCode: index < 4 ? 'SLITTING' : 'MIXING',
  fileUrl: 'https://example.com/dummy-sop.pdf', // 模拟文件地址
  status: index === 3 ? 0 : 1, // 模拟一个作废状态
  remark: '请操作工严格按照此版本执行',
  createTime: '2026-02-24 09:00:00',
}));

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getEsopPage(params: MesEsopApi.PageReq) {
  await sleep(300);
  const { pageNo, pageSize, docName, productName, processCode } = params;
  let list = [...mockData];
  if (docName) list = list.filter(i => i.docName.includes(docName));
  if (productName) list = list.filter(i => i.productName.includes(productName));
  if (processCode) list = list.filter(i => i.processCode === processCode);

  const total = list.length;
  const pageList = list.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return { list: pageList, total };
}

export async function getEsop(id: number) {
  await sleep(200);
  const data = mockData.find(item => item.id === id);
  if (!data) throw new Error('指导书不存在');
  return JSON.parse(JSON.stringify(data)) as MesEsopApi.Esop;
}

export async function deleteEsopList(ids: number[]) {
  await sleep(300);
  mockData = mockData.filter(i => !ids.includes(i.id!));
  return true;
}

export async function createEsop(data: MesEsopApi.Esop) {
  await sleep(400);
  const newRow = {
    ...data,
    id: nextId++,
    docNo: `SOP-NEW-${new Date().getTime().toString().slice(-4)}`,
    createTime: new Date().toLocaleString(),
  };
  mockData.unshift(newRow);
  return newRow.id;
}

export async function updateEsop(data: MesEsopApi.Esop) {
  await sleep(400);
  const index = mockData.findIndex(item => item.id === data.id);
  if (index > -1) {
    mockData[index] = JSON.parse(JSON.stringify(data));
  }
}
