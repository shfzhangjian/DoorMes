import type { PageParam, PageResult } from '@vben/request';

export namespace MesMaintRuleApi {
  export interface Rule {
    id?: string | number;
    ruleNo?: string;         // 规程编号
    ruleName?: string;       // 规程名称
    maintType?: string;      // 维保类型 (一级/二级/大修)
    cycleValue?: number;     // 周期值
    cycleUnit?: string;      // 周期单位 (YEAR/MONTH/WEEK)
    principal?: string;      // 默认责任人
    taskDesc?: string;       // 任务说明
    requirement?: string;    // 维保要求
    status?: number;         // 状态 (1启用 0停用)
    createTime?: string;
    devices?: RuleDevice[];  // 绑定的设备清单
  }

  export interface RuleDevice {
    id?: string | number;
    ruleId?: string | number;
    deviceCode?: string;     // 设备编号
    deviceName?: string;     // 设备名称
    deviceType?: string;     // 设备分类
    lastMaintDate?: string;  // 上次保养日期 (核心：用于推算计划)
    sort?: number;
  }
}

// ================= Mock 数据池 =================
const mockRuleDevices: MesMaintRuleApi.RuleDevice[] = [
  { id: 101, ruleId: 1, deviceCode: 'EQ-MIX-01', deviceName: '1#真空搅拌机', deviceType: 'MIXER', lastMaintDate: '2025-12-15', sort: 1 },
  { id: 102, ruleId: 1, deviceCode: 'EQ-MIX-02', deviceName: '2#真空搅拌机', deviceType: 'MIXER', lastMaintDate: '2026-01-10', sort: 2 },

  { id: 201, ruleId: 2, deviceCode: 'EQ-COA-01', deviceName: '1#精密涂布机', deviceType: 'COATER', lastMaintDate: '2026-02-01', sort: 1 },
  { id: 202, ruleId: 2, deviceCode: 'EQ-COA-02', deviceName: '2#精密涂布机', deviceType: 'COATER', lastMaintDate: '2026-02-15', sort: 2 },

  { id: 301, ruleId: 3, deviceCode: 'EQ-SLI-01', deviceName: '1#高速分条机', deviceType: 'SLITTER', lastMaintDate: '2025-11-20', sort: 1 },
];

const mockRules: MesMaintRuleApi.Rule[] = [
  { id: 1, ruleNo: 'MR-MIX-M01', ruleName: '搅拌机月度一级保养规程', maintType: '一级保养', cycleValue: 1, cycleUnit: 'MONTH', principal: '机修班长', taskDesc: '减速机润滑油位检查、电机散热风扇除尘、主轴密封圈探伤。', requirement: '油位处于上下限之间，无漏油；密封圈无肉眼可见磨损裂纹。', status: 1, createTime: '2026-01-05 09:00:00' },
  { id: 2, ruleId: 2, ruleNo: 'MR-COA-W01', ruleName: '涂布机周度精细保养', maintType: '日常巡检', cycleValue: 1, cycleUnit: 'WEEK', principal: '涂布主操', taskDesc: '背辊表面残胶彻底清理、模头唇口无水乙醇擦拭、静电针清理。', requirement: '背辊表面绝对光滑；模头唇口无任何异物堵塞。', status: 1, createTime: '2026-01-10 10:30:00' },
  { id: 3, ruleId: 3, ruleNo: 'MR-SLI-Y01', ruleName: '分条机年度大修规程', maintType: '三级大修', cycleValue: 1, cycleUnit: 'YEAR', principal: '设备工程师', taskDesc: '收卷滑差轴全拆解更换气囊、主传动同步带更换、动平衡校准。', requirement: '装配后整机动平衡震动<2mm/s，滑差轴保压24小时不漏气。', status: 1, createTime: '2026-02-01 14:00:00' },
];

export function getRulePage(params: PageParam) { return Promise.resolve({ list: mockRules, total: mockRules.length } as PageResult<MesMaintRuleApi.Rule>); }
export function getRule(id: string | number) {
  const rule = mockRules.find(i => i.id == id);
  const devices = mockRuleDevices.filter(i => i.ruleId == id);
  return Promise.resolve(JSON.parse(JSON.stringify({ ...rule, devices })) as MesMaintRuleApi.Rule);
}
export function createRule(data: MesMaintRuleApi.Rule) { return Promise.resolve({ success: true, data }); }
export function updateRule(data: MesMaintRuleApi.Rule) { return Promise.resolve({ success: true, data }); }
export function deleteRuleList(ids: (string | number)[]) { return Promise.resolve({ success: true }); }
export function getRuleDeviceListById(ruleId: string | number) {
  return Promise.resolve(JSON.parse(JSON.stringify(mockRuleDevices.filter(i => i.ruleId == ruleId))) as MesMaintRuleApi.RuleDevice[]);
}
