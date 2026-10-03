/**
 * 供方退出审批单 API 接口模拟 (Mock)
 */

export interface SupplierExitVO {
  id: string;
  exitNo: string;
  supplierName: string;
  exitType: string; // '质量淘汰', '绩效不达标', '业务转型', '主动退出', '长期无交易'
  riskLevel: string; // '高', '中', '低'
  applyDept: string;
  applicant: string;
  applyDate: string;
  exitReason: string;
  mitigationPlan: string; // 风险应对与替代方案
  // 历史原型分组字段
  node: string;
  status: 'DRAFT' | 'APPROVING' | 'PASSED' | 'REJECTED';
  tabType: 'todo' | 'initiated' | 'processed' | 'monitor';
}

const mockData: SupplierExitVO[] = [
  // --- 处理分组 ---
  { id: '1', exitNo: 'SRM-EX-260301-01', supplierName: '常州某电子辅料包装厂', exitType: '绩效不达标', riskLevel: '中', applyDept: '品质部', applicant: '刘品质', applyDate: '2026-03-01', exitReason: '连续两个季度绩效考核为D级，质量退货频发。', mitigationPlan: '已引入两家新包材供应商，库存足够支撑1个月切换。', node: '采购总监审核', status: 'APPROVING', tabType: 'todo' },
  { id: '2', exitNo: 'SRM-EX-260228-05', supplierName: '上海某特种化学品代理', exitType: '业务转型', riskLevel: '高', applyDept: '采购部', applicant: '张采购', applyDate: '2026-02-28', exitReason: '该供应商被原厂取消代理权，无法继续供货。', mitigationPlan: '正在紧急接洽原厂及其他一级代理，已要求研发启动B方案。', node: '财务清算确认', status: 'APPROVING', tabType: 'todo' },
  { id: '3', exitNo: 'SRM-EX-260225-12', supplierName: '无锡某五金加工厂', exitType: '主动退出', riskLevel: '低', applyDept: '采购部', applicant: '张采购', applyDate: '2026-02-25', exitReason: '对方产能调整，主动提出终止合作。', mitigationPlan: '非核心定制件，市场可替代性强。', node: '技术部影响评估', status: 'APPROVING', tabType: 'todo' },

  // --- 发起分组 ---
  { id: '4', exitNo: 'SRM-EX-260302-02', supplierName: '苏州某塑胶注塑厂', exitType: '长期无交易', riskLevel: '低', applyDept: '采购部', applicant: '我(当前用户)', applyDate: '2026-03-02', exitReason: '系统显示已超过24个月无采买记录，清理冗余档案。', mitigationPlan: '无业务影响。', node: '草稿状态', status: 'DRAFT', tabType: 'initiated' },
  { id: '5', exitNo: 'SRM-EX-260210-08', supplierName: '杭州某设备机加工厂', exitType: '质量淘汰', riskLevel: '中', applyDept: '采购部', applicant: '我(当前用户)', applyDate: '2026-02-10', exitReason: '核心部件加工精度严重不达标，导致客户投诉。', mitigationPlan: '已索赔10万元，转由现有A级供应商接手。', node: '流程结束(归档)', status: 'PASSED', tabType: 'initiated' },

  // --- 办结分组 ---
  { id: '6', exitNo: 'SRM-EX-260220-03', supplierName: '江苏某高分子树脂厂', exitType: '环保合规不达标', riskLevel: '高', applyDept: 'EHS安环部', applicant: '李安环', applyDate: '2026-02-20', exitReason: '现场考察发现偷排漏排，被环保局勒令停产。', mitigationPlan: '已冻结所有未付款项，紧急切换替代料。', node: '总经理批准', status: 'APPROVING', tabType: 'processed' },
  { id: '7', exitNo: 'SRM-EX-260215-09', supplierName: '深圳某电子元器件经销', exitType: '绩效不达标', riskLevel: '低', applyDept: '品质部', applicant: '刘品质', applyDate: '2026-02-15', exitReason: '交期准交率低于60%。', mitigationPlan: '无。', node: '采购部退回修改', status: 'REJECTED', tabType: 'processed' },

  // --- 👁️ 全局监控附加数据 (monitor) ---
  { id: '8', exitNo: 'SRM-EX-260105-01', supplierName: '合肥某防护用品行', exitType: '长期无交易', riskLevel: '低', applyDept: '采购部', applicant: '王采购', applyDate: '2026-01-05', exitReason: '公司更换劳保标准，该供应商产品不符。', mitigationPlan: '平稳过渡无影响。', node: '流程结束(归档)', status: 'PASSED', tabType: 'monitor' },
];

export const getSupplierExitPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = mockData;
      if (params.tabType && params.tabType !== 'monitor') {
        filtered = filtered.filter(item => item.tabType === params.tabType);
      }
      resolve({ list: filtered, total: filtered.length });
    }, 400);
  });
};
