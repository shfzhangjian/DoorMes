type RuntimeSchemaSource = Record<string, any> | string | undefined;

function parseRuntimeSchema(source?: RuntimeSchemaSource): Record<string, any> {
  if (!source) return {};
  if (typeof source === 'object' && !Array.isArray(source)) return source;
  try {
    const parsed = JSON.parse(String(source));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
}

function ensureArray<T = any>(value?: T | T[]) {
  return Array.isArray(value) ? value : value ? [value] : [];
}

function readText(...values: any[]) {
  return values
    .map((value) => String(value ?? '').trim())
    .filter(Boolean)
    .join(' ');
}

function markerIncludesDepth(marker: string) {
  const normalized = marker.toLowerCase();
  return normalized.includes('firstslotdepth') || marker.includes('首件槽深') || marker.includes('槽深');
}

function isDepthRuntimeSection(section?: Record<string, any>) {
  if (!section || typeof section !== 'object') return false;
  const marker = readText(section.key, section.rowKey, section.type, section.title, section.label, section.id);
  if (markerIncludesDepth(marker)) return true;
  return ensureArray<Record<string, any>>(section.fields).some((field) =>
    markerIncludesDepth(readText(field.field, field.key, field.label, field.title, field.id)),
  );
}

function renderNodeChildren(node?: Record<string, any>) {
  return ensureArray<Record<string, any>>(node?.slots?.default || node?.children);
}

function renderNodeHasDepth(node?: Record<string, any>) {
  if (!node || typeof node !== 'object') return false;
  const marker = readText(
    node.id,
    node.key,
    node.type,
    node.component,
    node.binding?.path,
    node.props?.field,
    node.props?.label,
    node.props?.title,
  );
  if (markerIncludesDepth(marker)) return true;
  return renderNodeChildren(node).some((child) => renderNodeHasDepth(child));
}

function readExplicitBoolean(schema: Record<string, any>, ...keys: string[]) {
  for (const key of keys) {
    if (schema[key] === true || schema[key] === 'true' || schema[key] === 1 || schema[key] === '1') return true;
    if (schema[key] === false || schema[key] === 'false' || schema[key] === 0 || schema[key] === '0') return false;
  }
  return undefined;
}

export function shouldShowPressSlotIntermediateDepthSection(record?: Record<string, any>) {
  const schema = parseRuntimeSchema(record?.stationFormSchemaJson || record?.schemaJson || record?.runtimeSchema);
  const explicitShow = readExplicitBoolean(schema, 'showFirstSlotDepth', 'firstSlotDepthVisible', 'slotDepthVisible', 'showSlotDepthSection');
  if (explicitShow !== undefined) return explicitShow;
  const explicitHide = readExplicitBoolean(schema, 'hideFirstSlotDepth', 'hideSlotDepthSection');
  if (explicitHide === true) return false;

  const runtimeLayout = schema.runtimeLayout;
  if (!runtimeLayout || typeof runtimeLayout !== 'object' || Array.isArray(runtimeLayout)) {
    return true;
  }
  const sections = [
    ...ensureArray<Record<string, any>>(runtimeLayout.sections),
    ...ensureArray<Record<string, any>>(runtimeLayout.regions),
    ...ensureArray<Record<string, any>>(runtimeLayout.blocks),
  ];
  if (sections.some(isDepthRuntimeSection)) return true;
  return renderNodeHasDepth(runtimeLayout.renderTree);
}
