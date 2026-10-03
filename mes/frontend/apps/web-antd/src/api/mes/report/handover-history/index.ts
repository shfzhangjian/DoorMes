import { requestClient } from '#/api/request';

export namespace MesHandoverHistoryApi {
  export interface Item {
    id?: number;
    checkItem: string;       // 检查项目
    checkResult: 'OK' | 'NG';// 检查结果
    remark?: string;         // 异常说明
  }

  export interface Record {
    id?: number;
    handoverNo: string;      // 交接单号
    workstation: string;     // 交接工位
    shiftName: string;       // 交接班次
    handoverUser: string;    // 交班人
    takeoverUser: string;    // 接班人
    wipQty: number;          // 🌟新增：线边在制品/成品结存数量
    status: 'COMPLETED' | 'ABNORMAL'; // 整体状态
    remark?: string;         // 异常与遗留事项交代
    createTime?: string;     // 交接完成时间
    items?: Item[];          // 现场点检明细(5S/设备等)
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    workstation?: string;
    status?: string;
  }
}

// 模拟历史数据，包含完整的结存数量和点检明细
const mockHistoryData: MesHandoverHistoryApi.Record[] = Array.from({ length: 25 }).map((_, index) => ({
  id: 6000 + index,
  handoverNo: `HO-202602${String(20 - (index % 10)).padStart(2, '0')}-${String(index + 1).padStart(4, '0')}`,
  workstation: index % 3 === 0 ? '抛光打磨主控台-01' : '分切站-06',
  shiftName: index % 2 === 0 ? '早班 -> 中班' : '中班 -> 夜班',
  handoverUser: ['张伟 (8801)', '李娜 (8802)', '王强 (8803)'][index % 3],
  takeoverUser: ['刘洋 (9901)', '陈杰 (9902)', '赵敏 (9903)'][index % 3],
  wipQty: 1500 + (index * 125), // 模拟实物盘点结存
  status: index % 5 === 0 ? 'ABNORMAL' : 'COMPLETED',
  remark: index % 5 === 0 ? '[设备有异响/轻微故障] 轴承声音偏大，已通知机修' : '[一切正常，无遗留] 现场已打扫',
  createTime: `2026-02-${String(20 - (index % 10)).padStart(2, '0')} 16:05:00`,
  items: [
    { id: 9001 + index, checkItem: '机台表面是否已清理干净，无粉尘积水？', checkResult: 'OK', remark: '' },
    { id: 9002 + index, checkItem: '量具、工具是否已归位并清点无误？', checkResult: 'OK', remark: '' },
    { id: 9003 + index, checkItem: '设备是否处于正常待机/运行状态？', checkResult: index % 5 === 0 ? 'NG' : 'OK', remark: index % 5 === 0 ? '主轴异响' : '' },
  ]
}));

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getHistoryPage(params: MesHandoverHistoryApi.PageReq) {
  await sleep(300);
  const { pageNo, pageSize, workstation, status } = params;
  let list = [...mockHistoryData];
  if (workstation) list = list.filter(i => i.workstation.includes(workstation));
  if (status) list = list.filter(i => i.status === status);

  const total = list.length;
  const pageList = list.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return { list: pageList, total };
}

export async function getHistoryDetail(id: number) {
  await sleep(200);
  const data = mockHistoryData.find(item => item.id === id);
  if (!data) throw new Error('记录不存在');
  return JSON.parse(JSON.stringify(data)) as MesHandoverHistoryApi.Record;
}
