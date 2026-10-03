import { requestClient } from '#/api/request';

// ==================================================================================
// 1. 类型定义
// ==================================================================================
export namespace MesHandoverApi {
  export interface HandoverItem {
    id?: number;
    handoverId?: number;
    checkItem: string;       // 检查项目 (如：设备状态、5S、物料结余)
    checkResult: 'OK' | 'NG';// 检查结果
    remark?: string;         // 异常说明
  }

  export interface Handover {
    id?: number;
    handoverNo: string;      // 交接单号
    workstation: string;     // 交接工位
    shiftName: string;       // 班次 (如：早班交中班)
    handoverUser: string;    // 交班人
    takeoverUser: string;    // 接班人
    handoverTime?: string;   // 交接时间
    status: 'PENDING' | 'COMPLETED' | 'ABNORMAL'; // 状态: 待接班/已完成/异常
    remark?: string;         // 整体备注
    items?: HandoverItem[];  // 交接明细项目
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    workstation?: string;
    handoverUser?: string;
  }
}

// ==================================================================================
// 2. Mock 数据与接口模拟
// ==================================================================================
let nextId = 3000;
let nextItemId = 8000;
let mockData: MesHandoverApi.Handover[] = Array.from({ length: 12 }).map((_, index) => ({
  id: nextId++,
  handoverNo: `HO-20260224-${String(index + 1).padStart(4, '0')}`,
  workstation: index % 2 === 0 ? '抛光打磨主控台-01' : '分切站-06',
  shiftName: index % 2 === 0 ? '早班 -> 中班' : '中班 -> 夜班',
  handoverUser: ['张伟', '李娜', '王强'][index % 3],
  takeoverUser: ['刘洋', '陈杰', '赵敏'][index % 3],
  handoverTime: '2026-02-24 08:00:00',
  status: index === 0 ? 'PENDING' : (index === 3 ? 'ABNORMAL' : 'COMPLETED'),
  remark: index === 3 ? '机台有异响，已报修' : '交接正常',
  items: [
    { id: nextItemId++, checkItem: '设备运行状态', checkResult: index === 3 ? 'NG' : 'OK', remark: index === 3 ? '主轴异响' : '' },
    { id: nextItemId++, checkItem: '现场 5S 情况', checkResult: 'OK', remark: '已清扫' },
    { id: nextItemId++, checkItem: '在制品结存核对', checkResult: 'OK', remark: '账物相符' },
  ]
}));

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getHandoverPage(params: MesHandoverApi.PageReq) {
  await sleep(300);
  const { pageNo, pageSize, workstation, handoverUser } = params;
  let list = [...mockData];
  if (workstation) list = list.filter(i => i.workstation.includes(workstation));
  if (handoverUser) list = list.filter(i => i.handoverUser.includes(handoverUser));

  const total = list.length;
  const pageList = list.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return { list: pageList, total };
}

export async function getHandover(id: number) {
  await sleep(200);
  const data = mockData.find(item => item.id === id);
  if (!data) throw new Error('交接班记录不存在');
  return JSON.parse(JSON.stringify(data)) as MesHandoverApi.Handover;
}

export async function deleteHandoverList(ids: number[]) {
  await sleep(300);
  mockData = mockData.filter(i => !ids.includes(i.id!));
  return true;
}

export async function createHandover(data: MesHandoverApi.Handover) {
  await sleep(400);
  const newRow = {
    ...data,
    id: nextId++,
    handoverNo: `HO-NEW-${new Date().getTime().toString().slice(-4)}`,
    handoverTime: new Date().toLocaleString(),
  };
  if (newRow.items) newRow.items.forEach(item => { item.id = nextItemId++; });
  mockData.unshift(newRow);
  return newRow.id;
}

export async function updateHandover(data: MesHandoverApi.Handover) {
  await sleep(400);
  const index = mockData.findIndex(item => item.id === data.id);
  if (index > -1) {
    if (data.items) data.items.forEach(item => { if (!item.id) item.id = nextItemId++; });
    mockData[index] = JSON.parse(JSON.stringify(data));
  }
}
