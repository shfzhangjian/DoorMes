import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';

import { requestClient } from '#/api/request';

export namespace MesHcProcessAnalysisApi {
  export type Aggregation =
    | 'AVG'
    | 'COUNT'
    | 'COUNT_DISTINCT'
    | 'MAX'
    | 'MIN'
    | 'SUM'
    | 'SUM_DISTINCT';

  export type DataType = 'BOOLEAN' | 'DATE' | 'DATETIME' | 'NUMBER' | 'TEXT';
  export type FieldRole = 'DETAIL' | 'DIMENSION' | 'METRIC' | 'TIME';

  export interface FieldDefinition {
    category: string;
    code: string;
    dataType: DataType;
    defaultAggregation?: Aggregation;
    description?: string;
    distinctKeyField?: string;
    drillable?: boolean;
    filterOperators: string[];
    label: string;
    role: FieldRole;
    sourcePath: string;
  }

  export interface Config {
    configJson: string;
    configName: string;
    configVersion: number;
    createTime?: string;
    defaultFlag: boolean;
    editable: boolean;
    id: number;
    ownerUserId: number;
    remark?: string;
    scopeType: 'PRIVATE' | 'SHARED';
    updateTime?: string;
  }

  export interface ConfigSaveReq {
    configJson: string;
    configName: string;
    configVersion?: number;
    defaultFlag?: boolean;
    id?: number;
    remark?: string;
    scopeType?: 'PRIVATE' | 'SHARED';
  }
}

const BASE_URL = '/mes/hc/plan/process-analysis';

export function getProcessAnalysisFieldCatalog() {
  return requestClient.get<MesHcProcessAnalysisApi.FieldDefinition[]>(
    `${BASE_URL}/field-catalog`,
  );
}

export function getProcessAnalysisSourceData(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.get<MesHcPlanOrderApi.ProcessPivotRow[]>(
    `${BASE_URL}/source-data`,
    { params },
  );
}

export function getProcessAnalysisConfigList() {
  return requestClient.get<MesHcProcessAnalysisApi.Config[]>(
    `${BASE_URL}/config/list`,
  );
}

export function createProcessAnalysisConfig(
  data: MesHcProcessAnalysisApi.ConfigSaveReq,
) {
  return requestClient.post<number>(`${BASE_URL}/config/create`, data);
}

export function updateProcessAnalysisConfig(
  data: MesHcProcessAnalysisApi.ConfigSaveReq,
) {
  return requestClient.put(`${BASE_URL}/config/update`, data);
}

export function deleteProcessAnalysisConfig(id: number) {
  return requestClient.delete(`${BASE_URL}/config/delete?id=${id}`);
}
