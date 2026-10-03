import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { productionRecordGroups } from './production-record-columns';

const pages: [string, string][] = [
  ['FORMULA_PRODUCTION_RECORD', '../production-record/formula/index.vue'],
  [
    'WET_PRODUCTION_RECORD',
    '../../execution/report/wet-production-record/index.vue',
  ],
  [
    'GRINDING_PRODUCTION_RECORD',
    '../production-record/rough-grinding/index.vue',
  ],
  [
    'ADHESIVE1_PRODUCTION_RECORD',
    '../production-record/adhesive/components/AdhesiveProductionRecordPage.vue',
  ],
  [
    'ADHESIVE2_PRODUCTION_RECORD',
    '../production-record/adhesive/components/AdhesiveProductionRecordPage.vue',
  ],
  [
    'SLITTING_PRESS_PRODUCTION_RECORD',
    '../production-record/slitting-press/index.vue',
  ],
  ['CUT_ROUND_PRODUCTION_RECORD', '../production-record/index.vue'],
];
function group(type: string, data: Record<string, unknown>) {
  return productionRecordGroups([
    { sourceType: type, productionData: data },
  ])[0]!;
}
function cell(type: string, data: Record<string, unknown>, key: string) {
  const result = group(type, data);
  return result.columns
    .find((column) => column.key === key)!
    .customRender({ record: result.rows[0]! });
}
describe('生产记录完整展示契约', () => {
  it.each(pages)('%s 表头逐列对齐原生产记录页面', (type, path) => {
    const source = readFileSync(new URL(path, import.meta.url), 'utf8');
    const header = source.match(/<thead>([\s\S]*?)<\/thead>/)![1]!;
    const expected = [...header.matchAll(/<th\b[^>]*>([\s\S]*?)<\/th>/g)]
      .map((match) => match[1]!.trim())
      .filter((title) => title !== '操作' && !title.includes('<Checkbox'))
      .map((title) =>
        title
          .replace(
            '{{ config.inputLabel }}',
            type.startsWith('ADHESIVE2') ? '粘胶2投入(pcs)' : '投入米数(m)',
          )
          .replace(
            '{{ config.outputLabel }}',
            type.startsWith('ADHESIVE2') ? '粘胶2产出(pcs)' : '产出米数(m)',
          ),
      );
    expect(group(type, {}).columns.map((column) => column.title)).toEqual(
      expected,
    );
  });
  it('磨皮同源拆分记录保留新旧砂纸标识', () => {
    const result = productionRecordGroups(
      [1, 2].map((segment) => ({
        id: segment,
        sourceType: 'GRINDING_PRODUCTION_RECORD',
        productionData: {
          sourceDetailId: 7,
          sourceBizType: 'SECOND',
          sourceSegmentNo: segment,
          sandpaperBatchNo: 'SAND',
        },
      })),
    )[0]!;
    const column = result.columns.find(
      (item) => item.key === 'sandpaperBatchNo',
    )!;
    expect(column.customRender({ record: result.rows[0]! })).toBe(
      'SAND（旧砂纸）',
    );
    expect(column.customRender({ record: result.rows[1]! })).toBe(
      'SAND（新砂纸）',
    );
  });
  it('实际产出、合格产出和零值保持来源服务口径', () => {
    const data = {
      pressSlotActualOutputPcs: 100,
      pressSlotOutputPcs: 95,
      slittingNgPcs: 0,
    };
    expect(
      cell(
        'SLITTING_PRESS_PRODUCTION_RECORD',
        data,
        'pressSlotActualOutputPcs',
      ),
    ).toBe('100');
    expect(
      cell('SLITTING_PRESS_PRODUCTION_RECORD', data, 'pressSlotOutputPcs'),
    ).toBe('95');
    expect(
      cell('SLITTING_PRESS_PRODUCTION_RECORD', data, 'slittingNgPcs'),
    ).toBe('0');
  });
  it('完工时间保留秒，配料日期仍取业务日期，来源显示原业务文案', () => {
    const data = {
      recordTime: '2026-09-08 19:10:18',
      reportDate: '2026-09-07',
      recordSource: '报工生成',
    };
    expect(cell('ADHESIVE1_PRODUCTION_RECORD', data, 'completion')).toBe(
      '2026-09-08 19:10:18',
    );
    expect(cell('FORMULA_PRODUCTION_RECORD', data, 'reportDate')).toBe(
      '2026-09-07',
    );
    expect(cell('ADHESIVE1_PRODUCTION_RECORD', data, 'recordSource')).toBe(
      '报工生成',
    );
  });
  it('原服务修订字段优先于通用字段，旧响应快照仍可读取', () => {
    const result = productionRecordGroups([
      {
        sourceType: 'FORMULA_PRODUCTION_RECORD',
        modelCode: 'OLD',
        productionData: { modelCode: 'REVISED', filterBatchNo: 'FILTER-A' },
        detailJson: '{"modelCode":"STALE"}',
      },
    ])[0]!;
    expect(result.rows[0]!.modelCode).toBe('REVISED');
    const legacy = productionRecordGroups([
      {
        sourceType: 'WET_PRODUCTION_RECORD',
        detailJson: '{"petBatchNo":"PET-01"}',
      },
    ])[0]!;
    expect(legacy.rows[0]!.petBatchNo).toBe('PET-01');
    expect(() =>
      productionRecordGroups([
        { sourceType: 'WET_PRODUCTION_RECORD', detailJson: 'invalid' },
      ]),
    ).not.toThrow();
  });
});
