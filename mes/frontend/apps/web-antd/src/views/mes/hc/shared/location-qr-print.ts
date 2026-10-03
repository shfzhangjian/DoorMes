export interface LocationQrPrintField {
  label: string;
  value?: unknown;
}

export interface LocationQrPrintItem {
  fields: LocationQrPrintField[];
  /** 二维码内容：不可变的系统库位唯一键。 */
  locationKey: string;
  /** 标签上供现场识读的展示编码。 */
  displayLocationCode?: string;
  processName: string;
  title?: string;
}

const LOCATION_PRINT_AGENT_URL = 'http://127.0.0.1:17820';

function normalizeLocationValue(value?: unknown) {
  return String(value ?? '').trim();
}

function buildPrintItem(item: LocationQrPrintItem) {
  const locationKey = normalizeLocationValue(item.locationKey);
  const displayLocationCode =
    normalizeLocationValue(item.displayLocationCode) || locationKey;
  return {
    fields: item.fields,
    planNo: locationKey,
    processName: item.processName,
    productionBatchNo: locationKey,
    qrBottomText: displayLocationCode,
    qrTopText: displayLocationCode,
    qrValue: locationKey,
    ticketId: locationKey,
    ...(item.title ? { title: item.title } : {}),
  };
}

export function buildLocationQrPrintPayload(
  items: LocationQrPrintItem[],
  title: string,
) {
  const printItems = items
    .filter((item) => normalizeLocationValue(item.locationKey))
    .map((item) => buildPrintItem(item));
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'default',
    printMode: 'raw',
    rawProtocol: 'pplb',
    title,
    waitForSpooler: true,
  };
  if (printItems.length === 1) return { ...commonPayload, ...printItems[0] };
  return { ...commonPayload, items: printItems };
}

export async function printLocationQrLabels(
  items: LocationQrPrintItem[],
  title: string,
) {
  const validItems = items.filter((item) =>
    normalizeLocationValue(item.locationKey),
  );
  if (validItems.length === 0) {
    throw new Error('请先选择需要打印二维码的库位');
  }
  const endpoint =
    validItems.length > 1
      ? '/print/transfer-tickets'
      : '/print/transfer-ticket';
  const response = await fetch(`${LOCATION_PRINT_AGENT_URL}${endpoint}`, {
    body: JSON.stringify(buildLocationQrPrintPayload(validItems, title)),
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return result;
}
