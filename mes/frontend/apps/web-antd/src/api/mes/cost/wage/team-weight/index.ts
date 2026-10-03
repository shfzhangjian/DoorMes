// 文件路径：src/api/mes/cost/wage/team-weight/index.ts

export namespace MesCostWageTeamApi {
  export interface TeamNode {
    id: string;
    name: string;
    type: string; // 'workshop' | 'team'
    children?: TeamNode[];
  }

  export interface TeamMember {
    id: number;
    teamId: string;       // 归属班组ID
    empNo: string;        // 员工工号
    empName: string;      // 员工姓名
    role: string;         // 班组角色 (如: 线长, 核心操作工, 辅助工)
    weight: number;       // 分配权重系数 (基准为 1.0)
    status: number;       // 状态 (1-在位, 0-借调/离岗)
    joinDate: string;     // 入组日期
  }
}

// 模拟左侧：车间 -> 班组 树
const mockTeamTree: MesCostWageTeamApi.TeamNode[] = [
  {
    id: 'W-1111', name: 'CNC柔性加工中心', type: 'workshop',
    children: [
      { id: 'T-1111-01', name: 'CNC白班组', type: 'team' },
      { id: 'T-1111-02', name: 'CNC夜班组', type: 'team' },
    ]
  },
  {
    id: 'W-1121', name: '人工组装车间', type: 'workshop',
    children: [
      { id: 'T-1121-A', name: '组装A线', type: 'team' },
      { id: 'T-1121-B', name: '组装B线', type: 'team' },
    ]
  }
];

// 模拟右侧：班组人员名单及权重
let mockMembers: MesCostWageTeamApi.TeamMember[] = [
  { id: 1, teamId: 'T-1121-A', empNo: 'EMP-0012', empName: '张建国', role: '线长', weight: 1.2, status: 1, joinDate: '2025-01-15' },
  { id: 2, teamId: 'T-1121-A', empNo: 'EMP-0056', empName: '李秀兰', role: '熟练操作工', weight: 1.0, status: 1, joinDate: '2025-03-01' },
  { id: 3, teamId: 'T-1121-A', empNo: 'EMP-0088', empName: '王小明', role: '学徒/辅助', weight: 0.8, status: 1, joinDate: '2026-01-10' },
  { id: 4, teamId: 'T-1111-01', empNo: 'EMP-0033', empName: '赵铁柱', role: 'CNC调机员', weight: 1.5, status: 1, joinDate: '2024-11-20' },
  { id: 5, teamId: 'T-1111-01', empNo: 'EMP-0034', empName: '孙大强', role: '上下料工', weight: 1.0, status: 0, joinDate: '2025-06-18' }, // 0表示借调外出
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getTeamTree() {
  await delay(200);
  return mockTeamTree;
}

export async function getTeamMemberPage(params: any) {
  await delay(300);
  let list = [...mockMembers];

  if (params.teamId) list = list.filter(item => item.teamId === params.teamId);
  if (params.empName) list = list.filter(item => item.empName.includes(params.empName) || item.empNo.includes(params.empName));
  if (params.status !== undefined && params.status !== '') list = list.filter(item => String(item.status) === String(params.status));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

export async function getTeamMember(id: number) {
  await delay(200);
  return mockMembers.find(item => item.id === id) || null;
}

export async function createTeamMember(data: MesCostWageTeamApi.TeamMember) {
  await delay(300);
  data.id = Math.floor(Math.random() * 10000);
  data.joinDate = new Date().toISOString().slice(0, 10);
  mockMembers.unshift(data);
  return { code: 200, data: true, msg: 'success' };
}

export async function updateTeamMember(data: MesCostWageTeamApi.TeamMember) {
  await delay(300);
  const index = mockMembers.findIndex(item => item.id === data.id);
  if (index > -1) mockMembers[index] = { ...mockMembers[index], ...data };
  return { code: 200, data: true, msg: 'success' };
}

export async function deleteTeamMembers(ids: number[]) {
  await delay(300);
  mockMembers = mockMembers.filter(item => !ids.includes(item.id));
  return { code: 200, data: true, msg: 'success' };
}

/** 快捷行内修改权重系数 */
export async function updateMemberWeight(id: number, weight: number) {
  await delay(100);
  const item = mockMembers.find(i => i.id === id);
  if (item) item.weight = weight;
  return { code: 200, data: true, msg: 'success' };
}
