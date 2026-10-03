import { applyPrintFieldTemplate } from './printFieldTemplate';
import { buildTransferTicketQrValue } from './workOrderTicketPrint';

export interface SlittingTransferTicketContext {
  /** 无料号时隐藏标签中的料号行，仅供 NG 标签使用。 */
  hideMaterialCodeWhenEmpty?: boolean;
  materialCode?: string;
  modelCode?: string;
  planNo?: string;
  processName?: string;
  recorderName?: string;
  segmentBatchNo?: string;
  sliceSerialNo?: string;
  ticketId: number | string;
  workTime?: string;
}

async function buildSlittingTransferTicketItem(
  row: SlittingTransferTicketContext,
  now: string,
) {
  const resolvedMaterialCode = row.materialCode?.trim();
  const materialCode = resolvedMaterialCode || '-';
  const modelCode = row.modelCode || '-';
  const segmentBatchNo = row.segmentBatchNo || '-';
  const sliceSerialNo = row.sliceSerialNo || '-';
  const planNo = row.planNo;
  const processName = row.processName?.trim() || '分切';
  const workTime = row.workTime || now;
  const recorderName = row.recorderName || '-';
  const fallbackFields = [
    { label: '料号', value: materialCode },
    { label: '型号', value: modelCode },
    { label: '分段批号', value: segmentBatchNo },
    { label: '片号', value: sliceSerialNo },
    { label: '当前工序', value: processName },
    { label: '报工时间', value: workTime },
    { label: '记录人', value: recorderName },
  ];
  const fields = await applyPrintFieldTemplate(
    'SLITTING_TRANSFER',
    fallbackFields,
    {
      endTime: workTime,
      materialCode,
      modelCode,
      planNo,
      processName,
      recorderName,
      segmentBatchNo,
      sliceSerialNo,
      startTime: workTime,
    },
  );
  const visibleFields =
    !resolvedMaterialCode && row.hideMaterialCodeWhenEmpty
      ? fields.filter(
          (field) => field.fieldKey !== 'materialCode' && field.label !== '料号',
        )
      : fields;
  return {
    endTime: workTime,
    fields: visibleFields,
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo: sliceSerialNo,
    qrBottomText: sliceSerialNo,
    qrTopText: planNo,
    qrValue: buildTransferTicketQrValue(planNo, sliceSerialNo),
    recorderName,
    startTime: workTime,
    ticketId: row.ticketId,
  };
}

export async function buildSlittingTransferTicketPayload(
  rows: SlittingTransferTicketContext[],
  now: string,
) {
  const items = await Promise.all(
    rows.map((row) => buildSlittingTransferTicketItem(row, now)),
  );
  const commonPayload = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printerKey: 'slitting',
    printMode: 'raw',
    rawProtocol: 'pplb',
    title: '工艺流转单',
    waitForSpooler: true,
  };
  if (items.length === 1) {
    return { ...commonPayload, ...items[0] };
  }
  return { ...commonPayload, items };
}
