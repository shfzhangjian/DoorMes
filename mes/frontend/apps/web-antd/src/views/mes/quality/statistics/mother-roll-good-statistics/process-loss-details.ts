import type { MesQmsMotherRollGoodStatisticsApi } from '#/api/mes/quality/statistics/mother-roll-good-statistics';

type Piece = MesQmsMotherRollGoodStatisticsApi.ProcessPivotPiece;
type Inspection = MesQmsMotherRollGoodStatisticsApi.ProcessPivotInspection;

const normalize = (value?: string) => (value || '').trim().toUpperCase();
const validKey = (key: string) => !!key && key !== '-';

/** 与后端 processLossQty 的片号集合及 putIfAbsent 别名匹配规则保持一致。 */
export function collectProcessLossPieces(
  stage: MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage,
) {
  const pieces = new Map<string, {
    inspections: Inspection[];
    piece: Piece;
    selfCheckNg: boolean;
  }>();
  const aliases = new Map<string, string>();
  for (const piece of stage.pieceDetails || []) {
    if (stage.stageCode === 'PRESS_SLOT' && piece.sourceSlittingOkFlag !== true) continue;
    const key = normalize([piece.pieceNo, piece.outputBatchNo, piece.sourceBatchNo]
      .find((value) => value?.trim()));
    if (!validKey(key)) continue;
    const current = pieces.get(key);
    if (current) {
      current.selfCheckNg ||= piece.defectFlag === true;
    } else {
      pieces.set(key, { inspections: [], piece, selfCheckNg: piece.defectFlag === true });
    }
    for (const alias of [key, piece.pieceNo, piece.outputBatchNo, piece.sourceBatchNo].map(normalize)) {
      if (validKey(alias) && !aliases.has(alias)) aliases.set(alias, key);
    }
  }
  for (const inspection of stage.inspectionDetails || []) {
    if (stage.stageCode === 'PRESS_SLOT' && inspection.sourceSlittingOkFlag !== true) continue;
    if (stage.stageCode === 'ADHESIVE2' && normalize(inspection.sourceType) === 'GLUE_BOARD_FAI') continue;
    const matchedKeys = new Set([inspection.productBatchNo, inspection.inspectionNo]
      .map((value) => aliases.get(normalize(value))));
    // 与后端一致：被正常产出排除的已完成压槽首检，以完整产品片号补入损耗。
    const sampleKey = normalize(inspection.productBatchNo);
    if (![...matchedKeys].some(Boolean)
      && stage.stageCode === 'PRESS_SLOT'
      && inspection.firstInspectionSampleFlag === true
      && normalize(inspection.sourceType) === 'FAI'
      && normalize(inspection.status) === 'COMPLETED'
      && /^[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS][0-9]{3}[A-Z]?$/.test(sampleKey)) {
      if (!pieces.has(sampleKey)) {
        pieces.set(sampleKey, {
          inspections: [],
          piece: { pieceNo: sampleKey, outputBatchNo: inspection.productBatchNo },
          selfCheckNg: false,
        });
      }
      matchedKeys.add(sampleKey);
    }
    for (const key of matchedKeys) {
      if (key) pieces.get(key)?.inspections.push(inspection);
    }
  }
  return [...pieces.values()].filter((item) => item.selfCheckNg || item.inspections.length > 0);
}
