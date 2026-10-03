import { requestClient } from '#/api/request';

export namespace MesSetupRuleApi {
  export interface RuleItem {
    id?: number;
    ruleId?: number;
    materialCode: string;
    materialName: string;
    requiredQty: number;
    allowSubstitute: boolean;
  }

  export interface Rule {
    id?: number;
    ruleNo: string;
    productName: string;
    processCode: string;
    status: number;
    remark?: string;
    createTime?: string;
    items?: RuleItem[];
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    productName?: string;
    processCode?: string;
  }
}

// 模拟数据
let nextId = 1000;
let nextItemId = 5000;
let mockData: MesSetupRuleApi.Rule[] = Array.from({ length: 12 }).map((_, index) => ({
  id: nextId++,
  ruleNo: `SR-2026-${String(index + 1).padStart(4, '0')}`,
  productName: index < 6 ? '电子显示高透抛光垫 (Pro)' : '纳米级氧化铈抛光液',
  processCode: index % 2 === 0 ? 'MIXING' : 'COATING',
  status: 1,
  remark: '系统默认BOM防错规则',
  createTime: '2026-02-24 10:00:00',
  items: [
    { id: nextItemId++, materialCode: 'RM-PU-001', materialName: '聚氨酯基材', requiredQty: 1, allowSubstitute: false },
    { id: nextItemId++, materialCode: 'RM-AB-002', materialName: '高标号研磨微粉', requiredQty: 2.5, allowSubstitute: true },
  ]
}));

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getRulePage(params: MesSetupRuleApi.PageReq) {
  await sleep(300);
  const { pageNo, pageSize, productName, processCode } = params;
  let list = [...mockData];
  if (productName) list = list.filter(i => i.productName.includes(productName));
  if (processCode) list = list.filter(i => i.processCode === processCode);

  const total = list.length;
  const pageList = list.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return { list: pageList, total };
}

export async function getRule(id: number) {
  await sleep(200);
  const data = mockData.find(item => item.id === id);
  if (!data) throw new Error('规则不存在');
  return JSON.parse(JSON.stringify(data)) as MesSetupRuleApi.Rule;
}

export async function deleteRuleList(ids: number[]) {
  await sleep(300);
  mockData = mockData.filter(i => !ids.includes(i.id!));
  return true;
}

export async function createRule(data: MesSetupRuleApi.Rule) {
  await sleep(400);
  const newRule = {
    ...data,
    id: nextId++,
    ruleNo: `SR-NEW-${new Date().getTime().toString().slice(-4)}`,
    createTime: new Date().toLocaleString(),
  };
  if (newRule.items) newRule.items.forEach(item => { item.id = nextItemId++; });
  mockData.unshift(newRule);
  return newRule.id;
}

export async function updateRule(data: MesSetupRuleApi.Rule) {
  await sleep(400);
  const index = mockData.findIndex(item => item.id === data.id);
  if (index > -1) {
    if (data.items) data.items.forEach(item => { if (!item.id) item.id = nextItemId++; });
    mockData[index] = JSON.parse(JSON.stringify(data));
  }
}
