// 文件路径：src/api/mes/cost/wage/performance/index.ts

export namespace MesCostWagePerfApi {
  export interface PerfRecord {
    id: number;
    recordDate: string;    // 发生日期
    empNo: string;         // 员工工号
    empName: string;       // 员工姓名
    type: number;          // 奖惩类型 (1-奖励, 2-扣款)
    category: string;      // 绩效类目 (质量表现, 5S考评, 生产效率, 考勤纪律, 其它)
    amount: number;        // 金额 (元)
    source: number;        // 数据来源 (1-手工录入, 2-质检系统推送, 3-考勤系统推送)
    status: number;        // 状态 (0-草稿/待确认, 1-已核对锁定)
    reason?: string;       // 事由说明
  }
}

let mockData: MesCostWagePerfApi.PerfRecord[] = [
  { id: 1, recordDate: '2026-02-25', empNo: 'EMP-0012', empName: '张建国', type: 1, category: '生产效率', amount: 200.00, source: 1, status: 1, reason: '当周产量超标 15%，发放超产激励' },
  { id: 2, recordDate: '2026-02-26', empNo: 'EMP-0056', empName: '李秀兰', type: 2, category: '质量表现', amount: 50.00, source: 2, status: 0, reason: '关联 NCR-2602-081，因操作失误导致物料报废' },
  { id: 3, recordDate: '2026-02-26', empNo: 'EMP-0033', empName: '赵铁柱', type: 1, category: '5S考评', amount: 100.00, source: 1, status: 0, reason: '车间区域 5S 评比第一名' },
  { id: 4, recordDate: '2026-02-27', empNo: 'EMP-0088', empName: '王小明', type: 2, category: '考勤纪律', amount: 30.00, source: 3, status: 0, reason: '早会迟到 15 分钟' },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getPerfPage(params: any) {
  await delay(300);
  let list = [...mockData];

  if (params.recordDate) list = list.filter(item => item.recordDate === params.recordDate);
  if (params.empName) list = list.filter(item => item.empName.includes(params.empName) || item.empNo.includes(params.empName));
  if (params.type) list = list.filter(item => String(item.type) === String(params.type));
  if (params.status !== undefined && params.status !== '') list = list.filter(item => String(item.status) === String(params.status));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

export async function getPerfRecord(id: number) {
  await delay(200);
  return mockData.find(item => item.id === id) || null;
}

export async function createPerfRecord(data: MesCostWagePerfApi.PerfRecord) {
  await delay(300);
  data.id = Math.floor(Math.random() * 10000);
  data.source = 1; // 手工录入
  data.status = 0; // 默认草稿待确认
  mockData.unshift(data);
  return { code: 200, data: true, msg: 'success' };
}

export async function updatePerfRecord(data: MesCostWagePerfApi.PerfRecord) {
  await delay(300);
  const index = mockData.findIndex(item => item.id === data.id);
  if (index > -1 && mockData[index].status === 0) {
    mockData[index] = { ...mockData[index], ...data };
  }
  return { code: 200, data: true, msg: 'success' };
}

export async function deletePerfRecords(ids: number[]) {
  await delay(300);
  mockData = mockData.filter(item => !(ids.includes(item.id) && item.status === 0));
  return { code: 200, data: true, msg: 'success' };
}

/** 批量核对确认：变更为已核对锁定 */
export async function approvePerfRecords(ids: number[]) {
  await delay(400);
  let approvedCount = 0;
  mockData = mockData.map(item => {
    if (ids.includes(item.id) && item.status === 0) {
      approvedCount++;
      return { ...item, status: 1 };
    }
    return item;
  });
  return { code: 200, data: approvedCount, msg: `成功审核锁定 ${approvedCount} 条奖惩记录` };
}
