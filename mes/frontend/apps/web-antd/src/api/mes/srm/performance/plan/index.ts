export interface EvalPlanVO {
  id: string;
  planNo: string;
  title: string;
  templateId: string;
  templateName: string;
  periodType: string;
  evalYear: number;
  evalQuarter?: number;
  deadline: string;
  status: number;
  remark: string;
  suppliers?: any[]; // 参评供应商列表
}

const mockData: EvalPlanVO[] = [
  {
    id: '1',
    planNo: 'EP-2026Q1-001',
    title: '2026年第一季度A/B类供应商常规考核',
    templateId: '1',
    templateName: 'A/B类物料季度综合考核模板',
    periodType: 'QUARTER',
    evalYear: 2026,
    evalQuarter: 1,
    deadline: '2026-04-10',
    status: 1, // 已发布
    remark: '请品质、采购、技术、仓管部门在4月10日前完成打分。',
    suppliers: [
      { id: '101', supplierId: 'S001', supplierName: '江苏某高分子材料公司', materialType: 'A类', evalStatus: 1, totalScore: null, evalGrade: '' },
      { id: '102', supplierId: 'S002', supplierName: '德国舍弗勒(FAG)', materialType: 'A类', evalStatus: 2, totalScore: 92.5, evalGrade: 'A' },
      { id: '103', supplierId: 'S003', supplierName: '苏州某新材料科技', materialType: 'B类', evalStatus: 0, totalScore: null, evalGrade: '' }
    ]
  },
  {
    id: '2',
    planNo: 'EP-2025Y-001',
    title: '2025年度C类辅材供应商综合评定',
    templateId: '2',
    templateName: 'C类辅材年度考核模板',
    periodType: 'YEAR',
    evalYear: 2025,
    deadline: '2026-01-31',
    status: 2, // 已完结
    remark: '年度考核，包含环保协议核查扣分项。',
    suppliers: []
  }
];

export const getPlanPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ list: mockData, total: mockData.length });
    }, 300);
  });
};

export const getPlanDetail = async (id: string) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockData.find(p => p.id === id) || {});
    }, 200);
  });
};
