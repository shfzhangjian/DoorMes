import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesIpqcApi {
  export type Judgment = 'OK' | 'NG' | 'PENDING' | '-';

  export interface IpqcSample {
    id?: number;
    sampleSeq: number;
    samplePosition?: string;
    measuredValue?: number;
    qualitativeValue?: 'OK' | 'NG';
    sampleResult?: Judgment;
    defectCode?: string;
    defectName?: string;
    remark?: string;
  }

  export interface IpqcItem {
    id?: number;
    standardItemId?: number;
    category: 'PRODUCT' | 'PROCESS';
    inspectionItem: string;
    itemType: 'QUANTITATIVE' | 'QUALITATIVE';
    targetValue?: number;
    standardDesc: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    testTool?: string;
    sampleSize: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    sampleValues: any[];
    maxValue?: number;
    minValue?: number;
    averageValue?: number;
    itemResult: Judgment;
    isSpc?: boolean;
    sort?: number;
    samples?: IpqcSample[];
  }

  export interface IpqcAbnormal {
    id?: number;
    ipqcItemId?: number;
    sampleId?: number;
    defectCode?: string;
    defectName?: string;
    abnormalDesc: string;
    processStatus?: string;
    actionRequired?: string;
    ncRecordId?: number;
  }

  export interface IpqcRecord {
    id?: number;
    ipqcNo: string;
    workOrderNo: string;
    planOrderId?: number;
    operationCode?: string;
    operationName?: string;
    machineId?: number;
    machineCode: string;
    machineName?: string;
    materialId?: number;
    materialCode: string;
    materialName: string;
    specification: string;
    inspectionType: 'ROUTINE' | 'ABNORMAL_RECHECK' | 'CHANGEOVER_RECHECK';
    status?: 'PENDING' | 'INSPECTING' | 'SUSPENDED' | 'COMPLETED' | 'ABNORMAL' | 'CANCELED';
    scheduledTime?: string;
    nextInspectionTime?: string | number;
    lastInspectStatus?: 'OK' | 'NG' | 'NONE';
    inspector?: string;
    inspectorId?: number;
    inspectorName?: string;
    inspectionTime?: string;
    controlAction?: 'REPORT_ONLY' | 'PAUSE_MACHINE';
    judgment: Judgment;
    remark?: string;
    items?: IpqcItem[];
    abnormals?: IpqcAbnormal[];
  }

  export interface IpqcPageReq extends PageParam {
    ipqcNo?: string;
    workOrderNo?: string;
    machineCode?: string;
    materialCode?: string;
    inspectionType?: string;
    status?: string;
    judgment?: string;
    inspectionTime?: string[];
  }

  export interface ActiveMachine extends IpqcRecord {
    nextInspectTime: number;
  }

  export interface IpqcStandard {
    standardId: number;
    standardNo: string;
    standardName: string;
    version: string;
    applyType: string;
    items: Array<{
      standardItemId?: number;
      category?: 'PRODUCT' | 'PROCESS';
      inspectionItem: string;
      itemType: 'QUANTITATIVE' | 'QUALITATIVE';
      targetValue?: number;
      standardDesc: string;
      inspectionMethod?: string;
      testFrequencyJudgement?: string;
      testTool?: string;
      sampleSize: number;
      minValueLimit?: number;
      maxValueLimit?: number;
      isSpc?: boolean;
      sort?: number;
    }>;
  }
}

function toUiJudgment(value?: string): MesIpqcApi.Judgment {
  return !value || value === 'PENDING' ? '-' : (value as MesIpqcApi.Judgment);
}

function toApiJudgment(value?: MesIpqcApi.Judgment) {
  return !value || value === '-' ? 'PENDING' : value;
}

function buildValues(item: any) {
  const samples = (item.samples || []).sort((a: MesIpqcApi.IpqcSample, b: MesIpqcApi.IpqcSample) => a.sampleSeq - b.sampleSeq);
  if (samples.length === 0) return new Array(item.sampleSize || 1).fill(undefined);
  return samples.map((sample: MesIpqcApi.IpqcSample) =>
    item.itemType === 'QUANTITATIVE' ? sample.measuredValue : sample.qualitativeValue,
  );
}

function normalizeItem(item: any): MesIpqcApi.IpqcItem {
  return {
    ...item,
    category: item.category || 'PRODUCT',
    itemResult: toUiJudgment(item.itemResult),
    sampleValues: item.sampleValues || buildValues(item),
  };
}

function normalizeRecord(record: any): MesIpqcApi.IpqcRecord {
  const nextInspectTime = record.nextInspectTime || (record.nextInspectionTime ? new Date(record.nextInspectionTime).getTime() : Date.now());
  return {
    ...record,
    inspector: record.inspectorName || record.inspector,
    judgment: toUiJudgment(record.judgment),
    lastInspectStatus: record.judgment === 'OK' || record.judgment === 'NG' ? record.judgment : 'NONE',
    nextInspectTime,
    items: (record.items || []).map(normalizeItem),
  };
}

function buildSamples(item: MesIpqcApi.IpqcItem): MesIpqcApi.IpqcSample[] {
  return (item.sampleValues || []).map((value, index) => ({
    sampleSeq: index + 1,
    measuredValue: item.itemType === 'QUANTITATIVE' && value !== undefined && value !== '' ? Number(value) : undefined,
    qualitativeValue: item.itemType === 'QUALITATIVE' ? value : undefined,
    sampleResult: item.itemType === 'QUALITATIVE' ? toApiJudgment(value) : undefined,
  }));
}

function toSubmitPayload(data: MesIpqcApi.IpqcRecord) {
  return {
    id: data.id,
    controlAction: data.controlAction,
    remark: data.remark,
    items: (data.items || []).map((item) => ({
      id: item.id,
      standardItemId: item.standardItemId,
      category: item.category,
      inspectionItem: item.inspectionItem,
      itemType: item.itemType,
      targetValue: item.targetValue,
      standardDesc: item.standardDesc,
      inspectionMethod: item.inspectionMethod,
      testFrequencyJudgement: item.testFrequencyJudgement,
      testTool: item.testTool,
      sampleSize: item.sampleSize,
      minValueLimit: item.minValueLimit,
      maxValueLimit: item.maxValueLimit,
      itemResult: toApiJudgment(item.itemResult),
      isSpc: item.isSpc,
      sort: item.sort,
      samples: buildSamples(item),
    })),
    abnormals: data.abnormals,
  };
}

export async function getActiveMachines() {
  const list = await requestClient.get<MesIpqcApi.IpqcRecord[]>('/mes/quality/ipqc/pending-list');
  return (list || []).map(normalizeRecord) as MesIpqcApi.ActiveMachine[];
}

export async function getStandardByMachine(
  machineCode: string,
  materialCode: string,
  operationCode?: string,
  operationName?: string,
  existingItems?: MesIpqcApi.IpqcItem[],
) {
  if (existingItems && existingItems.length > 0) return existingItems.map(normalizeItem);
  const standard = await requestClient.get<MesIpqcApi.IpqcStandard>('/mes/quality/ipqc/standard-by-machine', {
    params: { machineCode, materialCode, operationCode, operationName },
  });
  return (standard.items || []).map((item) => ({
    ...item,
    category: item.category || 'PRODUCT',
    itemResult: '-',
    sampleValues: new Array(item.sampleSize || 1).fill(undefined),
  }));
}

export async function getIpqcPage(params: MesIpqcApi.IpqcPageReq) {
  const page = await requestClient.get<PageResult<MesIpqcApi.IpqcRecord>>('/mes/quality/ipqc/page', { params });
  return { ...page, list: (page.list || []).map(normalizeRecord) };
}

export async function getIpqcDetail(id: number) {
  const record = await requestClient.get<MesIpqcApi.IpqcRecord>(`/mes/quality/ipqc/get?id=${id}`);
  return normalizeRecord(record);
}

export async function createIpqcRecord(data: MesIpqcApi.IpqcRecord) {
  const payload = { ...data };
  delete payload.items;
  delete payload.abnormals;
  return requestClient.post<number>('/mes/quality/ipqc/create', payload);
}

export async function suspendIpqcRecord(id: number) {
  return requestClient.put<boolean>(`/mes/quality/ipqc/suspend?id=${id}`);
}

export async function submitIpqcRecord(data: MesIpqcApi.IpqcRecord) {
  if (!data.id) {
    data.id = await createIpqcRecord(data);
  }
  return requestClient.put<boolean>('/mes/quality/ipqc/submit', toSubmitPayload(data));
}
