// 文件路径：src/api/mes/cost/report/analysis/index.ts

export namespace MesCostReportApi {
  export interface CostNode {
    id: string;
    itemName: string;
    method?: string;
    qty?: string;
    materialCost?: number;
    overheadCost?: number;
    totalCost?: number;
    unitCost?: number;
    remark?: string;
    children?: CostNode[];
  }
}

const mockTreeData: MesCostReportApi.CostNode[] = [
  {
    id: 'C-1111',
    itemName: 'CNC柔性加工中心',
    method: '机器工时比例',
    qty: '500 件',
    materialCost: 25000.00,
    overheadCost: 97500.50,
    totalCost: 122500.50,
    unitCost: 245.00,
    remark: '占基地总计 42%',
    children: [
      {
        id: 'WO-001',
        itemName: 'WO-2602-001 (A产品)',
        qty: '300 件',
        materialCost: 15000.00,
        overheadCost: 58500.30,
        totalCost: 73500.30,
        unitCost: 245.00,
        remark: '耗用工时 60%',
        children: [
          { id: 'WO-001-1', itemName: '直接材料投入', method: '实际领退料', materialCost: 15000.00, totalCost: 15000.00, unitCost: 50.00 },
          { id: 'WO-001-2', itemName: '机器折旧费分摊', method: '机器工时比例', overheadCost: 51000.00, totalCost: 51000.00, unitCost: 170.00, remark: '总折旧*60%' },
          { id: 'WO-001-3', itemName: '水电动力费分摊', method: '机器工时比例', overheadCost: 7500.30, totalCost: 7500.30, unitCost: 25.00, remark: '总电费*60%' },
        ]
      },
      {
        id: 'WO-003',
        itemName: 'WO-2602-003 (B产品)',
        qty: '200 件',
        materialCost: 10000.00,
        overheadCost: 39000.20,
        totalCost: 49000.20,
        unitCost: 245.01,
        remark: '耗用工时 40%',
        children: [
          { id: 'WO-003-1', itemName: '直接材料投入', method: '实际领退料', materialCost: 10000.00, totalCost: 10000.00, unitCost: 50.00 },
          { id: 'WO-003-2', itemName: '机器折旧费分摊', method: '机器工时比例', overheadCost: 34000.00, totalCost: 34000.00, unitCost: 170.00, remark: '总折旧*40%' },
          { id: 'WO-003-3', itemName: '水电动力费分摊', method: '机器工时比例', overheadCost: 5000.20, totalCost: 5000.20, unitCost: 25.00, remark: '总电费*40%' },
        ]
      }
    ]
  },
  {
    id: 'C-1121',
    itemName: '人工组装A线',
    method: '生产工时比例',
    qty: '120 件',
    materialCost: 120000.00,
    overheadCost: 45000.00,
    totalCost: 165000.00,
    unitCost: 1375.00,
    remark: '占基地总计 58%',
    children: [
      {
        id: 'WO-005',
        itemName: 'WO-2602-005 (终端)',
        qty: '120 件',
        materialCost: 120000.00,
        overheadCost: 45000.00,
        totalCost: 165000.00,
        unitCost: 1375.00,
        remark: '耗用工时 100%',
        children: [
          { id: 'WO-005-1', itemName: '直接材料投入', method: '实际领退料', materialCost: 120000.00, totalCost: 120000.00, unitCost: 1000.00 },
          { id: 'WO-005-2', itemName: '间接人工费分摊', method: '生产工时比例', overheadCost: 45000.00, totalCost: 45000.00, unitCost: 375.00, remark: '总人工*100%' },
        ]
      }
    ]
  },
  {
    id: 'C-1211',
    itemName: '制浆连续产线',
    method: '标准成本分摊',
    qty: '900 kg',
    materialCost: 45000.00,
    overheadCost: 8800.00,
    totalCost: 53800.00,
    unitCost: 59.78,
    remark: '占基地总计 100%',
    children: [
      {
        id: 'WO-002',
        itemName: 'WO-2602-002 (浆料)',
        qty: '900 kg',
        materialCost: 45000.00,
        overheadCost: 8800.00,
        totalCost: 53800.00,
        unitCost: 59.78,
        remark: '分摊率 9.78/kg',
        children: [
          { id: 'WO-002-1', itemName: '直接材料投入', method: '实际领退料', materialCost: 45000.00, totalCost: 45000.00, unitCost: 50.00 },
          { id: 'WO-002-2', itemName: '机物料消耗分摊', method: '标准成本分摊', overheadCost: 8800.00, totalCost: 8800.00, unitCost: 9.78, remark: '辅料定额分配' },
        ]
      }
    ]
  }
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getCostAnalysisTree(params: any) {
  await delay(400);
  return mockTreeData;
}
