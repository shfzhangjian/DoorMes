import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';

export const DEFAULT_STD_RULE = 'NULL_AS_DISPLAY_ONLY';

export type EntryRulePresetKey =
  | 'COMPRESSION_CALC'
  | 'DENSITY_CALC'
  | 'FIVE_POINT_THREE_REPEAT'
  | 'QUALITATIVE_JUDGMENT'
  | 'SEQUENCE_15'
  | 'SINGLE_VALUE'
  | 'SINGLE_VALUE_FIVE_REPEAT'
  | 'THREE_POINT'
  | 'THREE_POINT_FIVE_REPEAT';

type PositionRulePreset = {
  code: string;
  name: string;
  required?: boolean;
  sort: number;
};

export type EntryRulePresetParams = Record<string, any> & {
  dataRule?: MesQualityStandardApi.EntryRuleDataRule;
  displayLayout?: string;
  judgmentMetric?: string;
  positions?: PositionRulePreset[];
  repeatCount?: number;
  sampleSize?: number;
  stdRule?: string;
  valueTemplate?: MesQualityStandardApi.StandardItem['valueTemplate'];
};

type EntryRulePreset = {
  label: string;
  params: EntryRulePresetParams;
  summary: string;
};

export const DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET =
  'SINGLE_VALUE_FIVE_REPEAT' satisfies EntryRulePresetKey;
export const DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET =
  'QUALITATIVE_JUDGMENT' satisfies EntryRulePresetKey;

function singleValueDataRule(
  unit = '',
): MesQualityStandardApi.EntryRuleDataRule {
  return {
    inputFields: [
      {
        code: 'value',
        name: '实测值',
        unit,
        type: 'NUMBER',
        required: true,
        precision: 3,
      },
    ],
    resultFields: [
      {
        code: 'resultValue',
        name: '结果值',
        formula: 'value',
        unit,
        judgment: true,
        precision: 3,
      },
    ],
    judgmentMetric: 'resultValue',
  };
}

function singleValueParams(
  repeatCount: number,
  sampleSize: number,
): EntryRulePresetParams {
  return {
    positions: [{ code: 'P01', name: '实测', required: true, sort: 1 }],
    repeatCount,
    sampleSize,
    stdRule: DEFAULT_STD_RULE,
    displayLayout:
      repeatCount === 1 ? 'SEQUENCE_15_GRID' : 'POSITION_REPEAT_GRID',
    valueTemplate: 'SINGLE_VALUE',
    judgmentMetric: 'resultValue',
    dataRule: singleValueDataRule(),
  };
}

export const entryRulePresets: Record<EntryRulePresetKey, EntryRulePreset> = {
  SINGLE_VALUE_FIVE_REPEAT: {
    label: '单值实测：5 组',
    summary: '单值实测，5 组',
    params: singleValueParams(5, 5),
  },
  SINGLE_VALUE: {
    label: '单值实测模板',
    summary: '录入一个实测值，按结果值判定',
    params: singleValueParams(1, 1),
  },
  THREE_POINT: {
    label: '三点：L5 / L3 / 圆心0',
    summary: '按 L5/L3/圆心0 三点录入',
    params: {
      positions: [
        { code: 'L5', name: 'L5', required: true, sort: 1 },
        { code: 'L3', name: 'L3', required: true, sort: 2 },
        { code: 'C0', name: '圆心0', required: true, sort: 3 },
      ],
      repeatCount: 1,
      sampleSize: 3,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'THREE_POINT_GRID',
      valueTemplate: 'SINGLE_VALUE',
      judgmentMetric: 'resultValue',
      dataRule: singleValueDataRule(),
    },
  },
  THREE_POINT_FIVE_REPEAT: {
    label: '三点五组：L5 / L3 / 圆心0，每点5组',
    summary: '按 L5/L3/圆心0 三点，每点 5 组录入',
    params: {
      positions: [
        { code: 'L5', name: 'L5', required: true, sort: 1 },
        { code: 'L3', name: 'L3', required: true, sort: 2 },
        { code: 'C0', name: '圆心0', required: true, sort: 3 },
      ],
      repeatCount: 5,
      sampleSize: 15,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'THREE_POINT_FIVE_REPEAT_GRID',
      valueTemplate: 'SINGLE_VALUE',
      judgmentMetric: 'resultValue',
      dataRule: singleValueDataRule(),
    },
  },
  FIVE_POINT_THREE_REPEAT: {
    label: '五点三组：L5 / L3 / 圆心0 / R3 / R5，每点3组',
    summary: '按 L5/L3/圆心0/R3/R5 五点，每点 3 组录入',
    params: {
      positions: [
        { code: 'L5', name: 'L5', required: true, sort: 1 },
        { code: 'L3', name: 'L3', required: true, sort: 2 },
        { code: 'C0', name: '圆心0', required: true, sort: 3 },
        { code: 'R3', name: 'R3', required: true, sort: 4 },
        { code: 'R5', name: 'R5', required: true, sort: 5 },
      ],
      repeatCount: 3,
      sampleSize: 15,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'POSITION_REPEAT_GRID',
      valueTemplate: 'SINGLE_VALUE',
      judgmentMetric: 'resultValue',
      dataRule: singleValueDataRule(),
    },
  },
  SEQUENCE_15: {
    label: '序号：1-15',
    summary: '按 1-15 序号位置录入',
    params: {
      positions: Array.from({ length: 15 }).map((_, index) => ({
        code: `P${String(index + 1).padStart(2, '0')}`,
        name: String(index + 1),
        required: true,
        sort: index + 1,
      })),
      repeatCount: 1,
      sampleSize: 15,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'SEQUENCE_15_GRID',
      valueTemplate: 'SINGLE_VALUE',
      judgmentMetric: 'resultValue',
      dataRule: singleValueDataRule(),
    },
  },
  DENSITY_CALC: {
    label: '密度计算模板',
    summary:
      '按 L5/L3/圆心0/R3/R5 五点，每点 3 组录入厚度和质量，系统计算密度并按密度判定',
    params: {
      positions: [
        { code: 'L5', name: 'L5', required: true, sort: 1 },
        { code: 'L3', name: 'L3', required: true, sort: 2 },
        { code: 'C0', name: '圆心0', required: true, sort: 3 },
        { code: 'R3', name: 'R3', required: true, sort: 4 },
        { code: 'R5', name: 'R5', required: true, sort: 5 },
      ],
      repeatCount: 3,
      sampleSize: 15,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'POSITION_REPEAT_GRID',
      valueTemplate: 'DENSITY_CALC',
      judgmentMetric: 'densityValue',
      dataRule: {
        inputFields: [
          {
            code: 'thicknessMm',
            name: '厚度',
            unit: 'mm',
            type: 'NUMBER',
            required: true,
            precision: 3,
          },
          {
            code: 'weightG',
            name: '质量',
            unit: 'g',
            type: 'NUMBER',
            required: true,
            precision: 3,
          },
        ],
        resultFields: [
          {
            code: 'densityValue',
            name: '密度',
            formula: 'weightG / ((thicknessMm / 10) * 3.14 * 39 * 39 / 4)',
            unit: 'g/cm2',
            judgment: true,
            precision: 6,
          },
        ],
        judgmentMetric: 'densityValue',
      },
    },
  },
  COMPRESSION_CALC: {
    label: '压缩性能模板',
    summary: '录入 T1/T2/T3，自动计算压缩率/压缩弹性率并判定',
    params: {
      positions: [
        { code: 'L5', name: 'L5', required: true, sort: 1 },
        { code: 'L3', name: 'L3', required: true, sort: 2 },
        { code: 'C0', name: '圆心0', required: true, sort: 3 },
        { code: 'R3', name: 'R3', required: true, sort: 4 },
        { code: 'R5', name: 'R5', required: true, sort: 5 },
      ],
      repeatCount: 3,
      sampleSize: 15,
      stdRule: DEFAULT_STD_RULE,
      displayLayout: 'POSITION_REPEAT_GRID',
      valueTemplate: 'COMPRESSION_CALC',
      judgmentMetric: 'compressionRate',
      entryGroupCode: 'COMPRESSION_PERFORMANCE',
      entryGroupName: '压缩性能',
      dataRule: {
        inputFields: [
          {
            code: 't1Mm',
            name: 'T1',
            unit: 'mm',
            type: 'NUMBER',
            required: true,
            precision: 3,
          },
          {
            code: 't2Mm',
            name: 'T2',
            unit: 'mm',
            type: 'NUMBER',
            required: true,
            precision: 3,
          },
          {
            code: 't3Mm',
            name: 'T3',
            unit: 'mm',
            type: 'NUMBER',
            required: true,
            precision: 3,
          },
        ],
        resultFields: [
          {
            code: 'compressionRate',
            name: '压缩率',
            formula: '(t1Mm - t2Mm) / t1Mm * 100',
            unit: '%',
            judgment: true,
            precision: 3,
          },
          {
            code: 'compressionElasticityRate',
            name: '压缩弹性率',
            formula: '(t3Mm - t2Mm) / (t1Mm - t2Mm) * 100',
            unit: '%',
            judgment: false,
            precision: 3,
          },
        ],
        judgmentMetric: 'compressionRate',
      },
    },
  },
  QUALITATIVE_JUDGMENT: {
    label: '定性判定：OK / NG',
    summary: '定性 OK/NG，NG 必填备注',
    params: {
      displayLayout: 'QUALITATIVE_JUDGMENT',
      options: ['OK', 'NG'],
      ngRemarkRequired: true,
      sampleSize: 1,
    },
  },
};

const entryRulePresetOrder: EntryRulePresetKey[] = [
  DEFAULT_QUANTITATIVE_ENTRY_RULE_PRESET,
  'THREE_POINT',
  'THREE_POINT_FIVE_REPEAT',
  'FIVE_POINT_THREE_REPEAT',
  'SEQUENCE_15',
  'SINGLE_VALUE',
  'DENSITY_CALC',
  'COMPRESSION_CALC',
  DEFAULT_QUALITATIVE_ENTRY_RULE_PRESET,
];

export const entryRulePresetOptions = entryRulePresetOrder.map((value) => ({
  label: entryRulePresets[value].label,
  value,
}));

export function cloneEntryRulePresetParams(key: EntryRulePresetKey) {
  return JSON.parse(JSON.stringify(entryRulePresets[key].params));
}

export function getEntryRulePresetSummary(key: EntryRulePresetKey) {
  return entryRulePresets[key].summary;
}

export function isEntryRulePresetKey(
  value: string,
): value is EntryRulePresetKey {
  return Object.prototype.hasOwnProperty.call(entryRulePresets, value);
}

export function stringifyEntryRulePresetParams(key: EntryRulePresetKey) {
  return JSON.stringify(cloneEntryRulePresetParams(key));
}
