/**
 * SRM 评估指标库 API 接口模拟 (Mock)
 * 数据严格提取自《03供应商季度品质考核评分表》与《07供应商质量考察评分表》
 */

export interface SrmIndicatorVO {
  id: string;
  code: string;
  name: string;
  category: string;
  type: number; // 1:定量(客观) 2:定性(主观) 3:红线(一票否决)
  scoringMethod: string;
  dataSource: string;
  status: number;
}

const mockData: SrmIndicatorVO[] = [
  // ================= 提取自《03供应商季度品质考核评分表》 (常规绩效) =================
  { id: '1', code: 'KPI-D-01', name: '交期达成率', category: '交付', type: 1, scoringMethod: '交付达成率≥80%, 按(分值*交付达成率)计算, <80%计0分', dataSource: 'WMS入库单据', status: 0 },
  { id: '2', code: 'KPI-D-02', name: '超额运费违规', category: '交付', type: 3, scoringMethod: '有超额运费项, 该维度计0分', dataSource: '采购财务账单', status: 0 },
  { id: '3', code: 'KPI-Q-01', name: '到料批合格率(LAR)', category: '质量', type: 1, scoringMethod: 'LAR≥90%且<98%时按(分值*LAR)计算, <90%计0分', dataSource: 'IQC检验记录', status: 0 },
  { id: '4', code: 'KPI-Q-02', name: '上线合格率及异常', category: '质量', type: 1, scoringMethod: '目标98%。按上线合格率得分，并根据产线异常扣减', dataSource: 'MES生产报工/不良', status: 0 },
  { id: '5', code: 'KPI-Q-03', name: '品质改善配合度', category: '质量', type: 2, scoringMethod: '异常处理时效、有效性(8D报告)及配合度评分', dataSource: 'SCAR/客诉处理单', status: 0 },
  { id: '6', code: 'KPI-S-01', name: '合同履行情况', category: '服务', type: 2, scoringMethod: '合同履约率及服务态度主观评分', dataSource: '采购部评价', status: 0 },
  { id: '7', code: 'KPI-S-02', name: '新品开发配合度', category: '服务', type: 2, scoringMethod: '打样配合度及研发支持响应速度', dataSource: '技术部评价', status: 0 },
  { id: '8', code: 'KPI-T-01', name: '技术支持能力', category: '技术', type: 2, scoringMethod: '技术问题解决的彻底性与时效性', dataSource: '技术部评价', status: 0 },
  { id: '9', code: 'KPI-C-01', name: '材料单价水平', category: '成本', type: 1, scoringMethod: '与市场均价或上年度采购均价对比计算得分段', dataSource: 'ERP比价单', status: 0 },

  // ================= 提取自《07供应商质量考察评分表》 (体系准入/飞行稽核) =================
  { id: '10', code: 'AUD-01', name: '质量管理体系', category: '体系', type: 2, scoringMethod: '是否取得ISO9001:2015, IATF16949等证书', dataSource: '电子档体系证书', status: 0 },
  { id: '11', code: 'AUD-02', name: '环境/健康/安全(EHS)', category: 'EHS', type: 2, scoringMethod: '是否取得ISO14001:2015, ISO45001等证书', dataSource: '电子档EHS证书', status: 0 },
  { id: '12', code: 'AUD-03', name: 'QC工程图', category: '质量', type: 2, scoringMethod: '是否明确责任及应对措施？是否定义关键过程点控制？', dataSource: 'QC Flow Chart', status: 0 },
  { id: '13', code: 'AUD-04', name: 'IQC与PQC管控', category: '质量', type: 2, scoringMethod: '解释IQC/PQC抽样流程；如何确认CoA报告准确性；中间品与成品检验流程。', dataSource: '检验SOP及报告', status: 0 },
  { id: '14', code: 'AUD-05', name: '设备与仪器校准', category: '体系', type: 2, scoringMethod: '是否有校准SOP？设备是否有校准标签？是否可追溯到国际标准？', dataSource: '校准记录与标签', status: 0 },
  { id: '15', code: 'AUD-06', name: '机器与生产设备', category: '生产', type: 2, scoringMethod: '是否有设备一览表及操作规程？是否有定期校验保养计划？', dataSource: '设备台账与保养计划', status: 0 },
  { id: '16', code: 'AUD-07', name: '分析测试能力', category: '技术', type: 2, scoringMethod: '实验室环境是否受控？测试员是否培训？仪器是否定期校验？', dataSource: '实验室及培训记录', status: 0 },
  { id: '17', code: 'AUD-08', name: '不符合项报告(NCR)', category: '质量', type: 2, scoringMethod: '解释NCR程序；如何处理超差(OOS)物料及客退物料？是否有根因分析？', dataSource: 'NCR程序及报告', status: 0 },
  { id: '18', code: 'AUD-09', name: '统计过程控制(SPC)', category: '质量', type: 2, scoringMethod: '哪些工序应用SPC？如何修订控制限？是否做CPK分析与改进？', dataSource: 'SPC及CPK控制图', status: 0 },
  { id: '19', code: 'AUD-10', name: '客户投诉管理', category: '服务', type: 2, scoringMethod: '是否有解决客诉的目标天数？投诉问题是否严格闭环？', dataSource: '客诉处理台账', status: 0 },
  { id: '20', code: 'AUD-11', name: '质量记录控制', category: '体系', type: 2, scoringMethod: '生产相关记录是否至少保存3年？系统中记录是否受控？', dataSource: '记录管控SOP', status: 0 },
  { id: '21', code: 'AUD-12', name: '批次可追溯性', category: '质量', type: 2, scoringMethod: '是否有留样规则？能否从最终成品精准追溯到原材料？', dataSource: '追溯系统或批次档案', status: 0 },
  { id: '22', code: 'AUD-13', name: '测量系统分析(MSA)', category: '质量', type: 2, scoringMethod: '是否规定MSA分析(GR&R)频率及验收标准？并进行例行评估改进？', dataSource: 'MSA/GR&R报告', status: 0 },
  { id: '23', code: 'AUD-14', name: '设计评审流程', category: '技术', type: 2, scoringMethod: '产品开发(小试-中试-量产)各阶段是否有里程碑审查机制与设计验证？', dataSource: 'APQP或开发计划', status: 0 },
  { id: '24', code: 'AUD-15', name: '质量改善项目(QIP)', category: '质量', type: 2, scoringMethod: '是否推行内外部QIP项目？有无内部合理化建议机制？', dataSource: 'QIP项目档案(近5年)', status: 0 },
  { id: '25', code: 'AUD-16', name: '自动数据库系统', category: 'IT', type: 2, scoringMethod: '是否引进了自动质量数据库？是否涵盖关键质量指标并能分析趋势？', dataSource: '系统演示/权限清单', status: 0 },
  { id: '26', code: 'AUD-17', name: '组织架构独立性', category: '体系', type: 2, scoringMethod: '各部门职责是否明确？质量部是否独立于制造运营部门？', dataSource: '组织架构图', status: 0 },
  { id: '27', code: 'AUD-18', name: '变更控制与PCN', category: '质量', type: 3, scoringMethod: '是否有变更分级(重大/轻微)？实施前是否验证？严重变更是否提前6个月发正式PCN通知？', dataSource: 'PCN协议与通知函', status: 0 },
  { id: '28', code: 'AUD-19', name: '教育和培训体系', category: '体系', type: 2, scoringMethod: '是否有年度教育培训计划？记录是否能证明关键岗位获得资质？', dataSource: '培训计划及考核记录', status: 0 },
  { id: '29', code: 'AUD-20', name: '下级供应商管理', category: '采购', type: 2, scoringMethod: '是否有二级供方评审与淘汰程序？是否将PCN/环保等要求传递给下级？', dataSource: '供方管理SOP及协议', status: 0 },
  { id: '30', code: 'AUD-21', name: '供应链稳定性', category: '采购', type: 2, scoringMethod: '关键材料是否有第二来源(双重采购)？是否有应对海外供应中断的储备预案？', dataSource: '采购策略/储备计划', status: 0 },
  { id: '31', code: 'AUD-22', name: 'EHS现场执行', category: 'EHS', type: 2, scoringMethod: '员工安全培训？特种作业持证？消防通过？危废合规处置？一年内无政府处罚？', dataSource: '三同时验收/危废合同', status: 0 },
  { id: '32', code: 'AUD-23', name: '文件控制', category: '体系', type: 2, scoringMethod: '是否定义了文件的审核人和授权人？', dataSource: '受控文件清单', status: 0 },
  { id: '33', code: 'AUD-24', name: '内部审核', category: '体系', type: 2, scoringMethod: '是否严格执行内部审核？最近的内审计划及不符合项闭环情况？', dataSource: '内审计划与报告', status: 0 },
  { id: '34', code: 'AUD-25', name: '业务连续性(BCP)', category: '管理', type: 2, scoringMethod: '是否有业务连续性计划(BCP)？多久审查一次？', dataSource: '最新BCP预案', status: 0 },
  { id: '35', code: 'AUD-26', name: '企业社会责任(CSR)', category: '管理', type: 2, scoringMethod: '是否发布CSR/可持续发展报告？是否有行为准则？', dataSource: 'CSR报告/合规准则', status: 0 },
  { id: '36', code: 'AUD-27', name: '业务IT系统建设', category: 'IT', type: 2, scoringMethod: '是否实施ERP/MES？是否有IT保障数据完整性、信息安全与访问权限控制？', dataSource: '系统截图/IT制度', status: 0 },
  { id: '37', code: 'AUD-28', name: '顾客满意度', category: '服务', type: 2, scoringMethod: '是否定期调查顾客满意度，并与管理层分享制定改善计划？', dataSource: '满意度调查报告', status: 0 },
  { id: '38', code: 'AUD-29', name: '外包管理', category: '采购', type: 2, scoringMethod: '若涉及外包，是否签订合同明确目的、责任与安全管理？', dataSource: '外包管理规定与合同', status: 0 },
];

export const getMetricPage = async (params: any) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = mockData;
      if (params.name) filtered = filtered.filter(i => i.name.includes(params.name));
      if (params.category) filtered = filtered.filter(i => i.category === params.category);
      resolve({ list: filtered, total: filtered.length });
    }, 300);
  });
};
