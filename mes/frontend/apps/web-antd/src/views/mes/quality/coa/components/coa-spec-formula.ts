export type CoaSpecFormulaKind =
  | 'ASYMMETRIC_TOLERANCE'
  | 'EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'IN_LIST'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'NOT_IN_LIST'
  | 'RANGE'
  | 'SYMMETRIC_TOLERANCE';

export interface CoaSpecFormulaResult {
  allowedValues?: string[];
  formula: string;
  kind?: CoaSpecFormulaKind;
  lowerLimit?: number;
  message?: string;
  targetValue?: number;
  upperLimit?: number;
  valid: boolean;
}

const NUMBER = '(-?\\d+(?:\\.\\d+)?)';
const POSITIVE_NUMBER = '(\\d+(?:\\.\\d+)?)';
const RANGE_PATTERN = new RegExp(`^${NUMBER}~${NUMBER}$`);
const TOLERANCE_PATTERN = new RegExp(`^${NUMBER}±${POSITIVE_NUMBER}$`);
const ASYMMETRIC_TOLERANCE_PATTERN = new RegExp(`^${NUMBER}\\+${POSITIVE_NUMBER}/-${POSITIVE_NUMBER}$`);
const GREATER_OR_EQUAL_PATTERN = new RegExp(`^≥${NUMBER}$`);
const GREATER_PATTERN = new RegExp(`^>${NUMBER}$`);
const LESS_OR_EQUAL_PATTERN = new RegExp(`^≤${NUMBER}$`);
const LESS_PATTERN = new RegExp(`^<${NUMBER}$`);
const EQUALS_PATTERN = new RegExp(`^=${NUMBER}$`);
const NOT_IN_PATTERN = /^NOTIN\((.+)\)$/i;
const IN_PATTERN = /^IN\((.+)\)$/i;

export function normalizeCoaSpecFormula(value?: string) {
  return (value || '')
    .replace(/\s+/g, '')
    .replaceAll('～', '~')
    .replaceAll('至', '~')
    .replaceAll('—', '~')
    .replaceAll('–', '~')
    .replaceAll('+/-', '±')
    .replace(/not\s*in/gi, 'NOTIN')
    .replaceAll('>=', '≥')
    .replaceAll('<=', '≤');
}

export function isCoaSpecFormula(value?: string) {
  return /[~～至—–±]|>=|<=|[<>=]|(?:not\s*)?in\(/i.test(value || '');
}

function parseAllowedValues(value: string) {
  const values = value
    .split(/[,，|]/)
    .map((item) => item.trim())
    .filter(Boolean);
  return values.length > 0 ? values : undefined;
}

export function parseCoaSpecFormula(value?: string): CoaSpecFormulaResult {
  const formula = normalizeCoaSpecFormula(value);
  if (!formula) {
    return { formula, message: '请输入判定公式', valid: false };
  }
  const range = formula.match(RANGE_PATTERN);
  if (range) {
    const lowerLimit = Number(range[1]);
    const upperLimit = Number(range[2]);
    if (lowerLimit > upperLimit) {
      return { formula, message: '区间下限不能大于上限', valid: false };
    }
    return { formula, kind: 'RANGE', lowerLimit, upperLimit, valid: true };
  }
  const tolerance = formula.match(TOLERANCE_PATTERN);
  if (tolerance) {
    const targetValue = Number(tolerance[1]);
    const toleranceValue = Number(tolerance[2]);
    return {
      formula,
      kind: 'SYMMETRIC_TOLERANCE',
      lowerLimit: targetValue - toleranceValue,
      targetValue,
      upperLimit: targetValue + toleranceValue,
      valid: true,
    };
  }
  const asymmetricTolerance = formula.match(ASYMMETRIC_TOLERANCE_PATTERN);
  if (asymmetricTolerance) {
    const targetValue = Number(asymmetricTolerance[1]);
    const upperTolerance = Number(asymmetricTolerance[2]);
    const lowerTolerance = Number(asymmetricTolerance[3]);
    return {
      formula,
      kind: 'ASYMMETRIC_TOLERANCE',
      lowerLimit: targetValue - lowerTolerance,
      targetValue,
      upperLimit: targetValue + upperTolerance,
      valid: true,
    };
  }
  const greaterOrEqual = formula.match(GREATER_OR_EQUAL_PATTERN);
  if (greaterOrEqual) {
    return { formula, kind: 'GREATER_THAN_OR_EQUAL', lowerLimit: Number(greaterOrEqual[1]), valid: true };
  }
  const greater = formula.match(GREATER_PATTERN);
  if (greater) {
    return { formula, kind: 'GREATER_THAN', lowerLimit: Number(greater[1]), valid: true };
  }
  const lessOrEqual = formula.match(LESS_OR_EQUAL_PATTERN);
  if (lessOrEqual) {
    return { formula, kind: 'LESS_THAN_OR_EQUAL', upperLimit: Number(lessOrEqual[1]), valid: true };
  }
  const less = formula.match(LESS_PATTERN);
  if (less) {
    return { formula, kind: 'LESS_THAN', upperLimit: Number(less[1]), valid: true };
  }
  const equals = formula.match(EQUALS_PATTERN);
  if (equals) {
    const targetValue = Number(equals[1]);
    return {
      formula,
      kind: 'EQUALS',
      lowerLimit: targetValue,
      targetValue,
      upperLimit: targetValue,
      valid: true,
    };
  }
  const notIn = formula.match(NOT_IN_PATTERN);
  if (notIn) {
    const allowedValues = parseAllowedValues(notIn[1]);
    return allowedValues
      ? { allowedValues, formula, kind: 'NOT_IN_LIST', valid: true }
      : { formula, message: '排除值集合不能为空', valid: false };
  }
  const inList = formula.match(IN_PATTERN);
  if (inList) {
    const allowedValues = parseAllowedValues(inList[1]);
    return allowedValues
      ? { allowedValues, formula, kind: 'IN_LIST', valid: true }
      : { formula, message: '允许值集合不能为空', valid: false };
  }
  return {
    formula,
    message: '支持：39.7~55.7、50±2、50+3/-2、≥39.7、>39.7、≤55.7、<55.7、=50、IN(合格,OK)、NOT IN(NG,报废)',
    valid: false,
  };
}

function isAllowedValue(actualValue: string, allowedValues: string[]) {
  const actualNumber = Number(actualValue);
  return allowedValues.some((allowedValue) => {
    const allowedNumber = Number(allowedValue);
    if (!Number.isNaN(actualNumber) && !Number.isNaN(allowedNumber)) {
      return actualNumber === allowedNumber;
    }
    return actualValue.trim().toLocaleLowerCase() === allowedValue.trim().toLocaleLowerCase();
  });
}

export function evaluateCoaSpecFormula(
  formula: string | undefined,
  actualValue: string | number | undefined,
) {
  const rule = parseCoaSpecFormula(formula);
  if (!rule.valid) return { ...rule, passed: undefined };
  if (actualValue === undefined || actualValue === null || String(actualValue).trim() === '') {
    return { ...rule, message: '请输入试算实际值', passed: undefined, valid: false };
  }
  const actualText = String(actualValue).trim();
  if (rule.kind === 'IN_LIST' || rule.kind === 'NOT_IN_LIST') {
    const matched = isAllowedValue(actualText, rule.allowedValues || []);
    return { ...rule, passed: rule.kind === 'IN_LIST' ? matched : !matched };
  }
  const actualNumber = Number(actualText);
  if (Number.isNaN(actualNumber)) {
    return { ...rule, message: '该公式需要数值型实际值', passed: undefined, valid: false };
  }
  const passed =
    rule.kind === 'GREATER_THAN'
      ? actualNumber > (rule.lowerLimit as number)
      : rule.kind === 'LESS_THAN'
        ? actualNumber < (rule.upperLimit as number)
        : (rule.lowerLimit === undefined || actualNumber >= rule.lowerLimit) &&
          (rule.upperLimit === undefined || actualNumber <= rule.upperLimit);
  return { ...rule, passed };
}

export function getCoaSpecFormulaKindLabel(kind?: CoaSpecFormulaKind) {
  const labels: Partial<Record<CoaSpecFormulaKind, string>> = {
    ASYMMETRIC_TOLERANCE: '非对称公差',
    EQUALS: '精确值',
    GREATER_THAN: '严格下限',
    GREATER_THAN_OR_EQUAL: '不小于',
    IN_LIST: '允许值集合',
    LESS_THAN: '严格上限',
    LESS_THAN_OR_EQUAL: '不大于',
    NOT_IN_LIST: '排除值集合',
    RANGE: '区间',
    SYMMETRIC_TOLERANCE: '目标±公差',
  };
  return kind ? labels[kind] || '判定公式' : '判定公式';
}
