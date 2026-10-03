import dayjs from 'dayjs';

import formulaPrintLogo from '#/assets/mes/formula-print-logo.png';

const WORK_ORDER_PRINT_DOC_CONFIG = {
  department: '材料研发中心',
  docCode: 'HC/R-21-0XX',
  publishDate: '2023.11.1',
  retention: '10年',
  title: 'CMP软垫制造工单',
  version: 'A/0',
};

const WORK_ORDER_PROCESS_CONFIG: Record<string, { metricLabel?: string }> = {
  分切: { metricLabel: '合格数量(片)' },
  压槽: { metricLabel: '合格数量(片)' },
  成品入库: { metricLabel: '入库数量(片)' },
  湿法: { metricLabel: '收卷米数(m)' },
  磨皮: { metricLabel: '磨皮米数(m)' },
  粘胶1: { metricLabel: '生产米数(m)' },
  粘胶2: { metricLabel: '合格数量(片)' },
  裁切: { metricLabel: '合格片数(片)' },
  配料: {},
};

export interface WorkOrderTicketOperation {
  id?: number | string;
  latestConfirmerName?: string;
  latestEndTime?: string;
  latestRecorderName?: string;
  latestRemark?: string;
  latestReportDate?: string;
  latestStartTime?: string;
  name?: string;
  operationName?: string;
  opName?: string;
  processName?: string;
  reportQtyByDate?: Record<string, number>;
}

export interface WorkOrderTicketRow {
  confirmer?: string;
  currentSection: string;
  endTime?: string;
  materialCode?: string;
  metricValue?: number;
  modelCode?: string;
  parentBatchNo?: string;
  planNo?: string;
  productionBatchNo?: string;
  qrDataUrl?: string;
  recorder?: string;
  remark?: string;
  segmentMark?: string;
  startTime?: string;
}

export interface WorkOrderTicketContext {
  materialCode?: string;
  modelCode?: string;
  operations?: WorkOrderTicketOperation[];
  planNo?: string;
  printTime?: string;
}

export interface TransferTicketField {
  label: string;
  value?: unknown;
}

export interface InspectionTransferTicketContext {
  applicantName?: unknown;
  applyTime?: unknown;
  boardBatchNo?: unknown;
  copies?: number;
  extraFields?: TransferTicketField[];
  fields?: TransferTicketField[];
  inspectionNo?: unknown;
  inspectionTransferNo?: unknown;
  inspectionType?: unknown;
  labelGapDots?: number;
  labelGapMm?: number;
  labelHeightMm?: number;
  labelWidthMm?: number;
  materialCode?: unknown;
  modelCode?: unknown;
  offsetXmm?: number;
  offsetYmm?: number;
  planNo?: unknown;
  printerKey?: string;
  processName?: unknown;
  productionBatchNo?: unknown;
  rawProtocol?: string;
  sampleType?: unknown;
  title?: string;
  waitForSpooler?: boolean;
}

export interface ThermalWorkOrderTransferTicketContext {
  endTime?: string;
  fields?: TransferTicketField[];
  materialCode?: string;
  modelCode?: string;
  offsetXmm?: number;
  offsetYmm?: number;
  planNo?: string;
  processName?: string;
  productionBatchNo?: string;
  qrDataUrl?: string;
  qrBottomText?: string;
  qrTopText?: string;
  qrValue?: string;
  recorderName?: string;
  startTime?: string;
  title?: string;
}

interface TransferTicketFieldsContext {
  endTime?: string;
  extraFields?: TransferTicketField[];
  includePlanNo?: boolean;
  materialCode?: string;
  modelCode?: string;
  planNo?: string;
  processName?: string;
  productionBatchNo?: string;
  recorderName?: string;
  startTime?: string;
}

function normalizeTransferTicketValue(value?: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

const LOCAL_TRANSFER_TICKET_PRINT_AGENT_URL = 'http://127.0.0.1:17820';

export async function sendTransferTicketToPrintAgent(ticketPayload: any, baseUrl = LOCAL_TRANSFER_TICKET_PRINT_AGENT_URL) {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/print/transfer-ticket`, {
    body: JSON.stringify(ticketPayload),
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success || !result?.accepted) {
    throw new Error(result?.message || `本机打印服务返回异常：${response.status}`);
  }
  return result;
}

export function formatTransferTicketMetric(value?: unknown, unit = '') {
  const text = normalizeTransferTicketValue(value);
  if (text === '-') return text;
  return unit ? `${text} ${unit}` : text;
}

export function buildTransferTicketQrValue(planNo?: unknown, batchNo?: unknown) {
  const planText = normalizeTransferTicketValue(planNo);
  const batchText = normalizeTransferTicketValue(batchNo);
  if (planText === '-' && batchText === '-') return '';
  if (planText === '-') return batchText;
  if (batchText === '-') return planText;
  return `${planText};${batchText}`;
}

export function resolveTransferTicketQrBusinessNo(value?: unknown) {
  const text = String(value ?? '').trim();
  const separatorIndex = text.indexOf(';');
  if (separatorIndex < 0) return text;
  return text.slice(separatorIndex + 1).trim() || text;
}

export function buildTransferTicketFields(context: TransferTicketFieldsContext): TransferTicketField[] {
  const fields: TransferTicketField[] = [
    { label: '料号', value: normalizeTransferTicketValue(context.materialCode) },
    { label: '型号', value: normalizeTransferTicketValue(context.modelCode) },
    { label: '生产批次', value: normalizeTransferTicketValue(context.productionBatchNo) },
    { label: '当前工序', value: normalizeTransferTicketValue(context.processName) },
    { label: '开工时间', value: normalizeTransferTicketValue(context.startTime) },
    { label: '完工时间', value: normalizeTransferTicketValue(context.endTime) },
  ];
  if (context.includePlanNo !== false) {
    fields.unshift({ label: '计划号', value: normalizeTransferTicketValue(context.planNo) });
  }
  if (context.recorderName !== undefined) {
    fields.push({ label: '记录人', value: normalizeTransferTicketValue(context.recorderName) });
  }
  return fields.concat((context.extraFields || []).filter((field) => String(field.label || '').trim()));
}

export function resolveInspectionTransferTicketNo(context: InspectionTransferTicketContext) {
  return normalizeTransferTicketValue(
    context.inspectionTransferNo || context.inspectionNo || context.productionBatchNo || context.planNo,
  );
}

export function buildInspectionTransferTicketFields(context: InspectionTransferTicketContext): TransferTicketField[] {
  const fields: TransferTicketField[] = [
    { label: '型号', value: normalizeTransferTicketValue(context.modelCode) },
    { label: '产品批次', value: normalizeTransferTicketValue(context.productionBatchNo) },
    { label: '当前工序', value: normalizeTransferTicketValue(context.processName) },
    { label: '送样类型', value: normalizeTransferTicketValue(context.sampleType) },
    { label: '送检时间', value: normalizeTransferTicketValue(context.applyTime) },
    { label: '送检人员', value: normalizeTransferTicketValue(context.applicantName) },
  ];
  if (String(context.inspectionType ?? '').trim()) {
    fields.splice(4, 0, { label: '送检类型', value: normalizeTransferTicketValue(context.inspectionType) });
  }
  if (String(context.boardBatchNo ?? '').trim()) {
    fields.push({ label: '胶板批次', value: normalizeTransferTicketValue(context.boardBatchNo) });
  }
  return fields.concat((context.extraFields || []).filter((field) => String(field.label || '').trim()));
}

export function buildInspectionTransferTicketPayload(context: InspectionTransferTicketContext) {
  const transferNo = resolveInspectionTransferTicketNo(context);
  const qrValue = transferNo === '-' ? '' : transferNo;
  return {
    copies: context.copies ?? 1,
    fields: context.fields?.length ? context.fields : buildInspectionTransferTicketFields(context),
    inspectionNo: normalizeTransferTicketValue(context.inspectionNo),
    labelGapDots: context.labelGapDots ?? 24,
    labelGapMm: context.labelGapMm ?? 3,
    labelHeightMm: context.labelHeightMm ?? 50,
    labelWidthMm: context.labelWidthMm ?? 80,
    materialCode: normalizeTransferTicketValue(context.materialCode),
    modelCode: normalizeTransferTicketValue(context.modelCode),
    offsetXmm: context.offsetXmm ?? 0,
    offsetYmm: context.offsetYmm ?? 0,
    planNo: normalizeTransferTicketValue(context.planNo),
    printerKey: context.printerKey || 'inspection',
    printMode: 'raw',
    processName: normalizeTransferTicketValue(context.processName),
    productionBatchNo: normalizeTransferTicketValue(context.productionBatchNo),
    qrTopText: qrValue || normalizeTransferTicketValue(context.planNo),
    qrValue,
    rawProtocol: context.rawProtocol || 'pplb',
    ticketType: 'inspection-transfer',
    title: context.title || '检验流转单',
    transferTicketNo: qrValue,
    waitForSpooler: context.waitForSpooler ?? true,
  };
}

function escapePrintHtml(value?: unknown) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function normalizeDate(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function normalizeDateTime(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function resolveWorkOrderPrintSectionName(name?: string) {
  const value = String(name || '').trim();
  if (!value) return '';
  if (value.includes('配料')) return '配料';
  if (value.includes('湿法')) return '湿法';
  if (value.includes('磨皮') || value.includes('粗磨') || value.includes('精磨')) return '磨皮';
  if (value.includes('粘胶1') || value.includes('粘双面胶')) return '粘胶1';
  if (value.includes('分切')) return '分切';
  if (value.includes('压槽')) return '压槽';
  if (value.includes('粘胶2') || value.includes('背胶')) return '粘胶2';
  if (value.includes('裁切') || value.includes('裁圆')) return '裁切';
  if (value.includes('入库') || value.includes('包装')) return '成品入库';
  return value;
}

const DEFAULT_WORK_ORDER_OPERATIONS: WorkOrderTicketOperation[] = [
  '配料',
  '湿法',
  '磨皮',
  '粘胶1',
  '分切',
  '压槽',
  '粘胶2',
  '裁切',
  '成品入库',
].map((opName, index) => ({ id: `default-${index}`, opName }));

function resolveOperationName(operation?: WorkOrderTicketOperation) {
  return (
    operation?.opName ||
    operation?.operationName ||
    operation?.name ||
    operation?.processName ||
    (operation as any)?.operation_name ||
    ''
  );
}

function buildCompletePrintOperations(operations?: WorkOrderTicketOperation[]) {
  if (!operations?.length) return DEFAULT_WORK_ORDER_OPERATIONS;
  const operationBySection = new Map<string, WorkOrderTicketOperation>();
  operations.forEach((operation) => {
    const section = resolveWorkOrderPrintSectionName(resolveOperationName(operation));
    if (section && !operationBySection.has(section)) {
      operationBySection.set(section, operation);
    }
  });
  const completed = DEFAULT_WORK_ORDER_OPERATIONS.map((operation) => {
    const section = resolveWorkOrderPrintSectionName(resolveOperationName(operation));
    return operationBySection.get(section) || operation;
  });
  const knownSections = new Set(completed.map((operation) => resolveWorkOrderPrintSectionName(resolveOperationName(operation))));
  operations.forEach((operation) => {
    const section = resolveWorkOrderPrintSectionName(resolveOperationName(operation));
    if (section && !knownSections.has(section)) {
      completed.push(operation);
    }
  });
  return completed;
}

function resolveWorkOrderLatestReportDate(operation: WorkOrderTicketOperation) {
  if (operation?.latestReportDate) return String(operation.latestReportDate);
  const qtyByDate = operation?.reportQtyByDate;
  if (!qtyByDate || typeof qtyByDate !== 'object') return '';
  const keys = Object.keys(qtyByDate)
    .filter(Boolean)
    .sort((a, b) => dayjs(b).valueOf() - dayjs(a).valueOf());
  return keys[0] || '';
}

function formatPrintNumber(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '';
  return Number(value).toFixed(3);
}

function buildProcessRows(row: WorkOrderTicketRow, operations: WorkOrderTicketOperation[]) {
  const currentSection = row.currentSection;
  const operationSections = operations.map((operation) =>
    resolveWorkOrderPrintSectionName(resolveOperationName(operation)),
  );
  const currentIndex = operationSections.findIndex((name) => name === currentSection);
  const currentProductionDate = normalizeDate(row.endTime || row.startTime);

  return operations.map((operation, index) => {
    const sectionName = resolveWorkOrderPrintSectionName(resolveOperationName(operation));
    const config = WORK_ORDER_PROCESS_CONFIG[sectionName] || {};
    const isCurrent = sectionName === currentSection;
    const latestReportDate = resolveWorkOrderLatestReportDate(operation);
    return {
      confirmer: isCurrent
        ? row.confirmer || ''
        : currentIndex >= 0 && index < currentIndex
          ? operation?.latestConfirmerName || ''
          : '',
      endTime: isCurrent
        ? normalizeDateTime(row.endTime)
        : currentIndex >= 0 && index < currentIndex
          ? normalizeDateTime(operation?.latestEndTime)
          : '',
      key: operation?.id || `${sectionName}-${index}`,
      metricLabel: config.metricLabel,
      metricValue: isCurrent ? formatPrintNumber(row.metricValue) : '',
      name: sectionName,
      productionDate:
        isCurrent
          ? currentProductionDate
          : currentIndex >= 0 && index < currentIndex
            ? normalizeDate(latestReportDate)
            : '',
      progressed: currentIndex >= 0 && index <= currentIndex,
      recorder: isCurrent
        ? row.recorder || ''
        : currentIndex >= 0 && index < currentIndex
          ? operation?.latestRecorderName || ''
          : '',
      remark: isCurrent ? row.remark || '' : currentIndex >= 0 && index < currentIndex ? operation?.latestRemark || '' : '',
      startTime: isCurrent
        ? normalizeDateTime(row.startTime)
        : currentIndex >= 0 && index < currentIndex
          ? normalizeDateTime(operation?.latestStartTime)
          : '',
    };
  });
}

export function buildWorkOrderTicketHtml(rows: WorkOrderTicketRow[], context: WorkOrderTicketContext) {
  const fallbackSection = rows[0]?.currentSection || '磨皮';
  const operations = buildCompletePrintOperations(context.operations);
  const flowNodes = operations
    .map((operation, index) => ({
      current: resolveWorkOrderPrintSectionName(resolveOperationName(operation)) === fallbackSection,
      key: operation?.id || `${resolveOperationName(operation) || 'op'}-${index}`,
      label: resolveWorkOrderPrintSectionName(resolveOperationName(operation)),
    }))
    .filter((node) => !!node.label);
  const printTime = context.printTime || dayjs().format('YYYY-MM-DD HH:mm:ss');
  const ticketItems = rows
    .map((row) => {
      const processRows = buildProcessRows(row, operations);
      const planNo = row.planNo || context.planNo || '-';
      const materialCode = row.materialCode || context.materialCode || '-';
      const modelCode = row.modelCode || context.modelCode || '-';
      return `
        <section class="print-sheet">
        <div class="print-dom-wrap">
        <div class="print-page" style="--process-line-height: 4.55mm; --section-font-size: 4.6mm; --client-section-font-size: 5.2mm;">
          <div class="sheet-header">
            <div class="sheet-logo"><img src="${formulaPrintLogo}" alt="HECHEN 禾臣" /></div>
            <div class="sheet-title">${WORK_ORDER_PRINT_DOC_CONFIG.title}</div>
            <div class="sheet-meta">
              <div class="sheet-meta__text">
                <div>编号: ${WORK_ORDER_PRINT_DOC_CONFIG.docCode}</div>
                <div>版本版次号: ${WORK_ORDER_PRINT_DOC_CONFIG.version}</div>
                <div>计划号: ${escapePrintHtml(planNo)}</div>
              </div>
              <div class="qr-box">${row.qrDataUrl ? `<img src="${row.qrDataUrl}" alt="分段批次二维码" />` : escapePrintHtml(row.productionBatchNo || planNo)}</div>
            </div>
          </div>
          <table class="sheet-table">
            <tbody>
              <tr>
                <td class="sheet-section sheet-section--client" rowspan="3">客户<br />规范</td>
                <td colspan="2"><span class="sheet-strong">料号：</span>${escapePrintHtml(materialCode)}</td>
                <td colspan="2"><span class="sheet-strong">型号：</span>${escapePrintHtml(modelCode)}</td>
                <td colspan="2"><span class="sheet-strong">生产批号：</span>${escapePrintHtml(row.productionBatchNo || '-')}</td>
              </tr>
              <tr>
                <td colspan="2">
                  <span class="check-box"><span>母工单</span><span class="check-mark"></span></span>
                </td>
                <td colspan="2">
                  <span class="check-box"><span>子工单</span><span class="check-mark">√</span></span>
                </td>
                <td colspan="2"></td>
              </tr>
              <tr>
                <td colspan="6" class="sheet-flow-cell">
                  <span class="sheet-strong">工艺流程：</span>
                  <span class="flow-line">
                    ${flowNodes
                      .map(
                        (node, index) =>
                          `<span class="flow-node${node.current ? ' flow-node--current' : ''}">${escapePrintHtml(node.label)}</span>${index < flowNodes.length - 1 ? '<span class="flow-arrow">→</span>' : ''}`,
                      )
                      .join('')}
                  </span>
                </td>
              </tr>
              ${processRows
                .map(
                  (processRow) => `
                    <tr>
                      <td class="sheet-section${processRow.progressed ? ' sheet-section--progressed' : ''}" rowspan="2">
                        <span class="sheet-section__marker"></span>
                        <span class="sheet-section__label">${escapePrintHtml(processRow.name)}</span>
                      </td>
                      <td colspan="6" class="process-cell process-cell--top">
                        <div class="process-line process-line--top">
                          <div class="process-field"><span class="sheet-strong">生产日期：</span>${escapePrintHtml(processRow.productionDate || '')}</div>
                          <div class="process-field"><span class="sheet-strong">开始时间：</span>${escapePrintHtml(processRow.startTime || '')}</div>
                          <div class="process-field"><span class="sheet-strong">结束时间：</span>${escapePrintHtml(processRow.endTime || '')}</div>
                          <div class="process-field process-field--metric">
                            <span class="sheet-strong">${escapePrintHtml(processRow.metricLabel || '')}</span>${processRow.metricLabel ? `：${escapePrintHtml(processRow.metricValue || '')}` : ''}
                          </div>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="6" class="process-cell process-cell--bottom">
                        <div class="process-line process-line--bottom">
                          <div class="process-field process-field--remark"><span class="sheet-strong">备注：</span>${escapePrintHtml(processRow.remark || '')}</div>
                          <div class="process-field"><span class="sheet-strong">担当：</span>${escapePrintHtml(processRow.recorder || '')}</div>
                          <div class="process-field"><span class="sheet-strong">确认：</span>${escapePrintHtml(processRow.confirmer || '')}</div>
                        </div>
                      </td>
                    </tr>`,
                )
                .join('')}
            </tbody>
          </table>
          <div class="sheet-footer">
            <span>制定/修订部门：${WORK_ORDER_PRINT_DOC_CONFIG.department}</span>
            <span>制定日期：${WORK_ORDER_PRINT_DOC_CONFIG.publishDate}</span>
            <span>修订日期：${normalizeDate(dayjs().format('YYYY-MM-DD'))}</span>
            <span>保管期限：${WORK_ORDER_PRINT_DOC_CONFIG.retention}</span>
            <span>打印时间：${escapePrintHtml(printTime)}</span>
          </div>
        </div>
        </div>
        </section>`;
    })
    .join('');
  return `
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>${escapePrintHtml(WORK_ORDER_PRINT_DOC_CONFIG.title)}</title>
        <style>
          * { box-sizing: border-box; }
          html, body { margin: 0; padding: 0; background: #fff; }
          body { font-family: "Microsoft YaHei", sans-serif; color: #111; }
          @page { size: A4 landscape; margin: 0; }
          .print-sheet {
            width: 297mm;
            height: 210mm;
            padding: 4mm;
            margin: 0 auto;
            overflow: hidden;
            background: #fff;
            page-break-after: always;
          }
          .print-sheet:last-child { page-break-after: auto; }
          .print-dom-wrap { width: 100%; height: 100%; overflow: hidden; }
          .print-page {
            width: 277mm;
            height: 190mm;
            margin: 0 auto;
            padding: 2.5mm 3.5mm 1.5mm;
            background: #fff;
            overflow: hidden;
            color: #111;
            display: flex;
            flex-direction: column;
          }
          .sheet-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1px; min-height: 15mm; }
          .sheet-logo { width: 44mm; display: flex; align-items: center; justify-content: flex-start; }
          .sheet-logo img { width: 100%; height: auto; display: block; }
          .sheet-title { flex: 1; text-align: center; font-size: 7.8mm; font-weight: 700; text-decoration: underline; line-height: 1; }
          .sheet-meta {
            width: 58mm;
            min-height: 12mm;
            padding: 1mm 1.2mm 0.8mm;
            font-size: 3.1mm;
            line-height: 1.15;
            display: flex;
            flex-direction: row;
            gap: 2mm;
            flex-shrink: 0;
            align-items: flex-start;
            justify-content: space-between;
            border: 1px solid #111;
          }
          .sheet-meta__text { display: flex; flex-direction: column; gap: 0.4mm; align-items: flex-start; justify-content: center; white-space: nowrap; }
          .qr-box {
            width: 10.5mm;
            height: 10.5mm;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            flex-shrink: 0;
            margin-right: 0.6mm;
            overflow: hidden;
            word-break: break-all;
            font-size: 2mm;
            line-height: 1.1;
          }
          .qr-box img { width: 100%; height: 100%; display: block; }
          .sheet-table { width: 100%; border-collapse: collapse; border: 1px solid #111; font-size: 3.45mm; table-layout: fixed; flex: 1 1 auto; }
          .sheet-table td, .sheet-table th { border: 1px solid #111; padding: 0.45mm 0.9mm; vertical-align: middle; line-height: 1; }
          .sheet-section { width: 15mm; text-align: center; font-size: var(--section-font-size, 4.3mm); font-weight: 700; position: relative; overflow: visible; padding-left: 3.2mm !important; }
          .sheet-section--client { font-size: var(--client-section-font-size, 5mm); }
          .sheet-section__marker { position: absolute; left: -1px; top: -1px; bottom: -1px; width: 0; border-left: 1.5mm solid #cbd5e1; }
          .sheet-section--progressed .sheet-section__marker { border-left-color: #1d4ed8; }
          .sheet-section__label { display: inline-block; position: relative; z-index: 1; }
          .sheet-strong { font-weight: 700; }
          .sheet-flow-cell { font-size: 3.1mm; line-height: 1.05; min-height: 11mm; }
          .flow-line { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 1.2mm; }
          .flow-node { display: inline-flex; align-items: center; padding: 0 0.8mm; }
          .flow-node--current { font-weight: 800; border: 1px dashed #111; }
          .flow-arrow { font-size: 3.4mm; }
          .check-box { display: inline-flex; align-items: center; gap: 1mm; margin-right: 5mm; font-size: 3.8mm; }
          .check-mark { display: inline-flex; align-items: center; justify-content: center; width: 4mm; height: 4mm; border: 1px solid #111; font-size: 3.5mm; }
          .process-line { display: flex; align-items: center; gap: 2mm; min-height: var(--process-line-height, 4.4mm); }
          .process-cell--top { border-bottom: none !important; }
          .process-cell--bottom { border-top: none !important; }
          .process-line--top, .process-line--bottom { justify-content: space-between; }
          .process-field { flex: 1 1 0; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
          .process-field--remark { flex: 2 1 0; }
          .sheet-footer { display: flex; justify-content: space-between; margin-top: auto; padding-top: 1mm; font-size: 3.2mm; line-height: 1.1; flex-shrink: 0; }
        </style>
      </head>
      <body>${ticketItems}</body>
    </html>`;
}

export function buildThermalWorkOrderTransferTicketHtml(context: ThermalWorkOrderTransferTicketContext) {
  const planNo = context.planNo || '-';
  const materialCode = context.materialCode || '-';
  const modelCode = context.modelCode || '-';
  const productionBatchNo = context.productionBatchNo || '-';
  const processName = context.processName || '配料';
  const startTime = context.startTime || '-';
  const endTime = context.endTime || '-';
  const title = context.title || '工艺流转单';
  const qrValue = context.qrValue || context.productionBatchNo || context.planNo || '';
  const qrBottomText = context.qrBottomText || qrValue || productionBatchNo || planNo;
  const qrTopText = context.qrTopText || context.planNo || '';
  const fields = context.fields?.length
    ? context.fields
    : buildTransferTicketFields({
        endTime,
        materialCode,
        modelCode,
        planNo,
        processName,
        productionBatchNo,
        recorderName: context.recorderName,
        startTime,
      });
  const offsetXmm = Number.isFinite(context.offsetXmm) ? Number(context.offsetXmm) : 0;
  const offsetYmm = Number.isFinite(context.offsetYmm) ? Number(context.offsetYmm) : 0;
  const safeOffsetXmm = Math.max(0, offsetXmm);
  const safeOffsetYmm = Math.max(0, offsetYmm);
  const ticketWidthMm = Math.max(34, 80 - safeOffsetXmm);
  const ticketHeightMm = Math.max(30, 50 - safeOffsetYmm);
  return `<!doctype html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <title>${escapePrintHtml(title)}</title>
        <style>
          * { box-sizing: border-box; }
          html, body { width: 80mm; min-height: 50mm; margin: 0; padding: 0; background: #fff; }
          body {
            color: #111;
            font-family: "Microsoft YaHei", Arial, sans-serif;
          }
          @page { size: 80mm 50mm; margin: 0; }
          .ticket {
            width: ${ticketWidthMm}mm;
            height: ${ticketHeightMm}mm;
            margin-left: ${safeOffsetXmm}mm;
            margin-top: ${safeOffsetYmm}mm;
            padding: 2.2mm;
            display: grid;
            grid-template-columns: minmax(0, 1fr) 25mm;
            grid-template-rows: auto 1fr;
            column-gap: 2mm;
            row-gap: 0.8mm;
            overflow: hidden;
            border: 1px solid #111;
          }
          .title {
            grid-column: 1;
            padding-bottom: 1mm;
            border-bottom: 1px solid #111;
            font-size: 4.6mm;
            font-weight: 800;
            letter-spacing: 0;
            line-height: 1;
            text-align: center;
          }
          .info {
            grid-column: 1;
            display: flex;
            flex-direction: column;
            gap: 0.08mm;
          }
          .row {
            display: grid;
            grid-template-columns: 12.6mm 1fr;
            gap: 0.8mm;
            align-items: center;
            min-height: 3.25mm;
            border-bottom: 1px solid #d1d5db;
            font-size: 2.05mm;
            line-height: 1.05;
          }
          .label { font-weight: 800; white-space: nowrap; }
          .value {
            min-width: 0;
            overflow-wrap: anywhere;
            word-break: break-all;
            font-weight: 700;
          }
          .qr-wrap {
            grid-column: 2;
            grid-row: 1 / span 2;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 0.45mm;
            border-left: 1px solid #111;
            padding-left: 1.4mm;
          }
          .qr-plan {
            max-width: 22mm;
            overflow-wrap: anywhere;
            word-break: break-all;
            font-size: 2.05mm;
            font-weight: 800;
            line-height: 1.12;
            text-align: center;
          }
          .qr {
            width: 19.5mm;
            height: 19.5mm;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 1px solid #111;
            padding: 1mm;
            font-size: 2.4mm;
            line-height: 1.1;
            position: relative;
            text-align: center;
            word-break: break-all;
          }
          .qr img { width: 100%; height: 100%; display: block; }
          .qr-logo {
            position: absolute;
            left: 50%;
            top: 50%;
            width: 5.5mm !important;
            height: 5.5mm !important;
            object-fit: cover;
            object-position: left center;
            padding: 0.5mm;
            border-radius: 0.6mm;
            background: #fff;
            transform: translate(-50%, -50%);
          }
          .qr-text {
            max-width: 22mm;
            overflow-wrap: anywhere;
            word-break: break-all;
            font-size: 1.85mm;
            font-weight: 800;
            line-height: 1.15;
            text-align: center;
          }
          @media print {
            html, body { width: 80mm; height: 50mm; overflow: hidden; }
            .ticket { border-color: #111; page-break-after: avoid; }
          }
        </style>
      </head>
      <body>
        <section class="ticket">
          <div class="title">${escapePrintHtml(title)}</div>
          <div class="info">
            ${fields
              .map(
                (field) =>
                  `<div class="row"><div class="label">${escapePrintHtml(field.label)}</div><div class="value">${escapePrintHtml(field.value)}</div></div>`,
              )
              .join('')}
          </div>
          <div class="qr-wrap">
            <div class="qr-plan">${escapePrintHtml(qrTopText)}</div>
            <div class="qr">
              ${context.qrDataUrl ? `<img src="${escapePrintHtml(context.qrDataUrl)}" alt="流转单二维码" /><img class="qr-logo" src="${formulaPrintLogo}" alt="HECHEN" />` : escapePrintHtml(qrValue)}
            </div>
            <div class="qr-text">${escapePrintHtml(qrBottomText)}</div>
          </div>
        </section>
      </body>
    </html>`;
}
