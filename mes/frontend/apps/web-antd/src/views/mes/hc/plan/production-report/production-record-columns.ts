import type { MesHcProductionReportApi } from '#/api/mes/hc/production-report';
import dayjs from 'dayjs';

type Row = Record<string, unknown>;
type Field = [string, string, ('number' | 'integer')?];
const identity: Field[] = [
  ['型号', 'modelCode'],
  ['类型', 'padType'],
];
const material: Field = ['料号', 'materialCode'];
const batch: Field = ['批号', 'batchNo'];
const completion: Field = ['完工日期', 'completion'];
const tail: Field[] = [
  ['记录人', 'recorderName'],
  ['备注', 'remark'],
];
const adhesive = (second: boolean): Field[] => [
  completion,
  ...identity,
  material,
  batch,
  [
    second ? '粘胶2投入(pcs)' : '投入米数(m)',
    'inputQty',
    second ? 'integer' : 'number',
  ],
  [
    second ? '粘胶2产出(pcs)' : '产出米数(m)',
    'outputQty',
    second ? 'integer' : 'number',
  ],
  ['胶板料号', 'glueBoardMaterialCode'],
  ['胶板批号', 'glueBoardBatchNo'],
  ['胶板消耗量(m)', 'glueBoardConsumeQty', 'number'],
  ['数据来源', 'recordSource'],
  ...tail,
];
const fields: Record<string, Field[]> = {
  FORMULA_PRODUCTION_RECORD: [
    ['日期', 'reportDate'],
    ...identity,
    material,
    batch,
    ['滤网批号', 'filterBatchNo'],
    ['投料重量(kg)', 'inputWeight', 'number'],
    ['产出重量(kg)', 'outputWeight', 'number'],
    ['搅拌机机台编号', 'mixerEquipmentCode'],
    ['配料罐罐号', 'batchingTankNo'],
    ['脱泡机机台编号', 'foamingEquipmentCode'],
    ['脱泡罐罐号', 'defoamingTankNo'],
    ...tail,
  ],
  WET_PRODUCTION_RECORD: [
    completion,
    ...identity,
    material,
    ['批次', 'batchNo'],
    ['投入(kg)', 'inputKg', 'number'],
    ['产出(m)', 'outputMeter', 'number'],
    ['PET型号', 'petModel'],
    ['PET批号', 'petBatchNo'],
    ['导布批号', 'guideClothBatchNo'],
    ['导布累计使用次数', 'guideClothUseCount'],
    ['导布更换', 'guideClothChanged'],
    ['更换说明', 'changeDesc'],
    ...tail,
  ],
  GRINDING_PRODUCTION_RECORD: [
    ['状态', 'status'],
    completion,
    ...identity,
    material,
    ['生产批号', 'batchNo'],
    ['记录角色', 'recordRole'],
    ['投入(m)', 'inputLength', 'number'],
    ['产出(m)', 'outputLength', 'number'],
    ['磨皮次数', 'passName'],
    ['砂纸累计寿命(m)', 'sandpaperLife', 'number'],
    ['砂纸累计天数', 'sandpaperLifeDays'],
    ['砂纸批号', 'sandpaperBatchNo'],
    ['导布累计寿命(次)', 'guideClothLife'],
    ['导布批号', 'guideClothBatchNo'],
    ['更换原因', 'replaceReason'],
    ['记录人', 'recorderName'],
    ['确认人', 'confirmerName'],
    ['备注', 'remark'],
  ],
  ADHESIVE1_PRODUCTION_RECORD: adhesive(false),
  ADHESIVE2_PRODUCTION_RECORD: adhesive(true),
  SLITTING_PRESS_PRODUCTION_RECORD: [
    completion,
    ...identity,
    batch,
    ['分切投入(m)', 'slittingInputM', 'number'],
    ['分切确认产出(pcs)', 'slittingOutputPcs', 'integer'],
    ['分切NG(pcs)', 'slittingNgPcs', 'integer'],
    ['压槽实际投入(pcs)', 'pressSlotActualInputPcs', 'number'],
    ['压槽实际产出(pcs)', 'pressSlotActualOutputPcs', 'number'],
    ['压槽合格产出(pcs)', 'pressSlotOutputPcs', 'number'],
    ['压槽清洗累计片数', 'rollerCleanAccumulatedPcs'],
    ['压槽辊累计使用天数', 'rollerCleanUseDays'],
    ['轴承更换累计片数', 'bearingReplaceAccumulatedPcs'],
    ['轴承累计使用天数', 'bearingReplaceUseDays'],
    ['来源', 'recordSource'],
    ...tail,
  ],
  CUT_ROUND_PRODUCTION_RECORD: [
    completion,
    ...identity,
    ['生产批号', 'productionBatchNo'],
    ['裁切尺寸(mm)', 'cutSizeMm'],
    ['投入(pcs)', 'inputQty', 'number'],
    ['产出(pcs)', 'outputQty', 'number'],
    ['刀片累计裁切(pcs)', 'bladeUseCount'],
    ['裁切片数累计(≤2000pcs)', 'feltUseCount'],
    ['毛毡累计使用天数(≤90天)', 'feltUseDays'],
    ['刀片更换原因', 'bladeReplaceReason'],
    ['来源', 'recordSource'],
    ...tail,
  ],
};

export function productionRecordGroups(
  records: MesHcProductionReportApi.ReportRecord[] = [],
) {
  const groups = new Map<string, Row[]>();
  for (const record of records) {
    const type = record.sourceType || 'UNKNOWN';
    let data = record.productionData;
    // 兼容旧详情响应；字段只按已知生产记录列展示。
    if (!data && record.detailJson) {
      try {
        const parsed = JSON.parse(record.detailJson);
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed))
          data = parsed;
      } catch {
        /* 保留可用的通用字段 */
      }
    }
    const row: Row = {
      modelCode: record.modelCode,
      padType: record.padType,
      materialCode: record.materialCode,
      reportDate: record.reportDate,
      recordTime: record.reportTime,
      batchNo: record.productionBatchNo || record.batchNo,
      productionBatchNo: record.productionBatchNo,
      recorderName: record.recorderName,
      confirmerName: record.confirmerName,
      remark: record.remark,
      ...data,
      _key: `${type}-${record.planOperationId}-${record.id}-${record.recordRole || record.reportType || ''}`,
    };
    const rows = groups.get(type) || [];
    rows.push(row);
    groups.set(type, rows);
  }
  return [...groups].map(([type, rows]) => ({
    type,
    rows,
    columns: (fields[type] || [...identity, batch, ...tail]).map(
      ([title, key, format]) => ({
        title,
        key,
        dataIndex: key,
        width: key === 'remark' ? 260 : key === 'completion' ? 180 : 150,
        customRender: ({ record }: { record: Row }) =>
          productionCell(type, key, record, format, rows),
      }),
    ),
  }));
}

function text(value: unknown) {
  return String(value ?? '').trim() || '-';
}
function productionCell(
  type: string,
  key: string,
  row: Row,
  format?: Field[2],
  rows: Row[] = [],
): string {
  const value = row[key];
  if (type === 'GRINDING_PRODUCTION_RECORD' && key === 'sandpaperBatchNo') {
    const sourceKey = (item: Row) =>
      item.sourceDetailId
        ? `${item.sourceBizType || (item.passType === 'SECOND' ? 'SECOND' : 'FIRST_ORIGINAL')}:${item.sourceDetailId}`
        : item.sourceType === 'MANUAL' && item.manualSplitGroupNo
          ? `MANUAL:${item.manualSplitGroupNo}`
          : '';
    const key = sourceKey(row);
    const split =
      key && rows.some((item) => item !== row && sourceKey(item) === key);
    return split
      ? `${text(value)}（${row.sourceSegmentNo === 2 ? '新砂纸' : '旧砂纸'}）`
      : text(value);
  }
  if (key === 'completion') {
    const time = row.recordTime;
    if (time) {
      const date = dayjs(time as string);
      return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text(time);
    }
    return type === 'GRINDING_PRODUCTION_RECORD'
      ? '-'
      : text(row.reportDate || row.recordDate);
  }
  if (key === 'padType')
    return value === 'BLACK_PAD'
      ? '黑垫'
      : value === 'WHITE_PAD'
        ? '白垫'
        : '未归类';
  if (key === 'guideClothChanged') return value === 'Y' ? '是' : '否';
  if (key === 'status') return value === 'CONFIRMED' ? '已确认' : '待确认';
  if (key === 'recordRole') {
    const role = row.recordRole || row.sourceBizType;
    if (
      role === 'FIRST_ALLOCATION' ||
      role === 'FIRST_ORIGINAL' ||
      row.passType === 'FIRST'
    )
      return '一磨';
    if (role === 'SECOND' || row.passType === 'SECOND') return '二磨';
    if (row.passType === 'THIRD') return '三磨';
    if (row.passType === 'FOURTH') return '四磨';
    return '手工记录';
  }
  if (format && value !== null && value !== undefined && value !== '') {
    const number = Number(value);
    return Number.isFinite(number)
      ? number.toLocaleString('zh-CN', {
          maximumFractionDigits: format === 'integer' ? 0 : 3,
        })
      : text(value);
  }
  return text(value);
}
