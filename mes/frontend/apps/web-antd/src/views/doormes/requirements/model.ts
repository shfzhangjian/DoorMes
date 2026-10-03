import type { RequirementInput, RequirementLine } from '../../../api/doormes/requirement-contract';
import { DEMAND_SCHEMA } from '../../../api/doormes/requirement-contract';

export function newRequirementLine(index: number): RequirementLine {
  return { mark: `C${index}`, kind: 'custom', quantity: 1, requirement: {
    widthMm: 1200, heightMm: 1500, material: '', glass: '', hardware: '',
    finish: '', dueDate: '', note: '',
  } };
}
export function newRequirementInput(): RequirementInput {
  return { schemaVersion: DEMAND_SCHEMA, number: '', customer: '', project: '', lines: [newRequirementLine(1)], note: '' };
}
export function validateRequirementInput(input: RequirementInput, submitting = false): string | undefined {
  const code = /^[A-Za-z0-9][A-Za-z0-9._-]{0,39}$/;
  if (input.schemaVersion !== DEMAND_SCHEMA || !code.test(input.number)) return '需求单号须为 1–40 位字母、数字、点、下划线或短横线。';
  if (!input.customer.trim() || input.customer.length > 200) return '请填写客户名称（最多 200 字）。';
  if (input.project.length > 200 || input.note.length > 2000) return '项目或备注内容过长。';
  if (!input.lines.length || input.lines.length > 100) return '需求明细须为 1–100 项。';
  const marks = new Set<string>();
  for (const line of input.lines) {
    const r = line.requirement;
    if (!code.test(line.mark) || marks.has(line.mark.toUpperCase())) return '门窗短编号无效或重复。';
    marks.add(line.mark.toUpperCase());
    if (line.kind !== 'custom') return '标准设计需引用已发布目录版本，当前目录接口尚在接入。';
    if (!Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > 1000) return '每项数量须为 1–1000 的整数。';
    if (![r.widthMm, r.heightMm].every((value) => Number.isFinite(value) && value >= 1 && value <= 50000)) return '尺寸须为 1–50000 mm。';
    if ([r.material,r.glass,r.hardware,r.finish].some((text) => text.length > 100) || r.note.length > 2000) return '材料要求或明细备注过长。';
    if (r.dueDate && (!/^\d{4}-\d{2}-\d{2}$/.test(r.dueDate) || Number.isNaN(Date.parse(r.dueDate)) || new Date(r.dueDate).toISOString().slice(0,10) !== r.dueDate)) return '需求日期无效。';
    if (submitting && [r.material,r.glass,r.hardware,r.finish,r.dueDate].some((text) => !text.trim())) return '提交前请补齐每项型材、玻璃、五金、表面颜色和需求日期。';
  }
}
