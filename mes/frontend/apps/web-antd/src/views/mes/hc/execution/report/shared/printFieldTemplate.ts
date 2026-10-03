import { getActivePrintFieldTemplate } from '#/api/mes/hc/printfieldtemplate';

export interface PrintTicketField {
  fieldKey?: string;
  label: string;
  value?: unknown;
}

type PrintFieldContext = Record<string, unknown>;

const TEMPLATE_CACHE_TTL_MS = 10_000;
const templateCache = new Map<
  string,
  { expireAt: number; promise: ReturnType<typeof getActivePrintFieldTemplate> }
>();

function readByPath(context: PrintFieldContext, path?: string) {
  if (!path) return undefined;
  return path.split('.').reduce<unknown>((current, key) => {
    if (current == null || typeof current !== 'object') return undefined;
    return (current as Record<string, unknown>)[key];
  }, context);
}

function normalizePrintValue(
  value: unknown,
  defaultValue?: string,
  suffix?: string,
) {
  const normalized =
    value === undefined || value === null || String(value).trim() === ''
      ? defaultValue || '-'
      : String(value);
  return suffix ? `${normalized}${suffix}` : normalized;
}

function getTemplate(templateCode: string) {
  const now = Date.now();
  const cached = templateCache.get(templateCode);
  if (cached && cached.expireAt > now) {
    return cached.promise;
  }
  const promise = getActivePrintFieldTemplate(templateCode);
  templateCache.set(templateCode, {
    expireAt: now + TEMPLATE_CACHE_TTL_MS,
    promise,
  });
  return promise;
}

/**
 * 读取启用的打印字段模板。
 *
 * 除字段明细外，调用方可使用 documentName 作为打印单据表头。
 */
export async function getActivePrintFieldTemplateCached(templateCode: string) {
  try {
    return await getTemplate(templateCode);
  } catch {
    return null;
  }
}

export async function applyPrintFieldTemplate(
  templateCode: string,
  fallbackFields: PrintTicketField[],
  context: PrintFieldContext,
) {
  try {
    const template = await getActivePrintFieldTemplateCached(templateCode);
    const items = (template?.items || [])
      .filter((item) => item.visible !== false)
      .slice()
      .sort((a, b) => (a.sort || 0) - (b.sort || 0));
    if (!items.length) {
      return fallbackFields;
    }
    return items.map((item) => ({
      fieldKey: item.fieldKey,
      label: item.fieldLabel || item.fieldKey || item.valueKey || '',
      value: normalizePrintValue(
        readByPath(context, item.valueKey),
        item.defaultValue,
        item.suffix,
      ),
    }));
  } catch {
    return fallbackFields;
  }
}
