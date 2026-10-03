export interface EvalRecordItemVO {
  id?: string;
  indicatorCode: string;
  indicatorName: string;
  category: string;
  type: number; // 1: 定量, 2: 定性
  scoringDept: string;
  maxScore: number;
  actualScore?: number;
  evidenceRemark?: string;
  autoCalcFormula?: string; // 仅前端展示系统算分逻辑用
}

export interface EvalRecordVO {
  id: string;
  recordNo: string;
  planTitle: string;
  supplierName: string;
  materialType: string;
  status: number;
  totalScore?: number;
  evalGrade?: string;
  items?: EvalRecordItemVO[];
}

const mockData: EvalRecordVO[] = [
  {
    id: '1', recordNo: 'EV-2026Q1-001', planTitle: '2026年第一季度A/B类供应商常规考核',
    supplierName: '江苏某高分子材料公司', materialType: 'A类', status: 0,
    items: [
      { indicatorCode: 'KPI-Q-01', indicatorName: '到料批合格率(LAR)', category: '质量', type: 1, scoringDept: '品质部', maxScore: 15, actualScore: 14.5, autoCalcFormula: 'WMS质检抓取: 本季交货50批, 拒收1批, 合格率98%。', evidenceRemark: '系统自动抓取并计算' },
      { indicatorCode: 'KPI-Q-02', indicatorName: '上线合格率及异常', category: '质量', type: 1, scoringDept: '生产部', maxScore: 15, actualScore: 12, autoCalcFormula: 'MES抓取: 发生2次轻微停线，每次扣1.5分。', evidenceRemark: '系统自动抓取并计算' },
      { indicatorCode: 'KPI-S-01', indicatorName: '合同履行情况', category: '服务', type: 2, scoringDept: '采购部', maxScore: 5, actualScore: undefined },
      { indicatorCode: 'KPI-S-02', indicatorName: '新品开发配合度', category: '服务', type: 2, scoringDept: '技术部', maxScore: 5, actualScore: undefined }
    ]
  },
  {
    id: '2', recordNo: 'EV-2026Q1-002', planTitle: '2026年第一季度A/B类供应商常规考核',
    supplierName: '德国舍弗勒(FAG)', materialType: 'A类', status: 2, totalScore: 92.5, evalGrade: 'A',
    items: []
  }
];

export const getEvalPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = mockData;
      if (params.tabType === 'todo') filtered = filtered.filter(i => i.status !== 2);
      if (params.tabType === 'done') filtered = filtered.filter(i => i.status === 2);
      resolve({ list: filtered, total: filtered.length });
    }, 300);
  });
};

export const getEvalDetail = async (id: string) => {
  return new Promise((resolve) => {
    setTimeout(() => { resolve(mockData.find(p => p.id === id) || {}); }, 200);
  });
};
