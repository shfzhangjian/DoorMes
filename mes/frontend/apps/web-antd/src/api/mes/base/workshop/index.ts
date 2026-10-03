import { requestClient } from '#/api/request';

export namespace MesWorkshopApi {
  /** MES车间产线定义信息 */
  export interface Workshop {
    id: number; // 主键ID
    parentId?: number; // 父节点ID
    code?: string; // 编号
    name?: string; // 名称
    type?: number; // 节点类型
    manager: string; // 负责人
    area: number; // 面积(㎡)
    sort?: number; // 排序
    status?: number; // 状态
    children?: Workshop[];
  }
}

/** 查询MES车间产线定义列表 */
export function getWorkshopList(params: any) {
  return requestClient.get<MesWorkshopApi.Workshop[]>(
    '/mes/base/workshop/list',
    { params },
  );
}

/** 查询MES车间产线定义详情 */
export function getWorkshop(id: number) {
  return requestClient.get<MesWorkshopApi.Workshop>(
    `/mes/base/workshop/get?id=${id}`,
  );
}

/** 新增MES车间产线定义 */
export function createWorkshop(data: MesWorkshopApi.Workshop) {
  return requestClient.post('/mes/base/workshop/create', data);
}

/** 修改MES车间产线定义 */
export function updateWorkshop(data: MesWorkshopApi.Workshop) {
  return requestClient.put('/mes/base/workshop/update', data);
}

/** 删除MES车间产线定义 */
export function deleteWorkshop(id: number) {
  return requestClient.delete(`/mes/base/workshop/delete?id=${id}`);
}

/** 导出MES车间产线定义 */
export function exportWorkshop(params: any) {
  return requestClient.download('/mes/base/workshop/export-excel', { params });
}

