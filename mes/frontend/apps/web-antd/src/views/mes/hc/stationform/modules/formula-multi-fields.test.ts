import { describe, expect, it } from 'vitest';
import { multiFields, readFormulaValue, writeFormulaValue, multiFieldError } from './formula-multi-fields';
const definitions = [
  { key: 'weight', label: '重量', type: 'NUMBER', unit: 'kg', required: true },
  { key: 'batch', label: '批号', type: 'TEXT', unit: '', required: true },
  { key: 'temperature', label: '温度', type: 'NUMBER', unit: '℃', required: true },
  { key: 'solids', label: '固含量', type: 'NUMBER', unit: '%', required: false },
  { key: 'note', label: '备注说明', type: 'TEXT', unit: '', required: false },
];
function item() { return { valueMode: 'MULTI_FIELDS', fieldDefinitionsJson: JSON.stringify(definitions), fieldValuesJson: '{}' }; }
describe('配料多字段值绑定', () => {
  it('五字段保存序列化后保留零、前导零、中文和小数精度', () => {
    const row = item();
    const values = ['0', '00123', '25.50', '45', '无异常'];
    multiFields(row).forEach((field, i) => writeFormulaValue(row, `multi:${field.key}`, values[i]));
    const reloaded = JSON.parse(JSON.stringify(row));
    expect(multiFields(reloaded).map((f) => readFormulaValue(reloaded, `multi:${f.key}`))).toEqual(values);
    expect(multiFieldError(row, true)).toBe('');
    row.fieldDefinitionsJson = JSON.stringify([...definitions].reverse().map((f) => ({ ...f, label: `修改${f.label}` })));
    expect(readFormulaValue(row, 'multi:batch')).toBe('00123');
    expect(readFormulaValue(row, 'multi:temperature')).toBe('25.50');
  });
  it('草稿允许未填，完成逐字段校验，数字错误不能保存', () => {
    const row = item();
    expect(multiFieldError(row, false)).toBe('');
    expect(multiFieldError(row, true)).toContain('重量');
    writeFormulaValue(row, 'multi:weight', 'abc');
    expect(multiFieldError(row, false)).toContain('必须填写数字');
    writeFormulaValue(row, 'multi:weight', '0');
    expect(multiFieldError(row, true)).toContain('批号');
  });
  it('Excel稳定字段绑定可逐项回填，不接收未知字段', () => {
    const row = item();
    const cells = definitions.map((f, i) => ({ bindField: `multi:${f.key}`, value: String(i) }));
    cells.forEach((cell) => writeFormulaValue(row, cell.bindField, cell.value));
    expect(JSON.parse(row.fieldValuesJson)).toEqual({ weight: '0', batch: '1', temperature: '2', solids: '3', note: '4' });
    expect(() => writeFormulaValue(row, 'multi:missing', '9')).toThrow();
  });
  it('原单值双值读写保持兼容', () => {
    const row = { actualValue: '12', actualValue2: '00123' };
    writeFormulaValue(row, 'actualValue', '0');
    expect(readFormulaValue(row, 'actualValue')).toBe('0');
    expect(readFormulaValue(row, 'actualValue2')).toBe('00123');
  });
});
