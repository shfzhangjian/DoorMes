import type { SrmCrudOption, SrmCrudRecord } from '../../shared/crud';

import dayjs from 'dayjs';

export type ComplianceFileType =
  | 'COMPLIANCE_AGREEMENT'
  | 'ENVIRONMENT_CERT'
  | 'SYSTEM_CERT';

export type ComplianceDictOption = SrmCrudOption & {
  parentType?: string;
};

export const COMPLIANCE_FILE_TYPE_DICT = 'mes_srm_compliance_file_type';
export const COMPLIANCE_FILE_NAME_DICT = 'mes_srm_compliance_file_name';
export const COMPLIANCE_ATTACHMENT_CATEGORY_DICT =
  'mes_srm_compliance_attachment_category';

export const FILE_TYPE_SYSTEM_CERT = 'SYSTEM_CERT';
export const FILE_TYPE_COMPLIANCE_AGREEMENT = 'COMPLIANCE_AGREEMENT';
export const FILE_TYPE_ENVIRONMENT_CERT = 'ENVIRONMENT_CERT';
export const COMPLIANCE_WARNING_DAYS = 31;

export const fallbackFileTypeOptions: ComplianceDictOption[] = [
  { label: '体系证书', value: FILE_TYPE_SYSTEM_CERT },
  { label: '合规协议', value: FILE_TYPE_COMPLIANCE_AGREEMENT },
  { label: '环保资料', value: FILE_TYPE_ENVIRONMENT_CERT },
];

export const fallbackFileNameOptions: ComplianceDictOption[] = [
  {
    label: 'IATF 16949',
    parentType: FILE_TYPE_SYSTEM_CERT,
    value: 'IATF 16949',
  },
  { label: 'ISO 14001', parentType: FILE_TYPE_SYSTEM_CERT, value: 'ISO 14001' },
  { label: 'ISO 45001', parentType: FILE_TYPE_SYSTEM_CERT, value: 'ISO 45001' },
  { label: 'ISO 9001', parentType: FILE_TYPE_SYSTEM_CERT, value: 'ISO 9001' },
  {
    label: '《供应商社会责任承诺书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《供应商社会责任承诺书》',
  },
  {
    label: '《无冲突矿物金属宣告书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《无冲突矿物金属宣告书》',
  },
  {
    label: '《供应商不使用对环境有害物质承诺书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《供应商不使用对环境有害物质承诺书》',
  },
  {
    label: '《环保协议书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《环保协议书》',
  },
  {
    label: '《供应商质量保证协议书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《供应商质量保证协议书》',
  },
  {
    label: '《廉洁协议书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《廉洁协议书》',
  },
  {
    label: '《保密协议》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《保密协议》',
  },
  {
    label: '《采购框架协议书》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《采购框架协议书》',
  },
  {
    label: '《采购合同知识产权补充协议》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《采购合同知识产权补充协议》',
  },
  {
    label: '《物料PCN协议》',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《物料PCN协议》',
  },
  {
    label: '《产品化学物质控制标准》文件接收确认表',
    parentType: FILE_TYPE_COMPLIANCE_AGREEMENT,
    value: '《产品化学物质控制标准》文件接收确认表',
  },
  { label: '卤素', parentType: FILE_TYPE_ENVIRONMENT_CERT, value: '卤素' },
  { label: 'ROHS', parentType: FILE_TYPE_ENVIRONMENT_CERT, value: 'ROHS' },
  { label: 'REACH', parentType: FILE_TYPE_ENVIRONMENT_CERT, value: 'REACH' },
  { label: 'MSDS', parentType: FILE_TYPE_ENVIRONMENT_CERT, value: 'MSDS' },
  {
    label: 'MSDS/TDS',
    parentType: FILE_TYPE_ENVIRONMENT_CERT,
    value: 'MSDS/TDS',
  },
];

export const fileStatusOptions: SrmCrudOption[] = [
  { label: '有效', value: 'VALID' },
  { label: '临期预警', value: 'WARNING' },
  { label: '已过期', value: 'EXPIRED' },
];

export const standardCompliantOptions: SrmCrudOption[] = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' },
];

const autoTwelveMonthEnvironmentNames = new Set(['REACH', 'ROHS', '卤素']);
const autoSixtyMonthEnvironmentNames = new Set(['MSDS', 'MSDS/TDS']);

const fileNameLabelMap: Record<string, string> = {
  [FILE_TYPE_COMPLIANCE_AGREEMENT]: '协议类型',
  [FILE_TYPE_ENVIRONMENT_CERT]: '报告类型',
  [FILE_TYPE_SYSTEM_CERT]: '证书类型',
};

const effectDateLabelMap: Record<string, string> = {
  [FILE_TYPE_COMPLIANCE_AGREEMENT]: '协议生效日期',
  [FILE_TYPE_ENVIRONMENT_CERT]: '报告生成日期',
  [FILE_TYPE_SYSTEM_CERT]: '报告生成日期',
};

const expiryDateLabelMap: Record<string, string> = {
  [FILE_TYPE_COMPLIANCE_AGREEMENT]: '协议失效日期',
  [FILE_TYPE_ENVIRONMENT_CERT]: '报告过期日期',
  [FILE_TYPE_SYSTEM_CERT]: '报告过期日期',
};

export function getFileNameOptions(
  fileType: unknown,
  fileNameOptions: ComplianceDictOption[],
) {
  const parentType = String(fileType || '');
  if (!parentType) {
    return [];
  }
  return fileNameOptions.filter((option) => option.parentType === parentType);
}

export function getFileNameFieldLabel(record: SrmCrudRecord) {
  return fileNameLabelMap[String(record.fileType || '')] || '档案名称';
}

export function getEffectDateFieldLabel(record: SrmCrudRecord) {
  return effectDateLabelMap[String(record.fileType || '')] || '生效/生成日期';
}

export function getExpiryDateFieldLabel(record: SrmCrudRecord) {
  return expiryDateLabelMap[String(record.fileType || '')] || '失效/过期日期';
}

export function isSystemCertRecord(record: SrmCrudRecord) {
  return String(record.fileType || '') === FILE_TYPE_SYSTEM_CERT;
}

export function isComplianceAgreementRecord(record: SrmCrudRecord) {
  return String(record.fileType || '') === FILE_TYPE_COMPLIANCE_AGREEMENT;
}

export function isEnvironmentCertRecord(record: SrmCrudRecord) {
  return String(record.fileType || '') === FILE_TYPE_ENVIRONMENT_CERT;
}

export function isReportCodeVisible(record: SrmCrudRecord) {
  return isSystemCertRecord(record) || isEnvironmentCertRecord(record);
}

export function isAutoExpiryRecord(record: SrmCrudRecord) {
  return resolveExpiryRule(record).validityMonths !== undefined;
}

export function refreshComplianceRecord(record: SrmCrudRecord) {
  clearUnusedFields(record);
  const rule = resolveExpiryRule(record);
  record.validityMonths = rule.validityMonths;
  record.expiryRule = rule.expiryRule;
  const effectDate = normalizeDateText(record.effectDate);
  if (rule.validityMonths && effectDate) {
    record.expiryDate = dayjs(effectDate)
      .add(rule.validityMonths, 'month')
      .subtract(1, 'day')
      .format('YYYY-MM-DD');
  } else if (rule.validityMonths) {
    record.expiryDate = undefined;
  }
  record.warningDays = COMPLIANCE_WARNING_DAYS;
  refreshStatus(record);
  return record;
}

export function prepareComplianceRecordForSave(record: SrmCrudRecord) {
  const normalized = refreshComplianceRecord({ ...record });
  normalized.standardCompliant = normalizeStandardCompliant(
    normalized.standardCompliant,
  );
  normalized.payloadJson = JSON.stringify(
    compactPayload({
      expiryRule: normalized.expiryRule,
      inspectionAgency: normalized.inspectionAgency,
      productModel: normalized.productModel,
      providedProduct: normalized.providedProduct,
      reportCode: normalized.reportCode,
      standardCompliant: normalized.standardCompliant,
      validityMonths: normalized.validityMonths,
    }),
  );
  return normalized;
}

export function buildComplianceValueLabels(
  fileTypeOptions: ComplianceDictOption[],
  fileNameOptions: ComplianceDictOption[],
  attachmentCategoryOptions: ComplianceDictOption[],
) {
  return {
    EXPIRED: '已过期',
    N: '否',
    NO: '否',
    VALID: '有效',
    WARNING: '临期预警',
    Y: '是',
    YES: '是',
    不合格: '否',
    不符合: '否',
    否: '否',
    合格: '是',
    符合: '是',
    是: '是',
    ...toValueLabelMap(fileTypeOptions),
    ...toValueLabelMap(fileNameOptions),
    ...toValueLabelMap(attachmentCategoryOptions),
  };
}

export function getFileTypeLabel(
  fileTypeOptions: ComplianceDictOption[],
  value?: unknown,
) {
  const stringValue = String(value || '');
  return (
    fileTypeOptions.find((option) => option.value === stringValue)?.label ||
    stringValue
  );
}

export function getFileNameLabel(
  fileNameOptions: ComplianceDictOption[],
  value?: unknown,
) {
  const stringValue = String(value || '');
  return (
    fileNameOptions.find((option) => option.value === stringValue)?.label ||
    stringValue
  );
}

function resolveExpiryRule(record: SrmCrudRecord) {
  const fileType = String(record.fileType || '');
  const fileName = String(record.fileName || '');
  if (fileType === FILE_TYPE_COMPLIANCE_AGREEMENT) {
    return {
      expiryRule: 'AUTO_12M_MINUS_1D',
      validityMonths: 12,
    };
  }
  if (
    fileType === FILE_TYPE_ENVIRONMENT_CERT &&
    autoTwelveMonthEnvironmentNames.has(fileName)
  ) {
    return {
      expiryRule: 'AUTO_12M_MINUS_1D',
      validityMonths: 12,
    };
  }
  if (
    fileType === FILE_TYPE_ENVIRONMENT_CERT &&
    autoSixtyMonthEnvironmentNames.has(fileName)
  ) {
    return {
      expiryRule: 'AUTO_60M_MINUS_1D',
      validityMonths: 60,
    };
  }
  return {
    expiryRule: 'MANUAL',
    validityMonths: undefined,
  };
}

function refreshStatus(record: SrmCrudRecord) {
  const expiryDate = normalizeDateText(record.expiryDate);
  if (!expiryDate) {
    record.daysLeft = undefined;
    record.fileStatus = 'VALID';
    return;
  }
  const daysLeft = dayjs(expiryDate)
    .startOf('day')
    .diff(dayjs().startOf('day'), 'day');
  record.daysLeft = daysLeft;
  if (daysLeft < 0) {
    record.fileStatus = 'EXPIRED';
  } else if (daysLeft <= COMPLIANCE_WARNING_DAYS) {
    record.fileStatus = 'WARNING';
  } else {
    record.fileStatus = 'VALID';
  }
}

function clearUnusedFields(record: SrmCrudRecord) {
  if (isComplianceAgreementRecord(record)) {
    record.inspectionAgency = undefined;
    record.productModel = undefined;
    record.reportCode = undefined;
    record.standardCompliant = undefined;
    return;
  }
  if (isSystemCertRecord(record)) {
    record.inspectionAgency = undefined;
    record.productModel = undefined;
    record.standardCompliant = undefined;
  }
}

function normalizeDateText(value: unknown) {
  const text = String(value || '').trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return '';
  }
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD') : '';
}

function compactPayload(payload: SrmCrudRecord) {
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([, value]) => value !== undefined && value !== null && value !== '',
    ),
  );
}

function normalizeStandardCompliant(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text) {
    return undefined;
  }
  const upperText = text.toUpperCase();
  if (
    ['1', 'TRUE', 'Y', 'YES'].includes(upperText) ||
    ['合格', '是', '符合'].includes(text)
  ) {
    return 'Y';
  }
  if (
    ['0', 'FALSE', 'N', 'NO'].includes(upperText) ||
    ['不合格', '不符合', '否'].includes(text)
  ) {
    return 'N';
  }
  return text;
}

function toValueLabelMap(options: ComplianceDictOption[]) {
  return Object.fromEntries(
    options.map((option) => [String(option.value), option.label]),
  );
}
