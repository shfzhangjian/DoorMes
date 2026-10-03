import type { SrmCrudRecord } from './crud';

import {
  createDocument,
  deleteDocument,
  getDocument,
  getDocumentPage,
  updateDocument,
  type SrmDocumentApi,
} from '#/api/mes/srm/document';

interface SrmDocumentCrudOptions {
  bizType: string;
  codeField: string;
  createDefaults?: (() => SrmCrudRecord) | SrmCrudRecord;
  docNoPrefix?: string;
  defaultStatus?: string | number;
  titleField?: string;
}

export function createSrmDocumentCrud(options: SrmDocumentCrudOptions) {
  return {
    attachmentBizType: options.bizType,
    codeField: options.codeField,
    createDefaults: () => {
      const defaults = resolveDefaults(options.createDefaults);
      return {
        ...defaults,
        [options.codeField]:
          defaults[options.codeField] ||
          buildSrmDocumentNo(options.docNoPrefix || options.bizType),
        status: defaults.status ?? options.defaultStatus ?? 'DRAFT',
      };
    },
    createRecord: async (record: SrmCrudRecord) => {
      return createDocument(toDocument(record, options));
    },
    deleteRecord: async (id: number | string) => {
      await deleteDocument(Number(id));
    },
    getDetail: async (id: number | string) => {
      return fromDocument(await getDocument(Number(id)), options);
    },
    loadPage: async (params: SrmCrudRecord) => {
      const pageResult = await getDocumentPage(toPageParams(params, options));
      const list = pageResult.list || [];
      return {
        list: list.map((item) => fromDocument(item, options)),
        total: pageResult.total,
      };
    },
    titleField: options.titleField,
    updateRecord: async (record: SrmCrudRecord) => {
      await updateDocument(toDocument(record, options));
      return record.id;
    },
  };
}

export function buildSrmDocumentNo(prefix: string) {
  const now = new Date();
  const datePart = [
    String(now.getFullYear()).slice(2),
    pad(now.getMonth() + 1),
    pad(now.getDate()),
  ].join('');
  const timePart = [pad(now.getHours()), pad(now.getMinutes()), pad(now.getSeconds())].join('');
  return `${prefix}-${datePart}-${timePart}`;
}

export function todayText() {
  const now = new Date();
  return [now.getFullYear(), pad(now.getMonth() + 1), pad(now.getDate())].join('-');
}

export function nowText() {
  const now = new Date();
  return `${todayText()} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function fromDocument(
  document: SrmDocumentApi.Document,
  options: SrmDocumentCrudOptions,
): SrmCrudRecord {
  const payload = parsePayload(document.payloadJson);
  const titleField = options.titleField || options.codeField;
  const row: SrmCrudRecord = {
    ...payload,
    id: document.id,
    version: document.version,
    bizType: document.bizType,
    createTime: document.createTime,
    updateTime: document.updateTime,
    docNo: document.docNo,
    title: document.title,
    supplierId: document.supplierId,
    supplierCode: document.supplierCode,
    supplierName: document.supplierName,
    materialName: document.materialName,
    status: document.status,
    applicantId: document.applicantId,
    applicantName: document.applicantName,
    applicant: payload.applicant ?? document.applicantName,
    applyDept: document.applyDept,
    applyTime: document.applyTime,
    dueDate: document.dueDate,
    totalScore: document.totalScore,
    finalScore: payload.finalScore ?? document.totalScore,
    evalGrade: document.evalGrade,
    bizCategory: document.bizCategory,
    bizLevel: document.bizLevel,
    riskLevel: payload.riskLevel ?? document.bizLevel,
    level: payload.level ?? document.bizLevel,
    periodType: document.periodType,
    evalYear: document.evalYear,
    evalQuarter: document.evalQuarter,
    remark: document.remark,
  };
  row[options.codeField] = payload[options.codeField] ?? document.docNo;
  row[titleField] = payload[titleField] ?? document.title ?? row[options.codeField];
  row.applyDate = payload.applyDate ?? toDateText(document.applyTime);
  row.deadline = payload.deadline ?? document.dueDate;
  return row;
}

function toDocument(
  record: SrmCrudRecord,
  options: SrmDocumentCrudOptions,
): SrmDocumentApi.Document {
  const docNo =
    toCleanString(record[options.codeField]) ||
    toCleanString(record.docNo) ||
    buildSrmDocumentNo(options.docNoPrefix || options.bizType);
  const titleField = options.titleField || options.codeField;
  const title =
    toCleanString(record[titleField]) ||
    toCleanString(record.title) ||
    toCleanString(record.name) ||
    toCleanString(record.supplierName) ||
    docNo;

  return {
    id: normalizeId(record.id),
    bizType: options.bizType,
    docNo,
    title,
    supplierId: normalizeId(record.supplierId),
    supplierCode: toCleanString(record.supplierCode),
    supplierName: toCleanString(record.supplierName),
    materialName: toCleanString(record.materialName),
    status: toCleanString(record.status) || toCleanString(options.defaultStatus) || 'DRAFT',
    applicantId: normalizeId(record.applicantId),
    applicantName:
      toCleanString(record.applicantName) || toCleanString(record.applicant),
    applyDept: toCleanString(record.applyDept),
    applyTime: normalizeDateTime(
      record.applyTime || record.applyDate || record.trialDate,
    ),
    dueDate: normalizeDate(record.dueDate || record.deadline),
    totalScore: normalizeNumber(record.totalScore ?? record.finalScore),
    evalGrade: toCleanString(record.evalGrade),
    bizCategory: resolveBizCategory(record),
    bizLevel: resolveBizLevel(record),
    periodType: toCleanString(record.periodType),
    evalYear: normalizeNumber(record.evalYear),
    evalQuarter: normalizeNumber(record.evalQuarter),
    payloadJson: JSON.stringify(record || {}),
    remark: toCleanString(record.remark),
    version: normalizeNumber(record.version),
  };
}

function toPageParams(
  params: SrmCrudRecord,
  options: SrmDocumentCrudOptions,
): SrmCrudRecord {
  return {
    ...params,
    bizType: options.bizType,
    bizCategory: resolveBizCategory(params),
    bizLevel: resolveBizLevel(params),
    docNo: toCleanString(params[options.codeField]) || toCleanString(params.docNo),
    materialName: toCleanString(params.materialName),
    payloadKeyword:
      toCleanString(params.payloadKeyword) ||
      toCleanString(params.planTitle) ||
      toCleanString(params.templateName) ||
      toCleanString(params.scoringMethod) ||
      toCleanString(params.dataSource),
    status: toCleanString(params.status),
    supplierName: toCleanString(params.supplierName),
    title:
      toCleanString(params[options.titleField || '']) ||
      toCleanString(params.title) ||
      toCleanString(params.name),
  };
}

function resolveDefaults(
  defaults?: (() => SrmCrudRecord) | SrmCrudRecord,
): SrmCrudRecord {
  if (!defaults) {
    return {};
  }
  if (typeof defaults === 'function') {
    return defaults();
  }
  return JSON.parse(JSON.stringify(defaults || {}));
}

function resolveBizCategory(record: SrmCrudRecord) {
  return (
    toCleanString(record.bizCategory) ||
    toCleanString(record.category) ||
    toCleanString(record.type) ||
    toCleanString(record.applyType) ||
    toCleanString(record.exitType) ||
    toCleanString(record.source) ||
    toCleanString(record.verifyType)
  );
}

function resolveBizLevel(record: SrmCrudRecord) {
  return (
    toCleanString(record.bizLevel) ||
    toCleanString(record.riskLevel) ||
    toCleanString(record.level)
  );
}

function parsePayload(payloadJson?: string) {
  if (!payloadJson) {
    return {};
  }
  try {
    const payload = JSON.parse(payloadJson);
    return payload && typeof payload === 'object' ? payload : {};
  } catch {
    return {};
  }
}

function normalizeDateTime(value: unknown) {
  const text = toCleanString(value);
  if (!text) {
    return undefined;
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return `${text} 00:00:00`;
  }
  return text.replace('T', ' ').slice(0, 19);
}

function normalizeDate(value: unknown) {
  const text = toCleanString(value);
  return text ? text.slice(0, 10) : undefined;
}

function toDateText(value?: string) {
  return value ? value.slice(0, 10) : undefined;
}

function normalizeId(value: unknown) {
  const id = normalizeNumber(value);
  return id && Number.isFinite(id) ? id : undefined;
}

function normalizeNumber(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function toCleanString(value: unknown) {
  if (value === undefined || value === null) {
    return undefined;
  }
  const text = String(value).trim();
  return text || undefined;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}
