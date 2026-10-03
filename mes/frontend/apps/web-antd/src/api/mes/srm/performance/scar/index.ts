/**
 * SRM 供方异常与整改 (SCAR / 8D) API Mock
 */

const mockData = [
  {
    id: 'SCAR-2603-001',
    supplierName: '浙江某特种包装厂',
    source: '季度绩效考核',
    issueDesc: '2026年Q1综合评定为C级。其中3月份发生严重客诉，纸箱在运输途中开裂导致内部膜材受损。',
    level: 'Major (严重)',
    status: 'WAIT_SUPPLIER', // 待供方回复
    publishDate: '2026-03-25',
    dueDate: '2026-03-28', // 要求3天内回复8D
    isOverdue: true,
    eightD: {
      d1_team: '', d2_desc: '', d3_containment: '', d4_rootCause: '',
      d5_action: '', d6_verify: '', d7_prevent: '', d8_close: ''
    }
  },
  {
    id: 'SCAR-2603-012',
    supplierName: '广州某自动化加工中心',
    source: 'PMC交期异常',
    issueDesc: '连续两批次关键机加件逾期超过7天，导致我司装配产线停线待料4小时。',
    level: 'Critical (致命)',
    status: 'QA_REVIEW', // 供方已回，待SQE审核
    publishDate: '2026-03-26',
    dueDate: '2026-03-29',
    isOverdue: false,
    eightD: {
      d1_team: '张三(厂长), 李四(生产), 王五(品质)',
      d2_desc: '因外协表面处理厂环保限产，导致工序积压逾期。',
      d3_containment: '已派专车去外协厂现场蹲点催货，首批 500件 已发顺丰特快专递。',
      d4_rootCause: '1. 产能评估未考虑环保政策突发风险；2. 缺乏备用外协资源。',
      d5_action: '引入深圳某合规电镀厂作为二供，重新排产。',
      d6_verify: '', d7_prevent: '', d8_close: ''
    }
  },
  {
    id: 'SCAR-2602-088',
    supplierName: '江苏某高分子材料公司',
    source: 'IQC进料检验',
    issueDesc: '入库单 IN-2602-15 抽检发现尺寸超差 0.05mm，触碰管控红线，整批 5000KG 拒收。',
    level: 'Major (严重)',
    status: 'VERIFYING', // 对策已确认，待下一批次验证有效性
    publishDate: '2026-02-18',
    dueDate: '2026-02-21',
    isOverdue: false,
    eightD: {
      d1_team: '品质总监领衔团队',
      d2_desc: '注塑模具滑块磨损导致尺寸偏大。',
      d3_containment: '冻结同批次库存，启动全检挑选，补发合格品 5000KG。',
      d4_rootCause: '模具保养SOP中未规定对滑块间隙进行塞尺测量，导致点检流于形式。',
      d5_action: '修订模具点检SOP，增加塞尺测量卡点；注塑机增加尺寸CPK在线监控。',
      d6_verify: '待验证 (预计3月批次入库时由IQC验证)', d7_prevent: '', d8_close: ''
    }
  },
  {
    id: 'SCAR-2601-045',
    supplierName: '深圳某电子科技有限公司',
    source: '产线不良抛料',
    issueDesc: 'SMT贴片机频发抛料，确认为供方载带孔距误差过大导致。',
    level: 'Minor (轻微)',
    status: 'CLOSED', // 已闭环
    publishDate: '2026-01-10',
    dueDate: '2026-01-15',
    isOverdue: false,
    eightD: {
      d1_team: '工程部、品质部',
      d2_desc: '载带冲孔模针磨损。',
      d3_containment: '更换模针，库存重检。',
      d4_rootCause: '模针寿命管理缺失。',
      d5_action: '导入模具寿命管理系统，达到 50万次 强制更换。',
      d6_verify: '连续3批次无抛料，改善有效。',
      d7_prevent: '推广至所有冲压机台。',
      d8_close: '同意闭环。'
    }
  }
];

export const getScarPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = [...mockData];
      if (params.tabType === 'todo') filtered = filtered.filter(i => ['WAIT_SUPPLIER', 'QA_REVIEW'].includes(i.status));
      if (params.tabType === 'verifying') filtered = filtered.filter(i => i.status === 'VERIFYING');
      if (params.tabType === 'closed') filtered = filtered.filter(i => i.status === 'CLOSED');
      resolve({ list: filtered, total: filtered.length });
    }, 300);
  });
};
