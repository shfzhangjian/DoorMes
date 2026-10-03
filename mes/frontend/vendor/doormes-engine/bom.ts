import type { DesignDocument, WindowUnit } from '@doormes/contracts';
import { calculateFormalBom } from '@doormes/calculation-engine';
import { findMemberHost, partitionTopologyRegion, resolveFabricationAssemblyGeometry, resolveWindowGeometry, resolveWindowInstallationSurroundGeometry } from '@doormes/geometry-topology';
import type { FormalBomResult, ManufacturingFeature } from '@doormes/manufacturing-model';
import { REFERENCE_SIMULATION_MANUFACTURING_CATALOG } from './packages/manufacturing-model/src/reference-catalog';
import { parseFormalDesignDocument } from '@doormes/persistence/local-design';

export interface DrawingBomDimensions {
  /** Reference calculations and design envelopes are never released cutting dimensions. */
  basis: 'reference-rule' | 'design-envelope' | 'unspecified';
  label: string;
  lengthMm?: number;
  widthMm?: number;
  heightMm?: number;
  thicknessMm?: number;
  grossLengthMm?: number;
  cutLeftDeg?: number;
  cutRightDeg?: number;
  envelopeWidthMm?: number;
  envelopeHeightMm?: number;
}

export interface DrawingBomLine {
  objectId: string;
  sourceObjectIds: string[];
  sourceWindowId: string;
  sourceMark: string;
  sourceComponentId: string;
  displayCode: string;
  category: 'profile' | 'glass' | 'gasket' | 'bead' | 'hardware' | 'panel' | 'accessory';
  name: string;
  modelCode: string;
  specification: string;
  material?: string;
  color?: string;
  /** Frozen design appearance; this is not a released surface-process mapping. */
  appearance?: Record<string, unknown>;
  quantity: number;
  unit: 'pcs' | 'set' | 'm';
  dimensions: DrawingBomDimensions;
  remark: string;
  productionReady: false;
  ruleId?: string;
  ruleVersion?: string;
  catalogItemId?: string;
  catalogVersion?: string;
}

export interface DrawingBomDiagnostic {
  code: string;
  message: string;
  sourceObjectIds: string[];
  blocksProduction: true;
}

export interface DrawingBom {
  schemaVersion: 'doormes-drawing-bom.v1';
  designId: string;
  revision: number;
  scope: 'design-reference';
  productionReady: false;
  /** Each line already includes the authored window.quantity exactly once. */
  quantityBasis: 'document-window-quantities';
  lines: DrawingBomLine[];
  diagnostics: DrawingBomDiagnostic[];
}

const referenceNotice = '参考目录及扣减规则尚未经工厂校核，参考长度、截面和切角不可直接用于下料。';
const envelopeNotice = '仅记录设计构件及包络，缺少经校核的型材截面、扣减和加工规则。';
const sides = ['top', 'bottom', 'left', 'right'] as const;
const sideNames = { top: '上', bottom: '下', left: '左', right: '右' };
const compare = (a: string, b: string) => a < b ? -1 : a > b ? 1 : 0;
const unique = (values: readonly string[]) => [...new Set(values)];
const geometryCache = new WeakMap<WindowUnit, ReturnType<typeof resolveWindowGeometry>>();

function windowGeometry(window: WindowUnit) {
  let geometry = geometryCache.get(window);
  if (!geometry) { geometry = resolveWindowGeometry(window); geometryCache.set(window, geometry); }
  return geometry;
}

function sourceIdentity(window: WindowUnit, component: string, original: readonly string[]) {
  const geometry = windowGeometry(window);
  const cellMatch = /^cell\.(\d+)\.(\d+)(.*)$/.exec(component);
  const cell = cellMatch ? window.layout.cells[(Number(cellMatch[1]) - 1) * window.layout.columns.length + Number(cellMatch[2]) - 1] : undefined;
  const frame = /^frame\.(top|bottom|left|right)$/.exec(component);
  const framePart = frame ? geometry.frames.find((part) => part.side === frame[1]) : undefined;
  const member = geometry.members.find((part) => part.sourceComponentId === component);
  const exactIds = [framePart?.objectId, member?.objectId, cell?.objectId].flatMap((id) => id ? [String(id)] : []);
  return {
    objectId: `BOM:${window.objectId}:${cell ? `${cell.objectId}${cellMatch![3]}` : framePart?.objectId ?? member?.objectId ?? component}`,
    sourceObjectIds: unique([...original, ...exactIds])
  };
}

function featureDimensions(feature: ManufacturingFeature): DrawingBomDimensions {
  if (feature.kind === 'hardware-demand') return { basis: 'unspecified', label: '五金需求；无加工尺寸' };
  if (feature.kind === 'glass-panel') return {
    basis: 'reference-rule', label: '参考玻璃尺寸（待校核）',
    widthMm: feature.widthMm, heightMm: feature.heightMm, thicknessMm: feature.thicknessMm
  };
  const result: DrawingBomDimensions = { basis: 'reference-rule', label: '参考长度/扣减结果（待校核）', lengthMm: feature.lengthMm };
  if ('grossLengthMm' in feature) result.grossLengthMm = feature.grossLengthMm;
  if ('cutLeftDeg' in feature) {
    result.cutLeftDeg = feature.cutLeftDeg;
    result.cutRightDeg = feature.cutRightDeg;
  }
  if (feature.kind === 'installation-material' && feature.category === 'panel') {
    result.widthMm = feature.widthMm;
    result.heightMm = feature.heightMm;
    result.label = '参考包套板尺寸（待校核）';
  }
  return result;
}

function featureLines(result: FormalBomResult, document: DesignDocument): DrawingBomLine[] {
  return result.features.map((feature, index) => {
    const row = result.mbom.lines[index]!;
    const window = document.windows.find((candidate) => candidate.objectId === row.sourceWindowId)!;
    const identity = feature.kind === 'engineering-joint-material'
      ? { objectId: `BOM:${feature.assemblyId}:${feature.jointId}:${feature.role}`, sourceObjectIds: [...feature.sourceObjectIds] }
      : sourceIdentity(window, feature.sourceComponentId, feature.sourceObjectIds);
    const remarks = window.designComponentRemarks;
    const member = window.topology.members.find((item) => feature.sourceObjectIds.includes(item.objectId));
    const roleRemark = feature.kind === 'glass-panel' ? remarks?.glass
      : feature.kind === 'hardware-demand' ? remarks?.hardware
      : feature.kind === 'installation-material' || feature.sourceComponentId.startsWith('installation.') ? remarks?.surround
      : feature.kind === 'engineering-joint-material' ? '' : remarks?.profile;
    const frameSide = /^frame\.(top|bottom|left|right)$/.exec(feature.sourceComponentId)?.[1];
    const frameModel = frameSide ? window.topology.frameSegments.find((item) => item.side === frameSide)?.profileId : undefined;
    return {
      ...identity, sourceWindowId: row.sourceWindowId, sourceMark: row.sourceMark,
      sourceComponentId: feature.sourceComponentId, displayCode: '', category: row.category,
      name: row.name, modelCode: frameModel || row.materialCode,
      specification: feature.kind === 'glass-panel' ? feature.specification ?? feature.name : row.spec,
      material: row.material, color: row.color,
      quantity: row.quantity, unit: row.unit, dimensions: featureDimensions(feature),
      remark: [roleRemark, member?.note].filter(Boolean).join('；'), productionReady: false,
      ruleId: feature.ruleId, ruleVersion: feature.ruleVersion,
      ...('catalogItemId' in feature && feature.catalogItemId ? { catalogItemId: feature.catalogItemId } : {}),
      ...('catalogVersion' in feature && feature.catalogVersion ? { catalogVersion: feature.catalogVersion } : {})
    };
  });
}

/** Geometry fallback records only named design components, with no invented cut lengths. */
function designLines(window: WindowUnit): DrawingBomLine[] {
  const geometry = windowGeometry(window);
  const catalog = REFERENCE_SIMULATION_MANUFACTURING_CATALOG;
  const series = catalog.profileSystems.find((item) => item.id === window.profileSystemId);
  const rows: DrawingBomLine[] = [];
  const add = (component: string, name: string, category: DrawingBomLine['category'], modelCode: string,
    quantity: number, sourceIds: string[], envelope?: { widthMm: number; heightMm: number }, specification = '', unit: DrawingBomLine['unit'] = 'pcs') => {
    const remarkKey = component.startsWith('installation.') ? 'surround' : category === 'glass' ? 'glass' : category === 'hardware' ? 'hardware' : 'profile';
    rows.push({
      ...sourceIdentity(window, component, [window.objectId, ...sourceIds]),
      sourceWindowId: window.objectId, sourceMark: window.mark, sourceComponentId: component,
      displayCode: '', category, name, modelCode, specification, quantity, unit,
      dimensions: envelope ? {
        basis: 'design-envelope', label: '设计包络（非下料尺寸）',
        envelopeWidthMm: envelope.widthMm, envelopeHeightMm: envelope.heightMm
      } : { basis: 'unspecified', label: '加工尺寸待校核' },
      remark: window.designComponentRemarks?.[remarkKey] ?? '', productionReady: false
    });
  };
  for (const side of sides) {
    const part = geometry.frames.find((item) => item.side === side);
    const selection = window.topology.frameSegments.find((item) => item.side === side);
    add(`frame.${side}`, `${sideNames[side]}框`, 'profile', selection?.profileId || series?.frameProfile || window.profileSystemId,
      window.quantity, part ? [part.objectId] : [], part);
  }
  for (const part of geometry.members) {
    const selected = window.topology.members.find((item) => item.objectId === part.objectId || part.sourceComponentId === `topology.member.${item.objectId}`);
    add(part.sourceComponentId, part.orientation === 'vertical' ? '竖梃' : '横梃', 'profile',
      selected?.profileId || series?.mullionProfile || window.profileSystemId, window.quantity, [part.objectId], part);
    if (selected?.note) rows[rows.length - 1]!.remark += `${rows[rows.length - 1]!.remark ? '；' : ''}${selected.note}`;
  }
  for (const part of geometry.meetingMullions) {
    add(part.sourceComponentId, part.kind === 'fixed-mullion' ? '固定中梃' : '假中梃', 'profile',
      series?.mullionProfile || window.profileSystemId, window.quantity, [part.objectId, part.sourceObjectId], part);
  }
  for (const cell of geometry.cells) {
    const definition = window.layout.cells.find((item) => item.objectId === cell.objectId)!;
    const prefix = `cell.${cell.row + 1}.${cell.column + 1}`;
    const panels = definition.type === 'sliding'
      ? geometry.slidingPanels.filter((item) => item.sourceObjectId === cell.objectId)
      : geometry.openings.filter((item) => item.objectId === cell.objectId);
    if (definition.type === 'fixed_glass') {
      const members = window.topology.members.filter((member) => findMemberHost(window.layout, member)?.cell.objectId === cell.objectId);
      const partition = partitionTopologyRegion(members);
      const regions = partition.valid ? partition.regions : [{ xStart: 0, xEnd: 1, yStart: 0, yEnd: 1 }];
      regions.forEach((region, index) => add(`${prefix}.glass${regions.length > 1 ? `.${index + 1}` : ''}`,
        partition.valid ? '固定玻璃（待加工校核）' : '玻璃区域（分区待校核）', 'glass', window.defaultGlassSelection?.materialCode || window.defaultGlassTypeId,
        window.quantity, [cell.objectId], { widthMm: cell.widthMm * (region.xEnd - region.xStart), heightMm: cell.heightMm * (region.yEnd - region.yStart) },
        window.defaultGlassSelection?.specification ?? ''));
    }
    for (const panel of panels) {
      for (const side of sides) add(`${panel.sourceComponentId}.sash.${side}`, `${'panelLabel' in panel ? panel.panelLabel : panel.panelId}扇${sideNames[side]}料`,
        'profile', series?.sashProfile || window.profileSystemId, window.quantity, [cell.objectId, panel.objectId], undefined,
        '仅识别扇型材部位；长度及截面待校核');
      add(`${panel.sourceComponentId}.glass`, `${'panelLabel' in panel ? panel.panelLabel : panel.panelId}玻璃`, 'glass',
        window.defaultGlassSelection?.materialCode || window.defaultGlassTypeId, window.quantity, [cell.objectId, panel.objectId], panel,
        window.defaultGlassSelection?.specification ?? '');
    }
    if (definition.type !== 'fixed_glass') add(`${prefix}.hardware-set`, '开启五金套装（明细待映射）', 'hardware',
      definition.hardwareSetId || window.defaultHardwareSetId, window.quantity, [cell.objectId], undefined,
      '图纸指定套装；内部零件及数量待工厂目录映射', 'set');
  }
  for (const track of geometry.slidingTracks) add(track.sourceComponentId, `推拉轨道 ${track.trackIndex + 1}（设计轨位）`, 'profile',
    '', window.quantity, [track.sourceObjectId, track.objectId], undefined, '轨位不代表已确认的采购轨道型号或截面');
  const surroundGeometry = resolveWindowInstallationSurroundGeometry(window);
  const surround = surroundGeometry.installation.surround;
  const selection = window.installationSurroundSelection;
  if (surround.enabled) {
    const layers = [
      ...(surroundGeometry.outsideEnabled ? [{ id: 'outside', name: '外包套', widthMm: surround.outsideWidthMm }] : []),
      ...(surroundGeometry.insideEnabled ? [{ id: 'inside', name: '内包套', widthMm: surround.insideWidthMm }] : [])
    ];
    for (const piece of surroundGeometry.pieces) {
      for (const layer of layers) add(`installation.surround.${layer.id}.${piece.side}`, `${layer.name}-${sideNames[piece.side]}`, 'profile',
        selection?.trimMaterialCode || surround.materialCode, window.quantity, [], { widthMm: layer.widthMm, heightMm: piece.lengthMm });
      if (surroundGeometry.linerEnabled) add(`installation.surround.liner.${piece.side}`, `洞口衬板-${sideNames[piece.side]}`, 'panel',
        selection?.linerMaterialCode || '', window.quantity, [], { widthMm: surround.wallThicknessMm, heightMm: piece.lengthMm });
    }
    if (surroundGeometry.cornerCount && layers.length) add('installation.surround.corner_connector', '包套转角连接件（待校核）', 'accessory',
      selection?.cornerConnectorMaterialCode || '', window.quantity * surroundGeometry.cornerCount * layers.length, []);
    if (surroundGeometry.perimeterMm > 0) add('installation.surround.seal', '包套设计密封路径（待校核）', 'gasket',
      selection?.sealMaterialCode || '', surroundGeometry.perimeterMm * window.quantity / 1000, [], undefined,
      '按设计周长记录；实际密封材料用量待校核', 'm');
  }
  return rows;
}

function numberRows(lines: DrawingBomLine[], document: DesignDocument) {
  const options = new Map<string, string | undefined>((document.factoryDrawingElementOptions ?? []).map((item) => [item.objectId, item.factoryDrawingNumber]));
  const parentIds = new Set([...document.windows.map((item) => String(item.objectId)), ...(document.assemblies ?? []).map((item) => String(item.objectId))]);
  const counts = new Map<string, number>();
  const used = new Set<string>();
  const code = (row: DrawingBomLine) => row.sourceComponentId.startsWith('frame.') ? 'FR'
    : row.sourceComponentId.includes('.sash.') ? 'SA'
    : row.sourceComponentId.includes('mullion') || row.sourceComponentId.includes('Mullion') || row.sourceComponentId.includes('member') || row.sourceComponentId.startsWith('divider.') ? 'MU'
    : ({ profile: 'PR', glass: 'GL', gasket: 'SE', bead: 'BD', hardware: 'HW', panel: 'PN', accessory: 'AC' }[row.category]);
  const customById = new Map<string, string>();
  const customCounts = new Map<string, number>();
  for (const row of lines) {
    const custom = unique(row.sourceObjectIds.filter((id) => !parentIds.has(id)).map((id) => options.get(id)).filter((value): value is string => !!value));
    if (custom.length > 1) throw new Error('Conflicting saved drawing numbers for one BOM component.');
    if (custom[0]) { customById.set(row.objectId, custom[0]); customCounts.set(custom[0], (customCounts.get(custom[0]) ?? 0) + 1); }
  }
  // Reserve authored numbers before generating any other row's number.
  for (const row of lines) {
    const custom = customById.get(row.objectId);
    if (custom && customCounts.get(custom) === 1) { row.displayCode = custom; used.add(custom); }
  }
  for (const row of [...lines].sort((a, b) => compare(a.objectId, b.objectId))) {
    if (row.displayCode) continue;
    const base = `${customById.get(row.objectId) || row.sourceMark}-${code(row)}`;
    let count = (counts.get(base) ?? 0) + 1;
    let value = `${base}${String(count).padStart(2, '0')}`;
    while (used.has(value)) value = `${base}${String(++count).padStart(2, '0')}`;
    counts.set(base, count);
    row.displayCode = value;
    used.add(value);
  }
}

/** Include the authored finish in change comparisons without promoting it to a manufacturing rule. */
function attachDesignAppearances(lines: DrawingBomLine[], document: DesignDocument) {
  const byWindow = new Map<string, WindowUnit>(document.windows.map((window) => [window.objectId, window]));
  for (const line of lines) {
    const window = byWindow.get(line.sourceWindowId);
    const visual = window?.visualConfiguration;
    if (!visual || line.sourceComponentId.startsWith('assembly.')) continue;
    const component = line.sourceComponentId;
    const appearances = visual.appearance;
    if (line.category === 'glass') {
      line.appearance = { ...appearances.glass };
      line.color = appearances.glass.baseColor;
    } else if (component.includes('installation.surround.outside.')) {
      line.appearance = { ...appearances.surroundOutside };
      line.color = appearances.surroundOutside.baseColor;
    } else if (component.includes('installation.surround.inside.')) {
      line.appearance = { ...appearances.surroundInside };
      line.color = appearances.surroundInside.baseColor;
    } else if (component.includes('installation.surround.liner.')) {
      line.appearance = { ...appearances.surroundLiner };
      line.color = appearances.surroundLiner.baseColor;
    } else if (line.category === 'profile' && !component.startsWith('installation.')) {
      const slot = component.includes('flyingMullion') ? 'flyingMullion'
        : component.includes('.sash.') ? 'sash'
        : component.startsWith('frame.') ? 'frame' : 'mullion';
      line.appearance = structuredClone(appearances[slot]) as unknown as Record<string, unknown>;
      line.color = `${appearances[slot].inside.baseColor}/${appearances[slot].outside.baseColor}`;
    } else if (line.category === 'hardware' && /\.(handle|hinge)$/.test(component)) {
      const roles = component.endsWith('.handle') ? ['handle'] : ['hinge-sash-leaf', 'hinge-frame-leaf'];
      line.appearance = Object.fromEntries(visual.hardwareModels.filter((item) => roles.includes(item.role))
        .map((item) => [item.role, { ...item.model.appearance }]));
    }
  }
}

/** Pure, deterministic server entry point. Order-set quantity is applied by the order service. */
export function deriveDrawingBom(input: unknown): DrawingBom {
  const document = parseFormalDesignDocument(input);
  const lines: DrawingBomLine[] = [];
  const diagnostics: DrawingBomDiagnostic[] = [{ code: 'REFERENCE_RULES_NOT_FACTORY_APPROVED', message: referenceNotice,
    sourceObjectIds: document.windows.map((window) => window.objectId), blocksProduction: true }];
  const appendResult = (result: FormalBomResult) => {
    lines.push(...featureLines(result, document));
    diagnostics.push(...result.diagnostics.filter((item) => item.blocksConfirmation).map((item) => ({
      code: item.code, message: item.message, sourceObjectIds: [...item.sourceObjectIds], blocksProduction: true as const
    })));
  };
  try {
    appendResult(calculateFormalBom(document, REFERENCE_SIMULATION_MANUFACTURING_CATALOG));
  } catch {
    // Isolate catalog failures so another valid window does not lose its material rows.
    for (const window of document.windows) {
      try {
        appendResult(calculateFormalBom({ ...document, windows: [window], assemblies: [] }, REFERENCE_SIMULATION_MANUFACTURING_CATALOG));
      } catch {
        diagnostics.push({ code: 'MANUFACTURING_REFERENCE_UNAVAILABLE', message: '该窗缺少可用的制造参考目录，保留设计构件清单。',
          sourceObjectIds: [window.objectId], blocksProduction: true });
      }
    }
  }
  for (const window of document.windows) {
    if (lines.some((line) => line.sourceWindowId === window.objectId && !line.sourceComponentId.startsWith('assembly.'))) continue;
    lines.push(...designLines(window));
    diagnostics.push({ code: 'DESIGN_COMPONENTS_REQUIRE_MANUFACTURING_MAPPING', message: envelopeNotice,
      sourceObjectIds: [window.objectId], blocksProduction: true });
  }
  // Keep every authored physical joint even when no material rule can resolve it.
  for (const assembly of document.assemblies ?? []) {
    const geometry = resolveFabricationAssemblyGeometry(assembly, document.windows);
    for (const joint of assembly.joints) {
      if (lines.some((line) => line.sourceObjectIds.includes(joint.objectId) && line.sourceComponentId.startsWith('assembly.'))) continue;
      const first = assembly.instances.find((item) => item.objectId === joint.firstInstanceId)!;
      const bounds = geometry.joints.find((item) => item.jointId === joint.objectId)!;
      lines.push({ objectId: `BOM:${assembly.objectId}:${joint.objectId}:connector`,
        sourceObjectIds: [assembly.objectId, joint.objectId, joint.firstInstanceId, joint.secondInstanceId],
        sourceWindowId: first.windowId, sourceMark: assembly.mark, sourceComponentId: `assembly.${assembly.objectId}.joint.${joint.objectId}`,
        displayCode: '', category: 'accessory', name: joint.catalogSelection?.businessName || '装配连接件（待映射）',
        modelCode: joint.catalogSelection?.catalogItemId || joint.jointType,
        specification: joint.catalogSelection?.specification || `设计接缝 ${joint.gapMm} mm`, quantity: 1, unit: 'pcs',
        dimensions: { basis: 'design-envelope', label: '设计连接区域（非加工尺寸）', envelopeWidthMm: bounds.widthMm, envelopeHeightMm: bounds.heightMm },
        remark: '连接件及紧固件明细待工厂目录校核', productionReady: false,
        ...(joint.catalogSelection ? { catalogItemId: joint.catalogSelection.catalogItemId, catalogVersion: joint.catalogSelection.catalogVersion } : {}) });
      diagnostics.push({ code: 'JOINT_MANUFACTURING_MAPPING_REQUIRED', message: '连接件型号、紧固件及加工规则尚未完整映射。',
        sourceObjectIds: [assembly.objectId, joint.objectId], blocksProduction: true });
    }
  }
  if (new Set(lines.map((line) => line.objectId)).size !== lines.length) throw new Error('Duplicate BOM component identity.');
  attachDesignAppearances(lines, document);
  numberRows(lines, document);
  return { schemaVersion: 'doormes-drawing-bom.v1', designId: document.designId, revision: document.revision,
    scope: 'design-reference', productionReady: false, quantityBasis: 'document-window-quantities', lines, diagnostics };
}
