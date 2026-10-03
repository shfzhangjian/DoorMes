import { requestClient } from '#/api/request';

export namespace MesProcessConfigApi {
  export interface Config {
    id?: number;
    productName: string;
    paramName: string;
    paramUnit: string;
    minValue: number;
    maxValue: number;
    isRequired: number;
    status: number;
    remark?: string;
    createTime?: string;
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    productName?: string;
  }
}

// 模拟数据 10 条
let mockData: MesProcessConfigApi.Config[] = Array.from({ length: 10 }).map((_, index) => ({
  id: 874404 + index,
  productName: '电子显示高透抛光垫 (Pro)',
  paramName: ['主轴转速', '加工压力', '槽液PH', '循环流量', '环境湿度'][index % 5],
  paramUnit: ['rpm', 'kPa', 'pH', 'L/min', '%'][index % 5],
  minValue: 100 * (index + 1),
  maxValue: 200 * (index + 1),
  isRequired: 1,
  status: 1,
  createTime: '2024-03-20 10:00:00',
  remark: '系统初始化配置',
}));

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getConfigPage(params: MesProcessConfigApi.PageReq) {
  await sleep(300);
  const { pageNo, pageSize, productName } = params;
  let list = [...mockData];
  if (productName) list = list.filter(i => i.productName.includes(productName));

  const total = list.length;
  const pageList = list.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  // 💡 严格对齐您的规范：返回 list 字段
  return { list: pageList, total };
}

export async function getConfig(id: number) {
  await sleep(200);
  return mockData.find(i => i.id === id);
}

export async function deleteConfigs(ids: number[]) {
  mockData = mockData.filter(i => !ids.includes(i.id!));
  return true;
}

export async function createConfig(data: MesProcessConfigApi.Config) {
  mockData.unshift({ ...data, id: Date.now(), createTime: '2024-03-20 12:00:00' });
}

export async function updateConfig(data: MesProcessConfigApi.Config) {
  const index = mockData.findIndex(i => i.id === data.id);
  if (index > -1) mockData[index] = { ...mockData[index], ...data };
}
