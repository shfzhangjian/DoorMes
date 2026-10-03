import type { MesFaiApi } from './index';

export const FAI_VALUE_TEMPLATE_OPTIONS = [
  { value: 'SINGLE_VALUE', label: '单值实测' },
  { value: 'DENSITY_CALC', label: '密度计算' },
  { value: 'COMPRESSION_CALC', label: '压缩性能' },
];

export const FAI_JUDGMENT_METRIC_OPTIONS = [
  { value: 'RESULT_VALUE', label: '实测结果值' },
  { value: 'DENSITY_VALUE', label: '密度值' },
  { value: 'COMPRESSION_RATE', label: '压缩率' },
  { value: 'COMPRESSION_ELASTICITY_RATE', label: '压缩弹性率' },
];

export type FaiSheetSection = NonNullable<
  MesFaiApi.FaiSheetTemplate['sections']
>[number];

export function getFaiValueTemplateName(value?: string) {
  if (value === 'DENSITY_CALC') return '密度计算模板';
  if (value === 'COMPRESSION_CALC') return '压缩性能计算模板';
  return '单值实测模板';
}

export function getFaiJudgmentMetricName(value?: string) {
  if (value === 'DENSITY_VALUE') return '密度值';
  if (value === 'COMPRESSION_RATE') return '压缩率';
  if (value === 'COMPRESSION_ELASTICITY_RATE') return '压缩弹性率';
  return '实测结果值';
}

export function findFaiSheetSection(
  item?: MesFaiApi.FaiItem | null,
  template?: MesFaiApi.FaiSheetTemplate,
) {
  if (!item) return undefined;
  return (template?.sections || []).find(
    (section) => section.sectionCode === item.sheetSectionCode,
  );
}

export function resolveFaiExpectedSampleCount(
  item?: MesFaiApi.FaiItem | null,
  template?: MesFaiApi.FaiSheetTemplate,
) {
  if (!item) return 1;
  if (item.itemType === 'QUALITATIVE') return 1;
  const section = findFaiSheetSection(item, template);
  if (!section) return Math.max(Number(item.sampleSize || 1), 1);

  const expectedRows = Math.max(Number(section.expectedRows || 0), 1);
  const expectedColumns = Math.max(Number(section.expectedColumns || 1), 1);
  return section.sectionType === 'GRID_SAMPLE'
    ? expectedRows * expectedColumns
    : expectedRows;
}

export function buildFaiGridPosition(index: number, section?: FaiSheetSection) {
  if (section?.sectionType !== 'GRID_SAMPLE') {
    return { sampleGroupNo: index + 1, sampleColumnNo: undefined };
  }
  const expectedColumns = Math.max(Number(section.expectedColumns || 1), 1);
  return {
    sampleGroupNo: Math.floor(index / expectedColumns) + 1,
    sampleColumnNo: (index % expectedColumns) + 1,
  };
}
