import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesProcessApi {
  // 工序-工位能力矩阵项
  export interface ProcessStation {
    id?: number;
    stationId: number;
    stationName?: string; // 回显
    defaultStatus: boolean; // 对应后端 VO 的 defaultStatus
    sort: number;
    remark?: string;
  }

  /** MES标准工序信息 */
  export interface Process {
    id?: number; // 主键ID
    code?: string; // 工序编码
    name?: string; // 工序名称
    workshopId?: number; // 默认车间ID
    workshopCode?: string; // 车间编码
    workshopName?: string; // 车间名称
    processType?: string; // 工序类型

    splitRuleType?: string; // 拆分规则类型 (CAPACITY/CARRIER/CONTINUOUS)
    maxSplitQty?: number; // 最大拆分额度 (容量或载具上限)

    remark?: string; // 备注
    status?: number; // 状态
    bindStation?: boolean; // 是否绑定工位
    stations?: ProcessStation[];
  }
}

/** 查询MES标准工序分页 */
export function getProcessPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesProcessApi.Process>>(
    '/mes/base/process/page',
    { params },
  );
}

/** 查询MES标准工序详情 */
export function getProcess(id: number) {
  return requestClient.get<MesProcessApi.Process>(
    `/mes/base/process/get?id=${id}`,
  );
}

/** 新增MES标准工序 */
export function createProcess(data: MesProcessApi.Process) {
  return requestClient.post('/mes/base/process/create', data);
}

/** 修改MES标准工序 */
export function updateProcess(data: MesProcessApi.Process) {
  return requestClient.put('/mes/base/process/update', data);
}

/** 删除MES标准工序 */
export function deleteProcess(id: number) {
  return requestClient.delete(`/mes/base/process/delete?id=${id}`);
}

/** 批量删除MES标准工序 */
export function deleteProcessList(ids: number[]) {
  return requestClient.delete(
    `/mes/base/process/delete-list?ids=${ids.join(',')}`,
  );
}

/** 导出MES标准工序 */
export function exportProcess(params: any) {
  return requestClient.download('/mes/process/export-excel', { params });
}
