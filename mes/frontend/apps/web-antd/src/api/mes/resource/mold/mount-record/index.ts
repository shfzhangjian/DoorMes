import type { PageParam, PageResult } from '@vben/request';

export namespace MesMoldMountApi {
  export interface Record {
    id?: string | number;
    // 基础关联信息
    moldCode?: string;
    moldName?: string;
    deviceCode?: string;     // 挂载设备
    deviceName?: string;
    workOrderNo?: string;    // 关联生产派工单

    // 上模动作
    mountTime?: string;
    mounter?: string;

    // 下模动作与状态评估
    teardownTime?: string;   // 为空代表正在机生产
    teardowner?: string;
    afterStatus?: number;    // 下模后状态评估: 10完好入库, 30维修保养, 40报废
    remark?: string;

    recordStatus?: 'MOUNTED' | 'TEARDOWN'; // 履历本身状态: 在机 / 已下线
  }
}

// ================= Mock 数据池 (严格呼应模具台账的状态) =================
const mockRecords: MesMoldMountApi.Record[] = [
  // 呼应 1：MD-INJ-001 当前在机生产 (无下模时间)
  { id: 1, moldCode: 'MD-INJ-001', moldName: 'IP15手机壳热流道注塑模', deviceCode: 'EQ-INJ-05', deviceName: '5#海天注塑机', workOrderNo: 'WO-20260221-001', mountTime: '2026-02-21 08:30:00', mounter: '装模工-张三', teardownTime: '', teardowner: '', recordStatus: 'MOUNTED', remark: '准备生产苹果外壳10000件' },

  // 呼应 2：一条已经完成闭环的旧历史记录
  { id: 2, moldCode: 'MD-STM-002', moldName: '前机盖大型冲压模具', deviceCode: 'EQ-STM-01', deviceName: '1#千吨级冲床', workOrderNo: 'WO-20260215-002', mountTime: '2026-02-15 09:00:00', mounter: '装模工-李四', teardownTime: '2026-02-18 17:30:00', teardowner: '装模工-李四', afterStatus: 10, recordStatus: 'TEARDOWN', remark: '订单完成，正常下线入库，天车运回A区' },
];

export function getMountRecordPage(params: PageParam) {
  return Promise.resolve({ list: mockRecords, total: mockRecords.length } as PageResult<MesMoldMountApi.Record>);
}

export function getMountRecord(id: string | number) {
  return Promise.resolve(mockRecords.find(i => i.id == id) as MesMoldMountApi.Record);
}

// 模拟上模动作
export function createMountRecord(data: MesMoldMountApi.Record) {
  // 【真实业务逻辑】：在这里，后端服务会在插入履历的同时，去更新 `mold_ledger` 表，
  // 将 moldCode 对应模具的 status 改为 20(在机)，location 改为 data.deviceName
  return Promise.resolve({ success: true, data });
}

// 模拟下模动作
export function teardownMold(data: MesMoldMountApi.Record) {
  // 【真实业务逻辑】：更新该条履历的下模信息。同时更新 `mold_ledger` 表，
  // 将 status 改为 data.afterStatus (如10入库/30维修)，location 改回默认库房或维修区
  return Promise.resolve({ success: true, data });
}

export function deleteMountRecordList(ids: (string | number)[]) {
  return Promise.resolve({ success: true });
}
