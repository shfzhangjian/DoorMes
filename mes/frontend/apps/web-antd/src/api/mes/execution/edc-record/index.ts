import { requestClient } from '#/api/request';

export namespace MesEdcRecordApi {
  export interface Record {
    id?: number;
    workOrderNo: string;   // 工单编号
    productName: string;   // 产品名称
    paramName: string;     // 采集参数
    standardRange: string; // 标准范围 (如: 2800~3200)
    actualValue: number;   // 实际采集值
    unit: string;          // 单位
    result: 'PASS' | 'FAIL'; // 判定结果
    operator: string;      // 操作人
    collectTime: string;   // 采集时间
    equipmentCode: string; // 采集设备
  }

  export interface PageReq {
    pageNo: number;
    pageSize: number;
    workOrderNo?: string;
    result?: string;
  }
}

// 模拟采集记录 15 条
let mockRecords: MesEdcRecordApi.Record[] = Array.from({ length: 15 }).map((_, index) => ({
  id: 900 + index,
  workOrderNo: `WO2026022300${index + 1}`,
  productName: index < 8 ? '电子显示高透抛光垫 (Pro)' : '纳米级氧化铈抛光液',
  paramName: index % 2 === 0 ? '主轴转速' : '加工压力',
  standardRange: index % 2 === 0 ? '2800 ~ 3200' : '15.0 ~ 18.0',
  actualValue: index === 3 ? 3500 : (index % 2 === 0 ? 3000 : 16.5), // 第4条设为异常
  unit: index % 2 === 0 ? 'rpm' : 'kPa',
  result: index === 3 ? 'FAIL' : 'PASS',
  operator: '张三',
  collectTime: '2026-02-23 14:30:00',
  equipmentCode: 'CNC-001',
}));

export async function getEdcPage(params: MesEdcRecordApi.PageReq) {
  const { pageNo, pageSize, workOrderNo, result } = params;
  let list = [...mockRecords];
  if (workOrderNo) list = list.filter(i => i.workOrderNo.includes(workOrderNo));
  if (result) list = list.filter(i => i.result === result);

  return {
    list: list.slice((pageNo - 1) * pageSize, pageNo * pageSize),
    total: list.length,
  };
}

export async function getEdcDetail(id: number) {
  return mockRecords.find(i => i.id === id);
}
