/**
 * 试产验证跟踪单 API 接口模拟 (Mock)
 */

export interface TrialTrackVO {
  id: string;
  trialNo: string;
  supplierName: string;
  materialName: string;
  trialQty: string;
  lineYield: number; // 试产良率
  applyDept: string;
  applicant: string;
  trialDate: string;
  // 历史原型分组字段
  node: string;
  status: 'DRAFT' | 'APPROVING' | 'PASSED' | 'REJECTED';
  tabType: 'todo' | 'initiated' | 'processed' | 'monitor';
}

const mockData: TrialTrackVO[] = [
  // --- 处理分组 ---
  { id: '1', trialNo: 'SRM-TR-260305-01', supplierName: '江苏某高分子材料公司', materialName: '光学级PET树脂', trialQty: '500 KG', lineYield: 98.5, applyDept: '研发部', applicant: '张工', trialDate: '2026-03-05', node: '生产部试产总结', status: 'APPROVING', tabType: 'todo' },
  { id: '2', trialNo: 'SRM-TR-260302-05', supplierName: '无锡某包装制品厂', materialName: '3寸加强纸管', trialQty: '1000 根', lineYield: 99.2, applyDept: '采购部', applicant: '李采购', trialDate: '2026-03-02', node: '品质部良率判定', status: 'APPROVING', tabType: 'todo' },
  { id: '3', trialNo: 'SRM-TR-260228-08', supplierName: '浙江某特种塑料', materialName: '阻燃PC/ABS', trialQty: '200 KG', lineYield: 92.0, applyDept: '工艺部', applicant: '王工艺', trialDate: '2026-02-28', node: '研发部最终核准', status: 'APPROVING', tabType: 'todo' },

  // --- 发起分组 ---
  { id: '4', trialNo: 'SRM-TR-260310-02', supplierName: '苏州某机加工厂', materialName: '精密主轴', trialQty: '50 件', lineYield: 0, applyDept: '技术部', applicant: '我(当前用户)', trialDate: '2026-03-10', node: '草稿状态', status: 'DRAFT', tabType: 'initiated' },
  { id: '5', trialNo: 'SRM-TR-260215-11', supplierName: '上海某电子辅料行', materialName: '导电银浆', trialQty: '10 瓶', lineYield: 99.8, applyDept: '研发部', applicant: '我(当前用户)', trialDate: '2026-02-15', node: '试产放行(归档)', status: 'PASSED', tabType: 'initiated' },

  // --- 办结分组 ---
  { id: '6', trialNo: 'SRM-TR-260220-03', supplierName: '合肥某防护用品行', materialName: '无尘布', trialQty: '1000 包', lineYield: 99.9, applyDept: '采购部', applicant: '张采购', trialDate: '2026-02-20', node: '试产放行(归档)', status: 'PASSED', tabType: 'processed' },
  { id: '7', trialNo: 'SRM-TR-260218-09', supplierName: '深圳某五金工具厂', materialName: '特种研磨头', trialQty: '500 个', lineYield: 75.5, applyDept: '工艺部', applicant: '刘工艺', trialDate: '2026-02-18', node: '试产失败退回', status: 'REJECTED', tabType: 'processed' },

  // --- 👁️ 全局监控附加数据 (monitor) ---
  { id: '8', trialNo: 'SRM-TR-260110-01', supplierName: '常州某化工树脂', materialName: 'UV固化胶', trialQty: '50 KG', lineYield: 85.0, applyDept: '技术部', applicant: '赵技术', trialDate: '2026-01-10', node: '工艺部整改复试', status: 'APPROVING', tabType: 'monitor' },
];

export const getTrialTrackPage = async (params: any) => {
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
