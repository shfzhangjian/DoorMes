import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProcessFormApi {
  export interface TemplateItem {
    id?: number;
    templateId?: number;
    versionId?: number;
    sectionId?: number;
    areaType?: string;
    itemSeq?: number;
    fieldKey?: string;
    fieldLabel?: string;
    itemCategory?: string;
    stepNode?: string;
    standardText?: string;
    unit?: string;
    valueMode?: string;
    controlType?: string;
    defaultValue?: string;
    defaultResult?: string;
    requiredFlag?: boolean;
    sourceSheet?: string;
    sourceCell?: string;
    sourceRowJson?: string;
    bindSourceType?: string;
    bindSourceKey?: string;
    remark?: string;
  }

  export interface TemplateVersion {
    id?: number;
    templateId?: number;
    templateCode?: string;
    versionNo?: string;
    isCurrent?: boolean;
    effectiveDate?: string;
    sourceFileName?: string;
    sourceFilePath?: string;
    sourceFileUrl?: string;
    sourceFileHash?: string;
    sheetJson?: string;
    layoutJson?: string;
    parseStatus?: string;
    parseMessage?: string;
    remark?: string;
  }

  export interface Template {
    id?: number;
    templateCode?: string;
    templateName?: string;
    processCode?: string;
    processName?: string;
    modelScope?: string;
    modelCode?: string;
    modelName?: string;
    formType?: string;
    formTypeName?: string;
    currentVersionId?: number;
    status?: string;
    needConfirm?: boolean;
    sortNo?: number;
    remark?: string;
    createTime?: string;
    currentVersion?: TemplateVersion;
    items?: TemplateItem[];
  }

  export interface TemplateOption {
    id?: number;
    templateCode?: string;
    templateName?: string;
    processCode?: string;
    processName?: string;
    modelScope?: string;
    modelCode?: string;
    modelName?: string;
    formType?: string;
    formTypeName?: string;
  }

  export interface RecordItem {
    id?: number;
    recordId?: number;
    templateItemId?: number;
    itemSeq?: number;
    fieldKey?: string;
    fieldLabel?: string;
    itemCategory?: string;
    stepNode?: string;
    standardText?: string;
    unit?: string;
    valueMode?: string;
    controlType?: string;
    actualValue?: string;
    actualValue2?: string;
    actualNumber?: number;
    actualTime?: string;
    resultFlag?: string;
    abnormalRemark?: string;
    sourceRowJson?: string;
  }

  export interface Record {
    id?: number;
    recordNo?: string;
    templateId?: number;
    versionId?: number;
    templateCode?: string;
    templateName?: string;
    processCode?: string;
    processName?: string;
    modelCode?: string;
    modelName?: string;
    formType?: string;
    formTypeName?: string;
    recordDate?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    batchNo?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    recordStatus?: string;
    resultStatus?: string;
    fillUserId?: number;
    fillUserName?: string;
    fillTime?: string;
    confirmUserId?: number;
    confirmUserName?: string;
    confirmTime?: string;
    headerDataJson?: string;
    contextJson?: string;
    remark?: string;
    createTime?: string;
    template?: Template;
    items?: RecordItem[];
  }

  export interface RecordSignerConfirmReq {
    id: number;
    confirmUserId: number;
  }

  export interface LayoutHeaderItem {
    label?: string;
    value?: string;
    valueType?: 'DATE' | 'DATETIME' | 'NUMBER' | 'TEXT';
    bindKey?: string;
    bindField?: string;
    editable?: boolean;
  }

  export interface LayoutColumn {
    title?: string;
    width?: number;
  }

  export interface LayoutCell {
    colIndex?: number;
    rowSpan?: number;
    colSpan?: number;
    text?: string;
    bindKey?: string;
    bindField?: string;
    editable?: boolean;
    importRowIndex?: number;
    importColIndex?: number;
  }

  export interface LayoutRow {
    cells?: LayoutCell[];
  }

  export interface LayoutExcelReq {
    fileName?: string;
    sheetName?: string;
    title?: string;
    detailTitle?: string;
    visualMode?: string;
    importBodyStartRow?: number;
    importBodyMaxRows?: number;
    importStopPrefixes?: string[];
    headerItems?: LayoutHeaderItem[];
    columns?: LayoutColumn[];
    rows?: LayoutRow[];
    footerNotes?: string[];
  }

  export interface LayoutImportResp {
    headerValues?: Array<{
      bindField?: string;
      bindKey?: string;
      value?: string;
    }>;
    cellValues?: Array<{
      bindField?: string;
      bindKey?: string;
      bodyRowIndex?: number;
      colIndex?: number;
      value?: string;
    }>;
    messages?: string[];
    totalCellCount?: number;
  }
}

export function getProcessFormRecordPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcProcessFormApi.Record>>(
    '/mes/hc/base/process-form-record/page',
    { params },
  );
}

export function getProcessFormRecordDetail(id: number) {
  return requestClient.get<MesHcProcessFormApi.Record>(
    `/mes/hc/base/process-form-record/get-detail?id=${id}`,
  );
}

export function createProcessFormRecord(data: MesHcProcessFormApi.Record) {
  return requestClient.post<number>('/mes/hc/base/process-form-record/create', data);
}

export function updateProcessFormRecord(data: MesHcProcessFormApi.Record) {
  return requestClient.put<boolean>('/mes/hc/base/process-form-record/update', data);
}

export function deleteProcessFormRecord(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/process-form-record/delete?id=${id}`);
}

export function submitProcessFormRecord(id: number) {
  return requestClient.post<boolean>(`/mes/hc/base/process-form-record/submit?id=${id}`);
}

export function confirmProcessFormRecord(id: number) {
  return requestClient.post<boolean>(`/mes/hc/base/process-form-record/confirm?id=${id}`);
}

export function confirmProcessFormRecordBySigner(data: MesHcProcessFormApi.RecordSignerConfirmReq) {
  return requestClient.post<boolean>('/mes/hc/base/process-form-record/confirm-by-signer', data);
}

export function exportProcessFormRecordLayout(data: MesHcProcessFormApi.LayoutExcelReq) {
  return requestClient.download('/mes/hc/base/process-form-record/export-layout', {
    data,
    method: 'POST',
  });
}

export function importProcessFormRecordLayout(
  file: File,
  layout: MesHcProcessFormApi.LayoutExcelReq,
) {
  return requestClient.upload<MesHcProcessFormApi.LayoutImportResp>(
    '/mes/hc/base/process-form-record/import-layout',
    {
      file,
      layoutJson: JSON.stringify(layout),
    },
  );
}
