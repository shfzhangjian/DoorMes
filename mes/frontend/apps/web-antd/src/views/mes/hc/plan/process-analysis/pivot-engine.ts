import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import type { MesQmsYieldAnalysisApi } from '#/api/mes/quality/statistics/yield-analysis';
import type {
  AnalysisConfig,
  AnalysisFact,
  AnalysisFilter,
  AnalysisFormula,
  AnalysisMetric,
  DrillStep,
  PivotCellColumn,
  PivotColumnGroup,
  PivotDisplayRow,
  PivotResult,
} from './types';

const STAGE_ORDER = [
  'FORMULA',
  'WET',
  'GRINDING',
  'ADHESIVE1',
  'SLITTING',
  'PRESS_SLOT',
  'ADHESIVE2',
  'CUT_ROUND',
  'SHIPPING_INSPECTION',
];

const NG_STATUS_PATTERN =
  /(?:^|[\s_-])(?:DEFECT|FAILED?|NG|UNQUALIFIED)(?:$|[\s_-])|不合格|异常/i;
const PIECE_DEFECT_VALUE_FIELDS = [
  'pieceNgCount',
  'defectQty',
  'totalDefectQty',
  'inspectionNgQty',
  'blackDotCount',
  'blueDotCount',
  'yellowDotCount',
  'redDotCount',
  'pinholeCount',
  'stripeCount',
  'wrinkleCount',
  'waveCount',
  'otherCount',
];

export function normalizeProcessPivotRows(
  rows: MesHcPlanOrderApi.ProcessPivotRow[],
  qualityRows: MesQmsYieldAnalysisApi.SummaryRow[] = [],
): AnalysisFact[] {
  const facts: AnalysisFact[] = [];
  const qualityMetricIndex = buildQualityMetricIndex(qualityRows);
  rows.forEach((row, rowIndex) => {
    const stages = row.stages || {};
    const stageCodes = [
      ...STAGE_ORDER.filter((code) => stages[code]),
      ...Object.keys(stages).filter((code) => !STAGE_ORDER.includes(code)),
    ];
    stageCodes.forEach((stageCode) => {
      const stage = stages[stageCode] || {};
      const pivotRowKey =
        row.pivotRowKey ||
        [
          row.planNo,
          row.segmentBatchNo,
          row.actualModelCode,
          row.actualSizeSpec,
          rowIndex,
        ].join('|');
      const planMergeKey =
        row.planMergeKey || `PLAN|${row.id || row.planNo || rowIndex}`;
      const stageGrainKey =
        row.stageMergeKeys?.[stageCode] ||
        `${pivotRowKey}|${stageCode || 'UNKNOWN'}`;
      const pieceDetails = (stage.pieceDetails || []).filter((piece) =>
        isProductionPiece(piece, row.segmentBatchNo),
      );
      const inspectionDetails = stage.inspectionDetails || [];
      const qualityMetric = resolveQualityMetric(
        qualityMetricIndex,
        row.motherRollBatchNo,
        row.segmentBatchNo,
        stageCode,
      );
      facts.push({
        ...row,
        ...stage,
        blackDotCount: Number(qualityMetric?.blackDotCount || 0),
        blueDotCount: Number(qualityMetric?.blueDotCount || 0),
        inspectionRecordCount: inspectionDetails.length,
        otherCount: Number(qualityMetric?.otherCount || 0),
        pinholeCount: Number(qualityMetric?.pinholeCount || 0),
        pieceCount: pieceDetails.length,
        pieceNgCount: pieceDetails.filter(
          (item) =>
            Boolean(item.defectFlag) ||
            NG_STATUS_PATTERN.test(item.status || ''),
        ).length,
        pivotRowKey,
        planMergeKey,
        qualityGrainKey: buildQualityMetricKey(
          row.motherRollBatchNo,
          row.segmentBatchNo,
          stageCode,
        ),
        redDotCount: Number(qualityMetric?.redDotCount || 0),
        sourceRow: row,
        stage,
        stageCode,
        stageGrainKey,
        stageName: stage.stageName || stageCode,
        stageRemark: stage.remark,
        stripeCount: Number(qualityMetric?.stripeCount || 0),
        waveCount: Number(qualityMetric?.waveCount || 0),
        wrinkleCount: Number(qualityMetric?.wrinkleCount || 0),
        yellowDotCount: Number(qualityMetric?.yellowDotCount || 0),
      });
    });
  });
  return facts;
}

function buildQualityMetricIndex(rows: MesQmsYieldAnalysisApi.SummaryRow[]) {
  const exact = new Map<string, MesQmsYieldAnalysisApi.SummaryRow>();
  const bySegment = new Map<string, MesQmsYieldAnalysisApi.SummaryRow>();
  rows.forEach((row) => {
    exact.set(
      buildQualityMetricKey(row.motherRollNo, row.segmentNo, row.processCode),
      row,
    );
    bySegment.set(
      buildQualityMetricKey('', row.segmentNo, row.processCode),
      row,
    );
  });
  return { bySegment, exact };
}

function resolveQualityMetric(
  index: ReturnType<typeof buildQualityMetricIndex>,
  motherRollBatchNo: unknown,
  segmentBatchNo: unknown,
  stageCode: unknown,
) {
  return (
    index.exact.get(
      buildQualityMetricKey(motherRollBatchNo, segmentBatchNo, stageCode),
    ) ||
    index.bySegment.get(buildQualityMetricKey('', segmentBatchNo, stageCode))
  );
}

function buildQualityMetricKey(
  motherRollBatchNo: unknown,
  segmentBatchNo: unknown,
  stageCode: unknown,
) {
  return [motherRollBatchNo, segmentBatchNo, stageCode]
    .map((value) =>
      String(value || '')
        .trim()
        .toUpperCase(),
    )
    .join('|');
}

export function applyAnalysisFilters(
  facts: AnalysisFact[],
  filters: AnalysisFilter[],
): AnalysisFact[] {
  const activeFilters = filters.filter(
    (filter) => filter.field && filter.operator,
  );
  if (activeFilters.length === 0) {
    return facts;
  }
  const filterFacts = activeFilters.some((filter) => filter.field === 'pieceNo')
    ? expandFactsByPiece(facts)
    : facts;
  return filterFacts.filter((fact) =>
    activeFilters.every((filter) => matchesFilter(fact[filter.field], filter)),
  );
}

export function buildPivotResult(
  sourceFacts: AnalysisFact[],
  config: AnalysisConfig,
  drillPath: DrillStep[],
  columnTemplateFacts?: AnalysisFact[],
): PivotResult {
  const pieceDimensionIndex = config.rowDimensions.indexOf('pieceNo');
  const requiresPieceGrain =
    config.columnDimensions.includes('pieceNo') ||
    (pieceDimensionIndex >= 0 && drillPath.length >= pieceDimensionIndex);
  const pivotFacts = requiresPieceGrain
    ? expandFactsByPiece(sourceFacts)
    : sourceFacts;
  const facts = pivotFacts.filter((fact) =>
    drillPath.every(
      (step) => normalizeDimensionValue(fact[step.field]) === step.value,
    ),
  );
  const currentDimension = config.rowDimensions[drillPath.length];
  const rowGroups = groupFacts(
    facts,
    currentDimension ? (fact) => fact[currentDimension] : () => '全部',
  );
  const columnTuples = buildColumnTuples(
    columnTemplateFacts || facts,
    config.columnDimensions,
  ).filter((tuple) =>
    config.columnDimensions.every((field, index) => {
      const visibleValues = config.columnValueFilters?.[field] || [];
      return (
        visibleValues.length === 0 ||
        visibleValues.includes(tuple.values[index] || '')
      );
    }),
  );
  const valueDefinitions: Array<AnalysisFormula | AnalysisMetric> = [
    ...config.metrics,
    ...config.formulas,
  ];
  const valueColumns = valueDefinitions.map((item) =>
    buildValueColumn(item, '__all__'),
  );
  const columnGroups: PivotColumnGroup[] = config.columnDimensions.length
    ? columnTuples.map((tuple, columnIndex) => ({
        children: valueDefinitions.map((item) =>
          buildValueColumn(item, `c${columnIndex}`),
        ),
        key: tuple.key,
        title: tuple.label,
      }))
    : [];

  const rows: PivotDisplayRow[] = [...rowGroups.entries()].map(
    ([groupValue, groupFactsValue], rowIndex) => {
      const row: PivotDisplayRow = {
        __facts: groupFactsValue,
        __groupLabel: displayValue(groupValue),
        __groupValue: groupValue,
        __key: `${drillPath.length}|${groupValue}|${rowIndex}`,
      };
      if (config.columnDimensions.length === 0) {
        const context = buildMetricContext(groupFactsValue, config);
        valueDefinitions.forEach((item) => {
          row[valueCellKey('__all__', item.id)] = resolveContextValue(
            item,
            context,
          );
        });
      } else {
        columnTuples.forEach((tuple, columnIndex) => {
          const tupleFacts = groupFactsValue.filter((fact) =>
            tuple.values.every(
              (value, index) =>
                normalizeDimensionValue(
                  fact[config.columnDimensions[index] || ''],
                ) === value,
            ),
          );
          const context = buildMetricContext(tupleFacts, config);
          valueDefinitions.forEach((item) => {
            row[valueCellKey(`c${columnIndex}`, item.id)] = resolveContextValue(
              item,
              context,
            );
          });
        });
      }
      return row;
    },
  );

  return {
    columnGroups,
    currentDimension,
    facts,
    rows,
    valueColumns,
  };
}

function expandFactsByPiece(facts: AnalysisFact[]): AnalysisFact[] {
  return facts.flatMap((fact) => {
    const pieceDetails = (fact.stage.pieceDetails || []).filter((piece) =>
      isProductionPiece(piece, fact.segmentBatchNo),
    );
    const inspectionDetails = fact.stage.inspectionDetails || [];
    const pieceNumbers = uniqueValues(
      pieceDetails
        .map((piece) =>
          normalizeDimensionValue(piece.pieceNo || piece.outputBatchNo),
        )
        .filter(Boolean),
    );
    return pieceNumbers.map((pieceNo) => {
      const matchingPieces = pieceDetails.filter(
        (piece) =>
          normalizeDimensionValue(piece.pieceNo || piece.outputBatchNo) ===
          pieceNo,
      );
      const matchingInspections = inspectionDetails.filter(
        (inspection) =>
          normalizeDimensionValue(inspection.productBatchNo) === pieceNo,
      );
      return {
        ...fact,
        inspectionRecordCount: matchingInspections.length,
        pieceCount: matchingPieces.length,
        pieceNgCount: matchingPieces.filter(
          (piece) =>
            Boolean(piece.defectFlag) ||
            NG_STATUS_PATTERN.test(piece.status || ''),
        ).length,
        pieceNo,
        stage: {
          ...fact.stage,
          inspectionDetails: matchingInspections,
          pieceDetails: matchingPieces,
        },
      };
    });
  });
}

export function buildPiecePivotFacts(
  facts: AnalysisFact[],
  qualityRows: MesQmsYieldAnalysisApi.DetailRow[] = [],
): AnalysisFact[] {
  const stageFacts = distinctStageFacts(facts);
  const pieceMap = collectPieceMetadata(facts, qualityRows);
  const qualityIndex = buildPieceQualityIndex(qualityRows);
  const result: AnalysisFact[] = [];
  pieceMap.forEach((pieceMeta, pieceNo) => {
    stageFacts.forEach((stageFact) => {
      const pieceDetail = findPieceDetail(facts, stageFact.stageCode, pieceNo);
      const quality = qualityIndex.get(
        buildPieceQualityKey(pieceNo, stageFact.stageCode),
      );
      const status = normalizeDimensionValue(pieceDetail?.status).toUpperCase();
      const defectFlag =
        Boolean(pieceDetail?.defectFlag) ||
        NG_STATUS_PATTERN.test(status) ||
        Number(quality?.ngCount || 0) > 0;
      const doneFlag =
        !defectFlag &&
        (status === 'DONE' ||
          Boolean(pieceDetail?.reportConfirmed) ||
          Boolean(quality));
      const pendingFlag =
        !defectFlag &&
        !doneFlag &&
        (status === 'PENDING' || Boolean(pieceDetail));
      const pieceStageKey = [
        normalizeDimensionValue(stageFact.segmentBatchNo),
        pieceNo,
        stageFact.stageCode,
      ].join('|');
      result.push({
        ...stageFact,
        actualModelCode:
          pieceDetail?.actualModelCode ||
          pieceMeta.actualModelCode ||
          stageFact.actualModelCode,
        actualSizeSpec:
          pieceDetail?.actualSizeSpec ||
          pieceMeta.actualSizeSpec ||
          stageFact.actualSizeSpec,
        blackDotCount: Number(quality?.blackDotCount || 0),
        blueDotCount: Number(quality?.blueDotCount || 0),
        confirmedQty: pieceDetail?.reportConfirmed ? 1 : 0,
        defectQty: defectFlag ? 1 : 0,
        doneQty: doneFlag ? 1 : 0,
        inputQty: pieceDetail ? 1 : 0,
        inspectionNgQty: Number(quality?.submissionNgCount || 0),
        inspectionQty: quality ? 1 : 0,
        inspectionRecordCount: quality ? 1 : 0,
        lastReportTime:
          pieceDetail?.lastReportTime || pieceMeta.lastReportTime || '',
        lengthQty: 0,
        latestReportTime:
          pieceDetail?.lastReportTime || pieceMeta.lastReportTime || '',
        netPlanQty: 0,
        otherCount: Number(quality?.otherCount || 0),
        outputBatchNos:
          pieceDetail?.outputBatchNo || pieceMeta.outputBatchNo || pieceNo,
        pendingQty: pendingFlag ? 1 : 0,
        pieceCount: pieceDetail ? 1 : 0,
        pieceListNo: pieceNo,
        pieceNgCount: defectFlag ? 1 : 0,
        pieceNo,
        pinholeCount: Number(quality?.pinholeCount || 0),
        planMergeKey: pieceStageKey,
        processLength: 0,
        qualityGrainKey: `PIECE_QUALITY|${pieceStageKey}`,
        redDotCount: Number(quality?.redDotCount || 0),
        reportQty: pieceDetail ? 1 : 0,
        sourceBatchNos:
          pieceDetail?.sourceBatchNo || pieceMeta.sourceBatchNo || '',
        stageRemark: pieceDetail?.remark || pieceMeta.remark || '',
        stageStatus: defectFlag
          ? 'DEFECT'
          : doneFlag
            ? 'DONE'
            : pendingFlag
              ? 'PENDING'
              : '',
        stage: {
          ...stageFact.stage,
          confirmedQty: pieceDetail?.reportConfirmed ? 1 : 0,
          defectQty: defectFlag ? 1 : 0,
          doneQty: doneFlag ? 1 : 0,
          inputQty: pieceDetail ? 1 : 0,
          inspectionDetails: [],
          inspectionNgQty: Number(quality?.submissionNgCount || 0),
          inspectionQty: quality ? 1 : 0,
          lengthQty: 0,
          outputBatchNos:
            pieceDetail?.outputBatchNo || pieceMeta.outputBatchNo || pieceNo,
          pendingQty: pendingFlag ? 1 : 0,
          pieceDetails: pieceDetail ? [pieceDetail] : [],
          processLength: 0,
          reportQty: pieceDetail ? 1 : 0,
          remark: pieceDetail?.remark || pieceMeta.remark || '',
          sourceBatchNos:
            pieceDetail?.sourceBatchNo || pieceMeta.sourceBatchNo || '',
          startPosition: 0,
        },
        stageGrainKey: `PIECE_STAGE|${pieceStageKey}`,
        startPosition: 0,
        stripeCount: Number(quality?.stripeCount || 0),
        targetQty: 0,
        totalDefectQty: defectFlag ? 1 : 0,
        waveCount: Number(quality?.waveCount || 0),
        wrinkleCount: Number(quality?.wrinkleCount || 0),
        yellowDotCount: Number(quality?.yellowDotCount || 0),
      });
    });
  });
  return result;
}

export function filterDefectivePieceFacts(
  facts: AnalysisFact[],
): AnalysisFact[] {
  const defectivePieceNumbers = new Set(
    facts
      .filter(
        (fact) =>
          NG_STATUS_PATTERN.test(normalizeDimensionValue(fact.stageStatus)) ||
          PIECE_DEFECT_VALUE_FIELDS.some(
            (field) => Number(fact[field] || 0) > 0,
          ),
      )
      .map((fact) => normalizeDimensionValue(fact.pieceListNo ?? fact.pieceNo))
      .filter(Boolean),
  );
  return facts.filter((fact) =>
    defectivePieceNumbers.has(
      normalizeDimensionValue(fact.pieceListNo ?? fact.pieceNo),
    ),
  );
}

function distinctStageFacts(facts: AnalysisFact[]) {
  const stages = new Map<string, AnalysisFact>();
  facts.forEach((fact) => {
    if (!stages.has(fact.stageCode)) {
      stages.set(fact.stageCode, fact);
    }
  });
  return [...stages.values()];
}

function collectPieceMetadata(
  facts: AnalysisFact[],
  qualityRows: MesQmsYieldAnalysisApi.DetailRow[],
) {
  const pieces = new Map<
    string,
    {
      actualModelCode: string;
      actualSizeSpec: string;
      lastReportTime: string;
      outputBatchNo: string;
      remark: string;
      sourceBatchNo: string;
    }
  >();
  facts.forEach((fact) => {
    (fact.stage.pieceDetails || []).forEach((piece) => {
      if (!isProductionPiece(piece, fact.segmentBatchNo)) return;
      const pieceNo = normalizeDimensionValue(
        piece.pieceNo || piece.outputBatchNo,
      );
      const current = pieces.get(pieceNo) || {
        actualModelCode: '',
        actualSizeSpec: '',
        lastReportTime: '',
        outputBatchNo: '',
        remark: '',
        sourceBatchNo: '',
      };
      current.actualModelCode ||= piece.actualModelCode || '';
      current.actualSizeSpec ||= piece.actualSizeSpec || '';
      current.outputBatchNo ||= piece.outputBatchNo || pieceNo;
      current.sourceBatchNo ||= piece.sourceBatchNo || '';
      current.lastReportTime = latestText(
        current.lastReportTime,
        piece.lastReportTime,
      );
      current.remark ||= piece.remark || '';
      pieces.set(pieceNo, current);
    });
  });
  qualityRows.forEach((row) => {
    const pieceNo = normalizeDimensionValue(row.pieceNo);
    const segmentNo = normalizeDimensionValue(row.segmentNo);
    if (!pieceNo || pieceNo.toUpperCase() === segmentNo.toUpperCase()) return;
    const current = pieces.get(pieceNo) || {
      actualModelCode: '',
      actualSizeSpec: '',
      lastReportTime: '',
      outputBatchNo: pieceNo,
      remark: '',
      sourceBatchNo: '',
    };
    current.actualModelCode ||= row.modelCode || '';
    current.lastReportTime = latestText(
      current.lastReportTime,
      row.confirmTime,
    );
    pieces.set(pieceNo, current);
  });
  return pieces;
}

function findPieceDetail(
  facts: AnalysisFact[],
  stageCode: string,
  pieceNo: string,
) {
  for (const fact of facts) {
    if (fact.stageCode !== stageCode) continue;
    const detail = (fact.stage.pieceDetails || []).find(
      (piece) =>
        normalizeDimensionValue(piece.pieceNo || piece.outputBatchNo) ===
        pieceNo,
    );
    if (detail) return detail;
  }
  return undefined;
}

function buildPieceQualityIndex(rows: MesQmsYieldAnalysisApi.DetailRow[]) {
  const result = new Map<string, MesQmsYieldAnalysisApi.DetailRow>();
  rows.forEach((row) => {
    const key = buildPieceQualityKey(row.pieceNo, row.processCode);
    const current = result.get(key);
    if (!current) {
      result.set(key, { ...row });
      return;
    }
    result.set(key, {
      ...current,
      blackDotCount: Math.max(
        Number(current.blackDotCount || 0),
        Number(row.blackDotCount || 0),
      ),
      blueDotCount: Math.max(
        Number(current.blueDotCount || 0),
        Number(row.blueDotCount || 0),
      ),
      ngCount: Math.max(Number(current.ngCount || 0), Number(row.ngCount || 0)),
      otherCount: Math.max(
        Number(current.otherCount || 0),
        Number(row.otherCount || 0),
      ),
      pinholeCount: Math.max(
        Number(current.pinholeCount || 0),
        Number(row.pinholeCount || 0),
      ),
      redDotCount: Math.max(
        Number(current.redDotCount || 0),
        Number(row.redDotCount || 0),
      ),
      stripeCount: Math.max(
        Number(current.stripeCount || 0),
        Number(row.stripeCount || 0),
      ),
      submissionNgCount: Math.max(
        Number(current.submissionNgCount || 0),
        Number(row.submissionNgCount || 0),
      ),
      waveCount: Math.max(
        Number(current.waveCount || 0),
        Number(row.waveCount || 0),
      ),
      wrinkleCount: Math.max(
        Number(current.wrinkleCount || 0),
        Number(row.wrinkleCount || 0),
      ),
      yellowDotCount: Math.max(
        Number(current.yellowDotCount || 0),
        Number(row.yellowDotCount || 0),
      ),
    });
  });
  return result;
}

function buildPieceQualityKey(pieceNo: unknown, stageCode: unknown) {
  return [pieceNo, stageCode]
    .map((value) => normalizeDimensionValue(value).trim().toUpperCase())
    .join('|');
}

function latestText(current: unknown, next: unknown) {
  const currentText = normalizeDimensionValue(current);
  const nextText = normalizeDimensionValue(next);
  if (!currentText) return nextText;
  if (!nextText) return currentText;
  return nextText > currentText ? nextText : currentText;
}

function isProductionPiece(
  piece: MesHcPlanOrderApi.ProcessPivotPiece,
  segmentBatchNo: unknown,
) {
  const pieceNo = normalizeDimensionValue(piece.pieceNo || piece.outputBatchNo);
  const segmentNo = normalizeDimensionValue(segmentBatchNo);
  return (
    Boolean(pieceNo) &&
    (!segmentNo || pieceNo.toUpperCase() !== segmentNo.toUpperCase())
  );
}

export function buildChartData(
  sourceFacts: AnalysisFact[],
  config: AnalysisConfig,
): {
  categories: string[];
  series: Array<{ data: number[]; name: string }>;
} {
  const { categoryField, seriesField, valueField } = config.chart;
  if (!categoryField || !valueField) {
    return { categories: [], series: [] };
  }
  const chartFacts =
    categoryField === 'pieceNo' || seriesField === 'pieceNo'
      ? expandFactsByPiece(sourceFacts)
      : sourceFacts;
  const categories = uniqueValues(
    chartFacts.map((fact) => normalizeDimensionValue(fact[categoryField])),
  ).sort(compareDimensionValues);
  const seriesNames = seriesField
    ? uniqueValues(
        chartFacts.map((fact) => normalizeDimensionValue(fact[seriesField])),
      ).sort(compareDimensionValues)
    : ['数值'];
  const series = seriesNames.map((seriesName) => ({
    data: categories.map((category) => {
      const group = chartFacts.filter(
        (fact) =>
          normalizeDimensionValue(fact[categoryField]) === category &&
          (!seriesField ||
            normalizeDimensionValue(fact[seriesField]) === seriesName),
      );
      return resolveValueById(group, config, valueField);
    }),
    name: displayValue(seriesName),
  }));
  return {
    categories: categories.map(displayValue),
    series,
  };
}

export function formatAnalysisValue(
  value: unknown,
  definition?: AnalysisFormula | AnalysisMetric,
) {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value !== 'number') return String(value);
  const decimals = Math.max(0, Math.min(6, Number(definition?.decimals ?? 2)));
  const text = Number.isFinite(value)
    ? value.toLocaleString('zh-CN', {
        maximumFractionDigits: decimals,
        minimumFractionDigits: decimals,
      })
    : '0';
  return 'format' in (definition || {}) &&
    (definition as AnalysisFormula).format === 'PERCENT'
    ? `${text}%`
    : text;
}

export function validateFormulaExpression(
  expression: string,
  allowedMetricIds: string[],
): string | undefined {
  if (!expression.trim()) return '公式表达式不能为空';
  const refs = [...expression.matchAll(/\{([A-Za-z][\w-]*)\}/g)].map(
    (match) => match[1] || '',
  );
  const unknown = refs.find((ref) => !allowedMetricIds.includes(ref));
  if (unknown) return `公式引用了不存在的指标：${unknown}`;
  const expressionWithoutRefs = expression.replace(
    /\{([A-Za-z][\w-]*)\}/g,
    '1',
  );
  if (/[^0-9+\-*/().\sEe]/.test(expressionWithoutRefs)) {
    return '公式仅允许数字、指标引用、括号和 + - * /';
  }
  try {
    evaluateArithmetic(expressionWithoutRefs);
    return undefined;
  } catch (error) {
    return error instanceof Error ? error.message : '公式语法不正确';
  }
}

export function evaluateFormula(
  formula: AnalysisFormula,
  context: Record<string, number>,
) {
  try {
    const expression = formula.expression.replace(
      /\{([A-Za-z][\w-]*)\}/g,
      (_, id: string) => String(Number(context[id] || 0)),
    );
    const value = evaluateArithmetic(expression);
    return Number.isFinite(value) ? value : 0;
  } catch {
    return 0;
  }
}

function buildMetricContext(
  facts: AnalysisFact[],
  config: AnalysisConfig,
): Record<string, number> {
  const context: Record<string, number> = {};
  config.metrics.forEach((metric) => {
    context[metric.id] = aggregateMetric(facts, metric);
  });
  config.formulas.forEach((formula) => {
    context[formula.id] = evaluateFormula(formula, context);
  });
  return context;
}

function aggregateMetric(facts: AnalysisFact[], metric: AnalysisMetric) {
  if (metric.aggregation === 'COUNT_DISTINCT') {
    return new Set(
      facts
        .map((fact) => normalizeDimensionValue(fact[metric.field]))
        .filter((value) => value !== ''),
    ).size;
  }
  const aggregationFacts =
    metric.aggregation === 'SUM'
      ? facts
      : distinctFacts(facts, metric.distinctKeyField);
  if (metric.aggregation === 'COUNT') {
    return aggregationFacts.filter((fact) => !isEmptyValue(fact[metric.field]))
      .length;
  }
  const values = aggregationFacts
    .map((fact) => toNumber(fact[metric.field]))
    .filter((value): value is number => value !== undefined);
  if (values.length === 0) return 0;
  switch (metric.aggregation) {
    case 'AVG':
      return values.reduce((sum, value) => sum + value, 0) / values.length;
    case 'MAX':
      return Math.max(...values);
    case 'MIN':
      return Math.min(...values);
    default:
      return values.reduce((sum, value) => sum + value, 0);
  }
}

function resolveValueById(
  facts: AnalysisFact[],
  config: AnalysisConfig,
  valueId: string,
) {
  return buildMetricContext(facts, config)[valueId] || 0;
}

function resolveContextValue(
  item: AnalysisFormula | AnalysisMetric,
  context: Record<string, number>,
) {
  return context[item.id] || 0;
}

function distinctFacts(facts: AnalysisFact[], keyField?: string) {
  if (!keyField) return facts;
  const seen = new Set<string>();
  return facts.filter((fact, index) => {
    const key =
      normalizeDimensionValue(fact[keyField]) ||
      `${keyField}|${fact.pivotRowKey}|${fact.stageCode}|${index}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function buildColumnTuples(facts: AnalysisFact[], dimensions: string[]) {
  if (dimensions.length === 0) {
    return [{ key: '__all__', label: '数值', values: [] as string[] }];
  }
  const tupleMap = new Map<
    string,
    { key: string; label: string; values: string[] }
  >();
  facts.forEach((fact) => {
    const values = dimensions.map((field) =>
      normalizeDimensionValue(fact[field]),
    );
    const key = values.join('\u001f');
    if (!tupleMap.has(key)) {
      tupleMap.set(key, {
        key,
        label: values.map(displayValue).join(' / '),
        values,
      });
    }
  });
  return [...tupleMap.values()].sort((left, right) => {
    const leftStage = STAGE_ORDER.indexOf(
      String(
        facts.find((fact) =>
          dimensions.every(
            (field, index) =>
              normalizeDimensionValue(fact[field]) === left.values[index],
          ),
        )?.stageCode || '',
      ),
    );
    const rightStage = STAGE_ORDER.indexOf(
      String(
        facts.find((fact) =>
          dimensions.every(
            (field, index) =>
              normalizeDimensionValue(fact[field]) === right.values[index],
          ),
        )?.stageCode || '',
      ),
    );
    if (leftStage >= 0 && rightStage >= 0 && leftStage !== rightStage) {
      return leftStage - rightStage;
    }
    return compareDimensionValues(left.label, right.label);
  });
}

function buildValueColumn(
  item: AnalysisFormula | AnalysisMetric,
  columnKey: string,
): PivotCellColumn {
  return {
    dataIndex: valueCellKey(columnKey, item.id),
    formulaId: 'expression' in item ? item.id : undefined,
    metricId: 'field' in item ? item.id : undefined,
    title: item.label,
  };
}

function valueCellKey(columnKey: string, valueId: string) {
  return `${columnKey}__${valueId}`.replaceAll(/[^A-Za-z0-9_-]/g, '_');
}

function groupFacts(
  facts: AnalysisFact[],
  selector: (fact: AnalysisFact) => unknown,
) {
  const result = new Map<string, AnalysisFact[]>();
  facts.forEach((fact) => {
    const key = normalizeDimensionValue(selector(fact));
    const group = result.get(key) || [];
    group.push(fact);
    result.set(key, group);
  });
  return new Map(
    [...result.entries()].sort(([a], [b]) => compareDimensionValues(a, b)),
  );
}

function matchesFilter(value: unknown, filter: AnalysisFilter) {
  const operator = filter.operator;
  const expected = String(filter.value || '').trim();
  if (operator === 'EMPTY') return isEmptyValue(value);
  if (operator === 'NOT_EMPTY') return !isEmptyValue(value);
  const actual = normalizeDimensionValue(value);
  if (operator === 'IN') {
    const values = expected
      .split(',')
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean);
    return values.includes(actual.toLowerCase());
  }
  if (operator === 'CONTAINS') {
    return actual.toLowerCase().includes(expected.toLowerCase());
  }
  if (operator === 'NOT_CONTAINS') {
    return !actual.toLowerCase().includes(expected.toLowerCase());
  }
  const actualNumber = toNumber(value);
  const expectedNumbers = expected
    .split(/[,~]/)
    .map((item) => Number(item.trim()));
  if (
    actualNumber !== undefined &&
    expectedNumbers.every((item) => Number.isFinite(item))
  ) {
    const target = expectedNumbers[0] || 0;
    if (operator === 'GT') return actualNumber > target;
    if (operator === 'GTE') return actualNumber >= target;
    if (operator === 'LT') return actualNumber < target;
    if (operator === 'LTE') return actualNumber <= target;
    if (operator === 'BETWEEN') {
      return (
        actualNumber >= target && actualNumber <= (expectedNumbers[1] ?? target)
      );
    }
    if (operator === 'NE') return actualNumber !== target;
    return actualNumber === target;
  }
  if (operator === 'GTE') return actual >= expected;
  if (operator === 'LTE') return actual <= expected;
  if (operator === 'BETWEEN') {
    const [start = '', end = start] = expected
      .split(/[,~]/)
      .map((item) => item.trim());
    return actual >= start && actual <= end;
  }
  if (operator === 'NE') return actual.toLowerCase() !== expected.toLowerCase();
  return actual.toLowerCase() === expected.toLowerCase();
}

function evaluateArithmetic(expression: string) {
  const tokens = tokenize(expression);
  const output: Array<number | string> = [];
  const operators: string[] = [];
  let expectingValue = true;
  tokens.forEach((token) => {
    if (typeof token === 'number') {
      output.push(token);
      expectingValue = false;
      return;
    }
    if (token === '(') {
      operators.push(token);
      expectingValue = true;
      return;
    }
    if (token === ')') {
      while (operators.length && operators.at(-1) !== '(') {
        output.push(operators.pop() || '');
      }
      if (operators.pop() !== '(') throw new Error('公式括号不匹配');
      expectingValue = false;
      return;
    }
    const operator = token === '-' && expectingValue ? 'u-' : token;
    while (
      operators.length &&
      operators.at(-1) !== '(' &&
      precedence(operators.at(-1) || '') >= precedence(operator)
    ) {
      output.push(operators.pop() || '');
    }
    operators.push(operator);
    expectingValue = true;
  });
  while (operators.length) {
    const operator = operators.pop() || '';
    if (operator === '(') throw new Error('公式括号不匹配');
    output.push(operator);
  }
  const values: number[] = [];
  output.forEach((token) => {
    if (typeof token === 'number') {
      values.push(token);
      return;
    }
    if (token === 'u-') {
      if (values.length < 1) throw new Error('公式缺少数值');
      values.push(-(values.pop() || 0));
      return;
    }
    if (values.length < 2) throw new Error('公式运算符位置不正确');
    const right = values.pop() || 0;
    const left = values.pop() || 0;
    if (token === '+') values.push(left + right);
    else if (token === '-') values.push(left - right);
    else if (token === '*') values.push(left * right);
    else if (token === '/') values.push(right === 0 ? 0 : left / right);
    else throw new Error(`不支持的运算符：${token}`);
  });
  if (values.length !== 1) throw new Error('公式语法不完整');
  return values[0] || 0;
}

function tokenize(expression: string): Array<number | string> {
  const tokens: Array<number | string> = [];
  let index = 0;
  while (index < expression.length) {
    const char = expression[index] || '';
    if (/\s/.test(char)) {
      index += 1;
      continue;
    }
    if ('+-*/()'.includes(char)) {
      tokens.push(char);
      index += 1;
      continue;
    }
    const match = expression
      .slice(index)
      .match(/^(?:\d+\.?\d*|\.\d+)(?:[Ee][+-]?\d+)?/);
    if (!match?.[0]) throw new Error(`公式包含无效字符：${char}`);
    tokens.push(Number(match[0]));
    index += match[0].length;
  }
  if (tokens.length === 0) throw new Error('公式表达式不能为空');
  return tokens;
}

function precedence(operator: string) {
  if (operator === 'u-') return 3;
  if (operator === '*' || operator === '/') return 2;
  if (operator === '+' || operator === '-') return 1;
  return 0;
}

function toNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

function isEmptyValue(value: unknown) {
  return (
    value === null ||
    value === undefined ||
    (typeof value === 'string' && value.trim() === '') ||
    (Array.isArray(value) && value.length === 0)
  );
}

function normalizeDimensionValue(value: unknown) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'boolean') return value ? '是' : '否';
  return String(value).trim();
}

function displayValue(value: unknown) {
  const text = normalizeDimensionValue(value);
  return text || '（空）';
}

function uniqueValues(values: string[]) {
  return [...new Set(values)];
}

function compareDimensionValues(left: string, right: string) {
  return left.localeCompare(right, 'zh-CN', {
    numeric: true,
    sensitivity: 'base',
  });
}
