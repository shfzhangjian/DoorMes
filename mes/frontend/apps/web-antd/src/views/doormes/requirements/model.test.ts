import { describe, expect, it } from 'vitest';
import { newRequirementInput, newRequirementLine, validateRequirementInput } from './model';

function valid() {
  const input = newRequirementInput();
  input.number = 'REQ-001'; input.customer = '测试客户';
  return input;
}
describe('actual MES requirement input', () => {
  it('allows incomplete material fields in drafts but not submissions', () => {
    expect(validateRequirementInput(valid())).toBeUndefined();
    expect(validateRequirementInput(valid(), true)).toContain('补齐');
  });
  it('submits complete business material requirements', () => {
    const input = valid();
    Object.assign(input.lines[0]!.requirement, { material:'AL70',glass:'GL24',hardware:'HW-TT',finish:'RAL7016',dueDate:'2026-10-15' });
    expect(validateRequirementInput(input, true)).toBeUndefined();
  });
  it('rejects duplicate short marks ignoring case', () => {
    const input = valid(); input.lines.push({...newRequirementLine(2),mark:'c1'});
    expect(validateRequirementInput(input)).toContain('重复');
  });
  it('rejects missing, non-finite and out-of-range geometry', () => {
    for (const width of [0,50001,NaN,Infinity]) {
      const input = valid(); input.lines[0]!.requirement.widthMm = width;
      expect(validateRequirementInput(input)).toContain('尺寸');
    }
  });
  it('rejects fractional and excessive quantities', () => {
    for (const quantity of [0,1.5,1001]) {
      const input = valid(); input.lines[0]!.quantity = quantity;
      expect(validateRequirementInput(input)).toContain('数量');
    }
  });
  it('rejects calendar rollover dates', () => {
    for (const dueDate of ['2026-02-30','2026-13-01','2026-1-1']) {
      const input = valid(); input.lines[0]!.requirement.dueDate = dueDate;
      expect(validateRequirementInput(input)).toContain('日期');
    }
  });
  it('does not fabricate a published standard reference', () => {
    const input = valid(); input.lines[0]!.kind = 'standard';
    expect(validateRequirementInput(input)).toContain('已发布');
  });
  it('creates independent drafts rather than shared mutable defaults', () => {
    const first = valid(), second = valid(); first.lines[0]!.requirement.widthMm = 800;
    expect(second.lines[0]!.requirement.widthMm).toBe(1200);
    expect(second.lines[0]!.id).toBeUndefined();
  });
});
