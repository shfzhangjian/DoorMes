import {
  applyPrintFieldTemplate,
  getActivePrintFieldTemplateCached,
} from './printFieldTemplate';

export const PACKAGING_FROZEN_PIECE_LABEL_TEMPLATE_CODE = 'PACKAGING_FROZEN_PIECE_LABEL';
export const PACKAGING_PIECE_LABEL_TEMPLATE_CODE = 'PACKAGING_PIECE_LABEL';
export const PACKAGING_NG_PIECE_LABEL_TEMPLATE_CODE =
  'PACKAGING_NG_PIECE_LABEL';

export interface PackagingPieceLabelContext {
  expiryDate?: string;
  modelCode?: string;
  segmentBatchNo?: string;
  sliceBatchNo?: string;
  ticketId: string;
}

export interface PackagingPieceLabelPrintOptions {
  templateCode?: string;
}

/** 根据包装片的最终质量状态选择标签模板。 */
export function resolvePackagingPieceLabelTemplateCode(qualityStatus?: string) {
  const normalized = String(qualityStatus || '')
    .trim()
    .toUpperCase();
  if (normalized === 'OK') return PACKAGING_PIECE_LABEL_TEMPLATE_CODE;
  if (normalized === 'FROZEN') return PACKAGING_FROZEN_PIECE_LABEL_TEMPLATE_CODE;
  if (normalized === 'NG') return PACKAGING_NG_PIECE_LABEL_TEMPLATE_CODE;
  throw new Error('片号质量状态异常，请刷新后重试');
}

function resolvePackagingPieceLabelFallbackTitle(templateCode: string) {
  if (templateCode === PACKAGING_FROZEN_PIECE_LABEL_TEMPLATE_CODE) return '冻结品入库单';
  return templateCode === PACKAGING_NG_PIECE_LABEL_TEMPLATE_CODE
    ? '不合格品入库单'
    : '成品入库单';
}

/**
 * 成品包装片号标签。
 *
 * 右侧二维码及其上方文本均使用片号；下方文本使用本次打印时刻。
 * printTime 仅为标签快照，不关联包装、入库等业务时间。
 */
export async function buildPackagingPieceLabelPayload(
  rows: PackagingPieceLabelContext[],
  printTime: string,
  options: PackagingPieceLabelPrintOptions = {},
) {
  const templateCode =
    options.templateCode || PACKAGING_PIECE_LABEL_TEMPLATE_CODE;
  const fallbackTitle = resolvePackagingPieceLabelFallbackTitle(templateCode);
  const template = await getActivePrintFieldTemplateCached(templateCode);
  const title = templateCode === PACKAGING_FROZEN_PIECE_LABEL_TEMPLATE_CODE
    ? '冻结品入库单' : String(template?.documentName || '').trim() || fallbackTitle;
  const items = await Promise.all(
    rows.map(async (row) => {
      const modelCode = row.modelCode || '-';
      const segmentBatchNo = row.segmentBatchNo || '-';
      const sliceBatchNo = row.sliceBatchNo || '-';
      const expiryDate = row.expiryDate || '-';
      const fallbackFields = [
        { fieldKey: 'modelCode', label: '型号', value: modelCode },
        {
          fieldKey: 'segmentBatchNo',
          label: '分段批号',
          value: segmentBatchNo,
        },
        { fieldKey: 'sliceBatchNo', label: '片号', value: sliceBatchNo },
        { fieldKey: 'expiryDate', label: '有效期', value: expiryDate },
      ];
      const fields = await applyPrintFieldTemplate(
        templateCode,
        fallbackFields,
        {
          expiryDate,
          modelCode,
          printTime,
          segmentBatchNo,
          sliceBatchNo,
        },
      );

      return {
        fields,
        modelCode,
        processName: '成品包装',
        qrBottomText: printTime,
        qrTopText: sliceBatchNo,
        qrValue: sliceBatchNo,
        ticketId: row.ticketId,
        title,
      };
    }),
  );

  const common = {
    continueOnError: false,
    copies: 1,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    offsetXmm: 0,
    offsetYmm: 0,
    printMode: 'raw',
    printerKey: 'slitting',
    rawProtocol: 'pplb',
    waitForSpooler: true,
  };

  return items.length === 1
    ? { ...common, ...items[0] }
    : { ...common, items, title };
}
