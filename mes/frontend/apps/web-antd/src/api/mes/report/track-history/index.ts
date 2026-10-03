import { requestClient } from '#/api/request';

export namespace MesTrackHistoryApi {
  export interface BarcodeItem { id?: number; serialNumber: string; status: 'GOOD' | 'SCRAP'; generateTime: string; }

  // 🌟 新增：全流程追溯数据结构
  export interface ChecklistItem { id?: number; itemName: string; standard: string; actualValue: string; result: 'OK' | 'NG'; }
  export interface BomItem { id?: number; materialCode: string; materialName: string; requiredQty: number; actualQty: number; }
  export interface EdcItem { id?: number; phase: string; paramName: string; standard: string; actualValue: number; result: 'OK' | 'NG'; time: string; }
  export interface InspectItem { id?: number; type: string; reportNo: string; result: 'PASS' | 'FAIL'; time: string; }

  export interface Record {
    id?: number;
    trackNo: string;
    workOrderNo: string;
    productName: string;
    workstation: string;
    operator: string;
    trackMode: 'PIECE' | 'BATCH';
    qty: number;
    trackTime: string;
    items?: BarcodeItem[]; // 过站条码明细

    // 🌟 新增：绑定的电子履历数据
    checklist?: ChecklistItem[];
    bomItems?: BomItem[];
    edcHistory?: EdcItem[];
    inspections?: InspectItem[];
  }
}

// 模拟历史追溯全景数据
const mockTrackData: MesTrackHistoryApi.Record[] = Array.from({ length: 20 }).map((_, index) => {
  const isPiece = index % 3 !== 0;
  const qty = isPiece ? (Math.floor(Math.random() * 5) + 1) : 50;
  const items = isPiece ? Array.from({ length: qty }).map((_, i) => ({
    id: 10000 + i, serialNumber: `SN2026${String(Math.floor(Math.random()*10000)).padStart(4,'0')}${i}`, status: 'GOOD' as const, generateTime: '2026-02-24 10:30:00'
  })) : [];

  return {
    id: 7000 + index,
    trackNo: `TRK-260224-${String(index + 1).padStart(4, '0')}`,
    workOrderNo: `WO2026022300${(index % 3) + 1}`,
    productName: '电子显示高透抛光垫 (Pro)',
    workstation: index % 2 === 0 ? '抛光打磨主控台-01' : '分切站-06',
    operator: '张三 (8801)',
    trackMode: isPiece ? 'PIECE' : 'BATCH',
    qty: qty,
    trackTime: `2026-02-24 10:${String(30 + index).padStart(2, '0')}:00`,
    items: items,

    // 🌟 模拟 HMI 传来的过程数据
    checklist: [
      { id: 1, itemName: '作业区域 5S 清扫', standard: '目视无杂物', actualValue: '已清扫', result: 'OK' },
      { id: 2, itemName: '主轴气压', standard: '0.5~0.7 MPa', actualValue: '0.62 MPa', result: 'OK' },
      { id: 3, itemName: '槽液浓度', standard: '5~8 %', actualValue: '6.5 %', result: 'OK' }
    ],
    bomItems: [
      { id: 1, materialCode: 'RM-PU-001', materialName: '聚氨酯基材', requiredQty: 100, actualQty: 100 },
      { id: 2, materialCode: 'RM-AB-002', materialName: '高标号研磨微粉', requiredQty: 50, actualQty: 51 }
    ],
    edcHistory: [
      { id: 1, phase: '开机前值', paramName: '主轴转速', standard: '2800~3200 rpm', actualValue: 2950, result: 'OK', time: '10:05:00' },
      { id: 2, phase: '监控中', paramName: '加工槽液 PH值', standard: '6.8~7.2 pH', actualValue: 7.0, result: 'OK', time: '10:15:00' }
    ],
    inspections: [
      { id: 1, type: '首件检验 (FAI)', reportNo: `FAI-260224-${index}`, result: 'PASS', time: '10:10:00' },
      { id: 2, type: '过程抽检 (IPQC)', reportNo: `IPQC-260224-${index}`, result: 'PASS', time: '10:20:00' },
      { id: 3, type: '成品检验 (FQC)', reportNo: `FQC-260224-${index}`, result: 'PASS', time: '10:28:00' }
    ]
  };
});

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getTrackPage(params: any) {
  await sleep(300);
  const { pageNo, pageSize, workOrderNo, trackMode } = params;
  let list = [...mockTrackData];
  if (workOrderNo) list = list.filter(i => i.workOrderNo.includes(workOrderNo));
  if (trackMode) list = list.filter(i => i.trackMode === trackMode);
  return { list: list.slice((pageNo - 1) * pageSize, pageNo * pageSize), total: list.length };
}

export async function getTrackDetail(id: number) {
  await sleep(200);
  return JSON.parse(JSON.stringify(mockTrackData.find(item => item.id === id))) as MesTrackHistoryApi.Record;
}
