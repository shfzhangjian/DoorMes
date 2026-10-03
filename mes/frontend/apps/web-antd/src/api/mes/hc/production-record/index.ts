import type { GrindingConsumption } from '#/api/mes/hc/grinding-consumption';
import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProductionRecordApi {
  export interface PageReqVO extends PageParam {
    reportDateStart?: string;
    reportDateEnd?: string;
    modelCode?: string;
    productionBatchNo?: string;
    cutSizeMm?: string;
    recorderName?: string;
    status?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
  }

  export type ExportReqVO = Omit<PageReqVO, 'pageNo' | 'pageSize'>;

  export interface RecordRow {
    id?: number;
    reportDate?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    productionBatchNo?: string;
    cutSizeMm?: string;
    inputQty?: number;
    outputQty?: number;
    bladeModel?: string;
    bladeBatchNo?: string;
    bladeUseCount?: number;
    feltModel?: string;
    feltBatchNo?: string;
    feltUseCount?: number;
    feltUseDays?: number;
    bladeReplaceReason?: string;
    recorderName?: string;
    recordSource?: string;
    recordTime?: string;
    confirmerName?: string;
    confirmTime?: string;
    status?: string;
    sourceType?: 'IMPORT' | 'MANUAL' | 'REPORT_INIT';
    remark?: string;
  }

  export interface ConfirmReqVO {
    ids: number[];
    confirmerName?: string;
  }

  export interface ImportRespVO {
    totalRows: number;
    skippedRows: number;
    createCount: number;
    updateCount: number;
    failureCount: number;
    messages: string[];
    failures: string[];
  }

  export interface GrindingPageReqVO extends PageParam {
    completionTimeStart?: string;
    completionTimeEnd?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
    materialCode?: string;
    motherBatchNo?: string;
    batchNo?: string;
    recordRole?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND';
    sourceBizType?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND';
    segmentMark?: 'NONE' | 'P' | 'Q' | 'R' | 'S';
    grindingPass?: string;
    recorderName?: string;
    status?: string;
  }

  export type GrindingExportReqVO = Omit<
    GrindingPageReqVO,
    'pageNo' | 'pageSize'
  >;

  export interface GrindingRecordRow {
    consumption?: GrindingConsumption;
    id?: number;
    batchNo?: string;
    detailId?: number;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    guideClothBatchNo?: string;
    guideClothChanged?: boolean;
    guideClothLife?: number;
    guideClothReplaceReason?: string;
    firstAllocationId?: number;
    inputLength?: number;
    materialCode?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    motherBatchNo?: string;
    outputLength?: number;
    passName?: string;
    passType?: string;
    recorderName?: string;
    remark?: string;
    replaceReason?: string;
    reportDate?: string;
    recordTime?: string;
    recordRole?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND';
    sandpaperBatchNo?: string;
    sandpaperChanged?: boolean;
    sandpaperLife?: number;
    sandpaperLifeDays?: number;
    sandpaperReplaceReason?: string;
    confirmerName?: string;
    confirmTime?: string;
    status?: string;
    sourceType?: 'IMPORT' | 'MANUAL' | 'REPORT_AUTO' | 'REPORT_INIT';
    sourceDetailId?: number;
    sourceBizType?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND';
    sourceSegmentNo?: number;
    /** 手工新增且发生砂纸更换时，两条研发生产记录的归属键。 */
    manualSplitGroupNo?: string;
    segmentMark?: 'NONE' | 'P' | 'Q' | 'R' | 'S';
    startPosition?: number;
  }

  export interface GrindingConsumableDefault {
    sourceRecordId?: number;
    sourceCompletionTime?: string;
    sourceStatus?: string;
    sourceType?: 'IMPORT' | 'MANUAL' | 'REPORT_AUTO' | 'REPORT_INIT';
    sandpaperLife?: number;
    sandpaperLifeDays?: number;
    sandpaperBatchNo?: string;
    guideClothLife?: number;
    guideClothBatchNo?: string;
  }

  export interface GrindingLifeUpdateReqVO {
    id: number;
    sandpaperLife?: number;
    sandpaperLifeDays?: number;
    guideClothLife?: number;
    /** 更换原因快照；传空字符串可清空。 */
    replaceReason?: string;
  }

  export interface GrindingConsumableSyncReqVO {
    equipmentId: number;
  }

  export interface GrindingConsumableSyncRespVO {
    sourceRecordId?: number;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    sourceRecordTime?: string;
    sandpaperLife?: number;
    sandpaperLifeDays?: number;
    guideClothLife?: number;
    synced?: boolean;
    message?: string;
  }

  export interface SlittingPressPageReqVO extends PageParam {
    reportDateStart?: string;
    reportDateEnd?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
    materialCode?: string;
    batchNo?: string;
    recorderName?: string;
    status?: string;
  }

  export interface FormulaPageReqVO extends PageParam {
    reportDateStart?: string;
    reportDateEnd?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
    materialCode?: string;
    batchNo?: string;
    filterBatchNo?: string;
    mixerEquipmentCode?: string;
    batchingTankNo?: string;
    foamingEquipmentCode?: string;
    defoamingTankNo?: string;
    recorderName?: string;
  }

  export type FormulaExportReqVO = Omit<
    FormulaPageReqVO,
    'pageNo' | 'pageSize'
  >;

  export interface FormulaRecordRow {
    id?: number;
    reportDate?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    materialCode?: string;
    batchNo?: string;
    filterBatchNo?: string;
    inputWeight?: number;
    outputWeight?: number;
    mixerEquipmentCode?: string;
    batchingTankNo?: string;
    foamingEquipmentCode?: string;
    defoamingTankNo?: string;
    recorderName?: string;
    recordTime?: string;
    remark?: string;
  }

  export type SlittingPressExportReqVO = Omit<
    SlittingPressPageReqVO,
    'pageNo' | 'pageSize'
  >;

  export interface SlittingPressRecordRow {
    id?: number;
    reportDate?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    materialCode?: string;
    batchNo?: string;
    slittingInputM?: number;
    slittingOutputPcs?: number;
    slittingNgPcs?: number;
    pressSlotActualInputPcs?: number;
    pressSlotActualOutputPcs?: number;
    pressSlotOutputPcs?: number;
    rollerCleanAccumulatedPcs?: number;
    rollerCleanUseDays?: number;
    bearingReplaceAccumulatedPcs?: number;
    bearingReplaceUseDays?: number;
    recordSource?: string;
    recorderName?: string;
    recordTime?: string;
    confirmerName?: string;
    confirmTime?: string;
    status?: string;
    sourceType?: 'IMPORT' | 'MANUAL' | 'REPORT_INIT';
    remark?: string;
  }

  export interface AdhesivePageReqVO extends PageParam {
    reportDateStart?: string;
    reportDateEnd?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | 'UNCLASSIFIED';
    materialCode?: string;
    batchNo?: string;
    recorderName?: string;
  }

  export type AdhesiveExportReqVO = Omit<
    AdhesivePageReqVO,
    'pageNo' | 'pageSize'
  >;

  export interface AdhesiveRecordRow {
    id?: number;
    reportDate?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    materialCode?: string;
    batchNo?: string;
    inputQty?: number;
    outputQty?: number;
    glueBoardMaterialCode?: string;
    glueBoardBatchNo?: string;
    glueBoardConsumeQty?: number;
    recordSource?: string;
    recorderName?: string;
    recordTime?: string;
    remark?: string;
  }
}

export async function getCutRoundProductionRecordPage(
  params: MesHcProductionRecordApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcProductionRecordApi.RecordRow>>(
    '/mes/hc/plan/production-record/page',
    { params },
  );
}

export async function exportCutRoundProductionRecord(
  params: MesHcProductionRecordApi.ExportReqVO,
) {
  return requestClient.download('/mes/hc/plan/production-record/export-excel', {
    params,
  });
}

export async function getGrindingProductionRecordPage(
  params: MesHcProductionRecordApi.GrindingPageReqVO,
) {
  return requestClient.get<
    PageResult<MesHcProductionRecordApi.GrindingRecordRow>
  >('/mes/hc/plan/production-record/rough-grinding/page', { params });
}

export async function exportGrindingProductionRecord(
  params: MesHcProductionRecordApi.GrindingExportReqVO,
) {
  return requestClient.download(
    '/mes/hc/plan/production-record/rough-grinding/export-excel',
    { params },
  );
}

const grindingBaseUrl = '/mes/hc/plan/production-record/rough-grinding';

export async function getGrindingProductionRecord(id: number) {
  return requestClient.get<MesHcProductionRecordApi.GrindingRecordRow>(
    `${grindingBaseUrl}/get`,
    { params: { id } },
  );
}

export async function getGrindingProductionRecordConsumableDefault(params: {
  equipmentId: number;
  completionTime: string;
}) {
  return requestClient.get<MesHcProductionRecordApi.GrindingConsumableDefault>(
    `${grindingBaseUrl}/consumable-default`,
    { params },
  );
}

export async function createGrindingProductionRecord(
  data: MesHcProductionRecordApi.GrindingRecordRow,
) {
  return requestClient.post<number>(`${grindingBaseUrl}/create`, data);
}

export async function updateGrindingProductionRecord(
  data: MesHcProductionRecordApi.GrindingRecordRow,
) {
  return requestClient.put(`${grindingBaseUrl}/update`, data);
}

export async function updateGrindingProductionRecordLife(
  data: MesHcProductionRecordApi.GrindingLifeUpdateReqVO,
) {
  return requestClient.put<boolean>(`${grindingBaseUrl}/life/update`, data);
}

export async function getGrindingProductionRecordConsumableSyncPreview(
  equipmentId: number,
) {
  return requestClient.get<MesHcProductionRecordApi.GrindingConsumableSyncRespVO>(
    `${grindingBaseUrl}/consumable-sync/latest`,
    { params: { equipmentId } },
  );
}

export async function syncGrindingProductionRecordConsumableState(
  data: MesHcProductionRecordApi.GrindingConsumableSyncReqVO,
) {
  return requestClient.post<MesHcProductionRecordApi.GrindingConsumableSyncRespVO>(
    `${grindingBaseUrl}/consumable-sync/latest`,
    data,
  );
}

export async function deleteGrindingProductionRecord(id: number) {
  return requestClient.delete(`${grindingBaseUrl}/delete`, { params: { id } });
}

export async function deleteGrindingProductionRecordList(ids: number[]) {
  return requestClient.delete(`${grindingBaseUrl}/delete-list`, {
    params: { ids },
  });
}

export async function confirmGrindingProductionRecord(
  data: MesHcProductionRecordApi.ConfirmReqVO,
) {
  return requestClient.post<number>(`${grindingBaseUrl}/confirm`, data);
}

export async function importGrindingProductionRecord(file: File) {
  return requestClient.upload<MesHcProductionRecordApi.ImportRespVO>(
    `${grindingBaseUrl}/import`,
    { file },
  );
}

export async function getSlittingPressProductionRecordPage(
  params: MesHcProductionRecordApi.SlittingPressPageReqVO,
) {
  return requestClient.get<
    PageResult<MesHcProductionRecordApi.SlittingPressRecordRow>
  >('/mes/hc/plan/slitting-press-production-record/page', { params });
}

export async function exportSlittingPressProductionRecord(
  params: MesHcProductionRecordApi.SlittingPressExportReqVO,
) {
  return requestClient.download(
    '/mes/hc/plan/slitting-press-production-record/export-excel',
    { params },
  );
}

const formulaProductionRecordBaseUrl =
  '/mes/hc/plan/formula-production-record';

export async function getFormulaProductionRecordPage(
  params: MesHcProductionRecordApi.FormulaPageReqVO,
) {
  return requestClient.get<
    PageResult<MesHcProductionRecordApi.FormulaRecordRow>
  >(`${formulaProductionRecordBaseUrl}/page`, { params });
}

export async function exportFormulaProductionRecord(
  params: MesHcProductionRecordApi.FormulaExportReqVO,
) {
  return requestClient.download(
    `${formulaProductionRecordBaseUrl}/export-excel`,
    { params },
  );
}

const adhesiveProductionRecordBaseUrl =
  '/mes/hc/plan/adhesive-production-record';

export async function getAdhesive1ProductionRecordPage(
  params: MesHcProductionRecordApi.AdhesivePageReqVO,
) {
  return requestClient.get<
    PageResult<MesHcProductionRecordApi.AdhesiveRecordRow>
  >(`${adhesiveProductionRecordBaseUrl}/adhesive1/page`, { params });
}

export async function exportAdhesive1ProductionRecord(
  params: MesHcProductionRecordApi.AdhesiveExportReqVO,
) {
  return requestClient.download(
    `${adhesiveProductionRecordBaseUrl}/adhesive1/export-excel`,
    { params },
  );
}

export async function getAdhesive2ProductionRecordPage(
  params: MesHcProductionRecordApi.AdhesivePageReqVO,
) {
  return requestClient.get<
    PageResult<MesHcProductionRecordApi.AdhesiveRecordRow>
  >(`${adhesiveProductionRecordBaseUrl}/adhesive2/page`, { params });
}

export async function exportAdhesive2ProductionRecord(
  params: MesHcProductionRecordApi.AdhesiveExportReqVO,
) {
  return requestClient.download(
    `${adhesiveProductionRecordBaseUrl}/adhesive2/export-excel`,
    { params },
  );
}
