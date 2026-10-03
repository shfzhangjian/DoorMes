import { requestClient } from '#/api/request';

export namespace MesRouteApi {

  // 1. 投料 (Material) - 对应后端 RouteProcessInput
  export interface RouteProcessInput {
    id?: number;
    materialId: number;
    materialCode?: string;
    materialName?: string;
    standardQty: number; // 标准用量
    lossRate: number;    // 损耗率
    unit: string;
  }

  // 2. 岗位 (Man) - 对应后端 RouteProcessPost
  export interface RouteProcessPost {
    id?: number;
    postCode: string;   // 岗位编码
    postName?: string;  // 岗位名称
    skillLevel: string; // L1-L5
    stdManHour: number; // 标准工时
    minPerson: number;  // 最少人数
  }

  // 3. SOP (Method) - 对应后端 RouteProcessSop
  export interface RouteProcessSop {
    id?: number;
    docCode: string;
    docName?: string;
    docUrl: string;
    version: string;
    critical: boolean; // 是否强制阅读
  }

  export interface Route {
    id?: number;
    code: string;
    name: string;
    productId: number;
    productName?: string;
    productCode?: string;
    productSpec?: string;
    productUnit?: string;
    version: string;
    active: boolean;
    status: number;

    // 🔥 新增：工艺整体默认良率(%)，用于宏观产能评估
    defaultYield?: number;

    remark?: string;
    createTime?: string;
    processes?: RouteProcess[];
  }

  export interface RouteProcess {
    id?: number;
    routeId?: number;
    processId: number;
    processCode?: string;
    processName?: string;
    sequence: number;

    // 🔥 新增：节点性质 (START/NORMAL/INSPECT/END) 替代原本单一的 isKeyNode
    nodeType?: string;
    // 🔥 新增：工序理论良率(%)，APS逆向算料的核心参数！
    yieldRate?: number;

    keyNode: boolean; // 兼容保留
    standardTime: number;
    timeUnit: string;
    remark?: string;
    params?: RouteProcessParam[];
    inputs?: RouteProcessInput[]; // 料
    posts?: RouteProcessPost[];   // 人
    sops?: RouteProcessSop[];     // 法

    _tempId?: string; // 辅助字段
  }

  export interface RouteProcessParam {
    id?: number;
    paramCode: string;
    paramName: string;
    paramType: string;
    standardValue: string;
    minValue?: number;
    maxValue?: number;
    unit?: string;
    critical: boolean;
    remark?: string;
  }
}

export function getRoutePage(params: any) { return requestClient.get('/mes/base/route/page', { params }); }
export function getRoute(id: number) { return requestClient.get<MesRouteApi.Route>(`/mes/base/route/get?id=${id}`); }
export function createRoute(data: MesRouteApi.Route) { return requestClient.post('/mes/base/route/create', data); }
export function updateRoute(data: MesRouteApi.Route) { return requestClient.put('/mes/base/route/update', data); }
export function deleteRoute(id: number) { return requestClient.delete(`/mes/base/route/delete?id=${id}`); }
export function deleteRouteList(ids: number[]) { return requestClient.delete(`/mes/base/route/delete-batch?ids=${ids.join(',')}`); }
export function getProcessList(params: any) { return requestClient.get('/mes/base/process/page', { params }); }
export function exportRoute(params: any) { return requestClient.download('/mes/base/route/export-excel', params); }
