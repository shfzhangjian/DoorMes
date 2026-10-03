import { requestClient } from '#/api/request';

export namespace MesQmsEnvironmentBoardApi {
  export interface Standard {
    humidityMax: number;
    humidityMin: number;
    id?: number;
    remark?: string;
    temperatureMax: number;
    temperatureMin: number;
    workshopCode: string;
    workshopName: string;
  }

  export interface Summary {
    abnormalDays: number;
    confirmedDays: number;
    pendingConfirmDays: number;
    recordedDays: number;
  }

  export interface Record {
    confirmStatus: 'CONFIRMED' | 'WAIT_CONFIRM' | string;
    confirmTime?: string;
    confirmerId?: number;
    confirmerName?: string;
    confirmerUsername?: string;
    day: number;
    humidityStatus: 'MISSING' | 'NG' | 'OK' | string;
    humidityValue?: number;
    id?: number;
    overallStatus: 'MISSING' | 'NG' | 'OK' | string;
    recordDate: string;
    recordStatus: 'CONFIRMED' | 'WAIT_CONFIRM' | 'WAIT_RECORD' | string;
    recordTime?: string;
    recorderId?: number;
    recorderName?: string;
    recorderUsername?: string;
    remark?: string;
    temperatureStatus: 'MISSING' | 'NG' | 'OK' | string;
    temperatureValue?: number;
  }

  export interface BoardResp {
    dayCount: number;
    recordMonth: string;
    records: Record[];
    standard: Standard;
    summary: Summary;
    workshopCode: string;
    workshopName: string;
  }

  export interface BoardReq {
    recordMonth: string;
    workshopCode: string;
    workshopName?: string;
  }

  export interface StandardSaveReq {
    humidityMax: number;
    humidityMin: number;
    remark?: string;
    temperatureMax: number;
    temperatureMin: number;
    workshopCode: string;
    workshopName?: string;
  }

  export interface RecordSaveReq {
    humidityValue: number;
    recordDate: string;
    recorderId?: number;
    recorderName: string;
    recorderUsername?: string;
    remark?: string;
    temperatureValue: number;
    workshopCode: string;
    workshopName?: string;
  }

  export interface RecordCorrectReq extends RecordSaveReq {
    correctionReason: string;
  }

  export interface RecordConfirmReq {
    confirmerId?: number;
    confirmerName: string;
    confirmerUsername?: string;
    recordDate: string;
    workshopCode: string;
    workshopName?: string;
  }

  export interface ImportResp {
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }

  export interface ImportReq {
    file: File;
    operatorId?: number;
    operatorName: string;
    operatorUsername?: string;
    recordMonth: string;
    workshopCode: string;
    workshopName?: string;
  }
}

const BASE_URL = '/mes/quality/environment-board';

export function getEnvironmentBoard(params: MesQmsEnvironmentBoardApi.BoardReq) {
  return requestClient.get<MesQmsEnvironmentBoardApi.BoardResp>(
    `${BASE_URL}/board`,
    { params },
  );
}

export function saveEnvironmentStandard(
  data: MesQmsEnvironmentBoardApi.StandardSaveReq,
) {
  return requestClient.post<MesQmsEnvironmentBoardApi.Standard>(
    `${BASE_URL}/standard/save`,
    data,
  );
}

export function saveEnvironmentRecord(
  data: MesQmsEnvironmentBoardApi.RecordSaveReq,
) {
  return requestClient.post<MesQmsEnvironmentBoardApi.Record>(
    `${BASE_URL}/record/save`,
    data,
  );
}

export function correctEnvironmentRecord(
  data: MesQmsEnvironmentBoardApi.RecordCorrectReq,
) {
  return requestClient.post<MesQmsEnvironmentBoardApi.Record>(
    `${BASE_URL}/record/correct`,
    data,
  );
}

export function confirmEnvironmentRecord(
  data: MesQmsEnvironmentBoardApi.RecordConfirmReq,
) {
  return requestClient.post<MesQmsEnvironmentBoardApi.Record>(
    `${BASE_URL}/record/confirm`,
    data,
  );
}

export function exportEnvironmentRecords(
  params: MesQmsEnvironmentBoardApi.BoardReq,
) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}

export function importEnvironmentRecords(
  data: MesQmsEnvironmentBoardApi.ImportReq,
) {
  return requestClient.upload<MesQmsEnvironmentBoardApi.ImportResp>(
    `${BASE_URL}/import-excel`,
    data,
  );
}
