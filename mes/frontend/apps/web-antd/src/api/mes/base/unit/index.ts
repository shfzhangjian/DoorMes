import type { PageParam, PageResult } from '@vben/request';
import { requestClient } from '#/api/request';

export namespace MesUnitApi {
  /** MES计量单位信息 */
  export interface Unit {
    id: number; // 主键ID
    code?: string; // 单位符号
    name?: string; // 单位名称
    category?: string; // 维度
    base?: boolean; // 基准单位
    ratio?: number; // 换算率
    precision: number; // 保留小数位数
    status?: number; // 状态
    remark: string; // 备注
  }
}

/** 查询MES计量单位分页 */
export function getUnitPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesUnitApi.Unit>>(
    '/mes/base/unit/page',
    { params },
  );
}

export async function getUnitSelectOptions() {
  const res = await getUnitPage({ pageNo: 1, pageSize: 200, status: 0 });
  return (res.list || []).map((item) => ({
    value: item.id,
    label: [item.code, item.name].filter(Boolean).join('/'),
    code: item.code,
    name: item.name,
    status: item.status,
  }));
}

/** 查询MES计量单位详情 */
export function getUnit(id: number) {
  return requestClient.get<MesUnitApi.Unit>(
    `/mes/base/unit/get?id=${id}`,
  );
}

/** 新增MES计量单位 */
export function createUnit(data: MesUnitApi.Unit) {
  return requestClient.post('/mes/base/unit/create', data);
}

/** 修改MES计量单位 */
export function updateUnit(data: MesUnitApi.Unit) {
  return requestClient.put('/mes/base/unit/update', data);
}

/** 删除MES计量单位 */
export function deleteUnit(id: number) {
  return requestClient.delete(`/mes/base/unit/delete?id=${id}`);
}

/** 批量删除MES计量单位 */
export function deleteUnitList(ids: number[]) {
  return requestClient.delete(
    `/mes/base/unit/delete-list?ids=${ids.join(',')}`,
  );
}

/** 导出MES计量单位 */
export function exportUnit(params: any) {
  return requestClient.download('/mes/base/unit/export-excel', { params });
}


