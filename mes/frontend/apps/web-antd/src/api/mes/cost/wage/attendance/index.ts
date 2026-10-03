// 文件路径：src/api/mes/cost/wage/attendance/index.ts

export namespace MesCostWageAttendApi {
  export interface AttendanceRecord {
    id: number;
    attendDate: string;       // 考勤日期
    teamName: string;         // 归属班组
    empNo: string;            // 员工工号
    empName: string;          // 员工姓名
    shiftName: string;        // 班次 (白班/夜班)
    clockIn?: string;         // 上班打卡时间
    clockOut?: string;        // 下班打卡时间
    sysRegularHours: number;  // 系统推算正班工时
    sysOvertimeHours: number; // 系统推算加班工时
    adjRegularHours: number;  // 修正后正班工时 (允许人工改)
    adjOvertimeHours: number; // 修正后加班工时 (允许人工改)
    nightShiftSubsidy: number;// 夜班补贴次数 (0 或 1)
    status: number;           // 状态 (0-待核对, 1-已核对)
    remark?: string;          // 考勤异常说明
  }
}

// 深度模拟业务场景数据
let mockData: MesCostWageAttendApi.AttendanceRecord[] = [
  { id: 1, attendDate: '2026-02-26', teamName: '组装A线', empNo: 'EMP-0012', empName: '张建国', shiftName: '白班', clockIn: '07:55', clockOut: '20:05', sysRegularHours: 8, sysOvertimeHours: 3.5, adjRegularHours: 8, adjOvertimeHours: 3.5, nightShiftSubsidy: 0, status: 1 },
  { id: 2, attendDate: '2026-02-26', teamName: 'CNC夜班组', empNo: 'EMP-0033', empName: '赵铁柱', shiftName: '夜班', clockIn: '19:50', clockOut: '08:10', sysRegularHours: 8, sysOvertimeHours: 3.5, adjRegularHours: 8, adjOvertimeHours: 3.5, nightShiftSubsidy: 1, status: 0 },
  // 漏打卡异常数据，系统推算工时为0，等待主任修正
  { id: 3, attendDate: '2026-02-26', teamName: '组装A线', empNo: 'EMP-0056', empName: '李秀兰', shiftName: '白班', clockIn: '07:58', clockOut: '', sysRegularHours: 0, sysOvertimeHours: 0, adjRegularHours: 0, adjOvertimeHours: 0, nightShiftSubsidy: 0, status: 0, remark: '下班卡缺失' },
  { id: 4, attendDate: '2026-02-25', teamName: '组装A线', empNo: 'EMP-0012', empName: '张建国', shiftName: '白班', clockIn: '07:50', clockOut: '17:30', sysRegularHours: 8, sysOvertimeHours: 0, adjRegularHours: 8, adjOvertimeHours: 0, nightShiftSubsidy: 0, status: 1 },
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function getAttendPage(params: any) {
  await delay(300);
  let list = [...mockData];

  if (params.attendDate) list = list.filter(item => item.attendDate === params.attendDate);
  if (params.teamName) list = list.filter(item => item.teamName.includes(params.teamName));
  if (params.empName) list = list.filter(item => item.empName.includes(params.empName) || item.empNo.includes(params.empName));
  if (params.status !== undefined && params.status !== '') list = list.filter(item => String(item.status) === String(params.status));

  const total = list.length;
  const start = (params.pageNo - 1) * params.pageSize;
  const end = start + params.pageSize;
  return { list: list.slice(start, end), total };
}

/** 独立接口：行内保存修正后的有效工时 */
export async function updateAttendHours(id: number, field: string, value: number) {
  await delay(100);
  const item = mockData.find(i => i.id === id);
  if (item && item.status === 0) {
    (item as any)[field] = value;
  }
  return { code: 200, data: true, msg: 'success' };
}

/** 批量核对确认：将状态从待核对变更为已核对锁定 */
export async function verifyAttendanceList(ids: number[]) {
  await delay(400);
  let verifiedCount = 0;
  mockData = mockData.map(item => {
    if (ids.includes(item.id) && item.status === 0) {
      verifiedCount++;
      return { ...item, status: 1 };
    }
    return item;
  });
  return { code: 200, data: verifiedCount, msg: `成功核对锁定 ${verifiedCount} 条工时记录` };
}

/** 撤销核对 */
export async function unverifyAttendanceList(ids: number[]) {
  await delay(300);
  mockData = mockData.map(item => {
    if (ids.includes(item.id) && item.status === 1) return { ...item, status: 0 };
    return item;
  });
  return { code: 200, data: true, msg: 'success' };
}
