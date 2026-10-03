const DEFAULT_WET_LOCAL_COLLECTOR_URL = 'http://127.0.0.1:17830';

export type WetLocalCollectorQuality =
  | 'ALARM'
  | 'MISSING'
  | 'NG'
  | 'OK'
  | 'STALE'
  | string;

export interface WetLocalCollectorFillSource {
  ageSeconds?: number;
  channelKey?: string;
  channelName?: string;
  collectedAt?: string;
  collectorId?: string;
  displayName?: string;
  equipmentCode?: string;
  equipmentName?: string;
  formattedValue?: string;
  maxAgeSeconds?: number;
  measurementKey?: string;
  metricName?: string;
  quality?: WetLocalCollectorQuality;
  rawValue?: number | string;
  resultFlag?: string;
  unit?: string;
  [key: string]: any;
}

export interface WetLocalCollectorFillItem {
  actualValue?: number | string;
  category?: string;
  iotSource?: WetLocalCollectorFillSource;
  item?: string;
  itemSeq: number | string;
  node?: string;
  remark?: string;
  standard?: string;
  status?: string;
  valueMode?: string;
}

export interface WetLocalCollectorReadForFillReq {
  batchNo?: string;
  equipmentCode?: string;
  equipmentId?: number | string;
  equipmentName?: string;
  eventType?: string;
  formCode: string;
  itemCategory?: string;
  modelCode?: string;
  planId?: number | string;
  planOperationId?: number | string;
  readFresh?: boolean;
  recordId?: number | string;
  stepNode?: string;
}

export interface WetLocalCollectorFillResult {
  candidates?: any[];
  collectedAt?: string;
  collectorId?: string;
  equipmentCode?: string;
  eventType?: string;
  fillItems?: WetLocalCollectorFillItem[];
  formCode?: string;
  itemCategory?: string;
  message?: string;
  stepNode?: string;
  stale?: boolean;
  success?: boolean;
  [key: string]: any;
}

function normalizeCollectorUrl(baseUrl = DEFAULT_WET_LOCAL_COLLECTOR_URL) {
  return baseUrl.replace(/\/+$/, '');
}

function resolveCollectorErrorMessage(result: any, status?: number) {
  return (
    result?.fill?.message ||
    result?.message ||
    result?.error ||
    (status ? `本机采集服务返回异常：${status}` : '本机采集服务返回异常')
  );
}

export async function readWetProcessCheckFromLocalCollector(
  body: WetLocalCollectorReadForFillReq,
  baseUrl = DEFAULT_WET_LOCAL_COLLECTOR_URL,
): Promise<WetLocalCollectorFillResult> {
  if (!body.formCode) {
    throw new Error('当前点检表缺少表单编码，无法匹配本机采集映射。');
  }

  let response: Response;
  try {
    response = await fetch(
      `${normalizeCollectorUrl(baseUrl)}/api/report/read-for-fill`,
      {
        body: JSON.stringify({
          eventType: 'MANUAL_FILL',
          readFresh: true,
          ...body,
        }),
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
      },
    );
  } catch (error: any) {
    throw new Error(
      `无法连接本机采集服务，请确认 HC-MES-ModbusConcentrationMonitor.exe 已启动并监听 ${normalizeCollectorUrl(baseUrl)}。${error?.message ? ` ${error.message}` : ''}`,
    );
  }

  let result: any = {};
  try {
    result = await response.json();
  } catch {
    result = {};
  }

  if (!response.ok) {
    throw new Error(resolveCollectorErrorMessage(result, response.status));
  }

  const fill = result?.fill || result;
  if (result?.success === false || fill?.success === false) {
    throw new Error(resolveCollectorErrorMessage(result));
  }

  if (!fill || !Array.isArray(fill.fillItems)) {
    throw new Error('本机采集服务未返回可填充的点检项，请检查当前表单映射配置。');
  }

  return fill as WetLocalCollectorFillResult;
}
