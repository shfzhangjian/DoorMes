export interface EntryRulePosition {
  code: string;
  name: string;
  sort: number;
}

export interface EntryRuleItemLike {
  cellRequiredCount?: number;
  itemType?: string;
  requiredSampleCount?: number;
  sampleSize?: number;
  templateParams?: string;
}

export function parseEntryRuleParams(
  templateParams?: string,
): Record<string, any> {
  if (!templateParams) return {};
  try {
    return JSON.parse(templateParams);
  } catch {
    return {};
  }
}

export function resolveEntryRuleRepeatCount(item: EntryRuleItemLike) {
  const params = parseEntryRuleParams(item.templateParams);
  const repeatCount = Number(params.repeatCount);
  return Number.isFinite(repeatCount) && repeatCount > 0 ? repeatCount : 1;
}

export function resolveEntryRuleSampleSize(item: EntryRuleItemLike) {
  const params = parseEntryRuleParams(item.templateParams);
  const sampleSize = Number(params.sampleSize);
  if (Number.isFinite(sampleSize) && sampleSize > 0) return sampleSize;
  return (
    item.sampleSize || item.requiredSampleCount || item.cellRequiredCount || 1
  );
}

export function hasEntryRulePositions(item: EntryRuleItemLike) {
  const params = parseEntryRuleParams(item.templateParams);
  return Array.isArray(params.positions) && params.positions.length > 0;
}

export function resolveEntryRulePositions(item: EntryRuleItemLike) {
  const params = parseEntryRuleParams(item.templateParams);
  if (Array.isArray(params.positions) && params.positions.length > 0) {
    return (params.positions as Array<Partial<EntryRulePosition>>)
      .map(
        (position, index): EntryRulePosition => ({
          code: position.code || position.name || `P${index + 1}`,
          name: position.name || position.code || `${index + 1}`,
          sort: position.sort ?? index + 1,
        }),
      )
      .toSorted((a, b) => a.sort - b.sort);
  }
  return buildFallbackEntryRulePositions(
    item,
    resolveEntryRuleSampleSize(item),
  );
}

export function resolveEntryRuleExpectedSampleCount(item: EntryRuleItemLike) {
  const positions = resolveEntryRulePositions(item);
  return Math.max(
    resolveEntryRuleSampleSize(item),
    positions.length * resolveEntryRuleRepeatCount(item),
  );
}

export function compactEntryRulePositionNames(positions: EntryRulePosition[]) {
  const names = positions.map((position) => position.name);
  const numeric = names.every((name) => /^\d+$/.test(name));
  if (numeric && names.length > 6) {
    return `${names[0]}-${names[names.length - 1]}`;
  }
  return names.join('/');
}

function buildFallbackEntryRulePositions(
  item: EntryRuleItemLike,
  sampleSize: number,
) {
  return Array.from({ length: sampleSize }).map(
    (_, index): EntryRulePosition => ({
      code:
        item.itemType === 'QUALITATIVE' && sampleSize === 1
          ? 'QUALITATIVE'
          : `P${index + 1}`,
      name:
        item.itemType === 'QUALITATIVE' && sampleSize === 1
          ? '判定'
          : `${index + 1}`,
      sort: index + 1,
    }),
  );
}
