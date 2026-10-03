/**
 * SRM 季度综合绩效评定 API Mock
 */

const mockData = [
  {
    id: 'RPT-2026Q1-001',
    supplierName: '江苏某高分子材料公司',
    materialType: 'A类(主材)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 92.5,
    evalGrade: 'A',
    status: 'APPROVING',
    currentNode: '品质部审核',
    months: ['01月', '02月', '03月'],
    totalScores: [90.5, 93.0, 94.0],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 18.5, m2: 19.0, m3: 20.0 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 19.0, m2: 19.5, m3: 19.5 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 18.0, m2: 19.0, m3: 18.5 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 17.0, m2: 18.5, m3: 18.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 18.0, m2: 17.0, m3: 18.0 },
    ],
    auditLogs: [
      { node: '系统生成', user: 'System', time: '2026-04-01 02:00', result: '自动发案', remark: '系统汇总各部门月度得分' },
      { node: '采购经理确认', user: '张采购', time: '2026-04-05 10:00', result: '同意', remark: '得分核对无误，同意提交品质审核。' }
    ]
  },
  {
    id: 'RPT-2026Q1-002',
    supplierName: '德国舍弗勒(FAG)',
    materialType: 'A类(关键)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 97.0,
    evalGrade: 'A',
    status: 'FINISHED',
    currentNode: '已归档',
    months: ['01月', '02月', '03月'],
    totalScores: [96.5, 97.5, 97.0],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 20.0, m2: 20.0, m3: 19.5 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 19.5, m2: 20.0, m3: 20.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 19.0, m2: 19.5, m3: 19.5 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 19.0, m2: 19.0, m3: 19.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 19.0, m2: 19.0, m3: 19.0 },
    ],
    auditLogs: [
      { node: '采购经理确认', user: '张采购', time: '2026-04-02 09:00', result: '同意', remark: '免检供应商，表现优异。' },
      { node: '品质部审核', user: '李品质', time: '2026-04-03 14:20', result: '同意', remark: '质量极度稳定。' },
      { node: '采购总监核准', user: '王总监', time: '2026-04-04 11:00', result: '同意', remark: '增加下一季度配额。' }
    ]
  },
  {
    id: 'RPT-2026Q1-003',
    supplierName: '深圳某电子科技有限公司',
    materialType: 'B类(辅材)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 84.5,
    evalGrade: 'B',
    status: 'APPROVING',
    currentNode: '采购总监核准',
    months: ['01月', '02月', '03月'],
    totalScores: [82.0, 85.0, 86.5],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 16.0, m2: 17.5, m3: 18.0 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 17.0, m2: 17.5, m3: 18.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 16.5, m2: 17.0, m3: 17.5 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 16.0, m2: 16.5, m3: 16.5 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 16.5, m2: 16.5, m3: 16.5 },
    ],
    auditLogs: [
      { node: '品质部审核', user: '李品质', time: '2026-04-05 16:30', result: '同意', remark: '合格率爬坡明显，B级评定合理。' }
    ]
  },
  {
    id: 'RPT-2026Q1-004',
    supplierName: '浙江某特种包装厂',
    materialType: 'C类(耗材)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 75.5,
    evalGrade: 'C',
    status: 'REJECTED',
    currentNode: '被驳回(待修改)',
    months: ['01月', '02月', '03月'],
    totalScores: [78.0, 76.5, 72.0], // 明显下滑趋势
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 15.0, m2: 14.5, m3: 12.0 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 16.0, m2: 15.5, m3: 14.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 15.5, m2: 15.0, m3: 13.0 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 15.5, m2: 15.5, m3: 16.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 16.0, m2: 16.0, m3: 17.0 },
    ],
    auditLogs: [
      { node: '采购经理确认', user: '张采购', time: '2026-04-02 10:00', result: '同意', remark: '提交审批。' },
      { node: '品质部审核', user: '李总监', time: '2026-04-03 09:15', result: '驳回', remark: '【驳回】3月份发生重大纸箱开裂客诉，8D报告未按时闭环，LAR项应当扣除5分，请重算！' }
    ]
  },
  {
    id: 'RPT-2026Q1-005',
    supplierName: '广州某自动化加工中心',
    materialType: 'B类(机加件)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 62.0,
    evalGrade: 'D',
    status: 'FINISHED',
    currentNode: '已归档',
    months: ['01月', '02月', '03月'],
    totalScores: [65.0, 62.0, 59.0], // 极差的走势
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 12.0, m2: 10.0, m3: 8.0 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 13.0, m2: 12.0, m3: 11.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 12.0, m2: 11.0, m3: 10.0 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 14.0, m2: 14.0, m3: 15.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 14.0, m2: 15.0, m3: 15.0 },
    ],
    auditLogs: [
      { node: '采购总监核准', user: '王总监', time: '2026-04-06 14:00', result: '同意', remark: '连续两月不及格导致产线停线，按照准则直接评定为D级，即刻冻结ERP下单并启动淘汰流程！' }
    ]
  },
  {
    id: 'RPT-2026Q1-006',
    supplierName: '上海某化工新材料有限公司',
    materialType: 'A类(主材)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 88.5,
    evalGrade: 'B',
    status: 'APPROVING',
    currentNode: '品质部审核',
    months: ['01月', '02月', '03月'],
    totalScores: [88.0, 89.0, 88.5],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 18.0, m2: 18.0, m3: 17.5 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 17.5, m2: 18.5, m3: 18.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 17.5, m2: 17.5, m3: 18.0 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 18.0, m2: 18.0, m3: 18.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 17.0, m2: 17.0, m3: 17.0 },
    ],
    auditLogs: [
      { node: '采购经理确认', user: '张采购', time: '2026-04-05 11:30', result: '同意', remark: '提交品质会签。' }
    ]
  },
  {
    id: 'RPT-2026Q1-007',
    supplierName: '苏州某精密五金配件厂',
    materialType: 'B类(机加件)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 78.5,
    evalGrade: 'C',
    status: 'DRAFT',
    currentNode: '草稿',
    months: ['01月', '02月', '03月'],
    totalScores: [80.0, 78.0, 77.5],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 16.0, m2: 15.0, m3: 15.5 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 16.0, m2: 15.5, m3: 15.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 15.5, m2: 15.0, m3: 14.5 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 16.5, m2: 16.5, m3: 16.5 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 16.0, m2: 16.0, m3: 16.0 },
    ],
    auditLogs: []
  },
  {
    id: 'RPT-2026Q1-008',
    supplierName: '安徽本土某特种树脂公司',
    materialType: 'A类(关键)',
    evalYear: 2026,
    evalQuarter: 1,
    totalScore: 91.0,
    evalGrade: 'A',
    status: 'FINISHED',
    currentNode: '已归档',
    months: ['01月', '02月', '03月'],
    totalScores: [89.5, 91.5, 92.0],
    indicatorTrends: [
      { metric: '交期达成率(OTD)', target: '20分', maxScore: 20, m1: 18.0, m2: 19.0, m3: 19.0 },
      { metric: '到料批合格率(LAR)', target: '20分', maxScore: 20, m1: 18.5, m2: 18.5, m3: 19.0 },
      { metric: '上线合格率及异常', target: '20分', maxScore: 20, m1: 18.0, m2: 18.5, m3: 18.5 },
      { metric: '品质改善配合度', target: '20分', maxScore: 20, m1: 17.5, m2: 18.0, m3: 18.0 },
      { metric: '材料成本竞争力', target: '20分', maxScore: 20, m1: 17.5, m2: 17.5, m3: 17.5 },
    ],
    auditLogs: [
      { node: '采购总监核准', user: '王总监', time: '2026-04-06 09:30', result: '同意', remark: '本地战略供应商，服务响应快，维持A级。' }
    ]
  }
];

export const getReportPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockData];

      // 历史原型四类分组过滤逻辑
      if (params.tabType === 'todo') {
        // 处理分组：状态为审批中
        filtered = filtered.filter(item => item.status === 'APPROVING');
      } else if (params.tabType === 'done') {
        // 办结分组：状态为已完成
        filtered = filtered.filter(item => item.status === 'FINISHED');
      } else if (params.tabType === 'apply') {
        // 发起分组：草稿、被驳回、审批中均纳入
        filtered = filtered.filter(item => ['DRAFT', 'REJECTED', 'APPROVING'].includes(item.status));
      } else if (params.tabType === 'all') {
        // 全部单据：不进行 status 过滤
      }

      // 常规搜索过滤
      if (params.supplierName) {
        filtered = filtered.filter(item => item.supplierName.includes(params.supplierName));
      }
      if (params.evalYear) {
        filtered = filtered.filter(item => item.evalYear === params.evalYear);
      }
      if (params.evalQuarter) {
        filtered = filtered.filter(item => item.evalQuarter === params.evalQuarter);
      }

      resolve({
        list: filtered,
        total: filtered.length
      });
    }, 400); // 增加一点网络延迟，让页面的 Spin 加载动画更明显
  });
};
