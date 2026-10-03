import { requestClient } from '#/api/request';

export namespace MesDefectCodeApi {
  export interface DefectCause {
    id?: number;
    defectCodeId?: number;
    defectCode?: string;
    defectName?: string;
    reasonCode?: string;
    reasonName: string;
    reasonType?: 'MAN' | 'MACHINE' | 'MATERIAL' | 'METHOD' | 'ENVIRONMENT';
    reasonDesc?: string;
    sort?: number;
    status?: number;
    remark?: string;
  }

  export interface DefectCode {
    id?: number;
    parentId: number;
    code: string;
    name: string;
    type: 'CATEGORY' | 'ITEM';
    level?: 'MINOR' | 'MAJOR' | 'CRITICAL';
    referencePicUrls?: string[];
    causes?: DefectCause[];
    sort?: number;
    status: number;
    remark?: string;
    createTime?: string;
    children?: DefectCode[];
  }

  export interface DefectCodeListReq {
    name?: string;
    type?: 'CATEGORY' | 'ITEM';
    status?: number;
  }
}

export async function getDefectCodeList(params?: MesDefectCodeApi.DefectCodeListReq) {
  return requestClient.get<MesDefectCodeApi.DefectCode[]>('/mes/quality/base/defect-code/list', { params });
}

export async function getDefectCode(id: number) {
  return requestClient.get<MesDefectCodeApi.DefectCode>(`/mes/quality/base/defect-code/get?id=${id}`);
}

export async function createDefectCode(data: MesDefectCodeApi.DefectCode) {
  return requestClient.post<number>('/mes/quality/base/defect-code/create', data);
}

export async function updateDefectCode(data: MesDefectCodeApi.DefectCode) {
  return requestClient.put<boolean>('/mes/quality/base/defect-code/update', data);
}

export async function deleteDefectCode(id: number) {
  return requestClient.delete<boolean>(`/mes/quality/base/defect-code/delete?id=${id}`);
}
