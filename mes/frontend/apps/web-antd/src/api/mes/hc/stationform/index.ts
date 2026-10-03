import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcStationFormApi {
  export interface StationFormItem {
    id?: number;
    formId?: number;
    itemSeq?: number;
    itemCategory?: string;
    stepNode?: string;
    itemName?: string;
    standardText?: string;
    valueMode?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    fieldDefinitionsJson?: string;
    fieldValuesJson?: string;
    defaultResult?: string;
    requiredFlag?: boolean;
    remark?: string;
  }

  export interface StationForm {
    id?: number;
    formCode?: string;
    formName?: string;
    processCode?: string;
    processName?: string;
    triggerTimingCode?: string;
    triggerTimingName?: string;
    needConfirm?: boolean;
    sortNo?: number;
    status?: number;
    schemaJson?: string;
    presetHeaderDataJson?: string;
    presetItems?: StationFormItem[];
    remark?: string;
    createTime?: string;
    items?: StationFormItem[];
  }

  export interface StationFormImportResp {
    status?: string;
    sourceFileName?: string;
    sourceSheetName?: string;
    totalRows?: number;
    successCount?: number;
    failureCount?: number;
    warningCount?: number;
    existing?: boolean;
    existingId?: number;
    form?: StationForm;
    messages?: string[];
    failures?: string[];
  }

  export interface StationFormImportConfirmReq {
    form: StationForm;
    overwriteExisting?: boolean;
  }

  export interface StationFormConfigPackageForm {
    checksum?: string;
    action?: 'INVALID' | 'NEW' | 'SAME' | 'UPDATE' | string;
    existingId?: number;
    existingFormName?: string;
    existingChecksum?: string;
    diffFields?: string[];
    form?: StationForm;
  }

  export interface StationFormConfigPackageResp {
    packageType?: string;
    packageVersion?: string;
    exportTime?: string;
    source?: string;
    status?: string;
    sourceFileName?: string;
    totalCount?: number;
    newCount?: number;
    updateCount?: number;
    sameCount?: number;
    skippedCount?: number;
    failureCount?: number;
    messages?: string[];
    failures?: string[];
    forms?: StationFormConfigPackageForm[];
  }

  export interface StationFormConfigPackageImportReq {
    configPackage: StationFormConfigPackageResp;
    overwriteExisting?: boolean;
  }

  export interface SimpleItem {
    id: number;
    formCode?: string;
    formName?: string;
    processCode?: string;
    processName?: string;
    triggerTimingCode?: string;
    triggerTimingName?: string;
    needConfirm?: boolean;
    sortNo?: number;
    status?: number;
    schemaJson?: string;
    remark?: string;
  }

  export interface ProcessOption {
    label: string;
    value: string;
  }
}

export async function getStationFormPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcStationFormApi.StationForm>>(
    '/mes/hc/base/station-form/page',
    { params },
  );
}

export async function getStationForm(id: number) {
  return requestClient.get<MesHcStationFormApi.StationForm>(
    `/mes/hc/base/station-form/get?id=${id}`,
  );
}

export async function getStationFormDetail(id: number) {
  return requestClient.get<MesHcStationFormApi.StationForm>(
    `/mes/hc/base/station-form/get-detail?id=${id}`,
  );
}

export async function createStationForm(data: MesHcStationFormApi.StationForm) {
  return requestClient.post<number>('/mes/hc/base/station-form/create', data);
}

export async function updateStationForm(data: MesHcStationFormApi.StationForm) {
  return requestClient.put<boolean>('/mes/hc/base/station-form/update', data);
}

export async function deleteStationForm(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/station-form/delete?id=${id}`);
}

export async function deleteStationFormList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/base/station-form/delete-list?ids=${ids.join(',')}`,
  );
}

export async function getStationFormSimpleList(processCode?: string) {
  return requestClient.get<MesHcStationFormApi.SimpleItem[]>(
    '/mes/hc/base/station-form/simple-list',
    { params: { processCode } },
  );
}

export async function resolvePublishedStationForm(params: {
  formType: string;
  grindingPass?: 'FIRST' | 'SECOND' | string;
  modelCode?: string;
  processCode: string;
}) {
  return requestClient.get<MesHcStationFormApi.StationForm | undefined>(
    '/mes/hc/base/station-form/resolve-published',
    { params },
  );
}

export async function getStationFormProcessOptions() {
  return requestClient.get<MesHcStationFormApi.ProcessOption[]>(
    '/mes/hc/base/station-form/process-options',
  );
}

export async function previewStationFormExcelImport(
  file: File,
  params: Record<string, any>,
) {
  return requestClient.upload<MesHcStationFormApi.StationFormImportResp>(
    '/mes/hc/base/station-form/excel-import/preview',
    {
      file,
      ...params,
    },
  );
}

export async function confirmStationFormExcelImport(
  data: MesHcStationFormApi.StationFormImportConfirmReq,
) {
  return requestClient.post<MesHcStationFormApi.StationFormImportResp>(
    '/mes/hc/base/station-form/excel-import/confirm',
    data,
  );
}

export function exportStationFormPreviewExcel(
  data: MesHcStationFormApi.StationForm,
) {
  return requestClient.download('/mes/hc/base/station-form/excel-preview/export', {
    data,
    method: 'POST',
  });
}

export function importStationFormPreviewExcel(
  file: File,
  data: MesHcStationFormApi.StationForm,
) {
  return requestClient.upload<MesHcStationFormApi.StationFormImportResp>(
    '/mes/hc/base/station-form/excel-preview/import',
    {
      file,
      formJson: JSON.stringify(data),
    },
  );
}

export function exportStationFormConfigPackage(ids: number[]) {
  return requestClient.download('/mes/hc/base/station-form/config-package/export', {
    params: { ids: ids.join(',') },
  });
}

export function previewStationFormConfigPackageImport(file: File) {
  return requestClient.upload<MesHcStationFormApi.StationFormConfigPackageResp>(
    '/mes/hc/base/station-form/config-package/import/preview',
    { file },
  );
}

export function confirmStationFormConfigPackageImport(
  data: MesHcStationFormApi.StationFormConfigPackageImportReq,
) {
  return requestClient.post<MesHcStationFormApi.StationFormConfigPackageResp>(
    '/mes/hc/base/station-form/config-package/import/confirm',
    data,
  );
}
