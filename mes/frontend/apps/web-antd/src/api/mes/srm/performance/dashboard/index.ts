/**
 * SRM 综合大屏矩阵式 API Mock
 */

const indicatorLibrary = [
  { id: '1', code: 'KPI-D-01', name: '交期达成率', category: '交付', type: 1, scoringMethod: '交付达成率≥80%, 按(分值*交付达成率)计算', dataSource: 'WMS入库/ERP逾期报表', target: 20 },
  { id: '3', code: 'KPI-Q-01', name: '到料批合格率(LAR)', category: '质量', type: 1, scoringMethod: 'LAR≥90%且<98%时按比例计算', dataSource: 'IQC检验记录', target: 20 },
  { id: '5', code: 'KPI-Q-03', name: '品质改善配合度', category: '质量', type: 2, scoringMethod: '异常处理时效、有效性(8D报告)及配合度', dataSource: 'SCAR/客诉处理单', target: 20 },
  { id: '9', code: 'KPI-C-01', name: '材料单价水平', category: '成本', type: 1, scoringMethod: '与市场均价或上年度采购均价对比计算', dataSource: 'ERP比价单', target: 20 },
];

const baseSuppliers = [
  { id: 'S001', name: '德国舍弗勒(FAG)' },
  { id: 'S002', name: '江苏某高分子材料公司' },
  { id: 'S003', name: '上海某化工新材料' },
  { id: 'S004', name: '安徽本土某特种树脂' },
  { id: 'S005', name: '深圳某电子科技有限公司' },
  { id: 'S006', name: '浙江某特种包装厂' },
  { id: 'S007', name: '广州某自动化加工中心' },
];

const allMonths = ['25年11月', '25年12月', '26年01月', '26年02月', '26年03月'];

// 1. 获取基础字典与宏观数据
export const getDashboardBase = async () => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        indicatorList: indicatorLibrary,
        supplierList: baseSuppliers,
        monthList: allMonths,
        workload: {
          sampleEvalCount: 156, sourcingCount: 42, requirementCount: 328,
          supplierInfoCount: 1024, newSupplierCount: 15, outSupplierCount: 3
        }
      });
    }, 200);
  });
};

// 2. 核心：获取多供应商、多月份的指标对比矩阵数据
export const getMatrixData = async (params: { indicatorCode: string, supplierIds: string[], months: string[] }) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const { indicatorCode, supplierIds, months } = params;
      const indicator = indicatorLibrary.find(i => i.code === indicatorCode)!;

      const seriesData: any[] = [];
      const tableData: any[] = [];

      supplierIds.forEach(sId => {
        const supp = baseSuppliers.find(s => s.id === sId)!;
        const isBad = sId === 'S006' || sId === 'S007'; // 模拟差等生

        // 组装折线图数据
        const lineScores: number[] = [];
        // 组装表格行数据
        const rowData: any = { id: supp.id, name: supp.name };
        let sum = 0;

        months.forEach((m, index) => {
          // 模拟生成分数 (差等生分数低且递减，优等生分数高)
          let baseScore = isBad ? (15 - index * 1.5) : (18 + Math.random() * 2);
          baseScore = Number(Math.min(indicator.target, Math.max(0, baseScore)).toFixed(1));

          lineScores.push(baseScore);
          rowData[m] = baseScore; // 动态列字段，例如 rowData['26年03月'] = 19.5
          sum += baseScore;
        });

        rowData.avgScore = Number((sum / months.length).toFixed(1)); // 计算周期平均分
        tableData.push(rowData);

        seriesData.push({
          name: supp.name,
          type: 'line',
          smooth: true,
          symbolSize: 8,
          data: lineScores
        });
      });

      // 按平均分给表格排个序
      tableData.sort((a, b) => b.avgScore - a.avgScore);

      resolve({ indicatorInfo: indicator, seriesData, tableData });
    }, 400);
  });
};

// 3. 钻取：获取某个供应商在特定月份特定指标的底层凭证
export const getDrillDownTrace = async (params: { indicatorCode: string, supplierId: string, month: string }) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const supp = baseSuppliers.find(s => s.id === params.supplierId);
      const ind = indicatorLibrary.find(i => i.code === params.indicatorCode);
      const isBad = params.supplierId === 'S006' || params.supplierId === 'S007';

      resolve({
        supplierName: supp?.name,
        month: params.month,
        metric: ind?.name,
        target: ind?.target,
        score: isBad ? 8.5 : 19.5, // 模拟抓取的分数
        actualRate: isBad ? '42.5%' : '98.5%',
        evalDept: ind?.category === '质量' ? '品质部' : '采购部',
        type: ind?.category === '质量' ? 'quality' : 'delivery',
        evidenceDesc: isBad
          ? `在${params.month}期间发生严重异常，系统根据【${ind?.dataSource}】重度扣分。`
          : `在${params.month}期间表现优异，无异常台账。`,
        files: isBad
          ? [{ name: `${params.month}异常纠正8D报告.pdf`, size: '2.1MB' }, { name: '扣分单据联络函.doc', size: '340KB' }]
          : [{ name: `${params.month}明细入库/质检台账.xlsx`, size: '128KB' }]
      });
    }, 300);
  });
};
